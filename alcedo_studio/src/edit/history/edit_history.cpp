//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "edit/history/edit_history.hpp"

#include <xxhash.h>

#include <array>
#include <algorithm>
#include <chrono>
#include <cstddef>
#include <cstdint>
#include <stdexcept>
#include <utility>
#include <vector>

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

auto IsZeroHash(const Hash128& id) -> bool {
  return id.low64() == 0 && id.high64() == 0;
}
}  // namespace

VersionNode::VersionNode(Version& ver) : ver_ref_(ver) {}

EditHistory::EditHistory(sl_element_id_t bound_image) : bound_image_(bound_image) {
  SetAddTime();
  EnsureRootVersion();
  history_id_ = Hash128::Blend(Hash128::Compute(&added_time_, sizeof(added_time_)),
                               Hash128::Compute(&bound_image, sizeof(bound_image)));
}

void EditHistory::EnsureRootVersion() {
  if (!IsZeroHash(root_version_id_) && version_storage_.find(root_version_id_) != version_storage_.end()) {
    return;
  }

  Version root = Version::Root(bound_image_, DefaultPipelineParams());
  root_version_id_ = root.GetVersionID();
  head_version_id_ = root_version_id_;
  head_pipeline_params_ = root.GetMaterializedParams();
  version_storage_[root_version_id_] = std::move(root);
  commit_tree_.clear();
  commit_tree_.emplace_back(version_storage_[root_version_id_]);
  commit_tree_.back().commit_id_ = static_cast<p_hash_t>(commit_tree_.size());
}

