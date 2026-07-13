//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "processing/creative_effects.hpp"

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <limits>
#include <numeric>
#include <stdexcept>
#include <vector>

namespace alcedo {
namespace effects {

// ==========================================================================
//  Anonymous namespace: internal helpers
// ==========================================================================

namespace {

/// Clamp a value to [lo, hi].
inline float clamp(float v, float lo, float hi) {
  return v < lo ? lo : (v > hi ? hi : v);
}

/// Linear interpolation.
inline float lerp(float a, float b, float t) {
  return a + (b - a) * t;
}

/// Smoothstep: 3t^2 - 2t^3.
inline float smoothstep(float t) {
  float t2 = t * t;
  return t2 * (3.0f - 2.0f * t);
}

/// Compute 1D Gaussian kernel.
std::vector<float> make_gaussian_kernel(float sigma) {
  int radius = static_cast<int>(std::ceil(sigma * 3.0f));
  if (radius < 1) radius = 1;
  int size = 2 * radius + 1;
  std::vector<float> kernel(size);
  float sum = 0.0f;
  float two_sigma2 = 2.0f * sigma * sigma;
  for (int i = -radius; i <= radius; ++i) {
    float val = std::exp(-static_cast<float>(i * i) / two_sigma2);
    kernel[i + radius] = val;
    sum += val;
  }
  for (int i = 0; i < size; ++i) {
    kernel[i] /= sum;
  }
  return kernel;
}

/// Horizontal separable blur pass.
void blur_horizontal(const std::vector<float>& src,
                     std::vector<float>& dst,
                     int width,
                     int height,
                     const std::vector<float>& kernel,
                     int radius) {
  dst.resize(src.size());
  for (int y = 0; y < height; ++y) {
    int row_base = y * width;
    for (int x = 0; x < width; ++x) {
      float sum = 0.0f;
      for (int k = -radius; k <= radius; ++k) {
        int sx = x + k;
        if (sx < 0) sx = 0;
        if (sx >= width) sx = width - 1;
        sum += src[row_base + sx] * kernel[k + radius];
      }
      dst[row_base + x] = sum;
    }
  }
}

/// Vertical separable blur pass.
void blur_vertical(const std::vector<float>& src,
                   std::vector<float>& dst,
                   int width,
                   int height,
                   const std::vector<float>& kernel,
                   int radius) {
  dst.resize(src.size());
  for (int y = 0; y < height; ++y) {
    int row_base = y * width;
    for (int x = 0; x < width; ++x) {
      float sum = 0.0f;
      for (int k = -radius; k <= radius; ++k) {
        int sy = y + k;
        if (sy < 0) sy = 0;
        if (sy >= height) sy = height - 1;
        sum += src[sy * width + x] * kernel[k + radius];
      }
      dst[row_base + x] = sum;
    }
  }
}

/// Box blur (used internally for guided filter box mean).
void box_mean(const std::vector<float>& src,
              std::vector<float>& dst,
              int width,
              int height,
              int radius) {
  dst.resize(src.size());
  // Horizontal pass
  std::vector<float> tmp(src.size());
  for (int y = 0; y < height; ++y) {
    int row_base = y * width;
    float running = 0.0f;
    int count = 0;
    int window = 2 * radius + 1;
    for (int x = 0; x < width; ++x) {
      int left = x - radius;
      int right = x + radius;
      if (left < 0) left = 0;
      if (right >= width) right = width - 1;
      if (x == 0) {
        running = 0.0f;
        count = 0;
        for (int i = left; i <= right; ++i) {
          running += src[row_base + i];
          ++count;
        }
      } else {
        // sliding window
        int old_left = x - 1 - radius;
        int new_right = x + radius;
        if (old_left >= 0) {
          running -= src[row_base + old_left];
          --count;
        }
        if (new_right < width) {
          running += src[row_base + new_right];
          ++count;
        }
      }
      tmp[row_base + x] = running / static_cast<float>(count);
    }
  }
  // Vertical pass
  for (int x = 0; x < width; ++x) {
    float running = 0.0f;
    int count = 0;
    for (int y = 0; y < height; ++y) {
      int top = y - radius;
      int bottom = y + radius;
      if (top < 0) top = 0;
      if (bottom >= height) bottom = height - 1;
      if (y == 0) {
        running = 0.0f;
        count = 0;
        for (int i = top; i <= bottom; ++i) {
          running += tmp[i * width + x];
          ++count;
        }
      } else {
        int old_top = y - 1 - radius;
        int new_bottom = y + radius;
        if (old_top >= 0) {
          running -= tmp[old_top * width + x];
          --count;
        }
        if (new_bottom < height) {
          running += tmp[new_bottom * width + x];
          ++count;
        }
      }
      dst[y * width + x] = running / static_cast<float>(count);
    }
  }
}

/// Bilinear sample from an RGBA image.
void sample_rgba_bilinear(const std::vector<float>& img,
                          int iw, int ih,
                          float u, float v,
                          float& r, float& g, float& b, float& a) {
  float fx = u * static_cast<float>(iw) - 0.5f;
  float fy = v * static_cast<float>(ih) - 0.5f;
  int x0 = static_cast<int>(std::floor(fx));
  int y0 = static_cast<int>(std::floor(fy));
  int x1 = x0 + 1;
  int y1 = y0 + 1;
  float tx = fx - static_cast<float>(x0);
  float ty = fy - static_cast<float>(y0);

  auto get = [&](int px, int py) {
    px = clamp(px, 0, iw - 1);
    py = clamp(py, 0, ih - 1);
    return py * iw * 4 + px * 4;
  };

  int idx00 = get(x0, y0);
  int idx10 = get(x1, y0);
  int idx01 = get(x0, y1);
  int idx11 = get(x1, y1);

  float r0 = lerp(img[idx00 + 0], img[idx10 + 0], tx);
  float r1 = lerp(img[idx01 + 0], img[idx11 + 0], tx);
  r = lerp(r0, r1, ty);

  float g0 = lerp(img[idx00 + 1], img[idx10 + 1], tx);
  float g1 = lerp(img[idx01 + 1], img[idx11 + 1], tx);
  g = lerp(g0, g1, ty);

  float b0 = lerp(img[idx00 + 2], img[idx10 + 2], tx);
  float b1 = lerp(img[idx01 + 2], img[idx11 + 2], tx);
  b = lerp(b0, b1, ty);

  float a0 = lerp(img[idx00 + 3], img[idx10 + 3], tx);
  float a1 = lerp(img[idx01 + 3], img[idx11 + 3], tx);
  a = lerp(a0, a1, ty);
}

/// Compute luminance from linear RGB.
inline float luminance(float r, float g, float b) {
  return 0.2126f * r + 0.7152f * g + 0.0722f * b;
}

/// Resolve WatermarkPosition to pixel coordinates.
void resolve_watermark_position(WatermarkPosition pos,
                                int img_w, int img_h,
                                int elem_w, int elem_h,
                                float custom_x, float custom_y,
                                float margin_x, float margin_y,
                                int& out_x, int& out_y) {
  int mx = static_cast<int>(margin_x * static_cast<float>(img_w));
  int my = static_cast<int>(margin_y * static_cast<float>(img_h));

  switch (pos) {
    case WatermarkPosition::TOP_LEFT:
      out_x = mx;
      out_y = my;
      break;
    case WatermarkPosition::TOP_CENTER:
      out_x = (img_w - elem_w) / 2;
      out_y = my;
      break;
    case WatermarkPosition::TOP_RIGHT:
      out_x = img_w - elem_w - mx;
      out_y = my;
      break;
    case WatermarkPosition::CENTER_LEFT:
      out_x = mx;
      out_y = (img_h - elem_h) / 2;
      break;
    case WatermarkPosition::CENTER:
      out_x = (img_w - elem_w) / 2;
      out_y = (img_h - elem_h) / 2;
      break;
    case WatermarkPosition::CENTER_RIGHT:
      out_x = img_w - elem_w - mx;
      out_y = (img_h - elem_h) / 2;
      break;
    case WatermarkPosition::BOTTOM_LEFT:
      out_x = mx;
      out_y = img_h - elem_h - my;
      break;
    case WatermarkPosition::BOTTOM_CENTER:
      out_x = (img_w - elem_w) / 2;
      out_y = img_h - elem_h - my;
      break;
    case WatermarkPosition::BOTTOM_RIGHT:
      out_x = img_w - elem_w - mx;
      out_y = img_h - elem_h - my;
      break;
    case WatermarkPosition::CUSTOM:
      out_x = static_cast<int>(custom_x * static_cast<float>(img_w));
      out_y = static_cast<int>(custom_y * static_cast<float>(img_h));
      break;
  }
}

/// Rotate a point around origin.
inline void rotate_point(float& x, float& y, float cx, float cy, float cos_a, float sin_a) {
  float dx = x - cx;
  float dy = y - cy;
  x = cx + dx * cos_a - dy * sin_a;
  y = cy + dx * sin_a + dy * cos_a;
}

/// Simple 5x7 bitmap font data (ASCII 32-126).
/// Each glyph is 5 columns wide, stored as 7 bytes (one per row, MSB is leftmost pixel).
/// Character cell is 6x9 pixels (1px padding on each side).
constexpr uint8_t FONT_GLYPHS[95][7] = {
  // space (32)
  {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
  // ! (33)
  {0x20, 0x20, 0x20, 0x20, 0x20, 0x00, 0x20},
  // " (34)
  {0x50, 0x50, 0x50, 0x00, 0x00, 0x00, 0x00},
  // # (35)
  {0x50, 0x50, 0xF8, 0x50, 0xF8, 0x50, 0x50},
  // $ (36)
  {0x20, 0x78, 0xA0, 0x70, 0x28, 0xF0, 0x20},
  // % (37)
  {0xC0, 0xC8, 0x10, 0x20, 0x40, 0x98, 0x18},
  // & (38)
  {0x40, 0xA0, 0xA0, 0x40, 0xA8, 0x90, 0x68},
  // ' (39)
  {0x20, 0x20, 0x40, 0x00, 0x00, 0x00, 0x00},
  // ( (40)
  {0x10, 0x20, 0x40, 0x40, 0x40, 0x20, 0x10},
  // ) (41)
  {0x40, 0x20, 0x10, 0x10, 0x10, 0x20, 0x40},
  // * (42)
  {0x00, 0x20, 0xA8, 0x70, 0xA8, 0x20, 0x00},
  // + (43)
  {0x00, 0x20, 0x20, 0xF8, 0x20, 0x20, 0x00},
  // , (44)
  {0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x40},
  // - (45)
  {0x00, 0x00, 0x00, 0xF8, 0x00, 0x00, 0x00},
  // . (46)
  {0x00, 0x00, 0x00, 0x00, 0x00, 0x40, 0x00},
  // / (47)
  {0x00, 0x08, 0x10, 0x20, 0x40, 0x80, 0x00},
  // 0 (48)
  {0x70, 0x88, 0x98, 0xA8, 0xC8, 0x88, 0x70},
  // 1 (49)
  {0x20, 0x60, 0x20, 0x20, 0x20, 0x20, 0x70},
  // 2 (50)
  {0x70, 0x88, 0x08, 0x10, 0x20, 0x40, 0xF8},
  // 3 (51)
  {0x70, 0x88, 0x08, 0x30, 0x08, 0x88, 0x70},
  // 4 (52)
  {0x10, 0x30, 0x50, 0x90, 0xF8, 0x10, 0x10},
  // 5 (53)
  {0xF8, 0x80, 0xF0, 0x08, 0x08, 0x88, 0x70},
  // 6 (54)
  {0x30, 0x40, 0x80, 0xF0, 0x88, 0x88, 0x70},
  // 7 (55)
  {0xF8, 0x08, 0x10, 0x20, 0x40, 0x40, 0x40},
  // 8 (56)
  {0x70, 0x88, 0x88, 0x70, 0x88, 0x88, 0x70},
  // 9 (57)
  {0x70, 0x88, 0x88, 0x78, 0x08, 0x10, 0x60},
  // : (58)
  {0x00, 0x00, 0x20, 0x00, 0x00, 0x20, 0x00},
  // ; (59)
  {0x00, 0x00, 0x20, 0x00, 0x00, 0x20, 0x40},
  // < (60)
  {0x10, 0x20, 0x40, 0x80, 0x40, 0x20, 0x10},
  // = (61)
  {0x00, 0x00, 0xF8, 0x00, 0xF8, 0x00, 0x00},
  // > (62)
  {0x40, 0x20, 0x10, 0x08, 0x10, 0x20, 0x40},
  // ? (63)
  {0x70, 0x88, 0x08, 0x10, 0x20, 0x00, 0x20},
  // @ (64)
  {0x70, 0x88, 0xB8, 0xA8, 0xB8, 0x80, 0x70},
  // A (65)
  {0x20, 0x50, 0x88, 0x88, 0xF8, 0x88, 0x88},
  // B (66)
  {0xF0, 0x88, 0x88, 0xF0, 0x88, 0x88, 0xF0},
  // C (67)
  {0x70, 0x88, 0x80, 0x80, 0x80, 0x88, 0x70},
  // D (68)
  {0xF0, 0x88, 0x88, 0x88, 0x88, 0x88, 0xF0},
  // E (69)
  {0xF8, 0x80, 0x80, 0xF0, 0x80, 0x80, 0xF8},
  // F (70)
  {0xF8, 0x80, 0x80, 0xF0, 0x80, 0x80, 0x80},
  // G (71)
  {0x70, 0x88, 0x80, 0xB8, 0x88, 0x88, 0x70},
  // H (72)
  {0x88, 0x88, 0x88, 0xF8, 0x88, 0x88, 0x88},
  // I (73)
  {0x70, 0x20, 0x20, 0x20, 0x20, 0x20, 0x70},
  // J (74)
  {0x38, 0x10, 0x10, 0x10, 0x10, 0x90, 0x60},
  // K (75)
  {0x88, 0x90, 0xA0, 0xC0, 0xA0, 0x90, 0x88},
  // L (76)
  {0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0xF8},
  // M (77)
  {0x88, 0xD8, 0xA8, 0xA8, 0x88, 0x88, 0x88},
  // N (78)
  {0x88, 0xC8, 0xA8, 0x98, 0x88, 0x88, 0x88},
  // O (79)
  {0x70, 0x88, 0x88, 0x88, 0x88, 0x88, 0x70},
  // P (80)
  {0xF0, 0x88, 0x88, 0xF0, 0x80, 0x80, 0x80},
  // Q (81)
  {0x70, 0x88, 0x88, 0x88, 0xA8, 0x90, 0x68},
  // R (82)
  {0xF0, 0x88, 0x88, 0xF0, 0xA0, 0x90, 0x88},
  // S (83)
  {0x70, 0x88, 0x80, 0x70, 0x08, 0x88, 0x70},
  // T (84)
  {0xF8, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20},
  // U (85)
  {0x88, 0x88, 0x88, 0x88, 0x88, 0x88, 0x70},
  // V (86)
  {0x88, 0x88, 0x88, 0x88, 0x50, 0x50, 0x20},
  // W (87)
  {0x88, 0x88, 0x88, 0xA8, 0xA8, 0xD8, 0x88},
  // X (88)
  {0x88, 0x88, 0x50, 0x20, 0x50, 0x88, 0x88},
  // Y (89)
  {0x88, 0x88, 0x50, 0x20, 0x20, 0x20, 0x20},
  // Z (90)
  {0xF8, 0x08, 0x10, 0x20, 0x40, 0x80, 0xF8},
  // [ (91)
  {0x70, 0x40, 0x40, 0x40, 0x40, 0x40, 0x70},
  // \ (92)
  {0x00, 0x80, 0x40, 0x20, 0x10, 0x08, 0x00},
  // ] (93)
  {0x70, 0x10, 0x10, 0x10, 0x10, 0x10, 0x70},
  // ^ (94)
  {0x20, 0x50, 0x88, 0x00, 0x00, 0x00, 0x00},
  // _ (95)
  {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xF8},
  // ` (96)
  {0x40, 0x20, 0x10, 0x00, 0x00, 0x00, 0x00},
  // a (97)
  {0x00, 0x00, 0x70, 0x08, 0x78, 0x88, 0x78},
  // b (98)
  {0x80, 0x80, 0xB0, 0xC8, 0x88, 0xC8, 0xB0},
  // c (99)
  {0x00, 0x00, 0x70, 0x88, 0x80, 0x88, 0x70},
  // d (100)
  {0x08, 0x08, 0x68, 0x98, 0x88, 0x98, 0x68},
  // e (101)
  {0x00, 0x00, 0x70, 0x88, 0xF8, 0x80, 0x70},
  // f (102)
  {0x30, 0x48, 0x40, 0xE0, 0x40, 0x40, 0x40},
  // g (103)
  {0x00, 0x00, 0x78, 0x88, 0x78, 0x08, 0x70},
  // h (104)
  {0x80, 0x80, 0xB0, 0xC8, 0x88, 0x88, 0x88},
  // i (105)
  {0x20, 0x00, 0x60, 0x20, 0x20, 0x20, 0x70},
  // j (106)
  {0x10, 0x00, 0x30, 0x10, 0x10, 0x90, 0x60},
  // k (107)
  {0x80, 0x80, 0x90, 0xA0, 0xC0, 0xA0, 0x90},
  // l (108)
  {0x60, 0x20, 0x20, 0x20, 0x20, 0x20, 0x70},
  // m (109)
  {0x00, 0x00, 0xD0, 0xA8, 0xA8, 0xA8, 0x88},
  // n (110)
  {0x00, 0x00, 0xB0, 0xC8, 0x88, 0x88, 0x88},
  // o (111)
  {0x00, 0x00, 0x70, 0x88, 0x88, 0x88, 0x70},
  // p (112)
  {0x00, 0x00, 0xB0, 0xC8, 0xC8, 0xB0, 0x80},
  // q (113)
  {0x00, 0x00, 0x68, 0x98, 0x98, 0x68, 0x08},
  // r (114)
  {0x00, 0x00, 0xB0, 0xC8, 0x80, 0x80, 0x80},
  // s (115)
  {0x00, 0x00, 0x78, 0x80, 0x70, 0x08, 0xF0},
  // t (116)
  {0x40, 0x40, 0xE0, 0x40, 0x40, 0x48, 0x30},
  // u (117)
  {0x00, 0x00, 0x88, 0x88, 0x88, 0x98, 0x68},
  // v (118)
  {0x00, 0x00, 0x88, 0x88, 0x50, 0x50, 0x20},
  // w (119)
  {0x00, 0x00, 0x88, 0x88, 0xA8, 0xA8, 0x50},
  // x (120)
  {0x00, 0x00, 0x88, 0x50, 0x20, 0x50, 0x88},
  // y (121)
  {0x00, 0x00, 0x88, 0x88, 0x78, 0x08, 0x70},
  // z (122)
  {0x00, 0x00, 0xF8, 0x10, 0x20, 0x40, 0xF8},
  // { (123)
  {0x10, 0x20, 0x20, 0xC0, 0x20, 0x20, 0x10},
  // | (124)
  {0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20},
  // } (125)
  {0x40, 0x20, 0x20, 0x18, 0x20, 0x20, 0x40},
  // ~ (126)
  {0x00, 0x00, 0x40, 0xA8, 0x10, 0x00, 0x00},
};

/// Render a single character glyph into an RGBA buffer at (ox, oy).
void render_glyph(char ch, std::vector<float>& buf, int buf_w, int buf_h,
                  int ox, int oy, float r, float g, float b) {
  if (ch < 32 || ch > 126) return;
  int idx = static_cast<int>(ch) - 32;
  const uint8_t* glyph = FONT_GLYPHS[idx];

  for (int row = 0; row < 7; ++row) {
    int py = oy + row + 1; // 1px padding top
    if (py < 0 || py >= buf_h) continue;
    uint8_t bits = glyph[row];
    for (int col = 0; col < 5; ++col) {
      int px = ox + col + 1; // 1px padding left
      if (px < 0 || px >= buf_w) continue;
      if (bits & (0x80 >> col)) {
        int pidx = py * buf_w * 4 + px * 4;
        buf[pidx + 0] = r;
        buf[pidx + 1] = g;
        buf[pidx + 2] = b;
        buf[pidx + 3] = 1.0f;
      }
    }
  }
}

}  // anonymous namespace

// ==========================================================================
//  Exposed helper implementations
// ==========================================================================

float blend_pixel(float base, float blend, BlendMode mode) {
  switch (mode) {
    case BlendMode::SCREEN:
      return 1.0f - (1.0f - base) * (1.0f - blend);
    case BlendMode::OVERLAY:
      if (base < 0.5f) {
        return 2.0f * base * blend;
      } else {
        return 1.0f - 2.0f * (1.0f - base) * (1.0f - blend);
      }
    case BlendMode::SOFT_LIGHT: {
      if (blend < 0.5f) {
        return base - (1.0f - 2.0f * blend) * base * (1.0f - base);
      } else {
        float d = (base < 0.25f)
                      ? ((16.0f * base - 12.0f) * base + 4.0f) * base
                      : std::sqrt(base);
        return base + (2.0f * blend - 1.0f) * (d - base);
      }
    }
    case BlendMode::MULTIPLY:
      return base * blend;
    case BlendMode::ADD:
      return clamp(base + blend, 0.0f, 1.0f);
    case BlendMode::NORMAL:
      return blend;
  }
  return blend;
}

void gaussian_blur(const std::vector<float>& input,
                   std::vector<float>& output,
                   int width, int height, float sigma) {
  auto kernel = make_gaussian_kernel(sigma);
  int radius = static_cast<int>(kernel.size() / 2);
  std::vector<float> tmp;
  blur_horizontal(input, tmp, width, height, kernel, radius);
  blur_vertical(tmp, output, width, height, kernel, radius);
}

void dark_channel(const std::vector<float>& input,
                  std::vector<float>& output,
                  int width, int height, int patch_size) {
  int half = patch_size / 2;
  int total = width * height;
  output.resize(total);

  for (int y = 0; y < height; ++y) {
    for (int x = 0; x < width; ++x) {
      float min_val = 1.0f;
      int y0 = std::max(0, y - half);
      int y1 = std::min(height - 1, y + half);
      int x0 = std::max(0, x - half);
      int x1 = std::min(width - 1, x + half);
      for (int py = y0; py <= y1; ++py) {
        int row_base = py * width * 3;
        for (int px = x0; px <= x1; ++px) {
          int idx = row_base + px * 3;
          float val = std::min({input[idx], input[idx + 1], input[idx + 2]});
          if (val < min_val) min_val = val;
        }
      }
      output[y * width + x] = min_val;
    }
  }
}

void guided_filter(const std::vector<float>& guide,
                   const std::vector<float>& input,
                   std::vector<float>& output,
                   int width, int height, int radius, float eps) {
  int total = width * height;

  // mean_I = box_mean(guide)
  std::vector<float> mean_I;
  box_mean(guide, mean_I, width, height, radius);

  // mean_p = box_mean(input)
  std::vector<float> mean_p;
  box_mean(input, mean_p, width, height, radius);

  // corr_I = box_mean(guide .* guide)
  std::vector<float> I2(total);
  for (int i = 0; i < total; ++i) I2[i] = guide[i] * guide[i];
  std::vector<float> corr_I;
  box_mean(I2, corr_I, width, height, radius);

  // corr_Ip = box_mean(guide .* input)
  std::vector<float> Ip(total);
  for (int i = 0; i < total; ++i) Ip[i] = guide[i] * input[i];
  std::vector<float> corr_Ip;
  box_mean(Ip, corr_Ip, width, height, radius);

  // var_I = corr_I - mean_I .* mean_I
  // cov_Ip = corr_Ip - mean_I .* mean_p
  std::vector<float> a(total), b(total);
  for (int i = 0; i < total; ++i) {
    float var_I = corr_I[i] - mean_I[i] * mean_I[i];
    float cov_Ip = corr_Ip[i] - mean_I[i] * mean_p[i];
    a[i] = cov_Ip / (var_I + eps);
    b[i] = mean_p[i] - a[i] * mean_I[i];
  }

  // mean_a, mean_b
  std::vector<float> mean_a, mean_b;
  box_mean(a, mean_a, width, height, radius);
  box_mean(b, mean_b, width, height, radius);

  output.resize(total);
  for (int i = 0; i < total; ++i) {
    output[i] = mean_a[i] * guide[i] + mean_b[i];
  }
}

// ==========================================================================
//  Section A: Glow effect
// ==========================================================================

void apply_glow(const std::vector<float>& input,
                std::vector<float>& output,
                int width, int height,
                const GlowParams& params) {
  int total = width * height;
  int channels = 3;
  output.resize(total * channels);

  std::vector<float> glow(total * channels, 0.0f);

  // Multi-scale blur
  float radius = params.radius;
  for (int s = 0; s < params.num_scales; ++s) {
    float weight = std::pow(0.5f, static_cast<float>(s));
    float sigma = radius / 3.0f; // approximate sigma from radius

    std::vector<float> blurred;
    // Blur each channel separately
    std::vector<float> channel_in(total);
    std::vector<float> channel_out(total);

    for (int c = 0; c < channels; ++c) {
      for (int i = 0; i < total; ++i) {
        channel_in[i] = input[i * channels + c];
      }
      gaussian_blur(channel_in, channel_out, width, height, sigma);
      for (int i = 0; i < total; ++i) {
        glow[i * channels + c] += channel_out[i] * weight;
      }
    }

    radius *= params.scale_factor;
  }

  // Normalize glow weights
  float weight_sum = 0.0f;
  for (int s = 0; s < params.num_scales; ++s) {
    weight_sum += std::pow(0.5f, static_cast<float>(s));
  }
  if (weight_sum > 0.0f) {
    for (int i = 0; i < total * channels; ++i) {
      glow[i] /= weight_sum;
    }
  }

  // Blend with original
  for (int i = 0; i < total; ++i) {
    float r = input[i * channels + 0];
    float g = input[i * channels + 1];
    float b = input[i * channels + 2];

    // Threshold mask
    float lum = luminance(r, g, b);
    float mask = 1.0f;
    if (params.threshold > 0.0f) {
      if (lum < params.threshold - params.threshold_smooth) {
        mask = 0.0f;
      } else if (lum < params.threshold + params.threshold_smooth) {
        float t = (lum - (params.threshold - params.threshold_smooth))
                  / (2.0f * params.threshold_smooth);
        mask = smoothstep(t);
      }
    }

    float glow_r = glow[i * channels + 0];
    float glow_g = glow[i * channels + 1];
    float glow_b = glow[i * channels + 2];

    float br = blend_pixel(r, glow_r, params.blend_mode);
    float bg = blend_pixel(g, glow_g, params.blend_mode);
    float bb = blend_pixel(b, glow_b, params.blend_mode);

    float final_r = lerp(r, br, params.intensity * mask);
    float final_g = lerp(g, bg, params.intensity * mask);
    float final_b = lerp(b, bb, params.intensity * mask);

    // Preserve luminosity if requested
    if (params.preserve_luminosity) {
      float orig_lum = lum;
      float new_lum = luminance(final_r, final_g, final_b);
      if (new_lum > 0.0001f) {
        float ratio = orig_lum / new_lum;
        final_r *= ratio;
        final_g *= ratio;
        final_b *= ratio;
      }
    }

    output[i * channels + 0] = clamp(final_r, 0.0f, 1.0f);
    output[i * channels + 1] = clamp(final_g, 0.0f, 1.0f);
    output[i * channels + 2] = clamp(final_b, 0.0f, 1.0f);
  }
}

// ==========================================================================
//  Section B: Lens flares
// ==========================================================================

void apply_lens_flare(const std::vector<float>& input,
                      std::vector<float>& output,
                      int width, int height,
                      const LensFlareParams& params) {
  int total = width * height;
  int channels = 3;
  output.resize(total * channels);

  // Copy input
  std::memcpy(output.data(), input.data(), total * channels * sizeof(float));

  float lx = params.light_x * static_cast<float>(width);
  float ly = params.light_y * static_cast<float>(height);
  float cx = static_cast<float>(width) * 0.5f;
  float cy = static_cast<float>(height) * 0.5f;

  // Helper: add flare contribution to output
  auto add_flare = [&](float px, float py, float val, float cr, float cg, float cb) {
    int x = static_cast<int>(px);
    int y = static_cast<int>(py);
    if (x < 0 || x >= width || y < 0 || y >= height) return;
    int idx = (y * width + x) * channels;
    float v = val * params.global_intensity;
    output[idx + 0] = clamp(output[idx + 0] + v * cr, 0.0f, 1.0f);
    output[idx + 1] = clamp(output[idx + 1] + v * cg, 0.0f, 1.0f);
    output[idx + 2] = clamp(output[idx + 2] + v * cb, 0.0f, 1.0f);
  };

  // Central light glow
  float glow_r = params.light_glow_radius;
  if (glow_r > 0.0f && params.light_glow_intensity > 0.0f) {
    for (int y = 0; y < height; ++y) {
      for (int x = 0; x < width; ++x) {
        float dx = static_cast<float>(x) - lx;
        float dy = static_cast<float>(y) - ly;
        float dist = std::sqrt(dx * dx + dy * dy);
        float val = std::exp(-dist * dist / (2.0f * glow_r * glow_r)) * params.light_glow_intensity;
        if (val > 0.001f) {
          int idx = (y * width + x) * channels;
          output[idx + 0] = clamp(output[idx + 0] + val * params.global_intensity, 0.0f, 1.0f);
          output[idx + 1] = clamp(output[idx + 1] + val * params.global_intensity, 0.0f, 1.0f);
          output[idx + 2] = clamp(output[idx + 2] + val * params.global_intensity, 0.0f, 1.0f);
        }
      }
    }
  }

  // Anamorphic streaks
  for (const auto& streak : params.streaks) {
    float cos_a = std::cos(streak.angle);
    float sin_a = std::sin(streak.angle);
    float half_len = streak.length * 0.5f;

    // Cast rays in both directions
    for (float t = -half_len; t <= half_len; t += 1.0f) {
      float px = lx + t * cos_a;
      float py = ly + t * sin_a;
      float dist_along = std::abs(t) / half_len;
      float base_val = streak.intensity * (1.0f - std::pow(dist_along, streak.falloff));

      // Cross-section is Gaussian
      int px_i = static_cast<int>(px);
      int py_i = static_cast<int>(py);
      int thick_r = static_cast<int>(streak.thickness * 3.0f) + 1;
      for (int dy = -thick_r; dy <= thick_r; ++dy) {
        for (int dx = -thick_r; dx <= thick_r; ++dx) {
          int sx = px_i + dx;
          int sy = py_i + dy;
          if (sx < 0 || sx >= width || sy < 0 || sy >= height) continue;
          float cross_dist = std::sqrt(static_cast<float>(dx * dx + dy * dy));
          float cross_val = std::exp(-cross_dist * cross_dist / (2.0f * streak.thickness * streak.thickness));
          float val = base_val * cross_val;
          if (val > 0.001f) {
            add_flare(static_cast<float>(sx), static_cast<float>(sy),
                      val, streak.color_r, streak.color_g, streak.color_b);
          }
        }
      }
    }
  }

  // Ghost reflections
  for (const auto& ghost : params.ghosts) {
    // Direction from center to light source
    float dir_x = lx - cx;
    float dir_y = ly - cy;
    float dir_len = std::sqrt(dir_x * dir_x + dir_y * dir_y);
    if (dir_len < 0.001f) continue;
    float nx = dir_x / dir_len;
    float ny = dir_y / dir_len;

    // Ghost positions along the axis through center
    float base_angle = std::atan2(ny, nx);
    float ghost_angle = base_angle + ghost.angle;

    float gx = cx + ghost.distance * std::cos(ghost_angle);
    float gy = cy + ghost.distance * std::sin(ghost_angle);

    int gi = static_cast<int>(gx);
    int gj = static_cast<int>(gy);
    int r = static_cast<int>(ghost.radius * 3.0f) + 1;

    for (int dy = -r; dy <= r; ++dy) {
      for (int dx = -r; dx <= r; ++dx) {
        int sx = gi + dx;
        int sy = gj + dy;
        if (sx < 0 || sx >= width || sy < 0 || sy >= height) continue;
        float dist = std::sqrt(static_cast<float>(dx * dx + dy * dy)) / ghost.radius;
        if (dist > 1.0f) continue;
        float val = ghost.intensity * (1.0f - std::pow(dist, ghost.falloff));
        if (val > 0.001f) {
          add_flare(static_cast<float>(sx), static_cast<float>(sy),
                    val, ghost.color_r, ghost.color_g, ghost.color_b);
        }
      }
    }

    // Mirror through center if enabled
    if (params.screen_center_axis) {
      float mgx = 2.0f * cx - gx;
      float mgy = 2.0f * cy - gy;
      int mgi = static_cast<int>(mgx);
      int mgj = static_cast<int>(mgy);
      for (int dy = -r; dy <= r; ++dy) {
        for (int dx = -r; dx <= r; ++dx) {
          int sx = mgi + dx;
          int sy = mgj + dy;
          if (sx < 0 || sx >= width || sy < 0 || sy >= height) continue;
          float dist = std::sqrt(static_cast<float>(dx * dx + dy * dy)) / ghost.radius;
          if (dist > 1.0f) continue;
          float val = ghost.intensity * 0.5f * (1.0f - std::pow(dist, ghost.falloff));
          if (val > 0.001f) {
            add_flare(static_cast<float>(sx), static_cast<float>(sy),
                      val, ghost.color_r, ghost.color_g, ghost.color_b);
          }
        }
      }
    }
  }

  // Ring flare
  if (params.has_ring_flare) {
    const auto& rf = params.ring_flare;
    float mid_r = (rf.inner_radius + rf.outer_radius) * 0.5f;
    float half_w = (rf.outer_radius - rf.inner_radius) * 0.5f;
    // Direction from center to light source
    float dir_x = lx - cx;
    float dir_y = ly - cy;
    float dir_len = std::sqrt(dir_x * dir_x + dir_y * dir_y);
    float base_angle = (dir_len > 0.001f) ? std::atan2(dir_y, dir_x) : 0.0f;

    int ring_r = static_cast<int>(rf.outer_radius) + 1;
    for (int dy = -ring_r; dy <= ring_r; ++dy) {
      for (int dx = -ring_r; dx <= ring_r; ++dx) {
        int sx = static_cast<int>(lx) + dx;
        int sy = static_cast<int>(ly) + dy;
        if (sx < 0 || sx >= width || sy < 0 || sy >= height) continue;
        float dist = std::sqrt(static_cast<float>(dx * dx + dy * dy));
        if (dist < rf.inner_radius || dist > rf.outer_radius) continue;

        float ring_dist = std::abs(dist - mid_r) / half_w;
        if (ring_dist > 1.0f) continue;

        // Segment check
        if (rf.segments > 0) {
          float ang = std::atan2(static_cast<float>(dy), static_cast<float>(dx));
          float rel_ang = ang - base_angle;
          while (rel_ang < 0.0f) rel_ang += 2.0f * static_cast<float>(M_PI);
          while (rel_ang >= 2.0f * static_cast<float>(M_PI)) rel_ang -= 2.0f * static_cast<float>(M_PI);
          float seg_size = (2.0f * static_cast<float>(M_PI) - rf.segment_angle * rf.segments) / rf.segments;
          float seg_start = 0.0f;
          bool in_seg = false;
          for (int s = 0; s < rf.segments; ++s) {
            if (rel_ang >= seg_start && rel_ang < seg_start + seg_size) {
              in_seg = true;
              break;
            }
            seg_start += seg_size + rf.segment_angle;
          }
          if (!in_seg) continue;
        }

        float val = rf.intensity * (1.0f - std::pow(ring_dist, rf.falloff));
        if (val > 0.001f) {
          add_flare(static_cast<float>(sx), static_cast<float>(sy),
                    val, rf.color_r, rf.color_g, rf.color_b);
        }
      }
    }
  }
}

// ==========================================================================
//  Section C: Dehaze
// ==========================================================================

void apply_dehaze(const std::vector<float>& input,
                  std::vector<float>& output,
                  int width, int height,
                  const DehazeParams& params) {
  int total = width * height;
  int channels = 3;
  output.resize(total * channels);

  // Step 1: Dark channel
  std::vector<float> dc;
  dark_channel(input, dc, width, height, params.patch_size);

  // Step 2: Estimate atmospheric light (top 0.1% brightest pixels in dark channel)
  float atm_r = params.atmospheric_light_r;
  float atm_g = params.atmospheric_light_g;
  float atm_b = params.atmospheric_light_b;

  if (atm_r == 0.0f && atm_g == 0.0f && atm_b == 0.0f) {
    // Auto-estimate
    std::vector<std::pair<float, int>> dc_indexed(total);
    for (int i = 0; i < total; ++i) {
      dc_indexed[i] = {dc[i], i};
    }
    int num_candidates = std::max(1, total / 1000);
    std::partial_sort(dc_indexed.begin(), dc_indexed.begin() + num_candidates, dc_indexed.end(),
                      [](const auto& a, const auto& b) { return a.first > b.first; });

    atm_r = 0.0f;
    atm_g = 0.0f;
    atm_b = 0.0f;
    for (int i = 0; i < num_candidates; ++i) {
      int idx = dc_indexed[i].second * channels;
      atm_r += input[idx + 0];
      atm_g += input[idx + 1];
      atm_b += input[idx + 2];
    }
    atm_r /= static_cast<float>(num_candidates);
    atm_g /= static_cast<float>(num_candidates);
    atm_b /= static_cast<float>(num_candidates);
  }

  // Step 3: Transmission map
  std::vector<float> transmission(total);
  for (int i = 0; i < total; ++i) {
    int idx = i * channels;
    float norm = std::min({input[idx] / std::max(atm_r, 0.0001f),
                            input[idx + 1] / std::max(atm_g, 0.0001f),
                            input[idx + 2] / std::max(atm_b, 0.0001f)});
    transmission[i] = 1.0f - params.omega * dc[i];
    transmission[i] = std::max(transmission[i], params.t0);
  }

  // Step 4: Guided filter refinement using luminance as guide
  std::vector<float> guide(total);
  for (int i = 0; i < total; ++i) {
    int idx = i * channels;
    guide[i] = luminance(input[idx], input[idx + 1], input[idx + 2]);
  }

  std::vector<float> refined_transmission;
  guided_filter(guide, transmission, refined_transmission,
                width, height, params.guided_filter_radius, params.guided_filter_eps);

  // Step 5: Recover scene radiance
  for (int i = 0; i < total; ++i) {
    int idx = i * channels;
    float t = std::max(refined_transmission[i], params.t0);

    float dehaze_r = (input[idx + 0] - atm_r) / t + atm_r;
    float dehaze_g = (input[idx + 1] - atm_g) / t + atm_g;
    float dehaze_b = (input[idx + 2] - atm_b) / t + atm_b;

    // Blend with original based on strength
    output[idx + 0] = clamp(lerp(input[idx + 0], dehaze_r, params.strength), 0.0f, 1.0f);
    output[idx + 1] = clamp(lerp(input[idx + 1], dehaze_g, params.strength), 0.0f, 1.0f);
    output[idx + 2] = clamp(lerp(input[idx + 2], dehaze_b, params.strength), 0.0f, 1.0f);
  }
}

// ==========================================================================
//  Section D: Collage maker
// ==========================================================================

std::vector<CollageSlot> compute_layout_slots(CollageLayout layout,
                                               int num_images,
                                               float spacing) {
  std::vector<CollageSlot> slots;

  switch (layout) {
    case CollageLayout::GRID_2X2: {
      int cols = 2, rows = 2;
      float gap = spacing;
      float cell_w = (1.0f - gap * (cols + 1)) / cols;
      float cell_h = (1.0f - gap * (rows + 1)) / rows;
      for (int i = 0; i < std::min(num_images, 4); ++i) {
        int r = i / cols;
        int c = i % cols;
        CollageSlot s;
        s.image_index = i;
        s.x = gap + c * (cell_w + gap);
        s.y = gap + r * (cell_h + gap);
        s.w = cell_w;
        s.h = cell_h;
        slots.push_back(s);
      }
      break;
    }
    case CollageLayout::GRID_3X3: {
      int cols = 3, rows = 3;
      float gap = spacing;
      float cell_w = (1.0f - gap * (cols + 1)) / cols;
      float cell_h = (1.0f - gap * (rows + 1)) / rows;
      for (int i = 0; i < std::min(num_images, 9); ++i) {
        int r = i / cols;
        int c = i % cols;
        CollageSlot s;
        s.image_index = i;
        s.x = gap + c * (cell_w + gap);
        s.y = gap + r * (cell_h + gap);
        s.w = cell_w;
        s.h = cell_h;
        slots.push_back(s);
      }
      break;
    }
    case CollageLayout::HORIZONTAL_STRIP: {
      int n = std::max(1, num_images);
      float gap = spacing;
      float cell_w = (1.0f - gap * (n + 1)) / n;
      float cell_h = 1.0f - gap * 2.0f;
      for (int i = 0; i < n; ++i) {
        CollageSlot s;
        s.image_index = i;
        s.x = gap + i * (cell_w + gap);
        s.y = gap;
        s.w = cell_w;
        s.h = cell_h;
        slots.push_back(s);
      }
      break;
    }
    case CollageLayout::VERTICAL_STRIP: {
      int n = std::max(1, num_images);
      float gap = spacing;
      float cell_w = 1.0f - gap * 2.0f;
      float cell_h = (1.0f - gap * (n + 1)) / n;
      for (int i = 0; i < n; ++i) {
        CollageSlot s;
        s.image_index = i;
        s.x = gap;
        s.y = gap + i * (cell_h + gap);
        s.w = cell_w;
        s.h = cell_h;
        slots.push_back(s);
      }
      break;
    }
    case CollageLayout::PYRAMID_3: {
      float gap = spacing;
      // Top: 1 image, bottom: 2 images
      float top_w = 0.5f - gap;
      float top_h = 0.5f - gap * 1.5f;
      float bot_w = (1.0f - gap * 3.0f) * 0.5f;
      float bot_h = 0.5f - gap * 1.5f;

      // Top slot
      CollageSlot s0;
      s0.image_index = 0;
      s0.x = (1.0f - top_w) * 0.5f;
      s0.y = gap;
      s0.w = top_w;
      s0.h = top_h;
      slots.push_back(s0);

      // Bottom slots
      for (int i = 0; i < 2 && (i + 1) < num_images; ++i) {
        CollageSlot s;
        s.image_index = i + 1;
        s.x = gap + i * (bot_w + gap);
        s.y = 0.5f + gap * 0.5f;
        s.w = bot_w;
        s.h = bot_h;
        slots.push_back(s);
      }
      break;
    }
    case CollageLayout::PYRAMID_5: {
      float gap = spacing;
      // Row 1: 1 image, Row 2: 2 images, Row 3: 2 images
      float row_h = (1.0f - gap * 4.0f) / 3.0f;
      float row1_w = 0.5f - gap;
      float row23_w = (1.0f - gap * 3.0f) * 0.5f;

      CollageSlot s0;
      s0.image_index = 0;
      s0.x = (1.0f - row1_w) * 0.5f;
      s0.y = gap;
      s0.w = row1_w;
      s0.h = row_h;
      slots.push_back(s0);

      for (int r = 0; r < 2; ++r) {
        for (int c = 0; c < 2; ++c) {
          int idx = 1 + r * 2 + c;
          if (idx >= num_images) break;
          CollageSlot s;
          s.image_index = idx;
          s.x = gap + c * (row23_w + gap);
          s.y = gap + (r + 1) * (row_h + gap);
          s.w = row23_w;
          s.h = row_h;
          slots.push_back(s);
        }
      }
      break;
    }
    case CollageLayout::SPIRAL_4: {
      float gap = spacing;
      float half = 0.5f - gap * 1.5f;
      // Top-left large
      CollageSlot s0;
      s0.image_index = 0;
      s0.x = gap;
      s0.y = gap;
      s0.w = half;
      s0.h = half;
      slots.push_back(s0);

      // Top-right
      if (num_images > 1) {
        CollageSlot s1;
        s1.image_index = 1;
        s1.x = 0.5f + gap * 0.5f;
        s1.y = gap;
        s1.w = half;
        s1.h = half;
        slots.push_back(s1);
      }
      // Bottom-right
      if (num_images > 2) {
        CollageSlot s2;
        s2.image_index = 2;
        s2.x = 0.5f + gap * 0.5f;
        s2.y = 0.5f + gap * 0.5f;
        s2.w = half;
        s2.h = half;
        slots.push_back(s2);
      }
      // Bottom-left
      if (num_images > 3) {
        CollageSlot s3;
        s3.image_index = 3;
        s3.x = gap;
        s3.y = 0.5f + gap * 0.5f;
        s3.w = half;
        s3.h = half;
        slots.push_back(s3);
      }
      break;
    }
    case CollageLayout::FREE_FORM:
      // Free-form slots must be provided externally
      break;
  }

  return slots;
}

void create_collage(const std::vector<std::vector<float>>& images,
                    const std::vector<int>& widths,
                    const std::vector<int>& heights,
                    std::vector<float>& output,
                    const CollageParams& params) {
  int out_w = params.output_width;
  int out_h = params.output_height;
  int out_total = out_w * out_h;
  output.resize(out_total * 4); // RGBA

  // Fill background
  for (int i = 0; i < out_total; ++i) {
    output[i * 4 + 0] = params.bg_r;
    output[i * 4 + 1] = params.bg_g;
    output[i * 4 + 2] = params.bg_b;
    output[i * 4 + 3] = params.bg_a;
  }

  // Get slots
  std::vector<CollageSlot> slots;
  if (params.layout == CollageLayout::FREE_FORM) {
    slots = params.free_slots;
  } else {
    slots = compute_layout_slots(params.layout,
                                  static_cast<int>(images.size()),
                                  params.spacing);
  }

  for (const auto& slot : slots) {
    if (slot.image_index < 0 || slot.image_index >= static_cast<int>(images.size())) continue;

    const auto& img = images[slot.image_index];
    int iw = widths[slot.image_index];
    int ih = heights[slot.image_index];

    // Slot boundaries in pixels
    int sx0 = static_cast<int>(slot.x * out_w);
    int sy0 = static_cast<int>(slot.y * out_h);
    int sw  = static_cast<int>(slot.w * out_w);
    int sh  = static_cast<int>(slot.h * out_h);

    // Image aspect ratio
    float img_aspect = static_cast<float>(iw) / static_cast<float>(ih);
    float slot_aspect = static_cast<float>(sw) / static_cast<float>(sh);

    int draw_w, draw_h, offset_x, offset_y;

    if (params.fit_mode) {
      // Fit image within slot (letterbox)
      if (img_aspect > slot_aspect) {
        draw_w = sw;
        draw_h = static_cast<int>(sw / img_aspect);
        offset_x = 0;
        offset_y = (sh - draw_h) / 2;
      } else {
        draw_h = sh;
        draw_w = static_cast<int>(sh * img_aspect);
        offset_x = (sw - draw_w) / 2;
        offset_y = 0;
      }
    } else {
      // Fill slot (crop)
      if (img_aspect > slot_aspect) {
        draw_h = sh;
        draw_w = static_cast<int>(sh * img_aspect);
        offset_x = (sw - draw_w) / 2;
        offset_y = 0;
      } else {
        draw_w = sw;
        draw_h = static_cast<int>(sw / img_aspect);
        offset_x = 0;
        offset_y = (sh - draw_h) / 2;
      }
    }

    float cos_a = std::cos(slot.rotation);
    float sin_a = std::sin(slot.rotation);
    float slot_cx = sx0 + sw * 0.5f;
    float slot_cy = sy0 + sh * 0.5f;

    for (int py = 0; py < sh; ++py) {
      for (int px = 0; px < sw; ++px) {
        // Source coordinates in image space
        float src_x = static_cast<float>(px - offset_x) / draw_w;
        float src_y = static_cast<float>(py - offset_y) / draw_h;

        if (src_x < 0.0f || src_x >= 1.0f || src_y < 0.0f || src_y >= 1.0f) continue;

        // Destination pixel
        int dx = sx0 + px;
        int dy = sy0 + py;

        // Apply rotation
        float rdx = static_cast<float>(dx);
        float rdy = static_cast<float>(dy);
        if (slot.rotation != 0.0f) {
          rotate_point(rdx, rdy, slot_cx, slot_cy, cos_a, -sin_a);
        }

        if (rdx < 0.0f || rdx >= out_w - 1.0f || rdy < 0.0f || rdy >= out_h - 1.0f) continue;

        // Bilinear sample from source
        int sx_i = static_cast<int>(src_x * iw);
        int sy_i = static_cast<int>(src_y * ih);
        sx_i = clamp(sx_i, 0, iw - 1);
        sy_i = clamp(sy_i, 0, ih - 1);

        int src_idx = (sy_i * iw + sx_i) * 3;
        float r = img[src_idx + 0];
        float g = img[src_idx + 1];
        float b = img[src_idx + 2];

        // Anti-aliased writing to output (nearest for now, with border check)
        int dx_i = static_cast<int>(rdx);
        int dy_i = static_cast<int>(rdy);
        if (dx_i < 0 || dx_i >= out_w || dy_i < 0 || dy_i >= out_h) continue;

        // Border radius check
        if (slot.border_radius > 0.0f) {
          float rx = static_cast<float>(px) / sw;
          float ry = static_cast<float>(py) / sh;
          float br = slot.border_radius;
          // Check corners
          bool in_corner = false;
          if (rx < br && ry < br) {
            float cx_c = br, cy_c = br;
            float dx_c = rx - cx_c, dy_c = ry - cy_c;
            if (dx_c * dx_c + dy_c * dy_c > br * br) in_corner = true;
          } else if (rx > (1.0f - br) && ry < br) {
            float cx_c = 1.0f - br, cy_c = br;
            float dx_c = rx - cx_c, dy_c = ry - cy_c;
            if (dx_c * dx_c + dy_c * dy_c > br * br) in_corner = true;
          } else if (rx < br && ry > (1.0f - br)) {
            float cx_c = br, cy_c = 1.0f - br;
            float dx_c = rx - cx_c, dy_c = ry - cy_c;
            if (dx_c * dx_c + dy_c * dy_c > br * br) in_corner = true;
          } else if (rx > (1.0f - br) && ry > (1.0f - br)) {
            float cx_c = 1.0f - br, cy_c = 1.0f - br;
            float dx_c = rx - cx_c, dy_c = ry - cy_c;
            if (dx_c * dx_c + dy_c * dy_c > br * br) in_corner = true;
          }
          if (in_corner) continue;
        }

        int out_idx = (dy_i * out_w + dx_i) * 4;
        output[out_idx + 0] = r;
        output[out_idx + 1] = g;
        output[out_idx + 2] = b;
        output[out_idx + 3] = 1.0f;
      }
    }
  }
}

// ==========================================================================
//  Section E: Watermark engine
// ==========================================================================

void render_text_bitmap(const std::string& text,
                        std::vector<float>& output,
                        int buf_w, int buf_h,
                        float color_r, float color_g, float color_b) {
  output.assign(buf_w * buf_h * 4, 0.0f);

  constexpr int CHAR_W = 6;  // cell width
  constexpr int CHAR_H = 9;  // cell height

  int total_chars = static_cast<int>(text.length());
  int text_w = total_chars * CHAR_W;
  int text_h = CHAR_H;

  // Center text in buffer
  int ox = (buf_w - text_w) / 2;
  int oy = (buf_h - text_h) / 2;

  for (int i = 0; i < total_chars; ++i) {
    char ch = text[i];
    render_glyph(ch, output, buf_w, buf_h,
                 ox + i * CHAR_W, oy,
                 color_r, color_g, color_b);
  }
}

// Helper: blend RGBA over RGB
static void blend_rgba_over_rgb(std::vector<float>& output,
                                 int width, int height,
                                 const std::vector<float>& overlay,
                                 int ov_w, int ov_h,
                                 int ox, int oy,
                                 float opacity,
                                 float rotation,
                                 float cos_a, float sin_a) {
  float cx = ox + ov_w * 0.5f;
  float cy = oy + ov_h * 0.5f;

  for (int py = 0; py < ov_h; ++py) {
    for (int px = 0; px < ov_w; ++px) {
      float rdx = static_cast<float>(ox + px);
      float rdy = static_cast<float>(oy + py);

      if (rotation != 0.0f) {
        rotate_point(rdx, rdy, cx, cy, cos_a, -sin_a);
      }

      int dx = static_cast<int>(rdx);
      int dy = static_cast<int>(rdy);
      if (dx < 0 || dx >= width || dy < 0 || dy >= height) continue;

      int ov_idx = (py * ov_w + px) * 4;
      float ov_a = overlay[ov_idx + 3] * opacity;
      if (ov_a <= 0.0f) continue;

      float ov_r = overlay[ov_idx + 0];
      float ov_g = overlay[ov_idx + 1];
      float ov_b = overlay[ov_idx + 2];

      int out_idx = (dy * width + dx) * 3;
      output[out_idx + 0] = output[out_idx + 0] * (1.0f - ov_a) + ov_r * ov_a;
      output[out_idx + 1] = output[out_idx + 1] * (1.0f - ov_a) + ov_g * ov_a;
      output[out_idx + 2] = output[out_idx + 2] * (1.0f - ov_a) + ov_b * ov_a;
    }
  }
}

void apply_text_watermark(const std::vector<float>& input,
                          std::vector<float>& output,
                          int width, int height,
                          const TextWatermarkParams& params) {
  int total = width * height;
  output.resize(total * 3);

  // Copy input
  std::memcpy(output.data(), input.data(), total * 3 * sizeof(float));

  // Render text to RGBA buffer
  int num_chars = static_cast<int>(params.text.length());
  if (num_chars == 0) return;

  constexpr int CHAR_W = 6;
  constexpr int CHAR_H = 9;
  float font_scale = params.font_size / static_cast<float>(CHAR_H);
  int text_w = static_cast<int>(num_chars * CHAR_W * font_scale);
  int text_h = static_cast<int>(CHAR_H * font_scale);

  int buf_w = text_w + 4;  // small padding
  int buf_h = text_h + 4;

  std::vector<float> text_buf;
  text_buf.assign(buf_w * buf_h * 4, 0.0f);

  for (int i = 0; i < num_chars; ++i) {
    char ch = params.text[i];
    int ox = 2 + static_cast<int>(i * CHAR_W * font_scale);
    int oy = 2;
    render_glyph(ch, text_buf, buf_w, buf_h, ox, oy,
                 params.color_r, params.color_g, params.color_b);
  }

  // Drop shadow
  if (params.shadow) {
    std::vector<float> shadow_buf(buf_w * buf_h * 4, 0.0f);
    for (int i = 0; i < buf_w * buf_h; ++i) {
      if (text_buf[i * 4 + 3] > 0.0f) {
        int sx = i % buf_w + static_cast<int>(params.shadow_offset_x);
        int sy = i / buf_w + static_cast<int>(params.shadow_offset_y);
        if (sx >= 0 && sx < buf_w && sy >= 0 && sy < buf_h) {
          int sidx = sy * buf_w + sx;
          shadow_buf[sidx * 4 + 0] = 0.0f;
          shadow_buf[sidx * 4 + 1] = 0.0f;
          shadow_buf[sidx * 4 + 2] = 0.0f;
          shadow_buf[sidx * 4 + 3] = params.shadow_opacity;
        }
      }
    }
    // Render shadow first
    int cx_sh, cy_sh;
    resolve_watermark_position(params.position,
                                width, height, buf_w, buf_h,
                                params.custom_x, params.custom_y,
                                params.margin_x, params.margin_y,
                                cx_sh, cy_sh);
    float cos_a = std::cos(params.rotation);
    float sin_a = std::sin(params.rotation);
    blend_rgba_over_rgb(output, width, height,
                        shadow_buf, buf_w, buf_h,
                        cx_sh, cy_sh, 1.0f, params.rotation, cos_a, sin_a);
  }

  // Render text
  int cx, cy;
  resolve_watermark_position(params.position,
                              width, height, buf_w, buf_h,
                              params.custom_x, params.custom_y,
                              params.margin_x, params.margin_y,
                              cx, cy);
  float cos_a = std::cos(params.rotation);
  float sin_a = std::sin(params.rotation);
  blend_rgba_over_rgb(output, width, height,
                      text_buf, buf_w, buf_h,
                      cx, cy, params.opacity, params.rotation, cos_a, sin_a);
}

void apply_image_watermark(const std::vector<float>& input,
                           std::vector<float>& output,
                           int width, int height,
                           const ImageWatermarkParams& params) {
  int total = width * height;
  output.resize(total * 3);
  std::memcpy(output.data(), input.data(), total * 3 * sizeof(float));

  if (params.image_data.empty()) return;

  int wm_w = static_cast<int>(params.image_w * params.scale);
  int wm_h = static_cast<int>(params.image_h * params.scale);

  // Resize watermark image
  std::vector<float> scaled_wm(wm_w * wm_h * 4, 0.0f);
  for (int y = 0; y < wm_h; ++y) {
    for (int x = 0; x < wm_w; ++x) {
      float u = (static_cast<float>(x) + 0.5f) / wm_w;
      float v = (static_cast<float>(y) + 0.5f) / wm_h;
      float r, g, b, a;
      sample_rgba_bilinear(params.image_data, params.image_w, params.image_h,
                           u, v, r, g, b, a);
      int idx = (y * wm_w + x) * 4;
      scaled_wm[idx + 0] = r;
      scaled_wm[idx + 1] = g;
      scaled_wm[idx + 2] = b;
      scaled_wm[idx + 3] = a;
    }
  }

  int cx, cy;
  resolve_watermark_position(params.position,
                              width, height, wm_w, wm_h,
                              params.custom_x, params.custom_y,
                              params.margin_x, params.margin_y,
                              cx, cy);
  float cos_a = std::cos(params.rotation);
  float sin_a = std::sin(params.rotation);
  blend_rgba_over_rgb(output, width, height,
                      scaled_wm, wm_w, wm_h,
                      cx, cy, params.opacity, params.rotation, cos_a, sin_a);
}

void apply_tile_watermark(const std::vector<float>& input,
                          std::vector<float>& output,
                          int width, int height,
                          const TileWatermarkParams& params) {
  int total = width * height;
  output.resize(total * 3);
  std::memcpy(output.data(), input.data(), total * 3 * sizeof(float));

  if (params.image_data.empty()) return;

  int tw = static_cast<int>(params.tile_w * params.scale);
  int th = static_cast<int>(params.tile_h * params.scale);

  // Resize tile
  std::vector<float> scaled_tile(tw * th * 4, 0.0f);
  for (int y = 0; y < th; ++y) {
    for (int x = 0; x < tw; ++x) {
      float u = (static_cast<float>(x) + 0.5f) / tw;
      float v = (static_cast<float>(y) + 0.5f) / th;
      float r, g, b, a;
      sample_rgba_bilinear(params.image_data, params.tile_w, params.tile_h,
                           u, v, r, g, b, a);
      int idx = (y * tw + x) * 4;
      scaled_tile[idx + 0] = r;
      scaled_tile[idx + 1] = g;
      scaled_tile[idx + 2] = b;
      scaled_tile[idx + 3] = a;
    }
  }

  float cos_a = std::cos(params.rotation);
  float sin_a = std::sin(params.rotation);

  int step_x = tw + static_cast<int>(params.spacing_x);
  int step_y = th + static_cast<int>(params.spacing_y);
  if (step_x < 1) step_x = 1;
  if (step_y < 1) step_y = 1;

  int ox = static_cast<int>(params.offset_x) % step_x;
  if (ox > 0) ox -= step_x;
  int oy = static_cast<int>(params.offset_y) % step_y;
  if (oy > 0) oy -= step_y;

  for (int ty = oy; ty < height; ty += step_y) {
    for (int tx = ox; tx < width; tx += step_x) {
      blend_rgba_over_rgb(output, width, height,
                          scaled_tile, tw, th,
                          tx, ty, params.opacity, params.rotation, cos_a, sin_a);
    }
  }
}

void batch_text_watermark(const std::vector<std::vector<float>>& images,
                          std::vector<std::vector<float>>& outputs,
                          const std::vector<int>& widths,
                          const std::vector<int>& heights,
                          const TextWatermarkParams& params) {
  int n = static_cast<int>(images.size());
  outputs.resize(n);
  for (int i = 0; i < n; ++i) {
    apply_text_watermark(images[i], outputs[i], widths[i], heights[i], params);
  }
}

void batch_image_watermark(const std::vector<std::vector<float>>& images,
                           std::vector<std::vector<float>>& outputs,
                           const std::vector<int>& widths,
                           const std::vector<int>& heights,
                           const ImageWatermarkParams& params) {
  int n = static_cast<int>(images.size());
  outputs.resize(n);
  for (int i = 0; i < n; ++i) {
    apply_image_watermark(images[i], outputs[i], widths[i], heights[i], params);
  }
}

}  // namespace effects
}  // namespace alcedo