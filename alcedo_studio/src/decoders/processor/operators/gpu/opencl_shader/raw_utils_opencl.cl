//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

// Shared RAW processing utilities for OpenCL kernels.
// Keep this file dependency-free so it can be concatenated with any
// operator-specific .cl source.

typedef struct {
  float black_level[4];
  float white_level[4];
  float wb_multipliers[4];
  int   apply_white_balance;
  int   black_tile_width;
  int   black_tile_height;
  float pattern_black[36];
} WBParams;

typedef struct {
  int width;
  int height;
  int tile_width;
  int tile_height;
  int raw_fc[36];
} PatternParams;

// ---------------------------------------------------------------------------
// Bayer / X-Trans CFA indexing
// ---------------------------------------------------------------------------

inline int BayerCellIndex(int y, int x) {
  return ((y & 1) << 1) | (x & 1);
}

inline int WrapPatternCoord(int coord, int period) {
  int wrapped = coord % period;
  return wrapped < 0 ? wrapped + period : wrapped;
}

inline int XTransCellIndex(int y, int x) {
  return WrapPatternCoord(y, 6) * 6 + WrapPatternCoord(x, 6);
}

inline int RawColorAt(PatternParams params, int y, int x) {
  if (params.tile_width == 6) {
    return params.raw_fc[XTransCellIndex(y, x)];
  }
  return params.raw_fc[BayerCellIndex(y, x)];
}

// ---------------------------------------------------------------------------
// Black-level helpers
// ---------------------------------------------------------------------------

inline float PatternBlackAt(WBParams wb, int y, int x) {
  if (wb.black_tile_width <= 0 || wb.black_tile_height <= 0) {
    return 0.0f;
  }
  int ty = ((y % wb.black_tile_height) + wb.black_tile_height) % wb.black_tile_height;
  int tx = ((x % wb.black_tile_width) + wb.black_tile_width) % wb.black_tile_width;
  return wb.pattern_black[ty * wb.black_tile_width + tx];
}

// ---------------------------------------------------------------------------
// Math helpers (match cpu/cuda raw_normalization.hpp semantics)
// ---------------------------------------------------------------------------

inline float Clamp01(float v) {
  return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
}

inline float NormalizeSample(float sample, float black, float white) {
  float denom = white - black;
  if (denom <= 0.0f) {
    return 0.0f;
  }
  return Clamp01((sample - black) / denom);
}

inline float RelativeWBMultiplier(WBParams wb, int color_idx) {
  if (!wb.apply_white_balance) {
    return 1.0f;
  }
  float green = wb.wb_multipliers[1];
  if (green <= 0.0f) {
    return 1.0f;
  }
  if (color_idx == 0 || color_idx == 2) {
    return wb.wb_multipliers[color_idx] / green;
  }
  return 1.0f;
}
