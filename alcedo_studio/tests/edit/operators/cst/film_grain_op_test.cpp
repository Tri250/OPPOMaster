//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include <gtest/gtest.h>

#include "edit/operators/cst/film_grain_op.hpp"
#include "edit/operators/operator_factory.hpp"
#include "edit/operators/operator_registeration.hpp"
#include "edit/pipeline/default_pipeline_params.hpp"

namespace alcedo {

TEST(FilmGrainOpTest, DefaultParamsExposeOnlyStrength) {
  FilmGrainOp op;
  const auto  params = op.GetParams();

  ASSERT_TRUE(params.contains("film_grain"));
  ASSERT_TRUE(params.at("film_grain").is_object());
  EXPECT_EQ(params.at("film_grain").size(), 1U);
  EXPECT_FLOAT_EQ(params.at("film_grain").at("strength").get<float>(), 0.0f);
  EXPECT_FLOAT_EQ(op.strength_scale(), 0.0f);
}

TEST(FilmGrainOpTest, ReadsStrengthAndClampsToUserRange) {
  FilmGrainOp op({{"film_grain", {{"strength", 135.0f}}}});
  EXPECT_FLOAT_EQ(op.strength(), 100.0f);
  EXPECT_FLOAT_EQ(op.strength_scale(), 1.0f);

  op.SetParams({{"film_grain", {{"strength", -10.0f}}}});
  EXPECT_FLOAT_EQ(op.strength(), 0.0f);
  EXPECT_FLOAT_EQ(op.strength_scale(), 0.0f);
}

TEST(FilmGrainOpTest, MissingParamsStayAtNeutralDefault) {
  FilmGrainOp op({{"odt", {{"method", "open_drt"}}}});
  EXPECT_FLOAT_EQ(op.strength(), 0.0f);
  EXPECT_FLOAT_EQ(op.strength_scale(), 0.0f);
}

TEST(FilmGrainOpTest, GlobalParamsHookIsCurrentlyNoOp) {
  FilmGrainOp    op({{"film_grain", {{"strength", 50.0f}}}});
  OperatorParams params;
  const auto     before = params.to_output_enabled_;

  op.EnableGlobalParams(params, true);
  op.SetGlobalParams(params);

  EXPECT_EQ(params.to_output_enabled_, before);
}

TEST(FilmGrainOpTest, FactoryCreatesFilmGrainOperator) {
  RegisterAllOperators();
  auto op = OperatorFactory::Instance().Create(OperatorType::FILM_GRAIN,
                                               pipeline_defaults::MakeDefaultFilmGrainParams());
  ASSERT_NE(op, nullptr);
  EXPECT_EQ(op->GetScriptName(), "film_grain");
  EXPECT_EQ(op->GetStage(), PipelineStageName::Output_Transform);
}

}  // namespace alcedo
