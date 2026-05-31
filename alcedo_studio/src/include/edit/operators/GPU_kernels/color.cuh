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
  constexpr float kTextureDeadbandStops = 0.24f;
  constexpr float kRangeStops           = 0.48f;
  const float     edge_delta = fmaxf(fabsf(sample - center) - kTextureDeadbandStops, 0.0f);
  const float     d          = edge_delta / kRangeStops;
  return exp2f(-(d * d));
}

GPU_FUNC float hs_shadow_upper_pivot(GPUOperatorParams params) {
  const float width = fmaxf(params.hs_shadow_log_width_, 0.35f);
  return params.hs_shadow_log_pivot_ + fmaxf(width * 0.40f, 0.24f);
}

GPU_FUNC float hs_shadow_black_floor_weight(float mask_ref, GPUOperatorParams params) {
  const float width = fmaxf(params.hs_shadow_log_width_, 0.35f);
  const float upper_pivot = hs_shadow_upper_pivot(params);
  const float black_start = upper_pivot - fmaxf(width * 7.20f, 4.45f);
  const float black_end = upper_pivot - fmaxf(width * 5.20f, 3.25f);
  const float toe = hls_oklch_smoothstep(black_start, black_end, mask_ref);
  return 0.30f + 0.70f * toe;
}

GPU_FUNC float hs_shadow_zone_weight(float mask_ref, GPUOperatorParams params) {
  const float width = fmaxf(params.hs_shadow_log_width_, 0.35f);
  const float upper_pivot = hs_shadow_upper_pivot(params);
  const float fade_start = upper_pivot - fmaxf(width * 3.15f, 1.95f);
  const float tonal_weight = 1.0f - hls_oklch_smoothstep(fade_start, upper_pivot, mask_ref);
  const float black_floor = hs_shadow_black_floor_weight(mask_ref, params);
  return fminf(fmaxf(tonal_weight * black_floor, 0.0f), 1.0f);
}

GPU_FUNC float hs_shadow_base_distance(float mask_ref, GPUOperatorParams params) {
  return fminf(fmaxf(hs_shadow_upper_pivot(params) - mask_ref, 0.0f), 3.65f);
}

GPU_FUNC float hs_shadow_deep_recovery_weight(float mask_ref, GPUOperatorParams params) {
  const float width = fmaxf(params.hs_shadow_log_width_, 0.35f);
  const float upper_pivot = hs_shadow_upper_pivot(params);
  const float deep_start = upper_pivot - fmaxf(width * 5.80f, 3.60f);
  const float deep_full = upper_pivot - fmaxf(width * 7.60f, 4.70f);
  return 1.0f - hls_oklch_smoothstep(deep_full, deep_start, mask_ref);
}

GPU_FUNC float hs_highlight_zone_weight(float mask_ref, GPUOperatorParams params) {
  constexpr float kToe = 1.0e-3f;
  constexpr float kToePow = 0.1023292992f;
  constexpr float kInvToeRange = 1.1135850f;
  constexpr float kGamma = 0.33f;

  const float t = hls_oklch_smoothstep(params.hs_highlight_log_pivot_,
                                       params.hs_highlight_log_pivot_ +
                                           params.hs_highlight_log_width_,
                                       mask_ref);
  const float lifted = powf(t + kToe, kGamma);
  return fminf(fmaxf((lifted - kToePow) * kInvToeRange, 0.0f), 1.0f);
}

GPU_FUNC float hs_softplus_distance(float distance, float softness) {
  constexpr float kLog2 = 0.6931471805599453f;
  const float     safe_softness = fmaxf(softness, 1.0e-4f);
  const float     x = distance / safe_softness;
  if (x > 20.0f) {
    return distance - safe_softness * kLog2;
  }
  return safe_softness * (log1pf(expf(x)) - kLog2);
}

