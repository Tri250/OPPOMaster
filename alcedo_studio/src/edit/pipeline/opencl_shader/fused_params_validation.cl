//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

__kernel void edit_pipeline_validate_fused_params(__global const OpenClFusedParams* params,
                                                  __global float* output) {
  if (get_global_id(0) != 0) {
    return;
  }

  output[0] = (float)params->exposure_enabled_ + params->exposure_offset_;
  output[1] = (float)params->contrast_enabled_ + params->contrast_scale_;
  output[2] = params->shared_tone_curve_ctrl_pts_x_[0] +
              params->shared_tone_curve_ctrl_pts_y_[0];
  output[3] = (float)params->hls_profile_count_ + params->hls_profile_hues_[0];
  output[4] = (float)params->to_output_params_.method_ +
              params->to_output_params_.display_linear_scale_;
  output[5] = params->to_output_params_.limit_to_display_matx[0] +
              params->to_output_params_.open_drt_params_.tn_con_;
  output[6] = (float)params->curve_enabled_ + (float)params->curve_ctrl_pts_size_;
  output[7] = (float)params->lmt_lut_enabled_ + (float)params->lmt_lut_edge_size_;
}
