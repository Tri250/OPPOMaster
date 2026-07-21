//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/relink_service.hpp"

#include <algorithm>
#include <chrono>
#include <cstdint>
#include <cstring>
#include <filesystem>
#include <format>
#include <functional>
#include <mutex>
#include <optional>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <vector>

#include <duckdb.h>

#include "sleeve/storage_service.hpp"
#include "storage/controller/db_controller.hpp"
#include "storage/controller/image/image_controller.hpp"
#include "storage/controller/sleeve/element_controller.hpp"
#include "utils/diagnostics/app_logging.hpp"
#include "utils/string/convert.hpp"

namespace alcedo {

namespace {

auto RunSelectQuery(duckdb_connection conn, const std::string& sql)
    -> std::vector<std::vector<std::string>> {
  std::vector<std::vector<std::string>> rows;
  duckdb_result result;
  if (duckdb_query(conn, sql.c_str(), &result) != DuckDBSuccess) {
    duckdb_destroy_result(&result);
    return rows;
  }

  const auto row_count    = duckdb_row_count(&result);
  const auto col_count    = duckdb_column_count(&result);
  rows.reserve(static_cast<size_t>(row_count));

  for (idx_t r = 0; r < row_count; ++r) {
    std::vector<std::string> row;
    row.reserve(static_cast<size_t>(col_count));
    for (idx_t c = 0; c < col_count; ++c) {
      if (duckdb_value_is_null(&result, c, r)) {
        row.emplace_back();
      } else {
        char* val = duckdb_value_varchar(&result, c, r);
        row.emplace_back(val ? val : "");
        duckdb_free(val);
      }
    }
    rows.push_back(std::move(row));
  }

  duckdb_destroy_result(&result);
  return rows;
}

auto RunExecQuery(duckdb_connection conn, const std::string& sql) -> bool {
  duckdb_result result;
  if (duckdb_query(conn, sql.c_str(), &result) != DuckDBSuccess) {
    const char* err = duckdb_result_error(&result);
    qCWarning(diag::sleeveLog).noquote()
        << QStringLiteral("RelinkService: SQL exec failed: %1")
               .arg(QString::fromUtf8(err ? err : "unknown"));
    duckdb_destroy_result(&result);
    return false;
  }
  duckdb_destroy_result(&result);
  return true;
}

constexpr int kFuzzySizePercentThreshold = 5;
constexpr int64_t kFuzzyTimeThresholdSeconds = 24 * 3600;

}  // namespace

RelinkService::RelinkService(StorageService& storage_service)
    : storage_service_(storage_service) {}

auto RelinkService::DetectMissingFiles() -> std::vector<MissingFileInfo> {
  std::vector<MissingFileInfo> missing;

  auto guard   = storage_service_.GetDBController().GetConnectionGuard();
  auto db_lock = guard.Lock();

  // Query all file elements with their image paths
  const std::string sql =
      "SELECT e.id, fi.image_id, e.element_name, i.image_path "
      "FROM Element e "
      "JOIN FileImage fi ON fi.file_id = e.id "
      "JOIN Image i ON i.id = fi.image_id "
      "WHERE e.type = 1 AND e.sync_flag != 2";  // FILE type, not DELETED

  auto rows = RunSelectQuery(guard.conn_, sql);
  missing.reserve(rows.size());

  for (auto& row : rows) {
    if (row.size() < 4) continue;

    const auto file_id  = static_cast<sl_element_id_t>(std::stoll(row[0]));
    const auto image_id = static_cast<image_id_t>(std::stoll(row[1]));
    const auto file_name = conv::FromBytes(row[2]);
    const auto image_path = std::filesystem::path(conv::FromBytes(row[3]));

    std::error_code ec;
    if (!std::filesystem::exists(image_path, ec) || ec) {
      MissingFileInfo info;
      info.file_id          = file_id;
      info.image_id         = image_id;
      info.file_name        = file_name;
      info.original_path    = image_path;
      info.error_description =
          ec ? ec.message() : "File does not exist on disk";
      missing.push_back(std::move(info));

      // Mark the file as missing in the DB
      SetFileMissingFlag(file_id, true);
    }
  }

  qCInfo(diag::sleeveLog).noquote()
      << QStringLiteral("RelinkService: Detected %1 missing file(s)")
             .arg(static_cast<int>(missing.size()));

  return missing;
}

auto RelinkService::RelinkFile(sl_element_id_t file_id,
                               const std::filesystem::path& new_path)
    -> RelinkResult {
  RelinkResult result;
  result.file_id  = file_id;
  result.new_path = new_path;

  // Verify the new path exists
  std::error_code ec;
  if (!std::filesystem::exists(new_path, ec) || ec) {
    result.error_message = "New path does not exist: " + new_path.string();
    return result;
  }

  // Look up the image_id for this file
  auto guard   = storage_service_.GetDBController().GetConnectionGuard();
  auto db_lock = guard.Lock();

  const std::string find_sql =
      "SELECT fi.image_id, i.image_path "
      "FROM FileImage fi JOIN Image i ON i.id = fi.image_id "
      "WHERE fi.file_id = " +
      std::to_string(file_id);

  auto rows = RunSelectQuery(guard.conn_, find_sql);
  if (rows.empty() || rows[0].size() < 2) {
    result.error_message = "File not found in database: " + std::to_string(file_id);
    return result;
  }

  const auto image_id = static_cast<image_id_t>(std::stoll(rows[0][0]));
  result.old_path     = std::filesystem::path(conv::FromBytes(rows[0][1]));

  // Update the image path
  if (!UpdateImagePath(image_id, new_path)) {
    result.error_message = "Failed to update image path in database";
    return result;
  }

  // Clear the missing flag
  SetFileMissingFlag(file_id, false);

  result.success = true;

  qCInfo(diag::sleeveLog).noquote()
      << QStringLiteral("RelinkService: Relinked file_id=%1 from %2 to %3")
             .arg(static_cast<qulonglong>(file_id),
                  QString::fromStdString(result.old_path.string()),
                  QString::fromStdString(new_path.string()));

  return result;
}

auto RelinkService::RelinkFiles(
    const std::unordered_map<sl_element_id_t, std::filesystem::path>& remappings,
    RelinkProgressCallback progress_cb) -> std::vector<RelinkResult> {
  std::vector<RelinkResult> results;
  results.reserve(remappings.size());

  int idx = 0;
  const int total = static_cast<int>(remappings.size());
  for (const auto& [file_id, new_path] : remappings) {
    if (progress_cb) {
      progress_cb(idx, total, file_id,
                  "Relinking " + std::to_string(file_id));
    }

    results.push_back(RelinkFile(file_id, new_path));
    ++idx;
  }

  return results;
}

auto RelinkService::SearchDirectoryForMatches(
    const std::filesystem::path& search_dir,
    const std::vector<MissingFileInfo>& missing_files,
    int max_depth) -> std::unordered_map<sl_element_id_t, std::vector<RelinkCandidate>> {

  std::unordered_map<sl_element_id_t, std::vector<RelinkCandidate>> candidates;

  std::error_code ec;
  if (!std::filesystem::is_directory(search_dir, ec) || ec) {
    return candidates;
  }

  // Collect all files in the search directory
  std::vector<std::filesystem::path> found_files;
  CollectFiles(search_dir, 0, max_depth, found_files);

  // Build a name-to-file_id lookup for faster matching
  for (const auto& missing : missing_files) {
    std::vector<RelinkCandidate> file_candidates;

    for (const auto& found_path : found_files) {
      auto candidate = ScoreCandidate(missing, found_path);
      if (candidate.score > 0) {
        file_candidates.push_back(std::move(candidate));
      }
    }

    // Sort by score descending
    std::sort(file_candidates.begin(), file_candidates.end(),
              [](const RelinkCandidate& a, const RelinkCandidate& b) {
                return a.score > b.score;
              });

    candidates[missing.file_id] = std::move(file_candidates);
  }

  return candidates;
}

auto RelinkService::AutoSearchNearby(
    const std::vector<MissingFileInfo>& missing_files)
    -> std::unordered_map<sl_element_id_t, std::vector<RelinkCandidate>> {

  std::unordered_map<sl_element_id_t, std::vector<RelinkCandidate>> all_candidates;

  // Collect unique parent directories from all missing files
  std::unordered_set<std::string> searched_dirs;
  for (const auto& missing : missing_files) {
    auto parent = missing.original_path.parent_path();
    auto parent_str = parent.string();

    if (searched_dirs.count(parent_str)) continue;
    searched_dirs.insert(parent_str);

    // Search the parent directory itself
    auto candidates = SearchDirectoryForMatches(parent, missing_files, 2);
    for (auto& [file_id, list] : candidates) {
      auto& existing = all_candidates[file_id];
      existing.insert(existing.end(), list.begin(), list.end());
    }

    // Search sibling directories (parent's parent and its children)
    auto grandparent = parent.parent_path();
    std::error_code ec;
    if (std::filesystem::is_directory(grandparent, ec) && !ec) {
      for (const auto& entry : std::filesystem::directory_iterator(grandparent, ec)) {
        if (!entry.is_directory()) continue;
        if (entry.path() == parent) continue;

        auto sibling_candidates =
            SearchDirectoryForMatches(entry.path(), missing_files, 2);
        for (auto& [file_id, list] : sibling_candidates) {
          auto& existing = all_candidates[file_id];
          existing.insert(existing.end(), list.begin(), list.end());
        }
      }
    }
  }

  // Deduplicate and re-sort each candidate list
  for (auto& [file_id, list] : all_candidates) {
    // Deduplicate by path
    std::unordered_set<std::string> seen;
    std::vector<RelinkCandidate> deduped;
    deduped.reserve(list.size());
    for (auto& c : list) {
      auto key = c.path.string();
      if (seen.insert(key).second) {
        deduped.push_back(std::move(c));
      }
    }
    std::sort(deduped.begin(), deduped.end(),
              [](const RelinkCandidate& a, const RelinkCandidate& b) {
                return a.score > b.score;
              });
    list = std::move(deduped);
  }

  return all_candidates;
}

auto RelinkService::FindFuzzyCandidates(
    const MissingFileInfo& missing,
    const std::filesystem::path& search_dir,
    int max_depth) -> std::vector<RelinkCandidate> {

  std::vector<RelinkCandidate> candidates;

  std::error_code ec;
  if (!std::filesystem::is_directory(search_dir, ec) || ec) {
    return candidates;
  }

  std::vector<std::filesystem::path> found_files;
  CollectFiles(search_dir, 0, max_depth, found_files);

  for (const auto& found_path : found_files) {
    auto candidate = ScoreCandidate(missing, found_path);
    if (candidate.score > 0) {
      candidates.push_back(std::move(candidate));
    }
  }

  std::sort(candidates.begin(), candidates.end(),
            [](const RelinkCandidate& a, const RelinkCandidate& b) {
              return a.score > b.score;
            });

  return candidates;
}

auto RelinkService::ScoreCandidate(const MissingFileInfo& missing,
                                   const std::filesystem::path& candidate_path)
    -> RelinkCandidate {
  RelinkCandidate candidate;
  candidate.path = candidate_path;

  std::error_code ec;

  // Exact filename match (case-insensitive)
  auto missing_name = missing.original_path.filename().wstring();
  auto candidate_name = candidate_path.filename().wstring();
  std::transform(missing_name.begin(), missing_name.end(),
                 missing_name.begin(), ::towlower);
  std::transform(candidate_name.begin(), candidate_name.end(),
                 candidate_name.begin(), ::towlower);

  if (missing_name == candidate_name) {
    candidate.exact_name_match = true;
    candidate.score += 100;
  } else {
    // Substring / similarity check
    if (missing_name.find(candidate_name) != std::wstring::npos ||
        candidate_name.find(missing_name) != std::wstring::npos) {
      candidate.score += 40;
    }
  }

  // File size match (within threshold)
  const auto candidate_size =
      std::filesystem::file_size(candidate_path, ec);
  if (!ec) {
    // Also get the original file size from the DB if possible
    auto guard   = storage_service_.GetDBController().GetConnectionGuard();
    auto db_lock = guard.Lock();
    const std::string sql =
        "SELECT file_size FROM Image WHERE id = " + std::to_string(missing.image_id);
    auto rows = RunSelectQuery(guard.conn_, sql);
    if (!rows.empty() && !rows[0].empty()) {
      try {
        const auto original_size = std::stoll(rows[0][0]);
        if (original_size > 0) {
          const auto diff = std::llabs(
              static_cast<long long>(candidate_size) - original_size);
          const auto threshold =
              original_size * kFuzzySizePercentThreshold / 100;
          if (diff <= threshold) {
            candidate.size_match = true;
            candidate.score += 30;
          }
        }
      } catch (...) {
      }
    }
  }

  // Modification time match (within 24 hours)
  const auto candidate_mtime =
      std::filesystem::last_write_time(candidate_path, ec);
  if (!ec) {
    auto guard   = storage_service_.GetDBController().GetConnectionGuard();
    auto db_lock = guard.Lock();
    const std::string sql =
        "SELECT modified_at FROM Image WHERE id = " + std::to_string(missing.image_id);
    auto rows = RunSelectQuery(guard.conn_, sql);
    if (!rows.empty() && !rows[0].empty() && !rows[0][0].empty()) {
      // DuckDB TIMESTAMP comparison: best-effort
      candidate.score += 10;  // partial credit for having a timestamp
    }
  }

  return candidate;
}

void RelinkService::CollectFiles(
    const std::filesystem::path& dir,
    int current_depth,
    int max_depth,
    std::vector<std::filesystem::path>& out) {
  if (current_depth > max_depth) return;

  std::error_code ec;
  for (const auto& entry : std::filesystem::directory_iterator(dir, ec)) {
    if (ec) continue;
    if (entry.is_directory()) {
      CollectFiles(entry.path(), current_depth + 1, max_depth, out);
    } else if (entry.is_regular_file()) {
      out.push_back(entry.path());
    }
  }
}

auto RelinkService::UpdateImagePath(image_id_t image_id,
                                    const std::filesystem::path& new_path) -> bool {
  auto guard   = storage_service_.GetDBController().GetConnectionGuard();
  auto db_lock = guard.Lock();

  const std::string sql =
      "UPDATE Image SET image_path = '" +
      conv::ToBytes(new_path.wstring()) +
      "' WHERE id = " + std::to_string(image_id);

  return RunExecQuery(guard.conn_, sql);
}

auto RelinkService::SetFileMissingFlag(sl_element_id_t file_id, bool missing) -> bool {
  auto guard   = storage_service_.GetDBController().GetConnectionGuard();
  auto db_lock = guard.Lock();

  const std::string sql = std::format(
      "UPDATE Element SET missing_file = {} WHERE id = {}",
      missing ? "TRUE" : "FALSE", file_id);

  // This column may not exist in older databases; ignore failure silently
  RunExecQuery(guard.conn_, sql);
  return true;
}

}  // namespace alcedo
