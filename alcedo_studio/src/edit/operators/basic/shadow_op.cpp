//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "edit/operators/basic/shadow_op.hpp"

#include <algorithm>
#include <opencv2/core/hal/interface.h>
#include <string>

#include <cmath>
#include <opencv2/core.hpp>
#include <opencv2/core/types.hpp>
#include <opencv2/opencv.hpp>

#include "edit/operators/op_kernel.hpp"
#include "edit/operators/basic/shadows_highlights_shared_curve.hpp"
#include "edit/operators/utils/functions.hpp"
#include "hwy/contrib/math/math-inl.h"
#include "image/image_buffer.hpp"

namespace alcedo {
ShadowsOp::ShadowsOp(float offset) : offset_(offset) {
  float normalized_offset = offset_ / 100.0f;
  gamma_                  = std::pow(2.0f, -normalized_offset * 1.3f);
}

ShadowsOp::ShadowsOp(const nlohmann::json& params) {
  SetParams(params);
  float normalized_offset = offset_ / 100.0f;
  gamma_                  = std::pow(2.0f, -normalized_offset * 1.3f);
}

auto ShadowsOp::GetScale() -> float { return offset_ / 100.0f; }

void ShadowsOp::Apply(std::shared_ptr<ImageBuffer>) {}

void ShadowsOp::ApplyGPU(std::shared_ptr<ImageBuffer>) {
  throw std::runtime_error("ShadowsOp: ApplyGPU not implemented");
}

static inline float Luma(const Pixel& rgb) {
  return 0.2126f * rgb.r_ + 0.7152f * rgb.g_ + 0.0722f * rgb.b_;
}

auto ShadowsOp::GetParams() const -> nlohmann::json {
  return {{std::string(script_name_), offset_}};
}

namespace {
constexpr float kShadowsAdjustmentStrengthScale = 1.5f;

void BuildGaussianKernel(float sigma, int max_radius, int& tap_count,
                         float (&weights)[OperatorParams::kDetailMaxGaussianTapCount]) {
  std::fill_n(weights, OperatorParams::kDetailMaxGaussianTapCount, 0.0f);
  tap_count = 0;
  if (sigma <= 0.0f) {
    return;
  }

  const float safe_sigma = std::max(sigma, 1.0e-4f);
  const int   radius =
      std::clamp(static_cast<int>(std::ceil(3.0f * safe_sigma)), 1, max_radius);
  tap_count = std::min(radius + 1, OperatorParams::kDetailMaxGaussianTapCount);

  const double inv2sigma2 = 0.5 / (static_cast<double>(safe_sigma) * safe_sigma);
  double       full_weight = 1.0;
  weights[0] = 1.0f;
  for (int tap = 1; tap < tap_count; ++tap) {
    const double w = std::exp(-(static_cast<double>(tap) * static_cast<double>(tap)) * inv2sigma2);
    weights[tap] = static_cast<float>(w);
    full_weight += 2.0 * w;
  }
  if (full_weight > 0.0) {
    for (int tap = 0; tap < tap_count; ++tap) {
      weights[tap] = static_cast<float>(static_cast<double>(weights[tap]) / full_weight);
    }
  }
}

void UpdateHsLocalTonePayload(OperatorParams& params) {
  params.hs_local_tone_enabled_ = true;
  params.hs_base_radius_        = 18.0f;
  BuildGaussianKernel(params.hs_base_radius_, 48, params.hs_base_gaussian_tap_count_,
                      params.hs_base_gaussian_weights_);

  params.hs_shadow_log_pivot_    = -3.35f;
  params.hs_shadow_log_width_    = 0.62f;
  params.hs_highlight_log_pivot_ = -2.80f;
  params.hs_highlight_log_width_ = 3.65f;
}

void UpdateSharedToneCurvePayload(OperatorParams& params) {
  const bool shadows_active = params.shadows_operator_present_ && params.shadows_enabled_;
  const bool highlights_active = params.highlights_operator_present_ && params.highlights_enabled_;
  const auto curve = detail::BuildSharedToneCurve(shadows_active, params.shadows_slider_value_,
                                                  highlights_active, params.highlights_slider_value_);
  detail::StoreSharedToneCurve(curve, params);
  params.shared_tone_curve_apply_in_shadows_    = shadows_active;
  params.shared_tone_curve_apply_in_highlights_ = (!shadows_active) && highlights_active;
}
}  // namespace

void ShadowsOp::SetParams(const nlohmann::json& params) {
  float value = 0.0f;
  bool  found = false;

  if (params.is_object() && params.contains(script_name_)) {
    value = params[script_name_].get<float>();
    found = true;
  } else if (params.is_array() && params.size() == 2) {
    // Backward compatibility for legacy snapshots serialized as ["shadows", value].
    try {
      if (params[0].is_string() && params[0].get<std::string>() == script_name_) {
        value = params[1].get<float>();
        found = true;
      }
    } catch (...) {
    }
  }

  if (!found) {
    offset_ = 0.0f;
  } else {
    offset_         = value;
    curve_.control_ = offset_ / 80.0f;
    curve_.toe_end_ = std::clamp(0.55f, 0.0f, 1.0f);
    curve_.m0_      = 1.0f + curve_.control_ * curve_.slope_range_;
    curve_.x1_      = curve_.toe_end_;
    curve_.y1_      = curve_.x1_;
    curve_.dx_      = curve_.x1_ - curve_.x0_;
  }
}

void ShadowsOp::SetGlobalParams(OperatorParams& params) const {
  params.shadows_operator_present_ = true;
  params.shadows_slider_value_     = offset_;
  params.shadows_offset_ = (offset_ * kShadowsAdjustmentStrengthScale) / 80.0f;
  params.shadows_m0_     = 1.0f + params.shadows_offset_ * curve_.slope_range_;
  params.shadows_x0_     = curve_.x0_;
  params.shadows_x1_     = curve_.x1_;
  params.shadows_y0_     = curve_.y0_;
  params.shadows_y1_     = curve_.y1_;
  params.shadows_m1_     = curve_.m1_;
  params.shadows_dx_     = curve_.dx_;
  UpdateSharedToneCurvePayload(params);
  UpdateHsLocalTonePayload(params);
}

void ShadowsOp::EnableGlobalParams(OperatorParams& params, bool enable) {
  params.shadows_enabled_ = enable;
}
}  // namespace alcedo
