//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstddef>
#include <string>
#include <vector>

namespace alcedo {

inline constexpr const char* kDefaultSemanticPhotographyPromptConfigHash =
    "photography-labels-v1-simple";
inline constexpr double kDefaultSemanticLabelConfidenceThreshold = 0.20;
inline constexpr double kDefaultSemanticLabelMarginThreshold     = 0.03;
inline constexpr size_t kDefaultSemanticLabelTopScoreCount       = 5;

struct SemanticLabelQueryConfig {
  std::string label{};
  std::string query{};
};

struct SemanticGenerationLabelPrototype {
  std::string        label{};
  std::vector<float> embedding{};
};

inline auto DefaultSemanticPhotographyLabelQueries()
    -> const std::vector<SemanticLabelQueryConfig>& {
  static const std::vector<SemanticLabelQueryConfig> labels{
      {"portrait", "a photo of a portrait"},
      {"group", "a photo of a group of people"},
      {"family", "a family photo"},
      {"wedding", "a wedding photo"},
      {"event", "an event photo"},
      {"concert", "a concert photo"},
      {"stage", "a stage performance photo"},
      {"fashion", "a fashion photo"},
      {"sports", "a sports photo"},
      {"action", "an action photo"},
      {"street", "a street photography photo"},
      {"travel", "a travel photo"},
      {"landscape", "a landscape photo"},
      {"mountain", "a mountain photo"},
      {"forest", "a forest photo"},
      {"beach", "a beach photo"},
      {"coast", "a coastal photo"},
      {"lake", "a lake photo"},
      {"waterfall", "a waterfall photo"},
      {"cityscape", "a cityscape photo"},
      {"architecture", "an architecture photo"},
      {"interior", "an interior photo"},
      {"real estate", "a real estate photo"},
      {"food", "a food photo"},
      {"drink", "a drink photo"},
      {"product", "a product photo"},
      {"still life", "a still life photo"},
      {"macro", "a macro photo"},
      {"flower", "a flower photo"},
      {"wildlife", "a wildlife photo"},
      {"vehicle", "a vehicle photo"},
      {"night", "a night photo"},
      {"sunset", "a sunset photo"},
      {"sunrise", "a sunrise photo"},
      {"snow", "a snow photo"},
      {"autumn", "an autumn photo"},
      {"fog", "a foggy photo"},
      {"black and white", "a black and white photo"},
      {"silhouette", "a silhouette photo"},
      {"aerial", "an aerial photo"},
      {"drone", "a drone photo"},
      {"panorama", "a panorama photo"},
      {"long exposure", "a long exposure photo"},
      {"light trail", "a light trail photo"},
      {"fireworks", "a fireworks photo"},
      {"studio", "a studio photo"},
      {"document", "a document photo"},
      {"screenshot", "a screenshot"},
  };
  return labels;
}

inline auto DefaultSemanticPhotographyLabels() -> std::vector<std::string> {
  std::vector<std::string> labels;
  const auto&              queries = DefaultSemanticPhotographyLabelQueries();
  labels.reserve(queries.size());
  for (const auto& query : queries) {
    labels.push_back(query.label);
  }
  return labels;
}

}  // namespace alcedo
