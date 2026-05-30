//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#ifdef HAVE_OPENCL

#include <algorithm>
#include <cstdint>
#include <cstring>
#include <filesystem>
#include <mutex>
#include <sstream>
#include <stdexcept>
#include <string>
#include <type_traits>
#include <unordered_map>
#include <vector>

#include "edit/operators/GPU_kernels/fused_param.hpp"
#include "opencl/opencl_context.hpp"
#include "utils/lut/cube_lut.hpp"

namespace alcedo::OpenCL::Pipeline {

inline constexpr int32_t kOpenClAcesOdtTableSize = TOTAL_TABLE_SIZE;

struct OpenClJMhParams {
  float MATRIX_RGB_to_CAM16_c_[9]       = {};
  float MATRIX_CAM16_c_to_RGB_[9]       = {};
  float MATRIX_cone_response_to_Aab_[9] = {};
  float MATRIX_Aab_to_cone_response_[9] = {};
  float F_L_n_                          = 0.0f;
  float cz_                             = 0.0f;
  float inv_cz_                         = 0.0f;
  float A_w_J_                          = 0.0f;
  float inv_A_w_J_                      = 0.0f;
};

struct OpenClTSParams {
  float n_             = 0.0f;
  float n_r_           = 0.0f;
  float g_             = 0.0f;
  float t_1_           = 0.0f;
  float c_t_           = 0.0f;
  float s_2_           = 0.0f;
  float u_2_           = 0.0f;
  float m_2_           = 0.0f;
  float forward_limit_ = 0.0f;
  float inverse_limit_ = 0.0f;
  float log_peak_      = 0.0f;
};

struct OpenClODTParams {
  float           peak_luminance_                                  = 100.0f;
  OpenClJMhParams input_params_                                    = {};
  OpenClJMhParams reach_params_                                    = {};
  OpenClJMhParams limit_params_                                    = {};
  OpenClTSParams  ts_                                              = {};
  float           limit_J_max                                      = 0.0f;
  float           model_gamma_inv                                  = 0.0f;
  float           mid_J                                            = 0.0f;
  float           focus_dist                                       = 0.0f;
  float           lower_hull_gamma_inv                             = 0.0f;
  int32_t         hue_linearity_search_range[2]                    = {0, 1};
  float           sat                                              = 0.0f;
  float           sat_thr                                          = 0.0f;
  float           compr                                            = 0.0f;
  float           chroma_compress_scale                            = 0.0f;
  float           table_reach_M_[kOpenClAcesOdtTableSize]          = {};
  float           table_hues_[kOpenClAcesOdtTableSize]             = {};
  float           table_upper_hull_gamma_[kOpenClAcesOdtTableSize] = {};
  float           table_gamut_cusps_[kOpenClAcesOdtTableSize][4]   = {};
};

struct OpenClOpenDRTParams {
  int32_t tn_hcon_enable_ = 0;
  int32_t tn_lcon_enable_ = 0;
  int32_t pt_enable_      = 1;
  int32_t ptl_enable_     = 1;
  int32_t ptm_enable_     = 1;
  int32_t brl_enable_     = 1;
  int32_t brlp_enable_    = 1;
  int32_t hc_enable_      = 1;
  int32_t hs_rgb_enable_  = 1;
  int32_t hs_cmy_enable_  = 1;
  int32_t creative_white_ = 2;
  int32_t surround_       = 2;
  int32_t clamp_          = 1;
  int32_t display_gamut_  = 0;
  int32_t display_eotf_   = 1;
  float   tn_con_         = 1.66f;
  float   tn_sh_          = 0.5f;
  float   tn_toe_         = 0.003f;
  float   tn_off_         = 0.005f;
  float   tn_hcon_        = 0.0f;
  float   tn_hcon_pv_     = 1.0f;
  float   tn_hcon_st_     = 4.0f;
  float   tn_lcon_        = 0.0f;
  float   tn_lcon_w_      = 0.5f;
  float   cwp_lm_         = 0.25f;
  float   rs_sa_          = 0.35f;
  float   rs_rw_          = 0.25f;
  float   rs_bw_          = 0.55f;
  float   pt_lml_         = 0.25f;
  float   pt_lml_r_       = 0.5f;
  float   pt_lml_g_       = 0.0f;
  float   pt_lml_b_       = 0.1f;
  float   pt_lmh_         = 0.25f;
  float   pt_lmh_r_       = 0.5f;
  float   pt_lmh_b_       = 0.0f;
  float   ptl_c_          = 0.06f;
  float   ptl_m_          = 0.08f;
  float   ptl_y_          = 0.06f;
  float   ptm_low_        = 0.4f;
  float   ptm_low_rng_    = 0.25f;
  float   ptm_low_st_     = 0.5f;
  float   ptm_high_       = -0.8f;
  float   ptm_high_rng_   = 0.35f;
  float   ptm_high_st_    = 0.4f;
  float   brl_            = 0.0f;
  float   brl_r_          = -2.5f;
  float   brl_g_          = -1.5f;
  float   brl_b_          = -1.5f;
  float   brl_rng_        = 0.5f;
  float   brl_st_         = 0.35f;
  float   brlp_           = -0.5f;
  float   brlp_r_         = -1.25f;
  float   brlp_g_         = -1.25f;
  float   brlp_b_         = -0.25f;
  float   hc_r_           = 1.0f;
  float   hc_r_rng_       = 0.3f;
  float   hs_r_           = 0.6f;
  float   hs_r_rng_       = 0.6f;
  float   hs_g_           = 0.35f;
  float   hs_g_rng_       = 1.0f;
  float   hs_b_           = 0.66f;
  float   hs_b_rng_       = 1.0f;
  float   hs_c_           = 0.25f;
  float   hs_c_rng_       = 1.0f;
  float   hs_m_           = 0.0f;
  float   hs_m_rng_       = 1.0f;
  float   hs_y_           = 0.0f;
  float   hs_y_rng_       = 1.0f;
  float   ts_x1_          = 0.0f;
  float   ts_y1_          = 0.0f;
  float   ts_x0_          = 0.0f;
  float   ts_y0_          = 0.0f;
  float   ts_s0_          = 0.0f;
  float   ts_p_           = 0.0f;
  float   ts_s10_         = 0.0f;
  float   ts_m1_          = 0.0f;
  float   ts_m2_          = 0.0f;
  float   ts_s_           = 0.0f;
  float   ts_dsc_         = 0.0f;
  float   pt_cmp_Lf_      = 0.0f;
  float   s_Lp100_        = 0.0f;
  float   ts_s1_          = 0.0f;
};

struct OpenClToOutputParams {
  int32_t             method_                  = static_cast<int32_t>(GPU_ODTMethod::OPEN_DRT);
  int32_t             eotf_                    = static_cast<int32_t>(GPU_EOTF::LINEAR);
  OpenClODTParams     aces_params_             = {};
  OpenClOpenDRTParams open_drt_params_         = {};
  float               limit_to_display_matx[9] = {};
  float               display_linear_scale_    = 1.0f;
};

struct OpenClFusedParams {
  uint32_t exposure_enabled_                                                                = 1;
  float    exposure_offset_                                                                 = 0.0f;
  uint32_t contrast_enabled_                                                                = 1;
  float    contrast_scale_                                                                  = 0.0f;
  uint32_t shadows_enabled_                                                                 = 1;
  float    shadows_offset_                                                                  = 0.0f;
  float    shadows_x0_                                                                      = 0.0f;
  float    shadows_x1_                                                                      = 0.25f;
  float    shadows_y0_                                                                      = 0.0f;
  float    shadows_y1_                                                                      = 0.25f;
  float    shadows_m0_                                                                      = 0.0f;
  float    shadows_m1_                                                                      = 1.0f;
  float    shadows_dx_                                                                      = 0.25f;
  uint32_t highlights_enabled_                                                              = 1;
  float    highlights_k_                                                                    = 0.2f;
  float    highlights_offset_                                                               = 0.0f;
  float    highlights_slope_range_                                                          = 0.8f;
  float    highlights_m0_                                                                   = 1.0f;
  float    highlights_m1_                                                                   = 1.0f;
  float    highlights_x0_                                                                   = 0.2f;
  float    highlights_y0_                                                                   = 0.2f;
  float    highlights_y1_                                                                   = 1.0f;
  float    highlights_dx_                                                                   = 0.8f;
  uint32_t shared_tone_curve_enabled_                                                       = 0;
  uint32_t shared_tone_curve_apply_in_shadows_                                              = 0;
  uint32_t shared_tone_curve_apply_in_highlights_                                           = 0;
  int32_t  shared_tone_curve_ctrl_pts_size_                                                 = 0;
  float    shared_tone_curve_ctrl_pts_x_[OperatorParams::kSharedToneCurveControlPointCount] = {};
  float    shared_tone_curve_ctrl_pts_y_[OperatorParams::kSharedToneCurveControlPointCount] = {};
  float    shared_tone_curve_h_[OperatorParams::kSharedToneCurveControlPointCount - 1]      = {};
  float    shared_tone_curve_m_[OperatorParams::kSharedToneCurveControlPointCount]          = {};
  uint32_t white_enabled_                                                                   = 1;
  float    white_point_                                                                     = 1.0f;
  uint32_t black_enabled_                                                                   = 1;
  float    black_point_                                                                     = 0.0f;
  float    slope_                                                                           = 1.0f;
  uint32_t hls_enabled_                                                                     = 1;
  float    target_hls_[3]                                      = {0.0f, 0.5f, 1.0f};
  float    hls_adjustment_[3]                                  = {0.0f, 0.0f, 0.0f};
  float    hue_range_                                          = 45.0f;
  float    lightness_range_                                    = 0.1f;
  float    saturation_range_                                   = 0.1f;
  int32_t  hls_profile_count_                                  = OperatorParams::kHlsProfileCount;
  float    hls_profile_hues_[OperatorParams::kHlsProfileCount] = {0.0f,   45.0f,  90.0f,  135.0f,
                                                                  180.0f, 225.0f, 270.0f, 315.0f};
  float    hls_profile_adjustments_[OperatorParams::kHlsProfileCount][3] = {};
  float    hls_profile_hue_ranges_[OperatorParams::kHlsProfileCount] = {45.0f, 45.0f, 45.0f, 45.0f,
                                                                        45.0f, 45.0f, 45.0f, 45.0f};
  uint32_t saturation_enabled_                                       = 1;
  float    saturation_offset_                                        = 1.0f;
  uint32_t tint_enabled_                                             = 1;
  float    tint_offset_                                              = 0.0f;
  uint32_t vibrance_enabled_                                         = 1;
  float    vibrance_offset_                                          = 0.0f;
  uint32_t to_ws_enabled_                                            = 1;
  uint32_t color_temp_enabled_                                       = 1;
  int32_t  color_temp_mode_                                          = 0;
  float    color_temp_resolved_xy_[2]                                = {0.3127f, 0.3290f};
  uint32_t raw_runtime_valid_                                        = 0;
  int32_t  raw_decode_input_space_                                   = 0;
  float    color_temp_cam_to_ap1_[9]     = {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};
  uint32_t color_temp_matrices_valid_    = 0;
  uint32_t lmt_enabled_                  = 0;
  uint32_t lmt_lut_enabled_              = 0;
  uint32_t lmt_lut_edge_size_            = 0;
  uint32_t to_output_enabled_            = 1;
  OpenClToOutputParams to_output_params_ = {};
  uint32_t             curve_enabled_    = 1;
  int32_t              curve_ctrl_pts_size_                                           = 0;
  float                curve_ctrl_pts_x_[FusedOperatorParams::kMaxCurveControlPoints] = {};
  float                curve_ctrl_pts_y_[FusedOperatorParams::kMaxCurveControlPoints] = {};
  float                curve_h_[FusedOperatorParams::kMaxCurveControlPoints - 1]      = {};
  float                curve_m_[FusedOperatorParams::kMaxCurveControlPoints]          = {};
  uint32_t             clarity_enabled_                                               = 1;
  float                clarity_offset_                                                = 0.0f;
  float                clarity_radius_                                                = 5.0f;
  uint32_t             sharpen_enabled_                                               = 1;
  float                sharpen_offset_                                                = 0.0f;
  float                sharpen_radius_                                                = 3.0f;
  float                sharpen_threshold_                                             = 0.0f;
  uint32_t             color_wheel_enabled_                                           = 1;
  float                lift_color_offset_[3]   = {0.0f, 0.0f, 0.0f};
  float                lift_luminance_offset_  = 0.0f;
  float                gamma_color_offset_[3]  = {1.0f, 1.0f, 1.0f};
  float                gamma_luminance_offset_ = 0.0f;
  float                gain_color_offset_[3]   = {1.0f, 1.0f, 1.0f};
  float                gain_luminance_offset_  = 0.0f;
};

static_assert(std::is_standard_layout_v<OpenClFusedParams>);
static_assert(sizeof(uint32_t) == sizeof(float));
static_assert(sizeof(int32_t) == sizeof(float));

class OpenClBuffer {
 private:
  cl_mem buffer_ = nullptr;

