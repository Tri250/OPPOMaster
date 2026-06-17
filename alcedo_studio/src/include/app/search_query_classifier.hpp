//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstddef>
#include <string>
#include <string_view>

namespace alcedo {

/// Routes a user search query can take. Exposed to QML and tests as a stable
/// string via SearchQueryRouteName. Label and Traditional both execute the
/// ordinary SQL search path; the distinction is observability and the rule that
/// neither starts the semantic runtime. Semantic is the only route that reaches
/// a SemanticSearchProvider.
enum class SearchQueryRoute {
  Empty,
  Traditional,  // metadata / EXIF / filename / structured tokens
  Label,        // exact label name, synonym, or #tag
  Semantic,     // natural-language free text
};

struct SearchQueryClassification {
  SearchQueryRoute route_ = SearchQueryRoute::Empty;
  std::wstring     normalized_query_{};   // trimmed query used for matching
  std::string      matched_label_{};      // canonical label when route_ == Label
  bool             too_long_ = false;     // semantic query exceeds the token budget
};

/// Conservative prompt-token budget used as a control-layer backstop for the
/// CLIP context length. The real per-model limit is enforced in the runtime
/// (5D) via GetModelInfo; this only prevents obviously over-long queries from
/// being routed to embedding.
inline constexpr std::size_t kDefaultSemanticPromptTokenBudget = 64;

/// Classify a search query. `semantic_toggle_enabled` decides whether genuine
/// natural-language text routes to Semantic (toggle on) or Traditional (off).
/// Label, tag, metadata, and empty routes are independent of the toggle.
SearchQueryClassification ClassifySearchQuery(
    const std::wstring& query, bool semantic_toggle_enabled,
    std::size_t max_prompt_tokens = kDefaultSemanticPromptTokenBudget);

/// Stable lowercase name for a route: "empty" | "traditional" | "label" |
/// "semantic". Used by QML and tests.
std::string_view SearchQueryRouteName(SearchQueryRoute route);

/// Rough prompt-token estimate: each CJK character counts as one token, each
/// non-CJK whitespace-delimited word counts as one token. Deliberately
/// conservative; it is only a guard, not a tokenizer replacement.
std::size_t EstimatePromptTokens(const std::wstring& query);

}  // namespace alcedo
