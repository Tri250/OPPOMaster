//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/semantic_generation_service.hpp"

#include <gtest/gtest.h>

#include <atomic>
#include <chrono>
#include <filesystem>
#include <future>
#include <mutex>
#include <thread>
#include <vector>

#include "app/history_mgmt_service.hpp"
#include "app/import_service.hpp"
#include "app/pipeline_service.hpp"
#include "app/project_service.hpp"
#include "edit/operators/operator_registeration.hpp"
#include "utils/clock/time_provider.hpp"

namespace alcedo {
namespace {
using namespace std::chrono_literals;

class CountingRealThumbnailProvider final : public ISemanticThumbnailProvider {
 public:
  explicit CountingRealThumbnailProvider(std::shared_ptr<ThumbnailService> service)
      : inner_(std::move(service)) {}

  void RequestThumbnail(const SemanticGenerationItem& item, ThumbnailResolution resolution,
                        SemanticThumbnailRequestCallback callback) override {
    request_count_.fetch_add(1);
    inner_.RequestThumbnail(item, resolution, std::move(callback));
  }

  void CancelThumbnail(const ThumbnailCacheKey& key) override {
    cancel_count_.fetch_add(1);
    inner_.CancelThumbnail(key);
  }

  void ReleaseThumbnail(const ThumbnailCacheKey& key) override {
    release_count_.fetch_add(1);
    inner_.ReleaseThumbnail(key);
  }

  auto RequestCount() const -> int { return request_count_.load(); }
  auto ReleaseCount() const -> int { return release_count_.load(); }
  auto CancelCount() const -> int { return cancel_count_.load(); }

 private:
  ThumbnailServiceSemanticThumbnailProvider inner_;
  std::atomic<int>                          request_count_{0};
  std::atomic<int>                          release_count_{0};
  std::atomic<int>                          cancel_count_{0};
};

class RecordingEmbeddingClient final : public ISemanticImageEmbeddingClient {
 public:
  explicit RecordingEmbeddingClient(std::chrono::milliseconds delay = 0ms) : delay_(delay) {}

  auto GetModelInfo(SemanticRuntimeModelInfo* info, std::string* error) -> bool override {
    (void)error;
    if (info) {
      info->model_id            = "mock/mobileclip";
      info->revision            = "mock-revision";
      info->embedding_dimension = 2;
      info->image_size          = 256;
      info->provider            = "mock";
    }
    return true;
  }

  void EmbedImageBatch(std::vector<SemanticImageEmbeddingInput> inputs,
                       std::chrono::milliseconds                timeout,
                       SemanticImageEmbeddingBatchCallback      callback) override {
    (void)timeout;
    {
      std::unique_lock lock(lock_);
      batch_sizes_.push_back(inputs.size());
      for (const auto& input : inputs) {
        EXPECT_FALSE(input.rgba8_image.empty());
        EXPECT_TRUE(input.format_hint.starts_with("rgba8:"));
      }
    }

    const auto delay = delay_;
    std::thread([inputs = std::move(inputs), callback = std::move(callback), delay]() mutable {
      if (delay.count() > 0) {
        std::this_thread::sleep_for(delay);
      }

      std::vector<SemanticImageEmbeddingBatchResult> results;
      results.reserve(inputs.size());
      for (const auto& input : inputs) {
        SemanticImageEmbeddingBatchResult result;
        result.item                 = input.item;
        result.embedding.request_id = input.request_id;
        result.embedding.ok         = true;
        result.embedding.dimension  = 2;
        result.embedding.embedding  = {1.0f, 0.0f};
        results.push_back(std::move(result));
      }
      callback(std::move(results));
    }).detach();
  }

  auto BatchSizes() const -> std::vector<size_t> {
    std::unique_lock lock(lock_);
    return batch_sizes_;
  }

