package com.omaster.app.network

import com.omaster.app.model.SceneType
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

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
}

object SceneDetectionPrompt {
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

    fun buildDetectionRequest(imageBase64: String?): DeepSeekRequest {
        return DeepSeekRequest(
            model = "deepseek-chat",
            messages = listOf(
                Message(role = "system", content = SYSTEM_PROMPT),
                Message(
                    role = "user", 
                    content = if (imageBase64 != null) {
                        "$USER_PROMPT_TEMPLATE\n[图片数据已提供]"
                    } else {
                        "$USER_PROMPT_TEMPLATE\n[无图片数据]"
                    }
                )
            ),
            temperature = 0.3,
            max_tokens = 50
        )
    }

    fun parseSceneType(response: DeepSeekResponse): SceneType? {
        return try {
            val content = response.choices.firstOrNull()?.message?.content?.trim()?.uppercase()
            SceneType.entries.find { it.name == content }
        } catch (e: Exception) {
            null
        }
    }
}
