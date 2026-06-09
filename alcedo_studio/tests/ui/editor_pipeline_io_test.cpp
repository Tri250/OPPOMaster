//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include <gtest/gtest.h>

#include "ui/alcedo_main/editor_dialog/modules/pipeline_io.hpp"

namespace alcedo::ui {
namespace {

TEST(EditorPipelineIoTest, HighlightReadbackPrefersSerializedNegativeSliderValue) {
  OperatorParams params;
  params.highlights_offset_ = -1.5f;

  const auto resolved       = pipeline_io::ResolveHighlightsSliderValue(-100.0f, true, params);

  ASSERT_TRUE(resolved.has_value());
  EXPECT_FLOAT_EQ(resolved.value(), -100.0f);
}

TEST(EditorPipelineIoTest, HighlightGlobalParamFallbackUsesFullSliderInverseScale) {
  OperatorParams params;
  params.highlights_offset_ = -1.5f;

  const auto resolved       = pipeline_io::ResolveHighlightsSliderValue(std::nullopt, true, params);

  ASSERT_TRUE(resolved.has_value());
  EXPECT_FLOAT_EQ(resolved.value(), -100.0f);
}

}  // namespace
}  // namespace alcedo::ui
