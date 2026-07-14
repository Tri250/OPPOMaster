//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "io/image/extended_image_writer.hpp"

#include <algorithm>
#include <cstring>
#include <filesystem>
#include <fstream>

#include <opencv2/imgcodecs.hpp>
#include <opencv2/imgproc.hpp>

namespace alcedo {
namespace io {

namespace {

/// Convert ImageBuffer's CPU data to a BGR/BGRA cv::Mat suitable for cv::imwrite.
/// Returns an empty Mat if conversion fails.
auto ImageBufferToBgrMat(const std::shared_ptr<ImageBuffer>& image) -> cv::Mat {
    if (!image) return {};

    cv::Mat& cpu = image->GetCPUData();
    if (cpu.empty()) return {};

    // Ensure CPU data is valid
    if (!image->cpu_data_valid_) {
        if (image->gpu_data_valid_) {
            image->SyncToCPU();
        } else {
            return {};
        }
    }

    cv::Mat& src = image->GetCPUData();
    if (src.empty()) return {};

    const int channels = src.channels();

    // Handle common types: 8U, 16U, 32F
    switch (src.depth()) {
        case CV_8U:
        case CV_16U:
        case CV_32F:
            break;
        default:
            return {};
    }

    if (channels == 4) {
        // RGBA -> BGRA for imwrite
        cv::Mat bgra;
        if (src.depth() == CV_32F) {
            // Float RGBA -> convert to 8U BGRA for most formats
            cv::Mat tmp;
            src.convertTo(tmp, CV_8UC4, 255.0);
            cv::cvtColor(tmp, bgra, cv::COLOR_RGBA2BGRA);
        } else {
            cv::cvtColor(src, bgra, cv::COLOR_RGBA2BGRA);
        }
        return bgra;
    } else if (channels == 3) {
        // RGB -> BGR for imwrite
        cv::Mat bgr;
        if (src.depth() == CV_32F) {
            cv::Mat tmp;
            src.convertTo(tmp, CV_8UC3, 255.0);
            cv::cvtColor(tmp, bgr, cv::COLOR_RGB2BGR);
        } else {
            cv::cvtColor(src, bgr, cv::COLOR_RGB2BGR);
        }
        return bgr;
    } else if (channels == 1) {
        if (src.depth() == CV_32F) {
            cv::Mat gray;
            src.convertTo(gray, CV_8UC1, 255.0);
            return gray;
        }
        return src.clone();
    }

    return {};
}

/// Convert ImageBuffer's CPU data to a BGR cv::Mat for formats that don't support alpha.
auto ImageBufferToBgrMatNoAlpha(const std::shared_ptr<ImageBuffer>& image) -> cv::Mat {
    cv::Mat mat = ImageBufferToBgrMat(image);
    if (mat.empty()) return mat;
    if (mat.channels() == 4) {
        cv::Mat bgr;
        cv::cvtColor(mat, bgr, cv::COLOR_BGRA2BGR);
        return bgr;
    }
    return mat;
}

/// Convert ImageBuffer's CPU data to a float BGRA cv::Mat for HDR formats.
auto ImageBufferToFloatBgra(const std::shared_ptr<ImageBuffer>& image) -> cv::Mat {
    if (!image) return {};

    if (!image->cpu_data_valid_) {
        if (image->gpu_data_valid_) {
            image->SyncToCPU();
        } else {
            return {};
        }
    }

    cv::Mat& src = image->GetCPUData();
    if (src.empty()) return {};

    cv::Mat rgba32f;
    if (src.depth() != CV_32F) {
        src.convertTo(rgba32f, CV_32F, 1.0 / 255.0);
    } else {
        rgba32f = src.clone();
    }

    const int channels = rgba32f.channels();
    if (channels == 4) {
        cv::Mat bgra;
        cv::cvtColor(rgba32f, bgra, cv::COLOR_RGBA2BGRA);
        return bgra;
    } else if (channels == 3) {
        cv::Mat bgr;
        cv::cvtColor(rgba32f, bgr, cv::COLOR_RGB2BGR);
        return bgr;
    } else if (channels == 1) {
        cv::Mat bgr;
        cv::cvtColor(rgba32f, bgr, cv::COLOR_GRAY2BGR);
        return bgr;
    }

    return {};
}

/// Try to write with cv::imwrite and return whether it succeeded.
auto TryImwrite(const std::string& path, const cv::Mat& img, const std::vector<int>& params) -> bool {
    try {
        return cv::imwrite(path, img, params);
    } catch (const cv::Exception&) {
        return false;
    }
}

}  // namespace

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
    // Check actual OpenCV support by attempting to encode a 1x1 image
    cv::Mat test_img(1, 1, CV_8UC3, cv::Scalar(0, 0, 0));

