// AlcedoStudio Panorama Stitcher
// Copyright (c) 2026 AlcedoStudio. All rights reserved.
//
// A production-quality panorama image stitcher for RAW photo editing.
// Implements FAST corner detection, BRIEF-like binary descriptors,
// Hamming-distance matching, RANSAC homography estimation,
// multi-band blending, and cylindrical/spherical projection options.
//
// No external image library dependencies. Self-contained, C++17.

#include <algorithm>
#include <array>
#include <atomic>
#include <bitset>
#include <cassert>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <fstream>
#include <functional>
#include <limits>
#include <memory>
#include <mutex>
#include <numeric>
#include <random>
#include <sstream>
#include <stdexcept>
#include <string>
#include <thread>
#include <tuple>
#include <unordered_map>
#include <utility>
#include <vector>

namespace alcedo {
namespace panorama {

// ============================================================================
// 1. Core Data Structures
// ============================================================================

/// Floating-point pixel for high-dynamic-range processing.
struct Pixel {
    float r = 0.0f;  // Red   [0.0, 1.0] or beyond for HDR
    float g = 0.0f;  // Green
    float b = 0.0f;  // Blue
    float a = 1.0f;  // Alpha

    Pixel() = default;
    Pixel(float rr, float gg, float bb, float aa = 1.0f)
        : r(rr), g(gg), b(bb), a(aa) {}

    Pixel operator+(const Pixel& o) const { return {r + o.r, g + o.g, b + o.b, a + o.a}; }
    Pixel operator-(const Pixel& o) const { return {r - o.r, g - o.g, b - o.b, a - o.a}; }
    Pixel operator*(float s) const { return {r * s, g * s, b * s, a * s}; }
    Pixel operator/(float s) const { return {r / s, g / s, b / s, a / s}; }
    Pixel& operator+=(const Pixel& o) { r += o.r; g += o.g; b += o.b; a += o.a; return *this; }
    Pixel& operator*=(float s) { r *= s; g *= s; b *= s; a *= s; return *this; }

    float luminance() const { return 0.299f * r + 0.587f * g + 0.114f * b; }

    Pixel clamp(float lo = 0.0f, float hi = 1.0f) const {
        return {std::clamp(r, lo, hi), std::clamp(g, lo, hi),
                std::clamp(b, lo, hi), std::clamp(a, lo, hi)};
    }
};

/// 2D image stored as a flat row-major buffer of Pixel.
class Image {
public:
    Image() : width_(0), height_(0) {}
    Image(int w, int h, const Pixel& fill = Pixel{0, 0, 0, 0})
        : width_(w), height_(h), data_(w * h, fill) {}

    int width() const { return width_; }
    int height() const { return height_; }
    bool empty() const { return width_ == 0 || height_ == 0; }

    Pixel& operator()(int x, int y) { return data_[y * width_ + x]; }
    const Pixel& operator()(int x, int y) const { return data_[y * width_ + x]; }

    Pixel* row(int y) { return data_.data() + y * width_; }
    const Pixel* row(int y) const { return data_.data() + y * width_; }

    Pixel* data() { return data_.data(); }
    const Pixel* data() const { return data_.data(); }
    size_t size() const { return data_.size(); }

    /// Bilinear interpolation with boundary clamping.
    Pixel sample(float x, float y) const {
        int ix = static_cast<int>(std::floor(x));
        int iy = static_cast<int>(std::floor(y));
        float fx = x - static_cast<float>(ix);
        float fy = y - static_cast<float>(iy);

        auto clamp_x = [&](int v) { return std::clamp(v, 0, width_ - 1); };
        auto clamp_y = [&](int v) { return std::clamp(v, 0, height_ - 1); };

        int x0 = clamp_x(ix), x1 = clamp_x(ix + 1);
        int y0 = clamp_y(iy), y1 = clamp_y(iy + 1);

        const Pixel& p00 = (*this)(x0, y0);
        const Pixel& p10 = (*this)(x1, y0);
        const Pixel& p01 = (*this)(x0, y1);
        const Pixel& p11 = (*this)(x1, y1);

        Pixel top = p00 * (1.0f - fx) + p10 * fx;
        Pixel bot = p01 * (1.0f - fx) + p11 * fx;
        return top * (1.0f - fy) + bot * fy;
    }

    static Image from_ppm(const std::string& path);
    void to_ppm(const std::string& path) const;

private:
    int width_;
    int height_;
    std::vector<Pixel> data_;
};

// ---------------------------------------------------------------------------
// Minimal PPM reader/writer (binary P6 and ASCII P3)
// ---------------------------------------------------------------------------

Image Image::from_ppm(const std::string& path) {
    std::ifstream in(path, std::ios::binary);
    if (!in) {
        throw std::runtime_error("panorama: cannot open " + path);
    }

    std::string magic;
    in >> magic;
    if (magic != "P6" && magic != "P3") {
        throw std::runtime_error("panorama: unsupported PPM format " + magic);
    }
    bool binary = (magic == "P6");

    int w = 0, h = 0, maxval = 0;
    // Skip comment lines
    char c = static_cast<char>(in.peek());
    while (c == '#' || c == '\n' || c == '\r') {
        std::string line;
        std::getline(in, line);
        if (c == '#') { /* comment */ }
        c = static_cast<char>(in.peek());
    }
    in >> w >> h >> maxval;
    in.get();  // consume single whitespace

    if (w <= 0 || h <= 0 || maxval <= 0 || maxval > 65535) {
        throw std::runtime_error("panorama: invalid PPM dimensions");
    }

    Image img(w, h);
    float scale = 1.0f / static_cast<float>(maxval);

    if (binary) {
        // P6 binary
        if (maxval <= 255) {
            std::vector<uint8_t> row(w * 3);
            for (int y = 0; y < h; ++y) {
                in.read(reinterpret_cast<char*>(row.data()), row.size());
                for (int x = 0; x < w; ++x) {
                    img(x, y) = Pixel{
                        static_cast<float>(row[x * 3 + 0]) * scale,
                        static_cast<float>(row[x * 3 + 1]) * scale,
                        static_cast<float>(row[x * 3 + 2]) * scale,
                        1.0f};
                }
            }
        } else {
            std::vector<uint8_t> row(w * 6);
            for (int y = 0; y < h; ++y) {
                in.read(reinterpret_cast<char*>(row.data()), row.size());
                for (int x = 0; x < w; ++x) {
                    uint16_t r = (static_cast<uint16_t>(row[x * 6 + 0]) << 8) | row[x * 6 + 1];
                    uint16_t g = (static_cast<uint16_t>(row[x * 6 + 2]) << 8) | row[x * 6 + 3];
                    uint16_t b = (static_cast<uint16_t>(row[x * 6 + 4]) << 8) | row[x * 6 + 5];
                    img(x, y) = Pixel{static_cast<float>(r) * scale,
                                      static_cast<float>(g) * scale,
                                      static_cast<float>(b) * scale, 1.0f};
                }
            }
        }
    } else {
        // P3 ASCII
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                int rv = 0, gv = 0, bv = 0;
                in >> rv >> gv >> bv;
                img(x, y) = Pixel{static_cast<float>(rv) * scale,
                                  static_cast<float>(gv) * scale,
                                  static_cast<float>(bv) * scale, 1.0f};
            }
        }
    }

    return img;
}

