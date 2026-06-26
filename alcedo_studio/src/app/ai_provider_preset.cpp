//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/ai_provider_preset.hpp"

#include <QLatin1String>
#include <QSettings>
#include <QStringList>

namespace alcedo {
namespace {

// QSettings keys for the selected compatible-protocol preset. All non-secret.
// Grouped under "ai/preset/" so a single Remove() clears the whole preset.
constexpr auto kGroup                = "ai/preset";
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

// Defaults mirror the primary product-facing Opencode Go Anthropic-compatible
// preset (the proven compatible path). They are used when a key is absent so a
// fresh install lands on a sensible preset the user can edit.
constexpr auto kDefaultProtocolFamily       = "anthropic_messages";
constexpr auto kDefaultAuthType             = "bearer";
constexpr auto kDefaultStructuredOutputMode = "tool";
constexpr auto kDefaultRendition            = "preview";
constexpr qint64 kDefaultTimeoutMs          = 60000;
constexpr qint64 kDefaultMaxImageBytes      = 4194304;

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

}  // namespace

AiProviderPresetController::AiProviderPresetController(QObject* parent) : QObject(parent) {}

AiProviderPreset AiProviderPresetController::CurrentPreset() const {
  AiProviderPreset preset;
  preset.display_name           = read_string(kKeyDisplayName, QString{});
  preset.protocol_family        = NormalizedProtocolFamily(read_string(kKeyProtocolFamily, kDefaultProtocolFamily));
  preset.base_url               = read_string(kKeyBaseUrl, QString{});
  preset.endpoint               = read_string(kKeyEndpoint, QString{});
  preset.auth_type              = NormalizedAuthType(read_string(kKeyAuthType, kDefaultAuthType));
  preset.credential_slot        = read_string(kKeyCredentialSlot, QString{});
  preset.model_id               = read_string(kKeyModelId, QString{});
  preset.model_display_name     = read_string(kKeyModelDisplayName, QString{});
  preset.structured_output_mode = NormalizedStructuredOutputMode(
      read_string(kKeyStructuredOutput, kDefaultStructuredOutputMode));
  preset.timeout_ms            = read_int(kKeyTimeoutMs, kDefaultTimeoutMs);
  preset.max_image_bytes       = read_int(kKeyMaxImageBytes, kDefaultMaxImageBytes);
  preset.recommended_rendition = NormalizedRendition(read_string(kKeyRecommendedRendition, kDefaultRendition));
  preset.masked_key_label      = read_string(kKeyMaskedKeyLabel, QString{});
  preset.remember_key          = read_bool(kKeyRememberKey, false);
  return preset;
}

void AiProviderPresetController::SetFromPreset(const AiProviderPreset& preset) {
  QSettings s;
  s.setValue(QLatin1String(kKeyDisplayName), preset.display_name);
  s.setValue(QLatin1String(kKeyProtocolFamily), NormalizedProtocolFamily(preset.protocol_family));
  s.setValue(QLatin1String(kKeyBaseUrl), preset.base_url);
  s.setValue(QLatin1String(kKeyEndpoint), preset.endpoint);
  s.setValue(QLatin1String(kKeyAuthType), NormalizedAuthType(preset.auth_type));
  s.setValue(QLatin1String(kKeyCredentialSlot), preset.credential_slot);
  s.setValue(QLatin1String(kKeyModelId), preset.model_id);
  s.setValue(QLatin1String(kKeyModelDisplayName), preset.model_display_name);
  s.setValue(QLatin1String(kKeyStructuredOutput),
             NormalizedStructuredOutputMode(preset.structured_output_mode));
  s.setValue(QLatin1String(kKeyTimeoutMs), preset.timeout_ms);
  s.setValue(QLatin1String(kKeyMaxImageBytes), preset.max_image_bytes);
  s.setValue(QLatin1String(kKeyRecommendedRendition), NormalizedRendition(preset.recommended_rendition));
  s.setValue(QLatin1String(kKeyMaskedKeyLabel), preset.masked_key_label);
  s.setValue(QLatin1String(kKeyRememberKey), preset.remember_key);
  emit PresetChanged();
}

void AiProviderPresetController::Clear() {
  QSettings s;
  s.remove(QLatin1String(kGroup));
  emit PresetChanged();
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

void AiProviderPresetController::SetDisplayName(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyDisplayName), value);
  emit PresetChanged();
}
void AiProviderPresetController::SetProtocolFamily(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyProtocolFamily), NormalizedProtocolFamily(value));
  emit PresetChanged();
}
void AiProviderPresetController::SetBaseUrl(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyBaseUrl), value);
  emit PresetChanged();
}
void AiProviderPresetController::SetEndpoint(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyEndpoint), value);
  emit PresetChanged();
}
void AiProviderPresetController::SetAuthType(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyAuthType), NormalizedAuthType(value));
  emit PresetChanged();
}
void AiProviderPresetController::SetCredentialSlot(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyCredentialSlot), value);
  emit PresetChanged();
}
void AiProviderPresetController::SetModelId(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyModelId), value);
  emit PresetChanged();
}
void AiProviderPresetController::SetModelDisplayName(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyModelDisplayName), value);
  emit PresetChanged();
}
void AiProviderPresetController::SetStructuredOutputMode(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyStructuredOutput), NormalizedStructuredOutputMode(value));
  emit PresetChanged();
}
void AiProviderPresetController::SetTimeoutMs(qint64 value) {
  QSettings().setValue(QLatin1String(kKeyTimeoutMs), value);
  emit PresetChanged();
}
void AiProviderPresetController::SetMaxImageBytes(qint64 value) {
  QSettings().setValue(QLatin1String(kKeyMaxImageBytes), value);
  emit PresetChanged();
}
void AiProviderPresetController::SetRecommendedRendition(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyRecommendedRendition), NormalizedRendition(value));
  emit PresetChanged();
}
void AiProviderPresetController::SetMaskedKeyLabel(const QString& value) {
  QSettings().setValue(QLatin1String(kKeyMaskedKeyLabel), value);
  emit PresetChanged();
}
void AiProviderPresetController::SetRememberKey(bool value) {
  QSettings().setValue(QLatin1String(kKeyRememberKey), value);
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

}  // namespace alcedo
