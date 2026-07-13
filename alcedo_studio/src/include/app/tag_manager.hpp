//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <map>
#include <optional>
#include <set>
#include <string>
#include <string_view>
#include <unordered_map>
#include <unordered_set>
#include <vector>

namespace alcedo::app {

// ── Color label system ──────────────────────────────────────────────────────

enum class ColorLabel : uint8_t {
  None   = 0,
  Red    = 1,
  Orange = 2,
  Yellow = 3,
  Green  = 4,
  Blue   = 5,
  Purple = 6,
};

/// Convert ColorLabel to a human-readable name.
auto color_label_to_string(ColorLabel label) -> std::string_view;

/// Parse a ColorLabel from its string name (case-insensitive).
auto color_label_from_string(std::string_view name) -> ColorLabel;

// ── Star rating ─────────────────────────────────────────────────────────────

/// Star rating with half-star support. Internally stored as an integer
/// representing 0-10 steps (0.0 to 5.0 in 0.5 increments).
class StarRating {
 public:
  constexpr StarRating() = default;
  constexpr explicit StarRating(double stars) : value_(clamp(stars)) {}

  /// Construct from raw step count (0-10).
  static constexpr auto from_steps(int steps) -> StarRating {
    StarRating r;
    r.value_ = static_cast<uint8_t>(steps > 10 ? 10 : (steps < 0 ? 0 : steps));
    return r;
  }

  /// Get the rating as a double (0.0-5.0).
  constexpr auto stars() const -> double { return value_ / 2.0; }

  /// Get the raw step count (0-10).
  constexpr auto steps() const -> int { return value_; }

  constexpr auto operator==(const StarRating& o) const -> bool {
    return value_ == o.value_;
  }
  constexpr auto operator!=(const StarRating& o) const -> bool {
    return value_ != o.value_;
  }
  constexpr auto operator<(const StarRating& o) const -> bool {
    return value_ < o.value_;
  }
  constexpr auto operator<=(const StarRating& o) const -> bool {
    return value_ <= o.value_;
  }
  constexpr auto operator>(const StarRating& o) const -> bool {
    return value_ > o.value_;
  }
  constexpr auto operator>=(const StarRating& o) const -> bool {
    return value_ >= o.value_;
  }

 private:
  uint8_t value_ = 0;

  static constexpr auto clamp(double v) -> uint8_t {
    auto steps = static_cast<int>(v * 2.0 + 0.25);
    if (steps < 0) return 0;
    if (steps > 10) return 10;
    return static_cast<uint8_t>(steps);
  }
};

// ── Tag / keyword types ─────────────────────────────────────────────────────

using ImageId = uint32_t;
using TagId   = uint32_t;

/// A single tag entry in the central registry.
struct TagEntry {
  TagId        id = 0;          ///< Unique identifier
  std::string  full_path;       ///< Full hierarchical path, e.g. "Landscape/Beach"
  std::string  display_name;    ///< Leaf name, e.g. "Beach"
  TagId        parent_id = 0;   ///< 0 means root-level tag
  uint32_t     image_count = 0; ///< Number of images carrying this tag
};

/// Result of a tag search or auto-complete query.
struct TagSuggestion {
  std::string full_path;
  int         relevance = 0;    ///< Higher = more relevant
};

/// Per-image tag data (owned by TagManager).
struct ImageTagData {
  ColorLabel                     color_label = ColorLabel::None;
  StarRating                     rating;
  std::unordered_set<TagId>      tag_ids;
};

// ── Persistence result types ────────────────────────────────────────────────

struct TagPersistenceResult {
  bool        success  = false;
  std::string error_message;
};

struct TagHierarchyNode {
  std::string                          name;
  std::vector<TagHierarchyNode>        children;
};

// ── TagManager ──────────────────────────────────────────────────────────────

/// Central tag registry with image-to-tag mapping, tag-to-image reverse
/// lookup, color labels, star ratings, persistence, and XMP sidecar sync.
class TagManager {
 public:
  TagManager()  = default;
  ~TagManager() = default;

  TagManager(const TagManager&)            = delete;
  TagManager& operator=(const TagManager&) = delete;

  // ── Color labels ────────────────────────────────────────────────────────

  /// Set the color label for an image. Creates the image entry if needed.
  void set_color_label(ImageId image_id, ColorLabel label);

