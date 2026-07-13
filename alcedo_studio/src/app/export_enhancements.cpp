//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/export_enhancements.hpp"

#include <algorithm>
#include <array>
#include <cctype>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <fstream>
#include <iomanip>
#include <sstream>
#include <utility>

namespace alcedo {
namespace export_enh {

// ─────────────────────────────────────────────────────────────────────
// (a) Format extension helpers
// ─────────────────────────────────────────────────────────────────────

auto ToString(ExtendedFormat fmt) -> std::string_view {
  switch (fmt) {
    case ExtendedFormat::JXL:      return "JXL";
    case ExtendedFormat::AVIF:     return "AVIF";
    case ExtendedFormat::EXR:      return "EXR";
    case ExtendedFormat::QOI:      return "QOI";
    case ExtendedFormat::CUBE_LUT: return "CUBE_LUT";
  }
  return "UNKNOWN";
}

auto FileExtensionForFormat(ExtendedFormat fmt) -> std::string_view {
  switch (fmt) {
    case ExtendedFormat::JXL:      return ".jxl";
    case ExtendedFormat::AVIF:     return ".avif";
    case ExtendedFormat::EXR:      return ".exr";
    case ExtendedFormat::QOI:      return ".qoi";
    case ExtendedFormat::CUBE_LUT: return ".cube";
  }
  return "";
}

auto ToString(PresetCategory cat) -> std::string_view {
  switch (cat) {
    case PresetCategory::FULL_RESOLUTION: return "full_resolution";
    case PresetCategory::WEB:             return "web";
    case PresetCategory::SOCIAL_MEDIA:    return "social_media";
    case PresetCategory::PRINT:           return "print";
  }
  return "unknown";
}

auto ToString(MetadataPolicy policy) -> std::string_view {
  switch (policy) {
    case MetadataPolicy::KEEP_ALL:         return "keep_all";
    case MetadataPolicy::STRIP_ALL:        return "strip_all";
    case MetadataPolicy::COPYRIGHT_ONLY:   return "copyright_only";
    case MetadataPolicy::CAMERA_INFO_ONLY: return "camera_info_only";
    case MetadataPolicy::CUSTOM_WHITELIST: return "custom_whitelist";
    case MetadataPolicy::CUSTOM_BLACKLIST: return "custom_blacklist";
  }
  return "unknown";
}

auto ToString(MetadataField field) -> std::string_view {
  switch (field) {
    case MetadataField::MAKE:            return "make";
    case MetadataField::MODEL:           return "model";
    case MetadataField::LENS:            return "lens";
    case MetadataField::LENS_MAKE:       return "lens_make";
    case MetadataField::DATE_TIME:       return "date_time";
    case MetadataField::APERTURE:        return "aperture";
    case MetadataField::SHUTTER_SPEED:   return "shutter_speed";
    case MetadataField::ISO:             return "iso";
    case MetadataField::FOCAL_LENGTH:    return "focal_length";
    case MetadataField::FOCAL_LENGTH_35MM: return "focal_length_35mm";
    case MetadataField::FOCUS_DISTANCE:  return "focus_distance";
    case MetadataField::RATING:          return "rating";
    case MetadataField::LABEL:           return "label";
    case MetadataField::GPS_LATITUDE:    return "gps_latitude";
    case MetadataField::GPS_LONGITUDE:   return "gps_longitude";
    case MetadataField::GPS_ALTITUDE:    return "gps_altitude";
    case MetadataField::COPYRIGHT:       return "copyright";
    case MetadataField::ARTIST:          return "artist";
    case MetadataField::DESCRIPTION:     return "description";
    case MetadataField::KEYWORDS:        return "keywords";
    case MetadataField::IMAGE_WIDTH:     return "image_width";
    case MetadataField::IMAGE_HEIGHT:    return "image_height";
    case MetadataField::COLOR_SPACE:     return "color_space";
    case MetadataField::ORIENTATION:     return "orientation";
    case MetadataField::SOFTWARE:        return "software";
  }
  return "unknown";
}

auto ToString(ResizeMode mode) -> std::string_view {
  switch (mode) {
    case ResizeMode::LONG_EDGE:   return "long_edge";
    case ResizeMode::SHORT_EDGE:  return "short_edge";
    case ResizeMode::WIDTH:       return "width";
    case ResizeMode::HEIGHT:      return "height";
    case ResizeMode::PERCENTAGE:  return "percentage";
    case ResizeMode::MEGAPIXEL:   return "megapixel";
  }
  return "unknown";
}

auto ToString(SharpenTarget target) -> std::string_view {
  switch (target) {
    case SharpenTarget::SCREEN: return "screen";
    case SharpenTarget::PRINT:  return "print";
  }
  return "unknown";
}

// ─────────────────────────────────────────────────────────────────────
// (b) Export naming templates
// ─────────────────────────────────────────────────────────────────────

namespace {

// Token → string mapping table
auto TokenToString(NamingToken token) -> std::string_view {
  switch (token) {
    case NamingToken::DATE:       return "date";
    case NamingToken::TIME:       return "time";
    case NamingToken::CAMERA:     return "camera";
    case NamingToken::LENS:       return "lens";
    case NamingToken::ISO:        return "iso";
    case NamingToken::APERTURE:   return "aperture";
    case NamingToken::SHUTTER:    return "shutter";
    case NamingToken::FOCAL:      return "focal";
    case NamingToken::INDEX:      return "index";
    case NamingToken::RATING:     return "rating";
    case NamingToken::LABEL:      return "label";
    case NamingToken::TAG:        return "tag";
    case NamingToken::DIMENSIONS: return "dimensions";
    case NamingToken::ORIGINAL:   return "original";
  }
  return "";
}

auto ResolveTokenValue(NamingToken token, const NamingContext& ctx) -> std::string {
  switch (token) {
    case NamingToken::DATE: {
      if (!ctx.date_.empty()) return ctx.date_;
      return "YYYY-MM-DD";
    }
    case NamingToken::TIME: {
      if (!ctx.time_.empty()) return ctx.time_;
      return "HH-MM-SS";
    }
    case NamingToken::CAMERA: {
      if (!ctx.camera_.empty()) return ctx.camera_;
      return "UnknownCamera";
    }
    case NamingToken::LENS: {
      if (!ctx.lens_.empty()) return ctx.lens_;
      return "UnknownLens";
    }
    case NamingToken::ISO: {
      if (ctx.iso_ > 0) return std::to_string(ctx.iso_);
      return "ISO0";
    }
    case NamingToken::APERTURE: {
      if (ctx.aperture_ > 0.0f) {
        std::ostringstream oss;
        oss << "f" << std::fixed << std::setprecision(1) << ctx.aperture_;
        return oss.str();
      }
      return "f0.0";
    }
    case NamingToken::SHUTTER: {
      if (!ctx.shutter_.empty()) return ctx.shutter_;
      return "0s";
    }
    case NamingToken::FOCAL: {
      if (ctx.focal_ > 0.0f) {
        std::ostringstream oss;
        oss << static_cast<int>(std::round(ctx.focal_)) << "mm";
        return oss.str();
      }
      return "0mm";
    }
    case NamingToken::INDEX: {
      return std::to_string(ctx.index_);
    }
    case NamingToken::RATING: {
      if (ctx.rating_ > 0) return std::to_string(ctx.rating_);
      return "0";
    }
    case NamingToken::LABEL: {
      if (!ctx.label_.empty()) return ctx.label_;
      return "NoLabel";
    }
    case NamingToken::TAG: {
      if (!ctx.tag_.empty()) return ctx.tag_;
      return "";
    }
    case NamingToken::DIMENSIONS: {
      if (ctx.width_ > 0 && ctx.height_ > 0) {
        return std::to_string(ctx.width_) + "x" + std::to_string(ctx.height_);
      }
      return "0x0";
    }
    case NamingToken::ORIGINAL: {
      if (!ctx.original_.empty()) return ctx.original_;
      return "Untitled";
    }
  }
  return "";
}

// Sanitize a string for use in filename: replace path separators, etc.
auto SanitizeFilenameComponent(const std::string& input) -> std::string {
  std::string result = input;
  const std::string illegal_chars = R"(/\:*?"<>|)";
  for (auto& ch : result) {
    if (illegal_chars.find(ch) != std::string::npos) {
      ch = '_';
    }
  }
  return result;
}

}  // namespace

auto ExpandNamingTokens(const std::string& pattern, const NamingContext& ctx) -> std::string {
  std::string result = pattern;

  // Iterate over all token types and replace
  for (int t = 0; t <= static_cast<int>(NamingToken::ORIGINAL); ++t) {
    auto token = static_cast<NamingToken>(t);
    std::string token_str = "{" + std::string(TokenToString(token)) + "}";
    std::string replacement = ResolveTokenValue(token, ctx);

    // Find and replace all occurrences
    size_t pos = 0;
    while ((pos = result.find(token_str, pos)) != std::string::npos) {
      result.replace(pos, token_str.length(), replacement);
      pos += replacement.length();
    }
  }

  // Handle {index:N} padded format
  {
    size_t pos = 0;
    while ((pos = result.find("{index:", pos)) != std::string::npos) {
      size_t end = result.find('}', pos);
      if (end == std::string::npos) break;
      std::string width_str = result.substr(pos + 7, end - pos - 7);
      int width = std::stoi(width_str);
      std::ostringstream oss;
      oss << std::setw(width) << std::setfill('0') << ctx.index_;
      result.replace(pos, end - pos + 1, oss.str());
      pos += oss.str().length();
    }
  }

  return SanitizeFilenameComponent(result);
}

auto ResolveNamingTemplate(const std::string& pattern, const NamingContext& ctx) -> std::string {
  return ExpandNamingTokens(pattern, ctx);
}

auto ResolveSubfolder(const std::string& subfolder_pattern, const NamingContext& ctx,
                      const std::filesystem::path& base_export_dir)
    -> std::filesystem::path {
  if (subfolder_pattern.empty()) {
    return base_export_dir;
  }
  auto resolved = ExpandNamingTokens(subfolder_pattern, ctx);
  if (resolved.empty()) {
    return base_export_dir;
  }
  return base_export_dir / resolved;
}

// ─────────────────────────────────────────────────────────────────────
// (c) Export presets
// ─────────────────────────────────────────────────────────────────────

auto PresetRegistry::AddPreset(const ExportPreset& preset) -> bool {
  if (preset.name_.empty()) return false;
  presets_[preset.name_] = preset;
  return true;
}

auto PresetRegistry::RemovePreset(const std::string& name) -> bool {
  return presets_.erase(name) > 0;
}

auto PresetRegistry::GetPreset(const std::string& name) const -> std::optional<ExportPreset> {
  auto it = presets_.find(name);
  if (it != presets_.end()) {
    return it->second;
  }
  return std::nullopt;
}

auto PresetRegistry::GetAllPresets() const -> const std::map<std::string, ExportPreset>& {
  return presets_;
}

auto PresetRegistry::GetAllPresetsByCategory(PresetCategory category) const
    -> std::vector<ExportPreset> {
  std::vector<ExportPreset> result;
  for (const auto& [name, preset] : presets_) {
    if (preset.category_ == category) {
      result.push_back(preset);
    }
  }
  return result;
}

auto PresetRegistry::GetDefaultPresetForFormat(ExtendedFormat format) const
    -> std::optional<ExportPreset> {
  // Search for default preset matching the format
  for (const auto& [name, preset] : presets_) {
    if (preset.format_ == format && preset.name_.find("Default") != std::string::npos) {
      return preset;
    }
  }
  return std::nullopt;
}

namespace {

auto EscapeJsonString(const std::string& s) -> std::string {
  std::string result;
  result.reserve(s.size() + 2);
  result += '"';
  for (char c : s) {
    switch (c) {
      case '"':  result += "\\\""; break;
      case '\\': result += "\\\\"; break;
      case '\n': result += "\\n";  break;
      case '\r': result += "\\r";  break;
      case '\t': result += "\\t";  break;
      default:   result += c;      break;
    }
  }
  result += '"';
  return result;
}

auto PresetToJson(const ExportPreset& p) -> std::string {
  std::ostringstream oss;
  oss << "{";
  oss << "\"name\":" << EscapeJsonString(p.name_) << ",";
  oss << "\"category\":\"" << ToString(p.category_) << "\",";
  oss << "\"format\":\"" << ToString(p.format_) << "\",";
  oss << "\"quality\":" << p.quality_ << ",";
  oss << "\"resize_enabled\":" << (p.resize_enabled_ ? "true" : "false") << ",";
  oss << "\"long_edge_px\":" << p.long_edge_px_ << ",";
  oss << "\"short_edge_px\":" << p.short_edge_px_ << ",";
  oss << "\"max_width_px\":" << p.max_width_px_ << ",";
  oss << "\"max_height_px\":" << p.max_height_px_ << ",";
  oss << "\"percentage\":" << p.percentage_ << ",";
  oss << "\"megapixel_limit\":" << p.megapixel_limit_ << ",";
  oss << "\"dpi\":" << p.dpi_ << ",";
  oss << "\"output_sharpen\":" << (p.output_sharpen_ ? "true" : "false") << ",";
  oss << "\"sharpen_amount\":" << p.sharpen_amount_ << ",";
  oss << "\"sharpen_radius\":" << p.sharpen_radius_ << ",";
  oss << "\"sharpen_threshold\":" << p.sharpen_threshold_ << ",";
  oss << "\"sharpen_for_screen\":" << (p.sharpen_for_screen_ ? "true" : "false");
  oss << "}";
  return oss.str();
}

// Minimal JSON parser for preset format
auto ParseJsonBool(const std::string& json, const std::string& key, bool& val) -> void {
  std::string search = "\"" + key + "\":";
  auto pos = json.find(search);
  if (pos == std::string::npos) return;
  pos += search.length();
  while (pos < json.size() && (json[pos] == ' ' || json[pos] == '\t')) ++pos;
  if (pos < json.size() && json[pos] == 't') val = true;
  else if (pos < json.size() && json[pos] == 'f') val = false;
}

auto ParseJsonInt(const std::string& json, const std::string& key, int& val) -> void {
  std::string search = "\"" + key + "\":";
  auto pos = json.find(search);
  if (pos == std::string::npos) return;
  pos += search.length();
  while (pos < json.size() && (json[pos] == ' ' || json[pos] == '\t')) ++pos;
  val = std::stoi(json.substr(pos));
}

auto ParseJsonFloat(const std::string& json, const std::string& key, float& val) -> void {
  std::string search = "\"" + key + "\":";
  auto pos = json.find(search);
  if (pos == std::string::npos) return;
  pos += search.length();
  while (pos < json.size() && (json[pos] == ' ' || json[pos] == '\t')) ++pos;
  val = std::stof(json.substr(pos));
}

auto ParseJsonString(const std::string& json, const std::string& key, std::string& val) -> void {
  std::string search = "\"" + key + "\":";
  auto pos = json.find(search);
  if (pos == std::string::npos) return;
  pos += search.length();
  while (pos < json.size() && (json[pos] == ' ' || json[pos] == '\t')) ++pos;
  if (pos < json.size() && json[pos] == '"') {
    ++pos;
    auto end = json.find('"', pos);
    if (end != std::string::npos) {
      val = json.substr(pos, end - pos);
    }
  }
}

auto ParsePresetCategory(const std::string& s) -> PresetCategory {
  if (s == "full_resolution") return PresetCategory::FULL_RESOLUTION;
  if (s == "web")             return PresetCategory::WEB;
  if (s == "social_media")    return PresetCategory::SOCIAL_MEDIA;
  if (s == "print")           return PresetCategory::PRINT;
  return PresetCategory::FULL_RESOLUTION;
}

auto ParseExtendedFormat(const std::string& s) -> ExtendedFormat {
  if (s == "JXL")      return ExtendedFormat::JXL;
  if (s == "AVIF")     return ExtendedFormat::AVIF;
  if (s == "EXR")      return ExtendedFormat::EXR;
  if (s == "QOI")      return ExtendedFormat::QOI;
  if (s == "CUBE_LUT") return ExtendedFormat::CUBE_LUT;
  return ExtendedFormat::JXL;
}

auto JsonToPreset(const std::string& json) -> ExportPreset {
  ExportPreset p;
  std::string s;
  ParseJsonString(json, "name", p.name_);
  ParseJsonString(json, "category", s);
  p.category_ = ParsePresetCategory(s);
  ParseJsonString(json, "format", s);
  p.format_ = ParseExtendedFormat(s);
  ParseJsonInt(json, "quality", p.quality_);
  ParseJsonBool(json, "resize_enabled", p.resize_enabled_);
  ParseJsonInt(json, "long_edge_px", p.long_edge_px_);
  ParseJsonInt(json, "short_edge_px", p.short_edge_px_);
  ParseJsonInt(json, "max_width_px", p.max_width_px_);
  ParseJsonInt(json, "max_height_px", p.max_height_px_);
  ParseJsonFloat(json, "percentage", p.percentage_);
  ParseJsonFloat(json, "megapixel_limit", p.megapixel_limit_);
  ParseJsonInt(json, "dpi", p.dpi_);
  ParseJsonBool(json, "output_sharpen", p.output_sharpen_);
  ParseJsonFloat(json, "sharpen_amount", p.sharpen_amount_);
  ParseJsonFloat(json, "sharpen_radius", p.sharpen_radius_);
  ParseJsonFloat(json, "sharpen_threshold", p.sharpen_threshold_);
  ParseJsonBool(json, "sharpen_for_screen", p.sharpen_for_screen_);
  return p;
}

}  // namespace

auto PresetRegistry::ToJsonString() const -> std::string {
  std::ostringstream oss;
  oss << "{\n";
  oss << "  \"version\": 1,\n";
  oss << "  \"presets\": [\n";
  bool first = true;
  for (const auto& [name, preset] : presets_) {
    if (!first) oss << ",\n";
    oss << "    " << PresetToJson(preset);
    first = false;
  }
  oss << "\n  ]\n";
  oss << "}\n";
  return oss.str();
}

auto PresetRegistry::FromJsonString(const std::string& json) -> bool {
  // Find presets array
  auto array_start = json.find("\"presets\":");
  if (array_start == std::string::npos) return false;
  array_start = json.find('[', array_start);
  if (array_start == std::string::npos) return false;

  presets_.clear();

  size_t pos = array_start + 1;
  while (pos < json.size()) {
    // Skip whitespace
    while (pos < json.size() && (json[pos] == ' ' || json[pos] == '\t' ||
                                  json[pos] == '\n' || json[pos] == '\r')) {
      ++pos;
    }
    if (pos >= json.size() || json[pos] == ']') break;

    // Find the start of a JSON object
    if (json[pos] == ',') {
      ++pos;
      continue;
    }
    if (json[pos] != '{') { ++pos; continue; }

    // Find matching closing brace
    int depth = 1;
    size_t obj_start = pos;
    ++pos;
    while (pos < json.size() && depth > 0) {
      if (json[pos] == '{') ++depth;
      else if (json[pos] == '}') --depth;
      ++pos;
    }
    std::string obj_json = json.substr(obj_start, pos - obj_start);

    auto preset = JsonToPreset(obj_json);
    if (!preset.name_.empty()) {
      presets_[preset.name_] = std::move(preset);
    }

    // Skip comma
    while (pos < json.size() && (json[pos] == ' ' || json[pos] == '\t' ||
                                  json[pos] == '\n' || json[pos] == '\r')) {
      ++pos;
    }
    if (pos < json.size() && json[pos] == ',') ++pos;
  }
  return true;
}

auto PresetRegistry::SaveToFile(const std::filesystem::path& filepath) const -> bool {
  std::ofstream file(filepath, std::ios::out | std::ios::trunc);
  if (!file.is_open()) return false;
  file << ToJsonString();
  return file.good();
}

auto PresetRegistry::LoadFromFile(const std::filesystem::path& filepath) -> bool {
  std::ifstream file(filepath, std::ios::in);
  if (!file.is_open()) return false;
  std::string content((std::istreambuf_iterator<char>(file)),
                       std::istreambuf_iterator<char>());
  if (content.empty()) return false;
  return FromJsonString(content);
}

void PresetRegistry::PopulateDefaults() {
  // Full resolution presets
  for (auto fmt : {ExtendedFormat::JXL, ExtendedFormat::AVIF, ExtendedFormat::EXR,
                   ExtendedFormat::QOI, ExtendedFormat::CUBE_LUT}) {
    auto preset = MakeFullResolutionPreset(fmt);
    presets_[preset.name_] = std::move(preset);
  }
  // Web presets
  for (auto fmt : {ExtendedFormat::JXL, ExtendedFormat::AVIF, ExtendedFormat::QOI}) {
    auto preset = MakeWebPreset(fmt);
    presets_[preset.name_] = std::move(preset);
  }
  // Social media presets
  for (auto fmt : {ExtendedFormat::JXL, ExtendedFormat::AVIF}) {
    auto preset = MakeSocialMediaPreset(fmt);
    presets_[preset.name_] = std::move(preset);
  }
  // Print presets
  for (auto fmt : {ExtendedFormat::JXL, ExtendedFormat::AVIF, ExtendedFormat::EXR}) {
    auto preset = MakePrintPreset(fmt);
    presets_[preset.name_] = std::move(preset);
  }
}

// ─────────────────────────────────────────────────────────────────────
// (d) Metadata control
// ─────────────────────────────────────────────────────────────────────

auto ShouldKeepField(MetadataField field, const MetadataControlConfig& config) -> bool {
  // GPS removal
  if (config.remove_gps_) {
    switch (field) {
      case MetadataField::GPS_LATITUDE:
      case MetadataField::GPS_LONGITUDE:
      case MetadataField::GPS_ALTITUDE:
        return false;
      default:
        break;
    }
  }

  switch (config.policy_) {
    case MetadataPolicy::KEEP_ALL:
      return true;

    case MetadataPolicy::STRIP_ALL:
      // Still allow watermark metadata if enabled
      if (config.add_watermark_metadata_ &&
          (field == MetadataField::COPYRIGHT || field == MetadataField::ARTIST ||
           field == MetadataField::DESCRIPTION)) {
        return true;
      }
      return false;

    case MetadataPolicy::COPYRIGHT_ONLY: {
      switch (field) {
        case MetadataField::COPYRIGHT:
        case MetadataField::ARTIST:
        case MetadataField::DESCRIPTION:
          return true;
        default:
          return false;
      }
    }

    case MetadataPolicy::CAMERA_INFO_ONLY: {
      switch (field) {
        case MetadataField::MAKE:
        case MetadataField::MODEL:
        case MetadataField::LENS:
        case MetadataField::LENS_MAKE:
        case MetadataField::APERTURE:
        case MetadataField::SHUTTER_SPEED:
        case MetadataField::ISO:
        case MetadataField::FOCAL_LENGTH:
        case MetadataField::FOCAL_LENGTH_35MM:
        case MetadataField::FOCUS_DISTANCE:
        case MetadataField::DATE_TIME:
        case MetadataField::IMAGE_WIDTH:
        case MetadataField::IMAGE_HEIGHT:
        case MetadataField::ORIENTATION:
          return true;
        default:
          return false;
      }
    }

    case MetadataPolicy::CUSTOM_WHITELIST: {
      return config.whitelist_.count(field) > 0;
    }

    case MetadataPolicy::CUSTOM_BLACKLIST: {
      return config.blacklist_.count(field) == 0;
    }
  }

  return true;
}

// ─────────────────────────────────────────────────────────────────────
// (e) Image resizing
// ─────────────────────────────────────────────────────────────────────

auto ComputeResizeDimensions(int src_width, int src_height, const ResizeConfig& config)
    -> ImageDimensions {
  if (src_width <= 0 || src_height <= 0) {
    return {0, 0};
  }

  ImageDimensions result{src_width, src_height};

  switch (config.mode_) {
    case ResizeMode::LONG_EDGE: {
      if (config.long_edge_px_ <= 0) break;
      int long_edge = std::max(src_width, src_height);
      if (long_edge <= config.long_edge_px_) break;
      float scale = static_cast<float>(config.long_edge_px_) / static_cast<float>(long_edge);
      result.width_  = static_cast<int>(std::round(src_width * scale));
      result.height_ = static_cast<int>(std::round(src_height * scale));
      break;
    }

    case ResizeMode::SHORT_EDGE: {
      if (config.short_edge_px_ <= 0) break;
      int short_edge = std::min(src_width, src_height);
      if (short_edge <= config.short_edge_px_) break;
      float scale = static_cast<float>(config.short_edge_px_) / static_cast<float>(short_edge);
      result.width_  = static_cast<int>(std::round(src_width * scale));
      result.height_ = static_cast<int>(std::round(src_height * scale));
      break;
    }

    case ResizeMode::WIDTH: {
      if (config.max_width_px_ <= 0) break;
      if (src_width <= config.max_width_px_ && config.keep_aspect_ratio_) break;
      float scale = static_cast<float>(config.max_width_px_) / static_cast<float>(src_width);
      result.width_  = config.max_width_px_;
      if (config.keep_aspect_ratio_) {
        result.height_ = static_cast<int>(std::round(src_height * scale));
      }
      break;
    }

    case ResizeMode::HEIGHT: {
      if (config.max_height_px_ <= 0) break;
      if (src_height <= config.max_height_px_ && config.keep_aspect_ratio_) break;
      float scale = static_cast<float>(config.max_height_px_) / static_cast<float>(src_height);
      result.height_ = config.max_height_px_;
      if (config.keep_aspect_ratio_) {
        result.width_ = static_cast<int>(std::round(src_width * scale));
      }
      break;
    }

    case ResizeMode::PERCENTAGE: {
      if (config.percentage_ <= 0.0f || config.percentage_ >= 100.0f) break;
      float scale = config.percentage_ / 100.0f;
      result.width_  = static_cast<int>(std::round(src_width * scale));
      result.height_ = static_cast<int>(std::round(src_height * scale));
      break;
    }

    case ResizeMode::MEGAPIXEL: {
      if (config.megapixel_limit_ <= 0.0f) break;
      float current_mp = static_cast<float>(src_width * src_height) / 1'000'000.0f;
      if (current_mp <= config.megapixel_limit_) break;
      float scale = std::sqrt(config.megapixel_limit_ / current_mp);
      result.width_  = static_cast<int>(std::round(src_width * scale));
      result.height_ = static_cast<int>(std::round(src_height * scale));
      break;
    }
  }

  // Ensure minimum dimensions of 1
  if (result.width_ < 1) result.width_ = 1;
  if (result.height_ < 1) result.height_ = 1;

  return result;
}

// ─────────────────────────────────────────────────────────────────────
// LUT export: .cube format generation
// ─────────────────────────────────────────────────────────────────────

namespace {

auto FormatFloatForCube(float value) -> std::string {
  // .cube format uses space-separated float values with 6 decimal places
  char buf[32];
  std::snprintf(buf, sizeof(buf), "%.6f", value);
  return std::string(buf);
}

}  // namespace

auto GenerateCubeLutContent(const Lut3DTable& table, const CubeLutOptions& options) -> std::string {
  std::ostringstream oss;

  // Header
  oss << "TITLE \"" << options.title_ << "\"\n";
  oss << "DOMAIN_MIN " << FormatFloatForCube(options.domain_min_[0]) << " "
      << FormatFloatForCube(options.domain_min_[1]) << " "
      << FormatFloatForCube(options.domain_min_[2]) << "\n";
  oss << "DOMAIN_MAX " << FormatFloatForCube(options.domain_max_[0]) << " "
      << FormatFloatForCube(options.domain_max_[1]) << " "
      << FormatFloatForCube(options.domain_max_[2]) << "\n";

  int lut_size = table.size_ > 0 ? table.size_ : options.lut_size_;
  oss << "LUT_3D_SIZE " << lut_size << "\n";

  int expected_size = lut_size * lut_size * lut_size * 3;
  int data_size = static_cast<int>(table.data_.size());

  // Data lines: row-major, R varies fastest, then G, then B
  oss << "\n";
  int idx = 0;
  for (int b = 0; b < lut_size; ++b) {
    for (int g = 0; g < lut_size; ++g) {
      for (int r = 0; r < lut_size; ++r) {
        if (idx + 2 < data_size) {
          oss << FormatFloatForCube(table.data_[idx]) << " "
              << FormatFloatForCube(table.data_[idx + 1]) << " "
              << FormatFloatForCube(table.data_[idx + 2]) << "\n";
        } else {
          // Identity grid fallback: normalized R, G, B
          float nr = static_cast<float>(r) / static_cast<float>(lut_size - 1);
          float ng = static_cast<float>(g) / static_cast<float>(lut_size - 1);
          float nb = static_cast<float>(b) / static_cast<float>(lut_size - 1);
          oss << FormatFloatForCube(nr) << " "
              << FormatFloatForCube(ng) << " "
              << FormatFloatForCube(nb) << "\n";
        }
        idx += 3;
      }
    }
  }

  return oss.str();
}

auto WriteCubeLutFile(const std::filesystem::path& filepath, const Lut3DTable& table,
                      const CubeLutOptions& options) -> bool {
  std::ofstream file(filepath, std::ios::out | std::ios::trunc);
  if (!file.is_open()) return false;
  std::string content = GenerateCubeLutContent(table, options);
  file << content;
  return file.good();
}

// ─────────────────────────────────────────────────────────────────────
// Preset factory helpers
// ─────────────────────────────────────────────────────────────────────

auto MakeFullResolutionPreset(ExtendedFormat fmt) -> ExportPreset {
  ExportPreset p;
  p.name_     = std::string("Default Full Res - ") + std::string(ToString(fmt));
  p.category_ = PresetCategory::FULL_RESOLUTION;
  p.format_   = fmt;
  p.resize_enabled_ = false;
  p.quality_  = (fmt == ExtendedFormat::AVIF) ? 80 : 95;
  p.dpi_      = 300;
  p.output_sharpen_ = false;

  switch (fmt) {
    case ExtendedFormat::JXL:
      p.jxl_opts_.quality_ = 95;
      break;
    case ExtendedFormat::AVIF:
      p.avif_opts_.quality_ = 80;
      p.avif_opts_.speed_ = 4;
      break;
    case ExtendedFormat::EXR:
      p.exr_opts_.compression_ = ExrEncodeOptions::Compression::PIZ;
      p.exr_opts_.pixel_type_  = ExrEncodeOptions::PixelType::HALF;
      break;
    case ExtendedFormat::QOI:
      break;
    case ExtendedFormat::CUBE_LUT:
      p.cube_lut_opts_.lut_size_ = 33;
      break;
  }
  return p;
}

auto MakeWebPreset(ExtendedFormat fmt) -> ExportPreset {
  ExportPreset p;
  p.name_     = std::string("Default Web - ") + std::string(ToString(fmt));
  p.category_ = PresetCategory::WEB;
  p.format_   = fmt;
  p.resize_enabled_ = true;
  p.long_edge_px_   = 2560;
  p.quality_  = (fmt == ExtendedFormat::AVIF) ? 60 : 80;
  p.dpi_      = 72;
  p.output_sharpen_ = true;
  p.sharpen_amount_ = 0.4f;
  p.sharpen_radius_ = 0.6f;
  p.sharpen_threshold_ = 0.01f;
  p.sharpen_for_screen_ = true;

  switch (fmt) {
    case ExtendedFormat::JXL:
      p.jxl_opts_.quality_ = 80;
      p.jxl_opts_.effort_  = 6;
      break;
    case ExtendedFormat::AVIF:
      p.avif_opts_.quality_ = 60;
      p.avif_opts_.speed_ = 6;
      break;
    case ExtendedFormat::QOI:
      break;
    default:
      break;
  }
  return p;
}

auto MakeSocialMediaPreset(ExtendedFormat fmt) -> ExportPreset {
  ExportPreset p;
  p.name_     = std::string("Default Social Media - ") + std::string(ToString(fmt));
  p.category_ = PresetCategory::SOCIAL_MEDIA;
  p.format_   = fmt;
  p.resize_enabled_ = true;
  p.long_edge_px_   = 2048;
  p.quality_  = (fmt == ExtendedFormat::AVIF) ? 55 : 75;
  p.dpi_      = 72;
  p.output_sharpen_ = true;
  p.sharpen_amount_ = 0.5f;
  p.sharpen_radius_ = 0.7f;
  p.sharpen_threshold_ = 0.02f;
  p.sharpen_for_screen_ = true;

  switch (fmt) {
    case ExtendedFormat::JXL:
      p.jxl_opts_.quality_ = 75;
      p.jxl_opts_.effort_  = 6;
      break;
    case ExtendedFormat::AVIF:
      p.avif_opts_.quality_ = 55;
      p.avif_opts_.speed_ = 7;
      break;
    default:
      break;
  }
  return p;
}

auto MakePrintPreset(ExtendedFormat fmt) -> ExportPreset {
  ExportPreset p;
  p.name_     = std::string("Default Print - ") + std::string(ToString(fmt));
  p.category_ = PresetCategory::PRINT;
  p.format_   = fmt;
  p.resize_enabled_ = false;
  p.quality_  = 100;
  p.dpi_      = 300;
  p.output_sharpen_ = true;
  p.sharpen_amount_ = 0.6f;
  p.sharpen_radius_ = 1.0f;
  p.sharpen_threshold_ = 0.03f;
  p.sharpen_for_screen_ = false;

  switch (fmt) {
    case ExtendedFormat::JXL:
      p.jxl_opts_.quality_ = 100;
      p.jxl_opts_.effort_  = 9;
      break;
    case ExtendedFormat::AVIF:
      p.avif_opts_.quality_ = 100;
      p.avif_opts_.speed_ = 4;
      break;
    case ExtendedFormat::EXR:
      p.exr_opts_.compression_ = ExrEncodeOptions::Compression::ZIP;
      p.exr_opts_.pixel_type_  = ExrEncodeOptions::PixelType::FLOAT;
      break;
    default:
      break;
  }
  return p;
}

}  // namespace export_enh
}  // namespace alcedo