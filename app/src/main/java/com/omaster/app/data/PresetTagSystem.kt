package com.omaster.app.data

import com.omaster.app.domain.model.Preset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 预设标签系统 - 管理预设标签的分类、搜索和筛选
 */
@Singleton
class PresetTagSystem @Inject constructor() {

    /**
     * 标签数据模型
     * @property id 标签唯一标识
     * @property name 标签显示名称
     * @property category 标签分类
     * @property color 标签颜色（用于UI展示）
     * @property icon 标签图标（可选）
     * @property description 标签描述
     * @property usageCount 使用次数（用于排序）
     */
    data class Tag(
        val id: String,
        val name: String,
        val category: TagCategory,
        val color: String = "#FF6B6B",
        val icon: String? = null,
        val description: String = "",
        val usageCount: Int = 0
    )

    /**
     * 标签分类枚举
     */
    enum class TagCategory {
        STYLE,      // 风格类标签
        SCENE,      // 场景类标签
        DEVICE,     // 设备类标签
        COLOR,      // 色彩类标签
        MOOD,       // 情绪类标签
        CUSTOM      // 自定义标签
    }

    /**
     * 标签筛选结果
     */
    data class FilterResult(
        val presets: List<Preset>,
        val matchedTags: List<Tag>,
        val totalCount: Int
    )

    // 预定义标签库
    private val predefinedTags = listOf(
        // 风格类标签
        Tag("style_portrait", "人像", TagCategory.STYLE, "#FF6B9D", "👤", "人像摄影风格"),
        Tag("style_landscape", "风景", TagCategory.STYLE, "#4ECDC4", "🏔️", "风景摄影风格"),
        Tag("style_street", "街拍", TagCategory.STYLE, "#45B7D1", "📸", "街头摄影风格"),
        Tag("style_food", "美食", TagCategory.STYLE, "#F9CA24", "🍜", "美食摄影风格"),
        Tag("style_vintage", "复古", TagCategory.STYLE, "#A29BFE", "📷", "复古胶片风格"),
        Tag("style_cinematic", "电影感", TagCategory.STYLE, "#6C5CE7", "🎬", "电影色彩风格"),
        Tag("style_minimal", "极简", TagCategory.STYLE, "#74B9FF", "⬜", "极简主义风格"),
        Tag("style_bw", "黑白", TagCategory.STYLE, "#2D3436", "⚫", "黑白摄影风格"),
        Tag("style_hasselblad", "哈苏", TagCategory.STYLE, "#FF7675", "📷", "哈苏自然色彩"),

        // 场景类标签
        Tag("scene_night", "夜景", TagCategory.SCENE, "#2C3E50", "🌃", "夜间拍摄场景"),
        Tag("scene_daylight", "日光", TagCategory.SCENE, "#F39C12", "☀️", "白天拍摄场景"),
        Tag("scene_indoor", "室内", TagCategory.SCENE, "#8E44AD", "🏠", "室内拍摄场景"),
        Tag("scene_outdoor", "户外", TagCategory.SCENE, "#27AE60", "🌲", "户外拍摄场景"),
        Tag("scene_beach", "海边", TagCategory.SCENE, "#3498DB", "🏖️", "海滩拍摄场景"),
        Tag("scene_mountain", "山脉", TagCategory.SCENE, "#7F8C8D", "⛰️", "山地拍摄场景"),
        Tag("scene_city", "城市", TagCategory.SCENE, "#34495E", "🏙️", "城市建筑场景"),
        Tag("scene_nature", "自然", TagCategory.SCENE, "#16A085", "🌿", "自然风光场景"),

        // 设备类标签
        Tag("device_oppo", "OPPO", TagCategory.DEVICE, "#1ABC9C", "📱", "OPPO设备专用"),
        Tag("device_oneplus", "OnePlus", TagCategory.DEVICE, "#E74C3C", "📱", "一加设备专用"),
        Tag("device_realme", "realme", TagCategory.DEVICE, "#F39C12", "📱", "真我设备专用"),
        Tag("device_findx8", "Find X8", TagCategory.DEVICE, "#9B59B6", "📱", "Find X8系列专用"),
        Tag("device_hasselblad", "哈苏", TagCategory.DEVICE, "#E67E22", "📷", "哈苏合作款"),

        // 色彩类标签
        Tag("color_warm", "暖色调", TagCategory.COLOR, "#E17055", "🟠", "温暖色调风格"),
        Tag("color_cool", "冷色调", TagCategory.COLOR, "#74B9FF", "🔵", "冷色调风格"),
        Tag("color_vivid", "鲜艳", TagCategory.COLOR, "#FF6B6B", "🌈", "鲜艳色彩风格"),
        Tag("color_muted", "柔和", TagCategory.COLOR, "#B2BEC3", "🎨", "柔和色彩风格"),
        Tag("color_natural", "自然", TagCategory.COLOR, "#55EFC4", "🍃", "自然色彩风格"),

        // 情绪类标签
        Tag("mood_happy", "欢快", TagCategory.MOOD, "#FDCB6E", "😊", "欢快明亮风格"),
        Tag("mood_calm", "宁静", TagCategory.MOOD, "#81ECEC", "😌", "宁静平和风格"),
        Tag("mood_dramatic", "戏剧性", TagCategory.MOOD, "#636E72", "🎭", "戏剧化风格"),
        Tag("mood_romantic", "浪漫", TagCategory.MOOD, "#FD79A8", "💕", "浪漫柔和风格"),
        Tag("mood_mysterious", "神秘", TagCategory.MOOD, "#2D3436", "🌙", "神秘暗调风格")
    )

