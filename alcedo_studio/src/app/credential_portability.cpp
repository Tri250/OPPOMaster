//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/credential_portability.hpp"

#include <filesystem>
#include <fstream>
#include <random>
#include <sstream>
#include <stdexcept>
#include <string>
#include <vector>

#include <QCryptographicHash>
#include <QFile>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>

#include <openssl/evp.h>
#include <openssl/rand.h>
#include <openssl/err.h>

#include "utils/diagnostics/app_logging.hpp"

namespace alcedo {

namespace {

// AES-256-GCM parameters
constexpr int kKeyLength     = 32;   // 256 bits
constexpr int kIVLength      = 12;   // 96 bits for GCM (NIST recommendation)
constexpr int kTagLength     = 16;   // 128 bits
constexpr int kSaltLength    = 32;
constexpr int kPBKDF2Iter    = 600000;

auto Base64Encode(const std::string& data) -> std::string {
  QByteArray ba(data.data(), static_cast<qsizetype>(data.size()));
  return ba.toBase64().toStdString();
}

auto Base64Decode(const std::string& encoded) -> std::string {
  QByteArray ba = QByteArray::fromBase64(QByteArray::fromStdString(encoded));
  return std::string(ba.constData(), ba.size());
}

auto GenerateRandomBytes(size_t count) -> std::string {
  std::string result(count, '\0');
  if (RAND_bytes(reinterpret_cast<unsigned char*>(result.data()),
                 static_cast<int>(count)) != 1) {
    // Fallback to std::random_device if OpenSSL RAND_bytes fails
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<unsigned char> dist(0, 255);
    for (size_t i = 0; i < count; ++i) {
      result[i] = static_cast<char>(dist(gen));
    }
  }
  return result;
}

auto DeriveKeyFromPassword(const std::string& password, const std::string& salt)
    -> std::vector<uint8_t> {
  std::vector<uint8_t> key(kKeyLength);

  // Use OpenSSL's PKCS5_PBKDF2_HMAC for proper key derivation
  int result = PKCS5_PBKDF2_HMAC(
      password.data(), static_cast<int>(password.size()),
      reinterpret_cast<const unsigned char*>(salt.data()),
      static_cast<int>(salt.size()),
      kPBKDF2Iter,
      EVP_sha256(),
      kKeyLength,
      key.data());

  if (result != 1) {
    qCWarning(diag::appLog) << "PBKDF2 key derivation failed via OpenSSL;"
                            << "falling back to manual implementation.";

    // Fallback: manual PBKDF2-HMAC-SHA256 using QCryptographicHash
    QByteArray salt_bytes(salt.data(), static_cast<qsizetype>(salt.size()));
    QByteArray password_bytes(password.data(), static_cast<qsizetype>(password.size()));

    const int dkLen = kKeyLength;
    const int hLen  = 32;
    const int blocks = (dkLen + hLen - 1) / hLen;

    QByteArray pbkdf2_result;
    pbkdf2_result.reserve(dkLen);

    auto hmac_sha256 = [](const QByteArray& key, const QByteArray& data) -> QByteArray {
      QByteArray ipad(64, static_cast<char>(0x36));
      QByteArray opad(64, static_cast<char>(0x5c));
      QByteArray key_block = key;
      if (key_block.size() > 64) {
        key_block = QCryptographicHash::hash(key_block, QCryptographicHash::Sha256);
      }
      key_block.resize(64, '\0');
      for (int i = 0; i < 64; ++i) {
        ipad[i] = ipad[i] ^ key_block[i];
        opad[i] = opad[i] ^ key_block[i];
      }
      QCryptographicHash inner(QCryptographicHash::Sha256);
      inner.addData(ipad);
      inner.addData(data);
      QCryptographicHash outer(QCryptographicHash::Sha256);
      outer.addData(opad);
      outer.addData(inner.result());
      return outer.result();
    };

    for (int block = 1; block <= blocks && pbkdf2_result.size() < dkLen; ++block) {
      QByteArray U;
      U.reserve(salt_bytes.size() + 4);
      U.append(salt_bytes);
      U.append(static_cast<char>((block >> 24) & 0xFF));
      U.append(static_cast<char>((block >> 16) & 0xFF));
      U.append(static_cast<char>((block >> 8)  & 0xFF));
      U.append(static_cast<char>((block)       & 0xFF));

      QByteArray T = hmac_sha256(password_bytes, U);
      QByteArray U_result = T;

      for (int iter = 1; iter < kPBKDF2Iter; ++iter) {
        U = hmac_sha256(password_bytes, T);
        T = U;
        for (int j = 0; j < hLen; ++j) {
          U_result[j] = U_result[j] ^ T[j];
        }
      }
      pbkdf2_result.append(U_result);
    }

    pbkdf2_result.resize(dkLen);
    std::copy(pbkdf2_result.constBegin(), pbkdf2_result.constEnd(), key.begin());
  }

  return key;
}

/// AES-256-GCM encryption using OpenSSL EVP.
auto Aes256GcmEncrypt(const std::string& plaintext,
                       const std::vector<uint8_t>& key,
                       const std::string& iv,
                       std::string& tag_out) -> std::string {
  std::string ciphertext(plaintext.size(), '\0');

  EVP_CIPHER_CTX* ctx = EVP_CIPHER_CTX_new();
  if (!ctx) {
    throw std::runtime_error("Failed to create EVP_CIPHER_CTX for AES-256-GCM encryption");
  }

  int len = 0;
  int ciphertext_len = 0;

  // Initialize the encryption operation
  if (EVP_EncryptInit_ex(ctx, EVP_aes_256_gcm(), nullptr, nullptr, nullptr) != 1) {
    EVP_CIPHER_CTX_free(ctx);
    throw std::runtime_error("EVP_EncryptInit_ex failed for AES-256-GCM");
  }

  // Set IV length (default is 12 bytes for GCM, but we set it explicitly)
  if (EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_SET_IVLEN, kIVLength, nullptr) != 1) {
    EVP_CIPHER_CTX_free(ctx);
    throw std::runtime_error("Failed to set GCM IV length");
  }

