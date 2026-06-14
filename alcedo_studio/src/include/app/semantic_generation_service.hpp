//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <atomic>
#include <chrono>
#include <condition_variable>
#include <cstddef>
#include <cstdint>
#include <functional>
#include <memory>
#include <mutex>
#include <optional>
#include <string>
#include <thread>
#include <unordered_set>
#include <vector>

#include "app/semantic_runtime_service.hpp"
#include "app/thumbnail_service.hpp"
#include "app/thumbnail_types.hpp"
#include "type/type.hpp"

namespace alcedo {

struct SemanticGenerationItem {
  sl_element_id_t element_id = 0;
  image_id_t      image_id   = 0;
};

enum class SemanticGenerationItemStatus : uint8_t {
  kPending = 0,
  kThumbnailReady,
  kEmbeddingRequested,
  kEmbedded,
  kCanceled,
  kError,
};

struct SemanticGenerationItemResult {
  SemanticGenerationItem       item{};
  std::string                  request_id;
  SemanticGenerationItemStatus status = SemanticGenerationItemStatus::kPending;
  std::string                  error;
  std::vector<float>           embedding;
  uint32_t                     embedding_dimension = 0;
};

struct SemanticGenerationProgress {
  size_t total               = 0;
  size_t thumbnails_ready    = 0;
  size_t embedding_requested = 0;
  size_t embedded            = 0;
  size_t failed              = 0;
  size_t canceled            = 0;
};

struct SemanticGenerationOptions {
  ThumbnailResolution       thumbnail_resolution = ThumbnailResolution::k256;
  size_t                    batch_size           = 16;
  std::chrono::milliseconds embedding_timeout{30000};
  std::optional<SemanticRuntimeModelInfo> expected_model_info;
};

struct SemanticImageEmbeddingInput {
  SemanticGenerationItem item{};
  std::string            request_id;
  std::vector<uint8_t>   rgba8_image;
  std::string            format_hint;
};

struct SemanticImageEmbeddingBatchResult {
  SemanticGenerationItem  item{};
  SemanticEmbeddingResult embedding{};
};

using SemanticGenerationProgressCallback = std::function<void(const SemanticGenerationProgress&)>;
using SemanticGenerationFinishedCallback =
    std::function<void(std::vector<SemanticGenerationItemResult>)>;
using SemanticThumbnailRequestCallback = std::function<void(ThumbnailRequestResult)>;
using SemanticImageEmbeddingBatchCallback =
    std::function<void(std::vector<SemanticImageEmbeddingBatchResult>)>;

class ISemanticThumbnailProvider {
 public:
  virtual ~ISemanticThumbnailProvider()                                    = default;

  virtual void RequestThumbnail(const SemanticGenerationItem& item, ThumbnailResolution resolution,
                                SemanticThumbnailRequestCallback callback) = 0;
  virtual void CancelThumbnail(const ThumbnailCacheKey& key)               = 0;
  virtual void ReleaseThumbnail(const ThumbnailCacheKey& key)              = 0;
};

class ThumbnailServiceSemanticThumbnailProvider final : public ISemanticThumbnailProvider {
 public:
  explicit ThumbnailServiceSemanticThumbnailProvider(std::shared_ptr<ThumbnailService> service);

  void RequestThumbnail(const SemanticGenerationItem& item, ThumbnailResolution resolution,
                        SemanticThumbnailRequestCallback callback) override;
  void CancelThumbnail(const ThumbnailCacheKey& key) override;
  void ReleaseThumbnail(const ThumbnailCacheKey& key) override;

 private:
  std::shared_ptr<ThumbnailService> service_;
};

class ISemanticImageEmbeddingClient {
 public:
  virtual ~ISemanticImageEmbeddingClient()                                   = default;