    switch (format) {
        case ExportFormat::JPEG:
            return TryImwrite("/tmp/_alcedo_fmt_test.jpg", test_img, {cv::IMWRITE_JPEG_QUALITY, 95});

        case ExportFormat::PNG:
            return TryImwrite("/tmp/_alcedo_fmt_test.png", test_img, {cv::IMWRITE_PNG_COMPRESSION, 1});

        case ExportFormat::TIFF:
            return TryImwrite("/tmp/_alcedo_fmt_test.tif", test_img, {});

        case ExportFormat::BMP:
            return TryImwrite("/tmp/_alcedo_fmt_test.bmp", test_img, {});

        case ExportFormat::WebP:
            return TryImwrite("/tmp/_alcedo_fmt_test.webp", test_img, {cv::IMWRITE_WEBP_QUALITY, 90});

        case ExportFormat::OpenEXR: {
            cv::Mat test_f(1, 1, CV_32FC3, cv::Scalar(0.0f, 0.0f, 0.0f));
            return TryImwrite("/tmp/_alcedo_fmt_test.exr", test_f, {});
        }

        case ExportFormat::JPEGXL:
            return TryImwrite("/tmp/_alcedo_fmt_test.jxl", test_img, {});

        case ExportFormat::HEIF:
            return TryImwrite("/tmp/_alcedo_fmt_test.heic", test_img, {});
    }

