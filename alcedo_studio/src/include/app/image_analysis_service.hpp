//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <atomic>
#include <chrono>
#include <condition_variable>
#include <cstddef>
#include <cstdint>
#include <filesystem>
#include <functional>
#include <memory>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#include "app/ai_credential_store.hpp"
#include "app/ai_sidecar_runtime_service.hpp"
#include "app/thumbnail_service.hpp"
#include "app/thumbnail_types.hpp"
#include "sidecar_client/dto/image_analysis.hpp"
#include "type/type.hpp"

namespace alcedo {

struct ImageAnalysisItem {
  sl_element_id_t element_id = 0;
  image_id_t      image_id   = 0;
};

enum class ImageAnalysisTask : uint8_t {
  kDescribe = 0,
  kScore,
};

enum class ImageAnalysisItemStatus : uint8_t {
  kPending = 0,
  kThumbnailReady,
  kAnalyzed,
  kCanceled,
  kError,
};

// Long-lived provider credential for one analysis run. The `secret` is consumed once
// (registered with the sidecar vault to obtain an opaque handle) and then cleared from
// the options copy inside RunJob; it never enters ImageAnalysisRequest, result DTOs,
// AiSidecarRuntimeOptions, process args, or logs.
struct ImageAnalysisCredential {
  std::string provider_id;
  std::string secret;
};

// Phase 5e prefill queue depth upper bound. The plan calls for "initially 1 or 2"; this
// cap keeps a caller from requesting an unbounded prefill depth, which would let the
// producer encode most of a large album while one RPC is blocked and weaken the
// bounded-memory promise. RunJob clamps `ImageAnalysisOptions::prefetch` to
// [1, kMaxImageAnalysisPrefetch]. Each queued entry is one encoded k1024 JPEG buffer, so
// raise this only with a matching memory-budget review.
inline constexpr int kMaxImageAnalysisPrefetch = 4;

struct ImageAnalysisOptions {
  ImageAnalysisTask       task               = ImageAnalysisTask::kDescribe;
  ThumbnailResolution     thumbnail_resolution = ThumbnailResolution::k1024;
  int                     jpeg_quality       = 90;
  std::chrono::milliseconds timeout          {60000};
  std::string             provider_id;        // "" = sidecar default
  std::string             model_id;           // "" = provider default
  std::string             prompt_profile_id;
  std::string             rubric_id;          // ScoreImage only; "" = provider default
  ImageAnalysisCredential credential;
  std::filesystem::path   temp_dir;           // empty => std::filesystem::temp_directory_path()
  int64_t                 credential_ttl_ms = 0;  // 0 => sidecar default
  // Phase 5e prefill queue depth: the maximum number of encoded JPEG renditions
  // buffered ahead of the single in-flight remote call. The gate still caps remote
  // concurrency at one; this only overlaps local thumbnail/encode prep with the
  // active provider call. Clamped to [1, kMaxImageAnalysisPrefetch] in RunJob. A
  // large album cannot accumulate more than `prefetch` encoded byte buffers in
  // memory (plus the one in flight).
  int                     prefetch           = 1;
  // Phase 6d: optional cap on the encoded-rendition byte size, sourced from the
  // selected preset's `max_image_bytes`. 0 = no cap. When > 0, RunJob marks an
  // item as a prep failure (no provider call, no pin held) if the encoded JPEG
  // exceeds this limit — fail-closed so a preset cannot push an oversized payload
  // to a paid provider call.
  int64_t                 max_image_bytes    = 0;
};

struct ImageAnalysisConnectionValidationOptions {
  std::string               provider_id;
  std::string               credential_slot;
  std::chrono::milliseconds timeout{60000};
  int64_t                   credential_ttl_ms = 60000;
};

struct ImageAnalysisConnectionValidationResult {
  bool                            ok = false;
  std::string                     error;
  bool                            credential_revoked = false;
  ImageAnalysisListModelsResult   list_models;
  std::vector<AiDiscoveredModel>  models;
};

struct ImageAnalysisProgress {
  size_t total    = 0;
  size_t analyzed = 0;
  size_t failed   = 0;
  size_t canceled = 0;
};

struct ImageAnalysisItemResult {
  ImageAnalysisItem               item{};
  std::string                     request_id;
  ImageAnalysisItemStatus         status = ImageAnalysisItemStatus::kPending;
  std::string                     error;
  ImageAnalysisUnderstandingResult understanding;  // filled when task == kDescribe
  ImageAnalysisRatingResult       rating;          // filled when task == kScore
  ImageAnalysisRendition          rendition;       // the rendition actually analyzed
};

using ImageAnalysisProgressCallback = std::function<void(const ImageAnalysisProgress&)>;
using ImageAnalysisFinishedCallback = std::function<void(std::vector<ImageAnalysisItemResult>)>;
using ImageAnalysisThumbnailCallback = std::function<void(ThumbnailRequestResult)>;

// Serializes remote image-analysis calls to at most one in flight across ALL
// ImageAnalysisService instances that share the same gate. Phase 5d mandates a
// host-boundary in-flight limit of one (provider calls are non-idempotent / paid).
// Injectable so the album backend (Phase 6) can share one gate app-wide even if the
// service is constructed per-use; if none is passed the service creates a private one.
class ImageAnalysisInFlightGate {
 public:
  ImageAnalysisInFlightGate() = default;

