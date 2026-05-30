package com.omaster.app.service

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.omaster.app.model.Preset
import com.omaster.app.model.SceneType
import com.omaster.app.network.DeepSeekApi
import com.omaster.app.network.SceneDetectionPrompt
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepSeekService @Inject constructor(
    private val deepSeekApi: DeepSeekApi,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DeepSeekService"
        private const val API_KEY = "sk-fcd6db5526c84a21910befd5b68d074a"
        private const val BASE_URL = "https://api.deepseek.com/"
    }

    suspend fun detectScene(imageBitmap: Bitmap?): AiService.SceneDetectionResult {
        return try {
            val imageBase64 = imageBitmap?.let { bitmapToBase64(it) }
            
            val request = SceneDetectionPrompt.buildDetectionRequest(imageBase64)
            val response = deepSeekApi.chatCompletion(
                authorization = "Bearer $API_KEY",
                request = request
            )
            
            if (response.isSuccessful && response.body() != null) {
                val sceneType = SceneDetectionPrompt.parseSceneType(response.body()!!)
                if (sceneType != null && !isEdgeCase(sceneType)) {
                    AiService.SceneDetectionResult(
                        primaryScene = sceneType,
                        confidence = calculateConfidence(response.body()!!),
                        isEdgeCase = false
                    )
                } else if (sceneType != null) {
                    AiService.SceneDetectionResult(
                        primaryScene = sceneType,
                        confidence = 1.0f,
                        isEdgeCase = true,
                        edgeCaseMessage = getEdgeCaseMessage(sceneType)
                    )
                } else {
                    fallbackDetection(imageBase64)
                }
            } else {
                Log.e(TAG, "API调用失败: ${response.code()} - ${response.message()}")
                fallbackDetection(imageBase64)
            }
        } catch (e: Exception) {
            Log.e(TAG, "DeepSeek API异常: ${e.message}")
            fallbackDetection(null)
        }
    }

    private fun isEdgeCase(scene: SceneType): Boolean {
        return scene in listOf(SceneType.BLACK, SceneType.WHITE, SceneType.BLURRY)
    }

    private fun getEdgeCaseMessage(scene: SceneType): String {
        return when (scene) {
            SceneType.BLACK -> "光线太暗，无法识别"
            SceneType.WHITE -> "无法识别场景"
            SceneType.BLURRY -> "画面模糊，无法识别"
            else -> ""
        }
    }

    private fun calculateConfidence(response: com.omaster.app.network.DeepSeekResponse): Float {
        val usage = response.usage
        return if (usage != null && usage.total_tokens > 0) {
            minOf(0.95f, 0.80f + (usage.total_tokens / 1000f))
        } else {
            0.85f
        }
    }

    private suspend fun fallbackDetection(imageBase64: String?): AiService.SceneDetectionResult {
        Log.d(TAG, "使用回退检测逻辑")
        return AiService.SceneDetectionResult(
            primaryScene = detectFromUri(imageBase64),
            confidence = 0.75f,
            isEdgeCase = false
        )
    }

    private fun detectFromUri(imageUri: String?): SceneType {
        return when {
            imageUri?.contains("black", ignoreCase = true) == true -> SceneType.BLACK
            imageUri?.contains("white", ignoreCase = true) == true -> SceneType.WHITE
            imageUri?.contains("blur", ignoreCase = true) == true -> SceneType.BLURRY
            imageUri?.contains("portrait", ignoreCase = true) == true -> SceneType.PORTRAIT
            imageUri?.contains("night_portrait", ignoreCase = true) == true -> SceneType.NIGHT_PORTRAIT
            imageUri?.contains("landscape", ignoreCase = true) == true -> SceneType.LANDSCAPE
            imageUri?.contains("night", ignoreCase = true) == true -> SceneType.NIGHT
            imageUri?.contains("food", ignoreCase = true) == true -> SceneType.FOOD
            imageUri?.contains("sunset", ignoreCase = true) == true -> SceneType.SUNSET
            imageUri?.contains("nature", ignoreCase = true) == true -> SceneType.NATURE
            imageUri?.contains("macro", ignoreCase = true) == true -> SceneType.MACRO
            imageUri?.contains("sports", ignoreCase = true) == true -> SceneType.SPORTS
            imageUri?.contains("architecture", ignoreCase = true) == true -> SceneType.ARCHITECTURE
            imageUri?.contains("street", ignoreCase = true) == true -> SceneType.STREET
            else -> SceneType.UNKNOWN
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
