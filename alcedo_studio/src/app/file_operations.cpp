//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/file_operations.hpp"

#include <algorithm>
#include <chrono>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <fstream>
#include <iomanip>
#include <regex>
#include <set>
#include <sstream>
#include <stdexcept>
#include <system_error>

#if defined(__linux__) || defined(__unix__)
#include <utime.h>
#endif

namespace alcedo::app {

namespace {

// ── Sidecar helpers ─────────────────────────────────────────────────────────

/// Return the set of sidecar extensions that should accompany an image file.
auto SidecarExtensions() -> const std::vector<std::string>& {
  static const std::vector<std::string> exts = {".xmp", ".alcd"};
  return exts;
}

/// Collect all sidecar file paths for a given image path.
auto CollectSidecarPaths(const std::filesystem::path& image_path)
    -> std::vector<std::filesystem::path> {
  std::vector<std::filesystem::path> sidecars;
  auto stem     = image_path.stem();
  auto parent   = image_path.parent_path();

  for (const auto& ext : SidecarExtensions()) {
    auto sidecar = parent / (stem.string() + ext);
    if (std::filesystem::exists(sidecar)) {
      sidecars.push_back(std::move(sidecar));
    }
  }
  return sidecars;
}

/// Build the destination sidecar path for a given destination image path
/// and a source sidecar extension.
auto DstSidecarPath(const std::filesystem::path& dst_image,
                    const std::string& sidecar_ext) -> std::filesystem::path {
  auto stem   = dst_image.stem();
  auto parent = dst_image.parent_path();
  return parent / (stem.string() + sidecar_ext);
}

// ── Image extension detection ───────────────────────────────────────────────

auto IsImageExtension(const std::string& ext) -> bool {
  static const std::set<std::string> image_exts = {
      ".jpg", ".jpeg", ".png", ".tif", ".tiff", ".bmp", ".gif", ".webp",
      ".heic", ".heif", ".avif", ".jxl", ".exr",
      // RAW formats
      ".cr2", ".cr3", ".nef", ".arw", ".dng", ".raf", ".orf", ".rw2",
      ".pef", ".srw", ".crw", ".3fr", ".ari", ".srf", ".sr2", ".bay",
      ".cri", ".cap", ".iiq", ".eip", ".dcs", ".dcr", ".drf", ".k25",
      ".kdc", ".mdc", ".mef", ".mos", ".mrw", ".nrw", ".obm", ".ptx",
      ".pxn", ".r3d", ".raw", ".rwl", ".rwz", ".x3f",
  };
  std::string lower = ext;
  std::transform(lower.begin(), lower.end(), lower.begin(),
                 [](char c) { return static_cast<char>(std::tolower(c)); });
  if (lower.size() > 0 && lower[0] != '.') {
    lower = "." + lower;
  }
  return image_exts.count(lower) > 0;
}

// ── String / formatting helpers ─────────────────────────────────────────────

/// Format an integer with zero-padding.
auto FormatPadded(int value, int width) -> std::string {
  std::ostringstream ss;
  ss << std::setw(width) << std::setfill('0') << value;
  return ss.str();
}

/// Format a float value to a string with fixed precision.
auto FormatFloat(float value, int precision = 1) -> std::string {
  std::ostringstream ss;
  ss << std::fixed << std::setprecision(precision) << value;
  return ss.str();
}

// ── Virtual copy path helpers ───────────────────────────────────────────────

/// Build the virtual copy sidecar path for a given image stem and copy number.
auto VirtualSidecarPath(const std::filesystem::path& image_path,
                        int copy_number) -> std::filesystem::path {
  // IMG_001.CR3 → IMG_001_v2.alcd
  auto stem   = image_path.stem().string();
  auto parent = image_path.parent_path();
  return parent / (stem + "_v" + std::to_string(copy_number) + ".alcd");
}

/// Extract the base stem and copy number from a virtual copy sidecar filename.
/// Returns {base_stem, copy_number} or nullopt if not a virtual copy sidecar.
auto ParseVirtualSidecarName(const std::filesystem::path& sidecar_path)
    -> std::optional<std::pair<std::string, int>> {
  auto filename = sidecar_path.filename().string();
  if (filename.size() < 7) return std::nullopt;  // minimum: "a_v2.alcd"

  // Must end with .alcd
  if (filename.size() < 5 ||
      filename.compare(filename.size() - 5, 5, ".alcd") != 0) {
    return std::nullopt;
  }

  // Find the "_v" suffix pattern before .alcd
  auto base = filename.substr(0, filename.size() - 5);  // strip .alcd
  auto pos  = base.rfind("_v");
  if (pos == std::string::npos || pos + 2 >= base.size()) return std::nullopt;

  auto suffix = base.substr(pos + 2);
  if (suffix.empty()) return std::nullopt;

  // Verify suffix is all digits
  for (char c : suffix) {
    if (!std::isdigit(static_cast<unsigned char>(c))) return std::nullopt;
  }

  int copy_number = std::stoi(suffix);
  std::string stem = base.substr(0, pos);
  return std::make_pair(stem, copy_number);
}

// ── Platform trash helper ───────────────────────────────────────────────────

/// Move a file to the system trash or permanently delete it.
auto MoveToTrash(const std::filesystem::path& path) -> bool {
  std::error_code ec;

#if defined(__APPLE__)
  // macOS: use NSWorkspace recycle or 'osascript'
  auto cmd = "osascript -e 'tell application \"Finder\" to move POSIX file \"" +
             path.string() + "\" to trash'";
  return std::system(cmd.c_str()) == 0;
#elif defined(_WIN32)
  // Windows: use SHFileOperationW via COM
  // Fallback: permanent delete
  return std::filesystem::remove(path, ec);
#else
  // Linux: no standard trash API; permanent delete
  return std::filesystem::remove(path, ec);
#endif
}

}  // namespace

// ============================================================================
// (a) Batch rename with template patterns
// ============================================================================

auto ResolveRenamePattern(const std::string& pattern,
                          const RenameContext& ctx,
                          int index) -> std::string {
  std::string result = pattern;

  // Handle {index:NN} with configurable padding width
  // Match {index} or {index:NN} where NN is a digit count
  {
    std::regex index_re(R"(\{index(?::(\d+))?\})");
    std::string replaced;
    replaced.reserve(result.size());
    auto it  = result.cbegin();
    auto end = result.cend();
    std::smatch m;
    while (std::regex_search(it, end, m, index_re)) {
      replaced.append(it, m[0].first);
      int pad = m[1].matched ? std::stoi(m[1].str()) : 3;
      replaced += FormatPadded(index, pad);
      it = m[0].second;
    }
    replaced.append(it, end);
    result = std::move(replaced);
  }

  // Simple token replacements
  auto replace_token = [&](const std::string& token,
                          const std::string& value) {
    auto pos = result.find(token);
    while (pos != std::string::npos) {
      result.replace(pos, token.size(), value);
      pos = result.find(token, pos + value.size());
    }
  };

  replace_token("{date}",      ctx.date);
  replace_token("{time}",      ctx.time);
  replace_token("{camera}",    ctx.camera);
  replace_token("{lens}",      ctx.lens);
  replace_token("{iso}",       ctx.iso > 0 ? std::to_string(ctx.iso) : "");
  replace_token("{aperture}",  ctx.aperture > 0.0f
                                    ? ("f" + FormatFloat(ctx.aperture))
                                    : "");
  replace_token("{shutter}",   ctx.shutter);
  replace_token("{focal}",     ctx.focal > 0.0f
                                    ? (FormatFloat(ctx.focal, 0) + "mm")
                                    : "");
  replace_token("{rating}",
                ctx.rating > 0 ? std::to_string(ctx.rating) : "0");
  replace_token("{original}",  ctx.original);

  // Sanitize: replace characters unsafe for filenames
  std::string safe;
  safe.reserve(result.size());
  for (char c : result) {
    if (c == '/' || c == '\\' || c == ':' || c == '*' || c == '?' ||
        c == '"' || c == '<' || c == '>' || c == '|') {
      safe += '_';
    } else {
      safe += c;
    }
  }

  return safe;
}

auto PreviewBatchRename(const std::vector<std::filesystem::path>& paths,
                        const std::vector<RenameContext>& contexts,
                        const RenameConfig& config)
    -> std::vector<RenameEntry> {
  if (paths.size() != contexts.size()) {
    throw std::invalid_argument(
        "PreviewBatchRename: paths and contexts must have the same size");
  }

  std::vector<RenameEntry> entries;
  entries.reserve(paths.size());

  for (size_t i = 0; i < paths.size(); ++i) {
    RenameEntry entry;
    entry.old_path = paths[i];

    int seq = config.sequence_start + static_cast<int>(i);
    auto stem = ResolveRenamePattern(config.pattern, contexts[i], seq);

    auto new_path = paths[i].parent_path() / stem;
    if (config.keep_extension) {
      new_path += paths[i].extension();
    }

    entry.new_path = std::move(new_path);
    entries.push_back(std::move(entry));
  }

  return entries;
}

auto ApplyBatchRename(const std::vector<std::filesystem::path>& paths,
                      const std::vector<RenameContext>& contexts,
                      const RenameConfig& config) -> RenameResult {
  auto entries = PreviewBatchRename(paths, contexts, config);

  RenameResult result;
  result.entries.reserve(entries.size());

  for (auto& entry : entries) {
    std::error_code ec;

    // Check for name collision
    if (std::filesystem::exists(entry.new_path, ec) && entry.old_path != entry.new_path) {
      entry.error = "Target already exists: " + entry.new_path.string();
      result.entries.push_back(std::move(entry));
      ++result.failed;
      continue;
    }

    // Rename the image file
    std::filesystem::rename(entry.old_path, entry.new_path, ec);
    if (ec) {
      entry.error = "Rename failed: " + ec.message();
      result.entries.push_back(std::move(entry));
      ++result.failed;
      continue;
    }

    // Also rename sidecar files
    auto sidecars = CollectSidecarPaths(entry.old_path);
    for (const auto& sc : sidecars) {
      auto ext = sc.extension().string();
      auto new_sc = DstSidecarPath(entry.new_path, ext);
      std::filesystem::rename(sc, new_sc, ec);
      // Sidecar rename failure is non-fatal; continue with other sidecars.
    }

    entry.success = true;
    result.undo_map[entry.new_path] = entry.old_path;
    result.entries.push_back(std::move(entry));
    ++result.succeeded;
  }

  return result;
}

auto UndoBatchRename(
    const std::map<std::filesystem::path, std::filesystem::path>& undo_map)
    -> RenameResult {
  RenameResult result;

  for (const auto& [current, original] : undo_map) {
    RenameEntry entry;
    entry.old_path = current;
    entry.new_path = original;

    std::error_code ec;
    std::filesystem::rename(current, original, ec);
    if (ec) {
      entry.error = "Undo rename failed: " + ec.message();
      ++result.failed;
    } else {
      entry.success = true;
      // Also undo sidecar files
      auto sidecars = CollectSidecarPaths(current);
      for (const auto& sc : sidecars) {
        auto ext = sc.extension().string();
        auto orig_sc = DstSidecarPath(original, ext);
        std::filesystem::rename(sc, orig_sc, ec);
      }
      result.undo_map[original] = current;
      ++result.succeeded;
    }

    result.entries.push_back(std::move(entry));
  }

  return result;
}

// ============================================================================
// (b) File operations (with sidecar support)
// ============================================================================

void copy_file_with_sidecar(const std::filesystem::path& src,
                            const std::filesystem::path& dst) {
  std::error_code ec;

  // Ensure the destination parent directory exists
  auto dst_parent = dst.parent_path();
  if (!dst_parent.empty() && !std::filesystem::exists(dst_parent)) {
    std::filesystem::create_directories(dst_parent, ec);
    if (ec) {
      throw std::runtime_error("Cannot create destination directory: " +
                               dst_parent.string() + " (" + ec.message() + ")");
    }
  }

  // Copy the image file
  std::filesystem::copy_file(src, dst,
                             std::filesystem::copy_options::overwrite_existing,
                             ec);
  if (ec) {
    throw std::runtime_error("Cannot copy file: " + src.string() + " → " +
                             dst.string() + " (" + ec.message() + ")");
  }

  // Copy sidecar files
  auto sidecars = CollectSidecarPaths(src);
  for (const auto& sc : sidecars) {
    auto ext    = sc.extension().string();
    auto dst_sc = DstSidecarPath(dst, ext);
    std::filesystem::copy_file(sc, dst_sc,
                               std::filesystem::copy_options::overwrite_existing,
                               ec);
    // Sidecar copy failure is non-fatal
  }
}

void move_file_with_sidecar(const std::filesystem::path& src,
                            const std::filesystem::path& dst) {
  std::error_code ec;

  // Ensure the destination parent directory exists
  auto dst_parent = dst.parent_path();
  if (!dst_parent.empty() && !std::filesystem::exists(dst_parent)) {
    std::filesystem::create_directories(dst_parent, ec);
    if (ec) {
      throw std::runtime_error("Cannot create destination directory: " +
                               dst_parent.string() + " (" + ec.message() + ")");
    }
  }

  // Move the image file
  std::filesystem::rename(src, dst, ec);
  if (ec) {
    // rename may fail across filesystems; fall back to copy + delete
    std::filesystem::copy_file(src, dst,
                               std::filesystem::copy_options::overwrite_existing,
                               ec);
    if (ec) {
      throw std::runtime_error("Cannot move file: " + src.string() + " → " +
                               dst.string() + " (" + ec.message() + ")");
    }
    std::filesystem::remove(src, ec);
  }

  // Move sidecar files
  auto sidecars = CollectSidecarPaths(src);
  for (const auto& sc : sidecars) {
    auto ext    = sc.extension().string();
    auto dst_sc = DstSidecarPath(dst, ext);
    std::filesystem::rename(sc, dst_sc, ec);
    if (ec) {
      // Fallback: copy + delete
      std::filesystem::copy_file(sc, dst_sc,
                                 std::filesystem::copy_options::overwrite_existing,
                                 ec);
      std::filesystem::remove(sc, ec);
    }
  }
}

void delete_file_to_trash(const std::filesystem::path& path) {
  if (!std::filesystem::exists(path)) {
    throw std::runtime_error("File does not exist: " + path.string());
  }

  // Delete sidecar files first
  auto sidecars = CollectSidecarPaths(path);
  for (const auto& sc : sidecars) {
    MoveToTrash(sc);
  }

  // Delete the image file
  if (!MoveToTrash(path)) {
    throw std::runtime_error("Failed to delete file: " + path.string());
  }
}

auto duplicate_with_sidecar(const std::filesystem::path& src)
    -> std::filesystem::path {
  if (!std::filesystem::exists(src)) {
    throw std::runtime_error("Source file does not exist: " + src.string());
  }

  // Build duplicate path: stem + "_copy" + extension
  auto stem      = src.stem().string();
  auto ext       = src.extension().string();
  auto parent    = src.parent_path();
  auto dup_path  = parent / (stem + "_copy" + ext);

  // If the copy already exists, append a numeric suffix
  if (std::filesystem::exists(dup_path)) {
    int suffix = 2;
    while (std::filesystem::exists(
        parent / (stem + "_copy" + std::to_string(suffix) + ext))) {
      ++suffix;
    }
    dup_path = parent / (stem + "_copy" + std::to_string(suffix) + ext);
  }

  std::error_code ec;
  std::filesystem::copy_file(src, dup_path,
                             std::filesystem::copy_options::overwrite_existing,
                             ec);
  if (ec) {
    throw std::runtime_error("Cannot duplicate file: " + src.string() +
                             " (" + ec.message() + ")");
  }

  // Duplicate sidecar files
  auto sidecars = CollectSidecarPaths(src);
  for (const auto& sc : sidecars) {
    auto sc_ext  = sc.extension().string();
    auto dup_sc  = parent / (stem + "_copy" + sc_ext);

    if (std::filesystem::exists(dup_sc)) {
      int suffix = 2;
      while (std::filesystem::exists(
          parent / (stem + "_copy" + std::to_string(suffix) + sc_ext))) {
        ++suffix;
      }
      dup_sc = parent / (stem + "_copy" + std::to_string(suffix) + sc_ext);
    }

    std::filesystem::copy_file(sc, dup_sc,
                               std::filesystem::copy_options::overwrite_existing,
                               ec);
  }

  return dup_path;
}

// ============================================================================
// (c) Folder operations
// ============================================================================

void create_folder(const std::filesystem::path& path) {
  std::error_code ec;
  std::filesystem::create_directories(path, ec);
  if (ec) {
    throw std::runtime_error("Cannot create folder: " + path.string() +
                             " (" + ec.message() + ")");
  }
}

void rename_folder(const std::filesystem::path& old_path,
                   const std::filesystem::path& new_path) {
  if (!std::filesystem::is_directory(old_path)) {
    throw std::runtime_error("Source is not a directory: " + old_path.string());
  }

  std::error_code ec;
  std::filesystem::rename(old_path, new_path, ec);
  if (ec) {
    throw std::runtime_error("Cannot rename folder: " + old_path.string() +
                             " → " + new_path.string() + " (" + ec.message() +
                             ")");
  }
}

void delete_folder(const std::filesystem::path& path) {
  if (!std::filesystem::exists(path)) {
    throw std::runtime_error("Folder does not exist: " + path.string());
  }

  std::error_code ec;
  std::filesystem::remove_all(path, ec);
  if (ec) {
    throw std::runtime_error("Cannot delete folder: " + path.string() +
                             " (" + ec.message() + ")");
  }
}

auto estimate_folder_stats(const std::filesystem::path& path) -> FolderStats {
  if (!std::filesystem::is_directory(path)) {
    throw std::runtime_error("Path is not a directory: " + path.string());
  }

  FolderStats stats;
  std::error_code ec;

  for (const auto& entry : std::filesystem::recursive_directory_iterator(
           path, std::filesystem::directory_options::skip_permission_denied,
           ec)) {
    if (ec) continue;

    if (!entry.is_regular_file(ec)) continue;
    if (ec) continue;

    auto file_size = entry.file_size(ec);
    if (!ec) {
      stats.total_bytes += file_size;
    }
    ++stats.total_files;

    auto ext = entry.path().extension().string();
    if (IsImageExtension(ext)) {
      ++stats.image_count;
    }
  }

  return stats;
}

// ============================================================================
// (d) Virtual copy management
// ============================================================================

auto create_virtual_copy(const std::filesystem::path& image_path)
    -> VirtualCopyInfo {
  if (!std::filesystem::exists(image_path)) {
    throw std::runtime_error("Image file does not exist: " +
                             image_path.string());
  }

  // Find existing virtual copies to determine the next copy number
  auto existing = list_virtual_copies(image_path);

  int next_number = 2;  // v1 is the original sidecar
  if (!existing.empty()) {
    int max_num = 1;
    for (const auto& vc : existing) {
      if (vc.copy_number > max_num) {
        max_num = vc.copy_number;
      }
    }
    next_number = max_num + 1;
  }

  auto sidecar_path = VirtualSidecarPath(image_path, next_number);

  // Create the virtual copy sidecar file
  // The content references the original image and marks this as a virtual copy
  std::ofstream out(sidecar_path, std::ios::binary | std::ios::trunc);
  if (!out) {
    throw std::runtime_error("Cannot create virtual copy sidecar: " +
                             sidecar_path.string());
  }

  // Write minimal metadata referencing the original image
  out << "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
  out << "<alcedo:sidecar xmlns:alcedo=\"http://alcedo.studio/ns/1.0/\">\n";
  out << "  <alcedo:virtualCopy>true</alcedo:virtualCopy>\n";
  out << "  <alcedo:copyNumber>" << next_number << "</alcedo:copyNumber>\n";
  out << "  <alcedo:originalImage>"
      << image_path.filename().string()
      << "</alcedo:originalImage>\n";
  out << "</alcedo:sidecar>\n";
  out.close();

  VirtualCopyInfo info;
  info.sidecar_path = std::move(sidecar_path);
  info.copy_number  = next_number;
  return info;
}

auto list_virtual_copies(const std::filesystem::path& image_path)
    -> std::vector<VirtualCopyInfo> {
  std::vector<VirtualCopyInfo> copies;

  auto parent = image_path.parent_path();
  auto stem   = image_path.stem().string();

  if (!std::filesystem::is_directory(parent)) {
    return copies;
  }

  std::error_code ec;
  for (const auto& entry :
       std::filesystem::directory_iterator(parent, ec)) {
    if (ec) continue;

    auto parsed = ParseVirtualSidecarName(entry.path());
    if (!parsed) continue;

    const auto& [base_stem, copy_number] = *parsed;

    // Verify the base stem matches our image's stem
    if (base_stem != stem) continue;

    VirtualCopyInfo info;
    info.sidecar_path = entry.path();
    info.copy_number  = copy_number;
    copies.push_back(std::move(info));
  }

  // Sort by copy number
  std::sort(copies.begin(), copies.end(),
            [](const VirtualCopyInfo& a, const VirtualCopyInfo& b) {
              return a.copy_number < b.copy_number;
            });

  return copies;
}

auto resolve_virtual_path(const std::filesystem::path& virtual_sidecar_path)
    -> std::optional<std::filesystem::path> {
  auto parsed = ParseVirtualSidecarName(virtual_sidecar_path);
  if (!parsed) return std::nullopt;

  const auto& [base_stem, _] = *parsed;
  auto parent = virtual_sidecar_path.parent_path();

  // Search for an image file matching the base stem
  // Try common image extensions in order of likelihood
  static const std::vector<std::string> image_exts = {
      ".CR3", ".cr3", ".CR2", ".cr2", ".NEF", ".nef",
      ".ARW", ".arw", ".DNG", ".dng", ".RAF", ".raf",
      ".ORF", ".orf", ".RW2", ".rw2", ".PEF", ".pef",
      ".SRW", ".srw", ".JPG", ".jpg", ".JPEG", ".jpeg",
      ".TIF", ".tif", ".TIFF", ".tiff", ".PNG", ".png",
      ".HEIC", ".heic", ".HEIF", ".heif", ".WEBP", ".webp",
  };

  for (const auto& ext : image_exts) {
    auto candidate = parent / (base_stem + ext);
    if (std::filesystem::exists(candidate)) {
      return candidate;
    }
  }

  // If no extension-matched file found, scan the directory for any file
  // whose stem matches base_stem and is not a sidecar
  std::error_code ec;
  for (const auto& entry :
       std::filesystem::directory_iterator(parent, ec)) {
    if (ec) continue;
    if (!entry.is_regular_file(ec)) continue;
    if (entry.path().stem() == base_stem) {
      auto e = entry.path().extension().string();
      // Skip sidecar extensions
      if (e == ".xmp" || e == ".alcd") continue;
      return entry.path();
    }
  }

  return std::nullopt;
}

// ============================================================================
// (e) File utilities
// ============================================================================

auto get_file_size(const std::filesystem::path& path) -> uint64_t {
  std::error_code ec;
  auto size = std::filesystem::file_size(path, ec);
  if (ec) return 0;
  return static_cast<uint64_t>(size);
}

auto estimate_export_size(ExportFormat format,
                          int quality,
                          int width,
                          int height,
                          int bit_depth) -> uint64_t {
  if (width <= 0 || height <= 0) return 0;

  // Uncompressed pixel data size
  auto channels   = 3;  // RGB
  auto raw_bytes  = static_cast<uint64_t>(width) * height * channels *
                     (bit_depth / 8);

  // Quality factor normalized to 0..1
  double q = std::max(0, std::min(100, quality)) / 100.0;

  switch (format) {
    case ExportFormat::JPEG: {
      // JPEG compression ratio varies with quality.
      // Typical: q=1.0 → ~1:4, q=0.5 → ~1:10, q=0.0 → ~1:25
      double ratio = 4.0 + (1.0 - q) * 21.0;
      return static_cast<uint64_t>(std::ceil(raw_bytes / ratio));
    }

    case ExportFormat::PNG: {
      // PNG is lossless; compression ratio depends on content.
      // Assume ~1:2 compression for photographic content.
      return static_cast<uint64_t>(std::ceil(raw_bytes / 2.0));
    }

    case ExportFormat::TIFF: {
      // TIFF with no compression ≈ raw; with LZW ≈ 1:2
      // Assume uncompressed for estimation
      return raw_bytes + 4096;  // + header overhead
    }

    case ExportFormat::JXL: {
      // JPEG XL: lossy at q=1.0 ≈ 1:8, q=0.5 ≈ 1:20, q=0.0 ≈ 1:40
      double ratio = 8.0 + (1.0 - q) * 32.0;
      return static_cast<uint64_t>(std::ceil(raw_bytes / ratio));
    }

    case ExportFormat::AVIF: {
      // AVIF: lossy at q=1.0 ≈ 1:10, q=0.5 ≈ 1:25, q=0.0 ≈ 1:50
      double ratio = 10.0 + (1.0 - q) * 40.0;
      return static_cast<uint64_t>(std::ceil(raw_bytes / ratio));
    }

    case ExportFormat::WEBP: {
      // WebP: lossy at q=1.0 ≈ 1:5, q=0.5 ≈ 1:12, q=0.0 ≈ 1:30
      double ratio = 5.0 + (1.0 - q) * 25.0;
      return static_cast<uint64_t>(std::ceil(raw_bytes / ratio));
    }

    default:
      return raw_bytes;
  }
}

void preserve_timestamps(const std::filesystem::path& path,
                         const std::string& exif_date) {
  // Parse EXIF date format: "YYYY:MM:DD HH:MM:SS"
  if (exif_date.size() < 19) {
    throw std::invalid_argument(
        "Malformed EXIF date string (too short): " + exif_date);
  }

  // Validate separators
  if (exif_date[4] != ':' || exif_date[7] != ':' ||
      exif_date[10] != ' ' || exif_date[13] != ':' ||
      exif_date[16] != ':') {
    throw std::invalid_argument(
        "Malformed EXIF date string (invalid format): " + exif_date);
  }

  // Extract components
  auto parse_int = [&](size_t pos, size_t len) -> int {
    try {
      return std::stoi(exif_date.substr(pos, len));
    } catch (...) {
      throw std::invalid_argument(
          "Malformed EXIF date string (non-numeric): " + exif_date);
    }
  };

  int year   = parse_int(0, 4);
  int month  = parse_int(5, 2);
  int day    = parse_int(8, 2);
  int hour   = parse_int(11, 2);
  int minute = parse_int(14, 2);
  int second = parse_int(17, 2);

  // Validate ranges
  if (month < 1 || month > 12 || day < 1 || day > 31 ||
      hour < 0 || hour > 23 || minute < 0 || minute > 59 ||
      second < 0 || second > 59) {
    throw std::invalid_argument(
        "Malformed EXIF date string (out of range): " + exif_date);
  }

  // Build a time_t from the parsed components
  struct std::tm tm = {};
  tm.tm_year = year - 1900;
  tm.tm_mon  = month - 1;
  tm.tm_mday = day;
  tm.tm_hour = hour;
  tm.tm_min  = minute;
  tm.tm_sec  = second;
  tm.tm_isdst = -1;  // let mktime determine DST

  std::time_t time_val = std::mktime(&tm);
  if (time_val == -1) {
    throw std::invalid_argument(
        "Malformed EXIF date string (invalid date): " + exif_date);
  }

  // Set file times using std::filesystem.
  // C++17 filesystem uses file_time_type which is based on a different clock
  // than system_clock. We convert via time_t → system_clock → file_time_type.
  auto sys_time = std::chrono::system_clock::from_time_t(time_val);
  auto file_time = std::chrono::time_point_cast<std::chrono::system_clock::duration>(
      sys_time - std::chrono::system_clock::now() +
      std::filesystem::file_time_type::clock::now());
  std::error_code ec;
  std::filesystem::last_write_time(path, file_time, ec);
  if (ec) {
    throw std::runtime_error("Cannot set file timestamp: " + path.string() +
                             " (" + ec.message() + ")");
  }

  // Also attempt to set access time (last_write_time sets both mtime;
  // access time on most platforms tracks mtime or is set separately)
  // std::filesystem does not have a direct access-time setter in C++17,
  // so we rely on the OS. On Linux, we use utime as a fallback.
#if defined(__linux__) || defined(__unix__)
  struct utimbuf times;
  times.actime  = time_val;
  times.modtime = time_val;
  if (::utime(path.c_str(), &times) != 0) {
    // Non-fatal: mtime was already set via std::filesystem
  }
#endif
}

}  // namespace alcedo::app
