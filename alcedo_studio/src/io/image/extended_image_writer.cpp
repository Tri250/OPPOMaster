//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "io/image/extended_image_writer.hpp"

#include <algorithm>
#include <fstream>
#include <cstring>

namespace alcedo {
namespace io {

// ============================================================================
// FormatCapabilities Implementation
// ============================================================================

auto ExportParams::GetCapabilities(ExportFormat format) -> FormatCapabilities {
    FormatCapabilities caps;

    switch (format) {
        case ExportFormat::JPEG:
            caps.supports_alpha = false;
            caps.supports_16bit = false;
            caps.supports_animation = false;
            caps.supports_lossless = false;
            caps.supports_hdr = false;
            caps.max_width = 65500;
            caps.max_height = 65500;
            caps.supported_quality_range = {1, 100};
            break;

        case ExportFormat::PNG:
            caps.supports_alpha = true;
            caps.supports_16bit = true;
            caps.supports_animation = false;  // APNG not supported in basic impl
            caps.supports_lossless = true;
            caps.supports_hdr = false;
            caps.max_width = 0;  // No practical limit
            caps.max_height = 0;
            caps.supported_quality_range = {};  // PNG uses compression level
            break;

        case ExportFormat::TIFF:
            caps.supports_alpha = true;
            caps.supports_16bit = true;
            caps.supports_animation = false;
            caps.supports_lossless = true;
            caps.supports_hdr = true;
            caps.max_width = 0;
            caps.max_height = 0;
            break;

        case ExportFormat::JPEGXL:
            caps.supports_alpha = true;
            caps.supports_16bit = true;
            caps.supports_animation = true;  // JPEG XL supports animation
            caps.supports_lossless = true;
            caps.supports_hdr = true;  // JPEG XL supports HDR PQ/HLG
            caps.max_width = 0;
            caps.max_height = 0;
            caps.supported_quality_range = {1, 100};
            break;

        case ExportFormat::WebP:
            caps.supports_alpha = true;
            caps.supports_16bit = false;  // 8-bit only
            caps.supports_animation = true;
            caps.supports_lossless = true;
            caps.supports_hdr = false;
            caps.max_width = 16383;
            caps.max_height = 16383;
            caps.supported_quality_range = {1, 100};
            break;

        case ExportFormat::HEIF:
            caps.supports_alpha = true;
            caps.supports_16bit = true;  // 10/12-bit supported
            caps.supports_animation = false;
            caps.supports_lossless = false;  // HEIF always lossy
            caps.supports_hdr = true;  // HEIF supports HDR PQ
            caps.max_width = 0;
            caps.max_height = 0;
            break;

        case ExportFormat::BMP:
            caps.supports_alpha = false;
            caps.supports_16bit = false;
            caps.supports_animation = false;
            caps.supports_lossless = true;
            caps.supports_hdr = false;
            break;

        case ExportFormat::OpenEXR:
            caps.supports_alpha = true;
            caps.supports_16bit = true;  // Actually 16/32-bit float
            caps.supports_animation = false;
            caps.supports_lossless = true;
            caps.supports_hdr = true;
            caps.max_width = 0;
            caps.max_height = 0;
            break;
    }

    return caps;
}

// ============================================================================
// ExtendedImageWriter Implementation
// ============================================================================

auto ExtendedImageWriter::GetAvailableFormats() -> std::vector<ExportFormat> {
    return {
        ExportFormat::JPEG,
        ExportFormat::PNG,
        ExportFormat::TIFF,
        ExportFormat::JPEGXL,
        ExportFormat::WebP,
        ExportFormat::HEIF,
        ExportFormat::BMP,
        ExportFormat::OpenEXR
    };
}

auto ExtendedImageWriter::IsFormatAvailable(ExportFormat format) -> bool {
    // JPEG and PNG always available (stb_image_write)
    if (format == ExportFormat::JPEG || format == ExportFormat::PNG || format == ExportFormat::BMP) {
        return true;
    }

    // TIFF available via stb_image_write
    if (format == ExportFormat::TIFF) {
        return true;
    }

    // JPEG XL and WebP require external libraries
    // This would check for libjxl and libwebp availability
    // For now, return true (placeholder)
    return true;
}

auto ExtendedImageWriter::GetFileExtension(ExportFormat format) -> std::string {
    switch (format) {
        case ExportFormat::JPEG: return ".jpg";
        case ExportFormat::PNG: return ".png";
        case ExportFormat::TIFF: return ".tif";
        case ExportFormat::JPEGXL: return ".jxl";
        case ExportFormat::WebP: return ".webp";
        case ExportFormat::HEIF: return ".heic";
        case ExportFormat::BMP: return ".bmp";
        case ExportFormat::OpenEXR: return ".exr";
    }
    return ".jpg";
}

auto ExtendedImageWriter::GetFormatName(ExportFormat format) -> std::string {
    switch (format) {
        case ExportFormat::JPEG: return "JPEG";
        case ExportFormat::PNG: return "PNG";
        case ExportFormat::TIFF: return "TIFF";
        case ExportFormat::JPEGXL: return "JPEG XL";
        case ExportFormat::WebP: return "WebP";
        case ExportFormat::HEIF: return "HEIF/HEIC";
        case ExportFormat::BMP: return "BMP";
        case ExportFormat::OpenEXR: return "OpenEXR";
    }
    return "Unknown";
}

auto ExtendedImageWriter::GetRecommendedQuality(ExportFormat format) -> int {
    switch (format) {
        case ExportFormat::JPEG: return 92;
        case ExportFormat::WebP: return 90;
        case ExportFormat::JPEGXL: return 90;
        case ExportFormat::HEIF: return 90;
        default: return 0;  // Lossless formats don't use quality
    }
}

auto ExtendedImageWriter::WriteToFile(
    const std::shared_ptr<ImageBuffer>& image,
    const std::string& path,
    const ExportParams& params
) -> bool {
    if (!image || image->data.empty()) {
        return false;
    }

    switch (params.format) {
        case ExportFormat::JPEG:
            return WriteJPEG(image, path, params);

        case ExportFormat::PNG:
            return WritePNG(image, path, params);

        case ExportFormat::TIFF:
            return WriteTIFF(image, path, params);

        case ExportFormat::JPEGXL:
            return WriteJPEGXL(image, path, params);

        case ExportFormat::WebP:
            return WriteWebP(image, path, params);

        case ExportFormat::HEIF:
            return WriteHEIF(image, path, params);

        case ExportFormat::BMP:
            // BMP is trivial, just raw pixels
            return WritePNG(image, path, params);  // Placeholder

        case ExportFormat::OpenEXR:
            return WriteEXR(image, path, params);
    }

    return false;
}

auto ExtendedImageWriter::WriteToBuffer(
    const std::shared_ptr<ImageBuffer>& image,
    std::vector<uint8_t>& buffer,
    const ExportParams& params
) -> bool {
    // Placeholder - would use same format-specific writers to memory
    buffer.clear();
    return false;
}

auto ExtendedImageWriter::EstimateFileSize(
    const std::shared_ptr<ImageBuffer>& image,
    const ExportParams& params
) -> size_t {
    if (!image) return 0;

    const size_t pixel_count = static_cast<size_t>(image->width) * image->height;
    const size_t raw_size = pixel_count * image->channels;

    float compression_ratio = 1.0f;

    switch (params.format) {
        case ExportFormat::JPEG:
            compression_ratio = 0.1f + (params.quality / 100.0f) * 0.2f;
            break;

        case ExportFormat::PNG:
            compression_ratio = 0.3f;  // PNG typically 30-50% of raw
            break;

        case ExportFormat::JPEGXL:
            if (params.lossless) {
                compression_ratio = 0.4f;
            } else {
                compression_ratio = 0.05f + (params.quality / 100.0f) * 0.15f;
            }
            break;

        case ExportFormat::WebP:
            if (params.lossless) {
                compression_ratio = 0.25f;
            } else {
                compression_ratio = 0.08f + (params.quality / 100.0f) * 0.12f;
            }
            break;

        case ExportFormat::TIFF:
        case ExportFormat::OpenEXR:
            compression_ratio = 1.0f;  // Often uncompressed
            break;

        default:
            compression_ratio = 0.5f;
    }

    return static_cast<size_t>(raw_size * compression_ratio);
}

// ============================================================================
// Format-Specific Writers
// ============================================================================

auto ExtendedImageWriter::WriteJPEG(
    const std::shared_ptr<ImageBuffer>& image,
    const std::string& path,
    const ExportParams& params
) -> bool {
    // This would use stb_image_write or libjpeg
    // Placeholder implementation

    std::ofstream file(path, std::ios::binary);
    if (!file.is_open()) {
        return false;
    }

    // Placeholder: write minimal JPEG header
    // Actual implementation would use stbi_write_jpg

    file.close();
    return true;
}

auto ExtendedImageWriter::WritePNG(
    const std::shared_ptr<ImageBuffer>& image,
    const std::string& path,
    const ExportParams& params
) -> bool {
    // This would use stb_image_write or libpng
    // Placeholder implementation

    std::ofstream file(path, std::ios::binary);
    if (!file.is_open()) {
        return false;
    }

    file.close();
    return true;
}

auto ExtendedImageWriter::WriteTIFF(
    const std::shared_ptr<ImageBuffer>& image,
    const std::string& path,
    const ExportParams& params
) -> bool {
    // This would use stb_image_write or libtiff
    // Placeholder implementation

    std::ofstream file(path, std::ios::binary);
    if (!file.is_open()) {
        return false;
    }

    file.close();
    return true;
}

auto ExtendedImageWriter::WriteJPEGXL(
    const std::shared_ptr<ImageBuffer>& image,
    const std::string& path,
    const ExportParams& params
) -> bool {
    // JPEG XL requires libjxl
    // Key advantages over JPEG:
    // - Better compression at same quality
    // - True lossless mode
    // - HDR support (PQ, HLG)
    // - Animation support
    // - Non-destructive editing layers

    // Placeholder - would use JxlEncoder API

    std::ofstream file(path, std::ios::binary);
    if (!file.is_open()) {
        return false;
    }

    // Placeholder: write JPEG XL signature
    const uint8_t jxl_signature[] = {0xFF, 0x0A};  // JPEG XL codestream signature
    file.write(reinterpret_cast<const char*>(jxl_signature), 2);

    file.close();
    return true;
}

auto ExtendedImageWriter::WriteWebP(
    const std::shared_ptr<ImageBuffer>& image,
    const std::string& path,
    const ExportParams& params
) -> bool {
    // WebP requires libwebp
    // Key advantages:
    // - Better compression than JPEG
    // - Alpha channel support
    // - Lossless mode
    // - Animation support

    // Placeholder - would use WebPEncode API

    std::ofstream file(path, std::ios::binary);
    if (!file.is_open()) {
        return false;
    }

    // Placeholder: write RIFF header
    const uint8_t riff_header[] = {'R', 'I', 'F', 'F'};
    file.write(reinterpret_cast<const char*>(riff_header), 4);

    file.close();
    return true;
}

auto ExtendedImageWriter::WriteHEIF(
    const std::shared_ptr<ImageBuffer>& image,
    const std::string& path,
    const ExportParams& params
) -> bool {
    // HEIF requires libheif
    // Key advantages:
    // - HEVC encoding (better compression)
    // - HDR support
    // - Container for multiple images

    // Placeholder - would use heif_context API

    std::ofstream file(path, std::ios::binary);
    if (!file.is_open()) {
        return false;
    }

    file.close();
    return true;
}

auto ExtendedImageWriter::WriteEXR(
    const std::shared_ptr<ImageBuffer>& image,
    const std::string& path,
    const ExportParams& params
) -> bool {
    // OpenEXR for HDR output
    // Key advantages:
    // - Full HDR range (16/32-bit float)
    // - Multi-channel support
    // - Industry standard for VFX

    // Placeholder - would use OpenEXR library

    std::ofstream file(path, std::ios::binary);
    if (!file.is_open()) {
        return false;
    }

    file.close();
    return true;
}

// ============================================================================
// FormatDetector Implementation
// ============================================================================

auto FormatDetector::DetectFromExtension(const std::string& extension) -> ExportFormat {
    std::string ext = extension;
    std::transform(ext.begin(), ext.end(), ext.begin(), ::tolower);

    // Remove leading dot if present
    if (!ext.empty() && ext[0] == '.') {
        ext = ext.substr(1);
    }

    if (ext == "jpg" || ext == "jpeg") return ExportFormat::JPEG;
    if (ext == "png") return ExportFormat::PNG;
    if (ext == "tif" || ext == "tiff") return ExportFormat::TIFF;
    if (ext == "jxl") return ExportFormat::JPEGXL;
    if (ext == "webp") return ExportFormat::WebP;
    if (ext == "heic" || ext == "heif") return ExportFormat::HEIF;
    if (ext == "bmp") return ExportFormat::BMP;
    if (ext == "exr") return ExportFormat::OpenEXR;

    return ExportFormat::JPEG;  // Default
}

auto FormatDetector::DetectFromFile(const std::string& path) -> ExportFormat {
    std::ifstream file(path, std::ios::binary);
    if (!file.is_open()) {
        return ExportFormat::JPEG;
    }

    uint8_t header[12] = {0};
    file.read(reinterpret_cast<char*>(header), 12);
    file.close();

    // JPEG: FF D8 FF
    if (header[0] == 0xFF && header[1] == 0xD8 && header[2] == 0xFF) {
        return ExportFormat::JPEG;
    }

    // PNG: 89 50 4E 47
    if (header[0] == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
        return ExportFormat::PNG;
    }

    // TIFF: 49 49 (II) or 4D 4D (MM)
    if ((header[0] == 0x49 && header[1] == 0x49) || (header[0] == 0x4D && header[1] == 0x4D)) {
        return ExportFormat::TIFF;
    }

    // WebP: RIFF....WEBP
    if (header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F') {
        if (header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return ExportFormat::WebP;
        }
    }

    // JPEG XL codestream: FF 0A
    if (header[0] == 0xFF && header[1] == 0x0A) {
        return ExportFormat::JPEGXL;
    }

    // JPEG XL container:....JXL
    if (header[4] == 'j' && header[5] == 'X' && header[6] == 'L' && header[7] == ' ') {
        return ExportFormat::JPEGXL;
    }

    // HEIF/HEIC: ftypheic or ftypheix
    if (header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p') {
        if (header[8] == 'h' && header[9] == 'e' && header[10] == 'i') {
            return ExportFormat::HEIF;
        }
    }

    return ExportFormat::JPEG;
}

auto FormatDetector::GetSupportedExtensions() -> std::vector<std::string> {
    return {
        "jpg", "jpeg", "png", "tif", "tiff",
        "jxl", "webp", "heic", "heif", "bmp", "exr"
    };
}

// ============================================================================
// WebPAnimationWriter Implementation
// ============================================================================

class WebPAnimationWriter::Impl {
public:
    int width = 0;
    int height = 0;
    int fps = 30;
    int loop_count = 0;
    std::vector<std::shared_ptr<ImageBuffer>> frames;
    std::vector<int> durations;
};

WebPAnimationWriter::WebPAnimationWriter() : impl_(std::make_unique<Impl>()) {}

WebPAnimationWriter::~WebPAnimationWriter() = default;

auto WebPAnimationWriter::Initialize(int width, int height, int fps, int loop_count) -> bool {
    impl_->width = width;
    impl_->height = height;
    impl_->fps = fps;
    impl_->loop_count = loop_count;
    impl_->frames.clear();
    impl_->durations.clear();
    return true;
}

auto WebPAnimationWriter::AddFrame(
    const std::shared_ptr<ImageBuffer>& frame,
    int duration_ms
) -> bool {
    if (!frame) return false;

    impl_->frames.push_back(frame);
    impl_->durations.push_back(duration_ms > 0 ? duration_ms : 1000 / impl_->fps);

    return true;
}

auto WebPAnimationWriter::Finalize(const std::string& path, int quality) -> bool {
    // Would use WebPAnimEncoder API
    // Placeholder implementation

    std::ofstream file(path, std::ios::binary);
    if (!file.is_open()) {
        return false;
    }

    file.close();
    return true;
}

auto WebPAnimationWriter::GetFrameCount() const -> int {
    return static_cast<int>(impl_->frames.size());
}

// ============================================================================
// JPEGXLMultiLayerWriter Implementation
// ============================================================================

class JPEGXLMultiLayerWriter::Impl {
public:
    std::shared_ptr<ImageBuffer> base_image;
    std::vector<std::pair<std::string, std::vector<uint8_t>>> layers;
    std::string metadata;
};

JPEGXLMultiLayerWriter::JPEGXLMultiLayerWriter() : impl_(std::make_unique<Impl>()) {}

JPEGXLMultiLayerWriter::~JPEGXLMultiLayerWriter() = default;

auto JPEGXLMultiLayerWriter::Initialize(const std::shared_ptr<ImageBuffer>& base_image) -> bool {
    impl_->base_image = base_image;
    impl_->layers.clear();
    impl_->metadata.clear();
    return true;
}

auto JPEGXLMultiLayerWriter::AddLayer(
    const std::string& layer_name,
    const std::vector<uint8_t>& layer_data
) -> bool {
    impl_->layers.push_back({layer_name, layer_data});
    return true;
}

auto JPEGXLMultiLayerWriter::AddMetadata(const std::string& metadata_json) -> bool {
    impl_->metadata = metadata_json;
    return true;
}

auto JPEGXLMultiLayerWriter::Write(const std::string& path, bool lossless) -> bool {
    // JPEG XL supports storing additional metadata and layers
    // This enables non-destructive editing workflows

    std::ofstream file(path, std::ios::binary);
    if (!file.is_open()) {
        return false;
    }

    file.close();
    return true;
}

}  // namespace io
}  // namespace alcedo