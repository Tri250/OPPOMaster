//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#ifdef HAVE_OPENCL

namespace alcedo::OpenCL::Pipeline {

inline constexpr const char* kManifestName = "edit_pipeline";

inline constexpr const char* kFusedProgramName = "edit_pipeline_fused";

inline constexpr const char* kFusedKernelName = "edit_pipeline_fused_rgba32f";

inline constexpr const char* kValidateFusedParamsKernelName =
    "edit_pipeline_validate_fused_params";

}  // namespace alcedo::OpenCL::Pipeline

#endif
