//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include "prng.metal"

constant float    kMetalFilmGrainRedDensity[11]   = {0.22f, 0.22f, 0.25f, 0.42f, 0.78f, 1.19f,
                                                     1.58f, 1.94f, 2.26f, 2.45f, 2.52f};
constant float    kMetalFilmGrainRedSigma[11]     = {0.00594f, 0.00565f, 0.00524f, 0.01085f,
                                                     0.00844f, 0.00531f, 0.00486f, 0.00486f,
                                                     0.00445f, 0.00440f, 0.00474f};
constant float    kMetalFilmGrainGreenDensity[11] = {0.59f, 0.61f, 0.66f, 0.94f, 1.36f, 1.76f,
                                                     2.18f, 2.49f, 2.61f, 2.67f, 2.69f};
constant float    kMetalFilmGrainGreenSigma[11]   = {0.00517f, 0.00524f, 0.00625f, 0.01085f,
                                                     0.00823f, 0.00617f, 0.00625f, 0.00691f,
                                                     0.00602f, 0.00524f, 0.00445f};
constant float    kMetalFilmGrainBlueDensity[11]  = {1.00f, 1.03f, 1.10f, 1.32f, 1.51f, 1.78f,
                                                     2.05f, 2.38f, 2.68f, 2.91f, 3.00f};
constant float    kMetalFilmGrainBlueSigma[11]    = {0.01185f, 0.01261f, 0.01485f, 0.01581f,
                                                     0.01200f, 0.01099f, 0.01127f, 0.01058f,
                                                     0.00844f, 0.00641f, 0.00418f};

static inline int metal_film_grain_reference_coord(int coord, int length, int roi_origin,
                                                   float roi_scale, int reference_length,
                                                   uint roi_enabled) {
  const float safe_length = fmax(static_cast<float>(length), 1.0f);
  const int   full_extent = max((reference_length > 0) ? reference_length : length, 1);
  const float full_length = static_cast<float>(full_extent);
  const float origin      = (roi_enabled != 0u) ? static_cast<float>(roi_origin) : 0.0f;
  const float span        = (roi_enabled != 0u) ? fmax(roi_scale * full_length, 1.0f) : full_length;
  const float mapped = origin + (((static_cast<float>(coord) + 0.5f) * span / safe_length) - 0.5f);
  return clamp(static_cast<int>(floor(mapped + 0.5f)), 0, full_extent - 1);
}

static inline float metal_film_grain_channel(float4 value, int channel) {
  if (channel == 0) {
    return value.x;
  }
  if (channel == 1) {
    return value.y;
  }
  return value.z;
}

static inline float metal_film_grain_lerp(float a, float b, float t) { return a + (b - a) * t; }

static inline float metal_film_grain_eval_density_sigma(float density, constant float* density_lut,
                                                        constant float* sigma_lut) {
  if (density <= density_lut[0]) {
    return sigma_lut[0];
  }
  for (int i = 0; i < 10; ++i) {
    const float lo = density_lut[i];
    const float hi = density_lut[i + 1];
    if (density <= hi) {
      const float t = (density - lo) / fmax(hi - lo, 1.0e-6f);
      return metal_film_grain_lerp(sigma_lut[i], sigma_lut[i + 1], t);
    }
  }
  return sigma_lut[10];
}

static inline float metal_film_grain_layer_density(float signal, int channel) {
  const float u = clamp(signal, 0.0f, 1.0f);
  if (channel == 0) {
    return metal_film_grain_lerp(0.22f, 2.52f, u);
  }
  if (channel == 1) {
    return metal_film_grain_lerp(0.59f, 2.69f, u);
  }
  return metal_film_grain_lerp(1.00f, 3.00f, u);
}

static inline float metal_film_grain_datasheet_sigma_d(float density, int channel) {
  if (channel == 0) {
    return metal_film_grain_eval_density_sigma(density, kMetalFilmGrainRedDensity,
                                               kMetalFilmGrainRedSigma);
  }
  if (channel == 1) {
    return metal_film_grain_eval_density_sigma(density, kMetalFilmGrainGreenDensity,
                                               kMetalFilmGrainGreenSigma);
  }
  return metal_film_grain_eval_density_sigma(density, kMetalFilmGrainBlueDensity,
                                             kMetalFilmGrainBlueSigma);
}

static inline float metal_film_grain_datasheet_granularity_scale(float signal, int channel) {
  const float density = metal_film_grain_layer_density(signal, channel);
  const float sigma   = metal_film_grain_datasheet_sigma_d(density, channel);
  return clamp(sigma / 0.0075f, 0.55f, 2.15f);
}

static inline float metal_film_grain_sample(float probability, int ref_x, int ref_y, int channel,
                                            constant MetalNeighborStageParams& params) {
  const ulong seed =
      (static_cast<ulong>(params.seed_hi_) << 32u) | static_cast<ulong>(params.seed_lo_);
  const ulong stream = metal_prng_pixel_stream_2d(ref_x, ref_y, static_cast<uint>(channel));
  const float draw   = metal_prng_uniform_float01(seed, stream, 0xd1b54a32d192ed03UL);
  return draw < clamp(probability, 0.0f, 1.0f) ? 1.0f : 0.0f;
}

