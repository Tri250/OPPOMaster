//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifdef HAVE_OPENCL

#include <algorithm>
#include <array>
#include <chrono>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <iomanip>
#include <iostream>
#include <memory>
#include <stdexcept>
#include <utility>
#include <vector>

#include "edit/operators/GPU_kernels/fused_param.hpp"
#include "edit/operators/GPU_kernels/opencl_param.hpp"
#include "edit/pipeline/opencl_pipeline_programs.hpp"
#include "edit/pipeline/pipeline_gpu_wrapper.hpp"
#include "edit/scope/detail/scope_opencl_shared.hpp"
#include "edit/scope/scope_analyzer.hpp"
#include "image/image_buffer.hpp"
#include "image/opencl_image.hpp"
#include "opencl/opencl_context.hpp"
#include "opencl/opencl_program_library.hpp"
#include "ui/edit_viewer/frame_sink.hpp"

namespace alcedo {
namespace {

constexpr uint32_t kOpenClNeighborMaxTapCount = 64;

enum class OpenClNeighborOpKind : uint32_t {
  Sharpen = 1,
  Clarity = 2,
};

struct OpenClNeighborStageParams {
  uint32_t                                      kind_      = 0;
  uint32_t                                      radius_    = 0;
  uint32_t                                      tap_count_ = 0;
  float                                         amount_    = 0.0f;
  float                                         threshold_ = 0.0f;
  std::array<float, kOpenClNeighborMaxTapCount> weights_   = {};
};

struct OpenClNeighborStage {
  OpenClNeighborStageParams params_ = {};
};

auto ResolveViewerDisplayConfig(const OperatorParams& params) -> ViewerDisplayConfig {
  return ViewerDisplayConfig{params.to_output_params_.encoding_space_,
                             params.to_output_params_.eotf_};
}

auto BuildGaussianWeights(float sigma, uint32_t radius)
    -> std::array<float, kOpenClNeighborMaxTapCount> {
  std::array<float, kOpenClNeighborMaxTapCount> weights{};
  const double safe_sigma  = std::max(static_cast<double>(sigma), 1.0e-4);
  const double inv2sigma2  = 0.5 / (safe_sigma * safe_sigma);
  double       full_weight = 1.0;

  weights[0]               = 1.0f;
  for (uint32_t tap = 1; tap <= radius; ++tap) {
    const double w = std::exp(-(static_cast<double>(tap) * static_cast<double>(tap)) * inv2sigma2);
    weights[tap]   = static_cast<float>(w);
    full_weight += 2.0 * w;
  }

  if (full_weight > 0.0) {
    for (uint32_t tap = 0; tap <= radius; ++tap) {
      weights[tap] = static_cast<float>(static_cast<double>(weights[tap]) / full_weight);
    }
  }

  return weights;
}

auto BuildNeighborStageParams(OpenClNeighborOpKind kind, float sigma, float amount, float threshold,
                              int gaussian_tap_count, const float* gaussian_weights)
    -> OpenClNeighborStageParams {
  OpenClNeighborStageParams params;

  params.kind_      = static_cast<uint32_t>(kind);
  params.amount_    = amount;
  params.threshold_ = threshold;

  const int clamped_tap_count =
      std::clamp(gaussian_tap_count, 0, static_cast<int>(kOpenClNeighborMaxTapCount));
  if (clamped_tap_count > 0 && gaussian_weights != nullptr) {
    params.tap_count_ = static_cast<uint32_t>(clamped_tap_count);
    params.radius_    = params.tap_count_ - 1U;
    std::copy_n(gaussian_weights, clamped_tap_count, params.weights_.begin());
    return params;
  }

  if (sigma <= 0.0f) {
    return params;
  }

  const float    safe_sigma = std::max(sigma, 1.0e-4f);
  const uint32_t max_radius =
      (kOpenClNeighborMaxTapCount > 0U) ? (kOpenClNeighborMaxTapCount - 1U) : 0U;
  params.radius_ =
      std::clamp<uint32_t>(static_cast<uint32_t>(std::ceil(3.0f * safe_sigma)), 1U, max_radius);
  params.tap_count_ = params.radius_ + 1U;
  params.weights_   = BuildGaussianWeights(safe_sigma, params.radius_);
  return params;
}

auto UploadStageParams(const OpenClNeighborStageParams& params) -> OpenCL::Pipeline::OpenClBuffer {
  return OpenCL::Pipeline::OpenClBuffer::CreateReadOnlyCopy(&params,
                                                            sizeof(OpenClNeighborStageParams));
}

void CheckOpenClFrameCopy(cl_int error, const char* operation) {
  if (error != CL_SUCCESS) {
    throw std::runtime_error(std::string("OpenCL fused pipeline: ") + operation +
                             " failed with error " + std::to_string(error) + ".");
  }
}

auto TrySubmitOpenClFrameToSink(opencl::OpenClImage& image, IFrameSink& frame_sink) -> bool {
  frame_sink.EnsureSize(image.Width(), image.Height());
  const FrameWriteMapping mapping = frame_sink.MapResourceForWrite(FrameMemoryDomain::OpenClDevice);
  if (!mapping) {
    return false;
  }

  const auto unmap = [&frame_sink]() { frame_sink.UnmapResource(); };
  if (mapping.pixel_format != FramePixelFormat::RGBA32F ||
      mapping.memory_domain != FrameMemoryDomain::OpenClDevice ||
      mapping.target_type != FrameWriteTargetType::OpenClImage || mapping.data == nullptr) {
    unmap();
    return false;
  }

  auto&        context   = OpenClContext::Instance();
  const size_t origin[3] = {0, 0, 0};
  const size_t region[3] = {static_cast<size_t>(image.Width()), static_cast<size_t>(image.Height()),
                            1};
  cl_mem       target_image = static_cast<cl_mem>(mapping.data);
  const cl_int copy_error   = clEnqueueCopyBufferToImage(
      context.Queue(), image.Buffer(), target_image, 0, origin, region, 0, nullptr, nullptr);
  if (copy_error != CL_SUCCESS) {
    unmap();
    CheckOpenClFrameCopy(copy_error, "clEnqueueCopyBufferToImage");
  }

  frame_sink.UnmapResource();
  CheckOpenClFrameCopy(clFinish(context.Queue()), "clFinish after OpenGL frame copy");
  frame_sink.NotifyFrameReady();
  return true;
}

auto MakeOpenClScopeImageResource(const opencl::OpenClImage& image)
    -> std::shared_ptr<scope::opencl_detail::OpenClLinearImageResource> {
  if (image.Empty() || image.Type() != CV_32FC4 || image.Buffer() == nullptr) {
    return {};
  }

  CheckOpenClFrameCopy(clRetainMemObject(image.Buffer()), "clRetainMemObject(scope frame)");
  auto resource           = std::make_shared<scope::opencl_detail::OpenClLinearImageResource>();
  resource->buffer        = image.Buffer();
  resource->row_bytes     = image.RowBytes();
  resource->width         = image.Width();
  resource->height        = image.Height();
  resource->format        = FramePixelFormat::RGBA32F;
  resource->owns_memory   = true;
  resource->native_object = reinterpret_cast<std::uintptr_t>(image.Buffer());
  return resource;
}

void SubmitOpenClFrameForScope(const opencl::OpenClImage& image, IFrameSink& frame_sink,
                               const ViewerDisplayConfig& display_config) {
  auto final_image = MakeOpenClScopeImageResource(image);
  if (!final_image) {
    return;
  }

  frame_sink.SubmitFinalDisplayFrame(FinalDisplayFrameView{
      SharedGpuImageHandle{GpuBackend::OpenCL,
                           std::shared_ptr<void>(final_image, final_image.get()), image.Width(),
                           image.Height(), image.RowBytes(), FramePixelFormat::RGBA32F},
      image.Width(),
      image.Height(),
      FramePixelFormat::RGBA32F,
      display_config,
      AnalysisDomain::DisplayEncoded,
      {},
      0});
}

class OpenCLGPUPipeline final : public GPUPipelineImpl {
 private:
  std::shared_ptr<ImageBuffer>           input_img_;
  OperatorParams*                        cpu_params_      = nullptr;
  IFrameSink*                            frame_sink_      = nullptr;
  FusedOperatorParams                    fused_params_    = {};
  OpenCL::Pipeline::OpenClFusedResources resources_       = {};