 private:
  std::chrono::milliseconds delay_;
  mutable std::mutex        lock_;
  std::vector<size_t>       batch_sizes_;
};

class ImmediateThumbnailProvider final : public ISemanticThumbnailProvider {
 public:
  void RequestThumbnail(const SemanticGenerationItem& item, ThumbnailResolution resolution,
                        SemanticThumbnailRequestCallback callback) override {
    request_count_.fetch_add(1);
    ThumbnailRequestResult result;
    result.key     = ThumbnailCacheKey{item.element_id, resolution};
    result.status  = ThumbnailRequestStatus::kReady;
    result.guard   = std::make_shared<ThumbnailGuard>();
    result.guard->thumbnail_buffer_ =
        std::make_unique<ImageBuffer>(cv::Mat(3, 2, CV_8UC4, cv::Scalar(10, 20, 30, 255)));
    callback(std::move(result));
  }

  void CancelThumbnail(const ThumbnailCacheKey& key) override {
    (void)key;
    cancel_count_.fetch_add(1);
  }

  void ReleaseThumbnail(const ThumbnailCacheKey& key) override {
    (void)key;
    release_count_.fetch_add(1);
  }

  auto RequestCount() const -> int { return request_count_.load(); }
  auto ReleaseCount() const -> int { return release_count_.load(); }
  auto CancelCount() const -> int { return cancel_count_.load(); }

 private:
  std::atomic<int> request_count_{0};
  std::atomic<int> release_count_{0};
  std::atomic<int> cancel_count_{0};
};

class ScriptedEmbeddingClient final : public ISemanticImageEmbeddingClient {
 public:
  auto GetModelInfo(SemanticRuntimeModelInfo* info, std::string* error) -> bool override {
    (void)error;
    if (info) {
      info->model_id            = "mock/mobileclip";
      info->revision            = "mock-revision";
      info->embedding_dimension = 2;
      info->image_size          = 256;
      info->provider            = "mock";
    }
    return true;
  }

  void EmbedImageBatch(std::vector<SemanticImageEmbeddingInput> inputs,
                       std::chrono::milliseconds                timeout,
                       SemanticImageEmbeddingBatchCallback      callback) override {
    (void)timeout;
    std::vector<SemanticImageEmbeddingBatchResult> results;
    results.reserve(inputs.size());
    for (size_t i = 0; i < inputs.size(); ++i) {
      SemanticImageEmbeddingBatchResult result;
      result.item                 = inputs[i].item;
      result.embedding.request_id = inputs[i].request_id;
      result.embedding.dimension  = 2;
      if (i == 1) {
        result.embedding.ok    = false;
        result.embedding.error = "scripted partial failure";
      } else {
        result.embedding.ok        = true;
        result.embedding.embedding = {1.0f, 0.0f};
      }
      if (i == 2) {
        result.embedding.request_id = "unexpected-request-id";
      }
      results.push_back(std::move(result));
    }
    callback(std::move(results));
  }
};

class NeverRespondingEmbeddingClient final : public ISemanticImageEmbeddingClient {
 public:
  auto GetModelInfo(SemanticRuntimeModelInfo* info, std::string* error) -> bool override {
    (void)error;
    if (info) {
      info->model_id            = "mock/mobileclip";
      info->revision            = "mock-revision";
      info->embedding_dimension = 2;
      info->image_size          = 256;
    }
    return true;
  }

  void EmbedImageBatch(std::vector<SemanticImageEmbeddingInput> inputs,
                       std::chrono::milliseconds                timeout,
                       SemanticImageEmbeddingBatchCallback      callback) override {
    (void)inputs;
    (void)timeout;
    (void)callback;
  }
};

template <typename Predicate>
auto WaitUntil(Predicate predicate, std::chrono::milliseconds timeout = 120s) -> bool {
  const auto deadline = std::chrono::steady_clock::now() + timeout;
  while (std::chrono::steady_clock::now() < deadline) {
    if (predicate()) {
      return true;
    }
    std::this_thread::sleep_for(10ms);
  }
  return predicate();
}

class SemanticGenerationServiceTest : public ::testing::Test {
 protected:
  void SetUp() override {
    TimeProvider::Refresh();
    RegisterAllOperators();
    const auto unique = std::to_string(std::chrono::steady_clock::now().time_since_epoch().count());
    db_path_ = std::filesystem::temp_directory_path() / ("semantic_generation_" + unique + ".db");
    meta_path_ =
        std::filesystem::temp_directory_path() / ("semantic_generation_" + unique + ".json");
    std::filesystem::remove(db_path_);
    std::filesystem::remove(meta_path_);
  }