  /// Get the color label for an image. Returns ColorLabel::None if unknown.
  auto get_color_label(ImageId image_id) const -> ColorLabel;

  /// Clear the color label (set to None) for an image.
  void clear_color_label(ImageId image_id);

  /// Get all images with the specified color label.
  auto get_images_by_color(ColorLabel label) const -> std::vector<ImageId>;

  // ── Star rating ─────────────────────────────────────────────────────────

  /// Set the star rating for an image.
  void set_rating(ImageId image_id, StarRating rating);

  /// Get the star rating for an image. Returns 0-star if unknown.
  auto get_rating(ImageId image_id) const -> StarRating;

  /// Get all images whose rating falls within [min_rating, max_rating].
  auto get_images_by_rating_range(StarRating min_rating,
                                  StarRating max_rating) const
      -> std::vector<ImageId>;

  // ── Tag / keyword management ────────────────────────────────────────────

  /// Add a tag to an image. Creates the tag in the registry if it does not
  /// exist (including all intermediate parent tags).  Returns the tag ID.
  auto add_tag(ImageId image_id, const std::string& tag_path) -> TagId;

  /// Remove a tag from an image. Returns true if the tag was present.
  auto remove_tag(ImageId image_id, const std::string& tag_path) -> bool;

  /// Remove a tag from an image by TagId. Returns true if the tag was present.
  auto remove_tag_by_id(ImageId image_id, TagId tag_id) -> bool;

  /// Get all tags for an image.
  auto get_tags_for_image(ImageId image_id) const -> std::vector<TagEntry>;

  /// Get all images that carry the given tag.
  auto get_images_for_tag(TagId tag_id) const -> std::vector<ImageId>;

  /// Get all images that carry the given tag path.
  auto get_images_for_tag_path(const std::string& tag_path) const
      -> std::vector<ImageId>;

  /// Check whether a tag exists in the registry.
  auto tag_exists(const std::string& tag_path) const -> bool;

  /// Look up a tag by its full path. Returns nullptr if not found.
  auto find_tag(const std::string& tag_path) const -> const TagEntry*;

  /// Look up a tag by its ID. Returns nullptr if not found.
  auto find_tag_by_id(TagId id) const -> const TagEntry*;

  /// Get all tags in the registry.
  auto get_all_tags() const -> std::vector<TagEntry>;

  /// Get the number of distinct tags in the registry.
  auto tag_count() const -> size_t;

  /// Remove a tag from the registry entirely (also removes it from all images).
  void delete_tag(TagId tag_id);

  // ── Bulk operations ─────────────────────────────────────────────────────

  /// Add a tag to multiple images at once. Returns the tag ID.
  auto bulk_add_tag(const std::vector<ImageId>& image_ids,
                    const std::string& tag_path) -> TagId;

  /// Remove a tag from multiple images at once.
  void bulk_remove_tag(const std::vector<ImageId>& image_ids,
                       const std::string& tag_path);

  // ── Auto-complete & search ──────────────────────────────────────────────

  /// Auto-complete a partial tag path. Returns matching tags sorted by
  /// relevance (exact prefix match first, then by image count descending).
  auto autocomplete(const std::string& prefix,
                    size_t max_results = 10) const
      -> std::vector<TagSuggestion>;

  /// Search tags with fuzzy matching (Levenshtein distance). Returns tags
  /// whose edit distance to `query` is within `max_distance`, sorted by
  /// relevance.
  auto fuzzy_search(const std::string& query,
                    int max_distance = 2,
                    size_t max_results = 10) const
      -> std::vector<TagSuggestion>;

  // ── Tag statistics ──────────────────────────────────────────────────────

  /// Get the number of images carrying a specific tag.
  auto get_tag_image_count(TagId tag_id) const -> uint32_t;

  /// Get tag statistics: all tags with their image counts.
  auto get_tag_statistics() const -> std::vector<std::pair<TagEntry, uint32_t>>;

  // ── Tag suggestion ──────────────────────────────────────────────────────

  /// Suggest tags for an image based on tags already present. Returns tags
  /// that frequently co-occur with the image's existing tags.
  auto suggest_tags_for_image(ImageId image_id,
                              size_t max_suggestions = 5) const
      -> std::vector<TagSuggestion>;

  // ── Persistence: JSON ───────────────────────────────────────────────────

