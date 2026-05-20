//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifdef HAVE_OPENCL

#include "opencl/opencl_geometry_utils.hpp"

#include <algorithm>
#include <stdexcept>
#include <string>

#include "opencl/opencl_context.hpp"
#include "opencl/opencl_geometry_programs.hpp"
#include "opencl/opencl_program_library.hpp"

namespace alcedo::OpenCL::Geometry {
namespace {

void Check(cl_int error, const char* operation) {
  if (error != CL_SUCCESS) {
    throw std::runtime_error(std::string("OpenCL geometry: ") + operation + " failed with error " +
                             std::to_string(error) + ".");
  }
}

class KernelHandle {
 public:
  explicit KernelHandle(const char* kernel_name) {
    cl_int error = CL_SUCCESS;
    kernel_      = clCreateKernel(OpenClProgramLibrary::Instance().GetProgram(kGeometryProgramName),
                                  kernel_name, &error);
    Check(error, "clCreateKernel");
    if (kernel_ == nullptr) {
      throw std::runtime_error("OpenCL geometry: clCreateKernel returned a null kernel.");
    }
  }

  ~KernelHandle() {
    if (kernel_ != nullptr) {
      clReleaseKernel(kernel_);
    }
  }

  KernelHandle(const KernelHandle&)                    = delete;
  auto operator=(const KernelHandle&) -> KernelHandle& = delete;

  auto Get() const -> cl_kernel { return kernel_; }

