package com.omaster.app.service

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.omaster.app.model.AiAdjustmentParams
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.model.SceneType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI服务 - 真实实现
 * 使用ML Kit进行图像识别和分析
 */
@Singleton
class AiService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val imageLabeler by lazy {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.7f)
                .build()
        )
    }
    
    /**
     * AI场景识别 - 真实实现
     * 使用ML Kit图像标签识别场景
     */
    suspend fun detectScene(imageUri: String): SceneType = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(imageUri)
            val image = InputImage.fromFilePath(context, uri)
            
            val labels = imageLabeler.process(image).await()
            
            // 根据ML Kit识别结果判断场景
            val detectedScene = analyzeLabels(labels.map { it.text to it.confidence })
            
            Timber.d("Detected scene: $detectedScene from ${labels.size} labels")
            detectedScene
        } catch (e: Exception) {
            Timber.e(e, "Failed to detect scene")
            SceneType.UNKNOWN
        }
    }
    
    /**
     * 分析ML Kit标签，映射到场景类型
     */
    private fun analyzeLabels(labels: List<Pair<String, Float>>): SceneType {
        val labelTexts = labels.map { it.first.lowercase() }
        
        return when {
            // 人像检测
            labelTexts.any { it.contains("person") || it.contains("people") || it.contains("face") || it.contains("portrait") } -> SceneType.PORTRAIT
            
            // 美食检测
            labelTexts.any { it.contains("food") || it.contains("meal") || it.contains("dish") || it.contains("cuisine") } -> SceneType.FOOD
            
            // 风景检测
            labelTexts.any { it.contains("landscape") || it.contains("mountain") || it.contains("nature") || it.contains("scenery") } -> SceneType.LANDSCAPE
            
            // 夜景检测
            labelTexts.any { it.contains("night") || it.contains("dark") || it.contains("evening") } -> SceneType.NIGHT
            
            // 城市/建筑
            labelTexts.any { it.contains("city") || it.contains("building") || it.contains("architecture") || it.contains("urban") } -> SceneType.CITYSCAPE
            
            // 室内
            labelTexts.any { it.contains("indoor") || it.contains("room") || it.contains("interior") } -> SceneType.INDOOR_WARM
            
            // 静物
            labelTexts.any { it.contains("still life") || it.contains("object") || it.contains("product") } -> SceneType.STILL_LIFE
            
            // 微距/花卉
            labelTexts.any { it.contains("flower") || it.contains("plant") || it.contains("macro") || it.contains("blossom") } -> SceneType.FLOWER
            
            // 动物/昆虫
            labelTexts.any { it.contains("animal") || it.contains("pet") || it.contains("insect") || it.contains("bird") } -> SceneType.INSECT
            
            // 日落/日出
            labelTexts.any { it.contains("sunset") || it.contains("sunrise") || it.contains("dawn") || it.contains("dusk") } -> SceneType.SUNSET
            
            // 运动
            labelTexts.any { it.contains("sport") || it.contains("motion") || it.contains("action") || it.contains("running") } -> SceneType.MOTION
            
            else -> SceneType.UNKNOWN
        }
    }
    
    /**
     * 获取推荐预设 - 基于真实场景识别结果
     */
    suspend fun getRecommendedPresets(scene: SceneType, allPresets: List<Preset>): List<Preset> {
        return withContext(Dispatchers.Default) {
            val keywords = when (scene) {
                SceneType.PORTRAIT -> listOf("人像", "portrait", "哈苏", "HNCS")
                SceneType.FOOD -> listOf("美食", "food", "胶片")
                SceneType.LANDSCAPE -> listOf("风景", "landscape", "德味", "富士")
                SceneType.NIGHT -> listOf("夜景", "night", "城市")
                SceneType.CITYSCAPE -> listOf("城市", "city", "街拍")
                SceneType.FLOWER -> listOf("花卉", "flower", "微距")
                SceneType.MOTION -> listOf("运动", "motion", "抓拍")
                SceneType.SUNSET -> listOf("日落", "sunset", "黄昏")
                else -> listOf("通用", "auto", "默认")
            }
            
            // 根据关键词匹配预设
            val matchedPresets = allPresets.filter { preset ->
                keywords.any { keyword ->
                    preset.name.contains(keyword, ignoreCase = true) ||
                    preset.tags.any { it.contains(keyword, ignoreCase = true) }
                }
            }
            
            // 按评分排序
            matchedPresets.sortedByDescending { it.useCount }.take(3)
        }
    }
    
    /**
     * 获取场景对应的相机参数 - 专业级参数推荐
     */
    fun getCameraParamsForScene(scene: SceneType): CameraParams {
        return when (scene) {
            SceneType.PORTRAIT -> CameraParams(
                mode = "人像模式",
                iso = 100,
                shutter = "1/125",
                ev = "+0.3",
                wb = "5200K",
                focalLength = "85mm",
                aperture = "f/1.8",
                portraitMode = true,
                aiOptimization = true
            )
            
            SceneType.FOOD -> CameraParams(
                mode = "美食模式",
                iso = 200,
                shutter = "1/60",
                ev = "+0.3",
                wb = "5000K",
                focalLength = "50mm",
                aperture = "f/2.8",
                aiOptimization = true
            )
            
            SceneType.LANDSCAPE -> CameraParams(
                mode = "风景模式",
                iso = 64,
                shutter = "1/250",
                ev = "+0.7",
                wb = "6500K",
                focalLength = "24mm",
                aperture = "f/8.0",
                hdr = true,
                aiOptimization = true
            )
            
            SceneType.NIGHT -> CameraParams(
                mode = "夜景模式",
                iso = 1600,
                shutter = "1/30",
                ev = "+0.7",
                wb = "4000K",
                focalLength = "24mm",
                aperture = "f/1.8",
                nightMode = true,
                aiOptimization = true
            )
            
            SceneType.CITYSCAPE -> CameraParams(
                mode = "城市模式",
                iso = 100,
                shutter = "1/125",
                ev = "0",
                wb = "5500K",
                focalLength = "35mm",
                aperture = "f/5.6",
                aiOptimization = true
            )
            
            SceneType.FLOWER -> CameraParams(
                mode = "微距模式",
                iso = 100,
                shutter = "1/160",
                ev = "0",
                wb = "5200K",
                focalLength = "微距",
                aperture = "f/4.0",
                aiOptimization = true
            )
            
            SceneType.MOTION -> CameraParams(
                mode = "运动模式",
                iso = 400,
                shutter = "1/1000",
                ev = "0",
                wb = "5500K",
                focalLength = "200mm",
                aperture = "f/4.0",
                aiOptimization = true
            )
            
            SceneType.SUNSET -> CameraParams(
                mode = "日落模式",
                iso = 64,
                shutter = "1/500",
                ev = "+0.7",
                wb = "6000K",
                focalLength = "24mm",
                aperture = "f/5.6",
                hdr = true,
                aiOptimization = true
            )
            
            else -> CameraParams(
                mode = "自动模式",
                iso = 100,
                shutter = "1/125",
                ev = "0",
                wb = "自动",
                focalLength = "标准",
                aperture = "f/2.8",
                aiOptimization = true
            )
        }
    }
    
    /**
     * AI图片微调 - 基于场景的智能调整
     */
    suspend fun fineTuneImage(imageUri: String, preset: Preset?): AiAdjustmentParams {
        return withContext(Dispatchers.Default) {
            try {
                // 先进行场景识别
                val scene = detectScene(imageUri)
                
                // 根据场景返回对应的调整参数
                getAdjustmentParamsForScene(scene, preset)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fine tune image")
                AiAdjustmentParams.DEFAULT
            }
        }
    }
    
    /**
     * 根据场景获取调整参数
     */
    private fun getAdjustmentParamsForScene(scene: SceneType, preset: Preset?): AiAdjustmentParams {
        return when (scene) {
            SceneType.PORTRAIT -> AiAdjustmentParams(
                brightness = 10f,
                contrast = 6f,
                saturation = 8f,
                warmth = 8f,
                tint = 2f,
                highlights = -8f,
                shadows = 12f,
                clarity = 6f,
                vignette = 8f
            )
            
            SceneType.FOOD -> AiAdjustmentParams(
                brightness = 8f,
                contrast = 7f,
                saturation = 18f,
                warmth = 12f,
                tint = 3f,
                highlights = -5f,
                shadows = 8f,
                clarity = 12f,
                vignette = 3f
            )
            
            SceneType.LANDSCAPE -> AiAdjustmentParams(
                brightness = 5f,
                contrast = 12f,
                saturation = 15f,
                warmth = 0f,
                tint = -2f,
                highlights = -12f,
                shadows = 18f,
                clarity = 18f,
                vignette = 5f
            )
            
            SceneType.NIGHT -> AiAdjustmentParams(
                brightness = 15f,
                contrast = 10f,
                saturation = 5f,
                warmth = -3f,
                tint = 0f,
                highlights = -15f,
                shadows = 20f,
                clarity = 15f,
                vignette = 12f
            )
            
            else -> AiAdjustmentParams.DEFAULT
        }
    }
    
    /**
     * 批量AI微调
     */
    suspend fun batchFineTuneImages(imageUris: List<String>, preset: Preset?): List<AiAdjustmentParams> {
        return withContext(Dispatchers.Default) {
            imageUris.map { uri ->
                fineTuneImage(uri, preset)
            }
        }
    }
    
    /**
     * 创建自定义微调模板
     */
    fun createCustomTemplate(
        templateName: String,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        warmth: Float,
        tint: Float
    ): AiAdjustmentParams {
        return AiAdjustmentParams(
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            warmth = warmth,
            tint = tint,
            highlights = 0f,
            shadows = 0f,
            clarity = 5f,
            vignette = 2f
        )
    }
}
