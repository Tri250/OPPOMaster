package com.omaster.app.service

import android.content.Context
import android.graphics.Bitmap
import com.omaster.app.data.PresetRepository
import com.omaster.app.model.Preset
import com.omaster.app.model.PresetRecommendation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            val scenes = detectScenes(image)
            val colors = analyzeColors(image)
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val presets = presetRepository.getAllPresets()

            presets.map { preset ->
                PresetRecommendation(
                    preset = preset,
                    score = calculateScore(preset, scenes, colors, hour)
                )
            }.sortedByDescending { it.score }
                .take(3)
        }
    }

    private fun detectScenes(image: Bitmap): List<SceneType> {
        return listOf(SceneType.LANDSCAPE, SceneType.PORTRAIT)
    }

    private fun analyzeColors(image: Bitmap): List<Int> {
        return listOf(0xFF6200EE.toInt(), 0xFF03DAC6.toInt())
    }

    private fun calculateScore(
        preset: Preset,
        scenes: List<SceneType>,
        colors: List<Int>,
        hour: Int
    ): Float {
        val sceneScore = sceneMatchScore(preset, scenes) * 0.40f
        val colorScore = colorMatchScore(preset, colors) * 0.30f
        val timeScore = timeMatchScore(preset, hour) * 0.15f
        val preferenceScore = userPreferenceScore(preset) * 0.15f

        return sceneScore + colorScore + timeScore + preferenceScore
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
        return 0.7f
    }

    private fun timeMatchScore(preset: Preset, hour: Int): Float {
        return when {
            hour in 6..10 && preset.name.contains("sunrise", true) -> 1f
            hour in 17..20 && preset.name.contains("sunset", true) -> 1f
            hour in 21..5 || hour in 0..5 && preset.name.contains("night", true) -> 1f
            else -> 0.5f
        }
    }

    private fun userPreferenceScore(preset: Preset): Float {
        return if (preset.isFavorite) 1f else 0.5f + (preset.usageCount / 100f).coerceAtMost(0.5f)
    }
}
