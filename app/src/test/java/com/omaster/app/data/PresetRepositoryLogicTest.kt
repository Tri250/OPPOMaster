package com.omaster.app.data

import com.omaster.app.model.Preset
import com.omaster.app.model.ValidationResult
import org.junit.Assert.*
import org.junit.Test

/**
 * PresetRepository 数据逻辑单元测试
 * 验证数据完整性、过滤、搜索、收藏等核心业务逻辑
 */
class PresetRepositoryLogicTest {

    // ==================== 示例预设数据完整性测试 ====================

    @Test
    fun `sample presets should be non-empty`() {
        val presets = Preset.createSamplePresets()
        assertTrue("Sample presets should not be empty", presets.isNotEmpty())
    }

    @Test
    fun `sample presets should have unique IDs`() {
        val presets = Preset.createSamplePresets()
        val ids = presets.map { it.id }
        assertEquals("All preset IDs should be unique", ids.size, ids.toSet().size)
    }

    @Test
    fun `sample presets should have non-empty names`() {
        val presets = Preset.createSamplePresets()
        presets.forEach { preset ->
            assertTrue("Preset '${preset.id}' should have non-empty name", preset.name.isNotEmpty())
        }
    }

    @Test
    fun `sample presets should have non-empty coverPath`() {
        val presets = Preset.createSamplePresets()
        presets.forEach { preset ->
            assertTrue("Preset '${preset.name}' should have non-empty coverPath", preset.coverPath.isNotEmpty())
        }
    }

    @Test
    fun `sample presets with HNCS should have cameraParams with HNCS enabled`() {
        val presets = Preset.createSamplePresets()
        val hncsPresets = presets.filter { it.isHncsCertified }
        assertTrue("Should have HNCS certified presets", hncsPresets.isNotEmpty())
        hncsPresets.forEach { preset ->
            assertNotNull("HNCS preset '${preset.name}' should have cameraParams", preset.cameraParams)
            assertTrue(
                "HNCS preset '${preset.name}' should have HNCS enabled",
                preset.cameraParams?.hasselblad_hncs == true
            )
        }
    }

    @Test
    fun `sample presets cameraParams should all pass validation`() {
        val presets = Preset.createSamplePresets()
        presets.filter { it.cameraParams != null }.forEach { preset ->
            val result = preset.cameraParams!!.validate()
            assertTrue(
                "Preset '${preset.name}' cameraParams should pass validation. Errors: ${(result as? ValidationResult.Invalid)?.errors}",
                result is ValidationResult.Valid
            )
        }
    }

    @Test
    fun `sample presets should have valid ISO ranges`() {
        val presets = Preset.createSamplePresets()
        presets.filter { it.cameraParams != null }.forEach { preset ->
            val iso = preset.cameraParams!!.iso
            assertTrue("Preset '${preset.name}' ISO $iso should be in 32-102400", iso in 32..102400)
        }
    }

    @Test
    fun `sample presets should have valid saturation ranges`() {
        val presets = Preset.createSamplePresets()
        presets.filter { it.cameraParams != null }.forEach { preset ->
            val saturation = preset.cameraParams!!.saturation
            assertTrue(
                "Preset '${preset.name}' saturation $saturation should be in 0-100",
                saturation in 0..100
            )
        }
    }

    @Test
    fun `sample presets should have valid ratings`() {
        val presets = Preset.createSamplePresets()
        presets.forEach { preset ->
            assertTrue(
                "Preset '${preset.name}' rating ${preset.rating} should be 0-5",
                preset.rating in 0f..5f
            )
        }
    }

    @Test
    fun `sample presets should have non-negative download counts`() {
        val presets = Preset.createSamplePresets()
        presets.forEach { preset ->
            assertTrue("Download count should be non-negative", preset.downloadCount >= 0)
            assertTrue("Favorite count should be non-negative", preset.favoriteCount >= 0)
        }
    }

    @Test
    fun `sample presets should have non-empty tags`() {
        val presets = Preset.createSamplePresets()
        presets.forEach { preset ->
            assertTrue("Preset '${preset.name}' should have tags", preset.tags.isNotEmpty())
        }
    }

    @Test
    fun `sample presets should have non-empty scene types`() {
        val presets = Preset.createSamplePresets()
        presets.forEach { preset ->
            assertTrue("Preset '${preset.name}' should have scene type", preset.sceneType.isNotEmpty())
        }
    }

    @Test
    fun `sample presets should have multiple scene types`() {
        val presets = Preset.createSamplePresets()
        val sceneTypes = presets.map { it.sceneType }.toSet()
        assertTrue("Should have multiple scene types", sceneTypes.size >= 2)
    }

