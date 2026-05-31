package com.omaster.app.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import timber.log.Timber

object AppLifecycleManager {
    
    private val listeners = mutableListOf<AppLifecycleListener>()
    private var isAppInForeground = false
    
    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                isAppInForeground = true
                Timber.d("App entered foreground")
                listeners.forEach { it.onAppForeground() }
            }
            
            override fun onStop(owner: LifecycleOwner) {
                isAppInForeground = false
                Timber.d("App entered background")
                listeners.forEach { it.onAppBackground() }
            }
        })
    }
    
    fun addListener(listener: AppLifecycleListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }
    
    fun removeListener(listener: AppLifecycleListener) {
        listeners.remove(listener)
    }
    
    fun isForeground(): Boolean = isAppInForeground
}

interface AppLifecycleListener {
    fun onAppForeground()
    fun onAppBackground()
}
