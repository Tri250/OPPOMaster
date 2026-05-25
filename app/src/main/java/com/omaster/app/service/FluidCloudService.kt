package com.omaster.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.omaster.app.MainActivity
import com.omaster.app.R
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class FluidCloudService : Service() {
    
    companion object {
        const val ACTION_SHOW_CAPSULE = "com.omaster.app.action.SHOW_CAPSULE"
        const val ACTION_HIDE_CAPSULE = "com.omaster.app.action.HIDE_CAPSULE"
        const val ACTION_APPLY_PRESET = "com.omaster.app.action.APPLY_PRESET"
        const val EXTRA_PRESET_ID = "preset_id"
        const val EXTRA_PRESET_NAME = "preset_name"
        const val EXTRA_PRESET_PARAMS = "preset_params"
        private const val NOTIFICATION_CHANNEL_ID = "fluid_cloud_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private var currentPresetId: String? = null
    private var currentPresetName: String? = null
    private var currentPresetParams: String? = null

    override fun onCreate() {
        super.onCreate()
        Timber.d("FluidCloudService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("FluidCloudService onStartCommand with action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_SHOW_CAPSULE -> {
                currentPresetId = intent.getStringExtra(EXTRA_PRESET_ID)
                currentPresetName = intent.getStringExtra(EXTRA_PRESET_NAME)
                currentPresetParams = intent.getStringExtra(EXTRA_PRESET_PARAMS)
                showFluidCloudCapsule()
            }
            ACTION_HIDE_CAPSULE -> {
                hideFluidCloudCapsule()
            }
            ACTION_APPLY_PRESET -> {
                applyPresetToCamera()
            }
        }
        
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "流体云胶囊",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "OMaster 流体云胶囊服务"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showFluidCloudCapsule() {
        Timber.d("Showing fluid cloud capsule for preset: $currentPresetName")
        
        val applyIntent = Intent(this, FluidCloudService::class.java).apply {
            action = ACTION_APPLY_PRESET
            putExtra(EXTRA_PRESET_ID, currentPresetId)
            putExtra(EXTRA_PRESET_PARAMS, currentPresetParams)
        }
        val applyPendingIntent = PendingIntent.getService(
            this,
            0,
            applyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("OMaster")
            .setContentText(currentPresetName ?: "已选中预设")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("当前预设: ${currentPresetName}\n点击应用到相机"))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(mainPendingIntent)
            .addAction(
                R.drawable.ic_launcher,
                "应用到相机",
                applyPendingIntent
            )
            .setAutoCancel(false)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun hideFluidCloudCapsule() {
        Timber.d("Hiding fluid cloud capsule")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun applyPresetToCamera() {
        Timber.d("Applying preset to camera: $currentPresetId")
        sendBroadcast(Intent("com.omaster.app.ACTION_PRESET_APPLIED").apply {
            putExtra(EXTRA_PRESET_ID, currentPresetId)
            putExtra(EXTRA_PRESET_NAME, currentPresetName)
            putExtra(EXTRA_PRESET_PARAMS, currentPresetParams)
        })
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("FluidCloudService destroyed")
    }
}
