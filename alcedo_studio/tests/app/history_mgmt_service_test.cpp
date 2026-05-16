//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/history_mgmt_service.hpp"

#include <gtest/gtest.h>

#include <chrono>
#include <exiv2/exiv2.hpp>
#include <filesystem>
#include <memory>
#include <string>
#include <thread>
#include <tuple>

#include "app/project_service.hpp"
#include "edit/history/edit_history.hpp"
#include "edit/history/edit_transaction.hpp"
#include "edit/history/version.hpp"
#include "edit/operators/operator_registeration.hpp"
#include "edit/pipeline/pipeline_cpu.hpp"
#include "type/type.hpp"
#include "utils/clock/time_provider.hpp"

namespace alcedo {
class EditHistoryMgmtServiceTests : public ::testing::Test {
 protected:
  std::filesystem::path db_path_;
  std::filesystem::path meta_path_;

  void                  SetUp() override {
    TimeProvider::Refresh();
    Exiv2::LogMsg::setLevel(Exiv2::LogMsg::Level::mute);
    RegisterAllOperators();

    db_path_ = std::filesystem::temp_directory_path() / "history_mgmt_service_test.db";
    meta_path_ = std::filesystem::temp_directory_path() / "history_mgmt_service_test.json";

    if (std::filesystem::exists(db_path_)) {
      std::filesystem::remove(db_path_);
    }
    if (std::filesystem::exists(meta_path_)) {
      std::filesystem::remove(meta_path_);
    }
  }

  void TearDown() override {
    if (std::filesystem::exists(db_path_)) {
      std::filesystem::remove(db_path_);
    }
    if (std::filesystem::exists(meta_path_)) {
      std::filesystem::remove(meta_path_);
    }
  }

