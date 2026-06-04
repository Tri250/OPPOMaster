//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifndef ALCEDO_OPENCL_EDIT_PIPELINE_COLOR_CL
#define ALCEDO_OPENCL_EDIT_PIPELINE_COLOR_CL

#define ALCEDO_OPENCL_HLS_PROFILE_COUNT 8
#define ALCEDO_OPENCL_HS_HIGHLIGHT_STRENGTH_SCALE 1.5f

// === Tint =====================================================================

static inline float4 opencl_tint_op(float4 px, __global const OpenClFusedParams* params) {
  if (params->tint_enabled_ == 0u) return px;
  px.y += params->tint_offset_;
  return px;
}

// === Vibrance =================================================================

static inline float4 opencl_vibrance_op(float4 px, __global const OpenClFusedParams* params) {
  if (params->vibrance_enabled_ == 0u) return px;

  const float max_val  = fmax(fmax(px.x, px.y), px.z);
  const float min_val  = fmin(fmin(px.x, px.y), px.z);
  const float chroma   = max_val - min_val;
  const float strength = params->vibrance_offset_;
  const float falloff  = exp(-3.0f * chroma);
  const float scale    = 1.0f + strength * falloff;

  if (params->vibrance_offset_ >= 0.0f) {
    const float luma = px.x * 0.299f + px.y * 0.587f + px.z * 0.114f;
    px.x = luma + (px.x - luma) * scale;
    px.y = luma + (px.y - luma) * scale;
    px.z = luma + (px.z - luma) * scale;
  } else {
    const float avg = (px.x + px.y + px.z) / 3.0f;
    px.x += (avg - px.x) * (1.0f - scale);
    px.y += (avg - px.y) * (1.0f - scale);
    px.z += (avg - px.z) * (1.0f - scale);
  }
  return px;
}

// === Highlight / Shadow Local Tone ============================================

static inline float opencl_hs_ap1_luminance(float3 ap1) {
  return 0.27222872f * ap1.x + 0.67408177f * ap1.y + 0.05368952f * ap1.z;
}

static inline float opencl_hs_log_intensity_from_acescc(float4 px) {
  const float3 ap1 = opencl_acescc_to_ap1(px.xyz);
  return opencl_acescc_encode(fmax(opencl_hs_ap1_luminance(ap1), 1.0e-6f));
}

static inline float opencl_hs_range_weight(float center, float sample) {
  const float edge_delta = fmax(fabs(sample - center) - 0.018f, 0.0f);
  const float d = edge_delta / 0.075f;
  return exp2(-(d * d));
}

static inline float opencl_hs_lerp(float a, float b, float t) {
  return a + (b - a) * t;
}

static inline float opencl_hs_segment(float x, float x0, float y0, float x1, float y1) {
  const float t = clamp((x - x0) / fmax(x1 - x0, 1.0e-6f), 0.0f, 1.0f);
  return opencl_hs_lerp(y0, y1, t);
}

static inline float opencl_hs_relative_ev_from_log_intensity(float log_intensity) {
  return (log_intensity - 0.41358840f) * 17.52f;
}

static inline float opencl_hs_shadow_profile_ev(float relative_ev) {
  if (relative_ev <= -9.0f) return 0.02f;
  if (relative_ev <= -7.0f) return opencl_hs_segment(relative_ev, -9.0f, 0.02f, -7.0f, 0.35f);
  if (relative_ev <= -5.4f) return opencl_hs_segment(relative_ev, -7.0f, 0.35f, -5.4f, 0.82f);
  if (relative_ev <= -4.3f) return opencl_hs_segment(relative_ev, -5.4f, 0.82f, -4.3f, 0.98f);
  if (relative_ev <= -3.1f) return opencl_hs_segment(relative_ev, -4.3f, 0.98f, -3.1f, 0.72f);
  if (relative_ev <= -2.0f) return opencl_hs_segment(relative_ev, -3.1f, 0.72f, -2.0f, 0.42f);
  if (relative_ev <= -0.5f) return opencl_hs_segment(relative_ev, -2.0f, 0.42f, -0.5f, 0.08f);
  if (relative_ev <= 1.0f) return opencl_hs_segment(relative_ev, -0.5f, 0.08f, 1.0f, 0.0f);
  return 0.0f;
}

static inline float opencl_hs_highlight_profile_ev(float relative_ev) {
  if (relative_ev <= -1.0f) return 0.0f;
  if (relative_ev <= 0.0f) return opencl_hs_segment(relative_ev, -1.0f, 0.0f, 0.0f, 0.03f);
  if (relative_ev <= 1.2f) return opencl_hs_segment(relative_ev, 0.0f, 0.03f, 1.2f, 0.22f);
  if (relative_ev <= 2.8f) return opencl_hs_segment(relative_ev, 1.2f, 0.22f, 2.8f, 0.60f);
  if (relative_ev <= 4.5f) return opencl_hs_segment(relative_ev, 2.8f, 0.60f, 4.5f, 0.95f);
  if (relative_ev <= 6.5f) return opencl_hs_segment(relative_ev, 4.5f, 0.95f, 6.5f, 1.08f);
  if (relative_ev <= 8.0f) return opencl_hs_segment(relative_ev, 6.5f, 1.08f, 8.0f, 0.92f);
  return 0.92f;
}

