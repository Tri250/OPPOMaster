//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/update_checker.hpp"

#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QNetworkReply>
#include <QSslConfiguration>
#include <QSslSocket>
#include <QRegularExpression>
#include <QTimeZone>

#include "utils/diagnostics/app_logging.hpp"

namespace alcedo {

auto SemanticVersion::FromString(const QString& version_str) -> std::optional<SemanticVersion> {
  // Match semver: major.minor.patch[-prerelease][+build]
  static const QRegularExpression re(
      QStringLiteral(R"(^v?(\d+)\.(\d+)\.(\d+)(?:-([a-zA-Z0-9.]+))?(?:\+([a-zA-Z0-9.]+))?$)"));
  QRegularExpressionMatch match = re.match(version_str.trimmed());
  if (!match.hasMatch()) {
    return std::nullopt;
  }

  SemanticVersion v;
  v.major_           = match.captured(1).toInt();
  v.minor_           = match.captured(2).toInt();
  v.patch_           = match.captured(3).toInt();
  v.pre_release_     = match.captured(4);
  v.build_metadata_  = match.captured(5);
  return v;
}

auto SemanticVersion::ToString() const -> QString {
  QString s = QStringLiteral("%1.%2.%3").arg(major_).arg(minor_).arg(patch_);
  if (!pre_release_.isEmpty()) {
    s += QStringLiteral("-%1").arg(pre_release_);
  }
  if (!build_metadata_.isEmpty()) {
    s += QStringLiteral("+%1").arg(build_metadata_);
  }
  return s;
}

auto SemanticVersion::IsNewerThan(const SemanticVersion& other) const -> bool {
  if (major_ != other.major_) return major_ > other.major_;
  if (minor_ != other.minor_) return minor_ > other.minor_;
  if (patch_ != other.patch_) return patch_ > other.patch_;
  // Pre-release versions have lower precedence than release
  if (!pre_release_.isEmpty() && other.pre_release_.isEmpty()) return false;
  if (pre_release_.isEmpty() && !other.pre_release_.isEmpty()) return true;
  return pre_release_ > other.pre_release_;
}

UpdateChecker::UpdateChecker(QObject* parent)
    : QObject(parent),
      settings_(QSettings::UserScope, QStringLiteral("AlcedoStudio"),
                QStringLiteral("UpdateChecker")) {
  network_ = new QNetworkAccessManager(this);
  LoadSettings();
}

void UpdateChecker::MaybeCheckForUpdate() {
  if (!AutoCheckEnabled()) {
    return;
  }

  const QDateTime last_check = settings_.value("last_check").toDateTime();
  const int       interval_h = CheckIntervalHours();

  if (last_check.isValid()) {
    const qint64 elapsed_h = last_check.secsTo(QDateTime::currentDateTime()) / 3600;
    if (elapsed_h < interval_h) {
      return;  // Not enough time has elapsed
    }
  }

  CheckForUpdate();
}

void UpdateChecker::CheckForUpdate() {
  if (checking_) {
    return;
  }

  checking_ = true;
  emit CheckStateChanged();

  QUrl url(QStringLiteral("https://api.github.com/repos/%1/%2/releases/latest")
               .arg(repo_owner_, repo_name_));

  QNetworkRequest request(url);
  request.setHeader(QNetworkRequest::UserAgentHeader,
                    QStringLiteral("AlcedoStudio/%1").arg(CurrentVersion().ToString()));

  // Enforce HTTPS with proper SSL certificate validation
  QSslConfiguration ssl_config = QSslConfiguration::defaultConfiguration();
  ssl_config.setProtocol(QSsl::TlsV1_2OrLater);
  ssl_config.setPeerVerifyMode(QSslSocket::VerifyPeer);
  // Do not allow ignoring SSL errors — reject connections with invalid certs
  request.setSslConfiguration(ssl_config);

  QNetworkReply* reply = network_->get(request);
  // Explicitly reject SSL errors (do not auto-ignore)
  connect(reply, &QNetworkReply::sslErrors, this, [](const QList<QSslError>& errors) {
    qCWarning(diag::appLog).noquote()
        << QStringLiteral("UpdateChecker: SSL errors encountered, aborting: %1")
               .arg(errors.isEmpty() ? QStringLiteral("unknown")
                                     : errors.first().errorString());
  });
  connect(reply, &QNetworkReply::finished, this, [this, reply]() {
    reply->deleteLater();
    checking_ = false;
    emit CheckStateChanged();

    if (reply->error() != QNetworkReply::NoError) {
      emit CheckError(reply->errorString());
      return;
    }

    auto info = ParseReleaseResponse(reply->readAll());
    if (!info.has_value()) {
      emit CheckError(tr("Failed to parse update information"));
      return;
    }

    settings_.setValue("last_check", QDateTime::currentDateTime());

    if (info->version.IsNewerThan(CurrentVersion())) {
      if (IsVersionSkipped(info->version.ToString())) {
        emit NoUpdateAvailable();
      } else {
        emit UpdateAvailable(*info);
      }
    } else {
      emit NoUpdateAvailable();
    }

    emit CheckCompleted();
  });
}

void UpdateChecker::SkipVersion(const QString& version) {
  QStringList skipped = settings_.value("skipped_versions").toStringList();
  if (!skipped.contains(version)) {
    skipped.append(version);
    settings_.setValue("skipped_versions", skipped);
  }
}

bool UpdateChecker::IsVersionSkipped(const QString& version) const {
  const QStringList skipped = settings_.value("skipped_versions").toStringList();
  return skipped.contains(version);
}

auto UpdateChecker::CurrentVersion() -> SemanticVersion {
  // Read from project version define
  auto v = SemanticVersion::FromString(QStringLiteral(ALCEDO_APP_VERSION));
  return v.value_or(SemanticVersion{0, 2, 7});
}

bool UpdateChecker::AutoCheckEnabled() const {
  return settings_.value("auto_check", true).toBool();
}

void UpdateChecker::SetAutoCheckEnabled(bool enabled) {
  settings_.setValue("auto_check", enabled);
  emit SettingsChanged();
}

int UpdateChecker::CheckIntervalHours() const {
  return settings_.value("interval_hours", 24).toInt();
}

void UpdateChecker::SetCheckIntervalHours(int hours) {
  settings_.setValue("interval_hours", qMax(1, hours));
  emit SettingsChanged();
}

QString UpdateChecker::LastCheckTime() const {
  const QDateTime dt = settings_.value("last_check").toDateTime();
  return dt.isValid() ? dt.toString(Qt::DefaultLocaleShortDate) : tr("Never");
}

void UpdateChecker::SetRepository(const QString& owner, const QString& repo) {
  repo_owner_ = owner;
  repo_name_  = repo;
}

auto UpdateChecker::ParseReleaseResponse(const QByteArray& data)
    -> std::optional<UpdateInfo> {
  QJsonParseError error;
  QJsonDocument   doc = QJsonDocument::fromJson(data, &error);
  if (error.error != QJsonParseError::NoError || !doc.isObject()) {
    return std::nullopt;
  }

  QJsonObject root = doc.object();
  const QString tag_name = root.value("tag_name").toString();
  auto version = SemanticVersion::FromString(tag_name);
  if (!version.has_value()) {
    return std::nullopt;
  }

  UpdateInfo info;
  info.version         = *version;
  info.release_notes   = root.value("body").toString();
  info.release_page_url = root.value("html_url").toString();
  info.is_prerelease   = root.value("prerelease").toBool();
  info.published_at    = QDateTime::fromString(root.value("published_at").toString(),
                                               Qt::ISODate);

  // Find the download URL for the platform-appropriate asset
  const QJsonArray assets = root.value("assets").toArray();
  for (const QJsonValue& asset_val : assets) {
    const QJsonObject asset = asset_val.toObject();
    const QString name = asset.value("name").toString().toLower();
    // Prefer platform-specific downloads
#if defined(_WIN32)
    if (name.endsWith(".msi") || name.endsWith(".exe")) {
      info.download_url = asset.value("browser_download_url").toString();
      break;
    }
#elif defined(__APPLE__)
    if (name.endsWith(".dmg")) {
      info.download_url = asset.value("browser_download_url").toString();
      break;
    }
#elif defined(__linux__)
    if (name.endsWith(".appimage")) {
      info.download_url = asset.value("browser_download_url").toString();
      break;
    }
#endif
  }

  return info;
}

void UpdateChecker::PersistSettings() {
  settings_.sync();
}

void UpdateChecker::LoadSettings() {
  // Settings are loaded lazily by QSettings
}

}  // namespace alcedo
