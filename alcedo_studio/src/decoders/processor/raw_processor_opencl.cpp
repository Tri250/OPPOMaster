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
#include "decoders/processor/raw_processor_internal.hpp"

namespace alcedo {

namespace {

using ProfileClock = std::chrono::steady_clock;

struct DeferredOpenClLog {
  std::vector<std::string> entries;

  void Add(std::string entry) { entries.push_back(std::move(entry)); }

  void Flush() const {
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

}  // namespace

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

  DeferredOpenClLog deferred_log;
  const auto        full_frame_start = ProfileClock::now();

  // CPU downsample (same as Metal / CUDA paths).
  const auto stage_decode_res_start = ProfileClock::now();
  SetDecodeRes();
  LogProfileStep(deferred_log, "RAW OpenCL setup decode-res", stage_decode_res_start);

  const auto stage_upload_start = ProfileClock::now();
  process_buffer_.SyncToGPU(GpuBackendKind::OpenCL);
  process_buffer_.ReleaseCPUData();
  LogProfileStep(deferred_log, "RAW OpenCL sync/upload", stage_upload_start);

  auto& gpu_img = process_buffer_.GetOpenClImage();

  const auto stage_linear_start = ProfileClock::now();
  OpenCL::ToLinearRef(gpu_img, raw_processor_, cfa_pattern_);
  LogProfileStep(deferred_log, "RAW OpenCL to-linear", stage_linear_start);

  if (cfa_pattern_.kind == RawCfaKind::Bayer2x2 && params_.highlights_reconstruct_) {
    const auto stage_debayer_start = ProfileClock::now();
    OpenCL::Bayer2x2ToRGB_RCD(gpu_img, cfa_pattern_.bayer_pattern);
    LogProfileStep(deferred_log, "RAW OpenCL debayer", stage_debayer_start);

    const auto stage_crop_start = ProfileClock::now();
    const cv::Rect crop_rect = detail::BuildDecodeCropRect(
        raw_data_.sizes, default_crop_,
        cv::Size(gpu_img.Width(), gpu_img.Height()), params_.decode_res_);
    if (!detail::IsFullImageRect(crop_rect, cv::Size(gpu_img.Width(), gpu_img.Height()))) {
      opencl::OpenClImage cropped;
      gpu_img.CropTo(cropped, crop_rect);
      gpu_img = std::move(cropped);
    }
    LogProfileStep(deferred_log, "RAW OpenCL crop", stage_crop_start);

    const auto stage_highlight_start = ProfileClock::now();
    OpenCL::HighlightReconstruct(gpu_img, raw_processor_);
    LogProfileStep(deferred_log, "RAW OpenCL highlight reconstruct", stage_highlight_start);
  } else {
    if (cfa_pattern_.kind == RawCfaKind::XTrans6x6) {
      throw std::runtime_error(
          "RawProcessor: OpenCL X-Trans interpolation is not yet implemented.");
    } else {
      const auto stage_debayer_start = ProfileClock::now();
      OpenCL::Bayer2x2ToRGB_RCD(gpu_img, cfa_pattern_.bayer_pattern);
      LogProfileStep(deferred_log, "RAW OpenCL debayer", stage_debayer_start);
    }

    const auto stage_crop_start = ProfileClock::now();
    const cv::Rect crop_rect = detail::BuildDecodeCropRect(
        raw_data_.sizes, default_crop_,
        cv::Size(gpu_img.Width(), gpu_img.Height()), params_.decode_res_);
    if (!detail::IsFullImageRect(crop_rect, cv::Size(gpu_img.Width(), gpu_img.Height()))) {
      opencl::OpenClImage cropped;
      gpu_img.CropTo(cropped, crop_rect);
      gpu_img = std::move(cropped);
    }
    LogProfileStep(deferred_log, "RAW OpenCL crop", stage_crop_start);
  }

  const auto stage_cam_mul_start = ProfileClock::now();
  OpenCL::ApplyInverseCamMul(gpu_img, raw_data_.color.cam_mul);
  LogProfileStep(deferred_log, "RAW OpenCL apply inverse cam mul", stage_cam_mul_start);

  if (dng_warp_rectilinear_.has_value()) {
    throw std::runtime_error("RawProcessor: OpenCL DNG warp is not yet implemented.");
  }

  runtime_color_context_.output_in_camera_space_ = true;
  // TODO: Apply geometric corrections on OpenCL when implemented.
  // For now the buffer stays on the GPU for downstream consumption.

  PrintProfileMs(deferred_log, "RAW OpenCL FullFrame", ProfileClock::now() - full_frame_start);
  deferred_log.Flush();
  return {std::move(process_buffer_)};
}

}  // namespace alcedo

#endif
