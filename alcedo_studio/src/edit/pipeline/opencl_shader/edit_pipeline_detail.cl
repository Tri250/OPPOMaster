//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifndef ALCEDO_OPENCL_EDIT_PIPELINE_DETAIL_CL
#define ALCEDO_OPENCL_EDIT_PIPELINE_DETAIL_CL

#define ALCEDO_OPENCL_NEIGHBOR_MAX_TAP_COUNT 64
#define ALCEDO_OPENCL_NEIGHBOR_OP_SHARPEN    1u
#define ALCEDO_OPENCL_NEIGHBOR_OP_CLARITY    2u

typedef struct {
  uint  kind_;
  uint  radius_;
  uint  tap_count_;
  float amount_;
  float threshold_;
  float weights_[ALCEDO_OPENCL_NEIGHBOR_MAX_TAP_COUNT];
} OpenClNeighborStageParams;

// === Detail helpers =============================================================

static inline float opencl_detail_luminance(float4 c) {
  // Match the CUDA implementation's COLOR_BGR2GRAY coefficients.
  return c.x * 0.114f + c.y * 0.587f + c.z * 0.299f;
}

static inline float4 opencl_detail_read_clamped(__global const float4* src, int x, int y,
                                                int width, int height) {
  const int cx = clamp(x, 0, width - 1);
  const int cy = clamp(y, 0, height - 1);
  return src[(size_t)cy * (size_t)width + (size_t)cx];
}

static inline float opencl_detail_read_log_clamped(__global const float* src, int x, int y,
                                                   int width, int height) {
  const int cx = clamp(x, 0, width - 1);
  const int cy = clamp(y, 0, height - 1);
  return src[(size_t)cy * (size_t)width + (size_t)cx];
}

static inline float opencl_detail_smoothstep(float edge0, float edge1, float x) {
  const float t = clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
  return t * t * (3.0f - 2.0f * t);
}

// === Separable blur helpers =====================================================

static inline float4 opencl_neighbor_blur_horizontal(__global const float4* src, int x, int y,
                                                     int width, int height,
                                                     __global const OpenClNeighborStageParams* params) {
  if (params->tap_count_ == 0u) {
    return opencl_detail_read_clamped(src, x, y, width, height);
  }

  float4 blur = opencl_detail_read_clamped(src, x, y, width, height) * params->weights_[0];
  for (uint tap = 1u; tap < params->tap_count_; ++tap) {
    const float  w = params->weights_[tap];
    const float4 a = opencl_detail_read_clamped(src, x + (int)tap, y, width, height);
    const float4 b = opencl_detail_read_clamped(src, x - (int)tap, y, width, height);
    blur += (a + b) * w;
  }
  return blur;
}

static inline float4 opencl_neighbor_blur_vertical(__global const float4* src, int x, int y,
                                                   int width, int height,
                                                   __global const OpenClNeighborStageParams* params) {
  if (params->tap_count_ == 0u) {
    return opencl_detail_read_clamped(src, x, y, width, height);
  }

  float4 blur = opencl_detail_read_clamped(src, x, y, width, height) * params->weights_[0];
  for (uint tap = 1u; tap < params->tap_count_; ++tap) {
    const float  w = params->weights_[tap];
    const float4 a = opencl_detail_read_clamped(src, x, y + (int)tap, width, height);
    const float4 b = opencl_detail_read_clamped(src, x, y - (int)tap, width, height);
    blur += (a + b) * w;
  }
  return blur;
}

// === Apply operators ============================================================

static inline float4 opencl_apply_sharpen(float4 px, float4 blur,
                                          __global const OpenClNeighborStageParams* params) {
  if (params->amount_ == 0.0f || params->tap_count_ == 0u) {
    return px;
  }

  float4 high = px - blur;

  if (params->threshold_ > 0.0f) {
    const float hp_gray = opencl_detail_luminance(high);
    const float mask    = (fabs(hp_gray) > params->threshold_) ? 1.0f : 0.0f;
    high *= mask;
  }

  return px + high * params->amount_;
}

