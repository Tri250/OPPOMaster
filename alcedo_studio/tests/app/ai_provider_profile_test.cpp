//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/ai_provider_profile.hpp"

#include <gtest/gtest.h>

#include <QCoreApplication>
#include <QDir>
#include <QFile>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QSet>
#include <QString>
#include <QTemporaryDir>
#include <QVariantList>
#include <QVariantMap>
#include <filesystem>
#include <memory>
#include <string>

namespace {

#ifdef _WIN32
auto FsPathFromQStringForTest(const QString& path) -> std::filesystem::path {
  return std::filesystem::path(path.toStdWString());
}
#else
auto FsPathFromQStringForTest(const QString& path) -> std::filesystem::path {
  return std::filesystem::path(path.toStdString());
}
#endif

auto ReadJsonFile(const QString& path) -> QJsonObject {
  QFile file(path);
  EXPECT_TRUE(file.open(QIODevice::ReadOnly)) << path.toStdString();
  const auto doc = QJsonDocument::fromJson(file.readAll());
  EXPECT_TRUE(doc.isObject()) << path.toStdString();
  return doc.object();
}

class AiProviderProfileTest : public ::testing::Test {
 protected:
  static void SetUpTestSuite() {
    static int    argc = 0;
    static char** argv = nullptr;
    if (app_ == nullptr) {
      app_ = new QCoreApplication(argc, argv);
    }
    QCoreApplication::setOrganizationName(QStringLiteral("PuerhLabTest"));
    QCoreApplication::setApplicationName(QStringLiteral("AiProviderProfileTest"));
  }

  AiProviderProfileTest()
      : store_(std::make_shared<alcedo::InMemoryAiCredentialStore>()),
        controller_(StorageFile(), ConfigDir(), store_) {}

  auto StorageFile() const -> std::filesystem::path {
    return FsPathFromQStringForTest(temp_.filePath(QStringLiteral("ai_providers.json")));
  }

  auto ConfigDir() const -> std::filesystem::path {
    return FsPathFromQStringForTest(temp_.filePath(QStringLiteral("provider_configs")));
  }

  auto StorageFileQt() const -> QString {
    return temp_.filePath(QStringLiteral("ai_providers.json"));
  }

  auto ConfigFileQt(const QString& provider_id) const -> QString {
    return temp_.filePath(QStringLiteral("provider_configs/") + provider_id +
                          QStringLiteral(".json"));
  }

  auto Profile(const QString& id) const -> QVariantMap { return controller_.Profile(id); }

