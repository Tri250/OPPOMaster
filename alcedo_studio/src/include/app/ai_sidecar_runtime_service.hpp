//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QObject>
#include <QProcess>
#include <QString>
#include <chrono>
#include <cstdint>
#include <filesystem>
#include <memory>
#include <optional>
#include <string>
#include <vector>

namespace alcedo {

enum class AiSidecarRuntimeState : uint8_t {
  kStopped = 0,
  kStarting,
  kReady,
  kStopping,
  kFailed,
};

enum class AiSidecarRuntimeIssue : uint8_t {
  kNone = 0,
  kBinaryMissing,
  kStartFailed,
  kReadinessTimeout,
  kRuntimeExited,
  kRuntimeCrashed,
  kStopTimedOut,
  kClientUnavailable,
  kClientError,
};

struct AiSidecarRuntimeModelInfo {
  std::string profile_id;
  std::string model_id;
  std::string revision;
  std::string engine_profile_id;
  std::string language;
  uint32_t    embedding_dimension        = 0;
  uint32_t    native_embedding_dimension = 0;
  uint32_t    image_size                 = 0;
  std::string embedding_transform;
  std::string provider;
  std::string model_root;
  std::string prototype_config_hash;
};

struct AiSidecarRuntimeRemoteStatus {
  std::string state;
  std::string provider;
  uint32_t    image_batch_cap     = 0;
  uint32_t    image_batch_wait_ms = 0;
  uint64_t    uptime_ms           = 0;
};

struct SemanticModelAssetInfo {
  std::string role;
  std::string repo_id;
  std::string revision;
  std::string remote_path;
  std::string local_path;
  uint64_t    size_bytes = 0;
  std::string sha256;
};

struct SemanticModelProfileInfo {
  std::string                         profile_id;
  std::string                         display_name;
  std::string                         model_id;
  std::string                         revision;
  std::string                         engine_profile_id;
  std::string                         language;
  uint32_t                            embedding_dimension        = 0;
  uint32_t                            native_embedding_dimension = 0;
  uint32_t                            image_size                 = 0;
  bool                                installed                  = false;
  std::string                         local_root;
  std::string                         status;
  std::string                         embedding_transform;
  std::vector<SemanticModelAssetInfo> assets;
};

struct SemanticResolvedModelManifest {
  std::string                         profile_id;
  std::string                         model_id;
  std::string                         revision;
  std::string                         engine_profile_id;
  std::string                         language;
  uint32_t                            embedding_dimension        = 0;
  uint32_t                            native_embedding_dimension = 0;
  uint32_t                            image_size                 = 0;
  std::string                         embedding_transform;
  std::string                         model_root;
  std::vector<SemanticModelAssetInfo> assets;
};

struct SemanticModelManagerResult {
  bool                                         ok = false;
  std::string                                  status;
  std::string                                  error;
  SemanticModelProfileInfo                     profile;
  std::optional<SemanticResolvedModelManifest> manifest;
};

struct SemanticEmbeddingResult {
  std::string        request_id;
  std::vector<float> embedding;
  uint32_t           dimension = 0;
  std::string        model_name;
  uint64_t           elapsed_ms = 0;
  bool               ok         = false;
  std::string        error;
};

struct SemanticTextEmbeddingRequest {
  std::string request_id;
  std::string text;
  std::string model_name;
};

struct SemanticImageEmbeddingRequest {
  std::string          request_id;
  std::vector<uint8_t> rgba8_image;
  std::string          format_hint;
  std::string          model_name;
};

struct AiSidecarRuntimeStatusSnapshot {
  AiSidecarRuntimeState                       state = AiSidecarRuntimeState::kStopped;
  AiSidecarRuntimeIssue                       issue = AiSidecarRuntimeIssue::kNone;
  std::string                                 message;
  std::string                                 endpoint;
  int64_t                                     process_id = 0;
  std::string                                 stdout_tail;
  std::string                                 stderr_tail;
  std::optional<AiSidecarRuntimeModelInfo>    model_info;
  std::optional<AiSidecarRuntimeRemoteStatus> remote_status;
};

// Host-side mirror of `alcedo::ai::AiCapability` (proto/ai_common.proto). Plain struct so the
// header stays free of generated-proto includes; the .cpp hand-maps the proto fields here.
struct AiSidecarCapability {
  std::string      task_id;
  std::string      provider_id;
  std::string      model_id;
  std::vector<int> input_kinds;   // alcedo::ai::AiInputKind values
  std::vector<int> output_kinds;  // alcedo::ai::AiOutputKind values
  bool             supports_batch      = false;
  bool             supports_cancel     = false;
  bool             requires_credential = false;
  int64_t          max_payload_bytes   = 0;
};

// Phase 5d image-analysis DTOs (mirror of alcedo.ai messages in
// proto/image_analysis.proto). Plain structs so this header stays free of
// generated-proto includes; the .cpp hand-maps the proto fields here. status /
// error_code hold raw alcedo::ai::AiResponseStatus / AiErrorCode values (int),
// matching the input_kinds convention on AiSidecarCapability.

struct ImageAnalysisRendition {
  std::string kind;        // "thumbnail" | "preview" | "image"
  uint32_t    width        = 0;
  uint32_t    height       = 0;
  uint64_t    bytes        = 0;
  uint32_t    max_edge     = 0;  // host-recorded longest side actually sent
};

struct ImageAnalysisUsage {
  int64_t input_tokens  = 0;
  int64_t output_tokens = 0;
  int64_t total_tokens  = 0;
};

// Input to DescribeImage / ScoreImage. `credential_ref` is the opaque vault
// handle from RegisterCredential (never key material); `image_bytes` carries
// the encoded rendition (JPEG/PNG), NOT raw RGBA8.
struct ImageAnalysisRequest {
  std::string               request_id;
  std::vector<uint8_t>      image_bytes;
  std::string               image_format_hint;  // "image/jpeg;max_edge=1024" etc.
  ImageAnalysisRendition    rendition;
  std::string               provider_id;        // "" = sidecar default
  std::string               model_id;           // "" = provider default
  std::string               prompt_profile_id;
  std::string               credential_ref;     // vault handle; "" = no credential
  std::string               rubric_id;          // ScoreImage only; "" = provider default
};

struct ImageAnalysisUnderstandingResult {
  std::string                   request_id;
  bool                          ok            = false;
  int                           status        = 0;  // AiResponseStatus
  int                           error_code    = 0;  // AiErrorCode
  std::string                   error;
  std::string                   caption;
  std::vector<std::string>      tags;
  std::string                   scene;
  double                        confidence = 0.0;
  std::string                   provider;
  std::string                   model_id;
  std::string                   provider_request_id;
  std::string                   prompt_profile_id;
  ImageAnalysisRendition        rendition;
  ImageAnalysisUsage            usage;
  uint64_t                      elapsed_ms = 0;
};

// Result of image_rating.score. A single 1–5 integer star rating aligned with
// the EXIF-standard Rating the app already stores per file (see image/metadata.hpp:
// 0–5 stars, 0 = unrated, integer storage). The remote LLM contract requires 1..=5
// (Phase 5f) so a scored image is never confused with an unrated one; the host
// maps a 1..=5 AI rating onto the app's 0–5 Rating field directly. The remote LLM
// is NOT asked for a confidence (Phase 5f rating-contract change), so this DTO
// carries no confidence field — unlike ImageAnalysisUnderstandingResult, which
// still reports the describe-task confidence.
struct ImageAnalysisRatingResult {
  std::string                            request_id;
  bool                                   ok            = false;
  int                                    status        = 0;  // AiResponseStatus
  int                                    error_code    = 0;  // AiErrorCode
  std::string                            error;
  int                                    rating       = 0;   // 1..=5 on success; 0 = unset
  std::string                            rubric_id;
  std::string                            rubric_version;
  std::string                            reasons;
  std::string                            provider;
  std::string                            model_id;
  std::string                            provider_request_id;
  std::string                            prompt_profile_id;
  ImageAnalysisRendition                 rendition;
  ImageAnalysisUsage                     usage;
  uint64_t                               elapsed_ms = 0;
};

// Phase 6c: a model id discovered by live-listing the configured endpoint's
// /models (dry-run discovery). Carries NO capability verdict — listing proves
// only that the endpoint can see the id, not image input or structured-output
// support. The host merges candidates into preset state but keeps them
// unadvertised until a validation smoke pins capability (Phase 6f).
struct AiDiscoveredModel {
  std::string model_id;
  std::string display_name;
  std::string source_provider_id;
};

// Result of ImageAnalysisService::ListModels (Phase 6c). On a non-OK header the
// `models` vector is empty and `error`/`status`/`error_code` carry the failure
// (auth, unsupported provider, network, schema). No annotations are persisted —
// this is a dry run.
struct ImageAnalysisListModelsResult {
  std::string                     request_id;
  bool                            ok         = false;
  int                             status     = 0;  // AiResponseStatus
  int                             error_code = 0;  // AiErrorCode
  std::string                     error;
  std::string                     provider;
  uint64_t                        elapsed_ms = 0;
  std::vector<AiDiscoveredModel>  models;
};

struct AiSidecarRuntimeOptions {
  std::filesystem::path     runtime_binary;
  std::filesystem::path     model_root;
  std::string               host     = "127.0.0.1";
  uint16_t                  port     = 0;
  std::string               model_id = "plhery/mobileclip2-onnx:s2";
  std::string               revision;
  std::string               hf_endpoint        = "https://hf-mirror.com";
  std::string               device             = "auto";
  uint32_t                  batch_cap          = 512;
  uint32_t                  batch_wait_ms      = 25;
  uint32_t                  max_message_bytes  = 16 * 1024 * 1024;
  bool                      allow_download     = false;
  bool                      require_model_info = true;
  std::chrono::milliseconds startup_timeout{5000};
  std::chrono::milliseconds health_poll_interval{50};
  std::chrono::milliseconds graceful_stop_timeout{1000};
  std::chrono::milliseconds kill_timeout{1000};
  std::vector<std::string>  extra_arguments;
};

class IAiSidecarRuntimeClient {
 public:
  virtual ~IAiSidecarRuntimeClient()                                                     = default;

