//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "edit/operators/color_grading_3way.hpp"

#include <algorithm>
#include <cmath>
#include <fstream>
#include <iomanip>
#include <sstream>

#include "utils/diagnostics/app_logging.hpp"

namespace alcedo {

// ============================================================================
// OKLab Color Space Conversion (for perceptual color adjustments)
// Based on Björn Ottosson's OKLab color space
// ============================================================================

namespace {

constexpr float kSqrt3 = 1.7320508075688772f;
constexpr float kPi = 3.14159265358979323846f;

// Linear sRGB to OKLab conversion
void RGBToOKLab(float r, float g, float b, float& L, float& a, float& b_oklab) {
    // RGB to linear
    auto srgb_to_linear = [](float c) -> float {
        if (c <= 0.04045f) {
            return c / 12.92f;
        }
        return std::pow((c + 0.055f) / 1.055f, 2.4f);
    };

    float lr = srgb_to_linear(r);
    float lg = srgb_to_linear(g);
    float lb = srgb_to_linear(b);

    // Linear to LMS
    float l = 0.4122214708f * lr + 0.5363380520f * lg + 0.0514459929f * lb;
    float m = 0.2119034982f * lr + 0.6806995451f * lg + 0.1073969566f * lb;
    float s = 0.0883024619f * lr + 0.2817188716f * lg + 0.6299787005f * lb;

    // Non-linearity
    l = std::cbrt(l);
    m = std::cbrt(m);
    s = std::cbrt(s);

    // LMS to OKLab
    L = 0.2104542553f * l + 0.5779946307f * m + 0.0620708871f * s;
    a = 1.9779984951f * l - 2.4358239762f * m + 0.4578644762f * s;
    b_oklab = 0.0259040371f * l + 0.7827717664f * m - 0.8086757660f * s;
}

// OKLab to Linear sRGB conversion
void OKLabToRGB(float L, float a, float b_oklab, float& r, float& g, float& b) {
    // OKLab to LMS
    float l = L + 0.3963377774f * a + 0.2158037573f * b_oklab;
    float m = L - 0.1055613458f * a - 0.0638541728f * b_oklab;
    float s = L - 0.0894841775f * a - 1.2914855480f * b_oklab;

    // Non-linearity (cube)
    l = l * l * l;
    m = m * m * m;
    s = s * s * s;

    // LMS to linear RGB
    float lr = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s;
    float lg = -1.2684380046f * l + 2.6097575309f * m - 0.3413193965f * s;
    float lb = -0.0041960863f * l - 0.7034181814f * m + 1.7076147010f * s;

    // Linear to sRGB
    auto linear_to_srgb = [](float c) -> float {
        if (c <= 0.0031308f) {
            return 12.92f * c;
        }
        return 1.055f * std::pow(c, 1.0f / 2.4f) - 0.055f;
    };

    r = linear_to_srgb(lr);
    g = linear_to_srgb(lg);
    b = linear_to_srgb(lb);

    // Clamp to [0, 1]
    r = std::clamp(r, 0.0f, 1.0f);
    g = std::clamp(g, 0.0f, 1.0f);
    b = std::clamp(b, 0.0f, 1.0f);
}

// Compute hue from OKLab a and b
float OKLabToHue(float a, float b_oklab) {
    float hue = std::atan2(b_oklab, a) * 180.0f / kPi;
    if (hue < 0.0f) hue += 360.0f;
    return hue;
}

// Compute saturation from OKLab
float OKLabToSaturation(float a, float b_oklab) {
    return std::sqrt(a * a + b_oklab * b_oklab);
}

}  // namespace

// ============================================================================
// ColorGrading3WayOp Implementation
// ============================================================================

ColorGrading3WayOp::ColorGrading3WayOp() = default;

ColorGrading3WayOp::ColorGrading3WayOp(const nlohmann::json& params) {
    SetParams(params);
}

void ColorGrading3WayOp::Apply(std::shared_ptr<ImageBuffer> input) {
    if (!input) return;

    const int width = input->width;
    const int height = input->height;

    // Process each pixel
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            const size_t idx = static_cast<size_t>((y * width + x) * 3);

            float r = static_cast<float>(input->data[idx]) / 255.0f;
            float g = static_cast<float>(input->data[idx + 1]) / 255.0f;
            float b = static_cast<float>(input->data[idx + 2]) / 255.0f;

            // Convert to OKLab for perceptual processing
            float L, a_oklab, b_oklab;
            RGBToOKLab(r, g, b, L, a_oklab, b_oklab);

            // Compute luminance zone weights
            const float shadow_weight = ComputeZoneWeight(L, 0);
            const float midtone_weight = ComputeZoneWeight(L, 1);
            const float highlight_weight = ComputeZoneWeight(L, 2);

            // Apply zone-based color adjustments
            float hue_shift = 0.0f;
            float sat_mult = 1.0f;
            float lum_offset = 0.0f;

            // Shadows
            if (shadow_weight > 0.0f) {
                hue_shift += shadows_.hue_offset * shadow_weight;
                sat_mult *= 1.0f + shadows_.saturation * shadow_weight * 0.01f;
                lum_offset += shadows_.luminance * shadow_weight * 0.01f;
            }

            // Midtones
            if (midtone_weight > 0.0f) {
                hue_shift += midtones_.hue_offset * midtone_weight;
                sat_mult *= 1.0f + midtones_.saturation * midtone_weight * 0.01f;
                lum_offset += midtones_.luminance * midtone_weight * 0.01f;
            }

            // Highlights
            if (highlight_weight > 0.0f) {
                hue_shift += highlights_.hue_offset * highlight_weight;
                sat_mult *= 1.0f + highlights_.saturation * highlight_weight * 0.01f;
                lum_offset += highlights_.luminance * highlight_weight * 0.01f;
            }

            // Global adjustments
            hue_shift += global_.hue_offset;
            sat_mult *= 1.0f + global_.saturation * 0.01f;
            lum_offset += global_.luminance * 0.01f;

            // Apply hue shift
            if (std::abs(hue_shift) > 0.001f) {
                float hue = OKLabToHue(a_oklab, b_oklab);
                float sat = OKLabToSaturation(a_oklab, b_oklab);

                hue = std::fmod(hue + hue_shift, 360.0f);
                if (hue < 0.0f) hue += 360.0f;

                const float hue_rad = hue * kPi / 180.0f;
                a_oklab = sat * std::cos(hue_rad);
                b_oklab = sat * std::sin(hue_rad);
            }

            // Apply saturation
            if (std::abs(sat_mult - 1.0f) > 0.001f) {
                a_oklab *= sat_mult;
                b_oklab *= sat_mult;
            }

            // Apply luminance
            L = std::clamp(L + lum_offset, 0.0f, 1.0f);

            // Convert back to RGB
            OKLabToRGB(L, a_oklab, b_oklab, r, g, b);

            // Apply global saturation multiplier
            if (std::abs(saturation_global_ - 100.0f) > 0.001f) {
                const float gray = 0.2126f * r + 0.7152f * g + 0.0722f * b;
                const float sat_factor = saturation_global_ / 100.0f;
                r = gray + (r - gray) * sat_factor;
                g = gray + (g - gray) * sat_factor;
                b = gray + (b - gray) * sat_factor;
            }

            // Apply global hue rotation
            if (std::abs(hue_global_) > 0.001f) {
                float L2, a2, b2;
                RGBToOKLab(r, g, b, L2, a2, b2);

                float hue = OKLabToHue(a2, b2);
                float sat = OKLabToSaturation(a2, b2);

                hue = std::fmod(hue + hue_global_, 360.0f);
                if (hue < 0.0f) hue += 360.0f;

                const float hue_rad = hue * kPi / 180.0f;
                a2 = sat * std::cos(hue_rad);
                b2 = sat * std::sin(hue_rad);

                OKLabToRGB(L2, a2, b2, r, g, b);
            }

            // Store result
            input->data[idx] = static_cast<uint8_t>(r * 255.0f);
            input->data[idx + 1] = static_cast<uint8_t>(g * 255.0f);
            input->data[idx + 2] = static_cast<uint8_t>(b * 255.0f);
        }
    }
}

