//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/ai_sidecar_runtime_service.hpp"

#include <gtest/gtest.h>

#include <QThread>
#include <atomic>
#include <chrono>
#include <cstdlib>
#include <filesystem>
#include <fstream>
#include <memory>
#include <optional>
#include <string>
#include <thread>
#include <utility>
#include <vector>

#include "app/project_service.hpp"
#include "sidecar_client/dto/image_analysis.hpp"
#include "sidecar_client/dto/model_manager.hpp"
#include "sidecar_client/dto/semantic_embedding.hpp"

namespace alcedo {
namespace {

struct FakeSidecarState {
  std::atomic<bool> ready{true};
  std::atomic<bool> model_info_ready{true};
  std::atomic<int>  ping_count{0};
  std::atomic<int>  embed_text_calls{0};
  std::atomic<int>  embed_image_batch_calls{0};
  std::atomic<int>  describe_image_calls{0};
  std::atomic<int>  score_image_calls{0};
  std::atomic<int>  list_models_calls{0};
  std::atomic<int>  revoke_calls{0};
  std::string       last_revoked_handle;
  std::vector<AiDiscoveredModel> list_models_canned;
};

class FakeRuntimeControlClient final : public sidecar_client::RuntimeControlClient {
 public:
  explicit FakeRuntimeControlClient(std::shared_ptr<FakeSidecarState> state)
      : state_(std::move(state)) {}

  auto Ping(std::chrono::milliseconds timeout, std::string* error) -> bool override {
    (void)timeout;
    state_->ping_count.fetch_add(1);
    if (!state_->ready.load()) {
      if (error) *error = "fake runtime is not ready";
      return false;
    }
    return true;
  }

  auto GetRuntimeStatus(std::chrono::milliseconds timeout,
                        AiSidecarRuntimeRemoteStatus* status, std::string* error)
      -> bool override {
    (void)timeout;
    (void)error;
    if (status) {
      status->state               = "ready";
      status->provider            = "fake";
      status->image_batch_cap     = 8;
      status->image_batch_wait_ms = 2;
      status->uptime_ms           = 42;
    }
    return true;
  }

  auto ListCapabilities(std::chrono::milliseconds timeout,
                        std::vector<AiSidecarCapability>* capabilities,
                        std::string* error) -> bool override {
    (void)timeout;
    if (!state_->ready.load()) {
      if (error) *error = "fake runtime is not ready";
      return false;
    }
    if (capabilities) {
      capabilities->clear();
      capabilities->push_back(AiSidecarCapability{
          .task_id             = "semantic.embed_*",
          .provider_id         = "local",
          .model_id            = "test/mobileclip",
          .input_kinds         = {2, 3},
          .output_kinds        = {1},
          .supports_batch      = true,
          .supports_cancel     = true,
          .requires_credential = false,
          .max_payload_bytes   = 0,
      });
    }
    return true;
  }

  auto CancelTask(const std::string& request_id, std::chrono::milliseconds timeout,
                  bool* cancelled, std::string* error) -> bool override {
    (void)timeout;
    (void)error;
    if (!state_->ready.load()) {
      if (error) *error = "fake runtime is not ready";
      return false;
    }
    if (cancelled) *cancelled = request_id == "fake-in-flight";
    return true;
  }

 private:
  std::shared_ptr<FakeSidecarState> state_;
};

class FakeCredentialClient final : public sidecar_client::CredentialClient {
 public:
  explicit FakeCredentialClient(std::shared_ptr<FakeSidecarState> state)
      : state_(std::move(state)) {}

  auto RegisterCredential(const std::string& provider_id, const std::string& secret,
                          int64_t ttl_ms, std::chrono::milliseconds timeout,
                          std::string* handle, std::string* error) -> bool override {
    (void)provider_id;
    (void)secret;
    (void)ttl_ms;
    (void)timeout;
    (void)error;
    if (!state_->ready.load()) {
      if (error) *error = "fake runtime is not ready";
      return false;
    }
    if (handle) *handle = "fake-credential-handle";
    return true;
  }

  auto RevokeCredential(const std::string& handle, std::chrono::milliseconds timeout,
                        bool* revoked, std::string* error) -> bool override {
    (void)timeout;
    (void)error;
    state_->revoke_calls.fetch_add(1);
    state_->last_revoked_handle = handle;
    if (revoked) *revoked = handle == "fake-credential-handle";
    return true;
  }

 private:
  std::shared_ptr<FakeSidecarState> state_;
};

class FakeModelManagerClient final : public sidecar_client::ModelManagerClient {
 public:
  auto ListModelProfiles(const std::string& model_root, std::chrono::milliseconds timeout,
                         std::string* error) -> std::vector<SemanticModelProfileInfo> override {
    (void)timeout;
    (void)error;
    SemanticModelProfileInfo mobile;
    mobile.profile_id          = "mobileclip2-s2-en";
    mobile.model_id            = "plhery/mobileclip2-onnx:s2";
    mobile.language            = "en";
    mobile.embedding_dimension = 512;
    mobile.local_root          = model_root + "/mobileclip2-s2-en";

    SemanticModelProfileInfo multilingual;
    multilingual.profile_id                 = "jina-clip-v2-int8-multilingual";
    multilingual.model_id                   = "jinaai/jina-clip-v2";
    multilingual.language                   = "multilingual";
    multilingual.embedding_dimension        = 512;
    multilingual.native_embedding_dimension = 1024;
    multilingual.embedding_transform        = "matryoshka_truncate_then_l2_normalize";
    multilingual.local_root                 = model_root + "/jina-clip-v2-int8-multilingual";
    return {mobile, multilingual};
  }

