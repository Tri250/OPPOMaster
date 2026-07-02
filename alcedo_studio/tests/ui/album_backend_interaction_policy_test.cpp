//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include <QSignalSpy>
#include <QString>
#include <QVariantList>
#include <QVariantMap>

#include "ui/album_backend_test_fixture.hpp"
#include "ui/alcedo_main/album_backend/background_task_controller.hpp"
#include "ui/alcedo_main/album_backend/interaction_policy_controller.hpp"

namespace alcedo::ui::test {
namespace {

using AlbumBackendInteractionPolicyTests = AlbumBackendTestFixture;

auto Target(uint64_t elementId) -> QVariantMap {
  QVariantMap m;
  m.insert(QStringLiteral("elementId"), static_cast<qulonglong>(elementId));
  return m;
}

auto Targets(std::initializer_list<uint64_t> ids) -> QVariantList {
  QVariantList out;
  for (uint64_t id : ids) {
    out.append(Target(id));
  }
  return out;
}

auto Lock(InteractionCapability cap, uint64_t eid, const QString& reason) -> InteractionLock {
  return InteractionLock{cap, static_cast<quint64>(eid), reason};
}

// Register a task of `kind` holding `locks` and return its id.
auto RegisterLockedTask(BackgroundTaskController& registry, BackgroundTaskKind kind,
                        const std::vector<InteractionLock>& locks) -> QString {
  BackgroundTaskSnapshot s;
  s.kind_            = kind;
  s.state_           = BackgroundTaskState::Running;
  s.title_           = QStringLiteral("test");
  s.cancelable_      = false;
  s.shutdown_policy_ = BackgroundTaskShutdownPolicy::CancelAndWait;
  s.locks_           = locks;
  return registry.RegisterTask(s);
}

TEST_F(AlbumBackendInteractionPolicyTests, NoTasks_AllCapabilitiesAllowed) {
  BackgroundTaskController    registry;
  InteractionPolicyController policy(&registry);
  EXPECT_TRUE(policy.EvaluateEditImageDescription(42).value("allowed").toBool());
  EXPECT_TRUE(policy.EvaluateEditImageRating(42).value("allowed").toBool());
  EXPECT_TRUE(policy.EvaluateEditImageRatingReason(42).value("allowed").toBool());
  EXPECT_TRUE(policy.EvaluateRunImageAnalysis(Targets({42})).value("allowed").toBool());
  EXPECT_TRUE(policy.EvaluateDeleteImages(Targets({42})).value("allowed").toBool());
  EXPECT_TRUE(policy.EvaluateChangeSemanticModel().value("allowed").toBool());
  EXPECT_TRUE(policy.EvaluateRunSemanticGeneration().value("allowed").toBool());
  EXPECT_TRUE(policy.EvaluateChangeModelDownloadSettings().value("allowed").toBool());
  EXPECT_TRUE(policy.EvaluateChangeImageAnalysisProvider().value("allowed").toBool());
  // Cached Q_PROPERTYs likewise true.
  policy.SetFocusedElementId(42);
  EXPECT_TRUE(policy.CanEditFocusedDescription());
  EXPECT_TRUE(policy.CanEditFocusedRating());
  EXPECT_TRUE(policy.CanEditFocusedRatingReason());
  EXPECT_TRUE(policy.CanDeletePendingTargets());
  EXPECT_TRUE(policy.CanRunAnalysis());
  EXPECT_TRUE(policy.CanChangeSemanticModel());
  EXPECT_TRUE(policy.CanRunSemanticGeneration());
  EXPECT_TRUE(policy.CanChangeModelDownloadSettings());
  EXPECT_TRUE(policy.CanChangeImageAnalysisProvider());
}

TEST_F(AlbumBackendInteractionPolicyTests, ImageAnalysisPerElementLocks_BlockAffectedOnly) {
  BackgroundTaskController    registry;
  InteractionPolicyController policy(&registry);
  const QString               id = RegisterLockedTask(
      registry, BackgroundTaskKind::ImageAnalysis,
      {
          Lock(InteractionCapability::EditImageDescription, 42, QStringLiteral("analyzing")),
          Lock(InteractionCapability::EditImageRating, 42, QStringLiteral("analyzing")),
          Lock(InteractionCapability::EditImageRatingReason, 42, QStringLiteral("analyzing")),
          Lock(InteractionCapability::RunImageAnalysis, 42, QStringLiteral("rerun")),
          Lock(InteractionCapability::DeleteImages, 42, QStringLiteral("no delete")),
          Lock(InteractionCapability::ChangeImageAnalysisProvider, 0,
                             QStringLiteral("provider locked")),
          Lock(InteractionCapability::ChangeSemanticModel, 0, QStringLiteral("model locked")),
          Lock(InteractionCapability::ChangeModelDownloadSettings, 0,
                             QStringLiteral("model files locked")),
      });

  // Affected image 42 is blocked across the image-edit + run + delete caps.
  const QVariantMap desc42 = policy.EvaluateEditImageDescription(42);
  EXPECT_FALSE(desc42.value("allowed").toBool());
  EXPECT_FALSE(desc42.value("reason").toString().isEmpty());
  const QStringList blocking = desc42.value("blockingTaskIds").toStringList();
  EXPECT_TRUE(blocking.contains(id));

  EXPECT_FALSE(policy.EvaluateEditImageRating(42).value("allowed").toBool());
  EXPECT_FALSE(policy.EvaluateEditImageRatingReason(42).value("allowed").toBool());
  EXPECT_FALSE(policy.EvaluateDeleteImages(Targets({42})).value("allowed").toBool());
  EXPECT_FALSE(policy.EvaluateRunImageAnalysis(Targets({42})).value("allowed").toBool());
  // Unrelated image 99 is unaffected for the per-element caps.
  EXPECT_TRUE(policy.EvaluateEditImageDescription(99).value("allowed").toBool());
  EXPECT_TRUE(policy.EvaluateEditImageRating(99).value("allowed").toBool());
  EXPECT_TRUE(policy.EvaluateDeleteImages(Targets({99})).value("allowed").toBool());
  EXPECT_TRUE(policy.EvaluateRunImageAnalysis(Targets({99})).value("allowed").toBool());
  // Snapshot-setting caps are global, so they are blocked regardless of element.
  EXPECT_FALSE(policy.EvaluateChangeImageAnalysisProvider().value("allowed").toBool());
  EXPECT_FALSE(policy.EvaluateChangeSemanticModel().value("allowed").toBool());
  EXPECT_FALSE(policy.EvaluateChangeModelDownloadSettings().value("allowed").toBool());
  // A semantic run may still reuse the already-running sidecar; it is not a
  // startup-snapshot mutation.
  EXPECT_TRUE(policy.EvaluateRunSemanticGeneration().value("allowed").toBool());

  // Cached focused-image gates track the focused element id.
  policy.SetFocusedElementId(42);
  EXPECT_FALSE(policy.CanEditFocusedDescription());
  EXPECT_FALSE(policy.CanEditFocusedRating());
  EXPECT_FALSE(policy.CanEditFocusedRatingReason());
  EXPECT_FALSE(policy.FocusedEditReason().isEmpty());
  policy.SetFocusedElementId(99);
  EXPECT_TRUE(policy.CanEditFocusedDescription());
  EXPECT_TRUE(policy.CanEditFocusedRating());
  EXPECT_TRUE(policy.CanEditFocusedRatingReason());
  EXPECT_TRUE(policy.FocusedEditReason().isEmpty());

  // Pending delete / analysis target gates.
  policy.SetPendingDeleteTargets(Targets({42}));
  EXPECT_FALSE(policy.CanDeletePendingTargets());
  EXPECT_FALSE(policy.PendingDeleteReason().isEmpty());
  policy.SetPendingDeleteTargets(Targets({99}));
  EXPECT_TRUE(policy.CanDeletePendingTargets());

  policy.SetPendingAnalysisTargets(Targets({42}));
  EXPECT_FALSE(policy.CanRunAnalysis());
  EXPECT_FALSE(policy.RunAnalysisReason().isEmpty());
  policy.SetPendingAnalysisTargets(Targets({99}));
  EXPECT_TRUE(policy.CanRunAnalysis());
}

TEST_F(AlbumBackendInteractionPolicyTests, GlobalLock_BlocksEveryElement) {
  BackgroundTaskController    registry;
  InteractionPolicyController policy(&registry);
  RegisterLockedTask(
      registry, BackgroundTaskKind::ImageAnalysis,
      {
          Lock(InteractionCapability::DeleteImages, 0, QStringLiteral("album delete locked")),
          Lock(InteractionCapability::RunImageAnalysis, 0, QStringLiteral("album run locked")),
      });
  // A global (element_id == 0) lock blocks every element, not just 0.
  EXPECT_FALSE(policy.EvaluateDeleteImages(Targets({99})).value("allowed").toBool());
  EXPECT_FALSE(policy.EvaluateRunImageAnalysis(Targets({7})).value("allowed").toBool());
}

TEST_F(AlbumBackendInteractionPolicyTests, SemanticGenerationLocks_BlockModelAndGeneration) {
  BackgroundTaskController    registry;
  InteractionPolicyController policy(&registry);
  RegisterLockedTask(
      registry, BackgroundTaskKind::SemanticGeneration,
      {
          Lock(InteractionCapability::ChangeSemanticModel, 0, QStringLiteral("model")),
          Lock(InteractionCapability::RunSemanticGeneration, 0, QStringLiteral("gen")),
          Lock(InteractionCapability::ChangeModelDownloadSettings, 0, QStringLiteral("dl")),
          Lock(InteractionCapability::ChangeImageAnalysisProvider, 0, QStringLiteral("provider")),
      });
  EXPECT_FALSE(policy.EvaluateChangeSemanticModel().value("allowed").toBool());
  EXPECT_FALSE(policy.EvaluateRunSemanticGeneration().value("allowed").toBool());
  EXPECT_FALSE(policy.EvaluateChangeModelDownloadSettings().value("allowed").toBool());
  EXPECT_FALSE(policy.EvaluateChangeImageAnalysisProvider().value("allowed").toBool());
  EXPECT_TRUE(policy.EvaluateRunImageAnalysis(Targets({42})).value("allowed").toBool());
  EXPECT_FALSE(policy.CanChangeSemanticModel());
  EXPECT_FALSE(policy.CanRunSemanticGeneration());
  EXPECT_FALSE(policy.CanChangeModelDownloadSettings());
  // Image edits are unaffected by a semantic-generation run.
  EXPECT_TRUE(policy.EvaluateEditImageDescription(42).value("allowed").toBool());
}

TEST_F(AlbumBackendInteractionPolicyTests, ModelDownloadLocks_BlockSettingsAndModelNotGeneration) {
  BackgroundTaskController    registry;
  InteractionPolicyController policy(&registry);
  RegisterLockedTask(
      registry, BackgroundTaskKind::ModelDownload,
      {
          Lock(InteractionCapability::ChangeModelDownloadSettings, 0, QStringLiteral("dl")),
          Lock(InteractionCapability::ChangeSemanticModel, 0, QStringLiteral("model")),
      });
  EXPECT_FALSE(policy.EvaluateChangeModelDownloadSettings().value("allowed").toBool());
  EXPECT_FALSE(policy.EvaluateChangeSemanticModel().value("allowed").toBool());
  // Download does NOT block generation.
  EXPECT_TRUE(policy.EvaluateRunSemanticGeneration().value("allowed").toBool());
}

TEST_F(AlbumBackendInteractionPolicyTests, ModelActivationLocks_BlockAllThree) {
  BackgroundTaskController    registry;
  InteractionPolicyController policy(&registry);
  RegisterLockedTask(
      registry, BackgroundTaskKind::ModelActivation,
      {
          Lock(InteractionCapability::ChangeSemanticModel, 0, QStringLiteral("model")),
          Lock(InteractionCapability::RunSemanticGeneration, 0, QStringLiteral("gen")),
          Lock(InteractionCapability::ChangeModelDownloadSettings, 0, QStringLiteral("dl")),
          Lock(InteractionCapability::ChangeImageAnalysisProvider, 0, QStringLiteral("provider")),
      });
  EXPECT_FALSE(policy.EvaluateChangeSemanticModel().value("allowed").toBool());
  EXPECT_FALSE(policy.EvaluateRunSemanticGeneration().value("allowed").toBool());
  EXPECT_FALSE(policy.EvaluateChangeModelDownloadSettings().value("allowed").toBool());
  EXPECT_FALSE(policy.EvaluateChangeImageAnalysisProvider().value("allowed").toBool());
  EXPECT_TRUE(policy.EvaluateRunImageAnalysis(Targets({42})).value("allowed").toBool());
}

TEST_F(AlbumBackendInteractionPolicyTests, FinishTask_ClearsLocks) {
  BackgroundTaskController    registry;
  InteractionPolicyController policy(&registry);
  const QString               id = RegisterLockedTask(
      registry, BackgroundTaskKind::ImageAnalysis,
      {Lock(InteractionCapability::EditImageDescription, 42, QStringLiteral("analyzing"))});
  policy.SetFocusedElementId(42);
  EXPECT_FALSE(policy.CanEditFocusedDescription());
  registry.FinishTask(id, BackgroundTaskState::Succeeded);
  EXPECT_TRUE(policy.CanEditFocusedDescription());
}

TEST_F(AlbumBackendInteractionPolicyTests, PolicyChanged_FiresOnlyOnLockSetChange) {
  BackgroundTaskController    registry;
  InteractionPolicyController policy(&registry);
  QSignalSpy                  spy(&policy, &InteractionPolicyController::PolicyChanged);
  const QString               id = RegisterLockedTask(
      registry, BackgroundTaskKind::ImageAnalysis,
      {Lock(InteractionCapability::EditImageDescription, 42, QStringLiteral("analyzing"))});
  const int after_register = spy.count();
  EXPECT_GE(after_register, 1);
  // A progress tick does not change the lock set, so PolicyChanged must NOT fire.
  registry.UpdateTask(id, QStringLiteral("working"), QStringLiteral("d"), 42);
  EXPECT_EQ(spy.count(), after_register);
  // Finishing the task clears the lock set → PolicyChanged fires.
  registry.FinishTask(id, BackgroundTaskState::Succeeded);
  EXPECT_GT(spy.count(), after_register);
}

TEST_F(AlbumBackendInteractionPolicyTests, BlockingTaskIds_AggregatesAcrossTasks) {
  BackgroundTaskController    registry;
  InteractionPolicyController policy(&registry);
  const QString               a = RegisterLockedTask(
      registry, BackgroundTaskKind::SemanticGeneration,
      {Lock(InteractionCapability::ChangeSemanticModel, 0, QStringLiteral("a"))});
  const QString b = RegisterLockedTask(
      registry, BackgroundTaskKind::ModelActivation,
      {Lock(InteractionCapability::ChangeSemanticModel, 0, QStringLiteral("b"))});
  const QStringList ids =
      policy.EvaluateChangeSemanticModel().value("blockingTaskIds").toStringList();
  EXPECT_TRUE(ids.contains(a));
  EXPECT_TRUE(ids.contains(b));
  EXPECT_EQ(ids.size(), 2);
}

TEST_F(AlbumBackendInteractionPolicyTests, NullRegistry_PolicyStaysOpen) {
  InteractionPolicyController policy(nullptr);
  // No registry → no locks → everything allowed, no PolicyChanged expected.
  EXPECT_TRUE(policy.CanEditFocusedDescription());
  EXPECT_TRUE(policy.CanChangeSemanticModel());
  policy.SetFocusedElementId(42);
  EXPECT_TRUE(policy.CanEditFocusedDescription());
}

}  // namespace
}  // namespace alcedo::ui::test
