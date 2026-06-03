package com.omaster.app.data

import android.content.Context
import androidx.annotation.Keep
import androidx.annotation.NonNull
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
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "omaster_preferences")

@Keep
@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext @NonNull private val context: Context
) {
    private object PreferencesKeys {
        val FAVORITE_PRESETS = stringSetPreferencesKey("favorite_presets")
        val THEME_MODE = intPreferencesKey("theme_mode")
        val FLUID_CLOUD_ENABLED = booleanPreferencesKey("fluid_cloud_enabled")
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
    }

    @NonNull val favoritePresets: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.FAVORITE_PRESETS] ?: emptySet()
        }

    @NonNull val themeMode: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.value
        }

    @NonNull val fluidCloudEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.FLUID_CLOUD_ENABLED] ?: true
        }

    @NonNull val overlayEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.OVERLAY_ENABLED] ?: false
        }

    @NonNull val syncEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SYNC_ENABLED] ?: true
        }

    suspend fun toggleFavorite(@NonNull presetId: String) {
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[PreferencesKeys.FAVORITE_PRESETS]?.toMutableSet() ?: mutableSetOf()
            if (currentFavorites.contains(presetId)) {
                currentFavorites.remove(presetId)
            } else {
                currentFavorites.add(presetId)
            }
            preferences[PreferencesKeys.FAVORITE_PRESETS] = currentFavorites
        }
    }

    suspend fun setThemeMode(@NonNull themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.value
        }
    }

    suspend fun setFluidCloudEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FLUID_CLOUD_ENABLED] = enabled
        }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OVERLAY_ENABLED] = enabled
        }
    }

    suspend fun setSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYNC_ENABLED] = enabled
        }
    }
}

@Keep
enum class ThemeMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2)
}