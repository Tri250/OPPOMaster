//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "processing/ai_mask_generator.hpp"

#include <algorithm>
#include <cmath>
#include <cstring>
#include <mutex>
#include <thread>

namespace alcedo {
namespace ai {

// ============================================================================
// Mask Utility Functions Implementation
// ============================================================================

namespace mask_utils {

auto CombineMasks(
    const mask::MaskBitmap& mask1,
    const mask::MaskBitmap& mask2,
    mask::SubMaskMode mode
) -> mask::MaskBitmap {
    if (mask1.width != mask2.width || mask1.height != mask2.height) {
        return mask1;  // Invalid dimensions
    }

    mask::MaskBitmap result;
    result.width = mask1.width;
    result.height = mask1.height;
    result.data.resize(static_cast<size_t>(result.width) * result.height, 0);

    const size_t total_pixels = static_cast<size_t>(result.width) * result.height;

    switch (mode) {
        case mask::SubMaskMode::Additive: {
            for (size_t i = 0; i < total_pixels; ++i) {
                result.data[i] = std::min(255, static_cast<int>(mask1.data[i]) + static_cast<int>(mask2.data[i]));
            }
            break;
        }
        case mask::SubMaskMode::Subtractive: {
            for (size_t i = 0; i < total_pixels; ++i) {
                result.data[i] = std::max(0, static_cast<int>(mask1.data[i]) - static_cast<int>(mask2.data[i]));
            }
            break;
        }
        case mask::SubMaskMode::Intersect: {
            for (size_t i = 0; i < total_pixels; ++i) {
                result.data[i] = std::min(mask1.data[i], mask2.data[i]);
            }
            break;
        }
    }

    return result;
}

auto InvertMask(const mask::MaskBitmap& mask) -> mask::MaskBitmap {
    mask::MaskBitmap result;
    result.width = mask.width;
    result.height = mask.height;
    result.data.resize(static_cast<size_t>(result.width) * result.height);

    for (size_t i = 0; i < result.data.size(); ++i) {
        result.data[i] = 255 - mask.data[i];
    }

    return result;
}

auto ApplyOpacity(mask::MaskBitmap& mask, float opacity) -> void {
    if (opacity < 0.0f) opacity = 0.0f;
    if (opacity > 1.0f) opacity = 1.0f;

    const float scale = opacity;
    for (auto& pixel : mask.data) {
        pixel = static_cast<uint8_t>(static_cast<float>(pixel) * scale);
    }
}

auto BlurMask(mask::MaskBitmap& mask, int radius) -> void {
    if (radius <= 0) return;

    const int width = mask.width;
    const int height = mask.height;
    const size_t total_pixels = static_cast<size_t>(width) * height;

    std::vector<float> blurred(total_pixels, 0.0f);
    std::vector<float> temp(total_pixels, 0.0f);

    // Convert to float
    for (size_t i = 0; i < total_pixels; ++i) {
        temp[i] = static_cast<float>(mask.data[i]) / 255.0f;
    }

    // Box blur (separable)
    const int kernel_size = 2 * radius + 1;
    const float kernel_weight = 1.0f / static_cast<float>(kernel_size);

    // Horizontal pass
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            float sum = 0.0f;
            for (int k = -radius; k <= radius; ++k) {
                int src_x = std::clamp(x + k, 0, width - 1);
                sum += temp[static_cast<size_t>(y) * width + src_x];
            }
            blurred[static_cast<size_t>(y) * width + x] = sum * kernel_weight;
        }
    }

    // Copy back to temp
    temp = blurred;

    // Vertical pass
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            float sum = 0.0f;
            for (int k = -radius; k <= radius; ++k) {
                int src_y = std::clamp(y + k, 0, height - 1);
                sum += temp[static_cast<size_t>(src_y) * width + x];
            }
            blurred[static_cast<size_t>(y) * width + x] = sum * kernel_weight;
        }
    }

    // Convert back to uint8
    for (size_t i = 0; i < total_pixels; ++i) {
        mask.data[i] = static_cast<uint8_t>(std::clamp(blurred[i] * 255.0f, 0.0f, 255.0f));
    }
}

