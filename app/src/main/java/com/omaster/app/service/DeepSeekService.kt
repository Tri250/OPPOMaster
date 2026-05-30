package com.omaster.app.service

import android.graphics.Bitmap
import android.util.Log
import com.omaster.app.model.SceneType
import com.omaster.app.network.DeepSeekApi
import com.omaster.app.network.DeepSeekConfig
import com.omaster.app.network.SceneDetectionPrompt
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepSeekService @Inject constructor(
    private val deepSeekApi: DeepSeekApi,
    @ApplicationContext private val context: android.content.Context
) {
    companion object {
        private const val TAG = "DeepSeekService"
    }

    suspend fun detectScene(imageBitmap: Bitmap?): AiService.SceneDetectionResult {
        return try {
            Log.d(TAG, "Starting scene detection with image: ${imageBitmap != null}")

            val response = if (imageBitmap != null) {
                val request = SceneDetectionPrompt.buildVisionRequest(imageBitmap)
                val apiKey = DeepSeekConfig.getApiKey()
                Log.d(TAG, "Calling DeepSeek Vision API...")
                deepSeekApi.chatCompletionWithVision(
                    authorization = "Bearer $apiKey",
                    request = request
                )
            } else {
                Log.d(TAG, "No image provided, using fallback detection")
                return fallbackDetection()
            }

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "DeepSeek API response: ${response.code()}")
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
                    Log.w(TAG, "Failed to parse scene type from API response")
                    fallbackDetection()
                }
            } else {
                Log.e(TAG, "API call failed: ${response.code()} - ${response.message()}")
                fallbackDetection()
            }
        } catch (e: Exception) {
            Log.e(TAG, "DeepSeek API exception: ${e.message}", e)
            fallbackDetection()
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

    /**
     * 方案1 P0: DeepSeek API 失败时返回 UNKNOWN，不随机编造场景
     * 核心原则：宁可返回 UNKNOWN，也绝不随机编造场景
     */
    private fun fallbackDetection(): AiService.SceneDetectionResult {
        Log.w(TAG, "DeepSeek API 不可用，返回 UNKNOWN（不随机编造场景）")
        return AiService.SceneDetectionResult(
            primaryScene = SceneType.UNKNOWN,
            confidence = 0f,
            isEdgeCase = false,
            edgeCaseMessage = "AI 服务暂时不可用，请稍后重试"
        )
    }
}
