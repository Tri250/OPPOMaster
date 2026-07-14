//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ui/alcedo_main/editor_dialog/widgets/histogram_widget.hpp"

#include <QPainter>
#include <QPen>

#include <array>
#include <algorithm>
#include <cmath>

#include "ui/edit_viewer/edit_viewer.hpp"
#ifdef ALCEDO_HAS_LEGACY_GL_VIEWER
#include "ui/edit_viewer/gl_edit_viewer_surface.hpp"
#include <QByteArray>
#include <QOpenGLContext>
#include <QOpenGLShader>
#include <QOpenGLShaderProgram>
#endif
#ifndef ALCEDO_HAS_LEGACY_GL_VIEWER
#include <QImage>
#endif
#include "ui/alcedo_main/app_theme.hpp"

namespace alcedo::ui {

HistogramWidget::HistogramWidget(QtEditViewer* source_viewer, QWidget* parent)
    :
#ifdef ALCEDO_HAS_LEGACY_GL_VIEWER
      QOpenGLWidget(parent),
#else
      QWidget(parent),
#endif
      source_viewer_(source_viewer) {
  setMinimumHeight(126);
  setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Fixed);
  setAutoFillBackground(false);
}

HistogramWidget::~HistogramWidget() {
#ifdef ALCEDO_HAS_LEGACY_GL_VIEWER
  if (!context()) {
    return;
  }
  makeCurrent();
  CleanupGl();
  doneCurrent();
#endif
}

void HistogramWidget::SetSourceViewer(QtEditViewer* source_viewer) {
  source_viewer_ = source_viewer;
  update();
}

#ifdef ALCEDO_HAS_LEGACY_GL_VIEWER
void HistogramWidget::initializeGL() {
  initializeOpenGLFunctions();
  glDisable(GL_DEPTH_TEST);
  glGenVertexArrays(1, &vao_);
  glBindVertexArray(vao_);
  glBindVertexArray(0);
  gl_ready_ = InitPrograms();
}

void HistogramWidget::paintGL() {
  const float dpr = devicePixelRatioF();
  const int   vw  = std::max(1, static_cast<int>(std::lround(width() * dpr)));
  const int   vh  = std::max(1, static_cast<int>(std::lround(height() * dpr)));
  glViewport(0, 0, vw, vh);
  glClearColor(0.07f, 0.07f, 0.07f, 1.0f);
  glClear(GL_COLOR_BUFFER_BIT);

  auto* source_surface =
      source_viewer_ ? dynamic_cast<IOpenGLEditViewerSurface*>(source_viewer_->GetViewerSurface())
                     : nullptr;
  if (!gl_ready_ || !source_surface || !source_surface->hasHistogramData()) {
    return;
  }

  if (context() && source_surface->context() &&
      !QOpenGLContext::areSharing(context(), source_surface->context())) {
    if (!warned_context_sharing_) {
      qWarning("HistogramWidget disabled: OpenGL contexts are not sharing resources.");
      warned_context_sharing_ = true;
    }
    return;
  }

  const GLuint hist_buffer = source_surface->histogramBufferId();
  const int    bins        = source_surface->histogramBinCount();
  if (hist_buffer == 0 || bins <= 1 || !glIsBuffer(hist_buffer)) {
    return;
  }

  glBindVertexArray(vao_);
  glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, hist_buffer);
  glEnable(GL_BLEND);
  glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

  auto draw_fill = [&](int channel, const QVector4D& color) {
    if (!fill_program_) {
      return;
    }
    fill_program_->bind();
    fill_program_->setUniformValue("uBins", bins);
    fill_program_->setUniformValue("uChannel", channel);
    fill_program_->setUniformValue("uColor", color);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, bins * 2);
    fill_program_->release();
  };

  auto draw_line = [&](int channel, const QVector4D& color) {
    if (!line_program_) {
      return;
    }
    line_program_->bind();
    line_program_->setUniformValue("uBins", bins);
    line_program_->setUniformValue("uChannel", channel);
    line_program_->setUniformValue("uColor", color);
    glLineWidth(1.0f);
    glDrawArrays(GL_LINE_STRIP, 0, bins);
    line_program_->release();
  };

  draw_fill(0, QVector4D(1.0f, 0.20f, 0.20f, 0.30f));
  draw_fill(1, QVector4D(0.20f, 1.0f, 0.20f, 0.28f));
  draw_fill(2, QVector4D(0.20f, 0.45f, 1.0f, 0.28f));

  draw_line(0, QVector4D(1.0f, 0.45f, 0.45f, 0.24f));
  draw_line(1, QVector4D(0.45f, 1.0f, 0.45f, 0.22f));
  draw_line(2, QVector4D(0.45f, 0.68f, 1.0f, 0.22f));

  glDisable(GL_BLEND);
  glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, 0);
  glBindVertexArray(0);
}

