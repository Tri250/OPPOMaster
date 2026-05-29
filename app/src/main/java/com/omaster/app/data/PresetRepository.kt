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
                emit(Result.success(getEnhancedSamplePresets()))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching presets")
            emit(Result.success(getEnhancedSamplePresets()))
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
                emit(Result.success(getEnhancedSamplePresets().filter { 
                    it.deviceModel?.contains("OPPO", ignoreCase = true) == true 
                }))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching OPPO presets")
            emit(Result.success(getEnhancedSamplePresets().filter { 
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
                Timber.e("Failed to fetch realme presets: ${response.code()}")
                emit(Result.success(getEnhancedSamplePresets().filter { 
                    it.deviceModel?.contains("realme", ignoreCase = true) == true 
                }))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching realme presets")
            emit(Result.success(getEnhancedSamplePresets().filter { 
                it.deviceModel?.contains("realme", ignoreCase = true) == true 
            }))
        }
    }
    
    private fun getEnhancedSamplePresets(): List<Preset> {
        return listOf(
            Preset(
                id = "oppo_1",
                name = "德味预设",
                deviceModel = "OPPO Find X9 Pro",
                coverPath = "https://cdn.fky.ltd/dw_01.webp",
                source = "community",
                author = "@波子Booz",
                isNew = true,
                tags = listOf("Auto"),
                description = "适合日间户外或光线充足的室内，街拍、建筑、风景、人文，德味风格，影调偏暗，色彩浓郁",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 100,
                    shutter = "1/200",
                    ev = "+0.3",
                    wb = "5500K",
                    focal_length = "50mm",
                    aperture = "f/1.6",
                    portrait_mode = false,
                    ai_optimization = true,
                    hasselblad_hncs = true,
                    hasselblad_natural_color = true,
                    hasselblad_master_style = "German Flavor",
                    color_profile = "German",
                    filter = "德味色彩"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "日间户外或光线充足的室内，街拍、建筑、风景、人文"
                    ),
                    Section(
                        title = "拍摄要点",
                        content = "德味风格，影调偏暗，色彩浓郁，适合追求经典德系胶片质感的摄影爱好者"
                    )
                ),
                galleryImages = listOf(
                    "https://cdn.fky.ltd/dw_02.webp",
                    "https://cdn.fky.ltd/dw_03.webp"
                )
            ),
            Preset(
                id = "oppo_2",
                name = "富士胶片",
                deviceModel = "OPPO Find X8 Pro",
                coverPath = "https://picsum.photos/seed/fujifilm/600/450",
                source = "official",
                author = "@OPPO影像",
                tags = listOf("Auto"),
                description = "适合日间户外，街拍、人像、风景、建筑，经典胶片质感",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 200,
                    shutter = "1/250",
                    ev = "+0.0",
                    wb = "5200K",
                    focal_length = "35mm",
                    aperture = "f/2.0",
                    ai_optimization = true,
                    hasselblad_natural_color = true,
                    color_profile = "Classic Film",
                    filter = "富士胶片"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "日间户外，街拍、人像、风景、建筑"
                    ),
                    Section(
                        title = "拍摄要点",
                        content = "适合追求经典胶片质感的场景，色彩浓郁复古，建议寻找有光影对比的场景增强层次感"
                    )
                ),
                galleryImages = listOf(
                    "https://picsum.photos/seed/fujifilm1/600/450",
                    "https://picsum.photos/seed/fujifilm2/600/450"
                )
            ),
            Preset(
                id = "oppo_3",
                name = "胶片感",
                deviceModel = "OPPO Find X8",
                coverPath = "https://picsum.photos/seed/film/600/450",
                source = "official",
                author = "@OPPO影像",
                tags = listOf("Auto"),
                description = "适合自然光或柔和人工光源，人像写真、静物、咖啡馆、文艺场景",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 400,
                    shutter = "1/125",
                    ev = "+0.3",
                    wb = "5000K",
                    focal_length = "50mm",
                    aperture = "f/1.8",
                    ai_optimization = true,
                    hasselblad_natural_color = true,
                    color_profile = "Film",
                    filter = "胶片"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "自然光或柔和人工光源，人像写真、静物、咖啡馆、文艺场景"
                    ),
                    Section(
                        title = "拍摄要点",
                        content = "柔光效果营造梦幻氛围，适合拍摄情绪感照片，建议对焦主体保持清晰"
                    )
                ),
                galleryImages = listOf(
                    "https://picsum.photos/seed/film1/600/450",
                    "https://picsum.photos/seed/film2/600/450"
                )
            ),
            Preset(
                id = "oppo_4",
                name = "童话",
                deviceModel = "OPPO Reno 13",
                coverPath = "https://picsum.photos/seed/fairytale/600/450",
                source = "community",
                author = "@OPPO影像",
                tags = listOf("Auto"),
                description = "适合清晨、黄昏或阴天散射光，儿童摄影、花园、公园、浪漫场景",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 100,
                    shutter = "1/200",
                    ev = "+0.7",
                    wb = "6000K",
                    focal_length = "35mm",
                    aperture = "f/2.0",
                    ai_optimization = true,
                    hasselblad_natural_color = true,
                    color_profile = "Dreamy",
                    filter = "童话"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "清晨、黄昏或阴天散射光，儿童摄影、花园、公园、浪漫场景"
                    ),
                    Section(
                        title = "拍摄要点",
                        content = "影调偏暗营造神秘感，梦幻柔光适合营造童话氛围，建议寻找色彩丰富的场景"
                    )
                ),
                galleryImages = listOf(
                    "https://picsum.photos/seed/fairytale1/600/450",
                    "https://picsum.photos/seed/fairytale2/600/450"
                )
            ),
            Preset(
                id = "oppo_5",
                name = "高对比黑白",
                deviceModel = "OPPO Find X7 Ultra",
                coverPath = "https://picsum.photos/seed/bw/600/450",
                source = "official",
                author = "@OPPO影像",
                tags = listOf("Auto"),
                description = "适合强烈光影对比场景，建筑、纪实摄影、街头、艺术人像",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 200,
                    shutter = "1/500",
                    ev = "+0.0",
                    wb = "5500K",
                    focal_length = "35mm",
                    aperture = "f/2.2",
                    ai_optimization = true,
                    color_profile = "Monochrome",
                    filter = "高对比黑白"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "强烈光影对比场景，如阳光直射或聚光灯，建筑、纪实摄影、街头、艺术人像"
                    ),
                    Section(
                        title = "拍摄要点",
                        content = "利用明暗对比突出主体轮廓，适合几何线条和纹理丰富的场景，注意构图简洁有力"
                    )
                ),
                galleryImages = listOf(
                    "https://picsum.photos/seed/bw1/600/450",
                    "https://picsum.photos/seed/bw2/600/450"
                )
            ),
            Preset(
                id = "oppo_6",
                name = "理光绿",
                deviceModel = "OPPO Find X9",
                coverPath = "https://picsum.photos/seed/ricohg/600/450",
                source = "community",
                author = "@OPPO影像",
                tags = listOf("Auto"),
                description = "适合户外自然光，森林、草地、植物丰富的场景",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 100,
                    shutter = "1/320",
                    ev = "+0.3",
                    wb = "5200K",
                    focal_length = "28mm",
                    aperture = "f/2.8",
                    ai_optimization = true,
                    hasselblad_natural_color = true,
                    color_profile = "Ricoh Green",
                    filter = "理光绿"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "户外自然光，森林、草地、植物丰富的场景"
                    ),
                    Section(
                        title = "拍摄要点",
                        content = "影调偏亮突出清新感，绿色表现自然通透，适合拍摄植物和户外自然场景"
                    )
                ),
                galleryImages = listOf(
                    "https://picsum.photos/seed/ricohg1/600/450",
                    "https://picsum.photos/seed/ricohg2/600/450"
                )
            ),
            Preset(
                id = "oppo_7",
                name = "理光蓝",
                deviceModel = "OPPO Find X7",
                coverPath = "https://picsum.photos/seed/ricohb/600/450",
                source = "community",
                author = "@OPPO影像",
                tags = listOf("Auto"),
                description = "适合晴朗天气或蓝天背景，海边、城市建筑、天空、冷色调场景",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 100,
                    shutter = "1/1000",
                    ev = "+0.0",
                    wb = "7000K",
                    focal_length = "24mm",
                    aperture = "f/2.8",
                    ai_optimization = true,
                    color_profile = "Ricoh Blue",
                    filter = "理光蓝"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "晴朗天气或蓝天背景，海边、城市建筑、天空、冷色调场景"
                    ),
                    Section(
                        title = "拍摄要点",
                        content = "偏冷色调增强蓝色表现力，适合拍摄天空、水面和城市建筑，营造通透冷静的氛围"
                    )
                ),
                galleryImages = listOf(
                    "https://picsum.photos/seed/ricohb1/600/450",
                    "https://picsum.photos/seed/ricohb2/600/450"
                )
            ),
            Preset(
                id = "oppo_8",
                name = "蓝调时刻",
                deviceModel = "OPPO Find X9 Ultra",
                coverPath = "https://picsum.photos/seed/bluemoment/600/450",
                source = "official",
                author = "@OPPO影像",
                tags = listOf("Auto"),
                description = "适合日出前或日落后20分钟的蓝调时刻，城市夜景、灯光璀璨的场景、水面倒影",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 800,
                    shutter = "1/30",
                    ev = "+0.7",
                    wb = "4200K",
                    focal_length = "23mm",
                    aperture = "f/1.8",
                    night_mode = true,
                    hdr = true,
                    ai_optimization = true,
                    hasselblad_hncs = true,
                    color_profile = "Blue Hour",
                    filter = "蓝调时刻"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "日出前或日落后20分钟的蓝调时刻，城市夜景、灯光璀璨的场景、水面倒影"
                    ),
                    Section(
                        title = "拍摄要点",
                        content = "冷暖对比强烈，适合拍摄城市灯光和夜景，建议寻找有水面的场景增强倒影效果"
                    )
                ),
                galleryImages = listOf(
                    "https://picsum.photos/seed/bluemoment1/600/450"
                )
            ),
            Preset(
                id = "oppo_9",
                name = "梦幻黑柔",
                deviceModel = "OPPO Find X8 Ultra",
                coverPath = "https://picsum.photos/seed/dreamsoft/600/450",
                source = "official",
                author = "@OPPO影像",
                tags = listOf("Auto"),
                description = "适合逆光或侧逆光场景，人像写真、情绪摄影、艺术场景、柔美人像",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 200,
                    shutter = "1/250",
                    ev = "+1.0",
                    wb = "5200K",
                    focal_length = "85mm",
                    aperture = "f/1.4",
                    portrait_mode = true,
                    ai_optimization = true,
                    hasselblad_hncs = true,
                    color_profile = "Dreamy Soft",
                    filter = "梦幻黑柔"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "逆光或侧逆光场景，人像写真、情绪摄影、艺术场景、柔美人像"
                    ),
                    Section(
                        title = "拍摄要点",
                        content = "黑柔滤镜效果营造梦幻氛围，适合拍摄唯美人像，建议利用逆光创造光晕效果"
                    )
                ),
                galleryImages = listOf(
                    "https://picsum.photos/seed/dreamsoft1/600/450",
                    "https://picsum.photos/seed/dreamsoft2/600/450"
                )
            ),
            Preset(
                id = "oppo_10",
                name = "富士NC",
                deviceModel = "OPPO Find X8",
                coverPath = "https://picsum.photos/seed/fujinc/600/450",
                source = "official",
                author = "@OPPO影像",
                tags = listOf("Auto"),
                description = "适合日间户外，街拍、人文、日常记录",
                cameraParams = CameraParams(
                    mode = "哈苏大师",
                    iso = 200,
                    shutter = "1/250",
                    ev = "+0.0",
                    wb = "5500K",
                    focal_length = "35mm",
                    aperture = "f/2.0",
                    ai_optimization = true,
                    color_profile = "Fuji NC",
                    filter = "富士NC"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "日间户外，街拍、人文、日常记录"
                    ),
                    Section(
                        title = "拍摄要点",
                        content = "色彩鲜艳自然，适合记录生活中的美好瞬间"
                    )
                ),
                galleryImages = listOf(
                    "https://picsum.photos/seed/fujinc1/600/450",
                    "https://picsum.photos/seed/fujinc2/600/450"
                )
            ),
            Preset(
                id = "realme_1",
                name = "理光正片",
                deviceModel = "realme GT Neo 7",
                coverPath = "https://cdn.fky.ltd/zwzp_01.webp",
                source = "community",
                author = "@尼克lin",
                tags = listOf("Auto"),
                description = "适合日间户外，光线充足的场景，街拍、建筑、人文、日常记录",
                cameraParams = CameraParams(
                    mode = "大师模式",
                    iso = 100,
                    shutter = "1/500",
                    ev = "+0.0",
                    wb = "5500K",
                    focal_length = "35mm",
                    aperture = "f/2.0",
                    ai_optimization = true,
                    color_profile = "Ricoh Positive",
                    filter = "理光正片"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "日间户外，光线充足的场景，街拍、建筑、人文、日常记录"
                    ),
                    Section(
                        title = "拍摄要点",
                        content = "模拟理光GR正片风格，色彩鲜艳对比度高，适合追求胶片质感的拍摄场景"
                    )
                ),
                galleryImages = listOf(
                    "https://cdn.fky.ltd/zwzp_02.webp",
                    "https://cdn.fky.ltd/zwzp_03.webp"
                )
            ),
            Preset(
                id = "realme_2",
                name = "理光负片",
                deviceModel = "realme GT Neo 6",
                coverPath = "https://cdn.fky.ltd/lgfp_01.webp",
                source = "community",
                author = "@尼克lin",
                tags = listOf("Auto"),
                description = "适合日间户外，光线充足的场景，街拍、建筑、人文、日常记录",
                cameraParams = CameraParams(
                    mode = "大师模式",
                    iso = 200,
                    shutter = "1/250",
                    ev = "+0.0",
                    wb = "5200K",
                    focal_length = "28mm",
                    aperture = "f/2.8",
                    ai_optimization = true,
                    color_profile = "Ricoh Negative",
                    filter = "理光负片"
                ),
                sections = listOf(
                    Section(
                        title = "适用场景",
                        content = "日间户外，光线充足的场景，街拍、建筑、人文、日常记录"
                    ),
                    Section(
                        title = "拍摄要点",
                        content = "模拟理光GR负片风格，色彩自然略带胶片感，适合追求真实质感的拍摄场景"
                    )
                ),
                galleryImages = listOf(
                    "https://cdn.fky.ltd/lg/lgfp_02.webp",
                    "https://cdn.fky.ltd/lg/lgfp_03.webp"
                )
            )
        )
    }
}
