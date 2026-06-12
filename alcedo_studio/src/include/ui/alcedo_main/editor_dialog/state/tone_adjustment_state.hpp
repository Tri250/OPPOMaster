//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <vector>

#include "edit/pipeline/default_pipeline_params.hpp"
#include "ui/alcedo_main/editor_dialog/modules/curve.hpp"

namespace alcedo::ui {

struct ToneAdjustmentState {
  float                exposure_     = pipeline_defaults::kCleanBaselineExposure;
  float                contrast_     = 0.0f;
  float                blacks_       = 0.0f;
  float                whites_       = 0.0f;
  float                shadows_      = 0.0f;
  float                highlights_   = 0.0f;
  std::vector<QPointF> curve_points_ = curve::DefaultCurveControlPoints();
  float                saturation_   = pipeline_defaults::kCleanBaselineSaturation;
  float                sharpen_      = 0.0f;
  float                clarity_      = 0.0f;
  float                film_grain_   = 0.0f;
  float                halation_     = 0.0f;
};

}  // namespace alcedo::ui
