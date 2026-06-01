package com.omaster.app.service

import com.omaster.app.model.AiAdjustmentParams
import com.omaster.app.model.Preset
import com.omaster.app.model.SceneType
import kotlinx.coroutines.delay

class AiService {
    
    suspend fun detectScene(imageUri: String? = null): SceneType {
        delay(1500)
        val scenes = SceneType.values().filter { it != SceneType.UNKNOWN }
        return scenes.random()
    }
    
    suspend fun getRecommendedPresets(scene: SceneType, allPresets: List<Preset>): List<Preset> {
        delay(500)
        
        val sceneKeywords = when (scene) {
            SceneType.LANDSCAPE -> listOf("风景", "自然", "森林", "海边")
            SceneType.PORTRAIT -> listOf("人像", "樱花", "柔焦")
            SceneType.NIGHT -> listOf("夜景", "夜色", "霓虹")
            SceneType.SUNSET -> listOf("日落", "橙调", "佛罗伦萨")
            SceneType.FOOD -> listOf("美食", "自然", "清新")
            SceneType.STREET -> listOf("街头", "纪实", "黑白")
            SceneType.NATURE -> listOf("自然", "森林", "清新")
            SceneType.ARCHITECTURE -> listOf("建筑", "城市", "纪实")
            SceneType.MACRO -> listOf("自然", "清新", "特写")
            SceneType.UNKNOWN -> emptyList()
        }
        
        val matched = allPresets.filter { preset ->
            sceneKeywords.any { keyword ->
                preset.name.contains(keyword) || 
                preset.sections.any { it.title.contains(keyword) || it.content.contains(keyword) }
            }
        }
        
        return if (matched.isNotEmpty()) matched else allPresets.take(3)
    }
    
    suspend fun fineTuneImage(imageUri: String, preset: Preset?): AiAdjustmentParams {
        delay(2000)
        
        val baseAdjustment = when (preset?.cameraParams?.hasselblad_hncs) {
            true -> AiAdjustmentParams(
                brightness = 8f,
                contrast = 5f,
                saturation = 12f,
                warmth = 5f,
                clarity = 8f
            )
            else -> AiAdjustmentParams(
                brightness = 5f,
                contrast = 8f,
                saturation = 10f,
                warmth = 0f,
                clarity = 5f
            )
        }
        
        return baseAdjustment
    }
}
