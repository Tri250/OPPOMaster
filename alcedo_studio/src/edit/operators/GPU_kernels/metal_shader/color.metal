//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include "common.metal"

constant int kMetalHlsProfileCount = 8;

static inline float metal_wrap_hue(float h) {
  h = fmod(h, 360.0f);
  if (h < 0.0f) {
    h += 360.0f;
  }
  return h;
}

static inline float metal_hue_distance(float a, float b) {
  const float diff = fabs(metal_wrap_hue(a) - metal_wrap_hue(b));
  return fmin(diff, 360.0f - diff);
}

static inline float metal_smoothstep_range(float edge0, float edge1, float x) {
  const float denom = fmax(edge1 - edge0, 1e-6f);
  const float t     = clamp((x - edge0) / denom, 0.0f, 1.0f);
  return t * t * (3.0f - 2.0f * t);
}

static inline float metal_soft_floor(float x, float floor, float softness) {
  const float t = (x - floor) / fmax(softness, 1e-6f);
  if (t > 20.0f) {
    return x;
  }
  if (t < -20.0f) {
    return floor;
  }
  return floor + softness * log(1.0f + exp(t));
}

static inline float metal_hls_acescc_decode(float acescc) {
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
    return (exp2(acescc * kB - kA) - kDenormOffset) * 2.0f;
  }
  return exp2(acescc * kB - kA);
}

static inline float metal_hls_acescc_encode(float linear_ap1) {
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
    return (log2(kDenormOffset + linear_ap1 * 0.5f) + kA) / kB;
  }
  return (log2(linear_ap1) + kA) / kB;
}

static inline float3 metal_hls_acescc_to_ap1(float3 acescc) {
  return float3(metal_hls_acescc_decode(acescc.x), metal_hls_acescc_decode(acescc.y),
                metal_hls_acescc_decode(acescc.z));
}

static inline float3 metal_hls_ap1_to_acescc(float3 ap1) {
  return float3(metal_hls_acescc_encode(ap1.x), metal_hls_acescc_encode(ap1.y),
                metal_hls_acescc_encode(ap1.z));
}

static inline float3 metal_hls_ap1_to_oklab(float3 ap1) {
  const float l = 0.62217537f * ap1.x + 0.34268438f * ap1.y + 0.02339492f * ap1.z;
  const float m = 0.26593478f * ap1.x + 0.62930460f * ap1.y + 0.10828100f * ap1.z;
  const float s = 0.09725037f * ap1.x + 0.18525749f * ap1.y + 0.77254586f * ap1.z;

  const float l_ = sign(l) * pow(fabs(l), 1.0f / 3.0f);
  const float m_ = sign(m) * pow(fabs(m), 1.0f / 3.0f);
  const float s_ = sign(s) * pow(fabs(s), 1.0f / 3.0f);

  return float3(0.2104542553f * l_ + 0.7936177850f * m_ - 0.0040720468f * s_,
                1.9779984951f * l_ - 2.4285922050f * m_ + 0.4505937099f * s_,
                0.0259040371f * l_ + 0.7827717662f * m_ - 0.8086757660f * s_);
}

static inline float3 metal_hls_oklab_to_ap1(float3 lab) {
  const float l_ = lab.x + 0.3963377774f * lab.y + 0.2158037573f * lab.z;
  const float m_ = lab.x - 0.1055613458f * lab.y - 0.0638541728f * lab.z;
  const float s_ = lab.x - 0.0894841775f * lab.y - 1.2914855480f * lab.z;

  const float l = l_ * l_ * l_;
  const float m = m_ * m_ * m_;
  const float s = s_ * s_ * s_;

  return float3(2.09085732f * l - 1.16812363f * m + 0.10040848f * s,
                -0.87435428f * l + 2.14592958f * m - 0.27429822f * s,
                -0.05353206f * l - 0.36754978f * m + 1.34755888f * s);
}

static inline float3 metal_hls_fit_ap1_lower_gamut(float3 adjusted_ap1, float3 neutral_ap1) {
  constexpr float kLower = -1e-5f;
  float           scale  = 1.0f;

  if (adjusted_ap1.x < kLower && neutral_ap1.x > adjusted_ap1.x) {
    scale = fmin(scale, (neutral_ap1.x - kLower) / (neutral_ap1.x - adjusted_ap1.x));
  }
  if (adjusted_ap1.y < kLower && neutral_ap1.y > adjusted_ap1.y) {
    scale = fmin(scale, (neutral_ap1.y - kLower) / (neutral_ap1.y - adjusted_ap1.y));
  }
  if (adjusted_ap1.z < kLower && neutral_ap1.z > adjusted_ap1.z) {
    scale = fmin(scale, (neutral_ap1.z - kLower) / (neutral_ap1.z - adjusted_ap1.z));
  }

  scale = clamp(scale, 0.0f, 1.0f);
  return neutral_ap1 + (adjusted_ap1 - neutral_ap1) * scale;
}

// === Highlight / Shadow Local Tone ============================================

static inline float metal_hs_ap1_luminance(float3 ap1) {
  return 0.27222872f * ap1.x + 0.67408177f * ap1.y + 0.05368952f * ap1.z;
}

