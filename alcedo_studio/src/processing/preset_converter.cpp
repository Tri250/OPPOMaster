//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "processing/preset_converter.hpp"

#include <algorithm>
#include <cctype>
#include <charconv>
#include <cmath>
#include <cstdlib>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <sstream>
#include <stdexcept>
#include <system_error>
#include <unordered_map>

namespace alcedo {
namespace preset {

// ============================================================================
// Internal helpers
// ============================================================================
namespace {

/// Trim leading/trailing whitespace.
auto trim(std::string_view sv) -> std::string_view {
  auto const start = sv.find_first_not_of(" \t\r\n");
  if (start == std::string_view::npos) return {};
  auto const end = sv.find_last_not_of(" \t\r\n");
  return sv.substr(start, end - start + 1);
}

/// Case-insensitive string comparison.
auto iequals(std::string_view a, std::string_view b) -> bool {
  return a.size() == b.size() &&
         std::equal(a.begin(), a.end(), b.begin(),
                    [](char ca, char cb) { return std::tolower(ca) == std::tolower(cb); });
}

/// Safe string-to-float conversion.
auto to_float(std::string_view sv) -> std::optional<float> {
  if (sv.empty()) return std::nullopt;
  std::string s(sv);
  char* end = nullptr;
  float v = std::strtof(s.c_str(), &end);
  if (end == s.c_str()) return std::nullopt;
  return v;
}

/// Safe string-to-int conversion.
auto to_int(std::string_view sv) -> std::optional<int> {
  if (sv.empty()) return std::nullopt;
  int v = 0;
  auto [ptr, ec] = std::from_chars(sv.data(), sv.data() + sv.size(), v);
  if (ec != std::errc{}) return std::nullopt;
  return v;
}

/// -----------------------------------------------------------------------
/// Simple XML parser — extracts element text by tag name.
/// Designed for the limited XML vocabulary of Lightroom XMP presets.
/// -----------------------------------------------------------------------
class XmlParser {
 public:
  explicit XmlParser(std::string_view xml) : xml_(xml), pos_(0) {}

  /// Extract the text content of the first occurrence of `tag`.
  auto get(std::string_view tag) -> std::optional<std::string> {
    auto open = "<" + std::string(tag);
    auto close = "</" + std::string(tag) + ">";

    auto start = xml_.find(open, pos_);
    if (start == std::string_view::npos) return std::nullopt;

    // Skip past the opening tag (may have attributes, so find '>')
    auto tag_end = xml_.find('>', start);
    if (tag_end == std::string_view::npos) return std::nullopt;

    auto end = xml_.find(close, tag_end);
    if (end == std::string_view::npos) return std::nullopt;

    pos_ = end + close.size();
    return std::string(xml_.substr(tag_end + 1, end - tag_end - 1));
  }

  /// Extract a named-attribute value from a tag.  e.g.
  ///   <rdf:li xml:lang="x-default">value</rdf:li>
  ///   attr_value("rdf:li", "xml:lang", "x-default") → "value"
  auto get_attr_element(std::string_view tag,
                        std::string_view attr_name,
                        std::string_view attr_value) -> std::optional<std::string> {
    auto open = "<" + std::string(tag);
    auto close = "</" + std::string(tag) + ">";

    auto search_from = pos_;
    while (true) {
      auto start = xml_.find(open, search_from);
      if (start == std::string_view::npos) return std::nullopt;

      auto tag_end = xml_.find('>', start);
      if (tag_end == std::string_view::npos) return std::nullopt;

      // Check if this instance has the desired attribute
      auto tag_content = xml_.substr(start, tag_end - start);
      auto needle = std::string(attr_name) + "=\"" + std::string(attr_value) + "\"";
      if (tag_content.find(needle) != std::string_view::npos) {
        auto end = xml_.find(close, tag_end);
        if (end == std::string_view::npos) return std::nullopt;
        pos_ = end + close.size();
        return std::string(xml_.substr(tag_end + 1, end - tag_end - 1));
      }

      search_from = tag_end + 1;
    }
  }

  /// Reset internal position for re-parsing.
  void reset() { pos_ = 0; }

 private:
  std::string_view xml_;
  size_t pos_;
};

/// -----------------------------------------------------------------------
/// Lightroom .lrtemplate parser (older key=value format).
/// Lines are "key = value", keys use dotted paths.
/// -----------------------------------------------------------------------
class LrtemplateParser {
 public:
  explicit LrtemplateParser(std::string_view content) {
    parse(content);
  }

  auto get(std::string_view key) const -> std::optional<std::string> {
    auto it = values_.find(std::string(key));
    if (it != values_.end()) return it->second;
    return std::nullopt;
  }

  auto get_all() const -> const std::unordered_map<std::string, std::string>& {
    return values_;
  }

 private:
  void parse(std::string_view content) {
    std::istringstream stream{std::string(content)};
    std::string line;
    while (std::getline(stream, line)) {
      auto trimmed = trim(line);
      if (trimmed.empty() || trimmed.front() == '#') continue;

      auto eq = trimmed.find('=');
      if (eq == std::string_view::npos) continue;

      auto key = trim(trimmed.substr(0, eq));
      auto val = trim(trimmed.substr(eq + 1));
      values_[std::string(key)] = std::string(val);
    }
  }

