//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "ui/alcedo_main/user_notification.hpp"

#include <QApplication>
#include <QDateTime>
#include <QMessageBox>
#include <QMetaObject>
#include <QVariant>
#include <QVariantMap>

namespace alcedo::ui {

auto UserNotificationManager::Instance() -> UserNotificationManager& {
  static UserNotificationManager mgr;
  return mgr;
}

UserNotificationManager::UserNotificationManager(QObject* parent) : QObject(parent) {}

void UserNotificationManager::Notify(NotificationSeverity severity,
                                      const QString&       title,
                                      const QString&       message,
                                      const QString&       category,
                                      bool                 actionable,
                                      const QString&       action_label,
                                      std::function<void()> action_callback) {
  NotificationEntry entry;
  entry.id              = next_id_++;
  entry.severity        = severity;
  entry.title           = title;
  entry.message         = message;
  entry.category        = category;
  entry.timestamp       = QDateTime::currentDateTime();
  entry.dismissed       = false;
  entry.actionable      = actionable;
  entry.action_label    = action_label;
  entry.action_callback = std::move(action_callback);

  notifications_.push_back(std::move(entry));

  // Trim if exceeding max history size
  while (notifications_.size() > kMaxHistorySize) {
    notifications_.removeFirst();
  }

  emit NotificationPosted(notifications_.last().id,
                           static_cast<int>(severity), title, message);

  // For critical notifications, also emit a separate signal and show a dialog
  if (severity == NotificationSeverity::Critical) {
    emit CriticalNotification(notifications_.last().id, title, message);
  }

  emit NotificationsChanged();
}

void UserNotificationManager::Info(const QString& title, const QString& message,
                                    const QString& category) {
  Notify(NotificationSeverity::Info, title, message, category);
}

void UserNotificationManager::Warning(const QString& title, const QString& message,
                                       const QString& category) {
  Notify(NotificationSeverity::Warning, title, message, category);
}

void UserNotificationManager::Error(const QString& title, const QString& message,
                                     const QString& category) {
  Notify(NotificationSeverity::Error, title, message, category);
}

void UserNotificationManager::Critical(const QString& title, const QString& message,
                                        const QString& category) {
  Notify(NotificationSeverity::Critical, title, message, category);
}

void UserNotificationManager::Dismiss(int id) {
  for (auto& entry : notifications_) {
    if (entry.id == id) {
      entry.dismissed = true;
      emit NotificationsChanged();
      return;
    }
  }
}

void UserNotificationManager::DismissAll() {
  for (auto& entry : notifications_) {
    entry.dismissed = true;
  }
  emit NotificationsChanged();
}

auto UserNotificationManager::ActiveNotifications() const -> QVariantList {
  QVariantList result;
  for (const auto& entry : notifications_) {
    if (entry.dismissed) continue;
    QVariantMap vm;
    vm["id"]        = entry.id;
    vm["severity"]  = static_cast<int>(entry.severity);
    vm["severityKey"] = SeverityToKey(entry.severity);
    vm["title"]     = entry.title;
    vm["message"]   = entry.message;
    vm["category"]  = entry.category;
    vm["timestamp"] = entry.timestamp;
    vm["actionable"] = entry.actionable;
    vm["actionLabel"] = entry.action_label;
    result.append(vm);
  }
  return result;
}

QVariantList UserNotificationManager::History(int limit) const {
  QVariantList result;
  int count = 0;
  for (auto it = notifications_.rbegin(); it != notifications_.rend() && count < limit;
       ++it, ++count) {
    QVariantMap vm;
    vm["id"]        = it->id;
    vm["severity"]  = static_cast<int>(it->severity);
    vm["severityKey"] = SeverityToKey(it->severity);
    vm["title"]     = it->title;
    vm["message"]   = it->message;
    vm["category"]  = it->category;
    vm["timestamp"] = it->timestamp;
    vm["dismissed"] = it->dismissed;
    result.append(vm);
  }
  return result;
}

int UserNotificationManager::UnreadCount() const {
  int count = 0;
  for (const auto& entry : notifications_) {
    if (!entry.dismissed) ++count;
  }
  return count;
}

void UserNotificationManager::ClearHistory() {
  notifications_.clear();
  emit NotificationsChanged();
}

auto UserNotificationManager::SeverityToString(NotificationSeverity severity) -> QString {
  switch (severity) {
    case NotificationSeverity::Info:     return tr("Info");
    case NotificationSeverity::Warning:  return tr("Warning");
    case NotificationSeverity::Error:    return tr("Error");
    case NotificationSeverity::Critical: return tr("Critical");
  }
  return tr("Unknown");
}

auto UserNotificationManager::SeverityToKey(NotificationSeverity severity) -> QString {
  switch (severity) {
    case NotificationSeverity::Info:     return QStringLiteral("info");
    case NotificationSeverity::Warning:  return QStringLiteral("warning");
    case NotificationSeverity::Error:    return QStringLiteral("error");
    case NotificationSeverity::Critical: return QStringLiteral("critical");
  }
  return QStringLiteral("info");
}

}  // namespace alcedo::ui