  cl_kernel                              fused_kernel_    = nullptr;
  cl_kernel                              fused_stage_kernel_ = nullptr;
  cl_kernel                              validate_kernel_ = nullptr;
  cl_kernel                              blur_h_kernel_   = nullptr;
  cl_kernel                              apply_v_kernel_  = nullptr;
  cl_kernel                              hs_base_h_kernel_ = nullptr;
  cl_kernel                              hs_base_v_kernel_ = nullptr;
  cl_kernel                              hs_apply_kernel_  = nullptr;

  opencl::OpenClImage                    working_;
  opencl::OpenClImage                    pre_hs_working_;
  opencl::OpenClImage                    hs_working_;
  opencl::OpenClImage                    blur_horizontal_;
  opencl::OpenClImage                    detail_scratch_;

  cl_mem                                 hs_base_log_ = nullptr;
  cl_mem                                 hs_temp_log_ = nullptr;
  size_t                                 hs_allocated_elems_ = 0;
  int                                    hs_cached_width_ = 0;
  int                                    hs_cached_height_ = 0;
  int                                    hs_cached_pitch_ = 0;
  std::uint64_t                          hs_cached_key_ = 0;
  bool                                   hs_cached_reference_base_ = false;

  void                                   ReleaseHsBaseBuffers() {
    if (hs_base_log_ != nullptr) {
      clReleaseMemObject(hs_base_log_);
      hs_base_log_ = nullptr;
    }
    if (hs_temp_log_ != nullptr) {
      clReleaseMemObject(hs_temp_log_);
      hs_temp_log_ = nullptr;
    }
    hs_allocated_elems_ = 0;
    hs_cached_width_ = 0;
    hs_cached_height_ = 0;
    hs_cached_pitch_ = 0;
    hs_cached_key_ = 0;
    hs_cached_reference_base_ = false;
  }

