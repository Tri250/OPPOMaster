//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#ifdef HAVE_OPENCL

namespace alcedo::OpenCL::Geometry {

inline constexpr const char* kManifestName               = "opencl_geometry";

inline constexpr const char* kGeometryProgramName        = "opencl_geometry_utils";
inline constexpr const char* kCropResizeLinearKernelName = "opencl_geometry_crop_resize_linear";
inline constexpr const char* kCropResizeAreaKernelName   = "opencl_geometry_crop_resize_area";
inline constexpr const char* kWarpAffineLinearKernelName = "opencl_geometry_warp_affine_linear";
inline constexpr const char* kRotateKernelName           = "opencl_geometry_rotate";

inline constexpr const char* kLensCalibProgramName       = "edit_geometry_lens_calib";
inline constexpr const char* kLensVignettingKernelName   = "edit_geometry_lens_vignetting_rgba32f";
inline constexpr const char* kLensWarpKernelName         = "edit_geometry_lens_warp_rgba32f";

}  // namespace alcedo::OpenCL::Geometry

#endif