  void TearDown() override {
    std::filesystem::remove(db_path_);
    std::filesystem::remove(meta_path_);
  }

  auto ImportItems(ProjectService& project, size_t count) -> std::vector<SemanticGenerationItem> {
    auto                        fs_service = project.GetSleeveService();
    auto                        img_pool   = project.GetImagePoolService();

    std::vector<image_path_t>   paths;
    const std::filesystem::path img_dir =
        std::filesystem::path(TEST_IMG_PATH) / "raw" / "batch_import";
    for (const auto& entry : std::filesystem::directory_iterator(img_dir)) {
      if (entry.is_regular_file()) {
        paths.push_back(entry.path());
      }
      if (paths.size() >= count) {
        break;
      }
    }
    EXPECT_GE(paths.size(), count);

    ImportServiceImpl          import_service(fs_service, img_pool);
    auto                       import_job = std::make_shared<ImportJob>();
    std::promise<ImportResult> done;
    auto                       future = done.get_future();
    import_job->on_finished_ = [&done](const ImportResult& result) { done.set_value(result); };

    import_job               = import_service.ImportToFolder(paths, L"", {}, import_job);
    EXPECT_NE(import_job, nullptr);
    EXPECT_EQ(future.wait_for(120s), std::future_status::ready);
    const auto result = future.get();
    EXPECT_EQ(result.failed_, 0u);

    const auto snapshot = import_job->import_log_->Snapshot();
    import_service.SyncImports(snapshot, L"");
    fs_service->Sync();
    img_pool->SyncWithStorage();
    project.SaveProject(meta_path_);

    std::vector<SemanticGenerationItem> items;
    items.reserve(snapshot.created_.size());
    for (const auto& created : snapshot.created_) {
      items.push_back(SemanticGenerationItem{created.element_id_, created.image_id_});
    }
    return items;
  }

