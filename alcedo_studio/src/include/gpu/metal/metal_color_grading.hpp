//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#ifdef __APPLE__
#include <Metal/Metal.h>
#else
// Placeholder for cross-platform Metal compatibility
#endif

#include <cstdint>
#include <memory>
#include <string>

#include "edit/operators/color_grading_3way.hpp"

namespace alcedo {
namespace gpu {
namespace metal {

#ifdef __APPLE__

/// Metal shader for 3-Way color grading
/// Uses OKLab color space for perceptual adjustments
extern const char* ColorGrading3WayShader;

/// Metal shader for LUT 3D application with trilinear interpolation
extern const char* ApplyLUT3DShader;

/// Metal shader for vectorscope generation
extern const char* VectorscopeShader;

/// Metal shader for RGB parade/waveform
extern const char* RGBParadeShader;

/// Metal shader for bilateral denoising
extern const char* BilateralDenoiseShader;

/// Metal shader for ROI rendering
extern const char* ROIRenderShader;

/// Metal shader for parametric mask generation
extern const char* ParametricMaskShader;

/// Metal wrapper class for color grading operations
class MetalColorGradingProcessor {
public:
    MetalColorGradingProcessor();
    ~MetalColorGradingProcessor();

    /// Initialize Metal resources
    auto Initialize(int max_width, int max_height) -> bool;

    /// Apply 3-Way color grading
    auto Apply3WayColorGrading(
        const uint8_t* input,
        uint8_t* output,
        int width,
        int height,
        const ColorGrading3WayOp::WheelControl& shadows,
        const ColorGrading3WayOp::WheelControl& midtones,
        const ColorGrading3WayOp::WheelControl& highlights,
        const ColorGrading3WayOp::WheelControl& global,
        float blending,
        float balance
    ) -> bool;

    /// Apply 3D LUT
    auto ApplyLUT3D(
        const uint8_t* input,
        uint8_t* output,
        int width,
        int height,
        const float* lut_data,
        int lut_size
    ) -> bool;

    /// Check if Metal is available on this system
    static auto IsMetalAvailable() -> bool;

    /// Get Metal device name
    auto GetDeviceName() const -> std::string;

private:
    id<MTLDevice> device_;
    id<MTLCommandQueue> command_queue_;
    id<MTLComputePipelineState> color_grading_pipeline_;
    id<MTLComputePipelineState> lut_pipeline_;
    id<MTLBuffer> input_buffer_;
    id<MTLBuffer> output_buffer_;
    id<MTLBuffer> lut_buffer_;
    id<MTLBuffer> params_buffer_;
    int max_width_;
    int max_height_;
    bool initialized_;
};

/// Metal wrapper for scope/analytics generation
class MetalScopeGenerator {
public:
    MetalScopeGenerator();
    ~MetalScopeGenerator();

    /// Initialize with scope dimensions
    auto Initialize(int scope_width, int scope_height) -> bool;

    /// Generate vectorscope
    auto GenerateVectorscope(
        const uint8_t* input,
        uint8_t* scope_output,
        int image_width,
        int image_height
    ) -> bool;

    /// Generate RGB parade
    auto GenerateRGBParade(
        const uint8_t* input,
        uint8_t* parade_output,
        int image_width,
        int image_height,
        int parade_height
    ) -> bool;

    /// Generate luma waveform
    auto GenerateLumaWaveform(
        const uint8_t* input,
        uint8_t* waveform_output,
        int image_width,
        int image_height,
        int waveform_height
    ) -> bool;

private:
    id<MTLDevice> device_;
    id<MTLCommandQueue> command_queue_;
    id<MTLComputePipelineState> vectorscope_pipeline_;
    id<MTLComputePipelineState> parade_pipeline_;
    id<MTLBuffer> input_buffer_;
    id<MTLBuffer> accumulation_buffer_;
    id<MTLBuffer> output_buffer_;
    int scope_width_;
    int scope_height_;
};

/// Metal wrapper for denoising operations
class MetalDenoiseProcessor {
public:
    MetalDenoiseProcessor();
    ~MetalDenoiseProcessor();

    /// Initialize with max dimensions
    auto Initialize(int max_width, int max_height) -> bool;

    /// Apply bilateral denoise
    auto ApplyBilateralDenoise(
        const uint8_t* input,
        uint8_t* output,
        int width,
        int height,
        float sigma_spatial,
        float sigma_range
    ) -> bool;

private:
    id<MTLDevice> device_;
    id<MTLCommandQueue> command_queue_;
    id<MTLComputePipelineState> bilateral_pipeline_;
    id<MTLBuffer> input_buffer_;
    id<MTLBuffer> output_buffer_;
    int max_width_;
    int max_height_;
};

/// Metal wrapper for ROI rendering
class MetalROIRenderer {
public:
    MetalROIRenderer();
    ~MetalROIRenderer();

