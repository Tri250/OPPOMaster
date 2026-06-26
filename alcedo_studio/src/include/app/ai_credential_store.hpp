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
// The plan's rule: `QSettings` may store only non-secret metadata (selected
// preset id, protocol family, endpoint, model id, masked key label, remember/
// delete preference). The raw API key itself lives ONLY in the OS credential
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
  virtual ~IAiCredentialStore() = default;

  // Persist `secret` under `slot`, replacing any existing entry. Returns false
  // (and sets `error`) if the OS store rejected the write. `error` must carry
  // no secret material.
  virtual auto SaveCredential(const std::string& slot, const std::string& secret,
                              std::string* error) -> bool = 0;
  // Read the secret stored under `slot` into `*secret`. Returns false (and sets
  // `error`) when no entry exists or the read failed; a missing entry is NOT an
  // error in the diagnostic sense but is reported as false with a benign
  // message so the caller can distinguish "no credential" from "store failure".
  virtual auto LoadCredential(const std::string& slot, std::string* secret,
                              std::string* error) -> bool = 0;
  // Remove the entry under `slot`. Returns true even if no entry existed
  // (idempotent delete); returns false only on a store failure.
  virtual auto DeleteCredential(const std::string& slot, std::string* error) -> bool = 0;
  // True when an entry exists under `slot`.
  virtual auto HasCredential(const std::string& slot) -> bool = 0;
};

// Windows Credential Manager (wincred) backed store. Entries are generic
// credentials named `AlcedoStudio/AiCredential/<slot>`, persisted per-user on
// the local machine. The secret is stored as raw UTF-8 bytes in the credential
// blob. This is the production store on Windows (the tested platform).
class WinCredAiCredentialStore final : public IAiCredentialStore {
 public:
  auto SaveCredential(const std::string& slot, const std::string& secret,
                      std::string* error) -> bool override;
  auto LoadCredential(const std::string& slot, std::string* secret,
                      std::string* error) -> bool override;
  auto DeleteCredential(const std::string& slot, std::string* error) -> bool override;
  auto HasCredential(const std::string& slot) -> bool override;
};

// In-memory store: used by unit tests (so they do not touch the real OS
// credential store) and as the non-Windows fallback. NOT for production use on
// a shipping build — secrets do not survive process exit. On macOS a native
// Keychain Services impl is deferred (Phase 6c ships Windows wincred + this
// fallback; see the plan's Phase 6c review notes).
class InMemoryAiCredentialStore final : public IAiCredentialStore {
 public:
  auto SaveCredential(const std::string& slot, const std::string& secret,
                      std::string* error) -> bool override;
  auto LoadCredential(const std::string& slot, std::string* secret,
                      std::string* error) -> bool override;
  auto DeleteCredential(const std::string& slot, std::string* error) -> bool override;
  auto HasCredential(const std::string& slot) -> bool override;

 private:
  // Mutable so `HasCredential`/`LoadCredential` can be const-correct on the
  // interface while the in-memory map mutates on cache population.
  std::unordered_map<std::string, std::string> entries_;
};

// Factory returning the platform-default production store. On Windows this is
// the wincred store; on other platforms it returns the in-memory fallback (a
// deferred-native-store placeholder) so the controller still compiles and tests
// run, with the documented caveat that secrets are not persisted off-Windows.
auto MakeDefaultAiCredentialStore() -> std::shared_ptr<IAiCredentialStore>;

}  // namespace alcedo
