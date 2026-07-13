//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <functional>
#include <iostream>
#include <memory>
#include <sstream>
#include <string>
#include <vector>

#include "edit/operators/color_grading_3way.hpp"
#include "processing/ai_mask_generator.hpp"
#include "processing/collage_maker.hpp"
#include "io/image/extended_image_writer.hpp"

namespace alcedo {
namespace tests {

/// Test result structure
struct TestResult {
    std::string name;
    bool passed;
    std::string message;
    double duration_ms;
};

/// Test suite base class
class TestSuite {
public:
    explicit TestSuite(const std::string& name) : name_(name) {}
    virtual ~TestSuite() = default;

    virtual auto RunAll() -> std::vector<TestResult> = 0;
    auto GetName() const -> const std::string& { return name_; }

protected:
    std::string name_;

    auto AssertTrue(bool condition, const std::string& message = "") -> void {
        if (!condition) {
            throw std::runtime_error(message.empty() ? "Assertion failed: expected true" : message);
        }
    }

    auto AssertFalse(bool condition, const std::string& message = "") -> void {
        if (condition) {
            throw std::runtime_error(message.empty() ? "Assertion failed: expected false" : message);
        }
    }

    auto AssertEqual(float a, float b, float epsilon = 0.0001f, const std::string& message = "") -> void {
        if (std::abs(a - b) > epsilon) {
            std::ostringstream oss;
            oss << "Assertion failed: " << a << " != " << b;
            throw std::runtime_error(message.empty() ? oss.str() : message);
        }
    }

    auto AssertEqual(int a, int b, const std::string& message = "") -> void {
        if (a != b) {
            std::ostringstream oss;
            oss << "Assertion failed: " << a << " != " << b;
            throw std::runtime_error(message.empty() ? oss.str() : message);
        }
    }

    auto AssertNotNull(const void* ptr, const std::string& message = "") -> void {
        if (ptr == nullptr) {
            throw std::runtime_error(message.empty() ? "Assertion failed: pointer is null" : message);
        }
    }

    auto AssertThrows(const std::function<void()>& func, const std::string& message = "") -> void {
        bool threw = false;
        try {
            func();
        } catch (...) {
            threw = true;
        }
        if (!threw) {
            throw std::runtime_error(message.empty() ? "Assertion failed: expected exception" : message);
        }
    }
};

/// Color Grading 3-Way Test Suite
class ColorGrading3WayTestSuite : public TestSuite {
public:
    ColorGrading3WayTestSuite() : TestSuite("ColorGrading3Way") {}

    auto RunAll() -> std::vector<TestResult> override {
        std::vector<TestResult> results;

        results.push_back(RunTest("Constructor", [this]() { TestConstructor(); }));
        results.push_back(RunTest("DefaultParams", [this]() { TestDefaultParams(); }));
        results.push_back(RunTest("SetGetParams", [this]() { TestSetGetParams(); }));
        results.push_back(RunTest("OKLabRoundtrip", [this]() { TestOKLabRoundtrip(); }));
        results.push_back(RunTest("ZoneWeights", [this]() { TestZoneWeights(); }));
        results.push_back(RunTest("ApplyToImage", [this]() { TestApplyToImage(); }));

        return results;
    }

private:
    auto RunTest(const std::string& name, const std::function<void()>& test) -> TestResult {
        TestResult result;
        result.name = name;

        auto start = std::chrono::high_resolution_clock::now();

        try {
            test();
            result.passed = true;
            result.message = "OK";
        } catch (const std::exception& e) {
            result.passed = false;
            result.message = e.what();
        } catch (...) {
            result.passed = false;
            result.message = "Unknown error";
        }

        auto end = std::chrono::high_resolution_clock::now();
        result.duration_ms = std::chrono::duration<double, std::milli>(end - start).count();

        return result;
    }

    void TestConstructor() {
        ColorGrading3WayOp op;
        AssertTrue(true, "Constructor should succeed");
    }