auto HistogramWidget::InitPrograms() -> bool {
  if (!context()) {
    return false;
  }

  const auto format = context()->format();
  const bool has_compute_compatible_ssbo =
      (format.majorVersion() > 4 || (format.majorVersion() == 4 && format.minorVersion() >= 3)) ||
      context()->hasExtension(QByteArrayLiteral("GL_ARB_shader_storage_buffer_object"));
  if (!has_compute_compatible_ssbo) {
    qWarning("HistogramWidget disabled: OpenGL SSBO support is not available.");
    return false;
  }

  static const char* kFillVertex = R"(
#version 430 core
layout(std430, binding = 0) readonly buffer HistogramBuffer {
  float hist[];
};
uniform int uBins;
uniform int uChannel;
void main() {
  int bin = gl_VertexID >> 1;
  int top = gl_VertexID & 1;
  float x = (uBins > 1) ? float(bin) / float(uBins - 1) : 0.0;
  float y = (top == 0) ? 0.0 : clamp(hist[uChannel * uBins + bin], 0.0, 1.0);
  gl_Position = vec4(x * 2.0 - 1.0, y * 2.0 - 1.0, 0.0, 1.0);
}
)";

  static const char* kLineVertex = R"(
#version 430 core
layout(std430, binding = 0) readonly buffer HistogramBuffer {
  float hist[];
};
uniform int uBins;
uniform int uChannel;
void main() {
  int bin = gl_VertexID;
  float x = (uBins > 1) ? float(bin) / float(uBins - 1) : 0.0;
  float y = clamp(hist[uChannel * uBins + bin], 0.0, 1.0);
  gl_Position = vec4(x * 2.0 - 1.0, y * 2.0 - 1.0, 0.0, 1.0);
}
)";

  static const char* kFragment   = R"(
#version 430 core
uniform vec4 uColor;
out vec4 FragColor;
void main() {
  FragColor = uColor;
}
)";

  fill_program_                  = new QOpenGLShaderProgram();
  if (!fill_program_->addShaderFromSourceCode(QOpenGLShader::Vertex, kFillVertex) ||
      !fill_program_->addShaderFromSourceCode(QOpenGLShader::Fragment, kFragment) ||
      !fill_program_->link()) {
    qWarning("HistogramWidget fill program failed: %s",
             fill_program_->log().toUtf8().constData());
    CleanupGl();
    return false;
  }

  line_program_ = new QOpenGLShaderProgram();
  if (!line_program_->addShaderFromSourceCode(QOpenGLShader::Vertex, kLineVertex) ||
      !line_program_->addShaderFromSourceCode(QOpenGLShader::Fragment, kFragment) ||
      !line_program_->link()) {
    qWarning("HistogramWidget line program failed: %s",
             line_program_->log().toUtf8().constData());
    CleanupGl();
    return false;
  }
  return true;
}

void HistogramWidget::CleanupGl() {
  if (fill_program_) {
    delete fill_program_;
    fill_program_ = nullptr;
  }
  if (line_program_) {
    delete line_program_;
    line_program_ = nullptr;
  }
  if (vao_) {
    glDeleteVertexArrays(1, &vao_);
    vao_ = 0;
  }
  gl_ready_ = false;
}
#else
void HistogramWidget::ComputeHistogramFromViewer() {
  hist_r_.fill(0.0f);
  hist_g_.fill(0.0f);
  hist_b_.fill(0.0f);
  hist_valid_ = false;

  if (!source_viewer_) {
    return;
  }

  auto* surface = source_viewer_->GetViewerSurface();
  if (!surface || !surface->widget()) {
    return;
  }

  // Grab the current rendered frame from the viewer widget.
  const QImage frame = surface->widget()->grab().toImage().convertedTo(QImage::Format_RGB32);
  if (frame.isNull()) {
    return;
  }

  const int w = frame.width();
  const int h = frame.height();
  if (w <= 0 || h <= 0) {
    return;
  }

  // Sample pixels with a stride to keep computation fast on large images.
  // For images <= 512px on either axis, sample every pixel; otherwise step
  // such that roughly 512 samples are taken along the longer dimension.
  const int stride = std::max(1, std::max(w, h) / 512);

  float max_val = 0.0f;
  for (int y = 0; y < h; y += stride) {
    const QRgb* line = reinterpret_cast<const QRgb*>(frame.constScanLine(y));
    for (int x = 0; x < w; x += stride) {
      const QRgb pixel = line[x];
      const int r = qRed(pixel);
      const int g = qGreen(pixel);
      const int b = qBlue(pixel);
      hist_r_[r] += 1.0f;
      hist_g_[g] += 1.0f;
      hist_b_[b] += 1.0f;
    }
  }

  // Find the global peak for normalization.
  for (int i = 0; i < kHistogramBins; ++i) {
    max_val = std::max({max_val, hist_r_[i], hist_g_[i], hist_b_[i]});
  }

  if (max_val <= 0.0f) {
    return;
  }

  // Normalize to [0, 1].
  for (int i = 0; i < kHistogramBins; ++i) {
    hist_r_[i] /= max_val;
    hist_g_[i] /= max_val;
    hist_b_[i] /= max_val;
  }

  hist_valid_ = true;
}

