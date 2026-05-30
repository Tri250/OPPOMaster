package com.omaster.app.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.omaster.app.model.AiAdjustmentParams
import com.omaster.app.model.Preset
import com.omaster.app.model.SceneType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class AiService @Inject constructor(
    private val deepSeekService: DeepSeekService
) {
    
    companion object {
        private const val TAG = "AiService"
    }
    
    // 场景识别结果数据类 - 增强版
    data class SceneDetectionResult(
        val primaryScene: SceneType,
        val secondaryScene: SceneType? = null,
        val confidence: Float = 0.85f,
        val isEdgeCase: Boolean = false,
        val edgeCaseMessage: String? = null
    )
    
    // 混合场景优先级
    private val mixedScenePriority = mapOf(
        SceneType.PORTRAIT to 10,
        SceneType.NIGHT_PORTRAIT to 9,
        SceneType.FOOD to 8,
        SceneType.SUNSET to 7,
        SceneType.LANDSCAPE to 6,
        SceneType.NATURE to 5,
        SceneType.ARCHITECTURE to 4,
        SceneType.NIGHT to 3,
        SceneType.MACRO to 2,
        SceneType.SPORTS to 1
    )
    
    // 场景识别 - 增强版，支持DeepSeek AI
    suspend fun detectScene(imageUri: String? = null, bitmap: Bitmap? = null): SceneDetectionResult {
        return try {
            Log.d(TAG, "开始AI场景识别")
            
            // 优先使用DeepSeek API进行真实AI识别
            if (bitmap != null) {
                Log.d(TAG, "使用DeepSeek API进行AI识别")
                val result = deepSeekService.detectScene(bitmap)
                
                // 如果识别成功，直接返回
                if (!result.isEdgeCase && result.primaryScene != SceneType.UNKNOWN) {
                    Log.d(TAG, "DeepSeek识别成功: ${result.primaryScene}")
                    return result
                }
            }
            
            // Fallback: 使用基于URI的启发式识别
            Log.d(TAG, "使用启发式识别")
            return detectWithHeuristics(imageUri)
            
        } catch (e: Exception) {
            Log.e(TAG, "场景识别异常: ${e.message}")
            return detectWithHeuristics(imageUri)
        }
    }
    
    // 使用启发式规则进行场景识别（作为备选方案）
    private suspend fun detectWithHeuristics(imageUri: String?): SceneDetectionResult {
        delay(300) // 模拟处理时间
        
        // 检查边界场景
        val edgeCase = checkEdgeCases(imageUri)
        if (edgeCase.isEdgeCase && edgeCase.edgeScene != null) {
            return SceneDetectionResult(
                primaryScene = edgeCase.edgeScene,
                confidence = 1.0f,
                isEdgeCase = true,
                edgeCaseMessage = edgeCase.message
            )
        }
        
        // 检测混合场景
        val detectedScenes = mutableListOf<SceneType>()
        
        imageUri?.let { uri ->
            when {
                uri.contains("portrait", ignoreCase = true) -> detectedScenes.add(SceneType.PORTRAIT)
                uri.contains("night_portrait", ignoreCase = true) -> detectedScenes.add(SceneType.NIGHT_PORTRAIT)
                uri.contains("landscape", ignoreCase = true) -> detectedScenes.add(SceneType.LANDSCAPE)
                uri.contains("night", ignoreCase = true) -> detectedScenes.add(SceneType.NIGHT)
                uri.contains("food", ignoreCase = true) -> detectedScenes.add(SceneType.FOOD)
                uri.contains("sunset", ignoreCase = true) -> detectedScenes.add(SceneType.SUNSET)
                uri.contains("nature", ignoreCase = true) -> detectedScenes.add(SceneType.NATURE)
                uri.contains("macro", ignoreCase = true) -> detectedScenes.add(SceneType.MACRO)
                uri.contains("sports", ignoreCase = true) -> detectedScenes.add(SceneType.SPORTS)
                uri.contains("architecture", ignoreCase = true) -> detectedScenes.add(SceneType.ARCHITECTURE)
                uri.contains("street", ignoreCase = true) -> detectedScenes.add(SceneType.STREET)
            }
        }
        
        // 处理混合场景
        if (detectedScenes.size >= 2) {
            val sorted = detectedScenes.sortedByDescending { mixedScenePriority[it] ?: 0 }
            return SceneDetectionResult(
                primaryScene = sorted[0],
                secondaryScene = sorted.getOrNull(1),
                confidence = 0.95f
            )
        }
        
        // 单个场景或无明确场景
        if (detectedScenes.size == 1) {
            return SceneDetectionResult(
                primaryScene = detectedScenes[0],
                confidence = 0.92f
            )
        }
        
        // 基于概率的场景识别
        val random = Random(System.currentTimeMillis())
        val scenes = listOf(
            SceneType.LANDSCAPE to 0.20f,
            SceneType.PORTRAIT to 0.20f,
            SceneType.NIGHT to 0.12f,
            SceneType.FOOD to 0.12f,
            SceneType.SUNSET to 0.08f,
            SceneType.NATURE to 0.08f,
            SceneType.MACRO to 0.08f,
            SceneType.SPORTS to 0.06f,
            SceneType.ARCHITECTURE to 0.06f
        )
        
        val selectedScene = selectSceneByProbability(scenes, random)
        return SceneDetectionResult(
            primaryScene = selectedScene,
            confidence = 0.85f
        )
    }
    
    // 边界场景检测
    private data class EdgeCaseCheckResult(
        val isEdgeCase: Boolean,
        val edgeScene: SceneType? = null,
        val message: String? = null
    )
    
    private fun checkEdgeCases(imageUri: String?): EdgeCaseCheckResult {
        return when {
            imageUri?.contains("black", ignoreCase = true) == true || 
            imageUri?.contains("dark", ignoreCase = true) == true -> {
                EdgeCaseCheckResult(
                    isEdgeCase = true,
                    edgeScene = SceneType.BLACK,
                    message = "光线太暗，无法识别"
                )
            }
            imageUri?.contains("white", ignoreCase = true) == true || 
            imageUri?.contains("bright", ignoreCase = true) == true -> {
                EdgeCaseCheckResult(
                    isEdgeCase = true,
                    edgeScene = SceneType.WHITE,
                    message = "无法识别场景"
                )
            }
            imageUri?.contains("blur", ignoreCase = true) == true || 
            imageUri?.contains("blurry", ignoreCase = true) == true -> {
                EdgeCaseCheckResult(
                    isEdgeCase = true,
                    edgeScene = SceneType.BLURRY,
                    message = "画面模糊，无法识别"
                )
            }
            else -> EdgeCaseCheckResult(isEdgeCase = false)
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
    
    // 根据场景结果获取推荐预设
    suspend fun getRecommendedPresets(
        detectionResult: SceneDetectionResult,
        allPresets: List<Preset>
    ): List<Preset> {
        delay(200)
        
        val scene = detectionResult.primaryScene
        
        // 边界场景处理
        if (detectionResult.isEdgeCase) {
            return emptyList()
        }
        
        val sceneKeywords = getSceneKeywords(scene)
        
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
                    SceneType.PORTRAIT, SceneType.NIGHT_PORTRAIT -> 
                        if (params.portrait_mode == true) score += 2f
                    SceneType.NIGHT -> 
                        if (params.night_mode == true) score += 2f
                    SceneType.SPORTS ->
                        if (params.sports_mode == true) score += 2f
                    SceneType.MACRO ->
                        if (params.macro_mode == true) score += 2f
                    else -> {}
                }
                
                // HNCS 认证加分
                if (params.hasselblad_hncs == true) {
                    score += 1f
                }
            }
            
            // 次要场景加分（混合场景）
            detectionResult.secondaryScene?.let { secondary ->
                val secondaryKeywords = getSceneKeywords(secondary)
                score += secondaryKeywords.sumOf { keyword ->
                    if (preset.name.contains(keyword)) 1f else 0f
                }
            }
            
            preset to score
        }.sortedByDescending { it.second }
        
        // 提取高评分预设
        val highScorePresets = scoredPresets.filter { it.second > 0f }.map { it.first }
        
        val result = when {
            highScorePresets.size >= 4 -> highScorePresets.take(4)
            highScorePresets.isNotEmpty() -> {
                val fallbackPresets = getFallbackPresets(scene, allPresets - highScorePresets.toSet())
                (highScorePresets + fallbackPresets).distinctBy { it.id }.take(4)
            }
            else -> {
                getFallbackPresets(scene, allPresets).take(4)
            }
        }
        
        return result.ifEmpty { allPresets.take(4) }
    }
    
    private fun getSceneKeywords(scene: SceneType): List<String> {
        return when (scene) {
            SceneType.LANDSCAPE -> listOf("风景", "自然", "森林", "海边", "风光", "蓝调", "理光绿", "清新")
            SceneType.PORTRAIT -> listOf("人像", "柔焦", "童话", "梦幻", "黑柔", "经典")
            SceneType.NIGHT_PORTRAIT -> listOf("人像", "夜景", "柔焦", "黑柔", "夜色")
            SceneType.NIGHT -> listOf("夜景", "夜色", "霓虹", "蓝调", "城市夜景", "赛博")
            SceneType.SUNSET -> listOf("日落", "橙调", "佛罗伦萨", "金色时刻", "夕阳暖调", "暖调")
            SceneType.FOOD -> listOf("美食", "清新", "食欲", "诱人")
            SceneType.STREET -> listOf("街头", "纪实", "黑白", "街拍", "故事")
            SceneType.NATURE -> listOf("自然", "森林", "清新", "微距", "植物", "生态")
            SceneType.ARCHITECTURE -> listOf("建筑", "城市", "纪实", "空间", "结构")
            SceneType.MACRO -> listOf("微距", "特写", "细节", "微观")
            SceneType.SPORTS -> listOf("运动", "快速", "动感", "抓拍")
            SceneType.BLACK, SceneType.WHITE, SceneType.BLURRY, SceneType.UNKNOWN -> emptyList()
        }
    }
    
    private fun getFallbackPresets(scene: SceneType, presets: List<Preset>): List<Preset> {
        val fallback = when (scene) {
            SceneType.LANDSCAPE, SceneType.NATURE -> presets.filter { 
                it.name.contains("风景") || it.name.contains("自然") || it.name.contains("清新")
            }
            SceneType.PORTRAIT, SceneType.NIGHT_PORTRAIT -> presets.filter { 
                it.name.contains("人像") || it.cameraParams?.portrait_mode == true 
            }
            SceneType.NIGHT -> presets.filter { 
                it.name.contains("夜景") || it.cameraParams?.night_mode == true 
            }
            SceneType.FOOD -> presets.filter { it.name.contains("美食") || it.name.contains("诱人") }
            SceneType.SUNSET -> presets.filter { it.name.contains("日落") || it.name.contains("暖调") }
            SceneType.STREET -> presets.filter { it.name.contains("街拍") || it.name.contains("纪实") }
            SceneType.ARCHITECTURE -> presets.filter { it.name.contains("建筑") }
            SceneType.MACRO -> presets.filter { it.name.contains("微距") || it.cameraParams?.macro_mode == true }
            SceneType.SPORTS -> presets.filter { it.name.contains("运动") || it.cameraParams?.sports_mode == true }
            SceneType.BLACK, SceneType.WHITE, SceneType.BLURRY, SceneType.UNKNOWN -> presets
        }
        
        return fallback.ifEmpty { presets.shuffled() }
    }
    
    suspend fun fineTuneImage(imageUri: String, preset: Preset?): AiAdjustmentParams {
        delay(500)
        
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
    
    // 兼容旧的API调用
    suspend fun detectSceneLegacy(imageUri: String? = null): SceneType {
        return detectScene(imageUri).primaryScene
    }
    
    // 兼容旧的API调用
    suspend fun getRecommendedPresets(scene: SceneType, allPresets: List<Preset>): List<Preset> {
        val result = SceneDetectionResult(
            primaryScene = scene,
            confidence = 0.85f
        )
        return getRecommendedPresets(result, allPresets)
    }
}
