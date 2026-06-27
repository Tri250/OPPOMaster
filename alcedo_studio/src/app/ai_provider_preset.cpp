//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/ai_provider_preset.hpp"

#include <algorithm>

#include <QChar>
#include <QLatin1String>
#include <QRegularExpression>
#include <QSettings>
#include <QStringList>

namespace alcedo {
namespace {

// QSettings keys for the selected compatible-protocol preset. All non-secret.
// Grouped under "ai/preset/" so a single Remove() clears the whole preset.
constexpr auto kGroup                = "ai/preset";
constexpr auto kKeyProviderId        = "ai/preset/providerId";
constexpr auto kKeyDisplayName       = "ai/preset/displayName";
constexpr auto kKeyProtocolFamily    = "ai/preset/protocolFamily";
constexpr auto kKeyBaseUrl           = "ai/preset/baseUrl";
constexpr auto kKeyEndpoint          = "ai/preset/endpoint";
constexpr auto kKeyAuthType          = "ai/preset/authType";
constexpr auto kKeyCredentialSlot    = "ai/preset/credentialSlot";
constexpr auto kKeyModelId           = "ai/preset/modelId";
constexpr auto kKeyModelDisplayName  = "ai/preset/modelDisplayName";
constexpr auto kKeyStructuredOutput = "ai/preset/structuredOutputMode";
constexpr auto kKeyTimeoutMs         = "ai/preset/timeoutMs";
constexpr auto kKeyMaxImageBytes     = "ai/preset/maxImageBytes";
constexpr auto kKeyRecommendedRendition = "ai/preset/recommendedRendition";
constexpr auto kKeyMaskedKeyLabel    = "ai/preset/maskedKeyLabel";
constexpr auto kKeyRememberKey       = "ai/preset/rememberKey";
constexpr auto kKeyOutputLanguage    = "ai/preset/outputLanguage";

// Defaults mirror the primary product-facing Opencode Go Anthropic-compatible
// preset (the proven compatible path). They are used when a key is absent so a
// fresh install lands on a sensible preset the user can edit.
constexpr auto kDefaultProtocolFamily       = "anthropic_messages";
constexpr auto kDefaultProviderId           = "opencode_go_anthropic";
constexpr auto kDefaultAuthType             = "bearer";
constexpr auto kDefaultStructuredOutputMode = "tool";
constexpr auto kDefaultRendition            = "preview";
constexpr auto kDefaultOutputLanguage       = "follow";  // resolve to app language at job time
constexpr qint64 kDefaultTimeoutMs          = 60000;
constexpr qint64 kDefaultMaxImageBytes      = 4194304;

// Numeric bounds mirror the Rust provider-config validation
// (provider_config.rs: MIN_TIMEOUT_MS, MAX_TIMEOUT_MS, MAX_IMAGE_BYTES) so a
// bad persisted or setter value can never reach a request or a generated user
// config. The C++ preset contract is being frozen in Phase 6a, so these guards
// keep the two sides from drifting once 6c/6d consume the DTO.
constexpr qint64 kMinTimeoutMs     = 1000;          // 1s
constexpr qint64 kMaxTimeoutMs     = 300000;        // 300s
constexpr qint64 kMinMaxImageBytes = 1;
constexpr qint64 kMaxMaxImageBytes = 16 * 1024 * 1024;  // 16 MiB

QString read_string(const char* key, const QString& fallback) {
  return QSettings().value(QLatin1String(key), fallback).toString();
}

qint64 read_int(const char* key, qint64 fallback) {
  // QSettings may store the value as int or qlonglong depending on platform;
  // toLongLong handles both and yields the fallback for missing/non-numeric.
  return QSettings().value(QLatin1String(key), fallback).toLongLong();
}

bool read_bool(const char* key, bool fallback) {
  return QSettings().value(QLatin1String(key), fallback).toBool();
}

bool LooksLikeRawSecret(const QString& value) {
  const QString v = value.trimmed();
  static const QRegularExpression kOpenAiStyleKey(QStringLiteral("sk-[A-Za-z0-9_-]{16,}"));
  static const QRegularExpression kBearerValue(QStringLiteral("Bearer\\s+\\S{8,}"),
                                               QRegularExpression::CaseInsensitiveOption);
  static const QRegularExpression kAwsAccessKey(QStringLiteral("AKIA[A-Z0-9]{16}"));
  return kOpenAiStyleKey.match(v).hasMatch() || kBearerValue.match(v).hasMatch() ||
         kAwsAccessKey.match(v).hasMatch();
}

bool is_ascii_lower_or_digit_or_underscore(QChar c) {
  const ushort ch = c.unicode();
  return (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_';
}

QString SanitizedNonSecretString(const QString& value) {
  return LooksLikeRawSecret(value) ? QString{} : value;
}

QString SanitizedCredentialSlot(const QString& value) {
  const QString v = value.trimmed();
  if (LooksLikeRawSecret(v) || v.isEmpty()) {
    return QString{};
  }
  for (QChar c : v) {
    if (!is_ascii_lower_or_digit_or_underscore(c)) {
      return QString{};
    }
  }
  return v;
}

QString read_non_secret_string(const char* key, const QString& fallback) {
  QSettings s;
  const QString value = s.value(QLatin1String(key), fallback).toString();
  if (LooksLikeRawSecret(value)) {
    s.remove(QLatin1String(key));
    return QString{};
  }
  return value;
}

QString read_credential_slot(const char* key) {
  QSettings s;
  const QString value = s.value(QLatin1String(key), QString{}).toString();
  const QString sanitized = SanitizedCredentialSlot(value);
  if (sanitized != value) {
    if (sanitized.isEmpty()) {
      s.remove(QLatin1String(key));
    } else {
      s.setValue(QLatin1String(key), sanitized);
    }
  }
  return sanitized;
}

}  // namespace

AiProviderPresetController::AiProviderPresetController(QObject* parent) : QObject(parent) {}

AiProviderPreset AiProviderPresetController::CurrentPreset() const {
  AiProviderPreset preset;
  preset.provider_id            = read_non_secret_string(kKeyProviderId, QLatin1String(kDefaultProviderId));
  preset.display_name           = read_non_secret_string(kKeyDisplayName, QString{});
  preset.protocol_family        = NormalizedProtocolFamily(read_string(kKeyProtocolFamily, kDefaultProtocolFamily));
  preset.base_url               = read_non_secret_string(kKeyBaseUrl, QString{});
  preset.endpoint               = read_non_secret_string(kKeyEndpoint, QString{});
  preset.auth_type              = NormalizedAuthType(read_string(kKeyAuthType, kDefaultAuthType));
  preset.credential_slot        = read_credential_slot(kKeyCredentialSlot);
  preset.model_id               = read_non_secret_string(kKeyModelId, QString{});
  preset.model_display_name     = read_non_secret_string(kKeyModelDisplayName, QString{});
  preset.structured_output_mode = NormalizedStructuredOutputMode(
      read_string(kKeyStructuredOutput, kDefaultStructuredOutputMode));
  preset.timeout_ms            = NormalizedTimeoutMs(read_int(kKeyTimeoutMs, kDefaultTimeoutMs));
  preset.max_image_bytes       = NormalizedMaxImageBytes(read_int(kKeyMaxImageBytes, kDefaultMaxImageBytes));
  preset.recommended_rendition = NormalizedRendition(read_string(kKeyRecommendedRendition, kDefaultRendition));
  preset.masked_key_label      = read_non_secret_string(kKeyMaskedKeyLabel, QString{});
  preset.remember_key          = read_bool(kKeyRememberKey, false);
  preset.output_language       = NormalizedOutputLanguage(read_string(kKeyOutputLanguage, kDefaultOutputLanguage));
  return preset;
}

void AiProviderPresetController::SetFromPreset(const AiProviderPreset& preset) {
  QSettings s;
  s.setValue(QLatin1String(kKeyProviderId), SanitizedNonSecretString(preset.provider_id));
  s.setValue(QLatin1String(kKeyDisplayName), SanitizedNonSecretString(preset.display_name));
  s.setValue(QLatin1String(kKeyProtocolFamily), NormalizedProtocolFamily(preset.protocol_family));
  s.setValue(QLatin1String(kKeyBaseUrl), SanitizedNonSecretString(preset.base_url));
  s.setValue(QLatin1String(kKeyEndpoint), SanitizedNonSecretString(preset.endpoint));
  s.setValue(QLatin1String(kKeyAuthType), NormalizedAuthType(preset.auth_type));
  s.setValue(QLatin1String(kKeyCredentialSlot), SanitizedCredentialSlot(preset.credential_slot));
  s.setValue(QLatin1String(kKeyModelId), SanitizedNonSecretString(preset.model_id));
  s.setValue(QLatin1String(kKeyModelDisplayName), SanitizedNonSecretString(preset.model_display_name));
  s.setValue(QLatin1String(kKeyStructuredOutput),
             NormalizedStructuredOutputMode(preset.structured_output_mode));
  s.setValue(QLatin1String(kKeyTimeoutMs), NormalizedTimeoutMs(preset.timeout_ms));
  s.setValue(QLatin1String(kKeyMaxImageBytes), NormalizedMaxImageBytes(preset.max_image_bytes));
  s.setValue(QLatin1String(kKeyRecommendedRendition), NormalizedRendition(preset.recommended_rendition));
  s.setValue(QLatin1String(kKeyMaskedKeyLabel), SanitizedNonSecretString(preset.masked_key_label));
  s.setValue(QLatin1String(kKeyRememberKey), preset.remember_key);
  s.setValue(QLatin1String(kKeyOutputLanguage), NormalizedOutputLanguage(preset.output_language));
  emit PresetChanged();
}

void AiProviderPresetController::Clear() {
  QSettings s;
  s.remove(QLatin1String(kGroup));
  emit PresetChanged();
}

QString AiProviderPresetController::ProviderId() const {
  return CurrentPreset().provider_id;
}
QString AiProviderPresetController::DisplayName() const {
  return CurrentPreset().display_name;
}
QString AiProviderPresetController::ProtocolFamily() const {
  return CurrentPreset().protocol_family;
}
QString AiProviderPresetController::BaseUrl() const {
  return CurrentPreset().base_url;
}
QString AiProviderPresetController::Endpoint() const {
  return CurrentPreset().endpoint;
}
QString AiProviderPresetController::AuthType() const {
  return CurrentPreset().auth_type;
}
QString AiProviderPresetController::CredentialSlot() const {
  return CurrentPreset().credential_slot;
}
QString AiProviderPresetController::ModelId() const {
  return CurrentPreset().model_id;
}
QString AiProviderPresetController::ModelDisplayName() const {
  return CurrentPreset().model_display_name;
}
QString AiProviderPresetController::StructuredOutputMode() const {
  return CurrentPreset().structured_output_mode;
}
qint64 AiProviderPresetController::TimeoutMs() const {
  return CurrentPreset().timeout_ms;
}
qint64 AiProviderPresetController::MaxImageBytes() const {
  return CurrentPreset().max_image_bytes;
}
QString AiProviderPresetController::RecommendedRendition() const {
  return CurrentPreset().recommended_rendition;
}
QString AiProviderPresetController::MaskedKeyLabel() const {
  return CurrentPreset().masked_key_label;
}
bool AiProviderPresetController::RememberKey() const {
  return CurrentPreset().remember_key;
}
QString AiProviderPresetController::OutputLanguage() const {
  return CurrentPreset().output_language;
}

void AiProviderPresetController::SetProviderId(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyProviderId), SanitizedNonSecretString(value));
  emit PresetChanged();
}
void AiProviderPresetController::SetDisplayName(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyDisplayName), SanitizedNonSecretString(value));
  emit PresetChanged();
}
void AiProviderPresetController::SetProtocolFamily(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyProtocolFamily), NormalizedProtocolFamily(value));
  emit PresetChanged();
}
void AiProviderPresetController::SetBaseUrl(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyBaseUrl), SanitizedNonSecretString(value));
  emit PresetChanged();
}
void AiProviderPresetController::SetEndpoint(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyEndpoint), SanitizedNonSecretString(value));
  emit PresetChanged();
}
void AiProviderPresetController::SetAuthType(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyAuthType), NormalizedAuthType(value));
  emit PresetChanged();
}
void AiProviderPresetController::SetCredentialSlot(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyCredentialSlot), SanitizedCredentialSlot(value));
  emit PresetChanged();
}
void AiProviderPresetController::SetModelId(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyModelId), SanitizedNonSecretString(value));
  emit PresetChanged();
}
void AiProviderPresetController::SetModelDisplayName(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyModelDisplayName), SanitizedNonSecretString(value));
  emit PresetChanged();
}
void AiProviderPresetController::SetStructuredOutputMode(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyStructuredOutput), NormalizedStructuredOutputMode(value));
  emit PresetChanged();
}
void AiProviderPresetController::SetTimeoutMs(qint64 value) {
  QSettings().setValue(QLatin1String(kKeyTimeoutMs), NormalizedTimeoutMs(value));
  emit PresetChanged();
}
void AiProviderPresetController::SetMaxImageBytes(qint64 value) {
  QSettings().setValue(QLatin1String(kKeyMaxImageBytes), NormalizedMaxImageBytes(value));
  emit PresetChanged();
}
void AiProviderPresetController::SetRecommendedRendition(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyRecommendedRendition), NormalizedRendition(value));
  emit PresetChanged();
}
void AiProviderPresetController::SetMaskedKeyLabel(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyMaskedKeyLabel), SanitizedNonSecretString(value));
  emit PresetChanged();
}
void AiProviderPresetController::SetRememberKey(bool value) {
  QSettings().setValue(QLatin1String(kKeyRememberKey), value);
  emit PresetChanged();
}
void AiProviderPresetController::SetOutputLanguage(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyOutputLanguage), NormalizedOutputLanguage(value));
  emit PresetChanged();
}

