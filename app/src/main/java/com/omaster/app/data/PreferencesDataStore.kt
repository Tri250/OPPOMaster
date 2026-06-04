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

/**
 * 用户偏好设置数据存储
 *
 * 安全说明:
 * - 此DataStore存储非敏感用户偏好数据（收藏预设、主题模式、功能开关等）
 * - 不存储敏感数据（密码、Token、个人隐私信息等）
 * - 非敏感数据不需要加密存储，加密会降低性能且增加复杂度
 * - 敏感数据应使用SecurePreferencesManager进行加密存储
 *
 * 数据分类:
 * - FAVORITE_PRESETS: 用户收藏的预设ID列表（非敏感）
 * - THEME_MODE: 主题模式设置（非敏感）
 * - FLUID_CLOUD_ENABLED: 流体云功能开关（非敏感）
 * - OVERLAY_ENABLED: 悬浮窗功能开关（非敏感）
 * - SYNC_ENABLED: 同步功能开关（非敏感）
 *
 * 如需存储敏感数据，请使用SecurePreferencesManager
 */
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