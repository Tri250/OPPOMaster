//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ui/scopes.hpp"

#include <algorithm>
#include <cmath>
#include <cstring>

namespace alcedo::ui {
namespace {

// ---------------------------------------------------------------------------
//  Luma coefficients (ITU-R)
// ---------------------------------------------------------------------------

struct LumaCoeff {
  float cr;
  float cg;
  float cb;
};

auto LumaCoefficients(ItuStandard std) -> LumaCoeff {
  switch (std) {
    case ItuStandard::BT_601:
      return {0.299f, 0.587f, 0.114f};
    case ItuStandard::BT_709:
      return {0.2126f, 0.7152f, 0.0722f};
    case ItuStandard::BT_2020:
      return {0.2627f, 0.6780f, 0.0593f};
  }
  return {0.2126f, 0.7152f, 0.0722f};
}

// ---------------------------------------------------------------------------
//  Fast R/G/B luminance for false-color (neutral Y = 0.3333R + 0.3333G + 0.3333B)
// ---------------------------------------------------------------------------

auto FastLuma(float r, float g, float b) -> float {
  return 0.3333333f * r + 0.3333333f * g + 0.3333333f * b;
}

// ---------------------------------------------------------------------------
//  Normalise a buffer in-place so its max value becomes 1.0.
//  Returns the number of non-zero entries (for diagnostics).
// ---------------------------------------------------------------------------

auto NormaliseMax(std::vector<float>& buf) -> size_t {
  const auto it = std::max_element(buf.begin(), buf.end());
  const float m  = *it;
  if (m > 0.0f) {
    const float inv = 1.0f / m;
    for (auto& v : buf) {
      v *= inv;
    }
  }
  return static_cast<size_t>(std::count_if(buf.begin(), buf.end(),
                                           [](float v) { return v > 0.0f; }));
}

// ---------------------------------------------------------------------------
//  Clamp a value to [lo, hi] and map to [0, bins-1].
// ---------------------------------------------------------------------------

auto MapToBin(float value, float lo, float hi, int bins) -> int {
  const float t = std::clamp((value - lo) / (hi - lo), 0.0f, 1.0f);
  return std::clamp(static_cast<int>(t * static_cast<float>(bins - 1)), 0, bins - 1);
}

// ---------------------------------------------------------------------------
//  RGBA pixel write helper
// ---------------------------------------------------------------------------

void WritePixel(std::vector<float>& rgba, size_t offset, float r, float g, float b,
                float a) {
  rgba[offset + 0] = r;
  rgba[offset + 1] = g;
  rgba[offset + 2] = b;
  rgba[offset + 3] = a;
}

}  // namespace

// ============================================================================
//  ScopeEngine::Luma
// ============================================================================

auto ScopeEngine::Luma(float r, float g, float b, ItuStandard std) -> float {
  const auto c = LumaCoefficients(std);
  return c.cr * r + c.cg * g + c.cb * b;
}

// ============================================================================
//  ScopeEngine::ApplyScale
// ============================================================================

auto ScopeEngine::ApplyScale(float value, HistogramScale scale) -> float {
  if (scale == HistogramScale::Logarithmic) {
    return std::log1pf(value);
  }
  return value;
}

// ============================================================================
//  ComputeHistogram
// ============================================================================

auto ScopeEngine::ComputeHistogram(const float* rgba, int width, int height,
                                   const HistogramOptions& opts) -> HistogramResult {
  HistogramResult result;
  if (rgba == nullptr || width <= 0 || height <= 0 || opts.bins <= 0) {
    return result;
  }

  result.bins = opts.bins;
  const bool do_rgb  = (opts.channel == HistogramChannel::RGB);
  const bool do_luma = (opts.channel == HistogramChannel::Luma) || do_rgb;

  if (do_rgb) {
    result.r.resize(static_cast<size_t>(opts.bins), 0.0f);
    result.g.resize(static_cast<size_t>(opts.bins), 0.0f);
    result.b.resize(static_cast<size_t>(opts.bins), 0.0f);
  }
  if (do_luma) {
    result.luma.resize(static_cast<size_t>(opts.bins), 0.0f);
  }

  const float lo = opts.domain_min;
  const float hi = opts.domain_max;
  const int   nb = opts.bins;
  const size_t np = static_cast<size_t>(width) * static_cast<size_t>(height);

  for (size_t i = 0; i < np; ++i) {
    const size_t off = i * 4U;
    const float  r   = rgba[off + 0];
    const float  g   = rgba[off + 1];
    const float  b   = rgba[off + 2];

    if (do_rgb) {
      result.r[static_cast<size_t>(MapToBin(r, lo, hi, nb))] += 1.0f;
      result.g[static_cast<size_t>(MapToBin(g, lo, hi, nb))] += 1.0f;
      result.b[static_cast<size_t>(MapToBin(b, lo, hi, nb))] += 1.0f;
    }
    if (do_luma) {
      const float y = Luma(r, g, b, ItuStandard::BT_709);
      result.luma[static_cast<size_t>(MapToBin(y, lo, hi, nb))] += 1.0f;
    }
  }

  // Cumulative
  if (opts.cumulative) {
    auto cumulate = [](std::vector<float>& vec) {
      for (size_t i = 1; i < vec.size(); ++i) {
        vec[i] += vec[i - 1];
      }
    };
    if (do_rgb) {
      cumulate(result.r);
      cumulate(result.g);
      cumulate(result.b);
    }
    if (do_luma) {
      cumulate(result.luma);
    }
  }

  // Scale
  if (opts.scale == HistogramScale::Logarithmic) {
    auto log_scale = [](std::vector<float>& vec) {
      for (auto& v : vec) {
        v = std::log1pf(v);
      }
    };
    if (do_rgb) {
      log_scale(result.r);
      log_scale(result.g);
      log_scale(result.b);
    }
    if (do_luma) {
      log_scale(result.luma);
    }
  }

  // Normalise each channel independently
  if (do_rgb) {
    NormaliseMax(result.r);
    NormaliseMax(result.g);
    NormaliseMax(result.b);
  }
  if (do_luma) {
    NormaliseMax(result.luma);
  }

  result.valid = true;
  return result;
}

// ============================================================================
//  ComputeWaveform
// ============================================================================

auto ScopeEngine::ComputeWaveform(const float* rgba, int width, int height,
                                  const WaveformOptions& opts) -> WaveformResult {
  WaveformResult result;
  if (rgba == nullptr || width <= 0 || height <= 0 || opts.width <= 0 ||
      opts.height <= 0) {
    return result;
  }

  result.width  = opts.width;
  result.height = opts.height;
  const size_t total = static_cast<size_t>(opts.width) * static_cast<size_t>(opts.height);
  result.rgba.resize(total * 4U, 0.0f);

  // Accumulation buffer (single-channel counts)
  std::vector<float> accum(total, 0.0f);

  const float x_scale = static_cast<float>(opts.width) / static_cast<float>(width);
  const float y_scale = static_cast<float>(opts.height - 1);
  const auto  coeff   = LumaCoefficients(opts.itu_std);

  for (int y = 0; y < height; ++y) {
    const size_t row_off = static_cast<size_t>(y) * static_cast<size_t>(width) * 4U;
    for (int x = 0; x < width; ++x) {
      const size_t px = row_off + static_cast<size_t>(x) * 4U;
      const float  r  = rgba[px + 0];
      const float  g  = rgba[px + 1];
      const float  b  = rgba[px + 2];

      const float luma = std::clamp(coeff.cr * r + coeff.cg * g + coeff.cb * b, 0.0f, 1.0f);

      const int wx = static_cast<int>(static_cast<float>(x) * x_scale);
      const int wy = static_cast<int>((1.0f - luma) * y_scale);  // top = bright

      if (wx >= 0 && wx < opts.width && wy >= 0 && wy < opts.height) {
        accum[static_cast<size_t>(wy) * static_cast<size_t>(opts.width) +
              static_cast<size_t>(wx)] += 1.0f;
      }
    }
  }

  // Normalise and convert to RGBA
  NormaliseMax(accum);

  const float intensity = opts.intensity;
  for (size_t i = 0; i < total; ++i) {
    const float v = std::min(accum[i] * intensity, 1.0f);
    WritePixel(result.rgba, i * 4U, v, v, v, v);
  }

  result.valid = true;
  return result;
}

// ============================================================================
//  ComputeParade
// ============================================================================

auto ScopeEngine::ComputeParade(const float* rgba, int width, int height,
                                const ParadeOptions& opts) -> ParadeResult {
  ParadeResult result;
  if (rgba == nullptr || width <= 0 || height <= 0 || opts.width <= 0 ||
      opts.height <= 0) {
    return result;
  }

  result.width  = opts.width;
  result.height = opts.height;
  const size_t total = static_cast<size_t>(opts.width) * static_cast<size_t>(opts.height);
  result.rgba.resize(total * 4U, 0.0f);

  // Each channel strip is 1/3 of the total width
  const int strip_w = opts.width / 3;
  if (strip_w <= 0) {
    return result;
  }

  // Three accumulation buffers, one per channel
  const size_t strip_total = static_cast<size_t>(strip_w) * static_cast<size_t>(opts.height);
  std::vector<float> accum_r(strip_total, 0.0f);
  std::vector<float> accum_g(strip_total, 0.0f);
  std::vector<float> accum_b(strip_total, 0.0f);

  const float x_scale = static_cast<float>(strip_w) / static_cast<float>(width);
  const float y_scale = static_cast<float>(opts.height - 1);

  for (int y = 0; y < height; ++y) {
    const size_t row_off = static_cast<size_t>(y) * static_cast<size_t>(width) * 4U;
    for (int x = 0; x < width; ++x) {
      const size_t px = row_off + static_cast<size_t>(x) * 4U;
      const float  r  = std::clamp(rgba[px + 0], 0.0f, 1.0f);
      const float  g  = std::clamp(rgba[px + 1], 0.0f, 1.0f);
      const float  b  = std::clamp(rgba[px + 2], 0.0f, 1.0f);

      const int sx = static_cast<int>(static_cast<float>(x) * x_scale);
      const int ry = static_cast<int>((1.0f - r) * y_scale);
      const int gy = static_cast<int>((1.0f - g) * y_scale);
      const int by = static_cast<int>((1.0f - b) * y_scale);

      if (sx >= 0 && sx < strip_w) {
        const size_t idx = static_cast<size_t>(sx);
        if (ry >= 0 && ry < opts.height) {
          accum_r[static_cast<size_t>(ry) * static_cast<size_t>(strip_w) + idx] += 1.0f;
        }
        if (gy >= 0 && gy < opts.height) {
          accum_g[static_cast<size_t>(gy) * static_cast<size_t>(strip_w) + idx] += 1.0f;
        }
        if (by >= 0 && by < opts.height) {
          accum_b[static_cast<size_t>(by) * static_cast<size_t>(strip_w) + idx] += 1.0f;
        }
      }
    }
  }

  NormaliseMax(accum_r);
  NormaliseMax(accum_g);
  NormaliseMax(accum_b);

  const float intensity = opts.intensity;
  for (int ch = 0; ch < 3; ++ch) {
    const std::vector<float>& src = (ch == 0) ? accum_r : (ch == 1) ? accum_g : accum_b;
    const int                  x0 = ch * strip_w;

    for (int sy = 0; sy < opts.height; ++sy) {
      for (int sx = 0; sx < strip_w; ++sx) {
        const size_t src_idx =
            static_cast<size_t>(sy) * static_cast<size_t>(strip_w) + static_cast<size_t>(sx);
        const float v = std::min(src[src_idx] * intensity, 1.0f);
        const size_t dst_idx =
            (static_cast<size_t>(sy) * static_cast<size_t>(opts.width) +
             static_cast<size_t>(x0 + sx)) *
            4U;

        float r_out = 0.0f, g_out = 0.0f, b_out = 0.0f;
        if (ch == 0) {
          r_out = v;
        } else if (ch == 1) {
          g_out = v;
        } else {
          b_out = v;
        }
        WritePixel(result.rgba, dst_idx, r_out, g_out, b_out, v);
      }
    }
  }

  result.valid = true;
  return result;
}

// ============================================================================
//  ComputeVectorscope
// ============================================================================

auto ScopeEngine::ComputeVectorscope(const float* rgba, int width, int height,
                                     const VectorscopeOptions& opts) -> VectorscopeResult {
  VectorscopeResult result;
  if (rgba == nullptr || width <= 0 || height <= 0 || opts.size <= 0) {
    return result;
  }

  result.size = opts.size;
  const size_t total = static_cast<size_t>(opts.size) * static_cast<size_t>(opts.size);
  result.rgba.resize(total * 4U, 0.0f);

  // Accumulation buffer (single-channel counts)
  std::vector<float> accum(total, 0.0f);

  const float half  = static_cast<float>(opts.size) * 0.5f;
  const float gain  = opts.gain;
  const size_t np   = static_cast<size_t>(width) * static_cast<size_t>(height);

  // YCbCr conversion coefficients (BT.709)
  // Cb = -0.168736*R - 0.331264*G + 0.5*B
  // Cr =  0.5*R      - 0.418688*G - 0.081312*B
  // Normalised to nominal [-0.5, 0.5]

  for (size_t i = 0; i < np; ++i) {
    const size_t off = i * 4U;
    const float  r   = rgba[off + 0];
    const float  g   = rgba[off + 1];
    const float  b   = rgba[off + 2];

    // Only plot pixels with meaningful luminance
    const float luma = FastLuma(r, g, b);
    if (luma <= 0.0f || luma >= 1.0f) {
      continue;
    }

    const float cb = (-0.168736f * r - 0.331264f * g + 0.5f * b) * gain;
    const float cr = (0.5f * r - 0.418688f * g - 0.081312f * b) * gain;

    // Map to coordinates: centre = (0,0), cb = x, cr = y
    const int px = static_cast<int>(half + cb * half);
    const int py = static_cast<int>(half - cr * half);  // flip y so +Cr is up

    if (px >= 0 && px < opts.size && py >= 0 && py < opts.size) {
      accum[static_cast<size_t>(py) * static_cast<size_t>(opts.size) +
            static_cast<size_t>(px)] += 1.0f;
    }
  }

  NormaliseMax(accum);

  const float intensity = opts.intensity;
  // Create a coloured vectorscope: hue from angle, saturation from radius
  for (int y = 0; y < opts.size; ++y) {
    for (int x = 0; x < opts.size; ++x) {
      const size_t idx =
          static_cast<size_t>(y) * static_cast<size_t>(opts.size) + static_cast<size_t>(x);
      const float v = std::min(accum[idx] * intensity, 1.0f);

      const float dx = static_cast<float>(x) - half;
      const float dy = static_cast<float>(y) - half;

      // Base colour: white for the trace
      float r_out = v;
      float g_out = v;
      float b_out = v;

      // Tint by position (hue from angle)
      if (v > 0.001f) {
        const float ang = std::atan2(dy, dx);
        // Map angle to RGB: R=0°, G=120°, B=240°
        float phase = (ang / 3.14159265358979323846f + 1.0f) * 0.5f;  // [0, 1]
        // Simple RGB from hue
        r_out = v * (0.5f + 0.5f * std::cos(phase * 6.2831853f));
        g_out = v * (0.5f + 0.5f * std::cos((phase - 1.0f / 3.0f) * 6.2831853f));
        b_out = v * (0.5f + 0.5f * std::cos((phase - 2.0f / 3.0f) * 6.2831853f));
      }

      WritePixel(result.rgba, idx * 4U, r_out, g_out, b_out, v);
    }
  }

  result.valid = true;
  return result;
}

// ============================================================================
//  ComputeFalseColor
// ============================================================================

auto ScopeEngine::ComputeFalseColor(const float* rgba, int width, int height,
                                    const FalseColorOptions& opts) -> FalseColorResult {
  FalseColorResult result;
  if (rgba == nullptr || width <= 0 || height <= 0) {
    return result;
  }

  result.width  = width;
  result.height = height;
  const size_t total = static_cast<size_t>(width) * static_cast<size_t>(height);
  result.rgba.resize(total * 4U, 0.0f);

  // Standard false-colour palette (Resolve / monitor convention)
  //   [0.00, shadow_limit)  → blue    (underexposed / noise floor)
  //   [shadow_limit, black) → indigo  (deep shadow)
  //   [black, mid_low)      → green   (shadow)
  //   [mid_low, mid_high)   → gray    (midtones — neutral exposure)
  //   [mid_high, white)     → pink    (highlight)
  //   [white, 1.0+]         → red     (overexposed / clipping)

  // Pre-computed colour table for the six bands
  struct FC {
    float r;
    float g;
    float b;
  };
  const FC colors[6] = {
      {0.0f, 0.0f, 1.0f},     // blue   – underexposed
      {0.29f, 0.0f, 0.51f},   // indigo – deep shadow
      {0.0f, 0.8f, 0.0f},     // green  – shadow
      {0.5f, 0.5f, 0.5f},     // gray   – midtones
      {1.0f, 0.41f, 0.71f},   // pink   – highlight
      {1.0f, 0.0f, 0.0f},     // red    – overexposed
  };

  const float limits[5] = {
      opts.shadow_limit,   // 0 → 1
      opts.black_limit,    // 1 → 2
      opts.mid_low_limit,  // 2 → 3
      opts.mid_high_limit, // 3 → 4
      opts.white_limit,    // 4 → 5
  };

  for (size_t i = 0; i < total; ++i) {
    const size_t off = i * 4U;
    const float  r   = rgba[off + 0];
    const float  g   = rgba[off + 1];
    const float  b   = rgba[off + 2];
    const float  a   = rgba[off + 3];

    const float luma = FastLuma(r, g, b);

    int band = 0;
    for (int k = 0; k < 5; ++k) {
      if (luma >= limits[k]) {
        band = k + 1;
      }
    }

    const FC& c = colors[band];
    WritePixel(result.rgba, off, c.r, c.g, c.b, a);
  }

  result.valid = true;
  return result;
}

}  // namespace alcedo::ui