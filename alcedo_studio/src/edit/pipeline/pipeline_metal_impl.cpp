//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifdef HAVE_METAL

#include <algorithm>
#include <array>
#include <chrono>
#include <cmath>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <iomanip>
#include <iostream>
#include <mutex>
#include <stdexcept>
#include <utility>
#include <vector>

#include "edit/operators/GPU_kernels/fused_param.hpp"
#include "edit/operators/GPU_kernels/metal_param.hpp"
#include "edit/pipeline/pipeline_gpu_wrapper.hpp"
#include "edit/scope/detail/scope_metal_shared.hpp"
#include "edit/scope/scope_analyzer.hpp"
#include "image/image_buffer.hpp"
#include "image/metal_image.hpp"
#include "metal/compute_pipeline_cache.hpp"
#include "metal/metal_context.hpp"
#include "ui/edit_viewer/frame_sink.hpp"

namespace alcedo {
namespace {
constexpr const char* kFusedPipelineKernelName         = "metal_fused_pipeline_rgba32f";
constexpr const char* kFusedStageKernelName            = "metal_fused_stage_rgba32f";
constexpr const char* kHsExtractLogIntensityKernelName = "metal_hs_extract_log_intensity_rgba32f";
constexpr const char* kHsExtractLogIntensityResampledKernelName =
    "metal_hs_extract_log_intensity_resampled_rgba32f";
constexpr const char* kHsBuildRemappedSamplesPackedKernelName =
    "metal_hs_build_remapped_samples_packed";
constexpr const char* kHsPyrDownKernelName       = "metal_hs_pyr_down";
constexpr const char* kHsPyrDownPackedKernelName = "metal_hs_pyr_down_packed";
constexpr const char* kHsSelectInterpolatedLevelPackedKernelName =
    "metal_hs_select_interpolated_level_packed";
constexpr const char* kHsCollapseLevelKernelName  = "metal_hs_collapse_level";
constexpr const char* kHsApplyAdjustedLKernelName = "metal_hs_apply_adjusted_l_rgba32f";
constexpr const char* kHsApplyAdjustedLFromFrameKernelName =
    "metal_hs_apply_adjusted_l_from_frame_rgba32f";
constexpr const char* kNeighborBlurHorizontalKernelName = "metal_neighbor_blur_h_rgba32f";
constexpr const char* kNeighborApplyVerticalKernelName  = "metal_neighbor_apply_v_rgba32f";
constexpr const char* kFusedPipelineDebugLabel          = "Metal fused pipeline";
constexpr const char* kFusedStageDebugLabel             = "Metal fused pipeline stage";
constexpr const char* kHsExtractLogIntensityDebugLabel  = "Metal H/S extract log intensity";
constexpr const char* kHsExtractLogIntensityResampledDebugLabel =
    "Metal H/S extract log intensity resampled";
constexpr const char* kHsBuildRemappedSamplesPackedDebugLabel = "Metal H/S remapped samples packed";
constexpr const char* kHsPyrDownDebugLabel                    = "Metal H/S pyr down";
constexpr const char* kHsPyrDownPackedDebugLabel              = "Metal H/S pyr down packed";
constexpr const char* kHsSelectInterpolatedLevelPackedDebugLabel = "Metal H/S select level packed";
constexpr const char* kHsCollapseLevelDebugLabel                 = "Metal H/S collapse level";
constexpr const char* kHsApplyAdjustedLDebugLabel                = "Metal H/S apply adjusted L";
constexpr const char* kHsApplyAdjustedLFromFrameDebugLabel =
    "Metal H/S apply adjusted L from frame";
constexpr const char* kNeighborBlurDebugLabel     = "Metal neighbor blur horizontal";
constexpr const char* kNeighborApplyDebugLabel    = "Metal neighbor apply vertical";
constexpr uint32_t    kMetalNeighborMaxTapCount   = 64;
constexpr int         kHsMaxLevels                = 12;
constexpr int         kHsMaxSamples               = 32;
constexpr float       kHsGammaMinL                = -0.15f;
constexpr float       kHsGammaMaxL                = 1.18f;
constexpr float       kHsBaseSigmaR               = 0.07545252f;
constexpr float       kHsGammaStepScale           = 1.35f;
constexpr float       kHsHighlightStrengthScale   = 1.5f;
constexpr float       kHsBackendAmountLimit       = 1.5f;
constexpr int         kHsReferenceMaskMaxLongEdge = 2048;
constexpr auto        kReportInterval             = std::chrono::milliseconds{500};
constexpr double      kFpsEmaAlpha                = 0.15;

enum class MetalNeighborOpKind : uint32_t {
  Sharpen = 1,
  Clarity = 2,
};

struct alignas(16) MetalNeighborStageParams {
  uint32_t                                     kind_        = 0;
  uint32_t                                     radius_      = 0;
  uint32_t                                     tap_count_   = 0;
  float                                        amount_      = 0.0f;
  float                                        threshold_   = 0.0f;
  float                                        reserved_[3] = {};
  std::array<float, kMetalNeighborMaxTapCount> weights_     = {};
};

static_assert(sizeof(MetalNeighborStageParams) ==
                  ((3U + 5U + kMetalNeighborMaxTapCount) * sizeof(float)),
              "MetalNeighborStageParams must stay ABI-compatible with Metal shaders.");

struct MetalNeighborStage {
  MetalNeighborStageParams params_ = {};
};

struct MetalHsLlfSample {
  float gamma_  = 0.0f;
  float target_ = 0.0f;
  float beta_   = 1.0f;
  float alpha_  = 1.0f;
};

struct MetalHsMaskDimensions {
  int32_t width_  = 1;
  int32_t height_ = 1;
};

struct alignas(16) MetalHsExtractParams {
  int32_t src_width_  = 0;
  int32_t src_height_ = 0;
  int32_t dst_width_  = 0;
  int32_t dst_height_ = 0;
};

struct alignas(16) MetalHsRemapParams {
  int32_t width_      = 0;
  int32_t height_     = 0;
  float   gamma_      = 0.0f;
  float   target_     = 0.0f;
  float   beta_       = 1.0f;
  float   alpha_      = 1.0f;
  float   sigma_r_    = kHsBaseSigmaR;
  int32_t dst_offset_ = 0;
};

struct alignas(16) MetalHsRemapPackedParams {
  int32_t                          width_        = 0;
  int32_t                          height_       = 0;
  int32_t                          sample_count_ = 0;
  int32_t                          reserved_     = 0;
  float                            sigma_r_      = kHsBaseSigmaR;
  std::array<float, kHsMaxSamples> gammas_       = {};
  std::array<float, kHsMaxSamples> targets_      = {};
  std::array<float, kHsMaxSamples> betas_        = {};
  std::array<float, kHsMaxSamples> alphas_       = {};
};

struct alignas(16) MetalHsPyrDownParams {
  int32_t src_width_   = 0;
  int32_t src_height_  = 0;
  int32_t dst_width_   = 0;
  int32_t dst_height_  = 0;
  int32_t src_offset_  = 0;
  int32_t dst_offset_  = 0;
  int32_t reserved_[2] = {};
};

struct alignas(16) MetalHsPyrDownPackedParams {
  int32_t src_width_    = 0;
  int32_t src_height_   = 0;
  int32_t dst_width_    = 0;
  int32_t dst_height_   = 0;
  int32_t sample_count_ = 0;
  int32_t reserved_[3]  = {};
};

struct alignas(16) MetalHsSelectPackedParams {
  int32_t                          width_         = 0;
  int32_t                          height_        = 0;
  int32_t                          coarse_width_  = 0;
  int32_t                          coarse_height_ = 0;
  int32_t                          sample_count_  = 0;
  int32_t                          top_level_     = 0;
  int32_t                          reserved_[2]   = {};
  std::array<float, kHsMaxSamples> gammas_        = {};
};

struct alignas(16) MetalHsPlaneApplyParams {
  int32_t width_           = 0;
  int32_t height_          = 0;
  int32_t adjusted_width_  = 0;
  int32_t adjusted_height_ = 0;
};

struct MetalExecutionStats {
  double input_prepare_ms      = 0.0;
  double fused_encode_ms       = 0.0;
  double hs_encode_ms          = 0.0;
  double hs_source_encode_ms   = 0.0;
  double hs_remap_encode_ms    = 0.0;
  double hs_select_encode_ms   = 0.0;
  double hs_collapse_encode_ms = 0.0;
  double hs_apply_encode_ms    = 0.0;
  double neighbor_encode_ms    = 0.0;
  double gpu_wait_ms           = 0.0;
  double host_download_ms      = 0.0;
  double host_copy_submit_ms   = 0.0;
  double output_wrap_ms        = 0.0;
  double total_ms              = 0.0;
  size_t detail_stage_count    = 0;
};

class MetalPreviewReporter {
 private:
  std::chrono::steady_clock::time_point last_report_time_{};
  double                                ema_fps_             = 0.0;
  double                                last_frame_ms_       = 0.0;
  double                                last_input_ms_       = 0.0;
  double                                last_fused_ms_       = 0.0;
  double                                last_hs_ms_          = 0.0;
  double                                last_hs_source_ms_   = 0.0;
  double                                last_hs_remap_ms_    = 0.0;
  double                                last_hs_select_ms_   = 0.0;
  double                                last_hs_collapse_ms_ = 0.0;
  double                                last_hs_apply_ms_    = 0.0;
  double                                last_neighbor_ms_    = 0.0;
  double                                last_gpu_wait_ms_    = 0.0;
  double                                last_download_ms_    = 0.0;
  double                                last_submit_ms_      = 0.0;
  double                                last_output_ms_      = 0.0;
  size_t                                last_stage_count_    = 0;
  size_t                                total_frames_        = 0;

