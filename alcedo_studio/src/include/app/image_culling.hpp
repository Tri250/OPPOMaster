//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <filesystem>
#include <functional>
#include <string>
#include <vector>

#include <opencv2/core.hpp>

namespace alcedo::app {

// ── Hash types ──────────────────────────────────────────────────────────────
using ImageHash = uint64_t;

// ── Per-image quality metrics ───────────────────────────────────────────────
struct ImageQualityMetrics {
  double  sharpness      = 0.0;   // Laplacian variance (higher = sharper)
  double  exposure_score = 0.0;   // 0 = severe under/over-exposure, 1 = ideal
  double  center_focus   = 0.0;   // 0..1, weight of center region sharpness
  double  overall_score   = 0.0;  // composite 0..1 (higher = keep)
  bool    is_blurry       = false;
  bool    is_overexposed  = false;
  bool    is_underexposed = false;
};

// ── Perceptual hash result ──────────────────────────────────────────────────
struct ImageHashResult {
  ImageHash dhash      = 0;
  ImageHash phash      = 0;
  bool      hash_valid = false;
};

// ── Single image analysis entry ─────────────────────────────────────────────
struct ImageCullingEntry {
  std::filesystem::path path;
  ImageHashResult       hash;
  ImageQualityMetrics   quality;
  int64_t               file_size = 0;
  bool                  analyzed  = false;
  std::string           error;
};

// ── Group of similar images ─────────────────────────────────────────────────
struct ImageSimilarityGroup {
  std::vector<size_t> indices;   // into the original entries vector
  ImageHash            representative_hash = 0;
};

// ── Suggestion for a single image ───────────────────────────────────────────
enum class CullingSuggestion : uint8_t {
  kKeep    = 0,
  kCull    = 1,
  kReview  = 2,   // borderline — needs human judgement
  kUnknown = 3,
};

struct ImageCullingSuggestion {
  size_t             entry_index = 0;
  CullingSuggestion  suggestion  = CullingSuggestion::kUnknown;
  std::string        reason;
  double             score       = 0.0;  // composite keep score 0..1
};

// ── Analysis options ────────────────────────────────────────────────────────
struct ImageCullingOptions {
  // Sharpness: Laplacian variance below this is considered blurry.
  double blur_threshold = 100.0;

  // Exposure: fraction of pixels allowed in extreme bins (0..1).
  double overexposure_threshold  = 0.05;
  double underexposure_threshold = 0.05;

  // Hamming distance below which two hashes are considered duplicates.
  int    duplicate_hamming_threshold = 10;

  // Similarity: Hamming distance below which images are grouped.
  int    similarity_hamming_threshold = 21;

  // Weight factors for the composite score (must sum to ~1.0).
  double sharpness_weight    = 0.35;
  double exposure_weight     = 0.25;
  double center_focus_weight = 0.20;
  double uniqueness_weight   = 0.20;

  // Minimum overall score to be kept when duplicates exist.
  double min_keep_score = 0.35;

  // When comparing duplicates, prefer the larger file (likely higher quality).
  bool   prefer_larger_file = true;
};

// ── Progress callback ───────────────────────────────────────────────────────
struct ImageCullingProgress {
  size_t total     = 0;
  size_t processed = 0;
  size_t errors    = 0;
};

using ImageCullingProgressCallback =
    std::function<void(const ImageCullingProgress&)>;

// ── Final result ────────────────────────────────────────────────────────────
struct ImageCullingResult {
  std::vector<ImageCullingEntry>       entries;
  std::vector<ImageSimilarityGroup>    groups;
  std::vector<ImageCullingSuggestion>  suggestions;
  size_t                               keep_count   = 0;
  size_t                               cull_count   = 0;
  size_t                               review_count = 0;
};

// ── Service ─────────────────────────────────────────────────────────────────
class ImageCullingService {
 public:
  ImageCullingService()          = default;
  ~ImageCullingService()         = default;

  ImageCullingService(const ImageCullingService&)            = delete;
  ImageCullingService& operator=(const ImageCullingService&) = delete;

  /// Analyze a set of image paths. Returns a populated result with quality
  /// metrics, perceptual hashes, duplicate groups, and keep/cull suggestions.
  /// @param paths       File paths to analyze.
  /// @param options     Tuning parameters (defaults are reasonable).
  /// @param on_progress Optional callback invoked after each image is processed.
  auto Analyze(const std::vector<std::filesystem::path>& paths,
               const ImageCullingOptions& options = {},
               ImageCullingProgressCallback on_progress = {})
      -> ImageCullingResult;

  /// Compute perceptual hashes and quality metrics for a single image file.
  /// @returns true on success; false with error populated in the entry.
  static auto AnalyzeSingle(const std::filesystem::path& path,
                            const ImageCullingOptions& options,
                            ImageCullingEntry& entry) -> bool;

  /// Group entries by perceptual hash similarity.
  static auto GroupSimilar(std::vector<ImageCullingEntry>& entries,
                            int hamming_threshold)
      -> std::vector<ImageSimilarityGroup>;

  /// Generate keep/cull suggestions from analyzed entries and groups.
  static auto GenerateSuggestions(
      const std::vector<ImageCullingEntry>& entries,
      const std::vector<ImageSimilarityGroup>& groups,
      const ImageCullingOptions& options)
      -> std::vector<ImageCullingSuggestion>;

  /// Compute a composite keep score (0..1) for a single entry.
  static auto ComputeCompositeScore(const ImageQualityMetrics& metrics,
                                     double uniqueness_bonus,
                                     const ImageCullingOptions& options) -> double;

 private:
  /// Compute dHash (difference hash) from a grayscale cv::Mat.
  static auto ComputeDHash(const cv::Mat& gray, int hash_size = 8) -> ImageHash;

  /// Compute pHash (perceptual hash via DCT) from a grayscale cv::Mat.
  static auto ComputePHash(const cv::Mat& gray, int hash_size = 8) -> ImageHash;

  /// Compute Laplacian variance (sharpness metric).
  static auto ComputeSharpness(const cv::Mat& gray) -> double;

  /// Compute center-focus weight: ratio of center-region sharpness to overall.
  static auto ComputeCenterFocus(const cv::Mat& gray) -> double;

  /// Compute exposure quality score (0..1).
  static auto ComputeExposureScore(const cv::Mat& gray) -> double;

  /// Compute Hamming distance between two 64-bit hashes.
  static auto HammingDistance(ImageHash a, ImageHash b) -> int;
};

}  // namespace alcedo::app