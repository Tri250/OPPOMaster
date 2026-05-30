package com.omaster.app.service

import android.graphics.Bitmap
import com.omaster.app.camera.ImageAnalyzer
import com.omaster.app.camera.DetailLevel
import com.omaster.app.camera.BrightnessLevel
import com.omaster.app.camera.ContrastLevel
import com.omaster.app.camera.CameraParamsAnalysis
import com.omaster.app.ml.LocalSceneClassifier
import com.omaster.app.ml.SceneClassification
import com.omaster.app.model.Preset
import com.omaster.app.model.SceneType
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
class AiServiceTest {

    @Mock
    private lateinit var mockLocalSceneClassifier: LocalSceneClassifier

    @Mock
    private lateinit var mockImageAnalyzer: ImageAnalyzer

    private lateinit var aiService: AiService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        aiService = AiService(mockLocalSceneClassifier, mockImageAnalyzer)
    }

    @Test
    fun `SceneDetectionResult should hold all fields correctly`() {
        val result = AiService.SceneDetectionResult(
            primaryScene = SceneType.PORTRAIT,
            secondaryScene = SceneType.NIGHT_PORTRAIT,
            confidence = 0.9f,
            isEdgeCase = false
        )

        assertEquals(SceneType.PORTRAIT, result.primaryScene)
        assertEquals(SceneType.NIGHT_PORTRAIT, result.secondaryScene)
        assertEquals(0.9f, result.confidence, 0.01f)
        assertFalse(result.isEdgeCase)
    }

    @Test
    fun `SceneDetectionResult should have default confidence`() {
        val result = AiService.SceneDetectionResult(
            primaryScene = SceneType.LANDSCAPE
        )

        assertEquals(0.85f, result.confidence, 0.01f)
    }

    @Test
    fun `detectScene should return UNKNOWN when ML Kit fails and heuristics can't match`() = runTest {
        val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        
        `when`(mockImageAnalyzer.analyzeImageForParams(any(Bitmap::class.java)))
            .thenReturn(CameraParamsAnalysis(
                brightness = 50f,
                brightnessLevel = BrightnessLevel.NORMAL,
                edgeDensity = 50f,
                detailLevel = DetailLevel.NORMAL,
                contrast = 50f,
                contrastLevel = ContrastLevel.NORMAL,
                wbEstimate = 5000
            ))
        
        `when`(mockLocalSceneClassifier.classify(any(Bitmap::class.java)))
            .thenReturn(SceneClassification(SceneType.UNKNOWN, 0f))

        val result = aiService.detectScene(bitmap = testBitmap)

        assertEquals(SceneType.UNKNOWN, result.primaryScene)
        assertEquals(0f, result.confidence, 0.01f)
    }

    @Test
    fun `detectScene should return BLACK for very dark images`() = runTest {
        val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        
        `when`(mockImageAnalyzer.analyzeImageForParams(any(Bitmap::class.java)))
            .thenReturn(CameraParamsAnalysis(
                brightness = 5f,
                brightnessLevel = BrightnessLevel.VERY_DARK,
                edgeDensity = 20f,
                detailLevel = DetailLevel.LOW,
                contrast = 30f,
                contrastLevel = ContrastLevel.LOW,
                wbEstimate = 3000
            ))

        val result = aiService.detectScene(bitmap = testBitmap)

        assertEquals(SceneType.BLACK, result.primaryScene)
        assertTrue(result.isEdgeCase)
        assertTrue(result.edgeCaseMessage?.contains("光线太暗") == true)
    }

    @Test
    fun `detectScene should return WHITE for very bright images`() = runTest {
        val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        
        `when`(mockImageAnalyzer.analyzeImageForParams(any(Bitmap::class.java)))
            .thenReturn(CameraParamsAnalysis(
                brightness = 95f,
                brightnessLevel = BrightnessLevel.VERY_BRIGHT,
                edgeDensity = 10f,
                detailLevel = DetailLevel.LOW,
                contrast = 20f,
                contrastLevel = ContrastLevel.LOW,
                wbEstimate = 8000
            ))

        val result = aiService.detectScene(bitmap = testBitmap)

        assertEquals(SceneType.WHITE, result.primaryScene)
        assertTrue(result.isEdgeCase)
        assertTrue(result.edgeCaseMessage?.contains("过亮") == true)
    }

    @Test
    fun `detectScene should use ML Kit classification when available`() = runTest {
        val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        
        `when`(mockImageAnalyzer.analyzeImageForParams(any(Bitmap::class.java)))
            .thenReturn(CameraParamsAnalysis(
                brightness = 50f,
                brightnessLevel = BrightnessLevel.NORMAL,
                edgeDensity = 60f,
                detailLevel = DetailLevel.NORMAL,
                contrast = 60f,
                contrastLevel = ContrastLevel.NORMAL,
                wbEstimate = 5000
            ))

        `when`(mockLocalSceneClassifier.classify(any(Bitmap::class.java)))
            .thenReturn(SceneClassification(SceneType.FOOD, 0.85f))

        val result = aiService.detectScene(bitmap = testBitmap)

        assertEquals(SceneType.FOOD, result.primaryScene)
        assertEquals(0.85f, result.confidence, 0.01f)
    }

    @Test
    fun `detectScene should use heuristics when no bitmap provided`() = runTest {
        val testUri = "content://test/image/portrait_test.jpg"
        val result = aiService.detectScene(imageUri = testUri)
        
        assertEquals(SceneType.PORTRAIT, result.primaryScene)
        assertEquals(0.92f, result.confidence, 0.01f)
    }

    @Test
    fun `detectScene should return UNKNOWN when heuristics can't match`() = runTest {
        val testUri = "content://test/image/unknown_type.jpg"
        val result = aiService.detectScene(imageUri = testUri)
        
        assertEquals(SceneType.UNKNOWN, result.primaryScene)
        assertEquals(0f, result.confidence, 0.01f)
    }

    @Test
    fun `getRecommendedPresets should return empty for edge cases`() = runTest {
        val result = AiService.SceneDetectionResult(
            primaryScene = SceneType.BLACK,
            isEdgeCase = true
        )
        val presets = listOf(
            Preset(id = "1", name = "Test Preset 1", coverPath = "test1"),
            Preset(id = "2", name = "Test Preset 2", coverPath = "test2")
        )

        val recommended = aiService.getRecommendedPresets(result, presets)

        assertTrue(recommended.isEmpty())
    }

    @Test
    fun `getRecommendedPresets should return presets for PORTRAIT`() = runTest {
        val result = AiService.SceneDetectionResult(
            primaryScene = SceneType.PORTRAIT,
            confidence = 0.9f
        )
        val presets = listOf(
            Preset(id = "1", name = "人像摄影", coverPath = "test1"),
            Preset(id = "2", name = "风景", coverPath = "test2"),
            Preset(id = "3", name = "黑柔人像", coverPath = "test3")
        )

        val recommended = aiService.getRecommendedPresets(result, presets)

        assertTrue(recommended.isNotEmpty())
        assertTrue(recommended.any { it.name.contains("人像") })
    }

    @Test
    fun `getRecommendedPresets should work with secondary scene`() = runTest {
        val result = AiService.SceneDetectionResult(
            primaryScene = SceneType.PORTRAIT,
            secondaryScene = SceneType.NIGHT,
            confidence = 0.95f
        )
        val presets = listOf(
            Preset(id = "1", name = "夜景人像", coverPath = "test1"),
            Preset(id = "2", name = "普通风景", coverPath = "test2")
        )

        val recommended = aiService.getRecommendedPresets(result, presets)

        assertTrue(recommended.isNotEmpty())
        assertTrue(recommended.any { it.name.contains("夜景") })
    }

    @Test
    fun `detectSceneLegacy should work for backward compatibility`() = runTest {
        val result = aiService.detectSceneLegacy("portrait.jpg")
        
        assertNotNull(result)
    }

    @Test
    fun `getRecommendedPresets should work with SceneType directly`() = runTest {
        val presets = listOf(
            Preset(id = "1", name = "美食摄影", coverPath = "test1"),
            Preset(id = "2", name = "风景", coverPath = "test2")
        )

        val recommended = aiService.getRecommendedPresets(SceneType.FOOD, presets)

        assertTrue(recommended.isNotEmpty())
    }

    @Test
    fun `getSceneKeywords should provide correct keywords for LANDSCAPE`() {
        val landscapeKeywords = aiService.javaClass.getDeclaredMethod("getSceneKeywords", SceneType::class.java).let {
            it.isAccessible = true
            it.invoke(aiService, SceneType.LANDSCAPE) as List<String>
        }

        assertTrue(landscapeKeywords.contains("风景"))
        assertTrue(landscapeKeywords.contains("自然"))
    }

    @Test
    fun `getSceneKeywords should return empty list for UNKNOWN`() {
        val unknownKeywords = aiService.javaClass.getDeclaredMethod("getSceneKeywords", SceneType::class.java).let {
            it.isAccessible = true
            it.invoke(aiService, SceneType.UNKNOWN) as List<String>
        }

        assertTrue(unknownKeywords.isEmpty())
    }
}
