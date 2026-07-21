//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ui/alcedo_main/editor_dialog/frame/editor_frame_manager.hpp"

#include <mutex>

#include "edit/pipeline/pipeline_cpu.hpp"
#include "edit/scope/final_display_frame_tap.hpp"
#include "ui/edit_viewer/edit_viewer.hpp"
#include "ui/alcedo_main/editor_dialog/controllers/pipeline_controller.hpp"
#include "ui/alcedo_main/editor_dialog/scope/scope_panel.hpp"

namespace alcedo::ui {

EditorFrameManager::~EditorFrameManager() {
  if (auto exec = attached_executor_.lock()) {
    std::unique_lock<std::mutex> lock(exec->GetRenderLock());
    exec->DetachFrameSink();
  }
}

void EditorFrameManager::SetViewer(QtEditViewer* viewer) {
  if (viewer_ == viewer) {
    return;
  }
  // Detach the current sink from the executor under lock before replacing the viewer.
  // This prevents a race where the old viewer/scope-tap is destroyed while a render
  // is still referencing it through the execution stages.
  if (auto exec = attached_executor_.lock()) {
    std::unique_lock<std::mutex> lock(exec->GetRenderLock());
    exec->DetachFrameSink();
  }
  viewer_ = viewer;
  final_display_frame_tap_.reset();
}

void EditorFrameManager::SetScopePanel(ScopePanel* scope_panel) {
  if (scope_panel_ == scope_panel) {
    return;
  }
  if (auto exec = attached_executor_.lock()) {
    std::unique_lock<std::mutex> lock(exec->GetRenderLock());
    exec->DetachFrameSink();
  }
  scope_panel_ = scope_panel;
  final_display_frame_tap_.reset();
}

void EditorFrameManager::EnsureFrameRouting() {
  if (!viewer_) {
    return;
  }
  if (!scope_analyzer_) {
    scope_analyzer_ = CreateDefaultScopeAnalyzer();
  }
  if (final_display_frame_tap_) {
    return;
  }

  final_display_frame_tap_ =
      std::make_unique<FinalDisplayFrameTapSink>(viewer_, scope_analyzer_);
  if (!scope_panel_) {
    return;
  }

  scope_panel_->SetAnalyzer(scope_analyzer_);
  scope_panel_->SetRequestChangedCallback([this](const ScopeRequest& request) {
    if (final_display_frame_tap_) {
      final_display_frame_tap_->SetScopeRequest(request);
    }
  });
  final_display_frame_tap_->SetScopeRequest(scope_panel_->CurrentRequest());
}

void EditorFrameManager::AttachExecutionStages(
    const std::shared_ptr<CPUPipelineExecutor>& exec) {
  EnsureFrameRouting();

  // Detach any previous executor before attaching a new one, and serialise
  // the attach under render_lock_ so that concurrent render tasks never see a
  // partially-updated frame sink or execution-stage configuration.
  if (auto prev_exec = attached_executor_.lock()) {
    if (prev_exec != exec) {
      std::unique_lock<std::mutex> lock(prev_exec->GetRenderLock());
      prev_exec->DetachFrameSink();
    }
  }

  {
    std::unique_lock<std::mutex> lock(exec->GetRenderLock());
    controllers::AttachExecutionStages(exec, CurrentFrameSink());
  }

  attached_executor_ = exec;
}

auto EditorFrameManager::CurrentFrameSink() -> IFrameSink* {
  if (final_display_frame_tap_) {
    return final_display_frame_tap_.get();
  }
  return viewer_;
}

void EditorFrameManager::SyncViewerDisplayEncoding(ColorUtils::ColorSpace encoding_space,
                                                   ColorUtils::EOTF       encoding_eotf,
                                                   float                  peak_luminance) {
  if (!viewer_) {
    return;
  }
  viewer_->SetDisplayEncoding(encoding_space, encoding_eotf, peak_luminance);
}

void EditorFrameManager::MarkNeedsFullFramePreviewAfterGeometryCommit() {
  force_next_full_frame_preview_ = true;
}

void EditorFrameManager::ClearNeedsFullFramePreviewAfterGeometryCommit() {
  force_next_full_frame_preview_ = false;
}

auto EditorFrameManager::NeedsFullFramePreviewAfterGeometryCommit() const -> bool {
  return force_next_full_frame_preview_;
}

auto EditorFrameManager::UseViewportRegionForPanelChange(ControlPanelKind previous_panel,
                                                         ControlPanelKind next_panel) -> bool {
  const bool force_full_frame_preview =
      previous_panel == ControlPanelKind::Geometry && next_panel != ControlPanelKind::Geometry &&
      force_next_full_frame_preview_;
  if (force_full_frame_preview) {
    force_next_full_frame_preview_ = false;
    return false;
  }
  return true;
}

}  // namespace alcedo::ui