void ColorGrading3WayOp::ApplyGPU(std::shared_ptr<ImageBuffer> input) {
    // GPU implementation would use CUDA/OpenCL/Metal shaders
    // For now, fall back to CPU implementation
    Apply(input);
}

auto ColorGrading3WayOp::GetParams() const -> nlohmann::json {
    return {
        {"shadows", {
            {"hue_offset", shadows_.hue_offset},
            {"saturation", shadows_.saturation},
            {"luminance", shadows_.luminance},
            {"master", shadows_.master}
        }},
        {"midtones", {
            {"hue_offset", midtones_.hue_offset},
            {"saturation", midtones_.saturation},
            {"luminance", midtones_.luminance},
            {"master", midtones_.master}
        }},
        {"highlights", {
            {"hue_offset", highlights_.hue_offset},
            {"saturation", highlights_.saturation},
            {"luminance", highlights_.luminance},
            {"master", highlights_.master}
        }},
        {"global", {
            {"hue_offset", global_.hue_offset},
            {"saturation", global_.saturation},
            {"luminance", global_.luminance},
            {"master", global_.master}
        }},
        {"blending", blending_},
        {"balance", balance_},
        {"saturation_global", saturation_global_},
        {"hue_global", hue_global_}
    };
}

void ColorGrading3WayOp::SetParams(const nlohmann::json& params) {
    if (params.contains("shadows")) {
        const auto& s = params["shadows"];
        shadows_.hue_offset = s.value("hue_offset", 0.0f);
        shadows_.saturation = s.value("saturation", 0.0f);
        shadows_.luminance = s.value("luminance", 0.0f);
        shadows_.master = s.value("master", 0.0f);
    }

    if (params.contains("midtones")) {
        const auto& m = params["midtones"];
        midtones_.hue_offset = m.value("hue_offset", 0.0f);
        midtones_.saturation = m.value("saturation", 0.0f);
        midtones_.luminance = m.value("luminance", 0.0f);
        midtones_.master = m.value("master", 0.0f);
    }

    if (params.contains("highlights")) {
        const auto& h = params["highlights"];
        highlights_.hue_offset = h.value("hue_offset", 0.0f);
        highlights_.saturation = h.value("saturation", 0.0f);
        highlights_.luminance = h.value("luminance", 0.0f);
        highlights_.master = h.value("master", 0.0f);
    }

    if (params.contains("global")) {
        const auto& g = params["global"];
        global_.hue_offset = g.value("hue_offset", 0.0f);
        global_.saturation = g.value("saturation", 0.0f);
        global_.luminance = g.value("luminance", 0.0f);
        global_.master = g.value("master", 0.0f);
    }

    blending_ = params.value("blending", 50.0f);
    balance_ = params.value("balance", 0.0f);
    saturation_global_ = params.value("saturation_global", 100.0f);
    hue_global_ = params.value("hue_global", 0.0f);
}

