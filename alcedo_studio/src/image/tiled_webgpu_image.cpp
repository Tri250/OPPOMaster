//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#ifdef HAVE_WEBGPU

#include "image/tiled_webgpu_image.hpp"

#include <algorithm>
#include <limits>
#include <stdexcept>

#include "webgpu/webgpu_context.hpp"

namespace alcedo {
namespace webgpu {
namespace {

auto CeilDiv(uint32_t numerator, uint32_t denominator) -> uint32_t {
  return (numerator + denominator - 1) / denominator;
}

auto MakeHostRect(const TileRect& rect) -> cv::Rect {
  return cv::Rect(static_cast<int>(rect.x), static_cast<int>(rect.y),
                  static_cast<int>(rect.width), static_cast<int>(rect.height));
}

auto Intersect(const TileRect& lhs, const TileRect& rhs) -> TileRect {
  const uint32_t left   = std::max(lhs.x, rhs.x);
  const uint32_t top    = std::max(lhs.y, rhs.y);
  const uint32_t right  = std::min(lhs.x + lhs.width, rhs.x + rhs.width);
  const uint32_t bottom = std::min(lhs.y + lhs.height, rhs.y + rhs.height);
  if (right <= left || bottom <= top) {
    return {};
  }
  return {left, top, right - left, bottom - top};
}

void SubmitAndWait(const wgpu::CommandBuffer& command_buffer) {
  WebGpuContext::Instance().Queue().Submit(1, &command_buffer);
  WebGpuContext::Instance().WaitForSubmittedWork();
}

}  // namespace

void TiledWebGpuImage::Create(uint32_t width, uint32_t height, PixelFormat format,
                              uint32_t tile_edge, bool texture_binding, bool storage_binding) {
  if (width == 0 || height == 0) {
    throw std::invalid_argument("TiledWebGpuImage: logical image dimensions must be non-zero.");
  }

  const uint32_t resolved_tile_edge = ResolveTileEdge(tile_edge);
  if (resolved_tile_edge == 0) {
    throw std::runtime_error("TiledWebGpuImage: no usable WebGPU tile edge is available.");
  }

  width_           = width;
  height_          = height;
  format_          = format;
  tile_shape_      = {resolved_tile_edge, resolved_tile_edge};
  tile_columns_    = CeilDiv(width_, tile_shape_.width);
  tile_rows_       = CeilDiv(height_, tile_shape_.height);
  texture_binding_ = texture_binding;
  storage_binding_ = storage_binding;
  tiles_.assign(static_cast<size_t>(tile_columns_) * tile_rows_, WebGpuImage{});
}

void TiledWebGpuImage::AdoptSingleTile(WebGpuImage&& tile) {
  if (tile.Empty()) {
    throw std::invalid_argument("TiledWebGpuImage: cannot adopt an empty physical tile.");
  }

  width_           = tile.Width();
  height_          = tile.Height();
  format_          = tile.Format();
  tile_shape_      = {tile.Width(), tile.Height()};
  tile_columns_    = 1;
  tile_rows_       = 1;
  texture_binding_ = true;
  storage_binding_ = true;
  tiles_.clear();
  tiles_.push_back(std::move(tile));
}

void TiledWebGpuImage::Upload(const cv::Mat& host_image, uint32_t tile_edge) {
  if (host_image.empty()) {
    throw std::invalid_argument("TiledWebGpuImage: cannot upload an empty host image.");
  }

  Create(static_cast<uint32_t>(host_image.cols), static_cast<uint32_t>(host_image.rows),
         WebGpuImage::PixelFormatFromCVType(host_image.type()), tile_edge);

  for (uint32_t tile_y = 0; tile_y < tile_rows_; ++tile_y) {
    for (uint32_t tile_x = 0; tile_x < tile_columns_; ++tile_x) {
      const TileIndex index{tile_x, tile_y};
      const TileRect  rect = TileRegion(index);
      Tile(index).Upload(host_image(MakeHostRect(rect)).clone());
    }
  }
}

void TiledWebGpuImage::Download(cv::Mat& host_image) const {
  if (Empty()) {
    throw std::runtime_error("TiledWebGpuImage: cannot download from an empty image.");
  }

  host_image.create(static_cast<int>(height_), static_cast<int>(width_),
                    WebGpuImage::CVTypeFromPixelFormat(format_));

  for (uint32_t tile_y = 0; tile_y < tile_rows_; ++tile_y) {
    for (uint32_t tile_x = 0; tile_x < tile_columns_; ++tile_x) {
      const TileIndex index{tile_x, tile_y};
      const TileRect  rect = TileRegion(index);
      cv::Mat         tile_host;
      Tile(index).Download(tile_host);
      tile_host.copyTo(host_image(MakeHostRect(rect)));
    }
  }
}

void TiledWebGpuImage::CopyTo(TiledWebGpuImage& dst) const {
  if (Empty()) {
    throw std::runtime_error("TiledWebGpuImage: cannot copy an empty image.");
  }

  dst.Create(width_, height_, format_, tile_shape_.width, texture_binding_, storage_binding_);
  for (uint32_t tile_y = 0; tile_y < tile_rows_; ++tile_y) {
    for (uint32_t tile_x = 0; tile_x < tile_columns_; ++tile_x) {
      const TileIndex index{tile_x, tile_y};
      Tile(index).CopyTo(dst.Tile(index));
    }
  }
}

void TiledWebGpuImage::CopyRegionTo(WebGpuImage& dst, const TileRect& src_rect, uint32_t dst_x,
                                    uint32_t dst_y) const {
  auto encoder = WebGpuContext::Instance().Device().CreateCommandEncoder();
  EncodeCopyRegionTo(encoder, dst, src_rect, dst_x, dst_y);
  SubmitAndWait(encoder.Finish());
}

void TiledWebGpuImage::EncodeCopyRegionTo(wgpu::CommandEncoder& encoder, WebGpuImage& dst,
                                          const TileRect& src_rect, uint32_t dst_x,
                                          uint32_t dst_y) const {
  if (Empty()) {
    throw std::runtime_error("TiledWebGpuImage: cannot copy from an empty image.");
  }
  if (src_rect.width == 0 || src_rect.height == 0) {
    throw std::invalid_argument("TiledWebGpuImage: copy region dimensions must be non-zero.");
  }
  if (src_rect.x + src_rect.width > width_ || src_rect.y + src_rect.height > height_) {
    throw std::out_of_range("TiledWebGpuImage: copy region is out of bounds.");
  }
  if (dst.Empty()) {
    throw std::runtime_error("TiledWebGpuImage: destination texture is empty.");
  }
  if (dst.Format() != format_) {
    throw std::invalid_argument("TiledWebGpuImage: copy region requires matching pixel formats.");
  }
  if (dst_x + src_rect.width > dst.Width() || dst_y + src_rect.height > dst.Height()) {
    throw std::out_of_range("TiledWebGpuImage: destination copy region is out of bounds.");
  }

  for (uint32_t tile_y = 0; tile_y < tile_rows_; ++tile_y) {
    for (uint32_t tile_x = 0; tile_x < tile_columns_; ++tile_x) {
      const TileIndex src_index{tile_x, tile_y};
      const TileRect  tile_region = TileRegion(src_index);
      const TileRect  overlap     = Intersect(tile_region, src_rect);
      if (overlap.width == 0 || overlap.height == 0) {
        continue;
      }

      Tile(src_index)
          .EncodeCopyRegionTo(encoder, dst, overlap.x - tile_region.x, overlap.y - tile_region.y,
                              overlap.width, overlap.height, dst_x + overlap.x - src_rect.x,
                              dst_y + overlap.y - src_rect.y);
    }
  }
}

void TiledWebGpuImage::ConvertTo(TiledWebGpuImage& dst, PixelFormat dst_format, double alpha,
                                 double beta) const {
  if (Empty()) {
    throw std::runtime_error("TiledWebGpuImage: cannot convert an empty image.");
  }

  dst.Create(width_, height_, dst_format, tile_shape_.width, texture_binding_, storage_binding_);
  for (uint32_t tile_y = 0; tile_y < tile_rows_; ++tile_y) {
    for (uint32_t tile_x = 0; tile_x < tile_columns_; ++tile_x) {
      const TileIndex index{tile_x, tile_y};
      Tile(index).ConvertTo(dst.Tile(index), dst_format, alpha, beta);
    }
  }
}

void TiledWebGpuImage::Crop(const TileRect& rect) {
  if (Empty()) {
    throw std::runtime_error("TiledWebGpuImage: cannot crop an empty image.");
  }
  if (rect.width == 0 || rect.height == 0) {
    throw std::invalid_argument("TiledWebGpuImage: crop dimensions must be non-zero.");
  }
  if (rect.x + rect.width > width_ || rect.y + rect.height > height_) {
    throw std::out_of_range("TiledWebGpuImage: crop region is out of bounds.");
  }
  if (rect.x == 0 && rect.y == 0 && rect.width == width_ && rect.height == height_) {
    return;
  }

  TiledWebGpuImage cropped;
  cropped.Create(rect.width, rect.height, format_, tile_shape_.width, texture_binding_,
                 storage_binding_);

  auto encoder = WebGpuContext::Instance().Device().CreateCommandEncoder();
  for (uint32_t dst_tile_y = 0; dst_tile_y < cropped.TileRows(); ++dst_tile_y) {
    for (uint32_t dst_tile_x = 0; dst_tile_x < cropped.TileColumns(); ++dst_tile_x) {
      const TileIndex dst_index{dst_tile_x, dst_tile_y};
      const TileRect  dst_region = cropped.TileRegion(dst_index);
      const TileRect  src_region{rect.x + dst_region.x, rect.y + dst_region.y, dst_region.width,
                                dst_region.height};
      auto&           dst_tile = cropped.Tile(dst_index);

      for (uint32_t src_tile_y = 0; src_tile_y < tile_rows_; ++src_tile_y) {
        for (uint32_t src_tile_x = 0; src_tile_x < tile_columns_; ++src_tile_x) {
          const TileIndex src_index{src_tile_x, src_tile_y};
          const TileRect  src_tile_region = TileRegion(src_index);
          const TileRect  overlap         = Intersect(src_tile_region, src_region);
          if (overlap.width == 0 || overlap.height == 0) {
            continue;
          }

          Tile(src_index)
              .EncodeCopyRegionTo(encoder, dst_tile, overlap.x - src_tile_region.x,
                                  overlap.y - src_tile_region.y, overlap.width, overlap.height,
                                  overlap.x - src_region.x, overlap.y - src_region.y);
        }
      }
    }
  }
  SubmitAndWait(encoder.Finish());

  *this = std::move(cropped);
}

void TiledWebGpuImage::Release() noexcept {
  width_           = 0;
  height_          = 0;
  format_          = PixelFormat::RGBA32FLOAT;
  tile_shape_      = {};
  tile_columns_    = 0;
  tile_rows_       = 0;
  texture_binding_ = true;
  storage_binding_ = true;
  tiles_.clear();
}

auto TiledWebGpuImage::Empty() const noexcept -> bool {
  return width_ == 0 || height_ == 0 || tile_columns_ == 0 || tile_rows_ == 0 || tiles_.empty();
}

auto TiledWebGpuImage::IsTileResident(TileIndex index) const -> bool {
  return !tiles_[FlatIndex(index)].Empty();
}

auto TiledWebGpuImage::TileRegion(TileIndex index) const -> TileRect {
  (void)FlatIndex(index);

  const uint32_t x      = index.x * tile_shape_.width;
  const uint32_t y      = index.y * tile_shape_.height;
  const uint32_t width  = std::min(tile_shape_.width, width_ - x);
  const uint32_t height = std::min(tile_shape_.height, height_ - y);
  return {x, y, width, height};
}

auto TiledWebGpuImage::Tile(TileIndex index) -> WebGpuImage& {
  EnsureTileAllocated(index);
  return tiles_[FlatIndex(index)];
}

auto TiledWebGpuImage::Tile(TileIndex index) const -> const WebGpuImage& {
  const auto& tile = tiles_[FlatIndex(index)];
  if (tile.Empty()) {
    throw std::runtime_error("TiledWebGpuImage: requested tile is not resident.");
  }
  return tile;
}

auto TiledWebGpuImage::HasSingleTile() const noexcept -> bool {
  return tile_columns_ == 1 && tile_rows_ == 1 && tiles_.size() == 1 && !tiles_.front().Empty();
}

auto TiledWebGpuImage::SingleTile() -> WebGpuImage& {
  if (!HasSingleTile()) {
    throw std::runtime_error(
        "TiledWebGpuImage: legacy single-tile access requires exactly one resident tile.");
  }
  return tiles_.front();
}

auto TiledWebGpuImage::SingleTile() const -> const WebGpuImage& {
  if (!HasSingleTile()) {
    throw std::runtime_error(
        "TiledWebGpuImage: legacy single-tile access requires exactly one resident tile.");
  }
  return tiles_.front();
}

auto TiledWebGpuImage::FlatIndex(TileIndex index) const -> size_t {
  if (index.x >= tile_columns_ || index.y >= tile_rows_) {
    throw std::out_of_range("TiledWebGpuImage: tile index is out of bounds.");
  }
  return static_cast<size_t>(index.y) * tile_columns_ + index.x;
}

auto TiledWebGpuImage::ResolveTileEdge(uint32_t requested_tile_edge) const -> uint32_t {
  const auto& context = WebGpuContext::Instance();
  if (!context.IsAvailable()) {
    throw std::runtime_error("TiledWebGpuImage: WebGPU backend is unavailable.");
  }

  const uint32_t max_edge = context.Limits().max_texture_dimension_2d;
  if (max_edge == 0) {
    return 0;
  }

  if (requested_tile_edge == 0) {
    requested_tile_edge = context.RecommendedTileEdge();
  }
  if (requested_tile_edge == 0) {
    requested_tile_edge = max_edge;
  }
  return std::clamp(requested_tile_edge, 1u, max_edge);
}

void TiledWebGpuImage::EnsureTileAllocated(TileIndex index) {
  auto& tile = tiles_[FlatIndex(index)];
  if (!tile.Empty()) {
    return;
  }

  const TileRect rect = TileRegion(index);
  tile.Create(rect.width, rect.height, format_, texture_binding_, storage_binding_);
}

}  // namespace webgpu
}  // namespace alcedo

#endif
