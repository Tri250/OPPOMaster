//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QString>
#include <QVariantList>
#include <QVariantMap>
#include <filesystem>
#include <memory>
#include <optional>
#include <string>
#include <vector>

namespace alcedo {

/// Color space for export output.
enum class ExportPresetColorSpace {
  SRGB,
  AdobeRGB,
  DisplayP3,
};

/// Built-in export preset for Chinese social media platforms and common uses.
struct ExportPreset {
  QString                   id;
  QString                   display_name;
  QString                   description;
  int                       width           = 0;
  int                       height          = 0;
  int                       quality         = 85;
  QString                   format          = "JPEG";   // JPEG, PNG, TIFF, WebP
  ExportPresetColorSpace    color_space     = ExportPresetColorSpace::SRGB;
  bool                      embed_icc       = true;
  bool                      built_in        = true;
  QString                   category;    // "social_media", "print", "web", "custom"
  QString                   icon_name;   // Optional icon for UI
};

/// Manages built-in and user-custom export presets.
class ExportPresetManager {
 public:
  ExportPresetManager();

  /// Get all built-in presets.
  auto BuiltInPresets() const -> const std::vector<ExportPreset>&;

  /// Get all user-created custom presets.
  auto CustomPresets() const -> const std::vector<ExportPreset>&;

  /// Get all presets (built-in + custom).
  auto AllPresets() const -> std::vector<ExportPreset>;

  /// Find a preset by id.
  auto FindPreset(const QString& id) const -> std::optional<ExportPreset>;

  /// Add or update a custom preset.
  auto SaveCustomPreset(const ExportPreset& preset) -> bool;

  /// Delete a custom preset by id. Cannot delete built-in presets.
  auto DeleteCustomPreset(const QString& id) -> bool;

  /// Convert a preset to a QVariantMap for QML.
  static auto PresetToVariantMap(const ExportPreset& preset) -> QVariantMap;

  /// Convert all presets to a QVariantList for QML.
  auto AllPresetsAsVariantList() const -> QVariantList;

  /// Load custom presets from disk.
  void LoadCustomPresets();

  /// Save custom presets to disk.
  void SaveCustomPresets() const;

  /// Get the storage file path for custom presets.
  auto CustomPresetFilePath() const -> std::filesystem::path;

  /// Get preset category display name (localized).
  static auto CategoryDisplayName(const QString& category) -> QString;

 private:
  void InitializeBuiltInPresets();

  std::vector<ExportPreset> built_in_presets_;
  std::vector<ExportPreset> custom_presets_;
  std::filesystem::path     storage_path_;
};

}  // namespace alcedo
