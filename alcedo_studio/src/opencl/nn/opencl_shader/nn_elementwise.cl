//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifndef ALCEDO_OPENCL_NN_ELEMENTWISE_CL
#define ALCEDO_OPENCL_NN_ELEMENTWISE_CL

// ReLU activation: f(x) = max(0, x) for NHWC4 layout.
__kernel void opencl_nn_relu_nhwc4(
    __global const float* input,
    __global float*       output,
    const int             total_elements) {
  const int idx = get_global_id(0);
  if (idx >= total_elements) return;
  const float v = input[idx];
  output[idx] = v > 0.0f ? v : 0.0f;
}

// LeakyReLU: f(x) = x > 0 ? x : alpha * x
__kernel void opencl_nn_leaky_relu_nhwc4(
    __global const float* input,
    __global float*       output,
    const int             total_elements,
    const float           alpha) {
  const int idx = get_global_id(0);
  if (idx >= total_elements) return;
  const float v = input[idx];
  output[idx] = v > 0.0f ? v : alpha * v;
}

// Element-wise multiply: out = a * b
__kernel void opencl_nn_mul_nhwc4(
    __global const float* a,
    __global const float* b,
    __global float*       output,
    const int             total_elements) {
  const int idx = get_global_id(0);
  if (idx >= total_elements) return;
  output[idx] = a[idx] * b[idx];
}

// Element-wise multiply with scalar: out = input * scalar
__kernel void opencl_nn_mul_scalar_nhwc4(
    __global const float* input,
    __global float*       output,
    const int             total_elements,
    const float           scalar) {
  const int idx = get_global_id(0);
  if (idx >= total_elements) return;
  output[idx] = input[idx] * scalar;
}

// Sigmoid activation: f(x) = 1 / (1 + exp(-x))
__kernel void opencl_nn_sigmoid_nhwc4(
    __global const float* input,
    __global float*       output,
    const int             total_elements) {
  const int idx = get_global_id(0);
  if (idx >= total_elements) return;
  const float v = input[idx];
  output[idx] = 1.0f / (1.0f + exp(-v));
}

// Tanh activation: f(x) = tanh(x)
__kernel void opencl_nn_tanh_nhwc4(
    __global const float* input,
    __global float*       output,
    const int             total_elements) {
  const int idx = get_global_id(0);
  if (idx >= total_elements) return;
  output[idx] = tanh(input[idx]);
}

#endif  // ALCEDO_OPENCL_NN_ELEMENTWISE_CL
