//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/ai_provider_preset.hpp"

#include <algorithm>

#include <QChar>
#include <QLatin1String>
#include <QRegularExpression>
#include <QSettings>
#include <QVariantMap>
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

struct BuiltinProviderProtocol {
  const char* provider_key;
  const char* provider_label;
  const char* provider_help;
  const char* provider_id;
  const char* display_name;
  const char* protocol_label;
  const char* protocol_family;
  const char* driver;
  const char* base_url;
  const char* endpoint;
  const char* auth_type;
  const char* credential_slot;
  const char* structured_output_mode;
  const char* default_model_id;
  const char* default_model_display_name;
  const char* recommended_rendition;
  qint64 timeout_ms;
  qint64 max_image_bytes;
};

constexpr BuiltinProviderProtocol kBuiltinProviderProtocols[] = {
    {"opencode", "OpenCode", "Shared OpenCode account; both request paths use opencode_api_key.",
     "opencode_go_anthropic", "OpenCode - Anthropic-compatible messages",
     "Anthropic-compatible messages", "anthropic_messages", "anthropic_messages",
     "https://opencode.ai/zen/go/v1", "/messages", "bearer", "opencode_api_key", "tool",
     "claude-sonnet-4-5", "Claude Sonnet 4.5", "preview", 60000, 4194304},
    {"opencode", "OpenCode", "Shared OpenCode account; both request paths use opencode_api_key.",
     "opencode_go_openai", "OpenCode - OpenAI-compatible chat", "OpenAI-compatible chat",
     "openai_chat_compatible", "openai_chat_compatible", "https://opencode.ai/zen/go/v1",
     "/chat/completions", "bearer", "opencode_api_key", "response_format_json_schema",
     "gpt-4o", "GPT-4o", "preview", 60000, 4194304},
    {"volcengine_ark", "Volcengine Ark / 火山方舟",
     "Shared Volcengine Ark account; both request paths use volcengine_ark_api_key.",
     "volcengine_ark", "Volcengine Ark / 火山方舟 - Responses", "Volcengine Ark responses",
     "volcengine_ark_responses", "volcengine_ark_responses",
     "https://ark.cn-beijing.volces.com/api/v3", "/responses", "bearer",
     "volcengine_ark_api_key", "responses_json_schema", "doubao-seed-2-0-lite-260428",
     "Doubao Seed 2.0 Lite (260428)", "preview", 60000, 4194304},
    {"volcengine_ark", "Volcengine Ark / 火山方舟",
     "Shared Volcengine Ark account; both request paths use volcengine_ark_api_key.",
     "volcengine_ark_coding", "Volcengine Ark / 火山方舟 - Anthropic-compatible messages",
     "Anthropic-compatible messages", "anthropic_messages", "anthropic_messages",
     "https://ark.cn-beijing.volces.com/api/coding", "/v1/messages", "bearer",
     "volcengine_ark_api_key", "tool", "doubao-seed-2.0-lite", "Doubao Seed 2.0 Lite",
     "preview", 60000, 4194304},
};

const BuiltinProviderProtocol* FindBuiltinProtocol(const QString& provider_key,
                                                   const QString& protocol_family) {
  const QString key = provider_key.trimmed();
  const QString fam = protocol_family.trimmed();
  for (const auto& option : kBuiltinProviderProtocols) {
    if (key == QLatin1String(option.provider_key) && fam == QLatin1String(option.protocol_family)) {
      return &option;
    }
  }
  return nullptr;
}

const BuiltinProviderProtocol* FindBuiltinByProviderId(const QString& provider_id) {
  const QString id = provider_id.trimmed();
  for (const auto& option : kBuiltinProviderProtocols) {
    if (id == QLatin1String(option.provider_id)) {
      return &option;
    }
  }
  return nullptr;
}

const BuiltinProviderProtocol& DefaultBuiltinProtocol() {
  return kBuiltinProviderProtocols[0];
}

