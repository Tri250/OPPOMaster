//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "app/crash_reporter.hpp"

#include <csignal>
#include <cstdlib>
#include <filesystem>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>

#include <QDateTime>
#include <QDir>
#include <QFile>
#include <QJsonDocument>
#include <QJsonObject>
#include <QProcess>
#include <QStandardPaths>

#include "utils/diagnostics/app_logging.hpp"

#ifdef __linux__
#include <execinfo.h>
#include <unistd.h>
#endif

namespace alcedo {

CrashReporter* CrashReporter::instance_ = nullptr;

CrashReporter::CrashReporter(QObject* parent) : QObject(parent) {
  instance_ = this;
  minidump_dir_ = DefaultMinidumpDirectory();
}

CrashReporter::~CrashReporter() {
  if (instance_ == this) {
    instance_ = nullptr;
  }
}

auto CrashReporter::DefaultMinidumpDirectory() const -> std::filesystem::path {
  const QString data_dir =
      QStandardPaths::writableLocation(QStandardPaths::AppDataLocation);
  return std::filesystem::path(data_dir.toStdString()) / "crash_reports";
}

void CrashReporter::Initialize() {
  if (initialized_) {
    return;
  }

  // Create minidump directory
  std::error_code ec;
  std::filesystem::create_directories(minidump_dir_, ec);

  SetupCrashHandler();
  initialized_ = true;

  qCInfo(diag::appLog) << "CrashReporter initialized. Minidump dir:"
                            << minidump_dir_.c_str();
}

void CrashReporter::SetupCrashHandler() {
  // Install signal handlers for common crash signals
  // In a production build, this would integrate Breakpad/Crashpad
  auto crash_handler = [](int sig) {
    if (instance_ != nullptr) {
      // Save a crash marker file
      auto crash_dir = instance_->minidump_dir_;
      std::string sig_name;
      switch (sig) {
        case SIGSEGV: sig_name = "SIGSEGV"; break;
        case SIGABRT: sig_name = "SIGABRT"; break;
        case SIGFPE:  sig_name = "SIGFPE";  break;
        case SIGILL:  sig_name = "SIGILL";  break;
        default:      sig_name = "SIGNAL_" + std::to_string(sig); break;
      }

      auto now = std::chrono::system_clock::now();
      auto timestamp = std::chrono::system_clock::to_time_t(now);
      std::string crash_id = sig_name + "_" + std::to_string(timestamp);

      std::filesystem::path marker_path = crash_dir / (crash_id + ".marker");
      std::ofstream marker(marker_path);
      if (marker.is_open()) {
        marker << "signal=" << sig_name << "\n";
        marker << "timestamp=" << timestamp << "\n";
        marker << "app_version=" ALCEDO_APP_VERSION "\n";

        // Capture stack trace on Linux
#ifdef __linux__
        void*  buffer[64];
        int    nframes = backtrace(buffer, 64);
        char** symbols = backtrace_symbols(buffer, nframes);
        marker << "stack_trace:\n";
        for (int i = 0; i < nframes; ++i) {
          marker << "  " << symbols[i] << "\n";
        }
        std::free(symbols);
#endif
        marker.close();
      }
    }

    // Re-raise the signal to get default behavior (core dump)
    std::signal(sig, SIG_DFL);
    std::raise(sig);
  };

  std::signal(SIGSEGV, crash_handler);
  std::signal(SIGABRT, crash_handler);
  std::signal(SIGFPE,  crash_handler);
  std::signal(SIGILL,  crash_handler);
}

void CrashReporter::CheckForPendingCrashes() {
  if (!std::filesystem::exists(minidump_dir_)) {
    return;
  }

  // Find the most recent marker file
  std::filesystem::path latest_marker;
  std::filesystem::file_time_type latest_time;

  for (const auto& entry : std::filesystem::directory_iterator(minidump_dir_)) {
    if (entry.path().extension() == ".marker") {
      auto ftime = std::filesystem::last_write_time(entry);
      if (latest_marker.empty() || ftime > latest_time) {
        latest_time  = ftime;
        latest_marker = entry.path();
      }
    }
  }

  if (latest_marker.empty()) {
    return;
  }

  // Assemble a crash report from the marker
  pending_report_ = std::make_unique<CrashReport>();
  pending_report_->minidump_path = latest_marker;

  std::ifstream ifs(latest_marker);
  std::string   line;
  while (std::getline(ifs, line)) {
    if (line.find("signal=") == 0) {
      pending_report_->crash_type = line.substr(7);
    } else if (line.find("timestamp=") == 0) {
      pending_report_->crash_time = line.substr(10);
    } else if (line.find("app_version=") == 0) {
      pending_report_->app_version = line.substr(12);
    } else if (line.find("  ") == 0) {
      pending_report_->stack_trace += line + "\n";
    }
  }

  pending_report_->crash_id = latest_marker.stem().string();
  pending_report_->os_name    = "Linux";
  pending_report_->gpu_info   = CollectGpuInfo();
  pending_report_->log_tail   = CollectLogTail();

  qCInfo(diag::appLog) << "Pending crash report found:"
                            << pending_report_->crash_id.c_str();
  emit PendingCrashChanged();
}

bool CrashReporter::HasPendingCrashReport() const {
  return pending_report_ != nullptr;
}

QString CrashReporter::PendingCrashInfo() const {
  if (!pending_report_) {
    return {};
  }
  return tr("Crash detected: %1 at %2")
      .arg(QString::fromStdString(pending_report_->crash_type),
           QString::fromStdString(pending_report_->crash_time));
}

auto CrashReporter::GetPendingCrashReport() const -> const CrashReport* {
  return pending_report_.get();
}

void CrashReporter::MarkSubmitted() {
  if (pending_report_) {
    pending_report_->submitted = true;

    // Remove the marker file
    if (std::filesystem::exists(pending_report_->minidump_path)) {
      std::filesystem::remove(pending_report_->minidump_path);
    }

    pending_report_.reset();
    emit PendingCrashChanged();
  }
}

void CrashReporter::DismissPending() {
  if (pending_report_) {
    // Remove the marker file
    if (std::filesystem::exists(pending_report_->minidump_path)) {
      std::filesystem::remove(pending_report_->minidump_path);
    }
    pending_report_.reset();
    emit PendingCrashChanged();
  }
}

auto CrashReporter::GetMinidumpDirectory() const -> std::filesystem::path {
  return minidump_dir_;
}

void CrashReporter::SetMinidumpDirectory(const std::filesystem::path& path) {
  minidump_dir_ = path;
}

auto CrashReporter::AssembleCrashReport(const std::filesystem::path& minidump_path) const
    -> CrashReport {
  CrashReport report;
  report.minidump_path = minidump_path;
  report.crash_id      = minidump_path.stem().string();
  report.app_version   = ALCEDO_APP_VERSION;
  report.os_name       = "Linux";
  report.os_version    = CollectSystemInfo();
  report.gpu_info      = CollectGpuInfo();
  report.log_tail      = CollectLogTail();
  report.crash_time    = std::to_string(
      std::chrono::system_clock::to_time_t(std::chrono::system_clock::now()));
  return report;
}

auto CrashReporter::CollectSystemInfo() -> std::string {
  std::string result;

  // Read /etc/os-release
  std::ifstream ifs("/etc/os-release");
  if (ifs.is_open()) {
    std::string line;
    while (std::getline(ifs, line)) {
      if (line.find("PRETTY_NAME=") == 0) {
        // Remove quotes
        if (line.size() > 13) {
          result = line.substr(13);
          if (result.front() == '"') result = result.substr(1);
          if (result.back() == '"') result.pop_back();
        }
        break;
      }
    }
  }

  // Add architecture
#if defined(__x86_64__)
  result += " (x86_64)";
#elif defined(__aarch64__)
  result += " (aarch64)";
#endif

  return result;
}

auto CrashReporter::CollectGpuInfo() -> std::string {
  // Try to get GPU info from system
  QProcess process;
  process.start(QStringLiteral("lspci"), {QStringLiteral("-m")});
  process.waitForFinished(3000);
  const QString output = process.readAllStandardOutput();

  for (const QString& line : output.split('\n')) {
    if (line.contains(QStringLiteral("VGA"), Qt::CaseInsensitive) ||
        line.contains(QStringLiteral("3D"), Qt::CaseInsensitive)) {
      return line.toStdString();
    }
  }
  return "Unknown GPU";
}

auto CrashReporter::CollectLogTail(size_t max_lines) -> std::vector<std::string> {
  // This would integrate with the app logging system
  // For now, read from the log file if available
  std::vector<std::string> lines;
  const QString log_dir = QStandardPaths::writableLocation(QStandardPaths::AppDataLocation);
  const std::filesystem::path log_path =
      std::filesystem::path(log_dir.toStdString()) / "alcedo.log";

  std::ifstream ifs(log_path);
  if (!ifs.is_open()) {
    return lines;
  }

  // Read all lines, then take the last max_lines
  std::vector<std::string> all_lines;
  std::string line;
  while (std::getline(ifs, line)) {
    all_lines.push_back(std::move(line));
  }

  size_t start = all_lines.size() > max_lines ? all_lines.size() - max_lines : 0;
  for (size_t i = start; i < all_lines.size(); ++i) {
    lines.push_back(std::move(all_lines[i]));
  }

  return lines;
}

auto CrashReporter::ExportCrashReport(const CrashReport& report,
                                       const std::filesystem::path& output_path) const -> bool {
  QJsonObject root;
  root["crash_id"]    = QString::fromStdString(report.crash_id);
  root["app_version"] = QString::fromStdString(report.app_version);
  root["os_name"]     = QString::fromStdString(report.os_name);
  root["os_version"]  = QString::fromStdString(report.os_version);
  root["gpu_info"]    = QString::fromStdString(report.gpu_info);
  root["crash_time"]  = QString::fromStdString(report.crash_time);
  root["crash_type"]  = QString::fromStdString(report.crash_type);
  root["stack_trace"] = QString::fromStdString(report.stack_trace);

  QJsonArray log_array;
  for (const auto& line : report.log_tail) {
    log_array.append(QString::fromStdString(line));
  }
  root["log_tail"] = log_array;
  root["submitted"] = report.submitted;

  QFile file(QString::fromStdString(output_path.string()));
  if (!file.open(QIODevice::WriteOnly | QIODevice::Truncate)) {
    return false;
  }
  file.write(QJsonDocument(root).toJson());
  return true;
}

void CrashReporter::CleanupOldMinidumps(size_t keep_count) {
  if (!std::filesystem::exists(minidump_dir_)) {
    return;
  }

  std::vector<std::filesystem::path> markers;
  for (const auto& entry : std::filesystem::directory_iterator(minidump_dir_)) {
    if (entry.path().extension() == ".marker") {
      markers.push_back(entry.path());
    }
  }

  // Sort by modification time (oldest first)
  std::sort(markers.begin(), markers.end(), [](const auto& a, const auto& b) {
    return std::filesystem::last_write_time(a) < std::filesystem::last_write_time(b);
  });

  // Remove oldest markers beyond keep_count
  while (markers.size() > keep_count) {
    std::filesystem::remove(markers.front());
    markers.erase(markers.begin());
  }
}

}  // namespace alcedo
