//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QAbstractListModel>
#include <QHash>
#include <QString>
#include <QVariant>
#include <QVariantMap>
#include <unordered_map>
#include <vector>

#include "type/type.hpp"
#include "ui/alcedo_main/album_backend/album_types.hpp"

namespace alcedo::ui {

class AlbumThumbnailModel : public QAbstractListModel {
  Q_OBJECT
  Q_PROPERTY(int count READ count NOTIFY countChanged)
  Q_PROPERTY(bool hasMore READ hasMore NOTIFY hasMoreChanged)
  Q_PROPERTY(bool loading READ loading NOTIFY loadingChanged)

 public:
  enum Roles {
    ElementId = Qt::UserRole + 1,
    FileId,
    ImageId,
    FolderId,
    ScopeType,
    FileName,
    CameraModel,
    Extension,
    Iso,
    Aperture,
    FocalLength,
    CaptureDate,
    ImportDate,
    Rating,
    Tags,
    Accent,
    ThumbUrl,
    ThumbLoading,
    ThumbMissingSource,
    ThumbErrorText,
  };

  explicit AlbumThumbnailModel(QObject* parent = nullptr);

  int rowCount(const QModelIndex& parent = QModelIndex()) const override;
  QVariant data(const QModelIndex& index, int role = Qt::DisplayRole) const override;
  QHash<int, QByteArray> roleNames() const override;

  int count() const { return static_cast<int>(rows_.size()); }
  bool hasMore() const { return has_more_; }
  bool loading() const { return loading_; }

  void setHasMore(bool v);
  void setLoading(bool v);

  /// Full reset — emits modelReset. Use for folder/filter/language changes.
  void resetModel(const std::vector<AlbumItem>& items, size_t totalCount);

  /// Append one page — emits beginInsertRows/endInsertRows.
  void appendPage(const std::vector<AlbumItem>& newItems);

  /// Patch thumbnail state for a single row — emits dataChanged.
  void updateThumbnailState(sl_element_id_t elementId, const QString& dataUrl,
                            bool loading, bool missingSource, const QString& errorText);

  /// QML helper: return a QVariantMap for the row at @p index, or empty map.
  Q_INVOKABLE QVariantMap getItemAt(int index) const;

  /// Lookup row index by elementId. Returns -1 if not found.
  int rowByElementId(sl_element_id_t elementId) const;

  /// Raw access for AlbumBackend friend classes.
  const std::vector<AlbumItem>& items() const { return rows_; }
  size_t totalCount() const { return total_count_; }

 signals:
  void countChanged();
  void hasMoreChanged();
  void loadingChanged();
  void thumbnailUpdated(uint elementId, const QString& dataUrl, bool loading,
                        bool missingSource, const QString& errorText);

 private:
  QVariant roleValue(const AlbumItem& item, int role) const;
  void rebuildElementIdIndex();

  std::vector<AlbumItem> rows_{};
  std::unordered_map<sl_element_id_t, int> element_id_to_row_{};
  size_t total_count_ = 0;
  bool has_more_ = false;
  bool loading_ = false;
};

}  // namespace alcedo::ui
