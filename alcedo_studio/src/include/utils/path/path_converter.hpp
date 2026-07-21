//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QString>
#include <filesystem>
#include <optional>
#include <string>

#ifdef Q_OS_ANDROID
class QJniObject;
#endif

namespace alcedo::path {

/// Utility for safe path conversion between QString and std::filesystem::path,
/// with special handling for Chinese (and other non-ASCII) characters in file
/// paths. On Windows, std::string paths use the system's ANSI code page which
/// cannot represent many CJK characters — this utility always converts through
/// QString (UTF-16) or std::filesystem::path (wide on Windows) to avoid
/// encoding corruption.
class PathConverter {
 public:
  /// Converts a QString path to a std::filesystem::path using the platform-
  /// appropriate encoding. On Windows, uses the UTF-16 constructor; on other
  /// platforms, uses UTF-8.
  [[nodiscard]] static auto ToFileSystemPath(const QString& qpath) -> std::filesystem::path;

  /// Converts a std::filesystem::path to a QString using the platform-
  /// appropriate encoding.
  [[nodiscard]] static auto ToQString(const std::filesystem::path& fpath) -> QString;

  /// Converts a std::string (assumed UTF-8) to a std::filesystem::path.
  /// Avoids the ANSI code page pitfall on Windows by going through
  /// QString first.
  [[nodiscard]] static auto Utf8ToFileSystemPath(const std::string& utf8_path)
      -> std::filesystem::path;

  /// Converts a std::filesystem::path to a UTF-8 std::string.
  [[nodiscard]] static auto ToUtf8String(const std::filesystem::path& fpath) -> std::string;

  /// Normalizes a path to use platform-appropriate separators and encoding.
  /// Removes trailing separators, collapses duplicate separators, and resolves
  /// "." and ".." where possible without touching the filesystem.
  [[nodiscard]] static auto NormalizePath(const QString& qpath) -> QString;

  /// Validates a path for problematic characters that may cause issues on
  /// the current platform. Returns nullopt if the path is clean, or a
  /// human-readable description of the issue if problematic characters are
  /// found.
  [[nodiscard]] static auto ValidatePath(const QString& qpath) -> std::optional<QString>;

  /// Safely converts a raw std::string file path (which may be in the system's
  /// ANSI code page on Windows) to a QString. Tries UTF-8 first, falls back
  /// to the system locale on Windows. This is useful when receiving paths
  /// from legacy APIs that return std::string.
  [[nodiscard]] static auto SafeStringToQString(const std::string& str_path) -> QString;

#ifdef Q_OS_ANDROID
  /// Resolves an Android content:// URI to a file path using ContentResolver.
  /// If the URI cannot be resolved (e.g. scoped storage), copies the content
  /// to a cache file and returns the cache path.
  [[nodiscard]] static auto ResolveAndroidContentUri(const QString& content_uri) -> QString;

  /// Fallback: copies content from a content:// URI to the app cache directory.
  [[nodiscard]] static auto CopyContentUriToCache(
      const QString& content_uri,
      const QJniObject& content_resolver,
      const QJniObject& uri,
      const QJniObject& context) -> QString;
#endif
};

}  // namespace alcedo::path
