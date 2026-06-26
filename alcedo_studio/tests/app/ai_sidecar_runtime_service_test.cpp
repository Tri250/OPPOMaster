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
#include <optional>
#include <string>
#include <thread>

#include "app/project_service.hpp"

namespace alcedo {
namespace {

class FakeAiSidecarRuntimeClient final : public IAiSidecarRuntimeClient {
 public:
  explicit FakeAiSidecarRuntimeClient(bool ready = true) : ready_(ready) {}

  auto Ping(const std::string& endpoint, std::chrono::milliseconds timeout, std::string* error)
      -> bool override {
    (void)endpoint;
    (void)timeout;
    ping_count_.fetch_add(1);
    if (!ready_.load()) {
      if (error) {
        *error = "fake runtime is not ready";
      }
      return false;
    }
    return true;
  }

  auto GetModelInfo(const std::string& endpoint, std::chrono::milliseconds timeout,
                    AiSidecarRuntimeModelInfo* info, std::string* error) -> bool override {
    (void)endpoint;
    (void)timeout;
    if (!model_info_ready_.load()) {
      if (error) {
        *error = "fake semantic model is unavailable";
      }
      return false;
    }
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
    return true;
  }

  auto GetRuntimeStatus(const std::string& endpoint, std::chrono::milliseconds timeout,
                        AiSidecarRuntimeRemoteStatus* status, std::string* error) -> bool override {
    (void)endpoint;
    (void)timeout;
    (void)error;
    status->state               = "ready";
    status->provider            = "fake";
    status->image_batch_cap     = 8;
    status->image_batch_wait_ms = 2;
    status->uptime_ms           = 42;
    return true;
  }

  // Canned `AiRuntimeService::ListCapabilities` response (Phase 0 §4.4): one local
  // semantic-embedding capability. input_kinds/output_kinds carry the raw enum values
  // (alcedo::ai::AiInputKind / AiOutputKind) since the host DTO stores them as int.
  auto ListCapabilities(const std::string& endpoint, std::chrono::milliseconds timeout,
                        std::vector<AiSidecarCapability>* capabilities, std::string* error)
      -> bool override {
    (void)endpoint;
    (void)timeout;
    (void)error;
    if (!ready_.load()) {
      if (error) {
        *error = "fake runtime is not ready";
      }
      return false;
    }
    if (capabilities) {
      capabilities->clear();
      AiSidecarCapability capability;
      capability.task_id             = "semantic.embed_*";
      capability.provider_id         = "local";
      capability.model_id            = "test/mobileclip";
      capability.input_kinds         = {2, 3};  // AI_INPUT_IMAGE, AI_INPUT_THUMBNAIL
      capability.output_kinds        = {1};     // AI_OUTPUT_EMBEDDING
      capability.supports_batch      = true;
      capability.supports_cancel     = true;
      capability.requires_credential = false;
      capability.max_payload_bytes   = 0;
      capabilities->push_back(std::move(capability));
    }
    return true;
  }

  // Canned `AiRuntimeService::RegisterCredential` (Phase 3): returns a fixed opaque handle
  // and ignores the secret/provider_id/ttl_ms. The secret never reaches the fake, which is
  // the point — it proves the host never routes key material through process args or logs.
  auto RegisterCredential(const std::string& endpoint, std::chrono::milliseconds timeout,
                          const std::string& provider_id, const std::string& secret, int64_t ttl_ms,
                          std::string* handle, std::string* error) -> bool override {
    (void)endpoint;
    (void)timeout;
    (void)provider_id;
    (void)secret;
    (void)ttl_ms;
    (void)error;
    if (!ready_.load()) {
      if (error) {
        *error = "fake runtime is not ready";
      }
      return false;
    }
    if (handle) {
      *handle = "fake-credential-handle";
    }
    return true;
  }

  auto RevokeCredential(const std::string& endpoint, std::chrono::milliseconds timeout,
                        const std::string& handle, bool* revoked, std::string* error)
      -> bool override {
    (void)endpoint;
    (void)timeout;
    (void)error;
    if (!ready_.load()) {
      if (revoked) *revoked = false;
      return true;  // idempotent: handle dies with the sidecar
    }
    ++revoke_calls_;
    last_revoked_handle_ = handle;
    // The fake revokes any handle it previously minted ("fake-credential-handle").
    if (revoked) {
      *revoked = (handle == "fake-credential-handle");
    }
    return true;
  }
  auto RevokeCalls() const -> int { return revoke_calls_.load(); }
  auto LastRevokedHandle() const -> std::string { return last_revoked_handle_; }

  // Canned `AiRuntimeService::CancelTask` (Phase 3): reports cancellation only for the
  // well-known in-flight request_id "fake-in-flight"; any other id maps to cancelled=false.
  auto CancelTask(const std::string& endpoint, std::chrono::milliseconds timeout,
                  const std::string& request_id, bool* cancelled, std::string* error)
      -> bool override {
    (void)endpoint;
    (void)timeout;
    (void)error;
    if (!ready_.load()) {
      if (error) {
        *error = "fake runtime is not ready";
      }
      return false;
    }
    if (cancelled) {
      *cancelled = (request_id == "fake-in-flight");
    }
    return true;
  }

