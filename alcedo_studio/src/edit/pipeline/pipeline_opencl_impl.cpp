//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifdef HAVE_OPENCL

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <memory>
#include <stdexcept>
#include <utility>
#include <vector>

#include "edit/operators/GPU_kernels/fused_param.hpp"
#include "edit/operators/GPU_kernels/opencl_param.hpp"
#include "edit/pipeline/opencl_pipeline_programs.hpp"
#include "edit/pipeline/pipeline_gpu_wrapper.hpp"
#include "image/image_buffer.hpp"
#include "image/opencl_image.hpp"
#include "opencl/opencl_context.hpp"
#include "opencl/opencl_program_library.hpp"
#include "ui/edit_viewer/frame_sink.hpp"

namespace alcedo {
namespace {

auto ResolveViewerDisplayConfig(const OperatorParams& params) -> ViewerDisplayConfig {
  return ViewerDisplayConfig{params.to_output_params_.encoding_space_,
                             params.to_output_params_.eotf_};
}

class OpenCLGPUPipeline final : public GPUPipelineImpl {
 private:
  std::shared_ptr<ImageBuffer>           input_img_;
  OperatorParams*                        cpu_params_      = nullptr;
  IFrameSink*                            frame_sink_      = nullptr;
  FusedOperatorParams                    fused_params_    = {};
  OpenCL::Pipeline::OpenClFusedResources resources_       = {};

  cl_kernel                              fused_kernel_    = nullptr;
  cl_kernel                              validate_kernel_ = nullptr;

  opencl::OpenClImage                    working_;

  void                                   EnsureOpenClInput() {
    if (!input_img_) {
      throw std::runtime_error("OpenCL fused pipeline: input image is null.");
    }

    const bool has_valid_gpu = input_img_->gpu_data_valid_;
    const bool has_valid_cpu = input_img_->cpu_data_valid_;

    if (!has_valid_gpu && !has_valid_cpu) {
      throw std::runtime_error("OpenCL fused pipeline: input image has no valid CPU or GPU data.");
    }

    const bool needs_sync = !has_valid_gpu || input_img_->GetGPUBackend() != GpuBackendKind::OpenCL;

    if (needs_sync) {
      if (has_valid_gpu && input_img_->GetGPUBackend() != GpuBackendKind::OpenCL) {
        input_img_->SyncToCPU();
      }
      input_img_->SyncToGPU(GpuBackendKind::OpenCL);
    }

    if (input_img_->GetGPUType() != CV_32FC4) {
      input_img_->ConvertGPUDataTo(CV_32FC4);
    }
  }

  void EnsureKernels() {
    auto&      library = OpenClProgramLibrary::Instance();
    cl_program program = library.GetProgram(OpenCL::Pipeline::kFusedProgramName);
    if (program == nullptr) {
      throw std::runtime_error("OpenCL fused pipeline: failed to get program from library.");
    }

    if (fused_kernel_ == nullptr) {
      cl_int err    = CL_SUCCESS;
      fused_kernel_ = clCreateKernel(program, OpenCL::Pipeline::kFusedKernelName, &err);
      if (err != CL_SUCCESS || fused_kernel_ == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create kernel '" +
                                 std::string(OpenCL::Pipeline::kFusedKernelName) + "' with error " +
                                 std::to_string(err) + ".");
      }
    }

    if (validate_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      validate_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kValidateFusedParamsKernelName, &err);
      if (err != CL_SUCCESS || validate_kernel_ == nullptr) {
        throw std::runtime_error(
            "OpenCL fused pipeline: failed to create validation kernel with error " +
            std::to_string(err) + ".");
      }
    }
  }

