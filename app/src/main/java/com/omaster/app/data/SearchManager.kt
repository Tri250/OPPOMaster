package com.omaster.app.data

import com.omaster.app.model.Preset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 搜索管理器
 * 提供智能搜索功能，支持关键词搜索、模糊匹配、多维度筛选
 */
@Singleton
class SearchManager @Inject constructor(
    private val presetRepository: PresetRepository,
    private val presetHistoryManager: PresetHistoryManager
) {
    // 搜索历史记录
    private val _searchHistory = mutableListOf<String>()
    val searchHistory: List<String> get() = _searchHistory.toList()

    // 热门搜索关键词
    val hotSearchKeywords = listOf(
        "哈苏", "胶片", "夜景", "人像", "风景",
        "街拍", "美食", "复古", "清新", "黑白",
        "日系", "赛博朋克", "自然", "城市", "旅行"
    )

    /**
     * 执行智能搜索
     * @param query 搜索关键词
     * @param presets 预设列表
     * @return 搜索结果
     */
    fun search(query: String, presets: List<Preset>): List<Preset> {
        if (query.isBlank()) return presets

        // 记录搜索历史
        addToSearchHistory(query)

        val normalizedQuery = normalizeText(query)
        val keywords = normalizedQuery.split(" ", "，", ",")

        return presets.filter { preset ->
            matchPreset(preset, keywords, query)
        }.sortedByDescending { preset ->
            calculateRelevanceScore(preset, query, keywords)
        }
    }

    /**
     * 多维度筛选
     * @param presets 预设列表
     * @param filterType 筛选类型
     * @return 筛选结果
     */
    fun filter(presets: List<Preset>, filterType: FilterType): List<Preset> {
        return when (filterType) {
            FilterType.ALL -> presets
            FilterType.FAVORITES -> presets.filter { it.isFavorite }
            FilterType.HNCS -> presets.filter { 
                it.name.contains("哈苏", ignoreCase = true) || 
                it.tags.contains("HNCS") ||
                it.tags.contains("哈苏自然")
            }
            FilterType.FIND_X -> presets.filter {
                it.supportedDevices.any { device -> 
                    device.contains("Find", ignoreCase = true) ||
                    device.contains("X", ignoreCase = true)
                }
            }
            FilterType.RENO -> presets.filter {
                it.supportedDevices.any { device ->
                    device.contains("Reno", ignoreCase = true)
                }
            }
            FilterType.NEW -> presets.sortedByDescending { it.createdAt }
            FilterType.TRENDING -> presets.sortedByDescending { it.useCount }
        }
    }

    /**
     * 组合搜索和筛选
     */
    fun searchAndFilter(
        query: String,
        filterType: FilterType,
        presets: List<Preset>
    ): List<Preset> {
        val filtered = filter(presets, filterType)
        return if (query.isBlank()) {
            filtered
        } else {
            search(query, filtered)
        }
    }

    /**
     * 获取搜索建议
     */
    fun getSearchSuggestions(query: String, presets: List<Preset>): List<String> {
        if (query.length < 2) return emptyList()

        val normalizedQuery = normalizeText(query)
        val suggestions = mutableSetOf<String>()

        // 从预设名称、标签、作者中提取建议
        presets.forEach { preset ->
            // 名称匹配
            if (normalizeText(preset.name).contains(normalizedQuery)) {
                suggestions.add(preset.name)
            }
            // 标签匹配
            preset.tags.forEach { tag ->
                if (normalizeText(tag).contains(normalizedQuery)) {
                    suggestions.add(tag)
                }
            }
            // 作者匹配
            if (preset.author != null && normalizeText(preset.author).contains(normalizedQuery)) {
                suggestions.add(preset.author)
            }
        }

        // 添加热门搜索匹配
        hotSearchKeywords.forEach { keyword ->
            if (normalizeText(keyword).contains(normalizedQuery)) {
                suggestions.add(keyword)
            }
        }

        return suggestions.take(8)
    }

    /**
     * 按风格筛选
     */
    fun filterByStyle(presets: List<Preset>, style: String): List<Preset> {
        return presets.filter { preset ->
            preset.tags.any { tag ->
                tag.equals(style, ignoreCase = true) ||
                tag.contains(style, ignoreCase = true)
            }
        }
    }

    /**
     * 按场景筛选
     */
    fun filterByScene(presets: List<Preset>, scene: String): List<Preset> {
        return presets.filter { preset ->
            preset.tags.any { tag ->
                tag.equals(scene, ignoreCase = true) ||
                tag.contains(scene, ignoreCase = true)
            } || preset.description.contains(scene, ignoreCase = true)
        }
    }

    /**
     * 按设备筛选
     */
    fun filterByDevice(presets: List<Preset>, device: String): List<Preset> {
        return presets.filter { preset ->
            preset.supportedDevices.any { supportedDevice ->
                supportedDevice.contains(device, ignoreCase = true)
            }
        }
    }

    /**
     * 按摄影师筛选
     */
    fun filterByPhotographer(presets: List<Preset>, photographer: String): List<Preset> {
        return presets.filter { preset ->
            preset.author?.contains(photographer, ignoreCase = true) == true
        }
    }

    /**
     * 清除搜索历史
     */
    fun clearSearchHistory() {
        _searchHistory.clear()
    }

    /**
     * 添加搜索历史
     */
    private fun addToSearchHistory(query: String) {
        if (query.isBlank()) return
        _searchHistory.remove(query)
        _searchHistory.add(0, query)
        if (_searchHistory.size > 20) {
            _searchHistory.removeAt(_searchHistory.lastIndex)
        }
    }

    /**
     * 匹配预设
     */
    private fun matchPreset(preset: Preset, keywords: List<String>, originalQuery: String): Boolean {
        val searchableText = buildSearchableText(preset)

        // 完整短语匹配（优先级更高）
        val normalizedOriginal = normalizeText(originalQuery)
        if (searchableText.contains(normalizedOriginal)) {
            return true
        }

        // 关键词匹配（任一关键词匹配即可）
        return keywords.any { keyword ->
            searchableText.contains(keyword)
        }
    }

    /**
     * 构建可搜索文本
     */
    private fun buildSearchableText(preset: Preset): String {
        return buildString {
            append(normalizeText(preset.name))
            append(" ")
            append(normalizeText(preset.description))
            append(" ")
            preset.tags.forEach { tag ->
                append(normalizeText(tag))
                append(" ")
            }
            preset.author?.let {
                append(normalizeText(it))
                append(" ")
            }
            preset.supportedDevices.forEach { device ->
                append(normalizeText(device))
                append(" ")
            }
        }.toString()
    }

    /**
     * 计算相关度分数
     */
    private fun calculateRelevanceScore(preset: Preset, query: String, keywords: List<String>): Int {
        var score = 0
        val normalizedQuery = normalizeText(query)

        // 名称完全匹配（最高优先级）
        if (normalizeText(preset.name) == normalizedQuery) {
            score += 100
        } else if (normalizeText(preset.name).contains(normalizedQuery)) {
            score += 50
        }

        // 标签匹配
        preset.tags.forEach { tag ->
            if (normalizeText(tag) == normalizedQuery) {
                score += 30
            } else if (keywords.any { normalizeText(tag).contains(it) }) {
                score += 15
            }
        }

        // 描述匹配
        if (normalizeText(preset.description).contains(normalizedQuery)) {
            score += 20
        }

        // 作者匹配
        preset.author?.let { author ->
            if (normalizeText(author).contains(normalizedQuery)) {
                score += 25
            }
        }

        // 使用次数加成
        score += (preset.useCount / 10).coerceAtMost(20)

        // 收藏加成
        if (preset.isFavorite) {
            score += 10
        }

        return score
    }

    /**
     * 文本标准化（统一大小写、去除重音符号）
     */
    private fun normalizeText(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase()
            .trim()
    }
}

/**
 * 筛选类型枚举
 */
enum class FilterType {
    ALL,           // 全部
    FAVORITES,     // 收藏
    HNCS,          // 哈苏色彩
    FIND_X,        // Find X系列
    RENO,          // Reno系列
    NEW,           // 最新
    TRENDING       // 热门
}