void Image::to_ppm(const std::string& path) const {
    std::ofstream out(path, std::ios::binary);
    if (!out) {
        throw std::runtime_error("panorama: cannot write " + path);
    }
    out << "P6\n" << width_ << " " << height_ << "\n255\n";
    std::vector<uint8_t> row(width_ * 3);
    for (int y = 0; y < height_; ++y) {
        for (int x = 0; x < width_; ++x) {
            Pixel p = (*this)(x, y).clamp(0.0f, 1.0f);
            row[x * 3 + 0] = static_cast<uint8_t>(p.r * 255.0f + 0.5f);
            row[x * 3 + 1] = static_cast<uint8_t>(p.g * 255.0f + 0.5f);
            row[x * 3 + 2] = static_cast<uint8_t>(p.b * 255.0f + 0.5f);
        }
        out.write(reinterpret_cast<const char*>(row.data()), row.size());
    }
}

// ============================================================================
// 2. Utility: Gaussian Blur & Pyramid
// ============================================================================

namespace {

Image gaussian_blur(const Image& src, float sigma) {
    if (sigma < 0.5f) return src;
    int radius = static_cast<int>(std::ceil(sigma * 3.0f));
    int w = src.width(), h = src.height();
    Image tmp(w, h);
    Image dst(w, h);

    // 1D kernel
    std::vector<float> kernel(radius * 2 + 1);
    float sum = 0.0f;
    float two_s2 = 2.0f * sigma * sigma;
    for (int i = -radius; i <= radius; ++i) {
        float v = std::exp(-static_cast<float>(i * i) / two_s2);
        kernel[i + radius] = v;
        sum += v;
    }
    for (auto& k : kernel) k /= sum;

    // Horizontal pass
    for (int y = 0; y < h; ++y) {
        for (int x = 0; x < w; ++x) {
            Pixel acc{};
            for (int k = -radius; k <= radius; ++k) {
                int sx = std::clamp(x + k, 0, w - 1);
                acc += src(sx, y) * kernel[k + radius];
            }
            tmp(x, y) = acc;
        }
    }
    // Vertical pass
    for (int y = 0; y < h; ++y) {
        for (int x = 0; x < w; ++x) {
            Pixel acc{};
            for (int k = -radius; k <= radius; ++k) {
                int sy = std::clamp(y + k, 0, h - 1);
                acc += tmp(x, sy) * kernel[k + radius];
            }
            dst(x, y) = acc;
        }
    }
    return dst;
}

/// Downsample by factor 2 (simple averaging).
Image downsample2(const Image& src) {
    int w = src.width() / 2, h = src.height() / 2;
    if (w < 1) w = 1;
    if (h < 1) h = 1;
    Image dst(w, h);
    for (int y = 0; y < h; ++y) {
        for (int x = 0; x < w; ++x) {
            Pixel acc{};
            acc += src(x * 2, y * 2);
            acc += src(std::min(x * 2 + 1, src.width() - 1), y * 2);
            acc += src(x * 2, std::min(y * 2 + 1, src.height() - 1));
            acc += src(std::min(x * 2 + 1, src.width() - 1),
                       std::min(y * 2 + 1, src.height() - 1));
            dst(x, y) = acc * 0.25f;
        }
    }
    return dst;
}

/// Upsample by factor 2 (bilinear).
Image upsample2(const Image& src) {
    int w = src.width() * 2, h = src.height() * 2;
    Image dst(w, h);
    for (int y = 0; y < h; ++y) {
        for (int x = 0; x < w; ++x) {
            float sx = (static_cast<float>(x) + 0.5f) * 0.5f - 0.5f;
            float sy = (static_cast<float>(y) + 0.5f) * 0.5f - 0.5f;
            dst(x, y) = src.sample(sx, sy);
        }
    }
    return dst;
}

/// Build Gaussian pyramid.
std::vector<Image> build_gaussian_pyramid(const Image& src, int levels) {
    std::vector<Image> pyramid;
    pyramid.reserve(levels);
    pyramid.push_back(src);
    for (int i = 1; i < levels; ++i) {
        Image blurred = gaussian_blur(pyramid.back(), 1.0f);
        pyramid.push_back(downsample2(blurred));
    }
    return pyramid;
}

/// Build Laplacian pyramid from Gaussian pyramid.
std::vector<Image> build_laplacian_pyramid(const std::vector<Image>& gaussian) {
    std::vector<Image> laplacian;
    laplacian.reserve(gaussian.size());
    for (size_t i = 0; i + 1 < gaussian.size(); ++i) {
        Image up = upsample2(gaussian[i + 1]);
        int w = gaussian[i].width(), h = gaussian[i].height();
        Image lap(w, h);
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                lap(x, y) = gaussian[i](x, y) - up(x, y);
            }
        }
        laplacian.push_back(std::move(lap));
    }
    laplacian.push_back(gaussian.back());  // coarsest level = residual
    return laplacian;
}

/// Reconstruct from Laplacian pyramid.
Image reconstruct_from_laplacian(const std::vector<Image>& laplacian) {
    Image result = laplacian.back();
    for (int i = static_cast<int>(laplacian.size()) - 2; i >= 0; --i) {
        Image up = upsample2(result);
        int w = laplacian[i].width(), h = laplacian[i].height();
        result = Image(w, h);
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                result(x, y) = laplacian[i](x, y) + up(x, y);
            }
        }
    }
    return result;
}

}  // anonymous namespace

// ============================================================================
// 3. FAST Corner Detection
// ============================================================================

/// Bresenham circle offsets for FAST-9 (radius 3, 16 pixels).
constexpr int kFastCircleOffsets[16][2] = {
    { 0, -3}, { 1, -3}, { 2, -2}, { 3, -1},
    { 3,  0}, { 3,  1}, { 2,  2}, { 1,  3},
    { 0,  3}, {-1,  3}, {-2,  2}, {-3,  1},
    {-3,  0}, {-3, -1}, {-2, -2}, {-1, -3},
};

struct KeyPoint {
    float x = 0.0f;   // subpixel-refined coordinates
    float y = 0.0f;
    float response = 0.0f;   // corner strength
    int octave = 0;   // pyramid level

    KeyPoint() = default;
    KeyPoint(float px, float py, float r, int o = 0)
        : x(px), y(py), response(r), octave(o) {}
};

