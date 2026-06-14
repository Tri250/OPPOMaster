//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QObject>
#include <QString>
#include <memory>
#include <string>
#include <vector>

#include "app/semantic_generation_service.hpp"
#include "ui/alcedo_main/i18n.hpp"

namespace alcedo::ui {

class AlbumBackend;
class SemanticRuntimeSessionGuard;

class SemanticGenerationController final : public QObject {
  Q_OBJECT
  Q_PROPERTY(bool promptVisible READ PromptVisible NOTIFY StateChanged)
  Q_PROPERTY(bool running READ Running NOTIFY StateChanged)
  Q_PROPERTY(int pendingCount READ PendingCount NOTIFY StateChanged)
  Q_PROPERTY(int total READ Total NOTIFY StateChanged)
  Q_PROPERTY(int embedded READ Embedded NOTIFY StateChanged)
  Q_PROPERTY(int skipped READ Skipped NOTIFY StateChanged)
  Q_PROPERTY(int failed READ Failed NOTIFY StateChanged)
  Q_PROPERTY(int canceled READ Canceled NOTIFY StateChanged)
  Q_PROPERTY(QString statusText READ StatusText NOTIFY StateChanged)
  Q_PROPERTY(int albumTotalCount READ AlbumTotalCount NOTIFY StateChanged)
  Q_PROPERTY(int albumLabeledCount READ AlbumLabeledCount NOTIFY StateChanged)
  Q_PROPERTY(int albumUnlabeledCount READ AlbumUnlabeledCount NOTIFY StateChanged)
  Q_PROPERTY(QString albumSummaryText READ AlbumSummaryText NOTIFY StateChanged)
  Q_PROPERTY(QString importPreference READ ImportPreference NOTIFY StateChanged)

 public:
  explicit SemanticGenerationController(AlbumBackend& backend, QObject* parent = nullptr);

  bool    PromptVisible() const;
  bool    Running() const { return running_; }
  int     PendingCount() const { return static_cast<int>(pending_items_.size()); }
  int     Total() const { return total_; }
  int     Embedded() const { return embedded_; }
  int     Skipped() const { return skipped_; }
  int     Failed() const { return failed_; }
  int     Canceled() const { return canceled_; }
  QString StatusText() const { return status_text_.Render(); }
  int     AlbumTotalCount() const { return album_total_count_; }
  int     AlbumLabeledCount() const { return album_labeled_count_; }
  int     AlbumUnlabeledCount() const { return album_unlabeled_count_; }
  QString AlbumSummaryText() const { return album_summary_text_.Render(); }
  QString ImportPreference() const;

  Q_INVOKABLE void StartPendingGeneration(bool forceRegenerate = false);
  Q_INVOKABLE void SkipPendingGeneration(bool rememberChoice = false);
  Q_INVOKABLE void SetImportPreference(const QString& preference);
  Q_INVOKABLE void CancelGeneration();
  Q_INVOKABLE void RefreshAlbumSummary();
  Q_INVOKABLE void StartAlbumGeneration(bool forceRegenerate = false);

  void QueuePrompt(std::vector<SemanticGenerationItem> items);
  void ResumeQueuedWorkflow();

  [[nodiscard]] auto ActiveModelKey() const -> std::string;
  [[nodiscard]] auto LabelDisplayText(sl_element_id_t elementId) const -> QString;

 signals:
  void StateChanged();

 private:
  [[nodiscard]] auto StoredModelKey() const -> std::string;
  void StartGenerationForItems(std::vector<SemanticGenerationItem> items, bool forceRegenerate);
  void ContinueGenerationForItems(bool forceRegenerate);
  void UpdateProgress(const SemanticGenerationProgress& progress);
  void Finish(std::vector<SemanticGenerationItemResult> results);
  void ClearPrompt();
  void ResetCounters();

  AlbumBackend& backend_;
  std::vector<SemanticGenerationItem> pending_items_{};
  std::shared_ptr<SemanticRuntimeSessionGuard> runtime_session_{};
  std::shared_ptr<SemanticGenerationJob>       job_{};
  i18n::LocalizedText status_text_{};
  i18n::LocalizedText album_summary_text_{};
  std::string         model_key_{};
  bool                prompt_pending_       = false;
  bool                running_              = false;
  int                 total_                = 0;
  int                 embedded_             = 0;
  int                 skipped_              = 0;
  int                 failed_               = 0;
  int                 canceled_             = 0;
  int                 album_total_count_     = 0;
  int                 album_labeled_count_   = 0;
  int                 album_unlabeled_count_ = 0;
};

}  // namespace alcedo::ui