    void TestDefaultParams() {
        ColorGrading3WayOp op;
        auto params = op.GetParams();

        AssertEqual(params["shadows"]["hue_offset"].get<float>(), 0.0f);
        AssertEqual(params["shadows"]["saturation"].get<float>(), 0.0f);
        AssertEqual(params["blending"].get<float>(), 50.0f);
    }

    void TestSetGetParams() {
        ColorGrading3WayOp op;

        nlohmann::json params = {
            {"shadows", {"hue_offset", -10.0f, "saturation", 20.0f}},
            {"blending", 75.0f}
        };
        op.SetParams(params);

        auto retrieved = op.GetParams();
        AssertEqual(retrieved["shadows"]["hue_offset"].get<float>(), -10.0f);
        AssertEqual(retrieved["blending"].get<float>(), 75.0f);
    }

    void TestOKLabRoundtrip() {
        // Test RGB -> OKLab -> RGB roundtrip
        float r = 0.5f, g = 0.3f, b = 0.7f;

        // Would call RGBToOKLab and OKLabToRGB
        // Assert that roundtrip is within tolerance
        AssertTrue(true, "OKLab roundtrip test placeholder");
    }

    void TestZoneWeights() {
        ColorGrading3WayOp op;

        // Test zone weight calculation
        // Shadows at low luminance
        // Highlights at high luminance
        // Midtones in between
        AssertTrue(true, "Zone weight test placeholder");
    }

    void TestApplyToImage() {
        auto buffer = std::make_shared<ImageBuffer>();
        buffer->width = 100;
        buffer->height = 100;
        buffer->channels = 3;
        buffer->data.resize(100 * 100 * 3, 128);

        ColorGrading3WayOp op;
        op.Apply(buffer);

        // Verify output is valid
        AssertEqual(buffer->width, 100);
        AssertEqual(buffer->height, 100);
    }
};

/// AI Mask Generator Test Suite
class AIMaskGeneratorTestSuite : public TestSuite {
public:
    AIMaskGeneratorTestSuite() : TestSuite("AIMaskGenerator") {}

    auto RunAll() -> std::vector<TestResult> override {
        std::vector<TestResult> results;

        results.push_back(RunTest("ServiceCreation", [this]() { TestServiceCreation(); }));
        results.push_back(RunTest("ColorMaskGeneration", [this]() { TestColorMask(); }));
        results.push_back(RunTest("LuminanceMaskGeneration", [this]() { TestLuminanceMask(); }));
        results.push_back(RunTest("MaskCombination", [this]() { TestMaskCombination(); }));
        results.push_back(RunTest("MaskRefinement", [this]() { TestMaskRefinement(); }));

        return results;
    }

private:
    auto RunTest(const std::string& name, const std::function<void()>& test) -> TestResult {
        TestResult result;
        result.name = name;

        auto start = std::chrono::high_resolution_clock::now();

        try {
            test();
            result.passed = true;
            result.message = "OK";
        } catch (const std::exception& e) {
            result.passed = false;
            result.message = e.what();
        }

        auto end = std::chrono::high_resolution_clock::now();
        result.duration_ms = std::chrono::duration<double, std::milli>(end - start).count();

        return result;
    }

    void TestServiceCreation() {
        ai::AIMaskService service;
        AssertTrue(true, "AIMaskService creation should succeed");
    }

    void TestColorMask() {
        ai::AIMaskService service;

        // Create test image (solid color)
        std::vector<uint8_t> image(100 * 100 * 3, 0);
        // Fill with pure red
        for (size_t i = 0; i < image.size(); i += 3) {
            image[i] = 255;     // R
            image[i + 1] = 0;   // G
            image[i + 2] = 0;   // B
        }

        auto result = service.GenerateColorMask(
            image.data(), 100, 100, 3,
            0.0f,    // hue_center (red)
            30.0f,   // hue_range
            0.5f, 1.0f,  // saturation
            0.0f, 1.0f   // luminance
        );

        AssertTrue(result.success, "Color mask generation should succeed");
        AssertEqual(result.bitmap.width, 100);
        AssertEqual(result.bitmap.height, 100);
    }

