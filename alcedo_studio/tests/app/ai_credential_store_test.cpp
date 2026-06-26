//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/ai_credential_store.hpp"

#include <gtest/gtest.h>

#include <memory>
#include <string>

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

}  // namespace
}  // namespace alcedo
