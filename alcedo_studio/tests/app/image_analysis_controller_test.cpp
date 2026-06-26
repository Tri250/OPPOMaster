//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

// Phase 6d — album image-analysis job controller. Drives ImageAnalysisController
// with fakes (IImageAnalysisEnvironment + AiProviderPresetController on temp
// QSettings) so the 6d-required cases (empty selection, one/multi success, cancel,
// retry, provider error, schema error, missing credential, score, shared-gate
// serialization) run without a live project or sidecar.

#include "ui/alcedo_main/album_backend/image_analysis_controller.hpp"

#include <gtest/gtest.h>

#include <QCoreApplication>
#include <QLatin1String>
#include <QSettings>
#include <QString>
#include <QVariantList>
#include <QVariantMap>

#include <atomic>
#include <chrono>
#include <condition_variable>
#include <functional>
#include <memory>
#include <mutex>
#include <string>
#include <thread>
#include <unordered_set>
#include <utility>
#include <vector>

#include <opencv2/core.hpp>

#include "app/ai_provider_preset.hpp"
#include "app/image_analysis_service.hpp"
#include "image/image_buffer.hpp"

namespace alcedo::ui {
namespace {

constexpr int kStatusOk               = 1;
constexpr int kStatusUnauthenticated  = 6;
constexpr int kStatusProviderError    = 9;
constexpr int kStatusUnsupportedTask  = 10;
constexpr int kErrorMissingCredential = 2;
constexpr int kErrorTaskUnknown       = 9;
constexpr int kErrorPayloadDecode     = 10;

// Synchronous thumbnail provider: returns a ready guard holding a small CPU mat so
// the encoder produces real JPEG bytes. No ThumbnailService / pipeline required.
class FakeThumbProvider : public alcedo::IImageAnalysisThumbnailProvider {
 public:
  void RequestThumbnail(const alcedo::ImageAnalysisItem& item,
                        alcedo::ThumbnailResolution             resolution,
                        alcedo::ImageAnalysisThumbnailCallback  callback) override {
    ++request_count_;
    alcedo::ThumbnailRequestResult r;
    r.key    = alcedo::ThumbnailCacheKey{item.element_id, resolution};
    r.status = alcedo::ThumbnailRequestStatus::kReady;
    cv::Mat  mat(16, 16, CV_8UC3, cv::Scalar(100, 150, 200));
    r.guard  = std::make_shared<alcedo::ThumbnailGuard>();
    r.guard->thumbnail_buffer_ = std::make_unique<alcedo::ImageBuffer>(std::move(mat));
    callback(std::move(r));
  }
  void CancelThumbnail(const alcedo::ThumbnailCacheKey&) override {}
  void ReleaseThumbnail(const alcedo::ThumbnailCacheKey&) override { ++release_count_; }
  auto RequestCount() const -> int { return request_count_.load(); }
  auto ReleaseCount() const -> int { return release_count_.load(); }

 private:
  std::atomic<int> request_count_{0};
  std::atomic<int> release_count_{0};
};

class FakeClient : public alcedo::IImageAnalysisClient {
 public:
  enum class Outcome { kSuccess, kMissingCredential, kUnsupported, kSchemaError };

  void SetDescribeOutcome(Outcome o) { describe_outcome_ = o; }
  void SetScoreOutcome(Outcome o) { score_outcome_ = o; }
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
  auto RegisterCredential(const std::string&, const std::string&, int64_t,
                          std::chrono::milliseconds, std::string* handle,
                          std::string*) -> bool override {
    ++register_calls_;
    if (handle) {
      *handle = "fake-handle";
    }
    return true;
  }
  auto RevokeCredential(const std::string&, std::chrono::milliseconds, bool*,
                        std::string*) -> bool override {
    ++revoke_calls_;
    return true;
  }
  auto DescribeImage(const alcedo::ImageAnalysisRequest& req, std::chrono::milliseconds)
      -> alcedo::ImageAnalysisUnderstandingResult override {
    ++describe_calls_;
    if (block_mode_.load()) {
      std::unique_lock lk(block_mutex_);
      block_cv_.wait(lk, [this] { return release_blocked_.load(); });
    }
    return MakeUnderstanding(req, describe_outcome_);
  }
  auto ScoreImage(const alcedo::ImageAnalysisRequest& req, std::chrono::milliseconds)
      -> alcedo::ImageAnalysisRatingResult override {
    ++score_calls_;
    if (block_mode_.load()) {
      std::unique_lock lk(block_mutex_);
      block_cv_.wait(lk, [this] { return release_blocked_.load(); });
    }
    return MakeRating(req, score_outcome_);
  }
  auto CancelTask(const std::string&, std::chrono::milliseconds, bool* cancelled,
                  std::string*) -> bool override {
    ++cancel_calls_;
    if (cancelled) {
      *cancelled = true;
    }
    return true;
  }
  auto ListModels(const std::string&, const std::string&, std::chrono::milliseconds)
      -> alcedo::ImageAnalysisListModelsResult override {
    ++list_models_calls_;
    alcedo::ImageAnalysisListModelsResult r;
    r.ok     = true;
    r.status = kStatusOk;
    r.models.push_back(alcedo::AiDiscoveredModel{.model_id = "fake-model"});
    return r;
  }