 public:
  void Report(const MetalExecutionStats& stats) {
    const auto now        = std::chrono::steady_clock::now();

    last_frame_ms_        = stats.total_ms;
    last_input_ms_        = stats.input_prepare_ms;
    last_fused_ms_        = stats.fused_encode_ms;
    last_hs_ms_           = stats.hs_encode_ms;
    last_hs_source_ms_    = stats.hs_source_encode_ms;
    last_hs_remap_ms_     = stats.hs_remap_encode_ms;
    last_hs_select_ms_    = stats.hs_select_encode_ms;
    last_hs_collapse_ms_  = stats.hs_collapse_encode_ms;
    last_hs_apply_ms_     = stats.hs_apply_encode_ms;
    last_neighbor_ms_     = stats.neighbor_encode_ms;
    last_gpu_wait_ms_     = stats.gpu_wait_ms;
    last_download_ms_     = stats.host_download_ms;
    last_submit_ms_       = stats.host_copy_submit_ms;
    last_output_ms_       = stats.output_wrap_ms;
    last_stage_count_     = stats.detail_stage_count;

    const double inst_fps = (stats.total_ms > 0.0) ? (1000.0 / stats.total_ms) : 0.0;
    ema_fps_ =
        (ema_fps_ <= 0.0) ? inst_fps : (ema_fps_ * (1.0 - kFpsEmaAlpha) + inst_fps * kFpsEmaAlpha);
    ++total_frames_;

    if (last_report_time_.time_since_epoch().count() == 0) {
      last_report_time_ = now;
    }
    const bool force_report = std::getenv("ALCEDO_METAL_PROFILE_VERBOSE") != nullptr;
    if (!force_report && (now - last_report_time_) < kReportInterval) {
      return;
    }

    static std::mutex           print_mutex;
    std::lock_guard<std::mutex> guard(print_mutex);

    std::cout << "\r\033[2KMetal preview: " << std::fixed << std::setprecision(1) << ema_fps_
              << " fps"
              << " | last " << std::setprecision(2) << last_frame_ms_ << " ms"
              << " | parts in:" << last_input_ms_ << " fe:" << last_fused_ms_
              << " lt:" << last_hs_ms_ << " hs_src:" << last_hs_source_ms_
              << " hs_remap:" << last_hs_remap_ms_ << " hs_sel:" << last_hs_select_ms_
              << " hs_col:" << last_hs_collapse_ms_ << " hs_app:" << last_hs_apply_ms_
              << " ne:" << last_neighbor_ms_ << " gw:" << last_gpu_wait_ms_
              << " hd:" << last_download_ms_ << " sub:" << last_submit_ms_
              << " ow:" << last_output_ms_ << " | stages " << last_stage_count_ << " | frames "
              << total_frames_ << std::flush;

    last_report_time_ = now;
  }
};

auto MakeCommandBuffer() -> NS::SharedPtr<MTL::CommandBuffer> {
  auto* queue = MetalContext::Instance().Queue();
  if (queue == nullptr) {
    throw std::runtime_error("Metal fused pipeline: Metal queue is unavailable.");
  }
  auto command_buffer = NS::RetainPtr(queue->commandBuffer());
  if (!command_buffer) {
    throw std::runtime_error("Metal fused pipeline: failed to create command buffer.");
  }
  return command_buffer;
}

void DispatchThreads(MTL::ComputeCommandEncoder* encoder, MTL::ComputePipelineState* pipeline,
                     uint32_t width, uint32_t height) {
  const auto thread_width = std::max<NS::UInteger>(1, pipeline->threadExecutionWidth());
  const auto thread_height =
      std::max<NS::UInteger>(1, pipeline->maxTotalThreadsPerThreadgroup() / thread_width);
  const MTL::Size threads_per_group{thread_width, thread_height, 1};
  const MTL::Size threads_per_grid{width, height, 1};
  encoder->dispatchThreads(threads_per_grid, threads_per_group);
}

void DispatchSampleThreads(MTL::ComputeCommandEncoder* encoder, MTL::ComputePipelineState* pipeline,
                           uint32_t width, uint32_t height, uint32_t sample_count) {
  const auto thread_width = std::max<NS::UInteger>(1, pipeline->threadExecutionWidth());
  const auto thread_height =
      std::max<NS::UInteger>(1, pipeline->maxTotalThreadsPerThreadgroup() / thread_width);
  const MTL::Size threads_per_group{thread_width, thread_height, 1};
  const MTL::Size threads_per_grid{width, height, sample_count};
  encoder->dispatchThreads(threads_per_grid, threads_per_group);
}

auto MakeSharedBuffer(size_t length) -> NS::SharedPtr<MTL::Buffer> {
  auto* device = MetalContext::Instance().Device();
  if (device == nullptr) {
    throw std::runtime_error("Metal fused pipeline: Metal device is unavailable.");
  }

  auto buffer = NS::TransferPtr(
      device->newBuffer(static_cast<NS::UInteger>(length), MTL::ResourceStorageModeShared));
  if (!buffer) {
    throw std::runtime_error("Metal fused pipeline: failed to allocate shared buffer.");
  }
  return buffer;
}

auto MakeDeviceBuffer(size_t length) -> NS::SharedPtr<MTL::Buffer> {
  auto* device = MetalContext::Instance().Device();
  if (device == nullptr) {
    throw std::runtime_error("Metal fused pipeline: Metal device is unavailable.");
  }

  auto buffer = NS::TransferPtr(
      device->newBuffer(static_cast<NS::UInteger>(length), MTL::ResourceStorageModePrivate));
  if (!buffer) {
    throw std::runtime_error("Metal fused pipeline: failed to allocate device buffer.");
  }
  return buffer;
}

auto UploadStageParams(const MetalNeighborStageParams& params) -> NS::SharedPtr<MTL::Buffer> {
  auto buffer = MakeSharedBuffer(sizeof(MetalNeighborStageParams));
  std::memcpy(buffer->contents(), &params, sizeof(MetalNeighborStageParams));
  return buffer;
}

auto ResolveViewerDisplayConfig(const OperatorParams& params) -> ViewerDisplayConfig {
  return ViewerDisplayConfig{params.to_output_params_.encoding_space_,
                             params.to_output_params_.eotf_};
}

auto BuildGaussianWeights(float sigma, uint32_t radius)
    -> std::array<float, kMetalNeighborMaxTapCount> {
  std::array<float, kMetalNeighborMaxTapCount> weights{};
  const double safe_sigma  = std::max(static_cast<double>(sigma), 1.0e-4);
  const double inv2sigma2  = 0.5 / (safe_sigma * safe_sigma);
  double       full_weight = 1.0;

  weights[0]               = 1.0f;
  for (uint32_t tap = 1; tap <= radius; ++tap) {
    const double w = std::exp(-(static_cast<double>(tap) * static_cast<double>(tap)) * inv2sigma2);
    weights[tap]   = static_cast<float>(w);
    full_weight += 2.0 * w;
  }

  if (full_weight > 0.0) {
    for (uint32_t tap = 0; tap <= radius; ++tap) {
      weights[tap] = static_cast<float>(static_cast<double>(weights[tap]) / full_weight);
    }
  }

  return weights;
}

auto BuildNeighborStageParams(MetalNeighborOpKind kind, float sigma, float amount, float threshold,
                              int gaussian_tap_count, const float* gaussian_weights)
    -> MetalNeighborStageParams {
  MetalNeighborStageParams params;

  params.kind_      = static_cast<uint32_t>(kind);
  params.amount_    = amount;
  params.threshold_ = threshold;

  const int clamped_tap_count =
      std::clamp(gaussian_tap_count, 0, static_cast<int>(kMetalNeighborMaxTapCount));
  if (clamped_tap_count > 0 && gaussian_weights != nullptr) {
    params.tap_count_ = static_cast<uint32_t>(clamped_tap_count);
    params.radius_    = params.tap_count_ - 1U;
    std::copy_n(gaussian_weights, clamped_tap_count, params.weights_.begin());
    return params;
  }

  if (sigma <= 0.0f) {
    return params;
  }

  const float    safe_sigma = std::max(sigma, 1.0e-4f);
  const uint32_t max_radius =
      (kMetalNeighborMaxTapCount > 0U) ? (kMetalNeighborMaxTapCount - 1U) : 0U;
  params.radius_ =
      std::clamp<uint32_t>(static_cast<uint32_t>(std::ceil(3.0f * safe_sigma)), 1U, max_radius);
  params.tap_count_ = params.radius_ + 1U;
  params.weights_   = BuildGaussianWeights(safe_sigma, params.radius_);
  return params;
}

class MetalGPUPipeline final : public GPUPipelineImpl {
 private:
  std::shared_ptr<ImageBuffer>             input_img_;
  OperatorParams*                          cpu_params_                                = nullptr;
  IFrameSink*                              frame_sink_                                = nullptr;
  FusedOperatorParams                      fused_params_                              = {};
  metal::MetalFusedResources               resources_                                 = {};
  NS::SharedPtr<MTL::ComputePipelineState> fused_pipeline_                            = nullptr;
  NS::SharedPtr<MTL::ComputePipelineState> fused_stage_pipeline_                      = nullptr;
  NS::SharedPtr<MTL::ComputePipelineState> hs_extract_pipeline_                       = nullptr;
  NS::SharedPtr<MTL::ComputePipelineState> hs_extract_resampled_pipeline_             = nullptr;
  NS::SharedPtr<MTL::ComputePipelineState> hs_build_remapped_samples_packed_pipeline_ = nullptr;
  NS::SharedPtr<MTL::ComputePipelineState> hs_pyr_down_pipeline_                      = nullptr;
  NS::SharedPtr<MTL::ComputePipelineState> hs_pyr_down_packed_pipeline_               = nullptr;
  NS::SharedPtr<MTL::ComputePipelineState> hs_select_level_packed_pipeline_           = nullptr;
  NS::SharedPtr<MTL::ComputePipelineState> hs_collapse_level_pipeline_                = nullptr;
  NS::SharedPtr<MTL::ComputePipelineState> hs_apply_adjusted_l_pipeline_              = nullptr;
  NS::SharedPtr<MTL::ComputePipelineState> hs_apply_adjusted_l_from_frame_pipeline_   = nullptr;
  NS::SharedPtr<MTL::ComputePipelineState> neighbor_blur_horizontal_pipeline_         = nullptr;
  NS::SharedPtr<MTL::ComputePipelineState> neighbor_apply_vertical_pipeline_          = nullptr;
  metal::MetalImage                        pre_hs_working_;
  metal::MetalImage                        hs_working_;
  std::array<NS::SharedPtr<MTL::Buffer>, kHsMaxLevels> hs_source_levels_         = {};
  std::array<NS::SharedPtr<MTL::Buffer>, kHsMaxLevels> hs_remap_a_levels_        = {};
  std::array<NS::SharedPtr<MTL::Buffer>, kHsMaxLevels> hs_sample_levels_         = {};
  std::array<NS::SharedPtr<MTL::Buffer>, kHsMaxLevels> hs_output_levels_         = {};
  std::array<int32_t, kHsMaxLevels>                    hs_level_widths_          = {};
  std::array<int32_t, kHsMaxLevels>                    hs_level_heights_         = {};
  int32_t                                              hs_level_count_           = 0;
  int32_t                                              hs_sample_count_          = 0;
  int32_t                                              hs_cached_width_          = 0;
  int32_t                                              hs_cached_height_         = 0;
  int32_t                                              hs_cached_frame_width_    = 0;
  int32_t                                              hs_cached_frame_height_   = 0;
  int32_t                                              hs_cached_pitch_          = 0;
  std::uint64_t                                        hs_cached_key_            = 0;
  bool                                                 hs_cached_reference_base_ = false;
  MetalPreviewReporter                                 preview_reporter_;

