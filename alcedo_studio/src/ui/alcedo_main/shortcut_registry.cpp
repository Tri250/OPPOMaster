//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ui/alcedo_main/shortcut_registry.hpp"

#include <QAction>
#include <QInputMethod>
#include <QApplication>
#include <QObject>

namespace alcedo::ui {

// Helper function to check if an input method is currently composing.
// This is particularly relevant for Chinese/Japanese/Korean input methods
// where the user may be in the middle of composing characters.
auto IsInputMethodComposing(QWidget* focus_widget) -> bool {
  if (!focus_widget) {
    focus_widget = QApplication::focusWidget();
  }
  if (!focus_widget) {
    return false;
  }
  
  QInputMethod* im = QApplication::inputMethod();
  if (!im) {
    return false;
  }
  
  // Check if the input method is actively composing text
  return im->isAnimating() || im->isVisible();
}

ShortcutRegistry::ShortcutRegistry(QWidget* owner) : owner_(owner) {}

auto ShortcutRegistry::Register(ShortcutBindingSpec spec) -> QAction* {
  if (!owner_ || spec.id.isEmpty() || !spec.on_trigger) {
    return nullptr;
  }

  if (const auto it = entries_.find(spec.id); it != entries_.end()) {
    return it->second.action;
  }

  auto* action = new QAction(owner_);
  action->setObjectName(spec.id);
  action->setShortcut(spec.default_sequence);
  action->setAutoRepeat(true);
  action->setShortcutContext(spec.context);
  if (!spec.description.isEmpty()) {
    action->setText(spec.description);
    action->setToolTip(spec.description);
    action->setStatusTip(spec.description);
  }
  owner_->addAction(action);

  const ShortcutCommandId id = spec.id;
  Entry entry{.spec = std::move(spec), .action = action, .alternate_action = nullptr};
  
  // Register alternate shortcut if provided (for IME conflict scenarios)
  if (entry.spec.alternate_sequence.has_value() && 
      entry.spec.alternate_sequence->isEmpty() == false) {
    auto* alt_action = new QAction(owner_);
    alt_action->setObjectName(spec.id + QStringLiteral("_alternate"));
    alt_action->setShortcut(*entry.spec.alternate_sequence);
    alt_action->setAutoRepeat(true);
    alt_action->setShortcutContext(spec.context);
    owner_->addAction(alt_action);
    entry.alternate_action = alt_action;
    
    // Connect alternate action to the same trigger
    QObject::connect(alt_action, &QAction::triggered, owner_, [this, id](bool) {
      const auto it = entries_.find(id);
      if (it == entries_.end()) {
        return;
      }
      auto& entry = it->second;
      if (entry.spec.enabled_when) {
        const bool enabled = entry.spec.enabled_when();
        entry.action->setEnabled(enabled);
        if (!enabled) {
          return;
        }
      }
      entry.spec.on_trigger();
    });
  }
  
  entries_.emplace(id, std::move(entry));
  
  QObject::connect(action, &QAction::triggered, owner_, [this, id](bool) {
    const auto it = entries_.find(id);
    if (it == entries_.end()) {
      return;
    }

    auto& entry = it->second;
    if (entry.spec.enabled_when) {
      const bool enabled = entry.spec.enabled_when();
      entry.action->setEnabled(enabled);
      if (!enabled) {
        return;
      }
    }

    entry.spec.on_trigger();
  });

  RefreshEnabledStates();
  return action;
}

auto ShortcutRegistry::Action(const ShortcutCommandId& id) const -> QAction* {
  if (const auto it = entries_.find(id); it != entries_.end()) {
    return it->second.action;
  }
  return nullptr;
}

auto ShortcutRegistry::ShortcutText(const ShortcutCommandId& id,
                                    QKeySequence::SequenceFormat format) const -> QString {
  const auto* action = Action(id);
  if (!action) {
    return {};
  }
  return action->shortcut().toString(format);
}

auto ShortcutRegistry::ShortcutTextLocalized(const ShortcutCommandId& id) const -> QString {
  const auto it = entries_.find(id);
  if (it == entries_.end()) {
    return {};
  }
  
  const auto& entry = it->second;
  
  // If a Chinese accelerator text is provided, use it
  if (!entry.spec.chinese_accelerator.isEmpty()) {
    QString result = entry.spec.chinese_accelerator;
    // Append alternate shortcut if available
    if (entry.spec.alternate_sequence.has_value()) {
      QString alt_text = entry.spec.alternate_sequence->toString(QKeySequence::NativeText);
      if (!alt_text.isEmpty()) {
        result = QStringLiteral("%1 / %2").arg(result, alt_text);
      }
    }
    return result;
  }
  
  // Otherwise use the standard shortcut text
  QString shortcut_text = ShortcutText(id);
  
  // Append alternate shortcut if available
  if (entry.spec.alternate_sequence.has_value()) {
    QString alt_text = entry.spec.alternate_sequence->toString(QKeySequence::NativeText);
    if (!alt_text.isEmpty()) {
      shortcut_text = QStringLiteral("%1 / %2").arg(shortcut_text, alt_text);
    }
  }
  
  return shortcut_text;
}

auto ShortcutRegistry::DecorateTooltip(const QString& base_tooltip,
                                       const ShortcutCommandId& id) const -> QString {
  const QString shortcut_text = ShortcutTextLocalized(id);
  if (shortcut_text.isEmpty()) {
    return base_tooltip;
  }
  if (base_tooltip.isEmpty()) {
    return shortcut_text;
  }
  return QStringLiteral("%1 (%2)").arg(base_tooltip, shortcut_text);
}

void ShortcutRegistry::RefreshEnabledStates() {
  for (auto& [id, entry] : entries_) {
    Q_UNUSED(id);
    const bool enabled = !entry.spec.enabled_when || entry.spec.enabled_when();
    entry.action->setEnabled(enabled);
    if (entry.alternate_action) {
      entry.alternate_action->setEnabled(enabled);
    }
  }
}

auto ShortcutRegistry::ShouldConsumeKeyDespiteIme(const QKeySequence& sequence) const -> bool {
  // Check if any registered shortcut matches the sequence and should NOT be suppressed
  // when IME is composing (i.e., suppress_when_ime_composing is false)
  for (const auto& [id, entry] : entries_) {
    Q_UNUSED(id);
    if (entry.spec.suppress_when_ime_composing) {
      continue;  // This shortcut should be suppressed, don't consume
    }
    if (entry.action && entry.action->shortcut() == sequence) {
      return true;
    }
    if (entry.alternate_action && entry.alternate_action->shortcut() == sequence) {
      return true;
    }
  }
  return false;
}

}  // namespace alcedo::ui
