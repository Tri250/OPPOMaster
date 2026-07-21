//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <filesystem>
#include <functional>
#include <optional>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <vector>

#include "type/type.hpp"

namespace alcedo {

class StorageService;

/// Describes a file that is referenced in the database but missing on disk.
struct MissingFileInfo {
  sl_element_id_t   file_id;
  image_id_t        image_id;
  std::wstring      file_name;
  std::filesystem::path original_path;
  std::string       error_description;
};

/// Describes the result of a relink attempt for a single file.
struct RelinkResult {
  sl_element_id_t   file_id;
  bool              success             = false;
  std::filesystem::path old_path;
  std::filesystem::path new_path;
  std::string       error_message;
};

/// Fuzzy-match candidate for auto-search.
struct RelinkCandidate {
  std::filesystem::path path;
  int                   score = 0;  // higher = better match
  bool                  exact_name_match = false;
  bool                  size_match       = false;
};

/// Callback for reporting relink progress.
/// Arguments: current_index, total_count, file_id, description.
using RelinkProgressCallback =
    std::function<void(int, int, sl_element_id_t, const std::string&)>;

/// RelinkService detects missing files when a project is opened and provides
/// mechanisms to find and relink them while preserving all edit history and
/// adjustments.
class RelinkService {
 public:
  explicit RelinkService(StorageService& storage_service);
  ~RelinkService() = default;

  /// Scan all files in the project and return those whose on-disk path
  /// does not exist. This queries the database for all file/image records
  /// and checks each referenced path.
  auto DetectMissingFiles() -> std::vector<MissingFileInfo>;

  /// Relink a single file by providing its new on-disk path.
  /// This updates the Image table's image_path and preserves all edit
  /// history and adjustments (which are keyed by file_id/image_id).
  auto RelinkFile(sl_element_id_t file_id,
                  const std::filesystem::path& new_path) -> RelinkResult;

  /// Batch-relink multiple files at once.
  /// The map keys are file_ids, values are new paths.
  auto RelinkFiles(const std::unordered_map<sl_element_id_t, std::filesystem::path>& remappings,
                   RelinkProgressCallback progress_cb = nullptr)
      -> std::vector<RelinkResult>;

  /// Search a directory for files matching the given missing file names.
  /// Returns candidates scored by name match, file size, and modification time.
  auto SearchDirectoryForMatches(
      const std::filesystem::path& search_dir,
      const std::vector<MissingFileInfo>& missing_files,
      int max_depth = 3) -> std::unordered_map<sl_element_id_t, std::vector<RelinkCandidate>>;

  /// Auto-search nearby directories (siblings of the original directory)
  /// for matching filenames. Useful when an entire folder was moved.
  auto AutoSearchNearby(
      const std::vector<MissingFileInfo>& missing_files)
      -> std::unordered_map<sl_element_id_t, std::vector<RelinkCandidate>>;

  /// Fuzzy-match a single file by looking for files with similar:
  /// - filename (case-insensitive substring)
  /// - file size (within 5%)
  /// - modification date (within 24 hours)
  auto FindFuzzyCandidates(
      const MissingFileInfo& missing,
      const std::filesystem::path& search_dir,
      int max_depth = 3) -> std::vector<RelinkCandidate>;

 private:
  StorageService& storage_service_;

  /// Score a candidate file against a missing file reference.
  auto ScoreCandidate(const MissingFileInfo& missing,
                      const std::filesystem::path& candidate_path) -> RelinkCandidate;

  /// Recursively collect files from a directory up to max_depth.
  void CollectFiles(const std::filesystem::path& dir,
                    int current_depth,
                    int max_depth,
                    std::vector<std::filesystem::path>& out);

  /// Update the image_path in the Image table for the given image_id.
  auto UpdateImagePath(image_id_t image_id,
                       const std::filesystem::path& new_path) -> bool;

  /// Mark a file as missing in the Element table.
  auto SetFileMissingFlag(sl_element_id_t file_id, bool missing) -> bool;
};

}  // namespace alcedo
