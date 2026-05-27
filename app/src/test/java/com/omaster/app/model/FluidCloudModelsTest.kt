package com.omaster.app.model

import com.omaster.app.config.FluidCloudConstants
import org.junit.Assert.*
import org.junit.Test

class FluidCloudModelsTest {

    private val testCameraParams = CameraParams(
        mode = "master",
        filter = "复古",
        iso = 200,
        shutter = "1/250",
        ev = "+0.3",
        wb = "5600K",
        hasselblad_hncs = true,
        contrast = 1.1f,
        saturation = 1.2f,
        sharpness = 1.0f,
        vignette = 0.2f,
        videoLut = "",
        sceneTags = listOf("portrait", "indoor", "warm")
    )

    private val testPreset = Preset(
        id = "test_preset_1",
        name = "Test Preset",
        coverPath = "test_cover.png",
        sections = emptyList(),
        cameraParams = testCameraParams,
        deviceModel = "Find X8 Pro",
        source = "omaster_cloud",
        isFavorite = false,
        createdAt = 1234567890L,
        updatedAt = 1234567890L,
        usageCount = 100,
        rating = 4.5f,
        author = "OPPO"
    )

    @Test
    fun `FluidCloudConstants should have correct values`() {
        assertEquals("fluid_cloud_preset_template.xml", FluidCloudConstants.TEMPLATE_FILE)
        assertEquals("fluid_cloud_compact_template.xml", FluidCloudConstants.COMPACT_TEMPLATE_FILE)
        assertEquals("#6366F1", FluidCloudConstants.DEFAULT_GRADIENT_START)
        assertEquals("#8B5CF6", FluidCloudConstants.DEFAULT_GRADIENT_END)
        assertEquals("#8B5CF6", FluidCloudConstants.DEFAULT_BORDER_COLOR)
        assertEquals("colorFlow", FluidCloudConstants.ANIMATION_TYPE_COLOR_FLOW)
        assertEquals("none", FluidCloudConstants.ANIMATION_TYPE_NONE)
        assertEquals("up", FluidCloudConstants.UPDATE_TRANSFORM_UP)
        assertEquals("down", FluidCloudConstants.UPDATE_TRANSFORM_DOWN)
        assertEquals("none", FluidCloudConstants.UPDATE_TRANSFORM_NONE)
        assertEquals("notification", FluidCloudConstants.ENTRY_TYPE_NOTIFICATION)
        assertEquals("modular", FluidCloudConstants.CATEGORY_MODULAR)
        assertEquals("general", FluidCloudConstants.CATEGORY_GENERAL)
        assertEquals("mirror", FluidCloudConstants.LEADING_CATEGORY_MIRROR)
        assertEquals("switches", FluidCloudConstants.LEADING_CATEGORY_SWITCHES)
        assertEquals("common", FluidCloudConstants.CENTER_CATEGORY_COMMON)
        assertEquals("graphic-highlight", FluidCloudConstants.CENTER_CATEGORY_GRAPHIC_HIGHLIGHT)
        assertEquals("text-highlight", FluidCloudConstants.CENTER_CATEGORY_TEXT_HIGHLIGHT)
        assertEquals("multi-texts", FluidCloudConstants.TRAILING_CATEGORY_MULTI_TEXTS)
        assertEquals("multi-buttons", FluidCloudConstants.TRAILING_CATEGORY_MULTI_BUTTONS)
        assertEquals("progress", FluidCloudConstants.TRAILING_CATEGORY_PROGRESS)
        assertEquals("diff-element", FluidCloudConstants.TRAILING_CATEGORY_DIFF_ELEMENT)
        assertEquals("primary", FluidCloudConstants.BUTTON_STYLE_PRIMARY)
        assertEquals("secondary", FluidCloudConstants.BUTTON_STYLE_SECONDARY)
        assertEquals(26, FluidCloudConstants.MIN_API_LEVEL)
        assertEquals(30, FluidCloudConstants.RECOMMENDED_API_LEVEL)
    }

    @Test
    fun `createGradient should format correctly`() {
        val gradient = FluidCloudConstants.createGradient(180, "#FF5733", "#33FF57")

        assertEquals("linear-gradient(180deg,#FF5733,#33FF57)", gradient)
    }

    @Test
    fun `createGradient should handle default angle`() {
        val gradient = FluidCloudConstants.createGradient("#FF5733", "#33FF57")

        assertTrue(gradient.startsWith("linear-gradient(180deg"))
    }

    @Test
    fun `createDefaultGradient should use preset colors`() {
        val gradient = FluidCloudConstants.createDefaultGradient()

        assertTrue(gradient.contains("#6366F1"))
        assertTrue(gradient.contains("#8B5CF6"))
    }

