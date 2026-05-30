package com.omaster.app.data

import com.omaster.app.model.Preset
import com.omaster.app.network.PresetApi
import com.omaster.app.util.JsonUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresetRepository @Inject constructor(
    private val presetApi: PresetApi,
    private val preferencesDataStore: PreferencesDataStore,
    private val jsonUtil: JsonUtil
) {
    private val _presets = MutableStateFlow<List<Preset>>(emptyList())
    val presets: StateFlow<List<Preset>> = _presets.asStateFlow()

    private var cachedPresets: List<Preset> = emptyList()
    private var favoriteIds: Set<String> = emptySet()
    private var isDataLoaded = false

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        repositoryScope.launch {
            preferencesDataStore.favoritePresets.collect { favorites ->
                favoriteIds = favorites
                if (cachedPresets.isNotEmpty()) {
                    _presets.value = applyFavoriteStates(cachedPresets)
                }
            }
        }
    }

    suspend fun loadPresets() {
        if (isDataLoaded && cachedPresets.isNotEmpty()) {
            return
        }

        val presets = try {
            fetchRemotePresets()
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch remote presets, falling back to local")
            loadLocalPresets()
        }

        cachedPresets = presets
        _presets.value = applyFavoriteStates(presets)
        isDataLoaded = true
    }

    suspend fun refreshPresets() {
        isDataLoaded = false
        cachedPresets = emptyList()
        _presets.value = emptyList()
        loadPresets()
    }

    private suspend fun fetchRemotePresets(): List<Preset> {
        return try {
            // 先尝试获取 OPPO 预设
            val oppoResponse = presetApi.getOppoPresets()
            val oppoPresets = if (oppoResponse.isSuccessful && oppoResponse.body() != null) {
                jsonUtil.convertRemoteToLocalPresets(oppoResponse.body()!!.presets, "oppo")
            } else {
                Timber.w("Failed to fetch OPPO presets")
                emptyList()
            }

            // 再尝试获取 Realme 预设
            val realmeResponse = presetApi.getRealmePresets()
            val realmePresets = if (realmeResponse.isSuccessful && realmeResponse.body() != null) {
                jsonUtil.convertRemoteToLocalPresets(realmeResponse.body()!!.presets, "realme")
            } else {
                Timber.w("Failed to fetch Realme presets")
                emptyList()
            }

            // 合并两个来源的预设
            val allPresets = oppoPresets + realmePresets
            
            if (allPresets.isNotEmpty()) {
                // 保存到本地缓存
                jsonUtil.saveToLocalCache(allPresets)
                Timber.d("Remote presets fetched and cached: ${allPresets.size}")
                return allPresets
            }
            
            // 如果远程没有数据，加载本地数据
            loadLocalPresets()
        } catch (e: Exception) {
            Timber.e(e, "Error fetching remote presets")
            loadLocalPresets()
        }
    }

    private suspend fun loadLocalPresets(): List<Preset> {
        return jsonUtil.loadPresets().ifEmpty {
            Timber.w("No presets found in local storage")
            emptyList()
        }
    }

    private fun applyFavoriteStates(presets: List<Preset>): List<Preset> {
        return presets.map { preset ->
            preset.copy(isFavorite = favoriteIds.contains(preset.id))
        }
    }

    suspend fun toggleFavorite(presetId: String) {
        preferencesDataStore.toggleFavorite(presetId)
    }

    fun getLocalCacheVersion(): String? = jsonUtil.getCacheVersion()

    fun hasLocalCache(): Boolean = jsonUtil.hasLocalCache()

    fun clearCache() {
        jsonUtil.clearCache()
        isDataLoaded = false
        cachedPresets = emptyList()
        _presets.value = emptyList()
    }
}
