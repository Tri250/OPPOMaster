//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/image_analysis_service.hpp"

#include <gtest/gtest.h>

#include <atomic>
#include <chrono>
#include <filesystem>
#include <memory>
#include <mutex>
#include <string>
#include <thread>

#include <opencv2/core.hpp>

#include "image/image_buffer.hpp"

namespace alcedo {
namespace {

// AIResponseStatus / AiErrorCode values (proto/ai_common.proto). Hardcoded as ints to
// keep the test proto-free, matching the input_kinds convention on AiSidecarCapability.
constexpr int kStatusOk                 = 1;
constexpr int kStatusDeadlineExceeded   = 3;
constexpr int kStatusUnauthenticated    = 6;
constexpr int kStatusProviderError      = 9;
constexpr int kStatusUnsupportedTask    = 10;
constexpr int kErrorMissingCredential   = 2;
constexpr int kErrorTaskUnknown         = 9;
constexpr int kErrorPayloadDecode       = 10;

auto ScratchDir(const std::string& tag) -> std::filesystem::path {
  return std::filesystem::temp_directory_path() / ("alcedo_ia_svc_test_" + tag);
}

// Synchronous thumbnail provider: returns a ready guard holding a small CPU mat so the
// encoder produces real JPEG bytes. No ThumbnailService / pipeline required.
class FakeThumbnailProvider : public IImageAnalysisThumbnailProvider {
 public:
  void RequestThumbnail(const ImageAnalysisItem& item, ThumbnailResolution resolution,
                        ImageAnalysisThumbnailCallback callback) override {
    ++request_count_;
    ThumbnailRequestResult r;
    r.key    = ThumbnailCacheKey{item.element_id, resolution};
    r.status = ThumbnailRequestStatus::kReady;
    cv::Mat  mat(16, 16, CV_8UC3, cv::Scalar(100, 150, 200));
    r.guard  = std::make_shared<ThumbnailGuard>();
    r.guard->thumbnail_buffer_ = std::make_unique<ImageBuffer>(std::move(mat));
    callback(std::move(r));
  }
  void CancelThumbnail(const ThumbnailCacheKey& /*key*/) override { ++cancel_count_; }
  void ReleaseThumbnail(const ThumbnailCacheKey& /*key*/) override { ++release_count_; }

  auto RequestCount() const -> int { return request_count_.load(); }
  auto ReleaseCount() const -> int { return release_count_.load(); }

 private:
  std::atomic<int> request_count_{0};
  std::atomic<int> cancel_count_{0};
  std::atomic<int> release_count_{0};
};

class FakeImageAnalysisClient : public IImageAnalysisClient {
 public:
  enum class Outcome { kSuccess, kMissingCredential, kUnsupported, kTimeout, kSchemaError };

  void SetDescribeOutcome(Outcome o) { describe_outcome_ = o; }
  void SetBlockMode(bool block) {
    block_mode_ = block;
    release_blocked_ = false;
  }
  void ReleaseBlock() {
    {
      std::unique_lock lk(block_mutex_);
      release_blocked_ = true;
    }
    block_cv_.notify_all();
  }

  auto Ready() -> bool override { return true; }

  auto RegisterCredential(const std::string& provider_id, const std::string& secret,
                          int64_t /*ttl_ms*/, std::chrono::milliseconds /*timeout*/,
                          std::string* handle, std::string* /*error*/) -> bool override {
    ++register_calls_;
    std::unique_lock lk(record_mutex_);
    registered_provider_ = provider_id;
    registered_secret_   = secret;
    if (handle) *handle = "fake-handle";
    return true;
  }

  auto DescribeImage(const ImageAnalysisRequest& request, std::chrono::milliseconds /*timeout*/)
      -> ImageAnalysisUnderstandingResult override {
    ++describe_calls_;
    {
      std::unique_lock lk(record_mutex_);
      last_describe_request_ = request;
    }
    if (block_mode_.load()) {
      std::unique_lock lk(block_mutex_);
      block_cv_.wait(lk, [this] { return release_blocked_.load(); });
    }
    return MakeUnderstanding(request, describe_outcome_);
  }

  auto ScoreImage(const ImageAnalysisRequest& request, std::chrono::milliseconds /*timeout*/)
      -> ImageAnalysisRatingResult override {
    ++score_calls_;
    {
      std::unique_lock lk(record_mutex_);
      last_score_request_ = request;
    }
    if (block_mode_.load()) {
      std::unique_lock lk(block_mutex_);
      block_cv_.wait(lk, [this] { return release_blocked_.load(); });
    }
    return MakeRating(request, describe_outcome_);
  }

