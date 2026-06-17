//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "storage/controller/semantic/semantic_storage_controller.hpp"

#include <duckdb.h>

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdlib>
#include <filesystem>
#include <format>
#include <iomanip>
#include <optional>
#include <sstream>
#include <string>
#include <unordered_map>
#include <vector>

#include "storage/controller/sleeve/element_controller.hpp"
#include "storage/mapper/duckorm/duckdb_orm.hpp"

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

auto InsertReadyImageEmbedding(duckdb_connection conn, const SemanticImageEmbeddingRecord& record,
                               int model_dim, std::string* error) -> bool {
  static constexpr std::array<duckorm::DuckFieldDesc, 5> fields = {
      FIELD_AS(SemanticImageEmbeddingRecord, file_id_, "file_id", UINT32),
      FIELD_AS(SemanticImageEmbeddingRecord, image_id_, "image_id", UINT32),
      FIELD_AS(SemanticImageEmbeddingRecord, model_key_, "model_key", STRING),
      FIELD_AS(SemanticImageEmbeddingRecord, embedding_, "embedding", FLOAT_ARRAY),
      FIELD_AS(SemanticImageEmbeddingRecord, thumbnail_resolution_, "thumbnail_resolution", INT32)};
  try {
    duckorm::insert_by_query(conn,
                             std::format("INSERT INTO SemanticImageEmbedding "
                                         "(file_id, image_id, model_key, embedding, embedding_dim, "
                                         "thumbnail_resolution, status, error) "
                                         "VALUES (?, ?, ?, ?, {}, ?, 'ready', NULL);",
                                         model_dim),
                             &record, fields, fields.size());
    return true;
  } catch (const std::exception& e) {
    SetError(error, e.what());
    return false;
  }
}

auto UpsertLabelPrototypePrepared(duckdb_connection                   conn,
                                  const SemanticLabelPrototypeRecord& record, std::string* error)
    -> bool {
  static constexpr std::array<duckorm::DuckFieldDesc, 4> fields = {
      FIELD_AS(SemanticLabelPrototypeRecord, model_key_, "model_key", STRING),
      FIELD_AS(SemanticLabelPrototypeRecord, label_, "label", STRING),
      FIELD_AS(SemanticLabelPrototypeRecord, prompt_config_hash_, "prompt_config_hash", STRING),
      FIELD_AS(SemanticLabelPrototypeRecord, embedding_, "embedding", FLOAT_ARRAY)};
  try {
    duckorm::insert_or_replace(conn, "SemanticLabelPrototype", &record, fields, fields.size());
    return true;
  } catch (const std::exception& e) {
    SetError(error, e.what());
    return false;
  }
}

