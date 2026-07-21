//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QLoggingCategory>
#include <QString>

#include <chrono>
#include <string>
#include <string_view>

namespace alcedo::diag {

Q_DECLARE_LOGGING_CATEGORY(appLog)
Q_DECLARE_LOGGING_CATEGORY(semanticLog)
Q_DECLARE_LOGGING_CATEGORY(semanticRpcLog)
Q_DECLARE_LOGGING_CATEGORY(semanticDbLog)
Q_DECLARE_LOGGING_CATEGORY(editorLog)
Q_DECLARE_LOGGING_CATEGORY(pipelineLog)
Q_DECLARE_LOGGING_CATEGORY(memoryLog)
Q_DECLARE_LOGGING_CATEGORY(openclLog)
Q_DECLARE_LOGGING_CATEGORY(hdrLog)
Q_DECLARE_LOGGING_CATEGORY(panoramaLog)

auto InitializeApplicationLogging(const QString& preferred_directory = {}) -> QString;
void ShutdownApplicationLogging();
auto CurrentLogFilePath() -> QString;

// Set the maximum log file size in bytes. When the current log file exceeds
// this size, it is rotated (renamed with a timestamp suffix and a new file
// is started). Default: 10 MB.
void SetLogRotationMaxSize(qint64 max_size_bytes);

// Set the maximum number of rotated log files to keep. Older files are
// deleted when the limit is exceeded. Default: 5.
void SetLogRotationMaxFiles(int max_files);

// Set per-module log level. Modules are identified by their QLoggingCategory
// name (e.g., "alcedo.pipeline"). Level strings: "trace", "debug", "info",
// "warn", "error", "critical", "off".
void SetModuleLogLevel(const QString& module, const QString& level);

// Export all log files (current + rotated) to a ZIP archive at the given
// path. Returns true on success. Useful for user bug reports.
auto ExportLogsToArchive(const QString& archive_path) -> bool;

// Get the list of all available log file paths (current + rotated).
auto GetAllLogFilePaths() -> QStringList;

class TraceScope final {
 public:
  TraceScope(const QLoggingCategory& category, QString event, QString details = {});
  ~TraceScope();

  TraceScope(const TraceScope&)            = delete;
  TraceScope& operator=(const TraceScope&) = delete;

 private:
  const QLoggingCategory&                         category_;
  QString                                         event_;
  QString                                         details_;
  std::chrono::steady_clock::time_point           started_at_;
  bool                                            enabled_ = false;
};

// Performance timer for measuring operation durations. Lightweight RAII
// wrapper that logs the elapsed time on destruction.
class PerfTimer final {
 public:
  explicit PerfTimer(const QLoggingCategory& category, QString operation)
      : category_(category),
        operation_(std::move(operation)),
        started_at_(std::chrono::steady_clock::now()),
        enabled_(category_.isInfoEnabled()) {}

  ~PerfTimer() {
    if (!enabled_) return;
    const auto elapsed_ms = std::chrono::duration_cast<std::chrono::milliseconds>(
                                std::chrono::steady_clock::now() - started_at_)
                                .count();
    QMessageLogger(QT_MESSAGELOG_FILE, QT_MESSAGELOG_LINE, QT_MESSAGELOG_FUNC)
        .info(category_)
        .noquote()
        << QStringLiteral("perf.%1 elapsed_ms=%2").arg(operation_).arg(elapsed_ms);
  }

  // Get elapsed milliseconds so far without stopping the timer.
  auto ElapsedMs() const -> qint64 {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
               std::chrono::steady_clock::now() - started_at_)
        .count();
  }

  PerfTimer(const PerfTimer&)            = delete;
  PerfTimer& operator=(const PerfTimer&) = delete;

 private:
  const QLoggingCategory&                         category_;
  QString                                         operation_;
  std::chrono::steady_clock::time_point           started_at_;
  bool                                            enabled_;
};

}  // namespace alcedo::diag

// Convenience macros for structured logging with the fmt-style API
// that the existing APP_LOG_* macros provide.
#define APP_LOG_TRACE(module, ...) \
  qCDebug(module).noquote() << QString::asprintf(__VA_ARGS__)
#define APP_LOG_DEBUG(module, ...) \
  qCDebug(module).noquote() << QString::asprintf(__VA_ARGS__)
#define APP_LOG_INFO(module, ...) \
  qCInfo(module).noquote() << QString::asprintf(__VA_ARGS__)
#define APP_LOG_WARN(module, ...) \
  qCWarning(module).noquote() << QString::asprintf(__VA_ARGS__)
#define APP_LOG_ERROR(module, ...) \
  qCCritical(module).noquote() << QString::asprintf(__VA_ARGS__)

// Shortcuts that use the default appLog category.
#define APP_LOG_TRACE_DEFAULT(...) APP_LOG_TRACE(alcedo::diag::appLog(), __VA_ARGS__)
#define APP_LOG_DEBUG_DEFAULT(...) APP_LOG_DEBUG(alcedo::diag::appLog(), __VA_ARGS__)
#define APP_LOG_INFO_DEFAULT(...) APP_LOG_INFO(alcedo::diag::appLog(), __VA_ARGS__)
#define APP_LOG_WARN_DEFAULT(...) APP_LOG_WARN(alcedo::diag::appLog(), __VA_ARGS__)
#define APP_LOG_ERROR_DEFAULT(...) APP_LOG_ERROR(alcedo::diag::appLog(), __VA_ARGS__)

// Performance timing macro: creates a PerfTimer that logs on scope exit.
#define APP_PERF_TIMER(operation) \
  ::alcedo::diag::PerfTimer perf_timer_##__LINE__(::alcedo::diag::pipelineLog(), operation)

#define APP_PERF_TIMER_IN(category, operation) \
  ::alcedo::diag::PerfTimer perf_timer_##__LINE__(category, operation)
