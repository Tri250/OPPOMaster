//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifdef HAVE_OPENCL

#include <gtest/gtest.h>

#include <algorithm>
#include <cmath>
#include <filesystem>
#include <fstream>
#include <memory>

#include "edit/operators/cst/odt_op.hpp"
#include "edit/pipeline/pipeline_gpu_wrapper.hpp"
#include "image/image_buffer.hpp"
#include "opencl/opencl_context.hpp"
#include "opencl/opencl_runtime.hpp"

namespace alcedo {
namespace {

auto TryEnsureOpenClRuntime() -> bool {
  if (TryPrepareOpenClRuntime()) {
    return true;
  }
  return OpenClContext::Instance().IsInitialized();
}

auto MakeNoOpFusedParams() -> OperatorParams {
  OperatorParams params;
  params.to_ws_enabled_       = false;
  params.color_temp_enabled_  = false;
  params.exposure_enabled_    = false;
  params.contrast_enabled_    = false;
  params.white_enabled_       = false;
  params.black_enabled_       = false;
  params.highlights_enabled_  = false;
  params.shadows_enabled_     = false;
  params.curve_enabled_       = false;
  params.saturation_enabled_  = false;
  params.tint_enabled_        = false;
  params.vibrance_enabled_    = false;
  params.color_wheel_enabled_ = false;
  params.hls_enabled_         = false;
  params.lmt_enabled_         = false;
  params.to_output_enabled_   = false;
  params.clarity_enabled_     = false;
  params.sharpen_enabled_     = false;
  return params;
}

auto MakeAcesccTestImage() -> cv::Mat {
  cv::Mat image(2, 3, CV_32FC4);
  image.at<cv::Vec4f>(0, 0) = {0.34f, 0.38f, 0.42f, 1.0f};
  image.at<cv::Vec4f>(0, 1) = {0.48f, 0.50f, 0.53f, 1.0f};
  image.at<cv::Vec4f>(0, 2) = {0.58f, 0.62f, 0.66f, 1.0f};
  image.at<cv::Vec4f>(1, 0) = {0.28f, 0.32f, 0.36f, 1.0f};
  image.at<cv::Vec4f>(1, 1) = {0.44f, 0.47f, 0.51f, 1.0f};
  image.at<cv::Vec4f>(1, 2) = {0.70f, 0.72f, 0.74f, 1.0f};
  return image;
}

auto RunOpenClFusedPipeline(GPUPipelineWrapper& pipeline, const cv::Mat& input,
                            OperatorParams& params) -> cv::Mat {
  auto input_buffer  = std::make_shared<ImageBuffer>(input.clone());
  auto output_buffer = std::make_shared<ImageBuffer>();

  pipeline.SetInputImage(input_buffer);
  pipeline.SetParams(params);
  pipeline.Execute(output_buffer);

  output_buffer->SyncToCPU();
  return output_buffer->GetCPUData().clone();
}

auto RunOpenClFusedPipeline(const cv::Mat& input, OperatorParams params) -> cv::Mat {
  GPUPipelineWrapper pipeline(GpuBackendKind::OpenCL);
  return RunOpenClFusedPipeline(pipeline, input, params);
}

auto MaxRgbDiff(const cv::Mat& a, const cv::Mat& b) -> float {
  CV_Assert(a.size() == b.size());
  CV_Assert(a.type() == b.type());
  float max_diff = 0.0f;
  for (int y = 0; y < a.rows; ++y) {
    for (int x = 0; x < a.cols; ++x) {
      const cv::Vec4f lhs = a.at<cv::Vec4f>(y, x);
      const cv::Vec4f rhs = b.at<cv::Vec4f>(y, x);
      for (int c = 0; c < 3; ++c) {
        max_diff = std::max(max_diff, std::abs(lhs[c] - rhs[c]));
      }
    }
  }
  return max_diff;
}

auto MakeRgbOffsetImage(const cv::Mat& input, const cv::Vec3f& offset) -> cv::Mat {
  cv::Mat expected = input.clone();
  for (int y = 0; y < expected.rows; ++y) {
    for (int x = 0; x < expected.cols; ++x) {
      cv::Vec4f& px = expected.at<cv::Vec4f>(y, x);
      px[0] += offset[0];
      px[1] += offset[1];
      px[2] += offset[2];
    }
  }
  return expected;
}

void ExpectFiniteRgba32f(const cv::Mat& image) {
  ASSERT_EQ(image.type(), CV_32FC4);
  for (int y = 0; y < image.rows; ++y) {
    for (int x = 0; x < image.cols; ++x) {
      const cv::Vec4f px = image.at<cv::Vec4f>(y, x);
      for (int c = 0; c < 4; ++c) {
        EXPECT_TRUE(std::isfinite(px[c])) << "at (" << x << ", " << y << ") channel " << c;
      }
    }
  }
}

auto MakeDefaultOutputTransformParams() -> OperatorParams {
  auto   params = MakeNoOpFusedParams();
  ODT_Op odt({{"odt",
               {{"method", "open_drt"},
                {"encoding_space", "rec709"},
                {"encoding_eotf", "gamma_2_2"},
                {"peak_luminance", 100.0f}}}});
  odt.SetGlobalParams(params);
  params.to_output_enabled_ = true;
  return params;
}

}  // namespace

TEST(OpenClFusedEditPipelineTest, AppliesPointOperatorsWhenOutputTransformDisabled) {
  if (!TryEnsureOpenClRuntime()) {
    GTEST_SKIP() << OpenClContext::Instance().LastInitializationError();
  }

  cv::Mat input(1, 2, CV_32FC4);
  input.at<cv::Vec4f>(0, 0) = {0.10f, 0.20f, 0.30f, 1.0f};
  input.at<cv::Vec4f>(0, 1) = {0.40f, 0.50f, 0.60f, 0.75f};

  auto params               = MakeNoOpFusedParams();
  params.exposure_enabled_  = true;
  params.exposure_offset_   = 0.25f;

  const cv::Mat output      = RunOpenClFusedPipeline(input, params);

  ASSERT_EQ(output.rows, input.rows);
  ASSERT_EQ(output.cols, input.cols);
  ExpectFiniteRgba32f(output);
  EXPECT_NEAR(output.at<cv::Vec4f>(0, 0)[0], 0.35f, 1.0e-6f);
  EXPECT_NEAR(output.at<cv::Vec4f>(0, 0)[1], 0.45f, 1.0e-6f);
  EXPECT_NEAR(output.at<cv::Vec4f>(0, 0)[2], 0.55f, 1.0e-6f);
  EXPECT_NEAR(output.at<cv::Vec4f>(0, 0)[3], 1.0f, 1.0e-6f);
  EXPECT_NEAR(output.at<cv::Vec4f>(0, 1)[0], 0.65f, 1.0e-6f);
  EXPECT_NEAR(output.at<cv::Vec4f>(0, 1)[1], 0.75f, 1.0e-6f);
  EXPECT_NEAR(output.at<cv::Vec4f>(0, 1)[2], 0.85f, 1.0e-6f);
  EXPECT_NEAR(output.at<cv::Vec4f>(0, 1)[3], 0.75f, 1.0e-6f);
}

TEST(OpenClFusedEditPipelineTest, OutputTransformIsDeferredInPhase3) {
  if (!TryEnsureOpenClRuntime()) {
    GTEST_SKIP() << OpenClContext::Instance().LastInitializationError();
  }

  const cv::Mat input                       = MakeAcesccTestImage();

  auto          output_disabled_params      = MakeNoOpFusedParams();
  output_disabled_params.to_output_enabled_ = false;
  const cv::Mat output_disabled             = RunOpenClFusedPipeline(input, output_disabled_params);

  const cv::Mat output_enabled = RunOpenClFusedPipeline(input, MakeDefaultOutputTransformParams());

  ExpectFiniteRgba32f(output_disabled);
  ExpectFiniteRgba32f(output_enabled);
  EXPECT_LE(MaxRgbDiff(output_enabled, output_disabled), 1.0e-6f)
      << "Phase 3 OpenCL fused pipeline should not apply deferred CST/ToOutput yet.";
}

TEST(OpenClFusedEditPipelineTest, LmtPassthroughWhenDisabled) {
  if (!TryEnsureOpenClRuntime()) {
    GTEST_SKIP() << OpenClContext::Instance().LastInitializationError();
  }

  const cv::Mat input  = MakeAcesccTestImage();

  auto          params = MakeNoOpFusedParams();
  params.lmt_enabled_  = false;

  const cv::Mat output = RunOpenClFusedPipeline(input, params);

  ExpectFiniteRgba32f(output);
  ASSERT_EQ(output.rows, input.rows);
  ASSERT_EQ(output.cols, input.cols);
  EXPECT_LE(MaxRgbDiff(output, input), 1.0e-6f)
      << "When LMT is disabled, output should match input for NoOp params.";
}

TEST(OpenClFusedEditPipelineTest, LmtIdentityLutPreservesValues) {
  if (!TryEnsureOpenClRuntime()) {
    GTEST_SKIP() << OpenClContext::Instance().LastInitializationError();
  }

  // Generate a minimal identity 3D LUT (edge=2).
  // Cube file data order: blue slowest, green middle, red fastest.
  const std::string cube_content = R"(LUT_3D_SIZE 2
0.0 0.0 0.0
1.0 0.0 0.0
0.0 1.0 0.0
1.0 1.0 0.0
0.0 0.0 1.0
1.0 0.0 1.0
0.0 1.0 1.0
1.0 1.0 1.0
)";

