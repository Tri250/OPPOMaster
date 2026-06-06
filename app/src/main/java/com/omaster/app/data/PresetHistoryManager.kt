package com.omaster.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omaster.app.domain.model.Preset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "preset_history")

/**
 * 预设使用历史记录管理器
 * 记录预设的使用时间、次数，提供最近使用列表和使用统计数据
 */
@Singleton
class PresetHistoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object HistoryKeys {
        val HISTORY_ENTRIES = stringPreferencesKey("history_entries")
        val TOTAL_USAGE_COUNT = intPreferencesKey("total_usage_count")
        val LAST_CLEANUP_TIME = longPreferencesKey("last_cleanup_time")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 历史记录条目数据类
     */
    @Serializable
    data class HistoryEntry(
        val presetId: String,
        val presetName: String,
        val timestamp: Long,
        val deviceModel: String? = null,
        val coverUrl: String? = null
    ) {
        /**
         * 获取格式化的时间字符串
         */
        fun getFormattedTime(): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60_000 -> "刚刚"
                diff < 3_600_000 -> "${diff / 60_000}分钟前"
                diff < 86_400_000 -> "${diff / 3_600_000}小时前"
                diff < 604_800_000 -> "${diff / 86_400_000}天前"
                else -> {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(timestamp))
                }
            }
        }
    }

    /**
     * 使用统计数据
     */
    data class UsageStatistics(
        val totalUsageCount: Int,           // 总使用次数
        val uniquePresetCount: Int,         // 使用过的预设数量
        val mostUsedPresetId: String?,      // 最常使用的预设ID
        val mostUsedPresetName: String?,    // 最常使用的预设名称
        val mostUsedCount: Int,             // 最常使用预设的次数
        val dailyAverage: Float,            // 日均使用次数
        val weeklyUsage: Map<String, Int>,  // 每周使用分布
        val favoriteCategory: String?       // 最喜欢的分类
    )

    /**
     * 历史记录流
     */
    val historyEntries: Flow<List<HistoryEntry>> = context.historyDataStore.data
        .map { preferences ->
            val historyJson = preferences[HistoryKeys.HISTORY_ENTRIES] ?: "[]"
            try {
                json.decodeFromString<List<HistoryEntry>>(historyJson)
            } catch (e: Exception) {
                emptyList()
            }
        }

    /**
     * 最近使用的预设（去重，按时间倒序）
     */
    val recentPresets: Flow<List<HistoryEntry>> = historyEntries
        .map { entries ->
            entries
                .sortedByDescending { it.timestamp }
                .distinctBy { it.presetId }
                .take(20)
        }

    /**
     * 总使用次数
     */
    val totalUsageCount: Flow<Int> = context.historyDataStore.data
        .map { preferences ->
            preferences[HistoryKeys.TOTAL_USAGE_COUNT] ?: 0
        }

    /**
     * 记录预设使用
     */
    suspend fun recordPresetUsage(preset: Preset) {
        context.historyDataStore.edit { preferences ->
            // 获取现有历史记录
            val currentHistoryJson = preferences[HistoryKeys.HISTORY_ENTRIES] ?: "[]"
            val currentHistory = try {
                json.decodeFromString<MutableList<HistoryEntry>>(currentHistoryJson)
            } catch (e: Exception) {
                mutableListOf()
            }

            // 创建新记录
            val newEntry = HistoryEntry(
                presetId = preset.id,
                presetName = preset.name,
                timestamp = System.currentTimeMillis(),
                deviceModel = preset.deviceModel,
                coverUrl = preset.coverUrl
            )

            // 添加到历史记录
            currentHistory.add(newEntry)

            // 限制历史记录数量（保留最近500条）
            if (currentHistory.size > 500) {
                currentHistory.removeAt(0)
            }

            // 保存更新后的历史记录
            preferences[HistoryKeys.HISTORY_ENTRIES] = json.encodeToString(currentHistory)

            // 更新总使用次数
            val currentCount = preferences[HistoryKeys.TOTAL_USAGE_COUNT] ?: 0
            preferences[HistoryKeys.TOTAL_USAGE_COUNT] = currentCount + 1
        }
    }

    /**
     * 快速记录预设使用（仅通过ID和名称）
     */
    suspend fun recordPresetUsage(presetId: String, presetName: String) {
        context.historyDataStore.edit { preferences ->
            val currentHistoryJson = preferences[HistoryKeys.HISTORY_ENTRIES] ?: "[]"
            val currentHistory = try {
                json.decodeFromString<MutableList<HistoryEntry>>(currentHistoryJson)
            } catch (e: Exception) {
                mutableListOf()
            }

            val newEntry = HistoryEntry(
                presetId = presetId,
                presetName = presetName,
                timestamp = System.currentTimeMillis()
            )

            currentHistory.add(newEntry)

            if (currentHistory.size > 500) {
                currentHistory.removeAt(0)
            }

            preferences[HistoryKeys.HISTORY_ENTRIES] = json.encodeToString(currentHistory)

            val currentCount = preferences[HistoryKeys.TOTAL_USAGE_COUNT] ?: 0
            preferences[HistoryKeys.TOTAL_USAGE_COUNT] = currentCount + 1
        }
    }

    /**
     * 获取特定预设的使用次数
     */
    suspend fun getPresetUsageCount(presetId: String): Int {
        val entries = historyEntries.first()
        return entries.count { it.presetId == presetId }
    }

    /**
     * 获取特定预设的使用历史
     */
    suspend fun getPresetHistory(presetId: String): List<HistoryEntry> {
        val entries = historyEntries.first()
        return entries.filter { it.presetId == presetId }
            .sortedByDescending { it.timestamp }
    }

    /**
     * 获取使用统计数据
     */
    suspend fun getUsageStatistics(): UsageStatistics {
        val entries = historyEntries.first()

        if (entries.isEmpty()) {
            return UsageStatistics(
                totalUsageCount = 0,
                uniquePresetCount = 0,
                mostUsedPresetId = null,
                mostUsedPresetName = null,
                mostUsedCount = 0,
                dailyAverage = 0f,
                weeklyUsage = emptyMap(),
                favoriteCategory = null
            )
        }

        // 统计每个预设的使用次数
        val presetUsageCount = entries.groupingBy { it.presetId }.eachCount()
        val mostUsedEntry = presetUsageCount.maxByOrNull { it.value }

        // 获取最常使用预设的名称
        val mostUsedPresetName = mostUsedEntry?.key?.let { presetId ->
            entries.find { it.presetId == presetId }?.presetName
        }

        // 计算每周使用分布
        val calendar = java.util.Calendar.getInstance()
        val weeklyUsage = mutableMapOf<String, Int>()
        val weekDays = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

        entries.forEach { entry ->
            calendar.timeInMillis = entry.timestamp
            val dayOfWeek = weekDays[calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1]
            weeklyUsage[dayOfWeek] = (weeklyUsage[dayOfWeek] ?: 0) + 1
        }

        // 计算日均使用次数
        val firstUsageTime = entries.minOfOrNull { it.timestamp } ?: System.currentTimeMillis()
        val daysSinceFirstUsage = ((System.currentTimeMillis() - firstUsageTime) / 86_400_000).coerceAtLeast(1)
        val dailyAverage = entries.size.toFloat() / daysSinceFirstUsage

        return UsageStatistics(
            totalUsageCount = entries.size,
            uniquePresetCount = presetUsageCount.size,
            mostUsedPresetId = mostUsedEntry?.key,
            mostUsedPresetName = mostUsedPresetName,
            mostUsedCount = mostUsedEntry?.value ?: 0,
            dailyAverage = dailyAverage,
            weeklyUsage = weeklyUsage,
            favoriteCategory = null
        )
    }

    /**
     * 获取指定时间范围内的历史记录
     */
    suspend fun getHistoryInTimeRange(startTime: Long, endTime: Long): List<HistoryEntry> {
        val entries = historyEntries.first()
        return entries.filter { it.timestamp in startTime..endTime }
            .sortedByDescending { it.timestamp }
    }

    /**
     * 获取今天的使用记录
     */
    suspend fun getTodayHistory(): List<HistoryEntry> {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + 86_400_000

        return getHistoryInTimeRange(startOfDay, endOfDay)
    }

    /**
     * 获取本周的使用记录
     */
    suspend fun getThisWeekHistory(): List<HistoryEntry> {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        val startOfWeek = calendar.timeInMillis
        val endOfWeek = startOfWeek + 604_800_000

        return getHistoryInTimeRange(startOfWeek, endOfWeek)
    }

    /**
     * 清空所有历史记录
     */
    suspend fun clearAllHistory() {
        context.historyDataStore.edit { preferences ->
            preferences[HistoryKeys.HISTORY_ENTRIES] = "[]"
            preferences[HistoryKeys.TOTAL_USAGE_COUNT] = 0
            preferences[HistoryKeys.LAST_CLEANUP_TIME] = System.currentTimeMillis()
        }
    }

    /**
     * 删除指定预设的历史记录
     */
    suspend fun clearPresetHistory(presetId: String) {
        context.historyDataStore.edit { preferences ->
            val currentHistoryJson = preferences[HistoryKeys.HISTORY_ENTRIES] ?: "[]"
            val currentHistory = try {
                json.decodeFromString<MutableList<HistoryEntry>>(currentHistoryJson)
            } catch (e: Exception) {
                mutableListOf()
            }

            val removedCount = currentHistory.removeAll { it.presetId == presetId }

            if (removedCount) {
                preferences[HistoryKeys.HISTORY_ENTRIES] = json.encodeToString(currentHistory)

                val currentCount = preferences[HistoryKeys.TOTAL_USAGE_COUNT] ?: 0
                preferences[HistoryKeys.TOTAL_USAGE_COUNT] = (currentCount - removedCount).coerceAtLeast(0)
            }
        }
    }

    /**
     * 清理过期历史记录（保留最近N天）
     */
    suspend fun cleanupOldHistory(daysToKeep: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 86_400_000L)

        context.historyDataStore.edit { preferences ->
            val currentHistoryJson = preferences[HistoryKeys.HISTORY_ENTRIES] ?: "[]"
            val currentHistory = try {
                json.decodeFromString<MutableList<HistoryEntry>>(currentHistoryJson)
            } catch (e: Exception) {
                mutableListOf()
            }

            val originalSize = currentHistory.size
            currentHistory.removeAll { it.timestamp < cutoffTime }
            val removedCount = originalSize - currentHistory.size

            if (removedCount > 0) {
                preferences[HistoryKeys.HISTORY_ENTRIES] = json.encodeToString(currentHistory)

                val currentCount = preferences[HistoryKeys.TOTAL_USAGE_COUNT] ?: 0
                preferences[HistoryKeys.TOTAL_USAGE_COUNT] = (currentCount - removedCount).coerceAtLeast(0)
            }

            preferences[HistoryKeys.LAST_CLEANUP_TIME] = System.currentTimeMillis()
        }
    }

    /**
     * 检查是否需要清理（每周自动清理一次）
     */
    suspend fun shouldCleanup(): Boolean {
        val lastCleanup = context.historyDataStore.data
            .map { it[HistoryKeys.LAST_CLEANUP_TIME] ?: 0 }
            .first()

        return System.currentTimeMillis() - lastCleanup > 604_800_000 // 7天
    }

    /**
     * 获取最近使用的预设ID列表（用于快速访问）
     */
    suspend fun getRecentPresetIds(limit: Int = 10): List<String> {
        return recentPresets.first()
            .take(limit)
            .map { it.presetId }
    }

    /**
     * 搜索历史记录
     */
    suspend fun searchHistory(query: String): List<HistoryEntry> {
        if (query.isBlank()) return emptyList()

        val entries = historyEntries.first()
        return entries.filter {
            it.presetName.contains(query, ignoreCase = true) ||
            it.deviceModel?.contains(query, ignoreCase = true) == true
        }.sortedByDescending { it.timestamp }
    }
}
