//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "processing/collage_maker.hpp"

#include <algorithm>
#include <cctype>
#include <cmath>
#include <fstream>
#include <mutex>

#include <opencv2/imgcodecs.hpp>
#include <opencv2/imgproc.hpp>

namespace alcedo {

// ============================================================================
// CollageMaker Implementation
// ============================================================================

class CollageMaker::Impl {
public:
    CollageConfig config;
    std::vector<cv::Mat> loaded_images;
    std::vector<CollageFrame> frames;
    bool initialized = false;
};

CollageMaker::CollageMaker() : impl_(std::make_unique<Impl>()) {}

CollageMaker::~CollageMaker() = default;

auto CollageMaker::Create(const CollageConfig& config) -> bool {
    impl_->config = config;

    // Calculate frames based on layout
    if (config.layout == CollageLayout::Custom) {
        impl_->frames = config.custom_frames;
    } else {
        impl_->frames = CalculateLayoutFrames(
            config.layout, config.canvas_width, config.canvas_height,
            config.spacing, config.margin);
    }

    // Apply default frame properties
    for (auto& frame : impl_->frames) {
        frame.border_radius = config.border_radius;
        frame.border_width = config.border_width;
        std::memcpy(frame.border_color, config.border_color, 4);
    }

    impl_->loaded_images.clear();
    impl_->loaded_images.resize(impl_->frames.size());
    impl_->initialized = true;

    return true;
}

auto CollageMaker::AddImage(const std::string& image_path, int frame_index) -> bool {
    if (!impl_->initialized) return false;

    // Find an empty frame if frame_index is -1
    if (frame_index < 0) {
        for (size_t i = 0; i < impl_->loaded_images.size(); ++i) {
            if (impl_->loaded_images[i].empty()) {
                frame_index = static_cast<int>(i);
                break;
            }
        }
    }

    if (frame_index < 0 || static_cast<size_t>(frame_index) >= impl_->frames.size()) {
        return false;
    }

    impl_->frames[frame_index].image_path = image_path;

    // Load the image file into the buffer
    try {
        cv::Mat img = cv::imread(image_path, cv::IMREAD_UNCHANGED);
        if (!img.empty()) {
            // Convert to BGRA for consistent alpha handling
            if (img.channels() == 3) {
                cv::cvtColor(img, img, cv::COLOR_BGR2BGRA);
            } else if (img.channels() == 1) {
                cv::cvtColor(img, img, cv::COLOR_GRAY2BGRA);
            }
            impl_->loaded_images[frame_index] = img;
        }
    } catch (...) {
        // Image loading failed - path will remain but no buffer
    }

    return true;
}

auto CollageMaker::RemoveImage(int frame_index) -> bool {
    if (!impl_->initialized || frame_index < 0 ||
        static_cast<size_t>(frame_index) >= impl_->frames.size()) {
        return false;
    }

    impl_->frames[frame_index].image_path.clear();
    impl_->loaded_images[frame_index] = cv::Mat();

    return true;
}

auto CollageMaker::SetFrameProperties(int frame_index, const CollageFrame& frame) -> bool {
    if (!impl_->initialized || frame_index < 0 ||
        static_cast<size_t>(frame_index) >= impl_->frames.size()) {
        return false;
    }

    impl_->frames[frame_index] = frame;
    return true;
}

auto CollageMaker::AdjustImage(int frame_index, int offset_x, int offset_y, float scale) -> bool {
    if (!impl_->initialized || frame_index < 0 ||
        static_cast<size_t>(frame_index) >= impl_->frames.size()) {
        return false;
    }

    impl_->frames[frame_index].image_x = offset_x;
    impl_->frames[frame_index].image_y = offset_y;
    impl_->frames[frame_index].image_scale = scale;

    return true;
}

auto CollageMaker::RotateImage(int frame_index, float degrees) -> bool {
    if (!impl_->initialized || frame_index < 0 ||
        static_cast<size_t>(frame_index) >= impl_->frames.size()) {
        return false;
    }

    impl_->frames[frame_index].rotation = degrees;
    return true;
}

auto CollageMaker::GeneratePreview(int max_width) -> std::shared_ptr<ImageBuffer> {
    if (!impl_->initialized) return nullptr;

    // Calculate preview scale
    const float scale = static_cast<float>(max_width) / static_cast<float>(impl_->config.canvas_width);

    const int pw = static_cast<int>(impl_->config.canvas_width * scale);
    const int ph = static_cast<int>(impl_->config.canvas_height * scale);

    // Create BGRA canvas with background color
    cv::Mat canvas(ph, pw, CV_8UC4, cv::Scalar(
        impl_->config.background_color[0],
        impl_->config.background_color[1],
        impl_->config.background_color[2],
        impl_->config.background_color[3]
    ));

    // Render frames with actual images
    for (size_t fi = 0; fi < impl_->frames.size(); ++fi) {
        const auto& frame = impl_->frames[fi];
        const int scaled_x = static_cast<int>(frame.x * scale);
        const int scaled_y = static_cast<int>(frame.y * scale);
        const int scaled_width = static_cast<int>(frame.width * scale);
        const int scaled_height = static_cast<int>(frame.height * scale);

        if (frame.image_path.empty()) {
            // Empty frame placeholder - draw gray rectangle
            cv::Rect frame_rect(scaled_x, scaled_y, scaled_width, scaled_height);
            cv::rectangle(canvas, frame_rect, cv::Scalar(100, 100, 100, 255), cv::FILLED);
            continue;
        }

        const auto& src = impl_->loaded_images[fi];
        if (src.empty()) continue;

        // Resize source image to cover-fit the scaled frame
        const int src_w = src.cols;
        const int src_h = src.rows;
        if (src_w <= 0 || src_h <= 0) continue;

        const float frame_aspect = static_cast<float>(scaled_width) / static_cast<float>(scaled_height);
        const float img_aspect = static_cast<float>(src_w) / static_cast<float>(src_h);

        int draw_w, draw_h;
        if (img_aspect > frame_aspect) {
            draw_h = scaled_height;
            draw_w = static_cast<int>(draw_h * img_aspect);
        } else {
            draw_w = scaled_width;
            draw_h = static_cast<int>(draw_w / img_aspect);
        }

        cv::Mat resized;
        cv::resize(src, resized, cv::Size(draw_w, draw_h), 0, 0, cv::INTER_LINEAR);

        // Ensure BGRA
        cv::Mat resized_bgra;
        if (resized.channels() == 3) {
            cv::cvtColor(resized, resized_bgra, cv::COLOR_BGR2BGRA);
        } else if (resized.channels() == 4) {
            resized_bgra = resized;
        } else if (resized.channels() == 1) {
            cv::cvtColor(resized, resized_bgra, cv::COLOR_GRAY2BGRA);
        } else {
            continue;
        }

        // Center-crop into frame
        const int offset_x = (scaled_width - draw_w) / 2;
        const int offset_y = (scaled_height - draw_h) / 2;

        const int src_start_x = std::max(0, -offset_x);
        const int src_start_y = std::max(0, -offset_y);
        const int dst_start_x = scaled_x + std::max(0, offset_x);
        const int dst_start_y = scaled_y + std::max(0, offset_y);
        const int copy_w = std::min(draw_w - src_start_x, std::min(scaled_width - std::max(0, offset_x), pw - dst_start_x));
        const int copy_h = std::min(draw_h - src_start_y, std::min(scaled_height - std::max(0, offset_y), ph - dst_start_y));

        if (copy_w <= 0 || copy_h <= 0 || dst_start_x < 0 || dst_start_y < 0) continue;

        cv::Rect src_roi(src_start_x, src_start_y, copy_w, copy_h);
        cv::Rect dst_roi(dst_start_x, dst_start_y, copy_w, copy_h);
        resized_bgra(src_roi).copyTo(canvas(dst_roi));

        // Draw frame border
        if (frame.border_width > 0) {
            cv::Rect border_rect(scaled_x, scaled_y, scaled_width, scaled_height);
            cv::Scalar border_color(
                frame.border_color[0], frame.border_color[1],
                frame.border_color[2], frame.border_color[3]
            );
            cv::rectangle(canvas, border_rect, border_color,
                          std::max(1, static_cast<int>(frame.border_width * scale)));
        }
    }

    auto preview = std::make_shared<ImageBuffer>(std::move(canvas));
    return preview;
}

auto CollageMaker::Render() -> std::shared_ptr<ImageBuffer> {
    if (!impl_->initialized) return nullptr;

    const int w = impl_->config.canvas_width;
    const int h = impl_->config.canvas_height;

    // Create BGRA canvas with background color
    cv::Mat canvas(h, w, CV_8UC4, cv::Scalar(
        impl_->config.background_color[0],
        impl_->config.background_color[1],
        impl_->config.background_color[2],
        impl_->config.background_color[3]
    ));

    // Render each frame with its image
    for (size_t fi = 0; fi < impl_->frames.size(); ++fi) {
        const auto& frame = impl_->frames[fi];
        if (frame.image_path.empty()) continue;

        const auto& src = impl_->loaded_images[fi];
        if (src.empty()) continue;

        // Get source image dimensions
        const int src_w = src.cols;
        const int src_h = src.rows;
        if (src_w <= 0 || src_h <= 0) continue;

        // Calculate cover-fit scaling
        const float frame_aspect = static_cast<float>(frame.width) / static_cast<float>(frame.height);
        const float img_aspect = static_cast<float>(src_w) / static_cast<float>(src_h);

        int draw_w, draw_h;
        if (img_aspect > frame_aspect) {
            draw_h = frame.height;
            draw_w = static_cast<int>(draw_h * img_aspect);
        } else {
            draw_w = frame.width;
            draw_h = static_cast<int>(draw_w / img_aspect);
        }

        // Resize source image to draw size
        cv::Mat resized;
        cv::resize(src, resized, cv::Size(draw_w, draw_h), 0, 0, cv::INTER_LINEAR);

        // Ensure BGRA
        cv::Mat resized_bgra;
        if (resized.channels() == 3) {
            cv::cvtColor(resized, resized_bgra, cv::COLOR_BGR2BGRA);
        } else if (resized.channels() == 4) {
            resized_bgra = resized;
        } else if (resized.channels() == 1) {
            cv::cvtColor(resized, resized_bgra, cv::COLOR_GRAY2BGRA);
        } else {
            continue;
        }

        // Calculate offset for center-crop within frame
        const int offset_x = (frame.width - draw_w) / 2;
        const int offset_y = (frame.height - draw_h) / 2;

        // Determine source and destination regions for the cover-fit crop
        const int src_start_x = std::max(0, -offset_x);
        const int src_start_y = std::max(0, -offset_y);
        const int dst_start_x = frame.x + std::max(0, offset_x);
        const int dst_start_y = frame.y + std::max(0, offset_y);
        const int copy_w = std::min(draw_w - src_start_x, std::min(frame.width - std::max(0, offset_x), w - dst_start_x));
        const int copy_h = std::min(draw_h - src_start_y, std::min(frame.height - std::max(0, offset_y), h - dst_start_y));

        if (copy_w <= 0 || copy_h <= 0 || dst_start_x < 0 || dst_start_y < 0) continue;

        cv::Rect src_roi(src_start_x, src_start_y, copy_w, copy_h);
        cv::Rect dst_roi(dst_start_x, dst_start_y, copy_w, copy_h);

        // Apply opacity blending
        const float alpha = frame.opacity;
        if (alpha >= 1.0f) {
            resized_bgra(src_roi).copyTo(canvas(dst_roi));
        } else {
            cv::Mat src_region = resized_bgra(src_roi);
            cv::Mat dst_region = canvas(dst_roi);
            const float inv_a = 1.0f - alpha;
            for (int y = 0; y < copy_h; ++y) {
                for (int x = 0; x < copy_w; ++x) {
                    dst_region.at<cv::Vec4b>(y, x)[0] = static_cast<uint8_t>(src_region.at<cv::Vec4b>(y, x)[0] * alpha + dst_region.at<cv::Vec4b>(y, x)[0] * inv_a);
                    dst_region.at<cv::Vec4b>(y, x)[1] = static_cast<uint8_t>(src_region.at<cv::Vec4b>(y, x)[1] * alpha + dst_region.at<cv::Vec4b>(y, x)[1] * inv_a);
                    dst_region.at<cv::Vec4b>(y, x)[2] = static_cast<uint8_t>(src_region.at<cv::Vec4b>(y, x)[2] * alpha + dst_region.at<cv::Vec4b>(y, x)[2] * inv_a);
                    dst_region.at<cv::Vec4b>(y, x)[3] = 255;
                }
            }
        }

        // Draw border
        if (frame.border_width > 0) {
            cv::Rect border_rect(frame.x, frame.y, frame.width, frame.height);
            cv::Scalar border_color(
                frame.border_color[0], frame.border_color[1],
                frame.border_color[2], frame.border_color[3]
            );
            cv::rectangle(canvas, border_rect, border_color, frame.border_width);
        }
    }

    // Create ImageBuffer from cv::Mat
    auto result = std::make_shared<ImageBuffer>(std::move(canvas));
    return result;
}

auto CollageMaker::Export(const std::string& path, const std::string& format, int quality) -> bool {
    auto rendered = Render();
    if (!rendered) return false;

    // Write the rendered image data to the specified file
    try {
        cv::Mat& canvas = rendered->GetCPUData();
        if (canvas.empty()) return false;

        // Convert BGRA to BGR for standard image formats
        cv::Mat output;
        if (canvas.channels() == 4) {
            cv::cvtColor(canvas, output, cv::COLOR_BGRA2BGR);
        } else {
            output = canvas;
        }

        // Determine OpenCV format flag from the format string
        std::vector<int> params;
        std::string fmt_upper = format;
        std::transform(fmt_upper.begin(), fmt_upper.end(), fmt_upper.begin(),
                       [](unsigned char c) { return static_cast<char>(std::toupper(c)); });

        if (fmt_upper == "JPEG" || fmt_upper == "JPG") {
            params.push_back(cv::IMWRITE_JPEG_QUALITY);
            params.push_back(std::clamp(quality, 1, 100));
        } else if (fmt_upper == "PNG") {
            params.push_back(cv::IMWRITE_PNG_COMPRESSION);
            params.push_back(3);
        } else if (fmt_upper == "WEBP") {
            params.push_back(cv::IMWRITE_WEBP_QUALITY);
            params.push_back(std::clamp(quality, 1, 100));
        }
        // PPM format handled manually for cross-platform compatibility
        else if (fmt_upper == "PPM") {
            std::ofstream out(path, std::ios::binary);
            if (!out) return false;
            cv::Mat rgb;
            if (output.channels() == 4) {
                cv::cvtColor(output, rgb, cv::COLOR_BGRA2BGR);
            } else {
                rgb = output;
            }
            out << "P6\n" << rgb.cols << " " << rgb.rows << "\n255\n";
            for (int y = 0; y < rgb.rows; ++y) {
                out.write(reinterpret_cast<const char*>(rgb.ptr(y)),
                          static_cast<std::streamsize>(rgb.cols * rgb.channels()));
            }
            return out.good();
        }

        return cv::imwrite(path, output, params);
    } catch (...) {
        return false;
    }
}

auto CollageMaker::GetFrameCount() const -> int {
    return static_cast<int>(impl_->frames.size());
}

auto CollageMaker::GetAvailableLayouts() -> std::vector<CollageLayout> {
    return {
        CollageLayout::Grid2x2,
        CollageLayout::Grid3x3,
        CollageLayout::Grid4x4,
        CollageLayout::Magazine,
        CollageLayout::Polaroid,
        CollageLayout::StripHorizontal,
        CollageLayout::StripVertical,
        CollageLayout::Custom
    };
}

auto CollageMaker::GetLayoutName(CollageLayout layout) -> std::string {
    switch (layout) {
        case CollageLayout::Grid2x2: return "Grid 2×2";
        case CollageLayout::Grid3x3: return "Grid 3×3";
        case CollageLayout::Grid4x4: return "Grid 4×4";
        case CollageLayout::Magazine: return "Magazine Style";
        case CollageLayout::Polaroid: return "Polaroid";
        case CollageLayout::StripHorizontal: return "Horizontal Strip";
        case CollageLayout::StripVertical: return "Vertical Strip";
        case CollageLayout::Custom: return "Custom";
    }
    return "Unknown";
}

auto CollageMaker::CalculateLayoutFrames(
    CollageLayout layout,
    int canvas_width,
    int canvas_height,
    int spacing,
    int margin
) -> std::vector<CollageFrame> {
    std::vector<CollageFrame> frames;

    const int usable_width = canvas_width - 2 * margin;
    const int usable_height = canvas_height - 2 * margin;

    switch (layout) {
        case CollageLayout::Grid2x2: {
            const int cell_width = (usable_width - spacing) / 2;
            const int cell_height = (usable_height - spacing) / 2;

            frames.push_back({margin, margin, cell_width, cell_height});
            frames.push_back({margin + cell_width + spacing, margin, cell_width, cell_height});
            frames.push_back({margin, margin + cell_height + spacing, cell_width, cell_height});
            frames.push_back({margin + cell_width + spacing, margin + cell_height + spacing, cell_width, cell_height});
            break;
        }

        case CollageLayout::Grid3x3: {
            const int cell_width = (usable_width - 2 * spacing) / 3;
            const int cell_height = (usable_height - 2 * spacing) / 3;

            for (int row = 0; row < 3; ++row) {
                for (int col = 0; col < 3; ++col) {
                    frames.push_back({
                        margin + col * (cell_width + spacing),
                        margin + row * (cell_height + spacing),
                        cell_width,
                        cell_height
                    });
                }
            }
            break;
        }

        case CollageLayout::Grid4x4: {
            const int cell_width = (usable_width - 3 * spacing) / 4;
            const int cell_height = (usable_height - 3 * spacing) / 4;

            for (int row = 0; row < 4; ++row) {
                for (int col = 0; col < 4; ++col) {
                    frames.push_back({
                        margin + col * (cell_width + spacing),
                        margin + row * (cell_height + spacing),
                        cell_width,
                        cell_height
                    });
                }
            }
            break;
        }

        case CollageLayout::StripHorizontal: {
            const int num_frames = 4;
            const int cell_height = usable_height;
            const int cell_width = (usable_width - (num_frames - 1) * spacing) / num_frames;

            for (int i = 0; i < num_frames; ++i) {
                frames.push_back({
                    margin + i * (cell_width + spacing),
                    margin,
                    cell_width,
                    cell_height
                });
            }
            break;
        }

        case CollageLayout::StripVertical: {
            const int num_frames = 4;
            const int cell_width = usable_width;
            const int cell_height = (usable_height - (num_frames - 1) * spacing) / num_frames;

            for (int i = 0; i < num_frames; ++i) {
                frames.push_back({
                    margin,
                    margin + i * (cell_height + spacing),
                    cell_width,
                    cell_height
                });
            }
            break;
        }

        case CollageLayout::Magazine: {
            // Asymmetric layout
            const int main_width = usable_width * 2 / 3 - spacing / 2;
            const int side_width = usable_width / 3 - spacing / 2;

            // Main large image (left)
            frames.push_back({margin, margin, main_width, usable_height});

            // Side images (right)
            const int side_height = (usable_height - 2 * spacing) / 3;
            frames.push_back({margin + main_width + spacing, margin, side_width, side_height * 2 + spacing});
            frames.push_back({margin + main_width + spacing, margin + side_height * 2 + 2 * spacing, side_width, side_height});
            break;
        }

        case CollageLayout::Polaroid: {
            // Scattered polaroid-style frames
            const int polaroid_width = usable_width / 3;
            const int polaroid_height = usable_height / 3;

            frames.push_back({margin + 10, margin + 20, polaroid_width, polaroid_height, 5, 4});
            frames.push_back({margin + polaroid_width + 30, margin + 50, polaroid_width, polaroid_height, 5, 4});
            frames.push_back({margin + usable_width - polaroid_width - 10, margin + 30, polaroid_width, polaroid_height, 5, 4});
            frames.push_back({margin + 40, margin + usable_height - polaroid_height - 10, polaroid_width, polaroid_height, 5, 4});

            // Add random rotations for polaroid effect
            frames[0].rotation = -3.0f;
            frames[1].rotation = 5.0f;
            frames[2].rotation = -2.0f;
            frames[3].rotation = 4.0f;
            break;
        }

        case CollageLayout::Custom:
            // Custom frames are provided by user
            break;
    }

    return frames;
}

// ============================================================================
// ROIRenderManager Implementation
// ============================================================================

class ROIRenderManager::Impl {
public:
    ROIConfig config;
    bool enabled = false;
};

ROIRenderManager::ROIRenderManager() : impl_(std::make_unique<Impl>()) {}

ROIRenderManager::~ROIRenderManager() = default;

void ROIRenderManager::SetROI(const ROIConfig& config) {
    impl_->config = config;
}

auto ROIRenderManager::GetROI() const -> const ROIConfig& {
    return impl_->config;
}

void ROIRenderManager::SetEnabled(bool enabled) {
    impl_->enabled = enabled;
    impl_->config.enabled = enabled;
}

auto ROIRenderManager::IsEnabled() const -> bool {
    return impl_->enabled && impl_->config.IsValid();
}

void ROIRenderManager::UpdateFromViewport(
    int viewport_x,
    int viewport_y,
    int viewport_width,
    int viewport_height,
    float zoom_level
) {
    impl_->config.enabled = true;
    impl_->config.x = viewport_x;
    impl_->config.y = viewport_y;
    impl_->config.width = viewport_width;
    impl_->config.height = viewport_height;
    impl_->config.scale_x = zoom_level;
    impl_->config.scale_y = zoom_level;
}

auto ROIRenderManager::IsPointInROI(int x, int y) const -> bool {
    if (!impl_->config.enabled) return true;

    return x >= impl_->config.x &&
           x < impl_->config.x + impl_->config.width &&
           y >= impl_->config.y &&
           y < impl_->config.y + impl_->config.height;
}

auto ROIRenderManager::IsRegionInROI(int x, int y, int width, int height) const -> bool {
    if (!impl_->config.enabled) return true;

    // Check if region overlaps with ROI
    return (x + width > impl_->config.x &&
            x < impl_->config.x + impl_->config.width &&
            y + height > impl_->config.y &&
            y < impl_->config.y + impl_->config.height);
}

auto ROIRenderManager::CalculateTileBounds(
    int full_width,
    int full_height,
    int tile_size
) const -> std::vector<std::tuple<int, int, int, int>> {
    std::vector<std::tuple<int, int, int, int>> tiles;

    if (!impl_->config.enabled) {
        // Return all tiles for full image
        for (int y = 0; y < full_height; y += tile_size) {
            for (int x = 0; x < full_width; x += tile_size) {
                const int w = std::min(tile_size, full_width - x);
                const int h = std::min(tile_size, full_height - y);
                tiles.push_back({x, y, w, h});
            }
        }
    } else {
        // Return only tiles within ROI
        const int roi_x = impl_->config.x;
        const int roi_y = impl_->config.y;
        const int roi_w = impl_->config.width;
        const int roi_h = impl_->config.height;

        for (int y = roi_y; y < roi_y + roi_h; y += tile_size) {
            for (int x = roi_x; x < roi_x + roi_w; x += tile_size) {
                if (x < full_width && y < full_height) {
                    const int w = std::min(tile_size, std::min(full_width - x, roi_x + roi_w - x));
                    const int h = std::min(tile_size, std::min(full_height - y, roi_y + roi_h - y));
                    tiles.push_back({x, y, w, h});
                }
            }
        }
    }

    return tiles;
}

auto ROIRenderManager::GetEffectiveRenderDimensions(int full_width, int full_height) const
    -> std::pair<int, int> {
    if (!impl_->config.enabled) {
        return {full_width, full_height};
    }
    return {impl_->config.width, impl_->config.height};
}

auto ROIRenderManager::GetRenderScale(int full_width, int full_height) const
    -> std::pair<float, float> {
    if (!impl_->config.enabled) {
        return {1.0f, 1.0f};
    }
    return impl_->config.CalculateRenderScale(full_width, full_height);
}

// ============================================================================
// BatchDenoiseProcessor Implementation
// ============================================================================

auto BatchDenoiseProcessor::DenoiseImage(
    std::shared_ptr<ImageBuffer> input,
    const BatchDenoiseConfig& config
) -> std::shared_ptr<ImageBuffer> {
    if (!input) return nullptr;

    auto output = std::make_shared<ImageBuffer>(*input);

    const int width = input->width;
    const int height = input->height;

    // Convert to YCbCr for better denoising
    std::vector<float> y(static_cast<size_t>(width) * height);
    std::vector<float> cb(static_cast<size_t>(width) * height);
    std::vector<float> cr(static_cast<size_t>(width) * height);

    for (int i = 0; i < width * height; ++i) {
        const float r = input->data[i * 3] / 255.0f;
        const float g = input->data[i * 3 + 1] / 255.0f;
        const float b = input->data[i * 3 + 2] / 255.0f;

        y[i] = 0.299f * r + 0.587f * g + 0.114f * b;
        cb[i] = (b - y[i]) * 0.5f + 0.5f;
        cr[i] = (r - y[i]) * 0.5f + 0.5f;
    }

    std::vector<float> y_out, cb_out, cr_out;

    DenoiseYCbCr(y, cb, cr, y_out, cb_out, cr_out, width, height, config);

    // Convert back to RGB
    for (int i = 0; i < width * height; ++i) {
        const float y_val = y_out[i];
        const float cb_val = cb_out[i] - 0.5f;
        const float cr_val = cr_out[i] - 0.5f;

        float r = y_val + 1.402f * cr_val;
        float g = y_val - 0.344136f * cb_val - 0.714136f * cr_val;
        float b = y_val + 1.772f * cb_val;

        r = std::clamp(r * 255.0f, 0.0f, 255.0f);
        g = std::clamp(g * 255.0f, 0.0f, 255.0f);
        b = std::clamp(b * 255.0f, 0.0f, 255.0f);

        output->data[i * 3] = static_cast<uint8_t>(r);
        output->data[i * 3 + 1] = static_cast<uint8_t>(g);
        output->data[i * 3 + 2] = static_cast<uint8_t>(b);
    }

    return output;
}

auto BatchDenoiseProcessor::DenoiseYCbCr(
    const std::vector<float>& y_in,
    const std::vector<float>& cb_in,
    const std::vector<float>& cr_in,
    std::vector<float>& y_out,
    std::vector<float>& cb_out,
    std::vector<float>& cr_out,
    int width,
    int height,
    const BatchDenoiseConfig& config
) -> void {
    // Simple bilateral filter implementation
    // Full BM3D implementation would be in denoiser.cpp

    y_out.resize(y_in.size());
    cb_out.resize(cb_in.size());
    cr_out.resize(cr_in.size());

    const float sigma_s = 4.0f * config.strength;  // Spatial sigma
    const float sigma_r = 0.1f * config.strength;  // Range sigma

    // Apply bilateral filter to luminance
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            float sum = 0.0f;
            float weight_sum = 0.0f;

            const int radius = static_cast<int>(sigma_s * 2);

            const float center_val = y_in[y * width + x];

            for (int dy = -radius; dy <= radius; ++dy) {
                for (int dx = -radius; dx <= radius; ++dx) {
                    const int nx = std::clamp(x + dx, 0, width - 1);
                    const int ny = std::clamp(y + dy, 0, height - 1);

                    const float neighbor_val = y_in[ny * width + nx];

                    const float spatial_dist = std::sqrt(static_cast<float>(dx * dx + dy * dy));
                    const float range_dist = std::abs(center_val - neighbor_val);

                    const float spatial_weight = std::exp(-spatial_dist * spatial_dist / (2 * sigma_s * sigma_s));
                    const float range_weight = std::exp(-range_dist * range_dist / (2 * sigma_r * sigma_r));

                    const float weight = spatial_weight * range_weight;

                    sum += neighbor_val * weight;
                    weight_sum += weight;
                }
            }

            y_out[y * width + x] = sum / weight_sum;
        }
    }

    // Apply stronger denoising to chroma if separate_chroma is true
    const float chroma_sigma = config.chroma_strength * 0.1f;
    const int chroma_radius = static_cast<int>(chroma_sigma * 4 * 2);

    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            float cb_sum = 0.0f;
            float cr_sum = 0.0f;
            float cb_weight_sum = 0.0f;
            float cr_weight_sum = 0.0f;

            const float cb_center = cb_in[y * width + x];
            const float cr_center = cr_in[y * width + x];

            for (int dy = -chroma_radius; dy <= chroma_radius; ++dy) {
                for (int dx = -chroma_radius; dx <= chroma_radius; ++dx) {
                    const int nx = std::clamp(x + dx, 0, width - 1);
                    const int ny = std::clamp(y + dy, 0, height - 1);

                    const float cb_neighbor = cb_in[ny * width + nx];
                    const float cr_neighbor = cr_in[ny * width + nx];

                    const float spatial_dist = std::sqrt(static_cast<float>(dx * dx + dy * dy));
                    const float cb_range_dist = std::abs(cb_center - cb_neighbor);
                    const float cr_range_dist = std::abs(cr_center - cr_neighbor);

                    const float spatial_weight = std::exp(-spatial_dist * spatial_dist / (2 * chroma_sigma * chroma_sigma * 16));
                    const float cb_range_weight = std::exp(-cb_range_dist * cb_range_dist / (2 * chroma_sigma * chroma_sigma));
                    const float cr_range_weight = std::exp(-cr_range_dist * cr_range_dist / (2 * chroma_sigma * chroma_sigma));

                    const float cb_weight = spatial_weight * cb_range_weight;
                    const float cr_weight = spatial_weight * cr_range_weight;

                    cb_sum += cb_neighbor * cb_weight;
                    cb_weight_sum += cb_weight;
                    cr_sum += cr_neighbor * cr_weight;
                    cr_weight_sum += cr_weight;
                }
            }

            cb_out[y * width + x] = cb_sum / cb_weight_sum;
            cr_out[y * width + x] = cr_sum / cr_weight_sum;
        }
    }
}

