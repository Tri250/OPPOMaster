//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "gpu/cuda/cuda_color_grading.hpp"
#include <cmath>
#include <cstring>

namespace alcedo {
namespace gpu {
namespace cuda {

// ============================================================================
// Device Functions (OKLab Color Space Conversion)
// ============================================================================

__device__ float srgb_to_linear(float c) {
    if (c <= 0.04045f) return c / 12.92f;
    return powf((c + 0.055f) / 1.055f, 2.4f);
}

__device__ float linear_to_srgb(float c) {
    if (c <= 0.0031308f) return 12.92f * c;
    return 1.055f * powf(c, 1.0f / 2.4f) - 0.055f;
}

__device__ void rgb_to_oklab(float r, float g, float b, float* L, float* a, float* b_oklab) {
    float lr = srgb_to_linear(r);
    float lg = srgb_to_linear(g);
    float lb = srgb_to_linear(b);

    float l = 0.4122214708f * lr + 0.5363380520f * lg + 0.0514459929f * lb;
    float m = 0.2119034982f * lr + 0.6806995451f * lg + 0.1073969566f * lb;
    float s = 0.0883024619f * lr + 0.2817188716f * lg + 0.6299787005f * lb;

    l = cbrtf(l);
    m = cbrtf(m);
    s = cbrtf(s);

    *L = 0.2104542553f * l + 0.5779946307f * m + 0.0620708871f * s;
    *a = 1.9779984951f * l - 2.4358239762f * m + 0.4578644762f * s;
    *b_oklab = 0.0259040371f * l + 0.7827717664f * m - 0.8086757660f * s;
}

__device__ void oklab_to_rgb(float L, float a, float b_oklab, float* r, float* g, float* b) {
    float l = L + 0.3963377774f * a + 0.2158037573f * b_oklab;
    float m = L - 0.1055613458f * a - 0.0638541728f * b_oklab;
    float s = L - 0.0894841775f * a - 1.2914855480f * b_oklab;

    l = l * l * l;
    m = m * m * m;
    s = s * s * s;

    float lr = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s;
    float lg = -1.2684380046f * l + 2.6097575309f * m - 0.3413193965f * s;
    float lb = -0.0041960863f * l - 0.7034181814f * m + 1.7076147010f * s;

    *r = linear_to_srgb(lr);
    *g = linear_to_srgb(lg);
    *b = linear_to_srgb(lb);
}

__device__ float oklab_to_hue(float a, float b_oklab) {
    float hue = atan2f(b_oklab, a) * 180.0f / 3.14159265f;
    if (hue < 0.0f) hue += 360.0f;
    return hue;
}

__device__ float oklab_to_saturation(float a, float b_oklab) {
    return sqrtf(a * a + b_oklab * b_oklab);
}

__device__ void oklab_rotate_hue(float a, float b_oklab, float hue_shift, float* a_out, float* b_out) {
    float hue = oklab_to_hue(a, b_oklab);
    float sat = oklab_to_saturation(a, b_oklab);

    hue = fmodf(hue + hue_shift, 360.0f);
    if (hue < 0.0f) hue += 360.0f;

    float hue_rad = hue * 3.14159265f / 180.0f;
    *a_out = sat * cosf(hue_rad);
    *b_out = sat * sinf(hue_rad);
}

__device__ float compute_luminance(uint8_t r, uint8_t g, uint8_t b) {
    return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255.0f;
}

__device__ float compute_zone_weight(float luminance, int zone, float blending, float balance) {
    float blend_factor = blending / 100.0f;
    float balance_shift = balance / 100.0f;

    float shadow_start = 0.0f;
    float shadow_end = 0.33f + balance_shift - blend_factor * 0.15f;
    float midtone_start = 0.33f + balance_shift - blend_factor * 0.15f;
    float midtone_end = 0.66f + balance_shift + blend_factor * 0.15f;
    float highlight_start = 0.66f + balance_shift + blend_factor * 0.15f;
    float highlight_end = 1.0f;

    if (zone == 0) {  // Shadows
        if (luminance <= shadow_end) return 1.0f;
        if (luminance < midtone_end) return (midtone_end - luminance) / (midtone_end - shadow_end);
        return 0.0f;
    }
    if (zone == 1) {  // Midtones
        if (luminance >= midtone_start && luminance <= midtone_end) return 1.0f;
        if (luminance > shadow_start && luminance < midtone_start) return (luminance - shadow_start) / (midtone_start - shadow_start);
        if (luminance > midtone_end && luminance < highlight_end) return (highlight_end - luminance) / (highlight_end - midtone_end);
        return 0.0f;
    }
    if (zone == 2) {  // Highlights
        if (luminance >= highlight_start) return 1.0f;
        if (luminance > midtone_start) return (luminance - midtone_start) / (highlight_start - midtone_start);
        return 0.0f;
    }
    return 0.0f;
}

// ============================================================================
// Color Grading 3-Way Kernel
// ============================================================================

__global__ void ColorGrading3WayKernel(
    const uint8_t* input,
    uint8_t* output,
    int width,
    int height,
    float shadows_hue,
    float shadows_sat,
    float shadows_lum,
    float midtones_hue,
    float midtones_sat,
    float midtones_lum,
    float highlights_hue,
    float highlights_sat,
    float highlights_lum,
    float global_hue,
    float global_sat,
    float blending,
    float balance
) {
    int x = blockIdx.x * blockDim.x + threadIdx.x;
    int y = blockIdx.y * blockDim.y + threadIdx.y;

    if (x >= width || y >= height) return;

    size_t idx = (y * width + x) * 3;

    float r = input[idx] / 255.0f;
    float g = input[idx + 1] / 255.0f;
    float b = input[idx + 2] / 255.0f;

    // Convert to OKLab
    float L, a, b_oklab;
    rgb_to_oklab(r, g, b, &L, &a, &b_oklab);

    // Compute zone weights
    float shadow_weight = compute_zone_weight(L, 0, blending, balance);
    float midtone_weight = compute_zone_weight(L, 1, blending, balance);
    float highlight_weight = compute_zone_weight(L, 2, blending, balance);

    // Apply zone-based color adjustments
    float hue_shift = 0.0f;
    float sat_mult = 1.0f;
    float lum_offset = 0.0f;

    // Shadows
    if (shadow_weight > 0.0f) {
        hue_shift += shadows_hue * shadow_weight;
        sat_mult *= 1.0f + shadows_sat * shadow_weight * 0.01f;
        lum_offset += shadows_lum * shadow_weight * 0.01f;
    }

    // Midtones
    if (midtone_weight > 0.0f) {
        hue_shift += midtones_hue * shadow_weight;
        sat_mult *= 1.0f + midtones_sat * midtone_weight * 0.01f;
        lum_offset += midtones_lum * midtone_weight * 0.01f;
    }

    // Highlights
    if (highlight_weight > 0.0f) {
        hue_shift += highlights_hue * highlight_weight;
        sat_mult *= 1.0f + highlights_sat * highlight_weight * 0.01f;
        lum_offset += highlights_lum * highlight_weight * 0.01f;
    }

    // Global
    hue_shift += global_hue;
    sat_mult *= 1.0f + global_sat * 0.01f;

    // Apply hue shift
    if (fabsf(hue_shift) > 0.001f) {
        oklab_rotate_hue(a, b_oklab, hue_shift, &a, &b_oklab);
    }

    // Apply saturation
    if (fabsf(sat_mult - 1.0f) > 0.001f) {
        a *= sat_mult;
        b_oklab *= sat_mult;
    }

    // Apply luminance
    L = fminf(fmaxf(L + lum_offset, 0.0f), 1.0f);

    // Convert back to RGB
    oklab_to_rgb(L, a, b_oklab, &r, &g, &b);

    // Store result
    output[idx] = (uint8_t)(r * 255.0f);
    output[idx + 1] = (uint8_t)(g * 255.0f);
    output[idx + 2] = (uint8_t)(b * 255.0f);
}

// ============================================================================
// LUT 3D Application Kernel
// ============================================================================

__global__ void ApplyLUT3DKernel(
    const uint8_t* input,
    uint8_t* output,
    int width,
    int height,
    const float* lut_data,
    int lut_size
) {
    int x = blockIdx.x * blockDim.x + threadIdx.x;
    int y = blockIdx.y * blockDim.y + threadIdx.y;

    if (x >= width || y >= height) return;

    size_t idx = (y * width + x) * 3;

    // Normalize input to [0, lut_size-1]
    float r_idx = input[idx] / 255.0f * (lut_size - 1);
    float g_idx = input[idx + 1] / 255.0f * (lut_size - 1);
    float b_idx = input[idx + 2] / 255.0f * (lut_size - 1);

    // Trilinear interpolation
    int r0 = (int)r_idx;
    int g0 = (int)g_idx;
    int b0 = (int)b_idx;
    int r1 = min(r0 + 1, lut_size - 1);
    int g1 = min(g0 + 1, lut_size - 1);
    int b1 = min(b0 + 1, lut_size - 1);

    float r_frac = r_idx - r0;
    float g_frac = g_idx - g0;
    float b_frac = b_idx - b0;

    // Get 8 corner values from LUT (CUBE format: B G R order)
    size_t lut_base = b0 * lut_size * lut_size * 3;
    float lut000[3], lut001[3], lut010[3], lut011[3], lut100[3], lut101[3], lut110[3], lut111[3];

    for (int c = 0; c < 3; ++c) {
        lut000[c] = lut_data[lut_base + g0 * lut_size * 3 + r0 * 3 + c];
        lut001[c] = lut_data[lut_base + g0 * lut_size * 3 + r1 * 3 + c];
        lut010[c] = lut_data[lut_base + g1 * lut_size * 3 + r0 * 3 + c];
        lut011[c] = lut_data[lut_base + g1 * lut_size * 3 + r1 * 3 + c];
    }

    lut_base = b1 * lut_size * lut_size * 3;
    for (int c = 0; c < 3; ++c) {
        lut100[c] = lut_data[lut_base + g0 * lut_size * 3 + r0 * 3 + c];
        lut101[c] = lut_data[lut_base + g0 * lut_size * 3 + r1 * 3 + c];
        lut110[c] = lut_data[lut_base + g1 * lut_size * 3 + r0 * 3 + c];
        lut111[c] = lut_data[lut_base + g1 * lut_size * 3 + r1 * 3 + c];
    }

    // Trilinear interpolation
    for (int c = 0; c < 3; ++c) {
        float v00 = lut000[c] * (1 - r_frac) + lut001[c] * r_frac;
        float v01 = lut010[c] * (1 - r_frac) + lut011[c] * r_frac;
        float v10 = lut100[c] * (1 - r_frac) + lut101[c] * r_frac;
        float v11 = lut110[c] * (1 - r_frac) + lut111[c] * r_frac;

        float v0 = v00 * (1 - g_frac) + v01 * g_frac;
        float v1 = v10 * (1 - g_frac) + v11 * g_frac;

        float v = v0 * (1 - b_frac) + v1 * b_frac;

        output[idx + c] = (uint8_t)(v * 255.0f);
    }
}

// ============================================================================
// Vectorscope Kernel
// ============================================================================

__global__ void VectorscopeKernel(
    const uint8_t* input,
    float* accumulation,
    int scope_width,
    int scope_height,
    int image_width,
    int image_height
) {
    int x = blockIdx.x * blockDim.x + threadIdx.x;
    int y = blockIdx.y * blockDim.y + threadIdx.y;

    if (x >= image_width || y >= image_height) return;

    size_t idx = (y * image_width + x) * 3;

    float r = input[idx] / 255.0f;
    float g = input[idx + 1] / 255.0f;
    float b = input[idx + 2] / 255.0f;

    // Convert to YCbCr
    float Y = 0.299f * r + 0.587f * g + 0.114f * b;
    float Cb = (b - Y) * 0.565f;
    float Cr = (r - Y) * 0.713f;

    // Map to vectorscope coordinates
    int center_x = scope_width / 2;
    int center_y = scope_height / 2;
    float scale = min(scope_width, scope_height) / 2.0f * 0.9f;

    int vx = (int)(center_x + Cr * scale);
    int vy = (int)(center_y - Cb * scale);

    if (vx >= 0 && vx < scope_width && vy >= 0 && vy < scope_height) {
        atomicAdd(&accumulation[vy * scope_width + vx], 1.0f);
    }
}

// ============================================================================
// RGB Parade Kernel
// ============================================================================

__global__ void RGBParadeKernel(
    const uint8_t* input,
    uint32_t* histograms,
    int parade_width,
    int image_width,
    int image_height
) {
    int x = blockIdx.x * blockDim.x + threadIdx.x;
    int y = blockIdx.y * blockDim.y + threadIdx.y;

    if (x >= image_width || y >= image_height) return;

    size_t idx = (y * image_width + x) * 3;

    float scale_x = (float)parade_width / image_width;
    int parade_x = (int)(x * scale_x);

    for (int c = 0; c < 3; ++c) {
        uint8_t value = input[idx + c];
        atomicAdd(&histograms[c * parade_width * 256 + parade_x * 256 + value], 1);
    }
}

// ============================================================================
// Bilateral Denoise Kernel
// ============================================================================

__global__ void BilateralDenoiseKernel(
    const uint8_t* input,
    uint8_t* output,
    int width,
    int height,
    float sigma_spatial,
    float sigma_range,
    int radius
) {
    int x = blockIdx.x * blockDim.x + threadIdx.x;
    int y = blockIdx.y * blockDim.y + threadIdx.y;

    if (x >= width || y >= height) return;

    size_t idx = (y * width + x) * 3;

    for (int c = 0; c < 3; ++c) {
        float center_val = input[idx + c] / 255.0f;
        float sum = 0.0f;
        float weight_sum = 0.0f;

        for (int dy = -radius; dy <= radius; ++dy) {
            for (int dx = -radius; dx <= radius; ++dx) {
                int nx = x + dx;
                int ny = y + dy;

                if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;

                float neighbor_val = input[(ny * width + nx) * 3 + c] / 255.0f;

                float spatial_dist = sqrtf((float)(dx * dx + dy * dy));
                float range_dist = fabsf(center_val - neighbor_val);

                float spatial_weight = expf(-spatial_dist * spatial_dist / (2 * sigma_spatial * sigma_spatial));
                float range_weight = expf(-range_dist * range_dist / (2 * sigma_range * sigma_range));

                float weight = spatial_weight * range_weight;
                sum += neighbor_val * weight;
                weight_sum += weight;
            }
        }

        output[idx + c] = (uint8_t)(sum / weight_sum * 255.0f);
    }
}

// ============================================================================
// BM3D Collaborative Filter Kernel
// ============================================================================

__global__ void BM3DCollaborativeFilterKernel(
    const float* blocks,
    float* filtered_blocks,
    int block_count,
    int block_size,
    float threshold
) {
    int block_idx = blockIdx.x * blockDim.x + threadIdx.x;

    if (block_idx >= block_count) return;

    // Simplified Wiener filtering on DCT coefficients
    // Full BM3D would have more complex group matching and aggregation

    for (int i = 0; i < block_size * block_size; ++i) {
        size_t idx = block_idx * block_size * block_size + i;
        float coef = blocks[idx];

        // Hard thresholding
        if (fabsf(coef) < threshold) {
            filtered_blocks[idx] = 0.0f;
        } else {
            filtered_blocks[idx] = coef;
        }
    }
}

// ============================================================================
// ROI Render Kernel
// ============================================================================

__global__ void ROIRenderKernel(
    const uint8_t* full_input,
    uint8_t* roi_output,
    int full_width,
    int full_height,
    int roi_x,
    int roi_y,
    int roi_width,
    int roi_height,
    float scale_x,
    float scale_y
) {
    int x = blockIdx.x * blockDim.x + threadIdx.x;
    int y = blockIdx.y * blockDim.y + threadIdx.y;

    if (x >= roi_width || y >= roi_height) return;

    // Map ROI coordinates to full image coordinates
    int src_x = roi_x + (int)(x * scale_x);
    int src_y = roi_y + (int)(y * scale_y);

    if (src_x < 0 || src_x >= full_width || src_y < 0 || src_y >= full_height) {
        // Fill with transparent/black for out-of-bounds
        roi_output[(y * roi_width + x) * 4] = 0;
        roi_output[(y * roi_width + x) * 4 + 1] = 0;
        roi_output[(y * roi_width + x) * 4 + 2] = 0;
        roi_output[(y * roi_width + x) * 4 + 3] = 0;
        return;
    }

    // Copy pixel (with bilinear interpolation if scaling)
    size_t src_idx = (src_y * full_width + src_x) * 3;
    size_t dst_idx = (y * roi_width + x) * 4;

    roi_output[dst_idx] = full_input[src_idx];
    roi_output[dst_idx + 1] = full_input[src_idx + 1];
    roi_output[dst_idx + 2] = full_input[src_idx + 2];
    roi_output[dst_idx + 3] = 255;
}

// ============================================================================
// Parametric Mask Kernel
// ============================================================================

__device__ void rgb_to_hsl(float r, float g, float b, float* h, float* s, float* l) {
    float max_c = fmaxf(r, fmaxf(g, b));
    float min_c = fminf(r, fminf(g, b));
    *l = (max_c + min_c) / 2.0f;
    float delta = max_c - min_c;

    if (delta < 0.001f) {
        *h = 0.0f;
        *s = 0.0f;
    } else {
        *s = (*l > 0.5f) ? (delta / (2.0f - max_c - min_c)) : (delta / (max_c + min_c));

        if (max_c == r) {
            *h = 60.0f * fmodf((g - b) / delta, 6.0f);
        } else if (max_c == g) {
            *h = 60.0f * ((b - r) / delta + 2.0f);
        } else {
            *h = 60.0f * ((r - g) / delta + 4.0f);
        }

        if (*h < 0.0f) *h += 360.0f;
    }
}

__global__ void ParametricMaskKernel(
    const uint8_t* input,
    uint8_t* mask,
    int width,
    int height,
    float hue_center,
    float hue_range,
    float sat_min,
    float sat_max,
    float lum_min,
    float lum_max,
    int mode
) {
    int x = blockIdx.x * blockDim.x + threadIdx.x;
    int y = blockIdx.y * blockDim.y + threadIdx.y;

    if (x >= width || y >= height) return;

    size_t idx = (y * width + x) * 3;

    float r = input[idx] / 255.0f;
    float g = input[idx + 1] / 255.0f;
    float b = input[idx + 2] / 255.0f;

    if (mode == 0) {  // Color mask
        float h, s, l;
        rgb_to_hsl(r, g, b, &h, &s, &l);

        // Check hue range (handle wrap-around)
        bool hue_match = false;
        float hue_dist = 360.0f;

        if (h >= hue_center - hue_range && h <= hue_center + hue_range) {
            hue_match = true;
            hue_dist = fabsf(h - hue_center);
        } else if (hue_center - hue_range < 0.0f) {
            if (h >= hue_center - hue_range + 360.0f) {
                hue_match = true;
                hue_dist = 360.0f - fabsf(h - hue_center);
            }
        } else if (hue_center + hue_range > 360.0f) {
            if (h <= hue_center + hue_range - 360.0f) {
                hue_match = true;
                hue_dist = fabsf(h - hue_center);
            }
        }

        bool sat_match = (s >= sat_min && s <= sat_max);
        bool lum_match = (l >= lum_min && l <= lum_max);

        float mask_val = 0.0f;
        if (hue_match && sat_match && lum_match) {
            float hue_factor = 1.0f - hue_dist / hue_range;
            float sat_factor = 1.0f - fabsf(s - (sat_min + sat_max) / 2) / ((sat_max - sat_min) / 2);
            float lum_factor = 1.0f - fabsf(l - (lum_min + lum_max) / 2) / ((lum_max - lum_min) / 2);
            mask_val = hue_factor * sat_factor * lum_factor;
        }

        mask[y * width + x] = (uint8_t)(mask_val * 255.0f);
    } else {  // Luminance mask
        float luminance = compute_luminance(input[idx], input[idx + 1], input[idx + 2]);

        // Apply threshold with feathering
        float feather = 0.1f;
        float mask_val = 255.0f;

        if (luminance < lum_min - feather) {
            mask_val = 0.0f;
        } else if (luminance < lum_min + feather) {
            mask_val *= (luminance - (lum_min - feather)) / (2 * feather);
        }

        if (luminance > lum_max + feather) {
            mask_val = 0.0f;
        } else if (luminance > lum_max - feather) {
            mask_val *= ((lum_max + feather) - luminance) / (2 * feather);
        }

        mask[y * width + x] = (uint8_t)(mask_val);
    }
}

// ============================================================================
// CUDA Processor Classes Implementation
// ============================================================================

CUDAColorGradingProcessor::CUDAColorGradingProcessor()
    : stream_(nullptr)
    , d_input_(nullptr)
    , d_output_(nullptr)
    , d_lut_(nullptr)
    , max_width_(0)
    , max_height_(0)
    , initialized_(false) {}

CUDAColorGradingProcessor::~CUDAColorGradingProcessor() {
    if (stream_) cudaStreamDestroy(stream_);
    if (d_input_) cudaFree(d_input_);
    if (d_output_) cudaFree(d_output_);
    if (d_lut_) cudaFree(d_lut_);
}

auto CUDAColorGradingProcessor::Initialize(int max_width, int max_height) -> bool {
    max_width_ = max_width;
    max_height_ = max_height;

    cudaStreamCreate(&stream_);

    size_t image_size = max_width * max_height * 3;
    cudaMalloc(&d_input_, image_size);
    cudaMalloc(&d_output_, image_size);

    initialized_ = true;
    return true;
}

auto CUDAColorGradingProcessor::Apply3WayColorGrading(
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
) -> bool {
    if (!initialized_ || width > max_width_ || height > max_height_) return false;

    size_t image_size = width * height * 3;

    cudaMemcpyAsync(d_input_, input, image_size, cudaMemcpyHostToDevice, stream_);

    dim3 block(16, 16);
    dim3 grid((width + 15) / 16, (height + 15) / 16);

    ColorGrading3WayKernel<<<grid, block, 0, stream_>>>(
        d_input_, d_output_, width, height,
        shadows.hue_offset, shadows.saturation, shadows.luminance,
        midtones.hue_offset, midtones.saturation, midtones.luminance,
        highlights.hue_offset, highlights.saturation, highlights.luminance,
        global.hue_offset, global.saturation,
        blending, balance
    );

    cudaMemcpyAsync(output, d_output_, image_size, cudaMemcpyDeviceToHost, stream_);
    cudaStreamSynchronize(stream_);

    return true;
}

auto CUDAColorGradingProcessor::ApplyLUT3D(
    const uint8_t* input,
    uint8_t* output,
    int width,
    int height,
    const float* lut_data,
    int lut_size
) -> bool {
    if (!initialized_) return false;

    size_t image_size = width * height * 3;
    size_t lut_size_bytes = lut_size * lut_size * lut_size * 3 * sizeof(float);

    cudaMemcpyAsync(d_input_, input, image_size, cudaMemcpyHostToDevice, stream_);
    cudaMemcpyAsync(d_lut_, lut_data, lut_size_bytes, cudaMemcpyHostToDevice, stream_);

    dim3 block(16, 16);
    dim3 grid((width + 15) / 16, (height + 15) / 16);

    ApplyLUT3DKernel<<<grid, block, 0, stream_>>>(d_input_, d_output_, width, height, d_lut_, lut_size);

    cudaMemcpyAsync(output, d_output_, image_size, cudaMemcpyDeviceToHost, stream_);
    cudaStreamSynchronize(stream_);

    return true;
}

// Additional class implementations would follow similar patterns...

namespace cuda_utils {

auto CheckCudaError(cudaError_t error) -> std::string {
    if (error == cudaSuccess) return "";
    return cudaGetErrorString(error);
}

auto GetOptimalBlockSize(int width, int height) -> dim3 {
    return dim3(16, 16);
}

auto GetOptimalGridSize(int width, int height, dim3 block_size) -> dim3 {
    return dim3((width + block_size.x - 1) / block_size.x, (height + block_size.y - 1) / block_size.y);
}

auto AllocatePinnedMemory(size_t size) -> void* {
    void* ptr = nullptr;
    cudaMallocHost(&ptr, size);
    return ptr;
}

auto FreePinnedMemory(void* ptr) -> void {
    cudaFreeHost(ptr);
}

auto CopyToDeviceAsync(void* d_ptr, const void* h_ptr, size_t size, cudaStream_t stream) -> bool {
    return cudaMemcpyAsync(d_ptr, h_ptr, size, cudaMemcpyHostToDevice, stream) == cudaSuccess;
}

auto CopyFromDeviceAsync(void* h_ptr, const void* d_ptr, size_t size, cudaStream_t stream) -> bool {
    return cudaMemcpyAsync(h_ptr, d_ptr, size, cudaMemcpyDeviceToHost, stream) == cudaSuccess;
}

auto SynchronizeStream(cudaStream_t stream) -> bool {
    return cudaStreamSynchronize(stream) == cudaSuccess;
}

}  // namespace cuda_utils

}  // namespace cuda
}  // namespace gpu
}  // namespace alcedo