  static QCoreApplication*                           app_;
  QTemporaryDir                                      temp_;
  std::shared_ptr<alcedo::InMemoryAiCredentialStore> store_;
  alcedo::AiProviderProfileController                controller_;
};

QCoreApplication* AiProviderProfileTest::app_ = nullptr;

TEST_F(AiProviderProfileTest, StartsEmptyAndOffersOnlyFrontend1bTemplates) {
  EXPECT_TRUE(controller_.Profiles().isEmpty());
  EXPECT_TRUE(controller_.ActiveProfileId().isEmpty());
  EXPECT_FALSE(controller_.HasProfiles());

  QSet<QString> ids;
  for (const auto& option : controller_.TemplateOptions()) {
    ids.insert(option.toMap().value(QStringLiteral("templateId")).toString());
  }
  EXPECT_EQ(ids.size(), 5);
  EXPECT_TRUE(ids.contains(QStringLiteral("opencode_go_anthropic")));
  EXPECT_TRUE(ids.contains(QStringLiteral("opencode_go_openai")));
  EXPECT_TRUE(ids.contains(QStringLiteral("volcengine_ark")));
  EXPECT_TRUE(ids.contains(QStringLiteral("volcengine_ark_coding")));
  EXPECT_TRUE(ids.contains(QStringLiteral("custom")));
  EXPECT_FALSE(ids.contains(QStringLiteral("openrouter")));
}

TEST_F(AiProviderProfileTest, AddCreatesActiveProfilesWithUniqueIdsAndCredentialSlots) {
  const QString first = controller_.AddProfileFromTemplate(QStringLiteral("opencode_go_anthropic"));
  const QString second =
      controller_.AddProfileFromTemplate(QStringLiteral("opencode_go_anthropic"));

  ASSERT_FALSE(first.isEmpty());
  ASSERT_FALSE(second.isEmpty());
  EXPECT_NE(first, second);
  EXPECT_EQ(controller_.ActiveProfileId(), second);
  EXPECT_EQ(controller_.Profiles().size(), 2);

  const QVariantMap p1 = Profile(first);
  const QVariantMap p2 = Profile(second);
  EXPECT_NE(p1.value(QStringLiteral("providerId")).toString(),
            p2.value(QStringLiteral("providerId")).toString());
  EXPECT_NE(p1.value(QStringLiteral("credentialSlot")).toString(),
            p2.value(QStringLiteral("credentialSlot")).toString());
  EXPECT_TRUE(
      p1.value(QStringLiteral("providerId")).toString().startsWith(QStringLiteral("profile_")));
  EXPECT_TRUE(p1.value(QStringLiteral("credentialSlot"))
                  .toString()
                  .startsWith(QStringLiteral("alcedo_ai_")));
}

TEST_F(AiProviderProfileTest, CloneCopiesMetadataButNeverCopiesCredential) {
  const QString source = controller_.AddProfileFromTemplate(QStringLiteral("volcengine_ark"));
  ASSERT_TRUE(
      controller_.SaveApiKey(source, QStringLiteral("sk-clone-source-secret-123456")).isEmpty());
  const QVariantMap source_profile = Profile(source);
  const QString     source_slot = source_profile.value(QStringLiteral("credentialSlot")).toString();
  ASSERT_TRUE(store_->HasCredential(source_slot.toStdString()));

  const QString clone = controller_.CloneProfile(source);
  ASSERT_FALSE(clone.isEmpty());
  EXPECT_EQ(controller_.ActiveProfileId(), source);

  const QVariantMap clone_profile = Profile(clone);
  EXPECT_EQ(clone_profile.value(QStringLiteral("baseUrl")).toString(),
            source_profile.value(QStringLiteral("baseUrl")).toString());
  EXPECT_EQ(clone_profile.value(QStringLiteral("modelId")).toString(),
            source_profile.value(QStringLiteral("modelId")).toString());
  EXPECT_NE(clone_profile.value(QStringLiteral("credentialSlot")).toString(), source_slot);
  EXPECT_FALSE(clone_profile.value(QStringLiteral("credentialAvailable")).toBool());
  EXPECT_TRUE(clone_profile.value(QStringLiteral("maskedKeyLabel")).toString().isEmpty());
  EXPECT_FALSE(store_->HasCredential(
      clone_profile.value(QStringLiteral("credentialSlot")).toString().toStdString()));
}

TEST_F(AiProviderProfileTest, DeleteActiveProfileCanWipeCredentialAndFallsBackToRemainingProfile) {
  const QString first = controller_.AddProfileFromTemplate(QStringLiteral("opencode_go_anthropic"));
  const QString second = controller_.AddProfileFromTemplate(QStringLiteral("opencode_go_openai"));
  ASSERT_EQ(controller_.ActiveProfileId(), second);
  ASSERT_TRUE(
      controller_.SaveApiKey(second, QStringLiteral("sk-delete-me-secret-123456")).isEmpty());
  const QString slot = Profile(second).value(QStringLiteral("credentialSlot")).toString();
  ASSERT_TRUE(store_->HasCredential(slot.toStdString()));

  EXPECT_TRUE(controller_.DeleteProfile(second, true));
  EXPECT_EQ(controller_.Profiles().size(), 1);
  EXPECT_EQ(controller_.ActiveProfileId(), first);
  EXPECT_FALSE(store_->HasCredential(slot.toStdString()));
}

TEST_F(AiProviderProfileTest, DeleteProfileCanKeepCredentialWhenRequested) {
  const QString id = controller_.AddProfileFromTemplate(QStringLiteral("opencode_go_openai"));
  ASSERT_TRUE(controller_.SaveApiKey(id, QStringLiteral("sk-keep-me-secret-123456")).isEmpty());
  const QString slot = Profile(id).value(QStringLiteral("credentialSlot")).toString();
  ASSERT_TRUE(store_->HasCredential(slot.toStdString()));

  EXPECT_TRUE(controller_.DeleteProfile(id, false));
  EXPECT_TRUE(controller_.Profiles().isEmpty());
  EXPECT_FALSE(controller_.HasProfiles());
  EXPECT_TRUE(store_->HasCredential(slot.toStdString()));
}

TEST_F(AiProviderProfileTest, EditingInactiveProfileDoesNotActivateIt) {
  const QString first = controller_.AddProfileFromTemplate(QStringLiteral("opencode_go_anthropic"));
  const QString second = controller_.AddProfileFromTemplate(QStringLiteral("volcengine_ark"));
  ASSERT_EQ(controller_.ActiveProfileId(), second);

  EXPECT_TRUE(controller_.SetProfileField(first, QStringLiteral("displayName"),
                                          QStringLiteral("Edited inactive profile")));
  EXPECT_EQ(controller_.ActiveProfileId(), second);
  EXPECT_EQ(Profile(first).value(QStringLiteral("displayName")).toString(),
            QStringLiteral("Edited inactive profile"));
}

TEST_F(AiProviderProfileTest, SettingsPersistInOneAiProvidersJsonFile) {
  const QString id = controller_.AddProfileFromTemplate(QStringLiteral("opencode_go_openai"));
  ASSERT_TRUE(controller_.SetOutputLanguage(QStringLiteral("zh")));
  ASSERT_TRUE(controller_.SetProfileField(id, QStringLiteral("baseUrl"),
                                          QStringLiteral("https://example.test/v1")));
  ASSERT_TRUE(controller_.SetProfileField(id, QStringLiteral("modelId"),
                                          QStringLiteral("vision-live-model")));

  alcedo::AiProviderProfileController reloaded(StorageFile(), ConfigDir(), store_);
  EXPECT_EQ(reloaded.ActiveProfileId(), id);
  EXPECT_EQ(reloaded.OutputLanguage(), QStringLiteral("zh"));
  EXPECT_EQ(reloaded.Profiles().size(), 1);
  const QVariantMap p = reloaded.Profile(id);
  EXPECT_EQ(p.value(QStringLiteral("baseUrl")).toString(),
            QStringLiteral("https://example.test/v1"));
  EXPECT_EQ(p.value(QStringLiteral("modelId")).toString(), QStringLiteral("vision-live-model"));

  const QJsonObject root = ReadJsonFile(StorageFileQt());
  EXPECT_EQ(root.value(QStringLiteral("schema_version")).toInt(), 1);
  EXPECT_TRUE(root.contains(QStringLiteral("profiles")));
}

TEST_F(AiProviderProfileTest, ReloadMigratesOldOpencodeGoDefaultModels) {
  const QString openai_id = controller_.AddProfileFromTemplate(QStringLiteral("opencode_go_openai"));
  const QString anthropic_id =
      controller_.AddProfileFromTemplate(QStringLiteral("opencode_go_anthropic"));

  QFile file(StorageFileQt());
  ASSERT_TRUE(file.open(QIODevice::ReadOnly));
  QJsonObject root = QJsonDocument::fromJson(file.readAll()).object();
  file.close();

  QJsonArray profiles = root.value(QStringLiteral("profiles")).toArray();
  for (auto value : profiles) {
    QJsonObject profile = value.toObject();
    if (profile.value(QStringLiteral("uuid")).toString() == openai_id) {
      profile.insert(QStringLiteral("model_id"), QStringLiteral("gpt-4o"));
      profile.insert(QStringLiteral("model_display_name"), QStringLiteral("GPT-4o"));
    } else if (profile.value(QStringLiteral("uuid")).toString() == anthropic_id) {
      profile.insert(QStringLiteral("model_id"), QStringLiteral("claude-sonnet-4-5"));
      profile.insert(QStringLiteral("model_display_name"), QStringLiteral("Claude Sonnet 4.5"));
    }
    for (int i = 0; i < profiles.size(); ++i) {
      if (profiles.at(i).toObject().value(QStringLiteral("uuid")) ==
          profile.value(QStringLiteral("uuid"))) {
        profiles.replace(i, profile);
        break;
      }
    }
  }
  root.insert(QStringLiteral("profiles"), profiles);
  ASSERT_TRUE(file.open(QIODevice::WriteOnly | QIODevice::Truncate));
  file.write(QJsonDocument(root).toJson(QJsonDocument::Indented));
  file.close();

  alcedo::AiProviderProfileController reloaded(StorageFile(), ConfigDir(), store_);
  EXPECT_EQ(reloaded.Profile(openai_id).value(QStringLiteral("modelId")).toString(),
            QStringLiteral("kimi-k2.7-code"));
  EXPECT_EQ(reloaded.Profile(anthropic_id).value(QStringLiteral("modelId")).toString(),
            QStringLiteral("qwen3.7-plus"));
  EXPECT_EQ(reloaded.Profile(anthropic_id).value(QStringLiteral("authType")).toString(),
            QStringLiteral("api_key_header"));
}
TEST_F(AiProviderProfileTest, PrepareSidecarConfigDirWritesProviderConfigsAndRemovesStaleFiles) {
  const QString     id      = controller_.AddProfileFromTemplate(QStringLiteral("volcengine_ark"));
  const QVariantMap profile = Profile(id);
  const QString     provider_id = profile.value(QStringLiteral("providerId")).toString();
  const QString     stale_path  = temp_.filePath(QStringLiteral("provider_configs/stale.json"));
  ASSERT_TRUE(QDir().mkpath(temp_.filePath(QStringLiteral("provider_configs"))));
  QFile stale(stale_path);
  ASSERT_TRUE(stale.open(QIODevice::WriteOnly));
  stale.write("{}");
  stale.close();

  std::string error;
  EXPECT_TRUE(controller_.PrepareSidecarConfigDir(&error)) << error;
  EXPECT_FALSE(QFile::exists(stale_path));

  const QJsonObject root = ReadJsonFile(ConfigFileQt(provider_id));
  EXPECT_EQ(root.value(QStringLiteral("provider_id")).toString(), provider_id);
  EXPECT_EQ(root.value(QStringLiteral("base_url")).toString(),
            profile.value(QStringLiteral("baseUrl")).toString());
  EXPECT_EQ(root.value(QStringLiteral("endpoint")).toString(),
            profile.value(QStringLiteral("endpoint")).toString());
  EXPECT_EQ(root.value(QStringLiteral("auth"))
                .toObject()
                .value(QStringLiteral("credential_slot"))
                .toString(),
            profile.value(QStringLiteral("credentialSlot")).toString());
  EXPECT_EQ(
      root.value(QStringLiteral("defaults")).toObject().value(QStringLiteral("model")).toString(),
      profile.value(QStringLiteral("modelId")).toString());
  EXPECT_FALSE(controller_.SidecarConfigsDirty());
}

TEST_F(AiProviderProfileTest, OpenCodeOpenAiConfigIncludesDocumentedAndDiscoverableKimiIds) {
  const QString     id = controller_.AddProfileFromTemplate(QStringLiteral("opencode_go_openai"));
  const QVariantMap profile = Profile(id);
  std::string       error;
  ASSERT_TRUE(controller_.PrepareSidecarConfigDir(&error)) << error;

  const QJsonObject root =
      ReadJsonFile(ConfigFileQt(profile.value(QStringLiteral("providerId")).toString()));
  QSet<QString> model_ids;
  for (const auto& value : root.value(QStringLiteral("models")).toArray()) {
    model_ids.insert(value.toObject().value(QStringLiteral("slug")).toString());
  }
  EXPECT_TRUE(model_ids.contains(QStringLiteral("kimi-k2.7-code")));
  EXPECT_TRUE(model_ids.contains(QStringLiteral("kimi-k2.7")));
}

TEST_F(AiProviderProfileTest, CcSwitchTemplatesWriteModelsResponsePath) {
  const QString     id = controller_.AddProfileFromTemplate(QStringLiteral("ccswitch_openai"));
  const QVariantMap profile = Profile(id);
  EXPECT_EQ(profile.value(QStringLiteral("modelsResponseDataJsonPointer")).toString(),
            QStringLiteral("/models"));

  std::string error;
  ASSERT_TRUE(controller_.PrepareSidecarConfigDir(&error)) << error;

  const QJsonObject root =
      ReadJsonFile(ConfigFileQt(profile.value(QStringLiteral("providerId")).toString()));
  EXPECT_EQ(root.value(QStringLiteral("models_response"))
                .toObject()
                .value(QStringLiteral("data_json_pointer"))
                .toString(),
            QStringLiteral("/models"));
}

TEST_F(AiProviderProfileTest, ExistingCcSwitchProfilesMigrateModelsResponsePath) {
  const QString id = controller_.AddProfileFromTemplate(QStringLiteral("ccswitch_openai"));

  QFile file(StorageFileQt());
  ASSERT_TRUE(file.open(QIODevice::ReadOnly));
  QJsonObject root = QJsonDocument::fromJson(file.readAll()).object();
  file.close();
  QJsonArray profiles = root.value(QStringLiteral("profiles")).toArray();
  for (int i = 0; i < profiles.size(); ++i) {
    QJsonObject profile = profiles.at(i).toObject();
    if (profile.value(QStringLiteral("uuid")).toString() == id) {
      profile.remove(QStringLiteral("models_response_data_json_pointer"));
      profiles.replace(i, profile);
      break;
    }
  }
  root.insert(QStringLiteral("profiles"), profiles);
  ASSERT_TRUE(file.open(QIODevice::WriteOnly | QIODevice::Truncate));
  file.write(QJsonDocument(root).toJson(QJsonDocument::Indented));
  file.close();

  alcedo::AiProviderProfileController reloaded(StorageFile(), ConfigDir(), store_);
  EXPECT_EQ(reloaded.Profile(id).value(QStringLiteral("modelsResponseDataJsonPointer")).toString(),
            QStringLiteral("/models"));
}

TEST_F(AiProviderProfileTest, DiscoveredModelsPersistAndBecomeSidecarModels) {
  const QString id = controller_.AddProfileFromTemplate(QStringLiteral("opencode_go_openai"));
  QVariantMap   discovered;
  discovered.insert(QStringLiteral("modelId"), QStringLiteral("live-vision-model"));
  discovered.insert(QStringLiteral("displayName"), QStringLiteral("Live Vision Model"));
  discovered.insert(QStringLiteral("supportsVision"), true);
  discovered.insert(QStringLiteral("supportsStructuredOutput"), true);
  QVariantList models;
  models.push_back(discovered);

  controller_.SetDiscoveredModels(id, models);
  ASSERT_TRUE(controller_.SetProfileField(id, QStringLiteral("modelId"),
                                          QStringLiteral("live-vision-model")));
  const QVariantMap profile = Profile(id);
  std::string       error;
  ASSERT_TRUE(controller_.PrepareSidecarConfigDir(&error)) << error;

  const QJsonObject root =
      ReadJsonFile(ConfigFileQt(profile.value(QStringLiteral("providerId")).toString()));
  bool found = false;
  for (const auto& value : root.value(QStringLiteral("models")).toArray()) {
    if (value.toObject().value(QStringLiteral("slug")).toString() ==
        QStringLiteral("live-vision-model")) {
      found = true;
    }
  }
  EXPECT_TRUE(found);

  alcedo::AiProviderProfileController reloaded(StorageFile(), ConfigDir(), store_);
  EXPECT_EQ(reloaded.Profile(id).value(QStringLiteral("modelId")).toString(),
            QStringLiteral("live-vision-model"));
}

TEST_F(AiProviderProfileTest, DiscoveredModelsDefaultToImageAnalysisCandidate) {
  const QString id = controller_.AddProfileFromTemplate(QStringLiteral("opencode_go_openai"));
  QVariantMap   discovered;
  discovered.insert(QStringLiteral("modelId"), QStringLiteral("listed-code-model"));
  discovered.insert(QStringLiteral("displayName"), QStringLiteral("Listed Code Model"));
  QVariantList models;
  models.push_back(discovered);

  controller_.SetDiscoveredModels(id, models);
  const auto options = controller_.ModelOptions(id);
  auto it = std::find_if(options.begin(), options.end(), [](const QVariant& value) {
    return value.toMap().value(QStringLiteral("modelId")).toString() ==
           QStringLiteral("listed-code-model");
  });
  ASSERT_NE(it, options.end());
  const auto model = it->toMap();
  EXPECT_TRUE(model.value(QStringLiteral("supportsVision")).toBool());
  EXPECT_TRUE(model.value(QStringLiteral("supportsStructuredOutput")).toBool());
}
TEST_F(AiProviderProfileTest, RawApiKeyNeverEntersProfileJsonOrSidecarConfig) {
  const QString id  = controller_.AddProfileFromTemplate(QStringLiteral("opencode_go_anthropic"));
  const QString raw = QStringLiteral("sk-never-persist-this-secret-123456");
  ASSERT_TRUE(controller_.SaveApiKey(id, raw).isEmpty());
  std::string error;
  ASSERT_TRUE(controller_.PrepareSidecarConfigDir(&error)) << error;

  QFile store_file(StorageFileQt());
  ASSERT_TRUE(store_file.open(QIODevice::ReadOnly));
  const QString stored_json = QString::fromUtf8(store_file.readAll());
  EXPECT_FALSE(stored_json.contains(raw));
  EXPECT_TRUE(stored_json.contains(QStringLiteral("****3456")));

  const QString provider_id = Profile(id).value(QStringLiteral("providerId")).toString();
  QFile         config_file(ConfigFileQt(provider_id));
  ASSERT_TRUE(config_file.open(QIODevice::ReadOnly));
  const QString config_json = QString::fromUtf8(config_file.readAll());
  EXPECT_FALSE(config_json.contains(raw));
  EXPECT_TRUE(config_json.contains(Profile(id).value(QStringLiteral("credentialSlot")).toString()));
}

}  // namespace
