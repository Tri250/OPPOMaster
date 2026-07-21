//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ai/ai_rating.hpp"

namespace alcedo {

auto AiRating::IsValid() const -> bool {
  return file_id_ != 0 && !task_id_.empty() && !provider_id_.empty() &&
         !model_id_.empty() && rating_ >= kMinRating && rating_ <= kMaxRating;
}

auto AiRating::IsValidReasonsOnly() const -> bool {
  // The rating value is intentionally ignored: a 7a reasons row carries `rating_ = 0`
  // as a sentinel because the real star lives in the EXIF/metadata `Rating` column. Only
  // the file key, provider/model identity, and non-empty reasons are required.
  return file_id_ != 0 && !task_id_.empty() && !provider_id_.empty() &&
         !model_id_.empty() && !reasons_.empty();
}

}  // namespace alcedo