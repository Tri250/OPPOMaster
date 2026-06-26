//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ai/ai_rating.hpp"

namespace alcedo {

auto AiRating::IsValid() const -> bool {
  return file_id_ != 0 && !task_id_.empty() && !provider_id_.empty() &&
         !model_id_.empty() && rating_ >= kMinRating && rating_ <= kMaxRating;
}

}  // namespace alcedo