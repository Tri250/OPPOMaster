package com.omaster.app.accessibility

import android.content.Context
import android.os.Build
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MappingTableManager @Inject constructor(
    private val context: Context,
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val MAPPING_TABLE_URL = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/mapping/device_mapping.json"
        private const val CACHE_KEY = "device_mapping_table"
        private const val CACHE_VERSION_KEY = "device_mapping_version"
        private const val CURRENT_VERSION = "2026.05.29"
    }
    
    private val gson = Gson()
    private var cachedMapping: MutableMap<String, DeviceMappingTable.CameraAppInfo>? = null
    private var currentMappingVersion: String? = null
    
    suspend fun loadMappingTable(): Map<String, DeviceMappingTable.CameraAppInfo> {
        return cachedMapping ?: loadFromNetwork().also { cachedMapping = it.toMutableMap() }
    }
    
    private suspend fun loadFromNetwork(): Map<String, DeviceMappingTable.CameraAppInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(MAPPING_TABLE_URL)
                .build()
            
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.w("Failed to load mapping table: ${response.code}")
                    return@use getDefaultMapping()
                }
                
                val json = response.body?.string() ?: return@use getDefaultMapping()
                val type = object : TypeToken<Map<String, DeviceMappingTable.CameraAppInfo>>() {}.type
                val mapping: Map<String, DeviceMappingTable.CameraAppInfo> = gson.fromJson(json, type)
                
                saveToCache(json)
                currentMappingVersion = CURRENT_VERSION
                
                Timber.d("Mapping table loaded: ${mapping.size} entries")
                mapping
            }
        } catch (e: IOException) {
            Timber.e(e, "Network error loading mapping table")
            loadFromCache() ?: getDefaultMapping()
        } catch (e: Exception) {
            Timber.e(e, "Error loading mapping table")
            loadFromCache() ?: getDefaultMapping()
        }
    }
    
    private fun getDefaultMapping(): Map<String, DeviceMappingTable.CameraAppInfo> {
        return DeviceMappingTable.cameraApps.associateBy { it.packageName }
    }
    
    private fun saveToCache(json: String) {
        try {
            val prefs = context.getSharedPreferences("omaster_mapping", Context.MODE_PRIVATE)
            prefs.edit()
                .putString(CACHE_KEY, json)
                .putString(CACHE_VERSION_KEY, CURRENT_VERSION)
                .apply()
            Timber.d("Mapping table cached")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cache mapping table")
        }
    }
    
    private fun loadFromCache(): Map<String, DeviceMappingTable.CameraAppInfo>? {
        return try {
            val prefs = context.getSharedPreferences("omaster_mapping", Context.MODE_PRIVATE)
            val json = prefs.getString(CACHE_KEY, null) ?: return null
            val version = prefs.getString(CACHE_VERSION_KEY, null) ?: return null
            
            if (version != CURRENT_VERSION) {
                Timber.d("Cached mapping version $version is outdated")
                return null
            }
            
            val type = object : TypeToken<Map<String, DeviceMappingTable.CameraAppInfo>>() {}.type
            val mapping: Map<String, DeviceMappingTable.CameraAppInfo> = gson.fromJson(json, type)
            
            Timber.d("Mapping table loaded from cache: ${mapping.size} entries")
            mapping
        } catch (e: Exception) {
            Timber.e(e, "Failed to load mapping from cache")
            null
        }
    }
    
    fun clearCache() {
        try {
            val prefs = context.getSharedPreferences("omaster_mapping", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            cachedMapping = null
            Timber.d("Mapping cache cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear mapping cache")
        }
    }
    
    suspend fun updateMappingTable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(MAPPING_TABLE_URL)
                .build()
            
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.w("Failed to update mapping table: ${response.code}")
                    return@use false
                }
                
                val json = response.body?.string() ?: return@use false
                
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val data: Map<String, Any> = gson.fromJson(json, type)
                
                if (data.containsKey("version")) {
                    val newVersion = data["version"].toString()
                    if (newVersion > (currentMappingVersion ?: "0")) {
                        saveToCache(json)
                        currentMappingVersion = newVersion
                        cachedMapping = null
                        Timber.d("Mapping table updated to version $newVersion")
                        return@use true
                    }
                }
                
                saveToCache(json)
                cachedMapping = null
                true
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update mapping table")
            false
        }
    }
    
    fun getDeviceInfo(packageName: String): DeviceMappingTable.CameraAppInfo? {
        return cachedMapping?.get(packageName) 
            ?: DeviceMappingTable.getCameraApp(packageName)
    }
    
    fun isMappingCached(): Boolean {
        return cachedMapping != null
    }
    
    fun getMappingVersion(): String? {
        return currentMappingVersion
    }
}

class DeviceDetector {
    
    data class DeviceInfo(
        val brand: String,
        val model: String,
        val manufacturer: String,
        val packageName: String
    )
    
    fun detectCurrentDevice(): DeviceInfo {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val model = Build.MODEL
        
        val packageName = when {
            manufacturer.contains("oppo") || brand.contains("oppo") -> "com.oppo.camera"
            manufacturer.contains("oneplus") || brand.contains("oneplus") -> "com.oneplus.camera"
            manufacturer.contains("realme") || brand.contains("realme") -> "com.realme.camera"
            manufacturer.contains("xiaomi") || brand.contains("xiaomi") || brand.contains("redmi") -> "com.android.camera"
            manufacturer.contains("vivo") || brand.contains("vivo") -> "com.vivo.camera"
            manufacturer.contains("huawei") || brand.contains("huawei") -> "com.huawei.camera"
            manufacturer.contains("honor") || brand.contains("honor") -> "com.hihonor.camera"
            manufacturer.contains("meizu") || brand.contains("meizu") -> "com.meizu.camera"
            manufacturer.contains("zte") || brand.contains("zte") -> "com.zte.camera"
            manufacturer.contains("nubia") || brand.contains("nubia") -> "com.nubia.camera"
            manufacturer.contains("lenovo") || brand.contains("lenovo") -> "com.lenovo.camera"
            manufacturer.contains("motorola") || brand.contains("motorola") -> "com.motorola.camera"
            manufacturer.contains("samsung") || brand.contains("samsung") -> "com.samsung.android.camera"
            manufacturer.contains("tcl") || brand.contains("tcl") -> "com.tcl.camera"
            manufacturer.contains("hisense") || brand.contains("hisense") -> "com.hisense.camera"
            manufacturer.contains("coolpad") || brand.contains("coolpad") -> "com.coolpad.camera"
            manufacturer.contains("smartisan") || brand.contains("smartisan") -> "com.smartisan.camera"
            else -> "com.android.camera"
        }
        
        return DeviceInfo(
            brand = Build.BRAND,
            model = model,
            manufacturer = Build.MANUFACTURER,
            packageName = packageName
        )
    }
    
    fun getSupportedBrands(): List<String> {
        return listOf(
            "OPPO",
            "OnePlus",
            "realme",
            "小米",
            "vivo",
            "iQOO",
            "华为",
            "荣耀",
            "魅族",
            "中兴",
            "努比亚",
            "联想",
            "摩托罗拉",
            "三星",
            "TCL",
            "海信",
            "酷派",
            "锤子"
        )
    }
    
    fun isBrandSupported(brand: String): Boolean {
        return getSupportedBrands().any { it.equals(brand, ignoreCase = true) }
    }
}
