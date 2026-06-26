//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QObject>
#include <QString>

namespace alcedo {

/// Phase 6a — the editable fields of a "compatible protocol preset".
///
/// The Phase 6 product mental model is "compatible protocol preset", not
/// "provider brand": a preset is a configured endpoint over one of the protocol
/// families (`openai_chat_compatible`, `anthropic_messages`). `provider_id` is
/// kept on the Rust wire for compatibility but means "configured endpoint id".
///
/// This DTO carries ONLY non-secret metadata. There is intentionally NO field
/// for a raw API key / secret: the long-lived key is owned by the host OS
/// credential store (Phase 6c `AiCredentialStore`) and reaches the sidecar only
/// as a short-lived vault handle at job time. `credential_slot` is a slot *label*
/// (e.g. `opencode_api_key`), never the secret itself; `masked_key_label` is a
/// display-only mask. Persisting this DTO through `QSettings` therefore never
/// stores raw key material.
struct AiProviderPreset {
  QString display_name;             // user-facing preset label.
  QString protocol_family;          // openai_chat_compatible | anthropic_messages.
  QString base_url;                 // e.g. https://opencode.ai/zen/go/v1
  QString endpoint;                 // e.g. /messages or /chat/completions
  QString auth_type;                // bearer | api_key_header | none
  QString credential_slot;          // vault slot label (NOT a secret).
  QString model_id;                 // provider model slug.
  QString model_display_name;       // optional human-readable model name.
  QString structured_output_mode;   // response_format_json_schema | responses_json_schema | tool | none
  qint64 timeout_ms = 60000;
  qint64 max_image_bytes = 4194304;
  QString recommended_rendition;    // thumbnail | preview | image
  QString masked_key_label;         // display-only mask of the persisted key (no raw key).
  bool remember_key = false;        // whether the user enabled persistent key storage.
};

/// Owns the selected compatible-protocol preset's non-secret settings and
/// round-trips them through `QSettings` (the same default-scope pattern as the
/// semantic model-endpoint settings in `project_service.cpp`). Mirrors the
/// `ModelDownloadController` convention: `Q_PROPERTY` per field, `Q_INVOKABLE`
/// setters that write `QSettings` and emit `PresetChanged`, one shared NOTIFY.
///
/// The controller never accepts a raw API key — there is no `SetApiKey` /
/// `SetSecret`. Phase 6c wires the actual secret flow (OS credential store ->
/// `RegisterCredential` -> opaque handle); Phase 6a freezes only the
/// settings-round-trip contract.
class AiProviderPresetController final : public QObject {
  Q_OBJECT
  Q_PROPERTY(QString displayName READ DisplayName NOTIFY PresetChanged)
  Q_PROPERTY(QString protocolFamily READ ProtocolFamily NOTIFY PresetChanged)
  Q_PROPERTY(QString baseUrl READ BaseUrl NOTIFY PresetChanged)
  Q_PROPERTY(QString endpoint READ Endpoint NOTIFY PresetChanged)
  Q_PROPERTY(QString authType READ AuthType NOTIFY PresetChanged)
  Q_PROPERTY(QString credentialSlot READ CredentialSlot NOTIFY PresetChanged)
  Q_PROPERTY(QString modelId READ ModelId NOTIFY PresetChanged)
  Q_PROPERTY(QString modelDisplayName READ ModelDisplayName NOTIFY PresetChanged)
  Q_PROPERTY(QString structuredOutputMode READ StructuredOutputMode NOTIFY PresetChanged)
  Q_PROPERTY(qint64 timeoutMs READ TimeoutMs NOTIFY PresetChanged)
  Q_PROPERTY(qint64 maxImageBytes READ MaxImageBytes NOTIFY PresetChanged)
  Q_PROPERTY(QString recommendedRendition READ RecommendedRendition NOTIFY PresetChanged)
  Q_PROPERTY(QString maskedKeyLabel READ MaskedKeyLabel NOTIFY PresetChanged)
  Q_PROPERTY(bool rememberKey READ RememberKey NOTIFY PresetChanged)

 public:
  explicit AiProviderPresetController(QObject* parent = nullptr);

  // The current preset, loaded from QSettings (defaults applied for missing keys).
  [[nodiscard]] AiProviderPreset CurrentPreset() const;

  // Writes every field of `preset` to QSettings and emits PresetChanged.
  void SetFromPreset(const AiProviderPreset& preset);

  // Removes every persisted preset key (logout / settings deletion). Emits PresetChanged.
  void Clear();

  // Individual getters (back the Q_PROPERTYs).
  QString DisplayName() const;
  QString ProtocolFamily() const;
  QString BaseUrl() const;
  QString Endpoint() const;
  QString AuthType() const;
  QString CredentialSlot() const;
  QString ModelId() const;
  QString ModelDisplayName() const;
  QString StructuredOutputMode() const;
  qint64  TimeoutMs() const;
  qint64  MaxImageBytes() const;
  QString RecommendedRendition() const;
  QString MaskedKeyLabel() const;
  bool    RememberKey() const;

  // Individual Q_INVOKABLE setters (write QSettings + emit PresetChanged).
  Q_INVOKABLE void SetDisplayName(const QString& value);
  Q_INVOKABLE void SetProtocolFamily(const QString& value);
  Q_INVOKABLE void SetBaseUrl(const QString& value);
  Q_INVOKABLE void SetEndpoint(const QString& value);
  Q_INVOKABLE void SetAuthType(const QString& value);
  Q_INVOKABLE void SetCredentialSlot(const QString& value);
  Q_INVOKABLE void SetModelId(const QString& value);
  Q_INVOKABLE void SetModelDisplayName(const QString& value);
  Q_INVOKABLE void SetStructuredOutputMode(const QString& value);
  Q_INVOKABLE void SetTimeoutMs(qint64 value);
  Q_INVOKABLE void SetMaxImageBytes(qint64 value);
  Q_INVOKABLE void SetRecommendedRendition(const QString& value);
  Q_INVOKABLE void SetMaskedKeyLabel(const QString& value);
  Q_INVOKABLE void SetRememberKey(bool value);

 signals:
  // Shared NOTIFY for every Q_PROPERTY above (mirrors the existing controller convention).
  void PresetChanged();

 private:
  // Clamp helpers (mirror NormalizedEndpointPreset in project_service.cpp): keep
  // the persisted value inside the known closed set so a stale/garbage QSettings
  // value cannot describe an unsupported protocol/auth/rendition, and keep the
  // numeric fields inside the same bounds the Rust config enforces
  // (timeout_ms in [1s, 300s], max_image_bytes in [1, 16 MiB]) so a bad value
  // can never be persisted or reach a request / generated user config.
  static QString NormalizedProtocolFamily(const QString& value);
  static QString NormalizedAuthType(const QString& value);
  static QString NormalizedStructuredOutputMode(const QString& value);
  static QString NormalizedRendition(const QString& value);
  static qint64 NormalizedTimeoutMs(qint64 value);
  static qint64 NormalizedMaxImageBytes(qint64 value);
};

}  // namespace alcedo
