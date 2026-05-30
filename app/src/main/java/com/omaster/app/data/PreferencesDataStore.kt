package com.omaster.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "omaster_preferences")

@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private object PreferencesKeys {
        val FAVORITE_PRESETS = stringSetPreferencesKey("favorite_presets")
        val THEME_MODE = intPreferencesKey("theme_mode")
        val FLUID_CLOUD_ENABLED = intPreferencesKey("fluid_cloud_enabled")
        val OVERLAY_ENABLED = intPreferencesKey("overlay_enabled")
    }

    // 同步状态：使用 StateFlow 缓存当前收藏
    private val _favoritePresetsState = MutableStateFlow<Set<String>>(emptySet())
    val favoritePresetsState: StateFlow<Set<String>> = _favoritePresetsState.asStateFlow()

    init {
        // 初始化时从 DataStore 加载收藏
        scope.launch {
            context.dataStore.data.collect { prefs ->
                val favorites = prefs[PreferencesKeys.FAVORITE_PRESETS] ?: emptySet()
                _favoritePresetsState.value = favorites
            }
        }
    }

    private fun Preferences.getBoolean(key: Preferences.Key<Int>, defaultValue: Boolean): Boolean {
        val value = this[key]
        return if (value == null) defaultValue else value == 1
    }

    val favoritePresets: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.FAVORITE_PRESETS] ?: emptySet()
        }

    /**
     * 同步获取当前收藏状态（从缓存的 StateFlow 读取）
     */
    fun getFavoritePresetsIds(): Set<String> {
        return _favoritePresetsState.value
    }

    val themeMode: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.value
        }

    val fluidCloudEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences.getBoolean(PreferencesKeys.FLUID_CLOUD_ENABLED, defaultValue = true)
        }

    val overlayEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences.getBoolean(PreferencesKeys.OVERLAY_ENABLED, defaultValue = false)
        }

    suspend fun toggleFavorite(presetId: String) {
        Timber.d("Toggling favorite for preset: $presetId")
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[PreferencesKeys.FAVORITE_PRESETS]?.toMutableSet() ?: mutableSetOf()
            if (currentFavorites.contains(presetId)) {
                currentFavorites.remove(presetId)
                Timber.d("Removed from favorites")
            } else {
                currentFavorites.add(presetId)
                Timber.d("Added to favorites")
            }
            preferences[PreferencesKeys.FAVORITE_PRESETS] = currentFavorites
        }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.value
        }
    }

    suspend fun setFluidCloudEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FLUID_CLOUD_ENABLED] = if (enabled) 1 else 0
        }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OVERLAY_ENABLED] = if (enabled) 1 else 0
        }
    }
}

enum class ThemeMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2)
}
