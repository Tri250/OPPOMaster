//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

__kernel void edit_pipeline_fused_rgba32f(__global const float* input,
                                          __global float* output,
                                          __global const OpenClFusedParams* params,
                                          int width,
                                          int height) {
  int x = get_global_id(0);
  int y = get_global_id(1);
  if (x >= width || y >= height) {
    return;
  }
  int idx = (y * width + x) * 4;
  output[idx + 0] = input[idx + 0];
  output[idx + 1] = input[idx + 1];
  output[idx + 2] = input[idx + 2];
  output[idx + 3] = input[idx + 3];
}