static inline float metal_hs_log2_luminance_from_acescc(float4 px) {
  const float3 ap1 = metal_hls_acescc_to_ap1(px.xyz);
  return log2(fmax(metal_hs_ap1_luminance(ap1), 1.0e-8f));
}

static inline float metal_hs_range_weight(float center, float sample) {
  const float edge_delta = fmax(fabs(sample - center) - 0.24f, 0.0f);
  const float d = edge_delta / 0.48f;
  return exp2(-(d * d));
}

static inline float metal_hs_shadow_upper_pivot(constant MetalFusedParams& params) {
  const float width = fmax(params.hs_shadow_log_width_, 0.35f);
  return params.hs_shadow_log_pivot_ + fmax(width * 0.40f, 0.24f);
}

static inline float metal_hs_shadow_black_floor_weight(float mask_ref,
                                                       constant MetalFusedParams& params) {
  const float width = fmax(params.hs_shadow_log_width_, 0.35f);
  const float upper_pivot = metal_hs_shadow_upper_pivot(params);
  const float black_start = upper_pivot - fmax(width * 7.20f, 4.45f);
  const float black_end = upper_pivot - fmax(width * 5.20f, 3.25f);
  const float toe = metal_smoothstep_range(black_start, black_end, mask_ref);
  return 0.30f + 0.70f * toe;
}

static inline float metal_hs_shadow_zone_weight(float mask_ref,
                                                constant MetalFusedParams& params) {
  const float width = fmax(params.hs_shadow_log_width_, 0.35f);
  const float upper_pivot = metal_hs_shadow_upper_pivot(params);
  const float fade_start = upper_pivot - fmax(width * 3.15f, 1.95f);
  const float tonal_weight = 1.0f - metal_smoothstep_range(fade_start, upper_pivot, mask_ref);
  const float black_floor = metal_hs_shadow_black_floor_weight(mask_ref, params);
  return clamp(tonal_weight * black_floor, 0.0f, 1.0f);
}

static inline float metal_hs_highlight_zone_weight(float mask_ref,
                                                   constant MetalFusedParams& params) {
  constexpr float kToe = 1.0e-3f;
  constexpr float kToePow = 0.1023292992f;
  constexpr float kInvToeRange = 1.1135850f;
  constexpr float kGamma = 0.33f;
  const float t =
      metal_smoothstep_range(params.hs_highlight_log_pivot_,
                             params.hs_highlight_log_pivot_ + params.hs_highlight_log_width_,
                             mask_ref);
  const float lifted = pow(t + kToe, kGamma);
  return clamp((lifted - kToePow) * kInvToeRange, 0.0f, 1.0f);
}

static inline float metal_hs_softplus_distance(float distance, float softness) {
  constexpr float kLog2 = 0.6931471805599453f;
  const float safe_softness = fmax(softness, 1.0e-4f);
  const float x = distance / safe_softness;
  if (x > 20.0f) {
    return distance - safe_softness * kLog2;
  }
  return safe_softness * (log(1.0f + exp(x)) - kLog2);
}

static inline float metal_hs_softrelu_distance(float signed_distance, float softness,
                                               float onset) {
  const float safe_softness = fmax(softness, 1.0e-4f);
  const float safe_onset = fmax(onset, 0.0f);
  const float x = (signed_distance - safe_onset) / safe_softness;
  if (x > 20.0f) {
    return signed_distance - safe_onset;
  }
  if (x < -20.0f) {
    return safe_softness * exp(x);
  }
  return safe_softness * log(1.0f + exp(x));
}

static inline float metal_hs_texture_detail_weight(float detail) {
  return 1.0f - metal_smoothstep_range(0.28f, 0.88f, fabs(detail));
}

static inline float metal_hs_active_mask_disagreement(
    float base_shadow_mask, float base_highlight_mask, float source_shadow_mask,
    float source_highlight_mask, float shadow_amount, float highlight_amount) {
  float disagreement = 0.0f;
  if (fabs(shadow_amount) > 1.0e-6f) {
    disagreement = fmax(disagreement, fabs(base_shadow_mask - source_shadow_mask));
    disagreement =
        fmax(disagreement, 0.50f * fabs(base_highlight_mask - source_highlight_mask));
  }
  if (fabs(highlight_amount) > 1.0e-6f) {
    disagreement = fmax(disagreement, fabs(base_highlight_mask - source_highlight_mask));
  }
  return clamp(disagreement, 0.0f, 1.0f);
}

static inline float metal_hs_tonal_reference_mix(float mask_disagreement) {
  return metal_smoothstep_range(0.025f, 0.16f, mask_disagreement);
}

static inline float metal_hs_local_detail_reference_guard(float detail,
                                                          float tonal_reference_mix) {
  const float edge_reference = metal_smoothstep_range(0.42f, 0.95f, fabs(detail));
  return 1.0f - tonal_reference_mix * edge_reference;
}

