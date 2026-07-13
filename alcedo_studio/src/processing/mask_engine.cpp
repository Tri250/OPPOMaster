//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "mask/mask_definition.hpp"

#include <algorithm>
#include <cmath>
#include <vector>
#include <memory>

#include "image/image.hpp"
#include "edit/operators/color/conversion/Oklab_cvt.hpp"

namespace alcedo {
namespace mask {

namespace {

// Internal helper for Oklab color distance calculation
inline float oklab_distance_squared(const OklabCvt::Oklab& a, const OklabCvt::Oklab& b) {
    float dl = a.l_ - b.l_;
    float da = a.a_ - b.a_;
    float db = a.b_ - b.b_;
    return dl * dl + da * da + db * db;
}

// Gaussian falloff for brush stamps
inline float gaussian_falloff(float dist_sq, float radius_sq, float hardness) {
    if (dist_sq >= radius_sq) {
        return 0.0f;
    }
    float t = dist_sq / radius_sq;
    // Smooth falloff based on hardness: hardness = 1 gives sharp edge, hardness = 0 gives full gaussian
    float sigma = (1.0f - hardness) * 0.5f + 0.125f;
    return std::exp(-t / (2.0f * sigma * sigma));
}

// Clamp mask value to [0, 1]
inline float clamp_mask(float val) {
    return std::clamp(val, 0.0f, 1.0f);
}

// Convert 0-1 float to 0-255 byte
inline unsigned char float_to_byte(float val) {
    return static_cast<unsigned char>(std::round(val * 255.0f));
}

// Bresenham's line algorithm for connecting brush points
std::vector<std::pair<int, int>> bresenham_line(int x0, int y0, int x1, int y1) {
    std::vector<std::pair<int, int>> points;

    int dx = std::abs(x1 - x0);
    int dy = std::abs(y1 - y0);
    int sx = x0 < x1 ? 1 : -1;
    int sy = y0 < y1 ? 1 : -1;
    int err = dx - dy;

    while (true) {
        points.emplace_back(x0, y0);
        if (x0 == x1 && y0 == y1) {
            break;
        }
        int e2 = 2 * err;
        if (e2 > -dy) {
            err -= dy;
            x0 += sx;
        }
        if (e2 < dx) {
            err += dx;
            y0 += sy;
        }
    }

    return points;
}

} // anonymous namespace

// Forward declarations
static void generate_brush_mask(
    const std::vector<float>& parameters,
    int width,
    int height,
    std::vector<float>& output
);
static void generate_linear_gradient_mask(
    const std::vector<float>& parameters,
    int width,
    int height,
    std::vector<float>& output
);
static void generate_radial_gradient_mask(
    const std::vector<float>& parameters,
    int width,
    int height,
    std::vector<float>& output
);
static void generate_color_mask(
    const std::vector<float>& parameters,
    const image::Image& image,
    int width,
    int height,
    std::vector<float>& output
);
static void generate_luminance_mask(
    const std::vector<float>& parameters,
    const image::Image& image,
    int width,
    int height,
    std::vector<float>& output
);

/// Generate the combined bitmap from all sub-masks
/// Result: 0 = masked, 255 = fully visible
MaskBitmap generate_mask_bitmap(
    const MaskDefinition& definition,
    const image::Image& image,
    int width,
    int height
) {
    // Initialize with 0 (fully masked)
    std::vector<float> accumulator(width * height, 0.0f);

    for (const auto& submask : definition.sub_masks) {
        if (!submask.visible) {
            continue;
        }

        std::vector<float> submask_buffer(width * height, 0.0f);

        // Generate based on mask type
        switch (submask.mask_type) {
            case MaskType::Brush:
                generate_brush_mask(submask.parameters, width, height, submask_buffer);
                break;
            case MaskType::GradientLinear:
                generate_linear_gradient_mask(submask.parameters, width, height, submask_buffer);
                break;
            case MaskType::GradientRadial:
                generate_radial_gradient_mask(submask.parameters, width, height, submask_buffer);
                break;
            case MaskType::Color:
                generate_color_mask(submask.parameters, image, width, height, submask_buffer);
                break;
            case MaskType::Luminance:
                generate_luminance_mask(submask.parameters, image, width, height, submask_buffer);
                break;
            case MaskType::AiSubject:
            case MaskType::AiSky:
            case MaskType::AiDepth:
            case MaskType::AiForeground:
                // These are handled externally by AI segmentation
                // For now, just leave as all 0 - will be filled by AI output
                break;
        }

        // Apply invert if needed
        if (submask.invert) {
            for (auto& val : submask_buffer) {
                val = 1.0f - val;
            }
        }

        // Apply opacity
        float opacity = std::clamp(submask.opacity / 100.0f, 0.0f, 1.0f);
        if (opacity < 1.0f) {
            for (auto& val : submask_buffer) {
                val *= opacity;
            }
        }

        // Blend into accumulator based on blend mode
        switch (submask.mode) {
            case SubMaskMode::Additive:
                for (size_t i = 0; i < accumulator.size(); ++i) {
                    accumulator[i] = clamp_mask(accumulator[i] + submask_buffer[i]);
                }
                break;
            case SubMaskMode::Subtractive:
                for (size_t i = 0; i < accumulator.size(); ++i) {
                    accumulator[i] = clamp_mask(accumulator[i] - submask_buffer[i]);
                }
                break;
            case SubMaskMode::Intersect:
                for (size_t i = 0; i < accumulator.size(); ++i) {
                    accumulator[i] = std::min(accumulator[i], submask_buffer[i]);
                }
                break;
        }
    }

    // Apply top-level invert
    if (definition.invert) {
        for (auto& val : accumulator) {
            val = 1.0f - val;
        }
    }

    // Apply top-level opacity
    float global_opacity = std::clamp(definition.opacity / 100.0f, 0.0f, 1.0f);
    if (global_opacity < 1.0f) {
        for (auto& val : accumulator) {
            val *= global_opacity;
        }
    }

    // Convert float to 8-bit bitmap
    MaskBitmap result;
    result.resize(width, height);
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            size_t idx = static_cast<size_t>(y * width + x);
            result.at(x, y) = float_to_byte(accumulator[idx]);
        }
    }