void ColorGrading3WayOp::SetGlobalParams(OperatorParams& params) const {
    // Copy to global params for GPU pipeline
    params.color_wheel_enabled_ = true;
}

void ColorGrading3WayOp::EnableGlobalParams(OperatorParams& params, bool enable) {
    params.color_wheel_enabled_ = enable;
}

float ColorGrading3WayOp::ComputeZoneWeight(float luminance, int zone) const {
    // Zone 0: Shadows (low luminance)
    // Zone 1: Midtones (medium luminance)
    // Zone 2: Highlights (high luminance)

    const float blend_factor = blending_ / 100.0f;
    const float balance_shift = balance_ / 100.0f;

    // Define zone boundaries with blending
    const float shadow_start = 0.0f;
    const float shadow_end = 0.33f + balance_shift - blend_factor * 0.15f;
    const float midtone_start = 0.33f + balance_shift - blend_factor * 0.15f;
    const float midtone_end = 0.66f + balance_shift + blend_factor * 0.15f;
    const float highlight_start = 0.66f + balance_shift + blend_factor * 0.15f;
    const float highlight_end = 1.0f;

    switch (zone) {
        case 0: { // Shadows
            if (luminance <= shadow_end) {
                return 1.0f;
            } else if (luminance < midtone_end) {
                return (midtone_end - luminance) / (midtone_end - shadow_end);
            }
            return 0.0f;
        }
        case 1: { // Midtones
            if (luminance >= midtone_start && luminance <= midtone_end) {
                return 1.0f;
            } else if (luminance > shadow_start && luminance < midtone_start) {
                return (luminance - shadow_start) / (midtone_start - shadow_start);
            } else if (luminance > midtone_end && luminance < highlight_end) {
                return (highlight_end - luminance) / (highlight_end - midtone_end);
            }
            return 0.0f;
        }
        case 2: { // Highlights
            if (luminance >= highlight_start) {
                return 1.0f;
            } else if (luminance > midtone_start) {
                return (luminance - midtone_start) / (highlight_start - midtone_start);
            }
            return 0.0f;
        }
    }
    return 0.0f;
}

