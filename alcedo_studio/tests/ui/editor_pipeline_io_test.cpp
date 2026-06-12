//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include <gtest/gtest.h>

#include "edit/operators/operator_registeration.hpp"
#include "edit/pipeline/pipeline_cpu.hpp"
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

TEST(EditorPipelineIoTest, HalationFieldTargetsOutputTransformOperator) {
  const auto [stage, op] = pipeline_io::FieldSpec(AdjustmentField::Halation);

  EXPECT_EQ(stage, PipelineStageName::Output_Transform);
  EXPECT_EQ(op, OperatorType::HALATION);
}

TEST(EditorPipelineIoTest, HalationParamsUseStrengthOnlySerializedShape) {
  AdjustmentState state;
  state.halation_ = 42.0f;

  const auto params = pipeline_io::ParamsForField(AdjustmentField::Halation, state, nullptr);

  ASSERT_TRUE(params.contains("halation"));
  ASSERT_TRUE(params.at("halation").is_object());
  EXPECT_EQ(params.at("halation").size(), 1U);
  EXPECT_FLOAT_EQ(params.at("halation").at("strength").get<float>(), 42.0f);
}

TEST(EditorPipelineIoTest, HalationFieldChangedAndCopyFieldStateUseHalationValue) {
  AdjustmentState current;
  AdjustmentState committed;
  current.halation_ = 35.0f;

  EXPECT_TRUE(pipeline_io::FieldChanged(AdjustmentField::Halation, current, committed));

  CopyFieldState(AdjustmentField::Halation, current, committed);

  EXPECT_FLOAT_EQ(committed.halation_, 35.0f);
  EXPECT_FALSE(pipeline_io::FieldChanged(AdjustmentField::Halation, current, committed));
}

TEST(EditorPipelineIoTest, HalationStrengthRoundTripsThroughPipelineLoad) {
  alcedo::RegisterAllOperators();
  CPUPipelineExecutor source;
  auto&               output = source.GetStage(PipelineStageName::Output_Transform);
  output.SetOperator(OperatorType::HALATION, {{"halation", {{"strength", 64.0f}}}},
                     source.GetGlobalParams());

  const auto exported = source.ExportPipelineParams();
  const auto& halation =
      exported.at("Output Transform").at("Output Transform").at("halation").at("params");

  ASSERT_TRUE(halation.contains("halation"));
  EXPECT_EQ(halation.at("halation").size(), 1U);
  EXPECT_FLOAT_EQ(halation.at("halation").at("strength").get<float>(), 64.0f);

  CPUPipelineExecutor loaded;
  loaded.ImportPipelineParams(exported);

  auto [state, has_any] = pipeline_io::LoadStateFromPipeline(loaded, AdjustmentState{});

  EXPECT_TRUE(has_any);
  EXPECT_FLOAT_EQ(state.halation_, 64.0f);
}

TEST(EditorPipelineIoTest, HalationLoadAcceptsLegacyNumericShape) {
  alcedo::RegisterAllOperators();
  CPUPipelineExecutor exec;
  auto&               output = exec.GetStage(PipelineStageName::Output_Transform);
  output.SetOperator(OperatorType::HALATION, {{"halation", 27.0f}}, exec.GetGlobalParams());

  auto [state, has_any] = pipeline_io::LoadStateFromPipeline(exec, AdjustmentState{});

  EXPECT_TRUE(has_any);
  EXPECT_FLOAT_EQ(state.halation_, 27.0f);
}

TEST(EditorPipelineIoTest, ImportOldSnapshotWithoutFilmGrainAndHalationResetsThemToDefaults) {
  alcedo::RegisterAllOperators();
  CPUPipelineExecutor current;
  auto&               current_output = current.GetStage(PipelineStageName::Output_Transform);
  current_output.SetOperator(OperatorType::FILM_GRAIN,
                             {{"film_grain", {{"strength", 66.0f}}}},
                             current.GetGlobalParams());
  current_output.SetOperator(OperatorType::HALATION, {{"halation", {{"strength", 55.0f}}}},
                             current.GetGlobalParams());

  CPUPipelineExecutor old_snapshot_source;
  auto                old_snapshot = old_snapshot_source.ExportPipelineParams();
  auto&               output_stage = old_snapshot["Output Transform"]["Output Transform"];
  output_stage.erase("film_grain");
  output_stage.erase("halation");

  current.ImportPipelineParams(old_snapshot);
  auto [state, has_any] = pipeline_io::LoadStateFromPipeline(current, AdjustmentState{});

  EXPECT_TRUE(has_any);
  EXPECT_FLOAT_EQ(state.film_grain_, 0.0f);
  EXPECT_FLOAT_EQ(state.halation_, 0.0f);
}

}  // namespace
}  // namespace alcedo::ui