QVariantMap BuiltinProtocolMap(const BuiltinProviderProtocol& option) {
  QVariantMap m;
  m.insert(QStringLiteral("providerKey"), QString::fromLatin1(option.provider_key));
  m.insert(QStringLiteral("providerLabel"), QString::fromUtf8(option.provider_label));
  m.insert(QStringLiteral("providerHelp"), QString::fromUtf8(option.provider_help));
  m.insert(QStringLiteral("providerId"), QString::fromLatin1(option.provider_id));
  m.insert(QStringLiteral("displayName"), QString::fromUtf8(option.display_name));
  m.insert(QStringLiteral("protocolLabel"), QString::fromUtf8(option.protocol_label));
  m.insert(QStringLiteral("protocolFamily"), QString::fromLatin1(option.protocol_family));
  m.insert(QStringLiteral("driver"), QString::fromLatin1(option.driver));
  m.insert(QStringLiteral("baseUrl"), QString::fromLatin1(option.base_url));
  m.insert(QStringLiteral("endpoint"), QString::fromLatin1(option.endpoint));
  m.insert(QStringLiteral("authType"), QString::fromLatin1(option.auth_type));
  m.insert(QStringLiteral("credentialSlot"), QString::fromLatin1(option.credential_slot));
  m.insert(QStringLiteral("structuredOutputMode"), QString::fromLatin1(option.structured_output_mode));
  m.insert(QStringLiteral("modelId"), QString::fromLatin1(option.default_model_id));
  m.insert(QStringLiteral("modelDisplayName"), QString::fromUtf8(option.default_model_display_name));
  m.insert(QStringLiteral("recommendedRendition"), QString::fromLatin1(option.recommended_rendition));
  m.insert(QStringLiteral("timeoutMs"), option.timeout_ms);
  m.insert(QStringLiteral("maxImageBytes"), option.max_image_bytes);
  return m;
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
  const auto& d = DefaultBuiltinProtocol();
  AiProviderPreset preset;
  preset.provider_id = read_non_secret_string(kKeyProviderId, QLatin1String(d.provider_id));
  const auto* builtin = FindBuiltinByProviderId(preset.provider_id);
  if (builtin == nullptr) {
    builtin = &d;
  }
  preset.display_name = read_non_secret_string(kKeyDisplayName, QString::fromUtf8(builtin->display_name));
  preset.protocol_family = NormalizedProtocolFamily(
      read_string(kKeyProtocolFamily, QLatin1String(builtin->protocol_family)));
  preset.base_url = read_non_secret_string(kKeyBaseUrl, QLatin1String(builtin->base_url));
  preset.endpoint = read_non_secret_string(kKeyEndpoint, QLatin1String(builtin->endpoint));
  preset.auth_type = NormalizedAuthType(read_string(kKeyAuthType, QLatin1String(builtin->auth_type)));
  preset.credential_slot = read_credential_slot(kKeyCredentialSlot);
  if (preset.credential_slot.isEmpty()) {
    preset.credential_slot = QString::fromLatin1(builtin->credential_slot);
  }
  preset.model_id = read_non_secret_string(kKeyModelId, QLatin1String(builtin->default_model_id));
  preset.model_display_name = read_non_secret_string(
      kKeyModelDisplayName, QString::fromUtf8(builtin->default_model_display_name));
  preset.structured_output_mode = NormalizedStructuredOutputMode(
      read_string(kKeyStructuredOutput, QLatin1String(builtin->structured_output_mode)));
  preset.timeout_ms            = NormalizedTimeoutMs(read_int(kKeyTimeoutMs, kDefaultTimeoutMs));
  preset.max_image_bytes       = NormalizedMaxImageBytes(read_int(kKeyMaxImageBytes, kDefaultMaxImageBytes));
  preset.recommended_rendition = NormalizedRendition(read_string(kKeyRecommendedRendition, QLatin1String(builtin->recommended_rendition)));
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

QVariantList AiProviderPresetController::BuiltinProviderOptions() const {
  QVariantList out;
  QStringList  seen;
  for (const auto& option : kBuiltinProviderProtocols) {
    const QString key = QString::fromLatin1(option.provider_key);
    if (seen.contains(key)) {
      continue;
    }
    seen.push_back(key);
    QVariantMap m;
    m.insert(QStringLiteral("providerKey"), key);
    m.insert(QStringLiteral("label"), QString::fromUtf8(option.provider_label));
    m.insert(QStringLiteral("help"), QString::fromUtf8(option.provider_help));
    m.insert(QStringLiteral("credentialSlot"), QString::fromLatin1(option.credential_slot));
    out.push_back(m);
  }
  QVariantMap custom;
  custom.insert(QStringLiteral("providerKey"), QStringLiteral("custom"));
  custom.insert(QStringLiteral("label"), QStringLiteral("Custom"));
  custom.insert(QStringLiteral("help"), QStringLiteral("Use advanced fields for a custom provider."));
  custom.insert(QStringLiteral("credentialSlot"), QString{});
  out.push_back(custom);
  return out;
}

QVariantList AiProviderPresetController::BuiltinProtocolOptions(const QString& provider_key) const {
  QVariantList out;
  const QString key = provider_key.trimmed();
  for (const auto& option : kBuiltinProviderProtocols) {
    if (key == QLatin1String(option.provider_key)) {
      out.push_back(BuiltinProtocolMap(option));
    }
  }
  return out;
}

bool AiProviderPresetController::ApplyBuiltinProviderProtocol(const QString& provider_key,
                                                              const QString& protocol_family) {
  const auto* option = FindBuiltinProtocol(provider_key, protocol_family);
  if (option == nullptr) {
    return false;
  }
  AiProviderPreset preset;
  preset.provider_id            = QString::fromLatin1(option->provider_id);
  preset.display_name           = QString::fromUtf8(option->display_name);
  preset.protocol_family        = QString::fromLatin1(option->protocol_family);
  preset.base_url               = QString::fromLatin1(option->base_url);
  preset.endpoint               = QString::fromLatin1(option->endpoint);
  preset.auth_type              = QString::fromLatin1(option->auth_type);
  preset.credential_slot        = QString::fromLatin1(option->credential_slot);
  preset.model_id               = QString::fromLatin1(option->default_model_id);
  preset.model_display_name     = QString::fromUtf8(option->default_model_display_name);
  preset.structured_output_mode = QString::fromLatin1(option->structured_output_mode);
  preset.timeout_ms             = option->timeout_ms;
  preset.max_image_bytes        = option->max_image_bytes;
  preset.recommended_rendition  = QString::fromLatin1(option->recommended_rendition);
  preset.masked_key_label       = CurrentPreset().masked_key_label;
  preset.remember_key           = CurrentPreset().remember_key;
  preset.output_language        = CurrentPreset().output_language;
  SetFromPreset(preset);
  return true;
}
QString AiProviderPresetController::NormalizedProtocolFamily(const QString& value) {
  const QString v = value.trimmed();
  if (v == QLatin1String("openai_chat_compatible") || v == QLatin1String("anthropic_messages") ||
      v == QLatin1String("volcengine_ark_responses")) {
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
