package com.omaster.app.viewmodel

import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import org.junit.Assert.*
import org.junit.Test

/**
 * FilterType 和过滤逻辑全面单元测试
 * 覆盖：枚举值、过滤逻辑、搜索逻辑、组合过滤、边界条件
 */
class FilterTypeLogicTest {

    // ==================== FilterType 枚举测试 ====================

    @Test
    fun `FilterType should have 7 values`() {
        assertEquals(7, FilterType.values().size)
    }

    @Test
    fun `FilterType ALL should be first`() {
        assertEquals(FilterType.ALL, FilterType.values()[0])
    }

    @Test
    fun `all FilterType values should be distinct`() {
        val values = FilterType.values().toSet()
        assertEquals("All FilterType values should be unique", FilterType.values().size, values.size)
    }

    @Test
    fun `FilterType should contain all expected values`() {
        val values = FilterType.values().toSet()
        assertTrue(values.contains(FilterType.ALL))
        assertTrue(values.contains(FilterType.FAVORITES))
        assertTrue(values.contains(FilterType.HNCS))
        assertTrue(values.contains(FilterType.FIND_X))
        assertTrue(values.contains(FilterType.RENO))
        assertTrue(values.contains(FilterType.NEW))
        assertTrue(values.contains(FilterType.TRENDING))
    }

    // ==================== 测试数据准备 ====================

    private val testPresets = listOf(
        Preset(
            id = "1", name = "哈苏人像", coverPath = "p1",
            deviceModel = "OPPO Find X8 Ultra",
            sceneType = "portrait",
            isFavorite = true,
            isHncsCertified = true,
            cameraParams = CameraParams(hasselblad_hncs = true),
            tags = listOf("人像", "HNCS"),
            version = "3.0",
            downloadCount = 158642
        ),
        Preset(
            id = "2", name = "胶片风景", coverPath = "p2",
            deviceModel = "OPPO Reno12 Pro",
            sceneType = "landscape",
            isFavorite = false,
            isHncsCertified = false,
            cameraParams = CameraParams(hasselblad_hncs = false),
            tags = listOf("风景", "胶片"),
            version = "2.5",
            downloadCount = 45231
        ),
        Preset(
            id = "3", name = "夜景霓虹", coverPath = "p3",
            deviceModel = "OPPO Find N3",
            sceneType = "night",
            isFavorite = true,
            isHncsCertified = true,
            cameraParams = CameraParams(hasselblad_hncs = true),
            tags = listOf("夜景", "城市"),
            version = "3.0",
            downloadCount = 32156
        ),
        Preset(
            id = "4", name = "美食鲜亮", coverPath = "p4",
            deviceModel = "realme GT7 Pro",
            sceneType = "food",
            isFavorite = false,
            isHncsCertified = false,
            cameraParams = CameraParams(hasselblad_hncs = false),
            tags = listOf("美食", "鲜亮"),
            version = "1.8",
            downloadCount = 28547
        ),
        Preset(
            id = "5", name = "街拍纪实", coverPath = "p5",
            deviceModel = "通用",
            sceneType = "street",
            isFavorite = false,
            isHncsCertified = false,
            cameraParams = CameraParams(hasselblad_hncs = false),
            tags = listOf("街拍", "黑白"),
            version = "1.5",
            downloadCount = 1923
        )
    )

    // 复现 ProHomeScreenV2 中的过滤逻辑
    private fun filterPresets(
        presets: List<Preset>,
        query: String,
        filterType: FilterType
    ): List<Preset> {
        return presets.filter { preset ->
            val matchesQuery = query.isEmpty() ||
                preset.name.contains(query, ignoreCase = true) ||
                preset.deviceModel.contains(query, ignoreCase = true) ||
                preset.tags.any { it.contains(query, ignoreCase = true) }

            val matchesFilter = when (filterType) {
                FilterType.ALL -> true
                FilterType.FAVORITES -> preset.isFavorite
                FilterType.HNCS -> preset.isHncsCertified
                FilterType.FIND_X -> preset.deviceModel.contains("Find X", ignoreCase = true)
                FilterType.RENO -> preset.deviceModel.contains("Reno", ignoreCase = true)
                FilterType.NEW -> preset.version.contains("3.0") || preset.downloadCount < 5000
                FilterType.TRENDING -> preset.downloadCount > 10000
            }

            matchesQuery && matchesFilter
        }
    }

    // ==================== 过滤逻辑测试 ====================

    @Test
    fun `ALL filter should return all presets`() {
        val filtered = filterPresets(testPresets, "", FilterType.ALL)
        assertEquals(5, filtered.size)
    }

    @Test
    fun `FAVORITES filter should return only favorited presets`() {
        val filtered = filterPresets(testPresets, "", FilterType.FAVORITES)
        assertEquals(2, filtered.size)
        filtered.forEach { assertTrue(it.isFavorite) }
    }

