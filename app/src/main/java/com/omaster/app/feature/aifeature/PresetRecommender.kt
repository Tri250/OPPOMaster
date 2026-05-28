package com.omaster.app.feature.aifeature

import com.omaster.app.feature.aifeature.model.*
import com.omaster.app.model.Preset
import timber.log.Timber

class PresetRecommender {

    fun recommendPresets(
        sceneAnalysis: SceneAnalysis,
        userProfile: UserProfile,
        allPresets: List<Preset>
    ): List<RecommendedPreset> {
        val recommendations = mutableListOf<RecommendedPreset>()

        val sceneRecommendations = getSceneBasedRecommendations(sceneAnalysis, allPresets)
        recommendations.addAll(sceneRecommendations)

        val userHistoryRecommendations = getUserHistoryRecommendations(userProfile, allPresets)
        recommendations.addAll(userHistoryRecommendations)

        val popularRecommendations = getPopularRecommendations(allPresets)
        recommendations.addAll(popularRecommendations)

        return recommendations
            .distinctBy { it.presetId }
            .sortedByDescending { it.confidenceScore }
            .take(5)
    }

    private fun getSceneBasedRecommendations(
        sceneAnalysis: SceneAnalysis,
        allPresets: List<Preset>
    ): List<RecommendedPreset> {
        return sceneAnalysis.recommendedPresetIds.mapNotNull { presetId ->
            val preset = allPresets.find { it.id == presetId }
            preset?.let {
                RecommendedPreset(
                    presetId = it.id,
                    confidenceScore = 0.8f,
                    reason = "基于场景识别推荐"
                )
            }
        }
    }

    private fun getUserHistoryRecommendations(
        userProfile: UserProfile,
        allPresets: List<Preset>
    ): List<RecommendedPreset> {
        val recentPresets = userProfile.usageHistory
            .sortedByDescending { it.timestamp }
            .take(5)
            .map { it.presetId }

        return recentPresets.mapNotNull { presetId ->
            val preset = allPresets.find { it.id == presetId }
            preset?.let {
                RecommendedPreset(
                    presetId = it.id,
                    confidenceScore = 0.7f,
                    reason = "基于使用历史推荐"
                )
            }
        }
    }

    private fun getPopularRecommendations(
        allPresets: List<Preset>
    ): List<RecommendedPreset> {
        return allPresets
            .take(3)
            .map { preset ->
                RecommendedPreset(
                    presetId = preset.id,
                    confidenceScore = 0.5f,
                    reason = "热门推荐"
                )
            }
    }
}
