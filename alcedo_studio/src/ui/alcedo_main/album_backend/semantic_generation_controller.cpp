//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ui/alcedo_main/album_backend/semantic_generation_controller.hpp"

#include <QCoreApplication>
#include <QDir>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QMetaObject>
#include <QPointer>
#include <QSettings>
#include <QTimer>
#include <QUrl>
#include <algorithm>
#include <chrono>
#include <limits>
#include <string>
#include <utility>

#include "app/album_browse_service.hpp"
#include "storage/controller/semantic/semantic_label_config.hpp"
#include "ui/alcedo_main/album_backend/album_backend.hpp"

namespace alcedo::ui {

using namespace std::chrono_literals;

#define PL_TEXT(text, ...)                     \
  i18n::MakeLocalizedText(ALCEDO_I18N_CONTEXT, \
                          QT_TRANSLATE_NOOP(ALCEDO_I18N_CONTEXT, text) __VA_OPT__(, ) __VA_ARGS__)

namespace {

constexpr auto kSemanticGenerationImportPreferenceKey = "semantic/importGenerationPreference";
constexpr auto kSemanticModelProfileKey               = "semantic/modelProfileId";
constexpr auto kSemanticModelDirectoryKey             = "semantic/modelDirectory";
constexpr auto kSemanticEndpointPresetKey             = "semantic/modelEndpointPreset";
constexpr auto kSemanticCustomEndpointKey             = "semantic/customModelEndpoint";
constexpr auto kSemanticPreferenceAsk                 = "ask";
constexpr auto kSemanticPreferenceAlways              = "always";
constexpr auto kSemanticPreferenceNever               = "never";
constexpr auto kSemanticRuntimeStartupTimeout         = 60s;
constexpr auto kSemanticModelManagerTimeout           = 3s;
constexpr auto kSemanticModelDownloadPollIntervalMs   = 650;
constexpr auto kMobileClipProfileId                   = "mobileclip2-s2-en";
constexpr auto kMobileClipModelId                     = "plhery/mobileclip2-onnx:s2";
constexpr auto kMobileClipRevision                    = "ba95759a5bdbaca53e9111e2550a76ec09c8fd9e";
constexpr auto kChineseClipProfileId                  = "chinese-clip-vit-base-patch16-zh";
constexpr auto kChineseClipModelId  = "felixdu/chinese-clip-vit-base-patch16-onnx";
constexpr auto kChineseClipRevision = "47080d16c631d8416d2e6b155c59f8fd2c322e98";
constexpr auto kJinaClipProfileId   = "jina-clip-v2-int8-multilingual";
constexpr auto kJinaClipModelId     = "jinaai/jina-clip-v2";
constexpr auto kJinaClipRevision    = "e10d47f5691d0454a0fb5d13f46f2199b74cb436";

struct SemanticModelProfileUiInfo {
  const char* profile_id;
  const char* display_name;
  const char* model_id;
  const char* revision;
  const char* language;
  int         image_size;
  int         native_embedding_dim;
  bool        activatable;
};

constexpr SemanticModelProfileUiInfo kSemanticModelProfiles[] = {
    {kMobileClipProfileId, "MobileCLIP2 S2 English", kMobileClipModelId, kMobileClipRevision, "en",
     256, 512, true},
    {kChineseClipProfileId, "Chinese-CLIP ViT-B/16", kChineseClipModelId, kChineseClipRevision,
     "zh", 224, 512, false},
    {kJinaClipProfileId, "Jina CLIP v2 INT8 Multilingual", kJinaClipModelId, kJinaClipRevision,
     "multilingual", 512, 1024, false},
};

auto SemanticModelKeyFromInfo(const SemanticRuntimeModelInfo& info) -> std::string {
  if (info.revision.empty()) {
    return info.model_id;
  }
  return info.model_id + "@" + info.revision;
}

auto NormalizedSemanticPreference(QString preference) -> QString {
  preference = preference.trimmed().toLower();
  if (preference == QLatin1String(kSemanticPreferenceAlways) ||
      preference == QLatin1String(kSemanticPreferenceNever)) {
    return preference;
  }
  return QString::fromLatin1(kSemanticPreferenceAsk);
}

auto ClampToInt(size_t value) -> int {
  return static_cast<int>(
      std::min<size_t>(value, static_cast<size_t>(std::numeric_limits<int>::max())));
}

auto DefaultSemanticModelDirectory() -> QString {
  return QDir(QCoreApplication::applicationDirPath()).filePath(QStringLiteral("model"));
}

auto NormalizedProfileId(QString profile_id) -> QString {
  profile_id = profile_id.trimmed();
  for (const auto& profile : kSemanticModelProfiles) {
    if (profile_id == QLatin1String(profile.profile_id)) {
      return profile_id;
    }
  }
  return QString::fromLatin1(kMobileClipProfileId);
}

auto FindProfile(const QString& profile_id) -> const SemanticModelProfileUiInfo* {
  const QString normalized = NormalizedProfileId(profile_id);
  for (const auto& profile : kSemanticModelProfiles) {
    if (normalized == QLatin1String(profile.profile_id)) {
      return &profile;
    }
  }
  return &kSemanticModelProfiles[0];
}

auto ProfileRootPath(const QString& base_directory, const QString& profile_id) -> QString {
  return QDir(base_directory).filePath(NormalizedProfileId(profile_id));
}

auto QStringToPath(const QString& value) -> std::filesystem::path {
#ifdef _WIN32
  return std::filesystem::path(value.toStdWString());
#else
  return std::filesystem::path(value.toStdString());
#endif
}

auto PathString(const QString& value) -> std::string {
#ifdef _WIN32
  return QStringToPath(value).string();
#else
  return value.toStdString();
#endif
}

auto NormalizedEndpointPreset(QString preset) -> QString {
  preset = preset.trimmed().toLower();
  if (preset == QLatin1String("huggingface") || preset == QLatin1String("custom")) {
    return preset;
  }
  return QStringLiteral("mirror");
}

auto EndpointForPreset(const QString& preset, const QString& custom_endpoint) -> QString {
  const QString normalized = NormalizedEndpointPreset(preset);
  if (normalized == QLatin1String("huggingface")) {
    return QStringLiteral("https://huggingface.co");
  }
  if (normalized == QLatin1String("custom") && !custom_endpoint.trimmed().isEmpty()) {
    return custom_endpoint.trimmed();
  }
  return QStringLiteral("https://hf-mirror.com");
}

auto ProgressPercent(const SemanticModelManagerResult& result) -> int {
  if (!result.progress.has_value() || result.progress->bytes_total == 0) {
    return result.ok && result.status == "installed" ? 100 : 0;
  }
  return static_cast<int>(std::min<uint64_t>(
      100, (result.progress->bytes_downloaded * 100) / result.progress->bytes_total));
}

auto ItemsNeedingSemanticGeneration(const std::vector<SemanticGenerationItem>& items,
                                    SemanticStorageController&                 semantic,
                                    const std::string& model_key, bool force_regenerate)
    -> std::vector<SemanticGenerationItem> {
  if (force_regenerate || model_key.empty()) {
    return items;
  }

  std::vector<SemanticGenerationItem> pending;
  pending.reserve(items.size());
  constexpr bool require_label = true;
  for (const auto& item : items) {
    if (!semantic.HasReadyImageEmbedding(item.element_id, item.image_id, model_key,
                                         require_label)) {
      pending.push_back(item);
    }
  }
  return pending;
}

}  // namespace

class SemanticRuntimeSessionGuard final {
 public:
  explicit SemanticRuntimeSessionGuard(std::shared_ptr<SemanticRuntimeService> runtime)
      : runtime_(std::move(runtime)) {}