    // 用户自定义标签
    private val customTags = mutableListOf<Tag>()

    /**
     * 获取所有标签
     */
    fun getAllTags(): List<Tag> {
        return predefinedTags + customTags
    }

    /**
     * 按分类获取标签
     */
    fun getTagsByCategory(category: TagCategory): List<Tag> {
        return getAllTags().filter { it.category == category }
    }

    /**
     * 获取所有标签分类
     */
    fun getAllCategories(): List<TagCategory> {
        return TagCategory.entries.toList()
    }

    /**
     * 获取分类的显示名称
     */
    fun getCategoryDisplayName(category: TagCategory): String {
        return when (category) {
            TagCategory.STYLE -> "风格"
            TagCategory.SCENE -> "场景"
            TagCategory.DEVICE -> "设备"
            TagCategory.COLOR -> "色彩"
            TagCategory.MOOD -> "情绪"
            TagCategory.CUSTOM -> "自定义"
        }
    }

    /**
     * 根据ID获取标签
     */
    fun getTagById(tagId: String): Tag? {
        return getAllTags().find { it.id == tagId }
    }

    /**
     * 根据名称搜索标签
     */
    fun searchTags(query: String): List<Tag> {
        if (query.isBlank()) return emptyList()
        return getAllTags().filter {
            it.name.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
        }
    }

    /**
     * 添加自定义标签
     */
    fun addCustomTag(name: String, description: String = "", color: String = "#FF6B6B"): Tag {
        val id = "custom_${System.currentTimeMillis()}"
        val tag = Tag(
            id = id,
            name = name,
            category = TagCategory.CUSTOM,
            color = color,
            description = description
        )
        customTags.add(tag)
        return tag
    }

    /**
     * 删除自定义标签
     */
    fun removeCustomTag(tagId: String): Boolean {
        return customTags.removeIf { it.id == tagId }
    }

    /**
     * 根据标签筛选预设
     */
    fun filterPresetsByTags(
        presets: List<Preset>,
        selectedTags: List<String>,
        matchMode: TagMatchMode = TagMatchMode.ANY
    ): FilterResult {
        if (selectedTags.isEmpty()) {
            return FilterResult(presets, emptyList(), presets.size)
        }

        val matchedTags = selectedTags.mapNotNull { getTagById(it) }

        val filteredPresets = presets.filter { preset ->
            val presetTagIds = preset.tags.map { tagName ->
                // 将预设的标签名称映射到标签ID
                getAllTags().find { it.name == tagName }?.id ?: tagName
            }

            when (matchMode) {
                TagMatchMode.ANY -> selectedTags.any { it in presetTagIds }
                TagMatchMode.ALL -> selectedTags.all { it in presetTagIds }
                TagMatchMode.EXACT -> selectedTags.toSet() == presetTagIds.toSet()
            }
        }

        return FilterResult(filteredPresets, matchedTags, filteredPresets.size)
    }

