package com.omaster.app.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omaster.app.domain.model.Preset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

private val Context.incrementalSyncDataStore by preferencesDataStore(name = "incremental_sync")

/**
 * 增量同步管理器
 * 负责对比本地和云端数据版本，只下载变更的数据，支持断点续传
 */
@Singleton
class IncrementalSyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object PreferencesKeys {
        // 本地数据版本号
        val LOCAL_DATA_VERSION = longPreferencesKey("local_data_version")
        // 云端数据版本号
        val CLOUD_DATA_VERSION = longPreferencesKey("cloud_data_version")
        // 最后同步时间戳
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        // 断点续传的文件路径
        val RESUME_FILE_PATH = stringPreferencesKey("resume_file_path")
        // 断点续传的进度位置
        val RESUME_POSITION = longPreferencesKey("resume_position")
        // 当前同步批次ID
        val CURRENT_SYNC_BATCH_ID = stringPreferencesKey("current_sync_batch_id")
    }

    // 同步状态
    private val _syncState = MutableStateFlow<IncrementalSyncState>(IncrementalSyncState.Idle)
    val syncState: Flow<IncrementalSyncState> = _syncState.asStateFlow()

    // 同步进度
    private val _syncProgress = MutableStateFlow<SyncProgress>(SyncProgress())
    val syncProgress: Flow<SyncProgress> = _syncProgress.asStateFlow()

    // 本地数据版本
    val localDataVersion: Flow<Long> = context.incrementalSyncDataStore.data
        .map { it[PreferencesKeys.LOCAL_DATA_VERSION] ?: 0L }

    // 云端数据版本
    val cloudDataVersion: Flow<Long> = context.incrementalSyncDataStore.data
        .map { it[PreferencesKeys.CLOUD_DATA_VERSION] ?: 0L }

    // 最后同步时间
    val lastSyncTimestamp: Flow<Long> = context.incrementalSyncDataStore.data
        .map { it[PreferencesKeys.LAST_SYNC_TIMESTAMP] ?: 0L }

    /**
     * 同步状态密封类
     */
    sealed class IncrementalSyncState {
        object Idle : IncrementalSyncState()
        object CheckingVersion : IncrementalSyncState()
        object CalculatingDiff : IncrementalSyncState()
        object Downloading : IncrementalSyncState()
        object ApplyingChanges : IncrementalSyncState()
        data class Success(val changesCount: Int) : IncrementalSyncState()
        data class Error(val message: String) : IncrementalSyncState()
        object NoChanges : IncrementalSyncState()
    }

    /**
     * 同步进度数据类
     */
    data class SyncProgress(
        val totalItems: Int = 0,
        val completedItems: Int = 0,
        val currentItem: String = "",
        val percentage: Int = 0,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = 0L
    )

    /**
     * 数据变更项
     */
    data class DataChange(
        val id: String,
        val type: ChangeType,
        val entityType: EntityType,
        val data: String,
        val version: Long,
        val checksum: String
    )

    enum class ChangeType {
        CREATE,     // 新增
        UPDATE,     // 更新
        DELETE      // 删除
    }

    enum class EntityType {
        PRESET,     // 预设
        TEMPLATE,   // 模板
        CONFIG,     // 配置
        METADATA    // 元数据
    }

    /**
     * 版本信息
     */
    data class VersionInfo(
        val version: Long,
        val timestamp: Long,
        val checksum: String,
        val changeCount: Int
    )

    /**
     * 执行增量同步
     * @param forceFullSync 是否强制全量同步
     * @return 同步结果
     */
    suspend fun performIncrementalSync(forceFullSync: Boolean = false): Result<SyncResult> =
        withContext(Dispatchers.IO) {
            try {
                _syncState.value = IncrementalSyncState.CheckingVersion
                _syncProgress.value = SyncProgress()

                // 1. 获取云端版本信息
                val cloudVersion = fetchCloudVersionInfo()
                val localVersion = localDataVersion.first()

                Timber.d("本地版本: $localVersion, 云端版本: ${cloudVersion.version}")

                // 2. 检查是否需要同步
                if (!forceFullSync && cloudVersion.version <= localVersion) {
                    _syncState.value = IncrementalSyncState.NoChanges
                    return@withContext Result.success(SyncResult(0, 0, 0, true))
                }

                // 3. 计算差异
                _syncState.value = IncrementalSyncState.CalculatingDiff
                val changes = calculateDataDiff(localVersion, cloudVersion.version)

                if (changes.isEmpty()) {
                    _syncState.value = IncrementalSyncState.NoChanges
                    updateLocalVersion(cloudVersion.version)
                    return@withContext Result.success(SyncResult(0, 0, 0, true))
                }

                // 4. 下载变更数据（支持断点续传）
                _syncState.value = IncrementalSyncState.Downloading
                _syncProgress.value = SyncProgress(
                    totalItems = changes.size,
                    totalBytes = estimateTotalBytes(changes)
                )

                val downloadResult = downloadChangesWithResume(changes)

                // 5. 应用变更
                _syncState.value = IncrementalSyncState.ApplyingChanges
                val appliedCount = applyChanges(downloadResult)

                // 6. 更新版本号
                updateLocalVersion(cloudVersion.version)
                updateLastSyncTimestamp(System.currentTimeMillis())
                clearResumeInfo()

                _syncState.value = IncrementalSyncState.Success(appliedCount)

                Result.success(SyncResult(
                    downloadedChanges = changes.size,
                    appliedChanges = appliedCount,
                    failedChanges = changes.size - appliedCount,
                    success = true
                ))
            } catch (e: Exception) {
                Timber.e(e, "增量同步失败")
                _syncState.value = IncrementalSyncState.Error(e.message ?: "同步失败")
                Result.failure(e)
            }
        }

    /**
     * 获取云端版本信息
     */
    private suspend fun fetchCloudVersionInfo(): VersionInfo {
        // 模拟从云端获取版本信息
        // 实际实现中应该调用云端API
        return VersionInfo(
            version = System.currentTimeMillis(),
            timestamp = System.currentTimeMillis(),
            checksum = "",
            changeCount = 0
        )
    }

    /**
     * 计算数据差异
     */
    private suspend fun calculateDataDiff(
        localVersion: Long,
        cloudVersion: Long
    ): List<DataChange> {
        // 模拟计算差异
        // 实际实现中应该对比本地和云端的数据哈希
        return emptyList()
    }

    /**
     * 支持断点续传的下载
     */
    private suspend fun downloadChangesWithResume(
        changes: List<DataChange>
    ): List<DownloadedChange> = withContext(Dispatchers.IO) {
        val downloadedChanges = mutableListOf<DownloadedChange>()
        val resumeInfo = getResumeInfo()

        var startIndex = 0
        var resumePosition = 0L

        // 检查是否有断点续传信息
        if (resumeInfo.filePath != null && resumeInfo.position > 0) {
            startIndex = changes.indexOfFirst { it.id == resumeInfo.batchId }
            resumePosition = resumeInfo.position
        }

        for (i in startIndex until changes.size) {
            val change = changes[i]
            try {
                updateSyncProgress(i, changes.size, change.id)

                val downloadedData = downloadChangeWithResume(change, resumePosition)
                downloadedChanges.add(DownloadedChange(change, downloadedData))

                // 清除断点位置，当前项已完成
                resumePosition = 0L
                saveResumeInfo(change.id, 0L)
            } catch (e: Exception) {
                // 保存断点信息以便续传
                saveResumeInfo(change.id, resumePosition)
                Timber.e(e, "下载变更失败: ${change.id}")
                throw e
            }
        }

        downloadedChanges
    }

    /**
     * 下载单个变更（支持断点续传）
     */
    private suspend fun downloadChangeWithResume(
        change: DataChange,
        resumePosition: Long
    ): ByteArray = withContext(Dispatchers.IO) {
        // 模拟断点续传下载
        // 实际实现中应该使用HTTP Range请求
        val tempFile = File(context.cacheDir, "sync_${change.id}.tmp")

        if (resumePosition > 0 && tempFile.exists()) {
            // 断点续传
            val raf = RandomAccessFile(tempFile, "rw")
            raf.seek(resumePosition)
            // 继续下载...
            raf.close()
        } else {
            // 新下载
            // 下载数据到tempFile
        }

        tempFile.readBytes()
    }

    /**
     * 应用变更到本地数据库
     */
    private suspend fun applyChanges(changes: List<DownloadedChange>): Int =
        withContext(Dispatchers.IO) {
            var appliedCount = 0

            changes.forEach { downloadedChange ->
                try {
                    when (downloadedChange.change.type) {
                        ChangeType.CREATE -> applyCreateChange(downloadedChange)
                        ChangeType.UPDATE -> applyUpdateChange(downloadedChange)
                        ChangeType.DELETE -> applyDeleteChange(downloadedChange)
                    }
                    appliedCount++
                } catch (e: Exception) {
                    Timber.e(e, "应用变更失败: ${downloadedChange.change.id}")
                }
            }

            appliedCount
        }

    /**
     * 应用新增变更
     */
    private suspend fun applyCreateChange(downloadedChange: DownloadedChange) {
        // 将新数据插入本地数据库
        Timber.d("创建新数据: ${downloadedChange.change.id}")
    }

    /**
     * 应用更新变更
     */
    private suspend fun applyUpdateChange(downloadedChange: DownloadedChange) {
        // 更新本地数据库中的数据
        Timber.d("更新数据: ${downloadedChange.change.id}")
    }

    /**
     * 应用删除变更
     */
    private suspend fun applyDeleteChange(downloadedChange: DownloadedChange) {
        // 从本地数据库删除数据
        Timber.d("删除数据: ${downloadedChange.change.id}")
    }

    /**
     * 更新本地版本号
     */
    private suspend fun updateLocalVersion(version: Long) {
        context.incrementalSyncDataStore.edit { preferences ->
            preferences[PreferencesKeys.LOCAL_DATA_VERSION] = version
        }
    }

    /**
     * 更新云端版本号
     */
    suspend fun updateCloudVersion(version: Long) {
        context.incrementalSyncDataStore.edit { preferences ->
            preferences[PreferencesKeys.CLOUD_DATA_VERSION] = version
        }
    }

    /**
     * 更新最后同步时间戳
     */
    private suspend fun updateLastSyncTimestamp(timestamp: Long) {
        context.incrementalSyncDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    /**
     * 保存断点续传信息
     */
    private suspend fun saveResumeInfo(batchId: String, position: Long) {
        context.incrementalSyncDataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_SYNC_BATCH_ID] = batchId
            preferences[PreferencesKeys.RESUME_POSITION] = position
        }
    }

    /**
     * 获取断点续传信息
     */
    private suspend fun getResumeInfo(): ResumeInfo {
        val prefs = context.incrementalSyncDataStore.data.first()
        return ResumeInfo(
            filePath = prefs[PreferencesKeys.RESUME_FILE_PATH],
            position = prefs[PreferencesKeys.RESUME_POSITION] ?: 0L,
            batchId = prefs[PreferencesKeys.CURRENT_SYNC_BATCH_ID] ?: ""
        )
    }

    /**
     * 清除断点续传信息
     */
    private suspend fun clearResumeInfo() {
        context.incrementalSyncDataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.RESUME_FILE_PATH)
            preferences.remove(PreferencesKeys.RESUME_POSITION)
            preferences.remove(PreferencesKeys.CURRENT_SYNC_BATCH_ID)
        }
    }

    /**
     * 更新同步进度
     */
    private fun updateSyncProgress(completed: Int, total: Int, currentItem: String) {
        val percentage = if (total > 0) (completed * 100 / total) else 0
        _syncProgress.value = _syncProgress.value.copy(
            completedItems = completed,
            totalItems = total,
            currentItem = currentItem,
            percentage = percentage
        )
    }

    /**
     * 估算总字节数
     */
    private fun estimateTotalBytes(changes: List<DataChange>): Long {
        // 估算下载数据的总大小
        return changes.size * 1024L // 假设每个变更平均1KB
    }

    /**
     * 检查是否需要同步
     */
    suspend fun needsSync(): Boolean {
        val local = localDataVersion.first()
        val cloud = fetchCloudVersionInfo()
        return cloud.version > local
    }

    /**
     * 获取版本差异统计
     */
    suspend fun getVersionDiffStats(): VersionDiffStats {
        val local = localDataVersion.first()
        val cloud = cloudDataVersion.first()
        val lastSync = lastSyncTimestamp.first()

        return VersionDiffStats(
            localVersion = local,
            cloudVersion = cloud,
            versionDiff = cloud - local,
            lastSyncTime = lastSync,
            timeSinceLastSync = System.currentTimeMillis() - lastSync
        )
    }

    /**
     * 重置同步状态
     */
    suspend fun resetSyncState() {
        _syncState.value = IncrementalSyncState.Idle
        _syncProgress.value = SyncProgress()
        clearResumeInfo()
    }

    // 数据类定义
    data class DownloadedChange(
        val change: DataChange,
        val data: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as DownloadedChange
            return change.id == other.change.id
        }

        override fun hashCode(): Int = change.id.hashCode()
    }

    data class ResumeInfo(
        val filePath: String?,
        val position: Long,
        val batchId: String
    )

    data class SyncResult(
        val downloadedChanges: Int,
        val appliedChanges: Int,
        val failedChanges: Int,
        val success: Boolean
    )

    data class VersionDiffStats(
        val localVersion: Long,
        val cloudVersion: Long,
        val versionDiff: Long,
        val lastSyncTime: Long,
        val timeSinceLastSync: Long
    )
}
