//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <cstdint>
#include <filesystem>
#include <map>
#include <optional>
#include <string>
#include <string_view>
#include <vector>

namespace alcedo::app {

// ─────────────────────────────────────────────────────────────────────
// (a) Batch rename with template patterns
// ─────────────────────────────────────────────────────────────────────

/// Metadata context used for resolving rename template tokens.
struct RenameContext {
  std::string date;         ///< {date}   – YYYY-MM-DD
  std::string time;         ///< {time}   – HH-MM-SS
  std::string camera;       ///< {camera} – make + model
  std::string lens;         ///< {lens}   – lens name
  uint64_t    iso         = 0;
  float       aperture    = 0.0f;
  std::string shutter;     ///< e.g. "1/250"
  float       focal       = 0.0f;
  int         rating      = 0;
  std::string original;   ///< original filename stem (without extension)
};

/// Configuration for a batch rename operation.
struct RenameConfig {
  std::string pattern          = "{date}_{original}";  ///< template pattern
  int         sequence_start   = 1;       ///< starting index for {index}
  int         sequence_padding = 3;       ///< e.g. 3 → "001", or format like "03"
  bool        keep_extension   = true;    ///< preserve original file extension
};

/// A single rename mapping (old → new), produced by preview or apply.
struct RenameEntry {
  std::filesystem::path old_path;
  std::filesystem::path new_path;
  bool                  success = false;
  std::string           error;
};

/// Result of a batch rename operation, including undo information.
struct RenameResult {
  std::vector<RenameEntry>              entries;
  /// Undo map: new_path → old_path for all successfully renamed files.
  std::map<std::filesystem::path, std::filesystem::path> undo_map;
  size_t                                succeeded = 0;
  size_t                                failed    = 0;
};

/// Resolve a rename template pattern against a single context.
/// Supports tokens: {date}, {time}, {camera}, {lens}, {iso}, {aperture},
/// {shutter}, {focal}, {index:NN}, {rating}, {original}.
/// {index:NN} uses NN as the zero-padded width (e.g. {index:03} → "001").
///
/// @param pattern   The template string with tokens.
/// @param ctx       Metadata context for the file.
/// @param index     The sequence number for this file.
/// @return The resolved filename stem (no extension).
auto ResolveRenamePattern(const std::string& pattern,
                          const RenameContext& ctx,
                          int index) -> std::string;

/// Preview a batch rename without actually renaming files.
/// Returns the full list of RenameEntry with old_path and new_path filled,
/// but success remains false and no files are touched.
auto PreviewBatchRename(const std::vector<std::filesystem::path>& paths,
                        const std::vector<RenameContext>& contexts,
                        const RenameConfig& config) -> std::vector<RenameEntry>;

/// Execute a batch rename operation.
/// Returns a RenameResult with undo information that can be passed to
/// UndoBatchRename().
auto ApplyBatchRename(const std::vector<std::filesystem::path>& paths,
                      const std::vector<RenameContext>& contexts,
                      const RenameConfig& config) -> RenameResult;

/// Undo a previous batch rename using the undo_map from RenameResult.
/// Returns a new RenameResult for the undo operation itself.
auto UndoBatchRename(
    const std::map<std::filesystem::path, std::filesystem::path>& undo_map)
    -> RenameResult;

// ─────────────────────────────────────────────────────────────────────
// (b) File operations (with sidecar support)
// ─────────────────────────────────────────────────────────────────────

/// Copy an image file together with its sidecar files (.xmp, .alcd).
/// Sidecar files are looked up in the same directory as the source image.
///
/// @param src  Source image path.
/// @param dst  Destination image path.
/// @throws std::runtime_error on I/O failure.
void copy_file_with_sidecar(const std::filesystem::path& src,
                            const std::filesystem::path& dst);

/// Move an image file together with its sidecar files (.xmp, .alcd).
///
/// @param src  Source image path.
/// @param dst  Destination image path.
/// @throws std::runtime_error on I/O failure.
void move_file_with_sidecar(const std::filesystem::path& src,
                            const std::filesystem::path& dst);

/// Move a file to the platform recycle bin (macOS/Windows) or permanently
/// delete it on Linux. Sidecar files (.xmp, .alcd) are also removed.
///
/// @param path  Path to the image file.
/// @throws std::runtime_error on failure.
void delete_file_to_trash(const std::filesystem::path& path);

/// Create a duplicate of an image (and its sidecar files) in the same
/// directory, appending "_copy" before the extension.
///
/// @param src  Source image path.
/// @return Path to the newly created duplicate.
/// @throws std::runtime_error on I/O failure.
auto duplicate_with_sidecar(const std::filesystem::path& src)
    -> std::filesystem::path;

// ─────────────────────────────────────────────────────────────────────
// (c) Folder operations
// ─────────────────────────────────────────────────────────────────────

/// Create a new folder at the specified path (including any missing parents).
///
/// @param path  Folder path to create.
/// @throws std::runtime_error if the folder cannot be created.
void create_folder(const std::filesystem::path& path);

/// Rename an existing folder.
///
/// @param old_path  Current folder path.
/// @param new_path  New folder path.
/// @throws std::runtime_error on failure.
void rename_folder(const std::filesystem::path& old_path,
                   const std::filesystem::path& new_path);

/// Delete a folder and all its contents.
///
/// @param path  Folder path to delete.
/// @throws std::runtime_error on failure.
void delete_folder(const std::filesystem::path& path);

/// Folder size and image count estimation result.
struct FolderStats {
  uint64_t total_bytes  = 0;
  size_t   image_count  = 0;
  size_t   total_files  = 0;
};

/// Compute the total size, file count, and image count of a folder.
/// Image files are identified by common image extensions.
///
/// @param path  Folder path to scan.
/// @return FolderStats with size and counts.
/// @throws std::runtime_error if the folder cannot be accessed.
auto estimate_folder_stats(const std::filesystem::path& path) -> FolderStats;

// ─────────────────────────────────────────────────────────────────────
// (d) Virtual copy management
// ─────────────────────────────────────────────────────────────────────

/// A virtual copy references the original image file but has its own
/// sidecar metadata (adjustments, rating, labels, etc.). It is stored
/// as an additional .alcd sidecar with a virtual-copy suffix.
///
/// Virtual copy naming convention:
///   original:  IMG_001.CR3
///   sidecar:   IMG_001.alcd
///   virtual:   IMG_001_v2.alcd  (second virtual copy)
///               IMG_001_v3.alcd  (third virtual copy), etc.

/// Information about a single virtual copy.
struct VirtualCopyInfo {
  std::filesystem::path sidecar_path;  ///< .alcd sidecar for this virtual copy
  int                   copy_number = 0;
};

/// Create a virtual copy of an image by creating a new .alcd sidecar
/// with the next available virtual copy number.
///
/// @param image_path  Path to the original image file.
/// @return VirtualCopyInfo for the newly created virtual copy.
/// @throws std::runtime_error if the sidecar cannot be created.
auto create_virtual_copy(const std::filesystem::path& image_path)
    -> VirtualCopyInfo;

/// List all virtual copies of an image file.
///
/// @param image_path  Path to the original image file.
/// @return Vector of VirtualCopyInfo, one per virtual copy.
auto list_virtual_copies(const std::filesystem::path& image_path)
    -> std::vector<VirtualCopyInfo>;

/// Resolve a virtual copy sidecar path to the actual image file path.
///
/// @param virtual_sidecar_path  Path to a virtual copy .alcd sidecar.
/// @return Path to the actual image file, or nullopt if it cannot be resolved.
auto resolve_virtual_path(const std::filesystem::path& virtual_sidecar_path)
    -> std::optional<std::filesystem::path>;

// ─────────────────────────────────────────────────────────────────────
// (e) File utilities
// ─────────────────────────────────────────────────────────────────────

/// Get the file size in bytes.
///
/// @param path  File path.
/// @return File size in bytes, or 0 if the file does not exist.
auto get_file_size(const std::filesystem::path& path) -> uint64_t;

/// Export format for size estimation.
enum class ExportFormat : uint8_t {
  JPEG = 0,
  PNG,
  TIFF,
  JXL,
  AVIF,
  WEBP,
};

/// Estimate the exported file size based on format, compression quality,
/// and image dimensions.
///
/// @param format      Target export format.
/// @param quality     Compression quality 0–100 (format-dependent).
/// @param width       Image width in pixels.
/// @param height      Image height in pixels.
/// @param bit_depth   Bits per channel (8, 10, 12, 14, 16).
/// @return Estimated file size in bytes.
auto estimate_export_size(ExportFormat format,
                          int quality,
                          int width,
                          int height,
                          int bit_depth = 8) -> uint64_t;

/// Set file modification/access times from an EXIF date string.
/// The EXIF date format is "YYYY:MM:DD HH:MM:SS".
///
/// @param path         File path to update timestamps on.
/// @param exif_date    EXIF date/time string.
/// @throws std::invalid_argument if the date string is malformed.
/// @throws std::runtime_error if the timestamps cannot be set.
void preserve_timestamps(const std::filesystem::path& path,
                         const std::string& exif_date);

}  // namespace alcedo::app
