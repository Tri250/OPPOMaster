package com.omaster.app.model

object PresetDatabase {
    
    private val presets = listOf(
        // ==================== 哈苏影像系列（OPPO/一加） ====================
        Preset(
            id = "hasselblad_001",
            name = "哈苏经典",
            coverPath = "hasselblad_classic",
            sections = listOf(
                Section("风格特点", "复刻哈苏中画幅胶片的经典色调，色彩准确，层次丰富"),
                Section("适用场景", "人像、风光、建筑、美食等各类场景")
            ),
            cameraParams = CameraParams(
                hasselblad_hncs = true,
                saturation = 10,
                contrast = 12,
                clarity = 8
            ),
            deviceModel = "OPPO Find X7 Ultra",
            source = "omaster_official"
        ),
        Preset(
            id = "hasselblad_002",
            name = "哈苏人像",
            coverPath = "hasselblad_portrait",
            sections = listOf(
                Section("风格特点", "专为哈苏影像优化的人像模式，肤色准确，立体感强"),
                Section("适用场景", "人像、合影、婚礼、儿童摄影")
            ),
            cameraParams = CameraParams(
                hasselblad_hncs = true,
                master_hdr = "智能",
                brightness = 5,
                saturation = 8,
                clarity = 6
            ),
            deviceModel = "OPPO Find X7 Ultra",
            source = "omaster_official"
        ),
        Preset(
            id = "hasselblad_003",
            name = "哈苏风光",
            coverPath = "hasselblad_landscape",
            sections = listOf(
                Section("风格特点", "增强风光摄影的色彩和层次，蓝天更蓝，绿植更翠"),
                Section("适用场景", "自然风光、城市风景、建筑摄影")
            ),
            cameraParams = CameraParams(
                hasselblad_hncs = true,
                hdr = true,
                saturation = 15,
                contrast = 12,
                clarity = 10
            ),
            deviceModel = "OPPO Find X6 Pro",
            source = "omaster_official"
        ),
        Preset(
            id = "hasselblad_004",
            name = "哈苏夜景",
            coverPath = "hasselblad_night",
            sections = listOf(
                Section("风格特点", "优化暗光环境下的细节和纯净度"),
                Section("适用场景", "夜景、星空、霓虹灯")
            ),
            cameraParams = CameraParams(
                hasselblad_hncs = true,
                ai_scene = true,
                brightness = -5,
                contrast = 15,
                saturation = 12
            ),
            deviceModel = "OPPO Find X7",
            source = "omaster_official"
        ),
        Preset(
            id = "hasselblad_005",
            name = "哈苏大师",
            coverPath = "hasselblad_master",
            sections = listOf(
                Section("风格特点", "综合哈苏影像优势，适合各类场景的万能预设"),
                Section("适用场景", "日常记录、旅行摄影、创作拍摄")
            ),
            cameraParams = CameraParams(
                hasselblad_hncs = true,
                master_hdr = "智能",
                ai_scene = true,
                saturation = 10,
                contrast = 10,
                clarity = 8
            ),
            deviceModel = "OnePlus 12",
            source = "omaster_official"
        ),
        
        // ==================== OPPO Find系列专属 ====================
        Preset(
            id = "findx7_001",
            name = "Find X7 杜绝对比",
            coverPath = "findx7_contrast",
            sections = listOf(
                Section("风格特点", "OPPO Find X7 Ultra专属，强劲对比，层次分明"),
                Section("适用场景", "艺术创作、黑白摄影、戏剧性表达")
            ),
            cameraParams = CameraParams(
                contrast = 20,
                saturation = 5,
                clarity = 15,
                hasselblad_hncs = true
            ),
            deviceModel = "OPPO Find X7 Ultra",
            source = "omaster_official"
        ),
        Preset(
            id = "findx7_002",
            name = "Find X7 哈苏HNCS",
            coverPath = "findx7_hncs",
            sections = listOf(
                Section("风格特点", "充分发挥哈苏自然色彩解决方案的优势"),
                Section("适用场景", "专业摄影、商业拍摄、色彩还原")
            ),
            cameraParams = CameraParams(
                hasselblad_hncs = true,
                master_hdr = "智能",
                saturation = 12,
                contrast = 8,
                clarity = 6
            ),
            deviceModel = "OPPO Find X7 Ultra",
            source = "omaster_official"
        ),
        Preset(
            id = "findx6_001",
            name = "Find X6 夜间人像",
            coverPath = "findx6_night_portrait",
            sections = listOf(
                Section("风格特点", "Find X6系列专属夜间人像优化，肤色自然"),
                Section("适用场景", "夜景人像、晚会、演唱会")
            ),
            cameraParams = CameraParams(
                hasselblad_hncs = true,
                ai_scene = true,
                brightness = 3,
                saturation = 8,
                clarity = 5
            ),
            deviceModel = "OPPO Find X6 Pro",
            source = "omaster_official"
        ),
        Preset(
            id = "reno_001",
            name = "Reno人像专家",
            coverPath = "reno_portrait",
            sections = listOf(
                Section("风格特点", "Reno系列专属人像优化，美颜自然不失真"),
                Section("适用场景", "人像自拍、写真、合照")
            ),
            cameraParams = CameraParams(
                hasselblad_hncs = true,
                brightness = 8,
                saturation = 10,
                clarity = 5
            ),
            deviceModel = "OPPO Reno12 Pro",
            source = "omaster_official"
        ),
        
        // ==================== 一加哈苏系列 ====================
        Preset(
            id = "oneplus_001",
            name = "一加哈苏XPan",
            coverPath = "oneplus_xpan",
            sections = listOf(
                Section("风格特点", "复刻一加与哈苏合作的XPan模式电影感"),
                Section("适用场景", "街头摄影、风光、人文纪实")
            ),
            cameraParams = CameraParams(
                hasselblad_hncs = true,
                contrast = 15,
                saturation = 8,
                warmth = 5,
                clarity = 10
            ),
            deviceModel = "OnePlus 12",
            source = "omaster_official"
        ),
        Preset(
            id = "oneplus_002",
            name = "一加影像大师",
            coverPath = "oneplus_master",
            sections = listOf(
                Section("风格特点", "综合一加12系列的影像优势"),
                Section("适用场景", "日常摄影、旅行记录")
            ),
            cameraParams = CameraParams(
                hasselblad_hncs = true,
                master_hdr = "智能",
                ai_scene = true,
                saturation = 10,
                contrast = 10,
                clarity = 8
            ),
            deviceModel = "OnePlus 12",
            source = "omaster_official"
        ),
        Preset(
            id = "oneplus_003",
            name = "一加人像影调",
            coverPath = "oneplus_portrait_tone",
            sections = listOf(
                Section("风格特点", "一加系列专属人像影调，立体感强"),
                Section("适用场景", "人像、合影、婚礼")
            ),
            cameraParams = CameraParams(
                hasselblad_hncs = true,
                brightness = 5,
                saturation = 12,
                contrast = 8,
                clarity = 6
            ),
            deviceModel = "OnePlus 11",
            source = "omaster_official"
        ),
        
        // ==================== 徕卡影像系列（小米） ====================
        Preset(
            id = "leica_001",
            name = "徕卡经典",
            coverPath = "leica_classic",
            sections = listOf(
                Section("风格特点", "复刻徕卡M系列经典色调，浓郁德味"),
                Section("适用场景", "人文纪实、街头摄影、黑白创作")
            ),
            cameraParams = CameraParams(
                saturation = 8,
                contrast = 15,
                clarity = 10,
                warmth = 8
            ),
            deviceModel = "Xiaomi 14 Ultra",
            source = "omaster_official"
        ),
        Preset(
            id = "leica_002",
            name = "徕卡生动",
            coverPath = "leica_vivid",
            sections = listOf(
                Section("风格特点", "徕卡生动模式，增强色彩表现力"),
                Section("适用场景", "风光、美食、色彩丰富的场景")
            ),
            cameraParams = CameraParams(
                master_hdr = "智能",
                saturation = 18,
                contrast = 12,
                clarity = 10
            ),
            deviceModel = "Xiaomi 14 Pro",
            source = "omaster_official"
        ),
        Preset(
            id = "leica_003",
            name = "徕卡黑白",
            coverPath = "leica_bw",
            sections = listOf(
                Section("风格特点", "纯正徕卡黑白影调，灰阶丰富"),
                Section("适用场景", "人像、建筑、纪实摄影")
            ),
            cameraParams = CameraParams(
                blackWhite = true,
                contrast = 18,
                clarity = 12
            ),
            deviceModel = "Xiaomi 14 Ultra",
            source = "omaster_official"
        ),
        Preset(
            id = "leica_004",
            name = "徕卡自然",
            coverPath = "leica_natural",
            sections = listOf(
                Section("风格特点", "徕卡自然模式，色彩准确还原"),
                Section("适用场景", "产品摄影、商业拍摄")
            ),
            cameraParams = CameraParams(
                saturation = 5,
                contrast = 8,
                clarity = 6
            ),
            deviceModel = "Xiaomi 14",
            source = "omaster_official"
        ),
        
        // ==================== 蔡司影像系列（vivo） ====================
        Preset(
            id = "zeiss_001",
            name = "蔡司经典",
            coverPath = "zeiss_classic",
            sections = listOf(
                Section("风格特点", "蔡司T*镀膜特有的清透画质"),
                Section("适用场景", "风光、人像、建筑")
            ),
            cameraParams = CameraParams(
                saturation = 12,
                contrast = 10,
                clarity = 10
            ),
            deviceModel = "vivo X100 Ultra",
            source = "omaster_official"
        ),
        Preset(
            id = "zeiss_002",
            name = "蔡司Biotar",
            coverPath = "zeiss_biotar",
            sections = listOf(
                Section("风格特点", "复刻蔡司Biotar镜头特有的旋转散景"),
                Section("适用场景", "人像、创意摄影")
            ),
            cameraParams = CameraParams(
                saturation = 10,
                contrast = 12,
                clarity = 5,
                vignette = 15
            ),
            deviceModel = "vivo X100 Pro",
            source = "omaster_official"
        ),
        Preset(
            id = "zeiss_003",
            name = "蔡司Planar",
            coverPath = "zeiss_planar",
            sections = listOf(
                Section("风格特点", "蔡司Planar镜头的锐利中心"),
                Section("适用场景", "产品摄影、人像特写")
            ),
            cameraParams = CameraParams(
                contrast = 15,
                clarity = 15,
                saturation = 8
            ),
            deviceModel = "vivo X100",
            source = "omaster_official"
        ),
        
        // ==================== XMAGE影像系列（华为） ====================
        Preset(
            id = "xm_001",
            name = "XMAGE原色",
            coverPath = "xm_original",
            sections = listOf(
                Section("风格特点", "华为XMAGE原色引擎，色彩精准"),
                Section("适用场景", "风光、人像、美食")
            ),
            cameraParams = CameraParams(
                ai_scene = true,
                saturation = 12,
                contrast = 10,
                clarity = 8
            ),
            deviceModel = "Mate 60 Pro+",
            source = "omaster_official"
        ),
        Preset(
            id = "xm_002",
            name = "XMAGE明快",
            coverPath = "xm_vivid",
            sections = listOf(
                Section("风格特点", "华为XMAGE明快模式，色彩鲜活"),
                Section("适用场景", "风光、美食、旅行")
            ),
            cameraParams = CameraParams(
                ai_scene = true,
                master_hdr = "智能",
                saturation = 18,
                contrast = 12,
                clarity = 10
            ),
            deviceModel = "P60 Pro",
            source = "omaster_official"
        ),
        Preset(
            id = "xm_003",
            name = "XMAGE质感",
            coverPath = "xm_texture",
            sections = listOf(
                Section("风格特点", "华为XMAGE质感模式，层次丰富"),
                Section("适用场景", "人文、建筑、纪实")
            ),
            cameraParams = CameraParams(
                ai_scene = true,
                contrast = 18,
                clarity = 12,
                saturation = 8
            ),
            deviceModel = "Mate 60 Pro",
            source = "omaster_official"
        ),
        
        // ==================== 通用预设 ====================
        Preset(
            id = "general_001",
            name = "清新自然",
            coverPath = "fresh_natural",
            sections = listOf(
                Section("风格特点", "小清新风格，适合日常生活记录"),
                Section("适用场景", "日常、美食、风光、旅行")
            ),
            cameraParams = CameraParams(
                brightness = 8,
                saturation = 12,
                contrast = 5,
                warmth = 5
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_002",
            name = "胶片质感",
            coverPath = "film_texture",
            sections = listOf(
                Section("风格特点", "复刻经典胶片色调，有颗粒感"),
                Section("适用场景", "人像、街头、纪实")
            ),
            cameraParams = CameraParams(
                saturation = 5,
                contrast = 12,
                warmth = 10,
                grain = true
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_003",
            name = "电影感",
            coverPath = "cinematic",
            sections = listOf(
                Section("风格特点", "好莱坞电影色调，宽幅比例"),
                Section("适用场景", "创意摄影、短视频")
            ),
            cameraParams = CameraParams(
                contrast = 18,
                saturation = 8,
                brightness = -3,
                clarity = 10
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_004",
            name = "美食专家",
            coverPath = "food_expert",
            sections = listOf(
                Section("风格特点", "增强食物的色彩和食欲感"),
                Section("适用场景", "美食摄影、餐厅拍摄")
            ),
            cameraParams = CameraParams(
                saturation = 20,
                warmth = 8,
                brightness = 5,
                clarity = 10
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_005",
            name = "夜景大师",
            coverPath = "night_master",
            sections = listOf(
                Section("风格特点", "增强夜间摄影的纯净度和细节"),
                Section("适用场景", "夜景、星空、霓虹灯")
            ),
            cameraParams = CameraParams(
                brightness = -8,
                contrast = 18,
                saturation = 15,
                clarity = 12
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_006",
            name = "复古怀旧",
            coverPath = "retro_vintage",
            sections = listOf(
                Section("风格特点", "80年代复古色调，暖黄滤镜"),
                Section("适用场景", "人像、风景、创意摄影")
            ),
            cameraParams = CameraParams(
                saturation = 15,
                warmth = 20,
                contrast = 10,
                vignette = 15
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_007",
            name = "黑白经典",
            coverPath = "bw_classic",
            sections = listOf(
                Section("风格特点", "纯正黑白摄影，高对比"),
                Section("适用场景", "人像、建筑、纪实")
            ),
            cameraParams = CameraParams(
                blackWhite = true,
                contrast = 20,
                clarity = 10
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_008",
            name = "赛博朋克",
            coverPath = "cyberpunk",
            sections = listOf(
                Section("风格特点", "科技感霓虹色调，高饱和"),
                Section("适用场景", "创意摄影、夜景")
            ),
            cameraParams = CameraParams(
                saturation = 25,
                contrast = 20,
                hue = 15,
                brightness = -5
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_009",
            name = "极简主义",
            coverPath = "minimal",
            sections = listOf(
                Section("风格特点", "简约冷淡风格，低饱和"),
                Section("适用场景", "建筑、产品、空间摄影")
            ),
            cameraParams = CameraParams(
                saturation = 3,
                contrast = 8,
                brightness = 10,
                clarity = 5
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_010",
            name = "日系治愈",
            coverPath = "japanese",
            sections = listOf(
                Section("风格特点", "日系治愈系风格，柔和光线"),
                Section("适用场景", "人像、生活记录、静物")
            ),
            cameraParams = CameraParams(
                brightness = 12,
                saturation = 8,
                contrast = -3,
                warmth = 5
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_011",
            name = "韩系奶油",
            coverPath = "korean_cream",
            sections = listOf(
                Section("风格特点", "韩系奶油肌色调，柔和通透"),
                Section("适用场景", "人像自拍、合照")
            ),
            cameraParams = CameraParams(
                brightness = 15,
                saturation = 5,
                contrast = -5,
                clarity = -3
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_012",
            name = "油画质感",
            coverPath = "oil_painting",
            sections = listOf(
                Section("风格特点", "模拟油画质感，柔和过渡"),
                Section("适用场景", "人像、风景、艺术创作")
            ),
            cameraParams = CameraParams(
                saturation = 8,
                contrast = 5,
                clarity = 15,
                brightness = 3
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_013",
            name = "蓝调忧郁",
            coverPath = "blue_mood",
            sections = listOf(
                Section("风格特点", "蓝色调忧郁氛围"),
                Section("适用场景", "人像、风景、情绪表达")
            ),
            cameraParams = CameraParams(
                saturation = 5,
                contrast = 12,
                warmth = -15,
                tint = 5
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_014",
            name = "暖阳午后",
            coverPath = "warm_sun",
            sections = listOf(
                Section("风格特点", "午后阳光的温暖色调"),
                Section("适用场景", "人像、生活、美食")
            ),
            cameraParams = CameraParams(
                saturation = 15,
                warmth = 18,
                brightness = 8,
                contrast = 5
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_015",
            name = "少女粉",
            coverPath = "girl_pink",
            sections = listOf(
                Section("风格特点", "少女感粉色系滤镜"),
                Section("适用场景", "人像、自拍、美食")
            ),
            cameraParams = CameraParams(
                saturation = 10,
                warmth = 10,
                tint = 8,
                brightness = 5
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_016",
            name = "商务专业",
            coverPath = "business_pro",
            sections = listOf(
                Section("风格特点", "专业商务风格，色彩准确"),
                Section("适用场景", "产品摄影、文档扫描")
            ),
            cameraParams = CameraParams(
                saturation = 0,
                contrast = 15,
                clarity = 15
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_017",
            name = "夕阳剪影",
            coverPath = "sunset_silhouette",
            sections = listOf(
                Section("风格特点", "夕阳剪影效果，强烈对比"),
                Section("适用场景", "人像、风景")
            ),
            cameraParams = CameraParams(
                brightness = -10,
                contrast = 25,
                saturation = 18,
                warmth = 20
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_018",
            name = "雾霾灰",
            coverPath = "haze_gray",
            sections = listOf(
                Section("风格特点", "高级灰调，雾霾感"),
                Section("适用场景", "人像、建筑、时尚")
            ),
            cameraParams = CameraParams(
                saturation = -10,
                contrast = 5,
                brightness = 8,
                clarity = 3
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_019",
            name = "糖果甜心",
            coverPath = "candy_sweet",
            sections = listOf(
                Section("风格特点", "糖果色系甜美滤镜"),
                Section("适用场景", "人像、自拍、美食")
            ),
            cameraParams = CameraParams(
                saturation = 20,
                brightness = 10,
                contrast = 5,
                warmth = 5
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_020",
            name = "冬日暖阳",
            coverPath = "winter_warm",
            sections = listOf(
                Section("风格特点", "冬日阳光的温暖质感"),
                Section("适用场景", "人像、风景、雪景")
            ),
            cameraParams = CameraParams(
                saturation = 12,
                warmth = 15,
                brightness = 10,
                contrast = 3
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_021",
            name = "莫兰迪色",
            coverPath = "morandi",
            sections = listOf(
                Section("风格特点", "莫兰迪色系高级灰调"),
                Section("适用场景", "人像、产品、空间")
            ),
            cameraParams = CameraParams(
                saturation = -15,
                contrast = 5,
                brightness = 5,
                warmth = 3
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_022",
            name = "港风复古",
            coverPath = "hongkong_retro",
            sections = listOf(
                Section("风格特点", "90年代港风色调"),
                Section("适用场景", "人像、街头、纪实")
            ),
            cameraParams = CameraParams(
                saturation = 8,
                contrast = 15,
                warmth = 12,
                clarity = 8
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_023",
            name = "法式浪漫",
            coverPath = "french_romantic",
            sections = listOf(
                Section("风格特点", "法式浪漫色调，优雅复古"),
                Section("适用场景", "人像、旅行、美食")
            ),
            cameraParams = CameraParams(
                saturation = 10,
                warmth = 12,
                brightness = 5,
                contrast = 5
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_024",
            name = "北欧冷淡",
            coverPath = "nordic_cool",
            sections = listOf(
                Section("风格特点", "北欧冷淡风格，高级灰"),
                Section("适用场景", "建筑、空间、产品")
            ),
            cameraParams = CameraParams(
                saturation = -20,
                contrast = 8,
                brightness = 8,
                clarity = 5
            ),
            deviceModel = "",
            source = "omaster_official"
        ),
        Preset(
            id = "general_025",
            name = "自然风光",
            coverPath = "natural_landscape",
            sections = listOf(
                Section("风格特点", "增强自然风光的色彩和层次"),
                Section("适用场景", "风光、风景、旅行")
            ),
            cameraParams = CameraParams(
                hdr = true,
                saturation = 15,
                contrast = 12,
                clarity = 10
            ),
            deviceModel = "",
            source = "omaster_official"
        )
    )
    
    fun getAllPresets(): List<Preset> = presets
    
    fun getPresetsByDevice(deviceModel: String): List<Preset> {
        if (deviceModel.isEmpty()) return presets
        return presets.filter { 
            it.deviceModel.contains(deviceModel, ignoreCase = true) || 
            it.deviceModel.isEmpty()
        }
    }
    
    fun getPresetsBySource(source: String): List<Preset> {
        return presets.filter { it.source == source }
    }
    
    fun searchPresets(query: String): List<Preset> {
        return presets.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.sections.any { section ->
                section.title.contains(query, ignoreCase = true) ||
                section.content.contains(query, ignoreCase = true)
            } ||
            it.deviceModel.contains(query, ignoreCase = true)
        }
    }
    
    fun getPresetCount(): Int = presets.size
    
    fun getPresetById(id: String): Preset? {
        return presets.find { it.id == id }
    }
}