    return result;
}

/// Creates a brush stroke mask from a list of points with pressure
/// Parameters format: [brush_radius, hardness, x0, y0, pressure0, x1, y1, pressure1, ...]
void generate_brush_mask(
    const std::vector<float>& parameters,
    int width,
    int height,
    std::vector<float>& output
) {
    if (parameters.size() < 3) {
        return;
    }

    float radius = parameters[0];
    float hardness = std::clamp(parameters[1], 0.0f, 1.0f);

    int radius_px = static_cast<int>(std::ceil(radius));
    float radius_sq = radius * radius;

    // Process each brush point and draw stamps
    for (size_t i = 2; i + 2 < parameters.size(); i += 3) {
        float cx_f = parameters[i];
        float cy_f = parameters[i + 1];
        float pressure = std::clamp(parameters[i + 2], 0.0f, 1.0f);

        int cx = static_cast<int>(std::round(cx_f));
        int cy = static_cast<int>(std::round(cy_f));

        // Calculate affected bounding box
        int x0 = std::max(0, cx - radius_px);
        int x1 = std::min(width - 1, cx + radius_px);
        int y0 = std::max(0, cy - radius_px);
        int y1 = std::min(height - 1, cy + radius_px);

        // Draw circular stamp with gaussian falloff
        for (int y = y0; y <= y1; ++y) {
            for (int x = x0; x <= x1; ++x) {
                float dx = static_cast<float>(x) - cx_f;
                float dy = static_cast<float>(y) - cy_f;
                float dist_sq = dx * dx + dy * dy;

                if (dist_sq >= radius_sq) {
                    continue;
                }

                float falloff = gaussian_falloff(dist_sq, radius_sq, hardness);
                size_t idx = static_cast<size_t>(y * width + x);
                output[idx] = clamp_mask(output[idx] + falloff * pressure);
            }
        }

        // If this isn't the first point, connect to previous point with line
        if (i > 2) {
            float prev_cx_f = parameters[i - 3];
            float prev_cy_f = parameters[i - 2];
            int prev_cx = static_cast<int>(std::round(prev_cx_f));
            int prev_cy = static_cast<int>(std::round(prev_cy_f));

            auto line_points = bresenham_line(prev_cx, prev_cy, cx, cy);
            for (const auto& pt : line_points) {
                int lx = pt.first;
                int ly = pt.second;

                int lx0 = std::max(0, lx - radius_px);
                int lx1 = std::min(width - 1, lx + radius_px);
                int ly0 = std::max(0, ly - radius_px);
                int ly1 = std::min(height - 1, ly + radius_px);

                for (int yy = ly0; yy <= ly1; ++yy) {
                    for (int xx = lx0; xx <= lx1; ++xx) {
                        float dx = static_cast<float>(xx) - static_cast<float>(lx);
                        float dy = static_cast<float>(yy) - static_cast<float>(ly);
                        float dist_sq = dx * dx + dy * dy;

                        if (dist_sq >= radius_sq) {
                            continue;
                        }

                        float falloff = gaussian_falloff(dist_sq, radius_sq, hardness);
                        size_t idx = static_cast<size_t>(yy * width + xx);
                        output[idx] = clamp_mask(output[idx] + falloff * pressure * 0.5f);
                    }
                }
            }
        }
    }
}

