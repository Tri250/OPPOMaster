//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "benchmark_framework.hpp"

#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>

#include <algorithm>
#include <atomic>
#include <cmath>
#include <cstdint>
#include <functional>
#include <iostream>
#include <memory>
#include <mutex>
#include <numeric>
#include <random>
#include <string>
#include <thread>
#include <vector>

namespace alcedo::bench {

// ============================================================================
// Memory Leak Detection for Common Operations
// ============================================================================

/// Run a function repeatedly and check if memory grows without bound.
/// Returns true if a potential leak is detected (RSS grows by more than
/// threshold_mb across the iterations).
auto DetectMemoryLeak(const std::string& name,
                       std::function<void()> operation,
                       size_t iterations = 100,
                       double threshold_mb = 10.0) -> bool {
  std::cout << "  Leak test: " << name << " (" << iterations << " iterations)..." << std::endl;

  // Warmup: let the allocator stabilize
  for (size_t i = 0; i < 5; ++i) {
    operation();
  }

  MemorySnapshot snap_before = CaptureMemorySnapshot();

  for (size_t i = 0; i < iterations; ++i) {
    operation();
  }

  MemorySnapshot snap_after = CaptureMemorySnapshot();

  size_t delta = 0;
  if (snap_after.rss_bytes > snap_before.rss_bytes) {
    delta = snap_after.rss_bytes - snap_before.rss_bytes;
  }

  double delta_mb = static_cast<double>(delta) / (1024.0 * 1024.0);
  bool leak_detected = delta_mb > threshold_mb;

  std::cout << "    RSS before: " << FormatBytes(snap_before.rss_bytes)
            << "  after: " << FormatBytes(snap_after.rss_bytes)
            << "  delta: " << FormatBytes(delta)
            << " (" << std::fixed << std::setprecision(2) << delta_mb << " MB)"
            << (leak_detected ? " *** POTENTIAL LEAK ***" : " OK")
            << std::endl;

  return leak_detected;
}

// ============================================================================
// Benchmark: Image Decode/Encode Cycle Leak
// ============================================================================

void BenchmarkDecodeEncodeLeak() {
  // Create a small test image encoded as JPEG
  cv::Mat img(1000, 1000, CV_8UC3);
  std::mt19937 gen(42);
  std::uniform_int_distribution<int> dist(0, 255);
  for (int y = 0; y < img.rows; ++y) {
    auto* row = img.ptr<cv::Vec3b>(y);
    for (int x = 0; x < img.cols; ++x) {
      row[x] = cv::Vec3b(
          static_cast<uchar>(dist(gen)),
          static_cast<uchar>(dist(gen)),
          static_cast<uchar>(dist(gen)));
    }
  }

  std::vector<uchar> jpeg_buf;
  std::vector<int> params = {cv::IMWRITE_JPEG_QUALITY, 90};
  cv::imencode(".jpg", img, jpeg_buf, params);

  DetectMemoryLeak("JPEG decode/encode cycle", [&]() {
    cv::Mat decoded = cv::imdecode(jpeg_buf, cv::IMREAD_COLOR);
    std::vector<uchar> reencoded;
    cv::imencode(".jpg", decoded, reencoded, params);
  }, 200, 5.0);
}

// ============================================================================
// Benchmark: Image Processing Pipeline Leak
// ============================================================================

void BenchmarkPipelineLeak() {
  cv::Mat img(2000, 3000, CV_32FC3);
  std::mt19937 gen(42);
  std::uniform_real_distribution<float> dist(0.0f, 1.0f);
  for (int y = 0; y < img.rows; ++y) {
    auto* row = img.ptr<cv::Vec3f>(y);
    for (int x = 0; x < img.cols; ++x) {
      row[x] = cv::Vec3f(dist(gen), dist(gen), dist(gen));
    }
  }

  DetectMemoryLeak("Image processing pipeline (resize+convert+filter)", [&]() {
    cv::Mat resized;
    cv::resize(img, resized, cv::Size(1000, 667));

    cv::Mat converted;
    cv::cvtColor(resized, converted, cv::COLOR_RGB2Lab);

    cv::Mat blurred;
    cv::GaussianBlur(converted, blurred, cv::Size(5, 5), 1.5);

    cv::Mat sharpened;
    cv::addWeighted(converted, 1.5, blurred, -0.5, 0.0, sharpened);

    cv::Mat result;
    cv::cvtColor(sharpened, result, cv::COLOR_Lab2RGB);
  }, 200, 5.0);
}

// ============================================================================
// Benchmark: Color Space Conversion Leak
// ============================================================================

void BenchmarkColorConversionLeak() {
  cv::Mat img(2000, 3000, CV_8UC3);
  std::mt19937 gen(42);
  std::uniform_int_distribution<int> dist(0, 255);
  for (int y = 0; y < img.rows; ++y) {
    auto* row = img.ptr<cv::Vec3b>(y);
    for (int x = 0; x < img.cols; ++x) {
      row[x] = cv::Vec3b(
          static_cast<uchar>(dist(gen)),
          static_cast<uchar>(dist(gen)),
          static_cast<uchar>(dist(gen)));
    }
  }

  DetectMemoryLeak("Color space conversion (BGR→RGB→Lab→BGR)", [&]() {
    cv::Mat rgb, lab, bgr;
    cv::cvtColor(img, rgb, cv::COLOR_BGR2RGB);
    cv::cvtColor(rgb, lab, cv::COLOR_RGB2Lab);
    cv::cvtColor(lab, bgr, cv::COLOR_Lab2BGR);
  }, 500, 3.0);
}

// ============================================================================
// Benchmark: Peak Memory Measurement
// ============================================================================

void BenchmarkPeakMemory() {
  BenchmarkRunner runner;
  runner.Run("Peak Memory: Image allocation (10 × 12MP RGBA32F)", [](size_t) {
    std::vector<cv::Mat> images;
    images.reserve(10);
    for (int i = 0; i < 10; ++i) {
      images.emplace_back(4000, 3000, CV_32FC4);
      // Fill with data to ensure physical allocation
      images.back().setTo(cv::Scalar(0.5f, 0.5f, 0.5f, 1.0f));
    }
    // Images go out of scope here; memory should be freed
  });
}

// ============================================================================
// Benchmark: Large Image (100MP) Memory Usage
// ============================================================================

void Benchmark100MPImage() {
  // 100MP image = 10000 × 10000 pixels
  const int dim = 10000;
  const double mp = static_cast<double>(dim) * static_cast<double>(dim) / 1e6;

  std::cout << "\n--- 100MP Image Memory Test ---" << std::endl;
  std::cout << "  Image dimensions: " << dim << " × " << dim
            << " (" << std::fixed << std::setprecision(0) << mp << " MP)" << std::endl;

  MemorySnapshot before = CaptureMemorySnapshot();

  // Allocate as 16-bit (typical for RAW processing)
  {
    std::cout << "  Allocating 100MP 16-bit image..." << std::endl;
    cv::Mat img16(dim, dim, CV_16UC3);
    std::cout << "    Theoretical size: " << FormatBytes(
        static_cast<size_t>(dim) * dim * 3 * 2) << std::endl;

    MemorySnapshot after_alloc = CaptureMemorySnapshot();
    size_t delta = (after_alloc.rss_bytes > before.rss_bytes)
        ? after_alloc.rss_bytes - before.rss_bytes : 0;
    std::cout << "    Actual RSS delta: " << FormatBytes(delta) << std::endl;
  }

  MemorySnapshot after_free = CaptureMemorySnapshot();
  size_t remaining = (after_free.rss_bytes > before.rss_bytes)
      ? after_free.rss_bytes - before.rss_bytes : 0;
  std::cout << "    RSS after free:   " << FormatBytes(remaining)
            << " (should be near 0)" << std::endl;

  // Allocate as 32-bit float (typical for pipeline processing)
  {
    std::cout << "  Allocating 100MP float32 image..." << std::endl;
    cv::Mat img32(dim, dim, CV_32FC3);
    std::cout << "    Theoretical size: " << FormatBytes(
        static_cast<size_t>(dim) * dim * 3 * 4) << std::endl;

    MemorySnapshot after_alloc = CaptureMemorySnapshot();
    size_t delta = (after_alloc.rss_bytes > before.rss_bytes)
        ? after_alloc.rss_bytes - before.rss_bytes : 0;
    std::cout << "    Actual RSS delta: " << FormatBytes(delta) << std::endl;
  }

  MemorySnapshot after_all_free = CaptureMemorySnapshot();
  remaining = (after_all_free.rss_bytes > before.rss_bytes)
      ? after_all_free.rss_bytes - before.rss_bytes : 0;
  std::cout << "    RSS after free:   " << FormatBytes(remaining) << std::endl;

  // Multi-buffer pipeline simulation (source + intermediate + output)
  {
    std::cout << "  Simulating pipeline: source + intermediate + output (100MP)..." << std::endl;
    MemorySnapshot pre = CaptureMemorySnapshot();

    cv::Mat source(dim, dim, CV_32FC4);
    cv::Mat intermediate(dim, dim, CV_32FC4);
    cv::Mat output(dim, dim, CV_8UC4);

    MemorySnapshot post = CaptureMemorySnapshot();
    size_t total_theoretical =
        static_cast<size_t>(dim) * dim * 4 * 4 +  // source: 32FC4
        static_cast<size_t>(dim) * dim * 4 * 4 +   // intermediate: 32FC4
        static_cast<size_t>(dim) * dim * 4 * 1;    // output: 8UC4
    size_t actual_delta = (post.rss_bytes > pre.rss_bytes)
        ? post.rss_bytes - pre.rss_bytes : 0;

    std::cout << "    Theoretical: " << FormatBytes(total_theoretical) << std::endl;
    std::cout << "    Actual RSS:  " << FormatBytes(actual_delta) << std::endl;
    double overhead = static_cast<double>(actual_delta) /
                      static_cast<double>(total_theoretical) * 100.0;
    std::cout << "    Overhead:    " << std::fixed << std::setprecision(1)
              << overhead << "%" << std::endl;
  }
}

// ============================================================================
// Benchmark: Batch Processing Memory Growth
// ============================================================================

void BenchmarkBatchMemoryGrowth() {
  std::cout << "\n--- Batch Processing Memory Growth ---" << std::endl;

  const int image_w = 4000;
  const int image_h = 3000;
  const int batch_sizes[] = {1, 5, 10, 20, 50};

  MemorySnapshot baseline = CaptureMemorySnapshot();
  std::cout << "  Baseline RSS: " << FormatBytes(baseline.rss_bytes) << std::endl;

  for (int batch : batch_sizes) {
    MemorySnapshot pre = CaptureMemorySnapshot();

    // Simulate batch processing: decode → process → encode
    std::vector<cv::Mat> images;
    images.reserve(batch);
    for (int i = 0; i < batch; ++i) {
      // Simulate decode
      cv::Mat img(image_h, image_w, CV_8UC3);
      // Simulate processing
      cv::Mat processed;
      cv::cvtColor(img, processed, cv::COLOR_BGR2RGB);
      // Store result (simulating batch accumulation)
      images.push_back(std::move(processed));
    }

    MemorySnapshot post = CaptureMemorySnapshot();
    size_t delta = (post.rss_bytes > pre.rss_bytes)
        ? post.rss_bytes - pre.rss_bytes : 0;

    // Expected: ~batch * 4000 * 3000 * 3 bytes
    size_t expected = static_cast<size_t>(batch) * image_w * image_h * 3;
    double ratio = (expected > 0) ? static_cast<double>(delta) / static_cast<double>(expected) : 0.0;

    std::cout << "  Batch size " << std::setw(3) << batch
              << ": RSS delta = " << FormatBytes(delta)
              << " (expected ~" << FormatBytes(expected)
              << ", ratio = " << std::fixed << std::setprecision(2) << ratio << ")"
              << std::endl;

    // Release
    images.clear();
  }

  MemorySnapshot final_snap = CaptureMemorySnapshot();
  size_t total_growth = (final_snap.rss_bytes > baseline.rss_bytes)
      ? final_snap.rss_bytes - baseline.rss_bytes : 0;
  std::cout << "  Final RSS growth: " << FormatBytes(total_growth)
            << " (should be near 0 after all batches freed)" << std::endl;
}

// ============================================================================
// Benchmark: Concurrent Memory Pressure
// ============================================================================

void BenchmarkConcurrentMemory() {
  std::cout << "\n--- Concurrent Memory Pressure ---" << std::endl;

  const int num_threads = std::max(1u, std::thread::hardware_concurrency());
  const int images_per_thread = 5;
  const int img_w = 2000;
  const int img_h = 1500;

  std::cout << "  Threads: " << num_threads
            << ", images/thread: " << images_per_thread << std::endl;

  MemorySnapshot before = CaptureMemorySnapshot();

  std::vector<std::thread> threads;
  std::atomic<size_t> total_processed{0};
  std::mutex cout_mutex;

  for (int t = 0; t < num_threads; ++t) {
    threads.emplace_back([&]() {
      for (int i = 0; i < images_per_thread; ++i) {
        // Simulate per-image processing
        cv::Mat img(img_h, img_w, CV_32FC3);
        img.setTo(cv::Scalar(0.5f, 0.5f, 0.5f));

        cv::Mat resized;
        cv::resize(img, resized, cv::Size(1000, 750));

        cv::Mat result;
        cv::cvtColor(resized, result, cv::COLOR_RGB2Lab);
        cv::cvtColor(result, result, cv::COLOR_Lab2RGB);

        total_processed.fetch_add(1, std::memory_order_relaxed);
      }
    });
  }

  for (auto& t : threads) {
    t.join();
  }

  MemorySnapshot after = CaptureMemorySnapshot();
  size_t delta = (after.rss_bytes > before.rss_bytes)
      ? after.rss_bytes - before.rss_bytes : 0;

  std::cout << "  Processed: " << total_processed.load() << " images" << std::endl;
  std::cout << "  RSS delta: " << FormatBytes(delta) << std::endl;
  std::cout << "  Peak RSS:  " << FormatBytes(after.peak_rss_bytes) << std::endl;
}

// ============================================================================
// Main
// ============================================================================

}  // namespace alcedo::bench

auto main(int argc, char* argv[]) -> int {
  using namespace alcedo::bench;

  std::cout << "========================================================" << std::endl;
  std::cout << "  AlcedoStudio Memory Benchmarks" << std::endl;
  std::cout << "========================================================" << std::endl;
  std::cout << std::endl;

  // --- Leak Detection ---
  std::cout << "=== Leak Detection ===" << std::endl;
  BenchmarkDecodeEncodeLeak();
  BenchmarkPipelineLeak();
  BenchmarkColorConversionLeak();

  // --- Peak Memory ---
  std::cout << "\n=== Peak Memory ===" << std::endl;
  BenchmarkPeakMemory();

  // --- 100MP Image ---
  Benchmark100MPImage();

  // --- Batch Processing Memory Growth ---
  BenchmarkBatchMemoryGrowth();

  // --- Concurrent Memory ---
  BenchmarkConcurrentMemory();

  std::cout << "\n========================================================" << std::endl;
  std::cout << "  Memory benchmarks complete." << std::endl;
  std::cout << "========================================================" << std::endl;

  return 0;
}
