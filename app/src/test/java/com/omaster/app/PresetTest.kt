package com.omaster.app

import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.model.PresetCategory
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
            hasselblad_hncs = true
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
            isFavorite = false
        )

        assertEquals("1", preset.id)
        assertEquals("哈苏 X2D | 慵懒午后的佛罗伦萨", preset.name)
        assertEquals("Find X8 Pro", preset.deviceModel)
        assertEquals("omaster_cloud", preset.source)
        assertFalse(preset.isFavorite)
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
                hasselblad_hncs = true
            ),
            deviceModel = "",
            source = ""
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
                hasselblad_hncs = false
            ),
            deviceModel = "",
            source = ""
        )

        assertTrue(presetWithHncs.cameraParams?.hasselblad_hncs ?: false)
        assertFalse(presetWithoutHncs.cameraParams?.hasselblad_hncs ?: true)
    }

    @Test
    fun `presetCategory_fromName should identify portrait category`() {
        val category = PresetCategory.fromName("人像")
        assertEquals(PresetCategory.PORTRAIT, category)
    }

    @Test
    fun `presetCategory_fromName should identify landscape category`() {
        val category = PresetCategory.fromName("风景")
        assertEquals(PresetCategory.LANDSCAPE, category)
    }

    @Test
    fun `presetCategory_fromName should identify night category`() {
        val category = PresetCategory.fromName("夜景")
        assertEquals(PresetCategory.NIGHT, category)
    }

    @Test
    fun `presetCategory_fromName should identify food category`() {
        val category = PresetCategory.fromName("美食")
        assertEquals(PresetCategory.FOOD, category)
    }

    @Test
    fun `presetCategory_fromName should handle english keywords`() {
        assertEquals(PresetCategory.PORTRAIT, PresetCategory.fromName("portrait"))
        assertEquals(PresetCategory.LANDSCAPE, PresetCategory.fromName("landscape"))
        assertEquals(PresetCategory.NIGHT, PresetCategory.fromName("night"))
        assertEquals(PresetCategory.FOOD, PresetCategory.fromName("food"))
    }

    @Test
    fun `presetCategory_fromCameraParams should identify portrait mode`() {
        val params = CameraParams(
            mode = "master",
            filter = "",
            iso = 100,
            shutter = "1/100",
            ev = "0",
            wb = "auto",
            portrait_mode = true
        )
        assertEquals(PresetCategory.PORTRAIT, PresetCategory.fromCameraParams(params))
    }

    @Test
    fun `presetCategory_fromCameraParams should identify night mode`() {
        val params = CameraParams(
            mode = "master",
            filter = "",
            iso = 3200,
            shutter = "1/30",
            ev = "0",
            wb = "auto",
            night_mode = true
        )
        assertEquals(PresetCategory.NIGHT, PresetCategory.fromCameraParams(params))
    }

    @Test
    fun `presetCategory_fromCameraParams should identify food profile`() {
        val params = CameraParams(
            mode = "master",
            filter = "美食模式",
            iso = 200,
            shutter = "1/125",
            ev = "0",
            wb = "auto",
            color_profile = "Food"
        )
        assertEquals(PresetCategory.FOOD, PresetCategory.fromCameraParams(params))
    }

    @Test
    fun `presetCategory_fromCameraParams should return null for unknown profile`() {
        val params = CameraParams(
            mode = "master",
            filter = "",
            iso = 100,
            shutter = "1/100",
            ev = "0",
            wb = "auto"
        )
        assertNull(PresetCategory.fromCameraParams(params))
    }

    @Test
    fun `presetCategory_fromCameraParams should handle null params`() {
        assertNull(PresetCategory.fromCameraParams(null))
    }

    @Test
    fun `preset_getEffectiveCategory should use explicit category first`() {
        val preset = Preset(
            id = "1",
            name = "Test",
            coverPath = "test",
            category = PresetCategory.FOOD
        )
        assertEquals(PresetCategory.FOOD, preset.getEffectiveCategory())
    }

    @Test
    fun `preset_getEffectiveCategory should fallback to camera params`() {
        val preset = Preset(
            id = "1",
            name = "Test",
            coverPath = "test",
            cameraParams = CameraParams(
                mode = "master",
                filter = "",
                iso = 100,
                shutter = "1/100",
                ev = "0",
                wb = "auto",
                night_mode = true
            )
        )
        assertEquals(PresetCategory.NIGHT, preset.getEffectiveCategory())
    }

    @Test
    fun `preset_getEffectiveCategory should fallback to name`() {
        val preset = Preset(
            id = "1",
            name = "美食摄影",
            coverPath = "test"
        )
        assertEquals(PresetCategory.FOOD, preset.getEffectiveCategory())
    }

    @Test
    fun `preset_getEffectiveCategory should return landscape as default`() {
        val preset = Preset(
            id = "1",
            name = "Unknown",
            coverPath = "test"
        )
        assertEquals(PresetCategory.LANDSCAPE, preset.getEffectiveCategory())
    }

    @Test
    fun `preset_matchesCategory should work correctly`() {
        val preset = Preset(
            id = "1",
            name = "人像",
            coverPath = "test",
            category = PresetCategory.PORTRAIT
        )
        assertTrue(preset.matchesCategory(PresetCategory.PORTRAIT))
        assertFalse(preset.matchesCategory(PresetCategory.LANDSCAPE))
    }

    @Test
    fun `preset_matchesCategories should work with empty list`() {
        val preset = Preset(
            id = "1",
            name = "Test",
            coverPath = "test"
        )
        assertTrue(preset.matchesCategories(emptyList()))
    }

    @Test
    fun `preset_matchesCategories should match any category in list`() {
        val preset = Preset(
            id = "1",
            name = "Test",
            coverPath = "test",
            category = PresetCategory.PORTRAIT
        )
        val categories = listOf(PresetCategory.PORTRAIT, PresetCategory.LANDSCAPE)
        assertTrue(preset.matchesCategories(categories))
    }

    @Test
    fun `preset_matchesCategories should return false when no match`() {
        val preset = Preset(
            id = "1",
            name = "Test",
            coverPath = "test",
            category = PresetCategory.PORTRAIT
        )
        val categories = listOf(PresetCategory.LANDSCAPE, PresetCategory.NIGHT)
        assertFalse(preset.matchesCategories(categories))
    }
}
