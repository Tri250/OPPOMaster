//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include <gtest/gtest.h>

#include <atomic>
#include <chrono>
#include <cstdlib>
#include <filesystem>
#include <fstream>
#include <string>
#include <thread>

#include "app/project_service.hpp"
#include "app/semantic_runtime_service.hpp"

namespace alcedo {
namespace {

class FakeSemanticRuntimeClient final : public ISemanticRuntimeClient {
 public:
  explicit FakeSemanticRuntimeClient(bool ready = true) : ready_(ready) {}

  auto Ping(const std::string& endpoint, std::chrono::milliseconds timeout,
            std::string* error) -> bool override {
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
                    SemanticRuntimeModelInfo* info, std::string* error) -> bool override {
    (void)endpoint;
    (void)timeout;
    (void)error;
    info->model_id = "test/mobileclip";
    info->revision = "rev-a";
    info->embedding_dimension = 512;
    info->image_size = 256;
    info->provider = "fake";
    info->model_root = "test-model-root";
    info->prototype_config_hash = "hash-a";
    return true;
  }

  auto GetRuntimeStatus(const std::string& endpoint, std::chrono::milliseconds timeout,
                        SemanticRuntimeRemoteStatus* status, std::string* error) -> bool override {
    (void)endpoint;
    (void)timeout;
    (void)error;
    status->state = "ready";
    status->provider = "fake";
    status->image_batch_cap = 8;
    status->image_batch_wait_ms = 2;
    status->uptime_ms = 42;
    return true;
  }

  auto EmbedText(const std::string& endpoint, const std::string& request_id,
                 const std::string& text, std::chrono::milliseconds timeout)
      -> SemanticEmbeddingResult override {
    (void)endpoint;
    (void)text;
    (void)timeout;
    SemanticEmbeddingResult result;
    result.request_id = request_id;
    result.embedding = {1.0f, 0.0f};
    result.dimension = 2;
    result.model_name = "test/mobileclip";
    result.ok = true;
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
    result.embedding = {0.0f, 1.0f};
    result.dimension = 2;
    result.model_name = "test/mobileclip";
    result.ok = true;
    return result;
  }

  auto EmbedImageBatch(const std::string& endpoint,
                       const std::vector<SemanticImageEmbeddingRequest>& requests,
                       std::chrono::milliseconds timeout)
      -> std::vector<SemanticEmbeddingResult> override {
    (void)endpoint;
    (void)timeout;
    std::vector<SemanticEmbeddingResult> results;
    results.reserve(requests.size());
    for (const auto& request : requests) {
      SemanticEmbeddingResult result;
      result.request_id = request.request_id;
      result.embedding = {0.0f, 1.0f};
      result.dimension = 2;
      result.model_name = "test/mobileclip";
      result.ok = true;
      results.push_back(std::move(result));
    }
    return results;
  }

  void SetReady(bool ready) { ready_.store(ready); }
  auto PingCount() const -> int { return ping_count_.load(); }