auto DilateMask(mask::MaskBitmap& mask, int pixels) -> void {
    if (pixels <= 0) return;

    const int width = mask.width;
    const int height = mask.height;
    const size_t total_pixels = static_cast<size_t>(width) * height;

    std::vector<uint8_t> dilated = mask.data;

    for (int iteration = 0; iteration < pixels; ++iteration) {
        std::vector<uint8_t> temp = dilated;

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                uint8_t max_val = 0;
                for (int dy = -1; dy <= 1; ++dy) {
                    for (int dx = -1; dx <= 1; ++dx) {
                        int nx = std::clamp(x + dx, 0, width - 1);
                        int ny = std::clamp(y + dy, 0, height - 1);
                        max_val = std::max(max_val, temp[static_cast<size_t>(ny) * width + nx]);
                    }
                }
                dilated[static_cast<size_t>(y) * width + x] = max_val;
            }
        }
    }

    mask.data = std::move(dilated);
}

auto ErodeMask(mask::MaskBitmap& mask, int pixels) -> void {
    if (pixels <= 0) return;

    const int width = mask.width;
    const int height = mask.height;
    const size_t total_pixels = static_cast<size_t>(width) * height;

    std::vector<uint8_t> eroded = mask.data;

    for (int iteration = 0; iteration < pixels; ++iteration) {
        std::vector<uint8_t> temp = eroded;

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                uint8_t min_val = 255;
                for (int dy = -1; dy <= 1; ++dy) {
                    for (int dx = -1; dx <= 1; ++dx) {
                        int nx = std::clamp(x + dx, 0, width - 1);
                        int ny = std::clamp(y + dy, 0, height - 1);
                        min_val = std::min(min_val, temp[static_cast<size_t>(ny) * width + nx]);
                    }
                }
                eroded[static_cast<size_t>(y) * width + x] = min_val;
            }
        }
    }

    mask.data = std::move(eroded);
}

auto ComputeCoverage(const mask::MaskBitmap& mask) -> float {
    if (mask.data.empty()) return 0.0f;

    uint64_t sum = 0;
    for (const auto& pixel : mask.data) {
        sum += static_cast<uint64_t>(pixel);
    }

    return static_cast<float>(sum) / (static_cast<float>(mask.data.size()) * 255.0f);
}

auto ThresholdMask(mask::MaskBitmap& mask, float threshold) -> void {
    const uint8_t thresh_value = static_cast<uint8_t>(threshold * 255.0f);

    for (auto& pixel : mask.data) {
        pixel = (pixel > thresh_value) ? 255 : 0;
    }
}

}  // namespace mask_utils

// ============================================================================
// AIMaskService Implementation
// ============================================================================

class AIMaskService::Impl {
public:
    std::shared_ptr<ISAM2Model> sam2_model_;
    std::shared_ptr<ISemanticSegmentationModel> semantic_model_;
    std::mutex mutex_;
    bool initialized_ = false;
};

AIMaskService::AIMaskService() : impl_(std::make_unique<Impl>()) {}

AIMaskService::~AIMaskService() = default;

auto AIMaskService::Initialize(ProgressCallback callback) -> bool {
    std::lock_guard<std::mutex> lock(impl_->mutex_);

    if (impl_->initialized_) {
        return true;
    }

    if (callback) {
        callback(0.0f, "Loading SAM-2 model...");
    }

    // Try to load SAM-2 model
    if (AIMaskGeneratorFactory::IsSAM2Available()) {
        impl_->sam2_model_ = AIMaskGeneratorFactory::CreateSAM2Model();
    }

    if (callback) {
        callback(0.5f, "Loading semantic segmentation model...");
    }

    // Try to load semantic segmentation model
    if (AIMaskGeneratorFactory::IsSemanticSegmentationAvailable()) {
        impl_->semantic_model_ = AIMaskGeneratorFactory::CreateSemanticSegmentationModel();
    }

    if (callback) {
        callback(1.0f, "Initialization complete");
    }

    impl_->initialized_ = (impl_->sam2_model_ || impl_->semantic_model_);
    return impl_->initialized_;
}

