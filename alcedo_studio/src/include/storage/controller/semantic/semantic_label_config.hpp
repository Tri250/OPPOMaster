//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstddef>
#include <string>
#include <vector>

namespace alcedo {

inline constexpr const char* kDefaultSemanticPhotographyPromptConfigHash =
    "photography-labels-v3";
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
      {"portrait", "a photograph whose main subject is one person or a close portrait"},
      {"group", "a photograph of several people together"},
      {"family", "a family photograph with relatives or close household members"},
      {"children", "a photograph where children or babies are the main subject"},
      {"wedding", "a wedding photograph of a couple, ceremony, reception, or bridal party"},
      {"ceremony", "a graduation, award, religious, or formal ceremony photograph"},
      {"event", "a social event, party, conference, or gathering photograph"},
      {"concert", "a concert or live music photograph"},
      {"performance", "a stage, theater, dance, or public performance photograph"},
      {"fashion", "a fashion, model, outfit, or editorial portrait photograph"},
      {"sports", "a sports photograph with athletes, games, races, or athletic activity"},
      {"street", "a street photography image of candid public life or urban scenes"},
      {"cityscape", "a skyline or broad city view photograph"},
      {"architecture", "a photograph focused on buildings, facades, or structures"},
      {"interior", "an indoor room, interior design, or architectural interior photograph"},
      {"landscape", "a wide natural landscape photograph"},
      {"mountain", "a mountain, cliff, or alpine landscape photograph"},
      {"forest", "a forest, woodland, trees, or trail photograph"},
      {"desert", "a desert, dunes, arid land, or canyon photograph"},
      {"beach", "a beach, shoreline, seaside, or sandy coast photograph"},
      {"lake", "a lake, river, pond, or calm inland water photograph"},
      {"waterfall", "a waterfall, cascade, or rushing water photograph"},
      {"garden", "a garden, park, cultivated plants, or landscaped greenery photograph"},
      {"flower", "a flower, blossom, or botanical close-up photograph"},
      {"wildlife", "a wild animal, bird, insect, or nature animal photograph"},
      {"pet", "a domestic pet such as a dog, cat, or companion animal photograph"},
      {"food and drink", "a photograph of prepared food, drinks, dining, or table service"},
      {"product", "a product, merchandise, packaging, or ecommerce photograph"},
      {"still life", "an arranged still life photograph of objects"},
      {"vehicle", "a car, motorcycle, bicycle, aircraft, boat, or other vehicle photograph"},
      {"artwork", "a photograph of artwork, sculpture, mural, craft, or museum objects"},
      {"document", "a document, receipt, sign, whiteboard, page, or printed text photograph"},
      {"screenshot", "a screenshot"},
      {"macro", "a macro or extreme close-up photograph with fine detail"},
      {"night", "a night, low-light, or dark scene photograph"},
      {"sunrise and sunset", "a sunrise, sunset, golden hour, or colorful sky photograph"},
      {"snow", "a snow, ice, frost, or winter photograph"},
      {"autumn", "an autumn, fall foliage, or seasonal leaves photograph"},
      {"fog", "a fog, mist, haze, or atmospheric weather photograph"},
      {"black and white", "a black and white, monochrome, or grayscale photograph"},
      {"silhouette", "a silhouette or strong backlit outline photograph"},
      {"aerial", "an aerial, drone, top-down, or high viewpoint photograph"},
      {"panorama", "a panoramic, wide aspect, or stitched landscape photograph"},
      {"long exposure", "a long exposure photograph with motion blur, smooth water, stars, or light trails"},
      {"fireworks", "a fireworks, sparkler, pyrotechnic, or celebration lights photograph"},
      {"studio", "a studio-lit photograph with controlled lighting or seamless background"},
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
