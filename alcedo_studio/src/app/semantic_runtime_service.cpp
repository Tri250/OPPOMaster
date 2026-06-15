//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/semantic_runtime_service.hpp"

#include <algorithm>
#include <array>
#include <chrono>
#include <sstream>
#include <thread>
#include <vector>

#include <grpcpp/create_channel.h>
#include <grpcpp/security/credentials.h>

#include <QCoreApplication>
#include <QHostAddress>
#include <QStandardPaths>
#include <QTcpServer>

#include "semantic.grpc.pb.h"

#ifdef _WIN32
#include <windows.h>
#endif

namespace alcedo {
namespace {

constexpr size_t kLogTailBytes = 16 * 1024;
constexpr auto   kSemanticRuntimeBinaryEnv = "ALCEDO_MIND_BINARY";
constexpr auto   kSemanticModelRootEnv = "ALCEDO_MIND_MODEL_ROOT";
constexpr auto   kMobileClipModelRoot  = "models/mobileclip2-s2-openclip";
#ifdef _WIN32
constexpr auto   kSemanticRuntimeBinaryName = "alcedo_mind.exe";
#else
constexpr auto   kSemanticRuntimeBinaryName = "alcedo_mind";
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
  const QByteArray env_binary = qgetenv(kSemanticRuntimeBinaryEnv);
  if (!env_binary.isEmpty()) {
    return std::filesystem::path(env_binary.constData());
  }

  const auto app_dir = QCoreApplication::applicationDirPath();
#ifdef _WIN32
  const auto app_path = std::filesystem::path(app_dir.toStdWString());
#else
  const auto app_path = std::filesystem::path(app_dir.toStdString());
#endif

  const auto append_ancestor_runtime_binaries =
      [](const std::filesystem::path& start, std::vector<std::filesystem::path>* candidates) {
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
                                kSemanticRuntimeBinaryName);
          candidates->push_back(current / "rust" / "puerh_mind" / "target" / "debug" /
                                kSemanticRuntimeBinaryName);
          const auto parent = current.parent_path();
          if (parent == current) {
            break;
          }
          current = parent;
        }
      };

  std::vector<std::filesystem::path> candidates;
  candidates.push_back(app_path / kSemanticRuntimeBinaryName);
  append_ancestor_runtime_binaries(app_path, &candidates);
  std::error_code ec;
  append_ancestor_runtime_binaries(std::filesystem::current_path(ec), &candidates);

  for (const auto& candidate : candidates) {
    if (std::filesystem::exists(candidate, ec) && !ec &&
        std::filesystem::is_regular_file(candidate, ec) && !ec) {
      return candidate;
    }
  }
  return app_path / kSemanticRuntimeBinaryName;
}

auto ExistingDirectory(const std::filesystem::path& path) -> bool {
  std::error_code ec;
  return std::filesystem::exists(path, ec) && !ec && std::filesystem::is_directory(path, ec) &&
         !ec;
}

void AppendAncestorModelRoots(const std::filesystem::path& start,
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
    candidates->push_back(current / "rust" / "puerh_mind" / kMobileClipModelRoot);
    const auto parent = current.parent_path();
    if (parent == current) {
      break;
    }
    current = parent;
  }
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

  std::vector<std::filesystem::path> candidates;
  const auto app_data_dir = QStandardPaths::writableLocation(QStandardPaths::AppDataLocation);
  if (!app_data_dir.isEmpty()) {
#ifdef _WIN32
    const auto app_data_path = std::filesystem::path(app_data_dir.toStdWString());
#else
    const auto app_data_path = std::filesystem::path(app_data_dir.toStdString());
#endif
    candidates.push_back(app_data_path / kMobileClipModelRoot);
  }

  candidates.push_back(app_path / kMobileClipModelRoot);

  AppendAncestorModelRoots(app_path, &candidates);
  std::error_code ec;
  AppendAncestorModelRoots(std::filesystem::current_path(ec), &candidates);

  for (const auto& candidate : candidates) {
    if (ExistingDirectory(candidate)) {
      return candidate;
    }
  }
  return {};
}

