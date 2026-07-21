//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QCryptographicHash>
#include <QFile>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QObject>
#include <QStandardPaths>
#include <QString>
#include <QTimer>
#include <cstdint>
#include <filesystem>
#include <functional>
#include <optional>
#include <string>
#include <unordered_map>

namespace alcedo {

/// Describes a single ONNX model file that the application requires.
struct ModelFileEntry {
  std::string filename;     ///< e.g. "bayer.onnx"
  std::string sha256;       ///< Expected SHA-256 hex digest (lowercase)
  uint64_t    size_bytes;  ///< Expected file size in bytes
  std::string url_path;     ///< Relative path on the download server
};

/// Result of a model integrity check.
enum class ModelVerifyResult {
  kOk,
  kFileMissing,
  kSizeMismatch,
  kHashMismatch,
  kIoError,
};

/// Configuration for the model distribution service.
struct ModelDistributionConfig {
  /// Base URL for downloading models. Defaults to GitHub Releases; can be
  /// overridden to a mirror for Chinese users.
  std::string base_url = "https://github.com/alcedo-studio/alcedo/releases/download/models";

  /// Alternative mirror URL for users in regions with poor GitHub access
  /// (e.g. Chinese mainland). If set and the primary download fails, the
  /// mirror will be tried automatically.
  std::string mirror_url;

  /// Local directory for storing downloaded models. Defaults to
  /// QStandardPaths::AppDataLocation + "/models".
  std::filesystem::path model_dir;

  /// Number of retry attempts for failed downloads.
  int max_retries = 3;

  /// Enable resumable downloads (using HTTP Range headers).
  bool enable_resume = true;
};

/// Progress information for a model download.
struct ModelDistProgress {
  std::string  filename;
  uint64_t     bytes_downloaded = 0;
  uint64_t     bytes_total      = 0;
  double       percent          = 0.0;
  std::string  message;
};

/// Service that checks for required ONNX model files on startup, downloads
/// missing models from GitHub Releases or a configurable mirror, verifies
/// SHA256 checksums, and supports resumable downloads.
class ModelDistributionService final : public QObject {
  Q_OBJECT

 public:
  explicit ModelDistributionService(QObject* parent = nullptr);
  ~ModelDistributionService() override;

  ModelDistributionService(const ModelDistributionService&)            = delete;
  ModelDistributionService& operator=(const ModelDistributionService&) = delete;

  /// Sets the configuration. Must be called before CheckAndDownload().
  void SetConfig(const ModelDistributionConfig& config);

  /// Returns the current configuration.
  [[nodiscard]] auto GetConfig() const -> const ModelDistributionConfig&;

  /// Checks for required model files and starts downloading any that are
  /// missing or corrupted. Returns the number of models that need to be
  /// downloaded (0 if all models are present and valid).
  auto CheckAndDownload() -> int;

  /// Cancels any in-progress download.
  void CancelDownload();

  /// Returns true if a download is currently in progress.
  [[nodiscard]] auto IsDownloading() const -> bool;

  /// Verifies a single model file against its expected hash and size.
  [[nodiscard]] static auto VerifyModel(const ModelFileEntry& entry,
                                         const std::filesystem::path& path) -> ModelVerifyResult;

  /// Computes the SHA-256 hex digest of a file. Returns empty string on error.
  [[nodiscard]] static auto ComputeFileSha256(const std::filesystem::path& path) -> std::string;

  /// Returns the list of required model files.
  [[nodiscard]] static auto GetRequiredModels() -> const std::vector<ModelFileEntry>&;

  /// Returns the default model storage directory.
  [[nodiscard]] static auto DefaultModelDir() -> std::filesystem::path;

 signals:
  /// Emitted periodically during download to report progress.
  void ProgressChanged(const alcedo::ModelDistProgress& progress);

  /// Emitted when all model downloads are complete (or failed).
  /// ok is true if all models were successfully downloaded and verified.
  void DownloadComplete(bool ok, const QString& message);

  /// Emitted when a single model file download finishes (success or failure).
  void ModelFileComplete(const QString& filename, bool ok, const QString& error);

 private:
  void StartNextDownload();
  void DownloadFile(const ModelFileEntry& entry);
  void OnDownloadProgress(qint64 bytes_received, qint64 bytes_total);
  void OnDownloadFinished();
  void OnDownloadReadyRead();
  auto TryMirrorUrl(const ModelFileEntry& entry) -> QUrl;

  ModelDistributionConfig config_;
  QNetworkAccessManager*  network_manager_ = nullptr;
  QNetworkReply*          current_reply_   = nullptr;
  QFile                   current_output_file_;
  std::vector<ModelFileEntry> pending_downloads_;
  int                     current_index_   = 0;
  bool                    downloading_     = false;
  bool                    cancelled_       = false;
  bool                    using_mirror_    = false;
  uint64_t                resume_offset_   = 0;
};

}  // namespace alcedo
