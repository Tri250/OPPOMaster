package com.omaster.app.privacy

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.omaster.app.data.Preset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户数据管理器
 * DATA-PRV-003: 用户数据控制权 - 导出、删除、备份
 */
@Singleton
class UserDataManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataCollectionTracker: DataCollectionTracker,
    private val privacyPolicyManager: PrivacyPolicyManager
) {
    private val gson = Gson()
    
    /**
     * 导出所有用户数据
     * DATA-PRV-003: 用户可以导出所有自定义预设数据
     */
    suspend fun exportUserData(presets: List<Preset>): Result<File> = withContext(Dispatchers.IO) {
        try {
            Timber.d("开始导出用户数据")
            
            // 记录导出事件
            dataCollectionTracker.trackDataExport()
            
            // 创建导出数据
            val exportData = UserDataExport(
                version = "1.0",
                exportTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                deviceId = dataCollectionTracker.getAnonymousDeviceId(),
                presets = presets,
                settings = getUserSettings()
            )
            
            // 序列化为JSON
            val json = gson.toJson(exportData)
            
            // 创建导出文件
            val fileName = "omaster_export_${System.currentTimeMillis()}.json"
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            val exportFile = File(exportDir, fileName)
            exportFile.writeText(json)
            
            Timber.d("用户数据导出成功: ${exportFile.absolutePath}")
            Result.success(exportFile)
        } catch (e: Exception) {
            Timber.e(e, "导出用户数据失败")
            Result.failure(e)
        }
    }
    
    /**
     * 分享导出的数据文件
     * DATA-PRV-003: 用户可以分享导出的数据
     */
    suspend fun shareExportedData(file: File): Intent? = withContext(Dispatchers.IO) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.opmaster.provider",
                file
            )
            
            Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "小O帮帮数据导出")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            Timber.e(e, "分享导出数据失败")
            null
        }
    }
    
    /**
     * 删除所有用户数据
     * DATA-PRV-003: 用户可以删除账户及所有相关数据
     */
    suspend fun deleteAllUserData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Timber.d("开始删除所有用户数据")
            
            // 记录删除事件
            dataCollectionTracker.trackDataDeletion()
            
            // 清除DataStore中的数据
            privacyPolicyManager.resetPrivacyPolicy()
            
            // 清除缓存目录
            clearCacheDirectory()
            
            // 清除应用数据
            context.getSharedPreferences("presets", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
            
            context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
            
            Timber.d("用户数据删除成功")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "删除用户数据失败")
            Result.failure(e)
        }
    }
    
    /**
     * 清除导出文件
     */
    suspend fun clearExportFiles() = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.cacheDir, "exports")
            if (exportDir.exists()) {
                exportDir.listFiles()?.forEach { file ->
                    file.delete()
                }
            }
            Timber.d("导出文件已清除")
        } catch (e: Exception) {
            Timber.e(e, "清除导出文件失败")
        }
    }
    
    /**
     * 获取用户数据大小
     * DATA-PRV-003: 显示数据占用空间
     */
    suspend fun getUserDataSize(): Long = withContext(Dispatchers.IO) {
        var totalSize = 0L
        
        // 计算预设数据大小
        totalSize += getPresetsDataSize()
        
        // 计算设置数据大小
        totalSize += getSettingsDataSize()
        
        // 计算缓存大小
        totalSize += getCacheSize()
        
        totalSize
    }
    
    /**
     * 获取用户数据统计信息
     * DATA-PRV-003: 数据统计
     */
    suspend fun getUserDataStats(): UserDataStats = withContext(Dispatchers.IO) {
        UserDataStats(
            presetsCount = getPresetsCount(),
            presetsSize = getPresetsDataSize(),
            settingsSize = getSettingsDataSize(),
            cacheSize = getCacheSize(),
            totalSize = getUserDataSize()
        )
    }
    
    private fun clearCacheDirectory() {
        try {
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "清除缓存目录失败")
        }
    }
    
    private fun getPresetsCount(): Int {
        return try {
            val prefs = context.getSharedPreferences("presets", Context.MODE_PRIVATE)
            val presetsJson = prefs.getString("presets_list", "[]") ?: "[]"
            val type = object : TypeToken<List<Preset>>() {}.type
            val presets: List<Preset> = gson.fromJson(presetsJson, type)
            presets.size
        } catch (e: Exception) {
            0
        }
    }
    
    private fun getPresetsDataSize(): Long {
        return try {
            val prefs = context.getSharedPreferences("presets", Context.MODE_PRIVATE)
            prefs.getString("presets_list", "[]")?.toByteArray()?.size?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun getSettingsDataSize(): Long {
        return try {
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            prefs.all.entries.sumOf { (_, value) ->
                value?.toString()?.toByteArray()?.size ?: 0
            }.toLong()
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun getCacheSize(): Long {
        return try {
            context.cacheDir.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun getUserSettings(): Map<String, Any> {
        return try {
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            prefs.all.mapValues { (_, value) ->
                value ?: ""
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

/**
 * 用户数据导出结构
 */
data class UserDataExport(
    val version: String,
    val exportTime: String,
    val deviceId: String,
    val presets: List<Preset>,
    val settings: Map<String, Any>
)

/**
 * 用户数据统计
 */
data class UserDataStats(
    val presetsCount: Int,
    val presetsSize: Long,
    val settingsSize: Long,
    val cacheSize: Long,
    val totalSize: Long
) {
    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        }
    }
}
