//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <filesystem>
#include <map>
#include <optional>
#include <ostream>
#include <string>
#include <string_view>
#include <unordered_map>
#include <unordered_set>
#include <vector>

namespace alcedo {
namespace export_enh {

// ─────────────────────────────────────────────────────────────────────
// (a) Export format extensions
// ─────────────────────────────────────────────────────────────────────

enum class ExtendedFormat : uint8_t {
  JXL = 0,   // JPEG XL
  AVIF,      // AV1 Image File Format
  EXR,       // OpenEXR (HDR)
  QOI,       // Quite OK Image (fast lossless)
  CUBE_LUT,  // 3D LUT .cube file
};

struct JxlEncodeOptions {
  int  quality_        = 90;   // 0-100, lossy
  bool lossless_       = false;
  int  effort_         = 7;    // 1-9, higher = slower
  bool use_modular_    = false;
};

struct AvifEncodeOptions {
  int  quality_        = 60;   // 0-100, AVIF quality
  int  speed_          = 6;    // 0-10, higher = faster
  bool lossless_       = false;
  int  subsampling_    = 0;    // 0=4:4:4, 1=4:2:2, 2=4:2:0
};

struct ExrEncodeOptions {
  enum class Compression : uint8_t { NONE = 0, RLE, ZIPS, ZIP, PIZ, PXR24, B44, B44A, DWAA, DWAB };
  enum class PixelType : uint8_t { HALF = 0, FLOAT, UINT };

  Compression compression_ = Compression::PIZ;
  PixelType   pixel_type_  = PixelType::HALF;
  bool        dwa_quality_ = false;  // Use DWAA/DWAB quality-based compression
  float       dwa_level_   = 45.0f;  // DWAA/DWAB compression level
};

struct QoiEncodeOptions {
  // QOI is inherently lossless; no quality parameters
  bool embed_icc_ = true;
};

struct CubeLutOptions {
  int          lut_size_        = 33;   // grid points per dimension (typically 17, 33, 65)
  std::string  title_           = "AlcedoStudio LUT";
  float        domain_min_[3]   = {0.0f, 0.0f, 0.0f};
  float        domain_max_[3]   = {1.0f, 1.0f, 1.0f};
  bool         use_1d_prelut_   = false;
};

// ─────────────────────────────────────────────────────────────────────
// (b) Export naming templates
// ─────────────────────────────────────────────────────────────────────

enum class NamingToken : uint8_t {
  DATE,        // {date}      - YYYY-MM-DD
  TIME,        // {time}      - HH-MM-SS
  CAMERA,      // {camera}    - camera make + model
  LENS,        // {lens}      - lens name
  ISO,         // {iso}       - ISO value
  APERTURE,    // {aperture}  - f-number
  SHUTTER,     // {shutter}   - shutter speed
  FOCAL,       // {focal}     - focal length
  INDEX,       // {index}     - sequence number (padded)
  RATING,      // {rating}    - star rating
  LABEL,       // {label}     - color label
  TAG,         // {tag}       - custom tag
  DIMENSIONS,  // {dimensions} - WxH
  ORIGINAL,    // {original}  - original filename stem
};

struct NamingTemplateConfig {
  std::string               pattern_           = "{date}_{time}_{original}";
  int                       sequence_padding_  = 4;     // e.g., 4 → "0001"
  int                       sequence_start_    = 1;
  bool                      create_subfolders_ = false;
  std::string               subfolder_pattern_ = "{date}";  // only used when create_subfolders_=true
};

// Token resolution context: metadata provided per image
struct NamingContext {
  std::string  date_;
  std::string  time_;
  std::string  camera_;
  std::string  lens_;
  uint64_t     iso_            = 0;
  float        aperture_       = 0.0f;
  std::string  shutter_;
  float        focal_          = 0.0f;
  int          index_          = 0;
  int          rating_         = 0;
  std::string  label_;
  std::string  tag_;
  int          width_          = 0;
  int          height_         = 0;
  std::string  original_;
};

// Resolve a pattern string against a naming context, producing a concrete filename stem.
// (extension is appended by the caller)
auto ResolveNamingTemplate(const std::string& pattern, const NamingContext& ctx) -> std::string;

// Replace all tokens in the pattern with their resolved values.
auto ExpandNamingTokens(const std::string& pattern, const NamingContext& ctx) -> std::string;

// Generate a subfolder path from the subfolder_pattern against ctx.
// Returns empty path if pattern is empty or create_subfolders is disabled.
auto ResolveSubfolder(const std::string& subfolder_pattern, const NamingContext& ctx,
                      const std::filesystem::path& base_export_dir)
    -> std::filesystem::path;

// ─────────────────────────────────────────────────────────────────────
// (c) Export presets
// ─────────────────────────────────────────────────────────────────────

enum class PresetCategory : uint8_t {
  FULL_RESOLUTION = 0,
  WEB,
  SOCIAL_MEDIA,
  PRINT,
};

struct ExportPreset {
  std::string           name_{};
  PresetCategory        category_       = PresetCategory::FULL_RESOLUTION;
  ExtendedFormat        format_         = ExtendedFormat::JXL;
  int                   quality_        = 90;
  bool                  resize_enabled_ = false;
  int                   long_edge_px_   = 0;
  int                   short_edge_px_  = 0;
  int                   max_width_px_   = 0;
  int                   max_height_px_  = 0;
  float                 percentage_     = 100.0f;
  float                 megapixel_limit_ = 0.0f;
  int                   dpi_            = 300;
  bool                  output_sharpen_ = false;
  float                 sharpen_amount_ = 0.5f;
  float                 sharpen_radius_ = 0.8f;
  float                 sharpen_threshold_ = 0.02f;
  bool                  sharpen_for_screen_ = true;  // false = print
  JxlEncodeOptions      jxl_opts_{};
  AvifEncodeOptions     avif_opts_{};
  ExrEncodeOptions      exr_opts_{};
  QoiEncodeOptions      qoi_opts_{};
  CubeLutOptions        cube_lut_opts_{};
};

// Preset registry: stores named presets, supports save/load via JSON.
class PresetRegistry {
 public:
  PresetRegistry()  = default;
  ~PresetRegistry() = default;