  void                                                 EnsureMetalInput() {
    if (!input_img_) {
      throw std::runtime_error("Metal fused pipeline: input image is null.");
    }
    if (!input_img_->gpu_data_valid_) {
      if (!input_img_->cpu_data_valid_) {
        throw std::runtime_error("Metal fused pipeline: input image has no valid CPU or GPU data.");
      }
      input_img_->SyncToGPU();
    }
    if (input_img_->GetGPUType() != CV_32FC4) {
      input_img_->ConvertGPUDataTo(CV_32FC4);
    }
  }

  auto GetPipelineState(const char* kernel_name, const char* debug_label)
      -> NS::SharedPtr<MTL::ComputePipelineState> {
#ifndef ALCEDO_METAL_FUSED_PIPELINE_METALLIB_PATH
    throw std::runtime_error("Metal fused pipeline metallib path is not configured.");
#else
    return metal::ComputePipelineCache::Instance().GetPipelineState(
        ALCEDO_METAL_FUSED_PIPELINE_METALLIB_PATH, kernel_name, debug_label);
#endif
  }

  void InvalidateHsBaseCache() {
    hs_cached_width_          = 0;
    hs_cached_height_         = 0;
    hs_cached_frame_width_    = 0;
    hs_cached_frame_height_   = 0;
    hs_cached_pitch_          = 0;
    hs_cached_key_            = 0;
    hs_cached_reference_base_ = false;
  }

  void ReleaseHsPyramidBuffers() {
    for (auto& buffer : hs_source_levels_) {
      buffer = nullptr;
    }
    for (auto& buffer : hs_remap_a_levels_) {
      buffer = nullptr;
    }
    for (auto& buffer : hs_sample_levels_) {
      buffer = nullptr;
    }
    for (auto& buffer : hs_output_levels_) {
      buffer = nullptr;
    }
    hs_level_widths_.fill(0);
    hs_level_heights_.fill(0);
    hs_level_count_  = 0;
    hs_sample_count_ = 0;
    InvalidateHsBaseCache();
  }

  static auto FloatBits(float value) -> std::uint32_t {
    std::uint32_t bits = 0;
    std::memcpy(&bits, &value, sizeof(bits));
    return bits;
  }

  static void HashCombine(std::uint64_t& seed, std::uint64_t value) {
    seed ^= value + 0x9e3779b97f4a7c15ull + (seed << 6) + (seed >> 2);
  }

  static auto BuildAdjustedResultCacheKey(const FusedOperatorParams& params, float shadow_amount,
                                          float highlight_amount) -> std::uint64_t {
    std::uint64_t key = params.hs_mask_base_cache_key_;
    HashCombine(key, static_cast<std::uint64_t>(params.shadows_enabled_));
    HashCombine(key, static_cast<std::uint64_t>(params.highlights_enabled_));
    HashCombine(key, static_cast<std::uint64_t>(FloatBits(shadow_amount)));
    HashCombine(key, static_cast<std::uint64_t>(FloatBits(highlight_amount)));
    HashCombine(key, static_cast<std::uint64_t>(params.render_roi_enabled_));
    if (params.render_roi_enabled_) {
      HashCombine(key, static_cast<std::uint64_t>(params.render_roi_x_));
      HashCombine(key, static_cast<std::uint64_t>(params.render_roi_y_));
      HashCombine(key, static_cast<std::uint64_t>(FloatBits(params.render_roi_scale_x_)));
      HashCombine(key, static_cast<std::uint64_t>(FloatBits(params.render_roi_scale_y_)));
      HashCombine(key, static_cast<std::uint64_t>(params.render_roi_reference_width_));
      HashCombine(key, static_cast<std::uint64_t>(params.render_roi_reference_height_));
    }
    return key;
  }

  static auto ComputeHsMaskDimensions(int width, int height, bool roi_frame_with_source_reference)
      -> MetalHsMaskDimensions {
    const int max_long_edge =
        roi_frame_with_source_reference ? std::max(width, height) : kHsReferenceMaskMaxLongEdge;
    const float scale = std::min(1.0f, static_cast<float>(std::max(1, max_long_edge)) /
                                           static_cast<float>(std::max(width, height)));
    return {std::max(1, static_cast<int>(std::ceil(static_cast<float>(width) * scale))),
            std::max(1, static_cast<int>(std::ceil(static_cast<float>(height) * scale)))};
  }

  static auto ComputeHsLevelCount(int width, int height, float radius) -> int {
    const int radius_levels = std::max(
        3,
        std::min(kHsMaxLevels, static_cast<int>(std::ceil(std::log2(std::max(radius, 1.0f)))) + 2));
    int count = 1;
    int w     = width;
    int h     = height;
    while (count < radius_levels && (w > 1 || h > 1)) {
      w = std::max(1, (w + 1) / 2);
      h = std::max(1, (h + 1) / 2);
      ++count;
    }
    return count;
  }

  static auto HsLerp(float a, float b, float t) -> float { return a + (b - a) * t; }

  static auto HsSegment(float x, float x0, float y0, float x1, float y1) -> float {
    const float t = std::clamp((x - x0) / std::max(x1 - x0, 1.0e-6f), 0.0f, 1.0f);
    return HsLerp(y0, y1, t);
  }

  static auto HsShadowProfileEv(float relative_ev) -> float {
    if (relative_ev <= -9.0f) return 0.02f;
    if (relative_ev <= -7.0f) return HsSegment(relative_ev, -9.0f, 0.02f, -7.0f, 0.35f);
    if (relative_ev <= -5.4f) return HsSegment(relative_ev, -7.0f, 0.35f, -5.4f, 0.82f);
    if (relative_ev <= -4.3f) return HsSegment(relative_ev, -5.4f, 0.82f, -4.3f, 0.98f);
    if (relative_ev <= -3.1f) return HsSegment(relative_ev, -4.3f, 0.98f, -3.1f, 0.72f);
    if (relative_ev <= -2.0f) return HsSegment(relative_ev, -3.1f, 0.72f, -2.0f, 0.42f);
    if (relative_ev <= -0.5f) return HsSegment(relative_ev, -2.0f, 0.42f, -0.5f, 0.08f);
    if (relative_ev <= 1.0f) return HsSegment(relative_ev, -0.5f, 0.08f, 1.0f, 0.0f);
    return 0.0f;
  }

