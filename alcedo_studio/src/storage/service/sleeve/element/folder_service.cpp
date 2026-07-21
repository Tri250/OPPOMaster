//  Copyright 2025 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "storage/service/sleeve/element/folder_service.hpp"

#include <format>
#include <sstream>
#include <unordered_map>
#include <unordered_set>
#include <vector>

#include "type/type.hpp"

namespace alcedo {
auto FolderService::ToParams(const std::pair<sl_element_id_t, sl_element_id_t> source)
    -> FolderMapperParams {
  return {source.first, source.second};
}

auto FolderService::FromParams(FolderMapperParams&& param)
    -> std::pair<sl_element_id_t, sl_element_id_t> {
  return {param.folder_id, param.element_id};
}

auto FolderService::GetFolderContent(const sl_element_id_t id) -> std::vector<sl_element_id_t> {
  auto                         results = GetByPredicate(std::format("folder_id={}", id));
  std::vector<sl_element_id_t> folder_content;
  folder_content.resize(results.size());
  for (size_t i = 0; i < results.size(); ++i) {
    folder_content[i] = results[i].second;
  }
  return folder_content;
}

void FolderService::RemoveAllContents(const sl_element_id_t folder_id) { RemoveById(folder_id); }

void FolderService::RemoveContentById(const sl_element_id_t content_id) {
  RemoveByClause(std::format("element_id={}", content_id));
}

void FolderService::RemoveContentByIds(std::span<const sl_element_id_t> content_ids) {
  if (content_ids.empty()) {
    return;
  }

  std::ostringstream                 id_list;
  std::unordered_set<sl_element_id_t> seen;
  seen.reserve(content_ids.size() * 2 + 1);
  bool first = true;
  for (const auto content_id : content_ids) {
    if (content_id == 0 || !seen.insert(content_id).second) {
      continue;
    }
    if (!first) {
      id_list << ",";
    }
    id_list << content_id;
    first = false;
  }

  if (first) {
    return;
  }
  RemoveByClause(std::format("element_id IN ({})", id_list.str()));
}

void FolderService::RemoveFolderContent(sl_element_id_t folder_id, sl_element_id_t content_id) {
  RemoveByClause(std::format("folder_id={} AND element_id={}", folder_id, content_id));
}

void FolderService::UpdateFolderContent(const std::vector<sl_element_id_t>& content,
                                        const sl_element_id_t               folder_id) {
  RemoveAllContents(folder_id);
  for (auto& element_id : content) {
    Insert({folder_id, element_id});
  }
}
};  // namespace alcedo
