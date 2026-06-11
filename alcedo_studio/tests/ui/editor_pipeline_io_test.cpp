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

TEST(EditorPipelineIoTest, FilmGrainFieldTargetsOutputTransformOperator) {
  const auto [stage, op] = pipeline_io::FieldSpec(AdjustmentField::FilmGrain);

  EXPECT_EQ(stage, PipelineStageName::Output_Transform);
  EXPECT_EQ(op, OperatorType::FILM_GRAIN);
}

TEST(EditorPipelineIoTest, FilmGrainParamsUseStrengthOnlySerializedShape) {
  AdjustmentState state;
  state.film_grain_ = 42.0f;

  const auto params = pipeline_io::ParamsForField(AdjustmentField::FilmGrain, state, nullptr);

  ASSERT_TRUE(params.contains("film_grain"));
  ASSERT_TRUE(params.at("film_grain").is_object());
  EXPECT_EQ(params.at("film_grain").size(), 1U);
  EXPECT_FLOAT_EQ(params.at("film_grain").at("strength").get<float>(), 42.0f);
}

TEST(EditorPipelineIoTest, FilmGrainFieldChangedAndCopyFieldStateUseFilmGrainValue) {
  AdjustmentState current;
  AdjustmentState committed;
  current.film_grain_ = 35.0f;

  EXPECT_TRUE(pipeline_io::FieldChanged(AdjustmentField::FilmGrain, current, committed));

  CopyFieldState(AdjustmentField::FilmGrain, current, committed);

  EXPECT_FLOAT_EQ(committed.film_grain_, 35.0f);
  EXPECT_FALSE(pipeline_io::FieldChanged(AdjustmentField::FilmGrain, current, committed));
}

}  // namespace
}  // namespace alcedo::ui
