package com.omaster.app.model

enum class CameraType(val displayName: String, val partner: String) {
    HASSELBLAD("哈苏影像", "OPPO & 一加"),
    LEICA("徕卡影像", "小米"),
    ZEISS("蔡司影像", "vivo"),
    XMAGE("XMAGE影像", "华为"),
    STANDARD("标准影像", "通用")
}

data class DeviceMapping(
    val brand: String,
    val series: String,
    val model: String,
    val cameraType: CameraType,
    val recommendedParams: CameraParams,
    val supportedFeatures: List<String>,
    val releaseYear: Int,
    val flagship: Boolean = false
)

object DeviceDatabase {
    
    private val devices = listOf(
        // ==================== OPPO Find系列（哈苏影像） ====================
        DeviceMapping(
            brand = "OPPO",
            series = "Find",
            model = "Find X7 Ultra",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                hasselblad_hncs = true,
                master_hdr = "智能",
                ai_scene = true,
                hdr = true
            ),
            supportedFeatures = listOf("哈苏人像", "专业模式", "AI场景识别", "RAW格式", "10亿色"),
            releaseYear = 2024,
            flagship = true
        ),
        DeviceMapping(
            brand = "OPPO",
            series = "Find",
            model = "Find X7",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                hasselblad_hncs = true,
                master_hdr = "智能",
                ai_scene = true
            ),
            supportedFeatures = listOf("哈苏人像", "AI场景识别", "专业模式", "夜景模式"),
            releaseYear = 2024,
            flagship = true
        ),
        DeviceMapping(
            brand = "OPPO",
            series = "Find",
            model = "Find X6 Pro",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                hasselblad_hncs = true,
                master_hdr = "开启",
                ai_scene = true
            ),
            supportedFeatures = listOf("哈苏人像", "超光影潜望长焦", "夜景模式"),
            releaseYear = 2023,
            flagship = true
        ),
        DeviceMapping(
            brand = "OPPO",
            series = "Find",
            model = "Find X6",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                hasselblad_hncs = true,
                ai_scene = true
            ),
            supportedFeatures = listOf("哈苏人像", "AI场景识别", "夜景模式"),
            releaseYear = 2023,
            flagship = true
        ),
        DeviceMapping(
            brand = "OPPO",
            series = "Find",
            model = "Find N3",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                hasselblad_hncs = true,
                ai_scene = true
            ),
            supportedFeatures = listOf("哈苏人像", "折叠屏专用", "多角度悬停"),
            releaseYear = 2023,
            flagship = true
        ),
        DeviceMapping(
            brand = "OPPO",
            series = "Find",
            model = "Find N3 Flip",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                hasselblad_hncs = true,
                ai_scene = true
            ),
            supportedFeatures = listOf("哈苏人像", "竖向折叠", "外屏预览"),
            releaseYear = 2023,
            flagship = true
        ),
        DeviceMapping(
            brand = "OPPO",
            series = "Find",
            model = "Find N2",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                hasselblad_hncs = true
            ),
            supportedFeatures = listOf("哈苏人像", "折叠屏", "轻量化设计"),
            releaseYear = 2022,
            flagship = true
        ),
        DeviceMapping(
            brand = "OPPO",
            series = "Find",
            model = "Find N2 Flip",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                hasselblad_hncs = true,
                ai_scene = true
            ),
            supportedFeatures = listOf("哈苏人像", "竖向折叠", "大外屏"),
            releaseYear = 2022,
            flagship = true
        ),
        
        // ==================== OPPO Reno系列 ====================
        DeviceMapping(
            brand = "OPPO",
            series = "Reno",
            model = "Reno12 Pro",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                hasselblad_hncs = true,
                ai_scene = true
            ),
            supportedFeatures = listOf("AI人像专家", "AI场景识别", "专业模式"),
            releaseYear = 2024,
            flagship = false
        ),
        DeviceMapping(
            brand = "OPPO",
            series = "Reno",
            model = "Reno12",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("AI人像专家", "AI场景识别"),
            releaseYear = 2024,
            flagship = false
        ),
        DeviceMapping(
            brand = "OPPO",
            series = "Reno",
            model = "Reno11 Pro",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                hasselblad_hncs = true
            ),
            supportedFeatures = listOf("单反级人像", "AI场景识别"),
            releaseYear = 2023,
            flagship = false
        ),
        DeviceMapping(
            brand = "OPPO",
            series = "Reno",
            model = "Reno11",
            cameraType = CameraType.STANDARD,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("AI人像", "场景识别"),
            releaseYear = 2023,
            flagship = false
        ),
        DeviceMapping(
            brand = "OPPO",
            series = "Reno",
            model = "Reno10 Pro+",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                hasselblad_hncs = true
            ),
            supportedFeatures = listOf("超光影潜望长焦", "人像模式"),
            releaseYear = 2023,
            flagship = false
        ),
        DeviceMapping(
            brand = "OPPO",
            series = "Reno",
            model = "Reno10 Pro",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                hasselblad_hncs = true
            ),
            supportedFeatures = listOf("人像模式", "AI场景识别"),
            releaseYear = 2023,
            flagship = false
        ),
        DeviceMapping(
            brand = "OPPO",
            series = "Reno",
            model = "Reno10",
            cameraType = CameraType.STANDARD,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("AI场景识别", "人像模式"),
            releaseYear = 2023,
            flagship = false
        ),
        
        // ==================== OPPO K系列 ====================
        DeviceMapping(
            brand = "OPPO",
            series = "K",
            model = "K12",
            cameraType = CameraType.STANDARD,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("AI场景识别", "夜景模式"),
            releaseYear = 2024,
            flagship = false
        ),
        DeviceMapping(
            brand = "OPPO",
            series = "K",
            model = "K11",
            cameraType = CameraType.STANDARD,
            recommendedParams = CameraParams(
                ai_scene = true,
                hdr = true
            ),
            supportedFeatures = listOf("IMX890主摄", "AI场景识别", "夜景模式"),
            releaseYear = 2023,
            flagship = false
        ),
        DeviceMapping(
            brand = "OPPO",
            series = "K",
            model = "K10x",
            cameraType = CameraType.STANDARD,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("AI场景识别", "超级夜景"),
            releaseYear = 2022,
            flagship = false
        ),
        DeviceMapping(
            brand = "OPPO",
            series = "K",
            model = "K9x",
            cameraType = CameraType.STANDARD,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("AI场景识别", "夜景模式"),
            releaseYear = 2022,
            flagship = false
        ),
        
        // ==================== 一加系列（哈苏影像） ====================
        DeviceMapping(
            brand = "一加",
            series = "数字旗舰",
            model = "OnePlus 12",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                hasselblad_hncs = true,
                master_hdr = "智能",
                ai_scene = true,
                hdr = true
            ),
            supportedFeatures = listOf("哈苏影像", "潜望长焦", "AI场景识别", "专业模式"),
            releaseYear = 2023,
            flagship = true
        ),
        DeviceMapping(
            brand = "一加",
            series = "数字旗舰",
            model = "OnePlus 11",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                hasselblad_hncs = true,
                master_hdr = "开启",
                ai_scene = true
            ),
            supportedFeatures = listOf("哈苏影像", "二代骁龙8", "夜景模式"),
            releaseYear = 2023,
            flagship = true
        ),
        DeviceMapping(
            brand = "一加",
            series = "数字旗舰",
            model = "OnePlus 10 Pro",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                hasselblad_hncs = true,
                master_hdr = "智能",
                ai_scene = true
            ),
            supportedFeatures = listOf("哈苏影像2.0", "150°超广角", "大师影调"),
            releaseYear = 2022,
            flagship = true
        ),
        DeviceMapping(
            brand = "一加",
            series = "数字旗舰",
            model = "OnePlus 10T",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("极速对焦", "夜景模式", "AI场景识别"),
            releaseYear = 2022,
            flagship = false
        ),
        DeviceMapping(
            brand = "一加",
            series = "数字旗舰",
            model = "OnePlus 9 Pro",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                hasselblad_hncs = true,
                ai_scene = true
            ),
            supportedFeatures = listOf("哈苏影像", "超广角", "自由曲面镜头"),
            releaseYear = 2021,
            flagship = true
        ),
        DeviceMapping(
            brand = "一加",
            series = "数字旗舰",
            model = "OnePlus 9",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                hasselblad_hncs = true
            ),
            supportedFeatures = listOf("哈苏影像", "超广角镜头"),
            releaseYear = 2021,
            flagship = false
        ),
        DeviceMapping(
            brand = "一加",
            series = "Ace",
            model = "OnePlus Ace 3",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                hasselblad_hncs = true,
                ai_scene = true
            ),
            supportedFeatures = listOf("旗舰影像", "AI场景识别", "夜景模式"),
            releaseYear = 2024,
            flagship = false
        ),
        DeviceMapping(
            brand = "一加",
            series = "Ace",
            model = "OnePlus Ace 2 Pro",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("旗舰主摄", "AI场景优化"),
            releaseYear = 2023,
            flagship = false
        ),
        DeviceMapping(
            brand = "一加",
            series = "Ace",
            model = "OnePlus Ace 2",
            cameraType = CameraType.HASSELBLAD,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("旗舰影像算法", "超清画质"),
            releaseYear = 2023,
            flagship = false
        ),
        DeviceMapping(
            brand = "一加",
            series = "Nord",
            model = "OnePlus Nord 3",
            cameraType = CameraType.STANDARD,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("AI场景识别", "夜景模式"),
            releaseYear = 2023,
            flagship = false
        ),
        
        // ==================== realme系列 ====================
        DeviceMapping(
            brand = "realme",
            series = "GT",
            model = "realme GT5 Pro",
            cameraType = CameraType.STANDARD,
            recommendedParams = CameraParams(
                ai_scene = true,
                hdr = true
            ),
            supportedFeatures = listOf("IMX890大底", "AI场景识别", "夜景模式"),
            releaseYear = 2024,
            flagship = false
        ),
        DeviceMapping(
            brand = "realme",
            series = "GT",
            model = "realme GT5",
            cameraType = CameraType.STANDARD,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("AI场景识别", "144Hz刷新率"),
            releaseYear = 2023,
            flagship = false
        ),
        DeviceMapping(
            brand = "realme",
            series = "GT",
            model = "realme GT Neo5",
            cameraType = CameraType.STANDARD,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("AI场景优化", "超级夜景"),
            releaseYear = 2023,
            flagship = false
        ),
        DeviceMapping(
            brand = "realme",
            series = "数字",
            model = "realme 12 Pro+",
            cameraType = CameraType.STANDARD,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("旗舰长焦", "AI月亮模式", "街拍模式"),
            releaseYear = 2024,
            flagship = false
        ),
        DeviceMapping(
            brand = "realme",
            series = "数字",
            model = "realme 11 Pro+",
            cameraType = CameraType.STANDARD,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("2亿像素", "AI场景识别", "月亮模式"),
            releaseYear = 2023,
            flagship = false
        ),
        DeviceMapping(
            brand = "realme",
            series = "数字",
            model = "realme 10 Pro+",
            cameraType = CameraType.STANDARD,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("AI场景识别", "街拍模式"),
            releaseYear = 2022,
            flagship = false
        ),
        
        // ==================== 小米系列（徕卡影像） ====================
        DeviceMapping(
            brand = "小米",
            series = "数字旗舰",
            model = "Xiaomi 14 Ultra",
            cameraType = CameraType.LEICA,
            recommendedParams = CameraParams(
                master_hdr = "智能",
                ai_scene = true,
                hdr = true
            ),
            supportedFeatures = listOf("徕卡光学", "1英寸大底", "可变光圈", "专业模式"),
            releaseYear = 2024,
            flagship = true
        ),
        DeviceMapping(
            brand = "小米",
            series = "数字旗舰",
            model = "Xiaomi 14 Pro",
            cameraType = CameraType.LEICA,
            recommendedParams = CameraParams(
                master_hdr = "智能",
                ai_scene = true
            ),
            supportedFeatures = listOf("徕卡Summilux镜头", "1024级可变光圈"),
            releaseYear = 2023,
            flagship = true
        ),
        DeviceMapping(
            brand = "小米",
            series = "数字旗舰",
            model = "Xiaomi 14",
            cameraType = CameraType.LEICA,
            recommendedParams = CameraParams(
                master_hdr = "开启",
                ai_scene = true
            ),
            supportedFeatures = listOf("徕卡光学", "AI场景识别", "专业模式"),
            releaseYear = 2023,
            flagship = true
        ),
        DeviceMapping(
            brand = "小米",
            series = "Ultra",
            model = "Xiaomi 13 Ultra",
            cameraType = CameraType.LEICA,
            recommendedParams = CameraParams(
                master_hdr = "智能",
                ai_scene = true,
                hdr = true
            ),
            supportedFeatures = listOf("徕卡四摄", "1英寸可变光圈", "专业摄影"),
            releaseYear = 2023,
            flagship = true
        ),
        DeviceMapping(
            brand = "小米",
            series = "Pro",
            model = "Xiaomi 13 Pro",
            cameraType = CameraType.LEICA,
            recommendedParams = CameraParams(
                master_hdr = "开启",
                ai_scene = true
            ),
            supportedFeatures = listOf("徕卡光学", "1英寸大底", "夜景模式"),
            releaseYear = 2022,
            flagship = true
        ),
        DeviceMapping(
            brand = "小米",
            series = "数字旗舰",
            model = "Xiaomi 13",
            cameraType = CameraType.LEICA,
            recommendedParams = CameraParams(
                master_hdr = "智能",
                ai_scene = true
            ),
            supportedFeatures = listOf("徕卡影像", "AI场景识别", "徕卡滤镜"),
            releaseYear = 2022,
            flagship = true
        ),
        DeviceMapping(
            brand = "小米",
            series = "Redmi K",
            model = "Redmi K70 Pro",
            cameraType = CameraType.LEICA,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("光影猎人800", "AI场景识别", "夜景模式"),
            releaseYear = 2024,
            flagship = false
        ),
        DeviceMapping(
            brand = "小米",
            series = "Redmi K",
            model = "Redmi K70",
            cameraType = CameraType.LEICA,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("AI场景识别", "专业模式"),
            releaseYear = 2024,
            flagship = false
        ),
        DeviceMapping(
            brand = "小米",
            series = "Redmi Note",
            model = "Redmi Note 13 Pro+",
            cameraType = CameraType.STANDARD,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("2亿像素", "AI场景识别", "Smart-ISO Pro"),
            releaseYear = 2024,
            flagship = false
        ),
        
        // ==================== vivo系列（蔡司影像） ====================
        DeviceMapping(
            brand = "vivo",
            series = "X",
            model = "vivo X100 Ultra",
            cameraType = CameraType.ZEISS,
            recommendedParams = CameraParams(
                ai_scene = true,
                hdr = true
            ),
            supportedFeatures = listOf("蔡司光学", "2亿像素长焦", "AI场景识别", "人文街拍"),
            releaseYear = 2024,
            flagship = true
        ),
        DeviceMapping(
            brand = "vivo",
            series = "X",
            model = "vivo X100 Pro",
            cameraType = CameraType.ZEISS,
            recommendedParams = CameraParams(
                ai_scene = true,
                hdr = true
            ),
            supportedFeatures = listOf("蔡司APO长焦", "1英寸主摄", "T*镀膜"),
            releaseYear = 2023,
            flagship = true
        ),
        DeviceMapping(
            brand = "vivo",
            series = "X",
            model = "vivo X100",
            cameraType = CameraType.ZEISS,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("蔡司光学", "AI场景识别", "专业人像"),
            releaseYear = 2023,
            flagship = true
        ),
        DeviceMapping(
            brand = "vivo",
            series = "X",
            model = "vivo X90 Pro+",
            cameraType = CameraType.ZEISS,
            recommendedParams = CameraParams(
                ai_scene = true,
                master_hdr = "开启"
            ),
            supportedFeatures = listOf("蔡司一英寸T*", "全焦段四摄", "夜景模式"),
            releaseYear = 2022,
            flagship = true
        ),
        DeviceMapping(
            brand = "vivo",
            series = "X",
            model = "vivo X90 Pro",
            cameraType = CameraType.ZEISS,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("蔡司T*镀膜", "专业人像", "AI场景识别"),
            releaseYear = 2022,
            flagship = true
        ),
        DeviceMapping(
            brand = "vivo",
            series = "S",
            model = "vivo S18 Pro",
            cameraType = CameraType.ZEISS,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("影棚级人像", "AI场景识别", "柔光人像"),
            releaseYear = 2024,
            flagship = false
        ),
        DeviceMapping(
            brand = "vivo",
            series = "S",
            model = "vivo S18",
            cameraType = CameraType.ZEISS,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("AI场景识别", "人像模式", "柔光补光"),
            releaseYear = 2023,
            flagship = false
        ),
        
        // ==================== 华为系列（XMAGE影像） ====================
        DeviceMapping(
            brand = "华为",
            series = "Mate",
            model = "Mate 60 Pro+",
            cameraType = CameraType.XMAGE,
            recommendedParams = CameraParams(
                ai_scene = true,
                hdr = true
            ),
            supportedFeatures = listOf("XMAGE影像", "可变光圈", "超微距长焦", "专业模式"),
            releaseYear = 2023,
            flagship = true
        ),
        DeviceMapping(
            brand = "华为",
            series = "Mate",
            model = "Mate 60 Pro",
            cameraType = CameraType.XMAGE,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("XMAGE影像", "长焦微距", "夜景模式"),
            releaseYear = 2023,
            flagship = true
        ),
        DeviceMapping(
            brand = "华为",
            series = "Mate",
            model = "Mate 60",
            cameraType = CameraType.XMAGE,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("XMAGE影像", "AI场景识别", "超光变主摄"),
            releaseYear = 2023,
            flagship = true
        ),
        DeviceMapping(
            brand = "华为",
            series = "P",
            model = "P60 Pro",
            cameraType = CameraType.XMAGE,
            recommendedParams = CameraParams(
                ai_scene = true,
                master_hdr = "智能"
            ),
            supportedFeatures = listOf("XMAGE超聚光", "长焦微距", "超动态主摄"),
            releaseYear = 2023,
            flagship = true
        ),
        DeviceMapping(
            brand = "华为",
            series = "P",
            model = "P60 Art",
            cameraType = CameraType.XMAGE,
            recommendedParams = CameraParams(
                ai_scene = true,
                hdr = true
            ),
            supportedFeatures = listOf("XMAGE艺术版", "超光变镜头", "夜景模式"),
            releaseYear = 2023,
            flagship = true
        ),
        DeviceMapping(
            brand = "华为",
            series = "nova",
            model = "nova 12 Pro",
            cameraType = CameraType.XMAGE,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("XD Portrait人像", "AI场景识别", "物理可变光圈"),
            releaseYear = 2024,
            flagship = false
        ),
        DeviceMapping(
            brand = "华为",
            series = "nova",
            model = "nova 12",
            cameraType = CameraType.XMAGE,
            recommendedParams = CameraParams(
                ai_scene = true
            ),
            supportedFeatures = listOf("AI场景识别", "人像模式"),
            releaseYear = 2023,
            flagship = false
        )
    )
    
    fun getAllDevices(): List<DeviceMapping> = devices
    
    fun getDevicesByBrand(brand: String): List<DeviceMapping> {
        return devices.filter { it.brand.contains(brand, ignoreCase = true) }
    }
    
    fun getDevicesByCameraType(cameraType: CameraType): List<DeviceMapping> {
        return devices.filter { it.cameraType == cameraType }
    }
    
    fun getFlagshipDevices(): List<DeviceMapping> {
        return devices.filter { it.flagship }
    }
    
    fun findDevice(modelName: String): DeviceMapping? {
        return devices.find { 
            it.model.contains(modelName, ignoreCase = true) ||
            it.model.replace(" ", "").contains(modelName.replace(" ", ""), ignoreCase = true)
        }
    }
    
    fun searchDevices(query: String): List<DeviceMapping> {
        return devices.filter {
            it.brand.contains(query, ignoreCase = true) ||
            it.series.contains(query, ignoreCase = true) ||
            it.model.contains(query, ignoreCase = true) ||
            it.cameraType.displayName.contains(query, ignoreCase = true)
        }
    }
    
    fun getDeviceCount(): Int = devices.size
    
    fun getBrandCount(): Int = devices.map { it.brand }.distinct().size
    
    fun getCameraTypeStats(): Map<CameraType, Int> {
        return CameraType.values().associateWith { type ->
            devices.count { it.cameraType == type }
        }
    }
}
