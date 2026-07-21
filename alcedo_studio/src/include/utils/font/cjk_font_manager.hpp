//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QString>
#include <QStringList>
#include <QFont>

namespace alcedo::font {

/// Manages CJK (Chinese, Japanese, Korean) font fallback chains for optimal
/// rendering on each platform. Detects the current platform, selects the best
/// available CJK font, and configures Qt's application font with proper CJK
/// fallback and hinting settings.
class CjkFontManager {
 public:
  /// Returns the singleton instance.
  static auto Instance() -> CjkFontManager&;

  /// Detects platform and initializes the optimal CJK font fallback chain.
  /// Must be called after QApplication is constructed.
  void Initialize();

  /// Returns the primary CJK font family name for the current platform.
  [[nodiscard]] auto PrimaryCjkFontFamily() const -> const QString&;

  /// Returns the full CJK fallback chain (ordered by preference).
  [[nodiscard]] auto CjkFallbackChain() const -> const QStringList&;

  /// Applies the CJK-optimized font to the given QApplication instance.
  /// Sets the application font with CJK fallback and configures hinting.
  void ApplyCjkFont(class QApplication& app);

  /// Configures font hinting for CJK clarity on the given font.
  void ConfigureHintingForCjk(QFont& font);

 private:
  CjkFontManager();

  /// Detects the first available font family from the given candidates.
  [[nodiscard]] auto DetectAvailableFont(const QStringList& candidates) const -> QString;

  /// Builds the platform-specific CJK fallback chain.
  void BuildFallbackChain();

  QString    primary_cjk_family_;
  QStringList cjk_fallback_chain_;
  bool       initialized_ = false;
};

}  // namespace alcedo::font
