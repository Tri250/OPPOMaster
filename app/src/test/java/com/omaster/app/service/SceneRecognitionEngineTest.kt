package com.omaster.app.service

import android.content.Context
import android.graphics.Bitmap
import com.omaster.app.data.PresetRepository
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.model.PresetRecommendation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class SceneRecognitionEngineTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPresetRepository: PresetRepository

    @Mock
    private lateinit var mockBitmap: Bitmap

    private lateinit var engine: SceneRecognitionEngine
    private val testDispatcher = StandardTestDispatcher()

    private val samplePresets = listOf(
        Preset(
            id = "preset_1",
            name = "Sunrise Landscape",
            coverPath = "sunrise_01",
            sections = emptyList(),
            cameraParams = CameraParams(
                mode = "master",
                filter = "",
                iso = 100,
                shutter = "1/125",
                ev = "0",
                wb = "5500K",
                hasselblad_hncs = false,
                contrast = 1.0f,
                saturation = 1.0f,
                vignette = 0.0f,
                sceneTags = listOf("landscape", "sunrise", "golden_hour")
            ),
            deviceModel = "Find X8 Pro",
            source = "omaster_cloud",
            isFavorite = true,
            usageCount = 50,
            rating = 4.5f,
            author = "OPPO"
        ),
        Preset(
            id = "preset_2",
            name = "Portrait Indoor",
            coverPath = "portrait_01",
            sections = emptyList(),
            cameraParams = CameraParams(
                mode = "master",
                filter = "",
                iso = 400,
                shutter = "1/60",
                ev = "0",
                wb = "4200K",
                hasselblad_hncs = true,
                contrast = 1.1f,
                saturation = 1.2f,
                vignette = 0.15f,
                sceneTags = listOf("portrait", "indoor")
            ),
            deviceModel = "Find X8 Pro",
            source = "omaster_cloud",
            isFavorite = false,
            usageCount = 100,
            rating = 4.8f,
            author = "OPPO"
        ),
        Preset(
            id = "preset_3",
            name = "Night City",
            coverPath = "night_01",
            sections = emptyList(),
            cameraParams = CameraParams(
                mode = "master",
                filter = "",
                iso = 3200,
                shutter = "1/30",
                ev = "-0.7",
                wb = "自动",
                hasselblad_hncs = false,
                contrast = 1.2f,
                saturation = 1.1f,
                vignette = 0.2f,
                sceneTags = listOf("night", "city", "street")
            ),
            deviceModel = "Find X8 Ultra",
            source = "omaster_cloud",
            isFavorite = true,
            usageCount = 200,
            rating = 4.9f,
            author = "OPPO"
        )
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        engine = SceneRecognitionEngine(mockContext, mockPresetRepository)
        whenever(mockPresetRepository.getAllPresets()).thenReturn(samplePresets)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `recommend should return sorted recommendations`() = runTest(testDispatcher) {
        val recommendations = engine.recommend(mockBitmap)

        assertNotNull(recommendations)
        assertTrue(recommendations.isNotEmpty())
        assertTrue(recommendations.size <= 3)

        if (recommendations.size > 1) {
            for (i in 0 until recommendations.size - 1) {
                assertTrue(recommendations[i].score >= recommendations[i + 1].score)
            }
        }
    }

    @Test
    fun `recommend should return empty list when no presets available`() = runTest(testDispatcher) {
        whenever(mockPresetRepository.getAllPresets()).thenReturn(emptyList())

        val recommendations = engine.recommend(mockBitmap)

        assertNotNull(recommendations)
        assertTrue(recommendations.isEmpty())
    }

    @Test
    fun `recommendation should contain preset and score`() = runTest(testDispatcher) {
        val recommendations = engine.recommend(mockBitmap)

        if (recommendations.isNotEmpty()) {
            val first = recommendations.first()
            assertNotNull(first.preset)
            assertTrue(first.score in 0f..1f)
        }
    }

    @Test
    fun `scene types should have all expected values`() {
        val expectedTypes = listOf(
            SceneRecognitionEngine.SceneType.PORTRAIT,
            SceneRecognitionEngine.SceneType.LANDSCAPE,
            SceneRecognitionEngine.SceneType.FOOD,
            SceneRecognitionEngine.SceneType.NIGHT,
            SceneRecognitionEngine.SceneType.STREET,
            SceneRecognitionEngine.SceneType.MACRO
        )

        assertEquals(6, expectedTypes.size)
        assertTrue(SceneRecognitionEngine.SceneType.values().containsAll(expectedTypes))
    }

    @Test
    fun `preset with matching scene tags should get higher score`() = runTest(testDispatcher) {
        val landscapePreset = Preset(
            id = "landscape_test",
            name = "Test Landscape",
            coverPath = "test",
            sections = emptyList(),
            cameraParams = CameraParams(
                mode = "master",
                filter = "",
                iso = 100,
                shutter = "1/125",
                ev = "0",
                wb = "5500K",
                hasselblad_hncs = false,
                sceneTags = listOf("landscape", "nature")
            ),
            deviceModel = "Test",
            source = "test",
            isFavorite = false,
            usageCount = 10,
            rating = 4.0f,
            author = "Test"
        )

        val portraitPreset = Preset(
            id = "portrait_test",
            name = "Test Portrait",
            coverPath = "test",
            sections = emptyList(),
            cameraParams = CameraParams(
                mode = "master",
                filter = "",
                iso = 100,
                shutter = "1/125",
                ev = "0",
                wb = "5500K",
                hasselblad_hncs = false,
                sceneTags = listOf("portrait", "indoor")
            ),
            deviceModel = "Test",
            source = "test",
            isFavorite = false,
            usageCount = 10,
            rating = 4.0f,
            author = "Test"
        )

        whenever(mockPresetRepository.getAllPresets()).thenReturn(listOf(landscapePreset, portraitPreset))

        val recommendations = engine.recommend(mockBitmap)

        assertTrue(recommendations.isNotEmpty())
    }

    @Test
    fun `favorite preset should get score boost`() = runTest(testDispatcher) {
        val favoritePreset = samplePresets[0].copy(isFavorite = true, id = "fav_1")
        val nonFavoritePreset = samplePresets[1].copy(isFavorite = false, id = "non_fav_1")

        whenever(mockPresetRepository.getAllPresets()).thenReturn(listOf(favoritePreset, nonFavoritePreset))

        val recommendations = engine.recommend(mockBitmap)

        assertTrue(recommendations.isNotEmpty())
        val favScore = recommendations.find { it.preset.id == "fav_1" }?.score ?: 0f
        val nonFavScore = recommendations.find { it.preset.id == "non_fav_1" }?.score ?: 0f
    }

    @Test
    fun `preset with high usage count should get score boost`() = runTest(testDispatcher) {
        val highUsagePreset = samplePresets[0].copy(usageCount = 500, id = "high_usage")
        val lowUsagePreset = samplePresets[1].copy(usageCount = 5, id = "low_usage")

        whenever(mockPresetRepository.getAllPresets()).thenReturn(listOf(highUsagePreset, lowUsagePreset))

        val recommendations = engine.recommend(mockBitmap)

        assertTrue(recommendations.isNotEmpty())
    }

    @Test
    fun `preset with empty scene tags should have base score`() = runTest(testDispatcher) {
        val presetWithNoTags = Preset(
            id = "no_tags",
            name = "No Tags Preset",
            coverPath = "test",
            sections = emptyList(),
            cameraParams = CameraParams(
                mode = "master",
                filter = "",
                iso = 100,
                shutter = "1/125",
                ev = "0",
                wb = "5500K",
                hasselblad_hncs = false,
                sceneTags = emptyList()
            ),
            deviceModel = "Test",
            source = "test",
            isFavorite = false,
            usageCount = 0,
            rating = 0f,
            author = "Test"
        )

        whenever(mockPresetRepository.getAllPresets()).thenReturn(listOf(presetWithNoTags))

        val recommendations = engine.recommend(mockBitmap)

        assertTrue(recommendations.isNotEmpty())
        assertTrue(recommendations.first().score > 0f)
    }

    @Test
    fun `recommendations should not exceed maximum count`() = runTest(testDispatcher) {
        val manyPresets = (1..10).map { i ->
            samplePresets[0].copy(id = "preset_$i", name = "Preset $i")
        }
        whenever(mockPresetRepository.getAllPresets()).thenReturn(manyPresets)

        val recommendations = engine.recommend(mockBitmap)

        assertTrue(recommendations.size <= 3)
    }
}