  static auto HsHighlightProfileEv(float relative_ev) -> float {
    if (relative_ev <= -1.0f) return 0.0f;
    if (relative_ev <= 0.0f) return HsSegment(relative_ev, -1.0f, 0.0f, 0.0f, 0.03f);
    if (relative_ev <= 1.2f) return HsSegment(relative_ev, 0.0f, 0.03f, 1.2f, 0.22f);
    if (relative_ev <= 2.8f) return HsSegment(relative_ev, 1.2f, 0.22f, 2.8f, 0.60f);
    if (relative_ev <= 4.5f) return HsSegment(relative_ev, 2.8f, 0.60f, 4.5f, 0.95f);
    if (relative_ev <= 6.5f) return HsSegment(relative_ev, 4.5f, 0.95f, 6.5f, 1.08f);
    if (relative_ev <= 8.0f) return HsSegment(relative_ev, 6.5f, 1.08f, 8.0f, 0.92f);
    return 0.92f;
  }

  static auto Smoothstep(float edge0, float edge1, float x) -> float {
    const float t = std::clamp((x - edge0) / std::max(edge1 - edge0, 1.0e-6f), 0.0f, 1.0f);
    return t * t * (3.0f - 2.0f * t);
  }

  static auto HsRelativeEv(float log_intensity) -> float {
    return (log_intensity - 0.41358840f) * 17.52f;
  }

  static auto HsApplyReferenceCurve(float reference_l, float shadow_amount, float highlight_amount)
      -> float {
    const float relative_ev = HsRelativeEv(reference_l);
    const float shadow_lift = std::max(shadow_amount, 0.0f) * HsShadowProfileEv(relative_ev);
    const float shadow_darken =
        std::max(-shadow_amount, 0.0f) * 0.55f * HsShadowProfileEv(relative_ev);
    const float highlight_reduce = std::max(highlight_amount, 0.0f) * kHsHighlightStrengthScale *
                                   HsHighlightProfileEv(relative_ev);
    const float highlight_boost =
        std::max(-highlight_amount, 0.0f) * 0.65f * HsHighlightProfileEv(relative_ev);
    const float practical_dark =
        Smoothstep(-5.85f, -3.95f, relative_ev) * (1.0f - Smoothstep(-3.20f, -1.65f, relative_ev));
    const float fill_plateau = Smoothstep(-5.55f, -3.30f, relative_ev) *
                               (1.0f - 0.45f * Smoothstep(-2.65f, -0.20f, relative_ev));
    const float deep_toe_fill =
        shadow_lift * (1.0f - Smoothstep(-7.35f, -4.95f, relative_ev)) * 0.28f;
    const float shadow_fill_lift =
        shadow_lift * (0.62f * practical_dark + 0.14f * fill_plateau) + deep_toe_fill;
    const float lifted_relative_ev = relative_ev + 0.24f * (shadow_lift + 0.84f * shadow_fill_lift);
    const float combo_shadow_rollback =
        ((shadow_lift > 1.0e-6f && highlight_reduce > 1.0e-6f) ? 1.0f : 0.0f) * shadow_fill_lift *
        Smoothstep(-2.00f, -0.60f, lifted_relative_ev) *
        (1.0f - Smoothstep(0.10f, 1.30f, lifted_relative_ev)) * 1.08f;
    const float combo_low_mid_darken = std::min(shadow_lift + shadow_fill_lift, highlight_reduce) *
                                       Smoothstep(-2.45f, -0.90f, lifted_relative_ev) *
                                       (1.0f - Smoothstep(0.50f, 1.95f, lifted_relative_ev)) *
                                       1.30f;
    const float delta_ev = shadow_lift + shadow_fill_lift - combo_shadow_rollback - shadow_darken -
                           highlight_reduce - combo_low_mid_darken + highlight_boost;
    return reference_l + delta_ev * (1.0f / 17.52f);
  }

  static auto HsDetailAlpha(float reference_l, float shadow_amount, float highlight_amount)
      -> float {
    (void)highlight_amount;
    const float relative_ev = HsRelativeEv(reference_l);
    const float deep_shadow = 1.0f - Smoothstep(-5.7f, -4.1f, relative_ev);
    const float mid_shadow =
        Smoothstep(-5.0f, -3.6f, relative_ev) * (1.0f - Smoothstep(-2.4f, -1.0f, relative_ev));
    const float lift_amount = std::max(shadow_amount, 0.0f);
    return 1.0f + 0.40f * lift_amount * deep_shadow - 0.14f * lift_amount * mid_shadow;
  }

  static auto HsToneBeta(float reference_l, float shadow_amount, float highlight_amount) -> float {
    constexpr float kEps = 0.035f;
    const float     lo = HsApplyReferenceCurve(reference_l - kEps, shadow_amount, highlight_amount);
    const float     hi = HsApplyReferenceCurve(reference_l + kEps, shadow_amount, highlight_amount);
    return std::clamp((hi - lo) / (2.0f * kEps), 0.08f, 1.70f);
  }

  static auto BuildHsSamples(float shadow_amount, float highlight_amount)
      -> std::vector<MetalHsLlfSample> {
    const float sample_step = std::max(kHsBaseSigmaR * kHsGammaStepScale, 0.045f);
    const int   sample_count =
        std::max(2, static_cast<int>(std::ceil((kHsGammaMaxL - kHsGammaMinL) / sample_step)) + 1);
    std::vector<MetalHsLlfSample> samples;
    samples.reserve(static_cast<size_t>(sample_count));
    for (int i = 0; i < sample_count; ++i) {
      const float t =
          sample_count == 1 ? 0.0f : static_cast<float>(i) / static_cast<float>(sample_count - 1);
      const float gamma = HsLerp(kHsGammaMinL, kHsGammaMaxL, t);
      samples.push_back({gamma, HsApplyReferenceCurve(gamma, shadow_amount, highlight_amount),
                         HsToneBeta(gamma, shadow_amount, highlight_amount),
                         HsDetailAlpha(gamma, shadow_amount, highlight_amount)});
    }
    return samples;
  }

  static auto HsLevelElems(int32_t width, int32_t height) -> size_t {
    return static_cast<size_t>(width) * static_cast<size_t>(height);
  }

  void EnsureHsPyramidBuffers(int32_t width, int32_t height, float radius) {
    if (width <= 0 || height <= 0) {
      throw std::runtime_error("Metal fused pipeline: invalid H/S pyramid dimensions.");
    }

    const int                         new_level_count = ComputeHsLevelCount(width, height, radius);
    std::array<int32_t, kHsMaxLevels> new_widths      = {};
    std::array<int32_t, kHsMaxLevels> new_heights     = {};
    new_widths[0]                                     = width;
    new_heights[0]                                    = height;
    for (int level = 1; level < new_level_count; ++level) {
      new_widths[level]  = std::max<int32_t>(1, (new_widths[level - 1] + 1) / 2);
      new_heights[level] = std::max<int32_t>(1, (new_heights[level - 1] + 1) / 2);
    }

    bool layout_matches = hs_level_count_ == new_level_count;
    for (int level = 0; layout_matches && level < new_level_count; ++level) {
      layout_matches = hs_level_widths_[level] == new_widths[level] &&
                       hs_level_heights_[level] == new_heights[level] &&
                       hs_source_levels_[level].get() != nullptr &&
                       hs_remap_a_levels_[level].get() != nullptr &&
                       hs_output_levels_[level].get() != nullptr;
    }
    if (layout_matches) {
      return;
    }

    ReleaseHsPyramidBuffers();
    hs_level_count_   = new_level_count;
    hs_level_widths_  = new_widths;
    hs_level_heights_ = new_heights;
    for (int level = 0; level < hs_level_count_; ++level) {
      const size_t elems        = HsLevelElems(hs_level_widths_[level], hs_level_heights_[level]);
      const size_t bytes        = elems * sizeof(float);
      hs_source_levels_[level]  = MakeDeviceBuffer(bytes);
      hs_remap_a_levels_[level] = MakeDeviceBuffer(bytes);
      hs_output_levels_[level]  = MakeDeviceBuffer(bytes);
    }
  }

