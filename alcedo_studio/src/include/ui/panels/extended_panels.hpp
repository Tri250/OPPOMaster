//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QWidget>
#include <QSlider>
#include <QLabel>
#include <QButtonGroup>
#include <QGraphicsView>
#include <QGraphicsEllipseItem>
#include <memory>

#include "edit/operators/color_grading_3way.hpp"
#include "processing/ai_mask_generator.hpp"
#include "processing/collage_maker.hpp"

namespace alcedo {
namespace ui {

/// Color wheel widget for shadows/midtones/highlights
class ColorWheelWidget : public QWidget {
    Q_OBJECT

public:
    explicit ColorWheelWidget(QWidget* parent = nullptr);
    ~ColorWheelWidget() override;

    void SetHue(float hue);
    void SetSaturation(float sat);
    void SetLuminance(float lum);
    void SetZoneName(const QString& name);

    auto GetHue() const -> float;
    auto GetSaturation() const -> float;
    auto GetLuminance() const -> float;

signals:
    void ValuesChanged(float hue, float saturation, float luminance);

protected:
    void paintEvent(QPaintEvent* event) override;
    void mousePressEvent(QMouseEvent* event) override;
    void mouseMoveEvent(QMouseEvent* event) override;
    void mouseReleaseEvent(QMouseEvent* event) override;

private:
    void UpdatePickerPosition();

    float hue_{0.0f};
    float saturation_{0.0f};
    float luminance_{0.0f};
    float master_{0.0f};
    QString zone_name_;
    bool dragging_{false};
    QPointF picker_pos_;
};

/// Luminance slider with zone overlay
class ZoneLuminanceSlider : public QSlider {
    Q_OBJECT

public:
    explicit ZoneLuminanceSlider(QWidget* parent = nullptr);
    ~ZoneLuminanceSlider() override;

    void SetShadowEnd(float value);
    void SetHighlightStart(float value);
    void SetBlending(float value);
    void SetBalance(float value);

protected:
    void paintEvent(QPaintEvent* event) override;

private:
    float shadow_end_{0.33f};
    float highlight_start_{0.66f};
    float blending_{0.5f};
    float balance_{0.0f};
};

/// Complete 3-Way color grading panel
class ColorGrading3WayPanel : public QWidget {
    Q_OBJECT

public:
    explicit ColorGrading3WayPanel(QWidget* parent = nullptr);
    ~ColorGrading3WayPanel() override;

    void SetOperator(std::shared_ptr<ColorGrading3WayOp> op);

signals:
    void ParametersChanged();

private slots:
    void OnShadowsChanged(float hue, float sat, float lum);
    void OnMidtonesChanged(float hue, float sat, float lum);
    void OnHighlightsChanged(float hue, float sat, float lum);
    void OnGlobalChanged(float hue, float sat, float lum);
    void OnBlendingChanged(int value);
    void OnBalanceChanged(int value);
    void OnSaturationGlobalChanged(int value);
    void OnHueGlobalChanged(int value);
    void OnResetClicked();

private:
    void SetupUI();
    void UpdateFromOperator();

    std::shared_ptr<ColorGrading3WayOp> op_;

    ColorWheelWidget* shadows_wheel_;
    ColorWheelWidget* midtones_wheel_;
    ColorWheelWidget* highlights_wheel_;
    ColorWheelWidget* global_wheel_;

    QSlider* blending_slider_;
    QSlider* balance_slider_;
    QSlider* saturation_global_slider_;
    QSlider* hue_global_slider_;
    QSlider* luminance_shadows_slider_;
    QSlider* luminance_midtones_slider_;
    QSlider* luminance_highlights_slider_;

    ZoneLuminanceSlider* zone_preview_slider_;
};

/// AI mask generation panel
class AIMaskPanel : public QWidget {
    Q_OBJECT

public:
    explicit AIMaskPanel(QWidget* parent = nullptr);
    ~AIMaskPanel() override;

    void SetMaskService(std::shared_ptr<ai::AIMaskService> service);

signals:
    void MaskGenerated(const ai::AIMaskResult& result);
    void MaskGenerationProgress(float progress, const QString& stage);

private slots:
    void OnMaskTypeChanged(int type);
    void OnGenerateClicked();
    void OnRefineClicked();

private:
    void SetupUI();

    std::shared_ptr<ai::AIMaskService> mask_service_;

