package com.omaster.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val FLUID_CLOUD_ENABLED = booleanPreferencesKey("fluid_cloud_enabled")
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
        
        // 水印配置相关
        val WATERMARK_TEMPLATE = stringPreferencesKey("watermark_template")
        val WATERMARK_POSITION = stringPreferencesKey("watermark_position")
        val WATERMARK_OPACITY = floatPreferencesKey("watermark_opacity")
        val WATERMARK_SHOW_TIMESTAMP = booleanPreferencesKey("watermark_show_timestamp")
        val WATERMARK_SHOW_DEVICE = booleanPreferencesKey("watermark_show_device")
        val WATERMARK_CUSTOM_TEXT = stringPreferencesKey("watermark_custom_text")
        val WATERMARK_TEXT_SIZE = floatPreferencesKey("watermark_text_size")
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
    
    // 水印配置相关Flow
    val watermarkTemplate: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.WATERMARK_TEMPLATE] ?: "HASSELBLAD"
        }
    
    val watermarkPosition: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.WATERMARK_POSITION] ?: "BOTTOM_RIGHT"
        }
    
    val watermarkOpacity: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.WATERMARK_OPACITY] ?: 0.8f
        }
    
    val watermarkShowTimestamp: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.WATERMARK_SHOW_TIMESTAMP] ?: true
        }
    
    val watermarkShowDevice: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.WATERMARK_SHOW_DEVICE] ?: true
        }
    
    val watermarkCustomText: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.WATERMARK_CUSTOM_TEXT] ?: ""
        }
    
    val watermarkTextSize: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.WATERMARK_TEXT_SIZE] ?: 1.0f
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
    
    // 水印配置保存方法
    suspend fun saveWatermarkConfig(
        template: String,
        position: String,
        opacity: Float,
        showTimestamp: Boolean,
        showDevice: Boolean,
        customText: String,
        textSize: Float
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WATERMARK_TEMPLATE] = template
            preferences[PreferencesKeys.WATERMARK_POSITION] = position
            preferences[PreferencesKeys.WATERMARK_OPACITY] = opacity
            preferences[PreferencesKeys.WATERMARK_SHOW_TIMESTAMP] = showTimestamp
            preferences[PreferencesKeys.WATERMARK_SHOW_DEVICE] = showDevice
            preferences[PreferencesKeys.WATERMARK_CUSTOM_TEXT] = customText
            preferences[PreferencesKeys.WATERMARK_TEXT_SIZE] = textSize
        }
    }
}

enum class ThemeMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2)
}