auto AIMaskService::IsReady() const -> bool {
    return impl_->initialized_;
}

auto AIMaskService::GenerateSubjectMask(
    const uint8_t* image_data,
    int width,
    int height,
    int channels,
    const std::vector<PointSelection>* points,
    const BoundingBox* box,
    ProgressCallback callback
) -> AIMaskResult {
    AIMaskResult result;

    if (!impl_->sam2_model_ || !impl_->sam2_model_->IsReady()) {
        result.error_message = "SAM-2 model not available";
        return result;
    }

    if (points && !points->empty()) {
        result = impl_->sam2_model_->GenerateFromPoints(
            image_data, width, height, channels, *points, callback);
    } else if (box) {
        result = impl_->sam2_model_->GenerateFromBox(
            image_data, width, height, channels, *box, callback);
    } else {
        result = impl_->sam2_model_->GenerateAutomatic(
            image_data, width, height, channels, callback);
    }

    return result;
}

auto AIMaskService::GenerateSkyMask(
    const uint8_t* image_data,
    int width,
    int height,
    int channels,
    ProgressCallback callback
) -> AIMaskResult {
    if (!impl_->semantic_model_ || !impl_->semantic_model_->IsReady()) {
        return {mask::MaskBitmap{}, 0.0f, 0.0f, false, "Semantic segmentation model not available"};
    }

    return impl_->semantic_model_->GenerateForClass(
        image_data, width, height, channels, mask::MaskType::AiSky, callback);
}

auto AIMaskService::GenerateForegroundMask(
    const uint8_t* image_data,
    int width,
    int height,
    int channels,
    ProgressCallback callback
) -> AIMaskResult {
    if (!impl_->semantic_model_ || !impl_->semantic_model_->IsReady()) {
        return {mask::MaskBitmap{}, 0.0f, 0.0f, false, "Semantic segmentation model not available"};
    }

    return impl_->semantic_model_->GenerateForClass(
        image_data, width, height, channels, mask::MaskType::AiForeground, callback);
}

auto AIMaskService::GenerateDepthMask(
    const uint8_t* image_data,
    int width,
    int height,
    int channels,
    float near_threshold,
    float far_threshold,
    ProgressCallback callback
) -> AIMaskResult {
    if (!impl_->semantic_model_ || !impl_->semantic_model_->IsReady()) {
        return {mask::MaskBitmap{}, 0.0f, 0.0f, false, "Semantic segmentation model not available"};
    }

    // Generate depth map first
    auto depth_result = impl_->semantic_model_->GenerateForClass(
        image_data, width, height, channels, mask::MaskType::AiDepth, callback);

    if (!depth_result.success) {
        return depth_result;
    }

    // Apply near/far threshold
    const float range = far_threshold - near_threshold;
    if (range <= 0.0f) {
        return depth_result;
    }

    for (auto& pixel : depth_result.bitmap.data) {
        float normalized = static_cast<float>(pixel) / 255.0f;
        if (normalized < near_threshold || normalized > far_threshold) {
            pixel = 0;
        } else {
            // Remap to [0, 255]
            pixel = static_cast<uint8_t>(((normalized - near_threshold) / range) * 255.0f);
        }
    }

    return depth_result;
}

