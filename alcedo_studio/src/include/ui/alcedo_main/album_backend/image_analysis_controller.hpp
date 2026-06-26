//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QObject>
#include <QPointer>
#include <QString>
#include <QVariantList>
#include <memory>
#include <string>
#include <vector>

#include "app/ai_credential_store.hpp"
#include "app/ai_provider_preset.hpp"
#include "app/image_analysis_service.hpp"
#include "ui/alcedo_main/i18n.hpp"

namespace alcedo::ui {

class AlbumBackend;

/// Phase 6d — the runtime seams `ImageAnalysisController` needs, as an interface
/// so the controller is unit-testable without a live project / sidecar.
///
/// The production implementation (`AlbumImageAnalysisEnvironment`, in the .cpp)
/// resolves these lazily from `AlbumBackend`'s open project at call time, exactly
/// as `SemanticGenerationController` does at
/// `semantic_generation_controller.cpp:762–830`. Tests pass a fake that returns
/// fake thumbnail/client/credential-store seams and a shared gate.
class IImageAnalysisEnvironment {
 public:
  virtual ~IImageAnalysisEnvironment() = default;
  virtual auto ThumbnailProvider() -> std::shared_ptr<IImageAnalysisThumbnailProvider> = 0;
  virtual auto AnalysisClient() -> std::shared_ptr<IImageAnalysisClient>            = 0;
  virtual auto CredentialStore() -> std::shared_ptr<IAiCredentialStore>              = 0;
  virtual auto Gate() -> std::shared_ptr<ImageAnalysisInFlightGate>                 = 0;
  /// Start the AI sidecar on demand with `require_model_info=false` (remote image
  /// analysis uses the HTTP-provider path; no CLIP model is needed). Returns true
  /// if the sidecar is ready. Does NOT check `model_info` (it is unpopulated when
  /// `require_model_info=false`).
  virtual auto EnsureSidecarReady(std::string* error) -> bool = 0;
};

/// Drives remote image analysis (caption/tags via `image_understanding.describe`,
/// rating via `image_rating.score`) from the album workflow.
///
/// Mirrors the cleanly-factored QObject sub-controller pattern
/// (`SemanticGenerationController`, `ModelDownloadController`): own hpp/cpp under
/// `album_backend/`, surfaced to QML as a `Q_PROPERTY(QObject* ... CONSTANT)` on
/// `AlbumBackend`. The controller is deliberately NOT inlined into
/// `album_backend.cpp` — it is a standalone module. It is constructable with an
/// `IImageAnalysisEnvironment` + `AiProviderPresetController*` so it has no direct
/// `AlbumBackend` dependency and is unit-testable with fakes.
///
/// The controller owns NO database writes (persistence + search refresh is Phase
/// 6e). A cancelled or failed remote call therefore cannot upsert an active
/// understanding/rating row — the controller only surfaces results in QML state,
/// and failed/canceled items are never counted as `analyzed`. The sidecar is
/// started on demand with `require_model_info=false` so ordinary album
/// browsing/search requires neither a running sidecar nor an API key. Remote calls
/// are serialized through one shared `ImageAnalysisInFlightGate` (Phase 6d mandate)
/// passed via the environment, so jobs serialize app-wide, not per service instance.
class ImageAnalysisController final : public QObject {
  Q_OBJECT
  Q_PROPERTY(bool running READ Running NOTIFY StateChanged)
  Q_PROPERTY(int total READ Total NOTIFY StateChanged)
  Q_PROPERTY(int analyzed READ Analyzed NOTIFY StateChanged)
  Q_PROPERTY(int failed READ Failed NOTIFY StateChanged)
  Q_PROPERTY(int canceled READ Canceled NOTIFY StateChanged)
  Q_PROPERTY(QString statusText READ StatusText NOTIFY StateChanged)
  Q_PROPERTY(QString lastError READ LastError NOTIFY StateChanged)
  Q_PROPERTY(bool canRetry READ CanRetry NOTIFY StateChanged)
  Q_PROPERTY(bool providerConfigured READ ProviderConfigured NOTIFY StateChanged)
  Q_PROPERTY(bool credentialAvailable READ CredentialAvailable NOTIFY StateChanged)
  Q_PROPERTY(QVariantList lastResults READ LastResults NOTIFY StateChanged)

 public:
  ImageAnalysisController(std::shared_ptr<IImageAnalysisEnvironment> env,
                          AiProviderPresetController*                 preset,
                          QObject*                                    parent = nullptr);

  bool             Running() const { return running_; }
  int              Total() const { return total_; }
  int              Analyzed() const { return analyzed_; }
  int              Failed() const { return failed_; }
  int              Canceled() const { return canceled_; }
  QString          StatusText() const { return status_text_.Render(); }
  QString          LastError() const { return last_error_; }
  bool             CanRetry() const { return can_retry_; }
  bool             ProviderConfigured() const { return provider_configured_; }
  bool             CredentialAvailable() const { return credential_available_; }
  QVariantList     LastResults() const { return last_results_; }

  // Album selection is a QVariantList of {elementId, imageId} maps (the same
  // convention as ImportExportHandler::CollectExportTargets). Empty selection is
  // a no-op with a clear error — image analysis is a paid remote call, so it must
  // never silently fall back to "whole view".
  Q_INVOKABLE void StartDescribeForTargets(const QVariantList& targetEntries);
  Q_INVOKABLE void StartScoreForTargets(const QVariantList& targetEntries);
  Q_INVOKABLE void CancelAnalysis();
  Q_INVOKABLE void RetryLast();
  // Dry-run model discovery against the selected preset (reuses the Phase 6c
  // ValidateConnection path). Surfaces ok/error in lastError.
  Q_INVOKABLE void ValidateConnection();
  Q_INVOKABLE void RefreshCredentialState();

 signals:
  void StateChanged();

 private:
  void StartForTargets(const QVariantList& targetEntries, alcedo::ImageAnalysisTask task);
  auto CollectItems(const QVariantList& targetEntries) -> std::vector<alcedo::ImageAnalysisItem>;
  void RefreshConfiguredState();
  void UpdateProgress(const alcedo::ImageAnalysisProgress& progress);
  void Finish(std::vector<alcedo::ImageAnalysisItemResult> results);
  void SetError(const QString& error);
  void ResetCounters();

  std::shared_ptr<IImageAnalysisEnvironment> env_;
  AiProviderPresetController*                 preset_;
  std::shared_ptr<alcedo::ImageAnalysisJob>   job_;
  std::vector<alcedo::ImageAnalysisItem>      last_items_;
  alcedo::ImageAnalysisTask                    last_task_ = alcedo::ImageAnalysisTask::kDescribe;

  i18n::LocalizedText status_text_{};
  QString             last_error_;
  QVariantList        last_results_;
  bool                running_              = false;
  bool                can_retry_            = false;
  bool                provider_configured_  = false;
  bool                credential_available_ = false;
  int                 total_                = 0;
  int                 analyzed_             = 0;
  int                 failed_               = 0;
  int                 canceled_             = 0;
};

/// Factory for the production environment. Defined in the .cpp (the concrete
/// `AlbumImageAnalysisEnvironment` is an implementation detail); `AlbumBackend`
/// calls this from its member-init list to construct `ImageAnalysisController`.
std::shared_ptr<IImageAnalysisEnvironment> MakeAlbumImageAnalysisEnvironment(AlbumBackend& backend);

}  // namespace alcedo::ui
