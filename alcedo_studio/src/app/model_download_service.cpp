//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/model_download_service.hpp"

#include "app/model_asset_catalog.hpp"

#include <QCoreApplication>
#include <QDir>
#include <QEventLoop>
#include <QHostAddress>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QJsonValue>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QNetworkRequest>
#include <QPointer>
#include <QProcess>
#include <QRandomGenerator>
#include <QTcpServer>
#include <QThread>
#include <QTimer>
#include <QUrl>
#include <algorithm>
#include <atomic>
#include <chrono>
#include <filesystem>
#include <memory>
#include <optional>
#include <system_error>
#include <variant>

namespace alcedo {

namespace {

constexpr int      kAria2ReadyTimeoutMs    = 10000;
constexpr int      kAria2RpcTimeoutMs      = 15000;
constexpr int      kAria2ProgressPollMs    = 250;
constexpr int      kAria2StopGracefulMs    = 2000;
constexpr int      kAria2StopKillMs        = 2000;
constexpr int      kAria2Connections       = 16;
constexpr const char* kAria2cBinaryEnv     = "ALCEDO_ARIA2C_BINARY";
#ifdef _WIN32
constexpr const char* kAria2cBinaryName    = "aria2c.exe";
#else
constexpr const char* kAria2cBinaryName    = "aria2c";
#endif

auto ToPath(const QString& value) -> std::filesystem::path {
#ifdef _WIN32
  return std::filesystem::path(value.toStdWString());
#else
  return std::filesystem::path(value.toStdString());
#endif
}

auto DefaultAria2cBinary() -> std::filesystem::path {
  const QByteArray env_binary = qgetenv(kAria2cBinaryEnv);
  if (!env_binary.isEmpty()) {
    return ToPath(QString::fromUtf8(env_binary));
  }
  const auto app_dir = QCoreApplication::applicationDirPath();
  return ToPath(app_dir) / kAria2cBinaryName;
}

auto ChooseFreePort() -> quint16 {
  QTcpServer server;
  if (server.listen(QHostAddress::LocalHost, 0)) {
    const auto port = static_cast<quint16>(server.serverPort());
    server.close();
    return port;
  }
  return 6800;
}

auto GenerateSecret() -> QString {
  QByteArray bytes;
  for (int i = 0; i < 16; ++i) {
    bytes.append(static_cast<char>(QRandomGenerator::global()->bounded(0, 256)));
  }
  return QString::fromUtf8(bytes.toHex());
}

// Synchronous aria2 JSON-RPC client over HTTP POST. aria2's /jsonrpc endpoint
// accepts plain JSON-RPC 2.0 POST requests (it also speaks WebSocket, but we
// poll via tellStatus so we don't need push notifications and can avoid a
// Qt6::WebSockets dependency). Lives on the worker thread and uses a nested
// QEventLoop to turn the async QNetworkReply into a blocking call() so the
// download orchestration reads as straight-line code.
class Aria2Rpc final : public QObject {
 public:
  explicit Aria2Rpc(QObject* parent = nullptr) : QObject(parent) {
    nam_ = new QNetworkAccessManager(this);
  }

  // Records the daemon endpoint + secret and probes it with getVersion.
  // Returns true once the daemon responds.
  auto ping(const QUrl& url, const QString& secret, int timeout_ms) -> bool {
    secret_    = "token:" + secret;
    base_url_  = url;
    const auto resp = call(QStringLiteral("aria2.getVersion"),
                           QJsonArray{secret_}, timeout_ms);
    return resp.contains(QStringLiteral("result"));
  }

