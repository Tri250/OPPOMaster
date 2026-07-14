//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include <cstdint>
#include <list>
#include <memory>
#include <optional>
#include <unordered_map>
#include <vector>

#include "edit/pipeline/pipeline.hpp"
#include "type/hash_type.hpp"
#include "type/type.hpp"
#include "utils/diagnostics/app_logging.hpp"
#include "version.hpp"

#pragma once

namespace alcedo {

class VersionNode {
 public:
  Version&                        ver_ref_;
  p_hash_t                        commit_id_;
  history_id_t                    parent_version_id_{};            // Parent in the tree (empty for root)
  std::vector<history_id_t>       children_ids_{};                // Children in the tree
  history_id_t                    active_child_id_{};              // Which child branch is currently "active"

 public:
  VersionNode(Version& ver);

  auto HasChildren() const -> bool { return !children_ids_.empty(); }
  auto IsBranchPoint() const -> bool { return children_ids_.size() > 1; }
};

struct BranchInfo {
  history_id_t              tip_id;           // The tip (leaf) version of this branch
  history_id_t              fork_id;          // Where this branch forks from the parent branch
  std::vector<history_id_t> path;             // Version IDs along this branch from fork to tip
  std::string               display_name;     // Human-readable branch name
};

using history_id_t = Hash128;
/**
 * @brief A history of alternate looks for a specific image.
 *
 * Each Version is a user-visible look with its own transaction timeline. Versions replay from the
 * image-specific import baseline rather than from one another; cached materialized params are an
 * internal acceleration detail only.
 */
class EditHistory {
 private:
  history_id_t                              history_id_;
  sl_element_id_t                           bound_image_;

  std::time_t                               added_time_;
  std::time_t                               last_modified_time_;

  std::list<VersionNode>                    version_order_;

  std::unordered_map<history_id_t, Version> version_storage_;
  history_id_t                              default_version_id_{};
  history_id_t                              active_version_id_{};
  nlohmann::json                            import_pipeline_params_ = nlohmann::json::object();
  std::optional<nlohmann::json>             active_pipeline_params_ = std::nullopt;
  uint64_t                                  branch_counter_ = 0;   // Counter for generating branch names

  void                                      CalculateHistoryID();
  void                                      EnsureDefaultVersion();
  auto                                      FindNode(history_id_t ver_id) -> std::list<VersionNode>::iterator;
  auto                                      FindNode(history_id_t ver_id) const -> std::list<VersionNode>::const_iterator;
  auto                                      TraceBranchPath(history_id_t tip_id) const -> std::vector<history_id_t>;
  void                                      RebuildNodeIndex();

 public:
  EditHistory(sl_element_id_t bound_image);
  void SetAddTime();
  void SetLastModifiedTime();
  auto GetAddTime() const -> std::time_t;
  auto GetLastModifiedTime() const -> std::time_t;

  auto GetHistoryId() const -> history_id_t;
  auto GetBoundImage() const -> sl_element_id_t;

  auto GetVersion(history_id_t ver_id) -> Version&;
  auto GetDefaultVersion() -> Version&;
  auto GetDefaultVersionID() const -> history_id_t { return default_version_id_; }
  auto GetActiveVersionID() const -> history_id_t { return active_version_id_; }
  auto GetActiveVersion() -> Version&;
  auto GetActiveVersionHash() -> Hash128;
  auto CloneForFile(sl_element_id_t bound_image) const -> std::shared_ptr<EditHistory>;
  auto GetImportPipelineParams() const -> const nlohmann::json& { return import_pipeline_params_; }
  void SetImportPipelineParams(nlohmann::json params);
  auto ReconstructPipelineParamsForVersion(history_id_t ver_id) -> std::optional<nlohmann::json>;
  auto CreateVersion(std::string display_name = {}) -> history_id_t;
  auto CommitVersion(Version&& ver) -> history_id_t;
  void RenameVersion(history_id_t ver_id, std::string display_name);

  auto RemoveVersion(history_id_t ver_id) -> bool;
  void SetActiveVersionID(history_id_t ver_id);
  void UpdateVersionFromWorkingVersion(history_id_t ver_id, const WorkingVersion& working_version,
                                       const nlohmann::json& head_pipeline_params);

  auto GetVersions() const -> const std::list<VersionNode>& { return version_order_; }

  // ── Branching support ──────────────────────────────────────────────────
  /**
   * @brief Switch to a specific version, activating its branch.
   * Updates active_child_id_ along the path from root to the target.
   */
  void SwitchBranch(history_id_t ver_id);

  /**
   * @brief Return information about all branches (paths from fork points to tips).
   */
  auto GetBranches() const -> std::vector<BranchInfo>;

  /**
   * @brief Navigate to the parent of the current active version (history-level undo).
   * @return true if navigated successfully, false if already at root.
   */
  auto UndoVersion() -> bool;

  /**
   * @brief Navigate to the active child of the current active version (history-level redo).
   * Follows the active_child_id_ path for branch-aware navigation.
   * @return true if navigated successfully, false if no child or no active child.
   */
  auto RedoVersion() -> bool;

  /**
   * @brief Check if a version is at a branch tip (has no children).
   */
  auto IsBranchTip(history_id_t ver_id) const -> bool;

  /**
   * @brief Get the parent version ID of a given version.
   * @return Parent version ID, or empty Hash128 if it's the root.
   */
  auto GetParentVersionID(history_id_t ver_id) const -> history_id_t;

  /**
   * @brief Get the children version IDs of a given version.
   */
  auto GetChildrenVersionIDs(history_id_t ver_id) const -> std::vector<history_id_t>;
  // ── End branching support ─────────────────────────────────────────────

  auto ToJSON() const -> nlohmann::json;
  void FromJSON(const nlohmann::json& j);
};
};  // namespace alcedo
