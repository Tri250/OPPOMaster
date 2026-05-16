//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "edit/history/version.hpp"

#include <xxhash.h>

#include <algorithm>
#include <chrono>
#include <cstdint>
#include <stdexcept>
#include <string>
#include <utility>

#include "edit/history/edit_transaction.hpp"
#include "type/hash_type.hpp"
#include "utils/clock/time_provider.hpp"

namespace alcedo {
namespace {
auto ZeroHash() -> version_id_t { return version_id_t{}; }

auto IsZeroHash(const version_id_t& id) -> bool { return id.low64() == 0 && id.high64() == 0; }

auto JsonHash(const nlohmann::json& j) -> Hash128 {
  const std::string text = j.dump();
  return Hash128::Compute(text.data(), text.size());
}

auto NowTime() -> std::time_t { return std::chrono::system_clock::to_time_t(TimeProvider::Now()); }

auto NewCreationNonce() -> uint64_t {
  return static_cast<uint64_t>(
      std::chrono::high_resolution_clock::now().time_since_epoch().count());
}

auto MaxTransactionId(const std::vector<EditTransaction>& transactions) -> tx_id_t {
  tx_id_t max_id = 0;
  for (const auto& tx : transactions) {
    max_id = std::max(max_id, tx.GetTransactionID());
  }
  return max_id;
}
}  // namespace

Version::Version() {
  added_time_         = NowTime();
  last_modified_time_ = added_time_;
  creation_nonce_     = NewCreationNonce();
  kind_               = VersionKind::Root;
  CalculateVersionID();
}

Version::Version(sl_element_id_t bound_image) : bound_image_(bound_image) {
  added_time_         = NowTime();
  last_modified_time_ = added_time_;
  creation_nonce_     = NewCreationNonce();
  kind_               = VersionKind::Root;
  CalculateVersionID();
}

Version::Version(sl_element_id_t bound_image, version_id_t parent_version_id)
    : parent_version_id_(parent_version_id), bound_image_(bound_image) {
  added_time_         = NowTime();
  last_modified_time_ = added_time_;
  creation_nonce_     = NewCreationNonce();
  kind_               = VersionKind::UserVersion;
  CalculateVersionID();
}

Version::Version(nlohmann::json& j) { FromJSON(j); }

auto Version::Root(sl_element_id_t bound_image, nlohmann::json params) -> Version {
  Version version(bound_image);
  version.kind_                = VersionKind::Root;
  version.parent_version_id_   = ZeroHash();
  version.delta_from_parent_   = nlohmann::json::array();
  version.materialized_params_ = std::move(params);
  version.transaction_count_   = 0;
  version.last_transaction_    = std::nullopt;
  version.CalculateVersionID();
  return version;
}

auto Version::Checkpoint(sl_element_id_t bound_image, version_id_t parent_version_id,
                         nlohmann::json                delta_from_parent,
                         std::optional<nlohmann::json> materialized_params,
                         size_t transaction_count, std::optional<EditTransaction> last_transaction)
    -> Version {
  Version version(bound_image, parent_version_id);
  version.kind_                = VersionKind::UserVersion;
  version.delta_from_parent_   = std::move(delta_from_parent);
  version.materialized_params_ = std::move(materialized_params);
  version.transaction_count_   = transaction_count;
  version.last_transaction_    = std::move(last_transaction);
  version.CalculateVersionID();
  return version;
}

void Version::CalculateVersionID() {
  Hash128   h          = Hash128::Compute(&bound_image_, sizeof(bound_image_));
  const int kind_value = static_cast<int>(kind_);
  h                    = Hash128::Blend(h, Hash128::Compute(&kind_value, sizeof(kind_value)));
  h                    = Hash128::Blend(h, parent_version_id_);
  h                    = Hash128::Blend(h, JsonHash(delta_from_parent_));
  if (materialized_params_.has_value()) {
    h = Hash128::Blend(h, JsonHash(*materialized_params_));
  }
  h = Hash128::Blend(h, Hash128::Compute(&transaction_count_, sizeof(transaction_count_)));
  if (last_transaction_.has_value()) {
    h = Hash128::Blend(h, last_transaction_->Hash());
  }
  if (kind_ == VersionKind::UserVersion) {
    h = Hash128::Blend(h, Hash128::Compute(&added_time_, sizeof(added_time_)));
    h = Hash128::Blend(h, Hash128::Compute(&creation_nonce_, sizeof(creation_nonce_)));
  }
  version_id_ = h;
}

auto Version::GetVersionID() const -> version_id_t { return version_id_; }

auto Version::GetParentVersionID() const -> version_id_t { return parent_version_id_; }

auto Version::HasParentVersion() const -> bool { return !IsZeroHash(parent_version_id_); }

void Version::SetParentVersionID(version_id_t parent_version_id) {
  parent_version_id_ = parent_version_id;
  kind_ = IsZeroHash(parent_version_id_) ? VersionKind::Root : VersionKind::UserVersion;
  SetLastModifiedTime();
}

void Version::ClearParentVersionID() {
  parent_version_id_ = ZeroHash();
  kind_              = VersionKind::Root;
  SetLastModifiedTime();
}

auto Version::GetAddTime() const -> std::time_t { return added_time_; }

auto Version::GetLastModifiedTime() const -> std::time_t { return last_modified_time_; }

void Version::SetLastModifiedTime() { last_modified_time_ = NowTime(); }

void Version::SetBoundImage(sl_element_id_t image_id) { bound_image_ = image_id; }

auto Version::GetBoundImage() const -> sl_element_id_t { return bound_image_; }

void Version::AppendEditTransaction(EditTransaction&& edit_transaction) {
  transactions_.push_back(std::move(edit_transaction));
  cursor_            = transactions_.size();
  transaction_count_ = cursor_;
  if (!transactions_.empty()) {
    last_transaction_ = transactions_.back();
  }
  SetLastModifiedTime();
}

auto Version::RemoveLastEditTransaction() -> EditTransaction {
  if (cursor_ == 0 || transactions_.empty()) {
    throw std::runtime_error("Version: No edit transaction to remove");
  }
  EditTransaction last = std::move(transactions_[cursor_ - 1]);
  transactions_.erase(transactions_.begin() + static_cast<std::ptrdiff_t>(cursor_ - 1));
  cursor_            = std::min(cursor_ - 1, transactions_.size());
  transaction_count_ = cursor_;
  last_transaction_ =
      cursor_ > 0 ? std::optional<EditTransaction>(transactions_[cursor_ - 1]) : std::nullopt;
  SetLastModifiedTime();
  return last;
}

auto Version::GetTransactionByID(tx_id_t transaction_id) -> EditTransaction& {
  auto it = std::find_if(transactions_.begin(), transactions_.end(),
                         [transaction_id](const EditTransaction& tx) {
                           return tx.GetTransactionID() == transaction_id;
                         });
  if (it == transactions_.end()) {
    throw std::runtime_error("Version: transaction not found");
  }
  return *it;
}

auto Version::GetLastEditTransaction() -> EditTransaction& {
  if (cursor_ == 0 || transactions_.empty()) {
    throw std::runtime_error("Version: No edit transaction");
  }
  return transactions_[cursor_ - 1];
}

auto Version::GetAllEditTransactions() const -> const std::vector<EditTransaction>& {
  return transactions_;
}

void Version::UpdateFromWorkingVersion(const WorkingVersion& working_version,
                                       const nlohmann::json& base_pipeline_params,
                                       const nlohmann::json& head_pipeline_params) {
  delta_from_parent_   = nlohmann::json::diff(base_pipeline_params, head_pipeline_params);
  materialized_params_ = head_pipeline_params;
  transactions_        = working_version.GetAllEditTransactions();
  cursor_              = working_version.GetCursor();
  transaction_count_   = cursor_;
  last_transaction_ =
      cursor_ > 0 ? std::optional<EditTransaction>(transactions_[cursor_ - 1]) : std::nullopt;
  SetLastModifiedTime();
}

auto Version::ToJSON() const -> nlohmann::json {
  nlohmann::json j;
  j["version_id"]          = version_id_.ToString();
  j["version_id_low"]      = version_id_.low64();
  j["version_id_high"]     = version_id_.high64();
  j["parent_version_id"]   = parent_version_id_.ToString();
  j["parent_version_low"]  = parent_version_id_.low64();
  j["parent_version_high"] = parent_version_id_.high64();
  j["kind"]                = static_cast<int>(kind_);
  j["added_time"]          = added_time_;
  j["last_modified_time"]  = last_modified_time_;
  j["creation_nonce"]      = creation_nonce_;
  j["bound_image"]         = bound_image_;
  j["delta_from_parent"]   = delta_from_parent_;
  j["transaction_count"]   = transaction_count_;
  j["display_name"]        = display_name_;
  j["cursor"]              = cursor_;
  j["transactions"]        = nlohmann::json::array();
  for (const auto& tx : transactions_) {
    j["transactions"].push_back(tx.ToJSON());
  }
  if (materialized_params_.has_value()) {
    j["materialized_params"] = *materialized_params_;
  }
  if (last_transaction_.has_value()) {
    j["last_transaction"] = last_transaction_->ToJSON();
  }
  return j;
}

void Version::FromJSON(const nlohmann::json& j) {
  if (!j.is_object() || (!j.contains("version_id_low") && !j.contains("version_id")) ||
      !j.contains("added_time") || !j.contains("last_modified_time") ||
      !j.contains("bound_image")) {
    throw std::runtime_error("Version: Invalid JSON format");
  }
  if (j.contains("version_id_low") && j.contains("version_id_high")) {
    version_id_ =
        Hash128(j.at("version_id_low").get<uint64_t>(), j.at("version_id_high").get<uint64_t>());
  } else {
    version_id_ = Hash128::FromString(j.at("version_id").get<std::string>());
  }

  if (j.contains("parent_version_low") && j.contains("parent_version_high")) {
    parent_version_id_ = Hash128(j.at("parent_version_low").get<uint64_t>(),
                                 j.at("parent_version_high").get<uint64_t>());
  } else if (j.contains("parent_version_id")) {
    parent_version_id_ = Hash128::FromString(j.at("parent_version_id").get<std::string>());
  } else {
    parent_version_id_ = ZeroHash();
  }

  kind_               = static_cast<VersionKind>(j.value("kind", HasParentVersion() ? 1 : 0));
  added_time_         = j.at("added_time").get<std::time_t>();
  last_modified_time_ = j.at("last_modified_time").get<std::time_t>();
  creation_nonce_     = j.value("creation_nonce", uint64_t{0});
  bound_image_        = j.at("bound_image").get<sl_element_id_t>();
  delta_from_parent_  = j.value("delta_from_parent", nlohmann::json::array());
  transaction_count_  = j.value("transaction_count", size_t{0});
  display_name_       = j.value("display_name", std::string{});

  if (j.contains("materialized_params")) {
    materialized_params_ = j.at("materialized_params");
  } else if (j.contains("final_pipeline_params")) {
    materialized_params_ = j.at("final_pipeline_params");
  } else {
    materialized_params_ = std::nullopt;
  }
  if (j.contains("last_transaction")) {
    last_transaction_ = EditTransaction(j.at("last_transaction"));
  } else {
    last_transaction_ = std::nullopt;
  }
  transactions_.clear();
  if (j.contains("transactions") && j.at("transactions").is_array()) {
    for (const auto& tx_j : j.at("transactions")) {
      transactions_.push_back(EditTransaction(tx_j));
    }
  }
  cursor_ = std::min(j.value("cursor", transaction_count_), transactions_.size());
  if (transactions_.empty() && last_transaction_.has_value() && transaction_count_ > 0) {
    transaction_count_ = std::max<size_t>(transaction_count_, cursor_);
  } else {
    transaction_count_ = cursor_;
    last_transaction_ =
        cursor_ > 0 ? std::optional<EditTransaction>(transactions_[cursor_ - 1]) : std::nullopt;
  }
}

WorkingVersion::WorkingVersion(sl_element_id_t bound_image, version_id_t base_version_id,
                               std::optional<nlohmann::json> head_pipeline_params)
    : base_version_id_(base_version_id),
      bound_image_(bound_image),
      head_pipeline_params_(std::move(head_pipeline_params)) {}

WorkingVersion::WorkingVersion(sl_element_id_t bound_image, version_id_t base_version_id,
                               std::optional<nlohmann::json> head_pipeline_params,
                               std::vector<EditTransaction> transactions, size_t cursor)
    : base_version_id_(base_version_id),
      bound_image_(bound_image),
      transactions_(std::move(transactions)),
      cursor_(std::min(cursor, transactions_.size())),
      head_pipeline_params_(std::move(head_pipeline_params)) {
  tx_id_generator_.SetStartID(MaxTransactionId(transactions_));
}

void WorkingVersion::AppendEditTransaction(EditTransaction&& edit_transaction) {
  if (cursor_ < transactions_.size()) {
    transactions_.erase(transactions_.begin() + static_cast<std::ptrdiff_t>(cursor_),
                        transactions_.end());
  }
  edit_transaction.SetTransactionID(tx_id_generator_.GenerateID());
  transactions_.push_back(std::move(edit_transaction));
  cursor_ = transactions_.size();
}

auto WorkingVersion::RemoveLastEditTransaction() -> EditTransaction {
  if (cursor_ == 0 || transactions_.empty()) {
    throw std::runtime_error("WorkingVersion: No edit transaction to remove");
  }
  EditTransaction last = std::move(transactions_[cursor_ - 1]);
  transactions_.erase(transactions_.begin() + static_cast<std::ptrdiff_t>(cursor_ - 1));
  cursor_ = std::min(cursor_ - 1, transactions_.size());
  return last;
}

auto WorkingVersion::UndoLastTransaction(PipelineExecutor& pipeline) -> bool {
  if (cursor_ == 0) {
    return false;
  }
  auto& tx = transactions_[cursor_ - 1];
  if (!tx.ApplyBackward(pipeline)) {
    return false;
  }
  --cursor_;
  head_pipeline_params_ = pipeline.ExportPipelineParams();
  return true;
}

auto WorkingVersion::RedoNextTransaction(PipelineExecutor& pipeline) -> bool {
  if (cursor_ >= transactions_.size()) {
    return false;
  }
  auto& tx = transactions_[cursor_];
  if (!tx.ApplyForward(pipeline)) {
    return false;
  }
  ++cursor_;
  head_pipeline_params_ = pipeline.ExportPipelineParams();
  return true;
}

auto WorkingVersion::MoveCursorTo(size_t target_cursor, PipelineExecutor& pipeline) -> bool {
  target_cursor = std::min(target_cursor, transactions_.size());
  while (cursor_ > target_cursor) {
    if (!UndoLastTransaction(pipeline)) {
      return false;
    }
  }
  while (cursor_ < target_cursor) {
    if (!RedoNextTransaction(pipeline)) {
      return false;
    }
  }
  return true;
}

auto WorkingVersion::AppliedTransactions() const -> std::vector<EditTransaction> {
  return std::vector<EditTransaction>(transactions_.begin(),
                                      transactions_.begin() + static_cast<std::ptrdiff_t>(cursor_));
}

auto WorkingVersion::ToJSON() const -> nlohmann::json {
  nlohmann::json j;
  j["base_version_id"] = base_version_id_.ToString();
  j["bound_image"]     = bound_image_;
  j["cursor"]          = cursor_;
  j["tx_id_start"]     = tx_id_generator_.GetCurrentID();
  if (head_pipeline_params_.has_value()) {
    j["head_pipeline_params"] = *head_pipeline_params_;
  }
  j["transactions"] = nlohmann::json::array();
  for (const auto& tx : transactions_) {
    j["transactions"].push_back(tx.ToJSON());
  }
  return j;
}

void WorkingVersion::FromJSON(const nlohmann::json& j) {
  if (!j.is_object() || !j.contains("base_version_id") || !j.contains("bound_image") ||
      !j.contains("cursor") || !j.contains("transactions")) {
    throw std::runtime_error("WorkingVersion: Invalid JSON format");
  }
  base_version_id_ = Hash128::FromString(j.at("base_version_id").get<std::string>());
  bound_image_     = j.at("bound_image").get<sl_element_id_t>();
  cursor_          = j.at("cursor").get<size_t>();
  tx_id_generator_.SetStartID(j.value("tx_id_start", tx_id_t{0}));
  head_pipeline_params_ = j.contains("head_pipeline_params")
                              ? std::optional<nlohmann::json>(j.at("head_pipeline_params"))
                              : std::nullopt;
  transactions_.clear();
  for (const auto& tx_j : j.at("transactions")) {
    transactions_.push_back(EditTransaction(tx_j));
  }
  cursor_ = std::min(cursor_, transactions_.size());
}
}  // namespace alcedo
