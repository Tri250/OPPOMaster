//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifndef ALCEDO_OPENCL_EDIT_PIPELINE_DETAIL_CL
#define ALCEDO_OPENCL_EDIT_PIPELINE_DETAIL_CL

#define ALCEDO_OPENCL_NEIGHBOR_MAX_TAP_COUNT 64
#define ALCEDO_OPENCL_NEIGHBOR_OP_SHARPEN    1u
#define ALCEDO_OPENCL_NEIGHBOR_OP_CLARITY    2u
#define ALCEDO_OPENCL_NEIGHBOR_OP_HALATION   3u
#define ALCEDO_OPENCL_NEIGHBOR_OP_FILM_GRAIN 4u

typedef struct {
  uint  kind_;
  uint  radius_;
  uint  tap_count_;
  float amount_;
  float threshold_;
  float weights_[ALCEDO_OPENCL_NEIGHBOR_MAX_TAP_COUNT];
  uint  enabled_;
  int   eotf_;
  uint  seed_lo_;
  uint  seed_hi_;
  float sigma_x_;
  float sigma_y_;
  float redshift_[3];
  float reserved_;
  uint  roi_enabled_;
  int   roi_x_;
  int   roi_y_;
  float roi_scale_x_;
  float roi_scale_y_;
  int   roi_reference_width_;
  int   roi_reference_height_;
  uint  reserved_tail_;
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

// === Film grain helpers ========================================================

static inline int opencl_film_grain_reference_coord(int coord,
                                                    int length,
                                                    int roi_origin,
                                                    float roi_scale,
                                                    int reference_length,
                                                    uint roi_enabled) {
  const float safe_length = fmax((float)length, 1.0f);
  const int full_extent = max((reference_length > 0) ? reference_length : length, 1);
  const float full_length = (float)full_extent;
  const float origin = (roi_enabled != 0u) ? (float)roi_origin : 0.0f;
  const float span = (roi_enabled != 0u) ? fmax(roi_scale * full_length, 1.0f) : full_length;
  const float mapped = origin + (((float)coord + 0.5f) * span / safe_length) - 0.5f;
  return clamp((int)floor(mapped + 0.5f), 0, full_extent - 1);
}

static inline float opencl_film_grain_channel(float4 value, int channel) {
  if (channel == 0) return value.x;
  if (channel == 1) return value.y;
  return value.z;
}

static inline float opencl_film_grain_lerp(float a, float b, float t) {
  return a + (b - a) * t;
}

static inline float opencl_film_grain_eval_density_sigma(float density,
                                                         const float density_lut[11],
                                                         const float sigma_lut[11]) {
  if (density <= density_lut[0]) {
    return sigma_lut[0];
  }
  for (int i = 0; i < 10; ++i) {
    const float lo = density_lut[i];
    const float hi = density_lut[i + 1];
    if (density <= hi) {
      const float t = (density - lo) / fmax(hi - lo, 1.0e-6f);
      return opencl_film_grain_lerp(sigma_lut[i], sigma_lut[i + 1], t);
    }
  }
  return sigma_lut[10];
}

static inline float opencl_film_grain_layer_density(float signal, int channel) {
  const float u = clamp(signal, 0.0f, 1.0f);
  if (channel == 0) return opencl_film_grain_lerp(0.22f, 2.52f, u);
  if (channel == 1) return opencl_film_grain_lerp(0.59f, 2.69f, u);
  return opencl_film_grain_lerp(1.00f, 3.00f, u);
}

static inline float opencl_film_grain_datasheet_sigma_d(float density, int channel) {
  const float red_density[11] = {0.22f, 0.22f, 0.25f, 0.42f, 0.78f, 1.19f,
                                 1.58f, 1.94f, 2.26f, 2.45f, 2.52f};
  const float red_sigma[11] = {0.00594f, 0.00565f, 0.00524f, 0.01085f, 0.00844f, 0.00531f,
                               0.00486f, 0.00486f, 0.00445f, 0.00440f, 0.00474f};
  const float green_density[11] = {0.59f, 0.61f, 0.66f, 0.94f, 1.36f, 1.76f,
                                   2.18f, 2.49f, 2.61f, 2.67f, 2.69f};
  const float green_sigma[11] = {0.00517f, 0.00524f, 0.00625f, 0.01085f, 0.00823f, 0.00617f,
                                 0.00625f, 0.00691f, 0.00602f, 0.00524f, 0.00445f};
  const float blue_density[11] = {1.00f, 1.03f, 1.10f, 1.32f, 1.51f, 1.78f,
                                  2.05f, 2.38f, 2.68f, 2.91f, 3.00f};
  const float blue_sigma[11] = {0.01185f, 0.01261f, 0.01485f, 0.01581f, 0.01200f, 0.01099f,
                                0.01127f, 0.01058f, 0.00844f, 0.00641f, 0.00418f};

  if (channel == 0) return opencl_film_grain_eval_density_sigma(density, red_density, red_sigma);
  if (channel == 1) {
    return opencl_film_grain_eval_density_sigma(density, green_density, green_sigma);
  }
  return opencl_film_grain_eval_density_sigma(density, blue_density, blue_sigma);
}

static inline float opencl_film_grain_datasheet_granularity_scale(float signal, int channel) {
  const float density = opencl_film_grain_layer_density(signal, channel);
  const float sigma = opencl_film_grain_datasheet_sigma_d(density, channel);
  return clamp(sigma / 0.0075f, 0.55f, 2.15f);
}

static inline float opencl_film_grain_sample(float probability,
                                             int ref_x,
                                             int ref_y,
                                             int channel,
                                             __global const OpenClNeighborStageParams* params) {
  const ulong seed = (((ulong)params->seed_hi_) << 32u) | (ulong)params->seed_lo_;
  const ulong stream = opencl_prng_pixel_stream_2d(ref_x, ref_y, (uint)channel);
  const float draw = opencl_prng_uniform_float01(seed, stream, 0xd1b54a32d192ed03UL);
  return draw < clamp(probability, 0.0f, 1.0f) ? 1.0f : 0.0f;
}

static inline float opencl_film_grain_sample_at(__global const float4* src,
                                                int x,
                                                int y,
                                                int channel,
                                                int width,
                                                int height,
                                                __global const OpenClNeighborStageParams* params) {
  const int clamped_x = clamp(x, 0, width - 1);
  const int clamped_y = clamp(y, 0, height - 1);
  const float4 signal = src[(size_t)clamped_y * (size_t)width + (size_t)clamped_x];
  const int ref_x = (params->roi_enabled_ != 0u)
                        ? clamped_x
                        : opencl_film_grain_reference_coord(
                              clamped_x, width, params->roi_x_, params->roi_scale_x_,
                              params->roi_reference_width_, params->roi_enabled_);
  const int ref_y = (params->roi_enabled_ != 0u)
                        ? clamped_y
                        : opencl_film_grain_reference_coord(
                              clamped_y, height, params->roi_y_, params->roi_scale_y_,
                              params->roi_reference_height_, params->roi_enabled_);
  return opencl_film_grain_sample(opencl_film_grain_channel(signal, channel), ref_x, ref_y,
                                  channel, params);
}

static inline float opencl_film_grain_gaussian7(float c0,
                                                float n1,
                                                float p1,
                                                float n2,
                                                float p2,
                                                float n3,
                                                float p3) {
  return c0 * 0.49867642f + (n1 + p1) * 0.22831073f + (n2 + p2) * 0.02192964f +
         (n3 + p3) * 0.00042142f;
}

static inline float4 opencl_film_grain_blur_horizontal(
    __global const float4* src, int x, int y, int width, int height,
    __global const OpenClNeighborStageParams* params) {
  float blurred[3] = {0.0f, 0.0f, 0.0f};
  for (int channel = 0; channel < 3; ++channel) {
    blurred[channel] = opencl_film_grain_gaussian7(
        opencl_film_grain_sample_at(src, x, y, channel, width, height, params),
        opencl_film_grain_sample_at(src, x - 1, y, channel, width, height, params),
        opencl_film_grain_sample_at(src, x + 1, y, channel, width, height, params),
        opencl_film_grain_sample_at(src, x - 2, y, channel, width, height, params),
        opencl_film_grain_sample_at(src, x + 2, y, channel, width, height, params),
        opencl_film_grain_sample_at(src, x - 3, y, channel, width, height, params),
        opencl_film_grain_sample_at(src, x + 3, y, channel, width, height, params));
  }

  return (float4)(blurred[0], blurred[1], blurred[2],
                  opencl_detail_read_clamped(src, x, y, width, height).w);
}

static inline float4 opencl_film_grain_blur_vertical(__global const float4* src,
                                                     int x,
                                                     int y,
                                                     int width,
                                                     int height) {
  const float4 c0 = opencl_detail_read_clamped(src, x, y, width, height);
  const float4 n1 = opencl_detail_read_clamped(src, x, y - 1, width, height);
  const float4 p1 = opencl_detail_read_clamped(src, x, y + 1, width, height);
  const float4 n2 = opencl_detail_read_clamped(src, x, y - 2, width, height);
  const float4 p2 = opencl_detail_read_clamped(src, x, y + 2, width, height);
  const float4 n3 = opencl_detail_read_clamped(src, x, y - 3, width, height);
  const float4 p3 = opencl_detail_read_clamped(src, x, y + 3, width, height);

  return (float4)(opencl_film_grain_gaussian7(c0.x, n1.x, p1.x, n2.x, p2.x, n3.x, p3.x),
                  opencl_film_grain_gaussian7(c0.y, n1.y, p1.y, n2.y, p2.y, n3.y, p3.y),
                  opencl_film_grain_gaussian7(c0.z, n1.z, p1.z, n2.z, p2.z, n3.z, p3.z),
                  c0.w);
}

// === Halation helpers ==========================================================

static inline float opencl_detail_moncurve_fwd(float x, float gamma, float offs) {
  const float fs = ((gamma - 1.0f) / offs) *
                   pow(offs * gamma / ((gamma - 1.0f) * (1.0f + offs)), gamma);
  const float xb = offs / (gamma - 1.0f);
  return (x >= xb) ? pow((x + offs) / (1.0f + offs), gamma) : x * fs;
}

static inline float opencl_detail_moncurve_inv(float y, float gamma, float offs) {
  const float yb = pow(offs * gamma / ((gamma - 1.0f) * (1.0f + offs)), gamma);
  const float rs = pow((gamma - 1.0f) / offs, gamma - 1.0f) *
                   pow((1.0f + offs) / gamma, gamma);
  return (y >= yb) ? (1.0f + offs) * pow(y, 1.0f / gamma) - offs : y * rs;
}

static inline float opencl_detail_st2084_to_y(float n) {
  const float pq_m1 = 0.1593017578125f;
  const float pq_m2 = 78.84375f;
  const float pq_c1 = 0.8359375f;
  const float pq_c2 = 18.8515625f;
  const float pq_c3 = 18.6875f;
  const float pq_c = 10000.0f;
  const float np = pow(n, 1.0f / pq_m2);
  float l = fmax(np - pq_c1, 0.0f);
  l = l / (pq_c2 - pq_c3 * np);
  return pow(l, 1.0f / pq_m1) * pq_c;
}

static inline float opencl_detail_y_to_st2084(float c) {
  const float pq_m1 = 0.1593017578125f;
  const float pq_m2 = 78.84375f;
  const float pq_c1 = 0.8359375f;
  const float pq_c2 = 18.8515625f;
  const float pq_c3 = 18.6875f;
  const float pq_c = 10000.0f;
  const float l = c / pq_c;
  const float lm = pow(l, pq_m1);
  const float n = (pq_c1 + pq_c2 * lm) / (1.0f + pq_c3 * lm);
  return pow(n, pq_m2);
}

static inline float3 opencl_detail_hlg_to_display_linear_1000nits(float3 hlg_signal) {
  const float a = 0.17883277f;
  const float b = 0.28466892f;
  const float c = 0.55991073f;
  float3 rgb;
  rgb.x = (hlg_signal.x <= 0.5f) ? (hlg_signal.x * hlg_signal.x) / 3.0f
                                 : (exp((hlg_signal.x - c) / a) + b) / 12.0f;
  rgb.y = (hlg_signal.y <= 0.5f) ? (hlg_signal.y * hlg_signal.y) / 3.0f
                                 : (exp((hlg_signal.y - c) / a) + b) / 12.0f;
  rgb.z = (hlg_signal.z <= 0.5f) ? (hlg_signal.z * hlg_signal.z) / 3.0f
                                 : (exp((hlg_signal.z - c) / a) + b) / 12.0f;

  const float ys = 0.2627f * rgb.x + 0.6780f * rgb.y + 0.0593f * rgb.z;
  if (ys > 0.0f) {
    rgb *= pow(ys, 1.2f - 1.0f);
  }
  return rgb;
}

static inline float3 opencl_detail_hlg_from_display_linear_1000nits(float3 display_linear) {
  float y_d = 0.2627f * display_linear.x + 0.6780f * display_linear.y +
              0.0593f * display_linear.z;
  float3 rgb = display_linear;
  if (y_d > 0.0f) {
    rgb *= pow(y_d, (1.0f - 1.2f) / 1.2f);
  }

  const float a = 0.17883277f;
  const float b = 0.28466892f;
  const float c = 0.55991073f;
  rgb.x = (rgb.x <= (1.0f / 12.0f)) ? sqrt(3.0f * rgb.x) : a * log(12.0f * rgb.x - b) + c;
  rgb.y = (rgb.y <= (1.0f / 12.0f)) ? sqrt(3.0f * rgb.y) : a * log(12.0f * rgb.y - b) + c;
  rgb.z = (rgb.z <= (1.0f / 12.0f)) ? sqrt(3.0f * rgb.z) : a * log(12.0f * rgb.z - b) + c;
  return rgb;
}

static inline float3 opencl_detail_eotf(float3 rgb_cv, int eotf_type) {
  if (eotf_type == 0) return rgb_cv;
  if (eotf_type == 1) {
    return (float3)(opencl_detail_st2084_to_y(rgb_cv.x), opencl_detail_st2084_to_y(rgb_cv.y),
                    opencl_detail_st2084_to_y(rgb_cv.z)) *
           (1.0f / 100.0f);
  }
  if (eotf_type == 2) return opencl_detail_hlg_to_display_linear_1000nits(rgb_cv);
  if (eotf_type == 4) return pow(rgb_cv, (float3)(2.6f));
  if (eotf_type == 3) return pow(rgb_cv, (float3)(2.6f));
  if (eotf_type == 5) return pow(rgb_cv, (float3)(2.2f));
  if (eotf_type == 6) return pow(rgb_cv, (float3)(1.8f));
  return (float3)(opencl_detail_moncurve_fwd(rgb_cv.x, 2.4f, 0.055f),
                  opencl_detail_moncurve_fwd(rgb_cv.y, 2.4f, 0.055f),
                  opencl_detail_moncurve_fwd(rgb_cv.z, 2.4f, 0.055f));
}

static inline float3 opencl_detail_eotf_inv(float3 rgb_linear_in, int eotf_type) {
  float3 rgb = fmax(rgb_linear_in, (float3)(0.0f));
  if (eotf_type == 0) return rgb;
  if (eotf_type == 1) {
    return (float3)(opencl_detail_y_to_st2084(rgb.x), opencl_detail_y_to_st2084(rgb.y),
                    opencl_detail_y_to_st2084(rgb.z));
  }
  if (eotf_type == 2) return opencl_detail_hlg_from_display_linear_1000nits(rgb);
  if (eotf_type == 4) {
    return (float3)(opencl_detail_moncurve_inv(rgb.x, 2.4f, 0.055f),
                    opencl_detail_moncurve_inv(rgb.y, 2.4f, 0.055f),
                    opencl_detail_moncurve_inv(rgb.z, 2.4f, 0.055f));
  }
  if (eotf_type == 3) return pow(rgb, (float3)(1.0f / 2.6f));
  if (eotf_type == 5) return pow(rgb, (float3)(1.0f / 2.2f));
  if (eotf_type == 6) return pow(rgb, (float3)(1.0f / 1.8f));
  return (float3)(opencl_detail_moncurve_inv(rgb.x, 2.4f, 0.055f),
                  opencl_detail_moncurve_inv(rgb.y, 2.4f, 0.055f),
                  opencl_detail_moncurve_inv(rgb.z, 2.4f, 0.055f));
}

static inline float3 opencl_halation_decode_display_linear(
    float4 pixel, __global const OpenClNeighborStageParams* params) {
  return opencl_detail_eotf(
      (float3)(fmax(pixel.x, 0.0f), fmax(pixel.y, 0.0f), fmax(pixel.z, 0.0f)),
      params->eotf_);
}

static inline float opencl_halation_blur_radius(float sigma) {
  if (!(sigma > 0.0f)) return 0.0f;
  return (float)clamp((int)ceil(sigma * 3.0f), 1, ALCEDO_OPENCL_NEIGHBOR_MAX_TAP_COUNT - 1);
}

static inline float opencl_halation_weight(int tap, float sigma) {
  return (tap == 0) ? 1.0f : exp(-((float)tap) / fmax(sigma, 1.0e-6f));
}

static inline float opencl_halation_weight_norm(int radius, float sigma) {
  float sum = 1.0f;
  for (int tap = 1; tap <= radius; ++tap) {
    sum += 2.0f * opencl_halation_weight(tap, sigma);
  }
  return 1.0f / fmax(sum, 1.0e-6f);
}

static inline float4 opencl_halation_blur_horizontal(
    __global const float4* src, int x, int y, int width, int height,
    __global const OpenClNeighborStageParams* params) {
  const float sigma = params->sigma_x_;
  const int radius = (int)opencl_halation_blur_radius(sigma);
  const float norm = opencl_halation_weight_norm(radius, sigma);

  const float4 center = opencl_detail_read_clamped(src, x, y, width, height);
  const float3 center_linear = opencl_halation_decode_display_linear(center, params);
  float4 blur = (float4)(center_linear.x * norm, center_linear.y * norm,
                         center_linear.z * norm, center.w);

  for (int tap = 1; tap <= radius; ++tap) {
    const float weight = opencl_halation_weight(tap, sigma) * norm;
    const float4 left = opencl_detail_read_clamped(src, x - tap, y, width, height);
    const float4 right = opencl_detail_read_clamped(src, x + tap, y, width, height);
    const float3 left_linear = opencl_halation_decode_display_linear(left, params);
    const float3 right_linear = opencl_halation_decode_display_linear(right, params);
    blur.x += (left_linear.x + right_linear.x) * weight;
    blur.y += (left_linear.y + right_linear.y) * weight;
    blur.z += (left_linear.z + right_linear.z) * weight;
  }

  return blur;
}

static inline float4 opencl_halation_blur_vertical(
    __global const float4* src, int x, int y, int width, int height,
    __global const OpenClNeighborStageParams* params) {
  const float sigma = params->sigma_y_;
  const int radius = (int)opencl_halation_blur_radius(sigma);
  const float norm = opencl_halation_weight_norm(radius, sigma);
  float4 blur = opencl_detail_read_clamped(src, x, y, width, height);
  blur.x *= norm;
  blur.y *= norm;
  blur.z *= norm;

  for (int tap = 1; tap <= radius; ++tap) {
    const float weight = opencl_halation_weight(tap, sigma) * norm;
    const float4 top = opencl_detail_read_clamped(src, x, y - tap, width, height);
    const float4 bottom = opencl_detail_read_clamped(src, x, y + tap, width, height);
    blur.x += (top.x + bottom.x) * weight;
    blur.y += (top.y + bottom.y) * weight;
    blur.z += (top.z + bottom.z) * weight;
  }
  return blur;
}

// === Separable blur helpers =====================================================

static inline float4 opencl_neighbor_blur_horizontal(__global const float4* src, int x, int y,
                                                     int width, int height,
                                                     __global const OpenClNeighborStageParams* params) {
  if (params->enabled_ == 0u || params->amount_ == 0.0f) {
    return opencl_detail_read_clamped(src, x, y, width, height);
  }
  if (params->kind_ == ALCEDO_OPENCL_NEIGHBOR_OP_HALATION) {
    return opencl_halation_blur_horizontal(src, x, y, width, height, params);
  }
  if (params->kind_ == ALCEDO_OPENCL_NEIGHBOR_OP_FILM_GRAIN) {
    return opencl_film_grain_blur_horizontal(src, x, y, width, height, params);
  }
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
  if (params->kind_ == ALCEDO_OPENCL_NEIGHBOR_OP_HALATION) {
    return opencl_halation_blur_vertical(src, x, y, width, height, params);
  }
  if (params->kind_ == ALCEDO_OPENCL_NEIGHBOR_OP_FILM_GRAIN) {
    return opencl_film_grain_blur_vertical(src, x, y, width, height);
  }
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

static inline float4 opencl_apply_halation(float4 px, float4 blur,
                                           __global const OpenClNeighborStageParams* params) {
  if (params->enabled_ == 0u || !(params->amount_ > 0.0f)) {
    return px;
  }

  const float3 original_linear = opencl_halation_decode_display_linear(px, params);
  const float3 spill_linear = (float3)(fmax(blur.x - original_linear.x, 0.0f),
                                       fmax(blur.y - original_linear.y, 0.0f),
                                       fmax(blur.z - original_linear.z, 0.0f));
  const float3 result_linear =
      original_linear + spill_linear *
                            (float3)(params->amount_ * params->redshift_[0],
                                     params->amount_ * params->redshift_[1],
                                     params->amount_ * params->redshift_[2]);
  const float3 encoded = opencl_detail_eotf_inv(result_linear, params->eotf_);
  return (float4)(encoded.x, encoded.y, encoded.z, px.w);
}

static inline float4 opencl_apply_film_grain(float4 px, float4 blur,
                                             __global const OpenClNeighborStageParams* params) {
  if (params->enabled_ == 0u || !(params->amount_ > 0.0f)) {
    return px;
  }

  const float red_strength =
      params->amount_ * opencl_film_grain_datasheet_granularity_scale(px.x, 0);
  const float green_strength =
      params->amount_ * opencl_film_grain_datasheet_granularity_scale(px.y, 1);
  const float blue_strength =
      params->amount_ * opencl_film_grain_datasheet_granularity_scale(px.z, 2);
  return (float4)(px.x + red_strength * (blur.x - px.x),
                  px.y + green_strength * (blur.y - px.y),
                  px.z + blue_strength * (blur.z - px.z), px.w);
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
    case ALCEDO_OPENCL_NEIGHBOR_OP_HALATION:
      dst[idx] = opencl_apply_halation(px, blur, params);
      break;
    case ALCEDO_OPENCL_NEIGHBOR_OP_FILM_GRAIN:
      dst[idx] = opencl_apply_film_grain(px, blur, params);
      break;
    default:
      dst[idx] = px;
      break;
  }
}

static inline float opencl_hs_read_plane_clamped(__global const float* src, int x, int y,
                                                 int width, int height) {
  const int cx = clamp(x, 0, width - 1);
  const int cy = clamp(y, 0, height - 1);
  return src[(size_t)cy * (size_t)width + (size_t)cx];
}

static inline float opencl_hs_pyr_weight_1d(int tap) {
  if (tap == -2 || tap == 2) {
    return 1.0f / 16.0f;
  }
  if (tap == -1 || tap == 1) {
    return 4.0f / 16.0f;
  }
  return 6.0f / 16.0f;
}

static inline float opencl_hs_expand_from_coarse(__global const float* coarse,
                                                 int coarse_width,
                                                 int coarse_height,
                                                 int x,
                                                 int y) {
  float sum = 0.0f;
  for (int ky = -2; ky <= 2; ++ky) {
    const int sample_y = y - ky;
    if ((sample_y & 1) != 0) continue;
    const int cy = clamp(sample_y / 2, 0, coarse_height - 1);
    const float wy = opencl_hs_pyr_weight_1d(ky);
    for (int kx = -2; kx <= 2; ++kx) {
      const int sample_x = x - kx;
      if ((sample_x & 1) != 0) continue;
      const int cx = clamp(sample_x / 2, 0, coarse_width - 1);
      const float wx = opencl_hs_pyr_weight_1d(kx);
      sum += 4.0f * wx * wy * coarse[(size_t)cy * (size_t)coarse_width + (size_t)cx];
    }
  }
  return sum;
}

static inline float4 opencl_hs_read_rgba_bilinear(__global const float4* src,
                                                  int width,
                                                  int height,
                                                  float x,
                                                  float y) {
  const float clamped_x = clamp(x, 0.0f, (float)(width - 1));
  const float clamped_y = clamp(y, 0.0f, (float)(height - 1));
  const int x0 = clamp((int)floor(clamped_x), 0, width - 1);
  const int y0 = clamp((int)floor(clamped_y), 0, height - 1);
  const int x1 = min(x0 + 1, width - 1);
  const int y1 = min(y0 + 1, height - 1);
  const float tx = clamped_x - (float)x0;
  const float ty = clamped_y - (float)y0;
  const float4 v00 = src[(size_t)y0 * (size_t)width + (size_t)x0];
  const float4 v10 = src[(size_t)y0 * (size_t)width + (size_t)x1];
  const float4 v01 = src[(size_t)y1 * (size_t)width + (size_t)x0];
  const float4 v11 = src[(size_t)y1 * (size_t)width + (size_t)x1];
  const float4 vx0 = v00 + (v10 - v00) * tx;
  const float4 vx1 = v01 + (v11 - v01) * tx;
  return vx0 + (vx1 - vx0) * ty;
}

static inline float opencl_hs_read_plane_bilinear(__global const float* plane,
                                                  int width,
                                                  int height,
                                                  float x,
                                                  float y) {
  const float clamped_x = clamp(x, 0.0f, (float)(width - 1));
  const float clamped_y = clamp(y, 0.0f, (float)(height - 1));
  const int x0 = clamp((int)floor(clamped_x), 0, width - 1);
  const int y0 = clamp((int)floor(clamped_y), 0, height - 1);
  const int x1 = min(x0 + 1, width - 1);
  const int y1 = min(y0 + 1, height - 1);
  const float tx = clamped_x - (float)x0;
  const float ty = clamped_y - (float)y0;
  const float v00 = plane[(size_t)y0 * (size_t)width + (size_t)x0];
  const float v10 = plane[(size_t)y0 * (size_t)width + (size_t)x1];
  const float v01 = plane[(size_t)y1 * (size_t)width + (size_t)x0];
  const float v11 = plane[(size_t)y1 * (size_t)width + (size_t)x1];
  const float vx0 = v00 + (v10 - v00) * tx;
  const float vx1 = v01 + (v11 - v01) * tx;
  return vx0 + (vx1 - vx0) * ty;
}

__kernel void edit_pipeline_hs_extract_log_intensity_rgba32f(
    __global const float4* src,
    __global float* dst,
    int width,
    int height) {
  const int x = get_global_id(0);
  const int y = get_global_id(1);
  if (x >= width || y >= height) {
    return;
  }

  const int idx = y * width + x;
  dst[idx] = opencl_hs_log_intensity_from_acescc(src[idx]);
}

__kernel void edit_pipeline_hs_extract_log_intensity_resampled_rgba32f(
    __global const float4* src,
    __global float* dst,
    int src_width,
    int src_height,
    int dst_width,
    int dst_height) {
  const int x = get_global_id(0);
  const int y = get_global_id(1);
  if (x >= dst_width || y >= dst_height) {
    return;
  }

  const float src_x =
      (((float)x + 0.5f) * (float)src_width / fmax((float)dst_width, 1.0f)) - 0.5f;
  const float src_y =
      (((float)y + 0.5f) * (float)src_height / fmax((float)dst_height, 1.0f)) - 0.5f;
  dst[(size_t)y * (size_t)dst_width + (size_t)x] =
      opencl_hs_log_intensity_from_acescc(
          opencl_hs_read_rgba_bilinear(src, src_width, src_height, src_x, src_y));
}

__kernel void edit_pipeline_hs_build_remapped_sample(
    __global const float* source_l,
    __global float* remapped_l,
    int width,
    int height,
    float gamma,
    float target,
    float beta,
    float alpha,
    float sigma_r) {
  const int x = get_global_id(0);
  const int y = get_global_id(1);
  if (x >= width || y >= height) {
    return;
  }
  const size_t offset = (size_t)y * (size_t)width + (size_t)x;
  const float source_value = source_l[offset];
  remapped_l[offset] = target + opencl_hs_llf_remap_delta(source_value - gamma, sigma_r,
                                                          alpha, beta);
}

__kernel void edit_pipeline_hs_pyr_down(
    __global const float* src,
    __global float* dst,
    int src_width,
    int src_height,
    int dst_width,
    int dst_height) {
  const int x = get_global_id(0);
  const int y = get_global_id(1);
  if (x >= dst_width || y >= dst_height) {
    return;
  }

  const int center_x = x * 2;
  const int center_y = y * 2;
  float sum = 0.0f;
  for (int ky = -2; ky <= 2; ++ky) {
    const float wy = opencl_hs_pyr_weight_1d(ky);
    for (int kx = -2; kx <= 2; ++kx) {
      const float wx = opencl_hs_pyr_weight_1d(kx);
      sum += wx * wy *
             opencl_hs_read_plane_clamped(src, center_x + kx, center_y + ky,
                                          src_width, src_height);
    }
  }
  dst[(size_t)y * (size_t)dst_width + (size_t)x] = sum;
}

__kernel void edit_pipeline_hs_select_interpolated_level(
    __global const float* source_level,
    __global const float* sample_lo_level,
    __global const float* sample_lo_coarse,
    __global const float* sample_hi_level,
    __global const float* sample_hi_coarse,
    __global float* output_level,
    int width,
    int height,
    int coarse_width,
    int coarse_height,
    float gamma_lo,
    float gamma_hi,
    int first_pair,
    int last_pair,
    int top_level) {
  const int x = get_global_id(0);
  const int y = get_global_id(1);
  if (x >= width || y >= height) {
    return;
  }

  const size_t offset = (size_t)y * (size_t)width + (size_t)x;
  const float g = source_level[offset];
  const bool in_interval =
      ((first_pair != 0) && g <= gamma_hi) || ((last_pair != 0) && g >= gamma_lo) ||
      (g >= gamma_lo && g < gamma_hi);
  if (!in_interval) {
    return;
  }

  const float t = clamp((g - gamma_lo) / fmax(gamma_hi - gamma_lo, 1.0e-6f), 0.0f, 1.0f);
  if (top_level != 0) {
    output_level[offset] = sample_lo_level[offset] + (sample_hi_level[offset] -
                                                      sample_lo_level[offset]) * t;
    return;
  }

  const float lap_lo = sample_lo_level[offset] -
                       opencl_hs_expand_from_coarse(sample_lo_coarse, coarse_width,
                                                    coarse_height, x, y);
  const float lap_hi = sample_hi_level[offset] -
                       opencl_hs_expand_from_coarse(sample_hi_coarse, coarse_width,
                                                    coarse_height, x, y);
  output_level[offset] = lap_lo + (lap_hi - lap_lo) * t;
}

__kernel void edit_pipeline_hs_collapse_level(
    __global const float* lap_level,
    __global const float* coarse_level,
    __global float* dst_level,
    int width,
    int height,
    int coarse_width,
    int coarse_height) {
  const int x = get_global_id(0);
  const int y = get_global_id(1);
  if (x >= width || y >= height) {
    return;
  }

  const size_t offset = (size_t)y * (size_t)width + (size_t)x;
  dst_level[offset] = lap_level[offset] +
                      opencl_hs_expand_from_coarse(coarse_level, coarse_width,
                                                   coarse_height, x, y);
}

__kernel void edit_pipeline_hs_apply_adjusted_l_rgba32f(
    __global const float4* src,
    __global const float* adjusted_l,
    __global float4* dst,
    int width,
    int height) {
  const int x = get_global_id(0);
  const int y = get_global_id(1);
  if (x >= width || y >= height) {
    return;
  }

  const size_t offset = (size_t)y * (size_t)width + (size_t)x;
  dst[offset] = opencl_hs_apply_adjusted_l_pixel(src[offset], adjusted_l[offset]);
}

__kernel void edit_pipeline_hs_apply_adjusted_l_from_frame_rgba32f(
    __global const float4* src,
    __global const float* reference_l,
    __global const float* adjusted_l,
    __global float4* dst,
    int width,
    int height,
    int adjusted_width,
    int adjusted_height) {
  const int x = get_global_id(0);
  const int y = get_global_id(1);
  if (x >= width || y >= height) {
    return;
  }

  const float adjusted_x =
      (((float)x + 0.5f) * (float)adjusted_width / fmax((float)width, 1.0f)) - 0.5f;
  const float adjusted_y =
      (((float)y + 0.5f) * (float)adjusted_height / fmax((float)height, 1.0f)) - 0.5f;
  const size_t offset = (size_t)y * (size_t)width + (size_t)x;
  const float sampled_reference =
      opencl_hs_read_plane_bilinear(reference_l, adjusted_width, adjusted_height,
                                    adjusted_x, adjusted_y);
  const float sampled_adjusted =
      opencl_hs_read_plane_bilinear(adjusted_l, adjusted_width, adjusted_height,
                                    adjusted_x, adjusted_y);
  dst[offset] =
      opencl_hs_apply_adjusted_l_delta_pixel(src[offset], sampled_reference, sampled_adjusted);
}

__kernel void edit_pipeline_hs_apply_adjusted_l_from_reference_rgba32f(
    __global const float4* src,
    __global const float* reference_l,
    __global const float* adjusted_l,
    __global float4* dst,
    __global const OpenClFusedParams* params,
    int width,
    int height,
    int adjusted_width,
    int adjusted_height) {
  const int x = get_global_id(0);
  const int y = get_global_id(1);
  if (x >= width || y >= height) {
    return;
  }

  const float reference_width = (float)max(params->render_roi_reference_width_, width);
  const float reference_height = (float)max(params->render_roi_reference_height_, height);
  const float roi_origin_x =
      (params->render_roi_enabled_ != 0u) ? (float)params->render_roi_x_ : 0.0f;
  const float roi_origin_y =
      (params->render_roi_enabled_ != 0u) ? (float)params->render_roi_y_ : 0.0f;
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
  const float adjusted_x =
      ((reference_x + 0.5f) * (float)adjusted_width / fmax(reference_width, 1.0f)) - 0.5f;
  const float adjusted_y =
      ((reference_y + 0.5f) * (float)adjusted_height / fmax(reference_height, 1.0f)) - 0.5f;

  const size_t offset = (size_t)y * (size_t)width + (size_t)x;
  const float sampled_reference =
      opencl_hs_read_plane_bilinear(reference_l, adjusted_width, adjusted_height,
                                    adjusted_x, adjusted_y);
  const float sampled_adjusted =
      opencl_hs_read_plane_bilinear(adjusted_l, adjusted_width, adjusted_height,
                                    adjusted_x, adjusted_y);
  dst[offset] =
      opencl_hs_apply_adjusted_l_delta_pixel(src[offset], sampled_reference, sampled_adjusted);
}

#endif  // ALCEDO_OPENCL_EDIT_PIPELINE_DETAIL_CL
