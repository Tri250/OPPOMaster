package com.omaster.app.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.omaster.app.domain.model.CameraParams
import com.omaster.app.domain.model.ColorStyle
import com.omaster.app.domain.model.Preset
import com.omaster.app.domain.model.SampleImage
import com.omaster.app.domain.model.Section
import com.omaster.app.data.remote.PresetApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
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
    private val gson = Gson()
    private var cachedPresets: List<Preset> = emptyList()
    private var lastSyncTime: Long = 0
    private var isInitialized: Boolean = false
    
    private val _presets = MutableStateFlow<List<Preset>>(emptyList())
    val presets: StateFlow<List<Preset>> = _presets.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()
    
    // 使用 SupervisorJob 避免协程取消影响整个作用域
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    init {
        // 异步初始化，避免阻塞主线程导致 ANR
        repositoryScope.launch {
            initializePresets()
        }
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
        
        val cacheExpired = (currentTime - lastSyncTime) > 5 * 60 * 1000
        
        try {
            if (forceRefresh || !isInitialized || cacheExpired) {
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
                Timber.d("使用缓存数据，共 ${cachedPresets.size} 个预设")
                emit(Result.success(cachedPresets.ifEmpty { getSamplePresets() }))
            }
        } catch (e: Exception) {
            Timber.e(e, "获取预设数据失败")
            emit(Result.success(getPresetsWithFallback()))
        }
    }
    
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
     * 刷新预设样张库 - 从远程API获取最新数据
     */
    suspend fun refreshPresetLibrary(): Result<List<Preset>> {
        _isRefreshing.value = true
        _refreshError.value = null
        
        return try {
            Timber.d("开始刷新预设样张库...")
            
            val response = presetApi.getAllPresets()
            if (response.isSuccessful) {
                val presets = response.body() ?: emptyList()
                
                // 解析并验证数据
                val validatedPresets = presets.map { preset ->
                    validateAndEnrichPreset(preset)
                }
                
                cachedPresets = validatedPresets
                lastSyncTime = System.currentTimeMillis()
                isInitialized = true
                _presets.value = cachedPresets
                
                Timber.d("预设样张库刷新成功，共 ${validatedPresets.size} 个预设")
                Result.success(validatedPresets)
            } else {
                val error = "刷新失败: HTTP ${response.code()}"
                _refreshError.value = error
                Timber.e(error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            _refreshError.value = e.message ?: "刷新异常"
            Timber.e(e, "刷新预设样张库异常")
            Result.failure(e)
        } finally {
            _isRefreshing.value = false
        }
    }
    
    /**
     * 从JSON字符串解析预设列表
     */
    fun parsePresetsFromJson(jsonString: String): Result<List<Preset>> {
        return try {
            val type = object : TypeToken<List<PresetJsonData>>() {}.type
            val jsonDataList: List<PresetJsonData> = gson.fromJson(jsonString, type)
            
            val presets = jsonDataList.map { jsonData ->
                parsePresetFromJsonData(jsonData)
            }
            
            Timber.d("JSON解析成功，共 ${presets.size} 个预设")
            Result.success(presets)
        } catch (e: Exception) {
            Timber.e(e, "JSON解析失败")
            Result.failure(e)
        }
    }
    
    /**
     * 从JSON字符串解析单个预设
     */
    fun parsePresetFromJson(jsonString: String): Result<Preset> {
        return try {
            val jsonData: PresetJsonData = gson.fromJson(jsonString, PresetJsonData::class.java)
            val preset = parsePresetFromJsonData(jsonData)
            Timber.d("预设JSON解析成功: ${preset.name}")
            Result.success(preset)
        } catch (e: Exception) {
            Timber.e(e, "预设JSON解析失败")
            Result.failure(e)
        }
    }
    
    /**
     * 将预设列表转换为JSON字符串
     */
    fun presetsToJson(presets: List<Preset>): String {
        return gson.toJson(presets.map { it.toJson() })
    }
    
    /**
     * 验证并丰富预设数据
     */
    private fun validateAndEnrichPreset(preset: Preset): Preset {
        return preset.copy(
            // 确保HNCS认证状态正确
            isHncsCertified = preset.cameraParams?.hasselblad_hncs == true,
            // 确保版本信息存在
            version = preset.version.ifEmpty { "3.0" },
            // 确保作者信息存在
            author = preset.author.ifEmpty { "哈苏影像实验室" }
        )
    }
    
    /**
     * 从JSON数据对象解析预设
     */
    private fun parsePresetFromJsonData(jsonData: PresetJsonData): Preset {
        val cameraParams = jsonData.cameraParams?.let { params ->
            CameraParams(
                mode = params.mode ?: "哈苏大师",
                filter = params.filter ?: "",
                iso = params.iso ?: 100,
                shutter = params.shutter ?: "1/200",
                ev = params.ev ?: "+0.0",
                wb = params.wb ?: "5500K",
                focalLength = params.focalLength ?: "23mm",
                focalLengthMode = params.focalLengthMode ?: "标准",
                aperture = params.aperture ?: "f/1.8",
                hdr = params.hdr ?: false,
                nightMode = params.nightMode ?: false,
                portraitMode = params.portraitMode ?: false,
                aiOptimization = params.aiOptimization ?: true,
                opticalStabilization = params.opticalStabilization ?: false,
                rawCapture = params.rawCapture ?: false,
                proMode = params.proMode ?: false,
                hasselblad_hncs = params.hasselblad_hncs ?: false,
                hasselbladNaturalColor = params.hasselbladNaturalColor ?: false,
                hasselbladMasterStyle = params.hasselbladMasterStyle ?: "",
                hasselbladColorScience = params.hasselbladColorScience ?: "HNCS 3.0",
                colorProfile = params.colorProfile ?: "自然",
                colorStyle = params.colorStyle ?: ColorStyle.Natural.name,
                sharpness = params.sharpness ?: 50,
                contrast = params.contrast ?: 50,
                saturation = params.saturation ?: 50,
                noiseReduction = params.noiseReduction ?: 0,
                sensorSize = params.sensorSize ?: "1英寸大底"
            )
        }
        
        return Preset(
            id = jsonData.id ?: "",
            name = jsonData.name ?: "",
            coverPath = jsonData.coverPath ?: "",
            coverUrl = jsonData.coverUrl ?: "",
            deviceModel = jsonData.deviceModel ?: "",
            source = jsonData.source ?: "omaster_cloud",
            author = jsonData.author ?: "哈苏影像实验室",
            description = jsonData.description ?: "",
            sceneType = jsonData.sceneType ?: "",
            tags = jsonData.tags ?: emptyList(),
            rating = jsonData.rating ?: 5.0f,
            downloadCount = jsonData.downloadCount ?: 0,
            favoriteCount = jsonData.favoriteCount ?: 0,
            version = jsonData.version ?: "3.0",
            lastUpdated = jsonData.lastUpdated ?: System.currentTimeMillis(),
            publishDate = jsonData.publishDate ?: System.currentTimeMillis(),
            isHncsCertified = jsonData.isHncsCertified ?: (cameraParams?.hasselblad_hncs == true),
            cameraParams = cameraParams,
            sections = jsonData.sections?.map { Section(it.title ?: "", it.content ?: "") } ?: emptyList(),
            sampleImages = jsonData.sampleImages?.map { 
                SampleImage(it.id ?: "", it.imagePath ?: "", it.title ?: "", it.description ?: "", it.isBeforeImage ?: false, it.isAfterImage ?: false) 
            } ?: emptyList()
        )
    }
    
    /**
     * JSON数据类 - 用于解析
     */
    private data class PresetJsonData(
        val id: String? = null,
        val name: String? = null,
        val coverPath: String? = null,
        val coverUrl: String? = null,
        val deviceModel: String? = null,
        val source: String? = null,
        val author: String? = null,
        val description: String? = null,
        val sceneType: String? = null,
        val tags: List<String>? = null,
        val rating: Float? = null,
        val downloadCount: Int? = null,
        val favoriteCount: Int? = null,
        val version: String? = null,
        val lastUpdated: Long? = null,
        val publishDate: Long? = null,
        val isHncsCertified: Boolean? = null,
        val cameraParams: CameraParamsJsonData? = null,
        val sections: List<SectionJsonData>? = null,
        val sampleImages: List<SampleImageJsonData>? = null
    )
    
    private data class CameraParamsJsonData(
        val mode: String? = null,
        val filter: String? = null,
        val iso: Int? = null,
        val shutter: String? = null,
        val ev: String? = null,
        val wb: String? = null,
        val focalLength: String? = null,
        val focalLengthMode: String? = null,
        val aperture: String? = null,
        val hdr: Boolean? = null,
        val nightMode: Boolean? = null,
        val portraitMode: Boolean? = null,
        val aiOptimization: Boolean? = null,
        val opticalStabilization: Boolean? = null,
        val rawCapture: Boolean? = null,
        val proMode: Boolean? = null,
        val hasselblad_hncs: Boolean? = null,
        val hasselbladNaturalColor: Boolean? = null,
        val hasselbladMasterStyle: String? = null,
        val hasselbladColorScience: String? = null,
        val colorProfile: String? = null,
        val colorStyle: String? = null,
        val sharpness: Int? = null,
        val contrast: Int? = null,
        val saturation: Int? = null,
        val noiseReduction: Int? = null,
        val sensorSize: String? = null
    )
    
    private data class SectionJsonData(
        val title: String? = null,
        val content: String? = null
    )
    
    private data class SampleImageJsonData(
        val id: String? = null,
        val imagePath: String? = null,
        val title: String? = null,
        val description: String? = null,
        val isBeforeImage: Boolean? = null,
        val isAfterImage: Boolean? = null
    )
    
    /**
     * 切换预设收藏状态
     */
    suspend fun toggleFavorite(presetId: String) {
        preferencesDataStore.toggleFavorite(presetId)
        
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
            Preset(
                id = "oppo_findx8ultra_portrait_classic",
                name = "哈苏人像经典",
                coverPath = "hasselblad_portrait_classic",
                coverUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=800&h=600&fit=crop",
                deviceModel = "OPPO Find X8 Ultra",
                source = "hasselblad_official",
                author = "哈苏影像实验室",
                description = "专为OPPO Find X8 Ultra打造的人像摄影预设，采用哈苏自然色彩解决方案(HNCS)，还原真实肤色和自然光影。",
                sceneType = "portrait",
                tags = listOf("人像", "哈苏", "经典", "HNCS"),
                rating = 5.0f,
                downloadCount = 158642,
                favoriteCount = 28453,
                version = "3.0",
                isHncsCertified = true,
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
                    hasselblad_hncs = true,
                    hasselbladNaturalColor = true,
                    hasselbladMasterStyle = "Portrait Pro",
                    hasselbladColorScience = "HNCS 3.0",
                    colorProfile = "自然",
                    colorStyle = ColorStyle.Portrait.name,
                    sharpness = 45,
                    contrast = 50,
                    saturation = 50,
                    sensorSize = "1英寸双大底"
                ),
                sections = listOf(
                    Section("适用场景", "室内外人像摄影，呈现自然肤色和柔和背景虚化效果。"),
                    Section("哈苏特性", "采用哈苏自然色彩解决方案 (HNCS) 3.0，精确还原真实肤色。")
                ),
                sampleImages = listOf(
                    SampleImage("portrait_1", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800&h=600&fit=crop", "室内人像", "自然肤色呈现", false, true),
                    SampleImage("portrait_2", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=800&h=600&fit=crop", "街拍人像", "光影层次感", false, true)
                )
            ),
            
            Preset(
                id = "oppo_findx8ultra_landscape_master",
                name = "哈苏风景大师",
                coverPath = "hasselblad_landscape_master",
                coverUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&h=600&fit=crop",
                deviceModel = "OPPO Find X8 Ultra",
                source = "hasselblad_official",
                author = "哈苏影像实验室",
                description = "风景摄影专用预设，精准还原天空、草地、树木等自然色彩。",
                sceneType = "landscape",
                tags = listOf("风景", "哈苏", "自然色彩", "HNCS"),
                rating = 4.9f,
                downloadCount = 98642,
                favoriteCount = 19872,
                version = "3.0",
                isHncsCertified = true,
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
                    hasselblad_hncs = true,
                    hasselbladNaturalColor = true,
                    hasselbladMasterStyle = "Landscape",
                    hasselbladColorScience = "HNCS 3.0",
                    colorProfile = "鲜明",
                    colorStyle = ColorStyle.Vivid.name,
                    sharpness = 55,
                    contrast = 55,
                    saturation = 55,
                    sensorSize = "1英寸双大底"
                ),
                sections = listOf(
                    Section("适用场景", "风景、建筑摄影，展现真实色彩和细腻细节。"),
                    Section("技术参数", "Find X8 Ultra 1英寸双大底广角镜头。")
                ),
                sampleImages = listOf(
                    SampleImage("landscape_1", "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&h=600&fit=crop", "山脉风景", "层次分明的色彩呈现", false, true),
                    SampleImage("landscape_2", "https://images.unsplash.com/photo-1433086966358-54859d0ed716?w=800&h=600&fit=crop", "海滩风景", "自然蓝色调还原", false, true)
                )
            ),
            
            Preset(
                id = "oppo_findx8ultra_night_city",
                name = "哈苏夜景大师",
                coverPath = "hasselblad_night_master",
                coverUrl = "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=800&h=600&fit=crop",
                deviceModel = "OPPO Find X8 Ultra",
                source = "hasselblad_official",
                author = "哈苏影像实验室",
                description = "夜景摄影专用预设，有效控制噪点，保留暗部细节。",
                sceneType = "night",
                tags = listOf("夜景", "哈苏", "城市", "HNCS"),
                rating = 4.8f,
                downloadCount = 152342,
                favoriteCount = 21543,
                version = "3.0",
                isHncsCertified = true,
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
                    hasselblad_hncs = true,
                    hasselbladNaturalColor = true,
                    hasselbladMasterStyle = "Night Pro",
                    hasselbladColorScience = "HNCS 3.0",
                    colorProfile = "电影感",
                    colorStyle = ColorStyle.Cinematic.name,
                    sharpness = 50,
                    contrast = 55,
                    saturation = 50,
                    noiseReduction = 60,
                    sensorSize = "1英寸双大底"
                ),
                sections = listOf(
                    Section("适用场景", "夜间城市风光摄影，捕捉光影流动和建筑轮廓。"),
                    Section("哈苏特性", "夜景模式优化，超光影引擎带来纯净画质。")
                ),
                sampleImages = listOf(
                    SampleImage("night_1", "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=800&h=600&fit=crop", "城市夜景", "霓虹灯光控制", false, true),
                    SampleImage("night_2", "https://images.unsplash.com/photo-1480714378408-67cf0d13bc1b?w=800&h=600&fit=crop", "摩天大楼", "暗部细节保留", false, true)
                )
            ),
            
            Preset(
                id = "oppo_findx8ultra_food_expert",
                name = "哈苏美食摄影",
                coverPath = "hasselblad_food_expert",
                coverUrl = "https://images.unsplash.com/photo-1466978913421-dad2ebd01d17?w=800&h=600&fit=crop",
                deviceModel = "OPPO Find X8 Ultra",
                source = "hasselblad_official",
                author = "哈苏影像实验室",
                description = "美食摄影专用预设，提升色彩饱和度和食欲感。",
                sceneType = "food",
                tags = listOf("美食", "哈苏", "摄影", "HNCS"),
                rating = 4.7f,
                downloadCount = 78642,
                favoriteCount = 12543,
                version = "3.0",
                isHncsCertified = true,
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
                    hasselblad_hncs = true,
                    hasselbladNaturalColor = true,
                    hasselbladColorScience = "HNCS 3.0",
                    colorProfile = "美食",
                    colorStyle = ColorStyle.Food.name,
                    sharpness = 50,
                    contrast = 50,
                    saturation = 65,
                    sensorSize = "1英寸双大底"
                ),
                sections = listOf(
                    Section("适用场景", "美食摄影，提升色彩饱和度和食欲感。")
                ),
                sampleImages = listOf(
                    SampleImage("food_1", "https://images.unsplash.com/photo-1466978913421-dad2ebd01d17?w=800&h=600&fit=crop", "美食摆盘", "食欲色彩提升", false, true),
                    SampleImage("food_2", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800&h=600&fit=crop", "烘焙美食", "暖色调呈现", false, true)
                )
            ),
            
            Preset(
                id = "oneplus_13pro_street_hasselblad",
                name = "哈苏街拍模式",
                coverPath = "hasselblad_street_mode",
                coverUrl = "https://images.unsplash.com/photo-1494521695290-e1b495b63894?w=800&h=600&fit=crop",
                deviceModel = "OnePlus 13 Pro",
                source = "hasselblad_official",
                author = "一加影像实验室",
                description = "街拍专用预设，快速捕捉瞬间，色彩鲜明有活力。",
                sceneType = "street",
                tags = listOf("街拍", "哈苏", "街头", "HNCS"),
                rating = 4.8f,
                downloadCount = 87642,
                favoriteCount = 11234,
                version = "3.0",
                isHncsCertified = true,
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
                    hasselblad_hncs = true,
                    hasselbladNaturalColor = true,
                    hasselbladMasterStyle = "Street",
                    hasselbladColorScience = "HNCS 3.0",
                    colorProfile = "经典",
                    colorStyle = ColorStyle.Classic.name,
                    sharpness = 55,
                    contrast = 55,
                    saturation = 45,
                    sensorSize = "1英寸大底"
                ),
                sections = listOf(
                    Section("适用场景", "街头摄影，快速捕捉瞬间画面。")
                ),
                sampleImages = listOf(
                    SampleImage("street_1", "https://images.unsplash.com/photo-1494521695290-e1b495b63894?w=800&h=600&fit=crop", "街拍瞬间", "黑白对比效果", false, true),
                    SampleImage("street_2", "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?w=800&h=600&fit=crop", "城市街景", "层次分明", false, true)
                )
            ),
            
            Preset(
                id = "realme_gt7pro_beach_paradise",
                name = "海岛风情",
                coverPath = "realme_beach_paradise",
                coverUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&h=600&fit=crop",
                deviceModel = "realme GT7 Pro",
                source = "official",
                author = "真我影像实验室",
                description = "海边度假摄影专用，展现蓝天白云和清澈海水。",
                sceneType = "landscape",
                tags = listOf("海边", "度假", "清新"),
                rating = 4.7f,
                downloadCount = 72345,
                favoriteCount = 9876,
                version = "2.0",
                isHncsCertified = true,
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
                    hasselblad_hncs = true,
                    hasselbladNaturalColor = true,
                    hasselbladColorScience = "HNCS 3.0",
                    colorProfile = "鲜明",
                    colorStyle = ColorStyle.Vivid.name,
                    sharpness = 50,
                    contrast = 50,
                    saturation = 60,
                    sensorSize = "1英寸大底"
                ),
                sections = listOf(
                    Section("适用场景", "海边度假摄影，呈现蓝天白云和清澈海水。")
                ),
                sampleImages = listOf(
                    SampleImage("beach_1", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&h=600&fit=crop", "海滩风光", "碧海蓝天", false, true)
                )
            )
        )
    }
}