  auto CancelTask(const std::string& request_id, std::chrono::milliseconds /*timeout*/,
                  bool* cancelled, std::string* /*error*/) -> bool override {
    ++cancel_calls_;
    std::unique_lock lk(record_mutex_);
    last_cancelled_id_ = request_id;
    if (cancelled) *cancelled = true;
    return true;
  }

  auto WaitForDescribeEntered(std::chrono::milliseconds timeout) -> bool {
    const auto deadline = std::chrono::steady_clock::now() + timeout;
    while (describe_calls_.load() < 1) {
      if (std::chrono::steady_clock::now() >= deadline) {
        return false;
      }
      std::this_thread::sleep_for(std::chrono::milliseconds(5));
    }
    return true;
  }

  auto DescribeCalls() const -> int { return describe_calls_.load(); }
  auto ScoreCalls() const -> int { return score_calls_.load(); }
  auto CancelCalls() const -> int { return cancel_calls_.load(); }
  auto RegisterCalls() const -> int { return register_calls_.load(); }
  auto RegisteredSecret() const -> std::string {
    std::unique_lock lk(record_mutex_);
    return registered_secret_;
  }
  auto LastDescribeRequest() const -> ImageAnalysisRequest {
    std::unique_lock lk(record_mutex_);
    return last_describe_request_;
  }
  auto LastCancelledId() const -> std::string {
    std::unique_lock lk(record_mutex_);
    return last_cancelled_id_;
  }

 private:
  static auto MakeUnderstanding(const ImageAnalysisRequest& req, Outcome o)
      -> ImageAnalysisUnderstandingResult {
    ImageAnalysisUnderstandingResult r;
    r.request_id = req.request_id;
    r.rendition  = req.rendition;
    r.provider   = "fake";
    r.model_id   = req.model_id.empty() ? std::string("fake-model") : req.model_id;
    switch (o) {
      case Outcome::kSuccess:
        r.ok = true;
        r.status = kStatusOk;
        r.caption = "a caption";
        r.tags = {"tag1", "tag2"};
        r.scene = "scene";
        r.confidence = 0.9;
        break;
      case Outcome::kMissingCredential:
        r.ok = false;
        r.status = kStatusUnauthenticated;
        r.error_code = kErrorMissingCredential;
        r.error = "credential missing";
        break;
      case Outcome::kUnsupported:
        r.ok = false;
        r.status = kStatusUnsupportedTask;
        r.error_code = kErrorTaskUnknown;
        r.error = "unsupported provider";
        break;
      case Outcome::kTimeout:
        r.ok = false;
        r.status = kStatusDeadlineExceeded;
        r.error = "timeout";
        break;
      case Outcome::kSchemaError:
        r.ok = false;
        r.status = kStatusProviderError;
        r.error_code = kErrorPayloadDecode;
        r.error = "schema validation failed";
        break;
    }
    return r;
  }

  static auto MakeRating(const ImageAnalysisRequest& req, Outcome o) -> ImageAnalysisRatingResult {
    ImageAnalysisRatingResult r;
    r.request_id = req.request_id;
    r.rendition  = req.rendition;
    r.provider   = "fake";
    r.model_id   = req.model_id.empty() ? std::string("fake-model") : req.model_id;
    if (o == Outcome::kSuccess) {
      r.ok = true;
      r.status = kStatusOk;
      r.scores.push_back({"aesthetic", 0.8});
      r.rubric_id = "alcedo-default-v1";
      r.rubric_version = "1";
      r.confidence = 0.9;
    } else {
      r.ok = false;
      r.status = (o == Outcome::kTimeout) ? kStatusDeadlineExceeded : kStatusProviderError;
      r.error = "failed";
    }
    return r;
  }

  std::atomic<int> describe_calls_{0};
  std::atomic<int> score_calls_{0};
  std::atomic<int> cancel_calls_{0};
  std::atomic<int> register_calls_{0};

  Outcome describe_outcome_ = Outcome::kSuccess;

  std::atomic<bool> block_mode_{false};
  std::atomic<bool> release_blocked_{false};
  std::mutex        block_mutex_;
  std::condition_variable block_cv_;