/// Creates a linear gradient mask from start point to end point
/// Parameters format: [start_x, start_y, end_x, end_y, feather_start, feather_end, invert_gradient]
/// Feather values are 0-1, 0 = hard edge at the endpoint
void generate_linear_gradient_mask(
    const std::vector<float>& parameters,
    int width,
    int height,
    std::vector<float>& output
) {
    if (parameters.size() < 4) {
        return;
    }

    float start_x = parameters[0];
    float start_y = parameters[1];
    float end_x = parameters[2];
    float end_y = parameters[3];
    float feather_start = parameters.size() > 4 ? parameters[4] : 0.0f;
    float feather_end = parameters.size() > 5 ? parameters[5] : 0.0f;
    bool invert = parameters.size() > 6 ? parameters[6] > 0.5f : false;

    // Vector from start to end
    float dx = end_x - start_x;
    float dy = end_y - start_y;
    float len_sq = dx * dx + dy * dy;

    if (len_sq < 1e-6f) {
        return;
    }

    float inv_len = 1.0f / std::sqrt(len_sq);
    dx *= inv_len;
    dy *= inv_len;

    float feather_start_dist = std::max(0.0f, feather_start * std::sqrt(len_sq));
    float feather_end_dist = std::max(0.0f, feather_end * std::sqrt(len_sq));

    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            // Project point onto gradient line
            float proj_x = static_cast<float>(x) - start_x;
            float proj_y = static_cast<float>(y) - start_y;
            float t = proj_x * dx + proj_y * dy;

            float val;
            if (t <= 0.0f) {
                val = 0.0f;
            } else if (t >= std::sqrt(len_sq)) {
                val = 1.0f;
            } else {
                if (feather_start_dist > 0.0f && t < feather_start_dist) {
                    val = t / feather_start_dist;
                } else if (feather_end_dist > 0.0f && t > (std::sqrt(len_sq) - feather_end_dist)) {
                    val = (std::sqrt(len_sq) - t) / feather_end_dist;
                    val = 1.0f - val;
                } else {
                    val = t / std::sqrt(len_sq);
                }
            }

            if (invert) {
                val = 1.0f - val;
            }

            size_t idx = static_cast<size_t>(y * width + x);
            output[idx] = val;
        }
    }
}

/// Creates a radial gradient mask from center with radius
/// Parameters format: [center_x, center_y, radius_x, radius_y, feather_inner, feather_outer]
void generate_radial_gradient_mask(
    const std::vector<float>& parameters,
    int width,
    int height,
    std::vector<float>& output
) {
    if (parameters.size() < 4) {
        return;
    }

    float cx = parameters[0];
    float cy = parameters[1];
    float rx = parameters[2];
    float ry = parameters.size() > 3 ? parameters[3] : rx;
    float feather_inner = parameters.size() > 4 ? parameters[4] : 0.0f;
    float feather_outer = parameters.size() > 5 ? parameters[5] : 0.0f;

    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            float dx = (static_cast<float>(x) - cx) / rx;
            float dy = (static_cast<float>(y) - cy) / ry;
            float dist = std::sqrt(dx * dx + dy * dy);

            float val;
            if (dist <= 0.0f) {
                val = 0.0f;
            } else if (dist <= feather_inner) {
                val = dist / feather_inner;
            } else if (dist <= 1.0f - feather_outer) {
                val = 1.0f;
            } else if (dist <= 1.0f) {
                val = (1.0f - dist) / feather_outer;
            } else {
                val = 0.0f;
            }

            size_t idx = static_cast<size_t>(y * width + x);
            output[idx] = clamp_mask(val);
        }
    }
}

