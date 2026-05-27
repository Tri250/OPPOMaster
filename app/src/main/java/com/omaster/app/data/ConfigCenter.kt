package com.omaster.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "omaster_config")

@Singleton
class ConfigCenter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentVersion = AtomicInteger(1)
    private val _isInitialized = AtomicBoolean(false)
    private val _versionLock = Any()

    private object PreferencesKeys {
        val THEME_MODE = intPreferencesKey("theme_mode")
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        val FLUID_CLOUD_ENABLED = booleanPreferencesKey("fluid_cloud_enabled")
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        val AUTO_CHECK_UPDATE = booleanPreferencesKey("auto_check_update")
        val UPDATE_CHANNEL = stringPreferencesKey("update_channel")
        val DEFAULT_START_TAB = intPreferencesKey("default_start_tab")
        val LAST_UPDATE_CHECK = longPreferencesKey("last_update_check")
        val FAVORITE_PRESETS = stringSetPreferencesKey("favorite_presets")
        val FLOATING_WINDOW_OPACITY = floatPreferencesKey("floating_window_opacity")
        val FLOATING_WINDOW_MODE = stringPreferencesKey("floating_window_mode")
        val USER_AGREED_PRIVACY = booleanPreferencesKey("user_agreed_privacy")
        val MIGRATION_COMPLETED = booleanPreferencesKey("migration_completed")
    }

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _darkModeEnabled = MutableStateFlow(false)
    val darkModeEnabled: StateFlow<Boolean> = _darkModeEnabled.asStateFlow()

    private val _fluidCloudEnabled = MutableStateFlow(false)
    val fluidCloudEnabled: StateFlow<Boolean> = _fluidCloudEnabled.asStateFlow()

    private val _overlayEnabled = MutableStateFlow(false)
    val overlayEnabled: StateFlow<Boolean> = _overlayEnabled.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(true)
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    private val _analyticsEnabled = MutableStateFlow(true)
    val analyticsEnabled: StateFlow<Boolean> = _analyticsEnabled.asStateFlow()

    private val _autoCheckUpdate = MutableStateFlow(true)
    val autoCheckUpdate: StateFlow<Boolean> = _autoCheckUpdate.asStateFlow()

    private val _updateChannel = MutableStateFlow("gitee")
    val updateChannel: StateFlow<String> = _updateChannel.asStateFlow()

    private val _defaultStartTab = MutableStateFlow(0)
    val defaultStartTab: StateFlow<Int> = _defaultStartTab.asStateFlow()

    private val _lastUpdateCheck = MutableStateFlow(0L)
    val lastUpdateCheck: StateFlow<Long> = _lastUpdateCheck.asStateFlow()

    private val _favoritePresets = MutableStateFlow<Set<String>>(emptySet())
    val favoritePresets: StateFlow<Set<String>> = _favoritePresets.asStateFlow()

    private val _floatingWindowOpacity = MutableStateFlow(0.7f)
    val floatingWindowOpacity: StateFlow<Float> = _floatingWindowOpacity.asStateFlow()

    private val _floatingWindowMode = MutableStateFlow("compact")
    val floatingWindowMode: StateFlow<String> = _floatingWindowMode.asStateFlow()

    private val _userAgreedPrivacy = MutableStateFlow(false)
    val userAgreedPrivacy: StateFlow<Boolean> = _userAgreedPrivacy.asStateFlow()

    private val _migrationCompleted = MutableStateFlow(false)
    val migrationCompleted: StateFlow<Boolean> = _migrationCompleted.asStateFlow()

    init {
        scope.launch {
            loadPreferences()
            _isInitialized.set(true)
        }
    }

    private suspend fun loadPreferences() {
        try {
            context.dataStore.data.collectLatest { preferences ->
                _themeMode.value = ThemeMode.fromValue(
                    preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.value
                )
                _darkModeEnabled.value = preferences[PreferencesKeys.DARK_MODE_ENABLED] ?: false
                _fluidCloudEnabled.value = preferences[PreferencesKeys.FLUID_CLOUD_ENABLED] ?: false
                _overlayEnabled.value = preferences[PreferencesKeys.OVERLAY_ENABLED] ?: false
                _hapticEnabled.value = preferences[PreferencesKeys.HAPTIC_ENABLED] ?: true
                _analyticsEnabled.value = preferences[PreferencesKeys.ANALYTICS_ENABLED] ?: true
                _autoCheckUpdate.value = preferences[PreferencesKeys.AUTO_CHECK_UPDATE] ?: true
                _updateChannel.value = preferences[PreferencesKeys.UPDATE_CHANNEL] ?: "gitee"
                _defaultStartTab.value = preferences[PreferencesKeys.DEFAULT_START_TAB] ?: 0
                _lastUpdateCheck.value = preferences[PreferencesKeys.LAST_UPDATE_CHECK] ?: 0L
                _favoritePresets.value = preferences[PreferencesKeys.FAVORITE_PRESETS] ?: emptySet()
                _floatingWindowOpacity.value = preferences[PreferencesKeys.FLOATING_WINDOW_OPACITY] ?: 0.7f
                _floatingWindowMode.value = preferences[PreferencesKeys.FLOATING_WINDOW_MODE] ?: "compact"
                _userAgreedPrivacy.value = preferences[PreferencesKeys.USER_AGREED_PRIVACY] ?: false
                _migrationCompleted.value = preferences[PreferencesKeys.MIGRATION_COMPLETED] ?: false
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load preferences")
        }
    }

    private suspend fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        try {
            context.dataStore.edit { preferences ->
                preferences[key] = value
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set preference $key")
        }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        _themeMode.value = themeMode
        setPreference(PreferencesKeys.THEME_MODE, themeMode.value)
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        _darkModeEnabled.value = enabled
        setPreference(PreferencesKeys.DARK_MODE_ENABLED, enabled)
    }

    suspend fun setFluidCloudEnabled(enabled: Boolean) {
        _fluidCloudEnabled.value = enabled
        setPreference(PreferencesKeys.FLUID_CLOUD_ENABLED, enabled)
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        _overlayEnabled.value = enabled
        setPreference(PreferencesKeys.OVERLAY_ENABLED, enabled)
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        _hapticEnabled.value = enabled
        setPreference(PreferencesKeys.HAPTIC_ENABLED, enabled)
    }

    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        _analyticsEnabled.value = enabled
        setPreference(PreferencesKeys.ANALYTICS_ENABLED, enabled)
    }

    suspend fun setAutoCheckUpdate(enabled: Boolean) {
        _autoCheckUpdate.value = enabled
        setPreference(PreferencesKeys.AUTO_CHECK_UPDATE, enabled)
    }

    suspend fun setUpdateChannel(channel: String) {
        _updateChannel.value = channel
        setPreference(PreferencesKeys.UPDATE_CHANNEL, channel)
    }

    suspend fun setDefaultStartTab(tab: Int) {
        val safeTab = tab.coerceIn(0, 2)
        _defaultStartTab.value = safeTab
        setPreference(PreferencesKeys.DEFAULT_START_TAB, safeTab)
    }

    suspend fun setLastUpdateCheck(time: Long) {
        _lastUpdateCheck.value = time
        setPreference(PreferencesKeys.LAST_UPDATE_CHECK, time)
    }

    suspend fun toggleFavorite(presetId: String) {
        val current = _favoritePresets.value.toMutableSet()
        if (current.contains(presetId)) {
            current.remove(presetId)
        } else {
            current.add(presetId)
        }
        _favoritePresets.value = current
        setPreference(PreferencesKeys.FAVORITE_PRESETS, current)
    }

    suspend fun setFavorite(presetId: String, isFavorite: Boolean) {
        val current = _favoritePresets.value.toMutableSet()
        if (isFavorite) {
            current.add(presetId)
        } else {
            current.remove(presetId)
        }
        _favoritePresets.value = current
        setPreference(PreferencesKeys.FAVORITE_PRESETS, current)
    }

    suspend fun setFloatingWindowOpacity(opacity: Float) {
        val safeOpacity = opacity.coerceIn(0.3f, 0.7f)
        _floatingWindowOpacity.value = safeOpacity
        setPreference(PreferencesKeys.FLOATING_WINDOW_OPACITY, safeOpacity)
    }

    suspend fun setFloatingWindowMode(mode: String) {
        _floatingWindowMode.value = mode
        setPreference(PreferencesKeys.FLOATING_WINDOW_MODE, mode)
    }

    suspend fun setUserAgreedPrivacy(agreed: Boolean) {
        _userAgreedPrivacy.value = agreed
        setPreference(PreferencesKeys.USER_AGREED_PRIVACY, agreed)
    }

    suspend fun setMigrationCompleted(completed: Boolean) {
        _migrationCompleted.value = completed
        setPreference(PreferencesKeys.MIGRATION_COMPLETED, completed)
    }

    fun isInitialized(): Boolean = _isInitialized.get()

    fun getCurrentVersion(): Int = _currentVersion.get()

    fun checkAndSetVersion(newVersion: Int): Boolean {
        synchronized(_versionLock) {
            if (newVersion > _currentVersion.get()) {
                _currentVersion.set(newVersion)
                Timber.d("Version updated to: $newVersion")
                return true
            }
            return false
        }
    }
}

enum class ThemeMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2);

    companion object {
        fun fromValue(value: Int): ThemeMode = values().find { it.value == value } ?: SYSTEM
    }
}
