//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "utils/path/path_converter.hpp"

#include <QDir>
#include <QRegularExpression>
#include <QStringConverter>

#ifdef Q_OS_ANDROID
#include <QJniObject>
#include <QJniEnvironment>
#include <QAndroidJniEnvironment>
#endif

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

#ifdef Q_OS_ANDROID
  // On Android, content:// URIs from the system file picker must be resolved
  // to an actual file path via ContentResolver before they can be used as
  // filesystem paths. Return the URI as-is if it cannot be resolved.
  if (qpath.startsWith(QStringLiteral("content://"))) {
    QString resolved = ResolveAndroidContentUri(qpath);
    if (!resolved.isEmpty()) {
      return QDir::cleanPath(resolved);
    }
    // If resolution fails, return the content URI as-is;
    // callers must handle content:// URIs via Android APIs.
    return qpath;
  }
#endif

  QString normalized = QDir::cleanPath(qpath);
  return normalized;
}

#ifdef Q_OS_ANDROID
auto PathConverter::ResolveAndroidContentUri(const QString& content_uri) -> QString {
  // Convert a content:// URI to a file path using Android's ContentResolver.
  // This handles URIs from the system file picker, gallery, etc.
  QJniObject activity = QJniObject::callStaticObjectMethod(
      "org/qtproject/qt/android/bindings/QtActivity",
      "currentActivity",
      "()Landroid/app/Activity;");

  if (!activity.isValid()) {
    return {};
  }

  QJniObject context = activity.callObjectMethod(
      "getApplicationContext",
      "()Landroid/content/Context;");

  if (!context.isValid()) {
    return {};
  }

  // Get ContentResolver
  QJniObject content_resolver = context.callObjectMethod(
      "getContentResolver",
      "()Landroid/content/ContentResolver;");

  if (!content_resolver.isValid()) {
    return {};
  }

  // Parse the URI
  QJniObject uri = QJniObject::callStaticObjectMethod(
      "android/net/Uri",
      "parse",
      "(Ljava/lang/String;)Landroid/net/Uri;",
      QJniObject::fromString(content_uri).object());

  if (!uri.isValid()) {
    return {};
  }

  // Try to get a file path from the content URI using the _data column
  QJniObject columns = QJniObject::callStaticObjectMethod(
      "java/lang/reflect/Array",
      "newInstance",
      "(Ljava/lang/Class;I)Ljava/lang/Object;",
      QJniObject::getStaticObjectField(
          "java/lang/String",
          "class",
          "Ljava/lang/Class;").object(),
      1);

  QAndroidJniEnvironment env;
  jobjectArray arr = columns.object<jobjectArray>();
  env->SetObjectArrayElement(arr, 0,
      QJniObject::fromString(QStringLiteral("_data")).object<jstring>());

  QJniObject cursor = content_resolver.callObjectMethod(
      "query",
      "(Landroid/net/Uri;[Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Landroid/database/Cursor;",
      uri.object(),
      arr,
      nullptr,
      nullptr,
      nullptr);

  if (cursor.isValid()) {
    jboolean has_first = cursor.callMethod<jboolean>("moveToFirst");
    if (has_first) {
      QJniObject file_path = cursor.callObjectMethod(
          "getString",
          "(I)Ljava/lang/String;",
          0);
      cursor.callMethod<void>("close");
      if (file_path.isValid()) {
        return file_path.toString();
      }
    }
    cursor.callMethod<void>("close");
  }

  // If _data column didn't work, try MEDIA_PATH from MediaStore
  // For scoped storage (API 29+), content:// URIs often cannot be resolved
  // to file paths. In that case, copy the content to the app cache dir.
  return CopyContentUriToCache(content_uri, content_resolver, uri, context);
}

auto PathConverter::CopyContentUriToCache(
    const QString& content_uri,
    const QJniObject& content_resolver,
    const QJniObject& uri,
    const QJniObject& context) -> QString {
  // Fallback for scoped storage: copy the content to a cache file
  // and return the cache file path.
  QJniObject cache_dir = context.callObjectMethod(
      "getCacheDir", "()Ljava/io/File;");

  if (!cache_dir.isValid()) {
    return {};
  }

  QJniObject cache_path = cache_dir.callObjectMethod(
      "getAbsolutePath", "()Ljava/lang/String;");

  if (!cache_path.isValid()) {
    return {};
  }

  // Create a unique filename from the URI's last path segment
  QJniObject last_segment = uri.callObjectMethod(
      "getLastPathSegment", "()Ljava/lang/String;");

  QString filename;
  if (last_segment.isValid()) {
    filename = last_segment.toString();
  } else {
    // Generate a unique filename
    filename = QStringLiteral("alcedo_import_") +
               QString::number(reinterpret_cast<quintptr>(content_uri.data()), 16);
  }

  QString dest_path = cache_path.toString() + QStringLiteral("/") + filename;

  // Open input stream from the content URI
  QJniObject input_stream = content_resolver.callObjectMethod(
      "openInputStream",
      "(Landroid/net/Uri;)Ljava/io/InputStream;",
      uri.object());

  if (!input_stream.isValid()) {
    return {};
  }

  // Open output stream to the cache file
  QJniObject dest_file = QJniObject("java/io/File",
      "(Ljava/lang/String;)V",
      QJniObject::fromString(dest_path).object());

  QJniObject output_stream = QJniObject("java/io/FileOutputStream",
      "(Ljava/io/File;)V",
      dest_file.object());

  if (!output_stream.isValid()) {
    input_stream.callMethod<void>("close");
    return {};
  }

  // Copy data: 8KB buffer
  QJniObject buffer = QJniObject("java/io/ByteArrayOutputStream");
  QJniObject byte_buf = QJniObject::callStaticObjectMethod(
      "java/nio/ByteBuffer", "allocate", "(I)Ljava/nio/ByteBuffer;", 8192);

  // Use a simple byte array for the copy loop
  QAndroidJniEnvironment env;
  jbyteArray read_buf = env->NewByteArray(8192);

  int bytes_read = 0;
  while ((bytes_read = input_stream.callMethod<int>(
      "read", "([B)I", read_buf)) > 0) {
    output_stream.callMethod<void>(
        "write", "([BII)V", read_buf, 0, bytes_read);
  }

  env->DeleteLocalRef(read_buf);

  input_stream.callMethod<void>("close");
  output_stream.callMethod<void>("close");

  return dest_path;
}
#endif

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
