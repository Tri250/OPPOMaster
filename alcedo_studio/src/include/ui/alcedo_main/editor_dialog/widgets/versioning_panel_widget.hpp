//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QGraphicsOpacityEffect>
#include <QLabel>
#include <QListWidget>
#include <QPushButton>
#include <QStackedWidget>
#include <QString>
#include <QVBoxLayout>
#include <QVariantAnimation>
#include <QWidget>
#include <functional>

#include "ui/alcedo_main/editor_dialog/modules/versioning.hpp"

namespace alcedo::ui {

// A thin resize handle that allows the user to resize the versioning flyout
// panel by dragging its left edge. Installed as an event filter on the flyout.
class FlyoutResizeHandle : public QWidget {
  Q_OBJECT
 public:
  explicit FlyoutResizeHandle(QWidget* flyout, QWidget* parent = nullptr);

 protected:
  void mousePressEvent(QMouseEvent* event) override;
  void mouseMoveEvent(QMouseEvent* event) override;
  void mouseReleaseEvent(QMouseEvent* event) override;

 private:
  QWidget* flyout_;
  bool     dragging_  = false;
  int      start_x_   = 0;
  int      start_w_   = 0;
};

class VersioningPanelWidget final : public QWidget {
 public:
  enum class FlyoutPage : int { History = 0, Versions = 1 };

  static constexpr int kCollapsedWidth = 64;

  struct Callbacks {
    std::function<void()>              undo_last_transaction;
    std::function<void(size_t)>        move_history_cursor;
    std::function<void()>              create_version;
    std::function<void(const QString&)> checkout_version_by_id;
    std::function<QRect()>             viewer_geometry;
  };

  explicit VersioningPanelWidget(QWidget* parent = nullptr);

  void Configure(QWidget* flyout_parent, Callbacks callbacks);
  void Build();
  void RetranslateUi();
  void SetBottomStatusWidget(QWidget* widget);

  auto MakeUiContext() const -> versioning::VersionUiContext;

  auto UndoButton() const -> QPushButton* { return undo_tx_btn_; }
  auto IsCollapsed() const -> bool { return collapsed_; }
  auto IsFlyoutVisible() const -> bool;

  void SetCollapsed(bool collapsed, bool animate = true);
  void OnDialogResized();
  void RefreshVersionLogSelectionStyles();

  // Reset the user-resized width override when the panel collapses.
  void ResetUserWidth();

 protected:
  bool eventFilter(QObject* obj, QEvent* event) override;

 private:
  void BuildRail();
  void BuildFlyout();
  void RefreshCollapseUi();
  void RepositionFlyout();
  void HandleHistoryButtonClicked();
  void HandleVersionsButtonClicked();

  Callbacks callbacks_{};
  QWidget*  flyout_parent_ = nullptr;

  // Rail (this widget hosts the rail directly).
  QWidget*     rail_         = nullptr;
  QVBoxLayout* rail_layout_  = nullptr;
  QPushButton* history_btn_  = nullptr;
  QPushButton* versions_btn_ = nullptr;

  // Floating flyout overlay.
  QWidget*                flyout_                = nullptr;
  QWidget*                flyout_root_           = nullptr;
  QGraphicsOpacityEffect* flyout_opacity_effect_ = nullptr;
  QVariantAnimation*      flyout_anim_           = nullptr;
  QStackedWidget*         pages_stack_           = nullptr;
  QVBoxLayout*            shared_layout_         = nullptr;

  // Page widgets.
  QLabel*      history_status_   = nullptr;
  QPushButton* undo_tx_btn_      = nullptr;
  QListWidget* tx_stack_         = nullptr;
  QPushButton* create_version_btn_ = nullptr;
  QListWidget* version_log_      = nullptr;

  FlyoutResizeHandle* resize_handle_ = nullptr;
  int                 user_width_    = -1;  // User override for flyout width (-1 = auto).

  bool       collapsed_   = true;
  qreal      progress_    = 0.0;
  FlyoutPage active_page_ = FlyoutPage::History;
  bool       built_       = false;
};

}  // namespace alcedo::ui