    @Test
    fun `toFluidCloudData should correctly map preset`() {
        val fluidData = testPreset.toFluidCloudData()

        assertEquals(testPreset.id, fluidData.presetId)
        assertEquals(testPreset.name, fluidData.title)
        assertEquals("ISO ${testCameraParams.iso} | ${testCameraParams.shutter}", fluidData.subtitle)
        assertEquals(testPreset.name, fluidData.leading.titleText)
        assertEquals("ISO ${testCameraParams.iso} | ${testCameraParams.shutter}", fluidData.leading.subtitleText)
        assertEquals(testPreset.coverPath, fluidData.leading.iconPath)
        assertEquals(testPreset.name, fluidData.center.mainTitle)
        assertEquals(testPreset.coverPath, fluidData.center.coverImagePath)
        assertEquals(2, fluidData.center.buttons.size)
        assertNotNull(fluidData.trailing)
    }

    @Test
    fun `toFluidCloudData should handle null camera params with defaults`() {
        val presetWithoutParams = testPreset.copy(cameraParams = null)
        val fluidData = presetWithoutParams.toFluidCloudData()

        assertEquals(testPreset.id, fluidData.presetId)
        assertEquals(testPreset.name, fluidData.title)
        assertEquals("ISO 100 | 1/125", fluidData.subtitle)
        assertEquals("100", fluidData.center.cameraParams.iso)
        assertEquals("1/125", fluidData.center.cameraParams.shutter)
        assertEquals("0", fluidData.center.cameraParams.ev)
    }

    @Test
    fun `toFluidCloudData should include correct buttons`() {
        val fluidData = testPreset.toFluidCloudData()

        assertEquals(2, fluidData.center.buttons.size)
        assertEquals("应用", fluidData.center.buttons[0].text)
        assertEquals("apply", fluidData.center.buttons[0].action)
        assertEquals("详情", fluidData.center.buttons[1].text)
        assertEquals("detail", fluidData.center.buttons[1].action)
    }

    @Test
    fun `toFluidCloudData should format camera params correctly`() {
        val fluidData = testPreset.toFluidCloudData()

        assertEquals(testCameraParams.iso.toString(), fluidData.center.cameraParams.iso)
        assertEquals(testCameraParams.shutter, fluidData.center.cameraParams.shutter)
        assertEquals(testCameraParams.ev, fluidData.center.cameraParams.ev)
        assertEquals(String.format("%.1f", testCameraParams.contrast), fluidData.center.cameraParams.contrast)
        assertEquals(String.format("%.1f", testCameraParams.saturation), fluidData.center.cameraParams.saturation)
        assertEquals(testCameraParams.wb, fluidData.center.cameraParams.whiteBalance)
        assertEquals(String.format("%.2f", testCameraParams.vignette), fluidData.center.cameraParams.vignette)
    }

    @Test
    fun `toFluidCloudData should include trailing data`() {
        val fluidData = testPreset.toFluidCloudData()

        assertNotNull(fluidData.trailing)
        assertEquals(3, fluidData.trailing?.texts?.size)
        assertEquals(String.format("%.1f", testPreset.rating), fluidData.trailing?.texts?.get(0))
        assertEquals("${testPreset.usageCount}次使用", fluidData.trailing?.texts?.get(1))
        assertEquals(testPreset.deviceModel, fluidData.trailing?.texts?.get(2))
    }

    @Test
    fun `toUiDataMap should map all fields correctly`() {
        val fluidData = testPreset.toFluidCloudData()
        val uiMap = fluidData.toUiDataMap()

        assertEquals(testPreset.name, uiMap["presetName"])
        assertEquals(testCameraParams.iso.toString(), uiMap["isoText"])
        assertEquals(testCameraParams.shutter, uiMap["shutterText"])
        assertEquals(testCameraParams.ev, uiMap["evText"])
        assertEquals(String.format("%.1f", testCameraParams.contrast), uiMap["contrastText"])
        assertEquals(String.format("%.1f", testCameraParams.saturation), uiMap["saturationText"])
        assertEquals(testCameraParams.wb, uiMap["wbText"])
        assertEquals(String.format("%.2f", testCameraParams.vignette), uiMap["vignetteText"])
        assertEquals(String.format("%.1f", testPreset.rating), uiMap["ratingText"])
        assertEquals("${testPreset.usageCount}次使用", uiMap["usageText"])
        assertEquals(testPreset.deviceModel, uiMap["deviceText"])
        assertEquals(testPreset.coverPath, uiMap["iconPath"])
        assertEquals(testPreset.coverPath, uiMap["coverPath"])
        assertEquals("应用", uiMap["applyBtnText"])
        assertEquals("详情", uiMap["detailBtnText"])
        assertTrue(uiMap["showApplyBtn"] as Boolean)
        assertTrue(uiMap["showDetailBtn"] as Boolean)
        assertTrue(uiMap["showParams"] as Boolean)
        assertTrue(uiMap["showTrailing"] as Boolean)
    }