GPU_FUNC float hs_softrelu_distance(float signed_distance, float softness, float onset) {
  const float safe_softness = fmaxf(softness, 1.0e-4f);
  const float safe_onset = fmaxf(onset, 0.0f);
  const float x = (signed_distance - safe_onset) / safe_softness;
  if (x > 20.0f) {
    return signed_distance - safe_onset;
  }
  if (x < -20.0f) {
    return safe_softness * expf(x);
  }
  return safe_softness * log1pf(expf(x));
}

GPU_FUNC float hs_texture_detail_weight(float detail) {
  return 1.0f - hls_oklch_smoothstep(0.28f, 0.88f, fabsf(detail));
}

GPU_FUNC float hs_lerp(float a, float b, float t) { return a + (b - a) * t; }

GPU_FUNC float hs_active_mask_disagreement(float base_shadow_mask, float base_highlight_mask,
                                           float source_shadow_mask, float source_highlight_mask,
                                           float shadow_amount, float highlight_amount) {
  float disagreement = 0.0f;
  if (fabsf(shadow_amount) > 1.0e-6f) {
    disagreement = fmaxf(disagreement, fabsf(base_shadow_mask - source_shadow_mask));
    disagreement = fmaxf(disagreement, 0.50f * fabsf(base_highlight_mask - source_highlight_mask));
  }
  if (fabsf(highlight_amount) > 1.0e-6f) {
    disagreement = fmaxf(disagreement, fabsf(base_highlight_mask - source_highlight_mask));
  }
  return fminf(fmaxf(disagreement, 0.0f), 1.0f);
}

GPU_FUNC float hs_tonal_reference_mix(float mask_disagreement) {
  return hls_oklch_smoothstep(0.025f, 0.16f, mask_disagreement);
}

GPU_FUNC float hs_llf_detail_mix(float detail) {
  return 1.0f - hls_oklch_smoothstep(0.42f, 0.95f, fabsf(detail));
}

GPU_FUNC float hs_local_tone_mix(float detail, float local_delta, float source_delta) {
  const float mag = fabsf(detail);
  const float edge_weight = hls_oklch_smoothstep(0.62f, 1.55f, mag);
  const float delta_mismatch =
      hls_oklch_smoothstep(0.16f, 0.52f, fabsf(local_delta - source_delta));
  const float guard = fminf(
      fmaxf(edge_weight * (0.82f + 0.18f * delta_mismatch) + 0.18f * edge_weight * edge_weight,
            0.0f),
      1.0f);
  return 1.0f - guard;
}

GPU_FUNC float hs_shadow_detail_preserve_weight(float detail) {
  const float mag = fabsf(detail);
  const float noise_gate = hls_oklch_smoothstep(0.045f, 0.15f, mag);
  const float edge_guard = 1.0f - hls_oklch_smoothstep(0.78f, 1.35f, mag);
  return noise_gate * edge_guard;
}

