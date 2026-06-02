package com.omaster.app.model

/**
 * 2026年 OPPO Find X8 Ultra 哈苏大师模式影像参数
 * 基于 OPPO HyperTone Camera System
 */

sealed interface ValidationResult {
    object Valid : ValidationResult
    data class Invalid(val errors: List<String>) : ValidationResult
}

/**
 * 相机模式枚举
 */
enum class CameraMode(val displayName: String) {
    HasselbladMaster("哈苏大师"),
    HasselbladPortrait("哈苏人像"),
    HasselbladLandscape("哈苏风景"),
    HasselbladNight("哈苏夜景"),
    HasselbladStreet("哈苏街拍"),
    HasselbladPro("哈苏专业"),
    AutoMode("智能模式"),
    ManualMode("专业模式")
}

/**
 * 色彩风格枚举
 */
enum class ColorStyle(val displayName: String, val description: String) {
    Natural("自然", "忠实地再现真实色彩"),
    Vivid("鲜明", "增强色彩饱和度和对比度"),
    Cinematic("电影感", "低饱和度高对比度的电影风格"),
    Professional("专业", "哈苏标准色彩科学"),
    Warm("暖调", "温暖柔和的色调"),
    Cool("冷调", "清冷清爽的色调"),
    Classic("经典", "复古胶片风格"),
    BlackWhite("黑白", "纯粹的黑白影像"),
    Portrait("人像", "优化人像肤色"),
    Food("美食", "提升美食色彩饱和度")
}

/**
 * 焦距模式枚举
 */
enum class FocalLengthMode(val displayName: String) {
    UltraWide("超广角"),
    Wide("广角"),
    Standard("标准"),
    Portrait("人像焦"),
    Telephoto("长焦"),
    UltraTelephoto("超长焦"),
    Macro("微距"),
    SuperMacro("超级微距")
}

/**
 * 哈苏大师模式影像参数 - 2026年 OPPO Find X8 Ultra
 */
