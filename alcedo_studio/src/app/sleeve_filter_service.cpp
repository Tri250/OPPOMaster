//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/sleeve_filter_service.hpp"

#include <algorithm>
#include <cstdint>
#include <cwctype>
#include <format>
#include <memory>
#include <sstream>

namespace alcedo {
namespace {
auto FilterScopeCacheKey(filter_id_t filter_id, sl_element_id_t parent_id) -> std::uint64_t {
  return (static_cast<std::uint64_t>(parent_id) << 32U) | static_cast<std::uint64_t>(filter_id);
}

auto TrimCopy(std::wstring value) -> std::wstring {
  const auto first = std::find_if_not(value.begin(), value.end(),
                                      [](wchar_t ch) { return std::iswspace(ch) != 0; });
  const auto last  = std::find_if_not(value.rbegin(), value.rend(), [](wchar_t ch) {
                      return std::iswspace(ch) != 0;
                    }).base();
  if (first >= last) {
    return {};
  }
  return std::wstring(first, last);
}

auto SqlLikeEscape(const std::wstring& value) -> std::wstring {
  std::wstring out;
  out.reserve(value.size() + 4);
  for (const auto ch : value) {
    if (ch == L'\\' || ch == L'%' || ch == L'_') {
      out.push_back(L'\\');
    }
    out.push_back(ch);
    if (ch == L'\'') {
      out.push_back(L'\'');
    }
  }
  return out;
}

auto LikeClause(const std::wstring& expr, const std::wstring& token) -> std::wstring {
  const auto escaped = SqlLikeEscape(token);
  const auto value   = std::format(L"COALESCE({}, '')", expr);
  const auto pattern = std::format(L"'%{}%'", escaped);
  return std::format(L"(({} LIKE {} ESCAPE '\\') OR (LOWER({}) LIKE LOWER({}) ESCAPE '\\'))", value,
                     pattern, value, pattern);
}

auto SplitTokens(const std::wstring& query) -> std::vector<std::wstring> {
  std::wistringstream       stream(query);
  std::vector<std::wstring> tokens;
  std::wstring              token;
  while (stream >> token) {
    token = TrimCopy(token);
    if (!token.empty()) {
      tokens.push_back(std::move(token));
    }
  }
  return tokens;
}

auto DigitsOnly(const std::wstring& value) -> std::wstring {
  std::wstring digits;
  for (const auto ch : value) {
    if (std::iswdigit(ch) != 0) {
      digits.push_back(ch);
    }
  }
  return digits;
}

auto SafeToInt(const std::wstring& value) -> std::optional<int> {
  if (value.empty() || value.size() > 9) {
    return std::nullopt;
  }
  try {
    return std::stoi(value);
  } catch (...) {
    return std::nullopt;
  }
}

auto DigitGroups(const std::wstring& value) -> std::vector<int> {
  std::vector<int> groups;
  std::wstring     current;
  for (const auto ch : value) {
    if (std::iswdigit(ch) != 0) {
      if (current.size() < 9) {
        current.push_back(ch);
      }
      continue;
    }
    if (!current.empty()) {
      if (const auto parsed = SafeToInt(current); parsed.has_value()) {
        groups.push_back(*parsed);
      }
      current.clear();
    }
  }
  if (!current.empty()) {
    if (const auto parsed = SafeToInt(current); parsed.has_value()) {
      groups.push_back(*parsed);
    }
  }
  return groups;
}

auto IsValidMonth(int month) -> bool { return month >= 1 && month <= 12; }

auto IsValidDay(int day) -> bool { return day >= 1 && day <= 31; }

auto DateLiteral(int year, int month, int day) -> std::wstring {
  return std::format(L"{:04}-{:02}-{:02}", year, month, day);
}

auto NextMonthStart(int year, int month) -> std::wstring {
  if (month >= 12) {
    return DateLiteral(year + 1, 1, 1);
  }
  return DateLiteral(year, month + 1, 1);
}

auto DateColumn() -> std::wstring {
  return L"TRY_CAST(json_extract_string(i.metadata, '$.DateTimeString') AS DATE)";
}

auto DateMatchClauses(const std::wstring& token) -> std::vector<std::wstring> {
  std::vector<std::wstring> clauses;
  const auto                digits    = DigitsOnly(token);
  const auto                groups    = DigitGroups(token);
  const auto                col       = DateColumn();

  auto                      add_exact = [&](int year, int month, int day) {
    if (year >= 1000 && IsValidMonth(month) && IsValidDay(day)) {
      clauses.push_back(std::format(L"({} = DATE '{}')", col, DateLiteral(year, month, day)));
    }
  };
  auto add_month = [&](int year, int month) {
    if (year >= 1000 && IsValidMonth(month)) {
      clauses.push_back(std::format(L"({} >= DATE '{}' AND {} < DATE '{}')", col,
                                    DateLiteral(year, month, 1), col, NextMonthStart(year, month)));
    }
  };
  auto add_year = [&](int year) {
    if (year >= 1000) {
      clauses.push_back(std::format(L"({} >= DATE '{}' AND {} < DATE '{}')", col,
                                    DateLiteral(year, 1, 1), col, DateLiteral(year + 1, 1, 1)));
    }
  };

  if (digits.size() == 8) {
    const auto year  = SafeToInt(digits.substr(0, 4));
    const auto month = SafeToInt(digits.substr(4, 2));
    const auto day   = SafeToInt(digits.substr(6, 2));
    if (year.has_value() && month.has_value() && day.has_value()) {
      add_exact(*year, *month, *day);
    }
  } else if (digits.size() == 6) {
    const auto yy    = SafeToInt(digits.substr(0, 2));
    const auto month = SafeToInt(digits.substr(2, 2));
    const auto day   = SafeToInt(digits.substr(4, 2));
    if (yy.has_value() && month.has_value() && day.has_value()) {
      add_exact(*yy >= 70 ? 1900 + *yy : 2000 + *yy, *month, *day);
    }
  } else if (digits.size() == 4 && token.size() == 4) {
    if (const auto year = SafeToInt(digits); year.has_value()) {
      add_year(*year);
    }
  }

  if (groups.size() >= 3) {
    add_exact(groups[0], groups[1], groups[2]);
  } else if (groups.size() == 2) {
    add_month(groups[0], groups[1]);
  } else if (groups.size() == 1 && digits.size() == 4 && groups[0] >= 1000) {
    add_year(groups[0]);
  }

  clauses.push_back(LikeClause(L"json_extract_string(i.metadata, '$.DateTimeString')", token));
  return clauses;
}

auto JoinWith(const std::vector<std::wstring>& parts, const std::wstring& sep) -> std::wstring {
  std::wstring out;
  for (size_t i = 0; i < parts.size(); ++i) {
    if (i > 0) {
      out += sep;
    }
    out += parts[i];
  }
  return out;
}

auto TokenSearchClause(const std::wstring& token) -> std::wstring {
  std::vector<std::wstring> clauses{
      LikeClause(L"e.element_name", token),
      LikeClause(L"i.file_name", token),
      LikeClause(L"i.image_path", token),
      LikeClause(L"json_extract_string(i.metadata, '$.Make')", token),
      LikeClause(L"json_extract_string(i.metadata, '$.Model')", token),
      LikeClause(L"json_extract_string(i.metadata, '$.Lens')", token),
      LikeClause(L"json_extract_string(i.metadata, '$.LensMake')", token),
      LikeClause(L"CAST(i.metadata AS VARCHAR)", token),
      LikeClause(L"CAST(json_extract(i.metadata, '$.ISO') AS VARCHAR)", token),
      LikeClause(L"CAST(json_extract(i.metadata, '$.FocalLength') AS VARCHAR)", token),
      LikeClause(L"CAST(json_extract(i.metadata, '$.Aperture') AS VARCHAR)", token),
      LikeClause(L"CAST(json_extract(i.metadata, '$.Rating') AS VARCHAR)", token),
  };

  auto date_clauses = DateMatchClauses(token);
  clauses.insert(clauses.end(), date_clauses.begin(), date_clauses.end());

  const auto digits = DigitsOnly(token);
  if (digits.size() == 1) {
    const int rating = digits[0] - L'0';
    if (rating >= 0 && rating <= 5) {
      clauses.push_back(std::format(L"(json_extract(i.metadata, '$.Rating')::INT = {})", rating));
    }
  }

  return L"(" + JoinWith(clauses, L" OR ") + L")";
}

void AppendSuggestionRows(std::vector<FuzzySearchSuggestion>& out,
                          const std::vector<StatsBucket>& buckets, const std::string& category,
                          size_t limit) {
  for (const auto& bucket : buckets) {
    if (out.size() >= limit) {
      return;
    }
    if (bucket.label_.empty() || bucket.label_ == "(unknown)") {
      continue;
    }
    out.push_back({category, bucket.label_, bucket.label_, bucket.count_});
  }
}
}  // namespace

auto SleeveFilterService::CreateFilterCombo(const FilterNode& root) -> filter_id_t {
  filter_id_t new_id = filter_id_generator_.GenerateID();
  filter_storage_.RecordAccess(new_id, std::make_shared<FilterCombo>(new_id, root));
  return new_id;
}

auto SleeveFilterService::GetFilterCombo(filter_id_t filter_id)
    -> std::optional<std::shared_ptr<FilterCombo>> {
  auto combo_opt = filter_storage_.AccessElement(filter_id);
  if (combo_opt.has_value()) {
    return combo_opt.value();
  } else {
    return std::nullopt;
  }
}

void SleeveFilterService::RemoveFilterCombo(filter_id_t filter_id) {
  // If there is no record, this is a no-op.
  filter_storage_.RemoveRecord(filter_id);
  // Result cache keys include folder scope; flushing keeps removal simple and stable.
  filter_result_cache_.Flush();
}

auto SleeveFilterService::ApplyFilterOn(filter_id_t filter_id, sl_element_id_t parent_id)
    -> std::optional<std::vector<sl_element_id_t>> {
  // First, check if the filter combo exists.
  auto combo_opt = filter_storage_.AccessElement(filter_id);
  if (!combo_opt.has_value()) {
    return std::nullopt;
  }
  auto       combo      = combo_opt.value();

  // Next, check if we have a cached result for this filter in this folder scope.
  const auto cache_key  = FilterScopeCacheKey(filter_id, parent_id);
  auto       result_opt = filter_result_cache_.AccessElement(cache_key);
  if (result_opt.has_value()) {
    return result_opt;
  }

  // No cached result, we need to execute the filter.
  auto result_ids =
      storage_service_->GetElementController().GetElementIdsInFolderByFilter(combo, parent_id);
  // Cache the result for future use.
  filter_result_cache_.RecordAccess(cache_key, result_ids);
  return result_ids;
}

auto SleeveFilterService::BuildFolderStats(sl_element_id_t                  parent_id,
                                           const std::optional<FilterNode>& extra_filter) const
    -> AlbumStatsView {
  std::optional<std::wstring> extra_where;
  if (extra_filter.has_value()) {
    const auto where_w = FilterSQLCompiler::Compile(*extra_filter);
    if (!where_w.empty()) {
      extra_where = where_w;
    }
  }

  const auto storage_stats =
      storage_service_->GetElementController().BuildFolderStats(parent_id, extra_where);

  AlbumStatsView out;
  out.total_photo_count_ = storage_stats.total_photo_count_;

  out.date_stats_.reserve(storage_stats.date_stats_.size());
  for (const auto& bucket : storage_stats.date_stats_) {
    out.date_stats_.push_back({bucket.label_, bucket.count_});
  }

  out.camera_stats_.reserve(storage_stats.camera_stats_.size());
  for (const auto& bucket : storage_stats.camera_stats_) {
    out.camera_stats_.push_back({bucket.label_, bucket.count_});
  }

  out.lens_stats_.reserve(storage_stats.lens_stats_.size());
  for (const auto& bucket : storage_stats.lens_stats_) {
    out.lens_stats_.push_back({bucket.label_, bucket.count_});
  }

  out.rating_stats_.reserve(storage_stats.rating_stats_.size());
  for (const auto& bucket : storage_stats.rating_stats_) {
    out.rating_stats_.push_back({bucket.label_, bucket.count_});
  }

  return out;
}

auto SleeveFilterService::BuildFuzzySearchWhere(const std::wstring& query) const
    -> std::optional<std::wstring> {
  const auto trimmed = TrimCopy(query);
  if (trimmed.empty()) {
    return std::nullopt;
  }

  auto tokens = SplitTokens(trimmed);
  if (tokens.empty()) {
    return std::nullopt;
  }

  std::vector<std::wstring> token_clauses;
  token_clauses.reserve(tokens.size());
  for (const auto& token : tokens) {
    token_clauses.push_back(TokenSearchClause(token));
  }

  std::wstring where = L"(" + JoinWith(token_clauses, L" AND ") + L")";
  if (tokens.size() > 1) {
    where = L"(" + where + L" OR " + TokenSearchClause(trimmed) + L")";
  }
  return where;
}

auto SleeveFilterService::BuildExactFileWhere(sl_element_id_t file_id) const -> std::wstring {
  return std::format(L"e.id = {}", file_id);
}

auto SleeveFilterService::SearchFolder(sl_element_id_t parent_id, const std::wstring& query,
                                       size_t offset, size_t limit) const
    -> std::vector<FuzzySearchMatch> {
  std::vector<FuzzySearchMatch> out;
  if (!storage_service_) {
    return out;
  }
  const auto where = BuildFuzzySearchWhere(query);
  if (!where.has_value()) {
    return out;
  }

  const auto rows = storage_service_->GetElementController().ListFilesInFolderPage(
      parent_id, offset, limit, where);
  out.reserve(rows.size());
  for (const auto& row : rows) {
    out.push_back({row.file_id_, row.image_id_, row.file_name_});
  }
  return out;
}

void SleeveFilterService::SetSemanticSearchProvider(
    std::shared_ptr<SemanticSearchProvider> provider) {
  semantic_search_provider_ = std::move(provider);
}

auto SleeveFilterService::SearchFolderSemantic(sl_element_id_t parent_id, const std::wstring& query,
                                               size_t offset, size_t limit) const
    -> std::vector<FuzzySearchMatch> {
  if (!semantic_search_provider_) {
    return {};
  }
  return semantic_search_provider_->Search(parent_id, query, offset, limit);
}

auto SleeveFilterService::CountSearchResults(sl_element_id_t     parent_id,
                                             const std::wstring& query) const -> size_t {
  if (!storage_service_) {
    return 0;
  }
  const auto where = BuildFuzzySearchWhere(query);
  if (!where.has_value()) {
    return 0;
  }
  return storage_service_->GetElementController().CountFilesInFolder(parent_id, where);
}

auto SleeveFilterService::BuildSearchSuggestions(sl_element_id_t parent_id, size_t limit) const
    -> std::vector<FuzzySearchSuggestion> {
  std::vector<FuzzySearchSuggestion> out;
  if (limit == 0) {
    return out;
  }

  const auto stats = BuildFolderStats(parent_id);
  out.reserve(limit);
  AppendSuggestionRows(out, stats.camera_stats_, "camera", limit);
  AppendSuggestionRows(out, stats.date_stats_, "date", limit);
  AppendSuggestionRows(out, stats.lens_stats_, "lens", limit);
  AppendSuggestionRows(out, stats.rating_stats_, "rating", limit);
  return out;
}

void SleeveFilterService::InvalidateResultCache(sl_element_id_t folder_id) {
  const auto keys = filter_result_cache_.GetLRUKeys();
  for (const auto& key : keys) {
    const auto key_folder_id = static_cast<sl_element_id_t>(key >> 32U);
    if (key_folder_id == folder_id) {
      filter_result_cache_.RemoveRecord(key);
    }
  }
}

void SleeveFilterService::InvalidateResultCache() { filter_result_cache_.Flush(); }
}  // namespace alcedo
