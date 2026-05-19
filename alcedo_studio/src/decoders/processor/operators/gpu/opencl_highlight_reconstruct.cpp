//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifdef HAVE_OPENCL

#include "decoders/processor/operators/gpu/opencl_highlight_reconstruct.hpp"

#include <algorithm>
#include <array>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <stdexcept>
#include <vector>

#include "image/opencl_image.hpp"
#include "opencl/opencl_context.hpp"
#include "opencl/opencl_program_library.hpp"

namespace alcedo {
namespace OpenCL {
namespace {

constexpr float kHilightMagic = 0.987f;
constexpr int   kMaskPlanes   = 8;

struct HighlightCorrectionParams {
  float    clips[4];
  float    clipdark[4];
  float    chrominance[4];
  uint32_t width;
  uint32_t height;
  uint32_t stride;
};

void CheckOpenCl(cl_int err, const char* operation) {
  if (err != CL_SUCCESS) {
    throw std::runtime_error(std::string("OpenCL HighlightReconstruct: ") + operation +
                             " failed with error " + std::to_string(err) + ".");
  }
}

auto RoundUpToMultiple(uint32_t value, uint32_t multiple) -> uint32_t {
  return ((value + multiple - 1) / multiple) * multiple;
}

void DispatchKernel(cl_kernel kernel, uint32_t width, uint32_t height) {
  auto&    context   = OpenClContext::Instance();
  uint32_t local_x   = 16;
  uint32_t local_y   = 16;
  size_t   global[2] = {RoundUpToMultiple(width, local_x), RoundUpToMultiple(height, local_y)};
  size_t   local[2]  = {local_x, local_y};

  cl_int err = clEnqueueNDRangeKernel(context.Queue(), kernel, 2, nullptr, global, local, 0,
                                      nullptr, nullptr);
  CheckOpenCl(err, "clEnqueueNDRangeKernel");
}

}  // namespace

void HighlightReconstruct(opencl::OpenClImage& img, LibRaw& raw_processor) {
  if (img.Empty()) {
    throw std::runtime_error("OpenCL HighlightReconstruct: input image is empty.");
  }
  if (img.Type() != CV_32FC4) {
    throw std::runtime_error("OpenCL HighlightReconstruct: expected CV_32FC4 RGBA input.");
  }

  const uint32_t width  = static_cast<uint32_t>(img.Width());
  const uint32_t height = static_cast<uint32_t>(img.Height());
  if (width == 0 || height == 0) {
    return;
  }

  const size_t   row_bytes   = img.RowBytes();
  const size_t   buffer_size = row_bytes * height;
  const uint32_t stride      = static_cast<uint32_t>(row_bytes / (sizeof(float) * 4U));
  const auto     size        = static_cast<size_t>(width) * height;
  const auto     mask_size   = static_cast<size_t>(kMaskPlanes) * size * sizeof(uint8_t);

  HighlightCorrectionParams params = {};
  const float*              cam_mul = raw_processor.imgdata.color.cam_mul;
  const float               green   = std::max(cam_mul[1], 1e-6f);
  params.clips[0]                   = kHilightMagic * (cam_mul[0] / green);
  params.clips[1]                   = kHilightMagic;
  params.clips[2]                   = kHilightMagic * (cam_mul[2] / green);
  params.clipdark[0]                = 0.03f * params.clips[0];
  params.clipdark[1]                = 0.125f * params.clips[1];
  params.clipdark[2]                = 0.03f * params.clips[2];
  params.width                      = width;
  params.height                     = height;
  params.stride                     = stride;

  auto& context = OpenClContext::Instance();
  if (!context.IsInitialized()) {
    context.Initialize();
  }

  cl_int err = CL_SUCCESS;

  // Allocate intermediate buffers.
  cl_mem mask_buffer =
      clCreateBuffer(context.Context(), CL_MEM_READ_WRITE, mask_size, nullptr, &err);
  CheckOpenCl(err, "clCreateBuffer(mask)");
  cl_mem dilated_mask_buffer =
      clCreateBuffer(context.Context(), CL_MEM_READ_WRITE, mask_size, nullptr, &err);
  CheckOpenCl(err, "clCreateBuffer(dilated_mask)");
  cl_mem sums_buffer =
      clCreateBuffer(context.Context(), CL_MEM_READ_WRITE, sizeof(float) * 4, nullptr, &err);
  CheckOpenCl(err, "clCreateBuffer(sums)");
  cl_mem cnts_buffer =
      clCreateBuffer(context.Context(), CL_MEM_READ_WRITE, sizeof(float) * 4, nullptr, &err);
  CheckOpenCl(err, "clCreateBuffer(cnts)");
  cl_mem anyclipped_buffer =
      clCreateBuffer(context.Context(), CL_MEM_READ_WRITE, sizeof(cl_int), nullptr, &err);
  CheckOpenCl(err, "clCreateBuffer(anyclipped)");

  const uint8_t zero_u8 = 0;
  err = clEnqueueFillBuffer(context.Queue(), mask_buffer, &zero_u8, sizeof(uint8_t), 0, mask_size,
                            0, nullptr, nullptr);
  CheckOpenCl(err, "clEnqueueFillBuffer(mask)");
  err = clEnqueueFillBuffer(context.Queue(), dilated_mask_buffer, &zero_u8, sizeof(uint8_t), 0,
                            mask_size, 0, nullptr, nullptr);
  CheckOpenCl(err, "clEnqueueFillBuffer(dilated_mask)");
  const float zero_f = 0.0f;
  err = clEnqueueFillBuffer(context.Queue(), sums_buffer, &zero_f, sizeof(float), 0,
                            sizeof(float) * 4, 0, nullptr, nullptr);
  CheckOpenCl(err, "clEnqueueFillBuffer(sums)");
  err = clEnqueueFillBuffer(context.Queue(), cnts_buffer, &zero_f, sizeof(float), 0,
                            sizeof(float) * 4, 0, nullptr, nullptr);
  CheckOpenCl(err, "clEnqueueFillBuffer(cnts)");
  const cl_int zero_i = 0;
  err = clEnqueueFillBuffer(context.Queue(), anyclipped_buffer, &zero_i, sizeof(cl_int), 0,
                            sizeof(cl_int), 0, nullptr, nullptr);
  CheckOpenCl(err, "clEnqueueFillBuffer(anyclipped)");

  cl_program program = OpenClProgramLibrary::Instance().GetProgram("raw_processor_highlight");

  cl_mem input_buffer = img.Buffer();

  // --- hlr_build_mask ---
  {
    cl_kernel kernel = clCreateKernel(program, "hlr_build_mask", &err);
    CheckOpenCl(err, "clCreateKernel(hlr_build_mask)");
    err = clSetKernelArg(kernel, 0, sizeof(cl_mem), &input_buffer);
    CheckOpenCl(err, "clSetKernelArg(build_mask,0)");
    err = clSetKernelArg(kernel, 1, sizeof(cl_mem), &mask_buffer);
    CheckOpenCl(err, "clSetKernelArg(build_mask,1)");
    err = clSetKernelArg(kernel, 2, sizeof(cl_mem), &anyclipped_buffer);
    CheckOpenCl(err, "clSetKernelArg(build_mask,2)");
    err = clSetKernelArg(kernel, 3, sizeof(params), &params);
    CheckOpenCl(err, "clSetKernelArg(build_mask,3)");
    DispatchKernel(kernel, width, height);
    clReleaseKernel(kernel);
  }

  // --- hlr_dilate_mask ---
  {
    cl_kernel kernel = clCreateKernel(program, "hlr_dilate_mask", &err);
    CheckOpenCl(err, "clCreateKernel(hlr_dilate_mask)");
    err = clSetKernelArg(kernel, 0, sizeof(cl_mem), &mask_buffer);
    CheckOpenCl(err, "clSetKernelArg(dilate_mask,0)");
    err = clSetKernelArg(kernel, 1, sizeof(cl_mem), &dilated_mask_buffer);
    CheckOpenCl(err, "clSetKernelArg(dilate_mask,1)");
    err = clSetKernelArg(kernel, 2, sizeof(params), &params);
    CheckOpenCl(err, "clSetKernelArg(dilate_mask,2)");
    DispatchKernel(kernel, width, height);
    clReleaseKernel(kernel);
  }

  // --- hlr_chrominance_contrib ---
  {
    cl_kernel kernel = clCreateKernel(program, "hlr_chrominance_contrib", &err);
    CheckOpenCl(err, "clCreateKernel(hlr_chrominance_contrib)");
    err = clSetKernelArg(kernel, 0, sizeof(cl_mem), &input_buffer);
    CheckOpenCl(err, "clSetKernelArg(chrominance_contrib,0)");
    err = clSetKernelArg(kernel, 1, sizeof(cl_mem), &dilated_mask_buffer);
    CheckOpenCl(err, "clSetKernelArg(chrominance_contrib,1)");
    err = clSetKernelArg(kernel, 2, sizeof(cl_mem), &sums_buffer);
    CheckOpenCl(err, "clSetKernelArg(chrominance_contrib,2)");
    err = clSetKernelArg(kernel, 3, sizeof(cl_mem), &cnts_buffer);
    CheckOpenCl(err, "clSetKernelArg(chrominance_contrib,3)");
    err = clSetKernelArg(kernel, 4, sizeof(params), &params);
    CheckOpenCl(err, "clSetKernelArg(chrominance_contrib,4)");
    DispatchKernel(kernel, width, height);
    clReleaseKernel(kernel);
  }

  err = clFinish(context.Queue());
  CheckOpenCl(err, "clFinish after chrominance_contrib");

  cl_int anyclipped = 0;
  err = clEnqueueReadBuffer(context.Queue(), anyclipped_buffer, CL_TRUE, 0, sizeof(cl_int),
                            &anyclipped, 0, nullptr, nullptr);
  CheckOpenCl(err, "clEnqueueReadBuffer(anyclipped)");
  if (anyclipped == 0) {
    clReleaseMemObject(mask_buffer);
    clReleaseMemObject(dilated_mask_buffer);
    clReleaseMemObject(sums_buffer);
    clReleaseMemObject(cnts_buffer);
    clReleaseMemObject(anyclipped_buffer);
    return;
  }

  std::array<float, 4> sums = {0.0f, 0.0f, 0.0f, 0.0f};
  std::array<float, 4> cnts = {0.0f, 0.0f, 0.0f, 0.0f};

  err = clEnqueueReadBuffer(context.Queue(), sums_buffer, CL_TRUE, 0, sizeof(float) * 4,
                            sums.data(), 0, nullptr, nullptr);
  CheckOpenCl(err, "clEnqueueReadBuffer(sums)");
  err = clEnqueueReadBuffer(context.Queue(), cnts_buffer, CL_TRUE, 0, sizeof(float) * 4,
                            cnts.data(), 0, nullptr, nullptr);
  CheckOpenCl(err, "clEnqueueReadBuffer(cnts)");

  for (int c = 0; c < 3; ++c) {
    params.chrominance[c] = (cnts[c] > 0.0f) ? (sums[c] / cnts[c]) : 0.0f;
  }

  // Allocate output buffer.
  opencl::OpenClImage output_img;
  output_img.Create(static_cast<int>(width), static_cast<int>(height), CV_32FC4);
  cl_mem output_buffer = output_img.Buffer();

  // --- hlr_reconstruct ---
  {
    cl_kernel kernel = clCreateKernel(program, "hlr_reconstruct", &err);
    CheckOpenCl(err, "clCreateKernel(hlr_reconstruct)");
    err = clSetKernelArg(kernel, 0, sizeof(cl_mem), &input_buffer);
    CheckOpenCl(err, "clSetKernelArg(reconstruct,0)");
    err = clSetKernelArg(kernel, 1, sizeof(cl_mem), &output_buffer);
    CheckOpenCl(err, "clSetKernelArg(reconstruct,1)");
    err = clSetKernelArg(kernel, 2, sizeof(params), &params);
    CheckOpenCl(err, "clSetKernelArg(reconstruct,2)");
    DispatchKernel(kernel, width, height);
    clReleaseKernel(kernel);
  }

  err = clFinish(context.Queue());
  CheckOpenCl(err, "clFinish after reconstruct");

  clReleaseMemObject(mask_buffer);
  clReleaseMemObject(dilated_mask_buffer);
  clReleaseMemObject(sums_buffer);
  clReleaseMemObject(cnts_buffer);
  clReleaseMemObject(anyclipped_buffer);

  img = std::move(output_img);
}

}  // namespace OpenCL
}  // namespace alcedo

#endif
