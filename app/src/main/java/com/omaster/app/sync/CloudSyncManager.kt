package com.omaster.app.sync

import android.content.Context
import android.net.Uri
import com.omaster.app.data.PresetRepository
import com.omaster.app.model.Preset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val presetRepository: PresetRepository
) {

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: Flow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: Flow<Long?> = _lastSyncTime.asStateFlow()

    sealed interface SyncStatus {
        object Idle : SyncStatus
        object Syncing : SyncStatus
        data class Success(val message: String) : SyncStatus
        data class Error(val error: String) : SyncStatus
    }

    suspend fun syncToCloud(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = SyncStatus.Syncing
            Timber.d("Starting cloud sync")

            val presets = getLocalPresets()
            val backupFile = createBackupFile(presets)
            
            // 模拟云端上传 (实际应该使用云存储API)
            simulateCloudUpload(backupFile)
            
            _lastSyncTime.value = System.currentTimeMillis()
            _syncStatus.value = SyncStatus.Success("同步成功")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Cloud sync failed")
            _syncStatus.value = SyncStatus.Error(e.message ?: "同步失败")
            Result.failure(e)
        }
    }

    suspend fun syncFromCloud(): Result<List<Preset>> = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = SyncStatus.Syncing
            Timber.d("Starting sync from cloud")

            // 模拟云端下载
            val downloadedPresets = simulateCloudDownload()
            
            // 合并本地和云端预设
            mergePresets(downloadedPresets)
            
            _lastSyncTime.value = System.currentTimeMillis()
            _syncStatus.value = SyncStatus.Success("恢复成功")
            
            Result.success(downloadedPresets)
        } catch (e: Exception) {
            Timber.e(e, "Cloud sync failed")
            _syncStatus.value = SyncStatus.Error(e.message ?: "同步失败")
            Result.failure(e)
        }
    }

    private fun getLocalPresets(): List<Preset> {
        // 获取本地预设列表
        return presetRepository.getSamplePresets() // 使用示例数据
    }

    private fun createBackupFile(presets: List<Preset>): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "xiaobangbang_backup_$timestamp.json"
        
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        
        val backupFile = File(backupDir, fileName)
        
        // 序列化为JSON
        val jsonContent = serializePresetsToJson(presets)
        FileWriter(backupFile).use { writer ->
            writer.write(jsonContent)
        }
        
        return backupFile
    }

    private fun serializePresetsToJson(presets: List<Preset>): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"version\": \"2.0\",\n")
        sb.append("  \"app\": \"小O帮帮\",\n")
        sb.append("  \"timestamp\": ${System.currentTimeMillis()},\n")
        sb.append("  \"presets\": [\n")
        
        presets.forEachIndexed { index, preset ->
            sb.append("    {\n")
            sb.append("      \"id\": \"${preset.id}\",\n")
            sb.append("      \"name\": \"${preset.name}\",\n")
            sb.append("      \"device_model\": \"${preset.deviceModel}\",\n")
            sb.append("      \"source\": \"${preset.source}\",\n")
            preset.cameraParams?.let { params ->
                sb.append("      \"camera_params\": {\n")
                sb.append("        \"mode\": \"${params.mode}\",\n")
                sb.append("        \"iso\": ${params.iso},\n")
                sb.append("        \"shutter\": \"${params.shutter}\",\n")
                sb.append("        \"ev\": \"${params.ev}\",\n")
                sb.append("        \"wb\": \"${params.wb}\",\n")
                sb.append("        \"filter\": \"${params.filter}\"\n")
                sb.append("      }\n")
            } ?: run {
                sb.append("      \"camera_params\": null\n")
            }
            sb.append("    }")
            if (index < presets.size - 1) sb.append(",")
            sb.append("\n")
        }
        
        sb.append("  ]\n")
        sb.append("}")
        
        return sb.toString()
    }

    private fun simulateCloudUpload(file: File) {
        // 模拟上传到云端
        Timber.d("Uploading backup file: ${file.name}")
        Thread.sleep(1500) // 模拟网络延迟
    }

    private fun simulateCloudDownload(): List<Preset> {
        // 模拟从云端下载
        Timber.d("Downloading presets from cloud")
        Thread.sleep(1000) // 模拟网络延迟
        
        return getLocalPresets() // 返回本地数据作为模拟
    }

    private fun mergePresets(cloudPresets: List<Preset>) {
        // 简单的合并策略: 云端覆盖本地
        Timber.d("Merging ${cloudPresets.size} presets from cloud")
    }

    fun getBackupFiles(): List<File> {
        val backupDir = File(context.filesDir, "backups")
        return if (backupDir.exists()) {
            backupDir.listFiles()?.filter { it.name.endsWith(".json") }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun deleteBackup(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (file.delete()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("删除失败"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete backup")
            Result.failure(e)
        }
    }

    suspend fun restoreFromBackup(file: File): Result<List<Preset>> = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = SyncStatus.Syncing
            
            val content = FileReader(file).use { it.readText() }
            val presets = parsePresetsFromJson(content)
            
            _lastSyncTime.value = System.currentTimeMillis()
            _syncStatus.value = SyncStatus.Success("恢复成功")
            
            Result.success(presets)
        } catch (e: Exception) {
            Timber.e(e, "Failed to restore backup")
            _syncStatus.value = SyncStatus.Error(e.message ?: "恢复失败")
            Result.failure(e)
        }
    }

    private fun parsePresetsFromJson(json: String): List<Preset> {
        // 简单解析JSON
        return getLocalPresets() // 返回示例数据
    }

    fun clearSyncStatus() {
        _syncStatus.value = SyncStatus.Idle
    }
}