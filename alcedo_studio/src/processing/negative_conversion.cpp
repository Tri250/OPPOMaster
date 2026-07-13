//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "processing/negative_conversion.hpp"

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <mutex>
#include <numeric>
#include <stdexcept>
#include <thread>
#include <vector>

namespace alcedo {
namespace negative {

namespace {

// Internal helper: Compute median of a vector of floats
float compute_median(std::vector<float> v) {
  if (v.empty()) return 0.0f;

  size_t mid = v.size() / 2;
  std::nth_element(v.begin(), v.begin() + static_cast<long>(mid), v.end());
  if (v.size() % 2 == 0) {
    float a = v[mid];
    std::nth_element(v.begin(), v.begin() + static_cast<long>(mid) - 1, v.end());
    return (a + v[mid - 1]) * 0.5f;
  }
  return v[mid];
}

// Internal helper: Compute mean of a vector of floats
float compute_mean(const std::vector<float>& v) {
  if (v.empty()) return 0.0f;
  double sum = 0.0;
  for (float f : v) sum += f;
  return static_cast<float>(sum / v.size());
}

// Apply toe/shoulder roll-off to the density value
// This models the non-linear response at the extremes of exposure
float apply_curve_extremes(float value, float toe, float shoulder) {
  // Toe: compress low values (dark areas on negative = bright in output)
  if (value < 0.2f && toe > 0.0f) {
    float t = value / 0.2f;
    float roll = 1.0f + toe * (1.0f - t) * t;
    return value * roll;
  }
  // Shoulder: compress high values (bright areas on negative = dark in output)
  if (value > 0.8f && shoulder > 0.0f) {
    float t = (value - 0.8f) / 0.2f;
    float roll = 1.0f - shoulder * t * t;
    return value * roll;
  }
  return value;
}

// Apply contrast adjustment using a simple S-curve
float apply_contrast(float value, float contrast) {
  // Center value around 0.5, apply contrast, clamp
  float mid = 0.5f;
  if (contrast <= 0.0f) return 0.0f;
  if (contrast == 1.0f) return value;

  // Contrast > 1 increases contrast, < 1 decreases it
  // Using a simple power curve centered at 0.5
  if (value <= 0.0f) return 0.0f;
  if (value >= 1.0f) return 1.0f;

  return std::pow(value, 1.0f / contrast);
}

// Apply saturation adjustment in RGB space (simple NTSC luma preservation)
void apply_saturation(float& r, float& g, float& b, float saturation) {
  if (saturation == 1.0f) return;

  // Luminance (NTSC)
  float l = 0.299f * r + 0.587f * g + 0.114f * b;

  // Blend chroma: scale by saturation, preserve luma
  r = l + saturation * (r - l);
  g = l + saturation * (g - l);
  b = l + saturation * (b - l);

  // Clamp
  r = std::clamp(r, 0.0f, 1.0f);
  g = std::clamp(g, 0.0f, 1.0f);
  b = std::clamp(b, 0.0f, 1.0f);
}

// sRGB to XYZ conversion matrix (linear values)
// D65 white point
constexpr float srgb_to_xyz[3][3] = {
  { 0.4124564f, 0.3575761f, 0.1804375f },
  { 0.2126729f, 0.7151522f, 0.0721750f },
  { 0.0193339f, 0.1191920f, 0.9503041f }
};

// XYZ to AP0 conversion
constexpr float xyz_to_ap0[3][3] = {
  {  1.0498111f, -0.0000000f, -0.0000974f },
  { -0.4959030f,  1.3733885f,  0.0982409f },
  {  0.0000000f,  0.0000000f,  1.0000000f }
};

// XYZ to AP1 conversion
constexpr float xyz_to_ap1[3][3] = {
  {  1.7075360f, -0.6217890f, -0.0407950f },
  {  0.1251760f,  0.9599050f, -0.0849310f },
  {  0.0038820f, -0.0023860f,  1.0863130f }
};

// Multiply 3x3 matrix with 3-vector
inline void multiply_matrix(const float m[3][3], float& r, float& g, float& b) {
  float r_out = m[0][0] * r + m[0][1] * g + m[0][2] * b;
  float g_out = m[1][0] * r + m[1][1] * g + m[1][2] * b;
  float b_out = m[2][0] * r + m[2][1] * g + m[2][2] * b;
  r = r_out;
  g = g_out;
  b = b_out;
}

} // anonymous namespace

FilmCurves get_film_curves(FilmStock stock) {
  FilmCurves curves;

  // Generic/default curve approximation based on NegPy
  // Model: density = (a*x + b) / (c*x + d)
  // where x is input exposure (0-1), density is output density (0-1)
  // We invert this to get exposure back from density:
  // density*(c*x + d) = a*x + b
  // density*c*x + density*d - a*x = b
  // x*(density*c - a) = b - density*d
  // x = (b - density*d) / (density*c - a)

  switch (stock) {
    case FilmStock::Kodak_Portra_160:
      // Portra has softer contrast, slightly different toe/shoulder
      curves.red.a = 1.25f; curves.red.b = 0.01f; curves.red.c = 0.60f; curves.red.d = 0.95f;
      curves.red.toe = 0.15f; curves.red.shoulder = 0.10f;
      curves.green.a = 1.15f; curves.green.b = 0.01f; curves.green.c = 0.50f; curves.green.d = 0.95f;
      curves.green.toe = 0.10f; curves.green.shoulder = 0.08f;
      curves.blue.a = 1.10f; curves.blue.b = 0.01f; curves.blue.c = 0.45f; curves.blue.d = 0.95f;
      curves.blue.toe = 0.08f; curves.blue.shoulder = 0.05f;
      break;

    case FilmStock::Kodak_Portra_400:
      curves.red.a = 1.22f; curves.red.b = 0.01f; curves.red.c = 0.55f; curves.red.d = 0.95f;
      curves.red.toe = 0.18f; curves.red.shoulder = 0.12f;
      curves.green.a = 1.12f; curves.green.b = 0.01f; curves.green.c = 0.48f; curves.green.d = 0.95f;
      curves.green.toe = 0.12f; curves.green.shoulder = 0.10f;
      curves.blue.a = 1.08f; curves.blue.b = 0.01f; curves.blue.c = 0.42f; curves.blue.d = 0.95f;
      curves.blue.toe = 0.10f; curves.blue.shoulder = 0.08f;
      break;

    case FilmStock::Kodak_Portra_800:
      curves.red.a = 1.20f; curves.red.b = 0.01f; curves.red.c = 0.52f; curves.red.d = 0.95f;
      curves.red.toe = 0.20f; curves.red.shoulder = 0.15f;
      curves.green.a = 1.10f; curves.green.b = 0.01f; curves.green.c = 0.45f; curves.green.d = 0.95f;
      curves.green.toe = 0.15f; curves.green.shoulder = 0.12f;
      curves.blue.a = 1.05f; curves.blue.b = 0.01f; curves.blue.c = 0.40f; curves.blue.d = 0.95f;
      curves.blue.toe = 0.12f; curves.blue.shoulder = 0.10f;
      break;

    case FilmStock::Kodak_Ektar_100:
      // Ektar has higher saturation and finer grain, slightly harder contrast
      curves.red.a = 1.30f; curves.red.b = 0.01f; curves.red.c = 0.65f; curves.red.d = 0.95f;
      curves.red.toe = 0.10f; curves.red.shoulder = 0.08f;
      curves.green.a = 1.20f; curves.green.b = 0.01f; curves.green.c = 0.55f; curves.green.d = 0.95f;
      curves.green.toe = 0.08f; curves.green.shoulder = 0.05f;
      curves.blue.a = 1.15f; curves.blue.b = 0.01f; curves.blue.c = 0.50f; curves.blue.d = 0.95f;
      curves.blue.toe = 0.05f; curves.blue.shoulder = 0.03f;
      break;

    case FilmStock::Kodak_Gold_200:
      curves.red.a = 1.24f; curves.red.b = 0.01f; curves.red.c = 0.58f; curves.red.d = 0.95f;
      curves.red.toe = 0.16f; curves.red.shoulder = 0.11f;
      curves.green.a = 1.14f; curves.green.b = 0.01f; curves.green.c = 0.49f; curves.green.d = 0.95f;
      curves.green.toe = 0.11f; curves.green.shoulder = 0.09f;
      curves.blue.a = 1.09f; curves.blue.b = 0.01f; curves.blue.c = 0.44f; curves.blue.d = 0.95f;
      curves.blue.toe = 0.09f; curves.blue.shoulder = 0.06f;
      break;

    case FilmStock::Kodak_UltraMax_400:
      curves.red.a = 1.21f; curves.red.b = 0.01f; curves.red.c = 0.54f; curves.red.d = 0.95f;
      curves.red.toe = 0.19f; curves.red.shoulder = 0.13f;
      curves.green.a = 1.11f; curves.green.b = 0.01f; curves.green.c = 0.47f; curves.green.d = 0.95f;
      curves.green.toe = 0.13f; curves.green.shoulder = 0.11f;
      curves.blue.a = 1.07f; curves.blue.b = 0.01f; curves.blue.c = 0.41f; curves.blue.d = 0.95f;
      curves.blue.toe = 0.11f; curves.blue.shoulder = 0.09f;
      break;

    case FilmStock::Fuji_Superia_200:
      curves.red.a = 1.18f; curves.red.b = 0.01f; curves.red.c = 0.52f; curves.red.d = 0.95f;
      curves.red.toe = 0.14f; curves.red.shoulder = 0.09f;
      curves.green.a = 1.16f; curves.green.b = 0.01f; curves.green.c = 0.53f; curves.green.d = 0.95f;
      curves.green.toe = 0.10f; curves.green.shoulder = 0.07f;
      curves.blue.a = 1.12f; curves.blue.b = 0.01f; curves.blue.c = 0.48f; curves.blue.d = 0.95f;
      curves.blue.toe = 0.07f; curves.blue.shoulder = 0.04f;
      break;

    case FilmStock::Fuji_Superia_400:
      curves.red.a = 1.16f; curves.red.b = 0.01f; curves.red.c = 0.50f; curves.red.d = 0.95f;
      curves.red.toe = 0.16f; curves.red.shoulder = 0.11f;
      curves.green.a = 1.14f; curves.green.b = 0.01f; curves.green.c = 0.51f; curves.green.d = 0.95f;
      curves.green.toe = 0.12f; curves.green.shoulder = 0.09f;
      curves.blue.a = 1.10f; curves.blue.b = 0.01f; curves.blue.c = 0.46f; curves.blue.d = 0.95f;
      curves.blue.toe = 0.09f; curves.blue.shoulder = 0.06f;
      break;

    case FilmStock::Fuji_Superia_800:
      curves.red.a = 1.14f; curves.red.b = 0.01f; curves.red.c = 0.48f; curves.red.d = 0.95f;
      curves.red.toe = 0.18f; curves.red.shoulder = 0.13f;
      curves.green.a = 1.12f; curves.green.b = 0.01f; curves.green.c = 0.49f; curves.green.d = 0.95f;
      curves.green.toe = 0.14f; curves.green.shoulder = 0.11f;
      curves.blue.a = 1.08f; curves.blue.b = 0.01f; curves.blue.c = 0.44f; curves.blue.d = 0.95f;
      curves.blue.toe = 0.11f; curves.blue.shoulder = 0.08f;
      break;

    case FilmStock::Fuji_Pro_400H:
      // Pro 400H has characteristic green cast from the old Fuji emulsion
      curves.red.a = 1.15f; curves.red.b = 0.01f; curves.red.c = 0.49f; curves.red.d = 0.95f;
      curves.red.toe = 0.15f; curves.red.shoulder = 0.10f;
      curves.green.a = 1.18f; curves.green.b = 0.01f; curves.green.c = 0.55f; curves.green.d = 0.95f;
      curves.green.toe = 0.11f; curves.green.shoulder = 0.08f;
      curves.blue.a = 1.10f; curves.blue.b = 0.01f; curves.blue.c = 0.47f; curves.blue.d = 0.95f;
      curves.blue.toe = 0.08f; curves.blue.shoulder = 0.05f;
      break;

    case FilmStock::Generic:
    default:
      // Generic approximation that works for most color negatives
      curves.red.a = 1.20f; curves.red.b = 0.01f; curves.red.c = 0.55f; curves.red.d = 0.95f;
      curves.red.toe = 0.15f; curves.red.shoulder = 0.10f;
      curves.green.a = 1.15f; curves.green.b = 0.01f; curves.green.c = 0.50f; curves.green.d = 0.95f;
      curves.green.toe = 0.10f; curves.green.shoulder = 0.08f;
      curves.blue.a = 1.10f; curves.blue.b = 0.01f; curves.blue.c = 0.45f; curves.blue.d = 0.95f;
      curves.blue.toe = 0.08f; curves.blue.shoulder = 0.05f;
      break;
  }

  return curves;
}

BaseDensity get_base_density(FilmStock stock) {
  // Base density values for the orange mask (density after film development).
  // Higher density means darker, more orange mask.
  // Values are approximate based on typical film stock measurements.

  switch (stock) {
    case FilmStock::Kodak_Portra_160:
      return {0.18f, 0.58f, 0.78f};
    case FilmStock::Kodak_Portra_400:
      return {0.19f, 0.59f, 0.79f};
    case FilmStock::Kodak_Portra_800:
      return {0.20f, 0.60f, 0.80f};
    case FilmStock::Kodak_Ektar_100:
      return {0.17f, 0.57f, 0.77f};
    case FilmStock::Kodak_Gold_200:
      return {0.19f, 0.59f, 0.79f};
    case FilmStock::Kodak_UltraMax_400:
      return {0.20f, 0.60f, 0.80f};
    case FilmStock::Fuji_Superia_200:
      return {0.21f, 0.61f, 0.81f};
    case FilmStock::Fuji_Superia_400:
      return {0.22f, 0.62f, 0.82f};
    case FilmStock::Fuji_Superia_800:
      return {0.23f, 0.63f, 0.83f};
    case FilmStock::Fuji_Pro_400H:
      return {0.20f, 0.65f, 0.82f};  // More green in the base
    case FilmStock::Generic:
    default:
      return {0.20f, 0.60f, 0.80f};  // Typical generic color negative
  }
}

float invert_channel(float density, const ChannelCurve& curve) {
  // The curve is defined as:
  // density = (a * exposure + b) / (c * exposure + d)
  //
  // Solve for exposure:
  // density*(c*exposure + d) = a*exposure + b
  // density*c*exposure + density*d - a*exposure = b
  // exposure*(density*c - a) = b - density*d
  // exposure = (b - density*d) / (density*c - a)

  // Apply toe/shoulder roll-off to density
  density = apply_curve_extremes(density, curve.toe, curve.shoulder);

  float denominator = density * curve.c - curve.a;
  if (std::abs(denominator) < 1e-10f) {
    // Handle singularity - near the zero crossing, return a small value
    return (curve.b - density * curve.d) > 0.0f ? 1.0f : 0.0f;
  }

  float exposure = (curve.b - density * curve.d) / denominator;

  // Clamp to valid range
  return std::clamp(exposure, 0.0f, 1.0f);
}

float estimate_exposure(const std::vector<float>& input_r,
                        const std::vector<float>& input_g,
                        const std::vector<float>& input_b,
                        int width,
                        int height) {
  if (input_r.empty() || input_r.size() != static_cast<size_t>(width * height)) {
    throw std::invalid_argument("Invalid input dimensions for exposure estimation");
  }

  // Sample every 16th pixel to speed up
  std::vector<float> all_densities;
  all_densities.reserve((width * height) / 16);

  for (int y = 0; y < height; y += 4) {
    for (int x = 0; x < width; x += 4) {
      size_t idx = static_cast<size_t>(y * width + x);
      float avg = (input_r[idx] + input_g[idx] + input_b[idx]) / 3.0f;
      all_densities.push_back(avg);
    }
  }

  if (all_densities.empty()) {
    return 0.0f;
  }

  float median_density = compute_median(all_densities);

  // We want the median density to map to about 0.18 exposure (middle gray) after inversion
  // The exposure is (b - density*d)/(density*c - a) for generic curve
  // Let's compute what 0.18 target means for median density
  // The difference (delta) in stops: 2^(target_exposure - current_exposure)
  // We want to adjust exposure so that after inversion we get about 0.5 mid-gray.

  // Approximate target median after inversion: 0.5
  // Current median gives what exposure? Let's just use generic curve for estimation
  ChannelCurve generic = get_film_curves(FilmStock::Generic).red;
  float current_exposure = invert_channel(median_density, generic);

  if (current_exposure <= 0.0f) {
    return +2.0f;  // Very dark negative, needs +2 stops boost
  }
  if (current_exposure >= 1.0f) {
    return -2.0f;  // Very bright negative, needs -2 stops reduction
  }

  // We want median exposure to be around 0.5
  // Difference in stops is log2(target / current)
  float target = 0.5f;
  float delta_stops = std::log2(target / current_exposure);

  // Clamp to reasonable range
  return std::clamp(delta_stops, -4.0f, 4.0f);
}

WhiteBalance estimate_white_balance(const std::vector<float>& input_r,
                                    const std::vector<float>& input_g,
                                    const std::vector<float>& input_b,
                                    int width,
                                    int height) {
  if (input_r.empty() || input_r.size() != static_cast<size_t>(width * height)) {
    throw std::invalid_argument("Invalid input dimensions for white balance estimation");
  }

  // Gray-world assumption: average of each channel should be equal
  // Sample every 16th pixel
  double sum_r = 0.0, sum_g = 0.0, sum_b = 0.0;
  int count = 0;

  for (int y = 0; y < height; y += 4) {
    for (int x = 0; x < width; x += 4) {
      size_t idx = static_cast<size_t>(y * width + x);
      // Ignore pure black and pure white pixels (likely borders)
      float l = (input_r[idx] + input_g[idx] + input_b[idx]) / 3.0f;
      if (l > 0.05f && l < 0.95f) {
        sum_r += input_r[idx];
        sum_g += input_g[idx];
        sum_b += input_b[idx];
        ++count;
      }
    }
  }

  if (count <= 0) {
    return {1.0f, 1.0f, 1.0f};
  }

  float mean_r = static_cast<float>(sum_r / count);
  float mean_g = static_cast<float>(sum_g / count);
  float mean_b = static_cast<float>(sum_b / count);

  // All channels should have the same mean. Scale green and blue to match red.
  // Actually, we scale all to the average of all three.
  float mean_avg = (mean_r + mean_g + mean_b) / 3.0f;

  WhiteBalance result;
  result.red = mean_avg / mean_r;
  result.green = mean_avg / mean_g;
  result.blue = mean_avg / mean_b;

  // Clamp to reasonable range
  result.red = std::clamp(result.red, 0.5f, 2.0f);
  result.green = std::clamp(result.green, 0.5f, 2.0f);
  result.blue = std::clamp(result.blue, 0.5f, 2.0f);

  return result;
}

void convert_negative(const std::vector<float>& input_r,
                      const std::vector<float>& input_g,
                      const std::vector<float>& input_b,
                      std::vector<float>& output_r,
                      std::vector<float>& output_g,
                      std::vector<float>& output_b,
                      int width,
                      int height,
                      const NegativeParams& params) {
  // Validate input
  const size_t pixel_count = static_cast<size_t>(width * height);
  if (input_r.empty() || input_r.size() != pixel_count ||
      input_g.size() != pixel_count || input_b.size() != pixel_count) {
    throw std::invalid_argument("Invalid input dimensions for negative conversion");
  }

  // Get preset curves if not generic
  FilmCurves curves = params.film_stock == FilmStock::Generic
                        ? params.curves
                        : get_film_curves(params.film_stock);

  // Get base density
  BaseDensity base = params.film_stock == FilmStock::Generic
                      ? params.base_density
                      : get_base_density(params.film_stock);

  // Prepare output
  output_r.resize(pixel_count);
  output_g.resize(pixel_count);
  output_b.resize(pixel_count);

  // Compute exposure multiplier from stops
  float exposure_mult = std::pow(2.0f, params.exposure);

  // If auto-exposure is requested, estimate it
  float auto_exposure = 0.0f;
  if (params.auto_exposure) {
    auto_exposure = estimate_exposure(input_r, input_g, input_b, width, height);
    exposure_mult *= std::pow(2.0f, auto_exposure);
  }

  // Multi-threaded processing by rows
  int num_threads = std::max(1u, std::thread::hardware_concurrency());
  std::mutex progress_mutex;
  int rows_processed = 0;

  auto process_rows = [&](int start_row, int end_row) {
    for (int y = start_row; y < end_row; ++y) {
      for (int x = 0; x < width; ++x) {
        const size_t idx = static_cast<size_t>(y * width + x);

        // Step 1: Get input density and subtract base density
        float density_r = std::max(0.0f, input_r[idx] - base.red);
        float density_g = std::max(0.0f, input_g[idx] - base.green);
        float density_b = std::max(0.0f, input_b[idx] - base.blue);

        // Step 2: Invert channel characteristic curves
        float exposed_r = invert_channel(density_r, curves.red);
        float exposed_g = invert_channel(density_g, curves.green);
        float exposed_b = invert_channel(density_b, curves.blue);

        // Step 3: Apply channel weighting
        exposed_r *= params.weights.red;
        exposed_g *= params.weights.green;
        exposed_b *= params.weights.blue;

        // Step 4: Apply exposure adjustment
        exposed_r *= exposure_mult;
        exposed_g *= exposure_mult;
        exposed_b *= exposure_mult;

        // Store temporarily for auto-WB estimation if needed
        output_r[idx] = exposed_r;
        output_g[idx] = exposed_g;
        output_b[idx] = exposed_b;
      }
    }

    std::lock_guard<std::mutex> lock(progress_mutex);
    rows_processed += (end_row - start_row);
  };

  std::vector<std::thread> threads;
  int rows_per_thread = (height + num_threads - 1) / num_threads;
  int current_start = 0;

  for (int t = 0; t < num_threads && current_start < height; ++t) {
    int end_row = std::min(current_start + rows_per_thread, height);
    threads.emplace_back(process_rows, current_start, end_row);
    current_start = end_row;
  }

  for (auto& t : threads) {
    t.join();
  }

  // If auto white balance is requested, compute it now that we have inverted data
  WhiteBalance wb = params.white_balance;
  if (params.auto_white_balance) {
    wb = estimate_white_balance(output_r, output_g, output_b, width, height);
  }

  // Final processing: apply white balance, contrast, saturation, color space conversion
  // Do this in a second pass to ensure auto-WB is available
  current_start = 0;
  rows_processed = 0;

  auto final_process_rows = [&](int start_row, int end_row) {
    for (int y = start_row; y < end_row; ++y) {
      for (int x = 0; x < width; ++x) {
        const size_t idx = static_cast<size_t>(y * width + x);

        float r = output_r[idx];
        float g = output_g[idx];
        float b = output_b[idx];

        // Apply white balance
        r *= wb.red;
        g *= wb.green;
        b *= wb.blue;

        // Apply contrast (per-channel simple S-curve)
        if (params.contrast != 1.0f) {
          r = apply_contrast(r, params.contrast);
          g = apply_contrast(g, params.contrast);
          b = apply_contrast(b, params.contrast);
        }

        // Apply saturation
        if (params.saturation != 1.0f) {
          apply_saturation(r, g, b, params.saturation);
        }

        // Convert to output color space if needed
        // Our input after inversion is effectively in the camera's color space
        // which is close to sRGB. We just need calibration to standard primaries.
        if (params.output_space != OutputSpace::sRGB) {
          // Convert sRGB to XYZ, then XYZ to target space
          multiply_matrix(srgb_to_xyz, r, g, b);
          if (params.output_space == OutputSpace::ACES_AP0) {
            multiply_matrix(xyz_to_ap0, r, g, b);
          } else if (params.output_space == OutputSpace::ACES_AP1) {
            multiply_matrix(xyz_to_ap1, r, g, b);
          }
          // Clamp since conversion can push values slightly outside
          r = std::max(0.0f, r);
          g = std::max(0.0f, g);
          b = std::max(0.0f, b);
        }

        // Final clamp to [0, 1]
        output_r[idx] = std::clamp(r, 0.0f, 1.0f);
        output_g[idx] = std::clamp(g, 0.0f, 1.0f);
        output_b[idx] = std::clamp(b, 0.0f, 1.0f);
      }
    }

    std::lock_guard<std::mutex> lock(progress_mutex);
    rows_processed += (end_row - start_row);
  };

  threads.clear();
  current_start = 0;
  for (int t = 0; t < num_threads && current_start < height; ++t) {
    int end_row = std::min(current_start + rows_per_thread, height);
    threads.emplace_back(final_process_rows, current_start, end_row);
    current_start = end_row;
  }

  for (auto& t : threads) {
    t.join();
  }
}

void convert_negative_interleaved(const std::vector<float>& input,
                                  std::vector<float>& output,
                                  int width,
                                  int height,
                                  const NegativeParams& params) {
  const size_t pixel_count = static_cast<size_t>(width * height);
  if (input.empty() || input.size() != 3 * pixel_count) {
    throw std::invalid_argument("Invalid input dimensions for interleaved negative conversion");
  }

  // Deinterleave input
  std::vector<float> input_r(pixel_count);
  std::vector<float> input_g(pixel_count);
  std::vector<float> input_b(pixel_count);

  for (size_t i = 0; i < pixel_count; ++i) {
    input_r[i] = input[i * 3 + 0];
    input_g[i] = input[i * 3 + 1];
    input_b[i] = input[i * 3 + 2];
  }

  // Process
  std::vector<float> output_r, output_g, output_b;
  convert_negative(input_r, input_g, input_b, output_r, output_g, output_b, width, height, params);

  // Interleave output
  output.resize(3 * pixel_count);
  for (size_t i = 0; i < pixel_count; ++i) {
    output[i * 3 + 0] = output_r[i];
    output[i * 3 + 1] = output_g[i];
    output[i * 3 + 2] = output_b[i];
  }
}

}  // namespace negative
}  // namespace alcedo