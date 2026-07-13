//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "processing/inpainting.hpp"

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <limits>
#include <mutex>
#include <queue>
#include <stdexcept>
#include <thread>
#include <tuple>
#include <vector>

namespace alcedo {
namespace inpaint {

// =============================================================================
// Internal helpers (anonymous namespace)
// =============================================================================
namespace {

// ---------------------------------------------------------------------------
// Utility: pixel index helpers
// ---------------------------------------------------------------------------
inline size_t px_idx(int x, int y, int c, int width, int channels) {
  return static_cast<size_t>(y * width + x) * channels + c;
}

inline size_t px_idx_flat(int x, int y, int width) {
  return static_cast<size_t>(y) * width + x;
}

inline int clamp_coord(int x, int max_val) {
  return std::clamp(x, 0, max_val - 1);
}

inline float lerp(float a, float b, float t) {
  return a + (b - a) * t;
}

// ---------------------------------------------------------------------------
// Patch distance / SSD computation
// ---------------------------------------------------------------------------
float patch_ssd(const std::vector<float>& image, int width, int height, int channels,
                int x1, int y1, int x2, int y2, int half_patch,
                const std::vector<uint8_t>& known_mask) {
  float ssd = 0.0f;
  int count = 0;
  for (int dy = -half_patch; dy <= half_patch; ++dy) {
    int py1 = y1 + dy;
    int py2 = y2 + dy;
    if (py1 < 0 || py1 >= height || py2 < 0 || py2 >= height) continue;
    for (int dx = -half_patch; dx <= half_patch; ++dx) {
      int px1 = x1 + dx;
      int px2 = x2 + dx;
      if (px1 < 0 || px1 >= width || px2 < 0 || px2 >= width) continue;
      // Only consider known pixels in the source patch
      if (known_mask[px_idx_flat(px2, py2, width)] == 0) continue;
      for (int c = 0; c < channels; ++c) {
        float diff = image[px_idx(px1, py1, c, width, channels)] -
                     image[px_idx(px2, py2, c, width, channels)];
        ssd += diff * diff;
      }
      ++count;
    }
  }
  if (count == 0) return std::numeric_limits<float>::max();
  return ssd / static_cast<float>(count);
}

// ---------------------------------------------------------------------------
// Distance transform (Euclidean, approximate)
// ---------------------------------------------------------------------------
void distance_transform_approx(std::vector<float>& dist, int width, int height) {
  // Forward pass
  for (int y = 0; y < height; ++y) {
    for (int x = 0; x < width; ++x) {
      size_t i = px_idx_flat(x, y, width);
      if (dist[i] > 0.0f) {
        float best = dist[i];
        if (x > 0 && dist[px_idx_flat(x - 1, y, width)] + 1.0f < best)
          best = dist[px_idx_flat(x - 1, y, width)] + 1.0f;
        if (y > 0 && dist[px_idx_flat(x, y - 1, width)] + 1.0f < best)
          best = dist[px_idx_flat(x, y - 1, width)] + 1.0f;
        dist[i] = best;
      }
    }
  }
  // Backward pass
  for (int y = height - 1; y >= 0; --y) {
    for (int x = width - 1; x >= 0; --x) {
      size_t i = px_idx_flat(x, y, width);
      if (dist[i] > 0.0f) {
        float best = dist[i];
        if (x + 1 < width && dist[px_idx_flat(x + 1, y, width)] + 1.0f < best)
          best = dist[px_idx_flat(x + 1, y, width)] + 1.0f;
        if (y + 1 < height && dist[px_idx_flat(x, y + 1, width)] + 1.0f < best)
          best = dist[px_idx_flat(x, y + 1, width)] + 1.0f;
        dist[i] = best;
      }
    }
  }
}

// ---------------------------------------------------------------------------
// Compute gradient magnitude (central difference)
// ---------------------------------------------------------------------------
void compute_gradient(const std::vector<float>& image, int width, int height, int channels,
                      std::vector<float>& grad_mag) {
  grad_mag.assign(width * height, 0.0f);
  for (int y = 1; y < height - 1; ++y) {
    for (int x = 1; x < width - 1; ++x) {
      float max_grad = 0.0f;
      for (int c = 0; c < channels; ++c) {
        float gx = image[px_idx(x + 1, y, c, width, channels)] -
                   image[px_idx(x - 1, y, c, width, channels)];
        float gy = image[px_idx(x, y + 1, c, width, channels)] -
                   image[px_idx(x, y - 1, c, width, channels)];
        float g = gx * gx + gy * gy;
        if (g > max_grad) max_grad = g;
      }
      grad_mag[px_idx_flat(x, y, width)] = std::sqrt(max_grad);
    }
  }
}

// ---------------------------------------------------------------------------
// Compute normal vector at boundary
// ---------------------------------------------------------------------------
void compute_boundary_normal(const std::vector<uint8_t>& known_mask, int width, int height,
                             int x, int y, float& nx, float& ny) {
  nx = 0.0f;
  ny = 0.0f;
  // Use 4-neighbor to estimate normal
  int count = 0;
  if (x > 0 && known_mask[px_idx_flat(x - 1, y, width)] != 0) { nx -= 1.0f; ++count; }
  if (x + 1 < width && known_mask[px_idx_flat(x + 1, y, width)] != 0) { nx += 1.0f; ++count; }
  if (y > 0 && known_mask[px_idx_flat(x, y - 1, width)] != 0) { ny -= 1.0f; ++count; }
  if (y + 1 < height && known_mask[px_idx_flat(x, y + 1, width)] != 0) { ny += 1.0f; ++count; }
  float len = std::sqrt(nx * nx + ny * ny);
  if (len > 0.0f) {
    nx /= len;
    ny /= len;
  }
}

// ---------------------------------------------------------------------------
// Determine number of threads to use
// ---------------------------------------------------------------------------
int resolve_thread_count(const InpaintOptions& options, int total_pixels) {
  if (!options.multi_threaded || total_pixels < 50000) return 1;
  if (options.num_threads > 0) return options.num_threads;
  int n = static_cast<int>(std::thread::hardware_concurrency());
  return std::max(1, std::min(n, 16));
}

// ---------------------------------------------------------------------------
// Initialize known mask from float mask
// ---------------------------------------------------------------------------
void init_known_mask(const std::vector<float>& mask, int width, int height,
                     std::vector<uint8_t>& known_mask) {
  known_mask.resize(width * height);
  size_t total = width * height;
  for (size_t i = 0; i < total; ++i) {
    known_mask[i] = (mask[i] < 0.5f) ? 1 : 0;  // 1 = known, 0 = unknown
  }
}

// ---------------------------------------------------------------------------
// Check if any unknown pixels remain
// ---------------------------------------------------------------------------
bool has_unknown(const std::vector<uint8_t>& known_mask, int width, int height) {
  size_t total = width * height;
  for (size_t i = 0; i < total; ++i) {
    if (known_mask[i] == 0) return true;
  }
  return false;
}

// ---------------------------------------------------------------------------
// Find frontier pixels (unknown pixels adjacent to known pixels)
// ---------------------------------------------------------------------------
void find_frontier(const std::vector<uint8_t>& known_mask, int width, int height,
                   std::vector<std::pair<int, int>>& frontier) {
  frontier.clear();
  static const int dx[] = {-1, 1, 0, 0};
  static const int dy[] = {0, 0, -1, 1};
  for (int y = 0; y < height; ++y) {
    for (int x = 0; x < width; ++x) {
      if (known_mask[px_idx_flat(x, y, width)] != 0) continue;
      bool on_frontier = false;
      for (int k = 0; k < 4; ++k) {
        int nx = x + dx[k];
        int ny = y + dy[k];
        if (nx >= 0 && nx < width && ny >= 0 && ny < height &&
            known_mask[px_idx_flat(nx, ny, width)] != 0) {
          on_frontier = true;
          break;
        }
      }
      if (on_frontier) {
        frontier.emplace_back(x, y);
      }
    }
  }
}

}  // namespace

// =============================================================================
// clone_heal – copy pixels from source region to target mask with feathering
// =============================================================================
void clone_heal(std::vector<float>& image,
                int width,
                int height,
                int channels,
                const std::vector<float>& mask,
                int source_x0,
                int source_y0,
                int target_x0,
                int target_y0,
                int region_width,
                int region_height,
                const InpaintOptions& options) {
  if (image.empty()) {
    throw std::invalid_argument("clone_heal: image is empty");
  }
  size_t expected_size = static_cast<size_t>(width) * height * channels;
  if (image.size() != expected_size) {
    throw std::invalid_argument("clone_heal: image size mismatch");
  }
  if (mask.size() != static_cast<size_t>(width) * height) {
    throw std::invalid_argument("clone_heal: mask size mismatch");
  }
  if (channels < 1 || channels > 4) {
    throw std::invalid_argument("clone_heal: channels must be 1-4");
  }

  // Clamp coordinates
  int sx0 = clamp_coord(source_x0, width);
  int sy0 = clamp_coord(source_y0, height);
  int tx0 = clamp_coord(target_x0, width);
  int ty0 = clamp_coord(target_y0, height);
  int rw = std::min(region_width, std::min(width - tx0, width - sx0));
  int rh = std::min(region_height, std::min(height - ty0, height - sy0));

  if (rw <= 0 || rh <= 0) return;

  // Compute distance transform of mask for feathering
  int feather_radius = std::max(1, static_cast<int>(options.feather_radius));
  std::vector<float> mask_dist(width * height, 0.0f);
  for (int y = 0; y < height; ++y) {
    for (int x = 0; x < width; ++x) {
      mask_dist[px_idx_flat(x, y, width)] =
          (mask[px_idx_flat(x, y, width)] > 0.5f) ? 1.0f : 0.0f;
    }
  }
  distance_transform_approx(mask_dist, width, height);

  int num_threads = resolve_thread_count(options, rw * rh);

  auto process_rows = [&](int start_row, int end_row) {
    for (int dy = start_row; dy < end_row; ++dy) {
      int ty = ty0 + dy;
      int sy = sy0 + dy;
      for (int dx = 0; dx < rw; ++dx) {
        int tx = tx0 + dx;
        int sx = sx0 + dx;

        size_t ti = px_idx_flat(tx, ty, width);
        float alpha = mask[ti];
        if (alpha <= 0.0f) continue;

        // Feathering weight based on distance to mask boundary
        float dist = mask_dist[ti];
        float feather_weight = 1.0f;
        if (dist < feather_radius) {
          feather_weight = dist / feather_radius;
        }
        float blend = alpha * feather_weight;

        for (int c = 0; c < channels; ++c) {
          float src_val = image[px_idx(sx, sy, c, width, channels)];
          float dst_val = image[px_idx(tx, ty, c, width, channels)];
          image[px_idx(tx, ty, c, width, channels)] = lerp(dst_val, src_val, blend);
        }
      }
    }
  };

  if (num_threads <= 1) {
    process_rows(0, rh);
  } else {
    std::vector<std::thread> threads;
    int rows_per_thread = (rh + num_threads - 1) / num_threads;
    for (int t = 0; t < num_threads; ++t) {
      int start = t * rows_per_thread;
      int end = std::min(start + rows_per_thread, rh);
      if (start < rh) {
        threads.emplace_back(process_rows, start, end);
      }
    }
    for (auto& t : threads) {
      t.join();
    }
  }
}

// =============================================================================
// clone_heal_transform – clone healing with affine transformation
// =============================================================================
void clone_heal_transform(std::vector<float>& image,
                          int width,
                          int height,
                          int channels,
                          const std::vector<float>& mask,
                          const std::vector<std::pair<float, float>>& source_points,
                          const std::vector<std::pair<float, float>>& target_points,
                          const InpaintOptions& options) {
  if (image.empty()) {
    throw std::invalid_argument("clone_heal_transform: image is empty");
  }
  if (source_points.size() < 3 || target_points.size() < 3) {
    throw std::invalid_argument(
        "clone_heal_transform: at least 3 point pairs required for affine transform");
  }
  if (mask.size() != static_cast<size_t>(width) * height) {
    throw std::invalid_argument("clone_heal_transform: mask size mismatch");
  }

  // Solve 2x3 affine: [a b tx; c d ty] from 3 point pairs
  // Using the first 3 pairs
  float x1 = source_points[0].first,  y1 = source_points[0].second;
  float x2 = source_points[1].first,  y2 = source_points[1].second;
  float x3 = source_points[2].first,  y3 = source_points[2].second;
  float u1 = target_points[0].first,  v1 = target_points[0].second;
  float u2 = target_points[1].first,  v2 = target_points[1].second;
  float u3 = target_points[2].first,  v3 = target_points[2].second;

  // Solve for a, b, tx
  float det = x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2);
  if (std::abs(det) < 1e-10f) {
    throw std::invalid_argument("clone_heal_transform: collinear source points");
  }
  float inv_det = 1.0f / det;

