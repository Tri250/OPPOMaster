//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QString>
#include <QVariantList>
#include <QVariantMap>
#include <filesystem>
#include <vector>

#include "type/type.hpp"

namespace alcedo::ui {

class AlbumBackend;

/// Handles single/batch image deletion and related project data cleanup.
class ImageController {
 public:
  struct DeleteTarget {
    sl_element_id_t       element_id_ = 0;
    image_id_t            image_id_   = 0;
    sl_element_id_t       folder_id_  = 0;
    std::filesystem::path file_path_{};
  };

  struct DeleteExecutionResult {
    bool                         success_       = false;
    int                          deleted_count_ = 0;
    int                          failed_count_  = 0;
    std::vector<sl_element_id_t> deleted_element_ids_{};
    std::vector<sl_element_id_t> failed_element_ids_{};
    QString                      message_{};
  };

  explicit ImageController(AlbumBackend& backend);

  auto DeleteImages(const QVariantList& targetEntries) -> QVariantMap;
  auto AddImagesToFolder(const QVariantList& targetEntries, uint targetFolderId) -> QVariantMap;
  auto DeleteTargets(const std::vector<DeleteTarget>& targets) -> DeleteExecutionResult;
  auto GetImageDetails(uint elementId, uint imageId) -> QVariantMap;
  auto GetImageRating(uint elementId, uint imageId) -> QVariantMap;
  auto SetImageRating(uint elementId, uint imageId, int rating) -> QVariantMap;

  // Phase 7a: the light half of the star-rating path, extracted from `SetImageRating`.
  // Writes the 1..5 value into the in-memory EXIF/metadata `Rating` column via
  // `Write_NoSync` (sets MODIFIED, no DB flush), patches the album view-state item, and
  // emits the thumbnail-model `Rating` dataChanged. NO `SyncWithStorage`/`SaveProject`/
  // `Package`/`RefreshStats` — those are `FlushPendingStarRatings`, called once at batch
  // end by the AI image-analysis sink so a batch AI scoring run does one DB flush, not
  // one per image. `SetImageRating` (manual single star click) is unchanged and still
  // does a full sync+save per one-off user action.
  void ApplyStarRatingLight(uint elementId, uint imageId, int rating);
  // Phase 7a: the batched flush half — `SyncWithStorage` (one transaction for all
  // MODIFIED image rows) + `RefreshStats` (re-run the rating-bucket GROUP BY so
  // star-filter stats are correct). No `SaveProject`/`Package` (the `.alcd` packaged
  // snapshot is left stale until the next normal save/close; the live DB is
  // authoritative).
  void FlushPendingStarRatings();

 private:
  struct RatingTarget {
    sl_element_id_t element_id_ = 0;
    image_id_t      image_id_   = 0;
  };

  [[nodiscard]] auto CollectDeleteTargets(const QVariantList& targetEntries) const
      -> std::vector<DeleteTarget>;
  [[nodiscard]] auto ResolveRatingTarget(uint elementId, uint imageId) const -> RatingTarget;

  AlbumBackend&      backend_;
};

}  // namespace alcedo::ui