 private:
  cl_kernel kernel_ = nullptr;
};

auto ChannelCountFromType(int type) -> int {
  const int channels = CV_MAT_CN(type);
  if (CV_MAT_DEPTH(type) != CV_32F || (channels != 1 && channels != 3 && channels != 4)) {
    throw std::runtime_error("OpenCL geometry: expected CV_32FC1, CV_32FC3, or CV_32FC4 image.");
  }
  return channels;
}

template <typename T>
void SetKernelArg(cl_kernel kernel, cl_uint index, const T& value) {
  Check(clSetKernelArg(kernel, index, sizeof(T), &value), "clSetKernelArg");
}

void Enqueue2D(cl_kernel kernel, int width, int height) {
  if (width <= 0 || height <= 0) {
    return;
  }
  auto&        context        = OpenClContext::Instance();
  const size_t global_size[2] = {static_cast<size_t>(width), static_cast<size_t>(height)};
  Check(clEnqueueNDRangeKernel(context.Queue(), kernel, 2, nullptr, global_size, nullptr, 0,
                               nullptr, nullptr),
        "clEnqueueNDRangeKernel");
  Check(clFinish(context.Queue()), "clFinish");
}

void ValidateDstSize(cv::Size dst_size, const char* operation) {
  if (dst_size.width <= 0 || dst_size.height <= 0) {
    throw std::runtime_error(std::string(operation) + ": destination size must be positive");
  }
}

void ValidateCropRect(const opencl::OpenClImage& src, const cv::Rect& crop_rect) {
  if (crop_rect.width <= 0 || crop_rect.height <= 0) {
    throw std::runtime_error("OpenCL geometry: crop rectangle must be non-empty.");
  }
  if (crop_rect.x < 0 || crop_rect.y < 0 || crop_rect.x + crop_rect.width > src.Width() ||
      crop_rect.y + crop_rect.height > src.Height()) {
    throw std::runtime_error("OpenCL geometry: crop rectangle is out of bounds.");
  }
}

void CropResizeWithKernel(const opencl::OpenClImage& src, opencl::OpenClImage& dst,
                          const cv::Rect& crop_rect, cv::Size dst_size, const char* kernel_name) {
  if (src.Empty()) {
    dst.Release();
    return;
  }
  ValidateDstSize(dst_size, "OpenCL geometry crop resize");
  ValidateCropRect(src, crop_rect);

  const int  channels            = ChannelCountFromType(src.Type());
  const bool is_full_source_crop = crop_rect.x == 0 && crop_rect.y == 0 &&
                                   crop_rect.width == src.Width() &&
                                   crop_rect.height == src.Height();
  if (is_full_source_crop && src.Width() == dst_size.width && src.Height() == dst_size.height) {
    src.CopyTo(dst);
    return;
  }

  dst.Create(dst_size.width, dst_size.height, src.Type());
  const float  scale_x = static_cast<float>(crop_rect.width) / static_cast<float>(dst_size.width);
  const float  scale_y = static_cast<float>(crop_rect.height) / static_cast<float>(dst_size.height);
  const int    src_stride = static_cast<int>(src.RowBytes() / sizeof(float));
  const int    dst_stride = static_cast<int>(dst.RowBytes() / sizeof(float));

  KernelHandle kernel(kernel_name);
  cl_mem       src_buffer = src.Buffer();
  cl_mem       dst_buffer = dst.Buffer();
  SetKernelArg(kernel.Get(), 0, src_buffer);
  SetKernelArg(kernel.Get(), 1, src.Width());
  SetKernelArg(kernel.Get(), 2, src.Height());
  SetKernelArg(kernel.Get(), 3, src_stride);
  SetKernelArg(kernel.Get(), 4, channels);
  SetKernelArg(kernel.Get(), 5, crop_rect.x);
  SetKernelArg(kernel.Get(), 6, crop_rect.y);
  SetKernelArg(kernel.Get(), 7, crop_rect.width);
  SetKernelArg(kernel.Get(), 8, crop_rect.height);
  SetKernelArg(kernel.Get(), 9, dst_buffer);
  SetKernelArg(kernel.Get(), 10, dst_size.width);
  SetKernelArg(kernel.Get(), 11, dst_size.height);
  SetKernelArg(kernel.Get(), 12, dst_stride);
  SetKernelArg(kernel.Get(), 13, scale_x);
  SetKernelArg(kernel.Get(), 14, scale_y);
  Enqueue2D(kernel.Get(), dst_size.width, dst_size.height);
}

}  // namespace

void Resize(const opencl::OpenClImage& src, opencl::OpenClImage& dst, cv::Size dst_size,
            ResizeDownsampleAlgorithm downsample_algorithm) {
  CropResize(src, dst, cv::Rect(0, 0, src.Width(), src.Height()), dst_size, downsample_algorithm);
}

void CropResize(const opencl::OpenClImage& src, opencl::OpenClImage& dst, const cv::Rect& crop_rect,
                cv::Size dst_size, ResizeDownsampleAlgorithm downsample_algorithm) {
  if (src.Empty()) {
    dst.Release();
    return;
  }
  ValidateDstSize(dst_size, "OpenCL geometry crop resize");
  ValidateCropRect(src, crop_rect);

  if (crop_rect.width <= dst_size.width || crop_rect.height <= dst_size.height ||
      downsample_algorithm == ResizeDownsampleAlgorithm::Bilinear) {
    CropResizeWithKernel(src, dst, crop_rect, dst_size, kCropResizeLinearKernelName);
    return;
  }
  CropResizeWithKernel(src, dst, crop_rect, dst_size, kCropResizeAreaKernelName);
}

void ResizeLinear(const opencl::OpenClImage& src, opencl::OpenClImage& dst, cv::Size dst_size) {
  CropResizeWithKernel(src, dst, cv::Rect(0, 0, src.Width(), src.Height()), dst_size,
                       kCropResizeLinearKernelName);
}

void ResizeAreaApprox(const opencl::OpenClImage& src, opencl::OpenClImage& dst, cv::Size dst_size) {
  if (src.Empty()) {
    dst.Release();
    return;
  }
  ValidateDstSize(dst_size, "OpenCL geometry area resize");

  if (src.Width() <= dst_size.width || src.Height() <= dst_size.height) {
    ResizeLinear(src, dst, dst_size);
    return;
  }
  CropResizeWithKernel(src, dst, cv::Rect(0, 0, src.Width(), src.Height()), dst_size,
                       kCropResizeAreaKernelName);
}

void WarpAffineLinear(const opencl::OpenClImage& src, opencl::OpenClImage& dst,
                      const cv::Mat& matrix, cv::Size out_size, const cv::Scalar& border_value) {
  if (src.Empty()) {
    dst.Release();
    return;
  }
  if (out_size.width <= 0 || out_size.height <= 0) {
    throw std::runtime_error("OpenCL geometry warp affine: output size must be positive");
  }

  cv::Mat matrix_32f;
  if (matrix.type() == CV_64F) {
    matrix.convertTo(matrix_32f, CV_32F);
  } else if (matrix.type() == CV_32F) {
    matrix_32f = matrix;
  } else {
    throw std::runtime_error("OpenCL geometry warp affine: matrix type must be CV_32F or CV_64F");
  }
  if (matrix_32f.rows != 2 || matrix_32f.cols != 3) {
    throw std::runtime_error("OpenCL geometry warp affine: matrix must be 2x3");
  }

  const int channels = ChannelCountFromType(src.Type());
  dst.Create(out_size.width, out_size.height, src.Type());

  const int src_stride = static_cast<int>(src.RowBytes() / sizeof(float));
  const int dst_stride = static_cast<int>(dst.RowBytes() / sizeof(float));
  cl_float4 border{};
  border.s[0] = static_cast<float>(border_value[0]);
  border.s[1] = static_cast<float>(border_value[1]);
  border.s[2] = static_cast<float>(border_value[2]);
  border.s[3] = static_cast<float>(border_value[3]);

  KernelHandle kernel(kWarpAffineLinearKernelName);
  cl_mem       src_buffer = src.Buffer();
  cl_mem       dst_buffer = dst.Buffer();
  SetKernelArg(kernel.Get(), 0, src_buffer);
  SetKernelArg(kernel.Get(), 1, src.Width());
  SetKernelArg(kernel.Get(), 2, src.Height());
  SetKernelArg(kernel.Get(), 3, src_stride);
  SetKernelArg(kernel.Get(), 4, channels);
  SetKernelArg(kernel.Get(), 5, dst_buffer);
  SetKernelArg(kernel.Get(), 6, out_size.width);
  SetKernelArg(kernel.Get(), 7, out_size.height);
  SetKernelArg(kernel.Get(), 8, dst_stride);
  SetKernelArg(kernel.Get(), 9, matrix_32f.at<float>(0, 0));
  SetKernelArg(kernel.Get(), 10, matrix_32f.at<float>(0, 1));
  SetKernelArg(kernel.Get(), 11, matrix_32f.at<float>(0, 2));
  SetKernelArg(kernel.Get(), 12, matrix_32f.at<float>(1, 0));
  SetKernelArg(kernel.Get(), 13, matrix_32f.at<float>(1, 1));
  SetKernelArg(kernel.Get(), 14, matrix_32f.at<float>(1, 2));
  SetKernelArg(kernel.Get(), 15, border);
  Enqueue2D(kernel.Get(), out_size.width, out_size.height);
}

void Rotate180(opencl::OpenClImage& image) {
  if (image.Empty()) {
    return;
  }
  opencl::OpenClImage dst;
  dst.Create(image.Width(), image.Height(), image.Type());
  const int     channels   = ChannelCountFromType(image.Type());
  const int     src_stride = static_cast<int>(image.RowBytes() / sizeof(float));
  const int     dst_stride = static_cast<int>(dst.RowBytes() / sizeof(float));

  KernelHandle  kernel(kRotateKernelName);
  cl_mem        src_buffer = image.Buffer();
  cl_mem        dst_buffer = dst.Buffer();
  constexpr int mode       = 0;
  SetKernelArg(kernel.Get(), 0, src_buffer);
  SetKernelArg(kernel.Get(), 1, image.Width());
  SetKernelArg(kernel.Get(), 2, image.Height());
  SetKernelArg(kernel.Get(), 3, src_stride);
  SetKernelArg(kernel.Get(), 4, channels);
  SetKernelArg(kernel.Get(), 5, dst_buffer);
  SetKernelArg(kernel.Get(), 6, dst.Width());
  SetKernelArg(kernel.Get(), 7, dst.Height());
  SetKernelArg(kernel.Get(), 8, dst_stride);
  SetKernelArg(kernel.Get(), 9, mode);
  Enqueue2D(kernel.Get(), dst.Width(), dst.Height());
  image = std::move(dst);
}

void Rotate90CW(opencl::OpenClImage& image) {
  if (image.Empty()) {
    return;
  }
  opencl::OpenClImage dst;
  dst.Create(image.Height(), image.Width(), image.Type());
  const int     channels   = ChannelCountFromType(image.Type());
  const int     src_stride = static_cast<int>(image.RowBytes() / sizeof(float));
  const int     dst_stride = static_cast<int>(dst.RowBytes() / sizeof(float));

  KernelHandle  kernel(kRotateKernelName);
  cl_mem        src_buffer = image.Buffer();
  cl_mem        dst_buffer = dst.Buffer();
  constexpr int mode       = 1;
  SetKernelArg(kernel.Get(), 0, src_buffer);
  SetKernelArg(kernel.Get(), 1, image.Width());
  SetKernelArg(kernel.Get(), 2, image.Height());
  SetKernelArg(kernel.Get(), 3, src_stride);
  SetKernelArg(kernel.Get(), 4, channels);
  SetKernelArg(kernel.Get(), 5, dst_buffer);
  SetKernelArg(kernel.Get(), 6, dst.Width());
  SetKernelArg(kernel.Get(), 7, dst.Height());
  SetKernelArg(kernel.Get(), 8, dst_stride);
  SetKernelArg(kernel.Get(), 9, mode);
  Enqueue2D(kernel.Get(), dst.Width(), dst.Height());
  image = std::move(dst);
}

void Rotate90CCW(opencl::OpenClImage& image) {
  if (image.Empty()) {
    return;
  }
  opencl::OpenClImage dst;
  dst.Create(image.Height(), image.Width(), image.Type());
  const int     channels   = ChannelCountFromType(image.Type());
  const int     src_stride = static_cast<int>(image.RowBytes() / sizeof(float));
  const int     dst_stride = static_cast<int>(dst.RowBytes() / sizeof(float));

  KernelHandle  kernel(kRotateKernelName);
  cl_mem        src_buffer = image.Buffer();
  cl_mem        dst_buffer = dst.Buffer();
  constexpr int mode       = 2;
  SetKernelArg(kernel.Get(), 0, src_buffer);
  SetKernelArg(kernel.Get(), 1, image.Width());
  SetKernelArg(kernel.Get(), 2, image.Height());
  SetKernelArg(kernel.Get(), 3, src_stride);
  SetKernelArg(kernel.Get(), 4, channels);
  SetKernelArg(kernel.Get(), 5, dst_buffer);
  SetKernelArg(kernel.Get(), 6, dst.Width());
  SetKernelArg(kernel.Get(), 7, dst.Height());
  SetKernelArg(kernel.Get(), 8, dst_stride);
  SetKernelArg(kernel.Get(), 9, mode);
  Enqueue2D(kernel.Get(), dst.Width(), dst.Height());
  image = std::move(dst);
}

}  // namespace alcedo::OpenCL::Geometry

#endif
