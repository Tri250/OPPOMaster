//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <filesystem>
#include <functional>
#include <memory>
#include <mutex>
#include <string>
#include <vector>

#include "concurrency/thread_pool.hpp"
#include "type/type.hpp"

namespace cv {
class Mat;
}  // namespace cv

namespace alcedo {

class SleeveServiceImpl;
class ImagePoolService;
class PipelineMgmtService;
class PipelineScheduler;

// Metadata about one image selected for panorama stitching.
struct PanoramaInputImage {
  image_id_t                image_id_ = 0;
  std::filesystem::path     file_path_;
  int                       width_  = 0;
  int                       height_ = 0;
};

// Progress callback information for the stitching pipeline.
struct PanoramaProgress {
  enum class Stage {
    kIdle = 0,
    kLoadingImages,
    kFeatureDetection,
    kFeatureMatching,
    kHomographyEstimation,
    kWarping,
    kSeamFinding,
    kBlending,
    kSavingResult,
    kDone,
    kFailed,
  };

  Stage       stage_       = Stage::kIdle;
  size_t      current_     = 0;
  size_t      total_       = 0;
  std::string message_{};
  float       percent_     = 0.0f;  // 0.0 - 1.0
};

// Result of a panorama stitch operation.
struct PanoramaResult {
  bool        success_              = false;
  image_id_t  result_image_id_     = 0;
  std::string result_file_path_{};
  int         result_width_        = 0;
  int         result_height_       = 0;
  std::string message_{};
};

// Configuration for the panorama stitching pipeline.
struct PanoramaConfig {
  // Feature detector type: "orb", "sift", "akaze"
  std::string feature_type_ = "orb";
  // Confidence threshold for homography estimation (0.0 - 1.0)
  double      confidence_    = 0.85;
  // Wave correction type: "auto", "horiz", "vert", "no"
  std::string wave_correction_ = "auto";
  // Blender type: "multiband", "feather", "no"
  std::string blender_type_    = "multiband";
  // Whether to try to estimate camera parameters as unknown
  bool       assume_unknown_focal_ = true;
  // Maximum resolution for processing (0 = no limit)
  int        max_working_megapixels_ = 0;
};

// Service that orchestrates panorama stitching from multiple input images.
// Handles feature detection, matching, warping, seam finding, and blending
// using OpenCV's stitching module, and saves the result back into the project.
class PanoramaService {
 public:
  PanoramaService() = delete;
  PanoramaService(std::shared_ptr<SleeveServiceImpl> sleeve_service,
                  std::shared_ptr<ImagePoolService>  image_pool_service,
                  std::shared_ptr<PipelineMgmtService> pipeline_service);

  PanoramaService(const PanoramaService&)            = delete;
  PanoramaService& operator=(const PanoramaService&) = delete;

  // Start a panorama stitch job asynchronously. The progress callback is
  // called from a worker thread; the completion callback is posted back to
  // the calling thread via the thread pool.
  void StitchAsync(const std::vector<PanoramaInputImage>& inputs,
                   const PanoramaConfig& config,
                   std::function<void(const PanoramaProgress&)> progress_callback,
                   std::function<void(PanoramaResult)> completion_callback);

  // Cancel any in-progress stitch operation.
  void Cancel();

  // Query whether a stitch is currently running.
  auto IsRunning() const -> bool;

 private:
  auto RunStitchPipeline(const std::vector<PanoramaInputImage>& inputs,
                         const PanoramaConfig& config,
                         std::function<void(const PanoramaProgress&)> progress_callback)
      -> PanoramaResult;

  std::shared_ptr<SleeveServiceImpl>    sleeve_service_;
  std::shared_ptr<ImagePoolService>     image_pool_service_;
  std::shared_ptr<PipelineMgmtService>  pipeline_service_;

  std::atomic<bool>   cancelled_{false};
  std::atomic<bool>   running_{false};
  mutable std::mutex  cancel_mutex_;

  // Keep this last so worker threads are joined before other members are torn down.
  ThreadPool thread_pool_{1};
};

}  // namespace alcedo
