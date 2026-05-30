package com.omaster.app.search

import android.content.Context
import com.omaster.app.model.FilterConfig
import com.omaster.app.model.Preset
import com.omaster.app.model.PresetScenes
import com.omaster.app.model.PresetStyles
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.Normalizer
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 搜索筛选管理器
 * 实现 Search-001 至 Search-006 所有功能
 */
@Singleton
class PresetSearchManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 所有预设数据
    private val allPresets = MutableStateFlow<List<Preset>>(emptyList())
    
    // 当前筛选配置
    private val currentFilter = MutableStateFlow(FilterConfig())
    
    // 筛选结果
    private val filteredPresets = MutableStateFlow<List<Preset>>(emptyList())
    
    // 上次搜索时间（性能监控）
    private var lastSearchTime = 0L
    
    // 公开Flow
    val allPresetsFlow: Flow<List<Preset>> = allPresets.asStateFlow()
    val filteredPresetsFlow: Flow<List<Preset>> = filteredPresets.asStateFlow()
    val currentFilterFlow: Flow<FilterConfig> = currentFilter.asStateFlow()
    
    /**
     * 初始化预设数据
     */
    fun initializePresets(presets: List<Preset>) {
        allPresets.value = presets
        applyFilter()
        Timber.d("PresetSearchManager initialized with ${presets.size} presets")
    }
    
    /**
     * Search-001: 按风格分类筛选
     */
    fun setStyle(style: String?) {
        val filter = currentFilter.value.copy(
            selectedStyle = if (style == PresetStyles.ALL) null else style
        )
        currentFilter.value = filter
        applyFilter()
        Timber.d("Style filter set: $style")
    }
    
    /**
     * Search-002: 按场景分类筛选
     */
    fun setScene(scene: String?) {
        val filter = currentFilter.value.copy(
            selectedScene = if (scene == PresetScenes.ALL) null else scene
        )
        currentFilter.value = filter
        applyFilter()
        Timber.d("Scene filter set: $scene")
    }
    
    /**
     * Search-003: 全文搜索功能
     * 支持部分匹配、模糊匹配、错别字匹配
     */
    fun search(query: String) {
        val trimmedQuery = query.trim()
        val filter = currentFilter.value.copy(searchQuery = trimmedQuery)
        currentFilter.value = filter
        applyFilter()
        Timber.d("Search query: \"$trimmedQuery\"")
    }
    
    /**
     * 设置只显示收藏
     */
    fun setFavoriteOnly(only: Boolean) {
        val filter = currentFilter.value.copy(isFavoriteOnly = only)
        currentFilter.value = filter
        applyFilter()
    }
    
    /**
     * 设置只显示新预设
     */
    fun setNewOnly(only: Boolean) {
        val filter = currentFilter.value.copy(isNewOnly = only)
        currentFilter.value = filter
        applyFilter()
    }
    
    /**
     * 重置所有筛选条件
     */
    fun resetFilters() {
        currentFilter.value = FilterConfig()
        applyFilter()
        Timber.d("All filters reset")
    }
    
    /**
     * 应用所有筛选条件
     */
    private fun applyFilter() {
        val startTime = System.currentTimeMillis()
        val filter = currentFilter.value
        val presets = allPresets.value
        
        val result = presets.filter { preset ->
            // 风格筛选 (Search-001)
            val styleMatch = filter.selectedStyle?.let { style ->
                presetMatchesStyle(preset, style)
            } ?: true
            
            // 场景筛选 (Search-002)
            val sceneMatch = filter.selectedScene?.let { scene ->
                presetMatchesScene(preset, scene)
            } ?: true
            
            // 搜索 (Search-003)
            val searchMatch = if (filter.searchQuery.isNotEmpty()) {
                presetMatchesSearch(preset, filter.searchQuery)
            } else {
                true
            }
            
            // 收藏筛选
            val favoriteMatch = if (filter.isFavoriteOnly) {
                preset.isFavorite
            } else {
                true
            }
            
            // 新品筛选
            val newMatch = if (filter.isNewOnly) {
                preset.isNew
            } else {
                true
            }
            
            styleMatch && sceneMatch && searchMatch && favoriteMatch && newMatch
        }
        
        filteredPresets.value = result
        lastSearchTime = System.currentTimeMillis() - startTime
        
        Timber.d("Filter applied: ${result.size} results in ${lastSearchTime}ms")
    }
    
    /**
     * 检查预设是否匹配指定风格
     */
    private fun presetMatchesStyle(preset: Preset, style: String): Boolean {
        // 首先检查 style 字段
        if (preset.style == style) return true
        
        // 检查 category 字段
        if (preset.category == style) return true
        
        // 检查 tags
        if (preset.tags.any { tag ->
            tag.contains(style, ignoreCase = true) ||
            style.contains(tag, ignoreCase = true)
        }) return true
        
        // 检查描述
        if (preset.description?.contains(style, ignoreCase = true) == true) return true
        
        // 检查名称
        if (preset.name.contains(style, ignoreCase = true)) return true
        
        return false
    }
    
    /**
     * 检查预设是否匹配指定场景
     */
    private fun presetMatchesScene(preset: Preset, scene: String): Boolean {
        // 首先检查 scene 字段
        if (preset.scene == scene) return true
        
        // 检查 category 字段
        if (preset.category == scene) return true
        
        // 检查 tags
        if (preset.tags.any { tag ->
            tag.contains(scene, ignoreCase = true) ||
            scene.contains(tag, ignoreCase = true)
        }) return true
        
        // 检查描述
        if (preset.description?.contains(scene, ignoreCase = true) == true) return true
        
        // 检查名称
        if (preset.name.contains(scene, ignoreCase = true)) return true
        
        return false
    }
    
    /**
     * Search-003 & Search-006: 全文搜索匹配
     * 支持部分匹配、模糊匹配、错别字匹配、特殊字符、生僻词
     */
    private fun presetMatchesSearch(preset: Preset, query: String): Boolean {
        val normalizedQuery = normalizeSearch(query)
        
        // 搜索名称
        if (normalizeSearch(preset.name).contains(normalizedQuery)) return true
        
        // 搜索标签
        if (preset.tags.any { normalizeSearch(it).contains(normalizedQuery) }) return true
        
        // 搜索分类
        if (preset.category?.let { normalizeSearch(it).contains(normalizedQuery) } == true) return true
        
        // 搜索风格
        if (preset.style?.let { normalizeSearch(it).contains(normalizedQuery) } == true) return true
        
        // 搜索场景
        if (preset.scene?.let { normalizeSearch(it).contains(normalizedQuery) } == true) return true
        
        // 搜索作者
        if (preset.author?.let { normalizeSearch(it).contains(normalizedQuery) } == true) return true
        
        // 搜索设备型号
        if (normalizeSearch(preset.deviceModel).contains(normalizedQuery)) return true
        
        // 搜索描述
        if (preset.description?.let { normalizeSearch(it).contains(normalizedQuery) } == true) return true
        
        // 模糊匹配 (Search-003: 错别字匹配)
        if (fuzzyMatch(preset.name, query)) return true
        if (preset.tags.any { fuzzyMatch(it, query) }) return true
        
        return false
    }
    
    /**
     * 标准化搜索字符串
     * 支持特殊字符、生僻词
     */
    private fun normalizeSearch(text: String): String {
        // 1. 转为小写
        var normalized = text.lowercase()
        
        // 2. Unicode 规范化 (处理生僻词、特殊字符)
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
        
        // 3. 移除重音符号
        normalized = normalized.replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
        
        // 4. 保留中文、英文、数字、常用特殊字符
        normalized = normalized.replace("[^\\p{L}\\p{N}\\s!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]".toRegex(), "")
        
        // 5. 移除多余空格
        normalized = normalized.trim().replace("\\s+".toRegex(), " ")
        
        return normalized
    }
    
    /**
     * 模糊匹配 (错别字匹配)
     * 使用 Levenshtein 距离算法
     */
    private fun fuzzyMatch(text: String, query: String, threshold: Int = 2): Boolean {
        if (query.length < 2) return false // 短查询不做模糊匹配
        
        val normalizedText = normalizeSearch(text)
        val normalizedQuery = normalizeSearch(query)
        
        // 如果长度差异太大，直接返回 false
        if (Math.abs(normalizedText.length - normalizedQuery.length) > threshold) {
            return false
        }
        
        // 计算 Levenshtein 距离
        val distance = levenshteinDistance(normalizedText, normalizedQuery)
        
        return distance <= threshold
    }
    
    /**
     * Levenshtein 距离算法
     * 计算两个字符串之间的编辑距离
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // 删除
                    dp[i][j - 1] + 1,      // 插入
                    dp[i - 1][j - 1] + cost // 替换
                )
            }
        }
        
        return dp[m][n]
    }
    
    /**
     * 获取上次搜索耗时
     * Search-004: 大量预设下的搜索性能监控
     */
    fun getLastSearchTime(): Long = lastSearchTime
    
    /**
     * 获取当前筛选配置
     */
    fun getCurrentFilter(): FilterConfig = currentFilter.value
    
    /**
     * 获取所有可用的风格 (用于UI显示)
     */
    fun getAvailableStyles(): List<String> {
        val styles = mutableSetOf<String>()
        allPresets.value.forEach { preset ->
            preset.style?.let { styles.add(it) }
            preset.category?.let { styles.add(it) }
        }
        return (PresetStyles.ALL_STYLES.toSet() + styles).toList().sorted()
    }
    
    /**
     * 获取所有可用的场景 (用于UI显示)
     */
    fun getAvailableScenes(): List<String> {
        val scenes = mutableSetOf<String>()
        allPresets.value.forEach { preset ->
            preset.scene?.let { scenes.add(it) }
            preset.category?.let { scenes.add(it) }
        }
        return (PresetScenes.ALL_SCENES.toSet() + scenes).toList().sorted()
    }
    
    /**
     * 获取热门搜索词
     */
    fun getHotSearchQueries(): List<String> {
        return listOf("胶片", "复古", "夜景", "人像", "风景", "清新", "蓝调", "黑白", "美食")
    }
    
    /**
     * 获取搜索建议
     */
    fun getSearchSuggestions(query: String): List<String> {
        if (query.length < 2) return emptyList()
        
        val suggestions = mutableSetOf<String>()
        val normalizedQuery = normalizeSearch(query)
        
        allPresets.value.forEach { preset ->
            // 从预设名称中获取建议
            if (normalizeSearch(preset.name).contains(normalizedQuery)) {
                suggestions.add(preset.name)
            }
            
            // 从标签中获取建议
            preset.tags.forEach { tag ->
                if (normalizeSearch(tag).contains(normalizedQuery)) {
                    suggestions.add(tag)
                }
            }
        }
        
        return suggestions.toList().take(10)
    }
}
