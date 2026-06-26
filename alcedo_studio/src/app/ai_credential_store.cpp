//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/ai_credential_store.hpp"

#include <algorithm>
#include <string>
#include <vector>

#if defined(_WIN32)
#include <windows.h>
#include <wincred.h>
#endif

namespace alcedo {

namespace {

// All Alcedo AI credentials live under this prefix so they are easy to audit
// and do not collide with other applications in the OS store. The full target
// name is `AlcedoStudio/AiCredential/<slot>`.
constexpr const char* kTargetPrefix = "AlcedoStudio/AiCredential/";

auto TargetName(const std::string& slot) -> std::string {
  return std::string(kTargetPrefix) + slot;
}

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

auto WinCredAiCredentialStore::SaveCredential(const std::string& slot, const std::string& secret,
                                              std::string* error) -> bool {
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

auto WinCredAiCredentialStore::LoadCredential(const std::string& slot, std::string* secret,
                                              std::string* error) -> bool {
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

auto WinCredAiCredentialStore::DeleteCredential(const std::string& slot, std::string* error)
    -> bool {
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

auto MakeDefaultAiCredentialStore() -> std::shared_ptr<IAiCredentialStore> {
#if defined(_WIN32)
  return std::make_shared<WinCredAiCredentialStore>();
#else
  // Non-Windows: in-memory fallback. A native Keychain Services impl is
  // deferred (see Phase 6c review notes).
  return std::make_shared<InMemoryAiCredentialStore>();
#endif
}

}  // namespace alcedo
