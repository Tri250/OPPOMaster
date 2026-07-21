//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QObject>
#include <QString>
#include <QVariantList>
#include <QVariantMap>
#include <memory>

#include "app/panorama_service.hpp"
#include "ui/alcedo_main/i18n.hpp"

namespace alcedo::ui {

class AlbumBackend;

// Bridges the C++ PanoramaService to QML. Owns the stitching pipeline state
// (progress, stage, result) and exposes Q_INVOKABLE methods for starting and
// cancelling panorama operations.
class PanoramaController final : public QObject {
  Q_OBJECT
  Q_PROPERTY(bool running READ Running NOTIFY StateChanged)
  Q_PROPERTY(float progress READ Progress NOTIFY ProgressChanged)
  Q_PROPERTY(QString stageText READ StageText NOTIFY ProgressChanged)
  Q_PROPERTY(bool hasResult READ HasResult NOTIFY ResultChanged)
  Q_PROPERTY(QVariantMap result READ Result NOTIFY ResultChanged)
  Q_PROPERTY(bool failed READ Failed NOTIFY ResultChanged)
  Q_PROPERTY(QString errorMessage READ ErrorMessage NOTIFY ResultChanged)

 public:
  explicit PanoramaController(AlbumBackend& backend, QObject* parent = nullptr);

  bool        Running() const { return running_; }
  float       Progress() const { return progress_; }
  QString     StageText() const { return stage_text_; }
  bool        HasResult() const { return has_result_; }
  QVariantMap Result() const { return result_; }
  bool        Failed() const { return failed_; }
  QString     ErrorMessage() const { return error_message_; }

  // Start a panorama stitch with the given image IDs and optional config.
  Q_INVOKABLE void stitchImages(const QVariantList& imageIds,
                                const QVariantMap& config = {});

  // Cancel any in-progress stitch operation.
  Q_INVOKABLE void cancel();

  // Dismiss the current result (clear hasResult / failed state).
  Q_INVOKABLE void dismissResult();

 signals:
  void StateChanged();
  void ProgressChanged();
  void ResultChanged();

 private:
  void HandleProgress(const alcedo::PanoramaProgress& progress);
  void HandleCompletion(alcedo::PanoramaResult result);
  static QString StageToString(alcedo::PanoramaProgress::Stage stage);

  AlbumBackend& backend_;
  std::unique_ptr<alcedo::PanoramaService> service_;

  bool        running_       = false;
  float       progress_      = 0.0f;
  QString     stage_text_;
  bool        has_result_    = false;
  QVariantMap result_;
  bool        failed_        = false;
  QString     error_message_;
};

}  // namespace alcedo::ui