    /// Initialize with max ROI dimensions
    auto Initialize(int max_roi_width, int max_roi_height) -> bool;

    /// Render ROI region from full image
    auto RenderROI(
        const uint8_t* full_input,
        uint8_t* roi_output,
        int full_width,
        int full_height,
        int roi_x,
        int roi_y,
        int roi_width,
        int roi_height,
        float scale_x = 1.0f,
        float scale_y = 1.0f
    ) -> bool;

private:
    id<MTLDevice> device_;
    id<MTLCommandQueue> command_queue_;
    id<MTLComputePipelineState> roi_pipeline_;
    id<MTLBuffer> full_input_buffer_;
    id<MTLBuffer> roi_output_buffer_;
    int max_roi_width_;
    int max_roi_height_;
};

/// Metal wrapper for mask generation
class MetalMaskGenerator {
public:
    MetalMaskGenerator();
    ~MetalMaskGenerator();

    /// Initialize
    auto Initialize(int max_width, int max_height) -> bool;

    /// Generate color parametric mask
    auto GenerateColorMask(
        const uint8_t* input,
        uint8_t* mask,
        int width,
        int height,
        float hue_center,
        float hue_range,
        float sat_min,
        float sat_max,
        float lum_min,
        float lum_max
    ) -> bool;

    /// Generate luminance parametric mask
    auto GenerateLuminanceMask(
        const uint8_t* input,
        uint8_t* mask,
        int width,
        int height,
        float shadow_threshold,
        float highlight_threshold,
        float feather
    ) -> bool;

private:
    id<MTLDevice> device_;
    id<MTLCommandQueue> command_queue_;
    id<MTLComputePipelineState> mask_pipeline_;
    id<MTLBuffer> input_buffer_;
    id<MTLBuffer> mask_buffer_;
    id<MTLBuffer> params_buffer_;
    int max_width_;
    int max_height_;
};

#else

// Placeholder implementations for non-Apple platforms
class MetalColorGradingProcessor {
public:
    auto Initialize(int, int) -> bool { return false; }
    auto Apply3WayColorGrading(...) -> bool { return false; }
    auto ApplyLUT3D(...) -> bool { return false; }
    static auto IsMetalAvailable() -> bool { return false; }
};

class MetalScopeGenerator {
public:
    auto Initialize(int, int) -> bool { return false; }
    auto GenerateVectorscope(...) -> bool { return false; }
};

class MetalDenoiseProcessor {
public:
    auto Initialize(int, int) -> bool { return false; }
    auto ApplyBilateralDenoise(...) -> bool { return false; }
};

class MetalROIRenderer {
public:
    auto Initialize(int, int) -> bool { return false; }
    auto RenderROI(...) -> bool { return false; }
};

class MetalMaskGenerator {
public:
    auto Initialize(int, int) -> bool { return false; }
    auto GenerateColorMask(...) -> bool { return false; }
};

#endif  // __APPLE__

/// Metal shader strings (WGSL-like syntax in Metal Shading Language)
namespace shaders {

/// OKLab color space conversion functions (shared between shaders)
const char* OKLabCommon = R"(
// OKLab color space conversion functions

float srgb_to_linear(float c) {
    if (c <= 0.04045) return c / 12.92;
    return pow((c + 0.055) / 1.055, 2.4);
}

float linear_to_srgb(float c) {
    if (c <= 0.0031308) return 12.92 * c;
    return 1.055 * pow(c, 1.0 / 2.4) - 0.055;
}

void rgb_to_oklab(float r, float g, float b, thread float& L, thread float& a, thread float& b_oklab) {
    float lr = srgb_to_linear(r);
    float lg = srgb_to_linear(g);
    float lb = srgb_to_linear(b);

    float l = 0.4122214708 * lr + 0.5363380520 * lg + 0.0514459929 * lb;
    float m = 0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb;
    float s = 0.0883024619 * lr + 0.2817188716 * lg + 0.6299787005 * lb;

    l = pow(l, 1.0/3.0);
    m = pow(m, 1.0/3.0);
    s = pow(s, 1.0/3.0);

    L = 0.2104542553 * l + 0.5779946307 * m + 0.0620708871 * s;
    a = 1.9779984951 * l - 2.4358239762 * m + 0.4578644762 * s;
    b_oklab = 0.0259040371 * l + 0.7827717664 * m - 0.8086757660 * s;
}

void oklab_to_rgb(float L, float a, float b_oklab, thread float& r, thread float& g, thread float& b) {
    float l = L + 0.3963377774 * a + 0.2158037573 * b_oklab;
    float m = L - 0.1055613458 * a - 0.0638541728 * b_oklab;
    float s = L - 0.0894841775 * a - 1.2914855480 * b_oklab;

    l = l * l * l;
    m = m * m * m;
    s = s * s * s;

    float lr = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s;
    float lg = -1.2684380046 * l + 2.6097575309 * m - 0.3413193965 * s;
    float lb = -0.0041960863 * l - 0.7034181814 * m + 1.7076147010 * s;

    r = linear_to_srgb(lr);
    g = linear_to_srgb(lg);
    b = linear_to_srgb(lb);
}
)";

const char* ColorGrading3WayShader = R"(
#include <metal_stdlib>
using namespace metal;

// Color grading parameters struct
struct ColorGradingParams {
    float shadows_hue;
    float shadows_sat;
    float shadows_lum;
    float midtones_hue;
    float midtones_sat;
    float midtones_lum;
    float highlights_hue;
    float highlights_sat;
    float highlights_lum;
    float global_hue;
    float global_sat;
    float blending;
    float balance;
};

// OKLab conversion functions (inline)
float srgb_to_linear(float c) {
    if (c <= 0.04045) return c / 12.92;
    return pow((c + 0.055) / 1.055, 2.4);
}

float linear_to_srgb(float c) {
    if (c <= 0.0031308) return 12.92 * c;
    return 1.055 * pow(c, 1.0 / 2.4) - 0.055;
}

void rgb_to_oklab(float r, float g, float b, thread float& L, thread float& a, thread float& b_oklab) {
    float lr = srgb_to_linear(r);
    float lg = srgb_to_linear(g);
    float lb = srgb_to_linear(b);

    float l = 0.4122214708 * lr + 0.5363380520 * lg + 0.0514459929 * lb;
    float m = 0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb;
    float s = 0.0883024619 * lr + 0.2817188716 * lg + 0.6299787005 * lb;

    l = pow(l, 1.0/3.0);
    m = pow(m, 1.0/3.0);
    s = pow(s, 1.0/3.0);

    L = 0.2104542553 * l + 0.5779946307 * m + 0.0620708871 * s;
    a = 1.9779984951 * l - 2.4358239762 * m + 0.4578644762 * s;
    b_oklab = 0.0259040371 * l + 0.7827717664 * m - 0.8086757660 * s;
}

void oklab_to_rgb(float L, float a, float b_oklab, thread float& r, thread float& g, thread float& b) {
    float l = L + 0.3963377774 * a + 0.2158037573 * b_oklab;
    float m = L - 0.1055613458 * a - 0.0638541728 * b_oklab;
    float s = L - 0.0894841775 * a - 1.2914855480 * b_oklab;

    l = l * l * l;
    m = m * m * m;
    s = s * s * s;

    float lr = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s;
    float lg = -1.2684380046 * l + 2.6097575309 * m - 0.3413193965 * s;
    float lb = -0.0041960863 * l - 0.7034181814 * m + 1.7076147010 * s;

    r = linear_to_srgb(lr);
    g = linear_to_srgb(lg);
    b = linear_to_srgb(lb);
}

float oklab_to_hue(float a, float b_oklab) {
    float hue = atan2(b_oklab, a) * 180.0 / M_PI_F;
    if (hue < 0.0) hue += 360.0;
    return hue;
}

float oklab_to_saturation(float a, float b_oklab) {
    return sqrt(a * a + b_oklab * b_oklab);
}

void rotate_hue(float a, float b_oklab, float hue_shift, thread float& a_out, thread float& b_out) {
    float hue = oklab_to_hue(a, b_oklab);
    float sat = oklab_to_saturation(a, b_oklab);
    hue = fmod(hue + hue_shift, 360.0);
    if (hue < 0.0) hue += 360.0;
    float hue_rad = hue * M_PI_F / 180.0;
    a_out = sat * cos(hue_rad);
    b_out = sat * sin(hue_rad);
}

float compute_zone_weight(float luminance, int zone, float blending, float balance) {
    float blend_factor = blending / 100.0;
    float balance_shift = balance / 100.0;

    float shadow_end = 0.33 + balance_shift - blend_factor * 0.15;
    float midtone_end = 0.66 + balance_shift + blend_factor * 0.15;
    float highlight_start = 0.66 + balance_shift + blend_factor * 0.15;

    if (zone == 0) { // Shadows
        if (luminance <= shadow_end) return 1.0;
        if (luminance < midtone_end) return (midtone_end - luminance) / (midtone_end - shadow_end);
        return 0.0;
    }
    if (zone == 1) { // Midtones
        if (luminance >= shadow_end && luminance <= midtone_end) return 1.0;
        if (luminance > 0.0 && luminance < shadow_end) return luminance / shadow_end;
        if (luminance > midtone_end && luminance < 1.0) return (1.0 - luminance) / (1.0 - midtone_end);
        return 0.0;
    }
    if (zone == 2) { // Highlights
        if (luminance >= highlight_start) return 1.0;
        if (luminance > shadow_end) return (luminance - shadow_end) / (highlight_start - shadow_end);
        return 0.0;
    }
    return 0.0;
}

kernel void color_grading_3way(
    device const uchar* input [[buffer(0)]],
    device uchar* output [[buffer(1)]],
    constant ColorGradingParams& params [[buffer(2)]],
    uint2 gid [[thread_position_in_grid]],
    uint2 dimensions [[threads_per_grid]]
) {
    uint x = gid.x;
    uint y = gid.y;

    if (x >= dimensions.x || y >= dimensions.y) return;

    uint idx = (y * dimensions.x + x) * 3;

    float r = input[idx] / 255.0;
    float g = input[idx + 1] / 255.0;
    float b = input[idx + 2] / 255.0;

    // Convert to OKLab
    float L, a, b_oklab;
    rgb_to_oklab(r, g, b, L, a, b_oklab);

    // Compute zone weights
    float shadow_weight = compute_zone_weight(L, 0, params.blending, params.balance);
    float midtone_weight = compute_zone_weight(L, 1, params.blending, params.balance);
    float highlight_weight = compute_zone_weight(L, 2, params.blending, params.balance);

    // Apply zone-based adjustments
    float hue_shift = 0.0;
    float sat_mult = 1.0;
    float lum_offset = 0.0;

    if (shadow_weight > 0.0) {
        hue_shift += params.shadows_hue * shadow_weight;
        sat_mult *= 1.0 + params.shadows_sat * shadow_weight * 0.01;
        lum_offset += params.shadows_lum * shadow_weight * 0.01;
    }

    if (midtone_weight > 0.0) {
        hue_shift += params.midtones_hue * midtone_weight;
        sat_mult *= 1.0 + params.midtones_sat * midtone_weight * 0.01;
        lum_offset += params.midtones_lum * midtone_weight * 0.01;
    }

    if (highlight_weight > 0.0) {
        hue_shift += params.highlights_hue * highlight_weight;
        sat_mult *= 1.0 + params.highlights_sat * highlight_weight * 0.01;
        lum_offset += params.highlights_lum * highlight_weight * 0.01;
    }

    // Global
    hue_shift += params.global_hue;
    sat_mult *= 1.0 + params.global_sat * 0.01;

    // Apply hue shift
    if (abs(hue_shift) > 0.001) {
        rotate_hue(a, b_oklab, hue_shift, a, b_oklab);
    }

    // Apply saturation
    a *= sat_mult;
    b_oklab *= sat_mult;

    // Apply luminance
    L = clamp(L + lum_offset, 0.0, 1.0);

    // Convert back to RGB
    oklab_to_rgb(L, a, b_oklab, r, g, b);

    output[idx] = uchar(r * 255.0);
    output[idx + 1] = uchar(g * 255.0);
    output[idx + 2] = uchar(b * 255.0);
}
)";

const char* VectorscopeShader = R"(
#include <metal_stdlib>
using namespace metal;

kernel void vectorscope(
    device const uchar* input [[buffer(0)]],
    device float* accumulation [[buffer(1)]],
    constant uint& image_width [[buffer(2)]],
    constant uint& image_height [[buffer(3)]],
    constant uint& scope_width [[buffer(4)]],
    constant uint& scope_height [[buffer(5)]],
    uint2 gid [[thread_position_in_grid]]
) {
    uint x = gid.x;
    uint y = gid.y;

    if (x >= image_width || y >= image_height) return;

    uint idx = (y * image_width + x) * 3;

    float r = input[idx] / 255.0;
    float g = input[idx + 1] / 255.0;
    float b = input[idx + 2] / 255.0;

    // Convert to YCbCr
    float Y = 0.299 * r + 0.587 * g + 0.114 * b;
    float Cb = (b - Y) * 0.565;
    float Cr = (r - Y) * 0.713;

    // Map to vectorscope
    uint center_x = scope_width / 2;
    uint center_y = scope_height / 2;
    float scale = min(scope_width, scope_height) / 2.0 * 0.9;

    uint vx = uint(center_x + Cr * scale);
    uint vy = uint(center_y - Cb * scale);

    if (vx < scope_width && vy < scope_height) {
        atomic_fetch_add_explicit(
            reinterpret_cast<device atomic<float*>>(accumulation + vy * scope_width + vx),
            1.0,
            memory_order_relaxed
        );
    }
}
)";

}  // namespace shaders

}  // namespace metal
}  // namespace gpu
}  // namespace alcedo