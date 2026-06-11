//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "edit/operators/cst/film_grain_op.hpp"

#include <algorithm>
#include <memory>

namespace alcedo {

FilmGrainOp::FilmGrainOp() { ComputeScale(); }

FilmGrainOp::FilmGrainOp(float strength) : strength_(strength) { ComputeScale(); }

FilmGrainOp::FilmGrainOp(const nlohmann::json& params) { SetParams(params); }

void FilmGrainOp::Apply(std::shared_ptr<ImageBuffer>) {}

void FilmGrainOp::ApplyGPU(std::shared_ptr<ImageBuffer>) {}

auto FilmGrainOp::GetParams() const -> nlohmann::json {
  return {{std::string(script_name_), {{"strength", strength_}}}};
}

void FilmGrainOp::SetParams(const nlohmann::json& params) {
  strength_ = 0.0f;
  if (params.contains(script_name_)) {
    const auto& film_grain = params.at(script_name_);
    if (film_grain.is_number()) {
      strength_ = film_grain.get<float>();
    } else if (film_grain.is_object() && film_grain.contains("strength") &&
               film_grain.at("strength").is_number()) {
      strength_ = film_grain.at("strength").get<float>();
    }
  }
  ComputeScale();
}

void FilmGrainOp::SetGlobalParams(OperatorParams&) const {}

void FilmGrainOp::EnableGlobalParams(OperatorParams&, bool) {}

void FilmGrainOp::ComputeScale() {
  strength_       = std::clamp(strength_, 0.0f, 100.0f);
  strength_scale_ = strength_ / 100.0f;
}

}  // namespace alcedo
