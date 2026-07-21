//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "utils/font/cjk_font_manager.hpp"

#include <QApplication>
#include <QFont>
#include <QFontDatabase>
#include <QFontInfo>
#include <QDebug>
#include <QtGlobal>

namespace alcedo::font {

auto CjkFontManager::Instance() -> CjkFontManager& {
  static CjkFontManager instance;
  return instance;
}

CjkFontManager::CjkFontManager() = default;

void CjkFontManager::Initialize() {
  if (initialized_) {
    return;
  }
  BuildFallbackChain();
  initialized_ = true;

  qDebug() << "CJK font manager initialized. Primary CJK font:"
           << primary_cjk_family_
           << "Fallback chain:" << cjk_fallback_chain_;
}

void CjkFontManager::BuildFallbackChain() {
#if defined(Q_OS_WIN)
  // Windows: Microsoft YaHei → SimHei → system default
  static const QStringList kWindowsCjkCandidates = {
      QStringLiteral("Microsoft YaHei"),
      QStringLiteral("Microsoft YaHei UI"),
      QStringLiteral("SimHei"),
      QStringLiteral("SimSun"),
      QStringLiteral("NSimSun"),
      QStringLiteral("FangSong"),
  };
  primary_cjk_family_ = DetectAvailableFont(kWindowsCjkCandidates);
  cjk_fallback_chain_ = QStringList{
      QStringLiteral("Microsoft YaHei"),
      QStringLiteral("Microsoft YaHei UI"),
      QStringLiteral("SimHei"),
      QStringLiteral("SimSun"),
      QStringLiteral("NSimSun"),
      QStringLiteral("FangSong"),
  };

#elif defined(Q_OS_MACOS)
  // macOS: PingFang SC → Hiragino Sans GB → system default
  static const QStringList kMacOSCjkCandidates = {
      QStringLiteral("PingFang SC"),
      QStringLiteral("Hiragino Sans GB"),
      QStringLiteral("STHeiti"),
      QStringLiteral("STSong"),
      QStringLiteral("Songti SC"),
      QStringLiteral("Heiti SC"),
  };
  primary_cjk_family_ = DetectAvailableFont(kMacOSCjkCandidates);
  cjk_fallback_chain_ = QStringList{
      QStringLiteral("PingFang SC"),
      QStringLiteral("Hiragino Sans GB"),
      QStringLiteral("STHeiti"),
      QStringLiteral("STSong"),
      QStringLiteral("Songti SC"),
      QStringLiteral("Heiti SC"),
  };

#elif defined(Q_OS_ANDROID)
  // Android: Noto Sans CJK SC is the system CJK font on Android 5.0+
  static const QStringList kAndroidCjkCandidates = {
      QStringLiteral("Noto Sans CJK SC"),
      QStringLiteral("Noto Sans CJK"),
      QStringLiteral("Noto Sans SC"),
      QStringLiteral("Droid Sans Fallback"),
      QStringLiteral("Noto Serif CJK SC"),
  };
  primary_cjk_family_ = DetectAvailableFont(kAndroidCjkCandidates);
  cjk_fallback_chain_ = QStringList{
      QStringLiteral("Noto Sans CJK SC"),
      QStringLiteral("Noto Sans CJK"),
      QStringLiteral("Noto Sans SC"),
      QStringLiteral("Droid Sans Fallback"),
      QStringLiteral("Noto Serif CJK SC"),
  };

#else
  // Linux: Noto Sans CJK SC → WenQuanYi Micro Hei → system default
  static const QStringList kLinuxCjkCandidates = {
      QStringLiteral("Noto Sans CJK SC"),
      QStringLiteral("Noto Sans CJK"),
      QStringLiteral("WenQuanYi Micro Hei"),
      QStringLiteral("WenQuanYi Zen Hei"),
      QStringLiteral("Droid Sans Fallback"),
      QStringLiteral("AR PL UMing CN"),
  };
  primary_cjk_family_ = DetectAvailableFont(kLinuxCjkCandidates);
  cjk_fallback_chain_ = QStringList{
      QStringLiteral("Noto Sans CJK SC"),
      QStringLiteral("Noto Sans CJK"),
      QStringLiteral("WenQuanYi Micro Hei"),
      QStringLiteral("WenQuanYi Zen Hei"),
      QStringLiteral("Droid Sans Fallback"),
      QStringLiteral("AR PL UMing CN"),
  };
#endif

  // If no CJK font was detected at all, use the system default.
  if (primary_cjk_family_.isEmpty()) {
    primary_cjk_family_ = QApplication::font().family();
    qDebug("No CJK font detected on this system; using application default: %s",
           primary_cjk_family_.toUtf8().constData());
  }
}

auto CjkFontManager::DetectAvailableFont(const QStringList& candidates) const -> QString {
  QFontDatabase db;
  const QStringList families = db.families();
  for (const auto& candidate : candidates) {
    if (families.contains(candidate)) {
      return candidate;
    }
    // Also check if the exact match exists (some systems report differently)
    for (const auto& family : families) {
      if (family.compare(candidate, Qt::CaseInsensitive) == 0) {
        return family;
      }
    }
  }
  return {};
}

auto CjkFontManager::PrimaryCjkFontFamily() const -> const QString& {
  return primary_cjk_family_;
}

auto CjkFontManager::CjkFallbackChain() const -> const QStringList& {
  return cjk_fallback_chain_;
}

void CjkFontManager::ApplyCjkFont(QApplication& app) {
  if (!initialized_) {
    Initialize();
  }

  QFont font = app.font();

  // Set the CJK font as a fallback family. Qt's font matching will use it
  // when rendering CJK characters that the primary font cannot handle.
  if (!primary_cjk_family_.isEmpty()) {
    // Build a comma-separated family list: primary font, CJK primary, then fallbacks
    QStringList family_list;
    family_list << font.family();
    family_list << primary_cjk_family_;
    for (const auto& fallback : cjk_fallback_chain_) {
      if (!family_list.contains(fallback)) {
        family_list << fallback;
      }
    }
    font.setFamilies(family_list);
  }

  ConfigureHintingForCjk(font);
  app.setFont(font);
}

void CjkFontManager::ConfigureHintingForCjk(QFont& font) {
  // CJK characters benefit from full hinting for crisp rendering at smaller
  // sizes. On macOS, the system handles subpixel rendering natively so we
  // prefer the default; on Windows and Linux we force full hinting.
#if defined(Q_OS_WIN) || defined(Q_OS_LINUX)
  font.setHintingPreference(QFont::PreferFullHinting);
#else
  font.setHintingPreference(QFont::PreferDefaultHinting);
#endif

  // Enable prefer antialias for smooth CJK stroke rendering
  font.setStyleStrategy(QFont::PreferAntialias);
}

}  // namespace alcedo::font