  void ValidateParamsABI() {
    auto& context = OpenClContext::Instance();
    if (!context.IsInitialized()) {
      throw std::runtime_error("OpenCL fused pipeline: OpenCL context is not initialized.");
    }

    cl_kernel kernel = validate_kernel_;
    if (kernel == nullptr) {
      throw std::runtime_error("OpenCL fused pipeline: validation kernel is null.");
    }

    cl_int err = CL_SUCCESS;
    cl_mem output_buffer =
        clCreateBuffer(context.Context(), CL_MEM_READ_WRITE, 12 * sizeof(float), nullptr, &err);
    if (err != CL_SUCCESS || output_buffer == nullptr) {
      throw std::runtime_error("OpenCL fused pipeline: failed to create validation output buffer.");
    }

    err                = CL_SUCCESS;
    cl_uint arg_index  = 0;
    cl_mem  params_buf = resources_.params_buffer_.Get();
    err |= clSetKernelArg(kernel, arg_index++, sizeof(cl_mem), &params_buf);
    err |= clSetKernelArg(kernel, arg_index++, sizeof(cl_mem), &output_buffer);
    if (err != CL_SUCCESS) {
      clReleaseMemObject(output_buffer);
      throw std::runtime_error("OpenCL fused pipeline: failed to set validation kernel arguments.");
    }

    size_t global_size = 1;
    err = clEnqueueNDRangeKernel(context.Queue(), kernel, 1, nullptr, &global_size, nullptr, 0,
                                 nullptr, nullptr);
    if (err != CL_SUCCESS) {
      clReleaseMemObject(output_buffer);
      throw std::runtime_error("OpenCL fused pipeline: failed to enqueue validation kernel.");
    }

    std::array<float, 12> result{};
    err = clEnqueueReadBuffer(context.Queue(), output_buffer, CL_TRUE, 0,
                              result.size() * sizeof(float), result.data(), 0, nullptr, nullptr);
    clReleaseMemObject(output_buffer);

    if (err != CL_SUCCESS) {
      throw std::runtime_error("OpenCL fused pipeline: failed to read validation output.");
    }

    clFinish(context.Queue());

    const auto nearly_equal = [](float lhs, float rhs) {
      return std::abs(lhs - rhs) <= 1.0e-5f * std::max(1.0f, std::abs(rhs));
    };
    const auto& params                 = resources_.opencl_params_;
    const float expected_output_header = static_cast<float>(params.to_output_params_.method_) +
                                         params.to_output_params_.display_linear_scale_;
    const float expected_open_drt_header = params.to_output_params_.limit_to_display_matx[0] +
                                           params.to_output_params_.open_drt_params_.tn_con_;
    if (!nearly_equal(result[4], expected_output_header) ||
        !nearly_equal(result[5], expected_open_drt_header) ||
        !nearly_equal(result[6], params.to_output_params_.aces_params_.ts_.forward_limit_) ||
        !nearly_equal(result[7], params.to_output_params_.aces_params_.limit_J_max) ||
        !nearly_equal(result[8], params.to_output_params_.open_drt_params_.ts_s_) ||
        !nearly_equal(result[9], params.to_output_params_.open_drt_params_.ts_m2_) ||
        !nearly_equal(result[10], params.to_output_params_.aces_params_.ts_.m_2_) ||
        !nearly_equal(result[11], params.to_output_params_.aces_params_.ts_.g_)) {
      throw std::runtime_error("OpenCL fused pipeline: fused params ABI validation failed.");
    }
  }

