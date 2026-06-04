//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifdef HAVE_OPENCL

#include <algorithm>
#include <array>
#include <chrono>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <iomanip>
#include <iostream>
#include <memory>
#include <stdexcept>
#include <utility>
#include <vector>

#include "edit/operators/GPU_kernels/fused_param.hpp"
#include "edit/operators/GPU_kernels/opencl_param.hpp"
#include "edit/pipeline/opencl_pipeline_programs.hpp"
#include "edit/pipeline/pipeline_gpu_wrapper.hpp"
#include "edit/scope/detail/scope_opencl_shared.hpp"
#include "edit/scope/scope_analyzer.hpp"
#include "image/image_buffer.hpp"
#include "image/opencl_image.hpp"
#include "opencl/opencl_context.hpp"
#include "opencl/opencl_program_library.hpp"
#include "ui/edit_viewer/frame_sink.hpp"

namespace alcedo {
namespace {

constexpr uint32_t kOpenClNeighborMaxTapCount = 64;
constexpr float    kHsHighlightStrengthScale  = 1.5f;
constexpr float    kHsBackendAmountLimit      = 1.5f;

enum class OpenClNeighborOpKind : uint32_t {
  Sharpen = 1,
  Clarity = 2,
};

struct OpenClNeighborStageParams {
  uint32_t                                      kind_      = 0;
  uint32_t                                      radius_    = 0;
  uint32_t                                      tap_count_ = 0;
  float                                         amount_    = 0.0f;
  float                                         threshold_ = 0.0f;
  std::array<float, kOpenClNeighborMaxTapCount> weights_   = {};
};

struct OpenClNeighborStage {
  OpenClNeighborStageParams params_ = {};
};

auto ResolveViewerDisplayConfig(const OperatorParams& params) -> ViewerDisplayConfig {
  return ViewerDisplayConfig{params.to_output_params_.encoding_space_,
                             params.to_output_params_.eotf_};
}

auto BuildGaussianWeights(float sigma, uint32_t radius)
    -> std::array<float, kOpenClNeighborMaxTapCount> {
  std::array<float, kOpenClNeighborMaxTapCount> weights{};
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

auto BuildNeighborStageParams(OpenClNeighborOpKind kind, float sigma, float amount, float threshold,
                              int gaussian_tap_count, const float* gaussian_weights)
    -> OpenClNeighborStageParams {
  OpenClNeighborStageParams params;

  params.kind_      = static_cast<uint32_t>(kind);
  params.amount_    = amount;
  params.threshold_ = threshold;

  const int clamped_tap_count =
      std::clamp(gaussian_tap_count, 0, static_cast<int>(kOpenClNeighborMaxTapCount));
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
      (kOpenClNeighborMaxTapCount > 0U) ? (kOpenClNeighborMaxTapCount - 1U) : 0U;
  params.radius_ =
      std::clamp<uint32_t>(static_cast<uint32_t>(std::ceil(3.0f * safe_sigma)), 1U, max_radius);
  params.tap_count_ = params.radius_ + 1U;
  params.weights_   = BuildGaussianWeights(safe_sigma, params.radius_);
  return params;
}

auto UploadStageParams(const OpenClNeighborStageParams& params) -> OpenCL::Pipeline::OpenClBuffer {
  return OpenCL::Pipeline::OpenClBuffer::CreateReadOnlyCopy(&params,
                                                            sizeof(OpenClNeighborStageParams));
}

void CheckOpenClFrameCopy(cl_int error, const char* operation) {
  if (error != CL_SUCCESS) {
    throw std::runtime_error(std::string("OpenCL fused pipeline: ") + operation +
                             " failed with error " + std::to_string(error) + ".");
  }
}

auto TrySubmitOpenClFrameToSink(opencl::OpenClImage& image, IFrameSink& frame_sink) -> bool {
  frame_sink.EnsureSize(image.Width(), image.Height());
  const FrameWriteMapping mapping = frame_sink.MapResourceForWrite(FrameMemoryDomain::OpenClDevice);
  if (!mapping) {
    return false;
  }

  const auto unmap = [&frame_sink]() { frame_sink.UnmapResource(); };
  if (mapping.pixel_format != FramePixelFormat::RGBA32F ||
      mapping.memory_domain != FrameMemoryDomain::OpenClDevice ||
      mapping.target_type != FrameWriteTargetType::OpenClImage || mapping.data == nullptr) {
    unmap();
    return false;
  }

  auto&        context   = OpenClContext::Instance();
  const size_t origin[3] = {0, 0, 0};
  const size_t region[3] = {static_cast<size_t>(image.Width()), static_cast<size_t>(image.Height()),
                            1};
  cl_mem       target_image = static_cast<cl_mem>(mapping.data);
  const cl_int copy_error   = clEnqueueCopyBufferToImage(
      context.Queue(), image.Buffer(), target_image, 0, origin, region, 0, nullptr, nullptr);
  if (copy_error != CL_SUCCESS) {
    unmap();
    CheckOpenClFrameCopy(copy_error, "clEnqueueCopyBufferToImage");
  }

  frame_sink.UnmapResource();
  CheckOpenClFrameCopy(clFinish(context.Queue()), "clFinish after OpenGL frame copy");
  frame_sink.NotifyFrameReady();
  return true;
}

auto MakeOpenClScopeImageResource(const opencl::OpenClImage& image)
    -> std::shared_ptr<scope::opencl_detail::OpenClLinearImageResource> {
  if (image.Empty() || image.Type() != CV_32FC4 || image.Buffer() == nullptr) {
    return {};
  }

  CheckOpenClFrameCopy(clRetainMemObject(image.Buffer()), "clRetainMemObject(scope frame)");
  auto resource           = std::make_shared<scope::opencl_detail::OpenClLinearImageResource>();
  resource->buffer        = image.Buffer();
  resource->row_bytes     = image.RowBytes();
  resource->width         = image.Width();
  resource->height        = image.Height();
  resource->format        = FramePixelFormat::RGBA32F;
  resource->owns_memory   = true;
  resource->native_object = reinterpret_cast<std::uintptr_t>(image.Buffer());
  return resource;
}

void SubmitOpenClFrameForScope(const opencl::OpenClImage& image, IFrameSink& frame_sink,
                               const ViewerDisplayConfig& display_config) {
  auto final_image = MakeOpenClScopeImageResource(image);
  if (!final_image) {
    return;
  }

  frame_sink.SubmitFinalDisplayFrame(FinalDisplayFrameView{
      SharedGpuImageHandle{GpuBackend::OpenCL,
                           std::shared_ptr<void>(final_image, final_image.get()), image.Width(),
                           image.Height(), image.RowBytes(), FramePixelFormat::RGBA32F},
      image.Width(),
      image.Height(),
      FramePixelFormat::RGBA32F,
      display_config,
      AnalysisDomain::DisplayEncoded,
      {},
      0});
}

class OpenCLGPUPipeline final : public GPUPipelineImpl {
 private:
  std::shared_ptr<ImageBuffer>           input_img_;
  OperatorParams*                        cpu_params_      = nullptr;
  IFrameSink*                            frame_sink_      = nullptr;
  FusedOperatorParams                    fused_params_    = {};
  OpenCL::Pipeline::OpenClFusedResources resources_       = {};

  cl_kernel                              fused_kernel_    = nullptr;
  cl_kernel                              fused_stage_kernel_ = nullptr;
  cl_kernel                              validate_kernel_ = nullptr;
  cl_kernel                              blur_h_kernel_   = nullptr;
  cl_kernel                              apply_v_kernel_  = nullptr;
  cl_kernel                              hs_extract_kernel_ = nullptr;
  cl_kernel                              hs_extract_resampled_kernel_ = nullptr;
  cl_kernel                              hs_build_remapped_sample_kernel_ = nullptr;
  cl_kernel                              hs_pyr_down_kernel_ = nullptr;
  cl_kernel                              hs_select_interpolated_level_kernel_ = nullptr;
  cl_kernel                              hs_collapse_level_kernel_ = nullptr;
  cl_kernel                              hs_apply_adjusted_l_kernel_ = nullptr;
  cl_kernel                              hs_apply_adjusted_l_from_frame_kernel_ = nullptr;
  cl_kernel                              hs_apply_adjusted_l_from_reference_kernel_ = nullptr;

  opencl::OpenClImage                    working_;
  opencl::OpenClImage                    pre_hs_working_;
  opencl::OpenClImage                    hs_working_;
  opencl::OpenClImage                    blur_horizontal_;
  opencl::OpenClImage                    detail_scratch_;

  static constexpr int                   kHsMaxLevels = 12;
  static constexpr float                 kHsGammaMinL = -0.15f;
  static constexpr float                 kHsGammaMaxL = 1.18f;
  static constexpr float                 kHsBaseSigmaR = 0.07545252f;
  static constexpr float                 kHsGammaStepScale = 1.35f;
  static constexpr size_t                kHsMaxRetainedMaskBytes = 256ULL * 1024ULL * 1024ULL;

  struct HsLlfSample {
    float gamma = 0.0f;
    float target = 0.0f;
    float beta = 1.0f;
    float alpha = 1.0f;
  };

  std::array<cl_mem, kHsMaxLevels>       hs_source_levels_ = {};
  std::array<cl_mem, kHsMaxLevels>       hs_remap_a_levels_ = {};
  std::array<cl_mem, kHsMaxLevels>       hs_remap_b_levels_ = {};
  std::array<cl_mem, kHsMaxLevels>       hs_output_levels_ = {};
  std::array<int, kHsMaxLevels>          hs_level_widths_ = {};
  std::array<int, kHsMaxLevels>          hs_level_heights_ = {};
  int                                    hs_level_count_ = 0;
  int                                    hs_cached_width_ = 0;
  int                                    hs_cached_height_ = 0;
  int                                    hs_cached_frame_width_ = 0;
  int                                    hs_cached_frame_height_ = 0;
  int                                    hs_cached_pitch_ = 0;
  std::uint64_t                          hs_cached_source_key_ = 0;
  std::uint64_t                          hs_cached_key_ = 0;
  bool                                   hs_cached_reference_base_ = false;

  void                                   ReleaseHsBaseBuffers() {
    for (cl_mem& buffer : hs_source_levels_) {
      if (buffer != nullptr) {
        clReleaseMemObject(buffer);
        buffer = nullptr;
      }
    }
    for (cl_mem& buffer : hs_remap_a_levels_) {
      if (buffer != nullptr) {
        clReleaseMemObject(buffer);
        buffer = nullptr;
      }
    }
    for (cl_mem& buffer : hs_remap_b_levels_) {
      if (buffer != nullptr) {
        clReleaseMemObject(buffer);
        buffer = nullptr;
      }
    }
    for (cl_mem& buffer : hs_output_levels_) {
      if (buffer != nullptr) {
        clReleaseMemObject(buffer);
        buffer = nullptr;
      }
    }
    hs_level_widths_.fill(0);
    hs_level_heights_.fill(0);
    hs_level_count_ = 0;
    hs_cached_width_ = 0;
    hs_cached_height_ = 0;
    hs_cached_frame_width_ = 0;
    hs_cached_frame_height_ = 0;
    hs_cached_pitch_ = 0;
    hs_cached_source_key_ = 0;
    hs_cached_key_ = 0;
    hs_cached_reference_base_ = false;
  }

  void                                   EnsureHsPyramidBuffers(int width, int height,
                                                               float radius) {
    if (width <= 0 || height <= 0) {
      throw std::runtime_error("OpenCL fused pipeline: invalid H/S pyramid dimensions.");
    }

    const int new_level_count = ComputeHsLevelCount(width, height, radius);
    std::array<int, kHsMaxLevels> new_widths = {};
    std::array<int, kHsMaxLevels> new_heights = {};
    new_widths[0] = width;
    new_heights[0] = height;
    for (int level = 1; level < new_level_count; ++level) {
      new_widths[level] = std::max(1, (new_widths[level - 1] + 1) / 2);
      new_heights[level] = std::max(1, (new_heights[level - 1] + 1) / 2);
    }

    bool layout_matches = hs_level_count_ == new_level_count;
    for (int level = 0; layout_matches && level < new_level_count; ++level) {
      layout_matches = hs_level_widths_[level] == new_widths[level] &&
                       hs_level_heights_[level] == new_heights[level] &&
                       hs_source_levels_[level] != nullptr &&
                       hs_remap_a_levels_[level] != nullptr &&
                       hs_remap_b_levels_[level] != nullptr &&
                       hs_output_levels_[level] != nullptr;
    }
    if (layout_matches) {
      return;
    }

    ReleaseHsBaseBuffers();
    hs_level_count_ = new_level_count;
    hs_level_widths_ = new_widths;
    hs_level_heights_ = new_heights;

    auto&  context = OpenClContext::Instance();
    for (int level = 0; level < hs_level_count_; ++level) {
      const size_t elems =
          static_cast<size_t>(hs_level_widths_[level]) *
          static_cast<size_t>(hs_level_heights_[level]);
      cl_int err = CL_SUCCESS;
      hs_source_levels_[level] =
          clCreateBuffer(context.Context(), CL_MEM_READ_WRITE, elems * sizeof(float), nullptr,
                         &err);
      if (err != CL_SUCCESS || hs_source_levels_[level] == nullptr) {
        ReleaseHsBaseBuffers();
        throw std::runtime_error("OpenCL fused pipeline: failed to allocate H/S source level.");
      }
      hs_remap_a_levels_[level] =
          clCreateBuffer(context.Context(), CL_MEM_READ_WRITE, elems * sizeof(float), nullptr,
                         &err);
      if (err != CL_SUCCESS || hs_remap_a_levels_[level] == nullptr) {
        ReleaseHsBaseBuffers();
        throw std::runtime_error("OpenCL fused pipeline: failed to allocate H/S remap A level.");
      }
      hs_remap_b_levels_[level] =
          clCreateBuffer(context.Context(), CL_MEM_READ_WRITE, elems * sizeof(float), nullptr,
                         &err);
      if (err != CL_SUCCESS || hs_remap_b_levels_[level] == nullptr) {
        ReleaseHsBaseBuffers();
        throw std::runtime_error("OpenCL fused pipeline: failed to allocate H/S remap B level.");
      }
      hs_output_levels_[level] =
          clCreateBuffer(context.Context(), CL_MEM_READ_WRITE, elems * sizeof(float), nullptr,
                         &err);
      if (err != CL_SUCCESS || hs_output_levels_[level] == nullptr) {
        ReleaseHsBaseBuffers();
        throw std::runtime_error("OpenCL fused pipeline: failed to allocate H/S output level.");
      }
    }
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
    return key;
  }

  static auto BuildRoiAdjustedResultCacheKey(const FusedOperatorParams& params,
                                             std::uint64_t base_key) -> std::uint64_t {
    std::uint64_t key = base_key;
    HashCombine(key, static_cast<std::uint64_t>(params.render_roi_enabled_));
    HashCombine(key, static_cast<std::uint64_t>(params.render_roi_x_));
    HashCombine(key, static_cast<std::uint64_t>(params.render_roi_y_));
    HashCombine(key, static_cast<std::uint64_t>(FloatBits(params.render_roi_scale_x_)));
    HashCombine(key, static_cast<std::uint64_t>(FloatBits(params.render_roi_scale_y_)));
    HashCombine(key, static_cast<std::uint64_t>(params.render_roi_reference_width_));
    HashCombine(key, static_cast<std::uint64_t>(params.render_roi_reference_height_));
    return key;
  }

  struct HsMaskDimensions {
    int width = 1;
    int height = 1;
  };

  static auto ComputeHsMaskDimensions(int width, int height, int max_long_edge)
      -> HsMaskDimensions {
    const float scale = std::min(
        1.0f, static_cast<float>(std::max(1, max_long_edge)) /
                  static_cast<float>(std::max(width, height)));
    return {std::max(1, static_cast<int>(std::ceil(static_cast<float>(width) * scale))),
            std::max(1, static_cast<int>(std::ceil(static_cast<float>(height) * scale)))};
  }

  static auto ComputeHsLevelCount(int width, int height, float radius) -> int {
    const int radius_levels =
        std::max(3, std::min(kHsMaxLevels,
                             static_cast<int>(std::ceil(std::log2(std::max(radius, 1.0f)))) + 2));
    int count = 1;
    int w = width;
    int h = height;
    while (count < radius_levels && (w > 1 || h > 1)) {
      w = std::max(1, (w + 1) / 2);
      h = std::max(1, (h + 1) / 2);
      ++count;
    }
    return count;
  }

  [[nodiscard]] auto AllocatedHsPyramidBytes() const -> size_t {
    size_t bytes = 0;
    for (int level = 0; level < hs_level_count_; ++level) {
      bytes += static_cast<size_t>(hs_level_widths_[level]) *
               static_cast<size_t>(hs_level_heights_[level]) * sizeof(float) * 4ULL;
    }
    return bytes;
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

  static auto HsApplyReferenceCurve(float reference_l, float shadow_amount,
                                    float highlight_amount) -> float {
    const float relative_ev = HsRelativeEv(reference_l);
    const float shadow_lift = std::max(shadow_amount, 0.0f) * HsShadowProfileEv(relative_ev);
    const float shadow_darken =
        std::max(-shadow_amount, 0.0f) * 0.55f * HsShadowProfileEv(relative_ev);
    const float highlight_reduce =
        std::max(highlight_amount, 0.0f) * kHsHighlightStrengthScale *
        HsHighlightProfileEv(relative_ev);
    const float highlight_boost =
        std::max(-highlight_amount, 0.0f) * 0.65f * HsHighlightProfileEv(relative_ev);
    const float practical_dark = Smoothstep(-5.85f, -3.95f, relative_ev) *
                                 (1.0f - Smoothstep(-3.20f, -1.65f, relative_ev));
    const float fill_plateau = Smoothstep(-5.55f, -3.30f, relative_ev) *
                               (1.0f - 0.45f * Smoothstep(-2.65f, -0.20f, relative_ev));
    const float deep_toe_fill =
        shadow_lift * (1.0f - Smoothstep(-7.35f, -4.95f, relative_ev)) * 0.28f;
    const float shadow_fill_lift =
        shadow_lift * (0.62f * practical_dark + 0.14f * fill_plateau) + deep_toe_fill;
    const float lifted_relative_ev =
        relative_ev + 0.24f * (shadow_lift + 0.84f * shadow_fill_lift);
    const float combo_shadow_rollback =
        ((shadow_lift > 1.0e-6f && highlight_reduce > 1.0e-6f) ? 1.0f : 0.0f) *
        shadow_fill_lift * Smoothstep(-2.00f, -0.60f, lifted_relative_ev) *
        (1.0f - Smoothstep(0.10f, 1.30f, lifted_relative_ev)) * 1.08f;
    const float combo_low_mid_darken =
        std::min(shadow_lift + shadow_fill_lift, highlight_reduce) *
        Smoothstep(-2.45f, -0.90f, lifted_relative_ev) *
        (1.0f - Smoothstep(0.50f, 1.95f, lifted_relative_ev)) * 1.30f;
    const float delta_ev = shadow_lift + shadow_fill_lift - combo_shadow_rollback -
                           shadow_darken - highlight_reduce - combo_low_mid_darken +
                           highlight_boost;
    return reference_l + delta_ev * (1.0f / 17.52f);
  }

  static auto HsDetailAlpha(float reference_l, float shadow_amount,
                            float highlight_amount) -> float {
    (void)highlight_amount;
    const float relative_ev = HsRelativeEv(reference_l);
    const float deep_shadow = 1.0f - Smoothstep(-5.7f, -4.1f, relative_ev);
    const float mid_shadow = Smoothstep(-5.0f, -3.6f, relative_ev) *
                             (1.0f - Smoothstep(-2.4f, -1.0f, relative_ev));
    const float lift_amount = std::max(shadow_amount, 0.0f);
    return 1.0f + 0.40f * lift_amount * deep_shadow - 0.14f * lift_amount * mid_shadow;
  }

  static auto HsToneBeta(float reference_l, float shadow_amount, float highlight_amount)
      -> float {
    constexpr float kEps = 0.035f;
    const float lo = HsApplyReferenceCurve(reference_l - kEps, shadow_amount, highlight_amount);
    const float hi = HsApplyReferenceCurve(reference_l + kEps, shadow_amount, highlight_amount);
    return std::clamp((hi - lo) / (2.0f * kEps), 0.08f, 1.70f);
  }

  static auto BuildHsSamples(float shadow_amount, float highlight_amount)
      -> std::vector<HsLlfSample> {
    const float sample_step = std::max(kHsBaseSigmaR * kHsGammaStepScale, 0.045f);
    const int sample_count =
        std::max(2, static_cast<int>(std::ceil((kHsGammaMaxL - kHsGammaMinL) / sample_step)) + 1);
    std::vector<HsLlfSample> samples;
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

  void                                   EnsureOpenClInput() {
    if (!input_img_) {
      throw std::runtime_error("OpenCL fused pipeline: input image is null.");
    }

    const bool has_valid_gpu = input_img_->gpu_data_valid_;
    const bool has_valid_cpu = input_img_->cpu_data_valid_;

    if (!has_valid_gpu && !has_valid_cpu) {
      throw std::runtime_error("OpenCL fused pipeline: input image has no valid CPU or GPU data.");
    }

    const bool needs_sync = !has_valid_gpu || input_img_->GetGPUBackend() != GpuBackendKind::OpenCL;

    if (needs_sync) {
      if (has_valid_gpu && input_img_->GetGPUBackend() != GpuBackendKind::OpenCL) {
        input_img_->SyncToCPU();
      }
      input_img_->SyncToGPU(GpuBackendKind::OpenCL);
    }

    if (input_img_->GetGPUType() != CV_32FC4) {
      input_img_->ConvertGPUDataTo(CV_32FC4);
    }
  }

  void EnsureFusedKernels() {
    auto&      library = OpenClProgramLibrary::Instance();
    cl_program program = library.GetProgram(OpenCL::Pipeline::kFusedProgramName);
    if (program == nullptr) {
      throw std::runtime_error("OpenCL fused pipeline: failed to get fused program from library.");
    }

    if (fused_kernel_ == nullptr) {
      cl_int err    = CL_SUCCESS;
      fused_kernel_ = clCreateKernel(program, OpenCL::Pipeline::kFusedKernelName, &err);
      if (err != CL_SUCCESS || fused_kernel_ == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create kernel '" +
                                 std::string(OpenCL::Pipeline::kFusedKernelName) + "' with error " +
                                 std::to_string(err) + ".");
      }
    }

    if (fused_stage_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      fused_stage_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kFusedStageKernelName, &err);
      if (err != CL_SUCCESS || fused_stage_kernel_ == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create kernel '" +
                                 std::string(OpenCL::Pipeline::kFusedStageKernelName) +
                                 "' with error " + std::to_string(err) + ".");
      }
    }

    if (validate_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      validate_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kValidateFusedParamsKernelName, &err);
      if (err != CL_SUCCESS || validate_kernel_ == nullptr) {
        throw std::runtime_error(
            "OpenCL fused pipeline: failed to create validation kernel with error " +
            std::to_string(err) + ".");
      }
    }
  }

