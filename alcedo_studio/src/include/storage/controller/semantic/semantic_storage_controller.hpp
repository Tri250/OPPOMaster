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
#include "storage/controller/semantic/semantic_label_config.hpp"
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

struct SemanticImageLabelRecord {
  sl_element_id_t       file_id_ = 0;
  std::string           model_key_{};
  std::string           label_{};
  double                score_ = 0.0;
  std::string           second_label_{};
  std::optional<double> second_score_{};
  double                margin_    = 0.0;
  bool                  confident_ = false;
  std::string           top_scores_json_{};
};

struct SemanticLabelPrototypeRecord {
  std::string        model_key_{};
  std::string        label_{};
  std::string        prompt_config_hash_{};
  std::vector<float> embedding_{};
};

struct SemanticLabelQueryRecord {
  std::string prompt_config_hash_{};
  std::string label_{};
  std::string query_text_{};
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
  [[nodiscard]] auto UpsertImageEmbeddingWithLabel(const SemanticImageEmbeddingRecord& record,
                                                   const SemanticImageLabelRecord*     label,
                                                   std::string* error = nullptr) const -> bool;
  [[nodiscard]] auto UpsertLabelPrototype(const SemanticLabelPrototypeRecord& record,
                                          std::string* error = nullptr) const -> bool;
  [[nodiscard]] auto UpsertLabelPrototypes(std::span<const SemanticLabelPrototypeRecord> records,
                                           std::string* error = nullptr) const -> bool;
  void               DeleteImageEmbeddingsForFiles(std::span<const sl_element_id_t> file_ids) const;
  [[nodiscard]] auto CountImageEmbeddings(const std::string& model_key) const -> size_t;
  [[nodiscard]] auto CountImageEmbeddingsForFile(sl_element_id_t    file_id,
                                                 const std::string& model_key) const -> size_t;
  [[nodiscard]] auto CountImageLabelsForFile(sl_element_id_t    file_id,
                                             const std::string& model_key) const -> size_t;
  [[nodiscard]] auto CountLabelPrototypes(const std::string& model_key,
                                          const std::string& prompt_config_hash) const -> size_t;
  [[nodiscard]] auto CountLabelQueries(const std::string& prompt_config_hash) const -> size_t;
  [[nodiscard]] auto ListLabelQueries(const std::string& prompt_config_hash,
                                      std::string*       error = nullptr) const
      -> std::vector<SemanticLabelQueryRecord>;
  [[nodiscard]] auto LoadLabelPrototypes(const std::string& model_key,
                                         const std::string& prompt_config_hash,
                                         std::string*       error = nullptr) const
      -> std::vector<SemanticGenerationLabelPrototype>;
  [[nodiscard]] auto GetImageLabelForFile(sl_element_id_t file_id, const std::string& model_key,
                                          std::string* error = nullptr) const
      -> std::optional<SemanticImageLabelRecord>;

  [[nodiscard]] auto SearchImageEmbeddings(sl_element_id_t folder_id, const std::string& model_key,
                                           std::span<const float> query_embedding, size_t offset,
                                           size_t limit, std::string* error = nullptr) const
      -> std::vector<SemanticRankedFile>;

  [[nodiscard]] auto EnsureVectorSearchIndex(const std::string& model_key,
                                             std::string*       error = nullptr) const -> bool;
};
}  // namespace alcedo