data class CameraParams(
    // 基础参数
    val mode: String = CameraMode.HasselbladMaster.displayName,
    val filter: String = "",
    
    // 核心影像参数
    val iso: Int = 100,
    val shutter: String = "1/200",
    val ev: String = "+0.0",
    val wb: String = "5500K",
    
    // 焦距与光圈
    val focalLength: String = "24mm",
    val focalLengthMode: String = FocalLengthMode.Wide.displayName,
    val aperture: String = "f/1.8",
    
    // 拍摄模式开关
    val hdr: Boolean = false,
    val nightMode: Boolean = false,
    val portraitMode: Boolean = false,
    val aiOptimization: Boolean = true,
    val autoFocus: Boolean = true,
    val opticalStabilization: Boolean = true,
    val rawCapture: Boolean = false,
    val proMode: Boolean = true,
    
    // 哈苏认证与风格
    val hasselblad_hncs: Boolean = true,
    val hasselbladNaturalColor: Boolean = true,
    val hasselbladMasterStyle: String = "",
    val hasselbladProMode: Boolean = true,
    val hasselbladColorScience: String = "HNCS 3.0",
    
    // 色彩与风格
    val colorProfile: String = ColorStyle.Natural.displayName,
    val colorStyle: String = ColorStyle.Natural.name,
    val colorTemperature: Int = 5500,
    val tint: Int = 0,
    
    // 图像质量调整
    val sharpness: Int = 50,
    val contrast: Int = 50,
    val saturation: Int = 50,
    val highlight: Int = 0,
    val shadow: Int = 0,
    val exposureCompensation: Int = 0,
    
    // 高级参数
    val meteringMode: String = "Evaluative",
    val focusMode: String = "Continuous AF",
    val whiteBalancePreset: String = "Auto",
    val noiseReduction: Int = 50,
    val detailEnhancement: Int = 50,
    
    // 镜头信息
    val lensId: String = "",
    val lensAperture: String = "f/1.8",
    val opticalZoom: Int = 1,
    val digitalZoom: Int = 1,
    val sensorSize: String = "1英寸",
    
    // 元数据
    val version: String = "3.0",
    val lastModified: Long = System.currentTimeMillis(),
    
    // 兼容性属性（下划线命名）
    val ai_scene_recognition: String? = null,
    val ai_optimization: Boolean = aiOptimization,
    val focal_length: String = focalLength,
    val focus_distance: String? = null,
    val hasselblad_master_style: String? = if (hasselbladMasterStyle.isNotEmpty()) hasselbladMasterStyle else null,
    val hasselblad_natural_color: Boolean = hasselbladNaturalColor,
    val color_profile: String = colorProfile
) {
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        // ISO 校验 - 2026年OPPO最新范围
        if (iso < 32 || iso > 102400) {
            errors.add("ISO 超出范围（应在 32-102400 之间）")
        }
        
        // 快门格式校验
        if (!isValidShutterSpeed(shutter)) {
            errors.add("快门速度格式无效")
        }
        
        // EV 格式校验
        if (!isValidExposureValue(ev)) {
            errors.add("曝光补偿格式无效")
        }
        
        // 白平衡色温校验
        if (colorTemperature < 2500 || colorTemperature > 10000) {
            errors.add("色温超出范围（应在 2500-10000K 之间）")
        }
        
        // 饱和度校验
        if (saturation < 0 || saturation > 100) {
            errors.add("饱和度超出范围（应在 0-100 之间）")
        }
        
        return if (errors.isEmpty()) ValidationResult.Valid 
               else ValidationResult.Invalid(errors)
    }
    
    private fun isValidShutterSpeed(shutter: String): Boolean {
        val fractionRegex = Regex("""^1/\d+$""")
        val numberRegex = Regex("""^\d+(\.\d+)?s$""")
        val plainNumberRegex = Regex("""^\d+(\.\d+)?$""")
        return fractionRegex.matches(shutter) || 
               numberRegex.matches(shutter) || 
               plainNumberRegex.matches(shutter)
    }
    
    private fun isValidExposureValue(ev: String): Boolean {
        val evRegex = Regex("""^[+-]?\d+(\.\d+)?$""")
        return evRegex.matches(ev)
    }
    
    /**
     * 格式化参数为标准展示格式
     */
    fun formatParamsForDisplay(): String {
        return buildString {
            append("ISO $iso")
            append(" · $shutter")
            if (ev != "+0.0" && ev != "0") append(" · EV $ev")
            append(" · $wb")
            if (hasselblad_hncs) append(" · HNCS")
        }
    }
    
    /**
     * 格式化完整参数展示
     */
    fun formatFullParams(): Map<String, String> {
        return mapOf(
            "模式" to mode,
            "ISO" to iso.toString(),
            "快门速度" to shutter,
            "曝光补偿" to ev,
            "白平衡" to wb,
            "焦距" to focalLength,
            "光圈" to aperture,
            "色彩风格" to colorStyle,
            "HDR" to if (hdr) "开启" else "关闭",
            "夜景模式" to if (nightMode) "开启" else "关闭",
            "人像模式" to if (portraitMode) "开启" else "关闭",
            "AI优化" to if (aiOptimization) "开启" else "关闭",
            "哈苏HNCS" to if (hasselblad_hncs) "认证" else "未认证",
            "哈苏风格" to hasselbladMasterStyle.ifEmpty { "默认" },
            "色彩科学" to hasselbladColorScience,
            "清晰度" to "$sharpness%",
            "对比度" to "$contrast%",
            "饱和度" to "$saturation%"
        )
    }
    
    /**
     * 转换为 JSON 格式用于数据同步
     */
    fun toJsonMap(): Map<String, Any> {
        return mapOf(
            "mode" to mode,
            "filter" to filter,
            "iso" to iso,
            "shutter" to shutter,
            "ev" to ev,
            "wb" to wb,
            "focalLength" to focalLength,
            "aperture" to aperture,
            "hdr" to hdr,
            "nightMode" to nightMode,
            "portraitMode" to portraitMode,
            "aiOptimization" to aiOptimization,
            "hasselblad_hncs" to hasselblad_hncs,
            "hasselbladNaturalColor" to hasselbladNaturalColor,
            "hasselbladMasterStyle" to hasselbladMasterStyle,
            "hasselbladColorScience" to hasselbladColorScience,
            "colorProfile" to colorProfile,
            "colorStyle" to colorStyle,
            "colorTemperature" to colorTemperature,
            "sharpness" to sharpness,
            "contrast" to contrast,
            "saturation" to saturation,
            "version" to version,
            "lastModified" to lastModified
        )
    }
    
    companion object {
        /**
         * 从 JSON 创建 CameraParams
         */
        fun fromJsonMap(json: Map<String, Any?>): CameraParams {
            return CameraParams(
                mode = json["mode"] as? String ?: CameraMode.HasselbladMaster.displayName,
                filter = json["filter"] as? String ?: "",
                iso = (json["iso"] as? Number)?.toInt() ?: 100,
                shutter = json["shutter"] as? String ?: "1/200",
                ev = json["ev"] as? String ?: "+0.0",
                wb = json["wb"] as? String ?: "5500K",
                focalLength = json["focalLength"] as? String ?: json["focal_length"] as? String ?: "24mm",
                aperture = json["aperture"] as? String ?: "f/1.8",
                hdr = json["hdr"] as? Boolean ?: false,
                nightMode = json["nightMode"] as? Boolean ?: false,
                portraitMode = json["portraitMode"] as? Boolean ?: false,
                aiOptimization = json["aiOptimization"] as? Boolean ?: true,
                hasselblad_hncs = json["hasselblad_hncs"] as? Boolean ?: true,
                hasselbladNaturalColor = json["hasselbladNaturalColor"] as? Boolean ?: json["hasselblad_natural_color"] as? Boolean ?: true,
                hasselbladMasterStyle = json["hasselbladMasterStyle"] as? String ?: json["hasselblad_master_style"] as? String ?: "",
                hasselbladColorScience = json["hasselbladColorScience"] as? String ?: "HNCS 3.0",
                colorProfile = json["colorProfile"] as? String ?: json["color_profile"] as? String ?: ColorStyle.Natural.displayName,
                colorStyle = json["colorStyle"] as? String ?: ColorStyle.Natural.name,
                colorTemperature = (json["colorTemperature"] as? Number)?.toInt() ?: 5500,
                sharpness = (json["sharpness"] as? Number)?.toInt() ?: 50,
                contrast = (json["contrast"] as? Number)?.toInt() ?: 50,
                saturation = (json["saturation"] as? Number)?.toInt() ?: 50,
                version = json["version"] as? String ?: "3.0",
                lastModified = (json["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                ai_scene_recognition = json["ai_scene_recognition"] as? String,
                ai_optimization = json["ai_optimization"] as? Boolean ?: json["aiOptimization"] as? Boolean ?: true,
                focus_distance = json["focus_distance"] as? String
            )
        }
        
        /**
         * 创建默认的哈苏大师模式参数
         */
        fun defaultHasselbladMaster(): CameraParams {
            return CameraParams(
                mode = CameraMode.HasselbladMaster.displayName,
                hasselblad_hncs = true,
                hasselbladNaturalColor = true,
                hasselbladProMode = true,
                hasselbladColorScience = "HNCS 3.0",
                colorStyle = ColorStyle.Natural.name,
                colorProfile = ColorStyle.Natural.displayName,
                aiOptimization = true,
                proMode = true
            )
        }
    }
}
