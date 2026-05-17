//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.
#pragma once

#ifdef HAVE_WEBGPU

#include <cstddef>
#include <cstdint>
#include <opencv2/core/mat.hpp>
#include <vector>

#include "image/webgpu_image.hpp"

namespace alcedo {
namespace webgpu {

struct TileIndex {
  uint32_t x = 0;
  uint32_t y = 0;

  auto operator==(const TileIndex& other) const -> bool = default;
};

struct TileExtent {
  uint32_t width  = 0;
  uint32_t height = 0;

  auto operator==(const TileExtent& other) const -> bool = default;
};

struct TileRect {
  uint32_t x      = 0;
  uint32_t y      = 0;
  uint32_t width  = 0;
  uint32_t height = 0;

  auto operator==(const TileRect& other) const -> bool = default;
};

// Represents one logical image backed by a regular grid of physical WebGPU textures.
// WebGpuImage intentionally remains the single-texture primitive.
class TiledWebGpuImage {
 public:
  TiledWebGpuImage()                                                      = default;
  ~TiledWebGpuImage()                                                     = default;
  TiledWebGpuImage(const TiledWebGpuImage&)                               = default;
  auto operator=(const TiledWebGpuImage&) -> TiledWebGpuImage&            = default;
  TiledWebGpuImage(TiledWebGpuImage&&) noexcept                           = default;
  auto        operator=(TiledWebGpuImage&&) noexcept -> TiledWebGpuImage& = default;

  void Create(uint32_t width, uint32_t height, PixelFormat format, uint32_t tile_edge = 0,
              bool texture_binding = true, bool storage_binding = true);
  void AdoptSingleTile(WebGpuImage&& tile);
  void SetFormat(PixelFormat format) noexcept { format_ = format; }
  void Upload(const cv::Mat& host_image, uint32_t tile_edge = 0);
  void Download(cv::Mat& host_image) const;
  void CopyTo(TiledWebGpuImage& dst) const;
  void EncodeCopyRegionTo(wgpu::CommandEncoder& encoder, WebGpuImage& dst,
                          const TileRect& src_rect, uint32_t dst_x = 0,
                          uint32_t dst_y = 0) const;
  void CopyRegionTo(WebGpuImage& dst, const TileRect& src_rect, uint32_t dst_x = 0,
                    uint32_t dst_y = 0) const;
  void ConvertTo(TiledWebGpuImage& dst, PixelFormat dst_format, double alpha = 1.0,
                 double beta = 0.0) const;
  void Crop(const TileRect& rect);
  void Release() noexcept;

  [[nodiscard]] auto Empty() const noexcept -> bool;
  [[nodiscard]] auto IsValid() const noexcept -> bool { return !Empty(); }
  explicit           operator bool() const noexcept { return IsValid(); }

  [[nodiscard]] auto Width() const noexcept { return width_; }
  [[nodiscard]] auto Height() const noexcept { return height_; }
  [[nodiscard]] auto Format() const noexcept { return format_; }
  [[nodiscard]] auto TileShape() const noexcept -> TileExtent { return tile_shape_; }
  [[nodiscard]] auto TileColumns() const noexcept { return tile_columns_; }
  [[nodiscard]] auto TileRows() const noexcept { return tile_rows_; }
  [[nodiscard]] auto TileCount() const noexcept -> size_t { return tiles_.size(); }
  [[nodiscard]] auto IsTileResident(TileIndex index) const -> bool;
  [[nodiscard]] auto TileRegion(TileIndex index) const -> TileRect;
  [[nodiscard]] auto HasSingleTile() const noexcept -> bool;

  auto               Tile(TileIndex index) -> WebGpuImage&;
  [[nodiscard]] auto Tile(TileIndex index) const -> const WebGpuImage&;
  auto               SingleTile() -> WebGpuImage&;
  [[nodiscard]] auto SingleTile() const -> const WebGpuImage&;

 private:
  [[nodiscard]] auto FlatIndex(TileIndex index) const -> size_t;
  [[nodiscard]] auto ResolveTileEdge(uint32_t requested_tile_edge) const -> uint32_t;
  void               EnsureTileAllocated(TileIndex index);

  uint32_t                 width_           = 0;
  uint32_t                 height_          = 0;
  PixelFormat              format_          = PixelFormat::RGBA32FLOAT;
  TileExtent               tile_shape_      = {};
  uint32_t                 tile_columns_    = 0;
  uint32_t                 tile_rows_       = 0;
  bool                     texture_binding_ = true;
  bool                     storage_binding_ = true;
  std::vector<WebGpuImage> tiles_;
};

}  // namespace webgpu
}  // namespace alcedo

#endif