  // Blocks until the slot is free or `is_canceled()` returns true. On success the slot
  // is acquired AND `request_id` is published under the same lock, so an observer can
  // never see the slot held with no published id. Returns true if the slot was acquired
  // (id published), false if the wait was canceled (the slot is NOT acquired, id NOT
  // published). Atomic acquire+publish closes the cancel race where ImageAnalysisJob::
  // Cancel could otherwise observe a held slot with an empty id (skip CancelTask) while
  // the worker was about to issue the paid provider RPC.
  auto AcquireAndPublish(const std::string& request_id, std::function<bool()> is_canceled)
      -> bool;
  void Release();
  // Clears the request_id of the job currently occupying the slot. Paired with
  // AcquireAndPublish; read by ImageAnalysisJob::Cancel to decide whether to best-effort
  // CancelTask this job's in-flight RPC. While the slot is held the id is always
  // non-empty (AcquireAndPublish publishes atomically), so Cancel's id check agrees with
  // its am_in_flight_ check.
  void ClearRequestId();
  auto CurrentRequestId() const -> std::string;
  // Wakes any waiter blocked in AcquireAndPublish (called by ImageAnalysisJob::Cancel).
  void NotifyAll();

 private:
  mutable std::mutex          mutex_;
  std::condition_variable     cv_;
  bool                        in_flight_ = false;
  std::string                 in_flight_request_id_;
};

class IImageAnalysisThumbnailProvider {
 public:
  virtual ~IImageAnalysisThumbnailProvider() = default;

  virtual void RequestThumbnail(const ImageAnalysisItem& item, ThumbnailResolution resolution,
                                ImageAnalysisThumbnailCallback callback) = 0;
  virtual void CancelThumbnail(const ThumbnailCacheKey& key)              = 0;
  virtual void ReleaseThumbnail(const ThumbnailCacheKey& key)             = 0;
};

class ThumbnailServiceImageAnalysisProvider final : public IImageAnalysisThumbnailProvider {
 public:
  explicit ThumbnailServiceImageAnalysisProvider(std::shared_ptr<ThumbnailService> service);

  void RequestThumbnail(const ImageAnalysisItem& item, ThumbnailResolution resolution,
                        ImageAnalysisThumbnailCallback callback) override;
  void CancelThumbnail(const ThumbnailCacheKey& key) override;
  void ReleaseThumbnail(const ThumbnailCacheKey& key) override;

 private:
  std::shared_ptr<ThumbnailService> service_;
};

// Sidecar-call seam for ImageAnalysisService (mirrors ISemanticImageEmbeddingClient).
// Adding Ready / RegisterCredential / CancelTask beyond the typed RPCs keeps the
// service's credential + server-cancel concerns behind one testable interface.
class IImageAnalysisClient {
 public:
  virtual ~IImageAnalysisClient() = default;

  virtual auto Ready() -> bool = 0;
  virtual auto RegisterCredential(const std::string& provider_id, const std::string& secret,
                                  int64_t ttl_ms, std::chrono::milliseconds timeout,
                                  std::string* handle, std::string* error) -> bool = 0;
  virtual auto RevokeCredential(const std::string& handle, std::chrono::milliseconds timeout,
                                bool* revoked, std::string* error) -> bool = 0;
  virtual auto DescribeImage(const ImageAnalysisRequest& request, std::chrono::milliseconds timeout)
      -> ImageAnalysisUnderstandingResult = 0;
  virtual auto ScoreImage(const ImageAnalysisRequest& request, std::chrono::milliseconds timeout)
      -> ImageAnalysisRatingResult = 0;
  // Phase 6c: dry-run model discovery (validate-connection flow). `provider_id`
  // selects the configured endpoint ("" = sidecar default); `credential_ref` is
  // the opaque vault handle. Returns unverified candidates; no persistence.
  virtual auto ListModels(const std::string& provider_id, const std::string& credential_ref,
                          std::chrono::milliseconds timeout) -> ImageAnalysisListModelsResult = 0;
  virtual auto CancelTask(const std::string& request_id, std::chrono::milliseconds timeout,
                          bool* cancelled, std::string* error) -> bool = 0;
};

class AiSidecarRuntimeImageAnalysisClient final : public IImageAnalysisClient {
 public:
  explicit AiSidecarRuntimeImageAnalysisClient(std::shared_ptr<AiSidecarRuntimeService> runtime);