auto AIMaskService::GenerateColorMask(
    const uint8_t* image_data,
    int width,
    int height,
    int channels,
    float hue_center,
    float hue_range,
    float saturation_min,
    float saturation_max,
    float luminance_min,
    float luminance_max
) -> AIMaskResult {
    AIMaskResult result;
    result.bitmap.width = width;
    result.bitmap.height = height;
    result.bitmap.data.resize(static_cast<size_t>(width) * height, 0);

    if (!image_data || channels < 3) {
        result.error_message = "Invalid image data";
        return result;
    }

    // Convert hue_center to [0, 360] and handle wrap-around
    const float hue_min = hue_center - hue_range;
    const float hue_max = hue_center + hue_range;

    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            const size_t idx = static_cast<size_t>((y * width + x) * channels);

            // Extract RGB
            const float r = static_cast<float>(image_data[idx]) / 255.0f;
            const float g = static_cast<float>(image_data[idx + 1]) / 255.0f;
            const float b = static_cast<float>(image_data[idx + 2]) / 255.0f;

            // Convert to HSL
            const float max_c = std::max({r, g, b});
            const float min_c = std::min({r, g, b});
            const float l = (max_c + min_c) / 2.0f;
            const float delta = max_c - min_c;

            float h = 0.0f;
            float s = 0.0f;

            if (delta > 0.0f) {
                s = (l > 0.5f) ? (delta / (2.0f - max_c - min_c)) : (delta / (max_c + min_c));

                if (max_c == r) {
                    h = 60.0f * std::fmod((g - b) / delta, 6.0f);
                } else if (max_c == g) {
                    h = 60.0f * ((b - r) / delta + 2.0f);
                } else {
                    h = 60.0f * ((r - g) / delta + 4.0f);
                }

                if (h < 0.0f) h += 360.0f;
            }

            // Check if within range
            bool hue_match = (h >= hue_min && h <= hue_max);
            // Handle wrap-around (e.g., red wraps from 330 to 30)
            if (hue_min < 0.0f) {
                hue_match = hue_match || (h >= (hue_min + 360.0f));
            }
            if (hue_max > 360.0f) {
                hue_match = hue_match || (h <= (hue_max - 360.0f));
            }

            const bool sat_match = (s >= saturation_min && s <= saturation_max);
            const bool lum_match = (l >= luminance_min && l <= luminance_max);

            // Compute mask value
            float mask_value = 0.0f;
            if (hue_match && sat_match && lum_match) {
                // Smooth falloff based on distance from center
                float hue_dist = 0.0f;
                if (hue_match) {
                    float dist1 = std::abs(h - hue_center);
                    float dist2 = 360.0f - dist1;
                    hue_dist = std::min(dist1, dist2);
                }
                const float hue_factor = 1.0f - (hue_dist / hue_range);

                const float sat_center = (saturation_min + saturation_max) / 2.0f;
                const float sat_range = (saturation_max - saturation_min) / 2.0f;
                const float sat_dist = std::abs(s - sat_center);
                const float sat_factor = sat_range > 0.0f ? (1.0f - sat_dist / sat_range) : 1.0f;

                const float lum_center = (luminance_min + luminance_max) / 2.0f;
                const float lum_range = (luminance_max - luminance_min) / 2.0f;
                const float lum_dist = std::abs(l - lum_center);
                const float lum_factor = lum_range > 0.0f ? (1.0f - lum_dist / lum_range) : 1.0f;

                mask_value = hue_factor * sat_factor * lum_factor;
            }

            result.bitmap.data[y * width + x] = static_cast<uint8_t>(mask_value * 255.0f);
        }
    }

    result.success = true;
    result.coverage_ratio = mask_utils::ComputeCoverage(result.bitmap);
    result.confidence = result.coverage_ratio;
    return result;
}

