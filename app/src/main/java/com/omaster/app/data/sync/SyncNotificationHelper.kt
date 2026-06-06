package com.omaster.app.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omaster.app.MainActivity
import com.omaster.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncNotificationDataStore by preferencesDataStore(name = "sync_notification")

/**
 * 同步通知助手
 * 负责同步开始/进度/完成/失败通知，支持点击跳转、进度条显示和静音时段设置
 */
@Singleton
class SyncNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object PreferencesKeys {
        // 静音时段开始时间（小时）
        val QUIET_HOURS_START = intPreferencesKey("quiet_hours_start")
        // 静音时段结束时间（小时）
        val QUIET_HOURS_END = intPreferencesKey("quiet_hours_end")
        // 是否启用静音时段
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        // 最后通知时间
        val LAST_NOTIFICATION_TIME = longPreferencesKey("last_notification_time")
        // 通知音效开关
        val NOTIFICATION_SOUND_ENABLED = booleanPreferencesKey("notification_sound_enabled")
        // 通知震动开关
        val NOTIFICATION_VIBRATION_ENABLED = booleanPreferencesKey("notification_vibration_enabled")
    }

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        const val CHANNEL_ID_SYNC = "sync_channel"
        const val CHANNEL_ID_SYNC_PROGRESS = "sync_progress_channel"
        const val CHANNEL_ID_SYNC_SILENT = "sync_silent_channel"

        const val NOTIFICATION_ID_SYNC_START = 1001
        const val NOTIFICATION_ID_SYNC_PROGRESS = 1002
        const val NOTIFICATION_ID_SYNC_COMPLETE = 1003
        const val NOTIFICATION_ID_SYNC_ERROR = 1004

        const val DEFAULT_QUIET_HOURS_START = 22 // 默认22:00开始静音
        const val DEFAULT_QUIET_HOURS_END = 8    // 默认08:00结束静音
    }

    // 静音时段设置
    val quietHoursStart: Flow<Int> = context.syncNotificationDataStore.data
        .map { it[PreferencesKeys.QUIET_HOURS_START] ?: DEFAULT_QUIET_HOURS_START }

    val quietHoursEnd: Flow<Int> = context.syncNotificationDataStore.data
        .map { it[PreferencesKeys.QUIET_HOURS_END] ?: DEFAULT_QUIET_HOURS_END }

    val quietHoursEnabled: Flow<Boolean> = context.syncNotificationDataStore.data
        .map { it[PreferencesKeys.QUIET_HOURS_ENABLED] ?: true }

    val notificationSoundEnabled: Flow<Boolean> = context.syncNotificationDataStore.data
        .map { it[PreferencesKeys.NOTIFICATION_SOUND_ENABLED] ?: true }

    val notificationVibrationEnabled: Flow<Boolean> = context.syncNotificationDataStore.data
        .map { it[PreferencesKeys.NOTIFICATION_VIBRATION_ENABLED] ?: true }

    init {
        createNotificationChannels()
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 同步状态通知渠道
            val syncChannel = NotificationChannel(
                CHANNEL_ID_SYNC,
                "同步通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "显示同步开始、完成和失败的通知"
                setShowBadge(true)
            }

            // 同步进度通知渠道（低重要性，不打扰用户）
            val progressChannel = NotificationChannel(
                CHANNEL_ID_SYNC_PROGRESS,
                "同步进度",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示同步进度更新"
                setShowBadge(false)
            }

            // 静音时段通知渠道（无声音）
            val silentChannel = NotificationChannel(
                CHANNEL_ID_SYNC_SILENT,
                "同步通知（静音）",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "静音时段的同步通知"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(syncChannel, progressChannel, silentChannel)
            )
        }
    }

    /**
     * 显示同步开始通知
     */
    suspend fun showSyncStartNotification(message: String = "正在开始同步...") {
        if (shouldSuppressNotification()) {
            Timber.d("处于静音时段，跳过同步开始通知")
            return
        }

        val channelId = if (isInQuietHours()) CHANNEL_ID_SYNC_SILENT else CHANNEL_ID_SYNC
        val pendingIntent = createMainActivityPendingIntent()

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_sync)
            .setContentTitle("数据同步")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .apply {
                if (isInQuietHours()) {
                    setSilent(true)
                } else {
                    applyNotificationSettings(this)
                }
            }
            .build()

        notificationManager.notify(NOTIFICATION_ID_SYNC_START, notification)
        updateLastNotificationTime()
    }

    /**
     * 显示同步进度通知
     */
    suspend fun showSyncProgressNotification(
        progress: Int,
        total: Int,
        message: String = "正在同步数据..."
    ) {
        val percentage = if (total > 0) (progress * 100 / total) else 0
        val channelId = CHANNEL_ID_SYNC_PROGRESS

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_sync)
            .setContentTitle("同步进度")
            .setContentText("$message ($progress/$total)")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(100, percentage, false)
            .setSilent(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_SYNC_PROGRESS, notification)
    }

    /**
     * 更新同步进度通知
     */
    suspend fun updateSyncProgress(progress: Int, total: Int, message: String? = null) {
        showSyncProgressNotification(progress, total, message ?: "正在同步数据...")
    }

    /**
     * 显示同步完成通知
     */
    suspend fun showSyncCompleteNotification(
        successCount: Int = 0,
        message: String = "同步完成"
    ) {
        // 取消进度通知
        notificationManager.cancel(NOTIFICATION_ID_SYNC_PROGRESS)
        notificationManager.cancel(NOTIFICATION_ID_SYNC_START)

        if (shouldSuppressNotification()) {
            Timber.d("处于静音时段，跳过同步完成通知")
            return
        }

        val channelId = if (isInQuietHours()) CHANNEL_ID_SYNC_SILENT else CHANNEL_ID_SYNC
        val pendingIntent = createMainActivityPendingIntent()

        val contentText = if (successCount > 0) {
            "$message，成功同步 $successCount 项数据"
        } else {
            message
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_sync_done)
            .setContentTitle("同步完成")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .apply {
                if (isInQuietHours()) {
                    setSilent(true)
                } else {
                    applyNotificationSettings(this)
                }
            }
            .build()

        notificationManager.notify(NOTIFICATION_ID_SYNC_COMPLETE, notification)
        updateLastNotificationTime()
    }

    /**
     * 显示同步失败通知
     */
    suspend fun showSyncErrorNotification(
        errorMessage: String,
        retryAction: (() -> Unit)? = null
    ) {
        // 取消进度通知
        notificationManager.cancel(NOTIFICATION_ID_SYNC_PROGRESS)
        notificationManager.cancel(NOTIFICATION_ID_SYNC_START)

        if (shouldSuppressNotification()) {
            Timber.d("处于静音时段，跳过同步失败通知")
            return
        }

        val channelId = if (isInQuietHours()) CHANNEL_ID_SYNC_SILENT else CHANNEL_ID_SYNC
        val pendingIntent = createMainActivityPendingIntent()

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_sync_error)
            .setContentTitle("同步失败")
            .setContentText(errorMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)

        // 添加重试操作
        if (retryAction != null) {
            val retryPendingIntent = createRetryPendingIntent()
            builder.addAction(
                R.drawable.ic_refresh,
                "重试",
                retryPendingIntent
            )
        }

        builder.apply {
            if (isInQuietHours()) {
                setSilent(true)
            } else {
                applyNotificationSettings(this)
            }
        }

        notificationManager.notify(NOTIFICATION_ID_SYNC_ERROR, builder.build())
        updateLastNotificationTime()
    }

    /**
     * 显示无变化通知
     */
    suspend fun showNoChangesNotification() {
        if (shouldSuppressNotification()) {
            return
        }

        val channelId = if (isInQuietHours()) CHANNEL_ID_SYNC_SILENT else CHANNEL_ID_SYNC
        val pendingIntent = createMainActivityPendingIntent()

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_sync_done)
            .setContentTitle("同步检查")
            .setContentText("数据已是最新，无需同步")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSilent(isInQuietHours())
            .build()

        notificationManager.notify(NOTIFICATION_ID_SYNC_COMPLETE, notification)
    }

    /**
     * 取消所有同步通知
     */
    fun cancelAllSyncNotifications() {
        notificationManager.cancel(NOTIFICATION_ID_SYNC_START)
        notificationManager.cancel(NOTIFICATION_ID_SYNC_PROGRESS)
        notificationManager.cancel(NOTIFICATION_ID_SYNC_COMPLETE)
        notificationManager.cancel(NOTIFICATION_ID_SYNC_ERROR)
    }

    /**
     * 取消特定通知
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * 设置静音时段
     */
    suspend fun setQuietHours(startHour: Int, endHour: Int, enabled: Boolean) {
        context.syncNotificationDataStore.edit { preferences ->
            preferences[PreferencesKeys.QUIET_HOURS_START] = startHour
            preferences[PreferencesKeys.QUIET_HOURS_END] = endHour
            preferences[PreferencesKeys.QUIET_HOURS_ENABLED] = enabled
        }
        Timber.d("设置静音时段: $startHour:00 - $endHour:00, 启用: $enabled")
    }

    /**
     * 设置通知音效
     */
    suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        context.syncNotificationDataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_SOUND_ENABLED] = enabled
        }
    }

    /**
     * 设置通知震动
     */
    suspend fun setNotificationVibrationEnabled(enabled: Boolean) {
        context.syncNotificationDataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_VIBRATION_ENABLED] = enabled
        }
    }

    /**
     * 检查是否在静音时段
     */
    suspend fun isInQuietHours(): Boolean {
        if (!quietHoursEnabled.first()) return false

        val startHour = quietHoursStart.first()
        val endHour = quietHoursEnd.first()
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        return if (startHour <= endHour) {
            // 例如 22:00 - 08:00（跨天）
            currentHour >= startHour || currentHour < endHour
        } else {
            // 例如 08:00 - 22:00（当天）
            currentHour in endHour until startHour
        }
    }

    /**
     * 检查是否应该抑制通知（静音时段或短时间内已发送过）
     */
    private suspend fun shouldSuppressNotification(): Boolean {
        // 检查是否在静音时段
        if (isInQuietHours()) {
            return true
        }

        // 检查是否在短时间内重复发送（5分钟内）
        val lastTime = context.syncNotificationDataStore.data
            .map { it[PreferencesKeys.LAST_NOTIFICATION_TIME] ?: 0L }
            .first()

        val fiveMinutesInMillis = 5 * 60 * 1000L
        return (System.currentTimeMillis() - lastTime) < fiveMinutesInMillis
    }

    /**
     * 更新最后通知时间
     */
    private suspend fun updateLastNotificationTime() {
        context.syncNotificationDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_NOTIFICATION_TIME] = System.currentTimeMillis()
        }
    }

    /**
     * 创建跳转到主界面的PendingIntent
     */
    private fun createMainActivityPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_sync", true)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            flags
        )
    }

    /**
     * 创建重试操作的PendingIntent
     */
    private fun createRetryPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action_retry_sync", true)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getActivity(
            context,
            1,
            intent,
            flags
        )
    }

    /**
     * 应用通知设置（音效和震动）
     */
    private suspend fun applyNotificationSettings(builder: NotificationCompat.Builder) {
        val soundEnabled = notificationSoundEnabled.first()
        val vibrationEnabled = notificationVibrationEnabled.first()

        if (!soundEnabled) {
            builder.setSilent(true)
        }

        if (vibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 300, 200, 300))
        } else {
            builder.setVibrate(null)
        }
    }

    /**
     * 获取通知渠道设置状态
     */
    fun isNotificationChannelEnabled(channelId: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(channelId)
            channel?.importance != NotificationManager.IMPORTANCE_NONE
        } else {
            true
        }
    }

    /**
     * 打开通知设置页面
     */
    fun openNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS.apply {
                val bundle = android.os.Bundle()
                bundle.putString(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(
                Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }
    }

    /**
     * 获取当前静音时段描述
     */
    suspend fun getQuietHoursDescription(): String {
        if (!quietHoursEnabled.first()) {
            return "未启用"
        }

        val start = quietHoursStart.first()
        val end = quietHoursEnd.first()
        return String.format("%02d:00 - %02d:00", start, end)
    }
}
