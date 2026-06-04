//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

// CUDA implementations of color adjustment operators

#pragma once

#include <array>
#include <vector>

#include <cuda_runtime.h>
#include <device_types.h>

#include "edit/operators/op_kernel.hpp"
#include "param.cuh"

#define GPU_HD_FUNC __host__ __device__ __forceinline__

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

GPU_HD_FUNC float hls_oklch_smoothstep(float edge0, float edge1, float x) {
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

GPU_HD_FUNC float hs_lerp(float a, float b, float t) { return a + (b - a) * t; }

constexpr float kHsAcesccMiddleGray = 0.41358840f;
constexpr float kHsAcesccCodePerEv  = 1.0f / 17.52f;

GPU_FUNC float hs_ap1_intensity(float3 ap1) {
  return 0.27222872f * ap1.x + 0.67408177f * ap1.y + 0.05368952f * ap1.z;
}

GPU_FUNC float hs_log_intensity_from_acescc(float4 px) {
  const float3 ap1 = hls_oklch_acescc_to_ap1(make_float3(px.x, px.y, px.z));
  return hls_oklch_acescc_encode(fmaxf(hs_ap1_intensity(ap1), 1.0e-6f));
}

template <size_t N>
GPU_HD_FUNC float hs_piecewise_linear(const float (&xs)[N], const float (&ys)[N], float x) {
  if (x <= xs[0]) {
    return ys[0];
  }
  if (x >= xs[N - 1]) {
    return ys[N - 1];
  }

  for (size_t i = 0; i + 1 < N; ++i) {
    if (x <= xs[i + 1]) {
      const float span = fmaxf(xs[i + 1] - xs[i], 1.0e-6f);
      const float t    = fminf(fmaxf((x - xs[i]) / span, 0.0f), 1.0f);
      return hs_lerp(ys[i], ys[i + 1], t);
    }
  }
  return ys[N - 1];
}

GPU_HD_FUNC float hs_relative_ev_from_log_intensity(float log_intensity) {
  return (log_intensity - kHsAcesccMiddleGray) / kHsAcesccCodePerEv;
}

GPU_HD_FUNC float hs_shadow_profile_ev(float relative_ev) {
  constexpr float kXs[] = {-9.0f, -7.0f, -5.4f, -4.3f, -3.1f, -2.0f, -0.5f, 1.0f};
  constexpr float kYs[] = {0.02f, 0.35f, 0.82f, 0.98f, 0.72f, 0.42f, 0.08f, 0.0f};
  return hs_piecewise_linear(kXs, kYs, relative_ev);
}

GPU_HD_FUNC float hs_highlight_profile_ev(float relative_ev) {
  constexpr float kXs[] = {-1.0f, 0.0f, 1.2f, 2.8f, 4.5f, 6.5f, 8.0f};
  constexpr float kYs[] = {0.0f, 0.03f, 0.22f, 0.60f, 0.95f, 1.08f, 0.92f};
  return hs_piecewise_linear(kXs, kYs, relative_ev);
}

constexpr float kHsHighlightStrengthScale = 1.5f;

GPU_FUNC float hs_read_l_clamped(const float* __restrict src, int x, int y, int width,
                                 int height, size_t pitch_elems) {
  const int clamped_x = min(max(x, 0), width - 1);
  const int clamped_y = min(max(y, 0), height - 1);
  return src[static_cast<size_t>(clamped_y) * pitch_elems + static_cast<size_t>(clamped_x)];
}

GPU_FUNC float hs_bilateral_range_weight(float center_l, float sample_l) {
  constexpr float kDeadbandL = 0.018f;
  constexpr float kSigmaL = 0.075f;
  const float     delta = fmaxf(fabsf(sample_l - center_l) - kDeadbandL, 0.0f);
  const float     normalized = delta / kSigmaL;
  return exp2f(-(normalized * normalized));
}

GPU_HD_FUNC float hs_shadow_region(float reference_l) {
  const float enters_above_black = hls_oklch_smoothstep(0.020f, 0.115f, reference_l);
  const float exits_midtones = 1.0f - hls_oklch_smoothstep(0.405f, 0.670f, reference_l);
  return fminf(fmaxf(enters_above_black * exits_midtones, 0.0f), 1.0f);
}

GPU_HD_FUNC float hs_highlight_region(float reference_l) {
  const float enters_upper_mid = hls_oklch_smoothstep(0.470f, 0.800f, reference_l);
  const float soft_white_tail = 1.0f - 0.08f * hls_oklch_smoothstep(1.100f, 1.720f, reference_l);
  return fminf(fmaxf(enters_upper_mid * soft_white_tail, 0.0f), 1.0f);
}

GPU_HD_FUNC void hs_regions(float reference_l, float shadow_amount, float highlight_amount,
                            float* shadow_region, float* highlight_region) {
  const float raw_shadow = hs_shadow_region(reference_l);
  const float raw_highlight = hs_highlight_region(reference_l);
  const bool  both_active =
      fabsf(shadow_amount) > 1.0e-6f && fabsf(highlight_amount) > 1.0e-6f;
  *shadow_region = both_active ? raw_shadow * (1.0f - 0.65f * raw_highlight) : raw_shadow;
  *highlight_region = both_active ? raw_highlight * (1.0f - 0.30f * raw_shadow) : raw_highlight;
}

GPU_HD_FUNC float hs_shadow_l_transform(float source_l, float amount, float region) {
  const float lift = fmaxf(amount, 0.0f);
  const float darken = fmaxf(-amount, 0.0f);
  const float nonnegative_l = fmaxf(source_l, 0.0f);
  const float lift_shape = nonnegative_l * expf(-nonnegative_l / 0.330f);
  const float lift_toe = hls_oklch_smoothstep(0.018f, 0.105f, nonnegative_l);
  const float lift_headroom = 1.0f - hls_oklch_smoothstep(0.570f, 0.760f, nonnegative_l);
  const float lift_delta = lift * region * lift_toe * lift_headroom * 0.90f * lift_shape;
  const float darken_delta =
      darken * region * 0.24f * nonnegative_l * (1.0f - expf(-nonnegative_l / 0.280f));
  return source_l + lift_delta - darken_delta;
}

GPU_HD_FUNC float hs_highlight_l_transform(float source_l, float amount, float region) {
  const float reduce = fmaxf(amount, 0.0f);
  const float boost = fmaxf(-amount, 0.0f);
  const float nonnegative_l = fmaxf(source_l, 0.0f);
  const float distance = fmaxf(nonnegative_l - 0.555f, 0.0f);
  const float onset = hls_oklch_smoothstep(0.555f, 0.760f, nonnegative_l);
  const float reduce_delta = reduce * region * onset * (0.190f * kHsHighlightStrengthScale) *
                             (1.0f - expf(-distance / 0.310f));
  const float boost_delta = boost * region * onset * 0.155f * (1.0f - expf(-distance / 0.360f));
  return source_l + boost_delta - reduce_delta;
}

GPU_HD_FUNC float hs_apply_reference_curve(float reference_l, float shadow_amount,
                                           float highlight_amount,
                                           float* shadow_region = nullptr,
                                           float* highlight_region = nullptr) {
  const float relative_ev = hs_relative_ev_from_log_intensity(reference_l);
  const float shadow_lift = fmaxf(shadow_amount, 0.0f) * hs_shadow_profile_ev(relative_ev);
  const float shadow_darken =
      fmaxf(-shadow_amount, 0.0f) * 0.55f * hs_shadow_profile_ev(relative_ev);
  const float highlight_reduce =
      fmaxf(highlight_amount, 0.0f) * kHsHighlightStrengthScale *
      hs_highlight_profile_ev(relative_ev);
  const float highlight_boost =
      fmaxf(-highlight_amount, 0.0f) * 0.65f * hs_highlight_profile_ev(relative_ev);
  const float practical_dark =
      hls_oklch_smoothstep(-5.85f, -3.95f, relative_ev) *
      (1.0f - hls_oklch_smoothstep(-3.20f, -1.65f, relative_ev));
  const float fill_plateau =
      hls_oklch_smoothstep(-5.55f, -3.30f, relative_ev) *
      (1.0f - 0.45f * hls_oklch_smoothstep(-2.65f, -0.20f, relative_ev));
  const float deep_toe_fill =
      shadow_lift * (1.0f - hls_oklch_smoothstep(-7.35f, -4.95f, relative_ev)) * 0.28f;
  const float shadow_fill_lift =
      shadow_lift * (0.62f * practical_dark + 0.14f * fill_plateau) + deep_toe_fill;
  const float lifted_relative_ev = relative_ev + 0.24f * (shadow_lift + 0.84f * shadow_fill_lift);
  const float combo_shadow_rollback =
      ((shadow_lift > 1.0e-6f && highlight_reduce > 1.0e-6f) ? 1.0f : 0.0f) * shadow_fill_lift *
      hls_oklch_smoothstep(-2.00f, -0.60f, lifted_relative_ev) *
      (1.0f - hls_oklch_smoothstep(0.10f, 1.30f, lifted_relative_ev)) * 1.08f;
  const float combo_low_mid_darken =
      fminf(shadow_lift + shadow_fill_lift, highlight_reduce) *
      hls_oklch_smoothstep(-2.45f, -0.90f, lifted_relative_ev) *
      (1.0f - hls_oklch_smoothstep(0.50f, 1.95f, lifted_relative_ev)) * 1.30f;

  if (shadow_region != nullptr) {
    *shadow_region =
        fminf(fmaxf((shadow_lift + 0.20f * shadow_fill_lift) / 0.86f, 0.0f), 1.0f);
  }
  if (highlight_region != nullptr) {
    *highlight_region = fminf(fmaxf(highlight_reduce / 1.08f, 0.0f), 1.0f);
  }

  const float delta_ev = shadow_lift + shadow_fill_lift - combo_shadow_rollback -
                         shadow_darken - highlight_reduce - combo_low_mid_darken +
                         highlight_boost;
  return reference_l + delta_ev * kHsAcesccCodePerEv;
}

GPU_HD_FUNC float hs_llf_detail_alpha(float reference_l, float shadow_amount,
                                      float highlight_amount) {
  (void)highlight_amount;
  const float relative_ev = hs_relative_ev_from_log_intensity(reference_l);
  const float deep_shadow =
      1.0f - hls_oklch_smoothstep(-5.7f, -4.1f, relative_ev);
  const float mid_shadow =
      hls_oklch_smoothstep(-5.0f, -3.6f, relative_ev) *
      (1.0f - hls_oklch_smoothstep(-2.4f, -1.0f, relative_ev));
  const float lift_amount = fmaxf(shadow_amount, 0.0f);
  return 1.0f + 0.40f * lift_amount * deep_shadow - 0.14f * lift_amount * mid_shadow;
}

GPU_HD_FUNC float hs_llf_tone_beta(float reference_l, float shadow_amount,
                                    float highlight_amount) {
  constexpr float kEps = 0.035f;
  const float lo = hs_apply_reference_curve(reference_l - kEps, shadow_amount, highlight_amount);
  const float hi = hs_apply_reference_curve(reference_l + kEps, shadow_amount, highlight_amount);
  return fminf(fmaxf((hi - lo) / (2.0f * kEps), 0.08f), 1.70f);
}

GPU_FUNC float hs_llf_gamma_interp_t(float gamma_lo, float gamma_hi, float g) {
  const float span = fmaxf(gamma_hi - gamma_lo, 1.0e-6f);
  return fminf(fmaxf((g - gamma_lo) / span, 0.0f), 1.0f);
}

GPU_FUNC float hs_llf_remap_delta(float delta_l, float sigma_r, float alpha, float beta) {
  const float abs_delta = fabsf(delta_l);
  if (abs_delta <= 1.0e-6f) {
    return 0.0f;
  }

  const float sign = copysignf(1.0f, delta_l);
  if (abs_delta <= sigma_r) {
    const float normalized = fminf(fmaxf(abs_delta / fmaxf(sigma_r, 1.0e-6f), 0.0f), 1.0f);
    return sign * sigma_r * powf(normalized, alpha);
  }
  return sign * (sigma_r + beta * (abs_delta - sigma_r));
}

GPU_FUNC float hs_read_plane_clamped(const float* __restrict src, int x, int y, int width,
                                     int height) {
  const int clamped_x = min(max(x, 0), width - 1);
  const int clamped_y = min(max(y, 0), height - 1);
  return src[static_cast<size_t>(clamped_y) * static_cast<size_t>(width) +
             static_cast<size_t>(clamped_x)];
}

GPU_FUNC float hs_pyr_weight_1d(int tap) {
  switch (tap) {
    case -2:
    case 2:
      return 1.0f / 16.0f;
    case -1:
    case 1:
      return 4.0f / 16.0f;
    default:
      return 6.0f / 16.0f;
  }
}

GPU_FUNC float hs_expand_from_coarse(const float* __restrict coarse, int coarse_width,
                                     int coarse_height, int x, int y) {
  float sum = 0.0f;
  for (int ky = -2; ky <= 2; ++ky) {
    const int sample_y = y - ky;
    if ((sample_y & 1) != 0) continue;
    const int cy = min(max(sample_y / 2, 0), coarse_height - 1);
    const float wy = hs_pyr_weight_1d(ky);
    for (int kx = -2; kx <= 2; ++kx) {
      const int sample_x = x - kx;
      if ((sample_x & 1) != 0) continue;
      const int cx = min(max(sample_x / 2, 0), coarse_width - 1);
      const float wx = hs_pyr_weight_1d(kx);
      sum += 4.0f * wx * wy *
             coarse[static_cast<size_t>(cy) * static_cast<size_t>(coarse_width) +
                    static_cast<size_t>(cx)];
    }
  }
  return sum;
}

__global__ void HsCopyThroughKernel(const float4* __restrict src, float4* __restrict dst,
                                    int width, int height, size_t pitch_elems) {
  const int x = blockIdx.x * blockDim.x + threadIdx.x;
  const int y = blockIdx.y * blockDim.y + threadIdx.y;
  if (x >= width || y >= height) return;
  const size_t offset = static_cast<size_t>(y) * pitch_elems + static_cast<size_t>(x);
  dst[offset] = src[offset];
}

__global__ void HsExtractLogIntensityKernel(const float4* __restrict src, float* __restrict dst,
                                            int width, int height, size_t src_pitch_elems) {
  const int x = blockIdx.x * blockDim.x + threadIdx.x;
  const int y = blockIdx.y * blockDim.y + threadIdx.y;
  if (x >= width || y >= height) return;

  const size_t src_offset = static_cast<size_t>(y) * src_pitch_elems + static_cast<size_t>(x);
  const size_t dst_offset = static_cast<size_t>(y) * static_cast<size_t>(width) +
                            static_cast<size_t>(x);
  dst[dst_offset] = hs_log_intensity_from_acescc(src[src_offset]);
}

GPU_FUNC float4 hs_read_rgba_bilinear(const float4* __restrict src, int width, int height,
                                      size_t pitch_elems, float x, float y) {
  const float clamped_x = fminf(fmaxf(x, 0.0f), static_cast<float>(width - 1));
  const float clamped_y = fminf(fmaxf(y, 0.0f), static_cast<float>(height - 1));
  const int x0 = min(max(static_cast<int>(floorf(clamped_x)), 0), width - 1);
  const int y0 = min(max(static_cast<int>(floorf(clamped_y)), 0), height - 1);
  const int x1 = min(x0 + 1, width - 1);
  const int y1 = min(y0 + 1, height - 1);
  const float tx = clamped_x - static_cast<float>(x0);
  const float ty = clamped_y - static_cast<float>(y0);

  const float4 v00 = src[static_cast<size_t>(y0) * pitch_elems + static_cast<size_t>(x0)];
  const float4 v10 = src[static_cast<size_t>(y0) * pitch_elems + static_cast<size_t>(x1)];
  const float4 v01 = src[static_cast<size_t>(y1) * pitch_elems + static_cast<size_t>(x0)];
  const float4 v11 = src[static_cast<size_t>(y1) * pitch_elems + static_cast<size_t>(x1)];
  const float4 vx0 = make_float4(hs_lerp(v00.x, v10.x, tx), hs_lerp(v00.y, v10.y, tx),
                                 hs_lerp(v00.z, v10.z, tx), hs_lerp(v00.w, v10.w, tx));
  const float4 vx1 = make_float4(hs_lerp(v01.x, v11.x, tx), hs_lerp(v01.y, v11.y, tx),
                                 hs_lerp(v01.z, v11.z, tx), hs_lerp(v01.w, v11.w, tx));
  return make_float4(hs_lerp(vx0.x, vx1.x, ty), hs_lerp(vx0.y, vx1.y, ty),
                     hs_lerp(vx0.z, vx1.z, ty), hs_lerp(vx0.w, vx1.w, ty));
}

__global__ void HsExtractLogIntensityResampledKernel(const float4* __restrict src,
                                                     float* __restrict dst, int src_width,
                                                     int src_height, size_t src_pitch_elems,
                                                     int dst_width, int dst_height) {
  const int x = blockIdx.x * blockDim.x + threadIdx.x;
  const int y = blockIdx.y * blockDim.y + threadIdx.y;
  if (x >= dst_width || y >= dst_height) return;

  const float src_x = ((static_cast<float>(x) + 0.5f) * static_cast<float>(src_width) /
                       fmaxf(static_cast<float>(dst_width), 1.0f)) -
                      0.5f;
  const float src_y = ((static_cast<float>(y) + 0.5f) * static_cast<float>(src_height) /
                       fmaxf(static_cast<float>(dst_height), 1.0f)) -
                      0.5f;
  const size_t dst_offset = static_cast<size_t>(y) * static_cast<size_t>(dst_width) +
                            static_cast<size_t>(x);
  dst[dst_offset] =
      hs_log_intensity_from_acescc(hs_read_rgba_bilinear(src, src_width, src_height,
                                                         src_pitch_elems, src_x, src_y));
}

__global__ void HsBuildRemappedSampleKernel(const float* __restrict source_l,
                                            float* __restrict remapped_l, int width, int height,
                                            float gamma, float target, float beta, float alpha,
                                            float sigma_r) {
  const int x = blockIdx.x * blockDim.x + threadIdx.x;
  const int y = blockIdx.y * blockDim.y + threadIdx.y;
  if (x >= width || y >= height) return;

  const size_t offset = static_cast<size_t>(y) * static_cast<size_t>(width) +
                        static_cast<size_t>(x);
  const float source_value = source_l[offset];
  remapped_l[offset] =
      target + hs_llf_remap_delta(source_value - gamma, sigma_r, alpha, beta);
}

__global__ void HsPyrDownKernel(const float* __restrict src, int src_width, int src_height,
                                float* __restrict dst, int dst_width, int dst_height) {
  const int x = blockIdx.x * blockDim.x + threadIdx.x;
  const int y = blockIdx.y * blockDim.y + threadIdx.y;
  if (x >= dst_width || y >= dst_height) return;

  const int center_x = x * 2;
  const int center_y = y * 2;
  float     sum = 0.0f;
  for (int ky = -2; ky <= 2; ++ky) {
    const float wy = hs_pyr_weight_1d(ky);
    for (int kx = -2; kx <= 2; ++kx) {
      const float wx = hs_pyr_weight_1d(kx);
      sum += wx * wy *
             hs_read_plane_clamped(src, center_x + kx, center_y + ky, src_width, src_height);
    }
  }

  dst[static_cast<size_t>(y) * static_cast<size_t>(dst_width) + static_cast<size_t>(x)] = sum;
}

__global__ void HsSelectInterpolatedLevelKernel(
    const float* __restrict source_level, const float* __restrict sample_lo_level,
    const float* __restrict sample_lo_coarse, const float* __restrict sample_hi_level,
    const float* __restrict sample_hi_coarse, float* __restrict output_level, int width,
    int height, int coarse_width, int coarse_height, float gamma_lo, float gamma_hi,
    bool first_pair, bool last_pair, bool top_level) {
  const int x = blockIdx.x * blockDim.x + threadIdx.x;
  const int y = blockIdx.y * blockDim.y + threadIdx.y;
  if (x >= width || y >= height) return;

  const size_t offset =
      static_cast<size_t>(y) * static_cast<size_t>(width) + static_cast<size_t>(x);
  const float  g = source_level[offset];
  const bool   in_interval =
      (first_pair && g <= gamma_hi) || (last_pair && g >= gamma_lo) ||
      (g >= gamma_lo && g < gamma_hi);
  if (!in_interval) {
    return;
  }

  const float t = hs_llf_gamma_interp_t(gamma_lo, gamma_hi, g);
  if (top_level) {
    output_level[offset] = hs_lerp(sample_lo_level[offset], sample_hi_level[offset], t);
    return;
  }

  const float lap_lo = sample_lo_level[offset] -
                       hs_expand_from_coarse(sample_lo_coarse, coarse_width, coarse_height, x, y);
  const float lap_hi = sample_hi_level[offset] -
                       hs_expand_from_coarse(sample_hi_coarse, coarse_width, coarse_height, x, y);
  output_level[offset] = hs_lerp(lap_lo, lap_hi, t);
}

__global__ void HsCollapseLevelKernel(const float* __restrict lap_level,
                                      const float* __restrict coarse_level,
                                      float* __restrict dst_level, int width, int height,
                                      int coarse_width, int coarse_height) {
  const int x = blockIdx.x * blockDim.x + threadIdx.x;
  const int y = blockIdx.y * blockDim.y + threadIdx.y;
  if (x >= width || y >= height) return;

  const size_t offset =
      static_cast<size_t>(y) * static_cast<size_t>(width) + static_cast<size_t>(x);
  dst_level[offset] = lap_level[offset] +
                      hs_expand_from_coarse(coarse_level, coarse_width, coarse_height, x, y);
}

GPU_FUNC float4 hs_apply_adjusted_l_pixel(float4 px, float adjusted_l) {
  const float3 source_ap1 = hls_oklch_acescc_to_ap1(make_float3(px.x, px.y, px.z));
  const float  source_intensity = fmaxf(hs_ap1_intensity(source_ap1), 1.0e-5f);
  const float  adjusted_intensity = hls_oklch_acescc_decode(adjusted_l);
  const float  ratio = fminf(fmaxf(adjusted_intensity / source_intensity, 0.0f), 32.0f);
  const float3 ratio_ap1 = make_float3(source_ap1.x * ratio, source_ap1.y * ratio,
                                       source_ap1.z * ratio);
  const float3 neutral_ap1 =
      make_float3(adjusted_intensity, adjusted_intensity, adjusted_intensity);
  const float3 output_ap1 = hls_oklch_fit_ap1_lower_gamut(ratio_ap1, neutral_ap1);
  const float3 output_acescc = hls_oklch_ap1_to_acescc(output_ap1);
  return make_float4(output_acescc.x, output_acescc.y, output_acescc.z, px.w);
}

GPU_FUNC float hs_read_plane_bilinear(const float* __restrict plane, int width, int height,
                                      size_t pitch_elems, float x, float y) {
  const float clamped_x = fminf(fmaxf(x, 0.0f), static_cast<float>(width - 1));
  const float clamped_y = fminf(fmaxf(y, 0.0f), static_cast<float>(height - 1));
  const int x0 = min(max(static_cast<int>(floorf(clamped_x)), 0), width - 1);
  const int y0 = min(max(static_cast<int>(floorf(clamped_y)), 0), height - 1);
  const int x1 = min(x0 + 1, width - 1);
  const int y1 = min(y0 + 1, height - 1);
  const float tx = clamped_x - static_cast<float>(x0);
  const float ty = clamped_y - static_cast<float>(y0);

  const float v00 = plane[static_cast<size_t>(y0) * pitch_elems + static_cast<size_t>(x0)];
  const float v10 = plane[static_cast<size_t>(y0) * pitch_elems + static_cast<size_t>(x1)];
  const float v01 = plane[static_cast<size_t>(y1) * pitch_elems + static_cast<size_t>(x0)];
  const float v11 = plane[static_cast<size_t>(y1) * pitch_elems + static_cast<size_t>(x1)];
  const float vx0 = hs_lerp(v00, v10, tx);
  const float vx1 = hs_lerp(v01, v11, tx);
  return hs_lerp(vx0, vx1, ty);
}

__global__ void HsApplyAdjustedLKernel(const float4* __restrict src,
                                        const float* __restrict adjusted_l,
                                        float4* __restrict dst, int width, int height,
                                        size_t src_pitch_elems) {
  const int x = blockIdx.x * blockDim.x + threadIdx.x;
  const int y = blockIdx.y * blockDim.y + threadIdx.y;
  if (x >= width || y >= height) return;

  const size_t src_offset =
      static_cast<size_t>(y) * src_pitch_elems + static_cast<size_t>(x);
  const size_t l_offset =
      static_cast<size_t>(y) * static_cast<size_t>(width) + static_cast<size_t>(x);
  dst[src_offset] = hs_apply_adjusted_l_pixel(src[src_offset], adjusted_l[l_offset]);
}

__global__ void HsApplyAdjustedLFromReferenceKernel(
    const float4* __restrict src, const float* __restrict adjusted_l, float4* __restrict dst,
    int width, int height, size_t src_pitch_elems, int adjusted_width, int adjusted_height,
    size_t adjusted_pitch_elems, GPUOperatorParams params) {
  const int x = blockIdx.x * blockDim.x + threadIdx.x;
  const int y = blockIdx.y * blockDim.y + threadIdx.y;
  if (x >= width || y >= height) return;

  const float reference_width =
      static_cast<float>(max(params.render_roi_reference_width_, width));
  const float reference_height =
      static_cast<float>(max(params.render_roi_reference_height_, height));
  const float roi_origin_x =
      params.render_roi_enabled_ ? static_cast<float>(params.render_roi_x_) : 0.0f;
  const float roi_origin_y =
      params.render_roi_enabled_ ? static_cast<float>(params.render_roi_y_) : 0.0f;
  const float roi_width = params.render_roi_enabled_
                              ? fmaxf(params.render_roi_scale_x_ * reference_width, 1.0f)
                              : reference_width;
  const float roi_height = params.render_roi_enabled_
                               ? fmaxf(params.render_roi_scale_y_ * reference_height, 1.0f)
                               : reference_height;
  const float reference_x = roi_origin_x +
                            ((static_cast<float>(x) + 0.5f) * roi_width /
                             fmaxf(static_cast<float>(width), 1.0f)) -
                            0.5f;
  const float reference_y = roi_origin_y +
                            ((static_cast<float>(y) + 0.5f) * roi_height /
                             fmaxf(static_cast<float>(height), 1.0f)) -
                            0.5f;
  const float adjusted_x =
      ((reference_x + 0.5f) * static_cast<float>(adjusted_width) /
       fmaxf(reference_width, 1.0f)) -
      0.5f;
  const float adjusted_y =
      ((reference_y + 0.5f) * static_cast<float>(adjusted_height) /
       fmaxf(reference_height, 1.0f)) -
      0.5f;

  const size_t src_offset =
      static_cast<size_t>(y) * src_pitch_elems + static_cast<size_t>(x);
  const float sampled_l = hs_read_plane_bilinear(adjusted_l, adjusted_width, adjusted_height,
                                                  adjusted_pitch_elems, adjusted_x, adjusted_y);
  dst[src_offset] = hs_apply_adjusted_l_pixel(src[src_offset], sampled_l);
}

__global__ void HsApplyAdjustedLFromFrameKernel(
    const float4* __restrict src, const float* __restrict adjusted_l, float4* __restrict dst,
    int width, int height, size_t src_pitch_elems, int adjusted_width, int adjusted_height,
    size_t adjusted_pitch_elems) {
  const int x = blockIdx.x * blockDim.x + threadIdx.x;
  const int y = blockIdx.y * blockDim.y + threadIdx.y;
  if (x >= width || y >= height) return;

  const float adjusted_x =
      ((static_cast<float>(x) + 0.5f) * static_cast<float>(adjusted_width) /
       fmaxf(static_cast<float>(width), 1.0f)) -
      0.5f;
  const float adjusted_y =
      ((static_cast<float>(y) + 0.5f) * static_cast<float>(adjusted_height) /
       fmaxf(static_cast<float>(height), 1.0f)) -
      0.5f;
  const size_t src_offset =
      static_cast<size_t>(y) * src_pitch_elems + static_cast<size_t>(x);
  const float sampled_l = hs_read_plane_bilinear(adjusted_l, adjusted_width, adjusted_height,
                                                  adjusted_pitch_elems, adjusted_x, adjusted_y);
  dst[src_offset] = hs_apply_adjusted_l_pixel(src[src_offset], sampled_l);
}

struct GPU_HighlightShadowLocalToneStage {
  static constexpr int   kMaxLevels   = 12;
  static constexpr float kGammaMinL   = -0.15f;
  static constexpr float kGammaMaxL   = 1.18f;
  static constexpr float kBaseSigmaR  = 0.07545252f;
  static constexpr float kGammaStepScale = 1.35f;
  static constexpr int   kReferenceMaskMaxLongEdge = 2048;

  struct HsLlfSample {
    float gamma  = 0.0f;
    float target = 0.0f;
    float beta   = 1.0f;
    float alpha  = 1.0f;
  };

  std::array<float*, kMaxLevels> source_levels_ = {};
  std::array<float*, kMaxLevels> remap_a_levels_ = {};
  std::array<float*, kMaxLevels> remap_b_levels_ = {};
  std::array<float*, kMaxLevels> output_levels_ = {};
  std::array<int, kMaxLevels>    level_widths_ = {};
  std::array<int, kMaxLevels>    level_heights_ = {};
  int                            level_count_ = 0;
  int                            cached_width_ = 0;
  int                            cached_height_ = 0;
  int                            cached_frame_width_ = 0;
  int                            cached_frame_height_ = 0;
  size_t                         cached_pitch_ = 0;
  std::uint64_t                  cached_key_ = 0;
  bool                           cached_reference_base_ = false;

  GPU_HighlightShadowLocalToneStage() = default;

  GPU_HighlightShadowLocalToneStage(const GPU_HighlightShadowLocalToneStage&) {}

  GPU_HighlightShadowLocalToneStage& operator=(const GPU_HighlightShadowLocalToneStage&) {
    ReleaseResources();
    return *this;
  }

  GPU_HighlightShadowLocalToneStage(GPU_HighlightShadowLocalToneStage&& other) noexcept
      : source_levels_(other.source_levels_),
        remap_a_levels_(other.remap_a_levels_),
        remap_b_levels_(other.remap_b_levels_),
        output_levels_(other.output_levels_),
        level_widths_(other.level_widths_),
        level_heights_(other.level_heights_),
        level_count_(other.level_count_),
        cached_width_(other.cached_width_),
        cached_height_(other.cached_height_),
        cached_frame_width_(other.cached_frame_width_),
        cached_frame_height_(other.cached_frame_height_),
        cached_pitch_(other.cached_pitch_),
        cached_key_(other.cached_key_),
        cached_reference_base_(other.cached_reference_base_) {
    other.source_levels_.fill(nullptr);
    other.remap_a_levels_.fill(nullptr);
    other.remap_b_levels_.fill(nullptr);
    other.output_levels_.fill(nullptr);
    other.level_widths_.fill(0);
    other.level_heights_.fill(0);
    other.level_count_ = 0;
    other.cached_width_ = 0;
    other.cached_height_ = 0;
    other.cached_frame_width_ = 0;
    other.cached_frame_height_ = 0;
    other.cached_pitch_ = 0;
    other.cached_key_ = 0;
    other.cached_reference_base_ = false;
  }

  GPU_HighlightShadowLocalToneStage& operator=(GPU_HighlightShadowLocalToneStage&& other) noexcept {
    if (this != &other) {
      ReleaseResources();
      source_levels_ = other.source_levels_;
      remap_a_levels_ = other.remap_a_levels_;
      remap_b_levels_ = other.remap_b_levels_;
      output_levels_ = other.output_levels_;
      level_widths_ = other.level_widths_;
      level_heights_ = other.level_heights_;
      level_count_ = other.level_count_;
      cached_width_ = other.cached_width_;
      cached_height_ = other.cached_height_;
      cached_frame_width_ = other.cached_frame_width_;
      cached_frame_height_ = other.cached_frame_height_;
      cached_pitch_ = other.cached_pitch_;
      cached_key_ = other.cached_key_;
      cached_reference_base_ = other.cached_reference_base_;
      other.source_levels_.fill(nullptr);
      other.remap_a_levels_.fill(nullptr);
      other.remap_b_levels_.fill(nullptr);
      other.output_levels_.fill(nullptr);
      other.level_widths_.fill(0);
      other.level_heights_.fill(0);
      other.level_count_ = 0;
      other.cached_width_ = 0;
      other.cached_height_ = 0;
      other.cached_frame_width_ = 0;
      other.cached_frame_height_ = 0;
      other.cached_pitch_ = 0;
      other.cached_key_ = 0;
      other.cached_reference_base_ = false;
    }
    return *this;
  }

  ~GPU_HighlightShadowLocalToneStage() { ReleaseResources(); }

  static auto FloatBits(float value) -> std::uint32_t {
    std::uint32_t bits = 0;
    std::memcpy(&bits, &value, sizeof(bits));
    return bits;
  }

  static void HashCombine(std::uint64_t& seed, std::uint64_t value) {
    seed ^= value + 0x9e3779b97f4a7c15ull + (seed << 6) + (seed >> 2);
  }

  static auto BuildAdjustedResultCacheKey(const GPUOperatorParams& params, float shadow_amount,
                                          float highlight_amount) -> std::uint64_t {
    std::uint64_t key = params.hs_mask_base_cache_key_;
    HashCombine(key, static_cast<std::uint64_t>(params.shadows_enabled_));
    HashCombine(key, static_cast<std::uint64_t>(params.highlights_enabled_));
    HashCombine(key, static_cast<std::uint64_t>(FloatBits(shadow_amount)));
    HashCombine(key, static_cast<std::uint64_t>(FloatBits(highlight_amount)));
    HashCombine(key, static_cast<std::uint64_t>(params.render_roi_enabled_));
    if (params.render_roi_enabled_) {
      HashCombine(key, static_cast<std::uint64_t>(params.render_roi_x_));
      HashCombine(key, static_cast<std::uint64_t>(params.render_roi_y_));
      HashCombine(key, static_cast<std::uint64_t>(FloatBits(params.render_roi_scale_x_)));
      HashCombine(key, static_cast<std::uint64_t>(FloatBits(params.render_roi_scale_y_)));
      HashCombine(key, static_cast<std::uint64_t>(params.render_roi_reference_width_));
      HashCombine(key, static_cast<std::uint64_t>(params.render_roi_reference_height_));
    }
    return key;
  }

  struct MaskDimensions {
    int width = 1;
    int height = 1;
  };

  static auto ComputeMaskDimensions(int width, int height, bool roi_frame_with_source_reference)
      -> MaskDimensions {
    const int max_long_edge =
        roi_frame_with_source_reference ? max(width, height) : kReferenceMaskMaxLongEdge;
    const float scale = fminf(
        1.0f, static_cast<float>(max(1, max_long_edge)) / static_cast<float>(max(width, height)));
    return {max(1, static_cast<int>(ceilf(static_cast<float>(width) * scale))),
            max(1, static_cast<int>(ceilf(static_cast<float>(height) * scale)))};
  }

  static auto GridFor(int width, int height, dim3 block) -> dim3 {
    return dim3((static_cast<unsigned int>(width) + block.x - 1) / block.x,
                (static_cast<unsigned int>(height) + block.y - 1) / block.y);
  }

  static auto ComputeLevelCount(int width, int height, float radius) -> int {
    const int radius_levels =
        max(3, min(kMaxLevels, static_cast<int>(ceilf(log2f(fmaxf(radius, 1.0f)))) + 2));
    int count = 1;
    int w = width;
    int h = height;
    while (count < radius_levels && (w > 1 || h > 1)) {
      w = max(1, (w + 1) / 2);
      h = max(1, (h + 1) / 2);
      ++count;
    }
    return count;
  }

  static auto SigmaR(float shadow_amount, float highlight_amount) -> float {
    (void)shadow_amount;
    (void)highlight_amount;
    return kBaseSigmaR;
  }

  static auto BuildSamples(float shadow_amount, float highlight_amount, float sigma_r)
      -> std::vector<HsLlfSample> {
    const float sample_step = fmaxf(sigma_r * kGammaStepScale, 0.045f);
    const int sample_count =
        max(2, static_cast<int>(ceilf((kGammaMaxL - kGammaMinL) / sample_step)) + 1);
    std::vector<HsLlfSample> samples;
    samples.reserve(static_cast<size_t>(sample_count));
    for (int i = 0; i < sample_count; ++i) {
      const float t =
          (sample_count == 1) ? 0.0f : static_cast<float>(i) / static_cast<float>(sample_count - 1);
      const float gamma = hs_lerp(kGammaMinL, kGammaMaxL, t);
      samples.push_back(
          {gamma, hs_apply_reference_curve(gamma, shadow_amount, highlight_amount),
           hs_llf_tone_beta(gamma, shadow_amount, highlight_amount),
           hs_llf_detail_alpha(gamma, shadow_amount, highlight_amount)});
    }
    return samples;
  }

  void ReleaseResources() {
    for (float*& ptr : source_levels_) {
      if (ptr != nullptr) {
        cudaFree(ptr);
        ptr = nullptr;
      }
    }
    for (float*& ptr : remap_a_levels_) {
      if (ptr != nullptr) {
        cudaFree(ptr);
        ptr = nullptr;
      }
    }
    for (float*& ptr : remap_b_levels_) {
      if (ptr != nullptr) {
        cudaFree(ptr);
        ptr = nullptr;
      }
    }
    for (float*& ptr : output_levels_) {
      if (ptr != nullptr) {
        cudaFree(ptr);
        ptr = nullptr;
      }
    }
    level_widths_.fill(0);
    level_heights_.fill(0);
    level_count_ = 0;
    cached_width_ = 0;
    cached_height_ = 0;
    cached_frame_width_ = 0;
    cached_frame_height_ = 0;
    cached_pitch_ = 0;
    cached_key_ = 0;
    cached_reference_base_ = false;
  }

  void EnsurePyramidBuffers(int width, int height, float radius) {
    const int new_level_count = ComputeLevelCount(width, height, radius);
    std::array<int, kMaxLevels> new_widths = {};
    std::array<int, kMaxLevels> new_heights = {};
    new_widths[0] = width;
    new_heights[0] = height;
    for (int level = 1; level < new_level_count; ++level) {
      new_widths[level] = max(1, (new_widths[level - 1] + 1) / 2);
      new_heights[level] = max(1, (new_heights[level - 1] + 1) / 2);
    }

    bool layout_matches = level_count_ == new_level_count;
    for (int level = 0; layout_matches && level < new_level_count; ++level) {
      layout_matches = level_widths_[level] == new_widths[level] &&
                       level_heights_[level] == new_heights[level] &&
                       source_levels_[level] != nullptr && remap_a_levels_[level] != nullptr &&
                       remap_b_levels_[level] != nullptr && output_levels_[level] != nullptr;
    }
    if (layout_matches) {
      return;
    }

    ReleaseResources();
    level_count_ = new_level_count;
    level_widths_ = new_widths;
    level_heights_ = new_heights;
    for (int level = 0; level < level_count_; ++level) {
      const size_t elems =
          static_cast<size_t>(level_widths_[level]) * static_cast<size_t>(level_heights_[level]);
      cudaMalloc(reinterpret_cast<void**>(&source_levels_[level]), elems * sizeof(float));
      cudaMalloc(reinterpret_cast<void**>(&remap_a_levels_[level]), elems * sizeof(float));
      cudaMalloc(reinterpret_cast<void**>(&remap_b_levels_[level]), elems * sizeof(float));
      cudaMalloc(reinterpret_cast<void**>(&output_levels_[level]), elems * sizeof(float));
    }
    cached_width_ = 0;
    cached_height_ = 0;
    cached_frame_width_ = 0;
    cached_frame_height_ = 0;
    cached_pitch_ = 0;
    cached_key_ = 0;
    cached_reference_base_ = false;
  }

  void BuildSourcePyramid(const float4* src, int width, int height, size_t src_pitch_elems,
                          dim3 block, cudaStream_t stream) {
    if (level_widths_[0] == width && level_heights_[0] == height) {
      HsExtractLogIntensityKernel<<<GridFor(width, height, block), block, 0, stream>>>(
          src, source_levels_[0], width, height, src_pitch_elems);
    } else {
      HsExtractLogIntensityResampledKernel<<<GridFor(level_widths_[0], level_heights_[0], block),
                                             block, 0, stream>>>(
          src, source_levels_[0], width, height, src_pitch_elems, level_widths_[0],
          level_heights_[0]);
    }
    for (int level = 1; level < level_count_; ++level) {
      HsPyrDownKernel<<<GridFor(level_widths_[level], level_heights_[level], block), block, 0,
                        stream>>>(source_levels_[level - 1], level_widths_[level - 1],
                                  level_heights_[level - 1], source_levels_[level],
                                  level_widths_[level], level_heights_[level]);
    }
  }

  void BuildRemapPyramid(const HsLlfSample& sample, float sigma_r,
                         std::array<float*, kMaxLevels>& remap_levels, dim3 block,
                         cudaStream_t stream) {
    HsBuildRemappedSampleKernel<<<GridFor(level_widths_[0], level_heights_[0], block), block, 0,
                                  stream>>>(source_levels_[0], remap_levels[0], level_widths_[0],
                                            level_heights_[0], sample.gamma, sample.target,
                                            sample.beta, sample.alpha, sigma_r);
    for (int level = 1; level < level_count_; ++level) {
      HsPyrDownKernel<<<GridFor(level_widths_[level], level_heights_[level], block), block, 0,
                        stream>>>(remap_levels[level - 1], level_widths_[level - 1],
                                  level_heights_[level - 1], remap_levels[level],
                                  level_widths_[level], level_heights_[level]);
    }
  }

  void BuildOutputPyramid(const std::vector<HsLlfSample>& samples, float sigma_r, dim3 block,
                          cudaStream_t stream) {
    for (int level = 0; level < level_count_; ++level) {
      cudaMemsetAsync(output_levels_[level], 0,
                      static_cast<size_t>(level_widths_[level]) *
                          static_cast<size_t>(level_heights_[level]) * sizeof(float),
                      stream);
    }

    BuildRemapPyramid(samples.front(), sigma_r, remap_a_levels_, block, stream);
    BuildRemapPyramid(samples[1], sigma_r, remap_b_levels_, block, stream);

    for (size_t pair_index = 0; pair_index + 1 < samples.size(); ++pair_index) {

      for (int level = 0; level < level_count_; ++level) {
        const bool top_level = level == (level_count_ - 1);
        const int coarse_width = top_level ? 1 : level_widths_[level + 1];
        const int coarse_height = top_level ? 1 : level_heights_[level + 1];
        HsSelectInterpolatedLevelKernel<<<GridFor(level_widths_[level], level_heights_[level], block),
                                          block, 0, stream>>>(
            source_levels_[level], remap_a_levels_[level],
            top_level ? nullptr : remap_a_levels_[level + 1], remap_b_levels_[level],
            top_level ? nullptr : remap_b_levels_[level + 1], output_levels_[level],
            level_widths_[level], level_heights_[level], coarse_width, coarse_height,
            samples[pair_index].gamma, samples[pair_index + 1].gamma, pair_index == 0,
            pair_index + 2 == samples.size(), top_level);
      }

      if (pair_index + 2 < samples.size()) {
        std::swap(remap_a_levels_, remap_b_levels_);
        BuildRemapPyramid(samples[pair_index + 2], sigma_r, remap_b_levels_, block, stream);
      }
    }

    for (int level = level_count_ - 2; level >= 0; --level) {
      HsCollapseLevelKernel<<<GridFor(level_widths_[level], level_heights_[level], block), block,
                              0, stream>>>(output_levels_[level], output_levels_[level + 1],
                                           remap_a_levels_[level], level_widths_[level],
                                           level_heights_[level], level_widths_[level + 1],
                                           level_heights_[level + 1]);
      std::swap(output_levels_[level], remap_a_levels_[level]);
    }
  }

  void Dispatch(float4* src, float4* dst, int width, int height, size_t pitch_elems,
                GPUOperatorParams& params, dim3 grid, dim3 block, cudaStream_t stream) {
    const bool active =
        params.hs_local_tone_enabled_ &&
        ((params.shadows_enabled_ && fabsf(params.shadows_offset_) > 1.0e-6f) ||
         (params.highlights_enabled_ && fabsf(params.highlights_offset_) > 1.0e-6f));
    if (!active) {
      HsCopyThroughKernel<<<grid, block, 0, stream>>>(src, dst, width, height, pitch_elems);
      return;
    }

    const float shadow_amount =
        params.shadows_enabled_ ? fminf(fmaxf(params.shadows_offset_, -1.0f), 1.0f) : 0.0f;
    const float highlight_amount = params.highlights_enabled_
                                       ? fminf(fmaxf(-params.highlights_offset_, -1.0f), 1.0f)
                                       : 0.0f;
    const std::uint64_t adjusted_cache_key =
        BuildAdjustedResultCacheKey(params, shadow_amount, highlight_amount);
    if (fabsf(shadow_amount) <= 1.0e-6f && fabsf(highlight_amount) <= 1.0e-6f) {
      HsCopyThroughKernel<<<grid, block, 0, stream>>>(src, dst, width, height, pitch_elems);
      return;
    }

    const bool roi_frame_with_source_reference =
        params.render_roi_enabled_ && params.render_roi_reference_width_ > 0 &&
        params.render_roi_reference_height_ > 0;
    const bool reference_result_cache_valid =
        cached_reference_base_ && output_levels_[0] != nullptr &&
        cached_key_ == adjusted_cache_key && cached_width_ > 0 &&
        cached_height_ > 0 && cached_frame_width_ > 0 && cached_frame_height_ > 0 &&
        cached_pitch_ > 0;
    if (!roi_frame_with_source_reference && reference_result_cache_valid &&
        (cached_frame_width_ > width || cached_frame_height_ > height)) {
      HsApplyAdjustedLFromFrameKernel<<<grid, block, 0, stream>>>(
          src, output_levels_[0], dst, width, height, pitch_elems, cached_width_, cached_height_,
          cached_pitch_);
      return;
    }

    const MaskDimensions mask_dims =
        ComputeMaskDimensions(width, height, roi_frame_with_source_reference);
    EnsurePyramidBuffers(mask_dims.width, mask_dims.height, params.hs_base_radius_);
    const bool cache_valid =
        output_levels_[0] != nullptr && cached_key_ == adjusted_cache_key &&
        cached_frame_width_ == width && cached_frame_height_ == height &&
        cached_width_ == mask_dims.width && cached_height_ == mask_dims.height &&
        cached_pitch_ == static_cast<size_t>(level_widths_[0]);
    if (!cache_valid) {
      const float sigma_r = SigmaR(shadow_amount, highlight_amount);
      const auto  samples = BuildSamples(shadow_amount, highlight_amount, sigma_r);
      BuildSourcePyramid(src, width, height, pitch_elems, block, stream);
      BuildOutputPyramid(samples, sigma_r, block, stream);
      cached_key_ = adjusted_cache_key;
      cached_width_ = mask_dims.width;
      cached_height_ = mask_dims.height;
      cached_frame_width_ = width;
      cached_frame_height_ = height;
      cached_pitch_ = static_cast<size_t>(level_widths_[0]);
      cached_reference_base_ = !roi_frame_with_source_reference;
    }

    if (cached_width_ == width && cached_height_ == height) {
      HsApplyAdjustedLKernel<<<grid, block, 0, stream>>>(src, output_levels_[0], dst, width, height,
                                                         pitch_elems);
    } else {
      HsApplyAdjustedLFromFrameKernel<<<grid, block, 0, stream>>>(
          src, output_levels_[0], dst, width, height, pitch_elems, cached_width_, cached_height_,
          cached_pitch_);
    }
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
