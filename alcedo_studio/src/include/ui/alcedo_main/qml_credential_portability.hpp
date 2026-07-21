//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QObject>
#include <QVariantMap>

#include "app/credential_portability.hpp"

namespace alcedo::ui {

/// QML-accessible wrapper around CredentialPortability.
/// Exposes the export/import workflow as Q_INVOKABLE methods that return
/// QVariantMaps so QML can read results without needing C++ struct access.
class QmlCredentialPortability : public QObject {
  Q_OBJECT
 public:
  explicit QmlCredentialPortability(CredentialPortability* backend, QObject* parent = nullptr)
      : QObject(parent), backend_(backend) {}

  /// Export credentials to an encrypted file.
  /// Returns {success, entriesProcessed, entriesSkipped, error}.
  Q_INVOKABLE QVariantMap ExportToFile(const QString& filePath, const QString& password) {
    if (!backend_) {
      QVariantMap r;
      r.insert("success", false);
      r.insert("entriesProcessed", 0);
      r.insert("entriesSkipped", 0);
      r.insert("error", tr("Credential service not available"));
      return r;
    }
    auto result = backend_->ExportToFile(
        filePath.toStdString(), password.toStdString());
    QVariantMap r;
    r.insert("success", result.success);
    r.insert("entriesProcessed", result.entries_processed);
    r.insert("entriesSkipped", result.entries_skipped);
    r.insert("error", QString::fromStdString(result.error));
    return r;
  }

  /// Import credentials from an encrypted file.
  /// Returns {success, entriesProcessed, entriesSkipped, error}.
  Q_INVOKABLE QVariantMap ImportFromFile(const QString& filePath,
                                         const QString& password,
                                         bool overwriteExisting = false) {
    if (!backend_) {
      QVariantMap r;
      r.insert("success", false);
      r.insert("entriesProcessed", 0);
      r.insert("entriesSkipped", 0);
      r.insert("error", tr("Credential service not available"));
      return r;
    }
    auto result = backend_->ImportFromFile(
        filePath.toStdString(), password.toStdString(), overwriteExisting);
    QVariantMap r;
    r.insert("success", result.success);
    r.insert("entriesProcessed", result.entries_processed);
    r.insert("entriesSkipped", result.entries_skipped);
    r.insert("error", QString::fromStdString(result.error));
    return r;
  }

 private:
  CredentialPortability* backend_;
};

}  // namespace alcedo::ui
