package com.omaster.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.omaster.app.data.remote.PresetApiService
import com.omaster.app.domain.model.Preset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchDataStore: DataStore<Preferences> by preferencesDataStore(name = "search_preferences")

/**
 * 搜索管理器 - 从远程API获取真实搜索数据
 * 不再使用硬编码的热门搜索词，所有数据来自远程服务器
 */
@Singleton
class SearchManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val presetApiService: PresetApiService
) {
    private val dataStore = context.searchDataStore

    companion object {
        private val SEARCH_HISTORY_KEY = stringPreferencesKey("search_history")
        private const val MAX_HISTORY_SIZE = 20
    }

    /**
     * 获取搜索历史
     */
    fun getSearchHistory(): Flow<List<String>> = dataStore.data.map { preferences ->
        val historyString = preferences[SEARCH_HISTORY_KEY] ?: ""
        if (historyString.isEmpty()) emptyList() else historyString.split(",")
    }

    /**
     * 添加搜索历史
     */
    suspend fun addSearchHistory(query: String) {
        if (query.isBlank()) return
        dataStore.edit { preferences ->
            val currentHistory = preferences[SEARCH_HISTORY_KEY]?.split(",")?.toMutableList() ?: mutableListOf()
            currentHistory.remove(query)
            currentHistory.add(0, query)
            if (currentHistory.size > MAX_HISTORY_SIZE) {
                currentHistory.subList(MAX_HISTORY_SIZE, currentHistory.size).clear()
            }
            preferences[SEARCH_HISTORY_KEY] = currentHistory.joinToString(",")
        }
    }

    /**
     * 清除搜索历史
     */
    suspend fun clearSearchHistory() {
        dataStore.edit { preferences ->
            preferences.remove(SEARCH_HISTORY_KEY)
        }
    }

    /**
     * 从远程API获取热门搜索关键词
     */
    suspend fun getHotSearches(): List<String> = withContext(Dispatchers.IO) {
        try {
            val response = presetApiService.getHotSearches()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "获取热门搜索失败")
            emptyList()
        }
    }

    /**
     * 从远程API获取搜索建议
     */
    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val response = presetApiService.getSearchSuggestions(query)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "获取搜索建议失败")
            emptyList()
        }
    }

    /**
     * 执行搜索
     */
    suspend fun search(query: String): List<Preset> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val response = presetApiService.searchPresets(query)
            if (response.isSuccessful) {
                val results = response.body() ?: emptyList()
                addSearchHistory(query)
                results
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "搜索失败")
            emptyList()
        }
    }

    /**
     * 获取搜索统计信息
     */
    suspend fun getSearchStats(): SearchStats = withContext(Dispatchers.IO) {
        try {
            val response = presetApiService.getSearchStats()
            if (response.isSuccessful) {
                response.body() ?: SearchStats()
            } else {
                SearchStats()
            }
        } catch (e: Exception) {
            Timber.e(e, "获取搜索统计失败")
            SearchStats()
        }
    }
}

/**
 * 搜索统计数据类
 */
data class SearchStats(
    val totalSearches: Int = 0,
    val uniqueQueries: Int = 0,
    val popularQueries: List<String> = emptyList()
)
