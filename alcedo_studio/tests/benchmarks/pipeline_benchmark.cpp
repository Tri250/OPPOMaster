//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "benchmark_framework.hpp"

#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/imgcodecs.hpp>

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <iostream>
#include <memory>
#include <random>
#include <string>
#include <vector>

// Conditional includes for project-specific modules
// These are included when the full project is built
#ifdef ALCEDO_FULL_PROJECT
#include "decoders/raw_decoder.hpp"
#include "edit/pipeline/pipeline_service.hpp"
#include "app/offline_ai_service.hpp"
#include "app/export_service.hpp"
#endif

namespace alcedo::bench {

// ============================================================================
// Test Image Generation
// ============================================================================

auto GenerateTestImage(int width, int height, int type = CV_8UC3) -> cv::Mat {
  std::mt19937 gen(42);  // Fixed seed for reproducibility
  cv::Mat img(height, width, type);

  if (type == CV_8UC3) {
    std::uniform_int_distribution<int> dist(0, 255);
    for (int y = 0; y < height; ++y) {
      auto* row = img.ptr<cv::Vec3b>(y);
      for (int x = 0; x < width; ++x) {
        row[x] = cv::Vec3b(
            static_cast<uchar>(dist(gen)),
            static_cast<uchar>(dist(gen)),
            static_cast<uchar>(dist(gen)));
      }
    }
  } else if (type == CV_16UC3) {
    std::uniform_int_distribution<int> dist(0, 65535);
    for (int y = 0; y < height; ++y) {
      auto* row = img.ptr<cv::Vec3w>(y);
      for (int x = 0; x < width; ++x) {
        row[x] = cv::Vec3w(
            static_cast<ushort>(dist(gen)),
            static_cast<ushort>(dist(gen)),
            static_cast<ushort>(dist(gen)));
      }
    }
  } else if (type == CV_32FC3) {
    std::uniform_real_distribution<float> dist(0.0f, 1.0f);
    for (int y = 0; y < height; ++y) {
      auto* row = img.ptr<cv::Vec3f>(y);
      for (int x = 0; x < width; ++x) {
        row[x] = cv::Vec3f(dist(gen), dist(gen), dist(gen));
      }
    }
  } else if (type == CV_32FC4) {
    std::uniform_real_distribution<float> dist(0.0f, 1.0f);
    for (int y = 0; y < height; ++y) {
      auto* row = img.ptr<cv::Vec4f>(y);
      for (int x = 0; x < width; ++x) {
        row[x] = cv::Vec4f(dist(gen), dist(gen), dist(gen), 1.0f);
      }
    }
  }

  return img;
}

auto Megapixels(int width, int height) -> double {
  return static_cast<double>(width) * static_cast<double>(height) / 1e6;
}

// ============================================================================
// Benchmark 1: RAW Decode Time (per megapixel)
// ============================================================================

void BenchmarkRawDecode(BenchmarkRunner& runner) {
#ifdef ALCEDO_FULL_PROJECT
  // Full project benchmark: decode actual RAW files
  // This requires sample RAW files to be available
  std::cout << "Note: RAW decode benchmark requires sample RAW files." << std::endl;
  std::cout << "Place .CR2/.NEF/.ARW files in the test data directory." << std::endl;
#else
  // Standalone benchmark: simulate RAW decode by converting 16-bit Bayer pattern
  auto result = runner.Run("RAW Decode (simulated 16-bit Bayer → RGB)", [](size_t) {
    // Simulate a 24MP Bayer-pattern RAW decode
    // Step 1: Generate a "raw" Bayer image (16-bit, single channel)
    cv::Mat bayer(4000, 6000, CV_16UC1);
    std::mt19937 gen(42);
    std::uniform_int_distribution<int> dist(0, 65535);
    for (int y = 0; y < bayer.rows; ++y) {
      auto* row = bayer.ptr<ushort>(y);
      for (int x = 0; x < bayer.cols; ++x) {
        row[x] = static_cast<ushort>(dist(gen));
      }
    }

    // Step 2: Demosaic using OpenCV (BGGR pattern)
    cv::Mat rgb;
    cv::cvtColor(bayer, rgb, cv::COLOR_BayerBG2RGB);

    // Step 3: Convert to float for pipeline
    cv::Mat rgb_f;
    rgb.convertTo(rgb_f, CV_32FC3, 1.0 / 65535.0);
  });

  result.unit = "megapixel";
  result.unit_scale = Megapixels(6000, 4000);
  BenchmarkRunner::PrintResult(result);
#endif
}

// ============================================================================
// Benchmark 2: Color Space Conversion (per megapixel)
// ============================================================================

void BenchmarkColorSpaceConversion(BenchmarkRunner& runner) {
  // Test various color space conversion operations
  const int w = 4000;
  const int h = 3000;
  const double mp = Megapixels(w, h);

  // sRGB → Linear
  {
    cv::Mat img = GenerateTestImage(w, h, CV_8UC3);
    auto result = runner.Run("Color Space: sRGB → Linear (8-bit)", [&img](size_t) {
      cv::Mat linear;
      img.convertTo(linear, CV_32FC3, 1.0 / 255.0);
      // Apply sRGB inverse gamma (linearize)
      cv::pow(linear, 2.2, linear);
    });
    result.unit = "megapixel";
    result.unit_scale = mp;
    BenchmarkRunner::PrintResult(result);
  }

  // Linear → sRGB
  {
    cv::Mat img = GenerateTestImage(w, h, CV_32FC3);
    auto result = runner.Run("Color Space: Linear → sRGB (float)", [&img](size_t) {
      cv::Mat srgb;
      cv::pow(img, 1.0 / 2.2, srgb);
      srgb.convertTo(srgb, CV_8UC3, 255.0);
    });
    result.unit = "megapixel";
    result.unit_scale = mp;
    BenchmarkRunner::PrintResult(result);
  }

  // BGR → RGB swap
  {
    cv::Mat img = GenerateTestImage(w, h, CV_8UC3);
    auto result = runner.Run("Color Space: BGR → RGB swap", [&img](size_t) {
      cv::Mat rgb;
      cv::cvtColor(img, rgb, cv::COLOR_BGR2RGB);
    });
    result.unit = "megapixel";
    result.unit_scale = mp;
    BenchmarkRunner::PrintResult(result);
  }

  // BGR → Lab
  {
    cv::Mat img = GenerateTestImage(w, h, CV_8UC3);
    auto result = runner.Run("Color Space: BGR → CIE Lab", [&img](size_t) {
      cv::Mat lab;
      cv::cvtColor(img, lab, cv::COLOR_BGR2Lab);
    });
    result.unit = "megapixel";
    result.unit_scale = mp;
    BenchmarkRunner::PrintResult(result);
  }

  // BGR → XYZ
  {
    cv::Mat img = GenerateTestImage(w, h, CV_8UC3);
    auto result = runner.Run("Color Space: BGR → XYZ", [&img](size_t) {
      cv::Mat xyz;
      cv::cvtColor(img, xyz, cv::COLOR_BGR2XYZ);
    });
    result.unit = "megapixel";
    result.unit_scale = mp;
    BenchmarkRunner::PrintResult(result);
  }
}

// ============================================================================
// Benchmark 3: Thumbnail Generation (batch of 100)
// ============================================================================

void BenchmarkThumbnailGeneration(BenchmarkRunner& runner) {
  const int batch_size = 100;
  const int src_w = 6000;
  const int src_h = 4000;
  const int thumb_size = 256;

  // Pre-generate images
  std::vector<cv::Mat> images;
  images.reserve(batch_size);
  for (int i = 0; i < batch_size; ++i) {
    images.push_back(GenerateTestImage(src_w, src_h, CV_8UC3));
  }

  auto result = runner.Run("Thumbnail Generation (100 images, 24MP → 256px)", [&](size_t) {
    for (int i = 0; i < batch_size; ++i) {
      cv::Mat thumb;
      cv::resize(images[i], thumb, cv::Size(thumb_size, thumb_size), 0, 0, cv::INTER_AREA);
    }
  });

  result.unit = "image";
  result.unit_scale = static_cast<double>(batch_size);
  result.custom_metric_name = "Thumbnails/sec";
  result.custom_metric_value = (result.timing_stats.mean_ns > 0.0)
      ? 1e9 * static_cast<double>(batch_size) / result.timing_stats.mean_ns
      : 0.0;
  BenchmarkRunner::PrintResult(result);
}

// ============================================================================
// Benchmark 4: Pipeline Full Render (per megapixel)
// ============================================================================

void BenchmarkPipelineRender(BenchmarkRunner& runner) {
  const int w = 4000;
  const int h = 3000;
  const double mp = Megapixels(w, h);

  // Simulate a full pipeline render: exposure → contrast → tone mapping → output
  cv::Mat input = GenerateTestImage(w, h, CV_32FC4);

  auto result = runner.Run("Pipeline Full Render (exposure+contrast+tonemap)", [&input](size_t) {
    cv::Mat processed = input.clone();

    // Exposure adjustment
    float exposure = 1.5f;
    cv::multiply(processed, cv::Scalar(exposure, exposure, exposure, 1.0), processed);

    // Contrast (simple S-curve)
    cv::subtract(processed, cv::Scalar(0.5, 0.5, 0.5, 0.0), processed);
    cv::pow(processed, 0.8, processed);
    cv::add(processed, cv::Scalar(0.5, 0.5, 0.5, 0.0), processed);

    // Tone mapping (Reinhard)
    std::vector<cv::Mat> channels;
    cv::split(processed, channels);
    cv::Mat luminance;
    cv::addWeighted(channels[0], 0.2126, channels[1], 0.7152, 0.0, luminance);
    cv::addWeighted(luminance, 1.0, channels[2], 0.0722, 0.0, luminance);

    // Reinhard: L_d = L / (1 + L)
    cv::Mat mapped;
    cv::add(luminance, cv::Scalar(1.0), mapped);
    cv::divide(luminance, mapped, mapped);

    // Scale RGB by mapped/original luminance ratio
    cv::Mat ratio;
    cv::divide(mapped, luminance + 1e-6, ratio);
    for (int c = 0; c < 3; ++c) {
      cv::multiply(channels[c], ratio, channels[c]);
    }
    cv::merge(channels, processed);

    // Gamma correction (linear → sRGB approximation)
    cv::pow(processed, 1.0 / 2.2, processed);

    // Convert to 8-bit output
    cv::Mat output;
    processed.convertTo(output, CV_8UC4, 255.0);
  });

  result.unit = "megapixel";
  result.unit_scale = mp;
  BenchmarkRunner::PrintResult(result);
}

// ============================================================================
// Benchmark 5: AI Image Analysis (per image)
// ============================================================================

void BenchmarkAiImageAnalysis(BenchmarkRunner& runner) {
#ifdef ALCEDO_FULL_PROJECT
  // Full project benchmark: run actual AI inference
  std::cout << "Note: AI analysis benchmark requires ONNX models." << std::endl;
#else
  // Standalone benchmark: simulate AI preprocessing pipeline
  const int w = 256;  // Typical model input size
  const int h = 256;

  // Pre-generate test data
  cv::Mat image = GenerateTestImage(4000, 3000, CV_8UC3);

  auto result = runner.Run("AI Image Analysis (preprocessing + simulated inference)", [&image](size_t) {
    // Step 1: Resize to model input
    cv::Mat resized;
    cv::resize(image, resized, cv::Size(w, h));

    // Step 2: Convert to float and normalize
    cv::Mat float_img;
    resized.convertTo(float_img, CV_32FC3, 1.0 / 255.0);

    // Step 3: Normalize with ImageNet mean/std
    float mean[] = {0.485f, 0.456f, 0.406f};
    float std_val[] = {0.229f, 0.224f, 0.225f};
    std::vector<cv::Mat> channels;
    cv::split(float_img, channels);
    for (int c = 0; c < 3; ++c) {
      channels[c] = (channels[c] - mean[c]) / std_val[c];
    }

    // Step 4: Simulate embedding computation (512-dim)
    cv::Mat embedding(1, 512, CV_32F);
    std::mt19937 gen(42);
    std::normal_distribution<float> dist(0.0f, 1.0f);
    for (int i = 0; i < 512; ++i) {
      embedding.at<float>(0, i) = dist(gen);
    }

    // Step 5: L2 normalize
    float norm = 0.0f;
    for (int i = 0; i < 512; ++i) {
      norm += embedding.at<float>(0, i) * embedding.at<float>(0, i);
    }
    norm = std::sqrt(norm);
    if (norm > 1e-6f) {
      embedding /= norm;
    }

    // Step 6: Cosine similarity with cached embeddings (simulated)
    cv::Mat cached(100, 512, CV_32F);
    for (int i = 0; i < 100; ++i) {
      for (int j = 0; j < 512; ++j) {
        cached.at<float>(i, j) = dist(gen);
      }
    }
    // Compute similarities
    cv::Mat similarities;
    cv::gemm(cached, embedding.t(), 1.0, cv::Mat(), 0.0, similarities);
  });

  result.unit = "image";
  result.unit_scale = 1.0;
  BenchmarkRunner::PrintResult(result);
#endif
}

// ============================================================================
// Benchmark 6: Export Time (per megapixel)
// ============================================================================

void BenchmarkExport(BenchmarkRunner& runner) {
  const int w = 4000;
  const int h = 3000;
  const double mp = Megapixels(w, h);

  // JPEG export
  {
    cv::Mat img = GenerateTestImage(w, h, CV_8UC3);
    std::vector<int> params = {cv::IMWRITE_JPEG_QUALITY, 85};

    auto result = runner.Run("Export: JPEG 85% (12MP)", [&img, &params](size_t) {
      std::vector<uchar> buf;
      cv::imencode(".jpg", img, buf, params);
    });

    result.unit = "megapixel";
    result.unit_scale = mp;
    BenchmarkRunner::PrintResult(result);
  }

  // PNG export
  {
    cv::Mat img = GenerateTestImage(w, h, CV_8UC3);
    std::vector<int> params = {cv::IMWRITE_PNG_COMPRESSION, 6};

    auto result = runner.Run("Export: PNG level 6 (12MP)", [&img, &params](size_t) {
      std::vector<uchar> buf;
      cv::imencode(".png", img, buf, params);
    });

    result.unit = "megapixel";
    result.unit_scale = mp;
    BenchmarkRunner::PrintResult(result);
  }

  // TIFF 16-bit export
  {
    cv::Mat img = GenerateTestImage(w, h, CV_16UC3);

    auto result = runner.Run("Export: TIFF 16-bit (12MP)", [&img](size_t) {
      std::vector<uchar> buf;
      cv::imencode(".tiff", img, buf);
    });

    result.unit = "megapixel";
    result.unit_scale = mp;
    BenchmarkRunner::PrintResult(result);
  }
}

// ============================================================================
// Main
// ============================================================================

}  // namespace alcedo::bench