  auto DescribeCalls() const -> int { return describe_calls_.load(); }
  auto ScoreCalls() const -> int { return score_calls_.load(); }
  auto CancelCalls() const -> int { return cancel_calls_.load(); }
  auto RegisterCalls() const -> int { return register_calls_.load(); }
  auto ListModelsCalls() const -> int { return list_models_calls_.load(); }

 private:
  static auto MakeUnderstanding(const alcedo::ImageAnalysisRequest& req, Outcome o)
      -> alcedo::ImageAnalysisUnderstandingResult {
    alcedo::ImageAnalysisUnderstandingResult r;
    r.request_id = req.request_id;
    r.rendition  = req.rendition;
    r.provider   = "fake";
    r.model_id   = req.model_id.empty() ? std::string("fake-model") : req.model_id;
    if (o == Outcome::kSuccess) {
      r.ok = true;
      r.status = kStatusOk;
      r.caption = "a caption";
      r.tags    = {"tag1", "tag2"};
      r.scene   = "scene";
      r.confidence = 0.9;
    } else if (o == Outcome::kMissingCredential) {
      r.ok = false;
      r.status = kStatusUnauthenticated;
      r.error_code = kErrorMissingCredential;
      r.error = "credential missing";
    } else if (o == Outcome::kUnsupported) {
      r.ok = false;
      r.status = kStatusUnsupportedTask;
      r.error_code = kErrorTaskUnknown;
      r.error = "unsupported provider";
    } else {
      r.ok = false;
      r.status = kStatusProviderError;
      r.error_code = kErrorPayloadDecode;
      r.error = "schema validation failed";
    }
    return r;
  }
  static auto MakeRating(const alcedo::ImageAnalysisRequest& req, Outcome o)
      -> alcedo::ImageAnalysisRatingResult {
    alcedo::ImageAnalysisRatingResult r;
    r.request_id = req.request_id;
    r.rendition  = req.rendition;
    r.provider   = "fake";
    r.model_id   = req.model_id.empty() ? std::string("fake-model") : req.model_id;
    if (o == Outcome::kSuccess) {
      r.ok = true;
      r.status = kStatusOk;
      r.rating = 4;
      r.rubric_id = "general";
      r.rubric_version = "1.0";
    } else {
      r.ok = false;
      r.status = kStatusProviderError;
      r.error = "failed";
    }
    return r;
  }

  std::atomic<int> describe_calls_{0};
  std::atomic<int> score_calls_{0};
  std::atomic<int> cancel_calls_{0};
  std::atomic<int> register_calls_{0};
  std::atomic<int> list_models_calls_{0};
  std::atomic<int> revoke_calls_{0};
  Outcome describe_outcome_ = Outcome::kSuccess;
  Outcome score_outcome_    = Outcome::kSuccess;
  std::atomic<bool> block_mode_{false};
  std::atomic<bool> release_blocked_{false};
  std::mutex        block_mutex_;
  std::condition_variable block_cv_;
};

class CountingCredentialStore final : public alcedo::IAiCredentialStore {
 public:
  auto SaveCredential(const std::string& slot, const std::string& secret,
                      std::string* error) -> bool override {
    return inner_.SaveCredential(slot, secret, error);
  }
  auto LoadCredential(const std::string& slot, std::string* secret,
                      std::string* error) -> bool override {
    ++load_calls_;
    return inner_.LoadCredential(slot, secret, error);
  }
  auto DeleteCredential(const std::string& slot, std::string* error) -> bool override {
    return inner_.DeleteCredential(slot, error);
  }
  auto HasCredential(const std::string& slot) -> bool override {
    ++has_calls_;
    return inner_.HasCredential(slot);
  }