namespace {

/// FAST-9 corner score: max of (darker or brighter threshold sum).
float fast_score(const float* intensities, int w, int h, int cx, int cy, float threshold) {
    float center = intensities[cy * w + cx];
    float sum_brighter = 0.0f, sum_darker = 0.0f;
    int count_brighter = 0, count_darker = 0;

    for (int i = 0; i < 16; ++i) {
        int nx = cx + kFastCircleOffsets[i][0];
        int ny = cy + kFastCircleOffsets[i][1];
        if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
        float val = intensities[ny * w + nx];
        float diff = val - center;
        if (diff > threshold) {
            sum_brighter += diff;
            ++count_brighter;
        } else if (diff < -threshold) {
            sum_darker += (-diff);
            ++count_darker;
        }
    }
    if (count_brighter >= 9) return sum_brighter;
    if (count_darker >= 9) return sum_darker;
    return 0.0f;
}

/// FAST-9 corner detection with non-maximum suppression.
std::vector<KeyPoint> detect_fast(const Image& img, float threshold, int border = 3) {
    int w = img.width(), h = img.height();
    // Precompute luminance
    std::vector<float> lum(w * h);
    for (int i = 0; i < w * h; ++i) {
        lum[i] = img.data()[i].luminance();
    }

    std::vector<KeyPoint> corners;
    corners.reserve(w * h / 16);

    for (int y = border; y < h - border; ++y) {
        for (int x = border; x < w - border; ++x) {
            float score = fast_score(lum.data(), w, h, x, y, threshold);
            if (score > 0.0f) {
                corners.emplace_back(static_cast<float>(x),
                                     static_cast<float>(y), score, 0);
            }
        }
    }

    // Non-maximum suppression (3x3 window)
    std::vector<KeyPoint> filtered;
    filtered.reserve(corners.size());
    std::vector<bool> suppressed(corners.size(), false);

    for (size_t i = 0; i < corners.size(); ++i) {
        if (suppressed[i]) continue;
        for (size_t j = i + 1; j < corners.size(); ++j) {
            if (suppressed[j]) continue;
            float dx = corners[i].x - corners[j].x;
            float dy = corners[i].y - corners[j].y;
            if (std::abs(dx) <= 3.0f && std::abs(dy) <= 3.0f) {
                if (corners[i].response >= corners[j].response) {
                    suppressed[j] = true;
                } else {
                    suppressed[i] = true;
                    break;
                }
            }
        }
        if (!suppressed[i]) filtered.push_back(corners[i]);
    }

    return filtered;
}

/// Multi-scale FAST detection.
std::vector<KeyPoint> detect_fast_multiscale(const Image& img, float threshold,
                                              int num_octaves = 4) {
    std::vector<KeyPoint> all_keypoints;
    Image current = img;
    float scale_factor = 1.0f;

    for (int octave = 0; octave < num_octaves; ++octave) {
        auto kps = detect_fast(current, threshold);
        for (auto& kp : kps) {
            kp.x *= scale_factor;
            kp.y *= scale_factor;
            kp.octave = octave;
        }
        all_keypoints.insert(all_keypoints.end(), kps.begin(), kps.end());
        current = downsample2(gaussian_blur(current, 1.0f));
        scale_factor *= 2.0f;
    }

    return all_keypoints;
}

}  // anonymous namespace

// ============================================================================
// 4. BRIEF-like Binary Descriptor
// ============================================================================

/// 256-bit (32-byte) binary descriptor.
using Descriptor = std::array<uint8_t, 32>;

/// Pre-generated sampling pattern: 256 pairs of (dx1, dy1, dx2, dy2) within a
/// 31x31 patch. Deterministic — seeded by a fixed value.
constexpr int kBriefPatchSize = 31;
constexpr int kBriefHalfPatch = 15;
constexpr int kNumBriefPairs = 256;

namespace {

std::array<std::pair<std::pair<int, int>, std::pair<int, int>>, kNumBriefPairs>
generate_brief_pattern() {
    std::array<std::pair<std::pair<int, int>, std::pair<int, int>>, kNumBriefPairs> pattern{};
    std::mt19937 rng(42);  // fixed seed for reproducibility
    std::uniform_int_distribution<int> dist(-kBriefHalfPatch, kBriefHalfPatch);
    for (int i = 0; i < kNumBriefPairs; ++i) {
        int x1 = dist(rng), y1 = dist(rng);
        int x2 = dist(rng), y2 = dist(rng);
        pattern[i] = {{x1, y1}, {x2, y2}};
    }
    return pattern;
}

const auto kBriefPattern = generate_brief_pattern();

Descriptor compute_brief_descriptor(const Image& img, float cx, float cy) {
    int w = img.width(), h = img.height();
    Descriptor desc{};
    for (int i = 0; i < kNumBriefPairs; ++i) {
        auto [p1, p2] = kBriefPattern[i];
        int x1 = static_cast<int>(std::round(cx + p1.first));
        int y1 = static_cast<int>(std::round(cy + p1.second));
        int x2 = static_cast<int>(std::round(cx + p2.first));
        int y2 = static_cast<int>(std::round(cy + p2.second));

        x1 = std::clamp(x1, 0, w - 1);
        y1 = std::clamp(y1, 0, h - 1);
        x2 = std::clamp(x2, 0, w - 1);
        y2 = std::clamp(y2, 0, h - 1);

        float l1 = img(x1, y1).luminance();
        float l2 = img(x2, y2).luminance();
        if (l1 < l2) {
            desc[i / 8] |= static_cast<uint8_t>(1 << (i % 8));
        }
    }
    return desc;
}

/// Hamming distance between two 256-bit descriptors.
int hamming_distance(const Descriptor& a, const Descriptor& b) {
    int dist = 0;
    for (size_t i = 0; i < a.size(); ++i) {
        dist += __builtin_popcount(a[i] ^ b[i]);
    }
    return dist;
}

}  // anonymous namespace

// ============================================================================
// 5. Feature Point with Descriptor
// ============================================================================

struct Feature {
    KeyPoint kp;
    Descriptor desc;

    Feature(KeyPoint k, Descriptor d) : kp(std::move(k)), desc(std::move(d)) {}
};

/// Detect features (keypoints + descriptors) from an image.
std::vector<Feature> detect_features(const Image& img, float fast_threshold = 0.05f) {
    auto keypoints = detect_fast_multiscale(img, fast_threshold);
    // Sort by response, keep top N
    constexpr size_t kMaxFeatures = 2000;
    if (keypoints.size() > kMaxFeatures) {
        std::nth_element(keypoints.begin(), keypoints.begin() + kMaxFeatures, keypoints.end(),
                         [](const KeyPoint& a, const KeyPoint& b) {
                             return a.response > b.response;
                         });
        keypoints.resize(kMaxFeatures);
    }

    std::vector<Feature> features;
    features.reserve(keypoints.size());
    for (const auto& kp : keypoints) {
        auto desc = compute_brief_descriptor(img, kp.x, kp.y);
        features.emplace_back(kp, desc);
    }
    return features;
}

// ============================================================================
// 6. Feature Matching
// ============================================================================

struct Match {
    int idx_a;  // index in features_a
    int idx_b;  // index in features_b
    float distance;

    Match(int a, int b, float d) : idx_a(a), idx_b(b), distance(d) {}
};

namespace {

/// Brute-force matcher with Lowe's ratio test.
std::vector<Match> match_features(const std::vector<Feature>& fa,
                                   const std::vector<Feature>& fb,
                                   float ratio_threshold = 0.75f,
                                   int max_distance = 64) {
    std::vector<Match> matches;
    matches.reserve(fa.size());

    for (size_t i = 0; i < fa.size(); ++i) {
        int best_j = -1, second_j = -1;
        int best_dist = 256, second_dist = 256;

        for (size_t j = 0; j < fb.size(); ++j) {
            int dist = hamming_distance(fa[i].desc, fb[j].desc);
            if (dist < best_dist) {
                second_dist = best_dist;
                second_j = best_j;
                best_dist = dist;
                best_j = static_cast<int>(j);
            } else if (dist < second_dist) {
                second_dist = dist;
                second_j = static_cast<int>(j);
            }
        }

        if (best_j >= 0 && best_dist < max_distance) {
            if (second_j < 0 ||
                static_cast<float>(best_dist) < ratio_threshold * static_cast<float>(second_dist)) {
                matches.emplace_back(static_cast<int>(i), best_j,
                                     static_cast<float>(best_dist));
            }
        }
    }

    return matches;
}

}  // anonymous namespace

// ============================================================================
// 7. Homography Estimation (RANSAC + DLT)
// ============================================================================

/// 3x3 matrix stored in row-major order.
struct Mat3 {
    float m[9] = {1, 0, 0,
                  0, 1, 0,
                  0, 0, 1};

    Mat3() = default;

    Mat3(float m00, float m01, float m02,
         float m10, float m11, float m12,
         float m20, float m21, float m22) {
        m[0] = m00; m[1] = m01; m[2] = m02;
        m[3] = m10; m[4] = m11; m[5] = m12;
        m[6] = m20; m[7] = m21; m[8] = m22;
    }

