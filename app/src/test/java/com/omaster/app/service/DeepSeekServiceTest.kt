package com.omaster.app.service

import android.graphics.Bitmap
import android.content.Context
import com.omaster.app.model.SceneType
import com.omaster.app.network.DeepSeekApi
import com.omaster.app.network.DeepSeekResponse
import com.omaster.app.network.DeepSeekRequest
import com.omaster.app.network.Choice
import com.omaster.app.network.Message
import com.omaster.app.network.Usage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class DeepSeekServiceTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockDeepSeekApi: DeepSeekApi

    private lateinit var deepSeekService: DeepSeekService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        deepSeekService = DeepSeekService(mockDeepSeekApi, mockContext)
    }

    @Test
    fun `fallbackDetection should return UNKNOWN when API fails`() {
        val fallbackMethod = deepSeekService.javaClass.getDeclaredMethod("fallbackDetection")
        fallbackMethod.isAccessible = true
        val result = fallbackMethod.invoke(deepSeekService) as AiService.SceneDetectionResult

        assertEquals(SceneType.UNKNOWN, result.primaryScene)
        assertEquals(0f, result.confidence, 0.01f)
    }

    @Test
    fun `fallbackDetection should not use correct edge case message`() {
        val fallbackMethod = deepSeekService.javaClass.getDeclaredMethod("fallbackDetection")
        fallbackMethod.isAccessible = true
        val result = fallbackMethod.invoke(deepSeekService) as AiService.SceneDetectionResult

        assertTrue(result.edgeCaseMessage?.contains("AI 服务暂时不可用") == true)
    }

    @Test
    fun `isEdgeCase should return true for BLACK`() {
        val isEdgeCaseMethod = deepSeekService.javaClass.getDeclaredMethod("isEdgeCase", SceneType::class.java)
        isEdgeCaseMethod.isAccessible = true
        val result = isEdgeCaseMethod.invoke(deepSeekService, SceneType.BLACK) as Boolean
        assertTrue(result)
    }

    @Test
    fun `isEdgeCase should return true for WHITE`() {
        val isEdgeCaseMethod = deepSeekService.javaClass.getDeclaredMethod("isEdgeCase", SceneType::class.java)
        isEdgeCaseMethod.isAccessible = true
        val result = isEdgeCaseMethod.invoke(deepSeekService, SceneType.WHITE) as Boolean
        assertTrue(result)
    }

    @Test
    fun `isEdgeCase should return true for BLURRY`() {
        val isEdgeCaseMethod = deepSeekService.javaClass.getDeclaredMethod("isEdgeCase", SceneType::class.java)
        isEdgeCaseMethod.isAccessible = true
        val result = isEdgeCaseMethod.invoke(deepSeekService, SceneType.BLURRY) as Boolean
        assertTrue(result)
    }

    @Test
    fun `isEdgeCase should return false for normal scene types`() {
        val isEdgeCaseMethod = deepSeekService.javaClass.getDeclaredMethod("isEdgeCase", SceneType::class.java)
        isEdgeCaseMethod.isAccessible = true

        assertFalse(isEdgeCaseMethod.invoke(deepSeekService, SceneType.PORTRAIT) as Boolean)
        assertFalse(isEdgeCaseMethod.invoke(deepSeekService, SceneType.LANDSCAPE) as Boolean)
        assertFalse(isEdgeCaseMethod.invoke(deepSeekService, SceneType.FOOD) as Boolean)
    }

    @Test
    fun `getEdgeCaseMessage should return correct message for BLACK`() {
        val getEdgeCaseMessageMethod = deepSeekService.javaClass.getDeclaredMethod("getEdgeCaseMessage", SceneType::class.java)
        getEdgeCaseMessageMethod.isAccessible = true
        val message = getEdgeCaseMessageMethod.invoke(deepSeekService, SceneType.BLACK) as String

        assertTrue(message.contains("光线太暗"))
    }

    @Test
    fun `getEdgeCaseMessage should return correct message for WHITE`() {
        val getEdgeCaseMessageMethod = deepSeekService.javaClass.getDeclaredMethod("getEdgeCaseMessage", SceneType::class.java)
        getEdgeCaseMessageMethod.isAccessible = true
        val message = getEdgeCaseMessageMethod.invoke(deepSeekService, SceneType.WHITE) as String

        assertTrue(message.contains("无法识别"))
    }

    @Test
    fun `getEdgeCaseMessage should return correct message for BLURRY`() {
        val getEdgeCaseMessageMethod = deepSeekService.javaClass.getDeclaredMethod("getEdgeCaseMessage", SceneType::class.java)
        getEdgeCaseMessageMethod.isAccessible = true
        val message = getEdgeCaseMessageMethod.invoke(deepSeekService, SceneType.BLURRY) as String

        assertTrue(message.contains("画面模糊"))
    }

    @Test
    fun `calculateConfidence should calculate correctly for usage`() {
        val calculateConfidenceMethod = deepSeekService.javaClass.getDeclaredMethod("calculateConfidence", DeepSeekResponse::class.java)
        calculateConfidenceMethod.isAccessible = true

        val testUsage = Usage(100, 50, 150)
        val testResponse = DeepSeekResponse(
            id = "test",
            `object` = "chat.completion",
            created = System.currentTimeMillis(),
            model = "deepseek-chat",
            choices = listOf(
                Choice(0, Message("assistant", "test"), "stop")
            ),
            usage = testUsage
        )

        val confidence = calculateConfidenceMethod.invoke(deepSeekService, testResponse) as Float
        assertEquals(0.95f, confidence, 0.01f)
    }

    @Test
    fun `calculateConfidence should return default when usage is null`() {
        val calculateConfidenceMethod = deepSeekService.javaClass.getDeclaredMethod("calculateConfidence", DeepSeekResponse::class.java)
        calculateConfidenceMethod.isAccessible = true

        val testResponse = DeepSeekResponse(
            id = "test",
            `object` = "chat.completion",
            created = System.currentTimeMillis(),
            model = "deepseek-chat",
            choices = listOf(
                Choice(0, Message("assistant", "test"), "stop")
            ),
            usage = null
        )

        val confidence = calculateConfidenceMethod.invoke(deepSeekService, testResponse) as Float
        assertEquals(0.85f, confidence, 0.01f)
    }

    @Test
    fun `detectScene should work without bitmap`() = runTest {
        val result = deepSeekService.detectScene(null)
        
        assertEquals(SceneType.UNKNOWN, result.primaryScene)
        assertEquals(0f, result.confidence, 0.01f)
    }
}