static inline float opencl_hs_apply_reference_curve(float reference_l, float shadow_amount,
                                                    float highlight_amount) {
  const float relative_ev = opencl_hs_relative_ev_from_log_intensity(reference_l);
  const float shadow_lift = fmax(shadow_amount, 0.0f) * opencl_hs_shadow_profile_ev(relative_ev);
  const float shadow_darken =
      fmax(-shadow_amount, 0.0f) * 0.55f * opencl_hs_shadow_profile_ev(relative_ev);
  const float highlight_reduce =
      fmax(highlight_amount, 0.0f) * ALCEDO_OPENCL_HS_HIGHLIGHT_STRENGTH_SCALE *
      opencl_hs_highlight_profile_ev(relative_ev);
  const float highlight_boost =
      fmax(-highlight_amount, 0.0f) * 0.65f * opencl_hs_highlight_profile_ev(relative_ev);
  const float practical_dark =
      opencl_smoothstep_range(-5.85f, -3.95f, relative_ev) *
      (1.0f - opencl_smoothstep_range(-3.20f, -1.65f, relative_ev));
  const float fill_plateau =
      opencl_smoothstep_range(-5.55f, -3.30f, relative_ev) *
      (1.0f - 0.45f * opencl_smoothstep_range(-2.65f, -0.20f, relative_ev));
  const float deep_toe_fill =
      shadow_lift * (1.0f - opencl_smoothstep_range(-7.35f, -4.95f, relative_ev)) * 0.28f;
  const float shadow_fill_lift =
      shadow_lift * (0.62f * practical_dark + 0.14f * fill_plateau) + deep_toe_fill;
  const float lifted_relative_ev = relative_ev + 0.24f * (shadow_lift + 0.84f * shadow_fill_lift);
  const float combo_shadow_rollback =
      ((shadow_lift > 1.0e-6f && highlight_reduce > 1.0e-6f) ? 1.0f : 0.0f) *
      shadow_fill_lift * opencl_smoothstep_range(-2.00f, -0.60f, lifted_relative_ev) *
      (1.0f - opencl_smoothstep_range(0.10f, 1.30f, lifted_relative_ev)) * 1.08f;
  const float combo_low_mid_darken =
      fmin(shadow_lift + shadow_fill_lift, highlight_reduce) *
      opencl_smoothstep_range(-2.45f, -0.90f, lifted_relative_ev) *
      (1.0f - opencl_smoothstep_range(0.50f, 1.95f, lifted_relative_ev)) * 1.30f;
  const float delta_ev = shadow_lift + shadow_fill_lift - combo_shadow_rollback -
                         shadow_darken - highlight_reduce - combo_low_mid_darken +
                         highlight_boost;
  return reference_l + delta_ev * (1.0f / 17.52f);
}

static inline float opencl_hs_llf_detail_alpha(float reference_l, float shadow_amount,
                                               float highlight_amount) {
  (void)highlight_amount;
  const float relative_ev = opencl_hs_relative_ev_from_log_intensity(reference_l);
  const float deep_shadow =
      1.0f - opencl_smoothstep_range(-5.7f, -4.1f, relative_ev);
  const float mid_shadow =
      opencl_smoothstep_range(-5.0f, -3.6f, relative_ev) *
      (1.0f - opencl_smoothstep_range(-2.4f, -1.0f, relative_ev));
  const float lift_amount = fmax(shadow_amount, 0.0f);
  return 1.0f + 0.40f * lift_amount * deep_shadow - 0.14f * lift_amount * mid_shadow;
}

static inline float opencl_hs_llf_tone_beta(float reference_l, float shadow_amount,
                                            float highlight_amount) {
  const float eps = 0.035f;
  const float lo = opencl_hs_apply_reference_curve(reference_l - eps, shadow_amount,
                                                   highlight_amount);
  const float hi = opencl_hs_apply_reference_curve(reference_l + eps, shadow_amount,
                                                   highlight_amount);
  return clamp((hi - lo) / (2.0f * eps), 0.08f, 1.70f);
}

static inline float opencl_hs_llf_remap_delta(float delta_l, float sigma_r, float alpha,
                                              float beta) {
  const float abs_delta = fabs(delta_l);
  if (abs_delta <= 1.0e-6f) {
    return 0.0f;
  }

  const float sign = delta_l < 0.0f ? -1.0f : 1.0f;
  if (abs_delta <= sigma_r) {
    const float normalized = clamp(abs_delta / fmax(sigma_r, 1.0e-6f), 0.0f, 1.0f);
    return sign * sigma_r * pow(normalized, alpha);
  }
  return sign * (sigma_r + beta * (abs_delta - sigma_r));
}

static inline float4 opencl_hs_apply_adjusted_l_pixel(float4 px, float adjusted_l) {
  const float3 source_ap1 = opencl_acescc_to_ap1(px.xyz);
  const float source_intensity = fmax(opencl_hs_ap1_luminance(source_ap1), 1.0e-5f);
  const float adjusted_intensity = opencl_acescc_decode(adjusted_l);
  const float ratio = clamp(adjusted_intensity / source_intensity, 0.0f, 32.0f);
  const float3 ratio_ap1 = source_ap1 * ratio;
  const float3 neutral_ap1 = (float3)(adjusted_intensity, adjusted_intensity,
                                      adjusted_intensity);
  const float3 output_ap1 = opencl_fit_ap1_lower_gamut(ratio_ap1, neutral_ap1);
  return (float4)(opencl_ap1_to_acescc(output_ap1), px.w);
}

