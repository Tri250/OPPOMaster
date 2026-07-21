//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#ifdef HAVE_OPENCL

#ifndef CL_TARGET_OPENCL_VERSION
#define CL_TARGET_OPENCL_VERSION 120
#endif
#include <CL/cl.h>

#include "opencl/nn/common.hpp"
#include "opencl/nn/tensor_view.hpp"

namespace alcedo::opencl::nn {

// ReLU activation: output = max(0, input)
void Relu(const Nhwc4TensorView& input, Nhwc4TensorView& output, cl_command_queue queue);

// LeakyReLU: output = input > 0 ? input : alpha * input
void LeakyRelu(const Nhwc4TensorView& input, Nhwc4TensorView& output,
               float alpha, cl_command_queue queue);

// Element-wise multiply: output = a * b
void Mul(const Nhwc4TensorView& a, const Nhwc4TensorView& b,
         Nhwc4TensorView& output, cl_command_queue queue);

// Scalar multiply: output = input * scalar
void MulScalar(const Nhwc4TensorView& input, Nhwc4TensorView& output,
               float scalar, cl_command_queue queue);

// Sigmoid: output = 1 / (1 + exp(-input))
void Sigmoid(const Nhwc4TensorView& input, Nhwc4TensorView& output, cl_command_queue queue);

// Tanh: output = tanh(input)
void Tanh(const Nhwc4TensorView& input, Nhwc4TensorView& output, cl_command_queue queue);

// Concat two tensors along channel axis.
void ConcatChannels(const Nhwc4TensorView& a, const Nhwc4TensorView& b,
                    Nhwc4TensorView& output, cl_command_queue queue);

// Crop: extract spatial region.
void Crop(const Nhwc4TensorView& input, Nhwc4TensorView& output,
          int crop_y, int crop_x, cl_command_queue queue);

// Slice: extract channel range.
void SliceChannels(const Nhwc4TensorView& input, Nhwc4TensorView& output,
                   int start_channel, cl_command_queue queue);

// Fused post-output: activation + scale + bias + clamp.
void FusedPostOutput(const Nhwc4TensorView& input, Nhwc4TensorView& output,
                     const float* bias, int channel_blocks, float scale,
                     float min_val, float max_val, int activation_type,
                     cl_command_queue queue);

}  // namespace alcedo::opencl::nn

#endif  // HAVE_OPENCL
