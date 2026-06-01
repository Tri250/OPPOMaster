//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "../../operators/GPU_kernels/metal_shader/common.metal"
#include "../../operators/GPU_kernels/metal_shader/basic.metal"
#include "../../operators/GPU_kernels/metal_shader/color.metal"
#include "../../operators/GPU_kernels/metal_shader/cst.metal"
#include "../../operators/GPU_kernels/metal_shader/detail.metal"

static inline float4 metal_fused_pre_hs(float4 px, constant MetalFusedParams& params) {
  px = GPU_TOWS_Kernel(px, params);
  px = GPU_ExposureOpKernel(px, params);
  px = GPU_ContrastOpKernel(px, params);
  px = GPU_ToneOpKernel(px, params);
  return px;
}

static inline float4 metal_fused_post_hs(float4 px,
                                         constant MetalFusedParams& params,
                                         device const float4* lmt_lut) {
  px = GPU_CurveOpKernel(px, params);
  px = GPU_VibranceOpKernel(px, params);
  px = GPU_ColorWheelOpKernel(px, params);
  px = GPU_HLSOpKernel(px, params);
  px = GPU_LMT_Kernel(px, params, lmt_lut);
  px = GPU_OUTPUT_Kernel(px, params);
  return px;
}

static inline float4 metal_fused_full(float4 px,
                                      constant MetalFusedParams& params,
                                      device const float4* lmt_lut) {
  px = metal_fused_pre_hs(px, params);
  px = GPU_HighlightOpKernel(px, params);
  px = GPU_ShadowOpKernel(px, params);
  px = metal_fused_post_hs(px, params, lmt_lut);
  return px;
}

kernel void metal_fused_pipeline_rgba32f(texture2d<float, access::read> src [[texture(0)]],
                                         texture2d<float, access::write> dst [[texture(1)]],
                                         constant MetalFusedParams& params [[buffer(0)]],
                                         device const float4* lmt_lut [[buffer(1)]],
                                         uint2 gid [[thread_position_in_grid]]) {
  if (gid.x >= dst.get_width() || gid.y >= dst.get_height()) {
    return;
  }

  float4 px = src.read(gid);
  px        = metal_fused_full(px, params, lmt_lut);

  (void)lmt_lut;
  dst.write(px, gid);
}

kernel void metal_fused_stage_rgba32f(texture2d<float, access::read> src [[texture(0)]],
                                      texture2d<float, access::write> dst [[texture(1)]],
                                      constant MetalFusedParams& params [[buffer(0)]],
                                      device const float4* lmt_lut [[buffer(1)]],
                                      constant int& stage [[buffer(2)]],
                                      uint2 gid [[thread_position_in_grid]]) {
  if (gid.x >= dst.get_width() || gid.y >= dst.get_height()) {
    return;
  }

  float4 px = src.read(gid);
  if (stage == 1) {
    px = metal_fused_pre_hs(px, params);
  } else if (stage == 2) {
    px = metal_fused_post_hs(px, params, lmt_lut);
  } else {
    px = metal_fused_full(px, params, lmt_lut);
  }

  (void)lmt_lut;
  dst.write(px, gid);
}

kernel void metal_hs_build_log_base_h_rgba32f(texture2d<float, access::read> src [[texture(0)]],
                                             texture2d<float, access::write> dst [[texture(1)]],
                                             constant MetalFusedParams& params [[buffer(0)]],
                                             uint2 gid [[thread_position_in_grid]]) {
  if (gid.x >= dst.get_width() || gid.y >= dst.get_height()) {
    return;
  }

  const int width = static_cast<int>(dst.get_width());
  const int x = static_cast<int>(gid.x);
  const int y = static_cast<int>(gid.y);
  const int tap_count = params.hs_base_gaussian_tap_count_;
  if (tap_count <= 0) {
    dst.write(float4(metal_hs_log2_luminance_from_acescc(src.read(gid))), gid);
    return;
  }

  const float center = metal_hs_log2_luminance_from_acescc(src.read(gid));
  float base = center * params.hs_base_gaussian_weights_[0];
  float weight_sum = params.hs_base_gaussian_weights_[0];
  for (int tap = 1; tap < tap_count; ++tap) {
    const int ax = min(x + tap, width - 1);
    const int bx = max(x - tap, 0);
    const float wa = metal_hs_log2_luminance_from_acescc(
        src.read(uint2(static_cast<uint>(ax), static_cast<uint>(y))));
    const float wb = metal_hs_log2_luminance_from_acescc(
        src.read(uint2(static_cast<uint>(bx), static_cast<uint>(y))));
    const float spatial = params.hs_base_gaussian_weights_[tap];
    const float aw = spatial * metal_hs_range_weight(center, wa);
    const float bw = spatial * metal_hs_range_weight(center, wb);
    base += wa * aw + wb * bw;
    weight_sum += aw + bw;
  }

  dst.write(float4(base / fmax(weight_sum, 1.0e-6f)), gid);
}