  virtual auto GetModelInfo(SemanticRuntimeModelInfo* info, std::string* error) -> bool = 0;
  virtual void EmbedImageBatch(std::vector<SemanticImageEmbeddingInput> inputs,
                               std::chrono::milliseconds                timeout,
                               SemanticImageEmbeddingBatchCallback      callback) = 0;
};

class SemanticRuntimeImageEmbeddingClient final : public ISemanticImageEmbeddingClient {
 public:
  explicit SemanticRuntimeImageEmbeddingClient(std::shared_ptr<SemanticRuntimeService> runtime);

  auto GetModelInfo(SemanticRuntimeModelInfo* info, std::string* error) -> bool override;
  void EmbedImageBatch(std::vector<SemanticImageEmbeddingInput> inputs,
                       std::chrono::milliseconds                timeout,
                       SemanticImageEmbeddingBatchCallback      callback) override;

 private:
  std::shared_ptr<SemanticRuntimeService> runtime_;
};

class MockSemanticImageEmbeddingClient final : public ISemanticImageEmbeddingClient {
 public:
  explicit MockSemanticImageEmbeddingClient(
      std::chrono::milliseconds response_delay      = std::chrono::milliseconds(0),
      uint32_t                  embedding_dimension = 2);

  auto GetModelInfo(SemanticRuntimeModelInfo* info, std::string* error) -> bool override;
  void EmbedImageBatch(std::vector<SemanticImageEmbeddingInput> inputs,
                       std::chrono::milliseconds                timeout,
                       SemanticImageEmbeddingBatchCallback      callback) override;

  void FailRequestIds(std::unordered_set<std::string> request_ids);

 private:
  std::chrono::milliseconds       response_delay_;
  uint32_t                        embedding_dimension_;
  std::mutex                      lock_;
  std::unordered_set<std::string> fail_request_ids_;
};

class SemanticGenerationJob final {
 public:
  SemanticGenerationJob() = default;
  ~SemanticGenerationJob();

  SemanticGenerationJob(const SemanticGenerationJob&)            = delete;
  SemanticGenerationJob& operator=(const SemanticGenerationJob&) = delete;

  void                   Cancel();
  auto                   IsCanceled() const -> bool;
  void                   Wait();
  auto                   SnapshotProgress() const -> SemanticGenerationProgress;
  auto                   Results() const -> std::vector<SemanticGenerationItemResult>;

 private:
  friend class SemanticGenerationService;

  void UpdateProgress(const std::function<void(SemanticGenerationProgress&)>& updater);
  void AppendResult(SemanticGenerationItemResult result);
  void SetWorkerThread(std::thread worker);
  void Finish();

  mutable std::mutex                        lock_;
  std::condition_variable                   finished_cv_;
  SemanticGenerationProgress                progress_{};
  std::vector<SemanticGenerationItemResult> results_;
  std::atomic<bool>                         canceled_{false};
  std::thread                               worker_;
  bool                                      finished_ = false;
};

class SemanticGenerationService final {
 public:
  SemanticGenerationService(std::shared_ptr<ISemanticThumbnailProvider>    thumbnail_provider,
                            std::shared_ptr<ISemanticImageEmbeddingClient> embedding_client);

  auto StartGeneration(std::vector<SemanticGenerationItem> items,
                       SemanticGenerationOptions           options     = {},
                       SemanticGenerationProgressCallback  on_progress = {},
                       SemanticGenerationFinishedCallback  on_finished = {})
      -> std::shared_ptr<SemanticGenerationJob>;

 private:
  static void RunJob(const std::shared_ptr<SemanticGenerationJob>&  job,
                     const std::vector<SemanticGenerationItem>&     items,
                     SemanticGenerationOptions                      options,
                     SemanticGenerationProgressCallback             on_progress,
                     SemanticGenerationFinishedCallback             on_finished,
                     std::shared_ptr<ISemanticThumbnailProvider>    thumbnail_provider,
                     std::shared_ptr<ISemanticImageEmbeddingClient> embedding_client);

  std::shared_ptr<ISemanticThumbnailProvider>    thumbnail_provider_;
  std::shared_ptr<ISemanticImageEmbeddingClient> embedding_client_;
};

auto ToString(SemanticGenerationItemStatus status) -> const char*;

}  // namespace alcedo
