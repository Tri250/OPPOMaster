//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/model_distribution_service.hpp"

#include <QDir>
#include <QFileInfo>
#include <QNetworkRequest>
#include <QUrl>
#include <QDebug>
#include <QCoreApplication>
#include <QStandardPaths>

#include <algorithm>
#include <fstream>
#include <nlohmann/json.hpp>

namespace alcedo {

namespace {

// Required ONNX model files with their expected SHA-256 hashes and sizes.
// These serve as compile-time defaults; at runtime, the model_hashes.json
// manifest file is loaded and takes precedence.
const std::vector<ModelFileEntry>& GetBuiltinModelManifest() {
  static const std::vector<ModelFileEntry> kModels = {
      {
          "bayer.onnx",
          "0000000000000000000000000000000000000000000000000000000000000000",
          0,  // Size will be determined from the download
          "models/bayer.onnx"
      },
      {
          "xtrans.onnx",
          "0000000000000000000000000000000000000000000000000000000000000000",
          0,
          "models/xtrans.onnx"
      },
  };
  return kModels;
}

/// Tries to load the model_hashes.json manifest from the application's
/// config directory or the bundled config resource. Returns a map from
/// filename to {sha256, size_bytes}. Returns an empty map on failure.
auto LoadHashManifest() -> std::unordered_map<std::string, std::pair<std::string, uint64_t>> {
  std::unordered_map<std::string, std::pair<std::string, uint64_t>> result;

  // Try loading from the config directory first
  const QString config_dir = QStandardPaths::writableLocation(QStandardPaths::AppDataLocation);
  const QString manifest_path = config_dir + QStringLiteral("/model_hashes.json");

  std::string path_to_load;
  if (QFileInfo::exists(manifest_path)) {
    path_to_load = manifest_path.toStdString();
  } else {
    // Try the bundled config directory (next to the executable)
    const QString bundle_path = QCoreApplication::applicationDirPath()
                                + QStringLiteral("/../share/alcedo-studio/config/model_hashes.json");
    if (QFileInfo::exists(bundle_path)) {
      path_to_load = bundle_path.toStdString();
    }
  }

  if (path_to_load.empty()) {
    return result;
  }

  try {
    std::ifstream in(path_to_load);
    if (!in.is_open()) {
      return result;
    }
    auto json = nlohmann::json::parse(in, nullptr, false);
    if (json.is_discarded() || !json.contains("models")) {
      return result;
    }
    const auto& models = json["models"];
    for (auto it = models.begin(); it != models.end(); ++it) {
      const auto& key = it.key();
      const auto& val = it.value();
      std::string sha256;
      uint64_t size = 0;
      if (val.contains("sha256") && val["sha256"].is_string()) {
        sha256 = val["sha256"].get<std::string>();
      }
      if (val.contains("size_bytes") && val["size_bytes"].is_number()) {
        size = val["size_bytes"].get<uint64_t>();
      }
      result[key] = {sha256, size};
    }
  } catch (...) {
    qWarning() << "Failed to load model_hashes.json manifest";
  }

  return result;
}

// The resolved manifest (builtin defaults + any overrides from JSON).
static std::vector<ModelFileEntry> g_resolved_manifest;
static bool g_manifest_resolved = false;

const std::vector<ModelFileEntry>& GetModelManifest() {
  if (g_manifest_resolved) {
    return g_resolved_manifest;
  }

  auto overrides = LoadHashManifest();
  g_resolved_manifest = GetBuiltinModelManifest();

  // Apply overrides from the manifest file
  for (auto& entry : g_resolved_manifest) {
    auto it = overrides.find(entry.filename);
    if (it != overrides.end()) {
      if (!it->second.first.empty()) {
        entry.sha256 = it->second.first;
      }
      if (it->second.second > 0) {
        entry.size_bytes = it->second.second;
      }
    }
  }

  // Add any models from the manifest that are not in the builtin list
  for (const auto& [filename, hash_size] : overrides) {
    bool found = false;
    for (const auto& entry : g_resolved_manifest) {
      if (entry.filename == filename) {
        found = true;
        break;
      }
    }
    if (!found) {
      g_resolved_manifest.push_back({
          filename,
          hash_size.first,
          hash_size.second,
          "models/" + filename
      });
    }
  }

  g_manifest_resolved = true;
  return g_resolved_manifest;
}

}  // namespace

ModelDistributionService::ModelDistributionService(QObject* parent)
    : QObject(parent),
      network_manager_(new QNetworkAccessManager(this)) {
  config_.model_dir = DefaultModelDir();
}

ModelDistributionService::~ModelDistributionService() {
  CancelDownload();
}

void ModelDistributionService::SetConfig(const ModelDistributionConfig& config) {
  config_ = config;
  if (config_.model_dir.empty()) {
    config_.model_dir = DefaultModelDir();
  }
}

auto ModelDistributionService::GetConfig() const -> const ModelDistributionConfig& {
  return config_;
}

auto ModelDistributionService::DefaultModelDir() -> std::filesystem::path {
  const QString app_data = QStandardPaths::writableLocation(QStandardPaths::AppDataLocation);
  const QString model_dir = app_data + QStringLiteral("/models");
  return std::filesystem::path(model_dir.toUtf8().constData());
}

auto ModelDistributionService::GetRequiredModels() -> const std::vector<ModelFileEntry>& {
  return GetModelManifest();
}

auto ModelDistributionService::CheckAndDownload() -> int {
  if (downloading_) {
    return 0;
  }

  // Ensure the model directory exists
  std::error_code ec;
  std::filesystem::create_directories(config_.model_dir, ec);
  if (ec) {
    emit DownloadComplete(false,
        QString(QStringLiteral("Failed to create model directory: %1"))
            .arg(QString::fromStdString(ec.message())));
    return 0;
  }

  // Check which models need to be downloaded
  pending_downloads_.clear();
  for (const auto& entry : GetModelManifest()) {
    const auto local_path = config_.model_dir / entry.filename;
    const auto verify = VerifyModel(entry, local_path);
    if (verify != ModelVerifyResult::kOk) {
      // If the file exists but is corrupted, delete it so we can re-download
      if (verify == ModelVerifyResult::kHashMismatch
          || verify == ModelVerifyResult::kSizeMismatch) {
        std::filesystem::remove(local_path, ec);
        qWarning() << "Model file corrupted, will re-download:" << entry.filename.c_str();
      }
      pending_downloads_.push_back(entry);
    }
  }

  if (pending_downloads_.empty()) {
    emit DownloadComplete(true, QStringLiteral("All models are present and verified."));
    return 0;
  }

  downloading_ = true;
  cancelled_ = false;
  current_index_ = 0;
  using_mirror_ = false;
  StartNextDownload();

  return static_cast<int>(pending_downloads_.size());
}

void ModelDistributionService::StartNextDownload() {
  if (cancelled_ || current_index_ >= static_cast<int>(pending_downloads_.size())) {
    downloading_ = false;
    if (cancelled_) {
      emit DownloadComplete(false, QStringLiteral("Download cancelled."));
    } else {
      emit DownloadComplete(true,
          QString(QStringLiteral("All %1 model(s) downloaded and verified successfully."))
              .arg(pending_downloads_.size()));
    }
    return;
  }

  const auto& entry = pending_downloads_[current_index_];
  DownloadFile(entry);
}

void ModelDistributionService::DownloadFile(const ModelFileEntry& entry) {
  const auto local_path = config_.model_dir / entry.filename;

  // Determine if we should resume an existing partial download
  resume_offset_ = 0;
  if (config_.enable_resume && std::filesystem::exists(local_path)) {
    resume_offset_ = static_cast<uint64_t>(std::filesystem::file_size(local_path));
  }

  // Build the URL
  QUrl url;
  if (using_mirror_ && !config_.mirror_url.empty()) {
    url = QUrl(QString::fromStdString(config_.mirror_url + "/" + entry.url_path));
  } else {
    url = QUrl(QString::fromStdString(config_.base_url + "/" + entry.url_path));
  }

  QNetworkRequest request(url);
  request.setAttribute(QNetworkRequest::FollowRedirectsAttribute, true);
  request.setHeader(QNetworkRequest::UserAgentHeader,
                    QStringLiteral("AlcedoStudio/1.0"));

  if (resume_offset_ > 0) {
    const QByteArray range_header = QByteArray("bytes=")
        + QByteArray::number(static_cast<qint64>(resume_offset_)) + "-";
    request.setRawHeader("Range", range_header);
  }

  // Open the output file in append mode if resuming, otherwise write mode
  current_output_file_.setFileName(QString::fromStdString(local_path.string()));
  if (resume_offset_ > 0) {
    current_output_file_.open(QIODevice::Append | QIODevice::Unbuffered);
  } else {
    current_output_file_.open(QIODevice::WriteOnly | QIODevice::Unbuffered);
  }

  if (!current_output_file_.isOpen()) {
    qWarning() << "Failed to open output file:" << current_output_file_.fileName();
    emit ModelFileComplete(QString::fromStdString(entry.filename), false,
                           current_output_file_.errorString());
    current_index_++;
    StartNextDownload();
    return;
  }

  current_reply_ = network_manager_->get(request);

  connect(current_reply_, &QNetworkReply::downloadProgress,
          this, &ModelDistributionService::OnDownloadProgress);
  connect(current_reply_, &QNetworkReply::finished,
          this, &ModelDistributionService::OnDownloadFinished);
  connect(current_reply_, &QNetworkReply::readyRead,
          this, &ModelDistributionService::OnDownloadReadyRead);

  ModelDistProgress progress;
  progress.filename = entry.filename;
  progress.message = "Downloading " + entry.filename + "...";
  progress.bytes_downloaded = resume_offset_;
  progress.bytes_total = entry.size_bytes > 0 ? entry.size_bytes : 0;
  emit ProgressChanged(progress);
}

void ModelDistributionService::OnDownloadProgress(qint64 bytes_received, qint64 bytes_total) {
  if (!current_reply_ || current_index_ >= static_cast<int>(pending_downloads_.size())) {
    return;
  }

  const auto& entry = pending_downloads_[current_index_];
  ModelDistProgress progress;
  progress.filename = entry.filename;
  progress.bytes_downloaded = static_cast<uint64_t>(resume_offset_ + bytes_received);
  progress.bytes_total = static_cast<uint64_t>(resume_offset_ + bytes_total);
  if (progress.bytes_total > 0) {
    progress.percent = static_cast<double>(progress.bytes_downloaded) / progress.bytes_total * 100.0;
  }
  progress.message = "Downloading " + entry.filename
      + " (" + std::to_string(static_cast<int>(progress.percent)) + "%)";
  emit ProgressChanged(progress);
}

void ModelDistributionService::OnDownloadReadyRead() {
  if (current_reply_ && current_output_file_.isOpen()) {
    current_output_file_.write(current_reply_->readAll());
  }
}

void ModelDistributionService::OnDownloadFinished() {
  if (!current_reply_) {
    return;
  }

  current_output_file_.close();

  const auto& entry = pending_downloads_[current_index_];
  const auto local_path = config_.model_dir / entry.filename;
  bool success = false;
  QString error_msg;

  if (current_reply_->error() == QNetworkReply::NoError) {
    // Verify the downloaded file
    const auto verify = VerifyModel(entry, local_path);
    if (verify == ModelVerifyResult::kOk
        || verify == ModelVerifyResult::kSizeMismatch) {
      // Size mismatch with 0 expected size is OK (we didn't know the size)
      if (verify == ModelVerifyResult::kSizeMismatch && entry.size_bytes == 0) {
        success = true;
      } else if (verify == ModelVerifyResult::kOk) {
        success = true;
      } else {
        error_msg = QStringLiteral("Size mismatch for %1").arg(entry.filename.c_str());
        std::error_code ec;
        std::filesystem::remove(local_path, ec);
      }
    } else if (verify == ModelVerifyResult::kHashMismatch) {
      // Hash mismatch — delete and potentially retry from mirror
      error_msg = QStringLiteral("SHA-256 hash mismatch for %1").arg(entry.filename.c_str());
      std::error_code ec;
      std::filesystem::remove(local_path, ec);

      // If we haven't tried the mirror yet, try it now
      if (!using_mirror_ && !config_.mirror_url.empty()) {
        using_mirror_ = true;
        current_reply_->deleteLater();
        current_reply_ = nullptr;
        DownloadFile(entry);
        return;
      }
    } else {
      error_msg = QStringLiteral("Verification failed for %1 (result=%2)")
                      .arg(entry.filename.c_str())
                      .arg(static_cast<int>(verify));
    }
  } else {
    error_msg = current_reply_->errorString();

    // If primary URL failed and we haven't tried the mirror, try it
    if (!using_mirror_ && !config_.mirror_url.empty()) {
      using_mirror_ = true;
      current_reply_->deleteLater();
      current_reply_ = nullptr;
      DownloadFile(entry);
      return;
    }
  }

  current_reply_->deleteLater();
  current_reply_ = nullptr;

  emit ModelFileComplete(QString::fromStdString(entry.filename), success, error_msg);

  current_index_++;
  using_mirror_ = false;  // Reset for next file
  StartNextDownload();
}

void ModelDistributionService::CancelDownload() {
  if (!downloading_) {
    return;
  }
  cancelled_ = true;
  if (current_reply_) {
    current_reply_->abort();
  }
  if (current_output_file_.isOpen()) {
    current_output_file_.close();
  }
}

auto ModelDistributionService::IsDownloading() const -> bool {
  return downloading_;
}

auto ModelDistributionService::VerifyModel(const ModelFileEntry& entry,
                                            const std::filesystem::path& path) -> ModelVerifyResult {
  if (!std::filesystem::exists(path)) {
    return ModelVerifyResult::kFileMissing;
  }

  std::error_code ec;
  const auto file_size = std::filesystem::file_size(path, ec);
  if (ec) {
    return ModelVerifyResult::kIoError;
  }

  // Check size if expected size is known
  if (entry.size_bytes > 0 && file_size != entry.size_bytes) {
    return ModelVerifyResult::kSizeMismatch;
  }

  // Check SHA-256 hash if one is pinned
  if (!entry.sha256.empty() && entry.sha256 != std::string(64, '0')) {
    const auto actual_hash = ComputeFileSha256(path);
    if (actual_hash.empty()) {
      return ModelVerifyResult::kIoError;
    }
    // Case-insensitive comparison
    std::string expected = entry.sha256;
    std::transform(expected.begin(), expected.end(), expected.begin(), ::tolower);
    std::string actual = actual_hash;
    std::transform(actual.begin(), actual.end(), actual.begin(), ::tolower);
    if (expected != actual) {
      return ModelVerifyResult::kHashMismatch;
    }
  }

  return ModelVerifyResult::kOk;
}

auto ModelDistributionService::ComputeFileSha256(const std::filesystem::path& path) -> std::string {
  QFile file(QString::fromStdString(path.string()));
  if (!file.open(QIODevice::ReadOnly)) {
    return {};
  }

  QCryptographicHash hash(QCryptographicHash::Sha256);
  constexpr qint64 kBufferSize = 1024 * 1024;  // 1 MB
  while (!file.atEnd()) {
    const QByteArray chunk = file.read(kBufferSize);
    if (chunk.isEmpty()) {
      break;
    }
    hash.addData(chunk);
  }

  return QString::fromLatin1(hash.result().toHex()).toStdString();
}

}  // namespace alcedo