  void EnqueueFusedKernel(const opencl::OpenClImage& src) {
    auto& context = OpenClContext::Instance();
    if (!context.IsInitialized()) {
      throw std::runtime_error("OpenCL fused pipeline: context is not initialized.");
    }

    working_.Create(src.Width(), src.Height(), src.Type());

    cl_int             err               = CL_SUCCESS;
    cl_uint            arg_index         = 0;

    cl_mem             src_buffer        = src.Buffer();
    cl_mem             dst_buffer        = working_.Buffer();
    cl_mem             params_buffer     = resources_.params_buffer_.Get();
    cl_mem             lmt_lut_buffer    = resources_.lmt_lut_buffer_.Get();
    cl_int             width             = src.Width();
    cl_int             height            = src.Height();

    // Always pass a valid LUT buffer to satisfy the kernel signature.
    // When LMT is disabled, the kernel will skip the lookup via the
    // lmt_lut_enabled_ guard and never actually read from this buffer.
    static const float kDummyLutEntry[4] = {0.0f, 0.0f, 0.0f, 1.0f};
    cl_mem             fallback_lut      = nullptr;
    if (lmt_lut_buffer == nullptr) {
      fallback_lut =
          clCreateBuffer(context.Context(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                         sizeof(kDummyLutEntry), const_cast<float*>(kDummyLutEntry), &err);
      if (err != CL_SUCCESS || fallback_lut == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create fallback LUT buffer.");
      }
      lmt_lut_buffer = fallback_lut;
    }

    err |= clSetKernelArg(fused_kernel_, arg_index++, sizeof(cl_mem), &src_buffer);
    err |= clSetKernelArg(fused_kernel_, arg_index++, sizeof(cl_mem), &dst_buffer);
    err |= clSetKernelArg(fused_kernel_, arg_index++, sizeof(cl_mem), &params_buffer);
    err |= clSetKernelArg(fused_kernel_, arg_index++, sizeof(cl_mem), &lmt_lut_buffer);
    err |= clSetKernelArg(fused_kernel_, arg_index++, sizeof(cl_int), &width);
    err |= clSetKernelArg(fused_kernel_, arg_index++, sizeof(cl_int), &height);

    if (err != CL_SUCCESS) {
      if (fallback_lut != nullptr) clReleaseMemObject(fallback_lut);
      throw std::runtime_error("OpenCL fused pipeline: failed to set fused kernel arguments.");
    }

    size_t global_size[2] = {static_cast<size_t>(width), static_cast<size_t>(height)};
    err = clEnqueueNDRangeKernel(context.Queue(), fused_kernel_, 2, nullptr, global_size, nullptr,
                                 0, nullptr, nullptr);

    if (fallback_lut != nullptr) {
      clReleaseMemObject(fallback_lut);
    }

    if (err != CL_SUCCESS) {
      throw std::runtime_error("OpenCL fused pipeline: failed to enqueue fused kernel with error " +
                               std::to_string(err) + ".");
    }
  }

 public:
  void SetInputImage(std::shared_ptr<ImageBuffer> input_image) override {
    input_img_ = std::move(input_image);
  }

  void SetParams(OperatorParams& params) override {
    cpu_params_   = &params;
    fused_params_ = FusedParamsConverter::ConvertFromCPU(params, fused_params_);
    resources_ =
        OpenCL::Pipeline::OpenClFusedParamUploader::Upload(fused_params_, params, resources_);
  }

  void SetFrameSink(IFrameSink* frame_sink) override { frame_sink_ = frame_sink; }

  void Execute(std::shared_ptr<ImageBuffer> output_img) override {
    if (!cpu_params_) {
      throw std::runtime_error("OpenCL fused pipeline: parameters were not set.");
    }

    EnsureOpenClInput();
    EnsureKernels();
    ValidateParamsABI();

    const auto& input = input_img_->GetOpenClImage();

    EnqueueFusedKernel(input);

    auto& context = OpenClContext::Instance();
    clFinish(context.Queue());

    if (frame_sink_) {
      cv::Mat host_image;
      working_.Download(host_image);

      if (host_image.type() != CV_32FC4) {
        throw std::runtime_error("OpenCL fused pipeline: expected RGBA32F host frame for viewer.");
      }

      const size_t row_bytes =
          static_cast<size_t>(host_image.cols) * static_cast<size_t>(sizeof(cv::Vec4f));
      auto host_pixels = std::make_shared<std::vector<float>>(
          static_cast<size_t>(host_image.cols) * static_cast<size_t>(host_image.rows) * 4U);
      cv::Mat contiguous_host(host_image.rows, host_image.cols, CV_32FC4, host_pixels->data(),
                              row_bytes);
      host_image.copyTo(contiguous_host);

      const ViewerDisplayConfig display_config = ResolveViewerDisplayConfig(*cpu_params_);
      frame_sink_->SubmitHostFrame(
          ViewerFrame{host_image.cols, host_image.rows, row_bytes,
                      std::shared_ptr<const void>(host_pixels, host_pixels->data()), display_config,
                      FramePresentationMode::FullFrame});
    }

    if (output_img) {
      *output_img = ImageBuffer(std::move(working_));
    }
  }

  void ReleaseResources() override {
    if (fused_kernel_ != nullptr) {
      clReleaseKernel(fused_kernel_);
      fused_kernel_ = nullptr;
    }
    if (validate_kernel_ != nullptr) {
      clReleaseKernel(validate_kernel_);
      validate_kernel_ = nullptr;
    }
    working_.Release();
    resources_.Reset();
  }
};

}  // namespace

auto CreateOpenCLGPUPipeline() -> std::unique_ptr<GPUPipelineImpl> {
  return std::make_unique<OpenCLGPUPipeline>();
}

}  // namespace alcedo

#endif
