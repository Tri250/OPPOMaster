package com.omaster.app

import android.app.Application
import com.omaster.app.analytics.PrivacyCompliantAnalytics
import com.omaster.app.data.PresetRepository
import com.omaster.app.data.secure.SecurePreferences
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class OMasterApplication : Application() {

    @Inject
    lateinit var analytics: PrivacyCompliantAnalytics

    @Inject
    lateinit var presetRepository: PresetRepository

    @Inject
    lateinit var securePreferences: SecurePreferences

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        try {
            analytics.preInitialize()
            if (securePreferences.hasUserAgreedPrivacy()) {
                analytics.initializeIfAgreed()
            }
        } catch (e: Exception) {
            Timber.e(e, "Analytics pre-initialization failed")
        }

        applicationScope.launch {
            try {
                presetRepository.initializeDatabase()
            } catch (e: Exception) {
                Timber.e(e, "Database initialization failed")
            }
        }
    }
}
