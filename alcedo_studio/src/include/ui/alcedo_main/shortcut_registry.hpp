//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <functional>
#include <map>
#include <optional>

#include <QKeySequence>
#include <QString>
#include <QWidget>

class QAction;
class QInputMethod;

namespace alcedo::ui {

using ShortcutCommandId = QString;

// Extended shortcut binding specification with IME compatibility support.
// For Chinese users, some shortcuts may conflict with input method switching
// (e.g., Ctrl+Space for IME toggle). The alternate_sequence provides a fallback.
struct ShortcutBindingSpec {
  ShortcutCommandId    id;
  QString              description;
  QKeySequence         default_sequence;
  std::optional<QKeySequence> alternate_sequence{};  // Fallback for IME conflict scenarios
  QString              chinese_accelerator{};         // Chinese-friendly display text
  Qt::ShortcutContext  context = Qt::WidgetWithChildrenShortcut;
  std::function<bool()> enabled_when{};
  std::function<void()> on_trigger{};
  
  // Check if this shortcut should be suppressed when IME is composing
  bool suppress_when_ime_composing = false;
};

// IME-aware shortcut context helper.
// Returns true if an input method is currently composing text (Chinese input active).
auto IsInputMethodComposing(QWidget* focus_widget) -> bool;

class ShortcutRegistry final {
 public:
  explicit ShortcutRegistry(QWidget* owner);

  auto Register(ShortcutBindingSpec spec) -> QAction*;
  auto Action(const ShortcutCommandId& id) const -> QAction*;
  auto ShortcutText(const ShortcutCommandId& id,
                    QKeySequence::SequenceFormat format = QKeySequence::NativeText) const
      -> QString;
  
  // Returns the shortcut text with Chinese accelerator display if available.
  auto ShortcutTextLocalized(const ShortcutCommandId& id) const -> QString;
  
  auto DecorateTooltip(const QString& base_tooltip, const ShortcutCommandId& id) const
      -> QString;
  void RefreshEnabledStates();
  
  // Check if any action should consume the key event despite IME being active.
  auto ShouldConsumeKeyDespiteIme(const QKeySequence& sequence) const -> bool;

 private:
  struct Entry {
    ShortcutBindingSpec spec;
    QAction*            action = nullptr;
    QAction*            alternate_action = nullptr;  // For alternate shortcut
  };

  QWidget*                     owner_ = nullptr;
  std::map<ShortcutCommandId, Entry> entries_{};
};

}  // namespace alcedo::ui