void EditHistory::CalculateHistoryID() {
  auto& last_node = GetLatestVersion();
  history_id_     = Hash128::Blend(
      history_id_,
      Hash128::Blend(last_node.ver_ref_.GetVersionID(),
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
  EnsureRootVersion();
  if (version_storage_.find(ver_id) == version_storage_.end()) {
    throw std::runtime_error("Version not found");
  }
  return version_storage_[ver_id];
}

auto EditHistory::GetRootVersion() -> Version& {
  EnsureRootVersion();
  return GetVersion(root_version_id_);
}

auto EditHistory::HasUserVersions() const -> bool {
  for (const auto& node : commit_tree_) {
    if (!node.ver_ref_.IsRoot()) {
      return true;
    }
  }
  return false;
}

auto EditHistory::ReconstructPipelineParamsForVersion(history_id_t ver_id)
    -> std::optional<nlohmann::json> {
  EnsureRootVersion();

  Version* version = nullptr;
  try {
    version = &GetVersion(ver_id);
  } catch (...) {
    return std::nullopt;
  }

  if (const auto snapshot = version->GetFinalPipelineParams(); snapshot.has_value()) {
    return snapshot;
  }

  std::vector<Version*> lineage;
  lineage.push_back(version);
  while (lineage.back()->HasParentVersion()) {
    try {
      lineage.push_back(&GetVersion(lineage.back()->GetParentVersionID()));
    } catch (...) {
      return std::nullopt;
    }
  }
  std::reverse(lineage.begin(), lineage.end());

  size_t replay_from = 0;
  nlohmann::json params = nlohmann::json::object();
  for (size_t i = lineage.size(); i > 0; --i) {
    if (const auto snapshot = lineage[i - 1]->GetFinalPipelineParams(); snapshot.has_value()) {
      params = *snapshot;
      replay_from = i;
      break;
    }
  }

  for (size_t i = replay_from; i < lineage.size(); ++i) {
    try {
      params = params.patch(lineage[i]->GetDeltaFromParent());
    } catch (...) {
      return std::nullopt;
    }
  }
  return params;
}

auto EditHistory::CommitVersion(Version&& ver) -> history_id_t {
  EnsureRootVersion();
  auto ver_id = ver.GetVersionID();
  if (version_storage_.find(ver_id) != version_storage_.end()) {
    throw std::runtime_error("Version already exists");
  }
  version_storage_[ver_id] = std::move(ver);
  commit_tree_.emplace_back(version_storage_[ver_id]);
  commit_tree_.back().commit_id_ = static_cast<p_hash_t>(commit_tree_.size());
  if (!version_storage_[ver_id].IsRoot()) {
    head_version_id_ = ver_id;
    head_pipeline_params_ = version_storage_[ver_id].GetMaterializedParams();
  }
  SetLastModifiedTime();
  CalculateHistoryID();
  return ver_id;
}

auto EditHistory::CommitWorkingVersion(WorkingVersion&& working_version,
                                       const nlohmann::json& base_pipeline_params,
                                       const nlohmann::json& head_pipeline_params) -> history_id_t {
  const auto applied = working_version.AppliedTransactions();
  std::optional<EditTransaction> last_tx = std::nullopt;
  if (!applied.empty()) {
    last_tx = applied.back();
  }

  Version checkpoint = Version::Checkpoint(
      working_version.GetBoundImage(), working_version.GetParentVersionID(),
      nlohmann::json::diff(base_pipeline_params, head_pipeline_params), head_pipeline_params,
      applied.size(), std::move(last_tx));
  return CommitVersion(std::move(checkpoint));
}

auto EditHistory::GetLatestVersion() -> VersionNode& {
  EnsureRootVersion();
  if (!IsZeroHash(head_version_id_)) {
    for (auto it = commit_tree_.rbegin(); it != commit_tree_.rend(); ++it) {
      if (it->ver_ref_.GetVersionID() == head_version_id_) {
        return *it;
      }
    }
  }
  if (commit_tree_.empty()) {
    throw std::runtime_error("No version in history");
  }
  return commit_tree_.back();
}

auto EditHistory::RemoveVersion(history_id_t ver_id) -> bool {
  EnsureRootVersion();
  if (ver_id == root_version_id_) {
    return false;
  }
  if (version_storage_.find(ver_id) == version_storage_.end()) {
    return false;
  }
  for (auto it = commit_tree_.begin(); it != commit_tree_.end(); ++it) {
    if (it->ver_ref_.GetVersionID() == ver_id) {
      commit_tree_.erase(it);
      break;
    }
  }
  version_storage_.erase(ver_id);
  if (head_version_id_ == ver_id) {
    head_version_id_ = root_version_id_;
    for (auto it = commit_tree_.rbegin(); it != commit_tree_.rend(); ++it) {
      if (!it->ver_ref_.IsRoot()) {
        head_version_id_ = it->ver_ref_.GetVersionID();
        break;
      }
    }
  }
  SetLastModifiedTime();
  return true;
}

auto EditHistory::ToJSON() const -> nlohmann::json {
  nlohmann::json j;
  j["history_id"]           = history_id_.ToString();
  j["bound_image"]          = bound_image_;
  j["added_time"]           = added_time_;
  j["last_modified_time"]   = last_modified_time_;
  j["root_version_id"]      = root_version_id_.ToString();
  j["head_version_id"]      = head_version_id_.ToString();
  if (head_pipeline_params_.has_value()) {
    j["head_pipeline_params"] = *head_pipeline_params_;
  }

  j["commit_tree"] = nlohmann::json::array();
  for (const auto& node : commit_tree_) {
    nlohmann::json node_json;
    node_json["commit_id"] = node.commit_id_;
    node_json["version_id"] = node.ver_ref_.GetVersionID().ToString();
    j["commit_tree"].push_back(node_json);
  }

  auto append_version = [&j](const history_id_t& ver_id, const Version& ver) {
    nlohmann::json ver_json;
    ver_json["version_id"] = ver_id.ToString();
    ver_json["version"]    = ver.ToJSON();
    j["version_storage"].push_back(ver_json);
  };

  j["version_storage"] = nlohmann::json::array();
  std::unordered_map<std::string, bool> emitted;
  for (const auto& node : commit_tree_) {
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
      !j.contains("commit_tree") || !j.contains("version_storage")) {
    throw std::runtime_error("EditHistory: Invalid JSON format for EditHistory");
  }

  history_id_         = Hash128::FromString(j.at("history_id").get<std::string>());
  bound_image_        = j.at("bound_image").get<sl_element_id_t>();
  added_time_         = j.at("added_time").get<std::time_t>();
  last_modified_time_ = j.at("last_modified_time").get<std::time_t>();
  root_version_id_    = j.contains("root_version_id")
                            ? Hash128::FromString(j.at("root_version_id").get<std::string>())
                            : history_id_t{};
  head_version_id_    = j.contains("head_version_id")
                            ? Hash128::FromString(j.at("head_version_id").get<std::string>())
                            : history_id_t{};
  head_pipeline_params_ =
      j.contains("head_pipeline_params") ? std::optional<nlohmann::json>(j.at("head_pipeline_params"))
                                         : std::nullopt;

  commit_tree_.clear();
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

  for (const auto& node_json : j.at("commit_tree")) {
    if (!node_json.is_object() || !node_json.contains("commit_id")) {
      commit_tree_.clear();
      version_storage_.clear();
      throw std::runtime_error("EditHistory: Invalid JSON format for commit_tree node");
    }
    history_id_t ver_id{};
    if (node_json.contains("version_id")) {
      ver_id = Hash128::FromString(node_json.at("version_id").get<std::string>());
    } else if (node_json.contains("version")) {
      Version ver;
      ver.FromJSON(node_json.at("version"));
      ver_id = ver.GetVersionID();
      version_storage_[ver_id] = std::move(ver);
    } else {
      continue;
    }
    auto it = version_storage_.find(ver_id);
    if (it == version_storage_.end()) {
      continue;
    }
    VersionNode node(it->second);
    node.commit_id_ = node_json.at("commit_id").get<p_hash_t>();
    commit_tree_.push_back(std::move(node));
  }

  if (IsZeroHash(root_version_id_)) {
    for (const auto& [ver_id, ver] : version_storage_) {
      if (ver.IsRoot()) {
        root_version_id_ = ver_id;
        break;
      }
    }
  }
  if (IsZeroHash(head_version_id_)) {
    head_version_id_ = root_version_id_;
    for (auto it = commit_tree_.rbegin(); it != commit_tree_.rend(); ++it) {
      if (!it->ver_ref_.IsRoot()) {
        head_version_id_ = it->ver_ref_.GetVersionID();
        break;
      }
    }
  }
  EnsureRootVersion();
}
};  // namespace alcedo
