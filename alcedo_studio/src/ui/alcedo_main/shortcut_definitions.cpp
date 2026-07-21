//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ui/alcedo_main/shortcut_definitions.hpp"

#include <QSettings>

namespace alcedo::ui {
namespace shortcuts {

namespace {

struct Definition {
  ShortcutCommandId id;
  QString           category;
  QString           description;
  QKeySequence      default_sequence;
};

const std::vector<Definition>& AllDefs() {
  static const std::vector<Definition> defs = {
    // File
    {kFileOpen,    QStringLiteral("File"), QStringLiteral("Open Project"),     QKeySequence::Open},
    {kFileSave,    QStringLiteral("File"), QStringLiteral("Save Project"),     QKeySequence::Save},
    {kFileExport,  QStringLiteral("File"), QStringLiteral("Export Image"),     QKeySequence(Qt::CTRL | Qt::Key_E)},
    {kFileClose,   QStringLiteral("File"), QStringLiteral("Close Editor"),     QKeySequence::Close},
    {kFileQuit,    QStringLiteral("File"), QStringLiteral("Quit Application"), QKeySequence::Quit},

    // Edit
    {kEditUndo,    QStringLiteral("Edit"), QStringLiteral("Undo"),             QKeySequence::Undo},
    {kEditRedo,    QStringLiteral("Edit"), QStringLiteral("Redo"),             QKeySequence(Qt::CTRL | Qt::SHIFT | Qt::Key_Z)},
    {kEditCopy,    QStringLiteral("Edit"), QStringLiteral("Copy Adjustments"), QKeySequence::Copy},
    {kEditPaste,   QStringLiteral("Edit"), QStringLiteral("Paste Adjustments"),QKeySequence::Paste},
    {kEditSelectAll,     QStringLiteral("Edit"), QStringLiteral("Select All"),   QKeySequence::SelectAll},
    {kEditDeselect,      QStringLiteral("Edit"), QStringLiteral("Deselect All"), QKeySequence(Qt::CTRL | Qt::SHIFT | Qt::Key_A)},
    {kEditDelete,        QStringLiteral("Edit"), QStringLiteral("Delete"),       QKeySequence::Delete},
    {kEditRate,          QStringLiteral("Edit"), QStringLiteral("Rate Image"),   QKeySequence(Qt::CTRL | Qt::Key_R)},

    // View
    {kViewZoomIn,        QStringLiteral("View"), QStringLiteral("Zoom In"),        QKeySequence::ZoomIn},
    {kViewZoomOut,       QStringLiteral("View"), QStringLiteral("Zoom Out"),       QKeySequence::ZoomOut},
    {kViewFitToWindow,   QStringLiteral("View"), QStringLiteral("Fit to Window"),  QKeySequence(Qt::CTRL | Qt::Key_0)},
    {kViewActualSize,    QStringLiteral("View"), QStringLiteral("Actual Size 100%"),QKeySequence(Qt::CTRL | Qt::Key_1)},
    {kViewToggleInfo,    QStringLiteral("View"), QStringLiteral("Toggle Info Panel"),QKeySequence(Qt::Key_I)},
    {kViewToggleBeforeAfter, QStringLiteral("View"), QStringLiteral("Toggle Before/After"),QKeySequence(Qt::Key_B)},

    // Image
    {kImageRotateCW,     QStringLiteral("Image"), QStringLiteral("Rotate Clockwise"),  QKeySequence(Qt::CTRL | Qt::Key_BracketRight)},
    {kImageRotateCCW,    QStringLiteral("Image"), QStringLiteral("Rotate Counter-Clockwise"), QKeySequence(Qt::CTRL | Qt::Key_BracketLeft)},
    {kImageFlipH,        QStringLiteral("Image"), QStringLiteral("Flip Horizontal"),    QKeySequence(Qt::CTRL | Qt::SHIFT | Qt::Key_BracketLeft)},
    {kImageFlipV,        QStringLiteral("Image"), QStringLiteral("Flip Vertical"),      QKeySequence(Qt::CTRL | Qt::SHIFT | Qt::Key_BracketRight)},

    // Panel
    {kPanelTone,     QStringLiteral("Panel"), QStringLiteral("Tone Panel"),        QKeySequence(Qt::Key_1)},
    {kPanelLook,     QStringLiteral("Panel"), QStringLiteral("Look Panel"),        QKeySequence(Qt::Key_2)},
    {kPanelDRT,      QStringLiteral("Panel"), QStringLiteral("DRT Panel"),         QKeySequence(Qt::Key_3)},
    {kPanelGeometry, QStringLiteral("Panel"), QStringLiteral("Geometry Panel"),    QKeySequence(Qt::Key_4)},
    {kPanelRaw,      QStringLiteral("Panel"), QStringLiteral("Raw Decode Panel"),  QKeySequence(Qt::Key_5)},

    // Adjustment focus
    {kAdjExposure,   QStringLiteral("Adjustment"), QStringLiteral("Focus Exposure"),    QKeySequence(Qt::Key_E)},
    {kAdjContrast,   QStringLiteral("Adjustment"), QStringLiteral("Focus Contrast"),    QKeySequence(Qt::Key_C)},
    {kAdjHighlights, QStringLiteral("Adjustment"), QStringLiteral("Focus Highlights"),  QKeySequence(Qt::SHIFT | Qt::Key_H)},
    {kAdjShadows,    QStringLiteral("Adjustment"), QStringLiteral("Focus Shadows"),     QKeySequence(Qt::SHIFT | Qt::Key_S)},
    {kAdjWhite,      QStringLiteral("Adjustment"), QStringLiteral("Focus White Point"), QKeySequence(Qt::Key_W)},
    {kAdjBlack,      QStringLiteral("Adjustment"), QStringLiteral("Focus Black Point"), QKeySequence(Qt::SHIFT | Qt::Key_B)},
    {kAdjSaturation, QStringLiteral("Adjustment"), QStringLiteral("Focus Saturation"),  QKeySequence(Qt::Key_S)},
    {kAdjVibrance,   QStringLiteral("Adjustment"), QStringLiteral("Focus Vibrance"),    QKeySequence(Qt::Key_V)},
    {kAdjClarity,    QStringLiteral("Adjustment"), QStringLiteral("Focus Clarity"),     QKeySequence(Qt::SHIFT | Qt::Key_C)},
    {kAdjSharpen,    QStringLiteral("Adjustment"), QStringLiteral("Focus Sharpen"),     QKeySequence(Qt::SHIFT | Qt::Key_R)},
  };
  return defs;
}

}  // namespace

void RegisterAllShortcuts(ShortcutRegistry& registry, QWidget* owner) {
  auto custom = LoadCustomShortcuts();

  for (const auto& def : AllDefs()) {
    ShortcutBindingSpec spec;
    spec.id       = def.id;
    spec.description = def.description;

    // Apply custom override if it exists
    if (auto it = custom.find(def.id); it != custom.end()) {
      spec.default_sequence = it.value();
    } else {
      spec.default_sequence = def.default_sequence;
    }

    spec.context = Qt::WidgetWithChildrenShortcut;
    spec.enabled_when = nullptr;   // Always enabled; caller can override
    spec.on_trigger   = nullptr;   // Caller connects actions after registration

    registry.Register(spec);
  }
}

auto DefaultSequence(const ShortcutCommandId& id) -> QKeySequence {
  for (const auto& def : AllDefs()) {
    if (def.id == id) return def.default_sequence;
  }
  return {};
}

auto Description(const ShortcutCommandId& id) -> QString {
  for (const auto& def : AllDefs()) {
    if (def.id == id) return def.description;
  }
  return {};
}

void SaveCustomShortcuts(const QMap<ShortcutCommandId, QKeySequence>& overrides) {
  QSettings settings(QSettings::UserScope, QStringLiteral("AlcedoStudio"),
                     QStringLiteral("Shortcuts"));
  settings.beginGroup(QStringLiteral("custom"));
  settings.remove(QString());  // Clear existing
  for (auto it = overrides.cbegin(); it != overrides.cend(); ++it) {
    settings.setValue(it.key(), it.value().toString());
  }
  settings.endGroup();
}

auto LoadCustomShortcuts() -> QMap<ShortcutCommandId, QKeySequence> {
  QMap<ShortcutCommandId, QKeySequence> result;
  QSettings settings(QSettings::UserScope, QStringLiteral("AlcedoStudio"),
                     QStringLiteral("Shortcuts"));
  settings.beginGroup(QStringLiteral("custom"));
  for (const QString& key : settings.childKeys()) {
    QKeySequence seq = QKeySequence::fromString(settings.value(key).toString());
    if (!seq.isEmpty()) {
      result[key] = seq;
    }
  }
  settings.endGroup();
  return result;
}

void ResetToDefaults() {
  QSettings settings(QSettings::UserScope, QStringLiteral("AlcedoStudio"),
                     QStringLiteral("Shortcuts"));
  settings.beginGroup(QStringLiteral("custom"));
  settings.remove(QString());
  settings.endGroup();
}

auto AllDefinitions() -> std::vector<ShortcutDefinition> {
  std::vector<ShortcutDefinition> result;
  for (const auto& def : AllDefs()) {
    result.push_back({def.id, def.category, def.description, def.default_sequence});
  }
  return result;
}

}  // namespace shortcuts
}  // namespace alcedo::ui