  // Initialize key and IV
  if (EVP_EncryptInit_ex(ctx, nullptr, nullptr,
                         key.data(),
                         reinterpret_cast<const unsigned char*>(iv.data())) != 1) {
    EVP_CIPHER_CTX_free(ctx);
    throw std::runtime_error("EVP_EncryptInit_ex (key/IV) failed for AES-256-GCM");
  }

  // Provide the plaintext to be encrypted
  if (EVP_EncryptUpdate(ctx,
                        reinterpret_cast<unsigned char*>(ciphertext.data()), &len,
                        reinterpret_cast<const unsigned char*>(plaintext.data()),
                        static_cast<int>(plaintext.size())) != 1) {
    EVP_CIPHER_CTX_free(ctx);
    throw std::runtime_error("EVP_EncryptUpdate failed for AES-256-GCM");
  }
  ciphertext_len = len;

  // Finalize encryption (GCM: no additional ciphertext on finalize)
  if (EVP_EncryptFinal_ex(ctx,
                          reinterpret_cast<unsigned char*>(ciphertext.data()) + len,
                          &len) != 1) {
    EVP_CIPHER_CTX_free(ctx);
    throw std::runtime_error("EVP_EncryptFinal_ex failed for AES-256-GCM");
  }
  ciphertext_len += len;

  // Get the authentication tag
  tag_out.resize(kTagLength);
  if (EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_GET_TAG, kTagLength,
                          tag_out.data()) != 1) {
    EVP_CIPHER_CTX_free(ctx);
    throw std::runtime_error("Failed to get GCM authentication tag");
  }

  EVP_CIPHER_CTX_free(ctx);
  ciphertext.resize(static_cast<size_t>(ciphertext_len));
  return ciphertext;
}