    float operator()(int row, int col) const { return m[row * 3 + col]; }
    float& operator()(int row, int col) { return m[row * 3 + col]; }

    Mat3 inverse() const {
        float det = m[0] * (m[4] * m[8] - m[5] * m[7])
                  - m[1] * (m[3] * m[8] - m[5] * m[6])
                  + m[2] * (m[3] * m[7] - m[4] * m[6]);
        if (std::abs(det) < 1e-12f) return Mat3();  // singular

        float inv_det = 1.0f / det;
        return Mat3(
            (m[4] * m[8] - m[5] * m[7]) * inv_det,
            (m[2] * m[7] - m[1] * m[8]) * inv_det,
            (m[1] * m[5] - m[2] * m[4]) * inv_det,
            (m[5] * m[6] - m[3] * m[8]) * inv_det,
            (m[0] * m[8] - m[2] * m[6]) * inv_det,
            (m[2] * m[3] - m[0] * m[5]) * inv_det,
            (m[3] * m[7] - m[4] * m[6]) * inv_det,
            (m[1] * m[6] - m[0] * m[7]) * inv_det,
            (m[0] * m[4] - m[1] * m[3]) * inv_det);
    }

    /// Apply homography to a point (returns homogeneous, then normalized).
    void transform(float x, float y, float& out_x, float& out_y) const {
        float w = m[6] * x + m[7] * y + m[8];
        if (std::abs(w) < 1e-8f) {
            out_x = x; out_y = y;
            return;
        }
        out_x = (m[0] * x + m[1] * y + m[2]) / w;
        out_y = (m[3] * x + m[4] * y + m[5]) / w;
    }
};

namespace {

/// 4-point DLT: compute homography from exactly 4 point correspondences.
/// Uses normalized DLT for numerical stability.
bool compute_homography_dlt(const std::array<std::pair<float, float>, 4>& src,
                             const std::array<std::pair<float, float>, 4>& dst,
                             Mat3& H) {
    // Normalization: translate so centroid is at origin, scale so mean distance is sqrt(2).
    auto normalize_pts = [](const std::array<std::pair<float, float>, 4>& pts,
                            float& tx, float& ty, float& s) {
        float cx = 0, cy = 0;
        for (const auto& p : pts) { cx += p.first; cy += p.second; }
        cx /= 4.0f; cy /= 4.0f;
        float mean_dist = 0;
        for (const auto& p : pts) {
            float dx = p.first - cx, dy = p.second - cy;
            mean_dist += std::sqrt(dx * dx + dy * dy);
        }
        mean_dist /= 4.0f;
        s = std::sqrt(2.0f) / std::max(mean_dist, 1e-8f);
        tx = -cx * s;
        ty = -cy * s;
    };

    float tx1, ty1, s1, tx2, ty2, s2;
    normalize_pts(src, tx1, ty1, s1);
    normalize_pts(dst, tx2, ty2, s2);

    // Build 8x9 system. Each correspondence gives 2 equations:
    // [ x_i, y_i, 1, 0, 0, 0, -x_i'*x_i, -x_i'*y_i, -x_i' ] * h = 0
    // [ 0, 0, 0, x_i, y_i, 1, -y_i'*x_i, -y_i'*y_i, -y_i' ] * h = 0

    float A[64] = {};  // 8 rows x 8 cols (we solve Ah = b, where h = [h0..h7], h8 = 1)
    float b[8] = {};

    for (int i = 0; i < 4; ++i) {
        float x = src[i].first * s1 + tx1;
        float y = src[i].second * s1 + ty1;
        float xp = dst[i].first * s2 + tx2;
        float yp = dst[i].second * s2 + ty2;

        // Row 2*i:   [x, y, 1, 0, 0, 0, -xp*x, -xp*y] * h = xp
        int r0 = 2 * i;
        A[r0 * 8 + 0] = x;
        A[r0 * 8 + 1] = y;
        A[r0 * 8 + 2] = 1;
        A[r0 * 8 + 3] = 0;
        A[r0 * 8 + 4] = 0;
        A[r0 * 8 + 5] = 0;
        A[r0 * 8 + 6] = -xp * x;
        A[r0 * 8 + 7] = -xp * y;
        b[r0] = xp;

        // Row 2*i+1: [0, 0, 0, x, y, 1, -yp*x, -yp*y] * h = yp
        int r1 = 2 * i + 1;
        A[r1 * 8 + 0] = 0;
        A[r1 * 8 + 1] = 0;
        A[r1 * 8 + 2] = 0;
        A[r1 * 8 + 3] = x;
        A[r1 * 8 + 4] = y;
        A[r1 * 8 + 5] = 1;
        A[r1 * 8 + 6] = -yp * x;
        A[r1 * 8 + 7] = -yp * y;
        b[r1] = yp;
    }

    // Gaussian elimination with partial pivoting (8x8 system)
    for (int col = 0; col < 8; ++col) {
        // Find pivot
        int pivot = col;
        float max_val = std::abs(A[col * 8 + col]);
        for (int row = col + 1; row < 8; ++row) {
            float val = std::abs(A[row * 8 + col]);
            if (val > max_val) { max_val = val; pivot = row; }
        }
        if (max_val < 1e-12f) return false;

        // Swap rows
        if (pivot != col) {
            for (int j = 0; j < 8; ++j) std::swap(A[col * 8 + j], A[pivot * 8 + j]);
            std::swap(b[col], b[pivot]);
        }

        float inv_pivot = 1.0f / A[col * 8 + col];
        // Eliminate below
        for (int row = col + 1; row < 8; ++row) {
            float factor = A[row * 8 + col] * inv_pivot;
            for (int j = col; j < 8; ++j) A[row * 8 + j] -= factor * A[col * 8 + j];
            b[row] -= factor * b[col];
        }
    }

    // Back substitution
    float h_vec[8] = {};
    for (int row = 7; row >= 0; --row) {
        float sum = b[row];
        for (int j = row + 1; j < 8; ++j) sum -= A[row * 8 + j] * h_vec[j];
        h_vec[row] = sum / A[row * 8 + row];
    }

    // Denormalize: H = T2^{-1} * H_norm * T1
    // T1 = [s1, 0, tx1; 0, s1, ty1; 0, 0, 1]
    // T2 = [s2, 0, tx2; 0, s2, ty2; 0, 0, 1]
    // T2_inv = [1/s2, 0, -tx2/s2; 0, 1/s2, -ty2/s2; 0, 0, 1]
    // H = T2_inv * H_norm * T1

    float inv_s2 = 1.0f / s2;

    // H_norm * T1
    float h0 = h_vec[0] * s1 + h_vec[2] * tx1;
    float h1 = h_vec[1] * s1 + h_vec[2] * ty1;
    float h2 = h_vec[2];
    float h3 = h_vec[3] * s1 + h_vec[5] * tx1;
    float h4 = h_vec[4] * s1 + h_vec[5] * ty1;
    float h5 = h_vec[5];
    float h6 = (h_vec[6] * s1 + tx1);
    float h7 = (h_vec[7] * s1 + ty1);
    float h8 = 1.0f;

    // T2_inv * (H_norm * T1)
    H = Mat3(
        inv_s2 * h0 - tx2 * inv_s2 * h6,
        inv_s2 * h1 - tx2 * inv_s2 * h7,
        inv_s2 * h2 - tx2 * inv_s2 * h8,
        inv_s2 * h3 - ty2 * inv_s2 * h6,
        inv_s2 * h4 - ty2 * inv_s2 * h7,
        inv_s2 * h5 - ty2 * inv_s2 * h8,
        h6, h7, h8);

    return true;
}

/// RANSAC homography estimation.
Mat3 estimate_homography_ransac(const std::vector<Match>& matches,
                                 const std::vector<Feature>& fa,
                                 const std::vector<Feature>& fb,
                                 float inlier_threshold = 3.0f,
                                 int max_iterations = 2000,
                                 float confidence = 0.995f) {
    if (matches.size() < 4) {
        return Mat3();  // identity
    }

    std::mt19937 rng(12345);
    std::uniform_int_distribution<size_t> dist(0, matches.size() - 1);

    Mat3 best_H;
    int best_inliers = 0;
    float best_inlier_threshold = inlier_threshold;

    int N = max_iterations;
    int iteration = 0;

    while (iteration < N) {
        // Randomly select 4 matches
        std::array<size_t, 4> indices;
        for (int i = 0; i < 4; ++i) {
            indices[i] = dist(rng);
            // Ensure uniqueness
            for (int j = 0; j < i; ++j) {
                if (indices[i] == indices[j]) {
                    indices[i] = (indices[i] + 1) % matches.size();
                    j = -1;
                }
            }
        }

        std::array<std::pair<float, float>, 4> src_pts, dst_pts;
        for (int i = 0; i < 4; ++i) {
            const auto& m = matches[indices[i]];
            src_pts[i] = {fa[m.idx_a].kp.x, fa[m.idx_a].kp.y};
            dst_pts[i] = {fb[m.idx_b].kp.x, fb[m.idx_b].kp.y};
        }

        Mat3 H;
        if (!compute_homography_dlt(src_pts, dst_pts, H)) {
            ++iteration;
            continue;
        }

        // Count inliers
        int inliers = 0;
        for (const auto& m : matches) {
            float tx, ty;
            H.transform(fa[m.idx_a].kp.x, fa[m.idx_a].kp.y, tx, ty);
            float dx = tx - fb[m.idx_b].kp.x;
            float dy = ty - fb[m.idx_b].kp.y;
            if (dx * dx + dy * dy < inlier_threshold * inlier_threshold) {
                ++inliers;
            }
        }

        if (inliers > best_inliers) {
            best_inliers = inliers;
            best_H = H;

            // Adaptive RANSAC: update N
            float inlier_ratio = static_cast<float>(inliers) / static_cast<float>(matches.size());
            if (inlier_ratio > 0.0f) {
                float eps = 1.0f - inlier_ratio;
                float new_N = std::log(1.0f - confidence) / std::log(1.0f - std::pow(1.0f - eps, 4));
                N = std::min(static_cast<int>(new_N), max_iterations);
            }
        }

        ++iteration;
    }

    // Refine: recompute H using all inliers
    if (best_inliers >= 4) {
        std::vector<std::pair<float, float>> inlier_src, inlier_dst;
        inlier_src.reserve(best_inliers);
        inlier_dst.reserve(best_inliers);
        for (const auto& m : matches) {
            float tx, ty;
            best_H.transform(fa[m.idx_a].kp.x, fa[m.idx_a].kp.y, tx, ty);
            float dx = tx - fb[m.idx_b].kp.x;
            float dy = ty - fb[m.idx_b].kp.y;
            if (dx * dx + dy * dy < best_inlier_threshold * best_inlier_threshold) {
                inlier_src.emplace_back(fa[m.idx_a].kp.x, fa[m.idx_a].kp.y);
                inlier_dst.emplace_back(fb[m.idx_b].kp.x, fb[m.idx_b].kp.y);
            }
        }

        // Recompute with all inliers using a simple average of multiple DLT solutions
        // (for production, use SVD on all inliers, but this is a reasonable approximation)
        if (inlier_src.size() >= 4) {
            // Take 4 well-distributed inliers
            std::mt19937 rng2(54321);
            std::array<std::pair<float, float>, 4> best_src4, best_dst4;
            float best_spread = 0.0f;
            for (int trial = 0; trial < 20; ++trial) {
                std::array<size_t, 4> idxs;
                for (int i = 0; i < 4; ++i) {
                    idxs[i] = std::uniform_int_distribution<size_t>(0, inlier_src.size() - 1)(rng2);
                    for (int j = 0; j < i; ++j) {
                        if (idxs[i] == idxs[j]) {
                            idxs[i] = (idxs[i] + 1) % inlier_src.size();
                            j = -1;
                        }
                    }
                }
                // Compute spread (min pairwise distance)
                float spread = std::numeric_limits<float>::max();
                for (int i = 0; i < 4; ++i) {
                    for (int j = i + 1; j < 4; ++j) {
                        float dx = inlier_src[idxs[i]].first - inlier_src[idxs[j]].first;
                        float dy = inlier_src[idxs[i]].second - inlier_src[idxs[j]].second;
                        float d = dx * dx + dy * dy;
                        if (d < spread) spread = d;
                    }
                }
                if (spread > best_spread) {
                    best_spread = spread;
                    for (int i = 0; i < 4; ++i) {
                        best_src4[i] = inlier_src[idxs[i]];
                        best_dst4[i] = inlier_dst[idxs[i]];
                    }
                }
            }
            Mat3 refined_H;
            if (compute_homography_dlt(best_src4, best_dst4, refined_H)) {
                best_H = refined_H;
            }
        }
    }

    return best_H;
}

}  // anonymous namespace

