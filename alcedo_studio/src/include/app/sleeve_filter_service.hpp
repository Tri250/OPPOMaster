//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <memory>
#include <optional>
#include <string>
#include <vector>

#include "sleeve/sleeve_filter/filter_combo.hpp"
#include "sleeve/storage_service.hpp"
#include "type/type.hpp"
#include "utils/cache/lru_cache.hpp"
#include "utils/id/id_generator.hpp"

namespace alcedo {
struct StatsBucket {
  std::string label_{};
  int         count_ = 0;
};

struct AlbumStatsView {
  int                      total_photo_count_ = 0;
  std::vector<StatsBucket> date_stats_{};
  std::vector<StatsBucket> camera_stats_{};
  std::vector<StatsBucket> lens_stats_{};
  std::vector<StatsBucket> rating_stats_{};
};

struct FuzzySearchMatch {
  sl_element_id_t file_id_  = 0;
  image_id_t      image_id_ = 0;
  std::string     file_name_{};
};

struct FuzzySearchSuggestion {
  std::string category_{};
  std::string label_{};
  std::string query_{};
  int         count_ = 0;
};

class SemanticSearchProvider {
 public:
  virtual ~SemanticSearchProvider() = default;

  [[nodiscard]] virtual auto Search(sl_element_id_t folder_id, const std::wstring& query,
                                    size_t offset, size_t limit) const
      -> std::vector<FuzzySearchMatch> = 0;
};

// This service should not be used in multi-threaded scenarios.
class SleeveFilterService {
 private:
  std::shared_ptr<StorageService>                       storage_service_;
  std::shared_ptr<SemanticSearchProvider>               semantic_search_provider_{};

  // Filter will not be saved in DB for now. It will be only stored in memory for the lifetime of
  // the application.
  IncrID::IDGenerator<filter_id_t>                      filter_id_generator_;

  LRUCache<filter_id_t, std::shared_ptr<FilterCombo>>   filter_storage_;
  LRUCache<std::uint64_t, std::vector<sl_element_id_t>> filter_result_cache_;

 public:
  // Disable all copy operations
  SleeveFilterService()                                      = delete;
  SleeveFilterService(const SleeveFilterService&)            = delete;
  SleeveFilterService& operator=(const SleeveFilterService&) = delete;

  SleeveFilterService(std::shared_ptr<StorageService> storage_service)
      : storage_service_(std::move(storage_service)), filter_id_generator_(0) {}

  auto CreateFilterCombo(const FilterNode& root) -> filter_id_t;
  auto GetFilterCombo(filter_id_t filter_id) -> std::optional<std::shared_ptr<FilterCombo>>;
  void RemoveFilterCombo(filter_id_t filter_id);
  auto ApplyFilterOn(filter_id_t filter_id, sl_element_id_t parent_id)
      -> std::optional<std::vector<sl_element_id_t>>;
  auto BuildFolderStats(sl_element_id_t                  parent_id,
                        const std::optional<FilterNode>& extra_filter = std::nullopt) const
      -> AlbumStatsView;
  [[nodiscard]] auto BuildFuzzySearchWhere(const std::wstring& query) const
      -> std::optional<std::wstring>;
  [[nodiscard]] auto BuildExactFileWhere(sl_element_id_t file_id) const -> std::wstring;
  [[nodiscard]] auto SearchFolder(sl_element_id_t parent_id, const std::wstring& query,
                                  size_t offset = 0, size_t limit = 48) const
      -> std::vector<FuzzySearchMatch>;
  void               SetSemanticSearchProvider(std::shared_ptr<SemanticSearchProvider> provider);
  [[nodiscard]] auto SearchFolderSemantic(sl_element_id_t parent_id, const std::wstring& query,
                                          size_t offset = 0, size_t limit = 48) const
      -> std::vector<FuzzySearchMatch>;
  [[nodiscard]] auto CountSearchResults(sl_element_id_t parent_id, const std::wstring& query) const
      -> size_t;
  [[nodiscard]] auto BuildSearchSuggestions(sl_element_id_t parent_id, size_t limit = 12) const
      -> std::vector<FuzzySearchSuggestion>;

  /// Invalidate all cached filter results for a specific folder scope.
  /// Call after membership changes (link / unlink / delete) that affect that folder.
  void InvalidateResultCache(sl_element_id_t folder_id);

  /// Invalidate the entire filter result cache.
  void InvalidateResultCache();
};
}  // namespace alcedo
