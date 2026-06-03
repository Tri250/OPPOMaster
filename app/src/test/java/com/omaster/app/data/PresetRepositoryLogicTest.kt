package com.omaster.app.data

import com.omaster.app.model.CameraParams
import com.omaster.app.model.ColorStyle
import com.omaster.app.model.Preset
import org.junit.Assert.*
import org.junit.Test

/**
 * ThemeMode 和设计系统单元测试
 * 覆盖：主题模式枚举、设计系统常量
 */
class ThemeModeTest {

    @Test
    fun `ThemeMode SYSTEM should have value 0`() {
        assertEquals(0, ThemeMode.SYSTEM.value)
    }

    @Test
    fun `ThemeMode LIGHT should have value 1`() {
        assertEquals(1, ThemeMode.LIGHT.value)
    }

    @Test
    fun `ThemeMode DARK should have value 2`() {
        assertEquals(2, ThemeMode.DARK.value)
    }

    @Test
    fun `ThemeMode should have exactly 3 values`() {
        assertEquals(3, ThemeMode.values().size)
    }

    @Test
    fun `ThemeMode values should be ordered correctly`() {
        val values = ThemeMode.values()
        assertEquals(ThemeMode.SYSTEM, values[0])
        assertEquals(ThemeMode.LIGHT, values[1])
        assertEquals(ThemeMode.DARK, values[2])
    }

    @Test
    fun `ThemeMode valueOf should work correctly`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.valueOf("SYSTEM"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.valueOf("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.valueOf("DARK"))
    }
}

/**
 * PresetRepository 数据逻辑单元测试
 * 由于PresetRepository依赖Android Context和Retrofit，这里测试数据逻辑
 */
class PresetRepositoryLogicTest {

    // ==================== 示例预设数据验证测试 ====================

    @Test
    fun `sample presets should have valid structure`() {
        val presets = Preset.createSamplePresets()
        presets.forEach { preset ->
            assertTrue("Preset ID should not be empty", preset.id.isNotEmpty())
            assertTrue("Preset name should not be empty", preset.name.isNotEmpty())
            assertTrue("Preset coverPath should not be empty", preset.coverPath.isNotEmpty())
        }
    }

    @Test
    fun `sample presets with HNCS should have cameraParams`() {
        val presets = Preset.createSamplePresets()
        val hncsPresets = presets.filter { it.isHncsCertified }
        hncsPresets.forEach { preset ->
            assertNotNull(
                "HNCS preset '${preset.name}' should have cameraParams",
                preset.cameraParams
            )
            assertTrue(
                "HNCS preset '${preset.name}' should have HNCS enabled in cameraParams",
                preset.cameraParams?.hasselblad_hncs == true
            )
        }
    }

    @Test
    fun `sample presets should have valid ISO ranges`() {
        val presets = Preset.createSamplePresets()
        presets.filter { it.cameraParams != null }.forEach { preset ->
            val iso = preset.cameraParams!!.iso
            assertTrue(
                "Preset '${preset.name}' ISO $iso should be in valid range",
                iso in 32..102400
            )
        }
    }

    @Test
    fun `sample presets should have valid saturation ranges`() {
        val presets = Preset.createSamplePresets()
        presets.filter { it.cameraParams != null }.forEach { preset ->
            val saturation = preset.cameraParams!!.saturation
            assertTrue(
                "Preset '${preset.name}' saturation $saturation should be in valid range",
                saturation in 0..100
            )
        }
    }

    @Test
    fun `sample presets should have valid ratings`() {
        val presets = Preset.createSamplePresets()
        presets.forEach { preset ->
            assertTrue(
                "Preset '${preset.name}' rating ${preset.rating} should be in 0-5 range",
                preset.rating in 0f..5f
            )
        }
    }

    // ==================== 过滤逻辑测试 ====================

    @Test
    fun `filter by Find X should match correct presets`() {
        val presets = Preset.createSamplePresets()
        val findXPresets = presets.filter {
            it.deviceModel.contains("Find X", ignoreCase = true)
        }
        assertTrue("Should find Find X presets", findXPresets.isNotEmpty())
        findXPresets.forEach { preset ->
            assertTrue(
                "Preset '${preset.name}' should contain Find X",
                preset.deviceModel.contains("Find X", ignoreCase = true)
            )
        }
    }

    @Test
    fun `filter by Reno should match correct presets`() {
        val presets = Preset.createSamplePresets()
        val renoPresets = presets.filter {
            it.deviceModel.contains("Reno", ignoreCase = true)
        }
        renoPresets.forEach { preset ->
            assertTrue(
                "Preset '${preset.name}' should contain Reno",
                preset.deviceModel.contains("Reno", ignoreCase = true)
            )
        }
    }

    @Test
    fun `filter by HNCS should match certified presets`() {
        val presets = Preset.createSamplePresets()
        val hncsPresets = presets.filter { it.isHncsCertified }
        assertTrue("Should find HNCS presets", hncsPresets.isNotEmpty())
        hncsPresets.forEach { preset ->
            assertTrue("Preset '${preset.name}' should be HNCS certified", preset.isHncsCertified)
        }
    }