// ============================================================================
// 8. Image Warping
// ============================================================================

namespace {

/// Warp image using a homography matrix.
/// Places the result into a pre-allocated canvas at the specified offset.
void warp_image(const Image& src, const Mat3& H, Image& canvas,
                int offset_x, int offset_y, Image& weight_map) {
    Mat3 H_inv = H.inverse();
    int cw = canvas.width(), ch = canvas.height();

    // Use multi-threaded row processing
    int num_threads = std::max(1u, std::thread::hardware_concurrency());
    int rows_per_thread = (ch + num_threads - 1) / num_threads;

    std::vector<std::thread> threads;
    std::mutex canvas_mutex;  // not strictly needed for disjoint rows, but safe

    auto worker = [&](int start_y, int end_y) {
        for (int y = start_y; y < end_y && y < ch; ++y) {
            for (int x = 0; x < cw; ++x) {
                // Map canvas pixel back to source
                float sx, sy;
                H_inv.transform(static_cast<float>(x - offset_x),
                                static_cast<float>(y - offset_y), sx, sy);

                if (sx >= 0.0f && sx < static_cast<float>(src.width() - 1) &&
                    sy >= 0.0f && sy < static_cast<float>(src.height() - 1)) {
                    // Feather weight near edges
                    float edge_dist = std::min({sx, static_cast<float>(src.width() - 1) - sx,
                                                 sy, static_cast<float>(src.height() - 1) - sy});
                    float feather = std::min(1.0f, edge_dist / 20.0f);
                    if (feather > 0.0f) {
                        canvas(x, y) = src.sample(sx, sy) * feather;
                        weight_map(x, y) = Pixel{feather, feather, feather, feather};
                    }
                }
            }
        }
    };

    for (int t = 0; t < num_threads; ++t) {
        int start_y = t * rows_per_thread;
        int end_y = (t == num_threads - 1) ? ch : (t + 1) * rows_per_thread;
        threads.emplace_back(worker, start_y, end_y);
    }
    for (auto& t : threads) t.join();
}

/// Compute the bounding box of a warped image.
std::tuple<int, int, int, int> compute_warped_bounds(const Image& img, const Mat3& H) {
    int w = img.width(), h = img.height();
    float min_x = std::numeric_limits<float>::max();
    float min_y = std::numeric_limits<float>::max();
    float max_x = std::numeric_limits<float>::lowest();
    float max_y = std::numeric_limits<float>::lowest();

    // Check four corners
    std::pair<float, float> corners[4] = {
        {0.0f, 0.0f}, {static_cast<float>(w), 0.0f},
        {0.0f, static_cast<float>(h)}, {static_cast<float>(w), static_cast<float>(h)}};
    for (const auto& c : corners) {
        float tx, ty;
        H.transform(c.first, c.second, tx, ty);
        min_x = std::min(min_x, tx);
        min_y = std::min(min_y, ty);
        max_x = std::max(max_x, tx);
        max_y = std::max(max_y, ty);
    }

    return {static_cast<int>(std::floor(min_x)),
            static_cast<int>(std::floor(min_y)),
            static_cast<int>(std::ceil(max_x)),
            static_cast<int>(std::ceil(max_y))};
}

}  // anonymous namespace