auto DeadlineFromNow(std::chrono::milliseconds timeout) -> std::chrono::system_clock::time_point {
  return std::chrono::system_clock::now() + timeout;
}

auto GrpcErrorMessage(const grpc::Status& status) -> std::string {
  if (!status.error_message().empty()) {
    return status.error_message();
  }
  return "gRPC call failed with code " + std::to_string(static_cast<int>(status.error_code()));
}

auto ToRuntimeModelInfo(const semantic::GetModelInfoResponse& response)
    -> SemanticRuntimeModelInfo {
  SemanticRuntimeModelInfo info;
  info.model_id              = response.model_id();
  info.revision              = response.revision();
  info.embedding_dimension   = response.embedding_dimension();
  info.image_size            = response.image_size();
  info.provider              = response.provider();
  info.model_root            = response.model_root();
  info.prototype_config_hash = response.prototype_config_hash();
  return info;
}

auto ToRuntimeStatus(const semantic::GetRuntimeStatusResponse& response)
    -> SemanticRuntimeRemoteStatus {
  SemanticRuntimeRemoteStatus status;
  status.state               = response.state();
  status.provider            = response.provider();
  status.image_batch_cap     = response.image_batch_cap();
  status.image_batch_wait_ms = response.image_batch_wait_ms();
  status.uptime_ms           = response.uptime_ms();
  return status;
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

}  // namespace

auto ToString(SemanticRuntimeState state) -> const char* {
  switch (state) {
    case SemanticRuntimeState::kStopped:
      return "stopped";
    case SemanticRuntimeState::kStarting:
      return "starting";
    case SemanticRuntimeState::kReady:
      return "ready";
    case SemanticRuntimeState::kStopping:
      return "stopping";
    case SemanticRuntimeState::kFailed:
      return "failed";
  }
  return "unknown";
}

auto ToString(SemanticRuntimeIssue issue) -> const char* {
  switch (issue) {
    case SemanticRuntimeIssue::kNone:
      return "none";
    case SemanticRuntimeIssue::kBinaryMissing:
      return "binary_missing";
    case SemanticRuntimeIssue::kStartFailed:
      return "start_failed";
    case SemanticRuntimeIssue::kReadinessTimeout:
      return "readiness_timeout";
    case SemanticRuntimeIssue::kRuntimeExited:
      return "runtime_exited";
    case SemanticRuntimeIssue::kRuntimeCrashed:
      return "runtime_crashed";
    case SemanticRuntimeIssue::kStopTimedOut:
      return "stop_timed_out";
    case SemanticRuntimeIssue::kClientUnavailable:
      return "client_unavailable";
    case SemanticRuntimeIssue::kClientError:
      return "client_error";
  }
  return "unknown";
}