  auto ListInstalledModels(const std::string& model_root, std::chrono::milliseconds timeout,
                           std::string* error) -> std::vector<SemanticModelProfileInfo> override {
    auto profiles = ListModelProfiles(model_root, timeout, error);
    profiles.resize(1);
    profiles[0].installed = true;
    return profiles;
  }

  auto ValidateModel(const std::string& profile_id, const std::string& model_root,
                     std::chrono::milliseconds timeout) -> SemanticModelManagerResult override {
    (void)timeout;
    SemanticModelManagerResult result;
    result.ok                          = true;
    result.status                      = "installed";
    result.profile.profile_id          = profile_id;
    result.profile.local_root          = model_root + "/" + profile_id;
    result.profile.embedding_dimension = 512;
    result.manifest                    = SemanticResolvedModelManifest{
                           .profile_id                 = profile_id,
                           .model_id                   = "test/mobileclip",
                           .revision                   = "rev-a",
                           .engine_profile_id          = "mobileclip2-openclip",
                           .language                   = "en",
                           .embedding_dimension        = 512,
                           .native_embedding_dimension = 512,
                           .image_size                 = 256,
                           .embedding_transform        = "l2_normalize",
                           .model_root                 = result.profile.local_root,
                           .assets                     = {},
    };
    return result;
  }

  auto DeleteModel(const std::string& profile_id, const std::string& model_root,
                   std::chrono::milliseconds timeout) -> SemanticModelManagerResult override {
    (void)model_root;
    (void)timeout;
    SemanticModelManagerResult result;
    result.ok                 = true;
    result.status             = "deleted";
    result.profile.profile_id = profile_id;
    return result;
  }
};

class FakeSemanticEmbeddingClient final : public sidecar_client::SemanticEmbeddingClient {
 public:
  explicit FakeSemanticEmbeddingClient(std::shared_ptr<FakeSidecarState> state)
      : state_(std::move(state)) {}

  auto GetModelInfo(std::chrono::milliseconds timeout, AiSidecarRuntimeModelInfo* info,
                    std::string* error) -> bool override {
    (void)timeout;
    if (!state_->model_info_ready.load()) {
      if (error) *error = "fake semantic model is unavailable";
      return false;
    }
    if (info) {
      info->profile_id                 = "mobileclip2-s2-en";
      info->model_id                   = "test/mobileclip";
      info->revision                   = "rev-a";
      info->engine_profile_id          = "mobileclip2-openclip";
      info->language                   = "en";
      info->embedding_dimension        = 512;
      info->native_embedding_dimension = 512;
      info->image_size                 = 256;
      info->embedding_transform        = "l2_normalize";
      info->provider                   = "fake";
      info->model_root                 = "test-model-root";
      info->prototype_config_hash      = "hash-a";
    }
    return true;
  }

  auto EmbedText(const std::string& request_id, const std::string& text,
                 std::chrono::milliseconds timeout) -> SemanticEmbeddingResult override {
    (void)text;
    (void)timeout;
    state_->embed_text_calls.fetch_add(1);
    return {.request_id = request_id,
            .embedding  = {1.0f, 0.0f},
            .dimension  = 2,
            .model_name = "test/mobileclip",
            .ok         = true};
  }

  auto EmbedTextBatch(const std::vector<SemanticTextEmbeddingRequest>& requests,
                      std::chrono::milliseconds timeout)
      -> std::vector<SemanticEmbeddingResult> override {
    (void)timeout;
    std::vector<SemanticEmbeddingResult> results;
    results.reserve(requests.size());
    for (const auto& request : requests) {
      results.push_back(EmbedText(request.request_id, request.text, timeout));
    }
    return results;
  }

  auto EmbedImage(const std::string& request_id, const std::vector<uint8_t>& rgba8_image,
                  const std::string& format_hint, std::chrono::milliseconds timeout)
      -> SemanticEmbeddingResult override {
    (void)rgba8_image;
    (void)format_hint;
    (void)timeout;
    return {.request_id = request_id,
            .embedding  = {0.0f, 1.0f},
            .dimension  = 2,
            .model_name = "test/mobileclip",
            .ok         = true};
  }

  auto EmbedImageBatch(std::vector<SemanticImageEmbeddingRequest> requests,
                       std::chrono::milliseconds timeout)
      -> std::vector<SemanticEmbeddingResult> override {
    (void)timeout;
    state_->embed_image_batch_calls.fetch_add(1);
    std::vector<SemanticEmbeddingResult> results;
    results.reserve(requests.size());
    for (const auto& request : requests) {
      results.push_back(EmbedImage(request.request_id, request.rgba8_image, request.format_hint,
                                   timeout));
    }
    return results;
  }

 private:
  std::shared_ptr<FakeSidecarState> state_;
};

class FakeImageAnalysisClient final : public sidecar_client::ImageAnalysisClient {
 public:
  explicit FakeImageAnalysisClient(std::shared_ptr<FakeSidecarState> state)
      : state_(std::move(state)) {}

