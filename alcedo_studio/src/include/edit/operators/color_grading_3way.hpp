//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <string>

#include "edit/operators/op_base.hpp"
#include "image/image_buffer.hpp"
#include "json.hpp"

namespace alcedo {

/// 3-Way Color Grading Wheels - Film-style color grading
/// Inspired by DaVinci Resolve / FilmLight style color grading
class ColorGrading3WayOp : public OperatorBase<ColorGrading3WayOp> {
public:
    /// Individual wheel control for Shadows/Midtones/Highlights
    struct WheelControl {
        /// Hue offset in degrees [-180, 180]
        float hue_offset{0.0f};
        /// Saturation scale [-100, 100]
        float saturation{0.0f};
        /// Luminance offset [-100, 100]
        float luminance{0.0f};
        /// Master offset [-1, 1] applied to RGB equally
        float master{0.0f};
    };

private:
    /// Shadows wheel (dark tones)
    WheelControl shadows_;
    /// Midtones wheel (middle tones)
    WheelControl midtones_;
    /// Highlights wheel (bright tones)
    WheelControl highlights_;
    /// Global wheel (affects entire image)
    WheelControl global_;

    /// Blending between zones [0, 100]
    float blending_{50.0f};
    /// Balance between shadows and highlights [-100, 100]
    float balance_{0.0f};
    /// Global saturation multiplier [0, 200]
    float saturation_global_{100.0f};
    /// Global hue rotation [-180, 180]
    float hue_global_{0.0f};

public:
    static constexpr PriorityLevel     priority_level_    = 5;
    static constexpr PipelineStageName affiliation_stage_ = PipelineStageName::Color_Adjustment;
    static constexpr std::string_view  canonical_name_    = "3-Way Color Grading";
    static constexpr std::string_view  script_name_       = "color_grading_3way";
    static constexpr OperatorType      operator_type_     = OperatorType::COLOR_WHEEL;

    ColorGrading3WayOp();
    ColorGrading3WayOp(const nlohmann::json& params);

    void Apply(std::shared_ptr<ImageBuffer> input) override;
    void ApplyGPU(std::shared_ptr<ImageBuffer> input) override;
    auto GetParams() const -> nlohmann::json override;
    void SetParams(const nlohmann::json& params) override;

    void SetGlobalParams(OperatorParams& params) const override;
    void EnableGlobalParams(OperatorParams& params, bool enable) override;

    // Getters
    auto GetShadows() const -> const WheelControl& { return shadows_; }
    auto GetMidtones() const -> const WheelControl& { return midtones_; }
    auto GetHighlights() const -> const WheelControl& { return highlights_; }
    auto GetGlobal() const -> const WheelControl& { return global_; }
    auto GetBlending() const -> float { return blending_; }
    auto GetBalance() const -> float { return balance_; }

    // Setters
    void SetShadows(const WheelControl& shadows) { shadows_ = shadows; }
    void SetMidtones(const WheelControl& midtones) { midtones_ = midtones; }
    void SetHighlights(const WheelControl& highlights) { highlights_ = highlights; }
    void SetGlobal(const WheelControl& global) { global_ = global; }
    void SetBlending(float blending) { blending_ = blending; }
    void SetBalance(float balance) { balance_ = balance; }

private:
    /// Convert RGB to OKLab for perceptual color adjustments
    static void RGBToOKLab(float r, float g, float b, float& L, float& a, float& b_oklab);
    /// Convert OKLab back to RGB
    static void OKLabToRGB(float L, float a, float b_oklab, float& r, float& g, float& b);
    /// Compute luminance zone weight based on pixel luminance
    float ComputeZoneWeight(float luminance, int zone) const;
};

/// LUT Export Service - Export color grades as 3D LUTs
class LUTExporter {
public:
    /// LUT size options
    enum class LUTSize {
        Size17x17x17,   /// 17^3 = 4913 entries (small, fast)
        Size33x33x33,   /// 33^3 = 35937 entries (medium)
        Size65x65x65,   /// 65^3 = 274625 entries (high quality)
        Size129x129x129 /// 129^3 = 2.1M entries (maximum quality)
    };

    /// LUT format options
    enum class LUTFormat {
        CUBE,   /// Adobe CUBE format (most compatible)
        3DL,    /// Autodesk 3DL format
        CSP     /// Cinespace CSP format
    };

