//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <vector>
#include <functional>

namespace alcedo {
namespace inpaint {

/// Inpainting algorithm selection.
enum class InpaintMethod {
  CLONE_HEAL,          ///< Clone healing with feathering
  FAST_MARCHING,       ///< Fast marching inpainting (simplified)
  EXEMPLAR_PRIORITY,   ///< Exemplar-based patch inpainting with priority filling
  CONTENT_AWARE        ///< Content-aware fill with best matching patch
};

/// Image pixel status for inpainting process.
enum class PixelStatus : uint8_t {
  KNOWN,       ///< Pixel contains valid known data
  UNKNOWN,     ///< Pixel is in the mask (needs inpainting)
  FRONTIER     ///< Pixel is on the frontier between known/unknown
};

/// Options controlling inpainting behavior.
struct InpaintOptions {
  int patch_size = 9;            ///< Patch size for patch-based methods (odd number)
  float feather_radius = 2.0f;   ///< Feathering radius for clone healing
  bool multi_threaded = true;    ///< Enable multi-threading for large images
  int num_threads = 0;           ///< Number of threads (0 = hardware_concurrency)
  float search_step = 1.0f;      ///< Search step for patch matching
};

/// Clone healing: copy pixels from source region to target mask with feathering.
///
/// @param image          Input/output image, row-major, 32-bit float [0..1].
/// @param width          Image width in pixels.
/// @param height         Image height in pixels.
/// @param channels       Number of channels (1 = grayscale, 3 = RGB).
/// @param mask           Binary mask: 1.0 = area to inpaint, 0.0 = original. Same size as image.
/// @param source_x0      Source region top-left x coordinate.
/// @param source_y0      Source region top-left y coordinate.
/// @param target_x0      Target region top-left x coordinate.
/// @param target_y0      Target region top-left y coordinate.
/// @param region_width   Width of source/target region.
/// @param region_height  Height of source/target region.
/// @param options        Inpainting options (feather_radius used here).
void clone_heal(std::vector<float>& image,
                int width,
                int height,
                int channels,
                const std::vector<float>& mask,
                int source_x0,
                int source_y0,
                int target_x0,
                int target_y0,
                int region_width,
                int region_height,
                const InpaintOptions& options = InpaintOptions());

/// Clone healing with transformation (supports rotation/scale).
///
/// @param image          Input/output image, row-major, 32-bit float [0..1].
/// @param width          Image width in pixels.
/// @param height         Image height in pixels.
/// @param channels       Number of channels (1 = grayscale, 3 = RGB).
/// @param mask           Binary mask: 1.0 = area to inpaint.
/// @param source_points  Source region three points for affine transform (for rotation/scale).
/// @param target_points  Target region corresponding three points.
/// @param options        Inpainting options.
void clone_heal_transform(std::vector<float>& image,
                          int width,
                          int height,
                          int channels,
                          const std::vector<float>& mask,
                          const std::vector<std::pair<float, float>>& source_points,
                          const std::vector<std::pair<float, float>>& target_points,
                          const InpaintOptions& options = InpaintOptions());

/// Fast marching inpainting (simplified implementation).
/// Fills the masked region by propagating gradient from known pixels.
///
/// @param image      Input/output image, row-major, 32-bit float [0..1].
/// @param width      Image width in pixels.
/// @param height     Image height in pixels.
/// @param channels   Number of channels (1, 3).
/// @param mask       Binary mask: 1.0 = area to inpaint, 0.0 = original.
/// @param options    Inpainting options.
void fast_marching_inpaint(std::vector<float>& image,
                           int width,
                           int height,
                           int channels,
                           const std::vector<float>& mask,
                           const InpaintOptions& options = InpaintOptions());

/// Exemplar-based inpainting with priority-based filling.
/// Uses confidence and gradient terms to determine fill order.
///
/// @param image      Input/output image, row-major, 32-bit float [0..1].
/// @param width      Image width in pixels.
/// @param height     Image height in pixels.
/// @param channels   Number of channels (1, 3).
/// @param mask       Binary mask: 1.0 = area to inpaint, 0.0 = original.
/// @param options    Inpainting options (patch_size used here).
void exemplar_inpaint(std::vector<float>& image,
                       int width,
                       int height,
                       int channels,
                       const std::vector<float>& mask,
                       const InpaintOptions& options = InpaintOptions());

/// Content-aware fill based on best matching patch search.
/// Similar to exemplar but uses different search strategy.
///
/// @param image      Input/output image, row-major, 32-bit float [0..1].
/// @param width      Image width in pixels.
/// @param height     Image height in pixels.
/// @param channels   Number of channels (1, 3).
/// @param mask       Binary mask: 1.0 = area to inpaint, 0.0 = original.
/// @param options    Inpainting options.
void content_aware_fill(std::vector<float>& image,
                         int width,
                         int height,
                         int channels,
                         const std::vector<float>& mask,
                         const InpaintOptions& options = InpaintOptions());

/// Main entry point that selects method based on parameters.
///
/// @param image      Input/output image, row-major, 32-bit float [0..1].
/// @param width      Image width in pixels.
/// @param height     Image height in pixels.
/// @param channels   Number of channels (1, 3).
/// @param mask       Binary mask: 1.0 = area to inpaint, 0.0 = original.
/// @param method     Inpainting algorithm to use.
/// @param options    Inpainting options.
void inpaint_image(std::vector<float>& image,
                   int width,
                   int height,
                   int channels,
                   const std::vector<float>& mask,
                   InpaintMethod method,
                   const InpaintOptions& options = InpaintOptions());

/// Calculate bounding box of mask region.
/// @param mask   Input mask.
/// @param width  Image width.
/// @param height Image height.
/// @param threshold Mask threshold to consider as filled.
/// @return (xmin, ymin, xmax, ymax)
std::tuple<int, int, int, int> mask_bounding_box(const std::vector<float>& mask,
                                                  int width,
                                                  int height,
                                                  float threshold = 0.5f);

}  // namespace inpaint
}  // namespace alcedo
