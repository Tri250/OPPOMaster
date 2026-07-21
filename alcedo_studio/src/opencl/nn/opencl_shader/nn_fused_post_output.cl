//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifndef ALCEDO_OPENCL_NN_FUSED_POST_OUTPUT_CL
#define ALCEDO_OPENCL_NN_FUSED_POST_OUTPUT_CL

// Fused post-output processing: apply activation, scale, bias, and clamp
// in a single kernel to reduce memory bandwidth for NN inference passes.
// Common in DemosaicNet and similar architectures where the final layer
// needs: output = clamp(activation(input * scale + bias), min_val, max_val)
__kernel void opencl_nn_fused_post_output_nhwc4(
    __global const float* input,
    __global float*       output,
    __global const float* bias,        // per-channel bias, may be null
    const int             total_elements,
    const int             channel_blocks,
    const float           scale,
    const float           min_val,
    const float           max_val,
    const int             activation_type) {  // 0=none, 1=relu, 2=sigmoid, 3=tanh
  const int idx = get_global_id(0);
  if (idx >= total_elements) return;

  float val = input[idx] * scale;

  // Apply bias if provided (bias != null)
  if (bias != 0) {
    const int ch_offset = idx % (channel_blocks * 4);
    val += bias[ch_offset];
  }

  // Apply activation
  switch (activation_type) {
    case 1:  // ReLU
      val = val > 0.0f ? val : 0.0f;
      break;
    case 2:  // Sigmoid
      val = 1.0f / (1.0f + exp(-val));
      break;
    case 3:  // Tanh
      val = tanh(val);
      break;
    default:  // None
      break;
  }

  // Clamp
  val = clamp(val, min_val, max_val);
  output[idx] = val;
}

// ConvTranspose2d (deconvolution) with stride and padding for NHWC4 layout.
// This is used in DemosaicNet for upsampling stages.
__kernel void opencl_nn_conv_transpose2d_nhwc4(
    __global const float* input,
    __global const float* weights,
    __global const float* bias,
    __global float*       output,
    const int             in_height,
    const int             in_width,
    const int             in_channel_blocks,
    const int             out_height,
    const int             out_width,
    const int             out_channel_blocks,
    const int             kernel_h,
    const int             kernel_w,
    const int             stride_h,
    const int             stride_w,
    const int             pad_h,
    const int             pad_w,
    const int             batch) {
  const int ox  = get_global_id(0);
  const int oy  = get_global_id(1);
  const int ocb = get_global_id(2) % out_channel_blocks;
  const int n   = get_global_id(2) / out_channel_blocks;

  if (ox >= out_width || oy >= out_height || n >= batch) return;

  float4 sum = (float4)(0.0f);

  for (int kh = 0; kh < kernel_h; ++kh) {
    for (int kw = 0; kw < kernel_w; ++kw) {
      // Compute the input position that contributes to this output position
      const int iy = oy - kh * stride_h + pad_h;
      const int ix = ox - kw * stride_w + pad_w;

      if (iy >= 0 && iy < in_height && ix >= 0 && ix < in_width) {
        for (int icb = 0; icb < in_channel_blocks; ++icb) {
          const int in_idx = ((n * in_height + iy) * in_width + ix) * in_channel_blocks + icb;
          const float4 in_val = vload4(0, input + in_idx * 4);

          // Weight shape: [in_channel_blocks, kernel_h, kernel_w, out_channel_blocks, 4, 4]
          // Simplified indexing for the weight tensor
          const int w_base = ((icb * kernel_h + kh) * kernel_w + kw) * out_channel_blocks + ocb;
          // Each weight block is a 4x4 matrix, we access row by input channel offset
          for (int ic = 0; ic < 4; ++ic) {
            const float4 w_row = vload4(w_base * 16 + ic * 4, weights);
            const float iv = (&in_val.s0)[ic];
            sum.s0 += iv * w_row.s0;
            sum.s1 += iv * w_row.s1;
            sum.s2 += iv * w_row.s2;
            sum.s3 += iv * w_row.s3;
          }
        }
      }
    }
  }

  // Add bias
  if (bias != 0) {
    const float4 b = vload4(ocb, bias);
    sum += b;
  }

  const int out_idx = ((n * out_height + oy) * out_width + ox) * out_channel_blocks + ocb;
  vstore4(sum, 0, output + out_idx * 4);
}

#endif  // ALCEDO_OPENCL_NN_FUSED_POST_OUTPUT_CL
