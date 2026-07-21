//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#pragma once

#include <QDateTime>
#include <QObject>
#include <QString>
#include <QTimer>
#include <QVector>

#include <chrono>
#include <functional>

namespace alcedo::ui {

/// Severity level for user-facing notifications.
enum class NotificationSeverity {
  Info     = 0,
  Warning  = 1,
  Error    = 2,
  Critical = 3,
};

/// A single user-visible notification entry.
struct NotificationEntry {
  int                  id        = 0;
  NotificationSeverity severity  = NotificationSeverity::Info;
  QString              title;
  QString              message;
  QString              category;    // e.g. "export", "import", "gpu", "ai", "project"
  QDateTime            timestamp;
  bool                 dismissed  = false;
  bool                 actionable = false;
  QString              action_label;
  std::function<void()> action_callback;
};

/// Centralized user notification system.
///
/// Provides:
/// - Non-modal toast notifications for transient issues (Info/Warning)
/// - Modal dialogs for critical errors (Critical)
/// - Notification history/log
/// - Category-based filtering
class UserNotificationManager : public QObject {
  Q_OBJECT

  Q_PROPERTY(QVariantList activeNotifications READ ActiveNotifications NOTIFY NotificationsChanged)
  Q_PROPERTY(int unreadCount READ UnreadCount NOTIFY NotificationsChanged)

 public:
  static auto Instance() -> UserNotificationManager&;

  /// Post a notification.
  void Notify(NotificationSeverity severity,
              const QString&       title,
              const QString&       message,
              const QString&       category  = {},
              bool                 actionable = false,
              const QString&       action_label = {},
              std::function<void()> action_callback = {});

  /// Convenience methods.
  void Info(const QString& title, const QString& message, const QString& category = {});
  void Warning(const QString& title, const QString& message, const QString& category = {});
  void Error(const QString& title, const QString& message, const QString& category = {});
  void Critical(const QString& title, const QString& message, const QString& category = {});

  /// Dismiss a notification by id.
  Q_INVOKABLE void Dismiss(int id);

  /// Dismiss all notifications.
  Q_INVOKABLE void DismissAll();

  /// Get all non-dismissed notifications.
  auto ActiveNotifications() const -> QVariantList;

  /// Get notification history (including dismissed).
  Q_INVOKABLE QVariantList History(int limit = 100) const;

  /// Number of unread (non-dismissed) notifications.
  int UnreadCount() const;

  /// Clear the entire history.
  Q_INVOKABLE void ClearHistory();

  /// Severity as a display string.
  static auto SeverityToString(NotificationSeverity severity) -> QString;

  /// Severity as a QML-friendly string key.
  static auto SeverityToKey(NotificationSeverity severity) -> QString;

 signals:
  void NotificationsChanged();
  void NotificationPosted(int id, int severity, const QString& title, const QString& message);
  void CriticalNotification(int id, const QString& title, const QString& message);

 private:
  explicit UserNotificationManager(QObject* parent = nullptr);

  int                                   next_id_ = 1;
  QVector<NotificationEntry>            notifications_;
  static constexpr int                  kMaxHistorySize = 500;
};

}  // namespace alcedo::ui
