//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ui/alcedo_main/album_backend/image_analysis_controller.hpp"

#include <QMetaObject>
#include <QPointer>
#include <QVariantMap>
#include <chrono>
#include <filesystem>
#include <unordered_set>
#include <utility>
#include <vector>

namespace alcedo::ui {

using namespace std::chrono_literals;

#define PL_TEXT(text, ...)                     \
  i18n::MakeLocalizedText(ALCEDO_I18N_CONTEXT, \
                          QT_TRANSLATE_NOOP(ALCEDO_I18N_CONTEXT, text) __VA_OPT__(, ) __VA_ARGS__)

namespace {

// Credential handle TTL for a remote image-analysis job (matches the 6c default).
constexpr int64_t kCredentialTtlMs = 60000;

}  // namespace

// ────────────────────────────────────────────────────────────────────────────
// ImageAnalysisController
// ────────────────────────────────────────────────────────────────────────────
ImageAnalysisController::ImageAnalysisController(std::shared_ptr<IImageAnalysisEnvironment> env,
                                                 AiProviderPresetController*                 preset,
                                                 QObject*                                    parent)
    : QObject(parent), env_(std::move(env)), preset_(preset) {
  if (preset_) {
    connect(preset_, &AiProviderPresetController::PresetChanged, this,
            [this] { RefreshConfiguredState(); });
  }
  RefreshConfiguredState();
}

void ImageAnalysisController::RefreshConfiguredState() {
  if (!preset_) {
    provider_configured_  = false;
    credential_available_ = false;
    emit StateChanged();
    return;
  }
  const auto preset = preset_->CurrentPreset();
  const bool was_provider_configured  = provider_configured_;
  const bool was_credential_available = credential_available_;
  provider_configured_ = !preset.provider_id.isEmpty() && !preset.model_id.isEmpty();
  // Re-check the credential store without starting the sidecar.
  if (provider_configured_ && !preset.credential_slot.isEmpty()) {
    std::string secret;
    std::string err;
    auto        store = env_ ? env_->CredentialStore() : nullptr;
    credential_available_ = store && store->LoadCredential(preset.credential_slot.toStdString(), &secret, &err);
  } else {
    credential_available_ = false;
  }
  if (was_provider_configured != provider_configured_ ||
      was_credential_available != credential_available_) {
    emit StateChanged();
  }
}

void ImageAnalysisController::RefreshCredentialState() { RefreshConfiguredState(); }

auto ImageAnalysisController::CollectItems(const QVariantList& targetEntries)
    -> std::vector<alcedo::ImageAnalysisItem> {
  std::vector<alcedo::ImageAnalysisItem> items;
  std::unordered_set<uint64_t>           seen;
  for (const QVariant& entry : targetEntries) {
    const auto map       = entry.toMap();
    const auto elementId = static_cast<sl_element_id_t>(map.value("elementId").toUInt());
    const auto imageId   = static_cast<image_id_t>(map.value("imageId").toUInt());
    if (elementId == 0 || imageId == 0) {
      continue;
    }
    const uint64_t key = (static_cast<uint64_t>(elementId) << 32) | static_cast<uint64_t>(imageId);
    if (!seen.insert(key).second) {
      continue;
    }
    items.push_back(alcedo::ImageAnalysisItem{elementId, imageId});
  }
  return items;
}

void ImageAnalysisController::ResetCounters() {
  total_    = 0;
  analyzed_ = 0;
  failed_   = 0;
  canceled_ = 0;
}

void ImageAnalysisController::SetError(const QString& error) {
  last_error_ = error;
  status_text_ = i18n::LocalizedText{};
  running_     = false;
  can_retry_   = false;
  emit StateChanged();
}

void ImageAnalysisController::StartDescribeForTargets(const QVariantList& targetEntries) {
  StartForTargets(targetEntries, alcedo::ImageAnalysisTask::kDescribe);
}

void ImageAnalysisController::StartScoreForTargets(const QVariantList& targetEntries) {
  StartForTargets(targetEntries, alcedo::ImageAnalysisTask::kScore);
}

void ImageAnalysisController::StartForTargets(const QVariantList&        targetEntries,
                                              alcedo::ImageAnalysisTask task) {
  if (running_) {
    return;
  }
  last_error_.clear();
  last_task_ = task;

  auto items = CollectItems(targetEntries);
  if (items.empty()) {
    SetError(Tr("Select at least one image to analyze."));
    return;
  }

  if (!preset_) {
    SetError(Tr("No provider preset is configured."));
    return;
  }
  const auto preset = preset_->CurrentPreset();
  if (preset.provider_id.isEmpty() || preset.model_id.isEmpty()) {
    provider_configured_ = false;
    SetError(Tr("Configure a provider and model in the AI preset before analyzing images."));
    return;
  }
  provider_configured_ = true;

  // Load the secret from the OS credential store (never from QSettings). If the
  // slot is missing, fail before starting the sidecar.
  std::string secret;
  std::string cred_err;
  auto        store = env_ ? env_->CredentialStore() : nullptr;
  if (!store || !store->LoadCredential(preset.credential_slot.toStdString(), &secret, &cred_err)) {
    credential_available_ = false;
    SetError(Tr("No API key stored for credential slot '%1'. Save a key first.").arg(
        preset.credential_slot));
    return;
  }
  credential_available_ = true;

  // Start the sidecar on demand (require_model_info=false).
  std::string sidecar_err;
  if (!env_->EnsureSidecarReady(&sidecar_err)) {
    SetError(Tr("Could not start the AI sidecar: %1").arg(QString::fromStdString(sidecar_err)));
    return;
  }

  auto thumbnail_provider = env_->ThumbnailProvider();
  auto analysis_client    = env_->AnalysisClient();
  auto gate               = env_->Gate();
  if (!thumbnail_provider || !analysis_client || !gate) {
    SetError(Tr("Image analysis runtime is unavailable. Open a project first."));
    return;
  }

  alcedo::ImageAnalysisOptions options;
  options.task                 = task;
  options.thumbnail_resolution = alcedo::ThumbnailResolution::k1024;
  options.jpeg_quality         = 90;
  options.timeout              = std::chrono::milliseconds(preset.timeout_ms);
  options.provider_id          = preset.provider_id.toStdString();
  options.model_id             = preset.model_id.toStdString();
  options.credential.provider_id = preset.provider_id.toStdString();
  options.credential.secret     = std::move(secret);
  options.credential_ttl_ms     = kCredentialTtlMs;
  options.temp_dir              = std::filesystem::temp_directory_path();
  options.prefetch             = 1;
  options.max_image_bytes      = preset.max_image_bytes;
  if (task == alcedo::ImageAnalysisTask::kScore) {
    options.rubric_id = "general";
  }

  ResetCounters();
  total_      = static_cast<int>(items.size());
  running_    = true;
  can_retry_  = false;
  last_items_ = items;
  status_text_ =
      task == alcedo::ImageAnalysisTask::kDescribe
          ? PL_TEXT("Analyzing %1 image(s) for captions and tags...", total_)
          : PL_TEXT("Scoring %1 image(s)...", total_);
  emit StateChanged();

  // Build a fresh service per job, passing the SHARED gate so remote calls
  // serialize app-wide across every controller/service instance.
  alcedo::ImageAnalysisService service(thumbnail_provider, analysis_client, gate);

  QPointer<ImageAnalysisController> self(this);
  auto job = service.StartAnalysis(
      std::move(items), options,
      [self](const alcedo::ImageAnalysisProgress& progress) {
        if (!self) {
          return;
        }
        QMetaObject::invokeMethod(
            self,
            [self, progress]() {
              if (self) {
                self->UpdateProgress(progress);
              }
            },
            Qt::QueuedConnection);
      },
      [self](std::vector<alcedo::ImageAnalysisItemResult> results) {
        if (!self) {
          return;
        }
        QMetaObject::invokeMethod(
            self,
            [self, results = std::move(results)]() mutable {
              if (self) {
                self->Finish(std::move(results));
              }
            },
            Qt::QueuedConnection);
      });
  job_ = std::move(job);
}

void ImageAnalysisController::CancelAnalysis() {
  if (!running_ || !job_) {
    return;
  }
  status_text_ = PL_TEXT("Cancelling...");
  emit StateChanged();
  job_->Cancel();
}

void ImageAnalysisController::RetryLast() {
  if (running_ || last_items_.empty()) {
    return;
  }
  auto items = last_items_;
  auto task  = last_task_;
  // Re-run via StartForTargets by reconstructing the target entries from items.
  QVariantList entries;
  for (const auto& it : items) {
    QVariantMap m;
    m.insert("elementId", static_cast<uint>(it.element_id));
    m.insert("imageId", static_cast<uint>(it.image_id));
    entries.push_back(m);
  }
  StartForTargets(entries, task);
}

void ImageAnalysisController::ValidateConnection() {
  if (running_ || !preset_ || !env_) {
    return;
  }
  const auto preset = preset_->CurrentPreset();
  if (preset.provider_id.isEmpty() || preset.credential_slot.isEmpty()) {
    SetError(Tr("Configure a provider id and credential slot before validating."));
    return;
  }

  std::string sidecar_err;
  if (!env_->EnsureSidecarReady(&sidecar_err)) {
    SetError(Tr("Could not start the AI sidecar: %1").arg(QString::fromStdString(sidecar_err)));
    return;
  }
  auto thumbnail_provider = env_->ThumbnailProvider();
  auto analysis_client    = env_->AnalysisClient();
  auto gate               = env_->Gate();
  auto store              = env_->CredentialStore();
  if (!thumbnail_provider || !analysis_client || !gate || !store) {
    SetError(Tr("Image analysis runtime is unavailable."));
    return;
  }
  // Run the dry-run off the QML thread so the UI stays responsive.
  std::string provider_id = preset.provider_id.toStdString();
  std::string slot        = preset.credential_slot.toStdString();
  int64_t     timeout_ms  = preset.timeout_ms;
  QPointer<ImageAnalysisController> self(this);
  std::thread([self, provider_id, slot, timeout_ms, thumbnail_provider, analysis_client, gate,
               store]() {
    alcedo::ImageAnalysisService                         service(thumbnail_provider, analysis_client, gate);
    alcedo::ImageAnalysisConnectionValidationOptions opts;
    opts.provider_id        = provider_id;
    opts.credential_slot    = slot;
    opts.timeout            = std::chrono::milliseconds(timeout_ms);
    opts.credential_ttl_ms  = kCredentialTtlMs;
    auto result = service.ValidateConnection(opts, *store);
    QMetaObject::invokeMethod(
        self,
        [self, result]() {
          if (!self) {
            return;
          }
          if (result.ok) {
            self->last_error_.clear();
            const int n = static_cast<int>(result.models.size());
            self->status_text_ = PL_TEXT("Connection OK — %1 model(s) visible.", n);
          } else {
            self->last_error_ = QString::fromStdString(result.error);
            self->status_text_ = i18n::LocalizedText{};
          }
          self->RefreshConfiguredState();
          emit self->StateChanged();
        },
        Qt::QueuedConnection);
  }).detach();
}

void ImageAnalysisController::UpdateProgress(const alcedo::ImageAnalysisProgress& progress) {
  total_    = static_cast<int>(progress.total);
  analyzed_ = static_cast<int>(progress.analyzed);
  failed_   = static_cast<int>(progress.failed);
  canceled_ = static_cast<int>(progress.canceled);
  const int completed = analyzed_ + failed_ + canceled_;
  const int pct       = total_ > 0 ? (completed * 100) / total_ : 0;
  status_text_        = (last_task_ == alcedo::ImageAnalysisTask::kDescribe)
                 ? PL_TEXT("Analyzing captions/tags: %1/%2 (%3%)", completed, total_, pct)
                 : PL_TEXT("Scoring: %1/%2 (%3%)", completed, total_, pct);
  emit StateChanged();
}

void ImageAnalysisController::Finish(std::vector<alcedo::ImageAnalysisItemResult> results) {
  running_ = false;
  ResetCounters();
  last_results_.clear();
  for (const auto& r : results) {
    QVariantMap m;
    m.insert("elementId", static_cast<uint>(r.item.element_id));
    m.insert("imageId", static_cast<uint>(r.item.image_id));
    m.insert("status", QString::fromUtf8(alcedo::ToString(r.status)));
    m.insert("error", QString::fromStdString(r.error));
    const bool describe = (last_task_ == alcedo::ImageAnalysisTask::kDescribe);
    m.insert("provider", QString::fromStdString(describe ? r.understanding.provider
                                                          : r.rating.provider));
    m.insert("modelId", QString::fromStdString(describe ? r.understanding.model_id
                                                         : r.rating.model_id));
    if (r.status == alcedo::ImageAnalysisItemStatus::kAnalyzed) {
      if (describe) {
        m.insert("caption", QString::fromStdString(r.understanding.caption));
        QVariantList tags;
        for (const auto& t : r.understanding.tags) {
          tags.push_back(QString::fromStdString(t));
        }
        m.insert("tags", tags);
        m.insert("scene", QString::fromStdString(r.understanding.scene));
        analyzed_++;
      } else {
        m.insert("rating", r.rating.rating);
        m.insert("rubricId", QString::fromStdString(r.rating.rubric_id));
        m.insert("reasons", QString::fromStdString(r.rating.reasons));
        analyzed_++;
      }
    } else if (r.status == alcedo::ImageAnalysisItemStatus::kCanceled) {
      canceled_++;
    } else {
      failed_++;
    }
    last_results_.push_back(m);
  }
  total_ = static_cast<int>(results.size());

  const bool any_failure = (failed_ > 0 || canceled_ > 0);
  can_retry_             = any_failure && !last_items_.empty();
  if (analyzed_ == total_ && total_ > 0) {
    last_error_.clear();
    status_text_ = (last_task_ == alcedo::ImageAnalysisTask::kDescribe)
                       ? PL_TEXT("Analyzed %1 image(s).", analyzed_)
                       : PL_TEXT("Scored %1 image(s).", analyzed_);
  } else if (any_failure) {
    status_text_ = PL_TEXT("Done: %1 ok, %2 failed, %3 canceled.", analyzed_, failed_, canceled_);
  } else {
    status_text_ = i18n::LocalizedText{};
  }
  // A cancelled or failed run must leave no active annotation. The controller
  // performs NO persistence (Phase 6e), so this holds trivially; failed/canceled
  // items are counted here, never as analyzed.
  RefreshConfiguredState();
  emit StateChanged();
}

#undef PL_TEXT

}  // namespace alcedo::ui
