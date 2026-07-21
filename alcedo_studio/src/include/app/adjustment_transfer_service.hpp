//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <optional>
#include <set>
#include <span>
#include <string>
#include <vector>

#include "app/history_mgmt_service.hpp"
#include "app/pipeline_service.hpp"
#include "edit/operators/op_base.hpp"
#include "edit/pipeline/pipeline.hpp"
#include "json.hpp"
#include "type/type.hpp"

namespace alcedo {

struct AdjustmentTransferEntry {
  PipelineStageName stage_         = PipelineStageName::Stage_Count;
  OperatorType      operator_type_ = OperatorType::UNKNOWN;
  bool              enabled_       = true;
  bool              merge_params_  = false;
  nlohmann::json    params_        = nlohmann::json::object();
};

struct AdjustmentTransferPackage {
  std::string                          schema_ = "alcedo.adjustment_transfer.v1";
  std::vector<AdjustmentTransferEntry> operators_;

  [[nodiscard]] auto                   Empty() const -> bool { return operators_.empty(); }
};

struct AdjustmentTransferSelection {
  bool                                  include_geometry_                          = true;
  bool                                  include_tone_                              = true;
  bool                                  include_color_                             = true;
  bool                                  include_color_temperature_                 = true;
  bool                                  include_detail_                            = true;
  bool                                  include_output_transform_                  = true;

  bool                                  include_image_loading_                     = false;
  bool                                  include_lens_calibration_                  = false;

  // Runtime-resolved values are image-derived. Keep these false for normal copy/paste.
  bool                                  include_color_temperature_resolved_values_ = false;
  bool                                  include_lens_calibration_runtime_metadata_ = false;

  // Optional UI/SDK fine selection. If set, only listed operators can be captured.
  std::optional<std::set<OperatorType>> operator_filter_                           = std::nullopt;
};

struct AdjustmentApplyFailure {
  sl_element_id_t file_id_ = 0;
  std::string     message_;
};

struct AdjustmentApplyResult {
  std::vector<sl_element_id_t>        applied_ids_;
  std::vector<sl_element_id_t>        unchanged_ids_;
  std::vector<AdjustmentApplyFailure> failures_;
};

enum class AdjustmentVersionApplyMode {
  kPaste,
  kMerge,
};

class AdjustmentTransferService final {
 public:
  AdjustmentTransferService() = delete;

  [[nodiscard]] static auto Capture(PipelineExecutor&                  source,
                                    const AdjustmentTransferSelection& selection = {})
      -> AdjustmentTransferPackage;

  // Accepts stable external JSON, for example:
  // {"schema":"alcedo.adjustment_transfer.v1","operators":[{"operator":"exposure",
  // "params":{"exposure":2.0}}]}
  [[nodiscard]] static auto ImportPackage(const nlohmann::json& package_json)
      -> AdjustmentTransferPackage;
  [[nodiscard]] static auto ExportPackage(const AdjustmentTransferPackage& package)
      -> nlohmann::json;

  // Returns true when at least one target operator actually changed.
  static auto Apply(PipelineExecutor& target, const AdjustmentTransferPackage& package) -> bool;

  // Loads, applies, saves, and syncs selected pipelines. The returned applied ids are the ids whose
  // pipelines changed; callers can invalidate thumbnail caches and refresh album rows for them.
  // This low-level overload is intended for direct pipeline tooling; UI/CLI project operations
  // should prefer the versioned overload below so pasted adjustments participate in edit history.
  [[nodiscard]] static auto Apply(PipelineMgmtService&             pipeline_service,
                                  std::span<const sl_element_id_t> target_ids,
                                  const AdjustmentTransferPackage& package)
      -> AdjustmentApplyResult;

  // Creates a new active version for each changed target and checkouts the target image to that
  // version. kPaste records one edit transaction per applied transfer entry. kMerge materializes
  // the merged final pipeline params into a transaction-free version.
  [[nodiscard]] static auto Apply(
      PipelineMgmtService& pipeline_service, EditHistoryMgmtService& history_service,
      std::span<const sl_element_id_t> target_ids, const AdjustmentTransferPackage& package,
      std::string                version_display_name = "",
      AdjustmentVersionApplyMode mode                 = AdjustmentVersionApplyMode::kPaste)
      -> AdjustmentApplyResult;
};

}  // namespace alcedo