 public:
  OpenClBuffer() = default;
  ~OpenClBuffer() { Reset(); }

  OpenClBuffer(const OpenClBuffer& other) : buffer_(other.buffer_) {
    if (buffer_ != nullptr) {
      clRetainMemObject(buffer_);
    }
  }

  auto operator=(const OpenClBuffer& other) -> OpenClBuffer& {
    if (this == &other) {
      return *this;
    }
    Reset();
    buffer_ = other.buffer_;
    if (buffer_ != nullptr) {
      clRetainMemObject(buffer_);
    }
    return *this;
  }

  OpenClBuffer(OpenClBuffer&& other) noexcept : buffer_(other.buffer_) { other.buffer_ = nullptr; }

  auto operator=(OpenClBuffer&& other) noexcept -> OpenClBuffer& {
    if (this == &other) {
      return *this;
    }
    Reset();
    buffer_       = other.buffer_;
    other.buffer_ = nullptr;
    return *this;
  }

  [[nodiscard]] auto Get() const -> cl_mem { return buffer_; }
  [[nodiscard]] auto Valid() const -> bool { return buffer_ != nullptr; }

  void               Reset() {
    if (buffer_ != nullptr) {
      clReleaseMemObject(buffer_);
      buffer_ = nullptr;
    }
  }