    return false;
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
    if (!image) {
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

        case ExportFormat::BMP: {
            cv::Mat bgr = ImageBufferToBgrMatNoAlpha(image);
            if (bgr.empty()) return false;
            return TryImwrite(path, bgr, {});
        }

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
    if (!image) return false;

    // Ensure CPU data is available
    if (!image->cpu_data_valid_) {
        if (image->gpu_data_valid_) {
            image->SyncToCPU();
        } else {
            return false;
        }
    }

    cv::Mat& src = image->GetCPUData();
    if (src.empty()) return false;

    // Determine file extension for imencode
    std::string ext = GetFileExtension(params.format);

    // Convert to BGR/BGRA mat suitable for encoding
    cv::Mat img;
    switch (params.format) {
        case ExportFormat::JPEG:
        case ExportFormat::BMP:
            img = ImageBufferToBgrMatNoAlpha(image);
            break;
        case ExportFormat::OpenEXR:
            img = ImageBufferToFloatBgra(image);
            break;
        default:
            img = ImageBufferToBgrMat(image);
            break;
    }

    if (img.empty()) return false;

    // Build compression params
    std::vector<int> compression_params;
    switch (params.format) {
        case ExportFormat::JPEG:
            compression_params = {cv::IMWRITE_JPEG_QUALITY, params.quality};
            break;
        case ExportFormat::PNG:
            compression_params = {cv::IMWRITE_PNG_COMPRESSION, params.compression_level};
            break;
        case ExportFormat::WebP:
            if (params.lossless) {
                compression_params = {cv::IMWRITE_WEBP_QUALITY, 100};
            } else {
                compression_params = {cv::IMWRITE_WEBP_QUALITY, params.quality};
            }
            break;
        case ExportFormat::OpenEXR:
            compression_params = {cv::IMWRITE_EXR_TYPE, cv::IMWRITE_EXR_TYPE_HALF};
            break;
        default:
            break;
    }

    try {
        return cv::imencode(ext, img, buffer, compression_params);
    } catch (const cv::Exception&) {
        return false;
    }
}

auto ExtendedImageWriter::EstimateFileSize(
    const std::shared_ptr<ImageBuffer>& image,
    const ExportParams& params
) -> size_t {
    if (!image) return 0;

    // Ensure CPU data is valid to get dimensions
    if (!image->cpu_data_valid_ && !image->gpu_data_valid_) return 0;

    int width = 0, height = 0, channels = 0;
    if (image->cpu_data_valid_) {
        cv::Mat& cpu = image->GetCPUData();
        if (cpu.empty()) return 0;
        width = cpu.cols;
        height = cpu.rows;
        channels = cpu.channels();
    } else if (image->gpu_data_valid_) {
        width = image->GetGPUWidth();
        height = image->GetGPUHeight();
        // Default to 4 channels for GPU data (typical RGBA)
        channels = 4;
    }

    const size_t pixel_count = static_cast<size_t>(width) * height;
    const size_t raw_size = pixel_count * channels;

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
    if (!image) return false;

    cv::Mat bgr = ImageBufferToBgrMatNoAlpha(image);
    if (bgr.empty()) return false;

    std::vector<int> compression_params;
    compression_params.push_back(cv::IMWRITE_JPEG_QUALITY);
    compression_params.push_back(params.quality);

    return TryImwrite(path, bgr, compression_params);
}

auto ExtendedImageWriter::WritePNG(
    const std::shared_ptr<ImageBuffer>& image,
    const std::string& path,
    const ExportParams& params
) -> bool {
    if (!image) return false;

    cv::Mat bgr = ImageBufferToBgrMat(image);
    if (bgr.empty()) return false;

    std::vector<int> compression_params;
    compression_params.push_back(cv::IMWRITE_PNG_COMPRESSION);
    compression_params.push_back(params.compression_level);

    return TryImwrite(path, bgr, compression_params);
}

auto ExtendedImageWriter::WriteTIFF(
    const std::shared_ptr<ImageBuffer>& image,
    const std::string& path,
    const ExportParams& params
) -> bool {
    if (!image) return false;

    cv::Mat bgr = ImageBufferToBgrMat(image);
    if (bgr.empty()) return false;

    std::vector<int> compression_params;
    compression_params.push_back(cv::IMWRITE_TIFF_COMPRESSION);
    compression_params.push_back(1);  // Default compression

    return TryImwrite(path, bgr, compression_params);
}

auto ExtendedImageWriter::WriteJPEGXL(
    const std::shared_ptr<ImageBuffer>& image,
    const std::string& path,
    const ExportParams& params
) -> bool {
    if (!image) return false;

    cv::Mat bgr = ImageBufferToBgrMat(image);
    if (bgr.empty()) return false;

    // JPEG XL support in OpenCV depends on build configuration
    // Try cv::imwrite with .jxl extension; return false if not supported
    std::vector<int> compression_params;
    if (params.lossless) {
        compression_params.push_back(cv::IMWRITE_JPEGXL_QUALITY);
        compression_params.push_back(100);
    } else {
        compression_params.push_back(cv::IMWRITE_JPEGXL_QUALITY);
        compression_params.push_back(params.quality);
    }
    compression_params.push_back(cv::IMWRITE_JPEGXL_EFFORT);
    compression_params.push_back(params.effort_level);

    if (TryImwrite(path, bgr, compression_params)) {
        return true;
    }

    // If JXL is not supported by this OpenCV build, return false
    return false;
}

auto ExtendedImageWriter::WriteWebP(
    const std::shared_ptr<ImageBuffer>& image,
    const std::string& path,
    const ExportParams& params
) -> bool {
    if (!image) return false;

    cv::Mat bgr = ImageBufferToBgrMat(image);
    if (bgr.empty()) return false;

    std::vector<int> compression_params;
    if (params.lossless) {
        compression_params.push_back(cv::IMWRITE_WEBP_QUALITY);
        compression_params.push_back(100);
    } else {
        compression_params.push_back(cv::IMWRITE_WEBP_QUALITY);
        compression_params.push_back(params.quality);
    }

    return TryImwrite(path, bgr, compression_params);
}

auto ExtendedImageWriter::WriteHEIF(
    const std::shared_ptr<ImageBuffer>& image,
    const std::string& path,
    const ExportParams& params
) -> bool {
    if (!image) return false;

    cv::Mat bgr = ImageBufferToBgrMat(image);
    if (bgr.empty()) return false;

    // HEIF support in OpenCV depends on build configuration
    // Try cv::imwrite with .heic extension; return false if not supported
    std::vector<int> compression_params;
    compression_params.push_back(cv::IMWRITE_HEIF_QUALITY);
    compression_params.push_back(params.quality);

    return TryImwrite(path, bgr, compression_params);
}

auto ExtendedImageWriter::WriteEXR(
    const std::shared_ptr<ImageBuffer>& image,
    const std::string& path,
    const ExportParams& params
) -> bool {
    if (!image) return false;

    cv::Mat bgr = ImageBufferToFloatBgra(image);
    if (bgr.empty()) return false;

    std::vector<int> compression_params;
    compression_params.push_back(cv::IMWRITE_EXR_TYPE);
    compression_params.push_back(cv::IMWRITE_EXR_TYPE_HALF);

    return TryImwrite(path, bgr, compression_params);
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
    if (impl_->frames.empty()) return false;

    // For single frame, write as a static WebP image
    if (impl_->frames.size() == 1) {
        const auto& frame = impl_->frames[0];
        if (!frame) return false;

        if (!frame->cpu_data_valid_) {
            if (frame->gpu_data_valid_) {
                frame->SyncToCPU();
            } else {
                return false;
            }
        }

        cv::Mat& src = frame->GetCPUData();
        if (src.empty()) return false;

        cv::Mat bgr;
        const int channels = src.channels();
        if (channels == 4) {
            if (src.depth() == CV_32F) {
                cv::Mat tmp;
                src.convertTo(tmp, CV_8UC4, 255.0);
                cv::cvtColor(tmp, bgr, cv::COLOR_RGBA2BGRA);
            } else {
                cv::cvtColor(src, bgr, cv::COLOR_RGBA2BGRA);
            }
        } else if (channels == 3) {
            if (src.depth() == CV_32F) {
                cv::Mat tmp;
                src.convertTo(tmp, CV_8UC3, 255.0);
                cv::cvtColor(tmp, bgr, cv::COLOR_RGB2BGR);
            } else {
                cv::cvtColor(src, bgr, cv::COLOR_RGB2BGR);
            }
        } else if (channels == 1) {
            if (src.depth() == CV_32F) {
                src.convertTo(bgr, CV_8UC1, 255.0);
            } else {
                bgr = src.clone();
            }
        } else {
            return false;
        }

        if (bgr.empty()) return false;

        std::vector<int> params = {cv::IMWRITE_WEBP_QUALITY, quality};
        return TryImwrite(path, bgr, params);
    }

    // Multi-frame WebP animation is not supported via cv::imwrite
    return false;
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
    // JPEG XL multi-layer support depends on libjxl.
    // Write the base image as a single-layer JXL file as a minimum.
    if (!impl_->base_image) return false;

    if (!impl_->base_image->cpu_data_valid_) {
        if (impl_->base_image->gpu_data_valid_) {
            impl_->base_image->SyncToCPU();
        } else {
            return false;
        }
    }

    cv::Mat& src = impl_->base_image->GetCPUData();
    if (src.empty()) return false;

    cv::Mat bgr;
    const int channels = src.channels();
    if (channels == 4) {
        if (src.depth() == CV_32F) {
            cv::Mat tmp;
            src.convertTo(tmp, CV_8UC4, 255.0);
            cv::cvtColor(tmp, bgr, cv::COLOR_RGBA2BGRA);
        } else {
            cv::cvtColor(src, bgr, cv::COLOR_RGBA2BGRA);
        }
    } else if (channels == 3) {
        if (src.depth() == CV_32F) {
            cv::Mat tmp;
            src.convertTo(tmp, CV_8UC3, 255.0);
            cv::cvtColor(tmp, bgr, cv::COLOR_RGB2BGR);
        } else {
            cv::cvtColor(src, bgr, cv::COLOR_RGB2BGR);
        }
    } else if (channels == 1) {
        if (src.depth() == CV_32F) {
            src.convertTo(bgr, CV_8UC1, 255.0);
        } else {
            bgr = src.clone();
        }
    } else {
        return false;
    }

    if (bgr.empty()) return false;

    // Try to write as JPEG XL; return false if format not supported
    std::vector<int> params;
    params.push_back(cv::IMWRITE_JPEGXL_QUALITY);
    params.push_back(lossless ? 100 : 90);
    params.push_back(cv::IMWRITE_JPEGXL_EFFORT);
    params.push_back(5);

    return TryImwrite(path, bgr, params);
}

}  // namespace io
}  // namespace alcedo