  // Returns the JSON-RPC response object (contains "result" on success or
  // "error" on failure). Returns an empty object on timeout or transport error.
  auto call(const QString& method, const QJsonArray& params, int timeout_ms) -> QJsonObject {
    QJsonObject request;
    request.insert("jsonrpc", QStringLiteral("2.0"));
    request.insert("id", ++next_id_);
    request.insert("method", method);
    request.insert("params", params);

    QNetworkRequest req(base_url_);
    req.setHeader(QNetworkRequest::ContentTypeHeader, QStringLiteral("application/json"));
    const QByteArray body =
        QJsonDocument(request).toJson(QJsonDocument::Compact);

    QEventLoop          loop;
    QNetworkReply*      reply = nam_->post(req, body);
    QPointer<QNetworkReply> reply_guard(reply);
    QObject::connect(reply, &QNetworkReply::finished, &loop, &QEventLoop::quit);
    QTimer::singleShot(timeout_ms, &loop, [reply_guard]() {
      if (reply_guard) {
        reply_guard->abort();
      }
    });
    loop.exec();

    QJsonObject result;
    if (reply_guard && reply_guard->error() == QNetworkReply::NoError) {
      const auto doc = QJsonDocument::fromJson(reply_guard->readAll());
      if (doc.isObject()) {
        result = doc.object();
      }
    }
    if (reply_guard) {
      reply_guard->deleteLater();
    }
    return result;
  }

  auto secret() const -> QString { return secret_; }

 private:
  QNetworkAccessManager* nam_;
  QString                secret_;
  QUrl                   base_url_;
  int                    next_id_ = 0;
};

auto StopAria2Process(QProcess& process, Aria2Rpc& rpc) -> void {
  // Ask the daemon to exit cleanly, then fall back to terminate/kill.
  rpc.call(QStringLiteral("aria2.forceShutdown"),
           QJsonArray{rpc.secret(), QStringLiteral("true")}, 2000);
  if (process.state() != QProcess::NotRunning) {
    process.terminate();
    if (!process.waitForFinished(kAria2StopGracefulMs)) {
      process.kill();
      process.waitForFinished(kAria2StopKillMs);
    }
  }
}

}  // namespace

// Runs on a dedicated worker thread. Owns the aria2c process and the RPC
// client for the lifetime of a single download.
class ModelDownloadWorker final : public QObject {
  Q_OBJECT

 public:
  explicit ModelDownloadWorker(std::atomic<bool>& cancel_flag) : cancel_flag_(cancel_flag) {}

 signals:
  void ProgressChanged(const alcedo::ModelDownloadProgress& progress);
  void Finished(bool ok, const QString& error);

 public slots:
  void DoStartDownload(QString profile_id, QString model_root, QString hf_endpoint) {
    const auto* profile = FindSemanticProfile(profile_id.toStdString());
    if (profile == nullptr) {
      emit Finished(false, QStringLiteral("unknown semantic model profile %1").arg(profile_id));
      return;
    }

    const auto root    = ToPath(model_root) / profile->profile_id;
    const auto staging = StagingRoot(root);

    if (IsProfileInstalled(*profile, root)) {
      emit ProgressChanged(InstalledProgress(*profile));
      emit Finished(true, QString{});
      return;
    }

    std::error_code ec;
    std::filesystem::create_directories(staging, ec);
    if (ec) {
      emit Finished(false, QStringLiteral("failed to create staging directory %1: %2")
                                 .arg(QString::fromStdString(staging.string()))
                                 .arg(QString::fromStdString(ec.message())));
      return;
    }

    QProcess process;
    process.setProcessChannelMode(QProcess::SeparateChannels);
    const auto binary = DefaultAria2cBinary();
    if (!std::filesystem::exists(binary, ec)) {
      emit Finished(false, QStringLiteral("aria2c binary was not found at %1").arg(
                                 QString::fromStdString(binary.string())));
      return;
    }
    const quint16 port   = ChooseFreePort();
    const QString secret = GenerateSecret();
    QStringList    args;
    args << QStringLiteral("--enable-rpc") << QStringLiteral("--rpc-listen-port")
         << QString::number(port) << QStringLiteral("--rpc-secret") << secret
         << QStringLiteral("--rpc-allow-origin-all") << QStringLiteral("--quiet")
         << QStringLiteral("--max-concurrent-downloads=1") << QStringLiteral("--max-tries=5")
         << QStringLiteral("--retry-wait=2") << QStringLiteral("--file-allocation=none");
    process.setProgram(QString::fromStdString(binary.string()));
    process.setArguments(args);
    process.start();
    if (!process.waitForStarted(kAria2ReadyTimeoutMs)) {
      emit Finished(false, QStringLiteral("failed to start aria2c: %1").arg(process.errorString()));
      return;
    }

    Aria2Rpc rpc;
    const QUrl rpc_url(QStringLiteral("ws://127.0.0.1:%1/jsonrpc").arg(port));
    if (!WaitForRpc(rpc, rpc_url, secret)) {
      StopProcessOnly(process);
      emit Finished(false, QStringLiteral("aria2c RPC did not become ready"));
      return;
    }

    const auto result = RunDownloadLoop(*profile, root, staging, hf_endpoint, rpc);
    // Always stop the daemon before reporting the outcome.
    StopAria2Process(process, rpc);
    if (std::holds_alternative<QString>(result)) {
      emit Finished(false, std::get<QString>(result));
      return;
    }
    emit Finished(true, QString{});
  }

