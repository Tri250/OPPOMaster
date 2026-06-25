//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/ai_sidecar_runtime_service.hpp"

#include <grpcpp/create_channel.h>
#include <grpcpp/security/credentials.h>

#include <QCoreApplication>
#include <QHostAddress>
#include <QMetaObject>
#include <QStringList>
#include <QTcpServer>
#include <QThread>
#include <QUuid>
#include <algorithm>
#include <array>
#include <chrono>
#include <sstream>
#include <thread>
#include <vector>

#include "ai_common.pb.h"
#include "ai_runtime.grpc.pb.h"
#include "image_analysis.grpc.pb.h"
#include "semantic.grpc.pb.h"
#include "utils/diagnostics/app_logging.hpp"

#ifdef _WIN32
#include <windows.h>
#endif

namespace alcedo {
namespace {

constexpr size_t kLogTailBytes              = 16 * 1024;
constexpr auto   kAiSidecarRuntimeBinaryEnv = "ALCEDO_MIND_BINARY";
constexpr auto   kSemanticModelRootEnv      = "ALCEDO_MIND_MODEL_ROOT";
constexpr auto   kDefaultModelDirectory     = "model";
#ifdef _WIN32
constexpr auto kAiSidecarRuntimeBinaryName = "alcedo_mind.exe";
#else
constexpr auto kAiSidecarRuntimeBinaryName = "alcedo_mind";
#endif

auto TailAppend(std::string* target, const QByteArray& bytes) -> void {
  target->append(bytes.constData(), static_cast<size_t>(bytes.size()));
  if (target->size() > kLogTailBytes) {
    target->erase(0, target->size() - kLogTailBytes);
  }
}

auto BuildEndpoint(const std::string& host, uint16_t port) -> std::string {
  return host + ":" + std::to_string(port);
}

auto DefaultRuntimeBinary() -> std::filesystem::path {
  const QByteArray env_binary = qgetenv(kAiSidecarRuntimeBinaryEnv);
  if (!env_binary.isEmpty()) {
    return std::filesystem::path(env_binary.constData());
  }

  const auto app_dir = QCoreApplication::applicationDirPath();
#ifdef _WIN32
  const auto app_path = std::filesystem::path(app_dir.toStdWString());
#else
  const auto app_path = std::filesystem::path(app_dir.toStdString());
#endif

  const auto append_ancestor_runtime_binaries = [](const std::filesystem::path&        start,
                                                   std::vector<std::filesystem::path>* candidates) {
    if (start.empty()) {
      return;
    }

    std::error_code ec;
    auto            current = std::filesystem::absolute(start, ec);
    if (ec) {
      current = start;
    }
    while (!current.empty()) {
      candidates->push_back(current / "rust" / "puerh_mind" / "target" / "release" /
                            kAiSidecarRuntimeBinaryName);
      candidates->push_back(current / "rust" / "puerh_mind" / "target" / "debug" /
                            kAiSidecarRuntimeBinaryName);
      const auto parent = current.parent_path();
      if (parent == current) {
        break;
      }
      current = parent;
    }
  };

  std::vector<std::filesystem::path> candidates;
  candidates.push_back(app_path / kAiSidecarRuntimeBinaryName);
  append_ancestor_runtime_binaries(app_path, &candidates);
  std::error_code ec;
  append_ancestor_runtime_binaries(std::filesystem::current_path(ec), &candidates);

  for (const auto& candidate : candidates) {
    if (std::filesystem::exists(candidate, ec) && !ec &&
        std::filesystem::is_regular_file(candidate, ec) && !ec) {
      return candidate;
    }
  }
  return app_path / kAiSidecarRuntimeBinaryName;
}

auto DefaultRuntimeModelRoot() -> std::filesystem::path {
  const QByteArray env_root = qgetenv(kSemanticModelRootEnv);
  if (!env_root.isEmpty()) {
    return std::filesystem::path(env_root.constData());
  }

  const auto app_dir = QCoreApplication::applicationDirPath();
#ifdef _WIN32
  const auto app_path = std::filesystem::path(app_dir.toStdWString());
#else
  const auto app_path = std::filesystem::path(app_dir.toStdString());
#endif

  return app_path / kDefaultModelDirectory;
}

auto DeadlineFromNow(std::chrono::milliseconds timeout) -> std::chrono::system_clock::time_point {
  return std::chrono::system_clock::now() + timeout;
}

// Mints a fresh per-batch correlation id for v2 batch RPCs. header.request_id of a batch call
// correlates the whole batch; per-item request_id (the host's file-identity mapping) rides in
// each EmbedTextItemV2 / EmbedImageItemV2, unchanged from the v1 EmbeddingBatchItem shape.
auto MakeBatchRequestId() -> std::string {
  return QUuid::createUuid().toString(QUuid::WithoutBraces).toStdString();
}

// Fills the shared AiRequestHeader carried by every AiRuntimeService RPC for uniform request
// correlation. `credential_ref` is the opaque vault handle (empty for local/no-credential
// tasks); it is never key material. Centralizing header construction here keeps every RPC on
// the same redaction-safe path — no call site logs or echoes a secret through this helper.
auto FillAiRequestHeader(alcedo::ai::AiRequestHeader* header, const std::string& request_id,
                         const std::string& task_id, std::chrono::milliseconds timeout,
                         const std::string& credential_ref = {},
                         const std::string& trace_id = {}) -> void {
  header->set_request_id(request_id);
  header->set_task_id(task_id);
  header->set_timeout_ms(std::chrono::duration_cast<std::chrono::milliseconds>(timeout).count());
  if (!credential_ref.empty()) {
    header->set_credential_ref(credential_ref);
  }
  if (!trace_id.empty()) {
    header->set_trace_id(trace_id);
  }
}

auto SummarizeTextRequests(const std::vector<SemanticTextEmbeddingRequest>& requests,
                           size_t max_items = 12) -> QString {
  QStringList  parts;
  const size_t count = std::min(requests.size(), max_items);
  for (size_t i = 0; i < count; ++i) {
    parts << QString::fromStdString(requests[i].request_id);
  }
  if (requests.size() > max_items) {
    parts << QStringLiteral("...");
  }
  return parts.join(QLatin1Char(','));
}

auto SummarizeImageRequests(const std::vector<SemanticImageEmbeddingRequest>& requests,
                            size_t max_items = 12) -> QString {
  QStringList  parts;
  const size_t count = std::min(requests.size(), max_items);
  for (size_t i = 0; i < count; ++i) {
    parts << QStringLiteral("%1/%2/%3B")
                 .arg(QString::fromStdString(requests[i].request_id),
                      QString::fromStdString(requests[i].format_hint))
                 .arg(static_cast<qulonglong>(requests[i].rgba8_image.size()));
  }
  if (requests.size() > max_items) {
    parts << QStringLiteral("...");
  }
  return parts.join(QLatin1Char(','));
}

auto GrpcErrorMessage(const grpc::Status& status) -> std::string {
  if (!status.error_message().empty()) {
    return status.error_message();
  }
  return "gRPC call failed with code " + std::to_string(static_cast<int>(status.error_code()));
}

auto ToRuntimeModelInfo(const semantic::GetModelInfoResponse& response)
    -> AiSidecarRuntimeModelInfo {
  AiSidecarRuntimeModelInfo info;
  info.profile_id                 = response.profile_id();
  info.model_id                   = response.model_id();
  info.revision                   = response.revision();
  info.engine_profile_id          = response.engine_profile_id();
  info.language                   = response.language();
  info.embedding_dimension        = response.embedding_dimension();
  info.native_embedding_dimension = response.native_embedding_dimension();
  info.image_size                 = response.image_size();
  info.embedding_transform        = response.embedding_transform();
  info.provider                   = response.provider();
  info.model_root                 = response.model_root();
  info.prototype_config_hash      = response.prototype_config_hash();
  return info;
}

auto ToRuntimeStatus(const semantic::GetRuntimeStatusResponse& response)
    -> AiSidecarRuntimeRemoteStatus {
  AiSidecarRuntimeRemoteStatus status;
  status.state               = response.state();
  status.provider            = response.provider();
  status.image_batch_cap     = response.image_batch_cap();
  status.image_batch_wait_ms = response.image_batch_wait_ms();
  status.uptime_ms           = response.uptime_ms();
  return status;
}

auto ToAiSidecarCapability(const alcedo::ai::AiCapability& capability) -> AiSidecarCapability {
  AiSidecarCapability out;
  out.task_id     = capability.task_id();
  out.provider_id = capability.provider_id();
  out.model_id    = capability.model_id();
  out.input_kinds.reserve(static_cast<size_t>(capability.input_kinds_size()));
  for (const auto kind : capability.input_kinds()) {
    out.input_kinds.push_back(static_cast<int>(kind));
  }
  out.output_kinds.reserve(static_cast<size_t>(capability.output_kinds_size()));
  for (const auto kind : capability.output_kinds()) {
    out.output_kinds.push_back(static_cast<int>(kind));
  }
  out.supports_batch      = capability.supports_batch();
  out.supports_cancel     = capability.supports_cancel();
  out.requires_credential = capability.requires_credential();
  out.max_payload_bytes   = capability.max_payload_bytes();
  return out;
}

auto ToModelAssetInfo(const semantic::ModelAsset& response) -> SemanticModelAssetInfo {
  SemanticModelAssetInfo asset;
  asset.role        = response.role();
  asset.repo_id     = response.repo_id();
  asset.revision    = response.revision();
  asset.remote_path = response.remote_path();
  asset.local_path  = response.local_path();
  asset.size_bytes  = response.size_bytes();
  asset.sha256      = response.sha256();
  return asset;
}

auto ToModelProfileInfo(const semantic::ModelProfile& response) -> SemanticModelProfileInfo {
  SemanticModelProfileInfo profile;
  profile.profile_id                 = response.profile_id();
  profile.display_name               = response.display_name();
  profile.model_id                   = response.model_id();
  profile.revision                   = response.revision();
  profile.engine_profile_id          = response.engine_profile_id();
  profile.language                   = response.language();
  profile.embedding_dimension        = response.embedding_dimension();
  profile.native_embedding_dimension = response.native_embedding_dimension();
  profile.image_size                 = response.image_size();
  profile.installed                  = response.installed();
  profile.local_root                 = response.local_root();
  profile.status                     = response.status();
  profile.embedding_transform        = response.embedding_transform();
  profile.assets.reserve(static_cast<size_t>(response.assets_size()));
  for (const auto& asset : response.assets()) {
    profile.assets.push_back(ToModelAssetInfo(asset));
  }
  return profile;
}

auto ToResolvedModelManifest(const semantic::ResolvedModelManifest& response)
    -> SemanticResolvedModelManifest {
  SemanticResolvedModelManifest manifest;
  manifest.profile_id                 = response.profile_id();
  manifest.model_id                   = response.model_id();
  manifest.revision                   = response.revision();
  manifest.engine_profile_id          = response.engine_profile_id();
  manifest.language                   = response.language();
  manifest.embedding_dimension        = response.embedding_dimension();
  manifest.native_embedding_dimension = response.native_embedding_dimension();
  manifest.image_size                 = response.image_size();
  manifest.embedding_transform        = response.embedding_transform();
  manifest.model_root                 = response.model_root();
  manifest.assets.reserve(static_cast<size_t>(response.assets_size()));
  for (const auto& asset : response.assets()) {
    manifest.assets.push_back(ToModelAssetInfo(asset));
  }
  return manifest;
}

auto ToModelManagerResult(const semantic::ModelManagerResponse& response)
    -> SemanticModelManagerResult {
  SemanticModelManagerResult result;
  result.ok      = response.ok();
  result.status  = response.status();
  result.error   = response.error();
  result.profile = ToModelProfileInfo(response.profile());
  if (response.has_manifest()) {
    result.manifest = ToResolvedModelManifest(response.manifest());
  }
  return result;
}

auto ToEmbeddingResult(const semantic::EmbeddingResponse& response) -> SemanticEmbeddingResult {
  SemanticEmbeddingResult result;
  result.request_id = response.request_id();
  result.embedding.assign(response.embedding().begin(), response.embedding().end());
  result.dimension  = response.dimension();
  result.model_name = response.model_name();
  result.elapsed_ms = response.elapsed_ms();
  result.ok         = true;
  return result;
}

auto ToEmbeddingResult(const semantic::EmbeddingBatchItem& response) -> SemanticEmbeddingResult {
  SemanticEmbeddingResult result;
  result.request_id = response.request_id();
  result.embedding.assign(response.embedding().begin(), response.embedding().end());
  result.dimension  = response.dimension();
  result.model_name = response.model_name();
  result.elapsed_ms = response.elapsed_ms();
  result.ok         = response.ok();
  result.error      = response.error();
  return result;
}

// v2 embedding mappers (Phase 4). The v2 single response carries the correlation id in its
// AiResponseHeader (there is no body request_id), so the single-call mapper reads
// header().request_id(); the batch item mapper uses the item's own per-item request_id, exactly
// like the v1 item. The AiResponseHeader itself is not mapped into SemanticEmbeddingResult
// (which has no header field) — only the correlation id and embedding payload are.
auto ToEmbeddingResult(const semantic::EmbeddingResponseV2& response) -> SemanticEmbeddingResult {
  SemanticEmbeddingResult result;
  result.request_id = response.header().request_id();
  result.embedding.assign(response.embedding().begin(), response.embedding().end());
  result.dimension  = response.dimension();
  result.model_name = response.model_name();
  result.elapsed_ms = response.elapsed_ms();
  result.ok         = true;
  return result;
}

auto ToEmbeddingResult(const semantic::EmbeddingBatchItemV2& response) -> SemanticEmbeddingResult {
  SemanticEmbeddingResult result;
  result.request_id = response.request_id();
  result.embedding.assign(response.embedding().begin(), response.embedding().end());
  result.dimension  = response.dimension();
  result.model_name = response.model_name();
  result.elapsed_ms = response.elapsed_ms();
  result.ok         = response.ok();
  result.error      = response.error();
  return result;
}

// Phase 5d image-analysis mappers (proto/image_analysis.proto). The response carries the
// outcome in its AiResponseHeader (status / error_code / redacted error_message) plus the
// typed result body. ok follows the header status: AI_STATUS_OK => ok=true and the result
// body is mapped; anything else => ok=false with the header's redacted error message and
// the result body left empty (no active annotation — fail-closed, matching the Rust service).
auto ToImageAnalysisRendition(const alcedo::ai::RenditionMetadata& rendition)
    -> ImageAnalysisRendition {
  ImageAnalysisRendition out;
  out.kind     = rendition.kind();
  out.width    = rendition.width();
  out.height   = rendition.height();
  out.bytes    = rendition.bytes();
  out.max_edge = std::max(rendition.width(), rendition.height());
  return out;
}

auto ToImageAnalysisUsage(const alcedo::ai::UsageMetadata& usage) -> ImageAnalysisUsage {
  ImageAnalysisUsage out;
  out.input_tokens  = usage.input_tokens();
  out.output_tokens = usage.output_tokens();
  out.total_tokens  = usage.total_tokens();
  return out;
}

auto ToImageUnderstandingResult(const alcedo::ai::DescribeImageResponse& response,
                                const std::string& fallback_request_id,
                                const std::string& transport_error = {})
    -> ImageAnalysisUnderstandingResult {
  ImageAnalysisUnderstandingResult result;
  const auto& header = response.header();
  result.request_id          = header.request_id().empty() ? fallback_request_id : header.request_id();
  result.status              = static_cast<int>(header.status());
  result.error_code          = static_cast<int>(header.error_code());
  result.error               = transport_error.empty() ? header.error_message() : transport_error;
  result.provider            = header.provider();
  result.model_id            = header.model_id();
  result.elapsed_ms          = static_cast<uint64_t>(header.elapsed_ms());
  result.provider_request_id = response.provider_request_id();
  result.prompt_profile_id   = response.prompt_profile_id();
  result.rendition           = ToImageAnalysisRendition(response.rendition());
  if (response.has_usage()) {
    result.usage = ToImageAnalysisUsage(response.usage());
  }
  result.ok = (header.status() == alcedo::ai::AI_STATUS_OK);
  if (result.ok && response.has_result()) {
    const auto& body = response.result();
    result.caption    = body.caption();
    result.tags.assign(body.tags().begin(), body.tags().end());
    result.scene      = body.scene();
    result.confidence = body.confidence();
  }
  return result;
}

auto ToImageRatingResult(const alcedo::ai::ScoreImageResponse& response,
                         const std::string& fallback_request_id,
                         const std::string& transport_error = {}) -> ImageAnalysisRatingResult {
  ImageAnalysisRatingResult result;
  const auto& header = response.header();
  result.request_id          = header.request_id().empty() ? fallback_request_id : header.request_id();
  result.status              = static_cast<int>(header.status());
  result.error_code          = static_cast<int>(header.error_code());
  result.error               = transport_error.empty() ? header.error_message() : transport_error;
  result.provider            = header.provider();
  result.model_id            = header.model_id();
  result.elapsed_ms          = static_cast<uint64_t>(header.elapsed_ms());
  result.provider_request_id = response.provider_request_id();
  result.prompt_profile_id   = response.prompt_profile_id();
  result.rendition           = ToImageAnalysisRendition(response.rendition());
  if (response.has_usage()) {
    result.usage = ToImageAnalysisUsage(response.usage());
  }
  result.ok = (header.status() == alcedo::ai::AI_STATUS_OK);
  if (result.ok && response.has_result()) {
    const auto& body = response.result();
    result.scores.reserve(static_cast<size_t>(body.scores_size()));
    for (const auto& dim : body.scores()) {
      result.scores.push_back({dim.name(), dim.score()});
    }
    result.rubric_id      = body.rubric_id();
    result.rubric_version = body.rubric_version();
    result.reasons        = body.reasons();
    result.confidence     = body.confidence();
  }
  return result;
}

// Fills the proto request from the host DTO (shared by DescribeImage / ScoreImage). The
// credential_ref is the opaque vault handle; image_bytes is the encoded rendition. trace_id
// is set to request_id for single-call local correlation, matching the v2 embedding path.
auto FillImageAnalysisRequestProto(alcedo::ai::AiRequestHeader* header, const std::string& task_id,
                                   const ImageAnalysisRequest& req, std::chrono::milliseconds timeout)
    -> void {
  FillAiRequestHeader(header, req.request_id, task_id, timeout, req.credential_ref, req.request_id);
}

}  // namespace

auto ToString(AiSidecarRuntimeState state) -> const char* {
  switch (state) {
    case AiSidecarRuntimeState::kStopped:
      return "stopped";
    case AiSidecarRuntimeState::kStarting:
      return "starting";
    case AiSidecarRuntimeState::kReady:
      return "ready";
    case AiSidecarRuntimeState::kStopping:
      return "stopping";
    case AiSidecarRuntimeState::kFailed:
      return "failed";
  }
  return "unknown";
}

auto ToString(AiSidecarRuntimeIssue issue) -> const char* {
  switch (issue) {
    case AiSidecarRuntimeIssue::kNone:
      return "none";
    case AiSidecarRuntimeIssue::kBinaryMissing:
      return "binary_missing";
    case AiSidecarRuntimeIssue::kStartFailed:
      return "start_failed";
    case AiSidecarRuntimeIssue::kReadinessTimeout:
      return "readiness_timeout";
    case AiSidecarRuntimeIssue::kRuntimeExited:
      return "runtime_exited";
    case AiSidecarRuntimeIssue::kRuntimeCrashed:
      return "runtime_crashed";
    case AiSidecarRuntimeIssue::kStopTimedOut:
      return "stop_timed_out";
    case AiSidecarRuntimeIssue::kClientUnavailable:
      return "client_unavailable";
    case AiSidecarRuntimeIssue::kClientError:
      return "client_error";
  }
  return "unknown";
}

auto GrpcAiSidecarRuntimeClient::Ping(const std::string&        endpoint,
                                      std::chrono::milliseconds timeout, std::string* error)
    -> bool {
  auto                channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto                stub    = semantic::SemanticService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  semantic::PingRequest  request;
  semantic::PingResponse response;
  request.set_request_id("alcedo-runtime-ping");
  const auto status = stub->Ping(&context, request, &response);
  if (!status.ok()) {
    if (error) {
      *error = GrpcErrorMessage(status);
    }
    return false;
  }
  return true;
}

auto GrpcAiSidecarRuntimeClient::GetModelInfo(const std::string&         endpoint,
                                              std::chrono::milliseconds  timeout,
                                              AiSidecarRuntimeModelInfo* info, std::string* error)
    -> bool {
  auto                channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto                stub    = semantic::SemanticService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  semantic::GetModelInfoRequest  request;
  semantic::GetModelInfoResponse response;
  const auto                     status = stub->GetModelInfo(&context, request, &response);
  if (!status.ok()) {
    if (error) {
      *error = GrpcErrorMessage(status);
    }
    return false;
  }
  if (info) {
    *info = ToRuntimeModelInfo(response);
  }
  return true;
}

auto GrpcAiSidecarRuntimeClient::GetRuntimeStatus(const std::string&            endpoint,
                                                  std::chrono::milliseconds     timeout,
                                                  AiSidecarRuntimeRemoteStatus* status,
                                                  std::string*                  error) -> bool {
  auto                channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto                stub    = semantic::SemanticService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  semantic::GetRuntimeStatusRequest  request;
  semantic::GetRuntimeStatusResponse response;
  const auto status_result = stub->GetRuntimeStatus(&context, request, &response);
  if (!status_result.ok()) {
    if (error) {
      *error = GrpcErrorMessage(status_result);
    }
    return false;
  }
  if (status) {
    *status = ToRuntimeStatus(response);
  }
  return true;
}

auto GrpcAiSidecarRuntimeClient::ListCapabilities(const std::string&                endpoint,
                                                  std::chrono::milliseconds         timeout,
                                                  std::vector<AiSidecarCapability>* capabilities,
                                                  std::string*                      error) -> bool {
  auto                channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto                stub    = alcedo::ai::AiRuntimeService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  alcedo::ai::ListCapabilitiesRequest request;
  FillAiRequestHeader(request.mutable_header(), "alcedo-sidecar-list-capabilities",
                      "ai_runtime.list_capabilities", timeout);
  alcedo::ai::ListCapabilitiesResponse response;
  const auto status = stub->ListCapabilities(&context, request, &response);
  if (!status.ok()) {
    if (error) {
      *error = GrpcErrorMessage(status);
    }
    return false;
  }
  if (capabilities) {
    capabilities->clear();
    capabilities->reserve(static_cast<size_t>(response.capabilities_size()));
    for (const auto& capability : response.capabilities()) {
      capabilities->push_back(ToAiSidecarCapability(capability));
    }
  }
  return true;
}

auto GrpcAiSidecarRuntimeClient::RegisterCredential(
    const std::string& endpoint, std::chrono::milliseconds timeout, const std::string& provider_id,
    const std::string& secret, int64_t ttl_ms, std::string* handle, std::string* error) -> bool {
  auto                channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto                stub    = alcedo::ai::AiRuntimeService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  alcedo::ai::RegisterCredentialRequest request;
  // The secret is placed only on the loopback gRPC request body; it is never logged here.
  FillAiRequestHeader(request.mutable_header(), "alcedo-sidecar-register-credential",
                      "ai_runtime.register_credential", timeout);
  request.set_provider_id(provider_id);
  request.set_secret(secret);
  request.set_ttl_ms(ttl_ms);
  alcedo::ai::RegisterCredentialResponse response;
  const auto status = stub->RegisterCredential(&context, request, &response);
  if (!status.ok()) {
    if (error) {
      *error = GrpcErrorMessage(status);
    }
    return false;
  }
  if (handle) {
    *handle = response.credential_handle();
  }
  return true;
}

auto GrpcAiSidecarRuntimeClient::CancelTask(const std::string&        endpoint,
                                            std::chrono::milliseconds timeout,
                                            const std::string& request_id, bool* cancelled,
                                            std::string* error) -> bool {
  auto                channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto                stub    = alcedo::ai::AiRuntimeService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  alcedo::ai::CancelTaskRequest request;
  FillAiRequestHeader(request.mutable_header(), "alcedo-sidecar-cancel-task",
                      "ai_runtime.cancel_task", timeout);
  request.set_request_id(request_id);
  alcedo::ai::CancelTaskResponse response;
  const auto                     status = stub->CancelTask(&context, request, &response);
  if (!status.ok()) {
    if (error) {
      *error = GrpcErrorMessage(status);
    }
    return false;
  }
  if (cancelled) {
    *cancelled = response.cancelled();
  }
  return true;
}

auto GrpcAiSidecarRuntimeClient::ListModelProfiles(const std::string&        endpoint,
                                                   const std::string&        model_root,
                                                   std::chrono::milliseconds timeout,
                                                   std::string*              error)
    -> std::vector<SemanticModelProfileInfo> {
  auto                channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto                stub    = semantic::ModelManagerService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  semantic::ListModelProfilesRequest  request;
  semantic::ListModelProfilesResponse response;
  request.set_model_root(model_root);
  const auto status = stub->ListModelProfiles(&context, request, &response);
  if (!status.ok()) {
    if (error) {
      *error = GrpcErrorMessage(status);
    }
    return {};
  }
  std::vector<SemanticModelProfileInfo> profiles;
  profiles.reserve(static_cast<size_t>(response.profiles_size()));
  for (const auto& profile : response.profiles()) {
    profiles.push_back(ToModelProfileInfo(profile));
  }
  return profiles;
}

auto GrpcAiSidecarRuntimeClient::ListInstalledModels(const std::string&        endpoint,
                                                     const std::string&        model_root,
                                                     std::chrono::milliseconds timeout,
                                                     std::string*              error)
    -> std::vector<SemanticModelProfileInfo> {
  auto                channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto                stub    = semantic::ModelManagerService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  semantic::ListInstalledModelsRequest  request;
  semantic::ListInstalledModelsResponse response;
  request.set_model_root(model_root);
  const auto status = stub->ListInstalledModels(&context, request, &response);
  if (!status.ok()) {
    if (error) {
      *error = GrpcErrorMessage(status);
    }
    return {};
  }
  std::vector<SemanticModelProfileInfo> profiles;
  profiles.reserve(static_cast<size_t>(response.profiles_size()));
  for (const auto& profile : response.profiles()) {
    profiles.push_back(ToModelProfileInfo(profile));
  }
  return profiles;
}

auto GrpcAiSidecarRuntimeClient::ValidateModel(const std::string&        endpoint,
                                               const std::string&        profile_id,
                                               const std::string&        model_root,
                                               std::chrono::milliseconds timeout)
    -> SemanticModelManagerResult {
  auto                channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto                stub    = semantic::ModelManagerService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  semantic::ValidateModelRequest request;
  request.set_profile_id(profile_id);
  request.set_model_root(model_root);
  semantic::ModelManagerResponse response;
  const auto                     status = stub->ValidateModel(&context, request, &response);
  if (status.ok()) {
    return ToModelManagerResult(response);
  }
  SemanticModelManagerResult result;
  result.ok     = false;
  result.status = "error";
  result.error  = GrpcErrorMessage(status);
  return result;
}

auto GrpcAiSidecarRuntimeClient::DeleteModel(const std::string&        endpoint,
                                             const std::string&        profile_id,
                                             const std::string&        model_root,
                                             std::chrono::milliseconds timeout)
    -> SemanticModelManagerResult {
  auto                channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto                stub    = semantic::ModelManagerService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  semantic::DeleteModelRequest request;
  request.set_profile_id(profile_id);
  request.set_model_root(model_root);
  semantic::ModelManagerResponse response;
  const auto                     status = stub->DeleteModel(&context, request, &response);
  if (status.ok()) {
    return ToModelManagerResult(response);
  }
  SemanticModelManagerResult result;
  result.ok     = false;
  result.status = "error";
  result.error  = GrpcErrorMessage(status);
  return result;
}

auto IAiSidecarRuntimeClient::EmbedTextBatch(
    const std::string& endpoint, const std::vector<SemanticTextEmbeddingRequest>& requests,
    std::chrono::milliseconds timeout) -> std::vector<SemanticEmbeddingResult> {
  std::vector<SemanticEmbeddingResult> results;
  results.reserve(requests.size());
  for (const auto& request : requests) {
    results.push_back(EmbedText(endpoint, request.request_id, request.text, timeout));
  }
  return results;
}

// v2 default impls (Phase 4): a client that does not override v2 signals v2-unavailable so the
// service wrapper falls back to v1 transparently. The real gRPC client and the test fake both
// override these. Unused parameters are intentionally unnamed.
auto IAiSidecarRuntimeClient::EmbedTextV2(const std::string&, const std::string&,
                                           const std::string&, std::chrono::milliseconds,
                                           bool* v2_available) -> SemanticEmbeddingResult {
  if (v2_available) *v2_available = false;
  return {};
}

auto IAiSidecarRuntimeClient::EmbedImageV2(const std::string&, const std::string&,
                                           const std::vector<uint8_t>&, const std::string&,
                                           std::chrono::milliseconds, bool* v2_available)
    -> SemanticEmbeddingResult {
  if (v2_available) *v2_available = false;
  return {};
}

auto IAiSidecarRuntimeClient::EmbedTextBatchV2(
    const std::string&, const std::vector<SemanticTextEmbeddingRequest>&,
    std::chrono::milliseconds, bool* v2_available) -> std::vector<SemanticEmbeddingResult> {
  if (v2_available) *v2_available = false;
  return {};
}

auto IAiSidecarRuntimeClient::EmbedImageBatchV2(
    const std::string&, const std::vector<SemanticImageEmbeddingRequest>&,
    std::chrono::milliseconds, bool* v2_available) -> std::vector<SemanticEmbeddingResult> {
  if (v2_available) *v2_available = false;
  return {};
}

auto GrpcAiSidecarRuntimeClient::EmbedText(const std::string& endpoint,
                                           const std::string& request_id, const std::string& text,
                                           std::chrono::milliseconds timeout)
    -> SemanticEmbeddingResult {
  auto                channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto                stub    = semantic::SemanticService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  semantic::EmbedTextRequest request;
  request.set_request_id(request_id);
  request.set_text(text);
  semantic::EmbeddingResponse response;
  const auto                  status = stub->EmbedText(&context, request, &response);
  if (status.ok()) {
    return ToEmbeddingResult(response);
  }
  SemanticEmbeddingResult result;
  result.request_id = request_id;
  result.ok         = false;
  result.error      = GrpcErrorMessage(status);
  return result;
}

auto GrpcAiSidecarRuntimeClient::EmbedTextBatch(
    const std::string& endpoint, const std::vector<SemanticTextEmbeddingRequest>& requests,
    std::chrono::milliseconds timeout) -> std::vector<SemanticEmbeddingResult> {
  diag::TraceScope    trace(diag::semanticRpcLog(), QStringLiteral("semantic.rpc.embed_text_batch"),
                            QStringLiteral("endpoint=%1 count=%2 timeout_ms=%3 ids=%4")
                                .arg(QString::fromStdString(endpoint))
                                .arg(static_cast<qulonglong>(requests.size()))
                                .arg(timeout.count())
                                .arg(SummarizeTextRequests(requests)));
  auto                channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto                stub    = semantic::SemanticService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  semantic::EmbedTextBatchRequest request;
  for (const auto& input : requests) {
    auto* item = request.add_items();
    item->set_request_id(input.request_id);
    item->set_text(input.text);
    item->set_model_name(input.model_name);
  }

  semantic::EmbeddingBatchResponse     response;
  const auto                           status = stub->EmbedTextBatch(&context, request, &response);
  std::vector<SemanticEmbeddingResult> results;
  if (!status.ok()) {
    qCWarning(diag::semanticRpcLog).noquote()
        << QStringLiteral("semantic.rpc.embed_text_batch.failed endpoint=%1 count=%2 error=%3")
               .arg(QString::fromStdString(endpoint))
               .arg(static_cast<qulonglong>(requests.size()))
               .arg(QString::fromStdString(GrpcErrorMessage(status)));
    results.reserve(requests.size());
    for (const auto& input : requests) {
      SemanticEmbeddingResult result;
      result.request_id = input.request_id;
      result.ok         = false;
      result.error      = GrpcErrorMessage(status);
      results.push_back(std::move(result));
    }
    return results;
  }

  results.reserve(static_cast<size_t>(response.items_size()));
  for (const auto& item : response.items()) {
    results.push_back(ToEmbeddingResult(item));
  }
  qCInfo(diag::semanticRpcLog).noquote()
      << QStringLiteral("semantic.rpc.embed_text_batch.response endpoint=%1 count=%2")
             .arg(QString::fromStdString(endpoint))
             .arg(static_cast<qulonglong>(results.size()));
  return results;
}

auto GrpcAiSidecarRuntimeClient::EmbedImage(const std::string&          endpoint,
                                            const std::string&          request_id,
                                            const std::vector<uint8_t>& rgba8_image,
                                            const std::string&          format_hint,
                                            std::chrono::milliseconds   timeout)
    -> SemanticEmbeddingResult {
  auto                channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto                stub    = semantic::SemanticService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  semantic::EmbedImageRequest request;
  request.set_request_id(request_id);
  request.set_image_bytes(reinterpret_cast<const char*>(rgba8_image.data()), rgba8_image.size());
  request.set_image_format_hint(format_hint);
  semantic::EmbeddingResponse response;
  const auto                  status = stub->EmbedImage(&context, request, &response);
  if (status.ok()) {
    return ToEmbeddingResult(response);
  }
  SemanticEmbeddingResult result;
  result.request_id = request_id;
  result.ok         = false;
  result.error      = GrpcErrorMessage(status);
  return result;
}

auto GrpcAiSidecarRuntimeClient::DescribeImage(const std::string&          endpoint,
                                               const ImageAnalysisRequest& request,
                                               std::chrono::milliseconds   timeout)
    -> ImageAnalysisUnderstandingResult {
  auto channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto stub    = alcedo::ai::ImageAnalysisService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  alcedo::ai::DescribeImageRequest req;
  FillImageAnalysisRequestProto(req.mutable_header(), "image_understanding.describe", request,
                                timeout);
  req.set_image_bytes(reinterpret_cast<const char*>(request.image_bytes.data()),
                      request.image_bytes.size());
  req.set_image_format_hint(request.image_format_hint);
  req.set_provider_id(request.provider_id);
  req.set_model_id(request.model_id);
  req.set_prompt_profile_id(request.prompt_profile_id);
  auto* rendition = req.mutable_rendition();
  rendition->set_kind(request.rendition.kind);
  rendition->set_width(request.rendition.width);
  rendition->set_height(request.rendition.height);
  rendition->set_bytes(request.rendition.bytes);

  alcedo::ai::DescribeImageResponse response;
  const auto                        status = stub->DescribeImage(&context, req, &response);
  if (status.ok()) {
    return ToImageUnderstandingResult(response, request.request_id);
  }
  // grpc::UNIMPLEMENTED => the sidecar predates Phase 5d; map to a typed failed result
  // (status=UNIMPLEMENTED) rather than a fallback RPC, since image analysis has no v1 path.
  ImageAnalysisUnderstandingResult result;
  result.request_id   = request.request_id;
  result.ok           = false;
  result.status       = static_cast<int>(alcedo::ai::AI_STATUS_UNIMPLEMENTED);
  result.error        = GrpcErrorMessage(status);
  return result;
}

auto GrpcAiSidecarRuntimeClient::ScoreImage(const std::string&          endpoint,
                                            const ImageAnalysisRequest& request,
                                            std::chrono::milliseconds   timeout)
    -> ImageAnalysisRatingResult {
  auto channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto stub    = alcedo::ai::ImageAnalysisService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  alcedo::ai::ScoreImageRequest req;
  FillImageAnalysisRequestProto(req.mutable_header(), "image_rating.score", request, timeout);
  req.set_image_bytes(reinterpret_cast<const char*>(request.image_bytes.data()),
                      request.image_bytes.size());
  req.set_image_format_hint(request.image_format_hint);
  req.set_provider_id(request.provider_id);
  req.set_model_id(request.model_id);
  req.set_prompt_profile_id(request.prompt_profile_id);
  req.set_rubric_id(request.rubric_id);
  auto* rendition = req.mutable_rendition();
  rendition->set_kind(request.rendition.kind);
  rendition->set_width(request.rendition.width);
  rendition->set_height(request.rendition.height);
  rendition->set_bytes(request.rendition.bytes);

  alcedo::ai::ScoreImageResponse response;
  const auto                     status = stub->ScoreImage(&context, req, &response);
  if (status.ok()) {
    return ToImageRatingResult(response, request.request_id);
  }
  ImageAnalysisRatingResult result;
  result.request_id = request.request_id;
  result.ok         = false;
  result.status     = static_cast<int>(alcedo::ai::AI_STATUS_UNIMPLEMENTED);
  result.error      = GrpcErrorMessage(status);
  return result;
}

auto GrpcAiSidecarRuntimeClient::EmbedImageBatch(
    const std::string& endpoint, std::vector<SemanticImageEmbeddingRequest> requests,
    std::chrono::milliseconds timeout) -> std::vector<SemanticEmbeddingResult> {
  diag::TraceScope trace(diag::semanticRpcLog(), QStringLiteral("semantic.rpc.embed_image_batch"),
                         QStringLiteral("endpoint=%1 count=%2 timeout_ms=%3 ids=%4")
                             .arg(QString::fromStdString(endpoint))
                             .arg(static_cast<qulonglong>(requests.size()))
                             .arg(timeout.count())
                             .arg(SummarizeImageRequests(requests)));
  auto             channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto             stub    = semantic::SemanticService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  semantic::EmbedImageBatchRequest request;
  for (const auto& input : requests) {
    auto* item = request.add_items();
    item->set_request_id(input.request_id);
    item->set_image_bytes(reinterpret_cast<const char*>(input.rgba8_image.data()),
                          input.rgba8_image.size());
    item->set_image_format_hint(input.format_hint);
    item->set_model_name(input.model_name);
  }

  semantic::EmbeddingBatchResponse     response;
  const auto                           status = stub->EmbedImageBatch(&context, request, &response);
  std::vector<SemanticEmbeddingResult> results;
  if (!status.ok()) {
    qCWarning(diag::semanticRpcLog).noquote()
        << QStringLiteral("semantic.rpc.embed_image_batch.failed endpoint=%1 count=%2 error=%3")
               .arg(QString::fromStdString(endpoint))
               .arg(static_cast<qulonglong>(requests.size()))
               .arg(QString::fromStdString(GrpcErrorMessage(status)));
    results.reserve(requests.size());
    for (const auto& input : requests) {
      SemanticEmbeddingResult result;
      result.request_id = input.request_id;
      result.ok         = false;
      result.error      = GrpcErrorMessage(status);
      results.push_back(std::move(result));
    }
    return results;
  }

  results.reserve(static_cast<size_t>(response.items_size()));
  for (const auto& item : response.items()) {
    results.push_back(ToEmbeddingResult(item));
  }
  qCInfo(diag::semanticRpcLog).noquote()
      << QStringLiteral("semantic.rpc.embed_image_batch.response endpoint=%1 count=%2")
             .arg(QString::fromStdString(endpoint))
             .arg(static_cast<qulonglong>(results.size()));
  return results;
}

auto GrpcAiSidecarRuntimeClient::EmbedTextV2(const std::string& endpoint,
                                             const std::string& request_id, const std::string& text,
                                             std::chrono::milliseconds timeout, bool* v2_available)
    -> SemanticEmbeddingResult {
  auto                channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto                stub    = semantic::SemanticService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  semantic::EmbedTextRequestV2 request;
  FillAiRequestHeader(request.mutable_header(), request_id, "semantic.embed_text", timeout, "",
                      request_id);
  request.set_text(text);
  semantic::EmbeddingResponseV2 response;
  const auto                  status = stub->EmbedTextV2(&context, request, &response);
  if (status.ok()) {
    if (v2_available) *v2_available = true;
    return ToEmbeddingResult(response);
  }
  // UNIMPLEMENTED => the sidecar predates Phase 4; signal v2-unavailable so the service wrapper
  // falls back to v1. Any other grpc code (incl. DEADLINE_EXCEEDED) means v2 is present but the
  // call failed — synthesize a per-input failure and do not retry over v1.
  if (status.error_code() == grpc::StatusCode::UNIMPLEMENTED) {
    if (v2_available) *v2_available = false;
    return {};
  }
  if (v2_available) *v2_available = true;
  SemanticEmbeddingResult result;
  result.request_id = request_id;
  result.ok         = false;
  result.error      = GrpcErrorMessage(status);
  return result;
}

auto GrpcAiSidecarRuntimeClient::EmbedImageV2(const std::string&          endpoint,
                                              const std::string&          request_id,
                                              const std::vector<uint8_t>& rgba8_image,
                                              const std::string&          format_hint,
                                              std::chrono::milliseconds   timeout,
                                              bool*                       v2_available)
    -> SemanticEmbeddingResult {
  auto                channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto                stub    = semantic::SemanticService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  semantic::EmbedImageRequestV2 request;
  FillAiRequestHeader(request.mutable_header(), request_id, "semantic.embed_image", timeout, "",
                      request_id);
  request.set_image_bytes(reinterpret_cast<const char*>(rgba8_image.data()), rgba8_image.size());
  request.set_image_format_hint(format_hint);
  semantic::EmbeddingResponseV2 response;
  const auto                  status = stub->EmbedImageV2(&context, request, &response);
  if (status.ok()) {
    if (v2_available) *v2_available = true;
    return ToEmbeddingResult(response);
  }
  if (status.error_code() == grpc::StatusCode::UNIMPLEMENTED) {
    if (v2_available) *v2_available = false;
    return {};
  }
  if (v2_available) *v2_available = true;
  SemanticEmbeddingResult result;
  result.request_id = request_id;
  result.ok         = false;
  result.error      = GrpcErrorMessage(status);
  return result;
}

auto GrpcAiSidecarRuntimeClient::EmbedTextBatchV2(
    const std::string& endpoint, const std::vector<SemanticTextEmbeddingRequest>& requests,
    std::chrono::milliseconds timeout, bool* v2_available) -> std::vector<SemanticEmbeddingResult> {
  diag::TraceScope    trace(diag::semanticRpcLog(), QStringLiteral("semantic.rpc.embed_text_batch_v2"),
                            QStringLiteral("endpoint=%1 count=%2 timeout_ms=%3 ids=%4")
                                .arg(QString::fromStdString(endpoint))
                                .arg(static_cast<qulonglong>(requests.size()))
                                .arg(timeout.count())
                                .arg(SummarizeTextRequests(requests)));
  auto                channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto                stub    = semantic::SemanticService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  const auto           batch_id = MakeBatchRequestId();
  semantic::EmbedTextBatchRequestV2 request;
  FillAiRequestHeader(request.mutable_header(), batch_id, "semantic.embed_text_batch", timeout, "",
                      batch_id);
  for (const auto& input : requests) {
    auto* item = request.add_items();
    item->set_request_id(input.request_id);
    item->set_text(input.text);
    item->set_model_name(input.model_name);
  }

  semantic::EmbeddingBatchResponseV2     response;
  const auto                             status = stub->EmbedTextBatchV2(&context, request, &response);
  std::vector<SemanticEmbeddingResult> results;
  if (status.ok()) {
    if (v2_available) *v2_available = true;
    results.reserve(static_cast<size_t>(response.items_size()));
    for (const auto& item : response.items()) {
      results.push_back(ToEmbeddingResult(item));
    }
    qCInfo(diag::semanticRpcLog).noquote()
        << QStringLiteral("semantic.rpc.embed_text_batch_v2.response endpoint=%1 count=%2")
               .arg(QString::fromStdString(endpoint))
               .arg(static_cast<qulonglong>(results.size()));
    return results;
  }
  if (status.error_code() == grpc::StatusCode::UNIMPLEMENTED) {
    if (v2_available) *v2_available = false;
    return {};
  }
  if (v2_available) *v2_available = true;
  qCWarning(diag::semanticRpcLog).noquote()
      << QStringLiteral("semantic.rpc.embed_text_batch_v2.failed endpoint=%1 count=%2 error=%3")
             .arg(QString::fromStdString(endpoint))
             .arg(static_cast<qulonglong>(requests.size()))
             .arg(QString::fromStdString(GrpcErrorMessage(status)));
  results.reserve(requests.size());
  for (const auto& input : requests) {
    SemanticEmbeddingResult result;
    result.request_id = input.request_id;
    result.ok         = false;
    result.error      = GrpcErrorMessage(status);
    results.push_back(std::move(result));
  }
  return results;
}

auto GrpcAiSidecarRuntimeClient::EmbedImageBatchV2(
    const std::string& endpoint, const std::vector<SemanticImageEmbeddingRequest>& requests,
    std::chrono::milliseconds timeout, bool* v2_available) -> std::vector<SemanticEmbeddingResult> {
  diag::TraceScope trace(diag::semanticRpcLog(), QStringLiteral("semantic.rpc.embed_image_batch_v2"),
                         QStringLiteral("endpoint=%1 count=%2 timeout_ms=%3 ids=%4")
                             .arg(QString::fromStdString(endpoint))
                             .arg(static_cast<qulonglong>(requests.size()))
                             .arg(timeout.count())
                             .arg(SummarizeImageRequests(requests)));
  auto             channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto             stub    = semantic::SemanticService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  const auto          batch_id = MakeBatchRequestId();
  semantic::EmbedImageBatchRequestV2 request;
  FillAiRequestHeader(request.mutable_header(), batch_id, "semantic.embed_image_batch", timeout, "",
                      batch_id);
  for (const auto& input : requests) {
    auto* item = request.add_items();
    item->set_request_id(input.request_id);
    item->set_image_bytes(reinterpret_cast<const char*>(input.rgba8_image.data()),
                          input.rgba8_image.size());
    item->set_image_format_hint(input.format_hint);
    item->set_model_name(input.model_name);
  }

  semantic::EmbeddingBatchResponseV2     response;
  const auto                             status = stub->EmbedImageBatchV2(&context, request, &response);
  std::vector<SemanticEmbeddingResult> results;
  if (status.ok()) {
    if (v2_available) *v2_available = true;
    results.reserve(static_cast<size_t>(response.items_size()));
    for (const auto& item : response.items()) {
      results.push_back(ToEmbeddingResult(item));
    }
    qCInfo(diag::semanticRpcLog).noquote()
        << QStringLiteral("semantic.rpc.embed_image_batch_v2.response endpoint=%1 count=%2")
               .arg(QString::fromStdString(endpoint))
               .arg(static_cast<qulonglong>(results.size()));
    return results;
  }
  if (status.error_code() == grpc::StatusCode::UNIMPLEMENTED) {
    if (v2_available) *v2_available = false;
    return {};
  }
  if (v2_available) *v2_available = true;
  qCWarning(diag::semanticRpcLog).noquote()
      << QStringLiteral("semantic.rpc.embed_image_batch_v2.failed endpoint=%1 count=%2 error=%3")
             .arg(QString::fromStdString(endpoint))
             .arg(static_cast<qulonglong>(requests.size()))
             .arg(QString::fromStdString(GrpcErrorMessage(status)));
  results.reserve(requests.size());
  for (const auto& input : requests) {
    SemanticEmbeddingResult result;
    result.request_id = input.request_id;
    result.ok         = false;
    result.error      = GrpcErrorMessage(status);
    results.push_back(std::move(result));
  }
  return results;
}

AiSidecarRuntimeService::AiSidecarRuntimeService(std::shared_ptr<IAiSidecarRuntimeClient> client,
                                                 QObject*                                 parent)
    : QObject(parent), client_(std::move(client)) {
  status_.state   = AiSidecarRuntimeState::kStopped;
  status_.issue   = AiSidecarRuntimeIssue::kNone;
  status_.message = "Semantic runtime is stopped";

  connect(&process_, &QProcess::readyReadStandardOutput, this,
          [this]() { AppendStdout(process_.readAllStandardOutput()); });
  connect(&process_, &QProcess::readyReadStandardError, this,
          [this]() { AppendStderr(process_.readAllStandardError()); });
  connect(&process_, &QProcess::errorOccurred, this, [this](QProcess::ProcessError error) {
    if (error == QProcess::FailedToStart) {
      SetStatus(AiSidecarRuntimeState::kFailed, AiSidecarRuntimeIssue::kStartFailed,
                process_.errorString().toStdString());
    }
  });
}

AiSidecarRuntimeService::~AiSidecarRuntimeService() { StopForProjectClose(); }

auto AiSidecarRuntimeService::StartAndWait(const AiSidecarRuntimeOptions& options) -> bool {
  if (QThread::currentThread() != thread()) {
    bool result = false;
    QMetaObject::invokeMethod(
        this, [this, options, &result]() { result = StartAndWait(options); },
        Qt::BlockingQueuedConnection);
    return result;
  }

  if (IsRunning()) {
    const auto requested_root =
        options.model_root.empty() ? DefaultRuntimeModelRoot() : options.model_root;
    if (options_.model_id == options.model_id && options_.revision == options.revision &&
        options_.model_root == requested_root && options_.device == options.device &&
        options_.allow_download == options.allow_download &&
        options_.require_model_info == options.require_model_info) {
      return true;
    }
    Stop();
  }

  options_ = options;
  if (options_.runtime_binary.empty()) {
    options_.runtime_binary = DefaultRuntimeBinary();
  }
  if (options_.model_root.empty()) {
    options_.model_root = DefaultRuntimeModelRoot();
  }
  if (options_.port == 0) {
    options_.port = ChoosePort();
  }
  endpoint_ = BuildEndpoint(options_.host, options_.port);
  status_.stdout_tail.clear();
  status_.stderr_tail.clear();
  status_.model_info.reset();
  status_.remote_status.reset();

  std::error_code ec;
  if (!std::filesystem::exists(options_.runtime_binary, ec) || ec) {
    SetStatus(AiSidecarRuntimeState::kFailed, AiSidecarRuntimeIssue::kBinaryMissing,
              "Semantic runtime binary was not found: " + options_.runtime_binary.string());
    return false;
  }
  qCInfo(diag::semanticLog).noquote()
      << QStringLiteral(
             "semantic.runtime.start binary=%1 endpoint=%2 model_id=%3 revision=%4 "
             "model_root=%5 device=%6")
             .arg(QString::fromStdString(options_.runtime_binary.string()),
                  QString::fromStdString(endpoint_), QString::fromStdString(options_.model_id),
                  QString::fromStdString(options_.revision),
                  QString::fromStdString(options_.model_root.string()),
                  QString::fromStdString(options_.device));
  SetStatus(AiSidecarRuntimeState::kStarting, AiSidecarRuntimeIssue::kNone,
            "Starting semantic runtime");
  process_.setProgram(QString::fromStdString(options_.runtime_binary.string()));
  process_.setArguments(BuildArguments());
  process_.setProcessChannelMode(QProcess::SeparateChannels);
  process_.start();

  if (!process_.waitForStarted(static_cast<int>(options_.startup_timeout.count()))) {
    SetStatus(AiSidecarRuntimeState::kFailed, AiSidecarRuntimeIssue::kStartFailed,
              process_.errorString().toStdString());
    return false;
  }
  status_.process_id = static_cast<int64_t>(process_.processId());
  qCInfo(diag::semanticLog).noquote()
      << QStringLiteral("semantic.runtime.started pid=%1 endpoint=%2")
             .arg(status_.process_id)
             .arg(QString::fromStdString(endpoint_));
  AttachChildTreeCleanup();

  return WaitForReadiness();
}

void AiSidecarRuntimeService::Stop() {
  if (QThread::currentThread() != thread()) {
    QMetaObject::invokeMethod(this, [this]() { Stop(); }, Qt::BlockingQueuedConnection);
    return;
  }

  if (!IsRunning()) {
    SetStatus(AiSidecarRuntimeState::kStopped, AiSidecarRuntimeIssue::kNone,
              "Semantic runtime is stopped");
    return;
  }

  SetStatus(AiSidecarRuntimeState::kStopping, AiSidecarRuntimeIssue::kNone,
            "Stopping semantic runtime");
  qCInfo(diag::semanticLog).noquote() << QStringLiteral("semantic.runtime.stop pid=%1 endpoint=%2")
                                             .arg(status_.process_id)
                                             .arg(QString::fromStdString(endpoint_));
  process_.terminate();
  if (!process_.waitForFinished(static_cast<int>(options_.graceful_stop_timeout.count()))) {
    process_.kill();
    if (!process_.waitForFinished(static_cast<int>(options_.kill_timeout.count()))) {
      SetStatus(AiSidecarRuntimeState::kFailed, AiSidecarRuntimeIssue::kStopTimedOut,
                "Semantic runtime did not exit after kill request");
      return;
    }
  }
  ReleaseChildTreeCleanup();
  status_.process_id = 0;
  SetStatus(AiSidecarRuntimeState::kStopped, AiSidecarRuntimeIssue::kNone,
            "Semantic runtime is stopped");
}

void AiSidecarRuntimeService::StopForProjectClose() { Stop(); }

auto AiSidecarRuntimeService::Status() -> AiSidecarRuntimeStatusSnapshot {
  if (QThread::currentThread() != thread()) {
    AiSidecarRuntimeStatusSnapshot snapshot;
    QMetaObject::invokeMethod(
        this, [this, &snapshot]() { snapshot = Status(); }, Qt::BlockingQueuedConnection);
    return snapshot;
  }

  RefreshProcessExit();
  if (status_.state == AiSidecarRuntimeState::kReady && client_) {
    AiSidecarRuntimeRemoteStatus remote;
    std::string                  error;
    if (client_->GetRuntimeStatus(endpoint_, std::chrono::milliseconds(250), &remote, &error)) {
      status_.remote_status = remote;
    }
  }
  return status_;
}

auto AiSidecarRuntimeService::IsRunning() -> bool {
  if (QThread::currentThread() != thread()) {
    bool result = false;
    QMetaObject::invokeMethod(
        this, [this, &result]() { result = IsRunning(); }, Qt::BlockingQueuedConnection);
    return result;
  }

  RefreshProcessExit();
  return process_.state() != QProcess::NotRunning;
}

auto AiSidecarRuntimeService::ListModelProfiles(const std::string&        model_root,
                                                std::chrono::milliseconds timeout,
                                                std::string*              error)
    -> std::vector<SemanticModelProfileInfo> {
  if (status_.state != AiSidecarRuntimeState::kReady || !client_) {
    if (error) {
      *error = "semantic runtime is not ready";
    }
    return {};
  }
  return client_->ListModelProfiles(endpoint_, model_root, timeout, error);
}

auto AiSidecarRuntimeService::ListInstalledModels(const std::string&        model_root,
                                                  std::chrono::milliseconds timeout,
                                                  std::string*              error)
    -> std::vector<SemanticModelProfileInfo> {
  if (status_.state != AiSidecarRuntimeState::kReady || !client_) {
    if (error) {
      *error = "semantic runtime is not ready";
    }
    return {};
  }
  return client_->ListInstalledModels(endpoint_, model_root, timeout, error);
}

auto AiSidecarRuntimeService::ListCapabilities(std::chrono::milliseconds timeout,
                                               std::string*              error)
    -> std::vector<AiSidecarCapability> {
  if (status_.state != AiSidecarRuntimeState::kReady || !client_) {
    if (error) {
      *error = "ai sidecar runtime is not ready";
    }
    return {};
  }
  std::vector<AiSidecarCapability> capabilities;
  if (!client_->ListCapabilities(endpoint_, timeout, &capabilities, error)) {
    return {};
  }
  return capabilities;
}

auto AiSidecarRuntimeService::RegisterCredential(const std::string& provider_id,
                                                 const std::string& secret, int64_t ttl_ms,
                                                 std::chrono::milliseconds timeout,
                                                 std::string* handle, std::string* error) -> bool {
  if (status_.state != AiSidecarRuntimeState::kReady || !client_) {
    if (error) {
      *error = "ai sidecar runtime is not ready";
    }
    return false;
  }
  return client_->RegisterCredential(endpoint_, timeout, provider_id, secret, ttl_ms, handle,
                                     error);
}

auto AiSidecarRuntimeService::CancelTask(const std::string&        request_id,
                                         std::chrono::milliseconds timeout, bool* cancelled,
                                         std::string* error) -> bool {
  if (status_.state != AiSidecarRuntimeState::kReady || !client_) {
    if (error) {
      *error = "ai sidecar runtime is not ready";
    }
    return false;
  }
  return client_->CancelTask(endpoint_, timeout, request_id, cancelled, error);
}

auto AiSidecarRuntimeService::ValidateModel(const std::string&        profile_id,
                                            const std::string&        model_root,
                                            std::chrono::milliseconds timeout)
    -> SemanticModelManagerResult {
  if (status_.state != AiSidecarRuntimeState::kReady || !client_) {
    SemanticModelManagerResult result;
    result.ok     = false;
    result.status = "error";
    result.error  = "semantic runtime is not ready";
    return result;
  }
  return client_->ValidateModel(endpoint_, profile_id, model_root, timeout);
}

auto AiSidecarRuntimeService::DeleteModel(const std::string&        profile_id,
                                          const std::string&        model_root,
                                          std::chrono::milliseconds timeout)
    -> SemanticModelManagerResult {
  if (status_.state != AiSidecarRuntimeState::kReady || !client_) {
    SemanticModelManagerResult result;
    result.ok     = false;
    result.status = "error";
    result.error  = "semantic runtime is not ready";
    return result;
  }
  return client_->DeleteModel(endpoint_, profile_id, model_root, timeout);
}

auto AiSidecarRuntimeService::EmbedText(const std::string& request_id, const std::string& text,
                                        std::chrono::milliseconds timeout)
    -> SemanticEmbeddingResult {
  if (status_.state != AiSidecarRuntimeState::kReady || !client_) {
    SemanticEmbeddingResult result;
    result.request_id = request_id;
    result.ok         = false;
    result.error      = "semantic runtime is not ready";
    return result;
  }
  // Prefer v2 (shared AiRequestHeader control surface); fall back to v1 for sidecars that
  // predate Phase 4 (v2 returns *v2_available=false on grpc::UNIMPLEMENTED).
  bool v2_available = false;
  auto result = client_->EmbedTextV2(endpoint_, request_id, text, timeout, &v2_available);
  if (v2_available) {
    return result;
  }
  return client_->EmbedText(endpoint_, request_id, text, timeout);
}

auto AiSidecarRuntimeService::EmbedTextBatch(
    const std::vector<SemanticTextEmbeddingRequest>& requests, std::chrono::milliseconds timeout)
    -> std::vector<SemanticEmbeddingResult> {
  if (status_.state != AiSidecarRuntimeState::kReady || !client_) {
    std::vector<SemanticEmbeddingResult> results;
    results.reserve(requests.size());
    for (const auto& request : requests) {
      SemanticEmbeddingResult result;
      result.request_id = request.request_id;
      result.ok         = false;
      result.error      = "semantic runtime is not ready";
      results.push_back(std::move(result));
    }
    return results;
  }
  bool v2_available = false;
  auto results = client_->EmbedTextBatchV2(endpoint_, requests, timeout, &v2_available);
  if (v2_available) {
    return results;
  }
  return client_->EmbedTextBatch(endpoint_, requests, timeout);
}

auto AiSidecarRuntimeService::EmbedImage(const std::string&          request_id,
                                         const std::vector<uint8_t>& rgba8_image,
                                         const std::string&          format_hint,
                                         std::chrono::milliseconds   timeout)
    -> SemanticEmbeddingResult {
  if (status_.state != AiSidecarRuntimeState::kReady || !client_) {
    SemanticEmbeddingResult result;
    result.request_id = request_id;
    result.ok         = false;
    result.error      = "semantic runtime is not ready";
    return result;
  }
  bool v2_available = false;
  auto result =
      client_->EmbedImageV2(endpoint_, request_id, rgba8_image, format_hint, timeout, &v2_available);
  if (v2_available) {
    return result;
  }
  return client_->EmbedImage(endpoint_, request_id, rgba8_image, format_hint, timeout);
}

auto AiSidecarRuntimeService::EmbedImageBatch(std::vector<SemanticImageEmbeddingRequest> requests,
                                              std::chrono::milliseconds                  timeout)
    -> std::vector<SemanticEmbeddingResult> {
  if (status_.state != AiSidecarRuntimeState::kReady || !client_) {
    std::vector<SemanticEmbeddingResult> results;
    results.reserve(requests.size());
    for (const auto& request : requests) {
      SemanticEmbeddingResult result;
      result.request_id = request.request_id;
      result.ok         = false;
      result.error      = "semantic runtime is not ready";
      results.push_back(std::move(result));
    }
    return results;
  }
  bool v2_available = false;
  auto results = client_->EmbedImageBatchV2(endpoint_, requests, timeout, &v2_available);
  if (v2_available) {
    return results;
  }
  return client_->EmbedImageBatch(endpoint_, std::move(requests), timeout);
}

auto AiSidecarRuntimeService::DescribeImage(const ImageAnalysisRequest& request,
                                            std::chrono::milliseconds   timeout)
    -> ImageAnalysisUnderstandingResult {
  if (status_.state != AiSidecarRuntimeState::kReady || !client_) {
    ImageAnalysisUnderstandingResult result;
    result.request_id = request.request_id;
    result.ok         = false;
    result.status     = static_cast<int>(alcedo::ai::AI_STATUS_UNIMPLEMENTED);
    result.error      = "ai sidecar runtime is not ready";
    return result;
  }
  return client_->DescribeImage(endpoint_, request, timeout);
}

auto AiSidecarRuntimeService::ScoreImage(const ImageAnalysisRequest& request,
                                         std::chrono::milliseconds   timeout)
    -> ImageAnalysisRatingResult {
  if (status_.state != AiSidecarRuntimeState::kReady || !client_) {
    ImageAnalysisRatingResult result;
    result.request_id = request.request_id;
    result.ok         = false;
    result.status     = static_cast<int>(alcedo::ai::AI_STATUS_UNIMPLEMENTED);
    result.error      = "ai sidecar runtime is not ready";
    return result;
  }
  return client_->ScoreImage(endpoint_, request, timeout);
}

auto AiSidecarRuntimeService::StateName() const -> QString {
  return QString::fromLatin1(ToString(status_.state));
}

auto AiSidecarRuntimeService::IssueName() const -> QString {
  return QString::fromLatin1(ToString(status_.issue));
}

void AiSidecarRuntimeService::SetStatus(AiSidecarRuntimeState state, AiSidecarRuntimeIssue issue,
                                        std::string message) {
  status_.state    = state;
  status_.issue    = issue;
  status_.message  = std::move(message);
  status_.endpoint = endpoint_;
  qCInfo(diag::semanticLog).noquote()
      << QStringLiteral("semantic.runtime.status state=%1 issue=%2 endpoint=%3 message=%4")
             .arg(QString::fromLatin1(ToString(state)), QString::fromLatin1(ToString(issue)),
                  QString::fromStdString(endpoint_), QString::fromStdString(status_.message));
  emit statusChanged();
}

void AiSidecarRuntimeService::AppendStdout(const QByteArray& bytes) {
  TailAppend(&status_.stdout_tail, bytes);
  const QString text = QString::fromUtf8(bytes).trimmed();
  if (!text.isEmpty()) {
    qCInfo(diag::semanticLog).noquote()
        << QStringLiteral("semantic.runtime.stdout %1").arg(text.left(1000));
  }
}

void AiSidecarRuntimeService::AppendStderr(const QByteArray& bytes) {
  TailAppend(&status_.stderr_tail, bytes);
  const QString text = QString::fromUtf8(bytes).trimmed();
  if (!text.isEmpty()) {
    qCWarning(diag::semanticLog).noquote()
        << QStringLiteral("semantic.runtime.stderr %1").arg(text.left(1000));
  }
}

void AiSidecarRuntimeService::RefreshProcessExit() {
  if (process_.state() != QProcess::NotRunning) {
    process_.waitForFinished(0);
  }
  if (process_.state() != QProcess::NotRunning) {
    return;
  }
  if (status_.state != AiSidecarRuntimeState::kReady &&
      status_.state != AiSidecarRuntimeState::kStarting &&
      status_.state != AiSidecarRuntimeState::kStopping) {
    return;
  }

  ReleaseChildTreeCleanup();
  AppendStdout(process_.readAllStandardOutput());
  AppendStderr(process_.readAllStandardError());
  status_.process_id = 0;
  const bool crashed = process_.exitStatus() == QProcess::CrashExit || process_.exitCode() != 0;
  std::ostringstream message;
  message << "Semantic runtime exited with code " << process_.exitCode();
  SetStatus(
      AiSidecarRuntimeState::kFailed,
      crashed ? AiSidecarRuntimeIssue::kRuntimeCrashed : AiSidecarRuntimeIssue::kRuntimeExited,
      message.str());
}

auto AiSidecarRuntimeService::BuildArguments() const -> QStringList {
  QStringList args;
  args << "--host" << QString::fromStdString(options_.host);
  args << "--port" << QString::number(options_.port);
  if (!options_.model_root.empty()) {
    args << "--model-root" << QString::fromStdString(options_.model_root.string());
  }
  args << "--model-id" << QString::fromStdString(options_.model_id);
  if (!options_.revision.empty()) {
    args << "--revision" << QString::fromStdString(options_.revision);
  }
  if (!options_.hf_endpoint.empty()) {
    args << "--hf-endpoint" << QString::fromStdString(options_.hf_endpoint);
  }
  args << "--device" << QString::fromStdString(options_.device);
  args << (options_.allow_download ? "--allow-download" : "--no-download");
  args << "--batch-cap" << QString::number(options_.batch_cap);
  args << "--batch-wait-ms" << QString::number(options_.batch_wait_ms);
  args << "--max-message-bytes" << QString::number(options_.max_message_bytes);
  for (const auto& arg : options_.extra_arguments) {
    args << QString::fromStdString(arg);
  }
  return args;
}

auto AiSidecarRuntimeService::ChoosePort() const -> uint16_t {
  QTcpServer server;
  if (server.listen(QHostAddress::LocalHost, 0)) {
    const auto port = static_cast<uint16_t>(server.serverPort());
    server.close();
    return port;
  }
  return 50051;
}

auto AiSidecarRuntimeService::WaitForReadiness() -> bool {
  const auto  deadline = std::chrono::steady_clock::now() + options_.startup_timeout;
  std::string last_error;
  while (std::chrono::steady_clock::now() < deadline) {
    process_.waitForReadyRead(static_cast<int>(options_.health_poll_interval.count()));
    RefreshProcessExit();
    if (status_.state == AiSidecarRuntimeState::kFailed) {
      return false;
    }
    if (client_ && client_->Ping(endpoint_, options_.health_poll_interval, &last_error)) {
      AiSidecarRuntimeModelInfo info;
      std::string               info_error;
      if (client_->GetModelInfo(endpoint_, std::chrono::milliseconds(500), &info, &info_error)) {
        status_.model_info = info;
        SetStatus(AiSidecarRuntimeState::kReady, AiSidecarRuntimeIssue::kNone,
                  "Semantic runtime is ready");
        return true;
      } else if (!options_.require_model_info) {
        SetStatus(AiSidecarRuntimeState::kReady, AiSidecarRuntimeIssue::kNone,
                  "Semantic model manager is ready");
        return true;
      } else if (!info_error.empty()) {
        last_error      = info_error;
        status_.message = info_error;
      } else {
        last_error = "Semantic runtime responded but semantic model is not ready";
      }
    }
    std::this_thread::sleep_for(options_.health_poll_interval);
  }

  SetStatus(AiSidecarRuntimeState::kFailed, AiSidecarRuntimeIssue::kReadinessTimeout,
            last_error.empty() ? "Timed out waiting for semantic runtime readiness" : last_error);
  if (process_.state() != QProcess::NotRunning) {
    process_.terminate();
    if (!process_.waitForFinished(static_cast<int>(options_.graceful_stop_timeout.count()))) {
      process_.kill();
      process_.waitForFinished(static_cast<int>(options_.kill_timeout.count()));
    }
  }
  ReleaseChildTreeCleanup();
  status_.process_id = 0;
  return false;
}

void AiSidecarRuntimeService::AttachChildTreeCleanup() {
#ifdef _WIN32
  ReleaseChildTreeCleanup();
  HANDLE job = CreateJobObjectW(nullptr, nullptr);
  if (job == nullptr) {
    return;
  }
  JOBOBJECT_EXTENDED_LIMIT_INFORMATION info{};
  info.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE;
  if (!SetInformationJobObject(job, JobObjectExtendedLimitInformation, &info, sizeof(info))) {
    CloseHandle(job);
    return;
  }
  HANDLE process_handle = OpenProcess(PROCESS_SET_QUOTA | PROCESS_TERMINATE, FALSE,
                                      static_cast<DWORD>(process_.processId()));
  if (process_handle == nullptr) {
    CloseHandle(job);
    return;
  }
  if (!AssignProcessToJobObject(job, process_handle)) {
    CloseHandle(process_handle);
    CloseHandle(job);
    return;
  }
  CloseHandle(process_handle);
  job_object_ = job;
#endif
}

void AiSidecarRuntimeService::ReleaseChildTreeCleanup() {
#ifdef _WIN32
  if (job_object_ != nullptr) {
    CloseHandle(static_cast<HANDLE>(job_object_));
    job_object_ = nullptr;
  }
#endif
}

}  // namespace alcedo
