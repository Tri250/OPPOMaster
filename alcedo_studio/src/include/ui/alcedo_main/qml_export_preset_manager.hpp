//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QObject>
#include <QVariantList>
#include <QVariantMap>

#include "app/export_preset.hpp"

namespace alcedo::ui {

/// QML-accessible wrapper around ExportPresetManager.
/// Exposes preset list and preset lookup as Q_INVOKABLE methods so QML
/// can populate combo boxes and apply preset values.
class QmlExportPresetManager : public QObject {
  Q_OBJECT
 public:
  explicit QmlExportPresetManager(ExportPresetManager* backend, QObject* parent = nullptr)
      : QObject(parent), backend_(backend) {}

  /// Returns a QVariantList of {text, value} maps for QML ComboBox.
  /// "text" is the display name, "value" is the preset id.
  Q_INVOKABLE QVariantList presetList() const {
    if (!backend_) return {};
    QVariantList list;
    // First item: "Custom" (no preset)
    QVariantMap custom;
    custom.insert("text", tr("Custom"));
    custom.insert("value", "");
    list.append(custom);

    auto presets = backend_->AllPresets();
    for (const auto& p : presets) {
      QVariantMap entry;
      entry.insert("text", p.display_name);
      entry.insert("value", p.id);
      list.append(entry);
    }
    return list;
  }

  /// Returns a QVariantMap for a preset by id, or empty map if not found.
  Q_INVOKABLE QVariantMap GetPreset(const QString& id) const {
    if (!backend_) return {};
    auto preset = backend_->FindPreset(id);
    if (!preset) return {};
    auto m = ExportPresetManager::PresetToVariantMap(*preset);
    // Ensure key names are consistent with QML expectations
    m.insert("maxWidth", preset->width);
    m.insert("maxHeight", preset->height);
    m.insert("quality", preset->quality);
    m.insert("format", preset->format);
    // Map color space enum to a string QML can use
    switch (preset->color_space) {
      case ExportPresetColorSpace::SRGB:     m.insert("colorSpace", "srgb"); break;
      case ExportPresetColorSpace::AdobeRGB: m.insert("colorSpace", "adobergb"); break;
      case ExportPresetColorSpace::DisplayP3:m.insert("colorSpace", "display-p3"); break;
    }
    return m;
  }

 private:
  ExportPresetManager* backend_;
};

}  // namespace alcedo::ui
