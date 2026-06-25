//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/image_analysis_service.hpp"

#include "app/image_analysis_encoder.hpp"

#include <algorithm>
#include <exception>
#include <filesystem>
#include <stdexcept>
#include <utility>

#include "utils/diagnostics/app_logging.hpp"

namespace alcedo {
namespace {

// Generic RAII scope-exit: invokes `fn` on destruction. Used to guarantee the
// in-flight gate is released (and am_in_flight_ / the published request_id are
// cleared) even if a provider RPC throws between Acquire and Release.
template <typename F>
class ScopeExit {
 public:
  explicit ScopeExit(F fn) : fn_(std::move(fn)) {}
  ~ScopeExit() {
    if (engaged_) {
      fn_();
    }
  }
  ScopeExit(const ScopeExit&)            = delete;
  ScopeExit& operator=(const ScopeExit&) = delete;

 private:
  F    fn_;
  bool engaged_ = true;
};
template <typename F>
ScopeExit(F) -> ScopeExit<F>;

auto MakeRequestId(const ImageAnalysisItem& item, ImageAnalysisTask task) -> std::string {
  return std::string("image-analysis-") +
         (task == ImageAnalysisTask::kScore ? "score-" : "describe-") +
         std::to_string(item.element_id) + "-" + std::to_string(item.image_id);
}

struct ThumbnailWaitState {
  std::mutex             mutex;
  std::condition_variable cv;
  ThumbnailRequestResult result;
  bool                   done      = false;
  bool                   abandoned = false;
};

// Requests one thumbnail and blocks (cooperatively, 25ms poll) until it returns or the
// job is canceled. Mirrors WaitForThumbnailBatch in semantic_generation_service.cpp but
// for a single item, since image analysis is serialized one image at a time.
auto WaitForOneThumbnail(const std::shared_ptr<ImageAnalysisJob>&             job,
                         const std::shared_ptr<IImageAnalysisThumbnailProvider>& provider,
                         const ImageAnalysisItem& item, ThumbnailResolution resolution)
    -> ThumbnailRequestResult {
  auto state = std::make_shared<ThumbnailWaitState>();

  try {
    provider->RequestThumbnail(item, resolution,
                               [state, provider](ThumbnailRequestResult result) {
                                 bool               release_late = false;
                                 ThumbnailCacheKey  late_key{};
                                 {
                                   std::unique_lock lk(state->mutex);
                                   release_late = state->abandoned && result.guard != nullptr;
                                   late_key     = result.key;
                                   if (!state->abandoned && !state->done) {
                                     state->result = std::move(result);
                                     state->done   = true;
                                   }
                                 }
                                 if (release_late) {
                                   provider->ReleaseThumbnail(late_key);
                                 }
                                 state->cv.notify_all();
                               });
  } catch (const std::exception& e) {
    ThumbnailRequestResult r;
    r.key     = ThumbnailCacheKey{item.element_id, resolution};
    r.status  = ThumbnailRequestStatus::kError;
    r.message = std::string("thumbnail request failed: ") + e.what();
    return r;
  } catch (...) {
    ThumbnailRequestResult r;
    r.key     = ThumbnailCacheKey{item.element_id, resolution};
    r.status  = ThumbnailRequestStatus::kError;
    r.message = "thumbnail request failed";
    return r;
  }

  {
    std::unique_lock lk(state->mutex);
    while (!state->done) {
      if (job->IsCanceled()) {
        state->abandoned = true;
        lk.unlock();
        provider->CancelThumbnail(ThumbnailCacheKey{item.element_id, resolution});
        ThumbnailRequestResult r;
        r.key     = ThumbnailCacheKey{item.element_id, resolution};
        r.status  = ThumbnailRequestStatus::kCanceled;
        r.message = "image analysis job was canceled";
        return r;
      }
      state->cv.wait_for(lk, std::chrono::milliseconds(25));
    }
  }
  return std::move(state->result);
}

}  // namespace

auto ToString(ImageAnalysisItemStatus status) -> const char* {
  switch (status) {
    case ImageAnalysisItemStatus::kPending:
      return "pending";
    case ImageAnalysisItemStatus::kThumbnailReady:
      return "thumbnail_ready";
    case ImageAnalysisItemStatus::kAnalyzed:
      return "analyzed";
    case ImageAnalysisItemStatus::kCanceled:
      return "canceled";
    case ImageAnalysisItemStatus::kError:
      return "error";
  }
  return "unknown";
}

// --- ImageAnalysisInFlightGate ---

auto ImageAnalysisInFlightGate::Acquire(std::function<bool()> is_canceled) -> bool {
  std::unique_lock lk(mutex_);
  cv_.wait(lk, [&] { return !in_flight_ || is_canceled(); });
  if (is_canceled()) {
    return false;
  }
  in_flight_ = true;
  return true;
}

void ImageAnalysisInFlightGate::Release() {
  {
    std::unique_lock lk(mutex_);
    in_flight_ = false;
  }
  cv_.notify_all();
}

void ImageAnalysisInFlightGate::PublishRequestId(const std::string& id) {
  std::unique_lock lk(mutex_);
  in_flight_request_id_ = id;
}

void ImageAnalysisInFlightGate::ClearRequestId() {
  std::unique_lock lk(mutex_);
  in_flight_request_id_.clear();
}

auto ImageAnalysisInFlightGate::CurrentRequestId() const -> std::string {
  std::unique_lock lk(mutex_);
  return in_flight_request_id_;
}

void ImageAnalysisInFlightGate::NotifyAll() { cv_.notify_all(); }

// --- ThumbnailServiceImageAnalysisProvider ---

ThumbnailServiceImageAnalysisProvider::ThumbnailServiceImageAnalysisProvider(
    std::shared_ptr<ThumbnailService> service)
    : service_(std::move(service)) {}

void ThumbnailServiceImageAnalysisProvider::RequestThumbnail(const ImageAnalysisItem& item,
                                                             ThumbnailResolution      resolution,
                                                             ImageAnalysisThumbnailCallback callback) {
  if (!service_) {
    ThumbnailRequestResult r;
    r.key     = ThumbnailCacheKey{item.element_id, resolution};
    r.status  = ThumbnailRequestStatus::kError;
    r.message = "ThumbnailService is not available";
    callback(std::move(r));
    return;
  }
  service_->GetThumbnailDetailed(item.element_id, item.image_id, std::move(callback), true, nullptr,
                                 resolution);
}

void ThumbnailServiceImageAnalysisProvider::CancelThumbnail(const ThumbnailCacheKey& key) {
  if (service_) {
    service_->CancelPending(key);
  }
}

void ThumbnailServiceImageAnalysisProvider::ReleaseThumbnail(const ThumbnailCacheKey& key) {
  if (service_) {
    service_->ReleaseThumbnail(key);
  }
}

// --- AiSidecarRuntimeImageAnalysisClient ---

AiSidecarRuntimeImageAnalysisClient::AiSidecarRuntimeImageAnalysisClient(
    std::shared_ptr<AiSidecarRuntimeService> runtime)
    : runtime_(std::move(runtime)) {}

auto AiSidecarRuntimeImageAnalysisClient::Ready() -> bool {
  return runtime_ && runtime_->IsRunning();
}

auto AiSidecarRuntimeImageAnalysisClient::RegisterCredential(
    const std::string& provider_id, const std::string& secret, int64_t ttl_ms,
    std::chrono::milliseconds timeout, std::string* handle, std::string* error) -> bool {
  if (!runtime_) {
    if (error) *error = "ai sidecar runtime is not available";
    return false;
  }
  return runtime_->RegisterCredential(provider_id, secret, ttl_ms, timeout, handle, error);
}

auto AiSidecarRuntimeImageAnalysisClient::DescribeImage(const ImageAnalysisRequest& request,
                                                        std::chrono::milliseconds timeout)
    -> ImageAnalysisUnderstandingResult {
  if (!runtime_) {
    ImageAnalysisUnderstandingResult r;
    r.request_id = request.request_id;
    r.ok         = false;
    r.error      = "ai sidecar runtime is not available";
    return r;
  }
  return runtime_->DescribeImage(request, timeout);
}

auto AiSidecarRuntimeImageAnalysisClient::ScoreImage(const ImageAnalysisRequest& request,
                                                     std::chrono::milliseconds timeout)
    -> ImageAnalysisRatingResult {
  if (!runtime_) {
    ImageAnalysisRatingResult r;
    r.request_id = request.request_id;
    r.ok         = false;
    r.error      = "ai sidecar runtime is not available";
    return r;
  }
  return runtime_->ScoreImage(request, timeout);
}

auto AiSidecarRuntimeImageAnalysisClient::CancelTask(const std::string& request_id,
                                                     std::chrono::milliseconds timeout,
                                                     bool* cancelled, std::string* error) -> bool {
  if (!runtime_) {
    if (error) *error = "ai sidecar runtime is not available";
    return false;
  }
  return runtime_->CancelTask(request_id, timeout, cancelled, error);
}

// --- ImageAnalysisJob ---

ImageAnalysisJob::~ImageAnalysisJob() {
  Cancel();
  if (worker_.joinable()) {
    if (worker_.get_id() == std::this_thread::get_id()) {
      worker_.detach();
    } else {
      worker_.join();
    }
  }
}

void ImageAnalysisJob::Cancel() {
  canceled_.store(true);
  if (gate_) {
    gate_->NotifyAll();
  }
  // Best-effort server-side cancel of THIS job's in-flight RPC. am_in_flight_ is true
  // only while this job occupies the gate slot, so the gate's current id (when non-empty)
  // is this job's request_id. CancelTask returning cancelled=false (already finished /
  // unknown) is harmless; the post-RPC IsCanceled() discard in RunJob is the guarantee.
  if (am_in_flight_.load() && client_ && gate_) {
    const auto id = gate_->CurrentRequestId();
    if (!id.empty()) {
      bool cancelled = false;
      client_->CancelTask(id, std::chrono::milliseconds(2000), &cancelled, nullptr);
    }
  }
}

auto ImageAnalysisJob::IsCanceled() const -> bool { return canceled_.load(); }

void ImageAnalysisJob::Wait() {
  {
    std::unique_lock lk(lock_);
    finished_cv_.wait(lk, [this]() { return finished_; });
  }
  if (worker_.joinable() && worker_.get_id() != std::this_thread::get_id()) {
    worker_.join();
  }
}

auto ImageAnalysisJob::SnapshotProgress() const -> ImageAnalysisProgress {
  std::unique_lock lk(lock_);
  return progress_;
}

auto ImageAnalysisJob::Results() const -> std::vector<ImageAnalysisItemResult> {
  std::unique_lock lk(lock_);
  return results_;
}

void ImageAnalysisJob::UpdateProgress(
    const std::function<void(ImageAnalysisProgress&)>& updater) {
  std::unique_lock lk(lock_);
  updater(progress_);
}

void ImageAnalysisJob::AppendResult(ImageAnalysisItemResult result) {
  std::unique_lock lk(lock_);
  results_.push_back(std::move(result));
}

void ImageAnalysisJob::SetWorkerThread(std::thread worker) { worker_ = std::move(worker); }

void ImageAnalysisJob::Finish() {
  {
    std::unique_lock lk(lock_);
    finished_ = true;
  }
  finished_cv_.notify_all();
}

void ImageAnalysisJob::SetGate(std::shared_ptr<ImageAnalysisInFlightGate> gate) {
  gate_ = std::move(gate);
}

void ImageAnalysisJob::SetClient(std::shared_ptr<IImageAnalysisClient> client) {
  client_ = std::move(client);
}

// --- ImageAnalysisService ---

ImageAnalysisService::ImageAnalysisService(
    std::shared_ptr<IImageAnalysisThumbnailProvider> thumbnail_provider,
    std::shared_ptr<IImageAnalysisClient>            analysis_client,
    std::shared_ptr<ImageAnalysisInFlightGate>       in_flight_gate)
    : thumbnail_provider_(std::move(thumbnail_provider)),
      analysis_client_(std::move(analysis_client)),
      in_flight_gate_(in_flight_gate ? in_flight_gate
                                     : std::make_shared<ImageAnalysisInFlightGate>()) {
  if (!thumbnail_provider_) {
    throw std::invalid_argument("ImageAnalysisService requires a thumbnail provider");
  }
  if (!analysis_client_) {
    throw std::invalid_argument("ImageAnalysisService requires an analysis client");
  }
}

auto ImageAnalysisService::StartAnalysis(std::vector<ImageAnalysisItem> items,
                                         ImageAnalysisOptions           options,
                                         ImageAnalysisProgressCallback  on_progress,
                                         ImageAnalysisFinishedCallback  on_finished)
    -> std::shared_ptr<ImageAnalysisJob> {
  auto job = std::make_shared<ImageAnalysisJob>();
  job->UpdateProgress(
      [total = items.size()](ImageAnalysisProgress& p) { p.total = total; });
  job->SetGate(in_flight_gate_);
  job->SetClient(analysis_client_);

  auto thumbnail_provider = thumbnail_provider_;
  auto analysis_client    = analysis_client_;
  auto gate               = in_flight_gate_;
  auto worker             = std::thread(
      [job, items = std::move(items), options, on_progress = std::move(on_progress),
       on_finished = std::move(on_finished), thumbnail_provider = std::move(thumbnail_provider),
       analysis_client = std::move(analysis_client), gate = std::move(gate)]() mutable {
        RunJob(job, items, options, std::move(on_progress), std::move(on_finished),
               std::move(thumbnail_provider), std::move(analysis_client), std::move(gate));
      });
  qCInfo(diag::semanticLog).noquote()
      << QStringLiteral("image_analysis.start total=%1 task=%2 provider=%3 model=%4")
             .arg(static_cast<qulonglong>(job->SnapshotProgress().total))
             .arg(options.task == ImageAnalysisTask::kScore ? QStringLiteral("score")
                                                            : QStringLiteral("describe"))
             .arg(QString::fromStdString(options.provider_id))
             .arg(QString::fromStdString(options.model_id));
  job->SetWorkerThread(std::move(worker));
  return job;
}

void ImageAnalysisService::RunJob(const std::shared_ptr<ImageAnalysisJob>& job,
                                  const std::vector<ImageAnalysisItem>&    items,
                                  ImageAnalysisOptions                     options,
                                  ImageAnalysisProgressCallback            on_progress,
                                  ImageAnalysisFinishedCallback            on_finished,
                                  std::shared_ptr<IImageAnalysisThumbnailProvider> thumbnail_provider,
                                  std::shared_ptr<IImageAnalysisClient>            analysis_client,
                                  std::shared_ptr<ImageAnalysisInFlightGate> in_flight_gate) {
  auto dispatch_progress = [&](const ImageAnalysisProgress& p) {
    if (on_progress) {
      on_progress(p);
    }
  };
  auto update_and_dispatch = [&](std::function<void(ImageAnalysisProgress&)> updater) {
    job->UpdateProgress(updater);
    dispatch_progress(job->SnapshotProgress());
  };

  update_and_dispatch([&](ImageAnalysisProgress& p) { p.total = items.size(); });

  // (1) Register the credential once for this run, then drop the secret from the local
  // options copy. The handle (not the secret) threads into every request.
  std::string credential_ref;
  if (!options.credential.secret.empty()) {
    std::string reg_error;
    std::string handle;
    const bool  registered = analysis_client->RegisterCredential(
        options.credential.provider_id, options.credential.secret, options.credential_ttl_ms,
        options.timeout, &handle, &reg_error);
    // Zeroize + clear the secret from the local copy; it must not survive registration.
    auto& secret_field = options.credential.secret;
    if (!secret_field.empty()) {
      std::fill(secret_field.begin(), secret_field.end(), '0');
    }
    secret_field.clear();
    secret_field.shrink_to_fit();
    if (!registered) {
      for (const auto& item : items) {
        ImageAnalysisItemResult r;
        r.item   = item;
        r.status = ImageAnalysisItemStatus::kError;
        r.error  = std::string("credential registration failed: ") + reg_error;
        job->AppendResult(std::move(r));
      }
      update_and_dispatch([&](ImageAnalysisProgress& p) { p.failed += items.size(); });
      if (on_finished) {
        on_finished(job->Results());
      }
      job->Finish();
      return;
    }
    credential_ref = std::move(handle);
  }

  const auto temp_dir   = options.temp_dir.empty()
                              ? std::filesystem::temp_directory_path()
                              : options.temp_dir;
  const auto resolution = options.thumbnail_resolution;

  for (const auto& item : items) {
    if (job->IsCanceled()) {
      ImageAnalysisItemResult r;
      r.item   = item;
      r.status = ImageAnalysisItemStatus::kCanceled;
      job->AppendResult(std::move(r));
      update_and_dispatch([&](ImageAnalysisProgress& p) { p.canceled += 1; });
      continue;
    }

    // (2) Materialize the k1024 thumbnail and encode it to JPEG. This is the encoded
    // remote-analysis path — distinct from the raw RGBA8 CLIP embedding path.
    auto thumb = WaitForOneThumbnail(job, thumbnail_provider, item, resolution);
    if (thumb.status != ThumbnailRequestStatus::kReady || !thumb.guard) {
      ImageAnalysisItemResult r;
      r.item   = item;
      r.status = (thumb.status == ThumbnailRequestStatus::kCanceled)
                     ? ImageAnalysisItemStatus::kCanceled
                     : ImageAnalysisItemStatus::kError;
      r.error  = thumb.message.empty() ? std::string("thumbnail unavailable") : thumb.message;
      update_and_dispatch(
          [&](ImageAnalysisProgress& p) {
            if (r.status == ImageAnalysisItemStatus::kCanceled) {
              p.canceled += 1;
            } else {
              p.failed += 1;
            }
          });
      job->AppendResult(std::move(r));
      continue;
    }

    std::string encode_error;
    auto        encoded = EncodeThumbnailForRemoteAnalysis(
        *thumb.guard, options.jpeg_quality, static_cast<uint32_t>(resolution), temp_dir, &encode_error);
    thumbnail_provider->ReleaseThumbnail(thumb.key);
    if (!encoded.ok) {
      ImageAnalysisItemResult r;
      r.item   = item;
      r.status = ImageAnalysisItemStatus::kError;
      r.error  = encode_error.empty() ? std::string("image encode failed") : encode_error;
      job->AppendResult(std::move(r));
      update_and_dispatch([&](ImageAnalysisProgress& p) { p.failed += 1; });
      continue;
    }

    // (3) Build the typed request. The rendition records what was actually sent.
    const auto byte_count = encoded.bytes.size();
    ImageAnalysisRequest req;
    req.request_id        = MakeRequestId(item, options.task);
    req.image_bytes       = std::move(encoded.bytes);
    req.image_format_hint = encoded.format_hint;
    req.rendition.kind    = encoded.rendition_kind;
    req.rendition.width   = encoded.width;
    req.rendition.height  = encoded.height;
    req.rendition.bytes   = byte_count;
    req.rendition.max_edge = encoded.max_edge;
    req.provider_id       = options.provider_id;
    req.model_id          = options.model_id;
    req.prompt_profile_id = options.prompt_profile_id;
    req.credential_ref    = credential_ref;
    req.rubric_id         = options.rubric_id;

    // (4) Acquire the service-wide in-flight slot (max one remote analysis at a time).
    // If canceled while queued, exit without ever calling the provider.
    if (!in_flight_gate->Acquire([job]() { return job->IsCanceled(); })) {
      ImageAnalysisItemResult r;
      r.item      = item;
      r.request_id = req.request_id;
      r.status    = ImageAnalysisItemStatus::kCanceled;
      r.rendition = req.rendition;
      job->AppendResult(std::move(r));
      update_and_dispatch([&](ImageAnalysisProgress& p) { p.canceled += 1; });
      continue;
    }
    if (job->IsCanceled()) {
      in_flight_gate->Release();
      ImageAnalysisItemResult r;
      r.item      = item;
      r.request_id = req.request_id;
      r.status    = ImageAnalysisItemStatus::kCanceled;
      r.rendition = req.rendition;
      job->AppendResult(std::move(r));
      update_and_dispatch([&](ImageAnalysisProgress& p) { p.canceled += 1; });
      continue;
    }
    job->am_in_flight_.store(true);
    in_flight_gate->PublishRequestId(req.request_id);

    // (5) The typed RPC. DescribeImage / ScoreImage are distinct contracts (distinct
    // task_ids / result types) so a rating result can never overwrite an understanding.
    // The in-flight slot is held across the call. An RAII guard releases it (and clears
    // the published request_id + am_in_flight_) on scope exit — including if the
    // provider throws, which would otherwise leave the service-wide gate locked and the
    // job stuck in-flight (so Finish() would never run). The provider call is wrapped so
    // a thrown RPC becomes an item error instead of escaping the worker thread.
    ImageAnalysisItemResult r;
    r.item      = item;
    r.request_id = req.request_id;
    r.rendition = req.rendition;
    {
      ScopeExit slot_guard([&] {
        job->am_in_flight_.store(false);
        in_flight_gate->ClearRequestId();
        in_flight_gate->Release();
      });
      try {
        if (options.task == ImageAnalysisTask::kScore) {
          auto res   = analysis_client->ScoreImage(req, options.timeout);
          r.request_id = res.request_id.empty() ? req.request_id : res.request_id;
          r.rating   = std::move(res);
        } else {
          auto res   = analysis_client->DescribeImage(req, options.timeout);
          r.request_id = res.request_id.empty() ? req.request_id : res.request_id;
          r.understanding = std::move(res);
        }
      } catch (const std::exception& e) {
        r.status = ImageAnalysisItemStatus::kError;
        r.error  = std::string("image analysis rpc failed: ") + e.what();
        job->AppendResult(std::move(r));
        update_and_dispatch([&](ImageAnalysisProgress& p) { p.failed += 1; });
        continue;  // slot_guard releases the slot on scope exit
      }
    }  // slot_guard releases the slot here on the success path

    // (6) Post-RPC cancel check: if canceled during the call, discard the result even
    // if the provider succeeded — this is the correctness guarantee, not CancelTask.
    if (job->IsCanceled()) {
      r.status = ImageAnalysisItemStatus::kCanceled;
      job->AppendResult(std::move(r));
      update_and_dispatch([&](ImageAnalysisProgress& p) { p.canceled += 1; });
      continue;
    }

    const bool ok =
        (options.task == ImageAnalysisTask::kScore) ? r.rating.ok : r.understanding.ok;
    if (ok) {
      r.status = ImageAnalysisItemStatus::kAnalyzed;
      // Prefer the sidecar-echoed rendition when present (records what was analyzed).
      if (r.understanding.rendition.width != 0 || r.understanding.rendition.height != 0 ||
          r.rating.rendition.width != 0 || r.rating.rendition.height != 0) {
        r.rendition = (options.task == ImageAnalysisTask::kScore) ? r.rating.rendition
                                                                  : r.understanding.rendition;
      }
      update_and_dispatch([&](ImageAnalysisProgress& p) { p.analyzed += 1; });
    } else {
      r.status = ImageAnalysisItemStatus::kError;
      r.error  = (options.task == ImageAnalysisTask::kScore) ? r.rating.error
                                                             : r.understanding.error;
      update_and_dispatch([&](ImageAnalysisProgress& p) { p.failed += 1; });
    }
    job->AppendResult(std::move(r));
  }

  if (on_finished) {
    on_finished(job->Results());
  }
  job->Finish();
}

}  // namespace alcedo