static inline float metal_hs_chromatic_fringe_guard(float source_chroma, float detail,
                                                    float tonal_reference_mix,
                                                    float active_highlight_mask,
                                                    float shadow_amount,
                                                    float highlight_amount) {
  if (highlight_amount <= 1.0e-6f && shadow_amount <= 1.0e-6f) {
    return 1.0f;
  }

  const float chroma_gate = metal_smoothstep_range(0.012f, 0.060f, source_chroma);
  const float detail_gate = metal_smoothstep_range(0.055f, 0.34f, fabs(detail)) *
                            (1.0f - metal_smoothstep_range(0.82f, 1.42f, fabs(detail)));
  const float edge_gate = metal_smoothstep_range(0.020f, 0.12f, tonal_reference_mix);
  const float highlight_gate = 0.35f + 0.65f * active_highlight_mask;
  const float strength = 0.82f * chroma_gate * detail_gate * edge_gate * highlight_gate;
  return 1.0f - clamp(strength, 0.0f, 0.82f);
}

static inline float metal_hs_chromatic_local_mix_guard(
    float source_chroma, float detail, float local_delta, float source_delta,
    float active_highlight_mask, float shadow_amount, float highlight_amount) {
  if (highlight_amount <= 1.0e-6f && shadow_amount <= 1.0e-6f) {
    return 1.0f;
  }

  const float chroma_gate = metal_smoothstep_range(0.012f, 0.055f, source_chroma);
  const float detail_gate = metal_smoothstep_range(0.050f, 0.20f, fabs(detail)) *
                            (1.0f - metal_smoothstep_range(0.85f, 1.45f, fabs(detail)));
  const float mismatch_gate = metal_smoothstep_range(0.055f, 0.16f,
                                                     fabs(local_delta - source_delta));
  const float highlight_gate = 0.35f + 0.65f * active_highlight_mask;
  const float both_active_gate =
      0.55f + 0.45f * clamp(fmin(fmax(shadow_amount, 0.0f),
                                  fmax(highlight_amount, 0.0f)),
                            0.0f, 1.0f);
  const float strength =
      0.78f * chroma_gate * detail_gate * mismatch_gate * highlight_gate * both_active_gate;
  return 1.0f - clamp(strength, 0.0f, 0.78f);
}

static inline float metal_hs_llf_detail_mix(float detail) {
  return 1.0f - metal_smoothstep_range(0.42f, 0.95f, fabs(detail));
}

static inline float metal_hs_local_tone_mix(float detail, float local_delta, float source_delta) {
  const float mag = fabs(detail);
  const float edge_weight = metal_smoothstep_range(0.62f, 1.55f, mag);
  const float delta_mismatch =
      metal_smoothstep_range(0.16f, 0.52f, fabs(local_delta - source_delta));
  const float guard =
      clamp(edge_weight * (0.82f + 0.18f * delta_mismatch) +
                0.18f * edge_weight * edge_weight,
            0.0f, 1.0f);
  return 1.0f - guard;
}

static inline float metal_hs_shadow_detail_preserve_weight(float detail) {
  const float mag = fabs(detail);
  const float noise_gate = metal_smoothstep_range(0.045f, 0.15f, mag);
  const float edge_guard = 1.0f - metal_smoothstep_range(0.78f, 1.35f, mag);
  return noise_gate * edge_guard;
}

static inline float metal_hs_shadow_llf_detail_gain(float detail, float shadow_amount,
                                                    float shadow_zone) {
  const float lift_amount = fmax(shadow_amount, 0.0f);
  if (lift_amount <= 1.0e-6f || shadow_zone <= 1.0e-6f) {
    return 1.0f;
  }

  const float mag = fabs(detail);
  const float noise_gate = metal_smoothstep_range(0.035f, 0.11f, mag);
  const float fine_detail_gate = 1.0f - metal_smoothstep_range(0.38f, 0.82f, mag);
  if (noise_gate <= 0.0f || fine_detail_gate <= 0.0f) {
    return 1.0f;
  }

  constexpr float kSigmaRStops = 0.42f;
  const float x = clamp(mag / kSigmaRStops, 1.0e-4f, 1.0f);
  const float alpha = 1.0f - 0.38f * lift_amount;
  const float remapped_mag = kSigmaRStops * pow(x, alpha);
  const float remap_gain = remapped_mag / fmax(mag, 1.0e-4f);
  const float limited_gain = clamp(remap_gain - 1.0f, 0.0f, 0.38f);
  const float mix = lift_amount * shadow_zone * noise_gate * fine_detail_gate;
  return 1.0f + limited_gain * mix;
}

static inline float metal_hs_shadow_detail_sign_weight(float detail) {
  if (detail >= 0.0f) {
    return 1.0f;
  }
  return 1.0f - 0.65f * metal_smoothstep_range(0.18f, 0.72f, -detail);
}