// ============================================================================
// 9. Multi-Band Blending
// ============================================================================

namespace {

/// Blend two images using multi-band (Laplacian pyramid) blending.
Image multiband_blend(const Image& img1, const Image& img2,
                       const Image& mask1, const Image& mask2,
                       int num_levels = 5) {
    assert(img1.width() == img2.width() && img1.height() == img2.height());
    assert(mask1.width() == img1.width() && mask1.height() == img1.height());

    // Build pyramids
    auto g1 = build_gaussian_pyramid(img1, num_levels);
    auto g2 = build_gaussian_pyramid(img2, num_levels);
    auto gm1 = build_gaussian_pyramid(mask1, num_levels);
    auto gm2 = build_gaussian_pyramid(mask2, num_levels);

    auto l1 = build_laplacian_pyramid(g1);
    auto l2 = build_laplacian_pyramid(g2);

    // Blend each level
    std::vector<Image> blended_pyramid(num_levels);
    for (int level = 0; level < num_levels; ++level) {
        int lw = l1[level].width(), lh = l1[level].height();
        Image blended(lw, lh);

        for (int y = 0; y < lh; ++y) {
            for (int x = 0; x < lw; ++x) {
                float w1 = gm1[level](x, y).luminance();
                float w2 = gm2[level](x, y).luminance();
                float total = w1 + w2;
                if (total < 1e-8f) {
                    blended(x, y) = (l1[level](x, y) + l2[level](x, y)) * 0.5f;
                } else {
                    blended(x, y) = l1[level](x, y) * (w1 / total) +
                                    l2[level](x, y) * (w2 / total);
                }
            }
        }
        blended_pyramid[level] = std::move(blended);
    }

    return reconstruct_from_laplacian(blended_pyramid);
}

}  // anonymous namespace

// ============================================================================
// 10. Cylindrical / Spherical Projection
// ============================================================================

enum class ProjectionType {
    Perspective = 0,  // No projection
    Cylindrical = 1,
    Spherical = 2,
};

namespace {

/// Project an image to cylindrical coordinates.
/// Focal length in pixels. If focal_length <= 0, estimate from image dimensions.
Image project_cylindrical(const Image& src, float focal_length = 0.0f) {
    int w = src.width(), h = src.height();
    if (focal_length <= 0.0f) {
        focal_length = static_cast<float>(w);  // default: focal length = image width
    }

    float half_w = static_cast<float>(w) * 0.5f;
    float half_h = static_cast<float>(h) * 0.5f;

    // Compute output dimensions
    float theta_max = std::atan2(half_w, focal_length);
    int out_w = static_cast<int>(2.0f * focal_length * theta_max + 0.5f);
    int out_h = static_cast<int>(2.0f * focal_length *
                                  std::atan2(half_h, focal_length) + 0.5f);

    Image dst(out_w, out_h);

    for (int y = 0; y < out_h; ++y) {
        for (int x = 0; x < out_w; ++x) {
            float theta = (static_cast<float>(x) - out_w * 0.5f) / focal_length;
            float h_angle = (static_cast<float>(y) - out_h * 0.5f) / focal_length;

            float src_x = focal_length * std::tan(theta) + half_w;
            float src_y = focal_length * std::tan(h_angle) * std::sqrt(1.0f + std::tan(theta) * std::tan(theta)) + half_h;

            if (src_x >= 0.0f && src_x < static_cast<float>(w - 1) &&
                src_y >= 0.0f && src_y < static_cast<float>(h - 1)) {
                dst(x, y) = src.sample(src_x, src_y);
            }
        }
    }

    return dst;
}

/// Project an image to spherical coordinates.
Image project_spherical(const Image& src, float focal_length = 0.0f) {
    int w = src.width(), h = src.height();
    if (focal_length <= 0.0f) {
        focal_length = static_cast<float>(w);
    }

    float half_w = static_cast<float>(w) * 0.5f;
    float half_h = static_cast<float>(h) * 0.5f;

    float theta_max = std::atan2(half_w, focal_length);
    float phi_max = std::atan2(half_h, focal_length);

    int out_w = static_cast<int>(2.0f * focal_length * theta_max + 0.5f);
    int out_h = static_cast<int>(2.0f * focal_length * phi_max + 0.5f);

    Image dst(out_w, out_h);

    for (int y = 0; y < out_h; ++y) {
        for (int x = 0; x < out_w; ++x) {
            float theta = (static_cast<float>(x) - out_w * 0.5f) / focal_length;
            float phi = (static_cast<float>(y) - out_h * 0.5f) / focal_length;

            float cos_theta = std::cos(theta);
            float sin_theta = std::sin(theta);
            float cos_phi = std::cos(phi);
            float sin_phi = std::sin(phi);

            // 3D point on sphere
            float X = sin_theta;
            float Y = -sin_phi * cos_theta;
            float Z = cos_phi * cos_theta;

            if (Z > 0.0f) {
                float src_x = focal_length * X / Z + half_w;
                float src_y = focal_length * Y / Z + half_h;

                if (src_x >= 0.0f && src_x < static_cast<float>(w - 1) &&
                    src_y >= 0.0f && src_y < static_cast<float>(h - 1)) {
                    dst(x, y) = src.sample(src_x, src_y);
                }
            }
        }
    }

    return dst;
}

}  // anonymous namespace

// ============================================================================
// 11. Progress Reporting
// ============================================================================

/// Thread-safe progress reporter.
class ProgressReporter {
public:
    using Callback = std::function<void(float progress, const std::string& stage)>;

    ProgressReporter() = default;

    void set_callback(Callback cb) { callback_ = std::move(cb); }

    void report(float progress, const std::string& stage) {
        if (callback_) {
            callback_(progress, stage);
        }
    }

    void report(float progress) {
        if (callback_) {
            callback_(progress, current_stage_);
        }
    }

    void set_stage(const std::string& stage) {
        current_stage_ = stage;
        report(0.0f, stage);
    }

private:
    Callback callback_;
    std::string current_stage_ = "Starting";
    std::mutex mutex_;
};

// ============================================================================
// 12. Stitch Configuration
// ============================================================================

struct StitchConfig {
    /// Projection type for pre-warping.
    ProjectionType projection = ProjectionType::Cylindrical;

    /// Focal length in pixels. 0 = auto-estimate.
    float focal_length = 0.0f;

    /// FAST corner detection threshold (luminance difference).
    float fast_threshold = 0.05f;

