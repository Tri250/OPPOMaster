//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <algorithm>
#include <chrono>
#include <cmath>
#include <cstdint>
#include <functional>
#include <iomanip>
#include <iostream>
#include <numeric>
#include <optional>
#include <sstream>
#include <string>
#include <vector>

#ifdef _WIN32
#ifndef NOMINMAX
#define NOMINMAX
#endif
#include <windows.h>
#include <psapi.h>
#elif defined(__APPLE__)
#include <mach/mach.h>
#include <sys/sysctl.h>
#else
#include <sys/sysinfo.h>
#include <sys/resource.h>
#endif

namespace alcedo::bench {

// ============================================================================
// Statistical Helpers
// ============================================================================

struct BenchmarkStats {
  double min_ns          = 0.0;
  double max_ns          = 0.0;
  double mean_ns         = 0.0;
  double median_ns       = 0.0;
  double p95_ns          = 0.0;
  double p99_ns          = 0.0;
  double stddev_ns       = 0.0;
  double cv_percent      = 0.0;   // coefficient of variation
  size_t iterations      = 0;
  size_t outliers        = 0;
};

auto ComputeStats(std::vector<double> samples) -> BenchmarkStats {
  BenchmarkStats stats;
  if (samples.empty()) return stats;

  const size_t n = samples.size();
  stats.iterations = n;

  std::sort(samples.begin(), samples.end());

  stats.min_ns = samples.front();
  stats.max_ns = samples.back();

  // Mean
  double sum = std::accumulate(samples.begin(), samples.end(), 0.0);
  stats.mean_ns = sum / static_cast<double>(n);

  // Median
  if (n % 2 == 0) {
    stats.median_ns = (samples[n / 2 - 1] + samples[n / 2]) / 2.0;
  } else {
    stats.median_ns = samples[n / 2];
  }

  // Percentiles
  auto percentile = [&samples, n](double p) -> double {
    double idx = p / 100.0 * static_cast<double>(n - 1);
    size_t lo = static_cast<size_t>(std::floor(idx));
    size_t hi = static_cast<size_t>(std::ceil(idx));
    if (hi >= n) hi = n - 1;
    double frac = idx - static_cast<double>(lo);
    return samples[lo] * (1.0 - frac) + samples[hi] * frac;
  };

  stats.p95_ns = percentile(95.0);
  stats.p99_ns = percentile(99.0);

  // Standard deviation
  double sq_sum = 0.0;
  for (double s : samples) {
    double diff = s - stats.mean_ns;
    sq_sum += diff * diff;
  }
  stats.stddev_ns = std::sqrt(sq_sum / static_cast<double>(n));

  // CV
  if (stats.mean_ns > 0.0) {
    stats.cv_percent = (stats.stddev_ns / stats.mean_ns) * 100.0;
  }

  // Outlier detection: values beyond 3 sigma from median
  const double outlier_lo = stats.median_ns - 3.0 * stats.stddev_ns;
  const double outlier_hi = stats.median_ns + 3.0 * stats.stddev_ns;
  stats.outliers = 0;
  for (double s : samples) {
    if (s < outlier_lo || s > outlier_hi) {
      ++stats.outliers;
    }
  }

  return stats;
}

auto FormatNanos(double ns) -> std::string {
  if (ns < 1e3) {
    return std::to_string(static_cast<int64_t>(ns)) + " ns";
  } else if (ns < 1e6) {
    std::ostringstream oss;
    oss << std::fixed << std::setprecision(2) << (ns / 1e3) << " us";
    return oss.str();
  } else if (ns < 1e9) {
    std::ostringstream oss;
    oss << std::fixed << std::setprecision(2) << (ns / 1e6) << " ms";
    return oss.str();
  } else {
    std::ostringstream oss;
    oss << std::fixed << std::setprecision(3) << (ns / 1e9) << " s";
    return oss.str();
  }
}

auto FormatBytes(size_t bytes) -> std::string {
  if (bytes < 1024) {
    return std::to_string(bytes) + " B";
  } else if (bytes < 1024 * 1024) {
    std::ostringstream oss;
    oss << std::fixed << std::setprecision(2) << (static_cast<double>(bytes) / 1024.0) << " KB";
    return oss.str();
  } else if (bytes < 1024ULL * 1024 * 1024) {
    std::ostringstream oss;
    oss << std::fixed << std::setprecision(2)
        << (static_cast<double>(bytes) / (1024.0 * 1024.0)) << " MB";
    return oss.str();
  } else {
    std::ostringstream oss;
    oss << std::fixed << std::setprecision(2)
        << (static_cast<double>(bytes) / (1024.0 * 1024.0 * 1024.0)) << " GB";
    return oss.str();
  }
}

// ============================================================================
// Memory Tracking
// ============================================================================

struct MemorySnapshot {
  size_t rss_bytes       = 0;  // Resident Set Size
  size_t peak_rss_bytes  = 0;  // Peak RSS
  size_t vms_bytes       = 0;  // Virtual Memory Size
};

auto CaptureMemorySnapshot() -> MemorySnapshot {
  MemorySnapshot snap;

#ifdef _WIN32
  PROCESS_MEMORY_COUNTERS_EX pmc;
  pmc.cb = sizeof(pmc);
  if (GetProcessMemoryInfo(GetCurrentProcess(),
                           reinterpret_cast<PROCESS_MEMORY_COUNTERS*>(&pmc),
                           sizeof(pmc))) {
    snap.rss_bytes      = pmc.WorkingSetSize;
    snap.peak_rss_bytes = pmc.PeakWorkingSetSize;
    snap.vms_bytes      = pmc.PrivateUsage;
  }
#elif defined(__APPLE__)
  struct mach_task_basic_info info;
  mach_msg_type_number_t count = MACH_TASK_BASIC_INFO_COUNT;
  if (task_info(mach_task_self(), MACH_TASK_BASIC_INFO,
                reinterpret_cast<task_info_t>(&info), &count) == KERN_SUCCESS) {
    snap.rss_bytes      = info.resident_size;
    snap.vms_bytes      = info.virtual_size;
    snap.peak_rss_bytes = info.resident_size_max;
  }
#else
  // Linux: read from /proc/self/status
  FILE* f = fopen("/proc/self/status", "r");
  if (f) {
    char line[256];
    while (fgets(line, sizeof(line), f)) {
      if (strncmp(line, "VmRSS:", 6) == 0) {
        unsigned long kb = 0;
        if (sscanf(line + 6, "%lu", &kb) == 1) {
          snap.rss_bytes = kb * 1024;
        }
      } else if (strncmp(line, "VmHWM:", 6) == 0) {
        unsigned long kb = 0;
        if (sscanf(line + 6, "%lu", &kb) == 1) {
          snap.peak_rss_bytes = kb * 1024;
        }
      } else if (strncmp(line, "VmSize:", 7) == 0) {
        unsigned long kb = 0;
        if (sscanf(line + 7, "%lu", &kb) == 1) {
          snap.vms_bytes = kb * 1024;
        }
      }
    }
    fclose(f);
  }

  // Fallback to getrusage for peak RSS on Linux
  if (snap.peak_rss_bytes == 0) {
    struct rusage usage;
    if (getrusage(RUSAGE_SELF, &usage) == 0) {
      snap.peak_rss_bytes = static_cast<size_t>(usage.ru_maxrss) * 1024;
    }
  }
#endif

  return snap;
}

// ============================================================================
// GPU Timing Support
// ============================================================================

#ifdef HAVE_CUDA
#include <cuda_runtime.h>

struct GpuTimer {
  cudaEvent_t start_event;
  cudaEvent_t stop_event;

