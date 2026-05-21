//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

// OpenCL kernels for reference-space color conversion.

typedef struct {
  float scale_r;
  float scale_g;
  float scale_b;
  float scale_a;
  uint  width;
  uint  height;
  uint  stride;
} InverseCamMulParams;

__kernel void apply_inverse_cam_mul_rgba32f(global float4* buffer,
                                            InverseCamMulParams params) {
  uint x = get_global_id(0);
  uint y = get_global_id(1);
  if (x >= params.width || y >= params.height) {
    return;
  }

  uint   idx = y * params.stride + x;
  float4 v   = buffer[idx];
  v.x *= params.scale_r;
  v.y *= params.scale_g;
  v.z *= params.scale_b;
  v.w *= params.scale_a;
  buffer[idx] = v;
}
