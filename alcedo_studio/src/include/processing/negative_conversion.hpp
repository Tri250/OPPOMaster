//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <vector>

namespace alcedo {
namespace negative {

/// Output color space for the converted image.
enum class OutputSpace {
  sRGB,      ///< Standard sRGB (linear, D65)
  ACES_AP0,  ///< ACES AP0 primaries (linear)
  ACES_AP1,  ///< ACES AP1 primaries (linear, ACEScg)
};

/// Film stock type for characteristic curve approximation.
/// Based on the NegPy research into common color negative films.
enum class FilmStock {
  Generic,          ///< Generic/unknown color negative
  Kodak_Portra_160,   ///< Kodak Portra 160
  Kodak_Portra_400,   ///< Kodak Portra 400
  Kodak_Portra_800,   ///< Kodak Portra 800
  Kodak_Ektar_100,    ///< Kodak Ektar 100
  Kodak_Gold_200,     ///< Kodak Gold 200
  Kodak_UltraMax_400, ///< Kodak UltraMax 400
  Fuji_Superia_200,   ///< Fuji Superia 200
  Fuji_Superia_400,   ///< Fuji Superia 400
  Fuji_Superia_800,   ///< Fuji Superia 800
  Fuji_Pro_400H,      ///< Fuji Pro 400H
};

/// Per-channel characteristic curve parameters.
/// Each channel (R, G, B) has its own density response curve
/// approximated by a rational function: f(x) = (a*x + b) / (c*x + d)
/// where x is the input linear exposure and f(x) is the density.
struct ChannelCurve {
  float a = 1.0f;   ///< Numerator linear coefficient
  float b = 0.0f;   ///< Numerator constant term
  float c = 0.0f;   ///< Denominator linear coefficient
  float d = 1.0f;   ///< Denominator constant term
  float toe = 0.0f; ///< Toe roll-off factor (0 = no toe)
  float shoulder = 0.0f; ///< Shoulder roll-off factor (0 = no shoulder)
};

/// Full set of film characteristic curves for all three channels.
struct FilmCurves {
  ChannelCurve red;
  ChannelCurve green;
  ChannelCurve blue;
};

/// Color channel weighting for fine-tuning the conversion look.
/// These weights are applied during the density inversion step
/// to adjust the relative contribution of each color channel.
struct ChannelWeights {
  float red = 1.0f;    ///< Red channel weight (default 1.0)
  float green = 1.0f;  ///< Green channel weight (default 1.0)
  float blue = 1.0f;   ///< Blue channel weight (default 1.0)
};

/// Base density (orange mask) correction parameters.
/// Color negative film has an orange-tinted base that must be
/// subtracted during inversion. The base density varies by film stock.
struct BaseDensity {
  float red = 0.20f;    ///< Red channel base density
  float green = 0.60f;  ///< Green channel base density
  float blue = 0.80f;   ///< Blue channel base density
};

/// White balance adjustment parameters.
/// Specified as per-channel multipliers applied after inversion.
struct WhiteBalance {
  float red = 1.0f;   ///< Red channel multiplier
  float green = 1.0f; ///< Green channel multiplier
  float blue = 1.0f;  ///< Blue channel multiplier
};

/// Negative conversion parameters.
/// All parameters have sensible defaults for a generic color negative.
struct NegativeParams {
  FilmStock film_stock = FilmStock::Generic;
  FilmCurves curves;           ///< Custom curves (used if film_stock == Generic)
  ChannelWeights weights;     ///< Color channel weighting
  BaseDensity base_density;   ///< Orange mask base density
  WhiteBalance white_balance; ///< White balance adjustment
  float exposure = 0.0f;      ///< Exposure adjustment in stops
  float contrast = 1.0f;      ///< Contrast adjustment (1.0 = no change)
  float saturation = 1.0f;    ///< Saturation adjustment (1.0 = no change)
  OutputSpace output_space = OutputSpace::sRGB; ///< Target output color space
  bool auto_white_balance = false; ///< If true, estimate WB from image
  bool auto_exposure = false;      ///< If true, estimate exposure from image
};

/// Retrieve the preset characteristic curves for a known film stock.
/// Returns the curves for the given film stock, or a generic curve
/// if the stock is not recognized.
///
/// @param stock  The film stock type.
/// @return       The characteristic curves for the film.
FilmCurves get_film_curves(FilmStock stock);

/// Retrieve the default base density for a known film stock.
///
/// @param stock  The film stock type.
/// @return       The base density (orange mask) values.
BaseDensity get_base_density(FilmStock stock);

/// Invert a single channel using the characteristic curve.
/// Applies the inverse of the density curve to convert from
/// measured density back to linear scene exposure.
///
/// @param density   Input density value (scanned film value).
/// @param curve     The characteristic curve parameters.
/// @return          Linear scene exposure value.
float invert_channel(float density, const ChannelCurve& curve);

/// Convert a color negative scan to linear scene-referred RGB.
///
/// This is the main conversion pipeline:
///   1. Subtract base density (orange mask removal)
///   2. Invert characteristic curves per channel
///   3. Apply channel weighting
///   4. Apply exposure and contrast adjustments
///   5. Apply white balance
///   6. Calibrate to output color space (sRGB or ACES)
///
/// All processing is done in 32-bit floating point.
/// Input and output are planar RGB: red, green, blue channels stored
/// as separate float arrays, row-major, values in [0, 1] range.
///
/// @param input_r     Red channel input, row-major, [0..1].
/// @param input_g     Green channel input, row-major, [0..1].
/// @param input_b     Blue channel input, row-major, [0..1].
/// @param output_r    Red channel output, row-major, [0..1].
/// @param output_g    Green channel output, row-major, [0..1].
/// @param output_b    Blue channel output, row-major, [0..1].
/// @param width       Image width in pixels.
/// @param height      Image height in pixels.
/// @param params      Conversion parameters.
void convert_negative(const std::vector<float>& input_r,
                      const std::vector<float>& input_g,
                      const std::vector<float>& input_b,
                      std::vector<float>& output_r,
                      std::vector<float>& output_g,
                      std::vector<float>& output_b,
                      int width,
                      int height,
                      const NegativeParams& params);

/// Convenience overload for interleaved RGB input.
/// Input and output are interleaved: RGBRGBRGB..., row-major.
///
/// @param input    Interleaved RGB input, [0..1].
/// @param output   Interleaved RGB output, [0..1].
/// @param width    Image width in pixels.
/// @param height   Image height in pixels.
/// @param params   Conversion parameters.
void convert_negative_interleaved(const std::vector<float>& input,
                                  std::vector<float>& output,
                                  int width,
                                  int height,
                                  const NegativeParams& params);

/// Estimate auto-exposure from the negative scan.
/// Computes the median density and returns an exposure adjustment
/// in stops that centers the image histogram.
///
/// @param input_r   Red channel input.
/// @param input_g   Green channel input.
/// @param input_b   Blue channel input.
/// @param width     Image width.
/// @param height    Image height.
/// @return          Exposure adjustment in stops.
float estimate_exposure(const std::vector<float>& input_r,
                        const std::vector<float>& input_g,
                        const std::vector<float>& input_b,
                        int width,
                        int height);

/// Estimate auto white balance from the inverted image.
/// Finds the per-channel multipliers that gray-world the image.
///
/// @param input_r   Red channel input (already inverted).
/// @param input_g   Green channel input (already inverted).
/// @param input_b   Blue channel input (already inverted).
/// @param width     Image width.
/// @param height    Image height.
/// @return          White balance multipliers.
WhiteBalance estimate_white_balance(const std::vector<float>& input_r,
                                    const std::vector<float>& input_g,
                                    const std::vector<float>& input_b,
                                    int width,
                                    int height);

}  // namespace negative
}  // namespace alcedo