static inline float opencl_hs_shadow_upper_pivot(__global const OpenClFusedParams* params) {
  const float width = fmax(params->hs_shadow_log_width_, 0.35f);
  return params->hs_shadow_log_pivot_ + fmax(width * 0.40f, 0.24f);
}

static inline float opencl_hs_shadow_black_floor_weight(float mask_ref,
                                                        __global const OpenClFusedParams* params) {
  const float width = fmax(params->hs_shadow_log_width_, 0.35f);
  const float upper_pivot = opencl_hs_shadow_upper_pivot(params);
  const float black_start = upper_pivot - fmax(width * 7.20f, 4.45f);
  const float black_end = upper_pivot - fmax(width * 5.20f, 3.25f);
  const float toe = opencl_smoothstep_range(black_start, black_end, mask_ref);
  return 0.30f + 0.70f * toe;
}

static inline float opencl_hs_shadow_range_weight(float mask_ref) {
  const float middle_gray_log2 = -2.4739311883f;
  const float relative_ev = mask_ref - middle_gray_log2;
  return 1.0f - opencl_smoothstep_range(-5.50f, -0.50f, relative_ev);
}

static inline float opencl_hs_shadow_reference_lift_delta(float mask_ref) {
  const float middle_gray_log2 = -2.4739311883f;
  const float ev = mask_ref - middle_gray_log2;
#define OPENCL_HS_REF_SEG(x0, y0, x1, y1)                                                  \
  do {                                                                                      \
    if (ev < (x1)) {                                                                        \
      const float t = clamp((ev - (x0)) / ((x1) - (x0)), 0.0f, 1.0f);                       \
      return (y0) + ((y1) - (y0)) * t;                                                      \
    }                                                                                       \
  } while (0)
  if (ev <= -5.520f) return 3.170f;
  OPENCL_HS_REF_SEG(-5.520f, 3.170f, -3.935f, 3.700f);
  OPENCL_HS_REF_SEG(-3.935f, 3.700f, -2.713f, 2.974f);
  OPENCL_HS_REF_SEG(-2.713f, 2.974f, -1.713f, 2.100f);
  OPENCL_HS_REF_SEG(-1.713f, 2.100f, -0.997f, 1.564f);
  OPENCL_HS_REF_SEG(-0.997f, 1.564f, -0.433f, 1.179f);
  OPENCL_HS_REF_SEG(-0.433f, 1.179f, 0.065f, 0.807f);
  OPENCL_HS_REF_SEG(0.065f, 0.807f, 0.475f, 0.570f);
  OPENCL_HS_REF_SEG(0.475f, 0.570f, 0.850f, 0.483f);
  OPENCL_HS_REF_SEG(0.850f, 0.483f, 1.188f, 0.415f);
  OPENCL_HS_REF_SEG(1.188f, 0.415f, 1.485f, 0.355f);
  OPENCL_HS_REF_SEG(1.485f, 0.355f, 1.760f, 0.300f);
  OPENCL_HS_REF_SEG(1.760f, 0.300f, 2.015f, 0.255f);
  OPENCL_HS_REF_SEG(2.015f, 0.255f, 2.251f, 0.220f);
  OPENCL_HS_REF_SEG(2.251f, 0.220f, 2.474f, 0.0f);
#undef OPENCL_HS_REF_SEG
  return 0.0f;
}

static inline float opencl_hs_shadow_zone_weight(float mask_ref,
                                                 __global const OpenClFusedParams* params) {
  const float width = fmax(params->hs_shadow_log_width_, 0.35f);
  const float upper_pivot = opencl_hs_shadow_upper_pivot(params);
  const float fade_start = upper_pivot - fmax(width * 3.15f, 1.95f);
  const float tonal_weight = 1.0f - opencl_smoothstep_range(fade_start, upper_pivot, mask_ref);
  const float black_floor = opencl_hs_shadow_black_floor_weight(mask_ref, params);
  const float range_weight = opencl_hs_shadow_range_weight(mask_ref);
  return clamp(tonal_weight * black_floor * range_weight, 0.0f, 1.0f);
}

static inline float opencl_hs_shadow_base_distance(float mask_ref,
                                                   __global const OpenClFusedParams* params) {
  return clamp(opencl_hs_shadow_upper_pivot(params) - mask_ref, 0.0f, 3.65f);
}

static inline float opencl_hs_shadow_fill_plateau_weight(
    float mask_ref, __global const OpenClFusedParams* params) {
  const float lower_gate = opencl_smoothstep_range(params->hs_shadow_log_pivot_ - 4.05f,
                                                   params->hs_shadow_log_pivot_ - 1.75f,
                                                   mask_ref);
  const float upper_gate =
      1.0f - 0.45f * opencl_smoothstep_range(params->hs_shadow_log_pivot_ - 1.05f,
                                             params->hs_shadow_log_pivot_ + 3.15f,
                                             mask_ref);
  return lower_gate * upper_gate;
}

static inline float opencl_hs_shadow_practical_dark_weight(
    float mask_ref, __global const OpenClFusedParams* params) {
  const float lower_gate = opencl_smoothstep_range(params->hs_shadow_log_pivot_ - 4.60f,
                                                   params->hs_shadow_log_pivot_ - 2.25f,
                                                   mask_ref);
  const float upper_gate =
      1.0f - opencl_smoothstep_range(params->hs_shadow_log_pivot_ - 1.55f,
                                     params->hs_shadow_log_pivot_ + 0.20f, mask_ref);
  return lower_gate * upper_gate;
}

