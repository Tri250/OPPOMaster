//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#ifdef HAVE_OPENCL

namespace alcedo::OpenCL::Scope {

inline constexpr const char* kManifestName        = "scope_analyzer";

inline constexpr const char* kScopeProgramName    = "scope_analyzer";

inline constexpr const char* kHistogramKernelName = "scope_accumulate_histogram";

inline constexpr const char* kWaveformKernelName  = "scope_accumulate_waveform";

}  // namespace alcedo::OpenCL::Scope

#endif