static inline float metal_film_grain_sample_at(texture2d<float, access::read> src, int2 coord,
                                               int                                channel,
                                               constant MetalNeighborStageParams& params) {
  const int    width     = static_cast<int>(src.get_width());
  const int    height    = static_cast<int>(src.get_height());
  const int    clamped_x = clamp(coord.x, 0, width - 1);
  const int    clamped_y = clamp(coord.y, 0, height - 1);
  const float4 signal = src.read(uint2(static_cast<uint>(clamped_x), static_cast<uint>(clamped_y)));
  const int    ref_x =
      (params.roi_enabled_ != 0u)
             ? clamped_x
             : metal_film_grain_reference_coord(clamped_x, width, params.roi_x_, params.roi_scale_x_,
                                                params.roi_reference_width_, params.roi_enabled_);
  const int ref_y =
      (params.roi_enabled_ != 0u)
          ? clamped_y
          : metal_film_grain_reference_coord(clamped_y, height, params.roi_y_, params.roi_scale_y_,
                                             params.roi_reference_height_, params.roi_enabled_);
  return metal_film_grain_sample(metal_film_grain_channel(signal, channel), ref_x, ref_y, channel,
                                 params);
}

static inline float metal_film_grain_gaussian7(float c0, float n1, float p1, float n2, float p2,
                                               float n3, float p3) {
  return c0 * 0.49867642f + (n1 + p1) * 0.22831073f + (n2 + p2) * 0.02192964f +
         (n3 + p3) * 0.00042142f;
}

static inline float metal_film_grain_blur_horizontal_channel(
    texture2d<float, access::read> src, int2 center, int channel,
    constant MetalNeighborStageParams& params) {
  return metal_film_grain_gaussian7(
      metal_film_grain_sample_at(src, center, channel, params),
      metal_film_grain_sample_at(src, center + int2(-1, 0), channel, params),
      metal_film_grain_sample_at(src, center + int2(1, 0), channel, params),
      metal_film_grain_sample_at(src, center + int2(-2, 0), channel, params),
      metal_film_grain_sample_at(src, center + int2(2, 0), channel, params),
      metal_film_grain_sample_at(src, center + int2(-3, 0), channel, params),
      metal_film_grain_sample_at(src, center + int2(3, 0), channel, params));
}

static inline float4 metal_film_grain_blur_horizontal(texture2d<float, access::read> src, uint2 gid,
                                                      constant MetalNeighborStageParams& params) {
  const int2 center = int2(static_cast<int>(gid.x), static_cast<int>(gid.y));
  return float4(metal_film_grain_blur_horizontal_channel(src, center, 0, params),
                metal_film_grain_blur_horizontal_channel(src, center, 1, params),
                metal_film_grain_blur_horizontal_channel(src, center, 2, params),
                metal_detail_read_clamped(src, center).w);
}

static inline float4 metal_film_grain_blur_vertical(texture2d<float, access::read> src, uint2 gid) {
  const int2   center = int2(static_cast<int>(gid.x), static_cast<int>(gid.y));
  const float4 c0     = metal_detail_read_clamped(src, center);
  const float4 n1     = metal_detail_read_clamped(src, center + int2(0, -1));
  const float4 p1     = metal_detail_read_clamped(src, center + int2(0, 1));
  const float4 n2     = metal_detail_read_clamped(src, center + int2(0, -2));
  const float4 p2     = metal_detail_read_clamped(src, center + int2(0, 2));
  const float4 n3     = metal_detail_read_clamped(src, center + int2(0, -3));
  const float4 p3     = metal_detail_read_clamped(src, center + int2(0, 3));

  return float4(metal_film_grain_gaussian7(c0.x, n1.x, p1.x, n2.x, p2.x, n3.x, p3.x),
                metal_film_grain_gaussian7(c0.y, n1.y, p1.y, n2.y, p2.y, n3.y, p3.y),
                metal_film_grain_gaussian7(c0.z, n1.z, p1.z, n2.z, p2.z, n3.z, p3.z), c0.w);
}

static inline float4 metal_apply_film_grain(float4 px, float4 blur,
                                            constant MetalNeighborStageParams& params) {
  if (params.enabled_ == 0u || !(params.amount_ > 0.0f)) {
    return px;
  }

  const float red_strength = params.amount_ * metal_film_grain_datasheet_granularity_scale(px.x, 0);
  const float green_strength =
      params.amount_ * metal_film_grain_datasheet_granularity_scale(px.y, 1);
  const float blue_strength =
      params.amount_ * metal_film_grain_datasheet_granularity_scale(px.z, 2);
  return float4(px.x + red_strength * (blur.x - px.x), px.y + green_strength * (blur.y - px.y),
                px.z + blue_strength * (blur.z - px.z), px.w);
}
