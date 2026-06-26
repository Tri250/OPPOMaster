//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <optional>
#include <string>
#include <vector>

namespace alcedo {

struct AiSidecarRuntimeModelInfo {
  std::string profile_id;
  std::string model_id;
  std::string revision;
  std::string engine_profile_id;
  std::string language;
  uint32_t    embedding_dimension        = 0;
  uint32_t    native_embedding_dimension = 0;
  uint32_t    image_size                 = 0;
  std::string embedding_transform;
  std::string provider;
  std::string model_root;
  std::string prototype_config_hash;
};

struct AiSidecarRuntimeRemoteStatus {
  std::string state;
  std::string provider;
  uint32_t    image_batch_cap     = 0;
  uint32_t    image_batch_wait_ms = 0;
  uint64_t    uptime_ms           = 0;
};

struct AiSidecarCapability {
  std::string      task_id;
  std::string      provider_id;
  std::string      model_id;
  std::vector<int> input_kinds;
  std::vector<int> output_kinds;
  bool             supports_batch      = false;
  bool             supports_cancel     = false;
  bool             requires_credential = false;
  int64_t          max_payload_bytes   = 0;
};

}  // namespace alcedo
