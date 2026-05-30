package com.omaster.app.data

import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.model.Section
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 预设数据仓库
 * 提供丰富的示例数据用于测试 Search-001 至 Search-006
 */
@Singleton
class PresetRepository @Inject constructor() {
    
    /**
     * 获取所有预设数据
     * 包含 100+ 预设用于 Search-004 性能测试
     */
    fun getAllPresets(): List<Preset> {
        return buildList {
            // 基础预设
            addAll(getBasicPresets())
            
            // 扩展预设（大量数据用于性能测试）
            addAll(getExtendedPresets())
        }
    }
    
    /**
     * 基础预设数据
     */
    private fun getBasicPresets(): List<Preset> {
        return listOf(
            // 胶片风格
            Preset(
                id = "hasselblad_dewei",
                name = "德味预设",
                coverPath = "https://picsum.photos/seed/dewei/400/600",
                sections = listOf(
                    Section("色彩调校", "滤镜:明艳 100%, 柔光:无, 色调曲线:-35")
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "明艳",
                    filterIntensity = 100,
                    softLight = "无",
                    toneCurve = -35,
                    saturation = 0,
                    warmCool = -5,
                    cyanMagenta = 4,
                    sharpness = 10,
                    vignette = true
                ),
                deviceModel = "Find X9spro",
                author = "@波子Booz",
                source = "omaster_cloud",
                isFavorite = false,
                isNew = true,
                category = "街拍",
                difficulty = "中等",
                tags = listOf("德味", "哈苏", "大师模式", "胶片", "复古"),
                description = "环境建议:日间户外或光线充足的室内，场景推荐:街拍、建筑、风景、人文",
                style = "胶片",
                scene = "街拍"
            ),
            
            Preset(
                id = "fujifilm_film",
                name = "富士胶片",
                coverPath = "https://picsum.photos/seed/fujifilm/400/600",
                sections = listOf(
                    Section("色彩调校", "滤镜:复古 100%, 柔光:无, 色调曲线:0")
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "复古",
                    filterIntensity = 100,
                    softLight = "无",
                    toneCurve = 0,
                    saturation = 19,
                    warmCool = -5,
                    cyanMagenta = 0,
                    sharpness = 15,
                    vignette = true
                ),
                deviceModel = "Find X9spro",
                author = "@OPPO影像",
                source = "omaster_cloud",
                isFavorite = true,
                isNew = false,
                category = "胶片",
                difficulty = "简单",
                tags = listOf("胶片", "富士", "经典"),
                description = "适合追求经典胶片质感的场景",
                style = "胶片",
                scene = "街拍"
            ),
            
            Preset(
                id = "vintage_film",
                name = "复古胶片",
                coverPath = "https://picsum.photos/seed/vintage/400/600",
                sections = listOf(
                    Section("色彩调校", "滤镜:复古 100%, 颗粒感:+15")
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "复古",
                    filterIntensity = 100,
                    softLight = "无",
                    toneCurve = 0,
                    saturation = 15,
                    warmCool = 25,
                    vignette = true
                ),
                deviceModel = "Find X9spro",
                author = "@小陈工",
                source = "omaster_cloud",
                isFavorite = true,
                isNew = false,
                category = "胶片",
                difficulty = "中等",
                tags = listOf("复古", "胶片", "怀旧"),
                description = "复古胶片风格",
                style = "复古",
                scene = "街拍"
            ),
            
            // 清新风格
            Preset(
                id = "ricoh_green",
                name = "理光绿",
                coverPath = "https://picsum.photos/seed/green/400/600",
                sections = listOf(
                    Section("色彩调校", "滤镜:清新 100%, 柔光:梦幻, 色调曲线:+39")
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "清新",
                    filterIntensity = 100,
                    softLight = "梦幻",
                    toneCurve = 39,
                    saturation = 12,
                    warmCool = -2,
                    cyanMagenta = -9,
                    sharpness = 10,
                    vignette = true
                ),
                deviceModel = "Find X9spro",
                author = "@OPPO影像",
                source = "omaster_cloud",
                isFavorite = false,
                isNew = false,
                category = "风景",
                difficulty = "简单",
                tags = listOf("清新", "绿色", "自然"),
                description = "环境建议:户外自然光，森林、草地、植物丰富的场景",
                style = "清新",
                scene = "风景"
            ),
            
            Preset(
                id = "fresh_green",
                name = "清新绿野",
                coverPath = "https://picsum.photos/seed/freshgreen/400/600",
                sections = listOf(
                    Section("色彩调校", "滤镜:清新 100%, 饱和度:+15")
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "清新",
                    filterIntensity = 100,
                    softLight = "柔美",
                    toneCurve = 20,
                    saturation = 15,
                    warmCool = -10,
                    cyanMagenta = -15,
                    sharpness = 6,
                    vignette = false
                ),
                deviceModel = "Find X9spro",
                author = "@小陈工",
                source = "omaster_cloud",
                isFavorite = false,
                isNew = false,
                category = "风景",
                difficulty = "简单",
                tags = listOf("清新", "绿植", "自然"),
                description = "清新自然风格",
                style = "清新",
                scene = "风景"
            ),
            
            // 蓝调风格
            Preset(
                id = "ricoh_blue",
                name = "理光蓝",
                coverPath = "https://picsum.photos/seed/blue/400/600",
                sections = listOf(
                    Section("色彩调校", "滤镜:通透 100%, 柔光:柔美, 色调曲线:+18")
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "通透",
                    filterIntensity = 100,
                    softLight = "柔美",
                    toneCurve = 18,
                    saturation = -2,
                    warmCool = -8,
                    cyanMagenta = 19,
                    sharpness = 11,
                    vignette = true
                ),
                deviceModel = "Find X9spro",
                author = "@OPPO影像",
                source = "omaster_cloud",
                isFavorite = true,
                isNew = true,
                category = "建筑",
                difficulty = "中等",
                tags = listOf("蓝色", "通透", "建筑"),
                description = "环境建议:晴朗天气或蓝天背景，场景推荐:海边、城市建筑、天空",
                style = "蓝调",
                scene = "街拍"
            ),
            
            Preset(
                id = "blue_hour",
                name = "蓝调时刻",
                coverPath = "https://picsum.photos/seed/bluehour/400/600",
                sections = listOf(
                    Section("色彩调校", "滤镜:复古 100%, 柔光:梦幻, 色调曲线:-5")
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "复古",
                    filterIntensity = 100,
                    softLight = "梦幻",
                    toneCurve = -5,
                    saturation = 15,
                    warmCool = 47,
                    cyanMagenta = 28,
                    sharpness = 12,
                    vignette = true,
                    iso = 800,
                    shutterSpeed = "1/30",
                    exposureCompensation = "-0.7"
                ),
                deviceModel = "Find X9spro",
                author = "@OPPO影像",
                source = "omaster_cloud",
                isFavorite = false,
                isNew = false,
                category = "夜景",
                difficulty = "进阶",
                tags = listOf("蓝调", "夜景", "城市"),
                description = "环境建议:日出前或日落后20分钟的蓝调时刻",
                style = "蓝调",
                scene = "夜景"
            ),
            
            // 人像风格
            Preset(
                id = "fairy_tale",
                name = "童话",
                coverPath = "https://picsum.photos/seed/fairytale/400/600",
                sections = listOf(
                    Section("色彩调校", "滤镜:童话 73%, 柔光:梦幻, 色调曲线:-24")
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "童话",
                    filterIntensity = 73,
                    softLight = "梦幻",
                    toneCurve = -24,
                    saturation = 12,
                    warmCool = 3,
                    cyanMagenta = 7,
                    sharpness = 0,
                    vignette = true
                ),
                deviceModel = "Find X9spro",
                author = "@OPPO影像",
                source = "omaster_cloud",
                isFavorite = true,
                isNew = false,
                category = "人像",
                difficulty = "中等",
                tags = listOf("童话", "梦幻", "儿童"),
                description = "环境建议:清晨、黄昏或阴天散射光，场景推荐:儿童摄影、花园",
                style = "梦幻",
                scene = "人像"
            ),
            
            Preset(
                id = "dream_soft",
                name = "梦幻黑柔",
                coverPath = "https://picsum.photos/seed/dream/400/600",
                sections = listOf(
                    Section("色彩调校", "滤镜:标准 0%, 柔光:梦幻, 色调曲线:-25")
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "标准",
                    filterIntensity = 0,
                    softLight = "梦幻",
                    toneCurve = -25,
                    saturation = 11,
                    warmCool = 30,
                    cyanMagenta = -9,
                    sharpness = 0,
                    vignette = true
                ),
                deviceModel = "Find X9spro",
                author = "@OPPO影像",
                source = "omaster_cloud",
                isFavorite = false,
                isNew = true,
                category = "人像",
                difficulty = "专家",
                tags = listOf("黑柔", "梦幻", "唯美人像"),
                description = "环境建议:逆光或侧逆光场景，场景推荐:人像写真、情绪摄影",
                style = "梦幻",
                scene = "人像"
            ),
            
            Preset(
                id = "portrait_soft",
                name = "柔光人像",
                coverPath = "https://picsum.photos/seed/portrait/400/600",
                sections = listOf(
                    Section("色彩调校", "滤镜:童话 80%, 柔光:柔美")
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "童话",
                    filterIntensity = 80,
                    softLight = "柔美",
                    toneCurve = 5,
                    saturation = 10,
                    warmCool = 5,
                    vignette = true
                ),
                deviceModel = "Find X9spro",
                author = "@小陈工",
                source = "omaster_cloud",
                isFavorite = true,
                isNew = false,
                category = "人像",
                difficulty = "简单",
                tags = listOf("人像", "柔光", "清新"),
                description = "柔和的人像风格",
                style = "清新",
                scene = "人像"
            ),
            
            // 纪实风格
            Preset(
                id = "high_contrast_bw",
                name = "高对比黑白",
                coverPath = "https://picsum.photos/seed/bw/400/600",
                sections = listOf(
                    Section("色彩调校", "滤镜:黑白 100%, 柔光:柔美, 色调曲线:-61")
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "黑白",
                    filterIntensity = 100,
                    softLight = "柔美",
                    toneCurve = -61,
                    saturation = 0,
                    warmCool = 100,
                    cyanMagenta = -39,
                    sharpness = 0,
                    vignette = false
                ),
                deviceModel = "Find X9spro",
                author = "@OPPO影像",
                source = "omaster_cloud",
                isFavorite = false,
                isNew = true,
                category = "纪实",
                difficulty = "进阶",
                tags = listOf("黑白", "纪实", "街拍"),
                description = "环境建议:强烈光影对比场景，场景推荐:建筑、纪实摄影、街头",
                style = "黑白",
                scene = "街拍"
            ),
            
            // 夜景风格
            Preset(
                id = "neon_night",
                name = "霓虹夜色",
                coverPath = "https://picsum.photos/seed/neon/400/600",
                sections = listOf(
                    Section("色彩调校", "滤镜:通透 100%, 饱和度:+30")
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "通透",
                    filterIntensity = 100,
                    softLight = "梦幻",
                    toneCurve = -10,
                    saturation = 30,
                    warmCool = -30,
                    cyanMagenta = 20,
                    sharpness = 12,
                    vignette = true
                ),
                deviceModel = "Find X9spro",
                author = "@小陈工",
                source = "omaster_cloud",
                isFavorite = true,
                isNew = true,
                category = "夜景",
                difficulty = "中等",
                tags = listOf("霓虹", "夜景", "城市"),
                description = "霓虹夜景风格",
                style = "霓虹",
                scene = "夜景"
            ),
            
            Preset(
                id = "night_cyber",
                name = "赛博夜景",
                coverPath = "https://picsum.photos/seed/cyber/400/600",
                sections = listOf(
                    Section("色彩调校", "滤镜:通透 100%, 饱和度:+40")
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "通透",
                    filterIntensity = 100,
                    softLight = "无",
                    toneCurve = 20,
                    saturation = 40,
                    warmCool = -40,
                    cyanMagenta = 35,
                    sharpness = 15,
                    vignette = true
                ),
                deviceModel = "Find X9spro",
                author = "@小陈工",
                source = "omaster_cloud",
                isFavorite = false,
                isNew = true,
                category = "夜景",
                difficulty = "进阶",
                tags = listOf("赛博", "霓虹", "夜景"),
                description = "赛博朋克风格夜景",
                style = "霓虹",
                scene = "夜景"
            ),
            
            // 美食风格
            Preset(
                id = "food_vibrant",
                name = "美食诱人",
                coverPath = "https://picsum.photos/seed/food/400/600",
                sections = listOf(
                    Section("色彩调校", "滤镜:明艳 90%, 饱和度:+20")
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "明艳",
                    filterIntensity = 90,
                    softLight = "无",
                    toneCurve = 15,
                    saturation = 20,
                    warmCool = 15,
                    sharpness = 10,
                    vignette = false
                ),
                deviceModel = "Find X9spro",
                author = "@小陈工",
                source = "omaster_cloud",
                isFavorite = false,
                isNew = true,
                category = "美食",
                difficulty = "简单",
                tags = listOf("美食", "明艳", "暖色"),
                description = "诱人的美食风格",
                style = "胶片",
                scene = "美食"
            ),
            
            // 电影风格
            Preset(
                id = "cinema_wide",
                name = "电影宽幅",
                coverPath = "https://picsum.photos/seed/cinema/400/600",
                sections = listOf(
                    Section("色彩调校", "滤镜:通透 85%, 对比度:+20")
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "通透",
                    filterIntensity = 85,
                    softLight = "无",
                    toneCurve = 10,
                    saturation = 10,
                    warmCool = -15,
                    cyanMagenta = 5,
                    sharpness = 10,
                    vignette = true
                ),
                deviceModel = "Find X9spro",
                author = "@小陈工",
                source = "omaster_cloud",
                isFavorite = false,
                isNew = true,
                category = "电影",
                difficulty = "进阶",
                tags = listOf("电影", "宽幅", "质感"),
                description = "电影质感风格",
                style = "纪实",
                scene = "电影"
            ),
            
            // 生活风格
            Preset(
                id = "coffee_mood",
                name = "咖啡时光",
                coverPath = "https://picsum.photos/seed/coffee/400/600",
                sections = listOf(
                    Section("色彩调校", "滤镜:复古 85%, 暖色调:+30")
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "复古",
                    filterIntensity = 85,
                    softLight = "无",
                    toneCurve = -5,
                    saturation = 15,
                    warmCool = 30,
                    sharpness = 5,
                    vignette = true
                ),
                deviceModel = "Find X9spro",
                author = "@小陈工",
                source = "omaster_cloud",
                isFavorite = false,
                isNew = false,
                category = "生活",
                difficulty = "简单",
                tags = listOf("咖啡", "文艺", "复古"),
                description = "文艺咖啡风格",
                style = "复古",
                scene = "生活"
            )
        )
    }
    