  auto LoadCalls() const -> int { return load_calls_.load(); }
  auto HasCalls() const -> int { return has_calls_.load(); }

 private:
  alcedo::InMemoryAiCredentialStore inner_;
  std::atomic<int> load_calls_{0};
  std::atomic<int> has_calls_{0};
};
class FakeEnv : public IImageAnalysisEnvironment {
 public:
  FakeEnv(std::shared_ptr<FakeThumbProvider> thumbs,
          std::shared_ptr<FakeClient>        client,
          std::shared_ptr<alcedo::ImageAnalysisInFlightGate> gate,
          std::shared_ptr<alcedo::IAiCredentialStore>        store)
      : thumbs_(std::move(thumbs)),
        client_(std::move(client)),
        gate_(std::move(gate)),
        store_(std::move(store)) {}

  auto ThumbnailProvider() -> std::shared_ptr<alcedo::IImageAnalysisThumbnailProvider> override {
    return thumbs_;
  }
  auto AnalysisClient() -> std::shared_ptr<alcedo::IImageAnalysisClient> override { return client_; }
  auto CredentialStore() -> std::shared_ptr<alcedo::IAiCredentialStore> override { return store_; }
  auto Gate() -> std::shared_ptr<alcedo::ImageAnalysisInFlightGate> override { return gate_; }
  auto EnsureSidecarReady(std::string*) -> bool override {
    sidecar_ensured_ = true;
    return sidecar_ready_;
  }
  void SetSidecarReady(bool r) { sidecar_ready_ = r; }
  auto SidecarEnsured() const -> bool { return sidecar_ensured_.load(); }

 private:
  std::shared_ptr<FakeThumbProvider>                  thumbs_;
  std::shared_ptr<FakeClient>                         client_;
  std::shared_ptr<alcedo::ImageAnalysisInFlightGate>  gate_;
  std::shared_ptr<alcedo::IAiCredentialStore>         store_;
  std::atomic<bool> sidecar_ensured_{false};
  bool              sidecar_ready_ = true;
};

auto Targets(std::vector<std::pair<uint, uint>> ids) -> QVariantList {
  QVariantList out;
  for (auto [eid, iid] : ids) {
    QVariantMap m;
    m.insert("elementId", eid);
    m.insert("imageId", iid);
    out.push_back(m);
  }
  return out;
}

// Pump the event loop until the controller is no longer running (the worker's
// on_finished is delivered to the main thread via Qt::QueuedConnection) or the
// timeout elapses. Returns true if the controller finished.
auto WaitForFinished(ImageAnalysisController& controller, std::chrono::milliseconds timeout)
    -> bool {
  const auto deadline = std::chrono::steady_clock::now() + timeout;
  while (controller.Running()) {
    if (std::chrono::steady_clock::now() >= deadline) {
      return false;
    }
    QCoreApplication::processEvents();
    std::this_thread::sleep_for(std::chrono::milliseconds(5));
  }
  QCoreApplication::processEvents();
  return true;
}

auto SpinWaitFor(std::function<bool()> pred, std::chrono::milliseconds timeout) -> bool {
  const auto deadline = std::chrono::steady_clock::now() + timeout;
  while (std::chrono::steady_clock::now() < deadline) {
    QCoreApplication::processEvents();
    if (pred()) {
      return true;
    }
    std::this_thread::sleep_for(std::chrono::milliseconds(5));
  }
  QCoreApplication::processEvents();
  return pred();
}

struct EnvBundle {
  std::shared_ptr<FakeThumbProvider>                 thumbs = std::make_shared<FakeThumbProvider>();
  std::shared_ptr<FakeClient>                        client = std::make_shared<FakeClient>();
  std::shared_ptr<alcedo::ImageAnalysisInFlightGate> gate   = std::make_shared<
      alcedo::ImageAnalysisInFlightGate>();
  std::shared_ptr<CountingCredentialStore> store = std::make_shared<CountingCredentialStore>();
  std::shared_ptr<FakeEnv>               env;
  alcedo::AiProviderPresetController     preset;  // outlives the controller(s)

