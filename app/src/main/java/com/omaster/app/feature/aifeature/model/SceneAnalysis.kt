package com.omaster.app.feature.aifeature.model

data class SceneAnalysis(
    val primaryLabels: List<Pair<String, Float>>,
    val detectedObjects: List<String>,
    val lightingCondition: LightingCondition,
    val colorTemperature: ColorTemperature,
    val recommendedPresetIds: List<String>
)

enum class LightingCondition {
    BRIGHT_SUNLIGHT,
    SOFT_LIGHT,
    LOW_LIGHT,
    NIGHT,
    INDOOR
}

enum class ColorTemperature {
    WARM,
    NEUTRAL,
    COOL
}

data class UserProfile(
    val userId: String,
    val preferredStyles: Map<String, Int>,
    val usageHistory: List<UsageRecord>,
    val deviceModel: String,
    val skillLevel: SkillLevel
)

data class UsageRecord(
    val presetId: String,
    val timestamp: Long,
    val sceneType: String?
)

enum class SkillLevel {
    BEGINNER,
    ENTHUSIAST,
    PROFESSIONAL
}

data class RecommendedPreset(
    val presetId: String,
    val confidenceScore: Float,
    val reason: String
)
