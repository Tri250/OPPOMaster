//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <memory>
#include <span>
#include <string>
#include <utility>
#include <vector>

#include "image/image.hpp"
#include "storage/controller/controller_types.hpp"
#include "storage/image_pool/image_pool_manager.hpp"
#include "storage/service/image/image_service.hpp"
#include "type/type.hpp"

namespace alcedo {
class ImageController {
 private:
  ConnectionGuard guard_;
  ImageService    service_;

 public:
  ImageController(ConnectionGuard&& guard);
  void CaptureImagePool(const std::shared_ptr<ImagePoolManager> image_pool);
  void AddImage(const std::shared_ptr<Image> image);
  // Bulk-insert a batch of images in a single transaction. Callers that persist more
  // than one image (e.g. import sync) should prefer this over the per-row AddImage,
  // since one transaction for N rows is dramatically cheaper in DuckDB than N.
  void AddImages(std::span<const std::shared_ptr<Image>> images);
  void RemoveImageById(const image_id_t remove_id);
  void RemoveImagesByIds(std::span<const image_id_t> remove_ids);
  void RemoveImageByType(const ImageType type);
  void RemoveImageByPath(const std::wstring& path);

  void UpdateImage(const std::shared_ptr<Image> image);
  // Bulk-update a batch of images in a single transaction, keyed by image id.
  void UpdateImages(std::span<const std::pair<image_id_t, std::shared_ptr<Image>>> updates);
  
  auto GetImageById(const image_id_t id) -> std::shared_ptr<Image>;
  auto GetImageByType(const ImageType type) -> std::vector<std::shared_ptr<Image>>;
  auto GetImageByName(const std::wstring& name) -> std::vector<std::shared_ptr<Image>>;
  auto GetImageByPath(const std::filesystem::path path) -> std::vector<std::shared_ptr<Image>>;
};
};  // namespace alcedo
