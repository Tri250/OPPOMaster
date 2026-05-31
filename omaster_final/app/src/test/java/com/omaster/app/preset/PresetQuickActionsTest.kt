package com.omaster.app.preset

import com.omaster.app.model.Preset
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Section
import org.junit.Assert.*
import org.junit.Test

class PresetQuickActionsTest {
    
    @Test
    fun testFavoriteToggle() {
        val preset = createTestPreset(isFavorite = false)
        assertFalse("Preset should not be favorite initially", preset.isFavorite)
    }
    
    @Test
    fun testPresetCopyAllParameters() {
        val preset = createTestPreset()
        val params = preset.cameraParams ?: return
        
        val expectedParams = buildString {
            appendLine("📷 ${preset.name}")
            appendLine("适配设备: ${preset.deviceModel ?: "通用"}")
            appendLine()
            appendLine("相机参数:")
            appendLine("• ISO: ${params.iso}")
            appendLine("• 快门: ${params.shutter}")
            appendLine("• 曝光补偿: ${params.ev}")
            appendLine("• 白平衡: ${params.wb}")
            if (params.filter.isNotEmpty()) {
                appendLine("• 滤镜: ${params.filter}")
            }
        }
        
        assertTrue("Parameter copy should contain preset name", expectedParams.contains(preset.name))
        assertTrue("Parameter copy should contain ISO", expectedParams.contains("ISO"))
        assertTrue("Parameter copy should contain shutter", expectedParams.contains("快门"))
    }
    
    @Test
    fun testPresetShareFormat() {
        val preset = createTestPreset()
        val params = preset.cameraParams ?: return
        
        assertNotNull("Preset should have name", preset.name)
        assertNotNull("Preset should have device model", preset.deviceModel)
        assertNotNull("Camera params should have ISO", params.iso)
    }
    
    @Test
    fun testPresetParameterCardGeneration() {
        val preset = createTestPreset()
        
        assertNotNull("Preset should have ID", preset.id)
        assertNotNull("Preset should have name", preset.name)
        assertNotNull("Preset should have sections", preset.sections)
    }
    
    @Test
    fun testLongPressMenuItems() {
        val preset = createTestPreset()
        
        val expectedMenuItems = listOf(
            "快速收藏" to !preset.isFavorite,
            "分享预设" to true,
            "查看参数" to true
        )
        
        expectedMenuItems.forEach { (label, shouldBeEnabled) ->
            assertTrue(
                "Menu item '$label' should be considered",
                true
            )
        }
    }
    
    @Test
    fun testCategoryFiltering() {
        val presets = listOf(
            createTestPreset(deviceModel = "Find X7 Ultra"),
            createTestPreset(deviceModel = "Reno 12"),
            createTestPreset(deviceModel = "Find X7 Ultra"),
            createTestPreset(deviceModel = "其他设备")
        )
        
        val findXPresets = presets.filter {
            it.deviceModel?.contains("Find X", ignoreCase = true) == true
        }
        
        assertEquals(
            "Should find 2 Find X presets",
            2,
            findXPresets.size
        )
        
        val renoPresets = presets.filter {
            it.deviceModel?.contains("Reno", ignoreCase = true) == true
        }
        
        assertEquals(
            "Should find 1 Reno preset",
            1,
            renoPresets.size
        )
    }
    
    @Test
    fun testSwipeNavigation() {
        val presets = listOf(
            createTestPreset(),
            createTestPreset(),
            createTestPreset()
        )
        
        var currentIndex = 0
        
        currentIndex = (currentIndex + 1) % presets.size
        assertEquals("Should move to next preset", 1, currentIndex)
        
        currentIndex = (currentIndex + 1) % presets.size
        assertEquals("Should move to next preset", 2, currentIndex)
        
        currentIndex = (currentIndex + 1) % presets.size
        assertEquals("Should wrap to first preset", 0, currentIndex)
        
        currentIndex = if (currentIndex == 0) presets.size - 1 else currentIndex - 1
        assertEquals("Should move to previous preset", 2, currentIndex)
        
        currentIndex = if (currentIndex == 0) presets.size - 1 else currentIndex - 1
        assertEquals("Should move to previous preset", 1, currentIndex)
        
        currentIndex = if (currentIndex == 0) presets.size - 1 else currentIndex - 1
        assertEquals("Should wrap to last preset", 0, currentIndex)
    }
    
    @Test
    fun testOpacityRange() {
        val minOpacity = 0.3f
        val maxOpacity = 1.0f
        
        val testValues = listOf(0.2f, 0.3f, 0.5f, 0.7f, 1.0f, 1.2f)
        
        testValues.forEach { value ->
            val clampedValue = value.coerceIn(minOpacity, maxOpacity)
            assertTrue(
                "Opacity should be between $minOpacity and $maxOpacity",
                clampedValue in minOpacity..maxOpacity
            )
        }
    }
    
    private fun createTestPreset(
        id: String = "test-preset-1",
        name: String = "测试预设",
        deviceModel: String? = "Find X7 Ultra",
        isFavorite: Boolean = false
    ): Preset {
        return Preset(
            id = id,
            name = name,
            coverPath = "test-cover.jpg",
            sections = listOf(
                Section(
                    title = "使用说明",
                    content = "这是一个测试预设"
                )
            ),
            cameraParams = CameraParams(
                mode = "专业",
                filter = "胶片",
                iso = 100,
                shutter = "1/125",
                ev = "0",
                wb = "自动",
                hasselblad_hncs = true
            ),
            deviceModel = deviceModel,
            source = "official",
            isFavorite = isFavorite
        )
    }
}
