//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <functional>
#include <memory>
#include <string>
#include <vector>

#include "mask/mask_definition.hpp"

namespace alcedo {
namespace ai {

/// AI mask generation result with quality metrics
struct AIMaskResult {
    mask::MaskBitmap bitmap;
    float confidence{0.0f};          /// Overall confidence score [0, 1]
    float coverage_ratio{0.0f};      /// Percentage of image covered [0, 1]
    bool success{false};
    std::string error_message;
};

/// Point-based selection for SAM-2 style single-click masking
struct PointSelection {
    float x{0.0f};  /// Normalized x coordinate [0, 1]
    float y{0.0f};  /// Normalized y coordinate [0, 1]
    bool is_positive{true};  /// true = add point, false = remove area
};

/// Bounding box for SAM-2 style box selection
struct BoundingBox {
    float x1{0.0f};  /// Top-left x normalized [0, 1]
    float y1{0.0f};  /// Top-left y normalized [0, 1]
    float x2{1.0f};  /// Bottom-right x normalized [0, 1]
    float y2{1.0f};  /// Bottom-right y normalized [0, 1]
};

/// Progress callback for AI mask generation
using ProgressCallback = std::function<void(float progress, const std::string& stage)>;

/// Abstract interface for AI segmentation models (SAM-2 style)
class ISegmentationModel {
public:
    virtual ~ISegmentationModel() = default;

    /// Check if the model is loaded and ready
    virtual auto IsReady() const -> bool = 0;

    /// Load model from path (async, use callback for progress)
    virtual auto LoadModel(const std::string& model_path, ProgressCallback callback) -> bool = 0;

    /// Unload model and free resources
    virtual void UnloadModel() = 0;

    /// Get model name/identifier
    virtual auto GetModelName() const -> std::string = 0;

    /// Get required input dimensions (width, height)
    virtual auto GetInputDimensions() const -> std::pair<int, int> = 0;
};

/// SAM-2 style segment-anything model interface
class ISAM2Model : public ISegmentationModel {
public:
    /// Generate mask from point prompts (single or multiple points)
    virtual auto GenerateFromPoints(
        const uint8_t* image_data,
        int width,
        int height,
        int channels,
        const std::vector<PointSelection>& points,
        ProgressCallback callback = nullptr
    ) -> AIMaskResult = 0;

    /// Generate mask from bounding box prompt
    virtual auto GenerateFromBox(
        const uint8_t* image_data,
        int width,
        int height,
        int channels,
        const BoundingBox& box,
        ProgressCallback callback = nullptr
    ) -> AIMaskResult = 0;

    /// Generate mask automatically (no prompts, auto-detect subject)
    virtual auto GenerateAutomatic(
        const uint8_t* image_data,
        int width,
        int height,
        int channels,
        ProgressCallback callback = nullptr
    ) -> AIMaskResult = 0;
};

/// Semantic segmentation model for specific classes (Sky, Depth, Foreground)
class ISemanticSegmentationModel : public ISegmentationModel {
public:
    /// Generate mask for a specific semantic class
    virtual auto GenerateForClass(
        const uint8_t* image_data,
        int width,
        int height,
        int channels,
        mask::MaskType target_class,
        ProgressCallback callback = nullptr
    ) -> AIMaskResult = 0;

    /// Get list of supported semantic classes
    virtual auto GetSupportedClasses() const -> std::vector<mask::MaskType> = 0;
};

/// Factory for creating AI mask generators
class AIMaskGeneratorFactory {
public:
    /// Create SAM-2 style model (for subject detection with prompts)
    static auto CreateSAM2Model(const std::string& model_type = "sam2-hiera-large")
        -> std::shared_ptr<ISAM2Model>;

    /// Create semantic segmentation model (for sky/depth/foreground)
    static auto CreateSemanticSegmentationModel(const std::string& model_type = "segformer-b5")
        -> std::shared_ptr<ISemanticSegmentationModel>;

