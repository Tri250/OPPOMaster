//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ai/ai_description.hpp"

#include <exception>
#include <string>
#include <vector>

#include <json.hpp>

namespace alcedo {

auto AiDescription::Tags() const -> std::vector<std::string> {
  if (tags_json_.empty()) {
    return {};
  }
  try {
    return nlohmann::json::parse(tags_json_).get<std::vector<std::string>>();
  } catch (const std::exception&) {
    // A malformed store is a data issue, not a caller error: surface "no tags" rather
    // than propagating an exception through the app/search path.
    return {};
  }
}

void AiDescription::SetTags(const std::vector<std::string>& tags) {
  tags_json_ = nlohmann::json(tags).dump();
}

auto AiDescription::IsValid() const -> bool {
  return file_id_ != 0 && !task_id_.empty() && !provider_id_.empty() && !model_id_.empty();
}

}  // namespace alcedo