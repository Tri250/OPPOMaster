//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "processing/denoiser.hpp"

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <limits>
#include <mutex>
#include <numeric>
#include <stdexcept>
#include <thread>
#include <vector>

namespace alcedo {
namespace denoise {

// Internal helper functions
namespace {

// Precompute DCT basis for 8x8 blocks
struct DCTCache {
  std::vector<std::vector<float>> dct_basis;
  std::vector<std::vector<float>> idct_basis;
  int block_size;

  DCTCache(int bs) : block_size(bs) {
    dct_basis.resize(block_size, std::vector<float>(block_size));
    idct_basis.resize(block_size, std::vector<float>(block_size));

    const float inv_sqrt_2 = 1.0f / std::sqrt(2.0f);
    const float scale = 2.0f / static_cast<float>(block_size);

    for (int u = 0; u < block_size; ++u) {
      for (int x = 0; x < block_size; ++x) {
        float cu = (u == 0) ? inv_sqrt_2 : 1.0f;
        dct_basis[u][x] = cu * std::cos(static_cast<float>((2 * x + 1) * u) *
                                        static_cast<float>(M_PI) / (2.0f * block_size));
        idct_basis[x][u] = cu * std::cos(static_cast<float>((2 * x + 1) * u) *
                                         static_cast<float>(M_PI) / (2.0f * block_size)) * scale;
      }
    }
  }
};

// 2D DCT transform
void dct2d(const float* input, float* output, int block_size, const DCTCache& cache) {
  std::vector<float> temp(block_size * block_size, 0.0f);

  // Column transforms
  for (int i = 0; i < block_size; ++i) {
    for (int u = 0; u < block_size; ++u) {
      float sum = 0.0f;
      for (int x = 0; x < block_size; ++x) {
        sum += cache.dct_basis[u][x] * input[x * block_size + i];
      }
      temp[u * block_size + i] = sum;
    }
  }

  // Row transforms
  for (int u = 0; u < block_size; ++u) {
    for (int v = 0; v < block_size; ++v) {
      float sum = 0.0f;
      for (int y = 0; y < block_size; ++y) {
        sum += temp[u * block_size + y] * cache.dct_basis[v][y];
      }
      output[u * block_size + v] = sum;
    }
  }
}

// 2D inverse DCT
void idct2d(const float* input, float* output, int block_size, const DCTCache& cache) {
  std::vector<float> temp(block_size * block_size, 0.0f);

  // Column transforms
  for (int i = 0; i < block_size; ++i) {
    for (int x = 0; x < block_size; ++x) {
      float sum = 0.0f;
      for (int u = 0; u < block_size; ++u) {
        sum += input[u * block_size + i] * cache.idct_basis[x][u];
      }
      temp[x * block_size + i] = sum;
    }
  }

  // Row transforms
  for (int x = 0; x < block_size; ++x) {
    for (int y = 0; y < block_size; ++y) {
      float sum = 0.0f;
      for (int v = 0; v < block_size; ++v) {
        sum += temp[x * block_size + v] * cache.idct_basis[y][v];
      }
      output[x * block_size + y] = sum;
    }
  }
}

// 1D Haar wavelet forward transform
void haar1d_forward(float* data, int n) {
  std::vector<float> temp(n);
  int length = n;
  while (length > 1) {
    int half = length / 2;
    for (int i = 0; i < half; ++i) {
      temp[i] = (data[2 * i] + data[2 * i + 1]) / std::sqrt(2.0f);
      temp[half + i] = (data[2 * i] - data[2 * i + 1]) / std::sqrt(2.0f);
    }
    std::copy(temp.begin(), temp.begin() + length, data);
    length = half;
  }
}

// 1D Haar wavelet inverse transform
void haar1d_inverse(float* data, int n) {
  std::vector<float> temp(n);
  int length = 1;
  while (length < n) {
    int half = length;
    length *= 2;
    for (int i = 0; i < half; ++i) {
      float a = data[i];
      float b = data[half + i];
      temp[2 * i] = (a + b) / std::sqrt(2.0f);
      temp[2 * i + 1] = (a - b) / std::sqrt(2.0f);
    }
    std::copy(temp.begin(), temp.begin() + length, data);
  }
}

// Hard thresholding
void hard_threshold(float* data, int size, float threshold) {
  for (int i = 0; i < size; ++i) {
    if (std::abs(data[i]) < threshold) {
      data[i] = 0.0f;
    }
  }
}

// Soft thresholding
float soft_threshold(float x, float threshold) {
  float abs_x = std::abs(x);
  if (abs_x < threshold) {
    return 0.0f;
  }
  return x > 0 ? (x - threshold) : (x + threshold);
}

// Compute L2 distance between two blocks in transform domain
float block_distance_l2(const float* a, const float* b, int size) {
  float dist = 0.0f;
  for (int i = 0; i < size; ++i) {
    float d = a[i] - b[i];
    dist += d * d;
  }
  return dist;
}

// Clamp coordinates to image bounds
inline int clamp_coord(int x, int max_val) {
  return std::clamp(x, 0, max_val - 1);
}

// Bilateral spatial weight
inline float bilateral_spatial_weight(int dx, int dy, float sigma_spatial) {
  float dist_sq = static_cast<float>(dx * dx + dy * dy);
  return std::exp(-dist_sq / (2.0f * sigma_spatial * sigma_spatial));
}

// Bilateral range weight
inline float bilateral_range_weight(float diff, float sigma_range) {
  float diff_sq = diff * diff;
  return std::exp(-diff_sq / (2.0f * sigma_range * sigma_range));
}

}  // namespace

// BM3D denoising (simplified implementation)
void bm3d_denoise(const std::vector<float>& input,
                  std::vector<float>& output,
                  int width,
                  int height,
                  float sigma,
                  int block_size,
                  int search_window,
                  int max_blocks) {
  if (input.empty() || input.size() != static_cast<size_t>(width * height)) {
    throw std::invalid_argument("Invalid input dimensions");
  }

  output.resize(input.size());
  std::vector<float> accumulator(width * height, 0.0f);
  std::vector<float> weight_accum(width * height, 0.0f);

  const DCTCache dct_cache(block_size);
  const float threshold = 2.7f * sigma;
  const float similarity_threshold = 25.0f * sigma * sigma;

  int step = block_size / 2;
  int n_blocks_x = (width - block_size + step - 1) / step;
  int n_blocks_y = (height - block_size + step - 1) / step;

  std::mutex accum_mutex;

  auto process_block = [&](int by, int bx) {
    // Get reference block position
    int x0 = bx * step;
    int y0 = by * step;
    if (x0 + block_size > width) x0 = width - block_size;
    if (y0 + block_size > height) y0 = height - block_size;

    // Extract reference block and compute 2D DCT
    std::vector<float> ref_block(block_size * block_size);
    std::vector<float> ref_dct(block_size * block_size);
    for (int y = 0; y < block_size; ++y) {
      for (int x = 0; x < block_size; ++x) {
        ref_block[y * block_size + x] =
            input[(y0 + y) * width + (x0 + x)];
      }
    }
    dct2d(ref_block.data(), ref_dct.data(), block_size, dct_cache);

    // Block matching - find similar blocks
    std::vector<std::pair<float, std::pair<int, int>>> candidates;
    candidates.reserve(search_window * search_window);

    int search_start_x = std::max(0, x0 - search_window / 2);
    int search_start_y = std::max(0, y0 - search_window / 2);
    int search_end_x = std::min(width - block_size, x0 + search_window / 2);
    int search_end_y = std::min(height - block_size, y0 + search_window / 2);

    for (int sy = search_start_y; sy <= search_end_y; sy += step) {
      for (int sx = search_start_x; sx <= search_end_x; sx += step) {
        std::vector<float> candidate_block(block_size * block_size);
        std::vector<float> candidate_dct(block_size * block_size);
        for (int y = 0; y < block_size; ++y) {
          for (int x = 0; x < block_size; ++x) {
            candidate_block[y * block_size + x] =
                input[(sy + y) * width + (sx + x)];
          }
        }
        dct2d(candidate_block.data(), candidate_dct.data(), block_size, dct_cache);

        float dist = block_distance_l2(ref_dct.data(), candidate_dct.data(),
                                       block_size * block_size);
        if (dist < similarity_threshold) {
          candidates.emplace_back(dist, std::make_pair(sx, sy));
        }
      }
    }

    // Sort by similarity and keep best max_blocks
    std::sort(candidates.begin(), candidates.end(),
              [](const auto& a, const auto& b) { return a.first < b.first; });
    if (candidates.size() > static_cast<size_t>(max_blocks)) {
      candidates.resize(max_blocks);
    }

    int group_size = static_cast<int>(candidates.size());
    if (group_size == 0) {
      group_size = 1;
      candidates.emplace_back(0.0f, std::make_pair(x0, y0));
    }

    // Pad to next power of two for Haar
    int haar_size = 1;
    while (haar_size < group_size) haar_size *= 2;

    // Build 3D group and apply transforms
    std::vector<std::vector<float>> group(haar_size,
                                          std::vector<float>(block_size * block_size));
    for (int i = 0; i < group_size; ++i) {
      int sx = candidates[i].second.first;
      int sy = candidates[i].second.second;
      for (int y = 0; y < block_size; ++y) {
        for (int x = 0; x < block_size; ++x) {
          group[i][y * block_size + x] =
              input[(sy + y) * width + (sx + x)];
        }
      }
      dct2d(group[i].data(), group[i].data(), block_size, dct_cache);
    }

    // 1D Haar along third dimension for each coefficient
    std::vector<float> haar_line(haar_size);
    for (int coeff_y = 0; coeff_y < block_size; ++coeff_y) {
      for (int coeff_x = 0; coeff_x < block_size; ++coeff_x) {
        for (int i = 0; i < haar_size; ++i) {
          haar_line[i] = group[i][coeff_y * block_size + coeff_x];
        }
        haar1d_forward(haar_line.data(), haar_size);
        hard_threshold(haar_line.data(), haar_size, threshold);
        haar1d_inverse(haar_line.data(), haar_size);
        for (int i = 0; i < haar_size; ++i) {
          group[i][coeff_y * block_size + coeff_x] = haar_line[i];
        }
      }
    }

    // Inverse transforms and aggregate
    float group_weight = 1.0f / (sigma * sigma * std::max(1, group_size));
    for (int i = 0; i < group_size; ++i) {
      int sx = candidates[i].second.first;
      int sy = candidates[i].second.second;
      std::vector<float> filtered_block(block_size * block_size);
      idct2d(group[i].data(), filtered_block.data(), block_size, dct_cache);

      std::lock_guard<std::mutex> lock(accum_mutex);
      for (int y = 0; y < block_size; ++y) {
        for (int x = 0; x < block_size; ++x) {
          int img_y = sy + y;
          int img_x = sx + x;
          int idx = img_y * width + img_x;
          accumulator[idx] += filtered_block[y * block_size + x] * group_weight;
          weight_accum[idx] += group_weight;
        }
      }
    }
  };

  // Parallel processing
  std::vector<std::thread> threads;
  int num_threads = std::thread::hardware_concurrency();
  if (num_threads == 0) num_threads = 4;

  int total_blocks = n_blocks_y * n_blocks_x;
  int blocks_per_thread = (total_blocks + num_threads - 1) / num_threads;
  int block_counter = 0;

  for (int t = 0; t < num_threads && block_counter < total_blocks; ++t) {
    int start_block = block_counter;
    int end_block = std::min(start_block + blocks_per_thread, total_blocks);
    threads.emplace_back([&, start_block, end_block]() {
      for (int b = start_block; b < end_block; ++b) {
        int by = b / n_blocks_x;
        int bx = b % n_blocks_x;
        process_block(by, bx);
      }
    });
    block_counter = end_block;
  }

  for (auto& t : threads) {
    t.join();
  }

  // Normalize output
  for (size_t i = 0; i < input.size(); ++i) {
    if (weight_accum[i] > 0.0f) {
      output[i] = accumulator[i] / weight_accum[i];
    } else {
      output[i] = input[i];
    }
  }
}

// Wavelet-based denoising with soft thresholding
void wavelet_denoise(const std::vector<float>& input,
                     std::vector<float>& output,
                     int width,
                     int height,
                     float sigma,
                     int levels) {
  if (input.empty() || input.size() != static_cast<size_t>(width * height)) {
    throw std::invalid_argument("Invalid input dimensions");
  }

  // Find the largest power of two that fits
  int max_size = std::max(width, height);
  int pow2_size = 1;
  while (pow2_size < max_size) pow2_size *= 2;

  // Pad image to power of two
  std::vector<float> img_padded(pow2_size * pow2_size, 0.0f);
  for (int y = 0; y < height; ++y) {
    for (int x = 0; x < width; ++x) {
      img_padded[y * pow2_size + x] = input[y * width + x];
    }
  }

  // 2D Haar wavelet decomposition
  int current_size = pow2_size;
  std::vector<std::vector<float>> detail_coeffs;

  for (int level = 0; level < levels && current_size > 1; ++level) {
    // Process rows
    std::vector<float> temp(current_size * current_size);
    int half = current_size / 2;

    for (int y = 0; y < current_size; ++y) {
      std::vector<float> row(current_size);
      for (int x = 0; x < current_size; ++x) {
        row[x] = img_padded[y * current_size + x];
      }
      haar1d_forward(row.data(), current_size);
      for (int x = 0; x < current_size; ++x) {
        temp[y * current_size + x] = row[x];
      }
    }

    // Process columns
    for (int x = 0; x < current_size; ++x) {
      std::vector<float> col(current_size);
      for (int y = 0; y < current_size; ++y) {
        col[y] = temp[y * current_size + x];
      }
      haar1d_forward(col.data(), current_size);
      for (int y = 0; y < current_size; ++y) {
        img_padded[y * current_size + x] = col[y];
      }
    }

    // Extract detail coefficients for thresholding
    current_size /= 2;
  }

  // Apply soft thresholding to detail coefficients
  float threshold = sigma * std::sqrt(2.0f * std::log(static_cast<float>(width * height)));
  current_size = pow2_size;
  for (int level = 0; level < levels && current_size > 1; ++level) {
    int half = current_size / 2;

    for (int y = 0; y < current_size; ++y) {
      for (int x = half; x < current_size; ++x) {
        img_padded[y * current_size + x] =
            soft_threshold(img_padded[y * current_size + x], threshold);
      }
    }

    for (int y = half; y < current_size; ++y) {
      for (int x = 0; x < half; ++x) {
        img_padded[y * current_size + x] =
            soft_threshold(img_padded[y * current_size + x], threshold);
      }
    }

    current_size = half;
  }

  // Reconstruct image
  current_size = 2;
  for (int level = 1; level <= levels && current_size <= pow2_size; ++level) {
    int half = current_size / 2;

    // Process columns
    for (int x = 0; x < current_size; ++x) {
      std::vector<float> col(current_size);
      for (int y = 0; y < current_size; ++y) {
        col[y] = img_padded[y * current_size + x];
      }
      haar1d_inverse(col.data(), current_size);
      for (int y = 0; y < current_size; ++y) {
        img_padded[y * current_size + x] = col[y];
      }
    }

    // Process rows
    std::vector<float> temp(current_size * current_size);
    for (int y = 0; y < current_size; ++y) {
      std::vector<float> row(current_size);
      for (int x = 0; x < current_size; ++x) {
        row[x] = img_padded[y * current_size + x];
      }
      haar1d_inverse(row.data(), current_size);
      for (int x = 0; x < current_size; ++x) {
        temp[y * current_size + x] = row[x];
      }
    }

    for (int y = 0; y < current_size; ++y) {
      for (int x = 0; x < current_size; ++x) {
        img_padded[y * current_size + x] = temp[y * current_size + x];
      }
    }

    current_size *= 2;
  }

  // Extract result
  output.resize(input.size());
  for (int y = 0; y < height; ++y) {
    for (int x = 0; x < width; ++x) {
      output[y * width + x] = std::clamp(img_padded[y * pow2_size + x],
                                         0.0f, 1.0f);
    }
  }
}

// Bilateral filter for edge-preserving denoising
void bilateral_denoise(const std::vector<float>& input,
                       std::vector<float>& output,
                       int width,
                       int height,
                       float sigma_spatial,
                       float sigma_range) {
  if (input.empty() || input.size() != static_cast<size_t>(width * height)) {
    throw std::invalid_argument("Invalid input dimensions");
  }

  output.resize(input.size());

  int radius = static_cast<int>(std::ceil(3.0f * sigma_spatial));
  if (radius < 1) radius = 1;

  std::mutex out_mutex;

  auto process_row = [&](int start_y, int end_y) {
    for (int y = start_y; y < end_y; ++y) {
      for (int x = 0; x < width; ++x) {
        float center_val = input[y * width + x];
        float sum = 0.0f;
        float norm = 0.0f;

        int y_start = clamp_coord(y - radius, height);
        int y_end = clamp_coord(y + radius + 1, height);

        for (int wy = y_start; wy < y_end; ++wy) {
          int dy = wy - y;
          int x_start = clamp_coord(x - radius, width);
          int x_end = clamp_coord(x + radius + 1, width);

          for (int wx = x_start; wx < x_end; ++wx) {
            int dx = wx - x;
            float neighbor_val = input[wy * width + wx];

            float w_s = bilateral_spatial_weight(dx, dy, sigma_spatial);
            float w_r = bilateral_range_weight(neighbor_val - center_val, sigma_range);
            float w = w_s * w_r;

            sum += neighbor_val * w;
            norm += w;
          }
        }

        float result = sum / norm;
        std::lock_guard<std::mutex> lock(out_mutex);
        output[y * width + x] = std::clamp(result, 0.0f, 1.0f);
      }
    }
  };

  // Parallel processing by rows
  std::vector<std::thread> threads;
  int num_threads = std::thread::hardware_concurrency();
  if (num_threads == 0) num_threads = 4;

  int rows_per_thread = (height + num_threads - 1) / num_threads;
  for (int t = 0; t < num_threads; ++t) {
    int start_y = t * rows_per_thread;
    int end_y = std::min(start_y + rows_per_thread, height);
    if (start_y < height) {
      threads.emplace_back(process_row, start_y, end_y);
    }
  }

  for (auto& t : threads) {
    t.join();
  }
}

// Non-local means denoising
void nlm_denoise(const std::vector<float>& input,
                 std::vector<float>& output,
                 int width,
                 int height,
                 float h,
                 int patch_size,
                 int search_window) {
  if (input.empty() || input.size() != static_cast<size_t>(width * height)) {
    throw std::invalid_argument("Invalid input dimensions");
  }

  output.resize(input.size());

  int half_patch = patch_size / 2;
  int half_search = search_window / 2;
  float h_sq = h * h;

  std::mutex out_mutex;

  auto process_pixel = [&](int start_y, int end_y) {
    for (int y = start_y; y < end_y; ++y) {
      for (int x = 0; x < width; ++x) {
        float sum = 0.0f;
        float weight_sum = 0.0f;

        int search_y0 = std::max(0, y - half_search);
        int search_y1 = std::min(height - 1, y + half_search);
        int search_x0 = std::max(0, x - half_search);
        int search_x1 = std::min(width - 1, x + half_search);

        for (int ny = search_y0; ny <= search_y1; ++ny) {
          for (int nx = search_x0; nx <= search_x1; ++nx) {
            // Compute patch distance
            float dist_sq = 0.0f;
            int count = 0;

            for (int dy = -half_patch; dy <= half_patch; ++dy) {
              int py = y + dy;
              int pny = ny + dy;
              if (pny < 0 || pny >= height) continue;
              for (int dx = -half_patch; dx <= half_patch; ++dx) {
                int px = x + dx;
                int pnx = nx + dx;
                if (pnx < 0 || pnx >= width) continue;

                float diff = input[py * width + px] - input[pny * width + pnx];
                dist_sq += diff * diff;
                count++;
              }
            }

            if (count > 0) {
              dist_sq /= static_cast<float>(count);
              float weight = std::exp(-dist_sq / h_sq);
              sum += input[ny * width + nx] * weight;
              weight_sum += weight;
            }
          }
        }

        float result = sum / weight_sum;
        std::lock_guard<std::mutex> lock(out_mutex);
        output[y * width + x] = std::clamp(result, 0.0f, 1.0f);
      }
    }
  };

  // Parallel processing
  std::vector<std::thread> threads;
  int num_threads = std::thread::hardware_concurrency();
  if (num_threads == 0) num_threads = 4;

  int rows_per_thread = (height + num_threads - 1) / num_threads;
  for (int t = 0; t < num_threads; ++t) {
    int start_y = t * rows_per_thread;
    int end_y = std::min(start_y + rows_per_thread, height);
    if (start_y < height) {
      threads.emplace_back(process_pixel, start_y, end_y);
    }
  }

  for (auto& t : threads) {
    t.join();
  }
}

// Separate chroma denoising - denoise YCbCr channels with higher strength on chroma
void separate_chroma_denoise(const std::vector<float>& input_y,
                              const std::vector<float>& input_cb,
                              const std::vector<float>& input_cr,
                              std::vector<float>& output_y,
                              std::vector<float>& output_cb,
                              std::vector<float>& output_cr,
                              int width,
                              int height,
                              float luma_strength,
                              float chroma_strength,
                              DenoiseMethod method) {
  if (input_y.size() != static_cast<size_t>(width * height) ||
      input_cb.size() != static_cast<size_t>(width * height) ||
      input_cr.size() != static_cast<size_t>(width * height)) {
    throw std::invalid_argument("Invalid input dimensions");
  }

  output_y.resize(input_y.size());
  output_cb.resize(input_cb.size());
  output_cr.resize(input_cr.size());

  // Denoise luma with lower strength
  switch (method) {
    case DenoiseMethod::BM3D:
      bm3d_denoise(input_y, output_y, width, height, luma_strength);
      bm3d_denoise(input_cb, output_cb, width, height, chroma_strength);
      bm3d_denoise(input_cr, output_cr, width, height, chroma_strength);
      break;
    case DenoiseMethod::WAVELET:
      wavelet_denoise(input_y, output_y, width, height, luma_strength);
      wavelet_denoise(input_cb, output_cb, width, height, chroma_strength);
      wavelet_denoise(input_cr, output_cr, width, height, chroma_strength);
      break;
    case DenoiseMethod::BILATERAL:
      bilateral_denoise(input_y, output_y, width, height,
                        std::max(1.0f, 3.0f * luma_strength), luma_strength * 0.1f);
      bilateral_denoise(input_cb, output_cb, width, height,
                        std::max(2.0f, 5.0f * chroma_strength), chroma_strength * 0.2f);
      bilateral_denoise(input_cr, output_cr, width, height,
                        std::max(2.0f, 5.0f * chroma_strength), chroma_strength * 0.2f);
      break;
    case DenoiseMethod::NLM:
      nlm_denoise(input_y, output_y, width, height, luma_strength);
      nlm_denoise(input_cb, output_cb, width, height, chroma_strength);
      nlm_denoise(input_cr, output_cr, width, height, chroma_strength);
      break;
  }
}

// Main entry point with method selection
void denoise_image(const std::vector<float>& input,
                   std::vector<float>& output,
                   int width,
                   int height,
                   float strength,
                   DenoiseMethod method,
                   bool separate_chroma,
                   const std::vector<float>& input_cb,
                   const std::vector<float>& input_cr,
                   std::vector<float>& output_cb,
                   std::vector<float>& output_cr) {
  if (strength <= 0.0f) {
    output = input;
    if (separate_chroma) {
      output_cb = input_cb;
      output_cr = input_cr;
    }
    return;
  }

  // Clamp strength to reasonable range
  strength = std::clamp(strength, 0.001f, 0.5f);

  if (separate_chroma) {
    float chroma_strength = strength * 1.5f;
    separate_chroma_denoise(input, input_cb, input_cr,
                            output, output_cb, output_cr,
                            width, height, strength, chroma_strength, method);
  } else {
    switch (method) {
      case DenoiseMethod::BM3D:
        bm3d_denoise(input, output, width, height, strength);
        break;
      case DenoiseMethod::WAVELET:
        wavelet_denoise(input, output, width, height, strength);
        break;
      case DenoiseMethod::BILATERAL: {
        float sigma_spatial = std::max(1.0f, 3.0f * strength * 10.0f);
        float sigma_range = strength * 0.1f;
        bilateral_denoise(input, output, width, height, sigma_spatial, sigma_range);
        break;
      }
      case DenoiseMethod::NLM:
        nlm_denoise(input, output, width, height, strength * 0.1f);
        break;
      default:
        throw std::invalid_argument("Unknown denoising method");
    }
  }
}

// Overload for single channel
void denoise_image(const std::vector<float>& input,
                   std::vector<float>& output,
                   int width,
                   int height,
                   float strength,
                   DenoiseMethod method) {
  std::vector<float> dummy1, dummy2, dummy3, dummy4;
  denoise_image(input, output, width, height, strength, method, false,
                dummy1, dummy2, dummy3, dummy4);
}

}  // namespace denoise
}  // namespace alcedo