/// AES-256-GCM decryption using OpenSSL EVP.
auto Aes256GcmDecrypt(const std::string& ciphertext,
                       const std::vector<uint8_t>& key,
                       const std::string& iv,
                       const std::string& tag) -> std::optional<std::string> {
  EVP_CIPHER_CTX* ctx = EVP_CIPHER_CTX_new();
  if (!ctx) {
    return std::nullopt;
  }

  int len = 0;
  int plaintext_len = 0;

  // Initialize the decryption operation
  if (EVP_DecryptInit_ex(ctx, EVP_aes_256_gcm(), nullptr, nullptr, nullptr) != 1) {
    EVP_CIPHER_CTX_free(ctx);
    return std::nullopt;
  }

  // Set IV length
  if (EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_SET_IVLEN, kIVLength, nullptr) != 1) {
    EVP_CIPHER_CTX_free(ctx);
    return std::nullopt;
  }

  // Initialize key and IV
  if (EVP_DecryptInit_ex(ctx, nullptr, nullptr,
                         key.data(),
                         reinterpret_cast<const unsigned char*>(iv.data())) != 1) {
    EVP_CIPHER_CTX_free(ctx);
    return std::nullopt;
  }

  // Provide the ciphertext to be decrypted
  std::string plaintext(ciphertext.size(), '\0');
  if (EVP_DecryptUpdate(ctx,
                        reinterpret_cast<unsigned char*>(plaintext.data()), &len,
                        reinterpret_cast<const unsigned char*>(ciphertext.data()),
                        static_cast<int>(ciphertext.size())) != 1) {
    EVP_CIPHER_CTX_free(ctx);
    return std::nullopt;
  }
  plaintext_len = len;

  // Set the expected authentication tag before finalizing
  std::string tag_copy = tag;  // Need non-const for EVP_CIPHER_CTX_ctrl
  if (EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_SET_TAG, kTagLength,
                          tag_copy.data()) != 1) {
    EVP_CIPHER_CTX_free(ctx);
    return std::nullopt;
  }

  // Finalize decryption: this verifies the authentication tag.
  // If the tag does not match, EVP_DecryptFinal_ex returns 0.
  if (EVP_DecryptFinal_ex(ctx,
                          reinterpret_cast<unsigned char*>(plaintext.data()) + len,
                          &len) != 1) {
    EVP_CIPHER_CTX_free(ctx);
    // Authentication tag mismatch — data is corrupted or wrong password
    return std::nullopt;
  }
  plaintext_len += len;

  EVP_CIPHER_CTX_free(ctx);
  plaintext.resize(static_cast<size_t>(plaintext_len));
  return plaintext;
}

}  // namespace

CredentialPortability::CredentialPortability(
    std::shared_ptr<IAiCredentialStore>  credential_store,
    AiProviderProfileController*         profile_controller)
    : credential_store_(std::move(credential_store)),
      profile_controller_(profile_controller) {}

auto CredentialPortability::CollectExportEntries() const
    -> std::vector<CredentialExportEntry> {
  std::vector<CredentialExportEntry> entries;
  if (!profile_controller_) {
    return entries;
  }

  const QVariantList profiles = profile_controller_->Profiles();
  for (const QVariant& pv : profiles) {
    const QVariantMap pm        = pv.toMap();
    const QString     slot      = pm.value("credential_slot").toString();
    if (slot.isEmpty()) {
      continue;
    }

    std::string secret;
    std::string error;
    if (!credential_store_->LoadCredential(slot.toStdString(), &secret, &error)) {
      continue;  // Skip profiles without stored credentials
    }

    CredentialExportEntry entry;
    entry.profile_uuid       = pm.value("uuid").toString();
    entry.display_name       = pm.value("display_name").toString();
    entry.credential_slot    = slot;
    entry.masked_key_label   = pm.value("masked_key_label").toString();
    entry.provider_id        = pm.value("provider_id").toString();
    entry.driver             = pm.value("driver").toString();
    entry.base_url           = pm.value("base_url").toString();
    entry.model_id           = pm.value("model_id").toString();
    entry.model_display_name = pm.value("model_display_name").toString();

    // Store the raw secret temporarily; it will be encrypted in ExportToFile
    entry.encrypted_secret = Base64Encode(secret);

    entries.push_back(std::move(entry));
  }
  return entries;
}

