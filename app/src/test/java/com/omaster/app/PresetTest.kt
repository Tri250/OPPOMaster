package com.omaster.app

import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.model.Section
import org.junit.Test
import org.junit.Assert.*

class PresetTest {
    @Test
    fun `preset creation should work correctly`() {
        val cameraParams = CameraParams(
            mode = "master",
            filter = "复古",
            iso = 200,
            shutter = "1/250",
            ev = "+0.3",
            wb = "5600K",
            hasselblad_hncs = true,
            contrast = 1.1f,
            saturation = 1.0f,
            vignette = 0.1f,
            sceneTags = listOf("landscape", "portrait")
        )

        val sections = listOf(
            Section("光感设置", "降低对比度，提高高光保留"),
            Section("色彩调校", "暖色调偏移，饱和度适中")
        )

        val preset = Preset(
            id = "1",
            name = "哈苏 X2D | 慵懒午后的佛罗伦萨",
            coverPath = "hasselblad_florence_01",
            sections = sections,
            cameraParams = cameraParams,
            deviceModel = "Find X8 Pro",
            source = "omaster_cloud",
            isFavorite = false,
            createdAt = 1234567890L,
            updatedAt = 1234567890L,
            usageCount = 100,
            rating = 4.5f,
            author = "OPPO"
        )

        assertEquals("1", preset.id)
        assertEquals("哈苏 X2D | 慵懒午后的佛罗伦萨", preset.name)
        assertEquals("Find X8 Pro", preset.deviceModel)
        assertEquals("omaster_cloud", preset.source)
        assertFalse(preset.isFavorite)
        assertEquals(100, preset.usageCount)
        assertEquals(4.5f, preset.rating)
        assertEquals("OPPO", preset.author)
    }

    @Test
    fun `preset with HNCS should be identified correctly`() {
        val presetWithHncs = Preset(
            id = "1",
            name = "Test",
            coverPath = "test",
            sections = emptyList(),
            cameraParams = CameraParams(
                mode = "master",
                filter = "",
                iso = 100,
                shutter = "1/100",
                ev = "0",
                wb = "auto",
                hasselblad_hncs = true,
                contrast = 1.1f,
                saturation = 1.2f,
                vignette = 0.1f,
                sceneTags = listOf("portrait")
            ),
            deviceModel = "",
            source = "",
            rating = 4.5f,
            author = "TestAuthor"
        )

        val presetWithoutHncs = Preset(
            id = "2",
            name = "Test 2",
            coverPath = "test2",
            sections = emptyList(),
            cameraParams = CameraParams(
                mode = "master",
                filter = "",
                iso = 100,
                shutter = "1/100",
                ev = "0",
                wb = "auto",
                hasselblad_hncs = false,
                contrast = 1.0f,
                saturation = 1.0f,
                vignette = 0.0f,
                sceneTags = emptyList()
            ),
            deviceModel = "",
            source = "",
            rating = 0f,
            author = ""
        )

        assertTrue(presetWithHncs.cameraParams?.hasselblad_hncs ?: false)
        assertFalse(presetWithoutHncs.cameraParams?.hasselblad_hncs ?: true)
        assertEquals(4.5f, presetWithHncs.rating)
        assertEquals("TestAuthor", presetWithHncs.author)
    }

    @Test
    fun `preset scene tags should be correctly parsed`() {
        val presetWithTags = Preset(
            id = "3",
            name = "Landscape Test",
            coverPath = "landscape",
            sections = emptyList(),
            cameraParams = CameraParams(
                mode = "master",
                filter = "",
                iso = 100,
                shutter = "1/100",
                ev = "0",
                wb = "auto",
                hasselblad_hncs = false,
                sceneTags = listOf("landscape", "sunset", "golden_hour")
            ),
            deviceModel = "Find X8 Pro",
            source = "omaster_community",
            rating = 4.8f,
            author = "Community"
        )

        assertEquals(3, presetWithTags.cameraParams?.sceneTags?.size)
        assertTrue(presetWithTags.cameraParams?.sceneTags?.contains("landscape") == true)
        assertTrue(presetWithTags.cameraParams?.sceneTags?.contains("sunset") == true)
        assertEquals("omaster_community", presetWithTags.source)
    }

    @Test
    fun `preset camera params should have correct defaults`() {
        val presetWithDefaults = Preset(
            id = "4",
            name = "Default Test",
            coverPath = "default",
            sections = emptyList(),
            cameraParams = null,
            deviceModel = "Test Device",
            source = "test"
        )

        assertNull(presetWithDefaults.cameraParams)
    }

    @Test
    fun `preset favorite and usage tracking`() {
        val favoritePreset = Preset(
            id = "5",
            name = "Favorite Test",
            coverPath = "favorite",
            sections = emptyList(),
            cameraParams = CameraParams(),
            deviceModel = "Test",
            source = "test",
            isFavorite = true,
            usageCount = 500,
            rating = 4.9f
        )

        assertTrue(favoritePreset.isFavorite)
        assertEquals(500, favoritePreset.usageCount)
        assertTrue(favoritePreset.rating > 4.0f)
    }
}