  /// Save all tag data (registry, image mappings, labels, ratings) to a
  /// JSON file at the given path.
  auto save_to_json(const std::string& file_path) const -> TagPersistenceResult;

  /// Load tag data from a JSON file, replacing all current state.
  auto load_from_json(const std::string& file_path) -> TagPersistenceResult;

  // ── Persistence: tag hierarchy export/import ────────────────────────────

  /// Export the tag hierarchy (no image associations) as a JSON tree.
  auto export_tag_hierarchy(const std::string& file_path) const
      -> TagPersistenceResult;

  /// Import a tag hierarchy from a JSON tree, merging with existing tags.
  auto import_tag_hierarchy(const std::string& file_path)
      -> TagPersistenceResult;

  /// Get the tag hierarchy as a tree structure.
  auto get_tag_hierarchy() const -> std::vector<TagHierarchyNode>;

  // ── Persistence: XMP sidecar sync ───────────────────────────────────────

  /// Sync tag data to an XMP sidecar file. Writes dc:subject (flat keywords),
  /// lr:hierarchicalSubject (pipe-separated), and digiKam:TagsList
  /// (slash-separated) for compatibility with Lightroom and digiKam.
  auto sync_to_xmp_sidecar(const std::string& image_path,
                            ImageId image_id) const -> TagPersistenceResult;

  /// Read tag data from an XMP sidecar file and apply it to the given image.
  auto sync_from_xmp_sidecar(const std::string& image_path,
                              ImageId image_id) -> TagPersistenceResult;

  // ── Image data management ───────────────────────────────────────────────

  /// Remove all tag data for an image.
  void remove_image(ImageId image_id);

  /// Check whether an image has any tag data.
  auto has_image_data(ImageId image_id) const -> bool;

  /// Get the number of images with tag data.
  auto image_count() const -> size_t;

 private:
  // ── Internal helpers ────────────────────────────────────────────────────

  /// Ensure the image entry exists in the data map.
  auto ensure_image(ImageId image_id) -> ImageTagData&;

  /// Find or create a tag (and all its parents) by full path.
  /// Returns the TagId of the leaf tag.
  auto find_or_create_tag(const std::string& tag_path) -> TagId;

  /// Extract the leaf name from a full tag path.
  static auto extract_leaf_name(const std::string& tag_path) -> std::string;

  /// Extract the parent path from a full tag path. Returns "" for root tags.
  static auto extract_parent_path(const std::string& tag_path) -> std::string;

  /// Compute Levenshtein edit distance between two strings.
  static auto levenshtein_distance(std::string_view a,
                                    std::string_view b) -> int;

  /// Build the hierarchy tree recursively.
  void build_hierarchy_tree(TagId parent_id,
                            TagHierarchyNode& node) const;

  /// Write a TagHierarchyNode subtree as JSON.
  static auto hierarchy_node_to_json(const TagHierarchyNode& node,
                                     int indent) -> std::string;

  /// Parse a JSON string and populate tag registry (for import_tag_hierarchy).
  auto parse_hierarchy_json(const std::string& json_str) -> bool;

  /// Write all state as a JSON string.
  auto state_to_json_string() const -> std::string;

  /// Parse a JSON string and restore state.
  auto state_from_json_string(const std::string& json_str) -> bool;

  /// Extract text between XML tags.
  static auto extract_xml_tag_content(const std::string& xml,
                                      const std::string& tag_name)
      -> std::vector<std::string>;

  /// Extract items from an XMP rdf:Bag/rdf:Seq element.
  static auto extract_xmp_bag_items(const std::string& xml,
                                    const std::string& element_name)
      -> std::vector<std::string>;

  // ── Data members ────────────────────────────────────────────────────────

  /// Tag registry: TagId -> TagEntry.
  std::unordered_map<TagId, TagEntry> tags_;

  /// Reverse lookup: full_path -> TagId.
  std::unordered_map<std::string, TagId> path_to_id_;

  /// Image data: ImageId -> per-image tag data.
  std::unordered_map<ImageId, ImageTagData> image_data_;

  /// Tag-to-image reverse lookup: TagId -> set of ImageId.
  std::unordered_map<TagId, std::unordered_set<ImageId>> tag_to_images_;

  /// Color-label-to-images index.
  std::map<ColorLabel, std::unordered_set<ImageId>> color_index_;

  /// Next available tag ID.
  TagId next_tag_id_ = 1;
};

}  // namespace alcedo::app
