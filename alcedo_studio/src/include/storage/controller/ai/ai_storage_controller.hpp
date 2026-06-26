//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <duckdb.h>

#include <optional>
#include <span>
#include <string>

#include "ai/ai_description.hpp"
#include "ai/ai_rating.hpp"
#include "storage/controller/db_controller.hpp"
#include "type/type.hpp"

namespace alcedo {

// Persists the remote image-analysis sidecar's results for a file (Phase 5f):
// `AiDescription` (the searchable understanding — caption, tags, scene, confidence) and
// `AiRating` (the 1..5 integer rating, kept out of full-text search). Both bind to
// `file_id` (the Sleeve element id / inode), the same key the CLIP embeddings bind to, so
// deleting a file cascades cleanly and a re-import under a new image id recovers prior
// annotations.
//
// All serialization/deserialization goes through the duckorm layer (`insert_or_replace`
// and `select`); no raw INSERT/SELECT is written here. The `(file_id, task_id)` primary
// key makes `insert_or_replace` enforce "at most one row per pair" — hence at most one
// active-for-search understanding per (file_id, task_id); a re-run for the same pair
// replaces the prior row in place. `prompt_profile_id` (understanding) and
// `rubric_id` / `rubric_version` (rating) are stored per row so a prompt/profile or
// rubric change is never silently reinterpreted as the old row's score.
class AiStorageController {
 private:
  DBController& db_ctrl_;

 public:
  explicit AiStorageController(DBController& db_ctrl);

  // Persist a successful image-understanding result. `insert_or_replace` on the table's
  // PRIMARY KEY (file_id, task_id) replaces the prior row for that pair, so there is at
  // most one row — hence at most one active-for-search understanding — per
  // (file_id, task_id). Returns false and writes nothing when `IsValid()` is false, so a
  // partial/failed remote call leaves no active search document (the primary guard is the
  // caller only reaching here on a complete successful describe; IsValid is the
  // storage-layer backstop). Throws on a DuckDB error.
  [[nodiscard]] auto UpsertUnderstanding(const AiDescription& description) const -> bool;

  // Read the row for an exact (file_id, task_id) pair — a deterministic primary-key
  // lookup. Returns std::nullopt when no such row exists.
  [[nodiscard]] auto GetUnderstanding(sl_element_id_t      file_id,
                                      const std::string& task_id) const
      -> std::optional<AiDescription>;

  // Read the active-for-search understanding for a file (the first active row). The host
  // uses a single task_id slot per file, so this is the row `UpsertUnderstanding` most
  // recently wrote. Returns std::nullopt when no active row exists.
  [[nodiscard]] auto GetActiveUnderstanding(sl_element_id_t file_id) const
      -> std::optional<AiDescription>;

  // Persist a successful image-rating result (1..5 integer). Same upsert/identity
  // contract as `UpsertUnderstanding`; rejected when `IsValid()` is false (a rating of 0
  // is "unset" and never persisted, so a scored image is never confused with an unrated
  // one). Throws on a DuckDB error.
  [[nodiscard]] auto UpsertRating(const AiRating& rating) const -> bool;

  [[nodiscard]] auto GetRating(sl_element_id_t file_id, const std::string& task_id) const
      -> std::optional<AiRating>;
  [[nodiscard]] auto GetActiveRating(sl_element_id_t file_id) const -> std::optional<AiRating>;

  // Drop all AI annotation rows for the given files using the controller's own
  // connection. The file-deletion cascade (element_controller) calls the free function
  // below on its own connection so the cleanup is atomic with element deletion; this
  // convenience overload is for non-cascade callers and tests. A no-op for an empty list.
  void DeleteForFiles(std::span<const sl_element_id_t> file_ids) const;
};

// Delete every `AiImageUnderstanding` and `AiImageRating` row for the given files on the
// supplied connection, via the duckorm `remove` path (this function does not write raw
// DELETE statements — only the `file_id IN (...)` predicate). The element-deletion
// cascade passes its own connection so the AI row cleanup shares the caller's
// transaction. A no-op for an empty file list.
void DeleteAiAnnotationRowsForFiles(duckdb_connection               conn,
                                    std::span<const sl_element_id_t> file_ids);

}  // namespace alcedo