  auto DescribeImage(const ImageAnalysisRequest& request, std::chrono::milliseconds timeout)
      -> ImageAnalysisUnderstandingResult override {
    (void)timeout;
    state_->describe_image_calls.fetch_add(1);
    ImageAnalysisUnderstandingResult result;
    result.request_id = request.request_id;
    result.ok         = true;
    result.status     = 1;
    result.caption    = "fake caption";
    result.tags       = {"fake", "tag"};
    result.scene      = "fake scene";
    result.confidence = 0.9;
    result.provider   = "fake";
    result.model_id   = request.model_id.empty() ? "fake-model" : request.model_id;
    result.rendition  = request.rendition;
    return result;
  }

  auto ScoreImage(const ImageAnalysisRequest& request, std::chrono::milliseconds timeout)
      -> ImageAnalysisRatingResult override {
    (void)timeout;
    state_->score_image_calls.fetch_add(1);
    ImageAnalysisRatingResult result;
    result.request_id     = request.request_id;
    result.ok             = true;
    result.status         = 1;
    result.rating         = 4;
    result.rubric_id      = "alcedo-default-v1";
    result.rubric_version = "1";
    result.provider       = "fake";
    result.model_id       = request.model_id.empty() ? "fake-model" : request.model_id;
    result.rendition      = request.rendition;
    return result;
  }

  auto AnalyzeImage(const ImageAnalysisRequest& request, std::chrono::milliseconds timeout)
      -> ImageAnalysisCombinedResult override {
    (void)timeout;
    state_->describe_image_calls.fetch_add(1);
    state_->score_image_calls.fetch_add(1);
    ImageAnalysisCombinedResult result;
    result.request_id          = request.request_id;
    result.ok                  = true;
    result.status              = 1;
    result.provider            = "fake";
    result.model_id            = request.model_id.empty() ? "fake-model" : request.model_id;
    result.rendition           = request.rendition;
    result.has_understanding   = true;
    result.has_rating          = true;
    result.understanding.request_id = request.request_id;
    result.understanding.ok         = true;
    result.understanding.status     = 1;
    result.understanding.caption    = "fake caption";
    result.understanding.tags       = {"fake", "tag"};
    result.understanding.scene      = "fake scene";
    result.understanding.confidence = 0.9;
    result.understanding.provider   = "fake";
    result.understanding.model_id   = result.model_id;
    result.understanding.rendition  = request.rendition;
    result.rating.request_id        = request.request_id;
    result.rating.ok                = true;
    result.rating.status            = 1;
    result.rating.rating            = 4;
    result.rating.rubric_id         = "alcedo-default-v1";
    result.rating.rubric_version    = "1";
    result.rating.provider          = "fake";
    result.rating.model_id          = result.model_id;
    result.rating.rendition         = request.rendition;
    return result;
  }

  auto BatchAnalyzeImage(const std::vector<ImageAnalysisRequest>& requests,
                         std::chrono::milliseconds timeout)
      -> std::vector<ImageAnalysisCombinedResult> override {
    std::vector<ImageAnalysisCombinedResult> results;
    results.reserve(requests.size());
    for (const auto& request : requests) {
      results.push_back(AnalyzeImage(request, timeout));
    }
    return results;
  }

  auto ListModels(const std::string& provider_id, const std::string& credential_ref,
                  std::chrono::milliseconds timeout) -> ImageAnalysisListModelsResult override {
    (void)provider_id;
    (void)credential_ref;
    (void)timeout;
    state_->list_models_calls.fetch_add(1);
    ImageAnalysisListModelsResult result;
    result.ok     = true;
    result.status = 1;
    result.models = state_->list_models_canned;
    return result;
  }

 private:
  std::shared_ptr<FakeSidecarState> state_;
};

class FakeSidecarClient final : public sidecar_client::Client {
 public:
  explicit FakeSidecarClient(bool ready = true)
      : state_(std::make_shared<FakeSidecarState>()),
        runtime_(state_),
        credentials_(state_),
        semantic_(state_),
        image_analysis_(state_) {
    state_->ready = ready;
  }

  auto endpoint() const -> const std::string& override { return endpoint_; }
  auto runtime() -> sidecar_client::RuntimeControlClient& override { return runtime_; }
  auto credentials() -> sidecar_client::CredentialClient& override { return credentials_; }
  auto models() -> sidecar_client::ModelManagerClient& override { return models_; }
  auto semantic() -> sidecar_client::SemanticEmbeddingClient& override { return semantic_; }
  auto image_analysis() -> sidecar_client::ImageAnalysisClient& override {
    return image_analysis_;
  }

  void SetEndpoint(std::string endpoint) { endpoint_ = std::move(endpoint); }
  void SetReady(bool ready) { state_->ready.store(ready); }
  void SetModelInfoReady(bool ready) { state_->model_info_ready.store(ready); }
  void SetListModelsCanned(std::vector<AiDiscoveredModel> models) {
    state_->list_models_canned = std::move(models);
  }
  auto PingCount() const -> int { return state_->ping_count.load(); }
  auto EmbedTextCalls() const -> int { return state_->embed_text_calls.load(); }
  auto EmbedImageBatchCalls() const -> int { return state_->embed_image_batch_calls.load(); }
  auto DescribeImageCalls() const -> int { return state_->describe_image_calls.load(); }
  auto ScoreImageCalls() const -> int { return state_->score_image_calls.load(); }
  auto ListModelsCalls() const -> int { return state_->list_models_calls.load(); }
  auto RevokeCalls() const -> int { return state_->revoke_calls.load(); }
  auto LastRevokedHandle() const -> std::string { return state_->last_revoked_handle; }

