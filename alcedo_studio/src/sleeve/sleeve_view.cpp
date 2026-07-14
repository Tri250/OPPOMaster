//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "sleeve/sleeve_view.hpp"

#include <cstddef>
#include <cstdint>
#include <memory>
#include <unordered_map>
#include <vector>

#include "io/image/image_loader.hpp"
#include "sleeve/sleeve_element/sleeve_element.hpp"
#include "sleeve/sleeve_element/sleeve_file.hpp"
#include "sleeve/sleeve_element/sleeve_folder.hpp"
#include "storage/image_pool/image_pool_manager.hpp"
#include "type/type.hpp"
#include "utils/diagnostics/app_logging.hpp"

namespace alcedo {

DisplayingImage::DisplayingImage(std::weak_ptr<Image> displaying, bool require_thumb,
                                 bool require_full) {
  displaying_                = displaying.lock();
  displaying_->full_pinned_  = require_full;
  displaying_->thumb_pinned_ = require_thumb;
}

DisplayingImage::DisplayingImage(std::shared_ptr<Image> displaying, bool require_thumb,
                                 bool require_full) {
  displaying_                = displaying;
  displaying_->full_pinned_  = require_full;
  displaying_->thumb_pinned_ = require_thumb;
}

DisplayingImage::~DisplayingImage() {
  displaying_->full_pinned_  = false;
  displaying_->thumb_pinned_ = false;
}

SleeveView::SleeveView(std::shared_ptr<FileSystem> fs, std::shared_ptr<ImagePoolManager> image_pool)
    : fs_(fs), image_pool_(image_pool), loader_(64, 24, 0) {}

SleeveView::SleeveView(std::shared_ptr<FileSystem> fs, std::shared_ptr<ImagePoolManager> image_pool,
                       sl_path_t viewing_path)
    : fs_(fs), viewing_path_(viewing_path), image_pool_(image_pool), loader_(64, 24, 0) {
  UpdateView();
}

SleeveView::SleeveView(std::shared_ptr<FileSystem> fs, std::shared_ptr<ImagePoolManager> image_pool,
                       std::shared_ptr<SleeveFolder> viewing_node, sl_path_t viewing_path)
    : fs_(fs),
      viewing_node_(viewing_node),
      viewing_path_(viewing_path),
      image_pool_(image_pool),
      loader_(64, 24, 0) {
  auto& elements = viewing_node_.lock()->ListElements();
  for (auto& e : elements) {
    children_.push_back(fs_->Get(e));
  }
}

void SleeveView::UpdateView() {
  auto target = fs_->Get(viewing_path_, false);
  if (target->type_ != ElementType::FOLDER) {
    return;
  }
  viewing_node_ = std::dynamic_pointer_cast<SleeveFolder>(target);
  children_.clear();
  auto& elements = viewing_node_.lock()->ListElements();
  for (auto& e : elements) {
    children_.push_back(fs_->Get(e));
  }
}

void SleeveView::UpdateView(sl_path_t new_viewing_path) {
  viewing_path_ = new_viewing_path;
  UpdateView();
}

void SleeveView::LoadPreview(uint32_t range_low, uint32_t range_high,
                             std::function<void(size_t, std::weak_ptr<Image>)> callback) {
  std::unordered_map<image_id_t, size_t> index_map;
  uint32_t                               empty_img_count = 0;

  // to_display is a array to store images that require thumbnail lock.
  // once LoadPreview returns, all the images in the to_display will be released from their
  // thumbnail locks
  std::vector<DisplayingImage>           to_display;
  range_high = range_high > children_.size() - 1 ? children_.size() - 1 : range_high;

  // Notify UI framework in advance for each item in the range before any data changes
  for (size_t i = range_low; i <= range_high; ++i) {
    auto e_shared = children_[i].lock();
    if (e_shared->type_ == ElementType::FILE) {
      auto e_file = std::dynamic_pointer_cast<SleeveFile>(e_shared);
      // Pre-notify UI that loading is starting for this index
      callback(i, std::weak_ptr<Image>());
    }
  }

  for (size_t i = range_low; i <= range_high; ++i) {
    auto e_shared = children_[i].lock();
    if (e_shared->type_ == ElementType::FILE) {
      auto e_file  = std::dynamic_pointer_cast<SleeveFile>(e_shared);
      auto img_opt = image_pool_->AccessElement(e_file->GetImage()->image_id_, AccessType::THUMB);
      if (!img_opt.has_value()) {
        loader_.StartLoading(e_file->GetImage(), DecodeType::THUMB);
        ++empty_img_count;
      } else {
        qCInfo(appLog) << "SleeveView::LoadPreview: cached thumbnail available for index" << i;
        callback(i, img_opt.value());
        to_display.push_back({img_opt.value(), true, false});
      }
      index_map[e_file->GetImage()->image_id_] = i;
    } else {
      // Notify UI to display the "folder icon"
    }
  }

  // Grow cache if needed to accommodate the viewing range
  if (image_pool_->Capacity(AccessType::THUMB) < range_high - range_low + 10)
    image_pool_->ResizeCache(range_high - range_low + 10, AccessType::THUMB);

  // Shrink cache with LRU eviction when it far exceeds the current viewing range
  static constexpr uint32_t kCacheShrinkThreshold = 3;
  const uint32_t needed_capacity = range_high - range_low + 10;
  if (image_pool_->Capacity(AccessType::THUMB) > needed_capacity * kCacheShrinkThreshold) {
    uint32_t target_capacity = needed_capacity * 2;
    qCInfo(appLog) << "SleeveView::LoadPreview: shrinking thumb cache from"
                   << image_pool_->Capacity(AccessType::THUMB) << "to" << target_capacity
                   << "(LRU eviction will evict least recently used entries)";
    image_pool_->ResizeCache(target_capacity, AccessType::THUMB);
  }

  // Fetch loaded images and notify UI to update
  for (size_t i = 0; i < empty_img_count; i++) {
    auto   loaded     = loader_.LoadImage();
    size_t view_index = index_map.at(loaded->image_id_);
    qCInfo(appLog) << "SleeveView::LoadPreview: loaded thumbnail for image_id"
                   << loaded->image_id_ << "at view_index" << view_index;
    callback(view_index, loaded);
    image_pool_->RecordAccess(loaded->image_id_, AccessType::THUMB);
    to_display.push_back({loaded, true, false});
  }
}

};  // namespace alcedo