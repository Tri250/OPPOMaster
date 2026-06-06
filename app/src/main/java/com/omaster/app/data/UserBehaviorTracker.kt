package com.omaster.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omaster.app.domain.model.BehaviorType
import com.omaster.app.domain.model.SearchKeyword
import com.omaster.app.domain.model.UserBehavior
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.behaviorDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_behavior")

/**
 * 用户行为追踪器
 * 负责记录和管理用户的各种行为数据，包括浏览、收藏、使用、搜索等
 * 使用 DataStore 进行持久化存储
 */
@Singleton
class UserBehaviorTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    companion object {
        // 浏览记录键
        private val VIEW_HISTORY = stringPreferencesKey("view_history")

        // 使用记录键
        private val USAGE_HISTORY = stringPreferencesKey("usage_history")

        // 收藏记录键
        private val FAVORITE_HISTORY = stringPreferencesKey("favorite_history")

        // 搜索关键词记录键
        private val SEARCH_KEYWORDS = stringPreferencesKey("search_keywords")

        // 预设浏览次数统计
        private val PRESET_VIEW_COUNTS = stringPreferencesKey("preset_view_counts")

        // 预设使用次数统计
        private val PRESET_USAGE_COUNTS = stringPreferencesKey("preset_usage_counts")

        // 用户偏好标签
        private val USER_PREFERENCE_TAGS = stringSetPreferencesKey("user_preference_tags")

        // 用户偏好场景类型
        private val USER_PREFERRED_SCENES = stringSetPreferencesKey("user_preferred_scenes")

        // 最后更新时间
        private val LAST_UPDATE_TIME = longPreferencesKey("last_update_time")

