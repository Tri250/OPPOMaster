//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

// OpenCL kernels for highlight reconstruction (opposed algorithm).
// Operates on RGBA32F buffers.

typedef struct {
  float clips[4];
  float clipdark[4];
  float chrominance[4];
  uint  width;
  uint  height;
  uint  stride;
} HighlightParams;

constant uint kDilateRadius   = 3u;
constant uint kPlaneBaseR     = 0u;
constant uint kPlaneBaseG     = 1u;
constant uint kPlaneBaseB     = 2u;
constant uint kPlaneDilatedR  = 3u;
constant uint kPlaneDilatedG  = 4u;
constant uint kPlaneDilatedB  = 5u;
constant uint kPlaneBaseMulti = 6u;
constant uint kPlaneDilMulti  = 7u;

static inline float Cube(float value) { return value * value * value; }

static inline float4 MaxRgb(float4 value) {
  return (float4)(fmax(value.x, 0.0f), fmax(value.y, 0.0f), fmax(value.z, 0.0f), value.w);
}

static inline int CountClippedChannels(float4 pixel, HighlightParams params) {
  int count = 0;
  count += pixel.x >= params.clips[0] ? 1 : 0;
  count += pixel.y >= params.clips[1] ? 1 : 0;
  count += pixel.z >= params.clips[2] ? 1 : 0;
  return count;
}

static inline float4 CalcRefavg(global const float4* input, int row, int col,
                                HighlightParams params) {
  float mean[3] = {0.0f, 0.0f, 0.0f};
  float cnt[3]  = {0.0f, 0.0f, 0.0f};

  const int dymin = max(0, row - 1);
  const int dxmin = max(0, col - 1);
  const int dymax = min((int)params.height - 1, row + 1);
  const int dxmax = min((int)params.width - 1, col + 1);

  for (int dy = dymin; dy <= dymax; ++dy) {
    for (int dx = dxmin; dx <= dxmax; ++dx) {
      const float4 sample = MaxRgb(input[dy * params.stride + dx]);
      mean[0] += sample.x;
      mean[1] += sample.y;
      mean[2] += sample.z;
      cnt[0] += 1.0f;
      cnt[1] += 1.0f;
      cnt[2] += 1.0f;
    }
  }

  for (uint c = 0; c < 3u; ++c) {
    mean[c] = (cnt[c] > 0.0f) ? pow(mean[c] / cnt[c], 1.0f / 3.0f) : 0.0f;
  }

  return (float4)(Cube(0.5f * (mean[1] + mean[2])),
                  Cube(0.5f * (mean[0] + mean[2])),
                  Cube(0.5f * (mean[0] + mean[1])),
                  0.0f);
}

static inline uchar DilateMaskAt(global const uchar* plane, uint width, uint height, int row,
                                 int col, int radius) {
  const int y0 = max(0, row - radius);
  const int x0 = max(0, col - radius);
  const int y1 = min((int)height - 1, row + radius);
  const int x1 = min((int)width - 1, col + radius);

  for (int y = y0; y <= y1; ++y) {
    const int row_offset = y * (int)width;
    for (int x = x0; x <= x1; ++x) {
      if (plane[row_offset + x] != 0) {
        return 1;
      }
    }
  }

  return 0;
}

static inline void AtomicAddFloat(global float* addr, float val) {
  int old_val = as_int(*addr);
  int new_val;
  int cur_val;
  do {
    cur_val = old_val;
    new_val = as_int(as_float(cur_val) + val);
    old_val = atomic_cmpxchg((global int*)addr, cur_val, new_val);
  } while (old_val != cur_val);
}

__kernel void hlr_build_mask(global const float4* input,
                             global uchar*        mask_buf,
                             global int*          anyclipped,
                             HighlightParams      params) {
  uint x = get_global_id(0);
  uint y = get_global_id(1);
  if (x >= params.width || y >= params.height) {
    return;
  }
  const uint   size  = params.width * params.height;
  const uint   index = y * params.stride + x;
  const uint   idx   = y * params.width + x;
  const float4 pixel = MaxRgb(input[index]);
  const int    count = CountClippedChannels(pixel, params);

  mask_buf[kPlaneBaseR * size + idx]     = pixel.x >= params.clips[0] ? 1 : 0;
  mask_buf[kPlaneBaseG * size + idx]     = pixel.y >= params.clips[1] ? 1 : 0;
  mask_buf[kPlaneBaseB * size + idx]     = pixel.z >= params.clips[2] ? 1 : 0;
  mask_buf[kPlaneBaseMulti * size + idx] = count >= 2 ? 1 : 0;

  if (count > 0) {
    atomic_add(anyclipped, 1);
  }
}

