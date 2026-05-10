//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ui/alcedo_main/editor_dialog/controllers/history_controller.hpp"

#include <optional>

namespace alcedo::ui::controllers {
namespace {
auto ParamsForVersion(const std::shared_ptr<EditHistoryGuard>& history_guard, const Hash128& id)
    -> std::optional<nlohmann::json> {
  if (!history_guard || !history_guard->history_) {
    return std::nullopt;
  }
  return history_guard->history_->ReconstructPipelineParamsForVersion(id);
}
}  // namespace

auto SeedWorkingVersionFromLatest(sl_element_id_t element_id,
                                  const std::shared_ptr<EditHistoryGuard>& history_guard)
    -> WorkingVersion {
  try {
    if (history_guard && history_guard->history_) {
      const auto parent_id = history_guard->history_->GetLatestVersion().ver_ref_.GetVersionID();
      return WorkingVersion(element_id, parent_id, ParamsForVersion(history_guard, parent_id));
    }
  } catch (...) {
  }
  return WorkingVersion(element_id, Hash128{});
}

auto SeedWorkingVersionFromParent(sl_element_id_t element_id,
                                  const Hash128& parent_id,
                                  bool incremental_mode,
                                  const std::shared_ptr<EditHistoryGuard>& history_guard)
    -> WorkingVersion {
  if (incremental_mode) {
    return WorkingVersion(element_id, parent_id, ParamsForVersion(history_guard, parent_id));
  }
  if (history_guard && history_guard->history_) {
    const auto root_id = history_guard->history_->GetRootVersionID();
    return WorkingVersion(element_id, root_id, ParamsForVersion(history_guard, root_id));
  }
  return WorkingVersion(element_id, Hash128{});
}

auto CommitWorkingVersion(const std::shared_ptr<EditHistoryMgmtService>& history_service,
                          const std::shared_ptr<EditHistoryGuard>& history_guard,
                          WorkingVersion&& working_version,
                          const nlohmann::json& base_pipeline_params,
                          const nlohmann::json& head_pipeline_params) -> history_id_t {
  return history_service->CommitVersion(history_guard, std::move(working_version),
                                        base_pipeline_params, head_pipeline_params);
}

}  // namespace alcedo::ui::controllers