    @Test
    fun `toUiDataMap should handle missing trailing data`() {
        val fluidData = testPreset.toFluidCloudData().copy(trailing = null)
        val uiMap = fluidData.toUiDataMap()

        assertEquals("0.0", uiMap["ratingText"])
        assertEquals("0次使用", uiMap["usageText"])
        assertEquals("", uiMap["deviceText"])
        assertFalse(uiMap["showTrailing"] as Boolean)
    }

    @Test
    fun `LeadingData should have correct defaults`() {
        val leading = LeadingData(
            iconPath = "test.png",
            titleText = "Test Title",
            subtitleText = "Test Subtitle"
        )

        assertEquals(FluidCloudConstants.LEADING_CATEGORY_MIRROR, leading.category)
        assertEquals("test.png", leading.iconPath)
        assertEquals("Test Title", leading.titleText)
        assertEquals("Test Subtitle", leading.subtitleText)
        assertTrue(leading.showIconBg)
        assertTrue(leading.smallMargin)
    }

    @Test
    fun `CenterData should have correct defaults`() {
        val center = CenterData(
            mainTitle = "Test",
            cameraParams = CameraParamsDisplay(
                iso = "100",
                shutter = "1/125",
                ev = "0",
                contrast = "1.0",
                saturation = "1.0",
                whiteBalance = "5500K",
                vignette = "0.0"
            ),
            coverImagePath = "test.png",
            buttons = emptyList(),
            paramsDisplay = null
        )

        assertEquals(FluidCloudConstants.CENTER_CATEGORY_COMMON, center.category)
        assertEquals("Test", center.mainTitle)
        assertEquals("test.png", center.coverImagePath)
        assertTrue(center.buttons.isEmpty())
        assertNull(center.paramsDisplay)
        assertFalse(center.showLargeImage)
    }

    @Test
    fun `TrailingData should have correct defaults`() {
        val trailing = TrailingData(
            texts = listOf("Text 1", "Text 2")
        )

        assertEquals(FluidCloudConstants.TRAILING_CATEGORY_MULTI_TEXTS, trailing.category)
        assertEquals(2, trailing.texts.size)
        assertTrue(trailing.divider)
        assertFalse(trailing.showIconBg)
        assertFalse(trailing.smallMargin)
    }

    @Test
    fun `ButtonData should have correct defaults`() {
        val button = ButtonData(
            text = "Click Me",
            action = "click"
        )

        assertEquals("Click Me", button.text)
        assertEquals("click", button.action)
        assertEquals("E1*", button.level)
    }

    @Test
    fun `ParamDisplay should have correct defaults`() {
        val param = ParamDisplay(
            label = "Contrast",
            value = "1.0"
        )

        assertEquals("Contrast", param.label)
        assertEquals("1.0", param.value)
        assertEquals("C5", param.level)
    }

    @Test
    fun `CameraParamsDisplay should contain all expected fields`() {
        val params = CameraParamsDisplay(
            iso = "100",
            shutter = "1/125",
            ev = "0",
            contrast = "1.1",
            saturation = "1.2",
            whiteBalance = "5600K",
            vignette = "0.2"
        )

        assertEquals("100", params.iso)
        assertEquals("1/125", params.shutter)
        assertEquals("0", params.ev)
        assertEquals("1.1", params.contrast)
        assertEquals("1.2", params.saturation)
        assertEquals("5600K", params.whiteBalance)
        assertEquals("0.2", params.vignette)
    }

    @Test
    fun `FluidCloudPresetData should have correct defaults`() {
        val fluidData = FluidCloudPresetData(
            presetId = "test",
            title = "Test",
            subtitle = "Subtitle",
            leading = LeadingData(
                iconPath = "",
                titleText = "",
                subtitleText = ""
            ),
            center = CenterData(
                mainTitle = "",
                cameraParams = CameraParamsDisplay(
                    iso = "",
                    shutter = "",
                    ev = "",
                    contrast = "",
                    saturation = "",
                    whiteBalance = "",
                    vignette = ""
                ),
                coverImagePath = "",
                buttons = emptyList(),
                paramsDisplay = null
            ),
            trailing = null,
            backgroundColor = "",
            borderColor = ""
        )

        assertEquals(FluidCloudConstants.ANIMATION_TYPE_COLOR_FLOW, fluidData.animationType)
        assertEquals(FluidCloudConstants.UPDATE_TRANSFORM_NONE, fluidData.updateTransform)
    }
}
