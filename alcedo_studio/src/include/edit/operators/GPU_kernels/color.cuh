//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

// CUDA implementations of color adjustment operators

#pragma once

#include <cuda_runtime.h>
#include <device_types.h>

#include "edit/operators/op_kernel.hpp"
#include "param.cuh"

namespace alcedo {
namespace CUDA {

GPU_FUNC float hls_oklch_wrap_hue(float h) {
  h = fmodf(h, 360.0f);
  if (h < 0.0f) h += 360.0f;
  return h;
}

GPU_FUNC float hls_oklch_hue_distance(float a, float b) {
  const float diff = fabsf(hls_oklch_wrap_hue(a) - hls_oklch_wrap_hue(b));
  return fminf(diff, 360.0f - diff);
}

GPU_FUNC float hls_oklch_smoothstep(float edge0, float edge1, float x) {
  const float denom = fmaxf(edge1 - edge0, 1e-6f);
  const float t     = fminf(fmaxf((x - edge0) / denom, 0.0f), 1.0f);
  return t * t * (3.0f - 2.0f * t);
}

GPU_FUNC float hls_oklch_hue_selection_weight(float hue_distance, float range) {
  const float half_weight_radius = fmaxf(range, 1.0f);
  const float t                  = hue_distance / half_weight_radius;
  return exp2f(-(t * t));
}

GPU_FUNC float hls_oklch_soft_floor(float x, float floor, float softness) {
  const float t = (x - floor) / fmaxf(softness, 1e-6f);
  if (t > 20.0f) return x;
  if (t < -20.0f) return floor;
  return floor + softness * logf(1.0f + expf(t));
}

GPU_FUNC float hls_oklch_acescc_decode(float acescc) {
  constexpr float kLog2Min      = -15.0f;
  constexpr float kLog2Denorm   = -16.0f;
  constexpr float kDenormOffset = 0.00001525878906f;
  constexpr float kA            = 9.72f;
  constexpr float kB            = 17.52f;

  const float encode_floor     = (kLog2Denorm + kA) / kB;
  const float denorm_threshold = (kLog2Min + kA) / kB;
  if (acescc < encode_floor) {
    return acescc - encode_floor;
  }
  if (acescc <= denorm_threshold) {
    return (exp2f(acescc * kB - kA) - kDenormOffset) * 2.0f;
  }
  return exp2f(acescc * kB - kA);
}

GPU_FUNC float hls_oklch_acescc_encode(float linear_ap1) {
  constexpr float kLog2Denorm   = -16.0f;
  constexpr float kDenormTrans  = 0.00003051757812f;
  constexpr float kDenormOffset = 0.00001525878906f;
  constexpr float kA            = 9.72f;
  constexpr float kB            = 17.52f;

  const float encode_floor = (kLog2Denorm + kA) / kB;
  if (linear_ap1 <= 0.0f) {
    return encode_floor + linear_ap1;
  }
  if (linear_ap1 < kDenormTrans) {
    return (log2f(kDenormOffset + linear_ap1 * 0.5f) + kA) / kB;
  }
  return (log2f(linear_ap1) + kA) / kB;
}

GPU_FUNC float3 hls_oklch_acescc_to_ap1(float3 acescc) {
  return make_float3(hls_oklch_acescc_decode(acescc.x), hls_oklch_acescc_decode(acescc.y),
                     hls_oklch_acescc_decode(acescc.z));
}

GPU_FUNC float3 hls_oklch_ap1_to_acescc(float3 ap1) {
  return make_float3(hls_oklch_acescc_encode(ap1.x), hls_oklch_acescc_encode(ap1.y),
                     hls_oklch_acescc_encode(ap1.z));
}

GPU_FUNC float3 hls_oklch_ap1_to_oklab(float3 ap1) {
  const float l = 0.62217537f * ap1.x + 0.34268438f * ap1.y + 0.02339492f * ap1.z;
  const float m = 0.26593478f * ap1.x + 0.62930460f * ap1.y + 0.10828100f * ap1.z;
  const float s = 0.09725037f * ap1.x + 0.18525749f * ap1.y + 0.77254586f * ap1.z;

  const float l_ = cbrtf(l);
  const float m_ = cbrtf(m);
  const float s_ = cbrtf(s);

  return make_float3(0.2104542553f * l_ + 0.7936177850f * m_ - 0.0040720468f * s_,
                     1.9779984951f * l_ - 2.4285922050f * m_ + 0.4505937099f * s_,
                     0.0259040371f * l_ + 0.7827717662f * m_ - 0.8086757660f * s_);
}

GPU_FUNC float3 hls_oklch_oklab_to_ap1(float3 lab) {
  const float l_ = lab.x + 0.3963377774f * lab.y + 0.2158037573f * lab.z;
  const float m_ = lab.x - 0.1055613458f * lab.y - 0.0638541728f * lab.z;
  const float s_ = lab.x - 0.0894841775f * lab.y - 1.2914855480f * lab.z;

  const float l = l_ * l_ * l_;
  const float m = m_ * m_ * m_;
  const float s = s_ * s_ * s_;

  return make_float3(2.09085732f * l - 1.16812363f * m + 0.10040848f * s,
                     -0.87435428f * l + 2.14592958f * m - 0.27429822f * s,
                     -0.05353206f * l - 0.36754978f * m + 1.34755888f * s);
}

GPU_FUNC float3 hls_oklch_fit_ap1_lower_gamut(float3 adjusted_ap1, float3 neutral_ap1) {
  constexpr float kLower = -1e-5f;
  float           scale  = 1.0f;

  if (adjusted_ap1.x < kLower && neutral_ap1.x > adjusted_ap1.x) {
    scale = fminf(scale, (neutral_ap1.x - kLower) / (neutral_ap1.x - adjusted_ap1.x));
  }
  if (adjusted_ap1.y < kLower && neutral_ap1.y > adjusted_ap1.y) {
    scale = fminf(scale, (neutral_ap1.y - kLower) / (neutral_ap1.y - adjusted_ap1.y));
  }
  if (adjusted_ap1.z < kLower && neutral_ap1.z > adjusted_ap1.z) {
    scale = fminf(scale, (neutral_ap1.z - kLower) / (neutral_ap1.z - adjusted_ap1.z));
  }

  scale = fminf(fmaxf(scale, 0.0f), 1.0f);
  return make_float3(neutral_ap1.x + (adjusted_ap1.x - neutral_ap1.x) * scale,
                     neutral_ap1.y + (adjusted_ap1.y - neutral_ap1.y) * scale,
                     neutral_ap1.z + (adjusted_ap1.z - neutral_ap1.z) * scale);
}

GPU_FUNC float hs_ap1_luminance(float3 ap1) {
  return 0.27222872f * ap1.x + 0.67408177f * ap1.y + 0.05368952f * ap1.z;
}

GPU_FUNC float hs_log2_luminance_from_acescc(float4 px) {
  const float3 ap1 = hls_oklch_acescc_to_ap1(make_float3(px.x, px.y, px.z));
  return log2f(fmaxf(hs_ap1_luminance(ap1), 1.0e-8f));
}

GPU_FUNC float hs_read_log_clamped(const float* __restrict src, int x, int y, int width,
                                   int height, size_t pitch_elems) {
  const int clamped_x = min(max(x, 0), width - 1);
  const int clamped_y = min(max(y, 0), height - 1);
  return src[static_cast<size_t>(clamped_y) * pitch_elems + static_cast<size_t>(clamped_x)];
}

GPU_FUNC float hs_range_weight(float center, float sample) {
  constexpr float kTextureDeadbandStops = 0.22f;
  constexpr float kRangeStops           = 0.42f;
  const float     edge_delta = fmaxf(fabsf(sample - center) - kTextureDeadbandStops, 0.0f);
  const float     d          = edge_delta / kRangeStops;
  return exp2f(-(d * d));
}

GPU_FUNC float hs_shadow_zone_weight(float mask_ref, GPUOperatorParams params) {
  const float shoulder_width = fmaxf(params.hs_shadow_log_width_ * 2.45f, 1.35f);
  const float shoulder_end_offset = fmaxf(params.hs_shadow_log_width_ * 0.35f, 0.20f);
  const float shoulder =
      1.0f - hls_oklch_smoothstep(params.hs_shadow_log_pivot_ - shoulder_width,
                                  params.hs_shadow_log_pivot_ - shoulder_end_offset, mask_ref);
  const float black_floor =
      hls_oklch_smoothstep(params.hs_shadow_log_pivot_ - 4.5f,
                           params.hs_shadow_log_pivot_ - 2.25f, mask_ref);
  return fminf(fmaxf(shoulder * black_floor, 0.0f), 1.0f);
}

GPU_FUNC float hs_texture_detail_weight(float detail) {
  return 1.0f - hls_oklch_smoothstep(0.28f, 0.88f, fabsf(detail));
}

GPU_FUNC float hs_shadow_detail_sign_weight(float detail) {
  if (detail >= 0.0f) {
    return 1.0f;
  }
  return 1.0f - 0.65f * hls_oklch_smoothstep(0.18f, 0.72f, -detail);
}

GPU_FUNC void hs_compute_masks(float mask_ref, float shadow_amount, float highlight_amount,
                               GPUOperatorParams params, float* shadow_mask,
                               float* highlight_mask) {
  const float raw_shadow = hs_shadow_zone_weight(mask_ref, params);
  const float raw_highlight =
      powf(hls_oklch_smoothstep(params.hs_highlight_log_pivot_,
                                params.hs_highlight_log_pivot_ + params.hs_highlight_log_width_,
                                mask_ref),
           0.33f);

  const bool both_active = fabsf(shadow_amount) > 1.0e-6f && fabsf(highlight_amount) > 1.0e-6f;
  *shadow_mask = both_active ? raw_shadow * (1.0f - 0.72f * raw_highlight) : raw_shadow;
  *highlight_mask = both_active ? raw_highlight * (1.0f - 0.48f * raw_shadow) : raw_highlight;
}

GPU_FUNC float hs_shadow_tonal_weight(float mask_ref, float shadow_mask,
                                      GPUOperatorParams params) {
  (void)shadow_mask;
  return hs_shadow_zone_weight(mask_ref, params);
}

GPU_FUNC float hs_shadow_base_delta_from_ref(float mask_ref, float shadow_amount,
                                             float highlight_amount, GPUOperatorParams params) {
  float shadow_mask = 0.0f;
  float highlight_mask = 0.0f;
  hs_compute_masks(mask_ref, shadow_amount, highlight_amount, params, &shadow_mask,
                   &highlight_mask);

  const float deep_shadow =
      1.0f - hls_oklch_smoothstep(params.hs_shadow_log_pivot_ - 3.2f,
                                  params.hs_shadow_log_pivot_ - 1.15f, mask_ref);
  const float shadow_weight = hs_shadow_tonal_weight(mask_ref, shadow_mask, params);
  const float highlight_overlap = fminf(fmaxf(highlight_mask, 0.0f), 1.0f);
  const float overlap_guard = 1.0f - 0.35f * highlight_overlap;
  const float shadow_lift_gain = 1.0f + 0.10f * deep_shadow * fmaxf(shadow_amount, 0.0f);
  return shadow_amount * 0.96f * shadow_weight * shadow_lift_gain * overlap_guard;
}

GPU_FUNC float hs_highlight_base_delta_from_ref(float mask_ref, float shadow_amount,
                                                float highlight_amount, GPUOperatorParams params) {
  float shadow_mask = 0.0f;
  float highlight_mask = 0.0f;
  hs_compute_masks(mask_ref, shadow_amount, highlight_amount, params, &shadow_mask,
                   &highlight_mask);
  return -highlight_amount * 1.04f * highlight_mask;
}

GPU_FUNC float hs_base_delta_from_ref(float mask_ref, float shadow_amount, float highlight_amount,
                                      GPUOperatorParams params) {
  return hs_shadow_base_delta_from_ref(mask_ref, shadow_amount, highlight_amount, params) +
         hs_highlight_base_delta_from_ref(mask_ref, shadow_amount, highlight_amount, params);
}

GPU_FUNC float hs_base_curve_slope(float mask_ref, float shadow_amount, float highlight_amount,
                                   GPUOperatorParams params) {
  constexpr float kEpsStops = 0.08f;
  const float delta_lo =
      hs_base_delta_from_ref(mask_ref - kEpsStops, shadow_amount, highlight_amount, params);
  const float delta_hi =
      hs_base_delta_from_ref(mask_ref + kEpsStops, shadow_amount, highlight_amount, params);
  return 1.0f + (delta_hi - delta_lo) / (2.0f * kEpsStops);
}

GPU_FUNC float hs_shadow_curve_slope(float mask_ref, float shadow_amount, float highlight_amount,
                                     GPUOperatorParams params) {
  constexpr float kEpsStops = 0.08f;
  const float delta_lo =
      hs_shadow_base_delta_from_ref(mask_ref - kEpsStops, shadow_amount, highlight_amount, params);
  const float delta_hi =
      hs_shadow_base_delta_from_ref(mask_ref + kEpsStops, shadow_amount, highlight_amount, params);
  return 1.0f + (delta_hi - delta_lo) / (2.0f * kEpsStops);
}

GPU_FUNC float hs_shadow_detail_weight(float mask_ref, float shadow_mask,
                                       GPUOperatorParams params) {
  const float tonal_weight = hs_shadow_tonal_weight(mask_ref, shadow_mask, params);
  const float noise_floor =
      hls_oklch_smoothstep(params.hs_shadow_log_pivot_ - 3.5f,
                           params.hs_shadow_log_pivot_ - 1.75f, mask_ref);
  return tonal_weight * noise_floor;
}

__global__ void HsBuildLogBaseHorizontalKernel(const float4* __restrict src,
                                               float* __restrict dst, int width, int height,
                                               size_t pitch_elems, GPUOperatorParams params) {
  const int x = blockIdx.x * blockDim.x + threadIdx.x;
  const int y = blockIdx.y * blockDim.y + threadIdx.y;
  if (x >= width || y >= height) return;

  const int    tap_count = params.hs_base_gaussian_tap_count_;
  const size_t offset    = static_cast<size_t>(y) * pitch_elems + static_cast<size_t>(x);
  if (tap_count <= 0) {
    dst[offset] = hs_log2_luminance_from_acescc(src[offset]);
    return;
  }

  const float center = hs_log2_luminance_from_acescc(src[offset]);
  float       base   = center * params.hs_base_gaussian_weights_[0];
  float       weight_sum = params.hs_base_gaussian_weights_[0];
  for (int tap = 1; tap < tap_count; ++tap) {
    const int ax = min(x + tap, width - 1);
    const int bx = max(x - tap, 0);
    const float wa = hs_log2_luminance_from_acescc(
        src[static_cast<size_t>(y) * pitch_elems + static_cast<size_t>(ax)]);
    const float wb = hs_log2_luminance_from_acescc(
        src[static_cast<size_t>(y) * pitch_elems + static_cast<size_t>(bx)]);
    const float spatial = params.hs_base_gaussian_weights_[tap];
    const float aw      = spatial * hs_range_weight(center, wa);
    const float bw      = spatial * hs_range_weight(center, wb);
    base += wa * aw + wb * bw;
    weight_sum += aw + bw;
  }
  dst[offset] = base / fmaxf(weight_sum, 1.0e-6f);
}

__global__ void HsBuildLogBaseVerticalKernel(const float* __restrict src, float* __restrict dst,
                                             int width, int height, size_t pitch_elems,
                                             GPUOperatorParams params) {
  const int x = blockIdx.x * blockDim.x + threadIdx.x;
  const int y = blockIdx.y * blockDim.y + threadIdx.y;
  if (x >= width || y >= height) return;

  const int    tap_count = params.hs_base_gaussian_tap_count_;
  const size_t offset    = static_cast<size_t>(y) * pitch_elems + static_cast<size_t>(x);
  if (tap_count <= 0) {
    dst[offset] = src[offset];
    return;
  }

  const float center = src[offset];
  float       base   = center * params.hs_base_gaussian_weights_[0];
  float       weight_sum = params.hs_base_gaussian_weights_[0];
  for (int tap = 1; tap < tap_count; ++tap) {
    const float a = hs_read_log_clamped(src, x, y + tap, width, height, pitch_elems);
    const float b = hs_read_log_clamped(src, x, y - tap, width, height, pitch_elems);
    const float spatial = params.hs_base_gaussian_weights_[tap];
    const float aw      = spatial * hs_range_weight(center, a);
    const float bw      = spatial * hs_range_weight(center, b);
    base += a * aw + b * bw;
    weight_sum += aw + bw;
  }
  dst[offset] = base / fmaxf(weight_sum, 1.0e-6f);
}

__global__ void HsCopyThroughKernel(const float4* __restrict src, float4* __restrict dst,
                                    int width, int height, size_t pitch_elems) {
  const int x = blockIdx.x * blockDim.x + threadIdx.x;
  const int y = blockIdx.y * blockDim.y + threadIdx.y;
  if (x >= width || y >= height) return;
  const size_t offset = static_cast<size_t>(y) * pitch_elems + static_cast<size_t>(x);
  dst[offset]         = src[offset];
}

__global__ void HsApplyLocalToneKernel(const float4* __restrict src,
                                       const float* __restrict base_log, float4* __restrict dst,
                                       int width, int height, size_t pitch_elems,
                                       GPUOperatorParams params) {
  const int x = blockIdx.x * blockDim.x + threadIdx.x;
  const int y = blockIdx.y * blockDim.y + threadIdx.y;
  if (x >= width || y >= height) return;

  const size_t offset = static_cast<size_t>(y) * pitch_elems + static_cast<size_t>(x);
  const float4 px     = src[offset];

  const float shadow_amount =
      (params.shadows_enabled_) ? fminf(fmaxf(params.shadows_offset_, -1.0f), 1.0f) : 0.0f;
  const float highlight_amount =
      (params.highlights_enabled_) ? fminf(fmaxf(-params.highlights_offset_ * 0.5f, -1.0f), 1.0f)
                                   : 0.0f;
  if (fabsf(shadow_amount) <= 1.0e-6f && fabsf(highlight_amount) <= 1.0e-6f) {
    dst[offset] = px;
    return;
  }

  const float3 source_ap1 = hls_oklch_acescc_to_ap1(make_float3(px.x, px.y, px.z));
  const float  source_log_y  = log2f(fmaxf(hs_ap1_luminance(source_ap1), 1.0e-8f));
  const float  base          = base_log[offset];
  const float  detail        = source_log_y - base;
  const float  mask_ref      = base;
  float        shadow_mask = 0.0f;
  float        highlight_mask = 0.0f;
  hs_compute_masks(mask_ref, shadow_amount, highlight_amount, params, &shadow_mask,
                   &highlight_mask);

  const float base_delta = hs_base_delta_from_ref(mask_ref, shadow_amount, highlight_amount, params);
  const float base_curve_slope =
      fminf(fmaxf(hs_base_curve_slope(mask_ref, shadow_amount, highlight_amount, params), 0.42f),
            1.65f);
  const float base_contrast_loss =
      fminf(fmaxf(1.0f / base_curve_slope - 1.0f, 0.0f), 0.62f);
  const float shadow_curve_slope =
      fminf(fmaxf(hs_shadow_curve_slope(mask_ref, shadow_amount, highlight_amount, params), 0.58f),
            1.35f);
  const float shadow_contrast_loss =
      fminf(fmaxf(1.0f / shadow_curve_slope - 1.0f, 0.0f), 0.46f);
  const float shadow_texture_zone = hs_shadow_detail_weight(mask_ref, shadow_mask, params);
  const float texture_detail = hs_texture_detail_weight(detail);
  const float shadow_detail_sign = hs_shadow_detail_sign_weight(detail);
  const float shadow_detail_zone = shadow_texture_zone * texture_detail * shadow_detail_sign;
  const float detail_boost =
      fmaxf(shadow_amount, 0.0f) * shadow_detail_zone *
          (0.06f + 0.24f * fmaxf(base_contrast_loss, shadow_contrast_loss)) +
      0.025f * fmaxf(-shadow_amount, 0.0f) * shadow_detail_zone -
      0.03f * fmaxf(highlight_amount, 0.0f) * highlight_mask;
  const float dark_edge_residual = hls_oklch_smoothstep(0.26f, 0.95f, -detail);
  const float dark_edge_relief =
      fmaxf(shadow_amount, 0.0f) * shadow_texture_zone * dark_edge_residual * 0.42f;
  const float detail_scale = fminf(1.15f, fmaxf(0.72f, 1.0f + detail_boost - dark_edge_relief));
  const float adjusted_log_y = base + base_delta + detail * detail_scale;
  const float log_delta = adjusted_log_y - source_log_y;

  const float rgb_scale = exp2f(fminf(fmaxf(log_delta, -3.5f), 3.5f));
  const float3 output_ap1 =
      make_float3(source_ap1.x * rgb_scale, source_ap1.y * rgb_scale, source_ap1.z * rgb_scale);
  const float3 output_acescc = hls_oklch_ap1_to_acescc(output_ap1);

  dst[offset] = make_float4(output_acescc.x, output_acescc.y, output_acescc.z, px.w);
}

struct GPU_HighlightShadowLocalToneStage {
  float*        base_log_         = nullptr;
  float*        temp_log_         = nullptr;
  size_t        allocated_elems_  = 0;
  int           cached_width_     = 0;
  int           cached_height_    = 0;
  size_t        cached_pitch_     = 0;
  std::uint64_t cached_key_       = 0;

