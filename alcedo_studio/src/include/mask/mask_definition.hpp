#ifndef ALCEDO_MASK_MASK_DEFINITION_HPP
#define ALCEDO_MASK_MASK_DEFINITION_HPP

#include <string>
#include <vector>
#include <memory>
#include <functional>
#include <image.hpp>

namespace alcedo {
namespace mask {

/// Mask blend mode for sub-masks
enum class SubMaskMode {
    Additive,      /// Add mask to the combined result
    Subtractive,   /// Subtract mask from the combined result
    Intersect      /// Intersect with existing mask
};

/// Type of mask
enum class MaskType {
    Brush,         /// Manual brush mask
    GradientLinear,/// Linear gradient
    GradientRadial,/// Radial gradient
    AiSubject,     /// AI detected subject
    AiSky,         /// AI detected sky
    AiDepth,       /// AI depth based mask
    AiForeground,  /// AI detected foreground
    Color,         /// Color-based parametric mask
    Luminance      /// Luminance-based parametric mask
};

/// Single sub-mask within a mask definition
struct SubMask {
    std::string id;
    MaskType mask_type;
    bool visible = true;
    bool invert = false;
    float opacity = 100.0f;
    SubMaskMode mode = SubMaskMode::Additive;

    // For brush masks: points with pressure
    // For gradient masks: start/end points
    // For parametric: threshold values
    std::vector<float> parameters;
};

/// Complete mask definition containing multiple sub-masks
struct MaskDefinition {
    std::string id;
    std::string name;
    bool visible = true;
    bool invert = false;
    float opacity = 100.0f;

    // All sub-masks that make up this mask
    std::vector<SubMask> sub_masks;

    /// Check if any submask requires a warped image (parametric masks need processed pixels)
    bool requires_warped_image() const {
        for (const auto& sm : sub_masks) {
            if (sm.mask_type == MaskType::Color || sm.mask_type == MaskType::Luminance) {
                return true;
            }
        }
        return false;
    }
};

using MaskBitmap = image::ImageBuffer<image::Luma<unsigned char>, std::vector<unsigned char>>;

/// Generate the combined bitmap from all sub-masks
/// The result is a single channel 8-bit bitmap where 0 = masked, 255 = visible
MaskBitmap generate_mask_bitmap(
    const MaskDefinition& definition,
    const image::Image& image,
    int width,
    int height
);

} // namespace mask
} // namespace alcedo

#endif // ALCEDO_MASK_MASK_DEFINITION_HPP