__kernel void hlr_dilate_mask(global const uchar* mask_buf,
                              global uchar*       dilated_mask_buf,
                              HighlightParams     params) {
  uint x = get_global_id(0);
  uint y = get_global_id(1);
  if (x >= params.width || y >= params.height) {
    return;
  }
  const uint size = params.width * params.height;
  const uint idx  = y * params.width + x;

  dilated_mask_buf[kPlaneDilatedR * size + idx] =
      DilateMaskAt(mask_buf + kPlaneBaseR * size, params.width, params.height,
                   (int)y, (int)x, (int)kDilateRadius);
  dilated_mask_buf[kPlaneDilatedG * size + idx] =
      DilateMaskAt(mask_buf + kPlaneBaseG * size, params.width, params.height,
                   (int)y, (int)x, (int)kDilateRadius);
  dilated_mask_buf[kPlaneDilatedB * size + idx] =
      DilateMaskAt(mask_buf + kPlaneBaseB * size, params.width, params.height,
                   (int)y, (int)x, (int)kDilateRadius);
  dilated_mask_buf[kPlaneDilMulti * size + idx] =
      DilateMaskAt(mask_buf + kPlaneBaseMulti * size, params.width, params.height,
                   (int)y, (int)x, (int)kDilateRadius);
}

__kernel void hlr_chrominance_contrib(global const float4* input,
                                      global const uchar*  mask_buf,
                                      global float*        global_sums,
                                      global float*        global_cnts,
                                      HighlightParams      params) {
  uint x = get_global_id(0);
  uint y = get_global_id(1);
  bool in_bounds = x < params.width && y < params.height;

  float4 contrib_value = (float4)(0.0f, 0.0f, 0.0f, 0.0f);
  float4 count_value   = (float4)(0.0f, 0.0f, 0.0f, 0.0f);

  if (in_bounds) {
    const uint   size  = params.width * params.height;
    const uint   index = y * params.stride + x;
    const uint   idx   = y * params.width + x;
    const float4 pixel = MaxRgb(input[index]);
    const float4 ref   = CalcRefavg(input, (int)y, (int)x, params);

    if (mask_buf[kPlaneDilatedR * size + idx] && pixel.x > params.clipdark[0] &&
        pixel.x < params.clips[0]) {
      contrib_value.x = pixel.x - ref.x;
      count_value.x   = 1.0f;
    }
    if (mask_buf[kPlaneDilatedG * size + idx] && pixel.y > params.clipdark[1] &&
        pixel.y < params.clips[1]) {
      contrib_value.y = pixel.y - ref.y;
      count_value.y   = 1.0f;
    }
    if (mask_buf[kPlaneDilatedB * size + idx] && pixel.z > params.clipdark[2] &&
        pixel.z < params.clips[2]) {
      contrib_value.z = pixel.z - ref.z;
      count_value.z   = 1.0f;
    }
  }

  const uint lid  = get_local_id(1) * get_local_size(0) + get_local_id(0);
  const uint lsz  = get_local_size(0) * get_local_size(1);

  local float l_contrib_r[256];
  local float l_contrib_g[256];
  local float l_contrib_b[256];
  local float l_cnt_r[256];
  local float l_cnt_g[256];
  local float l_cnt_b[256];

  l_contrib_r[lid] = contrib_value.x;
  l_contrib_g[lid] = contrib_value.y;
  l_contrib_b[lid] = contrib_value.z;
  l_cnt_r[lid]     = count_value.x;
  l_cnt_g[lid]     = count_value.y;
  l_cnt_b[lid]     = count_value.z;

  barrier(CLK_LOCAL_MEM_FENCE);

  for (uint stride = lsz / 2; stride > 0; stride >>= 1) {
    if (lid < stride) {
      l_contrib_r[lid] += l_contrib_r[lid + stride];
      l_contrib_g[lid] += l_contrib_g[lid + stride];
      l_contrib_b[lid] += l_contrib_b[lid + stride];
      l_cnt_r[lid] += l_cnt_r[lid + stride];
      l_cnt_g[lid] += l_cnt_g[lid + stride];
      l_cnt_b[lid] += l_cnt_b[lid + stride];
    }
    barrier(CLK_LOCAL_MEM_FENCE);
  }

  if (lid == 0) {
    AtomicAddFloat(global_sums + 0, l_contrib_r[0]);
    AtomicAddFloat(global_sums + 1, l_contrib_g[0]);
    AtomicAddFloat(global_sums + 2, l_contrib_b[0]);
    AtomicAddFloat(global_cnts + 0, l_cnt_r[0]);
    AtomicAddFloat(global_cnts + 1, l_cnt_g[0]);
    AtomicAddFloat(global_cnts + 2, l_cnt_b[0]);
  }
}

__kernel void hlr_reconstruct(global const float4* input,
                              global float4*       output,
                              HighlightParams      params) {
  uint x = get_global_id(0);
  uint y = get_global_id(1);
  if (x >= params.width || y >= params.height) {
    return;
  }

  const uint   index       = y * params.stride + x;
  const float4 input_pixel = input[index];
  const float4 pixel       = MaxRgb(input_pixel);
  const float4 ref         = CalcRefavg(input, (int)y, (int)x, params);

  float4 result = pixel;
  if (pixel.x >= params.clips[0]) {
    result.x = fmax(pixel.x, ref.x + params.chrominance[0]);
  }
  if (pixel.y >= params.clips[1]) {
    result.y = fmax(pixel.y, ref.y + params.chrominance[1]);
  }
  if (pixel.z >= params.clips[2]) {
    result.z = fmax(pixel.z, ref.z + params.chrominance[2]);
  }

  output[index] = (float4)(result.x, result.y, result.z, input_pixel.w);
}
