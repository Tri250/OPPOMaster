//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <filesystem>
#include <memory>
#include <string>
#include <vector>

#include <QObject>
#include <QString>

namespace alcedo {

/// Crash report metadata assembled from system info, log tail, and GPU info.
struct CrashReport {
  std::string              crash_id;
  std::string              app_version;
  std::string              os_name;
  std::string              os_version;
  std::string              architecture;
  std::string              gpu_info;
  std::string              crash_time;
  std::string              crash_type;       // e.g. "SIGSEGV", "SIGABRT"
  std::string              stack_trace;
  std::vector<std::string> log_tail;         // Last N log lines
  std::filesystem::path    minidump_path;    // Path to the minidump file
  bool                     submitted = false;
};

/// Cross-platform crash handling and reporting.
///
/// On startup:
///   1. Initialize the crash handler (Breakpad/Crashpad)
///   2. Check for leftover minidumps from previous crashes
///   3. If found, show crash dialog for user-initiated submission
///
/// On crash:
///   1. Capture minidump
///   2. Assemble crash report with system info, log tail, GPU info
///   3. Save locally for next-launch submission
class CrashReporter : public QObject {
  Q_OBJECT

  Q_PROPERTY(bool hasPendingCrashReport READ HasPendingCrashReport NOTIFY PendingCrashChanged)
  Q_PROPERTY(QString pendingCrashInfo READ PendingCrashInfo NOTIFY PendingCrashChanged)

 public:
  explicit CrashReporter(QObject* parent = nullptr);
  ~CrashReporter() override;

  /// Initialize the crash handler. Call early in main().
  void Initialize();

  /// Check for pending crash reports from a previous session.
  void CheckForPendingCrashes();

  /// Whether there is a pending crash report to submit.
  bool HasPendingCrashReport() const;

  /// Short human-readable info about the pending crash.
  QString PendingCrashInfo() const;

  /// Get the full pending crash report details.
  auto GetPendingCrashReport() const -> const CrashReport*;

  /// Mark the pending report as submitted (user consented).
  Q_INVOKABLE void MarkSubmitted();

  /// Dismiss the pending report without submitting.
  Q_INVOKABLE void DismissPending();

  /// Get the minidump directory.
  auto GetMinidumpDirectory() const -> std::filesystem::path;

  /// Set the minidump directory (override default).
  void SetMinidumpDirectory(const std::filesystem::path& path);

  /// Assemble crash report from a minidump file.
  auto AssembleCrashReport(const std::filesystem::path& minidump_path) const -> CrashReport;

  /// Collect system information for crash report.
  static auto CollectSystemInfo() -> std::string;

  /// Collect GPU information for crash report.
  static auto CollectGpuInfo() -> std::string;

  /// Collect recent log lines.
  static auto CollectLogTail(size_t max_lines = 50) -> std::vector<std::string>;

  /// Export crash report to a file for manual inspection.
  auto ExportCrashReport(const CrashReport& report,
                         const std::filesystem::path& output_path) const -> bool;

 signals:
  void PendingCrashChanged();
  void CrashDetected(const QString& crash_id);

 private:
  void SetupCrashHandler();
  auto DefaultMinidumpDirectory() const -> std::filesystem::path;
  void CleanupOldMinidumps(size_t keep_count = 5);

  std::filesystem::path              minidump_dir_;
  std::unique_ptr<CrashReport>       pending_report_;
  bool                               initialized_ = false;

  static CrashReporter*              instance_;
};

}  // namespace alcedo
