package com.omaster.app.network

import android.graphics.Bitmap
import com.omaster.app.model.SceneType
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.mock

class DeepSeekModelsTest {

    @Test
    fun `DeepSeekConfig should provide API key`() {
        val apiKey = DeepSeekConfig.getApiKey()
        assertNotNull(apiKey)
        assertTrue(apiKey.isNotEmpty())
    }

    @Test
    fun `DeepSeekConfig should have correct base URL`() {
        assertEquals("https://api.deepseek.com/", DeepSeekConfig.BASE_URL)
    }

    @Test
    fun `DeepSeekConfig should have correct vision model`() {
        assertEquals("deepseek-chat", DeepSeekConfig.VISION_MODEL)
    }

    @Test
    fun `buildTextRequest should create valid request`() {
        val request = SceneDetectionPrompt.buildTextRequest()

        assertEquals("deepseek-chat", request.model)
        assertEquals(2, request.messages.size)
        assertEquals("system", request.messages[0].role)
        assertEquals("user", request.messages[1].role)
        assertEquals(0.3, request.temperature, 0.01)
        assertEquals(50, request.max_tokens)
    }

    @Test
    fun `system prompt should contain all scene types`() {
        assertTrue(SceneDetectionPrompt.SYSTEM_PROMPT.contains("PORTRAIT"))
        assertTrue(SceneDetectionPrompt.SYSTEM_PROMPT.contains("LANDSCAPE"))
        assertTrue(SceneDetectionPrompt.SYSTEM_PROMPT.contains("NIGHT"))
        assertTrue(SceneDetectionPrompt.SYSTEM_PROMPT.contains("FOOD"))
        assertTrue(SceneDetectionPrompt.SYSTEM_PROMPT.contains("STREET"))
        assertTrue(SceneDetectionPrompt.SYSTEM_PROMPT.contains("SUNSET"))
        assertTrue(SceneDetectionPrompt.SYSTEM_PROMPT.contains("NATURE"))
        assertTrue(SceneDetectionPrompt.SYSTEM_PROMPT.contains("ARCHITECTURE"))
        assertTrue(SceneDetectionPrompt.SYSTEM_PROMPT.contains("MACRO"))
        assertTrue(SceneDetectionPrompt.SYSTEM_PROMPT.contains("SPORTS"))
        assertTrue(SceneDetectionPrompt.SYSTEM_PROMPT.contains("NIGHT_PORTRAIT"))
    }

    @Test
    fun `user prompt should contain analysis instructions`() {
        assertTrue(SceneDetectionPrompt.USER_PROMPT_TEMPLATE.contains("分析这张图片"))
        assertTrue(SceneDetectionPrompt.USER_PROMPT_TEMPLATE.contains("场景类型"))
    }

    @Test
    fun `parseSceneType should return PORTRAIT for valid response`() {
        val response = createMockResponse("PORTRAIT")
        val sceneType = SceneDetectionPrompt.parseSceneType(response)
        assertEquals(SceneType.PORTRAIT, sceneType)
    }

    @Test
    fun `parseSceneType should return LANDSCAPE for valid response`() {
        val response = createMockResponse("LANDSCAPE")
        val sceneType = SceneDetectionPrompt.parseSceneType(response)
        assertEquals(SceneType.LANDSCAPE, sceneType)
    }

    @Test
    fun `parseSceneType should return NIGHT for valid response`() {
        val response = createMockResponse("NIGHT")
        val sceneType = SceneDetectionPrompt.parseSceneType(response)
        assertEquals(SceneType.NIGHT, sceneType)
    }

    @Test
    fun `parseSceneType should return FOOD for valid response`() {
        val response = createMockResponse("FOOD")
        val sceneType = SceneDetectionPrompt.parseSceneType(response)
        assertEquals(SceneType.FOOD, sceneType)
    }

    @Test
    fun `parseSceneType should return null for invalid response`() {
        val response = createMockResponse("INVALID_TYPE")
        val sceneType = SceneDetectionPrompt.parseSceneType(response)
        assertNull(sceneType)
    }

    @Test
    fun `parseSceneType should be case insensitive`() {
        val response1 = createMockResponse("portrait")
        assertEquals(SceneType.PORTRAIT, SceneDetectionPrompt.parseSceneType(response1))

        val response2 = createMockResponse("Portrait")
        assertEquals(SceneType.PORTRAIT, SceneDetectionPrompt.parseSceneType(response2))

        val response3 = createMockResponse("PORTRAIT")
        assertEquals(SceneType.PORTRAIT, SceneDetectionPrompt.parseSceneType(response3))
    }