  ~SemanticRuntimeSessionGuard() {
    if (runtime_) {
      runtime_->Stop();
    }
  }

  SemanticRuntimeSessionGuard(const SemanticRuntimeSessionGuard&)            = delete;
  SemanticRuntimeSessionGuard& operator=(const SemanticRuntimeSessionGuard&) = delete;

 private:
  std::shared_ptr<SemanticRuntimeService> runtime_;
};

SemanticGenerationController::SemanticGenerationController(AlbumBackend& backend, QObject* parent)
    : QObject(parent), backend_(backend), model_download_timer_(this) {
  model_download_status_text_ = PL_TEXT("Model status has not been checked.");
  model_download_timer_.setInterval(kSemanticModelDownloadPollIntervalMs);
  connect(&model_download_timer_, &QTimer::timeout, this,
          &SemanticGenerationController::PollModelDownloadStatus);
}

bool SemanticGenerationController::PromptVisible() const {
  return prompt_pending_ && !running_ && !backend_.nikon_he_recovery_.is_active();
}

void SemanticGenerationController::StartPendingGeneration(bool forceRegenerate) {
  StartGenerationForItems(pending_items_, forceRegenerate);
}

void SemanticGenerationController::SkipPendingGeneration(bool rememberChoice) {
  if (rememberChoice) {
    SetImportPreference(QString::fromLatin1(kSemanticPreferenceNever));
  }
  ClearPrompt();
  status_text_ = PL_TEXT("Semantic generation skipped.");
  emit StateChanged();
}

QString SemanticGenerationController::ImportPreference() const {
  return NormalizedSemanticPreference(
      QSettings{}
          .value(QLatin1String(kSemanticGenerationImportPreferenceKey),
                 QLatin1String(kSemanticPreferenceAsk))
          .toString());
}

void SemanticGenerationController::SetImportPreference(const QString& preference) {
  QSettings{}.setValue(QLatin1String(kSemanticGenerationImportPreferenceKey),
                       NormalizedSemanticPreference(preference));
  emit StateChanged();
}

QVariantList SemanticGenerationController::ModelProfileOptions() const {
  QVariantList options;
  for (const auto& profile : kSemanticModelProfiles) {
    QVariantMap entry;
    entry.insert(QStringLiteral("profileId"), QString::fromLatin1(profile.profile_id));
    entry.insert(QStringLiteral("label"), QString::fromLatin1(profile.display_name));
    entry.insert(QStringLiteral("modelId"), QString::fromLatin1(profile.model_id));
    entry.insert(QStringLiteral("revision"), QString::fromLatin1(profile.revision));
    entry.insert(QStringLiteral("language"), QString::fromLatin1(profile.language));
    entry.insert(QStringLiteral("imageSize"), profile.image_size);
    entry.insert(QStringLiteral("nativeEmbeddingDim"), profile.native_embedding_dim);
    entry.insert(QStringLiteral("activatable"), profile.activatable);
    options.push_back(entry);
  }
  return options;
}

QString SemanticGenerationController::SelectedModelProfileId() const {
  return NormalizedProfileId(
      QSettings{}
          .value(QLatin1String(kSemanticModelProfileKey), QLatin1String(kMobileClipProfileId))
          .toString());
}

QString SemanticGenerationController::ActiveModelProfileId() const {
  return QString::fromLatin1(kMobileClipProfileId);
}

QString SemanticGenerationController::ModelDownloadDirectory() const {
  const QString stored =
      QSettings{}.value(QLatin1String(kSemanticModelDirectoryKey), QString{}).toString().trimmed();
  return stored.isEmpty() ? DefaultSemanticModelDirectory() : stored;
}

QString SemanticGenerationController::ModelEndpointPreset() const {
  return NormalizedEndpointPreset(
      QSettings{}
          .value(QLatin1String(kSemanticEndpointPresetKey), QStringLiteral("mirror"))
          .toString());
}

QString SemanticGenerationController::CustomModelEndpoint() const {
  return QSettings{}.value(QLatin1String(kSemanticCustomEndpointKey), QString{}).toString();
}

QString SemanticGenerationController::EffectiveModelEndpoint() const {
  return EndpointForPreset(ModelEndpointPreset(), CustomModelEndpoint());
}

void SemanticGenerationController::SetSelectedModelProfileId(const QString& profileId) {
  QSettings{}.setValue(QLatin1String(kSemanticModelProfileKey), NormalizedProfileId(profileId));
  model_download_status_text_ = PL_TEXT("Model status has not been checked.");
  model_download_progress_    = 0;
  emit StateChanged();
}

void SemanticGenerationController::SetModelDownloadDirectory(const QString& directory) {
  QString value = directory.trimmed();
  if (value.startsWith(QLatin1String("file:"))) {
    value = QUrl(value).toLocalFile();
  }
  QSettings{}.setValue(QLatin1String(kSemanticModelDirectoryKey),
                       value.isEmpty() ? DefaultSemanticModelDirectory() : value);
  model_download_status_text_ = PL_TEXT("Model directory updated.");
  model_download_progress_    = 0;
  emit StateChanged();
}

void SemanticGenerationController::SetModelEndpointPreset(const QString& preset) {
  QSettings{}.setValue(QLatin1String(kSemanticEndpointPresetKey), NormalizedEndpointPreset(preset));
  emit StateChanged();
}

void SemanticGenerationController::SetCustomModelEndpoint(const QString& endpoint) {
  QSettings{}.setValue(QLatin1String(kSemanticCustomEndpointKey), endpoint.trimmed());
  emit StateChanged();
}

void SemanticGenerationController::ResetModelDownloadDirectory() {
  QSettings{}.remove(QLatin1String(kSemanticModelDirectoryKey));
  model_download_status_text_ = PL_TEXT("Model directory reset to the executable folder.");
  model_download_progress_    = 0;
  emit StateChanged();
}

void SemanticGenerationController::RefreshSelectedModelStatus() {
  auto runtime = EnsureModelManagerRuntime();
  if (!runtime) {
    emit StateChanged();
    return;
  }

  const auto profile_id = SelectedModelProfileId();
  const auto result     = runtime->ValidateModel(
      profile_id.toStdString(), PathString(ModelDownloadDirectory()), kSemanticModelManagerTimeout);
  model_download_progress_ = ProgressPercent(result);
  if (result.ok) {
    model_download_status_text_ =
        PL_TEXT("Model is installed at %1",
                QString::fromStdString(
                    result.manifest.value_or(SemanticResolvedModelManifest{}).model_root));
  } else {
    const QString message =
        QString::fromStdString(result.error.empty() ? result.status : result.error);
    model_download_status_text_ = message.isEmpty() ? PL_TEXT("Model is not installed.")
                                                    : PL_TEXT("Model missing: %1", message);
  }
  emit StateChanged();
}

void SemanticGenerationController::StartSelectedModelDownload() {
  if (model_download_running_) {
    return;
  }
  auto runtime = EnsureModelManagerRuntime();
  if (!runtime) {
    emit StateChanged();
    return;
  }

  const auto result = runtime->DownloadModel(
      SelectedModelProfileId().toStdString(), PathString(ModelDownloadDirectory()),
      EffectiveModelEndpoint().toStdString(), kSemanticModelManagerTimeout);
  if (!result.ok || result.job_id.empty()) {
    const QString message =
        QString::fromStdString(result.error.empty() ? result.status : result.error);
    model_download_status_text_ = message.isEmpty() ? PL_TEXT("Model download failed to start.")
                                                    : PL_TEXT("Model download failed: %1", message);
    model_download_running_     = false;
    model_download_progress_    = ProgressPercent(result);
    emit StateChanged();
    return;
  }

  model_download_job_id_      = QString::fromStdString(result.job_id);
  model_download_running_     = true;
  model_download_progress_    = ProgressPercent(result);
  model_download_status_text_ = PL_TEXT("Model download queued from %1", EffectiveModelEndpoint());
  model_download_timer_.start();
  emit StateChanged();
}

void SemanticGenerationController::CancelSelectedModelDownload() {
  if (model_download_job_id_.isEmpty()) {
    return;
  }
  auto project = backend_.project_handler_.project();
  auto runtime = project ? project->GetSemanticRuntimeService() : nullptr;
  if (!runtime) {
    return;
  }

  std::string message;
  const bool  cancelled = runtime->CancelModelDownload(model_download_job_id_.toStdString(),
                                                       kSemanticModelManagerTimeout, &message);
  model_download_timer_.stop();
  model_download_running_     = false;
  model_download_progress_    = 0;
  model_download_status_text_ = cancelled ? PL_TEXT("Model download cancelled.")
                                          : PL_TEXT("Model download cancellation failed: %1",
                                                    QString::fromStdString(message));
  emit StateChanged();
}

void SemanticGenerationController::DeleteSelectedModel() {
  if (model_download_running_) {
    CancelSelectedModelDownload();
  }
  auto runtime = EnsureModelManagerRuntime();
  if (!runtime) {
    emit StateChanged();
    return;
  }
  const auto result =
      runtime->DeleteModel(SelectedModelProfileId().toStdString(),
                           PathString(ModelDownloadDirectory()), kSemanticModelManagerTimeout);
  model_download_progress_ = 0;
  if (result.ok) {
    model_download_status_text_ = PL_TEXT("Model files deleted.");
  } else {
    const QString message =
        QString::fromStdString(result.error.empty() ? result.status : result.error);
    model_download_status_text_ = message.isEmpty() ? PL_TEXT("Model delete failed.")
                                                    : PL_TEXT("Model delete failed: %1", message);
  }
  emit StateChanged();
}

void SemanticGenerationController::ActivateSelectedModel() {
  const QString profile_id = SelectedModelProfileId();
  if (profile_id != QLatin1String(kMobileClipProfileId)) {
    model_download_status_text_ = PL_TEXT("Only MobileCLIP2 S2 can be activated in this build.");
    emit StateChanged();
    return;
  }

  auto runtime = EnsureModelManagerRuntime();
  if (!runtime) {
    emit StateChanged();
    return;
  }
  const auto result = runtime->ValidateModel(
      profile_id.toStdString(), PathString(ModelDownloadDirectory()), kSemanticModelManagerTimeout);
  if (!result.ok || !result.manifest.has_value()) {
    const QString message =
        QString::fromStdString(result.error.empty() ? result.status : result.error);
    model_download_status_text_ = message.isEmpty()
                                      ? PL_TEXT("Install MobileCLIP2 S2 before activating it.")
                                      : PL_TEXT("Cannot activate model: %1", message);
    emit StateChanged();
    return;
  }

  auto project = backend_.project_handler_.project();
  if (!project) {
    model_download_status_text_ = PL_TEXT("Open a project before activating a semantic model.");
    emit StateChanged();
    return;
  }
  auto&             semantic = project->GetStorageService()->GetSemanticStorageController();
  const auto&       manifest = *result.manifest;
  const std::string model_key =
      manifest.revision.empty() ? manifest.model_id : manifest.model_id + "@" + manifest.revision;
  std::string error;
  if (!semantic.UpsertModel(
          SemanticModelRecord{.model_key_     = model_key,
                              .model_id_      = manifest.model_id,
                              .revision_      = manifest.revision,
                              .embedding_dim_ = static_cast<int>(manifest.embedding_dimension),
                              .image_size_    = static_cast<int>(manifest.image_size),
                              .engine_id_     = manifest.engine_profile_id,
                              .profile_id_    = manifest.profile_id,
                              .supported_text_languages_json_ = R"(["en"])",
                              .prompt_config_hash_  = kDefaultSemanticPhotographyPromptConfigHash,
                              .asset_manifest_json_ = {},
                              .active_              = true},
          &error)) {
    model_download_status_text_ =
        PL_TEXT("Semantic model activation failed: %1", QString::fromUtf8(error.c_str()));
  } else {
    model_key_                  = model_key;
    model_download_status_text_ = PL_TEXT("MobileCLIP2 S2 is active for this project.");
    RefreshAlbumSummary();
    backend_.ReloadCurrentFolder();
  }
  emit StateChanged();
}

void SemanticGenerationController::CancelGeneration() {
  if (job_) {
    job_->Cancel();
    status_text_ = PL_TEXT("Cancelling semantic generation...");
    emit StateChanged();
  }
}

void SemanticGenerationController::RefreshAlbumSummary() {
  const auto previous_total     = album_total_count_;
  const auto previous_labeled   = album_labeled_count_;
  const auto previous_unlabeled = album_unlabeled_count_;
  const auto previous_summary   = album_summary_text_;

  auto       project            = backend_.project_handler_.project();
  auto       browse             = project ? project->GetAlbumBrowseService() : nullptr;
  if (!project || !browse) {
    album_total_count_     = 0;
    album_labeled_count_   = 0;
    album_unlabeled_count_ = 0;
    album_summary_text_    = PL_TEXT("Open a project before running AI content recognition.");
    if (previous_total != album_total_count_ || previous_labeled != album_labeled_count_ ||
        previous_unlabeled != album_unlabeled_count_ ||
        previous_summary.source_ != album_summary_text_.source_ ||
        previous_summary.args_ != album_summary_text_.args_) {
      emit StateChanged();
    }
    return;
  }

  album_total_count_   = static_cast<int>(std::min<size_t>(
      browse->CountFilesInFolderById(0), static_cast<size_t>(std::numeric_limits<int>::max())));

  const auto model_key = ActiveModelKey();
  if (model_key.empty()) {
    album_labeled_count_ = 0;
  } else {
    album_labeled_count_ = static_cast<int>(std::min<size_t>(
        project->GetStorageService()->GetSemanticStorageController().CountImageLabelsInFolder(
            0, model_key),
        static_cast<size_t>(std::numeric_limits<int>::max())));
  }
  album_unlabeled_count_ = std::max(0, album_total_count_ - album_labeled_count_);
  album_summary_text_    = PL_TEXT("%1 image(s) total. %2 already have labels, %3 need labels.",
                                   album_total_count_, album_labeled_count_, album_unlabeled_count_);
  if (previous_total != album_total_count_ || previous_labeled != album_labeled_count_ ||
      previous_unlabeled != album_unlabeled_count_ ||
      previous_summary.source_ != album_summary_text_.source_ ||
      previous_summary.args_ != album_summary_text_.args_) {
    emit StateChanged();
  }
}

void SemanticGenerationController::StartAlbumGeneration(bool forceRegenerate) {
  auto project = backend_.project_handler_.project();
  auto browse  = project ? project->GetAlbumBrowseService() : nullptr;
  if (!project || !browse) {
    status_text_ = PL_TEXT("Semantic generation is unavailable without an open project.");
    emit StateChanged();
    return;
  }

  std::vector<SemanticGenerationItem> items;
  const auto                          files = browse->ListFilesInFolderById(0);
  items.reserve(files.size());
  for (const auto& file : files) {
    if (file.file_id_ == 0 || file.image_id_ == 0) {
      continue;
    }
    items.push_back(SemanticGenerationItem{file.file_id_, file.image_id_});
  }

  StartGenerationForItems(std::move(items), forceRegenerate);
}

void SemanticGenerationController::QueuePrompt(std::vector<SemanticGenerationItem> items) {
  pending_items_  = std::move(items);
  prompt_pending_ = !pending_items_.empty();
  total_          = static_cast<int>(pending_items_.size());
  embedded_       = 0;
  skipped_        = 0;
  failed_         = 0;
  canceled_       = 0;
  if (prompt_pending_) {
    status_text_ = PL_TEXT("Generate semantic labels for %1 imported image(s)?", total_);
  }
  emit StateChanged();
  ResumeQueuedWorkflow();
}

void SemanticGenerationController::ResumeQueuedWorkflow() {
  if (!prompt_pending_ || running_ || backend_.nikon_he_recovery_.is_active()) {
    emit StateChanged();
    return;
  }

  const QString preference =
      NormalizedSemanticPreference(QSettings{}
                                       .value(QLatin1String(kSemanticGenerationImportPreferenceKey),
                                              QLatin1String(kSemanticPreferenceAsk))
                                       .toString());
  if (preference == QLatin1String(kSemanticPreferenceAlways)) {
    StartPendingGeneration(false);
    return;
  }
  if (preference == QLatin1String(kSemanticPreferenceNever)) {
    ClearPrompt();
    return;
  }
  emit StateChanged();
}

auto SemanticGenerationController::StoredModelKey() const -> std::string {
  auto project = backend_.project_handler_.project();
  if (!project) {
    return {};
  }
  return project->GetStorageService()->GetSemanticStorageController().ActiveModelKey();
}

auto SemanticGenerationController::ActiveModelKey() const -> std::string {
  if (!model_key_.empty()) {
    return model_key_;
  }
  auto project = backend_.project_handler_.project();
  if (!project) {
    return {};
  }
  auto runtime = project->GetSemanticRuntimeService();
  if (runtime) {
    const auto status = runtime->Status();
    if (status.model_info.has_value()) {
      return SemanticModelKeyFromInfo(*status.model_info);
    }
  }
  return StoredModelKey();
}

auto SemanticGenerationController::LabelDisplayText(sl_element_id_t elementId) const -> QString {
  const auto model_key = ActiveModelKey();
  if (model_key.empty()) {
    return {};
  }
  auto project = backend_.project_handler_.project();
  if (!project) {
    return {};
  }
  std::string error;
  const auto  label =
      project->GetStorageService()->GetSemanticStorageController().GetImageLabelForFile(
          elementId, model_key, &error);
  if (!label.has_value()) {
    return {};
  }
  if (label->confident_ || label->top_scores_json_.empty()) {
    return QString::fromUtf8(label->label_.c_str());
  }

  const QJsonDocument doc =
      QJsonDocument::fromJson(QByteArray::fromStdString(label->top_scores_json_));
  if (!doc.isArray()) {
    return QString::fromUtf8(label->label_.c_str());
  }
  const auto  array      = doc.array();
  const auto  best_score = label->score_;
  QStringList labels;
  for (const auto& value : array) {
    const auto object = value.toObject();
    const auto score  = object.value(QStringLiteral("score")).toDouble(-1.0);
    const auto name   = object.value(QStringLiteral("label")).toString();
    if (name.isEmpty()) {
      continue;
    }
    if (labels.isEmpty() || (best_score - score) <= kDefaultSemanticLabelMarginThreshold) {
      labels.push_back(name);
      if (labels.size() >= static_cast<qsizetype>(kMaxSemanticImageLabelCount)) {
        break;
      }
    }
  }
  return labels.isEmpty() ? QString::fromUtf8(label->label_.c_str())
                          : labels.join(QStringLiteral(", "));
}

void SemanticGenerationController::StartGenerationForItems(
    std::vector<SemanticGenerationItem> items, bool forceRegenerate) {
  if (running_) {
    return;
  }
  if (items.empty()) {
    prompt_pending_ = false;
    status_text_    = PL_TEXT("No images are waiting for semantic generation.");
    RefreshAlbumSummary();
    emit StateChanged();
    return;
  }

  pending_items_  = std::move(items);
  prompt_pending_ = false;
  running_        = true;
  embedded_       = 0;
  skipped_        = 0;
  failed_         = 0;
  canceled_       = 0;
  total_          = 0;
  status_text_    = PL_TEXT("Preparing semantic generation...");
  backend_.SetTaskState(status_text_, 0, true);
  emit                                   StateChanged();

  QPointer<SemanticGenerationController> self(this);
  QTimer::singleShot(160, this, [self, forceRegenerate]() {
    if (self) {
      self->ContinueGenerationForItems(forceRegenerate);
    }
  });
}

void SemanticGenerationController::ContinueGenerationForItems(bool forceRegenerate) {
  if (!running_) {
    return;
  }

  auto project           = backend_.project_handler_.project();
  auto thumbnail_service = backend_.project_handler_.thumbnail_service();
  if (!project || !thumbnail_service) {
    running_ = false;
    pending_items_.clear();
    status_text_ = PL_TEXT("Semantic generation is unavailable without an open project.");
    backend_.SetTaskState(status_text_, 0, false);
    RefreshAlbumSummary();
    emit StateChanged();
    return;
  }

  auto runtime = project->GetSemanticRuntimeService();
  if (!runtime) {
    running_ = false;
    pending_items_.clear();
    status_text_ = PL_TEXT("Semantic runtime service is unavailable.");
    backend_.SetTaskState(status_text_, 0, false);
    RefreshAlbumSummary();
    emit StateChanged();
    return;
  }
  SemanticRuntimeOptions runtime_options =
      RuntimeOptionsForProfile(QString::fromLatin1(kMobileClipProfileId), true);
  auto runtime_status = runtime->Status();
  if (runtime_status.state == SemanticRuntimeState::kReady &&
      runtime_status.model_info.has_value() &&
      (runtime_status.model_info->model_id != runtime_options.model_id ||
       runtime_status.model_info->revision != runtime_options.revision ||
       runtime_status.model_info->model_root != runtime_options.model_root.string())) {
    runtime->Stop();
    runtime_status = runtime->Status();
  }
  if (runtime_status.state != SemanticRuntimeState::kReady ||
      !runtime_status.model_info.has_value()) {
    status_text_ = PL_TEXT("Starting semantic runtime...");
    emit StateChanged();

    if (!runtime->StartAndWait(runtime_options)) {
      runtime_status        = runtime->Status();
      const QString message = QString::fromStdString(runtime_status.message);
      running_              = false;
      pending_items_.clear();
      status_text_ = message.isEmpty() ? PL_TEXT("Semantic runtime failed to start.")
                                       : PL_TEXT("Semantic runtime failed to start: %1", message);
      backend_.SetTaskState(status_text_, 0, false);
      RefreshAlbumSummary();
      emit StateChanged();
      return;
    }

    runtime_status = runtime->Status();
    if (runtime_status.state != SemanticRuntimeState::kReady ||
        !runtime_status.model_info.has_value()) {
      const QString message = QString::fromStdString(runtime_status.message);
      running_              = false;
      pending_items_.clear();
      status_text_ = message.isEmpty()
                         ? PL_TEXT("Semantic runtime did not report model information.")
                         : PL_TEXT("Semantic runtime is not ready: %1", message);
      backend_.SetTaskState(status_text_, 0, false);
      RefreshAlbumSummary();
      emit StateChanged();
      return;
    }
  }

  auto              runtime_session = std::make_shared<SemanticRuntimeSessionGuard>(runtime);

  auto&             semantic        = project->GetStorageService()->GetSemanticStorageController();
  const std::string model_key       = SemanticModelKeyFromInfo(*runtime_status.model_info);
  std::string       error;
  if (!semantic.UpsertModel(
          SemanticModelRecord{
              .model_key_     = model_key,
              .model_id_      = runtime_status.model_info->model_id,
              .revision_      = runtime_status.model_info->revision,
              .embedding_dim_ = static_cast<int>(runtime_status.model_info->embedding_dimension),
              .image_size_    = static_cast<int>(runtime_status.model_info->image_size),
              .engine_id_     = runtime_status.model_info->provider,
              .profile_id_    = runtime_status.model_info->model_id,
              .supported_text_languages_json_ = R"(["en"])",
              .prompt_config_hash_            = kDefaultSemanticPhotographyPromptConfigHash,
              .active_                        = true},
          &error)) {
    running_ = false;
    pending_items_.clear();
    status_text_ =
        PL_TEXT("Semantic model registration failed: %1", QString::fromUtf8(error.c_str()));
    backend_.SetTaskState(status_text_, 0, false);
    RefreshAlbumSummary();
    emit StateChanged();
    return;
  }

  pending_items_ =
      ItemsNeedingSemanticGeneration(pending_items_, semantic, model_key, forceRegenerate);
  model_key_ = model_key;
  total_     = ClampToInt(pending_items_.size());
  embedded_  = 0;
  skipped_   = 0;
  failed_    = 0;
  canceled_  = 0;
  if (pending_items_.empty()) {
    running_     = false;
    status_text_ = PL_TEXT("All images already have semantic labels.");
    backend_.SetTaskState(status_text_, 100, false);
    backend_.ScheduleIdleTaskStateReset(1800);
    RefreshAlbumSummary();
    emit StateChanged();
    return;
  }
  status_text_ = PL_TEXT("Generating semantic labels for %1 image(s)...", total_);
  emit                      StateChanged();

  SemanticGenerationOptions options;
  options.thumbnail_resolution = ThumbnailResolution::k256;
  options.thumbnail_batch_size = 8;
  options.embedding_batch_size = 64;
  options.expected_model_info  = runtime_status.model_info;
  options.force_regenerate     = forceRegenerate;
  SemanticGenerationPersistenceOptions persistence;
  persistence.storage_controller = &semantic;
  persistence.model_key          = model_key;
  options.persistence            = persistence;

  auto thumbnails = std::make_shared<ThumbnailServiceSemanticThumbnailProvider>(thumbnail_service);
  auto embedder   = std::make_shared<SemanticRuntimeImageEmbeddingClient>(runtime);
  auto service    = std::make_shared<SemanticGenerationService>(thumbnails, embedder);
  QPointer<SemanticGenerationController> self(this);
  auto                                   job = service->StartGeneration(
      pending_items_, options,
      [self](const SemanticGenerationProgress& progress) {
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
      [self](std::vector<SemanticGenerationItemResult> results) {
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

  runtime_session_ = std::move(runtime_session);
  job_             = std::move(job);
}

auto SemanticGenerationController::EnsureModelManagerRuntime()
    -> std::shared_ptr<SemanticRuntimeService> {
  auto project = backend_.project_handler_.project();
  if (!project) {
    model_download_status_text_ =
        PL_TEXT("Open a project before managing semantic model downloads.");
    return nullptr;
  }
  auto runtime = project->GetSemanticRuntimeService();
  if (!runtime) {
    model_download_status_text_ = PL_TEXT("Semantic runtime service is unavailable.");
    return nullptr;
  }
  if (runtime->Status().state == SemanticRuntimeState::kReady) {
    return runtime;
  }

  SemanticRuntimeOptions options = RuntimeOptionsForProfile(SelectedModelProfileId(), false);
  if (!runtime->StartAndWait(options)) {
    const auto    status        = runtime->Status();
    const QString message       = QString::fromStdString(status.message);
    model_download_status_text_ = message.isEmpty()
                                      ? PL_TEXT("Semantic runtime failed to start.")
                                      : PL_TEXT("Semantic runtime failed to start: %1", message);
    return nullptr;
  }
  return runtime;
}

auto SemanticGenerationController::RuntimeOptionsForProfile(const QString& profileId,
                                                            bool           profileRoot) const
    -> SemanticRuntimeOptions {
  const auto*            profile = FindProfile(profileId);
  SemanticRuntimeOptions options;
  const QString          base_dir = ModelDownloadDirectory();
  const QString          root =
      profileRoot ? ProfileRootPath(base_dir, QString::fromLatin1(profile->profile_id)) : base_dir;
  options.model_root      = QStringToPath(root);
  options.model_id        = profile->model_id;
  options.revision        = profile->revision;
  options.hf_endpoint     = EffectiveModelEndpoint().toStdString();
  options.allow_download  = false;
  options.startup_timeout = kSemanticRuntimeStartupTimeout;
  return options;
}

void SemanticGenerationController::PollModelDownloadStatus() {
  if (model_download_job_id_.isEmpty()) {
    model_download_timer_.stop();
    model_download_running_ = false;
    emit StateChanged();
    return;
  }
  auto project = backend_.project_handler_.project();
  auto runtime = project ? project->GetSemanticRuntimeService() : nullptr;
  if (!runtime) {
    model_download_timer_.stop();
    model_download_running_     = false;
    model_download_status_text_ = PL_TEXT("Semantic runtime service is unavailable.");
    emit StateChanged();
    return;
  }

  const auto result        = runtime->GetModelDownloadStatus(model_download_job_id_.toStdString(),
                                                             kSemanticModelManagerTimeout);
  model_download_progress_ = ProgressPercent(result);
  if (result.progress.has_value()) {
    const QString message = QString::fromStdString(result.progress->message);
    model_download_status_text_ =
        message.isEmpty() ? PL_TEXT("Downloading model... %1%", model_download_progress_)
                          : PL_TEXT("%1 (%2%)", message, model_download_progress_);
  }
  if (!result.ok || result.status == "installed" || result.status == "cancelled" ||
      result.status == "error") {
    model_download_timer_.stop();
    model_download_running_ = false;
    if (result.ok && result.status == "installed") {
      model_download_progress_    = 100;
      model_download_status_text_ = PL_TEXT("Model download complete.");
    } else if (!result.ok || result.status == "error") {
      const QString message =
          QString::fromStdString(result.error.empty() ? result.status : result.error);
      model_download_status_text_ = message.isEmpty()
                                        ? PL_TEXT("Model download failed.")
                                        : PL_TEXT("Model download failed: %1", message);
    }
  }
  emit StateChanged();
}

void SemanticGenerationController::UpdateProgress(const SemanticGenerationProgress& progress) {
  total_              = static_cast<int>(progress.total);
  embedded_           = static_cast<int>(progress.embedded);
  skipped_            = static_cast<int>(progress.skipped);
  failed_             = static_cast<int>(progress.failed);
  canceled_           = static_cast<int>(progress.canceled);
  const int completed = embedded_ + skipped_ + failed_ + canceled_;
  status_text_ =
      PL_TEXT("Generating semantic labels... %1/%2 complete", completed, std::max(total_, 1));
  backend_.SetTaskState(status_text_, total_ > 0 ? (completed * 100) / total_ : 0, true);
  RefreshAlbumSummary();
  emit StateChanged();
}

void SemanticGenerationController::Finish(std::vector<SemanticGenerationItemResult> results) {
  running_ = false;
  job_.reset();
  runtime_session_.reset();
  pending_items_.clear();
  prompt_pending_ = false;

  int embedded    = 0;
  int skipped     = 0;
  int failed      = 0;
  int canceled    = 0;
  for (const auto& result : results) {
    switch (result.status) {
      case SemanticGenerationItemStatus::kEmbedded:
        ++embedded;
        break;
      case SemanticGenerationItemStatus::kSkipped:
        ++skipped;
        break;
      case SemanticGenerationItemStatus::kCanceled:
        ++canceled;
        break;
      case SemanticGenerationItemStatus::kError:
        ++failed;
        break;
      default:
        break;
    }
  }
  embedded_    = embedded;
  skipped_     = skipped;
  failed_      = failed;
  canceled_    = canceled;
  total_       = static_cast<int>(results.size());

  status_text_ = PL_TEXT("Semantic generation complete: %1 generated, %2 skipped, %3 failed.",
                         embedded, skipped, failed + canceled);
  backend_.SetTaskState(status_text_, 100, false);
  backend_.ScheduleIdleTaskStateReset(2200);
  RefreshAlbumSummary();
  backend_.ReloadCurrentFolder();
  if (backend_.project_handler_.PersistCurrentProjectState()) {
    QString ignored_error;
    (void)backend_.project_handler_.PackageCurrentProjectFiles(&ignored_error);
  }
  emit StateChanged();
}

void SemanticGenerationController::ClearPrompt() {
  pending_items_.clear();
  prompt_pending_ = false;
  ResetCounters();
}

void SemanticGenerationController::ResetCounters() {
  total_    = 0;
  embedded_ = 0;
  skipped_  = 0;
  failed_   = 0;
  canceled_ = 0;
}

}  // namespace alcedo::ui

#undef PL_TEXT