auto CredentialPortability::EncryptSecret(const std::string& secret,
                                          const std::string& password,
                                          std::string&       iv_out,
                                          std::string&       tag_out) const
    -> std::string {
  // Generate a cryptographically random IV (12 bytes for GCM)
  iv_out = GenerateRandomBytes(kIVLength);

  // Generate a salt for key derivation
  std::string salt = GenerateRandomBytes(kSaltLength);

  // Derive 256-bit key from password using PBKDF2-HMAC-SHA256
  std::vector<uint8_t> key;
  if (!password.empty()) {
    key = DeriveKey(password, salt);
  } else {
    // No password: derive from a machine-specific salt
    key = DeriveKey("alcedo_studio_portable", salt);
  }

  // Encrypt with AES-256-GCM
  std::string encrypted = Aes256GcmEncrypt(secret, key, iv_out, tag_out);

  // Prepend salt to the encrypted data so we can recover it during decryption
  std::string result = salt + encrypted;
  return Base64Encode(result);
}

auto CredentialPortability::DecryptSecret(const std::string& encrypted,
                                          const std::string& iv,
                                          const std::string& tag,
                                          const std::string& password) const
    -> std::optional<std::string> {
  std::string decoded = Base64Decode(encrypted);
  if (decoded.size() < static_cast<size_t>(kSaltLength)) {
    return std::nullopt;
  }

  std::string salt(decoded.data(), kSaltLength);
  std::string ciphertext(decoded.data() + kSaltLength, decoded.size() - kSaltLength);

  std::vector<uint8_t> key;
  if (!password.empty()) {
    key = DeriveKey(password, salt);
  } else {
    key = DeriveKey("alcedo_studio_portable", salt);
  }

  // Decrypt with AES-256-GCM; authentication tag is verified by EVP_DecryptFinal_ex
  return Aes256GcmDecrypt(ciphertext, key, iv, tag);
}

auto CredentialPortability::DeriveKey(const std::string& password,
                                      const std::string& salt) const
    -> std::vector<uint8_t> {
  return DeriveKeyFromPassword(password, salt);
}

auto CredentialPortability::ExportToFile(const std::filesystem::path& file_path,
                                          const std::string&           password)
    -> CredentialPortabilityResult {
  CredentialPortabilityResult result;

  try {
    auto entries = CollectExportEntries();
    if (entries.empty()) {
      result.success = true;
      result.entries_processed = 0;
      return result;
    }

    // Re-encrypt with the provided password
    QJsonArray entries_array;
    for (auto& entry : entries) {
      // Decrypt the base64'd raw secret collected earlier
      std::string raw_secret = Base64Decode(entry.encrypted_secret);

      // Encrypt with the user's password
      std::string iv, tag;
      entry.encrypted_secret = EncryptSecret(raw_secret, password, iv, tag);
      entry.iv  = iv;
      entry.tag = tag;

      QJsonObject obj;
      obj["profile_uuid"]       = entry.profile_uuid;
      obj["display_name"]       = entry.display_name;
      obj["credential_slot"]    = entry.credential_slot;
      obj["masked_key_label"]   = entry.masked_key_label;
      obj["provider_id"]        = entry.provider_id;
      obj["driver"]             = entry.driver;
      obj["base_url"]           = entry.base_url;
      obj["model_id"]           = entry.model_id;
      obj["model_display_name"] = entry.model_display_name;
      obj["encrypted_secret"]   = QString::fromStdString(entry.encrypted_secret);
      obj["iv"]                 = QString::fromStdString(Base64Encode(entry.iv));
      obj["tag"]                = QString::fromStdString(Base64Encode(entry.tag));
      entries_array.append(obj);
    }

    QJsonObject root;
    root["schema"]   = QString::fromStdString(kCredentialBundleSchema);
    root["entries"]  = entries_array;
    root["export_time"] = QDateTime::currentDateTime().toString(Qt::ISODate);
    root["app_version"] = QString::fromStdString(ALCEDO_APP_VERSION);

    QJsonDocument doc(root);

    // Ensure parent directory exists
    std::error_code ec;
    std::filesystem::create_directories(file_path.parent_path(), ec);
    if (ec) {
      result.error = "Could not create export directory: " + ec.message();
      return result;
    }

    QFile file(QString::fromStdString(file_path.string()));
    if (!file.open(QIODevice::WriteOnly | QIODevice::Truncate)) {
      result.error = "Could not open file for writing";
      return result;
    }
    file.write(doc.toJson(QJsonDocument::Indented));
    file.close();

    result.success = true;
    result.entries_processed = static_cast<int>(entries.size());

    qCInfo(diag::appLog) << "Credential export completed:"
                              << entries.size() << "entries exported to"
                              << file_path.c_str();
  } catch (const std::exception& e) {
    result.error = std::string("Export failed: ") + e.what();
  }

  return result;
}