static inline void metal_hs_compute_masks(float mask_ref, float shadow_amount,
                                          float highlight_amount,
                                          constant MetalFusedParams& params,
                                          thread float& shadow_mask,
                                          thread float& highlight_mask) {
  const float raw_shadow = metal_hs_shadow_zone_weight(mask_ref, params);
  const float raw_highlight = metal_hs_highlight_zone_weight(mask_ref, params);
  const bool both_active = fabs(shadow_amount) > 1.0e-6f &&
                           fabs(highlight_amount) > 1.0e-6f;
  shadow_mask = both_active ? raw_shadow * (1.0f - 0.72f * raw_highlight) : raw_shadow;
  highlight_mask =
      both_active ? raw_highlight * (1.0f - 0.48f * raw_shadow) : raw_highlight;
}

static inline float metal_hs_shadow_base_delta_from_ref(float mask_ref, float shadow_amount,
                                                        float highlight_amount,
                                                        constant MetalFusedParams& params) {
  float shadow_mask = 0.0f;
  float highlight_mask = 0.0f;
  metal_hs_compute_masks(mask_ref, shadow_amount, highlight_amount, params, shadow_mask,
                         highlight_mask);

  const float width = fmax(params.hs_shadow_log_width_, 0.35f);
  const float upper_pivot = metal_hs_shadow_upper_pivot(params);
  const float lift_pivot = upper_pivot + fmax(width * 4.00f, 2.48f);
  const float distance_to_pivot = fmax(lift_pivot - mask_ref, 0.0f);
  const float soft_distance =
      metal_hs_softplus_distance(distance_to_pivot, fmax(width * 2.18f, 1.35f));
  const float lift_shape = 1.0f - exp(-soft_distance / 1.25f);
  const float deep_noise_compression =
      1.0f - metal_smoothstep_range(params.hs_shadow_log_pivot_ - 4.15f,
                                    params.hs_shadow_log_pivot_ - 2.35f, mask_ref);
  const float black_guard = 0.82f + 0.18f * metal_hs_shadow_black_floor_weight(mask_ref, params);
  const float highlight_overlap = clamp(highlight_mask, 0.0f, 1.0f);
  const float highlight_active = (fabs(highlight_amount) > 1.0e-6f) ? 1.0f : 0.0f;
  const float overlap_guard = 1.0f - 0.28f * highlight_active * highlight_overlap;
  const float lift_amount = fmax(shadow_amount, 0.0f);
  const float darken_amount = fmax(-shadow_amount, 0.0f);
  const float lift_delta =
      lift_amount * (0.88f * lift_shape * black_guard * overlap_guard +
                     0.28f * deep_noise_compression);
  const float darken_delta =
      darken_amount * 0.34f * soft_distance * (0.85f + 0.15f * black_guard);
  return lift_delta - darken_delta;
}

static inline float metal_hs_highlight_base_delta_from_ref(float mask_ref, float shadow_amount,
                                                           float highlight_amount,
                                                           constant MetalFusedParams& params) {
  (void)shadow_amount;
  const float width = fmax(params.hs_highlight_log_width_, 0.35f);
  const float soft_distance = metal_hs_softrelu_distance(
      mask_ref - params.hs_highlight_log_pivot_, fmin(fmax(width * 0.12f, 0.36f), 0.55f),
      fmin(fmax(width * 0.24f, 0.72f), 1.10f));
  const float reduce_amount = fmax(highlight_amount, 0.0f);
  const float boost_amount = fmax(-highlight_amount, 0.0f);
  const float reduce_delta = 1.68f * (1.0f - exp(-soft_distance / 1.33f));
  const float boost_delta = 1.24f * (1.0f - exp(-soft_distance / 1.45f));
  return boost_amount * boost_delta - reduce_amount * reduce_delta;
}

static inline float metal_hs_base_delta_from_ref(float mask_ref, float shadow_amount,
                                                 float highlight_amount,
                                                 constant MetalFusedParams& params) {
  return metal_hs_shadow_base_delta_from_ref(mask_ref, shadow_amount, highlight_amount, params) +
         metal_hs_highlight_base_delta_from_ref(mask_ref, shadow_amount, highlight_amount, params);
}

static inline float metal_hs_base_curve_slope(float mask_ref, float shadow_amount,
                                              float highlight_amount,
                                              constant MetalFusedParams& params) {
  constexpr float kEpsStops = 0.08f;
  const float delta_lo =
      metal_hs_base_delta_from_ref(mask_ref - kEpsStops, shadow_amount, highlight_amount, params);
  const float delta_hi =
      metal_hs_base_delta_from_ref(mask_ref + kEpsStops, shadow_amount, highlight_amount, params);
  return 1.0f + (delta_hi - delta_lo) / (2.0f * kEpsStops);
}

static inline float metal_hs_shadow_curve_slope(float mask_ref, float shadow_amount,
                                                float highlight_amount,
                                                constant MetalFusedParams& params) {
  constexpr float kEpsStops = 0.08f;
  const float delta_lo = metal_hs_shadow_base_delta_from_ref(
      mask_ref - kEpsStops, shadow_amount, highlight_amount, params);
  const float delta_hi = metal_hs_shadow_base_delta_from_ref(
      mask_ref + kEpsStops, shadow_amount, highlight_amount, params);
  return 1.0f + (delta_hi - delta_lo) / (2.0f * kEpsStops);
}

