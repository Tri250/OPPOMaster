//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QObject>
#include <QVariantList>
#include <QVariantMap>

#include "ui/alcedo_main/shortcut_definitions.hpp"
#include "ui/alcedo_main/shortcut_registry.hpp"

namespace alcedo::ui {

/// QML-accessible wrapper around ShortcutDefinitions and ShortcutRegistry.
/// Exposes shortcut categories, shortcut lists, conflict checking, and
/// reset-to-defaults as Q_INVOKABLE methods for the ShortcutSettingsPanel.
class QmlShortcutDefinitions : public QObject {
  Q_OBJECT
 public:
  explicit QmlShortcutDefinitions(QObject* parent = nullptr)
      : QObject(parent) {}

  /// Returns a QStringList of unique category names.
  Q_INVOKABLE QStringList GetCategories() const {
    QStringList cats;
    auto defs = shortcuts::AllDefinitions();
    for (const auto& d : defs) {
      if (!cats.contains(d.category)) {
        cats.append(d.category);
      }
    }
    return cats;
  }

  /// Returns a QVariantList of {id, label, keySequence, defaultKeySequence, category}
  /// for shortcuts in the given category.
  Q_INVOKABLE QVariantList GetShortcutsByCategory(const QString& category) const {
    QVariantList list;
    auto defs = shortcuts::AllDefinitions();
    for (const auto& d : defs) {
      if (d.category != category) continue;
      QVariantMap m;
      m.insert("id", d.id);
      m.insert("label", d.description);
      m.insert("keySequence", d.default_sequence.toString(QKeySequence::NativeText));
      m.insert("defaultKeySequence", d.default_sequence.toString(QKeySequence::NativeText));
      m.insert("category", d.category);
      list.append(m);
    }
    return list;
  }

  /// Check if assigning newKeySequence to shortcutId would conflict with
  /// another shortcut. Returns the label of the conflicting shortcut,
  /// or empty string if no conflict.
  Q_INVOKABLE QString CheckConflict(const QString& shortcutId,
                                    const QString& newKeySequence) const {
    QKeySequence new_seq(newKeySequence);
    if (new_seq.isEmpty()) return {};
    auto defs = shortcuts::AllDefinitions();
    for (const auto& d : defs) {
      if (d.id == shortcutId) continue;
      if (d.default_sequence == new_seq) {
        return d.description;
      }
    }
    // Also check custom overrides
    auto customs = shortcuts::LoadCustomShortcuts();
    for (auto it = customs.constBegin(); it != customs.constEnd(); ++it) {
      if (it.key() == shortcutId) continue;
      if (it.value() == new_seq) {
        // Find the description for this shortcut
        for (const auto& d : defs) {
          if (d.id == it.key()) return d.description;
        }
      }
    }
    return {};
  }

  /// Reset all shortcuts to their defaults.
  Q_INVOKABLE void ResetAllToDefaults() {
    shortcuts::ResetToDefaults();
  }
};

}  // namespace alcedo::ui
