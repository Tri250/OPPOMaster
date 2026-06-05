package com.omaster.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.omaster.app.model.CameraConfig
import com.omaster.app.model.CameraConfigExport
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 相机配置文件Repository
 * 负责配置文件的存储、读取、更新、删除操作
 */
@Singleton
class CameraConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : Closeable {
    private val gson = Gson()
    private val configsDir: File by lazy {
        File(context.filesDir, "camera_configs").apply {
            if (!exists()) mkdirs()
        }
    }
    private val configsFile: File by lazy {
        File(configsDir, "configs.json")
    }

    private val _configs = MutableStateFlow<List<CameraConfig>>(emptyList())
    val configs: StateFlow<List<CameraConfig>> = _configs.asStateFlow()

    // 使用 SupervisorJob 避免协程取消影响整个作用域
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // 异步初始化，避免阻塞主线程
        repositoryScope.launch {
            loadConfigsAsync()
        }
    }

    /**
     * 异步加载所有配置
     */
    private suspend fun loadConfigsAsync() {
        try {
            if (configsFile.exists()) {
                // 安全检查: 限制文件大小
                val maxFileSize = 1024 * 1024 // 1MB
                if (configsFile.length() > maxFileSize) {
                    Timber.w("Configs file too large, using sample configs")
                    _configs.value = CameraConfig.sampleConfigs()
                    return
                }
                
                val type = object : TypeToken<List<CameraConfig>>() {}.type
                FileReader(configsFile).use { reader ->
                    // 限制读取的字符数
                    val content = reader.readText()
                    if (content.length > maxFileSize) {
                        Timber.w("Configs content too large, using sample configs")
                        _configs.value = CameraConfig.sampleConfigs()
                        return
                    }
                    
                    val configs = gson.fromJson<List<CameraConfig>>(content, type)
                    // 验证解析结果
                    val validConfigs = configs?.filter { config ->
                        // 基本验证：ID和名称不能为空
                        config.id.isNotEmpty() && config.name.isNotEmpty()
                    } ?: CameraConfig.sampleConfigs()
                    
                    _configs.value = validConfigs
                }
            } else {
                val sampleConfigs = CameraConfig.sampleConfigs()
                // 先写文件成功后再更新内存
                if (saveConfigsToFile(sampleConfigs)) {
                    _configs.value = sampleConfigs
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading configs")
            _configs.value = CameraConfig.sampleConfigs()
        }
    }

    /**
     * 仅保存配置到文件（不更新内存）
     */
    private fun saveConfigsToFile(configs: List<CameraConfig>): Boolean {
        try {
            FileWriter(configsFile).use { writer ->
                gson.toJson(configs, writer)
            }
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error saving configs to file")
            return false
        }
    }

    /**
     * 保存配置列表 - 先写文件成功后再更新内存
     */
    private fun saveConfigs(configs: List<CameraConfig>): Boolean {
        // 先写文件
        val fileSaved = saveConfigsToFile(configs)
        if (fileSaved) {
            // 文件写入成功后再更新内存
            _configs.value = configs
            return true
        }
        return false
    }

    /**
     * 添加新配置 - 先写文件成功后再更新内存
     */
    suspend fun addConfig(config: CameraConfig): Result<CameraConfig> {
        return try {
            val currentConfigs = _configs.value.toMutableList()
            currentConfigs.add(0, config)
            // 先写文件成功后再更新内存
            if (saveConfigs(currentConfigs)) {
                Result.success(config)
            } else {
                Result.failure(Exception("Failed to save config to file"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error adding config")
            Result.failure(e)
        }
    }

    /**
     * 更新配置 - 先写文件成功后再更新内存
     */
    suspend fun updateConfig(config: CameraConfig): Result<CameraConfig> {
        return try {
            val currentConfigs = _configs.value.toMutableList()
            val index = currentConfigs.indexOfFirst { it.id == config.id }
            if (index != -1) {
                val updatedConfig = config.copy(updatedAt = System.currentTimeMillis())
                currentConfigs[index] = updatedConfig
                // 先写文件成功后再更新内存
                if (saveConfigs(currentConfigs)) {
                    Result.success(updatedConfig)
                } else {
                    Result.failure(Exception("Failed to save config to file"))
                }
            } else {
                Result.failure(Exception("Config not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating config")
            Result.failure(e)
        }
    }

    /**
     * 删除配置 - 先写文件成功后再更新内存
     */
    suspend fun deleteConfig(configId: String): Result<Unit> {
        return try {
            val currentConfigs = _configs.value.toMutableList()
            currentConfigs.removeAll { it.id == configId }
            // 先写文件成功后再更新内存
            if (saveConfigs(currentConfigs)) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to save config to file"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting config")
            Result.failure(e)
        }
    }

    /**
     * 批量删除配置 - 先写文件成功后再更新内存
     */
    suspend fun deleteConfigs(configIds: List<String>): Result<Unit> {
        return try {
            val currentConfigs = _configs.value.toMutableList()
            currentConfigs.removeAll { it.id in configIds }
            // 先写文件成功后再更新内存
            if (saveConfigs(currentConfigs)) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to save config to file"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting configs")
            Result.failure(e)
        }
    }

    /**
     * 切换收藏状态 - 先写文件成功后再更新内存
     */
    suspend fun toggleFavorite(configId: String): Result<CameraConfig> {
        return try {
            val currentConfigs = _configs.value.toMutableList()
            val index = currentConfigs.indexOfFirst { it.id == configId }
            if (index != -1) {
                val updatedConfig = currentConfigs[index].copy(
                    isFavorite = !currentConfigs[index].isFavorite,
                    updatedAt = System.currentTimeMillis()
                )
                currentConfigs[index] = updatedConfig
                // 先写文件成功后再更新内存
                if (saveConfigs(currentConfigs)) {
                    Result.success(updatedConfig)
                } else {
                    Result.failure(Exception("Failed to save config to file"))
                }
            } else {
                Result.failure(Exception("Config not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error toggling favorite")
            Result.failure(e)
        }
    }

    /**
     * 获取所有分类
     */
    fun getCategories(): List<String> {
        return _configs.value.map { it.category }.distinct().sorted()
    }

    /**
     * 按分类筛选配置
     */
    fun getConfigsByCategory(category: String): List<CameraConfig> {
        return _configs.value.filter { it.category == category }
    }

    /**
     * 获取收藏的配置
     */
    fun getFavoriteConfigs(): List<CameraConfig> {
        return _configs.value.filter { it.isFavorite }
    }

    /**
     * 导出配置到文件
     */
    suspend fun exportConfig(config: CameraConfig, outputFile: File): Result<File> {
        return try {
            val exportData = CameraConfigExport(configs = listOf(config))
            FileWriter(outputFile).use { writer ->
                gson.toJson(exportData, writer)
            }
            Result.success(outputFile)
        } catch (e: Exception) {
            Timber.e(e, "Error exporting config")
            Result.failure(e)
        }
    }

    /**
     * 批量导出配置
     */
    suspend fun exportConfigs(configIds: List<String>, outputFile: File): Result<File> {
        return try {
            val configsToExport = _configs.value.filter { it.id in configIds }
            val exportData = CameraConfigExport(configs = configsToExport)
            FileWriter(outputFile).use { writer ->
                gson.toJson(exportData, writer)
            }
            Result.success(outputFile)
        } catch (e: Exception) {
            Timber.e(e, "Error exporting configs")
            Result.failure(e)
        }
    }

    /**
     * 导入配置文件 - 先写文件成功后再更新内存
     */
    suspend fun importConfig(inputFile: File): Result<List<CameraConfig>> {
        return try {
            // 安全检查: 验证导入文件
            if (!inputFile.exists() || !inputFile.canRead()) {
                return Result.failure(IllegalArgumentException("Import file does not exist or cannot be read"))
            }
            
            // 限制文件大小，防止内存攻击
            val maxFileSize = 1024 * 1024 // 1MB
            if (inputFile.length() > maxFileSize) {
                Timber.w("Import file too large: ${inputFile.length()} bytes")
                return Result.failure(IllegalArgumentException("File too large, max size is 1MB"))
            }
            
            FileReader(inputFile).use { reader ->
                val content = reader.readText()
                if (content.length > maxFileSize) {
                    return Result.failure(IllegalArgumentException("Content too large"))
                }
                
                val exportData = gson.fromJson(content, CameraConfigExport::class.java)
                
                // 验证导入数据
                if (exportData?.configs == null || exportData.configs.isEmpty()) {
                    return Result.failure(IllegalArgumentException("No valid configs in import file"))
                }
                
                // 验证每个配置的有效性
                val validConfigs = exportData.configs.filter { config ->
                    config.id.isNotEmpty() && config.name.isNotEmpty()
                }
                
                if (validConfigs.isEmpty()) {
                    return Result.failure(IllegalArgumentException("No valid configs after validation"))
                }
                
                val importedConfigs = validConfigs.map { config ->
                    config.copy(
                        id = sanitizeConfigId(config.id) + "_imported_${System.currentTimeMillis()}",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                }
                val currentConfigs = _configs.value.toMutableList()
                currentConfigs.addAll(0, importedConfigs)
                // 先写文件成功后再更新内存
                if (saveConfigs(currentConfigs)) {
                    Result.success(importedConfigs)
                } else {
                    Result.failure(Exception("Failed to save imported configs to file"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error importing config")
            Result.failure(e)
        }
    }
    
    /**
     * 验证并清理配置ID，防止注入攻击
     */
    private fun sanitizeConfigId(id: String): String {
        // 只允许安全字符：字母、数字、下划线、连字符
        return id.filter { char ->
            char.isLetterOrDigit() || char == '_' || char == '-'
        }.take(64).ifEmpty { "config" }
    }

    /**
     * 根据ID获取配置
     */
    fun getConfigById(id: String): CameraConfig? {
        return _configs.value.find { it.id == id }
    }

    /**
     * 搜索配置
     */
    fun searchConfigs(query: String): List<CameraConfig> {
        val lowercaseQuery = query.lowercase()
        return _configs.value.filter { config ->
            config.name.lowercase().contains(lowercaseQuery) ||
                    config.description.lowercase().contains(lowercaseQuery) ||
                    config.tags.any { it.lowercase().contains(lowercaseQuery) }
        }
    }

    override fun close() {
        // 清理资源，取消所有协程
        repositoryScope.coroutineContext.cancel()
        Timber.d("CameraConfigRepository closed, resources cleaned up")
    }
}