 private:
  auto IsProfileInstalled(const ModelProfileSpec& profile, const std::filesystem::path& root)
      -> bool {
    std::error_code ec;
    if (!std::filesystem::exists(root, ec)) {
      return false;
    }
    for (const auto& asset : profile.assets) {
      if (!std::filesystem::exists(root / asset.local_path, ec)) {
        return false;
      }
      if (ValidateAssetFile(asset, root / asset.local_path).has_value()) {
        return false;
      }
    }
    return true;
  }

  auto InstalledProgress(const ModelProfileSpec& profile) -> ModelDownloadProgress {
    ModelDownloadProgress progress;
    progress.phase            = "installed";
    progress.bytes_downloaded = ProfileTotalBytes(profile);
    progress.bytes_total      = progress.bytes_downloaded;
    progress.files_completed  = static_cast<std::uint32_t>(profile.assets.size());
    progress.files_total      = progress.files_completed;
    progress.message          = "model profile is already installed";
    return progress;
  }

  auto WaitForRpc(Aria2Rpc& rpc, const QUrl& url, const QString& secret) -> bool {
    const auto deadline = std::chrono::steady_clock::now() + std::chrono::milliseconds(kAria2ReadyTimeoutMs);
    while (std::chrono::steady_clock::now() < deadline) {
      if (cancel_flag_.load()) {
        return false;
      }
      if (rpc.ping(url, secret, 2000)) {
        return true;
      }
      QThread::msleep(250);
    }
    return false;
  }

  // Returns a string error on failure, or an empty std::monostate on success.
  auto RunDownloadLoop(const ModelProfileSpec& profile, const std::filesystem::path& root,
                       const std::filesystem::path& staging, const QString& hf_endpoint,
                       Aria2Rpc& rpc)
      -> std::variant<std::monostate, QString> {
    const auto bytes_total = ProfileTotalBytes(profile);
    const auto files_total = static_cast<std::uint32_t>(profile.assets.size());
    std::uint64_t bytes_completed = 0;

    EmitProgress(rpc, profile, "preparing", "", 0, bytes_total, 0, files_total,
                 QStringLiteral("preparing model download from %1").arg(hf_endpoint));

    for (std::size_t index = 0; index < profile.assets.size(); ++index) {
      const auto& asset = profile.assets[index];
      if (cancel_flag_.load()) {
        return QStringLiteral("download cancelled");
      }

      const auto staging_path = staging / asset.local_path;
      if (std::filesystem::exists(staging_path) && !ValidateAssetFile(asset, staging_path).has_value()) {
        bytes_completed += asset.size_bytes;
        EmitProgress(rpc, profile, "reused", asset.remote_path, bytes_completed, bytes_total,
                     static_cast<std::uint32_t>(index + 1), files_total,
                     QStringLiteral("reused staged %1").arg(QString::fromLatin1(asset.remote_path)));
        continue;
      }

      auto asset_error = DownloadAsset(rpc, asset, staging, hf_endpoint, index, bytes_total,
                                       files_total, bytes_completed, profile);
      if (asset_error.has_value()) {
        return asset_error.value();
      }

      if (const auto validate_error = ValidateAssetFile(asset, staging_path);
          validate_error.has_value()) {
        return QString::fromStdString(validate_error.value());
      }
      bytes_completed += asset.size_bytes;
      EmitProgress(rpc, profile, "validated", asset.remote_path, bytes_completed, bytes_total,
                   static_cast<std::uint32_t>(index + 1), files_total,
                   QStringLiteral("validated %1").arg(QString::fromLatin1(asset.remote_path)));
    }

    EmitProgress(rpc, profile, "promoting", "", bytes_total, bytes_total, files_total, files_total,
                 QStringLiteral("promoting staged model profile"));
    if (auto err = PromoteStagingRoot(staging, root); err.has_value()) {
      return QString::fromStdString(err.value());
    }
    if (auto err = WriteResolvedManifest(profile, root); err.has_value()) {
      return QString::fromStdString(err.value());
    }
    EmitProgress(rpc, profile, "installed", "", bytes_total, bytes_total, files_total, files_total,
                 QStringLiteral("model profile installed"));
    return std::monostate{};
  }

