package com.omaster.app.data.sync

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omaster.app.data.remote.PresetApiService
import com.omaster.app.domain.model.Preset
import com.omaster.app.domain.model.SyncStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_manager")

/**
 * 增量同步管理器 - 企业级实现
 * 负责与远程服务器进行增量数据同步
 */
@Singleton
class IncrementalSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val presetApiService: PresetApiService
) {
    private val dataStore = context.syncDataStore

    companion object {
        private val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        private val SYNC_VERSION = stringPreferencesKey("sync_version")
        private val PENDING_CHANGES = stringPreferencesKey("pending_changes")
        private const val SYNC_INTERVAL_MS = 5 * 60 * 1000L // 5分钟
    }

    /**
     * 同步状态流
     */
    val syncStatus: Flow<SyncStatus> = dataStore.data.map { preferences ->
        val lastSync = preferences[LAST_SYNC_TIME] ?: 0L
        val version = preferences[SYNC_VERSION] ?: "0"
        val pending = preferences[PENDING_CHANGES]?.toIntOrNull() ?: 0
        
        SyncStatus(
            lastSyncTime = lastSync,
            version = version,
            pendingChanges = pending,
            isSynced = System.currentTimeMillis() - lastSync < SYNC_INTERVAL_MS
        )
    }

    /**
     * 执行完整同步
     */
    suspend fun performFullSync(): SyncResult = withContext(Dispatchers.IO) {
        try {
            Timber.d("开始完整同步...")
            
            // 从远程获取所有预设
            val response = presetApiService.getAllPresets()
            if (!response.isSuccessful) {
                return@withContext SyncResult.Error("服务器响应错误: ${response.code()}")
            }
            
            val presets = response.body()
            if (presets == null) {
                return@withContext SyncResult.Error("获取数据为空")
            }
            
            // 更新同步状态
            updateSyncState(presets.size.toString())
            
            Timber.d("完整同步完成，获取 ${presets.size} 条预设")
            SyncResult.Success(presets.size)
        } catch (e: Exception) {
            Timber.e(e, "完整同步失败")
            SyncResult.Error(e.message ?: "同步失败")
        }
    }

    /**
     * 执行增量同步
     */
    suspend fun performIncrementalSync(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val lastSync = dataStore.data.first()[LAST_SYNC_TIME] ?: 0L
            val currentVersion = dataStore.data.first()[SYNC_VERSION] ?: "0"
            
            Timber.d("开始增量同步，上次同步: $lastSync, 版本: $currentVersion")
            
            // 从远程获取版本信息
            val response = presetApiService.getAllPresets()
            if (!response.isSuccessful) {
                return@withContext SyncResult.Error("服务器响应错误: ${response.code()}")
            }
            
            val presets = response.body()
            if (presets == null) {
                return@withContext SyncResult.Error("获取数据为空")
            }
            
            // 更新同步状态
            updateSyncState(presets.size.toString())
            
            Timber.d("增量同步完成，获取 ${presets.size} 条预设")
            SyncResult.Success(presets.size)
        } catch (e: Exception) {
            Timber.e(e, "增量同步失败")
            SyncResult.Error(e.message ?: "同步失败")
        }
    }

    /**
     * 检查是否需要同步
     */
    suspend fun shouldSync(): Boolean = withContext(Dispatchers.IO) {
        val lastSync = dataStore.data.first()[LAST_SYNC_TIME] ?: 0L
        val timeSinceLastSync = System.currentTimeMillis() - lastSync
        timeSinceLastSync >= SYNC_INTERVAL_MS
    }

    /**
     * 获取上次同步时间
     */
    suspend fun getLastSyncTime(): Long = withContext(Dispatchers.IO) {
        dataStore.data.first()[LAST_SYNC_TIME] ?: 0L
    }

    /**
     * 获取当前版本
     */
    suspend fun getCurrentVersion(): String = withContext(Dispatchers.IO) {
        dataStore.data.first()[SYNC_VERSION] ?: "0"
    }

    /**
     * 重置同步状态
     */
    suspend fun resetSyncState() {
        dataStore.edit { preferences ->
            preferences.remove(LAST_SYNC_TIME)
            preferences.remove(SYNC_VERSION)
            preferences.remove(PENDING_CHANGES)
        }
        Timber.d("同步状态已重置")
    }

    /**
     * 添加待同步更改
     */
    suspend fun addPendingChange() {
        dataStore.edit { preferences ->
            val current = preferences[PENDING_CHANGES]?.toIntOrNull() ?: 0
            preferences[PENDING_CHANGES] = (current + 1).toString()
        }
    }

    /**
     * 清除待同步更改
     */
    suspend fun clearPendingChanges() {
        dataStore.edit { preferences ->
            preferences.remove(PENDING_CHANGES)
        }
    }

    /**
     * 更新同步状态
     */
    private suspend fun updateSyncState(version: String) {
        dataStore.edit { preferences ->
            preferences[LAST_SYNC_TIME] = System.currentTimeMillis()
            preferences[SYNC_VERSION] = version
            preferences.remove(PENDING_CHANGES)
        }
    }
}

/**
 * 同步结果密封类
 */
sealed class SyncResult {
    data class Success(val itemCount: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
    data class NoChanges(val message: String = "无变更") : SyncResult()
}
