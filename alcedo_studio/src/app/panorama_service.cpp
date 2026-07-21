//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/panorama_service.hpp"

#include <opencv2/core.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/stitching.hpp>
#include <opencv2/stitching/detail/matchers.hpp>
#include <opencv2/stitching/detail/motion_estimators.hpp>
#include <opencv2/stitching/detail/seam_finders.hpp>
#include <opencv2/stitching/detail/blenders.hpp>
#include <opencv2/stitching/detail/warpers.hpp>

#include <algorithm>
#include <cmath>
#include <format>
#include <stdexcept>
#include <utility>

#include "app/image_pool_service.hpp"
#include "app/sleeve_service.hpp"
#include "pipeline_service.hpp"
#include "utils/diagnostics/app_logging.hpp"

namespace alcedo {

namespace {
auto StageToString(PanoramaProgress::Stage stage) -> const char* {
  switch (stage) {
    case PanoramaProgress::Stage::kIdle:
      return "Idle";
    case PanoramaProgress::Stage::kLoadingImages:
      return "Loading images";
    case PanoramaProgress::Stage::kFeatureDetection:
      return "Detecting features";
    case PanoramaProgress::Stage::kFeatureMatching:
      return "Matching features";
    case PanoramaProgress::Stage::kHomographyEstimation:
      return "Estimating homography";
    case PanoramaProgress::Stage::kWarping:
      return "Warping images";
    case PanoramaProgress::Stage::kSeamFinding:
      return "Finding seams";
    case PanoramaProgress::Stage::kBlending:
      return "Blending";
    case PanoramaProgress::Stage::kSavingResult:
      return "Saving result";
    case PanoramaProgress::Stage::kDone:
      return "Done";
    case PanoramaProgress::Stage::kFailed:
      return "Failed";
  }
  return "Unknown";
}

void EmitProgress(std::function<void(const PanoramaProgress&)>& callback,
                  PanoramaProgress::Stage stage, size_t current, size_t total,
                  const std::string& message = "") {
  if (callback) {
    PanoramaProgress p;
    p.stage_   = stage;
    p.current_ = current;
    p.total_   = total;
    p.message_ = message.empty() ? StageToString(stage) : message;
    p.percent_ = total > 0 ? static_cast<float>(current) / static_cast<float>(total) : 0.0f;
    callback(p);
  }
}

auto MapFeatureType(const std::string& feature_type)
    -> cv::detail::Feature2DCollection::Feature2DType {
  if (feature_type == "sift") {
    return cv::detail::Feature2DCollection::Feature2DType::SIFT;
  }
  if (feature_type == "akaze") {
    return cv::detail::Feature2DCollection::Feature2DType::AKAZE;
  }
  // Default to ORB for speed.
  return cv::detail::Feature2DCollection::Feature2DType::ORB;
}

auto MapWaveCorrection(const std::string& wc) -> cv::detail::WaveCorrectKind {
  if (wc == "horiz") {
    return cv::detail::WaveCorrectKind::HORIZONTAL;
  }
  if (wc == "vert") {
    return cv::detail::WaveCorrectKind::VERTICAL;
  }
  if (wc == "no") {
    return cv::detail::WaveCorrectKind::WAVE_CORRECT_NONE;
  }
  return cv::detail::WaveCorrectKind::HORIZONTAL;
}

}  // namespace

PanoramaService::PanoramaService(std::shared_ptr<SleeveServiceImpl> sleeve_service,
                                 std::shared_ptr<ImagePoolService>  image_pool_service,
                                 std::shared_ptr<PipelineMgmtService> pipeline_service)
    : sleeve_service_(std::move(sleeve_service)),
      image_pool_service_(std::move(image_pool_service)),
      pipeline_service_(std::move(pipeline_service)) {}

void PanoramaService::StitchAsync(const std::vector<PanoramaInputImage>& inputs,
                                  const PanoramaConfig& config,
                                  std::function<void(const PanoramaProgress&)> progress_callback,
                                  std::function<void(PanoramaResult)> completion_callback) {
  if (running_.load()) {
    if (completion_callback) {
      PanoramaResult r;
      r.success_ = false;
      r.message_ = "Another panorama stitch is already in progress";
      completion_callback(r);
    }
    return;
  }
  if (inputs.size() < 2) {
    if (completion_callback) {
      PanoramaResult r;
      r.success_ = false;
      r.message_ = "At least 2 images are required for panorama stitching";
      completion_callback(r);
    }
    return;
  }

  cancelled_.store(false);
  running_.store(true);

  thread_pool.Enqueue([this, inputs, config, progress_callback, completion_callback]() {
    PanoramaResult result = RunStitchPipeline(inputs, config, progress_callback);
    running_.store(false);
    if (completion_callback) {
      completion_callback(result);
    }
  });
}

void PanoramaService::Cancel() {
  cancelled_.store(true);
}

auto PanoramaService::IsRunning() const -> bool {
  return running_.load();
}

auto PanoramaService::RunStitchPipeline(const std::vector<PanoramaInputImage>& inputs,
                                        const PanoramaConfig& config,
                                        std::function<void(const PanoramaProgress&)> progress_cb)
    -> PanoramaResult {
  const size_t n = inputs.size();
  const auto cancelled_check = [this]() -> bool { return cancelled_.load(); };

  // Stage 1: Load images.
  EmitProgress(progress_cb, PanoramaProgress::Stage::kLoadingImages, 0, n);
  std::vector<cv::Mat> images(n);
  for (size_t i = 0; i < n; ++i) {
    if (cancelled_check()) {
      return {.success_ = false, .message_ = "Cancelled"};
    }
    images[i] = cv::imread(inputs[i].file_path_.string(), cv::IMREAD_COLOR);
    if (images[i].empty()) {
      return {.success_ = false,
              .message_ = std::format("Failed to load image: {}", inputs[i].file_path_.string())};
    }
    EmitProgress(progress_cb, PanoramaProgress::Stage::kLoadingImages, i + 1, n);
  }

  // Stage 2: Feature detection.
  EmitProgress(progress_cb, PanoramaProgress::Stage::kFeatureDetection, 0, n);
  cv::Ptr<cv::Feature2D> finder;
  const auto ft = MapFeatureType(config.feature_type_);
  switch (ft) {
    case cv::detail::Feature2DCollection::Feature2DType::SIFT:
      finder = cv::SIFT::create();
      break;
    case cv::detail::Feature2DCollection::Feature2DType::AKAZE:
      finder = cv::AKAZE::create();
      break;
    default:
      finder = cv::ORB::create();
      break;
  }

  std::vector<cv::detail::ImageFeatures> image_features(n);
  cv::Mat full_img;
  for (size_t i = 0; i < n; ++i) {
    if (cancelled_check()) {
      return {.success_ = false, .message_ = "Cancelled"};
    }
    double scale = 1.0;
    if (config.max_working_megapixels_ > 0) {
      double megapixels =
          static_cast<double>(images[i].cols) * images[i].rows / 1000000.0;
      if (megapixels > static_cast<double>(config.max_working_megapixels_)) {
        scale = std::sqrt(static_cast<double>(config.max_working_megapixels_) / megapixels);
      }
    }
    if (std::abs(scale - 1.0) > 1e-6) {
      cv::resize(images[i], full_img, cv::Size(), scale, scale);
    } else {
      full_img = images[i];
    }
    cv::detail::computeImageFeatures(finder, full_img, image_features[i]);
    image_features[i].img_idx = static_cast<int>(i);
    EmitProgress(progress_cb, PanoramaProgress::Stage::kFeatureDetection, i + 1, n);
  }

  // Stage 3: Feature matching.
  EmitProgress(progress_cb, PanoramaProgress::Stage::kFeatureMatching, 0, n);
  std::vector<cv::detail::MatchesInfo> pairwise_matches;
  cv::Ptr<cv::detail::BestOf2NearestMatcher> matcher =
      cv::makePtr<cv::detail::BestOf2NearestMatcher>(false, 0.3f);
  (*matcher)(image_features, pairwise_matches);
  matcher.reset();
  if (cancelled_check()) {
    return {.success_ = false, .message_ = "Cancelled"};
  }
  EmitProgress(progress_cb, PanoramaProgress::Stage::kFeatureMatching, n, n);

  // Stage 4: Homography estimation / bundle adjustment.
  EmitProgress(progress_cb, PanoramaProgress::Stage::kHomographyEstimation, 0, 1);
  cv::Ptr<cv::detail::HomographyBasedEstimator> estimator =
      cv::makePtr<cv::detail::HomographyBasedEstimator>();
  std::vector<cv::detail::CameraParams> cameras;
  if (!(*estimator)(image_features, pairwise_matches, cameras)) {
    return {.success_ = false, .message_ = "Homography estimation failed"};
  }

  for (auto& cam : cameras) {
    cam.R.convertTo(cam.R, CV_32F);
  }

  // Bundle adjustment.
  cv::Ptr<cv::detail::BundleAdjusterBase> adjuster =
      cv::makePtr<cv::detail::BundleAdjusterRay>();
  adjuster->setConfThresh(config.confidence_);
  if (!(*adjuster)(image_features, pairwise_matches, cameras)) {
    return {.success_ = false, .message_ = "Bundle adjustment failed"};
  }

  // Wave correction.
  if (config.wave_correction_ != "no") {
    auto wc_kind = MapWaveCorrection(config.wave_correction_);
    std::vector<cv::Mat> rmats;
    rmats.reserve(cameras.size());
    for (const auto& cam : cameras) {
      rmats.push_back(cam.R.clone());
    }
    cv::detail::waveCorrect(rmats, wc_kind);
    for (size_t i = 0; i < cameras.size(); ++i) {
      cameras[i].R = rmats[i];
    }
  }

  if (cancelled_check()) {
    return {.success_ = false, .message_ = "Cancelled"};
  }
  EmitProgress(progress_cb, PanoramaProgress::Stage::kHomographyEstimation, 1, 1);

  // Stage 5: Warping.
  EmitProgress(progress_cb, PanoramaProgress::Stage::kWarping, 0, n);
  float warped_image_scale = 0.0f;
  for (const auto& cam : cameras) {
    float focal = static_cast<float>(cam.focal);
    if (focal > warped_image_scale) {
      warped_image_scale = focal;
    }
  }

  float seam_scale = 1.0f;
  const float seam_work_aspect = 1.0f;
  cv::Ptr<cv::WarperCreator> warper =
      cv::makePtr<cv::SphericalWarper>();
  std::vector<cv::Mat> masks(n);

  std::vector<cv::Point> corners(n);
  std::vector<cv::Size> sizes(n);
  std::vector<cv::Mat> images_warped(n);
  std::vector<cv::Mat> masks_warped(n);

  for (size_t i = 0; i < n; ++i) {
    if (cancelled_check()) {
      return {.success_ = false, .message_ = "Cancelled"};
    }
    cv::Mat img = images[i];
    cv::Mat mask;
    mask.create(img.size(), CV_8U);
    mask.setTo(cv::Scalar::all(255));

    cv::Ptr<cv::detail::RotationWarper> w =
        warper->create(static_cast<float>(cameras[i].focal) / seam_scale, img.size());
    cv::Mat K;
    cameras[i].K().convertTo(K, CV_32F);
    corners[i] = w->warp(img, K, cameras[i].R, cv::INTER_LINEAR, cv::BORDER_REFLECT,
                         images_warped[i]);
    w->warp(mask, K, cameras[i].R, cv::INTER_NEAREST, cv::BORDER_CONSTANT, masks_warped[i]);
    sizes[i] = images_warped[i].size();
    EmitProgress(progress_cb, PanoramaProgress::Stage::kWarping, i + 1, n);
  }

  // Stage 6: Seam finding.
  EmitProgress(progress_cb, PanoramaProgress::Stage::kSeamFinding, 0, 1);
  cv::Ptr<cv::detail::SeamFinder> seam_finder;
  if (config.blender_type_ == "no") {
    seam_finder = cv::makePtr<cv::detail::NoSeamFinder>();
  } else {
    seam_finder = cv::makePtr<cv::detail::GraphCutSeamFinder>(
        cv::detail::GraphCutSeamFinderBase::COST_COLOR);
  }
  seam_finder->find(images_warped, corners, masks_warped);
  if (cancelled_check()) {
    return {.success_ = false, .message_ = "Cancelled"};
  }
  EmitProgress(progress_cb, PanoramaProgress::Stage::kSeamFinding, 1, 1);

  // Stage 7: Blending.
  EmitProgress(progress_cb, PanoramaProgress::Stage::kBlending, 0, 1);
  // Compute the panorama bounding box.
  cv::Rect dst_roi = cv::detail::resultRoi(corners, sizes);
  cv::Mat panorama;
  cv::Mat panorama_mask;

  cv::Ptr<cv::detail::Blender> blender;
  if (config.blender_type_ == "multiband") {
    auto mb = cv::makePtr<cv::detail::MultiBandBlender>(false);
    blender = mb;
  } else if (config.blender_type_ == "feather") {
    blender = cv::makePtr<cv::detail::FeatherBlender>();
  } else {
    blender = cv::makePtr<cv::detail::NormalBlender>();
  }

  blender->prepare(dst_roi);
  for (size_t i = 0; i < n; ++i) {
    if (cancelled_check()) {
      return {.success_ = false, .message_ = "Cancelled"};
    }
    blender->feed(images_warped[i], masks_warped[i], corners[i]);
  }
  blender->blend(panorama, panorama_mask);
  EmitProgress(progress_cb, PanoramaProgress::Stage::kBlending, 1, 1);

  // Stage 8: Save the result.
  EmitProgress(progress_cb, PanoramaProgress::Stage::kSavingResult, 0, 1);
  PanoramaResult result;
  result.result_width_  = panorama.cols;
  result.result_height_ = panorama.rows;

  // Save to a temporary file, then import into the project.
  const std::string tmp_path =
      std::filesystem::temp_directory_path().string() + "/alcedo_panorama_result.jpg";
  if (!cv::imwrite(tmp_path, panorama)) {
    EmitProgress(progress_cb, PanoramaProgress::Stage::kFailed, 0, 0,
                 "Failed to save panorama image");
    result.success_ = false;
    result.message_ = "Failed to save panorama image";
    return result;
  }
  result.result_file_path_ = tmp_path;
  result.success_ = true;

  // Import the result into the project via sleeve service.
  if (sleeve_service_) {
    try {
      auto [file, sync] = sleeve_service_->CreateFileInLibrary("Panorama");
      if (file) {
        result.result_image_id_ = file->GetId();
      }
    } catch (const std::exception& e) {
      APP_LOG_WARN_DEFAULT("PanoramaService: Failed to register result in sleeve: %s", e.what());
    }
  }

  EmitProgress(progress_cb, PanoramaProgress::Stage::kSavingResult, 1, 1);
  EmitProgress(progress_cb, PanoramaProgress::Stage::kDone, n, n, "Stitch complete");
  return result;
}

}  // namespace alcedo