  void EnsureHsSamplePyramidBuffers(int32_t sample_count) {
    if (sample_count < 2 || sample_count > kHsMaxSamples) {
      throw std::runtime_error("Metal fused pipeline: invalid H/S sample count.");
    }

    bool layout_matches = hs_sample_count_ == sample_count;
    for (int level = 0; layout_matches && level < hs_level_count_; ++level) {
      layout_matches = hs_sample_levels_[level].get() != nullptr;
    }
    if (layout_matches) {
      return;
    }

    for (auto& buffer : hs_sample_levels_) {
      buffer = nullptr;
    }
    hs_sample_count_ = sample_count;
    for (int level = 0; level < hs_level_count_; ++level) {
      const size_t elems = HsLevelElems(hs_level_widths_[level], hs_level_heights_[level]);
      hs_sample_levels_[level] =
          MakeDeviceBuffer(elems * static_cast<size_t>(sample_count) * sizeof(float));
    }
    InvalidateHsBaseCache();
  }

  void EncodeFusedKernel(MTL::CommandBuffer* command_buffer, const metal::MetalImage& src,
                         metal::MetalImage& dst) {
    if (!fused_pipeline_) {
      fused_pipeline_ = GetPipelineState(kFusedPipelineKernelName, kFusedPipelineDebugLabel);
    }
    auto encoder = NS::RetainPtr(command_buffer->computeCommandEncoder());
    encoder->setComputePipelineState(fused_pipeline_.get());
    encoder->setTexture(src.Texture(), 0);
    encoder->setTexture(dst.Texture(), 1);
    encoder->setBuffer(resources_.params_buffer_.get(), 0, 0);
    encoder->setBuffer(resources_.lmt_lut_.buffer_.get(), 0, 1);
    DispatchThreads(encoder.get(), fused_pipeline_.get(), src.Width(), src.Height());
    encoder->endEncoding();
  }

  void EncodeFusedStageKernel(MTL::CommandBuffer* command_buffer, const metal::MetalImage& src,
                              metal::MetalImage& dst, int32_t stage) {
    if (!fused_stage_pipeline_) {
      fused_stage_pipeline_ = GetPipelineState(kFusedStageKernelName, kFusedStageDebugLabel);
    }

    dst.Create(src.Width(), src.Height(), src.Format(), true, true, false);

    auto encoder = NS::RetainPtr(command_buffer->computeCommandEncoder());
    encoder->setComputePipelineState(fused_stage_pipeline_.get());
    encoder->setTexture(src.Texture(), 0);
    encoder->setTexture(dst.Texture(), 1);
    encoder->setBuffer(resources_.params_buffer_.get(), 0, 0);
    encoder->setBuffer(resources_.lmt_lut_.buffer_.get(), 0, 1);
    encoder->setBytes(&stage, sizeof(stage), 2);
    DispatchThreads(encoder.get(), fused_stage_pipeline_.get(), src.Width(), src.Height());
    encoder->endEncoding();
  }

  void EncodeHsExtractLogIntensity(MTL::CommandBuffer*      command_buffer,
                                   const metal::MetalImage& src) {
    if (!hs_extract_pipeline_) {
      hs_extract_pipeline_ =
          GetPipelineState(kHsExtractLogIntensityKernelName, kHsExtractLogIntensityDebugLabel);
    }

    const MetalHsExtractParams params{static_cast<int32_t>(src.Width()),
                                      static_cast<int32_t>(src.Height()), hs_level_widths_[0],
                                      hs_level_heights_[0]};
    auto                       encoder = NS::RetainPtr(command_buffer->computeCommandEncoder());
    encoder->setComputePipelineState(hs_extract_pipeline_.get());
    encoder->setTexture(src.Texture(), 0);
    encoder->setBuffer(hs_source_levels_[0].get(), 0, 0);
    encoder->setBytes(&params, sizeof(params), 1);
    DispatchThreads(encoder.get(), hs_extract_pipeline_.get(),
                    static_cast<uint32_t>(hs_level_widths_[0]),
                    static_cast<uint32_t>(hs_level_heights_[0]));
    encoder->endEncoding();
  }

  void EncodeHsExtractLogIntensityResampled(MTL::CommandBuffer*      command_buffer,
                                            const metal::MetalImage& src) {
    if (!hs_extract_resampled_pipeline_) {
      hs_extract_resampled_pipeline_ = GetPipelineState(kHsExtractLogIntensityResampledKernelName,
                                                        kHsExtractLogIntensityResampledDebugLabel);
    }

    const MetalHsExtractParams params{static_cast<int32_t>(src.Width()),
                                      static_cast<int32_t>(src.Height()), hs_level_widths_[0],
                                      hs_level_heights_[0]};
    auto                       encoder = NS::RetainPtr(command_buffer->computeCommandEncoder());
    encoder->setComputePipelineState(hs_extract_resampled_pipeline_.get());
    encoder->setTexture(src.Texture(), 0);
    encoder->setBuffer(hs_source_levels_[0].get(), 0, 0);
    encoder->setBytes(&params, sizeof(params), 1);
    DispatchThreads(encoder.get(), hs_extract_resampled_pipeline_.get(),
                    static_cast<uint32_t>(hs_level_widths_[0]),
                    static_cast<uint32_t>(hs_level_heights_[0]));
    encoder->endEncoding();
  }

  void EncodeHsPyrDown(MTL::CommandBuffer* command_buffer, MTL::Buffer* src, int32_t src_width,
                       int32_t src_height, int32_t src_offset, MTL::Buffer* dst, int32_t dst_width,
                       int32_t dst_height, int32_t dst_offset) {
    if (!hs_pyr_down_pipeline_) {
      hs_pyr_down_pipeline_ = GetPipelineState(kHsPyrDownKernelName, kHsPyrDownDebugLabel);
    }

    const MetalHsPyrDownParams params{src_width,  src_height, dst_width, dst_height,
                                      src_offset, dst_offset, {0, 0}};
    auto                       encoder = NS::RetainPtr(command_buffer->computeCommandEncoder());
    encoder->setComputePipelineState(hs_pyr_down_pipeline_.get());
    encoder->setBuffer(src, 0, 0);
    encoder->setBuffer(dst, 0, 1);
    encoder->setBytes(&params, sizeof(params), 2);
    DispatchThreads(encoder.get(), hs_pyr_down_pipeline_.get(), static_cast<uint32_t>(dst_width),
                    static_cast<uint32_t>(dst_height));
    encoder->endEncoding();
  }

  void BuildHsSourcePyramid(MTL::CommandBuffer* command_buffer, const metal::MetalImage& src) {
    if (hs_level_widths_[0] == static_cast<int32_t>(src.Width()) &&
        hs_level_heights_[0] == static_cast<int32_t>(src.Height())) {
      EncodeHsExtractLogIntensity(command_buffer, src);
    } else {
      EncodeHsExtractLogIntensityResampled(command_buffer, src);
    }
    for (int level = 1; level < hs_level_count_; ++level) {
      EncodeHsPyrDown(command_buffer, hs_source_levels_[level - 1].get(),
                      hs_level_widths_[level - 1], hs_level_heights_[level - 1], 0,
                      hs_source_levels_[level].get(), hs_level_widths_[level],
                      hs_level_heights_[level], 0);
    }
  }

  void EncodeHsBuildPackedSamplesLevel0(MTL::CommandBuffer*                  command_buffer,
                                        const std::vector<MetalHsLlfSample>& samples) {
    if (!hs_build_remapped_samples_packed_pipeline_) {
      hs_build_remapped_samples_packed_pipeline_ = GetPipelineState(
          kHsBuildRemappedSamplesPackedKernelName, kHsBuildRemappedSamplesPackedDebugLabel);
    }

    MetalHsRemapPackedParams params;
    params.width_        = hs_level_widths_[0];
    params.height_       = hs_level_heights_[0];
    params.sample_count_ = static_cast<int32_t>(samples.size());
    params.sigma_r_      = kHsBaseSigmaR;
    for (size_t i = 0; i < samples.size(); ++i) {
      params.gammas_[i]  = samples[i].gamma_;
      params.targets_[i] = samples[i].target_;
      params.betas_[i]   = samples[i].beta_;
      params.alphas_[i]  = samples[i].alpha_;
    }

    auto encoder = NS::RetainPtr(command_buffer->computeCommandEncoder());
    encoder->setComputePipelineState(hs_build_remapped_samples_packed_pipeline_.get());
    encoder->setBuffer(hs_source_levels_[0].get(), 0, 0);
    encoder->setBuffer(hs_sample_levels_[0].get(), 0, 1);
    encoder->setBytes(&params, sizeof(params), 2);
    DispatchSampleThreads(encoder.get(), hs_build_remapped_samples_packed_pipeline_.get(),
                          static_cast<uint32_t>(hs_level_widths_[0]),
                          static_cast<uint32_t>(hs_level_heights_[0]),
                          static_cast<uint32_t>(samples.size()));
    encoder->endEncoding();
  }