  const auto        tmp_dir      = std::filesystem::temp_directory_path();
  const auto        lut_path     = tmp_dir / "opencl_test_identity_lut.cube";
  {
    std::ofstream ofs(lut_path, std::ios::binary);
    ofs << cube_content;
  }

  const cv::Mat input    = MakeAcesccTestImage();

  auto          params   = MakeNoOpFusedParams();
  params.lmt_enabled_    = true;
  params.lmt_lut_path_   = lut_path;

  const cv::Mat   output = RunOpenClFusedPipeline(input, params);

  std::error_code ec;
  std::filesystem::remove(lut_path, ec);

  ExpectFiniteRgba32f(output);

  // An identity 2-edge LUT preserves values in [0, 1] exactly (trilinear
  // interpolation of corner values gives back the input coordinate).
  // Slight fp differences from the lookup math are expected; we allow a
  // generous tolerance because the LUT resolution is extremely low (edge=2)
  // and ACEScc values near 0/1 fall into the clamped region.
  EXPECT_LE(MaxRgbDiff(output, input), 1.0e-5f);
}

TEST(OpenClFusedEditPipelineTest, LmtAppliesCubeLut) {
  if (!TryEnsureOpenClRuntime()) {
    GTEST_SKIP() << OpenClContext::Instance().LastInitializationError();
  }

  // This LUT encodes a simple affine offset: rgb -> rgb + (0.1, 0.2, 0.3).
  const std::string cube_content = R"(LUT_3D_SIZE 2
0.1 0.2 0.3
1.1 0.2 0.3
0.1 1.2 0.3
1.1 1.2 0.3
0.1 0.2 1.3
1.1 0.2 1.3
0.1 1.2 1.3
1.1 1.2 1.3
)";

  const auto        tmp_dir      = std::filesystem::temp_directory_path();
  const auto        lut_path     = tmp_dir / "opencl_test_offset_lut.cube";
  {
    std::ofstream ofs(lut_path, std::ios::binary);
    ofs << cube_content;
  }

  const cv::Mat input    = MakeAcesccTestImage();

  auto          params   = MakeNoOpFusedParams();
  params.lmt_enabled_    = true;
  params.lmt_lut_path_   = lut_path;

  const cv::Mat   output = RunOpenClFusedPipeline(input, params);

  std::error_code ec;
  std::filesystem::remove(lut_path, ec);

  ExpectFiniteRgba32f(output);
  const cv::Mat expected = MakeRgbOffsetImage(input, {0.1f, 0.2f, 0.3f});
  EXPECT_LE(MaxRgbDiff(output, expected), 1.0e-5f);
}

