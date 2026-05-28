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
                coverPath = "findx7_portrait_hasselblad",
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
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OPPO Find X7 Pro 拍摄，展现哈苏 HNCS 色彩还原能力。"
                    )
                )
            ),
            Preset(
                id = "oppo_002",
                name = "自然风光",
                deviceModel = "OPPO Find X7 Pro",
                coverPath = "findx7_landscape_natural",
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
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OPPO Find X7 Pro 超广角镜头拍摄自然风光。"
                    )
                )
            ),
            Preset(
                id = "oppo_003",
                name = "城市夜景",
                deviceModel = "OPPO Find X7 Ultra",
                coverPath = "findx7ultra_night_city",
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
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OPPO Find X7 Ultra 夜景模式拍摄，支持哈苏专业调校。"
                    )
                )
            ),
            Preset(
                id = "oppo_004",
                name = "美食摄影",
                deviceModel = "OPPO Reno 10 Pro+",
                coverPath = "reno10_food_photo",
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
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OPPO Reno 10 Pro+ 人像镜头拍摄美食特写。"
                    )
                )
            ),
            Preset(
                id = "oppo_005",
                name = "逆光人像",
                deviceModel = "OPPO Find X6 Pro",
                coverPath = "findx6_backlight_portrait",
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
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OPPO Find X6 Pro 逆光算法，展现专业人像效果。"
                    )
                )
            ),
            Preset(
                id = "oneplus_001",
                name = "哈苏街头模式",
                deviceModel = "OnePlus 12",
                coverPath = "oneplus12_street_photography",
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
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OnePlus 12 哈苏色彩调校，街拍更有质感。"
                    )
                )
            ),
            Preset(
                id = "oneplus_002",
                name = "哈苏微距",
                deviceModel = "OnePlus 12",
                coverPath = "oneplus12_macro_closeup",
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
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OnePlus 12 微距镜头，捕捉微观世界。"
                    )
                )
            ),
            Preset(
                id = "oneplus_003",
                name = "金色时刻",
                deviceModel = "OnePlus 11",
                coverPath = "oneplus11_golden_hour",
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
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OnePlus 11 哈苏镜头拍摄日出日落美景。"
                    )
                )
            ),
            Preset(
                id = "realme_001",
                name = "街拍达人",
                deviceModel = "realme GT5 Pro",
                coverPath = "realmegt5_street_style",
                source = "official",
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
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 realme GT5 Pro 拍摄，黑白风格更具艺术感。"
                    )
                )
            ),
            Preset(
                id = "realme_002",
                name = "海岛风情",
                deviceModel = "realme GT5 Pro",
                coverPath = "realmegt5_beach_vacation",
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
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 realme GT5 Pro 拍摄海岛风光，色彩鲜艳通透。"
                    )
                )
            ),
            Preset(
                id = "oppo_006",
                name = "哈苏经典蓝",
                deviceModel = "OPPO Find X7 Pro",
                coverPath = "findx7_classic_blue",
                source = "community",
                cameraParams = CameraParams(
                    mode = "专业",
                    iso = 100,
                    shutter = "1/320s",
                    ev = "+0.0",
                    wb = "7000K",
                    filter = "冷色调"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合冷色调风格摄影，营造静谧氛围。"
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OPPO Find X7 Pro 哈苏色彩，呈现经典蓝色调。"
                    )
                )
            ),
            Preset(
                id = "oppo_007",
                name = "复古胶片",
                deviceModel = "OPPO Reno 11",
                coverPath = "reno11_vintage_film",
                source = "community",
                cameraParams = CameraParams(
                    mode = "专业",
                    iso = 400,
                    shutter = "1/125s",
                    ev = "+0.3",
                    wb = "4500K",
                    filter = "复古"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合复古风格摄影，重现胶片质感。"
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OPPO Reno 11 拍摄，复古滤镜增添怀旧感。"
                    )
                )
            )
        )
    }
}