    @Test
    fun `HNCS filter should return only HNCS certified presets`() {
        val filtered = filterPresets(testPresets, "", FilterType.HNCS)
        assertEquals(2, filtered.size)
        filtered.forEach { assertTrue(it.isHncsCertified) }
    }

    @Test
    fun `FIND_X filter should return only Find X device presets`() {
        val filtered = filterPresets(testPresets, "", FilterType.FIND_X)
        assertEquals(1, filtered.size)
        assertTrue(filtered[0].deviceModel.contains("Find X"))
    }

    @Test
    fun `RENO filter should return only Reno device presets`() {
        val filtered = filterPresets(testPresets, "", FilterType.RENO)
        assertEquals(1, filtered.size)
        assertTrue(filtered[0].deviceModel.contains("Reno"))
    }

    @Test
    fun `NEW filter should return version 3 or low download presets`() {
        val filtered = filterPresets(testPresets, "", FilterType.NEW)
        assertTrue("Should find new presets", filtered.isNotEmpty())
    }

    @Test
    fun `TRENDING filter should return high download presets`() {
        val filtered = filterPresets(testPresets, "", FilterType.TRENDING)
        assertEquals(4, filtered.size)
        filtered.forEach { assertTrue(it.downloadCount > 10000) }
    }

    // ==================== 搜索逻辑测试 ====================

    @Test
    fun `search by name should find matching presets`() {
        val results = filterPresets(testPresets, "人像", FilterType.ALL)
        assertEquals(1, results.size)
        assertEquals("哈苏人像", results[0].name)
    }

    @Test
    fun `search by device model should work`() {
        val results = filterPresets(testPresets, "Find X8", FilterType.ALL)
        assertEquals(1, results.size)
    }

    @Test
    fun `search by tag should work`() {
        val results = filterPresets(testPresets, "HNCS", FilterType.ALL)
        assertTrue("Should find HNCS tag matches", results.isNotEmpty())
    }

    @Test
    fun `search should be case insensitive`() {
        val results = filterPresets(testPresets, "hncs", FilterType.ALL)
        assertTrue("Case insensitive search should find results", results.isNotEmpty())
    }

    @Test
    fun `search with no results should return empty`() {
        val results = filterPresets(testPresets, "nonexistent_xyz_9999", FilterType.ALL)
        assertTrue("Non-matching query should return empty", results.isEmpty())
    }

    // ==================== 组合过滤测试 ====================

    @Test
    fun `search combined with filter should narrow results`() {
        val results = filterPresets(testPresets, "哈苏", FilterType.HNCS)
        assertTrue("Combined filter should find results", results.isNotEmpty())
        results.forEach { preset ->
            assertTrue("Result should match HNCS filter", preset.isHncsCertified)
            assertTrue("Result should match search query",
                preset.name.contains("哈苏", ignoreCase = true) ||
                preset.deviceModel.contains("哈苏", ignoreCase = true) ||
                preset.tags.any { it.contains("哈苏", ignoreCase = true) }
            )
        }
    }

    @Test
    fun `empty search with ALL filter should return all presets`() {
        val results = filterPresets(testPresets, "", FilterType.ALL)
        assertEquals(5, results.size)
    }

    @Test
    fun `search with no match and ALL filter should return empty`() {
        val results = filterPresets(testPresets, "nonexistent_xyz", FilterType.ALL)
        assertTrue("Non-matching search should return empty", results.isEmpty())
    }

    @Test
    fun `search with match but filter excludes should return empty`() {
        val results = filterPresets(testPresets, "人像", FilterType.RENO)
        // "人像" preset is on Find X8 Ultra, not Reno, so should be filtered out
        assertTrue("Reno filter should exclude Find X presets", results.isEmpty())
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `filtering empty preset list should return empty`() {
        val filtered = filterPresets(emptyList(), "test", FilterType.HNCS)
        assertTrue("Filtering empty list should return empty", filtered.isEmpty())
    }

    @Test
    fun `all presets favorited should return all for FAVORITES filter`() {
        val allFavorited = testPresets.map { it.copy(isFavorite = true) }
        val filtered = filterPresets(allFavorited, "", FilterType.FAVORITES)
        assertEquals(testPresets.size, filtered.size)
    }

    @Test
    fun `no presets favorited should return empty for FAVORITES filter`() {
        val noneFavorited = testPresets.map { it.copy(isFavorite = false) }
        val filtered = filterPresets(noneFavorited, "", FilterType.FAVORITES)
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `no HNCS presets should return empty for HNCS filter`() {
        val noHncs = testPresets.map { it.copy(isHncsCertified = false) }
        val filtered = filterPresets(noHncs, "", FilterType.HNCS)
        assertTrue(filtered.isEmpty())
    }
}
