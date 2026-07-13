//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <memory>
#include <string>
#include <vector>

#include "image/image_buffer.hpp"

namespace alcedo {

/// Collage layout types
enum class CollageLayout {
    Grid2x2,       /// 2x2 grid
    Grid3x3,       /// 3x3 grid
    Grid4x4,       /// 4x4 grid
    Magazine,      /// Magazine style (asymmetric)
    Polaroid,      /// Polaroid-style scattered
    StripHorizontal, /// Horizontal strip
    StripVertical,   /// Vertical strip
    Custom         /// User-defined positions
};

/// Single frame/cell in a collage
struct CollageFrame {
    int x{0};
    int y{0};
    int width{0};
    int height{0};
    int border_radius{0};
    int border_width{0};
    uint8_t border_color[4] = {255, 255, 255, 255};
    float rotation{0.0f};      /// Rotation in degrees
    float opacity{1.0f};
    std::string image_path;
    int image_x{0};            /// Image offset within frame
    int image_y{0};
    float image_scale{1.0f};
};

/// Collage configuration
struct CollageConfig {
    CollageLayout layout = CollageLayout::Grid2x2;
    int canvas_width{1920};
    int canvas_height{1080};
    uint8_t background_color[4] = {30, 30, 30, 255};
    int spacing{10};           /// Gap between frames
    int margin{20};            /// Outer margin
    int border_radius{0};      /// Default border radius for all frames
    int border_width{0};       /// Default border width
    uint8_t border_color[4] = {255, 255, 255, 255};

    /// For custom layouts
    std::vector<CollageFrame> custom_frames;

    /// Export settings
    std::string output_path;
    std::string output_format{"JPEG"};
    int output_quality{95};
};

/// Collage maker - Create multi-image compositions
class CollageMaker {
public:
    CollageMaker();
    ~CollageMaker();

    /// Create a new collage with given configuration
    auto Create(const CollageConfig& config) -> bool;

    /// Add an image to the collage
    auto AddImage(const std::string& image_path, int frame_index = -1) -> bool;

    /// Remove an image from a frame
    auto RemoveImage(int frame_index) -> bool;

    /// Set frame properties
    auto SetFrameProperties(int frame_index, const CollageFrame& frame) -> bool;

    /// Adjust image position/scale within a frame
    auto AdjustImage(int frame_index, int offset_x, int offset_y, float scale) -> bool;

    /// Rotate an image within a frame
    auto RotateImage(int frame_index, float degrees) -> bool;

    /// Generate preview at specified resolution
    auto GeneratePreview(int max_width = 800) -> std::shared_ptr<ImageBuffer>;

    /// Render final collage at full resolution
    auto Render() -> std::shared_ptr<ImageBuffer>;

    /// Export to file
    auto Export(const std::string& path, const std::string& format = "JPEG", int quality = 95) -> bool;

    /// Get number of frames in current layout
    auto GetFrameCount() const -> int;

    /// Get available layout presets
    static auto GetAvailableLayouts() -> std::vector<CollageLayout>;

    /// Get layout name
    static auto GetLayoutName(CollageLayout layout) -> std::string;

    /// Calculate frame positions for a layout
    static auto CalculateLayoutFrames(CollageLayout layout, int canvas_width, int canvas_height,
                                       int spacing, int margin) -> std::vector<CollageFrame>;

private:
    class Impl;
    std::unique_ptr<Impl> impl_;
};

/// ROI (Region of Interest) rendering configuration
struct ROIConfig {
    bool enabled{false};
    int x{0};
    int y{0};
    int width{0};
    int height{0};
    float scale_x{1.0f};  /// Output scale factor
    float scale_y{1.0f};

    /// Reference dimensions for scale calculation
    int reference_width{0};
    int reference_height{0};

    /// Check if ROI is valid
    auto IsValid() const -> bool {
        return enabled && width > 0 && height > 0;
    }

    /// Calculate effective dimensions
    auto GetEffectiveDimensions() const -> std::pair<int, int> {
        if (!enabled) return {reference_width, reference_height};
        return {width, height};
    }

    /// Calculate render scale for a given reference size
    auto CalculateRenderScale(int ref_width, int ref_height) const -> std::pair<float, float> {
        if (!enabled || ref_width <= 0 || ref_height <= 0) return {1.0f, 1.0f};

        const float sx = static_cast<float>(width) / static_cast<float>(ref_width);
        const float sy = static_cast<float>(height) / static_cast<float>(ref_height);
        return {sx, sy};
    }
};

/// ROI Rendering Manager - Optimizes rendering for visible region only
class ROIRenderManager {
public:
    ROIRenderManager();
    ~ROIRenderManager();

    /// Set current ROI configuration
    void SetROI(const ROIConfig& config);

    /// Get current ROI configuration
    auto GetROI() const -> const ROIConfig&;

    /// Enable/disable ROI rendering
    void SetEnabled(bool enabled);

    /// Check if ROI rendering is active
    auto IsEnabled() const -> bool;

    /// Update ROI from viewport (typically called during zoom/pan)
    void UpdateFromViewport(int viewport_x, int viewport_y,
                             int viewport_width, int viewport_height,
                             float zoom_level);

    /// Check if a point is inside the ROI
    auto IsPointInROI(int x, int y) const -> bool;

    /// Check if a region overlaps with the ROI
    auto IsRegionInROI(int x, int y, int width, int height) const -> bool;

    /// Calculate tile boundaries for ROI-aware rendering
    auto CalculateTileBounds(int full_width, int full_height,
                              int tile_size) const -> std::vector<std::tuple<int, int, int, int>>;

    /// Get effective render dimensions considering ROI
    auto GetEffectiveRenderDimensions(int full_width, int full_height) const -> std::pair<int, int>;

    /// Get scale factors for rendering
    auto GetRenderScale(int full_width, int full_height) const -> std::pair<float, float>;

private:
    class Impl;
    std::unique_ptr<Impl> impl_;
};

/// Batch denoise configuration
struct BatchDenoiseConfig {
    enum class Method {
        BM3D,      /// BM3D denoising (good quality/speed balance)
        Wavelet,   /// Wavelet-based denoising
        Bilateral, /// Bilateral filter (fast)
        NLM        /// Non-local means (slow but quality)
    };

    Method method = Method::BM3D;
    float strength{0.1f};        /// Denoising strength [0, 1]
    float chroma_strength{0.15f}; /// Chroma denoising strength (usually higher)
    bool separate_chroma{true};  /// Apply stronger denoising to chroma
    int num_threads{0};          /// Number of threads (0 = auto)

    /// For BM3D
    int bm3d_block_size{8};
    int bm3d_search_window{16};
    int bm3d_max_blocks{16};
};

/// Batch denoise processor
class BatchDenoiseProcessor {
public:
    /// Denoise a single image
    static auto DenoiseImage(
        std::shared_ptr<ImageBuffer> input,
        const BatchDenoiseConfig& config
    ) -> std::shared_ptr<ImageBuffer>;

    /// Denoise with separate YCbCr channels
    static auto DenoiseYCbCr(
        const std::vector<float>& y_in,
        const std::vector<float>& cb_in,
        const std::vector<float>& cr_in,
        std::vector<float>& y_out,
        std::vector<float>& cb_out,
        std::vector<float>& cr_out,
        int width,
        int height,
        const BatchDenoiseConfig& config
    ) -> void;

    /// Get recommended strength for ISO value
    static auto GetRecommendedStrength(int iso) -> float;

    /// Estimate processing time for given dimensions
    static auto EstimateProcessingTime(int width, int height, const BatchDenoiseConfig& config) -> float;
};

}  // namespace alcedo