package com.omaster.app.analytics

import android.content.Context
import com.omaster.app.BuildConfig
import com.omaster.app.data.secure.SecurePreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class AnalyticsEvent(
    val name: String,
    val params: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class AnalyticsEventType {
    PRESET_APPLIED,
    PRESET_FAVORITED,
    PRESET_CREATED,
    PRESET_DELETED,
    FLOATING_WINDOW_OPENED,
    FLOATING_WINDOW_CLOSED,
    SCREEN_VIEWED,
    SETTING_CHANGED,
    UPDATE_CHECKED,
    ERROR_ENCOUNTERED
}

@Singleton
class PrivacyCompliantAnalytics @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePreferences: SecurePreferences
) {
    private var isInitialized = false
    private var isPaused = false
    private val pendingEvents = mutableListOf<AnalyticsEvent>()

    fun preInitialize() {
        try {
            com.umeng.analytics.MobclickAgent.preInit(context, BuildConfig.UMENG_APP_KEY)
            Timber.d("Analytics pre-initialized with AppKey: ${BuildConfig.UMENG_APP_KEY.take(8)}...")
        } catch (e: Exception) {
            Timber.e(e, "Failed to pre-initialize analytics")
        }
    }

    fun initializeIfAgreed() {
        if (securePreferences.hasUserAgreedPrivacy() && securePreferences.isAnalyticsEnabled()) {
            initialize()
        } else {
            Timber.d("Analytics not initialized: privacy not agreed or analytics disabled")
        }
    }

    private fun initialize() {
        if (isInitialized) {
            Timber.d("Analytics already initialized")
            return
        }

        try {
            if (BuildConfig.UMENG_APP_KEY.isBlank()) {
                Timber.w("Umeng AppKey is empty, skipping initialization")
                return
            }

            com.umeng.analytics.MobclickAgent.setPageCollectionMode(
                com.umeng.analytics.MobclickAgent.PageMode.AUTO
            )

            flushPendingEvents()
            isInitialized = true
            Timber.d("Analytics initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize analytics")
        }
    }

    fun updateAnalyticsState(enabled: Boolean) {
        try {
            if (enabled && securePreferences.hasUserAgreedPrivacy()) {
                if (!isInitialized) {
                    initialize()
                }
                resume()
                Timber.d("Analytics enabled")
            } else {
                pause()
                disable()
                Timber.d("Analytics disabled")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update analytics state")
        }
    }

    fun resume() {
        if (isInitialized && !isPaused) {
            try {
                com.umeng.analytics.MobclickAgent.resume(context)
                isPaused = false
            } catch (e: Exception) {
                Timber.e(e, "Failed to resume analytics")
            }
        }
    }

    fun pause() {
        if (isInitialized) {
            try {
                com.umeng.analytics.MobclickAgent.onPause(context)
                isPaused = true
            } catch (e: Exception) {
                Timber.e(e, "Failed to pause analytics")
            }
        }
    }

    fun disable() {
        try {
            com.umeng.analytics.MobclickAgent.disable()
            isInitialized = false
            isPaused = false
            Timber.d("Analytics disabled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to disable analytics")
        }
    }

    fun logEvent(eventType: AnalyticsEventType, params: Map<String, Any> = emptyMap()) {
        val event = AnalyticsEvent(
            name = eventType.name.lowercase(),
            params = params
        )
        logEventInternal(event)
    }

    fun logEvent(event: AnalyticsEvent) {
        logEventInternal(event)
    }

    private fun logEventInternal(event: AnalyticsEvent) {
        if (!isInitialized || !securePreferences.isAnalyticsEnabled() || !securePreferences.hasUserAgreedPrivacy()) {
            Timber.d("Analytics not active, event queued: ${event.name}")
            pendingEvents.add(event)
            trimPendingEvents()
            return
        }

        try {
            if (pendingEvents.isNotEmpty()) {
                flushPendingEvents()
            }

            val params = event.params.mapValues { (_, value) ->
                value.toString()
            }
            com.umeng.analytics.MobclickAgent.onEvent(context, event.name, params)
            Timber.d("Event logged: ${event.name}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to log event: ${event.name}")
        }
    }

    private fun flushPendingEvents() {
        if (pendingEvents.isEmpty()) return

        try {
            pendingEvents.forEach { event ->
                val params = event.params.mapValues { (_, value) ->
                    value.toString()
                }
                com.umeng.analytics.MobclickAgent.onEvent(context, event.name, params)
            }
            Timber.d("Flushed ${pendingEvents.size} pending events")
            pendingEvents.clear()
        } catch (e: Exception) {
            Timber.e(e, "Failed to flush pending events")
        }
    }

    private fun trimPendingEvents() {
        if (pendingEvents.size > MAX_PENDING_EVENTS) {
            val overflow = pendingEvents.size - MAX_PENDING_EVENTS
            pendingEvents.subList(0, overflow).clear()
            Timber.w("Trimmed $overflow pending events")
        }
    }

    fun logScreenView(screenName: String) {
        logEvent(AnalyticsEventType.SCREEN_VIEWED, mapOf("screen" to screenName))
    }

    fun logPresetApplied(presetId: String, presetName: String) {
        logEvent(
            AnalyticsEventType.PRESET_APPLIED,
            mapOf("preset_id" to presetId, "preset_name" to presetName)
        )
    }

    fun logError(errorType: String, errorMessage: String) {
        logEvent(
            AnalyticsEventType.ERROR_ENCOUNTERED,
            mapOf("error_type" to errorType, "error_message" to errorMessage)
        )
    }

    fun isAnalyticsActive(): Boolean = isInitialized && securePreferences.isAnalyticsEnabled()

    fun getPendingEventCount(): Int = pendingEvents.size

    companion object {
        private const val MAX_PENDING_EVENTS = 100
    }
}
