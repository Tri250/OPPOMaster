//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

inline float4 edit_geometry_read_pixel(__global const float* src, int width, int height, int stride,
                                       int channels, int x, int y, float4 border_value) {
  if (x < 0 || y < 0 || x >= width || y >= height) {
    return border_value;
  }
  __global const float* p = src + y * stride + x * channels;
  if (channels == 1) {
    return (float4)(p[0], 0.0f, 0.0f, 1.0f);
  }
  if (channels == 3) {
    return (float4)(p[0], p[1], p[2], 1.0f);
  }
  return (float4)(p[0], p[1], p[2], p[3]);
}

inline void edit_geometry_write_pixel(__global float* dst, int stride, int channels, int x, int y,
                                      float4 value) {
  __global float* p = dst + y * stride + x * channels;
  if (channels == 1) {
    p[0] = value.x;
  } else if (channels == 3) {
    p[0] = value.x;
    p[1] = value.y;
    p[2] = value.z;
  } else {
    p[0] = value.x;
    p[1] = value.y;
    p[2] = value.z;
    p[3] = value.w;
  }
}

inline float4 edit_geometry_bilinear_sample(__global const float* src, int width, int height,
                                            int stride, int channels, float sx, float sy,
                                            float4 border_value) {
  const int    x0  = (int)floor(sx);
  const int    y0  = (int)floor(sy);
  const int    x1  = x0 + 1;
  const int    y1  = y0 + 1;

  const float  fx  = sx - (float)x0;
  const float  fy  = sy - (float)y0;

  const float  w00 = (1.0f - fx) * (1.0f - fy);
  const float  w10 = fx * (1.0f - fy);
  const float  w01 = (1.0f - fx) * fy;
  const float  w11 = fx * fy;

  const float4 p00 =
      edit_geometry_read_pixel(src, width, height, stride, channels, x0, y0, border_value);
  const float4 p10 =
      edit_geometry_read_pixel(src, width, height, stride, channels, x1, y0, border_value);
  const float4 p01 =
      edit_geometry_read_pixel(src, width, height, stride, channels, x0, y1, border_value);
  const float4 p11 =
      edit_geometry_read_pixel(src, width, height, stride, channels, x1, y1, border_value);
  return p00 * w00 + p10 * w10 + p01 * w01 + p11 * w11;
}

__kernel void opencl_geometry_crop_resize_linear(__global const float* src, int src_width,
                                                 int src_height, int src_stride, int channels,
                                                 int crop_x, int crop_y, int crop_width,
                                                 int crop_height, __global float* dst,
                                                 int dst_width, int dst_height, int dst_stride,
                                                 float scale_x, float scale_y) {
  const int x = (int)get_global_id(0);
  const int y = (int)get_global_id(1);
  if (x >= dst_width || y >= dst_height) {
    return;
  }

  (void)crop_width;
  (void)crop_height;
  const float  sx    = (float)crop_x + ((float)x + 0.5f) * scale_x - 0.5f;
  const float  sy    = (float)crop_y + ((float)y + 0.5f) * scale_y - 0.5f;
  const float4 value = edit_geometry_bilinear_sample(src, src_width, src_height, src_stride,
                                                     channels, sx, sy, (float4)(0.0f));
  edit_geometry_write_pixel(dst, dst_stride, channels, x, y, value);
}