  virtual auto Ping(const std::string& endpoint, std::chrono::milliseconds timeout,
                    std::string* error) -> bool                                          = 0;
  virtual auto GetModelInfo(const std::string& endpoint, std::chrono::milliseconds timeout,
                            AiSidecarRuntimeModelInfo* info, std::string* error) -> bool = 0;
  virtual auto GetRuntimeStatus(const std::string& endpoint, std::chrono::milliseconds timeout,
                                AiSidecarRuntimeRemoteStatus* status, std::string* error)
      -> bool = 0;
  // Queries the sidecar's `AiRuntimeService::ListCapabilities` (proto/ai_runtime.proto). Fills
  // `capabilities` with the host-side DTOs. task_id/credential_ref/timeout_ms are documented as
  // ignored by the server for this RPC (Phase 0 §3.1) but the client fills a header for uniform
  // request correlation.
  virtual auto ListCapabilities(const std::string& endpoint, std::chrono::milliseconds timeout,
                                std::vector<AiSidecarCapability>* capabilities, std::string* error)
      -> bool = 0;
  // Registers a long-lived provider secret with the sidecar's in-memory credential vault
  // (proto/ai_runtime.proto RegisterCredential). The secret travels only over the gRPC
  // loopback channel to the Rust vault; it is never written into AiSidecarRuntimeOptions,
  // process launch args, or logs. On success `*handle` receives the opaque credential
  // handle to place in AiRequestHeader.credential_ref for later task calls. `ttl_ms` of 0
  // asks the server to apply its default lifetime.
  virtual auto RegisterCredential(const std::string& endpoint, std::chrono::milliseconds timeout,
                                  const std::string& provider_id, const std::string& secret,
                                  int64_t ttl_ms, std::string* handle, std::string* error)
      -> bool = 0;
  // Phase 6c: revoke a previously-registered credential handle so it can no
  // longer be resolved by task RPCs. Idempotent. `*revoked` is true only when a
  // live handle was revoked. Used on provider logout, settings deletion, sidecar
  // stop, and project close.
  virtual auto RevokeCredential(const std::string& endpoint, std::chrono::milliseconds timeout,
                                const std::string& handle, bool* revoked, std::string* error)
      -> bool = 0;
  // Cancels an in-flight sidecar task by request_id. `*cancelled` is true only if a task
  // with that request_id was registered and signalled.
  virtual auto CancelTask(const std::string& endpoint, std::chrono::milliseconds timeout,
                          const std::string& request_id, bool* cancelled, std::string* error)
      -> bool = 0;
  virtual auto ListModelProfiles(const std::string& endpoint, const std::string& model_root,
                                 std::chrono::milliseconds timeout, std::string* error)
      -> std::vector<SemanticModelProfileInfo> = 0;
  virtual auto ListInstalledModels(const std::string& endpoint, const std::string& model_root,
                                   std::chrono::milliseconds timeout, std::string* error)
      -> std::vector<SemanticModelProfileInfo> = 0;
  virtual auto ValidateModel(const std::string& endpoint, const std::string& profile_id,
                             const std::string& model_root, std::chrono::milliseconds timeout)
      -> SemanticModelManagerResult = 0;
  virtual auto DeleteModel(const std::string& endpoint, const std::string& profile_id,
                           const std::string& model_root, std::chrono::milliseconds timeout)
      -> SemanticModelManagerResult = 0;
  virtual auto EmbedText(const std::string& endpoint, const std::string& request_id,
                         const std::string& text, std::chrono::milliseconds timeout)
      -> SemanticEmbeddingResult = 0;
  virtual auto EmbedTextBatch(const std::string&                               endpoint,
                              const std::vector<SemanticTextEmbeddingRequest>& requests,
                              std::chrono::milliseconds                        timeout)
      -> std::vector<SemanticEmbeddingResult>;
  virtual auto EmbedImage(const std::string& endpoint, const std::string& request_id,
                          const std::vector<uint8_t>& rgba8_image, const std::string& format_hint,
                          std::chrono::milliseconds timeout) -> SemanticEmbeddingResult = 0;
  virtual auto EmbedImageBatch(const std::string&                         endpoint,
                               std::vector<SemanticImageEmbeddingRequest> requests,
                               std::chrono::milliseconds                  timeout)
      -> std::vector<SemanticEmbeddingResult> = 0;
  // v2 embedding RPCs (Phase 4): carry the shared AiRequestHeader/AiResponseHeader inline for
  // uniform request correlation, timeout, and (future) cancellation. The service wrappers try
  // v2 first and fall back to v1 when the sidecar reports v2 unavailable (*v2_available=false on
  // the default impl, or grpc::UNIMPLEMENTED from a pre-Phase-4 server). v1 RPCs stay frozen.
  virtual auto EmbedTextV2(const std::string& endpoint, const std::string& request_id,
                            const std::string& text, std::chrono::milliseconds timeout,
                            bool* v2_available) -> SemanticEmbeddingResult;
  virtual auto EmbedImageV2(const std::string& endpoint, const std::string& request_id,
                            const std::vector<uint8_t>& rgba8_image,
                            const std::string& format_hint, std::chrono::milliseconds timeout,
                            bool* v2_available) -> SemanticEmbeddingResult;
  virtual auto EmbedTextBatchV2(const std::string&                               endpoint,
                                 const std::vector<SemanticTextEmbeddingRequest>& requests,
                                 std::chrono::milliseconds                        timeout,
                                 bool* v2_available) -> std::vector<SemanticEmbeddingResult>;
  virtual auto EmbedImageBatchV2(const std::string&                                endpoint,
                                 const std::vector<SemanticImageEmbeddingRequest>& requests,
                                 std::chrono::milliseconds                        timeout,
                                 bool* v2_available) -> std::vector<SemanticEmbeddingResult>;
  // Phase 5d image-analysis RPCs (proto/image_analysis.proto ImageAnalysisService).
  // Typed task RPCs with inline AiRequestHeader/AiResponseHeader — no v1/v2 split
  // (image analysis is new; a sidecar that predates 5d returns grpc::UNIMPLEMENTED,
  // which the client maps to a failed result). `credential_ref` is the opaque vault
  // handle, never key material. image_bytes is the encoded rendition (JPEG/PNG).
  virtual auto DescribeImage(const std::string&          endpoint,
                             const ImageAnalysisRequest& request, std::chrono::milliseconds timeout)
      -> ImageAnalysisUnderstandingResult = 0;
  virtual auto ScoreImage(const std::string&          endpoint,
                          const ImageAnalysisRequest& request, std::chrono::milliseconds timeout)
      -> ImageAnalysisRatingResult = 0;
  // Phase 6c: dry-run model discovery against the configured endpoint's /models
  // (proto/image_analysis.proto ImageAnalysisService::ListModels). `provider_id`
  // selects the configured endpoint ("" = sidecar default); `credential_ref` is
  // the opaque vault handle, never key material. Returns unverified candidate
  // DTOs; no annotations are persisted.
  virtual auto ListModels(const std::string& endpoint, const std::string& provider_id,
                          const std::string& credential_ref, std::chrono::milliseconds timeout)
      -> ImageAnalysisListModelsResult = 0;
};

class GrpcAiSidecarRuntimeClient final : public IAiSidecarRuntimeClient {
 public:
  auto Ping(const std::string& endpoint, std::chrono::milliseconds timeout, std::string* error)
      -> bool override;
  auto GetModelInfo(const std::string& endpoint, std::chrono::milliseconds timeout,
                    AiSidecarRuntimeModelInfo* info, std::string* error) -> bool override;
  auto GetRuntimeStatus(const std::string& endpoint, std::chrono::milliseconds timeout,
                        AiSidecarRuntimeRemoteStatus* status, std::string* error) -> bool override;
  auto ListCapabilities(const std::string& endpoint, std::chrono::milliseconds timeout,
                        std::vector<AiSidecarCapability>* capabilities, std::string* error)
      -> bool override;
  auto RegisterCredential(const std::string& endpoint, std::chrono::milliseconds timeout,
                          const std::string& provider_id, const std::string& secret, int64_t ttl_ms,
                          std::string* handle, std::string* error) -> bool override;
  auto RevokeCredential(const std::string& endpoint, std::chrono::milliseconds timeout,
                        const std::string& handle, bool* revoked, std::string* error)
      -> bool override;
  auto CancelTask(const std::string& endpoint, std::chrono::milliseconds timeout,
                  const std::string& request_id, bool* cancelled, std::string* error)
      -> bool override;
  auto ListModelProfiles(const std::string& endpoint, const std::string& model_root,
                         std::chrono::milliseconds timeout, std::string* error)
      -> std::vector<SemanticModelProfileInfo> override;
  auto ListInstalledModels(const std::string& endpoint, const std::string& model_root,
                           std::chrono::milliseconds timeout, std::string* error)
      -> std::vector<SemanticModelProfileInfo> override;
  auto ValidateModel(const std::string& endpoint, const std::string& profile_id,
                     const std::string& model_root, std::chrono::milliseconds timeout)
      -> SemanticModelManagerResult override;
  auto DeleteModel(const std::string& endpoint, const std::string& profile_id,
                   const std::string& model_root, std::chrono::milliseconds timeout)
      -> SemanticModelManagerResult override;
  auto EmbedText(const std::string& endpoint, const std::string& request_id,
                 const std::string& text, std::chrono::milliseconds timeout)
      -> SemanticEmbeddingResult override;
  auto EmbedTextBatch(const std::string&                               endpoint,
                      const std::vector<SemanticTextEmbeddingRequest>& requests,
                      std::chrono::milliseconds                        timeout)
      -> std::vector<SemanticEmbeddingResult> override;
  auto EmbedImage(const std::string& endpoint, const std::string& request_id,
                  const std::vector<uint8_t>& rgba8_image, const std::string& format_hint,
                  std::chrono::milliseconds timeout) -> SemanticEmbeddingResult override;
  auto EmbedImageBatch(const std::string&                         endpoint,
                       std::vector<SemanticImageEmbeddingRequest> requests,
                       std::chrono::milliseconds                  timeout)
      -> std::vector<SemanticEmbeddingResult> override;
  auto EmbedTextV2(const std::string& endpoint, const std::string& request_id,
                   const std::string& text, std::chrono::milliseconds timeout, bool* v2_available)
      -> SemanticEmbeddingResult override;
  auto EmbedImageV2(const std::string& endpoint, const std::string& request_id,
                    const std::vector<uint8_t>& rgba8_image, const std::string& format_hint,
                    std::chrono::milliseconds timeout, bool* v2_available)
      -> SemanticEmbeddingResult override;
  auto EmbedTextBatchV2(const std::string&                               endpoint,
                        const std::vector<SemanticTextEmbeddingRequest>& requests,
                        std::chrono::milliseconds                        timeout, bool* v2_available)
      -> std::vector<SemanticEmbeddingResult> override;
  auto EmbedImageBatchV2(const std::string&                                endpoint,
                         const std::vector<SemanticImageEmbeddingRequest>& requests,
                         std::chrono::milliseconds                        timeout, bool* v2_available)
      -> std::vector<SemanticEmbeddingResult> override;
  auto DescribeImage(const std::string& endpoint, const ImageAnalysisRequest& request,
                     std::chrono::milliseconds timeout) -> ImageAnalysisUnderstandingResult override;
  auto ScoreImage(const std::string& endpoint, const ImageAnalysisRequest& request,
                  std::chrono::milliseconds timeout) -> ImageAnalysisRatingResult override;
  auto ListModels(const std::string& endpoint, const std::string& provider_id,
                  const std::string& credential_ref, std::chrono::milliseconds timeout)
      -> ImageAnalysisListModelsResult override;
};

class AiSidecarRuntimeService final : public QObject {
  Q_OBJECT
  Q_PROPERTY(QString state READ StateName NOTIFY statusChanged)
  Q_PROPERTY(QString issue READ IssueName NOTIFY statusChanged)
  Q_PROPERTY(QString statusMessage READ StatusMessage NOTIFY statusChanged)
  Q_PROPERTY(QString endpoint READ EndpointQString NOTIFY statusChanged)

