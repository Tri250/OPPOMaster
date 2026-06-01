package com.omaster.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class FluidCloudService : Service() {
    
    companion object {
        const val ACTION_SHOW_CAPSULE = "com.omaster.app.action.SHOW_CAPSULE"
        const val ACTION_HIDE_CAPSULE = "com.omaster.app.action.HIDE_CAPSULE"
        const val EXTRA_PRESET_ID = "preset_id"
        const val EXTRA_PRESET_NAME = "preset_name"
    }

    private var currentPresetId: String? = null
    private var currentPresetName: String? = null

    override fun onCreate() {
        super.onCreate()
        Timber.d("FluidCloudService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("FluidCloudService onStartCommand with action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_SHOW_CAPSULE -> {
                currentPresetId = intent.getStringExtra(EXTRA_PRESET_ID)
                currentPresetName = intent.getStringExtra(EXTRA_PRESET_NAME)
                showFluidCloudCapsule()
            }
            ACTION_HIDE_CAPSULE -> {
                hideFluidCloudCapsule()
            }
        }
        
        return START_STICKY
    }

    private fun showFluidCloudCapsule() {
        Timber.d("Showing fluid cloud capsule for preset: $currentPresetName")
    }

    private fun hideFluidCloudCapsule() {
        Timber.d("Hiding fluid cloud capsule")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("FluidCloudService destroyed")
    }
}