QString AiProviderPresetController::NormalizedProtocolFamily(const QString& value) {
  const QString v = value.trimmed();
  if (v == QLatin1String("openai_chat_compatible") || v == QLatin1String("anthropic_messages")) {
    return v;
  }
  return QLatin1String(kDefaultProtocolFamily);
}

QString AiProviderPresetController::NormalizedAuthType(const QString& value) {
  const QString v = value.trimmed();
  if (v == QLatin1String("bearer") || v == QLatin1String("api_key_header") || v == QLatin1String("none")) {
    return v;
  }
  return QLatin1String(kDefaultAuthType);
}

QString AiProviderPresetController::NormalizedStructuredOutputMode(const QString& value) {
  const QString v = value.trimmed();
  if (v == QLatin1String("response_format_json_schema") || v == QLatin1String("responses_json_schema") ||
      v == QLatin1String("tool") || v == QLatin1String("none")) {
    return v;
  }
  return QLatin1String(kDefaultStructuredOutputMode);
}

QString AiProviderPresetController::NormalizedRendition(const QString& value) {
  const QString v = value.trimmed();
  if (v == QLatin1String("thumbnail") || v == QLatin1String("preview") || v == QLatin1String("image")) {
    return v;
  }
  return QLatin1String(kDefaultRendition);
}

QString AiProviderPresetController::NormalizedOutputLanguage(const QString& value) {
  const QString v = value.trimmed().toLower();
  if (v == QLatin1String("follow") || v == QLatin1String("en") || v == QLatin1String("zh")) {
    return v;
  }
  return QLatin1String(kDefaultOutputLanguage);
}

qint64 AiProviderPresetController::NormalizedTimeoutMs(qint64 value) {
  // std::clamp clamps negative / sub-min / over-max values into the valid range
  // (matches the Rust limits validation, but fail-soft here: clamp rather than
  // reject, since the value may come from a stale QSettings on an older install).
  return std::clamp(value, kMinTimeoutMs, kMaxTimeoutMs);
}

qint64 AiProviderPresetController::NormalizedMaxImageBytes(qint64 value) {
  return std::clamp(value, kMinMaxImageBytes, kMaxMaxImageBytes);
}

}  // namespace alcedo