static inline float4 opencl_apply_clarity(float4 px, float4 blur,
                                          __global const OpenClNeighborStageParams* params) {
  if (params->amount_ == 0.0f || params->tap_count_ == 0u) {
    return px;
  }

  float4 diff = (float4)(px.x - blur.x, px.y - blur.y, px.z - blur.z, 0.0f);

  const float diff_lum = opencl_detail_luminance(diff);
  const float edge_mag = fabs(diff_lum);
  const float kEdgeThreshold = 0.18f;
  const float protect = 1.0f - opencl_detail_smoothstep(0.0f, kEdgeThreshold, edge_mag);

  const float lum   = opencl_detail_luminance(px);
  const float t_lum = (lum - 0.5f) * 2.0f;
  const float mask  = fmax(1.0f - t_lum * t_lum, 0.0f);
  const float strength = params->amount_ * protect * mask;

  return (float4)(fma(diff.x, strength, px.x), fma(diff.y, strength, px.y),
                  fma(diff.z, strength, px.z), px.w);
}

// === Kernels ====================================================================

__kernel void edit_pipeline_neighbor_blur_h_rgba32f(__global const float4* src,
                                                    __global float4* dst,
                                                    __global const OpenClNeighborStageParams* params,
                                                    int width,
                                                    int height) {
  const int x = get_global_id(0);
  const int y = get_global_id(1);
  if (x >= width || y >= height) {
    return;
  }

  const int idx = y * width + x;
  dst[idx] = opencl_neighbor_blur_horizontal(src, x, y, width, height, params);
}

__kernel void edit_pipeline_neighbor_apply_v_rgba32f(__global const float4* src,
                                                     __global const float4* blur_h,
                                                     __global float4* dst,
                                                     __global const OpenClNeighborStageParams* params,
                                                     int width,
                                                     int height) {
  const int x = get_global_id(0);
  const int y = get_global_id(1);
  if (x >= width || y >= height) {
    return;
  }

  const int   idx  = y * width + x;
  const float4 px   = src[idx];
  const float4 blur = opencl_neighbor_blur_vertical(blur_h, x, y, width, height, params);

  switch (params->kind_) {
    case ALCEDO_OPENCL_NEIGHBOR_OP_SHARPEN:
      dst[idx] = opencl_apply_sharpen(px, blur, params);
      break;
    case ALCEDO_OPENCL_NEIGHBOR_OP_CLARITY:
      dst[idx] = opencl_apply_clarity(px, blur, params);
      break;
    default:
      dst[idx] = px;
      break;
  }
}

__kernel void edit_pipeline_hs_build_log_base_h_rgba32f(
    __global const float4* src,
    __global float* dst,
    __global const OpenClFusedParams* params,
    int width,
    int height) {
  const int x = get_global_id(0);
  const int y = get_global_id(1);
  if (x >= width || y >= height) {
    return;
  }

  const int idx = y * width + x;
  const int tap_count = params->hs_base_gaussian_tap_count_;
  if (tap_count <= 0) {
    dst[idx] = opencl_hs_log2_luminance_from_acescc(src[idx]);
    return;
  }

  const float center = opencl_hs_log2_luminance_from_acescc(src[idx]);
  float base = center * params->hs_base_gaussian_weights_[0];
  float weight_sum = params->hs_base_gaussian_weights_[0];
  for (int tap = 1; tap < tap_count; ++tap) {
    const int ax = min(x + tap, width - 1);
    const int bx = max(x - tap, 0);
    const float wa = opencl_hs_log2_luminance_from_acescc(src[y * width + ax]);
    const float wb = opencl_hs_log2_luminance_from_acescc(src[y * width + bx]);
    const float spatial = params->hs_base_gaussian_weights_[tap];
    const float aw = spatial * opencl_hs_range_weight(center, wa);
    const float bw = spatial * opencl_hs_range_weight(center, wb);
    base += wa * aw + wb * bw;
    weight_sum += aw + bw;
  }
  dst[idx] = base / fmax(weight_sum, 1.0e-6f);
}