kernel void metal_hs_build_log_base_v_rgba32f(texture2d<float, access::read> guidance [[texture(0)]],
                                             texture2d<float, access::read> src [[texture(1)]],
                                             texture2d<float, access::write> dst [[texture(2)]],
                                             constant MetalFusedParams& params [[buffer(0)]],
                                             uint2 gid [[thread_position_in_grid]]) {
  if (gid.x >= dst.get_width() || gid.y >= dst.get_height()) {
    return;
  }

  const int height = static_cast<int>(dst.get_height());
  const int x = static_cast<int>(gid.x);
  const int y = static_cast<int>(gid.y);
  const int tap_count = params.hs_base_gaussian_tap_count_;
  if (tap_count <= 0) {
    dst.write(src.read(gid), gid);
    return;
  }

  const float center = src.read(gid).x;
  const float center_guidance = metal_hs_log2_luminance_from_acescc(guidance.read(gid));
  float base = center * params.hs_base_gaussian_weights_[0];
  float weight_sum = params.hs_base_gaussian_weights_[0];
  for (int tap = 1; tap < tap_count; ++tap) {
    const int ay = min(y + tap, height - 1);
    const int by = max(y - tap, 0);
    const uint2 acoord = uint2(static_cast<uint>(x), static_cast<uint>(ay));
    const uint2 bcoord = uint2(static_cast<uint>(x), static_cast<uint>(by));
    const float a = src.read(acoord).x;
    const float b = src.read(bcoord).x;
    const float ag = metal_hs_log2_luminance_from_acescc(guidance.read(acoord));
    const float bg = metal_hs_log2_luminance_from_acescc(guidance.read(bcoord));
    const float spatial = params.hs_base_gaussian_weights_[tap];
    const float aw = spatial * metal_hs_range_weight(center_guidance, ag);
    const float bw = spatial * metal_hs_range_weight(center_guidance, bg);
    base += a * aw + b * bw;
    weight_sum += aw + bw;
  }

  dst.write(float4(base / fmax(weight_sum, 1.0e-6f)), gid);
}

static inline float metal_hs_read_base_bilinear(texture2d<float, access::read> base_log,
                                                int width, int height, float x, float y) {
  const float clamped_x = clamp(x, 0.0f, static_cast<float>(width - 1));
  const float clamped_y = clamp(y, 0.0f, static_cast<float>(height - 1));
  const int x0 = clamp(static_cast<int>(floor(clamped_x)), 0, width - 1);
  const int y0 = clamp(static_cast<int>(floor(clamped_y)), 0, height - 1);
  const int x1 = min(x0 + 1, width - 1);
  const int y1 = min(y0 + 1, height - 1);
  const float tx = clamped_x - static_cast<float>(x0);
  const float ty = clamped_y - static_cast<float>(y0);

  const float v00 = base_log.read(uint2(static_cast<uint>(x0), static_cast<uint>(y0))).x;
  const float v10 = base_log.read(uint2(static_cast<uint>(x1), static_cast<uint>(y0))).x;
  const float v01 = base_log.read(uint2(static_cast<uint>(x0), static_cast<uint>(y1))).x;
  const float v11 = base_log.read(uint2(static_cast<uint>(x1), static_cast<uint>(y1))).x;
  const float vx0 = mix(v00, v10, tx);
  const float vx1 = mix(v01, v11, tx);
  return mix(vx0, vx1, ty);
}

struct MetalHsApplyParams {
  int base_width_;
  int base_height_;
  int use_reference_base_;
  int reserved_;
};

kernel void metal_hs_apply_local_tone_rgba32f(texture2d<float, access::read> src [[texture(0)]],
                                             texture2d<float, access::read> base_log [[texture(1)]],
                                             texture2d<float, access::write> dst [[texture(2)]],
                                             constant MetalFusedParams& params [[buffer(0)]],
                                             constant MetalHsApplyParams& apply_params [[buffer(1)]],
                                             uint2 gid [[thread_position_in_grid]]) {
  if (gid.x >= dst.get_width() || gid.y >= dst.get_height()) {
    return;
  }

  const int width = static_cast<int>(dst.get_width());
  const int height = static_cast<int>(dst.get_height());
  const int base_width = max(apply_params.base_width_, 1);
  const int base_height = max(apply_params.base_height_, 1);
  const int x = static_cast<int>(gid.x);
  const int y = static_cast<int>(gid.y);

  float base = 0.0f;
  if (apply_params.use_reference_base_ != 0) {
    const float reference_width =
        static_cast<float>(max(params.render_roi_reference_width_, width));
    const float reference_height =
        static_cast<float>(max(params.render_roi_reference_height_, height));
    const float roi_origin_x = (params.render_roi_enabled_ != 0u)
                                   ? static_cast<float>(params.render_roi_x_)
                                   : 0.0f;
    const float roi_origin_y = (params.render_roi_enabled_ != 0u)
                                   ? static_cast<float>(params.render_roi_y_)
                                   : 0.0f;
    const float roi_width = (params.render_roi_enabled_ != 0u)
                                ? fmax(params.render_roi_scale_x_ * reference_width, 1.0f)
                                : reference_width;
    const float roi_height = (params.render_roi_enabled_ != 0u)
                                 ? fmax(params.render_roi_scale_y_ * reference_height, 1.0f)
                                 : reference_height;
    const float reference_x =
        roi_origin_x + ((static_cast<float>(x) + 0.5f) * roi_width /
                        fmax(static_cast<float>(width), 1.0f)) -
        0.5f;
    const float reference_y =
        roi_origin_y + ((static_cast<float>(y) + 0.5f) * roi_height /
                        fmax(static_cast<float>(height), 1.0f)) -
        0.5f;
    const float base_x =
        ((reference_x + 0.5f) * static_cast<float>(base_width) / fmax(reference_width, 1.0f)) -
        0.5f;
    const float base_y =
        ((reference_y + 0.5f) * static_cast<float>(base_height) / fmax(reference_height, 1.0f)) -
        0.5f;
    base = metal_hs_read_base_bilinear(base_log, base_width, base_height, base_x, base_y);
  } else {
    base = base_log.read(gid).x;
  }

  dst.write(GPU_HighlightShadowLocalToneOpKernel(src.read(gid), base, params), gid);
}
