//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ui/alcedo_main/language_manager.hpp"

#include <QCoreApplication>
#include <QLocale>
#include <QSettings>
#include <QQmlEngine>

#include "ui/alcedo_main/i18n.hpp"

namespace {

constexpr auto kLanguageSettingKey = "ui/language";

auto DisplayLabelForCode(const QString& code) -> QString {
  if (code == QStringLiteral("system")) {
    return QCoreApplication::translate("LanguageManager", "Follow System");
  }
  if (code == QStringLiteral("zh-CN")) {
    return QCoreApplication::translate("LanguageManager", "Simplified Chinese");
  }
  if (code == QStringLiteral("ja")) {
    return QCoreApplication::translate("LanguageManager", "Japanese");
  }
  if (code == QStringLiteral("ko")) {
    return QCoreApplication::translate("LanguageManager", "Korean");
  }
  if (code == QStringLiteral("fr")) {
    return QCoreApplication::translate("LanguageManager", "French");
  }
  if (code == QStringLiteral("de")) {
    return QCoreApplication::translate("LanguageManager", "German");
  }
  return QCoreApplication::translate("LanguageManager", "English");
}

}  // namespace

namespace alcedo::ui {

LanguageManager::LanguageManager(QCoreApplication* app, QObject* parent)
    : QObject(parent), app_(app), translator_(std::make_unique<QTranslator>()) {
  LoadPersistedLanguage();
  ApplyLanguage(false);
}

auto LanguageManager::AvailableLanguages() const -> QVariantList {
  return {
      QVariantMap{{"code", QStringLiteral("system")},
                  {"label", DisplayLabelForCode(QStringLiteral("system"))}},
      QVariantMap{{"code", QStringLiteral("en")},
                  {"label", DisplayLabelForCode(QStringLiteral("en"))}},
      QVariantMap{{"code", QStringLiteral("zh-CN")},
                  {"label", DisplayLabelForCode(QStringLiteral("zh-CN"))}},
      QVariantMap{{"code", QStringLiteral("ja")},
                  {"label", DisplayLabelForCode(QStringLiteral("ja"))}},
      QVariantMap{{"code", QStringLiteral("ko")},
                  {"label", DisplayLabelForCode(QStringLiteral("ko"))}},
      QVariantMap{{"code", QStringLiteral("fr")},
                  {"label", DisplayLabelForCode(QStringLiteral("fr"))}},
      QVariantMap{{"code", QStringLiteral("de")},
                  {"label", DisplayLabelForCode(QStringLiteral("de"))}},
  };
}

auto LanguageManager::ResolveSystemLanguageCode(const QLocale& locale) -> QString {
  const auto bcp = locale.bcp47Name();
  if (bcp.startsWith(QStringLiteral("zh"), Qt::CaseInsensitive)) {
    return QStringLiteral("zh-CN");
  }
  if (bcp.startsWith(QStringLiteral("ja"), Qt::CaseInsensitive)) {
    return QStringLiteral("ja");
  }
  if (bcp.startsWith(QStringLiteral("ko"), Qt::CaseInsensitive)) {
    return QStringLiteral("ko");
  }
  if (bcp.startsWith(QStringLiteral("fr"), Qt::CaseInsensitive)) {
    return QStringLiteral("fr");
  }
  if (bcp.startsWith(QStringLiteral("de"), Qt::CaseInsensitive)) {
    return QStringLiteral("de");
  }
  return QStringLiteral("en");
}

void LanguageManager::AttachEngine(QQmlEngine* engine) {
  qml_engine_ = engine;
  if (qml_engine_) {
    qml_engine_->retranslate();
  }
}

void LanguageManager::setLanguage(const QString& code) {
  const QString normalized = NormalizeLanguageCode(code);
  if (normalized == current_language_code_) {
    return;
  }

  current_language_code_ = normalized;
  QSettings{}.setValue(QLatin1String(kLanguageSettingKey), current_language_code_);
  ApplyLanguage(true);
}

auto LanguageManager::NormalizeLanguageCode(const QString& code) -> QString {
  const QString normalized = code.trimmed();
  if (normalized.compare(QStringLiteral("system"), Qt::CaseInsensitive) == 0) {
    return QStringLiteral("system");
  }
  if (normalized.compare(QStringLiteral("zh-CN"), Qt::CaseInsensitive) == 0 ||
      normalized.compare(QStringLiteral("zh_CN"), Qt::CaseInsensitive) == 0 ||
      normalized.compare(QStringLiteral("zh"), Qt::CaseInsensitive) == 0) {
    return QStringLiteral("zh-CN");
  }
  if (normalized.compare(QStringLiteral("ja"), Qt::CaseInsensitive) == 0 ||
      normalized.compare(QStringLiteral("ja-JP"), Qt::CaseInsensitive) == 0) {
    return QStringLiteral("ja");
  }
  if (normalized.compare(QStringLiteral("ko"), Qt::CaseInsensitive) == 0 ||
      normalized.compare(QStringLiteral("ko-KR"), Qt::CaseInsensitive) == 0) {
    return QStringLiteral("ko");
  }
  if (normalized.compare(QStringLiteral("fr"), Qt::CaseInsensitive) == 0 ||
      normalized.compare(QStringLiteral("fr-FR"), Qt::CaseInsensitive) == 0) {
    return QStringLiteral("fr");
  }
  if (normalized.compare(QStringLiteral("de"), Qt::CaseInsensitive) == 0 ||
      normalized.compare(QStringLiteral("de-DE"), Qt::CaseInsensitive) == 0) {
    return QStringLiteral("de");
  }
  return QStringLiteral("en");
}

auto LanguageManager::EffectiveCodeForCurrentSelection() const -> QString {
  if (current_language_code_ == QStringLiteral("system")) {
    return ResolveSystemLanguageCode(QLocale::system());
  }
  return current_language_code_;
}

auto LanguageManager::TranslationResourcePathForCode(const QString& code) const -> QString {
  if (code == QStringLiteral("zh-CN")) {
    return QStringLiteral(":/i18n/alcedo_main_zh_CN.qm");
  }
  if (code == QStringLiteral("ja")) {
    return QStringLiteral(":/i18n/alcedo_main_ja.qm");
  }
  if (code == QStringLiteral("ko")) {
    return QStringLiteral(":/i18n/alcedo_main_ko.qm");
  }
  if (code == QStringLiteral("fr")) {
    return QStringLiteral(":/i18n/alcedo_main_fr.qm");
  }
  if (code == QStringLiteral("de")) {
    return QStringLiteral(":/i18n/alcedo_main_de.qm");
  }
  return QStringLiteral(":/i18n/alcedo_main_en.qm");
}

void LanguageManager::LoadPersistedLanguage() {
  const QString stored = QSettings{}.value(QLatin1String(kLanguageSettingKey),
                                           QStringLiteral("system"))
                             .toString();
  current_language_code_ = NormalizeLanguageCode(stored);
}

void LanguageManager::ApplyLanguage(bool emitSignals) {
  if (!app_) {
    return;
  }

  app_->removeTranslator(translator_.get());

  const QString next_effective = EffectiveCodeForCurrentSelection();
  const QString qm_path        = TranslationResourcePathForCode(next_effective);
  const bool translator_loaded = translator_->load(qm_path);
  if (translator_loaded) {
    app_->installTranslator(translator_.get());
  }

  const bool effective_changed = effective_language_code_ != next_effective;
  effective_language_code_     = next_effective;

  if (qml_engine_) {
    qml_engine_->retranslate();
  }

  i18n::TranslationNotifier::Instance().NotifyLanguageChanged();

  if (!emitSignals) {
    return;
  }

  emit CurrentLanguageCodeChanged();
  if (effective_changed) {
    emit EffectiveLanguageCodeChanged();
  }
  emit AvailableLanguagesChanged();
  emit LanguageChanged();
}

}  // namespace alcedo::ui
