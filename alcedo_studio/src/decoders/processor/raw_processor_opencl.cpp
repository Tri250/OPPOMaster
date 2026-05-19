//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "decoders/processor/raw_processor.hpp"

#ifdef HAVE_OPENCL

#include <stdexcept>

#include "decoders/processor/operators/gpu/opencl_cvt_ref_space.hpp"
#include "decoders/processor/operators/gpu/opencl_debayer_rcd.hpp"
#include "decoders/processor/operators/gpu/opencl_to_linear_ref.hpp"
#include "decoders/processor/raw_processor_internal.hpp"

namespace alcedo {

auto RawProcessor::ProcessDirectRgbOpenCL() -> ImageBuffer {
  process_buffer_.SyncToGPU(GpuBackendKind::OpenCL);
  process_buffer_.ReleaseCPUData();
  // TODO: Apply geometric corrections on OpenCL when implemented.
  // For now the buffer stays on the GPU for downstream consumption.
  return {std::move(process_buffer_)};
}

auto RawProcessor::ProcessOpenCL() -> ImageBuffer {
  if (input_kind_ == RawInputKind::DebayeredRgb) {
    return ProcessDirectRgbOpenCL();
  }

  // CPU downsample (same as Metal / CUDA paths).
  SetDecodeRes();
  process_buffer_.SyncToGPU(GpuBackendKind::OpenCL);
  process_buffer_.ReleaseCPUData();

  auto& gpu_img = process_buffer_.GetOpenClImage();
  OpenCL::ToLinearRef(gpu_img, raw_processor_, cfa_pattern_);

  if (cfa_pattern_.kind == RawCfaKind::Bayer2x2 && params_.highlights_reconstruct_) {
    throw std::runtime_error(
        "RawProcessor: OpenCL highlight reconstruction is not yet implemented.");
  } else {
    if (cfa_pattern_.kind == RawCfaKind::XTrans6x6) {
      throw std::runtime_error(
          "RawProcessor: OpenCL X-Trans interpolation is not yet implemented.");
    } else {
      OpenCL::Bayer2x2ToRGB_RCD(gpu_img, cfa_pattern_.bayer_pattern);
    }

    const cv::Rect crop_rect = detail::BuildDecodeCropRect(
        raw_data_.sizes, default_crop_,
        cv::Size(gpu_img.Width(), gpu_img.Height()), params_.decode_res_);
    if (!detail::IsFullImageRect(crop_rect, cv::Size(gpu_img.Width(), gpu_img.Height()))) {
      opencl::OpenClImage cropped;
      gpu_img.CropTo(cropped, crop_rect);
      gpu_img = std::move(cropped);
    }
  }

  OpenCL::ApplyInverseCamMul(gpu_img, raw_data_.color.cam_mul);

  if (dng_warp_rectilinear_.has_value()) {
    throw std::runtime_error("RawProcessor: OpenCL DNG warp is not yet implemented.");
  }

  runtime_color_context_.output_in_camera_space_ = true;
  // TODO: Apply geometric corrections on OpenCL when implemented.
  // For now the buffer stays on the GPU for downstream consumption.
  return {std::move(process_buffer_)};
}

}  // namespace alcedo

#endif
