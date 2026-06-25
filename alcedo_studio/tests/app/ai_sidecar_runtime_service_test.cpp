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
                        std::vector<AiSidecarCapability>* capabilities,
                        std::string* error) -> bool override {
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
      capability.task_id              = "semantic.embed_*";
      capability.provider_id          = "local";
      capability.model_id             = "test/mobileclip";
      capability.input_kinds          = {2, 3};   // AI_INPUT_IMAGE, AI_INPUT_THUMBNAIL
      capability.output_kinds         = {1};      // AI_OUTPUT_EMBEDDING
      capability.supports_batch       = true;
      capability.supports_cancel      = true;
      capability.requires_credential  = false;
      capability.max_payload_bytes    = 0;
      capabilities->push_back(std::move(capability));
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

  void SetReady(bool ready) { ready_.store(ready); }
  void SetModelInfoReady(bool ready) { model_info_ready_.store(ready); }
  auto PingCount() const -> int { return ping_count_.load(); }

 private:
  std::atomic<bool> ready_;
  std::atomic<bool> model_info_ready_{true};
  std::atomic<int>  ping_count_{0};
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
  auto                   client = std::make_shared<FakeAiSidecarRuntimeClient>();
  AiSidecarRuntimeService service(client);

  auto                   options = BaseOptions();
  options.extra_arguments        = {"--sleep-ms", "30000"};

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
  auto                   options = BaseOptions();
  options.runtime_binary = std::filesystem::temp_directory_path() / "missing_alcedo_mind.exe";

  EXPECT_FALSE(service.StartAndWait(options));
  const auto status = service.Status();
  EXPECT_EQ(status.state, AiSidecarRuntimeState::kFailed);
  EXPECT_EQ(status.issue, AiSidecarRuntimeIssue::kBinaryMissing);
}

TEST(AiSidecarRuntimeServiceTest, RuntimeExitBeforeReadyIsReported) {
  AiSidecarRuntimeService service(std::make_shared<FakeAiSidecarRuntimeClient>(false));
  auto                   options = BaseOptions();
  options.extra_arguments        = {"--exit-now", "--exit-code", "7"};

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

  auto                   options = BaseOptions();
  options.extra_arguments        = {"--sleep-ms", "30000"};
  options.startup_timeout        = std::chrono::milliseconds(120);
  options.health_poll_interval   = std::chrono::milliseconds(20);

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

  auto                   options = BaseOptions();
  options.extra_arguments        = {"--sleep-ms", "30000"};
  options.require_model_info     = false;

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
  auto                   options = BaseOptions();
  options.extra_arguments        = {"--exit-after-ms", "100", "--exit-code", "9"};

  ASSERT_TRUE(service.StartAndWait(options));
  std::this_thread::sleep_for(std::chrono::milliseconds(250));
  const auto status = service.Status();
  EXPECT_EQ(status.state, AiSidecarRuntimeState::kFailed);
  EXPECT_EQ(status.issue, AiSidecarRuntimeIssue::kRuntimeCrashed);
  EXPECT_NE(status.stderr_tail.find("self-exit"), std::string::npos);
}

TEST(AiSidecarRuntimeServiceTest, StopKillsHungRuntime) {
  AiSidecarRuntimeService service(std::make_shared<FakeAiSidecarRuntimeClient>());
  auto                   options = BaseOptions();
  options.extra_arguments        = {"--ignore-terminate", "--sleep-ms", "30000"};
  options.graceful_stop_timeout  = std::chrono::milliseconds(1);
  options.kill_timeout           = std::chrono::milliseconds(1000);

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
  auto                   client = std::make_shared<FakeAiSidecarRuntimeClient>();
  AiSidecarRuntimeService service(client);

  auto                   options = BaseOptions();
  options.extra_arguments        = {"--sleep-ms", "30000"};

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
  auto                   client = std::make_shared<FakeAiSidecarRuntimeClient>();
  AiSidecarRuntimeService service(client);

  auto                   options = BaseOptions();
  options.extra_arguments        = {"--sleep-ms", "30000"};

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
  EXPECT_EQ(capability.input_kinds[0], 2);   // AI_INPUT_IMAGE
  EXPECT_EQ(capability.input_kinds[1], 3);   // AI_INPUT_THUMBNAIL
  ASSERT_EQ(capability.output_kinds.size(), 1u);
  EXPECT_EQ(capability.output_kinds[0], 1);  // AI_OUTPUT_EMBEDDING

  service.Stop();
}

TEST(AiSidecarRuntimeServiceTest, EmptyModelRootUsesEnvironmentFallback) {
  AiSidecarRuntimeService service(std::make_shared<FakeAiSidecarRuntimeClient>());
  const auto             record_path =
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
