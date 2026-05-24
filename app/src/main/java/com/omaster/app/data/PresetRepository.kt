package com.omaster.app.data

import com.omaster.app.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PresetRepository {
    private val _presets = MutableStateFlow<List<Preset>>(emptyList())
    val presets: StateFlow<List<Preset>> = _presets

    private val _selectedPreset = MutableStateFlow<Preset?>(null)
    val selectedPreset: StateFlow<Preset?> = _selectedPreset

    init {
        loadOfficialPresets()
    }

    private fun loadOfficialPresets() {
        _presets.value = listOf(
            Preset(
                id = "1",
                name = "德味预设",
                coverPath = "https://cdn.fky.ltd/dw_01.webp",
                galleryImages = listOf("https://cdn.fky.ltd/dw_02.webp", "https://cdn.fky.ltd/dw_03.webp"),
                author = "@波子Booz",
                isNew = true,
                sections = listOf(
                    Section(
                        title = "色彩调校",
                        items = listOf(
                            SectionItem("滤镜", "明艳 100%", 2),
                            SectionItem("柔光灯", "无", 1),
                            SectionItem("色调曲线", "-35", 1),
                            SectionItem("饱和度", "0", 1),
                            SectionItem("冷暖", "-5", 1),
                            SectionItem("青品", "4", 1),
                            SectionItem("锐度", "10", 1),
                            SectionItem("暗角", "开", 2)
                        )
                    )
                ),
                tags = listOf("Auto"),
                description = Description(
                    title = "Shooting Tips",
                    content = "【环境建议】日间户外或光线充足的室内\n【场景推荐】街拍、建筑、风景、人文\n【拍摄要点】德味风格，影调偏暗，色彩浓郁，适合追求经典德系胶片质感的摄影爱好者"
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "明艳",
                    iso = 100,
                    shutter = "1/125",
                    ev = "0",
                    wb = "自动",
                    hasselblad_hncs = true
                ),
                applicableScenes = listOf(SceneType.STREET, SceneType.ARCHITECTURE, SceneType.LANDSCAPE),
                source = "OMaster官方"
            ),
            Preset(
                id = "2",
                name = "富士胶片",
                coverPath = "https://picsum.photos/seed/fujifilm/800/600",
                galleryImages = listOf(),
                author = "@OPPO影像",
                isNew = false,
                sections = listOf(
                    Section(
                        title = "色彩调校",
                        items = listOf(
                            SectionItem("滤镜", "复古 100%", 2),
                            SectionItem("柔光灯", "无", 1),
                            SectionItem("色调曲线", "0", 1),
                            SectionItem("饱和度", "+19", 1),
                            SectionItem("冷暖", "-5", 1),
                            SectionItem("青品", "0", 1),
                            SectionItem("锐度", "15", 1),
                            SectionItem("暗角", "开", 2)
                        )
                    )
                ),
                tags = listOf("Auto"),
                description = Description(
                    title = "Shooting Tips",
                    content = "【环境建议】日间户外或光线充足的室内\n【场景推荐】街拍、人像、风景、建筑\n【拍摄要点】适合追求经典胶片质感的场景，色彩浓郁复古，建议寻找有光影对比的场景增强层次感"
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "复古",
                    iso = 200,
                    shutter = "1/200",
                    ev = "0",
                    wb = "自动",
                    hasselblad_hncs = false
                ),
                applicableScenes = listOf(SceneType.STREET, SceneType.PORTRAIT, SceneType.LANDSCAPE),
                source = "OMaster官方"
            ),
            Preset(
                id = "3",
                name = "胶片感",
                coverPath = "https://picsum.photos/seed/film/800/600",
                galleryImages = listOf(),
                author = "@OPPO影像",
                isNew = false,
                sections = listOf(
                    Section(
                        title = "色彩调校",
                        items = listOf(
                            SectionItem("滤镜", "复古 75%", 2),
                            SectionItem("柔光灯", "柔美", 1),
                            SectionItem("色调曲线", "-5", 1),
                            SectionItem("饱和度", "+20", 1),
                            SectionItem("冷暖", "-3", 1),
                            SectionItem("青品", "+4", 1),
                            SectionItem("锐度", "7", 1),
                            SectionItem("暗角", "开", 2)
                        )
                    )
                ),
                tags = listOf("Auto"),
                description = Description(
                    title = "Shooting Tips",
                    content = "【环境建议】自然光或柔和人工光源\n【场景推荐】人像写真、静物、咖啡馆、文艺场景\n【拍摄要点】柔光效果营造梦幻氛围，适合拍摄情绪感照片，建议对焦主体保持清晰"
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "复古",
                    iso = 100,
                    shutter = "1/160",
                    ev = "0",
                    wb = "5200K",
                    hasselblad_hncs = true
                ),
                applicableScenes = listOf(SceneType.PORTRAIT, SceneType.MACRO),
                source = "OMaster官方"
            ),
            Preset(
                id = "4",
                name = "童话",
                coverPath = "https://picsum.photos/seed/fairy/800/600",
                galleryImages = listOf(),
                author = "@OPPO影像",
                isNew = false,
                sections = listOf(
                    Section(
                        title = "色彩调校",
                        items = listOf(
                            SectionItem("滤镜", "童话 73%", 2),
                            SectionItem("柔光灯", "梦幻", 1),
                            SectionItem("色调曲线", "-24", 1),
                            SectionItem("饱和度", "+12", 1),
                            SectionItem("冷暖", "+3", 1),
                            SectionItem("青品", "+7", 1),
                            SectionItem("锐度", "0", 1),
                            SectionItem("暗角", "开", 2)
                        )
                    )
                ),
                tags = listOf("Auto"),
                description = Description(
                    title = "Shooting Tips",
                    content = "【环境建议】清晨、黄昏或阴天散射光\n【场景推荐】儿童摄影、花园、公园、浪漫场景\n【拍摄要点】影调偏暗营造神秘感，梦幻柔光适合营造童话氛围，建议寻找色彩丰富的场景"
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "童话",
                    iso = 100,
                    shutter = "1/125",
                    ev = "+0.3",
                    wb = "5500K",
                    hasselblad_hncs = true
                ),
                applicableScenes = listOf(SceneType.PORTRAIT, SceneType.NATURE),
                source = "OMaster官方"
            ),
            Preset(
                id = "5",
                name = "高对比黑白",
                coverPath = "https://picsum.photos/seed/bw/800/600",
                galleryImages = listOf(),
                author = "@OPPO影像",
                isNew = false,
                sections = listOf(
                    Section(
                        title = "色彩调校",
                        items = listOf(
                            SectionItem("滤镜", "黑白 100%", 2),
                            SectionItem("柔光灯", "柔美", 1),
                            SectionItem("色调曲线", "-61", 1),
                            SectionItem("饱和度", "0", 1),
                            SectionItem("冷暖", "+100", 1),
                            SectionItem("青品", "-39", 1),
                            SectionItem("锐度", "0", 1),
                            SectionItem("暗角", "关", 2)
                        )
                    )
                ),
                tags = listOf("Auto"),
                description = Description(
                    title = "Shooting Tips",
                    content = "【环境建议】强烈光影对比场景，如阳光直射或聚光灯\n【场景推荐】建筑、纪实摄影、街头、艺术人像\n【拍摄要点】利用明暗对比突出主体轮廓，适合几何线条和纹理丰富的场景，注意构图简洁有力"
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "黑白",
                    iso = 200,
                    shutter = "1/250",
                    ev = "0",
                    wb = "自动",
                    hasselblad_hncs = false
                ),
                applicableScenes = listOf(SceneType.ARCHITECTURE, SceneType.STREET),
                source = "OMaster官方"
            ),
            Preset(
                id = "6",
                name = "理光绿",
                coverPath = "https://picsum.photos/seed/green/800/600",
                galleryImages = listOf(),
                author = "@OPPO影像",
                isNew = false,
                sections = listOf(
                    Section(
                        title = "色彩调校",
                        items = listOf(
                            SectionItem("滤镜", "清新 100%", 2),
                            SectionItem("柔光灯", "梦幻", 1),
                            SectionItem("色调曲线", "+39", 1),
                            SectionItem("饱和度", "+12", 1),
                            SectionItem("冷暖", "-2", 1),
                            SectionItem("青品", "-9", 1),
                            SectionItem("锐度", "10", 1),
                            SectionItem("暗角", "开", 2)
                        )
                    )
                ),
                tags = listOf("Auto"),
                description = Description(
                    title = "Shooting Tips",
                    content = "【环境建议】户外自然光，森林、草地、植物丰富的场景\n【场景推荐】植物摄影、森林漫步、春日户外、清新人像\n【拍摄要点】影调偏亮突出清新感，绿色表现自然通透，适合拍摄植物和户外自然场景"
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "清新",
                    iso = 100,
                    shutter = "1/500",
                    ev = "0",
                    wb = "5000K",
                    hasselblad_hncs = true
                ),
                applicableScenes = listOf(SceneType.NATURE, SceneType.LANDSCAPE, SceneType.MACRO),
                source = "OMaster官方"
            ),
            Preset(
                id = "7",
                name = "理光蓝",
                coverPath = "https://picsum.photos/seed/blue/800/600",
                galleryImages = listOf(),
                author = "@OPPO影像",
                isNew = false,
                sections = listOf(
                    Section(
                        title = "色彩调校",
                        items = listOf(
                            SectionItem("滤镜", "通透 100%", 2),
                            SectionItem("柔光灯", "柔美", 1),
                            SectionItem("色调曲线", "+18", 1),
                            SectionItem("饱和度", "-2", 1),
                            SectionItem("冷暖", "-8", 1),
                            SectionItem("青品", "+19", 1),
                            SectionItem("锐度", "11", 1),
                            SectionItem("暗角", "开", 2)
                        )
                    )
                ),
                tags = listOf("Auto"),
                description = Description(
                    title = "Shooting Tips",
                    content = "【环境建议】晴朗天气或蓝天背景\n【场景推荐】海边、城市建筑、天空、冷色调场景\n【拍摄要点】偏冷色调增强蓝色表现力，适合拍摄天空、水面和城市建筑，营造通透冷静的氛围"
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "通透",
                    iso = 100,
                    shutter = "1/400",
                    ev = "0",
                    wb = "4800K",
                    hasselblad_hncs = true
                ),
                applicableScenes = listOf(SceneType.LANDSCAPE, SceneType.ARCHITECTURE),
                source = "OMaster官方"
            ),
            Preset(
                id = "8",
                name = "蓝调时刻",
                coverPath = "https://picsum.photos/seed/bluemoment/800/600",
                galleryImages = listOf(),
                author = "@OPPO影像",
                isNew = true,
                sections = listOf(
                    Section(
                        title = "色彩调校",
                        items = listOf(
                            SectionItem("滤镜", "复古 100%", 2),
                            SectionItem("柔光灯", "梦幻", 1),
                            SectionItem("色调曲线", "-5", 1),
                            SectionItem("饱和度", "+15", 1),
                            SectionItem("冷暖", "+47", 1),
                            SectionItem("青品", "+28", 1),
                            SectionItem("锐度", "12", 1),
                            SectionItem("暗角", "开", 2)
                        )
                    )
                ),
                tags = listOf("Auto"),
                description = Description(
                    title = "Shooting Tips",
                    content = "【环境建议】日出前或日落后20分钟的蓝调时刻\n【场景推荐】城市夜景、灯光璀璨的场景、水面倒影\n【拍摄要点】冷暖对比强烈，适合拍摄城市灯光和夜景，建议寻找有水面的场景增强倒影效果"
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "复古",
                    iso = 800,
                    shutter = "1/30",
                    ev = "-0.7",
                    wb = "4200K",
                    hasselblad_hncs = true
                ),
                applicableScenes = listOf(SceneType.NIGHT, SceneType.SUNSET),
                source = "OMaster官方"
            ),
            Preset(
                id = "9",
                name = "梦幻黑柔",
                coverPath = "https://picsum.photos/seed/dream/800/600",
                galleryImages = listOf(),
                author = "@OPPO影像",
                isNew = false,
                sections = listOf(
                    Section(
                        title = "色彩调校",
                        items = listOf(
                            SectionItem("滤镜", "标准 0%", 2),
                            SectionItem("柔光灯", "梦幻", 1),
                            SectionItem("色调曲线", "-25", 1),
                            SectionItem("饱和度", "+11", 1),
                            SectionItem("冷暖", "+30", 1),
                            SectionItem("青品", "-9", 1),
                            SectionItem("锐度", "0", 1),
                            SectionItem("暗角", "开", 2)
                        )
                    )
                ),
                tags = listOf("Auto"),
                description = Description(
                    title = "Shooting Tips",
                    content = "【环境建议】逆光或侧逆光场景\n【场景推荐】人像写真、情绪摄影、艺术场景、柔美人像\n【拍摄要点】黑柔滤镜效果营造梦幻氛围，适合拍摄唯美人像，建议利用逆光创造光晕效果"
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "标准",
                    iso = 100,
                    shutter = "1/200",
                    ev = "0",
                    wb = "5500K",
                    hasselblad_hncs = true
                ),
                applicableScenes = listOf(SceneType.PORTRAIT),
                source = "OMaster官方"
            ),
            Preset(
                id = "10",
                name = "哈苏 X2D | 慵懒午后的佛罗伦萨",
                coverPath = "https://picsum.photos/seed/florence/800/600",
                galleryImages = listOf(),
                author = "@哈苏影像",
                isNew = true,
                sections = listOf(
                    Section(
                        title = "光感设置",
                        items = listOf(
                            SectionItem("对比度", "降低", 1),
                            SectionItem("高光", "保留", 1)
                        )
                    ),
                    Section(
                        title = "色彩调校",
                        items = listOf(
                            SectionItem("色调", "暖偏", 1),
                            SectionItem("饱和度", "适中", 1)
                        )
                    )
                ),
                tags = listOf("HNCS"),
                description = Description(
                    title = "哈苏色彩科学",
                    content = "【环境建议】午后阳光，欧洲城市街道\n【场景推荐】人文街拍、城市风景\n【拍摄要点】哈苏自然色彩科学，还原真实而富有魅力的色彩表现"
                ),
                cameraParams = CameraParams(
                    mode = "master",
                    filter = "复古",
                    iso = 200,
                    shutter = "1/250",
                    ev = "+0.3",
                    wb = "5600K",
                    hasselblad_hncs = true
                ),
                applicableScenes = listOf(SceneType.STREET, SceneType.LANDSCAPE, SceneType.SUNSET),
                deviceModel = "Find X8 Pro",
                source = "OMaster官方"
            )
        )
    }

    fun toggleFavorite(presetId: String) {
        _presets.value = _presets.value.map { preset ->
            if (preset.id == presetId) {
                preset.copy(isFavorite = !preset.isFavorite)
            } else {
                preset
            }
        }
    }

    fun selectPreset(preset: Preset) {
        _selectedPreset.value = preset
    }

    fun getPresetById(id: String): Preset? {
        return _presets.value.find { it.id == id }
    }
}
