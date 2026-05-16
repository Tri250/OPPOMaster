//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <xxhash.h>

#include <ctime>
#include <memory>
#include <optional>
#include <string>
#include <unordered_map>
#include <vector>

#include "edit/history/edit_transaction.hpp"
#include "edit/pipeline/pipeline.hpp"
#include "edit_transaction.hpp"
#include "type/hash_type.hpp"
#include "type/type.hpp"
#include "utils/id/id_generator.hpp"

namespace alcedo {
using version_id_t = Hash128;

enum class VersionKind : int { Root = 0, UserVersion = 1 };

class WorkingVersion;

class Version {
 private:
  /**
   * @brief Version checkpoint ID.
   */
  version_id_t                   version_id_        = version_id_t{};

  version_id_t                   parent_version_id_ = version_id_t{};
  VersionKind                    kind_              = VersionKind::UserVersion;
  /**
   * @brief Last modified time for this version
   */
  std::time_t                    added_time_;
  std::time_t                    last_modified_time_;
  uint64_t                       creation_nonce_ = 0;
  /**
   * @brief collection of images related to this version
   */
  sl_element_id_t                bound_image_;
  nlohmann::json                 delta_from_parent_   = nlohmann::json::array();
  std::optional<nlohmann::json>  materialized_params_ = std::nullopt;
  size_t                         transaction_count_   = 0;
  std::optional<EditTransaction> last_transaction_    = std::nullopt;
  std::string                    display_name_{};
  std::vector<EditTransaction>   transactions_{};
  size_t                         cursor_ = 0;

 public:
  Version();
  Version(sl_element_id_t bound_image);
  Version(sl_element_id_t bound_image, version_id_t parent_version_id);
  Version(nlohmann::json& j);

  static auto Root(sl_element_id_t bound_image, nlohmann::json params) -> Version;
  static auto Checkpoint(sl_element_id_t bound_image, version_id_t parent_version_id,
                         nlohmann::json                delta_from_parent,
                         std::optional<nlohmann::json> materialized_params,
                         size_t transaction_count, std::optional<EditTransaction> last_transaction)
      -> Version;

  void CalculateVersionID();
  auto GetVersionID() const -> version_id_t;
  auto GetParentVersionID() const -> version_id_t;
  auto HasParentVersion() const -> bool;
  auto GetKind() const -> VersionKind { return kind_; }
  auto IsRoot() const -> bool { return kind_ == VersionKind::Root; }
  void SetParentVersionID(version_id_t parent_version_id);
  void ClearParentVersionID();

  void SetAddTime();
  auto GetAddTime() const -> std::time_t;
  void SetLastModifiedTime();
  auto GetLastModifiedTime() const -> std::time_t;

  void SetBoundImage(sl_element_id_t bound_image);
  auto GetBoundImage() const -> sl_element_id_t;

  void SetBasePipelineExecutor(std::shared_ptr<PipelineExecutor> /*pipeline_executor*/) {}

  void SetDeltaFromParent(const nlohmann::json& delta) {
    delta_from_parent_ = delta;
    SetLastModifiedTime();
  }
  auto GetDeltaFromParent() const -> const nlohmann::json& { return delta_from_parent_; }

  void SetFinalPipelineParams(const nlohmann::json& params) { materialized_params_ = params; }
  auto GetFinalPipelineParams() const -> std::optional<nlohmann::json> {
    return materialized_params_;
  }
  auto GetMaterializedParams() const -> std::optional<nlohmann::json> {
    return materialized_params_;
  }
  auto GetTransactionCount() const -> size_t { return transaction_count_; }
  auto GetLastTransaction() const -> const std::optional<EditTransaction>& {
    return last_transaction_;
  }
  auto GetDisplayName() const -> const std::string& { return display_name_; }
  void SetDisplayName(std::string display_name) { display_name_ = std::move(display_name); }
  auto GetCursor() const -> size_t { return cursor_; }

  void AppendEditTransaction(EditTransaction&& edit_transaction);
  auto RemoveLastEditTransaction() -> EditTransaction;
  auto GetTransactionByID(tx_id_t transaction_id) -> EditTransaction&;
  auto GetLastEditTransaction() -> EditTransaction&;
  auto GetAllEditTransactions() const -> const std::vector<EditTransaction>&;
  void UpdateFromWorkingVersion(const WorkingVersion& working_version,
                                const nlohmann::json& base_pipeline_params,
                                const nlohmann::json& head_pipeline_params);

  auto ToJSON() const -> nlohmann::json;
  void FromJSON(const nlohmann::json& j);
};

class WorkingVersion {
 private:
  IncrID::IDGenerator<tx_id_t>  tx_id_generator_{0};
  version_id_t                  base_version_id_ = version_id_t{};
  sl_element_id_t               bound_image_     = 0;
  std::vector<EditTransaction>  transactions_;
  size_t                        cursor_               = 0;
  std::optional<nlohmann::json> head_pipeline_params_ = std::nullopt;

 public:
  WorkingVersion() = default;
  WorkingVersion(sl_element_id_t bound_image, version_id_t base_version_id,
                 std::optional<nlohmann::json> head_pipeline_params = std::nullopt);
  WorkingVersion(sl_element_id_t bound_image, version_id_t base_version_id,
                 std::optional<nlohmann::json> head_pipeline_params,
                 std::vector<EditTransaction> transactions, size_t cursor);

  auto GetParentVersionID() const -> version_id_t { return base_version_id_; }
  auto HasParentVersion() const -> bool {
    return base_version_id_.low64() != 0 || base_version_id_.high64() != 0;
  }
  auto GetBoundImage() const -> sl_element_id_t { return bound_image_; }
  auto GetAppliedTransactionCount() const -> size_t { return cursor_; }
  auto GetCursor() const -> size_t { return cursor_; }
  auto GetAllEditTransactions() const -> const std::vector<EditTransaction>& {
    return transactions_;
  }
  auto GetHeadPipelineParams() const -> std::optional<nlohmann::json> {
    return head_pipeline_params_;
  }
  void SetHeadPipelineParams(const nlohmann::json& params) { head_pipeline_params_ = params; }

  void AppendEditTransaction(EditTransaction&& edit_transaction);
  auto RemoveLastEditTransaction() -> EditTransaction;
  auto UndoLastTransaction(PipelineExecutor& pipeline) -> bool;
  auto RedoNextTransaction(PipelineExecutor& pipeline) -> bool;
  auto MoveCursorTo(size_t target_cursor, PipelineExecutor& pipeline) -> bool;
  auto AppliedTransactions() const -> std::vector<EditTransaction>;

  auto ToJSON() const -> nlohmann::json;
  void FromJSON(const nlohmann::json& j);
};
};  // namespace alcedo
