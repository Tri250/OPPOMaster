//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/ai_credential_store.hpp"

#include <algorithm>
#include <chrono>
#include <cstdlib>
#include <filesystem>
#include <fstream>
#include <memory>
#include <numeric>
#include <string>
#include <vector>

#if defined(_WIN32)
#include <windows.h>
#include <wincred.h>
#include <wincrypt.h>
#elif defined(__APPLE__)
#include <CoreFoundation/CoreFoundation.h>
#include <Security/Security.h>
#elif defined(__linux__)
#include <unistd.h>
#include <sys/stat.h>
#endif

namespace alcedo {

namespace {

#if defined(_WIN32)
// All Alcedo AI credentials live under this prefix so they are easy to audit
// and do not collide with other applications in the OS store. The full target
// name is `AlcedoStudio/AiCredential/<slot>`.
constexpr const char* kTargetPrefix = "AlcedoStudio/AiCredential/";
constexpr const char* kFilePrefix   = "ALCEDO_DPAPI_FILE_CREDENTIAL_V1:";
constexpr const char* kSplitPrefix  = "ALCEDO_SPLIT_CREDENTIAL_V1:";
constexpr size_t      kWinCredInlineBytes = 2000;

auto TargetName(const std::string& slot) -> std::string {
  return std::string(kTargetPrefix) + slot;
}

auto FileManifest(const std::string& slot) -> std::string {
  return std::string(kFilePrefix) + slot + ".bin";
}

auto ParseFileManifest(const std::string& value, std::string* filename) -> bool {
  if (!value.starts_with(kFilePrefix)) {
    return false;
  }
  const std::string name = value.substr(std::char_traits<char>::length(kFilePrefix));
  if (name.empty() || name.find('/') != std::string::npos || name.find('\\') != std::string::npos ||
      name.find("..") != std::string::npos) {
    return false;
  }
  if (filename) *filename = name;
  return true;
}

auto ChunkSlot(const std::string& slot, size_t index) -> std::string {
  return slot + "_chunk_" + std::to_string(index);
}
#endif  // _WIN32

// Validate the slot is a safe label ([a-z0-9_]+) before it reaches the OS
// store, so a malformed slot can never inject a path separator into the target
// name. This mirrors the Rust provider_config `credential_slot` rule.
auto IsValidSlot(const std::string& slot) -> bool {
  if (slot.empty()) {
    return false;
  }
  return std::all_of(slot.begin(), slot.end(), [](char c) {
    return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_';
  });
}

}  // namespace

// ---------------- In-memory store ----------------

auto InMemoryAiCredentialStore::SaveCredential(const std::string& slot,
                                               const std::string& secret, std::string* error)
    -> bool {
  if (!IsValidSlot(slot)) {
    if (error) *error = "invalid credential slot";
    return false;
  }
  entries_[slot] = secret;
  return true;
}

auto InMemoryAiCredentialStore::LoadCredential(const std::string& slot, std::string* secret,
                                               std::string* error) -> bool {
  auto it = entries_.find(slot);
  if (it == entries_.end()) {
    if (error) *error = "no credential stored for slot";
    return false;
  }
  if (secret) *secret = it->second;
  return true;
}

auto InMemoryAiCredentialStore::DeleteCredential(const std::string& slot, std::string* /*error*/)
    -> bool {
  entries_.erase(slot);
  return true;
}

auto InMemoryAiCredentialStore::HasCredential(const std::string& slot) -> bool {
  return entries_.find(slot) != entries_.end();
}

// ---------------- Linux encrypted file store ----------------

#if defined(__linux__)

namespace {

// Simple FNV-1a hash for key stretching. Not cryptographic, but sufficient
// for obfuscating credentials at rest on a single-user Linux desktop where
// the threat model is casual file snooping, not targeted cryptanalysis.
auto Fnv1a64(const std::string& data) -> uint64_t {
  uint64_t hash = 14695981039346656037ULL;
  for (unsigned char c : data) {
    hash ^= static_cast<uint64_t>(c);
    hash *= 1099511628211ULL;
  }
  return hash;
}

// Derive a 32-byte key from machine-specific identifiers and a per-file salt.
// The salt is stored alongside the ciphertext so the same machine always
// produces the same key given the same salt.
auto DeriveMachineKey(const std::string& salt) -> std::vector<uint8_t> {
  // Collect machine-specific identity: hostname + username.
  char hostname_buf[256] = {};
  gethostname(hostname_buf, sizeof(hostname_buf) - 1);

  const char* username = getenv("USER");
  if (!username) {
    username = getenv("LOGNAME");
  }
  if (!username) {
    username = "unknown";
  }

  const std::string identity = std::string(hostname_buf) + ":" + username;

  // Stretch the identity + salt into 32 bytes using repeated FNV-1a rounds
  // with different seed offsets. This is not a KDF but provides key
  // diversification so that two different salts on the same machine produce
  // unrelated keys.
  std::vector<uint8_t> key(32, 0);
  for (int round = 0; round < 4; ++round) {
    std::string input = std::to_string(round) + ":" + identity + ":" + salt;
    uint64_t h = Fnv1a64(input);
    // Mix with a second pass using the reverse string.
    std::string rev_input(input.rbegin(), input.rend());
    uint64_t h2 = Fnv1a64(rev_input);
    // Combine and store 8 bytes per round.
    key[round * 8 + 0] = static_cast<uint8_t>(h >> 0);
    key[round * 8 + 1] = static_cast<uint8_t>(h >> 8);
    key[round * 8 + 2] = static_cast<uint8_t>(h >> 16);
    key[round * 8 + 3] = static_cast<uint8_t>(h >> 24);
    key[round * 8 + 4] = static_cast<uint8_t>(h >> 32);
    key[round * 8 + 5] = static_cast<uint8_t>(h >> 40);
    key[round * 8 + 6] = static_cast<uint8_t>(h2 >> 0);
    key[round * 8 + 7] = static_cast<uint8_t>(h2 >> 8);
  }
  return key;
}

// XOR stream cipher: generate a pseudo-random byte stream from the key and
// XOR it with the plaintext. The stream is produced by iterating FNV-1a
// over successive counter values, using the key bytes as a pre-shared seed.
auto XorStreamEncrypt(const std::string& plaintext, const std::vector<uint8_t>& key)
    -> std::vector<uint8_t> {
  std::vector<uint8_t> output(plaintext.size());
  const size_t key_len = key.size();

  // Generate stream bytes. Each block of 8 bytes comes from one FNV-1a hash
  // round seeded with (key_fragment + counter).
  uint64_t counter = 0;
  uint8_t stream_buf[8] = {};
  int stream_pos = 8;  // Force initial generation.

  for (size_t i = 0; i < plaintext.size(); ++i) {
    if (stream_pos >= 8) {
      // Mix key bytes around the counter to produce a stream word.
      std::string seed;
      seed.reserve(key_len + 8);
      seed.append(reinterpret_cast<const char*>(key.data()), key_len);
      const uint64_t c = counter++;
      for (int b = 0; b < 8; ++b) {
        seed.push_back(static_cast<char>(c >> (b * 8)));
      }
      uint64_t h = Fnv1a64(seed);
      for (int b = 0; b < 8; ++b) {
        stream_buf[b] = static_cast<uint8_t>(h >> (b * 8));
      }
      stream_pos = 0;
    }
    output[i] = static_cast<uint8_t>(plaintext[i]) ^ stream_buf[stream_pos++];
  }
  return output;
}

// Decrypt is identical to encrypt for XOR stream ciphers.
auto XorStreamDecrypt(const std::vector<uint8_t>& ciphertext, const std::vector<uint8_t>& key)
    -> std::string {
  std::string output(ciphertext.size(), '\0');
  const size_t key_len = key.size();

  uint64_t counter = 0;
  uint8_t stream_buf[8] = {};
  int stream_pos = 8;

  for (size_t i = 0; i < ciphertext.size(); ++i) {
    if (stream_pos >= 8) {
      std::string seed;
      seed.reserve(key_len + 8);
      seed.append(reinterpret_cast<const char*>(key.data()), key_len);
      const uint64_t c = counter++;
      for (int b = 0; b < 8; ++b) {
        seed.push_back(static_cast<char>(c >> (b * 8)));
      }
      uint64_t h = Fnv1a64(seed);
      for (int b = 0; b < 8; ++b) {
        stream_buf[b] = static_cast<uint8_t>(h >> (b * 8));
      }
      stream_pos = 0;
    }
    output[i] = static_cast<char>(ciphertext[i] ^ stream_buf[stream_pos++]);
  }
  return output;
}

// File format: [32-byte salt][4-byte payload_len (little-endian)][encrypted payload]
// The salt is random per write; payload_len guards against truncation.
constexpr size_t kSaltLen    = 32;
constexpr size_t kLenFieldLen = 4;

auto WriteLittleEndianU32(uint32_t val) -> std::array<uint8_t, kLenFieldLen> {
  return {static_cast<uint8_t>(val & 0xFF),
          static_cast<uint8_t>((val >> 8) & 0xFF),
          static_cast<uint8_t>((val >> 16) & 0xFF),
          static_cast<uint8_t>((val >> 24) & 0xFF)};
}

auto ReadLittleEndianU32(const uint8_t* buf) -> uint32_t {
  return static_cast<uint32_t>(buf[0]) |
         (static_cast<uint32_t>(buf[1]) << 8) |
         (static_cast<uint32_t>(buf[2]) << 16) |
         (static_cast<uint32_t>(buf[3]) << 24);
}

// Generate a 32-byte salt from the current time and address space layout
// randomization. Not truly random, but good enough for salting on a
// single-user desktop without depending on /dev/urandom at link time.
auto GenerateSalt() -> std::array<uint8_t, kSaltLen> {
  std::array<uint8_t, kSaltLen> salt{};
  // Mix in various sources of entropy.
  auto t = std::chrono::high_resolution_clock::now().time_since_epoch().count();
  uint64_t h = Fnv1a64(std::to_string(t));
  for (int i = 0; i < 8; ++i) {
    salt[i] = static_cast<uint8_t>(h >> (i * 8));
  }
  // Second round with stack address for ASLR diversity.
  volatile uintptr_t stack_var = reinterpret_cast<uintptr_t>(&salt);
  h = Fnv1a64(std::to_string(stack_var) + ":" + std::to_string(t + 1));
  for (int i = 0; i < 8; ++i) {
    salt[8 + i] = static_cast<uint8_t>(h >> (i * 8));
  }
  // Third and fourth rounds.
  h = Fnv1a64(std::to_string(t + 2) + ":" + std::to_string(stack_var + 1));
  for (int i = 0; i < 8; ++i) {
    salt[16 + i] = static_cast<uint8_t>(h >> (i * 8));
  }
  h = Fnv1a64(std::to_string(stack_var + 2) + ":" + std::to_string(t + 3));
  for (int i = 0; i < 8; ++i) {
    salt[24 + i] = static_cast<uint8_t>(h >> (i * 8));
  }
  return salt;
}

}  // namespace

auto LinuxEncryptedFileAiCredentialStore::DeriveKey(const std::string& salt)
    -> std::vector<uint8_t> {
  return DeriveMachineKey(salt);
}

auto LinuxEncryptedFileAiCredentialStore::Encrypt(
    const std::string& plaintext, const std::vector<uint8_t>& key) -> std::vector<uint8_t> {
  return XorStreamEncrypt(plaintext, key);
}

auto LinuxEncryptedFileAiCredentialStore::Decrypt(
    const std::vector<uint8_t>& ciphertext, const std::vector<uint8_t>& key) -> std::string {
  return XorStreamDecrypt(ciphertext, key);
}

auto LinuxEncryptedFileAiCredentialStore::GetCredentialPath(const std::string& slot)
    -> std::filesystem::path {
  return EnsureCredentialDir() / (slot + ".enc");
}

auto LinuxEncryptedFileAiCredentialStore::EnsureCredentialDir() -> std::filesystem::path {
  const char* xdg = getenv("XDG_CONFIG_HOME");
  std::filesystem::path config_dir;
  if (xdg && xdg[0] != '\0') {
    config_dir = std::filesystem::path(xdg);
  } else {
    const char* home = getenv("HOME");
    if (!home) {
      home = "/tmp";
    }
    config_dir = std::filesystem::path(home) / ".config";
  }
  auto cred_dir = config_dir / "AlcedoStudio" / "AiCredentialStore";

  std::error_code ec;
  std::filesystem::create_directories(cred_dir, ec);
  if (!ec) {
    // Set directory permissions to 0700 (owner only).
    std::filesystem::permissions(cred_dir,
                                  std::filesystem::perms::owner_read |
                                  std::filesystem::perms::owner_write |
                                  std::filesystem::perms::owner_exec,
                                  std::filesystem::perm_options::replace, ec);
  }
  return cred_dir;
}

auto LinuxEncryptedFileAiCredentialStore::SaveCredential(const std::string& slot,
                                                         const std::string& secret,
                                                         std::string* error) -> bool {
  if (!IsValidSlot(slot)) {
    if (error) *error = "invalid credential slot";
    return false;
  }

  // Generate a fresh salt for each save.
  const auto salt = GenerateSalt();
  const std::string salt_str(reinterpret_cast<const char*>(salt.data()), salt.size());

  const auto key = DeriveKey(salt_str);
  const auto encrypted = Encrypt(secret, key);

  // Build the file: [salt][payload_len LE][encrypted_payload].
  const auto len_bytes = WriteLittleEndianU32(static_cast<uint32_t>(encrypted.size()));

  const auto path = GetCredentialPath(slot);
  std::ofstream out(path, std::ios::binary | std::ios::trunc);
  if (!out) {
    if (error) *error = "could not open credential file for writing";
    return false;
  }

  out.write(reinterpret_cast<const char*>(salt.data()), kSaltLen);
  out.write(reinterpret_cast<const char*>(len_bytes.data()), kLenFieldLen);
  out.write(reinterpret_cast<const char*>(encrypted.data()),
            static_cast<std::streamsize>(encrypted.size()));

  if (!out) {
    if (error) *error = "could not write credential file";
    return false;
  }
  out.close();

  // Set file permissions to 0600 (owner read/write only).
  std::error_code ec;
  std::filesystem::permissions(path,
                                std::filesystem::perms::owner_read |
                                std::filesystem::perms::owner_write,
                                std::filesystem::perm_options::replace, ec);
  if (ec) {
    // Non-fatal: the data is already written, just with wider permissions.
    // Log a warning but don't fail the save.
  }

  return true;
}

auto LinuxEncryptedFileAiCredentialStore::LoadCredential(const std::string& slot,
                                                         std::string* secret,
                                                         std::string* error) -> bool {
  if (!IsValidSlot(slot)) {
    if (error) *error = "invalid credential slot";
    return false;
  }

  const auto path = GetCredentialPath(slot);
  std::ifstream in(path, std::ios::binary);
  if (!in) {
    if (error) *error = "no credential stored for slot";
    return false;
  }

  // Read salt.
  std::array<uint8_t, kSaltLen> salt{};
  if (!in.read(reinterpret_cast<char*>(salt.data()), kSaltLen)) {
    if (error) *error = "credential file is corrupt (missing salt)";
    return false;
  }

  // Read payload length.
  std::array<uint8_t, kLenFieldLen> len_bytes{};
  if (!in.read(reinterpret_cast<char*>(len_bytes.data()), kLenFieldLen)) {
    if (error) *error = "credential file is corrupt (missing length)";
    return false;
  }
  const uint32_t payload_len = ReadLittleEndianU32(len_bytes.data());

  // Sanity-check: a credential should not be absurdly large.
  if (payload_len > 1024 * 1024) {
    if (error) *error = "credential file is corrupt (implausible length)";
    return false;
  }

  // Read encrypted payload.
  std::vector<uint8_t> encrypted(payload_len);
  if (payload_len > 0 &&
      !in.read(reinterpret_cast<char*>(encrypted.data()),
               static_cast<std::streamsize>(payload_len))) {
    if (error) *error = "credential file is corrupt (truncated payload)";
    return false;
  }

  // Decrypt.
  const std::string salt_str(reinterpret_cast<const char*>(salt.data()), salt.size());
  const auto key = DeriveKey(salt_str);
  if (secret) {
    *secret = Decrypt(encrypted, key);
  }
  return true;
}

auto LinuxEncryptedFileAiCredentialStore::DeleteCredential(const std::string& slot,
                                                            std::string* error) -> bool {
  if (!IsValidSlot(slot)) {
    if (error) *error = "invalid credential slot";
    return false;
  }

  const auto path = GetCredentialPath(slot);
  std::error_code ec;
  // Idempotent: removing a non-existent file is success.
  std::filesystem::remove(path, ec);
  if (ec && std::filesystem::exists(path)) {
    if (error) *error = "could not delete credential file: " + ec.message();
    return false;
  }
  return true;
}

auto LinuxEncryptedFileAiCredentialStore::HasCredential(const std::string& slot) -> bool {
  if (!IsValidSlot(slot)) {
    return false;
  }
  return std::filesystem::exists(GetCredentialPath(slot));
}

#endif  // __linux__

// ---------------- Windows wincred store ----------------

#if defined(_WIN32)

// Converts a UTF-8 string to a wide string for wincred target/user names.
auto ToWide(const std::string& utf8) -> std::wstring {
  if (utf8.empty()) {
    return std::wstring();
  }
  int len = MultiByteToWideChar(CP_UTF8, 0, utf8.c_str(),
                                static_cast<int>(utf8.size()), nullptr, 0);
  std::wstring out(static_cast<size_t>(len), L'\0');
  MultiByteToWideChar(CP_UTF8, 0, utf8.c_str(), static_cast<int>(utf8.size()), out.data(), len);
  return out;
}

auto FromWide(const wchar_t* wide, int wide_len) -> std::string {
  if (wide == nullptr || wide_len <= 0) {
    return std::string();
  }
  int len = WideCharToMultiByte(CP_UTF8, 0, wide, wide_len, nullptr, 0, nullptr, nullptr);
  std::string out(static_cast<size_t>(len), '\0');
  WideCharToMultiByte(CP_UTF8, 0, wide, wide_len, out.data(), len, nullptr, nullptr);
  return out;
}

auto WriteWinCredentialBlob(const std::string& slot, const std::string& secret, std::string* error)
    -> bool {
  if (!IsValidSlot(slot)) {
    if (error) *error = "invalid credential slot";
    return false;
  }
  const std::wstring target = ToWide(TargetName(slot));
  const std::wstring user   = ToWide(slot);

  CREDENTIALW cred{};
  cred.Type     = CRED_TYPE_GENERIC;
  cred.TargetName = const_cast<LPWSTR>(target.c_str());
  cred.UserName   = const_cast<LPWSTR>(user.c_str());
  cred.Persist    = CRED_PERSIST_LOCAL_MACHINE;
  // Store the UTF-8 secret as raw bytes (CredentialBlob is a binary blob; the
  // size is in BYTES, not characters).
  cred.CredentialBlobSize = static_cast<DWORD>(secret.size());
  cred.CredentialBlob = reinterpret_cast<LPBYTE>(const_cast<char*>(secret.data()));

  if (!CredWriteW(&cred, 0)) {
    if (error) *error = "CredWriteW failed (code " + std::to_string(GetLastError()) + ")";
    return false;
  }
  return true;
}

auto ReadWinCredentialBlob(const std::string& slot, std::string* secret, std::string* error)
    -> bool {
  if (!IsValidSlot(slot)) {
    if (error) *error = "invalid credential slot";
    return false;
  }
  const std::wstring target = ToWide(TargetName(slot));
  PCREDENTIALW raw = nullptr;
  if (!CredReadW(target.c_str(), CRED_TYPE_GENERIC, 0, &raw)) {
    DWORD code = GetLastError();
    if (code == ERROR_NOT_FOUND) {
      if (error) *error = "no credential stored for slot";
    } else if (error) {
      *error = "CredReadW failed (code " + std::to_string(code) + ")";
    }
    return false;
  }
  bool ok = false;
  if (raw != nullptr) {
    if (secret != nullptr && raw->CredentialBlob != nullptr && raw->CredentialBlobSize > 0) {
      secret->assign(reinterpret_cast<const char*>(raw->CredentialBlob),
                     raw->CredentialBlobSize);
      ok = true;
    } else if (secret != nullptr) {
      // An entry exists but holds an empty blob — treat as present-but-empty.
      secret->clear();
      ok = true;
    }
    CredFree(raw);
  }
  if (!ok && error) {
    *error = "credential entry was malformed";
  }
  return ok;
}

auto DeleteWinCredentialBlob(const std::string& slot, std::string* error) -> bool {
  if (!IsValidSlot(slot)) {
    if (error) *error = "invalid credential slot";
    return false;
  }
  const std::wstring target = ToWide(TargetName(slot));
  // CredDeleteW returns ERROR_NOT_FOUND when the entry does not exist; treat
  // that as success (idempotent delete).
  if (!CredDeleteW(target.c_str(), CRED_TYPE_GENERIC, 0)) {
    DWORD code = GetLastError();
    if (code != ERROR_NOT_FOUND) {
      if (error) *error = "CredDeleteW failed (code " + std::to_string(code) + ")";
      return false;
    }
  }
  return true;
}

auto LocalAppDataDir() -> std::string {
  char*  value = nullptr;
  size_t len   = 0;
  if (_dupenv_s(&value, &len, "LOCALAPPDATA") != 0 || value == nullptr) {
    return {};
  }
  std::string out = value;
  std::free(value);
  return out;
}

auto CredentialFileRoot() -> std::filesystem::path {
  const std::string local_app_data = LocalAppDataDir();
  if (!local_app_data.empty()) {
    return std::filesystem::path(local_app_data) / "AlcedoStudio" / "AiCredentialStore";
  }
  return std::filesystem::temp_directory_path() / "AlcedoStudio" / "AiCredentialStore";
}

auto CredentialFilePath(const std::string& filename) -> std::filesystem::path {
  return CredentialFileRoot() / filename;
}

auto ProtectSecret(const std::string& secret, std::vector<unsigned char>* encrypted,
                   std::string* error) -> bool {
  DATA_BLOB input{};
  input.pbData = reinterpret_cast<BYTE*>(const_cast<char*>(secret.data()));
  input.cbData = static_cast<DWORD>(secret.size());

  DATA_BLOB output{};
  if (!CryptProtectData(&input, L"Alcedo Studio AI credential", nullptr, nullptr, nullptr,
                        CRYPTPROTECT_UI_FORBIDDEN, &output)) {
    if (error) *error = "CryptProtectData failed (code " + std::to_string(GetLastError()) + ")";
    return false;
  }
  encrypted->assign(output.pbData, output.pbData + output.cbData);
  LocalFree(output.pbData);
  return true;
}

auto UnprotectSecret(const std::vector<unsigned char>& encrypted, std::string* secret,
                     std::string* error) -> bool {
  DATA_BLOB input{};
  input.pbData = const_cast<BYTE*>(encrypted.data());
  input.cbData = static_cast<DWORD>(encrypted.size());

  DATA_BLOB output{};
  if (!CryptUnprotectData(&input, nullptr, nullptr, nullptr, nullptr, CRYPTPROTECT_UI_FORBIDDEN,
                          &output)) {
    if (error) *error = "CryptUnprotectData failed (code " + std::to_string(GetLastError()) + ")";
    return false;
  }
  if (secret) {
    secret->assign(reinterpret_cast<const char*>(output.pbData), output.cbData);
  }
  LocalFree(output.pbData);
  return true;
}

auto WriteEncryptedCredentialFile(const std::string& filename, const std::string& secret,
                                  std::string* error) -> bool {
  std::vector<unsigned char> encrypted;
  if (!ProtectSecret(secret, &encrypted, error)) {
    return false;
  }

  const auto path = CredentialFilePath(filename);
  std::error_code ec;
  std::filesystem::create_directories(path.parent_path(), ec);
  if (ec) {
    if (error) *error = "could not create credential directory: " + ec.message();
    return false;
  }

  std::ofstream out(path, std::ios::binary | std::ios::trunc);
  if (!out) {
    if (error) *error = "could not open credential file for writing";
    return false;
  }
  out.write(reinterpret_cast<const char*>(encrypted.data()),
            static_cast<std::streamsize>(encrypted.size()));
  if (!out) {
    if (error) *error = "could not write credential file";
    return false;
  }
  return true;
}

auto ReadEncryptedCredentialFile(const std::string& filename, std::string* secret,
                                 std::string* error) -> bool {
  const auto path = CredentialFilePath(filename);
  std::ifstream in(path, std::ios::binary);
  if (!in) {
    if (error) *error = "credential file is missing";
    return false;
  }
  std::vector<unsigned char> encrypted((std::istreambuf_iterator<char>(in)),
                                       std::istreambuf_iterator<char>());
  if (encrypted.empty()) {
    if (error) *error = "credential file is empty";
    return false;
  }
  return UnprotectSecret(encrypted, secret, error);
}

auto DeleteCredentialFile(const std::string& filename, std::string* error) -> bool {
  std::error_code ec;
  std::filesystem::remove(CredentialFilePath(filename), ec);
  if (ec) {
    if (error) *error = "could not delete credential file: " + ec.message();
    return false;
  }
  return true;
}

auto ParseSplitManifest(const std::string& value, size_t* count, size_t* total_bytes) -> bool {
  if (!value.starts_with(kSplitPrefix)) {
    return false;
  }
  const std::string rest = value.substr(std::char_traits<char>::length(kSplitPrefix));
  const size_t      sep  = rest.find(':');
  if (sep == std::string::npos || sep == 0 || sep + 1 >= rest.size()) {
    return false;
  }
  const std::string count_text = rest.substr(0, sep);
  const std::string total_text = rest.substr(sep + 1);
  char*       count_end = nullptr;
  char*       total_end = nullptr;
  const auto  parsed_count = std::strtoull(count_text.c_str(), &count_end, 10);
  const auto  parsed_total = std::strtoull(total_text.c_str(), &total_end, 10);
  const bool count_ok = count_end != nullptr && *count_end == '\0' && parsed_count > 0;
  const bool total_ok = total_end != nullptr && *total_end == '\0';
  if (!count_ok || !total_ok) {
    return false;
  }
  if (count) *count = static_cast<size_t>(parsed_count);
  if (total_bytes) *total_bytes = static_cast<size_t>(parsed_total);
  return true;
}

auto DeleteWinCredentialChunks(const std::string& slot, size_t count, std::string* error) -> bool {
  for (size_t i = 0; i < count; ++i) {
    if (!DeleteWinCredentialBlob(ChunkSlot(slot, i), error)) {
      return false;
    }
  }
  return true;
}

auto WinCredAiCredentialStore::SaveCredential(const std::string& slot, const std::string& secret,
                                              std::string* error) -> bool {
  if (!IsValidSlot(slot)) {
    if (error) *error = "invalid credential slot";
    return false;
  }

  // WinCred generic credentials have a small blob limit. Keep small secrets in
  // WinCred; store large token bundles as a DPAPI-encrypted local secret and put
  // only a tiny manifest in WinCred.
  if (secret.size() <= kWinCredInlineBytes) {
    std::string ignored;
    DeleteCredential(slot, &ignored);
    return WriteWinCredentialBlob(slot, secret, error);
  }

  std::string ignored;
  DeleteCredential(slot, &ignored);
  const std::string filename = slot + ".bin";
  if (!WriteEncryptedCredentialFile(filename, secret, error)) {
    return false;
  }
  if (!WriteWinCredentialBlob(slot, FileManifest(slot), error)) {
    DeleteCredentialFile(filename, &ignored);
    return false;
  }
  return true;
}

auto WinCredAiCredentialStore::LoadCredential(const std::string& slot, std::string* secret,
                                              std::string* error) -> bool {
  std::string main;
  if (!ReadWinCredentialBlob(slot, &main, error)) {
    return false;
  }
  size_t count       = 0;
  size_t total_bytes = 0;
  if (!ParseSplitManifest(main, &count, &total_bytes)) {
    std::string filename;
    if (ParseFileManifest(main, &filename)) {
      return ReadEncryptedCredentialFile(filename, secret, error);
    }
    if (secret) *secret = std::move(main);
    return true;
  }

  std::string joined;
  joined.reserve(total_bytes);
  for (size_t i = 0; i < count; ++i) {
    std::string chunk;
    if (!ReadWinCredentialBlob(ChunkSlot(slot, i), &chunk, error)) {
      if (error && error->empty()) *error = "credential chunk is missing";
      return false;
    }
    joined += chunk;
  }
  if (joined.size() != total_bytes) {
    if (error) *error = "credential chunks were malformed";
    return false;
  }
  if (secret) *secret = std::move(joined);
  return true;
}

auto WinCredAiCredentialStore::DeleteCredential(const std::string& slot, std::string* error)
    -> bool {
  if (!IsValidSlot(slot)) {
    if (error) *error = "invalid credential slot";
    return false;
  }

  std::string main;
  std::string read_error;
  if (ReadWinCredentialBlob(slot, &main, &read_error)) {
    size_t count       = 0;
    size_t total_bytes = 0;
    std::string filename;
    if (ParseFileManifest(main, &filename) && !DeleteCredentialFile(filename, error)) {
      return false;
    }
    if (ParseSplitManifest(main, &count, &total_bytes) &&
        !DeleteWinCredentialChunks(slot, count, error)) {
      return false;
    }
  }
  return DeleteWinCredentialBlob(slot, error);
}

auto WinCredAiCredentialStore::HasCredential(const std::string& slot) -> bool {
  if (!IsValidSlot(slot)) {
    return false;
  }
  const std::wstring target = ToWide(TargetName(slot));
  PCREDENTIALW raw = nullptr;
  if (!CredReadW(target.c_str(), CRED_TYPE_GENERIC, 0, &raw)) {
    return false;
  }
  CredFree(raw);
  return true;
}

#endif  // _WIN32

// ---------------- macOS Keychain Services store ----------------

#if defined(__APPLE__)

namespace {

// All Alcedo AI credentials on macOS live under this service label so they are
// easy to audit and do not collide with other applications in the keychain. The
// item is keyed by (service = kMacService, account = <slot>), matching the
// Windows `AlcedoStudio/AiCredential/<slot>` target naming.
constexpr const char* kMacService = "AlcedoStudio.AiCredential";

// RAII wrapper for a retained CoreFoundation object. `CFRef` is the CF type
// (e.g. CFStringRef); the deleter calls CFRelease on destruction. Uses the
// `using pointer = CFRef;` idiom so unique_ptr stores the CF ref directly.
template <typename CFRef>
struct CfDeleter {
  using pointer = CFRef;
  auto operator()(pointer p) const noexcept -> void {
    if (p) {
      CFRelease(p);
    }
  }
};
template <typename CFRef>
using CfPtr = std::unique_ptr<void, CfDeleter<CFRef>>;

auto MakeCfString(const std::string& utf8) -> CfPtr<CFStringRef> {
  // slot is validated to [a-z0-9_]+ (pure ASCII) and kMacService is a literal,
  // so UTF-8 conversion cannot fail here.
  return CfPtr<CFStringRef>(
      CFStringCreateWithCString(kCFAllocatorDefault, utf8.c_str(), kCFStringEncodingUTF8));
}

// Copies the bytes into a new CFData. Secrets are small (API keys), so copying
// is preferred over a no-copy binding that would couple the CFData lifetime to
// the caller's std::string. Handles empty secrets (length 0) correctly.
auto MakeCfData(const std::string& bytes) -> CfPtr<CFDataRef> {
  return CfPtr<CFDataRef>(CFDataCreate(kCFAllocatorDefault,
                                       reinterpret_cast<const UInt8*>(bytes.data()),
                                       static_cast<CFIndex>(bytes.size())));
}

auto MakeMutableDict() -> CfPtr<CFMutableDictionaryRef> {
  return CfPtr<CFMutableDictionaryRef>(CFDictionaryCreateMutable(
      kCFAllocatorDefault, 0, &kCFTypeDictionaryKeyCallBacks, &kCFTypeDictionaryValueCallBacks));
}

// Update the kSecValueData of the (service, account) generic-password item.
// Returns errSecItemNotFound when no item exists yet (caller falls through to
// SecItemAdd), errSecSuccess on update, or another OSStatus on failure.
auto UpdateItemData(CFStringRef service, CFStringRef account, CFDataRef data, bool use_dp)
    -> OSStatus {
  auto query = MakeMutableDict();
  CFDictionaryAddValue(query.get(), kSecClass, kSecClassGenericPassword);
  CFDictionaryAddValue(query.get(), kSecAttrService, service);
  CFDictionaryAddValue(query.get(), kSecAttrAccount, account);
  if (use_dp) {
    CFDictionaryAddValue(query.get(), kSecUseDataProtectionKeychain, kCFBooleanTrue);
  }
  auto update = MakeMutableDict();
  CFDictionaryAddValue(update.get(), kSecValueData, data);
  return SecItemUpdate(query.get(), update.get());
}

}  // namespace

MacKeychainAiCredentialStore::MacKeychainAiCredentialStore() {
  // Probe whether the Data Protection keychain accepts writes here. On
  // unsigned/dev builds without a keychain-access-groups entitlement, DP writes
  // fail with errSecMissingEntitlement (-34018); a mere *search* returns
  // errSecItemNotFound instead, so only a real write can detect the problem. We
  // add a throwaway item under a dedicated probe service (so it can never
  // collide with real credentials) and delete it immediately on success. On
  // -34018 (or any other failure) we fall back to the legacy file-based login
  // keychain, which needs no entitlement. Resolved once at construction
  // (single-threaded) so later reads of the flag are race-free.
  constexpr const char* kProbeService = "AlcedoStudio.DpProbe";
  CfPtr<CFStringRef>    service       = MakeCfString(kProbeService);
  CfPtr<CFStringRef>    account       = MakeCfString("probe");
  CfPtr<CFDataRef>      data          = MakeCfData(std::string{});

  auto                  add           = MakeMutableDict();
  CFDictionaryAddValue(add.get(), kSecClass, kSecClassGenericPassword);
  CFDictionaryAddValue(add.get(), kSecAttrService, service.get());
  CFDictionaryAddValue(add.get(), kSecAttrAccount, account.get());
  CFDictionaryAddValue(add.get(), kSecValueData, data.get());
  CFDictionaryAddValue(add.get(), kSecUseDataProtectionKeychain, kCFBooleanTrue);
  CFDictionaryAddValue(add.get(), kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlock);

  OSStatus status               = SecItemAdd(add.get(), nullptr);
  use_data_protection_keychain_ = (status == errSecSuccess || status == errSecDuplicateItem);
  if (use_data_protection_keychain_) {
    // DP is usable. Remove the probe item — this cleans up both the item just
    // added and any leftover from a prior probe that crashed before deleting.
    auto del = MakeMutableDict();
    CFDictionaryAddValue(del.get(), kSecClass, kSecClassGenericPassword);
    CFDictionaryAddValue(del.get(), kSecAttrService, service.get());
    CFDictionaryAddValue(del.get(), kSecAttrAccount, account.get());
    CFDictionaryAddValue(del.get(), kSecUseDataProtectionKeychain, kCFBooleanTrue);
    SecItemDelete(del.get());
  }
}

auto MacKeychainAiCredentialStore::SaveCredential(const std::string& slot,
                                                  const std::string& secret, std::string* error)
    -> bool {
  if (!IsValidSlot(slot)) {
    if (error) *error = "invalid credential slot";
    return false;
  }
  const bool         use_dp  = use_data_protection_keychain_;
  CfPtr<CFStringRef> service = MakeCfString(kMacService);
  CfPtr<CFStringRef> account = MakeCfString(slot);
  CfPtr<CFDataRef>   data    = MakeCfData(secret);

  // Upsert: update an existing item's data, or add a new one if none exists.
  OSStatus           status  = UpdateItemData(service.get(), account.get(), data.get(), use_dp);
  if (status == errSecSuccess) {
    return true;
  }
  if (status != errSecItemNotFound) {
    if (error)
      *error = "SecItemUpdate failed (code " + std::to_string(static_cast<int>(status)) + ")";
    return false;
  }

  auto add = MakeMutableDict();
  CFDictionaryAddValue(add.get(), kSecClass, kSecClassGenericPassword);
  CFDictionaryAddValue(add.get(), kSecAttrService, service.get());
  CFDictionaryAddValue(add.get(), kSecAttrAccount, account.get());
  CFDictionaryAddValue(add.get(), kSecValueData, data.get());
  if (use_dp) {
    CFDictionaryAddValue(add.get(), kSecUseDataProtectionKeychain, kCFBooleanTrue);
    // AfterFirstUnlock: accessible from first login until reboot, so reads do
    // not require the session to be re-unlocked. No kSecAttrAccessControl, so
    // no biometric/passcode prompt per access.
    CFDictionaryAddValue(add.get(), kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlock);
  }

  status = SecItemAdd(add.get(), nullptr);
  if (status == errSecDuplicateItem) {
    // Race: item appeared between the update and the add. Retry the update.
    status = UpdateItemData(service.get(), account.get(), data.get(), use_dp);
  }
  if (status != errSecSuccess) {
    if (error) *error = "SecItemAdd failed (code " + std::to_string(static_cast<int>(status)) + ")";
    return false;
  }
  return true;
}

auto MacKeychainAiCredentialStore::LoadCredential(const std::string& slot, std::string* secret,
                                                  std::string* error) -> bool {
  if (!IsValidSlot(slot)) {
    if (error) *error = "invalid credential slot";
    return false;
  }
  CfPtr<CFStringRef> service = MakeCfString(kMacService);
  CfPtr<CFStringRef> account = MakeCfString(slot);
  auto               query   = MakeMutableDict();
  CFDictionaryAddValue(query.get(), kSecClass, kSecClassGenericPassword);
  CFDictionaryAddValue(query.get(), kSecAttrService, service.get());
  CFDictionaryAddValue(query.get(), kSecAttrAccount, account.get());
  CFDictionaryAddValue(query.get(), kSecReturnData, kCFBooleanTrue);
  CFDictionaryAddValue(query.get(), kSecMatchLimit, kSecMatchLimitOne);
  if (use_data_protection_keychain_) {
    CFDictionaryAddValue(query.get(), kSecUseDataProtectionKeychain, kCFBooleanTrue);
  }

  CFTypeRef out    = nullptr;
  OSStatus  status = SecItemCopyMatching(query.get(), &out);
  if (status == errSecItemNotFound) {
    if (error) *error = "no credential stored for slot";
    return false;
  }
  if (status != errSecSuccess) {
    if (error)
      *error = "SecItemCopyMatching failed (code " + std::to_string(static_cast<int>(status)) + ")";
    return false;
  }
  bool ok = false;
  if (out != nullptr) {
    if (CFGetTypeID(out) == CFDataGetTypeID()) {
      CFDataRef data = reinterpret_cast<CFDataRef>(out);
      if (secret != nullptr) {
        secret->assign(reinterpret_cast<const char*>(CFDataGetBytePtr(data)),
                       static_cast<size_t>(CFDataGetLength(data)));
      }
      ok = true;
    }
    CFRelease(out);
  }
  if (!ok && error) {
    *error = "credential entry was malformed";
  }
  return ok;
}

auto MacKeychainAiCredentialStore::DeleteCredential(const std::string& slot, std::string* error)
    -> bool {
  if (!IsValidSlot(slot)) {
    if (error) *error = "invalid credential slot";
    return false;
  }
  CfPtr<CFStringRef> service = MakeCfString(kMacService);
  CfPtr<CFStringRef> account = MakeCfString(slot);
  auto               query   = MakeMutableDict();
  CFDictionaryAddValue(query.get(), kSecClass, kSecClassGenericPassword);
  CFDictionaryAddValue(query.get(), kSecAttrService, service.get());
  CFDictionaryAddValue(query.get(), kSecAttrAccount, account.get());
  if (use_data_protection_keychain_) {
    CFDictionaryAddValue(query.get(), kSecUseDataProtectionKeychain, kCFBooleanTrue);
  }
  OSStatus status = SecItemDelete(query.get());
  // errSecItemNotFound means nothing to delete — treat as success (idempotent),
  // mirroring the Windows CredDeleteW behavior.
  if (status == errSecItemNotFound) {
    return true;
  }
  if (status != errSecSuccess) {
    if (error)
      *error = "SecItemDelete failed (code " + std::to_string(static_cast<int>(status)) + ")";
    return false;
  }
  return true;
}

auto MacKeychainAiCredentialStore::HasCredential(const std::string& slot) -> bool {
  if (!IsValidSlot(slot)) {
    return false;
  }
  CfPtr<CFStringRef> service = MakeCfString(kMacService);
  CfPtr<CFStringRef> account = MakeCfString(slot);
  auto               query   = MakeMutableDict();
  CFDictionaryAddValue(query.get(), kSecClass, kSecClassGenericPassword);
  CFDictionaryAddValue(query.get(), kSecAttrService, service.get());
  CFDictionaryAddValue(query.get(), kSecAttrAccount, account.get());
  // Return attributes (not the secret) for a lightweight existence check that
  // works with the Data Protection keychain (kSecReturnRef is unsupported for
  // generic-password items there).
  CFDictionaryAddValue(query.get(), kSecReturnAttributes, kCFBooleanTrue);
  CFDictionaryAddValue(query.get(), kSecMatchLimit, kSecMatchLimitOne);
  if (use_data_protection_keychain_) {
    CFDictionaryAddValue(query.get(), kSecUseDataProtectionKeychain, kCFBooleanTrue);
  }
  CFTypeRef out    = nullptr;
  OSStatus  status = SecItemCopyMatching(query.get(), &out);
  if (out != nullptr) {
    CFRelease(out);
  }
  return status == errSecSuccess;
}

#endif  // __APPLE__

auto MakeDefaultAiCredentialStore() -> std::shared_ptr<IAiCredentialStore> {
#if defined(_WIN32)
  return std::make_shared<WinCredAiCredentialStore>();
#elif defined(__APPLE__)
  return std::make_shared<MacKeychainAiCredentialStore>();
#else
  // Linux: encrypted file-based credential store with machine-bound key
  // derivation. Secrets are persisted to disk with XOR stream encryption;
  // the encryption key is derived from hostname + username so that the files
  // are not portable to another account or machine.
  return std::make_shared<LinuxEncryptedFileAiCredentialStore>();
#endif
}

}  // namespace alcedo