  float a = (u1 * (y2 - y3) + u2 * (y3 - y1) + u3 * (y1 - y2)) * inv_det;
  float b = (u1 * (x3 - x2) + u2 * (x1 - x3) + u3 * (x2 - x1)) * inv_det;
  float tx = (u1 * (x2 * y3 - x3 * y2) + u2 * (x3 * y1 - x1 * y3) +
              u3 * (x1 * y2 - x2 * y1)) * inv_det;

  float c = (v1 * (y2 - y3) + v2 * (y3 - y1) + v3 * (y1 - y2)) * inv_det;
  float d = (v1 * (x3 - x2) + v2 * (x1 - x3) + v3 * (x2 - x1)) * inv_det;
  float ty = (v1 * (x2 * y3 - x3 * y2) + v2 * (x3 * y1 - x1 * y3) +
              v3 * (x1 * y2 - x2 * y1)) * inv_det;

  // Inverse: map target → source
  float inv_aa = d, inv_bb = -b, inv_cc = -c, inv_dd = a;
  float inv_det2 = inv_aa * inv_dd - inv_bb * inv_cc;
  if (std::abs(inv_det2) < 1e-10f) {
    throw std::invalid_argument("clone_heal_transform: singular transform matrix");
  }
  float inv_det2_recip = 1.0f / inv_det2;

