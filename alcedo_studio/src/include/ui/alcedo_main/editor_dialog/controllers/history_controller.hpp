//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <memory>

#include "app/history_mgmt_service.hpp"
#include "edit/history/version.hpp"

namespace alcedo::ui::controllers {

auto SeedWorkingVersionFromLatest(sl_element_id_t element_id,
                                  const std::shared_ptr<EditHistoryGuard>& history_guard)
    -> WorkingVersion;

auto SeedWorkingVersionFromParent(sl_element_id_t element_id,
                                  const Hash128& parent_id,
                                  bool incremental_mode,
                                  const std::shared_ptr<EditHistoryGuard>& history_guard)
    -> WorkingVersion;

auto CommitWorkingVersion(const std::shared_ptr<EditHistoryMgmtService>& history_service,
                          const std::shared_ptr<EditHistoryGuard>& history_guard,
                          WorkingVersion&& working_version,
                          const nlohmann::json& base_pipeline_params,
                          const nlohmann::json& head_pipeline_params) -> history_id_t;

}  // namespace alcedo::ui::controllers
