package com.omaster.app.data

import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.model.Section
import com.omaster.app.network.PresetApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresetRepository @Inject constructor(
    private val presetApi: PresetApi
) {
    private var cachedPresets: List<Preset> = emptyList()
    
    fun getPresets(): Flow<Result<List<Preset>>> = flow {
        try {
            val response = presetApi.getAllPresets()
            if (response.isSuccessful) {
                val presets = response.body() ?: emptyList()
                cachedPresets = presets
                emit(Result.success(presets))
            } else {
                Timber.e("Failed to fetch presets: ${response.code()}")
                emit(Result.success(getSamplePresets()))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching presets")
            emit(Result.success(getSamplePresets()))
        }
    }
    
    fun getOppoPresets(): Flow<Result<List<Preset>>> = flow {
        try {
            val response = presetApi.getOppoPresets()
            if (response.isSuccessful) {
                val presets = response.body() ?: emptyList()
                emit(Result.success(presets))
            } else {
                Timber.e("Failed to fetch OPPO presets: ${response.code()}")
                emit(Result.success(getSamplePresets().filter { it.deviceModel?.contains("OPPO", ignoreCase = true) == true }))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching OPPO presets")
            emit(Result.success(getSamplePresets().filter { it.deviceModel?.contains("OPPO", ignoreCase = true) == true }))
        }
    }
    
    fun getRealmePresets(): Flow<Result<List<Preset>>> = flow {
        try {
            val response = presetApi.getRealmePresets()
            if (response.isSuccessful) {
                val presets = response.body() ?: emptyList()
                emit(Result.success(presets))
            } else {
                Timber.e("Failed to fetch realme presets: ${response.code()}")
                emit(Result.success(getSamplePresets().filter { it.deviceModel?.contains("realme", ignoreCase = true) == true }))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching realme presets")
            emit(Result.success(getSamplePresets().filter { it.deviceModel?.contains("realme", ignoreCase = true) == true }))
        }
    }
    
    private fun getSamplePresets(): List<Preset> {
        return listOf(
            Preset(
                id = "oppo_001",
                name = "哈苏人像经典",
                deviceModel = "OPPO Find X7 Pro",
                coverPath = "hasselblad_portrait",
                source = "official",
                cameraParams = CameraParams(
                    mode = "专业",
                    iso = 100,
                    shutter = "1/200s",
                    ev = "+0.3",
                    wb = "5500K",
                    filter = "哈苏 HNCS"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合室内外人像拍摄，呈现自然肤色和柔和背景虚化效果。"
                    )
                )
            ),
            Preset(
                id = "oppo_002",
                name = "自然风光",
                deviceModel = "OPPO Find X7 Pro",
                coverPath = "natural_landscape",
                source = "official",
                cameraParams = CameraParams(
                    mode = "专业",
                    iso = 64,
                    shutter = "1/500s",
                    ev = "+0.7",
                    wb = "6500K",
                    filter = "自然"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合风景、建筑摄影，展现真实色彩和细腻细节。"
                    )
                )
            ),
            Preset(
                id = "oppo_003",
                name = "城市夜景",
                deviceModel = "OPPO Find X7 Pro",
                coverPath = "night_city",
                source = "official",
                cameraParams = CameraParams(
                    mode = "专业",
                    iso = 3200,
                    shutter = "1/30s",
                    ev = "+0.7",
                    wb = "4000K",
                    filter = "夜景"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合夜间城市风光摄影，捕捉光影流动和建筑轮廓。"
                    )
                )
            ),
            Preset(
                id = "oppo_004",
                name = "美食摄影",
                deviceModel = "OPPO Find X7 Pro",
                coverPath = "food_photo",
                source = "official",
                cameraParams = CameraParams(
                    mode = "专业",
                    iso = 200,
                    shutter = "1/125s",
                    ev = "+0.3",
                    wb = "5000K",
                    filter = "美食"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合美食摄影，提升色彩饱和度和食欲感。"
                    )
                )
            ),
            Preset(
                id = "oppo_005",
                name = "逆光人像",
                deviceModel = "OPPO Find X7 Pro",
                coverPath = "backlight_portrait",
                source = "official",
                cameraParams = CameraParams(
                    mode = "专业",
                    iso = 100,
                    shutter = "1/400s",
                    ev = "+1.0",
                    wb = "5500K",
                    filter = "人像"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合逆光人像拍摄，保持主体亮度同时保留背景细节。"
                    )
                )
            ),
            Preset(
                id = "realme_001",
                name = "街拍利器",
                deviceModel = "realme GT5 Pro",
                coverPath = "street_photo",
                source = "official",
                cameraParams = CameraParams(
                    mode = "专业",
                    iso = 400,
                    shutter = "1/250s",
                    ev = "+0.3",
                    wb = "5500K",
                    filter = "街拍"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合街头摄影，快速捕捉瞬间画面。"
                    )
                )
            ),
            Preset(
                id = "realme_002",
                name = "微距世界",
                deviceModel = "realme GT5 Pro",
                coverPath = "macro_photo",
                source = "official",
                cameraParams = CameraParams(
                    mode = "专业",
                    iso = 100,
                    shutter = "1/160s",
                    ev = "+0.0",
                    wb = "5200K",
                    filter = "微距"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合微距摄影，展现细节之美。"
                    )
                )
            ),
            Preset(
                id = "realme_003",
                name = "日出日落",
                deviceModel = "realme GT5 Pro",
                coverPath = "sunset_sunrise",
                source = "official",
                cameraParams = CameraParams(
                    mode = "专业",
                    iso = 64,
                    shutter = "1/1000s",
                    ev = "+0.7",
                    wb = "6000K",
                    filter = "暖色"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合日出日落拍摄，记录金色时刻。"
                    )
                )
            ),
            Preset(
                id = "realme_004",
                name = "黑白肖像",
                deviceModel = "realme GT5 Pro",
                coverPath = "bw_portrait",
                source = "community",
                cameraParams = CameraParams(
                    mode = "专业",
                    iso = 200,
                    shutter = "1/160s",
                    ev = "+0.3",
                    wb = "5500K",
                    filter = "黑白"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合人像摄影，呈现经典黑白质感。"
                    )
                )
            ),
            Preset(
                id = "realme_005",
                name = "海岛度假",
                deviceModel = "realme GT5 Pro",
                coverPath = "beach_photo",
                source = "community",
                cameraParams = CameraParams(
                    mode = "专业",
                    iso = 50,
                    shutter = "1/800s",
                    ev = "+0.3",
                    wb = "6500K",
                    filter = "清新"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合海边度假摄影，呈现蓝天白云和清澈海水。"
                    )
                )
            )
        )
    }
}