  void EncodeHsPackedSamplesPyrDown(MTL::CommandBuffer* command_buffer, int level,
                                    int32_t sample_count) {
    if (!hs_pyr_down_packed_pipeline_) {
      hs_pyr_down_packed_pipeline_ =
          GetPipelineState(kHsPyrDownPackedKernelName, kHsPyrDownPackedDebugLabel);
    }

    const MetalHsPyrDownPackedParams params{hs_level_widths_[level - 1],
                                            hs_level_heights_[level - 1],
                                            hs_level_widths_[level],
                                            hs_level_heights_[level],
                                            sample_count,
                                            {0, 0, 0}};
    auto encoder = NS::RetainPtr(command_buffer->computeCommandEncoder());
    encoder->setComputePipelineState(hs_pyr_down_packed_pipeline_.get());
    encoder->setBuffer(hs_sample_levels_[level - 1].get(), 0, 0);
    encoder->setBuffer(hs_sample_levels_[level].get(), 0, 1);
    encoder->setBytes(&params, sizeof(params), 2);
    DispatchSampleThreads(encoder.get(), hs_pyr_down_packed_pipeline_.get(),
                          static_cast<uint32_t>(hs_level_widths_[level]),
                          static_cast<uint32_t>(hs_level_heights_[level]),
                          static_cast<uint32_t>(sample_count));
    encoder->endEncoding();
  }

  void BuildHsPackedSamplePyramids(MTL::CommandBuffer*                  command_buffer,
                                   const std::vector<MetalHsLlfSample>& samples) {
    const int32_t sample_count = static_cast<int32_t>(samples.size());
    EncodeHsBuildPackedSamplesLevel0(command_buffer, samples);
    for (int level = 1; level < hs_level_count_; ++level) {
      EncodeHsPackedSamplesPyrDown(command_buffer, level, sample_count);
    }
  }

  void ClearHsOutputPyramid(MTL::CommandBuffer* command_buffer) {
    auto blit = NS::RetainPtr(command_buffer->blitCommandEncoder());
    for (int level = 0; level < hs_level_count_; ++level) {
      const size_t elems = static_cast<size_t>(hs_level_widths_[level]) *
                           static_cast<size_t>(hs_level_heights_[level]);
      blit->fillBuffer(hs_output_levels_[level].get(), NS::Range::Make(0, elems * sizeof(float)),
                       0);
    }
    blit->endEncoding();
  }

  void EncodeHsSelectPackedLevel(MTL::CommandBuffer* command_buffer, int level,
                                 const std::vector<MetalHsLlfSample>& samples) {
    if (!hs_select_level_packed_pipeline_) {
      hs_select_level_packed_pipeline_ = GetPipelineState(
          kHsSelectInterpolatedLevelPackedKernelName, kHsSelectInterpolatedLevelPackedDebugLabel);
    }

    const bool                top_level = level == (hs_level_count_ - 1);
    MetalHsSelectPackedParams params;
    params.width_         = hs_level_widths_[level];
    params.height_        = hs_level_heights_[level];
    params.coarse_width_  = top_level ? 1 : hs_level_widths_[level + 1];
    params.coarse_height_ = top_level ? 1 : hs_level_heights_[level + 1];
    params.sample_count_  = static_cast<int32_t>(samples.size());
    params.top_level_     = top_level ? 1 : 0;
    for (size_t i = 0; i < samples.size(); ++i) {
      params.gammas_[i] = samples[i].gamma_;
    }

    auto encoder = NS::RetainPtr(command_buffer->computeCommandEncoder());
    encoder->setComputePipelineState(hs_select_level_packed_pipeline_.get());
    encoder->setBuffer(hs_source_levels_[level].get(), 0, 0);
    encoder->setBuffer(hs_sample_levels_[level].get(), 0, 1);
    encoder->setBuffer(
        top_level ? hs_sample_levels_[level].get() : hs_sample_levels_[level + 1].get(), 0, 2);
    encoder->setBuffer(hs_output_levels_[level].get(), 0, 3);
    encoder->setBytes(&params, sizeof(params), 4);
    DispatchThreads(encoder.get(), hs_select_level_packed_pipeline_.get(),
                    static_cast<uint32_t>(hs_level_widths_[level]),
                    static_cast<uint32_t>(hs_level_heights_[level]));
    encoder->endEncoding();
  }

  void EncodeHsCollapseLevel(MTL::CommandBuffer* command_buffer, int level) {
    if (!hs_collapse_level_pipeline_) {
      hs_collapse_level_pipeline_ =
          GetPipelineState(kHsCollapseLevelKernelName, kHsCollapseLevelDebugLabel);
    }

    const MetalHsPyrDownParams params{hs_level_widths_[level],
                                      hs_level_heights_[level],
                                      hs_level_widths_[level + 1],
                                      hs_level_heights_[level + 1],
                                      0,
                                      0,
                                      {0, 0}};
    auto                       encoder = NS::RetainPtr(command_buffer->computeCommandEncoder());
    encoder->setComputePipelineState(hs_collapse_level_pipeline_.get());
    encoder->setBuffer(hs_output_levels_[level].get(), 0, 0);
    encoder->setBuffer(hs_output_levels_[level + 1].get(), 0, 1);
    encoder->setBuffer(hs_remap_a_levels_[level].get(), 0, 2);
    encoder->setBytes(&params, sizeof(params), 3);
    DispatchThreads(encoder.get(), hs_collapse_level_pipeline_.get(),
                    static_cast<uint32_t>(hs_level_widths_[level]),
                    static_cast<uint32_t>(hs_level_heights_[level]));
    encoder->endEncoding();
    std::swap(hs_output_levels_[level], hs_remap_a_levels_[level]);
  }

  void BuildHsOutputPyramid(MTL::CommandBuffer*                  command_buffer,
                            const std::vector<MetalHsLlfSample>& samples,
                            MetalExecutionStats*                 stats) {
    EnsureHsSamplePyramidBuffers(static_cast<int32_t>(samples.size()));

    const auto remap_start = std::chrono::steady_clock::now();
    BuildHsPackedSamplePyramids(command_buffer, samples);
    const auto remap_end = std::chrono::steady_clock::now();
    if (stats != nullptr) {
      stats->hs_remap_encode_ms +=
          std::chrono::duration<double, std::milli>(remap_end - remap_start).count();
    }

    ClearHsOutputPyramid(command_buffer);

    const auto select_start = std::chrono::steady_clock::now();
    for (int level = 0; level < hs_level_count_; ++level) {
      EncodeHsSelectPackedLevel(command_buffer, level, samples);
    }
    const auto select_end = std::chrono::steady_clock::now();
    if (stats != nullptr) {
      stats->hs_select_encode_ms +=
          std::chrono::duration<double, std::milli>(select_end - select_start).count();
    }

    const auto collapse_start = std::chrono::steady_clock::now();
    for (int level = hs_level_count_ - 2; level >= 0; --level) {
      EncodeHsCollapseLevel(command_buffer, level);
    }
    const auto collapse_end = std::chrono::steady_clock::now();
    if (stats != nullptr) {
      stats->hs_collapse_encode_ms +=
          std::chrono::duration<double, std::milli>(collapse_end - collapse_start).count();
    }
  }

  void EncodeHsApplyAdjustedL(MTL::CommandBuffer* command_buffer, const metal::MetalImage& src,
                              metal::MetalImage& dst) {
    if (!hs_apply_adjusted_l_pipeline_) {
      hs_apply_adjusted_l_pipeline_ =
          GetPipelineState(kHsApplyAdjustedLKernelName, kHsApplyAdjustedLDebugLabel);
    }

    dst.Create(src.Width(), src.Height(), src.Format(), true, true, false);
    const MetalHsPlaneApplyParams params{static_cast<int32_t>(src.Width()),
                                         static_cast<int32_t>(src.Height()), hs_cached_width_,
                                         hs_cached_height_};
    auto                          encoder = NS::RetainPtr(command_buffer->computeCommandEncoder());
    encoder->setComputePipelineState(hs_apply_adjusted_l_pipeline_.get());
    encoder->setTexture(src.Texture(), 0);
    encoder->setBuffer(hs_output_levels_[0].get(), 0, 0);
    encoder->setTexture(dst.Texture(), 1);
    encoder->setBytes(&params, sizeof(params), 1);
    DispatchThreads(encoder.get(), hs_apply_adjusted_l_pipeline_.get(), src.Width(), src.Height());
    encoder->endEncoding();
  }

  void EncodeHsApplyAdjustedLFromFrame(MTL::CommandBuffer*      command_buffer,
                                       const metal::MetalImage& src, metal::MetalImage& dst) {
    if (!hs_apply_adjusted_l_from_frame_pipeline_) {
      hs_apply_adjusted_l_from_frame_pipeline_ = GetPipelineState(
          kHsApplyAdjustedLFromFrameKernelName, kHsApplyAdjustedLFromFrameDebugLabel);
    }

    dst.Create(src.Width(), src.Height(), src.Format(), true, true, false);
    const MetalHsPlaneApplyParams params{static_cast<int32_t>(src.Width()),
                                         static_cast<int32_t>(src.Height()), hs_cached_width_,
                                         hs_cached_height_};
    auto                          encoder = NS::RetainPtr(command_buffer->computeCommandEncoder());
    encoder->setComputePipelineState(hs_apply_adjusted_l_from_frame_pipeline_.get());
    encoder->setTexture(src.Texture(), 0);
    encoder->setBuffer(hs_output_levels_[0].get(), 0, 0);
    encoder->setTexture(dst.Texture(), 1);
    encoder->setBytes(&params, sizeof(params), 1);
    DispatchThreads(encoder.get(), hs_apply_adjusted_l_from_frame_pipeline_.get(), src.Width(),
                    src.Height());
    encoder->endEncoding();
  }