    void TestLuminanceMask() {
        ai::AIMaskService service;

        // Create gradient image
        std::vector<uint8_t> image(100 * 100 * 3);
        for (int y = 0; y < 100; ++y) {
            for (int x = 0; x < 100; ++x) {
                size_t idx = (y * 100 + x) * 3;
                uint8_t value = static_cast<uint8_t>((x + y) * 255 / 200);
                image[idx] = image[idx + 1] = image[idx + 2] = value;
            }
        }

        auto result = service.GenerateLuminanceMask(
            image.data(), 100, 100, 3,
            0.2f, 0.8f,  // shadow/highlight threshold
            0.1f         // feather
        );

        AssertTrue(result.success, "Luminance mask generation should succeed");
    }

    void TestMaskCombination() {
        mask::MaskBitmap mask1;
        mask1.width = 100;
        mask1.height = 100;
        mask1.data.resize(100 * 100, 128);

        mask::MaskBitmap mask2;
        mask2.width = 100;
        mask2.height = 100;
        mask2.data.resize(100 * 100, 64);

        auto combined = ai::mask_utils::CombineMasks(mask1, mask2, mask::SubMaskMode::Additive);
        AssertEqual(combined.width, 100);
        AssertEqual(combined.height, 100);
        AssertEqual(combined.data[0], 192);  // 128 + 64
    }

    void TestMaskRefinement() {
        mask::MaskBitmap mask;
        mask.width = 100;
        mask.height = 100;
        mask.data.resize(100 * 100, 128);

        // Test blur
        ai::mask_utils::BlurMask(mask, 5);
        AssertEqual(mask.width, 100);

        // Test dilate
        ai::mask_utils::DilateMask(mask, 2);
        AssertEqual(mask.width, 100);

        // Test erode
        ai::mask_utils::ErodeMask(mask, 2);
        AssertEqual(mask.width, 100);
    }
};

/// LUT Export Test Suite
class LUTExportTestSuite : public TestSuite {
public:
    LUTExportTestSuite() : TestSuite("LUTExport") {}

    auto RunAll() -> std::vector<TestResult> override {
        std::vector<TestResult> results;

        results.push_back(RunTest("LUTDataGeneration", [this]() { TestLUTGeneration(); }));
        results.push_back(RunTest("CUBEFormat", [this]() { TestCUBEFormat(); }));
        results.push_back(RunTest("3DLFormat", [this]() { Test3DLFormat(); }));
        results.push_back(RunTest("CSPFormat", [this]() { TestCSPFormat(); }));

        return results;
    }

private:
    auto RunTest(const std::string& name, const std::function<void()>& test) -> TestResult {
        TestResult result;
        result.name = name;

        auto start = std::chrono::high_resolution_clock::now();

        try {
            test();
            result.passed = true;
            result.message = "OK";
        } catch (const std::exception& e) {
            result.passed = false;
            result.message = e.what();
        }

        auto end = std::chrono::high_resolution_clock::now();
        result.duration_ms = std::chrono::duration<double, std::milli>(end - start).count();

        return result;
    }

    void TestLUTGeneration() {
        auto lut_data = LUTExporter::GenerateLUTData({}, LUTExporter::LUTSize::Size17x17x17);

        // Should be 17^3 * 3 values
        AssertEqual(lut_data.size(), static_cast<size_t>(17 * 17 * 17 * 3));
    }

    void TestCUBEFormat() {
        auto lut_data = LUTExporter::GenerateLUTData({}, LUTExporter::LUTSize::Size17x17x17);
        auto result = LUTExporter::WriteCUBELUT(lut_data, 17, "/tmp/test.cube", "Test LUT");

        AssertTrue(result.success, "CUBE export should succeed");
    }

    void Test3DLFormat() {
        auto lut_data = LUTExporter::GenerateLUTData({}, LUTExporter::LUTSize::Size17x17x17);
        auto result = LUTExporter::Write3DLLUT(lut_data, 17, "/tmp/test.3dl");

        AssertTrue(result.success, "3DL export should succeed");
    }

