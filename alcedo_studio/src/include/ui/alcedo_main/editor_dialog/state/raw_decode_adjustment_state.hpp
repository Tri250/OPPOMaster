//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <string>

#include "edit/pipeline/default_pipeline_params.hpp"

namespace alcedo::ui {

struct RawDecodeAdjustmentState {
  bool raw_highlights_reconstruct_ = true;
  bool lens_calib_enabled_ = pipeline_defaults::kCleanBaselineLensCalibEnabled;
  std::string lens_override_make_{};
  std::string lens_override_model_{};
};

}  // namespace alcedo::ui