  void                                   EnsureHsBaseBuffers(int width, int height) {
    if (width <= 0 || height <= 0) {
      throw std::runtime_error("OpenCL fused pipeline: invalid H/S base dimensions.");
    }

    const size_t needed = static_cast<size_t>(width) * static_cast<size_t>(height);
    if (needed <= hs_allocated_elems_) {
      return;
    }

    ReleaseHsBaseBuffers();

    auto&  context = OpenClContext::Instance();
    cl_int err     = CL_SUCCESS;
    hs_base_log_ =
        clCreateBuffer(context.Context(), CL_MEM_READ_WRITE, needed * sizeof(float), nullptr, &err);
    if (err != CL_SUCCESS || hs_base_log_ == nullptr) {
      throw std::runtime_error("OpenCL fused pipeline: failed to allocate H/S base buffer.");
    }
    hs_temp_log_ =
        clCreateBuffer(context.Context(), CL_MEM_READ_WRITE, needed * sizeof(float), nullptr, &err);
    if (err != CL_SUCCESS || hs_temp_log_ == nullptr) {
      ReleaseHsBaseBuffers();
      throw std::runtime_error("OpenCL fused pipeline: failed to allocate H/S temp buffer.");
    }
    hs_allocated_elems_ = needed;
  }

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

  void EnsureFusedKernels() {
    auto&      library = OpenClProgramLibrary::Instance();
    cl_program program = library.GetProgram(OpenCL::Pipeline::kFusedProgramName);
    if (program == nullptr) {
      throw std::runtime_error("OpenCL fused pipeline: failed to get fused program from library.");
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

    if (fused_stage_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      fused_stage_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kFusedStageKernelName, &err);
      if (err != CL_SUCCESS || fused_stage_kernel_ == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create kernel '" +
                                 std::string(OpenCL::Pipeline::kFusedStageKernelName) +
                                 "' with error " + std::to_string(err) + ".");
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

  void EnsureDetailKernels() {
    auto&      library = OpenClProgramLibrary::Instance();
    cl_program program = library.GetProgram(OpenCL::Pipeline::kDetailProgramName);
    if (program == nullptr) {
      throw std::runtime_error("OpenCL fused pipeline: failed to get detail program from library.");
    }

    if (blur_h_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      blur_h_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kNeighborBlurHorizontalKernelName, &err);
      if (err != CL_SUCCESS || blur_h_kernel_ == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create kernel '" +
                                 std::string(OpenCL::Pipeline::kNeighborBlurHorizontalKernelName) +
                                 "' with error " + std::to_string(err) + ".");
      }
    }

    if (apply_v_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      apply_v_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kNeighborApplyVerticalKernelName, &err);
      if (err != CL_SUCCESS || apply_v_kernel_ == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create kernel '" +
                                 std::string(OpenCL::Pipeline::kNeighborApplyVerticalKernelName) +
                                 "' with error " + std::to_string(err) + ".");
      }
    }