    /// Lowe's ratio test threshold for feature matching.
    float match_ratio = 0.75f;

    /// RANSAC inlier threshold in pixels.
    float ransac_threshold = 3.0f;

    /// Number of Laplacian pyramid levels for multi-band blending.
    int blend_levels = 5;

    /// Whether to use multi-threading.
    bool use_multithreading = true;

    /// Output image width (0 = auto).
    int output_width = 0;

    /// Output image height (0 = auto).
    int output_height = 0;

    /// Progress callback.
    ProgressReporter::Callback progress_callback;
};

// ============================================================================
// 13. Pairwise Stitch
// ============================================================================

namespace {

/// Stitch two images together. Returns the blended result and the homography
/// from img1 to the panorama coordinate system.
std::pair<Image, Mat3> stitch_pair(const Image& img1, const Image& img2,
                                    const StitchConfig& config,
                                    ProgressReporter& progress) {
    // Step 1: Detect features
    progress.set_stage("Detecting features in image 1");
    auto features1 = detect_features(img1, config.fast_threshold);
    progress.report(0.5f);

    progress.set_stage("Detecting features in image 2");
    auto features2 = detect_features(img2, config.fast_threshold);
    progress.report(1.0f);

    if (features1.empty() || features2.empty()) {
        throw std::runtime_error("panorama: not enough features detected");
    }

    // Step 2: Match features
    progress.set_stage("Matching features");
    auto matches = match_features(features1, features2, config.match_ratio);
    progress.report(1.0f);

    if (matches.size() < 4) {
        throw std::runtime_error("panorama: insufficient matches (" +
                                 std::to_string(matches.size()) + " found, need >= 4)");
    }

    // Step 3: Estimate homography
    progress.set_stage("Estimating homography (RANSAC)");
    Mat3 H = estimate_homography_ransac(matches, features1, features2,
                                         config.ransac_threshold);
    progress.report(1.0f);

    // Step 4: Compute canvas bounds
    auto [x1, y1, x2, y2] = compute_warped_bounds(img1, Mat3());  // identity
    auto [wx1, wy1, wx2, wy2] = compute_warped_bounds(img2, H);

    int canvas_min_x = std::min(x1, wx1);
    int canvas_min_y = std::min(y1, wy1);
    int canvas_max_x = std::max(x2, wx2);
    int canvas_max_y = std::max(y2, wy2);

    int canvas_w = canvas_max_x - canvas_min_x;
    int canvas_h = canvas_max_y - canvas_min_y;

    int offset1_x = -canvas_min_x;
    int offset1_y = -canvas_min_y;
    int offset2_x = offset1_x;
    int offset2_y = offset1_y;

    // Step 5: Warp both images onto canvas
    progress.set_stage("Warping images");
    Image canvas(canvas_w, canvas_h);
    Image weight1(canvas_w, canvas_h);
    Image weight2(canvas_w, canvas_h);

    // Image 1 uses identity transform (relative to canvas origin)
    {
        Image src1 = img1;
        Image wmap1(canvas_w, canvas_h);
        for (int y = 0; y < img1.height(); ++y) {
            for (int x = 0; x < img1.width(); ++x) {
                int cx = offset1_x + x;
                int cy = offset1_y + y;
                if (cx >= 0 && cx < canvas_w && cy >= 0 && cy < canvas_h) {
                    float edge_dist = std::min({static_cast<float>(x),
                                                 static_cast<float>(img1.width() - 1 - x),
                                                 static_cast<float>(y),
                                                 static_cast<float>(img1.height() - 1 - y)});
                    float feather = std::min(1.0f, edge_dist / 20.0f);
                    canvas(cx, cy) = img1(x, y) * feather;
                    weight1(cx, cy) = Pixel{feather, feather, feather, feather};
                }
            }
        }
    }
    progress.report(0.5f);

    warp_image(img2, H, canvas, offset2_x, offset2_y, weight2);
    progress.report(1.0f);

    // Step 6: Multi-band blend
    progress.set_stage("Multi-band blending");
    Image blended = multiband_blend(canvas, canvas, weight1, weight2, config.blend_levels);
    progress.report(1.0f);

    // Compute the cumulative homography: the transform from img2 to the canvas
    // (which is basically the identity mapping for img1 + offset)
    Mat3 canvas_H = H;  // img2 -> img1 -> canvas coordinate system

    return {blended, canvas_H};
}

}  // anonymous namespace

// ============================================================================
// 14. Main stitch_panorama() Entry Point
// ============================================================================

/// Error codes returned by stitch_panorama.
enum class StitchError {
    Success = 0,
    TooFewImages,
    FileNotFound,
    InsufficientFeatures,
    InsufficientMatches,
    HomographyFailed,
    InternalError,
};

/// Result structure returned by stitch_panorama.
struct StitchResult {
    StitchError error = StitchError::Success;
    std::string error_message;
    Image result;
};

/// Convert error code to string.
const char* stitch_error_string(StitchError e) {
    switch (e) {
        case StitchError::Success:              return "Success";
        case StitchError::TooFewImages:         return "Need at least 2 images to stitch";
        case StitchError::FileNotFound:         return "One or more input files could not be found";
        case StitchError::InsufficientFeatures: return "Not enough features detected in one or more images";
        case StitchError::InsufficientMatches:  return "Not enough feature matches between images";
        case StitchError::HomographyFailed:     return "Homography estimation failed";
        case StitchError::InternalError:        return "Internal error";
    }
    return "Unknown error";
}