  float feather_radius = std::max(1.0f, options.feather_radius);

  // Compute distance transform of mask for feathering
  std::vector<float> mask_dist(width * height, 0.0f);
  for (int y = 0; y < height; ++y) {
    for (int x = 0; x < width; ++x) {
      mask_dist[px_idx_flat(x, y, width)] =
          (mask[px_idx_flat(x, y, width)] > 0.5f) ? 1.0f : 0.0f;
    }
  }
  distance_transform_approx(mask_dist, width, height);

  int num_threads = resolve_thread_count(options, width * height);

  auto process_rows = [&](int start_row, int end_row) {
    for (int y = start_row; y < end_row; ++y) {
      for (int x = 0; x < width; ++x) {
        size_t idx = px_idx_flat(x, y, width);
        float alpha = mask[idx];
        if (alpha <= 0.0f) continue;

        // Map target pixel to source via inverse affine
        float sx_f = (inv_aa * (x - tx) + inv_bb * (y - ty)) * inv_det2_recip;
        float sy_f = (inv_cc * (x - tx) + inv_dd * (y - ty)) * inv_det2_recip;

        int sx = static_cast<int>(std::floor(sx_f));
        int sy = static_cast<int>(std::floor(sy_f));
        float fx = sx_f - sx;
        float fy = sy_f - sy;

        // Feathering
        float dist = mask_dist[idx];
        float feather_weight = 1.0f;
        if (dist < feather_radius) {
          feather_weight = dist / feather_radius;
        }
        float blend = alpha * feather_weight;

        for (int ch = 0; ch < channels; ++ch) {
          // Bilinear interpolation from source
          int x0 = clamp_coord(sx, width);
          int y0 = clamp_coord(sy, height);
          int x1 = clamp_coord(sx + 1, width);
          int y1 = clamp_coord(sy + 1, height);

          float v00 = image[px_idx(x0, y0, ch, width, channels)];
          float v10 = image[px_idx(x1, y0, ch, width, channels)];
          float v01 = image[px_idx(x0, y1, ch, width, channels)];
          float v11 = image[px_idx(x1, y1, ch, width, channels)];

          float src_val = lerp(lerp(v00, v10, fx), lerp(v01, v11, fx), fy);
          float dst_val = image[px_idx(x, y, ch, width, channels)];
          image[px_idx(x, y, ch, width, channels)] = lerp(dst_val, src_val, blend);
        }
      }
    }
  };

