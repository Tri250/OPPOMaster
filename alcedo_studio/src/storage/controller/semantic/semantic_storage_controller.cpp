//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "storage/controller/semantic/semantic_storage_controller.hpp"

#include <duckdb.h>

#include <algorithm>
#include <cmath>
#include <cstdlib>
#include <filesystem>
#include <format>
#include <iomanip>
#include <optional>
#include <sstream>
#include <string>
#include <vector>

#include "storage/controller/sleeve/element_controller.hpp"

#ifdef _WIN32
#ifndef NOMINMAX
#define NOMINMAX
#endif
#include <windows.h>
#elif defined(__APPLE__)
#include <mach-o/dyld.h>
#else
#include <limits.h>
#include <unistd.h>
#endif

namespace alcedo {
namespace {
auto SqlString(const std::string& value) -> std::string {
  std::string out;
  out.reserve(value.size() + 2);
  out.push_back('\'');
  for (const char ch : value) {
    if (ch == '\'') {
      out.push_back('\'');
    }
    out.push_back(ch);
  }
  out.push_back('\'');
  return out;
}

auto SqlNullableString(const std::string& value) -> std::string {
  return value.empty() ? "NULL" : SqlString(value);
}

auto SqlNullableDouble(const std::optional<double>& value) -> std::string {
  if (!value.has_value()) {
    return "NULL";
  }
  return std::format("{}", *value);
}

auto SqlBool(bool value) -> const char* { return value ? "TRUE" : "FALSE"; }

auto JsonString(const std::string& value) -> std::string {
  std::string out;
  out.reserve(value.size() + 2);
  out.push_back('"');
  for (const char ch : value) {
    switch (ch) {
      case '\\':
        out += "\\\\";
        break;
      case '"':
        out += "\\\"";
        break;
      default:
        out.push_back(ch);
        break;
    }
  }
  out.push_back('"');
  return out;
}

auto VectorArrayLiteral(std::span<const float> values) -> std::string {
  std::ostringstream out;
  out << "CAST([";
  for (size_t i = 0; i < values.size(); ++i) {
    if (i > 0) {
      out << ",";
    }
    out << std::setprecision(9) << values[i] << "::FLOAT";
  }
  out << "] AS FLOAT[" << values.size() << "])";
  return out.str();
}

auto IdList(std::span<const sl_element_id_t> ids) -> std::string {
  std::ostringstream out;
  for (size_t i = 0; i < ids.size(); ++i) {
    if (i > 0) {
      out << ",";
    }
    out << ids[i];
  }
  return out.str();
}

auto ExecutableDirectory() -> std::filesystem::path {
#ifdef _WIN32
  std::wstring buffer(MAX_PATH, L'\0');
  DWORD        size = 0;
  while (true) {
    size = GetModuleFileNameW(nullptr, buffer.data(), static_cast<DWORD>(buffer.size()));
    if (size == 0) {
      return {};
    }
    if (size < buffer.size() - 1) {
      buffer.resize(size);
      return std::filesystem::path(buffer).parent_path();
    }
    buffer.resize(buffer.size() * 2);
  }
#elif defined(__APPLE__)
  uint32_t size = 0;
  _NSGetExecutablePath(nullptr, &size);
  std::string buffer(size, '\0');
  if (_NSGetExecutablePath(buffer.data(), &size) != 0) {
    return {};
  }
  return std::filesystem::weakly_canonical(std::filesystem::path(buffer.c_str())).parent_path();
#else
  std::string buffer(PATH_MAX, '\0');
  const auto  size = readlink("/proc/self/exe", buffer.data(), buffer.size() - 1);
  if (size <= 0) {
    return {};
  }
  buffer.resize(static_cast<size_t>(size));
  return std::filesystem::path(buffer).parent_path();
#endif
}

void SetError(std::string* error, const std::string& message) {
  if (error) {
    *error = message;
  }
}

auto RunQuery(duckdb_connection conn, const std::string& sql, std::string* error = nullptr)
    -> bool {
  duckdb_result result;
  if (duckdb_query(conn, sql.c_str(), &result) != DuckDBSuccess) {
    const char* raw_error = duckdb_result_error(&result);
    SetError(error, raw_error ? raw_error : "DuckDB query failed");
    duckdb_destroy_result(&result);
    return false;
  }
  duckdb_destroy_result(&result);
  return true;
}

auto LoadVssExtension(duckdb_connection conn, std::string* error) -> bool {
  std::vector<std::filesystem::path> candidates;
  if (const char* env_path = std::getenv("ALCEDO_DUCKDB_VSS_EXTENSION")) {
    if (*env_path != '\0') {
      candidates.emplace_back(env_path);
    }
  }

  const auto exe_dir = ExecutableDirectory();
  if (!exe_dir.empty()) {
    candidates.push_back(exe_dir / "duckdb_extensions" / "vss.duckdb_extension");
    candidates.push_back(exe_dir / "extensions" / "vss.duckdb_extension");
  }

  std::string load_error;
  for (const auto& candidate : candidates) {
    std::error_code ec;
    if (!std::filesystem::is_regular_file(candidate, ec) || ec) {
      continue;
    }
    std::string candidate_error;
    if (RunQuery(conn, "LOAD " + SqlString(candidate.generic_string()) + ";", &candidate_error)) {
      return true;
    }
    load_error += "\nPackaged extension load failed from " + candidate.generic_string() + ": " +
                  candidate_error;
  }

  if (RunQuery(conn, "LOAD vss;", &load_error)) {
    return true;
  }

  SetError(error, load_error);
  return false;
}

auto ScalarInt64(duckdb_connection conn, const std::string& sql) -> std::optional<int64_t> {
  duckdb_result result;
  if (duckdb_query(conn, sql.c_str(), &result) != DuckDBSuccess) {
    duckdb_destroy_result(&result);
    return std::nullopt;
  }

  std::optional<int64_t> value;
  if (duckdb_row_count(&result) > 0 && duckdb_column_count(&result) > 0 &&
      !duckdb_value_is_null(&result, 0, 0)) {
    value = duckdb_value_int64(&result, 0, 0);
  }
  duckdb_destroy_result(&result);
  return value;
}

auto ValidateEmbedding(std::span<const float> embedding, int expected_dim, std::string* error)
    -> bool {
  if (expected_dim != kSemanticEmbeddingDim) {
    SetError(error, "Semantic storage currently supports 512-dimensional embeddings only.");
    return false;
  }
  if (embedding.size() != static_cast<size_t>(expected_dim)) {
    SetError(error, std::format("Embedding dimension mismatch: expected {}, got {}.", expected_dim,
                                embedding.size()));
    return false;
  }

  double norm_sq = 0.0;
  for (const float value : embedding) {
    if (!std::isfinite(value)) {
      SetError(error, "Embedding contains NaN or infinity.");
      return false;
    }
    norm_sq += static_cast<double>(value) * static_cast<double>(value);
  }
  if (norm_sq <= 0.0) {
    SetError(error, "Embedding norm is zero.");
    return false;
  }
  return true;
}

auto ValidateLabel(const SemanticImageEmbeddingRecord& record,
                   const SemanticImageLabelRecord* label, std::string* error) -> bool {
  if (!label) {
    return true;
  }
  if (label->file_id_ != record.file_id_) {
    SetError(error, "Semantic label file id does not match embedding file id.");
    return false;
  }
  if (label->model_key_ != record.model_key_) {
    SetError(error, "Semantic label model key does not match embedding model key.");
    return false;
  }
  if (label->label_.empty()) {
    SetError(error, "Semantic label is empty.");
    return false;
  }
  if (!std::isfinite(label->score_) ||
      (label->second_score_.has_value() && !std::isfinite(*label->second_score_)) ||
      !std::isfinite(label->margin_)) {
    SetError(error, "Semantic label score contains NaN or infinity.");
    return false;
  }
  return true;
}

auto MakeTopScoresJson(const std::vector<std::pair<std::string, double>>& scores, size_t limit)
    -> std::string {
  std::ostringstream out;
  out << "[";
  const auto count = std::min(limit, scores.size());
  for (size_t i = 0; i < count; ++i) {
    if (i > 0) {
      out << ",";
    }
    out << "{\"label\":" << JsonString(scores[i].first) << ",\"score\":" << std::setprecision(9)
        << scores[i].second << "}";
  }
  out << "]";
  return out.str();
}

auto InsertSemanticLabel(duckdb_connection conn, const SemanticImageLabelRecord& label,
                         std::string* error) -> bool {
  const auto insert_label_sql = std::format(
      "INSERT INTO SemanticImageLabel "
      "(file_id, model_key, label, score, second_label, second_score, margin, confident, "
      "top_scores) "
      "VALUES ({}, {}, {}, {}, {}, {}, {}, {}, {});",
      label.file_id_, SqlString(label.model_key_), SqlString(label.label_), label.score_,
      SqlNullableString(label.second_label_), SqlNullableDouble(label.second_score_), label.margin_,
      SqlBool(label.confident_), SqlNullableString(label.top_scores_json_));
  return RunQuery(conn, insert_label_sql, error);
}

auto BuildAssignedLabel(sl_element_id_t file_id, const std::string& model_key,
                        const SemanticLabelAssignmentOptions&              assignment_options,
                        const std::vector<std::pair<std::string, double>>& scores)
    -> SemanticImageLabelRecord {
  const auto               second_score = scores.size() > 1 ? scores[1].second : 0.0;

  SemanticImageLabelRecord label;
  label.file_id_      = file_id;
  label.model_key_    = model_key;
  label.label_        = scores[0].first;
  label.score_        = scores[0].second;
  label.second_label_ = scores.size() > 1 ? scores[1].first : std::string{};
  label.second_score_ = scores.size() > 1 ? std::optional<double>(scores[1].second) : std::nullopt;
  label.margin_       = scores[0].second - second_score;
  label.confident_    = label.score_ >= assignment_options.confidence_score_threshold_ &&
                     label.margin_ >= assignment_options.confidence_margin_threshold_;
  label.top_scores_json_ = MakeTopScoresJson(scores, assignment_options.top_score_count_);
  return label;
}

auto QueryAssignedLabel(duckdb_connection conn, const SemanticImageEmbeddingRecord& record,
                        const SemanticLabelAssignmentOptions& assignment_options,
                        std::string* error) -> std::optional<SemanticImageLabelRecord> {
  if (assignment_options.prompt_config_hash_.empty()) {
    SetError(error, "Semantic label assignment prompt config hash is empty.");
    return std::nullopt;
  }
  if (!std::isfinite(assignment_options.confidence_score_threshold_) ||
      !std::isfinite(assignment_options.confidence_margin_threshold_)) {
    SetError(error, "Semantic label assignment threshold contains NaN or infinity.");
    return std::nullopt;
  }

  const auto result_limit = std::max<size_t>(assignment_options.top_score_count_, 2U);
  const auto sql          = std::format(
      "SELECT lp.label, array_inner_product(lp.embedding, se.embedding) AS score "
               "FROM SemanticLabelPrototype lp "
               "JOIN SemanticImageEmbedding se ON se.model_key = lp.model_key "
               "AND se.file_id = {} AND se.image_id = {} "
               "WHERE lp.model_key = {} AND lp.prompt_config_hash = {} "
               "AND se.status = 'ready' AND se.error IS NULL "
               "ORDER BY score DESC, lp.label LIMIT {};",
      record.file_id_, record.image_id_, SqlString(record.model_key_),
      SqlString(assignment_options.prompt_config_hash_), result_limit);

  duckdb_result result;
  if (duckdb_query(conn, sql.c_str(), &result) != DuckDBSuccess) {
    const char* raw_error = duckdb_result_error(&result);
    SetError(error, raw_error ? raw_error : "DuckDB semantic label assignment failed.");
    duckdb_destroy_result(&result);
    return std::nullopt;
  }

  std::vector<std::pair<std::string, double>> scores;
  const auto                                  row_count = duckdb_row_count(&result);
  scores.reserve(static_cast<size_t>(row_count));
  for (idx_t r = 0; r < row_count; ++r) {
    std::string label;
    if (char* raw = duckdb_value_varchar(&result, 0, r)) {
      label = raw;
      duckdb_free(raw);
    }
    if (label.empty()) {
      duckdb_destroy_result(&result);
      SetError(error, "Semantic label assignment returned an empty label.");
      return std::nullopt;
    }
    scores.emplace_back(std::move(label), duckdb_value_double(&result, 1, r));
  }
  duckdb_destroy_result(&result);

  if (scores.empty()) {
    SetError(error, "Semantic label prototype cache is empty.");
    return std::nullopt;
  }
  return BuildAssignedLabel(record.file_id_, record.model_key_, assignment_options, scores);
}
}  // namespace

SemanticStorageController::SemanticStorageController(ConnectionGuard&& guard)
    : guard_(std::move(guard)) {}

auto SemanticStorageController::UpsertModel(const SemanticModelRecord& model,
                                            std::string*               error) const -> bool {
  if (model.model_key_.empty()) {
    SetError(error, "Semantic model key is empty.");
    return false;
  }
  if (model.model_id_.empty()) {
    SetError(error, "Semantic model id is empty.");
    return false;
  }
  if (model.embedding_dim_ != kSemanticEmbeddingDim) {
    SetError(error, "Semantic storage currently supports 512-dimensional embeddings only.");
    return false;
  }
  if (model.image_size_ <= 0) {
    SetError(error, "Semantic model image size must be positive.");
    return false;
  }

  const auto sql = std::format(
      "INSERT OR REPLACE INTO SemanticModel "
      "(model_key, model_id, revision, embedding_dim, image_size, "
      "prompt_config_hash, asset_manifest_json) "
      "VALUES ({}, {}, {}, {}, {}, {}, {});",
      SqlString(model.model_key_), SqlString(model.model_id_), SqlString(model.revision_),
      model.embedding_dim_, model.image_size_, SqlNullableString(model.prompt_config_hash_),
      SqlNullableString(model.asset_manifest_json_));
  return RunQuery(guard_.conn_, sql, error);
}

auto SemanticStorageController::HasModel(const std::string& model_key) const -> bool {
  return GetModelEmbeddingDim(model_key).has_value();
}

auto SemanticStorageController::GetModelEmbeddingDim(const std::string& model_key) const
    -> std::optional<int> {
  const auto value =
      ScalarInt64(guard_.conn_, std::format("SELECT embedding_dim FROM SemanticModel "
                                            "WHERE model_key = {};",
                                            SqlString(model_key)));
  if (!value.has_value()) {
    return std::nullopt;
  }
  return static_cast<int>(*value);
}

auto SemanticStorageController::LatestModelKey() const -> std::string {
  const auto sql =
      "SELECT model_key FROM SemanticModel ORDER BY created_at DESC, model_key DESC LIMIT 1;";

  duckdb_result result;
  if (duckdb_query(guard_.conn_, sql, &result) != DuckDBSuccess) {
    duckdb_destroy_result(&result);
    return {};
  }

  std::string out;
  if (duckdb_row_count(&result) > 0) {
    if (char* raw = duckdb_value_varchar(&result, 0, 0)) {
      out = raw;
      duckdb_free(raw);
    }
  }

  duckdb_destroy_result(&result);
  return out;
}

auto SemanticStorageController::UpsertImageEmbedding(const SemanticImageEmbeddingRecord& record,
                                                     std::string* error) const -> bool {
  return UpsertImageEmbeddingWithLabel(record, nullptr, error);
}

auto SemanticStorageController::UpsertImageEmbeddingWithLabel(
    const SemanticImageEmbeddingRecord& record, const SemanticImageLabelRecord* label,
    std::string* error) const -> bool {
  if (record.file_id_ == 0) {
    SetError(error, "Semantic embedding file id is zero.");
    return false;
  }
  if (record.model_key_.empty()) {
    SetError(error, "Semantic embedding model key is empty.");
    return false;
  }
  const auto model_dim = GetModelEmbeddingDim(record.model_key_);
  if (!model_dim.has_value()) {
    SetError(error, "Semantic model is not registered.");
    return false;
  }
  if (!ValidateEmbedding(record.embedding_, *model_dim, error)) {
    return false;
  }
  if (!ValidateLabel(record, label, error)) {
    return false;
  }

  const auto embedding_sql = VectorArrayLiteral(record.embedding_);
  const auto delete_sql =
      std::format("DELETE FROM SemanticImageEmbedding WHERE file_id = {} AND model_key = {};",
                  record.file_id_, SqlString(record.model_key_));
  const auto delete_label_sql =
      std::format("DELETE FROM SemanticImageLabel WHERE file_id = {} AND model_key = {};",
                  record.file_id_, SqlString(record.model_key_));
  const auto insert_sql = std::format(
      "INSERT INTO SemanticImageEmbedding "
      "(file_id, image_id, model_key, embedding, embedding_dim, "
      "thumbnail_resolution, status, error) "
      "VALUES ({}, {}, {}, {}, {}, {}, 'ready', NULL);",
      record.file_id_, record.image_id_, SqlString(record.model_key_), embedding_sql, *model_dim,
      record.thumbnail_resolution_);

  if (!RunQuery(guard_.conn_, "BEGIN TRANSACTION;", error)) {
    return false;
  }
  if (!RunQuery(guard_.conn_, delete_sql, error) ||
      !RunQuery(guard_.conn_, delete_label_sql, error) ||
      !RunQuery(guard_.conn_, insert_sql, error)) {
    std::string rollback_error;
    RunQuery(guard_.conn_, "ROLLBACK;", &rollback_error);
    return false;
  }
  if (label) {
    if (!InsertSemanticLabel(guard_.conn_, *label, error)) {
      std::string rollback_error;
      RunQuery(guard_.conn_, "ROLLBACK;", &rollback_error);
      return false;
    }
  }
  return RunQuery(guard_.conn_, "COMMIT;", error);
}

auto SemanticStorageController::UpsertImageEmbeddingAndAssignLabel(
    const SemanticImageEmbeddingRecord&   record,
    const SemanticLabelAssignmentOptions& assignment_options,
    SemanticImageLabelRecord* assigned_label, std::string* error) const -> bool {
  if (record.file_id_ == 0) {
    SetError(error, "Semantic embedding file id is zero.");
    return false;
  }
  if (record.model_key_.empty()) {
    SetError(error, "Semantic embedding model key is empty.");
    return false;
  }
  const auto model_dim = GetModelEmbeddingDim(record.model_key_);
  if (!model_dim.has_value()) {
    SetError(error, "Semantic model is not registered.");
    return false;
  }
  if (!ValidateEmbedding(record.embedding_, *model_dim, error)) {
    return false;
  }

  const auto embedding_sql = VectorArrayLiteral(record.embedding_);
  const auto delete_sql =
      std::format("DELETE FROM SemanticImageEmbedding WHERE file_id = {} AND model_key = {};",
                  record.file_id_, SqlString(record.model_key_));
  const auto delete_label_sql =
      std::format("DELETE FROM SemanticImageLabel WHERE file_id = {} AND model_key = {};",
                  record.file_id_, SqlString(record.model_key_));
  const auto insert_sql = std::format(
      "INSERT INTO SemanticImageEmbedding "
      "(file_id, image_id, model_key, embedding, embedding_dim, "
      "thumbnail_resolution, status, error) "
      "VALUES ({}, {}, {}, {}, {}, {}, 'ready', NULL);",
      record.file_id_, record.image_id_, SqlString(record.model_key_), embedding_sql, *model_dim,
      record.thumbnail_resolution_);

  if (!RunQuery(guard_.conn_, "BEGIN TRANSACTION;", error)) {
    return false;
  }
  if (!RunQuery(guard_.conn_, delete_sql, error) ||
      !RunQuery(guard_.conn_, delete_label_sql, error) ||
      !RunQuery(guard_.conn_, insert_sql, error)) {
    std::string rollback_error;
    RunQuery(guard_.conn_, "ROLLBACK;", &rollback_error);
    return false;
  }

  auto label = QueryAssignedLabel(guard_.conn_, record, assignment_options, error);
  if (!label.has_value() || !ValidateLabel(record, &*label, error) ||
      !InsertSemanticLabel(guard_.conn_, *label, error)) {
    std::string rollback_error;
    RunQuery(guard_.conn_, "ROLLBACK;", &rollback_error);
    return false;
  }

  if (!RunQuery(guard_.conn_, "COMMIT;", error)) {
    return false;
  }
  if (assigned_label) {
    *assigned_label = std::move(*label);
  }
  return true;
}

auto SemanticStorageController::UpsertLabelPrototype(const SemanticLabelPrototypeRecord& record,
                                                     std::string* error) const -> bool {
  if (record.model_key_.empty()) {
    SetError(error, "Semantic label prototype model key is empty.");
    return false;
  }
  if (record.label_.empty()) {
    SetError(error, "Semantic label prototype label is empty.");
    return false;
  }
  if (record.prompt_config_hash_.empty()) {
    SetError(error, "Semantic label prototype prompt config hash is empty.");
    return false;
  }
  const auto model_dim = GetModelEmbeddingDim(record.model_key_);
  if (!model_dim.has_value()) {
    SetError(error, "Semantic model is not registered.");
    return false;
  }
  if (!ValidateEmbedding(record.embedding_, *model_dim, error)) {
    return false;
  }

  return RunQuery(
      guard_.conn_,
      std::format("INSERT OR REPLACE INTO SemanticLabelPrototype "
                  "(model_key, label, prompt_config_hash, embedding) VALUES ({}, {}, {}, {});",
                  SqlString(record.model_key_), SqlString(record.label_),
                  SqlString(record.prompt_config_hash_), VectorArrayLiteral(record.embedding_)),
      error);
}

auto SemanticStorageController::UpsertLabelPrototypes(
    std::span<const SemanticLabelPrototypeRecord> records, std::string* error) const -> bool {
  if (records.empty()) {
    return true;
  }
  if (!RunQuery(guard_.conn_, "BEGIN TRANSACTION;", error)) {
    return false;
  }
  for (const auto& record : records) {
    if (!UpsertLabelPrototype(record, error)) {
      std::string rollback_error;
      RunQuery(guard_.conn_, "ROLLBACK;", &rollback_error);
      return false;
    }
  }
  return RunQuery(guard_.conn_, "COMMIT;", error);
}

void SemanticStorageController::DeleteImageEmbeddingsForFiles(
    std::span<const sl_element_id_t> file_ids) const {
  if (file_ids.empty()) {
    return;
  }
  const auto ids = IdList(file_ids);
  RunQuery(guard_.conn_,
           std::format("DELETE FROM SemanticImageEmbedding WHERE file_id IN ({});", ids));
  RunQuery(guard_.conn_, std::format("DELETE FROM SemanticImageLabel WHERE file_id IN ({});", ids));
}

auto SemanticStorageController::CountImageEmbeddings(const std::string& model_key) const -> size_t {
  const auto count = ScalarInt64(
      guard_.conn_, std::format("SELECT COUNT(*) FROM SemanticImageEmbedding WHERE model_key = {};",
                                SqlString(model_key)));
  return count.has_value() ? static_cast<size_t>(*count) : 0U;
}

auto SemanticStorageController::CountImageEmbeddingsForFile(sl_element_id_t    file_id,
                                                            const std::string& model_key) const
    -> size_t {
  const auto count =
      ScalarInt64(guard_.conn_, std::format("SELECT COUNT(*) FROM SemanticImageEmbedding "
                                            "WHERE file_id = {} AND model_key = {};",
                                            file_id, SqlString(model_key)));
  return count.has_value() ? static_cast<size_t>(*count) : 0U;
}

auto SemanticStorageController::HasReadyImageEmbedding(sl_element_id_t file_id, image_id_t image_id,
                                                       const std::string& model_key,
                                                       bool require_label) const -> bool {
  const auto model_dim = GetModelEmbeddingDim(model_key);
  if (!model_dim.has_value()) {
    return false;
  }

  std::string sql = std::format(
      "SELECT COUNT(*) FROM SemanticImageEmbedding se "
      "{} "
      "WHERE se.file_id = {} AND se.image_id = {} AND se.model_key = {} "
      "AND se.embedding_dim = {} AND se.status = 'ready' AND se.error IS NULL",
      require_label ? "JOIN SemanticImageLabel sl ON sl.file_id = se.file_id AND "
                      "sl.model_key = se.model_key"
                    : "",
      file_id, image_id, SqlString(model_key), *model_dim);
  if (require_label) {
    sql += " AND sl.label IS NOT NULL AND sl.label <> ''";
  }
  sql += ";";

  const auto count = ScalarInt64(guard_.conn_, sql);
  return count.has_value() && *count > 0;
}

auto SemanticStorageController::CountImageLabelsForFile(sl_element_id_t    file_id,
                                                        const std::string& model_key) const
    -> size_t {
  const auto count =
      ScalarInt64(guard_.conn_, std::format("SELECT COUNT(*) FROM SemanticImageLabel "
                                            "WHERE file_id = {} AND model_key = {};",
                                            file_id, SqlString(model_key)));
  return count.has_value() ? static_cast<size_t>(*count) : 0U;
}

auto SemanticStorageController::CountImageLabelsInFolder(sl_element_id_t    folder_id,
                                                         const std::string& model_key) const
    -> size_t {
  if (model_key.empty()) {
    return 0;
  }

  const auto scope = BuildScopedFileQuery(folder_id);
  const auto count = ScalarInt64(
      guard_.conn_,
      std::format("SELECT COUNT(*) FROM SemanticImageLabel sl "
                  "JOIN (SELECT e.id AS file_id {}) scoped ON scoped.file_id = sl.file_id "
                  "WHERE sl.model_key = {} AND sl.label IS NOT NULL AND sl.label <> '';",
                  scope.from_where_, SqlString(model_key)));
  return count.has_value() ? static_cast<size_t>(*count) : 0U;
}

auto SemanticStorageController::CountLabelPrototypes(const std::string& model_key,
                                                     const std::string& prompt_config_hash) const
    -> size_t {
  const auto count =
      ScalarInt64(guard_.conn_, std::format("SELECT COUNT(*) FROM SemanticLabelPrototype "
                                            "WHERE model_key = {} AND prompt_config_hash = {};",
                                            SqlString(model_key), SqlString(prompt_config_hash)));
  return count.has_value() ? static_cast<size_t>(*count) : 0U;
}

auto SemanticStorageController::CountLabelQueries(const std::string& prompt_config_hash) const
    -> size_t {
  const auto count =
      ScalarInt64(guard_.conn_, std::format("SELECT COUNT(*) FROM SemanticLabelQuery "
                                            "WHERE prompt_config_hash = {};",
                                            SqlString(prompt_config_hash)));
  return count.has_value() ? static_cast<size_t>(*count) : 0U;
}

auto SemanticStorageController::ListLabelQueries(const std::string& prompt_config_hash,
                                                 std::string*       error) const
    -> std::vector<SemanticLabelQueryRecord> {
  std::vector<SemanticLabelQueryRecord> out;
  const auto                            sql = std::format(
      "SELECT prompt_config_hash, label, query_text FROM SemanticLabelQuery "
                                 "WHERE prompt_config_hash = {} ORDER BY label;",
      SqlString(prompt_config_hash));

  duckdb_result result;
  if (duckdb_query(guard_.conn_, sql.c_str(), &result) != DuckDBSuccess) {
    const char* raw_error = duckdb_result_error(&result);
    SetError(error, raw_error ? raw_error : "DuckDB semantic label query list failed.");
    duckdb_destroy_result(&result);
    return out;
  }

  const auto row_count = duckdb_row_count(&result);
  out.reserve(static_cast<size_t>(row_count));
  for (idx_t r = 0; r < row_count; ++r) {
    SemanticLabelQueryRecord record;
    if (char* raw = duckdb_value_varchar(&result, 0, r)) {
      record.prompt_config_hash_ = raw;
      duckdb_free(raw);
    }
    if (char* raw = duckdb_value_varchar(&result, 1, r)) {
      record.label_ = raw;
      duckdb_free(raw);
    }
    if (char* raw = duckdb_value_varchar(&result, 2, r)) {
      record.query_text_ = raw;
      duckdb_free(raw);
    }
    out.push_back(std::move(record));
  }

  duckdb_destroy_result(&result);
  return out;
}

auto SemanticStorageController::LoadLabelPrototypes(const std::string& model_key,
                                                    const std::string& prompt_config_hash,
                                                    std::string*       error) const
    -> std::vector<SemanticGenerationLabelPrototype> {
  std::vector<SemanticGenerationLabelPrototype> out;
  const auto                                    model_dim = GetModelEmbeddingDim(model_key);
  if (!model_dim.has_value()) {
    SetError(error, "Semantic model is not registered.");
    return out;
  }
  if (*model_dim != kSemanticEmbeddingDim) {
    SetError(error, "Semantic storage currently supports 512-dimensional embeddings only.");
    return out;
  }

  std::ostringstream columns;
  for (int i = 1; i <= kSemanticEmbeddingDim; ++i) {
    columns << ", embedding[" << i << "]";
  }
  const auto sql = std::format(
      "SELECT label{} FROM SemanticLabelPrototype "
      "WHERE model_key = {} AND prompt_config_hash = {} ORDER BY label;",
      columns.str(), SqlString(model_key), SqlString(prompt_config_hash));

  duckdb_result result;
  if (duckdb_query(guard_.conn_, sql.c_str(), &result) != DuckDBSuccess) {
    const char* raw_error = duckdb_result_error(&result);
    SetError(error, raw_error ? raw_error : "DuckDB semantic label prototype query failed.");
    duckdb_destroy_result(&result);
    return out;
  }

  const auto row_count = duckdb_row_count(&result);
  out.reserve(static_cast<size_t>(row_count));
  for (idx_t r = 0; r < row_count; ++r) {
    SemanticGenerationLabelPrototype prototype;
    if (char* raw = duckdb_value_varchar(&result, 0, r)) {
      prototype.label = raw;
      duckdb_free(raw);
    }
    prototype.embedding.resize(kSemanticEmbeddingDim, 0.0F);
    for (int i = 0; i < kSemanticEmbeddingDim; ++i) {
      prototype.embedding[static_cast<size_t>(i)] =
          duckdb_value_float(&result, static_cast<idx_t>(i + 1), r);
    }
    out.push_back(std::move(prototype));
  }

  duckdb_destroy_result(&result);
  return out;
}

auto SemanticStorageController::GetImageLabelForFile(sl_element_id_t    file_id,
                                                     const std::string& model_key,
                                                     std::string*       error) const
    -> std::optional<SemanticImageLabelRecord> {
  const auto sql = std::format(
      "SELECT file_id, model_key, label, score, second_label, second_score, "
      "margin, confident, top_scores "
      "FROM SemanticImageLabel WHERE file_id = {} AND model_key = {};",
      file_id, SqlString(model_key));

  duckdb_result result;
  if (duckdb_query(guard_.conn_, sql.c_str(), &result) != DuckDBSuccess) {
    const char* raw_error = duckdb_result_error(&result);
    SetError(error, raw_error ? raw_error : "DuckDB semantic label query failed.");
    duckdb_destroy_result(&result);
    return std::nullopt;
  }

  if (duckdb_row_count(&result) == 0) {
    duckdb_destroy_result(&result);
    return std::nullopt;
  }

  SemanticImageLabelRecord record;
  record.file_id_ = static_cast<sl_element_id_t>(duckdb_value_int64(&result, 0, 0));
  if (char* raw = duckdb_value_varchar(&result, 1, 0)) {
    record.model_key_ = raw;
    duckdb_free(raw);
  }
  if (char* raw = duckdb_value_varchar(&result, 2, 0)) {
    record.label_ = raw;
    duckdb_free(raw);
  }
  record.score_ = duckdb_value_double(&result, 3, 0);
  if (!duckdb_value_is_null(&result, 4, 0)) {
    if (char* raw = duckdb_value_varchar(&result, 4, 0)) {
      record.second_label_ = raw;
      duckdb_free(raw);
    }
  }
  if (!duckdb_value_is_null(&result, 5, 0)) {
    record.second_score_ = duckdb_value_double(&result, 5, 0);
  }
  record.margin_    = duckdb_value_double(&result, 6, 0);
  record.confident_ = duckdb_value_boolean(&result, 7, 0);
  if (!duckdb_value_is_null(&result, 8, 0)) {
    if (char* raw = duckdb_value_varchar(&result, 8, 0)) {
      record.top_scores_json_ = raw;
      duckdb_free(raw);
    }
  }

  duckdb_destroy_result(&result);
  return record;
}

auto SemanticStorageController::SearchImageEmbeddings(
    sl_element_id_t folder_id, const std::string& model_key, std::span<const float> query_embedding,
    size_t offset, size_t limit, std::string* error) const -> std::vector<SemanticRankedFile> {
  std::vector<SemanticRankedFile> out;
  if (limit == 0) {
    return out;
  }
  const auto model_dim = GetModelEmbeddingDim(model_key);
  if (!model_dim.has_value()) {
    SetError(error, "Semantic model is not registered.");
    return out;
  }
  if (!ValidateEmbedding(query_embedding, *model_dim, error)) {
    return out;
  }
  if (!EnsureVectorSearchIndex(model_key, error)) {
    return out;
  }

  const auto scope           = BuildScopedFileQuery(folder_id);
  const auto query_vector    = VectorArrayLiteral(query_embedding);
  const auto candidate_limit = std::max<size_t>(offset + limit, 256U);
  const auto sql             = std::format(
      "WITH nearest AS ("
                  "SELECT se.file_id, se.image_id, "
                  "array_distance(se.embedding, {}) AS distance "
                  "FROM SemanticImageEmbedding se "
                  "WHERE se.model_key = {} AND se.status = 'ready' AND se.embedding_dim = {} "
                  "ORDER BY distance ASC "
                  "LIMIT {}"
                  "), "
                  "scoped AS ("
                  "SELECT e.id AS file_id, fi.image_id AS image_id, e.element_name AS file_name "
                  "{}) "
                  "SELECT scoped.file_id, scoped.image_id, scoped.file_name, "
                  "1.0 - ((nearest.distance * nearest.distance) / 2.0) AS score "
                  "FROM nearest "
                  "JOIN scoped ON scoped.file_id = nearest.file_id "
                  "AND scoped.image_id = nearest.image_id "
                  "ORDER BY nearest.distance ASC, scoped.file_id "
                  "LIMIT {} OFFSET {};",
      query_vector, SqlString(model_key), *model_dim, candidate_limit, scope.from_where_, limit,
      offset);

  duckdb_result result;
  if (duckdb_query(guard_.conn_, sql.c_str(), &result) != DuckDBSuccess) {
    const char* raw_error = duckdb_result_error(&result);
    SetError(error, raw_error ? raw_error : "DuckDB semantic search failed.");
    duckdb_destroy_result(&result);
    return out;
  }

  const auto row_count = duckdb_row_count(&result);
  out.reserve(static_cast<size_t>(row_count));
  for (idx_t r = 0; r < row_count; ++r) {
    SemanticRankedFile row;
    row.file_id_   = static_cast<sl_element_id_t>(duckdb_value_int64(&result, 0, r));
    row.image_id_  = static_cast<image_id_t>(duckdb_value_int64(&result, 1, r));
    char* name_raw = duckdb_value_varchar(&result, 2, r);
    if (name_raw) {
      row.file_name_ = name_raw;
      duckdb_free(name_raw);
    }
    row.score_ = duckdb_value_double(&result, 3, r);
    out.push_back(std::move(row));
  }

  duckdb_destroy_result(&result);
  return out;
}

auto SemanticStorageController::EnsureVectorSearchIndex(const std::string& model_key,
                                                        std::string*       error) const -> bool {
  const auto model_dim = GetModelEmbeddingDim(model_key);
  if (!model_dim.has_value()) {
    SetError(error, "Semantic model is not registered.");
    return false;
  }
  if (*model_dim != kSemanticEmbeddingDim) {
    SetError(error, "DuckDB VSS index requires the fixed 512-dimensional embedding column.");
    return false;
  }
  if (!LoadVssExtension(guard_.conn_, error)) {
    return false;
  }
  if (!RunQuery(guard_.conn_, "SET hnsw_enable_experimental_persistence = true;", error)) {
    return false;
  }
  return RunQuery(guard_.conn_,
                  "CREATE INDEX IF NOT EXISTS idx_semantic_image_embedding_hnsw "
                  "ON SemanticImageEmbedding USING HNSW (embedding);",
                  error);
}
}  // namespace alcedo
