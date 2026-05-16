//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstddef>
#include <functional>
#include <memory>
#include <optional>

#include <QString>
#include <json.hpp>

#include "app/history_mgmt_service.hpp"
#include "app/pipeline_service.hpp"
#include "edit/history/version.hpp"

class QLabel;
class QListWidget;
class QListWidgetItem;
class QPushButton;

namespace alcedo::ui::versioning {

struct VersionUiContext {
  QLabel*      history_status = nullptr;
  QPushButton* undo_tx_btn    = nullptr;
  QListWidget* version_log    = nullptr;
  QListWidget* tx_stack       = nullptr;
};

struct VersionUiCallbacks {
  std::function<void(const QString&)> request_rename_version;
};

struct ResolvedVersionSelection {
  Hash128  version_id{};
  Version* version = nullptr;
};

struct CursorMoveResult {
  bool    moved = false;
  QString error;
};

auto MakeHistoryCursorLabel(size_t cursor, size_t total) -> QString;

auto ReconstructPipelineParamsForVersion(Version& version,
                                         const std::shared_ptr<EditHistoryGuard>& history_guard)
    -> std::optional<nlohmann::json>;

auto ResolveSelectedVersion(QListWidgetItem* item,
                            const std::shared_ptr<EditHistoryGuard>& history_guard,
                            ResolvedVersionSelection* out_selection,
                            QString* error) -> bool;

auto ResolveVersionId(const QString& version_id_str,
                      const std::shared_ptr<EditHistoryGuard>& history_guard,
                      ResolvedVersionSelection* out_selection,
                      QString* error) -> bool;

auto UndoLastTransaction(WorkingVersion& working_version,
                         const std::shared_ptr<PipelineGuard>& pipeline_guard) -> CursorMoveResult;

auto MoveCursorTo(WorkingVersion& working_version, size_t target_cursor,
                  const std::shared_ptr<PipelineGuard>& pipeline_guard) -> CursorMoveResult;

auto SeedWorkingVersionFromActive(sl_element_id_t element_id,
                                  const std::shared_ptr<EditHistoryGuard>& history_guard)
    -> WorkingVersion;

auto SeedWorkingVersionFromVersion(sl_element_id_t element_id, const Hash128& version_id,
                                   const std::shared_ptr<EditHistoryGuard>& history_guard)
    -> WorkingVersion;

void PersistWorkingVersion(const std::shared_ptr<EditHistoryMgmtService>& history_service,
                           const std::shared_ptr<EditHistoryGuard>& history_guard,
                           const WorkingVersion& working_version,
                           const std::shared_ptr<PipelineGuard>& pipeline_guard);

void UpdateVersionUi(const VersionUiContext& ui, const VersionUiCallbacks& callbacks,
                     const WorkingVersion& working_version,
                     const std::shared_ptr<EditHistoryGuard>& history_guard,
                     const std::function<void()>& refresh_selection_styles);

}  // namespace alcedo::ui::versioning
