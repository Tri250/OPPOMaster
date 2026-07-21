//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <filesystem>
#include <memory>
#include <optional>
#include <string>
#include <vector>

#include "app/ai_credential_store.hpp"
#include "app/ai_provider_profile.hpp"

namespace alcedo {

/// Format version for the portable credential bundle.
constexpr const char* kCredentialBundleSchema = "alcedo.credential_bundle.v1";

/// Metadata for one credential entry in the export bundle.
struct CredentialExportEntry {
  QString       profile_uuid;
  QString       display_name;
  QString       credential_slot;
  QString       masked_key_label;
  QString       provider_id;
  QString       driver;
  QString       base_url;
  QString       model_id;
  QString       model_display_name;
  std::string   encrypted_secret;   // AES-256-CBC encrypted API key
  std::string   iv;                 // Initialization vector
  std::string   tag;                // GCM authentication tag
};

/// Result of an import/export operation.
struct CredentialPortabilityResult {
  bool        success     = false;
  int         entries_processed = 0;
  int         entries_skipped  = 0;
  std::string error;
};

/// Handles encrypted import/export of AI credentials for device migration.
///
/// Export workflow:
///   1. Collect profiles and their credentials from the OS store
///   2. Encrypt each secret with AES-256-GCM using a user-provided password
///   3. Write a portable JSON bundle
///
/// Import workflow:
///   1. Read and validate the bundle format
///   2. Decrypt each secret with the user-provided password
///   3. Store credentials, optionally overwriting existing ones
class CredentialPortability final {
 public:
  explicit CredentialPortability(
      std::shared_ptr<IAiCredentialStore>  credential_store,
      AiProviderProfileController*         profile_controller = nullptr);

  /// Export all credentials and profile metadata to an encrypted JSON file.
  /// @param file_path  Destination path for the .alcedo_cred bundle
  /// @param password   User-provided encryption password
  auto ExportToFile(const std::filesystem::path& file_path,
                    const std::string&           password) -> CredentialPortabilityResult;

  /// Import credentials and profiles from an encrypted JSON file.
  /// @param file_path       Source path of the .alcedo_cred bundle
  /// @param password        Decryption password
  /// @param overwrite_existing  If true, replace existing credentials on conflict
  auto ImportFromFile(const std::filesystem::path& file_path,
                      const std::string&           password,
                      bool                         overwrite_existing = false)
      -> CredentialPortabilityResult;

  /// Validate a bundle file without importing. Returns the number of entries.
  auto ValidateBundle(const std::filesystem::path& file_path) -> std::optional<int>;

 private:
  auto CollectExportEntries() const -> std::vector<CredentialExportEntry>;
  auto EncryptSecret(const std::string& secret,
                     const std::string& password,
                     std::string&       iv_out,
                     std::string&       tag_out) const -> std::string;
  auto DecryptSecret(const std::string& encrypted,
                     const std::string& iv,
                     const std::string& tag,
                     const std::string& password) const -> std::optional<std::string>;
  auto DeriveKey(const std::string& password, const std::string& salt) const -> std::vector<uint8_t>;

  std::shared_ptr<IAiCredentialStore>  credential_store_;
  AiProviderProfileController*         profile_controller_;
};

}  // namespace alcedo