 public:
  explicit AiSidecarRuntimeService(std::shared_ptr<IAiSidecarRuntimeClient> client =
                                       std::make_shared<GrpcAiSidecarRuntimeClient>(),
                                   QObject* parent = nullptr);
  ~AiSidecarRuntimeService() override;

  auto StartAndWait(const AiSidecarRuntimeOptions& options) -> bool;
  void Stop();
  void StopForProjectClose();

  auto Status() -> AiSidecarRuntimeStatusSnapshot;
  auto Options() const -> const AiSidecarRuntimeOptions& { return options_; }
  auto IsRunning() -> bool;
  auto Endpoint() const -> std::string { return endpoint_; }

  auto ListCapabilities(std::chrono::milliseconds timeout, std::string* error)
      -> std::vector<AiSidecarCapability>;
  // Host-side convenience wrappers around the client RPCs of the same name. The secret
  // passed to RegisterCredential is forwarded only to the sidecar vault; it never enters
  // BuildArguments or any logged surface.
  auto RegisterCredential(const std::string& provider_id, const std::string& secret, int64_t ttl_ms,
                          std::chrono::milliseconds timeout, std::string* handle,
                          std::string* error) -> bool;
  // Phase 6c: revoke a credential handle (idempotent). `*revoked` is true only
  // when a live handle was revoked. Used on logout / settings deletion / stop /
  // project close.
  auto RevokeCredential(const std::string& handle, std::chrono::milliseconds timeout,
                        bool* revoked, std::string* error) -> bool;
  auto CancelTask(const std::string& request_id, std::chrono::milliseconds timeout, bool* cancelled,
                  std::string* error) -> bool;
  auto ListModelProfiles(const std::string& model_root, std::chrono::milliseconds timeout,
                         std::string* error) -> std::vector<SemanticModelProfileInfo>;
  auto ListInstalledModels(const std::string& model_root, std::chrono::milliseconds timeout,
                           std::string* error) -> std::vector<SemanticModelProfileInfo>;
  auto ValidateModel(const std::string& profile_id, const std::string& model_root,
                     std::chrono::milliseconds timeout) -> SemanticModelManagerResult;
  auto DeleteModel(const std::string& profile_id, const std::string& model_root,
                   std::chrono::milliseconds timeout) -> SemanticModelManagerResult;

