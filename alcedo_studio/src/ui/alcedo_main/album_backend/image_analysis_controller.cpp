//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ui/alcedo_main/album_backend/image_analysis_controller.hpp"

#include <QLocale>
#include <QMetaObject>
#include <QPointer>
#include <QSettings>
#include <QVariantMap>
#include <algorithm>
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

void ClearSecret(std::string* secret) {
  if (secret == nullptr) {
    return;
  }
  if (!secret->empty()) {
    std::fill(secret->begin(), secret->end(), '0');
  }
  secret->clear();
  secret->shrink_to_fit();
}

// Resolve the preset's output_language to a concrete code the sidecar accepts.
// "follow" (the default) resolves to the current app language — reading
// QSettings("ui/language") and resolving "system" via QLocale, the same pattern
// as semantic_generation_controller.cpp's CurrentUiSemanticLabelLanguage. The
// sidecar only understands "" / "en" / "zh", so "follow" never crosses the
// wire. Returns "" (English default) for any unknown value.
QString ResolveOutputLanguage(const QString& preference) {
  const QString v = preference.trimmed().toLower();
  if (v == "en") {
    return QStringLiteral("en");
  }
  if (v == "zh") {
    return QStringLiteral("zh");
  }
  // "follow" (or anything else) -> current app language.
  QString code = QSettings().value(QStringLiteral("ui/language"), QStringLiteral("system")).toString();
  if (code.compare(QStringLiteral("system"), Qt::CaseInsensitive) == 0) {
    code = QLocale::system().bcp47Name();
  }
  return code.startsWith(QStringLiteral("zh"), Qt::CaseInsensitive) ? QStringLiteral("zh")
                                                                    : QStringLiteral("en");
}

// Build a display mask for a raw secret: "sk-••••1234" style — never the raw
// key. Used for `masked_key_label` so QML can show "a key is saved" without the
// secret itself ever being persisted or logged.
QString MaskedKeyLabel(const std::string& secret) {
  if (secret.empty()) {
    return QString{};
  }
  const auto n = secret.size();
  // Show up to the last 4 chars, masked prefix. Cap the visible tail so a long
  // key does not leak much.
  const size_t tail = std::min<size_t>(n, 4);
  QString masked;
  masked.reserve(16);
  masked += QStringLiteral("••••");
  if (tail > 0) {
    masked += QString::fromUtf8(secret.data() + (n - tail), static_cast<int>(tail));
  }
  return masked;
}

}  // namespace

// ────────────────────────────────────────────────────────────────────────────
// ImageAnalysisController
// ────────────────────────────────────────────────────────────────────────────
ImageAnalysisController::ImageAnalysisController(std::shared_ptr<IImageAnalysisEnvironment> env,
                                                 AiProviderPresetController*                 preset,
                                                 std::shared_ptr<IImageAnalysisSink>         sink,
                                                 QObject*                                    parent)
    : QObject(parent), env_(std::move(env)), preset_(preset), sink_(std::move(sink)) {
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
  // Re-check the credential store without reading the secret or starting the sidecar.
  if (!preset.credential_slot.isEmpty()) {
    auto store = env_ ? env_->CredentialStore() : nullptr;
    credential_available_ = store && store->HasCredential(preset.credential_slot.toStdString());
  } else {
    credential_available_ = false;
  }
  if (was_provider_configured != provider_configured_ ||
      was_credential_available != credential_available_) {
    emit StateChanged();
  }
}

void ImageAnalysisController::RefreshCredentialState() { RefreshConfiguredState(); }