    @Test
    fun `filter by trending should match high download presets`() {
        val presets = Preset.createSamplePresets()
        val trendingPresets = presets.filter { it.downloadCount > 10000 }
        assertTrue("Should find trending presets", trendingPresets.isNotEmpty())
    }

    @Test
    fun `filter by new should match version 3 presets`() {
        val presets = Preset.createSamplePresets()
        val newPresets = presets.filter { it.version.contains("3.0") || it.downloadCount < 5000 }
        assertTrue("Should find new presets", newPresets.isNotEmpty())
    }

    // ==================== 搜索逻辑测试 ====================

    @Test
    fun `search by name should find matching presets`() {
        val presets = Preset.createSamplePresets()
        val query = "人像"
        val results = presets.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.deviceModel.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        assertTrue("Search for '人像' should find results", results.isNotEmpty())
    }

    @Test
    fun `search by tag should find matching presets`() {
        val presets = Preset.createSamplePresets()
        val query = "HNCS"
        val results = presets.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.deviceModel.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        assertTrue("Search for 'HNCS' should find results", results.isNotEmpty())
    }

    @Test
    fun `search by device model should find matching presets`() {
        val presets = Preset.createSamplePresets()
        val query = "Find X8"
        val results = presets.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.deviceModel.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        assertTrue("Search for 'Find X8' should find results", results.isNotEmpty())
    }

    @Test
    fun `search with empty query should return all presets`() {
        val presets = Preset.createSamplePresets()
        val query = ""
        val results = presets.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.deviceModel.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        assertEquals("Empty query should return all presets", presets.size, results.size)
    }

    @Test
    fun `search with non-matching query should return empty`() {
        val presets = Preset.createSamplePresets()
        val query = "nonexistent_query_xyz"
        val results = presets.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.deviceModel.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        assertTrue("Non-matching query should return empty", results.isEmpty())
    }

    // ==================== 收藏切换逻辑测试 ====================

    @Test
    fun `toggling favorite should change state`() {
        val preset = Preset(
            id = "test-1", name = "Test", coverPath = "test",
            isFavorite = false
        )
        val toggled = preset.copy(isFavorite = !preset.isFavorite)
        assertTrue("Toggled preset should be favorite", toggled.isFavorite)

        val toggledBack = toggled.copy(isFavorite = !toggled.isFavorite)
        assertFalse("Toggled back should not be favorite", toggledBack.isFavorite)
    }

    @Test
    fun `favorite toggle should not affect other fields`() {
        val preset = Preset(
            id = "test-1", name = "Test", coverPath = "test",
            isFavorite = false,
            downloadCount = 1000,
            rating = 4.5f
        )
        val toggled = preset.copy(isFavorite = !preset.isFavorite)
        assertEquals(preset.id, toggled.id)
        assertEquals(preset.name, toggled.name)
        assertEquals(preset.downloadCount, toggled.downloadCount)
        assertEquals(preset.rating, toggled.rating, 0.01f)
    }

    // ==================== 品牌过滤测试 ====================

    @Test
    fun `OPPO presets should be identifiable`() {
        val presets = Preset.createSamplePresets()
        val oppoPresets = presets.filter {
            it.deviceModel.contains("OPPO", ignoreCase = true)
        }
        assertTrue("Should find OPPO presets", oppoPresets.isNotEmpty())
    }

    @Test
    fun `OnePlus presets should be identifiable`() {
        val presets = Preset.createSamplePresets()
        val onePlusPresets = presets.filter {
            it.deviceModel.contains("OnePlus", ignoreCase = true)
        }
        assertTrue("Should find OnePlus presets", onePlusPresets.isNotEmpty())
    }

    @Test
    fun `realme presets should be identifiable`() {
        val presets = Preset.createSamplePresets()
        val realmePresets = presets.filter {
            it.deviceModel.contains("realme", ignoreCase = true)
        }
        assertTrue("Should find realme presets", realmePresets.isNotEmpty())
    }

    // ==================== 数据完整性测试 ====================

    @Test
    fun `all sample presets should have valid cameraParams validation`() {
        val presets = Preset.createSamplePresets()
        presets.filter { it.cameraParams != null }.forEach { preset ->
            val result = preset.cameraParams!!.validate()
            assertTrue(
                "Preset '${preset.name}' cameraParams should pass validation: ${(result as? ValidationResult.Invalid)?.errors}",
                result is ValidationResult.Valid
            )
        }
    }

    @Test
    fun `all sample presets should have non-empty tags`() {
        val presets = Preset.createSamplePresets()
        presets.forEach { preset ->
            assertTrue(
                "Preset '${preset.name}' should have at least one tag",
                preset.tags.isNotEmpty()
            )
        }
    }

    @Test
    fun `all sample presets should have scene type`() {
        val presets = Preset.createSamplePresets()
        presets.forEach { preset ->
            assertTrue(
                "Preset '${preset.name}' should have scene type",
                preset.sceneType.isNotEmpty()
            )
        }
    }
}