// ============================================================================
// LUTExporter Implementation
// ============================================================================

auto LUTExporter::GenerateLUTData(
    const std::vector<OperatorType>& operators,
    LUTSize size
) -> std::vector<float> {
    const int dim = GetLUTDimension(size);
    std::vector<float> lut_data(static_cast<size_t>(dim) * dim * dim * 3);

    // Generate identity LUT as base
    for (int b_idx = 0; b_idx < dim; ++b_idx) {
        for (int g_idx = 0; g_idx < dim; ++g_idx) {
            for (int r_idx = 0; r_idx < dim; ++r_idx) {
                const size_t idx = static_cast<size_t>((b_idx * dim * dim + g_idx * dim + r_idx) * 3);
                lut_data[idx] = static_cast<float>(r_idx) / static_cast<float>(dim - 1);
                lut_data[idx + 1] = static_cast<float>(g_idx) / static_cast<float>(dim - 1);
                lut_data[idx + 2] = static_cast<float>(b_idx) / static_cast<float>(dim - 1);
            }
        }
    }

    // ALCEDO_DESIGN_NOTE: Operator application to transform LUT data is not yet implemented.
    // The intended design is to instantiate each operator from `operators`, apply it to every
    // LUT entry point, and produce a baked color transform.  Currently the identity LUT is
    // returned as-is, which means exported LUTs will not reflect the active grade until this
    // is implemented.
    qCDebug(alcedo::diag::appLog,
            "LUTExporter::GenerateLUTData: identity LUT returned — operator application not yet implemented (%zu operators requested)",
            operators.size());

    return lut_data;
}