auto BatchDenoiseProcessor::GetRecommendedStrength(int iso) -> float {
    // ISO-based strength estimation
    if (iso <= 100) return 0.02f;
    if (iso <= 200) return 0.04f;
    if (iso <= 400) return 0.08f;
    if (iso <= 800) return 0.15f;
    if (iso <= 1600) return 0.25f;
    if (iso <= 3200) return 0.4f;
    if (iso <= 6400) return 0.6f;
    return 0.8f;
}

auto BatchDenoiseProcessor::EstimateProcessingTime(
    int width,
    int height,
    const BatchDenoiseConfig& config
) -> float {
    // Rough estimation in seconds
    const float pixel_count = static_cast<float>(width * height);
    float time_per_pixel = 0.00001f;  // Base time

    switch (config.method) {
        case BatchDenoiseConfig::Method::BM3D:
            time_per_pixel = 0.00005f;
            break;
        case BatchDenoiseConfig::Method::Wavelet:
            time_per_pixel = 0.00003f;
            break;
        case BatchDenoiseConfig::Method::Bilateral:
            time_per_pixel = 0.00001f;
            break;
        case BatchDenoiseConfig::Method::NLM:
            time_per_pixel = 0.00008f;
            break;
    }

    return pixel_count * time_per_pixel * config.strength;
}

}  // namespace alcedo