auto main(int argc, char* argv[]) -> int {
  using namespace alcedo::bench;

  BenchmarkConfig config;
  config.warmup_iterations  = 3;
  config.measured_iterations = 20;
  config.track_memory       = true;
  config.verbose            = true;

  BenchmarkRunner runner(config);

  std::cout << "========================================================" << std::endl;
  std::cout << "  AlcedoStudio Pipeline Performance Benchmarks" << std::endl;
  std::cout << "========================================================" << std::endl;
  std::cout << "  Warmup:   " << config.warmup_iterations << " iterations" << std::endl;
  std::cout << "  Measured: " << config.measured_iterations << " iterations" << std::endl;
  std::cout << "  Memory:   " << (config.track_memory ? "enabled" : "disabled") << std::endl;
  std::cout << std::endl;

  std::vector<BenchmarkResult> all_results;

  // Run all benchmarks
  std::cout << "--- 1. RAW Decode ---" << std::endl;
  BenchmarkRawDecode(runner);

  std::cout << "--- 2. Color Space Conversion ---" << std::endl;
  BenchmarkColorSpaceConversion(runner);

  std::cout << "--- 3. Thumbnail Generation ---" << std::endl;
  BenchmarkThumbnailGeneration(runner);

  std::cout << "--- 4. Pipeline Full Render ---" << std::endl;
  BenchmarkPipelineRender(runner);

  std::cout << "--- 5. AI Image Analysis ---" << std::endl;
  BenchmarkAiImageAnalysis(runner);

  std::cout << "--- 6. Export ---" << std::endl;
  BenchmarkExport(runner);

  return 0;
}
