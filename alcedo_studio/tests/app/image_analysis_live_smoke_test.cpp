//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

// Env-gated live smoke for the Phase 5d/5e image-analysis module. Opens a real packed `.alcd`
// project, materializes one k1024 thumbnail, starts the real Rust sidecar (no CLIP model —
// `describe` uses the HTTP provider path), registers a real provider credential read from
// `.env.test`, and asks the live LLM to describe the image through `ImageAnalysisService`.
//
// Skipped unless ALL of these are set:
//   ALCEDO_IA_LIVE_RUNTIME_PATH     - absolute path to alcedo_mind(.exe)
//   ALCEDO_TEST_PACKED_PROJECT_PATH - absolute path to a packed .alcd project
//   ALCEDO_IA_LIVE_ENV_TEST_PATH    - absolute path to rust/puerh_mind/.env.test
// Optional:
//   ALCEDO_IA_LIVE_PROVIDER_ID      - "openrouter" (default) or "volcengine_ark"
//
// The API key is read from `.env.test` (ALCEDO_OPENROUTER_API_KEY / ALCEDO_VOLCENGINE_ARK_API_KEY)
// and is never printed, logged, or written to settings. The image itself is never inspected.

#include "app/ai_sidecar_runtime_service.hpp"
#include "app/album_browse_service.hpp"
#include "app/image_analysis_service.hpp"
#include "app/pipeline_service.hpp"
#include "app/project_package_service.hpp"
#include "app/project_service.hpp"
#include "app/thumbnail_service.hpp"
#include "app/thumbnail_types.hpp"
#include "edit/operators/operator_registeration.hpp"

#include <gtest/gtest.h>

#include <chrono>
#include <cstdint>
#include <cstdlib>
#include <filesystem>
#include <fstream>
#include <memory>
#include <string>

#include <QString>

