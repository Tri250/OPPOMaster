package com.omaster.app.data

import com.omaster.app.domain.model.CameraConfig
import com.omaster.app.domain.model.Preset
import com.omaster.app.domain.model.PresetTag
import com.omaster.app.domain.model.ISO
import com.omaster.app.domain.model.ShutterSpeed
import com.omaster.app.domain.model.Aperture
import com.omaster.app.domain.model.EV
import com.omaster.app.domain.model.WB
import com.omaster.app.domain.model.FocusMode
import com.omaster.app.data.remote.PresetApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 预设数据扩展器 - 从远程API加载真实预设数据
 * 不再使用硬编码模拟数据，所有数据来自远程服务器
 */
@Singleton
class PresetDataExpander @Inject constructor(
    private val presetApiService: PresetApiService
) {
    /**
     * 从远程API加载所有预设数据
     */
    suspend fun loadAllPresets(): List<Preset> = withContext(Dispatchers.IO) {
        try {
            val response = presetApiService.getAllPresets()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 从远程API加载OPPO预设
     */
    suspend fun loadOppoPresets(): List<Preset> = withContext(Dispatchers.IO) {
        try {
            val response = presetApiService.getOppoPresets()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 从远程API加载Realme预设
     */
    suspend fun loadRealmePresets(): List<Preset> = withContext(Dispatchers.IO) {
        try {
            val response = presetApiService.getRealmePresets()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 从远程API加载Honor预设
     */
    suspend fun loadHonorPresets(): List<Preset> = withContext(Dispatchers.IO) {
        try {
            val response = presetApiService.getHonorPresets()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 从远程API加载Vivo预设
     */
    suspend fun loadVivoPresets(): List<Preset> = withContext(Dispatchers.IO) {
        try {
            val response = presetApiService.getVivoPresets()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 按品牌分类加载预设
     */
    suspend fun loadPresetsByBrand(brand: String): List<Preset> = withContext(Dispatchers.IO) {
        when (brand.lowercase()) {
            "oppo" -> loadOppoPresets()
            "realme" -> loadRealmePresets()
            "honor" -> loadHonorPresets()
            "vivo" -> loadVivoPresets()
            else -> loadAllPresets()
        }
    }

    /**
     * 搜索预设
     */
    suspend fun searchPresets(query: String): List<Preset> = withContext(Dispatchers.IO) {
        val allPresets = loadAllPresets()
        allPresets.filter { preset ->
            preset.name.contains(query, ignoreCase = true) ||
            preset.description.contains(query, ignoreCase = true) ||
            preset.tags.any { it.name.contains(query, ignoreCase = true) }
        }
    }

    /**
     * 按标签筛选预设
     */
    suspend fun filterByTag(tag: PresetTag): List<Preset> = withContext(Dispatchers.IO) {
        val allPresets = loadAllPresets()
        allPresets.filter { preset ->
            preset.tags.any { it.name == tag.name }
        }
    }

    /**
     * 获取热门预设（按下载量排序）
     */
    suspend fun getPopularPresets(limit: Int = 10): List<Preset> = withContext(Dispatchers.IO) {
        val allPresets = loadAllPresets()
        allPresets.sortedByDescending { it.downloadCount }.take(limit)
    }

    /**
     * 获取最新预设（按创建时间排序）
     */
    suspend fun getLatestPresets(limit: Int = 10): List<Preset> = withContext(Dispatchers.IO) {
        val allPresets = loadAllPresets()
        allPresets.sortedByDescending { it.createdAt }.take(limit)
    }
}
