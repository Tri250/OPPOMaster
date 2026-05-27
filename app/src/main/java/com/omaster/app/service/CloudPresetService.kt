package com.omaster.app.service

import android.content.Context
import com.omaster.app.model.CloudPresetConstants
import com.omaster.app.model.CloudPresetResponse
import com.omaster.app.model.Preset
import com.omaster.app.model.toPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class CloudPresetService(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val cacheDir: File by lazy {
        File(context.cacheDir, "presets").apply { mkdirs() }
    }

    suspend fun loadOppoPresets(): List<Preset> {
        return loadPresetsFromUrl(CloudPresetConstants.OPPO_PRESETS_URL)
    }

    suspend fun loadRealmePresets(): List<Preset> {
        return loadPresetsFromUrl(CloudPresetConstants.REALME_PRESETS_URL)
    }

    suspend fun loadAllPresets(): List<Preset> {
        return withContext(Dispatchers.IO) {
            val oppoPresets = loadOppoPresets()
            val realmePresets = loadRealmePresets()
            oppoPresets + realmePresets
        }
    }

    private suspend fun loadPresetsFromUrl(url: String): List<Preset> {
        return withContext(Dispatchers.IO) {
            try {
                val cacheFile = File(cacheDir, url.hashCode().toString())
                
                if (cacheFile.exists() && isCacheValid(cacheFile)) {
                    Timber.d("Loading presets from cache: $url")
                    val cachedContent = cacheFile.readText()
                    return@withContext parsePresets(cachedContent)
                }

                Timber.d("Loading presets from network: $url")
                val request = Request.Builder().url(url).build()
                
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Timber.e("Failed to fetch presets: ${response.code}")
                        return@withContext emptyList()
                    }

                    val content = response.body?.string() ?: return@withContext emptyList()
                    
                    cacheFile.writeText(content)
                    return@withContext parsePresets(content)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load presets from $url")
                emptyList()
            }
        }
    }

    private fun parsePresets(content: String): List<Preset> {
        return try {
            val response = json.decodeFromString<CloudPresetResponse>(content)
            Timber.d("Loaded ${response.presets.size} presets from ${response.name}")
            response.presets.map { it.toPreset() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse presets")
            emptyList()
        }
    }

    private fun isCacheValid(cacheFile: File): Boolean {
        val lastModified = cacheFile.lastModified()
        val now = System.currentTimeMillis()
        return now - lastModified < CloudPresetConstants.CACHE_DURATION_MS
    }

    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
        Timber.d("Preset cache cleared")
    }

    suspend fun refreshPresets(): List<Preset> {
        clearCache()
        return loadAllPresets()
    }
}
