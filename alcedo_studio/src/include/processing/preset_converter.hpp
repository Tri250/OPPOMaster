//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <map>
#include <optional>
#include <string>
#include <string_view>
#include <vector>

namespace alcedo {
namespace preset {

/// ---------------------------------------------------------------------------
/// Adjustable parameter ranges (mirrors the internal processing pipeline).
/// Fields marked optional are only populated when the preset explicitly defines
/// the corresponding parameter.
/// ---------------------------------------------------------------------------

/// Tone curve point (input/output in [0..1] normalised space).
struct CurvePoint {
  float input   = 0.0f;
  float output  = 0.0f;
};

/// Per-channel tone curve (RGB).
struct ToneCurve {
  std::vector<CurvePoint> red;
  std::vector<CurvePoint> green;
  std::vector<CurvePoint> blue;
};

/// HSL adjustment for a single hue range.
struct HSLAdjustment {
  int   hue       = 0;      ///< Target hue in degrees [0..360)
  float hue_shift = 0.0f;   ///< Hue shift in degrees
  float saturation = 0.0f;  ///< Saturation adjustment [-100..100]
  float luminance = 0.0f;   ///< Luminance adjustment [-100..100]
};

/// Split-toning parameters.
struct SplitToning {
  float highlight_hue        = 0.0f;   ///< [0..360)
  float highlight_saturation = 0.0f;   ///< [0..100]
  float shadow_hue           = 0.0f;   ///< [0..360)
  float shadow_saturation    = 0.0f;   ///< [0..100]
  float balance              = 0.0f;   ///< [-100..100]
};

/// Lens correction parameters.
struct LensCorrectionParams {
  float distortion       = 0.0f;   ///< Distortion correction amount [0..100]
  float vertical         = 0.0f;   ///< Vertical perspective correction [-100..100]
  float horizontal       = 0.0f;   ///< Horizontal perspective correction [-100..100]
  float rotate           = 0.0f;   ///< Rotation in degrees [-45..45]
  float scale            = 100.0f; ///< Scale percentage [50..150]
  float aspect           = 0.0f;   ///< Aspect ratio correction [-100..100]
  bool  auto_crop        = true;
  bool  remove_chromatic_aberration = true;
  bool  enable_profile_corrections  = true;
};

/// Vignette parameters.
struct VignetteParams {
  float amount       = 0.0f;   ///< [-100..100], negative darkens, positive brightens
  float midpoint     = 50.0f;  ///< [0..100]
  float roundness    = 0.0f;   ///< [-100..100]
  float feather      = 50.0f;  ///< [0..100]
  float highlights   = 0.0f;   ///< Highlight preservation [0..100]
};

/// Grain parameters.
struct GrainParams {
  float amount     = 0.0f;   ///< [0..100]
  float size       = 25.0f;  ///< [0..100]
  float roughness  = 50.0f;  ///< [0..100]
};

/// Colour calibration (per-primary) parameters.
struct ColorCalibration {
  float red_hue        = 0.0f;   ///< [-100..100]
  float red_saturation = 0.0f;   ///< [-100..100]
  float green_hue      = 0.0f;   ///< [-100..100]
  float green_saturation = 0.0f; ///< [-100..100]
  float blue_hue       = 0.0f;   ///< [-100..100]
  float blue_saturation = 0.0f;  ///< [-100..100]
  float shadow_tint    = 0.0f;   ///< [-100..100]
};

/// ---------------------------------------------------------------------------
/// Main preset data structure — AlcedoStudio internal adjustment format.
/// All values use the same ranges as the internal pipeline.
/// ---------------------------------------------------------------------------
struct PresetAdjustments {
  // Basic
  std::optional<float> exposure;     ///< [-5..5] EV
  std::optional<float> contrast;     ///< [-100..100]
  std::optional<float> highlights;   ///< [-100..100]
  std::optional<float> shadows;      ///< [-100..100]
  std::optional<float> whites;       ///< [-100..100]
  std::optional<float> blacks;       ///< [-100..100]

  // White balance
  std::optional<float> temperature;  ///< [2000..50000] Kelvin
  std::optional<float> tint;         ///< [-150..150]

  // Presence
  std::optional<float> vibrance;     ///< [-100..100]
  std::optional<float> saturation;   ///< [-100..100]
  std::optional<float> clarity;      ///< [-100..100]
  std::optional<float> dehaze;       ///< [-100..100]

  // Detail
  std::optional<float> sharpening;       ///< [0..150]
  std::optional<float> sharpening_radius;///< [0.5..3.0]
  std::optional<float> sharpening_detail;///< [0..100]
  std::optional<float> sharpening_masking;///< [0..100]
  std::optional<float> noise_reduction;       ///< [0..100]
  std::optional<float> noise_reduction_detail;///< [0..100]
  std::optional<float> color_noise_reduction; ///< [0..100]

  // Tone curve
  std::optional<ToneCurve> tone_curve;

  // HSL
  std::vector<HSLAdjustment> hsl_adjustments;

  // Split toning
  std::optional<SplitToning> split_toning;

  // Lens correction
  std::optional<LensCorrectionParams> lens_correction;

  // Effects
  std::optional<VignetteParams> vignette;
  std::optional<GrainParams>    grain;

  // Colour calibration
  std::optional<ColorCalibration> color_calibration;

  // Metadata
  std::string preset_name;
  std::string preset_author;
  std::string preset_description;
};

/// ---------------------------------------------------------------------------
/// Result of importing a single preset file.
/// ---------------------------------------------------------------------------
struct PresetImportResult {
  std::string  file_path;
  bool         success = false;
  std::string  error_message;
  PresetAdjustments adjustments;
};

/// ---------------------------------------------------------------------------
/// Parse a Lightroom .xmp or .lrtemplate preset file into the internal format.
///
/// @param file_path  Path to the preset file (.xmp or .lrtemplate).
/// @return A PresetImportResult containing the parsed adjustments and status.
/// ---------------------------------------------------------------------------
auto parse_preset_file(const std::string& file_path) -> PresetImportResult;

/// ---------------------------------------------------------------------------
/// Parse preset data from an in-memory string (useful for embedded presets).
///
/// @param content  Raw content of the preset file.
/// @param ext      File extension hint ("xmp" or "lrtemplate").
/// @return A PresetImportResult containing the parsed adjustments and status.
/// ---------------------------------------------------------------------------
auto parse_preset_content(const std::string& content,
                          const std::string& ext) -> PresetImportResult;

/// ---------------------------------------------------------------------------
/// Batch-import all preset files from a directory (non-recursive).
///
/// @param folder_path  Path to the folder containing preset files.
/// @return A vector of PresetImportResult for every file in the folder.
/// ---------------------------------------------------------------------------
auto batch_import_presets(const std::string& folder_path)
    -> std::vector<PresetImportResult>;

/// ---------------------------------------------------------------------------
/// Export an AlcedoStudio preset to a Lightroom-compatible XMP file.
///
/// @param adjustments  The preset adjustments to export.
/// @param output_path  Path to write the output .xmp file.
/// @return true on success, false on failure.
/// ---------------------------------------------------------------------------
auto export_preset_xmp(const PresetAdjustments& adjustments,
                       const std::string& output_path) -> bool;

/// ---------------------------------------------------------------------------
/// Serialise the preset adjustments to an XMP string (for embedding).
///
/// @param adjustments  The preset adjustments to serialise.
/// @return The XMP string representation.
/// ---------------------------------------------------------------------------
auto serialize_preset_xmp(const PresetAdjustments& adjustments) -> std::string;

}  // namespace preset
}  // namespace alcedo