        // 行为记录数量限制
        private const val MAX_BEHAVIOR_RECORDS = 500
        private const val MAX_SEARCH_KEYWORDS = 100
        private const val MAX_PREFERENCE_TAGS = 30
    }

    // ==================== 数据流 ====================

    /**
     * 浏览记录流
     */
    val viewHistoryFlow: Flow<List<UserBehavior>> = context.behaviorDataStore.data
        .map { preferences ->
            preferences[VIEW_HISTORY]?.let { jsonStr ->
                try {
                    json.decodeFromString<List<UserBehavior>>(jsonStr)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }

    /**
     * 使用记录流
     */
    val usageHistoryFlow: Flow<List<UserBehavior>> = context.behaviorDataStore.data
        .map { preferences ->
            preferences[USAGE_HISTORY]?.let { jsonStr ->
                try {
                    json.decodeFromString<List<UserBehavior>>(jsonStr)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }

    /**
     * 收藏记录流
     */
    val favoriteHistoryFlow: Flow<List<UserBehavior>> = context.behaviorDataStore.data
        .map { preferences ->
            preferences[FAVORITE_HISTORY]?.let { jsonStr ->
                try {
                    json.decodeFromString<List<UserBehavior>>(jsonStr)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }

    /**
     * 搜索关键词流
     */
    val searchKeywordsFlow: Flow<List<SearchKeyword>> = context.behaviorDataStore.data
        .map { preferences ->
            preferences[SEARCH_KEYWORDS]?.let { jsonStr ->
                try {
                    json.decodeFromString<List<SearchKeyword>>(jsonStr)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }

    /**
     * 预设浏览次数流
     */
    val presetViewCountsFlow: Flow<Map<String, Int>> = context.behaviorDataStore.data
        .map { preferences ->
            preferences[PRESET_VIEW_COUNTS]?.let { jsonStr ->
                try {
                    json.decodeFromString<Map<String, Int>>(jsonStr)
                } catch (e: Exception) {
                    emptyMap()
                }
            } ?: emptyMap()
        }

    /**
     * 预设使用次数流
     */
    val presetUsageCountsFlow: Flow<Map<String, Int>> = context.behaviorDataStore.data
        .map { preferences ->
            preferences[PRESET_USAGE_COUNTS]?.let { jsonStr ->
                try {
                    json.decodeFromString<Map<String, Int>>(jsonStr)
                } catch (e: Exception) {
                    emptyMap()
                }
            } ?: emptyMap()
        }

    /**
     * 用户偏好标签流
     */
    val userPreferenceTagsFlow: Flow<Set<String>> = context.behaviorDataStore.data
        .map { preferences ->
            preferences[USER_PREFERENCE_TAGS] ?: emptySet()
        }

    /**
     * 用户偏好场景流
     */
    val userPreferredScenesFlow: Flow<Set<String>> = context.behaviorDataStore.data
        .map { preferences ->
            preferences[USER_PREFERRED_SCENES] ?: emptySet()
        }

    // ==================== 行为记录方法 ====================

    /**
     * 记录浏览行为
     * @param presetId 预设ID
     * @param duration 浏览时长（毫秒）
     * @param metadata 额外元数据
     */
    suspend fun recordView(
        presetId: String,
        duration: Long = 0,
        metadata: Map<String, String> = emptyMap()
    ) {
        val behavior = UserBehavior(
            presetId = presetId,
            behaviorType = BehaviorType.VIEW,
            duration = duration,
            metadata = metadata
        )
        addBehaviorToHistory(VIEW_HISTORY, behavior)
        incrementPresetCount(PRESET_VIEW_COUNTS, presetId)
    }

    /**
     * 记录使用行为（应用预设）
     * @param presetId 预设ID
     * @param metadata 额外元数据
     */
    suspend fun recordUsage(
        presetId: String,
        metadata: Map<String, String> = emptyMap()
    ) {
        val behavior = UserBehavior(
            presetId = presetId,
            behaviorType = BehaviorType.APPLY,
            metadata = metadata
        )
        addBehaviorToHistory(USAGE_HISTORY, behavior)
        incrementPresetCount(PRESET_USAGE_COUNTS, presetId)
    }

    /**
     * 记录收藏行为
     * @param presetId 预设ID
     * @param isFavorite 是否收藏（true为收藏，false为取消收藏）
     * @param metadata 额外元数据
     */
    suspend fun recordFavorite(
        presetId: String,
        isFavorite: Boolean,
        metadata: Map<String, String> = emptyMap()
    ) {
        val behavior = UserBehavior(
            presetId = presetId,
            behaviorType = if (isFavorite) BehaviorType.FAVORITE else BehaviorType.UNFAVORITE,
            metadata = metadata
        )
        addBehaviorToHistory(FAVORITE_HISTORY, behavior)
    }

    /**
     * 记录下载行为
     * @param presetId 预设ID
     * @param metadata 额外元数据
     */
    suspend fun recordDownload(
        presetId: String,
        metadata: Map<String, String> = emptyMap()
    ) {
        val behavior = UserBehavior(
            presetId = presetId,
            behaviorType = BehaviorType.DOWNLOAD,
            metadata = metadata
        )
        addBehaviorToHistory(USAGE_HISTORY, behavior)
    }

    /**
     * 记录分享行为
     * @param presetId 预设ID
     * @param metadata 额外元数据
     */
    suspend fun recordShare(
        presetId: String,
        metadata: Map<String, String> = emptyMap()
    ) {
        val behavior = UserBehavior(
            presetId = presetId,
            behaviorType = BehaviorType.SHARE,
            metadata = metadata
        )
        addBehaviorToHistory(USAGE_HISTORY, behavior)
    }

    /**
     * 记录搜索关键词
     * @param keyword 搜索关键词
     * @param resultCount 搜索结果数量
     * @param clickedPresetId 用户点击的预设ID
     */
    suspend fun recordSearch(
        keyword: String,
        resultCount: Int = 0,
        clickedPresetId: String? = null
    ) {
        if (keyword.isBlank()) return

        val searchKeyword = SearchKeyword(
            keyword = keyword.trim(),
            resultCount = resultCount,
            clickedPresetId = clickedPresetId
        )

        context.behaviorDataStore.edit { preferences ->
            val currentList = preferences[SEARCH_KEYWORDS]?.let { jsonStr ->
                try {
                    json.decodeFromString<MutableList<SearchKeyword>>(jsonStr)
                } catch (e: Exception) {
                    mutableListOf()
                }
            } ?: mutableListOf()

            // 添加到开头
            currentList.add(0, searchKeyword)

            // 限制数量并去重
            val uniqueList = currentList
                .distinctBy { it.keyword }
                .take(MAX_SEARCH_KEYWORDS)

            preferences[SEARCH_KEYWORDS] = json.encodeToString(uniqueList)
            preferences[LAST_UPDATE_TIME] = System.currentTimeMillis()
        }
    }

    // ==================== 偏好管理方法 ====================

    /**
     * 更新用户偏好标签
     * @param tags 标签列表
     */
    suspend fun updatePreferenceTags(tags: List<String>) {
        context.behaviorDataStore.edit { preferences ->
            val currentTags = preferences[USER_PREFERENCE_TAGS]?.toMutableSet() ?: mutableSetOf()
            currentTags.addAll(tags)
            preferences[USER_PREFERENCE_TAGS] = currentTags.take(MAX_PREFERENCE_TAGS).toSet()
            preferences[LAST_UPDATE_TIME] = System.currentTimeMillis()
        }
    }

    /**
     * 更新用户偏好场景类型
     * @param sceneTypes 场景类型列表
     */
    suspend fun updatePreferredScenes(sceneTypes: List<String>) {
        context.behaviorDataStore.edit { preferences ->
            val currentScenes = preferences[USER_PREFERRED_SCENES]?.toMutableSet() ?: mutableSetOf()
            currentScenes.addAll(sceneTypes)
            preferences[USER_PREFERRED_SCENES] = currentScenes
            preferences[LAST_UPDATE_TIME] = System.currentTimeMillis()
        }
    }

    /**
     * 添加单个偏好标签
     * @param tag 标签
     */
    suspend fun addPreferenceTag(tag: String) {
        if (tag.isBlank()) return
        context.behaviorDataStore.edit { preferences ->
            val currentTags = preferences[USER_PREFERENCE_TAGS]?.toMutableSet() ?: mutableSetOf()
            currentTags.add(tag.trim())
            preferences[USER_PREFERENCE_TAGS] = currentTags.take(MAX_PREFERENCE_TAGS).toSet()
            preferences[LAST_UPDATE_TIME] = System.currentTimeMillis()
        }
    }

    /**
     * 移除偏好标签
     * @param tag 标签
     */
    suspend fun removePreferenceTag(tag: String) {
        context.behaviorDataStore.edit { preferences ->
            val currentTags = preferences[USER_PREFERENCE_TAGS]?.toMutableSet() ?: mutableSetOf()
            currentTags.remove(tag)
            preferences[USER_PREFERENCE_TAGS] = currentTags
            preferences[LAST_UPDATE_TIME] = System.currentTimeMillis()
        }
    }

    // ==================== 查询方法 ====================

    /**
     * 获取预设的浏览次数
     * @param presetId 预设ID
     * @return 浏览次数
     */
    suspend fun getPresetViewCount(presetId: String): Int {
        return presetViewCountsFlow.first()[presetId] ?: 0
    }

    /**
     * 获取预设的使用次数
     * @param presetId 预设ID
     * @return 使用次数
     */
    suspend fun getPresetUsageCount(presetId: String): Int {
        return presetUsageCountsFlow.first()[presetId] ?: 0
    }

    /**
     * 获取预设的综合分数
     * 基于浏览、使用、收藏等行为计算
     * @param presetId 预设ID
     * @return 综合分数
     */
    suspend fun getPresetScore(presetId: String): Float {
        val viewCount = getPresetViewCount(presetId)
        val usageCount = getPresetUsageCount(presetId)

        // 获取收藏状态
        val favoriteHistory = favoriteHistoryFlow.first()
        val isFavorited = favoriteHistory.any {
            it.presetId == presetId && it.behaviorType == BehaviorType.FAVORITE
        }
        val isUnfavorited = favoriteHistory.any {
            it.presetId == presetId && it.behaviorType == BehaviorType.UNFAVORITE
        }
        val favoriteScore = when {
            isFavorited && !isUnfavorited -> 1.0f
            isUnfavorited -> -0.5f
            else -> 0f
        }

        // 权重：使用40%，收藏30%，浏览30%
        return (usageCount * 0.4f) + (favoriteScore * 30f) + (viewCount * 0.3f)
    }

    /**
     * 获取用户的所有行为记录
     * @return 所有行为记录
     */
    suspend fun getAllBehaviors(): List<UserBehavior> {
        val views = viewHistoryFlow.first()
        val usages = usageHistoryFlow.first()
        val favorites = favoriteHistoryFlow.first()
        return (views + usages + favorites).sortedByDescending { it.timestamp }
    }

    /**
     * 获取最近的行为记录
     * @param days 最近多少天
     * @return 行为记录列表
     */
    suspend fun getRecentBehaviors(days: Int): List<UserBehavior> {
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000)
        return getAllBehaviors().filter { it.timestamp >= cutoffTime }
    }

    /**
     * 获取用户最常使用的预设ID列表
     * @param limit 返回数量限制
     * @return 预设ID列表
     */
    suspend fun getTopUsedPresets(limit: Int = 10): List<String> {
        return presetUsageCountsFlow.first()
            .toList()
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    /**
     * 获取用户最常浏览的预设ID列表
     * @param limit 返回数量限制
     * @return 预设ID列表
     */
    suspend fun getTopViewedPresets(limit: Int = 10): List<String> {
        return presetViewCountsFlow.first()
            .toList()
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    /**
     * 获取热门搜索关键词
     * @param limit 返回数量限制
     * @return 关键词列表
     */
    suspend fun getPopularSearchKeywords(limit: Int = 10): List<String> {
        return searchKeywordsFlow.first()
            .map { it.keyword }
            .distinct()
            .take(limit)
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 添加行为到历史记录
     */
    private suspend fun addBehaviorToHistory(
        key: Preferences.Key<String>,
        behavior: UserBehavior
    ) {
        context.behaviorDataStore.edit { preferences ->
            val currentList = preferences[key]?.let { jsonStr ->
                try {
                    json.decodeFromString<MutableList<UserBehavior>>(jsonStr)
                } catch (e: Exception) {
                    mutableListOf()
                }
            } ?: mutableListOf()

            // 添加到开头
            currentList.add(0, behavior)

            // 限制数量
            while (currentList.size > MAX_BEHAVIOR_RECORDS) {
                currentList.removeAt(currentList.size - 1)
            }

            preferences[key] = json.encodeToString(currentList)
            preferences[LAST_UPDATE_TIME] = System.currentTimeMillis()
        }
    }

    /**
     * 增加预设计数
     */
    private suspend fun incrementPresetCount(
        key: Preferences.Key<String>,
        presetId: String
    ) {
        context.behaviorDataStore.edit { preferences ->
            val currentMap = preferences[key]?.let { jsonStr ->
                try {
                    json.decodeFromString<MutableMap<String, Int>>(jsonStr)
                } catch (e: Exception) {
                    mutableMapOf()
                }
            } ?: mutableMapOf()

            currentMap[presetId] = (currentMap[presetId] ?: 0) + 1
            preferences[key] = json.encodeToString(currentMap)
        }
    }

    // ==================== 数据清理方法 ====================

    /**
     * 清空所有行为数据
     */
    suspend fun clearAllData() {
        context.behaviorDataStore.edit { preferences ->
            preferences.remove(VIEW_HISTORY)
            preferences.remove(USAGE_HISTORY)
            preferences.remove(FAVORITE_HISTORY)
            preferences.remove(SEARCH_KEYWORDS)
            preferences.remove(PRESET_VIEW_COUNTS)
            preferences.remove(PRESET_USAGE_COUNTS)
            preferences.remove(USER_PREFERENCE_TAGS)
            preferences.remove(USER_PREFERRED_SCENES)
            preferences[LAST_UPDATE_TIME] = System.currentTimeMillis()
        }
    }

    /**
     * 清理过期数据
     * @param days 保留最近多少天的数据
     */
    suspend fun cleanOldData(days: Int) {
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000)

        context.behaviorDataStore.edit { preferences ->
            // 清理浏览记录
            preferences[VIEW_HISTORY]?.let { jsonStr ->
                try {
                    val list = json.decodeFromString<MutableList<UserBehavior>>(jsonStr)
                    val filtered = list.filter { it.timestamp >= cutoffTime }
                    preferences[VIEW_HISTORY] = json.encodeToString(filtered)
                } catch (e: Exception) { }
            }

            // 清理使用记录
            preferences[USAGE_HISTORY]?.let { jsonStr ->
                try {
                    val list = json.decodeFromString<MutableList<UserBehavior>>(jsonStr)
                    val filtered = list.filter { it.timestamp >= cutoffTime }
                    preferences[USAGE_HISTORY] = json.encodeToString(filtered)
                } catch (e: Exception) { }
            }

            // 清理搜索记录
            preferences[SEARCH_KEYWORDS]?.let { jsonStr ->
                try {
                    val list = json.decodeFromString<MutableList<SearchKeyword>>(jsonStr)
                    val filtered = list.filter { it.timestamp >= cutoffTime }
                    preferences[SEARCH_KEYWORDS] = json.encodeToString(filtered)
                } catch (e: Exception) { }
            }
        }
    }
}