__kernel void opencl_geometry_crop_resize_area(__global const float* src, int src_width,
                                               int src_height, int src_stride, int channels,
                                               int crop_x, int crop_y, int crop_width,
                                               int crop_height, __global float* dst, int dst_width,
                                               int dst_height, int dst_stride, float scale_x,
                                               float scale_y) {
  const int x = (int)get_global_id(0);
  const int y = (int)get_global_id(1);
  if (x >= dst_width || y >= dst_height) {
    return;
  }

  const float sx0   = (float)crop_x + (float)x * scale_x;
  const float sx1   = (float)crop_x + (float)(x + 1) * scale_x;
  const float sy0   = (float)crop_y + (float)y * scale_y;
  const float sy1   = (float)crop_y + (float)(y + 1) * scale_y;

  const int   ix0   = max(crop_x, (int)floor(sx0));
  const int   ix1   = min(crop_x + crop_width, (int)ceil(sx1));
  const int   iy0   = max(crop_y, (int)floor(sy0));
  const int   iy1   = min(crop_y + crop_height, (int)ceil(sy1));

  float4      acc   = (float4)(0.0f);
  float       total = 0.0f;
  for (int yy = iy0; yy < iy1; ++yy) {
    const float yy0 = fmax(sy0, (float)yy);
    const float yy1 = fmin(sy1, (float)(yy + 1));
    const float wy  = fmax(0.0f, yy1 - yy0);
    if (wy <= 0.0f) {
      continue;
    }
    for (int xx = ix0; xx < ix1; ++xx) {
      const float xx0 = fmax(sx0, (float)xx);
      const float xx1 = fmin(sx1, (float)(xx + 1));
      const float wx  = fmax(0.0f, xx1 - xx0);
      const float w   = wx * wy;
      if (w <= 0.0f) {
        continue;
      }
      acc += edit_geometry_read_pixel(src, src_width, src_height, src_stride, channels, xx, yy,
                                      (float4)(0.0f)) *
             w;
      total += w;
    }
  }

  if (total <= 1.0e-8f) {
    const int sx = clamp((int)sx0, 0, src_width - 1);
    const int sy = clamp((int)sy0, 0, src_height - 1);
    edit_geometry_write_pixel(dst, dst_stride, channels, x, y,
                              edit_geometry_read_pixel(src, src_width, src_height, src_stride,
                                                       channels, sx, sy, (float4)(0.0f)));
    return;
  }

  edit_geometry_write_pixel(dst, dst_stride, channels, x, y, acc / total);
}

__kernel void opencl_geometry_warp_affine_linear(__global const float* src, int src_width,
                                                 int src_height, int src_stride, int channels,
                                                 __global float* dst, int dst_width, int dst_height,
                                                 int dst_stride, float m00, float m01, float m02,
                                                 float m10, float m11, float m12,
                                                 float4 border_value) {
  const int x = (int)get_global_id(0);
  const int y = (int)get_global_id(1);
  if (x >= dst_width || y >= dst_height) {
    return;
  }

  const float  sx    = m00 * (float)x + m01 * (float)y + m02;
  const float  sy    = m10 * (float)x + m11 * (float)y + m12;
  const float4 value = edit_geometry_bilinear_sample(src, src_width, src_height, src_stride,
                                                     channels, sx, sy, border_value);
  edit_geometry_write_pixel(dst, dst_stride, channels, x, y, value);
}

typedef struct OpenClDngWarpRectilinearParams {
  uint  coefficient_set_count;
  uint  width;
  uint  height;
  uint  src_stride;
  uint  dst_stride;
  uint  channels;
  float coefficient_sets[3][6];
  float center_x;
  float center_y;
} OpenClDngWarpRectilinearParams;

inline float opencl_geometry_read_channel_or_zero(__global const float*          src,
                                                  OpenClDngWarpRectilinearParams p, int x, int y,
                                                  int channel) {
  if (x < 0 || y < 0 || x >= (int)p.width || y >= (int)p.height || channel < 0 ||
      channel >= (int)p.channels) {
    return 0.0f;
  }
  return src[(uint)y * p.src_stride + (uint)x * p.channels + (uint)channel];
}

inline float opencl_geometry_bilinear_sample_channel(__global const float*          src,
                                                     OpenClDngWarpRectilinearParams p, float sx,
                                                     float sy, int channel) {
  const int   x0  = (int)floor(sx);
  const int   y0  = (int)floor(sy);
  const int   x1  = x0 + 1;
  const int   y1  = y0 + 1;
  const float fx  = sx - (float)x0;
  const float fy  = sy - (float)y0;
  const float w00 = (1.0f - fx) * (1.0f - fy);
  const float w10 = fx * (1.0f - fy);
  const float w01 = (1.0f - fx) * fy;
  const float w11 = fx * fy;
  return opencl_geometry_read_channel_or_zero(src, p, x0, y0, channel) * w00 +
         opencl_geometry_read_channel_or_zero(src, p, x1, y0, channel) * w10 +
         opencl_geometry_read_channel_or_zero(src, p, x0, y1, channel) * w01 +
         opencl_geometry_read_channel_or_zero(src, p, x1, y1, channel) * w11;
}

