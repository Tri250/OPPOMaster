package com.omaster.app.service

import android.content.Context
import android.graphics.Bitmap
import com.omaster.app.data.PresetRepository
import com.omaster.app.model.Preset
import com.omaster.app.model.PresetRecommendation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar

class SceneRecognitionEngine(
    private val context: Context,
    private val presetRepository: PresetRepository
) {

    enum class SceneType {
        PORTRAIT,
        LANDSCAPE,
        FOOD,
        NIGHT,
        STREET,
        MACRO
    }

    suspend fun recommend(image: Bitmap): List<PresetRecommendation> {
        return withContext(Dispatchers.Default) {
            try {
                val scenes = detectScenes(image)
                val colors = analyzeColors(image)
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val presets = presetRepository.getAllPresets()

                if (presets.isEmpty()) {
                    Timber.w("No presets available for scene recommendation")
                    return@withContext emptyList()
                }

                val recommendations = presets.map { preset ->
                    PresetRecommendation(
                        preset = preset,
                        score = calculateScore(preset, scenes, colors, hour)
                    )
                }.sortedByDescending { it.score }
                    .take(3)

                Timber.d("Generated ${recommendations.size} scene recommendations")
                recommendations
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate scene recommendations")
                emptyList()
            }
        }
    }

    private fun detectScenes(image: Bitmap): List<SceneType> {
        return try {
            if (image.width <= 0 || image.height <= 0) {
                Timber.w("Invalid image dimensions: ${image.width}x${image.height}")
                return listOf(SceneType.LANDSCAPE)
            }
            listOf(SceneType.LANDSCAPE, SceneType.PORTRAIT)
        } catch (e: Exception) {
            Timber.e(e, "Failed to detect scenes")
            listOf(SceneType.LANDSCAPE)
        }
    }

    private fun analyzeColors(image: Bitmap): List<Int> {
        return try {
            if (image.width <= 0 || image.height <= 0) {
                Timber.w("Invalid image dimensions for color analysis")
                return emptyList()
            }
            listOf(0xFF6200EE.toInt(), 0xFF03DAC6.toInt())
        } catch (e: Exception) {
            Timber.e(e, "Failed to analyze colors")
            emptyList()
        }
    }

    private fun calculateScore(
        preset: Preset,
        scenes: List<SceneType>,
        colors: List<Int>,
        hour: Int
    ): Float {
        return try {
            val sceneScore = sceneMatchScore(preset, scenes) * 0.40f
            val colorScore = colorMatchScore(preset, colors) * 0.30f
            val timeScore = timeMatchScore(preset, hour) * 0.15f
            val preferenceScore = userPreferenceScore(preset) * 0.15f

            val totalScore = sceneScore + colorScore + timeScore + preferenceScore
            totalScore.coerceIn(0f, 1f)
        } catch (e: Exception) {
            Timber.w("Failed to calculate score for preset ${preset.name}: ${e.message}")
            0.5f
        }
    }

    private fun sceneMatchScore(preset: Preset, scenes: List<SceneType>): Float {
        val tags = preset.cameraParams?.sceneTags ?: emptyList()
        if (tags.isEmpty()) return 0.5f

        var score = 0f
        scenes.forEach { scene ->
            if (tags.contains(scene.name.lowercase())) {
                score += 1f / scenes.size
            }
        }
        return score
    }

    private fun colorMatchScore(preset: Preset, colors: List<Int>): Float {
        if (colors.isEmpty()) return 0.5f
        return 0.7f
    }

    private fun timeMatchScore(preset: Preset, hour: Int): Float {
        return try {
            when {
                hour in 6..10 && preset.name.contains("sunrise", ignoreCase = true) -> 1f
                hour in 17..20 && preset.name.contains("sunset", ignoreCase = true) -> 1f
                hour >= 21 || hour <= 5 -> {
                    if (preset.name.contains("night", ignoreCase = true)) 1f else 0.5f
                }
                else -> 0.5f
            }
        } catch (e: Exception) {
            Timber.w("Failed to calculate time score: ${e.message}")
            0.5f
        }
    }

    private fun userPreferenceScore(preset: Preset): Float {
        return try {
            val favoriteBoost = if (preset.isFavorite) 1f else 0.5f
            val usageBoost = (preset.usageCount / 100f).coerceAtMost(0.5f)
            favoriteBoost + usageBoost
        } catch (e: Exception) {
            Timber.w("Failed to calculate preference score: ${e.message}")
            0.5f
        }
    }
}
