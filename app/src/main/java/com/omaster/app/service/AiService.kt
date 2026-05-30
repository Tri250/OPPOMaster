package com.omaster.app.service

import com.omaster.app.model.AiAdjustmentParams
import com.omaster.app.model.Preset
import com.omaster.app.model.SceneType
import kotlinx.coroutines.delay
import kotlin.random.Random

class AiService {
    
    // 场景识别结果数据类
    data class SceneDetectionResult(
        val primaryScene: SceneType,
        val secondaryScene: SceneType? = null,
        val confidence: Float = 0.85f
    )
    
    // 为了演示，我们将根据一些启发式规则来模拟场景识别
    // 实际应用中，这里应该调用真实的 AI 模型
    suspend fun detectScene(imageUri: String? = null): SceneType {
        // 稍微缩短延迟，提升体验
        delay(1200)
        
        // 基于模拟的场景识别，而不是纯随机
        return when {
            // 模拟一些简单的启发式规则
            imageUri?.contains("portrait", ignoreCase = true) == true -> SceneType.PORTRAIT
            imageUri?.contains("landscape", ignoreCase = true) == true -> SceneType.LANDSCAPE
            imageUri?.contains("night", ignoreCase = true) == true -> SceneType.NIGHT
            imageUri?.contains("food", ignoreCase = true) == true -> SceneType.FOOD
            imageUri?.contains("sunset", ignoreCase = true) == true -> SceneType.SUNSET
            imageUri?.contains("nature", ignoreCase = true) == true -> SceneType.NATURE
            else -> {
                // 基于预定义概率的场景识别，确保结果更可预测
                val random = Random(System.currentTimeMillis())
                val scenes = listOf(
                    SceneType.LANDSCAPE to 0.25f,
                    SceneType.PORTRAIT to 0.25f,
                    SceneType.NIGHT to 0.15f,
                    SceneType.FOOD to 0.15f,
                    SceneType.SUNSET to 0.1f,
                    SceneType.NATURE to 0.1f
                )
                
                selectSceneByProbability(scenes, random)
            }
        }
    }
    
    private fun selectSceneByProbability(
        sceneProbabilities: List<Pair<SceneType, Float>>,
        random: Random
    ): SceneType {
        val cumulative = mutableListOf<Pair<SceneType, Float>>()
        var total = 0f
        
        sceneProbabilities.forEach { (scene, prob) ->
            total += prob
            cumulative.add(scene to total)
        }
        
        val value = random.nextFloat() * total
        return cumulative.firstOrNull { value <= it.second }?.first ?: SceneType.UNKNOWN
    }
    
    suspend fun getRecommendedPresets(scene: SceneType, allPresets: List<Preset>): List<Preset> {
        delay(400)
        
        val sceneKeywords = when (scene) {
            SceneType.LANDSCAPE -> listOf("风景", "自然", "森林", "海边", "风光", "蓝调", "理光绿", "清新")
            SceneType.PORTRAIT -> listOf("人像", "柔焦", "童话", "梦幻", "黑柔", "经典")
            SceneType.NIGHT -> listOf("夜景", "夜色", "霓虹", "蓝调", "城市夜景", "赛博")
            SceneType.SUNSET -> listOf("日落", "橙调", "佛罗伦萨", "金色时刻", "夕阳暖调", "暖调")
            SceneType.FOOD -> listOf("美食", "清新", "食欲", "诱人")
            SceneType.STREET -> listOf("街头", "纪实", "黑白", "街拍", "故事")
            SceneType.NATURE -> listOf("自然", "森林", "清新", "微距", "植物", "生态")
            SceneType.ARCHITECTURE -> listOf("建筑", "城市", "纪实", "空间", "结构")
            SceneType.MACRO -> listOf("微距", "特写", "细节", "微观")
            SceneType.UNKNOWN -> emptyList()
        }
        
        // 带评分的匹配算法
        val scoredPresets = allPresets.map { preset ->
            var score = 0f
            
            // 名称匹配评分
            score += sceneKeywords.sumOf { keyword ->
                if (preset.name.contains(keyword)) 3f else 0f
            }
            
            // 描述匹配评分
            score += sceneKeywords.sumOf { keyword ->
                if (preset.sections.any { it.title.contains(keyword) || it.content.contains(keyword) }) 2f else 0f
            }
            
            // 相机参数匹配评分
            preset.cameraParams?.let { params ->
                if (params.filter != null && sceneKeywords.any { params.filter.contains(it) }) {
                    score += 1.5f
                }
                
                // 特殊场景的相机参数匹配
                when (scene) {
                    SceneType.PORTRAIT -> if (params.portrait_mode == true) score += 2f
                    SceneType.NIGHT -> if (params.night_mode == true) score += 2f
                    else -> {}
                }
                
                // HNCS 认证加分
                if (params.hasselblad_hncs == true) {
                    score += 1f
                }
            }
            
            preset to score
        }.sortedByDescending { it.second }
        
        // 提取高评分预设
        val highScorePresets = scoredPresets.filter { it.second > 0f }.map { it.first }
        
        val result = when {
            highScorePresets.size >= 4 -> highScorePresets.take(4)
            highScorePresets.isNotEmpty() -> {
                // 补充一些相关场景的预设
                val fallbackPresets = getFallbackPresets(scene, allPresets - highScorePresets.toSet())
                (highScorePresets + fallbackPresets).distinctBy { it.id }.take(4)
            }
            else -> {
                // 完全没有匹配，使用场景特定的回退策略
                getFallbackPresets(scene, allPresets).take(4)
            }
        }
        
        return result.ifEmpty { allPresets.take(4) }
    }
    
    private fun getFallbackPresets(scene: SceneType, presets: List<Preset>): List<Preset> {
        val fallback = when (scene) {
            SceneType.LANDSCAPE, SceneType.NATURE -> presets.filter { 
                it.name.contains("风景") || it.name.contains("自然") || it.name.contains("清新")
            }
            SceneType.PORTRAIT -> presets.filter { 
                it.name.contains("人像") || it.cameraParams?.portrait_mode == true 
            }
            SceneType.NIGHT -> presets.filter { 
                it.name.contains("夜景") || it.cameraParams?.night_mode == true 
            }
            SceneType.FOOD -> presets.filter { it.name.contains("美食") || it.name.contains("诱人") }
            SceneType.SUNSET -> presets.filter { it.name.contains("日落") || it.name.contains("暖调") }
            SceneType.STREET -> presets.filter { it.name.contains("街拍") || it.name.contains("纪实") }
            SceneType.ARCHITECTURE -> presets.filter { it.name.contains("建筑") }
            SceneType.MACRO -> presets.filter { it.name.contains("微距") }
            SceneType.UNKNOWN -> presets
        }
        
        return fallback.ifEmpty { presets.shuffled() }
    }
    
    suspend fun fineTuneImage(imageUri: String, preset: Preset?): AiAdjustmentParams {
        delay(1800)
        
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