__kernel void edit_pipeline_hs_build_log_base_v_rgba32f(
    __global const float4* guidance,
    __global const float* src,
    __global float* dst,
    __global const OpenClFusedParams* params,
    int width,
    int height) {
  const int x = get_global_id(0);
  const int y = get_global_id(1);
  if (x >= width || y >= height) {
    return;
  }

  const int idx = y * width + x;
  const int tap_count = params->hs_base_gaussian_tap_count_;
  if (tap_count <= 0) {
    dst[idx] = src[idx];
    return;
  }

  const float center = src[idx];
  const float center_guidance = opencl_hs_log2_luminance_from_acescc(guidance[idx]);
  float base = center * params->hs_base_gaussian_weights_[0];
  float weight_sum = params->hs_base_gaussian_weights_[0];
  for (int tap = 1; tap < tap_count; ++tap) {
    const int ay = min(y + tap, height - 1);
    const int by = max(y - tap, 0);
    const float a = opencl_detail_read_log_clamped(src, x, y + tap, width, height);
    const float b = opencl_detail_read_log_clamped(src, x, y - tap, width, height);
    const float ag = opencl_hs_log2_luminance_from_acescc(guidance[ay * width + x]);
    const float bg = opencl_hs_log2_luminance_from_acescc(guidance[by * width + x]);
    const float spatial = params->hs_base_gaussian_weights_[tap];
    const float aw = spatial * opencl_hs_range_weight(center_guidance, ag);
    const float bw = spatial * opencl_hs_range_weight(center_guidance, bg);
    base += a * aw + b * bw;
    weight_sum += aw + bw;
  }
  dst[idx] = base / fmax(weight_sum, 1.0e-6f);
}

__kernel void edit_pipeline_hs_apply_local_tone_rgba32f(
    __global const float4* src,
    __global const float* base_log,
    __global float4* dst,
    __global const OpenClFusedParams* params,
    int width,
    int height,
    int base_width,
    int base_height,
    int base_pitch_elems,
    int use_reference_base) {
  const int x = get_global_id(0);
  const int y = get_global_id(1);
  if (x >= width || y >= height) {
    return;
  }

  const int idx = y * width + x;
  float base = base_log[idx];
  if (use_reference_base != 0) {
    const float reference_width = (float)max(params->render_roi_reference_width_, width);
    const float reference_height = (float)max(params->render_roi_reference_height_, height);
    const float roi_origin_x = (params->render_roi_enabled_ != 0u) ? (float)params->render_roi_x_
                                                                   : 0.0f;
    const float roi_origin_y = (params->render_roi_enabled_ != 0u) ? (float)params->render_roi_y_
                                                                   : 0.0f;
    const float roi_width = (params->render_roi_enabled_ != 0u)
                                ? fmax(params->render_roi_scale_x_ * reference_width, 1.0f)
                                : reference_width;
    const float roi_height = (params->render_roi_enabled_ != 0u)
                                 ? fmax(params->render_roi_scale_y_ * reference_height, 1.0f)
                                 : reference_height;
    const float reference_x =
        roi_origin_x + (((float)x + 0.5f) * roi_width / fmax((float)width, 1.0f)) - 0.5f;
    const float reference_y =
        roi_origin_y + (((float)y + 0.5f) * roi_height / fmax((float)height, 1.0f)) - 0.5f;
    const float base_x =
        ((reference_x + 0.5f) * (float)base_width / fmax(reference_width, 1.0f)) - 0.5f;
    const float base_y =
        ((reference_y + 0.5f) * (float)base_height / fmax(reference_height, 1.0f)) - 0.5f;
    base = opencl_hs_read_base_bilinear(base_log, base_width, base_height, base_pitch_elems,
                                        base_x, base_y);
  }

  dst[idx] = opencl_hs_apply_local_tone_pixel(src[idx], base, params);
}

#endif  // ALCEDO_OPENCL_EDIT_PIPELINE_DETAIL_CL