static inline float opencl_hs_highlight_zone_weight(
    float mask_ref, __global const OpenClFusedParams* params) {
  const float toe = 1.0e-3f;
  const float toe_pow = 0.1023292992f;
  const float inv_toe_range = 1.1135850f;
  const float gamma = 0.33f;
  const float t =
      opencl_smoothstep_range(params->hs_highlight_log_pivot_,
                              params->hs_highlight_log_pivot_ +
                                  params->hs_highlight_log_width_,
                              mask_ref);
  const float lifted = pow(t + toe, gamma);
  return clamp((lifted - toe_pow) * inv_toe_range, 0.0f, 1.0f);
}

static inline float opencl_hs_softplus_distance(float distance, float softness) {
  const float log2_value = 0.6931471805599453f;
  const float safe_softness = fmax(softness, 1.0e-4f);
  const float x = distance / safe_softness;
  if (x > 20.0f) {
    return distance - safe_softness * log2_value;
  }
  return safe_softness * (log(1.0f + exp(x)) - log2_value);
}

static inline float opencl_hs_softrelu_distance(float signed_distance, float softness,
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

static inline float opencl_hs_texture_detail_weight(float detail) {
  return 1.0f - opencl_smoothstep_range(0.28f, 0.88f, fabs(detail));
}

static inline float opencl_hs_active_mask_disagreement(
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

static inline float opencl_hs_tonal_reference_mix(float mask_disagreement) {
  return opencl_smoothstep_range(0.025f, 0.16f, mask_disagreement);
}

static inline float opencl_hs_local_detail_reference_guard(float detail,
                                                           float tonal_reference_mix) {
  const float edge_reference = opencl_smoothstep_range(0.42f, 0.95f, fabs(detail));
  return 1.0f - tonal_reference_mix * edge_reference;
}

static inline float opencl_hs_chromatic_fringe_guard(float source_chroma, float detail,
                                                     float tonal_reference_mix,
                                                     float active_highlight_mask,
                                                     float shadow_amount,
                                                     float highlight_amount) {
  if (highlight_amount <= 1.0e-6f && shadow_amount <= 1.0e-6f) {
    return 1.0f;
  }

  const float chroma_gate = opencl_smoothstep_range(0.012f, 0.060f, source_chroma);
  const float detail_gate = opencl_smoothstep_range(0.055f, 0.34f, fabs(detail)) *
                            (1.0f - opencl_smoothstep_range(0.82f, 1.42f, fabs(detail)));
  const float edge_gate = opencl_smoothstep_range(0.020f, 0.12f, tonal_reference_mix);
  const float highlight_gate = 0.35f + 0.65f * active_highlight_mask;
  const float strength = 0.82f * chroma_gate * detail_gate * edge_gate * highlight_gate;
  return 1.0f - clamp(strength, 0.0f, 0.82f);
}

static inline float opencl_hs_chromatic_local_mix_guard(
    float source_chroma, float detail, float local_delta, float source_delta,
    float active_highlight_mask, float shadow_amount, float highlight_amount) {
  if (highlight_amount <= 1.0e-6f && shadow_amount <= 1.0e-6f) {
    return 1.0f;
  }

  const float chroma_gate = opencl_smoothstep_range(0.012f, 0.055f, source_chroma);
  const float detail_gate = opencl_smoothstep_range(0.050f, 0.20f, fabs(detail)) *
                            (1.0f - opencl_smoothstep_range(0.85f, 1.45f, fabs(detail)));
  const float mismatch_gate =
      opencl_smoothstep_range(0.055f, 0.16f, fabs(local_delta - source_delta));
  const float highlight_gate = 0.35f + 0.65f * active_highlight_mask;
  const float both_active_gate =
      0.55f + 0.45f * clamp(fmin(fmax(shadow_amount, 0.0f),
                                  fmax(highlight_amount, 0.0f)),
                            0.0f, 1.0f);
  const float strength =
      0.78f * chroma_gate * detail_gate * mismatch_gate * highlight_gate * both_active_gate;
  return 1.0f - clamp(strength, 0.0f, 0.78f);
}

static inline float opencl_hs_llf_detail_mix(float detail) {
  return 1.0f - opencl_smoothstep_range(0.42f, 0.95f, fabs(detail));
}

static inline float opencl_hs_local_tone_mix(float detail, float local_delta,
                                             float source_delta) {
  const float mag = fabs(detail);
  const float edge_weight = opencl_smoothstep_range(0.62f, 1.55f, mag);
  const float delta_mismatch = opencl_smoothstep_range(0.16f, 0.52f,
                                                       fabs(local_delta - source_delta));
  const float guard =
      clamp(edge_weight * (0.82f + 0.18f * delta_mismatch) +
                0.18f * edge_weight * edge_weight,
            0.0f, 1.0f);
  return 1.0f - guard;
}

static inline float opencl_hs_shadow_detail_preserve_weight(float detail) {
  const float mag = fabs(detail);
  const float noise_gate = opencl_smoothstep_range(0.045f, 0.15f, mag);
  const float edge_guard = 1.0f - opencl_smoothstep_range(0.78f, 1.35f, mag);
  return noise_gate * edge_guard;
}

static inline float opencl_hs_shadow_llf_detail_gain(float detail, float shadow_amount,
                                                     float shadow_zone) {
  const float lift_amount = fmax(shadow_amount, 0.0f);
  if (lift_amount <= 1.0e-6f || shadow_zone <= 1.0e-6f) {
    return 1.0f;
  }

  const float mag = fabs(detail);
  const float noise_gate = opencl_smoothstep_range(0.035f, 0.11f, mag);
  const float fine_detail_gate = 1.0f - opencl_smoothstep_range(0.38f, 0.82f, mag);
  if (noise_gate <= 0.0f || fine_detail_gate <= 0.0f) {
    return 1.0f;
  }

  const float sigma_r_stops = 0.42f;
  const float x = clamp(mag / sigma_r_stops, 1.0e-4f, 1.0f);
  const float alpha = 1.0f - 0.44f * lift_amount;
  const float remapped_mag = sigma_r_stops * pow(x, alpha);
  const float remap_gain = remapped_mag / fmax(mag, 1.0e-4f);
  const float limited_gain = clamp(remap_gain - 1.0f, 0.0f, 0.50f);
  const float mix = lift_amount * shadow_zone * noise_gate * fine_detail_gate;
  return 1.0f + limited_gain * mix;
}

static inline float opencl_hs_shadow_detail_sign_weight(float detail) {
  if (detail >= 0.0f) {
    return 1.0f;
  }
  return 1.0f - 0.65f * opencl_smoothstep_range(0.18f, 0.72f, -detail);
}

static inline void opencl_hs_compute_masks(float mask_ref, float shadow_amount,
                                           float highlight_amount,
                                           __global const OpenClFusedParams* params,
                                           float* shadow_mask, float* highlight_mask) {
  const float raw_shadow = opencl_hs_shadow_zone_weight(mask_ref, params);
  const float raw_highlight = opencl_hs_highlight_zone_weight(mask_ref, params);
  const bool both_active = fabs(shadow_amount) > 1.0e-6f &&
                           fabs(highlight_amount) > 1.0e-6f;
  *shadow_mask = both_active ? raw_shadow * (1.0f - 0.72f * raw_highlight) : raw_shadow;
  *highlight_mask =
      both_active ? raw_highlight * (1.0f - 0.48f * raw_shadow) : raw_highlight;
}

static inline float opencl_hs_shadow_tonal_weight(float mask_ref, float shadow_mask,
                                                  __global const OpenClFusedParams* params) {
  (void)shadow_mask;
  return opencl_hs_shadow_zone_weight(mask_ref, params);
}

static inline float opencl_hs_shadow_base_delta_from_ref(
    float mask_ref, float shadow_amount, float highlight_amount,
    __global const OpenClFusedParams* params) {
  float shadow_mask = 0.0f;
  float highlight_mask = 0.0f;
  opencl_hs_compute_masks(mask_ref, shadow_amount, highlight_amount, params, &shadow_mask,
                          &highlight_mask);

  const float width = fmax(params->hs_shadow_log_width_, 0.35f);
  const float upper_pivot = opencl_hs_shadow_upper_pivot(params);
  const float lift_pivot = upper_pivot + fmax(width * 4.00f, 2.48f);
  const float distance_to_pivot = fmax(lift_pivot - mask_ref, 0.0f);
  const float soft_distance =
      opencl_hs_softplus_distance(distance_to_pivot, fmax(width * 2.18f, 1.35f));
  const float black_floor = opencl_hs_shadow_black_floor_weight(mask_ref, params);
  const float black_guard = 0.82f + 0.18f * black_floor;
  const float highlight_overlap = clamp(highlight_mask, 0.0f, 1.0f);
  const float highlight_active = (fabs(highlight_amount) > 1.0e-6f) ? 1.0f : 0.0f;
  const float overlap_guard = 1.0f - 0.28f * highlight_active * highlight_overlap;
  const float lift_amount = fmax(shadow_amount, 0.0f);
  const float darken_amount = fmax(-shadow_amount, 0.0f);
  const float lift_delta =
      lift_amount * opencl_hs_shadow_reference_lift_delta(mask_ref) * overlap_guard;
  const float darken_delta =
      darken_amount * 0.34f * soft_distance * (0.85f + 0.15f * black_guard);
  return lift_delta - opencl_hs_shadow_range_weight(mask_ref) * darken_delta;
}

static inline float opencl_hs_highlight_base_delta_from_ref(
    float mask_ref, float shadow_amount, float highlight_amount,
    __global const OpenClFusedParams* params) {
  const float middle_gray_log2 = -2.4739311883f;
  const float width = fmax(params->hs_highlight_log_width_, 0.35f);
  const float soft_distance = opencl_hs_softrelu_distance(
      mask_ref - params->hs_highlight_log_pivot_, fmin(fmax(width * 0.12f, 0.36f), 0.55f),
      fmin(fmax(width * 0.24f, 0.62f), 1.00f));
  const float reduce_amount = fmax(highlight_amount, 0.0f);
  const float boost_amount = fmax(-highlight_amount, 0.0f);
  float reduce_delta = 0.0f;
  if (reduce_amount > 1.0e-6f) {
    float shadow_lift_delta = 0.0f;
    if (shadow_amount > 1.0e-6f) {
      shadow_lift_delta =
          fmax(opencl_hs_shadow_base_delta_from_ref(mask_ref, shadow_amount, highlight_amount,
                                                    params),
               0.0f);
    }
    const float lifted_relative_ev = mask_ref + 0.18f * shadow_lift_delta - middle_gray_log2;
    const float highlight_shelf =
        0.60f * opencl_smoothstep_range(-2.80f, 0.50f, lifted_relative_ev);
    const float highlight_peak =
        0.50f * opencl_smoothstep_range(-1.35f, 1.60f, lifted_relative_ev) *
        (1.0f - opencl_smoothstep_range(2.25f, 4.90f, lifted_relative_ev));
    const float extreme_high_tail =
        0.23f * opencl_smoothstep_range(3.00f, 5.15f, lifted_relative_ev);
    reduce_delta = (highlight_shelf + highlight_peak + extreme_high_tail) *
                   ALCEDO_OPENCL_HS_HIGHLIGHT_STRENGTH_SCALE;
  }
  const float boost_delta = 1.24f * (1.0f - exp(-soft_distance / 1.45f));
  return boost_amount * boost_delta - reduce_amount * reduce_delta;
}

static inline float opencl_hs_base_delta_from_ref(float mask_ref, float shadow_amount,
                                                  float highlight_amount,
                                                  __global const OpenClFusedParams* params) {
  return opencl_hs_shadow_base_delta_from_ref(mask_ref, shadow_amount, highlight_amount, params) +
         opencl_hs_highlight_base_delta_from_ref(mask_ref, shadow_amount, highlight_amount, params);
}

static inline float opencl_hs_base_curve_slope(float mask_ref, float shadow_amount,
                                               float highlight_amount,
                                               __global const OpenClFusedParams* params) {
  const float eps_stops = 0.08f;
  const float delta_lo =
      opencl_hs_base_delta_from_ref(mask_ref - eps_stops, shadow_amount, highlight_amount, params);
  const float delta_hi =
      opencl_hs_base_delta_from_ref(mask_ref + eps_stops, shadow_amount, highlight_amount, params);
  return 1.0f + (delta_hi - delta_lo) / (2.0f * eps_stops);
}

static inline float opencl_hs_shadow_curve_slope(float mask_ref, float shadow_amount,
                                                 float highlight_amount,
                                                 __global const OpenClFusedParams* params) {
  const float eps_stops = 0.08f;
  const float delta_lo = opencl_hs_shadow_base_delta_from_ref(
      mask_ref - eps_stops, shadow_amount, highlight_amount, params);
  const float delta_hi = opencl_hs_shadow_base_delta_from_ref(
      mask_ref + eps_stops, shadow_amount, highlight_amount, params);
  return 1.0f + (delta_hi - delta_lo) / (2.0f * eps_stops);
}

static inline float opencl_hs_highlight_curve_slope(float mask_ref, float shadow_amount,
                                                    float highlight_amount,
                                                    __global const OpenClFusedParams* params) {
  const float eps_stops = 0.08f;
  const float delta_lo = opencl_hs_highlight_base_delta_from_ref(
      mask_ref - eps_stops, shadow_amount, highlight_amount, params);
  const float delta_hi = opencl_hs_highlight_base_delta_from_ref(
      mask_ref + eps_stops, shadow_amount, highlight_amount, params);
  return 1.0f + (delta_hi - delta_lo) / (2.0f * eps_stops);
}

static inline float opencl_hs_shadow_detail_weight(float mask_ref, float shadow_mask,
                                                   __global const OpenClFusedParams* params) {
  const float tonal_weight = opencl_hs_shadow_tonal_weight(mask_ref, shadow_mask, params);
  const float signal_gate = opencl_smoothstep_range(params->hs_shadow_log_pivot_ - 2.60f,
                                                    params->hs_shadow_log_pivot_ - 1.20f,
                                                    mask_ref);
  const float upper_guard =
      1.0f - opencl_smoothstep_range(params->hs_shadow_log_pivot_ + 1.05f,
                                     params->hs_shadow_log_pivot_ + 2.10f, mask_ref);
  const float practical_shadow = signal_gate * upper_guard;
  return fmax(tonal_weight * signal_gate, 0.72f * practical_shadow);
}

static inline float opencl_hs_shadow_fill_light_weight(
    float mask_ref, __global const OpenClFusedParams* params) {
  const float signal_gate = opencl_smoothstep_range(params->hs_shadow_log_pivot_ - 2.45f,
                                                    params->hs_shadow_log_pivot_ - 1.05f,
                                                    mask_ref);
  const float upper_guard =
      1.0f - opencl_smoothstep_range(params->hs_shadow_log_pivot_ + 1.55f,
                                     params->hs_shadow_log_pivot_ + 2.50f, mask_ref);
  return signal_gate * upper_guard;
}

static inline float opencl_hs_highlight_detail_weight(float mask_ref, float highlight_mask,
                                                      float detail,
                                                      __global const OpenClFusedParams* params) {
  const float width = fmax(params->hs_highlight_log_width_, 0.35f);
  const float tonal_weight = clamp(highlight_mask, 0.0f, 1.0f);
  const float noise_gate = opencl_smoothstep_range(0.035f, 0.12f, fabs(detail));
  const float edge_guard = 1.0f - opencl_smoothstep_range(0.78f, 1.45f, fabs(detail));
  const float clipped_guard =
      1.0f - opencl_smoothstep_range(params->hs_highlight_log_pivot_ + width * 1.15f,
                                     params->hs_highlight_log_pivot_ + width * 2.35f, mask_ref);
  return tonal_weight * noise_gate * edge_guard * clipped_guard;
}

static inline float3 opencl_hs_dampen_shadow_chroma(
    float3 source_ap1, float3 output_ap1, float3 source_lab, float source_chroma,
    float log_delta, float shadow_amount, float source_shadow_mask, float source_log_y,
    __global const OpenClFusedParams* params) {
  (void)source_ap1;
  (void)source_lab;
  const float lift_amount = fmax(shadow_amount, 0.0f);
  if (lift_amount <= 1.0e-6f || log_delta <= 1.0e-5f ||
      source_shadow_mask <= 1.0e-5f || source_chroma <= 1.0e-5f) {
    return output_ap1;
  }

  const float3 output_lab = opencl_ap1_to_oklab(output_ap1);
  const float output_chroma = hypot(output_lab.y, output_lab.z);
  if (output_chroma <= 1.0e-5f || output_lab.x <= 1.0e-5f) {
    return output_ap1;
  }

  const float dirty_chroma = opencl_smoothstep_range(0.045f, 0.18f, source_chroma);
  const float lift_gate = opencl_smoothstep_range(0.30f, 1.20f, log_delta);
  const float dark_gate =
      1.0f - opencl_smoothstep_range(params->hs_shadow_log_pivot_ + 0.75f,
                                     params->hs_shadow_log_pivot_ + 2.40f, source_log_y);
  const float strength = clamp(0.30f * lift_amount * source_shadow_mask * dirty_chroma *
                                   lift_gate * dark_gate,
                               0.0f, 0.34f);
  if (strength <= 1.0e-6f) {
    return output_ap1;
  }

  const float3 adjusted_lab =
      (float3)(output_lab.x, output_lab.y * (1.0f - strength),
               output_lab.z * (1.0f - strength));
  const float3 neutral_lab = (float3)(output_lab.x, 0.0f, 0.0f);
  const float3 neutral_ap1 = opencl_oklab_to_ap1(neutral_lab);
  return opencl_fit_ap1_lower_gamut(opencl_oklab_to_ap1(adjusted_lab), neutral_ap1);
}

static inline float4 opencl_hs_apply_local_tone_pixel(
    float4 px, float base, __global const OpenClFusedParams* params) {
  const float shadow_amount =
      (params->shadows_enabled_ != 0u) ? clamp(params->shadows_offset_, -1.0f, 1.0f) : 0.0f;
  const float highlight_amount = (params->highlights_enabled_ != 0u)
                                     ? clamp(-params->highlights_offset_, -1.0f, 1.0f)
                                     : 0.0f;
  if (fabs(shadow_amount) <= 1.0e-6f && fabs(highlight_amount) <= 1.0e-6f) {
    return px;
  }

  const float source_l = opencl_hs_log_intensity_from_acescc(px);
  const float sigma_r = 0.07545252f;
  const float target_l = opencl_hs_apply_reference_curve(base, shadow_amount, highlight_amount);
  const float alpha = opencl_hs_llf_detail_alpha(base, shadow_amount, highlight_amount);
  const float beta = opencl_hs_llf_tone_beta(base, shadow_amount, highlight_amount);
  const float adjusted_l =
      target_l + opencl_hs_llf_remap_delta(source_l - base, sigma_r, alpha, beta);
  return opencl_hs_apply_adjusted_l_pixel(px, adjusted_l);
}

static inline float opencl_hs_read_base_bilinear(__global const float* base_log, int width,
                                                 int height, int base_pitch_elems, float x,
                                                 float y) {
  const float clamped_x = clamp(x, 0.0f, (float)(width - 1));
  const float clamped_y = clamp(y, 0.0f, (float)(height - 1));
  const int x0 = clamp((int)floor(clamped_x), 0, width - 1);
  const int y0 = clamp((int)floor(clamped_y), 0, height - 1);
  const int x1 = min(x0 + 1, width - 1);
  const int y1 = min(y0 + 1, height - 1);
  const float tx = clamped_x - (float)x0;
  const float ty = clamped_y - (float)y0;

  const float v00 = base_log[y0 * base_pitch_elems + x0];
  const float v10 = base_log[y0 * base_pitch_elems + x1];
  const float v01 = base_log[y1 * base_pitch_elems + x0];
  const float v11 = base_log[y1 * base_pitch_elems + x1];
  const float vx0 = v00 + (v10 - v00) * tx;
  const float vx1 = v01 + (v11 - v01) * tx;
  return vx0 + (vx1 - vx0) * ty;
}

// === Color Wheel (Lift / Gamma / Gain) ========================================

static inline float4 opencl_color_wheel_op(float4 px, __global const OpenClFusedParams* params) {
  if (params->color_wheel_enabled_ == 0u) return px;

  const float offset_r = params->lift_color_offset_[0] + params->lift_luminance_offset_;
  const float offset_g = params->lift_color_offset_[1] + params->lift_luminance_offset_;
  const float offset_b = params->lift_color_offset_[2] + params->lift_luminance_offset_;

  const float slope_r = fmax(params->gain_color_offset_[0] + params->gain_luminance_offset_, 1e-6f);
  const float slope_g = fmax(params->gain_color_offset_[1] + params->gain_luminance_offset_, 1e-6f);
  const float slope_b = fmax(params->gain_color_offset_[2] + params->gain_luminance_offset_, 1e-6f);

  const float power_r = fmax(params->gamma_color_offset_[0] + params->gamma_luminance_offset_, 1e-6f);
  const float power_g = fmax(params->gamma_color_offset_[1] + params->gamma_luminance_offset_, 1e-6f);
  const float power_b = fmax(params->gamma_color_offset_[2] + params->gamma_luminance_offset_, 1e-6f);

  const float base_r = fmax(px.x * slope_r + offset_r, 0.0f);
  const float base_g = fmax(px.y * slope_g + offset_g, 0.0f);
  const float base_b = fmax(px.z * slope_b + offset_b, 0.0f);

  px.x = pow(base_r, power_r);
  px.y = pow(base_g, power_g);
  px.z = pow(base_b, power_b);
  return px;
}

// === HLS ======================================================================

static inline float4 opencl_hls_op(float4 px, __global const OpenClFusedParams* params) {
  if (params->hls_enabled_ == 0u && params->saturation_enabled_ == 0u) return px;

  const float kEps = 1e-6f;
  const float kPi  = 3.14159265358979323846f;
  const float3 source_ap1 = opencl_acescc_to_ap1(px.xyz);
  const float3 source_lab = opencl_ap1_to_oklab(source_ap1);
  const float source_chroma = hypot(source_lab.y, source_lab.z);
  if (source_chroma <= kEps) return px;

  const float source_hue = opencl_wrap_hue(atan2(source_lab.z, source_lab.y) * (180.0f / kPi));

  float3 curve = (float3)(0.0f, 0.0f, 0.0f);
  if (params->hls_enabled_ != 0u) {
    int profile_count = params->hls_profile_count_;
    if (profile_count < 1) profile_count = 1;
    if (profile_count > ALCEDO_OPENCL_HLS_PROFILE_COUNT) profile_count = ALCEDO_OPENCL_HLS_PROFILE_COUNT;

    float accum_h = 0.0f;
    float accum_l = 0.0f;
    float accum_c = 0.0f;
    float accum_weight = 0.0f;
    int nearest = 0;
    float nearest_dist = opencl_hue_distance(source_hue, opencl_wrap_hue(params->hls_profile_hues_[0]));

    for (int i = 0; i < ALCEDO_OPENCL_HLS_PROFILE_COUNT; ++i) {
      if (i >= profile_count) continue;

      const float width     = fmax(params->hls_profile_hue_ranges_[i], 1.0f);
      const float target_h  = opencl_wrap_hue(params->hls_profile_hues_[i]);
      const float hue_dist  = opencl_hue_distance(source_hue, target_h);
      if (hue_dist < nearest_dist) {
        nearest_dist = hue_dist;
        nearest = i;
      }

      const float t = hue_dist / width;
      const float weight = exp2(-(t * t));
      accum_h += params->hls_profile_adjustments_[i][0] * weight;
      accum_l += params->hls_profile_adjustments_[i][1] * weight;
      accum_c += params->hls_profile_adjustments_[i][2] * weight;
      accum_weight += weight;
    }

    curve = (float3)(params->hls_profile_adjustments_[nearest][0],
                     params->hls_profile_adjustments_[nearest][1],
                     params->hls_profile_adjustments_[nearest][2]);
    if (accum_weight > kEps) {
      curve = (float3)(accum_h, accum_l, accum_c) / accum_weight;
    }
  }
  const float saturation_scale =
      (params->saturation_enabled_ != 0u) ? fmax(params->saturation_offset_, 0.0f) : 1.0f;
  if (fabs(curve.x) <= kEps && fabs(curve.y) <= kEps && fabs(curve.z) <= kEps &&
      fabs(saturation_scale - 1.0f) <= kEps) return px;

  const bool has_curve =
      fabs(curve.x) > kEps || fabs(curve.y) > kEps || fabs(curve.z) > kEps;
  float protection = 0.0f;
  if (has_curve) {
    const float chroma_confidence = opencl_smoothstep_range(0.005f, 0.030f, source_chroma);
    const float shadow_confidence = opencl_smoothstep_range(0.005f, 0.050f, source_lab.x);
    const float highlight_confidence = 1.0f - opencl_smoothstep_range(1.35f, 2.25f, source_lab.x);
    protection = clamp(chroma_confidence * shadow_confidence * highlight_confidence, 0.0f, 1.0f);
  }
  if (protection <= kEps && fabs(saturation_scale - 1.0f) <= kEps) return px;

  const float curve_gain = 2.25f;
  const float adjusted_hue_rad =
      opencl_wrap_hue(source_hue + curve.x * curve_gain * protection) * (kPi / 180.0f);
  const float adjusted_lightness =
      (fabs(curve.y) > kEps && protection > kEps)
          ? opencl_soft_floor(source_lab.x + curve.y * curve_gain * 0.5f * protection, 0.0f,
                              0.02f)
          : source_lab.x;
  const float chroma_strength = (curve.z >= 0.0f) ? 4.5f : 3.25f;
  const float adjusted_chroma =
      source_chroma * saturation_scale * exp2(curve.z * curve_gain * chroma_strength * protection);

  const float3 adjusted_lab =
      (float3)(adjusted_lightness, adjusted_chroma * cos(adjusted_hue_rad),
               adjusted_chroma * sin(adjusted_hue_rad));
  const float3 neutral_lab = (float3)(adjusted_lightness, 0.0f, 0.0f);
  const float3 output_ap1 =
      opencl_fit_ap1_lower_gamut(opencl_oklab_to_ap1(adjusted_lab),
                                 opencl_oklab_to_ap1(neutral_lab));
  px.xyz = opencl_ap1_to_acescc(output_ap1);

  return px;
}

#endif  // ALCEDO_OPENCL_EDIT_PIPELINE_COLOR_CL