  GPU_HighlightShadowLocalToneStage() = default;

  GPU_HighlightShadowLocalToneStage(const GPU_HighlightShadowLocalToneStage&) {}

  GPU_HighlightShadowLocalToneStage& operator=(const GPU_HighlightShadowLocalToneStage&) {
    ReleaseResources();
    return *this;
  }

  GPU_HighlightShadowLocalToneStage(GPU_HighlightShadowLocalToneStage&& other) noexcept
      : base_log_(other.base_log_),
        temp_log_(other.temp_log_),
        allocated_elems_(other.allocated_elems_),
        cached_width_(other.cached_width_),
        cached_height_(other.cached_height_),
        cached_pitch_(other.cached_pitch_),
        cached_key_(other.cached_key_) {
    other.base_log_ = nullptr;
    other.temp_log_ = nullptr;
    other.allocated_elems_ = 0;
    other.cached_width_ = 0;
    other.cached_height_ = 0;
    other.cached_pitch_ = 0;
    other.cached_key_ = 0;
  }

  GPU_HighlightShadowLocalToneStage& operator=(GPU_HighlightShadowLocalToneStage&& other) noexcept {
    if (this != &other) {
      ReleaseResources();
      base_log_ = other.base_log_;
      temp_log_ = other.temp_log_;
      allocated_elems_ = other.allocated_elems_;
      cached_width_ = other.cached_width_;
      cached_height_ = other.cached_height_;
      cached_pitch_ = other.cached_pitch_;
      cached_key_ = other.cached_key_;
      other.base_log_ = nullptr;
      other.temp_log_ = nullptr;
      other.allocated_elems_ = 0;
      other.cached_width_ = 0;
      other.cached_height_ = 0;
      other.cached_pitch_ = 0;
      other.cached_key_ = 0;
    }
    return *this;
  }