  auto AddPreset(const ExportPreset& preset) -> bool;
  auto RemovePreset(const std::string& name) -> bool;
  auto GetPreset(const std::string& name) const -> std::optional<ExportPreset>;
  auto GetAllPresets() const -> const std::map<std::string, ExportPreset>&;
  auto GetAllPresetsByCategory(PresetCategory category) const -> std::vector<ExportPreset>;
  auto GetDefaultPresetForFormat(ExtendedFormat format) const -> std::optional<ExportPreset>;

  // Serialization
  auto ToJsonString() const -> std::string;
  auto FromJsonString(const std::string& json) -> bool;

  auto SaveToFile(const std::filesystem::path& filepath) const -> bool;
  auto LoadFromFile(const std::filesystem::path& filepath) -> bool;

  // Factory: populate built-in default presets
  void PopulateDefaults();

 private:
  std::map<std::string, ExportPreset> presets_{};
};

// ─────────────────────────────────────────────────────────────────────
// (d) Metadata control
// ─────────────────────────────────────────────────────────────────────

enum class MetadataPolicy : uint8_t {
  KEEP_ALL            = 0,
  STRIP_ALL,             // remove all metadata
  COPYRIGHT_ONLY,        // keep only copyright fields
  CAMERA_INFO_ONLY,      // keep only camera/lens/exposure fields
  CUSTOM_WHITELIST,      // keep only specified fields
  CUSTOM_BLACKLIST,      // remove only specified fields
};

// Known metadata field identifiers
enum class MetadataField : uint8_t {
  MAKE,
  MODEL,
  LENS,
  LENS_MAKE,
  DATE_TIME,
  APERTURE,
  SHUTTER_SPEED,
  ISO,
  FOCAL_LENGTH,
  FOCAL_LENGTH_35MM,
  FOCUS_DISTANCE,
  RATING,
  LABEL,
  GPS_LATITUDE,
  GPS_LONGITUDE,
  GPS_ALTITUDE,
  COPYRIGHT,
  ARTIST,
  DESCRIPTION,
  KEYWORDS,
  IMAGE_WIDTH,
  IMAGE_HEIGHT,
  COLOR_SPACE,
  ORIENTATION,
  SOFTWARE,
};

struct MetadataControlConfig {
  MetadataPolicy                     policy_            = MetadataPolicy::KEEP_ALL;
  std::unordered_set<MetadataField>  whitelist_{};
  std::unordered_set<MetadataField>  blacklist_{};
  bool                               remove_gps_        = false;
  bool                               add_watermark_metadata_ = false;
  std::string                        watermark_text_    = "";
  std::string                        watermark_copyright_ = "";
};

// Determine whether a specific metadata field should be kept given the policy.
auto ShouldKeepField(MetadataField field, const MetadataControlConfig& config) -> bool;

// ─────────────────────────────────────────────────────────────────────
// (e) Image resizing
// ─────────────────────────────────────────────────────────────────────

enum class ResizeMode : uint8_t {
  LONG_EDGE,    // fit to specified long edge in pixels
  SHORT_EDGE,   // fit to specified short edge in pixels
  WIDTH,        // exact width constraint
  HEIGHT,       // exact height constraint
  PERCENTAGE,   // scale by percentage
  MEGAPIXEL,    // limit to megapixel count
};

enum class SharpenTarget : uint8_t {
  SCREEN = 0,
  PRINT,
};

struct ResizeConfig {
  ResizeMode  mode_             = ResizeMode::LONG_EDGE;
  int         long_edge_px_     = 0;
  int         short_edge_px_    = 0;
  int         max_width_px_     = 0;
  int         max_height_px_    = 0;
  float       percentage_       = 100.0f;
  float       megapixel_limit_  = 0.0f;
  int         dpi_              = 300;       // for print metadata
  bool        keep_aspect_ratio_ = true;
};

struct SharpenConfig {
  bool        enabled_         = false;
  float       amount_          = 0.5f;       // 0.0 - 1.0
  float       radius_          = 0.8f;       // in pixels
  float       threshold_       = 0.02f;      // 0.0 - 1.0
  SharpenTarget target_        = SharpenTarget::SCREEN;
};

// Compute output dimensions given source dimensions and resize config.
struct ImageDimensions {
  int width_  = 0;
  int height_ = 0;
};

auto ComputeResizeDimensions(int src_width, int src_height, const ResizeConfig& config)
    -> ImageDimensions;

// ─────────────────────────────────────────────────────────────────────
// Combined export configuration
// ─────────────────────────────────────────────────────────────────────

struct ExportEnhancementConfig {
  // Format selection
  ExtendedFormat        extended_format_ = ExtendedFormat::JXL;

