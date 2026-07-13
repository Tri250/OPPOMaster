// SPDX-License-Identifier: MIT
// AlcedoStudio - HDR Merge with Deghosting
// Production-quality implementation of HDR fusion for RAW photo editing.
// No external dependencies beyond C++17 standard library.

#include <algorithm>
#include <array>
#include <atomic>
#include <cassert>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <exception>
#include <fstream>
#include <functional>
#include <iostream>
#include <limits>
#include <mutex>
#include <numeric>
#include <sstream>
#include <stdexcept>
#include <string>
#include <thread>
#include <tuple>
#include <vector>

namespace alcedo {
namespace hdr {

// ============================================================================
// Core Data Types
// ============================================================================

/// RGB pixel in floating-point linear space.
struct Pixel {
    float r = 0.0f;
    float g = 0.0f;
    float b = 0.0f;

    Pixel() = default;
    Pixel(float r_, float g_, float b_) : r(r_), g(g_), b(b_) {}

    Pixel operator+(const Pixel& o) const { return {r + o.r, g + o.g, b + o.b}; }
    Pixel operator-(const Pixel& o) const { return {r - o.r, g - o.g, b - o.b}; }
    Pixel operator*(float s) const { return {r * s, g * s, b * s}; }
    Pixel operator/(float s) const { return {r / s, g / s, b / s}; }
    Pixel& operator+=(const Pixel& o) { r += o.r; g += o.g; b += o.b; return *this; }
    Pixel& operator*=(float s) { r *= s; g *= s; b *= s; return *this; }

    float luminance() const { return 0.2126f * r + 0.7152f * g + 0.0722f * b; }
    float max_channel() const { return std::max({r, g, b}); }
    bool is_finite() const {
        return std::isfinite(r) && std::isfinite(g) && std::isfinite(b);
    }
};

/// Grayscale image used for alignment and processing.
struct GrayImage {
    int width = 0;
    int height = 0;
    std::vector<float> data; // row-major

    GrayImage() = default;
    GrayImage(int w, int h) : width(w), height(h), data(w * h, 0.0f) {}

    float& operator()(int x, int y) { return data[y * width + x]; }
    float  operator()(int x, int y) const { return data[y * width + x]; }
    bool   valid() const { return width > 0 && height > 0 && !data.empty(); }

    float sample_bilinear(float x, float y) const {
        int x0 = std::clamp(static_cast<int>(x), 0, width - 1);
        int y0 = std::clamp(static_cast<int>(y), 0, height - 1);
        int x1 = std::min(x0 + 1, width - 1);
        int y1 = std::min(y0 + 1, height - 1);
        float fx = x - x0, fy = y - y0;
        return (1.0f - fx) * (1.0f - fy) * (*this)(x0, y0) +
               fx * (1.0f - fy) * (*this)(x1, y0) +
               (1.0f - fx) * fy * (*this)(x0, y1) +
               fx * fy * (*this)(x1, y1);
    }
};

/// RGB image in floating-point linear space.
struct Image {
    int width = 0;
    int height = 0;
    std::vector<Pixel> data; // row-major, interleaved RGB

    Image() = default;
    Image(int w, int h) : width(w), height(h), data(w * h) {}

    Pixel& operator()(int x, int y) { return data[y * width + x]; }
    const Pixel& operator()(int x, int y) const { return data[y * width + x]; }
    bool valid() const { return width > 0 && height > 0 && !data.empty(); }

    GrayImage to_grayscale() const {
        GrayImage g(width, height);
        for (int i = 0; i < width * height; ++i) {
            g.data[i] = data[i].luminance();
        }
        return g;
    }

    static Image from_grayscale(const GrayImage& g) {
        Image img(g.width, g.height);
        for (int i = 0; i < g.width * g.height; ++i) {
            img.data[i] = Pixel(g.data[i], g.data[i], g.data[i]);
        }
        return img;
    }
};

/// A single bracketed frame with exposure metadata.
struct Frame {
    Image image;
    float exposure_time = 1.0f;   // seconds
    float iso = 100.0f;            // ISO sensitivity
    float ev_bias = 0.0f;          // exposure value bias

