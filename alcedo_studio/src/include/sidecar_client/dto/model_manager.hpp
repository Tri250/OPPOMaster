//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <optional>
#include <string>
#include <vector>

namespace alcedo {

struct SemanticModelAssetInfo {
  std::string role;
  std::string repo_id;
  std::string revision;
  std::string remote_path;
  std::string local_path;
  uint64_t    size_bytes = 0;
  std::string sha256;
};

struct SemanticModelProfileInfo {
  std::string                         profile_id;
  std::string                         display_name;
  std::string                         model_id;
  std::string                         revision;
  std::string                         engine_profile_id;
  std::string                         language;
  uint32_t                            embedding_dimension        = 0;
  uint32_t                            native_embedding_dimension = 0;
  uint32_t                            image_size                 = 0;
  bool                                installed                  = false;
  std::string                         local_root;
  std::string                         status;
  std::string                         embedding_transform;
  std::vector<SemanticModelAssetInfo> assets;
};

struct SemanticResolvedModelManifest {
  std::string                         profile_id;
  std::string                         model_id;
  std::string                         revision;
  std::string                         engine_profile_id;
  std::string                         language;
  uint32_t                            embedding_dimension        = 0;
  uint32_t                            native_embedding_dimension = 0;
  uint32_t                            image_size                 = 0;
  std::string                         embedding_transform;
  std::string                         model_root;
  std::vector<SemanticModelAssetInfo> assets;
};

struct SemanticModelManagerResult {
  bool                                         ok = false;
  std::string                                  status;
  std::string                                  error;
  SemanticModelProfileInfo                     profile;
  std::optional<SemanticResolvedModelManifest> manifest;
};

}  // namespace alcedo
