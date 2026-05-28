package com.omaster.app.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omaster.app.data.PresetRepository
import com.omaster.app.model.Preset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_preferences")

@Singleton
class CloudSyncService @Inject constructor(
    private val context: Context,
    private val presetRepository: PresetRepository
) {

    private object SyncKeys {
        val LAST_SYNC_TIME = stringPreferencesKey("last_sync_time")
        val SYNC_VERSION = stringPreferencesKey("sync_version")
        val AUTO_SYNC_ENABLED = stringPreferencesKey("auto_sync_enabled")
    }

    val autoSyncEnabled = context.syncDataStore.data
        .map { it[SyncKeys.AUTO_SYNC_ENABLED]?.toBoolean() ?: true }

    suspend fun syncPresets(): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("开始同步预设")
                val networkPresets = presetRepository.fetchPresetsFromNetwork()
                
                if (networkPresets != null && networkPresets.isNotEmpty()) {
                    Timber.d("获取到 ${networkPresets.size} 个云端预设")
                    
                    val currentVersion = getCurrentVersion()
                    val newVersion = generateVersion()
                    
                    saveSyncTime()
                    saveVersion(newVersion)
                    
                    Timber.d("同步完成")
                    SyncResult.Success(
                        updatedCount = networkPresets.size,
                        lastSyncTime = getLastSyncTime(),
                        version = newVersion
                    )
                } else {
                    Timber.d("云端没有新预设")
                    SyncResult.NoChanges
                }
            } catch (e: Exception) {
                Timber.e(e, "同步失败")
                SyncResult.Error(e.message ?: "未知错误")
            }
        }
    }

    suspend fun getLastSyncTime(): String {
        return context.syncDataStore.data.first()[SyncKeys.LAST_SYNC_TIME] ?: "从未同步"
    }

    suspend fun getCurrentVersion(): String {
        return context.syncDataStore.data.first()[SyncKeys.SYNC_VERSION] ?: "0.0.0"
    }

    private suspend fun saveSyncTime() {
        context.syncDataStore.edit { preferences ->
            preferences[SyncKeys.LAST_SYNC_TIME] = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        }
    }

    private suspend fun saveVersion(version: String) {
        context.syncDataStore.edit { preferences ->
            preferences[SyncKeys.SYNC_VERSION] = version
        }
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        context.syncDataStore.edit { preferences ->
            preferences[SyncKeys.AUTO_SYNC_ENABLED] = enabled.toString()
        }
    }

    private fun generateVersion(): String {
        val now = LocalDateTime.now()
        return "${now.year}.${now.monthValue}.${now.dayOfMonth}.${now.hour}"
    }

    sealed class SyncResult {
        data class Success(
            val updatedCount: Int,
            val lastSyncTime: String,
            val version: String
        ) : SyncResult()
        
        object NoChanges : SyncResult()
        
        data class Error(val message: String) : SyncResult()
    }
}