TEST(OpenClFusedEditPipelineTest, LmtReloadsWhenPathChangesWithoutDirtyFlag) {
  if (!TryEnsureOpenClRuntime()) {
    GTEST_SKIP() << OpenClContext::Instance().LastInitializationError();
  }

  const std::string first_cube  = R"(LUT_3D_SIZE 2
0.1 0.0 0.0
1.1 0.0 0.0
0.1 1.0 0.0
1.1 1.0 0.0
0.1 0.0 1.0
1.1 0.0 1.0
0.1 1.0 1.0
1.1 1.0 1.0
)";
  const std::string second_cube = R"(LUT_3D_SIZE 2
0.2 0.0 0.0
1.2 0.0 0.0
0.2 1.0 0.0
1.2 1.0 0.0
0.2 0.0 1.0
1.2 0.0 1.0
0.2 1.0 1.0
1.2 1.0 1.0
)";

  const auto        tmp_dir     = std::filesystem::temp_directory_path();
  const auto        first_path  = tmp_dir / "opencl_test_first_lut.cube";
  const auto        second_path = tmp_dir / "opencl_test_second_lut.cube";
  {
    std::ofstream first(first_path, std::ios::binary);
    first << first_cube;
    std::ofstream second(second_path, std::ios::binary);
    second << second_cube;
  }

  const cv::Mat input  = MakeAcesccTestImage();
  auto          params = MakeNoOpFusedParams();
  params.lmt_enabled_  = true;
  params.lmt_lut_path_ = first_path;

  GPUPipelineWrapper pipeline(GpuBackendKind::OpenCL);
  (void)RunOpenClFusedPipeline(pipeline, input, params);

  params.lmt_lut_path_ = second_path;
  ASSERT_FALSE(params.to_lmt_dirty_);

  const cv::Mat   output = RunOpenClFusedPipeline(pipeline, input, params);

  std::error_code ec;
  std::filesystem::remove(first_path, ec);
  std::filesystem::remove(second_path, ec);

  ExpectFiniteRgba32f(output);
  const cv::Mat expected = MakeRgbOffsetImage(input, {0.2f, 0.0f, 0.0f});
  EXPECT_LE(MaxRgbDiff(output, expected), 1.0e-5f);
}

TEST(OpenClFusedEditPipelineTest, LmtEnabledWithEmptyPathThrows) {
  if (!TryEnsureOpenClRuntime()) {
    GTEST_SKIP() << OpenClContext::Instance().LastInitializationError();
  }

  const cv::Mat input  = MakeAcesccTestImage();

  auto          params = MakeNoOpFusedParams();
  params.lmt_enabled_  = true;
  params.lmt_lut_path_ = std::filesystem::path();  // empty path

  EXPECT_THROW(RunOpenClFusedPipeline(input, params), std::runtime_error);
}

}  // namespace alcedo

#endif