  auto DownloadAsset(Aria2Rpc& rpc, const ModelAssetSpec& asset,
                     const std::filesystem::path& staging, const QString& hf_endpoint,
                     std::size_t index, std::uint64_t bytes_total, std::uint32_t files_total,
                     std::uint64_t bytes_before, const ModelProfileSpec& profile)
      -> std::optional<QString> {
    const auto staging_path = staging / asset.local_path;
    std::error_code ec;
    std::filesystem::create_directories(staging_path.parent_path(), ec);

    QJsonObject options;
    options.insert(QStringLiteral("split"), QString::number(kAria2Connections));
    options.insert(QStringLiteral("max-connection-per-server"), QString::number(kAria2Connections));
    options.insert(QStringLiteral("continue"), QStringLiteral("true"));
    options.insert(QStringLiteral("dir"),
                   QDir::toNativeSeparators(
                       QString::fromStdString(staging_path.parent_path().string())));
    options.insert(QStringLiteral("out"),
                   QString::fromStdString(staging_path.filename().string()));
    options.insert(QStringLiteral("min-split-size"), QStringLiteral("1M"));
    options.insert(QStringLiteral("max-tries"), QStringLiteral("5"));
    options.insert(QStringLiteral("retry-wait"), QStringLiteral("2"));
    options.insert(QStringLiteral("file-allocation"), QStringLiteral("none"));

    QJsonArray add_params;
    add_params.append(rpc.secret());
    add_params.append(QJsonArray{QString::fromStdString(BuildAssetUrl(hf_endpoint.toStdString(), asset))});
    add_params.append(options);
    const auto add_resp =
        rpc.call(QStringLiteral("aria2.addUri"), add_params, kAria2RpcTimeoutMs);
    if (!add_resp.contains(QStringLiteral("result"))) {
      return FormatRpcError(add_resp, asset.remote_path);
    }
    const QString gid = add_resp.value(QStringLiteral("result")).toString();
    if (gid.isEmpty()) {
      return QStringLiteral("aria2 did not return a gid for %1")
          .arg(QString::fromLatin1(asset.remote_path));
    }

    const QJsonArray status_keys{QStringLiteral("status"), QStringLiteral("totalLength"),
                                 QStringLiteral("completedLength"), QStringLiteral("errorCode"),
                                 QStringLiteral("errorMessage")};
    while (true) {
      if (cancel_flag_.load()) {
        rpc.call(QStringLiteral("aria2.remove"), QJsonArray{rpc.secret(), gid}, 2000);
        return QStringLiteral("download cancelled");
      }
      const auto resp = rpc.call(
          QStringLiteral("aria2.tellStatus"), QJsonArray{rpc.secret(), gid, status_keys}, 3000);
      if (!resp.contains(QStringLiteral("result"))) {
        return FormatRpcError(resp, asset.remote_path);
      }
      const QJsonObject status = resp.value(QStringLiteral("result")).toObject();
      const QString     state  = status.value(QStringLiteral("status")).toString();
      const std::uint64_t completed =
          status.value(QStringLiteral("completedLength")).toString().toULongLong();
      const std::uint64_t clamped = std::min(completed, asset.size_bytes);

      EmitProgress(rpc, profile, "downloading", asset.remote_path,
                   bytes_before + clamped, bytes_total, static_cast<std::uint32_t>(index),
                   files_total,
                   QStringLiteral("downloading %1").arg(QString::fromLatin1(asset.remote_path)));

      if (state == QStringLiteral("complete")) {
        return std::nullopt;
      }
      if (state == QStringLiteral("error") || state == QStringLiteral("removed")) {
        const QString msg = status.value(QStringLiteral("errorMessage")).toString();
        return QStringLiteral("aria2 failed to download %1: %2")
            .arg(QString::fromLatin1(asset.remote_path))
            .arg(msg.isEmpty() ? state : msg);
      }
      QThread::msleep(kAria2ProgressPollMs);
    }
  }

