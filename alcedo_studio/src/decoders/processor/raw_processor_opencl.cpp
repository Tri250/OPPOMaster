//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "decoders/processor/raw_processor.hpp"

#ifdef HAVE_OPENCL

#include <chrono>
#include <iostream>
#include <sstream>
#include <stdexcept>
#include <vector>

#include "decoders/processor/operators/gpu/opencl_cvt_ref_space.hpp"
#include "decoders/processor/operators/gpu/opencl_debayer_rcd.hpp"
#include "decoders/processor/operators/gpu/opencl_highlight_reconstruct.hpp"
#include "decoders/processor/operators/gpu/opencl_to_linear_ref.hpp"
#include "decoders/processor/operators/gpu/opencl_xtrans_interpolate.hpp"
#include "decoders/processor/raw_processor_internal.hpp"
#include "opencl/opencl_geometry_utils.hpp"

namespace alcedo {

namespace {

using ProfileClock = std::chrono::steady_clock;

struct DeferredOpenClLog {
  std::vector<std::string> entries;

  void                     Add(std::string entry) { entries.push_back(std::move(entry)); }

  void                     Flush() const {
    if (entries.empty()) {
      return;
    }
    std::cout << "[LOG] ";
    for (size_t i = 0; i < entries.size(); ++i) {
      if (i != 0) {
        std::cout << " | ";
      }
      std::cout << entries[i];
    }
    std::cout << '\n';
  }
};

void PrintProfileMs(DeferredOpenClLog& log, const char* label,
                    const ProfileClock::duration elapsed) {
  std::ostringstream oss;
  oss << label << '=' << std::chrono::duration_cast<std::chrono::milliseconds>(elapsed).count()
      << " ms";
  log.Add(oss.str());
}

void LogProfileStep(DeferredOpenClLog& log, const char* label,
                    const ProfileClock::time_point start) {
  PrintProfileMs(log, label, ProfileClock::now() - start);
}

void ApplyOpenClGeometricCorrections(opencl::OpenClImage& gpu_img, const int flip) {
  switch (flip) {
    case 3:
      OpenCL::Geometry::Rotate180(gpu_img);
      break;
    case 5:
      OpenCL::Geometry::Rotate90CCW(gpu_img);
      break;
    case 6:
      OpenCL::Geometry::Rotate90CW(gpu_img);
      break;
    default:
      break;
  }
}

void CropOpenClImage(opencl::OpenClImage& gpu_img, const cv::Rect& crop_rect) {
  if (detail::IsFullImageRect(crop_rect, cv::Size(gpu_img.Width(), gpu_img.Height()))) {
    return;
  }
  opencl::OpenClImage cropped;
  OpenCL::Geometry::CropResize(gpu_img, cropped, crop_rect, crop_rect.size());
  gpu_img = std::move(cropped);
}

}  // namespace

auto RawProcessor::ProcessDirectRgbOpenCL() -> ImageBuffer {
  process_buffer_.SyncToGPU(GpuBackendKind::OpenCL);
  process_buffer_.ReleaseCPUData();
  auto& gpu_img = process_buffer_.GetOpenClImage();
  ApplyOpenClGeometricCorrections(gpu_img, raw_data_.sizes.flip);
  return {std::move(process_buffer_)};
}

auto RawProcessor::ProcessOpenCL() -> ImageBuffer {
  if (input_kind_ == RawInputKind::DebayeredRgb) {
    return ProcessDirectRgbOpenCL();
  }

  DeferredOpenClLog deferred_log;
  const auto        full_frame_start       = ProfileClock::now();

  // CPU downsample (same as Metal / CUDA paths).
  const auto        stage_decode_res_start = ProfileClock::now();
  SetDecodeRes(gpu_input_downsample_passes_);
  LogProfileStep(deferred_log, "RAW OpenCL setup decode-res", stage_decode_res_start);

  const auto stage_upload_start = ProfileClock::now();
  process_buffer_.SyncToGPU(GpuBackendKind::OpenCL);
  process_buffer_.ReleaseCPUData();
  LogProfileStep(deferred_log, "RAW OpenCL sync/upload", stage_upload_start);

  auto&      gpu_img            = process_buffer_.GetOpenClImage();

  const auto stage_linear_start = ProfileClock::now();
  OpenCL::ToLinearRef(gpu_img, raw_processor_, cfa_pattern_);
  LogProfileStep(deferred_log, "RAW OpenCL to-linear", stage_linear_start);

  if (cfa_pattern_.kind == RawCfaKind::Bayer2x2 && params_.highlights_reconstruct_) {
    const auto stage_debayer_start = ProfileClock::now();
    OpenCL::Bayer2x2ToRGB_RCD(gpu_img, cfa_pattern_.bayer_pattern);
    LogProfileStep(deferred_log, "RAW OpenCL debayer", stage_debayer_start);

    const auto     stage_crop_start = ProfileClock::now();
    const cv::Rect crop_rect        = detail::BuildDecodeCropRect(
        raw_data_.sizes, default_crop_, cv::Size(gpu_img.Width(), gpu_img.Height()),
        params_.decode_res_);
    CropOpenClImage(gpu_img, crop_rect);
    LogProfileStep(deferred_log, "RAW OpenCL crop", stage_crop_start);

    const auto stage_highlight_start = ProfileClock::now();
    OpenCL::HighlightReconstruct(gpu_img, raw_processor_);
    LogProfileStep(deferred_log, "RAW OpenCL highlight reconstruct", stage_highlight_start);
  } else {
    if (cfa_pattern_.kind == RawCfaKind::XTrans6x6) {
      const int  passes             = params_.decode_res_ == DecodeRes::FULL ? 3 : 1;
      const auto stage_xtrans_start = ProfileClock::now();
      OpenCL::XTransToRGB_Ref(gpu_img, cfa_pattern_.xtrans_pattern, passes);
      LogProfileStep(deferred_log, "RAW OpenCL xtrans interpolate", stage_xtrans_start);
    } else {
      const auto stage_debayer_start = ProfileClock::now();
      OpenCL::Bayer2x2ToRGB_RCD(gpu_img, cfa_pattern_.bayer_pattern);
      LogProfileStep(deferred_log, "RAW OpenCL debayer", stage_debayer_start);
    }

    const auto     stage_crop_start = ProfileClock::now();
    const cv::Rect crop_rect        = detail::BuildDecodeCropRect(
        raw_data_.sizes, default_crop_, cv::Size(gpu_img.Width(), gpu_img.Height()),
        params_.decode_res_);
    CropOpenClImage(gpu_img, crop_rect);
    LogProfileStep(deferred_log, "RAW OpenCL crop", stage_crop_start);
  }

  const auto stage_cam_mul_start = ProfileClock::now();
  OpenCL::ApplyInverseCamMul(gpu_img, raw_data_.color.cam_mul);
  LogProfileStep(deferred_log, "RAW OpenCL apply inverse cam mul", stage_cam_mul_start);

  if (dng_warp_rectilinear_.has_value()) {
    const auto          stage_dng_warp_start = ProfileClock::now();
    opencl::OpenClImage warped;
    OpenCL::Geometry::WarpRectilinear(gpu_img, warped, *dng_warp_rectilinear_);
    gpu_img                                              = std::move(warped);
    runtime_color_context_.dng_warp_rectilinear_applied_ = true;
    LogProfileStep(deferred_log, "RAW OpenCL DNG warp rectilinear", stage_dng_warp_start);
  }

  runtime_color_context_.output_in_camera_space_ = true;
  const auto stage_geo_start                     = ProfileClock::now();
  ApplyOpenClGeometricCorrections(gpu_img, raw_data_.sizes.flip);
  LogProfileStep(deferred_log, "RAW OpenCL geometric corrections", stage_geo_start);

  PrintProfileMs(deferred_log, "RAW OpenCL FullFrame", ProfileClock::now() - full_frame_start);
  deferred_log.Flush();
  return {std::move(process_buffer_)};
}

}  // namespace alcedo

#endif
