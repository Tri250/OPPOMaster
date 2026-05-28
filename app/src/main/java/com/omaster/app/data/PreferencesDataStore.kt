package com.omaster.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        val FLUID_CLOUD_ENABLED = intPreferencesKey("fluid_cloud_enabled")
        val OVERLAY_ENABLED = intPreferencesKey("overlay_enabled")
        val ONBOARDING_COMPLETED = intPreferencesKey("onboarding_completed")
    }

    private fun Preferences.getBoolean(key: Preferences.Key<Int>, defaultValue: Boolean): Boolean {
        val value = this[key]
        return if (value == null) defaultValue else value == 1
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
            preferences.getBoolean(PreferencesKeys.FLUID_CLOUD_ENABLED, defaultValue = true)
        }

    val overlayEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences.getBoolean(PreferencesKeys.OVERLAY_ENABLED, defaultValue = false)
        }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences.getBoolean(PreferencesKeys.ONBOARDING_COMPLETED, defaultValue = false)
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = if (completed) 1 else 0
        }
    }

    suspend fun toggleFavorite(presetId: String) {
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[PreferencesKeys.FAVORITE_PRESETS]?.toMutableSet() ?: mutableSet()
            if (currentFavorites.contains(presetId)) {
                currentFavorites.remove(presetId)
            } else {
                currentFavorites.add(presetId)
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