QString ImageAnalysisController::SaveApiKey(const QString& secret) {
  if (!preset_ || !env_) {
    return Tr("Image analysis runtime is unavailable.");
  }
  const auto preset = preset_->CurrentPreset();
  if (preset.credential_slot.isEmpty()) {
    return Tr("Configure a credential slot before saving a key.");
  }
  auto store = env_->CredentialStore();
  if (!store) {
    return Tr("Image analysis runtime is unavailable.");
  }
  // Copy the secret into a std::string only long enough to hand it to the OS
  // credential store, then wipe it. It is never logged, never written to
  // QSettings, and never stored on the preset DTO — only a display mask is.
  std::string raw = secret.toStdString();
  std::string err;
  const bool ok = store->SaveCredential(preset.credential_slot.toStdString(), raw, &err);
  // Build the mask from the raw secret before wiping it.
  QString masked = MaskedKeyLabel(raw);
  ClearSecret(&raw);
  if (!ok) {
    return err.empty() ? Tr("Could not save the API key to the system credential store.")
                       : QString::fromStdString(err);
  }
  if (preset_) {
    preset_->SetMaskedKeyLabel(masked);
    // SetMaskedKeyLabel emits PresetChanged -> RefreshConfiguredState runs, but
    // call it explicitly so credentialAvailable reflects the new key promptly.
  }
  RefreshCredentialState();
  return {};
}

