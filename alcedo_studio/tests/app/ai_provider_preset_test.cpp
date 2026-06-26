//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

// Phase 6a — compatible-protocol preset settings round-trip. Proves the selected
// preset survives a QSettings save/reload with every editable field intact, that
// the controller has NO surface to store a raw API key, and that no secret-shaped
// value is ever persisted under the ai/preset group.

#include <gtest/gtest.h>

#include <QCoreApplication>
#include <QLatin1String>
#include <QRegularExpression>
#include <QSettings>
#include <QString>
#include <QStringList>

#include "app/ai_provider_preset.hpp"

namespace {

class AiProviderPresetTest : public ::testing::Test {
 protected:
  void SetUp() override {
    // Isolate QSettings to a test-specific org/app so we never touch the real
    // Alcedo settings, and so each process gets a fresh store.
    QCoreApplication::setOrganizationName(QStringLiteral("PuerhLabTest"));
    QCoreApplication::setApplicationName(QStringLiteral("AiProviderPresetTest"));
    // Start from a clean slate.
    QSettings().remove(QLatin1String("ai/preset"));
  }
  void TearDown() override { QSettings().remove(QLatin1String("ai/preset")); }
};

alcedo::AiProviderPreset SamplePreset() {
  alcedo::AiProviderPreset p;
  p.display_name           = QStringLiteral("Opencode Go (Anthropic-compatible)");
  p.protocol_family        = QStringLiteral("anthropic_messages");
  p.base_url               = QStringLiteral("https://opencode.ai/zen/go/v1");
  p.endpoint               = QStringLiteral("/messages");
  p.auth_type              = QStringLiteral("bearer");
  p.credential_slot        = QStringLiteral("opencode_api_key");
  p.model_id               = QStringLiteral("claude-sonnet-4-5");
  p.model_display_name     = QStringLiteral("Claude Sonnet 4.5");
  p.structured_output_mode = QStringLiteral("tool");
  p.timeout_ms             = 60000;
  p.max_image_bytes        = 4194304;
  p.recommended_rendition  = QStringLiteral("preview");
  p.masked_key_label       = QStringLiteral("sk-…4f2a");
  p.remember_key           = true;
  return p;
}

}  // namespace

TEST_F(AiProviderPresetTest, RoundTripsEveryEditableField) {
  alcedo::AiProviderPresetController controller;
  controller.SetFromPreset(SamplePreset());

  // A fresh controller instance reads back from QSettings — proves persistence.
  alcedo::AiProviderPresetController reloaded;
  const alcedo::AiProviderPreset got = reloaded.CurrentPreset();

  EXPECT_EQ(got.display_name, QStringLiteral("Opencode Go (Anthropic-compatible)"));
  EXPECT_EQ(got.protocol_family, QStringLiteral("anthropic_messages"));
  EXPECT_EQ(got.base_url, QStringLiteral("https://opencode.ai/zen/go/v1"));
  EXPECT_EQ(got.endpoint, QStringLiteral("/messages"));
  EXPECT_EQ(got.auth_type, QStringLiteral("bearer"));
  EXPECT_EQ(got.credential_slot, QStringLiteral("opencode_api_key"));
  EXPECT_EQ(got.model_id, QStringLiteral("claude-sonnet-4-5"));
  EXPECT_EQ(got.model_display_name, QStringLiteral("Claude Sonnet 4.5"));
  EXPECT_EQ(got.structured_output_mode, QStringLiteral("tool"));
  EXPECT_EQ(got.timeout_ms, 60000);
  EXPECT_EQ(got.max_image_bytes, 4194304);
  EXPECT_EQ(got.recommended_rendition, QStringLiteral("preview"));
  EXPECT_EQ(got.masked_key_label, QStringLiteral("sk-…4f2a"));
  EXPECT_TRUE(got.remember_key);
}

TEST_F(AiProviderPresetTest, IndividualSettersPersistAndReload) {
  alcedo::AiProviderPresetController controller;
  controller.SetProtocolFamily(QStringLiteral("openai_chat_compatible"));
  controller.SetEndpoint(QStringLiteral("/chat/completions"));
  controller.SetStructuredOutputMode(QStringLiteral("response_format_json_schema"));
  controller.SetModelId(QStringLiteral("gpt-4o"));
  controller.SetTimeoutMs(45000);
  controller.SetRememberKey(false);

  alcedo::AiProviderPresetController reloaded;
  const alcedo::AiProviderPreset got = reloaded.CurrentPreset();
  EXPECT_EQ(got.protocol_family, QStringLiteral("openai_chat_compatible"));
  EXPECT_EQ(got.endpoint, QStringLiteral("/chat/completions"));
  EXPECT_EQ(got.structured_output_mode, QStringLiteral("response_format_json_schema"));
  EXPECT_EQ(got.model_id, QStringLiteral("gpt-4o"));
  EXPECT_EQ(got.timeout_ms, 45000);
  EXPECT_FALSE(got.remember_key);
}

