//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <string>
#include <vector>
#include <optional>
#include <nlohmann/json.hpp>

#include "type/hash_type.hpp"
#include "type/type.hpp"

namespace alcedo {

/// Represents an AI labeling result attached to an edit history version.
struct AiLabelEntry {
  std::string              label_id;          // Canonical label ID
  std::string              display_name;      // User-visible label name
  float                    confidence = 0.0f; // Confidence score [0,1]
  std::string              provider_id;       // AI provider that generated this
  std::string              model_id;          // Model used
  std::time_t              timestamp = 0;     // When the label was assigned
  std::string              raw_response;      // Original AI response JSON
};

/// Represents an AI labeling session result that can be recorded in edit history.
struct AiLabelHistoryRecord {
  sl_element_id_t                bound_image;
  history_id_t                   version_id;
  std::vector<AiLabelEntry>      labels;
  std::string                     session_id;      // Unique session identifier
  std::time_t                     timestamp = 0;
  bool                            applied = false;  // Whether labels were applied to metadata

  auto ToJSON() const -> nlohmann::json;
  void FromJSON(const nlohmann::json& j);
};

/// Integrates AI labeling results with the edit history system.
///
/// Responsibilities:
/// - Record AI labeling as a history entry
/// - Allow undo/redo of AI label assignments
/// - Show AI annotations in version history
/// - Ensure AI labels persist across version switches
class AiLabelHistoryIntegration {
 public:
  AiLabelHistoryIntegration() = default;

  /// Record an AI labeling result as a history entry.
  void RecordAiLabels(const AiLabelHistoryRecord& record);

  /// Get the AI label history for a specific image and version.
  auto GetLabelsForVersion(sl_element_id_t image, history_id_t version) const
      -> std::vector<AiLabelEntry>;

  /// Get the most recent AI labels for an image (across all versions).
  auto GetLatestLabels(sl_element_id_t image) const -> std::vector<AiLabelEntry>;

  /// Undo the most recent AI label assignment for an image.
  auto UndoLastAiLabels(sl_element_id_t image) -> bool;

  /// Check if a version has AI label annotations.
  auto VersionHasAiLabels(sl_element_id_t image, history_id_t version) const -> bool;

  /// Get all AI label history records for an image.
  auto GetHistoryForImage(sl_element_id_t image) const -> std::vector<AiLabelHistoryRecord>;

  /// Serialize all records to JSON for persistence.
  auto ToJSON() const -> nlohmann::json;

  /// Deserialize records from JSON.
  void FromJSON(const nlohmann::json& j);

  /// Clear all records.
  void Clear();

 private:
  /// Key: bound_image -> list of records (ordered by timestamp)
  std::unordered_map<sl_element_id_t, std::vector<AiLabelHistoryRecord>> records_;
};

}  // namespace alcedo