  auto Ready() -> bool override;
  auto RegisterCredential(const std::string& provider_id, const std::string& secret,
                          int64_t ttl_ms, std::chrono::milliseconds timeout, std::string* handle,
                          std::string* error) -> bool override;
  auto RevokeCredential(const std::string& handle, std::chrono::milliseconds timeout,
                        bool* revoked, std::string* error) -> bool override;
  auto DescribeImage(const ImageAnalysisRequest& request, std::chrono::milliseconds timeout)
      -> ImageAnalysisUnderstandingResult override;
  auto ScoreImage(const ImageAnalysisRequest& request, std::chrono::milliseconds timeout)
      -> ImageAnalysisRatingResult override;
  auto ListModels(const std::string& provider_id, const std::string& credential_ref,
                  std::chrono::milliseconds timeout) -> ImageAnalysisListModelsResult override;
  auto CancelTask(const std::string& request_id, std::chrono::milliseconds timeout,
                  bool* cancelled, std::string* error) -> bool override;

 private:
  std::shared_ptr<AiSidecarRuntimeService> runtime_;
};

class ImageAnalysisJob final {
 public:
  ImageAnalysisJob() = default;
  ~ImageAnalysisJob();

  ImageAnalysisJob(const ImageAnalysisJob&)            = delete;
  ImageAnalysisJob& operator=(const ImageAnalysisJob&) = delete;

  // Sets the cooperative cancel flag, wakes any queued wait on the in-flight gate, and
  // best-effort calls CancelTask on this job's in-flight RPC (if any). The correctness
  // guarantee is the post-RPC discard in RunJob, not CancelTask: a long provider call
  // may still complete and its result is dropped.
  void Cancel();
  auto IsCanceled() const -> bool;
  void Wait();
  auto SnapshotProgress() const -> ImageAnalysisProgress;
  auto Results() const -> std::vector<ImageAnalysisItemResult>;

 private:
  friend class ImageAnalysisService;

  void UpdateProgress(const std::function<void(ImageAnalysisProgress&)>& updater);
  void AppendResult(ImageAnalysisItemResult result);
  void SetWorkerThread(std::thread worker);
  void Finish();
  void SetGate(std::shared_ptr<ImageAnalysisInFlightGate> gate);
  void SetClient(std::shared_ptr<IImageAnalysisClient> client);

  mutable std::mutex                        lock_;
  std::condition_variable                   finished_cv_;
  ImageAnalysisProgress                     progress_{};
  std::vector<ImageAnalysisItemResult>      results_;
  std::atomic<bool>                         canceled_{false};
  std::atomic<bool>                         am_in_flight_{false};
  std::thread                               worker_;
  std::thread                               producer_;  // Phase 5e prefill producer
  bool                                      finished_ = false;

  std::shared_ptr<ImageAnalysisInFlightGate> gate_;
  std::shared_ptr<IImageAnalysisClient>      client_;
};

class ImageAnalysisService final {
 public:
  ImageAnalysisService(std::shared_ptr<IImageAnalysisThumbnailProvider> thumbnail_provider,
                       std::shared_ptr<IImageAnalysisClient>            analysis_client,
                       std::shared_ptr<ImageAnalysisInFlightGate>       in_flight_gate = nullptr);

  auto StartAnalysis(std::vector<ImageAnalysisItem> items, ImageAnalysisOptions options = {},
                     ImageAnalysisProgressCallback  on_progress = {},
                     ImageAnalysisFinishedCallback  on_finished = {})
      -> std::shared_ptr<ImageAnalysisJob>;
  auto ValidateConnection(const ImageAnalysisConnectionValidationOptions& options,
                          IAiCredentialStore& credential_store)
      -> ImageAnalysisConnectionValidationResult;

 private:
  static void RunJob(const std::shared_ptr<ImageAnalysisJob>&             job,
                     const std::vector<ImageAnalysisItem>&                items,
                     ImageAnalysisOptions                                 options,
                     ImageAnalysisProgressCallback                        on_progress,
                     ImageAnalysisFinishedCallback                        on_finished,
                     std::shared_ptr<IImageAnalysisThumbnailProvider>     thumbnail_provider,
                     std::shared_ptr<IImageAnalysisClient>                analysis_client,
                     std::shared_ptr<ImageAnalysisInFlightGate>           in_flight_gate);

  std::shared_ptr<IImageAnalysisThumbnailProvider> thumbnail_provider_;
  std::shared_ptr<IImageAnalysisClient>            analysis_client_;
  std::shared_ptr<ImageAnalysisInFlightGate>       in_flight_gate_;
};

auto ToString(ImageAnalysisItemStatus status) -> const char*;

}  // namespace alcedo
