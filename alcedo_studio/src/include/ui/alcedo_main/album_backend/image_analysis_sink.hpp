//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>

#include "app/image_analysis_service.hpp"  // alcedo::ImageAnalysisItemResult

namespace alcedo::ui {

/// Phase 7a — the host-state mutation seam for `ImageAnalysisController`.
///
/// The controller stays decoupled from `AlbumBackend` (Phase 6d invariant) and
/// `ImageAnalysisService` stays storage-agnostic (Phase 5d/6d tests unchanged). This
/// narrow interface, injected into the controller's constructor, owns every host-side
/// side effect of a finished remote-analysis job: persisting understanding / rating
/// reasons, writing the EXIF star, flushing the batched star writes, and refreshing the
/// album search view. Tests pass a fake that records calls so "no upsert on failure /
/// cancel" is a one-liner assertion.
///
/// Persistence fires at job end (in the finished callback), not per item: for each
/// `kAnalyzed` item the controller calls `PersistUnderstanding` (describe) or
/// `PersistRatingReasons` + `ApplyStarRating` (score); `kError`/`kCanceled` items are
/// skipped entirely. After the loop, a score job calls `FlushPendingStarRatings` and a
/// describe job calls `NotifySearchDocumentChanged`. A cancelled or failed call therefore
/// produces ZERO sink calls — no active annotation can be left behind.
class IImageAnalysisSink {
 public:
  virtual ~IImageAnalysisSink() = default;

  /// Describe: persist the understanding (caption/tags/scene/confidence + provider/model/
  /// prompt-profile/rendition identity) as an active-for-search row. Returns false if the
  /// storage layer rejected the row (e.g. orphan file_id); the controller does not treat
  /// this as a job failure, it only surfaces results in QML state.
  virtual bool PersistUnderstanding(const ImageAnalysisItemResult& result) = 0;

  /// Score: persist the rating *reasons* only (rationale + identity), with `rating = 0`
  /// as a sentinel. Does NOT write the EXIF star — that is `ApplyStarRating`'s job.
  virtual bool PersistRatingReasons(const ImageAnalysisItemResult& result) = 0;

  /// Score: write the model's 1..5 value into the EXIF/metadata `Rating` column in-memory
  /// (`Write_NoSync` + view-state patch + thumbnail-model update) — the light half of the
  /// star-rating path. No `SyncWithStorage`/`SaveProject`/`Package` here; the batched
  /// flush is `FlushPendingStarRatings`.
  virtual bool ApplyStarRating(uint32_t elementId, uint32_t imageId, int rating) = 0;

  /// Score job end: one `SyncWithStorage` (flushes all MODIFIED image rows in a single
  /// transaction) + `RefreshStats` (re-runs the rating-bucket GROUP BY so star-filter
  /// stats are correct). No `SaveProject`/`Package` in 7a — the `.alcd` packaged snapshot
  /// is left stale until the next normal save/close; the live DB is authoritative.
  virtual void FlushPendingStarRatings() = 0;

  /// Describe job end: re-run the active search so newly-persisted captions/tags match.
  /// `AiUnderstandingExpr` is a live correlated subquery against `AiImageUnderstanding`,
  /// so active rows match immediately — there is no materialized index to rebuild; this
  /// just re-runs the thumbnail view against the current search/filter.
  virtual void NotifySearchDocumentChanged() = 0;
};

}  // namespace alcedo::ui
