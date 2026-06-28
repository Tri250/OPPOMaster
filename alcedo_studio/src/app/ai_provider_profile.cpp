//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/ai_provider_profile.hpp"

#include <QDateTime>
#include <QDir>
#include <QFile>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QJsonValue>
#include <QRegularExpression>
#include <QSaveFile>
#include <QStandardPaths>
#include <QUuid>
#include <algorithm>

namespace alcedo {
namespace {

constexpr int    kStoreSchemaVersion = 1;
constexpr qint64 kMinTimeoutMs       = 1000;
constexpr qint64 kMaxTimeoutMs       = 300000;
constexpr qint64 kMinImageBytes      = 1;
constexpr qint64 kMaxImageBytes      = 16 * 1024 * 1024;
constexpr qint64 kMinOutputTokens    = 1;
constexpr qint64 kMaxOutputTokens    = 8192;
constexpr double kDefaultTemperature = 0.2;

#ifdef _WIN32
std::filesystem::path FsPathFromQString(const QString& path) {
  return std::filesystem::path(path.toStdWString());
}
#else
std::filesystem::path FsPathFromQString(const QString& path) {
  return std::filesystem::path(path.toStdString());
}
#endif

QString QStringFromFsPath(const std::filesystem::path& path) {
#ifdef _WIN32
  return QString::fromStdWString(path.wstring());
#else
  return QString::fromStdString(path.string());
#endif
}

bool LooksLikeRawSecret(const QString& value) {
  const QString                   v = value.trimmed();
  static const QRegularExpression kOpenAiStyleKey(QStringLiteral("sk-[A-Za-z0-9_-]{16,}"));
  static const QRegularExpression kBearerValue(QStringLiteral("Bearer\\s+\\S{8,}"),
                                               QRegularExpression::CaseInsensitiveOption);
  static const QRegularExpression kAwsAccessKey(QStringLiteral("AKIA[A-Z0-9]{16}"));
  return kOpenAiStyleKey.match(v).hasMatch() || kBearerValue.match(v).hasMatch() ||
         kAwsAccessKey.match(v).hasMatch();
}

bool IsSlotChar(QChar c) {
  const ushort ch = c.unicode();
  return (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_';
}

QString SanitizedNonSecretString(const QString& value) {
  return LooksLikeRawSecret(value) ? QString{} : value.trimmed();
}

QString SanitizedId(const QString& value) {
  const QString v = SanitizedNonSecretString(value).toLower();
  if (v.isEmpty()) {
    return QString{};
  }
  for (QChar c : v) {
    if (!IsSlotChar(c)) {
      return QString{};
    }
  }
  return v;
}

QString NormalizedProtocolDriver(const QString& value) {
  const QString v = SanitizedNonSecretString(value);
  if (v == QStringLiteral("openai_chat_compatible") || v == QStringLiteral("anthropic_messages") ||
      v == QStringLiteral("volcengine_ark_responses") ||
      v == QStringLiteral("volcengine_ark_chat") || v == QStringLiteral("openai_responses") ||
      v == QStringLiteral("gemini_generate_content") || v == QStringLiteral("generic_json_http")) {
    return v;
  }
  return QStringLiteral("anthropic_messages");
}

QString NormalizedAuthType(const QString& value) {
  const QString v = SanitizedNonSecretString(value);
  if (v == QStringLiteral("bearer") || v == QStringLiteral("api_key_header") ||
      v == QStringLiteral("none")) {
    return v;
  }
  return QStringLiteral("bearer");
}

QString NormalizedStructuredOutputMode(const QString& value) {
  const QString v = SanitizedNonSecretString(value);
  if (v == QStringLiteral("response_format_json_schema") ||
      v == QStringLiteral("responses_json_schema") || v == QStringLiteral("tool") ||
      v == QStringLiteral("none")) {
    return v;
  }
  return QStringLiteral("tool");
}

QString NormalizedRendition(const QString& value) {
  const QString v = SanitizedNonSecretString(value);
  if (v == QStringLiteral("thumbnail") || v == QStringLiteral("preview") ||
      v == QStringLiteral("image")) {
    return v;
  }
  return QStringLiteral("preview");
}

QString NormalizedOutputLanguage(const QString& value) {
  const QString v = SanitizedNonSecretString(value).toLower();
  if (v == QStringLiteral("follow") || v == QStringLiteral("en") || v == QStringLiteral("zh")) {
    return v;
  }
  return QStringLiteral("follow");
}

qint64  Clamp(qint64 value, qint64 lo, qint64 hi) { return std::clamp(value, lo, hi); }

QString NewIdSuffix() {
  QString id = QUuid::createUuid().toString(QUuid::WithoutBraces).toLower();
  id.remove(QLatin1Char('-'));
  return id;
}

bool VariantBoolWithFallback(const QVariantMap& map, const QString& camel_key,
                             const QString& snake_key, bool fallback) {
  const QVariant camel = map.value(camel_key);
  if (camel.isValid()) {
    return camel.toBool();
  }
  const QVariant snake = map.value(snake_key);
  return snake.isValid() ? snake.toBool() : fallback;
}
QString MaskedKeyLabel(const std::string& secret) {
  if (secret.empty()) {
    return {};
  }
  const size_t tail = std::min<size_t>(secret.size(), 4);
  return QStringLiteral("****") +
         QString::fromUtf8(secret.data() + static_cast<int>(secret.size() - tail),
                           static_cast<int>(tail));
}

void ClearSecret(std::string* secret) {
  if (!secret) {
    return;
  }
  std::fill(secret->begin(), secret->end(), '0');
  secret->clear();
  secret->shrink_to_fit();
}

struct TemplateConfig {
  const char* template_id;
  const char* label;
  const char* driver;
  const char* base_url;
  const char* endpoint;
  const char* models_endpoint;
  const char* auth_type;
  const char* model_id;
  const char* model_display_name;
  const char* structured_output_mode;
  const char* response_content_json_pointer;
  const char* response_usage_json_pointer;
  const char* response_provider_request_id_json_pointer;
  const char* response_provider_request_id_header;
  const char* recommended_rendition;
  bool        supports_vision;
  bool        supports_structured_output;
  qint64      timeout_ms;
  qint64      max_image_bytes;
};

constexpr TemplateConfig kTemplates[] = {
    {"opencode_go_anthropic", "OpenCode - Anthropic-compatible messages", "anthropic_messages",
     "https://opencode.ai/zen/go/v1", "/messages", "", "api_key_header", "qwen3.7-plus",
     "Qwen3.7 Plus", "tool", "", "/usage", "", "request-id", "preview", true, true, 60000,
     4194304},
    {"opencode_go_openai", "OpenCode - OpenAI-compatible chat", "openai_chat_compatible",
     "https://opencode.ai/zen/go/v1", "/chat/completions", "", "bearer", "kimi-k2.7-code",
     "Kimi K2.7 Code", "response_format_json_schema", "/choices/0/message/content", "/usage", "/id", "",
     "preview", true, true, 60000, 4194304},
    {"volcengine_ark", "Volcengine Ark / 火山方舟", "volcengine_ark_responses",
     "https://ark.cn-beijing.volces.com/api/v3", "/responses", "", "bearer",
     "doubao-seed-2-0-lite-260428", "Doubao Seed 2.0 Lite (260428)", "responses_json_schema", "",
     "/usage", "/id", "", "preview", true, true, 60000, 4194304},
    {"volcengine_ark_coding", "Volcengine Ark Coding Plan - Anthropic-compatible",
     "anthropic_messages", "https://ark.cn-beijing.volces.com/api/coding", "/v1/messages", "",
     "bearer", "doubao-seed-2.0-lite", "Doubao Seed 2.0 Lite", "tool", "", "/usage", "/id", "",
     "preview", true, true, 60000, 4194304},
    {"custom", "Custom", "openai_chat_compatible", "http://localhost:11434/v1", "/chat/completions",
     "", "none", "unconfigured", "Unconfigured", "response_format_json_schema",
     "/choices/0/message/content", "/usage", "/id", "", "preview", true, true, 60000, 4194304},
};

const TemplateConfig* FindTemplate(const QString& template_id) {
  const QString id = template_id.trimmed();
  for (const auto& t : kTemplates) {
    if (id == QLatin1String(t.template_id)) {
      return &t;
    }
  }
  return nullptr;
}

AiProviderModelEntry ModelFromTemplate(const TemplateConfig& t) {
  AiProviderModelEntry m;
  m.model_id                   = QString::fromLatin1(t.model_id);
  m.display_name               = QString::fromUtf8(t.model_display_name);
  m.supports_vision            = t.supports_vision;
  m.supports_structured_output = t.supports_structured_output;
  m.max_image_bytes            = t.max_image_bytes;
  m.recommended_rendition      = QString::fromLatin1(t.recommended_rendition);
  return m;
}

void AddOrUpdateModel(AiProviderProfile* profile, const QString& model_id, const QString& display_name,
                      bool supports_vision, bool supports_structured_output) {
  if (profile == nullptr || model_id.isEmpty()) {
    return;
  }
  auto it = std::find_if(profile->models.begin(), profile->models.end(),
                         [&](const auto& model) { return model.model_id == model_id; });
  if (it == profile->models.end()) {
    AiProviderModelEntry model;
    model.model_id                   = model_id;
    model.display_name               = display_name.isEmpty() ? model_id : display_name;
    model.supports_vision            = supports_vision;
    model.supports_structured_output = supports_structured_output;
    model.max_image_bytes            = profile->max_image_bytes;
    model.recommended_rendition      = profile->recommended_rendition;
    profile->models.push_back(model);
    return;
  }
  if (!display_name.isEmpty()) {
    it->display_name = display_name;
  }
  it->supports_vision            = supports_vision;
  it->supports_structured_output = supports_structured_output;
}

void EnsureOpenCodeGoOpenAiModelAliases(AiProviderProfile* profile) {
  if (profile == nullptr || profile->based_on_template != QStringLiteral("opencode_go_openai")) {
    return;
  }
  AddOrUpdateModel(profile, QStringLiteral("kimi-k2.7-code"), QStringLiteral("Kimi K2.7 Code"), true,
                   true);
  AddOrUpdateModel(profile, QStringLiteral("kimi-k2.7"), QStringLiteral("Kimi K2.7"), true, true);
}

void MigrateOpenCodeGoProfileDefaults(AiProviderProfile* profile) {
  if (profile == nullptr) {
    return;
  }
  if (profile->based_on_template == QStringLiteral("opencode_go_anthropic") &&
      profile->auth_type != QStringLiteral("api_key_header")) {
    profile->auth_type = QStringLiteral("api_key_header");
  }
  if (profile->based_on_template == QStringLiteral("opencode_go_anthropic") &&
      profile->model_id == QStringLiteral("claude-sonnet-4-5")) {
    profile->model_id           = QStringLiteral("qwen3.7-plus");
    profile->model_display_name = QStringLiteral("Qwen3.7 Plus");
    AddOrUpdateModel(profile, profile->model_id, profile->model_display_name, true, true);
  } else if (profile->based_on_template == QStringLiteral("opencode_go_openai") &&
             profile->model_id == QStringLiteral("gpt-4o")) {
    profile->model_id           = QStringLiteral("kimi-k2.7-code");
    profile->model_display_name = QStringLiteral("Kimi K2.7 Code");
    AddOrUpdateModel(profile, profile->model_id, profile->model_display_name, true, true);
  }
  EnsureOpenCodeGoOpenAiModelAliases(profile);
}
AiProviderProfile ProfileFromTemplate(const TemplateConfig& t) {
  const QString     suffix = NewIdSuffix();
  AiProviderProfile p;
  p.uuid                          = QUuid::createUuid().toString(QUuid::WithoutBraces);
  p.display_name                  = QString::fromUtf8(t.label);
  p.based_on_template             = QString::fromLatin1(t.template_id);
  p.credential_slot               = QStringLiteral("alcedo_ai_") + suffix;
  p.provider_id                   = QStringLiteral("profile_") + suffix;
  p.driver                        = QString::fromLatin1(t.driver);
  p.base_url                      = QString::fromLatin1(t.base_url);
  p.endpoint                      = QString::fromLatin1(t.endpoint);
  p.models_endpoint               = QString::fromLatin1(t.models_endpoint);
  p.auth_type                     = QString::fromLatin1(t.auth_type);
  p.model_id                      = QString::fromLatin1(t.model_id);
  p.model_display_name            = QString::fromUtf8(t.model_display_name);
  p.structured_output_mode        = QString::fromLatin1(t.structured_output_mode);
  p.response_content_json_pointer = QString::fromLatin1(t.response_content_json_pointer);
  p.response_usage_json_pointer   = QString::fromLatin1(t.response_usage_json_pointer);
  p.response_provider_request_id_json_pointer =
      QString::fromLatin1(t.response_provider_request_id_json_pointer);
  p.response_provider_request_id_header =
      QString::fromLatin1(t.response_provider_request_id_header);
  p.timeout_ms            = t.timeout_ms;
  p.max_image_bytes       = t.max_image_bytes;
  p.recommended_rendition = QString::fromLatin1(t.recommended_rendition);
  p.models.push_back(ModelFromTemplate(t));
  EnsureOpenCodeGoOpenAiModelAliases(&p);
  return p;
}

QJsonValue OptionalString(const QString& value) {
  return value.trimmed().isEmpty() ? QJsonValue(QJsonValue::Null) : QJsonValue(value.trimmed());
}

QJsonObject ModelToJson(const AiProviderModelEntry& model) {
  QJsonObject o;
  o.insert(QStringLiteral("slug"), model.model_id);
  o.insert(QStringLiteral("display_name"),
           model.display_name.isEmpty() ? model.model_id : model.display_name);
  o.insert(QStringLiteral("supports_vision"), model.supports_vision);
  o.insert(QStringLiteral("supports_structured_output"), model.supports_structured_output);
  o.insert(QStringLiteral("live_confirmed"), model.live_confirmed);
  o.insert(QStringLiteral("max_image_bytes"), static_cast<qint64>(model.max_image_bytes));
  o.insert(QStringLiteral("recommended_rendition"),
           NormalizedRendition(model.recommended_rendition));
  return o;
}

AiProviderModelEntry ModelFromJson(const QJsonObject& o, qint64 fallback_max_bytes,
                                   const QString& fallback_rendition) {
  AiProviderModelEntry m;
  m.model_id = SanitizedNonSecretString(
      o.value(QStringLiteral("modelId")).toString(o.value(QStringLiteral("slug")).toString()));
  m.display_name =
      SanitizedNonSecretString(o.value(QStringLiteral("displayName"))
                                   .toString(o.value(QStringLiteral("display_name")).toString()));
  m.supports_vision = o.value(QStringLiteral("supportsVision"))
                          .toBool(o.value(QStringLiteral("supports_vision")).toBool(true));
  m.supports_structured_output =
      o.value(QStringLiteral("supportsStructuredOutput"))
          .toBool(o.value(QStringLiteral("supports_structured_output")).toBool(true));
  m.live_confirmed = o.value(QStringLiteral("liveConfirmed"))
                         .toBool(o.value(QStringLiteral("live_confirmed")).toBool(false));
  m.max_image_bytes = Clamp(
      static_cast<qint64>(
          o.value(QStringLiteral("maxImageBytes"))
              .toInteger(o.value(QStringLiteral("max_image_bytes")).toInteger(fallback_max_bytes))),
      kMinImageBytes, kMaxImageBytes);
  m.recommended_rendition = NormalizedRendition(
      o.value(QStringLiteral("recommendedRendition"))
          .toString(o.value(QStringLiteral("recommended_rendition")).toString(fallback_rendition)));
  return m;
}

QVariantMap ModelToVariant(const AiProviderModelEntry& model) {
  QVariantMap m;
  m.insert(QStringLiteral("modelId"), model.model_id);
  m.insert(QStringLiteral("displayName"),
           model.display_name.isEmpty() ? model.model_id : model.display_name);
  m.insert(QStringLiteral("supportsVision"), model.supports_vision);
  m.insert(QStringLiteral("supportsStructuredOutput"), model.supports_structured_output);
  m.insert(QStringLiteral("maxImageBytes"), model.max_image_bytes);
  m.insert(QStringLiteral("recommendedRendition"), model.recommended_rendition);
  return m;
}

QVariantList ModelsToVariant(const std::vector<AiProviderModelEntry>& models) {
  QVariantList out;
  for (const auto& model : models) {
    if (!model.model_id.isEmpty()) {
      out.push_back(ModelToVariant(model));
    }
  }
  return out;
}

QJsonObject ProfileToStoreJson(const AiProviderProfile& p) {
  QJsonObject o;
  o.insert(QStringLiteral("uuid"), p.uuid);
  o.insert(QStringLiteral("display_name"), p.display_name);
  o.insert(QStringLiteral("based_on_template"), p.based_on_template);
  o.insert(QStringLiteral("credential_slot"), p.credential_slot);
  o.insert(QStringLiteral("masked_key_label"), p.masked_key_label);
  o.insert(QStringLiteral("remember_key"), p.remember_key);
  o.insert(QStringLiteral("last_used_ms"), p.last_used_ms);
  o.insert(QStringLiteral("provider_id"), p.provider_id);
  o.insert(QStringLiteral("driver"), p.driver);
  o.insert(QStringLiteral("base_url"), p.base_url);
  o.insert(QStringLiteral("endpoint"), p.endpoint);
  o.insert(QStringLiteral("models_endpoint"), p.models_endpoint);
  o.insert(QStringLiteral("auth_type"), p.auth_type);
  o.insert(QStringLiteral("model_id"), p.model_id);
  o.insert(QStringLiteral("model_display_name"), p.model_display_name);
  o.insert(QStringLiteral("structured_output_mode"), p.structured_output_mode);
  o.insert(QStringLiteral("structured_output_strict"), p.structured_output_strict);
  o.insert(QStringLiteral("response_content_json_pointer"), p.response_content_json_pointer);
  o.insert(QStringLiteral("response_usage_json_pointer"), p.response_usage_json_pointer);
  o.insert(QStringLiteral("response_provider_request_id_json_pointer"),
           p.response_provider_request_id_json_pointer);
  o.insert(QStringLiteral("response_provider_request_id_header"),
           p.response_provider_request_id_header);
  o.insert(QStringLiteral("timeout_ms"), p.timeout_ms);
  o.insert(QStringLiteral("max_image_bytes"), p.max_image_bytes);
  o.insert(QStringLiteral("max_output_tokens"), p.max_output_tokens);
  o.insert(QStringLiteral("temperature"), p.temperature);
  o.insert(QStringLiteral("recommended_rendition"), p.recommended_rendition);
  QJsonArray models;
  for (const auto& model : p.models) {
    if (!model.model_id.isEmpty()) {
      models.push_back(ModelToJson(model));
    }
  }
  o.insert(QStringLiteral("models"), models);
  return o;
}

AiProviderProfile ProfileFromStoreJson(const QJsonObject& o) {
  AiProviderProfile p;
  p.uuid         = SanitizedNonSecretString(o.value(QStringLiteral("uuid")).toString());
  p.display_name = SanitizedNonSecretString(o.value(QStringLiteral("display_name")).toString());
  p.based_on_template =
      SanitizedNonSecretString(o.value(QStringLiteral("based_on_template")).toString());
  p.credential_slot = SanitizedId(o.value(QStringLiteral("credential_slot")).toString());
  p.masked_key_label =
      SanitizedNonSecretString(o.value(QStringLiteral("masked_key_label")).toString());
  p.remember_key = o.value(QStringLiteral("remember_key")).toBool(true);
  p.last_used_ms = o.value(QStringLiteral("last_used_ms")).toInteger(0);
  p.provider_id  = SanitizedId(o.value(QStringLiteral("provider_id")).toString());
  p.driver       = NormalizedProtocolDriver(o.value(QStringLiteral("driver")).toString());
  p.base_url     = SanitizedNonSecretString(o.value(QStringLiteral("base_url")).toString());
  p.endpoint     = SanitizedNonSecretString(o.value(QStringLiteral("endpoint")).toString());
  p.models_endpoint =
      SanitizedNonSecretString(o.value(QStringLiteral("models_endpoint")).toString());
  p.auth_type = NormalizedAuthType(o.value(QStringLiteral("auth_type")).toString());
  p.model_id  = SanitizedNonSecretString(o.value(QStringLiteral("model_id")).toString());
  p.model_display_name =
      SanitizedNonSecretString(o.value(QStringLiteral("model_display_name")).toString());
  p.structured_output_mode =
      NormalizedStructuredOutputMode(o.value(QStringLiteral("structured_output_mode")).toString());
  p.structured_output_strict = o.value(QStringLiteral("structured_output_strict")).toBool(true);
  p.response_content_json_pointer =
      SanitizedNonSecretString(o.value(QStringLiteral("response_content_json_pointer")).toString());
  p.response_usage_json_pointer =
      SanitizedNonSecretString(o.value(QStringLiteral("response_usage_json_pointer")).toString());
  p.response_provider_request_id_json_pointer = SanitizedNonSecretString(
      o.value(QStringLiteral("response_provider_request_id_json_pointer")).toString());
  p.response_provider_request_id_header = SanitizedNonSecretString(
      o.value(QStringLiteral("response_provider_request_id_header")).toString());
  p.timeout_ms =
      Clamp(o.value(QStringLiteral("timeout_ms")).toInteger(60000), kMinTimeoutMs, kMaxTimeoutMs);
  p.max_image_bytes   = Clamp(o.value(QStringLiteral("max_image_bytes")).toInteger(4194304),
                              kMinImageBytes, kMaxImageBytes);
  p.max_output_tokens = Clamp(o.value(QStringLiteral("max_output_tokens")).toInteger(1200),
                              kMinOutputTokens, kMaxOutputTokens);
  p.temperature       = o.value(QStringLiteral("temperature")).toDouble(kDefaultTemperature);
  if (p.temperature < 0.0 || p.temperature > 2.0) {
    p.temperature = kDefaultTemperature;
  }
  p.recommended_rendition =
      NormalizedRendition(o.value(QStringLiteral("recommended_rendition")).toString());

  const auto models = o.value(QStringLiteral("models")).toArray();
  for (const auto& value : models) {
    const auto model = ModelFromJson(value.toObject(), p.max_image_bytes, p.recommended_rendition);
    if (!model.model_id.isEmpty()) {
      p.models.push_back(model);
    }
  }
  if (!p.model_id.isEmpty() &&
      std::none_of(p.models.begin(), p.models.end(),
                   [&](const auto& model) { return model.model_id == p.model_id; })) {
    AiProviderModelEntry model;
    model.model_id              = p.model_id;
    model.display_name          = p.model_display_name;
    model.max_image_bytes       = p.max_image_bytes;
    model.recommended_rendition = p.recommended_rendition;
    p.models.push_back(model);
  }
  return p;
}

QJsonObject ProfileToSidecarConfigJson(const AiProviderProfile& p) {
  QJsonObject root;
  root.insert(QStringLiteral("schema_version"), 1);
  root.insert(QStringLiteral("provider_id"), p.provider_id);
  root.insert(QStringLiteral("display_name"), p.display_name);
  root.insert(QStringLiteral("driver"), p.driver);
  root.insert(QStringLiteral("base_url"), p.base_url);
  root.insert(QStringLiteral("endpoint"), p.endpoint);
  if (!p.models_endpoint.trimmed().isEmpty()) {
    root.insert(QStringLiteral("models_endpoint"), p.models_endpoint.trimmed());
  }

  QJsonObject auth;
  auth.insert(QStringLiteral("type"), p.auth_type);
  auth.insert(QStringLiteral("credential_slot"), p.credential_slot);
  root.insert(QStringLiteral("auth"), auth);
  root.insert(QStringLiteral("attribution_headers"), QJsonObject{});

  QJsonObject defaults;
  defaults.insert(QStringLiteral("model"), p.model_id);
  defaults.insert(QStringLiteral("stream"), false);
  defaults.insert(QStringLiteral("temperature"), p.temperature);
  root.insert(QStringLiteral("defaults"), defaults);

  QJsonObject structured;
  structured.insert(QStringLiteral("mode"), p.structured_output_mode);
  structured.insert(QStringLiteral("strict"), p.structured_output_strict);
  root.insert(QStringLiteral("structured_output"), structured);

  QJsonObject response;
  response.insert(QStringLiteral("content_json_pointer"),
                  OptionalString(p.response_content_json_pointer));
  response.insert(QStringLiteral("usage_json_pointer"),
                  OptionalString(p.response_usage_json_pointer));
  response.insert(QStringLiteral("provider_request_id_json_pointer"),
                  OptionalString(p.response_provider_request_id_json_pointer));
  response.insert(QStringLiteral("provider_request_id_header"),
                  OptionalString(p.response_provider_request_id_header));
  root.insert(QStringLiteral("response"), response);

  QJsonObject limits;
  limits.insert(QStringLiteral("timeout_ms"), p.timeout_ms);
  limits.insert(QStringLiteral("max_image_bytes"), p.max_image_bytes);
  limits.insert(QStringLiteral("max_output_tokens"), p.max_output_tokens);
  root.insert(QStringLiteral("limits"), limits);

  QJsonArray models;
  for (const auto& model : p.models) {
    if (!model.model_id.isEmpty()) {
      models.push_back(ModelToJson(model));
    }
  }
  root.insert(QStringLiteral("models"), models);
  return root;
}

QVariantMap ProfileToVariant(const AiProviderProfile&                   p,
                             const std::shared_ptr<IAiCredentialStore>& store) {
  QVariantMap m;
  m.insert(QStringLiteral("uuid"), p.uuid);
  m.insert(QStringLiteral("displayName"), p.display_name);
  m.insert(QStringLiteral("basedOnTemplate"), p.based_on_template);
  m.insert(QStringLiteral("credentialSlot"), p.credential_slot);
  m.insert(QStringLiteral("maskedKeyLabel"), p.masked_key_label);
  m.insert(QStringLiteral("rememberKey"), p.remember_key);
  m.insert(QStringLiteral("lastUsedMs"), p.last_used_ms);
  m.insert(QStringLiteral("providerId"), p.provider_id);
  m.insert(QStringLiteral("driver"), p.driver);
  m.insert(QStringLiteral("baseUrl"), p.base_url);
  m.insert(QStringLiteral("endpoint"), p.endpoint);
  m.insert(QStringLiteral("modelsEndpoint"), p.models_endpoint);
  m.insert(QStringLiteral("authType"), p.auth_type);
  m.insert(QStringLiteral("modelId"), p.model_id);
  m.insert(QStringLiteral("modelDisplayName"), p.model_display_name);
  m.insert(QStringLiteral("structuredOutputMode"), p.structured_output_mode);
  m.insert(QStringLiteral("timeoutMs"), p.timeout_ms);
  m.insert(QStringLiteral("maxImageBytes"), p.max_image_bytes);
  m.insert(QStringLiteral("recommendedRendition"), p.recommended_rendition);
  m.insert(QStringLiteral("models"), ModelsToVariant(p.models));
  const bool requires_credential = p.auth_type != QStringLiteral("none");
  m.insert(QStringLiteral("credentialRequired"), requires_credential);
  m.insert(QStringLiteral("credentialAvailable"),
           !requires_credential || (store && !p.credential_slot.isEmpty() &&
                                    store->HasCredential(p.credential_slot.toStdString())));
  return m;
}

}  // namespace

AiProviderProfileController::AiProviderProfileController(QObject* parent)
    : AiProviderProfileController(DefaultStorageFile(),
                                  DefaultSidecarConfigDir(DefaultStorageFile()),
                                  MakeDefaultAiCredentialStore(), parent) {}

AiProviderProfileController::AiProviderProfileController(
    std::filesystem::path storage_file, std::filesystem::path sidecar_config_dir,
    std::shared_ptr<IAiCredentialStore> credential_store, QObject* parent)
    : QObject(parent),
      storage_file_(std::move(storage_file)),
      sidecar_config_dir_(std::move(sidecar_config_dir)),
      credential_store_(std::move(credential_store)) {
  if (!credential_store_) {
    credential_store_ = MakeDefaultAiCredentialStore();
  }
  Load();
}

auto AiProviderProfileController::DefaultStorageFile() -> std::filesystem::path {
  QString root = QStandardPaths::writableLocation(QStandardPaths::AppConfigLocation);
  if (root.isEmpty()) {
    root = QDir::homePath() + QStringLiteral("/.alcedo_studio");
  }
  return FsPathFromQString(root) / "ai_providers.json";
}

auto AiProviderProfileController::DefaultSidecarConfigDir(const std::filesystem::path& storage_file)
    -> std::filesystem::path {
  return storage_file.parent_path() / "ai_provider_configs";
}

QVariantList AiProviderProfileController::Profiles() const {
  QVariantList out;
  for (const auto& p : profiles_) {
    QVariantMap m = ProfileToVariant(p, credential_store_);
    m.insert(QStringLiteral("active"), p.uuid == active_profile_id_);
    out.push_back(m);
  }
  return out;
}

QVariantList AiProviderProfileController::TemplateOptions() const {
  QVariantList out;
  for (const auto& t : kTemplates) {
    QVariantMap m;
    m.insert(QStringLiteral("templateId"), QString::fromLatin1(t.template_id));
    m.insert(QStringLiteral("label"), QString::fromUtf8(t.label));
    m.insert(QStringLiteral("baseUrl"), QString::fromLatin1(t.base_url));
    m.insert(QStringLiteral("driver"), QString::fromLatin1(t.driver));
    m.insert(QStringLiteral("modelId"), QString::fromLatin1(t.model_id));
    out.push_back(m);
  }
  return out;
}

QString AiProviderProfileController::ActiveDisplayName() const {
  const auto* p = FindProfile(active_profile_id_);
  return p ? p->display_name : QString{};
}

QString AiProviderProfileController::ActiveModelDisplayName() const {
  const auto* p = FindProfile(active_profile_id_);
  if (!p) {
    return {};
  }
  return p->model_display_name.isEmpty() ? p->model_id : p->model_display_name;
}

auto AiProviderProfileController::ActiveProfile() const -> std::optional<AiProviderProfile> {
  return ProfileById(active_profile_id_);
}

auto AiProviderProfileController::ProfileById(const QString& profile_id) const
    -> std::optional<AiProviderProfile> {
  const auto* p = FindProfile(profile_id);
  if (!p) {
    return std::nullopt;
  }
  return *p;
}

QVariantMap AiProviderProfileController::Profile(const QString& profile_id) const {
  const auto* p = FindProfile(profile_id);
  if (!p) {
    return {};
  }
  QVariantMap m = ProfileToVariant(*p, credential_store_);
  m.insert(QStringLiteral("active"), p->uuid == active_profile_id_);
  return m;
}

QVariantList AiProviderProfileController::ModelOptions(const QString& profile_id) const {
  const auto* p = FindProfile(profile_id);
  if (!p) {
    return {};
  }
  return ModelsToVariant(p->models);
}

QString AiProviderProfileController::AddProfileFromTemplate(const QString& template_id) {
  const TemplateConfig* t = FindTemplate(template_id);
  if (!t) {
    return {};
  }
  AiProviderProfile p = ProfileFromTemplate(*t);
  p.display_name      = UniqueDisplayName(p.display_name);
  p.last_used_ms      = QDateTime::currentMSecsSinceEpoch();
  const QString id    = p.uuid;
  profiles_.push_back(std::move(p));
  active_profile_id_ = id;
  MarkSidecarConfigsDirty();
  Save();
  emit ProfilesChanged();
  return id;
}

QString AiProviderProfileController::CloneProfile(const QString& profile_id) {
  const auto* source = FindProfile(profile_id);
  if (!source) {
    return {};
  }
  AiProviderProfile clone    = *source;
  const QString     new_uuid = QUuid::createUuid().toString(QUuid::WithoutBraces);
  const QString     suffix   = NewIdSuffix();
  clone.uuid                 = new_uuid;
  clone.display_name         = UniqueDisplayName(source->display_name + QStringLiteral(" copy"));
  clone.provider_id          = QStringLiteral("profile_") + suffix;
  clone.credential_slot      = QStringLiteral("alcedo_ai_") + suffix;
  clone.masked_key_label.clear();
  clone.last_used_ms = QDateTime::currentMSecsSinceEpoch();

  auto it            = std::find_if(profiles_.begin(), profiles_.end(),
                                    [&](const auto& p) { return p.uuid == profile_id; });
  if (it == profiles_.end()) {
    profiles_.push_back(std::move(clone));
  } else {
    profiles_.insert(std::next(it), std::move(clone));
  }
  MarkSidecarConfigsDirty();
  Save();
  emit ProfilesChanged();
  return new_uuid;
}

bool AiProviderProfileController::DeleteProfile(const QString& profile_id, bool delete_credential) {
  auto it = std::find_if(profiles_.begin(), profiles_.end(),
                         [&](const auto& p) { return p.uuid == profile_id; });
  if (it == profiles_.end()) {
    return false;
  }
  const QString slot = it->credential_slot;
  if (delete_credential && credential_store_ && !slot.isEmpty()) {
    std::string error;
    credential_store_->DeleteCredential(slot.toStdString(), &error);
  }
  const bool was_active = it->uuid == active_profile_id_;
  profiles_.erase(it);
  if (was_active) {
    EnsureActiveProfile();
  }
  MarkSidecarConfigsDirty();
  Save();
  emit ProfilesChanged();
  return true;
}

bool AiProviderProfileController::SetActiveProfile(const QString& profile_id) {
  auto* p = FindProfile(profile_id);
  if (!p) {
    return false;
  }
  active_profile_id_ = p->uuid;
  p->last_used_ms    = QDateTime::currentMSecsSinceEpoch();
  Save();
  emit ProfilesChanged();
  return true;
}

bool AiProviderProfileController::SetOutputLanguage(const QString& value) {
  const QString normalized = NormalizedOutputLanguage(value);
  if (normalized == output_language_) {
    return true;
  }
  output_language_ = normalized;
  Save();
  emit ProfilesChanged();
  return true;
}

bool AiProviderProfileController::SetProfileField(const QString& profile_id, const QString& field,
                                                  const QVariant& value) {
  auto* p = FindProfile(profile_id);
  if (!p) {
    return false;
  }
  const QString name  = field.trimmed();
  const QString text  = value.toString();
  bool          dirty = true;

  if (name == QStringLiteral("displayName")) {
    const QString v = SanitizedNonSecretString(text);
    if (v.isEmpty()) {
      return false;
    }
    p->display_name = v;
  } else if (name == QStringLiteral("providerId")) {
    const QString v = SanitizedId(text);
    if (v.isEmpty() || std::any_of(profiles_.begin(), profiles_.end(), [&](const auto& other) {
          return other.uuid != p->uuid && other.provider_id == v;
        })) {
      return false;
    }
    p->provider_id = v;
  } else if (name == QStringLiteral("driver")) {
    p->driver = NormalizedProtocolDriver(text);
  } else if (name == QStringLiteral("baseUrl")) {
    p->base_url = SanitizedNonSecretString(text);
  } else if (name == QStringLiteral("endpoint")) {
    p->endpoint = SanitizedNonSecretString(text);
  } else if (name == QStringLiteral("modelsEndpoint")) {
    p->models_endpoint = SanitizedNonSecretString(text);
  } else if (name == QStringLiteral("authType")) {
    p->auth_type = NormalizedAuthType(text);
  } else if (name == QStringLiteral("credentialSlot")) {
    const QString v = SanitizedId(text);
    if (v.isEmpty() || std::any_of(profiles_.begin(), profiles_.end(), [&](const auto& other) {
          return other.uuid != p->uuid && other.credential_slot == v;
        })) {
      return false;
    }
    p->credential_slot = v;
  } else if (name == QStringLiteral("structuredOutputMode")) {
    p->structured_output_mode = NormalizedStructuredOutputMode(text);
  } else if (name == QStringLiteral("timeoutMs")) {
    p->timeout_ms = Clamp(value.toLongLong(), kMinTimeoutMs, kMaxTimeoutMs);
  } else if (name == QStringLiteral("maxImageBytes")) {
    p->max_image_bytes = Clamp(value.toLongLong(), kMinImageBytes, kMaxImageBytes);
  } else if (name == QStringLiteral("recommendedRendition")) {
    p->recommended_rendition = NormalizedRendition(text);
  } else if (name == QStringLiteral("modelId")) {
    const QString v = SanitizedNonSecretString(text);
    if (v.isEmpty()) {
      return false;
    }
    p->model_id = v;
    if (std::none_of(p->models.begin(), p->models.end(),
                     [&](const auto& model) { return model.model_id == v; })) {
      AiProviderModelEntry model;
      model.model_id              = v;
      model.display_name          = v;
      model.max_image_bytes       = p->max_image_bytes;
      model.recommended_rendition = p->recommended_rendition;
      p->models.push_back(model);
    }
  } else if (name == QStringLiteral("modelDisplayName")) {
    p->model_display_name = SanitizedNonSecretString(text);
  } else {
    return false;
  }

  if (dirty) {
    MarkSidecarConfigsDirty();
  }
  Save();
  emit ProfilesChanged();
  return true;
}

QString AiProviderProfileController::SaveApiKey(const QString& profile_id, const QString& secret) {
  auto* p = FindProfile(profile_id);
  if (!p || !credential_store_) {
    return QStringLiteral("Image analysis provider profile is unavailable.");
  }
  if (p->credential_slot.isEmpty()) {
    return QStringLiteral("Configure a credential slot before saving a key.");
  }
  std::string raw = secret.toStdString();
  std::string error;
  const bool  ok = credential_store_->SaveCredential(p->credential_slot.toStdString(), raw, &error);
  const QString mask = MaskedKeyLabel(raw);
  ClearSecret(&raw);
  if (!ok) {
    return error.empty() ? QStringLiteral("Could not save the API key to the credential store.")
                         : QString::fromStdString(error);
  }
  p->masked_key_label = mask;
  p->remember_key     = true;
  Save();
  emit ProfilesChanged();
  return {};
}

void AiProviderProfileController::DeleteApiKey(const QString& profile_id) {
  auto* p = FindProfile(profile_id);
  if (!p || !credential_store_ || p->credential_slot.isEmpty()) {
    return;
  }
  std::string error;
  credential_store_->DeleteCredential(p->credential_slot.toStdString(), &error);
  p->masked_key_label.clear();
  Save();
  emit ProfilesChanged();
}

void AiProviderProfileController::SetDiscoveredModels(const QString&      profile_id,
                                                      const QVariantList& models) {
  auto* p = FindProfile(profile_id);
  if (!p) {
    return;
  }
  bool changed = false;
  for (const auto& value : models) {
    const auto    map = value.toMap();
    const QString id  = SanitizedNonSecretString(map.value(QStringLiteral("modelId")).toString());
    if (id.isEmpty()) {
      continue;
    }
    const QString display =
        SanitizedNonSecretString(map.value(QStringLiteral("displayName")).toString());
    auto it = std::find_if(p->models.begin(), p->models.end(),
                           [&](const auto& model) { return model.model_id == id; });
    const bool discovered_supports_vision =
        VariantBoolWithFallback(map, QStringLiteral("supportsVision"),
                                QStringLiteral("supports_vision"), true);
    const bool discovered_supports_structured_output =
        VariantBoolWithFallback(map, QStringLiteral("supportsStructuredOutput"),
                                QStringLiteral("supports_structured_output"),
                                p->structured_output_mode != QStringLiteral("none"));
    const bool discovered_live_confirmed =
        VariantBoolWithFallback(map, QStringLiteral("liveConfirmed"),
                                QStringLiteral("live_confirmed"), false);
    if (it == p->models.end()) {
      AiProviderModelEntry model;
      model.model_id                   = id;
      model.display_name               = display.isEmpty() ? id : display;
      model.supports_vision            = discovered_supports_vision;
      model.supports_structured_output = discovered_supports_structured_output;
      model.live_confirmed             = discovered_live_confirmed;
      model.max_image_bytes            = p->max_image_bytes;
      model.recommended_rendition      = p->recommended_rendition;
      p->models.push_back(model);
      changed = true;
    } else {
      if (!display.isEmpty() && it->display_name != display) {
        it->display_name = display;
        changed          = true;
      }
      if (it->supports_vision != discovered_supports_vision) {
        it->supports_vision = discovered_supports_vision;
        changed             = true;
      }
      if (it->supports_structured_output != discovered_supports_structured_output) {
        it->supports_structured_output = discovered_supports_structured_output;
        changed                        = true;
      }
      if (it->live_confirmed != discovered_live_confirmed) {
        it->live_confirmed = discovered_live_confirmed;
        changed            = true;
      }
    }
  }
  if (changed) {
    MarkSidecarConfigsDirty();
    Save();
    emit ProfilesChanged();
  }
}

auto AiProviderProfileController::PrepareSidecarConfigDir(std::string* error) -> bool {
  QDir dir(QStringFromFsPath(sidecar_config_dir_));
  if (!dir.exists() && !dir.mkpath(QStringLiteral("."))) {
    if (error) {
      *error = "could not create provider config directory";
    }
    return false;
  }

  const auto stale = dir.entryList(QStringList{QStringLiteral("*.json")}, QDir::Files);
  for (const QString& file : stale) {
    if (!dir.remove(file)) {
      if (error) {
        *error = "could not remove stale provider config: " + file.toStdString();
      }
      return false;
    }
  }

  for (const auto& profile : profiles_) {
    if (profile.provider_id.isEmpty() || profile.credential_slot.isEmpty() ||
        profile.base_url.isEmpty() || profile.endpoint.isEmpty() || profile.model_id.isEmpty()) {
      continue;
    }
    const QString file_name = profile.provider_id + QStringLiteral(".json");
    QSaveFile     file(dir.filePath(file_name));
    if (!file.open(QIODevice::WriteOnly | QIODevice::Truncate)) {
      if (error) {
        *error = "could not write provider config: " + file_name.toStdString();
      }
      return false;
    }
    const QJsonDocument doc(ProfileToSidecarConfigJson(profile));
    file.write(doc.toJson(QJsonDocument::Indented));
    if (!file.commit()) {
      if (error) {
        *error = "could not commit provider config: " + file_name.toStdString();
      }
      return false;
    }
  }

  if (sidecar_configs_dirty_) {
    sidecar_configs_dirty_ = false;
    emit ProfilesChanged();
  }
  return true;
}

auto AiProviderProfileController::FindProfile(const QString& profile_id) -> AiProviderProfile* {
  auto it = std::find_if(profiles_.begin(), profiles_.end(),
                         [&](const auto& p) { return p.uuid == profile_id; });
  return it == profiles_.end() ? nullptr : &*it;
}

auto AiProviderProfileController::FindProfile(const QString& profile_id) const
    -> const AiProviderProfile* {
  auto it = std::find_if(profiles_.begin(), profiles_.end(),
                         [&](const auto& p) { return p.uuid == profile_id; });
  return it == profiles_.end() ? nullptr : &*it;
}

void AiProviderProfileController::Load() {
  profiles_.clear();
  active_profile_id_.clear();
  output_language_ = QStringLiteral("follow");

  QFile file(QStringFromFsPath(storage_file_));
  if (!file.exists() || !file.open(QIODevice::ReadOnly)) {
    sidecar_configs_dirty_ = true;
    return;
  }
  const auto doc = QJsonDocument::fromJson(file.readAll());
  if (!doc.isObject()) {
    sidecar_configs_dirty_ = true;
    return;
  }
  const auto root = doc.object();
  if (root.value(QStringLiteral("schema_version")).toInt() != kStoreSchemaVersion) {
    sidecar_configs_dirty_ = true;
    return;
  }
  output_language_ =
      NormalizedOutputLanguage(root.value(QStringLiteral("output_language")).toString());
  active_profile_id_ =
      SanitizedNonSecretString(root.value(QStringLiteral("active_profile_id")).toString());
  const auto profiles = root.value(QStringLiteral("profiles")).toArray();
  for (const auto& value : profiles) {
    auto p = ProfileFromStoreJson(value.toObject());
    if (!p.uuid.isEmpty() && !p.provider_id.isEmpty() && !p.credential_slot.isEmpty()) {
      MigrateOpenCodeGoProfileDefaults(&p);
      profiles_.push_back(std::move(p));
    }
  }
  EnsureActiveProfile();
  sidecar_configs_dirty_ = true;
}

void AiProviderProfileController::Save() {
  QDir dir(QStringFromFsPath(storage_file_.parent_path()));
  if (!dir.exists()) {
    dir.mkpath(QStringLiteral("."));
  }
  QSaveFile file(QStringFromFsPath(storage_file_));
  if (!file.open(QIODevice::WriteOnly | QIODevice::Truncate)) {
    return;
  }
  QJsonObject root;
  root.insert(QStringLiteral("schema_version"), kStoreSchemaVersion);
  root.insert(QStringLiteral("active_profile_id"), active_profile_id_);
  root.insert(QStringLiteral("output_language"), output_language_);
  QJsonArray profiles;
  for (const auto& p : profiles_) {
    profiles.push_back(ProfileToStoreJson(p));
  }
  root.insert(QStringLiteral("profiles"), profiles);
  file.write(QJsonDocument(root).toJson(QJsonDocument::Indented));
  file.commit();
}

void AiProviderProfileController::MarkSidecarConfigsDirty() { sidecar_configs_dirty_ = true; }

void AiProviderProfileController::EnsureActiveProfile() {
  if (profiles_.empty()) {
    active_profile_id_.clear();
    return;
  }
  if (FindProfile(active_profile_id_) != nullptr) {
    return;
  }
  auto best = std::max_element(
      profiles_.begin(), profiles_.end(),
      [](const auto& a, const auto& b) { return a.last_used_ms < b.last_used_ms; });
  active_profile_id_ = best != profiles_.end() ? best->uuid : profiles_.front().uuid;
}

auto AiProviderProfileController::UniqueDisplayName(const QString& base,
                                                    const QString& exclude_profile_id) const
    -> QString {
  const QString seed  = base.trimmed().isEmpty() ? QStringLiteral("Provider") : base.trimmed();
  auto          taken = [&](const QString& name) {
    return std::any_of(profiles_.begin(), profiles_.end(), [&](const auto& p) {
      return p.uuid != exclude_profile_id && p.display_name == name;
    });
  };
  if (!taken(seed)) {
    return seed;
  }
  for (int i = 2; i < 1000; ++i) {
    const QString candidate = seed + QStringLiteral(" ") + QString::number(i);
    if (!taken(candidate)) {
      return candidate;
    }
  }
  return seed + QStringLiteral(" ") + QString::number(QDateTime::currentMSecsSinceEpoch());
}

}  // namespace alcedo
