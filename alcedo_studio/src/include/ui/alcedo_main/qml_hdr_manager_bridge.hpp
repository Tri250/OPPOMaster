//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QObject>
#include <QVariant>

#include "ui/edit_viewer/hdr_manager.hpp"

namespace alcedo::ui {

/// QML-accessible bridge for HDRManager static methods.
/// Exposes HDR display state as properties and toggle as Q_INVOKABLE.
class QmlHdrManagerBridge : public QObject {
  Q_OBJECT
  Q_PROPERTY(bool hdrPreviewEnabled READ IsHdrPreviewEnabled NOTIFY hdrPreviewEnabledChanged)
  Q_PROPERTY(bool isHdrDisplayAvailable READ IsHdrDisplayAvailable NOTIFY hdrDisplayChanged)
  Q_PROPERTY(QString currentDisplayInfo READ CurrentDisplayInfo NOTIFY hdrDisplayChanged)

 public:
  explicit QmlHdrManagerBridge(QObject* parent = nullptr) : QObject(parent) {
    // Initialize from current state
    auto& info = alcedo::HDRManager::GetCachedDisplayInfo();
    is_hdr_available_ = info.is_hdr_capable;
    hdr_enabled_ = alcedo::HDRManager::IsHDRPreviewEnabled();

    // Register for display changes
    alcedo::HDRManager::SetHDRDisplayChangeCallback([this](bool is_now_hdr) {
      is_hdr_available_ = is_now_hdr;
      emit hdrDisplayChanged();
    });
  }

  bool IsHdrPreviewEnabled() const { return hdr_enabled_; }
  bool IsHdrDisplayAvailable() const { return is_hdr_available_; }
  QString CurrentDisplayInfo() const {
    return is_hdr_available_ ? QStringLiteral("HDR") : QStringLiteral("SDR");
  }

  /// Toggle HDR preview on/off
  Q_INVOKABLE void ToggleHdrPreview() {
    hdr_enabled_ = !hdr_enabled_;
    alcedo::HDRManager::SetHDRPreviewEnabled(hdr_enabled_);
    emit hdrPreviewEnabledChanged();
  }

 signals:
  void hdrPreviewEnabledChanged();
  void hdrDisplayChanged();

 private:
  bool is_hdr_available_ = false;
  bool hdr_enabled_      = false;
};

}  // namespace alcedo::ui
