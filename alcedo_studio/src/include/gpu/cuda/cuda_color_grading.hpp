//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cuda_runtime.h>
#include <cstdint>
#include <memory>

#include "edit/operators/color_grading_3way.hpp"

namespace alcedo {
namespace gpu {
namespace cuda {

/// CUDA kernel for 3-Way color grading with OKLab color space
/// @param input Input image data (RGB, 8-bit or float)
/// @param output Output image data
/// @param width Image width
/// @param height Image height
/// @param shadows_hue Shadows hue offset
/// @param shadows_sat Shadows saturation
/// @param shadows_lum Shadows luminance
/// @param midtones_hue Midtones hue offset
/// @param midtones_sat Midtones saturation
/// @param midtones_lum Midtones luminance
/// @param highlights_hue Highlights hue offset
/// @param highlights_sat Highlights saturation
/// @param highlights_lum Highlights luminance
/// @param global_hue Global hue offset
/// @param global_sat Global saturation multiplier
/// @param blending Zone blending factor
/// @param balance Zone balance shift
extern __global__ void ColorGrading3WayKernel(
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
);

/// CUDA kernel for LUT application (3D LUT interpolation)
/// @param input Input image data
/// @param output Output image data
/// @param width Image width
/// @param height Image height
/// @param lut_data 3D LUT data (RGB float array)
/// @param lut_size LUT dimension (e.g., 33)
extern __global__ void ApplyLUT3DKernel(
    const uint8_t* input,
    uint8_t* output,
    int width,
    int height,
    const float* lut_data,
    int lut_size
);

/// CUDA kernel for vectorscope generation
/// @param input Input image data (RGB)
/// @param accumulation Accumulation buffer for vectorscope
/// @param scope_width Vectorscope width
/// @param scope_height Vectorscope height
/// @param image_width Image width
/// @param image_height Image height
extern __global__ void VectorscopeKernel(
    const uint8_t* input,
    float* accumulation,
    int scope_width,
    int scope_height,
    int image_width,
    int image_height
);

/// CUDA kernel for RGB parade/waveform generation
/// @param input Input image data (RGB)
/// @param histograms Per-column histograms for each channel
/// @param parade_width Parade width (typically image width)
/// @param image_width Image width
/// @param image_height Image height
extern __global__ void RGBParadeKernel(
    const uint8_t* input,
    uint32_t* histograms,
    int parade_width,
    int image_width,
    int image_height
);

/// CUDA kernel for bilateral denoising
/// @param input Input image data
/// @param output Output image data
/// @param width Image width
/// @param height Image height
/// @param sigma_spatial Spatial sigma
/// @param sigma_range Range sigma
/// @param radius Kernel radius
extern __global__ void BilateralDenoiseKernel(
    const uint8_t* input,
    uint8_t* output,
    int width,
    int height,
    float sigma_spatial,
    float sigma_range,
    int radius
);

/// CUDA kernel for BM3D collaborative filtering step
/// @param blocks DCT-transformed blocks
/// @param filtered_blocks Filtered blocks after Wiener filtering
/// @param block_count Number of blocks
/// @param block_size Block size (typically 8)
/// @param threshold Hard threshold for coefficient filtering
extern __global__ void BM3DCollaborativeFilterKernel(
    const float* blocks,
    float* filtered_blocks,
    int block_count,
    int block_size,
    float threshold
);

/// CUDA kernel for ROI-aware rendering (only render visible region)
/// @param full_input Full resolution input
/// @param roi_output ROI region output
/// @param full_width Full image width
/// @param full_height Full image height
/// @param roi_x ROI start X
/// @param roi_y ROI start Y
/// @param roi_width ROI width
/// @param roi_height ROI height
/// @param scale_x Scale factor X
/// @param scale_y Scale factor Y
extern __global__ void ROIRenderKernel(
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
);

/// CUDA kernel for color/luminance mask generation
/// @param input Input image data
/// @param mask Output mask (grayscale)
/// @param width Image width
/// @param height Image height
/// @param hue_center Hue center for color mask
/// @param hue_range Hue range tolerance
/// @param sat_min Minimum saturation threshold
/// @param sat_max Maximum saturation threshold
/// @param lum_min Minimum luminance threshold
/// @param lum_max Maximum luminance threshold
/// @param mode 0 = color mask, 1 = luminance mask
extern __global__ void ParametricMaskKernel(
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
);

/// CUDA wrapper class for color grading operations
class CUDAColorGradingProcessor {
public:
    CUDAColorGradingProcessor();
    ~CUDAColorGradingProcessor();

    /// Initialize CUDA resources
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

private:
    cudaStream_t stream_;
    uint8_t* d_input_;
    uint8_t* d_output_;
    float* d_lut_;
    int max_width_;
    int max_height_;
    bool initialized_;
};

/// CUDA wrapper class for scope/analytics generation
class CUDAScopeGenerator {
public:
    CUDAScopeGenerator();
    ~CUDAScopeGenerator();

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
    cudaStream_t stream_;
    float* d_accumulation_;
    uint32_t* d_histograms_;
    uint8_t* d_scope_output_;
    int scope_width_;
    int scope_height_;
};

/// CUDA wrapper for denoising operations
class CUDADenoiseProcessor {
public:
    CUDADenoiseProcessor();
    ~CUDADenoiseProcessor();

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

    /// Apply BM3D denoise (multi-step)
    auto ApplyBM3DDenoise(
        const uint8_t* input,
        uint8_t* output,
        int width,
        int height,
        float strength
    ) -> bool;

private:
    cudaStream_t stream_;
    uint8_t* d_input_;
    uint8_t* d_output_;
    float* d_blocks_;
    float* d_filtered_blocks_;
    int max_width_;
    int max_height_;
};

/// CUDA wrapper for ROI rendering
class CUDAROIRenderer {
public:
    CUDAROIRenderer();
    ~CUDAROIRenderer();

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
    cudaStream_t stream_;
    uint8_t* d_full_input_;
    uint8_t* d_roi_output_;
    int max_roi_width_;
    int max_roi_height_;
};

/// CUDA wrapper for mask generation
class CUDAMaskGenerator {
public:
    CUDAMaskGenerator();
    ~CUDAMaskGenerator();

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
    cudaStream_t stream_;
    uint8_t* d_input_;
    uint8_t* d_mask_;
    int max_width_;
    int max_height_;
};

/// Utility functions for CUDA operations
namespace cuda_utils {

/// Check CUDA error and return error message
auto CheckCudaError(cudaError_t error) -> std::string;

/// Get optimal block size for given operation
auto GetOptimalBlockSize(int width, int height) -> dim3;

/// Get optimal grid size for given dimensions
auto GetOptimalGridSize(int width, int height, dim3 block_size) -> dim3;

/// Allocate pinned memory for faster transfers
auto AllocatePinnedMemory(size_t size) -> void*;

/// Free pinned memory
auto FreePinnedMemory(void* ptr) -> void;

/// Copy to device with pinned memory (async)
auto CopyToDeviceAsync(void* d_ptr, const void* h_ptr, size_t size, cudaStream_t stream) -> bool;

/// Copy from device with pinned memory (async)
auto CopyFromDeviceAsync(void* h_ptr, const void* d_ptr, size_t size, cudaStream_t stream) -> bool;

/// Synchronize stream
auto SynchronizeStream(cudaStream_t stream) -> bool;

}  // namespace cuda_utils

}  // namespace cuda
}  // namespace gpu
}  // namespace alcedo