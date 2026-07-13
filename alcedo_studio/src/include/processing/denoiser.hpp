//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <vector>

namespace alcedo {
namespace denoise {

/// Denoising algorithm selection.
enum class DenoiseMethod {
  BM3D,     ///< 3D block-matching with collaborative filtering
  WAVELET,  ///< Wavelet-based (Haar) with soft thresholding
  BILATERAL,///< Bilateral filter for edge-preserving denoising
  NLM,      ///< Non-local means
};

/// BM3D denoising (simplified implementation).
///   - 2D DCT on each block
///   - 1D Haar wavelet across matched blocks
///   - Hard thresholding in 3D transform domain
///   - Inverse transforms and weighted aggregation
///
/// @param input        Single-channel input image, row-major, [0..1] float.
/// @param output       Denoised output image, same layout.
/// @param width        Image width in pixels.
/// @param height       Image height in pixels.
/// @param sigma        Noise standard deviation estimate.
/// @param block_size   Side length of square blocks (default 8).
/// @param search_window Search window side length (default 21).
/// @param max_blocks   Maximum number of similar blocks per group (default 16).
void bm3d_denoise(const std::vector<float>& input,
                  std::vector<float>& output,
                  int width,
                  int height,
                  float sigma,
                  int block_size = 8,
                  int search_window = 21,
                  int max_blocks = 16);

/// Wavelet-based denoising using Haar wavelets with soft thresholding.
///
/// @param input   Single-channel input image, row-major, [0..1].
/// @param output  Denoised output image, same layout.
/// @param width   Image width in pixels.
/// @param height  Image height in pixels.
/// @param sigma   Noise standard deviation.
/// @param levels  Number of decomposition levels (default 3).
void wavelet_denoise(const std::vector<float>& input,
                     std::vector<float>& output,
                     int width,
                     int height,
                     float sigma,
                     int levels = 3);

/// Bilateral filter for edge-preserving denoising.
///
/// @param input         Single-channel input image, row-major, [0..1].
/// @param output        Denoised output image, same layout.
/// @param width         Image width in pixels.
/// @param height        Image height in pixels.
/// @param sigma_spatial Spatial-domain standard deviation.
/// @param sigma_range   Range-domain standard deviation.
void bilateral_denoise(const std::vector<float>& input,
                       std::vector<float>& output,
                       int width,
                       int height,
                       float sigma_spatial,
                       float sigma_range);

/// Non-local means denoising.
///
/// @param input         Single-channel input image, row-major, [0..1].
/// @param output        Denoised output image, same layout.
/// @param width         Image width in pixels.
/// @param height        Image height in pixels.
/// @param h             Filtering strength parameter.
/// @param patch_size    Side length of comparison patch (default 5).
/// @param search_window Side length of search window (default 11).
void nlm_denoise(const std::vector<float>& input,
                 std::vector<float>& output,
                 int width,
                 int height,
                 float h,
                 int patch_size = 5,
                 int search_window = 11);

/// Split YCbCr channels, denoise color channels more aggressively.
///
/// @param input_y        Luma channel, [0..1].
/// @param input_cb       Cb chroma channel, [-0.5..0.5].
/// @param input_cr       Cr chroma channel, [-0.5..0.5].
/// @param output_y       Denoised luma channel.
/// @param output_cb      Denoised Cb channel.
/// @param output_cr      Denoised Cr channel.
/// @param width          Image width.
/// @param height         Image height.
/// @param luma_strength  Denoising strength for luma.
/// @param chroma_strength Denoising strength for chroma (typically higher).
/// @param method         Denoising algorithm to use.
void separate_chroma_denoise(const std::vector<float>& input_y,
                              const std::vector<float>& input_cb,
                              const std::vector<float>& input_cr,
                              std::vector<float>& output_y,
                              std::vector<float>& output_cb,
                              std::vector<float>& output_cr,
                              int width,
                              int height,
                              float luma_strength,
                              float chroma_strength,
                              DenoiseMethod method);

/// Main entry point that selects method based on parameters.
///
/// @param input           Single-channel input image, row-major, [0..1].
/// @param output          Denoised output image, same layout.
/// @param width           Image width in pixels.
/// @param height          Image height in pixels.
/// @param strength        Denoising strength [0..1] (mapped to sigma internally).
/// @param method          Denoising algorithm to use.
/// @param separate_chroma If true, apply separate chroma denoising.
/// @param input_cb        Cb channel (required if separate_chroma is true).
/// @param input_cr        Cr channel (required if separate_chroma is true).
/// @param output_cb       Denoised Cb channel (required if separate_chroma is true).
/// @param output_cr       Denoised Cr channel (required if separate_chroma is true).
void denoise_image(const std::vector<float>& input,
                   std::vector<float>& output,
                   int width,
                   int height,
                   float strength,
                   DenoiseMethod method,
                   bool separate_chroma,
                   const std::vector<float>& input_cb,
                   const std::vector<float>& input_cr,
                   std::vector<float>& output_cb,
                   std::vector<float>& output_cr);

/// Convenience overload for single-channel denoising.
///
/// @param input    Single-channel input image, row-major, [0..1].
/// @param output   Denoised output image, same layout.
/// @param width    Image width in pixels.
/// @param height   Image height in pixels.
/// @param strength Denoising strength [0..1].
/// @param method   Denoising algorithm to use.
void denoise_image(const std::vector<float>& input,
                   std::vector<float>& output,
                   int width,
                   int height,
                   float strength,
                   DenoiseMethod method);

}  // namespace denoise
}  // namespace alcedo