    /// Check if SAM-2 model is available
    static auto IsSAM2Available() -> bool;

    /// Check if semantic segmentation model is available
    static auto IsSemanticSegmentationAvailable() -> bool;

    /// Get available model types for SAM-2
    static auto GetAvailableSAM2Models() -> std::vector<std::string>;

    /// Get available model types for semantic segmentation
    static auto GetAvailableSemanticModels() -> std::vector<std::string>;
};

/// High-level AI mask generation service
class AIMaskService {
public:
    AIMaskService();
    ~AIMaskService();

    /// Initialize the service with default models
    auto Initialize(ProgressCallback callback = nullptr) -> bool;

    /// Check if service is ready
    auto IsReady() const -> bool;

    /// Generate subject mask with SAM-2 (automatic detection or point/box guided)
    auto GenerateSubjectMask(
        const uint8_t* image_data,
        int width,
        int height,
        int channels,
        const std::vector<PointSelection>* points = nullptr,
        const BoundingBox* box = nullptr,
        ProgressCallback callback = nullptr
    ) -> AIMaskResult;

    /// Generate sky mask using semantic segmentation
    auto GenerateSkyMask(
        const uint8_t* image_data,
        int width,
        int height,
        int channels,
        ProgressCallback callback = nullptr
    ) -> AIMaskResult;

    /// Generate foreground/background separation mask
    auto GenerateForegroundMask(
        const uint8_t* image_data,
        int width,
        int height,
        int channels,
        ProgressCallback callback = nullptr
    ) -> AIMaskResult;

    /// Generate depth-based mask (distance from camera)
    auto GenerateDepthMask(
        const uint8_t* image_data,
        int width,
        int height,
        int channels,
        float near_threshold = 0.0f,
        float far_threshold = 1.0f,
        ProgressCallback callback = nullptr
    ) -> AIMaskResult;

    /// Generate color-based parametric mask
    auto GenerateColorMask(
        const uint8_t* image_data,
        int width,
        int height,
        int channels,
        float hue_center,
        float hue_range,
        float saturation_min = 0.0f,
        float saturation_max = 1.0f,
        float luminance_min = 0.0f,
        float luminance_max = 1.0f
    ) -> AIMaskResult;

    /// Generate luminance-based parametric mask
    auto GenerateLuminanceMask(
        const uint8_t* image_data,
        int width,
        int height,
        int channels,
        float shadow_threshold = 0.0f,
        float highlight_threshold = 1.0f,
        float feather = 0.1f
    ) -> AIMaskResult;

    /// Refine existing mask with morphological operations
    auto RefineMask(
        mask::MaskBitmap& mask,
        int blur_radius = 0,
        int expand_pixels = 0,
        int contract_pixels = 0
    ) -> void;

private:
    class Impl;
    std::unique_ptr<Impl> impl_;
};

/// Utility functions for mask manipulation
namespace mask_utils {

/// Combine two masks with given operation
auto CombineMasks(
    const mask::MaskBitmap& mask1,
    const mask::MaskBitmap& mask2,
    mask::SubMaskMode mode
) -> mask::MaskBitmap;

/// Invert a mask
auto InvertMask(const mask::MaskBitmap& mask) -> mask::MaskBitmap;

/// Apply opacity to mask
auto ApplyOpacity(mask::MaskBitmap& mask, float opacity) -> void;

/// Blur mask edges
auto BlurMask(mask::MaskBitmap& mask, int radius) -> void;

/// Dilate mask (expand white areas)
auto DilateMask(mask::MaskBitmap& mask, int pixels) -> void;

/// Erode mask (shrink white areas)
auto ErodeMask(mask::MaskBitmap& mask, int pixels) -> void;

/// Compute mask coverage ratio
auto ComputeCoverage(const mask::MaskBitmap& mask) -> float;

/// Threshold mask to binary
auto ThresholdMask(mask::MaskBitmap& mask, float threshold) -> void;

}  // namespace mask_utils

}  // namespace ai
}  // namespace alcedo