auto AIMaskService::GenerateLuminanceMask(
    const uint8_t* image_data,
    int width,
    int height,
    int channels,
    float shadow_threshold,
    float highlight_threshold,
    float feather
) -> AIMaskResult {
    AIMaskResult result;
    result.bitmap.width = width;
    result.bitmap.height = height;
    result.bitmap.data.resize(static_cast<size_t>(width) * height, 0);

    if (!image_data) {
        result.error_message = "Invalid image data";
        return result;
    }

    const float shadow_thresh_norm = shadow_threshold * 255.0f;
    const float highlight_thresh_norm = highlight_threshold * 255.0f;
    const float feather_norm = feather * 255.0f;

    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            size_t idx = static_cast<size_t>((y * width + x) * channels);

            // Compute luminance
            float luminance = 0.0f;
            if (channels >= 3) {
                // Standard luminance formula
                luminance = 0.2126f * static_cast<float>(image_data[idx]) +
                           0.7152f * static_cast<float>(image_data[idx + 1]) +
                           0.0722f * static_cast<float>(image_data[idx + 2]);
            } else {
                luminance = static_cast<float>(image_data[idx]);
            }

            // Compute mask value based on luminance range
            float mask_value = 255.0f;

            // Apply shadow threshold with feathering
            if (luminance < shadow_thresh_norm - feather_norm) {
                mask_value = 0.0f;
            } else if (luminance < shadow_thresh_norm + feather_norm) {
                float t = (luminance - (shadow_thresh_norm - feather_norm)) / (2.0f * feather_norm);
                mask_value *= t;
            }

            // Apply highlight threshold with feathering
            if (luminance > highlight_thresh_norm + feather_norm) {
                mask_value = 0.0f;
            } else if (luminance > highlight_thresh_norm - feather_norm) {
                float t = ((highlight_thresh_norm + feather_norm) - luminance) / (2.0f * feather_norm);
                mask_value *= t;
            }

            result.bitmap.data[y * width + x] = static_cast<uint8_t>(mask_value);
        }
    }

    result.success = true;
    result.coverage_ratio = mask_utils::ComputeCoverage(result.bitmap);
    result.confidence = result.coverage_ratio;
    return result;
}

auto AIMaskService::RefineMask(
    mask::MaskBitmap& mask,
    int blur_radius,
    int expand_pixels,
    int contract_pixels
) -> void {
    // First expand
    if (expand_pixels > 0) {
        mask_utils::DilateMask(mask, expand_pixels);
    }

    // Then contract
    if (contract_pixels > 0) {
        mask_utils::ErodeMask(mask, contract_pixels);
    }

    // Finally blur
    if (blur_radius > 0) {
        mask_utils::BlurMask(mask, blur_radius);
    }
}

// ============================================================================
// AIMaskGeneratorFactory Implementation (Stub - actual implementation would load ONNX models)
// ============================================================================

auto AIMaskGeneratorFactory::CreateSAM2Model(const std::string& model_type)
    -> std::shared_ptr<ISAM2Model> {
    // TODO: Implement actual SAM-2 model loading via ONNX Runtime
    // This would be implemented with ONNX Runtime or similar
    return nullptr;
}

auto AIMaskGeneratorFactory::CreateSemanticSegmentationModel(const std::string& model_type)
    -> std::shared_ptr<ISemanticSegmentationModel> {
    // TODO: Implement actual semantic segmentation model loading
    return nullptr;
}

auto AIMaskGeneratorFactory::IsSAM2Available() -> bool {
    // Check if SAM-2 model files are available
    return false;  // Placeholder - would check actual model availability
}

auto AIMaskGeneratorFactory::IsSemanticSegmentationAvailable() -> bool {
    // Check if semantic segmentation model files are available
    return false;  // Placeholder
}

auto AIMaskGeneratorFactory::GetAvailableSAM2Models() -> std::vector<std::string> {
    return {
        "sam2-hiera-tiny",
        "sam2-hiera-small",
        "sam2-hiera-base-plus",
        "sam2-hiera-large"
    };
}

auto AIMaskGeneratorFactory::GetAvailableSemanticModels() -> std::vector<std::string> {
    return {
        "segformer-b0",
        "segformer-b5",
        "isnet-sam",
        "depth-anything-v2"
    };
}

}  // namespace ai
}  // namespace alcedo