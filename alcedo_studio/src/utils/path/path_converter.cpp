//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "utils/path/path_converter.hpp"

#include <QDir>
#include <QRegularExpression>
#include <QStringConverter>

#ifdef _WIN32
#include <windows.h>
#endif

namespace alcedo::path {

auto PathConverter::ToFileSystemPath(const QString& qpath) -> std::filesystem::path {
#ifdef _WIN32
  // On Windows, std::filesystem::path can be constructed from wchar_t* (UTF-16),
  // which is the native encoding of QString.
  return std::filesystem::path(reinterpret_cast<const wchar_t*>(qpath.utf16()));
#else
  // On Linux/macOS, use UTF-8 encoding.
  return std::filesystem::path(qpath.toUtf8().constData());
#endif
}

auto PathConverter::ToQString(const std::filesystem::path& fpath) -> QString {
#ifdef _WIN32
  return QString::fromWCharArray(fpath.wstring().c_str());
#else
  return QString::fromStdString(fpath.string());
#endif
}

auto PathConverter::Utf8ToFileSystemPath(const std::string& utf8_path) -> std::filesystem::path {
  const QString qpath = QString::fromStdString(utf8_path);
  return ToFileSystemPath(qpath);
}

auto PathConverter::ToUtf8String(const std::filesystem::path& fpath) -> std::string {
  return ToQString(fpath).toStdString();
}

auto PathConverter::NormalizePath(const QString& qpath) -> QString {
  if (qpath.isEmpty()) {
    return qpath;
  }
  QString normalized = QDir::cleanPath(qpath);
  return normalized;
}

auto PathConverter::ValidatePath(const QString& qpath) -> std::optional<QString> {
  if (qpath.isEmpty()) {
    return QString(QStringLiteral("Path is empty."));
  }

#ifdef _WIN32
  // On Windows, check for characters that are invalid in file paths.
  static const QRegularExpression kInvalidChars(
      QStringLiteral("[<>:\"|?*]"));
  auto match = kInvalidChars.match(qpath);
  if (match.hasMatch()) {
    return QString(QStringLiteral("Path contains invalid character '%1' on Windows."))
        .arg(match.captured(0));
  }

  // Check for reserved device names (CON, PRN, AUX, NUL, COM1-COM9, LPT1-LPT9)
  QString filename = qpath.section(QDir::separator(), -1);
  QString base = filename.section(QLatin1Char('.'), 0, 0).toUpper();
  static const QStringList kReservedNames = {
      QStringLiteral("CON"), QStringLiteral("PRN"), QStringLiteral("AUX"),
      QStringLiteral("NUL"),
      QStringLiteral("COM1"), QStringLiteral("COM2"), QStringLiteral("COM3"),
      QStringLiteral("COM4"), QStringLiteral("COM5"), QStringLiteral("COM6"),
      QStringLiteral("COM7"), QStringLiteral("COM8"), QStringLiteral("COM9"),
      QStringLiteral("LPT1"), QStringLiteral("LPT2"), QStringLiteral("LPT3"),
      QStringLiteral("LPT4"), QStringLiteral("LPT5"), QStringLiteral("LPT6"),
      QStringLiteral("LPT7"), QStringLiteral("LPT8"), QStringLiteral("LPT9"),
  };
  if (kReservedNames.contains(base)) {
    return QString(QStringLiteral("Path uses reserved device name '%1' on Windows."))
        .arg(base);
  }
#endif

  // Check for null bytes (which would truncate the path silently in C APIs)
  if (qpath.contains(QLatin1Char('\0'))) {
    return QString(QStringLiteral("Path contains null byte."));
  }

  // Check for extremely long paths (Windows MAX_PATH = 260, though this can be
  // overridden; other platforms generally handle longer paths)
  if (qpath.length() > 4096) {
    return QString(QStringLiteral("Path exceeds 4096 characters, which may cause issues."));
  }

  return std::nullopt;
}

auto PathConverter::SafeStringToQString(const std::string& str_path) -> QString {
  // Try UTF-8 first — most modern APIs produce UTF-8 strings.
  QString result = QString::fromStdString(str_path);
  if (!result.isEmpty()) {
    // Heuristic: if the result contains replacement characters (U+FFFD),
    // the input was likely not UTF-8.
    bool has_replacement = false;
    for (int i = 0; i < result.length(); ++i) {
      if (result[i] == QChar(0xFFFD)) {
        has_replacement = true;
        break;
      }
    }
    if (!has_replacement) {
      return result;
    }
  }

#ifdef _WIN32
  // On Windows, the string may be in the system's ANSI code page.
  // Try converting via MultiByteToWideChar.
  if (str_path.empty()) {
    return {};
  }
  int wlen = MultiByteToWideChar(CP_ACP, MB_ERR_INVALID_CHARS,
                                  str_path.c_str(), -1, nullptr, 0);
  if (wlen > 0) {
    std::wstring wide(wlen, L'\0');
    MultiByteToWideChar(CP_ACP, 0, str_path.c_str(), -1, wide.data(), wlen);
    // Remove the trailing null that MultiByteToWideChar adds
    if (!wide.empty() && wide.back() == L'\0') {
      wide.pop_back();
    }
    return QString::fromWCharArray(wide.c_str());
  }
#endif

  // Fallback: return the best-effort UTF-8 conversion
  return result;
}

}  // namespace alcedo::path
