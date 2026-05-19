//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "decoders/processor/raw_processor.hpp"

#ifdef HAVE_OPENCL

#include <stdexcept>

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

  // Additional operators (debayer, highlight reconstruction, warp, pack) are
  // not yet implemented for OpenCL.  Throw so callers know the skeleton is
  // present but incomplete.
  throw std::runtime_error(
      "RawProcessor: OpenCL backend to_linear_ref is implemented, but subsequent operators "
      "(debayer, highlight reconstruct, pack-rgba, etc.) are not yet available.");
}

}  // namespace alcedo

#endif
