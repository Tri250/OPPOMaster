//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#ifdef HAVE_OPENCL

namespace alcedo::OpenCL::Pipeline {

inline constexpr const char* kManifestName = "edit_pipeline";

inline constexpr const char* kFusedProgramName = "edit_pipeline_fused";

inline constexpr const char* kDetailProgramName = "edit_pipeline_detail";

inline constexpr const char* kFusedKernelName = "edit_pipeline_fused_rgba32f";

inline constexpr const char* kFusedStageKernelName = "edit_pipeline_fused_stage_rgba32f";

inline constexpr const char* kNeighborBlurHorizontalKernelName =
    "edit_pipeline_neighbor_blur_h_rgba32f";
inline constexpr const char* kNeighborApplyVerticalKernelName =
    "edit_pipeline_neighbor_apply_v_rgba32f";
inline constexpr const char* kHsBuildLogBaseHorizontalKernelName =
    "edit_pipeline_hs_build_log_base_h_rgba32f";
inline constexpr const char* kHsBuildLogBaseVerticalKernelName =
    "edit_pipeline_hs_build_log_base_v_rgba32f";
inline constexpr const char* kHsApplyLocalToneKernelName =
    "edit_pipeline_hs_apply_local_tone_rgba32f";

inline constexpr const char* kValidateFusedParamsKernelName =
    "edit_pipeline_validate_fused_params";

}  // namespace alcedo::OpenCL::Pipeline

#endif