  void EncodeNeighborBlurHorizontal(MTL::CommandBuffer* command_buffer, MTL::Buffer* stage_buffer,
                                    const metal::MetalImage& src, metal::MetalImage& dst) {
    if (!neighbor_blur_horizontal_pipeline_) {
      neighbor_blur_horizontal_pipeline_ =
          GetPipelineState(kNeighborBlurHorizontalKernelName, kNeighborBlurDebugLabel);
    }

    auto encoder = NS::RetainPtr(command_buffer->computeCommandEncoder());
    encoder->setComputePipelineState(neighbor_blur_horizontal_pipeline_.get());
    encoder->setTexture(src.Texture(), 0);
    encoder->setTexture(dst.Texture(), 1);
    encoder->setBuffer(stage_buffer, 0, 0);
    DispatchThreads(encoder.get(), neighbor_blur_horizontal_pipeline_.get(), src.Width(),
                    src.Height());
    encoder->endEncoding();
  }

  void EncodeNeighborApplyVertical(MTL::CommandBuffer* command_buffer, MTL::Buffer* stage_buffer,
                                   const metal::MetalImage& src,
                                   const metal::MetalImage& blur_horizontal,
                                   metal::MetalImage&       dst) {
    if (!neighbor_apply_vertical_pipeline_) {
      neighbor_apply_vertical_pipeline_ =
          GetPipelineState(kNeighborApplyVerticalKernelName, kNeighborApplyDebugLabel);
    }

    auto encoder = NS::RetainPtr(command_buffer->computeCommandEncoder());
    encoder->setComputePipelineState(neighbor_apply_vertical_pipeline_.get());
    encoder->setTexture(src.Texture(), 0);
    encoder->setTexture(blur_horizontal.Texture(), 1);
    encoder->setTexture(dst.Texture(), 2);
    encoder->setBuffer(stage_buffer, 0, 0);
    DispatchThreads(encoder.get(), neighbor_apply_vertical_pipeline_.get(), src.Width(),
                    src.Height());
    encoder->endEncoding();
  }

  auto ShouldRunHighlightShadowLocalTone() const -> bool {
    if (!fused_params_.hs_local_tone_enabled_) {
      return false;
    }
    const float shadow_amount    = fused_params_.shadows_enabled_
                                       ? std::clamp(fused_params_.shadows_offset_,
                                                    -kHsBackendAmountLimit,
                                                    kHsBackendAmountLimit)
                                       : 0.0f;
    const float highlight_amount = fused_params_.highlights_enabled_
                                       ? std::clamp(-fused_params_.highlights_offset_,
                                                    -kHsBackendAmountLimit,
                                                    kHsBackendAmountLimit)
                                       : 0.0f;
    return std::abs(shadow_amount) > 1.0e-6f || std::abs(highlight_amount) > 1.0e-6f;
  }

  void EncodeHighlightShadowLocalTone(MTL::CommandBuffer*      command_buffer,
                                      const metal::MetalImage& src, metal::MetalImage& dst,
                                      MetalExecutionStats* stats) {
    const float         shadow_amount    = fused_params_.shadows_enabled_
                                               ? std::clamp(fused_params_.shadows_offset_,
                                                            -kHsBackendAmountLimit,
                                                            kHsBackendAmountLimit)
                                               : 0.0f;
    const float         highlight_amount = fused_params_.highlights_enabled_
                                               ? std::clamp(-fused_params_.highlights_offset_,
                                                            -kHsBackendAmountLimit,
                                                            kHsBackendAmountLimit)
                                               : 0.0f;
    const std::uint64_t adjusted_cache_key =
        BuildAdjustedResultCacheKey(fused_params_, shadow_amount, highlight_amount);
    const bool roi_frame_with_source_reference = fused_params_.render_roi_enabled_ &&
                                                 fused_params_.render_roi_reference_width_ > 0 &&
                                                 fused_params_.render_roi_reference_height_ > 0;
    const bool reference_result_cache_valid =
        hs_cached_reference_base_ && hs_output_levels_[0].get() != nullptr &&
        hs_cached_key_ == adjusted_cache_key && hs_cached_width_ > 0 && hs_cached_height_ > 0 &&
        hs_cached_frame_width_ > 0 && hs_cached_frame_height_ > 0 && hs_cached_pitch_ > 0;
    if (!roi_frame_with_source_reference && reference_result_cache_valid &&
        (hs_cached_frame_width_ > static_cast<int32_t>(src.Width()) ||
         hs_cached_frame_height_ > static_cast<int32_t>(src.Height()))) {
      const auto apply_start = std::chrono::steady_clock::now();
      EncodeHsApplyAdjustedLFromFrame(command_buffer, src, dst);
      const auto apply_end = std::chrono::steady_clock::now();
      if (stats != nullptr) {
        stats->hs_apply_encode_ms +=
            std::chrono::duration<double, std::milli>(apply_end - apply_start).count();
      }
      return;
    }

    const MetalHsMaskDimensions mask_dims = ComputeHsMaskDimensions(
        static_cast<int32_t>(src.Width()), static_cast<int32_t>(src.Height()),
        roi_frame_with_source_reference);
    EnsureHsPyramidBuffers(mask_dims.width_, mask_dims.height_, fused_params_.hs_base_radius_);
    const bool cache_valid =
        hs_output_levels_[0].get() != nullptr && hs_cached_key_ == adjusted_cache_key &&
        hs_cached_frame_width_ == static_cast<int32_t>(src.Width()) &&
        hs_cached_frame_height_ == static_cast<int32_t>(src.Height()) &&
        hs_cached_width_ == mask_dims.width_ && hs_cached_height_ == mask_dims.height_ &&
        hs_cached_pitch_ == hs_level_widths_[0];
    if (!cache_valid) {
      const auto samples      = BuildHsSamples(shadow_amount, highlight_amount);
      const auto source_start = std::chrono::steady_clock::now();
      BuildHsSourcePyramid(command_buffer, src);
      const auto source_end = std::chrono::steady_clock::now();
      if (stats != nullptr) {
        stats->hs_source_encode_ms +=
            std::chrono::duration<double, std::milli>(source_end - source_start).count();
      }
      BuildHsOutputPyramid(command_buffer, samples, stats);
      hs_cached_key_            = adjusted_cache_key;
      hs_cached_width_          = mask_dims.width_;
      hs_cached_height_         = mask_dims.height_;
      hs_cached_frame_width_    = static_cast<int32_t>(src.Width());
      hs_cached_frame_height_   = static_cast<int32_t>(src.Height());
      hs_cached_pitch_          = hs_level_widths_[0];
      hs_cached_reference_base_ = !roi_frame_with_source_reference;
    }

    const auto apply_start = std::chrono::steady_clock::now();
    if (hs_cached_width_ == static_cast<int32_t>(src.Width()) &&
        hs_cached_height_ == static_cast<int32_t>(src.Height())) {
      EncodeHsApplyAdjustedL(command_buffer, src, dst);
    } else {
      EncodeHsApplyAdjustedLFromFrame(command_buffer, src, dst);
    }
    const auto apply_end = std::chrono::steady_clock::now();
    if (stats != nullptr) {
      stats->hs_apply_encode_ms +=
          std::chrono::duration<double, std::milli>(apply_end - apply_start).count();
    }
  }

  auto ShouldRunSharpen() const -> bool {
    return fused_params_.sharpen_enabled_ && fused_params_.sharpen_offset_ != 0.0f &&
           fused_params_.sharpen_radius_ > 0.0f;
  }

  auto ShouldRunClarity() const -> bool {
    return fused_params_.clarity_enabled_ && fused_params_.clarity_offset_ != 0.0f &&
           fused_params_.clarity_radius_ > 0.0f;
  }

  auto BuildNeighborStages() const -> std::vector<MetalNeighborStage> {
    std::vector<MetalNeighborStage> stages;
    stages.reserve(2);

    if (ShouldRunSharpen()) {
      stages.push_back(MetalNeighborStage{BuildNeighborStageParams(
          MetalNeighborOpKind::Sharpen, fused_params_.sharpen_radius_,
          fused_params_.sharpen_offset_, fused_params_.sharpen_threshold_,
          fused_params_.sharpen_gaussian_tap_count_, fused_params_.sharpen_gaussian_weights_)});
    }
    if (ShouldRunClarity()) {
      stages.push_back(MetalNeighborStage{BuildNeighborStageParams(
          MetalNeighborOpKind::Clarity, fused_params_.clarity_radius_,
          fused_params_.clarity_offset_, 0.0f, fused_params_.clarity_gaussian_tap_count_,
          fused_params_.clarity_gaussian_weights_)});
    }

    return stages;
  }

