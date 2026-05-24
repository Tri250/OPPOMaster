package com.omaster.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class FluidCloudService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        // 流体云胶囊实现
    }
}