  std::unordered_map<std::string, std::string> values_;
};

/// -----------------------------------------------------------------------
/// Parse a sequence of float values separated by commas or spaces.
/// -----------------------------------------------------------------------
auto parse_float_seq(std::string_view sv) -> std::vector<float> {
  std::vector<float> result;
  std::istringstream ss{std::string(sv)};
  std::string token;
  while (std::getline(ss, token, ',')) {
    auto trimmed = trim(token);
    if (trimmed.empty()) continue;
    // Also split on spaces
    std::istringstream inner{std::string(trimmed)};
    std::string subtoken;
    while (inner >> subtoken) {
      if (auto v = to_float(subtoken)) result.push_back(*v);
    }
  }
  return result;
}

/// -----------------------------------------------------------------------
/// Convert a Lightroom XMP / crs parameter name to our internal format.
/// -----------------------------------------------------------------------

/// Parse a Lightroom XMP preset (XML) and populate PresetAdjustments.
auto parse_xmp_preset(std::string_view xml) -> PresetAdjustments {
  PresetAdjustments adj;
  XmlParser p(xml);

  // --- Metadata ---
  if (auto v = p.get("dc:title")) {
    if (auto inner = p.get_attr_element("rdf:li", "xml:lang", "x-default")) {
      adj.preset_name = *inner;
    } else {
      adj.preset_name = *v;
    }
  }
  p.reset();
  if (auto v = p.get("dc:creator")) {
    auto inner = p.get_attr_element("rdf:li", "xml:lang", "x-default");
    // dc:creator might be a simple rdf:Seq, try direct
    p.reset();
    adj.preset_author = std::string(trim(*v));
  }
  p.reset();
  if (auto v = p.get("dc:description")) {
    if (auto inner = p.get_attr_element("rdf:li", "xml:lang", "x-default")) {
      adj.preset_description = *inner;
    } else {
      adj.preset_description = std::string(trim(*v));
    }
  }
  p.reset();

  // Helper to get a crs: property value
  auto crs_get = [&](const char* name) -> std::optional<std::string> {
    return p.get(std::string("crs:") + name);
  };

  // --- Basic ---
  if (auto v = crs_get("Exposure2012"); v) adj.exposure = to_float(*v);
  p.reset();
  if (auto v = crs_get("Contrast2012"); v) adj.contrast = to_float(*v);
  p.reset();
  if (auto v = crs_get("Highlights2012"); v) adj.highlights = to_float(*v);
  p.reset();
  if (auto v = crs_get("Shadows2012"); v) adj.shadows = to_float(*v);
  p.reset();
  if (auto v = crs_get("Whites2012"); v) adj.whites = to_float(*v);
  p.reset();
  if (auto v = crs_get("Blacks2012"); v) adj.blacks = to_float(*v);
  p.reset();

  // --- White Balance ---
  if (auto v = crs_get("Temperature"); v) adj.temperature = to_float(*v);
  p.reset();
  if (auto v = crs_get("Tint"); v) adj.tint = to_float(*v);
  p.reset();

  // --- Presence ---
  if (auto v = crs_get("Vibrance"); v) adj.vibrance = to_float(*v);
  p.reset();
  if (auto v = crs_get("Saturation"); v) adj.saturation = to_float(*v);
  p.reset();
  if (auto v = crs_get("Clarity2012"); v) adj.clarity = to_float(*v);
  p.reset();
  if (auto v = crs_get("Dehaze"); v) adj.dehaze = to_float(*v);
  p.reset();

  // --- Detail ---
  if (auto v = crs_get("Sharpness"); v) adj.sharpening = to_float(*v);
  p.reset();
  if (auto v = crs_get("SharpnessRadius"); v) adj.sharpening_radius = to_float(*v);
  p.reset();
  if (auto v = crs_get("SharpnessDetail"); v) adj.sharpening_detail = to_float(*v);
  p.reset();
  if (auto v = crs_get("SharpnessMasking"); v) adj.sharpening_masking = to_float(*v);
  p.reset();
  if (auto v = crs_get("LuminanceNoiseReduction"); v) adj.noise_reduction = to_float(*v);
  p.reset();
  if (auto v = crs_get("LuminanceNoiseReductionDetail"); v) adj.noise_reduction_detail = to_float(*v);
  p.reset();
  if (auto v = crs_get("ColorNoiseReduction"); v) adj.color_noise_reduction = to_float(*v);
  p.reset();

  // --- Tone Curve ---
  {
    auto pts2012 = crs_get("ToneCurvePV2012");
    if (pts2012) {
      ToneCurve tc;
      auto floats = parse_float_seq(*pts2012);
      // crs:ToneCurvePV2012 stores interleaved R,G,B points as [r0,r1,r2,r3, g0,g1,g2,g3, b0,b1,b2,b3]
      // where each point is (input, output).  Typical: 4 points per channel = 24 floats total.
      if (floats.size() >= 24) {
        for (size_t i = 0; i + 1 < 8; i += 2) tc.red.push_back({floats[i], floats[i + 1]});
        for (size_t i = 8; i + 1 < 16; i += 2) tc.green.push_back({floats[i], floats[i + 1]});
        for (size_t i = 16; i + 1 < 24; i += 2) tc.blue.push_back({floats[i], floats[i + 1]});
      } else if (floats.size() >= 8) {
        // Single curve applied to all channels
        for (size_t i = 0; i + 1 < floats.size(); i += 2) {
          CurvePoint pt{floats[i], floats[i + 1]};
          tc.red.push_back(pt);
          tc.green.push_back(pt);
          tc.blue.push_back(pt);
        }
      }
      adj.tone_curve = std::move(tc);
    }
    p.reset();
  }

  // --- HSL ---
  // Lightroom has 8 hue ranges: Red, Orange, Yellow, Green, Aqua, Blue, Purple, Magenta
  static constexpr const char* hsl_hues[] = {"Red", "Orange", "Yellow", "Green",
                                              "Aqua", "Blue", "Purple", "Magenta"};
  static constexpr int hsl_hue_values[] = {0, 30, 60, 120, 180, 240, 270, 300};
  for (int i = 0; i < 8; ++i) {
    HSLAdjustment hsl;
    hsl.hue = hsl_hue_values[i];

    auto hue_key = std::string("crs:HueAdjustment") + hsl_hues[i];
    auto sat_key = std::string("crs:SaturationAdjustment") + hsl_hues[i];
    auto lum_key = std::string("crs:LuminanceAdjustment") + hsl_hues[i];

    p.reset();
    if (auto v = p.get(hue_key); v) hsl.hue_shift = to_float(*v).value_or(0.0f);
    p.reset();
    if (auto v = p.get(sat_key); v) hsl.saturation = to_float(*v).value_or(0.0f);
    p.reset();
    if (auto v = p.get(lum_key); v) hsl.luminance = to_float(*v).value_or(0.0f);

    if (hsl.hue_shift != 0.0f || hsl.saturation != 0.0f || hsl.luminance != 0.0f) {
      adj.hsl_adjustments.push_back(hsl);
    }
  }
  p.reset();

  // --- Split Toning ---
  {
    SplitToning st;
    bool has_st = false;
    p.reset();
    if (auto v = crs_get("SplitToningHighlightHue"); v) {
      st.highlight_hue = to_float(*v).value_or(0.0f); has_st = true;
    }
    p.reset();
    if (auto v = crs_get("SplitToningHighlightSaturation"); v) {
      st.highlight_saturation = to_float(*v).value_or(0.0f); has_st = true;
    }
    p.reset();
    if (auto v = crs_get("SplitToningShadowHue"); v) {
      st.shadow_hue = to_float(*v).value_or(0.0f); has_st = true;
    }
    p.reset();
    if (auto v = crs_get("SplitToningShadowSaturation"); v) {
      st.shadow_saturation = to_float(*v).value_or(0.0f); has_st = true;
    }
    p.reset();
    if (auto v = crs_get("SplitToningBalance"); v) {
      st.balance = to_float(*v).value_or(0.0f); has_st = true;
    }
    if (has_st) adj.split_toning = st;
  }
  p.reset();

  // --- Lens Correction ---
  {
    LensCorrectionParams lc;
    bool has_lc = false;
    if (auto v = crs_get("LensProfileEnable"); v) {
      lc.enable_profile_corrections = (to_int(*v).value_or(0) != 0); has_lc = true;
    }
    p.reset();
    if (auto v = crs_get("RemoveChromaticAberration"); v) {
      lc.remove_chromatic_aberration = (to_int(*v).value_or(0) != 0); has_lc = true;
    }
    p.reset();
    if (auto v = crs_get("AutoLateralCA"); v) {
      lc.remove_chromatic_aberration = (to_int(*v).value_or(0) != 0); has_lc = true;
    }
    p.reset();
    if (auto v = crs_get("LensManualDistortionAmount"); v) {
      lc.distortion = to_float(*v).value_or(0.0f); has_lc = true;
    }
    p.reset();
    if (auto v = crs_get("PerspectiveVertical"); v) {
      lc.vertical = to_float(*v).value_or(0.0f); has_lc = true;
    }
    p.reset();
    if (auto v = crs_get("PerspectiveHorizontal"); v) {
      lc.horizontal = to_float(*v).value_or(0.0f); has_lc = true;
    }
    p.reset();
    if (auto v = crs_get("PerspectiveRotate"); v) {
      lc.rotate = to_float(*v).value_or(0.0f); has_lc = true;
    }
    p.reset();
    if (auto v = crs_get("PerspectiveScale"); v) {
      lc.scale = to_float(*v).value_or(100.0f); has_lc = true;
    }
    p.reset();
    if (auto v = crs_get("PerspectiveAspect"); v) {
      lc.aspect = to_float(*v).value_or(0.0f); has_lc = true;
    }
    p.reset();
    if (auto v = crs_get("AutoWhiteVersion"); v) {
      // Not directly a lens param, but use its presence to detect LR version
    }
    if (has_lc) adj.lens_correction = lc;
  }
  p.reset();

  // --- Vignette ---
  {
    VignetteParams vig;
    bool has_vig = false;
    if (auto v = crs_get("PostCropVignetteAmount"); v) {
      vig.amount = to_float(*v).value_or(0.0f); has_vig = true;
    }
    p.reset();
    if (auto v = crs_get("PostCropVignetteMidpoint"); v) {
      vig.midpoint = to_float(*v).value_or(50.0f); has_vig = true;
    }
    p.reset();
    if (auto v = crs_get("PostCropVignetteRoundness"); v) {
      vig.roundness = to_float(*v).value_or(0.0f); has_vig = true;
    }
    p.reset();
    if (auto v = crs_get("PostCropVignetteFeather"); v) {
      vig.feather = to_float(*v).value_or(50.0f); has_vig = true;
    }
    p.reset();
    if (auto v = crs_get("PostCropVignetteHighlightContrast"); v) {
      vig.highlights = to_float(*v).value_or(0.0f); has_vig = true;
    }
    if (has_vig) adj.vignette = vig;
  }
  p.reset();

  // --- Grain ---
  {
    GrainParams gr;
    bool has_gr = false;
    if (auto v = crs_get("GrainAmount"); v) {
      gr.amount = to_float(*v).value_or(0.0f); has_gr = true;
    }
    p.reset();
    if (auto v = crs_get("GrainSize"); v) {
      gr.size = to_float(*v).value_or(25.0f); has_gr = true;
    }
    p.reset();
    if (auto v = crs_get("GrainFrequency"); v) {
      gr.roughness = to_float(*v).value_or(50.0f); has_gr = true;
    }
    if (has_gr) adj.grain = gr;
  }
  p.reset();

  // --- Color Calibration ---
  {
    ColorCalibration cc;
    bool has_cc = false;
    if (auto v = crs_get("RedHue"); v) {
      cc.red_hue = to_float(*v).value_or(0.0f); has_cc = true;
    }
    p.reset();
    if (auto v = crs_get("RedSaturation"); v) {
      cc.red_saturation = to_float(*v).value_or(0.0f); has_cc = true;
    }
    p.reset();
    if (auto v = crs_get("GreenHue"); v) {
      cc.green_hue = to_float(*v).value_or(0.0f); has_cc = true;
    }
    p.reset();
    if (auto v = crs_get("GreenSaturation"); v) {
      cc.green_saturation = to_float(*v).value_or(0.0f); has_cc = true;
    }
    p.reset();
    if (auto v = crs_get("BlueHue"); v) {
      cc.blue_hue = to_float(*v).value_or(0.0f); has_cc = true;
    }
    p.reset();
    if (auto v = crs_get("BlueSaturation"); v) {
      cc.blue_saturation = to_float(*v).value_or(0.0f); has_cc = true;
    }
    p.reset();
    if (auto v = crs_get("ShadowTint"); v) {
      cc.shadow_tint = to_float(*v).value_or(0.0f); has_cc = true;
    }
    if (has_cc) adj.color_calibration = cc;
  }

  return adj;
}

/// Parse a legacy .lrtemplate file and populate PresetAdjustments.
auto parse_lrtemplate_preset(std::string_view content) -> PresetAdjustments {
  PresetAdjustments adj;
  LrtemplateParser lp(content);

  // --- Metadata ---
  if (auto v = lp.get("name"); v) adj.preset_name = *v;
  if (auto v = lp.get("author"); v) adj.preset_author = *v;
  if (auto v = lp.get("description"); v) adj.preset_description = *v;

  // --- Basic ---
  if (auto v = lp.get("Exposure2012"); v) adj.exposure = to_float(*v);
  if (auto v = lp.get("Contrast2012"); v) adj.contrast = to_float(*v);
  if (auto v = lp.get("Highlights2012"); v) adj.highlights = to_float(*v);
  if (auto v = lp.get("Shadows2012"); v) adj.shadows = to_float(*v);
  if (auto v = lp.get("Whites2012"); v) adj.whites = to_float(*v);
  if (auto v = lp.get("Blacks2012"); v) adj.blacks = to_float(*v);

  // --- White Balance ---
  if (auto v = lp.get("Temperature"); v) adj.temperature = to_float(*v);
  if (auto v = lp.get("Tint"); v) adj.tint = to_float(*v);

  // --- Presence ---
  if (auto v = lp.get("Vibrance"); v) adj.vibrance = to_float(*v);
  if (auto v = lp.get("Saturation"); v) adj.saturation = to_float(*v);
  if (auto v = lp.get("Clarity2012"); v) adj.clarity = to_float(*v);
  if (auto v = lp.get("Dehaze"); v) adj.dehaze = to_float(*v);

  // --- Detail ---
  if (auto v = lp.get("Sharpness"); v) adj.sharpening = to_float(*v);
  if (auto v = lp.get("SharpnessRadius"); v) adj.sharpening_radius = to_float(*v);
  if (auto v = lp.get("SharpnessDetail"); v) adj.sharpening_detail = to_float(*v);
  if (auto v = lp.get("SharpnessMasking"); v) adj.sharpening_masking = to_float(*v);
  if (auto v = lp.get("LuminanceNoiseReduction"); v) adj.noise_reduction = to_float(*v);
  if (auto v = lp.get("LuminanceNoiseReductionDetail"); v) adj.noise_reduction_detail = to_float(*v);
  if (auto v = lp.get("ColorNoiseReduction"); v) adj.color_noise_reduction = to_float(*v);

  // --- Tone Curve ---
  if (auto v = lp.get("ToneCurvePV2012"); v) {
    ToneCurve tc;
    auto floats = parse_float_seq(*v);
    if (floats.size() >= 24) {
      for (size_t i = 0; i + 1 < 8; i += 2) tc.red.push_back({floats[i], floats[i + 1]});
      for (size_t i = 8; i + 1 < 16; i += 2) tc.green.push_back({floats[i], floats[i + 1]});
      for (size_t i = 16; i + 1 < 24; i += 2) tc.blue.push_back({floats[i], floats[i + 1]});
    } else if (floats.size() >= 8) {
      for (size_t i = 0; i + 1 < floats.size(); i += 2) {
        CurvePoint pt{floats[i], floats[i + 1]};
        tc.red.push_back(pt); tc.green.push_back(pt); tc.blue.push_back(pt);
      }
    }
    adj.tone_curve = std::move(tc);
  }

  // --- HSL ---
  static constexpr const char* hsl_hues[] = {"Red", "Orange", "Yellow", "Green",
                                              "Aqua", "Blue", "Purple", "Magenta"};
  static constexpr int hsl_hue_values[] = {0, 30, 60, 120, 180, 240, 270, 300};
  for (int i = 0; i < 8; ++i) {
    HSLAdjustment hsl;
    hsl.hue = hsl_hue_values[i];
    auto hue_key = std::string("HueAdjustment") + hsl_hues[i];
    auto sat_key = std::string("SaturationAdjustment") + hsl_hues[i];
    auto lum_key = std::string("LuminanceAdjustment") + hsl_hues[i];
    if (auto v = lp.get(hue_key); v) hsl.hue_shift = to_float(*v).value_or(0.0f);
    if (auto v = lp.get(sat_key); v) hsl.saturation = to_float(*v).value_or(0.0f);
    if (auto v = lp.get(lum_key); v) hsl.luminance = to_float(*v).value_or(0.0f);
    if (hsl.hue_shift != 0.0f || hsl.saturation != 0.0f || hsl.luminance != 0.0f) {
      adj.hsl_adjustments.push_back(hsl);
    }
  }

  // --- Split Toning ---
  {
    SplitToning st;
    bool has_st = false;
    if (auto v = lp.get("SplitToningHighlightHue"); v) {
      st.highlight_hue = to_float(*v).value_or(0.0f); has_st = true;
    }
    if (auto v = lp.get("SplitToningHighlightSaturation"); v) {
      st.highlight_saturation = to_float(*v).value_or(0.0f); has_st = true;
    }
    if (auto v = lp.get("SplitToningShadowHue"); v) {
      st.shadow_hue = to_float(*v).value_or(0.0f); has_st = true;
    }
    if (auto v = lp.get("SplitToningShadowSaturation"); v) {
      st.shadow_saturation = to_float(*v).value_or(0.0f); has_st = true;
    }
    if (auto v = lp.get("SplitToningBalance"); v) {
      st.balance = to_float(*v).value_or(0.0f); has_st = true;
    }
    if (has_st) adj.split_toning = st;
  }

  // --- Lens Correction ---
  {
    LensCorrectionParams lc;
    bool has_lc = false;
    if (auto v = lp.get("LensProfileEnable"); v) {
      lc.enable_profile_corrections = (to_int(*v).value_or(0) != 0); has_lc = true;
    }
    if (auto v = lp.get("RemoveChromaticAberration"); v) {
      lc.remove_chromatic_aberration = (to_int(*v).value_or(0) != 0); has_lc = true;
    }
    if (auto v = lp.get("AutoLateralCA"); v) {
      lc.remove_chromatic_aberration = (to_int(*v).value_or(0) != 0); has_lc = true;
    }
    if (auto v = lp.get("LensManualDistortionAmount"); v) {
      lc.distortion = to_float(*v).value_or(0.0f); has_lc = true;
    }
    if (auto v = lp.get("PerspectiveVertical"); v) {
      lc.vertical = to_float(*v).value_or(0.0f); has_lc = true;
    }
    if (auto v = lp.get("PerspectiveHorizontal"); v) {
      lc.horizontal = to_float(*v).value_or(0.0f); has_lc = true;
    }
    if (auto v = lp.get("PerspectiveRotate"); v) {
      lc.rotate = to_float(*v).value_or(0.0f); has_lc = true;
    }
    if (auto v = lp.get("PerspectiveScale"); v) {
      lc.scale = to_float(*v).value_or(100.0f); has_lc = true;
    }
    if (auto v = lp.get("PerspectiveAspect"); v) {
      lc.aspect = to_float(*v).value_or(0.0f); has_lc = true;
    }
    if (has_lc) adj.lens_correction = lc;
  }

  // --- Vignette ---
  {
    VignetteParams vig;
    bool has_vig = false;
    if (auto v = lp.get("PostCropVignetteAmount"); v) {
      vig.amount = to_float(*v).value_or(0.0f); has_vig = true;
    }
    if (auto v = lp.get("PostCropVignetteMidpoint"); v) {
      vig.midpoint = to_float(*v).value_or(50.0f); has_vig = true;
    }
    if (auto v = lp.get("PostCropVignetteRoundness"); v) {
      vig.roundness = to_float(*v).value_or(0.0f); has_vig = true;
    }
    if (auto v = lp.get("PostCropVignetteFeather"); v) {
      vig.feather = to_float(*v).value_or(50.0f); has_vig = true;
    }
    if (auto v = lp.get("PostCropVignetteHighlightContrast"); v) {
      vig.highlights = to_float(*v).value_or(0.0f); has_vig = true;
    }
    if (has_vig) adj.vignette = vig;
  }

  // --- Grain ---
  {
    GrainParams gr;
    bool has_gr = false;
    if (auto v = lp.get("GrainAmount"); v) {
      gr.amount = to_float(*v).value_or(0.0f); has_gr = true;
    }
    if (auto v = lp.get("GrainSize"); v) {
      gr.size = to_float(*v).value_or(25.0f); has_gr = true;
    }
    if (auto v = lp.get("GrainFrequency"); v) {
      gr.roughness = to_float(*v).value_or(50.0f); has_gr = true;
    }
    if (has_gr) adj.grain = gr;
  }

  // --- Color Calibration ---
  {
    ColorCalibration cc;
    bool has_cc = false;
    if (auto v = lp.get("RedHue"); v) {
      cc.red_hue = to_float(*v).value_or(0.0f); has_cc = true;
    }
    if (auto v = lp.get("RedSaturation"); v) {
      cc.red_saturation = to_float(*v).value_or(0.0f); has_cc = true;
    }
    if (auto v = lp.get("GreenHue"); v) {
      cc.green_hue = to_float(*v).value_or(0.0f); has_cc = true;
    }
    if (auto v = lp.get("GreenSaturation"); v) {
      cc.green_saturation = to_float(*v).value_or(0.0f); has_cc = true;
    }
    if (auto v = lp.get("BlueHue"); v) {
      cc.blue_hue = to_float(*v).value_or(0.0f); has_cc = true;
    }
    if (auto v = lp.get("BlueSaturation"); v) {
      cc.blue_saturation = to_float(*v).value_or(0.0f); has_cc = true;
    }
    if (auto v = lp.get("ShadowTint"); v) {
      cc.shadow_tint = to_float(*v).value_or(0.0f); has_cc = true;
    }
    if (has_cc) adj.color_calibration = cc;
  }

  return adj;
}

/// Guess the file extension from a path.
auto get_ext(std::string_view path) -> std::string {
  auto dot = path.rfind('.');
  if (dot == std::string_view::npos) return {};
  std::string ext(path.substr(dot + 1));
  std::transform(ext.begin(), ext.end(), ext.begin(),
                 [](char c) { return static_cast<char>(std::tolower(c)); });
  return ext;
}

/// Read entire file into a string.
auto read_file(const std::string& path) -> std::string {
  std::ifstream in(path, std::ios::binary | std::ios::ate);
  if (!in) throw std::runtime_error("Cannot open file: " + path);
  auto size = in.tellg();
  in.seekg(0);
  std::string content(static_cast<size_t>(size), '\0');
  if (!in.read(content.data(), size)) throw std::runtime_error("Failed to read file: " + path);
  return content;
}

/// Escape XML special characters.
auto xml_escape(std::string_view sv) -> std::string {
  std::string result;
  result.reserve(sv.size());
  for (char c : sv) {
    switch (c) {
      case '&':  result += "&amp;"; break;
      case '<':  result += "&lt;"; break;
      case '>':  result += "&gt;"; break;
      case '"':  result += "&quot;"; break;
      case '\'': result += "&apos;"; break;
      default:   result += c; break;
    }
  }
  return result;
}

/// Format a float with reasonable precision.
auto fmt_float(float v) -> std::string {
  char buf[64];
  std::snprintf(buf, sizeof(buf), "%.6g", static_cast<double>(v));
  return buf;
}

}  // namespace

// ============================================================================
// Public API
// ============================================================================

auto parse_preset_file(const std::string& file_path) -> PresetImportResult {
  PresetImportResult result;
  result.file_path = file_path;

  try {
    auto content = read_file(file_path);
    auto ext = get_ext(file_path);

    if (ext == "xmp") {
      result.adjustments = parse_xmp_preset(content);
    } else if (ext == "lrtemplate") {
      result.adjustments = parse_lrtemplate_preset(content);
    } else {
      result.error_message = "Unsupported preset file extension: ." + ext;
      return result;
    }

    result.success = true;
  } catch (const std::exception& e) {
    result.error_message = e.what();
  }

  return result;
}

auto parse_preset_content(const std::string& content,
                          const std::string& ext) -> PresetImportResult {
  PresetImportResult result;
  result.file_path = "<memory>";

  try {
    if (iequals(ext, "xmp")) {
      result.adjustments = parse_xmp_preset(content);
    } else if (iequals(ext, "lrtemplate")) {
      result.adjustments = parse_lrtemplate_preset(content);
    } else {
      result.error_message = "Unsupported preset format: " + ext;
      return result;
    }

    result.success = true;
  } catch (const std::exception& e) {
    result.error_message = e.what();
  }

  return result;
}

auto batch_import_presets(const std::string& folder_path)
    -> std::vector<PresetImportResult> {
  std::vector<PresetImportResult> results;

  std::error_code ec;
  for (auto const& entry : std::filesystem::directory_iterator(folder_path, ec)) {
    if (ec) break;
    if (!entry.is_regular_file()) continue;

    auto ext = get_ext(entry.path().string());
    if (iequals(ext, "xmp") || iequals(ext, "lrtemplate")) {
      results.push_back(parse_preset_file(entry.path().string()));
    }
  }

  return results;
}

auto export_preset_xmp(const PresetAdjustments& adjustments,
                       const std::string& output_path) -> bool {
  try {
    auto xmp = serialize_preset_xmp(adjustments);
    std::ofstream out(output_path, std::ios::binary | std::ios::trunc);
    if (!out) return false;
    out << xmp;
    return true;
  } catch (...) {
    return false;
  }
}

auto serialize_preset_xmp(const PresetAdjustments& adj) -> std::string {
  std::ostringstream ss;

  ss << "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
  ss << "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\" x:xmptk=\"AlcedoStudio\">\n";
  ss << " <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n";
  ss << "  <rdf:Description rdf:about=\"\"\n";
  ss << "   xmlns:crs=\"http://ns.adobe.com/camera-raw-settings/1.0/\"\n";
  ss << "   xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n";
  ss << "   xmlns:xmp=\"http://ns.adobe.com/xap/1.0/\">\n";

  // Metadata
  if (!adj.preset_name.empty()) {
    ss << "   <dc:title>\n    <rdf:Alt>\n     <rdf:li xml:lang=\"x-default\">"
       << xml_escape(adj.preset_name) << "</rdf:li>\n    </rdf:Alt>\n   </dc:title>\n";
  }
  if (!adj.preset_author.empty()) {
    ss << "   <dc:creator>\n    <rdf:Seq>\n     <rdf:li>"
       << xml_escape(adj.preset_author) << "</rdf:li>\n    </rdf:Seq>\n   </dc:creator>\n";
  }
  if (!adj.preset_description.empty()) {
    ss << "   <dc:description>\n    <rdf:Alt>\n     <rdf:li xml:lang=\"x-default\">"
       << xml_escape(adj.preset_description) << "</rdf:li>\n    </rdf:Alt>\n   </dc:description>\n";
  }

  // Helper lambda
  auto emit = [&](const char* name, const std::optional<float>& val) {
    if (val) ss << "   <crs:" << name << ">" << fmt_float(*val) << "</crs:" << name << ">\n";
  };

  // Basic
  emit("Exposure2012", adj.exposure);
  emit("Contrast2012", adj.contrast);
  emit("Highlights2012", adj.highlights);
  emit("Shadows2012", adj.shadows);
  emit("Whites2012", adj.whites);
  emit("Blacks2012", adj.blacks);

  // White Balance
  emit("Temperature", adj.temperature);
  emit("Tint", adj.tint);

  // Presence
  emit("Vibrance", adj.vibrance);
  emit("Saturation", adj.saturation);
  emit("Clarity2012", adj.clarity);
  emit("Dehaze", adj.dehaze);

  // Detail
  emit("Sharpness", adj.sharpening);
  emit("SharpnessRadius", adj.sharpening_radius);
  emit("SharpnessDetail", adj.sharpening_detail);
  emit("SharpnessMasking", adj.sharpening_masking);
  emit("LuminanceNoiseReduction", adj.noise_reduction);
  emit("LuminanceNoiseReductionDetail", adj.noise_reduction_detail);
  emit("ColorNoiseReduction", adj.color_noise_reduction);

  // Tone Curve
  if (adj.tone_curve) {
    ss << "   <crs:ToneCurvePV2012>";
    const auto& tc = *adj.tone_curve;
    size_t max_pts = std::max({tc.red.size(), tc.green.size(), tc.blue.size()});
    max_pts = std::max(max_pts, size_t(4));
    for (size_t i = 0; i < max_pts; ++i) {
      if (i > 0) ss << ", ";
      if (i < tc.red.size()) ss << fmt_float(tc.red[i].input) << ", " << fmt_float(tc.red[i].output);
      else ss << "0.000000, 0.000000";
    }
    for (size_t i = 0; i < max_pts; ++i) {
      ss << ", ";
      if (i < tc.green.size()) ss << fmt_float(tc.green[i].input) << ", " << fmt_float(tc.green[i].output);
      else ss << "0.000000, 0.000000";
    }
    for (size_t i = 0; i < max_pts; ++i) {
      ss << ", ";
      if (i < tc.blue.size()) ss << fmt_float(tc.blue[i].input) << ", " << fmt_float(tc.blue[i].output);
      else ss << "0.000000, 0.000000";
    }
    ss << "</crs:ToneCurvePV2012>\n";
  }

  // HSL
  static constexpr const char* hsl_hues[] = {"Red", "Orange", "Yellow", "Green",
                                              "Aqua", "Blue", "Purple", "Magenta"};
  for (const auto& hsl : adj.hsl_adjustments) {
    int idx = -1;
    if (hsl.hue == 0) idx = 0;
    else if (hsl.hue == 30) idx = 1;
    else if (hsl.hue == 60) idx = 2;
    else if (hsl.hue == 120) idx = 3;
    else if (hsl.hue == 180) idx = 4;
    else if (hsl.hue == 240) idx = 5;
    else if (hsl.hue == 270) idx = 6;
    else if (hsl.hue == 300) idx = 7;
    if (idx < 0) continue;

    ss << "   <crs:HueAdjustment" << hsl_hues[idx] << ">" << fmt_float(hsl.hue_shift)
       << "</crs:HueAdjustment" << hsl_hues[idx] << ">\n";
    ss << "   <crs:SaturationAdjustment" << hsl_hues[idx] << ">" << fmt_float(hsl.saturation)
       << "</crs:SaturationAdjustment" << hsl_hues[idx] << ">\n";
    ss << "   <crs:LuminanceAdjustment" << hsl_hues[idx] << ">" << fmt_float(hsl.luminance)
       << "</crs:LuminanceAdjustment" << hsl_hues[idx] << ">\n";
  }

  // Split Toning
  if (adj.split_toning) {
    const auto& st = *adj.split_toning;
    ss << "   <crs:SplitToningHighlightHue>" << fmt_float(st.highlight_hue)
       << "</crs:SplitToningHighlightHue>\n";
    ss << "   <crs:SplitToningHighlightSaturation>" << fmt_float(st.highlight_saturation)
       << "</crs:SplitToningHighlightSaturation>\n";
    ss << "   <crs:SplitToningShadowHue>" << fmt_float(st.shadow_hue)
       << "</crs:SplitToningShadowHue>\n";
    ss << "   <crs:SplitToningShadowSaturation>" << fmt_float(st.shadow_saturation)
       << "</crs:SplitToningShadowSaturation>\n";
    ss << "   <crs:SplitToningBalance>" << fmt_float(st.balance)
       << "</crs:SplitToningBalance>\n";
  }

  // Lens Correction
  if (adj.lens_correction) {
    const auto& lc = *adj.lens_correction;
    ss << "   <crs:LensProfileEnable>" << (lc.enable_profile_corrections ? "1" : "0")
       << "</crs:LensProfileEnable>\n";
    ss << "   <crs:RemoveChromaticAberration>" << (lc.remove_chromatic_aberration ? "1" : "0")
       << "</crs:RemoveChromaticAberration>\n";
    ss << "   <crs:AutoLateralCA>" << (lc.remove_chromatic_aberration ? "1" : "0")
       << "</crs:AutoLateralCA>\n";
    if (lc.distortion != 0.0f) {
      ss << "   <crs:LensManualDistortionAmount>" << fmt_float(lc.distortion)
         << "</crs:LensManualDistortionAmount>\n";
    }
    if (lc.vertical != 0.0f) {
      ss << "   <crs:PerspectiveVertical>" << fmt_float(lc.vertical)
         << "</crs:PerspectiveVertical>\n";
    }
    if (lc.horizontal != 0.0f) {
      ss << "   <crs:PerspectiveHorizontal>" << fmt_float(lc.horizontal)
         << "</crs:PerspectiveHorizontal>\n";
    }
    if (lc.rotate != 0.0f) {
      ss << "   <crs:PerspectiveRotate>" << fmt_float(lc.rotate)
         << "</crs:PerspectiveRotate>\n";
    }
    if (lc.scale != 100.0f) {
      ss << "   <crs:PerspectiveScale>" << fmt_float(lc.scale)
         << "</crs:PerspectiveScale>\n";
    }
    if (lc.aspect != 0.0f) {
      ss << "   <crs:PerspectiveAspect>" << fmt_float(lc.aspect)
         << "</crs:PerspectiveAspect>\n";
    }
  }

  // Vignette
  if (adj.vignette) {
    const auto& vig = *adj.vignette;
    ss << "   <crs:PostCropVignetteAmount>" << fmt_float(vig.amount)
       << "</crs:PostCropVignetteAmount>\n";
    ss << "   <crs:PostCropVignetteMidpoint>" << fmt_float(vig.midpoint)
       << "</crs:PostCropVignetteMidpoint>\n";
    ss << "   <crs:PostCropVignetteRoundness>" << fmt_float(vig.roundness)
       << "</crs:PostCropVignetteRoundness>\n";
    ss << "   <crs:PostCropVignetteFeather>" << fmt_float(vig.feather)
       << "</crs:PostCropVignetteFeather>\n";
    ss << "   <crs:PostCropVignetteHighlightContrast>" << fmt_float(vig.highlights)
       << "</crs:PostCropVignetteHighlightContrast>\n";
  }

  // Grain
  if (adj.grain) {
    const auto& gr = *adj.grain;
    ss << "   <crs:GrainAmount>" << fmt_float(gr.amount) << "</crs:GrainAmount>\n";
    ss << "   <crs:GrainSize>" << fmt_float(gr.size) << "</crs:GrainSize>\n";
    ss << "   <crs:GrainFrequency>" << fmt_float(gr.roughness) << "</crs:GrainFrequency>\n";
  }

  // Color Calibration
  if (adj.color_calibration) {
    const auto& cc = *adj.color_calibration;
    ss << "   <crs:RedHue>" << fmt_float(cc.red_hue) << "</crs:RedHue>\n";
    ss << "   <crs:RedSaturation>" << fmt_float(cc.red_saturation) << "</crs:RedSaturation>\n";
    ss << "   <crs:GreenHue>" << fmt_float(cc.green_hue) << "</crs:GreenHue>\n";
    ss << "   <crs:GreenSaturation>" << fmt_float(cc.green_saturation) << "</crs:GreenSaturation>\n";
    ss << "   <crs:BlueHue>" << fmt_float(cc.blue_hue) << "</crs:BlueHue>\n";
    ss << "   <crs:BlueSaturation>" << fmt_float(cc.blue_saturation) << "</crs:BlueSaturation>\n";
    ss << "   <crs:ShadowTint>" << fmt_float(cc.shadow_tint) << "</crs:ShadowTint>\n";
  }

  ss << "  </rdf:Description>\n";
  ss << " </rdf:RDF>\n";
  ss << "</x:xmpmeta>\n";

  return ss.str();
}

}  // namespace preset
}  // namespace alcedo