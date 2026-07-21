//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QWidget>

namespace alcedo::ui {

class SpinnerWidget final : public QWidget {
 public:
  enum class State {
    Idle,
    Active,
  };

  explicit SpinnerWidget(QWidget* parent = nullptr);

  void Start();
  void Stop();
  void SetState(State state);

 protected:
  void paintEvent(QPaintEvent*) override;

 private:
  State state_ = State::Idle;
};

}  // namespace alcedo::ui