auto CredentialPortability::ImportFromFile(const std::filesystem::path& file_path,
                                            const std::string&           password,
                                            bool                         overwrite_existing)
    -> CredentialPortabilityResult {
  CredentialPortabilityResult result;

  try {
    QFile file(QString::fromStdString(file_path.string()));
    if (!file.open(QIODevice::ReadOnly)) {
      result.error = "Could not open import file";
      return result;
    }

    QJsonDocument doc = QJsonDocument::fromJson(file.readAll());
    file.close();

    if (!doc.isObject()) {
      result.error = "Invalid credential bundle format";
      return result;
    }

    QJsonObject root = doc.object();
    QString schema = root.value("schema").toString();
    if (schema != QString::fromStdString(kCredentialBundleSchema)) {
      result.error = "Unsupported credential bundle schema: " + schema.toStdString();
      return result;
    }

    QJsonArray entries = root.value("entries").toArray();
    for (const QJsonValue& val : entries) {
      if (!val.isObject()) continue;
      QJsonObject obj = val.toObject();

      std::string encrypted = obj.value("encrypted_secret").toString().toStdString();
      std::string iv        = Base64Decode(obj.value("iv").toString().toStdString());
      std::string tag       = Base64Decode(obj.value("tag").toString().toStdString());
      std::string slot      = obj.value("credential_slot").toString().toStdString();

      // Check for existing credential
      if (!overwrite_existing && credential_store_->HasCredential(slot)) {
        result.entries_skipped++;
        continue;
      }

      // Decrypt
      auto decrypted = DecryptSecret(encrypted, iv, tag, password);
      if (!decrypted.has_value()) {
        qCWarning(diag::appLog) << "Failed to decrypt credential for slot:"
                                     << slot.c_str();
        result.entries_skipped++;
        continue;
      }

      // Store
      std::string error;
      if (!credential_store_->SaveCredential(slot, *decrypted, &error)) {
        qCWarning(diag::appLog) << "Failed to store credential for slot:"
                                     << slot.c_str() << "error:" << error.c_str();
        result.entries_skipped++;
        continue;
      }

      // If we have a profile controller, also recreate/update the profile
      if (profile_controller_) {
        // The profile might already exist or need to be created
        // This is handled separately by the caller through the profile controller
      }

      result.entries_processed++;
    }

    result.success = result.entries_processed > 0 || result.entries_skipped == 0;

    qCInfo(diag::appLog) << "Credential import completed:"
                              << result.entries_processed << "processed,"
                              << result.entries_skipped << "skipped";
  } catch (const std::exception& e) {
    result.error = std::string("Import failed: ") + e.what();
  }

  return result;
}

auto CredentialPortability::ValidateBundle(const std::filesystem::path& file_path)
    -> std::optional<int> {
  try {
    QFile file(QString::fromStdString(file_path.string()));
    if (!file.open(QIODevice::ReadOnly)) {
      return std::nullopt;
    }

    QJsonDocument doc = QJsonDocument::fromJson(file.readAll());
    if (!doc.isObject()) return std::nullopt;

    QJsonObject root = doc.object();
    QString schema = root.value("schema").toString();
    if (schema != QString::fromStdString(kCredentialBundleSchema)) {
      return std::nullopt;
    }

    return root.value("entries").toArray().size();
  } catch (...) {
    return std::nullopt;
  }
}

}  // namespace alcedo
