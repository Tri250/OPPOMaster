//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "sleeve/sleeve_element/sleeve_element.hpp"

#include <chrono>
#include <memory>

#include "utils/clock/time_provider.hpp"
#include "utils/diagnostics/app_logging.hpp"

namespace alcedo {

SleeveElement::SleeveElement(sl_element_id_t id, file_name_t element_name)
    : element_id_(id), element_name_(element_name), ref_count_(0), pinned_(false) {
  this->SetAddTime();
}

SleeveElement::~SleeveElement() {}

auto SleeveElement::Copy(sl_element_id_t new_id) const -> std::shared_ptr<SleeveElement> {
  qCWarning(appLog) << "SleeveElement::Copy: base class copy called — derived classes should"
                    << "override; creating base-only copy for element" << element_id_;
  auto copy          = std::make_shared<SleeveElement>(new_id, element_name_);
  copy->type_        = type_;
  copy->added_time_  = added_time_;
  copy->last_modified_time_ = last_modified_time_;
  copy->pinned_     = pinned_;
  copy->sync_flag_  = sync_flag_;
  return copy;
}

auto SleeveElement::Clear() -> bool {
  // Placeholder
  return true;
}

void SleeveElement::SetAddTime() {
  added_time_         = std::chrono::system_clock::to_time_t(TimeProvider::Now());
  last_modified_time_ = added_time_;
}

void SleeveElement::SetLastModifiedTime() {
  last_modified_time_ = std::chrono::system_clock::to_time_t(TimeProvider::Now());
}

void SleeveElement::IncrementRefCount() { ++ref_count_; }

void SleeveElement::DecrementRefCount() {
  --ref_count_;
  if (ref_count_ <= 0) {
    sync_flag_ = SyncFlag::DELETED;
  }
}

auto SleeveElement::IsShared() -> bool { return ref_count_ > 1; }

void SleeveElement::SetSyncFlag(SyncFlag flag) { sync_flag_ = flag; }
};  // namespace alcedo