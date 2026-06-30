//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <memory>
#include <string>
#include <unordered_map>

namespace alcedo {

// Phase 6c: host-side OS credential store for long-lived provider API keys.
//
// The plan's rule: `ai_providers.json` may store only non-secret metadata
// (profile id, provider config fields, model id, masked key label, delete
// preference). The raw API key itself lives ONLY in the OS credential
// store and reaches the sidecar exclusively as a vault handle via
// `sidecar_client::CredentialClient::RegisterCredential` — never through QSettings,
// `AiSidecarRuntimeOptions`, process launch args, or logs.
//
// `slot` is the provider config's `credential_slot` label (e.g.
// `opencode_api_key`), validated to `[a-z0-9_]+` upstream. The store namespaces
// entries under an Alcedo-owned prefix so they do not collide with other
// applications. Implementations must treat the secret as sensitive: never log
// it, never echo it through `error`.
class IAiCredentialStore {
 public:
  virtual ~IAiCredentialStore()                           = default;

  // Persist `secret` under `slot`, replacing any existing entry. Returns false
  // (and sets `error`) if the OS store rejected the write. `error` must carry
  // no secret material.
  virtual auto SaveCredential(const std::string& slot, const std::string& secret,
                              std::string* error) -> bool = 0;
  // Read the secret stored under `slot` into `*secret`. Returns false (and sets
  // `error`) when no entry exists or the read failed; a missing entry is NOT an
  // error in the diagnostic sense but is reported as false with a benign
  // message so the caller can distinguish "no credential" from "store failure".
  virtual auto LoadCredential(const std::string& slot, std::string* secret, std::string* error)
      -> bool                                                                        = 0;
  // Remove the entry under `slot`. Returns true even if no entry existed
  // (idempotent delete); returns false only on a store failure.
  virtual auto DeleteCredential(const std::string& slot, std::string* error) -> bool = 0;
  // True when an entry exists under `slot`.
  virtual auto HasCredential(const std::string& slot) -> bool                        = 0;
};

// Windows Credential Manager (wincred) backed store. Entries are generic
// credentials named `AlcedoStudio/AiCredential/<slot>`, persisted per-user on
// the local machine. The secret is stored as raw UTF-8 bytes in the credential
// blob. This is the production store on Windows (the tested platform).
class WinCredAiCredentialStore final : public IAiCredentialStore {
 public:
  auto SaveCredential(const std::string& slot, const std::string& secret, std::string* error)
      -> bool override;
  auto LoadCredential(const std::string& slot, std::string* secret, std::string* error)
      -> bool override;
  auto DeleteCredential(const std::string& slot, std::string* error) -> bool override;
  auto HasCredential(const std::string& slot) -> bool override;
};

// macOS Keychain Services backed store. Entries are generic-password items in
// the user's keychain with service `AlcedoStudio.AiCredential` and account =
// <slot>; the secret is stored as raw bytes in kSecValueData. Uses the modern
// SecItem API against the Data Protection keychain
// (kSecUseDataProtectionKeychain + kSecAttrAccessibleAfterFirstUnlock), which
// gates access by app code-signature and unlock state rather than per-access
// password/biometric prompts — the right trade-off for low-sensitivity,
// frequently-read provider API keys, where per-access prompts would hurt
// usability. No kSecAttrAccessControl is set, so no biometric/passcode is
// required. On unsigned/dev builds where the Data Protection keychain is
// unavailable (errSecMissingEntitlement, -34018), the store transparently falls
// back to the legacy file-based login keychain (whose default ACL trusts the
// creating app, also no per-access prompt). This is the production store on
// macOS.
class MacKeychainAiCredentialStore final : public IAiCredentialStore {
 public:
  MacKeychainAiCredentialStore();

  auto SaveCredential(const std::string& slot, const std::string& secret, std::string* error)
      -> bool override;
  auto LoadCredential(const std::string& slot, std::string* secret, std::string* error)
      -> bool override;
  auto DeleteCredential(const std::string& slot, std::string* error) -> bool override;
  auto HasCredential(const std::string& slot) -> bool override;

 private:
  // Resolved once in the constructor. When true, SecItem queries carry
  // kSecUseDataProtectionKeychain = true; when false, they target the legacy
  // file-based default (login) keychain. Set before any concurrent access
  // (construction is single-threaded), so reads need no synchronization.
  bool use_data_protection_keychain_ = false;
};

// In-memory store: used by unit tests (so they do not touch the real OS
// credential store) and as the fallback on platforms without a native impl
// (e.g. Linux). NOT for production use on a shipping build — secrets do not
// survive process exit. Windows uses WinCredAiCredentialStore and macOS uses
// MacKeychainAiCredentialStore.
class InMemoryAiCredentialStore final : public IAiCredentialStore {
 public:
  auto SaveCredential(const std::string& slot, const std::string& secret, std::string* error)
      -> bool override;
  auto LoadCredential(const std::string& slot, std::string* secret, std::string* error)
      -> bool override;
  auto DeleteCredential(const std::string& slot, std::string* error) -> bool override;
  auto HasCredential(const std::string& slot) -> bool override;

 private:
  // Mutable so `HasCredential`/`LoadCredential` can be const-correct on the
  // interface while the in-memory map mutates on cache population.
  std::unordered_map<std::string, std::string> entries_;
};

// Factory returning the platform-default production store: WinCred on Windows,
// MacKeychain on macOS, and the in-memory fallback elsewhere (e.g. Linux, where
// a native impl is not yet provided). The in-memory fallback does not persist
// secrets across process exit.
auto MakeDefaultAiCredentialStore() -> std::shared_ptr<IAiCredentialStore>;

}  // namespace alcedo
