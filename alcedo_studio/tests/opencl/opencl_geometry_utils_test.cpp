//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "opencl/opencl_geometry_utils.hpp"

#include <gtest/gtest.h>

#include <cmath>
#include <memory>
#include <opencv2/core.hpp>
#include <opencv2/core/cuda.hpp>
#include <opencv2/imgproc.hpp>
#include <string>

#include "decoders/processor/operators/gpu/cuda_rotate.hpp"
#include "edit/operators/geometry/crop_rotate_op.hpp"
#include "edit/operators/geometry/cuda_geometry_ops.hpp"
#include "edit/operators/geometry/resize_op.hpp"
#include "image/image_buffer.hpp"
#include "opencl/opencl_context.hpp"
#include "opencl/opencl_runtime.hpp"

namespace alcedo {
namespace {

auto EnsureOpenClRuntime() -> bool {
  if (TryPrepareOpenClRuntime()) {
    return true;
  }
  return OpenClContext::Instance().IsInitialized();
}

auto EnsureCudaDevice() -> bool {
#ifndef HAVE_CUDA
  return false;
#else
  const int device_count = cv::cuda::getCudaEnabledDeviceCount();
  if (device_count <= 0) {
    return false;
  }
  cv::cuda::setDevice(0);
  return true;
#endif
}

auto OpenClAndCudaSkipReason() -> std::string {
  if (!EnsureOpenClRuntime()) {
    const std::string error = OpenClContext::Instance().LastInitializationError();
    return error.empty() ? "OpenCL runtime is unavailable in this environment." : error;
  }
  if (!EnsureCudaDevice()) {
    return "CUDA device is unavailable in this environment.";
  }
  return {};
}

auto MakePattern(int width, int height, int type) -> cv::Mat {
  cv::Mat   image(height, width, type);
  const int channels = CV_MAT_CN(type);
  for (int y = 0; y < height; ++y) {
    for (int x = 0; x < width; ++x) {
      float* pixel = image.ptr<float>(y) + x * channels;
      for (int c = 0; c < channels; ++c) {
        pixel[c] = static_cast<float>((y * width + x) * 10 + c);
      }
    }
  }
  return image;
}

void ExpectMatNear(const cv::Mat& actual, const cv::Mat& expected, float tolerance) {
  ASSERT_EQ(actual.cols, expected.cols);
  ASSERT_EQ(actual.rows, expected.rows);
  ASSERT_EQ(actual.type(), expected.type());
  const int channels = actual.channels();
  for (int y = 0; y < actual.rows; ++y) {
    const float* actual_row   = actual.ptr<float>(y);
    const float* expected_row = expected.ptr<float>(y);
    for (int x = 0; x < actual.cols * channels; ++x) {
      ASSERT_NEAR(actual_row[x], expected_row[x], tolerance) << "row=" << y << " element=" << x;
    }
  }
}

auto RunOpenClCropResize(const cv::Mat& src, const cv::Rect& crop_rect, cv::Size dst_size,
                         ResizeDownsampleAlgorithm algorithm) -> cv::Mat {
  opencl::OpenClImage src_image;
  src_image.Upload(src);
  opencl::OpenClImage dst_image;
  OpenCL::Geometry::CropResize(src_image, dst_image, crop_rect, dst_size, algorithm);
  cv::Mat output;
  dst_image.Download(output);
  return output;
}

auto RunCudaCropResize(const cv::Mat& src, const cv::Rect& crop_rect, cv::Size dst_size,
                       ResizeDownsampleAlgorithm algorithm) -> cv::Mat {
  cv::cuda::GpuMat src_image(src);
  cv::cuda::GpuMat roi_image = src_image(crop_rect);
  cv::cuda::GpuMat dst_image;
  if (dst_size == crop_rect.size()) {
    dst_image = roi_image;
  } else if (algorithm == ResizeDownsampleAlgorithm::Bilinear) {
    CUDA::ResizeLinear(roi_image, dst_image, dst_size);
  } else {
    CUDA::ResizeAreaApprox(roi_image, dst_image, dst_size);
  }
  cv::Mat output;
  dst_image.download(output);
  return output;
}

auto RunOpenClRotate(const cv::Mat& src, int rotate_code) -> cv::Mat {
  opencl::OpenClImage image;
  image.Upload(src);
  switch (rotate_code) {
    case cv::ROTATE_180:
      OpenCL::Geometry::Rotate180(image);
      break;
    case cv::ROTATE_90_CLOCKWISE:
      OpenCL::Geometry::Rotate90CW(image);
      break;
    case cv::ROTATE_90_COUNTERCLOCKWISE:
      OpenCL::Geometry::Rotate90CCW(image);
      break;
    default:
      throw std::runtime_error("Unsupported rotate code");
  }
  cv::Mat output;
  image.Download(output);
  return output;
}

auto RunCudaRotate(const cv::Mat& src, int rotate_code) -> cv::Mat {
  cv::cuda::GpuMat image(src);
  switch (rotate_code) {
    case cv::ROTATE_180:
      CUDA::Rotate180(image);
      break;
    case cv::ROTATE_90_CLOCKWISE:
      CUDA::Rotate90CW(image);
      break;
    case cv::ROTATE_90_COUNTERCLOCKWISE:
      CUDA::Rotate90CCW(image);
      break;
    default:
      throw std::runtime_error("Unsupported rotate code");
  }
  cv::Mat output;
  image.download(output);
  return output;
}

}  // namespace

TEST(OpenClGeometryUtilsTest, CropResizeCopiesCudaRoi) {
  if (const std::string skip_reason = OpenClAndCudaSkipReason(); !skip_reason.empty()) {
    GTEST_SKIP() << skip_reason;
  }

  const cv::Mat  src = MakePattern(6, 5, CV_32FC3);
  const cv::Rect roi(2, 1, 3, 2);
  const cv::Mat  actual =
      RunOpenClCropResize(src, roi, roi.size(), ResizeDownsampleAlgorithm::Bilinear);
  const cv::Mat expected =
      RunCudaCropResize(src, roi, roi.size(), ResizeDownsampleAlgorithm::Bilinear);

  ExpectMatNear(actual, expected, 0.0f);
}

TEST(OpenClGeometryUtilsTest, CropResizeAreaMatchesCudaRoiResize) {
  if (const std::string skip_reason = OpenClAndCudaSkipReason(); !skip_reason.empty()) {
    GTEST_SKIP() << skip_reason;
  }

  const cv::Mat  src = MakePattern(8, 6, CV_32FC4);
  const cv::Rect roi(1, 1, 6, 4);
  const cv::Size dst_size(3, 2);

  const cv::Mat  actual = RunOpenClCropResize(src, roi, dst_size, ResizeDownsampleAlgorithm::Area);
  const cv::Mat  expected = RunCudaCropResize(src, roi, dst_size, ResizeDownsampleAlgorithm::Area);

  ExpectMatNear(actual, expected, 1.0e-5f);
}

TEST(OpenClGeometryUtilsTest, RotatesLikeCudaRawGeometryUtils) {
  if (const std::string skip_reason = OpenClAndCudaSkipReason(); !skip_reason.empty()) {
    GTEST_SKIP() << skip_reason;
  }

  const cv::Mat src = MakePattern(3, 2, CV_32FC3);
  for (int rotate_code :
       {cv::ROTATE_180, cv::ROTATE_90_CLOCKWISE, cv::ROTATE_90_COUNTERCLOCKWISE}) {
    const cv::Mat actual   = RunOpenClRotate(src, rotate_code);
    const cv::Mat expected = RunCudaRotate(src, rotate_code);
    ExpectMatNear(actual, expected, 0.0f);
  }
}

TEST(OpenClGeometryUtilsTest, ResizeOpOpenClMatchesCudaAreaDownsample) {
  if (const std::string skip_reason = OpenClAndCudaSkipReason(); !skip_reason.empty()) {
    GTEST_SKIP() << skip_reason;
  }

  nlohmann::json params;
  params["resize"] = {{"enable_scale", true},
                      {"maximum_edge", 2},
                      {"enable_roi", false},
                      {"downsample_algorithm", "inter_area"}};

  ResizeOp cuda_op(params);
  auto     cuda_buffer = std::make_shared<ImageBuffer>(MakePattern(4, 4, CV_32FC3));
  cuda_buffer->SyncToGPU(GpuBackendKind::CUDA);
  cuda_buffer->ReleaseCPUData();
  cuda_op.ApplyGPU(cuda_buffer);
  cuda_buffer->SyncToCPU();

  ResizeOp opencl_op(params);
  auto     opencl_buffer = std::make_shared<ImageBuffer>(MakePattern(4, 4, CV_32FC3));
  opencl_buffer->SyncToGPU(GpuBackendKind::OpenCL);
  opencl_buffer->ReleaseCPUData();
  opencl_op.ApplyGPU(opencl_buffer);
  opencl_buffer->SyncToCPU();

  ExpectMatNear(opencl_buffer->GetCPUData(), cuda_buffer->GetCPUData(), 1.0e-5f);
}

TEST(OpenClGeometryUtilsTest, CropRotateOpOpenClMatchesCuda) {
  if (const std::string skip_reason = OpenClAndCudaSkipReason(); !skip_reason.empty()) {
    GTEST_SKIP() << skip_reason;
  }

  nlohmann::json params;
  params["crop_rotate"] = {{"enabled", true},
                           {"angle_degrees", 15.0f},
                           {"enable_crop", true},
                           {"crop_rect", {{"x", 0.1f}, {"y", 0.1f}, {"w", 0.75f}, {"h", 0.7f}}},
                           {"expand_to_fit", true},
                           {"aspect_ratio_preset", "free"},
                           {"aspect_ratio", {{"width", 1.0f}, {"height", 1.0f}}}};

  CropRotateOp cuda_op(params);
  auto         cuda_buffer = std::make_shared<ImageBuffer>(MakePattern(8, 6, CV_32FC4));
  cuda_buffer->SyncToGPU(GpuBackendKind::CUDA);
  cuda_buffer->ReleaseCPUData();
  cuda_op.ApplyGPU(cuda_buffer);
  cuda_buffer->SyncToCPU();

  CropRotateOp opencl_op(params);
  auto         opencl_buffer = std::make_shared<ImageBuffer>(MakePattern(8, 6, CV_32FC4));
  opencl_buffer->SyncToGPU(GpuBackendKind::OpenCL);
  opencl_buffer->ReleaseCPUData();
  opencl_op.ApplyGPU(opencl_buffer);
  opencl_buffer->SyncToCPU();

  ExpectMatNear(opencl_buffer->GetCPUData(), cuda_buffer->GetCPUData(), 1.0e-4f);
}

}  // namespace alcedo
