//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifdef HAVE_OPENCL

#include <gtest/gtest.h>

#include <cmath>

#include "image/image_buffer.hpp"
#include "opencl/opencl_context.hpp"

namespace alcedo {
namespace {

auto TryEnsureOpenClContext() -> bool { return OpenClContext::Instance().TryInitialize(); }

auto MakeRampU16C1() -> cv::Mat {
  cv::Mat image(3, 4, CV_16UC1);
  for (int y = 0; y < image.rows; ++y) {
    for (int x = 0; x < image.cols; ++x) {
      image.at<uint16_t>(y, x) = static_cast<uint16_t>(y * image.cols + x);
    }
  }
  return image;
}

}  // namespace

TEST(ImageBufferOpenClTest, SyncRoundTripCopyAndCloneFromGPU) {
  if (!TryEnsureOpenClContext()) {
    GTEST_SKIP() << OpenClContext::Instance().LastInitializationError();
  }

  const auto  expected = MakeRampU16C1();
  ImageBuffer buffer{expected.clone()};

  buffer.SyncToGPU(GpuBackendKind::OpenCL);
  EXPECT_TRUE(buffer.gpu_data_valid_);
  EXPECT_EQ(buffer.GetGPUBackend(), GpuBackendKind::OpenCL);
  EXPECT_EQ(buffer.GetGPUWidth(), 4);
  EXPECT_EQ(buffer.GetGPUHeight(), 3);
  EXPECT_EQ(buffer.GetGPUType(), CV_16UC1);
  EXPECT_NE(buffer.GetOpenClImage().Buffer(), nullptr);

  ImageBuffer gpu_copy;
  gpu_copy.InitGPUData(buffer.GetGPUWidth(), buffer.GetGPUHeight(), buffer.GetGPUType(),
                       GpuBackendKind::OpenCL);
  buffer.CopyGPUDataTo(gpu_copy);
  gpu_copy.SyncToCPU();
  EXPECT_EQ(cv::countNonZero(gpu_copy.GetCPUData() != expected), 0);

  ImageBuffer cloned = buffer.Clone();
  EXPECT_TRUE(cloned.cpu_data_valid_);
  EXPECT_EQ(cv::countNonZero(cloned.GetCPUData() != expected), 0);
}

TEST(ImageBufferOpenClTest, ConvertGPUDataToFloat) {
  if (!TryEnsureOpenClContext()) {
    GTEST_SKIP() << OpenClContext::Instance().LastInitializationError();
  }

  ImageBuffer buffer{MakeRampU16C1()};
  buffer.SyncToGPU(GpuBackendKind::OpenCL);
  buffer.ConvertGPUDataTo(CV_32FC1, 1.0 / 65535.0, 0.0);
  buffer.SyncToCPU();

  const auto& converted = buffer.GetCPUData();
  ASSERT_EQ(converted.type(), CV_32FC1);
  EXPECT_NEAR(converted.at<float>(0, 0), 0.0f, 1e-6f);
  EXPECT_NEAR(converted.at<float>(2, 3), static_cast<float>(11.0 / 65535.0), 1e-6f);
}

}  // namespace alcedo

#endif
