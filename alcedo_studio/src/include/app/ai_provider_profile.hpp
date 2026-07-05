//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QObject>
#include <QString>
#include <QVariantList>
#include <QVariantMap>
#include <cstdint>
#include <filesystem>
#include <memory>
#include <optional>
#include <string>
#include <vector>

#include "app/ai_credential_store.hpp"

namespace alcedo {

struct AiProviderModelEntry {
  QString model_id;
  QString display_name;
  bool    supports_vision            = true;
  bool    supports_structured_output = true;
  bool    live_confirmed             = false;
  qint64  max_image_bytes            = 4194304;
  QString recommended_rendition      = "preview";
};

/// One user-created remote image-analysis provider profile.
///
/// The profile stores only non-secret metadata. `credential_slot` is a stable
/// host-minted slot label for the OS credential store, never a secret. The raw
/// API key enters only SaveApiKey(), is handed to IAiCredentialStore, and is then
/// cleared from process memory as far as this layer can control.
struct AiProviderProfile {
  QString                           uuid;
  QString                           display_name;
  QString                           based_on_template;
  QString                           credential_slot;
  QString                           masked_key_label;
  bool                              remember_key = true;
  qint64                            last_used_ms = 0;

  // Sidecar ProviderConfig-shaped fields.
  QString                           provider_id;  // host-minted, unique per profile
  QString                           driver;
  QString                           base_url;
  QString                           endpoint;
  QString                           models_endpoint;
  QString                           models_response_data_json_pointer;
  QString                           auth_type = "bearer";
  QString                           model_id;
  QString                           model_display_name;
  QString                           structured_output_mode   = "tool";
  bool                              structured_output_strict = true;
  QString                           response_content_json_pointer;
  QString                           response_usage_json_pointer = "/usage";
  QString                           response_provider_request_id_json_pointer;
  QString                           response_provider_request_id_header;
  qint64                            timeout_ms            = 60000;
  qint64                            max_image_bytes       = 4194304;
  qint64                            max_output_tokens     = 1200;
  double                            temperature           = 0.2;
  QString                           recommended_rendition = "preview";
  std::vector<AiProviderModelEntry> models;
};

class AiProviderProfileController final : public QObject {
  Q_OBJECT
  Q_PROPERTY(QVariantList profiles READ Profiles NOTIFY ProfilesChanged)
  Q_PROPERTY(QVariantList templateOptions READ TemplateOptions CONSTANT)
  Q_PROPERTY(QString activeProfileId READ ActiveProfileId NOTIFY ProfilesChanged)
  Q_PROPERTY(QString outputLanguage READ OutputLanguage NOTIFY ProfilesChanged)
  Q_PROPERTY(QString activeDisplayName READ ActiveDisplayName NOTIFY ProfilesChanged)
  Q_PROPERTY(QString activeModelDisplayName READ ActiveModelDisplayName NOTIFY ProfilesChanged)
  Q_PROPERTY(bool hasProfiles READ HasProfiles NOTIFY ProfilesChanged)
  Q_PROPERTY(bool sidecarConfigsDirty READ SidecarConfigsDirty NOTIFY ProfilesChanged)

 public:
  explicit AiProviderProfileController(QObject* parent = nullptr);
  AiProviderProfileController(std::filesystem::path               storage_file,
                              std::filesystem::path               sidecar_config_dir,
                              std::shared_ptr<IAiCredentialStore> credential_store,
                              QObject*                            parent = nullptr);

  QVariantList Profiles() const;
  QVariantList TemplateOptions() const;
  QString      ActiveProfileId() const { return active_profile_id_; }
  QString      OutputLanguage() const { return output_language_; }
  QString      ActiveDisplayName() const;
  QString      ActiveModelDisplayName() const;
  bool         HasProfiles() const { return !profiles_.empty(); }
  bool         SidecarConfigsDirty() const { return sidecar_configs_dirty_; }

  auto CredentialStore() const -> std::shared_ptr<IAiCredentialStore> { return credential_store_; }
  auto ActiveProfile() const -> std::optional<AiProviderProfile>;
  auto ProfileById(const QString& profile_id) const -> std::optional<AiProviderProfile>;
  auto SidecarConfigDir() const -> std::filesystem::path { return sidecar_config_dir_; }

  /// Writes generated ProviderConfig JSON files for every profile and clears the
  /// dirty flag. Safe to call before every remote analysis/test; it is cheap when
  /// no profile metadata changed.
  auto PrepareSidecarConfigDir(std::string* error) -> bool;

  Q_INVOKABLE QVariantMap  Profile(const QString& profile_id) const;
  Q_INVOKABLE QVariantList ModelOptions(const QString& profile_id) const;
  Q_INVOKABLE QString      AddProfileFromTemplate(const QString& template_id);
  Q_INVOKABLE QString      CloneProfile(const QString& profile_id);
  Q_INVOKABLE bool         DeleteProfile(const QString& profile_id, bool delete_credential);
  Q_INVOKABLE bool         SetActiveProfile(const QString& profile_id);
  Q_INVOKABLE bool         SetOutputLanguage(const QString& value);
  Q_INVOKABLE bool         SetProfileField(const QString& profile_id, const QString& field,
                                           const QVariant& value);
  Q_INVOKABLE QString      SaveApiKey(const QString& profile_id, const QString& secret);
  Q_INVOKABLE QString      ImportCodexAuth(const QString& profile_id);
  Q_INVOKABLE QString      OpenCodexLogin();
  Q_INVOKABLE void         DeleteApiKey(const QString& profile_id);

  void SetDiscoveredModels(const QString& profile_id, const QVariantList& models);

 signals:
  void ProfilesChanged();

 private:
  auto FindProfile(const QString& profile_id) -> AiProviderProfile*;
  auto FindProfile(const QString& profile_id) const -> const AiProviderProfile*;
  void Load();
  void Save();
  void MarkSidecarConfigsDirty();
  void EnsureActiveProfile();
  auto UniqueDisplayName(const QString& base, const QString& exclude_profile_id = {}) const
      -> QString;

  static auto DefaultStorageFile() -> std::filesystem::path;
  static auto DefaultSidecarConfigDir(const std::filesystem::path& storage_file)
      -> std::filesystem::path;

  std::filesystem::path               storage_file_;
  std::filesystem::path               sidecar_config_dir_;
  std::shared_ptr<IAiCredentialStore> credential_store_;
  std::vector<AiProviderProfile>      profiles_;
  QString                             active_profile_id_;
  QString                             output_language_       = "follow";
  bool                                sidecar_configs_dirty_ = true;
};

}  // namespace alcedo
