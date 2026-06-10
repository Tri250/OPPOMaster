//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include <gtest/gtest.h>

#include <cmath>
#include <cstdint>
#include <filesystem>
#include <fstream>
#include <regex>
#include <sstream>
#include <string>

#include "edit/pipeline/highlight_shadow_local_tone.hpp"
#include "edit/pipeline/local_tone_mapping.hpp"

namespace alcedo {
namespace {

namespace tone = local_tone_mapping;

struct TestToneParams {
  std::uint64_t hs_mask_base_cache_key_      = 0;
  bool          shadows_enabled_             = false;
  bool          highlights_enabled_          = false;
  bool          render_roi_enabled_          = false;
  int           render_roi_x_                = 0;
  int           render_roi_y_                = 0;
  float         render_roi_scale_x_          = 1.0f;
  float         render_roi_scale_y_          = 1.0f;
  int           render_roi_reference_width_  = 0;
  int           render_roi_reference_height_ = 0;
};

auto ReadSourceFile(const std::filesystem::path& path) -> std::string {
  std::ifstream file(path, std::ios::binary);
  EXPECT_TRUE(file.is_open()) << path.string();
  std::ostringstream contents;
  contents << file.rdbuf();
  return contents.str();
}

auto SourcePath(const std::filesystem::path& relative) -> std::filesystem::path {
  return std::filesystem::path(ALCEDO_SOURCE_ROOT) / relative;
}

auto ExtractFloatLiteral(const std::string& source, const std::string& symbol) -> float {
  const std::regex pattern(symbol + R"(\s*(?:=|\s)\s*(-?\d+(?:\.\d+)?)(?:f)?)");
  std::smatch      match;
  EXPECT_TRUE(std::regex_search(source, match, pattern)) << symbol;
  if (match.size() < 2) {
    return 0.0f;
  }
  return std::stof(match[1].str());
}

}  // namespace

TEST(LocalToneMappingContractTest, CompatibilityHeaderExportsSharedContract) {
  EXPECT_EQ(highlight_shadow_local_tone::kMaxLevels, tone::kMaxLevels);
  EXPECT_FLOAT_EQ(highlight_shadow_local_tone::kBaseSigmaR, tone::kBaseSigmaR);
  EXPECT_FLOAT_EQ(highlight_shadow_local_tone::kHighlightStrengthScale,
                  tone::kHighlightStrengthScale);
  EXPECT_FLOAT_EQ(highlight_shadow_local_tone::kBackendAmountLimit, tone::kBackendAmountLimit);
}

TEST(LocalToneMappingContractTest, BuildSamplesCoversConfiguredGammaDomain) {
  const auto  samples = tone::BuildSamples(0.75f, 0.65f);
  const float expected_step =
      std::max(tone::kBaseSigmaR * tone::kGammaStepScale, tone::kMinSampleStep);
  const int expected_count = std::max(
      2, static_cast<int>(std::ceil((tone::kGammaMaxL - tone::kGammaMinL) / expected_step)) + 1);

  ASSERT_EQ(static_cast<int>(samples.size()), expected_count);
  EXPECT_NEAR(samples.front().gamma, tone::kGammaMinL, 1.0e-6f);
  EXPECT_NEAR(samples.back().gamma, tone::kGammaMaxL, 1.0e-6f);
  for (std::size_t i = 1; i < samples.size(); ++i) {
    EXPECT_GT(samples[i].gamma, samples[i - 1].gamma);
  }

  const auto& mid = samples[samples.size() / 2];
  EXPECT_FLOAT_EQ(mid.target, tone::ApplyReferenceCurve(mid.gamma, 0.75f, 0.65f));
  EXPECT_FLOAT_EQ(mid.beta, tone::ToneBeta(mid.gamma, 0.75f, 0.65f));
  EXPECT_FLOAT_EQ(mid.alpha, tone::DetailAlpha(mid.gamma, 0.75f, 0.65f));
}

TEST(LocalToneMappingContractTest, ReferenceCurvePreservesExpectedToneDirections) {
  const float shadow_l    = tone::kAcesccMiddleGray - 4.5f * tone::kAcesccCodePerEv;
  const float highlight_l = tone::kAcesccMiddleGray + 5.0f * tone::kAcesccCodePerEv;

  EXPECT_GT(tone::ApplyReferenceCurve(shadow_l, 1.0f, 0.0f), shadow_l);
  EXPECT_LT(tone::ApplyReferenceCurve(shadow_l, -1.0f, 0.0f), shadow_l);
  EXPECT_LT(tone::ApplyReferenceCurve(highlight_l, 0.0f, 1.0f), highlight_l);
  EXPECT_GT(tone::ApplyReferenceCurve(highlight_l, 0.0f, -1.0f), highlight_l);

  const float combined = tone::ApplyReferenceCurve(tone::kAcesccMiddleGray, 1.0f, 1.0f);
  EXPECT_TRUE(std::isfinite(combined));
}

TEST(LocalToneMappingContractTest, DetailAlphaAndToneBetaStayBounded) {
  const float deep_shadow = tone::kAcesccMiddleGray - 6.0f * tone::kAcesccCodePerEv;
  const float mid_shadow  = tone::kAcesccMiddleGray - 2.0f * tone::kAcesccCodePerEv;

  EXPECT_GT(tone::DetailAlpha(deep_shadow, 1.0f, 0.0f), 1.0f);
  EXPECT_LT(tone::DetailAlpha(mid_shadow, 1.0f, 0.0f), 1.0f);

  for (float l = tone::kGammaMinL; l <= tone::kGammaMaxL; l += 0.11f) {
    const float beta = tone::ToneBeta(l, 1.0f, 1.0f);
    EXPECT_GE(beta, tone::kToneBetaMin);
    EXPECT_LE(beta, tone::kToneBetaMax);
  }
}

TEST(LocalToneMappingContractTest, CacheKeysTrackAmountsFlagsAndRoi) {
  TestToneParams params;
  params.hs_mask_base_cache_key_ = 0x12345678ull;
  params.shadows_enabled_        = true;
  params.highlights_enabled_     = true;

  const std::uint64_t base       = tone::BuildAdjustedResultCacheKey(params, 0.5f, 0.25f);
  EXPECT_EQ(base, tone::BuildAdjustedResultCacheKey(params, 0.5f, 0.25f));
  EXPECT_NE(base, tone::BuildAdjustedResultCacheKey(params, 0.6f, 0.25f));
  EXPECT_NE(base, tone::BuildAdjustedResultCacheKey(params, 0.5f, 0.30f));

  params.highlights_enabled_ = false;
  EXPECT_NE(base, tone::BuildAdjustedResultCacheKey(params, 0.5f, 0.25f));
  params.highlights_enabled_          = true;

  params.render_roi_enabled_          = true;
  params.render_roi_x_                = 4;
  params.render_roi_y_                = 7;
  params.render_roi_scale_x_          = 0.5f;
  params.render_roi_scale_y_          = 0.75f;
  params.render_roi_reference_width_  = 1280;
  params.render_roi_reference_height_ = 720;
  const std::uint64_t roi_key         = tone::BuildRoiAdjustedResultCacheKey(params, base);
  EXPECT_NE(base, roi_key);
  params.render_roi_x_ = 5;
  EXPECT_NE(roi_key, tone::BuildRoiAdjustedResultCacheKey(params, base));
}

TEST(LocalToneMappingContractTest, RoiReferenceReuseDoesNotRequireSamePresentationSize) {
  EXPECT_TRUE(tone::CanReuseReferenceForRoi(
      /*roi_frame_with_source_reference=*/true,
      /*reference_source_cache_valid=*/true,
      /*roi_reference_width=*/4096,
      /*roi_reference_height=*/2731));
  EXPECT_TRUE(tone::CanReuseReferenceForRoi(
      /*roi_frame_with_source_reference=*/true,
      /*reference_source_cache_valid=*/true,
      /*roi_reference_width=*/2560,
      /*roi_reference_height=*/1707));

  EXPECT_FALSE(tone::CanReuseReferenceForRoi(
      /*roi_frame_with_source_reference=*/true,
      /*reference_source_cache_valid=*/false,
      /*roi_reference_width=*/4096,
      /*roi_reference_height=*/2731));
  EXPECT_FALSE(tone::CanReuseReferenceForRoi(
      /*roi_frame_with_source_reference=*/false,
      /*reference_source_cache_valid=*/true,
      /*roi_reference_width=*/4096,
      /*roi_reference_height=*/2731));
  EXPECT_FALSE(tone::CanReuseReferenceForRoi(
      /*roi_frame_with_source_reference=*/true,
      /*reference_source_cache_valid=*/true,
      /*roi_reference_width=*/0,
      /*roi_reference_height=*/2731));
}

TEST(LocalToneMappingContractTest, ShaderMirrorConstantsMatchSharedContract) {
  const auto opencl = ReadSourceFile(SourcePath("edit/pipeline/opencl_shader/tone_mapping.cl"));
  const auto metal =
      ReadSourceFile(SourcePath("edit/operators/GPU_kernels/metal_shader/tone_mapping.metal"));

  EXPECT_NE(opencl.find("Mirrored from edit/pipeline/local_tone_mapping.hpp"), std::string::npos);
  EXPECT_NE(metal.find("Mirrored from edit/pipeline/local_tone_mapping.hpp"), std::string::npos);

  EXPECT_FLOAT_EQ(ExtractFloatLiteral(opencl, "ALCEDO_OPENCL_HS_ACESCC_MIDDLE_GRAY"),
                  tone::kAcesccMiddleGray);
  EXPECT_FLOAT_EQ(ExtractFloatLiteral(opencl, "ALCEDO_OPENCL_HS_BASE_SIGMA_R"), tone::kBaseSigmaR);
  EXPECT_FLOAT_EQ(ExtractFloatLiteral(opencl, "ALCEDO_OPENCL_HS_HIGHLIGHT_STRENGTH_SCALE"),
                  tone::kHighlightStrengthScale);
  EXPECT_FLOAT_EQ(ExtractFloatLiteral(opencl, "ALCEDO_OPENCL_HS_BACKEND_AMOUNT_LIMIT"),
                  tone::kBackendAmountLimit);
  EXPECT_FLOAT_EQ(ExtractFloatLiteral(opencl, "ALCEDO_OPENCL_HS_TONE_BETA_EPS"),
                  tone::kToneBetaEps);
  EXPECT_FLOAT_EQ(ExtractFloatLiteral(opencl, "ALCEDO_OPENCL_HS_TONE_BETA_MIN"),
                  tone::kToneBetaMin);
  EXPECT_FLOAT_EQ(ExtractFloatLiteral(opencl, "ALCEDO_OPENCL_HS_TONE_BETA_MAX"),
                  tone::kToneBetaMax);

  EXPECT_FLOAT_EQ(ExtractFloatLiteral(metal, "kMetalHsAcesccMiddleGray"), tone::kAcesccMiddleGray);
  EXPECT_FLOAT_EQ(ExtractFloatLiteral(metal, "kMetalHsBaseSigmaR"), tone::kBaseSigmaR);
  EXPECT_FLOAT_EQ(ExtractFloatLiteral(metal, "kMetalHsHighlightStrengthScale"),
                  tone::kHighlightStrengthScale);
  EXPECT_FLOAT_EQ(ExtractFloatLiteral(metal, "kMetalHsBackendAmountLimit"),
                  tone::kBackendAmountLimit);
  EXPECT_FLOAT_EQ(ExtractFloatLiteral(metal, "kMetalHsToneBetaEps"), tone::kToneBetaEps);
  EXPECT_FLOAT_EQ(ExtractFloatLiteral(metal, "kMetalHsToneBetaMin"), tone::kToneBetaMin);
  EXPECT_FLOAT_EQ(ExtractFloatLiteral(metal, "kMetalHsToneBetaMax"), tone::kToneBetaMax);
}

}  // namespace alcedo