  static auto CreateReadOnlyCopy(const void* data, size_t bytes) -> OpenClBuffer {
    if (data == nullptr || bytes == 0) {
      throw std::runtime_error("OpenCL fused params: invalid buffer upload.");
    }
    auto& context = OpenClContext::Instance();
    if (!context.IsInitialized()) {
      context.Initialize();
    }

    cl_int       err = CL_SUCCESS;
    OpenClBuffer buffer;
    buffer.buffer_ = clCreateBuffer(context.Context(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                                    bytes, const_cast<void*>(data), &err);
    if (err != CL_SUCCESS || buffer.buffer_ == nullptr) {
      throw std::runtime_error("OpenCL fused params: clCreateBuffer failed with error " +
                               std::to_string(err) + ".");
    }
    return buffer;
  }
};

struct OpenClFusedResources {
  FusedOperatorParams common_params_     = {};
  OpenClFusedParams   opencl_params_     = {};
  OpenClBuffer        params_buffer_     = {};
  OpenClBuffer        lmt_lut_buffer_    = {};
  uint32_t            lmt_lut_edge_size_ = 0;
  std::uintptr_t      lmt_lut_source_id_ = 0;

  void                Reset() {
    params_buffer_.Reset();
    lmt_lut_buffer_.Reset();
    lmt_lut_edge_size_ = 0;
    lmt_lut_source_id_ = 0;
  }
};

class OpenClFusedParamUploader {
 public:
  static auto Upload(const FusedOperatorParams& fused_params, OperatorParams& cpu_params,
                     OpenClFusedResources& orig_resources) -> OpenClFusedResources {
    OpenClFusedResources resources = orig_resources;
    resources.common_params_       = fused_params;
    UploadLmt(cpu_params, resources);
    resources.opencl_params_ = BuildOpenClParams(fused_params, cpu_params, resources);
    resources.params_buffer_ =
        OpenClBuffer::CreateReadOnlyCopy(&resources.opencl_params_, sizeof(OpenClFusedParams));
    return resources;
  }