GPU_FUNC float hs_shadow_llf_detail_gain(float detail, float shadow_amount, float shadow_zone) {
  const float lift_amount = fmaxf(shadow_amount, 0.0f);
  if (lift_amount <= 1.0e-6f || shadow_zone <= 1.0e-6f) {
    return 1.0f;
  }

  const float mag = fabsf(detail);
  const float noise_gate = hls_oklch_smoothstep(0.035f, 0.11f, mag);
  const float fine_detail_gate = 1.0f - hls_oklch_smoothstep(0.38f, 0.82f, mag);
  if (noise_gate <= 0.0f || fine_detail_gate <= 0.0f) {
    return 1.0f;
  }

  constexpr float kSigmaRStops = 0.42f;
  const float     x = fminf(fmaxf(mag / kSigmaRStops, 1.0e-4f), 1.0f);
  const float     alpha = 1.0f - 0.34f * lift_amount;
  const float     remapped_mag = kSigmaRStops * powf(x, alpha);
  const float     remap_gain = remapped_mag / fmaxf(mag, 1.0e-4f);
  const float     limited_gain = fminf(fmaxf(remap_gain - 1.0f, 0.0f), 0.34f);
  const float     mix = lift_amount * shadow_zone * noise_gate * fine_detail_gate;
  return 1.0f + limited_gain * mix;
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
  const float raw_highlight = hs_highlight_zone_weight(mask_ref, params);

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

  const float width = fmaxf(params.hs_shadow_log_width_, 0.35f);
  const float upper_pivot = hs_shadow_upper_pivot(params);
  const float lift_pivot = upper_pivot + fmaxf(width * 1.60f, 0.98f);
  const float distance_to_pivot = fmaxf(lift_pivot - mask_ref, 0.0f);
  const float soft_distance = hs_softplus_distance(distance_to_pivot, fmaxf(width * 2.18f, 1.35f));
  const float black_guard = 0.82f + 0.18f * hs_shadow_black_floor_weight(mask_ref, params);
  const float highlight_overlap = fminf(fmaxf(highlight_mask, 0.0f), 1.0f);
  const float overlap_guard = 1.0f - 0.28f * highlight_overlap;
  const float lift_amount = fmaxf(shadow_amount, 0.0f);
  const float darken_amount = fmaxf(-shadow_amount, 0.0f);
  const float lift_delta = lift_amount * 0.42f * soft_distance * black_guard * overlap_guard;
  const float darken_delta = darken_amount * 0.34f * soft_distance * (0.85f + 0.15f * black_guard);
  return lift_delta - darken_delta;
}