/// Creates a mask based on color similarity (parametric color mask)
/// Parameters format: [target_r, target_g, target_b, tolerance, luminosity_range_low, luminosity_range_high]
/// Colors are in ACES 0-1 floating point
void generate_color_mask(
    const std::vector<float>& parameters,
    const image::Image& image,
    int width,
    int height,
    std::vector<float>& output
) {
    if (parameters.size() < 4) {
        return;
    }

    float target_r = parameters[0];
    float target_g = parameters[1];
    float target_b = parameters[2];
    float tolerance = parameters[3];
    float lum_low = parameters.size() > 4 ? parameters[4] : 0.0f;
    float lum_high = parameters.size() > 5 ? parameters[5] : 1.0f;

    // Convert target color to Oklab for perceptual matching
    Pixel target_pixel;
    target_pixel.r_ = target_r;
    target_pixel.g_ = target_g;
    target_pixel.b_ = target_b;
    target_pixel.a_ = 1.0f;
    OklabCvt::Oklab target_ok = OklabCvt::ACESRGB2Oklab(target_pixel);

    float tolerance_sq = tolerance * tolerance * 0.1f; // Scaling for better usability

    cv::Mat& cpu_data = const_cast<image::Image&>(image).GetCPUData();

    int img_width = cpu_data.cols;
    int img_height = cpu_data.rows;

    float scale_x = static_cast<float>(width) / static_cast<float>(img_width);
    float scale_y = static_cast<float>(height) / static_cast<float>(img_height);

    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            // Sample from original image (handle resampling)
            int orig_x = std::clamp(static_cast<int>(static_cast<float>(x) / scale_x + 0.5f), 0, img_width - 1);
            int orig_y = std::clamp(static_cast<int>(static_cast<float>(y) / scale_y + 0.5f), 0, img_height - 1);

            float* pixel_ptr = cpu_data.ptr<float>(orig_y);
            Pixel pixel;
            pixel.r_ = pixel_ptr[orig_x * 3 + 0];
            pixel.g_ = pixel_ptr[orig_x * 3 + 1];
            pixel.b_ = pixel_ptr[orig_x * 3 + 2];
            pixel.a_ = 1.0f;

            OklabCvt::Oklab pixel_ok = OklabCvt::ACESRGB2Oklab(pixel);

            float dist_sq = oklab_distance_squared(target_ok, pixel_ok);
            float lum = pixel_ok.l_;

            float val;
            if (dist_sq <= tolerance_sq && lum >= lum_low && lum <= lum_high) {
                // Smooth falloff
                val = 1.0f - std::clamp(dist_sq / tolerance_sq, 0.0f, 1.0f);
            } else {
                val = 0.0f;
            }

            size_t idx = static_cast<size_t>(y * width + x);
            output[idx] = val;
        }
    }
}

