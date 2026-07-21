//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/ai_label_history.hpp"

#include <algorithm>
#include <chrono>

namespace alcedo {

auto AiLabelHistoryRecord::ToJSON() const -> nlohmann::json {
  nlohmann::json j;
  j["bound_image"] = bound_image;
  j["version_id"]  = version_id.ToString();
  j["session_id"]  = session_id;
  j["timestamp"]   = timestamp;
  j["applied"]     = applied;

  auto labels_arr = nlohmann::json::array();
  for (const auto& label : labels) {
    nlohmann::json lj;
    lj["label_id"]     = label.label_id;
    lj["display_name"] = label.display_name;
    lj["confidence"]   = label.confidence;
    lj["provider_id"]  = label.provider_id;
    lj["model_id"]     = label.model_id;
    lj["timestamp"]    = label.timestamp;
    lj["raw_response"] = label.raw_response;
    labels_arr.push_back(lj);
  }
  j["labels"] = labels_arr;

  return j;
}

void AiLabelHistoryRecord::FromJSON(const nlohmann::json& j) {
  if (!j.is_object()) return;
  bound_image = j.value("bound_image", sl_element_id_t{0});
  version_id  = Hash128::FromString(j.value("version_id", std::string{}));
  session_id  = j.value("session_id", std::string{});
  timestamp   = j.value("timestamp", std::time_t{0});
  applied     = j.value("applied", false);

  labels.clear();
  if (j.contains("labels") && j["labels"].is_array()) {
    for (const auto& lj : j["labels"]) {
      AiLabelEntry entry;
      entry.label_id      = lj.value("label_id", std::string{});
      entry.display_name  = lj.value("display_name", std::string{});
      entry.confidence    = lj.value("confidence", 0.0f);
      entry.provider_id   = lj.value("provider_id", std::string{});
      entry.model_id      = lj.value("model_id", std::string{});
      entry.timestamp     = lj.value("timestamp", std::time_t{0});
      entry.raw_response  = lj.value("raw_response", std::string{});
      labels.push_back(std::move(entry));
    }
  }
}

void AiLabelHistoryIntegration::RecordAiLabels(const AiLabelHistoryRecord& record) {
  auto& image_records = records_[record.bound_image];

  // Check if a record with the same session_id already exists
  auto it = std::find_if(image_records.begin(), image_records.end(),
                          [&record](const AiLabelHistoryRecord& r) {
                            return r.session_id == record.session_id;
                          });

  if (it != image_records.end()) {
    // Update existing record
    *it = record;
  } else {
    image_records.push_back(record);
  }

  // Keep records sorted by timestamp
  std::sort(image_records.begin(), image_records.end(),
            [](const AiLabelHistoryRecord& a, const AiLabelHistoryRecord& b) {
              return a.timestamp < b.timestamp;
            });
}

auto AiLabelHistoryIntegration::GetLabelsForVersion(
    sl_element_id_t image, history_id_t version) const
    -> std::vector<AiLabelEntry> {
  auto it = records_.find(image);
  if (it == records_.end()) return {};

  std::vector<AiLabelEntry> result;
  for (const auto& record : it->second) {
    if (record.version_id == version) {
      result.insert(result.end(), record.labels.begin(), record.labels.end());
    }
  }
  return result;
}

auto AiLabelHistoryIntegration::GetLatestLabels(
    sl_element_id_t image) const -> std::vector<AiLabelEntry> {
  auto it = records_.find(image);
  if (it == records_.end() || it->second.empty()) return {};

  // Return labels from the most recent record
  return it->second.back().labels;
}

auto AiLabelHistoryIntegration::UndoLastAiLabels(sl_element_id_t image) -> bool {
  auto it = records_.find(image);
  if (it == records_.end() || it->second.empty()) return false;

  it->second.pop_back();
  if (it->second.empty()) {
    records_.erase(it);
  }
  return true;
}

auto AiLabelHistoryIntegration::VersionHasAiLabels(
    sl_element_id_t image, history_id_t version) const -> bool {
  auto it = records_.find(image);
  if (it == records_.end()) return false;

  return std::any_of(it->second.begin(), it->second.end(),
                      [&version](const AiLabelHistoryRecord& r) {
                        return r.version_id == version && !r.labels.empty();
                      });
}

auto AiLabelHistoryIntegration::GetHistoryForImage(
    sl_element_id_t image) const -> std::vector<AiLabelHistoryRecord> {
  auto it = records_.find(image);
  if (it == records_.end()) return {};
  return it->second;
}

auto AiLabelHistoryIntegration::ToJSON() const -> nlohmann::json {
  nlohmann::json j = nlohmann::json::object();
  for (const auto& [image, image_records] : records_) {
    nlohmann::json records_arr = nlohmann::json::array();
    for (const auto& record : image_records) {
      records_arr.push_back(record.ToJSON());
    }
    j[std::to_string(image)] = records_arr;
  }
  return j;
}

void AiLabelHistoryIntegration::FromJSON(const nlohmann::json& j) {
  records_.clear();
  if (!j.is_object()) return;

  for (const auto& [key, value] : j.items()) {
    try {
      sl_element_id_t image = std::stoull(key);
      if (!value.is_array()) continue;

      std::vector<AiLabelHistoryRecord> image_records;
      for (const auto& record_json : value) {
        AiLabelHistoryRecord record;
        record.FromJSON(record_json);
        image_records.push_back(std::move(record));
      }
      records_[image] = std::move(image_records);
    } catch (...) {
      continue;
    }
  }
}

void AiLabelHistoryIntegration::Clear() {
  records_.clear();
}

}  // namespace alcedo
