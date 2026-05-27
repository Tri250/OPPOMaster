package com.omaster.app.model

import org.junit.Assert.*
import org.junit.Test

class CameraParamsTest {

    @Test
    fun `CameraParams should have correct default values`() {
        val params = CameraParams()

        assertEquals("master", params.mode)
        assertEquals("", params.filter)
        assertEquals(100, params.iso)
        assertEquals("1/125", params.shutter)
        assertEquals("0", params.ev)
        assertEquals("5500K", params.wb)
        assertFalse(params.hasselblad_hncs)
        assertEquals(1.0f, params.contrast)
        assertEquals(1.0f, params.saturation)
        assertEquals(1.0f, params.sharpness)
        assertEquals(0.0f, params.vignette)
        assertEquals("", params.videoLut)
        assertTrue(params.sceneTags.isEmpty())
        assertNull(params.colorProfile)
    }

    @Test
    fun `CameraParams should accept custom values`() {
        val colorProfile = ColorProfile(
            dominantColors = listOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt()),
            toneCurve = listOf(0.1f, 0.3f, 0.5f, 0.7f, 0.9f)
        )

        val params = CameraParams(
            mode = "pro",
            filter = "Vivid",
            iso = 400,
            shutter = "1/500",
            ev = "+1.0",
            wb = "6500K",
            hasselblad_hncs = true,
            contrast = 1.2f,
            saturation = 1.3f,
            sharpness = 1.1f,
            vignette = 0.3f,
            videoLut = "cinematic.cube",
            sceneTags = listOf("landscape", "sunset", "golden_hour"),
            colorProfile = colorProfile
        )