  std::filesystem::path db_path_;
  std::filesystem::path meta_path_;
};

}  // namespace

TEST_F(SemanticGenerationServiceTest, UsesRealThumbnailServiceAndBatchesMockEmbedding) {
  ProjectService project(db_path_, meta_path_, ProjectOpenMode::kCreateNew);
  auto           items = ImportItems(project, 3);
  ASSERT_EQ(items.size(), 3u);

  auto pipeline_service  = std::make_shared<PipelineMgmtService>(project.GetStorageService());
  auto history_service   = std::make_shared<EditHistoryMgmtService>(project.GetStorageService());
  auto thumbnail_service = std::make_shared<ThumbnailService>(
      project.GetSleeveService(), project.GetImagePoolService(), pipeline_service, history_service,
      project.GetProjectUUID());

  auto thumbnails = std::make_shared<CountingRealThumbnailProvider>(thumbnail_service);
  auto embedder   = std::make_shared<RecordingEmbeddingClient>();
  SemanticGenerationService service(thumbnails, embedder);

  SemanticGenerationOptions options;
  options.thumbnail_resolution = ThumbnailResolution::k256;
  options.batch_size           = 2;

  auto job                     = service.StartGeneration(items, options);
  job->Wait();

  const auto progress = job->SnapshotProgress();
  EXPECT_EQ(progress.total, 3u);
  EXPECT_EQ(progress.thumbnails_ready, 3u);
  EXPECT_EQ(progress.embedding_requested, 3u);
  EXPECT_EQ(progress.embedded, 3u);
  EXPECT_EQ(progress.failed, 0u);
  EXPECT_EQ(progress.canceled, 0u);
  EXPECT_EQ(thumbnails->RequestCount(), 3);
  EXPECT_EQ(thumbnails->ReleaseCount(), 3);

  const std::vector<size_t> expected_batches{2, 1};
  EXPECT_EQ(embedder->BatchSizes(), expected_batches);

  const auto results = job->Results();
  ASSERT_EQ(results.size(), 3u);
  for (const auto& result : results) {
    EXPECT_EQ(result.status, SemanticGenerationItemStatus::kEmbedded);
    EXPECT_EQ(result.embedding_dimension, 2u);
    EXPECT_FALSE(result.embedding.empty());
  }

  pipeline_service->Sync();
}

TEST_F(SemanticGenerationServiceTest, RealThumbnailFailureSkipsMockEmbeddingForThatItem) {
  ProjectService project(db_path_, meta_path_, ProjectOpenMode::kCreateNew);
  auto           items = ImportItems(project, 1);
  ASSERT_EQ(items.size(), 1u);
  items.push_back(SemanticGenerationItem{999999u, 999999u});

  auto pipeline_service  = std::make_shared<PipelineMgmtService>(project.GetStorageService());
  auto thumbnail_service = std::make_shared<ThumbnailService>(
      project.GetSleeveService(), project.GetImagePoolService(), pipeline_service);

  auto thumbnails = std::make_shared<CountingRealThumbnailProvider>(thumbnail_service);
  auto embedder   = std::make_shared<RecordingEmbeddingClient>();
  SemanticGenerationService service(thumbnails, embedder);

  SemanticGenerationOptions options;
  options.thumbnail_resolution = ThumbnailResolution::k256;
  options.batch_size           = 8;

  auto job                     = service.StartGeneration(items, options);
  job->Wait();

  const auto progress = job->SnapshotProgress();
  EXPECT_EQ(progress.total, 2u);
  EXPECT_EQ(progress.thumbnails_ready, 1u);
  EXPECT_EQ(progress.embedded, 1u);
  EXPECT_EQ(progress.failed, 1u);
  EXPECT_EQ(thumbnails->RequestCount(), 2);
  EXPECT_EQ(thumbnails->ReleaseCount(), 1);

  const std::vector<size_t> expected_batches{1};
  EXPECT_EQ(embedder->BatchSizes(), expected_batches);

  pipeline_service->Sync();
}

TEST_F(SemanticGenerationServiceTest, CancelDuringMockEmbeddingDoesNotHoldRealThumbnailPin) {
  ProjectService project(db_path_, meta_path_, ProjectOpenMode::kCreateNew);
  auto           items = ImportItems(project, 1);
  ASSERT_EQ(items.size(), 1u);

  auto pipeline_service  = std::make_shared<PipelineMgmtService>(project.GetStorageService());
  auto thumbnail_service = std::make_shared<ThumbnailService>(
      project.GetSleeveService(), project.GetImagePoolService(), pipeline_service);

  auto thumbnails = std::make_shared<CountingRealThumbnailProvider>(thumbnail_service);
  auto embedder   = std::make_shared<RecordingEmbeddingClient>(500ms);
  SemanticGenerationService service(thumbnails, embedder);

  auto                      job = service.StartGeneration(items);
  ASSERT_TRUE(WaitUntil([&]() { return thumbnails->ReleaseCount() == 1; }));

  job->Cancel();
  job->Wait();

  const auto progress = job->SnapshotProgress();
  EXPECT_EQ(progress.embedded, 0u);
  EXPECT_EQ(progress.canceled, 1u);
  EXPECT_EQ(thumbnails->ReleaseCount(), 1);
  EXPECT_EQ(thumbnails->CancelCount(), 0);

  pipeline_service->Sync();
}

TEST_F(SemanticGenerationServiceTest, RejectsModelInfoMismatchBeforeRequestingThumbnails) {
  auto thumbnails = std::make_shared<ImmediateThumbnailProvider>();
  auto embedder   = std::make_shared<RecordingEmbeddingClient>();
  SemanticGenerationService service(thumbnails, embedder);

  SemanticGenerationOptions options;
  SemanticRuntimeModelInfo expected;
  expected.model_id = "mock/mobileclip";
  expected.revision = "mock-revision";
  expected.embedding_dimension = 512;
  expected.image_size = 256;
  expected.provider = "mock";
  options.expected_model_info = expected;

  auto job = service.StartGeneration({{1, 10}, {2, 20}}, options);
  job->Wait();

  const auto progress = job->SnapshotProgress();
  EXPECT_EQ(progress.total, 2u);
  EXPECT_EQ(progress.failed, 2u);
  EXPECT_EQ(progress.thumbnails_ready, 0u);
  EXPECT_EQ(thumbnails->RequestCount(), 0);

  const auto results = job->Results();
  ASSERT_EQ(results.size(), 2u);
  for (const auto& result : results) {
    EXPECT_EQ(result.status, SemanticGenerationItemStatus::kError);
    EXPECT_NE(result.error.find("embedding dimension mismatch"), std::string::npos);
  }
}

TEST_F(SemanticGenerationServiceTest, MapsPartialFailureAndRequestIdMismatchPerItem) {
  auto thumbnails = std::make_shared<ImmediateThumbnailProvider>();
  auto embedder   = std::make_shared<ScriptedEmbeddingClient>();
  SemanticGenerationService service(thumbnails, embedder);

  SemanticGenerationOptions options;
  options.batch_size = 3;
  SemanticRuntimeModelInfo expected;
  expected.model_id = "mock/mobileclip";
  expected.revision = "mock-revision";
  expected.embedding_dimension = 2;
  expected.image_size = 256;
  expected.provider = "mock";
  options.expected_model_info = expected;

  auto job = service.StartGeneration({{1, 10}, {2, 20}, {3, 30}}, options);
  job->Wait();

  const auto progress = job->SnapshotProgress();
  EXPECT_EQ(progress.total, 3u);
  EXPECT_EQ(progress.thumbnails_ready, 3u);
  EXPECT_EQ(progress.embedding_requested, 3u);
  EXPECT_EQ(progress.embedded, 1u);
  EXPECT_EQ(progress.failed, 2u);
  EXPECT_EQ(thumbnails->ReleaseCount(), 3);

  const auto results = job->Results();
  ASSERT_EQ(results.size(), 3u);
  EXPECT_EQ(results[0].status, SemanticGenerationItemStatus::kEmbedded);
  EXPECT_EQ(results[1].status, SemanticGenerationItemStatus::kError);
  EXPECT_EQ(results[1].error, "scripted partial failure");
  EXPECT_EQ(results[2].status, SemanticGenerationItemStatus::kError);
  EXPECT_NE(results[2].error.find("missing image embedding response"), std::string::npos);
}

TEST_F(SemanticGenerationServiceTest, EmbeddingTimeoutFailsEveryPendingItem) {
  auto thumbnails = std::make_shared<ImmediateThumbnailProvider>();
  auto embedder   = std::make_shared<NeverRespondingEmbeddingClient>();
  SemanticGenerationService service(thumbnails, embedder);

  SemanticGenerationOptions options;
  options.batch_size = 2;
  options.embedding_timeout = 50ms;

  auto job = service.StartGeneration({{1, 10}, {2, 20}}, options);
  job->Wait();

  const auto progress = job->SnapshotProgress();
  EXPECT_EQ(progress.embedding_requested, 2u);
  EXPECT_EQ(progress.embedded, 0u);
  EXPECT_EQ(progress.failed, 2u);

  const auto results = job->Results();
  ASSERT_EQ(results.size(), 2u);
  for (const auto& result : results) {
    EXPECT_EQ(result.status, SemanticGenerationItemStatus::kError);
    EXPECT_NE(result.error.find("timed out"), std::string::npos);
  }
}

}  // namespace alcedo