  static auto MakeWorkingVersionWithTwoTransactions(EditHistory& history, sl_element_id_t file_id,
                                                    float exposure, float contrast)
      -> std::tuple<WorkingVersion, nlohmann::json, nlohmann::json> {
    auto base_params =
        history.GetRootVersion().GetFinalPipelineParams().value_or(nlohmann::json::object());
    CPUPipelineExecutor exec;
    exec.ImportPipelineParams(base_params);

    WorkingVersion  working{file_id, history.GetRootVersionID(), base_params};

    EditTransaction tx1{TransactionType::_ADD,
                        OperatorType::EXPOSURE,
                        PipelineStageName::Basic_Adjustment,
                        nlohmann::json(nullptr),
                        nlohmann::json{{"exposure", exposure}},
                        false,
                        true};
    tx1.ApplyForward(exec);
    working.AppendEditTransaction(std::move(tx1));

    EditTransaction tx2{TransactionType::_ADD,
                        OperatorType::CONTRAST,
                        PipelineStageName::Basic_Adjustment,
                        nlohmann::json(nullptr),
                        nlohmann::json{{"contrast", contrast}},
                        false,
                        true};
    tx2.ApplyForward(exec);
    working.AppendEditTransaction(std::move(tx2));
    const auto head_params = exec.ExportPipelineParams();
    working.SetHeadPipelineParams(head_params);

    return {std::move(working), base_params, head_params};
  }
};

TEST_F(EditHistoryMgmtServiceTests, InitTest) {
  ProjectService project(db_path_, meta_path_);
  EXPECT_NO_THROW(EditHistoryMgmtService history_service(project.GetStorageService()));
}

TEST_F(EditHistoryMgmtServiceTests, WorkingVersionUndoRedoAndRedoTruncation) {
  constexpr sl_element_id_t file_id = 7;
  EditHistory               history(file_id);
  CPUPipelineExecutor       exec;

  auto&                     stage = exec.GetStage(PipelineStageName::Basic_Adjustment);
  auto                      op    = stage.GetOperator(OperatorType::EXPOSURE);
  ASSERT_TRUE(op.has_value());
  ASSERT_NE(op.value(), nullptr);

  const auto      before_params  = op.value()->op_->GetParams();
  const bool      before_enabled = op.value()->enable_;
  EditTransaction exposure_tx{TransactionType::_EDIT,
                              OperatorType::EXPOSURE,
                              PipelineStageName::Basic_Adjustment,
                              before_params,
                              nlohmann::json{{"exposure", 2.0f}},
                              before_enabled,
                              true};

  WorkingVersion  working(file_id, history.GetRootVersionID(), exec.ExportPipelineParams());
  ASSERT_TRUE(exposure_tx.ApplyForward(exec));
  working.AppendEditTransaction(std::move(exposure_tx));
  working.SetHeadPipelineParams(exec.ExportPipelineParams());
  EXPECT_EQ(working.GetAppliedTransactionCount(), 1U);
  EXPECT_FLOAT_EQ(
      stage.GetOperator(OperatorType::EXPOSURE).value()->op_->GetParams()["exposure"].get<float>(),
      2.0f);

  ASSERT_TRUE(working.UndoLastTransaction(exec));
  EXPECT_EQ(working.GetAppliedTransactionCount(), 0U);
  EXPECT_EQ(stage.GetOperator(OperatorType::EXPOSURE).value()->enable_, before_enabled);
  EXPECT_EQ(stage.GetOperator(OperatorType::EXPOSURE).value()->op_->GetParams(), before_params);

  ASSERT_TRUE(working.RedoNextTransaction(exec));
  EXPECT_EQ(working.GetAppliedTransactionCount(), 1U);
  EXPECT_FLOAT_EQ(
      stage.GetOperator(OperatorType::EXPOSURE).value()->op_->GetParams()["exposure"].get<float>(),
      2.0f);

  ASSERT_TRUE(working.UndoLastTransaction(exec));
  EditTransaction contrast_tx{TransactionType::_EDIT,
                              OperatorType::CONTRAST,
                              PipelineStageName::Basic_Adjustment,
                              stage.GetOperator(OperatorType::CONTRAST).value()->op_->GetParams(),
                              nlohmann::json{{"contrast", 0.5f}},
                              stage.GetOperator(OperatorType::CONTRAST).value()->enable_,
                              true};
  ASSERT_TRUE(contrast_tx.ApplyForward(exec));
  working.AppendEditTransaction(std::move(contrast_tx));
  EXPECT_EQ(working.GetAppliedTransactionCount(), 1U);
  EXPECT_EQ(working.GetAllEditTransactions().size(), 1U);
  EXPECT_FALSE(working.RedoNextTransaction(exec));
}

TEST_F(EditHistoryMgmtServiceTests, BasicHistoryRWTest) {
  constexpr sl_element_id_t file_id = 1;
  std::string               history_dump;
  history_id_t              committed_id{};

  {
    ProjectService         project(db_path_, meta_path_);
    EditHistoryMgmtService history_service(project.GetStorageService());

    auto                   history_guard = history_service.LoadHistory(file_id);
    ASSERT_NE(history_guard, nullptr);
    EXPECT_EQ(history_guard->file_id_, file_id);
    ASSERT_NE(history_guard->history_, nullptr);
    EXPECT_EQ(history_guard->history_->GetBoundImage(), file_id);
    EXPECT_TRUE(history_guard->pinned_);
    EXPECT_FALSE(history_guard->dirty_);

    // Commit a version using the same patterns as edit/history tests.
    auto [v1, base_params, head_params] =
        MakeWorkingVersionWithTwoTransactions(*history_guard->history_, file_id, 1.0f, 2.2f);
    committed_id =
        history_service.CommitVersion(history_guard, std::move(v1), base_params, head_params);
    EXPECT_NO_THROW((void)history_guard->history_->GetVersion(committed_id));
    EXPECT_TRUE(history_guard->dirty_);

    // Save + sync to persist to DB.
    EXPECT_NO_THROW(history_service.SaveHistory(history_guard));
    EXPECT_FALSE(history_guard->pinned_);
    EXPECT_FALSE(history_guard->dirty_);

    history_service.Sync();

    // Load again (cache hit path) and snapshot the serialized history.
    auto history_guard_2 = history_service.LoadHistory(file_id);
    ASSERT_NE(history_guard_2, nullptr);
    EXPECT_TRUE(history_guard_2->pinned_);
    EXPECT_FALSE(history_guard_2->dirty_);
    EXPECT_NO_THROW((void)history_guard_2->history_->GetVersion(committed_id));

    history_dump = history_guard_2->history_->ToJSON().dump(2);
  }

  // Reopen and load again to verify persistence.
  {
    ProjectService         project(db_path_, meta_path_);
    EditHistoryMgmtService history_service(project.GetStorageService());

    auto                   history_guard = history_service.LoadHistory(file_id);
    ASSERT_NE(history_guard, nullptr);
    EXPECT_TRUE(history_guard->pinned_);
    EXPECT_FALSE(history_guard->dirty_);
    EXPECT_NO_THROW((void)history_guard->history_->GetVersion(committed_id));

    auto history_dump_2 = history_guard->history_->ToJSON().dump(2);
    EXPECT_EQ(history_dump, history_dump_2);
  }
}

TEST_F(EditHistoryMgmtServiceTests, CommitWorkingVersionPersistsFullTimeline) {
  constexpr sl_element_id_t file_id = 11;

  ProjectService            project(db_path_, meta_path_);
  EditHistoryMgmtService    history_service(project.GetStorageService());

  auto                      history_guard = history_service.LoadHistory(file_id);
  ASSERT_NE(history_guard, nullptr);

  auto [working, base_params, head_params] =
      MakeWorkingVersionWithTwoTransactions(*history_guard->history_, file_id, 0.8f, 1.4f);
  const auto committed_id =
      history_service.CommitVersion(history_guard, std::move(working), base_params, head_params);

  auto& committed = history_guard->history_->GetVersion(committed_id);
  EXPECT_EQ(committed.GetParentVersionID(), history_guard->history_->GetRootVersionID());
  EXPECT_EQ(committed.GetAllEditTransactions().size(), 2U);
  EXPECT_EQ(committed.GetCursor(), 2U);
  EXPECT_EQ(committed.GetTransactionCount(), 2U);
}

TEST_F(EditHistoryMgmtServiceTests, VersionsStayIndependentAndNamesRoundTrip) {
  constexpr sl_element_id_t file_id = 12;
  history_id_t              warm_id{};
  history_id_t              cool_id{};

  {
    ProjectService         project(db_path_, meta_path_);
    EditHistoryMgmtService history_service(project.GetStorageService());

    auto                   history_guard = history_service.LoadHistory(file_id);
    ASSERT_NE(history_guard, nullptr);

    auto [warm_working, warm_base, warm_head] =
        MakeWorkingVersionWithTwoTransactions(*history_guard->history_, file_id, 1.1f, 0.6f);
    warm_id =
        history_service.CommitVersion(history_guard, std::move(warm_working), warm_base, warm_head);

    auto [cool_working, cool_base, cool_head] =
        MakeWorkingVersionWithTwoTransactions(*history_guard->history_, file_id, -0.4f, 1.8f);
    cool_id =
        history_service.CommitVersion(history_guard, std::move(cool_working), cool_base, cool_head);

    auto& warm_version = history_guard->history_->GetVersion(warm_id);
    auto& cool_version = history_guard->history_->GetVersion(cool_id);
    warm_version.SetDisplayName("Warm");
    cool_version.SetDisplayName("Cool");
    history_guard->dirty_ = true;

    const auto root_id    = history_guard->history_->GetRootVersionID();
    EXPECT_EQ(warm_version.GetParentVersionID(), root_id);
    EXPECT_EQ(cool_version.GetParentVersionID(), root_id);
    EXPECT_NE(warm_id, cool_id);
    EXPECT_NE(history_guard->history_->ReconstructPipelineParamsForVersion(warm_id),
              history_guard->history_->ReconstructPipelineParamsForVersion(cool_id));

    history_service.SaveHistory(history_guard);
    history_service.Sync();
  }

  {
    ProjectService         project(db_path_, meta_path_);
    EditHistoryMgmtService history_service(project.GetStorageService());

    auto                   history_guard = history_service.LoadHistory(file_id);
    ASSERT_NE(history_guard, nullptr);

    auto& warm_version = history_guard->history_->GetVersion(warm_id);
    auto& cool_version = history_guard->history_->GetVersion(cool_id);
    EXPECT_EQ(warm_version.GetDisplayName(), "Warm");
    EXPECT_EQ(cool_version.GetDisplayName(), "Cool");
    EXPECT_EQ(warm_version.GetParentVersionID(), history_guard->history_->GetRootVersionID());
    EXPECT_EQ(cool_version.GetParentVersionID(), history_guard->history_->GetRootVersionID());
  }
}

TEST_F(EditHistoryMgmtServiceTests, PersistedCursorKeepsRedoTailUntilNewEdit) {
  constexpr sl_element_id_t file_id = 13;

  ProjectService            project(db_path_, meta_path_);
  EditHistoryMgmtService    history_service(project.GetStorageService());

  auto                      history_guard = history_service.LoadHistory(file_id);
  ASSERT_NE(history_guard, nullptr);

  auto [working, base_params, head_params] =
      MakeWorkingVersionWithTwoTransactions(*history_guard->history_, file_id, 0.5f, 1.2f);
  const auto committed_id =
      history_service.CommitVersion(history_guard, std::move(working), base_params, head_params);

  auto&               committed = history_guard->history_->GetVersion(committed_id);
  CPUPipelineExecutor exec;
  exec.ImportPipelineParams(head_params);
  WorkingVersion replay(file_id, committed_id, head_params, committed.GetAllEditTransactions(),
                        committed.GetCursor());

  ASSERT_TRUE(replay.UndoLastTransaction(exec));
  const auto undone_params = exec.ExportPipelineParams();
  history_guard->history_->UpdateVersionFromWorkingVersion(committed_id, replay, base_params,
                                                           undone_params);

  auto& updated = history_guard->history_->GetVersion(committed_id);
  EXPECT_EQ(updated.GetVersionID(), committed_id);
  EXPECT_EQ(updated.GetAllEditTransactions().size(), 2U);
  EXPECT_EQ(updated.GetCursor(), 1U);

  const auto  serialized = history_guard->history_->ToJSON();
  EditHistory reloaded(file_id);
  reloaded.FromJSON(serialized);
  auto& persisted = reloaded.GetVersion(committed_id);
  EXPECT_EQ(persisted.GetAllEditTransactions().size(), 2U);
  EXPECT_EQ(persisted.GetCursor(), 1U);

  CPUPipelineExecutor redo_exec;
  redo_exec.ImportPipelineParams(undone_params);
  WorkingVersion redo_working(file_id, committed_id, undone_params,
                              persisted.GetAllEditTransactions(), persisted.GetCursor());
  ASSERT_TRUE(redo_working.RedoNextTransaction(redo_exec));
  EXPECT_EQ(redo_working.GetCursor(), 2U);

  ASSERT_TRUE(redo_working.UndoLastTransaction(redo_exec));
  auto* contrast_op = redo_exec.GetStage(PipelineStageName::Basic_Adjustment)
                          .GetOperator(OperatorType::CONTRAST)
                          .value();
  EditTransaction replacement_tx{TransactionType::_EDIT,
                                 OperatorType::CONTRAST,
                                 PipelineStageName::Basic_Adjustment,
                                 contrast_op->op_->GetParams(),
                                 nlohmann::json{{"contrast", 0.25f}},
                                 contrast_op->enable_,
                                 true};
  ASSERT_TRUE(replacement_tx.ApplyForward(redo_exec));
  redo_working.AppendEditTransaction(std::move(replacement_tx));
  EXPECT_EQ(redo_working.GetCursor(), 2U);
  EXPECT_EQ(redo_working.GetAllEditTransactions().size(), 2U);
  EXPECT_FALSE(redo_working.RedoNextTransaction(redo_exec));
}

TEST_F(EditHistoryMgmtServiceTests, SyncPersistsDirtyHistoryWithoutSave) {
  constexpr sl_element_id_t file_id = 42;
  std::string               history_dump;
  history_id_t              committed_id{};

  {
    ProjectService         project(db_path_, meta_path_);
    EditHistoryMgmtService history_service(project.GetStorageService());

    auto                   history_guard = history_service.LoadHistory(file_id);
    ASSERT_NE(history_guard, nullptr);

    // Ensure a different timestamp path is exercised.
    std::this_thread::sleep_for(std::chrono::milliseconds(5));

    auto [v1, base_params, head_params] =
        MakeWorkingVersionWithTwoTransactions(*history_guard->history_, file_id, 0.3f, 0.8f);
    committed_id =
        history_service.CommitVersion(history_guard, std::move(v1), base_params, head_params);
    EXPECT_TRUE(history_guard->dirty_);

    history_service.Sync();
    EXPECT_FALSE(history_guard->dirty_);

    history_dump = history_guard->history_->ToJSON().dump(2);
  }

  {
    ProjectService         project(db_path_, meta_path_);
    EditHistoryMgmtService history_service(project.GetStorageService());

    auto                   history_guard = history_service.LoadHistory(file_id);
    ASSERT_NE(history_guard, nullptr);
    EXPECT_NO_THROW((void)history_guard->history_->GetVersion(committed_id));

    auto history_dump_2 = history_guard->history_->ToJSON().dump(2);
    EXPECT_EQ(history_dump, history_dump_2);
  }
}

TEST_F(EditHistoryMgmtServiceTests, SaveHistoryNullGuardNoThrow) {
  ProjectService                    project(db_path_, meta_path_);
  EditHistoryMgmtService            history_service(project.GetStorageService());

  std::shared_ptr<EditHistoryGuard> null_guard;
  EXPECT_NO_THROW(history_service.SaveHistory(null_guard));
}
}  // namespace alcedo
