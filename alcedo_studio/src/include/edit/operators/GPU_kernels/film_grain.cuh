//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cuda_runtime.h>

#include <cstdint>

#include "cuda/prng.hpp"
#include "edit/operators/GPU_kernels/param.cuh"

namespace alcedo {
namespace CUDA {

GPU_FUNC int FilmGrainReferenceCoord(int coord, int length, int roi_origin, float roi_scale,
                                     int reference_length, bool roi_enabled) {
  const float safe_length = fmaxf(static_cast<float>(length), 1.0f);
  const float full_length =
      fmaxf(static_cast<float>(reference_length > 0 ? reference_length : length), 1.0f);
  const float origin = roi_enabled ? static_cast<float>(roi_origin) : 0.0f;
  const float span   = roi_enabled ? fmaxf(roi_scale * full_length, 1.0f) : full_length;
  const float mapped = origin + ((static_cast<float>(coord) + 0.5f) * span / safe_length) - 0.5f;
  return static_cast<int>(floorf(mapped + 0.5f));
}

GPU_FUNC float FilmGrainChannel(float4 value, int channel) {
  if (channel == 0) {
    return value.x;
  }
  if (channel == 1) {
    return value.y;
  }
  return value.z;
}

GPU_FUNC std::uint64_t FilmGrainCellCounter(std::uint32_t grain_index, std::uint32_t draw_kind) {
  return 0x9e3779b97f4a7c15ULL + static_cast<std::uint64_t>(grain_index) * 4ULL +
         static_cast<std::uint64_t>(draw_kind);
}

GPU_FUNC bool FilmGrainBooleanAtPoint(const float4* __restrict src, float query_x, float query_y,
                                      int channel, int width, int height, size_t pitch_elems,
                                      const GPUOperatorParams& params) {
  constexpr float kPi            = 3.14159265358979323846f;
  constexpr float kProbability   = 0.9999f;

  const auto&     film_grain     = params.film_grain_;
  const float     radius         = fmaxf(film_grain.mean_radius_, 1.0e-4f);
  const float     max_radius     = fmaxf(film_grain.max_radius_, radius);
  const float     radius_squared = radius * radius;
  const float     expected_area  = fmaxf(kPi * radius_squared, 1.0e-8f);

  const int       min_cell_x     = max(0, static_cast<int>(floorf(query_x - max_radius)));
  const int       max_cell_x     = min(width - 1, static_cast<int>(floorf(query_x + max_radius)));
  const int       min_cell_y     = max(0, static_cast<int>(floorf(query_y - max_radius)));
  const int       max_cell_y     = min(height - 1, static_cast<int>(floorf(query_y + max_radius)));

  for (int cell_y = min_cell_y; cell_y <= max_cell_y; ++cell_y) {
    for (int cell_x = min_cell_x; cell_x <= max_cell_x; ++cell_x) {
      const size_t cell_offset =
          static_cast<size_t>(cell_y) * pitch_elems + static_cast<size_t>(cell_x);
      const float u_prob =
          fminf(fmaxf(FilmGrainChannel(src[cell_offset], channel), 0.0f), kProbability);
      const float lambda = -logf(1.0f - u_prob) / expected_area;

      const int   cell_ref_x =
          FilmGrainReferenceCoord(cell_x, width, params.render_roi_x_, params.render_roi_scale_x_,
                                  params.render_roi_reference_width_, params.render_roi_enabled_);
      const int cell_ref_y =
          FilmGrainReferenceCoord(cell_y, height, params.render_roi_y_, params.render_roi_scale_y_,
                                  params.render_roi_reference_height_, params.render_roi_enabled_);
      const std::uint64_t stream =
          cuda::PixelStream2D(cell_ref_x, cell_ref_y, static_cast<std::uint32_t>(channel));
      const std::uint32_t grain_count =
          cuda::SamplePoisson(film_grain.seed_, stream, 0x6a09e667f3bcc909ULL, lambda);

      for (std::uint32_t grain_index = 0; grain_index < grain_count; ++grain_index) {
        const float center_x =
            static_cast<float>(cell_x) +
            cuda::UniformFloat01(film_grain.seed_, stream, FilmGrainCellCounter(grain_index, 0U));
        const float center_y =
            static_cast<float>(cell_y) +
            cuda::UniformFloat01(film_grain.seed_, stream, FilmGrainCellCounter(grain_index, 1U));
        const float dx = query_x - center_x;
        const float dy = query_y - center_y;
        if (dx * dx + dy * dy <= radius_squared) {
          return true;
        }
      }
    }
  }

  return false;
}

GPU_FUNC float FilmGrainRenderChannel(const float4* __restrict src, float original, int x, int y,
                                      int channel, int width, int height, size_t pitch_elems,
                                      const GPUOperatorParams& params) {
  const auto& film_grain   = params.film_grain_;
  const int   sample_count = max(1, min(film_grain.samples_, 64));
  const float sigma        = fmaxf(film_grain.filter_sigma_, 0.0f);

  const int   ref_x =
      FilmGrainReferenceCoord(x, width, params.render_roi_x_, params.render_roi_scale_x_,
                              params.render_roi_reference_width_, params.render_roi_enabled_);
  const int ref_y =
      FilmGrainReferenceCoord(y, height, params.render_roi_y_, params.render_roi_scale_y_,
                              params.render_roi_reference_height_, params.render_roi_enabled_);
  const std::uint64_t sample_stream =
      cuda::PixelStream2D(ref_x, ref_y, static_cast<std::uint32_t>(channel + 17));

  float covered_sum = 0.0f;
  for (int sample_index = 0; sample_index < sample_count; ++sample_index) {
    const cuda::Float2 normal =
        cuda::NormalPair(film_grain.seed_, sample_stream, static_cast<std::uint64_t>(sample_index));
    const float query_x = fminf(fmaxf(static_cast<float>(x) + 0.5f + sigma * normal.x, 0.0f),
                                static_cast<float>(width) - 1.0e-4f);
    const float query_y = fminf(fmaxf(static_cast<float>(y) + 0.5f + sigma * normal.y, 0.0f),
                                static_cast<float>(height) - 1.0e-4f);
    covered_sum +=
        FilmGrainBooleanAtPoint(src, query_x, query_y, channel, width, height, pitch_elems, params)
            ? 1.0f
            : 0.0f;
  }

  const float grain_value = covered_sum / static_cast<float>(sample_count);
  const float strength    = fminf(fmaxf(film_grain.strength_, 0.0f), 1.0f);
  return original + strength * (grain_value - original);
}

__global__ void FilmGrainPixelWiseKernel(const float4* __restrict src, float4* __restrict dst,
                                         int width, int height, size_t pitch_elems,
                                         GPUOperatorParams params) {
  const int x = blockIdx.x * blockDim.x + threadIdx.x;
  const int y = blockIdx.y * blockDim.y + threadIdx.y;

  if (x >= width || y >= height) {
    return;
  }

  const float4* source     = (src != nullptr) ? src : dst;
  const size_t  offset     = static_cast<size_t>(y) * pitch_elems + static_cast<size_t>(x);
  const float4  original   = source[offset];
  const auto&   film_grain = params.film_grain_;
  const float   strength   = fminf(fmaxf(film_grain.strength_, 0.0f), 1.0f);

  if (!film_grain.enabled_ || !(strength > 0.0f)) {
    dst[offset] = original;
    return;
  }

  dst[offset] = make_float4(
      FilmGrainRenderChannel(source, original.x, x, y, 0, width, height, pitch_elems, params),
      FilmGrainRenderChannel(source, original.y, x, y, 1, width, height, pitch_elems, params),
      FilmGrainRenderChannel(source, original.z, x, y, 2, width, height, pitch_elems, params),
      original.w);
}

struct GPU_FilmGrainPixelWiseStage {
  void Dispatch(float4* src, float4* dst, int width, int height, size_t pitch_elems,
                GPUOperatorParams& params, dim3 grid, dim3 block, cudaStream_t stream) const {
    FilmGrainPixelWiseKernel<<<grid, block, 0, stream>>>(src, dst, width, height, pitch_elems,
                                                         params);
  }
};

}  // namespace CUDA
}  // namespace alcedo
