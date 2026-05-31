package com.omaster.app.data

import com.omaster.app.model.CameraParams
import com.omaster.app.model.ColorStyle
import com.omaster.app.model.Preset
import com.omaster.app.model.Section
import com.omaster.app.network.PresetApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 预设数据仓库 - 支持数据同步和JSON刷新
 */
@Singleton
class PresetRepository @Inject constructor(
    private val presetApi: PresetApi,
    private val preferencesDataStore: PreferencesDataStore
) {
    private var cachedPresets: List<Preset> = emptyList()
    private var lastSyncTime: Long = 0
    private var isInitialized: Boolean = false
    
    private val _presets = MutableStateFlow<List<Preset>>(emptyList())
    val presets: StateFlow<List<Preset>> = _presets.asStateFlow()
    
    init {
        initializePresets()
    }
    
    private fun initializePresets() {
        cachedPresets = getSamplePresets()
        _presets.value = cachedPresets
    }
    
    /**
     * 获取预设列表 - 支持实时同步
     */
    fun getPresets(forceRefresh: Boolean = false): Flow<Result<List<Preset>>> = flow {
        val currentTime = System.currentTimeMillis()
        
        // 检查是否需要强制刷新或缓存过期（5分钟）
        val cacheExpired = (currentTime - lastSyncTime) > 5 * 60 * 1000
        
        try {
            if (forceRefresh || !isInitialized || cacheExpired) {
                // 从网络获取最新数据
                val response = presetApi.getAllPresets()
                if (response.isSuccessful) {
                    val presets = response.body() ?: emptyList()
                    cachedPresets = presets
                    lastSyncTime = currentTime
                    isInitialized = true
                    _presets.value = cachedPresets
                    Timber.d("成功从网络刷新预设数据，共 ${presets.size} 个")
                    emit(Result.success(presets))
                } else {
                    Timber.w("网络请求失败 (${response.code()})，使用缓存或示例数据")
                    emit(Result.success(getPresetsWithFallback()))
                }
            } else {
                // 使用缓存数据
                Timber.d("使用缓存数据，共 ${cachedPresets.size} 个预设")
                emit(Result.success(cachedPresets.ifEmpty { getSamplePresets() }))
            }
        } catch (e: Exception) {
            Timber.e(e, "获取预设数据失败")
            emit(Result.success(getPresetsWithFallback()))
        }
    }
    
    /**
     * 获取备用预设列表
     */
    private fun getPresetsWithFallback(): List<Preset> {
        return if (cachedPresets.isNotEmpty()) {
            cachedPresets
        } else {
            getSamplePresets()
        }
    }
    
    /**
     * 同步刷新预设数据
     */
    suspend fun syncPresets(): Result<List<Preset>> {
        return try {
            val response = presetApi.getAllPresets()
            if (response.isSuccessful) {
                val presets = response.body() ?: emptyList()
                cachedPresets = presets
                lastSyncTime = System.currentTimeMillis()
                isInitialized = true
                _presets.value = cachedPresets
                Timber.d("数据同步成功，共 ${presets.size} 个预设")
                Result.success(presets)
            } else {
                Timber.e("同步失败: ${response.code()}")
                Result.failure(Exception("同步失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "同步异常")
            Result.failure(e)
        }
    }
    
    /**
     * 切换预设收藏状态
     */
    suspend fun toggleFavorite(presetId: String) {
        preferencesDataStore.toggleFavorite(presetId)
        
        // 更新本地缓存
        val updatedPresets = cachedPresets.map { preset ->
            if (preset.id == presetId) {
                preset.copy(isFavorite = !preset.isFavorite)
            } else {
                preset
            }
        }
        cachedPresets = updatedPresets
        _presets.value = cachedPresets
        Timber.d("预设 $presetId 收藏状态已切换")
    }
    
    /**
     * 获取OPPO预设
     */
    fun getOppoPresets(): Flow<Result<List<Preset>>> = flow {
        try {
            val response = presetApi.getOppoPresets()
            if (response.isSuccessful) {
                val presets = response.body() ?: emptyList()
                emit(Result.success(presets))
            } else {
                Timber.w("获取OPPO预设失败，使用过滤数据")
                emit(Result.success(getSamplePresets().filter { 
                    it.deviceModel?.contains("OPPO", ignoreCase = true) == true 
                }))
            }
        } catch (e: Exception) {
            Timber.e(e, "获取OPPO预设异常")
            emit(Result.success(getSamplePresets().filter { 
                it.deviceModel?.contains("OPPO", ignoreCase = true) == true 
            }))
        }
    }
    
    /**
     * 获取realme预设
     */
    fun getRealmePresets(): Flow<Result<List<Preset>>> = flow {
        try {
            val response = presetApi.getRealmePresets()
            if (response.isSuccessful) {
                val presets = response.body() ?: emptyList()
                emit(Result.success(presets))
            } else {
                Timber.w("获取realme预设失败，使用过滤数据")
                emit(Result.success(getSamplePresets().filter { 
                    it.deviceModel?.contains("realme", ignoreCase = true) == true 
                }))
            }
        } catch (e: Exception) {
            Timber.e(e, "获取realme预设异常")
            emit(Result.success(getSamplePresets().filter { 
                it.deviceModel?.contains("realme", ignoreCase = true) == true 
            }))
        }
    }
    
    /**
     * 更新预设收藏状态
     */
    fun updatePresetFavorite(presetId: String, isFavorite: Boolean): Flow<Result<Preset?>> = flow {
        try {
            val updatedPresets = cachedPresets.map { preset ->
                if (preset.id == presetId) {
                    preset.copy(isFavorite = isFavorite)
                } else {
                    preset
                }
            }
            cachedPresets = updatedPresets
            _presets.value = cachedPresets
            
            val updatedPreset = updatedPresets.find { it.id == presetId }
            emit(Result.success(updatedPreset))
            Timber.d("预设 $presetId 收藏状态已更新为 $isFavorite")
        } catch (e: Exception) {
            Timber.e(e, "更新收藏状态失败")
            emit(Result.failure(e))
        }
    }
    
    /**
     * 获取示例预设 - 符合2026年OPPO Find X8 Ultra哈苏大师模式
     */
    private fun getSamplePresets(): List<Preset> {
        return listOf(
            // ==================== OPPO Find X8 Ultra 哈苏大师预设 ====================
            Preset(
                id = "oppo_findx8ultra_001",
                name = "哈苏人像经典",
                coverPath = "findx8_ultra_portrait_classic",
                deviceModel = "OPPO Find X8 Ultra",
                source = "official",
                author = "哈苏影像实验室",
                sceneType = "portrait",
                tags = listOf("人像", "哈苏", "经典"),
                rating = 5.0f,
                downloadCount = 12580,
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    filter = "哈苏自然色彩",
                    iso = 100,
                    shutter = "1/200",
                    ev = "+0.3",
                    wb = "5500K",
                    focalLength = "50mm",
                    focalLengthMode = "人像焦",
                    aperture = "f/1.6",
                    portraitMode = true,
                    aiOptimization = true,
                    hasselbladHncs = true,
                    hasselbladNaturalColor = true,
                    hasselbladMasterStyle = "Portrait Pro",
                    hasselbladColorScience = "HNCS 3.0",
                    colorProfile = "自然",
                    colorStyle = "Portrait",
                    sharpness = 45,
                    contrast = 50,
                    saturation = 50,
                    sensorSize = "1英寸双大底"
                ),
                sections = listOf(
                    Section("适用场景", "室内外人像拍摄，呈现自然肤色和柔和背景虚化效果"),
                    Section("样张说明", "使用 OPPO Find X8 Ultra 哈苏人像镜头，1英寸双大底传感器加持")
                )
            ),
            
            Preset(
                id = "oppo_findx8ultra_002",
                name = "自然风光大师",
                coverPath = "findx8_ultra_landscape_master",
                deviceModel = "OPPO Find X8 Ultra",
                source = "official",
                author = "哈苏影像实验室",
                sceneType = "landscape",
                tags = listOf("风景", "自然", "大师"),
                rating = 4.9f,
                downloadCount = 9860,
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    filter = "自然风景",
                    iso = 64,
                    shutter = "1/500",
                    ev = "+0.7",
                    wb = "6500K",
                    focalLength = "23mm",
                    focalLengthMode = "超广角",
                    aperture = "f/2.8",
                    hdr = true,
                    aiOptimization = true,
                    hasselbladHncs = true,
                    hasselbladNaturalColor = true,
                    hasselbladMasterStyle = "Landscape",
                    hasselbladColorScience = "HNCS 3.0",
                    colorProfile = "鲜明",
                    colorStyle = "Vivid",
                    sharpness = 55,
                    contrast = 55,
                    saturation = 55,
                    sensorSize = "1英寸双大底"
                ),
                sections = listOf(
                    Section("适用场景", "风景、建筑摄影，展现真实色彩和细腻细节"),
                    Section("样张说明", "使用 OPPO Find X8 Ultra 1英寸双大底广角镜头拍摄")
                )
            ),
            
            Preset(
                id = "oppo_findx8ultra_003",
                name = "城市夜景之王",
                coverPath = "findx8_ultra_night_city_master",
                deviceModel = "OPPO Find X8 Ultra",
                source = "official",
                author = "哈苏影像实验室",
                sceneType = "night",
                tags = listOf("夜景", "城市", "夜拍"),
                rating = 4.8f,
                downloadCount = 15230,
                cameraParams = CameraParams(
                    mode = "哈苏夜景",
                    filter = "夜景增强",
                    iso = 3200,
                    shutter = "1/30",
                    ev = "+0.7",
                    wb = "4000K",
                    focalLength = "23mm",
                    focalLengthMode = "超广角",
                    aperture = "f/1.8",
                    nightMode = true,
                    hdr = true,
                    aiOptimization = true,
                    opticalStabilization = true,
                    hasselbladHncs = true,
                    hasselbladNaturalColor = true,
                    hasselbladMasterStyle = "Night Pro",
                    hasselbladColorScience = "HNCS 3.0",
                    colorProfile = "电影感",
                    colorStyle = "Cinematic",
                    sharpness = 50,
                    contrast = 55,
                    saturation = 50,
                    noiseReduction = 60,
                    sensorSize = "1英寸双大底"
                ),
                sections = listOf(
                    Section("适用场景", "夜间城市风光摄影，捕捉光影流动和建筑轮廓"),
                    Section("样张说明", "使用 OPPO Find X8 Ultra 1英寸双大底夜景模式拍摄")
                )
            ),
            
            Preset(
                id = "oppo_findx8ultra_004",
                name = "美食摄影专家",
                coverPath = "findx8_ultra_food_master",
                deviceModel = "OPPO Find X8 Ultra",
                source = "official",
                author = "哈苏影像实验室",
                sceneType = "food",
                tags = listOf("美食", "食物", "摄影"),
                rating = 4.7f,
                downloadCount = 7890,
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    filter = "美食模式",
                    iso = 200,
                    shutter = "1/125",
                    ev = "+0.3",
                    wb = "5000K",
                    focalLength = "50mm",
                    focalLengthMode = "人像焦",
                    aperture = "f/1.8",
                    aiOptimization = true,
                    hasselbladHncs = true,
                    hasselbladNaturalColor = true,
                    hasselbladColorScience = "HNCS 3.0",
                    colorProfile = "美食",
                    colorStyle = "Food",
                    sharpness = 50,
                    contrast = 50,
                    saturation = 65,
                    sensorSize = "1英寸双大底"
                ),
                sections = listOf(
                    Section("适用场景", "美食摄影，提升色彩饱和度和食欲感"),
                    Section("样张说明", "使用 OPPO Find X8 Ultra 超光影镜头拍摄")
                )
            ),
            
            Preset(
                id = "oppo_findx8ultra_005",
                name = "逆光人像大师",
                coverPath = "findx8_ultra_backlight_master",
                deviceModel = "OPPO Find X8 Ultra",
                source = "official",
                author = "哈苏影像实验室",
                sceneType = "portrait",
                tags = listOf("人像", "逆光", "哈苏"),
                rating = 4.9f,
                downloadCount = 11200,
                cameraParams = CameraParams(
                    mode = "哈苏人像",
                    filter = "人像逆光",
                    iso = 100,
                    shutter = "1/400",
                    ev = "+1.0",
                    wb = "5500K",
                    focalLength = "32mm",
                    focalLengthMode = "人像焦",
                    aperture = "f/1.8",
                    portraitMode = true,
                    hdr = true,
                    aiOptimization = true,
                    hasselbladHncs = true,
                    hasselbladNaturalColor = true,
                    hasselbladColorScience = "HNCS 3.0",
                    colorProfile = "人像",
                    colorStyle = "Portrait",
                    sharpness = 45,
                    contrast = 50,
                    saturation = 50,
                    highlight = -10,
                    shadow = 10,
                    sensorSize = "1英寸双大底"
                ),
                sections = listOf(
                    Section("适用场景", "逆光人像拍摄，保持主体亮度同时保留背景细节"),
                    Section("样张说明", "使用 OPPO Find X8 Ultra 哈苏超光影引擎拍摄")
                )
            ),
            
            Preset(
                id = "oppo_findx8ultra_006",
                name = "哈苏经典蓝调",
                coverPath = "findx8_ultra_classic_blue",
                deviceModel = "OPPO Find X8 Ultra",
                source = "community",
                author = "社区摄影师",
                sceneType = "artistic",
                tags = listOf("蓝调", "艺术", "冷调"),
                rating = 4.6f,
                downloadCount = 5430,
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    filter = "冷调风格",
                    iso = 100,
                    shutter = "1/320",
                    ev = "+0.0",
                    wb = "7000K",
                    focalLength = "48mm",
                    focalLengthMode = "标准",
                    aperture = "f/2.0",
                    aiOptimization = true,
                    hasselbladHncs = true,
                    hasselbladNaturalColor = true,
                    hasselbladColorScience = "HNCS 3.0",
                    colorProfile = "冷调",
                    colorStyle = "Cool",
                    sharpness = 50,
                    contrast = 55,
                    saturation = 45,
                    colorTemperature = 7000,
                    sensorSize = "1英寸双大底"
                ),
                sections = listOf(
                    Section("适用场景", "冷色调风格摄影，营造静谧氛围"),
                    Section("样张说明", "使用 OPPO Find X8 Ultra 哈苏色彩，呈现经典蓝色调")
                )
            ),
            
            Preset(
                id = "oppo_findx8ultra_007",
                name = "复古胶片风格",
                coverPath = "findx8_ultra_vintage_film",
                deviceModel = "OPPO Find X8 Ultra",
                source = "community",
                author = "复古摄影爱好者",
                sceneType = "artistic",
                tags = listOf("复古", "胶片", "艺术"),
                rating = 4.5f,
                downloadCount = 4210,
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    filter = "复古胶片",
                    iso = 400,
                    shutter = "1/125",
                    ev = "+0.3",
                    wb = "4500K",
                    focalLength = "24mm",
                    focalLengthMode = "广角",
                    aperture = "f/1.7",
                    aiOptimization = false,
                    hasselbladHncs = true,
                    hasselbladNaturalColor = true,
                    hasselbladColorScience = "HNCS 3.0",
                    colorProfile = "经典",
                    colorStyle = "Classic",
                    sharpness = 40,
                    contrast = 60,
                    saturation = 40,
                    colorTemperature = 4500,
                    tint = 5,
                    sensorSize = "1英寸双大底"
                ),
                sections = listOf(
                    Section("适用场景", "复古风格摄影，重现胶片质感"),
                    Section("样张说明", "使用 OPPO Find X8 Ultra 拍摄，复古滤镜增添怀旧感")
                )
            ),
            
            // ==================== OnePlus 13 Pro 哈苏预设 ====================
            Preset(
                id = "oneplus_13pro_001",
                name = "哈苏街头模式",
                coverPath = "oneplus_13pro_street_hasselblad",
                deviceModel = "OnePlus 13 Pro",
                source = "official",
                author = "一加影像实验室",
                sceneType = "street",
                tags = listOf("街拍", "街头", "哈苏"),
                rating = 4.8f,
                downloadCount = 8760,
                cameraParams = CameraParams(
                    mode = "哈苏街拍",
                    filter = "街头色彩",
                    iso = 400,
                    shutter = "1/250",
                    ev = "+0.3",
                    wb = "5500K",
                    focalLength = "35mm",
                    focalLengthMode = "标准",
                    aperture = "f/1.9",
                    aiOptimization = true,
                    hasselbladHncs = true,
                    hasselbladNaturalColor = true,
                    hasselbladMasterStyle = "Street",
                    hasselbladColorScience = "HNCS 3.0",
                    colorProfile = "经典",
                    colorStyle = "Classic",
                    sharpness = 55,
                    contrast = 55,
                    saturation = 45,
                    sensorSize = "1英寸大底"
                ),
                sections = listOf(
                    Section("适用场景", "街头摄影，快速捕捉瞬间画面"),
                    Section("样张说明", "使用 OnePlus 13 Pro 哈苏色彩调校")
                )
            ),
            
            Preset(
                id = "oneplus_13pro_002",
                name = "哈苏微距世界",
                coverPath = "oneplus_13pro_macro_hasselblad",
                deviceModel = "OnePlus 13 Pro",
                source = "official",
                author = "一加影像实验室",
                sceneType = "macro",
                tags = listOf("微距", "细节", "哈苏"),
                rating = 4.7f,
                downloadCount = 6540,
                cameraParams = CameraParams(
                    mode = "哈苏专业",
                    filter = "微距模式",
                    iso = 100,
                    shutter = "1/160",
                    ev = "+0.0",
                    wb = "5200K",
                    focalLength = "微距",
                    focalLengthMode = "超级微距",
                    aperture = "f/2.0",
                    aiOptimization = true,
                    hasselbladHncs = true,
                    hasselbladNaturalColor = true,
                    hasselbladColorScience = "HNCS 3.0",
                    colorProfile = "鲜明",
                    colorStyle = "Vivid",
                    sharpness = 60,
                    contrast = 50,
                    saturation = 55,
                    detailEnhancement = 70,
                    sensorSize = "1英寸大底"
                ),
                sections = listOf(
                    Section("适用场景", "微距摄影，展现细节之美"),
                    Section("样张说明", "使用 OnePlus 13 Pro 微距镜头，捕捉微观世界")
                )
            ),
            
            Preset(
                id = "oneplus_13pro_003",
                name = "金色时刻",
                coverPath = "oneplus_13pro_sunrise_golden",
                deviceModel = "OnePlus 13 Pro",
                source = "official",
                author = "一加影像实验室",
                sceneType = "sunset",
                tags = listOf("日出", "日落", "金色"),
                rating = 4.9f,
                downloadCount = 9120,
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    filter = "暖调色彩",
                    iso = 64,
                    shutter = "1/1000",
                    ev = "+0.7",
                    wb = "6000K",
                    focalLength = "23mm",
                    focalLengthMode = "超广角",
                    aperture = "f/2.6",
                    aiOptimization = true,
                    hasselbladHncs = true,
                    hasselbladNaturalColor = true,
                    hasselbladColorScience = "HNCS 3.0",
                    colorProfile = "暖调",
                    colorStyle = "Warm",
                    sharpness = 50,
                    contrast = 50,
                    saturation = 60,
                    colorTemperature = 6000,
                    sensorSize = "1英寸大底"
                ),
                sections = listOf(
                    Section("适用场景", "日出日落拍摄，记录金色时刻"),
                    Section("样张说明", "使用 OnePlus 13 Pro 哈苏镜头拍摄日出日落美景")
                )
            ),
            
            // ==================== realme GT7 Pro 预设 ====================
            Preset(
                id = "realme_gt7pro_001",
                name = "街拍达人",
                coverPath = "realme_gt7pro_street_master",
                deviceModel = "realme GT7 Pro",
                source = "official",
                author = "真我影像实验室",
                sceneType = "street",
                tags = listOf("街拍", "黑白", "艺术"),
                rating = 4.6f,
                downloadCount = 5890,
                cameraParams = CameraParams(
                    mode = "大师模式",
                    filter = "黑白艺术",
                    iso = 200,
                    shutter = "1/160",
                    ev = "+0.3",
                    wb = "5500K",
                    focalLength = "28mm",
                    focalLengthMode = "标准",
                    aperture = "f/1.9",
                    aiOptimization = true,
                    hasselbladHncs = true,
                    hasselbladNaturalColor = true,
                    hasselbladColorScience = "HNCS 3.0",
                    colorProfile = "黑白",
                    colorStyle = "BlackWhite",
                    sharpness = 55,
                    contrast = 60,
                    saturation = 0,
                    sensorSize = "1英寸大底"
                ),
                sections = listOf(
                    Section("适用场景", "人像摄影，呈现经典黑白质感"),
                    Section("样张说明", "使用 realme GT7 Pro 拍摄，黑白风格更具艺术感")
                )
            ),
            
            Preset(
                id = "realme_gt7pro_002",
                name = "海岛风情",
                coverPath = "realme_gt7pro_beach_paradise",
                deviceModel = "realme GT7 Pro",
                source = "official",
                author = "真我影像实验室",
                sceneType = "landscape",
                tags = listOf("海边", "度假", "清新"),
                rating = 4.7f,
                downloadCount = 7230,
                cameraParams = CameraParams(
                    mode = "大师模式",
                    filter = "清新色彩",
                    iso = 50,
                    shutter = "1/800",
                    ev = "+0.3",
                    wb = "6500K",
                    focalLength = "16mm",
                    focalLengthMode = "超广角",
                    aperture = "f/2.2",
                    aiOptimization = true,
                    hasselbladHncs = true,
                    hasselbladNaturalColor = true,
                    hasselbladColorScience = "HNCS 3.0",
                    colorProfile = "鲜明",
                    colorStyle = "Vivid",
                    sharpness = 50,
                    contrast = 50,
                    saturation = 60,
                    sensorSize = "1英寸大底"
                ),
                sections = listOf(
                    Section("适用场景", "海边度假摄影，呈现蓝天白云和清澈海水"),
                    Section("样张说明", "使用 realme GT7 Pro 拍摄海岛风光，色彩鲜艳通透")
                )
            )
        )
    }
}