GPU_FUNC float hs_highlight_base_delta_from_ref(float mask_ref, float shadow_amount,
                                                float highlight_amount, GPUOperatorParams params) {
  (void)shadow_amount;
  const float width = fmaxf(params.hs_highlight_log_width_, 0.35f);
  const float soft_distance =
      hs_softrelu_distance(mask_ref - params.hs_highlight_log_pivot_,
                           fminf(fmaxf(width * 0.12f, 0.36f), 0.55f),
                           fminf(fmaxf(width * 0.24f, 0.72f), 1.10f));
  const float reduce_amount = fmaxf(highlight_amount, 0.0f);
  const float boost_amount = fmaxf(-highlight_amount, 0.0f);
  const float reduce_delta = 1.68f * (1.0f - expf(-soft_distance / 1.33f));
  const float boost_delta = 1.24f * (1.0f - expf(-soft_distance / 1.45f));
  return boost_amount * boost_delta - reduce_amount * reduce_delta;
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

GPU_FUNC float hs_highlight_curve_slope(float mask_ref, float shadow_amount,
                                        float highlight_amount, GPUOperatorParams params) {
  constexpr float kEpsStops = 0.08f;
  const float delta_lo =
      hs_highlight_base_delta_from_ref(mask_ref - kEpsStops, shadow_amount, highlight_amount,
                                       params);
  const float delta_hi =
      hs_highlight_base_delta_from_ref(mask_ref + kEpsStops, shadow_amount, highlight_amount,
                                       params);
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

GPU_FUNC float hs_highlight_detail_weight(float mask_ref, float highlight_mask, float detail,
                                          GPUOperatorParams params) {
  const float width = fmaxf(params.hs_highlight_log_width_, 0.35f);
  const float tonal_weight = fminf(fmaxf(highlight_mask, 0.0f), 1.0f);
  const float noise_gate = hls_oklch_smoothstep(0.035f, 0.12f, fabsf(detail));
  const float edge_guard = 1.0f - hls_oklch_smoothstep(0.78f, 1.45f, fabsf(detail));
  const float clipped_guard =
      1.0f - hls_oklch_smoothstep(params.hs_highlight_log_pivot_ + width * 1.15f,
                                  params.hs_highlight_log_pivot_ + width * 2.35f, mask_ref);
  return tonal_weight * noise_gate * edge_guard * clipped_guard;
}

GPU_FUNC float3 hs_preserve_highlight_chroma(float3 source_ap1, float3 output_ap1,
                                             float log_delta, float highlight_amount,
                                             float source_highlight_mask) {
  const float reduce_amount = fmaxf(highlight_amount, 0.0f);
  if (reduce_amount <= 1.0e-6f || log_delta >= -1.0e-5f || source_highlight_mask <= 1.0e-5f) {
    return output_ap1;
  }

  const float3 source_lab = hls_oklch_ap1_to_oklab(source_ap1);
  const float3 output_lab = hls_oklch_ap1_to_oklab(output_ap1);
  const float  source_chroma = hypotf(source_lab.y, source_lab.z);
  const float  output_chroma = hypotf(output_lab.y, output_lab.z);
  if (source_chroma <= 1.0e-5f || output_lab.x <= 1.0e-5f) {
    return output_ap1;
  }

  const float chroma_confidence = hls_oklch_smoothstep(0.012f, 0.060f, source_chroma);
  const float compression = hls_oklch_smoothstep(0.18f, 1.15f, -log_delta);
  const float strength =
      fminf(fmaxf(0.56f * reduce_amount * source_highlight_mask * compression *
                      chroma_confidence,
                  0.0f),
            0.62f);
  if (strength <= 1.0e-6f) {
    return output_ap1;
  }

  const float target_chroma = output_chroma + (source_chroma - output_chroma) * strength;
  const float inv_source_chroma = 1.0f / fmaxf(source_chroma, 1.0e-5f);
  const float2 hue_dir = make_float2(source_lab.y * inv_source_chroma,
                                     source_lab.z * inv_source_chroma);
  const float3 adjusted_lab =
      make_float3(output_lab.x, hue_dir.x * target_chroma, hue_dir.y * target_chroma);
  const float3 neutral_lab = make_float3(output_lab.x, 0.0f, 0.0f);
  const float3 neutral_ap1 = hls_oklch_oklab_to_ap1(neutral_lab);
  return hls_oklch_fit_ap1_lower_gamut(hls_oklch_oklab_to_ap1(adjusted_lab), neutral_ap1);
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

__global__ void HsBuildLogBaseVerticalKernel(const float4* __restrict guidance,
                                             const float* __restrict src, float* __restrict dst,
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
  const float center_guidance = hs_log2_luminance_from_acescc(guidance[offset]);
  float       base   = center * params.hs_base_gaussian_weights_[0];
  float       weight_sum = params.hs_base_gaussian_weights_[0];
  for (int tap = 1; tap < tap_count; ++tap) {
    const int ay = min(y + tap, height - 1);
    const int by = max(y - tap, 0);
    const float a = hs_read_log_clamped(src, x, y + tap, width, height, pitch_elems);
    const float b = hs_read_log_clamped(src, x, y - tap, width, height, pitch_elems);
    const float ag = hs_log2_luminance_from_acescc(
        guidance[static_cast<size_t>(ay) * pitch_elems + static_cast<size_t>(x)]);
    const float bg = hs_log2_luminance_from_acescc(
        guidance[static_cast<size_t>(by) * pitch_elems + static_cast<size_t>(x)]);
    const float spatial = params.hs_base_gaussian_weights_[tap];
    const float aw      = spatial * hs_range_weight(center_guidance, ag);
    const float bw      = spatial * hs_range_weight(center_guidance, bg);
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

GPU_FUNC float4 hs_apply_local_tone_pixel(float4 px, float base, GPUOperatorParams params) {
  const float shadow_amount =
      (params.shadows_enabled_) ? fminf(fmaxf(params.shadows_offset_, -1.0f), 1.0f) : 0.0f;
  const float highlight_amount =
      (params.highlights_enabled_) ? fminf(fmaxf(-params.highlights_offset_ * 0.5f, -1.0f), 1.0f)
                                   : 0.0f;
  if (fabsf(shadow_amount) <= 1.0e-6f && fabsf(highlight_amount) <= 1.0e-6f) {
    return px;
  }

  const float3 source_ap1 = hls_oklch_acescc_to_ap1(make_float3(px.x, px.y, px.z));
  const float  source_log_y  = log2f(fmaxf(hs_ap1_luminance(source_ap1), 1.0e-8f));
  const float  detail        = source_log_y - base;
  const float  base_mask_ref = base;
  float        base_shadow_mask = 0.0f;
  float        base_highlight_mask = 0.0f;
  float        source_shadow_mask = 0.0f;
  float        source_highlight_mask = 0.0f;
  hs_compute_masks(base_mask_ref, shadow_amount, highlight_amount, params, &base_shadow_mask,
                   &base_highlight_mask);
  hs_compute_masks(source_log_y, shadow_amount, highlight_amount, params, &source_shadow_mask,
                   &source_highlight_mask);

  const float mask_disagreement =
      hs_active_mask_disagreement(base_shadow_mask, base_highlight_mask, source_shadow_mask,
                                  source_highlight_mask, shadow_amount, highlight_amount);
  const float tonal_reference_mix = hs_tonal_reference_mix(mask_disagreement);
  const float mask_ref = hs_lerp(base_mask_ref, source_log_y, tonal_reference_mix);
  float        shadow_mask = 0.0f;
  float        highlight_mask = 0.0f;
  hs_compute_masks(mask_ref, shadow_amount, highlight_amount, params, &shadow_mask,
                   &highlight_mask);

  const float base_delta =
      hs_base_delta_from_ref(mask_ref, shadow_amount, highlight_amount, params);
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
  const float highlight_curve_slope =
      fminf(fmaxf(hs_highlight_curve_slope(mask_ref, shadow_amount, highlight_amount, params),
                  0.50f),
            1.20f);
  const float highlight_contrast_loss =
      fminf(fmaxf(1.0f / highlight_curve_slope - 1.0f, 0.0f), 0.42f);
  const float shadow_texture_zone = hs_shadow_detail_weight(mask_ref, shadow_mask, params);
  const float texture_detail = hs_texture_detail_weight(detail);
  const float shadow_detail_sign = hs_shadow_detail_sign_weight(detail);
  const float shadow_detail_zone = shadow_texture_zone * texture_detail * shadow_detail_sign;
  const float shadow_detail_preserve = hs_shadow_detail_preserve_weight(detail);
  const float highlight_detail_zone =
      hs_highlight_detail_weight(mask_ref, highlight_mask, detail, params) * texture_detail;
  const float llf_detail_gain =
      hs_shadow_llf_detail_gain(detail, shadow_amount, shadow_texture_zone);
  const float dark_valley = hls_oklch_smoothstep(0.85f, 1.80f, -detail);
  const float contrast_recovery =
      0.035f + 0.075f * fmaxf(base_contrast_loss, shadow_contrast_loss);
  const float shadow_detail_scale =
      fmaxf(shadow_amount, 0.0f) * shadow_detail_zone * shadow_detail_preserve *
          contrast_recovery -
      0.018f * fmaxf(shadow_amount, 0.0f) * shadow_texture_zone * dark_valley +
      0.025f * fmaxf(-shadow_amount, 0.0f) * shadow_detail_zone *
          fmaxf(base_contrast_loss, shadow_contrast_loss);
  const float highlight_detail_scale =
      fmaxf(highlight_amount, 0.0f) * highlight_detail_zone *
      (0.035f + 0.12f * highlight_contrast_loss);
  const float raw_detail_scale = fminf(
      1.24f, fmaxf(0.97f, (1.0f + shadow_detail_scale + highlight_detail_scale) * llf_detail_gain));
  const float detail_scale = 1.0f + (raw_detail_scale - 1.0f) * hs_llf_detail_mix(detail);
  const float local_delta = base_delta + detail * (detail_scale - 1.0f);
  const float local_adjusted_log_y = source_log_y + local_delta;
  const float source_delta =
      hs_base_delta_from_ref(source_log_y, shadow_amount, highlight_amount, params);
  const float source_adjusted_log_y = source_log_y + source_delta;
  const float local_mix =
      hs_local_tone_mix(detail, local_delta, source_delta) * (1.0f - tonal_reference_mix);
  const float adjusted_log_y = hs_lerp(source_adjusted_log_y, local_adjusted_log_y, local_mix);
  const float log_delta = adjusted_log_y - source_log_y;

  const float rgb_scale = exp2f(fminf(fmaxf(log_delta, -3.5f), 3.5f));
  float3 output_ap1 =
      make_float3(source_ap1.x * rgb_scale, source_ap1.y * rgb_scale, source_ap1.z * rgb_scale);
  output_ap1 = hs_preserve_highlight_chroma(source_ap1, output_ap1, log_delta, highlight_amount,
                                            source_highlight_mask);
  const float3 output_acescc = hls_oklch_ap1_to_acescc(output_ap1);

  return make_float4(output_acescc.x, output_acescc.y, output_acescc.z, px.w);
}

GPU_FUNC float hs_read_base_bilinear(const float* __restrict base_log, int width, int height,
                                     size_t pitch_elems, float x, float y) {
  const float clamped_x = fminf(fmaxf(x, 0.0f), static_cast<float>(width - 1));
  const float clamped_y = fminf(fmaxf(y, 0.0f), static_cast<float>(height - 1));
  const int   x0 = min(max(static_cast<int>(floorf(clamped_x)), 0), width - 1);
  const int   y0 = min(max(static_cast<int>(floorf(clamped_y)), 0), height - 1);
  const int   x1 = min(x0 + 1, width - 1);
  const int   y1 = min(y0 + 1, height - 1);
  const float tx = clamped_x - static_cast<float>(x0);
  const float ty = clamped_y - static_cast<float>(y0);

  const float v00 = base_log[static_cast<size_t>(y0) * pitch_elems + static_cast<size_t>(x0)];
  const float v10 = base_log[static_cast<size_t>(y0) * pitch_elems + static_cast<size_t>(x1)];
  const float v01 = base_log[static_cast<size_t>(y1) * pitch_elems + static_cast<size_t>(x0)];
  const float v11 = base_log[static_cast<size_t>(y1) * pitch_elems + static_cast<size_t>(x1)];
  const float vx0 = v00 + (v10 - v00) * tx;
  const float vx1 = v01 + (v11 - v01) * tx;
  return vx0 + (vx1 - vx0) * ty;
}

__global__ void HsApplyLocalToneKernel(const float4* __restrict src,
                                       const float* __restrict base_log, float4* __restrict dst,
                                       int width, int height, size_t pitch_elems,
                                       GPUOperatorParams params) {
  const int x = blockIdx.x * blockDim.x + threadIdx.x;
  const int y = blockIdx.y * blockDim.y + threadIdx.y;
  if (x >= width || y >= height) return;

  const size_t offset = static_cast<size_t>(y) * pitch_elems + static_cast<size_t>(x);
  dst[offset] = hs_apply_local_tone_pixel(src[offset], base_log[offset], params);
}

__global__ void HsApplyLocalToneFromReferenceBaseKernel(
    const float4* __restrict src, const float* __restrict base_log, float4* __restrict dst,
    int width, int height, size_t pitch_elems, int base_width, int base_height,
    size_t base_pitch_elems, GPUOperatorParams params) {
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
  const float base_x = ((reference_x + 0.5f) * static_cast<float>(base_width) /
                        fmaxf(reference_width, 1.0f)) -
                       0.5f;
  const float base_y = ((reference_y + 0.5f) * static_cast<float>(base_height) /
                        fmaxf(reference_height, 1.0f)) -
                       0.5f;

  const size_t offset = static_cast<size_t>(y) * pitch_elems + static_cast<size_t>(x);
  const float base =
      hs_read_base_bilinear(base_log, base_width, base_height, base_pitch_elems, base_x, base_y);
  dst[offset] = hs_apply_local_tone_pixel(src[offset], base, params);
}

struct GPU_HighlightShadowLocalToneStage {
  float*        base_log_         = nullptr;
  float*        temp_log_         = nullptr;
  size_t        allocated_elems_  = 0;
  int           cached_width_     = 0;
  int           cached_height_    = 0;
  size_t        cached_pitch_     = 0;
  std::uint64_t cached_key_       = 0;
  bool          cached_reference_base_ = false;

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
        cached_key_(other.cached_key_),
        cached_reference_base_(other.cached_reference_base_) {
    other.base_log_ = nullptr;
    other.temp_log_ = nullptr;
    other.allocated_elems_ = 0;
    other.cached_width_ = 0;
    other.cached_height_ = 0;
    other.cached_pitch_ = 0;
    other.cached_key_ = 0;
    other.cached_reference_base_ = false;
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
      cached_reference_base_ = other.cached_reference_base_;
      other.base_log_ = nullptr;
      other.temp_log_ = nullptr;
      other.allocated_elems_ = 0;
      other.cached_width_ = 0;
      other.cached_height_ = 0;
      other.cached_pitch_ = 0;
      other.cached_key_ = 0;
      other.cached_reference_base_ = false;
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
    cached_reference_base_ = false;
  }

  void EnsureBuffers(int height, size_t pitch_elems) {
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
    cached_reference_base_ = false;
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

    const float shadow_amount =
        (params.shadows_enabled_) ? fminf(fmaxf(params.shadows_offset_, -1.0f), 1.0f) : 0.0f;
    const float highlight_amount =
        (params.highlights_enabled_) ? fminf(fmaxf(-params.highlights_offset_ * 0.5f, -1.0f), 1.0f)
                                     : 0.0f;
    if (fabsf(shadow_amount) <= 1.0e-6f && fabsf(highlight_amount) <= 1.0e-6f) {
      HsCopyThroughKernel<<<grid, block, 0, stream>>>(src, dst, width, height, pitch_elems);
      return;
    }

    const bool roi_frame_with_source_reference =
        params.render_roi_enabled_ && params.render_roi_reference_width_ > 0 &&
        params.render_roi_reference_height_ > 0;
    const bool reference_base_cache_valid =
        cached_reference_base_ && base_log_ != nullptr &&
        cached_key_ == params.hs_mask_base_cache_key_ && cached_width_ > 0 &&
        cached_height_ > 0 && cached_pitch_ > 0;
    if (roi_frame_with_source_reference && reference_base_cache_valid) {
      HsApplyLocalToneFromReferenceBaseKernel<<<grid, block, 0, stream>>>(
          src, base_log_, dst, width, height, pitch_elems, cached_width_, cached_height_,
          cached_pitch_, params);
      return;
    }
    if (!roi_frame_with_source_reference && reference_base_cache_valid &&
        (cached_width_ > width || cached_height_ > height)) {
      HsApplyLocalToneFromReferenceBaseKernel<<<grid, block, 0, stream>>>(
          src, base_log_, dst, width, height, pitch_elems, cached_width_, cached_height_,
          cached_pitch_, params);
      return;
    }

    EnsureBuffers(height, pitch_elems);
    const bool cache_valid =
        !roi_frame_with_source_reference && reference_base_cache_valid && cached_width_ == width &&
        cached_height_ == height && cached_pitch_ == pitch_elems;
    if (!cache_valid) {
      HsBuildLogBaseHorizontalKernel<<<grid, block, 0, stream>>>(src, temp_log_, width, height,
                                                                 pitch_elems, params);
      HsBuildLogBaseVerticalKernel<<<grid, block, 0, stream>>>(src, temp_log_, base_log_, width,
                                                               height, pitch_elems, params);
      cached_key_ = params.hs_mask_base_cache_key_;
      cached_width_ = width;
      cached_height_ = height;
      cached_pitch_ = pitch_elems;
      cached_reference_base_ = !roi_frame_with_source_reference;
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