  EnvBundle() {
    store->SaveCredential("opencode_api_key", "sk-fake-test-key", nullptr);
    env = std::make_shared<FakeEnv>(thumbs, client, gate, store);
    alcedo::AiProviderPreset p;
    p.provider_id              = QStringLiteral("opencode_go_anthropic");
    p.protocol_family          = QStringLiteral("anthropic_messages");
    p.base_url                 = QStringLiteral("https://opencode.ai/zen/go/v1");
    p.endpoint                 = QStringLiteral("/messages");
    p.auth_type                = QStringLiteral("bearer");
    p.credential_slot          = QStringLiteral("opencode_api_key");
    p.model_id                 = QStringLiteral("claude-sonnet-4-5");
    p.structured_output_mode   = QStringLiteral("tool");
    p.timeout_ms               = 5000;
    p.max_image_bytes          = 4 * 1024 * 1024;
    preset.SetFromPreset(p);
  }
};

class ImageAnalysisControllerTest : public ::testing::Test {
 protected:
  static void SetUpTestSuite() {
    static int    argc = 0;
    static char** argv = nullptr;
    if (app_ == nullptr) {
      app_ = new QCoreApplication(argc, argv);
    }
    QCoreApplication::setOrganizationName(QStringLiteral("PuerhLabTest"));
    QCoreApplication::setApplicationName(QStringLiteral("ImageAnalysisControllerTest"));
    QSettings().remove(QLatin1String("ai/preset"));
  }
  static void TearDownTestSuite() { QSettings().remove(QLatin1String("ai/preset")); }

  // A fresh controller + env per case. The preset lives in the bundle (kept
  // alive in bundles_) so the controller's raw pointer stays valid.
  auto MakeController() -> std::unique_ptr<ImageAnalysisController> {
    auto bundle = std::make_shared<EnvBundle>();
    bundles_.push_back(bundle);
    last_bundle_ = bundle;
    return std::make_unique<ImageAnalysisController>(bundle->env, &bundle->preset);
  }

