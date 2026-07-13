//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/image_culling.hpp"

#include <algorithm>
#include <bitset>
#include <cmath>
#include <cstring>
#include <numeric>
#include <stdexcept>
#include <unordered_set>

#include <opencv2/imgcodecs.hpp>
#include <opencv2/imgproc.hpp>

namespace alcedo::app {

namespace {

// ── Internal helpers ────────────────────────────────────────────────────────

/// Resize and convert to grayscale for analysis. Uses a moderate size that
/// balances accuracy with performance for large libraries.
auto LoadAnalysisImage(const std::filesystem::path& path) -> cv::Mat {
  cv::Mat color = cv::imread(path.string(), cv::IMREAD_COLOR);
  if (color.empty()) {
    return {};
  }
  cv::Mat gray;
  cv::cvtColor(color, gray, cv::COLOR_BGR2GRAY);
  return gray;
}

/// Count set bits in a 64-bit integer (popcount).
auto PopCount(uint64_t x) -> int {
  return static_cast<int>(std::bitset<64>(x).count());
}

/// Clamp a value to [0, 1].
auto Clamp01(double v) -> double {
  return std::max(0.0, std::min(1.0, v));
}

}  // namespace

// ── dHash (difference hash) ─────────────────────────────────────────────────

auto ImageCullingService::ComputeDHash(const cv::Mat& gray, int hash_size)
    -> ImageHash {
  cv::Mat resized;
  cv::resize(gray, resized, cv::Size(hash_size + 1, hash_size), 0, 0,
             cv::INTER_AREA);

  ImageHash hash = 0;
  int bit = 0;
  for (int row = 0; row < hash_size; ++row) {
    for (int col = 0; col < hash_size; ++col) {
      if (resized.at<uint8_t>(row, col) > resized.at<uint8_t>(row, col + 1)) {
        hash |= (static_cast<ImageHash>(1) << bit);
      }
      ++bit;
    }
  }
  return hash;
}

// ── pHash (perceptual hash via DCT) ─────────────────────────────────────────

auto ImageCullingService::ComputePHash(const cv::Mat& gray, int hash_size)
    -> ImageHash {
  // Resize to a power-of-two friendly size for DCT.
  cv::Mat resized;
  cv::resize(gray, resized, cv::Size(32, 32), 0, 0, cv::INTER_AREA);

  // Convert to float for DCT.
  cv::Mat f32;
  resized.convertTo(f32, CV_32F);

  // Perform 2D DCT.
  cv::Mat dct;
  cv::dct(f32, dct);

  // Extract the top-left hash_size×hash_size low-frequency coefficients.
  cv::Mat low_freq = dct(cv::Rect(0, 0, hash_size, hash_size));

  // Compute the mean of the low-frequency coefficients (excluding DC).
  double sum = 0.0;
  int count = 0;
  for (int row = 0; row < hash_size; ++row) {
    for (int col = 0; col < hash_size; ++col) {
      if (row == 0 && col == 0) continue;  // skip DC component
      sum += low_freq.at<float>(row, col);
      ++count;
    }
  }
  double mean = (count > 0) ? sum / count : 0.0;

  // Build hash: each coefficient > mean → 1, else 0.
  ImageHash hash = 0;
  int bit = 0;
  for (int row = 0; row < hash_size; ++row) {
    for (int col = 0; col < hash_size; ++col) {
      if (row == 0 && col == 0) continue;
      if (low_freq.at<float>(row, col) > mean) {
        hash |= (static_cast<ImageHash>(1) << bit);
      }
      ++bit;
    }
  }
  return hash;
}

// ── Sharpness (Laplacian variance) ──────────────────────────────────────────

auto ImageCullingService::ComputeSharpness(const cv::Mat& gray) -> double {
  cv::Mat laplacian;
  cv::Laplacian(gray, laplacian, CV_64F);

  cv::Scalar mean, stddev;
  cv::meanStdDev(laplacian, mean, stddev);

  double variance = stddev.val[0] * stddev.val[0];
  return variance;
}

// ── Center-focus weight ─────────────────────────────────────────────────────

auto ImageCullingService::ComputeCenterFocus(const cv::Mat& gray) -> double {
  int width  = gray.cols;
  int height = gray.rows;

  // Define center region: middle 50% of the image.
  int cx_start = width / 4;
  int cx_end   = width - cx_start;
  int cy_start = height / 4;
  int cy_end   = height - cy_start;

  if (cx_end <= cx_start || cy_end <= cy_start) {
    return 0.5;  // degenerate — no meaningful center
  }

  cv::Rect center_roi(cx_start, cy_start, cx_end - cx_start, cy_end - cy_start);
  cv::Mat  center_region = gray(center_roi);

  double overall_sharpness = ComputeSharpness(gray);
  double center_sharpness  = ComputeSharpness(center_region);

  if (overall_sharpness <= 0.0) {
    return 0.0;
  }

  // Ratio of center sharpness to overall sharpness, clamped.
  double ratio = center_sharpness / overall_sharpness;
  return Clamp01(ratio);
}

// ── Exposure quality score ──────────────────────────────────────────────────

auto ImageCullingService::ComputeExposureScore(const cv::Mat& gray) -> double {
  // Build a 256-bin histogram.
  int hist_size = 256;
  float range[] = {0, 256};
  const float* hist_range = {range};
  cv::Mat hist;
  cv::calcHist(&gray, 1, nullptr, cv::Mat(), hist, 1, &hist_size, &hist_range,
               true, false);

  double total_pixels = gray.total();
  if (total_pixels <= 0.0) return 0.0;

  // Count pixels in shadow (0–15) and highlight (240–255) regions.
  double shadow_count = 0.0;
  double highlight_count = 0.0;
  for (int i = 0; i <= 15; ++i) {
    shadow_count += hist.at<float>(i);
  }
  for (int i = 240; i <= 255; ++i) {
    highlight_count += hist.at<float>(i);
  }

  double shadow_frac   = shadow_count / total_pixels;
  double highlight_frac = highlight_count / total_pixels;

  // Penalize both extremes. Score = 1.0 when both are 0; 0.0 when either is 1.0.
  double shadow_penalty   = std::min(1.0, shadow_frac / 0.25);
  double highlight_penalty = std::min(1.0, highlight_frac / 0.25);

  double score = 1.0 - (shadow_penalty + highlight_penalty) * 0.5;
  return Clamp01(score);
}

// ── Hamming distance ────────────────────────────────────────────────────────

auto ImageCullingService::HammingDistance(ImageHash a, ImageHash b) -> int {
  return PopCount(a ^ b);
}

// ── Composite score ─────────────────────────────────────────────────────────

auto ImageCullingService::ComputeCompositeScore(
    const ImageQualityMetrics& metrics,
    double uniqueness_bonus,
    const ImageCullingOptions& options) -> double {
  // Normalize sharpness: a Laplacian variance of ~500 is "very sharp".
  double sharpness_norm = Clamp01(metrics.sharpness / 500.0);

  double score = options.sharpness_weight    * sharpness_norm +
                 options.exposure_weight     * metrics.exposure_score +
                 options.center_focus_weight * metrics.center_focus +
                 options.uniqueness_weight   * uniqueness_bonus;

  return Clamp01(score);
}

// ── AnalyzeSingle ───────────────────────────────────────────────────────────

auto ImageCullingService::AnalyzeSingle(const std::filesystem::path& path,
                                         const ImageCullingOptions& options,
                                         ImageCullingEntry& entry) -> bool {
  entry.path = path;
  entry.analyzed = false;
  entry.error.clear();

  // Determine file size.
  std::error_code ec;
  entry.file_size = static_cast<int64_t>(std::filesystem::file_size(path, ec));
  if (ec) {
    entry.file_size = 0;
  }

  // Load image.
  cv::Mat gray = LoadAnalysisImage(path);
  if (gray.empty()) {
    entry.error = "Failed to load image: " + path.string();
    return false;
  }

  // Compute perceptual hashes.
  try {
    entry.hash.dhash = ComputeDHash(gray);
    entry.hash.phash = ComputePHash(gray);
    entry.hash.hash_valid = true;
  } catch (const std::exception& e) {
    entry.error = std::string("Hash computation failed: ") + e.what();
    return false;
  }

  // Compute sharpness.
  entry.quality.sharpness = ComputeSharpness(gray);
  entry.quality.is_blurry = (entry.quality.sharpness < options.blur_threshold);

  // Compute center focus.
  entry.quality.center_focus = ComputeCenterFocus(gray);

  // Compute exposure score.
  entry.quality.exposure_score = ComputeExposureScore(gray);

  // Determine over/under exposure flags.
  // Count pixels in extreme bins for flagging.
  cv::Mat hist;
  int hist_size = 256;
  float range[] = {0, 256};
  const float* hist_range = {range};
  cv::calcHist(&gray, 1, nullptr, cv::Mat(), hist, 1, &hist_size, &hist_range,
               true, false);

  double total = gray.total();
  double shadow = 0.0, highlight = 0.0;
  for (int i = 0; i <= 10; ++i) shadow += hist.at<float>(i);
  for (int i = 245; i <= 255; ++i) highlight += hist.at<float>(i);

  entry.quality.is_underexposed =
      (shadow / total) > options.underexposure_threshold;
  entry.quality.is_overexposed =
      (highlight / total) > options.overexposure_threshold;

  entry.analyzed = true;
  return true;
}

// ── GroupSimilar ────────────────────────────────────────────────────────────

auto ImageCullingService::GroupSimilar(
    std::vector<ImageCullingEntry>& entries,
    int hamming_threshold) -> std::vector<ImageSimilarityGroup> {
  std::vector<ImageSimilarityGroup> groups;
  std::vector<bool> assigned(entries.size(), false);

  for (size_t i = 0; i < entries.size(); ++i) {
    if (assigned[i] || !entries[i].hash.hash_valid) continue;

    ImageSimilarityGroup group;
    group.indices.push_back(i);
    group.representative_hash = entries[i].hash.phash;
    assigned[i] = true;

    for (size_t j = i + 1; j < entries.size(); ++j) {
      if (assigned[j] || !entries[j].hash.hash_valid) continue;

      int dist = HammingDistance(entries[i].hash.phash, entries[j].hash.phash);
      if (dist <= hamming_threshold) {
        group.indices.push_back(j);
        assigned[j] = true;
      }
    }

    if (group.indices.size() > 1) {
      groups.push_back(std::move(group));
    }
    // Singular groups (only one image) are not reported — they are unique.
  }

  return groups;
}

// ── GenerateSuggestions ─────────────────────────────────────────────────────

auto ImageCullingService::GenerateSuggestions(
    const std::vector<ImageCullingEntry>& entries,
    const std::vector<ImageSimilarityGroup>& groups,
    const ImageCullingOptions& options)
    -> std::vector<ImageCullingSuggestion> {
  std::vector<ImageCullingSuggestion> suggestions;
  suggestions.reserve(entries.size());

  // Build a set of indices that belong to any similarity group.
  std::unordered_set<size_t> grouped_indices;
  for (const auto& group : groups) {
    for (size_t idx : group.indices) {
      grouped_indices.insert(idx);
    }
  }

  for (size_t i = 0; i < entries.size(); ++i) {
    const auto& entry = entries[i];
    ImageCullingSuggestion sug;
    sug.entry_index = i;

    if (!entry.analyzed) {
      sug.suggestion = CullingSuggestion::kUnknown;
      sug.reason = entry.error.empty() ? "Not analyzed" : entry.error;
      sug.score = 0.0;
      suggestions.push_back(sug);
      continue;
    }

    // Compute base score.
    bool in_group = (grouped_indices.count(i) > 0);
    double uniqueness_bonus = in_group ? 0.0 : 1.0;
    sug.score = ComputeCompositeScore(entry.quality, uniqueness_bonus, options);

    // Determine suggestion.
    bool is_failure = entry.quality.is_blurry ||
                      entry.quality.is_overexposed ||
                      entry.quality.is_underexposed;

    if (is_failure && sug.score < options.min_keep_score) {
      sug.suggestion = CullingSuggestion::kCull;
      std::string reasons;
      if (entry.quality.is_blurry) reasons += "blurry ";
      if (entry.quality.is_overexposed) reasons += "overexposed ";
      if (entry.quality.is_underexposed) reasons += "underexposed ";
      sug.reason = reasons;
    } else if (in_group) {
      // In a similarity group: find the best in the group to keep.
      const auto* group_ptr = static_cast<const ImageSimilarityGroup*>(nullptr);
      for (const auto& g : groups) {
        for (size_t idx : g.indices) {
          if (idx == i) {
            group_ptr = &g;
            break;
          }
        }
        if (group_ptr) break;
      }

      if (group_ptr) {
        // Find the best entry in this group.
        size_t best_idx = i;
        double best_score = sug.score;
        for (size_t idx : group_ptr->indices) {
          double s = ComputeCompositeScore(
              entries[idx].quality,
              0.0,  // no uniqueness bonus within a group
              options);
          // Tie-break: prefer larger file size.
          if (s > best_score ||
              (std::abs(s - best_score) < 0.01 &&
               options.prefer_larger_file &&
               entries[idx].file_size > entries[best_idx].file_size)) {
            best_score = s;
            best_idx = idx;
          }
        }

        if (i == best_idx) {
          sug.suggestion = CullingSuggestion::kKeep;
          sug.reason = "Best in similarity group";
        } else {
          sug.suggestion = CullingSuggestion::kCull;
          sug.reason = "Duplicate; best is " + entries[best_idx].path.filename().string();
        }
      }
    } else if (sug.score >= 0.6) {
      sug.suggestion = CullingSuggestion::kKeep;
      sug.reason = "Good quality";
    } else if (sug.score >= 0.35) {
      sug.suggestion = CullingSuggestion::kReview;
      sug.reason = "Borderline quality";
    } else {
      sug.suggestion = CullingSuggestion::kCull;
      sug.reason = "Low quality";
    }

    suggestions.push_back(sug);
  }

  return suggestions;
}

// ── Analyze (main entry) ────────────────────────────────────────────────────

auto ImageCullingService::Analyze(
    const std::vector<std::filesystem::path>& paths,
    const ImageCullingOptions& options,
    ImageCullingProgressCallback on_progress) -> ImageCullingResult {
  ImageCullingResult result;
  result.entries.reserve(paths.size());

  ImageCullingProgress progress;
  progress.total = paths.size();

  // Phase 1: analyze each image individually.
  for (size_t i = 0; i < paths.size(); ++i) {
    ImageCullingEntry entry;
    bool ok = AnalyzeSingle(paths[i], options, entry);
    if (!ok) {
      ++progress.errors;
    }
    ++progress.processed;
    result.entries.push_back(std::move(entry));

    if (on_progress) {
      on_progress(progress);
    }
  }

  // Phase 2: group similar images.
  result.groups = GroupSimilar(result.entries,
                                options.similarity_hamming_threshold);

  // Phase 3: generate suggestions.
  result.suggestions = GenerateSuggestions(result.entries, result.groups,
                                            options);

  // Tally suggestion counts.
  for (const auto& sug : result.suggestions) {
    switch (sug.suggestion) {
      case CullingSuggestion::kKeep:   ++result.keep_count;   break;
      case CullingSuggestion::kCull:   ++result.cull_count;   break;
      case CullingSuggestion::kReview: ++result.review_count; break;
      default: break;
    }
  }

  return result;
}

}  // namespace alcedo::app