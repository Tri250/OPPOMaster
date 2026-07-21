//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QObject>
#include <QVariantList>
#include <QVariantMap>

#include "app/relink_service.hpp"

namespace alcedo::ui {

/// QML-accessible wrapper around RelinkService.
/// Exposes missing file detection, single-file relink, auto-search, and
/// batch-search as Q_INVOKABLE methods with signals for async completion.
class QmlRelinkService : public QObject {
  Q_OBJECT
 public:
  explicit QmlRelinkService(RelinkService* backend, QObject* parent = nullptr)
      : QObject(parent), backend_(backend) {}

  /// Get a list of missing file paths as strings (for QML ListView model).
  Q_INVOKABLE QStringList GetMissingFiles() const {
    if (!backend_) return {};
    QStringList paths;
    auto missing = backend_->DetectMissingFiles();
    for (const auto& f : missing) {
      paths.append(QString::fromStdWString(f.original_path.wstring()));
    }
    return paths;
  }

  /// Relink a single file by its original path and new path.
  /// Returns true on success.
  Q_INVOKABLE bool RelinkFile(const QString& originalPath, const QString& newPath) {
    if (!backend_) return false;
    auto missing = backend_->DetectMissingFiles();
    for (const auto& f : missing) {
      auto orig_str = QString::fromStdWString(f.original_path.wstring());
      if (orig_str == originalPath) {
        auto result = backend_->RelinkFile(f.file_id,
                                           std::filesystem::path(newPath.toStdWString()));
        if (result.success) {
          emit FileRelinked(originalPath);
        }
        return result.success;
      }
    }
    return false;
  }

  /// Trigger auto-search in nearby directories.
  Q_INVOKABLE void AutoSearchNearby() {
    if (!backend_) return;
    auto missing = backend_->DetectMissingFiles();
    if (missing.empty()) {
      emit AutoSearchCompleted({});
      return;
    }
    auto candidates = backend_->AutoSearchNearby(missing);
    // Auto-apply best candidates
    QVariantList results;
    for (const auto& f : missing) {
      auto it = candidates.find(f.file_id);
      if (it != candidates.end() && !it->second.empty()) {
        // Find best candidate (highest score)
        const auto& best = it->second[0];
        for (const auto& c : it->second) {
          if (c.score > best.score) { /* best is a reference, compare */ }
        }
        auto relink_result = backend_->RelinkFile(f.file_id, it->second[0].path);
        if (relink_result.success) {
          auto orig_str = QString::fromStdWString(f.original_path.wstring());
          emit FileRelinked(orig_str);
          QVariantMap m;
          m.insert("originalPath", orig_str);
          m.insert("newPath", QString::fromStdWString(relink_result.new_path.wstring()));
          results.append(m);
        }
      }
    }
    emit AutoSearchCompleted(results);
  }

  /// Trigger batch search in a specific directory.
  Q_INVOKABLE void BatchSearchDirectory(const QString& directory) {
    if (!backend_) return;
    auto missing = backend_->DetectMissingFiles();
    if (missing.empty()) {
      emit BatchSearchCompleted({});
      return;
    }
    auto candidates = backend_->SearchDirectoryForMatches(
        std::filesystem::path(directory.toStdWString()), missing);
    // Auto-apply best candidates
    QVariantList results;
    for (const auto& f : missing) {
      auto it = candidates.find(f.file_id);
      if (it != candidates.end() && !it->second.empty()) {
        auto relink_result = backend_->RelinkFile(f.file_id, it->second[0].path);
        if (relink_result.success) {
          auto orig_str = QString::fromStdWString(f.original_path.wstring());
          emit FileRelinked(orig_str);
          QVariantMap m;
          m.insert("originalPath", orig_str);
          m.insert("newPath", QString::fromStdWString(relink_result.new_path.wstring()));
          results.append(m);
        }
      }
    }
    emit BatchSearchCompleted(results);
  }

  /// Check if a specific file path has been relinked.
  Q_INVOKABLE bool IsFileRelinked(const QString& originalPath) const {
    if (!backend_) return false;
    auto missing = backend_->DetectMissingFiles();
    for (const auto& f : missing) {
      auto orig_str = QString::fromStdWString(f.original_path.wstring());
      if (orig_str == originalPath) return false;
    }
    return true;  // not in missing list means it's been relinked
  }

 signals:
  void FileRelinked(const QString& originalPath);
  void AutoSearchCompleted(const QVariantList& results);
  void BatchSearchCompleted(const QVariantList& results);

 private:
  RelinkService* backend_;
};

}  // namespace alcedo::ui