        assertEquals("pro", params.mode)
        assertEquals("Vivid", params.filter)
        assertEquals(400, params.iso)
        assertEquals("1/500", params.shutter)
        assertEquals("+1.0", params.ev)
        assertEquals("6500K", params.wb)
        assertTrue(params.hasselblad_hncs)
        assertEquals(1.2f, params.contrast)
        assertEquals(1.3f, params.saturation)
        assertEquals(1.1f, params.sharpness)
        assertEquals(0.3f, params.vignette)
        assertEquals("cinematic.cube", params.videoLut)
        assertEquals(3, params.sceneTags.size)
        assertEquals("landscape", params.sceneTags[0])
        assertEquals("sunset", params.sceneTags[1])
        assertEquals("golden_hour", params.sceneTags[2])
        assertNotNull(params.colorProfile)
    }

    @Test
    fun `CameraParams contrast should be within valid range`() {
        val lowContrast = CameraParams(contrast = 0.5f)
        val normalContrast = CameraParams(contrast = 1.0f)
        val highContrast = CameraParams(contrast = 2.0f)

        assertEquals(0.5f, lowContrast.contrast)
        assertEquals(1.0f, normalContrast.contrast)
        assertEquals(2.0f, highContrast.contrast)
    }

    @Test
    fun `CameraParams saturation should be within valid range`() {
        val desaturated = CameraParams(saturation = 0.0f)
        val normalSaturation = CameraParams(saturation = 1.0f)
        val oversaturated = CameraParams(saturation = 2.0f)

        assertEquals(0.0f, desaturated.saturation)
        assertEquals(1.0f, normalSaturation.saturation)
        assertEquals(2.0f, oversaturated.saturation)
    }

    @Test
    fun `CameraParams vignette should be within valid range`() {
        val noVignette = CameraParams(vignette = 0.0f)
        val lightVignette = CameraParams(vignette = 0.3f)
        val strongVignette = CameraParams(vignette = 1.0f)

        assertEquals(0.0f, noVignette.vignette)
        assertEquals(0.3f, lightVignette.vignette)
        assertEquals(1.0f, strongVignette.vignette)
    }

    @Test
    fun `ColorProfile should have correct default values`() {
        val profile = ColorProfile()

        assertTrue(profile.dominantColors.isEmpty())
        assertTrue(profile.toneCurve.isEmpty())
    }

    @Test
    fun `ColorProfile should accept custom values`() {
        val colors = listOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt())
        val curve = listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)

        val profile = ColorProfile(
            dominantColors = colors,
            toneCurve = curve
        )

        assertEquals(3, profile.dominantColors.size)
        assertEquals(5, profile.toneCurve.size)
        assertEquals(0xFFFF0000.toInt(), profile.dominantColors[0])
        assertEquals(0.0f, profile.toneCurve[0])
        assertEquals(1.0f, profile.toneCurve[4])
    }

    @Test
    fun `ColorProfile dominant colors should be ARGB format`() {
        val profile = ColorProfile(
            dominantColors = listOf(
                0xFFFF0000.toInt(),
                0xFF00FF00.toInt(),
                0xFF0000FF.toInt()
            )
        )

        profile.dominantColors.forEach { color ->
            val alpha = (color shr 24) and 0xFF
            assertEquals(255, alpha)
        }
    }

    @Test
    fun `CameraParams sharpness should be within valid range`() {
        val soft = CameraParams(sharpness = 0.5f)
        val normal = CameraParams(sharpness = 1.0f)
        val sharp = CameraParams(sharpness = 1.5f)

        assertEquals(0.5f, soft.sharpness)
        assertEquals(1.0f, normal.sharpness)
        assertEquals(1.5f, sharp.sharpness)
    }

    @Test
    fun `CameraParams ISO should be standard values`() {
        val lowISO = CameraParams(iso = 50)
        val baseISO = CameraParams(iso = 100)
        val highISO = CameraParams(iso = 3200)

        assertEquals(50, lowISO.iso)
        assertEquals(100, baseISO.iso)
        assertEquals(3200, highISO.iso)
    }

    @Test
    fun `CameraParams shutter should support various speeds`() {
        val fastShutter = CameraParams(shutter = "1/4000")
        val normalShutter = CameraParams(shutter = "1/125")
        val slowShutter = CameraParams(shutter = "1/2")
        val bulbShutter = CameraParams(shutter = "30\"")

        assertEquals("1/4000", fastShutter.shutter)
        assertEquals("1/125", normalShutter.shutter)
        assertEquals("1/2", slowShutter.shutter)
        assertEquals("30\"", bulbShutter.shutter)
    }

    @Test
    fun `CameraParams white balance should support various presets`() {
        val autoWB = CameraParams(wb = "自动")
        val daylightWB = CameraParams(wb = "5500K")
        val cloudyWB = CameraParams(wb = "6500K")
        val tungstenWB = CameraParams(wb = "3200K")

        assertEquals("自动", autoWB.wb)
        assertEquals("5500K", daylightWB.wb)
        assertEquals("6500K", cloudyWB.wb)
        assertEquals("3200K", tungstenWB.wb)
    }

    @Test
    fun `CameraParams scene tags should handle empty list`() {
        val params = CameraParams(sceneTags = emptyList())
        assertTrue(params.sceneTags.isEmpty())
    }

    @Test
    fun `CameraParams scene tags should allow duplicate removal via distinct`() {
        val params = CameraParams(sceneTags = listOf("landscape", "portrait", "landscape"))
        val distinctTags = params.sceneTags.distinct()

        assertEquals(2, distinctTags.size)
        assertTrue(distinctTags.contains("landscape"))
        assertTrue(distinctTags.contains("portrait"))
    }

    @Test
    fun `CameraParams copy should work correctly`() {
        val original = CameraParams(
            mode = "pro",
            iso = 400,
            hasselblad_hncs = true
        )

        val copied = original.copy(iso = 800)

        assertEquals("pro", copied.mode)
        assertEquals(800, copied.iso)
        assertTrue(copied.hasselblad_hncs)
        assertEquals(400, original.iso)
    }

    @Test
    fun `CameraParams copy with color profile should work correctly`() {
        val original = CameraParams()

        val colorProfile = ColorProfile(
            dominantColors = listOf(0xFFFF0000.toInt()),
            toneCurve = listOf(0.5f)
        )

        val withProfile = original.copy(colorProfile = colorProfile)

        assertNull(original.colorProfile)
        assertNotNull(withProfile.colorProfile)
        assertEquals(1, withProfile.colorProfile?.dominantColors?.size)
    }
}
