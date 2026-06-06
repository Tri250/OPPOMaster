package com.omaster.app.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.omaster.app.MainActivity
import com.omaster.app.R
import com.omaster.app.service.FluidCloudService
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "omaster_floating_window"
        const val CHANNEL_NAME = "悬浮窗控制"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_EXPAND = "com.omaster.app.action.EXPAND"
        const val ACTION_COLLAPSE = "com.omaster.app.action.COLLAPSE"
        const val ACTION_NEXT_PRESET = "com.omaster.app.action.NEXT_PRESET"
        const val ACTION_PREV_PRESET = "com.omaster.app.action.PREV_PRESET"
        const val ACTION_CLOSE = "com.omaster.app.action.CLOSE"
    }
    
    private var isNotificationShowing = false
    
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于控制悬浮窗的常驻通知"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showFloatingWindowNotification() {
        if (isNotificationShowing) {
            Timber.d("Notification already showing")
            return
        }
        
        try {
            val notification = buildNotification()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            isNotificationShowing = true
            Timber.d("Floating window notification shown")
        } catch (e: SecurityException) {
            Timber.e(e, "Failed to show notification: permission denied")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show notification")
        }
    }
    
    fun hideFloatingWindowNotification() {
        if (!isNotificationShowing) {
            Timber.d("Notification not showing")
            return
        }
        
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            isNotificationShowing = false
            Timber.d("Floating window notification hidden")
        } catch (e: Exception) {
            Timber.e(e, "Failed to hide notification")
        }
    }
    
    fun updateNotification(presetName: String? = null, isExpanded: Boolean = true) {
        if (!isNotificationShowing) {
            showFloatingWindowNotification()
            return
        }
        
        try {
            val notification = buildNotification(presetName, isExpanded)
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            Timber.d("Notification updated: preset=$presetName, expanded=$isExpanded")
        } catch (e: Exception) {
            Timber.e(e, "Failed to update notification")
        }
    }
    
    private fun buildNotification(presetName: String? = null, isExpanded: Boolean = true): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val expandIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, FluidCloudService::class.java).apply {
                action = FluidCloudService.ACTION_SHOW_CAPSULE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val collapseIntent = PendingIntent.getService(
            context,
            2,
            Intent(context, FluidCloudService::class.java).apply {
                action = FluidCloudService.ACTION_HIDE_CAPSULE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val nextIntent = PendingIntent.getService(
            context,
            3,
            Intent(context, FluidCloudService::class.java).apply {
                action = ACTION_NEXT_PRESET
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val prevIntent = PendingIntent.getService(
            context,
            4,
            Intent(context, FluidCloudService::class.java).apply {
                action = ACTION_PREV_PRESET
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val closeIntent = PendingIntent.getService(
            context,
            5,
            Intent(context, FluidCloudService::class.java).apply {
                action = ACTION_CLOSE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = presetName ?: "OMaster"
        val text = if (isExpanded) "悬浮窗已展开" else "悬浮窗已收起"
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                R.drawable.ic_launcher_foreground,
                if (isExpanded) "收起" else "展开",
                if (isExpanded) collapseIntent else expandIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "上一个",
                prevIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "下一个",
                nextIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "关闭",
                closeIntent
            )
            .build()
    }
    
    fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_EXPAND -> {
                Timber.d("Notification action: EXPAND")
            }
            ACTION_COLLAPSE -> {
                Timber.d("Notification action: COLLAPSE")
            }
            ACTION_NEXT_PRESET -> {
                Timber.d("Notification action: NEXT_PRESET")
            }
            ACTION_PREV_PRESET -> {
                Timber.d("Notification action: PREV_PRESET")
            }
            ACTION_CLOSE -> {
                Timber.d("Notification action: CLOSE")
                hideFloatingWindowNotification()
            }
        }
    }
}