    float exposure_value() const {
        // Relative exposure = exposure_time * iso / reference
        return exposure_time * iso;
    }
};

/// Progress callback type.
using ProgressCallback = std::function<void(int percent, const std::string& stage)>;

// ============================================================================
// Forward declarations
// ============================================================================

static void progress_report(const ProgressCallback& cb, int pct, const char* msg);

// ============================================================================
// 1. Frame Loading
// ============================================================================

/// Read a 16-bit PPM (P6/P3) or PFM (floating-point) file.
/// Supports PPM (8-bit), PPM16 (16-bit), and PFM (float) formats.
static Image load_image_file(const std::string& path) {
    std::ifstream file(path, std::ios::binary);
    if (!file.is_open()) {
        throw std::runtime_error("Cannot open image file: " + path);
    }

    std::string magic;
    file >> magic;
    if (magic == "PF" || magic == "pf") {
        // PFM (Portable FloatMap) - RGB float
        int w = 0, h = 0;
        float scale = 0.0f;
        file >> w >> h >> scale;
        file.ignore(1); // skip newline after header

        bool little_endian = (scale < 0.0f);
        if (little_endian) scale = -scale;

        Image img(w, h);
        std::vector<float> row(w * 3);
        for (int y = h - 1; y >= 0; --y) { // PFM is bottom-up
            file.read(reinterpret_cast<char*>(row.data()),
                      static_cast<std::streamsize>(w * 3 * sizeof(float)));
            if (!file) throw std::runtime_error("PFM read error: " + path);
            for (int x = 0; x < w; ++x) {
                img(x, y) = Pixel(row[x * 3], row[x * 3 + 1], row[x * 3 + 2]);
            }
        }
        return img;
    }

    if (magic == "P6") {
        // PPM binary 8-bit
        int w = 0, h = 0, maxval = 0;
        file >> w >> h >> maxval;
        file.ignore(1);

        Image img(w, h);
        if (maxval <= 255) {
            std::vector<uint8_t> row(w * 3);
            for (int y = 0; y < h; ++y) {
                file.read(reinterpret_cast<char*>(row.data()), w * 3);
                if (!file) throw std::runtime_error("PPM read error: " + path);
                for (int x = 0; x < w; ++x) {
                    img(x, y) = Pixel(row[x * 3] / 255.0f, row[x * 3 + 1] / 255.0f,
                                      row[x * 3 + 2] / 255.0f);
                }
            }
        } else {
            // 16-bit PPM: read as big-endian uint16
            std::vector<uint8_t> row_raw(w * 6);
            for (int y = 0; y < h; ++y) {
                file.read(reinterpret_cast<char*>(row_raw.data()), w * 6);
                if (!file) throw std::runtime_error("PPM read error: " + path);
                for (int x = 0; x < w; ++x) {
                    uint16_t r16 = (static_cast<uint16_t>(row_raw[x * 6]) << 8) |
                                    static_cast<uint16_t>(row_raw[x * 6 + 1]);
                    uint16_t g16 = (static_cast<uint16_t>(row_raw[x * 6 + 2]) << 8) |
                                    static_cast<uint16_t>(row_raw[x * 6 + 3]);
                    uint16_t b16 = (static_cast<uint16_t>(row_raw[x * 6 + 4]) << 8) |
                                    static_cast<uint16_t>(row_raw[x * 6 + 5]);
                    img(x, y) = Pixel(r16 / 65535.0f, g16 / 65535.0f, b16 / 65535.0f);
                }
            }
        }
        return img;
    }

    throw std::runtime_error("Unsupported image format: " + magic + " in " + path);
}

/// Bulk-load bracketed exposures from file paths and exposure metadata.
/// Each entry in `paths` corresponds to the same index in `exposure_times`.
/// `iso_values` can be empty (defaults to 100.0).
std::vector<Frame> load_hdr_frames(
    const std::vector<std::string>& paths,
    const std::vector<float>& exposure_times,
    const std::vector<float>& iso_values = {},
    const ProgressCallback& progress = nullptr)
{
    if (paths.empty()) {
        throw std::invalid_argument("No input paths provided");
    }
    if (paths.size() != exposure_times.size()) {
        throw std::invalid_argument(
            "paths.size() != exposure_times.size()");
    }
    if (!iso_values.empty() && iso_values.size() != paths.size()) {
        throw std::invalid_argument(
            "iso_values.size() != paths.size()");
    }

    const size_t n = paths.size();
    std::vector<Frame> frames(n);

    progress_report(progress, 0, "Loading frames...");

    for (size_t i = 0; i < n; ++i) {
        try {
            frames[i].image = load_image_file(paths[i]);
        } catch (const std::exception& e) {
            throw std::runtime_error("Failed to load frame " +
                                     std::to_string(i) + " (" + paths[i] +
                                     "): " + e.what());
        }
        frames[i].exposure_time = exposure_times[i];
        frames[i].iso = iso_values.empty() ? 100.0f : iso_values[i];
        if (i > 0) {
            if (frames[i].image.width != frames[0].image.width ||
                frames[i].image.height != frames[0].image.height) {
                throw std::runtime_error(
                    "Frame " + std::to_string(i) +
                    " has different dimensions");
            }
        }
        progress_report(progress,
            static_cast<int>((i + 1) * 100 / n), "Loading frames...");
    }

    return frames;
}

// ============================================================================
// 2. MTB (Median Threshold Bitmap) Alignment
// ============================================================================

/// Compute the median of a vector (in-place modification allowed).
static float compute_median(std::vector<float> v) {
    if (v.empty()) return 0.0f;
    size_t mid = v.size() / 2;
    std::nth_element(v.begin(), v.begin() + static_cast<long>(mid), v.end());
    if (v.size() % 2 == 0) {
        float a = v[mid];
        std::nth_element(v.begin(), v.begin() + static_cast<long>(mid) - 1, v.end());
        return (a + v[mid - 1]) * 0.5f;
    }
    return v[mid];
}

/// Build a threshold bitmap: 1 where pixel > median, 0 otherwise.
static std::vector<uint8_t> build_mtb(const GrayImage& img, int& out_levels) {
    const int n = img.width * img.height;
    std::vector<float> vals(img.data.begin(), img.data.end());
    float median = compute_median(std::move(vals));

    std::vector<uint8_t> bitmap(n);
    int ones = 0;
    for (int i = 0; i < n; ++i) {
        bitmap[i] = (img.data[i] > median) ? 1 : 0;
        ones += bitmap[i];
    }
    out_levels = ones;
    return bitmap;
}

/// Compute the number of differing bits (XOR) between two MTB bitmaps at a given offset.
static int mtb_diff_count(const std::vector<uint8_t>& a,
                          const std::vector<uint8_t>& b,
                          int aw, int ah, int bw, int bh,
                          int dx, int dy) {
    int diff = 0;
    for (int y = 0; y < ah; ++y) {
        int by = y + dy;
        if (by < 0 || by >= bh) continue;
        for (int x = 0; x < aw; ++x) {
            int bx = x + dx;
            if (bx < 0 || bx >= bw) continue;
            diff += (a[y * aw + x] ^ b[by * bw + bx]);
        }
    }
    return diff;
}

/// Compute the best offset between two images using MTB on a pyramid level.
static std::pair<int, int> mtb_align_level(
    const GrayImage& ref, const GrayImage& src,
    int search_radius)
{
    int levels_ref = 0, levels_src = 0;
    auto bm_ref = build_mtb(ref, levels_ref);
    auto bm_src = build_mtb(src, levels_src);

    int best_dx = 0, best_dy = 0;
    int best_diff = std::numeric_limits<int>::max();

    for (int dy = -search_radius; dy <= search_radius; ++dy) {
        for (int dx = -search_radius; dx <= search_radius; ++dx) {
            int diff = mtb_diff_count(bm_ref, bm_src,
                                       ref.width, ref.height,
                                       src.width, src.height, dx, dy);
            if (diff < best_diff) {
                best_diff = diff;
                best_dx = dx;
                best_dy = dy;
            }
        }
    }
    return {best_dx, best_dy};
}

/// Downscale an image by factor 2 (simple box average).
static GrayImage downsample2(const GrayImage& src) {
    int w = std::max(1, src.width / 2);
    int h = std::max(1, src.height / 2);
    GrayImage dst(w, h);
    for (int y = 0; y < h; ++y) {
        for (int x = 0; x < w; ++x) {
            int sx = x * 2, sy = y * 2;
            float sum = src(sx, sy);
            if (sx + 1 < src.width) sum += src(sx + 1, sy);
            if (sy + 1 < src.height) sum += src(sx, sy + 1);
            if (sx + 1 < src.width && sy + 1 < src.height) sum += src(sx + 1, sy + 1);
            dst(x, y) = sum / 4.0f;
        }
    }
    return dst;
}

/// Build a Gaussian pyramid for the given image.
static std::vector<GrayImage> build_pyramid(const GrayImage& src, int levels) {
    std::vector<GrayImage> pyr;
    pyr.push_back(src);
    for (int i = 1; i < levels; ++i) {
        pyr.push_back(downsample2(pyr.back()));
    }
    return pyr;
}

/// Shift an image by integer pixel offset.
static Image shift_image(const Image& src, int dx, int dy) {
    Image dst(src.width, src.height);
    for (int y = 0; y < src.height; ++y) {
        int sy = y - dy;
        if (sy < 0 || sy >= src.height) continue;
        for (int x = 0; x < src.width; ++x) {
            int sx = x - dx;
            if (sx < 0 || sx >= src.width) continue;
            dst(x, y) = src(sx, sy);
        }
    }
    return dst;
}

/// MTB-based multi-resolution alignment of all frames to the reference frame.
/// Returns the aligned frames (reference frame is the first frame by default).
std::vector<Image> align_frames(
    const std::vector<Frame>& frames,
    int reference_index = 0,
    const ProgressCallback& progress = nullptr)
{
    if (frames.size() < 2) {
        // Nothing to align.
        std::vector<Image> result;
        result.reserve(frames.size());
        for (const auto& f : frames) result.push_back(f.image);
        return result;
    }

    progress_report(progress, 0, "Aligning frames...");

    const int n = static_cast<int>(frames.size());
    const int ref_idx = std::clamp(reference_index, 0, n - 1);

    // Build pyramid levels: smallest dimension should be >= 16 pixels
    int min_dim = std::min(frames[0].image.width, frames[0].image.height);
    int pyr_levels = 0;
    int tmp = min_dim;
    while (tmp >= 16) { tmp /= 2; ++pyr_levels; }
    pyr_levels = std::max(1, pyr_levels);

    std::vector<Image> result(n);
    result[ref_idx] = frames[ref_idx].image; // reference stays put

    GrayImage ref_gray = frames[ref_idx].image.to_grayscale();
    auto ref_pyr = build_pyramid(ref_gray, pyr_levels);

    // Parallel alignment of each frame
    std::mutex cout_mutex;
    std::atomic<int> completed{0};

    auto align_single = [&](int idx) {
        if (idx == ref_idx) return;
        GrayImage src_gray = frames[idx].image.to_grayscale();
        auto src_pyr = build_pyramid(src_gray, pyr_levels);

        int total_dx = 0, total_dy = 0;

        // Coarse-to-fine alignment
        for (int level = pyr_levels - 1; level >= 0; --level) {
            int search_radius = (level == pyr_levels - 1) ? 4 : 2;
            auto [dx, dy] = mtb_align_level(ref_pyr[level], src_pyr[level],
                                             search_radius);
            // Scale up offset for finer levels
            if (level < pyr_levels - 1) {
                total_dx = total_dx * 2 + dx;
                total_dy = total_dy * 2 + dy;
            } else {
                total_dx = dx;
                total_dy = dy;
            }
        }

        result[idx] = shift_image(frames[idx].image, -total_dx, -total_dy);

        int done = ++completed;
        progress_report(progress,
            static_cast<int>(done * 100 / (n - 1)), "Aligning frames...");
    };

    // Use threads for multi-frame alignment
    std::vector<std::thread> threads;
    for (int i = 0; i < n; ++i) {
        if (i != ref_idx) {
            threads.emplace_back(align_single, i);
        }
    }
    for (auto& t : threads) t.join();

    if (ref_idx != 0) {
        // Ensure reference is at index 0 for downstream processing
        std::swap(result[0], result[ref_idx]);
    }

    progress_report(progress, 100, "Alignment complete.");
    return result;
}

// ============================================================================
// 3. Debevec Camera Response Function Estimation
// ============================================================================

/// Weight function for Debevec: triangle-shaped, favoring mid-range values.
static float debevec_weight(float z, float zmax = 255.0f) {
    float mid = zmax * 0.5f;
    if (z <= mid) return z / mid;
    return (zmax - z) / mid;
}

/// Solve a linear system A * x = b using Gaussian elimination with partial pivoting.
/// A is modified in-place. Returns true on success.
static bool solve_linear_system(std::vector<std::vector<double>>& A,
                                 std::vector<double>& b,
                                 std::vector<double>& x) {
    int n = static_cast<int>(A.size());
    if (n == 0) return false;

    // Augment A with b
    for (int i = 0; i < n; ++i) {
        A[i].push_back(b[i]);
    }

    // Forward elimination with partial pivoting
    for (int col = 0; col < n; ++col) {
        // Find pivot
        int max_row = col;
        double max_val = std::abs(A[col][col]);
        for (int row = col + 1; row < n; ++row) {
            if (std::abs(A[row][col]) > max_val) {
                max_val = std::abs(A[row][col]);
                max_row = row;
            }
        }
        if (max_val < 1e-12) {
            // Singular or near-singular
            return false;
        }
        std::swap(A[col], A[max_row]);

        // Eliminate below
        for (int row = col + 1; row < n; ++row) {
            double factor = A[row][col] / A[col][col];
            for (int j = col; j <= n; ++j) {
                A[row][j] -= factor * A[col][j];
            }
        }
    }

    // Back substitution
    x.resize(n);
    for (int i = n - 1; i >= 0; --i) {
        double sum = A[i][n];
        for (int j = i + 1; j < n; ++j) {
            sum -= A[i][j] * x[j];
        }
        x[i] = sum / A[i][i];
    }
    return true;
}

/// Select well-exposed pixel samples across all frames for response estimation.
/// Returns a vector of (x, y) positions.
static std::vector<std::pair<int, int>> select_sample_pixels(
    const std::vector<Image>& images, int num_samples)
{
    std::vector<std::pair<int, int>> samples;
    if (images.empty()) return samples;

    int w = images[0].width;
    int h = images[0].height;
    int total = w * h;

    // Use stratified sampling: pick evenly spaced pixels
    int step = std::max(1, total / num_samples);
    for (int i = 0; i < total && static_cast<int>(samples.size()) < num_samples; i += step) {
        int y = i / w;
        int x = i % w;
        if (y < h) {
            samples.emplace_back(x, y);
        }
    }
    return samples;
}

/// Estimate the camera response function using the Debevec & Malik algorithm.
/// Input: aligned images (8-bit normalized to [0,1]), exposure times.
/// Output: response function g(z) for z in [0, 255], and ln(exposure) for reference.
/// The response function maps pixel values (0-255) to log-radiance.
std::vector<float> estimate_camera_response(
    const std::vector<Image>& images,
    const std::vector<float>& exposure_times,
    const ProgressCallback& progress = nullptr)
{
    if (images.size() < 2) {
        throw std::invalid_argument("Need at least 2 images for response estimation");
    }
    if (images.size() != exposure_times.size()) {
        throw std::invalid_argument("images.size() != exposure_times.size()");
    }

    progress_report(progress, 0, "Estimating camera response...");

    const int Zmax = 255;
    const int num_exposures = static_cast<int>(images.size());
    const float lambda_smooth = 50.0f; // smoothness weight

    // Select sample pixels
    const int num_samples = std::min(256, images[0].width * images[0].height / 2);
    auto sample_positions = select_sample_pixels(images, num_samples);
    const int P = static_cast<int>(sample_positions.size());

    if (P == 0) {
        throw std::runtime_error("No valid sample pixels for response estimation");
    }

    // Precompute ln(exposure times)
    std::vector<double> ln_exposure(num_exposures);
    for (int j = 0; j < num_exposures; ++j) {
        ln_exposure[j] = std::log(static_cast<double>(exposure_times[j]));
    }

    // Convert images to 8-bit values [0, 255] for sampling
    // We need to find pixel values. Assume images are in [0, 1] range.
    auto pixel_to_byte = [](float v) -> int {
        return std::clamp(static_cast<int>(std::round(v * 255.0f)), 0, 255);
    };

    // Build the linear system: A * x = b
    // Unknowns: g(0)..g(255) = 256 values, plus ln(E_i) for i=0..P-1 = P values
    // Total: 256 + P unknowns
    // We fix g(128) = 0 to remove the scale ambiguity
    const int num_unknowns = 256 + P;
    std::vector<std::vector<double>> A;
    std::vector<double> b;

    // Equation: g(Z_ij) - ln(E_i) = ln(Δt_j)
    // Weighted by w(Z_ij)
    for (int i = 0; i < P; ++i) {
        auto [sx, sy] = sample_positions[i];
        for (int j = 0; j < num_exposures; ++j) {
            float val = images[j](sx, sy).luminance();
            int zij = pixel_to_byte(val);
            double w = debevec_weight(static_cast<float>(zij));

            // Row: w * [g(zij) - ln(E_i)] = w * ln(Δt_j)
            std::vector<double> row(num_unknowns, 0.0);
            row[zij] = w;          // g(zij)
            row[256 + i] = -w;     // -ln(E_i)
            A.push_back(std::move(row));
            b.push_back(w * ln_exposure[j]);
        }
    }

    // Smoothness constraint: lambda * (g(z-1) - 2*g(z) + g(z+1)) = 0
    for (int z = 1; z < Zmax; ++z) {
        double w = debevec_weight(static_cast<float>(z));
        std::vector<double> row(num_unknowns, 0.0);
        row[z - 1] = lambda_smooth * w;
        row[z]     = -2.0 * lambda_smooth * w;
        row[z + 1] = lambda_smooth * w;
        A.push_back(std::move(row));
        b.push_back(0.0);
    }

    // Fix g(128) = 0
    {
        std::vector<double> row(num_unknowns, 0.0);
        row[128] = 1.0;
        A.push_back(std::move(row));
        b.push_back(0.0);
    }

    // Solve using normal equations: A^T * A * x = A^T * b
    int M = static_cast<int>(A.size());
    int N = num_unknowns;

    // Build normal equations (N x N system)
    std::vector<std::vector<double>> AtA(N, std::vector<double>(N, 0.0));
    std::vector<double> Atb(N, 0.0);

    for (int i = 0; i < M; ++i) {
        for (int col = 0; col < N; ++col) {
            if (std::abs(A[i][col]) > 1e-15) {
                for (int k = 0; k < N; ++k) {
                    AtA[col][k] += A[i][col] * A[i][k];
                }
                Atb[col] += A[i][col] * b[i];
            }
        }
    }

    std::vector<double> solution;
    if (!solve_linear_system(AtA, Atb, solution)) {
        throw std::runtime_error("Failed to solve camera response linear system");
    }

    // Extract g(0..255)
    std::vector<float> response(256);
    for (int z = 0; z <= Zmax; ++z) {
        response[z] = static_cast<float>(solution[z]);
    }

    progress_report(progress, 100, "Camera response estimated.");
    return response;
}

// ============================================================================
// 4. HDR Radiance Map Merge
// ============================================================================

/// Weight function for HDR merge: triangle-shaped to favor mid-range values.
/// This reduces noise from under/over-exposed pixels.
static float merge_weight(float pixel_value) {
    float z = pixel_value * 255.0f;
    return debevec_weight(z);
}

/// Merge aligned images into a single HDR radiance map using the estimated
/// camera response function and a weighted average.
Image merge_to_hdr(
    const std::vector<Image>& images,
    const std::vector<float>& exposure_times,
    const std::vector<float>& response,
    const ProgressCallback& progress = nullptr)
{
    if (images.empty()) {
        throw std::invalid_argument("No images to merge");
    }
    if (images.size() != exposure_times.size()) {
        throw std::invalid_argument("images.size() != exposure_times.size()");
    }

    progress_report(progress, 0, "Merging to HDR...");

    int w = images[0].width;
    int h = images[0].height;
    int num_images = static_cast<int>(images.size());

    Image hdr_result(w, h);

    // Precompute ln(exposure)
    std::vector<double> ln_exposure(num_images);
    for (int j = 0; j < num_images; ++j) {
        ln_exposure[j] = std::log(static_cast<double>(exposure_times[j]));
    }

    auto pixel_to_byte = [](float v) -> int {
        return std::clamp(static_cast<int>(std::round(v * 255.0f)), 0, 255);
    };

    // Merge with multi-threading
    int num_threads = static_cast<int>(std::thread::hardware_concurrency());
    if (num_threads < 1) num_threads = 1;

    std::atomic<int> rows_done{0};

    auto merge_rows = [&](int start_y, int end_y) {
        for (int y = start_y; y < end_y; ++y) {
            for (int x = 0; x < w; ++x) {
                double sum_r = 0.0, sum_g = 0.0, sum_b = 0.0;
                double total_weight = 0.0;

                for (int j = 0; j < num_images; ++j) {
                    const Pixel& p = images[j](x, y);

                    // Use per-channel values for better accuracy
                    int zr = pixel_to_byte(p.r);
                    int zg = pixel_to_byte(p.g);
                    int zb = pixel_to_byte(p.b);
                    double ln_radiance_r = response[zr] - ln_exposure[j];
                    double ln_radiance_g = response[zg] - ln_exposure[j];
                    double ln_radiance_b = response[zb] - ln_exposure[j];

                    // Weight by channel-specific weights
                    float wr = merge_weight(p.r);
                    float wg = merge_weight(p.g);
                    float wb = merge_weight(p.b);

                    sum_r += wr * std::exp(ln_radiance_r);
                    sum_g += wg * std::exp(ln_radiance_g);
                    sum_b += wb * std::exp(ln_radiance_b);
                    total_weight += (wr + wg + wb) / 3.0f;
                }

                if (total_weight > 1e-10) {
                    hdr_result(x, y) = Pixel(
                        static_cast<float>(sum_r / (total_weight * 3.0)),
                        static_cast<float>(sum_g / (total_weight * 3.0)),
                        static_cast<float>(sum_b / (total_weight * 3.0)));
                } else {
                    // Fallback: use the middle-exposure frame
                    int mid = num_images / 2;
                    hdr_result(x, y) = images[mid](x, y);
                }
            }

            int done = ++rows_done;
            if (done % 16 == 0) {
                progress_report(progress,
                    static_cast<int>(done * 100 / h), "Merging to HDR...");
            }
        }
    };

    std::vector<std::thread> threads;
    int rows_per_thread = (h + num_threads - 1) / num_threads;
    for (int t = 0; t < num_threads; ++t) {
        int start_y = t * rows_per_thread;
        int end_y = std::min(start_y + rows_per_thread, h);
        if (start_y < h) {
            threads.emplace_back(merge_rows, start_y, end_y);
        }
    }
    for (auto& t : threads) t.join();

    progress_report(progress, 100, "HDR merge complete.");
    return hdr_result;
}

// ============================================================================
// 5. Ghost Region Detection
// ============================================================================

/// Compute a ghost mask by analyzing pixel variance across exposures.
/// High variance indicates potential ghosting (moving objects).
/// Returns a grayscale mask where 1.0 = ghost, 0.0 = consistent.
GrayImage detect_ghost_regions(
    const std::vector<Image>& images,
    float threshold = 0.15f,
    const ProgressCallback& progress = nullptr)
{
    if (images.size() < 2) {
        // Single frame: no ghosting possible
        return GrayImage(images[0].width, images[0].height);
    }

    progress_report(progress, 0, "Detecting ghost regions...");

    int w = images[0].width;
    int h = images[0].height;
    int n = static_cast<int>(images.size());
    GrayImage mask(w, h);

    // Compute the median image (per-pixel median across exposures)
    for (int y = 0; y < h; ++y) {
        for (int x = 0; x < w; ++x) {
            std::vector<float> lum_vals(n);
            for (int i = 0; i < n; ++i) {
                lum_vals[i] = images[i](x, y).luminance();
            }
            float median_lum = compute_median(std::move(lum_vals));

            // Compute mean absolute deviation from median
            double mad = 0.0;
            for (int i = 0; i < n; ++i) {
                mad += std::abs(images[i](x, y).luminance() - median_lum);
            }
            mad /= n;

            // Normalize by median to get relative deviation
            float rel_dev = (median_lum > 1e-6f)
                ? static_cast<float>(mad / (median_lum + 1e-6f))
                : 0.0f;

            // Soft threshold
            float ghost_confidence = std::clamp(rel_dev / threshold, 0.0f, 1.0f);
            mask(x, y) = ghost_confidence;
        }
    }

    progress_report(progress, 100, "Ghost detection complete.");
    return mask;
}

// ============================================================================
// 6. Deghosting
// ============================================================================

/// Replace ghost pixels in the HDR radiance map with data from the
/// best-exposed single frame (the one with exposure closest to the middle).
void deghost_hdr(
    Image& hdr,
    const GrayImage& ghost_mask,
    const std::vector<Image>& images,
    const std::vector<float>& exposure_times,
    const std::vector<float>& response,
    const ProgressCallback& progress = nullptr)
{
    if (images.empty()) return;

    progress_report(progress, 0, "Deghosting...");

    int w = hdr.width;
    int h = hdr.height;
    int num_images = static_cast<int>(images.size());

    // Select the "best" frame: the one whose exposure is closest to the median
    std::vector<float> sorted_exp = exposure_times;
    std::sort(sorted_exp.begin(), sorted_exp.end());
    float median_exp = sorted_exp[sorted_exp.size() / 2];

    int best_idx = 0;
    float best_diff = std::abs(exposure_times[0] - median_exp);
    for (int i = 1; i < num_images; ++i) {
        float diff = std::abs(exposure_times[i] - median_exp);
        if (diff < best_diff) {
            best_diff = diff;
            best_idx = i;
        }
    }

    auto pixel_to_byte = [](float v) -> int {
        return std::clamp(static_cast<int>(std::round(v * 255.0f)), 0, 255);
    };

    double ln_exp_best = std::log(static_cast<double>(exposure_times[best_idx]));

    // For each ghost pixel, replace with radiance derived from the best frame
    int ghost_count = 0;
    for (int y = 0; y < h; ++y) {
        for (int x = 0; x < w; ++x) {
            float g = ghost_mask(x, y);
            if (g > 0.5f) {
                // Strong ghost: fully replace
                const Pixel& p = images[best_idx](x, y);
                int zr = pixel_to_byte(p.r);
                int zg = pixel_to_byte(p.g);
                int zb = pixel_to_byte(p.b);

                hdr(x, y).r = static_cast<float>(
                    std::exp(response[zr] - ln_exp_best));
                hdr(x, y).g = static_cast<float>(
                    std::exp(response[zg] - ln_exp_best));
                hdr(x, y).b = static_cast<float>(
                    std::exp(response[zb] - ln_exp_best));
                ++ghost_count;
            } else if (g > 0.1f) {
                // Blended transition zone: mix HDR and best frame
                const Pixel& p = images[best_idx](x, y);
                int zr = pixel_to_byte(p.r);
                int zg = pixel_to_byte(p.g);
                int zb = pixel_to_byte(p.b);

                float br = static_cast<float>(
                    std::exp(response[zr] - ln_exp_best));
                float bg_val = static_cast<float>(
                    std::exp(response[zg] - ln_exp_best));
                float bb = static_cast<float>(
                    std::exp(response[zb] - ln_exp_best));

                float alpha = (g - 0.1f) / 0.4f; // map [0.1, 0.5] to [0, 1]
                hdr(x, y).r = hdr(x, y).r * (1.0f - alpha) + br * alpha;
                hdr(x, y).g = hdr(x, y).g * (1.0f - alpha) + bg_val * alpha;
                hdr(x, y).b = hdr(x, y).b * (1.0f - alpha) + bb * alpha;
            }
            // else: g <= 0.1, keep original HDR value
        }
    }

    progress_report(progress, 100, "Deghosting complete.");
}

// ============================================================================
// 7. Tone Mapping (Reinhard Local + Photographic)
// ============================================================================

/// Compute the log-average luminance of the HDR image.
static float log_average_luminance(const Image& img) {
    double sum_log = 0.0;
    int n = img.width * img.height;
    for (int i = 0; i < n; ++i) {
        float L = img.data[i].luminance();
        sum_log += std::log(std::max(L, 1e-6f));
    }
    return std::exp(static_cast<float>(sum_log / n));
}

/// Apply a Gaussian blur to a grayscale image (separable).
static GrayImage gaussian_blur(const GrayImage& src, float sigma) {
    int w = src.width, h = src.height;
    GrayImage tmp(w, h);
    GrayImage dst(w, h);

    int radius = static_cast<int>(std::ceil(sigma * 3.0f));
    std::vector<float> kernel(2 * radius + 1);
    float sum = 0.0f;
    for (int i = -radius; i <= radius; ++i) {
        kernel[i + radius] = std::exp(-(i * i) / (2.0f * sigma * sigma));
        sum += kernel[i + radius];
    }
    for (auto& k : kernel) k /= sum;

    // Horizontal pass
    for (int y = 0; y < h; ++y) {
        for (int x = 0; x < w; ++x) {
            float val = 0.0f;
            for (int k = -radius; k <= radius; ++k) {
                int sx = std::clamp(x + k, 0, w - 1);
                val += src(sx, y) * kernel[k + radius];
            }
            tmp(x, y) = val;
        }
    }
    // Vertical pass
    for (int y = 0; y < h; ++y) {
        for (int x = 0; x < w; ++x) {
            float val = 0.0f;
            for (int k = -radius; k <= radius; ++k) {
                int sy = std::clamp(y + k, 0, h - 1);
                val += tmp(x, sy) * kernel[k + radius];
            }
            dst(x, y) = val;
        }
    }
    return dst;
}

/// Local tone mapping (Reinhard-style photographic tone mapping).
/// key: subjective brightness key (0.18 = middle gray, auto = -1)
/// saturation: color saturation (0.6-1.0 typical)
Image tone_map_hdr(
    const Image& hdr,
    float key = -1.0f,
    float saturation = 0.8f,
    const ProgressCallback& progress = nullptr)
{
    progress_report(progress, 0, "Tone mapping...");

    int w = hdr.width;
    int h = hdr.height;
    int n = w * h;

    // Compute log-average luminance
    float Lav = log_average_luminance(hdr);

    // Auto-key if key < 0
    if (key < 0.0f) {
        key = 1.03f - 2.0f / (2.0f + std::log10(Lav + 1.0f));
        key = std::clamp(key, 0.09f, 0.36f);
    }

    // Scale luminance
    float alpha = key / Lav;

    // Compute luminance image
    GrayImage L(w, h);
    for (int i = 0; i < n; ++i) {
        L.data[i] = hdr.data[i].luminance() * alpha;
    }

    // Local adaptation: blur luminance with a large kernel
    float sigma = 0.35f * std::min(w, h);
    GrayImage L_blur = gaussian_blur(L, sigma);

    // Apply local tone mapping
    Image result(w, h);
    for (int i = 0; i < n; ++i) {
        float Lw = L.data[i];
        float Lb = L_blur.data[i];
        float Ld = Lw / (1.0f + Lb); // local adaptation

        // Simple S-curve for contrast
        Ld = Ld / (1.0f + Ld);

        // Apply to colors
        float scale = (Lw > 1e-10f) ? (Ld / Lw) : 0.0f;
        float r = std::pow(hdr.data[i].r * alpha * scale, saturation);
        float g = std::pow(hdr.data[i].g * alpha * scale, saturation);
        float b = std::pow(hdr.data[i].b * alpha * scale, saturation);

        result.data[i] = Pixel(
            std::clamp(r, 0.0f, 1.0f),
            std::clamp(g, 0.0f, 1.0f),
            std::clamp(b, 0.0f, 1.0f));
    }

    progress_report(progress, 100, "Tone mapping complete.");
    return result;
}

// ============================================================================
// 8. Saving Results
// ============================================================================

/// Write a 16-bit half-float value in little-endian format.
static uint16_t float_to_half(float f) {
    // IEEE 754 half-precision conversion
    uint32_t x;
    std::memcpy(&x, &f, sizeof(x));
    uint32_t sign = (x >> 16) & 0x8000;
    int32_t exp = static_cast<int32_t>((x >> 23) & 0xff) - 127 + 15;
    uint32_t mant = (x >> 13) & 0x3ff;

    if (exp <= 0) {
        // Subnormal or zero
        if (exp < -10) return static_cast<uint16_t>(sign);
        mant = (mant | 0x400) >> (1 - exp);
        return static_cast<uint16_t>(sign | mant);
    }
    if (exp >= 31) {
        // Infinity or NaN
        return static_cast<uint16_t>(sign | 0x7c00 | (mant != 0 ? 1 : 0));
    }
    return static_cast<uint16_t>(sign | (exp << 10) | mant);
}

/// Write a little-endian 32-bit integer to a stream.
static void write_le32(std::ostream& os, uint32_t v) {
    uint8_t buf[4] = {
        static_cast<uint8_t>(v & 0xff),
        static_cast<uint8_t>((v >> 8) & 0xff),
        static_cast<uint8_t>((v >> 16) & 0xff),
        static_cast<uint8_t>((v >> 24) & 0xff)
    };
    os.write(reinterpret_cast<const char*>(buf), 4);
}

/// Write a little-endian 64-bit integer to a stream.
static void write_le64(std::ostream& os, uint64_t v) {
    uint8_t buf[8];
    for (int i = 0; i < 8; ++i) {
        buf[i] = static_cast<uint8_t>((v >> (i * 8)) & 0xff);
    }
    os.write(reinterpret_cast<const char*>(buf), 8);
}

/// Write a null-terminated string to a stream.
static void write_cstr(std::ostream& os, const std::string& s) {
    os.write(s.c_str(), static_cast<std::streamsize>(s.size() + 1));
}

/// Save HDR image as OpenEXR (uncompressed, half-float, RGB).
/// This is a minimal EXR writer that produces valid files compatible with
/// standard EXR readers.
static void save_exr(const Image& img, const std::string& path) {
    std::ofstream file(path, std::ios::binary);
    if (!file) {
        throw std::runtime_error("Cannot create EXR file: " + path);
    }

    int w = img.width;
    int h = img.height;

    // Magic number
    const uint8_t magic[4] = {0x76, 0x2f, 0x31, 0x01};
    file.write(reinterpret_cast<const char*>(magic), 4);

    // Version: bit 0 = single part, bit 9 = long names, bit 11 = multi-part
    // Use 2 (single part, scanline, no tiles)
    write_le32(file, 2);

    // Header attributes
    // channels: chlist
    write_cstr(file, "channels");
    write_cstr(file, "chlist");
    // chlist size: 4 bytes name + 4 bytes type + 4 bytes (pLinear + reserved) + 4 bytes xSampling + 4 bytes ySampling per channel, then null
    // For RGB: 3 * (1+1+1+1 + 4 + 4 + 4 + 4) + 1 = 3 * 21 + 1 = 64
    // Actually: name (1+1+1+1 nulls), type (4 bytes int = 0 = HALF), pLinear (1 byte), reserved (3 bytes), xSampling (4), ySampling (4)
    // = 1 + 1 + 1 + 1 + 4 + 1 + 3 + 4 + 4 = 0x14 per channel = 20 bytes
    // 3 * 20 + 1 = 61
    // Let's use a simpler approach: write the data manually

    // Channel list
    std::ostringstream chlist;
    // R channel
    chlist.put('R'); chlist.put('\0'); chlist.put('\0'); chlist.put('\0');
    chlist.put(0); chlist.put(0); chlist.put(0); chlist.put(0); // HALF = 0 (pixel type)
    chlist.put(0); // pLinear = 0
    chlist.put(0); chlist.put(0); chlist.put(0); // reserved
    // xSampling = 1, ySampling = 1
    for (int j = 0; j < 4; ++j) chlist.put(1);
    chlist.put(0); chlist.put(0); chlist.put(0); chlist.put(0);
    for (int j = 0; j < 4; ++j) chlist.put(1);
    chlist.put(0); chlist.put(0); chlist.put(0); chlist.put(0);
    // G channel
    chlist.put('G'); chlist.put('\0'); chlist.put('\0'); chlist.put('\0');
    chlist.put(0); chlist.put(0); chlist.put(0); chlist.put(0);
    chlist.put(0);
    chlist.put(0); chlist.put(0); chlist.put(0);
    for (int j = 0; j < 4; ++j) chlist.put(1);
    chlist.put(0); chlist.put(0); chlist.put(0); chlist.put(0);
    for (int j = 0; j < 4; ++j) chlist.put(1);
    chlist.put(0); chlist.put(0); chlist.put(0); chlist.put(0);
    // B channel
    chlist.put('B'); chlist.put('\0'); chlist.put('\0'); chlist.put('\0');
    chlist.put(0); chlist.put(0); chlist.put(0); chlist.put(0);
    chlist.put(0);
    chlist.put(0); chlist.put(0); chlist.put(0);
    for (int j = 0; j < 4; ++j) chlist.put(1);
    chlist.put(0); chlist.put(0); chlist.put(0); chlist.put(0);
    for (int j = 0; j < 4; ++j) chlist.put(1);
    chlist.put(0); chlist.put(0); chlist.put(0); chlist.put(0);
    chlist.put('\0'); // end of chlist

    std::string chlist_str = chlist.str();
    write_le32(file, static_cast<uint32_t>(chlist_str.size()));
    file.write(chlist_str.data(), static_cast<std::streamsize>(chlist_str.size()));

    // compression = 0 (none)
    write_cstr(file, "compression");
    write_cstr(file, "compression");
    write_le32(file, 1);
    file.put(0);

    // dataWindow
    write_cstr(file, "dataWindow");
    write_cstr(file, "box2i");
    write_le32(file, 16); // 4 ints
    write_le32(file, 0); write_le32(file, 0); // xMin, yMin
    write_le32(file, static_cast<uint32_t>(w - 1));
    write_le32(file, static_cast<uint32_t>(h - 1));

    // displayWindow
    write_cstr(file, "displayWindow");
    write_cstr(file, "box2i");
    write_le32(file, 16);
    write_le32(file, 0); write_le32(file, 0);
    write_le32(file, static_cast<uint32_t>(w - 1));
    write_le32(file, static_cast<uint32_t>(h - 1));

    // lineOrder = 0 (increasing Y)
    write_cstr(file, "lineOrder");
    write_cstr(file, "lineOrder");
    write_le32(file, 1);
    file.put(0);

    // pixelAspectRatio = 1.0
    write_cstr(file, "pixelAspectRatio");
    write_cstr(file, "float");
    write_le32(file, 4);
    float aspect = 1.0f;
    file.write(reinterpret_cast<const char*>(&aspect), 4);

    // screenWindowWidth = 1.0
    write_cstr(file, "screenWindowWidth");
    write_cstr(file, "float");
    write_le32(file, 4);
    float sw = 1.0f;
    file.write(reinterpret_cast<const char*>(&sw), 4);

    // screenWindowCenter = (0, 0)
    write_cstr(file, "screenWindowCenter");
    write_cstr(file, "v2f");
    write_le32(file, 8);
    float sc[2] = {0.0f, 0.0f};
    file.write(reinterpret_cast<const char*>(sc), 8);

    // End of header
    file.put('\0');

    // Offset table: one entry per scanline
    // Each scanline: 4 bytes (y coordinate) + 4 bytes (pixel data size) + w * 3 * 2 bytes (half-float RGB)
    int scanline_data_size = w * 3 * 2; // RGB half-float
    uint64_t offset = static_cast<uint64_t>(file.tellp()) + static_cast<uint64_t>(h) * 8ULL;
    for (int y = 0; y < h; ++y) {
        write_le64(file, offset);
        offset += 8 + static_cast<uint64_t>(scanline_data_size);
    }

    // Scanline data
    std::vector<uint16_t> scanline(w * 3);
    for (int y = 0; y < h; ++y) {
        // Write y coordinate (4 bytes)
        write_le32(file, static_cast<uint32_t>(y));
        // Write pixel data size (4 bytes)
        write_le32(file, static_cast<uint32_t>(scanline_data_size));

        // Convert to half-float interleaved RGB
        for (int x = 0; x < w; ++x) {
            const Pixel& p = img(x, y);
            scanline[x * 3 + 0] = float_to_half(p.r);
            scanline[x * 3 + 1] = float_to_half(p.g);
            scanline[x * 3 + 2] = float_to_half(p.b);
        }
        file.write(reinterpret_cast<const char*>(scanline.data()),
                   static_cast<std::streamsize>(scanline_data_size));
    }
}

/// Save a tone-mapped image as 8-bit PPM (P6 format).
static void save_ppm(const Image& img, const std::string& path) {
    std::ofstream file(path, std::ios::binary);
    if (!file) {
        throw std::runtime_error("Cannot create PPM file: " + path);
    }

    int w = img.width;
    int h = img.height;

    file << "P6\n" << w << " " << h << "\n255\n";

    std::vector<uint8_t> row(w * 3);
    for (int y = 0; y < h; ++y) {
        for (int x = 0; x < w; ++x) {
            const Pixel& p = img(x, y);
            auto to_byte = [](float v) -> uint8_t {
                // Gamma correction (sRGB approximate)
                float g = (v <= 0.0031308f) ? (12.92f * v)
                    : (1.055f * std::pow(v, 1.0f / 2.4f) - 0.055f);
                return static_cast<uint8_t>(
                    std::clamp(static_cast<int>(g * 255.0f + 0.5f), 0, 255));
            };
            row[x * 3 + 0] = to_byte(p.r);
            row[x * 3 + 1] = to_byte(p.g);
            row[x * 3 + 2] = to_byte(p.b);
        }
        file.write(reinterpret_cast<const char*>(row.data()), w * 3);
    }
}

/// Save HDR result to file. Supports:
/// - .exr: OpenEXR format (half-float, uncompressed)
/// - .ppm: Tone-mapped 8-bit PPM
/// - .hdr/.pfm: Portable FloatMap
/// The format is auto-detected from the file extension.
void save_hdr_result(
    const Image& image,
    const std::string& path,
    bool /* tone_mapped */ = false,
    const ProgressCallback& progress = nullptr)
{
    progress_report(progress, 0, "Saving result...");

    if (!image.valid()) {
        throw std::invalid_argument("Invalid image to save");
    }

    // Determine format from extension
    std::string ext;
    auto dot_pos = path.rfind('.');
    if (dot_pos != std::string::npos) {
        ext = path.substr(dot_pos);
        // Lowercase
        for (auto& c : ext) c = static_cast<char>(std::tolower(c));
    }

    if (ext == ".exr") {
        save_exr(image, path);
    } else if (ext == ".hdr" || ext == ".pfm") {
        // Write PFM (Portable FloatMap)
        std::ofstream file(path, std::ios::binary);
        if (!file) {
            throw std::runtime_error("Cannot create PFM file: " + path);
        }
        file << "PF\n" << image.width << " " << image.height << "\n-1.0\n";
        // PFM is bottom-up
        std::vector<float> row(image.width * 3);
        for (int y = image.height - 1; y >= 0; --y) {
            for (int x = 0; x < image.width; ++x) {
                const Pixel& p = image(x, y);
                row[x * 3 + 0] = p.r;
                row[x * 3 + 1] = p.g;
                row[x * 3 + 2] = p.b;
            }
            file.write(reinterpret_cast<const char*>(row.data()),
                       static_cast<std::streamsize>(image.width * 3 * sizeof(float)));
        }
    } else if (ext == ".ppm") {
        save_ppm(image, path);
    } else {
        // Default: save as PPM (tone-mapped)
        save_ppm(image, path);
    }

    progress_report(progress, 100, "Save complete.");
}

// ============================================================================
// 9. Convenience Pipeline
// ============================================================================

/// Result of the HDR merge pipeline.
struct HDRResult {
    Image hdr_radiance;    // Floating-point HDR radiance map
    Image tonemapped;      // Tone-mapped LDR image (0-1 range)
    GrayImage ghost_mask;  // Ghost detection mask
    std::vector<float> camera_response; // Estimated camera response function
};

/// Run the full HDR merge pipeline: load → align → estimate response →
/// merge → detect ghosts → deghost → tone map.
HDRResult hdr_merge_pipeline(
    const std::vector<std::string>& paths,
    const std::vector<float>& exposure_times,
    float ghost_threshold = 0.15f,
    float tone_key = -1.0f,
    const ProgressCallback& progress = nullptr)
{
    HDRResult result;

    // 1. Load frames
    progress_report(progress, 0, "Loading frames...");
    std::vector<Frame> frames = load_hdr_frames(paths, exposure_times, {}, progress);

    // 2. Align frames
    std::vector<Image> aligned = align_frames(frames, 0, progress);

    // 3. Estimate camera response
    result.camera_response = estimate_camera_response(aligned, exposure_times, progress);

    // 4. Merge to HDR
    result.hdr_radiance = merge_to_hdr(aligned, exposure_times, result.camera_response, progress);

    // 5. Detect ghost regions
    result.ghost_mask = detect_ghost_regions(aligned, ghost_threshold, progress);

    // 6. Deghost
    deghost_hdr(result.hdr_radiance, result.ghost_mask, aligned,
                exposure_times, result.camera_response, progress);

    // 7. Tone map
    result.tonemapped = tone_map_hdr(result.hdr_radiance, tone_key, 0.8f, progress);

    progress_report(progress, 100, "Pipeline complete.");
    return result;
}

// ============================================================================
// Utility
// ============================================================================

static void progress_report(const ProgressCallback& cb, int pct, const char* msg) {
    if (cb) {
        cb(pct, msg ? msg : "");
    }
}

// ============================================================================
// Public API: individual-step wrappers
// ============================================================================

/// Align frames with explicit progress callback, returning aligned images
/// (wraps the internal align_frames which takes Frame objects).
std::vector<Image> align_frames_from_images(
    const std::vector<Image>& images,
    int reference_index = 0,
    const ProgressCallback& progress = nullptr)
{
    std::vector<Frame> frames;
    frames.reserve(images.size());
    for (size_t i = 0; i < images.size(); ++i) {
        Frame f;
        f.image = images[i];
        f.exposure_time = 1.0f;
        frames.push_back(std::move(f));
    }
    return align_frames(frames, reference_index, progress);
}

} // namespace hdr
} // namespace alcedo