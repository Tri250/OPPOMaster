package com.omaster.app.data

import android.content.Context
import com.omaster.app.data.remote.PresetRemoteDataSource
import com.omaster.app.domain.model.Preset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresetRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteDataSource: PresetRemoteDataSource,
    private val historyManager: PresetHistoryManager
) {
    private val _presets = MutableStateFlow<List<Preset>>(emptyList())
    val presets: StateFlow<List<Preset>> = _presets.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // 初始化时从远程加载预设
        Timber.d("PresetRepository initialized")
    }

    /**
     * 从远程服务器加载预设
     */
    suspend fun loadPresetsFromRemote() {
        _isLoading.value = true
        _error.value = null
        
        try {
            val result = remoteDataSource.fetchAllPresets()
            result.onSuccess { presets ->
                _presets.value = presets
                // 加载本地收藏状态
                loadFavorites()
                Timber.d("Loaded ${presets.size} presets from remote")
            }.onFailure { error ->
                _error.value = error.message
                Timber.e(error, "Failed to load presets from remote")
            }
        } catch (e: Exception) {
            _error.value = e.message
            Timber.e(e, "Exception while loading presets")
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * 获取所有预设
     */
    fun getAllPresets(): Flow<List<Preset>> = flow {
        emit(_presets.value)
    }

    /**
     * 根据ID获取预设
     */
    fun getPresetById(id: String): Preset? {
        return _presets.value.find { it.id == id }
    }

    /**
     * 切换收藏状态
     */
    suspend fun toggleFavorite(presetId: String) {
        val currentPresets = _presets.value.toMutableList()
        val index = currentPresets.indexOfFirst { it.id == presetId }
        if (index != -1) {
            val preset = currentPresets[index]
            currentPresets[index] = preset.copy(isFavorite = !preset.isFavorite)
            _presets.value = currentPresets
            
            // 保存到本地
            saveFavorites()
            Timber.d("Toggled favorite for preset: $presetId")
        }
    }

    /**
     * 获取收藏的预设
     */
    fun getFavoritePresets(): List<Preset> {
        return _presets.value.filter { it.isFavorite }
    }

    /**
     * 根据标签筛选预设
     */
    fun getPresetsByTag(tag: String): List<Preset> {
        return _presets.value.filter { preset ->
            preset.tags.any { it.equals(tag, ignoreCase = true) }
        }
    }

    /**
     * 根据设备筛选预设
     */
    fun getPresetsByDevice(device: String): List<Preset> {
        return _presets.value.filter { preset ->
            preset.supportedDevices.any { it.contains(device, ignoreCase = true) }
        }
    }

    /**
     * 搜索预设
     */
    fun searchPresets(query: String): List<Preset> {
        if (query.isBlank()) return _presets.value
        
        val normalizedQuery = query.lowercase().trim()
        return _presets.value.filter { preset ->
            preset.name.lowercase().contains(normalizedQuery) ||
            preset.description.lowercase().contains(normalizedQuery) ||
            preset.tags.any { it.lowercase().contains(normalizedQuery) } ||
            preset.author?.lowercase()?.contains(normalizedQuery) == true
        }
    }

    /**
     * 记录预设使用
     */
    suspend fun recordPresetUsage(presetId: String) {
        historyManager.addToHistory(presetId)
        
        // 更新使用次数
        val currentPresets = _presets.value.toMutableList()
        val index = currentPresets.indexOfFirst { it.id == presetId }
        if (index != -1) {
            val preset = currentPresets[index]
            currentPresets[index] = preset.copy(useCount = preset.useCount + 1)
            _presets.value = currentPresets
        }
        
        Timber.d("Recorded usage for preset: $presetId")
    }

    /**
     * 获取最近使用的预设
     */
    suspend fun getRecentPresets(limit: Int = 10): List<Preset> {
        val history = historyManager.getHistory()
        return history.take(limit).mapNotNull { presetId ->
            _presets.value.find { it.id == presetId }
        }
    }

    /**
     * 同步预设（从远程刷新）
     */
    suspend fun syncPresets() {
        Timber.d("Syncing presets from remote...")
        loadPresetsFromRemote()
    }

    /**
     * 获取热门预设
     */
    fun getTrendingPresets(limit: Int = 10): List<Preset> {
        return _presets.value
            .sortedByDescending { it.useCount }
            .take(limit)
    }

    /**
     * 获取最新预设
     */
    fun getNewPresets(limit: Int = 10): List<Preset> {
        return _presets.value
            .sortedByDescending { it.createdAt }
            .take(limit)
    }

    /**
     * 保存收藏到本地
     */
    private fun saveFavorites() {
        val favorites = _presets.value
            .filter { it.isFavorite }
            .map { it.id }
            .toSet()
        
        context.getSharedPreferences("presets_prefs", Context.MODE_PRIVATE)
            .edit()
            .putStringSet("favorites", favorites)
            .apply()
    }

    /**
     * 从本地加载收藏
     */
    private fun loadFavorites() {
        val favorites = context.getSharedPreferences("presets_prefs", Context.MODE_PRIVATE)
            .getStringSet("favorites", emptySet()) ?: emptySet()
        
        val currentPresets = _presets.value.toMutableList()
        currentPresets.forEachIndexed { index, preset ->
            if (preset.id in favorites) {
                currentPresets[index] = preset.copy(isFavorite = true)
            }
        }
        _presets.value = currentPresets
    }
}
