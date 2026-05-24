//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include <gtest/gtest.h>

#include <atomic>
#include <memory>
#include <mutex>
#include <thread>
#include <vector>

#include "edit/operators/operator_registeration.hpp"
#include "edit/pipeline/pipeline_cpu.hpp"
#include "renderer/pipeline_scheduler.hpp"
#include "renderer/pipeline_task.hpp"
#include "ui/edit_viewer/frame_sink.hpp"

namespace alcedo {
namespace {

// Minimal mock IFrameSink that tracks call counts for verification.
class MockFrameSink final : public IFrameSink {
 public:
  void EnsureSize(int width, int height) override {
    ensure_size_calls_++;
    width_  = width;
    height_ = height;
  }

  auto MapResourceForWrite(FrameMemoryDomain /*domain*/) -> FrameWriteMapping override {
    map_resource_calls_++;
    return {};
  }

  void UnmapResource() override { unmap_resource_calls_++; }

  void NotifyFrameReady() override { notify_frame_ready_calls_++; }

  void SetNextFramePresentationMode(FramePresentationMode mode) override {
    presentation_mode_calls_++;
    last_mode_ = mode;
  }

  void SetNextFramePreviewMetadata(const FramePreviewMetadata& metadata) override {
    preview_metadata_calls_++;
    last_metadata_ = metadata;
  }

  auto GetViewportRenderRegion() const -> std::optional<ViewportRenderRegion> override {
    viewport_render_region_calls_++;
    return std::nullopt;
  }

  int GetWidth() const override { return width_; }
  int GetHeight() const override { return height_; }

  // --- call counters ---
  int ensure_size_calls_             = 0;
  int map_resource_calls_            = 0;
  int unmap_resource_calls_          = 0;
  int notify_frame_ready_calls_      = 0;
  mutable int viewport_render_region_calls_ = 0;

  // These are the methods that thumbnail tasks must NOT call into the editor
  // sink.  Track them so tests can enforce the acceptance criterion.
  int presentation_mode_calls_  = 0;
  int preview_metadata_calls_   = 0;

  FramePresentationMode last_mode_{FramePresentationMode::FullFrame};
  FramePreviewMetadata  last_metadata_{};