  GpuTimer() {
    cudaEventCreate(&start_event);
    cudaEventCreate(&stop_event);
  }

  ~GpuTimer() {
    cudaEventDestroy(start_event);
    cudaEventDestroy(stop_event);
  }

  void Start() { cudaEventRecord(start_event); }
  void Stop()  { cudaEventRecord(stop_event); }

  auto ElapsedMs() -> float {
    cudaEventSynchronize(stop_event);
    float ms = 0.0f;
    cudaEventElapsedTime(&ms, start_event, stop_event);
    return ms;
  }
};
#endif  // HAVE_CUDA

// ============================================================================
// BenchmarkRunner
// ============================================================================

struct BenchmarkConfig {
  size_t warmup_iterations   = 3;
  size_t measured_iterations = 20;
  bool   track_memory        = true;
  bool   verbose             = true;
};

struct BenchmarkResult {
  std::string     name;
  BenchmarkStats  timing_stats;
  MemorySnapshot  mem_before;
  MemorySnapshot  mem_after;
  size_t          mem_delta_bytes    = 0;
  size_t          mem_peak_delta     = 0;
  double          throughput_per_sec = 0.0;  // optional: ops/sec
  std::string     unit;                       // e.g. "per_megapixel", "per_image"
  double          unit_scale           = 1.0; // e.g. megapixels per iteration
  std::string     custom_metric_name;
  double          custom_metric_value  = 0.0;
};

class BenchmarkRunner {
 public:
  explicit BenchmarkRunner(BenchmarkConfig config = {})
      : config_(config) {}

