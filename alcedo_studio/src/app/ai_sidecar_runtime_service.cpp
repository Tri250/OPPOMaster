//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/ai_sidecar_runtime_service.hpp"

#include <QCoreApplication>
#include <QDir>
#include <QHostAddress>
#include <QMetaObject>
#include <QStandardPaths>
#include <QStringList>
#include <QTcpServer>
#include <QThread>
#include <chrono>
#include <sstream>
#include <thread>
#include <utility>
#include <vector>

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

auto FsPathFromQString(const QString& value) -> std::filesystem::path {
#ifdef _WIN32
  return std::filesystem::path(value.toStdWString());
#else
  return std::filesystem::path(value.toStdString());
#endif
}

auto DefaultProviderConfigDir() -> std::filesystem::path {
  QString root = QStandardPaths::writableLocation(QStandardPaths::AppConfigLocation);
  if (root.isEmpty()) {
    root = QDir::homePath() + QStringLiteral("/.alcedo_studio");
  }
  return FsPathFromQString(root) / "ai_provider_configs";
}

auto DefaultRuntimeBinary() -> std::filesystem::path;
auto DefaultRuntimeModelRoot() -> std::filesystem::path;

auto NormalizeRuntimeOptions(AiSidecarRuntimeOptions options) -> AiSidecarRuntimeOptions {
  if (options.runtime_binary.empty()) {
    options.runtime_binary = DefaultRuntimeBinary();
  }
  if (options.model_root.empty()) {
    options.model_root = DefaultRuntimeModelRoot();
  }
  if (options.provider_config_dir.empty()) {
    options.provider_config_dir = DefaultProviderConfigDir();
  }
  return options;
}

auto RuntimeOptionsCompatible(const AiSidecarRuntimeOptions& running,
                              const AiSidecarRuntimeOptions& requested) -> bool {
  const bool requested_default_port = requested.port == 0;
  return running.runtime_binary == requested.runtime_binary &&
         running.model_id == requested.model_id &&
         running.revision == requested.revision && running.model_root == requested.model_root &&
         running.provider_config_dir == requested.provider_config_dir &&
         running.host == requested.host &&
         (requested_default_port || running.port == requested.port) &&
         running.hf_endpoint == requested.hf_endpoint && running.device == requested.device &&
         running.batch_cap == requested.batch_cap &&
         running.batch_wait_ms == requested.batch_wait_ms &&
         running.max_message_bytes == requested.max_message_bytes &&
         running.allow_download == requested.allow_download &&
         running.extra_arguments == requested.extra_arguments;
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

  const auto append_ancestor_runtime_binaries = [](const std::filesystem::path& start,
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

AiSidecarRuntimeService::AiSidecarRuntimeService(AiSidecarClientFactory client_factory,
                                                 QObject* parent)
    : QObject(parent), client_factory_(std::move(client_factory)) {
  if (!client_factory_) {
    client_factory_ = [](const std::string& endpoint) {
      return sidecar_client::MakeGrpcClient(endpoint);
    };
  }

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
    const auto requested_options = NormalizeRuntimeOptions(options);
    if (RuntimeOptionsCompatible(options_, requested_options)) {
      if (requested_options.require_model_info && !status_.model_info.has_value()) {
        options_.require_model_info = true;
        options_.startup_timeout = requested_options.startup_timeout;
        options_.health_poll_interval = requested_options.health_poll_interval;
        return WaitForReadiness();
      }
      return true;
    }
    Stop();
  }

  options_ = NormalizeRuntimeOptions(options);
  if (options_.port == 0) {
    options_.port = ChoosePort();
  }
  endpoint_ = BuildEndpoint(options_.host, options_.port);
  client_.reset();
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

  client_ = client_factory_ ? client_factory_(endpoint_) : nullptr;
  if (!client_) {
    if (process_.state() != QProcess::NotRunning) {
      process_.terminate();
      if (!process_.waitForFinished(static_cast<int>(options_.graceful_stop_timeout.count()))) {
        process_.kill();
        process_.waitForFinished(static_cast<int>(options_.kill_timeout.count()));
      }
    }
    ReleaseChildTreeCleanup();
    status_.process_id = 0;
    SetStatus(AiSidecarRuntimeState::kFailed, AiSidecarRuntimeIssue::kClientUnavailable,
              "AI sidecar client session is not available");
    return false;
  }

  return WaitForReadiness();
}

void AiSidecarRuntimeService::Stop() {
  if (QThread::currentThread() != thread()) {
    QMetaObject::invokeMethod(this, [this]() { Stop(); }, Qt::BlockingQueuedConnection);
    return;
  }

  if (!IsRunning()) {
    client_.reset();
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
  client_.reset();
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
    if (client_->runtime().GetRuntimeStatus(std::chrono::milliseconds(250), &remote, &error)) {
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
  client_.reset();
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
  if (!options_.provider_config_dir.empty()) {
    args << "--provider-config-dir"
         << QString::fromStdString(options_.provider_config_dir.string());
  }
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
    if (client_ && client_->runtime().Ping(options_.health_poll_interval, &last_error)) {
      AiSidecarRuntimeModelInfo info;
      std::string               info_error;
      if (client_->semantic().GetModelInfo(std::chrono::milliseconds(500), &info, &info_error)) {
        status_.model_info = info;
        SetStatus(AiSidecarRuntimeState::kReady, AiSidecarRuntimeIssue::kNone,
                  "Semantic runtime is ready");
        return true;
      }
      if (!options_.require_model_info) {
        SetStatus(AiSidecarRuntimeState::kReady, AiSidecarRuntimeIssue::kNone,
                  "Semantic model manager is ready");
        return true;
      }
      if (!info_error.empty()) {
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
  client_.reset();
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
