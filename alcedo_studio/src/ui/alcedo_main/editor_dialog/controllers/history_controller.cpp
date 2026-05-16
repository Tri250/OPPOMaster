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

auto SeedWorkingVersionFromActive(sl_element_id_t element_id,
                                  const std::shared_ptr<EditHistoryGuard>& history_guard)
    -> WorkingVersion {
  try {
    if (history_guard && history_guard->history_) {
      auto& active = history_guard->history_->GetActiveVersion();
      const auto version_id = active.GetVersionID();
      return WorkingVersion(element_id, version_id, ParamsForVersion(history_guard, version_id),
                            active.GetAllEditTransactions(), active.GetCursor());
    }
  } catch (...) {
  }
  return WorkingVersion(element_id, Hash128{});
}

auto SeedWorkingVersionFromVersion(sl_element_id_t element_id, const Hash128& version_id,
                                   const std::shared_ptr<EditHistoryGuard>& history_guard)
    -> WorkingVersion {
  if (history_guard && history_guard->history_) {
    try {
      auto& version = history_guard->history_->GetVersion(version_id);
      return WorkingVersion(element_id, version_id, ParamsForVersion(history_guard, version_id),
                            version.GetAllEditTransactions(), version.GetCursor());
    } catch (...) {
    }
  }
  return WorkingVersion(element_id, Hash128{});
}

}  // namespace alcedo::ui::controllers