  // Format-specific options
  JxlEncodeOptions      jxl_opts_{};
  AvifEncodeOptions     avif_opts_{};
  ExrEncodeOptions      exr_opts_{};
  QoiEncodeOptions      qoi_opts_{};
  CubeLutOptions        cube_lut_opts_{};

  // Naming
  NamingTemplateConfig  naming_{};
  NamingContext         naming_ctx_{};

  // Resizing
  ResizeConfig          resize_{};
  SharpenConfig         sharpen_{};

  // Metadata
  MetadataControlConfig metadata_{};
};

// ─────────────────────────────────────────────────────────────────────
// Utility: format helpers
// ─────────────────────────────────────────────────────────────────────

auto ToString(ExtendedFormat fmt) -> std::string_view;
auto ToString(PresetCategory cat) -> std::string_view;
auto ToString(MetadataPolicy policy) -> std::string_view;
auto ToString(MetadataField field) -> std::string_view;
auto ToString(ResizeMode mode) -> std::string_view;
auto ToString(SharpenTarget target) -> std::string_view;

auto FileExtensionForFormat(ExtendedFormat fmt) -> std::string_view;

// ─────────────────────────────────────────────────────────────────────
// LUT export: convert 3D adjustment data to .cube format
// ─────────────────────────────────────────────────────────────────────

// Represents a 3D LUT table: dimensions[3] of RGB→RGB mapping.
// Data is stored in row-major order: R varies fastest, then G, then B.
struct Lut3DTable {
  int                 size_ = 0;  // grid points per dimension
  std::vector<float>  data_{};    // size * size * size * 3 floats
};

// Generate a .cube file content string from a 3D LUT table and options.
auto GenerateCubeLutContent(const Lut3DTable& table, const CubeLutOptions& options) -> std::string;

// Write a .cube LUT file to disk.
auto WriteCubeLutFile(const std::filesystem::path& filepath, const Lut3DTable& table,
                      const CubeLutOptions& options) -> bool;

// ─────────────────────────────────────────────────────────────────────
// Preset factory helpers
// ─────────────────────────────────────────────────────────────────────

auto MakeFullResolutionPreset(ExtendedFormat fmt) -> ExportPreset;
auto MakeWebPreset(ExtendedFormat fmt) -> ExportPreset;
auto MakeSocialMediaPreset(ExtendedFormat fmt) -> ExportPreset;
auto MakePrintPreset(ExtendedFormat fmt) -> ExportPreset;

}  // namespace export_enh
}  // namespace alcedo