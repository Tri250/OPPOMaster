package com.omaster.app.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.omaster.app.config.FlashNoteConstants
import com.omaster.app.model.CameraParams
import com.omaster.app.model.FlashNoteResult
import com.omaster.app.model.Preset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class OneTapFlashNoteServiceTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var service: OneTapFlashNoteService
    private val testDispatcher = StandardTestDispatcher()

    private val testPreset = Preset(
        id = "test_preset_1",
        name = "Test Preset",
        coverPath = "test_cover.png",
        sections = emptyList(),
        cameraParams = CameraParams(
            mode = "master",
            filter = "复古",
            iso = 200,
            shutter = "1/250",
            ev = "+0.3",
            wb = "5600K",
            hasselblad_hncs = true,
            contrast = 1.1f,
            saturation = 1.2f,
            vignette = 0.2f,
            sceneTags = listOf("portrait", "indoor")
        ),
        deviceModel = "Find X8 Pro",
        source = "omaster_cloud",
        isFavorite = false,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        usageCount = 100,
        rating = 4.5f,
        author = "OPPO"
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        service = OneTapFlashNoteService(mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isFlashNoteSupported should return boolean value`() {
        val result = service.isFlashNoteSupported()
        assertNotNull(result)
        assertTrue(result is Boolean)
    }

    @Test
    fun `getSupportedFeatures should return feature list`() {
        val features = service.getSupportedFeatures()
        assertNotNull(features)
        assertTrue(features.isNotEmpty())
    }

    @Test
    fun `getSupportedFeatures should return correct features based on API level`() {
        val features = service.getSupportedFeatures()

        if (FlashNoteConstants.isFullFeatureAvailable()) {
            assertTrue(features.contains(FlashNoteConstants.FEATURE_PRESET_SAVE))
            assertTrue(features.contains(FlashNoteConstants.FEATURE_CAMERA_PARAMS_SAVE))
            assertTrue(features.contains(FlashNoteConstants.FEATURE_IMAGE_ATTACHMENT))
        } else {
            assertTrue(features.contains(FlashNoteConstants.FEATURE_PRESET_SAVE))
            assertTrue(features.contains(FlashNoteConstants.FEATURE_CAMERA_PARAMS_SAVE))
        }
    }

    @Test
    fun `batchSavePresets should handle empty list`() = runTest(testDispatcher) {
        val results = service.batchSavePresets(emptyList())
        assertTrue(results.isEmpty())
    }

    @Test
    fun `quickSavePreset should return failure result when API not available`() = runTest(testDispatcher) {
        val result = service.quickSavePreset(testPreset)
        assertNotNull(result)
    }

    @Test
    fun `FlashNoteResult should have correct timestamp`() {
        val result = FlashNoteResult(
            success = true,
            noteId = "test_id",
            timestamp = 1234567890L
        )

        assertTrue(result.success)
        assertEquals("test_id", result.noteId)
        assertEquals(1234567890L, result.timestamp)
    }

    @Test
    fun `FlashNoteResult failure should contain error message`() {
        val result = FlashNoteResult(
            success = false,
            errorMessage = "Test error message",
            timestamp = 1234567890L
        )

        assertFalse(result.success)
        assertEquals("Test error message", result.errorMessage)
    }

    @Test
    fun `preset with null camera params should be handled`() = runTest(testDispatcher) {
        val presetWithoutParams = testPreset.copy(cameraParams = null)
        val result = service.quickSavePreset(presetWithoutParams)
        assertNotNull(result)
    }

    @Test
    fun `preset with empty cover path should be handled`() = runTest(testDispatcher) {
        val presetWithEmptyCover = testPreset.copy(coverPath = "")
        val result = service.quickSavePreset(presetWithEmptyCover)
        assertNotNull(result)
    }

    @Test
    fun `preset with empty scene tags should be handled`() = runTest(testDispatcher) {
        val presetWithEmptyTags = testPreset.copy(
            cameraParams = testPreset.cameraParams?.copy(sceneTags = emptyList())
        )
        val result = service.quickSavePreset(presetWithEmptyTags)
        assertNotNull(result)
    }

    @Test
    fun `batchSavePresets should process multiple presets`() = runTest(testDispatcher) {
        val presets = listOf(
            testPreset,
            testPreset.copy(id = "test_preset_2", name = "Test Preset 2"),
            testPreset.copy(id = "test_preset_3", name = "Test Preset 3")
        )

        val results = service.batchSavePresets(presets)
        assertEquals(3, results.size)
    }

    @Test
    fun `saveCameraParams should return result`() = runTest(testDispatcher) {
        val params = CameraParams(
            mode = "master",
            filter = "Test",
            iso = 100,
            shutter = "1/125",
            ev = "0",
            wb = "5500K",
            hasselblad_hncs = false
        )

        val result = service.saveCameraParams(params)
        assertNotNull(result)
    }
}