namespace alcedo {
namespace {

// Minimal .env reader: returns the value for `KEY=VALUE` lines, ignoring blanks / `#`
// comments, a leading `export `, and surrounding quotes. Used only to read the API key the
// user pre-placed in .env.test; the value never leaves this test except into RegisterCredential.
auto EnvFileValue(const std::filesystem::path& path, const std::string& key) -> std::string {
  std::ifstream in(path);
  if (!in.is_open()) {
    return {};
  }
  std::string line;
  while (std::getline(in, line)) {
    if (!line.empty() && line.back() == '\r') {
      line.pop_back();
    }
    const auto first = line.find_first_not_of(" \t");
    if (first == std::string::npos || line[first] == '#') {
      continue;
    }
    auto key_pos = first;
    if (line.compare(first, 7, "export ") == 0) {
      key_pos = first + 7;
    }
    if (line.compare(key_pos, key.size(), key) != 0) {
      continue;
    }
    if (line.size() <= key_pos + key.size() || line[key_pos + key.size()] != '=') {
      continue;
    }
    std::string v = line.substr(key_pos + key.size() + 1);
    const auto a = v.find_first_not_of(" \t");
    const auto b = v.find_last_not_of(" \t");
    if (a == std::string::npos) {
      return {};
    }
    v = v.substr(a, b - a + 1);
    if (v.size() >= 2 && ((v.front() == '"' && v.back() == '"') ||
                          (v.front() == '\'' && v.back() == '\''))) {
      v = v.substr(1, v.size() - 2);
    }
    return v;
  }
  return {};
}

auto EnvOr(const char* name, const char* fallback) -> std::string {
  const char* v = std::getenv(name);
  return (v != nullptr && v[0] != '\0') ? std::string(v) : std::string(fallback);
}

TEST(ImageAnalysisLiveSmokeTest, DescribesOneImageFromPackedProject) {
  const char* runtime_env = std::getenv("ALCEDO_IA_LIVE_RUNTIME_PATH");
  const char* project_env = std::getenv("ALCEDO_TEST_PACKED_PROJECT_PATH");
  const char* envtest_env = std::getenv("ALCEDO_IA_LIVE_ENV_TEST_PATH");
  if (runtime_env == nullptr || project_env == nullptr || envtest_env == nullptr) {
    GTEST_SKIP() << "Set ALCEDO_IA_LIVE_RUNTIME_PATH, ALCEDO_TEST_PACKED_PROJECT_PATH, and "
                    "ALCEDO_IA_LIVE_ENV_TEST_PATH to run the live image-analysis smoke.";
  }

  // Populate the global OperatorFactory singleton, exactly as main.cpp:142 and every
  // pipeline-using test fixture do. Without this, ThumbnailService's render path builds a
  // CPUPipelineExecutor whose InitDefaultPipeline() calls OperatorFactory::Create() for each
  // default operator — and Create() returns nullptr for unregistered types. SetOperator()'s
  // 3-arg overload then dereferences the null op_ in SetGlobalParams, crashing the process
  // (0xC0000005). This is a test-harness requirement, not a Phase 5e concern.
  RegisterAllOperators();

  const std::string provider_id = EnvOr("ALCEDO_IA_LIVE_PROVIDER_ID", "openrouter");
  // Both `volcengine_ark` (Responses) and `volcengine_ark_coding` (Anthropic-compatible
  // Coding Plan) reuse the same Ark account credential; `openrouter` and any other
  // provider use the OpenRouter key.
  const std::string key_var = (provider_id.starts_with("volcengine"))
                                   ? "ALCEDO_VOLCENGINE_ARK_API_KEY"
                                   : "ALCEDO_OPENROUTER_API_KEY";
  std::string api_key = EnvFileValue(envtest_env, key_var);
  if (api_key.empty()) {
    if (const char* e = std::getenv(key_var.c_str())) {
      api_key = e;
    }
  }
  ASSERT_FALSE(api_key.empty())
      << "No API key for provider '" << provider_id << "' (" << key_var
      << ") in .env.test at " << envtest_env;

  // (1) Unpack the packed .alcd into a temp workspace.
  ProjectPackageService package_service;
  ASSERT_TRUE(package_service.IsPackedProjectPath(project_env))
      << "not a packed .alcd project: " << project_env;
  std::filesystem::path workspace_dir;
  QString                workspace_err;
  ASSERT_TRUE(package_service.CreateProjectWorkspace(QString::fromUtf8("ia_live_smoke"),
                                                    &workspace_dir, &workspace_err))
      << workspace_err.toStdString();
  std::filesystem::path db_path;
  std::filesystem::path meta_path;
  QString                unpack_err;
  ASSERT_TRUE(package_service.UnpackProjectToWorkspace(project_env, workspace_dir,
                                                      QString::fromUtf8("ia_live_smoke"), &db_path,
                                                      &meta_path, &unpack_err))
      << unpack_err.toStdString();

  // (2) Open the unpacked project and enumerate one image from the root folder.
  ProjectService project(db_path, meta_path, ProjectOpenMode::kLoadExisting);
  auto           browse = project.GetAlbumBrowseService();
  ASSERT_TRUE(browse != nullptr);
  const auto files = browse->ListFilesInFolderById(0);  // root
  ASSERT_FALSE(files.empty()) << "no image files in the packed project";
  const auto view = files.front();
  ASSERT_NE(view.image_id_, 0u) << "first entry is not a real image";

  // (3) Thumbnail materialization stack (mirrors thumbnail_service_test.cpp:753-755).
  const auto thumbnail_cache_root =
      std::filesystem::temp_directory_path() / "alcedo_ia_live_smoke_thumbcache";
  std::filesystem::create_directories(thumbnail_cache_root);
  auto pipeline_service = std::make_shared<PipelineMgmtService>(project.GetStorageService());
  auto thumbnail_service = std::make_shared<ThumbnailService>(
      project.GetSleeveService(), project.GetImagePoolService(), pipeline_service, nullptr,
      project.GetProjectUUID(), thumbnail_cache_root);

  // (4) Start the real sidecar without a CLIP model: `describe` is served over the HTTP
  // provider path, so require_model_info=false is sufficient (verified to boot with an
  // empty model-root and --no-download).
  auto runtime = std::make_shared<AiSidecarRuntimeService>();
  AiSidecarRuntimeOptions options;
  options.runtime_binary        = std::filesystem::path(runtime_env);
  options.model_root            = std::filesystem::temp_directory_path() / "alcedo_ia_live_smoke_modelroot";
  std::filesystem::create_directories(options.model_root);
  options.model_id              = "plhery/mobileclip2-onnx:s2";
  options.device                = "cpu";
  options.batch_cap             = 8;
  options.batch_wait_ms         = 2;
  options.max_message_bytes     = 16 * 1024 * 1024;
  options.allow_download        = false;
  options.require_model_info    = false;
  options.startup_timeout       = std::chrono::milliseconds(120000);
  options.health_poll_interval  = std::chrono::milliseconds(100);
  options.graceful_stop_timeout = std::chrono::milliseconds(2000);
  options.kill_timeout          = std::chrono::milliseconds(3000);
  ASSERT_TRUE(runtime->StartAndWait(options)) << runtime->Status().message;

  // (5) Wire the image-analysis module: real thumbnail provider + real sidecar client + gate.
  auto thumb_provider   = std::make_shared<ThumbnailServiceImageAnalysisProvider>(thumbnail_service);
  auto analysis_client  = std::make_shared<AiSidecarRuntimeImageAnalysisClient>(runtime);
  auto gate             = std::make_shared<ImageAnalysisInFlightGate>();
  ImageAnalysisService ia_service(thumb_provider, analysis_client, gate);

  ImageAnalysisOptions opts;
  opts.task                 = ImageAnalysisTask::kDescribe;
  opts.thumbnail_resolution = ThumbnailResolution::k1024;
  opts.jpeg_quality         = 90;
  opts.timeout              = std::chrono::milliseconds(120000);
  opts.provider_id          = provider_id;
  opts.credential.provider_id = provider_id;
  opts.credential.secret      = api_key;  // consumed by RegisterCredential, then cleared
  opts.temp_dir              = std::filesystem::temp_directory_path() / "alcedo_ia_live_smoke_enc";
  std::filesystem::create_directories(opts.temp_dir);
  opts.prefetch             = 1;

  auto job = ia_service.StartAnalysis({ImageAnalysisItem{view.element_id_, view.image_id_}}, opts,
                                      {}, {});
  job->Wait();
  runtime->Stop();

  auto results = job->Results();
  ASSERT_EQ(results.size(), 1u);
  const auto& r = results[0];
  ASSERT_EQ(r.status, ImageAnalysisItemStatus::kAnalyzed)
      << "item status not analyzed; error: " << r.error;
  ASSERT_TRUE(r.understanding.ok)
      << "understanding not ok; status=" << r.understanding.status
      << " error=" << r.understanding.error;
  ASSERT_FALSE(r.understanding.caption.empty())
      << "provider returned an empty caption; error=" << r.understanding.error;

  // Print the description (NOT the image). The secret must never appear in any field.
  std::cout << "\n[LIVE DESCRIBE] provider=" << provider_id
            << " model=" << r.understanding.model_id
            << " scene=\"" << r.understanding.scene << "\""
            << " tags=" << r.understanding.tags.size()
            << " confidence=" << r.understanding.confidence
            << " elapsed_ms=" << r.understanding.elapsed_ms
            << " tokens=" << r.understanding.usage.total_tokens
            << "\n  caption: " << r.understanding.caption << "\n  tags:";
  for (const auto& t : r.understanding.tags) {
    std::cout << " \"" << t << "\"";
  }
  std::cout << "\n";

  EXPECT_EQ(r.understanding.caption.find(api_key), std::string::npos);
  EXPECT_EQ(r.understanding.error.find(api_key), std::string::npos);
  EXPECT_EQ(r.understanding.scene.find(api_key), std::string::npos);
  for (const auto& t : r.understanding.tags) {
    EXPECT_EQ(t.find(api_key), std::string::npos);
  }

  // Best-effort cleanup of the temp workspace / caches (a failure here must not fail the test).
  std::error_code ec;
  std::filesystem::remove_all(workspace_dir, ec);
  std::filesystem::remove_all(thumbnail_cache_root, ec);
  std::filesystem::remove_all(options.model_root, ec);
  std::filesystem::remove_all(opts.temp_dir, ec);
}

}  // namespace
}  // namespace alcedo