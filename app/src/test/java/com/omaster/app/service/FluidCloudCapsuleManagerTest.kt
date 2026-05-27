package com.omaster.app.service

import android.content.Context
import android.graphics.Color
import android.view.WindowManager
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
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
class FluidCloudCapsuleManagerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockWindowManager: WindowManager

    private lateinit var manager: FluidCloudCapsuleManager

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
        whenever(mockContext.getSystemService(Context.WINDOW_SERVICE)).thenReturn(mockWindowManager)
        manager = FluidCloudCapsuleManager(mockContext)
    }

    @Test
    fun `createFluidCloudData should correctly map preset to data`() {
        val data = manager.createFluidCloudData(testPreset)

        assertEquals(testPreset.id, data.presetId)
        assertEquals(testPreset.name, data.title)
        assertEquals("ISO ${testPreset.cameraParams?.iso} | ${testPreset.cameraParams?.shutter}", data.subtitle)
        assertEquals("${testPreset.cameraParams?.iso}", data.isoText)
        assertEquals(testPreset.cameraParams?.shutter, data.shutterText)
        assertEquals(testPreset.cameraParams?.ev, data.evText)
        assertEquals(String.format("%.1f", testPreset.cameraParams?.contrast ?: 1.0f), data.contrastText)
        assertEquals(String.format("%.1f", testPreset.cameraParams?.saturation ?: 1.0f), data.saturationText)
        assertEquals(testPreset.cameraParams?.wb, data.wbText)
        assertEquals(String.format("%.2f", testPreset.cameraParams?.vignette ?: 0.0f), data.vignetteText)
        assertEquals(String.format("%.1f", testPreset.rating), data.ratingText)
        assertEquals("${testPreset.usageCount}次使用", data.usageText)
        assertEquals(testPreset.deviceModel, data.deviceText)
        assertEquals(testPreset.coverPath, data.iconPath)
        assertEquals(testPreset.coverPath, data.coverPath)
        assertTrue(data.showApplyBtn)
        assertTrue(data.showDetailBtn)
        assertTrue(data.showParams)
    }

    @Test
    fun `createFluidCloudData should use default values for null camera params`() {
        val presetWithoutParams = testPreset.copy(cameraParams = null)
        val data = manager.createFluidCloudData(presetWithoutParams)

        assertEquals("100", data.isoText)
        assertEquals("1/125", data.shutterText)
        assertEquals("0", data.evText)
        assertEquals("1.0", data.contrastText)
        assertEquals("1.0", data.saturationText)
        assertEquals("5500K", data.wbText)
        assertEquals("0.00", data.vignetteText)
    }

    @Test
    fun `getCurrentData should return null initially`() {
        assertNull(manager.getCurrentData())
    }

    @Test
    fun `isExpanded should return false initially`() {
        assertFalse(manager.isExpanded())
    }

    @Test
    fun `hideCapsule should not throw when no capsule exists`() {
        manager.hideCapsule()
    }

    @Test
    fun `showCapsule should not throw when no capsule exists`() {
        manager.showCapsule()
    }

    @Test
    fun `destroy should clear all data`() {
        manager.destroy()
        assertNull(manager.getCurrentData())
        assertFalse(manager.isExpanded())
    }

    @Test
    fun `updateCapsuleWithData should update current data`() {
        val newData = manager.createFluidCloudData(testPreset)
        manager.updateCapsuleWithData(newData)

        val currentData = manager.getCurrentData()
        assertNotNull(currentData)
        assertEquals(newData.title, currentData?.title)
        assertEquals(newData.presetId, currentData?.presetId)
    }

    @Test
    fun `preset with empty device model should use empty string`() {
        val presetWithEmptyDevice = testPreset.copy(deviceModel = "")
        val data = manager.createFluidCloudData(presetWithEmptyDevice)

        assertEquals("", data.deviceText)
    }

    @Test
    fun `preset with zero usage count should display correctly`() {
        val presetWithZeroUsage = testPreset.copy(usageCount = 0)
        val data = manager.createFluidCloudData(presetWithZeroUsage)

        assertEquals("0次使用", data.usageText)
    }

    @Test
    fun `preset with zero rating should display correctly`() {
        val presetWithZeroRating = testPreset.copy(rating = 0f)
        val data = manager.createFluidCloudData(presetWithZeroRating)

        assertEquals("0.0", data.ratingText)
    }

    @Test
    fun `gradient colors should be properly formatted`() {
        val data = manager.createFluidCloudData(testPreset)

        assertTrue(data.bgGradient.startsWith("linear-gradient"))
        assertTrue(data.bgGradient.contains("#6366F1"))
        assertTrue(data.bgGradient.contains("#8B5CF6"))
        assertEquals("#8B5CF6", data.borderColor)
    }
}
