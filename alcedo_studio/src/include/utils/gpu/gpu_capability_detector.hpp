//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <string>

#include "image/gpu_backend.hpp"

namespace alcedo::gpu {

enum class GpuCapabilityLevel {
  Full,          // CUDA/Metal/OpenCL with full driver support
  Limited,       // OpenCL fallback (CUDA driver too old or unavailable)
  SoftwareOnly,  // CPU-only pipeline
};

struct GpuCapabilityInfo {
  GpuCapabilityLevel capability_level = GpuCapabilityLevel::SoftwareOnly;
  GpuBackendKind     recommended_backend = GpuBackendKind::None;
  int                detected_cuda_driver_version = 0;
  int                minimum_cuda_driver_version = 0;
  std::string        gpu_adapter_name;
  std::string        detail;
  bool               warning_suppressed = false;

  [[nodiscard]] auto IsFull() const -> bool {
    return capability_level == GpuCapabilityLevel::Full;
  }
  [[nodiscard]] auto IsLimited() const -> bool {
    return capability_level == GpuCapabilityLevel::Limited;
  }
  [[nodiscard]] auto IsSoftwareOnly() const -> bool {
    return capability_level == GpuCapabilityLevel::SoftwareOnly;
  }
};

class GpuCapabilityDetector {
 public:
  /// Detect GPU capability and determine the appropriate backend.
  /// On Windows, checks the CUDA driver version against the minimum required.
  /// On macOS, always returns Full with Metal backend.
  /// On Linux, checks OpenCL/CUDA availability.
  static auto Detect() -> GpuCapabilityInfo;

  /// Check whether the user has suppressed the driver warning for this session.
  /// The preference is persisted via QSettings.
  [[nodiscard]] static auto IsDriverWarningSuppressed() -> bool;

  /// Persist the user's choice to not show the driver warning again.
  static void SetDriverWarningSuppressed(bool suppressed);

  /// Generate a human-readable warning message about driver requirements.
  [[nodiscard]] static auto BuildDriverWarningMessage(const GpuCapabilityInfo& info)
      -> std::string;
};

}  // namespace alcedo::gpu