static inline float metal_hs_highlight_curve_slope(float mask_ref, float shadow_amount,
                                                   float highlight_amount,
                                                   constant MetalFusedParams& params) {
  constexpr float kEpsStops = 0.08f;
  const float delta_lo = metal_hs_highlight_base_delta_from_ref(
      mask_ref - kEpsStops, shadow_amount, highlight_amount, params);
  const float delta_hi = metal_hs_highlight_base_delta_from_ref(
      mask_ref + kEpsStops, shadow_amount, highlight_amount, params);
  return 1.0f + (delta_hi - delta_lo) / (2.0f * kEpsStops);
}

static inline float metal_hs_shadow_detail_weight(float mask_ref, float shadow_mask,
                                                  constant MetalFusedParams& params) {
  (void)shadow_mask;
  const float tonal_weight = metal_hs_shadow_zone_weight(mask_ref, params);
  const float signal_gate = metal_smoothstep_range(params.hs_shadow_log_pivot_ - 2.60f,
                                                   params.hs_shadow_log_pivot_ - 1.20f,
                                                   mask_ref);
  const float upper_guard =
      1.0f - metal_smoothstep_range(params.hs_shadow_log_pivot_ + 1.05f,
                                    params.hs_shadow_log_pivot_ + 2.10f, mask_ref);
  const float practical_shadow = signal_gate * upper_guard;
  return fmax(tonal_weight * signal_gate, 0.72f * practical_shadow);
}

static inline float metal_hs_shadow_fill_light_weight(float mask_ref,
                                                      constant MetalFusedParams& params) {
  const float signal_gate = metal_smoothstep_range(params.hs_shadow_log_pivot_ - 2.45f,
                                                   params.hs_shadow_log_pivot_ - 1.05f,
                                                   mask_ref);
  const float upper_guard =
      1.0f - metal_smoothstep_range(params.hs_shadow_log_pivot_ + 1.55f,
                                    params.hs_shadow_log_pivot_ + 2.50f, mask_ref);
  return signal_gate * upper_guard;
}

static inline float metal_hs_highlight_detail_weight(float mask_ref, float highlight_mask,
                                                     float detail,
                                                     constant MetalFusedParams& params) {
  const float width = fmax(params.hs_highlight_log_width_, 0.35f);
  const float tonal_weight = clamp(highlight_mask, 0.0f, 1.0f);
  const float noise_gate = metal_smoothstep_range(0.035f, 0.12f, fabs(detail));
  const float edge_guard = 1.0f - metal_smoothstep_range(0.78f, 1.45f, fabs(detail));
  const float clipped_guard =
      1.0f - metal_smoothstep_range(params.hs_highlight_log_pivot_ + width * 1.15f,
                                    params.hs_highlight_log_pivot_ + width * 2.35f, mask_ref);
  return tonal_weight * noise_gate * edge_guard * clipped_guard;
}

static inline float3 metal_hs_preserve_highlight_chroma(float3 source_ap1, float3 output_ap1,
                                                        float log_delta, float highlight_amount,
                                                        float source_highlight_mask) {
  const float reduce_amount = fmax(highlight_amount, 0.0f);
  if (reduce_amount <= 1.0e-6f || log_delta >= -1.0e-5f ||
      source_highlight_mask <= 1.0e-5f) {
    return output_ap1;
  }

  const float3 source_lab = metal_hls_ap1_to_oklab(source_ap1);
  const float3 output_lab = metal_hls_ap1_to_oklab(output_ap1);
  const float source_chroma = length(source_lab.yz);
  const float output_chroma = length(output_lab.yz);
  if (source_chroma <= 1.0e-5f || output_lab.x <= 1.0e-5f) {
    return output_ap1;
  }

  const float chroma_confidence = metal_smoothstep_range(0.012f, 0.060f, source_chroma);
  const float compression = metal_smoothstep_range(0.18f, 1.15f, -log_delta);
  const float strength = clamp(0.56f * reduce_amount * source_highlight_mask * compression *
                                   chroma_confidence,
                               0.0f, 0.62f);
  if (strength <= 1.0e-6f) {
    return output_ap1;
  }

  const float target_chroma = output_chroma + (source_chroma - output_chroma) * strength;
  const float inv_source_chroma = 1.0f / fmax(source_chroma, 1.0e-5f);
  const float2 hue_dir = float2(source_lab.y * inv_source_chroma,
                                source_lab.z * inv_source_chroma);
  const float3 adjusted_lab =
      float3(output_lab.x, hue_dir.x * target_chroma, hue_dir.y * target_chroma);
  const float3 neutral_lab = float3(output_lab.x, 0.0f, 0.0f);
  const float3 neutral_ap1 = metal_hls_oklab_to_ap1(neutral_lab);
  return metal_hls_fit_ap1_lower_gamut(metal_hls_oklab_to_ap1(adjusted_lab), neutral_ap1);
}