/// Creates a mask based on luminance ranges
/// Parameters format: [low_threshold, high_threshold, feather_low, feather_high]
/// Luminance is 0-100 in Oklab
void generate_luminance_mask(
    const std::vector<float>& parameters,
    const image::Image& image,
    int width,
    int height,
    std::vector<float>& output
) {
    if (parameters.size() < 2) {
        return;
    }

    float low = parameters[0] / 100.0f;
    float high = parameters[1] / 100.0f;
    float feather_low = parameters.size() > 2 ? parameters[2] / 100.0f : 0.0f;
    float feather_high = parameters.size() > 3 ? parameters[3] / 100.0f : 0.0f;

    cv::Mat& cpu_data = const_cast<image::Image&>(image).GetCPUData();

    int img_width = cpu_data.cols;
    int img_height = cpu_data.rows;

    float scale_x = static_cast<float>(width) / static_cast<float>(img_width);
    float scale_y = static_cast<float>(height) / static_cast<float>(img_height);

    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            int orig_x = std::clamp(static_cast<int>(static_cast<float>(x) / scale_x + 0.5f), 0, img_width - 1);
            int orig_y = std::clamp(static_cast<int>(static_cast<float>(y) / scale_y + 0.5f), 0, img_height - 1);

            float* pixel_ptr = cpu_data.ptr<float>(orig_y);
            Pixel pixel;
            pixel.r_ = pixel_ptr[orig_x * 3 + 0];
            pixel.g_ = pixel_ptr[orig_x * 3 + 1];
            pixel.b_ = pixel_ptr[orig_x * 3 + 2];
            pixel.a_ = 1.0f;

            OklabCvt::Oklab pixel_ok = OklabCvt::ACESRGB2Oklab(pixel);
            float lum = pixel_ok.l_; // L is already 0-1 in Oklab

            float val;
            if (lum < low - feather_low) {
                val = 0.0f;
            } else if (lum < low) {
                if (feather_low > 0.0f) {
                    val = (lum - (low - feather_low)) / feather_low;
                } else {
                    val = 0.0f;
                }
            } else if (lum > high + feather_high) {
                val = 0.0f;
            } else if (lum > high) {
                if (feather_high > 0.0f) {
                    val = ((high + feather_high) - lum) / feather_high;
                } else {
                    val = 0.0f;
                }
            } else {
                val = 1.0f;
            }

            size_t idx = static_cast<size_t>(y * width + x);
            output[idx] = clamp_mask(val);
        }
    }
}

/// Applies gaussian blur to mask edges for softening
std::vector<float> feather_mask(const std::vector<float>& input, int width, int height, float radius) {
    if (radius <= 0.0f) {
        return input;
    }

    std::vector<float> output(width * height, 0.0f);
    int radius_int = static_cast<int>(std::ceil(radius));

    // Box filter approximation of gaussian blur (multiple passes)
    std::vector<float> tmp = input;

    // Horizontal pass
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            int x0 = std::max(0, x - radius_int);
            int x1 = std::min(width - 1, x + radius_int);
            int count = x1 - x0 + 1;
            float sum = 0.0f;
            for (int xi = x0; xi <= x1; ++xi) {
                sum += tmp[static_cast<size_t>(y * width + xi)];
            }
            output[static_cast<size_t>(y * width + x)] = sum / static_cast<float>(count);
        }
    }

    tmp.swap(output);

    // Vertical pass
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            int y0 = std::max(0, y - radius_int);
            int y1 = std::min(height - 1, y + radius_int);
            int count = y1 - y0 + 1;
            float sum = 0.0f;
            for (int yi = y0; yi <= y1; ++yi) {
                sum += tmp[static_cast<size_t>(yi * width + x)];
            }
            output[static_cast<size_t>(y * width + x)] = sum / static_cast<float>(count);
        }
    }

    return output;
}

/// Dilate (grow) the mask - morphological operation
std::vector<float> grow_mask(const std::vector<float>& input, int width, int height, int iterations) {
    std::vector<float> current = input;
    std::vector<float> output(width * height);

    for (int iter = 0; iter < iterations; ++iter) {
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                float max_val = current[static_cast<size_t>(y * width + x)];

                // Check 3x3 neighborhood
                for (int dy = -1; dy <= 1; ++dy) {
                    int ny = y + dy;
                    if (ny < 0 || ny >= height) continue;
                    for (int dx = -1; dx <= 1; ++dx) {
                        int nx = x + dx;
                        if (nx < 0 || nx >= width) continue;
                        max_val = std::max(max_val, current[static_cast<size_t>(ny * width + nx)]);
                    }
                }

                output[static_cast<size_t>(y * width + x)] = max_val;
            }
        }

        current.swap(output);
    }

    return current;
}

