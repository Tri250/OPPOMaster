//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "storage/controller/ai/ai_storage_controller.hpp"

#include <duckdb.h>

#include <array>
#include <cstddef>
#include <cstdint>
#include <format>
#include <string>
#include <vector>

#include "storage/mapper/duckorm/duckdb_orm.hpp"

namespace alcedo {
namespace {

// ---- understanding (AiImageUnderstanding) field descriptors ----
//
// `kInsertUnderstandingFields` lists every column EXCEPT `updated_at` (which is left to
// its DDL default `current_timestamp` and re-stamped on each upsert). Member offsets are
// real — bind_field reads them via offsetof. The STRING bind type reads a `std::string`
// member directly (the AiDescription members are plain std::string).
inline constexpr std::array<duckorm::DuckFieldDesc, 11> kInsertUnderstandingFields = {
    FIELD_AS(AiDescription, file_id_, "file_id", UINT32),
    FIELD_AS(AiDescription, task_id_, "task_id", STRING),
    FIELD_AS(AiDescription, provider_id_, "provider_id", STRING),
    FIELD_AS(AiDescription, model_id_, "model_id", STRING),
    FIELD_AS(AiDescription, prompt_profile_id_, "prompt_profile_id", STRING),
    FIELD_AS(AiDescription, rendition_kind_, "rendition_kind", STRING),
    FIELD_AS(AiDescription, caption_, "caption", STRING),
    FIELD_AS(AiDescription, tags_json_, "tags_json", STRING),
    FIELD_AS(AiDescription, scene_, "scene", STRING),
    FIELD_AS(AiDescription, confidence_, "confidence", DOUBLE),
    FIELD_AS(AiDescription, active_, "active", BOOLEAN),
};

// `kSelectUnderstandingFields` lists ALL columns in DDL order (duckorm select runs
// `SELECT *`, so the count must match `duckdb_column_count` and the order matches the
// table definition). Offsets are unused for select, so they are zero; only the type drives
// the value extraction. file_id is read as INT64 (BIGINT column, the proven read type)
// and cast to uint32; BOOLEAN/TIMESTAMP come back as varchar ("true"/"false" / date
// string), so `active` is parsed from its string and `updated_at` is ignored.
inline constexpr std::array<duckorm::DuckFieldDesc, 12> kSelectUnderstandingFields = {
    duckorm::DuckFieldDesc{"file_id", duckorm::DuckDBType::INT64, 0},
    duckorm::DuckFieldDesc{"task_id", duckorm::DuckDBType::VARCHAR, 0},
    duckorm::DuckFieldDesc{"provider_id", duckorm::DuckDBType::VARCHAR, 0},
    duckorm::DuckFieldDesc{"model_id", duckorm::DuckDBType::VARCHAR, 0},
    duckorm::DuckFieldDesc{"prompt_profile_id", duckorm::DuckDBType::VARCHAR, 0},
    duckorm::DuckFieldDesc{"rendition_kind", duckorm::DuckDBType::VARCHAR, 0},
    duckorm::DuckFieldDesc{"caption", duckorm::DuckDBType::VARCHAR, 0},
    duckorm::DuckFieldDesc{"tags_json", duckorm::DuckDBType::VARCHAR, 0},
    duckorm::DuckFieldDesc{"scene", duckorm::DuckDBType::VARCHAR, 0},
    duckorm::DuckFieldDesc{"confidence", duckorm::DuckDBType::DOUBLE, 0},
    duckorm::DuckFieldDesc{"active", duckorm::DuckDBType::BOOLEAN, 0},
    duckorm::DuckFieldDesc{"updated_at", duckorm::DuckDBType::TIMESTAMP, 0},
};

// ---- rating (AiImageRating) field descriptors ----
inline constexpr std::array<duckorm::DuckFieldDesc, 11> kInsertRatingFields = {
    FIELD_AS(AiRating, file_id_, "file_id", UINT32),
    FIELD_AS(AiRating, task_id_, "task_id", STRING),
    FIELD_AS(AiRating, provider_id_, "provider_id", STRING),
    FIELD_AS(AiRating, model_id_, "model_id", STRING),
    FIELD_AS(AiRating, prompt_profile_id_, "prompt_profile_id", STRING),
    FIELD_AS(AiRating, rendition_kind_, "rendition_kind", STRING),
    FIELD_AS(AiRating, rating_, "rating", INT32),
    FIELD_AS(AiRating, rubric_id_, "rubric_id", STRING),
    FIELD_AS(AiRating, rubric_version_, "rubric_version", STRING),
    FIELD_AS(AiRating, reasons_, "reasons", STRING),
    FIELD_AS(AiRating, active_, "active", BOOLEAN),
};

inline constexpr std::array<duckorm::DuckFieldDesc, 12> kSelectRatingFields = {
    duckorm::DuckFieldDesc{"file_id", duckorm::DuckDBType::INT64, 0},
    duckorm::DuckFieldDesc{"task_id", duckorm::DuckDBType::VARCHAR, 0},
    duckorm::DuckFieldDesc{"provider_id", duckorm::DuckDBType::VARCHAR, 0},
    duckorm::DuckFieldDesc{"model_id", duckorm::DuckDBType::VARCHAR, 0},
    duckorm::DuckFieldDesc{"prompt_profile_id", duckorm::DuckDBType::VARCHAR, 0},
    duckorm::DuckFieldDesc{"rendition_kind", duckorm::DuckDBType::VARCHAR, 0},
    duckorm::DuckFieldDesc{"rating", duckorm::DuckDBType::INT32, 0},
    duckorm::DuckFieldDesc{"rubric_id", duckorm::DuckDBType::VARCHAR, 0},
    duckorm::DuckFieldDesc{"rubric_version", duckorm::DuckDBType::VARCHAR, 0},
    duckorm::DuckFieldDesc{"reasons", duckorm::DuckDBType::VARCHAR, 0},
    duckorm::DuckFieldDesc{"active", duckorm::DuckDBType::BOOLEAN, 0},
    duckorm::DuckFieldDesc{"updated_at", duckorm::DuckDBType::TIMESTAMP, 0},
};

constexpr const char* kUnderstandingTable = "AiImageUnderstanding";
constexpr const char* kRatingTable        = "AiImageRating";

// Read a VARCHAR/JSON/BOOLEAN/TIMESTAMP cell (always returned as a unique_ptr<string> by
// duckorm select). Returns "" for a null pointer (the columns are NOT NULL DEFAULT '' so
// this only guards against a hypothetical NULL).
auto CellString(const duckorm::VarTypes& value) -> std::string {
  const auto& ptr = std::get<std::unique_ptr<std::string>>(value);
  return ptr ? *ptr : std::string{};
}

// A BOOLEAN cell comes back as the varchar "true"/"false"; treat any string starting with
// 't' (any case) as true so the parse is robust to DuckDB's casing.
auto CellBool(const duckorm::VarTypes& value) -> bool {
  const auto& ptr = std::get<std::unique_ptr<std::string>>(value);
  return ptr && !ptr->empty() && (*ptr)[0] == 't';
}

auto MapUnderstanding(const std::vector<duckorm::VarTypes>& row) -> AiDescription {
  AiDescription d;
  d.file_id_           = static_cast<sl_element_id_t>(std::get<int64_t>(row[0]));
  d.task_id_           = CellString(row[1]);
  d.provider_id_       = CellString(row[2]);
  d.model_id_          = CellString(row[3]);
  d.prompt_profile_id_ = CellString(row[4]);
  d.rendition_kind_    = CellString(row[5]);
  d.caption_           = CellString(row[6]);
  d.tags_json_         = CellString(row[7]);
  d.scene_             = CellString(row[8]);
  d.confidence_        = std::get<double>(row[9]);
  d.active_            = CellBool(row[10]);
  // row[11] is updated_at — audit-only, not surfaced on the domain object.
  return d;
}

auto MapRating(const std::vector<duckorm::VarTypes>& row) -> AiRating {
  AiRating r;
  r.file_id_           = static_cast<sl_element_id_t>(std::get<int64_t>(row[0]));
  r.task_id_           = CellString(row[1]);
  r.provider_id_       = CellString(row[2]);
  r.model_id_          = CellString(row[3]);
  r.prompt_profile_id_ = CellString(row[4]);
  r.rendition_kind_    = CellString(row[5]);
  r.rating_            = static_cast<int>(std::get<int32_t>(row[6]));
  r.rubric_id_         = CellString(row[7]);
  r.rubric_version_    = CellString(row[8]);
  r.reasons_           = CellString(row[9]);
  r.active_            = CellBool(row[10]);
  // row[11] is updated_at — audit-only.
  return r;
}

auto JoinFileIds(std::span<const sl_element_id_t> file_ids) -> std::string {
  std::string out;
  for (size_t i = 0; i < file_ids.size(); ++i) {
    if (i > 0) {
      out += ',';
    }
    out += std::to_string(file_ids[i]);
  }
  return out;
}

}  // namespace

AiStorageController::AiStorageController(DBController& db_ctrl) : db_ctrl_(db_ctrl) {}

auto AiStorageController::UpsertUnderstanding(const AiDescription& description) const -> bool {
  if (!description.IsValid()) {
    return false;  // partial/failed result — leave no active search document
  }
  auto guard = db_ctrl_.GetConnectionGuard();
  auto lock  = guard.Lock();
  duckorm::insert_or_replace(guard.conn_, kUnderstandingTable, &description,
                             kInsertUnderstandingFields, kInsertUnderstandingFields.size());
  return true;
}

auto AiStorageController::GetUnderstanding(sl_element_id_t      file_id,
                                           const std::string& task_id) const
    -> std::optional<AiDescription> {
  const auto where = std::format("file_id = {}", file_id);
  auto       guard = db_ctrl_.GetConnectionGuard();
  auto       lock  = guard.Lock();
  // Query by file_id (an integer, safely interpolated) and match task_id in C++ so no
  // string is interpolated into the predicate.
  auto rows = duckorm::select(guard.conn_, kUnderstandingTable, kSelectUnderstandingFields,
                              kSelectUnderstandingFields.size(), where.c_str());
  for (auto& row : rows) {
    auto candidate = MapUnderstanding(row);
    if (candidate.task_id_ == task_id) {
      return candidate;
    }
  }
  return std::nullopt;
}

auto AiStorageController::GetActiveUnderstanding(sl_element_id_t file_id) const
    -> std::optional<AiDescription> {
  const auto where = std::format("file_id = {} AND active = TRUE", file_id);
  auto       guard = db_ctrl_.GetConnectionGuard();
  auto       lock  = guard.Lock();
  auto rows = duckorm::select(guard.conn_, kUnderstandingTable, kSelectUnderstandingFields,
                              kSelectUnderstandingFields.size(), where.c_str());
  if (rows.empty()) {
    return std::nullopt;
  }
  return MapUnderstanding(rows.front());
}

auto AiStorageController::UpsertRating(const AiRating& rating) const -> bool {
  if (!rating.IsValid()) {
    return false;  // rating 0 (unset) or missing identity — never persisted
  }
  auto guard = db_ctrl_.GetConnectionGuard();
  auto lock  = guard.Lock();
  duckorm::insert_or_replace(guard.conn_, kRatingTable, &rating, kInsertRatingFields,
                             kInsertRatingFields.size());
  return true;
}

auto AiStorageController::GetRating(sl_element_id_t file_id, const std::string& task_id) const
    -> std::optional<AiRating> {
  const auto where = std::format("file_id = {}", file_id);
  auto       guard = db_ctrl_.GetConnectionGuard();
  auto       lock  = guard.Lock();
  auto rows = duckorm::select(guard.conn_, kRatingTable, kSelectRatingFields,
                              kSelectRatingFields.size(), where.c_str());
  for (auto& row : rows) {
    auto candidate = MapRating(row);
    if (candidate.task_id_ == task_id) {
      return candidate;
    }
  }
  return std::nullopt;
}

auto AiStorageController::GetActiveRating(sl_element_id_t file_id) const
    -> std::optional<AiRating> {
  const auto where = std::format("file_id = {} AND active = TRUE", file_id);
  auto       guard = db_ctrl_.GetConnectionGuard();
  auto       lock  = guard.Lock();
  auto rows =
      duckorm::select(guard.conn_, kRatingTable, kSelectRatingFields, kSelectRatingFields.size(),
                      where.c_str());
  if (rows.empty()) {
    return std::nullopt;
  }
  return MapRating(rows.front());
}

void AiStorageController::DeleteForFiles(std::span<const sl_element_id_t> file_ids) const {
  auto guard = db_ctrl_.GetConnectionGuard();
  auto lock  = guard.Lock();
  DeleteAiAnnotationRowsForFiles(guard.conn_, file_ids);
}

void DeleteAiAnnotationRowsForFiles(duckdb_connection conn, std::span<const sl_element_id_t> file_ids) {
  if (file_ids.empty()) {
    return;
  }
  const auto where = std::format("file_id IN ({})", JoinFileIds(file_ids));
  // duckorm::remove builds `DELETE FROM <table> WHERE <where>`; we supply only the
  // integer IN-list predicate, so no raw DELETE statement is written here.
  duckorm::remove(conn, kUnderstandingTable, where.c_str());
  duckorm::remove(conn, kRatingTable, where.c_str());
}

}  // namespace alcedo