static inline float4 GPU_HighlightShadowLocalToneOpKernel(float4 px, float base,
                                                          constant MetalFusedParams& params) {
  const float shadow_amount =
      (params.shadows_enabled_ != 0u) ? clamp(params.shadows_offset_, -1.0f, 1.0f) : 0.0f;
  const float highlight_amount = (params.highlights_enabled_ != 0u)
                                     ? clamp(-params.highlights_offset_ * 0.5f, -1.0f, 1.0f)
                                     : 0.0f;
  if (fabs(shadow_amount) <= 1.0e-6f && fabs(highlight_amount) <= 1.0e-6f) {
    return px;
  }

  const float3 source_ap1 = metal_hls_acescc_to_ap1(px.xyz);
  const float source_log_y = log2(fmax(metal_hs_ap1_luminance(source_ap1), 1.0e-8f));
  const float detail = source_log_y - base;
  const float base_mask_ref = base;
  float base_shadow_mask = 0.0f;
  float base_highlight_mask = 0.0f;
  float source_shadow_mask = 0.0f;
  float source_highlight_mask = 0.0f;
  metal_hs_compute_masks(base_mask_ref, shadow_amount, highlight_amount, params,
                         base_shadow_mask, base_highlight_mask);
  metal_hs_compute_masks(source_log_y, shadow_amount, highlight_amount, params,
                         source_shadow_mask, source_highlight_mask);

  const float mask_disagreement = metal_hs_active_mask_disagreement(
      base_shadow_mask, base_highlight_mask, source_shadow_mask, source_highlight_mask,
      shadow_amount, highlight_amount);
  const float tonal_reference_mix = metal_hs_tonal_reference_mix(mask_disagreement);
  const float mask_ref = mix(base_mask_ref, source_log_y, tonal_reference_mix);
  float shadow_mask = 0.0f;
  float highlight_mask = 0.0f;
  metal_hs_compute_masks(mask_ref, shadow_amount, highlight_amount, params, shadow_mask,
                         highlight_mask);

  const float base_delta =
      metal_hs_base_delta_from_ref(mask_ref, shadow_amount, highlight_amount, params);
  const float base_curve_slope = clamp(
      metal_hs_base_curve_slope(mask_ref, shadow_amount, highlight_amount, params), 0.42f, 1.65f);
  const float base_contrast_loss = clamp(1.0f / base_curve_slope - 1.0f, 0.0f, 0.62f);
  const float shadow_curve_slope =
      clamp(metal_hs_shadow_curve_slope(mask_ref, shadow_amount, highlight_amount, params),
            0.58f, 1.35f);
  const float shadow_contrast_loss = clamp(1.0f / shadow_curve_slope - 1.0f, 0.0f, 0.46f);
  const float highlight_curve_slope =
      clamp(metal_hs_highlight_curve_slope(mask_ref, shadow_amount, highlight_amount, params),
            0.50f, 1.20f);
  const float highlight_contrast_loss =
      clamp(1.0f / highlight_curve_slope - 1.0f, 0.0f, 0.42f);
  const float shadow_texture_zone = metal_hs_shadow_detail_weight(mask_ref, shadow_mask, params);
  const float texture_detail = metal_hs_texture_detail_weight(detail);
  const float shadow_detail_sign = metal_hs_shadow_detail_sign_weight(detail);
  const float shadow_detail_zone = shadow_texture_zone * texture_detail * shadow_detail_sign;
  const float shadow_detail_preserve = metal_hs_shadow_detail_preserve_weight(detail);
  const float shadow_fill_light_zone = metal_hs_shadow_fill_light_weight(mask_ref, params);
  const float active_highlight_mask =
      (fabs(highlight_amount) > 1.0e-6f) ? clamp(highlight_mask, 0.0f, 1.0f) : 0.0f;
  const float3 source_lab = metal_hls_ap1_to_oklab(source_ap1);
  const float source_chroma = length(source_lab.yz);
  const float chromatic_fringe_guard = metal_hs_chromatic_fringe_guard(
      source_chroma, detail, tonal_reference_mix, active_highlight_mask, shadow_amount,
      highlight_amount);
  const float fill_highlight_guard = 1.0f - 0.35f * active_highlight_mask;
  const float fill_detail_polarity = detail >= 0.0f ? 1.0f : 0.68f;
  const float highlight_detail_zone =
      metal_hs_highlight_detail_weight(mask_ref, highlight_mask, detail, params) * texture_detail;
  const float llf_detail_gain =
      metal_hs_shadow_llf_detail_gain(
          detail, shadow_amount,
          fmax(shadow_texture_zone, 0.86f * shadow_fill_light_zone * fill_highlight_guard));
  const float guarded_llf_detail_gain =
      1.0f + (llf_detail_gain - 1.0f) * chromatic_fringe_guard;
  const float dark_valley = metal_smoothstep_range(0.85f, 1.80f, -detail);
  const float contrast_recovery =
      0.035f + 0.075f * fmax(base_contrast_loss, shadow_contrast_loss);
  const float fill_light_recovery =
      0.075f + 0.085f * fmax(base_contrast_loss, shadow_contrast_loss);
  const float shadow_detail_scale =
      fmax(shadow_amount, 0.0f) * shadow_detail_zone * shadow_detail_preserve *
          contrast_recovery -
      0.018f * fmax(shadow_amount, 0.0f) * shadow_texture_zone * dark_valley +
      fmax(shadow_amount, 0.0f) * shadow_fill_light_zone * fill_highlight_guard * texture_detail *
          shadow_detail_preserve * fill_detail_polarity * fill_light_recovery +
      0.025f * fmax(-shadow_amount, 0.0f) * shadow_detail_zone *
          fmax(base_contrast_loss, shadow_contrast_loss);
  const float guarded_shadow_detail_scale = shadow_detail_scale * chromatic_fringe_guard;
  const float highlight_detail_scale =
      fmax(highlight_amount, 0.0f) * highlight_detail_zone *
      (0.035f + 0.12f * highlight_contrast_loss);
  const float guarded_highlight_detail_scale = highlight_detail_scale * chromatic_fringe_guard;
  const float raw_detail_scale =
      clamp((1.0f + guarded_shadow_detail_scale + guarded_highlight_detail_scale) *
                guarded_llf_detail_gain,
            0.97f, 1.30f);
  const float detail_scale =
      1.0f + (raw_detail_scale - 1.0f) * metal_hs_llf_detail_mix(detail);
  const float local_delta = base_delta + detail * (detail_scale - 1.0f);
  const float local_adjusted_log_y = source_log_y + local_delta;
  const float source_delta =
      metal_hs_base_delta_from_ref(source_log_y, shadow_amount, highlight_amount, params);
  const float source_adjusted_log_y = source_log_y + source_delta;
  const float chromatic_local_mix_guard = metal_hs_chromatic_local_mix_guard(
      source_chroma, detail, local_delta, source_delta, active_highlight_mask, shadow_amount,
      highlight_amount);
  const float local_mix =
      metal_hs_local_tone_mix(detail, local_delta, source_delta) *
      metal_hs_local_detail_reference_guard(detail, tonal_reference_mix) *
      chromatic_fringe_guard * chromatic_local_mix_guard;
  const float adjusted_log_y = mix(source_adjusted_log_y, local_adjusted_log_y, local_mix);
  const float log_delta = adjusted_log_y - source_log_y;

  const float rgb_scale = exp2(clamp(log_delta, -3.5f, 3.5f));
  float3 output_ap1 = source_ap1 * rgb_scale;
  output_ap1 = metal_hs_preserve_highlight_chroma(source_ap1, output_ap1, log_delta,
                                                  highlight_amount, source_highlight_mask);
  return float4(metal_hls_ap1_to_acescc(output_ap1), px.w);
}