    /**
     * 扩展预设数据
     * Search-004: 大量预设 (100+ 用于性能测试)
     */
    private fun getExtendedPresets(): List<Preset> {
        val presets = mutableListOf<Preset>()
        val styles = listOf("胶片", "复古", "清新", "蓝调", "黑白", "梦幻", "霓虹", "纪实")
        val scenes = listOf("人像", "风景", "街拍", "夜景", "美食", "建筑", "生活", "电影")
        val baseNames = listOf(
            "春日樱花", "秋日枫叶", "雪景纯净", "海天一色", "街拍故事",
            "夕阳暖调", "黑白情绪", "文艺日常", "城市夜景", "自然纪实",
            "金色黄昏", "晨曦微光", "午后阳光", "月夜温柔", "都市霓虹",
            "森林秘境", "海边漫步", "城市印象", "人间烟火", "岁月静好"
        )
        
        repeat(100) { index ->
            val style = styles[index % styles.size]
            val scene = scenes[index % scenes.size]
            val baseName = baseNames[index % baseNames.size]
            
            presets.add(
                Preset(
                    id = "extended_preset_$index",
                    name = "$baseName ${index + 1}",
                    coverPath = "https://picsum.photos/seed/ext$index/400/600",
                    sections = listOf(
                        Section("色彩调校", "滤镜:${if (style == "胶片") "复古" else "标准"} ${70 + index % 30}%")
                    ),
                    cameraParams = CameraParams(
                        mode = "master",
                        filter = if (style == "胶片") "复古" else "标准",
                        filterIntensity = 70 + index % 30,
                        softLight = if (index % 3 == 0) "无" else if (index % 3 == 1) "柔美" else "梦幻",
                        toneCurve = index % 40 - 20,
                        saturation = index % 30,
                        warmCool = index % 60 - 30,
                        sharpness = index % 15,
                        vignette = index % 2 == 0
                    ),
                    deviceModel = if (index % 2 == 0) "Find X9spro" else "GT 6",
                    author = "@摄影师${index + 1}",
                    source = if (index % 3 == 0) "omaster_cloud" else "community",
                    isFavorite = index % 5 == 0,
                    isNew = index % 7 == 0,
                    category = style,
                    difficulty = if (index % 3 == 0) "简单" else if (index % 3 == 1) "中等" else "进阶",
                    tags = listOf(style, scene, "预设${index + 1}"),
                    description = "这是一个示例预设，编号 $index，风格:$style，场景:$scene",
                    style = style,
                    scene = scene
                )
            )
        }
        
        return presets
    }
}
