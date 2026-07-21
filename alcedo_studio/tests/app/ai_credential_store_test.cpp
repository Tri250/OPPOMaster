//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/ai_credential_store.hpp"

#include <gtest/gtest.h>

#include <memory>
#include <string>

#if defined(_WIN32)
#include <windows.h>
#endif

namespace alcedo {
namespace {

TEST(AiCredentialStoreTest, InMemoryStoreSavesLoadsAndDeletesCredential) {
  InMemoryAiCredentialStore store;
  const std::string         secret = "sk-DO-NOT-LEAK-CREDENTIAL-STORE-7c3f";
  std::string               error;

  ASSERT_TRUE(store.SaveCredential("opencode_api_key", secret, &error)) << error;
  EXPECT_TRUE(store.HasCredential("opencode_api_key"));

  std::string loaded;
  ASSERT_TRUE(store.LoadCredential("opencode_api_key", &loaded, &error)) << error;
  EXPECT_EQ(loaded, secret);

  EXPECT_TRUE(store.DeleteCredential("opencode_api_key", &error)) << error;
  EXPECT_FALSE(store.HasCredential("opencode_api_key"));
  loaded.clear();
  EXPECT_FALSE(store.LoadCredential("opencode_api_key", &loaded, &error));
  EXPECT_EQ(error.find(secret), std::string::npos);
}

TEST(AiCredentialStoreTest, InvalidSlotIsRejectedWithoutLeakingSecret) {
  InMemoryAiCredentialStore store;
  const std::string         secret = "sk-DO-NOT-LEAK-BAD-SLOT-8a21";
  std::string               error;

  EXPECT_FALSE(store.SaveCredential("../bad", secret, &error));
  EXPECT_FALSE(error.empty());
  EXPECT_EQ(error.find(secret), std::string::npos);
  EXPECT_FALSE(store.HasCredential("../bad"));
}

TEST(AiCredentialStoreTest, DeleteIsIdempotent) {
  InMemoryAiCredentialStore store;
  std::string               error;

  EXPECT_TRUE(store.DeleteCredential("missing_slot", &error));
  EXPECT_TRUE(error.empty());
}

TEST(AiCredentialStoreTest, FactoryReturnsUsableStore) {
  std::shared_ptr<IAiCredentialStore> store = MakeDefaultAiCredentialStore();
  ASSERT_TRUE(store);
}

#if defined(_WIN32)
TEST(AiCredentialStoreTest, WinCredStoreSavesLoadsAndDeletesLargeCredentialViaEncryptedFile) {
  WinCredAiCredentialStore store;
  const std::string        slot =
      "alcedo_test_large_secret_" + std::to_string(GetCurrentProcessId());
  const std::string        secret =
      "{\"tokens\":{\"access_token\":\"" + std::string(3600, 'a') +
      "\",\"refresh_token\":\"" + std::string(1200, 'r') +
      "\",\"account_id\":\"acct_test\"}}";
  ASSERT_GT(secret.size(), 4000u);

  std::string error;
  ASSERT_TRUE(store.DeleteCredential(slot, &error)) << error;
  error.clear();

  ASSERT_TRUE(store.SaveCredential(slot, secret, &error)) << error;
  EXPECT_TRUE(store.HasCredential(slot));

  std::string loaded;
  ASSERT_TRUE(store.LoadCredential(slot, &loaded, &error)) << error;
  EXPECT_EQ(loaded, secret);

  EXPECT_TRUE(store.DeleteCredential(slot, &error)) << error;
  EXPECT_FALSE(store.HasCredential(slot));
}
#endif

}  // namespace
}  // namespace alcedo