    @Test
    fun `parseSceneType should handle whitespace`() {
        val response = createMockResponse("  PORTRAIT  ")
        assertEquals(SceneType.PORTRAIT, SceneDetectionPrompt.parseSceneType(response))
    }

    @Test
    fun `parseSceneType should return null for empty response`() {
        val response = createMockResponse("")
        assertNull(SceneDetectionPrompt.parseSceneType(response))
    }

    @Test
    fun `DeepSeekVisionRequest should have correct structure`() {
        val request = DeepSeekVisionRequest(
            model = "deepseek-chat",
            messages = listOf(
                VisionMessage(
                    role = "system",
                    content = listOf(ContentPart.Text("Test"))
                )
            ),
            temperature = 0.3,
            max_tokens = 50
        )

        assertEquals("deepseek-chat", request.model)
        assertEquals(1, request.messages.size)
        assertEquals(0.3, request.temperature, 0.01)
        assertEquals(50, request.max_tokens)
    }

    @Test
    fun `VisionMessage should support multiple content parts`() {
        val message = VisionMessage(
            role = "user",
            content = listOf(
                ContentPart.Text("分析这张图片"),
                ContentPart.ImageUrl("data:image/jpeg;base64,abc123")
            )
        )

        assertEquals("user", message.role)
        assertEquals(2, message.content.size)
        assertTrue(message.content[0] is ContentPart.Text)
        assertTrue(message.content[1] is ContentPart.ImageUrl)
    }

    @Test
    fun `ContentPart_Text should hold correct value`() {
        val textPart = ContentPart.Text("Hello World")
        assertEquals("Hello World", textPart.text)
    }

    @Test
    fun `ContentPart_ImageUrl should hold correct value`() {
        val imagePart = ContentPart.ImageUrl("data:image/jpeg;base64,xyz789")
        assertEquals("data:image/jpeg;base64,xyz789", imagePart.url)
    }

    @Test
    fun `DeepSeekRequest should have correct defaults`() {
        val request = DeepSeekRequest(
            messages = listOf(Message("user", "test"))
        )

        assertEquals("deepseek-chat", request.model)
        assertEquals(0.7, request.temperature, 0.01)
        assertEquals(200, request.max_tokens)
    }

    @Test
    fun `Message should hold role and content`() {
        val message = Message("system", "You are a helpful assistant")
        assertEquals("system", message.role)
        assertEquals("You are a helpful assistant", message.content)
    }

    @Test
    fun `Choice should hold message and finish reason`() {
        val choice = Choice(
            index = 0,
            message = Message("assistant", "Hello"),
            finish_reason = "stop"
        )

        assertEquals(0, choice.index)
        assertEquals("assistant", choice.message.role)
        assertEquals("Hello", choice.message.content)
        assertEquals("stop", choice.finish_reason)
    }

    @Test
    fun `Usage should hold token counts`() {
        val usage = Usage(
            prompt_tokens = 100,
            completion_tokens = 50,
            total_tokens = 150
        )

        assertEquals(100, usage.prompt_tokens)
        assertEquals(50, usage.completion_tokens)
        assertEquals(150, usage.total_tokens)
    }

    @Test
    fun `DeepSeekResponse should hold all fields`() {
        val response = DeepSeekResponse(
            id = "chatcmpl-123",
            `object` = "chat.completion",
            created = 1234567890L,
            model = "deepseek-chat",
            choices = listOf(
                Choice(
                    index = 0,
                    message = Message("assistant", "PORTRAIT"),
                    finish_reason = "stop"
                )
            ),
            usage = Usage(100, 50, 150)
        )

        assertEquals("chatcmpl-123", response.id)
        assertEquals("chat.completion", response.`object`)
        assertEquals(1234567890L, response.created)
        assertEquals("deepseek-chat", response.model)
        assertEquals(1, response.choices.size)
        assertNotNull(response.usage)
        assertEquals(150, response.usage?.total_tokens)
    }

    private fun createMockResponse(content: String): DeepSeekResponse {
        return DeepSeekResponse(
            id = "test-id",
            `object` = "chat.completion",
            created = System.currentTimeMillis(),
            model = "deepseek-chat",
            choices = listOf(
                Choice(
                    index = 0,
                    message = Message("assistant", content),
                    finish_reason = "stop"
                )
            ),
            usage = Usage(100, 50, 150)
        )
    }
}