  static QCoreApplication*                              app_;
  std::vector<std::shared_ptr<EnvBundle>>               bundles_;
  std::shared_ptr<EnvBundle>                            last_bundle_;
};

QCoreApplication* ImageAnalysisControllerTest::app_ = nullptr;

// ── Required 6d cases ────────────────────────────────────────────────────────

TEST_F(ImageAnalysisControllerTest, EmptySelectionSetsErrorAndDoesNotStart) {
  auto controller = MakeController();
  controller->StartDescribeForTargets(QVariantList{});
  EXPECT_FALSE(controller->Running());
  EXPECT_FALSE(controller->LastError().isEmpty());
  EXPECT_EQ(last_bundle_->client->DescribeCalls(), 0);
  EXPECT_FALSE(last_bundle_->env->SidecarEnsured());
}

TEST_F(ImageAnalysisControllerTest, RefreshCredentialStateUsesHasCredentialWithoutLoadingSecret) {
  auto controller = MakeController();
  EXPECT_TRUE(controller->CredentialAvailable());
  EXPECT_EQ(last_bundle_->store->LoadCalls(), 0);
  EXPECT_GE(last_bundle_->store->HasCalls(), 1);

  controller->RefreshCredentialState();
  EXPECT_TRUE(controller->CredentialAvailable());
  EXPECT_EQ(last_bundle_->store->LoadCalls(), 0);
  EXPECT_GE(last_bundle_->store->HasCalls(), 2);
}

TEST_F(ImageAnalysisControllerTest, ErrorClearsPreviousResultsAndCounts) {
  auto controller = MakeController();
  controller->StartDescribeForTargets(Targets({{1, 100}}));
  ASSERT_TRUE(WaitForFinished(*controller, std::chrono::seconds(10)));
  ASSERT_EQ(controller->Analyzed(), 1);
  ASSERT_EQ(controller->LastResults().size(), 1);

  controller->StartDescribeForTargets(QVariantList{});
  EXPECT_FALSE(controller->Running());
  EXPECT_FALSE(controller->LastError().isEmpty());
  EXPECT_EQ(controller->Total(), 0);
  EXPECT_EQ(controller->Analyzed(), 0);
  EXPECT_EQ(controller->Failed(), 0);
  EXPECT_EQ(controller->Canceled(), 0);
  EXPECT_FALSE(controller->CanRetry());
  EXPECT_TRUE(controller->LastResults().isEmpty());
}

TEST_F(ImageAnalysisControllerTest, ValidateConnectionMissingCredentialDoesNotStartSidecar) {
  auto controller = MakeController();
  ASSERT_TRUE(last_bundle_->store->DeleteCredential("opencode_api_key", nullptr));

  controller->ValidateConnection();
  EXPECT_FALSE(controller->Running());
  EXPECT_FALSE(controller->LastError().isEmpty());
  EXPECT_FALSE(controller->CredentialAvailable());
  EXPECT_FALSE(last_bundle_->env->SidecarEnsured());
  EXPECT_EQ(last_bundle_->client->RegisterCalls(), 0);
  EXPECT_EQ(last_bundle_->client->ListModelsCalls(), 0);
}
TEST_F(ImageAnalysisControllerTest, OneImageDescribeSucceeds) {
  auto controller = MakeController();
  controller->StartDescribeForTargets(Targets({{1, 100}}));
  ASSERT_TRUE(WaitForFinished(*controller, std::chrono::seconds(10)));
  EXPECT_EQ(controller->Analyzed(), 1);
  EXPECT_EQ(controller->Failed(), 0);
  EXPECT_EQ(last_bundle_->client->DescribeCalls(), 1);
  ASSERT_EQ(controller->LastResults().size(), 1);
  const auto r = controller->LastResults().front().toMap();
  EXPECT_EQ(r.value("status").toString(), QStringLiteral("analyzed"));
  EXPECT_EQ(r.value("caption").toString(), QStringLiteral("a caption"));
}

TEST_F(ImageAnalysisControllerTest, MultiImageDescribeSucceeds) {
  auto controller = MakeController();
  controller->StartDescribeForTargets(Targets({{1, 100}, {2, 200}, {3, 300}}));
  ASSERT_TRUE(WaitForFinished(*controller, std::chrono::seconds(15)));
  EXPECT_EQ(controller->Analyzed(), 3);
  EXPECT_EQ(controller->Failed(), 0);
  EXPECT_EQ(last_bundle_->client->DescribeCalls(), 3);
}

TEST_F(ImageAnalysisControllerTest, CancelRunningAnalysisDiscardsResult) {
  auto controller = MakeController();
  last_bundle_->client->SetBlockMode(true);
  controller->StartDescribeForTargets(Targets({{1, 100}}));
  // Wait until the provider call has started (the item holds the gate).
  ASSERT_TRUE(SpinWaitFor([&] { return last_bundle_->client->DescribeCalls() >= 1; },
                          std::chrono::seconds(5)));
  controller->CancelAnalysis();
  last_bundle_->client->ReleaseBlock();
  ASSERT_TRUE(WaitForFinished(*controller, std::chrono::seconds(10)));
  EXPECT_EQ(controller->Analyzed(), 0);
  EXPECT_GE(controller->Canceled(), 1);
}

TEST_F(ImageAnalysisControllerTest, RetryLastReRunsTargets) {
  auto controller = MakeController();
  // First run: schema error -> failed, canRetry true.
  last_bundle_->client->SetDescribeOutcome(FakeClient::Outcome::kSchemaError);
  controller->StartDescribeForTargets(Targets({{1, 100}}));
  ASSERT_TRUE(WaitForFinished(*controller, std::chrono::seconds(10)));
  EXPECT_EQ(controller->Analyzed(), 0);
  EXPECT_EQ(controller->Failed(), 1);
  EXPECT_TRUE(controller->CanRetry());

  // Retry: switch to success and re-run the same targets.
  last_bundle_->client->SetDescribeOutcome(FakeClient::Outcome::kSuccess);
  controller->RetryLast();
  ASSERT_TRUE(WaitForFinished(*controller, std::chrono::seconds(10)));
  EXPECT_EQ(controller->Analyzed(), 1);
  EXPECT_EQ(controller->Failed(), 0);
}

TEST_F(ImageAnalysisControllerTest, ProviderErrorPropagatesAndNoActiveAnnotation) {
  auto controller = MakeController();
  last_bundle_->client->SetDescribeOutcome(FakeClient::Outcome::kUnsupported);
  controller->StartDescribeForTargets(Targets({{1, 100}}));
  ASSERT_TRUE(WaitForFinished(*controller, std::chrono::seconds(10)));
  EXPECT_EQ(controller->Analyzed(), 0);
  EXPECT_EQ(controller->Failed(), 1);
  ASSERT_EQ(controller->LastResults().size(), 1);
  EXPECT_EQ(controller->LastResults().front().toMap().value("status").toString(),
            QStringLiteral("error"));
}

TEST_F(ImageAnalysisControllerTest, SchemaErrorPropagatesAndNoActiveAnnotation) {
  auto controller = MakeController();
  last_bundle_->client->SetDescribeOutcome(FakeClient::Outcome::kSchemaError);
  controller->StartDescribeForTargets(Targets({{1, 100}}));
  ASSERT_TRUE(WaitForFinished(*controller, std::chrono::seconds(10)));
  EXPECT_EQ(controller->Analyzed(), 0);
  EXPECT_EQ(controller->Failed(), 1);
  EXPECT_EQ(controller->LastResults().front().toMap().value("status").toString(),
            QStringLiteral("error"));
}

TEST_F(ImageAnalysisControllerTest, MissingCredentialSetsErrorAndDoesNotStart) {
  auto controller = MakeController();
  // Wipe the credential so LoadCredential fails before the sidecar is touched.
  ASSERT_TRUE(
      last_bundle_->store->DeleteCredential("opencode_api_key", nullptr));
  controller->StartDescribeForTargets(Targets({{1, 100}}));
  EXPECT_FALSE(controller->Running());
  EXPECT_FALSE(controller->LastError().isEmpty());
  EXPECT_FALSE(controller->CredentialAvailable());
  EXPECT_FALSE(last_bundle_->env->SidecarEnsured());
  EXPECT_EQ(last_bundle_->client->DescribeCalls(), 0);
}

TEST_F(ImageAnalysisControllerTest, ScoreTaskReturnsRating) {
  auto controller = MakeController();
  controller->StartScoreForTargets(Targets({{1, 100}}));
  ASSERT_TRUE(WaitForFinished(*controller, std::chrono::seconds(10)));
  EXPECT_EQ(controller->Analyzed(), 1);
  EXPECT_EQ(last_bundle_->client->ScoreCalls(), 1);
  ASSERT_EQ(controller->LastResults().size(), 1);
  const auto r = controller->LastResults().front().toMap();
  EXPECT_EQ(r.value("status").toString(), QStringLiteral("analyzed"));
  const int rating = r.value("rating").toInt();
  EXPECT_GE(rating, 1);
  EXPECT_LE(rating, 5);
}

TEST_F(ImageAnalysisControllerTest, SharedGateSerializesTwoConcurrentRuns) {
  // Two controllers over ONE shared env (shared gate + shared fake client). The
  // Phase 6d mandate: remote calls serialize app-wide, not per service instance.
  auto bundle = std::make_shared<EnvBundle>();
  bundles_.push_back(bundle);

  ImageAnalysisController a(bundle->env, &bundle->preset);
  ImageAnalysisController b(bundle->env, &bundle->preset);

  bundle->client->SetBlockMode(true);
  a.StartDescribeForTargets(Targets({{1, 100}}));
  // Wait until A's provider call is in flight (A holds the shared gate).
  ASSERT_TRUE(SpinWaitFor([&] { return bundle->client->DescribeCalls() >= 1; },
                          std::chrono::seconds(5)));

  b.StartDescribeForTargets(Targets({{2, 200}}));
  // B must NOT reach the provider while A holds the gate.
  std::this_thread::sleep_for(std::chrono::milliseconds(100));
  QCoreApplication::processEvents();
  EXPECT_EQ(bundle->client->DescribeCalls(), 1);

  // Release A; B may then acquire the slot and run.
  bundle->client->ReleaseBlock();
  ASSERT_TRUE(WaitForFinished(a, std::chrono::seconds(10)));
  ASSERT_TRUE(WaitForFinished(b, std::chrono::seconds(10)));
  EXPECT_EQ(a.Analyzed(), 1);
  EXPECT_EQ(b.Analyzed(), 1);
  EXPECT_EQ(bundle->client->DescribeCalls(), 2);
}

}  // namespace
}  // namespace alcedo::ui
