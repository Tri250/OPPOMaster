package com.omaster.app.data

import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.model.Section
import com.omaster.app.network.PresetApi
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
    private val presetApi: PresetApi,
    private val preferencesDataStore: PreferencesDataStore
) {
    private val _presets = MutableStateFlow<List<Preset>>(emptyList())
    val presets: StateFlow<List<Preset>> = _presets.asStateFlow()
    
    private var cachedPresets: List<Preset> = emptyList()
    private var favoriteIds: Set<String> = emptySet()
    
    init {
        // 监听收藏状态变化
        kotlinx.coroutines.GlobalScope.launch {
            preferencesDataStore.favoritePresets.collect { favorites ->
                favoriteIds = favorites
                if (cachedPresets.isNotEmpty()) {
                    _presets.value = applyFavoriteStates(cachedPresets)
                }
            }
        }
    }
    
    suspend fun loadPresets() {
        try {
            val response = presetApi.getAllPresets()
            if (response.isSuccessful) {
                val presets = response.body() ?: getSamplePresets()
                cachedPresets = presets
                _presets.value = applyFavoriteStates(presets)
            } else {
                Timber.e("Failed to fetch presets: ${response.code()}")
                cachedPresets = getSamplePresets()
                _presets.value = applyFavoriteStates(cachedPresets)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching presets")
            cachedPresets = getSamplePresets()
            _presets.value = applyFavoriteStates(cachedPresets)
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
    
    private fun getSamplePresets(): List<Preset> {
        return listOf(
            // OPPO Find X8 Pro 预设
            Preset(
                id = "oppo_001",
                name = "哈苏人像经典",
                deviceModel = "OPPO Find X8 Pro",
                coverPath = "findx8_pro_portrait_hasselblad",
                source = "official",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 100,
                    shutter = "1/200",
                    ev = "+0.3",
                    wb = "5500K",
                    focal_length = "50mm",
                    aperture = "f/1.6",
                    portrait_mode = true,
                    ai_optimization = true,
                    hasselblad_hncs = true,
                    hasselblad_natural_color = true,
                    hasselblad_master_style = "Portrait Pro",
                    color_profile = "Natural",
                    filter = "哈苏自然色彩"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合室内外人像拍摄，呈现自然肤色和柔和背景虚化效果"
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OPPO Find X8 Pro 哈苏人像镜头拍摄，1英寸大底传感器加持"
                    )
                )
            ),
            Preset(
                id = "oppo_002",
                name = "自然风光大师",
                deviceModel = "OPPO Find X8 Pro",
                coverPath = "findx8_pro_landscape",
                source = "official",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 64,
                    shutter = "1/500",
                    ev = "+0.7",
                    wb = "6500K",
                    focal_length = "23mm",
                    aperture = "f/2.8",
                    hdr = true,
                    ai_optimization = true,
                    hasselblad_hncs = true,
                    hasselblad_natural_color = true,
                    hasselblad_master_style = "Landscape",
                    color_profile = "Vivid",
                    filter = "自然风景"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合风景、建筑摄影，展现真实色彩和细腻细节"
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OPPO Find X8 Pro 1英寸大底广角镜头拍摄"
                    )
                )
            ),
            Preset(
                id = "oppo_003",
                name = "城市夜景之王",
                deviceModel = "OPPO Find X8 Ultra",
                coverPath = "findx8_ultra_night_city",
                source = "official",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 3200,
                    shutter = "1/30",
                    ev = "+0.7",
                    wb = "4000K",
                    focal_length = "23mm",
                    aperture = "f/1.8",
                    night_mode = true,
                    hdr = true,
                    ai_optimization = true,
                    hasselblad_hncs = true,
                    hasselblad_natural_color = true,
                    hasselblad_master_style = "Night Pro",
                    color_profile = "Cinematic",
                    filter = "夜景增强"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合夜间城市风光摄影，捕捉光影流动和建筑轮廓"
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OPPO Find X8 Ultra 1英寸双大底夜景模式拍摄"
                    )
                )
            ),
            Preset(
                id = "food_special",
                name = "美食诱人",
                deviceModel = "OPPO Find X8 Pro",
                coverPath = "food_delicious",
                source = "community",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 200,
                    shutter = "1/125",
                    ev = "+0.5",
                    wb = "5200K",
                    focal_length = "35mm",
                    aperture = "f/1.9",
                    ai_optimization = true,
                    hasselblad_natural_color = true,
                    color_profile = "Food",
                    filter = "美食模式",
                    saturation = 70,
                    contrast = 55
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "专为美食摄影设计，提升色彩饱和度和食欲感"
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OPPO Find X8 Pro 拍摄，色彩鲜艳诱人"
                    )
                )
            ),
            Preset(
                id = "oppo_004",
                name = "美食摄影专家",
                deviceModel = "OPPO Reno 12 Pro+",
                coverPath = "reno12_pro_food",
                source = "official",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 200,
                    shutter = "1/125",
                    ev = "+0.3",
                    wb = "5000K",
                    focal_length = "50mm",
                    aperture = "f/1.8",
                    ai_optimization = true,
                    hasselblad_natural_color = true,
                    color_profile = "Food",
                    filter = "美食模式"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合美食摄影，提升色彩饱和度和食欲感"
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OPPO Reno 12 Pro+ 超光影镜头拍摄"
                    )
                )
            ),
            Preset(
                id = "oppo_005",
                name = "逆光人像大师",
                deviceModel = "OPPO Find X7 Ultra",
                coverPath = "findx7_ultra_backlight",
                source = "official",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 100,
                    shutter = "1/400",
                    ev = "+1.0",
                    wb = "5500K",
                    focal_length = "32mm",
                    aperture = "f/1.8",
                    portrait_mode = true,
                    hdr = true,
                    ai_optimization = true,
                    hasselblad_hncs = true,
                    hasselblad_natural_color = true,
                    color_profile = "Portrait",
                    filter = "人像逆光"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合逆光人像拍摄，保持主体亮度同时保留背景细节"
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OPPO Find X7 Ultra 哈苏超光影引擎拍摄"
                    )
                )
            ),
            // OnePlus 预设
            Preset(
                id = "oneplus_001",
                name = "哈苏街头模式",
                deviceModel = "OnePlus 13 Pro",
                coverPath = "oneplus13_pro_street",
                source = "official",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 400,
                    shutter = "1/250",
                    ev = "+0.3",
                    wb = "5500K",
                    focal_length = "35mm",
                    aperture = "f/1.9",
                    ai_optimization = true,
                    hasselblad_natural_color = true,
                    hasselblad_master_style = "Street",
                    color_profile = "Classic",
                    filter = "街头色彩"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合街头摄影，快速捕捉瞬间画面"
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OnePlus 13 Pro 哈苏色彩调校"
                    )
                )
            ),
            Preset(
                id = "oneplus_002",
                name = "哈苏微距世界",
                deviceModel = "OnePlus 13 Pro",
                coverPath = "oneplus13_pro_macro",
                source = "official",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 100,
                    shutter = "1/160",
                    ev = "+0.0",
                    wb = "5200K",
                    focal_length = "Macro",
                    aperture = "f/2.0",
                    ai_optimization = true,
                    hasselblad_natural_color = true,
                    color_profile = "Vivid",
                    filter = "微距模式"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合微距摄影，展现细节之美"
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OnePlus 13 Pro 微距镜头，捕捉微观世界"
                    )
                )
            ),
            Preset(
                id = "oneplus_003",
                name = "金色时刻",
                deviceModel = "OnePlus 12",
                coverPath = "oneplus12_sunrise",
                source = "official",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 64,
                    shutter = "1/1000",
                    ev = "+0.7",
                    wb = "6000K",
                    focal_length = "23mm",
                    aperture = "f/2.6",
                    ai_optimization = true,
                    hasselblad_natural_color = true,
                    color_profile = "Warm",
                    filter = "暖调色彩"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合日出日落拍摄，记录金色时刻"
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OnePlus 12 哈苏镜头拍摄日出日落美景"
                    )
                )
            ),
            // realme 预设
            Preset(
                id = "realme_001",
                name = "街拍达人",
                deviceModel = "realme GT7 Pro",
                coverPath = "realmegt7_pro_street",
                source = "official",
                cameraParams = CameraParams(
                    mode = "大师模式",
                    iso = 200,
                    shutter = "1/160",
                    ev = "+0.3",
                    wb = "5500K",
                    focal_length = "28mm",
                    aperture = "f/1.9",
                    ai_optimization = true,
                    color_profile = "Monochrome",
                    filter = "黑白艺术"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合人像摄影，呈现经典黑白质感"
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 realme GT7 Pro 拍摄，黑白风格更具艺术感"
                    )
                )
            ),
            Preset(
                id = "realme_002",
                name = "海岛风情",
                deviceModel = "realme GT7 Pro",
                coverPath = "realmegt7_pro_beach",
                source = "community",
                cameraParams = CameraParams(
                    mode = "大师模式",
                    iso = 50,
                    shutter = "1/800",
                    ev = "+0.3",
                    wb = "6500K",
                    focal_length = "16mm",
                    aperture = "f/2.2",
                    ai_optimization = true,
                    color_profile = "Vivid",
                    filter = "清新色彩"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合海边度假摄影，呈现蓝天白云和清澈海水"
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 realme GT7 Pro 拍摄海岛风光，色彩鲜艳通透"
                    )
                )
            ),
            Preset(
                id = "oppo_006",
                name = "哈苏经典蓝调",
                deviceModel = "OPPO Find X8 Pro",
                coverPath = "findx8_pro_classic_blue",
                source = "community",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 100,
                    shutter = "1/320",
                    ev = "+0.0",
                    wb = "7000K",
                    focal_length = "48mm",
                    aperture = "f/2.0",
                    ai_optimization = true,
                    hasselblad_hncs = true,
                    hasselblad_natural_color = true,
                    color_profile = "Cool",
                    filter = "冷调风格"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合冷色调风格摄影，营造静谧氛围"
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OPPO Find X8 Pro 哈苏色彩，呈现经典蓝色调"
                    )
                )
            ),
            Preset(
                id = "oppo_007",
                name = "复古胶片",
                deviceModel = "OPPO Reno 12",
                coverPath = "reno12_vintage",
                source = "community",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 400,
                    shutter = "1/125",
                    ev = "+0.3",
                    wb = "4500K",
                    focal_length = "24mm",
                    aperture = "f/1.7",
                    ai_optimization = false,
                    hasselblad_natural_color = true,
                    color_profile = "Classic Film",
                    filter = "复古胶片"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "适合复古风格摄影，重现胶片质感"
                    ),
                    Section(
                        title = "样张说明",
                        content = "使用 OPPO Reno 12 拍摄，复古滤镜增添怀旧感"
                    )
                )
            )
        )
    }
}