package com.omaster.app.model

// ============================================
// OPPO 哈苏相机参数模型 - 专业级扩展
// ============================================

data class CameraParams(
    val mode: String = "master",
    val filter: String = "",
    val iso: Int = 100,
    val shutter: String = "1/125",
    val ev: String = "0",
    val wb: String = "5500K",
    val hasselblad_hncs: Boolean = false
)

// ============================================
// 精细调控参数 - 哈苏专业级
// ============================================
data class FineTuneParams(
    // 锐度 (-20 ~ +40)
    val sharpness: Int = 25,
    
    // 对比度 (-20 ~ +40)
    val contrast: Int = 20,
    
    // 饱和度 (-20 ~ +20)
    val saturation: Int = -5,
    
    // 暗角 (0 ~ 3: 0关闭, 1低, 2中, 3高)
    val vignette: Int = 1,
    
    // 柔光 (0 ~ 3: 0关闭, 1低, 2中, 3高)
    val softness: Int = 0,
    
    // 色温 (-20 ~ +20)
    val colorTemperature: Int = 0,
    
    // 色调 (-20 ~ +20)
    val tint: Int = 0,
    
    // 肤色优化 (是否启用)
    val skinToneOptimization: Boolean = true,
    
    // 高光保护 (-10 ~ +10)
    val highlightProtection: Int = 5,
    
    // 阴影提升 (-10 ~ +10)
    val shadowLift: Int = 3,
    
    // 降噪强度 (0 ~ 5)
    val noiseReduction: Int = 2,
    
    // 动态范围模式 (0自动, 1标准, 2高)
    val dynamicRangeMode: Int = 1
) {
    companion object {
        // 默认参数 - 哈苏HNCS标准
        val HNCS_DEFAULT = FineTuneParams(
            sharpness = 25,
            contrast = 20,
            saturation = -5,
            vignette = 1,
            softness = 0,
            colorTemperature = 0,
            tint = 0,
            skinToneOptimization = true,
            highlightProtection = 5,
            shadowLift = 3,
            noiseReduction = 2,
            dynamicRangeMode = 1
        )
        
        // 人像风格
        val PORTRAIT = FineTuneParams(
            sharpness = 20,
            contrast = 15,
            saturation = -3,
            vignette = 2,
            softness = 2,
            colorTemperature = 5,
            tint = 0,
            skinToneOptimization = true,
            highlightProtection = 7,
            shadowLift = 5,
            noiseReduction = 3,
            dynamicRangeMode = 1
        )
        
        // 风景风格
        val LANDSCAPE = FineTuneParams(
            sharpness = 30,
            contrast = 25,
            saturation = 0,
            vignette = 1,
            softness = 0,
            colorTemperature = -3,
            tint = -2,
            skinToneOptimization = false,
            highlightProtection = 8,
            shadowLift = 2,
            noiseReduction = 1,
            dynamicRangeMode = 2
        )
        
        // 夜景风格
        val NIGHT = FineTuneParams(
            sharpness = 20,
            contrast = 18,
            saturation = -8,
            vignette = 2,
            softness = 1,
            colorTemperature = -5,
            tint = 0,
            skinToneOptimization = false,
            highlightProtection = 10,
            shadowLift = 8,
            noiseReduction = 5,
            dynamicRangeMode = 2
        )
    }
}

// ============================================
// 风格类型枚举
// ============================================
enum class StyleType(val displayName: String) {
    NATURAL("自然真实"),
    FILM("胶片质感"),
    CINEMATIC("电影感"),
    PORTRAIT("人像优化"),
    LANDSCAPE("风景优化"),
    NIGHT("夜景优化"),
    STREET("街头纪实"),
    FOOD("美食优化"),
    ARCHITECTURE("建筑摄影"),
    MONOCHROME("黑白质感"),
    VINTAGE("复古风格"),
    VIBRANT("鲜艳风格");
    
    companion object {
        fun fromString(value: String): StyleType {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: NATURAL
        }
    }
}

// ============================================
// 场景标签枚举
// ============================================
enum class SceneTag(val displayName: String) {
    SUNRISE("日出"),
    SUNSET("日落"),
    BLUE_HOUR("蓝调时刻"),
    GOLDEN_HOUR("黄金时刻"),
    PORTRAIT("人像"),
    GROUP_PHOTO("合影"),
    STREET("街头"),
    ARCHITECTURE("建筑"),
    LANDSCAPE("风景"),
    NATURE("自然"),
    FOOD("美食"),
    PRODUCT("产品"),
    NIGHT_SCENE("夜景"),
    FIREWORKS("烟花"),
    CONCERT("演唱会"),
    TRAVEL("旅行"),
    WEDDING("婚礼"),
    PETS("宠物"),
    FLOWERS("花卉"),
    CITYSCAPE("城市风光");
    
    companion object {
        fun fromString(value: String): SceneTag {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: STREET
        }
    }
}

// ============================================
// 创作者信息
// ============================================
data class Creator(
    val id: String,
    val name: String,
    val avatarPath: String = "",
    val bio: String = "",
    val presetCount: Int = 0,
    val followers: Int = 0,
    val isVerified: Boolean = false
)

// ============================================
// 预设统计信息
// ============================================
data class PresetStats(
    val usageCount: Long = 0,
    val favoriteCount: Long = 0,
    val shareCount: Long = 0,
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