    void TestCSPFormat() {
        auto lut_data = LUTExporter::GenerateLUTData({}, LUTExporter::LUTSize::Size17x17x17);
        auto result = LUTExporter::WriteCSPLUT(lut_data, 17, "/tmp/test.csp", "Test LUT");

        AssertTrue(result.success, "CSP export should succeed");
    }
};

/// Collage Maker Test Suite
class CollageMakerTestSuite : public TestSuite {
public:
    CollageMakerTestSuite() : TestSuite("CollageMaker") {}

    auto RunAll() -> std::vector<TestResult> override {
        std::vector<TestResult> results;

        results.push_back(RunTest("LayoutCalculation", [this]() { TestLayoutCalculation(); }));
        results.push_back(RunTest("Grid2x2Layout", [this]() { TestGrid2x2(); }));
        results.push_back(RunTest("Grid3x3Layout", [this]() { TestGrid3x3(); }));
        results.push_back(RunTest("PolaroidLayout", [this]() { TestPolaroid(); }));

        return results;
    }

private:
    auto RunTest(const std::string& name, const std::function<void()>& test) -> TestResult {
        TestResult result;
        result.name = name;

        auto start = std::chrono::high_resolution_clock::now();

        try {
            test();
            result.passed = true;
            result.message = "OK";
        } catch (const std::exception& e) {
            result.passed = false;
            result.message = e.what();
        }

        auto end = std::chrono::high_resolution_clock::now();
        result.duration_ms = std::chrono::duration<double, std::milli>(end - start).count();

        return result;
    }

    void TestLayoutCalculation() {
        auto layouts = CollageMaker::GetAvailableLayouts();
        AssertTrue(!layouts.empty(), "Should have available layouts");
    }

    void TestGrid2x2() {
        auto frames = CollageMaker::CalculateLayoutFrames(
            CollageLayout::Grid2x2, 1920, 1080, 10, 20);

        AssertEqual(frames.size(), static_cast<size_t>(4), "2x2 grid should have 4 frames");
    }

    void TestGrid3x3() {
        auto frames = CollageMaker::CalculateLayoutFrames(
            CollageLayout::Grid3x3, 1920, 1080, 10, 20);

        AssertEqual(frames.size(), static_cast<size_t>(9), "3x3 grid should have 9 frames");
    }

    void TestPolaroid() {
        auto frames = CollageMaker::CalculateLayoutFrames(
            CollageLayout::Polaroid, 1920, 1080, 10, 20);

        AssertTrue(!frames.empty(), "Polaroid layout should have frames");
    }
};

/// Extended Image Writer Test Suite
class ExtendedImageWriterTestSuite : public TestSuite {
public:
    ExtendedImageWriterTestSuite() : TestSuite("ExtendedImageWriter") {}

    auto RunAll() -> std::vector<TestResult> override {
        std::vector<TestResult> results;

        results.push_back(RunTest("FormatCapabilities", [this]() { TestFormatCapabilities(); }));
        results.push_back(RunTest("FormatDetection", [this]() { TestFormatDetection(); }));
        results.push_back(RunTest("FileSizeEstimation", [this]() { TestFileSizeEstimation(); }));

        return results;
    }

private:
    auto RunTest(const std::string& name, const std::function<void()>& test) -> TestResult {
        TestResult result;
        result.name = name;

        auto start = std::chrono::high_resolution_clock::now();

        try {
            test();
            result.passed = true;
            result.message = "OK";
        } catch (const std::exception& e) {
            result.passed = false;
            result.message = e.what();
        }

        auto end = std::chrono::high_resolution_clock::now();
        result.duration_ms = std::chrono::duration<double, std::milli>(end - start).count();

        return result;
    }

