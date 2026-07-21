//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ui/alcedo_main/album_backend/panorama_controller.hpp"

#include <QCoreApplication>
#include <QDebug>

#include "app/panorama_service.hpp"
#include "ui/alcedo_main/album_backend/album_backend.hpp"
#include "ui/alcedo_main/album_backend/project_handler.hpp"
#include "utils/logging/app_logging.hpp"

namespace alcedo::ui {

PanoramaController::PanoramaController(AlbumBackend& backend, QObject* parent)
    : QObject(parent), backend_(backend) {}

void PanoramaController::stitchImages(const QVariantList& imageIds,
                                      const QVariantMap& config) {
  if (running_) {
    return;
  }

  auto* ph = backend_.GetProjectHandler();
  if (!ph || !ph->project()) {
    error_message_ = QCoreApplication::translate("PanoramaController",
                                                  "No project is open.");
    failed_        = true;
    emit ResultChanged();
    return;
  }

  std::vector<alcedo::PanoramaInputImage> inputs;
  inputs.reserve(static_cast<size_t>(imageIds.size()));
  for (const auto& id_var : imageIds) {
    alcedo::PanoramaInputImage input;
    input.image_id_ = static_cast<alcedo::image_id_t>(id_var.toUInt());
    inputs.push_back(std::move(input));
  }

  alcedo::PanoramaConfig pano_config;
  if (config.contains("featureType")) {
    pano_config.feature_type_ = config.value("featureType").toString().toStdString();
  }
  if (config.contains("confidence")) {
    pano_config.confidence_ = config.value("confidence").toDouble();
  }
  if (config.contains("waveCorrection")) {
    pano_config.wave_correction_ = config.value("waveCorrection").toString().toStdString();
  }
  if (config.contains("blenderType")) {
    pano_config.blender_type_ = config.value("blenderType").toString().toStdString();
  }
  if (config.contains("assumeUnknownFocal")) {
    pano_config.assume_unknown_focal_ = config.value("assumeUnknownFocal").toBool();
  }
  if (config.contains("maxWorkingMegapixels")) {
    pano_config.max_working_megapixels_ = config.value("maxWorkingMegapixels").toInt();
  }

  // Create PanoramaService with current project dependencies.
  service_ = std::make_unique<alcedo::PanoramaService>(
      ph->project()->GetSleeveService(),
      ph->project()->GetImagePoolService(),
      ph->pipeline_service());

  running_       = true;
  progress_      = 0.0f;
  has_result_    = false;
  failed_        = false;
  error_message_ = QString();
  result_        = QVariantMap();
  stage_text_    = StageToString(alcedo::PanoramaProgress::Stage::kIdle);

  emit StateChanged();
  emit ProgressChanged();

  service_->StitchAsync(
      inputs, pano_config,
      // Progress callback — called from worker thread; marshal to main thread.
      [this](const alcedo::PanoramaProgress& progress) {
        QMetaObject::invokeMethod(this, [this, progress]() {
          HandleProgress(progress);
        }, Qt::QueuedConnection);
      },
      // Completion callback — called from worker thread; marshal to main thread.
      [this](alcedo::PanoramaResult result) {
        QMetaObject::invokeMethod(this, [this, result]() {
          HandleCompletion(result);
        }, Qt::QueuedConnection);
      });
}

void PanoramaController::cancel() {
  if (service_) {
    service_->Cancel();
  }
}

void PanoramaController::dismissResult() {
  has_result_    = false;
  failed_        = false;
  error_message_ = QString();
  result_        = QVariantMap();
  emit ResultChanged();
}

void PanoramaController::HandleProgress(const alcedo::PanoramaProgress& progress) {
  progress_   = progress.percent_;
  stage_text_ = StageToString(progress.stage_);

  if (!progress.message_.empty()) {
    stage_text_ = QString::fromStdString(progress.message_);
  }

  emit ProgressChanged();
}

void PanoramaController::HandleCompletion(alcedo::PanoramaResult result) {
  running_  = false;
  progress_ = 1.0f;

  if (result.success_) {
    has_result_ = true;
    failed_     = false;
    result_     = QVariantMap{
        {"imageId",       static_cast<uint>(result.result_image_id_)},
        {"filePath",      QString::fromStdString(result.result_file_path_)},
        {"width",         result.result_width_},
        {"height",        result.result_height_},
    };
  } else {
    has_result_    = false;
    failed_        = true;
    error_message_ = QString::fromStdString(result.message_);
  }

  emit StateChanged();
  emit ProgressChanged();
  emit ResultChanged();
}

QString PanoramaController::StageToString(alcedo::PanoramaProgress::Stage stage) {
  switch (stage) {
    case alcedo::PanoramaProgress::Stage::kIdle:
      return QCoreApplication::translate("PanoramaController", "Idle");
    case alcedo::PanoramaProgress::Stage::kLoadingImages:
      return QCoreApplication::translate("PanoramaController", "Loading images…");
    case alcedo::PanoramaProgress::Stage::kFeatureDetection:
      return QCoreApplication::translate("PanoramaController", "Detecting features…");
    case alcedo::PanoramaProgress::Stage::kFeatureMatching:
      return QCoreApplication::translate("PanoramaController", "Matching features…");
    case alcedo::PanoramaProgress::Stage::kHomographyEstimation:
      return QCoreApplication::translate("PanoramaController", "Estimating homography…");
    case alcedo::PanoramaProgress::Stage::kWarping:
      return QCoreApplication::translate("PanoramaController", "Warping images…");
    case alcedo::PanoramaProgress::Stage::kSeamFinding:
      return QCoreApplication::translate("PanoramaController", "Finding seams…");
    case alcedo::PanoramaProgress::Stage::kBlending:
      return QCoreApplication::translate("PanoramaController", "Blending…");
    case alcedo::PanoramaProgress::Stage::kSavingResult:
      return QCoreApplication::translate("PanoramaController", "Saving result…");
    case alcedo::PanoramaProgress::Stage::kDone:
      return QCoreApplication::translate("PanoramaController", "Done");
    case alcedo::PanoramaProgress::Stage::kFailed:
      return QCoreApplication::translate("PanoramaController", "Failed");
    default:
      return {};
  }
}

}  // namespace alcedo::ui