 private:
  std::shared_ptr<FakeSidecarState> state_;
  std::string                       endpoint_;
  FakeRuntimeControlClient          runtime_;
  FakeCredentialClient              credentials_;
  FakeModelManagerClient            models_;
  FakeSemanticEmbeddingClient       semantic_;
  FakeImageAnalysisClient           image_analysis_;
};

auto FakeFactory(const std::shared_ptr<FakeSidecarClient>& client) -> AiSidecarClientFactory {
  return [client](const std::string& endpoint) -> std::shared_ptr<sidecar_client::Client> {
    client->SetEndpoint(endpoint);
    return client;
  };
}

auto FakeRuntimePath() -> std::filesystem::path {
  return std::filesystem::path(ALCEDO_AI_SIDECAR_FAKE_RUNTIME_PATH);
}

auto BaseOptions() -> AiSidecarRuntimeOptions {
  AiSidecarRuntimeOptions options;
  options.runtime_binary         = FakeRuntimePath();
  options.model_root             = std::filesystem::temp_directory_path() / "semantic_runtime_test_model";
  options.model_id               = "test/mobileclip";
  options.revision               = "rev-a";
  options.device                 = "cpu";
  options.batch_cap              = 8;
  options.batch_wait_ms          = 2;
  options.startup_timeout        = std::chrono::milliseconds(1000);
  options.health_poll_interval   = std::chrono::milliseconds(20);
  options.graceful_stop_timeout  = std::chrono::milliseconds(100);
  options.kill_timeout           = std::chrono::milliseconds(1000);
  return options;
}

void SetModelRootEnv(const std::string& value) {
#ifdef _WIN32
  _putenv_s("ALCEDO_MIND_MODEL_ROOT", value.c_str());
#else
  if (value.empty()) {
    unsetenv("ALCEDO_MIND_MODEL_ROOT");
  } else {
    setenv("ALCEDO_MIND_MODEL_ROOT", value.c_str(), 1);
  }
#endif
}

class ScopedModelRootEnv final {
 public:
  explicit ScopedModelRootEnv(const std::string& value) {
    const char* previous = std::getenv("ALCEDO_MIND_MODEL_ROOT");
    if (previous != nullptr) {
      previous_value_ = previous;
    }
    SetModelRootEnv(value);
  }

  ~ScopedModelRootEnv() { SetModelRootEnv(previous_value_.value_or(std::string{})); }

  ScopedModelRootEnv(const ScopedModelRootEnv&)            = delete;
  ScopedModelRootEnv& operator=(const ScopedModelRootEnv&) = delete;

