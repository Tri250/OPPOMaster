package com.omaster.app.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.omaster.app.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val mutex = Mutex()
    private var cachedPresets: List<Preset>? = null

    // 远程数据到本地数据的转换函数
    fun convertRemoteToLocalPresets(remotePresets: List<RemotePreset>, source: String): List<Preset> {
        return remotePresets.mapIndexed { index, remote ->
            Preset(
                id = "preset_${index}_${remote.name.hashCode()}",
                name = remote.name,
                coverPath = remote.coverPath,
                deviceModel = if (source == "oppo") "OPPO Find X8" else "Realme GT7",
                source = source,
                sections = remote.sections?.map { section ->
                    Section(
                        title = section.title,
                        content = section.items?.joinToString(", ") { "${it.label}: ${it.value}" } ?: ""
                    )
                } ?: emptyList(),
                cameraParams = extractCameraParamsFromRemote(remote),
                category = detectCategoryFromRemote(remote)
            )
        }
    }

    private fun extractCameraParamsFromRemote(remote: RemotePreset): CameraParams {
        var params = CameraParams()
        
        remote.sections?.forEach { section ->
            section.items?.forEach { item ->
                val label = item.label.lowercase()
                val value = item.value
                
                when {
                    label.contains("iso") -> {
                        value.toIntOrNull()?.let { params = params.copy(iso = it) }
                    }
                    label.contains("快门") -> {
                        params = params.copy(shutter = value)
                    }
                    label.contains("曝光") || label.contains("ev") -> {
                        params = params.copy(ev = value)
                    }
                    label.contains("白平衡") || label.contains("wb") -> {
                        params = params.copy(wb = value)
                    }
                    label.contains("滤镜") -> {
                        params = params.copy(filter = value)
                    }
                    label.contains("饱和度") -> {
                        value.toIntOrNull()?.let { params = params.copy(saturation = it) }
                    }
                    label.contains("对比度") -> {
                        value.toIntOrNull()?.let { params = params.copy(contrast = it) }
                    }
                    label.contains("锐度") -> {
                        value.toIntOrNull()?.let { params = params.copy(sharpness = it) }
                    }
                    label.contains("暗角") -> {
                        value.toFloatOrNull()?.let { params = params.copy(vignette = it) }
                    }
                }
            }
        }
        
        return params
    }

    private fun detectCategoryFromRemote(remote: RemotePreset): PresetCategory? {
        val name = remote.name.lowercase()
        val tags = remote.tags?.map { it.lowercase() } ?: emptyList()
        
        return when {
            name.contains("人像") || tags.contains("人像") -> PresetCategory.PORTRAIT
            name.contains("风景") || name.contains("自然") || tags.contains("风景") -> PresetCategory.LANDSCAPE
            name.contains("夜景") || tags.contains("夜景") -> PresetCategory.NIGHT
            name.contains("美食") || tags.contains("美食") -> PresetCategory.FOOD
            name.contains("街拍") || tags.contains("街拍") -> PresetCategory.STREET
            name.contains("建筑") || tags.contains("建筑") -> PresetCategory.ARCHITECTURE
            name.contains("日落") || name.contains("日出") -> PresetCategory.SUNSET
            name.contains("微距") -> PresetCategory.MACRO
            name.contains("运动") -> PresetCategory.SPORTS
            name.contains("胶片") || name.contains("复古") -> PresetCategory.VINTAGE
            name.contains("黑白") -> PresetCategory.BLACK_WHITE
            name.contains("电影") || name.contains("cinematic") -> PresetCategory.CINEMATIC
            else -> null
        }
    }

    suspend fun loadPresets(): List<Preset> = withContext(Dispatchers.IO) {
        mutex.withLock {
            cachedPresets?.let { return@withContext it }
        }

        try {
            // 先尝试从本地缓存加载
            if (hasLocalCache()) {
                val presets = loadFromLocalCache()
                if (presets.isNotEmpty()) {
                    mutex.withLock {
                        cachedPresets = presets
                    }
                    return@withContext presets
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load presets from cache")
        }

        // 如果缓存失败，从 assets 加载
        loadFromAssets()
    }

    private fun loadFromAssets(): List<Preset> {
        return try {
            val json = context.assets.open("presets.json").bufferedReader().use { it.readText() }
            parsePresetsJson(json)
        } catch (e: IOException) {
            Timber.e(e, "Failed to read presets.json from assets")
            emptyList()
        }
    }

    private fun parsePresetsJson(json: String): List<Preset> {
        return try {
            // 先尝试解析为 PresetList（旧格式）
            val presetList = gson.fromJson(json, PresetList::class.java)
            if (presetList.presets.isNotEmpty()) {
                return presetList.presets
            }
            
            // 如果失败，尝试解析为 RemotePresetResponse（新格式）
            val remoteResponse = gson.fromJson(json, RemotePresetResponse::class.java)
            if (remoteResponse.presets.isNotEmpty()) {
                return convertRemoteToLocalPresets(remoteResponse.presets, "community")
            }
            
            emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse presets JSON")
            emptyList()
        }
    }

    suspend fun saveToLocalCache(presets: List<Preset>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val cacheDir = File(context.filesDir, "presets_cache")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                
                val presetList = PresetList(
                    version = "2.0.0",
                    lastUpdated = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                    presets = presets
                )
                val json = gson.toJson(presetList)
                
                val file = File(cacheDir, "local_presets.json")
                file.writeText(json)
                
                Timber.d("Presets saved to local cache: ${presets.size}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to save presets to local cache")
            }
        }
    }

    private fun loadFromLocalCache(): List<Preset> {
        return try {
            val file = File(File(context.filesDir, "presets_cache"), "local_presets.json")
            if (!file.exists()) return emptyList()
            
            val json = file.readText()
            parsePresetsJson(json)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load presets from local cache")
            emptyList()
        }
    }

    fun hasLocalCache(): Boolean {
        return try {
            val file = File(File(context.filesDir, "presets_cache"), "local_presets.json")
            file.exists()
        } catch (e: Exception) {
            false
        }
    }

    fun getCacheVersion(): String? {
        return try {
            val file = File(File(context.filesDir, "presets_cache"), "local_presets.json")
            if (!file.exists()) return null
            
            val json = file.readText()
            val presetList = gson.fromJson(json, PresetList::class.java)
            presetList.version
        } catch (e: Exception) {
            null
        }
    }

    fun clearCache() {
        mutex.withLock {
            cachedPresets = null
            try {
                val file = File(File(context.filesDir, "presets_cache"), "local_presets.json")
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear cache")
            }
        }
    }
}