void HistogramWidget::paintEvent(QPaintEvent*) {
  ComputeHistogramFromViewer();

  QPainter painter(this);
  painter.setRenderHint(QPainter::Antialiasing, true);
  painter.fillRect(rect(), QColor(0x12, 0x12, 0x12));

  if (!hist_valid_) {
    return;
  }

  const QRectF area = QRectF(rect()).adjusted(4.0, 4.0, -4.0, -4.0);
  if (area.width() <= 0 || area.height() <= 0) {
    return;
  }

  const float bin_width = static_cast<float>(area.width() / kHistogramBins);

  // Channel fill colors match the GL path (RGBA: R 1.0,0.20,0.20,0.30;
  // G 0.20,1.0,0.20,0.28; B 0.20,0.45,1.0,0.28).
  auto draw_fill = [&](const std::array<float, kHistogramBins>& hist, const QColor& color) {
    QPainterPath path;
    path.moveTo(area.left(), area.bottom());
    for (int i = 0; i < kHistogramBins; ++i) {
      const qreal x = area.left() + i * bin_width;
      const qreal y = area.bottom() - static_cast<qreal>(hist[i]) * area.height();
      path.lineTo(x, y);
    }
    path.lineTo(area.right(), area.bottom());
    path.closeSubpath();
    painter.fillPath(path, color);
  };

  // Channel line colors match the GL path (RGBA: R 1.0,0.45,0.45,0.24;
  // G 0.45,1.0,0.45,0.22; B 0.45,0.68,1.0,0.22).
  auto draw_line = [&](const std::array<float, kHistogramBins>& hist, const QColor& color) {
    QPainterPath path;
    for (int i = 0; i < kHistogramBins; ++i) {
      const qreal x = area.left() + i * bin_width;
      const qreal y = area.bottom() - static_cast<qreal>(hist[i]) * area.height();
      if (i == 0) {
        path.moveTo(x, y);
      } else {
        path.lineTo(x, y);
      }
    }
    painter.setPen(QPen(color, 1.0));
    painter.drawPath(path);
  };

  draw_fill(hist_r_, QColor(255, 51, 51, 77));
  draw_fill(hist_g_, QColor(51, 255, 51, 71));
  draw_fill(hist_b_, QColor(51, 115, 255, 71));

  draw_line(hist_r_, QColor(255, 115, 115, 61));
  draw_line(hist_g_, QColor(115, 255, 115, 56));
  draw_line(hist_b_, QColor(115, 173, 255, 56));
}
#endif

HistogramRulerWidget::HistogramRulerWidget(int bins, QWidget* parent)
    : QWidget(parent), bins_(std::max(2, bins)) {
  setMinimumHeight(28);
  setMaximumHeight(36);
  setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Fixed);
  setAttribute(Qt::WA_TransparentForMouseEvents);
}

void HistogramRulerWidget::SetBins(int bins) {
  bins_ = std::max(2, bins);
  update();
}

void HistogramRulerWidget::paintEvent(QPaintEvent*) {
  QPainter painter(this);
  painter.setRenderHint(QPainter::Antialiasing, true);
  painter.setRenderHint(QPainter::TextAntialiasing, true);

  painter.setFont(AppTheme::Font(AppTheme::FontRole::DataCaption));

  const QRectF area(10.0, 6.0, std::max(10.0, width() - 20.0), std::max(10.0, height() - 12.0));
  const qreal  baseline_y = area.top() + 2.0;
  const qreal  tick_h     = 7.0;

  painter.setPen(QPen(QColor(0x4A, 0x4A, 0x4A), 1.0));
  painter.drawLine(QPointF(area.left(), baseline_y), QPointF(area.right(), baseline_y));

  constexpr std::array<double, 5> stops = {0.0, 0.25, 0.50, 0.75, 1.0};
  painter.setPen(QPen(QColor(0x6F, 0x6F, 0x6F), 1.0));
  for (const double t : stops) {
    const qreal x = area.left() + t * area.width();
    painter.drawLine(QPointF(x, baseline_y), QPointF(x, baseline_y + tick_h));
  }

  painter.setPen(QColor(0x9A, 0x9A, 0x9A));
  for (const double t : stops) {
    const qreal   x    = area.left() + t * area.width();
    const QString text = QString::number(t, 'f', 2);
    const QRectF  text_rect(x - 20.0, baseline_y + tick_h + 1.0, 40.0, 14.0);
    painter.drawText(text_rect, Qt::AlignHCenter | Qt::AlignTop, text);
  }
}

}  // namespace alcedo::ui