auto LUTExporter::WriteCUBELUT(
    const std::vector<float>& lut_data,
    int size,
    const std::string& path,
    const std::string& title
) -> ExportResult {
    ExportResult result;

    std::ofstream file(path);
    if (!file.is_open()) {
        result.error_message = "Failed to open file for writing: " + path;
        return result;
    }

    // Write header
    file << "TITLE \"" << title << "\"\n";
    file << "# Created by Alcedo Studio\n";
    file << "# 3D LUT Export\n\n";
    file << "LUT_3D_SIZE " << size << "\n\n";

    // Write data
    file << std::fixed << std::setprecision(6);

    // CUBE format: B G R order (not R G B!)
    for (int b_idx = 0; b_idx < size; ++b_idx) {
        for (int g_idx = 0; g_idx < size; ++g_idx) {
            for (int r_idx = 0; r_idx < size; ++r_idx) {
                const size_t idx = static_cast<size_t>((b_idx * size * size + g_idx * size + r_idx) * 3);
                file << lut_data[idx] << " "     // R
                     << lut_data[idx + 1] << " "  // G
                     << lut_data[idx + 2] << "\n"; // B
            }
        }
    }

    file.close();
    result.success = true;
    result.bytes_written = lut_data.size() * sizeof(float);
    return result;
}

auto LUTExporter::Write3DLLUT(
    const std::vector<float>& lut_data,
    int size,
    const std::string& path
) -> ExportResult {
    ExportResult result;

    std::ofstream file(path);
    if (!file.is_open()) {
        result.error_message = "Failed to open file for writing: " + path;
        return result;
    }

    // 3DL header
    file << "# 3D LUT created by Alcedo Studio\n";
    file << "3DMESH\n";
    file << "Mesh " << size << " " << size << " " << size << "\n";
    file << "LUT16\n\n";

    // Write 16-bit values
    for (size_t i = 0; i < lut_data.size(); i += 3) {
        const uint16_t r = static_cast<uint16_t>(lut_data[i] * 65535.0f);
        const uint16_t g = static_cast<uint16_t>(lut_data[i + 1] * 65535.0f);
        const uint16_t b = static_cast<uint16_t>(lut_data[i + 2] * 65535.0f);

        file << r << " " << g << " " << b << "\n";
    }

    file.close();
    result.success = true;
    return result;
}

auto LUTExporter::WriteCSPLUT(
    const std::vector<float>& lut_data,
    int size,
    const std::string& path,
    const std::string& title
) -> ExportResult {
    ExportResult result;

    std::ofstream file(path);
    if (!file.is_open()) {
        result.error_message = "Failed to open file for writing: " + path;
        return result;
    }

    // CSP header
    file << "CSPLUTV100\n";
    file << "3D\n\n";

    file << "BEGIN METADATA\n";
    file << "Title: " << title << "\n";
    file << "Creator: Alcedo Studio\n";
    file << "END METADATA\n\n";

    file << size << "\n";

    // Write values
    for (size_t i = 0; i < lut_data.size(); ++i) {
        file << static_cast<int>(lut_data[i] * 65535.0f) << "\n";
    }

    file.close();
    result.success = true;
    return result;
}

auto LUTExporter::ExportColorGradeLUT(
    const std::vector<OperatorType>& operators,
    const ExportParams& params,
    const std::string& output_path
) -> ExportResult {
    auto lut_data = GenerateLUTData(operators, params.size);

    switch (params.format) {
        case LUTFormat::CUBE:
            return WriteCUBELUT(lut_data, GetLUTDimension(params.size), output_path, params.title);
        case LUTFormat::3DL:
            return Write3DLLUT(lut_data, GetLUTDimension(params.size), output_path);
        case LUTFormat::CSP:
            return WriteCSPLUT(lut_data, GetLUTDimension(params.size), output_path, params.title);
    }

    return {false, "Unknown LUT format"};
}

// ============================================================================
// ExtendedScopeAnalyzer Implementation
// ============================================================================