  auto EmbedText(const std::string& request_id, const std::string& text,
                 std::chrono::milliseconds timeout) -> SemanticEmbeddingResult;
  auto EmbedTextBatch(const std::vector<SemanticTextEmbeddingRequest>& requests,
                      std::chrono::milliseconds timeout) -> std::vector<SemanticEmbeddingResult>;
  auto EmbedImage(const std::string& request_id, const std::vector<uint8_t>& rgba8_image,
                  const std::string& format_hint, std::chrono::milliseconds timeout)
      -> SemanticEmbeddingResult;
  auto EmbedImageBatch(std::vector<SemanticImageEmbeddingRequest> requests,
                       std::chrono::milliseconds timeout) -> std::vector<SemanticEmbeddingResult>;

  // Phase 5d image-analysis host wrappers. The host fills `request` (including the
  // opaque `credential_ref` from a prior RegisterCredential call); the wrapper adds
  // the ready-guard and delegates to the client. No v1/v2 split. The sidecar returns
  // results only — the host owns all persistence (5e).
  auto DescribeImage(const ImageAnalysisRequest& request, std::chrono::milliseconds timeout)
      -> ImageAnalysisUnderstandingResult;
  auto ScoreImage(const ImageAnalysisRequest& request, std::chrono::milliseconds timeout)
      -> ImageAnalysisRatingResult;
  // Phase 6c: dry-run model discovery (validate-connection flow). Ready-guarded
  // delegate to the client; `credential_ref` is the opaque vault handle.
  auto ListModels(const std::string& provider_id, const std::string& credential_ref,
                  std::chrono::milliseconds timeout) -> ImageAnalysisListModelsResult;

  auto StateName() const -> QString;
  auto IssueName() const -> QString;
  auto StatusMessage() const -> QString { return QString::fromStdString(status_.message); }
  auto EndpointQString() const -> QString { return QString::fromStdString(endpoint_); }

 signals:
  void statusChanged();

 private:
  void SetStatus(AiSidecarRuntimeState state, AiSidecarRuntimeIssue issue, std::string message);
  void AppendStdout(const QByteArray& bytes);
  void AppendStderr(const QByteArray& bytes);
  void RefreshProcessExit();
  auto BuildArguments() const -> QStringList;
  auto ChoosePort() const -> uint16_t;
  auto WaitForReadiness() -> bool;
  void AttachChildTreeCleanup();
  void ReleaseChildTreeCleanup();

  std::shared_ptr<IAiSidecarRuntimeClient> client_;
  QProcess                                 process_;
  AiSidecarRuntimeOptions                  options_;
  AiSidecarRuntimeStatusSnapshot           status_;
  std::string                              endpoint_;
#ifdef _WIN32
  void* job_object_ = nullptr;
#endif
};

auto ToString(AiSidecarRuntimeState state) -> const char*;
auto ToString(AiSidecarRuntimeIssue issue) -> const char*;

}  // namespace alcedo