  if (num_threads <= 1) {
    process_rows(0, height);
  } else {
    std::vector<std::thread> threads;
    int rows_per_thread = (height + num_threads - 1) / num_threads;
    for (int t = 0; t < num_threads; ++t) {
      int start = t * rows_per_thread;
      int end = std::min(start + rows_per_thread, height);
      if (start < height) {
        threads.emplace_back(process_rows, start, end);
      }
    }
    for (auto& t : threads) {
      t.join();
    }
  }
}

// =============================================================================
// fast_marching_inpaint – simplified Telea's algorithm
// =============================================================================
void fast_marching_inpaint(std::vector<float>& image,
                           int width,
                           int height,
                           int channels,
                           const std::vector<float>& mask,
                           const InpaintOptions& options) {
  if (image.empty()) {
    throw std::invalid_argument("fast_marching_inpaint: image is empty");
  }
  size_t expected_size = static_cast<size_t>(width) * height * channels;
  if (image.size() != expected_size) {
    throw std::invalid_argument("fast_marching_inpaint: image size mismatch");
  }
  if (mask.size() != static_cast<size_t>(width) * height) {
    throw std::invalid_argument("fast_marching_inpaint: mask size mismatch");
  }

  std::vector<uint8_t> known_mask;
  init_known_mask(mask, width, height, known_mask);

  if (!has_unknown(known_mask, width, height)) return;

  float radius = std::max(1.0f, options.feather_radius);

  // Distance to nearest known pixel (T map)
  std::vector<float> T(width * height, std::numeric_limits<float>::max());
  // Flag: 0 = known, 1 = band (frontier), 2 = far away (unknown)
  std::vector<uint8_t> flag(width * height, 2);

  // Priority queue: (T, x, y)
  using PQElement = std::tuple<float, int, int>;
  std::priority_queue<PQElement, std::vector<PQElement>, std::greater<PQElement>> pq;

  // Initialize: known pixels get T=0, frontier pixels get computed T
  for (int y = 0; y < height; ++y) {
    for (int x = 0; x < width; ++x) {
      size_t i = px_idx_flat(x, y, width);
      if (known_mask[i] != 0) {
        T[i] = 0.0f;
        flag[i] = 0;
      }
    }
  }

  // Find frontier pixels and initialize their T values
  for (int y = 0; y < height; ++y) {
    for (int x = 0; x < width; ++x) {
      size_t i = px_idx_flat(x, y, width);
      if (flag[i] != 2) continue;
      // Check if adjacent to known pixel
      bool is_frontier = false;
      if (x > 0 && flag[px_idx_flat(x - 1, y, width)] == 0) is_frontier = true;
      else if (x + 1 < width && flag[px_idx_flat(x + 1, y, width)] == 0) is_frontier = true;
      else if (y > 0 && flag[px_idx_flat(x, y - 1, width)] == 0) is_frontier = true;
      else if (y + 1 < height && flag[px_idx_flat(x, y + 1, width)] == 0) is_frontier = true;

      if (is_frontier) {
        // Compute initial T using known neighbors
        float sum_T = 0.0f;
        float sum_w = 0.0f;
        static const int dx[] = {-1, 1, 0, 0};
        static const int dy[] = {0, 0, -1, 1};
        for (int k = 0; k < 4; ++k) {
          int nx = x + dx[k];
          int ny = y + dy[k];
          if (nx >= 0 && nx < width && ny >= 0 && ny < height &&
              flag[px_idx_flat(nx, ny, width)] == 0) {
            float w = 1.0f;
            sum_T += T[px_idx_flat(nx, ny, width)] * w;
            sum_w += w;
          }
        }
        if (sum_w > 0.0f) {
          T[i] = sum_T / sum_w + 1.0f;
        }
        flag[i] = 1;
        pq.emplace(T[i], x, y);
      }
    }
  }

  // Fast marching main loop
  static const int dx[] = {-1, 1, 0, 0};
  static const int dy[] = {0, 0, -1, 1};

  while (!pq.empty()) {
    auto [t_val, x, y] = pq.top();
    pq.pop();
    size_t i = px_idx_flat(x, y, width);
    if (flag[i] == 0) continue;  // already processed

    // Inpaint this pixel using weighted average of known neighbors
    for (int c = 0; c < channels; ++c) {
      float sum = 0.0f;
      float weight_sum = 0.0f;

      for (int k = 0; k < 4; ++k) {
        int nx = x + dx[k];
        int ny = y + dy[k];
        if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;
        size_t ni = px_idx_flat(nx, ny, width);
        if (flag[ni] == 2) continue;  // skip unknown

        float dist = std::abs(T[i] - T[ni]) + 1.0f;
        float w = 1.0f / (dist * dist);

        // Gradient contribution
        float grad_x = 0.0f;
        float grad_y = 0.0f;
        if (nx > 0 && nx + 1 < width) {
          grad_x = image[px_idx(nx + 1, ny, c, width, channels)] -
                   image[px_idx(nx - 1, ny, c, width, channels)];
        }
        if (ny > 0 && ny + 1 < height) {
          grad_y = image[px_idx(nx, ny + 1, c, width, channels)] -
                   image[px_idx(nx, ny - 1, c, width, channels)];
        }

        float contrib = image[px_idx(nx, ny, c, width, channels)] +
                        grad_x * (x - nx) + grad_y * (y - ny);
        sum += contrib * w;
        weight_sum += w;
      }

      if (weight_sum > 0.0f) {
        image[px_idx(x, y, c, width, channels)] =
            std::clamp(sum / weight_sum, 0.0f, 1.0f);
      }
    }

    flag[i] = 0;
    known_mask[i] = 1;

    // Update neighbors
    for (int k = 0; k < 4; ++k) {
      int nx = x + dx[k];
      int ny = y + dy[k];
      if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;
      size_t ni = px_idx_flat(nx, ny, width);
      if (flag[ni] == 0) continue;

      // Solve Eikonal equation for new T
      float T_min_h = std::numeric_limits<float>::max();
      float T_min_v = std::numeric_limits<float>::max();

      if (nx > 0) T_min_h = std::min(T_min_h, T[px_idx_flat(nx - 1, ny, width)]);
      if (nx + 1 < width) T_min_h = std::min(T_min_h, T[px_idx_flat(nx + 1, ny, width)]);
      if (ny > 0) T_min_v = std::min(T_min_v, T[px_idx_flat(nx, ny - 1, width)]);
      if (ny + 1 < height) T_min_v = std::min(T_min_v, T[px_idx_flat(nx, ny + 1, width)]);

      float new_T;
      if (T_min_h == std::numeric_limits<float>::max()) {
        new_T = T_min_v + 1.0f;
      } else if (T_min_v == std::numeric_limits<float>::max()) {
        new_T = T_min_h + 1.0f;
      } else {
        float diff = T_min_h - T_min_v;
        if (std::abs(diff) >= 1.0f) {
          new_T = std::min(T_min_h, T_min_v) + 1.0f;
        } else {
          new_T = (T_min_h + T_min_v +
                   std::sqrt(2.0f - (T_min_h - T_min_v) * (T_min_h - T_min_v))) / 2.0f;
        }
      }

      if (new_T < T[ni]) {
        T[ni] = new_T;
        flag[ni] = 1;
        pq.emplace(new_T, nx, ny);
      }
    }
  }
}

// =============================================================================
// exemplar_inpaint – priority-based exemplar inpainting
// =============================================================================
void exemplar_inpaint(std::vector<float>& image,
                       int width,
                       int height,
                       int channels,
                       const std::vector<float>& mask,
                       const InpaintOptions& options) {
  if (image.empty()) {
    throw std::invalid_argument("exemplar_inpaint: image is empty");
  }
  size_t expected_size = static_cast<size_t>(width) * height * channels;
  if (image.size() != expected_size) {
    throw std::invalid_argument("exemplar_inpaint: image size mismatch");
  }
  if (mask.size() != static_cast<size_t>(width) * height) {
    throw std::invalid_argument("exemplar_inpaint: mask size mismatch");
  }

  std::vector<uint8_t> known_mask;
  init_known_mask(mask, width, height, known_mask);

  if (!has_unknown(known_mask, width, height)) return;

  int patch_size = options.patch_size;
  if (patch_size % 2 == 0) patch_size++;  // ensure odd
  if (patch_size < 3) patch_size = 3;
  int half_patch = patch_size / 2;

  // Confidence map: 1 for known, 0 for unknown
  std::vector<float> confidence(width * height, 0.0f);
  for (int y = 0; y < height; ++y) {
    for (int x = 0; x < width; ++x) {
      confidence[px_idx_flat(x, y, width)] =
          (known_mask[px_idx_flat(x, y, width)] != 0) ? 1.0f : 0.0f;
    }
  }

  // Gradient magnitude
  std::vector<float> grad_mag;
  compute_gradient(image, width, height, channels, grad_mag);

  // Maximum iterations to prevent infinite loops
  int max_iterations = width * height;
  int iteration = 0;

  std::vector<std::pair<int, int>> frontier;
  std::mutex confidence_mutex;

  while (has_unknown(known_mask, width, height) && iteration < max_iterations) {
    ++iteration;

    // Find frontier pixels
    find_frontier(known_mask, width, height, frontier);
    if (frontier.empty()) break;

    // Compute priority for each frontier pixel
    float best_priority = -1.0f;
    int best_x = -1, best_y = -1;

    // Compute per-pixel priority
    for (const auto& [fx, fy] : frontier) {
      // Confidence term: average confidence within the patch
      float conf_sum = 0.0f;
      int conf_count = 0;
      for (int dy = -half_patch; dy <= half_patch; ++dy) {
        int py = fy + dy;
        if (py < 0 || py >= height) continue;
        for (int dx = -half_patch; dx <= half_patch; ++dx) {
          int px = fx + dx;
          if (px < 0 || px >= width) continue;
          conf_sum += confidence[px_idx_flat(px, py, width)];
          ++conf_count;
        }
      }
      float conf_term = (conf_count > 0) ? conf_sum / conf_count : 0.0f;

      // Data term: gradient normal dot product
      float nx, ny;
      compute_boundary_normal(known_mask, width, height, fx, fy, nx, ny);
      float gx = 0.0f, gy = 0.0f;
      // Compute gradient at boundary
      if (fx > 0 && fx + 1 < width) {
        for (int c = 0; c < channels; ++c) {
          gx += image[px_idx(fx + 1, fy, c, width, channels)] -
                image[px_idx(fx - 1, fy, c, width, channels)];
        }
        gx /= channels;
      }
      if (fy > 0 && fy + 1 < height) {
        for (int c = 0; c < channels; ++c) {
          gy += image[px_idx(fx, fy + 1, c, width, channels)] -
                image[px_idx(fx, fy - 1, c, width, channels)];
        }
        gy /= channels;
      }
      float data_term = std::abs(gx * nx + gy * ny);

      float priority = conf_term * data_term;
      if (priority > best_priority) {
        best_priority = priority;
        best_x = fx;
        best_y = fy;
      }
    }

    if (best_x < 0 || best_y < 0) break;

    // Find best matching patch from known region
    float best_ssd = std::numeric_limits<float>::max();
    int best_sx = -1, best_sy = -1;

    int search_step = std::max(1, static_cast<int>(options.search_step));
    for (int sy = half_patch; sy < height - half_patch; sy += search_step) {
      for (int sx = half_patch; sx < width - half_patch; sx += search_step) {
        // Source patch must be entirely known
        bool all_known = true;
        for (int dy = -half_patch; dy <= half_patch && all_known; ++dy) {
          for (int dx = -half_patch; dx <= half_patch && all_known; ++dx) {
            if (known_mask[px_idx_flat(sx + dx, sy + dy, width)] == 0) {
              all_known = false;
            }
          }
        }
        if (!all_known) continue;

        float ssd = patch_ssd(image, width, height, channels,
                              best_x, best_y, sx, sy, half_patch, known_mask);
        if (ssd < best_ssd) {
          best_ssd = ssd;
          best_sx = sx;
          best_sy = sy;
        }
      }
    }

    if (best_sx < 0 || best_sy < 0) {
      // Fallback: use nearest known pixel
      float min_dist = std::numeric_limits<float>::max();
      for (int sy = half_patch; sy < height - half_patch; ++sy) {
        for (int sx = half_patch; sx < width - half_patch; ++sx) {
          if (known_mask[px_idx_flat(sx, sy, width)] == 0) continue;
          float d = static_cast<float>((best_x - sx) * (best_x - sx) +
                                       (best_y - sy) * (best_y - sy));
          if (d < min_dist) {
            min_dist = d;
            best_sx = sx;
            best_sy = sy;
          }
        }
      }
    }

    if (best_sx < 0 || best_sy < 0) break;

    // Copy patch from source to target
    for (int dy = -half_patch; dy <= half_patch; ++dy) {
      int ty = best_y + dy;
      int sy2 = best_sy + dy;
      if (ty < 0 || ty >= height || sy2 < 0 || sy2 >= height) continue;
      for (int dx = -half_patch; dx <= half_patch; ++dx) {
        int tx = best_x + dx;
        int sx2 = best_sx + dx;
        if (tx < 0 || tx >= width || sx2 < 0 || sx2 >= width) continue;
        size_t ti = px_idx_flat(tx, ty, width);
        if (known_mask[ti] != 0) continue;  // only fill unknown pixels

        for (int c = 0; c < channels; ++c) {
          image[px_idx(tx, ty, c, width, channels)] =
              image[px_idx(sx2, sy2, c, width, channels)];
        }
        known_mask[ti] = 1;
        confidence[ti] = confidence[px_idx_flat(best_x, best_y, width)];
      }
    }
  }
}

// =============================================================================
// content_aware_fill – content-aware fill with best matching patch
// =============================================================================
void content_aware_fill(std::vector<float>& image,
                         int width,
                         int height,
                         int channels,
                         const std::vector<float>& mask,
                         const InpaintOptions& options) {
  if (image.empty()) {
    throw std::invalid_argument("content_aware_fill: image is empty");
  }
  size_t expected_size = static_cast<size_t>(width) * height * channels;
  if (image.size() != expected_size) {
    throw std::invalid_argument("content_aware_fill: image size mismatch");
  }
  if (mask.size() != static_cast<size_t>(width) * height) {
    throw std::invalid_argument("content_aware_fill: mask size mismatch");
  }

  std::vector<uint8_t> known_mask;
  init_known_mask(mask, width, height, known_mask);

  if (!has_unknown(known_mask, width, height)) return;

  int patch_size = options.patch_size;
  if (patch_size % 2 == 0) patch_size++;
  if (patch_size < 3) patch_size = 3;
  int half_patch = patch_size / 2;

  // Multi-scale approach: start with larger patches, reduce
  std::vector<int> scales = {patch_size, std::max(3, patch_size / 2)};

  for (int current_ps : scales) {
    int hp = current_ps / 2;

    // Build a list of all unknown pixel positions
    std::vector<std::pair<int, int>> unknown_pixels;
    for (int y = 0; y < height; ++y) {
      for (int x = 0; x < width; ++x) {
        if (known_mask[px_idx_flat(x, y, width)] == 0) {
          unknown_pixels.emplace_back(x, y);
        }
      }
    }
    if (unknown_pixels.empty()) break;

    // Sort unknown pixels by distance to known region (fill from boundary inward)
    std::vector<float> dist_to_known(width * height, std::numeric_limits<float>::max());
    for (int y = 0; y < height; ++y) {
      for (int x = 0; x < width; ++x) {
        if (known_mask[px_idx_flat(x, y, width)] != 0) {
          dist_to_known[px_idx_flat(x, y, width)] = 0.0f;
        }
      }
    }
    distance_transform_approx(dist_to_known, width, height);

    std::sort(unknown_pixels.begin(), unknown_pixels.end(),
              [&](const auto& a, const auto& b) {
                return dist_to_known[px_idx_flat(a.first, a.second, width)] <
                       dist_to_known[px_idx_flat(b.first, b.second, width)];
              });

    // Fill unknown pixels
    for (const auto& [ux, uy] : unknown_pixels) {
      size_t ui = px_idx_flat(ux, uy, width);
      if (known_mask[ui] != 0) continue;  // already filled

      // Find best matching patch centered at this pixel
      float best_ssd = std::numeric_limits<float>::max();
      float best_val[4] = {0.0f, 0.0f, 0.0f, 0.0f};

      int search_step = std::max(1, static_cast<int>(options.search_step));
      for (int sy = hp; sy < height - hp; sy += search_step) {
        for (int sx = hp; sx < width - hp; sx += search_step) {
          // Check if source patch is fully known
          bool all_known = true;
          for (int dy = -hp; dy <= hp && all_known; ++dy) {
            for (int dx = -hp; dx <= hp && all_known; ++dx) {
              if (known_mask[px_idx_flat(sx + dx, sy + dy, width)] == 0) {
                all_known = false;
              }
            }
          }
          if (!all_known) continue;

          // Compute SSD between target patch (known pixels only) and source patch
          float ssd = 0.0f;
          int count = 0;
          for (int dy = -hp; dy <= hp; ++dy) {
            int ty = uy + dy;
            int sy2 = sy + dy;
            if (ty < 0 || ty >= height || sy2 < 0 || sy2 >= height) continue;
            for (int dx = -hp; dx <= hp; ++dx) {
              int tx = ux + dx;
              int sx2 = sx + dx;
              if (tx < 0 || tx >= width || sx2 < 0 || sx2 >= width) continue;
              if (known_mask[px_idx_flat(tx, ty, width)] == 0) continue;

              for (int c = 0; c < channels; ++c) {
                float diff = image[px_idx(tx, ty, c, width, channels)] -
                             image[px_idx(sx2, sy2, c, width, channels)];
                ssd += diff * diff;
              }
              ++count;
            }
          }

          if (count > 0) {
            ssd /= count;
            if (ssd < best_ssd) {
              best_ssd = ssd;
              for (int c = 0; c < channels; ++c) {
                best_val[c] = image[px_idx(sx, sy, c, width, channels)];
              }
            }
          }
        }
      }

      // Fill with best match
      if (best_ssd < std::numeric_limits<float>::max()) {
        for (int c = 0; c < channels; ++c) {
          image[px_idx(ux, uy, c, width, channels)] =
              std::clamp(best_val[c], 0.0f, 1.0f);
        }
        known_mask[ui] = 1;
      }
    }
  }
}

// =============================================================================
// inpaint_image – main entry point / dispatcher
// =============================================================================
void inpaint_image(std::vector<float>& image,
                   int width,
                   int height,
                   int channels,
                   const std::vector<float>& mask,
                   InpaintMethod method,
                   const InpaintOptions& options) {
  if (image.empty()) {
    throw std::invalid_argument("inpaint_image: image is empty");
  }
  if (channels < 1 || channels > 4) {
    throw std::invalid_argument("inpaint_image: channels must be 1-4");
  }
  if (mask.size() != static_cast<size_t>(width) * height) {
    throw std::invalid_argument("inpaint_image: mask size mismatch");
  }
  if (width <= 0 || height <= 0) {
    throw std::invalid_argument("inpaint_image: invalid dimensions");
  }

  // Check if mask has any content to inpaint
  bool has_mask = false;
  for (size_t i = 0; i < mask.size(); ++i) {
    if (mask[i] > 0.5f) {
      has_mask = true;
      break;
    }
  }
  if (!has_mask) return;

  switch (method) {
    case InpaintMethod::CLONE_HEAL: {
      auto [xmin, ymin, xmax, ymax] = mask_bounding_box(mask, width, height);
      int rw = xmax - xmin + 1;
      int rh = ymax - ymin + 1;
      if (rw <= 0 || rh <= 0) break;
      // For dispatch, use mask center as source offset
      int src_x = std::max(0, xmin - rw / 2);
      int src_y = std::max(0, ymin - rh / 2);
      clone_heal(image, width, height, channels, mask,
                 src_x, src_y, xmin, ymin, rw, rh, options);
      break;
    }
    case InpaintMethod::FAST_MARCHING:
      fast_marching_inpaint(image, width, height, channels, mask, options);
      break;
    case InpaintMethod::EXEMPLAR_PRIORITY:
      exemplar_inpaint(image, width, height, channels, mask, options);
      break;
    case InpaintMethod::CONTENT_AWARE:
      content_aware_fill(image, width, height, channels, mask, options);
      break;
  }
}

// =============================================================================
// mask_bounding_box – compute bounding box of mask region
// =============================================================================
std::tuple<int, int, int, int> mask_bounding_box(const std::vector<float>& mask,
                                                  int width,
                                                  int height,
                                                  float threshold) {
  if (mask.empty()) {
    return std::make_tuple(0, 0, -1, -1);
  }

  int xmin = width, ymin = height, xmax = -1, ymax = -1;
  for (int y = 0; y < height; ++y) {
    for (int x = 0; x < width; ++x) {
      if (mask[px_idx_flat(x, y, width)] > threshold) {
        if (x < xmin) xmin = x;
        if (y < ymin) ymin = y;
        if (x > xmax) xmax = x;
        if (y > ymax) ymax = y;
      }
    }
  }

  return std::make_tuple(xmin, ymin, xmax, ymax);
}

}  // namespace inpaint
}  // namespace alcedo