auto ExtendedScopeAnalyzer::GenerateVectorscope(
    const uint8_t* image_data,
    int width,
    int height,
    int scope_width,
    int scope_height,
    VectorscopeMode mode
) -> std::vector<uint8_t> {
    std::vector<uint8_t> scope(static_cast<size_t>(scope_width) * scope_height * 4, 0);

    const int center_x = scope_width / 2;
    const int center_y = scope_height / 2;
    const float scale = std::min(scope_width, scope_height) / 2.0f * 0.9f;

    // Accumulate points on vectorscope
    std::vector<float> accumulation(static_cast<size_t>(scope_width) * scope_height, 0.0f);

    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            const size_t idx = static_cast<size_t>((y * width + x) * 3);

            const float r = image_data[idx] / 255.0f;
            const float g = image_data[idx + 1] / 255.0f;
            const float b = image_data[idx + 2] / 255.0f;

            // Convert to YCbCr
            const float Y = 0.299f * r + 0.587f * g + 0.114f * b;
            const float Cb = (b - Y) * 0.565f;
            const float Cr = (r - Y) * 0.713f;

            // Map to vectorscope coordinates
            const int vx = static_cast<int>(center_x + Cr * scale);
            const int vy = static_cast<int>(center_y - Cb * scale);

            if (vx >= 0 && vx < scope_width && vy >= 0 && vy < scope_height) {
                accumulation[vy * scope_width + vx] += 1.0f;
            }
        }
    }

    // Find max for normalization
    float max_val = 0.0f;
    for (const auto& val : accumulation) {
        max_val = std::max(max_val, val);
    }

    // Normalize and convert to RGBA
    for (size_t i = 0; i < accumulation.size(); ++i) {
        const float normalized = max_val > 0 ? accumulation[i] / max_val : 0.0f;
        const uint8_t intensity = static_cast<uint8_t>(normalized * 255.0f);

        const size_t rgba_idx = i * 4;
        scope[rgba_idx] = intensity;     // R
        scope[rgba_idx + 1] = intensity; // G
        scope[rgba_idx + 2] = intensity; // B
        scope[rgba_idx + 3] = 255;       // A
    }

    return scope;
}

auto ExtendedScopeAnalyzer::GenerateRGBParade(
    const uint8_t* image_data,
    int width,
    int height,
    const ParadeConfig& config
) -> std::vector<uint8_t> {
    const int total_width = width;
    const int total_height = config.height * 4; // R, G, B + spacing
    std::vector<uint8_t> parade(static_cast<size_t>(total_width) * total_height * 4, 0);

    // Calculate histograms for each column
    for (int channel = 0; channel < 3; ++channel) {
        const int y_offset = channel * (config.height + config.height / 8);

        // Accumulate values for each column
        std::vector<std::vector<uint32_t>> column_histograms(width);
        for (auto& hist : column_histograms) {
            hist.resize(256, 0);
        }

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                const size_t idx = static_cast<size_t>((y * width + x) * 3 + channel);
                const uint8_t value = image_data[idx];
                column_histograms[x][value]++;
            }
        }

        // Find max count for normalization
        uint64_t max_count = 1;
        for (const auto& hist : column_histograms) {
            for (const auto& count : hist) {
                max_count = std::max(max_count, static_cast<uint64_t>(count));
            }
        }

        // Draw parade
        for (int x = 0; x < width; ++x) {
            for (int val = 0; val < 256; ++val) {
                const float normalized = static_cast<float>(column_histograms[x][val]) / static_cast<float>(max_count);
                if (normalized > 0.01f) {
                    const int py = y_offset + config.height - static_cast<int>(normalized * config.height);
                    if (py >= 0 && py < total_height) {
                        const size_t rgba_idx = static_cast<size_t>((py * total_width + x) * 4);
                        parade[rgba_idx + channel] = static_cast<uint8_t>(normalized * 255.0f);
                        parade[rgba_idx + 3] = 255;
                    }
                }
            }
        }
    }

    return parade;
}