TEST_F(AiProviderPresetTest, ClearRemovesAllPresetKeys) {
  alcedo::AiProviderPresetController controller;
  controller.SetFromPreset(SamplePreset());
  controller.Clear();

  // After Clear, only defaults remain; the editable string fields are empty.
  alcedo::AiProviderPresetController reloaded;
  const alcedo::AiProviderPreset got = reloaded.CurrentPreset();
  EXPECT_TRUE(got.display_name.isEmpty());
  EXPECT_TRUE(got.base_url.isEmpty());
  EXPECT_TRUE(got.endpoint.isEmpty());
  EXPECT_TRUE(got.model_id.isEmpty());
  EXPECT_TRUE(got.credential_slot.isEmpty());
  EXPECT_FALSE(got.remember_key);
  // Defaults (not user values) are applied for the closed-set / numeric fields.
  EXPECT_EQ(got.protocol_family, QStringLiteral("anthropic_messages"));
  EXPECT_EQ(got.auth_type, QStringLiteral("bearer"));
  EXPECT_EQ(got.structured_output_mode, QStringLiteral("tool"));
  EXPECT_EQ(got.recommended_rendition, QStringLiteral("preview"));
  EXPECT_EQ(got.timeout_ms, 60000);
  EXPECT_EQ(got.max_image_bytes, 4194304);
}

TEST_F(AiProviderPresetTest, GarbageValuesClampToKnownClosedSets) {
  alcedo::AiProviderPresetController controller;
  controller.SetProtocolFamily(QStringLiteral("some_brand_driver"));
  controller.SetAuthType(QStringLiteral("weird_auth"));
  controller.SetStructuredOutputMode(QStringLiteral("prompt_for_json"));
  controller.SetRecommendedRendition(QStringLiteral("huge"));

  alcedo::AiProviderPresetController reloaded;
  const alcedo::AiProviderPreset got = reloaded.CurrentPreset();
  // A garbage / brand-named value never describes an unsupported protocol/auth/
  // rendition — it clamps to the defaults, preserving the protocol-first contract.
  EXPECT_EQ(got.protocol_family, QStringLiteral("anthropic_messages"));
  EXPECT_EQ(got.auth_type, QStringLiteral("bearer"));
  EXPECT_EQ(got.structured_output_mode, QStringLiteral("tool"));
  EXPECT_EQ(got.recommended_rendition, QStringLiteral("preview"));
}

TEST_F(AiProviderPresetTest, NoRawApiKeyIsPersisted) {
  // The controller has NO API to accept a raw API key — only a non-secret slot
  // label and a display-only mask. Persist a preset that a user might naively
  // think carries a key, then assert the ai/preset group holds no secret-shaped
  // value and no key-named key.
  alcedo::AiProviderPreset preset = SamplePreset();
  preset.credential_slot  = QStringLiteral("opencode_api_key");  // a label, not a secret
  preset.masked_key_label = QStringLiteral("sk-…4f2a");          // a mask, not a secret
  alcedo::AiProviderPresetController controller;
  controller.SetFromPreset(preset);

  QSettings s;
  s.beginGroup(QStringLiteral("ai/preset"));
  const QStringList keys = s.allKeys();
  // No key-named setting exists (no field that could hold a raw secret).
  for (const QString& key : keys) {
    const QString lower = key.toLower();
    EXPECT_FALSE(lower.contains("apikey") || lower == "key" || lower.contains("secret") ||
                 lower.contains("password") || lower.contains("token"))
        << "unexpected key-shaped setting: " << key.toStdString();
  }
  // No persisted value is a full raw secret. A real OpenAI-style key is
  // `sk-` followed by a long run of alphanumerics; the masked_key_label
  // ("sk-…4f2a") is a short display mask containing an ellipsis and so does
  // NOT match a full-key pattern — that is the distinction this asserts.
  const QRegularExpression full_key(QStringLiteral("sk-[A-Za-z0-9_-]{20,}"));
  for (const QString& key : keys) {
    const QString value = s.value(key).toString();
    EXPECT_FALSE(full_key.match(value).hasMatch())
        << "raw API key leaked into " << key.toStdString();
    EXPECT_FALSE(value.contains(QStringLiteral("Bearer "), Qt::CaseInsensitive))
        << "Bearer literal leaked into " << key.toStdString();
  }
  // The credential slot label IS persisted (it is non-secret metadata), but it
  // is a slot name, not key material.
  EXPECT_EQ(s.value(QStringLiteral("credentialSlot")).toString(), QStringLiteral("opencode_api_key"));
  // The masked label is persisted for display but is a mask, not a full key.
  EXPECT_EQ(s.value(QStringLiteral("maskedKeyLabel")).toString(), QStringLiteral("sk-…4f2a"));
  s.endGroup();
}