  /// Run a benchmark with the given callable.
  /// The callable should accept (size_t iteration_index) and perform one
  /// iteration of the operation being benchmarked.
  template <typename Func>
  auto Run(const std::string& name, Func&& func) -> BenchmarkResult {
    BenchmarkResult result;
    result.name = name;

    if (config_.verbose) {
      std::cout << "=== Benchmark: " << name << " ===" << std::endl;
    }

    // Capture memory before
    if (config_.track_memory) {
      result.mem_before = CaptureMemorySnapshot();
    }

    // Warmup iterations
    if (config_.verbose) {
      std::cout << "  Warming up (" << config_.warmup_iterations
                << " iterations)..." << std::endl;
    }
    for (size_t i = 0; i < config_.warmup_iterations; ++i) {
      func(i);
    }

    // Measured iterations
    std::vector<double> samples;
    samples.reserve(config_.measured_iterations);

    if (config_.verbose) {
      std::cout << "  Measuring (" << config_.measured_iterations
                << " iterations)..." << std::endl;
    }

    size_t peak_rss_during = result.mem_before.peak_rss_bytes;

    for (size_t i = 0; i < config_.measured_iterations; ++i) {
      // Track memory at start of each iteration
      MemorySnapshot iter_start;
      if (config_.track_memory) {
        iter_start = CaptureMemorySnapshot();
      }

      auto start = std::chrono::high_resolution_clock::now();
      func(config_.warmup_iterations + i);
      auto end = std::chrono::high_resolution_clock::now();

      double elapsed_ns = static_cast<double>(
          std::chrono::duration_cast<std::chrono::nanoseconds>(end - start).count());
      samples.push_back(elapsed_ns);

      // Track peak memory
      if (config_.track_memory) {
        MemorySnapshot iter_end = CaptureMemorySnapshot();
        if (iter_end.peak_rss_bytes > peak_rss_during) {
          peak_rss_during = iter_end.peak_rss_bytes;
        }
      }
    }

    // Compute statistics
    result.timing_stats = ComputeStats(std::move(samples));

    // Capture memory after
    if (config_.track_memory) {
      result.mem_after = CaptureMemorySnapshot();
      result.mem_delta_bytes =
          (result.mem_after.rss_bytes > result.mem_before.rss_bytes)
              ? result.mem_after.rss_bytes - result.mem_before.rss_bytes
              : 0;
      result.mem_peak_delta =
          (peak_rss_during > result.mem_before.peak_rss_bytes)
              ? peak_rss_during - result.mem_before.peak_rss_bytes
              : 0;
    }

    if (config_.verbose) {
      PrintResult(result);
    }

    return result;
  }

  /// Run a GPU benchmark with CUDA event timing.
  /// The callable receives (size_t iteration_index) and should include
  /// appropriate CUDA synchronization.
#ifdef HAVE_CUDA
  template <typename Func>
  auto RunGpu(const std::string& name, Func&& func) -> BenchmarkResult {
    BenchmarkResult result;
    result.name = name;

    if (config_.verbose) {
      std::cout << "=== GPU Benchmark: " << name << " ===" << std::endl;
    }

    // Warmup
    for (size_t i = 0; i < config_.warmup_iterations; ++i) {
      GpuTimer timer;
      timer.Start();
      func(i);
      timer.Stop();
    }

    // Measure
    std::vector<double> samples;
    samples.reserve(config_.measured_iterations);

    for (size_t i = 0; i < config_.measured_iterations; ++i) {
      GpuTimer timer;
      timer.Start();
      func(config_.warmup_iterations + i);
      timer.Stop();
      samples.push_back(static_cast<double>(timer.ElapsedMs() * 1e6));  // ns
    }

    result.timing_stats = ComputeStats(std::move(samples));

    if (config_.verbose) {
      PrintResult(result);
    }

    return result;
  }
#endif  // HAVE_CUDA