auto ExtendedScopeAnalyzer::GenerateLumaWaveform(
    const uint8_t* image_data,
    int width,
    int height,
    int waveform_width,
    int waveform_height
) -> std::vector<uint8_t> {
    std::vector<uint8_t> waveform(static_cast<size_t>(waveform_width) * waveform_height * 4, 0);

    // Accumulate luminance values for each column
    std::vector<std::vector<float>> columns(waveform_width);
    for (auto& col : columns) {
        col.resize(waveform_height, 0.0f);
    }

    const float x_scale = static_cast<float>(width) / static_cast<float>(waveform_width);

    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            const size_t idx = static_cast<size_t>((y * width + x) * 3);
            const float r = image_data[idx] / 255.0f;
            const float g = image_data[idx + 1] / 255.0f;
            const float b = image_data[idx + 2] / 255.0f;

            const float luma = 0.2126f * r + 0.7152f * g + 0.0722f * b;

            const int wx = static_cast<int>(x / x_scale);
            if (wx >= 0 && wx < waveform_width) {
                const int wy = static_cast<int>((1.0f - luma) * (waveform_height - 1));
                if (wy >= 0 && wy < waveform_height) {
                    columns[wx][wy] += 1.0f;
                }
            }
        }
    }

    // Find max and normalize
    float max_val = 0.0f;
    for (const auto& col : columns) {
        for (const auto& val : col) {
            max_val = std::max(max_val, val);
        }
    }

    // Convert to RGBA
    for (int x = 0; x < waveform_width; ++x) {
        for (int y = 0; y < waveform_height; ++y) {
            const float normalized = max_val > 0 ? columns[x][y] / max_val : 0.0f;
            const uint8_t intensity = static_cast<uint8_t>(normalized * 255.0f);

            const size_t rgba_idx = static_cast<size_t>((y * waveform_width + x) * 4);
            waveform[rgba_idx] = intensity;
            waveform[rgba_idx + 1] = intensity;
            waveform[rgba_idx + 2] = intensity;
            waveform[rgba_idx + 3] = 255;
        }
    }

    return waveform;
}

auto ExtendedScopeAnalyzer::GenerateRGBWaveform(
    const uint8_t* image_data,
    int width,
    int height,
    int waveform_width,
    int waveform_height,
    bool overlay
) -> std::vector<uint8_t> {
    std::vector<uint8_t> waveform(static_cast<size_t>(waveform_width) * waveform_height * 4, 0);

    // Process each channel
    const float x_scale = static_cast<float>(width) / static_cast<float>(waveform_width);

    std::vector<std::vector<std::vector<float>>> channel_data(3);
    for (int c = 0; c < 3; ++c) {
        channel_data[c].resize(waveform_width);
        for (auto& col : channel_data[c]) {
            col.resize(waveform_height, 0.0f);
        }
    }

    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            const size_t idx = static_cast<size_t>((y * width + x) * 3);

            for (int c = 0; c < 3; ++c) {
                const float val = image_data[idx + c] / 255.0f;

                const int wx = static_cast<int>(x / x_scale);
                if (wx >= 0 && wx < waveform_width) {
                    const int wy = static_cast<int>((1.0f - val) * (waveform_height - 1));
                    if (wy >= 0 && wy < waveform_height) {
                        channel_data[c][wx][wy] += 1.0f;
                    }
                }
            }
        }
    }

    // Find max and normalize per channel
    std::vector<float> max_vals(3, 0.0f);
    for (int c = 0; c < 3; ++c) {
        for (const auto& col : channel_data[c]) {
            for (const auto& val : col) {
                max_vals[c] = std::max(max_vals[c], val);
            }
        }
    }

    // Convert to RGBA
    for (int x = 0; x < waveform_width; ++x) {
        for (int y = 0; y < waveform_height; ++y) {
            const size_t rgba_idx = static_cast<size_t>((y * waveform_width + x) * 4);

            for (int c = 0; c < 3; ++c) {
                const float normalized = max_vals[c] > 0 ? channel_data[c][x][y] / max_vals[c] : 0.0f;
                waveform[rgba_idx + c] = static_cast<uint8_t>(normalized * 255.0f);
            }
            waveform[rgba_idx + 3] = 255;
        }
    }

    return waveform;
}

}  // namespace alcedo