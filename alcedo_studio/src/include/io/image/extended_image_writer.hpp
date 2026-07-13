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
namespace io {

/// Supported export formats
enum class ExportFormat {
    JPEG,
    PNG,
    TIFF,
    JPEGXL,      /// JPEG XL - next-gen format, better compression
    WebP,        /// WebP - good compression, animation support
    HEIF,        /// HEIF/HEIC - Apple/iOS format
    BMP,
    OpenEXR      /// OpenEXR - HDR format
};

/// Export format capabilities
struct FormatCapabilities {
    bool supports_alpha{false};
    bool supports_16bit{false};
    bool supports_animation{false};
    bool supports_lossless{false};
    bool supports_hdr{false};
    int max_width{0};
    int max_height{0};
    std::vector<int> supported_quality_range;
};

/// Export parameters for a specific format
struct ExportParams {
    ExportFormat format = ExportFormat::JPEG;
    int quality = 95;                 /// JPEG/WebP quality [1-100]
    bool lossless = false;            /// Use lossless mode (JPEG XL, WebP)
    int compression_level = 6;        /// PNG/JPEG XL compression level

    /// Resolution/DPI settings
    int dpi_x = 72;
    int dpi_y = 72;

    /// ICC profile embedding
    bool embed_icc_profile = true;
    std::string icc_profile_path;

    /// Metadata embedding
    bool embed_exif = true;
    bool embed_xmp = true;

    /// Tiled/chunked export (for large images)
    bool use_tiling = false;
    int tile_size = 512;

    /// Output dimensions (resize during export)
    int output_width = 0;             /// 0 = keep original
    int output_height = 0;
    float scale_factor = 1.0f;

    /// Color space conversion during export
    std::string output_colorspace;    /// e.g., "sRGB", "Rec709", "Rec2020", "P3-D65"

    /// HDR-specific settings
    float hdr_max_luminance = 1000.0f; /// Max luminance for HDR export (nits)

    /// WebP/JPEG XL specific
    int effort_level = 5;             /// Encoding effort [1-9], higher = slower/better
    int distance = 1.0f;              /// JPEG XL distance (0=lossless, higher=more compression)

    /// Animation settings (WebP animation)
    int animation_fps = 30;
    int animation_loop_count = 0;     /// 0 = infinite

    /// Get capabilities for current format
    static auto GetCapabilities(ExportFormat format) -> FormatCapabilities;
};

/// Extended image writer supporting modern formats
class ExtendedImageWriter {
public:
    /// Write image to file with given format and parameters
    static auto WriteToFile(
        const std::shared_ptr<ImageBuffer>& image,
        const std::string& path,
        const ExportParams& params
    ) -> bool;

    /// Write image to memory buffer
    static auto WriteToBuffer(
        const std::shared_ptr<ImageBuffer>& image,
        std::vector<uint8_t>& buffer,
        const ExportParams& params
    ) -> bool;

    /// Get available formats on this system
    static auto GetAvailableFormats() -> std::vector<ExportFormat>;

    /// Check if a format is available
    static auto IsFormatAvailable(ExportFormat format) -> bool;

    /// Get format file extension
    static auto GetFileExtension(ExportFormat format) -> std::string;

    /// Get format display name
    static auto GetFormatName(ExportFormat format) -> std::string;

    /// Get recommended quality for format
    static auto GetRecommendedQuality(ExportFormat format) -> int;

    /// Estimate output file size
    static auto EstimateFileSize(
        const std::shared_ptr<ImageBuffer>& image,
        const ExportParams& params
    ) -> size_t;

private:
    /// JPEG implementation
    static auto WriteJPEG(
        const std::shared_ptr<ImageBuffer>& image,
        const std::string& path,
        const ExportParams& params
    ) -> bool;

    /// PNG implementation
    static auto WritePNG(
        const std::shared_ptr<ImageBuffer>& image,
        const std::string& path,
        const ExportParams& params
    ) -> bool;

    /// TIFF implementation
    static auto WriteTIFF(
        const std::shared_ptr<ImageBuffer>& image,
        const std::string& path,
        const ExportParams& params
    ) -> bool;

    /// JPEG XL implementation (uses libjxl)
    static auto WriteJPEGXL(
        const std::shared_ptr<ImageBuffer>& image,
        const std::string& path,
        const ExportParams& params
    ) -> bool;

    /// WebP implementation (uses libwebp)
    static auto WriteWebP(
        const std::shared_ptr<ImageBuffer>& image,
        const std::string& path,
        const ExportParams& params
    ) -> bool;

    /// HEIF implementation
    static auto WriteHEIF(
        const std::shared_ptr<ImageBuffer>& image,
        const std::string& path,
        const ExportParams& params
    ) -> bool;

    /// OpenEXR implementation
    static auto WriteEXR(
        const std::shared_ptr<ImageBuffer>& image,
        const std::string& path,
        const ExportParams& params
    ) -> bool;
};

/// Export format detection utility
class FormatDetector {
public:
    /// Detect format from file extension
    static auto DetectFromExtension(const std::string& extension) -> ExportFormat;

    /// Detect format from file content
    static auto DetectFromFile(const std::string& path) -> ExportFormat;

    /// Get all supported extensions
    static auto GetSupportedExtensions() -> std::vector<std::string>;
};

/// WebP animation writer
class WebPAnimationWriter {
public:
    WebPAnimationWriter();
    ~WebPAnimationWriter();

    /// Initialize animation with dimensions and config
    auto Initialize(int width, int height, int fps = 30, int loop_count = 0) -> bool;

    /// Add a frame to the animation
    auto AddFrame(
        const std::shared_ptr<ImageBuffer>& frame,
        int duration_ms = 0  /// 0 = use fps-based duration
    ) -> bool;

    /// Finalize and write animation to file
    auto Finalize(const std::string& path, int quality = 90) -> bool;

    /// Get current frame count
    auto GetFrameCount() const -> int;

private:
    class Impl;
    std::unique_ptr<Impl> impl_;
};

/// JPEG XL multi-layer writer (for editing history)
class JPEGXLMultiLayerWriter {
public:
    JPEGXLMultiLayerWriter();
    ~JPEGXLMultiLayerWriter();

    /// Initialize with base image
    auto Initialize(const std::shared_ptr<ImageBuffer>& base_image) -> bool;

    /// Add an adjustment layer (non-destructive)
    auto AddLayer(
        const std::string& layer_name,
        const std::vector<uint8_t>& layer_data
    ) -> bool;

    /// Add metadata layer
    auto AddMetadata(const std::string& metadata_json) -> bool;

    /// Write to file
    auto Write(const std::string& path, bool lossless = true) -> bool;

private:
    class Impl;
    std::unique_ptr<Impl> impl_;
};

}  // namespace io
}  // namespace alcedo