    /**
     * 根据分类筛选预设
     */
    fun filterPresetsByCategory(
        presets: List<Preset>,
        category: TagCategory
    ): List<Preset> {
        val categoryTags = getTagsByCategory(category).map { it.name }
        return presets.filter { preset ->
            preset.tags.any { it in categoryTags }
        }
    }

    /**
     * 获取预设的标签列表
     */
    fun getPresetTags(preset: Preset): List<Tag> {
        return preset.tags.mapNotNull { tagName ->
            getAllTags().find { it.name == tagName }
        }
    }

    /**
     * 获取热门标签（按使用次数排序）
     */
    fun getPopularTags(limit: Int = 10): List<Tag> {
        return getAllTags()
            .sortedByDescending { it.usageCount }
            .take(limit)
    }

    /**
     * 获取相关标签（基于共同出现的预设）
     */
    fun getRelatedTags(tagId: String, presets: List<Preset>, limit: Int = 5): List<Tag> {
        val tag = getTagById(tagId) ?: return emptyList()

        // 找到包含该标签的所有预设
        val relatedPresets = presets.filter { preset ->
            preset.tags.contains(tag.name)
        }

        // 统计这些预设中其他标签的出现次数
        val tagFrequency = mutableMapOf<String, Int>()
        relatedPresets.forEach { preset ->
            preset.tags.forEach { tagName ->
                if (tagName != tag.name) {
                    tagFrequency[tagName] = (tagFrequency[tagName] ?: 0) + 1
                }
            }
        }

        // 返回最相关的标签
        return tagFrequency.entries
            .sortedByDescending { it.value }
            .take(limit)
            .mapNotNull { entry ->
                getAllTags().find { it.name == entry.key }
            }
    }

    /**
     * 标签匹配模式
     */
    enum class TagMatchMode {
        ANY,    // 匹配任意一个标签
        ALL,    // 匹配所有标签
        EXACT   // 精确匹配
    }

    /**
     * 获取标签云数据（用于可视化展示）
     */
    fun getTagCloudData(): List<TagCloudItem> {
        val allTags = getAllTags()
        val maxUsage = allTags.maxOfOrNull { it.usageCount } ?: 1

        return allTags.map { tag ->
            TagCloudItem(
                tag = tag,
                size = calculateTagSize(tag.usageCount, maxUsage),
                priority = tag.usageCount
            )
        }.sortedByDescending { it.priority }
    }

    /**
     * 标签云数据项
     */
    data class TagCloudItem(
        val tag: Tag,
        val size: Float,
        val priority: Int
    )

    private fun calculateTagSize(usageCount: Int, maxUsage: Int): Float {
        if (maxUsage == 0) return 1f
        return 0.8f + (usageCount.toFloat() / maxUsage) * 0.7f
    }

    /**
     * 增加标签使用次数
     */
    fun incrementTagUsage(tagId: String) {
        val index = customTags.indexOfFirst { it.id == tagId }
        if (index != -1) {
            val tag = customTags[index]
            customTags[index] = tag.copy(usageCount = tag.usageCount + 1)
        }
    }

    /**
     * 批量筛选预设（支持多个标签组合）
     */
    fun batchFilterPresets(
        presets: List<Preset>,
        tagFilters: Map<TagCategory, List<String>>,
        matchMode: TagMatchMode = TagMatchMode.ANY
    ): Flow<FilterResult> = flow {
        if (tagFilters.isEmpty() || tagFilters.values.all { it.isEmpty() }) {
            emit(FilterResult(presets, emptyList(), presets.size))
            return@flow
        }

        val allSelectedTags = tagFilters.values.flatten()
        val result = filterPresetsByTags(presets, allSelectedTags, matchMode)

        // 进一步按分类筛选
        var finalPresets = result.presets
        tagFilters.forEach { (category, tagIds) ->
            if (tagIds.isNotEmpty()) {
                finalPresets = finalPresets.filter { preset ->
                    val presetTagIds = preset.tags.mapNotNull { tagName ->
                        getAllTags().find { it.name == tagName && it.category == category }?.id
                    }
                    when (matchMode) {
                        TagMatchMode.ANY -> tagIds.any { it in presetTagIds }
                        TagMatchMode.ALL -> tagIds.all { it in presetTagIds }
                        TagMatchMode.EXACT -> tagIds.toSet() == presetTagIds.toSet()
                    }
                }
            }
        }

        emit(FilterResult(finalPresets, result.matchedTags, finalPresets.size))
    }
}
