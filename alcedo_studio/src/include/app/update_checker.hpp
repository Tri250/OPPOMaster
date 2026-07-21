//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QNetworkAccessManager>
#include <QObject>
#include <QSettings>
#include <QString>
#include <QTimer>

#include <functional>
#include <optional>

namespace alcedo {

/// Semantic version representation for comparison.
struct SemanticVersion {
  int major_ = 0;
  int minor_ = 0;
  int patch_ = 0;
  QString pre_release_;
  QString build_metadata_;

  static auto FromString(const QString& version_str) -> std::optional<SemanticVersion>;
  auto ToString() const -> QString;
  auto IsNewerThan(const SemanticVersion& other) const -> bool;
  auto operator==(const SemanticVersion& o) const -> bool {
    return major_ == o.major_ && minor_ == o.minor_ && patch_ == o.patch_;
  }
  auto operator!=(const SemanticVersion& o) const -> bool { return !(*this == o); }
};

/// Information about an available update.
struct UpdateInfo {
  SemanticVersion version;
  QString         download_url;
  QString         release_notes;
  QString         release_page_url;
  QDateTime       published_at;
  bool            is_prerelease = false;
};

/// Checks GitHub releases for new versions and notifies the user.
///
/// Features:
/// - Background version check on startup (configurable)
/// - Semantic version comparison
/// - Skip-version functionality
/// - Persists last check time and user preferences
/// - Configurable check interval
class UpdateChecker : public QObject {
  Q_OBJECT

  Q_PROPERTY(bool autoCheckEnabled READ AutoCheckEnabled WRITE SetAutoCheckEnabled
                 NOTIFY SettingsChanged)
  Q_PROPERTY(int checkIntervalHours READ CheckIntervalHours WRITE SetCheckIntervalHours
                 NOTIFY SettingsChanged)
  Q_PROPERTY(bool checking READ IsChecking NOTIFY CheckStateChanged)
  Q_PROPERTY(QString lastCheckTime READ LastCheckTime NOTIFY CheckCompleted)

 public:
  explicit UpdateChecker(QObject* parent = nullptr);

  /// Start a background version check if enough time has elapsed.
  void MaybeCheckForUpdate();

  /// Force a version check regardless of the interval.
  Q_INVOKABLE void CheckForUpdate();

  /// Skip a specific version (won't notify again for this version).
  Q_INVOKABLE void SkipVersion(const QString& version);

  /// Whether the given version has been skipped by the user.
  Q_INVOKABLE bool IsVersionSkipped(const QString& version) const;

  /// Current app version.
  static auto CurrentVersion() -> SemanticVersion;

  // Settings accessors
  bool AutoCheckEnabled() const;
  void SetAutoCheckEnabled(bool enabled);
  int  CheckIntervalHours() const;
  void SetCheckIntervalHours(int hours);
  bool IsChecking() const { return checking_; }
  QString LastCheckTime() const;

  /// The GitHub repository for releases (configurable).
  void SetRepository(const QString& owner, const QString& repo);

 signals:
  void UpdateAvailable(const UpdateInfo& info);
  void NoUpdateAvailable();
  void CheckError(const QString& error);
  void CheckCompleted();
  void CheckStateChanged();
  void SettingsChanged();

 private:
  void OnCheckFinished();
  auto ParseReleaseResponse(const QByteArray& data) -> std::optional<UpdateInfo>;
  void PersistSettings();
  void LoadSettings();

  QNetworkAccessManager* network_ = nullptr;
  QSettings              settings_;
  QString                repo_owner_  = "alcedo-studio";
  QString                repo_name_   = "alcedo";
  bool                   checking_    = false;
  QTimer                 cooldown_timer_;
};

}  // namespace alcedo