 private:
  struct OpenClLutCacheEntry {
    OpenClBuffer buffer;
    uint32_t     edge_size = 0;
  };

  // Intentionally leaked to avoid OpenCL shutdown ordering issues during static destruction.
  static auto GetOpenClLutPathCache() -> std::unordered_map<std::string, OpenClLutCacheEntry>& {
    static auto* cache = new std::unordered_map<std::string, OpenClLutCacheEntry>();
    return *cache;
  }

  static auto GetOpenClLutPathCacheMutex() -> std::mutex& {
    static auto* mutex = new std::mutex();
    return *mutex;
  }

  static auto BuildOpenClLutPathCacheKey(const std::filesystem::path& path) -> std::string {
    std::error_code ec;
    const auto      abs_path   = std::filesystem::absolute(path, ec);
    const auto      normalized = (ec ? path : abs_path).lexically_normal();
    const auto      utf8       = normalized.generic_u8string();
    std::string     key{reinterpret_cast<const char*>(utf8.data()), utf8.size()};

    std::error_code size_ec;
    const auto      file_size = std::filesystem::file_size(normalized, size_ec);
    key += "|s=" + std::to_string(size_ec ? static_cast<std::uintmax_t>(0) : file_size);

    std::error_code time_ec;
    const auto      mtime = std::filesystem::last_write_time(normalized, time_ec);
    const auto      stamp = time_ec ? 0LL : mtime.time_since_epoch().count();
    key += "|t=" + std::to_string(stamp);

    return key;
  }
  static auto BuildOpenClParams(const FusedOperatorParams&  fused_params,
                                const OperatorParams&       cpu_params,
                                const OpenClFusedResources& resources) -> OpenClFusedParams {
    OpenClFusedParams params;
    params.exposure_enabled_          = fused_params.exposure_enabled_ ? 1U : 0U;
    params.exposure_offset_           = fused_params.exposure_offset_;
    params.contrast_enabled_          = fused_params.contrast_enabled_ ? 1U : 0U;
    params.contrast_scale_            = fused_params.contrast_scale_;
    params.shadows_enabled_           = fused_params.shadows_enabled_ ? 1U : 0U;
    params.shadows_offset_            = fused_params.shadows_offset_;
    params.shadows_x0_                = fused_params.shadows_x0_;
    params.shadows_x1_                = fused_params.shadows_x1_;
    params.shadows_y0_                = fused_params.shadows_y0_;
    params.shadows_y1_                = fused_params.shadows_y1_;
    params.shadows_m0_                = fused_params.shadows_m0_;
    params.shadows_m1_                = fused_params.shadows_m1_;
    params.shadows_dx_                = fused_params.shadows_dx_;
    params.highlights_enabled_        = fused_params.highlights_enabled_ ? 1U : 0U;
    params.highlights_k_              = fused_params.highlights_k_;
    params.highlights_offset_         = fused_params.highlights_offset_;
    params.highlights_slope_range_    = fused_params.highlights_slope_range_;
    params.highlights_m0_             = fused_params.highlights_m0_;
    params.highlights_m1_             = fused_params.highlights_m1_;
    params.highlights_x0_             = fused_params.highlights_x0_;
    params.highlights_y0_             = fused_params.highlights_y0_;
    params.highlights_y1_             = fused_params.highlights_y1_;
    params.highlights_dx_             = fused_params.highlights_dx_;
    params.shared_tone_curve_enabled_ = fused_params.shared_tone_curve_enabled_ ? 1U : 0U;
    params.shared_tone_curve_apply_in_shadows_ =
        fused_params.shared_tone_curve_apply_in_shadows_ ? 1U : 0U;
    params.shared_tone_curve_apply_in_highlights_ =
        fused_params.shared_tone_curve_apply_in_highlights_ ? 1U : 0U;
    params.shared_tone_curve_ctrl_pts_size_ = fused_params.shared_tone_curve_ctrl_pts_size_;
    std::memcpy(params.shared_tone_curve_ctrl_pts_x_, fused_params.shared_tone_curve_ctrl_pts_x_,
                sizeof(params.shared_tone_curve_ctrl_pts_x_));
    std::memcpy(params.shared_tone_curve_ctrl_pts_y_, fused_params.shared_tone_curve_ctrl_pts_y_,
                sizeof(params.shared_tone_curve_ctrl_pts_y_));
    std::memcpy(params.shared_tone_curve_h_, fused_params.shared_tone_curve_h_,
                sizeof(params.shared_tone_curve_h_));
    std::memcpy(params.shared_tone_curve_m_, fused_params.shared_tone_curve_m_,
                sizeof(params.shared_tone_curve_m_));
    params.white_enabled_ = fused_params.white_enabled_ ? 1U : 0U;
    params.white_point_   = fused_params.white_point_;
    params.black_enabled_ = fused_params.black_enabled_ ? 1U : 0U;
    params.black_point_   = fused_params.black_point_;
    params.slope_         = fused_params.slope_;
    params.hls_enabled_   = fused_params.hls_enabled_ ? 1U : 0U;
    std::memcpy(params.target_hls_, fused_params.target_hls_, sizeof(params.target_hls_));
    std::memcpy(params.hls_adjustment_, fused_params.hls_adjustment_,
                sizeof(params.hls_adjustment_));
    params.hue_range_         = fused_params.hue_range_;
    params.lightness_range_   = fused_params.lightness_range_;
    params.saturation_range_  = fused_params.saturation_range_;
    params.hls_profile_count_ = fused_params.hls_profile_count_;
    std::memcpy(params.hls_profile_hues_, fused_params.hls_profile_hues_,
                sizeof(params.hls_profile_hues_));
    std::memcpy(params.hls_profile_adjustments_, fused_params.hls_profile_adjustments_,
                sizeof(params.hls_profile_adjustments_));
    std::memcpy(params.hls_profile_hue_ranges_, fused_params.hls_profile_hue_ranges_,
                sizeof(params.hls_profile_hue_ranges_));
    params.saturation_enabled_ = fused_params.saturation_enabled_ ? 1U : 0U;
    params.saturation_offset_  = fused_params.saturation_offset_;
    params.tint_enabled_       = fused_params.tint_enabled_ ? 1U : 0U;
    params.tint_offset_        = fused_params.tint_offset_;
    params.vibrance_enabled_   = fused_params.vibrance_enabled_ ? 1U : 0U;
    params.vibrance_offset_    = fused_params.vibrance_offset_;
    params.to_ws_enabled_      = fused_params.to_ws_enabled_ ? 1U : 0U;
    params.color_temp_enabled_ = fused_params.color_temp_enabled_ ? 1U : 0U;
    params.color_temp_mode_    = fused_params.color_temp_mode_;
    std::memcpy(params.color_temp_resolved_xy_, fused_params.color_temp_resolved_xy_,
                sizeof(params.color_temp_resolved_xy_));
    params.raw_runtime_valid_      = fused_params.raw_runtime_valid_ ? 1U : 0U;
    params.raw_decode_input_space_ = fused_params.raw_decode_input_space_;
    std::memcpy(params.color_temp_cam_to_ap1_, fused_params.color_temp_cam_to_ap1_,
                sizeof(params.color_temp_cam_to_ap1_));
    params.color_temp_matrices_valid_ = fused_params.color_temp_matrices_valid_ ? 1U : 0U;
    params.lmt_enabled_               = fused_params.lmt_enabled_ ? 1U : 0U;
    params.lmt_lut_enabled_ =
        (resources.lmt_lut_buffer_.Get() != nullptr && resources.lmt_lut_edge_size_ > 1U) ? 1U : 0U;
    params.lmt_lut_edge_size_ = resources.lmt_lut_edge_size_;
    params.to_output_enabled_ = fused_params.to_output_enabled_ ? 1U : 0U;

    auto copy33               = [](const cv::Matx33f& m, float out[9]) {
      out[0] = m(0, 0);
      out[1] = m(0, 1);
      out[2] = m(0, 2);
      out[3] = m(1, 0);
      out[4] = m(1, 1);
      out[5] = m(1, 2);
      out[6] = m(2, 0);
      out[7] = m(2, 1);
      out[8] = m(2, 2);
    };
    const auto& to_output_cpu = cpu_params.to_output_params_;
    auto&       to_output     = params.to_output_params_;
    to_output.method_         = static_cast<int32_t>(to_output_cpu.method_);
    to_output.eotf_           = static_cast<int32_t>(to_output_cpu.eotf_);
    copy33(to_output_cpu.limit_to_display_matx_, to_output.limit_to_display_matx);
    to_output.display_linear_scale_ = to_output_cpu.display_linear_scale_;

    auto copy_jmh                   = [&](const ColorUtils::JMhParams& src, OpenClJMhParams& dst) {
      copy33(src.MATRIX_RGB_to_CAM16_c_, dst.MATRIX_RGB_to_CAM16_c_);
      copy33(src.MATRIX_CAM16_c_to_RGB_, dst.MATRIX_CAM16_c_to_RGB_);
      copy33(src.MATRIX_cone_response_to_Aab_, dst.MATRIX_cone_response_to_Aab_);
      copy33(src.MATRIX_Aab_to_cone_response_, dst.MATRIX_Aab_to_cone_response_);
      dst.F_L_n_     = src.F_L_n_;
      dst.cz_        = src.cz_;
      dst.inv_cz_    = src.inv_cz_;
      dst.A_w_J_     = (src.inv_A_w_J_ != 0.0f) ? (1.0f / src.inv_A_w_J_) : 0.0f;
      dst.inv_A_w_J_ = src.inv_A_w_J_;
    };

    const auto& odt_cpu              = to_output_cpu.aces_params_;
    auto&       odt_opencl           = to_output.aces_params_;
    odt_opencl.peak_luminance_       = odt_cpu.peak_luminance_;
    odt_opencl.limit_J_max           = odt_cpu.limit_J_max_;
    odt_opencl.model_gamma_inv       = odt_cpu.model_gamma_inv_;
    odt_opencl.ts_.n_                = odt_cpu.ts_params_.n_;
    odt_opencl.ts_.n_r_              = odt_cpu.ts_params_.n_r_;
    odt_opencl.ts_.g_                = odt_cpu.ts_params_.g_;
    odt_opencl.ts_.t_1_              = odt_cpu.ts_params_.t_1_;
    odt_opencl.ts_.c_t_              = odt_cpu.ts_params_.c_t_;
    odt_opencl.ts_.s_2_              = odt_cpu.ts_params_.s_2_;
    odt_opencl.ts_.u_2_              = odt_cpu.ts_params_.u_2_;
    odt_opencl.ts_.m_2_              = odt_cpu.ts_params_.m_2_;
    odt_opencl.ts_.forward_limit_    = odt_cpu.ts_params_.forward_limit_;
    odt_opencl.ts_.inverse_limit_    = odt_cpu.ts_params_.inverse_limit_;
    odt_opencl.ts_.log_peak_         = odt_cpu.ts_params_.log_peak_;
    odt_opencl.sat                   = odt_cpu.sat_;
    odt_opencl.sat_thr               = odt_cpu.sat_thr_;
    odt_opencl.compr                 = odt_cpu.compr_;
    odt_opencl.chroma_compress_scale = odt_cpu.chroma_compress_scale_;
    odt_opencl.mid_J                 = odt_cpu.mid_J_;
    odt_opencl.focus_dist            = odt_cpu.focus_dist_;
    odt_opencl.lower_hull_gamma_inv  = odt_cpu.lower_hull_gamma_inv_;
    odt_opencl.hue_linearity_search_range[0] =
        static_cast<int32_t>(odt_cpu.hue_linearity_search_range_(0));
    odt_opencl.hue_linearity_search_range[1] =
        static_cast<int32_t>(odt_cpu.hue_linearity_search_range_(1));
    copy_jmh(odt_cpu.input_params_, odt_opencl.input_params_);
    copy_jmh(odt_cpu.reach_params_, odt_opencl.reach_params_);
    copy_jmh(odt_cpu.limit_params_, odt_opencl.limit_params_);
    if (params.to_output_enabled_ != 0U &&
        to_output_cpu.method_ == ColorUtils::ODTMethod::ACES_2_0) {
      const bool missing_tables = (!odt_cpu.table_reach_M_) || (!odt_cpu.table_hues_) ||
                                  (!odt_cpu.table_upper_hull_gammas_) ||
                                  (!odt_cpu.table_gamut_cusps_);
      if (missing_tables) {
        std::ostringstream oss;
        oss << "OpenCL fused params: ACES ODT tables are not initialized:";
        if (!odt_cpu.table_reach_M_) oss << " table_reach_M_";
        if (!odt_cpu.table_hues_) oss << " table_hues_";
        if (!odt_cpu.table_upper_hull_gammas_) oss << " table_upper_hull_gammas_";
        if (!odt_cpu.table_gamut_cusps_) oss << " table_gamut_cusps_";
        throw std::runtime_error(oss.str());
      }
    }
    if (odt_cpu.table_reach_M_) {
      std::copy_n(odt_cpu.table_reach_M_->data(), kOpenClAcesOdtTableSize,
                  odt_opencl.table_reach_M_);
    }
    if (odt_cpu.table_hues_) {
      std::copy_n(odt_cpu.table_hues_->data(), kOpenClAcesOdtTableSize, odt_opencl.table_hues_);
    }
    if (odt_cpu.table_upper_hull_gammas_) {
      std::copy_n(odt_cpu.table_upper_hull_gammas_->data(), kOpenClAcesOdtTableSize,
                  odt_opencl.table_upper_hull_gamma_);
    }
    if (odt_cpu.table_gamut_cusps_) {
      for (int i = 0; i < kOpenClAcesOdtTableSize; ++i) {
        const auto& cusp                    = (*odt_cpu.table_gamut_cusps_)[i];
        odt_opencl.table_gamut_cusps_[i][0] = cusp(0);
        odt_opencl.table_gamut_cusps_[i][1] = cusp(1);
        odt_opencl.table_gamut_cusps_[i][2] = cusp(2);
        odt_opencl.table_gamut_cusps_[i][3] = 0.0f;
      }
    }

    const auto& open_cpu        = to_output_cpu.open_drt_params_;
    auto&       open            = to_output.open_drt_params_;
    open.tn_hcon_enable_        = open_cpu.tn_hcon_enable_;
    open.tn_lcon_enable_        = open_cpu.tn_lcon_enable_;
    open.pt_enable_             = open_cpu.pt_enable_;
    open.ptl_enable_            = open_cpu.ptl_enable_;
    open.ptm_enable_            = open_cpu.ptm_enable_;
    open.brl_enable_            = open_cpu.brl_enable_;
    open.brlp_enable_           = open_cpu.brlp_enable_;
    open.hc_enable_             = open_cpu.hc_enable_;
    open.hs_rgb_enable_         = open_cpu.hs_rgb_enable_;
    open.hs_cmy_enable_         = open_cpu.hs_cmy_enable_;
    open.creative_white_        = open_cpu.creative_white_;
    open.surround_              = open_cpu.surround_;
    open.clamp_                 = open_cpu.clamp_;
    open.display_gamut_         = open_cpu.display_gamut_;
    open.display_eotf_          = open_cpu.display_eotf_;
    open.tn_con_                = open_cpu.tn_con_;
    open.tn_sh_                 = open_cpu.tn_sh_;
    open.tn_toe_                = open_cpu.tn_toe_;
    open.tn_off_                = open_cpu.tn_off_;
    open.tn_hcon_               = open_cpu.tn_hcon_;
    open.tn_hcon_pv_            = open_cpu.tn_hcon_pv_;
    open.tn_hcon_st_            = open_cpu.tn_hcon_st_;
    open.tn_lcon_               = open_cpu.tn_lcon_;
    open.tn_lcon_w_             = open_cpu.tn_lcon_w_;
    open.cwp_lm_                = open_cpu.cwp_lm_;
    open.rs_sa_                 = open_cpu.rs_sa_;
    open.rs_rw_                 = open_cpu.rs_rw_;
    open.rs_bw_                 = open_cpu.rs_bw_;
    open.pt_lml_                = open_cpu.pt_lml_;
    open.pt_lml_r_              = open_cpu.pt_lml_r_;
    open.pt_lml_g_              = open_cpu.pt_lml_g_;
    open.pt_lml_b_              = open_cpu.pt_lml_b_;
    open.pt_lmh_                = open_cpu.pt_lmh_;
    open.pt_lmh_r_              = open_cpu.pt_lmh_r_;
    open.pt_lmh_b_              = open_cpu.pt_lmh_b_;
    open.ptl_c_                 = open_cpu.ptl_c_;
    open.ptl_m_                 = open_cpu.ptl_m_;
    open.ptl_y_                 = open_cpu.ptl_y_;
    open.ptm_low_               = open_cpu.ptm_low_;
    open.ptm_low_rng_           = open_cpu.ptm_low_rng_;
    open.ptm_low_st_            = open_cpu.ptm_low_st_;
    open.ptm_high_              = open_cpu.ptm_high_;
    open.ptm_high_rng_          = open_cpu.ptm_high_rng_;
    open.ptm_high_st_           = open_cpu.ptm_high_st_;
    open.brl_                   = open_cpu.brl_;
    open.brl_r_                 = open_cpu.brl_r_;
    open.brl_g_                 = open_cpu.brl_g_;
    open.brl_b_                 = open_cpu.brl_b_;
    open.brl_rng_               = open_cpu.brl_rng_;
    open.brl_st_                = open_cpu.brl_st_;
    open.brlp_                  = open_cpu.brlp_;
    open.brlp_r_                = open_cpu.brlp_r_;
    open.brlp_g_                = open_cpu.brlp_g_;
    open.brlp_b_                = open_cpu.brlp_b_;
    open.hc_r_                  = open_cpu.hc_r_;
    open.hc_r_rng_              = open_cpu.hc_r_rng_;
    open.hs_r_                  = open_cpu.hs_r_;
    open.hs_r_rng_              = open_cpu.hs_r_rng_;
    open.hs_g_                  = open_cpu.hs_g_;
    open.hs_g_rng_              = open_cpu.hs_g_rng_;
    open.hs_b_                  = open_cpu.hs_b_;
    open.hs_b_rng_              = open_cpu.hs_b_rng_;
    open.hs_c_                  = open_cpu.hs_c_;
    open.hs_c_rng_              = open_cpu.hs_c_rng_;
    open.hs_m_                  = open_cpu.hs_m_;
    open.hs_m_rng_              = open_cpu.hs_m_rng_;
    open.hs_y_                  = open_cpu.hs_y_;
    open.hs_y_rng_              = open_cpu.hs_y_rng_;
    open.ts_x1_                 = open_cpu.ts_x1_;
    open.ts_y1_                 = open_cpu.ts_y1_;
    open.ts_x0_                 = open_cpu.ts_x0_;
    open.ts_y0_                 = open_cpu.ts_y0_;
    open.ts_s0_                 = open_cpu.ts_s0_;
    open.ts_p_                  = open_cpu.ts_p_;
    open.ts_s10_                = open_cpu.ts_s10_;
    open.ts_m1_                 = open_cpu.ts_m1_;
    open.ts_m2_                 = open_cpu.ts_m2_;
    open.ts_s_                  = open_cpu.ts_s_;
    open.ts_dsc_                = open_cpu.ts_dsc_;
    open.pt_cmp_Lf_             = open_cpu.pt_cmp_Lf_;
    open.s_Lp100_               = open_cpu.s_Lp100_;
    open.ts_s1_                 = open_cpu.ts_s1_;

    params.curve_enabled_       = fused_params.curve_enabled_ ? 1U : 0U;
    params.curve_ctrl_pts_size_ = fused_params.curve_ctrl_pts_size_;
    std::memcpy(params.curve_ctrl_pts_x_, fused_params.curve_ctrl_pts_x_,
                sizeof(params.curve_ctrl_pts_x_));
    std::memcpy(params.curve_ctrl_pts_y_, fused_params.curve_ctrl_pts_y_,
                sizeof(params.curve_ctrl_pts_y_));
    std::memcpy(params.curve_h_, fused_params.curve_h_, sizeof(params.curve_h_));
    std::memcpy(params.curve_m_, fused_params.curve_m_, sizeof(params.curve_m_));
    params.clarity_enabled_     = fused_params.clarity_enabled_ ? 1U : 0U;
    params.clarity_offset_      = fused_params.clarity_offset_;
    params.clarity_radius_      = fused_params.clarity_radius_;
    params.sharpen_enabled_     = fused_params.sharpen_enabled_ ? 1U : 0U;
    params.sharpen_offset_      = fused_params.sharpen_offset_;
    params.sharpen_radius_      = fused_params.sharpen_radius_;
    params.sharpen_threshold_   = fused_params.sharpen_threshold_;
    params.color_wheel_enabled_ = fused_params.color_wheel_enabled_ ? 1U : 0U;
    std::memcpy(params.lift_color_offset_, fused_params.lift_color_offset_,
                sizeof(params.lift_color_offset_));
    params.lift_luminance_offset_ = fused_params.lift_luminance_offset_;
    std::memcpy(params.gamma_color_offset_, fused_params.gamma_color_offset_,
                sizeof(params.gamma_color_offset_));
    params.gamma_luminance_offset_ = fused_params.gamma_luminance_offset_;
    std::memcpy(params.gain_color_offset_, fused_params.gain_color_offset_,
                sizeof(params.gain_color_offset_));
    params.gain_luminance_offset_ = fused_params.gain_luminance_offset_;
    return params;
  }

