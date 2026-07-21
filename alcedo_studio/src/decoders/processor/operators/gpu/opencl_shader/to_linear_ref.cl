//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

// OpenCL kernels for RAW to linear reference conversion.
// Depends on definitions from raw_utils_opencl.cl (concatenated at build time).

// Converts uint16 raw input to linearized float output in one pass.
__kernel void to_linear_ref_u16_to_f32(global const ushort* image_in,
                                       global float*          image_out,
                                       WBParams               wb,
                                       PatternParams          pattern) {
  int x = get_global_id(0);
  int y = get_global_id(1);

  if (x >= pattern.width || y >= pattern.height) {
    return;
  }

  int color_idx = RawColorAt(pattern, y, x);
  int idx       = y * pattern.width + x;

  float sample = (float)image_in[idx];
  float black  = wb.black_level[color_idx] + PatternBlackAt(wb, y, x);
  float pixel  = NormalizeSample(sample, black, wb.white_level[color_idx]);
  pixel *= RelativeWBMultiplier(wb, color_idx);

  image_out[idx] = pixel;
}

// In-place float variant for already-converted buffers.
__kernel void to_linear_ref_f32(global float* image,
                                WBParams      wb,
                                PatternParams pattern) {
  int x = get_global_id(0);
  int y = get_global_id(1);

  if (x >= pattern.width || y >= pattern.height) {
    return;
  }

  int color_idx = RawColorAt(pattern, y, x);
  int idx       = y * pattern.width + x;

  float sample = image[idx];
  float black  = wb.black_level[color_idx] + PatternBlackAt(wb, y, x);
  float pixel  = NormalizeSample(sample, black, wb.white_level[color_idx]);
  pixel *= RelativeWBMultiplier(wb, color_idx);

  image[idx] = pixel;
}