  mutable std::mutex record_mutex_;
  std::string        registered_provider_;
  std::string        registered_secret_;
  ImageAnalysisRequest last_describe_request_;
  ImageAnalysisRequest last_score_request_;
  std::string        last_cancelled_id_;
};

auto BaseDescribeOpts(const std::string& tag) -> ImageAnalysisOptions {
  ImageAnalysisOptions opts;
  opts.task                = ImageAnalysisTask::kDescribe;
  opts.thumbnail_resolution = ThumbnailResolution::k1024;
  opts.jpeg_quality        = 90;
  opts.timeout             = std::chrono::milliseconds(5000);
  opts.provider_id         = "openrouter";
  opts.temp_dir            = ScratchDir(tag);
  std::filesystem::remove_all(opts.temp_dir);
  std::filesystem::create_directories(opts.temp_dir);
  return opts;
}

auto RunDescribe(std::shared_ptr<IImageAnalysisThumbnailProvider> provider,
                 std::shared_ptr<IImageAnalysisClient>            client,
                 std::shared_ptr<ImageAnalysisInFlightGate>       gate, const std::string& tag)
    -> std::vector<ImageAnalysisItemResult> {
  ImageAnalysisService service(provider, client, gate);
  auto                 opts = BaseDescribeOpts(tag);
  auto                 job  = service.StartAnalysis({ImageAnalysisItem{1, 100}}, opts, {}, {});
  job->Wait();
  return job->Results();
}

TEST(ImageAnalysisServiceTest, DescribeSuccessReturnsAnalyzedResult) {
  auto provider = std::make_shared<FakeThumbnailProvider>();
  auto client   = std::make_shared<FakeImageAnalysisClient>();
  auto results  = RunDescribe(provider, client, nullptr, "success");
  ASSERT_EQ(results.size(), 1u);
  EXPECT_EQ(results[0].status, ImageAnalysisItemStatus::kAnalyzed);
  EXPECT_TRUE(results[0].understanding.ok);
  EXPECT_EQ(results[0].understanding.caption, "a caption");
  EXPECT_EQ(client->DescribeCalls(), 1);
  // Host controls rendition + records it in result metadata.
  EXPECT_EQ(results[0].rendition.width, 16u);
  EXPECT_EQ(results[0].rendition.height, 16u);
  EXPECT_EQ(results[0].rendition.max_edge, 16u);
  EXPECT_EQ(results[0].rendition.kind, "thumbnail");
  EXPECT_EQ(results[0].understanding.rendition.max_edge, 16u);
  std::filesystem::remove_all(ScratchDir("success"));
}

TEST(ImageAnalysisServiceTest, MissingCredentialPropagatesAsError) {
  auto provider = std::make_shared<FakeThumbnailProvider>();
  auto client   = std::make_shared<FakeImageAnalysisClient>();
  client->SetDescribeOutcome(FakeImageAnalysisClient::Outcome::kMissingCredential);
  auto results = RunDescribe(provider, client, nullptr, "missingcred");
  ASSERT_EQ(results.size(), 1u);
  EXPECT_EQ(results[0].status, ImageAnalysisItemStatus::kError);
  EXPECT_FALSE(results[0].understanding.ok);
  EXPECT_EQ(results[0].understanding.status, kStatusUnauthenticated);
  EXPECT_EQ(results[0].understanding.error_code, kErrorMissingCredential);
  std::filesystem::remove_all(ScratchDir("missingcred"));
}

TEST(ImageAnalysisServiceTest, InvalidProviderConfigPropagatesAsError) {
  auto provider = std::make_shared<FakeThumbnailProvider>();
  auto client   = std::make_shared<FakeImageAnalysisClient>();
  client->SetDescribeOutcome(FakeImageAnalysisClient::Outcome::kUnsupported);
  auto results = RunDescribe(provider, client, nullptr, "invalid");
  ASSERT_EQ(results.size(), 1u);
  EXPECT_EQ(results[0].status, ImageAnalysisItemStatus::kError);
  EXPECT_FALSE(results[0].understanding.ok);
  EXPECT_EQ(results[0].understanding.status, kStatusUnsupportedTask);
  EXPECT_EQ(results[0].understanding.error_code, kErrorTaskUnknown);
  std::filesystem::remove_all(ScratchDir("invalid"));
}

TEST(ImageAnalysisServiceTest, TimeoutPropagatesAsError) {
  auto provider = std::make_shared<FakeThumbnailProvider>();
  auto client   = std::make_shared<FakeImageAnalysisClient>();
  client->SetDescribeOutcome(FakeImageAnalysisClient::Outcome::kTimeout);
  auto results = RunDescribe(provider, client, nullptr, "timeout");
  ASSERT_EQ(results.size(), 1u);
  EXPECT_EQ(results[0].status, ImageAnalysisItemStatus::kError);
  EXPECT_EQ(results[0].understanding.status, kStatusDeadlineExceeded);
  std::filesystem::remove_all(ScratchDir("timeout"));
}

TEST(ImageAnalysisServiceTest, SchemaErrorPropagatesAsErrorWithoutActiveResult) {
  auto provider = std::make_shared<FakeThumbnailProvider>();
  auto client   = std::make_shared<FakeImageAnalysisClient>();
  client->SetDescribeOutcome(FakeImageAnalysisClient::Outcome::kSchemaError);
  auto results = RunDescribe(provider, client, nullptr, "schema");
  ASSERT_EQ(results.size(), 1u);
  EXPECT_EQ(results[0].status, ImageAnalysisItemStatus::kError);
  EXPECT_FALSE(results[0].understanding.ok);
  EXPECT_EQ(results[0].understanding.status, kStatusProviderError);
  EXPECT_EQ(results[0].understanding.error_code, kErrorPayloadDecode);
  // No active annotation: caption stays empty on a schema failure.
  EXPECT_TRUE(results[0].understanding.caption.empty());
  std::filesystem::remove_all(ScratchDir("schema"));
}

TEST(ImageAnalysisServiceTest, CancelRunningJobCallsCancelTaskAndDiscardsResult) {
  auto provider = std::make_shared<FakeThumbnailProvider>();
  auto client   = std::make_shared<FakeImageAnalysisClient>();
  client->SetBlockMode(true);
  ImageAnalysisService service(provider, client);
  auto                 opts = BaseDescribeOpts("cancel-running");
  auto                 job  = service.StartAnalysis({ImageAnalysisItem{1, 100}}, opts, {}, {});
  ASSERT_TRUE(client->WaitForDescribeEntered(std::chrono::seconds(2)));
  EXPECT_EQ(client->DescribeCalls(), 1);

  job->Cancel();
  EXPECT_EQ(client->CancelCalls(), 1);
  EXPECT_EQ(client->LastCancelledId(), "image-analysis-describe-1-100");

  // Release the blocked RPC so the worker observes the cancel and finishes.
  client->ReleaseBlock();
  job->Wait();

  auto results = job->Results();
  ASSERT_EQ(results.size(), 1u);
  EXPECT_EQ(results[0].status, ImageAnalysisItemStatus::kCanceled);
  // No extra provider call beyond the one that was in flight.
  EXPECT_EQ(client->DescribeCalls(), 1);
  std::filesystem::remove_all(ScratchDir("cancel-running"));
}

TEST(ImageAnalysisServiceTest, TwoJobsSharingGateRunSerially) {
  auto gate = std::make_shared<ImageAnalysisInFlightGate>();

  auto provider_a = std::make_shared<FakeThumbnailProvider>();
  auto client_a   = std::make_shared<FakeImageAnalysisClient>();
  client_a->SetBlockMode(true);
  ImageAnalysisService service_a(provider_a, client_a, gate);

  auto provider_b = std::make_shared<FakeThumbnailProvider>();
  auto client_b   = std::make_shared<FakeImageAnalysisClient>();
  ImageAnalysisService service_b(provider_b, client_b, gate);

  auto opts = BaseDescribeOpts("queue-serial");
  auto job_a = service_a.StartAnalysis({ImageAnalysisItem{1, 100}}, opts, {}, {});
  ASSERT_TRUE(client_a->WaitForDescribeEntered(std::chrono::seconds(2)));
  EXPECT_EQ(client_a->DescribeCalls(), 1);

  auto job_b = service_b.StartAnalysis({ImageAnalysisItem{2, 200}}, opts, {}, {});
  // B must wait for A to release the gate: its provider call must not start.
  std::this_thread::sleep_for(std::chrono::milliseconds(150));
  EXPECT_EQ(client_b->DescribeCalls(), 0);

  client_a->ReleaseBlock();  // A finishes and releases the slot
  job_a->Wait();
  ASSERT_TRUE(client_b->WaitForDescribeEntered(std::chrono::seconds(2)));
  EXPECT_EQ(client_b->DescribeCalls(), 1);
  job_b->Wait();

  auto rb = job_b->Results();
  ASSERT_EQ(rb.size(), 1u);
  EXPECT_EQ(rb[0].status, ImageAnalysisItemStatus::kAnalyzed);
  auto ra = job_a->Results();
  ASSERT_EQ(ra.size(), 1u);
  EXPECT_EQ(ra[0].status, ImageAnalysisItemStatus::kAnalyzed);
  std::filesystem::remove_all(ScratchDir("queue-serial"));
}

TEST(ImageAnalysisServiceTest, CancelQueuedJobDoesNotStartProviderCall) {
  auto gate = std::make_shared<ImageAnalysisInFlightGate>();

  auto provider_a = std::make_shared<FakeThumbnailProvider>();
  auto client_a   = std::make_shared<FakeImageAnalysisClient>();
  client_a->SetBlockMode(true);
  ImageAnalysisService service_a(provider_a, client_a, gate);

  auto provider_b = std::make_shared<FakeThumbnailProvider>();
  auto client_b   = std::make_shared<FakeImageAnalysisClient>();
  ImageAnalysisService service_b(provider_b, client_b, gate);

  auto opts = BaseDescribeOpts("cancel-queued");
  auto job_a = service_a.StartAnalysis({ImageAnalysisItem{1, 100}}, opts, {}, {});
  ASSERT_TRUE(client_a->WaitForDescribeEntered(std::chrono::seconds(2)));

  auto job_b = service_b.StartAnalysis({ImageAnalysisItem{2, 200}}, opts, {}, {});
  std::this_thread::sleep_for(std::chrono::milliseconds(150));
  EXPECT_EQ(client_b->DescribeCalls(), 0);  // B is queued behind A

  job_b->Cancel();
  client_a->ReleaseBlock();  // A finishes; B should skip its RPC entirely
  job_a->Wait();
  job_b->Wait();

  EXPECT_EQ(client_a->DescribeCalls(), 1);
  EXPECT_EQ(client_b->DescribeCalls(), 0);  // canceled queued job -> no provider call
  auto rb = job_b->Results();
  ASSERT_EQ(rb.size(), 1u);
  EXPECT_EQ(rb[0].status, ImageAnalysisItemStatus::kCanceled);
  std::filesystem::remove_all(ScratchDir("cancel-queued"));
}

TEST(ImageAnalysisServiceTest, SecretReachesOnlyRegisterCredentialNotDescribeImage) {
  auto provider = std::make_shared<FakeThumbnailProvider>();
  auto client   = std::make_shared<FakeImageAnalysisClient>();
  ImageAnalysisService service(provider, client);
  auto                 opts = BaseDescribeOpts("noleak");
  opts.credential.provider_id = "openrouter";
  opts.credential.secret      = "sk-DO-NOT-LEAK-SENTINEL-7c3f9a1e";

  auto job = service.StartAnalysis({ImageAnalysisItem{1, 100}}, opts, {}, {});
  job->Wait();
  auto results = job->Results();
  ASSERT_EQ(results.size(), 1u);
  EXPECT_EQ(results[0].status, ImageAnalysisItemStatus::kAnalyzed);

  // The secret traveled only to the loopback RegisterCredential call.
  EXPECT_EQ(client->RegisterCalls(), 1);
  EXPECT_EQ(client->RegisteredSecret(), "sk-DO-NOT-LEAK-SENTINEL-7c3f9a1e");
  // Only the opaque handle (not the secret) reached DescribeImage.
  const auto req = client->LastDescribeRequest();
  EXPECT_EQ(req.credential_ref, "fake-handle");
  EXPECT_EQ(req.credential_ref.find("sk-DO-NOT-LEAK"), std::string::npos);
  EXPECT_EQ(req.image_format_hint.find("sk-DO-NOT-LEAK"), std::string::npos);
  // The result must not carry the secret.
  EXPECT_EQ(results[0].understanding.error.find("sk-DO-NOT-LEAK"), std::string::npos);
  EXPECT_EQ(results[0].understanding.caption.find("sk-DO-NOT-LEAK"), std::string::npos);
  std::filesystem::remove_all(ScratchDir("noleak"));
}

}  // namespace
}  // namespace alcedo
