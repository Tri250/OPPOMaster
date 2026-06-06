package com.omaster.app.data

import android.content.Context
import com.omaster.app.data.remote.PresetApiService
import com.omaster.app.domain.model.Preset
import com.omaster.app.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 推荐引擎 - 基于用户行为和远程API数据的真实推荐系统
 * 不再使用模拟数据，所有推荐来自真实用户行为分析和远程服务器
 */
@Singleton
class RecommendationEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val presetApiService: PresetApiService,
    private val userBehaviorTracker: UserBehaviorTracker,
    private val favoritesManager: FavoritesManager
) {

    /**
     * 获取个性化推荐
     */
    suspend fun getPersonalizedRecommendations(userId: String, limit: Int = 10): List<Preset> = withContext(Dispatchers.IO) {
        try {
            val response = presetApiService.getPersonalizedRecommendations(userId, limit)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "获取个性化推荐失败")
            emptyList()
        }
    }

    /**
     * 获取热门推荐
     */
    suspend fun getTrendingRecommendations(limit: Int = 10): List<Preset> = withContext(Dispatchers.IO) {
        try {
            val response = presetApiService.getTrendingPresets(limit)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "获取热门推荐失败")
            emptyList()
        }
    }

    /**
     * 获取相似预设推荐
     */
    suspend fun getSimilarPresets(presetId: String, limit: Int = 5): List<Preset> = withContext(Dispatchers.IO) {
        try {
            val response = presetApiService.getSimilarPresets(presetId, limit)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "获取相似预设失败")
            emptyList()
        }
    }

    /**
     * 获取基于用户行为的推荐
     */
    suspend fun getBehaviorBasedRecommendations(limit: Int = 10): List<Preset> = withContext(Dispatchers.IO) {
        try {
            val userProfile = userBehaviorTracker.getUserProfile()
            val favoriteIds = favoritesManager.getFavorites().first()
            
            val response = presetApiService.getBehaviorBasedRecommendations(
                userProfile = userProfile,
                favoriteIds = favoriteIds,
                limit = limit
            )
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "获取行为推荐失败")
            emptyList()
        }
    }

    /**
     * 获取新品推荐
     */
    suspend fun getNewArrivals(limit: Int = 10): List<Preset> = withContext(Dispatchers.IO) {
        try {
            val response = presetApiService.getNewArrivals(limit)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "获取新品推荐失败")
            emptyList()
        }
    }

    /**
     * 获取编辑推荐
     */
    suspend fun getEditorPicks(limit: Int = 10): List<Preset> = withContext(Dispatchers.IO) {
        try {
            val response = presetApiService.getEditorPicks(limit)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "获取编辑推荐失败")
            emptyList()
        }
    }

    /**
     * 获取推荐理由
     */
    suspend fun getRecommendationReason(presetId: String, userId: String): String = withContext(Dispatchers.IO) {
        try {
            val response = presetApiService.getRecommendationReason(presetId, userId)
            if (response.isSuccessful) {
                response.body() ?: "根据您的偏好推荐"
            } else {
                "根据您的偏好推荐"
            }
        } catch (e: Exception) {
            Timber.e(e, "获取推荐理由失败")
            "根据您的偏好推荐"
        }
    }

    /**
     * 记录推荐反馈
     */
    suspend fun recordRecommendationFeedback(presetId: String, userId: String, action: String) {
        withContext(Dispatchers.IO) {
            try {
                presetApiService.recordRecommendationFeedback(presetId, userId, action)
            } catch (e: Exception) {
                Timber.e(e, "记录推荐反馈失败")
            }
        }
    }

    /**
     * 获取推荐统计
     */
    suspend fun getRecommendationStats(userId: String): RecommendationStats = withContext(Dispatchers.IO) {
        try {
            val response = presetApiService.getRecommendationStats(userId)
            if (response.isSuccessful) {
                response.body() ?: RecommendationStats()
            } else {
                RecommendationStats()
            }
        } catch (e: Exception) {
            Timber.e(e, "获取推荐统计失败")
            RecommendationStats()
        }
    }
}

/**
 * 推荐统计数据类
 */
data class RecommendationStats(
    val totalRecommendations: Int = 0,
    val clickRate: Float = 0f,
    val conversionRate: Float = 0f,
    val topCategories: List<String> = emptyList()
)