inline float2 opencl_geometry_warp_rectilinear_source_coord(int x, int y, int plane,
                                                            OpenClDngWarpRectilinearParams p) {
  const float x0 = 0.0f;
  const float y0 = 0.0f;
  const float x1 = (float)max((int)p.width - 1, 0);
  const float y1 = (float)max((int)p.height - 1, 0);
  const float cx = x0 + p.center_x * (x1 - x0);
  const float cy = y0 + p.center_y * (y1 - y0);
  const float mx = fmax(fabs(x0 - cx), fabs(x1 - cx));
  const float my = fmax(fabs(y0 - cy), fabs(y1 - cy));
  const float m  = sqrt(mx * mx + my * my);
  if (m <= 1.0e-8f) {
    return (float2)((float)x, (float)y);
  }

  const uint  set_index = p.coefficient_set_count <= 1u ? 0u : (uint)min(max(plane, 0), 2);
  const float dx        = ((float)x - cx) / m;
  const float dy        = ((float)y - cy) / m;
  const float r2        = dx * dx + dy * dy;
  const float f         = p.coefficient_sets[set_index][0] + p.coefficient_sets[set_index][1] * r2 +
                  p.coefficient_sets[set_index][2] * r2 * r2 +
                  p.coefficient_sets[set_index][3] * r2 * r2 * r2;
  const float dxr = f * dx;
  const float dyr = f * dy;
  const float dxt = p.coefficient_sets[set_index][4] * (2.0f * dx * dy) +
                    p.coefficient_sets[set_index][5] * (r2 + 2.0f * dx * dx);
  const float dyt = p.coefficient_sets[set_index][5] * (2.0f * dx * dy) +
                    p.coefficient_sets[set_index][4] * (r2 + 2.0f * dy * dy);
  return (float2)(cx + m * (dxr + dxt), cy + m * (dyr + dyt));
}

__kernel void opencl_geometry_warp_rectilinear(__global const float* src, __global float* dst,
                                               OpenClDngWarpRectilinearParams p) {
  const int x = (int)get_global_id(0);
  const int y = (int)get_global_id(1);
  if (x >= (int)p.width || y >= (int)p.height) {
    return;
  }

  const float2    red   = opencl_geometry_warp_rectilinear_source_coord(x, y, 0, p);
  const float2    green = opencl_geometry_warp_rectilinear_source_coord(x, y, 1, p);
  const float2    blue  = opencl_geometry_warp_rectilinear_source_coord(x, y, 2, p);
  __global float* out   = dst + (uint)y * p.dst_stride + (uint)x * p.channels;
  out[0]                = opencl_geometry_bilinear_sample_channel(src, p, red.x, red.y, 0);
  out[1]                = opencl_geometry_bilinear_sample_channel(src, p, green.x, green.y, 1);
  out[2]                = opencl_geometry_bilinear_sample_channel(src, p, blue.x, blue.y, 2);
  if (p.channels == 4u) {
    out[3] = opencl_geometry_bilinear_sample_channel(src, p, green.x, green.y, 3);
  }
}

__kernel void opencl_geometry_rotate(__global const float* src, int src_width, int src_height,
                                     int src_stride, int channels, __global float* dst,
                                     int dst_width, int dst_height, int dst_stride, int mode) {
  const int x = (int)get_global_id(0);
  const int y = (int)get_global_id(1);
  if (x >= dst_width || y >= dst_height) {
    return;
  }

  int sx = x;
  int sy = y;
  if (mode == 0) {
    sx = src_width - 1 - x;
    sy = src_height - 1 - y;
  } else if (mode == 1) {
    sx = y;
    sy = src_height - 1 - x;
  } else if (mode == 2) {
    sx = src_width - 1 - y;
    sy = x;
  }

  const float4 value = edit_geometry_read_pixel(src, src_width, src_height, src_stride, channels,
                                                sx, sy, (float4)(0.0f));
  edit_geometry_write_pixel(dst, dst_stride, channels, x, y, value);
}