void ImageAnalysisController::DeleteApiKey() {
  if (!preset_ || !env_) {
    return;
  }
  const auto preset = preset_->CurrentPreset();
  if (preset.credential_slot.isEmpty()) {
    return;
  }
  auto store = env_->CredentialStore();
  if (!store) {
    return;
  }
  std::string err;
  store->DeleteCredential(preset.credential_slot.toStdString(), &err);  // idempotent
  if (preset_) {
    preset_->SetMaskedKeyLabel(QString{});
  }
  RefreshCredentialState();
}

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
  job_.reset();
  last_items_.clear();
  last_results_.clear();
  last_usage_.clear();
  ResetCounters();
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
    ClearSecret(&secret);
    SetError(Tr("Could not start the AI sidecar: %1").arg(QString::fromStdString(sidecar_err)));
    return;
  }

  auto thumbnail_provider = env_->ThumbnailProvider();
  auto analysis_client    = env_->AnalysisClient();
  auto gate               = env_->Gate();
  if (!thumbnail_provider || !analysis_client || !gate) {
    ClearSecret(&secret);
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
  options.output_language      = ResolveOutputLanguage(preset.output_language).toStdString();
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
      std::move(items), std::move(options),
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

  auto store = env_->CredentialStore();
  if (!store) {
    SetError(Tr("Image analysis runtime is unavailable."));
    return;
  }
  std::string slot = preset.credential_slot.toStdString();
  if (!store->HasCredential(slot)) {
    credential_available_ = false;
    SetError(Tr("No API key stored for credential slot '%1'. Save a key first.").arg(
        preset.credential_slot));
    return;
  }
  credential_available_ = true;

  std::string sidecar_err;
  if (!env_->EnsureSidecarReady(&sidecar_err)) {
    SetError(Tr("Could not start the AI sidecar: %1").arg(QString::fromStdString(sidecar_err)));
    return;
  }
  auto thumbnail_provider = env_->ThumbnailProvider();
  auto analysis_client    = env_->AnalysisClient();
  auto gate               = env_->Gate();
  if (!thumbnail_provider || !analysis_client || !gate) {
    SetError(Tr("Image analysis runtime is unavailable."));
    return;
  }
  // Run the dry-run off the QML thread so the UI stays responsive.
  std::string provider_id = preset.provider_id.toStdString();
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
            // Surface the discovered candidates to QML so the settings page can
            // populate the model ComboBox. The sidecar committed them during
            // ListModels, so each is immediately usable as an explicit model_id.
            QVariantList models;
            models.reserve(static_cast<int>(result.models.size()));
            for (const auto& m : result.models) {
              QVariantMap entry;
              entry.insert(QStringLiteral("modelId"), QString::fromStdString(m.model_id));
              entry.insert(QStringLiteral("displayName"), QString::fromStdString(m.display_name));
              entry.insert(QStringLiteral("sourceProviderId"),
                           QString::fromStdString(m.source_provider_id));
              models.append(entry);
            }
            self->discovered_models_ = std::move(models);
            const int n = static_cast<int>(result.models.size());
            self->status_text_       = PL_TEXT("Connection OK — %1 model(s) visible.", n);
            self->connection_status_ = Tr("Connected — %1 model(s) available.").arg(n);
          } else {
            self->last_error_ = QString::fromStdString(result.error);
            self->status_text_ = i18n::LocalizedText{};
            self->connection_status_ =
                Tr("Connection failed: %1").arg(QString::fromStdString(result.error));
            self->discovered_models_.clear();
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
  // Phase 7a usage aggregate: token totals across all items + the distinct provider
  // request ids, plus how many items carried usage metadata vs not. Per-item usage is
  // NOT placed in `lastResults` — this aggregate is the 7a summary.
  int64_t             usage_input     = 0;
  int64_t             usage_output    = 0;
  int64_t             usage_total     = 0;
  QVariantList        usage_request_ids;
  int                 items_with_usage    = 0;
  int                 items_without_usage = 0;
  const bool          describe = (last_task_ == alcedo::ImageAnalysisTask::kDescribe);

  for (const auto& r : results) {
    QVariantMap m;
    m.insert("elementId", static_cast<uint>(r.item.element_id));
    m.insert("imageId", static_cast<uint>(r.item.image_id));
    m.insert("status", QString::fromUtf8(alcedo::ToString(r.status)));
    m.insert("error", QString::fromStdString(r.error));
    m.insert("provider", QString::fromStdString(describe ? r.understanding.provider
                                                          : r.rating.provider));
    m.insert("modelId", QString::fromStdString(describe ? r.understanding.model_id
                                                         : r.rating.model_id));
    // Identity on every job result so a prompt/model change does not reinterpret old
    // annotations: prompt-profile id + the provider's own request id (provider/modelId
    // already present above).
    m.insert("promptProfileId",
             QString::fromStdString(describe ? r.understanding.prompt_profile_id
                                             : r.rating.prompt_profile_id));
    m.insert("providerRequestId",
             QString::fromStdString(describe ? r.understanding.provider_request_id
                                             : r.rating.provider_request_id));

    const auto& usage = describe ? r.understanding.usage : r.rating.usage;
    const auto& provider_request_id =
        describe ? r.understanding.provider_request_id : r.rating.provider_request_id;
    const bool item_has_usage = (usage.total_tokens != 0 || !provider_request_id.empty());
    if (item_has_usage) {
      ++items_with_usage;
      usage_input += usage.input_tokens;
      usage_output += usage.output_tokens;
      usage_total += usage.total_tokens;
      if (!provider_request_id.empty()) {
        usage_request_ids.push_back(QString::fromStdString(provider_request_id));
      }
    } else {
      ++items_without_usage;
    }

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

  // Build the usage aggregate (always present, even on an all-failure job, so QML can
  // read itemsWithUsage=0).
  QVariantMap usage_map;
  usage_map.insert("inputTokens", static_cast<qlonglong>(usage_input));
  usage_map.insert("outputTokens", static_cast<qlonglong>(usage_output));
  usage_map.insert("totalTokens", static_cast<qlonglong>(usage_total));
  usage_map.insert("providerRequestIds", usage_request_ids);
  usage_map.insert("itemsWithUsage", items_with_usage);
  usage_map.insert("itemsWithoutUsage", items_without_usage);
  last_usage_ = usage_map;

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

  // Phase 7a persistence (job end, in the finished callback). A cancelled or failed run
  // must leave no active annotation: only `kAnalyzed` items reach the sink, and the
  // trailing flush/notify fires only when at least one item was persisted (analyzed_ > 0),
  // so a fully failed/cancelled job produces ZERO sink calls. The sink is nullable so the
  // controller stays usable in contexts without host-state wiring (e.g. a dry unit test).
  if (sink_ && analyzed_ > 0) {
    for (const auto& r : results) {
      if (r.status != alcedo::ImageAnalysisItemStatus::kAnalyzed) {
        continue;
      }
      if (describe) {
        sink_->PersistUnderstanding(r);
      } else {
        sink_->PersistRatingReasons(r);
        sink_->ApplyStarRating(static_cast<uint32_t>(r.item.element_id),
                               static_cast<uint32_t>(r.item.image_id), r.rating.rating);
      }
    }
    if (describe) {
      sink_->NotifySearchDocumentChanged();
    } else {
      sink_->FlushPendingStarRatings();
    }
  }

  RefreshConfiguredState();
  emit StateChanged();
}

#undef PL_TEXT

}  // namespace alcedo::ui
