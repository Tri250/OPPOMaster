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
}