//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace alcedo {

struct SemanticEmbeddingResult {
  std::string        request_id;
  std::vector<float> embedding;
  uint32_t           dimension  = 0;
  std::string        model_name;
  uint64_t           elapsed_ms = 0;
  bool               ok         = false;
  std::string        error;
};

struct SemanticTextEmbeddingRequest {
  std::string request_id;
  std::string text;
  std::string model_name;
};

struct SemanticImageEmbeddingRequest {
  std::string          request_id;
  std::vector<uint8_t> rgba8_image;
  std::string          format_hint;
  std::string          model_name;
};

}  // namespace alcedo