  auto ListModelProfiles(const std::string& endpoint, const std::string& model_root,
                         std::chrono::milliseconds timeout, std::string* error)
      -> std::vector<SemanticModelProfileInfo> override {
    (void)endpoint;
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

  auto ListInstalledModels(const std::string& endpoint, const std::string& model_root,
                           std::chrono::milliseconds timeout, std::string* error)
      -> std::vector<SemanticModelProfileInfo> override {
    auto profiles = ListModelProfiles(endpoint, model_root, timeout, error);
    profiles.resize(1);
    profiles[0].installed = true;
    return profiles;
  }

  auto ValidateModel(const std::string& endpoint, const std::string& profile_id,
                     const std::string& model_root, std::chrono::milliseconds timeout)
      -> SemanticModelManagerResult override {
    (void)endpoint;
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

  // NOTE: Model download (DownloadModel / GetModelDownloadStatus / CancelModelDownload)
  // was moved off the gRPC runtime service into the C++ aria2c layer; the runtime
  // service no longer exposes a download API, so the fake client does not either.

  auto DeleteModel(const std::string& endpoint, const std::string& profile_id,
                   const std::string& model_root, std::chrono::milliseconds timeout)
      -> SemanticModelManagerResult override {
    (void)endpoint;
    (void)model_root;
    (void)timeout;
    SemanticModelManagerResult result;
    result.ok                 = true;
    result.status             = "deleted";
    result.profile.profile_id = profile_id;
    return result;
  }

  auto EmbedText(const std::string& endpoint, const std::string& request_id,
                 const std::string& text, std::chrono::milliseconds timeout)
      -> SemanticEmbeddingResult override {
    (void)endpoint;
    (void)text;
    (void)timeout;
    embed_text_v1_calls_.fetch_add(1);
    SemanticEmbeddingResult result;
    result.request_id = request_id;
    result.embedding  = {1.0f, 0.0f};
    result.dimension  = 2;
    result.model_name = "test/mobileclip";
    result.ok         = true;
    return result;
  }

  auto EmbedImage(const std::string& endpoint, const std::string& request_id,
                  const std::vector<uint8_t>& rgba8_image, const std::string& format_hint,
                  std::chrono::milliseconds timeout) -> SemanticEmbeddingResult override {
    (void)endpoint;
    (void)rgba8_image;
    (void)format_hint;
    (void)timeout;
    SemanticEmbeddingResult result;
    result.request_id = request_id;
    result.embedding  = {0.0f, 1.0f};
    result.dimension  = 2;
    result.model_name = "test/mobileclip";
    result.ok         = true;
    return result;
  }

  auto EmbedImageBatch(const std::string&                         endpoint,
                       std::vector<SemanticImageEmbeddingRequest> requests,
                       std::chrono::milliseconds                  timeout)
      -> std::vector<SemanticEmbeddingResult> override {
    (void)endpoint;
    (void)timeout;
    embed_image_batch_v1_calls_.fetch_add(1);
    std::vector<SemanticEmbeddingResult> results;
    results.reserve(requests.size());
    for (const auto& request : requests) {
      SemanticEmbeddingResult result;
      result.request_id = request.request_id;
      result.embedding  = {0.0f, 1.0f};
      result.dimension  = 2;
      result.model_name = "test/mobileclip";
      result.ok         = true;
      results.push_back(std::move(result));
    }
    return results;
  }

  // v2 overrides (Phase 4): canned bit-identical to the v1 canned responses so existing
  // result-asserting tests pass on either path. When v2_supported_ is false the override
  // signals *v2_available=false (and returns {}) so the service wrapper falls back to v1.
  auto EmbedTextV2(const std::string& endpoint, const std::string& request_id,
                   const std::string& text, std::chrono::milliseconds timeout,
                   bool* v2_available) -> SemanticEmbeddingResult override {
    (void)endpoint;
    (void)text;
    (void)timeout;
    embed_text_v2_calls_.fetch_add(1);
    if (!v2_supported_.load()) {
      if (v2_available) *v2_available = false;
      return {};
    }
    if (v2_available) *v2_available = true;
    SemanticEmbeddingResult result;
    result.request_id = request_id;
    result.embedding  = {1.0f, 0.0f};
    result.dimension  = 2;
    result.model_name = "test/mobileclip";
    result.ok         = true;
    return result;
  }

  auto EmbedImageV2(const std::string& endpoint, const std::string& request_id,
                    const std::vector<uint8_t>& rgba8_image, const std::string& format_hint,
                    std::chrono::milliseconds timeout, bool* v2_available)
      -> SemanticEmbeddingResult override {
    (void)endpoint;
    (void)rgba8_image;
    (void)format_hint;
    (void)timeout;
    if (!v2_supported_.load()) {
      if (v2_available) *v2_available = false;
      return {};
    }
    if (v2_available) *v2_available = true;
    SemanticEmbeddingResult result;
    result.request_id = request_id;
    result.embedding  = {0.0f, 1.0f};
    result.dimension  = 2;
    result.model_name = "test/mobileclip";
    result.ok         = true;
    return result;
  }

  auto EmbedTextBatchV2(const std::string&                               endpoint,
                        const std::vector<SemanticTextEmbeddingRequest>& requests,
                        std::chrono::milliseconds                        timeout,
                        bool* v2_available) -> std::vector<SemanticEmbeddingResult> override {
    (void)endpoint;
    (void)timeout;
    if (!v2_supported_.load()) {
      if (v2_available) *v2_available = false;
      return {};
    }
    if (v2_available) *v2_available = true;
    std::vector<SemanticEmbeddingResult> results;
    results.reserve(requests.size());
    for (const auto& request : requests) {
      SemanticEmbeddingResult result;
      result.request_id = request.request_id;
      result.embedding  = {1.0f, 0.0f};
      result.dimension  = 2;
      result.model_name = "test/mobileclip";
      result.ok         = true;
      results.push_back(std::move(result));
    }
    return results;
  }

  auto EmbedImageBatchV2(const std::string&                                endpoint,
                         const std::vector<SemanticImageEmbeddingRequest>& requests,
                         std::chrono::milliseconds                        timeout,
                         bool* v2_available) -> std::vector<SemanticEmbeddingResult> override {
    (void)endpoint;
    (void)timeout;
    embed_image_batch_v2_calls_.fetch_add(1);
    if (!v2_supported_.load()) {
      if (v2_available) *v2_available = false;
      return {};
    }
    if (v2_available) *v2_available = true;
    std::vector<SemanticEmbeddingResult> results;
    results.reserve(requests.size());
    for (const auto& request : requests) {
      SemanticEmbeddingResult result;
      result.request_id = request.request_id;
      result.embedding  = {0.0f, 1.0f};
      result.dimension  = 2;
      result.model_name = "test/mobileclip";
      result.ok         = true;
      results.push_back(std::move(result));
    }
    return results;
  }

  // Phase 5d image-analysis overrides: canned success mirroring the request_id, bumping
  // counters. Used by the wire-layer ready-guard / delegation tests (proto->DTO mapping
  // is exercised only by a live sidecar, per the embedding-mapper convention).
  auto DescribeImage(const std::string& endpoint, const ImageAnalysisRequest& request,
                     std::chrono::milliseconds timeout) -> ImageAnalysisUnderstandingResult override {
    (void)endpoint;
    (void)timeout;
    describe_image_calls_.fetch_add(1);
    ImageAnalysisUnderstandingResult result;
    result.request_id = request.request_id;
    result.ok         = true;
    result.status     = 1;  // AI_STATUS_OK
    result.caption    = "fake caption";
    result.tags       = {"fake", "tag"};
    result.scene      = "fake scene";
    result.confidence = 0.9;
    result.provider   = "fake";
    result.model_id   = request.model_id.empty() ? "fake-model" : request.model_id;
    result.rendition  = request.rendition;
    return result;
  }
  auto ScoreImage(const std::string& endpoint, const ImageAnalysisRequest& request,
                  std::chrono::milliseconds timeout) -> ImageAnalysisRatingResult override {
    (void)endpoint;
    (void)timeout;
    score_image_calls_.fetch_add(1);
    ImageAnalysisRatingResult result;
    result.request_id    = request.request_id;
    result.ok            = true;
    result.status        = 1;  // AI_STATUS_OK
    // 1..=5 integer star rating (Phase 5f contract); no scores array, no confidence.
    result.rating        = 4;
    result.rubric_id     = "alcedo-default-v1";
    result.rubric_version = "1";
    result.provider      = "fake";
    result.model_id      = request.model_id.empty() ? "fake-model" : request.model_id;
    result.rendition     = request.rendition;
    return result;
  }

  auto ListModels(const std::string& endpoint, const std::string& provider_id,
                  const std::string& credential_ref, std::chrono::milliseconds timeout)
      -> ImageAnalysisListModelsResult override {
    (void)endpoint;
    (void)provider_id;
    (void)credential_ref;
    (void)timeout;
    list_models_calls_.fetch_add(1);
    ImageAnalysisListModelsResult result;
    result.ok = list_models_ok_.load();
    if (!result.ok) {
      result.error = list_models_error_;
      return result;
    }
    result.status = 1;  // AI_STATUS_OK
    result.models = list_models_canned_;
    return result;
  }

  void SetReady(bool ready) { ready_.store(ready); }
  void SetListModelsOk(bool ok, std::string error = {}) {
    list_models_ok_.store(ok);
    list_models_error_ = std::move(error);
  }
  void SetListModelsCanned(std::vector<AiDiscoveredModel> models) {
    list_models_canned_ = std::move(models);
  }
  auto ListModelsCalls() const -> int { return list_models_calls_.load(); }
  void SetModelInfoReady(bool ready) { model_info_ready_.store(ready); }
  auto PingCount() const -> int { return ping_count_.load(); }
  void SetV2Supported(bool supported) { v2_supported_.store(supported); }
  auto EmbedTextV1Calls() const -> int { return embed_text_v1_calls_.load(); }
  auto EmbedTextV2Calls() const -> int { return embed_text_v2_calls_.load(); }
  auto EmbedImageBatchV1Calls() const -> int { return embed_image_batch_v1_calls_.load(); }
  auto EmbedImageBatchV2Calls() const -> int { return embed_image_batch_v2_calls_.load(); }
  auto DescribeImageCalls() const -> int { return describe_image_calls_.load(); }
  auto ScoreImageCalls() const -> int { return score_image_calls_.load(); }

 private:
  std::atomic<bool> ready_;
  std::atomic<bool> model_info_ready_{true};
  std::atomic<int>  ping_count_{0};
  std::atomic<bool> v2_supported_{true};
  std::atomic<int>  embed_text_v1_calls_{0};
  std::atomic<int>  embed_text_v2_calls_{0};
  std::atomic<int>  embed_image_batch_v1_calls_{0};
  std::atomic<int>  embed_image_batch_v2_calls_{0};
  std::atomic<int>  describe_image_calls_{0};
  std::atomic<int>  score_image_calls_{0};
  std::atomic<int>  list_models_calls_{0};
  std::atomic<int>  revoke_calls_{0};
  std::atomic<bool> list_models_ok_{true};
  std::string       list_models_error_;
  std::string       last_revoked_handle_;
  std::vector<AiDiscoveredModel> list_models_canned_;
};

auto FakeRuntimePath() -> std::filesystem::path {
  return std::filesystem::path(ALCEDO_AI_SIDECAR_FAKE_RUNTIME_PATH);
}

auto BaseOptions() -> AiSidecarRuntimeOptions {
  AiSidecarRuntimeOptions options;
  options.runtime_binary  = FakeRuntimePath();
  options.model_root      = std::filesystem::temp_directory_path() / "semantic_runtime_test_model";
  options.model_id        = "test/mobileclip";
  options.revision        = "rev-a";
  options.device          = "cpu";
  options.batch_cap       = 8;
  options.batch_wait_ms   = 2;
  options.startup_timeout = std::chrono::milliseconds(1000);
  options.health_poll_interval  = std::chrono::milliseconds(20);
  options.graceful_stop_timeout = std::chrono::milliseconds(100);
  options.kill_timeout          = std::chrono::milliseconds(1000);
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
  auto                    client = std::make_shared<FakeAiSidecarRuntimeClient>();
  AiSidecarRuntimeService service(client);

  auto                    options = BaseOptions();
  options.extra_arguments         = {"--sleep-ms", "30000"};

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

  const auto text_result =
      service.EmbedText("text-1", "a quiet portrait", std::chrono::milliseconds(100));
  EXPECT_TRUE(text_result.ok);
  EXPECT_EQ(text_result.request_id, "text-1");
  const auto text_batch = service.EmbedTextBatch(
      {SemanticTextEmbeddingRequest{.request_id = "text-a", .text = "a quiet portrait"},
       SemanticTextEmbeddingRequest{.request_id = "text-b", .text = "a city street"}},
      std::chrono::milliseconds(100));
  ASSERT_EQ(text_batch.size(), 2U);
  EXPECT_TRUE(text_batch[0].ok);
  EXPECT_EQ(text_batch[0].request_id, "text-a");
  EXPECT_TRUE(text_batch[1].ok);
  EXPECT_EQ(text_batch[1].request_id, "text-b");

  service.Stop();
  status = service.Status();
  EXPECT_EQ(status.state, AiSidecarRuntimeState::kStopped);
  EXPECT_EQ(status.issue, AiSidecarRuntimeIssue::kNone);
  EXPECT_EQ(status.process_id, 0);
}

TEST(AiSidecarRuntimeServiceTest, MissingBinaryFailsBeforeProcessStart) {
  AiSidecarRuntimeService service(std::make_shared<FakeAiSidecarRuntimeClient>());
  auto                    options = BaseOptions();
  options.runtime_binary = std::filesystem::temp_directory_path() / "missing_alcedo_mind.exe";

  EXPECT_FALSE(service.StartAndWait(options));
  const auto status = service.Status();
  EXPECT_EQ(status.state, AiSidecarRuntimeState::kFailed);
  EXPECT_EQ(status.issue, AiSidecarRuntimeIssue::kBinaryMissing);
}

TEST(AiSidecarRuntimeServiceTest, RuntimeExitBeforeReadyIsReported) {
  AiSidecarRuntimeService service(std::make_shared<FakeAiSidecarRuntimeClient>(false));
  auto                    options = BaseOptions();
  options.extra_arguments         = {"--exit-now", "--exit-code", "7"};

  EXPECT_FALSE(service.StartAndWait(options));
  const auto status = service.Status();
  EXPECT_EQ(status.state, AiSidecarRuntimeState::kFailed);
  EXPECT_EQ(status.issue, AiSidecarRuntimeIssue::kRuntimeCrashed);
  EXPECT_NE(status.stderr_tail.find(""), std::string::npos);
}

TEST(AiSidecarRuntimeServiceTest, PingWithoutModelInfoDoesNotBecomeReady) {
  auto client = std::make_shared<FakeAiSidecarRuntimeClient>();
  client->SetModelInfoReady(false);
  AiSidecarRuntimeService service(client);

  auto                    options = BaseOptions();
  options.extra_arguments         = {"--sleep-ms", "30000"};
  options.startup_timeout         = std::chrono::milliseconds(120);
  options.health_poll_interval    = std::chrono::milliseconds(20);

  EXPECT_FALSE(service.StartAndWait(options));
  const auto status = service.Status();
  EXPECT_EQ(status.state, AiSidecarRuntimeState::kFailed);
  EXPECT_EQ(status.issue, AiSidecarRuntimeIssue::kReadinessTimeout);
  EXPECT_NE(status.message.find("fake semantic model is unavailable"), std::string::npos);
  EXPECT_FALSE(status.model_info.has_value());
}

TEST(AiSidecarRuntimeServiceTest, ModelManagerRuntimeCanStartWithoutModelInfo) {
  auto client = std::make_shared<FakeAiSidecarRuntimeClient>();
  client->SetModelInfoReady(false);
  AiSidecarRuntimeService service(client);

  auto                    options = BaseOptions();
  options.extra_arguments         = {"--sleep-ms", "30000"};
  options.require_model_info      = false;

  ASSERT_TRUE(service.StartAndWait(options));
  const auto status = service.Status();
  EXPECT_EQ(status.state, AiSidecarRuntimeState::kReady);
  EXPECT_EQ(status.issue, AiSidecarRuntimeIssue::kNone);
  EXPECT_FALSE(status.model_info.has_value());
  EXPECT_NE(status.message.find("model manager"), std::string::npos);
  service.Stop();
}

TEST(AiSidecarRuntimeServiceTest, ReadyRuntimeSelfExitBecomesUiVisibleFailure) {
  AiSidecarRuntimeService service(std::make_shared<FakeAiSidecarRuntimeClient>());
  auto                    options = BaseOptions();
  options.extra_arguments         = {"--exit-after-ms", "100", "--exit-code", "9"};

  ASSERT_TRUE(service.StartAndWait(options));
  std::this_thread::sleep_for(std::chrono::milliseconds(250));
  const auto status = service.Status();
  EXPECT_EQ(status.state, AiSidecarRuntimeState::kFailed);
  EXPECT_EQ(status.issue, AiSidecarRuntimeIssue::kRuntimeCrashed);
  EXPECT_NE(status.stderr_tail.find("self-exit"), std::string::npos);
}

TEST(AiSidecarRuntimeServiceTest, StopKillsHungRuntime) {
  AiSidecarRuntimeService service(std::make_shared<FakeAiSidecarRuntimeClient>());
  auto                    options = BaseOptions();
  options.extra_arguments         = {"--ignore-terminate", "--sleep-ms", "30000"};
  options.graceful_stop_timeout   = std::chrono::milliseconds(1);
  options.kill_timeout            = std::chrono::milliseconds(1000);

  ASSERT_TRUE(service.StartAndWait(options));
  service.Stop();
  const auto status = service.Status();
  EXPECT_EQ(status.state, AiSidecarRuntimeState::kStopped);
  EXPECT_EQ(status.issue, AiSidecarRuntimeIssue::kNone);
}

TEST(AiSidecarRuntimeServiceTest, RuntimeArgumentsCarryModelAndDeviceConfiguration) {
  AiSidecarRuntimeService service(std::make_shared<FakeAiSidecarRuntimeClient>());
  const auto record_path = std::filesystem::temp_directory_path() / "semantic_runtime_args.txt";
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

TEST(AiSidecarRuntimeServiceTest, ModelManagerListsProfilesViaRuntimeService) {
  auto                    client = std::make_shared<FakeAiSidecarRuntimeClient>();
  AiSidecarRuntimeService service(client);

  auto                    options = BaseOptions();
  options.extra_arguments         = {"--sleep-ms", "30000"};

  ASSERT_TRUE(service.StartAndWait(options));

  std::string error;
  const auto  profiles =
      service.ListModelProfiles("C:/models", std::chrono::milliseconds(100), &error);
  ASSERT_EQ(profiles.size(), 2u) << error;
  EXPECT_EQ(profiles[1].profile_id, "jina-clip-v2-int8-multilingual");
  EXPECT_EQ(profiles[1].language, "multilingual");
  EXPECT_EQ(profiles[1].embedding_dimension, 512u);
  EXPECT_EQ(profiles[1].native_embedding_dimension, 1024u);
  EXPECT_EQ(profiles[1].embedding_transform, "matryoshka_truncate_then_l2_normalize");

  service.Stop();
}

TEST(AiSidecarRuntimeServiceTest, ListsCapabilitiesViaRuntimeService) {
  auto                    client = std::make_shared<FakeAiSidecarRuntimeClient>();
  AiSidecarRuntimeService service(client);

  auto                    options = BaseOptions();
  options.extra_arguments         = {"--sleep-ms", "30000"};

  ASSERT_TRUE(service.StartAndWait(options));

  std::string error;
  const auto  capabilities = service.ListCapabilities(std::chrono::milliseconds(2000), &error);
  ASSERT_EQ(capabilities.size(), 1u) << error;
  const auto& capability = capabilities[0];
  EXPECT_EQ(capability.task_id, "semantic.embed_*");
  EXPECT_EQ(capability.provider_id, "local");
  EXPECT_EQ(capability.model_id, "test/mobileclip");
  EXPECT_FALSE(capability.requires_credential);
  EXPECT_TRUE(capability.supports_cancel);
  EXPECT_TRUE(capability.supports_batch);
  ASSERT_EQ(capability.input_kinds.size(), 2u);
  EXPECT_EQ(capability.input_kinds[0], 2);  // AI_INPUT_IMAGE
  EXPECT_EQ(capability.input_kinds[1], 3);  // AI_INPUT_THUMBNAIL
  ASSERT_EQ(capability.output_kinds.size(), 1u);
  EXPECT_EQ(capability.output_kinds[0], 1);  // AI_OUTPUT_EMBEDDING

  service.Stop();
}

TEST(AiSidecarRuntimeServiceTest,
     RegisterCredentialReturnsHandleWithoutLeakingSecretIntoProcessArgs) {
  auto                    client = std::make_shared<FakeAiSidecarRuntimeClient>();
  AiSidecarRuntimeService service(client);

  const auto              record_path =
      std::filesystem::temp_directory_path() / "ai_sidecar_credential_args.txt";
  std::filesystem::remove(record_path);

  auto options            = BaseOptions();
  options.extra_arguments = {"--record-args", record_path.string(), "--sleep-ms", "30000"};

  ASSERT_TRUE(service.StartAndWait(options));

  // A distinctive, never-used-elsewhere secret. It must travel only over the gRPC loopback
  // to the (fake) vault and never appear in process args or any captured runtime log surface.
  const std::string secret = "sk-DO-NOT-LEAK-7c3f9a1e-b2d4";
  std::string       handle;
  std::string       error;
  EXPECT_TRUE(service.RegisterCredential("remote", secret, /*ttl_ms=*/0,
                                         std::chrono::milliseconds(2000), &handle, &error));
  EXPECT_EQ(handle, "fake-credential-handle");
  EXPECT_TRUE(error.empty()) << error;

  service.Stop();

  std::ifstream in(record_path);
  ASSERT_TRUE(in.is_open());
  const std::string args((std::istreambuf_iterator<char>(in)), std::istreambuf_iterator<char>());
  in.close();
  // BuildArguments never sees the secret, so the recorded launch args must not contain it.
  EXPECT_EQ(args.find(secret), std::string::npos) << args;

  // Nor may it leak into any status/log surface the host captures from the child.
  const auto status = service.Status();
  EXPECT_EQ(status.message.find(secret), std::string::npos) << status.message;
  EXPECT_EQ(status.stdout_tail.find(secret), std::string::npos) << status.stdout_tail;
  EXPECT_EQ(status.stderr_tail.find(secret), std::string::npos) << status.stderr_tail;

  std::filesystem::remove(record_path);
}

TEST(AiSidecarRuntimeServiceTest, RevokeCredentialDelegatesWhenReadyAndIsNoOpWhenStopped) {
  auto                    client = std::make_shared<FakeAiSidecarRuntimeClient>();
  AiSidecarRuntimeService service(client);

  bool        revoked = true;
  std::string error;
  EXPECT_TRUE(
      service.RevokeCredential("fake-credential-handle", std::chrono::milliseconds(2000),
                               &revoked, &error));
  EXPECT_FALSE(revoked);
  EXPECT_EQ(client->RevokeCalls(), 0);

  auto options            = BaseOptions();
  options.extra_arguments = {"--sleep-ms", "30000"};
  ASSERT_TRUE(service.StartAndWait(options));

  EXPECT_TRUE(
      service.RevokeCredential("fake-credential-handle", std::chrono::milliseconds(2000),
                               &revoked, &error));
  EXPECT_TRUE(revoked) << error;
  EXPECT_EQ(client->RevokeCalls(), 1);
  EXPECT_EQ(client->LastRevokedHandle(), "fake-credential-handle");
  service.Stop();
}

TEST(AiSidecarRuntimeServiceTest, CancelTaskMapsResponseFromClient) {
  auto                    client = std::make_shared<FakeAiSidecarRuntimeClient>();
  AiSidecarRuntimeService service(client);

  auto                    options = BaseOptions();
  options.extra_arguments         = {"--sleep-ms", "30000"};

  ASSERT_TRUE(service.StartAndWait(options));

  std::string error;
  bool        cancelled = false;
  EXPECT_TRUE(
      service.CancelTask("fake-in-flight", std::chrono::milliseconds(2000), &cancelled, &error));
  EXPECT_TRUE(cancelled) << error;
  EXPECT_TRUE(error.empty()) << error;

  // Pre-seed cancelled=true so a no-op client would leave it set; the fake must overwrite it
  // with false for an unknown request_id, proving the response is propagated, not defaulted.
  cancelled = true;
  EXPECT_TRUE(service.CancelTask("unknown-request-id", std::chrono::milliseconds(2000), &cancelled,
                                 &error));
  EXPECT_FALSE(cancelled);
  EXPECT_TRUE(error.empty()) << error;

  service.Stop();
}

// Phase 5d: DescribeImage delegates to the client when the runtime is ready.
TEST(AiSidecarRuntimeServiceTest, DescribeImageDelegatesToClient) {
  auto                    client = std::make_shared<FakeAiSidecarRuntimeClient>();
  AiSidecarRuntimeService service(client);
  auto                    options = BaseOptions();
  options.extra_arguments         = {"--sleep-ms", "30000"};
  ASSERT_TRUE(service.StartAndWait(options));

  ImageAnalysisRequest req;
  req.request_id        = "ia-describe-1";
  req.image_bytes       = {0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10};
  req.image_format_hint = "image/jpeg;max_edge=1024";
  req.provider_id       = "openrouter";
  req.model_id          = "qwen/qwen3.7-plus";
  req.rendition.kind    = "thumbnail";
  req.rendition.width   = 1024;
  req.rendition.height  = 683;
  req.rendition.bytes   = req.image_bytes.size();

  const auto result = service.DescribeImage(req, std::chrono::milliseconds(5000));
  EXPECT_TRUE(result.ok) << result.error;
  EXPECT_EQ(result.request_id, "ia-describe-1");
  EXPECT_FALSE(result.caption.empty());
  EXPECT_FALSE(result.tags.empty());
  EXPECT_EQ(client->DescribeImageCalls(), 1);
  service.Stop();
}

// Phase 5d: DescribeImage fails fast without delegating when the runtime is not ready
// (sidecar startup stays on demand; no API key / binary required for this path).
TEST(AiSidecarRuntimeServiceTest, DescribeImageRespectsReadyGuard) {
  auto                    client = std::make_shared<FakeAiSidecarRuntimeClient>();
  AiSidecarRuntimeService service(client);  // not started -> not ready
  ImageAnalysisRequest    req;
  req.request_id = "ia-describe-2";
  const auto     result = service.DescribeImage(req, std::chrono::milliseconds(5000));
  EXPECT_FALSE(result.ok);
  EXPECT_FALSE(result.error.empty());
  EXPECT_EQ(client->DescribeImageCalls(), 0);
}

TEST(AiSidecarRuntimeServiceTest, ScoreImageRespectsReadyGuard) {
  auto                    client = std::make_shared<FakeAiSidecarRuntimeClient>();
  AiSidecarRuntimeService service(client);  // not started -> not ready
  ImageAnalysisRequest    req;
  req.request_id = "ia-score-2";
  req.rubric_id  = "alcedo-default-v1";
  const auto     result = service.ScoreImage(req, std::chrono::milliseconds(5000));
  EXPECT_FALSE(result.ok);
  EXPECT_FALSE(result.error.empty());
  EXPECT_EQ(client->ScoreImageCalls(), 0);
}

TEST(AiSidecarRuntimeServiceTest, ListModelsDelegatesToClient) {
  auto                    client = std::make_shared<FakeAiSidecarRuntimeClient>();
  client->SetListModelsCanned({
      AiDiscoveredModel{"gpt-4o", "GPT-4o", "opencode_go_openai"},
      AiDiscoveredModel{"gpt-4o-mini", "GPT-4o mini", "opencode_go_openai"},
  });
  AiSidecarRuntimeService service(client);
  auto                    options = BaseOptions();
  options.extra_arguments         = {"--sleep-ms", "30000"};
  ASSERT_TRUE(service.StartAndWait(options));

  const auto result =
      service.ListModels("opencode_go_openai", "fake-credential-handle",
                         std::chrono::milliseconds(5000));

  EXPECT_TRUE(result.ok) << result.error;
  ASSERT_EQ(result.models.size(), 2u);
  EXPECT_EQ(result.models[0].model_id, "gpt-4o");
  EXPECT_EQ(client->ListModelsCalls(), 1);
  service.Stop();
}

TEST(AiSidecarRuntimeServiceTest, ListModelsRespectsReadyGuard) {
  auto                    client = std::make_shared<FakeAiSidecarRuntimeClient>();
  AiSidecarRuntimeService service(client);

  const auto result =
      service.ListModels("opencode_go_openai", "fake-credential-handle",
                         std::chrono::milliseconds(5000));

  EXPECT_FALSE(result.ok);
  EXPECT_FALSE(result.error.empty());
  EXPECT_EQ(client->ListModelsCalls(), 0);
}

TEST(AiSidecarRuntimeServiceTest, EmptyModelRootUsesEnvironmentFallback) {
  AiSidecarRuntimeService service(std::make_shared<FakeAiSidecarRuntimeClient>());
  const auto              record_path =
      std::filesystem::temp_directory_path() / "semantic_runtime_env_model_args.txt";
  const auto model_root =
      std::filesystem::temp_directory_path() / "semantic_runtime_env_model_root";
  std::filesystem::remove(record_path);
  std::filesystem::create_directories(model_root);

  ScopedModelRootEnv model_root_env(model_root.string());

  auto               options = BaseOptions();
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

TEST(AiSidecarRuntimeServiceTest, EmbedTextV2ReturnsCannedViaV2Path) {
  auto client = std::make_shared<FakeAiSidecarRuntimeClient>();
  AiSidecarRuntimeService service(client);

  auto            options = BaseOptions();
  options.extra_arguments = {"--sleep-ms", "30000"};

  ASSERT_TRUE(service.StartAndWait(options));
  const auto result =
      service.EmbedText("text-v2", "a quiet portrait", std::chrono::milliseconds(100));
  EXPECT_TRUE(result.ok);
  EXPECT_EQ(result.request_id, "text-v2");
  EXPECT_EQ(result.embedding, (std::vector<float>{1.0f, 0.0f}));
  // v2 path taken: v2 called once, v1 not called.
  EXPECT_EQ(client->EmbedTextV2Calls(), 1);
  EXPECT_EQ(client->EmbedTextV1Calls(), 0);
  service.Stop();
}

TEST(AiSidecarRuntimeServiceTest, EmbedTextV2FallsBackToV1WhenV2Unsupported) {
  auto client = std::make_shared<FakeAiSidecarRuntimeClient>();
  client->SetV2Supported(false);
  AiSidecarRuntimeService service(client);

  auto            options = BaseOptions();
  options.extra_arguments = {"--sleep-ms", "30000"};

  ASSERT_TRUE(service.StartAndWait(options));
  const auto result =
      service.EmbedText("text-fb", "a quiet portrait", std::chrono::milliseconds(100));
  EXPECT_TRUE(result.ok);
  EXPECT_EQ(result.request_id, "text-fb");
  // v2 attempted (signals unavailable), then v1 fallback serves the call.
  EXPECT_EQ(client->EmbedTextV2Calls(), 1);
  EXPECT_EQ(client->EmbedTextV1Calls(), 1);
  service.Stop();
}

TEST(AiSidecarRuntimeServiceTest, EmbedImageBatchV2ReturnsCannedViaV2Path) {
  auto client = std::make_shared<FakeAiSidecarRuntimeClient>();
  AiSidecarRuntimeService service(client);

  auto            options = BaseOptions();
  options.extra_arguments = {"--sleep-ms", "30000"};

  ASSERT_TRUE(service.StartAndWait(options));

  SemanticImageEmbeddingRequest req_a;
  req_a.request_id  = "img-a";
  req_a.rgba8_image = std::vector<uint8_t>(4, 0);
  req_a.format_hint = "rgba8:1x1";
  SemanticImageEmbeddingRequest req_b;
  req_b.request_id  = "img-b";
  req_b.rgba8_image = std::vector<uint8_t>(4, 0);
  req_b.format_hint = "rgba8:1x1";

  const auto results = service.EmbedImageBatch({req_a, req_b}, std::chrono::milliseconds(100));
  ASSERT_EQ(results.size(), 2u);
  EXPECT_EQ(results[0].request_id, "img-a");
  EXPECT_TRUE(results[0].ok);
  EXPECT_EQ(results[0].embedding, (std::vector<float>{0.0f, 1.0f}));
  EXPECT_EQ(results[1].request_id, "img-b");
  // v2 path taken: v2 called once, v1 not called.
  EXPECT_EQ(client->EmbedImageBatchV2Calls(), 1);
  EXPECT_EQ(client->EmbedImageBatchV1Calls(), 0);
  service.Stop();
}

TEST(AiSidecarRuntimeServiceTest, EmbedImageBatchFallsBackToV1WhenV2Unsupported) {
  auto client = std::make_shared<FakeAiSidecarRuntimeClient>();
  client->SetV2Supported(false);
  AiSidecarRuntimeService service(client);

  auto            options = BaseOptions();
  options.extra_arguments = {"--sleep-ms", "30000"};

  ASSERT_TRUE(service.StartAndWait(options));

  SemanticImageEmbeddingRequest req;
  req.request_id  = "img-fb";
  req.rgba8_image = std::vector<uint8_t>(4, 0);
  req.format_hint = "rgba8:1x1";

  const auto results = service.EmbedImageBatch({req}, std::chrono::milliseconds(100));
  ASSERT_EQ(results.size(), 1u);
  EXPECT_EQ(results[0].request_id, "img-fb");
  EXPECT_TRUE(results[0].ok);
  // v2 attempted (signals unavailable), then v1 fallback serves the batch.
  EXPECT_EQ(client->EmbedImageBatchV2Calls(), 1);
  EXPECT_EQ(client->EmbedImageBatchV1Calls(), 1);
  service.Stop();
}

TEST(AiSidecarRuntimeServiceTest, EmbedImageBatchV2EchoesRequestIds) {
  auto client = std::make_shared<FakeAiSidecarRuntimeClient>();
  AiSidecarRuntimeService service(client);

  auto            options = BaseOptions();
  options.extra_arguments = {"--sleep-ms", "30000"};

  ASSERT_TRUE(service.StartAndWait(options));

  std::vector<SemanticImageEmbeddingRequest> requests;
  for (const char* id : {"echo-1", "echo-2", "echo-3"}) {
    SemanticImageEmbeddingRequest req;
    req.request_id  = id;
    req.rgba8_image = std::vector<uint8_t>(4, 0);
    req.format_hint = "rgba8:1x1";
    requests.push_back(std::move(req));
  }

  const auto results = service.EmbedImageBatch(requests, std::chrono::milliseconds(100));
  ASSERT_EQ(results.size(), 3u);
  EXPECT_EQ(results[0].request_id, "echo-1");
  EXPECT_EQ(results[1].request_id, "echo-2");
  EXPECT_EQ(results[2].request_id, "echo-3");
  for (const auto& r : results) {
    EXPECT_TRUE(r.ok);
  }
  // Per-item request_id preserved across the v2 path in one batch call; v1 not used.
  EXPECT_EQ(client->EmbedImageBatchV2Calls(), 1);
  EXPECT_EQ(client->EmbedImageBatchV1Calls(), 0);
  service.Stop();
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

  const auto results  = service.EmbedImageBatch({request}, std::chrono::milliseconds(120000));
  ASSERT_EQ(results.size(), 1u);
  EXPECT_EQ(results[0].request_id, "live-rgba8-image-1");
  EXPECT_TRUE(results[0].ok) << results[0].error;
  EXPECT_EQ(results[0].dimension, 512u);
  EXPECT_EQ(results[0].embedding.size(), 512u);

  service.Stop();
}

}  // namespace alcedo
