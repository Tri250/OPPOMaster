//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstddef>
#include <optional>
#include <span>
#include <string>
#include <vector>

#include "storage/controller/controller_types.hpp"
#include "type/type.hpp"

namespace alcedo {
inline constexpr int kSemanticEmbeddingDim = 512;

struct SemanticModelRecord {
  std::string model_key_{};
  std::string model_id_{};
  std::string revision_{};
  int         embedding_dim_ = kSemanticEmbeddingDim;
  int         image_size_    = 256;
  std::string prompt_config_hash_{};
  std::string asset_manifest_json_{};
};

struct SemanticImageEmbeddingRecord {
  sl_element_id_t    file_id_  = 0;
  image_id_t         image_id_ = 0;
  std::string        model_key_{};
  std::vector<float> embedding_{};
  int                thumbnail_resolution_ = 256;
};

struct SemanticRankedFile {
  sl_element_id_t file_id_  = 0;
  image_id_t      image_id_ = 0;
  std::string     file_name_{};
  double          score_ = 0.0;
};

class SemanticStorageController {
 private:
  ConnectionGuard guard_;

 public:
  explicit SemanticStorageController(ConnectionGuard&& guard);

  [[nodiscard]] auto UpsertModel(const SemanticModelRecord& model,
                                 std::string*               error = nullptr) const -> bool;
  [[nodiscard]] auto HasModel(const std::string& model_key) const -> bool;
  [[nodiscard]] auto GetModelEmbeddingDim(const std::string& model_key) const -> std::optional<int>;

  [[nodiscard]] auto UpsertImageEmbedding(const SemanticImageEmbeddingRecord& record,
                                          std::string* error = nullptr) const -> bool;
  void               DeleteImageEmbeddingsForFiles(std::span<const sl_element_id_t> file_ids) const;
  [[nodiscard]] auto CountImageEmbeddings(const std::string& model_key) const -> size_t;
  [[nodiscard]] auto CountImageEmbeddingsForFile(sl_element_id_t    file_id,
                                                 const std::string& model_key) const -> size_t;

  [[nodiscard]] auto SearchImageEmbeddings(sl_element_id_t folder_id, const std::string& model_key,
                                           std::span<const float> query_embedding, size_t offset,
                                           size_t limit, std::string* error = nullptr) const
      -> std::vector<SemanticRankedFile>;

  [[nodiscard]] auto EnsureVectorSearchIndex(const std::string& model_key,
                                             std::string*       error = nullptr) const -> bool;
};
}  // namespace alcedo
