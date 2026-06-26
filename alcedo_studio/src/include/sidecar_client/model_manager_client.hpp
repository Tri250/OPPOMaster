//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <chrono>
#include <string>
#include <vector>

#include "sidecar_client/dto/model_manager.hpp"

namespace alcedo::sidecar_client {

class ModelManagerClient {
 public:
  virtual ~ModelManagerClient() = default;

  virtual auto ListModelProfiles(const std::string& model_root,
                                 std::chrono::milliseconds timeout, std::string* error)
      -> std::vector<SemanticModelProfileInfo> = 0;
  virtual auto ListInstalledModels(const std::string& model_root,
                                   std::chrono::milliseconds timeout, std::string* error)
      -> std::vector<SemanticModelProfileInfo> = 0;
  virtual auto ValidateModel(const std::string& profile_id, const std::string& model_root,
                             std::chrono::milliseconds timeout) -> SemanticModelManagerResult = 0;
  virtual auto DeleteModel(const std::string& profile_id, const std::string& model_root,
                           std::chrono::milliseconds timeout) -> SemanticModelManagerResult = 0;
};

}  // namespace alcedo::sidecar_client