auto GrpcSemanticRuntimeClient::Ping(const std::string& endpoint,
                                     std::chrono::milliseconds timeout,
                                     std::string* error) -> bool {
  auto channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto stub    = semantic::SemanticService::NewStub(channel);

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

auto GrpcSemanticRuntimeClient::GetModelInfo(const std::string& endpoint,
                                             std::chrono::milliseconds timeout,
                                             SemanticRuntimeModelInfo* info,
                                             std::string* error) -> bool {
  auto channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto stub    = semantic::SemanticService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  semantic::GetModelInfoRequest  request;
  semantic::GetModelInfoResponse response;
  const auto status = stub->GetModelInfo(&context, request, &response);
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

auto GrpcSemanticRuntimeClient::GetRuntimeStatus(const std::string& endpoint,
                                                 std::chrono::milliseconds timeout,
                                                 SemanticRuntimeRemoteStatus* status,
                                                 std::string* error) -> bool {
  auto channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto stub    = semantic::SemanticService::NewStub(channel);

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

auto GrpcSemanticRuntimeClient::EmbedText(const std::string& endpoint,
                                          const std::string& request_id,
                                          const std::string& text,
                                          std::chrono::milliseconds timeout)
    -> SemanticEmbeddingResult {
  auto channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto stub    = semantic::SemanticService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  semantic::EmbedTextRequest request;
  request.set_request_id(request_id);
  request.set_text(text);
  semantic::EmbeddingResponse response;
  const auto status = stub->EmbedText(&context, request, &response);
  if (status.ok()) {
    return ToEmbeddingResult(response);
  }
  SemanticEmbeddingResult result;
  result.request_id = request_id;
  result.ok         = false;
  result.error      = GrpcErrorMessage(status);
  return result;
}

auto GrpcSemanticRuntimeClient::EmbedImage(const std::string& endpoint,
                                           const std::string& request_id,
                                           const std::vector<uint8_t>& rgba8_image,
                                           const std::string& format_hint,
                                           std::chrono::milliseconds timeout)
    -> SemanticEmbeddingResult {
  auto channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto stub    = semantic::SemanticService::NewStub(channel);

  grpc::ClientContext context;
  context.set_deadline(DeadlineFromNow(timeout));
  semantic::EmbedImageRequest request;
  request.set_request_id(request_id);
  request.set_image_bytes(reinterpret_cast<const char*>(rgba8_image.data()), rgba8_image.size());
  request.set_image_format_hint(format_hint);
  semantic::EmbeddingResponse response;
  const auto status = stub->EmbedImage(&context, request, &response);
  if (status.ok()) {
    return ToEmbeddingResult(response);
  }
  SemanticEmbeddingResult result;
  result.request_id = request_id;
  result.ok         = false;
  result.error      = GrpcErrorMessage(status);
  return result;
}

auto GrpcSemanticRuntimeClient::EmbedImageBatch(
    const std::string& endpoint, const std::vector<SemanticImageEmbeddingRequest>& requests,
    std::chrono::milliseconds timeout) -> std::vector<SemanticEmbeddingResult> {
  auto channel = grpc::CreateChannel(endpoint, grpc::InsecureChannelCredentials());
  auto stub    = semantic::SemanticService::NewStub(channel);

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

  semantic::EmbeddingBatchResponse response;
  const auto status = stub->EmbedImageBatch(&context, request, &response);
  std::vector<SemanticEmbeddingResult> results;
  if (!status.ok()) {
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
  return results;
}

SemanticRuntimeService::SemanticRuntimeService(std::shared_ptr<ISemanticRuntimeClient> client,
                                               QObject* parent)
    : QObject(parent), client_(std::move(client)) {
  status_.state = SemanticRuntimeState::kStopped;
  status_.issue = SemanticRuntimeIssue::kNone;
  status_.message = "Semantic runtime is stopped";

  connect(&process_, &QProcess::readyReadStandardOutput, this, [this]() {
    AppendStdout(process_.readAllStandardOutput());
  });
  connect(&process_, &QProcess::readyReadStandardError, this, [this]() {
    AppendStderr(process_.readAllStandardError());
  });
  connect(&process_, &QProcess::errorOccurred, this, [this](QProcess::ProcessError error) {
    if (error == QProcess::FailedToStart) {
      SetStatus(SemanticRuntimeState::kFailed, SemanticRuntimeIssue::kStartFailed,
                process_.errorString().toStdString());
    }
  });
}

SemanticRuntimeService::~SemanticRuntimeService() {
  StopForProjectClose();
}

auto SemanticRuntimeService::StartAndWait(const SemanticRuntimeOptions& options) -> bool {
  if (IsRunning()) {
    return true;
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
    SetStatus(SemanticRuntimeState::kFailed, SemanticRuntimeIssue::kBinaryMissing,
              "Semantic runtime binary was not found: " + options_.runtime_binary.string());
    return false;
  }
  if (options_.model_root.empty()) {
    SetStatus(SemanticRuntimeState::kFailed, SemanticRuntimeIssue::kStartFailed,
              "Semantic runtime model root was not configured. Set ALCEDO_MIND_MODEL_ROOT or "
              "install/download the model assets before starting semantic generation.");
    return false;
  }

  SetStatus(SemanticRuntimeState::kStarting, SemanticRuntimeIssue::kNone,
            "Starting semantic runtime");
  process_.setProgram(QString::fromStdString(options_.runtime_binary.string()));
  process_.setArguments(BuildArguments());
  process_.setProcessChannelMode(QProcess::SeparateChannels);
  process_.start();

  if (!process_.waitForStarted(static_cast<int>(options_.startup_timeout.count()))) {
    SetStatus(SemanticRuntimeState::kFailed, SemanticRuntimeIssue::kStartFailed,
              process_.errorString().toStdString());
    return false;
  }
  status_.process_id = static_cast<int64_t>(process_.processId());
  AttachChildTreeCleanup();

  return WaitForReadiness();
}

void SemanticRuntimeService::Stop() {
  if (!IsRunning()) {
    SetStatus(SemanticRuntimeState::kStopped, SemanticRuntimeIssue::kNone,
              "Semantic runtime is stopped");
    return;
  }

  SetStatus(SemanticRuntimeState::kStopping, SemanticRuntimeIssue::kNone,
            "Stopping semantic runtime");
  process_.terminate();
  if (!process_.waitForFinished(static_cast<int>(options_.graceful_stop_timeout.count()))) {
    process_.kill();
    if (!process_.waitForFinished(static_cast<int>(options_.kill_timeout.count()))) {
      SetStatus(SemanticRuntimeState::kFailed, SemanticRuntimeIssue::kStopTimedOut,
                "Semantic runtime did not exit after kill request");
      return;
    }
  }
  ReleaseChildTreeCleanup();
  status_.process_id = 0;
  SetStatus(SemanticRuntimeState::kStopped, SemanticRuntimeIssue::kNone,
            "Semantic runtime is stopped");
}

void SemanticRuntimeService::StopForProjectClose() {
  Stop();
}

auto SemanticRuntimeService::Status() -> SemanticRuntimeStatusSnapshot {
  RefreshProcessExit();
  if (status_.state == SemanticRuntimeState::kReady && client_) {
    SemanticRuntimeRemoteStatus remote;
    std::string                 error;
    if (client_->GetRuntimeStatus(endpoint_, std::chrono::milliseconds(250), &remote, &error)) {
      status_.remote_status = remote;
    }
  }
  return status_;
}

auto SemanticRuntimeService::IsRunning() -> bool {
  RefreshProcessExit();
  return process_.state() != QProcess::NotRunning;
}

auto SemanticRuntimeService::EmbedText(const std::string& request_id, const std::string& text,
                                       std::chrono::milliseconds timeout)
    -> SemanticEmbeddingResult {
  if (status_.state != SemanticRuntimeState::kReady || !client_) {
    SemanticEmbeddingResult result;
    result.request_id = request_id;
    result.ok = false;
    result.error = "semantic runtime is not ready";
    return result;
  }
  return client_->EmbedText(endpoint_, request_id, text, timeout);
}

auto SemanticRuntimeService::EmbedImage(const std::string& request_id,
                                        const std::vector<uint8_t>& rgba8_image,
                                        const std::string& format_hint,
                                        std::chrono::milliseconds timeout)
    -> SemanticEmbeddingResult {
  if (status_.state != SemanticRuntimeState::kReady || !client_) {
    SemanticEmbeddingResult result;
    result.request_id = request_id;
    result.ok = false;
    result.error = "semantic runtime is not ready";
    return result;
  }
  return client_->EmbedImage(endpoint_, request_id, rgba8_image, format_hint, timeout);
}

auto SemanticRuntimeService::EmbedImageBatch(
    const std::vector<SemanticImageEmbeddingRequest>& requests, std::chrono::milliseconds timeout)
    -> std::vector<SemanticEmbeddingResult> {
  if (status_.state != SemanticRuntimeState::kReady || !client_) {
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
  return client_->EmbedImageBatch(endpoint_, requests, timeout);
}

auto SemanticRuntimeService::StateName() const -> QString {
  return QString::fromLatin1(ToString(status_.state));
}

auto SemanticRuntimeService::IssueName() const -> QString {
  return QString::fromLatin1(ToString(status_.issue));
}

void SemanticRuntimeService::SetStatus(SemanticRuntimeState state, SemanticRuntimeIssue issue,
                                       std::string message) {
  status_.state = state;
  status_.issue = issue;
  status_.message = std::move(message);
  status_.endpoint = endpoint_;
  emit statusChanged();
}

void SemanticRuntimeService::AppendStdout(const QByteArray& bytes) {
  TailAppend(&status_.stdout_tail, bytes);
}

void SemanticRuntimeService::AppendStderr(const QByteArray& bytes) {
  TailAppend(&status_.stderr_tail, bytes);
}

void SemanticRuntimeService::RefreshProcessExit() {
  if (process_.state() != QProcess::NotRunning) {
    process_.waitForFinished(0);
  }
  if (process_.state() != QProcess::NotRunning) {
    return;
  }
  if (status_.state != SemanticRuntimeState::kReady &&
      status_.state != SemanticRuntimeState::kStarting &&
      status_.state != SemanticRuntimeState::kStopping) {
    return;
  }

  ReleaseChildTreeCleanup();
  AppendStdout(process_.readAllStandardOutput());
  AppendStderr(process_.readAllStandardError());
  status_.process_id = 0;
  const bool crashed = process_.exitStatus() == QProcess::CrashExit || process_.exitCode() != 0;
  std::ostringstream message;
  message << "Semantic runtime exited with code " << process_.exitCode();
  SetStatus(SemanticRuntimeState::kFailed,
            crashed ? SemanticRuntimeIssue::kRuntimeCrashed
                    : SemanticRuntimeIssue::kRuntimeExited,
            message.str());
}

auto SemanticRuntimeService::BuildArguments() const -> QStringList {
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

auto SemanticRuntimeService::ChoosePort() const -> uint16_t {
  QTcpServer server;
  if (server.listen(QHostAddress::LocalHost, 0)) {
    const auto port = static_cast<uint16_t>(server.serverPort());
    server.close();
    return port;
  }
  return 50051;
}

auto SemanticRuntimeService::WaitForReadiness() -> bool {
  const auto deadline = std::chrono::steady_clock::now() + options_.startup_timeout;
  std::string last_error;
  while (std::chrono::steady_clock::now() < deadline) {
    process_.waitForReadyRead(static_cast<int>(options_.health_poll_interval.count()));
    RefreshProcessExit();
    if (status_.state == SemanticRuntimeState::kFailed) {
      return false;
    }
    if (client_ && client_->Ping(endpoint_, options_.health_poll_interval, &last_error)) {
      SemanticRuntimeModelInfo info;
      std::string              info_error;
      if (client_->GetModelInfo(endpoint_, std::chrono::milliseconds(500), &info, &info_error)) {
        status_.model_info = info;
      } else if (!info_error.empty()) {
        status_.message = info_error;
      }
      SetStatus(SemanticRuntimeState::kReady, SemanticRuntimeIssue::kNone,
                "Semantic runtime is ready");
      return true;
    }
    std::this_thread::sleep_for(options_.health_poll_interval);
  }

  SetStatus(SemanticRuntimeState::kFailed, SemanticRuntimeIssue::kReadinessTimeout,
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

void SemanticRuntimeService::AttachChildTreeCleanup() {
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
  HANDLE process_handle =
      OpenProcess(PROCESS_SET_QUOTA | PROCESS_TERMINATE, FALSE,
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

void SemanticRuntimeService::ReleaseChildTreeCleanup() {
#ifdef _WIN32
  if (job_object_ != nullptr) {
    CloseHandle(static_cast<HANDLE>(job_object_));
    job_object_ = nullptr;
  }
#endif
}

}  // namespace alcedo
