package com.omaster.app.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * 预设数据模型
 */
@Parcelize
data class Preset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val coverUrl: String = "",
    val galleryImages: List<String> = emptyList(),
    val author: String? = null,
    val tags: List<String> = emptyList(),
    val supportedDevices: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isNew: Boolean = false,
    val useCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val params: List<ParamItem> = emptyList(),
    val sections: List<PresetSectionModel> = emptyList()
) : Parcelable {
    
    /**
     * 获取预设的显示标签
     */
    fun getDisplayTags(): List<String> {
        return tags.filter { it.isNotBlank() }
    }
    
    /**
     * 检查预设是否支持指定设备
     */
    fun supportsDevice(device: String): Boolean {
        return supportedDevices.any { 
            it.contains(device, ignoreCase = true) || 
            device.contains(it, ignoreCase = true)
        }
    }
    
    /**
     * 获取预设的简短描述
     */
    fun getShortDescription(maxLength: Int = 50): String {
        return if (description.length > maxLength) {
            description.take(maxLength) + "..."
        } else {
            description
        }
    }
    
    companion object {
        /**
         * 创建示例预设（用于测试）
         */
        fun createSample(
            name: String = "示例预设",
            description: String = "这是一个示例预设",
            tags: List<String> = listOf("示例")
        ): Preset {
            return Preset(
                name = name,
                description = description,
                tags = tags,
                supportedDevices = listOf("OPPO", "OnePlus", "realme")
            )
        }
    }
}

/**
 * 预设分类
 */
enum class PresetCategory {
    ALL,        // 全部
    PORTRAIT,   // 人像
    LANDSCAPE,  // 风景
    NIGHT,      // 夜景
    FOOD,       // 美食
    STREET,     // 街拍
    FILM,       // 胶片
    BLACK_WHITE // 黑白
}

/**
 * 预设排序方式
 */
enum class PresetSortOrder {
    DEFAULT,      // 默认
    NAME_ASC,     // 名称升序
    NAME_DESC,    // 名称降序
    NEWEST,       // 最新
    POPULAR,      // 热门
    MOST_USED     // 最多使用
}