 private:
  std::atomic<bool> ready_;
  std::atomic<int>  ping_count_{0};
};

auto FakeRuntimePath() -> std::filesystem::path {
  return std::filesystem::path(ALCEDO_SEMANTIC_FAKE_RUNTIME_PATH);
}

auto BaseOptions() -> SemanticRuntimeOptions {
  SemanticRuntimeOptions options;
  options.runtime_binary = FakeRuntimePath();
  options.model_root = std::filesystem::temp_directory_path() / "semantic_runtime_test_model";
  options.model_id = "test/mobileclip";
  options.revision = "rev-a";
  options.device = "cpu";
  options.batch_cap = 8;
  options.batch_wait_ms = 2;
  options.startup_timeout = std::chrono::milliseconds(1000);
  options.health_poll_interval = std::chrono::milliseconds(20);
  options.graceful_stop_timeout = std::chrono::milliseconds(100);
  options.kill_timeout = std::chrono::milliseconds(1000);
  return options;
}

}  // namespace

TEST(SemanticRuntimeServiceTest, StartStopReportsReadyAndStopped) {
  auto client = std::make_shared<FakeSemanticRuntimeClient>();
  SemanticRuntimeService service(client);

  auto options = BaseOptions();
  options.extra_arguments = {"--sleep-ms", "30000"};

  ASSERT_TRUE(service.StartAndWait(options));
  auto status = service.Status();
  EXPECT_EQ(status.state, SemanticRuntimeState::kReady);
  EXPECT_EQ(status.issue, SemanticRuntimeIssue::kNone);
  EXPECT_FALSE(status.endpoint.empty());
  EXPECT_GT(status.process_id, 0);
  ASSERT_TRUE(status.model_info.has_value());
  EXPECT_EQ(status.model_info->embedding_dimension, 512U);
  ASSERT_TRUE(status.remote_status.has_value());
  EXPECT_EQ(status.remote_status->provider, "fake");

  const auto text_result =
      service.EmbedText("text-1", "a quiet portrait", std::chrono::milliseconds(100));
  EXPECT_TRUE(text_result.ok);
  EXPECT_EQ(text_result.request_id, "text-1");

  service.Stop();
  status = service.Status();
  EXPECT_EQ(status.state, SemanticRuntimeState::kStopped);
  EXPECT_EQ(status.issue, SemanticRuntimeIssue::kNone);
  EXPECT_EQ(status.process_id, 0);
}

TEST(SemanticRuntimeServiceTest, MissingBinaryFailsBeforeProcessStart) {
  SemanticRuntimeService service(std::make_shared<FakeSemanticRuntimeClient>());
  auto options = BaseOptions();
  options.runtime_binary = std::filesystem::temp_directory_path() / "missing_alcedo_mind.exe";

  EXPECT_FALSE(service.StartAndWait(options));
  const auto status = service.Status();
  EXPECT_EQ(status.state, SemanticRuntimeState::kFailed);
  EXPECT_EQ(status.issue, SemanticRuntimeIssue::kBinaryMissing);
}

TEST(SemanticRuntimeServiceTest, RuntimeExitBeforeReadyIsReported) {
  SemanticRuntimeService service(std::make_shared<FakeSemanticRuntimeClient>(false));
  auto options = BaseOptions();
  options.extra_arguments = {"--exit-now", "--exit-code", "7"};

  EXPECT_FALSE(service.StartAndWait(options));
  const auto status = service.Status();
  EXPECT_EQ(status.state, SemanticRuntimeState::kFailed);
  EXPECT_EQ(status.issue, SemanticRuntimeIssue::kRuntimeCrashed);
  EXPECT_NE(status.stderr_tail.find(""), std::string::npos);
}

TEST(SemanticRuntimeServiceTest, ReadyRuntimeSelfExitBecomesUiVisibleFailure) {
  SemanticRuntimeService service(std::make_shared<FakeSemanticRuntimeClient>());
  auto options = BaseOptions();
  options.extra_arguments = {"--exit-after-ms", "100", "--exit-code", "9"};

  ASSERT_TRUE(service.StartAndWait(options));
  std::this_thread::sleep_for(std::chrono::milliseconds(250));
  const auto status = service.Status();
  EXPECT_EQ(status.state, SemanticRuntimeState::kFailed);
  EXPECT_EQ(status.issue, SemanticRuntimeIssue::kRuntimeCrashed);
  EXPECT_NE(status.stderr_tail.find("self-exit"), std::string::npos);
}

TEST(SemanticRuntimeServiceTest, StopKillsHungRuntime) {
  SemanticRuntimeService service(std::make_shared<FakeSemanticRuntimeClient>());
  auto options = BaseOptions();
  options.extra_arguments = {"--ignore-terminate", "--sleep-ms", "30000"};
  options.graceful_stop_timeout = std::chrono::milliseconds(1);
  options.kill_timeout = std::chrono::milliseconds(1000);

  ASSERT_TRUE(service.StartAndWait(options));
  service.Stop();
  const auto status = service.Status();
  EXPECT_EQ(status.state, SemanticRuntimeState::kStopped);
  EXPECT_EQ(status.issue, SemanticRuntimeIssue::kNone);
}

TEST(SemanticRuntimeServiceTest, RuntimeArgumentsCarryModelAndDeviceConfiguration) {
  SemanticRuntimeService service(std::make_shared<FakeSemanticRuntimeClient>());
  const auto record_path = std::filesystem::temp_directory_path() / "semantic_runtime_args.txt";
  std::filesystem::remove(record_path);

  auto options = BaseOptions();
  options.model_root = std::filesystem::path("C:/models/mobileclip");
  options.model_id = "repo/model:s2";
  options.revision = "abc123";
  options.device = "directml:0";
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

TEST(SemanticRuntimeServiceTest, ProjectServiceOwnsStoppedRuntimeService) {
  const auto db_path = std::filesystem::temp_directory_path() / "semantic_runtime_project.db";
  const auto meta_path = std::filesystem::temp_directory_path() / "semantic_runtime_project.json";
  std::filesystem::remove(db_path);
  std::filesystem::remove(meta_path);

  {
    ProjectService project(db_path, meta_path, ProjectOpenMode::kCreateNew);
    auto runtime = project.GetSemanticRuntimeService();
    ASSERT_NE(runtime, nullptr);
    const auto status = runtime->Status();
    EXPECT_EQ(status.state, SemanticRuntimeState::kStopped);
  }

  std::filesystem::remove(db_path);
  std::filesystem::remove(meta_path);
}

TEST(SemanticRuntimeServiceLiveTest, DefaultGrpcClientEmbedsRawRgba8AgainstRustRuntime) {
  const char* runtime_path_env = std::getenv("ALCEDO_SEMANTIC_LIVE_RUNTIME_PATH");
  const char* model_root_env   = std::getenv("ALCEDO_SEMANTIC_LIVE_MODEL_ROOT");
  if (runtime_path_env == nullptr || model_root_env == nullptr) {
    GTEST_SKIP() << "Set ALCEDO_SEMANTIC_LIVE_RUNTIME_PATH and "
                    "ALCEDO_SEMANTIC_LIVE_MODEL_ROOT to run the live Rust runtime smoke.";
  }

  SemanticRuntimeService service;
  SemanticRuntimeOptions options;
  options.runtime_binary = std::filesystem::path(runtime_path_env);
  options.model_root = std::filesystem::path(model_root_env);
  options.model_id = "plhery/mobileclip2-onnx:s2";
  options.revision = "ba95759a5bdbaca53e9111e2550a76ec09c8fd9e";
  options.device = "cpu";
  options.batch_cap = 8;
  options.batch_wait_ms = 2;
  options.startup_timeout = std::chrono::milliseconds(60000);
  options.health_poll_interval = std::chrono::milliseconds(100);
  options.graceful_stop_timeout = std::chrono::milliseconds(1000);
  options.kill_timeout = std::chrono::milliseconds(2000);

  ASSERT_TRUE(service.StartAndWait(options)) << service.Status().message;
  const auto status = service.Status();
  ASSERT_TRUE(status.model_info.has_value());
  EXPECT_EQ(status.model_info->model_id, options.model_id);
  EXPECT_EQ(status.model_info->revision, options.revision);
  EXPECT_EQ(status.model_info->embedding_dimension, 512u);
  EXPECT_EQ(status.model_info->image_size, 256u);

  std::vector<uint8_t> rgba8(256u * 256u * 4u);
  for (size_t i = 0; i < rgba8.size(); i += 4) {
    rgba8[i] = 64;
    rgba8[i + 1] = 128;
    rgba8[i + 2] = 192;
    rgba8[i + 3] = 255;
  }

  SemanticImageEmbeddingRequest request;
  request.request_id = "live-rgba8-image-1";
  request.rgba8_image = std::move(rgba8);
  request.format_hint = "rgba8:256x256";

  const auto results = service.EmbedImageBatch({request}, std::chrono::milliseconds(120000));
  ASSERT_EQ(results.size(), 1u);
  EXPECT_EQ(results[0].request_id, "live-rgba8-image-1");
  EXPECT_TRUE(results[0].ok) << results[0].error;
  EXPECT_EQ(results[0].dimension, 512u);
  EXPECT_EQ(results[0].embedding.size(), 512u);

  service.Stop();
}

}  // namespace alcedo
