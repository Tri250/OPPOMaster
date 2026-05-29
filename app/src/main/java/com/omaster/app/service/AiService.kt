package com.omaster.app.service

import com.omaster.app.model.AiAdjustmentParams
import com.omaster.app.model.Preset
import com.omaster.app.model.SceneType
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

class AiService {
    
    suspend fun detectScene(imageUri: String? = null): SceneType {
        delay(350)
        
        val sceneWeights = mutableMapOf<SceneType, Double>()
        
        SceneType.values().filter { it != SceneType.UNKNOWN }.forEach { scene ->
            var weight = 0.0
            scene.keywords.forEachIndexed { index, keyword ->
                val baseWeight = when {
                    keyword.length <= 2 -> 0.4
                    keyword.length <= 4 -> 0.6
                    keyword.length <= 6 -> 0.8
                    else -> 1.2
                }
                val positionBonus = (scene.keywords.size - index) * 0.1
                weight += baseWeight + positionBonus
            }
            sceneWeights[scene] = weight
        }
        
        val totalWeight = sceneWeights.values.sum()
        val random = Math.random() * totalWeight
        
        var cumulative = 0.0
        for ((scene, weight) in sceneWeights) {
            cumulative += weight
            if (random <= cumulative) {
                return scene
            }
        }
        
        return SceneType.PORTRAIT
    }
    
    suspend fun detectSceneAdvanced(imageUri: String): SceneDetectionResult {
        delay(380)
        
        val topScenes = mutableListOf<SceneMatch>()
        
        SceneType.values().filter { it != SceneType.UNKNOWN }.forEach { scene ->
            val confidence = calculateSceneConfidence(scene)
            topScenes.add(SceneMatch(scene, confidence))
        }
        
        topScenes.sortByDescending { it.confidence }
        val primary = topScenes.firstOrNull()?.scene ?: SceneType.UNKNOWN
        val secondary = topScenes.getOrNull(1)?.scene
        
        return SceneDetectionResult(
            primaryScene = primary,
            confidence = topScenes.firstOrNull()?.confidence ?: 0.0,
            secondaryScene = secondary,
            allMatches = topScenes.take(5),
            recommendedParams = primary.recommendedParams
        )
    }
    
    private fun calculateSceneConfidence(scene: SceneType): Double {
        var baseConfidence = 55.0
        
        scene.keywords.forEachIndexed { index, keyword ->
            val positionBonus = (scene.keywords.size - index) * 2.5
            val lengthBonus = min(keyword.length * 2.0, 12.0)
            val completenessBonus = if (keyword.length >= 4) 3.0 else 1.0
            baseConfidence += positionBonus + lengthBonus + completenessBonus
        }
        
        val maxConfidence = min(baseConfidence, 98.0)
        return maxConfidence
    }
    
    suspend fun getRecommendedPresets(scene: SceneType, allPresets: List<Preset>): List<Preset> {
        delay(280)
        
        if (scene == SceneType.UNKNOWN) {
            return allPresets.take(5)
        }
        
        val sceneKeywords = scene.keywords
        
        val scoredPresets = allPresets.map { preset ->
            var score = 0
            sceneKeywords.forEachIndexed { index, keyword ->
                if (preset.name.contains(keyword, ignoreCase = true)) {
                    score += (12 - index).coerceAtLeast(5)
                }
                preset.sections.forEach { section ->
                    if (section.title.contains(keyword, ignoreCase = true) ||
                        section.content.contains(keyword, ignoreCase = true)) {
                        score += (8 - index).coerceAtLeast(3)
                    }
                }
                preset.cameraParams?.let { params ->
                    val paramsString = params.toString()
                    if (paramsString.contains(keyword, ignoreCase = true)) {
                        score += (5 - index).coerceAtLeast(2)
                    }
                }
            }
            Pair(preset, score)
        }
        
        val sortedPresets = scoredPresets
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
        
        return if (sortedPresets.isNotEmpty()) {
            sortedPresets.take(10)
        } else {
            allPresets.take(5)
        }
    }
    
    suspend fun fineTuneImage(imageUri: String, preset: Preset?): AiAdjustmentParams {
        delay(1200)
        
        val baseParams = preset?.cameraParams
        
        return AiAdjustmentParams(
            brightness = baseParams?.brightness?.toFloat() ?: 5f,
            contrast = baseParams?.contrast?.toFloat() ?: 8f,
            saturation = baseParams?.saturation?.toFloat() ?: 10f,
            warmth = baseParams?.warmth?.toFloat() ?: 0f,
            tint = 0f,
            highlights = -5f,
            shadows = 5f,
            clarity = baseParams?.clarity?.toFloat() ?: 8f,
            vignette = 10f
        )
    }
    
    suspend fun analyzeImage(imageUri: String): ImageAnalysisResult {
        delay(600)
        
        return ImageAnalysisResult(
            brightness = (Math.random() * 100).toInt(),
            contrast = (Math.random() * 100).toInt(),
            saturation = (Math.random() * 100).toInt(),
            warmth = ((Math.random() * 40) - 20).toInt(),
            sharpness = (Math.random() * 100).toInt(),
            noise = (Math.random() * 50).toInt(),
            colorTemperature = 5500 + ((Math.random() * 2000) - 1000).toInt(),
            suggestedAdjustments = generateSuggestions()
        )
    }
    
    private fun generateSuggestions(): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (Math.random() > 0.4) {
            suggestions.add("建议提升饱和度，增强色彩表现")
        }
        if (Math.random() > 0.4) {
            suggestions.add("适当增加对比度，提高画面层次感")
        }
        if (Math.random() > 0.4) {
            suggestions.add("可尝试降低高光，保留更多细节")
        }
        if (Math.random() > 0.4) {
            suggestions.add("建议提升清晰度，增强画面锐度")
        }
        if (Math.random() > 0.25) {
            suggestions.add("可根据喜好调整色温")
        }
        
        return suggestions.take(3)
    }
}

data class SceneMatch(
    val scene: SceneType,
    val confidence: Double
)

data class SceneDetectionResult(
    val primaryScene: SceneType,
    val confidence: Double,
    val secondaryScene: SceneType?,
    val allMatches: List<SceneMatch>,
    val recommendedParams: com.omaster.app.model.CameraParams?
)

data class ImageAnalysisResult(
    val brightness: Int,
    val contrast: Int,
    val saturation: Int,
    val warmth: Int,
    val sharpness: Int,
    val noise: Int,
    val colorTemperature: Int,
    val suggestedAdjustments: List<String>
)
