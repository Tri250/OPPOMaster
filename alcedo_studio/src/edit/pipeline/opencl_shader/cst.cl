//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifndef ALCEDO_OPENCL_EDIT_PIPELINE_CST_CL
#define ALCEDO_OPENCL_EDIT_PIPELINE_CST_CL

// === 3D LUT helper ============================================================

static inline uint opencl_lut3d_index(uint edge, uint x, uint y, uint z) {
  return (z * edge + y) * edge + x;
}

// Sample a 3D LUT with trilinear interpolation.
// The LUT is stored as a flat buffer of packed float4 (R, G, B, A) values,
// with x (red) varying fastest, then y (green), then z (blue).
// Coordinate mapping matches CUDA tex3D with normalized coordinates and
// cudaFilterModeLinear.
static inline float4 opencl_sample_lut3d_linear(__global const float* lut, uint edge,
                                                 float u, float v, float w) {
  // tex3D normalized-coordinate mapping: texel_pos = coord * size - 0.5
  float3 tex_pos = (float3)(u * (float)edge - 0.5f,
                             v * (float)edge - 0.5f,
                             w * (float)edge - 0.5f);
  float3 pos     = clamp(tex_pos, 0.0f, (float)(edge - 1));
  uint3  lo      = convert_uint3(pos);
  uint3  hi      = min(lo + (uint3)(1), (uint3)(edge - 1));
  float3 t       = pos - convert_float3(lo);

  uint e = edge;
  // vload4(offset, ptr) reads from ptr + offset*4, so the offset is the
  // voxel index directly (each voxel is 4 packed floats).
  float4 c000 = vload4(opencl_lut3d_index(e, lo.x, lo.y, lo.z), lut);
  float4 c100 = vload4(opencl_lut3d_index(e, hi.x, lo.y, lo.z), lut);
  float4 c010 = vload4(opencl_lut3d_index(e, lo.x, hi.y, lo.z), lut);
  float4 c110 = vload4(opencl_lut3d_index(e, hi.x, hi.y, lo.z), lut);
  float4 c001 = vload4(opencl_lut3d_index(e, lo.x, lo.y, hi.z), lut);
  float4 c101 = vload4(opencl_lut3d_index(e, hi.x, lo.y, hi.z), lut);
  float4 c011 = vload4(opencl_lut3d_index(e, lo.x, hi.y, hi.z), lut);
  float4 c111 = vload4(opencl_lut3d_index(e, hi.x, hi.y, hi.z), lut);

  // Manual trilinear interpolation (avoids potential mix() scalar broadcast
  // differences across OpenCL implementations).
  float4 c00 = c000 + (c100 - c000) * t.x;
  float4 c10 = c010 + (c110 - c010) * t.x;
  float4 c01 = c001 + (c101 - c001) * t.x;
  float4 c11 = c011 + (c111 - c011) * t.x;
  float4 c0  = c00  + (c10  - c00)  * t.y;
  float4 c1  = c01  + (c11  - c01)  * t.y;
  return c0 + (c1 - c0) * t.z;
}

// === LMT (Look Modification Transform) operator ===============================

static inline float4 opencl_lmt_op(float4 px, __global const OpenClFusedParams* params,
                                    __global const float* lmt_lut) {
  if (params->lmt_enabled_ == 0u || params->lmt_lut_enabled_ == 0u ||
      params->lmt_lut_edge_size_ <= 1u) {
    return px;
  }

  uint  edge   = params->lmt_lut_edge_size_;
  float scale  = (float)(edge - 1u) / (float)edge;
  float offset = 1.0f / (2.0f * (float)edge);

  float u = px.x * scale + offset;
  float v = px.y * scale + offset;
  float w = px.z * scale + offset;

  float4 lut_v = opencl_sample_lut3d_linear(lmt_lut, edge, u, v, w);
  return (float4)(lut_v.x, lut_v.y, lut_v.z, px.w);
}

#endif  // ALCEDO_OPENCL_EDIT_PIPELINE_CST_CL
