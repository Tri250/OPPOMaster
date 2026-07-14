//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "edit/history/edit_history.hpp"

#include <xxhash.h>

#include <algorithm>
#include <array>
#include <chrono>
#include <cstddef>
#include <cstdint>
#include <stdexcept>
#include <utility>
#include <string>

#include "edit/pipeline/pipeline_cpu.hpp"
#include "type/hash_type.hpp"
#include "type/type.hpp"
#include "utils/clock/time_provider.hpp"

namespace alcedo {
namespace {
auto DefaultPipelineParams() -> nlohmann::json {
  CPUPipelineExecutor exec;
  return exec.ExportPipelineParams();
}

}  // namespace

VersionNode::VersionNode(Version& ver) : ver_ref_(ver) {}

EditHistory::EditHistory(sl_element_id_t bound_image) : bound_image_(bound_image) {
  SetAddTime();
  import_pipeline_params_ = DefaultPipelineParams();
  EnsureDefaultVersion();
  history_id_ = Hash128::Blend(Hash128::Compute(&added_time_, sizeof(added_time_)),
                               Hash128::Compute(&bound_image, sizeof(bound_image)));
}

void EditHistory::EnsureDefaultVersion() {
  if (version_storage_.find(default_version_id_) != version_storage_.end()) {
    return;
  }

  Version default_version                 = Version::Default(bound_image_, import_pipeline_params_);
  default_version_id_                     = default_version.GetVersionID();
  active_version_id_                      = default_version_id_;
  active_pipeline_params_                 = default_version.GetMaterializedParams();
  version_storage_[default_version_id_]   = std::move(default_version);
  version_order_.clear();
  version_order_.emplace_back(version_storage_[default_version_id_]);
  version_order_.back().commit_id_ = static_cast<p_hash_t>(version_order_.size());
  // Root node: parent_version_id_ remains empty (default-constructed Hash128)
}

void EditHistory::CalculateHistoryID() {
  history_id_ = Hash128::Blend(
      history_id_,
      Hash128::Blend(active_version_id_,
                     Hash128::Compute(&last_modified_time_, sizeof(last_modified_time_))));
}

void EditHistory::SetAddTime() {
  added_time_         = std::chrono::system_clock::to_time_t(TimeProvider::Now());
  last_modified_time_ = added_time_;
}

void EditHistory::SetLastModifiedTime() {
  last_modified_time_ = std::chrono::system_clock::to_time_t(TimeProvider::Now());
}

auto EditHistory::GetAddTime() const -> std::time_t { return added_time_; }

auto EditHistory::GetLastModifiedTime() const -> std::time_t { return last_modified_time_; }

auto EditHistory::GetHistoryId() const -> history_id_t { return history_id_; }

auto EditHistory::GetBoundImage() const -> sl_element_id_t { return bound_image_; }

auto EditHistory::GetVersion(history_id_t ver_id) -> Version& {
  EnsureDefaultVersion();
  if (version_storage_.find(ver_id) == version_storage_.end()) {
    throw std::runtime_error("Version not found");
  }
  return version_storage_[ver_id];
}

auto EditHistory::GetDefaultVersion() -> Version& {
  EnsureDefaultVersion();
  return GetVersion(default_version_id_);
}

auto EditHistory::GetActiveVersion() -> Version& {
  EnsureDefaultVersion();
  return GetVersion(active_version_id_);
}

auto EditHistory::GetActiveVersionHash() -> Hash128 {
  EnsureDefaultVersion();
  return GetVersion(active_version_id_).GetVersionHash();
}

// ── Helper: find a VersionNode by version_id ────────────────────────────────
auto EditHistory::FindNode(history_id_t ver_id) -> std::list<VersionNode>::iterator {
  for (auto it = version_order_.begin(); it != version_order_.end(); ++it) {
    if (it->ver_ref_.GetVersionID() == ver_id) {
      return it;
    }
  }
  return version_order_.end();
}

auto EditHistory::FindNode(history_id_t ver_id) const -> std::list<VersionNode>::const_iterator {
  for (auto it = version_order_.begin(); it != version_order_.end(); ++it) {
    if (it->ver_ref_.GetVersionID() == ver_id) {
      return it;
    }
  }
  return version_order_.end();
}

// ── Helper: rebuild parent/children links from parent_version_id_ ────────────
void EditHistory::RebuildNodeIndex() {
  for (auto& node : version_order_) {
    node.children_ids_.clear();
    node.active_child_id_ = history_id_t{};
  }
  for (auto& node : version_order_) {
    const auto parent_id = node.parent_version_id_;
    if (parent_id == history_id_t{}) {
      continue;
    }
    auto parent_it = FindNode(parent_id);
    if (parent_it != version_order_.end()) {
      parent_it->children_ids_.push_back(node.ver_ref_.GetVersionID());
      // First child becomes the active child by default
      if (parent_it->active_child_id_ == history_id_t{}) {
        parent_it->active_child_id_ = node.ver_ref_.GetVersionID();
      }
    }
  }
}

// ── Helper: trace a branch path from fork point to the given tip ────────────
auto EditHistory::TraceBranchPath(history_id_t tip_id) const -> std::vector<history_id_t> {
  std::vector<history_id_t> path;
  history_id_t current = tip_id;

  // Walk up from the tip until we find a branch point or the root
  std::vector<history_id_t> reverse_path;
  while (current != history_id_t{}) {
    auto it = FindNode(current);
    if (it == version_order_.end()) {
      break;
    }
    reverse_path.push_back(current);
    const auto parent_id = it->parent_version_id_;
    // Stop at the fork point (parent has >1 children) or root
    if (parent_id != history_id_t{}) {
      auto parent_it = FindNode(parent_id);
      if (parent_it != version_order_.end() && parent_it->IsBranchPoint()) {
        break;
      }
    }
    current = parent_id;
  }

  // Reverse to get fork→tip order
  path.assign(reverse_path.rbegin(), reverse_path.rend());
  return path;
}

// ── CloneForFile ────────────────────────────────────────────────────────────
auto EditHistory::CloneForFile(sl_element_id_t bound_image) const -> std::shared_ptr<EditHistory> {
  auto clone                     = std::make_shared<EditHistory>(bound_image);
  clone->import_pipeline_params_ = import_pipeline_params_;
  clone->active_pipeline_params_ = active_pipeline_params_;
  clone->version_order_.clear();
  clone->version_storage_.clear();

  std::unordered_map<history_id_t, history_id_t> version_id_map;
  version_id_map.reserve(version_storage_.size());

  for (const auto& [old_version_id, version] : version_storage_) {
    auto cloned_version = version.CloneForImage(bound_image);
    auto new_version_id = cloned_version.GetVersionID();
    version_id_map.emplace(old_version_id, new_version_id);
    clone->version_storage_.emplace(new_version_id, std::move(cloned_version));
  }

  if (auto default_it = version_id_map.find(default_version_id_); default_it != version_id_map.end()) {
    clone->default_version_id_ = default_it->second;
  }
  if (auto active_it = version_id_map.find(active_version_id_); active_it != version_id_map.end()) {
    clone->active_version_id_ = active_it->second;
  }

  for (const auto& node : version_order_) {
    const auto source_version_id = node.ver_ref_.GetVersionID();
    const auto mapped_it         = version_id_map.find(source_version_id);
    if (mapped_it == version_id_map.end()) {
      continue;
    }

    const auto cloned_version_it = clone->version_storage_.find(mapped_it->second);
    if (cloned_version_it == clone->version_storage_.end()) {
      continue;
    }

    clone->version_order_.emplace_back(cloned_version_it->second);
    auto& new_node          = clone->version_order_.back();
    new_node.commit_id_     = node.commit_id_;
    // Map parent version ID
    if (node.parent_version_id_ != history_id_t{}) {
      auto parent_mapped = version_id_map.find(node.parent_version_id_);
      if (parent_mapped != version_id_map.end()) {
        new_node.parent_version_id_ = parent_mapped->second;
      }
    }
    // Map children IDs
    for (const auto& child_id : node.children_ids_) {
      auto child_mapped = version_id_map.find(child_id);
      if (child_mapped != version_id_map.end()) {
        new_node.children_ids_.push_back(child_mapped->second);
      }
    }
    // Map active child ID
    if (node.active_child_id_ != history_id_t{}) {
      auto active_mapped = version_id_map.find(node.active_child_id_);
      if (active_mapped != version_id_map.end()) {
        new_node.active_child_id_ = active_mapped->second;
      }
    }
  }

  clone->branch_counter_ = branch_counter_;
  clone->SetLastModifiedTime();
  clone->CalculateHistoryID();
  return clone;
}

void EditHistory::SetImportPipelineParams(nlohmann::json params) {
  import_pipeline_params_ = std::move(params);
  if (version_storage_.find(default_version_id_) != version_storage_.end()) {
    auto& default_version = version_storage_.at(default_version_id_);
    if (default_version.GetAllEditTransactions().empty()) {
      default_version.SetFinalPipelineParams(import_pipeline_params_);
      active_pipeline_params_ = import_pipeline_params_;
    }
  }
  SetLastModifiedTime();
}

auto EditHistory::ReconstructPipelineParamsForVersion(history_id_t ver_id)
    -> std::optional<nlohmann::json> {
  EnsureDefaultVersion();

  Version* version = nullptr;
  try {
    version = &GetVersion(ver_id);
  } catch (...) {
    return std::nullopt;
  }

  if (const auto snapshot = version->GetFinalPipelineParams(); snapshot.has_value()) {
    return snapshot;
  }

  try {
    CPUPipelineExecutor exec;
    exec.ImportPipelineParams(import_pipeline_params_);
    const auto& txs = version->GetAllEditTransactions();
    const size_t cursor = std::min(version->GetCursor(), txs.size());
    for (size_t i = 0; i < cursor; ++i) {
      if (!txs[i].ApplyForward(exec)) {
        return std::nullopt;
      }
    }
    return exec.ExportPipelineParams();
  } catch (...) {
    return std::nullopt;
  }
}

auto EditHistory::CreateVersion(std::string display_name) -> history_id_t {
  EnsureDefaultVersion();

  // Determine if this is a branch (active version already has children)
  bool is_branch = false;
  auto active_it = FindNode(active_version_id_);
  if (active_it != version_order_.end() && active_it->HasChildren()) {
    is_branch = true;
    ++branch_counter_;
    if (display_name.empty()) {
      display_name = "Branch " + std::to_string(branch_counter_);
    }
    qCInfo(diag::appLog, "EditHistory::CreateVersion: Creating branch from version %s",
           active_version_id_.ToString().c_str());
  } else {
    if (display_name.empty()) {
      display_name = "Version " + std::to_string(version_order_.size());
    }
  }

  Version version = Version::Empty(bound_image_, std::move(display_name), import_pipeline_params_);
  const auto ver_id = CommitVersion(std::move(version));

  if (is_branch) {
    qCInfo(diag::appLog, "EditHistory::CreateVersion: New branch version %s created",
           ver_id.ToString().c_str());
  }

  SetActiveVersionID(ver_id);
  return ver_id;
}

auto EditHistory::CommitVersion(Version&& ver) -> history_id_t {
  EnsureDefaultVersion();
  auto ver_id = ver.GetVersionID();
  if (version_storage_.find(ver_id) != version_storage_.end()) {
    throw std::runtime_error("Version already exists");
  }

  // Determine the parent: the current active version
  history_id_t parent_id = active_version_id_;

  version_storage_[ver_id] = std::move(ver);
  version_order_.emplace_back(version_storage_[ver_id]);
  version_order_.back().commit_id_        = static_cast<p_hash_t>(version_order_.size());
  version_order_.back().parent_version_id_ = parent_id;

  // Add this version as a child of the parent
  if (parent_id != history_id_t{}) {
    auto parent_it = FindNode(parent_id);
    if (parent_it != version_order_.end()) {
      parent_it->children_ids_.push_back(ver_id);
      // Update parent's active_child_id_ to this new version
      parent_it->active_child_id_ = ver_id;

      if (parent_it->IsBranchPoint()) {
        qCInfo(diag::appLog,
               "EditHistory::CommitVersion: Branch point at %s now has %zu children",
               parent_id.ToString().c_str(), parent_it->children_ids_.size());
      }
    }
  }

  active_version_id_       = ver_id;
  active_pipeline_params_  = version_storage_[ver_id].GetMaterializedParams();
  SetLastModifiedTime();
  CalculateHistoryID();
  return ver_id;
}

void EditHistory::RenameVersion(history_id_t ver_id, std::string display_name) {
  EnsureDefaultVersion();
  GetVersion(ver_id).SetDisplayName(std::move(display_name));
  SetLastModifiedTime();
  CalculateHistoryID();
}

auto EditHistory::RemoveVersion(history_id_t ver_id) -> bool {
  EnsureDefaultVersion();
  if (ver_id == default_version_id_) {
    return false;
  }
  if (version_storage_.find(ver_id) == version_storage_.end()) {
    return false;
  }

  // Disallow removing a version that has children (would create orphans)
  auto node_it = FindNode(ver_id);
  if (node_it != version_order_.end() && node_it->HasChildren()) {
    qCDebug(diag::appLog,
             "EditHistory::RemoveVersion: Cannot remove version %s with %zu children",
             ver_id.ToString().c_str(), node_it->children_ids_.size());
    return false;
  }

  // Remove this version from its parent's children list
  if (node_it != version_order_.end()) {
    const auto parent_id = node_it->parent_version_id_;
    if (parent_id != history_id_t{}) {
      auto parent_it = FindNode(parent_id);
      if (parent_it != version_order_.end()) {
        auto& children    = parent_it->children_ids_;
        auto  child_pos   = std::find(children.begin(), children.end(), ver_id);
        if (child_pos != children.end()) {
          children.erase(child_pos);
        }
        // If the removed version was the active child, pick another child or clear
        if (parent_it->active_child_id_ == ver_id) {
          if (children.empty()) {
            parent_it->active_child_id_ = history_id_t{};
          } else {
            parent_it->active_child_id_ = children.front();
          }
        }
      }
    }
  }

  version_order_.erase(node_it);
  version_storage_.erase(ver_id);
  if (active_version_id_ == ver_id) {
    active_version_id_ = default_version_id_;
    active_pipeline_params_ = version_storage_[default_version_id_].GetMaterializedParams();
  }
  SetLastModifiedTime();
  return true;
}

void EditHistory::SetActiveVersionID(history_id_t ver_id) {
  EnsureDefaultVersion();
  if (version_storage_.find(ver_id) == version_storage_.end()) {
    throw std::runtime_error("Version not found");
  }
  active_version_id_      = ver_id;
  active_pipeline_params_ = version_storage_[ver_id].GetMaterializedParams();
  SetLastModifiedTime();
}

void EditHistory::UpdateVersionFromWorkingVersion(history_id_t          ver_id,
                                                  const WorkingVersion& working_version,
                                                  const nlohmann::json& head_pipeline_params) {
  EnsureDefaultVersion();
  auto it = version_storage_.find(ver_id);
  if (it == version_storage_.end()) {
    throw std::runtime_error("Version not found");
  }
  it->second.UpdateFromWorkingVersion(working_version, head_pipeline_params);
  if (active_version_id_ == ver_id) {
    active_pipeline_params_ = head_pipeline_params;
  }
  SetLastModifiedTime();
}

// ═══════════════════════════════════════════════════════════════════════════
// Branching support implementation
// ═══════════════════════════════════════════════════════════════════════════

void EditHistory::SwitchBranch(history_id_t ver_id) {
  EnsureDefaultVersion();
  if (version_storage_.find(ver_id) == version_storage_.end()) {
    throw std::runtime_error("SwitchBranch: Version not found");
  }

  // Walk from ver_id up to the root, updating active_child_id_ along the way
  // so that RedoVersion() follows the same path back down.
  history_id_t current = ver_id;
  while (current != history_id_t{}) {
    auto it = FindNode(current);
    if (it == version_order_.end()) {
      break;
    }
    const auto parent_id = it->parent_version_id_;
    if (parent_id != history_id_t{}) {
      auto parent_it = FindNode(parent_id);
      if (parent_it != version_order_.end()) {
        parent_it->active_child_id_ = current;
      }
    }
    current = parent_id;
  }

  active_version_id_      = ver_id;
  active_pipeline_params_ = version_storage_[ver_id].GetMaterializedParams();
  SetLastModifiedTime();
  qCDebug(diag::appLog, "EditHistory::SwitchBranch: Switched to version %s",
           ver_id.ToString().c_str());
}

auto EditHistory::GetBranches() const -> std::vector<BranchInfo> {
  if (version_order_.empty()) {
    return {};
  }
  std::vector<BranchInfo> branches;

  // Collect all leaf nodes (tip of each branch)
  std::vector<history_id_t> tips;
  for (const auto& node : version_order_) {
    if (!node.HasChildren()) {
      tips.push_back(node.ver_ref_.GetVersionID());
    }
  }

  for (const auto& tip_id : tips) {
    BranchInfo info;
    info.tip_id = tip_id;
    info.path   = TraceBranchPath(tip_id);

    // The fork point is the parent of the first element in the path
    if (!info.path.empty()) {
      auto first_it = FindNode(info.path.front());
      if (first_it != version_order_.end()) {
        info.fork_id = first_it->parent_version_id_;
      }
    }

    // Build a display name from the tip version
    auto tip_it = FindNode(tip_id);
    if (tip_it != version_order_.end()) {
      info.display_name = tip_it->ver_ref_.GetDisplayName();
    }

    branches.push_back(std::move(info));
  }

  return branches;
}

auto EditHistory::UndoVersion() -> bool {
  EnsureDefaultVersion();
  auto it = FindNode(active_version_id_);
  if (it == version_order_.end()) {
    return false;
  }
  const auto parent_id = it->parent_version_id_;
  if (parent_id == history_id_t{}) {
    qCDebug(diag::appLog, "EditHistory::UndoVersion: Already at root, cannot undo");
    return false;
  }

  active_version_id_      = parent_id;
  active_pipeline_params_ = version_storage_[parent_id].GetMaterializedParams();
  SetLastModifiedTime();
  qCDebug(diag::appLog, "EditHistory::UndoVersion: Navigated to parent %s",
           parent_id.ToString().c_str());
  return true;
}

auto EditHistory::RedoVersion() -> bool {
  EnsureDefaultVersion();
  auto it = FindNode(active_version_id_);
  if (it == version_order_.end()) {
    return false;
  }
  if (!it->HasChildren()) {
    qCDebug(diag::appLog, "EditHistory::RedoVersion: No children to redo into");
    return false;
  }
  // Follow the active_child_id_ for branch-aware navigation
  const auto next_id = it->active_child_id_;
  if (next_id == history_id_t{}) {
    // Fallback: pick the first child
    active_version_id_      = it->children_ids_.front();
  } else {
    active_version_id_      = next_id;
  }
  active_pipeline_params_ = version_storage_[active_version_id_].GetMaterializedParams();
  SetLastModifiedTime();
  qCDebug(diag::appLog, "EditHistory::RedoVersion: Navigated to child %s",
           active_version_id_.ToString().c_str());
  return true;
}

auto EditHistory::IsBranchTip(history_id_t ver_id) const -> bool {
  auto it = FindNode(ver_id);
  if (it == version_order_.end()) {
    return false;
  }
  return !it->HasChildren();
}

auto EditHistory::GetParentVersionID(history_id_t ver_id) const -> history_id_t {
  auto it = FindNode(ver_id);
  if (it == version_order_.end()) {
    return history_id_t{};
  }
  return it->parent_version_id_;
}

auto EditHistory::GetChildrenVersionIDs(history_id_t ver_id) const -> std::vector<history_id_t> {
  auto it = FindNode(ver_id);
  if (it == version_order_.end()) {
    return {};
  }
  return it->children_ids_;
}

// ═══════════════════════════════════════════════════════════════════════════
// Serialization
// ═══════════════════════════════════════════════════════════════════════════

auto EditHistory::ToJSON() const -> nlohmann::json {
  nlohmann::json j;
  j["history_id"]             = history_id_.ToString();
  j["bound_image"]            = bound_image_;
  j["added_time"]             = added_time_;
  j["last_modified_time"]     = last_modified_time_;
  j["default_version_id"]     = default_version_id_.ToString();
  j["active_version_id"]      = active_version_id_.ToString();
  j["import_pipeline_params"] = import_pipeline_params_;
  j["branch_counter"]         = branch_counter_;
  if (active_pipeline_params_.has_value()) {
    j["active_pipeline_params"] = *active_pipeline_params_;
  }

  j["version_order"] = nlohmann::json::array();
  for (const auto& node : version_order_) {
    nlohmann::json node_json;
    node_json["order"]               = node.commit_id_;
    node_json["version_id"]          = node.ver_ref_.GetVersionID().ToString();
    node_json["parent_version_id"]   = node.parent_version_id_.ToString();
    // Serialize children IDs
    node_json["children_ids"]        = nlohmann::json::array();
    for (const auto& child_id : node.children_ids_) {
      node_json["children_ids"].push_back(child_id.ToString());
    }
    node_json["active_child_id"]     = node.active_child_id_.ToString();
    j["version_order"].push_back(node_json);
  }

  auto append_version = [&j](const history_id_t& ver_id, const Version& ver) {
    nlohmann::json ver_json;
    ver_json["version_id"] = ver_id.ToString();
    ver_json["version"]    = ver.ToJSON();
    j["version_storage"].push_back(ver_json);
  };

  j["version_storage"] = nlohmann::json::array();
  std::unordered_map<std::string, bool> emitted;
  for (const auto& node : version_order_) {
    const auto ver_id = node.ver_ref_.GetVersionID();
    append_version(ver_id, node.ver_ref_);
    emitted[ver_id.ToString()] = true;
  }
  for (const auto& [ver_id, ver] : version_storage_) {
    if (emitted.find(ver_id.ToString()) != emitted.end()) {
      continue;
    }
    append_version(ver_id, ver);
  }

  return j;
}

void EditHistory::FromJSON(const nlohmann::json& j) {
  if (!j.is_object() || !j.contains("history_id") || !j.contains("bound_image") ||
      !j.contains("added_time") || !j.contains("last_modified_time") ||
      !j.contains("default_version_id") || !j.contains("active_version_id") ||
      !j.contains("import_pipeline_params") || !j.contains("version_order") ||
      !j.contains("version_storage")) {
    throw std::runtime_error("EditHistory: Invalid JSON format for EditHistory");
  }

  history_id_             = Hash128::FromString(j.at("history_id").get<std::string>());
  bound_image_            = j.at("bound_image").get<sl_element_id_t>();
  added_time_             = j.at("added_time").get<std::time_t>();
  last_modified_time_     = j.at("last_modified_time").get<std::time_t>();
  default_version_id_     = Hash128::FromString(j.at("default_version_id").get<std::string>());
  active_version_id_      = Hash128::FromString(j.at("active_version_id").get<std::string>());
  import_pipeline_params_ = j.at("import_pipeline_params");
  active_pipeline_params_ = j.contains("active_pipeline_params")
                                ? std::optional<nlohmann::json>(j.at("active_pipeline_params"))
                                : std::nullopt;
  branch_counter_         = j.value("branch_counter", uint64_t{0});

  version_order_.clear();
  version_storage_.clear();
  for (const auto& ver_json : j.at("version_storage")) {
    if (!ver_json.is_object() || !ver_json.contains("version")) {
      version_storage_.clear();
      throw std::runtime_error("EditHistory: Invalid JSON format for version_storage node");
    }
    Version ver;
    ver.FromJSON(ver_json.at("version"));
    history_id_t ver_id      = ver.GetVersionID();
    version_storage_[ver_id] = std::move(ver);
  }

  for (const auto& node_json : j.at("version_order")) {
    if (!node_json.is_object() || !node_json.contains("order") ||
        !node_json.contains("version_id")) {
      version_order_.clear();
      version_storage_.clear();
      throw std::runtime_error("EditHistory: Invalid JSON format for version_order node");
    }
    const history_id_t ver_id =
        Hash128::FromString(node_json.at("version_id").get<std::string>());
    auto it = version_storage_.find(ver_id);
    if (it == version_storage_.end()) {
      continue;
    }
    VersionNode node(it->second);
    node.commit_id_ = node_json.at("order").get<p_hash_t>();

    // Load parent version ID (backward compatible: default to empty if missing)
    if (node_json.contains("parent_version_id") && node_json.at("parent_version_id").is_string()) {
      node.parent_version_id_ =
          Hash128::FromString(node_json.at("parent_version_id").get<std::string>());
    }

    // Load children IDs if available
    if (node_json.contains("children_ids") && node_json.at("children_ids").is_array()) {
      for (const auto& child_str : node_json.at("children_ids")) {
        if (child_str.is_string()) {
          node.children_ids_.push_back(Hash128::FromString(child_str.get<std::string>()));
        }
      }
    }

    // Load active child ID if available
    if (node_json.contains("active_child_id") && node_json.at("active_child_id").is_string()) {
      node.active_child_id_ =
          Hash128::FromString(node_json.at("active_child_id").get<std::string>());
    }

    version_order_.push_back(std::move(node));
  }

  // Backward compatibility: if no parent_version_id was found in the JSON,
  // reconstruct a linear chain from the version_order_ list.
  bool has_parent_info = false;
  for (const auto& node : version_order_) {
    if (node.parent_version_id_ != history_id_t{}) {
      has_parent_info = true;
      break;
    }
  }
  if (!has_parent_info && version_order_.size() > 1) {
    // Old format: build a linear chain where each node's parent is the previous node
    auto prev_it = version_order_.begin();
    auto cur_it  = std::next(prev_it);
    while (cur_it != version_order_.end()) {
      cur_it->parent_version_id_ = prev_it->ver_ref_.GetVersionID();
      prev_it->children_ids_.push_back(cur_it->ver_ref_.GetVersionID());
      prev_it->active_child_id_ = cur_it->ver_ref_.GetVersionID();
      prev_it = cur_it;
      ++cur_it;
    }
  } else if (has_parent_info) {
    // New format with parent info: rebuild children/active_child from parent links
    // if they weren't explicitly loaded from JSON
    bool needs_children_rebuild = false;
    for (const auto& node : version_order_) {
      if (!node.children_ids_.empty()) {
        break;
      }
      needs_children_rebuild = true;
    }
    if (needs_children_rebuild) {
      RebuildNodeIndex();
    }
  }

  EnsureDefaultVersion();
}
};  // namespace alcedo