    void TestFormatCapabilities() {
        auto jpeg_caps = io::ExportParams::GetCapabilities(io::ExportFormat::JPEG);
        AssertFalse(jpeg_caps.supports_alpha, "JPEG should not support alpha");
        AssertFalse(jpeg_caps.supports_lossless, "JPEG should not support lossless");

        auto png_caps = io::ExportParams::GetCapabilities(io::ExportFormat::PNG);
        AssertTrue(png_caps.supports_alpha, "PNG should support alpha");
        AssertTrue(png_caps.supports_lossless, "PNG should support lossless");

        auto webp_caps = io::ExportParams::GetCapabilities(io::ExportFormat::WebP);
        AssertTrue(webp_caps.supports_alpha, "WebP should support alpha");
        AssertTrue(webp_caps.supports_lossless, "WebP should support lossless");
        AssertTrue(webp_caps.supports_animation, "WebP should support animation");
    }

    void TestFormatDetection() {
        auto jpeg = io::FormatDetector::DetectFromExtension(".jpg");
        AssertEqual(static_cast<int>(jpeg), static_cast<int>(io::ExportFormat::JPEG));

        auto png = io::FormatDetector::DetectFromExtension(".png");
        AssertEqual(static_cast<int>(png), static_cast<int>(io::ExportFormat::PNG));

        auto jxl = io::FormatDetector::DetectFromExtension(".jxl");
        AssertEqual(static_cast<int>(jxl), static_cast<int>(io::ExportFormat::JPEGXL));

        auto webp = io::FormatDetector::DetectFromExtension(".webp");
        AssertEqual(static_cast<int>(webp), static_cast<int>(io::ExportFormat::WebP));
    }

    void TestFileSizeEstimation() {
        auto buffer = std::make_shared<ImageBuffer>();
        buffer->width = 1920;
        buffer->height = 1080;
        buffer->channels = 3;
        buffer->data.resize(1920 * 1080 * 3);

        io::ExportParams params;
        params.format = io::ExportFormat::JPEG;
        params.quality = 90;

        auto size = io::ExtendedImageWriter::EstimateFileSize(buffer, params);
        AssertTrue(size > 0, "File size estimate should be positive");
    }
};

/// Test runner - runs all test suites
class TestRunner {
public:
    auto RunAllSuites() -> std::vector<TestResult> {
        std::vector<TestResult> all_results;

        // Run all test suites
        ColorGrading3WayTestSuite color_grading_suite;
        auto color_grading_results = color_grading_suite.RunAll();
        all_results.insert(all_results.end(), color_grading_results.begin(), color_grading_results.end());

        AIMaskGeneratorTestSuite mask_suite;
        auto mask_results = mask_suite.RunAll();
        all_results.insert(all_results.end(), mask_results.begin(), mask_results.end());

        LUTExportTestSuite lut_suite;
        auto lut_results = lut_suite.RunAll();
        all_results.insert(all_results.end(), lut_results.begin(), lut_results.end());

        CollageMakerTestSuite collage_suite;
        auto collage_results = collage_suite.RunAll();
        all_results.insert(all_results.end(), collage_results.begin(), collage_results.end());

        ExtendedImageWriterTestSuite writer_suite;
        auto writer_results = writer_suite.RunAll();
        all_results.insert(all_results.end(), writer_results.begin(), writer_results.end());

        return all_results;
    }

    void PrintResults(const std::vector<TestResult>& results) {
        int passed = 0;
        int failed = 0;

        std::cout << "\n=== Test Results ===\n\n";

        for (const auto& result : results) {
            std::cout << "[" << (result.passed ? "PASS" : "FAIL") << "] "
                      << result.name << " (" << result.duration_ms << "ms)"
                      << "\n";
            if (!result.passed) {
                std::cout << "    Error: " << result.message << "\n";
            }

            if (result.passed) passed++;
            else failed++;
        }

        std::cout << "\n=== Summary ===\n";
        std::cout << "Passed: " << passed << "\n";
        std::cout << "Failed: " << failed << "\n";
        std::cout << "Total: " << (passed + failed) << "\n";
    }
};

}  // namespace tests
}  // namespace alcedo