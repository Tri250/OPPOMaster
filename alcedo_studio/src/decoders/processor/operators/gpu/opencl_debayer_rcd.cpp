//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifdef HAVE_OPENCL

#include "decoders/processor/operators/gpu/opencl_debayer_rcd.hpp"

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <stdexcept>

#include "image/opencl_image.hpp"
#include "opencl/opencl_context.hpp"
#include "opencl/opencl_program_library.hpp"

namespace alcedo {
namespace OpenCL {
namespace {

struct SinglePlaneParams {
  uint32_t width;
  uint32_t height;
  uint32_t stride;
  uint32_t rgb_fc[4];
};

struct MergeParams {
  uint32_t width;
  uint32_t height;
  uint32_t plane_stride;
  uint32_t rgba_stride;
};

void CheckOpenCl(cl_int err, const char* operation) {
  if (err != CL_SUCCESS) {
    throw std::runtime_error(std::string("OpenCL Debayer RCD: ") + operation +
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

void Bayer2x2ToRGB_RCD(opencl::OpenClImage& image, const BayerPattern2x2& pattern) {
  if (image.Empty()) {
    throw std::runtime_error("OpenCL Debayer RCD: input image is empty.");
  }
  if (image.Type() != CV_32FC1) {
    throw std::runtime_error("OpenCL Debayer RCD: expected CV_32FC1 Bayer input.");
  }

  const uint32_t in_width  = static_cast<uint32_t>(image.Width());
  const uint32_t in_height = static_cast<uint32_t>(image.Height());
  if (in_width == 0 || in_height == 0) {
    return;
  }

  const int out_width  = std::max(0, static_cast<int>(in_width) - 8);
  const int out_height = std::max(0, static_cast<int>(in_height) - 8);
  if (out_width <= 0 || out_height <= 0) {
    throw std::runtime_error("OpenCL Debayer RCD: image too small for RCD radius.");
  }

  auto& context = OpenClContext::Instance();
  if (!context.IsInitialized()) {
    context.Initialize();
  }

  // Allocate intermediate single-plane buffers at input resolution.
  opencl::OpenClImage r_img, g_img, b_img, vh_img, pq_img;
  r_img.Create(static_cast<int>(in_width), static_cast<int>(in_height), CV_32FC1);
  g_img.Create(static_cast<int>(in_width), static_cast<int>(in_height), CV_32FC1);
  b_img.Create(static_cast<int>(in_width), static_cast<int>(in_height), CV_32FC1);
  vh_img.Create(static_cast<int>(in_width), static_cast<int>(in_height), CV_32FC1);
  pq_img.Create(static_cast<int>(in_width), static_cast<int>(in_height), CV_32FC1);

  // Output RGBA buffer at the cropped resolution (RCD invalidates a 4-pixel border band).
  opencl::OpenClImage out_img;
  out_img.Create(out_width, out_height, CV_32FC4);

  const SinglePlaneParams plane_params{
      .width  = in_width,
      .height = in_height,
      .stride = in_width,
      .rgb_fc = {static_cast<uint32_t>(pattern.rgb_fc[0]),
                 static_cast<uint32_t>(pattern.rgb_fc[1]),
                 static_cast<uint32_t>(pattern.rgb_fc[2]),
                 static_cast<uint32_t>(pattern.rgb_fc[3])},
  };

  const MergeParams merge_params{
      .width        = in_width,
      .height       = in_height,
      .plane_stride = in_width,
      .rgba_stride  = static_cast<uint32_t>(out_width),
  };

  cl_program program = OpenClProgramLibrary::Instance().GetProgram("raw_processor_debayer_rcd");
  cl_int     err     = CL_SUCCESS;

  // Helper lambdas to get stable l-values for buffer handles.
  cl_mem raw_buf  = image.Buffer();
  cl_mem r_buf    = r_img.Buffer();
  cl_mem g_buf    = g_img.Buffer();
  cl_mem b_buf    = b_img.Buffer();
  cl_mem vh_buf   = vh_img.Buffer();
  cl_mem pq_buf   = pq_img.Buffer();
  cl_mem out_buf  = out_img.Buffer();

  // Upload params through small cl_mem buffers to avoid struct-layout mismatches.
  cl_mem plane_params_buf =
      clCreateBuffer(context.Context(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                     sizeof(plane_params), const_cast<SinglePlaneParams*>(&plane_params), &err);
  CheckOpenCl(err, "clCreateBuffer(plane_params)");
  cl_mem merge_params_buf =
      clCreateBuffer(context.Context(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                     sizeof(merge_params), const_cast<MergeParams*>(&merge_params), &err);
  CheckOpenCl(err, "clCreateBuffer(merge_params)");

  // --- rcd_init_and_vh ---
  cl_kernel k0 = clCreateKernel(program, "rcd_init_and_vh", &err);
  CheckOpenCl(err, "clCreateKernel(init_and_vh)");
  err = clSetKernelArg(k0, 0, sizeof(cl_mem), &raw_buf);
  CheckOpenCl(err, "clSetKernelArg(k0,0)");
  err = clSetKernelArg(k0, 1, sizeof(cl_mem), &r_buf);
  CheckOpenCl(err, "clSetKernelArg(k0,1)");
  err = clSetKernelArg(k0, 2, sizeof(cl_mem), &g_buf);
  CheckOpenCl(err, "clSetKernelArg(k0,2)");
  err = clSetKernelArg(k0, 3, sizeof(cl_mem), &b_buf);
  CheckOpenCl(err, "clSetKernelArg(k0,3)");
  err = clSetKernelArg(k0, 4, sizeof(cl_mem), &vh_buf);
  CheckOpenCl(err, "clSetKernelArg(k0,4)");
  err = clSetKernelArg(k0, 5, sizeof(cl_mem), &plane_params_buf);
  CheckOpenCl(err, "clSetKernelArg(k0,5)");
  DispatchKernel(k0, in_width, in_height);
  clReleaseKernel(k0);

  // --- rcd_green_at_rb ---
  cl_kernel k1 = clCreateKernel(program, "rcd_green_at_rb", &err);
  CheckOpenCl(err, "clCreateKernel(green_at_rb)");
  err = clSetKernelArg(k1, 0, sizeof(cl_mem), &raw_buf);
  CheckOpenCl(err, "clSetKernelArg(k1,0)");
  err = clSetKernelArg(k1, 1, sizeof(cl_mem), &vh_buf);
  CheckOpenCl(err, "clSetKernelArg(k1,1)");
  err = clSetKernelArg(k1, 2, sizeof(cl_mem), &g_buf);
  CheckOpenCl(err, "clSetKernelArg(k1,2)");
  err = clSetKernelArg(k1, 3, sizeof(cl_mem), &plane_params_buf);
  CheckOpenCl(err, "clSetKernelArg(k1,3)");
  DispatchKernel(k1, in_width, in_height);
  clReleaseKernel(k1);

  // --- rcd_pq_dir ---
  cl_kernel k2 = clCreateKernel(program, "rcd_pq_dir", &err);
  CheckOpenCl(err, "clCreateKernel(pq_dir)");
  err = clSetKernelArg(k2, 0, sizeof(cl_mem), &raw_buf);
  CheckOpenCl(err, "clSetKernelArg(k2,0)");
  err = clSetKernelArg(k2, 1, sizeof(cl_mem), &pq_buf);
  CheckOpenCl(err, "clSetKernelArg(k2,1)");
  err = clSetKernelArg(k2, 2, sizeof(cl_mem), &plane_params_buf);
  CheckOpenCl(err, "clSetKernelArg(k2,2)");
  DispatchKernel(k2, in_width, in_height);
  clReleaseKernel(k2);

  // --- rcd_rb_at_rb ---
  cl_kernel k3 = clCreateKernel(program, "rcd_rb_at_rb", &err);
  CheckOpenCl(err, "clCreateKernel(rb_at_rb)");
  err = clSetKernelArg(k3, 0, sizeof(cl_mem), &pq_buf);
  CheckOpenCl(err, "clSetKernelArg(k3,0)");
  err = clSetKernelArg(k3, 1, sizeof(cl_mem), &g_buf);
  CheckOpenCl(err, "clSetKernelArg(k3,1)");
  err = clSetKernelArg(k3, 2, sizeof(cl_mem), &r_buf);
  CheckOpenCl(err, "clSetKernelArg(k3,2)");
  err = clSetKernelArg(k3, 3, sizeof(cl_mem), &b_buf);
  CheckOpenCl(err, "clSetKernelArg(k3,3)");
  err = clSetKernelArg(k3, 4, sizeof(cl_mem), &plane_params_buf);
  CheckOpenCl(err, "clSetKernelArg(k3,4)");
  DispatchKernel(k3, in_width, in_height);
  clReleaseKernel(k3);

  // --- rcd_rb_at_g ---
  cl_kernel k4 = clCreateKernel(program, "rcd_rb_at_g", &err);
  CheckOpenCl(err, "clCreateKernel(rb_at_g)");
  err = clSetKernelArg(k4, 0, sizeof(cl_mem), &vh_buf);
  CheckOpenCl(err, "clSetKernelArg(k4,0)");
  err = clSetKernelArg(k4, 1, sizeof(cl_mem), &g_buf);
  CheckOpenCl(err, "clSetKernelArg(k4,1)");
  err = clSetKernelArg(k4, 2, sizeof(cl_mem), &r_buf);
  CheckOpenCl(err, "clSetKernelArg(k4,2)");
  err = clSetKernelArg(k4, 3, sizeof(cl_mem), &b_buf);
  CheckOpenCl(err, "clSetKernelArg(k4,3)");
  err = clSetKernelArg(k4, 4, sizeof(cl_mem), &plane_params_buf);
  CheckOpenCl(err, "clSetKernelArg(k4,4)");
  DispatchKernel(k4, in_width, in_height);
  clReleaseKernel(k4);

  // --- rcd_merge_rgba ---
  cl_kernel k5 = clCreateKernel(program, "rcd_merge_rgba", &err);
  CheckOpenCl(err, "clCreateKernel(merge_rgba)");
  err = clSetKernelArg(k5, 0, sizeof(cl_mem), &r_buf);
  CheckOpenCl(err, "clSetKernelArg(k5,0)");
  err = clSetKernelArg(k5, 1, sizeof(cl_mem), &g_buf);
  CheckOpenCl(err, "clSetKernelArg(k5,1)");
  err = clSetKernelArg(k5, 2, sizeof(cl_mem), &b_buf);
  CheckOpenCl(err, "clSetKernelArg(k5,2)");
  err = clSetKernelArg(k5, 3, sizeof(cl_mem), &out_buf);
  CheckOpenCl(err, "clSetKernelArg(k5,3)");
  err = clSetKernelArg(k5, 4, sizeof(cl_mem), &plane_params_buf);
  CheckOpenCl(err, "clSetKernelArg(k5,4)");
  err = clSetKernelArg(k5, 5, sizeof(cl_mem), &merge_params_buf);
  CheckOpenCl(err, "clSetKernelArg(k5,5)");
  DispatchKernel(k5, in_width, in_height);
  clReleaseKernel(k5);

  clReleaseMemObject(plane_params_buf);
  clReleaseMemObject(merge_params_buf);

  err = clFinish(context.Queue());
  CheckOpenCl(err, "clFinish");

  image = std::move(out_img);
}

}  // namespace OpenCL
}  // namespace alcedo

#endif
