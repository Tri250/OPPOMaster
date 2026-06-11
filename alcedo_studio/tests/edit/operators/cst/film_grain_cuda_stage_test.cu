//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include <gtest/gtest.h>

#include <cmath>
#include <opencv2/core.hpp>
#include <opencv2/core/cuda.hpp>

#include "edit/operators/GPU_kernels/cst.cuh"
#include "edit/operators/GPU_kernels/film_grain.cuh"
#include "edit/operators/cst/film_grain_op.hpp"
#include "edit/operators/cst/odt_op.hpp"
#include "edit/pipeline/default_pipeline_params.hpp"
#include "edit/pipeline/gpu_scheduler.cuh"
#include "edit/pipeline/kernel_stream_gpu.cuh"
#include "image/image_buffer.hpp"

namespace alcedo {
namespace {

using OutputOnlyStream =
    CUDA::GPU_StaticKernelStream<CUDA::GPU_PointChain<CUDA::GPU_OUTPUT_Kernel>>;
using OutputFilmGrainStream =
    CUDA::GPU_StaticKernelStream<CUDA::GPU_PointChain<CUDA::GPU_OUTPUT_Kernel>,
                                 CUDA::GPU_FilmGrainBlurHorizontalKernel,
                                 CUDA::GPU_FilmGrainApplyVerticalKernel>;
using DirectFilmGrainStream = CUDA::GPU_StaticKernelStream<CUDA::GPU_FilmGrainBlurHorizontalKernel,
                                                           CUDA::GPU_FilmGrainApplyVerticalKernel>;

auto EnsureCudaDevice() -> bool {
  const int device_count = cv::cuda::getCudaEnabledDeviceCount();
  if (device_count <= 0) {
    return false;
  }
  cv::cuda::setDevice(0);
  return true;
}

auto AcesccEncode(float linear) -> float {
  constexpr float kLog2Denorm   = -16.0f;
  constexpr float kDenormTrans  = 0.00003051757812f;
  constexpr float kDenormOffset = 0.00001525878906f;
  constexpr float kA            = 9.72f;
  constexpr float kB            = 17.52f;

  if (linear <= 0.0f) {
    return (kLog2Denorm + kA) / kB;
  }
  if (linear < kDenormTrans) {
    return (std::log2(kDenormOffset + linear * 0.5f) + kA) / kB;
  }
  return (std::log2(linear) + kA) / kB;
}

auto MakeAcesccInput() -> cv::Mat {
  cv::Mat input(3, 3, CV_32FC4);
  for (int y = 0; y < input.rows; ++y) {
    for (int x = 0; x < input.cols; ++x) {
      const float r = 0.05f + 0.12f * static_cast<float>(x + y);
      const float g = 0.10f + 0.08f * static_cast<float>(x * 2 + y);
      const float b = 0.03f + 0.05f * static_cast<float>(x + y * 2);
      input.at<cv::Vec4f>(y, x) =
          cv::Vec4f(AcesccEncode(r), AcesccEncode(g), AcesccEncode(b), 1.0f);
    }
  }
  return input;
}

void SetOdtParams(OperatorParams& params) {
  nlohmann::json odt_params   = pipeline_defaults::MakeDefaultODTParams();
  odt_params["odt"]["method"] = "open_drt";
  ODT_Op odt(odt_params);
  odt.SetGlobalParams(params);
}

auto RunOutputOnly(const cv::Mat& input) -> cv::Mat {
  OperatorParams params;
  SetOdtParams(params);

  auto             input_buffer  = std::make_shared<ImageBuffer>(input.clone());
  auto             output_buffer = std::make_shared<ImageBuffer>();

  OutputOnlyStream stream{CUDA::GPU_PointChain(CUDA::GPU_OUTPUT_Kernel())};
  CUDA::GPU_KernelLauncher<OutputOnlyStream> launcher(nullptr, stream);
  launcher.SetInputImage(input_buffer);
  launcher.SetParams(params);
  launcher.SetOutputImage(output_buffer);
  launcher.Execute();

  output_buffer->SyncToCPU();
  return output_buffer->GetCPUData().clone();
}

auto RunOutputWithFilmGrain(const cv::Mat& input, float strength) -> cv::Mat {
  OperatorParams params;
  SetOdtParams(params);
  FilmGrainOp({{"film_grain", {{"strength", strength}}}}).SetGlobalParams(params);

  auto                  input_buffer  = std::make_shared<ImageBuffer>(input.clone());
  auto                  output_buffer = std::make_shared<ImageBuffer>();

  OutputFilmGrainStream stream{CUDA::GPU_PointChain(CUDA::GPU_OUTPUT_Kernel()),
                               CUDA::GPU_FilmGrainBlurHorizontalKernel(),
                               CUDA::GPU_FilmGrainApplyVerticalKernel()};
  CUDA::GPU_KernelLauncher<OutputFilmGrainStream> launcher(nullptr, stream);
  launcher.SetInputImage(input_buffer);
  launcher.SetParams(params);
  launcher.SetOutputImage(output_buffer);
  launcher.Execute();

  output_buffer->SyncToCPU();
  return output_buffer->GetCPUData().clone();
}

auto RunFilmGrainOnlyWithParams(const cv::Mat& input, const OperatorParams& params) -> cv::Mat {
  auto                  input_buffer  = std::make_shared<ImageBuffer>(input.clone());
  auto                  output_buffer = std::make_shared<ImageBuffer>();

  DirectFilmGrainStream stream{CUDA::GPU_FilmGrainBlurHorizontalKernel(),
                               CUDA::GPU_FilmGrainApplyVerticalKernel()};
  CUDA::GPU_KernelLauncher<DirectFilmGrainStream> launcher(nullptr, stream);
  launcher.SetInputImage(input_buffer);
  auto mutable_params = params;
  launcher.SetParams(mutable_params);
  launcher.SetOutputImage(output_buffer);
  launcher.Execute();

  output_buffer->SyncToCPU();
  return output_buffer->GetCPUData().clone();
}

auto RunFilmGrainOnly(const cv::Mat& input, float strength,
                      std::uint64_t seed = 0x6a09e667f3bcc909ULL) -> cv::Mat {
  OperatorParams params;
  FilmGrainOp({{"film_grain", {{"strength", strength}}}}).SetGlobalParams(params);
  params.film_grain_.seed_ = seed;
  return RunFilmGrainOnlyWithParams(input, params);
}

auto MakeDisplayGrayInput(int width, int height, float value) -> cv::Mat {
  cv::Mat input(height, width, CV_32FC4);
  input.setTo(cv::Scalar(value, value, value, 1.0f));
  return input;
}

void ExpectExactMatch(const cv::Mat& lhs, const cv::Mat& rhs) {
  ASSERT_EQ(lhs.size(), rhs.size());
  ASSERT_EQ(lhs.type(), rhs.type());
  for (int y = 0; y < lhs.rows; ++y) {
    for (int x = 0; x < lhs.cols; ++x) {
      const cv::Vec4f a = lhs.at<cv::Vec4f>(y, x);
      const cv::Vec4f b = rhs.at<cv::Vec4f>(y, x);
      for (int c = 0; c < 4; ++c) {
        EXPECT_EQ(a[c], b[c]) << "Mismatch at (" << x << ", " << y << "), channel " << c;
      }
    }
  }
}

auto AverageChannel(const cv::Mat& image, int channel) -> float {
  double sum = 0.0;
  for (int y = 0; y < image.rows; ++y) {
    for (int x = 0; x < image.cols; ++x) {
      sum += image.at<cv::Vec4f>(y, x)[channel];
    }
  }
  return static_cast<float>(sum / static_cast<double>(image.rows * image.cols));
}

auto CountChangedRgbPixels(const cv::Mat& lhs, const cv::Mat& rhs) -> int {
  int changed = 0;
  for (int y = 0; y < lhs.rows; ++y) {
    for (int x = 0; x < lhs.cols; ++x) {
      const cv::Vec4f a           = lhs.at<cv::Vec4f>(y, x);
      const cv::Vec4f b           = rhs.at<cv::Vec4f>(y, x);
      bool            rgb_changed = false;
      for (int c = 0; c < 3; ++c) {
        EXPECT_TRUE(std::isfinite(b[c]));
        rgb_changed = rgb_changed || std::abs(a[c] - b[c]) > 1.0e-6f;
      }
      EXPECT_EQ(a[3], b[3]);
      if (rgb_changed) {
        ++changed;
      }
    }
  }
  return changed;
}

}  // namespace

TEST(FilmGrainCudaStageTest, StrengthZeroIsExactPassThroughAfterOdt) {
  if (!EnsureCudaDevice()) {
    GTEST_SKIP() << "No CUDA device available.";
  }

  const cv::Mat input = MakeAcesccInput();

  ExpectExactMatch(RunOutputOnly(input), RunOutputWithFilmGrain(input, 0.0f));
}

TEST(FilmGrainCudaStageTest, PositiveStrengthChangesDisplayEncodedOutputAfterOdt) {
  if (!EnsureCudaDevice()) {
    GTEST_SKIP() << "No CUDA device available.";
  }

  const cv::Mat input    = MakeAcesccInput();

  const cv::Mat odt_only = RunOutputOnly(input);
  const cv::Mat grained  = RunOutputWithFilmGrain(input, 100.0f);

  EXPECT_GT(CountChangedRgbPixels(odt_only, grained), 0);
}

TEST(FilmGrainCudaStageTest, FixedSeedIsDeterministicAcrossRepeatedLaunches) {
  if (!EnsureCudaDevice()) {
    GTEST_SKIP() << "No CUDA device available.";
  }

  const cv::Mat input = MakeAcesccInput();

  ExpectExactMatch(RunOutputWithFilmGrain(input, 70.0f), RunOutputWithFilmGrain(input, 70.0f));
}

TEST(FilmGrainCudaStageTest, ConstantGrayMeanStaysCloseToInputProbability) {
  if (!EnsureCudaDevice()) {
    GTEST_SKIP() << "No CUDA device available.";
  }

  constexpr float kGray  = 0.45f;
  const cv::Mat   input  = MakeDisplayGrayInput(32, 32, kGray);
  const cv::Mat   output = RunFilmGrainOnly(input, 100.0f);

  EXPECT_NEAR(AverageChannel(output, 0), kGray, 0.08f);
  EXPECT_NEAR(AverageChannel(output, 1), kGray, 0.08f);
  EXPECT_NEAR(AverageChannel(output, 2), kGray, 0.08f);
}

TEST(FilmGrainCudaStageTest, DistinctSeedsChangeTheGrainPattern) {
  if (!EnsureCudaDevice()) {
    GTEST_SKIP() << "No CUDA device available.";
  }

  const cv::Mat input  = MakeDisplayGrayInput(16, 16, 0.5f);
  const cv::Mat seed_a = RunFilmGrainOnly(input, 100.0f, 0x6a09e667f3bcc909ULL);
  const cv::Mat seed_b = RunFilmGrainOnly(input, 100.0f, 0xbb67ae8584caa73bULL);

  EXPECT_GT(CountChangedRgbPixels(seed_a, seed_b), 0);
}

TEST(FilmGrainCudaStageTest, ColorChannelsUseIndependentGrainStreams) {
  if (!EnsureCudaDevice()) {
    GTEST_SKIP() << "No CUDA device available.";
  }

  const cv::Mat input           = MakeDisplayGrayInput(16, 16, 0.5f);
  const cv::Mat output          = RunFilmGrainOnly(input, 100.0f);

  int           distinct_pixels = 0;
  for (int y = 0; y < output.rows; ++y) {
    for (int x = 0; x < output.cols; ++x) {
      const cv::Vec4f pixel = output.at<cv::Vec4f>(y, x);
      if (std::abs(pixel[0] - pixel[1]) > 1.0e-6f || std::abs(pixel[1] - pixel[2]) > 1.0e-6f) {
        ++distinct_pixels;
      }
    }
  }
  EXPECT_GT(distinct_pixels, 0);
}

TEST(FilmGrainCudaStageTest, RoiPreviewUsesLocalOutputGrainCoordinates) {
  if (!EnsureCudaDevice()) {
    GTEST_SKIP() << "No CUDA device available.";
  }

  constexpr int   kFullSize = 32;
  constexpr int   kRoiSize  = 16;
  constexpr int   kRoiX     = 8;
  constexpr int   kRoiY     = 6;
  constexpr float kGray     = 0.5f;

  const cv::Mat   roi_input = MakeDisplayGrayInput(kRoiSize, kRoiSize, kGray);

  OperatorParams  local_params;
  FilmGrainOp({{"film_grain", {{"strength", 100.0f}}}}).SetGlobalParams(local_params);

  OperatorParams roi_params               = local_params;
  roi_params.render_roi_enabled_          = true;
  roi_params.render_roi_x_                = kRoiX;
  roi_params.render_roi_y_                = kRoiY;
  roi_params.render_roi_scale_x_          = static_cast<float>(kRoiSize) / kFullSize;
  roi_params.render_roi_scale_y_          = static_cast<float>(kRoiSize) / kFullSize;
  roi_params.render_roi_reference_width_  = kFullSize;
  roi_params.render_roi_reference_height_ = kFullSize;

  const cv::Mat local_output              = RunFilmGrainOnlyWithParams(roi_input, local_params);
  const cv::Mat roi_output                = RunFilmGrainOnlyWithParams(roi_input, roi_params);

  for (int y = 0; y < kRoiSize; ++y) {
    for (int x = 0; x < kRoiSize; ++x) {
      const cv::Vec4f local_pixel = local_output.at<cv::Vec4f>(y, x);
      const cv::Vec4f roi_pixel   = roi_output.at<cv::Vec4f>(y, x);
      for (int channel = 0; channel < 4; ++channel) {
        EXPECT_FLOAT_EQ(local_pixel[channel], roi_pixel[channel])
            << "Mismatch at ROI-local (" << x << ", " << y << "), channel " << channel;
      }
    }
  }
}

TEST(FilmGrainCudaStageTest, EdgeProbabilitiesAndHdrInputsStayFinite) {
  if (!EnsureCudaDevice()) {
    GTEST_SKIP() << "No CUDA device available.";
  }

  cv::Mat input(2, 2, CV_32FC4);
  input.at<cv::Vec4f>(0, 0) = cv::Vec4f(0.0f, 0.0f, 0.0f, 1.0f);
  input.at<cv::Vec4f>(0, 1) = cv::Vec4f(0.5f, 0.5f, 0.5f, 1.0f);
  input.at<cv::Vec4f>(1, 0) = cv::Vec4f(1.0f, 1.0f, 1.0f, 1.0f);
  input.at<cv::Vec4f>(1, 1) = cv::Vec4f(1.5f, 1.25f, 2.0f, 1.0f);

  const cv::Mat output      = RunFilmGrainOnly(input, 100.0f);
  for (int y = 0; y < output.rows; ++y) {
    for (int x = 0; x < output.cols; ++x) {
      const cv::Vec4f pixel = output.at<cv::Vec4f>(y, x);
      for (int channel = 0; channel < 4; ++channel) {
        EXPECT_TRUE(std::isfinite(pixel[channel]));
      }
      EXPECT_EQ(pixel[3], 1.0f);
    }
  }
}

}  // namespace alcedo