  static auto BuildPathIdentity(const std::filesystem::path& path) -> std::uintptr_t {
    std::error_code ec;
    const auto      abs_path   = std::filesystem::absolute(path, ec);
    const auto      normalized = (ec ? path : abs_path).lexically_normal();
    const auto      utf8       = normalized.generic_u8string();
    std::uintptr_t  h          = 0;
    for (const char8_t c : utf8) {
      h = h * 31u + static_cast<std::uintptr_t>(c);
    }
    return h;
  }

  static void UploadLmt(OperatorParams& cpu_params, OpenClFusedResources& resources) {
    if (!cpu_params.lmt_enabled_) {
      resources.lmt_lut_buffer_.Reset();
      resources.lmt_lut_edge_size_ = 0;
      resources.lmt_lut_source_id_ = 0;
      return;
    }
    if (cpu_params.lmt_lut_path_.empty()) {
      throw std::runtime_error("OpenCL fused params: LMT is enabled but lmt_lut_path_ is empty.");
    }

    // Check global path-based cache first (keyed by path + size + mtime).
    // This avoids re-parsing and re-uploading the LUT every frame when
    // to_lmt_dirty_ is spuriously true even though the file hasn't changed.
    const std::string cache_key = BuildOpenClLutPathCacheKey(cpu_params.lmt_lut_path_);
    {
      auto&                      cache = GetOpenClLutPathCache();
      std::lock_guard<std::mutex> lock(GetOpenClLutPathCacheMutex());
      const auto                 it = cache.find(cache_key);
      if (it != cache.end()) {
        resources.lmt_lut_buffer_    = it->second.buffer;
        resources.lmt_lut_edge_size_ = it->second.edge_size;
        resources.lmt_lut_source_id_ = BuildPathIdentity(cpu_params.lmt_lut_path_);
        cpu_params.to_lmt_dirty_     = false;
        return;
      }
    }

    // Fall back to the per-pipeline resource cache.
    const auto source_id = BuildPathIdentity(cpu_params.lmt_lut_path_);
    if (!cpu_params.to_lmt_dirty_ && resources.lmt_lut_buffer_.Get() != nullptr &&
        resources.lmt_lut_edge_size_ > 1U && resources.lmt_lut_source_id_ == source_id) {
      return;
    }

    CubeLut     lut;
    std::string error;
    if (!ParseCubeFile(cpu_params.lmt_lut_path_, lut, &error)) {
      const auto         utf8 = cpu_params.lmt_lut_path_.generic_u8string();
      std::ostringstream oss;
      oss << "OpenCL fused params: failed to parse LUT file '"
          << std::string(reinterpret_cast<const char*>(utf8.data()), utf8.size()) << "': " << error;
      throw std::runtime_error(oss.str());
    }
    if (!lut.Has3D()) {
      throw std::runtime_error("OpenCL fused params: only 3D LUTs are supported for OpenCL.");
    }

    const size_t       voxel_count = static_cast<size_t>(lut.edge3d_) * lut.edge3d_ * lut.edge3d_;
    std::vector<float> packed(voxel_count * 4);
    for (size_t i = 0; i < voxel_count; ++i) {
      packed[i * 4 + 0] = lut.lut3d_[i * 3 + 0];
      packed[i * 4 + 1] = lut.lut3d_[i * 3 + 1];
      packed[i * 4 + 2] = lut.lut3d_[i * 3 + 2];
      packed[i * 4 + 3] = 1.0f;
    }

    resources.lmt_lut_buffer_ =
        OpenClBuffer::CreateReadOnlyCopy(packed.data(), sizeof(float) * packed.size());
    resources.lmt_lut_edge_size_ = static_cast<uint32_t>(lut.edge3d_);
    resources.lmt_lut_source_id_ = source_id;
    cpu_params.to_lmt_dirty_     = false;

    // Store in global cache for future frames.
    {
      OpenClLutCacheEntry entry;
      entry.buffer    = resources.lmt_lut_buffer_;
      entry.edge_size = resources.lmt_lut_edge_size_;
      auto&                      cache = GetOpenClLutPathCache();
      std::lock_guard<std::mutex> lock(GetOpenClLutPathCacheMutex());
      cache.emplace(cache_key, entry);
    }
  }
};

}  // namespace alcedo::OpenCL::Pipeline

#endif
