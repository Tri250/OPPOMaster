package com.omaster.app.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
enum class PresetCategory(val displayName: String, val keywords: List<String>) {
    PORTRAIT("人像", listOf("人像", "portrait", "肖像", "自拍")),
    LANDSCAPE("风景", listOf("风景", "landscape", "风光", "自然")),
    NIGHT("夜景", listOf("夜景", "night", "夜色", "暗光")),
    FOOD("美食", listOf("美食", "food", "食物", "餐饮")),
    STREET("街拍", listOf("街拍", "street", "街头", "纪实")),
    ARCHITECTURE("建筑", listOf("建筑", "architecture", "空间", "城市")),
    NATURE("自然", listOf("自然", "nature", "植物", "生态")),
    SUNSET("日落", listOf("日落", "sunset", "日出", "暖调")),
    MACRO("微距", listOf("微距", "macro", "特写", "细节")),
    SPORTS("运动", listOf("运动", "sports", "动感", "快速")),
    NIGHT_PORTRAIT("夜景人像", listOf("夜景人像", "night_portrait", "夜晚人像")),
    VINTAGE("复古", listOf("复古", "vintage", "胶片", "经典")),
    CINEMATIC("电影感", listOf("电影", "cinematic", "视频")),
    BLACK_WHITE("黑白", listOf("黑白", "bw", "monochrome", "单色"));

    companion object {
        fun fromName(name: String): PresetCategory? {
            val normalizedName = name.lowercase()
            return entries.find { category ->
                category.name.lowercase() == normalizedName ||
                category.displayName == name ||
                category.keywords.any { keyword ->
                    normalizedName.contains(keyword.lowercase())
                }
            }
        }

        fun fromCameraParams(params: CameraParams?): PresetCategory? {
            params ?: return null
            return when {
                params.portrait_mode == true -> PORTRAIT
                params.night_mode == true -> NIGHT
                params.sports_mode == true -> SPORTS
                params.macro_mode == true -> MACRO
                params.color_profile?.contains("Food", ignoreCase = true) == true -> FOOD
                params.color_profile?.contains("Cinematic", ignoreCase = true) == true -> CINEMATIC
                params.color_profile?.contains("Warm", ignoreCase = true) == true -> SUNSET
                params.color_profile?.contains("Cool", ignoreCase = true) == true -> NIGHT
                params.color_profile?.contains("Film", ignoreCase = true) == true -> VINTAGE
                params.color_profile?.contains("Monochrome", ignoreCase = true) == true -> BLACK_WHITE
                params.filter?.contains("夜景", ignoreCase = true) == true -> NIGHT
                params.filter?.contains("美食", ignoreCase = true) == true -> FOOD
                params.filter?.contains("建筑", ignoreCase = true) == true -> ARCHITECTURE
                params.filter?.contains("人像", ignoreCase = true) == true -> PORTRAIT
                params.filter?.contains("风景", ignoreCase = true) == true -> LANDSCAPE
                else -> null
            }
        }
    }
}

@Serializable
@Parcelize
data class Section(
    val title: String,
    val content: String
) : Parcelable

@Serializable
@Parcelize
data class Preset(
    val id: String,
    val name: String,
    val coverPath: String,
    val sections: List<Section> = emptyList(),
    val cameraParams: CameraParams? = null,
    val deviceModel: String = "",
    val source: String = "omaster_cloud",
    val isFavorite: Boolean = false,
    val category: PresetCategory? = null,
    val coverResourceId: Int? = null
) : Parcelable {
    fun getEffectiveCategory(): PresetCategory {
        return category
            ?: PresetCategory.fromCameraParams(cameraParams)
            ?: PresetCategory.fromName(name)
            ?: PresetCategory.LANDSCAPE
    }

    fun matchesCategory(targetCategory: PresetCategory): Boolean {
        return getEffectiveCategory() == targetCategory
    }

    fun matchesCategories(categories: List<PresetCategory>): Boolean {
        if (categories.isEmpty()) return true
        return categories.any { matchesCategory(it) }
    }
}

@Serializable
data class PresetList(
    val version: String,
    val lastUpdated: String,
    val presets: List<Preset>
)