static inline float4 GPU_TintOpKernel(float4 px, constant MetalFusedParams& params) {
  if (params.tint_enabled_ == 0u) {
    return px;
  }

  px.y += params.tint_offset_;
  return px;
}

static inline float4 GPU_VibranceOpKernel(float4 px, constant MetalFusedParams& params) {
  if (params.vibrance_enabled_ == 0u) {
    return px;
  }

  const float max_val = fmax(fmax(px.x, px.y), px.z);
  const float min_val = fmin(fmin(px.x, px.y), px.z);
  const float chroma  = max_val - min_val;
  const float strength = params.vibrance_offset_;
  const float falloff  = exp(-3.0f * chroma);
  const float scale    = 1.0f + strength * falloff;

  if (params.vibrance_offset_ >= 0.0f) {
    const float luma = px.x * 0.299f + px.y * 0.587f + px.z * 0.114f;
    px.x             = luma + (px.x - luma) * scale;
    px.y             = luma + (px.y - luma) * scale;
    px.z             = luma + (px.z - luma) * scale;
  } else {
    const float avg = (px.x + px.y + px.z) / 3.0f;
    px.x += (avg - px.x) * (1.0f - scale);
    px.y += (avg - px.y) * (1.0f - scale);
    px.z += (avg - px.z) * (1.0f - scale);
  }
  return px;
}

static inline float4 GPU_ColorWheelOpKernel(float4 px, constant MetalFusedParams& params) {
  if (params.color_wheel_enabled_ == 0u) {
    return px;
  }

  constexpr float kEps = 1e-6f;
  const float offset_r = params.lift_color_offset_[0] + params.lift_luminance_offset_;
  const float offset_g = params.lift_color_offset_[1] + params.lift_luminance_offset_;
  const float offset_b = params.lift_color_offset_[2] + params.lift_luminance_offset_;

  const float slope_r  = fmax(params.gain_color_offset_[0] + params.gain_luminance_offset_, kEps);
  const float slope_g  = fmax(params.gain_color_offset_[1] + params.gain_luminance_offset_, kEps);
  const float slope_b  = fmax(params.gain_color_offset_[2] + params.gain_luminance_offset_, kEps);

  const float power_r  = fmax(params.gamma_color_offset_[0] + params.gamma_luminance_offset_, kEps);
  const float power_g  = fmax(params.gamma_color_offset_[1] + params.gamma_luminance_offset_, kEps);
  const float power_b  = fmax(params.gamma_color_offset_[2] + params.gamma_luminance_offset_, kEps);

  const float base_r   = fmax(px.x * slope_r + offset_r, 0.0f);
  const float base_g   = fmax(px.y * slope_g + offset_g, 0.0f);
  const float base_b   = fmax(px.z * slope_b + offset_b, 0.0f);

  px.x                 = pow(base_r, power_r);
  px.y                 = pow(base_g, power_g);
  px.z                 = pow(base_b, power_b);
  return px;
}