    if (hs_base_h_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      hs_base_h_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kHsBuildLogBaseHorizontalKernelName, &err);
      if (err != CL_SUCCESS || hs_base_h_kernel_ == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create kernel '" +
                                 std::string(OpenCL::Pipeline::kHsBuildLogBaseHorizontalKernelName) +
                                 "' with error " + std::to_string(err) + ".");
      }
    }

    if (hs_base_v_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      hs_base_v_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kHsBuildLogBaseVerticalKernelName, &err);
      if (err != CL_SUCCESS || hs_base_v_kernel_ == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create kernel '" +
                                 std::string(OpenCL::Pipeline::kHsBuildLogBaseVerticalKernelName) +
                                 "' with error " + std::to_string(err) + ".");
      }
    }

    if (hs_apply_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      hs_apply_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kHsApplyLocalToneKernelName, &err);
      if (err != CL_SUCCESS || hs_apply_kernel_ == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create kernel '" +
                                 std::string(OpenCL::Pipeline::kHsApplyLocalToneKernelName) +
                                 "' with error " + std::to_string(err) + ".");
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

  void EnqueueFusedStageKernel(const opencl::OpenClImage& src, opencl::OpenClImage& dst,
                               int stage) {
    auto& context = OpenClContext::Instance();
    if (!context.IsInitialized()) {
      throw std::runtime_error("OpenCL fused pipeline: context is not initialized.");
    }

    dst.Create(src.Width(), src.Height(), src.Type());

    cl_int  err            = CL_SUCCESS;
    cl_uint arg_index      = 0;
    cl_mem  src_buffer     = src.Buffer();
    cl_mem  dst_buffer     = dst.Buffer();
    cl_mem  params_buffer  = resources_.params_buffer_.Get();
    cl_mem  lmt_lut_buffer = resources_.lmt_lut_buffer_.Get();
    cl_int  width          = src.Width();
    cl_int  height         = src.Height();
    cl_int  stage_arg      = stage;

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

    err |= clSetKernelArg(fused_stage_kernel_, arg_index++, sizeof(cl_mem), &src_buffer);
    err |= clSetKernelArg(fused_stage_kernel_, arg_index++, sizeof(cl_mem), &dst_buffer);
    err |= clSetKernelArg(fused_stage_kernel_, arg_index++, sizeof(cl_mem), &params_buffer);
    err |= clSetKernelArg(fused_stage_kernel_, arg_index++, sizeof(cl_mem), &lmt_lut_buffer);
    err |= clSetKernelArg(fused_stage_kernel_, arg_index++, sizeof(cl_int), &width);
    err |= clSetKernelArg(fused_stage_kernel_, arg_index++, sizeof(cl_int), &height);
    err |= clSetKernelArg(fused_stage_kernel_, arg_index++, sizeof(cl_int), &stage_arg);

    if (err != CL_SUCCESS) {
      if (fallback_lut != nullptr) clReleaseMemObject(fallback_lut);
      throw std::runtime_error("OpenCL fused pipeline: failed to set fused stage arguments.");
    }

    size_t global_size[2] = {static_cast<size_t>(width), static_cast<size_t>(height)};
    err = clEnqueueNDRangeKernel(context.Queue(), fused_stage_kernel_, 2, nullptr, global_size,
                                 nullptr, 0, nullptr, nullptr);

    if (fallback_lut != nullptr) {
      clReleaseMemObject(fallback_lut);
    }

    if (err != CL_SUCCESS) {
      throw std::runtime_error("OpenCL fused pipeline: failed to enqueue fused stage with error " +
                               std::to_string(err) + ".");
    }
  }

  void EnqueueNeighborBlurHorizontal(const opencl::OpenClImage& src, opencl::OpenClImage& dst,
                                     cl_mem stage_buffer) {
    auto& context = OpenClContext::Instance();

    dst.Create(src.Width(), src.Height(), src.Type());

    cl_int  err       = CL_SUCCESS;
    cl_uint arg_index = 0;
    cl_mem  src_buf   = src.Buffer();
    cl_mem  dst_buf   = dst.Buffer();
    cl_int  width     = src.Width();
    cl_int  height    = src.Height();

    err |= clSetKernelArg(blur_h_kernel_, arg_index++, sizeof(cl_mem), &src_buf);
    err |= clSetKernelArg(blur_h_kernel_, arg_index++, sizeof(cl_mem), &dst_buf);
    err |= clSetKernelArg(blur_h_kernel_, arg_index++, sizeof(cl_mem), &stage_buffer);
    err |= clSetKernelArg(blur_h_kernel_, arg_index++, sizeof(cl_int), &width);
    err |= clSetKernelArg(blur_h_kernel_, arg_index++, sizeof(cl_int), &height);

    if (err != CL_SUCCESS) {
      throw std::runtime_error(
          "OpenCL fused pipeline: failed to set blur horizontal kernel arguments.");
    }

    size_t global_size[2] = {static_cast<size_t>(width), static_cast<size_t>(height)};
    err = clEnqueueNDRangeKernel(context.Queue(), blur_h_kernel_, 2, nullptr, global_size, nullptr,
                                 0, nullptr, nullptr);
    if (err != CL_SUCCESS) {
      throw std::runtime_error(
          "OpenCL fused pipeline: failed to enqueue blur horizontal kernel with error " +
          std::to_string(err) + ".");
    }
  }

  void EnqueueNeighborApplyVertical(const opencl::OpenClImage& src,
                                    const opencl::OpenClImage& blur_horizontal,
                                    opencl::OpenClImage& dst, cl_mem stage_buffer) {
    auto& context = OpenClContext::Instance();

    dst.Create(src.Width(), src.Height(), src.Type());

    cl_int  err       = CL_SUCCESS;
    cl_uint arg_index = 0;
    cl_mem  src_buf   = src.Buffer();
    cl_mem  blur_buf  = blur_horizontal.Buffer();
    cl_mem  dst_buf   = dst.Buffer();
    cl_int  width     = src.Width();
    cl_int  height    = src.Height();

    err |= clSetKernelArg(apply_v_kernel_, arg_index++, sizeof(cl_mem), &src_buf);
    err |= clSetKernelArg(apply_v_kernel_, arg_index++, sizeof(cl_mem), &blur_buf);
    err |= clSetKernelArg(apply_v_kernel_, arg_index++, sizeof(cl_mem), &dst_buf);
    err |= clSetKernelArg(apply_v_kernel_, arg_index++, sizeof(cl_mem), &stage_buffer);
    err |= clSetKernelArg(apply_v_kernel_, arg_index++, sizeof(cl_int), &width);
    err |= clSetKernelArg(apply_v_kernel_, arg_index++, sizeof(cl_int), &height);

    if (err != CL_SUCCESS) {
      throw std::runtime_error(
          "OpenCL fused pipeline: failed to set neighbor apply vertical kernel arguments.");
    }

    size_t global_size[2] = {static_cast<size_t>(width), static_cast<size_t>(height)};
    err = clEnqueueNDRangeKernel(context.Queue(), apply_v_kernel_, 2, nullptr, global_size, nullptr,
                                 0, nullptr, nullptr);
    if (err != CL_SUCCESS) {
      throw std::runtime_error(
          "OpenCL fused pipeline: failed to enqueue neighbor apply vertical kernel with error " +
          std::to_string(err) + ".");
    }
  }

  void EnqueueHsBuildLogBaseHorizontal(const opencl::OpenClImage& src) {
    auto& context = OpenClContext::Instance();

    cl_int  err           = CL_SUCCESS;
    cl_uint arg_index     = 0;
    cl_mem  src_buf       = src.Buffer();
    cl_mem  dst_buf       = hs_temp_log_;
    cl_mem  params_buffer = resources_.params_buffer_.Get();
    cl_int  width         = src.Width();
    cl_int  height        = src.Height();

    err |= clSetKernelArg(hs_base_h_kernel_, arg_index++, sizeof(cl_mem), &src_buf);
    err |= clSetKernelArg(hs_base_h_kernel_, arg_index++, sizeof(cl_mem), &dst_buf);
    err |= clSetKernelArg(hs_base_h_kernel_, arg_index++, sizeof(cl_mem), &params_buffer);
    err |= clSetKernelArg(hs_base_h_kernel_, arg_index++, sizeof(cl_int), &width);
    err |= clSetKernelArg(hs_base_h_kernel_, arg_index++, sizeof(cl_int), &height);
    if (err != CL_SUCCESS) {
      throw std::runtime_error(
          "OpenCL fused pipeline: failed to set H/S base horizontal arguments.");
    }

    size_t global_size[2] = {static_cast<size_t>(width), static_cast<size_t>(height)};
    err = clEnqueueNDRangeKernel(context.Queue(), hs_base_h_kernel_, 2, nullptr, global_size,
                                 nullptr, 0, nullptr, nullptr);
    if (err != CL_SUCCESS) {
      throw std::runtime_error(
          "OpenCL fused pipeline: failed to enqueue H/S base horizontal kernel with error " +
          std::to_string(err) + ".");
    }
  }

  void EnqueueHsBuildLogBaseVertical(const opencl::OpenClImage& guidance) {
    auto& context = OpenClContext::Instance();

    cl_int  err           = CL_SUCCESS;
    cl_uint arg_index     = 0;
    cl_mem  guidance_buf  = guidance.Buffer();
    cl_mem  src_buf       = hs_temp_log_;
    cl_mem  dst_buf       = hs_base_log_;
    cl_mem  params_buffer = resources_.params_buffer_.Get();
    cl_int  width         = guidance.Width();
    cl_int  height        = guidance.Height();

    err |= clSetKernelArg(hs_base_v_kernel_, arg_index++, sizeof(cl_mem), &guidance_buf);
    err |= clSetKernelArg(hs_base_v_kernel_, arg_index++, sizeof(cl_mem), &src_buf);
    err |= clSetKernelArg(hs_base_v_kernel_, arg_index++, sizeof(cl_mem), &dst_buf);
    err |= clSetKernelArg(hs_base_v_kernel_, arg_index++, sizeof(cl_mem), &params_buffer);
    err |= clSetKernelArg(hs_base_v_kernel_, arg_index++, sizeof(cl_int), &width);
    err |= clSetKernelArg(hs_base_v_kernel_, arg_index++, sizeof(cl_int), &height);
    if (err != CL_SUCCESS) {
      throw std::runtime_error(
          "OpenCL fused pipeline: failed to set H/S base vertical arguments.");
    }

    size_t global_size[2] = {static_cast<size_t>(width), static_cast<size_t>(height)};
    err = clEnqueueNDRangeKernel(context.Queue(), hs_base_v_kernel_, 2, nullptr, global_size,
                                 nullptr, 0, nullptr, nullptr);
    if (err != CL_SUCCESS) {
      throw std::runtime_error(
          "OpenCL fused pipeline: failed to enqueue H/S base vertical kernel with error " +
          std::to_string(err) + ".");
    }
  }

  void EnqueueHsApplyLocalTone(const opencl::OpenClImage& src, opencl::OpenClImage& dst,
                               bool use_reference_base) {
    auto& context = OpenClContext::Instance();

    dst.Create(src.Width(), src.Height(), src.Type());

    cl_int  err              = CL_SUCCESS;
    cl_uint arg_index        = 0;
    cl_mem  src_buf          = src.Buffer();
    cl_mem  base_buf         = hs_base_log_;
    cl_mem  dst_buf          = dst.Buffer();
    cl_mem  params_buffer    = resources_.params_buffer_.Get();
    cl_int  width            = src.Width();
    cl_int  height           = src.Height();
    cl_int  base_width       = hs_cached_width_;
    cl_int  base_height      = hs_cached_height_;
    cl_int  base_pitch_elems = hs_cached_pitch_;
    cl_int  use_reference    = use_reference_base ? 1 : 0;

    err |= clSetKernelArg(hs_apply_kernel_, arg_index++, sizeof(cl_mem), &src_buf);
    err |= clSetKernelArg(hs_apply_kernel_, arg_index++, sizeof(cl_mem), &base_buf);
    err |= clSetKernelArg(hs_apply_kernel_, arg_index++, sizeof(cl_mem), &dst_buf);
    err |= clSetKernelArg(hs_apply_kernel_, arg_index++, sizeof(cl_mem), &params_buffer);
    err |= clSetKernelArg(hs_apply_kernel_, arg_index++, sizeof(cl_int), &width);
    err |= clSetKernelArg(hs_apply_kernel_, arg_index++, sizeof(cl_int), &height);
    err |= clSetKernelArg(hs_apply_kernel_, arg_index++, sizeof(cl_int), &base_width);
    err |= clSetKernelArg(hs_apply_kernel_, arg_index++, sizeof(cl_int), &base_height);
    err |= clSetKernelArg(hs_apply_kernel_, arg_index++, sizeof(cl_int), &base_pitch_elems);
    err |= clSetKernelArg(hs_apply_kernel_, arg_index++, sizeof(cl_int), &use_reference);
    if (err != CL_SUCCESS) {
      throw std::runtime_error("OpenCL fused pipeline: failed to set H/S apply arguments.");
    }

    size_t global_size[2] = {static_cast<size_t>(width), static_cast<size_t>(height)};
    err = clEnqueueNDRangeKernel(context.Queue(), hs_apply_kernel_, 2, nullptr, global_size,
                                 nullptr, 0, nullptr, nullptr);
    if (err != CL_SUCCESS) {
      throw std::runtime_error("OpenCL fused pipeline: failed to enqueue H/S apply kernel with error " +
                               std::to_string(err) + ".");
    }
  }

  auto ShouldRunHighlightShadowLocalTone() const -> bool {
    if (!fused_params_.hs_local_tone_enabled_ || fused_params_.hs_base_gaussian_tap_count_ <= 0) {
      return false;
    }
    const float shadow_amount =
        fused_params_.shadows_enabled_ ? std::clamp(fused_params_.shadows_offset_, -1.0f, 1.0f)
                                       : 0.0f;
    const float highlight_amount =
        fused_params_.highlights_enabled_
            ? std::clamp(-fused_params_.highlights_offset_ * 0.5f, -1.0f, 1.0f)
            : 0.0f;
    return std::abs(shadow_amount) > 1.0e-6f || std::abs(highlight_amount) > 1.0e-6f;
  }

  void EnqueueHighlightShadowLocalTone(const opencl::OpenClImage& src, opencl::OpenClImage& dst) {
    const bool roi_frame_with_source_reference = fused_params_.render_roi_enabled_ &&
                                                 fused_params_.render_roi_reference_width_ > 0 &&
                                                 fused_params_.render_roi_reference_height_ > 0;
    const bool reference_base_cache_valid =
        hs_cached_reference_base_ && hs_base_log_ != nullptr &&
        hs_cached_key_ == fused_params_.hs_mask_base_cache_key_ && hs_cached_width_ > 0 &&
        hs_cached_height_ > 0 && hs_cached_pitch_ > 0;

    if (roi_frame_with_source_reference && reference_base_cache_valid) {
      EnqueueHsApplyLocalTone(src, dst, true);
      return;
    }
    if (!roi_frame_with_source_reference && reference_base_cache_valid &&
        (hs_cached_width_ > src.Width() || hs_cached_height_ > src.Height())) {
      EnqueueHsApplyLocalTone(src, dst, true);
      return;
    }

    EnsureHsBaseBuffers(src.Width(), src.Height());
    const bool cache_valid = !roi_frame_with_source_reference && reference_base_cache_valid &&
                             hs_cached_width_ == src.Width() && hs_cached_height_ == src.Height() &&
                             hs_cached_pitch_ == src.Width();
    if (!cache_valid) {
      EnqueueHsBuildLogBaseHorizontal(src);
      EnqueueHsBuildLogBaseVertical(src);
      hs_cached_key_ = fused_params_.hs_mask_base_cache_key_;
      hs_cached_width_ = src.Width();
      hs_cached_height_ = src.Height();
      hs_cached_pitch_ = src.Width();
      hs_cached_reference_base_ = !roi_frame_with_source_reference;
    }

    EnqueueHsApplyLocalTone(src, dst, false);
  }

  auto ShouldRunSharpen() const -> bool {
    return fused_params_.sharpen_enabled_ && fused_params_.sharpen_offset_ != 0.0f &&
           fused_params_.sharpen_radius_ > 0.0f;
  }

  auto ShouldRunClarity() const -> bool {
    return fused_params_.clarity_enabled_ && fused_params_.clarity_offset_ != 0.0f &&
           fused_params_.clarity_radius_ > 0.0f;
  }

  auto BuildNeighborStages() const -> std::vector<OpenClNeighborStage> {
    std::vector<OpenClNeighborStage> stages;
    stages.reserve(2);

    if (ShouldRunSharpen()) {
      stages.push_back(OpenClNeighborStage{BuildNeighborStageParams(
          OpenClNeighborOpKind::Sharpen, fused_params_.sharpen_radius_,
          fused_params_.sharpen_offset_, fused_params_.sharpen_threshold_,
          fused_params_.sharpen_gaussian_tap_count_, fused_params_.sharpen_gaussian_weights_)});
    }
    if (ShouldRunClarity()) {
      stages.push_back(OpenClNeighborStage{BuildNeighborStageParams(
          OpenClNeighborOpKind::Clarity, fused_params_.clarity_radius_,
          fused_params_.clarity_offset_, 0.0f, fused_params_.clarity_gaussian_tap_count_,
          fused_params_.clarity_gaussian_weights_)});
    }

    return stages;
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
    using ProfileClock    = std::chrono::steady_clock;
    const auto exec_start = ProfileClock::now();

    if (!cpu_params_) {
      throw std::runtime_error("OpenCL fused pipeline: parameters were not set.");
    }

    double ensure_input_ms     = 0.0;
    double ensure_kernels_ms   = 0.0;
    double validate_abi_ms     = 0.0;
    double fused_kernel_ms     = 0.0;
    double detail_ms           = 0.0;
    double sync_ms             = 0.0;
    double download_ms         = 0.0;
    double submit_ms           = 0.0;
    bool   submitted_gpu_frame = false;

    {
      const auto t0 = ProfileClock::now();
      EnsureOpenClInput();
      ensure_input_ms = std::chrono::duration<double, std::milli>(ProfileClock::now() - t0).count();
    }

    {
      const auto t0 = ProfileClock::now();
      EnsureFusedKernels();
      ValidateParamsABI();
      validate_abi_ms = std::chrono::duration<double, std::milli>(ProfileClock::now() - t0).count();
    }

    const auto neighbor_stages = BuildNeighborStages();
    const bool run_hs_local_tone = ShouldRunHighlightShadowLocalTone();

    {
      const auto t0 = ProfileClock::now();
      if (run_hs_local_tone || !neighbor_stages.empty()) {
        EnsureDetailKernels();
      }
      ensure_kernels_ms =
          std::chrono::duration<double, std::milli>(ProfileClock::now() - t0).count();
    }

    const auto& input = input_img_->GetOpenClImage();

    {
      const auto t0 = ProfileClock::now();
      if (run_hs_local_tone) {
        EnqueueFusedStageKernel(input, pre_hs_working_, 1);
        EnqueueHighlightShadowLocalTone(pre_hs_working_, hs_working_);
        EnqueueFusedStageKernel(hs_working_, working_, 2);
      } else {
        EnqueueFusedKernel(input);
      }
      fused_kernel_ms = std::chrono::duration<double, std::milli>(ProfileClock::now() - t0).count();
    }

    opencl::OpenClImage*                        detail_src = &working_;
    opencl::OpenClImage*                        detail_dst = &detail_scratch_;

    std::vector<OpenCL::Pipeline::OpenClBuffer> stage_buffers;
    stage_buffers.reserve(neighbor_stages.size());

    {
      const auto t0 = ProfileClock::now();
      for (const auto& stage : neighbor_stages) {
        stage_buffers.push_back(UploadStageParams(stage.params_));
        cl_mem stage_buffer = stage_buffers.back().Get();

        EnqueueNeighborBlurHorizontal(*detail_src, blur_horizontal_, stage_buffer);
        EnqueueNeighborApplyVertical(*detail_src, blur_horizontal_, *detail_dst, stage_buffer);

        std::swap(detail_src, detail_dst);
      }
      detail_ms = std::chrono::duration<double, std::milli>(ProfileClock::now() - t0).count();
    }

    {
      const auto t0      = ProfileClock::now();
      auto&      context = OpenClContext::Instance();
      clFinish(context.Queue());
      sync_ms = std::chrono::duration<double, std::milli>(ProfileClock::now() - t0).count();
    }

    if (frame_sink_) {
      const ViewerDisplayConfig display_config = ResolveViewerDisplayConfig(*cpu_params_);
      submitted_gpu_frame = TrySubmitOpenClFrameToSink(*detail_src, *frame_sink_);
      if (!submitted_gpu_frame) {
        cv::Mat host_image;
        {
          const auto t0 = ProfileClock::now();
          detail_src->Download(host_image);
          download_ms = std::chrono::duration<double, std::milli>(ProfileClock::now() - t0).count();
        }

        if (host_image.type() != CV_32FC4) {
          throw std::runtime_error(
              "OpenCL fused pipeline: expected RGBA32F host frame for viewer.");
        }

        const size_t row_bytes =
            static_cast<size_t>(host_image.cols) * static_cast<size_t>(sizeof(cv::Vec4f));
        auto host_pixels = std::make_shared<std::vector<float>>(
            static_cast<size_t>(host_image.cols) * static_cast<size_t>(host_image.rows) * 4U);
        cv::Mat contiguous_host(host_image.rows, host_image.cols, CV_32FC4, host_pixels->data(),
                                row_bytes);
        host_image.copyTo(contiguous_host);

        const auto t0 = ProfileClock::now();
        frame_sink_->SubmitHostFrame(
            ViewerFrame{host_image.cols, host_image.rows, row_bytes,
                        std::shared_ptr<const void>(host_pixels, host_pixels->data()),
                        display_config, FramePresentationMode::FullFrame});
        submit_ms = std::chrono::duration<double, std::milli>(ProfileClock::now() - t0).count();
      } else {
        submit_ms = 0.0;
      }
      SubmitOpenClFrameForScope(*detail_src, *frame_sink_, display_config);
    }

    if (output_img) {
      *output_img = ImageBuffer(std::move(*detail_src));
    }

    const double total_ms =
        std::chrono::duration<double, std::milli>(ProfileClock::now() - exec_start).count();

    static int           frame_count  = 0;
    static constexpr int kLogInterval = 30;
    if (++frame_count % kLogInterval == 1) {
      std::cout << "[OpenCL Pipeline] frame=" << frame_count << " total=" << std::fixed
                << std::setprecision(2) << total_ms << " ms"
                << " | input=" << ensure_input_ms << " abi=" << validate_abi_ms
                << " kernels=" << ensure_kernels_ms << " fused=" << fused_kernel_ms
                << " detail=" << detail_ms << " sync=" << sync_ms << " download=" << download_ms
                << " submit=" << submit_ms
                << " present=" << (submitted_gpu_frame ? "direct_opengl" : "host_upload")
                << " | size=" << input.Width() << "x" << input.Height() << std::endl;
    }
  }

  void ReleaseResources() override {
    if (fused_kernel_ != nullptr) {
      clReleaseKernel(fused_kernel_);
      fused_kernel_ = nullptr;
    }
    if (fused_stage_kernel_ != nullptr) {
      clReleaseKernel(fused_stage_kernel_);
      fused_stage_kernel_ = nullptr;
    }
    if (validate_kernel_ != nullptr) {
      clReleaseKernel(validate_kernel_);
      validate_kernel_ = nullptr;
    }
    if (blur_h_kernel_ != nullptr) {
      clReleaseKernel(blur_h_kernel_);
      blur_h_kernel_ = nullptr;
    }
    if (apply_v_kernel_ != nullptr) {
      clReleaseKernel(apply_v_kernel_);
      apply_v_kernel_ = nullptr;
    }
    if (hs_base_h_kernel_ != nullptr) {
      clReleaseKernel(hs_base_h_kernel_);
      hs_base_h_kernel_ = nullptr;
    }
    if (hs_base_v_kernel_ != nullptr) {
      clReleaseKernel(hs_base_v_kernel_);
      hs_base_v_kernel_ = nullptr;
    }
    if (hs_apply_kernel_ != nullptr) {
      clReleaseKernel(hs_apply_kernel_);
      hs_apply_kernel_ = nullptr;
    }
    working_.Release();
    pre_hs_working_.Release();
    hs_working_.Release();
    blur_horizontal_.Release();
    detail_scratch_.Release();
    ReleaseHsBaseBuffers();
    resources_.Reset();
  }
};

}  // namespace

auto CreateOpenCLGPUPipeline() -> std::unique_ptr<GPUPipelineImpl> {
  return std::make_unique<OpenCLGPUPipeline>();
}

}  // namespace alcedo

#endif