  auto FormatRpcError(const QJsonObject& resp, const char* remote_path) -> QString {
    const QJsonObject error = resp.value(QStringLiteral("error")).toObject();
    const QString     msg   = error.value(QStringLiteral("message")).toString();
    return QStringLiteral("aria2 RPC error for %1: %2")
        .arg(QString::fromLatin1(remote_path))
        .arg(msg.isEmpty() ? QStringLiteral("unknown error") : msg);
  }

  void EmitProgress(Aria2Rpc& rpc, const ModelProfileSpec& profile, const char* phase,
                    const char* current_file, std::uint64_t bytes_downloaded,
                    std::uint64_t bytes_total, std::uint32_t files_completed,
                    std::uint32_t files_total, const QString& message) {
    // rpc is unused here but kept in the signature so callers read clearly.
    (void)rpc;
    (void)profile;
    ModelDownloadProgress progress;
    progress.phase            = phase;
    progress.current_file     = current_file;
    progress.bytes_downloaded = std::min(bytes_downloaded, bytes_total);
    progress.bytes_total      = bytes_total;
    progress.files_completed  = files_completed;
    progress.files_total      = files_total;
    progress.message          = message.toStdString();
    emit ProgressChanged(progress);
  }

  void StopProcessOnly(QProcess& process) {
    if (process.state() == QProcess::NotRunning) {
      return;
    }
    process.terminate();
    if (!process.waitForFinished(kAria2StopGracefulMs)) {
      process.kill();
      process.waitForFinished(kAria2StopKillMs);
    }
  }

  std::atomic<bool>& cancel_flag_;
};

struct ModelDownloadService::Impl {
  std::atomic<bool>    cancel_flag_{false};
  std::atomic<bool>    running_{false};
  QThread              worker_thread_;
  ModelDownloadWorker* worker_ = nullptr;

  Impl() {
    worker_ = new ModelDownloadWorker(cancel_flag_);
    worker_->moveToThread(&worker_thread_);
    worker_thread_.start();
  }

  ~Impl() {
    worker_thread_.quit();
    worker_thread_.wait();
    // worker_ is child-free; delete after the thread stopped.
    delete worker_;
  }
};

ModelDownloadService::ModelDownloadService(QObject* parent)
    : QObject(parent), impl_(std::make_unique<Impl>()) {
  qRegisterMetaType<alcedo::ModelDownloadProgress>("alcedo::ModelDownloadProgress");
  QObject::connect(impl_->worker_, &ModelDownloadWorker::ProgressChanged, this,
                   &ModelDownloadService::ProgressChanged);
  QObject::connect(impl_->worker_, &ModelDownloadWorker::Finished, this,
                   [this](bool ok, const QString& error) {
                     impl_->running_.store(false);
                     emit Finished(ok, error);
                   });
}

ModelDownloadService::~ModelDownloadService() = default;

auto ModelDownloadService::StartDownload(const std::string& profile_id,
                                         const std::filesystem::path& model_root,
                                         const std::string& hf_endpoint) -> bool {
  if (impl_->running_.exchange(true)) {
    return false;
  }
  impl_->cancel_flag_.store(false);
  const QString q_profile  = QString::fromStdString(profile_id);
  const QString q_root     = QString::fromStdString(model_root.string());
  const QString q_endpoint = QString::fromStdString(hf_endpoint);
  // Capture the worker by raw pointer; the service outlives the call.
  auto* worker = impl_->worker_;
  QMetaObject::invokeMethod(
      worker,
      [worker, q_profile, q_root, q_endpoint]() {
        worker->DoStartDownload(q_profile, q_root, q_endpoint);
      },
      Qt::QueuedConnection);
  return true;
}

void ModelDownloadService::CancelDownload() { impl_->cancel_flag_.store(true); }

auto ModelDownloadService::IsRunning() const -> bool { return impl_->running_.load(); }

}  // namespace alcedo

#include "model_download_service.moc"
