//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <array>
#include <cstdint>
#include <filesystem>
#include <functional>
#include <optional>
#include <string>
#include <vector>

#include <duckdb.h>

class QString;

namespace alcedo {

struct MigrationStep {
  std::string from_version;
  std::string to_version;
  std::string description;
};

struct MigrationResult {
  bool        success           = false;
  std::string from_version;
  std::string to_version;
  std::string error_message;
  int         steps_completed   = 0;
  int         steps_total       = 0;
};

/// Callback for reporting migration progress.
/// Arguments: step_index (0-based), total_steps, description.
using MigrationProgressCallback =
    std::function<void(int, int, const std::string&)>;

/// ProjectMigrator handles backward-compatible loading of older project files.
/// It detects the project file version from metadata, runs migration steps
/// to bring the database schema up to the current version, and creates
/// backups before making any changes.
class ProjectMigrator {
 public:
  ProjectMigrator();
  ~ProjectMigrator() = default;

  /// Detect the project file version from metadata JSON.
  static auto DetectVersion(const std::string& metadata_json_str)
      -> std::optional<std::string>;

  /// Check whether a given version requires migration.
  auto NeedsMigration(const std::string& version) const -> bool;

  /// Get the ordered list of migration steps from `version` to current.
  auto GetMigrationPath(const std::string& version) const
      -> std::vector<MigrationStep>;

  /// Create a backup of the database file before migration.
  auto CreateBackup(const std::filesystem::path& db_path) const
      -> std::optional<std::filesystem::path>;

  /// Run all required migrations on the given database connection.
  /// The caller is responsible for providing a valid duckdb connection
  /// that is already locked.
  auto Migrate(duckdb_connection     conn,
               const std::string&   from_version,
               MigrationProgressCallback progress_cb = nullptr)
      -> MigrationResult;

  /// Run a single migration step. Returns true on success.
  auto RunStep(duckdb_connection conn,
               const MigrationStep& step) -> bool;

 private:
  /// Schema migrations keyed by target version.
  /// Each entry contains the SQL statements to bring the schema from
  /// the previous version to this version.

  /// Migration from 0.2.0 to 0.2.1: add SemanticModel columns.
  auto Migrate_0_2_0_to_0_2_1(duckdb_connection conn) -> bool;

  /// Migration from 0.2.1 to 0.2.2: add SemanticImageEmbedding768 table.
  auto Migrate_0_2_1_to_0_2_2(duckdb_connection conn) -> bool;

  /// Migration from 0.2.2 to 0.2.3: add SemanticLabelPrototype768 table.
  auto Migrate_0_2_2_to_0_2_3(duckdb_connection conn) -> bool;

  /// Migration from 0.2.3 to 0.2.4: add AiImageUnderstanding and related tables.
  auto Migrate_0_2_3_to_0_2_4(duckdb_connection conn) -> bool;

  /// Migration from 0.2.4 to 0.2.5: add thumbnail_status column to Image table
  /// and missing_file column to Element table.
  auto Migrate_0_2_4_to_0_2_5(duckdb_connection conn) -> bool;

  static constexpr std::array<const char*, 6> kVersionChain{
      "0.2.0", "0.2.1", "0.2.2", "0.2.3", "0.2.4", "0.2.5"};
};

}  // namespace alcedo