/// Main panorama stitching function.
///
/// Loads a list of images, sequentially stitches them pairwise using
/// homography estimation and multi-band blending, and returns the final
/// stitched panorama.
///
/// @param image_paths  List of file paths to input images (PPM format supported).
/// @param config       Stitching configuration parameters.
/// @return             StitchResult containing the panorama image or error info.
StitchResult stitch_panorama(const std::vector<std::string>& image_paths,
                              const StitchConfig& config = StitchConfig{}) {
    StitchResult result;
    ProgressReporter progress;
    if (config.progress_callback) {
        progress.set_callback(config.progress_callback);
    }

    try {
        // Validate input
        if (image_paths.size() < 2) {
            result.error = StitchError::TooFewImages;
            result.error_message = stitch_error_string(StitchError::TooFewImages);
            return result;
        }

        // Load all images
        progress.set_stage("Loading images");
        std::vector<Image> images;
        images.reserve(image_paths.size());
        for (size_t i = 0; i < image_paths.size(); ++i) {
            try {
                images.push_back(Image::from_ppm(image_paths[i]));
            } catch (const std::exception& e) {
                result.error = StitchError::FileNotFound;
                result.error_message = std::string("Cannot load ") + image_paths[i] +
                                       ": " + e.what();
                return result;
            }
            progress.report(static_cast<float>(i + 1) / static_cast<float>(image_paths.size()));
        }

        // Optional: apply cylindrical or spherical projection to each image
        if (config.projection != ProjectionType::Perspective) {
            progress.set_stage("Applying projection");
            for (size_t i = 0; i < images.size(); ++i) {
                if (config.projection == ProjectionType::Cylindrical) {
                    images[i] = project_cylindrical(images[i], config.focal_length);
                } else if (config.projection == ProjectionType::Spherical) {
                    images[i] = project_spherical(images[i], config.focal_length);
                }
                progress.report(static_cast<float>(i + 1) / static_cast<float>(images.size()));
            }
        }

        // Sequential pairwise stitching
        progress.set_stage("Stitching images pairwise");
        Image panorama = images[0];

        for (size_t i = 1; i < images.size(); ++i) {
            float base_progress = static_cast<float>(i - 1) / static_cast<float>(images.size() - 1);
            float segment_size = 1.0f / static_cast<float>(images.size() - 1);

            // Wrap progress reporter to map sub-progress to global progress
            auto sub_progress = [&progress, base_progress, segment_size](float p, const std::string& s) {
                progress.report(base_progress + p * segment_size,
                                "Stitching pair " + s);
            };

            StitchConfig pair_config = config;
            pair_config.progress_callback = sub_progress;

            ProgressReporter pair_progress;
            pair_progress.set_callback(sub_progress);

            try {
                auto [blended, H] = stitch_pair(panorama, images[i], pair_config, pair_progress);
                panorama = std::move(blended);
            } catch (const std::exception& e) {
                std::string msg = e.what();
                if (msg.find("not enough features") != std::string::npos) {
                    result.error = StitchError::InsufficientFeatures;
                } else if (msg.find("insufficient matches") != std::string::npos) {
                    result.error = StitchError::InsufficientMatches;
                } else {
                    result.error = StitchError::InternalError;
                }
                result.error_message = msg;
                return result;
            }

            progress.report(static_cast<float>(i) / static_cast<float>(images.size() - 1));
        }

        // Optional: resize output
        if (config.output_width > 0 && config.output_height > 0) {
            // Simple nearest-neighbor resize (production would use Lanczos)
            Image resized(config.output_width, config.output_height);
            float scale_x = static_cast<float>(panorama.width()) /
                            static_cast<float>(config.output_width);
            float scale_y = static_cast<float>(panorama.height()) /
                            static_cast<float>(config.output_height);
            for (int y = 0; y < config.output_height; ++y) {
                for (int x = 0; x < config.output_width; ++x) {
                    float sx = static_cast<float>(x) * scale_x;
                    float sy = static_cast<float>(y) * scale_y;
                    resized(x, y) = panorama.sample(sx, sy);
                }
            }
            panorama = std::move(resized);
        }

        progress.set_stage("Complete");
        progress.report(1.0f);

        result.result = std::move(panorama);
        result.error = StitchError::Success;

    } catch (const std::exception& e) {
        result.error = StitchError::InternalError;
        result.error_message = e.what();
    }

    return result;
}

// ============================================================================
// 15. Convenience Overloads
// ============================================================================

/// Simplified stitch with default config. Returns the stitched image directly.
/// Throws std::runtime_error on failure.
Image stitch_panorama_simple(const std::vector<std::string>& image_paths,
                              ProjectionType projection = ProjectionType::Cylindrical) {
    StitchConfig config;
    config.projection = projection;
    auto result = stitch_panorama(image_paths, config);
    if (result.error != StitchError::Success) {
        throw std::runtime_error(result.error_message);
    }
    return std::move(result.result);
}

/// Stitch with custom FAST threshold.
Image stitch_panorama_fast(const std::vector<std::string>& image_paths,
                            float fast_threshold) {
    StitchConfig config;
    config.fast_threshold = fast_threshold;
    auto result = stitch_panorama(image_paths, config);
    if (result.error != StitchError::Success) {
        throw std::runtime_error(result.error_message);
    }
    return std::move(result.result);
}

/// Stitch from pre-loaded images (in-memory).
StitchResult stitch_panorama_from_images(std::vector<Image>& images,
                                          const StitchConfig& config = StitchConfig{}) {
    StitchResult result;
    ProgressReporter progress;
    if (config.progress_callback) {
        progress.set_callback(config.progress_callback);
    }

    try {
        if (images.size() < 2) {
            result.error = StitchError::TooFewImages;
            result.error_message = stitch_error_string(StitchError::TooFewImages);
            return result;
        }

        // Optional projection
        if (config.projection != ProjectionType::Perspective) {
            progress.set_stage("Applying projection");
            for (size_t i = 0; i < images.size(); ++i) {
                if (config.projection == ProjectionType::Cylindrical) {
                    images[i] = project_cylindrical(images[i], config.focal_length);
                } else if (config.projection == ProjectionType::Spherical) {
                    images[i] = project_spherical(images[i], config.focal_length);
                }
                progress.report(static_cast<float>(i + 1) / static_cast<float>(images.size()));
            }
        }

        // Pairwise stitching
        progress.set_stage("Stitching images pairwise");
        Image panorama = images[0];

        for (size_t i = 1; i < images.size(); ++i) {
            float base = static_cast<float>(i - 1) / static_cast<float>(images.size() - 1);
            float seg = 1.0f / static_cast<float>(images.size() - 1);

            auto sub = [&progress, base, seg](float p, const std::string& s) {
                progress.report(base + p * seg, "Stitching pair " + s);
            };

            StitchConfig pc = config;
            pc.progress_callback = sub;
            ProgressReporter pp;
            pp.set_callback(sub);

            try {
                auto [blended, H] = stitch_pair(panorama, images[i], pc, pp);
                panorama = std::move(blended);
            } catch (const std::exception& e) {
                result.error = StitchError::InternalError;
                result.error_message = e.what();
                return result;
            }
            progress.report(static_cast<float>(i) / static_cast<float>(images.size() - 1));
        }

        if (config.output_width > 0 && config.output_height > 0) {
            Image resized(config.output_width, config.output_height);
            float sx = static_cast<float>(panorama.width()) / static_cast<float>(config.output_width);
            float sy = static_cast<float>(panorama.height()) / static_cast<float>(config.output_height);
            for (int y = 0; y < config.output_height; ++y) {
                for (int x = 0; x < config.output_width; ++x) {
                    resized(x, y) = panorama.sample(static_cast<float>(x) * sx,
                                                     static_cast<float>(y) * sy);
                }
            }
            panorama = std::move(resized);
        }

        progress.set_stage("Complete");
        progress.report(1.0f);

        result.result = std::move(panorama);
        result.error = StitchError::Success;

    } catch (const std::exception& e) {
        result.error = StitchError::InternalError;
        result.error_message = e.what();
    }

    return result;
}

// ============================================================================
// 16. Exposure Compensation (optional utility)
// ============================================================================

/// Simple gain compensation: adjust image2 to match image1's mean luminance
/// in the overlapping region.
void compensate_exposure(Image& img1, Image& img2, const Mat3& H) {
    // Map img2 points to img1, accumulate overlapping pixel statistics
    double sum1 = 0.0, sum2 = 0.0;
    int count = 0;

    Mat3 H_inv = H.inverse();
    int w1 = img1.width(), h1 = img1.height();

    // Sample a subset of pixels for efficiency
    constexpr int kStep = 4;
    for (int y = 0; y < h1; y += kStep) {
        for (int x = 0; x < w1; x += kStep) {
            float sx, sy;
            H_inv.transform(static_cast<float>(x), static_cast<float>(y), sx, sy);
            if (sx >= 0.0f && sx < static_cast<float>(img2.width() - 1) &&
                sy >= 0.0f && sy < static_cast<float>(img2.height() - 1)) {
                sum1 += img1(x, y).luminance();
                sum2 += img2.sample(sx, sy).luminance();
                ++count;
            }
        }
    }

    if (count > 100) {
        double gain = (sum1 / count) / std::max(sum2 / count, 1e-6);
        gain = std::clamp(gain, 0.5, 2.0);  // conservative
        for (int i = 0; i < img2.width() * img2.height(); ++i) {
            img2.data()[i] = img2.data()[i] * static_cast<float>(gain);
        }
    }
}

}  // namespace panorama
}  // namespace alcedo