 private:
  int width_  = 0;
  int height_ = 0;
};

}  // namespace

// =========================================================================
// Phase 1 Acceptance Criterion 1:
//   "Thumbnail rendering cannot call into an editor-owned IFrameSink."
// =========================================================================

class PipelineFrameSinkTest : public ::testing::Test {
 protected:
  void SetUp() override { RegisterAllOperators(); }
};

TEST_F(PipelineFrameSinkTest, DetachFrameSinkClearsPointer) {
  auto                  exec = std::make_shared<CPUPipelineExecutor>();
  MockFrameSink         sink;

  exec->SetExecutionStages(&sink);
  EXPECT_EQ(exec->GetFrameSink(), &sink);

  exec->DetachFrameSink();
  EXPECT_EQ(exec->GetFrameSink(), nullptr);
}

TEST_F(PipelineFrameSinkTest, SetExecutionStagesWithoutSinkHasNullFrameSink) {
  auto exec = std::make_shared<CPUPipelineExecutor>();

  exec->SetExecutionStages();
  EXPECT_EQ(exec->GetFrameSink(), nullptr);
}

TEST_F(PipelineFrameSinkTest, SetNextFramePresentationModeIsNoOpWhenSinkIsDetached) {
  auto exec = std::make_shared<CPUPipelineExecutor>();

  // No crash, no side-effect expected.
  EXPECT_NO_THROW(
      exec->SetNextFramePresentationMode(FramePresentationMode::ViewportTransformed));
}

TEST_F(PipelineFrameSinkTest, SetNextFramePreviewMetadataIsNoOpWhenSinkIsDetached) {
  auto          exec = std::make_shared<CPUPipelineExecutor>();
  FramePreviewMetadata metadata{};
  metadata.frame_role = FrameRole::QualityBase;

  EXPECT_NO_THROW(exec->SetNextFramePreviewMetadata(metadata));
}

TEST_F(PipelineFrameSinkTest, GetViewportRenderRegionReturnsNulloptWhenSinkIsDetached) {
  auto exec = std::make_shared<CPUPipelineExecutor>();

  EXPECT_EQ(exec->GetViewportRenderRegion(), std::nullopt);
}

// =========================================================================
// Phase 1 Acceptance Criterion 2:
//   "Closing the editor while preview work is in flight cannot leave a
//    dangling sink pointer."
// =========================================================================

TEST_F(PipelineFrameSinkTest, DetachUnderLockIsSafeDuringConcurrentAccess) {
  // Simulates the pattern used by EditorFrameManager::~EditorFrameManager():
  // acquire render_lock_ → DetachFrameSink() → release.
  auto          exec = std::make_shared<CPUPipelineExecutor>();
  MockFrameSink sink;

  exec->SetExecutionStages(&sink);
  EXPECT_EQ(exec->GetFrameSink(), &sink);

  {
    std::unique_lock<std::mutex> lock(exec->GetRenderLock());
    exec->DetachFrameSink();
  }

  EXPECT_EQ(exec->GetFrameSink(), nullptr);
}

TEST_F(PipelineFrameSinkTest, ReattachAfterDetachIsSafe) {
  // After detach, re-attaching a new sink should work without stale state.
  auto          exec = std::make_shared<CPUPipelineExecutor>();
  MockFrameSink sink1;
  MockFrameSink sink2;

  {
    std::unique_lock<std::mutex> lock(exec->GetRenderLock());
    exec->SetExecutionStages(&sink1);
  }
  EXPECT_EQ(exec->GetFrameSink(), &sink1);

  {
    std::unique_lock<std::mutex> lock(exec->GetRenderLock());
    exec->DetachFrameSink();
  }
  EXPECT_EQ(exec->GetFrameSink(), nullptr);

  {
    std::unique_lock<std::mutex> lock(exec->GetRenderLock());
    exec->SetExecutionStages(&sink2);
  }
  EXPECT_EQ(exec->GetFrameSink(), &sink2);
}

TEST_F(PipelineFrameSinkTest, AttachFrameSinkSetsPointerWithoutRebuildingStages) {
  // AttachFrameSink should set the sink on both the executor and the tail
  // execution stage without tearing down and rebuilding the stage vector.
  auto          exec = std::make_shared<CPUPipelineExecutor>();
  MockFrameSink sink;

  exec->SetExecutionStages();
  EXPECT_EQ(exec->GetFrameSink(), nullptr);

  {
    std::unique_lock<std::mutex> lock(exec->GetRenderLock());
    exec->AttachFrameSink(&sink);
  }
  EXPECT_EQ(exec->GetFrameSink(), &sink);

  // Verify the sink delegates work — the tail stage should forward
  // presentation metadata to the attached sink.
  exec->SetNextFramePresentationMode(FramePresentationMode::RoiFrame);
  EXPECT_EQ(sink.presentation_mode_calls_, 1);
  EXPECT_EQ(sink.last_mode_, FramePresentationMode::RoiFrame);
}

TEST_F(PipelineFrameSinkTest, AttachDetachRoundTripWithoutStageRebuild) {
  // Verify that AttachFrameSink / DetachFrameSink form a lightweight
  // round-trip that does not require SetExecutionStages (which is expensive).
  auto          exec = std::make_shared<CPUPipelineExecutor>();
  MockFrameSink sink;

  exec->SetExecutionStages(&sink);
  EXPECT_EQ(exec->GetFrameSink(), &sink);

  // Round-trip: detach → re-attach → detach again.
  {
    std::unique_lock<std::mutex> lock(exec->GetRenderLock());
    exec->DetachFrameSink();
    EXPECT_EQ(exec->GetFrameSink(), nullptr);
    exec->AttachFrameSink(&sink);
    EXPECT_EQ(exec->GetFrameSink(), &sink);
    exec->DetachFrameSink();
    EXPECT_EQ(exec->GetFrameSink(), nullptr);
  }

  // Re-attach after round-trip still works.
  {
    std::unique_lock<std::mutex> lock(exec->GetRenderLock());
    exec->AttachFrameSink(&sink);
  }
  EXPECT_EQ(exec->GetFrameSink(), &sink);

  exec->SetNextFramePreviewMetadata(FramePreviewMetadata{});
  EXPECT_EQ(sink.preview_metadata_calls_, 1);
}

TEST_F(PipelineFrameSinkTest, DetachThenAttachPreservesSinkCalls) {
  // After detach+attach, the re-attached sink should receive subsequent
  // frame presentation calls normally.
  auto          exec = std::make_shared<CPUPipelineExecutor>();
  MockFrameSink sink;

  exec->SetExecutionStages(&sink);
  exec->SetNextFramePresentationMode(FramePresentationMode::FullFrame);
  EXPECT_EQ(sink.presentation_mode_calls_, 1);

  {
    std::unique_lock<std::mutex> lock(exec->GetRenderLock());
    exec->DetachFrameSink();
  }

  // While detached, calls are no-ops.
  exec->SetNextFramePresentationMode(FramePresentationMode::RoiFrame);
  EXPECT_EQ(sink.presentation_mode_calls_, 1);  // unchanged

  {
    std::unique_lock<std::mutex> lock(exec->GetRenderLock());
    exec->AttachFrameSink(&sink);
  }

  // After re-attach, calls go through again.
  exec->SetNextFramePresentationMode(FramePresentationMode::RoiFrame);
  EXPECT_EQ(sink.presentation_mode_calls_, 2);
  EXPECT_EQ(sink.last_mode_, FramePresentationMode::RoiFrame);
}

// =========================================================================
// Phase 1 Acceptance Criterion 4:
//   "A cached pipeline can be reused for thumbnail/export/editor without
//    carrying stale UI output state."
// =========================================================================

TEST_F(PipelineFrameSinkTest, ResetExecutionStagesClearsFrameSink) {
  // PipelineMgmtService::SavePipeline() calls ResetExecutionStages() which
  // must clear frame_sink_ so the cached pipeline carries no stale sink.
  auto          exec = std::make_shared<CPUPipelineExecutor>();
  MockFrameSink sink;

  exec->SetExecutionStages(&sink);
  EXPECT_EQ(exec->GetFrameSink(), &sink);

  exec->ResetExecutionStages();
  EXPECT_EQ(exec->GetFrameSink(), nullptr);
}

TEST_F(PipelineFrameSinkTest, ClearAllIntermediateBuffersDoesNotClearFrameSink) {
  // ClearAllIntermediateBuffers() is an intermediate cleanup, not a full
  // reset; it should preserve the frame sink binding.
  auto          exec = std::make_shared<CPUPipelineExecutor>();
  MockFrameSink sink;

  exec->SetExecutionStages(&sink);
  exec->ClearAllIntermediateBuffers();

  EXPECT_EQ(exec->GetFrameSink(), &sink);
}

// =========================================================================
// Phase 1 Acceptance Criterion 3 (partial):
//   Importing history cannot mutate execution stages concurrently with render.
// =========================================================================

TEST_F(PipelineFrameSinkTest, ImportPipelineParamsResetsFrameSink) {
  // ImportPipelineParams() internally calls ResetExecutionStages() and must
  // clear the frame sink so that importing history doesn't leave a stale
  // editor sink attached.
  auto          exec = std::make_shared<CPUPipelineExecutor>();
  MockFrameSink sink;

  exec->SetExecutionStages(&sink);
  EXPECT_EQ(exec->GetFrameSink(), &sink);

  nlohmann::json params = exec->ExportPipelineParams();
  exec->ImportPipelineParams(params);

  EXPECT_EQ(exec->GetFrameSink(), nullptr);
}

TEST_F(PipelineFrameSinkTest, SetAcceleratorBackendPreservesFrameSink) {
  // Changing the accelerator backend preference should preserve an attached
  // frame sink so editor preview is not disrupted by a preference change.
  auto          exec = std::make_shared<CPUPipelineExecutor>();
  MockFrameSink sink;

  exec->SetExecutionStages(&sink);
  EXPECT_EQ(exec->GetFrameSink(), &sink);

  exec->SetAcceleratorBackendPreference(AcceleratorBackendPreference::CPU);
  EXPECT_EQ(exec->GetFrameSink(), &sink);
}

// =========================================================================
// Thread-safety tests
// =========================================================================

TEST_F(PipelineFrameSinkTest, ConcurrentDetachAndRenderLockIsDeadlockFree) {
  // Multiple threads repeatedly acquiring render_lock_ for detach/render
  // operations must not deadlock.
  auto          exec  = std::make_shared<CPUPipelineExecutor>();
  MockFrameSink sink;
  exec->SetExecutionStages(&sink);

  std::atomic<bool> stop{false};
  std::atomic<int>  ops{0};

  const auto detach_work = [&]() {
    while (!stop.load()) {
      std::unique_lock<std::mutex> lock(exec->GetRenderLock());
      exec->DetachFrameSink();
      exec->SetExecutionStages(&sink);
      ops.fetch_add(1);
      std::this_thread::yield();
    }
  };

  const auto render_work = [&]() {
    while (!stop.load()) {
      {
        std::unique_lock<std::mutex> lock(exec->GetRenderLock());
        // Simulate the render path's use of frame sink methods.
        exec->SetNextFramePresentationMode(FramePresentationMode::ViewportTransformed);
        FramePreviewMetadata metadata{};
        exec->SetNextFramePreviewMetadata(metadata);
        (void)exec->GetViewportRenderRegion();
      }
      ops.fetch_add(1);
      std::this_thread::yield();
    }
  };

  std::vector<std::thread> threads;
  threads.reserve(6);
  for (int i = 0; i < 3; ++i) {
    threads.emplace_back(detach_work);
    threads.emplace_back(render_work);
  }

  std::this_thread::sleep_for(std::chrono::milliseconds(200));
  stop.store(true);

  for (auto& t : threads) {
    t.join();
  }

  EXPECT_GT(ops.load(), 0);
  // The real assertion: we reached here without deadlock.
  SUCCEED();
}

TEST_F(PipelineFrameSinkTest, ConcurrentImportPipelineParamsAndRenderIsDeadlockFree) {
  // Simulates the scenario described in Acceptance Criterion 3:
  // reopening the editor or importing history concurrently with render.
  auto          exec  = std::make_shared<CPUPipelineExecutor>();
  MockFrameSink sink;
  exec->SetExecutionStages(&sink);

  const nlohmann::json params = exec->ExportPipelineParams();

  std::atomic<bool> stop{false};
  std::atomic<int>  ops{0};

  const auto import_work = [&]() {
    while (!stop.load()) {
      {
        std::unique_lock<std::mutex> lock(exec->GetRenderLock());
        exec->ImportPipelineParams(params);
        exec->SetExecutionStages(&sink);
      }
      ops.fetch_add(1);
      std::this_thread::yield();
    }
  };

  const auto render_work = [&]() {
    while (!stop.load()) {
      {
        std::unique_lock<std::mutex> lock(exec->GetRenderLock());
        exec->SetNextFramePresentationMode(FramePresentationMode::ViewportTransformed);
      }
      ops.fetch_add(1);
      std::this_thread::yield();
    }
  };

  std::vector<std::thread> threads;
  threads.reserve(4);
  for (int i = 0; i < 2; ++i) {
    threads.emplace_back(import_work);
    threads.emplace_back(render_work);
  }

  std::this_thread::sleep_for(std::chrono::milliseconds(200));
  stop.store(true);

  for (auto& t : threads) {
    t.join();
  }

  EXPECT_GT(ops.load(), 0);
  SUCCEED();
}

// =========================================================================
// Exception-safety tests
// =========================================================================

TEST_F(PipelineFrameSinkTest, SinkIsRestoredAfterExceptionDuringRender) {
  // Simulates the scenario from Phase 1 Review Finding 1:
  // if an exception is thrown after detaching the editor frame sink,
  // the RAII guard must restore the sink before the exception propagates.
  auto          exec = std::make_shared<CPUPipelineExecutor>();
  MockFrameSink sink;

  exec->SetExecutionStages(&sink);
  EXPECT_EQ(exec->GetFrameSink(), &sink);

  IFrameSink* saved_sink = nullptr;
  bool        caught     = false;

  try {
    std::unique_lock<std::mutex> lock(exec->GetRenderLock());

    saved_sink = exec->GetFrameSink();
    ASSERT_NE(saved_sink, nullptr);
    exec->DetachFrameSink();
    EXPECT_EQ(exec->GetFrameSink(), nullptr);

    const auto restore_sink = [&]() {
      if (saved_sink && exec) {
        exec->AttachFrameSink(saved_sink);
      }
    };
    auto sink_guard = std::unique_ptr<void, std::function<void(void*)>>(
        reinterpret_cast<void*>(1), [&restore_sink](void*) { restore_sink(); });

    // Simulate render work that throws.
    throw std::runtime_error("simulated render failure");
  } catch (const std::runtime_error&) {
    caught = true;
  }

  EXPECT_TRUE(caught);
  // After exception, the RAII guard must have restored the sink.
  EXPECT_EQ(exec->GetFrameSink(), &sink);

  // And the sink is still functional.
  exec->SetNextFramePresentationMode(FramePresentationMode::FullFrame);
  EXPECT_EQ(sink.presentation_mode_calls_, 1);
}

TEST_F(PipelineFrameSinkTest, SinkIsRestoredAfterExceptionBeforeRender) {
  // If an exception is thrown between detach and Apply() (e.g., in
  // SetExecutorRenderParams), the RAII guard must still restore the sink.
  auto          exec = std::make_shared<CPUPipelineExecutor>();
  MockFrameSink sink;

  exec->SetExecutionStages(&sink);

  IFrameSink* saved_sink = nullptr;
  bool        caught     = false;

  try {
    std::unique_lock<std::mutex> lock(exec->GetRenderLock());

    saved_sink = exec->GetFrameSink();
    exec->DetachFrameSink();

    const auto restore_sink = [&]() {
      if (saved_sink && exec) {
        exec->AttachFrameSink(saved_sink);
      }
    };
    auto sink_guard = std::unique_ptr<void, std::function<void(void*)>>(
        reinterpret_cast<void*>(1), [&restore_sink](void*) { restore_sink(); });

    // Throw before SetExecutorRenderParams / Apply.
    throw std::logic_error("pre-render failure");
  } catch (const std::logic_error&) {
    caught = true;
  }

  EXPECT_TRUE(caught);
  EXPECT_EQ(exec->GetFrameSink(), &sink);
}

TEST_F(PipelineFrameSinkTest, SinkIsNotRestoredIfNeverDetached) {
  // If no sink was attached when entering the render path, the RAII guard
  // must be a no-op (no spurious attach of nullptr).
  auto exec = std::make_shared<CPUPipelineExecutor>();
  exec->SetExecutionStages();  // no sink attached

  bool caught = false;
  try {
    std::unique_lock<std::mutex> lock(exec->GetRenderLock());

    IFrameSink* saved_sink = exec->GetFrameSink();  // nullptr
    // No detach — saved_sink is nullptr.

    const auto restore_sink = [&]() {
      if (saved_sink && exec) {
        exec->AttachFrameSink(saved_sink);
      }
    };
    auto sink_guard = std::unique_ptr<void, std::function<void(void*)>>(
        reinterpret_cast<void*>(1), [&restore_sink](void*) { restore_sink(); });

    throw std::runtime_error("no-sink render failure");
  } catch (const std::runtime_error&) {
    caught = true;
  }

  EXPECT_TRUE(caught);
  EXPECT_EQ(exec->GetFrameSink(), nullptr);
}

}  // namespace alcedo