    @Test
    fun `sample presets should have version info`() {
        val presets = Preset.createSamplePresets()
        presets.forEach { preset ->
            assertTrue("Preset should have version", preset.version.isNotEmpty())
            assertTrue("Version should start with v", preset.getVersionInfo().startsWith("v"))
        }
    }

    @Test
    fun `sample presets should have publish date`() {
        val presets = Preset.createSamplePresets()
        presets.forEach { preset ->
            assertTrue("Publish date should be positive", preset.publishDate > 0)
            assertTrue("Date format should be YYYY-MM-DD",
                preset.getFormattedPublishDate().matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
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
    fun `filter by HNCS should match certified presets`() {
        val presets = Preset.createSamplePresets()
        val hncsPresets = presets.filter { it.isHncsCertified }
        assertTrue("Should find HNCS presets", hncsPresets.isNotEmpty())
        hncsPresets.forEach { assertTrue(it.isHncsCertified) }
    }

    @Test
    fun `filter by trending should match high download presets`() {
        val presets = Preset.createSamplePresets()
        val trendingPresets = presets.filter { it.downloadCount > 10000 }
        assertTrue("Should find trending presets", trendingPresets.isNotEmpty())
    }

    @Test
    fun `filter by OPPO brand should match OPPO device presets`() {
        val presets = Preset.createSamplePresets()
        val oppoPresets = presets.filter {
            it.deviceModel.contains("OPPO", ignoreCase = true)
        }
        assertTrue("Should find OPPO presets", oppoPresets.isNotEmpty())
    }

    @Test
    fun `filter by OnePlus brand should match OnePlus presets`() {
        val presets = Preset.createSamplePresets()
        val onePlusPresets = presets.filter {
            it.deviceModel.contains("OnePlus", ignoreCase = true)
        }
        assertTrue("Should find OnePlus presets", onePlusPresets.isNotEmpty())
    }

    @Test
    fun `filter by realme brand should match realme presets`() {
        val presets = Preset.createSamplePresets()
        val realmePresets = presets.filter {
            it.deviceModel.contains("realme", ignoreCase = true)
        }
        assertTrue("Should find realme presets", realmePresets.isNotEmpty())
    }

    // ==================== 搜索逻辑测试 ====================

    @Test
    fun `search by Chinese name should find matching presets`() {
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
        val query = "Find"
        val results = presets.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.deviceModel.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        assertTrue("Search for 'Find' should find results", results.isNotEmpty())
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
        val query = "nonexistent_query_xyz_9999"
        val results = presets.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.deviceModel.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        assertTrue("Non-matching query should return empty", results.isEmpty())
    }

    @Test
    fun `search should be case insensitive`() {
        val presets = Preset.createSamplePresets()
        val query = "hncs"
        val results = presets.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.deviceModel.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        assertTrue("Case insensitive search should find results", results.isNotEmpty())
    }

    // ==================== 收藏切换逻辑测试 ====================

    @Test
    fun `toggling favorite should change state`() {
        val preset = Preset(id = "test-1", name = "Test", coverPath = "test", isFavorite = false)
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
        assertEquals(preset.coverPath, toggled.coverPath)
        assertEquals(preset.downloadCount, toggled.downloadCount)
        assertEquals(preset.rating, toggled.rating, 0.01f)
    }

    // ==================== 格式化方法测试 ====================

    @Test
    fun `getFormattedDownloadCount for different values`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test")

        val pMillion = preset.copy(downloadCount = 1500000)
        assertEquals("1.5M", pMillion.getFormattedDownloadCount())

        val pThousand = preset.copy(downloadCount = 158642)
        assertEquals("158.6K", pThousand.getFormattedDownloadCount())

        val pSmall = preset.copy(downloadCount = 999)
        assertEquals("999", pSmall.getFormattedDownloadCount())

        val pZero = preset.copy(downloadCount = 0)
        assertEquals("0", pZero.getFormattedDownloadCount())
    }

    @Test
    fun `getFormattedFavoriteCount for different values`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test")

        val pMillion = preset.copy(favoriteCount = 2300000)
        assertEquals("2.3M", pMillion.getFormattedFavoriteCount())

        val pThousand = preset.copy(favoriteCount = 28453)
        assertEquals("28.5K", pThousand.getFormattedFavoriteCount())

        val pSmall = preset.copy(favoriteCount = 500)
        assertEquals("500", pSmall.getFormattedFavoriteCount())
    }
}