  auto RunMetalPipeline(MetalExecutionStats& stats) -> metal::MetalImage {
    const auto input_prepare_start = std::chrono::steady_clock::now();
    EnsureMetalInput();
    const auto input_prepare_end = std::chrono::steady_clock::now();
    stats.input_prepare_ms =
        std::chrono::duration<double, std::milli>(input_prepare_end - input_prepare_start).count();

    const auto& input             = input_img_->GetMetalImage();
    const auto  neighbor_stages   = BuildNeighborStages();
    const bool  run_hs_local_tone = ShouldRunHighlightShadowLocalTone();
    stats.detail_stage_count      = neighbor_stages.size();

    metal::MetalImage working     = metal::MetalImage::Create2D(input.Width(), input.Height(),
                                                                input.Format(), true, true, false);
    metal::MetalImage blur_horizontal;
    metal::MetalImage scratch;
    if (!neighbor_stages.empty()) {
      blur_horizontal = metal::MetalImage::Create2D(input.Width(), input.Height(), input.Format(),
                                                    true, true, false);
      scratch = metal::MetalImage::Create2D(input.Width(), input.Height(), input.Format(), true,
                                            true, false);
    }

    auto       command_buffer     = MakeCommandBuffer();

    const auto fused_encode_start = std::chrono::steady_clock::now();
    if (run_hs_local_tone) {
      EncodeFusedStageKernel(command_buffer.get(), input, pre_hs_working_, 1);
    } else {
      EncodeFusedKernel(command_buffer.get(), input, working);
    }
    const auto fused_encode_end = std::chrono::steady_clock::now();
    stats.fused_encode_ms =
        std::chrono::duration<double, std::milli>(fused_encode_end - fused_encode_start).count();

    if (run_hs_local_tone) {
      const auto hs_encode_start = std::chrono::steady_clock::now();
      EncodeHighlightShadowLocalTone(command_buffer.get(), pre_hs_working_, hs_working_, &stats);
      const auto hs_encode_end = std::chrono::steady_clock::now();
      stats.hs_encode_ms =
          std::chrono::duration<double, std::milli>(hs_encode_end - hs_encode_start).count();

      const auto post_hs_encode_start = std::chrono::steady_clock::now();
      EncodeFusedStageKernel(command_buffer.get(), hs_working_, working, 2);
      const auto post_hs_encode_end = std::chrono::steady_clock::now();
      stats.fused_encode_ms +=
          std::chrono::duration<double, std::milli>(post_hs_encode_end - post_hs_encode_start)
              .count();
    }

    metal::MetalImage*                      detail_src = &working;
    metal::MetalImage*                      detail_dst = &scratch;
    std::vector<NS::SharedPtr<MTL::Buffer>> stage_buffers;
    stage_buffers.reserve(neighbor_stages.size());

    for (const auto& stage : neighbor_stages) {
      stage_buffers.push_back(UploadStageParams(stage.params_));
      auto*      stage_buffer          = stage_buffers.back().get();

      const auto neighbor_encode_start = std::chrono::steady_clock::now();
      EncodeNeighborBlurHorizontal(command_buffer.get(), stage_buffer, *detail_src,
                                   blur_horizontal);
      EncodeNeighborApplyVertical(command_buffer.get(), stage_buffer, *detail_src, blur_horizontal,
                                  *detail_dst);
      const auto neighbor_encode_end = std::chrono::steady_clock::now();
      stats.neighbor_encode_ms +=
          std::chrono::duration<double, std::milli>(neighbor_encode_end - neighbor_encode_start)
              .count();

      std::swap(detail_src, detail_dst);
    }

    const auto gpu_wait_start = std::chrono::steady_clock::now();
    command_buffer->commit();
    command_buffer->waitUntilCompleted();
    const auto gpu_wait_end = std::chrono::steady_clock::now();
    stats.gpu_wait_ms =
        std::chrono::duration<double, std::milli>(gpu_wait_end - gpu_wait_start).count();

    return *detail_src;
  }

 public:
  void SetInputImage(std::shared_ptr<ImageBuffer> input_image) override {
    input_img_ = std::move(input_image);
  }

  void SetParams(OperatorParams& params) override {
    cpu_params_   = &params;
    fused_params_ = FusedParamsConverter::ConvertFromCPU(params, fused_params_);
    resources_    = metal::MetalFusedParamUploader::Upload(fused_params_, params, resources_);
  }

  void SetFrameSink(IFrameSink* frame_sink) override { frame_sink_ = frame_sink; }

  void Execute(std::shared_ptr<ImageBuffer> output_img) override {
    if (!cpu_params_) {
      throw std::runtime_error("Metal fused pipeline: parameters were not set.");
    }

    const auto                exec_start = std::chrono::steady_clock::now();
    MetalExecutionStats       stats;
    metal::MetalImage         result         = RunMetalPipeline(stats);
    const ViewerDisplayConfig display_config = ResolveViewerDisplayConfig(*cpu_params_);

    if (frame_sink_) {
      const auto submit_start = std::chrono::steady_clock::now();
#ifdef HAVE_METAL
      auto final_image_resource =
          std::make_shared<scope::metal_detail::MetalTextureImageResource>();
      final_image_resource->texture = NS::RetainPtr(result.Texture());
      final_image_resource->width   = static_cast<int>(result.Width());
      final_image_resource->height  = static_cast<int>(result.Height());
      final_image_resource->format  = FramePixelFormat::RGBA32F;
      final_image_resource->native_object =
          reinterpret_cast<std::uintptr_t>(final_image_resource->texture.get());
      frame_sink_->SubmitFinalDisplayFrame(FinalDisplayFrameView{
          SharedGpuImageHandle{
              GpuBackend::Metal,
              std::shared_ptr<void>(final_image_resource, final_image_resource.get()),
              static_cast<int>(result.Width()), static_cast<int>(result.Height()), 0,
              FramePixelFormat::RGBA32F},
          static_cast<int>(result.Width()),
          static_cast<int>(result.Height()),
          FramePixelFormat::RGBA32F,
          display_config,
          AnalysisDomain::DisplayEncoded,
          {},
          0});
      frame_sink_->SubmitMetalFrame(ViewerMetalFrame{
          static_cast<int>(result.Width()), static_cast<int>(result.Height()),
          reinterpret_cast<std::uintptr_t>(final_image_resource->texture.get()),
          std::shared_ptr<const void>(final_image_resource, final_image_resource->texture.get()),
          display_config, FramePresentationMode::FullFrame});
#else
      cv::Mat host_image;
      result.Download(host_image);
      stats.host_download_ms =
          std::chrono::duration<double, std::milli>(std::chrono::steady_clock::now() - submit_start)
              .count();
      if (host_image.type() != CV_32FC4) {
        throw std::runtime_error("Metal fused pipeline: expected RGBA32F host frame for viewer.");
      }

      const size_t row_bytes =
          static_cast<size_t>(host_image.cols) * static_cast<size_t>(sizeof(cv::Vec4f));
      auto host_pixels = std::make_shared<std::vector<float>>(
          static_cast<size_t>(host_image.cols) * static_cast<size_t>(host_image.rows) * 4U);
      cv::Mat contiguous_host(host_image.rows, host_image.cols, CV_32FC4, host_pixels->data(),
                              row_bytes);
      host_image.copyTo(contiguous_host);
      frame_sink_->SubmitHostFrame(
          ViewerFrame{host_image.cols, host_image.rows, row_bytes,
                      std::shared_ptr<const void>(host_pixels, host_pixels->data()), display_config,
                      FramePresentationMode::FullFrame});
#endif
      const auto submit_end = std::chrono::steady_clock::now();
      stats.host_copy_submit_ms =
          std::chrono::duration<double, std::milli>(submit_end - submit_start).count();
    }

    if (output_img) {
      const auto output_wrap_start = std::chrono::steady_clock::now();
      *output_img                  = ImageBuffer(std::move(result));
      const auto output_wrap_end   = std::chrono::steady_clock::now();
      stats.output_wrap_ms =
          std::chrono::duration<double, std::milli>(output_wrap_end - output_wrap_start).count();
    }

    const auto exec_end = std::chrono::steady_clock::now();
    stats.total_ms      = std::chrono::duration<double, std::milli>(exec_end - exec_start).count();
    preview_reporter_.Report(stats);
  }

  void ReleaseResources() override {
    resources_.Reset();
    fused_pipeline_                            = nullptr;
    fused_stage_pipeline_                      = nullptr;
    hs_extract_pipeline_                       = nullptr;
    hs_extract_resampled_pipeline_             = nullptr;
    hs_build_remapped_samples_packed_pipeline_ = nullptr;
    hs_pyr_down_pipeline_                      = nullptr;
    hs_pyr_down_packed_pipeline_               = nullptr;
    hs_select_level_packed_pipeline_           = nullptr;
    hs_collapse_level_pipeline_                = nullptr;
    hs_apply_adjusted_l_pipeline_              = nullptr;
    hs_apply_adjusted_l_from_frame_pipeline_   = nullptr;
    neighbor_blur_horizontal_pipeline_         = nullptr;
    neighbor_apply_vertical_pipeline_          = nullptr;
    pre_hs_working_.Release();
    hs_working_.Release();
    ReleaseHsPyramidBuffers();
  }
};

}  // namespace

auto CreateMetalGPUPipeline() -> std::unique_ptr<GPUPipelineImpl> {
  return std::make_unique<MetalGPUPipeline>();
}

}  // namespace alcedo

#endif