    QButtonGroup* mask_type_group_;
    QSlider* blur_radius_slider_;
    QSlider* expand_pixels_slider_;
    QSlider* contract_pixels_slider_;

    // Color mask controls
    QSlider* hue_center_slider_;
    QSlider* hue_range_slider_;
    QSlider* saturation_min_slider_;
    QSlider* saturation_max_slider_;
    QSlider* luminance_min_slider_;
    QSlider* luminance_max_slider_;

    // Depth mask controls
    QSlider* depth_near_slider_;
    QSlider* depth_far_slider_;
};

/// Collage maker panel
class CollageMakerPanel : public QWidget {
    Q_OBJECT

public:
    explicit CollageMakerPanel(QWidget* parent = nullptr);
    ~CollageMakerPanel() override;

    void SetCollageMaker(std::shared_ptr<CollageMaker> maker);

signals:
    void CollageGenerated(const std::shared_ptr<ImageBuffer>& result);
    void ExportRequested(const QString& path);

private slots:
    void OnLayoutChanged(int layout);
    void OnCanvasSizeChanged();
    void OnAddImageClicked();
    void OnExportClicked();
    void OnPreviewClicked();

private:
    void SetupUI();
    void UpdatePreview();

    std::shared_ptr<CollageMaker> collage_maker_;

    QComboBox* layout_combo_;
    QSpinBox* canvas_width_spin_;
    QSpinBox* canvas_height_spin_;
    QSlider* spacing_slider_;
    QSlider* margin_slider_;
    QSlider* border_radius_slider_;
    QSlider* border_width_slider_;
    QColorDialog* border_color_picker_;
    QColorDialog* background_color_picker_;

    QLabel* preview_label_;
    QListWidget* frame_list_;
};

/// Extended scopes panel (Vectorscope, RGB Parade, Waveform)
class ExtendedScopesPanel : public QWidget {
    Q_OBJECT

public:
    explicit ExtendedScopesPanel(QWidget* parent = nullptr);
    ~ExtendedScopesPanel() override;

    void UpdateFromImage(const uint8_t* data, int width, int height, int channels);

private slots:
    void OnScopeTypeChanged(int type);
    void OnShowSkinToneChanged(bool show);
    void OnIntensityChanged(int value);

private:
    void SetupUI();
    void UpdateVectorscope();
    void UpdateRGBParade();
    void UpdateWaveform();

    QButtonGroup* scope_type_group_;
    QCheckBox* show_skin_tone_check_;
    QSlider* intensity_slider_;
    QLabel* scope_display_;

    const uint8_t* image_data_;
    int image_width_;
    int image_height_;
    int image_channels_;
};

/// ROI render settings panel
class ROISettingsPanel : public QWidget {
    Q_OBJECT

public:
    explicit ROISettingsPanel(QWidget* parent = nullptr);
    ~ROISettingsPanel() override;

    void SetROIManager(std::shared_ptr<ROIRenderManager> manager);

signals:
    void ROIChanged(int x, int y, int width, int height);

private slots:
    void OnROIModeChanged(bool enabled);
    void OnUpdateFromViewport();

private:
    void SetupUI();

    std::shared_ptr<ROIRenderManager> roi_manager_;

    QCheckBox* enable_roi_check_;
    QLabel* roi_info_label_;
    QLabel* performance_label_;
};

/// Export format selection panel
class ExportFormatPanel : public QWidget {
    Q_OBJECT

public:
    explicit ExportFormatPanel(QWidget* parent = nullptr);
    ~ExportFormatPanel() override;

    void SetExportParams(const io::ExportParams& params);
    auto GetExportParams() const -> io::ExportParams;

signals:
    void FormatChanged(io::ExportFormat format);

private slots:
    void OnFormatChanged(int format);
    void OnQualityChanged(int value);
    void OnLosslessChanged(bool lossless);
    void OnCompressionChanged(int value);

private:
    void SetupUI();
    void UpdateCapabilities();

    QComboBox* format_combo_;
    QSlider* quality_slider_;
    QSlider* compression_slider_;
    QCheckBox* lossless_check_;
    QCheckBox* embed_icc_check_;
    QCheckBox* embed_exif_check_;

    QLabel* format_info_label_;
    QLabel* size_estimate_label_;
};

}  // namespace ui
}  // namespace alcedo