package com.omaster.app.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.omaster.app.model.SceneType
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

object DeepSeekConfig {
    private const val TAG = "DeepSeekConfig"

    private const val API_KEY_ENV_VAR = "DEEPSEEK_API_KEY"
    private const val API_KEY_BUILD_CONFIG = "sk-fcd6db5526c84a21910befd5b68d074a"

    fun getApiKey(): String {
        return try {
            val envKey = System.getenv(API_KEY_ENV_VAR)
            if (!envKey.isNullOrEmpty()) {
                Log.d(TAG, "Using API key from environment variable")
                envKey
            } else {
                Log.d(TAG, "Using default API key from BuildConfig")
                API_KEY_BUILD_CONFIG
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read API key from environment: ${e.message}")
            API_KEY_BUILD_CONFIG
        }
    }

    const val BASE_URL = "https://api.deepseek.com/"
    const val VISION_MODEL = "deepseek-chat"
}

data class DeepSeekRequest(
    val model: String = "deepseek-chat",
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 200
)

data class Message(
    val role: String,
    val content: String
)

data class DeepSeekVisionRequest(
    val model: String = "deepseek-chat",
    val messages: List<VisionMessage>,
    val temperature: Double = 0.3,
    val max_tokens: Int = 50
)

data class VisionMessage(
    val role: String,
    val content: List<ContentPart>
)

sealed class ContentPart {
    data class Text(val text: String) : ContentPart()
    data class ImageUrl(val url: String) : ContentPart()
}

data class DeepSeekResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage?
)

data class Choice(
    val index: Int,
    val message: Message,
    val finish_reason: String
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

interface DeepSeekApi {
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: DeepSeekRequest
    ): Response<DeepSeekResponse>

    @POST("v1/chat/completions")
    suspend fun chatCompletionWithVision(
        @Header("Authorization") authorization: String,
        @Body request: DeepSeekVisionRequest
    ): Response<DeepSeekResponse>
}

object SceneDetectionPrompt {
    private const val TAG = "SceneDetectionPrompt"

    const val SYSTEM_PROMPT = """
你是一个专业的AI场景识别助手。请分析图片内容并返回最合适的场景类型。

支持的场景类型：
- LANDSCAPE: 风景（户外风景、山川湖海、城市天际线）
- PORTRAIT: 人像（人物摄影、正面/侧面/背面人像）
- NIGHT: 夜景（城市夜景、星空、灯光秀）
- SUNSET: 日落（日出、日落、黄金时刻）
- FOOD: 美食（美食拍摄、甜品、饮品）
- STREET: 街头（街头纪实、街拍）
- NATURE: 自然（森林、植物、生态）
- ARCHITECTURE: 建筑（城市建筑、室内空间）
- MACRO: 微距（特写、微距摄影）
- SPORTS: 运动（快速移动物体）
- NIGHT_PORTRAIT: 夜景人像（夜晚环境下的人像）

请只返回一个场景类型，不要添加任何解释。
"""

    const val USER_PROMPT_TEMPLATE = """
请分析这张图片的主要场景类型。

注意：
1. 优先识别主要主体
2. 对于混合场景，识别最重要的那个
3. 返回时只需返回场景代码，如：PORTRAIT
"""

    fun buildTextRequest(): DeepSeekRequest {
        return DeepSeekRequest(
            model = DeepSeekConfig.VISION_MODEL,
            messages = listOf(
                Message(role = "system", content = SYSTEM_PROMPT),
                Message(role = "user", content = "$USER_PROMPT_TEMPLATE\n[无图片数据，使用启发式分析]")
            ),
            temperature = 0.3,
            max_tokens = 50
        )
    }

    fun buildVisionRequest(imageBitmap: Bitmap): DeepSeekVisionRequest {
        val imageBase64 = bitmapToBase64(imageBitmap)
        val imageUrl = "data:image/jpeg;base64,$imageBase64"

        Log.d(TAG, "Building vision request with base64 image (size: ${imageBitmap.width}x${imageBitmap.height})")

        return DeepSeekVisionRequest(
            model = DeepSeekConfig.VISION_MODEL,
            messages = listOf(
                VisionMessage(
                    role = "system",
                    content = listOf(ContentPart.Text(SYSTEM_PROMPT))
                ),
                VisionMessage(
                    role = "user",
                    content = listOf(
                        ContentPart.Text(USER_PROMPT_TEMPLATE),
                        ContentPart.ImageUrl(imageUrl)
                    )
                )
            ),
            temperature = 0.3,
            max_tokens = 50
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDimension = 512
        val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val scale = minOf(
                maxDimension.toFloat() / bitmap.width,
                maxDimension.toFloat() / bitmap.height
            )
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }

        val outputStream = java.io.ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        if (scaledBitmap !== bitmap) {
            scaledBitmap.recycle()
        }
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    fun parseSceneType(response: DeepSeekResponse): SceneType? {
        return try {
            val content = response.choices.firstOrNull()?.message?.content?.trim()?.uppercase()
            Log.d(TAG, "Parsing scene type from response: $content")
            SceneType.entries.find { it.name == content }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse scene type: ${e.message}")
            null
        }
    }
}
