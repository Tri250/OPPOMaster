package com.omaster.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "omaster_preferences")

@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val FAVORITE_PRESETS = stringSetPreferencesKey("favorite_presets")
        val THEME_MODE = intPreferencesKey("theme_mode")
        val FLUID_CLOUD_ENABLED = booleanPreferencesKey("fluid_cloud_enabled")
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
    }

    val favoritePresets: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.FAVORITE_PRESETS] ?: emptySet()
        }

    val themeMode: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.value
        }

    val fluidCloudEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.FLUID_CLOUD_ENABLED] ?: true
        }

    val overlayEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.OVERLAY_ENABLED] ?: false
        }

    val syncEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SYNC_ENABLED] ?: true
        }

    suspend fun toggleFavorite(presetId: String) {
        try {
            context.dataStore.edit { preferences ->
                val currentFavorites = preferences[PreferencesKeys.FAVORITE_PRESETS]?.toMutableSet() ?: mutableSet()
                if (currentFavorites.contains(presetId)) {
                    currentFavorites.remove(presetId)
                } else {
                    currentFavorites.add(presetId)
                }
                preferences[PreferencesKeys.FAVORITE_PRESETS] = currentFavorites
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle favorite for preset: $presetId")
        }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.THEME_MODE] = themeMode.value
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set theme mode: ${themeMode.value}")
        }
    }

    suspend fun setFluidCloudEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.FLUID_CLOUD_ENABLED] = enabled
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set fluid cloud enabled: $enabled")
        }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.OVERLAY_ENABLED] = enabled
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set overlay enabled: $enabled")
        }
    }

    suspend fun setSyncEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.SYNC_ENABLED] = enabled
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set sync enabled: $enabled")
        }
    }
}

enum class ThemeMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2)
}