auto StoreQueryEmbeddingTempTable(duckdb_connection conn, std::span<const float> query_embedding,
                                  std::string* error) -> bool {
  struct QueryEmbeddingRow {
    std::vector<float> embedding_;
  };
  static constexpr std::array<duckorm::DuckFieldDesc, 1> fields = {
      FIELD_AS(QueryEmbeddingRow, embedding_, "embedding", FLOAT_ARRAY)};
  const QueryEmbeddingRow row{{query_embedding.begin(), query_embedding.end()}};
  if (!RunQuery(conn, "CREATE OR REPLACE TEMP TABLE SemanticQueryEmbedding(embedding FLOAT[512]);",
                error)) {
    return false;
  }
  try {
    duckorm::insert_by_query(conn, "INSERT INTO SemanticQueryEmbedding VALUES (?);", &row, fields,
                             fields.size());
    return true;
  } catch (const std::exception& e) {
    SetError(error, e.what());
    return false;
  }
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

auto ScalarString(duckdb_connection conn, const std::string& sql) -> std::string {
  duckdb_result result;
  if (duckdb_query(conn, sql.c_str(), &result) != DuckDBSuccess) {
    duckdb_destroy_result(&result);
    return {};
  }

  std::string value;
  if (duckdb_row_count(&result) > 0 && duckdb_column_count(&result) > 0 &&
      !duckdb_value_is_null(&result, 0, 0)) {
    if (char* raw = duckdb_value_varchar(&result, 0, 0)) {
      value = raw;
      duckdb_free(raw);
    }
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
  const auto count = std::min({limit, kMaxSemanticImageLabelCount, scores.size()});
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
  static constexpr std::array<duckorm::DuckFieldDesc, 9> fields = {
      FIELD_AS(SemanticImageLabelRecord, file_id_, "file_id", UINT32),
      FIELD_AS(SemanticImageLabelRecord, model_key_, "model_key", STRING),
      FIELD_AS(SemanticImageLabelRecord, label_, "label", STRING),
      FIELD_AS(SemanticImageLabelRecord, score_, "score", DOUBLE),
      FIELD_AS(SemanticImageLabelRecord, second_label_, "second_label", NULLABLE_STRING),
      FIELD_AS(SemanticImageLabelRecord, second_score_, "second_score", NULLABLE_DOUBLE),
      FIELD_AS(SemanticImageLabelRecord, margin_, "margin", DOUBLE),
      FIELD_AS(SemanticImageLabelRecord, confident_, "confident", BOOLEAN),
      FIELD_AS(SemanticImageLabelRecord, top_scores_json_, "top_scores", NULLABLE_STRING)};
  try {
    duckorm::insert_by_query(
        conn,
        "INSERT INTO SemanticImageLabel "
        "(file_id, model_key, label, score, second_label, second_score, margin, confident, "
        "top_scores) "
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);",
        &label, fields, fields.size());
    return true;
  } catch (const std::exception& e) {
    SetError(error, e.what());
    return false;
  }
}

auto UpsertSemanticModel(duckdb_connection conn, const SemanticModelRecord& model,
                         std::string* error) -> bool {
  static constexpr std::array<duckorm::DuckFieldDesc, 11> fields = {
      FIELD_AS(SemanticModelRecord, model_key_, "model_key", STRING),
      FIELD_AS(SemanticModelRecord, model_id_, "model_id", STRING),
      FIELD_AS(SemanticModelRecord, revision_, "revision", STRING),
      FIELD_AS(SemanticModelRecord, embedding_dim_, "embedding_dim", INT32),
      FIELD_AS(SemanticModelRecord, image_size_, "image_size", INT32),
      FIELD_AS(SemanticModelRecord, engine_id_, "engine_id", NULLABLE_STRING),
      FIELD_AS(SemanticModelRecord, profile_id_, "profile_id", NULLABLE_STRING),
      FIELD_AS(SemanticModelRecord, supported_text_languages_json_, "supported_text_languages_json",
               NULLABLE_STRING),
      FIELD_AS(SemanticModelRecord, prompt_config_hash_, "prompt_config_hash", NULLABLE_STRING),
      FIELD_AS(SemanticModelRecord, asset_manifest_json_, "asset_manifest_json", NULLABLE_STRING),
      FIELD_AS(SemanticModelRecord, active_, "active", BOOLEAN)};
  try {
    duckorm::insert_or_replace(conn, "SemanticModel", &model, fields, fields.size());
    return true;
  } catch (const std::exception& e) {
    SetError(error, e.what());
    return false;
  }
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

  const auto result_limit = std::max<size_t>(
      std::min(assignment_options.top_score_count_, kMaxSemanticImageLabelCount), 2U);
  const auto sql = std::format(
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

// RAII guard for a DuckDB logical type. The duckorm translation unit has its own; we
// keep a local one here so the semantic controller can build FLOAT array values for
// the Appender without depending on duckorm internals.
class SemanticLogicalTypeGuard {
 public:
  explicit SemanticLogicalTypeGuard(duckdb_logical_type type = nullptr) : type_(type) {}
  SemanticLogicalTypeGuard(const SemanticLogicalTypeGuard&)                    = delete;
  auto operator=(const SemanticLogicalTypeGuard&) -> SemanticLogicalTypeGuard& = delete;
  ~SemanticLogicalTypeGuard() {
    if (type_ != nullptr) {
      duckdb_destroy_logical_type(&type_);
    }
  }
  [[nodiscard]] auto get() const -> duckdb_logical_type { return type_; }

 private:
  duckdb_logical_type type_;
};

// Bulk-inserts ready image embedding rows through the DuckDB Appender, the most
// efficient load path in the C API. Runs within the caller's active transaction:
// the Appender participates in the connection's transaction and does not auto-commit,
// so flushed rows are visible to subsequent same-connection queries and are published
// only on COMMIT. Only the non-default columns are appended; `generated_at` and
// `error` fall back to their column defaults.
auto AppendImageEmbeddingRows(duckdb_connection conn,
                              std::span<const SemanticImageEmbeddingRecord> records, int model_dim,
                              std::string* error) -> bool {
  duckdb_appender appender = nullptr;
  if (duckdb_appender_create(conn, nullptr, "SemanticImageEmbedding", &appender) != DuckDBSuccess) {
    const char* msg = duckdb_appender_error(appender);
    SetError(error, msg ? msg : "DuckDB appender create failed for SemanticImageEmbedding");
    duckdb_appender_destroy(&appender);
    return false;
  }

  static constexpr std::array<const char*, 7> kActiveColumns = {
      "file_id", "image_id", "model_key", "embedding",
      "embedding_dim", "thumbnail_resolution", "status"};
  for (const char* column : kActiveColumns) {
    if (duckdb_appender_add_column(appender, column) != DuckDBSuccess) {
      const char* msg = duckdb_appender_error(appender);
      SetError(error, msg ? msg : "DuckDB appender add_column failed for SemanticImageEmbedding");
      duckdb_appender_destroy(&appender);
      return false;
    }
  }

  SemanticLogicalTypeGuard float_type(duckdb_create_logical_type(DUCKDB_TYPE_FLOAT));
  if (!float_type.get()) {
    SetError(error, "DuckDB failed to create FLOAT logical type for SemanticImageEmbedding");
    duckdb_appender_destroy(&appender);
    return false;
  }

  bool ok = true;
  for (const auto& record : records) {
    if (duckdb_append_uint32(appender, static_cast<uint32_t>(record.file_id_)) != DuckDBSuccess ||
        duckdb_append_uint32(appender, static_cast<uint32_t>(record.image_id_)) != DuckDBSuccess ||
        duckdb_append_varchar(appender, record.model_key_.c_str()) != DuckDBSuccess) {
      ok = false;
      break;
    }

    const auto dim = record.embedding_.size();
    std::vector<duckdb_value> values(dim, nullptr);
    bool values_ok = dim == static_cast<size_t>(model_dim);
    for (size_t i = 0; i < dim && values_ok; ++i) {
      values[i] = duckdb_create_float(record.embedding_[i]);
      if (values[i] == nullptr) {
        values_ok = false;
      }
    }
    duckdb_value array_value = nullptr;
    if (values_ok) {
      array_value =
          duckdb_create_array_value(float_type.get(), values.data(), static_cast<idx_t>(dim));
    }
    for (duckdb_value value : values) {
      if (value != nullptr) {
        duckdb_destroy_value(&value);
      }
    }
    if (array_value == nullptr) {
      ok = false;
      break;
    }
    const duckdb_state bind_state = duckdb_append_value(appender, array_value);
    duckdb_destroy_value(&array_value);
    if (bind_state != DuckDBSuccess) {
      ok = false;
      break;
    }

    if (duckdb_append_int32(appender, static_cast<int32_t>(model_dim)) != DuckDBSuccess ||
        duckdb_append_int32(appender, static_cast<int32_t>(record.thumbnail_resolution_)) !=
            DuckDBSuccess ||
        duckdb_append_varchar(appender, "ready") != DuckDBSuccess ||
        duckdb_appender_end_row(appender) != DuckDBSuccess) {
      ok = false;
      break;
    }
  }

  if (ok && duckdb_appender_flush(appender) != DuckDBSuccess) {
    ok = false;
  }
  if (!ok) {
    const char* msg = duckdb_appender_error(appender);
    SetError(error, msg ? msg : "DuckDB appender failed for SemanticImageEmbedding");
  }
  duckdb_appender_destroy(&appender);
  return ok;
}

// Bulk-inserts assigned label rows through the DuckDB Appender within the caller's
// active transaction. `updated_at` falls back to its column default.
auto AppendSemanticLabelRows(duckdb_connection conn,
                             std::span<const SemanticImageLabelRecord> labels,
                             std::string* error) -> bool {
  duckdb_appender appender = nullptr;
  if (duckdb_appender_create(conn, nullptr, "SemanticImageLabel", &appender) != DuckDBSuccess) {
    const char* msg = duckdb_appender_error(appender);
    SetError(error, msg ? msg : "DuckDB appender create failed for SemanticImageLabel");
    duckdb_appender_destroy(&appender);
    return false;
  }

  static constexpr std::array<const char*, 9> kActiveColumns = {
      "file_id", "model_key", "label", "score", "second_label",
      "second_score", "margin", "confident", "top_scores"};
  for (const char* column : kActiveColumns) {
    if (duckdb_appender_add_column(appender, column) != DuckDBSuccess) {
      const char* msg = duckdb_appender_error(appender);
      SetError(error, msg ? msg : "DuckDB appender add_column failed for SemanticImageLabel");
      duckdb_appender_destroy(&appender);
      return false;
    }
  }

  auto append_nullable_string = [&appender](const std::string& value) -> bool {
    if (value.empty()) {
      return duckdb_append_null(appender) == DuckDBSuccess;
    }
    return duckdb_append_varchar(appender, value.c_str()) == DuckDBSuccess;
  };

  bool ok = true;
  for (const auto& label : labels) {
    if (duckdb_append_uint32(appender, static_cast<uint32_t>(label.file_id_)) != DuckDBSuccess ||
        duckdb_append_varchar(appender, label.model_key_.c_str()) != DuckDBSuccess ||
        duckdb_append_varchar(appender, label.label_.c_str()) != DuckDBSuccess ||
        duckdb_append_double(appender, label.score_) != DuckDBSuccess ||
        !append_nullable_string(label.second_label_)) {
      ok = false;
      break;
    }
    if (label.second_score_.has_value()) {
      if (duckdb_append_double(appender, *label.second_score_) != DuckDBSuccess) {
        ok = false;
        break;
      }
    } else {
      if (duckdb_append_null(appender) != DuckDBSuccess) {
        ok = false;
        break;
      }
    }
    if (duckdb_append_double(appender, label.margin_) != DuckDBSuccess ||
        duckdb_append_bool(appender, label.confident_) != DuckDBSuccess ||
        !append_nullable_string(label.top_scores_json_) ||
        duckdb_appender_end_row(appender) != DuckDBSuccess) {
      ok = false;
      break;
    }
  }

  if (ok && duckdb_appender_flush(appender) != DuckDBSuccess) {
    ok = false;
  }
  if (!ok) {
    const char* msg = duckdb_appender_error(appender);
    SetError(error, msg ? msg : "DuckDB appender failed for SemanticImageLabel");
  }
  duckdb_appender_destroy(&appender);
  return ok;
}

// Assigns labels for an entire batch with a single SQL query. Ranks every prototype
// against each just-inserted embedding and keeps the top-N per file via a window
// function, then builds one SemanticImageLabelRecord per input record. `out_labels`
// is resized to `records.size()` and filled in input order. Requires all records to
// share the same model_key and to have unique file_ids.
auto QueryAssignedLabelsBatch(duckdb_connection conn,
                              std::span<const SemanticImageEmbeddingRecord> records,
                              const std::string& model_key,
                              const SemanticLabelAssignmentOptions& assignment_options,
                              std::vector<SemanticImageLabelRecord>& out_labels,
                              std::string* error) -> bool {
  out_labels.assign(records.size(), SemanticImageLabelRecord{});

  std::unordered_map<sl_element_id_t, size_t> index_by_file;
  index_by_file.reserve(records.size() * 2);
  std::string id_list;
  for (size_t i = 0; i < records.size(); ++i) {
    if (i > 0) {
      id_list += ",";
    }
    id_list += std::to_string(records[i].file_id_);
    index_by_file.emplace(records[i].file_id_, i);
  }

  const auto result_limit = std::max<size_t>(
      std::min(assignment_options.top_score_count_, kMaxSemanticImageLabelCount), 2U);
  const auto sql = std::format(
      "WITH scored AS ("
      "SELECT se.file_id AS file_id, lp.label AS label, "
      "array_inner_product(lp.embedding, se.embedding) AS score "
      "FROM SemanticImageEmbedding se "
      "JOIN SemanticLabelPrototype lp ON lp.model_key = se.model_key "
      "AND lp.prompt_config_hash = {} "
      "WHERE se.model_key = {} AND se.status = 'ready' AND se.error IS NULL "
      "AND se.file_id IN ({})"
      ") SELECT file_id, label, score FROM ("
      "SELECT file_id, label, score, "
      "ROW_NUMBER() OVER (PARTITION BY file_id ORDER BY score DESC, label) AS rn "
      "FROM scored) ranked WHERE rn <= {} ORDER BY file_id, rn;",
      SqlString(assignment_options.prompt_config_hash_), SqlString(model_key), id_list,
      result_limit);

  duckdb_result result;
  if (duckdb_query(conn, sql.c_str(), &result) != DuckDBSuccess) {
    const char* raw_error = duckdb_result_error(&result);
    SetError(error, raw_error ? raw_error : "DuckDB batched label assignment failed.");
    duckdb_destroy_result(&result);
    return false;
  }

  std::vector<std::pair<std::string, double>> scores;
  sl_element_id_t current_file = 0;
  bool have_current            = false;
  auto flush_group = [&](sl_element_id_t file_id) -> bool {
    if (!have_current) {
      return true;
    }
    have_current = false;
    if (scores.empty()) {
      return false;
    }
    const auto it = index_by_file.find(file_id);
    if (it == index_by_file.end()) {
      return false;
    }
    out_labels[it->second] =
        BuildAssignedLabel(file_id, model_key, assignment_options, scores);
    scores.clear();
    return true;
  };

  bool ok = true;
  const idx_t row_count = duckdb_row_count(&result);
  for (idx_t r = 0; r < row_count; ++r) {
    const auto file_id = static_cast<sl_element_id_t>(duckdb_value_int64(&result, 0, r));
    std::string label;
    if (char* raw = duckdb_value_varchar(&result, 1, r)) {
      label = raw;
      duckdb_free(raw);
    }
    if (label.empty()) {
      ok = false;
      break;
    }
    if (have_current && file_id != current_file) {
      if (!flush_group(current_file)) {
        ok = false;
        break;
      }
    }
    current_file = file_id;
    have_current = true;
    scores.emplace_back(std::move(label), duckdb_value_double(&result, 2, r));
  }
  if (ok && have_current && !flush_group(current_file)) {
    ok = false;
  }
  duckdb_destroy_result(&result);
  if (!ok) {
    SetError(error, "Semantic label assignment returned incomplete results for the batch.");
    return false;
  }

  for (size_t i = 0; i < records.size(); ++i) {
    if (out_labels[i].label_.empty()) {
      SetError(error, "Semantic label assignment produced no label for a file in the batch.");
      return false;
    }
  }
  return true;
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

  if (!model.active_) {
    return UpsertSemanticModel(guard_.conn_, model, error);
  }
  if (!RunQuery(guard_.conn_, "BEGIN TRANSACTION;", error)) {
    return false;
  }
  if (!RunQuery(guard_.conn_, "UPDATE SemanticModel SET active = FALSE;", error) ||
      !UpsertSemanticModel(guard_.conn_, model, error)) {
    std::string rollback_error;
    RunQuery(guard_.conn_, "ROLLBACK;", &rollback_error);
    return false;
  }
  return RunQuery(guard_.conn_, "COMMIT;", error);
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

auto SemanticStorageController::GetModelSupportedTextLanguagesJson(
    const std::string& model_key) const -> std::string {
  return ScalarString(guard_.conn_,
                      std::format("SELECT supported_text_languages_json FROM SemanticModel "
                                  "WHERE model_key = {};",
                                  SqlString(model_key)));
}

auto SemanticStorageController::GetModel(const std::string& model_key, std::string* error) const
    -> std::optional<SemanticModelRecord> {
  const auto sql = std::format(
      "SELECT model_key, model_id, revision, embedding_dim, image_size, "
      "engine_id, profile_id, supported_text_languages_json, prompt_config_hash, "
      "asset_manifest_json, active "
      "FROM SemanticModel WHERE model_key = {} LIMIT 1;",
      SqlString(model_key));

  duckdb_result result;
  if (duckdb_query(guard_.conn_, sql.c_str(), &result) != DuckDBSuccess) {
    const char* raw_error = duckdb_result_error(&result);
    SetError(error, raw_error ? raw_error : "DuckDB semantic model query failed.");
    duckdb_destroy_result(&result);
    return std::nullopt;
  }
  if (duckdb_row_count(&result) == 0) {
    duckdb_destroy_result(&result);
    return std::nullopt;
  }

  auto read_string = [&](idx_t column) {
    std::string value;
    if (!duckdb_value_is_null(&result, column, 0)) {
      if (char* raw = duckdb_value_varchar(&result, column, 0)) {
        value = raw;
        duckdb_free(raw);
      }
    }
    return value;
  };

  SemanticModelRecord record;
  record.model_key_                     = read_string(0);
  record.model_id_                      = read_string(1);
  record.revision_                      = read_string(2);
  record.embedding_dim_                 = static_cast<int>(duckdb_value_int32(&result, 3, 0));
  record.image_size_                    = static_cast<int>(duckdb_value_int32(&result, 4, 0));
  record.engine_id_                     = read_string(5);
  record.profile_id_                    = read_string(6);
  record.supported_text_languages_json_ = read_string(7);
  record.prompt_config_hash_            = read_string(8);
  record.asset_manifest_json_           = read_string(9);
  record.active_                        = duckdb_value_boolean(&result, 10, 0);
  duckdb_destroy_result(&result);
  return record;
}

auto SemanticStorageController::ActiveModel(std::string* error) const
    -> std::optional<SemanticModelRecord> {
  const auto key = ActiveModelKey();
  if (key.empty()) {
    return std::nullopt;
  }
  return GetModel(key, error);
}

auto SemanticStorageController::ActiveModelKey() const -> std::string {
  const auto sql =
      "SELECT model_key FROM SemanticModel WHERE active = TRUE "
      "ORDER BY created_at DESC, model_key DESC LIMIT 1;";

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

auto SemanticStorageController::SetActiveModelKey(const std::string& model_key,
                                                  std::string*       error) const -> bool {
  if (model_key.empty()) {
    SetError(error, "Semantic model key is empty.");
    return false;
  }
  if (!HasModel(model_key)) {
    SetError(error, "Semantic model is not registered.");
    return false;
  }
  if (!RunQuery(guard_.conn_, "BEGIN TRANSACTION;", error)) {
    return false;
  }
  if (!RunQuery(guard_.conn_, "UPDATE SemanticModel SET active = FALSE;", error) ||
      !RunQuery(guard_.conn_,
                std::format("UPDATE SemanticModel SET active = TRUE WHERE model_key = {};",
                            SqlString(model_key)),
                error)) {
    std::string rollback_error;
    RunQuery(guard_.conn_, "ROLLBACK;", &rollback_error);
    return false;
  }
  return RunQuery(guard_.conn_, "COMMIT;", error);
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

  const auto delete_sql =
      std::format("DELETE FROM SemanticImageEmbedding WHERE file_id = {} AND model_key = {};",
                  record.file_id_, SqlString(record.model_key_));
  const auto delete_label_sql =
      std::format("DELETE FROM SemanticImageLabel WHERE file_id = {} AND model_key = {};",
                  record.file_id_, SqlString(record.model_key_));
  if (!RunQuery(guard_.conn_, "BEGIN TRANSACTION;", error)) {
    return false;
  }
  if (!RunQuery(guard_.conn_, delete_sql, error) ||
      !RunQuery(guard_.conn_, delete_label_sql, error) ||
      !InsertReadyImageEmbedding(guard_.conn_, record, *model_dim, error)) {
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

  const auto delete_sql =
      std::format("DELETE FROM SemanticImageEmbedding WHERE file_id = {} AND model_key = {};",
                  record.file_id_, SqlString(record.model_key_));
  const auto delete_label_sql =
      std::format("DELETE FROM SemanticImageLabel WHERE file_id = {} AND model_key = {};",
                  record.file_id_, SqlString(record.model_key_));
  if (!RunQuery(guard_.conn_, "BEGIN TRANSACTION;", error)) {
    return false;
  }
  if (!RunQuery(guard_.conn_, delete_sql, error) ||
      !RunQuery(guard_.conn_, delete_label_sql, error) ||
      !InsertReadyImageEmbedding(guard_.conn_, record, *model_dim, error)) {
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

auto SemanticStorageController::UpsertImageEmbeddingsAndAssignLabels(
    std::span<const SemanticImageEmbeddingRecord>   records,
    const SemanticLabelAssignmentOptions&           assignment_options,
    std::vector<SemanticImageLabelRecord>* assigned_labels, std::string* error) const -> bool {
  if (assigned_labels) {
    assigned_labels->clear();
  }
  if (records.empty()) {
    return true;
  }
  if (assignment_options.prompt_config_hash_.empty()) {
    SetError(error, "Semantic label assignment prompt config hash is empty.");
    return false;
  }
  if (!std::isfinite(assignment_options.confidence_score_threshold_) ||
      !std::isfinite(assignment_options.confidence_margin_threshold_)) {
    SetError(error, "Semantic label assignment threshold contains NaN or infinity.");
    return false;
  }

  const auto model_key = records.front().model_key_;
  if (model_key.empty()) {
    SetError(error, "Semantic embedding model key is empty.");
    return false;
  }
  const auto model_dim = GetModelEmbeddingDim(model_key);
  if (!model_dim.has_value()) {
    SetError(error, "Semantic model is not registered.");
    return false;
  }
  for (const auto& record : records) {
    if (record.file_id_ == 0) {
      SetError(error, "Semantic embedding file id is zero.");
      return false;
    }
    if (record.model_key_ != model_key) {
      SetError(error, "Semantic embedding batch mixes multiple model keys.");
      return false;
    }
    if (!ValidateEmbedding(record.embedding_, *model_dim, error)) {
      return false;
    }
  }

  if (!RunQuery(guard_.conn_, "BEGIN TRANSACTION;", error)) {
    return false;
  }
  auto rollback = [&]() {
    std::string rollback_error;
    RunQuery(guard_.conn_, "ROLLBACK;", &rollback_error);
  };

  std::string id_list;
  for (size_t i = 0; i < records.size(); ++i) {
    if (i > 0) {
      id_list += ",";
    }
    id_list += std::to_string(records[i].file_id_);
  }
  const auto delete_embedding_sql =
      std::format("DELETE FROM SemanticImageEmbedding WHERE model_key = {} AND file_id IN ({});",
                  SqlString(model_key), id_list);
  const auto delete_label_sql =
      std::format("DELETE FROM SemanticImageLabel WHERE model_key = {} AND file_id IN ({});",
                  SqlString(model_key), id_list);
  if (!RunQuery(guard_.conn_, delete_embedding_sql, error) ||
      !RunQuery(guard_.conn_, delete_label_sql, error) ||
      !AppendImageEmbeddingRows(guard_.conn_, records, *model_dim, error)) {
    rollback();
    return false;
  }

  std::vector<SemanticImageLabelRecord> labels;
  if (!QueryAssignedLabelsBatch(guard_.conn_, records, model_key, assignment_options, labels,
                                error)) {
    rollback();
    return false;
  }
  for (size_t i = 0; i < records.size(); ++i) {
    if (!ValidateLabel(records[i], &labels[i], error)) {
      rollback();
      return false;
    }
  }
  if (!AppendSemanticLabelRows(guard_.conn_, labels, error)) {
    rollback();
    return false;
  }
  if (!RunQuery(guard_.conn_, "COMMIT;", error)) {
    rollback();
    return false;
  }

  if (assigned_labels) {
    *assigned_labels = std::move(labels);
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

  return UpsertLabelPrototypePrepared(guard_.conn_, record, error);
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
  if (!StoreQueryEmbeddingTempTable(guard_.conn_, query_embedding, error)) {
    return out;
  }

  const auto scope           = BuildScopedFileQuery(folder_id);
  const auto candidate_limit = std::max<size_t>(offset + limit, 256U);
  const auto sql             = std::format(
      "WITH nearest AS ("
                  "SELECT se.file_id, se.image_id, "
                  "array_distance(se.embedding, query.embedding) AS distance "
                  "FROM SemanticImageEmbedding se "
                  "CROSS JOIN SemanticQueryEmbedding query "
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
      SqlString(model_key), *model_dim, candidate_limit, scope.from_where_, limit, offset);

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