    /// Export parameters
    struct ExportParams {
        LUTSize size = LUTSize::Size33x33x33;
        LUTFormat format = LUTFormat::CUBE;
        std::string title;
        std::string description;
        /// Input color space (e.g., "ACEScg", "Rec709", "LogC")
        std::string input_colorspace;
        /// Output color space
        std::string output_colorspace;
        /// Apply inverse tone mapping before LUT
        bool linearize_input{false};
    };

    /// Export result
    struct ExportResult {
        bool success{false};
        std::string error_message;
        size_t bytes_written{0};
    };

    /// Generate 3D LUT from current color grading operators
    /// @param operators List of operator types to include in LUT
    /// @param params Export parameters
    /// @param output_path File path to write LUT
    /// @return Export result
    static auto ExportColorGradeLUT(
        const std::vector<OperatorType>& operators,
        const ExportParams& params,
        const std::string& output_path
    ) -> ExportResult;

    /// Generate 3D LUT data in memory
    /// @param operators List of operator types to include
    /// @param params Export parameters
    /// @return 3D LUT data as RGB float array
    static auto GenerateLUTData(
        const std::vector<OperatorType>& operators,
        LUTSize size
    ) -> std::vector<float>;

    /// Write CUBE format LUT
    static auto WriteCUBELUT(
        const std::vector<float>& lut_data,
        int size,
        const std::string& path,
        const std::string& title
    ) -> ExportResult;

    /// Write 3DL format LUT
    static auto Write3DLLUT(
        const std::vector<float>& lut_data,
        int size,
        const std::string& path
    ) -> ExportResult;

    /// Write CSP format LUT
    static auto WriteCSPLUT(
        const std::vector<float>& lut_data,
        int size,
        const std::string& path,
        const std::string& title
    ) -> ExportResult;

    /// Get size dimension from LUTSize enum
    static auto GetLUTDimension(LUTSize size) -> int {
        switch (size) {
            case LUTSize::Size17x17x17: return 17;
            case LUTSize::Size33x33x33: return 33;
            case LUTSize::Size65x65x65: return 65;
            case LUTSize::Size129x129x129: return 129;
        }
        return 33;
    }

    /// Get file extension for LUT format
    static auto GetFileExtension(LUTFormat format) -> std::string {
        switch (format) {
            case LUTFormat::CUBE: return ".cube";
            case LUTFormat::3DL: return ".3dl";
            case LUTFormat::CSP: return ".csp";
        }
        return ".cube";
    }
};

/// Scope Analyzer Extensions - Vectorscope and RGB Parade
class ExtendedScopeAnalyzer {
public:
    /// Vectorscope display modes
    enum class VectorscopeMode {
        Color,      /// Standard vectorscope (hue vs saturation)
        SkinTone,   /// Skin tone detection overlay
        Luminance   /// Luminance-weighted vectorscope
    };

    /// RGB Parade configuration
    struct ParadeConfig {
        bool show_red{true};
        bool show_green{true};
        bool show_blue{true};
        bool show_luma{false};
        int height{256};        /// Height of each channel trace
        float scale{1.0f};      /// Vertical scale multiplier
        float intensity{1.0f};  /// Trace intensity
    };

    /// Generate vectorscope data from image
    /// @param image_data RGB image data
    /// @param width Image width
    /// @param height Image height
    /// @param scope_width Output scope width
    /// @param scope_height Output scope height
    /// @param mode Vectorscope mode
    /// @return RGBA vectorscope image data
    static auto GenerateVectorscope(
        const uint8_t* image_data,
        int width,
        int height,
        int scope_width,
        int scope_height,
        VectorscopeMode mode = VectorscopeMode::Color
    ) -> std::vector<uint8_t>;

    /// Generate RGB Parade data from image
    /// @param image_data RGB image data
    /// @param width Image width
    /// @param height Image height
    /// @param config Parade configuration
    /// @return RGBA parade image data
    static auto GenerateRGBParade(
        const uint8_t* image_data,
        int width,
        int height,
        const ParadeConfig& config
    ) -> std::vector<uint8_t>;

    /// Generate luma waveform (combined luminance across horizontal axis)
    static auto GenerateLumaWaveform(
        const uint8_t* image_data,
        int width,
        int height,
        int waveform_width,
        int waveform_height
    ) -> std::vector<uint8_t>;

    /// Generate RGB waveform (separate channels)
    static auto GenerateRGBWaveform(
        const uint8_t* image_data,
        int width,
        int height,
        int waveform_width,
        int waveform_height,
        bool overlay = false
    ) -> std::vector<uint8_t>;
};

}  // namespace alcedo