//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/project_migrator.hpp"

#include <chrono>
#include <cstring>
#include <filesystem>
#include <format>
#include <fstream>
#include <sstream>

#include <duckdb.h>
#include <json.hpp>

#include "app/project_package_backend.hpp"
#include "utils/diagnostics/app_logging.hpp"

namespace alcedo {

namespace {

auto RunSql(duckdb_connection conn, const std::string& sql) -> bool {
  duckdb_result result;
  if (duckdb_query(conn, sql.c_str(), &result) != DuckDBSuccess) {
    const char* err = duckdb_result_error(&result);
    qCWarning(diag::projectLog).noquote()
        << QStringLiteral("ProjectMigrator: SQL failed: %1")
               .arg(QString::fromUtf8(err ? err : "unknown"));
    duckdb_destroy_result(&result);
    return false;
  }
  duckdb_destroy_result(&result);
  return true;
}

auto RunSqlList(duckdb_connection conn,
                std::initializer_list<const char*> statements) -> bool {
  for (const auto* sql : statements) {
    if (!RunSql(conn, sql)) {
      return false;
    }
  }
  return true;
}

auto TimestampSuffix() -> std::string {
  const auto now    = std::chrono::system_clock::now();
  const auto time_t = std::chrono::system_clock::to_time_t(now);
  std::tm    tm_buf{};
  std::memset(&tm_buf, 0, sizeof(tm_buf));
  std::localtime_r(&time_t, &tm_buf);
  char buf[32];
  std::strftime(buf, sizeof(buf), "%Y%m%d_%H%M%S", &tm_buf);
  return buf;
}

}  // namespace

ProjectMigrator::ProjectMigrator() = default;

auto ProjectMigrator::DetectVersion(const std::string& metadata_json_str)
    -> std::optional<std::string> {
  try {
    auto metadata = nlohmann::json::parse(metadata_json_str);
    if (metadata.contains("project_file_version") &&
        metadata.at("project_file_version").is_string()) {
      return metadata.at("project_file_version").get<std::string>();
    }
  } catch (...) {
  }
  // Pre-version projects: treat as 0.2.0
  return std::nullopt;
}

auto ProjectMigrator::NeedsMigration(const std::string& version) const -> bool {
  return version != std::string(project_pack::kProjectFileVersion);
}

auto ProjectMigrator::GetMigrationPath(const std::string& version) const
    -> std::vector<MigrationStep> {
  std::vector<MigrationStep> steps;

  int start_idx = -1;
  for (int i = 0; i < static_cast<int>(kVersionChain.size()); ++i) {
    if (kVersionChain[static_cast<size_t>(i)] == version) {
      start_idx = i;
      break;
    }
  }

  // If version not found in chain, start from the beginning (0.2.0).
  if (start_idx < 0) {
    start_idx = 0;
  }

  const int end_idx = static_cast<int>(kVersionChain.size()) - 1;
  for (int i = start_idx; i < end_idx; ++i) {
    MigrationStep step;
    step.from_version = kVersionChain[static_cast<size_t>(i)];
    step.to_version   = kVersionChain[static_cast<size_t>(i + 1)];
    step.description  = std::format("Migrate {} -> {}",
                                    step.from_version, step.to_version);
    steps.push_back(std::move(step));
  }

  return steps;
}

auto ProjectMigrator::CreateBackup(
    const std::filesystem::path& db_path) const
    -> std::optional<std::filesystem::path> {
  std::error_code ec;
  if (!std::filesystem::exists(db_path, ec) || ec) {
    qCWarning(diag::projectLog).noquote()
        << QStringLiteral("ProjectMigrator: DB path does not exist: %1")
               .arg(QString::fromStdString(db_path.string()));
    return std::nullopt;
  }

  auto backup_path = db_path;
  backup_path += ".";
  backup_path += TimestampSuffix();
  backup_path += ".bak";

  std::filesystem::copy_file(db_path, backup_path,
                             std::filesystem::copy_options::overwrite_existing,
                             ec);
  if (ec) {
    qCWarning(diag::projectLog).noquote()
        << QStringLiteral("ProjectMigrator: Backup failed: %1")
               .arg(QString::fromStdString(ec.message()));
    return std::nullopt;
  }

  qCInfo(diag::projectLog).noquote()
      << QStringLiteral("ProjectMigrator: Backup created at %1")
             .arg(QString::fromStdString(backup_path.string()));
  return backup_path;
}

auto ProjectMigrator::Migrate(duckdb_connection     conn,
                              const std::string&   from_version,
                              MigrationProgressCallback progress_cb)
    -> MigrationResult {
  MigrationResult result;
  result.from_version = from_version;
  result.to_version   = std::string(project_pack::kProjectFileVersion);

  auto steps = GetMigrationPath(from_version);
  result.steps_total = static_cast<int>(steps.size());

  if (steps.empty()) {
    result.success         = true;
    result.steps_completed = 0;
    return result;
  }

  qCInfo(diag::projectLog).noquote()
      << QStringLiteral("ProjectMigrator: Starting migration from %1 to %2 (%3 steps)")
             .arg(QString::fromStdString(from_version),
                  QString::fromStdString(result.to_version),
                  static_cast<int>(steps.size()));

  for (int i = 0; i < static_cast<int>(steps.size()); ++i) {
    const auto& step = steps[static_cast<size_t>(i)];

    qCInfo(diag::projectLog).noquote()
        << QStringLiteral("ProjectMigrator: Step %1/%2: %3")
               .arg(i + 1, result.steps_total,
                    QString::fromStdString(step.description));

    if (progress_cb) {
      progress_cb(i, result.steps_total, step.description);
    }

    if (!RunStep(conn, step)) {
      result.error_message = std::format(
          "Migration step {} -> {} failed", step.from_version, step.to_version);
      result.steps_completed = i;
      qCCritical(diag::projectLog).noquote()
          << QStringLiteral("ProjectMigrator: %1")
                 .arg(QString::fromStdString(result.error_message));
      return result;
    }

    result.steps_completed = i + 1;
  }

  // Update the project_file_version in the metadata
  if (!RunSql(conn,
              "CREATE TABLE IF NOT EXISTS _project_meta "
              "(key VARCHAR PRIMARY KEY, value VARCHAR);")) {
    result.error_message = "Failed to create _project_meta table";
    return result;
  }

  // Upsert the current version
  RunSql(conn,
         "DELETE FROM _project_meta WHERE key = 'project_file_version'");
  RunSql(conn,
         std::string("INSERT INTO _project_meta VALUES ('project_file_version', '") +
             std::string(project_pack::kProjectFileVersion) + "');");

  result.success = true;

  qCInfo(diag::projectLog).noquote()
      << QStringLiteral("ProjectMigrator: Migration completed successfully (%1 -> %2)")
             .arg(QString::fromStdString(from_version),
                  QString::fromStdString(result.to_version));

  return result;
}

auto ProjectMigrator::RunStep(duckdb_connection  conn,
                              const MigrationStep& step) -> bool {
  if (step.from_version == "0.2.0" && step.to_version == "0.2.1") {
    return Migrate_0_2_0_to_0_2_1(conn);
  }
  if (step.from_version == "0.2.1" && step.to_version == "0.2.2") {
    return Migrate_0_2_1_to_0_2_2(conn);
  }
  if (step.from_version == "0.2.2" && step.to_version == "0.2.3") {
    return Migrate_0_2_2_to_0_2_3(conn);
  }
  if (step.from_version == "0.2.3" && step.to_version == "0.2.4") {
    return Migrate_0_2_3_to_0_2_4(conn);
  }
  if (step.from_version == "0.2.4" && step.to_version == "0.2.5") {
    return Migrate_0_2_4_to_0_2_5(conn);
  }

  qCWarning(diag::projectLog).noquote()
      << QStringLiteral("ProjectMigrator: Unknown migration step %1 -> %2")
             .arg(QString::fromStdString(step.from_version),
                  QString::fromStdString(step.to_version));
  return false;
}

auto ProjectMigrator::Migrate_0_2_0_to_0_2_1(duckdb_connection conn)
    -> bool {
  // Add columns to SemanticModel that were introduced after 0.2.0.
  // DuckDB's ADD COLUMN IF NOT EXISTS is safe for idempotent migration.
  return RunSqlList(conn, {
      "ALTER TABLE SemanticModel ADD COLUMN IF NOT EXISTS engine_id VARCHAR;",
      "ALTER TABLE SemanticModel ADD COLUMN IF NOT EXISTS profile_id VARCHAR;",
      "ALTER TABLE SemanticModel ADD COLUMN IF NOT EXISTS "
      "supported_text_languages_json JSON;",
      "ALTER TABLE SemanticModel ADD COLUMN IF NOT EXISTS active "
      "BOOLEAN DEFAULT FALSE;",
      // Mark the first (or only) model as active if none is active yet
      "UPDATE SemanticModel SET active = TRUE "
      "WHERE model_key = ("
      "SELECT model_key FROM SemanticModel "
      "WHERE NOT EXISTS (SELECT 1 FROM SemanticModel WHERE active = TRUE) "
      "ORDER BY created_at DESC, model_key DESC LIMIT 1);",
  });
}

auto ProjectMigrator::Migrate_0_2_1_to_0_2_2(duckdb_connection conn)
    -> bool {
  // Add SemanticImageEmbedding768 table (introduced for 768-dim models).
  return RunSqlList(conn, {
      "CREATE TABLE IF NOT EXISTS SemanticImageEmbedding768 ("
      "file_id BIGINT NOT NULL,"
      "image_id BIGINT NOT NULL,"
      "model_key VARCHAR NOT NULL,"
      "embedding FLOAT[768] NOT NULL,"
      "embedding_dim INTEGER NOT NULL,"
      "thumbnail_resolution INTEGER NOT NULL,"
      "generated_at TIMESTAMP DEFAULT current_timestamp,"
      "status VARCHAR NOT NULL,"
      "error VARCHAR,"
      "PRIMARY KEY(file_id, model_key));",
      "CREATE INDEX IF NOT EXISTS idx_semantic_embedding768_model_file "
      "ON SemanticImageEmbedding768(model_key, file_id);",
  });
}

auto ProjectMigrator::Migrate_0_2_2_to_0_2_3(duckdb_connection conn)
    -> bool {
  // Add SemanticLabelPrototype768 table (introduced for 768-dim label
  // prototypes).
  return RunSqlList(conn, {
      "CREATE TABLE IF NOT EXISTS SemanticLabelPrototype768 ("
      "model_key VARCHAR NOT NULL,"
      "label VARCHAR NOT NULL,"
      "prompt_config_hash VARCHAR NOT NULL,"
      "embedding FLOAT[768] NOT NULL,"
      "PRIMARY KEY(model_key, label, prompt_config_hash));",
  });
}

auto ProjectMigrator::Migrate_0_2_3_to_0_2_4(duckdb_connection conn)
    -> bool {
  // Add AI image understanding and rating tables (Phase 5f).
  return RunSqlList(conn, {
      "CREATE TABLE IF NOT EXISTS AiImageUnderstanding ("
      "file_id BIGINT NOT NULL,"
      "task_id VARCHAR NOT NULL DEFAULT '',"
      "provider_id VARCHAR NOT NULL DEFAULT '',"
      "model_id VARCHAR NOT NULL DEFAULT '',"
      "prompt_profile_id VARCHAR NOT NULL DEFAULT '',"
      "rendition_kind VARCHAR NOT NULL DEFAULT '',"
      "caption VARCHAR NOT NULL DEFAULT '',"
      "tags_json VARCHAR NOT NULL DEFAULT '',"
      "scene VARCHAR NOT NULL DEFAULT '',"
      "confidence DOUBLE NOT NULL DEFAULT 0.0,"
      "active BOOLEAN NOT NULL DEFAULT TRUE,"
      "updated_at TIMESTAMP DEFAULT current_timestamp,"
      "PRIMARY KEY (file_id, task_id));",
      "CREATE INDEX IF NOT EXISTS idx_ai_understanding_file_active "
      "ON AiImageUnderstanding(file_id, active);",
      "CREATE TABLE IF NOT EXISTS AiImageFtsDocument ("
      "file_id BIGINT PRIMARY KEY,"
      "body VARCHAR NOT NULL DEFAULT '',"
      "updated_at TIMESTAMP DEFAULT current_timestamp);",
      "CREATE TABLE IF NOT EXISTS AiImageRating ("
      "file_id BIGINT NOT NULL,"
      "task_id VARCHAR NOT NULL DEFAULT '',"
      "provider_id VARCHAR NOT NULL DEFAULT '',"
      "model_id VARCHAR NOT NULL DEFAULT '',"
      "prompt_profile_id VARCHAR NOT NULL DEFAULT '',"
      "rendition_kind VARCHAR NOT NULL DEFAULT '',"
      "rating INTEGER NOT NULL DEFAULT 0,"
      "rubric_id VARCHAR NOT NULL DEFAULT '',"
      "rubric_version VARCHAR NOT NULL DEFAULT '',"
      "reasons VARCHAR NOT NULL DEFAULT '',"
      "active BOOLEAN NOT NULL DEFAULT TRUE,"
      "updated_at TIMESTAMP DEFAULT current_timestamp,"
      "PRIMARY KEY (file_id, task_id));",
      "CREATE INDEX IF NOT EXISTS idx_ai_rating_file_active "
      "ON AiImageRating(file_id, active);",
  });
}

auto ProjectMigrator::Migrate_0_2_4_to_0_2_5(duckdb_connection conn)
    -> bool {
  // Add thumbnail_status column and missing_file flag for relink support.
  // Also add prompt_config_hash column to SemanticModel if missing.
  return RunSqlList(conn, {
      "ALTER TABLE SemanticModel ADD COLUMN IF NOT EXISTS "
      "prompt_config_hash VARCHAR;",
      "ALTER TABLE SemanticModel ADD COLUMN IF NOT EXISTS "
      "asset_manifest_json JSON;",
  });
}

}  // namespace alcedo
