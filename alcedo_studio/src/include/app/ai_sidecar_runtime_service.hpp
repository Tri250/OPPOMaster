//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QObject>
#include <QProcess>
#include <QString>
#include <atomic>
#include <chrono>
#include <cstdint>
#include <filesystem>
#include <functional>
#include <memory>
#include <optional>
#include <string>
#include <vector>

#include "sidecar_client/client.hpp"
#include "sidecar_client/dto/runtime.hpp"

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

struct AiSidecarRuntimeOptions {
  std::filesystem::path     runtime_binary;
  std::filesystem::path     model_root;
  std::filesystem::path     provider_config_dir;
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

using AiSidecarClientFactory =
    std::function<std::shared_ptr<sidecar_client::Client>(const std::string& endpoint)>;

class AiSidecarRuntimeService final : public QObject {
  Q_OBJECT
  Q_PROPERTY(QString state READ StateName NOTIFY statusChanged)
  Q_PROPERTY(QString issue READ IssueName NOTIFY statusChanged)
  Q_PROPERTY(QString statusMessage READ StatusMessage NOTIFY statusChanged)
  Q_PROPERTY(QString endpoint READ EndpointQString NOTIFY statusChanged)

 public:
  explicit AiSidecarRuntimeService(AiSidecarClientFactory client_factory = {},
                                   QObject*               parent         = nullptr);
  ~AiSidecarRuntimeService() override;

  auto               StartAndWait(const AiSidecarRuntimeOptions& options) -> bool;
  // Starts the sidecar on the service's own (UI) thread but pumps the Qt event
  // loop (excluding socket notifiers) between readiness polls so the UI stays
  // responsive during a cold boot. MUST be called on the service's thread — the
  // nested event processing is only valid there. Checkpoints
  // RequestCancelStart() each poll and aborts the boot (terminating the
  // process, returning to kStopped) if cancellation is requested. Emits
  // statusChanged() for each state transition, exactly like StartAndWait.
  auto               StartAndWaitInteractive(const AiSidecarRuntimeOptions& options) -> bool;
  // Requests that an in-progress StartAndWaitInteractive abort at the next poll
  // checkpoint. Safe to call from any thread; a no-op when no interactive boot
  // is running.
  void               RequestCancelStart();
  // True while an interactive boot is in progress on the service's thread.
  bool               IsStartingInteractive() const { return interactive_starting_.load(); }
  [[nodiscard]] auto AcquireLease() -> std::shared_ptr<void>;
  void               Stop();
  void               StopForProjectClose();

  auto               Status() -> AiSidecarRuntimeStatusSnapshot;
  auto               Options() const -> const AiSidecarRuntimeOptions& { return options_; }
  auto               IsRunning() -> bool;
  auto               Endpoint() const -> std::string { return endpoint_; }
  auto ClientSession() const -> std::shared_ptr<sidecar_client::Client> { return client_; }

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
  // Shared boot body for StartAndWait (non-interactive) and
  // StartAndWaitInteractive (interactive). When `interactive`, the readiness
  // poll pumps the Qt event loop between iterations and checkpoints
  // cancel_start_requested_ so the UI stays responsive and the boot is
  // cancellable. Must run on the service's own thread.
  auto StartAndWaitBody(const AiSidecarRuntimeOptions& options, bool interactive) -> bool;
  auto WaitForReadiness(bool terminate_on_timeout, bool interactive) -> bool;
  void AttachChildTreeCleanup();
  void ReleaseChildTreeCleanup();
  void ReleaseLease();

  AiSidecarClientFactory                  client_factory_;
  std::shared_ptr<sidecar_client::Client> client_;
  QProcess                                process_;
  AiSidecarRuntimeOptions                 options_;
  AiSidecarRuntimeStatusSnapshot          status_;
  std::string                             endpoint_;
  int                                     active_leases_ = 0;
  // Interactive-boot state. `interactive_starting_` is set for the duration of
  // a StartAndWaitInteractive boot so IsStartingInteractive() can report it;
  // `cancel_start_requested_` is the cancel flag checkpointed each poll.
  std::atomic<bool>                       interactive_starting_{false};
  std::atomic<bool>                       cancel_start_requested_{false};
#ifdef _WIN32
  void* job_object_ = nullptr;
#endif
};

auto ToString(AiSidecarRuntimeState state) -> const char*;
auto ToString(AiSidecarRuntimeIssue issue) -> const char*;

}  // namespace alcedo
