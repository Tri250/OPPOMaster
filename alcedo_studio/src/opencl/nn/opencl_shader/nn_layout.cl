//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifndef ALCEDO_OPENCL_NN_LAYOUT_CL
#define ALCEDO_OPENCL_NN_LAYOUT_CL

// Concatenate two NHWC4 tensors along the channel dimension.
// out_channels = a_logical_channels + b_logical_channels
// out_channel_blocks = (out_channels + 3) / 4
__kernel void opencl_nn_concat_channels_nhwc4(
    __global const float* a,
    __global const float* b,
    __global float*       output,
    const int             spatial_size,      // batch * height * width
    const int             a_channel_blocks,
    const int             b_channel_blocks,
    const int             out_channel_blocks,
    const int             a_logical_channels,
    const int             b_logical_channels) {
  const int spatial_idx = get_global_id(0);
  const int out_cb      = get_global_id(1);
  if (spatial_idx >= spatial_size || out_cb >= out_channel_blocks) return;

  const int out_base = spatial_idx * out_channel_blocks * 4 + out_cb * 4;
  float4 out_val = (float4)(0.0f);

  // Copy from tensor A
  if (out_cb < a_channel_blocks) {
    const int a_base = spatial_idx * a_channel_blocks * 4 + out_cb * 4;
    out_val.s0 = a[a_base + 0];
    out_val.s1 = a[a_base + 1];
    out_val.s2 = a[a_base + 2];
    out_val.s3 = a[a_base + 3];
  } else {
    // Copy from tensor B
    const int b_cb = out_cb - a_channel_blocks;
    if (b_cb < b_channel_blocks) {
      const int b_base = spatial_idx * b_channel_blocks * 4 + b_cb * 4;
      out_val.s0 = b[b_base + 0];
      out_val.s1 = b[b_base + 1];
      out_val.s2 = b[b_base + 2];
      out_val.s3 = b[b_base + 3];
    }
  }

  // Mask out padding channels in the last block
  const int out_logical_offset = out_cb * 4;
  const int total_logical = a_logical_channels + b_logical_channels;
  if (out_logical_offset + 0 >= total_logical) out_val.s0 = 0.0f;
  if (out_logical_offset + 1 >= total_logical) out_val.s1 = 0.0f;
  if (out_logical_offset + 2 >= total_logical) out_val.s2 = 0.0f;
  if (out_logical_offset + 3 >= total_logical) out_val.s3 = 0.0f;

  output[out_base + 0] = out_val.s0;
  output[out_base + 1] = out_val.s1;
  output[out_base + 2] = out_val.s2;
  output[out_base + 3] = out_val.s3;
}

// Crop: extract a spatial region from an NHWC4 tensor.
// output[n, y, y, c_block, c4] = input[n, y + y_off, x + x_off, c_block, c4]
__kernel void opencl_nn_crop_nhwc4(
    __global const float* input,
    __global float*       output,
    const int             in_channel_blocks,
    const int             in_width,
    const int             crop_y,
    const int             crop_x,
    const int             out_height,
    const int             out_width,
    const int             out_channel_blocks) {
  const int x   = get_global_id(0);
  const int y   = get_global_id(1);
  const int n_cb = get_global_id(2);  // batch * channel_blocks
  if (x >= out_width || y >= out_height) return;

  const int cb       = n_cb % out_channel_blocks;
  const int batch    = n_cb / out_channel_blocks;
  const int in_y     = y + crop_y;
  const int in_x     = x + crop_x;
  const int in_idx   = ((batch * in_width * in_channel_blocks +
                         in_y * in_channel_blocks + cb) * in_width + in_x) * 4;
  const int out_idx  = ((batch * out_width * out_channel_blocks +
                         y * out_channel_blocks + cb) * out_width + x) * 4;

  // Simplified: just copy channels
  const int in_spatial_base  = (batch * (in_width * in_channel_blocks * 4) +
                                in_y * in_channel_blocks * 4 + cb * 4);
  const int out_spatial_base = (batch * (out_width * out_channel_blocks * 4) +
                                y * out_channel_blocks * 4 + cb * 4);

  // For the simple 1D dispatch we use a flat indexing
  const int flat_in  = batch * in_width * in_channel_blocks * 4 +
                       in_y * in_channel_blocks * 4 + cb * 4;
  const int flat_out = batch * out_width * out_channel_blocks * 4 +
                       y * out_channel_blocks * 4 + cb * 4;

  // 4 channels per block
  for (int c = 0; c < 4; ++c) {
    output[flat_out + c] = input[flat_in + c];
  }
}

// Slice: extract a range of channels from an NHWC4 tensor.
__kernel void opencl_nn_slice_channels_nhwc4(
    __global const float* input,
    __global float*       output,
    const int             spatial_size,
    const int             in_channel_blocks,
    const int             out_channel_blocks,
    const int             start_channel_block,
    const int             start_channel_offset,
    const int             out_logical_channels) {
  const int spatial_idx = get_global_id(0);
  const int out_cb      = get_global_id(1);
  if (spatial_idx >= spatial_size || out_cb >= out_channel_blocks) return;

  // Calculate the source channel block and offset
  const int out_ch_start = out_cb * 4;
  const int in_ch_start  = start_channel_block * 4 + start_channel_offset + out_ch_start;
  const int in_cb        = in_ch_start / 4;
  const int in_co        = in_ch_start % 4;

  float4 val = (float4)(0.0f);
  if (in_cb < in_channel_blocks) {
    const int in_base = spatial_idx * in_channel_blocks * 4 + in_cb * 4;
    // Copy channels one by one with potential striding
    for (int i = 0; i < 4; ++i) {
      const int src_ch = in_co + i;
      const int src_cb = (in_cb * 4 + src_ch) / 4;
      const int src_co = (in_cb * 4 + src_ch) % 4;
      if (src_cb < in_channel_blocks && (out_ch_start + i) < out_logical_channels) {
        const int src_base = spatial_idx * in_channel_blocks * 4 + src_cb * 4;
        switch (i) {
          case 0: val.s0 = input[src_base + src_co]; break;
          case 1: val.s1 = input[src_base + src_co]; break;
          case 2: val.s2 = input[src_base + src_co]; break;
          case 3: val.s3 = input[src_base + src_co]; break;
        }
      }
    }
  }

  // Mask padding
  if (out_ch_start + 0 >= out_logical_channels) val.s0 = 0.0f;
  if (out_ch_start + 1 >= out_logical_channels) val.s1 = 0.0f;
  if (out_ch_start + 2 >= out_logical_channels) val.s2 = 0.0f;
  if (out_ch_start + 3 >= out_logical_channels) val.s3 = 0.0f;

  const int out_base = spatial_idx * out_channel_blocks * 4 + out_cb * 4;
  output[out_base + 0] = val.s0;
  output[out_base + 1] = val.s1;
  output[out_base + 2] = val.s2;
  output[out_base + 3] = val.s3;
}

#endif  // ALCEDO_OPENCL_NN_LAYOUT_CL