  static void PrintResult(const BenchmarkResult& result) {
    const auto& s = result.timing_stats;
    std::cout << "  Result: " << result.name << std::endl;
    std::cout << "    Iterations:  " << s.iterations
              << " (outliers: " << s.outliers << ")" << std::endl;
    std::cout << "    Min:         " << FormatNanos(s.min_ns) << std::endl;
    std::cout << "    Max:         " << FormatNanos(s.max_ns) << std::endl;
    std::cout << "    Mean:        " << FormatNanos(s.mean_ns) << std::endl;
    std::cout << "    Median:      " << FormatNanos(s.median_ns) << std::endl;
    std::cout << "    P95:         " << FormatNanos(s.p95_ns) << std::endl;
    std::cout << "    P99:         " << FormatNanos(s.p99_ns) << std::endl;
    std::cout << "    StdDev:      " << FormatNanos(s.stddev_ns)
              << " (CV: " << std::fixed << std::setprecision(1)
              << s.cv_percent << "%)" << std::endl;

    if (result.throughput_per_sec > 0.0) {
      std::cout << "    Throughput:  " << std::fixed << std::setprecision(2)
                << result.throughput_per_sec << " ops/sec" << std::endl;
    }

    if (!result.unit.empty() && result.unit_scale > 0.0 && s.mean_ns > 0.0) {
      double per_unit_ns = s.mean_ns / result.unit_scale;
      std::cout << "    Per " << result.unit << ": "
                << FormatNanos(per_unit_ns) << std::endl;
    }

    if (!result.custom_metric_name.empty()) {
      std::cout << "    " << result.custom_metric_name << ": "
                << std::fixed << std::setprecision(2)
                << result.custom_metric_value << std::endl;
    }

    if (result.mem_delta_bytes > 0 || result.mem_peak_delta > 0) {
      std::cout << "    Memory delta: " << FormatBytes(result.mem_delta_bytes) << std::endl;
      std::cout << "    Peak delta:   " << FormatBytes(result.mem_peak_delta) << std::endl;
      std::cout << "    RSS before:   " << FormatBytes(result.mem_before.rss_bytes) << std::endl;
      std::cout << "    RSS after:    " << FormatBytes(result.mem_after.rss_bytes) << std::endl;
    }
    std::cout << std::endl;
  }

  /// Print a summary table of multiple benchmark results.
  static void PrintSummaryTable(const std::vector<BenchmarkResult>& results) {
    std::cout << "\n=== Benchmark Summary ===" << std::endl;
    std::cout << std::left << std::setw(40) << "Benchmark"
              << std::right << std::setw(12) << "Mean"
              << std::setw(12) << "Median"
              << std::setw(12) << "P95"
              << std::setw(12) << "Min"
              << std::setw(12) << "Max"
              << std::setw(10) << "CV%"
              << std::setw(12) << "Mem Delta"
              << std::endl;
    std::cout << std::string(122, '-') << std::endl;

    for (const auto& r : results) {
      std::cout << std::left << std::setw(40) << r.name
                << std::right << std::setw(12) << FormatNanos(r.timing_stats.mean_ns)
                << std::setw(12) << FormatNanos(r.timing_stats.median_ns)
                << std::setw(12) << FormatNanos(r.timing_stats.p95_ns)
                << std::setw(12) << FormatNanos(r.timing_stats.min_ns)
                << std::setw(12) << FormatNanos(r.timing_stats.max_ns)
                << std::setw(10) << std::fixed << std::setprecision(1)
                << r.timing_stats.cv_percent << "%"
                << std::setw(12) << FormatBytes(r.mem_delta_bytes)
                << std::endl;
    }
    std::cout << std::endl;
  }

 private:
  BenchmarkConfig config_;
};

}  // namespace alcedo::bench
