//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/search_query_classifier.hpp"

#include <gtest/gtest.h>

#include <string>
#include <string_view>

namespace alcedo {
namespace {

using Route = SearchQueryRoute;

auto Classify(std::wstring_view query, bool toggle = false,
              std::size_t budget = kDefaultSemanticPromptTokenBudget) -> SearchQueryClassification {
  return ClassifySearchQuery(std::wstring(query), toggle, budget);
}

TEST(SearchQueryClassifierTest, EmptyQueryRoutesToEmpty) {
  EXPECT_EQ(Classify(L"").route_, Route::Empty);
  EXPECT_EQ(Classify(L"   ").route_, Route::Empty);
  EXPECT_EQ(Classify(L"\t\n").route_, Route::Empty);
  EXPECT_EQ(SearchQueryRouteName(Route::Empty), "empty");
}

TEST(SearchQueryClassifierTest, LabelNamesRouteToLabelRegardlessOfToggle) {
  for (bool toggle : {false, true}) {
    EXPECT_EQ(Classify(L"portrait", toggle).route_, Route::Label);
    EXPECT_EQ(Classify(L"Portrait", toggle).route_, Route::Label);
    EXPECT_EQ(Classify(L"  portrait  ", toggle).route_, Route::Label);
    EXPECT_EQ(Classify(L"人像", toggle).route_, Route::Label);
    EXPECT_EQ(Classify(L"#portrait", toggle).route_, Route::Label);
    EXPECT_EQ(Classify(L"#人像", toggle).route_, Route::Label);
    EXPECT_EQ(Classify(L"food and drink", toggle).route_, Route::Label);
    EXPECT_EQ(Classify(L"Food And Drink", toggle).route_, Route::Label);
    EXPECT_EQ(Classify(L"black and white", toggle).route_, Route::Label);
    EXPECT_EQ(Classify(L"sunrise and sunset", toggle).route_, Route::Label);
    EXPECT_EQ(Classify(L"long exposure", toggle).route_, Route::Label);
    EXPECT_EQ(Classify(L"still life", toggle).route_, Route::Label);
  }
  EXPECT_EQ(Classify(L"portrait").matched_label_, "portrait");
  EXPECT_EQ(Classify(L"人像").matched_label_, "portrait");
  EXPECT_EQ(Classify(L"风景").matched_label_, "landscape");
  EXPECT_EQ(Classify(L"#portrait").matched_label_, "portrait");
  EXPECT_EQ(SearchQueryRouteName(Route::Label), "label");
}

TEST(SearchQueryClassifierTest, MetadataTokensRouteToTraditional) {
  for (bool toggle : {false, true}) {
    EXPECT_EQ(Classify(L"Canon", toggle).route_, Route::Traditional);
    EXPECT_EQ(Classify(L"canon", toggle).route_, Route::Traditional);
    EXPECT_EQ(Classify(L"Nikon", toggle).route_, Route::Traditional);
    EXPECT_EQ(Classify(L"Sony", toggle).route_, Route::Traditional);
    EXPECT_EQ(Classify(L"2024", toggle).route_, Route::Traditional);
    EXPECT_EQ(Classify(L"2024-03-01", toggle).route_, Route::Traditional);
    EXPECT_EQ(Classify(L"2024/03", toggle).route_, Route::Traditional);
    EXPECT_EQ(Classify(L"20240301", toggle).route_, Route::Traditional);
    EXPECT_EQ(Classify(L"f/2.8", toggle).route_, Route::Traditional);
    EXPECT_EQ(Classify(L"f2.8", toggle).route_, Route::Traditional);
    EXPECT_EQ(Classify(L"50mm", toggle).route_, Route::Traditional);
    EXPECT_EQ(Classify(L"ISO 100", toggle).route_, Route::Traditional);
    EXPECT_EQ(Classify(L"iso100", toggle).route_, Route::Traditional);
    EXPECT_EQ(Classify(L"_DSC2296.ARW", toggle).route_, Route::Traditional);
    EXPECT_EQ(Classify(L"100", toggle).route_, Route::Traditional);
  }
  EXPECT_EQ(SearchQueryRouteName(Route::Traditional), "traditional");
}

TEST(SearchQueryClassifierTest, NaturalLanguageRoutesByToggle) {
  // Toggle off -> natural language stays on the ordinary path.
  EXPECT_EQ(Classify(L"sunset over the mountains", false).route_, Route::Traditional);
  EXPECT_EQ(Classify(L"夕阳下的山脉", false).route_, Route::Traditional);
  // Toggle on -> natural language routes to semantic.
  EXPECT_EQ(Classify(L"sunset over the mountains", true).route_, Route::Semantic);
  EXPECT_EQ(Classify(L"夕阳下的山脉", true).route_, Route::Semantic);
  EXPECT_EQ(SearchQueryRouteName(Route::Semantic), "semantic");
}

TEST(SearchQueryClassifierTest, MetadataTokenInMixedQueryForcesTraditional) {
  // Even with the toggle on, a metadata-shaped token keeps the query on the
  // ordinary path instead of starting the semantic runtime.
  EXPECT_EQ(Classify(L"sunset Canon", true).route_, Route::Traditional);
  EXPECT_EQ(Classify(L"portrait 2024", true).route_, Route::Traditional);
  EXPECT_EQ(Classify(L"beach f/2.8", true).route_, Route::Traditional);
}

TEST(SearchQueryClassifierTest, NonLabelTagRoutesToNaturalLanguage) {
  // #tag whose body is not a known label is not a label query.
  EXPECT_EQ(Classify(L"#myvacation", true).route_, Route::Semantic);
  EXPECT_EQ(Classify(L"#myvacation", false).route_, Route::Traditional);
}

TEST(SearchQueryClassifierTest, PromptLengthBudgetFlagsTooLongSemanticQueries) {
  auto make_words = [](int count) {
    std::wstring q;
    for (int i = 0; i < count; ++i) {
      if (i > 0) {
        q += L' ';
      }
      q += L"photo";
    }
    return q;
  };

  // Within budget: semantic, not too long.
  auto within = Classify(make_words(60), true);
  EXPECT_EQ(within.route_, Route::Semantic);
  EXPECT_FALSE(within.too_long_);

  // Over budget: still semantic, but flagged too long.
  auto over = Classify(make_words(70), true);
  EXPECT_EQ(over.route_, Route::Semantic);
  EXPECT_TRUE(over.too_long_);

  // The toggle-off path never routes to semantic, so the budget is irrelevant.
  auto over_off = Classify(make_words(70), false);
  EXPECT_EQ(over_off.route_, Route::Traditional);
  EXPECT_FALSE(over_off.too_long_);
}

TEST(SearchQueryClassifierTest, EstimatePromptTokensCountsCjkAndWords) {
  EXPECT_EQ(EstimatePromptTokens(L""), 0u);
  EXPECT_EQ(EstimatePromptTokens(L"sunset over the mountains"), 4u);
  EXPECT_EQ(EstimatePromptTokens(L"人像"), 2u);          // two CJK characters
  EXPECT_EQ(EstimatePromptTokens(L"夕阳下的山脉"), 6u);   // six CJK characters
  EXPECT_EQ(EstimatePromptTokens(L"风景 landscape"), 3u);  // 2 CJK + 1 latin word
}

}  // namespace
}  // namespace alcedo
