//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <vector>

namespace alcedo::ui {

// ============================================================================
//  Image analysis scopes for UI display in the editing viewport.
//  Produces histogram arrays or RGBA bitmaps for downstream rendering.
//  Standalone C++17 – no Qt or GPU dependency.
// ============================================================================

// ---------------------------------------------------------------------------
//  Enumerations
// ---------------------------------------------------------------------------

enum class HistogramChannel : uint32_t {
  RGB  = 0,
  Luma = 1,
};

enum class HistogramScale : uint32_t {
  Linear      = 0,
  Logarithmic = 1,
};

enum class ItuStandard : uint32_t {
  BT_601  = 0,  // Y = 0.299*R + 0.587*G + 0.114*B
  BT_709  = 1,  // Y = 0.2126*R + 0.7152*G + 0.0722*B
  BT_2020 = 2,  // Y = 0.2627*R + 0.6780*G + 0.0593*B
};

// ---------------------------------------------------------------------------
//  Histogram
// ---------------------------------------------------------------------------

struct HistogramOptions {
  int              bins        = 256;
  HistogramChannel channel     = HistogramChannel::RGB;
  bool             cumulative  = false;
  HistogramScale   scale       = HistogramScale::Linear;
  float            domain_min  = 0.0f;
  float            domain_max  = 1.0f;
};

struct HistogramResult {
  int                bins  = 0;
  std::vector<float> r     = {};
  std::vector<float> g     = {};
  std::vector<float> b     = {};
  std::vector<float> luma  = {};
  bool               valid = false;
};

// ---------------------------------------------------------------------------
//  Waveform (luma)
// ---------------------------------------------------------------------------

struct WaveformOptions {
  int         width     = 384;
  int         height    = 192;
  ItuStandard itu_std   = ItuStandard::BT_709;
  float       intensity = 1.0f;
};

struct WaveformResult {
  int                width  = 0;
  int                height = 0;
  std::vector<float> rgba   = {};
  bool               valid  = false;
};

// ---------------------------------------------------------------------------
//  Parade RGB (separate R / G / B channels vertical parade)
// ---------------------------------------------------------------------------

struct ParadeOptions {
  int   width     = 384;
  int   height    = 192;
  float intensity = 1.0f;
};

struct ParadeResult {
  int                width  = 0;
  int                height = 0;
  std::vector<float> rgba   = {};
  bool               valid  = false;
};

// ---------------------------------------------------------------------------
//  Vectorscope (chrominance plot)
// ---------------------------------------------------------------------------

struct VectorscopeOptions {
  int   size      = 256;
  float intensity = 1.0f;
  float gain      = 1.0f;
};

struct VectorscopeResult {
  int                size  = 0;
  std::vector<float> rgba  = {};
  bool               valid = false;
};

// ---------------------------------------------------------------------------
//  False-color clipping display
// ---------------------------------------------------------------------------

struct FalseColorOptions {
  float shadow_limit   = 0.05f;   // underexposed: blue
  float black_limit    = 0.10f;   // deep shadow: indigo
  float mid_low_limit  = 0.40f;   // low mid: green
  float mid_high_limit = 0.60f;   // high mid: gray
  float white_limit    = 0.95f;   // near-clip: pink
  // above white_limit: overexposed: red
};

struct FalseColorResult {
  int                width  = 0;
  int                height = 0;
  std::vector<float> rgba   = {};
  bool               valid  = false;
};

// ---------------------------------------------------------------------------
//  ScopeEngine – standalone computation entry point
// ---------------------------------------------------------------------------

class ScopeEngine {
 public:
  ScopeEngine()  = default;
  ~ScopeEngine() = default;

  /// Compute per-channel (and/or luma) histogram from interleaved RGBA float
  /// data. The input `rgba` has `width*height*4` floats in R-G-B-A order.
  static auto ComputeHistogram(const float* rgba, int width, int height,
                               const HistogramOptions& opts) -> HistogramResult;

  /// Compute luma waveform from interleaved RGBA float data.
  static auto ComputeWaveform(const float* rgba, int width, int height,
                              const WaveformOptions& opts) -> WaveformResult;

  /// Compute three-channel RGB parade from interleaved RGBA float data.
  static auto ComputeParade(const float* rgba, int width, int height,
                            const ParadeOptions& opts) -> ParadeResult;

  /// Compute vectorscope (chrominance scatter) from interleaved RGBA float data.
  static auto ComputeVectorscope(const float* rgba, int width, int height,
                                 const VectorscopeOptions& opts) -> VectorscopeResult;

  /// Compute false-color overlay from interleaved RGBA float data.
  static auto ComputeFalseColor(const float* rgba, int width, int height,
                                const FalseColorOptions& opts) -> FalseColorResult;

 private:
  static auto Luma(float r, float g, float b, ItuStandard std) -> float;
  static auto ApplyScale(float value, HistogramScale scale) -> float;
};

}  // namespace alcedo::ui