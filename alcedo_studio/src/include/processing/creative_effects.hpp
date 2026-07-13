//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace alcedo {
namespace effects {

// ==========================================================================
//  Common blend modes
// ==========================================================================

/// Blend mode for compositing operations.
enum class BlendMode {
  SCREEN,       ///< Screen blend: 1 - (1-a)*(1-b)
  OVERLAY,      ///< Overlay blend: screen if a>0.5, multiply otherwise
  SOFT_LIGHT,   ///< Soft light blend (Photoshop-style)
  MULTIPLY,     ///< Multiply blend: a * b
  ADD,          ///< Additive blend: a + b
  NORMAL,       ///< Normal blend with alpha
};

// ==========================================================================
//  Section A: Glow effect (Orton-like)
// ==========================================================================

/// Glow effect parameters.
struct GlowParams {
  float intensity = 0.5f;       ///< Overall glow intensity [0..1]
  float radius = 20.0f;          ///< Base blur radius in pixels
  int   num_scales = 3;          ///< Number of blur scales (default 3)
  float scale_factor = 2.0f;     ///< Radius multiplier per scale
  float threshold = 0.0f;        ///< Highlights-only threshold [0..1], 0 = all pixels
  float threshold_smooth = 0.05f;///< Smooth transition width for threshold
  BlendMode blend_mode = BlendMode::SCREEN; ///< Blend mode for compositing
  bool  preserve_luminosity = false; ///< If true, preserve original luminosity
};

/// Apply glow (Orton-like) effect to an image.
///
/// Multi-scale Gaussian blur is applied to the input, then blended back
/// using the selected blend mode. A threshold can be used to restrict
/// the glow to highlights only.
///
/// @param input    Interleaved RGB input, row-major, [0..1].
/// @param output   Interleaved RGB output, row-major, [0..1].
/// @param width    Image width in pixels.
/// @param height   Image height in pixels.
/// @param params   Glow effect parameters.
void apply_glow(const std::vector<float>& input,
                std::vector<float>& output,
                int width,
                int height,
                const GlowParams& params);

// ==========================================================================
//  Section B: Lens flares
// ==========================================================================

/// Single anamorphic streak definition.
struct AnamorphicStreak {
  float length = 200.0f;       ///< Streak length in pixels
  float thickness = 2.0f;      ///< Streak thickness
  float intensity = 0.8f;      ///< Streak intensity [0..1]
  float angle = 0.0f;          ///< Streak angle in radians (0 = horizontal)
  float color_r = 1.0f;        ///< Streak color R
  float color_g = 1.0f;        ///< Streak color G
  float color_b = 1.0f;        ///< Streak color B
  float falloff = 2.0f;        ///< Falloff exponent along streak
};

/// Single ghost reflection definition.
struct GhostReflection {
  float distance = 50.0f;      ///< Distance from light source center
  float radius = 30.0f;        ///< Reflection circle radius
  float intensity = 0.5f;      ///< Intensity [0..1]
  float color_r = 1.0f;        ///< Reflection color R
  float color_g = 1.0f;        ///< Reflection color G
  float color_b = 1.0f;        ///< Reflection color B
  float falloff = 1.5f;        ///< Edge falloff exponent
  float angle = 0.0f;          ///< Angle offset from center-to-light axis
};

/// Ring flare parameters.
struct RingFlare {
  float inner_radius = 40.0f;  ///< Inner ring radius
  float outer_radius = 60.0f;  ///< Outer ring radius
  float intensity = 0.6f;      ///< Intensity [0..1]
  float color_r = 1.0f;        ///< Ring color R
  float color_g = 0.8f;        ///< Ring color G
  float color_b = 0.4f;        ///< Ring color B
  float falloff = 2.0f;        ///< Edge falloff exponent
  int   segments = 0;          ///< Number of arc segments (0 = full ring)
  float segment_angle = 0.0f;  ///< Angular gap between segments
};

/// Global lens flare parameters.
struct LensFlareParams {
  float light_x = 0.5f;           ///< Light source center X (normalized [0..1])
  float light_y = 0.5f;           ///< Light source center Y (normalized [0..1])
  float global_intensity = 1.0f;  ///< Global intensity multiplier
  float light_glow_radius = 20.0f;///< Central light glow radius
  float light_glow_intensity = 0.9f; ///< Central light glow intensity
  std::vector<AnamorphicStreak> streaks;     ///< Anamorphic streaks
  std::vector<GhostReflection>  ghosts;      ///< Ghost reflections
  bool  has_ring_flare = false;              ///< Enable ring flare
  RingFlare ring_flare;                      ///< Ring flare settings
  bool  screen_center_axis = true;   ///< Mirror reflections through center
};

/// Generate and apply lens flares to an image.
///
/// Creates anamorphic streaks, ghost reflections, and ring flares
/// centered on a light source position, composited additively.
///
/// @param input    Interleaved RGB input, row-major, [0..1].
/// @param output   Interleaved RGB output with flares added, row-major, [0..1].
/// @param width    Image width in pixels.
/// @param height   Image height in pixels.
/// @param params   Lens flare parameters.
void apply_lens_flare(const std::vector<float>& input,
                      std::vector<float>& output,
                      int width,
                      int height,
                      const LensFlareParams& params);

// ==========================================================================
//  Section C: Dehaze (contrast enhancement)
// ==========================================================================

/// Dehaze parameters.
struct DehazeParams {
  float strength = 0.5f;         ///< Dehaze strength [0..1], 0 = no change
  float atmospheric_light_r = 0.0f; ///< Manual atmospheric light R (0 = auto-estimate)
  float atmospheric_light_g = 0.0f; ///< Manual atmospheric light G (0 = auto-estimate)
  float atmospheric_light_b = 0.0f; ///< Manual atmospheric light B (0 = auto-estimate)
  float omega = 0.95f;           ///< Dark channel weight (keep some haze, typically 0.95)
  int   patch_size = 15;         ///< Patch size for dark channel computation
  float guided_filter_eps = 0.001f; ///< Regularization for guided filter
  int   guided_filter_radius = 60;  ///< Guided filter window radius
  float t0 = 0.1f;               ///< Minimum transmission to avoid division by zero
};

/// Apply dehazing to an image using dark channel prior.
///
/// Based on He et al. "Single Image Haze Removal Using Dark Channel Prior".
/// Steps: dark channel computation → atmospheric light estimation →
/// transmission map → guided filter refinement → scene radiance recovery.
///
/// @param input    Interleaved RGB input, row-major, [0..1].
/// @param output   Interleaved RGB dehazed output, row-major, [0..1].
/// @param width    Image width in pixels.
/// @param height   Image height in pixels.
/// @param params   Dehaze parameters.
void apply_dehaze(const std::vector<float>& input,
                  std::vector<float>& output,
                  int width,
                  int height,
                  const DehazeParams& params);

// ==========================================================================
//  Section D: Collage maker
// ==========================================================================

/// Layout preset types for collage.
enum class CollageLayout {
  GRID_2X2,         ///< 2x2 equal grid
  GRID_3X3,         ///< 3x3 equal grid
  HORIZONTAL_STRIP,  ///< Horizontal strip of images
  VERTICAL_STRIP,    ///< Vertical strip of images
  PYRAMID_3,        ///< 3-image pyramid (1 top, 2 bottom)
  PYRAMID_5,        ///< 5-image pyramid (1, 2, 2)
  SPIRAL_4,         ///< 4-image spiral layout
  FREE_FORM,        ///< Free-form layout with explicit coordinates
};

/// Single image placement in a collage.
struct CollageSlot {
  int   image_index = 0;       ///< Index into the input images array
  float x = 0.0f;              ///< Left position in output (normalized [0..1])
  float y = 0.0f;              ///< Top position in output (normalized [0..1])
  float w = 1.0f;              ///< Width in output (normalized [0..1])
  float h = 1.0f;              ///< Height in output (normalized [0..1])
  float border_radius = 0.0f;  ///< Rounded corner radius (normalized [0..1])
  float rotation = 0.0f;       ///< Rotation in radians around slot center
};

/// Collage parameters.
struct CollageParams {
  CollageLayout layout = CollageLayout::GRID_2X2; ///< Layout preset
  int   output_width = 1920;          ///< Output canvas width in pixels
  int   output_height = 1080;         ///< Output canvas height in pixels
  float spacing = 0.01f;              ///< Spacing between slots (normalized [0..1])
  float bg_r = 0.0f;                  ///< Background color R [0..1]
  float bg_g = 0.0f;                  ///< Background color G [0..1]
  float bg_b = 0.0f;                  ///< Background color B [0..1]
  float bg_a = 1.0f;                  ///< Background alpha [0..1] (0 = transparent)
  bool  fit_mode = true;              ///< true = fit within slot, false = fill (crop)
  bool  anti_alias = true;            ///< Enable anti-aliasing at slot edges
  std::vector<CollageSlot> free_slots; ///< Explicit slots for FREE_FORM layout
};

/// Compute slot placements for a given layout preset.
///
/// @param layout       Layout preset type.
/// @param num_images   Number of images to place in the collage.
/// @param spacing      Spacing between slots (normalized).
/// @return             Vector of CollageSlot with computed positions.
std::vector<CollageSlot> compute_layout_slots(CollageLayout layout,
                                               int num_images,
                                               float spacing);

/// Create a collage from multiple images.
///
/// Images are placed into slots according to the layout. Each input image
/// is resized to fit its slot. Spacing and background color are applied.
///
/// @param images       Vector of interleaved RGB images, row-major, [0..1].
/// @param widths       Vector of image widths.
/// @param heights      Vector of image heights.
/// @param output       Interleaved RGBA output, row-major, [0..1].
/// @param params       Collage parameters.
void create_collage(const std::vector<std::vector<float>>& images,
                    const std::vector<int>& widths,
                    const std::vector<int>& heights,
                    std::vector<float>& output,
                    const CollageParams& params);

// ==========================================================================
//  Section E: Watermark engine
// ==========================================================================

/// Watermark preset positions.
enum class WatermarkPosition {
  TOP_LEFT,
  TOP_CENTER,
  TOP_RIGHT,
  CENTER_LEFT,
  CENTER,
  CENTER_RIGHT,
  BOTTOM_LEFT,
  BOTTOM_CENTER,
  BOTTOM_RIGHT,
  CUSTOM,
};

/// Text watermark parameters.
struct TextWatermarkParams {
  std::string text;                 ///< Watermark text content
  float font_size = 24.0f;          ///< Font size in pixels (relative to image height)
  float color_r = 1.0f;             ///< Text color R [0..1]
  float color_g = 1.0f;             ///< Text color G [0..1]
  float color_b = 1.0f;             ///< Text color B [0..1]
  float opacity = 0.5f;             ///< Text opacity [0..1]
  float rotation = 0.0f;            ///< Rotation in radians
  WatermarkPosition position = WatermarkPosition::BOTTOM_RIGHT; ///< Preset position
  float custom_x = 0.0f;            ///< Custom X position (normalized [0..1])
  float custom_y = 0.0f;            ///< Custom Y position (normalized [0..1])
  float margin_x = 0.05f;           ///< Horizontal margin from edge (normalized)
  float margin_y = 0.05f;           ///< Vertical margin from edge (normalized)
  bool  shadow = true;              ///< Enable drop shadow for readability
  float shadow_opacity = 0.3f;      ///< Shadow opacity
  float shadow_offset_x = 2.0f;     ///< Shadow X offset in pixels
  float shadow_offset_y = 2.0f;     ///< Shadow Y offset in pixels
};

/// Image watermark parameters.
struct ImageWatermarkParams {
  std::vector<float> image_data;  ///< RGBA watermark image, row-major, [0..1]
  int   image_w = 0;              ///< Watermark image width
  int   image_h = 0;              ///< Watermark image height
  float opacity = 0.5f;           ///< Overall opacity [0..1]
  float scale = 1.0f;             ///< Scale factor (1.0 = original size relative to target)
  float rotation = 0.0f;          ///< Rotation in radians
  WatermarkPosition position = WatermarkPosition::BOTTOM_RIGHT; ///< Preset position
  float custom_x = 0.0f;          ///< Custom X position (normalized)
  float custom_y = 0.0f;          ///< Custom Y position (normalized)
  float margin_x = 0.05f;         ///< Horizontal margin (normalized)
  float margin_y = 0.05f;         ///< Vertical margin (normalized)
};

/// Tile/pattern watermark parameters.
struct TileWatermarkParams {
  std::vector<float> image_data;  ///< RGBA watermark tile, row-major, [0..1]
  int   tile_w = 0;               ///< Tile width in pixels
  int   tile_h = 0;               ///< Tile height in pixels
  float opacity = 0.15f;          ///< Tile opacity [0..1]
  float scale = 1.0f;             ///< Tile scale factor
  float rotation = 0.0f;          ///< Tile rotation in radians
  float spacing_x = 0.0f;         ///< Extra horizontal spacing between tiles (pixels)
  float spacing_y = 0.0f;         ///< Extra vertical spacing between tiles (pixels)
  float offset_x = 0.0f;          ///< Starting X offset in pixels
  float offset_y = 0.0f;          ///< Starting Y offset in pixels
};

/// Bitmap-based text rendering (simple fixed-size glyph rendering).
///
/// Renders a text string onto a float RGBA buffer. This is a simple
/// fixed-width bitmap font renderer — no external font library required.
/// Characters are rendered as 5x7 pixel glyphs on a 6x9 cell grid.
///
/// @param text        Text string to render (ASCII printable only).
/// @param output      Output RGBA buffer, row-major, [0..1].
/// @param buf_w       Output buffer width (in pixels).
/// @param buf_h       Output buffer height (in pixels).
/// @param color_r     Text color R [0..1].
/// @param color_g     Text color G [0..1].
/// @param color_b     Text color B [0..1].
void render_text_bitmap(const std::string& text,
                        std::vector<float>& output,
                        int buf_w,
                        int buf_h,
                        float color_r,
                        float color_g,
                        float color_b);

/// Apply a text watermark to an image.
///
/// @param input    Interleaved RGB input, row-major, [0..1].
/// @param output   Interleaved RGB output with watermark, row-major, [0..1].
/// @param width    Image width in pixels.
/// @param height   Image height in pixels.
/// @param params   Text watermark parameters.
void apply_text_watermark(const std::vector<float>& input,
                          std::vector<float>& output,
                          int width,
                          int height,
                          const TextWatermarkParams& params);

/// Apply an image watermark overlay to an image.
///
/// @param input    Interleaved RGB input, row-major, [0..1].
/// @param output   Interleaved RGB output with watermark, row-major, [0..1].
/// @param width    Image width in pixels.
/// @param height   Image height in pixels.
/// @param params   Image watermark parameters.
void apply_image_watermark(const std::vector<float>& input,
                           std::vector<float>& output,
                           int width,
                           int height,
                           const ImageWatermarkParams& params);

/// Apply a tile/pattern watermark to an image.
///
/// @param input    Interleaved RGB input, row-major, [0..1].
/// @param output   Interleaved RGB output with watermark, row-major, [0..1].
/// @param width    Image width in pixels.
/// @param height   Image height in pixels.
/// @param params   Tile watermark parameters.
void apply_tile_watermark(const std::vector<float>& input,
                          std::vector<float>& output,
                          int width,
                          int height,
                          const TileWatermarkParams& params);

/// Batch apply a text watermark to multiple images.
///
/// @param images       Vector of interleaved RGB images, row-major, [0..1].
/// @param outputs      Vector of output images (sized accordingly).
/// @param widths       Vector of image widths.
/// @param heights      Vector of image heights.
/// @param params       Text watermark parameters (same for all images).
void batch_text_watermark(const std::vector<std::vector<float>>& images,
                          std::vector<std::vector<float>>& outputs,
                          const std::vector<int>& widths,
                          const std::vector<int>& heights,
                          const TextWatermarkParams& params);

/// Batch apply an image watermark to multiple images.
///
/// @param images       Vector of interleaved RGB images, row-major, [0..1].
/// @param outputs      Vector of output images (sized accordingly).
/// @param widths       Vector of image widths.
/// @param heights      Vector of image heights.
/// @param params       Image watermark parameters (same for all images).
void batch_image_watermark(const std::vector<std::vector<float>>& images,
                           std::vector<std::vector<float>>& outputs,
                           const std::vector<int>& widths,
                           const std::vector<int>& heights,
                           const ImageWatermarkParams& params);

// ==========================================================================
//  Internal helpers (exposed for testing / reuse)
// ==========================================================================

/// Compute a Gaussian blur of a single-channel image.
///
/// @param input    Single-channel input, row-major, [0..1].
/// @param output   Single-channel output, row-major, [0..1].
/// @param width    Image width in pixels.
/// @param height   Image height in pixels.
/// @param sigma    Gaussian sigma in pixels.
void gaussian_blur(const std::vector<float>& input,
                   std::vector<float>& output,
                   int width,
                   int height,
                   float sigma);

/// Apply a blend mode to two pixel values.
///
/// @param base     Base pixel value [0..1].
/// @param blend    Blend pixel value [0..1].
/// @param mode     Blend mode to use.
/// @return         Blended pixel value.
float blend_pixel(float base, float blend, BlendMode mode);

/// Compute the dark channel of an RGB image.
///
/// @param input    Interleaved RGB input, row-major, [0..1].
/// @param output   Single-channel dark channel output, row-major, [0..1].
/// @param width    Image width in pixels.
/// @param height   Image height in pixels.
/// @param patch_size  Patch size for minimum filter.
void dark_channel(const std::vector<float>& input,
                  std::vector<float>& output,
                  int width,
                  int height,
                  int patch_size);

/// Guided filter for edge-aware smoothing.
///
/// @param guide    Guidance image (single-channel), row-major, [0..1].
/// @param input    Input image to filter (single-channel), row-major, [0..1].
/// @param output   Filtered output (single-channel), row-major, [0..1].
/// @param width    Image width in pixels.
/// @param height   Image height in pixels.
/// @param radius   Filter window radius.
/// @param eps      Regularization parameter.
void guided_filter(const std::vector<float>& guide,
                   const std::vector<float>& input,
                   std::vector<float>& output,
                   int width,
                   int height,
                   int radius,
                   float eps);

}  // namespace effects
}  // namespace alcedo