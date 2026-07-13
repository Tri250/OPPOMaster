//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/tag_manager.hpp"

#include <algorithm>
#include <cmath>
#include <filesystem>
#include <fstream>
#include <functional>
#include <sstream>
#include <stdexcept>

namespace alcedo::app {

namespace {

// ── JSON helpers (minimal, no external dependency) ──────────────────────────

auto json_escape(const std::string& s) -> std::string {
  std::string out;
  out.reserve(s.size() + 4);
  for (char c : s) {
    switch (c) {
      case '"':  out += "\\\""; break;
      case '\\': out += "\\\\"; break;
      case '\b': out += "\\b";  break;
      case '\f': out += "\\f";  break;
      case '\n': out += "\\n";  break;
      case '\r': out += "\\r";  break;
      case '\t': out += "\\t";  break;
      default:
        if (static_cast<unsigned char>(c) < 0x20) {
          char buf[8];
          std::snprintf(buf, sizeof(buf), "\\u%04x",
                        static_cast<unsigned>(static_cast<unsigned char>(c)));
          out += buf;
        } else {
          out += c;
        }
        break;
    }
  }
  return out;
}

auto json_unescape(const std::string& s) -> std::string {
  std::string out;
  out.reserve(s.size());
  for (size_t i = 0; i < s.size(); ++i) {
    if (s[i] == '\\' && i + 1 < s.size()) {
      switch (s[i + 1]) {
        case '"':  out += '"';  ++i; break;
        case '\\': out += '\\'; ++i; break;
        case '/':  out += '/';  ++i; break;
        case 'b':  out += '\b'; ++i; break;
        case 'f':  out += '\f'; ++i; break;
        case 'n':  out += '\n'; ++i; break;
        case 'r':  out += '\r'; ++i; break;
        case 't':  out += '\t'; ++i; break;
        case 'u':  {
          // Skip \uXXXX for simplicity — keep as-is
          out += s[i];
          break;
        }
        default:
          out += s[i];
          break;
      }
    } else {
      out += s[i];
    }
  }
  return out;
}

/// Skip whitespace in a JSON string view.
auto skip_ws(const std::string& s, size_t& pos) -> void {
  while (pos < s.size() &&
         (s[pos] == ' ' || s[pos] == '\t' || s[pos] == '\n' || s[pos] == '\r')) {
    ++pos;
  }
}

/// Parse a JSON string value starting at pos (which should point to the
/// opening '"').  Returns the unescaped string content.
auto parse_json_string(const std::string& s, size_t& pos) -> std::string {
  if (pos >= s.size() || s[pos] != '"') return {};
  ++pos;  // skip opening quote
  std::string result;
  while (pos < s.size() && s[pos] != '"') {
    if (s[pos] == '\\' && pos + 1 < s.size()) {
      result += s[pos];
      result += s[pos + 1];
      pos += 2;
    } else {
      result += s[pos];
      ++pos;
    }
  }
  if (pos < s.size()) ++pos;  // skip closing quote
  return json_unescape(result);
}

/// Parse a JSON number (integer only).
auto parse_json_int(const std::string& s, size_t& pos) -> int64_t {
  size_t start = pos;
  if (pos < s.size() && (s[pos] == '-' || s[pos] == '+')) ++pos;
  while (pos < s.size() && s[pos] >= '0' && s[pos] <= '9') ++pos;
  try {
    return std::stoll(s.substr(start, pos - start));
  } catch (...) {
    return 0;
  }
}

/// Extract a JSON array of strings: ["a", "b", ...]
auto parse_json_string_array(const std::string& s, size_t& pos)
    -> std::vector<std::string> {
  std::vector<std::string> result;
  skip_ws(s, pos);
  if (pos >= s.size() || s[pos] != '[') return result;
  ++pos;  // skip '['
  skip_ws(s, pos);
  while (pos < s.size() && s[pos] != ']') {
    skip_ws(s, pos);
    if (s[pos] == '"') {
      result.push_back(parse_json_string(s, pos));
    } else {
      ++pos;  // skip unexpected char
    }
    skip_ws(s, pos);
    if (pos < s.size() && s[pos] == ',') ++pos;
    skip_ws(s, pos);
  }
  if (pos < s.size()) ++pos;  // skip ']'
  return result;
}

/// Simple XML/XMP helpers ──────────────────────────────────────────────────

auto to_lower(std::string s) -> std::string {
  std::transform(s.begin(), s.end(), s.begin(),
                 [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
  return s;
}

auto xml_escape(const std::string& s) -> std::string {
  std::string out;
  out.reserve(s.size() + 8);
  for (char c : s) {
    switch (c) {
      case '&':  out += "&amp;";  break;
      case '<':  out += "&lt;";   break;
      case '>':  out += "&gt;";   break;
      case '"':  out += "&quot;"; break;
      case '\'': out += "&apos;"; break;
      default:   out += c;        break;
    }
  }
  return out;
}

auto xml_unescape(std::string s) -> std::string {
  auto replace_all = [](std::string& str, const std::string& from,
                        const std::string& to) {
    size_t pos = 0;
    while ((pos = str.find(from, pos)) != std::string::npos) {
      str.replace(pos, from.size(), to);
      pos += to.size();
    }
  };
  replace_all(s, "&amp;", "&");
  replace_all(s, "&lt;", "<");
  replace_all(s, "&gt;", ">");
  replace_all(s, "&quot;", "\"");
  replace_all(s, "&apos;", "'");
  return s;
}

}  // namespace

// ── Color label free functions ──────────────────────────────────────────────

auto color_label_to_string(ColorLabel label) -> std::string_view {
  switch (label) {
    case ColorLabel::Red:    return "Red";
    case ColorLabel::Orange: return "Orange";
    case ColorLabel::Yellow: return "Yellow";
    case ColorLabel::Green:  return "Green";
    case ColorLabel::Blue:   return "Blue";
    case ColorLabel::Purple: return "Purple";
    case ColorLabel::None:   return "None";
  }
  return "None";
}

auto color_label_from_string(std::string_view name) -> ColorLabel {
  auto lower = to_lower(std::string(name));
  if (lower == "red")    return ColorLabel::Red;
  if (lower == "orange") return ColorLabel::Orange;
  if (lower == "yellow") return ColorLabel::Yellow;
  if (lower == "green")  return ColorLabel::Green;
  if (lower == "blue")   return ColorLabel::Blue;
  if (lower == "purple") return ColorLabel::Purple;
  return ColorLabel::None;
}

// ── TagManager: internal helpers ────────────────────────────────────────────

auto TagManager::ensure_image(ImageId image_id) -> ImageTagData& {
  return image_data_[image_id];
}

auto TagManager::find_or_create_tag(const std::string& tag_path) -> TagId {
  // Normalize: trim whitespace, collapse consecutive slashes.
  std::string normalized;
  normalized.reserve(tag_path.size());
  bool last_was_slash = false;
  for (char c : tag_path) {
    if (c == '/') {
      if (!last_was_slash && !normalized.empty()) {
        normalized += c;
        last_was_slash = true;
      }
    } else {
      normalized += c;
      last_was_slash = false;
    }
  }
  // Remove trailing slash.
  while (!normalized.empty() && normalized.back() == '/') {
    normalized.pop_back();
  }
  if (normalized.empty()) return 0;

  // Check if it already exists.
  auto it = path_to_id_.find(normalized);
  if (it != path_to_id_.end()) return it->second;

  // Ensure parent exists first.
  std::string parent_path = extract_parent_path(normalized);
  TagId parent_id = 0;
  if (!parent_path.empty()) {
    parent_id = find_or_create_tag(parent_path);
  }

  // Create the new tag.
  TagId new_id = next_tag_id_++;
  TagEntry entry;
  entry.id          = new_id;
  entry.full_path   = normalized;
  entry.display_name = extract_leaf_name(normalized);
  entry.parent_id   = parent_id;
  entry.image_count = 0;

  tags_[new_id]     = std::move(entry);
  path_to_id_[normalized] = new_id;
  tag_to_images_[new_id];  // ensure entry exists

  return new_id;
}

auto TagManager::extract_leaf_name(const std::string& tag_path) -> std::string {
  auto pos = tag_path.rfind('/');
  if (pos == std::string::npos) return tag_path;
  return tag_path.substr(pos + 1);
}

auto TagManager::extract_parent_path(const std::string& tag_path) -> std::string {
  auto pos = tag_path.rfind('/');
  if (pos == std::string::npos) return {};
  return tag_path.substr(0, pos);
}

auto TagManager::levenshtein_distance(std::string_view a,
                                       std::string_view b) -> int {
  auto m = a.size();
  auto n = b.size();
  std::vector<int> prev(n + 1), curr(n + 1);

  for (size_t j = 0; j <= n; ++j) prev[j] = static_cast<int>(j);

  for (size_t i = 1; i <= m; ++i) {
    curr[0] = static_cast<int>(i);
    for (size_t j = 1; j <= n; ++j) {
      int cost = (a[i - 1] == b[j - 1]) ? 0 : 1;
      curr[j] = std::min({prev[j] + 1,
                          curr[j - 1] + 1,
                          prev[j - 1] + cost});
    }
    std::swap(prev, curr);
  }
  return prev[n];
}

// ── Color labels ────────────────────────────────────────────────────────────

void TagManager::set_color_label(ImageId image_id, ColorLabel label) {
  auto& data = ensure_image(image_id);

  // Remove from old color index.
  if (data.color_label != ColorLabel::None) {
    color_index_[data.color_label].erase(image_id);
  }

  data.color_label = label;

  // Add to new color index.
  if (label != ColorLabel::None) {
    color_index_[label].insert(image_id);
  }
}

auto TagManager::get_color_label(ImageId image_id) const -> ColorLabel {
  auto it = image_data_.find(image_id);
  if (it == image_data_.end()) return ColorLabel::None;
  return it->second.color_label;
}

void TagManager::clear_color_label(ImageId image_id) {
  set_color_label(image_id, ColorLabel::None);
}

auto TagManager::get_images_by_color(ColorLabel label) const
    -> std::vector<ImageId> {
  std::vector<ImageId> result;
  if (label == ColorLabel::None) {
    // Return images with no color label.
    for (const auto& [id, data] : image_data_) {
      if (data.color_label == ColorLabel::None) {
        result.push_back(id);
      }
    }
  } else {
    auto it = color_index_.find(label);
    if (it != color_index_.end()) {
      result.assign(it->second.begin(), it->second.end());
    }
  }
  std::sort(result.begin(), result.end());
  return result;
}

// ── Star rating ─────────────────────────────────────────────────────────────

void TagManager::set_rating(ImageId image_id, StarRating rating) {
  auto& data = ensure_image(image_id);
  data.rating = rating;
}

auto TagManager::get_rating(ImageId image_id) const -> StarRating {
  auto it = image_data_.find(image_id);
  if (it == image_data_.end()) return StarRating();
  return it->second.rating;
}

auto TagManager::get_images_by_rating_range(StarRating min_rating,
                                             StarRating max_rating) const
    -> std::vector<ImageId> {
  std::vector<ImageId> result;
  for (const auto& [id, data] : image_data_) {
    if (data.rating >= min_rating && data.rating <= max_rating) {
      result.push_back(id);
    }
  }
  std::sort(result.begin(), result.end());
  return result;
}

// ── Tag / keyword management ────────────────────────────────────────────────

auto TagManager::add_tag(ImageId image_id, const std::string& tag_path)
    -> TagId {
  TagId tag_id = find_or_create_tag(tag_path);
  if (tag_id == 0) return 0;

  auto& data = ensure_image(image_id);

  // If the image already has this tag, nothing to do.
  if (data.tag_ids.count(tag_id) > 0) return tag_id;

  data.tag_ids.insert(tag_id);
  tag_to_images_[tag_id].insert(image_id);

  // Increment image_count for this tag and all its ancestors.
  TagId current = tag_id;
  while (current != 0) {
    auto it = tags_.find(current);
    if (it != tags_.end()) {
      ++it->second.image_count;
      current = it->second.parent_id;
    } else {
      break;
    }
  }

  return tag_id;
}

auto TagManager::remove_tag(ImageId image_id, const std::string& tag_path)
    -> bool {
  auto path_it = path_to_id_.find(tag_path);
  if (path_it == path_to_id_.end()) return false;
  return remove_tag_by_id(image_id, path_it->second);
}

auto TagManager::remove_tag_by_id(ImageId image_id, TagId tag_id) -> bool {
  auto data_it = image_data_.find(image_id);
  if (data_it == image_data_.end()) return false;

  auto& tag_ids = data_it->second.tag_ids;
  if (tag_ids.erase(tag_id) == 0) return false;

  // Remove from reverse lookup.
  auto rev_it = tag_to_images_.find(tag_id);
  if (rev_it != tag_to_images_.end()) {
    rev_it->second.erase(image_id);
  }

  // Decrement image_count for this tag and all ancestors.
  TagId current = tag_id;
  while (current != 0) {
    auto it = tags_.find(current);
    if (it != tags_.end()) {
      if (it->second.image_count > 0) {
        --it->second.image_count;
      }
      current = it->second.parent_id;
    } else {
      break;
    }
  }

  return true;
}

auto TagManager::get_tags_for_image(ImageId image_id) const
    -> std::vector<TagEntry> {
  auto it = image_data_.find(image_id);
  if (it == image_data_.end()) return {};

  std::vector<TagEntry> result;
  result.reserve(it->second.tag_ids.size());
  for (TagId tid : it->second.tag_ids) {
    auto tag_it = tags_.find(tid);
    if (tag_it != tags_.end()) {
      result.push_back(tag_it->second);
    }
  }
  return result;
}

auto TagManager::get_images_for_tag(TagId tag_id) const
    -> std::vector<ImageId> {
  auto it = tag_to_images_.find(tag_id);
  if (it == tag_to_images_.end()) return {};
  std::vector<ImageId> result(it->second.begin(), it->second.end());
  std::sort(result.begin(), result.end());
  return result;
}

auto TagManager::get_images_for_tag_path(const std::string& tag_path) const
    -> std::vector<ImageId> {
  auto it = path_to_id_.find(tag_path);
  if (it == path_to_id_.end()) return {};
  return get_images_for_tag(it->second);
}

auto TagManager::tag_exists(const std::string& tag_path) const -> bool {
  return path_to_id_.find(tag_path) != path_to_id_.end();
}

auto TagManager::find_tag(const std::string& tag_path) const
    -> const TagEntry* {
  auto it = path_to_id_.find(tag_path);
  if (it == path_to_id_.end()) return nullptr;
  auto tag_it = tags_.find(it->second);
  if (tag_it == tags_.end()) return nullptr;
  return &tag_it->second;
}

auto TagManager::find_tag_by_id(TagId id) const -> const TagEntry* {
  auto it = tags_.find(id);
  if (it == tags_.end()) return nullptr;
  return &it->second;
}

auto TagManager::get_all_tags() const -> std::vector<TagEntry> {
  std::vector<TagEntry> result;
  result.reserve(tags_.size());
  for (const auto& [_, entry] : tags_) {
    result.push_back(entry);
  }
  return result;
}

auto TagManager::tag_count() const -> size_t {
  return tags_.size();
}

void TagManager::delete_tag(TagId tag_id) {
  auto it = tags_.find(tag_id);
  if (it == tags_.end()) return;

  // Remove this tag from all images that carry it.
  auto rev_it = tag_to_images_.find(tag_id);
  if (rev_it != tag_to_images_.end()) {
    for (ImageId img_id : rev_it->second) {
      auto data_it = image_data_.find(img_id);
      if (data_it != image_data_.end()) {
        data_it->second.tag_ids.erase(tag_id);
      }
    }
    tag_to_images_.erase(rev_it);
  }

  // Remove from path_to_id_.
  path_to_id_.erase(it->second.full_path);

  // Delete any children that have this as parent.
  std::vector<TagId> children;
  for (const auto& [cid, centry] : tags_) {
    if (centry.parent_id == tag_id) {
      children.push_back(cid);
    }
  }
  for (TagId cid : children) {
    delete_tag(cid);
  }

  tags_.erase(it);
}

// ── Bulk operations ─────────────────────────────────────────────────────────

auto TagManager::bulk_add_tag(const std::vector<ImageId>& image_ids,
                               const std::string& tag_path) -> TagId {
  TagId tag_id = find_or_create_tag(tag_path);
  if (tag_id == 0) return 0;

  for (ImageId img_id : image_ids) {
    auto& data = ensure_image(img_id);
    if (data.tag_ids.count(tag_id) > 0) continue;

    data.tag_ids.insert(tag_id);
    tag_to_images_[tag_id].insert(img_id);

    // Increment ancestor counts.
    TagId current = tag_id;
    while (current != 0) {
      auto it = tags_.find(current);
      if (it != tags_.end()) {
        ++it->second.image_count;
        current = it->second.parent_id;
      } else {
        break;
      }
    }
  }

  return tag_id;
}

void TagManager::bulk_remove_tag(const std::vector<ImageId>& image_ids,
                                  const std::string& tag_path) {
  auto path_it = path_to_id_.find(tag_path);
  if (path_it == path_to_id_.end()) return;
  TagId tag_id = path_it->second;

  for (ImageId img_id : image_ids) {
    remove_tag_by_id(img_id, tag_id);
  }
}

// ── Auto-complete & search ─────────────────────────────────────────────────

auto TagManager::autocomplete(const std::string& prefix,
                               size_t max_results) const
    -> std::vector<TagSuggestion> {
  std::vector<TagSuggestion> matches;

  // Normalize prefix for case-insensitive matching.
  auto lower_prefix = to_lower(prefix);

  for (const auto& [_, entry] : tags_) {
    auto lower_path = to_lower(entry.full_path);
    auto lower_leaf = to_lower(entry.display_name);

    int relevance = 0;
    // Exact prefix match on full path gets highest relevance.
    if (lower_path.compare(0, lower_prefix.size(), lower_prefix) == 0) {
      relevance = 1000;
    }
    // Prefix match on leaf name.
    else if (lower_leaf.compare(0, lower_prefix.size(), lower_prefix) == 0) {
      relevance = 800;
    }
    // Substring match on full path.
    else if (lower_path.find(lower_prefix) != std::string::npos) {
      relevance = 400;
    }
    // Substring match on leaf name.
    else if (lower_leaf.find(lower_prefix) != std::string::npos) {
      relevance = 200;
    }

    if (relevance > 0) {
      // Boost by image count.
      relevance += static_cast<int>(
          std::min(entry.image_count, static_cast<uint32_t>(100)));
      matches.push_back({entry.full_path, relevance});
    }
  }

  // Sort by relevance descending, then alphabetically.
  std::sort(matches.begin(), matches.end(),
            [](const TagSuggestion& a, const TagSuggestion& b) {
              if (a.relevance != b.relevance) return a.relevance > b.relevance;
              return a.full_path < b.full_path;
            });

  if (matches.size() > max_results) {
    matches.resize(max_results);
  }
  return matches;
}

auto TagManager::fuzzy_search(const std::string& query,
                               int max_distance,
                               size_t max_results) const
    -> std::vector<TagSuggestion> {
  std::vector<TagSuggestion> matches;
  auto lower_query = to_lower(query);

  for (const auto& [_, entry] : tags_) {
    auto lower_path = to_lower(entry.full_path);
    auto lower_leaf = to_lower(entry.display_name);

    // Check edit distance against the leaf name (most common use case).
    int dist = levenshtein_distance(lower_leaf, lower_query);
    // Also check against the full path.
    int path_dist = levenshtein_distance(lower_path, lower_query);

    int min_dist = std::min(dist, path_dist);

    if (min_dist <= max_distance) {
      // Relevance: lower distance = higher relevance, boosted by image count.
      int relevance = (max_distance - min_dist + 1) * 100 +
                      static_cast<int>(
                          std::min(entry.image_count, static_cast<uint32_t>(50)));
      matches.push_back({entry.full_path, relevance});
    }
  }

  std::sort(matches.begin(), matches.end(),
            [](const TagSuggestion& a, const TagSuggestion& b) {
              if (a.relevance != b.relevance) return a.relevance > b.relevance;
              return a.full_path < b.full_path;
            });

  if (matches.size() > max_results) {
    matches.resize(max_results);
  }
  return matches;
}

// ── Tag statistics ──────────────────────────────────────────────────────────

auto TagManager::get_tag_image_count(TagId tag_id) const -> uint32_t {
  auto it = tags_.find(tag_id);
  if (it == tags_.end()) return 0;
  return it->second.image_count;
}

auto TagManager::get_tag_statistics() const
    -> std::vector<std::pair<TagEntry, uint32_t>> {
  std::vector<std::pair<TagEntry, uint32_t>> result;
  result.reserve(tags_.size());
  for (const auto& [_, entry] : tags_) {
    result.emplace_back(entry, entry.image_count);
  }
  // Sort by image count descending.
  std::sort(result.begin(), result.end(),
            [](const auto& a, const auto& b) {
              return a.second > b.second;
            });
  return result;
}

// ── Tag suggestion ──────────────────────────────────────────────────────────

auto TagManager::suggest_tags_for_image(ImageId image_id,
                                         size_t max_suggestions) const
    -> std::vector<TagSuggestion> {
  auto it = image_data_.find(image_id);
  if (it == image_data_.end()) return {};

  const auto& image_tags = it->second.tag_ids;
  if (image_tags.empty()) return {};

  // Count co-occurrences: for each tag this image has, look at all other
  // images that also have that tag, and collect their tags.
  std::unordered_map<TagId, int> co_occurrence;
  for (TagId tid : image_tags) {
    auto rev_it = tag_to_images_.find(tid);
    if (rev_it == tag_to_images_.end()) continue;

    for (ImageId other_img : rev_it->second) {
      if (other_img == image_id) continue;
      auto other_it = image_data_.find(other_img);
      if (other_it == image_data_.end()) continue;

      for (TagId other_tid : other_it->second.tag_ids) {
        // Don't suggest tags the image already has.
        if (image_tags.count(other_tid) > 0) continue;
        ++co_occurrence[other_tid];
      }
    }
  }

  // Build suggestions sorted by co-occurrence count.
  std::vector<TagSuggestion> suggestions;
  for (const auto& [tid, count] : co_occurrence) {
    auto tag_it = tags_.find(tid);
    if (tag_it != tags_.end()) {
      suggestions.push_back({tag_it->second.full_path, count});
    }
  }

  std::sort(suggestions.begin(), suggestions.end(),
            [](const TagSuggestion& a, const TagSuggestion& b) {
              return a.relevance > b.relevance;
            });

  if (suggestions.size() > max_suggestions) {
    suggestions.resize(max_suggestions);
  }
  return suggestions;
}

// ── Persistence: JSON ───────────────────────────────────────────────────────

auto TagManager::state_to_json_string() const -> std::string {
  std::ostringstream ss;
  ss << "{\n";

  // next_tag_id
  ss << "  \"next_tag_id\": " << next_tag_id_ << ",\n";

  // tags array
  ss << "  \"tags\": [\n";
  {
    bool first = true;
    for (const auto& [_, entry] : tags_) {
      if (!first) ss << ",\n";
      first = false;
      ss << "    {\"id\": " << entry.id
         << ", \"full_path\": \"" << json_escape(entry.full_path) << "\""
         << ", \"display_name\": \"" << json_escape(entry.display_name) << "\""
         << ", \"parent_id\": " << entry.parent_id
         << ", \"image_count\": " << entry.image_count
         << "}";
    }
  }
  ss << "\n  ],\n";

  // images array
  ss << "  \"images\": [\n";
  {
    bool first = true;
    for (const auto& [img_id, data] : image_data_) {
      if (!first) ss << ",\n";
      first = false;
      ss << "    {\"id\": " << img_id
         << ", \"color_label\": \"" << color_label_to_string(data.color_label) << "\""
         << ", \"rating_steps\": " << data.rating.steps()
         << ", \"tag_ids\": [";
      {
        bool f2 = true;
        for (TagId tid : data.tag_ids) {
          if (!f2) ss << ", ";
          f2 = false;
          ss << tid;
        }
      }
      ss << "]}";
    }
  }
  ss << "\n  ]\n";

  ss << "}\n";
  return ss.str();
}

auto TagManager::state_from_json_string(const std::string& json_str) -> bool {
  // Minimal JSON parser for our known format.
  // Clear current state.
  tags_.clear();
  path_to_id_.clear();
  image_data_.clear();
  tag_to_images_.clear();
  color_index_.clear();
  next_tag_id_ = 1;

  // Find "next_tag_id"
  {
    auto pos = json_str.find("\"next_tag_id\"");
    if (pos != std::string::npos) {
      pos = json_str.find(':', pos);
      if (pos != std::string::npos) {
        ++pos;
        next_tag_id_ = static_cast<TagId>(parse_json_int(json_str, pos));
      }
    }
  }

  // Parse "tags" array.
  {
    auto pos = json_str.find("\"tags\"");
    if (pos != std::string::npos) {
      pos = json_str.find('[', pos);
      if (pos != std::string::npos) {
        ++pos;
        skip_ws(json_str, pos);
        while (pos < json_str.size() && json_str[pos] != ']') {
          if (json_str[pos] != '{') { ++pos; continue; }
          ++pos;  // skip '{'

          TagEntry entry;
          // Parse tag object fields.
          while (pos < json_str.size() && json_str[pos] != '}') {
            skip_ws(json_str, pos);
            if (json_str[pos] != '"') { ++pos; continue; }
            std::string key = parse_json_string(json_str, pos);
            skip_ws(json_str, pos);
            if (pos < json_str.size() && json_str[pos] == ':') ++pos;
            skip_ws(json_str, pos);

            if (key == "id") {
              entry.id = static_cast<TagId>(parse_json_int(json_str, pos));
            } else if (key == "full_path") {
              entry.full_path = parse_json_string(json_str, pos);
            } else if (key == "display_name") {
              entry.display_name = parse_json_string(json_str, pos);
            } else if (key == "parent_id") {
              entry.parent_id = static_cast<TagId>(parse_json_int(json_str, pos));
            } else if (key == "image_count") {
              entry.image_count = static_cast<uint32_t>(parse_json_int(json_str, pos));
            }

            skip_ws(json_str, pos);
            if (pos < json_str.size() && json_str[pos] == ',') ++pos;
          }
          if (pos < json_str.size()) ++pos;  // skip '}'

          tags_[entry.id] = entry;
          path_to_id_[entry.full_path] = entry.id;
          tag_to_images_[entry.id];  // ensure entry

          skip_ws(json_str, pos);
          if (pos < json_str.size() && json_str[pos] == ',') ++pos;
          skip_ws(json_str, pos);
        }
      }
    }
  }

  // Parse "images" array.
  {
    auto pos = json_str.find("\"images\"");
    if (pos != std::string::npos) {
      pos = json_str.find('[', pos);
      if (pos != std::string::npos) {
        ++pos;
        skip_ws(json_str, pos);
        while (pos < json_str.size() && json_str[pos] != ']') {
          if (json_str[pos] != '{') { ++pos; continue; }
          ++pos;  // skip '{'

          ImageId img_id = 0;
          ImageTagData data;
          std::vector<TagId> tag_id_list;

          while (pos < json_str.size() && json_str[pos] != '}') {
            skip_ws(json_str, pos);
            if (json_str[pos] != '"') { ++pos; continue; }
            std::string key = parse_json_string(json_str, pos);
            skip_ws(json_str, pos);
            if (pos < json_str.size() && json_str[pos] == ':') ++pos;
            skip_ws(json_str, pos);

            if (key == "id") {
              img_id = static_cast<ImageId>(parse_json_int(json_str, pos));
            } else if (key == "color_label") {
              auto label_str = parse_json_string(json_str, pos);
              data.color_label = color_label_from_string(label_str);
            } else if (key == "rating_steps") {
              auto steps = parse_json_int(json_str, pos);
              data.rating = StarRating::from_steps(static_cast<int>(steps));
            } else if (key == "tag_ids") {
              auto ids = parse_json_string_array(json_str, pos);
              for (const auto& id_str : ids) {
                try {
                  tag_id_list.push_back(static_cast<TagId>(std::stoul(id_str)));
                } catch (...) {}
              }
            }

            skip_ws(json_str, pos);
            if (pos < json_str.size() && json_str[pos] == ',') ++pos;
          }
          if (pos < json_str.size()) ++pos;  // skip '}'

          if (img_id != 0) {
            for (TagId tid : tag_id_list) {
              data.tag_ids.insert(tid);
              tag_to_images_[tid].insert(img_id);
            }
            image_data_[img_id] = std::move(data);

            // Rebuild color index.
            auto& stored = image_data_[img_id];
            if (stored.color_label != ColorLabel::None) {
              color_index_[stored.color_label].insert(img_id);
            }
          }

          skip_ws(json_str, pos);
          if (pos < json_str.size() && json_str[pos] == ',') ++pos;
          skip_ws(json_str, pos);
        }
      }
    }
  }

  return true;
}

auto TagManager::save_to_json(const std::string& file_path) const
    -> TagPersistenceResult {
  TagPersistenceResult result;
  try {
    std::ofstream out(file_path, std::ios::binary | std::ios::trunc);
    if (!out) {
      result.error_message = "Cannot open file for writing: " + file_path;
      return result;
    }
    out << state_to_json_string();
    result.success = true;
  } catch (const std::exception& e) {
    result.error_message = std::string("Failed to save tags: ") + e.what();
  }
  return result;
}

auto TagManager::load_from_json(const std::string& file_path)
    -> TagPersistenceResult {
  TagPersistenceResult result;
  try {
    std::ifstream in(file_path, std::ios::binary);
    if (!in) {
      result.error_message = "Cannot open file for reading: " + file_path;
      return result;
    }
    std::string content((std::istreambuf_iterator<char>(in)),
                         std::istreambuf_iterator<char>());
    if (!state_from_json_string(content)) {
      result.error_message = "Failed to parse JSON: " + file_path;
      return result;
    }
    result.success = true;
  } catch (const std::exception& e) {
    result.error_message = std::string("Failed to load tags: ") + e.what();
  }
  return result;
}

// ── Persistence: tag hierarchy export/import ────────────────────────────────

void TagManager::build_hierarchy_tree(TagId parent_id,
                                       TagHierarchyNode& node) const {
  for (const auto& [_, entry] : tags_) {
    if (entry.parent_id == parent_id) {
      TagHierarchyNode child;
      child.name = entry.display_name;
      build_hierarchy_tree(entry.id, child);
      node.children.push_back(std::move(child));
    }
  }
  // Sort children alphabetically.
  std::sort(node.children.begin(), node.children.end(),
            [](const TagHierarchyNode& a, const TagHierarchyNode& b) {
              return a.name < b.name;
            });
}

auto TagManager::hierarchy_node_to_json(const TagHierarchyNode& node,
                                         int indent) -> std::string {
  std::ostringstream ss;
  std::string pad(indent, ' ');
  ss << pad << "{\n";
  ss << pad << "  \"name\": \"" << json_escape(node.name) << "\"";
  if (!node.children.empty()) {
    ss << ",\n";
    ss << pad << "  \"children\": [\n";
    for (size_t i = 0; i < node.children.size(); ++i) {
      ss << hierarchy_node_to_json(node.children[i], indent + 4);
      if (i + 1 < node.children.size()) ss << ",";
      ss << "\n";
    }
    ss << pad << "  ]\n";
  } else {
    ss << "\n";
  }
  ss << pad << "}";
  return ss.str();
}

auto TagManager::get_tag_hierarchy() const -> std::vector<TagHierarchyNode> {
  std::vector<TagHierarchyNode> roots;

  // Collect root-level tags (parent_id == 0).
  for (const auto& [_, entry] : tags_) {
    if (entry.parent_id == 0) {
      TagHierarchyNode node;
      node.name = entry.display_name;
      build_hierarchy_tree(entry.id, node);
      roots.push_back(std::move(node));
    }
  }

  std::sort(roots.begin(), roots.end(),
            [](const TagHierarchyNode& a, const TagHierarchyNode& b) {
              return a.name < b.name;
            });
  return roots;
}

auto TagManager::export_tag_hierarchy(const std::string& file_path) const
    -> TagPersistenceResult {
  TagPersistenceResult result;
  try {
    auto roots = get_tag_hierarchy();

    std::ostringstream ss;
    ss << "{\n  \"hierarchy\": [\n";
    for (size_t i = 0; i < roots.size(); ++i) {
      ss << hierarchy_node_to_json(roots[i], 4);
      if (i + 1 < roots.size()) ss << ",";
      ss << "\n";
    }
    ss << "  ]\n}\n";

    std::ofstream out(file_path, std::ios::binary | std::ios::trunc);
    if (!out) {
      result.error_message = "Cannot open file for writing: " + file_path;
      return result;
    }
    out << ss.str();
    result.success = true;
  } catch (const std::exception& e) {
    result.error_message = std::string("Failed to export hierarchy: ") + e.what();
  }
  return result;
}

auto TagManager::parse_hierarchy_json(const std::string& json_str) -> bool {
  // Parse the hierarchy tree and create tags.
  // We recursively walk the JSON tree and call find_or_create_tag.
  std::function<void(const std::string&, size_t)> parse_node;
  parse_node = [&](const std::string& parent_path, size_t start_pos) {
    // Find "name" field.
    size_t pos = json_str.find("\"name\"", start_pos);
    while (pos != std::string::npos) {
      pos = json_str.find(':', pos);
      if (pos == std::string::npos) break;
      ++pos;
      skip_ws(json_str, pos);
      std::string name = parse_json_string(json_str, pos);
      std::string full_path =
          parent_path.empty() ? name : parent_path + "/" + name;

      find_or_create_tag(full_path);

      // Look for "children" array.
      size_t children_pos = json_str.find("\"children\"", pos);
      if (children_pos != std::string::npos) {
        // Find the opening '['.
        size_t arr_pos = json_str.find('[', children_pos);
        if (arr_pos != std::string::npos) {
          // Find child node openings '{'.
          size_t search_pos = arr_pos + 1;
          while (search_pos < json_str.size()) {
            size_t child_open = json_str.find('{', search_pos);
            if (child_open == std::string::npos) break;
            // Make sure this { is before the closing ] for this array.
            size_t close_bracket = json_str.find(']', arr_pos + 1);
            if (close_bracket != std::string::npos && child_open > close_bracket) {
              break;
            }
            parse_node(full_path, child_open);
            // Move past this node.
            search_pos = child_open + 1;
          }
        }
      }
      break;  // Only process the first "name" after start_pos.
    }
  };

  // Find the "hierarchy" array.
  auto pos = json_str.find("\"hierarchy\"");
  if (pos == std::string::npos) return false;
  pos = json_str.find('[', pos);
  if (pos == std::string::npos) return false;
  ++pos;

  // Find each top-level node.
  while (pos < json_str.size()) {
    skip_ws(json_str, pos);
    if (json_str[pos] == ']') break;
    if (json_str[pos] == '{') {
      parse_node("", pos);
    }
    // Advance to next potential node or end of array.
    size_t next_brace = json_str.find('{', pos + 1);
    size_t close = json_str.find(']', pos + 1);
    if (close != std::string::npos &&
        (next_brace == std::string::npos || close < next_brace)) {
      break;
    }
    pos = next_brace;
  }

  return true;
}

auto TagManager::import_tag_hierarchy(const std::string& file_path)
    -> TagPersistenceResult {
  TagPersistenceResult result;
  try {
    std::ifstream in(file_path, std::ios::binary);
    if (!in) {
      result.error_message = "Cannot open file for reading: " + file_path;
      return result;
    }
    std::string content((std::istreambuf_iterator<char>(in)),
                         std::istreambuf_iterator<char>());
    if (!parse_hierarchy_json(content)) {
      result.error_message = "Failed to parse hierarchy JSON: " + file_path;
      return result;
    }
    result.success = true;
  } catch (const std::exception& e) {
    result.error_message = std::string("Failed to import hierarchy: ") + e.what();
  }
  return result;
}

// ── Persistence: XMP sidecar sync ───────────────────────────────────────────

auto TagManager::extract_xml_tag_content(const std::string& xml,
                                          const std::string& tag_name)
    -> std::vector<std::string> {
  std::vector<std::string> result;
  std::string open_tag = "<" + tag_name;
  std::string close_tag = "</" + tag_name + ">";

  size_t pos = 0;
  while (pos < xml.size()) {
    auto start = xml.find(open_tag, pos);
    if (start == std::string::npos) break;

    // Find the end of the opening tag (could be <tag> or <tag attr="..">).
    auto gt = xml.find('>', start);
    if (gt == std::string::npos) break;

    // Check if self-closing.
    if (gt > 0 && xml[gt - 1] == '/') {
      pos = gt + 1;
      continue;
    }

    auto end = xml.find(close_tag, gt + 1);
    if (end == std::string::npos) break;

    result.push_back(xml_unescape(
        xml.substr(gt + 1, end - gt - 1)));
    pos = end + close_tag.size();
  }
  return result;
}

auto TagManager::extract_xmp_bag_items(const std::string& xml,
                                        const std::string& element_name)
    -> std::vector<std::string> {
  std::vector<std::string> items;

  // Find the element (e.g. <dc:subject>), then extract <rdf:li> values.
  auto contents = extract_xml_tag_content(xml, element_name);
  for (const auto& content : contents) {
    auto li_items = extract_xml_tag_content(content, "rdf:li");
    for (auto& li : li_items) {
      // Trim whitespace.
      auto start = li.find_first_not_of(" \t\n\r");
      auto end = li.find_last_not_of(" \t\n\r");
      if (start != std::string::npos && end != std::string::npos) {
        items.push_back(li.substr(start, end - start + 1));
      }
    }
  }
  return items;
}

auto TagManager::sync_to_xmp_sidecar(const std::string& image_path,
                                      ImageId image_id) const
    -> TagPersistenceResult {
  TagPersistenceResult result;

  auto data_it = image_data_.find(image_id);
  if (data_it == image_data_.end()) {
    result.error_message = "No tag data for image";
    return result;
  }

  const auto& data = data_it->second;

  try {
    // Build the flat keywords (leaf names) and hierarchical tags.
    std::vector<std::string> flat_keywords;
    std::vector<std::string> hier_pipe;     // Lightroom: "Landscape|Beach"
    std::vector<std::string> hier_slash;    // digiKam: "Landscape/Beach"

    for (TagId tid : data.tag_ids) {
      auto tag_it = tags_.find(tid);
      if (tag_it == tags_.end()) continue;
      const auto& entry = tag_it->second;

      flat_keywords.push_back(entry.display_name);
      hier_slash.push_back(entry.full_path);

      // Convert "/" to "|" for Lightroom format.
      std::string pipe_path = entry.full_path;
      std::replace(pipe_path.begin(), pipe_path.end(), '/', '|');
      hier_pipe.push_back(pipe_path);
    }

    // Sort for deterministic output.
    std::sort(flat_keywords.begin(), flat_keywords.end());
    std::sort(hier_pipe.begin(), hier_pipe.end());
    std::sort(hier_slash.begin(), hier_slash.end());

    // Build XMP content.
    std::ostringstream ss;
    ss << "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    ss << "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\" x:xmptk=\"AlcedoStudio\">\n";
    ss << " <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n";
    ss << "          xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n";
    ss << "          xmlns:xmp=\"http://ns.adobe.com/xap/1.0/\"\n";
    ss << "          xmlns:lr=\"http://ns.adobe.com/lightroom/1.0/\"\n";
    ss << "          xmlns:digiKam=\"http://www.digikam.org/ns/1.0/\">\n";
    ss << "  <rdf:Description rdf:about=\"\">\n";

    // dc:subject — flat keywords.
    if (!flat_keywords.empty()) {
      ss << "   <dc:subject>\n    <rdf:Bag>\n";
      for (const auto& kw : flat_keywords) {
        ss << "     <rdf:li>" << xml_escape(kw) << "</rdf:li>\n";
      }
      ss << "    </rdf:Bag>\n   </dc:subject>\n";
    }

    // lr:hierarchicalSubject — Lightroom pipe-separated.
    if (!hier_pipe.empty()) {
      ss << "   <lr:hierarchicalSubject>\n    <rdf:Bag>\n";
      for (const auto& hp : hier_pipe) {
        ss << "     <rdf:li>" << xml_escape(hp) << "</rdf:li>\n";
      }
      ss << "    </rdf:Bag>\n   </lr:hierarchicalSubject>\n";
    }

    // digiKam:TagsList — slash-separated.
    if (!hier_slash.empty()) {
      ss << "   <digiKam:TagsList>\n    <rdf:Bag>\n";
      for (const auto& hs : hier_slash) {
        ss << "     <rdf:li>" << xml_escape(hs) << "</rdf:li>\n";
      }
      ss << "    </rdf:Bag>\n   </digiKam:TagsList>\n";
    }

    // xmp:Rating.
    if (data.rating.steps() > 0) {
      ss << "   <xmp:Rating>" << data.rating.stars() << "</xmp:Rating>\n";
    }

    // xmp:Label — color label mapped to Lightroom label names.
    if (data.color_label != ColorLabel::None) {
      ss << "   <xmp:Label>" << xml_escape(std::string(color_label_to_string(data.color_label)))
         << "</xmp:Label>\n";
    }

    ss << "  </rdf:Description>\n";
    ss << " </rdf:RDF>\n";
    ss << "</x:xmpmeta>\n";

    // Determine sidecar path.
    std::filesystem::path img_p(image_path);
    std::filesystem::path xmp_path = img_p;
    xmp_path.replace_extension(".xmp");

    std::ofstream out(xmp_path, std::ios::binary | std::ios::trunc);
    if (!out) {
      result.error_message = "Cannot write XMP sidecar: " + xmp_path.string();
      return result;
    }
    out << ss.str();
    result.success = true;
  } catch (const std::exception& e) {
    result.error_message = std::string("XMP sync failed: ") + e.what();
  }
  return result;
}

auto TagManager::sync_from_xmp_sidecar(const std::string& image_path,
                                        ImageId image_id)
    -> TagPersistenceResult {
  TagPersistenceResult result;

  try {
    std::filesystem::path img_p(image_path);
    std::filesystem::path xmp_path = img_p;
    xmp_path.replace_extension(".xmp");

    std::ifstream in(xmp_path, std::ios::binary);
    if (!in) {
      result.error_message = "Cannot read XMP sidecar: " + xmp_path.string();
      return result;
    }
    std::string content((std::istreambuf_iterator<char>(in)),
                         std::istreambuf_iterator<char>());

    // Extract tags from digiKam:TagsList (preferred — hierarchical with "/").
    auto digikam_tags = extract_xmp_bag_items(content, "digiKam:TagsList");

    // Also try lr:hierarchicalSubject (pipe-separated).
    auto lr_tags = extract_xmp_bag_items(content, "lr:hierarchicalSubject");

    // Also try dc:subject (flat keywords).
    auto dc_tags = extract_xmp_bag_items(content, "dc:subject");

    // Process digiKam tags first (they have hierarchy info).
    for (const auto& tag_path : digikam_tags) {
      if (!tag_path.empty()) {
        add_tag(image_id, tag_path);
      }
    }

    // If no digiKam tags, process lr tags (convert "|" to "/").
    if (digikam_tags.empty()) {
      for (auto tag_path : lr_tags) {
        std::replace(tag_path.begin(), tag_path.end(), '|', '/');
        if (!tag_path.empty()) {
          add_tag(image_id, tag_path);
        }
      }
    }

    // If still no tags, add flat dc:subject keywords as root-level tags.
    if (digikam_tags.empty() && lr_tags.empty()) {
      for (const auto& kw : dc_tags) {
        if (!kw.empty()) {
          add_tag(image_id, kw);
        }
      }
    }

    // Extract rating.
    auto rating_vals = extract_xml_tag_content(content, "xmp:Rating");
    if (!rating_vals.empty()) {
      try {
        double stars = std::stod(rating_vals[0]);
        set_rating(image_id, StarRating(stars));
      } catch (...) {}
    }

    // Extract color label.
    auto label_vals = extract_xml_tag_content(content, "xmp:Label");
    if (!label_vals.empty()) {
      set_color_label(image_id, color_label_from_string(label_vals[0]));
    }

    result.success = true;
  } catch (const std::exception& e) {
    result.error_message = std::string("XMP read failed: ") + e.what();
  }
  return result;
}

// ── Image data management ───────────────────────────────────────────────────

void TagManager::remove_image(ImageId image_id) {
  auto it = image_data_.find(image_id);
  if (it == image_data_.end()) return;

  // Remove from tag-to-image reverse lookup and decrement counts.
  for (TagId tid : it->second.tag_ids) {
    auto rev_it = tag_to_images_.find(tid);
    if (rev_it != tag_to_images_.end()) {
      rev_it->second.erase(image_id);
    }
    // Decrement ancestor counts.
    TagId current = tid;
    while (current != 0) {
      auto tag_it = tags_.find(current);
      if (tag_it != tags_.end()) {
        if (tag_it->second.image_count > 0) {
          --tag_it->second.image_count;
        }
        current = tag_it->second.parent_id;
      } else {
        break;
      }
    }
  }

  // Remove from color index.
  if (it->second.color_label != ColorLabel::None) {
    color_index_[it->second.color_label].erase(image_id);
  }

  image_data_.erase(it);
}

auto TagManager::has_image_data(ImageId image_id) const -> bool {
  return image_data_.find(image_id) != image_data_.end();
}

auto TagManager::image_count() const -> size_t {
  return image_data_.size();
}

}  // namespace alcedo::app
