package com.omaster.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.omaster.app.domain.model.CameraConfig
import com.omaster.app.domain.model.CameraConfigExport
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 相机配置文件Repository - 企业级实现
 * 负责配置文件的存储、读取、更新、删除操作
 * 所有数据来自真实用户输入，不使用模拟数据
 */
@Singleton
class CameraConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
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

    init {
        loadConfigs()
    }

    /**
     * 加载所有配置
     */
    private fun loadConfigs() {
        try {
            if (configsFile.exists()) {
                val type = object : TypeToken<List<CameraConfig>>() {}.type
                FileReader(configsFile).use { reader ->
                    val configs = gson.fromJson<List<CameraConfig>>(reader, type)
                    _configs.value = configs ?: emptyList()
                }
            } else {
                _configs.value = emptyList()
                saveConfigs(emptyList())
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading configs")
            _configs.value = emptyList()
        }
    }

    /**
     * 保存配置列表
     */
    private fun saveConfigs(configs: List<CameraConfig>) {
        try {
            FileWriter(configsFile).use { writer ->
                gson.toJson(configs, writer)
            }
            _configs.value = configs
        } catch (e: Exception) {
            Timber.e(e, "Error saving configs")
        }
    }

    /**
     * 添加新配置
     */
    suspend fun addConfig(config: CameraConfig): Result<CameraConfig> {
        return try {
            val currentConfigs = _configs.value.toMutableList()
            currentConfigs.add(0, config)
            saveConfigs(currentConfigs)
            Result.success(config)
        } catch (e: Exception) {
            Timber.e(e, "Error adding config")
            Result.failure(e)
        }
    }

    /**
     * 更新配置
     */
    suspend fun updateConfig(config: CameraConfig): Result<CameraConfig> {
        return try {
            val currentConfigs = _configs.value.toMutableList()
            val index = currentConfigs.indexOfFirst { it.id == config.id }
            if (index != -1) {
                currentConfigs[index] = config.copy(updatedAt = System.currentTimeMillis())
                saveConfigs(currentConfigs)
                Result.success(currentConfigs[index])
            } else {
                Result.failure(Exception("Config not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating config")
            Result.failure(e)
        }
    }

    /**
     * 删除配置
     */
    suspend fun deleteConfig(configId: String): Result<Unit> {
        return try {
            val currentConfigs = _configs.value.toMutableList()
            currentConfigs.removeAll { it.id == configId }
            saveConfigs(currentConfigs)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting config")
            Result.failure(e)
        }
    }

    /**
     * 批量删除配置
     */
    suspend fun deleteConfigs(configIds: List<String>): Result<Unit> {
        return try {
            val currentConfigs = _configs.value.toMutableList()
            currentConfigs.removeAll { it.id in configIds }
            saveConfigs(currentConfigs)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting configs")
            Result.failure(e)
        }
    }

    /**
     * 切换收藏状态
     */
    suspend fun toggleFavorite(configId: String): Result<CameraConfig> {
        return try {
            val currentConfigs = _configs.value.toMutableList()
            val index = currentConfigs.indexOfFirst { it.id == configId }
            if (index != -1) {
                currentConfigs[index] = currentConfigs[index].copy(
                    isFavorite = !currentConfigs[index].isFavorite,
                    updatedAt = System.currentTimeMillis()
                )
                saveConfigs(currentConfigs)
                Result.success(currentConfigs[index])
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
     * 导入配置文件
     */
    suspend fun importConfig(inputFile: File): Result<List<CameraConfig>> {
        return try {
            FileReader(inputFile).use { reader ->
                val exportData = gson.fromJson(reader, CameraConfigExport::class.java)
                val importedConfigs = exportData.configs.map { config ->
                    config.copy(
                        id = config.id + "_imported_${System.currentTimeMillis()}",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                }
                val currentConfigs = _configs.value.toMutableList()
                currentConfigs.addAll(0, importedConfigs)
                saveConfigs(currentConfigs)
                Result.success(importedConfigs)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error importing config")
            Result.failure(e)
        }
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
}