  ~GPU_HighlightShadowLocalToneStage() { ReleaseResources(); }

  void ReleaseResources() {
    if (base_log_) {
      cudaFree(base_log_);
      base_log_ = nullptr;
    }
    if (temp_log_) {
      cudaFree(temp_log_);
      temp_log_ = nullptr;
    }
    allocated_elems_ = 0;
    cached_width_ = 0;
    cached_height_ = 0;
    cached_pitch_ = 0;
    cached_key_ = 0;
  }

  void EnsureBuffers(int width, int height, size_t pitch_elems) {
    const size_t needed = pitch_elems * static_cast<size_t>(height);
    if (needed <= allocated_elems_) return;
    ReleaseResources();
    cudaMalloc(reinterpret_cast<void**>(&base_log_), needed * sizeof(float));
    cudaMalloc(reinterpret_cast<void**>(&temp_log_), needed * sizeof(float));
    allocated_elems_ = needed;
    cached_width_ = 0;
    cached_height_ = 0;
    cached_pitch_ = 0;
    cached_key_ = 0;
  }

  void Dispatch(float4* src, float4* dst, int width, int height, size_t pitch_elems,
                GPUOperatorParams& params, dim3 grid, dim3 block, cudaStream_t stream) {
    const bool active =
        params.hs_local_tone_enabled_ &&
        ((params.shadows_enabled_ && fabsf(params.shadows_offset_) > 1.0e-6f) ||
         (params.highlights_enabled_ && fabsf(params.highlights_offset_) > 1.0e-6f));
    if (!active || params.hs_base_gaussian_tap_count_ <= 0) {
      HsCopyThroughKernel<<<grid, block, 0, stream>>>(src, dst, width, height, pitch_elems);
      return;
    }

    EnsureBuffers(width, height, pitch_elems);
    const bool cache_valid = cached_key_ == params.hs_mask_base_cache_key_ &&
                             cached_width_ == width && cached_height_ == height &&
                             cached_pitch_ == pitch_elems;
    if (!cache_valid) {
      HsBuildLogBaseHorizontalKernel<<<grid, block, 0, stream>>>(src, temp_log_, width, height,
                                                                 pitch_elems, params);
      HsBuildLogBaseVerticalKernel<<<grid, block, 0, stream>>>(temp_log_, base_log_, width, height,
                                                               pitch_elems, params);
      cached_key_ = params.hs_mask_base_cache_key_;
      cached_width_ = width;
      cached_height_ = height;
      cached_pitch_ = pitch_elems;
    }

    HsApplyLocalToneKernel<<<grid, block, 0, stream>>>(src, base_log_, dst, width, height,
                                                       pitch_elems, params);
  }
};

GPU_FUNC float3 hls_oklch_evaluate_hue_curve(float hue, GPUOperatorParams& params,
                                             int profile_count) {
  constexpr float kEps = 1e-6f;
  float           sum_h = 0.0f;
  float           sum_l = 0.0f;
  float           sum_c = 0.0f;
  float           sum_weight = 0.0f;
  int             nearest = 0;
  float           nearest_dist =
      hls_oklch_hue_distance(hue, hls_oklch_wrap_hue(params.hls_profile_hues_[0]));

#pragma unroll
  for (int i = 0; i < OperatorParams::kHlsProfileCount; ++i) {
    if (i >= profile_count) {
      continue;
    }

    const float target_h = hls_oklch_wrap_hue(params.hls_profile_hues_[i]);
    const float hue_dist = hls_oklch_hue_distance(hue, target_h);
    if (hue_dist < nearest_dist) {
      nearest_dist = hue_dist;
      nearest      = i;
    }

    const float width  = fmaxf(params.hls_profile_hue_ranges_[i], 1.0f);
    const float weight = hls_oklch_hue_selection_weight(hue_dist, width);
    sum_h += params.hls_profile_adjustments_[i][0] * weight;
    sum_l += params.hls_profile_adjustments_[i][1] * weight;
    sum_c += params.hls_profile_adjustments_[i][2] * weight;
    sum_weight += weight;
  }

  if (sum_weight <= kEps) {
    return make_float3(params.hls_profile_adjustments_[nearest][0],
                       params.hls_profile_adjustments_[nearest][1],
                       params.hls_profile_adjustments_[nearest][2]);
  }

  const float inv_weight = 1.0f / sum_weight;
  return make_float3(sum_h * inv_weight, sum_l * inv_weight, sum_c * inv_weight);
}

struct GPU_HLSOpKernel : GPUPointOpTag {
  __device__ __forceinline__ void operator()(float4* p, GPUOperatorParams& params) const {
    if (!params.hls_enabled_ && !params.saturation_enabled_) return;

    const float kEps = 1e-6f;
    const float kPi  = 3.14159265358979323846f;
    const float3 source_acescc = make_float3(p->x, p->y, p->z);
    const float3 source_ap1    = hls_oklch_acescc_to_ap1(source_acescc);
    const float3 source_lab    = hls_oklch_ap1_to_oklab(source_ap1);
    const float  source_chroma = hypotf(source_lab.y, source_lab.z);
    if (source_chroma <= kEps) {
      return;
    }
    const float source_hue =
        hls_oklch_wrap_hue(atan2f(source_lab.z, source_lab.y) * (180.0f / kPi));

    float3 curve = make_float3(0.0f, 0.0f, 0.0f);
    if (params.hls_enabled_) {
      int profile_count = params.hls_profile_count_;
      if (profile_count < 1) {
        profile_count = 1;
      }
      if (profile_count > OperatorParams::kHlsProfileCount) {
        profile_count = OperatorParams::kHlsProfileCount;
      }

      curve = hls_oklch_evaluate_hue_curve(source_hue, params, profile_count);
    }
    const float saturation_scale =
        params.saturation_enabled_ ? fmaxf(params.saturation_offset_, 0.0f) : 1.0f;
    if (fabsf(curve.x) <= kEps && fabsf(curve.y) <= kEps && fabsf(curve.z) <= kEps &&
        fabsf(saturation_scale - 1.0f) <= kEps) {
      return;
    }

    const bool  has_curve = fabsf(curve.x) > kEps || fabsf(curve.y) > kEps ||
                            fabsf(curve.z) > kEps;
    float       protection = 0.0f;
    if (has_curve) {
      const float chroma_confidence    = hls_oklch_smoothstep(0.005f, 0.030f, source_chroma);
      const float shadow_confidence    = hls_oklch_smoothstep(0.005f, 0.050f, source_lab.x);
      const float highlight_confidence = 1.0f - hls_oklch_smoothstep(1.35f, 2.25f, source_lab.x);
      protection =
          fminf(fmaxf(chroma_confidence * shadow_confidence * highlight_confidence, 0.0f), 1.0f);
    }
    if (protection <= kEps && fabsf(saturation_scale - 1.0f) <= kEps) {
      return;
    }

    constexpr float kCurveGain = 2.25f;
    const float adjusted_hue_rad =
        hls_oklch_wrap_hue(source_hue + curve.x * kCurveGain * protection) * (kPi / 180.0f);
    const float adjusted_lightness =
        (fabsf(curve.y) > kEps && protection > kEps)
            ? hls_oklch_soft_floor(source_lab.x + curve.y * kCurveGain * 0.5f * protection, 0.0f,
                                   0.02f)
            : source_lab.x;
    const float chroma_strength = (curve.z >= 0.0f) ? 4.5f : 3.25f;
    const float adjusted_chroma =
        source_chroma * saturation_scale *
        exp2f(curve.z * kCurveGain * chroma_strength * protection);

    const float3 adjusted_lab =
        make_float3(adjusted_lightness, adjusted_chroma * cosf(adjusted_hue_rad),
                    adjusted_chroma * sinf(adjusted_hue_rad));
    const float3 neutral_lab  = make_float3(adjusted_lightness, 0.0f, 0.0f);
    const float3 neutral_ap1  = hls_oklch_oklab_to_ap1(neutral_lab);
    const float3 output_ap1 =
        hls_oklch_fit_ap1_lower_gamut(hls_oklch_oklab_to_ap1(adjusted_lab), neutral_ap1);
    const float3 output_acescc = hls_oklch_ap1_to_acescc(output_ap1);

    p->x = output_acescc.x;
    p->y = output_acescc.y;
    p->z = output_acescc.z;
  }
};

struct GPU_TintOpKernel : GPUPointOpTag {
  __device__ __forceinline__ void operator()(float4* p, GPUOperatorParams& params) const {
    if (!params.tint_enabled_) return;

    p->y += params.tint_offset_;
  }
};

struct GPU_VibranceOpKernel : GPUPointOpTag {
  __device__ __forceinline__ void operator()(float4* p, GPUOperatorParams& params) const {
    if (!params.vibrance_enabled_) return;

    float max_val  = fmaxf(fmaxf(p->x, p->y), p->z);
    float min_val  = fminf(fminf(p->x, p->y), p->z);
    float chroma   = max_val - min_val;

    // chroma in [0, max], vibrance_offset in [-100, 100]
    float strength = params.vibrance_offset_;

    // Protect already highly saturated color
    float falloff  = expf(-3.0f * chroma);

    float scale    = 1.0f + strength * falloff;

    if (params.vibrance_offset_ >= 0.0f) {
      float luma = p->x * 0.299f + p->y * 0.587f + p->z * 0.114f;

      p->x          = luma + (p->x - luma) * scale;
      p->y          = luma + (p->y - luma) * scale;
      p->z          = luma + (p->z - luma) * scale;

    } else {
      float avg = (p->x + p->y + p->z) / 3.0f;
      p->x += (avg - p->x) * (1.0f - scale);
      p->y += (avg - p->y) * (1.0f - scale);
      p->z += (avg - p->z) * (1.0f - scale);
    }
  }
};

struct GPU_ColorWheelOpKernel : GPUPointOpTag {
  __device__ __forceinline__ void operator()(float4* p, GPUOperatorParams& params) const {
    if (!params.color_wheel_enabled_) return;

    constexpr float kEps = 1e-6f;

    const float offset_r = params.lift_color_offset_[0] + params.lift_luminance_offset_;
    const float offset_g = params.lift_color_offset_[1] + params.lift_luminance_offset_;
    const float offset_b = params.lift_color_offset_[2] + params.lift_luminance_offset_;

    const float slope_r  = fmaxf(params.gain_color_offset_[0] + params.gain_luminance_offset_, kEps);
    const float slope_g  = fmaxf(params.gain_color_offset_[1] + params.gain_luminance_offset_, kEps);
    const float slope_b  = fmaxf(params.gain_color_offset_[2] + params.gain_luminance_offset_, kEps);

    const float power_r  =
        fmaxf(params.gamma_color_offset_[0] + params.gamma_luminance_offset_, kEps);
    const float power_g  =
        fmaxf(params.gamma_color_offset_[1] + params.gamma_luminance_offset_, kEps);
    const float power_b  =
        fmaxf(params.gamma_color_offset_[2] + params.gamma_luminance_offset_, kEps);

    const float base_r = fmaxf(p->x * slope_r + offset_r, 0.0f);
    const float base_g = fmaxf(p->y * slope_g + offset_g, 0.0f);
    const float base_b = fmaxf(p->z * slope_b + offset_b, 0.0f);

    // p->x               = fminf(fmaxf(powf(base_r, power_r), 0.0f), 1.0f);
    // p->y               = fminf(fmaxf(powf(base_g, power_g), 0.0f), 1.0f);
    // p->z               = fminf(fmaxf(powf(base_b, power_b), 0.0f), 1.0f);
    p->x               = powf(base_r, power_r);
    p->y               = powf(base_g, power_g);
    p->z               = powf(base_b, power_b);
  }
};
};  // namespace CUDA
};  // namespace alcedo
