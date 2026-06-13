//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/semantic_generation_service.hpp"

#include <algorithm>
#include <cstdint>
#include <exception>
#include <future>
#include <numeric>
#include <opencv2/imgproc.hpp>
#include <stdexcept>
#include <thread>
#include <utility>

namespace alcedo {
namespace {

auto MakeRequestId(const SemanticGenerationItem& item) -> std::string {
  return "semantic-image-" + std::to_string(item.element_id) + "-" + std::to_string(item.image_id);
}

void DispatchProgress(const std::shared_ptr<SemanticGenerationJob>& job,
                      const SemanticGenerationProgressCallback&     callback) {
  if (callback) {
    callback(job->SnapshotProgress());
  }
}

auto EncodeThumbnailRgba8(const ThumbnailGuard& guard, std::vector<uint8_t>* encoded,
                          std::string* format_hint, std::string* error) -> bool {
  if (!guard.thumbnail_buffer_) {
    if (error) {
      *error = "thumbnail guard does not contain an image buffer";
    }
    return false;
  }

  auto* buffer = guard.thumbnail_buffer_.get();
  try {
    if (!buffer->cpu_data_valid_ && buffer->gpu_data_valid_) {
      buffer->SyncToCPU();
    }
  } catch (const std::exception& e) {
    if (error) {
      *error = std::string("failed to sync thumbnail to CPU: ") + e.what();
    }
    return false;
  } catch (...) {
    if (error) {
      *error = "failed to sync thumbnail to CPU";
    }
    return false;
  }

  if (!buffer->cpu_data_valid_) {
    if (error) {
      *error = "thumbnail has no CPU-readable image data";
    }
    return false;
  }

  cv::Mat src = buffer->GetCPUData();
  if (src.empty()) {
    if (error) {
      *error = "thumbnail image data is empty";
    }
    return false;
  }

  cv::Mat src8;
  if (src.depth() == CV_8U) {
    src8 = src.isContinuous() ? src : src.clone();
  } else if (src.depth() == CV_32F) {
    src.convertTo(src8, CV_MAKETYPE(CV_8U, src.channels()), 255.0);
  } else {
    src.convertTo(src8, CV_MAKETYPE(CV_8U, src.channels()));
  }

  cv::Mat rgba8;
  switch (src8.channels()) {
    case 1:
      cv::cvtColor(src8, rgba8, cv::COLOR_GRAY2RGBA);
      break;
    case 3:
      cv::cvtColor(src8, rgba8, cv::COLOR_RGB2RGBA);
      break;
    case 4:
      rgba8 = src8.isContinuous() ? src8 : src8.clone();
      break;
    default:
      if (error) {
        *error = "thumbnail image has unsupported channel count for RGBA8 conversion";
      }
      return false;
  }

  if (!rgba8.isContinuous()) {
    rgba8 = rgba8.clone();
  }

  const auto           byte_count = static_cast<size_t>(rgba8.total()) * rgba8.elemSize();
  std::vector<uint8_t> bytes(byte_count);
  std::copy(rgba8.data, rgba8.data + byte_count, bytes.begin());
  if (format_hint) {
    *format_hint = "rgba8:" + std::to_string(rgba8.cols) + "x" + std::to_string(rgba8.rows);
  }
  *encoded = std::move(bytes);
  return true;
}

struct ThumbnailWaitState {
  std::mutex              lock;
  std::condition_variable cv;
  ThumbnailRequestResult  result;
  bool                    done      = false;
  bool                    abandoned = false;
};

auto WaitForThumbnail(const std::shared_ptr<SemanticGenerationJob>&      job,
                      const std::shared_ptr<ISemanticThumbnailProvider>& provider,
                      const SemanticGenerationItem& item, ThumbnailResolution resolution)
    -> ThumbnailRequestResult {
  auto                    state = std::make_shared<ThumbnailWaitState>();
  const ThumbnailCacheKey key{item.element_id, resolution};

  provider->RequestThumbnail(item, resolution, [state, provider](ThumbnailRequestResult result) {
    bool release_late_guard = false;
    {
      std::unique_lock lock(state->lock);
      release_late_guard = state->abandoned && result.guard != nullptr;
      state->result      = std::move(result);
      state->done        = true;
    }
    if (release_late_guard) {
      provider->ReleaseThumbnail(state->result.key);
    }
    state->cv.notify_all();
  });

  std::unique_lock lock(state->lock);
  while (!state->done) {
    if (job->IsCanceled()) {
      state->abandoned = true;
      lock.unlock();
      provider->CancelThumbnail(key);
      ThumbnailRequestResult canceled;
      canceled.key     = key;
      canceled.status  = ThumbnailRequestStatus::kCanceled;
      canceled.message = "semantic generation job was canceled";
      return canceled;
    }
    state->cv.wait_for(lock, std::chrono::milliseconds(25));
  }

  return std::move(state->result);
}

auto WaitForEmbeddingBatch(const std::shared_ptr<SemanticGenerationJob>&         job,
                           const std::shared_ptr<ISemanticImageEmbeddingClient>& client,
                           std::vector<SemanticImageEmbeddingInput>              inputs,
                           std::chrono::milliseconds                             timeout)
    -> std::vector<SemanticImageEmbeddingBatchResult> {
  auto promise = std::make_shared<std::promise<std::vector<SemanticImageEmbeddingBatchResult>>>();
  auto future  = promise->get_future();

  client->EmbedImageBatch(std::move(inputs), timeout,
                          [promise](std::vector<SemanticImageEmbeddingBatchResult> results) {
                            try {
                              promise->set_value(std::move(results));
                            } catch (...) {
                            }
                          });

  while (future.wait_for(std::chrono::milliseconds(25)) != std::future_status::ready) {
    if (job->IsCanceled()) {
      return {};
    }
  }
  return future.get();
}

}  // namespace

auto ToString(SemanticGenerationItemStatus status) -> const char* {
  switch (status) {
    case SemanticGenerationItemStatus::kPending:
      return "pending";
    case SemanticGenerationItemStatus::kThumbnailReady:
      return "thumbnail_ready";
    case SemanticGenerationItemStatus::kEmbeddingRequested:
      return "embedding_requested";
    case SemanticGenerationItemStatus::kEmbedded:
      return "embedded";
    case SemanticGenerationItemStatus::kCanceled:
      return "canceled";
    case SemanticGenerationItemStatus::kError:
      return "error";
  }
  return "unknown";
}

ThumbnailServiceSemanticThumbnailProvider::ThumbnailServiceSemanticThumbnailProvider(
    std::shared_ptr<ThumbnailService> service)
    : service_(std::move(service)) {}

void ThumbnailServiceSemanticThumbnailProvider::RequestThumbnail(
    const SemanticGenerationItem& item, ThumbnailResolution resolution,
    SemanticThumbnailRequestCallback callback) {
  if (!service_) {
    ThumbnailRequestResult result;
    result.key     = ThumbnailCacheKey{item.element_id, resolution};
    result.status  = ThumbnailRequestStatus::kError;
    result.message = "ThumbnailService is not available";
    callback(std::move(result));
    return;
  }

  service_->GetThumbnailDetailed(item.element_id, item.image_id, std::move(callback), true, nullptr,
                                 resolution);
}

void ThumbnailServiceSemanticThumbnailProvider::CancelThumbnail(const ThumbnailCacheKey& key) {
  if (service_) {
    service_->CancelPending(key);
  }
}

void ThumbnailServiceSemanticThumbnailProvider::ReleaseThumbnail(const ThumbnailCacheKey& key) {
  if (service_) {
    service_->ReleaseThumbnail(key);
  }
}

MockSemanticImageEmbeddingClient::MockSemanticImageEmbeddingClient(
    std::chrono::milliseconds response_delay, uint32_t embedding_dimension)
    : response_delay_(response_delay), embedding_dimension_(embedding_dimension) {}

void MockSemanticImageEmbeddingClient::EmbedImageBatch(
    std::vector<SemanticImageEmbeddingInput> inputs, std::chrono::milliseconds timeout,
    SemanticImageEmbeddingBatchCallback callback) {
  (void)timeout;
  std::unordered_set<std::string> fail_ids;
  {
    std::unique_lock lock(lock_);
    fail_ids = fail_request_ids_;
  }

  const auto delay = response_delay_;
  const auto dim   = embedding_dimension_;
  std::thread([inputs = std::move(inputs), callback = std::move(callback),
               fail_ids = std::move(fail_ids), delay, dim]() mutable {
    if (delay.count() > 0) {
      std::this_thread::sleep_for(delay);
    }

    std::vector<SemanticImageEmbeddingBatchResult> results;
    results.reserve(inputs.size());
    for (const auto& input : inputs) {
      SemanticImageEmbeddingBatchResult batch_result;
      batch_result.item                 = input.item;
      batch_result.embedding.request_id = input.request_id;
      batch_result.embedding.model_name = "mock/mobileclip";
      batch_result.embedding.dimension  = dim;
      if (fail_ids.contains(input.request_id)) {
        batch_result.embedding.ok    = false;
        batch_result.embedding.error = "mock image embedding failure";
      } else {
        batch_result.embedding.ok = true;
        batch_result.embedding.embedding.resize(dim, 0.0f);
        if (dim > 0) {
          const auto byte_sum =
              std::accumulate(input.encoded_image.begin(), input.encoded_image.end(), uint64_t{0});
          batch_result.embedding.embedding[0] = static_cast<float>((byte_sum % 997) + 1) / 997.0f;
        }
        if (dim > 1) {
          batch_result.embedding.embedding[1] =
              static_cast<float>((input.encoded_image.size() % 991) + 1) / 991.0f;
        }
      }
      results.push_back(std::move(batch_result));
    }

    if (callback) {
      callback(std::move(results));
    }
  }).detach();
}

void MockSemanticImageEmbeddingClient::FailRequestIds(std::unordered_set<std::string> request_ids) {
  std::unique_lock lock(lock_);
  fail_request_ids_ = std::move(request_ids);
}

SemanticGenerationJob::~SemanticGenerationJob() {
  Cancel();
  if (worker_.joinable()) {
    if (worker_.get_id() == std::this_thread::get_id()) {
      worker_.detach();
    } else {
      worker_.join();
    }
  }
}

void SemanticGenerationJob::Cancel() { canceled_.store(true); }

auto SemanticGenerationJob::IsCanceled() const -> bool { return canceled_.load(); }

void SemanticGenerationJob::Wait() {
  {
    std::unique_lock lock(lock_);
    finished_cv_.wait(lock, [this]() { return finished_; });
  }
  if (worker_.joinable() && worker_.get_id() != std::this_thread::get_id()) {
    worker_.join();
  }
}

auto SemanticGenerationJob::SnapshotProgress() const -> SemanticGenerationProgress {
  std::unique_lock lock(lock_);
  return progress_;
}

auto SemanticGenerationJob::Results() const -> std::vector<SemanticGenerationItemResult> {
  std::unique_lock lock(lock_);
  return results_;
}

void SemanticGenerationJob::UpdateProgress(
    const std::function<void(SemanticGenerationProgress&)>& updater) {
  std::unique_lock lock(lock_);
  updater(progress_);
}

void SemanticGenerationJob::AppendResult(SemanticGenerationItemResult result) {
  std::unique_lock lock(lock_);
  results_.push_back(std::move(result));
}

void SemanticGenerationJob::SetWorkerThread(std::thread worker) { worker_ = std::move(worker); }

void SemanticGenerationJob::Finish() {
  {
    std::unique_lock lock(lock_);
    finished_ = true;
  }
  finished_cv_.notify_all();
}

SemanticGenerationService::SemanticGenerationService(
    std::shared_ptr<ISemanticThumbnailProvider>    thumbnail_provider,
    std::shared_ptr<ISemanticImageEmbeddingClient> embedding_client)
    : thumbnail_provider_(std::move(thumbnail_provider)),
      embedding_client_(std::move(embedding_client)) {
  if (!thumbnail_provider_) {
    throw std::invalid_argument("SemanticGenerationService requires a thumbnail provider");
  }
  if (!embedding_client_) {
    throw std::invalid_argument("SemanticGenerationService requires an embedding client");
  }
}

auto SemanticGenerationService::StartGeneration(std::vector<SemanticGenerationItem> items,
                                                SemanticGenerationOptions           options,
                                                SemanticGenerationProgressCallback  on_progress,
                                                SemanticGenerationFinishedCallback  on_finished)
    -> std::shared_ptr<SemanticGenerationJob> {
  if (options.batch_size == 0) {
    options.batch_size = 1;
  }

  auto job = std::make_shared<SemanticGenerationJob>();
  job->UpdateProgress(
      [total = items.size()](SemanticGenerationProgress& progress) { progress.total = total; });

  auto thumbnail_provider = thumbnail_provider_;
  auto embedding_client   = embedding_client_;
  auto worker             = std::thread(
      [job, items = std::move(items), options, on_progress = std::move(on_progress),
       on_finished = std::move(on_finished), thumbnail_provider = std::move(thumbnail_provider),
       embedding_client = std::move(embedding_client)]() mutable {
        RunJob(job, items, options, std::move(on_progress), std::move(on_finished),
                           std::move(thumbnail_provider), std::move(embedding_client));
      });
  job->SetWorkerThread(std::move(worker));

  return job;
}

void SemanticGenerationService::RunJob(
    const std::shared_ptr<SemanticGenerationJob>& job,
    const std::vector<SemanticGenerationItem>& items, SemanticGenerationOptions options,
    SemanticGenerationProgressCallback on_progress, SemanticGenerationFinishedCallback on_finished,
    std::shared_ptr<ISemanticThumbnailProvider>    thumbnail_provider,
    std::shared_ptr<ISemanticImageEmbeddingClient> embedding_client) {
  auto finish = [&]() {
    auto results = job->Results();
    job->Finish();
    if (on_finished) {
      on_finished(std::move(results));
    }
  };

  std::vector<SemanticImageEmbeddingInput> batch;
  batch.reserve(options.batch_size);

  auto flush_batch = [&]() {
    if (batch.empty()) {
      return;
    }

    const auto batch_count = batch.size();
    job->UpdateProgress([batch_count](SemanticGenerationProgress& progress) {
      progress.embedding_requested += batch_count;
    });
    DispatchProgress(job, on_progress);

    std::vector<SemanticImageEmbeddingBatchResult> batch_results;
    try {
      batch_results =
          WaitForEmbeddingBatch(job, embedding_client, std::move(batch), options.embedding_timeout);
    } catch (const std::exception& e) {
      for (size_t i = 0; i < batch_count; ++i) {
        job->UpdateProgress([](SemanticGenerationProgress& progress) { progress.failed++; });
      }
      SemanticGenerationItemResult result;
      result.status = SemanticGenerationItemStatus::kError;
      result.error  = std::string("image embedding batch failed: ") + e.what();
      job->AppendResult(std::move(result));
      DispatchProgress(job, on_progress);
      batch.clear();
      return;
    }
    batch.clear();

    if (job->IsCanceled()) {
      job->UpdateProgress([batch_count](SemanticGenerationProgress& progress) {
        progress.canceled += batch_count;
      });
      DispatchProgress(job, on_progress);
      return;
    }

    for (auto& batch_result : batch_results) {
      SemanticGenerationItemResult item_result;
      item_result.item       = batch_result.item;
      item_result.request_id = batch_result.embedding.request_id;
      if (batch_result.embedding.ok) {
        item_result.status              = SemanticGenerationItemStatus::kEmbedded;
        item_result.embedding           = std::move(batch_result.embedding.embedding);
        item_result.embedding_dimension = batch_result.embedding.dimension;
        job->UpdateProgress([](SemanticGenerationProgress& progress) { progress.embedded++; });
      } else {
        item_result.status = SemanticGenerationItemStatus::kError;
        item_result.error  = std::move(batch_result.embedding.error);
        job->UpdateProgress([](SemanticGenerationProgress& progress) { progress.failed++; });
      }
      job->AppendResult(std::move(item_result));
    }
    DispatchProgress(job, on_progress);
  };

  for (const auto& item : items) {
    if (job->IsCanceled()) {
      SemanticGenerationItemResult result;
      result.item       = item;
      result.request_id = MakeRequestId(item);
      result.status     = SemanticGenerationItemStatus::kCanceled;
      result.error      = "semantic generation job was canceled";
      job->UpdateProgress([](SemanticGenerationProgress& progress) { progress.canceled++; });
      job->AppendResult(std::move(result));
      continue;
    }

    ThumbnailRequestResult thumbnail_result;
    try {
      thumbnail_result =
          WaitForThumbnail(job, thumbnail_provider, item, options.thumbnail_resolution);
    } catch (const std::exception& e) {
      SemanticGenerationItemResult result;
      result.item       = item;
      result.request_id = MakeRequestId(item);
      result.status     = SemanticGenerationItemStatus::kError;
      result.error      = std::string("thumbnail request failed: ") + e.what();
      job->UpdateProgress([](SemanticGenerationProgress& progress) { progress.failed++; });
      job->AppendResult(std::move(result));
      DispatchProgress(job, on_progress);
      continue;
    }

    if (thumbnail_result.status == ThumbnailRequestStatus::kCanceled || job->IsCanceled()) {
      SemanticGenerationItemResult result;
      result.item       = item;
      result.request_id = MakeRequestId(item);
      result.status     = SemanticGenerationItemStatus::kCanceled;
      result.error      = thumbnail_result.message.empty() ? "thumbnail request was canceled"
                                                           : thumbnail_result.message;
      if (thumbnail_result.guard) {
        thumbnail_provider->ReleaseThumbnail(thumbnail_result.key);
      }
      job->UpdateProgress([](SemanticGenerationProgress& progress) { progress.canceled++; });
      job->AppendResult(std::move(result));
      DispatchProgress(job, on_progress);
      continue;
    }

    if (thumbnail_result.status != ThumbnailRequestStatus::kReady || !thumbnail_result.guard) {
      SemanticGenerationItemResult result;
      result.item       = item;
      result.request_id = MakeRequestId(item);
      result.status     = SemanticGenerationItemStatus::kError;
      result.error =
          thumbnail_result.message.empty() ? "thumbnail request failed" : thumbnail_result.message;
      job->UpdateProgress([](SemanticGenerationProgress& progress) { progress.failed++; });
      job->AppendResult(std::move(result));
      DispatchProgress(job, on_progress);
      continue;
    }

    job->UpdateProgress([](SemanticGenerationProgress& progress) { progress.thumbnails_ready++; });

    SemanticImageEmbeddingInput input;
    input.item       = item;
    input.request_id = MakeRequestId(item);
    std::string encode_error;
    const bool  encoded = EncodeThumbnailRgba8(*thumbnail_result.guard, &input.encoded_image,
                                               &input.format_hint, &encode_error);
    thumbnail_provider->ReleaseThumbnail(thumbnail_result.key);

    if (!encoded) {
      SemanticGenerationItemResult result;
      result.item       = item;
      result.request_id = input.request_id;
      result.status     = SemanticGenerationItemStatus::kError;
      result.error      = std::move(encode_error);
      job->UpdateProgress([](SemanticGenerationProgress& progress) { progress.failed++; });
      job->AppendResult(std::move(result));
      DispatchProgress(job, on_progress);
      continue;
    }

    batch.push_back(std::move(input));
    if (batch.size() >= options.batch_size) {
      flush_batch();
    }
  }

  flush_batch();
  finish();
}

}  // namespace alcedo