  void EnsureDetailKernels() {
    auto&      library = OpenClProgramLibrary::Instance();
    cl_program program = library.GetProgram(OpenCL::Pipeline::kDetailProgramName);
    if (program == nullptr) {
      throw std::runtime_error("OpenCL fused pipeline: failed to get detail program from library.");
    }

    if (blur_h_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      blur_h_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kNeighborBlurHorizontalKernelName, &err);
      if (err != CL_SUCCESS || blur_h_kernel_ == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create kernel '" +
                                 std::string(OpenCL::Pipeline::kNeighborBlurHorizontalKernelName) +
                                 "' with error " + std::to_string(err) + ".");
      }
    }

    if (apply_v_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      apply_v_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kNeighborApplyVerticalKernelName, &err);
      if (err != CL_SUCCESS || apply_v_kernel_ == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create kernel '" +
                                 std::string(OpenCL::Pipeline::kNeighborApplyVerticalKernelName) +
                                 "' with error " + std::to_string(err) + ".");
      }
    }

    if (hs_extract_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      hs_extract_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kHsExtractLogIntensityKernelName, &err);
      if (err != CL_SUCCESS || hs_extract_kernel_ == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create kernel '" +
                                 std::string(OpenCL::Pipeline::kHsExtractLogIntensityKernelName) +
                                 "' with error " + std::to_string(err) + ".");
      }
    }

    if (hs_extract_resampled_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      hs_extract_resampled_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kHsExtractLogIntensityResampledKernelName, &err);
      if (err != CL_SUCCESS || hs_extract_resampled_kernel_ == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create kernel '" +
                                 std::string(OpenCL::Pipeline::kHsExtractLogIntensityResampledKernelName) +
                                 "' with error " + std::to_string(err) + ".");
      }
    }

    if (hs_build_remapped_sample_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      hs_build_remapped_sample_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kHsBuildRemappedSampleKernelName, &err);
      if (err != CL_SUCCESS || hs_build_remapped_sample_kernel_ == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create kernel '" +
                                 std::string(OpenCL::Pipeline::kHsBuildRemappedSampleKernelName) +
                                 "' with error " + std::to_string(err) + ".");
      }
    }

    if (hs_pyr_down_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      hs_pyr_down_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kHsPyrDownKernelName, &err);
      if (err != CL_SUCCESS || hs_pyr_down_kernel_ == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create kernel '" +
                                 std::string(OpenCL::Pipeline::kHsPyrDownKernelName) +
                                 "' with error " + std::to_string(err) + ".");
      }
    }

    if (hs_select_interpolated_level_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      hs_select_interpolated_level_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kHsSelectInterpolatedLevelKernelName, &err);
      if (err != CL_SUCCESS || hs_select_interpolated_level_kernel_ == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create kernel '" +
                                 std::string(OpenCL::Pipeline::kHsSelectInterpolatedLevelKernelName) +
                                 "' with error " + std::to_string(err) + ".");
      }
    }

    if (hs_collapse_level_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      hs_collapse_level_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kHsCollapseLevelKernelName, &err);
      if (err != CL_SUCCESS || hs_collapse_level_kernel_ == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create kernel '" +
                                 std::string(OpenCL::Pipeline::kHsCollapseLevelKernelName) +
                                 "' with error " + std::to_string(err) + ".");
      }
    }

    if (hs_apply_adjusted_l_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      hs_apply_adjusted_l_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kHsApplyAdjustedLKernelName, &err);
      if (err != CL_SUCCESS || hs_apply_adjusted_l_kernel_ == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create kernel '" +
                                 std::string(OpenCL::Pipeline::kHsApplyAdjustedLKernelName) +
                                 "' with error " + std::to_string(err) + ".");
      }
    }

    if (hs_apply_adjusted_l_from_frame_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      hs_apply_adjusted_l_from_frame_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kHsApplyAdjustedLFromFrameKernelName, &err);
      if (err != CL_SUCCESS || hs_apply_adjusted_l_from_frame_kernel_ == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create kernel '" +
                                 std::string(OpenCL::Pipeline::kHsApplyAdjustedLFromFrameKernelName) +
                                 "' with error " + std::to_string(err) + ".");
      }
    }

    if (hs_apply_adjusted_l_from_reference_kernel_ == nullptr) {
      cl_int err = CL_SUCCESS;
      hs_apply_adjusted_l_from_reference_kernel_ =
          clCreateKernel(program, OpenCL::Pipeline::kHsApplyAdjustedLFromReferenceKernelName, &err);
      if (err != CL_SUCCESS || hs_apply_adjusted_l_from_reference_kernel_ == nullptr) {
        throw std::runtime_error(
            "OpenCL fused pipeline: failed to create kernel '" +
            std::string(OpenCL::Pipeline::kHsApplyAdjustedLFromReferenceKernelName) +
            "' with error " + std::to_string(err) + ".");
      }
    }
  }

  void ValidateParamsABI() {
    auto& context = OpenClContext::Instance();
    if (!context.IsInitialized()) {
      throw std::runtime_error("OpenCL fused pipeline: OpenCL context is not initialized.");
    }

    cl_kernel kernel = validate_kernel_;
    if (kernel == nullptr) {
      throw std::runtime_error("OpenCL fused pipeline: validation kernel is null.");
    }

    cl_int err = CL_SUCCESS;
    cl_mem output_buffer =
        clCreateBuffer(context.Context(), CL_MEM_READ_WRITE, 12 * sizeof(float), nullptr, &err);
    if (err != CL_SUCCESS || output_buffer == nullptr) {
      throw std::runtime_error("OpenCL fused pipeline: failed to create validation output buffer.");
    }

    err                = CL_SUCCESS;
    cl_uint arg_index  = 0;
    cl_mem  params_buf = resources_.params_buffer_.Get();
    err |= clSetKernelArg(kernel, arg_index++, sizeof(cl_mem), &params_buf);
    err |= clSetKernelArg(kernel, arg_index++, sizeof(cl_mem), &output_buffer);
    if (err != CL_SUCCESS) {
      clReleaseMemObject(output_buffer);
      throw std::runtime_error("OpenCL fused pipeline: failed to set validation kernel arguments.");
    }

    size_t global_size = 1;
    err = clEnqueueNDRangeKernel(context.Queue(), kernel, 1, nullptr, &global_size, nullptr, 0,
                                 nullptr, nullptr);
    if (err != CL_SUCCESS) {
      clReleaseMemObject(output_buffer);
      throw std::runtime_error("OpenCL fused pipeline: failed to enqueue validation kernel.");
    }

    std::array<float, 12> result{};
    err = clEnqueueReadBuffer(context.Queue(), output_buffer, CL_TRUE, 0,
                              result.size() * sizeof(float), result.data(), 0, nullptr, nullptr);
    clReleaseMemObject(output_buffer);

    if (err != CL_SUCCESS) {
      throw std::runtime_error("OpenCL fused pipeline: failed to read validation output.");
    }

    clFinish(context.Queue());

    const auto nearly_equal = [](float lhs, float rhs) {
      return std::abs(lhs - rhs) <= 1.0e-5f * std::max(1.0f, std::abs(rhs));
    };
    const auto& params                 = resources_.opencl_params_;
    const float expected_output_header = static_cast<float>(params.to_output_params_.method_) +
                                         params.to_output_params_.display_linear_scale_;
    const float expected_open_drt_header = params.to_output_params_.limit_to_display_matx[0] +
                                           params.to_output_params_.open_drt_params_.tn_con_;
    if (!nearly_equal(result[4], expected_output_header) ||
        !nearly_equal(result[5], expected_open_drt_header) ||
        !nearly_equal(result[6], params.to_output_params_.aces_params_.ts_.forward_limit_) ||
        !nearly_equal(result[7], params.to_output_params_.aces_params_.limit_J_max) ||
        !nearly_equal(result[8], params.to_output_params_.open_drt_params_.ts_s_) ||
        !nearly_equal(result[9], params.to_output_params_.open_drt_params_.ts_m2_) ||
        !nearly_equal(result[10], params.to_output_params_.aces_params_.ts_.m_2_) ||
        !nearly_equal(result[11], params.to_output_params_.aces_params_.ts_.g_)) {
      throw std::runtime_error("OpenCL fused pipeline: fused params ABI validation failed.");
    }
  }

  void EnqueueFusedKernel(const opencl::OpenClImage& src) {
    auto& context = OpenClContext::Instance();
    if (!context.IsInitialized()) {
      throw std::runtime_error("OpenCL fused pipeline: context is not initialized.");
    }

    working_.Create(src.Width(), src.Height(), src.Type());

    cl_int             err               = CL_SUCCESS;
    cl_uint            arg_index         = 0;

    cl_mem             src_buffer        = src.Buffer();
    cl_mem             dst_buffer        = working_.Buffer();
    cl_mem             params_buffer     = resources_.params_buffer_.Get();
    cl_mem             lmt_lut_buffer    = resources_.lmt_lut_buffer_.Get();
    cl_int             width             = src.Width();
    cl_int             height            = src.Height();

    static const float kDummyLutEntry[4] = {0.0f, 0.0f, 0.0f, 1.0f};
    cl_mem             fallback_lut      = nullptr;
    if (lmt_lut_buffer == nullptr) {
      fallback_lut =
          clCreateBuffer(context.Context(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                         sizeof(kDummyLutEntry), const_cast<float*>(kDummyLutEntry), &err);
      if (err != CL_SUCCESS || fallback_lut == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create fallback LUT buffer.");
      }
      lmt_lut_buffer = fallback_lut;
    }

    err |= clSetKernelArg(fused_kernel_, arg_index++, sizeof(cl_mem), &src_buffer);
    err |= clSetKernelArg(fused_kernel_, arg_index++, sizeof(cl_mem), &dst_buffer);
    err |= clSetKernelArg(fused_kernel_, arg_index++, sizeof(cl_mem), &params_buffer);
    err |= clSetKernelArg(fused_kernel_, arg_index++, sizeof(cl_mem), &lmt_lut_buffer);
    err |= clSetKernelArg(fused_kernel_, arg_index++, sizeof(cl_int), &width);
    err |= clSetKernelArg(fused_kernel_, arg_index++, sizeof(cl_int), &height);

    if (err != CL_SUCCESS) {
      if (fallback_lut != nullptr) clReleaseMemObject(fallback_lut);
      throw std::runtime_error("OpenCL fused pipeline: failed to set fused kernel arguments.");
    }

    size_t global_size[2] = {static_cast<size_t>(width), static_cast<size_t>(height)};
    err = clEnqueueNDRangeKernel(context.Queue(), fused_kernel_, 2, nullptr, global_size, nullptr,
                                 0, nullptr, nullptr);

    if (fallback_lut != nullptr) {
      clReleaseMemObject(fallback_lut);
    }

    if (err != CL_SUCCESS) {
      throw std::runtime_error("OpenCL fused pipeline: failed to enqueue fused kernel with error " +
                               std::to_string(err) + ".");
    }
  }

  void EnqueueFusedStageKernel(const opencl::OpenClImage& src, opencl::OpenClImage& dst,
                               int stage) {
    auto& context = OpenClContext::Instance();
    if (!context.IsInitialized()) {
      throw std::runtime_error("OpenCL fused pipeline: context is not initialized.");
    }

    dst.Create(src.Width(), src.Height(), src.Type());

    cl_int  err            = CL_SUCCESS;
    cl_uint arg_index      = 0;
    cl_mem  src_buffer     = src.Buffer();
    cl_mem  dst_buffer     = dst.Buffer();
    cl_mem  params_buffer  = resources_.params_buffer_.Get();
    cl_mem  lmt_lut_buffer = resources_.lmt_lut_buffer_.Get();
    cl_int  width          = src.Width();
    cl_int  height         = src.Height();
    cl_int  stage_arg      = stage;

    static const float kDummyLutEntry[4] = {0.0f, 0.0f, 0.0f, 1.0f};
    cl_mem             fallback_lut      = nullptr;
    if (lmt_lut_buffer == nullptr) {
      fallback_lut =
          clCreateBuffer(context.Context(), CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                         sizeof(kDummyLutEntry), const_cast<float*>(kDummyLutEntry), &err);
      if (err != CL_SUCCESS || fallback_lut == nullptr) {
        throw std::runtime_error("OpenCL fused pipeline: failed to create fallback LUT buffer.");
      }
      lmt_lut_buffer = fallback_lut;
    }

    err |= clSetKernelArg(fused_stage_kernel_, arg_index++, sizeof(cl_mem), &src_buffer);
    err |= clSetKernelArg(fused_stage_kernel_, arg_index++, sizeof(cl_mem), &dst_buffer);
    err |= clSetKernelArg(fused_stage_kernel_, arg_index++, sizeof(cl_mem), &params_buffer);
    err |= clSetKernelArg(fused_stage_kernel_, arg_index++, sizeof(cl_mem), &lmt_lut_buffer);
    err |= clSetKernelArg(fused_stage_kernel_, arg_index++, sizeof(cl_int), &width);
    err |= clSetKernelArg(fused_stage_kernel_, arg_index++, sizeof(cl_int), &height);
    err |= clSetKernelArg(fused_stage_kernel_, arg_index++, sizeof(cl_int), &stage_arg);

    if (err != CL_SUCCESS) {
      if (fallback_lut != nullptr) clReleaseMemObject(fallback_lut);
      throw std::runtime_error("OpenCL fused pipeline: failed to set fused stage arguments.");
    }

    size_t global_size[2] = {static_cast<size_t>(width), static_cast<size_t>(height)};
    err = clEnqueueNDRangeKernel(context.Queue(), fused_stage_kernel_, 2, nullptr, global_size,
                                 nullptr, 0, nullptr, nullptr);

    if (fallback_lut != nullptr) {
      clReleaseMemObject(fallback_lut);
    }

    if (err != CL_SUCCESS) {
      throw std::runtime_error("OpenCL fused pipeline: failed to enqueue fused stage with error " +
                               std::to_string(err) + ".");
    }
  }

  void EnqueueNeighborBlurHorizontal(const opencl::OpenClImage& src, opencl::OpenClImage& dst,
                                     cl_mem stage_buffer) {
    auto& context = OpenClContext::Instance();

    dst.Create(src.Width(), src.Height(), src.Type());

    cl_int  err       = CL_SUCCESS;
    cl_uint arg_index = 0;
    cl_mem  src_buf   = src.Buffer();
    cl_mem  dst_buf   = dst.Buffer();
    cl_int  width     = src.Width();
    cl_int  height    = src.Height();

    err |= clSetKernelArg(blur_h_kernel_, arg_index++, sizeof(cl_mem), &src_buf);
    err |= clSetKernelArg(blur_h_kernel_, arg_index++, sizeof(cl_mem), &dst_buf);
    err |= clSetKernelArg(blur_h_kernel_, arg_index++, sizeof(cl_mem), &stage_buffer);
    err |= clSetKernelArg(blur_h_kernel_, arg_index++, sizeof(cl_int), &width);
    err |= clSetKernelArg(blur_h_kernel_, arg_index++, sizeof(cl_int), &height);

    if (err != CL_SUCCESS) {
      throw std::runtime_error(
          "OpenCL fused pipeline: failed to set blur horizontal kernel arguments.");
    }

    size_t global_size[2] = {static_cast<size_t>(width), static_cast<size_t>(height)};
    err = clEnqueueNDRangeKernel(context.Queue(), blur_h_kernel_, 2, nullptr, global_size, nullptr,
                                 0, nullptr, nullptr);
    if (err != CL_SUCCESS) {
      throw std::runtime_error(
          "OpenCL fused pipeline: failed to enqueue blur horizontal kernel with error " +
          std::to_string(err) + ".");
    }
  }

  void EnqueueNeighborApplyVertical(const opencl::OpenClImage& src,
                                    const opencl::OpenClImage& blur_horizontal,
                                    opencl::OpenClImage& dst, cl_mem stage_buffer) {
    auto& context = OpenClContext::Instance();

    dst.Create(src.Width(), src.Height(), src.Type());

    cl_int  err       = CL_SUCCESS;
    cl_uint arg_index = 0;
    cl_mem  src_buf   = src.Buffer();
    cl_mem  blur_buf  = blur_horizontal.Buffer();
    cl_mem  dst_buf   = dst.Buffer();
    cl_int  width     = src.Width();
    cl_int  height    = src.Height();

    err |= clSetKernelArg(apply_v_kernel_, arg_index++, sizeof(cl_mem), &src_buf);
    err |= clSetKernelArg(apply_v_kernel_, arg_index++, sizeof(cl_mem), &blur_buf);
    err |= clSetKernelArg(apply_v_kernel_, arg_index++, sizeof(cl_mem), &dst_buf);
    err |= clSetKernelArg(apply_v_kernel_, arg_index++, sizeof(cl_mem), &stage_buffer);
    err |= clSetKernelArg(apply_v_kernel_, arg_index++, sizeof(cl_int), &width);
    err |= clSetKernelArg(apply_v_kernel_, arg_index++, sizeof(cl_int), &height);

    if (err != CL_SUCCESS) {
      throw std::runtime_error(
          "OpenCL fused pipeline: failed to set neighbor apply vertical kernel arguments.");
    }

    size_t global_size[2] = {static_cast<size_t>(width), static_cast<size_t>(height)};
    err = clEnqueueNDRangeKernel(context.Queue(), apply_v_kernel_, 2, nullptr, global_size, nullptr,
                                 0, nullptr, nullptr);
    if (err != CL_SUCCESS) {
      throw std::runtime_error(
          "OpenCL fused pipeline: failed to enqueue neighbor apply vertical kernel with error " +
          std::to_string(err) + ".");
    }
  }

  void EnqueueKernel2D(cl_kernel kernel, int width, int height, const char* label) {
    auto& context = OpenClContext::Instance();
    size_t global_size[2] = {static_cast<size_t>(width), static_cast<size_t>(height)};
    const cl_int err =
        clEnqueueNDRangeKernel(context.Queue(), kernel, 2, nullptr, global_size, nullptr, 0,
                               nullptr, nullptr);
    if (err != CL_SUCCESS) {
      throw std::runtime_error(std::string("OpenCL fused pipeline: failed to enqueue ") + label +
                               " with error " + std::to_string(err) + ".");
    }
  }

  void EnqueueHsExtractLogIntensity(const opencl::OpenClImage& src) {
    cl_int  err           = CL_SUCCESS;
    cl_uint arg_index     = 0;
    cl_mem  src_buf       = src.Buffer();
    cl_mem  dst_buf       = hs_source_levels_[0];
    cl_int  width         = src.Width();
    cl_int  height        = src.Height();

    err |= clSetKernelArg(hs_extract_kernel_, arg_index++, sizeof(cl_mem), &src_buf);
    err |= clSetKernelArg(hs_extract_kernel_, arg_index++, sizeof(cl_mem), &dst_buf);
    err |= clSetKernelArg(hs_extract_kernel_, arg_index++, sizeof(cl_int), &width);
    err |= clSetKernelArg(hs_extract_kernel_, arg_index++, sizeof(cl_int), &height);
    if (err != CL_SUCCESS) {
      throw std::runtime_error(
          "OpenCL fused pipeline: failed to set H/S extract log-intensity arguments.");
    }

    EnqueueKernel2D(hs_extract_kernel_, width, height, "H/S extract log-intensity kernel");
  }

  void EnqueueHsExtractLogIntensityResampled(const opencl::OpenClImage& src) {
    cl_int  err           = CL_SUCCESS;
    cl_uint arg_index     = 0;
    cl_mem  src_buf       = src.Buffer();
    cl_mem  dst_buf       = hs_source_levels_[0];
    cl_int  src_width     = src.Width();
    cl_int  src_height    = src.Height();
    cl_int  dst_width     = hs_level_widths_[0];
    cl_int  dst_height    = hs_level_heights_[0];

    err |= clSetKernelArg(hs_extract_resampled_kernel_, arg_index++, sizeof(cl_mem), &src_buf);
    err |= clSetKernelArg(hs_extract_resampled_kernel_, arg_index++, sizeof(cl_mem), &dst_buf);
    err |= clSetKernelArg(hs_extract_resampled_kernel_, arg_index++, sizeof(cl_int), &src_width);
    err |= clSetKernelArg(hs_extract_resampled_kernel_, arg_index++, sizeof(cl_int), &src_height);
    err |= clSetKernelArg(hs_extract_resampled_kernel_, arg_index++, sizeof(cl_int), &dst_width);
    err |= clSetKernelArg(hs_extract_resampled_kernel_, arg_index++, sizeof(cl_int), &dst_height);
    if (err != CL_SUCCESS) {
      throw std::runtime_error(
          "OpenCL fused pipeline: failed to set H/S resampled extract arguments.");
    }

    EnqueueKernel2D(hs_extract_resampled_kernel_, dst_width, dst_height,
                    "H/S resampled extract kernel");
  }

  void EnqueueHsPyrDown(cl_mem src, int src_width, int src_height, cl_mem dst, int dst_width,
                        int dst_height) {
    cl_int  err       = CL_SUCCESS;
    cl_uint arg_index = 0;
    err |= clSetKernelArg(hs_pyr_down_kernel_, arg_index++, sizeof(cl_mem), &src);
    err |= clSetKernelArg(hs_pyr_down_kernel_, arg_index++, sizeof(cl_mem), &dst);
    err |= clSetKernelArg(hs_pyr_down_kernel_, arg_index++, sizeof(cl_int), &src_width);
    err |= clSetKernelArg(hs_pyr_down_kernel_, arg_index++, sizeof(cl_int), &src_height);
    err |= clSetKernelArg(hs_pyr_down_kernel_, arg_index++, sizeof(cl_int), &dst_width);
    err |= clSetKernelArg(hs_pyr_down_kernel_, arg_index++, sizeof(cl_int), &dst_height);
    if (err != CL_SUCCESS) {
      throw std::runtime_error(
          "OpenCL fused pipeline: failed to set H/S pyr-down arguments.");
    }
    EnqueueKernel2D(hs_pyr_down_kernel_, dst_width, dst_height, "H/S pyr-down kernel");
  }

  void BuildHsSourcePyramid(const opencl::OpenClImage& src) {
    if (hs_level_widths_[0] == src.Width() && hs_level_heights_[0] == src.Height()) {
      EnqueueHsExtractLogIntensity(src);
    } else {
      EnqueueHsExtractLogIntensityResampled(src);
    }
    for (int level = 1; level < hs_level_count_; ++level) {
      EnqueueHsPyrDown(hs_source_levels_[level - 1], hs_level_widths_[level - 1],
                       hs_level_heights_[level - 1], hs_source_levels_[level],
                       hs_level_widths_[level], hs_level_heights_[level]);
    }
  }

  void BuildHsRemapPyramid(const HsLlfSample& sample,
                           std::array<cl_mem, kHsMaxLevels>& remap_levels) {
    cl_int  err       = CL_SUCCESS;
    cl_uint arg_index = 0;
    cl_mem  src_buf   = hs_source_levels_[0];
    cl_mem  dst_buf   = remap_levels[0];
    cl_int  width     = hs_level_widths_[0];
    cl_int  height    = hs_level_heights_[0];
    cl_float gamma    = sample.gamma;
    cl_float target   = sample.target;
    cl_float beta     = sample.beta;
    cl_float alpha    = sample.alpha;
    cl_float sigma_r  = kHsBaseSigmaR;

    err |= clSetKernelArg(hs_build_remapped_sample_kernel_, arg_index++, sizeof(cl_mem), &src_buf);
    err |= clSetKernelArg(hs_build_remapped_sample_kernel_, arg_index++, sizeof(cl_mem), &dst_buf);
    err |= clSetKernelArg(hs_build_remapped_sample_kernel_, arg_index++, sizeof(cl_int), &width);
    err |= clSetKernelArg(hs_build_remapped_sample_kernel_, arg_index++, sizeof(cl_int), &height);
    err |= clSetKernelArg(hs_build_remapped_sample_kernel_, arg_index++, sizeof(cl_float), &gamma);
    err |= clSetKernelArg(hs_build_remapped_sample_kernel_, arg_index++, sizeof(cl_float),
                          &target);
    err |= clSetKernelArg(hs_build_remapped_sample_kernel_, arg_index++, sizeof(cl_float), &beta);
    err |= clSetKernelArg(hs_build_remapped_sample_kernel_, arg_index++, sizeof(cl_float), &alpha);
    err |= clSetKernelArg(hs_build_remapped_sample_kernel_, arg_index++, sizeof(cl_float),
                          &sigma_r);
    if (err != CL_SUCCESS) {
      throw std::runtime_error(
          "OpenCL fused pipeline: failed to set H/S remapped-sample arguments.");
    }
    EnqueueKernel2D(hs_build_remapped_sample_kernel_, width, height,
                    "H/S remapped-sample kernel");

    for (int level = 1; level < hs_level_count_; ++level) {
      EnqueueHsPyrDown(remap_levels[level - 1], hs_level_widths_[level - 1],
                       hs_level_heights_[level - 1], remap_levels[level],
                       hs_level_widths_[level], hs_level_heights_[level]);
    }
  }

  auto ShouldRunHighlightShadowLocalTone() const -> bool {
    if (!fused_params_.hs_local_tone_enabled_) {
      return false;
    }
    const float shadow_amount =
        fused_params_.shadows_enabled_
            ? std::clamp(fused_params_.shadows_offset_, -kHsBackendAmountLimit,
                         kHsBackendAmountLimit)
                                       : 0.0f;
    const float highlight_amount =
        fused_params_.highlights_enabled_
            ? std::clamp(-fused_params_.highlights_offset_, -kHsBackendAmountLimit,
                         kHsBackendAmountLimit)
            : 0.0f;
    return std::abs(shadow_amount) > 1.0e-6f || std::abs(highlight_amount) > 1.0e-6f;
  }

  void BuildHsOutputPyramid(const std::vector<HsLlfSample>& samples) {
    auto& context = OpenClContext::Instance();
    const float zero = 0.0f;
    for (int level = 0; level < hs_level_count_; ++level) {
      const size_t elems =
          static_cast<size_t>(hs_level_widths_[level]) *
          static_cast<size_t>(hs_level_heights_[level]);
      const cl_int err = clEnqueueFillBuffer(context.Queue(), hs_output_levels_[level], &zero,
                                             sizeof(zero), 0, elems * sizeof(float), 0, nullptr,
                                             nullptr);
      if (err != CL_SUCCESS) {
        throw std::runtime_error("OpenCL fused pipeline: failed to clear H/S output level.");
      }
    }

    BuildHsRemapPyramid(samples.front(), hs_remap_a_levels_);
    BuildHsRemapPyramid(samples[1], hs_remap_b_levels_);

    for (size_t pair_index = 0; pair_index + 1 < samples.size(); ++pair_index) {
      for (int level = 0; level < hs_level_count_; ++level) {
        const bool top_level = level == (hs_level_count_ - 1);
        const int coarse_width = top_level ? 1 : hs_level_widths_[level + 1];
        const int coarse_height = top_level ? 1 : hs_level_heights_[level + 1];
        cl_mem source_level = hs_source_levels_[level];
        cl_mem sample_lo_level = hs_remap_a_levels_[level];
        cl_mem sample_lo_coarse = top_level ? hs_remap_a_levels_[level] : hs_remap_a_levels_[level + 1];
        cl_mem sample_hi_level = hs_remap_b_levels_[level];
        cl_mem sample_hi_coarse = top_level ? hs_remap_b_levels_[level] : hs_remap_b_levels_[level + 1];
        cl_mem output_level = hs_output_levels_[level];
        cl_int width = hs_level_widths_[level];
        cl_int height = hs_level_heights_[level];
        cl_float gamma_lo = samples[pair_index].gamma;
        cl_float gamma_hi = samples[pair_index + 1].gamma;
        cl_int first_pair = pair_index == 0 ? 1 : 0;
        cl_int last_pair = pair_index + 2 == samples.size() ? 1 : 0;
        cl_int top_level_arg = top_level ? 1 : 0;

        cl_int err = CL_SUCCESS;
        cl_uint arg_index = 0;
        err |= clSetKernelArg(hs_select_interpolated_level_kernel_, arg_index++, sizeof(cl_mem),
                              &source_level);
        err |= clSetKernelArg(hs_select_interpolated_level_kernel_, arg_index++, sizeof(cl_mem),
                              &sample_lo_level);
        err |= clSetKernelArg(hs_select_interpolated_level_kernel_, arg_index++, sizeof(cl_mem),
                              &sample_lo_coarse);
        err |= clSetKernelArg(hs_select_interpolated_level_kernel_, arg_index++, sizeof(cl_mem),
                              &sample_hi_level);
        err |= clSetKernelArg(hs_select_interpolated_level_kernel_, arg_index++, sizeof(cl_mem),
                              &sample_hi_coarse);
        err |= clSetKernelArg(hs_select_interpolated_level_kernel_, arg_index++, sizeof(cl_mem),
                              &output_level);
        err |= clSetKernelArg(hs_select_interpolated_level_kernel_, arg_index++, sizeof(cl_int),
                              &width);
        err |= clSetKernelArg(hs_select_interpolated_level_kernel_, arg_index++, sizeof(cl_int),
                              &height);
        err |= clSetKernelArg(hs_select_interpolated_level_kernel_, arg_index++, sizeof(cl_int),
                              &coarse_width);
        err |= clSetKernelArg(hs_select_interpolated_level_kernel_, arg_index++, sizeof(cl_int),
                              &coarse_height);
        err |= clSetKernelArg(hs_select_interpolated_level_kernel_, arg_index++, sizeof(cl_float),
                              &gamma_lo);
        err |= clSetKernelArg(hs_select_interpolated_level_kernel_, arg_index++, sizeof(cl_float),
                              &gamma_hi);
        err |= clSetKernelArg(hs_select_interpolated_level_kernel_, arg_index++, sizeof(cl_int),
                              &first_pair);
        err |= clSetKernelArg(hs_select_interpolated_level_kernel_, arg_index++, sizeof(cl_int),
                              &last_pair);
        err |= clSetKernelArg(hs_select_interpolated_level_kernel_, arg_index++, sizeof(cl_int),
                              &top_level_arg);
        if (err != CL_SUCCESS) {
          throw std::runtime_error(
              "OpenCL fused pipeline: failed to set H/S select-level arguments.");
        }
        EnqueueKernel2D(hs_select_interpolated_level_kernel_, width, height,
                        "H/S select-level kernel");
      }

      if (pair_index + 2 < samples.size()) {
        std::swap(hs_remap_a_levels_, hs_remap_b_levels_);
        BuildHsRemapPyramid(samples[pair_index + 2], hs_remap_b_levels_);
      }
    }

    for (int level = hs_level_count_ - 2; level >= 0; --level) {
      cl_mem lap_level = hs_output_levels_[level];
      cl_mem coarse_level = hs_output_levels_[level + 1];
      cl_mem dst_level = hs_remap_a_levels_[level];
      cl_int width = hs_level_widths_[level];
      cl_int height = hs_level_heights_[level];
      cl_int coarse_width = hs_level_widths_[level + 1];
      cl_int coarse_height = hs_level_heights_[level + 1];

      cl_int err = CL_SUCCESS;
      cl_uint arg_index = 0;
      err |= clSetKernelArg(hs_collapse_level_kernel_, arg_index++, sizeof(cl_mem), &lap_level);
      err |= clSetKernelArg(hs_collapse_level_kernel_, arg_index++, sizeof(cl_mem), &coarse_level);
      err |= clSetKernelArg(hs_collapse_level_kernel_, arg_index++, sizeof(cl_mem), &dst_level);
      err |= clSetKernelArg(hs_collapse_level_kernel_, arg_index++, sizeof(cl_int), &width);
      err |= clSetKernelArg(hs_collapse_level_kernel_, arg_index++, sizeof(cl_int), &height);
      err |= clSetKernelArg(hs_collapse_level_kernel_, arg_index++, sizeof(cl_int),
                            &coarse_width);
      err |= clSetKernelArg(hs_collapse_level_kernel_, arg_index++, sizeof(cl_int),
                            &coarse_height);
      if (err != CL_SUCCESS) {
        throw std::runtime_error(
            "OpenCL fused pipeline: failed to set H/S collapse-level arguments.");
      }
      EnqueueKernel2D(hs_collapse_level_kernel_, width, height, "H/S collapse-level kernel");
      std::swap(hs_output_levels_[level], hs_remap_a_levels_[level]);
    }
  }

  void EnqueueHsApplyAdjustedL(const opencl::OpenClImage& src, opencl::OpenClImage& dst) {
    dst.Create(src.Width(), src.Height(), src.Type());

    cl_int err = CL_SUCCESS;
    cl_uint arg_index = 0;
    cl_mem src_buf = src.Buffer();
    cl_mem adjusted_buf = hs_output_levels_[0];
    cl_mem dst_buf = dst.Buffer();
    cl_int width = src.Width();
    cl_int height = src.Height();
    err |= clSetKernelArg(hs_apply_adjusted_l_kernel_, arg_index++, sizeof(cl_mem), &src_buf);
    err |= clSetKernelArg(hs_apply_adjusted_l_kernel_, arg_index++, sizeof(cl_mem),
                          &adjusted_buf);
    err |= clSetKernelArg(hs_apply_adjusted_l_kernel_, arg_index++, sizeof(cl_mem), &dst_buf);
    err |= clSetKernelArg(hs_apply_adjusted_l_kernel_, arg_index++, sizeof(cl_int), &width);
    err |= clSetKernelArg(hs_apply_adjusted_l_kernel_, arg_index++, sizeof(cl_int), &height);
    if (err != CL_SUCCESS) {
      throw std::runtime_error("OpenCL fused pipeline: failed to set H/S apply adjusted-L arguments.");
    }
    EnqueueKernel2D(hs_apply_adjusted_l_kernel_, width, height, "H/S apply adjusted-L kernel");
  }

  void EnqueueHsApplyAdjustedLFromFrame(const opencl::OpenClImage& src, opencl::OpenClImage& dst) {
    dst.Create(src.Width(), src.Height(), src.Type());

    cl_int err = CL_SUCCESS;
    cl_uint arg_index = 0;
    cl_mem src_buf = src.Buffer();
    cl_mem reference_buf = hs_source_levels_[0];
    cl_mem adjusted_buf = hs_output_levels_[0];
    cl_mem dst_buf = dst.Buffer();
    cl_int width = src.Width();
    cl_int height = src.Height();
    cl_int adjusted_width = hs_cached_width_;
    cl_int adjusted_height = hs_cached_height_;
    err |= clSetKernelArg(hs_apply_adjusted_l_from_frame_kernel_, arg_index++, sizeof(cl_mem),
                          &src_buf);
    err |= clSetKernelArg(hs_apply_adjusted_l_from_frame_kernel_, arg_index++, sizeof(cl_mem),
                          &reference_buf);
    err |= clSetKernelArg(hs_apply_adjusted_l_from_frame_kernel_, arg_index++, sizeof(cl_mem),
                          &adjusted_buf);
    err |= clSetKernelArg(hs_apply_adjusted_l_from_frame_kernel_, arg_index++, sizeof(cl_mem),
                          &dst_buf);
    err |= clSetKernelArg(hs_apply_adjusted_l_from_frame_kernel_, arg_index++, sizeof(cl_int),
                          &width);
    err |= clSetKernelArg(hs_apply_adjusted_l_from_frame_kernel_, arg_index++, sizeof(cl_int),
                          &height);
    err |= clSetKernelArg(hs_apply_adjusted_l_from_frame_kernel_, arg_index++, sizeof(cl_int),
                          &adjusted_width);
    err |= clSetKernelArg(hs_apply_adjusted_l_from_frame_kernel_, arg_index++, sizeof(cl_int),
                          &adjusted_height);
    if (err != CL_SUCCESS) {
      throw std::runtime_error(
          "OpenCL fused pipeline: failed to set H/S apply adjusted-L-from-frame arguments.");
    }
    EnqueueKernel2D(hs_apply_adjusted_l_from_frame_kernel_, width, height,
                    "H/S apply adjusted-L-from-frame kernel");
  }

  void EnqueueHsApplyAdjustedLFromReference(const opencl::OpenClImage& src,
                                            opencl::OpenClImage& dst) {
    dst.Create(src.Width(), src.Height(), src.Type());

    cl_int err = CL_SUCCESS;
    cl_uint arg_index = 0;
    cl_mem src_buf = src.Buffer();
    cl_mem reference_buf = hs_source_levels_[0];
    cl_mem adjusted_buf = hs_output_levels_[0];
    cl_mem dst_buf = dst.Buffer();
    cl_mem params_buf = resources_.params_buffer_.Get();
    cl_int width = src.Width();
    cl_int height = src.Height();
    cl_int adjusted_width = hs_cached_width_;
    cl_int adjusted_height = hs_cached_height_;
    err |= clSetKernelArg(hs_apply_adjusted_l_from_reference_kernel_, arg_index++, sizeof(cl_mem),
                          &src_buf);
    err |= clSetKernelArg(hs_apply_adjusted_l_from_reference_kernel_, arg_index++, sizeof(cl_mem),
                          &reference_buf);
    err |= clSetKernelArg(hs_apply_adjusted_l_from_reference_kernel_, arg_index++, sizeof(cl_mem),
                          &adjusted_buf);
    err |= clSetKernelArg(hs_apply_adjusted_l_from_reference_kernel_, arg_index++, sizeof(cl_mem),
                          &dst_buf);
    err |= clSetKernelArg(hs_apply_adjusted_l_from_reference_kernel_, arg_index++, sizeof(cl_mem),
                          &params_buf);
    err |= clSetKernelArg(hs_apply_adjusted_l_from_reference_kernel_, arg_index++, sizeof(cl_int),
                          &width);
    err |= clSetKernelArg(hs_apply_adjusted_l_from_reference_kernel_, arg_index++, sizeof(cl_int),
                          &height);
    err |= clSetKernelArg(hs_apply_adjusted_l_from_reference_kernel_, arg_index++, sizeof(cl_int),
                          &adjusted_width);
    err |= clSetKernelArg(hs_apply_adjusted_l_from_reference_kernel_, arg_index++, sizeof(cl_int),
                          &adjusted_height);
    if (err != CL_SUCCESS) {
      throw std::runtime_error(
          "OpenCL fused pipeline: failed to set H/S apply adjusted-L-from-reference arguments.");
    }
    EnqueueKernel2D(hs_apply_adjusted_l_from_reference_kernel_, width, height,
                    "H/S apply adjusted-L-from-reference kernel");
  }

  void EnqueueHighlightShadowLocalTone(const opencl::OpenClImage& src, opencl::OpenClImage& dst) {
    const float shadow_amount =
        fused_params_.shadows_enabled_
            ? std::clamp(fused_params_.shadows_offset_, -kHsBackendAmountLimit,
                         kHsBackendAmountLimit)
                                       : 0.0f;
    const float highlight_amount =
        fused_params_.highlights_enabled_
            ? std::clamp(-fused_params_.highlights_offset_, -kHsBackendAmountLimit,
                         kHsBackendAmountLimit)
            : 0.0f;
    const std::uint64_t adjusted_cache_key =
        BuildAdjustedResultCacheKey(fused_params_, shadow_amount, highlight_amount);

    const bool roi_frame_with_source_reference = fused_params_.render_roi_enabled_ &&
                                                 fused_params_.render_roi_reference_width_ > 0 &&
                                                 fused_params_.render_roi_reference_height_ > 0;
    const bool preserve_source_detail = fused_params_.render_hs_preserve_source_detail_;
    const int reference_max_long_edge =
        std::max(1, fused_params_.render_hs_reference_max_long_edge_);
    const HsMaskDimensions current_reference_dims =
        ComputeHsMaskDimensions(src.Width(), src.Height(),
                                roi_frame_with_source_reference
                                    ? std::max(src.Width(), src.Height())
                                    : reference_max_long_edge);
    const std::uint64_t reference_source_cache_key = fused_params_.hs_mask_base_cache_key_;
    std::uint64_t reference_cache_key = adjusted_cache_key;
    HashCombine(reference_cache_key, static_cast<std::uint64_t>(preserve_source_detail));
    const bool reference_source_cache_valid =
        hs_cached_reference_base_ && hs_source_levels_[0] != nullptr &&
        hs_cached_source_key_ == reference_source_cache_key && hs_cached_width_ > 0 &&
        hs_cached_height_ > 0 && hs_cached_frame_width_ > 0 &&
        hs_cached_frame_height_ > 0 && hs_cached_pitch_ > 0;
    const bool reference_result_cache_valid =
        reference_source_cache_valid && hs_output_levels_[0] != nullptr &&
        hs_cached_key_ == reference_cache_key;
    const int current_reference_long_edge =
        std::max(current_reference_dims.width, current_reference_dims.height);
    const int cached_reference_long_edge = std::max(hs_cached_width_, hs_cached_height_);
    const bool current_can_improve_reference =
        fused_params_.render_hs_can_seed_reference_ &&
        current_reference_long_edge > cached_reference_long_edge;
    const auto ensure_reference_output = [&]() {
      if (!reference_result_cache_valid) {
        const auto samples = BuildHsSamples(shadow_amount, highlight_amount);
        BuildHsOutputPyramid(samples);
        hs_cached_key_ = reference_cache_key;
      }
    };
    if (roi_frame_with_source_reference && reference_source_cache_valid &&
        hs_cached_frame_width_ == fused_params_.render_roi_reference_width_ &&
        hs_cached_frame_height_ == fused_params_.render_roi_reference_height_) {
      ensure_reference_output();
      EnqueueHsApplyAdjustedLFromReference(src, dst);
      return;
    }
    if (!roi_frame_with_source_reference && reference_source_cache_valid &&
        !current_can_improve_reference &&
        (hs_cached_frame_width_ != src.Width() || hs_cached_frame_height_ != src.Height())) {
      ensure_reference_output();
      EnqueueHsApplyAdjustedLFromFrame(src, dst);
      return;
    }

    const bool build_roi_local_reference = roi_frame_with_source_reference;
    const bool seed_canonical_reference =
        fused_params_.render_hs_can_seed_reference_ && !build_roi_local_reference;
    std::uint64_t render_cache_key =
        build_roi_local_reference ? BuildRoiAdjustedResultCacheKey(fused_params_, reference_cache_key)
                                  : reference_cache_key;
    const HsMaskDimensions mask_dims = current_reference_dims;
    EnsureHsPyramidBuffers(mask_dims.width, mask_dims.height, fused_params_.hs_base_radius_);
    const bool cache_valid =
        hs_output_levels_[0] != nullptr && hs_cached_key_ == render_cache_key &&
        hs_cached_frame_width_ == src.Width() && hs_cached_frame_height_ == src.Height() &&
        hs_cached_width_ == mask_dims.width && hs_cached_height_ == mask_dims.height &&
        hs_cached_pitch_ == hs_level_widths_[0];
    if (!cache_valid) {
      const auto samples = BuildHsSamples(shadow_amount, highlight_amount);
      BuildHsSourcePyramid(src);
      BuildHsOutputPyramid(samples);
      hs_cached_key_ = render_cache_key;
      hs_cached_source_key_ = seed_canonical_reference ? reference_source_cache_key : 0;
      hs_cached_width_ = mask_dims.width;
      hs_cached_height_ = mask_dims.height;
      hs_cached_frame_width_ = src.Width();
      hs_cached_frame_height_ = src.Height();
      hs_cached_pitch_ = hs_level_widths_[0];
      hs_cached_reference_base_ = seed_canonical_reference;
    }

    const bool release_after_dispatch = AllocatedHsPyramidBytes() > kHsMaxRetainedMaskBytes;
    if (hs_cached_width_ == src.Width() && hs_cached_height_ == src.Height()) {
      EnqueueHsApplyAdjustedL(src, dst);
    } else {
      EnqueueHsApplyAdjustedLFromFrame(src, dst);
    }
    if (release_after_dispatch) {
      clFinish(OpenClContext::Instance().Queue());
      ReleaseHsBaseBuffers();
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

  auto BuildNeighborStages() const -> std::vector<OpenClNeighborStage> {
    std::vector<OpenClNeighborStage> stages;
    stages.reserve(2);

    if (ShouldRunSharpen()) {
      stages.push_back(OpenClNeighborStage{BuildNeighborStageParams(
          OpenClNeighborOpKind::Sharpen, fused_params_.sharpen_radius_,
          fused_params_.sharpen_offset_, fused_params_.sharpen_threshold_,
          fused_params_.sharpen_gaussian_tap_count_, fused_params_.sharpen_gaussian_weights_)});
    }
    if (ShouldRunClarity()) {
      stages.push_back(OpenClNeighborStage{BuildNeighborStageParams(
          OpenClNeighborOpKind::Clarity, fused_params_.clarity_radius_,
          fused_params_.clarity_offset_, 0.0f, fused_params_.clarity_gaussian_tap_count_,
          fused_params_.clarity_gaussian_weights_)});
    }

    return stages;
  }

 public:
  void SetInputImage(std::shared_ptr<ImageBuffer> input_image) override {
    input_img_ = std::move(input_image);
  }

  void SetParams(OperatorParams& params) override {
    cpu_params_   = &params;
    fused_params_ = FusedParamsConverter::ConvertFromCPU(params, fused_params_);
    resources_ =
        OpenCL::Pipeline::OpenClFusedParamUploader::Upload(fused_params_, params, resources_);
  }

  void SetFrameSink(IFrameSink* frame_sink) override { frame_sink_ = frame_sink; }

  void Execute(std::shared_ptr<ImageBuffer> output_img) override {
    using ProfileClock    = std::chrono::steady_clock;
    const auto exec_start = ProfileClock::now();

    if (!cpu_params_) {
      throw std::runtime_error("OpenCL fused pipeline: parameters were not set.");
    }

    double ensure_input_ms     = 0.0;
    double ensure_kernels_ms   = 0.0;
    double validate_abi_ms     = 0.0;
    double fused_kernel_ms     = 0.0;
    double detail_ms           = 0.0;
    double sync_ms             = 0.0;
    double download_ms         = 0.0;
    double submit_ms           = 0.0;
    bool   submitted_gpu_frame = false;

    {
      const auto t0 = ProfileClock::now();
      EnsureOpenClInput();
      ensure_input_ms = std::chrono::duration<double, std::milli>(ProfileClock::now() - t0).count();
    }

    {
      const auto t0 = ProfileClock::now();
      EnsureFusedKernels();
      ValidateParamsABI();
      validate_abi_ms = std::chrono::duration<double, std::milli>(ProfileClock::now() - t0).count();
    }

    const auto neighbor_stages = BuildNeighborStages();
    const bool run_hs_local_tone = ShouldRunHighlightShadowLocalTone();

    {
      const auto t0 = ProfileClock::now();
      if (run_hs_local_tone || !neighbor_stages.empty()) {
        EnsureDetailKernels();
      }
      ensure_kernels_ms =
          std::chrono::duration<double, std::milli>(ProfileClock::now() - t0).count();
    }

    const auto& input = input_img_->GetOpenClImage();

    {
      const auto t0 = ProfileClock::now();
      if (run_hs_local_tone) {
        EnqueueFusedStageKernel(input, pre_hs_working_, 1);
        EnqueueHighlightShadowLocalTone(pre_hs_working_, hs_working_);
        EnqueueFusedStageKernel(hs_working_, working_, 2);
      } else {
        EnqueueFusedKernel(input);
      }
      fused_kernel_ms = std::chrono::duration<double, std::milli>(ProfileClock::now() - t0).count();
    }

    opencl::OpenClImage*                        detail_src = &working_;
    opencl::OpenClImage*                        detail_dst = &detail_scratch_;

    std::vector<OpenCL::Pipeline::OpenClBuffer> stage_buffers;
    stage_buffers.reserve(neighbor_stages.size());

    {
      const auto t0 = ProfileClock::now();
      for (const auto& stage : neighbor_stages) {
        stage_buffers.push_back(UploadStageParams(stage.params_));
        cl_mem stage_buffer = stage_buffers.back().Get();

        EnqueueNeighborBlurHorizontal(*detail_src, blur_horizontal_, stage_buffer);
        EnqueueNeighborApplyVertical(*detail_src, blur_horizontal_, *detail_dst, stage_buffer);

        std::swap(detail_src, detail_dst);
      }
      detail_ms = std::chrono::duration<double, std::milli>(ProfileClock::now() - t0).count();
    }

    {
      const auto t0      = ProfileClock::now();
      auto&      context = OpenClContext::Instance();
      clFinish(context.Queue());
      sync_ms = std::chrono::duration<double, std::milli>(ProfileClock::now() - t0).count();
    }

    if (frame_sink_) {
      const ViewerDisplayConfig display_config = ResolveViewerDisplayConfig(*cpu_params_);
      submitted_gpu_frame = TrySubmitOpenClFrameToSink(*detail_src, *frame_sink_);
      if (!submitted_gpu_frame) {
        cv::Mat host_image;
        {
          const auto t0 = ProfileClock::now();
          detail_src->Download(host_image);
          download_ms = std::chrono::duration<double, std::milli>(ProfileClock::now() - t0).count();
        }

        if (host_image.type() != CV_32FC4) {
          throw std::runtime_error(
              "OpenCL fused pipeline: expected RGBA32F host frame for viewer.");
        }

        const size_t row_bytes =
            static_cast<size_t>(host_image.cols) * static_cast<size_t>(sizeof(cv::Vec4f));
        auto host_pixels = std::make_shared<std::vector<float>>(
            static_cast<size_t>(host_image.cols) * static_cast<size_t>(host_image.rows) * 4U);
        cv::Mat contiguous_host(host_image.rows, host_image.cols, CV_32FC4, host_pixels->data(),
                                row_bytes);
        host_image.copyTo(contiguous_host);

        const auto t0 = ProfileClock::now();
        frame_sink_->SubmitHostFrame(
            ViewerFrame{host_image.cols, host_image.rows, row_bytes,
                        std::shared_ptr<const void>(host_pixels, host_pixels->data()),
                        display_config, FramePresentationMode::FullFrame});
        submit_ms = std::chrono::duration<double, std::milli>(ProfileClock::now() - t0).count();
      } else {
        submit_ms = 0.0;
      }
      SubmitOpenClFrameForScope(*detail_src, *frame_sink_, display_config);
    }

    if (output_img) {
      *output_img = ImageBuffer(std::move(*detail_src));
    }

    const double total_ms =
        std::chrono::duration<double, std::milli>(ProfileClock::now() - exec_start).count();

    static int           frame_count  = 0;
    static constexpr int kLogInterval = 30;
    if (++frame_count % kLogInterval == 1) {
      std::cout << "[OpenCL Pipeline] frame=" << frame_count << " total=" << std::fixed
                << std::setprecision(2) << total_ms << " ms"
                << " | input=" << ensure_input_ms << " abi=" << validate_abi_ms
                << " kernels=" << ensure_kernels_ms << " fused=" << fused_kernel_ms
                << " detail=" << detail_ms << " sync=" << sync_ms << " download=" << download_ms
                << " submit=" << submit_ms
                << " present=" << (submitted_gpu_frame ? "direct_opengl" : "host_upload")
                << " | size=" << input.Width() << "x" << input.Height() << std::endl;
    }
  }

  void ReleaseScratchBuffers() override {
    working_.Release();
    pre_hs_working_.Release();
    hs_working_.Release();
    blur_horizontal_.Release();
    detail_scratch_.Release();
  }

  void ReleaseResources() override {
    if (fused_kernel_ != nullptr) {
      clReleaseKernel(fused_kernel_);
      fused_kernel_ = nullptr;
    }
    if (fused_stage_kernel_ != nullptr) {
      clReleaseKernel(fused_stage_kernel_);
      fused_stage_kernel_ = nullptr;
    }
    if (validate_kernel_ != nullptr) {
      clReleaseKernel(validate_kernel_);
      validate_kernel_ = nullptr;
    }
    if (blur_h_kernel_ != nullptr) {
      clReleaseKernel(blur_h_kernel_);
      blur_h_kernel_ = nullptr;
    }
    if (apply_v_kernel_ != nullptr) {
      clReleaseKernel(apply_v_kernel_);
      apply_v_kernel_ = nullptr;
    }
    if (hs_extract_kernel_ != nullptr) {
      clReleaseKernel(hs_extract_kernel_);
      hs_extract_kernel_ = nullptr;
    }
    if (hs_extract_resampled_kernel_ != nullptr) {
      clReleaseKernel(hs_extract_resampled_kernel_);
      hs_extract_resampled_kernel_ = nullptr;
    }
    if (hs_build_remapped_sample_kernel_ != nullptr) {
      clReleaseKernel(hs_build_remapped_sample_kernel_);
      hs_build_remapped_sample_kernel_ = nullptr;
    }
    if (hs_pyr_down_kernel_ != nullptr) {
      clReleaseKernel(hs_pyr_down_kernel_);
      hs_pyr_down_kernel_ = nullptr;
    }
    if (hs_select_interpolated_level_kernel_ != nullptr) {
      clReleaseKernel(hs_select_interpolated_level_kernel_);
      hs_select_interpolated_level_kernel_ = nullptr;
    }
    if (hs_collapse_level_kernel_ != nullptr) {
      clReleaseKernel(hs_collapse_level_kernel_);
      hs_collapse_level_kernel_ = nullptr;
    }
    if (hs_apply_adjusted_l_kernel_ != nullptr) {
      clReleaseKernel(hs_apply_adjusted_l_kernel_);
      hs_apply_adjusted_l_kernel_ = nullptr;
    }
    if (hs_apply_adjusted_l_from_frame_kernel_ != nullptr) {
      clReleaseKernel(hs_apply_adjusted_l_from_frame_kernel_);
      hs_apply_adjusted_l_from_frame_kernel_ = nullptr;
    }
    if (hs_apply_adjusted_l_from_reference_kernel_ != nullptr) {
      clReleaseKernel(hs_apply_adjusted_l_from_reference_kernel_);
      hs_apply_adjusted_l_from_reference_kernel_ = nullptr;
    }
    ReleaseScratchBuffers();
    ReleaseHsBaseBuffers();
    resources_.Reset();
  }
};

}  // namespace

auto CreateOpenCLGPUPipeline() -> std::unique_ptr<GPUPipelineImpl> {
  return std::make_unique<OpenCLGPUPipeline>();
}

}  // namespace alcedo

#endif
