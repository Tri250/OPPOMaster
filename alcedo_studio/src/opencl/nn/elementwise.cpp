//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifdef HAVE_OPENCL

#include "opencl/nn/elementwise.hpp"

#include <algorithm>
#include <stdexcept>
#include <string>

#include "opencl/opencl_context.hpp"
#include "opencl/opencl_program_library.hpp"
#include "opencl/opencl_geometry_programs.hpp"

namespace alcedo::opencl::nn {
namespace {

constexpr const char* kNnProgramName = "opencl_nn_elementwise";

template <typename T>
void SetArg(cl_kernel kernel, cl_uint index, const T& value, const char* what) {
  CheckOpenCl(clSetKernelArg(kernel, index, sizeof(T), &value), what);
}

void Enqueue1D(cl_kernel kernel, size_t count, cl_command_queue queue, const char* what) {
  if (count == 0) return;
  CheckOpenCl(clEnqueueNDRangeKernel(queue, kernel, 1, nullptr, &count, nullptr,
                                     0, nullptr, nullptr),
              what);
  CheckOpenCl(clFinish(queue), what);
}

void Enqueue2D(cl_kernel kernel, size_t gx, size_t gy, cl_command_queue queue, const char* what) {
  if (gx == 0 || gy == 0) return;
  const size_t global[2] = {gx, gy};
  CheckOpenCl(clEnqueueNDRangeKernel(queue, kernel, 2, nullptr, global, nullptr,
                                     0, nullptr, nullptr),
              what);
  CheckOpenCl(clFinish(queue), what);
}

auto TotalElements(const Nhwc4TensorView& v) -> int {
  return v.batch * v.height * v.width * v.channel_blocks * 4;
}

void ValidateSameSize(const Nhwc4TensorView& a, const Nhwc4TensorView& b, const char* what) {
  if (a.batch != b.batch || a.height != b.height || a.width != b.width ||
      a.logical_channels != b.logical_channels) {
    throw std::runtime_error(std::string(what) + ": tensor dimensions don't match");
  }
}

}  // namespace

void Relu(const Nhwc4TensorView& input, Nhwc4TensorView& output, cl_command_queue queue) {
  cl_int err = CL_SUCCESS;
  cl_kernel kernel = clCreateKernel(
      OpenClProgramLibrary::Instance().GetProgram(kNnProgramName),
      "opencl_nn_relu_nhwc4", &err);
  CheckOpenCl(err, "Relu: clCreateKernel");

  const int total = TotalElements(input);
  SetArg(kernel, 0, input.buffer, "Relu: set input");
  SetArg(kernel, 1, output.buffer, "Relu: set output");
  SetArg(kernel, 2, total, "Relu: set total");
  Enqueue1D(kernel, static_cast<size_t>(total), queue, "Relu: enqueue");
  clReleaseKernel(kernel);
}

void LeakyRelu(const Nhwc4TensorView& input, Nhwc4TensorView& output,
               float alpha, cl_command_queue queue) {
  cl_int err = CL_SUCCESS;
  cl_kernel kernel = clCreateKernel(
      OpenClProgramLibrary::Instance().GetProgram(kNnProgramName),
      "opencl_nn_leaky_relu_nhwc4", &err);
  CheckOpenCl(err, "LeakyRelu: clCreateKernel");

  const int total = TotalElements(input);
  SetArg(kernel, 0, input.buffer, "LeakyRelu: set input");
  SetArg(kernel, 1, output.buffer, "LeakyRelu: set output");
  SetArg(kernel, 2, total, "LeakyRelu: set total");
  SetArg(kernel, 3, alpha, "LeakyRelu: set alpha");
  Enqueue1D(kernel, static_cast<size_t>(total), queue, "LeakyRelu: enqueue");
  clReleaseKernel(kernel);
}

void Mul(const Nhwc4TensorView& a, const Nhwc4TensorView& b,
         Nhwc4TensorView& output, cl_command_queue queue) {
  ValidateSameSize(a, b, "Mul");

  cl_int err = CL_SUCCESS;
  cl_kernel kernel = clCreateKernel(
      OpenClProgramLibrary::Instance().GetProgram(kNnProgramName),
      "opencl_nn_mul_nhwc4", &err);
  CheckOpenCl(err, "Mul: clCreateKernel");

  const int total = TotalElements(a);
  SetArg(kernel, 0, a.buffer, "Mul: set a");
  SetArg(kernel, 1, b.buffer, "Mul: set b");
  SetArg(kernel, 2, output.buffer, "Mul: set output");
  SetArg(kernel, 3, total, "Mul: set total");
  Enqueue1D(kernel, static_cast<size_t>(total), queue, "Mul: enqueue");
  clReleaseKernel(kernel);
}

void MulScalar(const Nhwc4TensorView& input, Nhwc4TensorView& output,
               float scalar, cl_command_queue queue) {
  cl_int err = CL_SUCCESS;
  cl_kernel kernel = clCreateKernel(
      OpenClProgramLibrary::Instance().GetProgram(kNnProgramName),
      "opencl_nn_mul_scalar_nhwc4", &err);
  CheckOpenCl(err, "MulScalar: clCreateKernel");

  const int total = TotalElements(input);
  SetArg(kernel, 0, input.buffer, "MulScalar: set input");
  SetArg(kernel, 1, output.buffer, "MulScalar: set output");
  SetArg(kernel, 2, total, "MulScalar: set total");
  SetArg(kernel, 3, scalar, "MulScalar: set scalar");
  Enqueue1D(kernel, static_cast<size_t>(total), queue, "MulScalar: enqueue");
  clReleaseKernel(kernel);
}

void Sigmoid(const Nhwc4TensorView& input, Nhwc4TensorView& output, cl_command_queue queue) {
  cl_int err = CL_SUCCESS;
  cl_kernel kernel = clCreateKernel(
      OpenClProgramLibrary::Instance().GetProgram(kNnProgramName),
      "opencl_nn_sigmoid_nhwc4", &err);
  CheckOpenCl(err, "Sigmoid: clCreateKernel");

  const int total = TotalElements(input);
  SetArg(kernel, 0, input.buffer, "Sigmoid: set input");
  SetArg(kernel, 1, output.buffer, "Sigmoid: set output");
  SetArg(kernel, 2, total, "Sigmoid: set total");
  Enqueue1D(kernel, static_cast<size_t>(total), queue, "Sigmoid: enqueue");
  clReleaseKernel(kernel);
}

void Tanh(const Nhwc4TensorView& input, Nhwc4TensorView& output, cl_command_queue queue) {
  cl_int err = CL_SUCCESS;
  cl_kernel kernel = clCreateKernel(
      OpenClProgramLibrary::Instance().GetProgram(kNnProgramName),
      "opencl_nn_tanh_nhwc4", &err);
  CheckOpenCl(err, "Tanh: clCreateKernel");

  const int total = TotalElements(input);
  SetArg(kernel, 0, input.buffer, "Tanh: set input");
  SetArg(kernel, 1, output.buffer, "Tanh: set output");
  SetArg(kernel, 2, total, "Tanh: set total");
  Enqueue1D(kernel, static_cast<size_t>(total), queue, "Tanh: enqueue");
  clReleaseKernel(kernel);
}

void ConcatChannels(const Nhwc4TensorView& a, const Nhwc4TensorView& b,
                    Nhwc4TensorView& output, cl_command_queue queue) {
  if (a.batch != b.batch || a.height != b.height || a.width != b.width) {
    throw std::runtime_error("ConcatChannels: spatial dimensions don't match");
  }

  cl_int err = CL_SUCCESS;
  cl_kernel kernel = clCreateKernel(
      OpenClProgramLibrary::Instance().GetProgram("opencl_nn_layout"),
      "opencl_nn_concat_channels_nhwc4", &err);
  CheckOpenCl(err, "ConcatChannels: clCreateKernel");

  const int spatial_size = a.batch * a.height * a.width;
  SetArg(kernel, 0, a.buffer, "ConcatChannels: set a");
  SetArg(kernel, 1, b.buffer, "ConcatChannels: set b");
  SetArg(kernel, 2, output.buffer, "ConcatChannels: set output");
  SetArg(kernel, 3, spatial_size, "ConcatChannels: set spatial_size");
  SetArg(kernel, 4, a.channel_blocks, "ConcatChannels: set a_channel_blocks");
  SetArg(kernel, 5, b.channel_blocks, "ConcatChannels: set b_channel_blocks");
  SetArg(kernel, 6, output.channel_blocks, "ConcatChannels: set out_channel_blocks");
  SetArg(kernel, 7, a.logical_channels, "ConcatChannels: set a_logical_channels");
  SetArg(kernel, 8, b.logical_channels, "ConcatChannels: set b_logical_channels");

  const size_t global[2] = {static_cast<size_t>(spatial_size),
                            static_cast<size_t>(output.channel_blocks)};
  CheckOpenCl(clEnqueueNDRangeKernel(queue, kernel, 2, nullptr, global, nullptr,
                                     0, nullptr, nullptr),
              "ConcatChannels: enqueue");
  CheckOpenCl(clFinish(queue), "ConcatChannels: finish");
  clReleaseKernel(kernel);
}

void Crop(const Nhwc4TensorView& input, Nhwc4TensorView& output,
          int crop_y, int crop_x, cl_command_queue queue) {
  cl_int err = CL_SUCCESS;
  cl_kernel kernel = clCreateKernel(
      OpenClProgramLibrary::Instance().GetProgram("opencl_nn_layout"),
      "opencl_nn_crop_nhwc4", &err);
  CheckOpenCl(err, "Crop: clCreateKernel");

  SetArg(kernel, 0, input.buffer, "Crop: set input");
  SetArg(kernel, 1, output.buffer, "Crop: set output");
  SetArg(kernel, 2, input.channel_blocks, "Crop: set in_channel_blocks");
  SetArg(kernel, 3, input.width, "Crop: set in_width");
  SetArg(kernel, 4, crop_y, "Crop: set crop_y");
  SetArg(kernel, 5, crop_x, "Crop: set crop_x");
  SetArg(kernel, 6, output.height, "Crop: set out_height");
  SetArg(kernel, 7, output.width, "Crop: set out_width");
  SetArg(kernel, 8, output.channel_blocks, "Crop: set out_channel_blocks");

  const size_t global[3] = {static_cast<size_t>(output.width),
                            static_cast<size_t>(output.height),
                            static_cast<size_t>(output.batch * output.channel_blocks)};
  CheckOpenCl(clEnqueueNDRangeKernel(queue, kernel, 3, nullptr, global, nullptr,
                                     0, nullptr, nullptr),
              "Crop: enqueue");
  CheckOpenCl(clFinish(queue), "Crop: finish");
  clReleaseKernel(kernel);
}

void SliceChannels(const Nhwc4TensorView& input, Nhwc4TensorView& output,
                   int start_channel, cl_command_queue queue) {
  cl_int err = CL_SUCCESS;
  cl_kernel kernel = clCreateKernel(
      OpenClProgramLibrary::Instance().GetProgram("opencl_nn_layout"),
      "opencl_nn_slice_channels_nhwc4", &err);
  CheckOpenCl(err, "SliceChannels: clCreateKernel");

  const int spatial_size = input.batch * input.height * input.width;
  const int start_cb = start_channel / 4;
  const int start_co = start_channel % 4;

  SetArg(kernel, 0, input.buffer, "SliceChannels: set input");
  SetArg(kernel, 1, output.buffer, "SliceChannels: set output");
  SetArg(kernel, 2, spatial_size, "SliceChannels: set spatial_size");
  SetArg(kernel, 3, input.channel_blocks, "SliceChannels: set in_channel_blocks");
  SetArg(kernel, 4, output.channel_blocks, "SliceChannels: set out_channel_blocks");
  SetArg(kernel, 5, start_cb, "SliceChannels: set start_channel_block");
  SetArg(kernel, 6, start_co, "SliceChannels: set start_channel_offset");
  SetArg(kernel, 7, output.logical_channels, "SliceChannels: set out_logical_channels");

  const size_t global[2] = {static_cast<size_t>(spatial_size),
                            static_cast<size_t>(output.channel_blocks)};
  CheckOpenCl(clEnqueueNDRangeKernel(queue, kernel, 2, nullptr, global, nullptr,
                                     0, nullptr, nullptr),
              "SliceChannels: enqueue");
  CheckOpenCl(clFinish(queue), "SliceChannels: finish");
  clReleaseKernel(kernel);
}

void FusedPostOutput(const Nhwc4TensorView& input, Nhwc4TensorView& output,
                     const float* bias, int channel_blocks, float scale,
                     float min_val, float max_val, int activation_type,
                     cl_command_queue queue) {
  cl_int err = CL_SUCCESS;
  cl_kernel kernel = clCreateKernel(
      OpenClProgramLibrary::Instance().GetProgram("opencl_nn_fused_post_output"),
      "opencl_nn_fused_post_output_nhwc4", &err);
  CheckOpenCl(err, "FusedPostOutput: clCreateKernel");

  const int total = TotalElements(input);
  SetArg(kernel, 0, input.buffer, "FusedPostOutput: set input");
  SetArg(kernel, 1, output.buffer, "FusedPostOutput: set output");
  // Bias is a host pointer that needs to be a device buffer - pass as cl_mem or 0
  cl_mem bias_buffer = bias ? reinterpret_cast<cl_mem>(const_cast<float*>(bias)) : nullptr;
  SetArg(kernel, 2, bias_buffer, "FusedPostOutput: set bias");
  SetArg(kernel, 3, total, "FusedPostOutput: set total");
  SetArg(kernel, 4, channel_blocks, "FusedPostOutput: set channel_blocks");
  SetArg(kernel, 5, scale, "FusedPostOutput: set scale");
  SetArg(kernel, 6, min_val, "FusedPostOutput: set min_val");
  SetArg(kernel, 7, max_val, "FusedPostOutput: set max_val");
  SetArg(kernel, 8, activation_type, "FusedPostOutput: set activation_type");

  Enqueue1D(kernel, static_cast<size_t>(total), queue, "FusedPostOutput: enqueue");
  clReleaseKernel(kernel);
}

}  // namespace alcedo::opencl::nn

#endif  // HAVE_OPENCL