 private:
  std::optional<std::string> previous_value_;
};

}  // namespace

TEST(AiSidecarRuntimeServiceTest, StartStopReportsReadyAndStopped) {
  auto                    client = std::make_shared<FakeSidecarClient>();
  AiSidecarRuntimeService service(FakeFactory(client));

  auto options            = BaseOptions();
  options.extra_arguments = {"--sleep-ms", "30000"};

  ASSERT_TRUE(service.StartAndWait(options));
  auto status = service.Status();
  EXPECT_EQ(status.state, AiSidecarRuntimeState::kReady);
  EXPECT_EQ(status.issue, AiSidecarRuntimeIssue::kNone);
  EXPECT_FALSE(status.endpoint.empty());
  EXPECT_GT(status.process_id, 0);
  ASSERT_TRUE(status.model_info.has_value());
  EXPECT_EQ(status.model_info->profile_id, "mobileclip2-s2-en");
  EXPECT_EQ(status.model_info->embedding_dimension, 512U);
  EXPECT_EQ(status.model_info->native_embedding_dimension, 512U);
  EXPECT_EQ(status.model_info->embedding_transform, "l2_normalize");
  ASSERT_TRUE(status.remote_status.has_value());
  EXPECT_EQ(status.remote_status->provider, "fake");
  EXPECT_EQ(service.ClientSession(), client);
  EXPECT_EQ(client->endpoint(), status.endpoint);

  service.Stop();
  status = service.Status();
  EXPECT_EQ(status.state, AiSidecarRuntimeState::kStopped);
  EXPECT_EQ(status.issue, AiSidecarRuntimeIssue::kNone);
  EXPECT_EQ(status.process_id, 0);
  EXPECT_EQ(service.ClientSession(), nullptr);
}

TEST(AiSidecarRuntimeServiceTest, MissingBinaryFailsBeforeProcessStart) {
  auto                    client = std::make_shared<FakeSidecarClient>();
  AiSidecarRuntimeService service(FakeFactory(client));
  auto                    options = BaseOptions();
  options.runtime_binary = std::filesystem::temp_directory_path() / "missing_alcedo_mind.exe";

  EXPECT_FALSE(service.StartAndWait(options));
  const auto status = service.Status();
  EXPECT_EQ(status.state, AiSidecarRuntimeState::kFailed);
  EXPECT_EQ(status.issue, AiSidecarRuntimeIssue::kBinaryMissing);
  EXPECT_EQ(service.ClientSession(), nullptr);
}

TEST(AiSidecarRuntimeServiceTest, RuntimeExitBeforeReadyIsReported) {
  auto                    client = std::make_shared<FakeSidecarClient>(false);
  AiSidecarRuntimeService service(FakeFactory(client));
  auto                    options = BaseOptions();
  options.extra_arguments         = {"--exit-now", "--exit-code", "7"};

  EXPECT_FALSE(service.StartAndWait(options));
  const auto status = service.Status();
  EXPECT_EQ(status.state, AiSidecarRuntimeState::kFailed);
  EXPECT_EQ(status.issue, AiSidecarRuntimeIssue::kRuntimeCrashed);
  EXPECT_NE(status.stderr_tail.find(""), std::string::npos);
}

TEST(AiSidecarRuntimeServiceTest, MissingClientSessionReportsClientUnavailable) {
  AiSidecarRuntimeService service(
      [](const std::string&) -> std::shared_ptr<sidecar_client::Client> { return nullptr; });
  auto options            = BaseOptions();
  options.extra_arguments = {"--sleep-ms", "30000"};

  EXPECT_FALSE(service.StartAndWait(options));
  const auto status = service.Status();
  EXPECT_EQ(status.state, AiSidecarRuntimeState::kFailed);
  EXPECT_EQ(status.issue, AiSidecarRuntimeIssue::kClientUnavailable);
  EXPECT_EQ(status.process_id, 0);
  EXPECT_EQ(service.ClientSession(), nullptr);
}

TEST(AiSidecarRuntimeServiceTest, PingWithoutModelInfoDoesNotBecomeReady) {
  auto client = std::make_shared<FakeSidecarClient>();
  client->SetModelInfoReady(false);
  AiSidecarRuntimeService service(FakeFactory(client));

  auto options                  = BaseOptions();
  options.extra_arguments       = {"--sleep-ms", "30000"};
  options.startup_timeout       = std::chrono::milliseconds(120);
  options.health_poll_interval  = std::chrono::milliseconds(20);

  EXPECT_FALSE(service.StartAndWait(options));
  const auto status = service.Status();
  EXPECT_EQ(status.state, AiSidecarRuntimeState::kFailed);
  EXPECT_EQ(status.issue, AiSidecarRuntimeIssue::kReadinessTimeout);
  EXPECT_NE(status.message.find("fake semantic model is unavailable"), std::string::npos);
  EXPECT_FALSE(status.model_info.has_value());
  EXPECT_EQ(service.ClientSession(), nullptr);
}

TEST(AiSidecarRuntimeServiceTest, ModelManagerRuntimeCanStartWithoutModelInfo) {
  auto client = std::make_shared<FakeSidecarClient>();
  client->SetModelInfoReady(false);
  AiSidecarRuntimeService service(FakeFactory(client));

  auto options               = BaseOptions();
  options.extra_arguments    = {"--sleep-ms", "30000"};
  options.require_model_info = false;

  ASSERT_TRUE(service.StartAndWait(options));
  const auto status = service.Status();
  EXPECT_EQ(status.state, AiSidecarRuntimeState::kReady);
  EXPECT_EQ(status.issue, AiSidecarRuntimeIssue::kNone);
  EXPECT_FALSE(status.model_info.has_value());
  EXPECT_NE(status.message.find("model manager"), std::string::npos);
  service.Stop();
}

TEST(AiSidecarRuntimeServiceTest, ReadyRuntimeSelfExitBecomesUiVisibleFailure) {
  auto                    client = std::make_shared<FakeSidecarClient>();
  AiSidecarRuntimeService service(FakeFactory(client));
  auto                    options = BaseOptions();
  options.extra_arguments         = {"--exit-after-ms", "100", "--exit-code", "9"};

  ASSERT_TRUE(service.StartAndWait(options));
  std::this_thread::sleep_for(std::chrono::milliseconds(250));
  const auto status = service.Status();
  EXPECT_EQ(status.state, AiSidecarRuntimeState::kFailed);
  EXPECT_EQ(status.issue, AiSidecarRuntimeIssue::kRuntimeCrashed);
  EXPECT_NE(status.stderr_tail.find("self-exit"), std::string::npos);
  EXPECT_EQ(service.ClientSession(), nullptr);
}

TEST(AiSidecarRuntimeServiceTest, StopKillsHungRuntime) {
  auto                    client = std::make_shared<FakeSidecarClient>();
  AiSidecarRuntimeService service(FakeFactory(client));
  auto                    options = BaseOptions();
  options.extra_arguments         = {"--ignore-terminate", "--sleep-ms", "30000"};
  options.graceful_stop_timeout   = std::chrono::milliseconds(1);
  options.kill_timeout            = std::chrono::milliseconds(1000);

  ASSERT_TRUE(service.StartAndWait(options));
  service.Stop();
  const auto status = service.Status();
  EXPECT_EQ(status.state, AiSidecarRuntimeState::kStopped);
  EXPECT_EQ(status.issue, AiSidecarRuntimeIssue::kNone);
  EXPECT_EQ(service.ClientSession(), nullptr);
}

TEST(AiSidecarRuntimeServiceTest, RuntimeArgumentsCarryModelAndDeviceConfiguration) {
  auto                    client = std::make_shared<FakeSidecarClient>();
  AiSidecarRuntimeService service(FakeFactory(client));
  const auto              record_path =
      std::filesystem::temp_directory_path() / "semantic_runtime_args.txt";
  std::filesystem::remove(record_path);

  auto options            = BaseOptions();
  options.model_root      = std::filesystem::path("C:/models/mobileclip");
  options.model_id        = "repo/model:s2";
  options.revision        = "abc123";
  options.device          = "directml:0";
  options.extra_arguments = {"--record-args", record_path.string(), "--sleep-ms", "30000"};

  ASSERT_TRUE(service.StartAndWait(options));
  service.Stop();

  std::ifstream in(record_path);
  ASSERT_TRUE(in.is_open());
  const std::string args((std::istreambuf_iterator<char>(in)), std::istreambuf_iterator<char>());
  in.close();
  EXPECT_NE(args.find("--model-root\nC:/models/mobileclip"), std::string::npos);
  EXPECT_NE(args.find("--model-id\nrepo/model:s2"), std::string::npos);
  EXPECT_NE(args.find("--revision\nabc123"), std::string::npos);
  EXPECT_NE(args.find("--device\ndirectml:0"), std::string::npos);
  EXPECT_NE(args.find("--no-download"), std::string::npos);
  std::filesystem::remove(record_path);
}

TEST(AiSidecarRuntimeServiceTest, ClientSessionExposesNarrowModulesWhenReady) {
  auto client = std::make_shared<FakeSidecarClient>();
  client->SetListModelsCanned({
      AiDiscoveredModel{"gpt-4o", "GPT-4o", "opencode_go_openai"},
      AiDiscoveredModel{"gpt-4o-mini", "GPT-4o mini", "opencode_go_openai"},
  });
  AiSidecarRuntimeService service(FakeFactory(client));

  auto options            = BaseOptions();
  options.extra_arguments = {"--sleep-ms", "30000"};

  ASSERT_TRUE(service.StartAndWait(options));
  auto session = service.ClientSession();
  ASSERT_NE(session, nullptr);

  std::string error;
  const auto  profiles =
      session->models().ListModelProfiles("C:/models", std::chrono::milliseconds(100), &error);
  ASSERT_EQ(profiles.size(), 2u) << error;
  EXPECT_EQ(profiles[1].profile_id, "jina-clip-v2-int8-multilingual");
  EXPECT_EQ(profiles[1].native_embedding_dimension, 1024u);

  std::vector<AiSidecarCapability> capabilities;
  ASSERT_TRUE(session->runtime().ListCapabilities(std::chrono::milliseconds(100),
                                                  &capabilities, &error))
      << error;
  ASSERT_EQ(capabilities.size(), 1u);
  EXPECT_EQ(capabilities[0].task_id, "semantic.embed_*");

  const auto text =
      session->semantic().EmbedText("text-1", "a quiet portrait", std::chrono::milliseconds(100));
  EXPECT_TRUE(text.ok);
  EXPECT_EQ(text.request_id, "text-1");
  EXPECT_EQ(client->EmbedTextCalls(), 1);

  std::string handle;
  ASSERT_TRUE(session->credentials().RegisterCredential("remote", "sk-test", 0,
                                                        std::chrono::milliseconds(100),
                                                        &handle, &error))
      << error;
  EXPECT_EQ(handle, "fake-credential-handle");

  const auto models = session->image_analysis().ListModels(
      "opencode_go_openai", handle, std::chrono::milliseconds(100));
  ASSERT_TRUE(models.ok) << models.error;
  ASSERT_EQ(models.models.size(), 2u);
  EXPECT_EQ(client->ListModelsCalls(), 1);

  bool revoked = false;
  ASSERT_TRUE(session->credentials().RevokeCredential(handle, std::chrono::milliseconds(100),
                                                      &revoked, &error))
      << error;
  EXPECT_TRUE(revoked);
  EXPECT_EQ(client->RevokeCalls(), 1);
  EXPECT_EQ(client->LastRevokedHandle(), "fake-credential-handle");

  service.Stop();
}

TEST(AiSidecarRuntimeServiceTest, ClientSessionPreservesEmbeddingBatchRequestIds) {
  auto                    client = std::make_shared<FakeSidecarClient>();
  AiSidecarRuntimeService service(FakeFactory(client));

  auto options            = BaseOptions();
  options.extra_arguments = {"--sleep-ms", "30000"};

  ASSERT_TRUE(service.StartAndWait(options));
  auto session = service.ClientSession();
  ASSERT_NE(session, nullptr);

  std::vector<SemanticImageEmbeddingRequest> requests;
  for (const char* id : {"echo-1", "echo-2", "echo-3"}) {
    SemanticImageEmbeddingRequest req;
    req.request_id  = id;
    req.rgba8_image = std::vector<uint8_t>(4, 0);
    req.format_hint = "rgba8:1x1";
    requests.push_back(std::move(req));
  }

  const auto results = session->semantic().EmbedImageBatch(requests, std::chrono::milliseconds(100));
  ASSERT_EQ(results.size(), 3u);
  EXPECT_EQ(results[0].request_id, "echo-1");
  EXPECT_EQ(results[1].request_id, "echo-2");
  EXPECT_EQ(results[2].request_id, "echo-3");
  for (const auto& r : results) {
    EXPECT_TRUE(r.ok);
  }
  EXPECT_EQ(client->EmbedImageBatchCalls(), 1);
  service.Stop();
}

TEST(AiSidecarRuntimeServiceTest, ClientSessionExposesImageAnalysisAndCancel) {
  auto                    client = std::make_shared<FakeSidecarClient>();
  AiSidecarRuntimeService service(FakeFactory(client));
  auto                    options = BaseOptions();
  options.extra_arguments         = {"--sleep-ms", "30000"};
  ASSERT_TRUE(service.StartAndWait(options));
  auto session = service.ClientSession();
  ASSERT_NE(session, nullptr);

  ImageAnalysisRequest req;
  req.request_id        = "ia-describe-1";
  req.image_bytes       = {0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10};
  req.image_format_hint = "image/jpeg;max_edge=1024";
  req.provider_id       = "openrouter";
  req.model_id          = "qwen/qwen3.7-plus";
  req.rubric_id         = "alcedo-default-v1";
  req.rendition.kind    = "thumbnail";
  req.rendition.width   = 1024;
  req.rendition.height  = 683;
  req.rendition.bytes   = req.image_bytes.size();

  const auto understanding =
      session->image_analysis().DescribeImage(req, std::chrono::milliseconds(5000));
  EXPECT_TRUE(understanding.ok) << understanding.error;
  EXPECT_EQ(understanding.request_id, "ia-describe-1");
  EXPECT_FALSE(understanding.caption.empty());
  EXPECT_EQ(client->DescribeImageCalls(), 1);

  const auto rating = session->image_analysis().ScoreImage(req, std::chrono::milliseconds(5000));
  EXPECT_TRUE(rating.ok) << rating.error;
  EXPECT_EQ(rating.rating, 4);
  EXPECT_EQ(client->ScoreImageCalls(), 1);

  std::string error;
  bool        cancelled = false;
  EXPECT_TRUE(session->runtime().CancelTask("fake-in-flight", std::chrono::milliseconds(2000),
                                            &cancelled, &error));
  EXPECT_TRUE(cancelled) << error;
  service.Stop();
}

TEST(AiSidecarRuntimeServiceTest,
     RegisteredSecretDoesNotLeakIntoProcessArgsOrCapturedStatus) {
  auto                    client = std::make_shared<FakeSidecarClient>();
  AiSidecarRuntimeService service(FakeFactory(client));

  const auto record_path =
      std::filesystem::temp_directory_path() / "ai_sidecar_credential_args.txt";
  std::filesystem::remove(record_path);

  auto options            = BaseOptions();
  options.extra_arguments = {"--record-args", record_path.string(), "--sleep-ms", "30000"};

  ASSERT_TRUE(service.StartAndWait(options));
  auto session = service.ClientSession();
  ASSERT_NE(session, nullptr);

  const std::string secret = "sk-DO-NOT-LEAK-7c3f9a1e-b2d4";
  std::string       handle;
  std::string       error;
  EXPECT_TRUE(session->credentials().RegisterCredential("remote", secret, 0,
                                                        std::chrono::milliseconds(2000),
                                                        &handle, &error));
  EXPECT_EQ(handle, "fake-credential-handle");
  EXPECT_TRUE(error.empty()) << error;

  service.Stop();

  std::ifstream in(record_path);
  ASSERT_TRUE(in.is_open());
  const std::string args((std::istreambuf_iterator<char>(in)), std::istreambuf_iterator<char>());
  in.close();
  EXPECT_EQ(args.find(secret), std::string::npos) << args;

  const auto status = service.Status();
  EXPECT_EQ(status.message.find(secret), std::string::npos) << status.message;
  EXPECT_EQ(status.stdout_tail.find(secret), std::string::npos) << status.stdout_tail;
  EXPECT_EQ(status.stderr_tail.find(secret), std::string::npos) << status.stderr_tail;

  std::filesystem::remove(record_path);
}

TEST(AiSidecarRuntimeServiceTest, EmptyModelRootUsesEnvironmentFallback) {
  auto                    client = std::make_shared<FakeSidecarClient>();
  AiSidecarRuntimeService service(FakeFactory(client));
  const auto              record_path =
      std::filesystem::temp_directory_path() / "semantic_runtime_env_model_args.txt";
  const auto model_root =
      std::filesystem::temp_directory_path() / "semantic_runtime_env_model_root";
  std::filesystem::remove(record_path);
  std::filesystem::create_directories(model_root);

  ScopedModelRootEnv model_root_env(model_root.string());

  auto options = BaseOptions();
  options.model_root.clear();
  options.extra_arguments = {"--record-args", record_path.string(), "--sleep-ms", "30000"};

  ASSERT_TRUE(service.StartAndWait(options));
  service.Stop();

  std::ifstream in(record_path);
  ASSERT_TRUE(in.is_open());
  const std::string args((std::istreambuf_iterator<char>(in)), std::istreambuf_iterator<char>());
  in.close();
  EXPECT_NE(args.find("--model-root\n" + model_root.string()), std::string::npos);
  std::filesystem::remove(record_path);
  std::filesystem::remove_all(model_root);
}

TEST(AiSidecarRuntimeServiceTest, ProjectServiceOwnsStoppedRuntimeService) {
  const auto db_path   = std::filesystem::temp_directory_path() / "semantic_runtime_project.db";
  const auto meta_path = std::filesystem::temp_directory_path() / "semantic_runtime_project.json";
  std::filesystem::remove(db_path);
  std::filesystem::remove(meta_path);

  {
    ProjectService project(db_path, meta_path, ProjectOpenMode::kCreateNew);
    auto           runtime = project.GetAiSidecarRuntimeService();
    ASSERT_NE(runtime, nullptr);
    const auto status = runtime->Status();
    EXPECT_EQ(status.state, AiSidecarRuntimeState::kStopped);
  }

  std::filesystem::remove(db_path);
  std::filesystem::remove(meta_path);
}

TEST(AiSidecarRuntimeServiceTest, ProjectServiceCreatesRuntimeOnCallerThreadAfterBackgroundLoad) {
  const auto db_path   = std::filesystem::temp_directory_path() / "semantic_runtime_thread.db";
  const auto meta_path = std::filesystem::temp_directory_path() / "semantic_runtime_thread.json";
  std::filesystem::remove(db_path);
  std::filesystem::remove(meta_path);

  std::shared_ptr<ProjectService> project;
  std::thread                     loader([&project, &db_path, &meta_path]() {
    project = std::make_shared<ProjectService>(db_path, meta_path, ProjectOpenMode::kCreateNew);
  });
  loader.join();

  ASSERT_NE(project, nullptr);
  auto runtime = project->GetAiSidecarRuntimeService();
  ASSERT_NE(runtime, nullptr);
  ASSERT_EQ(runtime->thread(), QThread::currentThread());
  const auto status = runtime->Status();
  EXPECT_EQ(status.state, AiSidecarRuntimeState::kStopped);

  project.reset();
  std::filesystem::remove(db_path);
  std::filesystem::remove(meta_path);
}

TEST(AiSidecarRuntimeServiceLiveTest, DefaultGrpcClientEmbedsRawRgba8AgainstRustRuntime) {
  const char* runtime_path_env = std::getenv("ALCEDO_SEMANTIC_LIVE_RUNTIME_PATH");
  const char* model_root_env   = std::getenv("ALCEDO_SEMANTIC_LIVE_MODEL_ROOT");
  if (runtime_path_env == nullptr || model_root_env == nullptr) {
    GTEST_SKIP() << "Set ALCEDO_SEMANTIC_LIVE_RUNTIME_PATH and "
                    "ALCEDO_SEMANTIC_LIVE_MODEL_ROOT to run the live Rust runtime smoke.";
  }

  const auto env_or = [](const char* name, const char* fallback) -> std::string {
    const char* value = std::getenv(name);
    return value != nullptr && value[0] != '\0' ? std::string(value) : std::string(fallback);
  };
  const auto env_u32_or = [](const char* name, uint32_t fallback) -> uint32_t {
    const char* value = std::getenv(name);
    if (value == nullptr || value[0] == '\0') {
      return fallback;
    }
    return static_cast<uint32_t>(std::stoul(value));
  };

  const auto model_id = env_or("ALCEDO_SEMANTIC_LIVE_MODEL_ID", "plhery/mobileclip2-onnx:s2");
  const auto revision =
      env_or("ALCEDO_SEMANTIC_LIVE_REVISION", "ba95759a5bdbaca53e9111e2550a76ec09c8fd9e");
  const auto expected_image_size = env_u32_or("ALCEDO_SEMANTIC_LIVE_EXPECTED_IMAGE_SIZE", 256u);

  AiSidecarRuntimeService service;
  AiSidecarRuntimeOptions options;
  options.runtime_binary        = std::filesystem::path(runtime_path_env);
  options.model_root            = std::filesystem::path(model_root_env);
  options.model_id              = model_id;
  options.revision              = revision;
  options.device                = "cpu";
  options.batch_cap             = 8;
  options.batch_wait_ms         = 2;
  options.startup_timeout       = std::chrono::milliseconds(120000);
  options.health_poll_interval  = std::chrono::milliseconds(100);
  options.graceful_stop_timeout = std::chrono::milliseconds(1000);
  options.kill_timeout          = std::chrono::milliseconds(2000);

  ASSERT_TRUE(service.StartAndWait(options)) << service.Status().message;
  const auto status = service.Status();
  ASSERT_TRUE(status.model_info.has_value());
  EXPECT_EQ(status.model_info->model_id, options.model_id);
  EXPECT_EQ(status.model_info->revision, options.revision);
  EXPECT_EQ(status.model_info->embedding_dimension, 512u);
  EXPECT_EQ(status.model_info->image_size, expected_image_size);
  auto session = service.ClientSession();
  ASSERT_NE(session, nullptr);

  std::vector<uint8_t> rgba8(256u * 256u * 4u);
  for (size_t i = 0; i < rgba8.size(); i += 4) {
    rgba8[i]     = 64;
    rgba8[i + 1] = 128;
    rgba8[i + 2] = 192;
    rgba8[i + 3] = 255;
  }

  SemanticImageEmbeddingRequest request;
  request.request_id  = "live-rgba8-image-1";
  request.rgba8_image = std::move(rgba8);
  request.format_hint = "rgba8:256x256";

  const auto results =
      session->semantic().EmbedImageBatch({request}, std::chrono::milliseconds(120000));
  ASSERT_EQ(results.size(), 1u);
  EXPECT_EQ(results[0].request_id, "live-rgba8-image-1");
  EXPECT_TRUE(results[0].ok) << results[0].error;
  EXPECT_EQ(results[0].dimension, 512u);
  EXPECT_EQ(results[0].embedding.size(), 512u);

  service.Stop();
}

}  // namespace alcedo
