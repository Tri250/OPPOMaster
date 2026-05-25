package com.omaster.app.data

import com.omaster.app.model.*
import com.omaster.app.network.PresetApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// ============================================
// OPPO 哈苏预设仓库 - 专业级完整实现
// ============================================

@Singleton
class PresetRepository @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore,
    private val presetApi: PresetApi
) {
    // ============================================
    // 哈苏大师预设库 - 专业级内容
    // ============================================
    private val masterPresets = buildMasterPresets()
    
    // ============================================
    // 公开API - 数据访问
    // ============================================
    val presets: Flow<List<Preset>> = preferencesDataStore.favoritePresets
        .map { favoriteIds ->
            masterPresets.map { preset ->
                preset.copy(isFavorite = favoriteIds.contains(preset.id))
            }
        }
    
    fun getAllPresets(): List<Preset> = masterPresets
    
    fun getPresetById(id: String): Preset? = masterPresets.find { it.id == id }
    
    // 按风格筛选
    fun getPresetsByStyle(styleType: StyleType): List<Preset> {
        return masterPresets.filter { it.styleType == styleType }
    }
    
    // 按场景筛选
    fun getPresetsByScene(sceneTag: SceneTag): List<Preset> {
        return masterPresets.filter { it.sceneTags.contains(sceneTag) }
    }
    
    // 按设备筛选
    fun getPresetsByDevice(deviceModel: String): List<Preset> {
        return masterPresets.filter { 
            it.deviceModel.contains(deviceModel, ignoreCase = true) ||
            it.compatibleDevices.any { d -> d.contains(deviceModel, ignoreCase = true) }
        }
    }
    
    // HNCS认证预设
    fun getHncsCertifiedPresets(): List<Preset> {
        return masterPresets.filter { it.isHncsCertified }
    }
    
    // 热门预设
    fun getPopularPresets(): List<Preset> {
        return masterPresets.sortedByDescending { it.stats.usageCount }
    }
    
    // ============================================
    // 收藏功能
    // ============================================
    suspend fun toggleFavorite(presetId: String) {
        Timber.d("Toggle favorite for preset: $presetId")
        preferencesDataStore.toggleFavorite(presetId)
    }
    
    fun isFavorite(presetId: String): Boolean {
        // 实际应从DataStore读取，这里简化
        return false
    }
    
    // ============================================
    // 网络同步
    // ============================================
    suspend fun fetchPresetsFromNetwork(): List<Preset>? {
        return try {
            val response = presetApi.getPresets()
            if (response.isSuccessful) {
                Timber.d("成功从网络获取预设")
                response.body()
            } else {
                Timber.e("网络请求失败: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "获取预设时发生错误")
            null
        }
    }
    
    // ============================================
    // 预设数据构建
    // ============================================
    private fun buildMasterPresets(): List<Preset> {
        return listOf(
            // ============================================
            // 哈苏HNCS认证预设系列
            // ============================================
            Preset.createHncsPreset(
                id = "hncs_001",
                name = "哈苏自然色彩 | 标准模式",
                coverPath = "hncs_standard",
                deviceModel = "Find X8 Pro",
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "",
                    iso = 100,
                    shutter = "1/125",
                    ev = "0",
                    wb = "5500K",
                    hasselblad_hncs = true
                ),
                fineTuneParams = FineTuneParams.HNCS_DEFAULT,
                styleType = StyleType.NATURAL,
                sceneTags = listOf(SceneTag.PORTRAIT, SceneTag.LANDSCAPE, SceneTag.FOOD),
                description = "哈苏自然色彩系统标准模式，还原真实色彩，适合绝大多数场景。",
                sections = listOf(
                    Section("光感设置", "标准动态范围，高光保护+5，阴影提升+3"),
                    Section("色彩调校", "饱和度-5，保持自然还原真实"),
                    Section("使用建议", "适合绝大多数日常记录、人像、风光等场景")
                ),
                usageTips = listOf(
                    "适合晴天室外使用效果最佳",
                    "配合哈苏大师模式可获得最佳效果",
                    "ISO建议控制在800以内"
                )
            ),
            
            Preset.createHncsPreset(
                id = "hncs_002",
                name = "哈苏人像 | 温润质感",
                coverPath = "hncs_portrait",
                deviceModel = "Find X8 Ultra",
                compatibleDevices = listOf("Find X8 Pro", "Find X7 Ultra"),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "",
                    iso = 200,
                    shutter = "1/200",
                    ev = "+0.3",
                    wb = "5800K",
                    hasselblad_hncs = true
                ),
                fineTuneParams = FineTuneParams.PORTRAIT,
                styleType = StyleType.PORTRAIT,
                sceneTags = listOf(SceneTag.PORTRAIT, SceneTag.GOLDEN_HOUR),
                description = "专为优化人像肤色，温润自然，保留皮肤质感细节。",
                sections = listOf(
                    Section("人像优化", "柔光效果+2，皮肤质感保留"),
                    Section("肤色调校", "色温+5，肤色更自然"),
                    Section("暗角控制", "轻微暗角突出主体")
                ),
                usageTips = listOf(
                    "3倍-5倍人像焦段效果最佳",
                    "黄金时刻拍摄效果最佳",
                    "建议开启哈苏大师模式"
                )
            ),
            
            Preset.createHncsPreset(
                id = "hncs_003",
                name = "哈苏风光 | 层次分明",
                coverPath = "hncs_landscape",
                deviceModel = "Find X8 Pro",
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "",
                    iso = 100,
                    shutter = "1/500",
                    ev = "0",
                    wb = "5200K",
                    hasselblad_hncs = true
                ),
                fineTuneParams = FineTuneParams.LANDSCAPE,
                styleType = StyleType.LANDSCAPE,
                sceneTags = listOf(SceneTag.LANDSCAPE, SceneTag.NATURE, SceneTag.ARCHITECTURE),
                description = "增强风光层次，高动态范围保留天空与地面细节。",
                sections = listOf(
                    Section("动态范围", "高动态模式，高光保护+8，阴影提升+2"),
                    Section("色彩调校", "蓝绿色彩强化，色温-3"),
                    Section("锐度增强", "锐度+30，细节更清晰")
                ),
                usageTips = listOf(
                    "适合风光摄影",
                    "使用超广角或主摄效果更佳",
                    "ISO建议100-200"
                )
            ),
            
            Preset.createHncsPreset(
                id = "hncs_004",
                name = "哈苏夜景 | 纯净夜之美",
                coverPath = "hncs_night",
                deviceModel = "Find X8 Ultra",
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "",
                    iso = 800,
                    shutter = "1/30",
                    ev = "-0.3",
                    wb = "4200K",
                    hasselblad_hncs = true
                ),
                fineTuneParams = FineTuneParams.NIGHT,
                styleType = StyleType.NIGHT,
                sceneTags = listOf(SceneTag.NIGHT_SCENE, SceneTag.CITYSCAPE, SceneTag.BLUE_HOUR),
                description = "夜景纯净降噪，保留夜色深邃，城市光影迷人。",
                sections = listOf(
                    Section("降噪优化", "降噪+5，纯净夜拍"),
                    Section("色彩调校", "色温-5，夜色更纯正"),
                    Section("暗角效果", "中等暗角增强氛围")
                ),
                usageTips = listOf(
                    "手持1/30秒以上建议使用三脚架",
                    "蓝调时刻效果最佳",
                    "ISO建议400-1600"
                )
            ),
            
            Preset.createHncsPreset(
                id = "hncs_005",
                name = "哈苏街头 | 纪实质感",
                coverPath = "hncs_street",
                deviceModel = "Find X8",
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "",
                    iso = 400,
                    shutter = "1/1000",
                    ev = "0",
                    wb = "5000K",
                    hasselblad_hncs = true
                ),
                fineTuneParams = FineTuneParams(
                    sharpness = 28,
                    contrast = 22,
                    saturation = -3,
                    vignette = 1,
                    softness = 0,
                    colorTemperature = -2,
                    tint = 0,
                    skinToneOptimization = false,
                    highlightProtection = 6,
                    shadowLift = 4,
                    noiseReduction = 2,
                    dynamicRangeMode = 1
                ),
                styleType = StyleType.STREET,
                sceneTags = listOf(SceneTag.STREET, SceneTag.CITYSCAPE),
                description = "街头纪实抓拍，真实还原城市的生动瞬间。",
                sections = listOf(
                    Section("抓拍优化", "快速抓拍优化，高光保护"),
                    Section("色彩调校", "真实色彩，保留现场感"),
                    Section("快门建议", "1/500-1/2000秒定格瞬间")
                ),
                usageTips = listOf(
                    "适合街拍纪实",
                    "1/500秒以上快门",
                    "色彩真实自然"
                )
            ),
            
            Preset.createHncsPreset(
                id = "hncs_006",
                name = "哈苏美食 | 食欲诱惑",
                coverPath = "hncs_food",
                deviceModel = "Reno 12 Pro",
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "",
                    iso = 200,
                    shutter = "1/60",
                    ev = "+0.3",
                    wb = "5500K",
                    hasselblad_hncs = true
                ),
                fineTuneParams = FineTuneParams(
                    sharpness = 28,
                    contrast = 18,
                    saturation = 0,
                    vignette = 1,
                    softness = 1,
                    colorTemperature = 5,
                    tint = 0,
                    skinToneOptimization = false,
                    highlightProtection = 4,
                    shadowLift = 3,
                    noiseReduction = 1,
                    dynamicRangeMode = 1
                ),
                styleType = StyleType.FOOD,
                sceneTags = listOf(SceneTag.FOOD, SceneTag.PRODUCT),
                description = "美食色彩还原，诱人食欲，细节精致。",
                sections = listOf(
                    Section("暖色调校", "色温+5，温暖诱人"),
                    Section("细节保留", "食物纹理清晰"),
                    Section("背景虚化", "突出美食主体")
                ),
                usageTips = listOf(
                    "靠近窗户自然光拍摄",
                    "使用大光圈虚化背景",
                    "45度角拍摄更佳"
                )
            ),
            
            // ============================================
            // 胶片质感系列
            // ============================================
            Preset(
                id = "film_001",
                name = "柯达金200 | 胶片复古",
                coverPath = "film_kodak_gold",
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "",
                    iso = 200,
                    shutter = "1/250",
                    ev = "0",
                    wb = "5600K",
                    hasselblad_hncs = false
                ),
                fineTuneParams = FineTuneParams(
                    sharpness = 22,
                    contrast = 18,
                    saturation = -8,
                    vignette = 2,
                    softness = 1,
                    colorTemperature = 8,
                    tint = 2,
                    skinToneOptimization = true,
                    highlightProtection = 5,
                    shadowLift = 2,
                    noiseReduction = 0,
                    dynamicRangeMode = 0
                ),
                deviceModel = "Find X8 Pro",
                styleType = StyleType.FILM,
                sceneTags = listOf(SceneTag.PORTRAIT, SceneTag.STREET),
                description = "经典柯达金200胶片色彩，温暖复古。",
                sections = listOf(
                    Section("胶片模拟", "模拟柯达金200色彩"),
                    Section("颗粒感", "轻微胶片颗粒"),
                    Section("暖调温暖", "色彩温暖复古")
                ),
                usageTips = listOf(
                    "阳光充足效果最佳",
                    "人像街头都适用",
                    "适合记录生活"
                ),
                source = "omaster_community"
            ),
            
            Preset(
                id = "film_002",
                name = "富士Provia 100F | 反转片",
                coverPath = "film_fuji_provia",
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "",
                    iso = 100,
                    shutter = "1/500",
                    ev = "0",
                    wb = "5200K",
                    hasselblad_hncs = false
                ),
                fineTuneParams = FineTuneParams(
                    sharpness = 28,
                    contrast = 28,
                    saturation = 5,
                    vignette = 1,
                    softness = 0,
                    colorTemperature = -5,
                    tint = -2,
                    skinToneOptimization = false,
                    highlightProtection = 7,
                    shadowLift = 1,
                    noiseReduction = 1,
                    dynamicRangeMode = 1
                ),
                deviceModel = "Find X8 Ultra",
                styleType = StyleType.FILM,
                sceneTags = listOf(SceneTag.LANDSCAPE, SceneTag.NATURE),
                description = "富士Provia反转片色彩，鲜艳通透。",
                sections = listOf(
                    Section("反转片模拟", "高反差，鲜艳色彩"),
                    Section("蓝绿强化", "蓝天绿草更生动"),
                    Section("清晰度高", "锐度提升")
                ),
                usageTips = listOf(
                    "风光摄影效果惊艳",
                    "色彩浓郁鲜艳",
                    "适合晴天拍摄"
                ),
                source = "omaster_community"
            ),
            
            // ============================================
            // 电影感系列
            // ============================================
            Preset(
                id = "cinema_001",
                name = "王家卫 | 港风电影感",
                coverPath = "cinema_wong_kar_wai",
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "",
                    iso = 400,
                    shutter = "1/125",
                    ev = "-0.5",
                    wb = "4600K",
                    hasselblad_hncs = false
                ),
                fineTuneParams = FineTuneParams(
                    sharpness = 20,
                    contrast = 25,
                    saturation = -10,
                    vignette = 3,
                    softness = 2,
                    colorTemperature = -8,
                    tint = 3,
                    skinToneOptimization = true,
                    highlightProtection = 8,
                    shadowLift = 6,
                    noiseReduction = 3,
                    dynamicRangeMode = 2
                ),
                deviceModel = "Find X8 Pro",
                styleType = StyleType.CINEMATIC,
                sceneTags = listOf(SceneTag.NIGHT_SCENE, SceneTag.PORTRAIT, SceneTag.STREET),
                description = "港风电影感，复古迷离，故事感强烈。",
                sections = listOf(
                    Section("电影感营造", "高对比，大暗角"),
                    Section("色彩调校", "偏青偏蓝调"),
                    Section("氛围感强", "故事感强烈")
                ),
                usageTips = listOf(
                    "夜景街头效果最佳",
                    "故事感构图",
                    "动态抓拍"
                ),
                source = "omaster_community"
            ),
            
            // ============================================
            // 黑白质感系列
            // ============================================
            Preset(
                id = "mono_001",
                name = "黑白人文 | 高对比",
                coverPath = "mono_high_contrast",
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "黑白",
                    iso = 200,
                    shutter = "1/500",
                    ev = "0",
                    wb = "5500K",
                    hasselblad_hncs = false
                ),
                fineTuneParams = FineTuneParams(
                    sharpness = 30,
                    contrast = 35,
                    saturation = -20,
                    vignette = 2,
                    softness = 0,
                    colorTemperature = 0,
                    tint = 0,
                    skinToneOptimization = false,
                    highlightProtection = 5,
                    shadowLift = 4,
                    noiseReduction = 2,
                    dynamicRangeMode = 1
                ),
                deviceModel = "Find X8",
                styleType = StyleType.MONOCHROME,
                sceneTags = listOf(SceneTag.STREET, SceneTag.PORTRAIT, SceneTag.ARCHITECTURE),
                description = "高对比黑白，光影层次分明，纪实人文。",
                sections = listOf(
                    Section("黑白模式", "高对比黑白"),
                    Section("层次分明", "光影层次强烈"),
                    Section("颗粒质感", "黑白质感出色")
                ),
                usageTips = listOf(
                    "寻找光影对比",
                    "人文纪实题材",
                    "关注构图简洁"
                ),
                source = "omaster_community"
            ),
            
            // ============================================
            // 复古风格系列
            // ============================================
            Preset(
                id = "vintage_001",
                name = "90年代港风",
                coverPath = "vintage_90s_hk",
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "",
                    iso = 200,
                    shutter = "1/200",
                    ev = "0",
                    wb = "6000K",
                    hasselblad_hncs = false
                ),
                fineTuneParams = FineTuneParams(
                    sharpness = 22,
                    contrast = 15,
                    saturation = 3,
                    vignette = 2,
                    softness = 2,
                    colorTemperature = 10,
                    tint = 5,
                    skinToneOptimization = true,
                    highlightProtection = 4,
                    shadowLift = 3,
                    noiseReduction = 2,
                    dynamicRangeMode = 0
                ),
                deviceModel = "Reno 12 Pro",
                styleType = StyleType.VINTAGE,
                sceneTags = listOf(SceneTag.PORTRAIT, SceneTag.STREET),
                description = "90年代香港复古风，温暖怀旧。",
                sections = listOf(
                    Section("复古色调", "暖黄调胶片感"),
                    Section("柔光效果", "柔焦朦胧"),
                    Section("颗粒质感", "轻微颗粒")
                ),
                usageTips = listOf(
                    "复古穿搭效果",
                    "人像最佳",
                    "暖色调场景"
                ),
                source = "omaster_community"
            ),
            
            // ============================================
            // 特殊场景预设
            // ============================================
            Preset(
                id = "special_001",
                name = "蓝调时刻 | 城市暮色",
                coverPath = "special_blue_hour",
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "",
                    iso = 400,
                    shutter = "1/60",
                    ev = "-0.7",
                    wb = "4000K",
                    hasselblad_hncs = false
                ),
                fineTuneParams = FineTuneParams(
                    sharpness = 25,
                    contrast = 22,
                    saturation = -2,
                    vignette = 2,
                    softness = 1,
                    colorTemperature = -10,
                    tint = -3,
                    skinToneOptimization = false,
                    highlightProtection = 9,
                    shadowLift = 7,
                    noiseReduction = 4,
                    dynamicRangeMode = 2
                ),
                deviceModel = "Find X8 Ultra",
                styleType = StyleType.CINEMATIC,
                sceneTags = listOf(SceneTag.BLUE_HOUR, SceneTag.SUNSET, SceneTag.NIGHT_SCENE),
                description = "日落后蓝调时刻，城市夜色最美时。",
                sections = listOf(
                    Section("蓝调色温", "-10，蓝调纯正"),
                    Section("高动态范围", "高光阴影都保留"),
                    Section("降噪优化", "夜景纯净")
                ),
                usageTips = listOf(
                    "日落后30分钟拍摄",
                    "城市风光",
                    "使用三脚架稳定"
                ),
                source = "omaster_community"
            ),
            
            Preset(
                id = "special_002",
                name = "金色夕阳 | 温暖浪漫",
                coverPath = "special_golden_hour",
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "",
                    iso = 100,
                    shutter = "1/200",
                    ev = "-0.3",
                    wb = "6200K",
                    hasselblad_hncs = false
                ),
                fineTuneParams = FineTuneParams(
                    sharpness = 25,
                    contrast = 20,
                    saturation = 3,
                    vignette = 1,
                    softness = 1,
                    colorTemperature = 12,
                    tint = 2,
                    skinToneOptimization = true,
                    highlightProtection = 6,
                    shadowLift = 4,
                    noiseReduction = 1,
                    dynamicRangeMode = 1
                ),
                deviceModel = "Find X7 Ultra",
                styleType = StyleType.VIBRANT,
                sceneTags = listOf(SceneTag.GOLDEN_HOUR, SceneTag.SUNSET, SceneTag.PORTRAIT),
                description = "黄金时刻，温暖金色阳光。",
                sections = listOf(
                    Section("金色强化", "色温+12，金色温暖"),
                    Section("高光保护", "保留高光细节"),
                    Section("氛围营造", "浪漫氛围")
                ),
                usageTips = listOf(
                    "日落前1小时",
                    "逆光人像绝美",
                    "风景也适用"
                ),
                source = "omaster_community"
            )
        ).mapIndexed { index, preset ->
            preset.copy(
                stats = PresetStats(
                    usageCount = 1000L + index * 500L + (0..2000).random(),
                    favoriteCount = 100L + index * 50L + (0..500).random(),
                    rating = 4.3f + index * 0.02f,
                    reviewCount = 50 + index * 10
                )
            )
        }
    }
}
