//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "utils/diagnostics/app_logging.hpp"

#include <QCoreApplication>
#include <QDateTime>
#include <QDebug>
#include <QDir>
#include <QFile>
#include <QFileInfo>
#include <QStandardPaths>
#include <QThread>
#include <QZipWriter>

#include <algorithm>
#include <cstdio>
#include <cstdlib>
#include <memory>
#include <mutex>
#include <utility>
#include <vector>

namespace alcedo::diag {

Q_LOGGING_CATEGORY(appLog, "alcedo.app")
Q_LOGGING_CATEGORY(semanticLog, "alcedo.semantic")
Q_LOGGING_CATEGORY(semanticRpcLog, "alcedo.semantic.rpc")
Q_LOGGING_CATEGORY(semanticDbLog, "alcedo.semantic.db")
Q_LOGGING_CATEGORY(editorLog, "alcedo.editor")
Q_LOGGING_CATEGORY(pipelineLog, "alcedo.pipeline")
Q_LOGGING_CATEGORY(memoryLog, "alcedo.memory")
Q_LOGGING_CATEGORY(openclLog, "alcedo.opencl")
Q_LOGGING_CATEGORY(hdrLog, "alcedo.hdr")
Q_LOGGING_CATEGORY(panoramaLog, "alcedo.panorama")

namespace {

std::mutex             g_log_lock;
std::unique_ptr<QFile> g_log_file;
QtMessageHandler      g_previous_handler = nullptr;
QString               g_log_file_path;
QString               g_log_dir_path;
bool                  g_console_enabled = false;
qint64                g_rotation_max_size = 10 * 1024 * 1024;  // 10 MB default
int                   g_rotation_max_files = 5;
qint64                g_current_log_size = 0;

// Forward declarations for internal functions (must be called with g_log_lock held).
void RotateLogIfNeeded();
void CleanOldLogFiles();

auto MessageTypeName(QtMsgType type) -> const char* {
  switch (type) {
    case QtDebugMsg:
      return "DEBUG";
    case QtInfoMsg:
      return "INFO";
    case QtWarningMsg:
      return "WARN";
    case QtCriticalMsg:
      return "ERROR";
    case QtFatalMsg:
      return "FATAL";
  }
  return "LOG";
}

auto ThreadIdString() -> QString {
  return QString::number(reinterpret_cast<quintptr>(QThread::currentThreadId()), 16);
}

// Rotate the current log file if it exceeds the size limit.
// Must be called with g_log_lock held.
void RotateLogIfNeeded() {
  if (!g_log_file || !g_log_file->isOpen()) return;
  if (g_current_log_size < g_rotation_max_size) return;

  // Close current file.
  const QString old_path = g_log_file_path;
  g_log_file->flush();
  g_log_file->close();

  // Rename current file with a timestamp suffix.
  const QString rot_timestamp =
      QDateTime::currentDateTime().toString(QStringLiteral("yyyyMMdd_HHmmss"));
  const QString rotated_path =
      old_path.left(old_path.lastIndexOf(QLatin1Char('.'))) + QStringLiteral("_")
      + rot_timestamp + QStringLiteral(".log");
  QFile::rename(old_path, rotated_path);

  // Open a new log file.
  const qint64 pid = QCoreApplication::applicationPid();
  const QString timestamp = QDateTime::currentDateTime().toString(QStringLiteral("yyyyMMdd_HHmmss"));
  g_log_file_path =
      QDir(g_log_dir_path).filePath(QStringLiteral("alcedo_%1_%2.log").arg(timestamp).arg(pid));

  auto file = std::make_unique<QFile>(g_log_file_path);
  if (file->open(QIODevice::WriteOnly | QIODevice::Append | QIODevice::Text)) {
    g_log_file = std::move(file);
    g_current_log_size = 0;
  } else {
    g_log_file.reset();
    g_log_file_path.clear();
  }

  // Clean up old rotated files beyond the retention limit.
  CleanOldLogFiles();
}

// Remove rotated log files beyond the retention limit.
// Must be called with g_log_lock held.
void CleanOldLogFiles() {
  if (g_log_dir_path.isEmpty()) return;

  QDir dir(g_log_dir_path);
  const QStringList filters = {QStringLiteral("alcedo_*.log")};
  const QFileInfoList entries = dir.entryInfoList(filters, QDir::Files, QDir::Time | QDir::Reversed);

  // Keep current log file + g_rotation_max_files rotated files.
  const int max_to_keep = g_rotation_max_files + 1;
  if (entries.size() <= max_to_keep) return;

  for (int i = 0; i < entries.size() - max_to_keep; ++i) {
    QFile::remove(entries[i].absoluteFilePath());
  }
}

void ApplicationMessageHandler(QtMsgType type, const QMessageLogContext& context,
                               const QString& message) {
  const QString timestamp = QDateTime::currentDateTime().toString(Qt::ISODateWithMs);
  const QString category =
      context.category && context.category[0] != '\0' ? QString::fromUtf8(context.category)
                                                      : QStringLiteral("default");
  QString line = QStringLiteral("%1 [%2] [tid=%3] [%4] %5")
                     .arg(timestamp, QString::fromLatin1(MessageTypeName(type)), ThreadIdString(),
                          category, message);
  if (context.file && context.line > 0) {
    line += QStringLiteral(" (%1:%2)").arg(QString::fromUtf8(context.file)).arg(context.line);
  }
  line += QLatin1Char('\n');

  const QByteArray bytes = line.toUtf8();
  {
    std::lock_guard lock(g_log_lock);
    if (g_log_file && g_log_file->isOpen()) {
      g_log_file->write(bytes);
      g_log_file->flush();
      g_current_log_size += bytes.size();
      RotateLogIfNeeded();
    }
  }

  if (g_console_enabled) {
    std::fwrite(bytes.constData(), 1, static_cast<size_t>(bytes.size()), stderr);
    std::fflush(stderr);
  }

  if (type == QtFatalMsg) {
    std::abort();
  }
}

auto ResolveLogDirectory(const QString& preferred_directory) -> QString {
  if (!preferred_directory.isEmpty()) {
    return preferred_directory;
  }
  const QString env_dir = qEnvironmentVariable("ALCEDO_LOG_DIR");
  if (!env_dir.isEmpty()) {
    return env_dir;
  }
  QString base = QStandardPaths::writableLocation(QStandardPaths::AppLocalDataLocation);
  if (base.isEmpty()) {
    base = QDir::tempPath() + QStringLiteral("/Alcedo");
  }
  return QDir(base).filePath(QStringLiteral("logs"));
}

}  // namespace

auto InitializeApplicationLogging(const QString& preferred_directory) -> QString {
  QString initialized_path;
  {
    std::lock_guard lock(g_log_lock);
    if (g_log_file && g_log_file->isOpen()) {
      return g_log_file_path;
    }

    g_log_dir_path = ResolveLogDirectory(preferred_directory);
    QDir().mkpath(g_log_dir_path);

    const QString timestamp =
        QDateTime::currentDateTime().toString(QStringLiteral("yyyyMMdd_HHmmss"));
    const qint64 pid = QCoreApplication::applicationPid();
    g_log_file_path =
        QDir(g_log_dir_path).filePath(QStringLiteral("alcedo_%1_%2.log").arg(timestamp).arg(pid));

    auto file = std::make_unique<QFile>(g_log_file_path);
    if (!file->open(QIODevice::WriteOnly | QIODevice::Append | QIODevice::Text)) {
      g_log_file_path.clear();
      return {};
    }

    g_log_file        = std::move(file);
    g_current_log_size = 0;
    g_console_enabled = qEnvironmentVariableIntValue("ALCEDO_LOG_CONSOLE") != 0;

    if (qEnvironmentVariableIsEmpty("QT_LOGGING_RULES")) {
      QLoggingCategory::setFilterRules(QStringLiteral(
          "*.debug=false\n"
          "qt.*.info=false\n"
          "alcedo.*.info=true\n"
          "alcedo.*.warning=true\n"));
    }

    g_previous_handler = qInstallMessageHandler(ApplicationMessageHandler);
    initialized_path   = g_log_file_path;

    // Clean up any old rotated log files from previous sessions.
    CleanOldLogFiles();
  }

  qCInfo(appLog).noquote()
      << QStringLiteral("logging.initialized path=%1").arg(initialized_path);
  return initialized_path;
}

void ShutdownApplicationLogging() {
  std::lock_guard lock(g_log_lock);
  qInstallMessageHandler(g_previous_handler);
  g_previous_handler = nullptr;
  if (g_log_file) {
    g_log_file->flush();
    g_log_file->close();
    g_log_file.reset();
  }
}

auto CurrentLogFilePath() -> QString {
  std::lock_guard lock(g_log_lock);
  return g_log_file_path;
}

void SetLogRotationMaxSize(qint64 max_size_bytes) {
  std::lock_guard lock(g_log_lock);
  g_rotation_max_size = std::max(max_size_bytes, qint64(1024 * 1024));  // Min 1 MB
}

void SetLogRotationMaxFiles(int max_files) {
  std::lock_guard lock(g_log_lock);
  g_rotation_max_files = std::max(max_files, 1);
}

void SetModuleLogLevel(const QString& module, const QString& level) {
  // Build a Qt logging rule string for the given module/level pair.
  // Format: "<module>.<level>=true"
  QString rule;
  const auto level_lower = level.toLower();

  if (level_lower == QStringLiteral("off")) {
    rule = module + QStringLiteral(".info=false\n")
         + module + QStringLiteral(".warning=false\n")
         + module + QStringLiteral(".critical=false");
  } else if (level_lower == QStringLiteral("critical") || level_lower == QStringLiteral("error")) {
    rule = module + QStringLiteral(".critical=true\n")
         + module + QStringLiteral(".warning=false\n")
         + module + QStringLiteral(".info=false\n")
         + module + QStringLiteral(".debug=false");
  } else if (level_lower == QStringLiteral("warn") || level_lower == QStringLiteral("warning")) {
    rule = module + QStringLiteral(".warning=true\n")
         + module + QStringLiteral(".critical=true\n")
         + module + QStringLiteral(".info=false\n")
         + module + QStringLiteral(".debug=false");
  } else if (level_lower == QStringLiteral("info")) {
    rule = module + QStringLiteral(".info=true\n")
         + module + QStringLiteral(".warning=true\n")
         + module + QStringLiteral(".critical=true\n")
         + module + QStringLiteral(".debug=false");
  } else if (level_lower == QStringLiteral("debug")) {
    rule = module + QStringLiteral(".debug=true\n")
         + module + QStringLiteral(".info=true\n")
         + module + QStringLiteral(".warning=true\n")
         + module + QStringLiteral(".critical=true");
  } else if (level_lower == QStringLiteral("trace")) {
    // Qt doesn't have a separate trace level; treat as debug.
    rule = module + QStringLiteral(".debug=true\n")
         + module + QStringLiteral(".info=true\n")
         + module + QStringLiteral(".warning=true\n")
         + module + QStringLiteral(".critical=true");
  }

  if (!rule.isEmpty()) {
    QLoggingCategory::setFilterRules(rule);
  }
}

auto GetAllLogFilePaths() -> QStringList {
  std::lock_guard lock(g_log_lock);
  if (g_log_dir_path.isEmpty()) return {};

  QDir dir(g_log_dir_path);
  const QStringList filters = {QStringLiteral("alcedo_*.log")};
  QStringList result;
  const QFileInfoList entries = dir.entryInfoList(filters, QDir::Files, QDir::Time);
  for (const auto& info : entries) {
    result.append(info.absoluteFilePath());
  }
  return result;
}

auto ExportLogsToArchive(const QString& archive_path) -> bool {
  const QStringList paths = GetAllLogFilePaths();
  if (paths.isEmpty()) return false;

  QFile zip_file(archive_path);
  if (!zip_file.open(QIODevice::WriteOnly)) return false;

  QZipWriter zip_writer(&zip_file);
  zip_writer.setCompressionPolicy(QZipWriter::AutoCompress);

  for (const auto& path : paths) {
    QFile log_file(path);
    if (log_file.open(QIODevice::ReadOnly)) {
      const QString filename = QFileInfo(path).fileName();
      zip_writer.addFile(filename, &log_file);
      log_file.close();
    }
  }

  zip_writer.close();
  zip_file.close();
  return true;
}

TraceScope::TraceScope(const QLoggingCategory& category, QString event, QString details)
    : category_(category),
      event_(std::move(event)),
      details_(std::move(details)),
      started_at_(std::chrono::steady_clock::now()),
      enabled_(category_.isInfoEnabled()) {
  if (!enabled_) {
    return;
  }
  if (details_.isEmpty()) {
    QMessageLogger(QT_MESSAGELOG_FILE, QT_MESSAGELOG_LINE, QT_MESSAGELOG_FUNC)
        .info(category_)
        .noquote()
        << event_ + QStringLiteral(".begin");
  } else {
    QMessageLogger(QT_MESSAGELOG_FILE, QT_MESSAGELOG_LINE, QT_MESSAGELOG_FUNC)
        .info(category_)
        .noquote()
        << event_ + QStringLiteral(".begin ") + details_;
  }
}

TraceScope::~TraceScope() {
  if (!enabled_) {
    return;
  }
  const auto elapsed_ms = std::chrono::duration_cast<std::chrono::milliseconds>(
                              std::chrono::steady_clock::now() - started_at_)
                              .count();
  QString message =
      event_ + QStringLiteral(".end elapsed_ms=") + QString::number(elapsed_ms);
  if (!details_.isEmpty()) {
    message += QLatin1Char(' ');
    message += details_;
  }
  QMessageLogger(QT_MESSAGELOG_FILE, QT_MESSAGELOG_LINE, QT_MESSAGELOG_FUNC)
      .info(category_)
      .noquote()
      << message;
}

}  // namespace alcedo::diag