/// Erode (shrink) the mask - morphological operation
std::vector<float> shrink_mask(const std::vector<float>& input, int width, int height, int iterations) {
    std::vector<float> current = input;
    std::vector<float> output(width * height);

    for (int iter = 0; iter < iterations; ++iter) {
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                float min_val = current[static_cast<size_t>(y * width + x)];

                // Check 3x3 neighborhood
                for (int dy = -1; dy <= 1; ++dy) {
                    int ny = y + dy;
                    if (ny < 0 || ny >= height) continue;
                    for (int dx = -1; dx <= 1; ++dx) {
                        int nx = x + dx;
                        if (nx < 0 || nx >= width) continue;
                        min_val = std::min(min_val, current[static_cast<size_t>(ny * width + nx)]);
                    }
                }

                output[static_cast<size_t>(y * width + x)] = min_val;
            }
        }

        current.swap(output);
    }

    return current;
}

/// Invert the mask
void invert_mask(std::vector<float>& mask) {
    for (auto& val : mask) {
        val = 1.0f - val;
    }
}

/// Applies a mask bitmap to an image (blends with original)
/// mask values 0 = original, 255 = fully affected by adjustment
void apply_mask_to_image(
    const MaskBitmap& mask,
    cv::Mat& image,
    const std::function<void(cv::Mat&)>& apply_adjustment
) {
    CV_Assert(image.depth() == CV_32F);

    int mask_width = mask.width();
    int mask_height = mask.height();
    int img_width = image.cols;
    int img_height = image.rows;

    // Create a copy of original for blending
    cv::Mat original = image.clone();

    // Apply the adjustment to the entire image
    apply_adjustment(image);

    // Scale mask to image size and blend
    float scale_x = static_cast<float>(mask_width) / static_cast<float>(img_width);
    float scale_y = static_cast<float>(mask_height) / static_cast<float>(img_height);

    int channels = image.channels();

    for (int y = 0; y < img_height; ++y) {
        float* orig_row = original.ptr<float>(y);
        float* adj_row = image.ptr<float>(y);

        for (int x = 0; x < img_width; ++x) {
            int mask_x = static_cast<int>(static_cast<float>(x) * scale_x);
            int mask_y = static_cast<int>(static_cast<float>(y) * scale_y);

            mask_x = std::clamp(mask_x, 0, mask_width - 1);
            mask_y = std::clamp(mask_y, 0, mask_height - 1);

            unsigned char mask_val = mask.at(mask_x, mask_y);
            float weight = static_cast<float>(mask_val) / 255.0f;

            for (int c = 0; c < channels; ++c) {
                int idx = x * channels + c;
                adj_row[idx] = orig_row[idx] * (1.0f - weight) + adj_row[idx] * weight;
            }
        }
    }
}

/// Overload for float mask buffer directly
void apply_mask_to_image(
    const std::vector<float>& mask,
    int mask_width,
    int mask_height,
    cv::Mat& image,
    const std::function<void(cv::Mat&)>& apply_adjustment
) {
    CV_Assert(image.depth() == CV_32F);

    int img_width = image.cols;
    int img_height = image.rows;

    cv::Mat original = image.clone();
    apply_adjustment(image);

    float scale_x = static_cast<float>(mask_width) / static_cast<float>(img_width);
    float scale_y = static_cast<float>(mask_height) / static_cast<float>(img_height);

    int channels = image.channels();

    for (int y = 0; y < img_height; ++y) {
        float* orig_row = original.ptr<float>(y);
        float* adj_row = image.ptr<float>(y);

        for (int x = 0; x < img_width; ++x) {
            int mask_x = std::clamp(static_cast<int>(static_cast<float>(x) * scale_x), 0, mask_width - 1);
            int mask_y = std::clamp(static_cast<int>(static_cast<float>(y) * scale_y), 0, mask_height - 1);

            float weight = mask[static_cast<size_t>(mask_y * mask_width + mask_x)];
            weight = std::clamp(weight, 0.0f, 1.0f);

            for (int c = 0; c < channels; ++c) {
                int idx = x * channels + c;
                adj_row[idx] = orig_row[idx] * (1.0f - weight) + adj_row[idx] * weight;
            }
        }
    }
}

} // namespace mask
} // namespace alcedo
