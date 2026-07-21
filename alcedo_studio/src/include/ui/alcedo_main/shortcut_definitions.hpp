//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QKeySequence>
#include <QSettings>
#include <QString>
#include <QMap>

#include <functional>
#include <vector>

#include "ui/alcedo_main/shortcut_registry.hpp"

namespace alcedo::ui {

/// Comprehensive keyboard shortcut definitions for the application.
/// Centralizes all shortcut IDs, default key bindings, and descriptions.
namespace shortcuts {

// ---- File shortcuts ----
inline constexpr ShortcutCommandId kFileOpen           = "file_open";
inline constexpr ShortcutCommandId kFileSave           = "file_save";
inline constexpr ShortcutCommandId kFileExport         = "file_export";
inline constexpr ShortcutCommandId kFileClose          = "file_close";
inline constexpr ShortcutCommandId kFileQuit           = "file_quit";

// ---- Edit shortcuts ----
inline constexpr ShortcutCommandId kEditUndo           = "edit_undo";
inline constexpr ShortcutCommandId kEditRedo           = "edit_redo";
inline constexpr ShortcutCommandId kEditCopy           = "edit_copy";
inline constexpr ShortcutCommandId kEditPaste          = "edit_paste";
inline constexpr ShortcutCommandId kEditSelectAll      = "edit_select_all";
inline constexpr ShortcutCommandId kEditDeselect       = "edit_deselect";
inline constexpr ShortcutCommandId kEditDelete         = "edit_delete";
inline constexpr ShortcutCommandId kEditRate           = "edit_rate";

// ---- View shortcuts ----
inline constexpr ShortcutCommandId kViewZoomIn         = "view_zoom_in";
inline constexpr ShortcutCommandId kViewZoomOut        = "view_zoom_out";
inline constexpr ShortcutCommandId kViewFitToWindow    = "view_fit_to_window";
inline constexpr ShortcutCommandId kViewActualSize     = "view_actual_size";
inline constexpr ShortcutCommandId kViewToggleInfo     = "view_toggle_info";
inline constexpr ShortcutCommandId kViewToggleBeforeAfter = "view_toggle_before_after";

// ---- Image shortcuts ----
inline constexpr ShortcutCommandId kImageRotateCW      = "image_rotate_cw";
inline constexpr ShortcutCommandId kImageRotateCCW     = "image_rotate_ccw";
inline constexpr ShortcutCommandId kImageFlipH         = "image_flip_h";
inline constexpr ShortcutCommandId kImageFlipV         = "image_flip_v";

// ---- Panel shortcuts ----
inline constexpr ShortcutCommandId kPanelTone          = "panel_tone";
inline constexpr ShortcutCommandId kPanelLook          = "panel_look";
inline constexpr ShortcutCommandId kPanelDRT           = "panel_drt";
inline constexpr ShortcutCommandId kPanelGeometry      = "panel_geometry";
inline constexpr ShortcutCommandId kPanelRaw           = "panel_raw";

// ---- Adjustment focus shortcuts ----
inline constexpr ShortcutCommandId kAdjExposure        = "adj_exposure";
inline constexpr ShortcutCommandId kAdjContrast        = "adj_contrast";
inline constexpr ShortcutCommandId kAdjHighlights      = "adj_highlights";
inline constexpr ShortcutCommandId kAdjShadows         = "adj_shadows";
inline constexpr ShortcutCommandId kAdjWhite           = "adj_white";
inline constexpr ShortcutCommandId kAdjBlack           = "adj_black";
inline constexpr ShortcutCommandId kAdjSaturation      = "adj_saturation";
inline constexpr ShortcutCommandId kAdjVibrance        = "adj_vibrance";
inline constexpr ShortcutCommandId kAdjClarity         = "adj_clarity";
inline constexpr ShortcutCommandId kAdjSharpen         = "adj_sharpen";

/// Register all standard shortcuts with a ShortcutRegistry.
void RegisterAllShortcuts(ShortcutRegistry& registry, QWidget* owner);

/// Get the default key sequence for a shortcut ID.
auto DefaultSequence(const ShortcutCommandId& id) -> QKeySequence;

/// Get the description for a shortcut ID.
auto Description(const ShortcutCommandId& id) -> QString;

/// Persist custom shortcut overrides to QSettings.
void SaveCustomShortcuts(const QMap<ShortcutCommandId, QKeySequence>& overrides);

/// Load custom shortcut overrides from QSettings.
auto LoadCustomShortcuts() -> QMap<ShortcutCommandId, QKeySequence>;

/// Reset all shortcuts to their defaults.
void ResetToDefaults();

/// Get all shortcut definitions as a list (for UI display).
struct ShortcutDefinition {
  ShortcutCommandId id;
  QString           category;
  QString           description;
  QKeySequence      default_sequence;
};
auto AllDefinitions() -> std::vector<ShortcutDefinition>;

}  // namespace shortcuts
}  // namespace alcedo::ui
