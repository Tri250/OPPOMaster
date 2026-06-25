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
