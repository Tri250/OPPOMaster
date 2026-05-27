package com.omaster.app.service

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.omaster.app.model.Preset
import com.omaster.app.model.Section
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

data class CloudPresetResponse(
    @SerializedName("version") val version: Int,
    @SerializedName("name") val name: String,
    @SerializedName("author") val author: String,
    @SerializedName("build") val build: Int,
    @SerializedName("presets") val presets: List<CloudPreset>
)

data class CloudPreset(
    @SerializedName("name") val name: String,
    @SerializedName("cover_path") val coverPath: String,
    @SerializedName("gallery_images") val galleryImages: List<String> = emptyList(),
    @SerializedName("author") val author: String,
    @SerializedName("is_new") val isNew: Boolean = false,
    @SerializedName("sections") val sections: List<CloudSection>,
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("description") val description: CloudDescription? = null
)

data class CloudSection(
    @SerializedName("title") val title: String,
    @SerializedName("items") val items: List<CloudParamItem>
)

data class CloudParamItem(
    @SerializedName("label") val label: String,
    @SerializedName("value") val value: String,
    @SerializedName("span") val span: Int = 1
)

data class CloudDescription(
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String
)

class CloudPresetService(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val cacheMutex = Mutex()

    private val cacheDir: File by lazy {
        File(context.cacheDir, "presets").apply { mkdirs() }
    }

    private var cachedPresets: List<Preset>? = null
    private var lastCacheTime: Long = 0

    suspend fun loadOppoPresets(): List<Preset> {
        return loadPresetsFromUrl(OPPO_PRESETS_URL)
    }

    suspend fun loadRealmePresets(): List<Preset> {
        return loadPresetsFromUrl(REALME_PRESETS_URL)
    }

    suspend fun loadAllPresets(): List<Preset> {
        return withContext(Dispatchers.IO) {
            val oppoPresets = loadOppoPresets()
            val realmePresets = loadRealmePresets()
            oppoPresets + realmePresets
        }
    }

    private suspend fun loadPresetsFromUrl(url: String): List<Preset> {
        if (!isValidUrl(url)) {
            Timber.w("Invalid preset URL: $url")
            return emptyList()
        }

        return cacheMutex.withLock {
            try {
                if (cachedPresets != null && System.currentTimeMillis() - lastCacheTime < CACHE_DURATION_MS) {
                    Timber.d("Returning cached presets")
                    return@withLock cachedPresets!!
                }

                val cacheFile = File(cacheDir, url.hashCode().toString())

                if (cacheFile.exists() && isCacheValid(cacheFile)) {
                    Timber.d("Loading presets from cache: $url")
                    val cachedContent = cacheFile.readText()
                    val presets = parsePresets(cachedContent)
                    cachedPresets = presets
                    lastCacheTime = System.currentTimeMillis()
                    return@withLock presets
                }

                Timber.d("Loading presets from network: $url")
                val request = Request.Builder().url(url).build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Timber.e("Failed to fetch presets: ${response.code}")
                        return@withLock cachedPresets ?: emptyList()
                    }

                    val content = response.body?.string() ?: return@withLock cachedPresets ?: emptyList()

                    try {
                        cacheFile.writeText(content)
                    } catch (e: Exception) {
                        Timber.w("Failed to write cache file: ${e.message}")
                    }

                    val presets = parsePresets(content)
                    cachedPresets = presets
                    lastCacheTime = System.currentTimeMillis()
                    return@withLock presets
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load presets from $url")
                return@withLock cachedPresets ?: emptyList()
            }
        }
    }

    private fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) {
            Timber.w("URL is blank")
            return false
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Timber.w("URL does not start with http:// or https://: $url")
            return false
        }

        val invalidChars = listOf("<", ">", "\"", "'", " ", "\n", "\r")
        for (char in invalidChars) {
            if (url.contains(char)) {
                Timber.w("URL contains invalid character '$char': $url")
                return false
            }
        }

        if (!url.contains(".")) {
            Timber.w("URL does not appear to be a valid domain: $url")
            return false
        }

        return true
    }

    private fun parsePresets(content: String): List<Preset> {
        return try {
            val response = gson.fromJson(content, CloudPresetResponse::class.java)
            Timber.d("Loaded ${response.presets.size} presets from ${response.name}")
            response.presets.mapIndexed { index, cloudPreset ->
                cloudPreset.toPreset(index)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse presets")
            emptyList()
        }
    }

    private fun CloudPreset.toPreset(index: Int): Preset {
        return Preset(
            id = "${name.replace(" ", "_")}_$index",
            name = name,
            coverPath = coverPath,
            sections = sections.map { section ->
                Section(
                    title = section.title.replace("@string/", ""),
                    content = section.items.joinToString("\n") { "${it.label}: ${it.value}" }
                )
            },
            cameraParams = parseCameraParams(sections),
            deviceModel = "Community",
            source = "omaster_community",
            isFavorite = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            usageCount = 0,
            rating = 4.5f,
            author = author
        )
    }

    private fun parseCameraParams(sections: List<CloudSection>): com.omaster.app.model.CameraParams? {
        val params = mutableMapOf<String, String>()
        
        sections.forEach { section ->
            section.items.forEach { item ->
                val label = item.label.replace("@string/param_", "")
                params[label] = item.value
            }
        }
        
        return com.omaster.app.model.CameraParams(
            mode = "master",
            filter = params["filter"] ?: "",
            iso = params["iso"]?.toIntOrNull() ?: 100,
            shutter = params["shutter"] ?: "1/125",
            ev = params["ev"] ?: "0",
            wb = params["wb"] ?: "5500K",
            hasselblad_hncs = false,
            contrast = parseFloatParam(params["contrast"]) ?: 1.0f,
            saturation = parseFloatParam(params["saturation"]) ?: 1.0f,
            sharpness = parseFloatParam(params["sharpness"]) ?: 1.0f,
            vignette = if (params["vignette"] == "开") 0.2f else 0.0f,
            videoLut = "",
            sceneTags = emptyList()
        )
    }

    private fun parseFloatParam(value: String?): Float? {
        if (value == null) return null
        
        val cleanValue = value.replace("+", "").replace("%", "")
        return try {
            cleanValue.toFloat() / 100.0f
        } catch (e: Exception) {
            null
        }
    }

    private fun isCacheValid(cacheFile: File): Boolean {
        val lastModified = cacheFile.lastModified()
        val now = System.currentTimeMillis()
        return now - lastModified < CACHE_DURATION_MS
    }

    fun clearCache() {
        cacheMutex.tryLock()?.let {
            try {
                cacheDir.listFiles()?.forEach { it.delete() }
                cachedPresets = null
                lastCacheTime = 0
                Timber.d("Preset cache cleared")
            } finally {
                cacheMutex.unlock()
            }
        }
    }

    suspend fun refreshPresets(): List<Preset> {
        cacheMutex.withLock {
            cachedPresets = null
            lastCacheTime = 0
        }
        return loadAllPresets()
    }

    companion object {
        const val OPPO_PRESETS_URL = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json"
        const val REALME_PRESETS_URL = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json"
        const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L
    }
}