static inline float4 GPU_HLSOpKernel(float4 px, constant MetalFusedParams& params) {
  if (params.hls_enabled_ == 0u && params.saturation_enabled_ == 0u) {
    return px;
  }

  constexpr float kEps = 1e-6f;
  constexpr float kPi  = 3.14159265358979323846f;
  const float3 source_ap1    = metal_hls_acescc_to_ap1(px.xyz);
  const float3 source_lab    = metal_hls_ap1_to_oklab(source_ap1);
  const float  source_chroma = length(source_lab.yz);
  if (source_chroma <= kEps) {
    return px;
  }
  const float source_hue = metal_wrap_hue(atan2(source_lab.z, source_lab.y) * (180.0f / kPi));

  float3 curve = float3(0.0f);
  if (params.hls_enabled_ != 0u) {
    int profile_count = params.hls_profile_count_;
    if (profile_count < 1) {
      profile_count = 1;
    }
    if (profile_count > kMetalHlsProfileCount) {
      profile_count = kMetalHlsProfileCount;
    }

    float accum_h = 0.0f;
    float accum_l = 0.0f;
    float accum_c = 0.0f;
    float accum_weight = 0.0f;
    int   nearest = 0;
    float nearest_dist = metal_hue_distance(source_hue, metal_wrap_hue(params.hls_profile_hues_[0]));

    for (int i = 0; i < kMetalHlsProfileCount; ++i) {
      if (i >= profile_count) {
        continue;
      }

      const float width     = fmax(params.hls_profile_hue_ranges_[i], 1.0f);
      const float target_h  = metal_wrap_hue(params.hls_profile_hues_[i]);
      const float hue_dist  = metal_hue_distance(source_hue, target_h);
      if (hue_dist < nearest_dist) {
        nearest_dist = hue_dist;
        nearest      = i;
      }

      const float t      = hue_dist / width;
      const float weight = exp2(-(t * t));
      accum_h += params.hls_profile_adjustments_[i][0] * weight;
      accum_l += params.hls_profile_adjustments_[i][1] * weight;
      accum_c += params.hls_profile_adjustments_[i][2] * weight;
      accum_weight += weight;
    }

    curve = float3(params.hls_profile_adjustments_[nearest][0],
                   params.hls_profile_adjustments_[nearest][1],
                   params.hls_profile_adjustments_[nearest][2]);
    if (accum_weight > kEps) {
      curve = float3(accum_h, accum_l, accum_c) / accum_weight;
    }
  }
  const float saturation_scale =
      (params.saturation_enabled_ != 0u) ? fmax(params.saturation_offset_, 0.0f) : 1.0f;
  if (fabs(curve.x) <= kEps && fabs(curve.y) <= kEps && fabs(curve.z) <= kEps &&
      fabs(saturation_scale - 1.0f) <= kEps) {
    return px;
  }

  const bool  has_curve = fabs(curve.x) > kEps || fabs(curve.y) > kEps ||
                          fabs(curve.z) > kEps;
  float       protection = 0.0f;
  if (has_curve) {
    const float chroma_confidence    = metal_smoothstep_range(0.005f, 0.030f, source_chroma);
    const float shadow_confidence    = metal_smoothstep_range(0.005f, 0.050f, source_lab.x);
    const float highlight_confidence = 1.0f - metal_smoothstep_range(1.35f, 2.25f, source_lab.x);
    protection = clamp(chroma_confidence * shadow_confidence * highlight_confidence, 0.0f, 1.0f);
  }
  if (protection <= kEps && fabs(saturation_scale - 1.0f) <= kEps) {
    return px;
  }

  constexpr float kCurveGain = 2.25f;
  const float adjusted_hue_rad =
      metal_wrap_hue(source_hue + curve.x * kCurveGain * protection) * (kPi / 180.0f);
  const float adjusted_lightness =
      (fabs(curve.y) > kEps && protection > kEps)
          ? metal_soft_floor(source_lab.x + curve.y * kCurveGain * 0.5f * protection, 0.0f,
                             0.02f)
          : source_lab.x;
  const float chroma_strength = (curve.z >= 0.0f) ? 4.5f : 3.25f;
  const float adjusted_chroma =
      source_chroma * saturation_scale *
      exp2(curve.z * kCurveGain * chroma_strength * protection);

  const float3 adjusted_lab =
      float3(adjusted_lightness, adjusted_chroma * cos(adjusted_hue_rad),
             adjusted_chroma * sin(adjusted_hue_rad));
  const float3 neutral_lab = float3(adjusted_lightness, 0.0f, 0.0f);
  const float3 output_ap1 =
      metal_hls_fit_ap1_lower_gamut(metal_hls_oklab_to_ap1(adjusted_lab),
                                    metal_hls_oklab_to_ap1(neutral_lab));
  px.xyz = metal_hls_ap1_to_acescc(output_ap1);
  return px;
}
