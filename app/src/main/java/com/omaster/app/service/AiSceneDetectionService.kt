package com.omaster.app.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class AiSceneDetectionService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://api.omaster.example.com/v1/ai"
    
    data class SceneAnalysisResult(
        val detectedScene: SceneType,
        val confidence: Float,
        val recommendations: List<SceneRecommendation>
    )
    
    data class SceneRecommendation(
        val presetId: String,
        val presetName: String,
        val matchScore: Float,
        val reason: String
    )

    enum class SceneType(val displayName: String, val description: String) {
        LANDSCAPE("风景", "自然风景、户外景色"),
        PORTRAIT("人像", "人物摄影"),
        NIGHT("夜景", "夜间场景"),
        FOOD("美食", "美食摄影"),
        STREET("街拍", "城市街景"),
        ARCHITECTURE("建筑", "建筑摄影"),
        SUNSET("日落", "黄昏时分"),
        NATURE("自然", "植物、动物"),
        SPORTS("运动", "体育运动"),
        MACRO("微距", "特写摄影"),
        UNKNOWN("通用", "一般场景")
    }

    suspend fun analyzeImage(imageUri: Uri): SceneAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val file = compressImage(imageUri)
            if (file == null) {
                return@withContext getDefaultResult()
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    file.name,
                    file.asRequestBody("image/jpeg".toMediaType())
                )
                .addFormDataPart("mode", "scene_detection")
                .build()

            val request = Request.Builder()
                .url("$baseUrl/analyze")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                parseAnalysisResult(response.body?.string())
            } else {
                getDefaultResult()
            }
        } catch (e: Exception) {
            Timber.e(e, "场景分析失败")
            getDefaultResult()
        }
    }

    suspend fun analyzeImageFromBitmap(bitmap: Bitmap): SceneAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val file = saveBitmapToFile(bitmap)
            if (file == null) {
                return@withContext getDefaultResult()
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    file.name,
                    file.asRequestBody("image/jpeg".toMediaType())
                )
                .addFormDataPart("mode", "scene_detection")
                .build()

            val request = Request.Builder()
                .url("$baseUrl/analyze")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                parseAnalysisResult(response.body?.string())
            } else {
                getDefaultResult()
            }
        } catch (e: Exception) {
            Timber.e(e, "场景分析失败")
            getDefaultResult()
        }
    }

    fun analyzeImageLocally(imageUri: Uri): SceneAnalysisResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap != null) {
                analyzeBitmapLocally(bitmap)
            } else {
                getDefaultResult()
            }
        } catch (e: Exception) {
            Timber.e(e, "本地场景分析失败")
            getDefaultResult()
        }
    }

    fun analyzeBitmapLocally(bitmap: Bitmap): SceneAnalysisResult {
        val width = bitmap.width
        val height = bitmap.height
        
        val avgBrightness = calculateAverageBrightness(bitmap)
        val dominantColors = extractDominantColors(bitmap)
        val edgeDensity = calculateEdgeDensity(bitmap)
        
        val detectedScene = determineScene(avgBrightness, dominantColors, edgeDensity, width, height)
        
        return SceneAnalysisResult(
            detectedScene = detectedScene,
            confidence = calculateConfidence(avgBrightness, dominantColors, edgeDensity),
            recommendations = emptyList()
        )
    }

    private fun calculateAverageBrightness(bitmap: Bitmap): Float {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, true)
        var totalBrightness = 0f
        
        for (y in 0 until scaledBitmap.height) {
            for (x in 0 until scaledBitmap.width) {
                val pixel = scaledBitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                totalBrightness += (0.299 * r + 0.587 * g + 0.114 * b)
            }
        }
        
        return totalBrightness / (scaledBitmap.width * scaledBitmap.height)
    }

    private fun extractDominantColors(bitmap: Bitmap): List<Int> {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, true)
        val colorCounts = mutableMapOf<Int, Int>()
        
        for (y in 0 until scaledBitmap.height) {
            for (x in 0 until scaledBitmap.width) {
                val pixel = scaledBitmap.getPixel(x, y)
                val quantizedPixel = quantizeColor(pixel)
                colorCounts[quantizedPixel] = colorCounts.getOrDefault(quantizedPixel, 0) + 1
            }
        }
        
        return colorCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
    }

    private fun quantizeColor(pixel: Int): Int {
        val r = ((pixel shr 16) and 0xFF) / 32 * 32
        val g = ((pixel shr 8) and 0xFF) / 32 * 32
        val b = (pixel and 0xFF) / 32 * 32
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun calculateEdgeDensity(bitmap: Bitmap): Float {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, true)
        var edgeCount = 0
        
        for (y in 1 until scaledBitmap.height - 1) {
            for (x in 1 until scaledBitmap.width - 1) {
                val pixel = scaledBitmap.getPixel(x, y)
                val left = scaledBitmap.getPixel(x - 1, y)
                val right = scaledBitmap.getPixel(x + 1, y)
                val top = scaledBitmap.getPixel(x, y - 1)
                val bottom = scaledBitmap.getPixel(x, y + 1)
                
                val diff = kotlin.math.abs((pixel and 0xFF) - (left and 0xFF)) +
                        kotlin.math.abs((pixel and 0xFF) - (right and 0xFF)) +
                        kotlin.math.abs((pixel and 0xFF) - (top and 0xFF)) +
                        kotlin.math.abs((pixel and 0xFF) - (bottom and 0xFF))
                
                if (diff > 100) {
                    edgeCount++
                }
            }
        }
        
        return edgeCount.toFloat() / (scaledBitmap.width * scaledBitmap.height)
    }

    private fun determineScene(
        brightness: Float,
        colors: List<Int>,
        edgeDensity: Float,
        width: Int,
        height: Int
    ): SceneType {
        val aspectRatio = width.toFloat() / height
        
        if (brightness < 50) {
            return SceneType.NIGHT
        }
        
        if (brightness > 200) {
            if (colors.any { isWarmColor(it) }) {
                return SceneType.SUNSET
            }
        }
        
        if (edgeDensity > 0.3) {
            if (aspectRatio > 1.2f || aspectRatio < 0.8f) {
                return SceneType.ARCHITECTURE
            }
            return SceneType.STREET
        }
        
        if (colors.any { isGreenColor(it) }) {
            if (edgeDensity < 0.15f) {
                return SceneType.NATURE
            }
            return SceneType.LANDSCAPE
        }
        
        if (colors.any { isWarmColor(it) }) {
            return SceneType.FOOD
        }
        
        if (colors.any { isSkinTone(it) }) {
            return SceneType.PORTRAIT
        }
        
        return SceneType.UNKNOWN
    }

    private fun isWarmColor(color: Int): Boolean {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return r > g && r > b && (r - b) > 30
    }

    private fun isGreenColor(color: Int): Boolean {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return g > r && g > b && (g - r) > 20 && (g - b) > 20
    }

    private fun isSkinTone(color: Int): Boolean {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val rNormalized = r.toFloat() / (r + g + b)
        val gNormalized = g.toFloat() / (r + g + b)
        return r > 95 && g > 40 && b > 20 &&
                max(r, max(g, b)) - min(r, min(g, b)) > 15 &&
                rNormalized in 0.36f..0.55f
    }

    private fun calculateConfidence(
        brightness: Float,
        colors: List<Int>,
        edgeDensity: Float
    ): Float {
        var confidence = 0.5f
        
        if (brightness < 30 || brightness > 220) {
            confidence += 0.2f
        }
        
        if (colors.size >= 3) {
            confidence += 0.1f
        }
        
        if (edgeDensity > 0.2f) {
            confidence += 0.15f
        }
        
        return min(0.95f, max(0.3f, confidence))
    }

    private fun compressImage(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (bitmap == null) return null
            
            val maxDimension = 1024
            val scale = min(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
            val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
            
            val file = File(context.cacheDir, "temp_analysis_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            
            file
        } catch (e: Exception) {
            Timber.e(e, "图片压缩失败")
            null
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): File? {
        return try {
            val maxDimension = 1024
            val scale = min(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
            val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
            
            val file = File(context.cacheDir, "temp_analysis_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            
            file
        } catch (e: Exception) {
            Timber.e(e, "保存位图失败")
            null
        }
    }

    private fun parseAnalysisResult(json: String?): SceneAnalysisResult {
        return try {
            val data = com.google.gson.Gson().fromJson(json, Map::class.java)
            val sceneName = data["scene"] as? String ?: "UNKNOWN"
            val confidence = (data["confidence"] as? Double)?.toFloat() ?: 0.7f
            
            val scene = SceneType.entries.find { it.name == sceneName } ?: SceneType.UNKNOWN
            
            SceneAnalysisResult(
                detectedScene = scene,
                confidence = confidence,
                recommendations = emptyList()
            )
        } catch (e: Exception) {
            Timber.e(e, "解析分析结果失败")
            getDefaultResult()
        }
    }

    private fun getDefaultResult(): SceneAnalysisResult {
        return SceneAnalysisResult(
            detectedScene = SceneType.UNKNOWN,
            confidence = 0.5f,
            recommendations = emptyList()
        )
    }
}
