//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ui/alcedo_main/editor_dialog/widgets/spinner.hpp"

#include <QPainter>
#include <QPen>

namespace alcedo::ui {

SpinnerWidget::SpinnerWidget(QWidget* parent) : QWidget(parent) {
  setFixedSize(22, 22);
  setAttribute(Qt::WA_TransparentForMouseEvents);
  setAttribute(Qt::WA_TranslucentBackground);
  hide();
}

void SpinnerWidget::Start() {
  SetState(State::Active);
}

void SpinnerWidget::Stop() {
  SetState(State::Idle);
}

void SpinnerWidget::SetState(State state) {
  if (state_ == state) {
    return;
  }
  state_ = state;
  setVisible(state_ == State::Active);
  if (state_ == State::Active) {
    raise();
  }
  update();
}

void SpinnerWidget::paintEvent(QPaintEvent*) {
  if (state_ != State::Active) {
    return;
  }

  QPainter painter(this);
  painter.setRenderHint(QPainter::Antialiasing, true);

  const QRectF r = QRectF(4.0, 4.0, width() - 8.0, height() - 8.0);

  {
    QPen pen(QColor(0xFC, 0xC7, 0x04, 210));
    pen.setWidthF(1.7);
    pen.setCapStyle(Qt::RoundCap);
    painter.setPen(pen);
    painter.drawArc(r, 0 * 16, 360 * 16);
  }

  {
    painter.setPen(Qt::NoPen);
    painter.setBrush(QColor(0xFC, 0xC7, 0x04, 235));
    painter.drawEllipse(QPointF(width() * 0.5, height() * 0.5), 2.6, 2.6);
  }
}

}  // namespace alcedo::ui
