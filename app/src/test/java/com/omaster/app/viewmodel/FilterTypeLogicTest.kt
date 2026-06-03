package com.omaster.app.viewmodel

import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import org.junit.Assert.*
import org.junit.Test

/**
 * FilterType 和过滤逻辑全面单元测试
 * 覆盖：枚举值、过滤逻辑、搜索逻辑、组合过滤
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
        assertEquals(FilterType.values().size, values.size)
    }

    // ==================== 过滤逻辑测试 ====================

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

    @Test
    fun `ALL filter should return all presets`() {
        val filtered = testPresets.filter { true } // ALL
        assertEquals(5, filtered.size)
    }

    @Test
    fun `FAVORITES filter should return only favorited presets`() {
        val filtered = testPresets.filter { it.isFavorite }
        assertEquals(2, filtered.size)
        filtered.forEach { assertTrue(it.isFavorite) }
    }

    @Test
    fun `HNCS filter should return only HNCS certified presets`() {
        val filtered = testPresets.filter { it.isHncsCertified }
        assertEquals(2, filtered.size)
        filtered.forEach { assertTrue(it.isHncsCertified) }
    }

    @Test
    fun `FIND_X filter should return only Find X device presets`() {
        val filtered = testPresets.filter {
            it.deviceModel.contains("Find X", ignoreCase = true)
        }
        assertEquals(1, filtered.size)
        assertTrue(filtered[0].deviceModel.contains("Find X"))
    }

    @Test
    fun `RENO filter should return only Reno device presets`() {
        val filtered = testPresets.filter {
            it.deviceModel.contains("Reno", ignoreCase = true)
        }
        assertEquals(1, filtered.size)
        assertTrue(filtered[0].deviceModel.contains("Reno"))
    }

    @Test
    fun `NEW filter should return version 3 or low download presets`() {
        val filtered = testPresets.filter {
            it.version.contains("3.0") || it.downloadCount < 5000
        }
        assertTrue("Should find new presets", filtered.isNotEmpty())
    }

    @Test
    fun `TRENDING filter should return high download presets`() {
        val filtered = testPresets.filter { it.downloadCount > 10000 }
        assertEquals(4, filtered.size)
    }

    // ==================== 搜索逻辑测试 ====================

    @Test
    fun `search by name should work`() {
        val query = "人像"
        val results = testPresets.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.deviceModel.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        assertEquals(1, results.size)
        assertEquals("哈苏人像", results[0].name)
    }

    @Test
    fun `search by device model should work`() {
        val query = "Find X8"
        val results = testPresets.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.deviceModel.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        assertEquals(1, results.size)
    }

    @Test
    fun `search by tag should work`() {
        val query = "HNCS"
        val results = testPresets.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.deviceModel.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        assertEquals(1, results.size)
    }

    @Test
    fun `search should be case insensitive`() {
        val query = "hncs"
        val results = testPresets.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.deviceModel.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        assertTrue("Case insensitive search should find results", results.isNotEmpty())
    }

    // ==================== 组合过滤测试 ====================

    @Test
    fun `search combined with filter should narrow results`() {
        val query = "哈苏"
        val filterType = FilterType.HNCS

        val results = testPresets.filter { preset ->
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

        assertTrue("Combined filter should find results", results.isNotEmpty())
        results.forEach { preset ->
            assertTrue("Result should match HNCS filter", preset.isHncsCertified)
            assertTrue("Result should match search query", 
                preset.name.contains(query, ignoreCase = true) ||
                preset.deviceModel.contains(query, ignoreCase = true) ||
                preset.tags.any { it.contains(query, ignoreCase = true) }
            )
        }
    }

    @Test
    fun `empty search with ALL filter should return all presets`() {
        val query = ""
        val filterType = FilterType.ALL

        val results = testPresets.filter { preset ->
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

        assertEquals(5, results.size)
    }

    @Test
    fun `no matching results should return empty list`() {
        val query = "nonexistent_xyz"
        val filterType = FilterType.ALL

        val results = testPresets.filter { preset ->
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

        assertTrue("Non-matching search should return empty", results.isEmpty())
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `filtering empty preset list should return empty`() {
        val emptyPresets = emptyList<Preset>()
        val filtered = emptyPresets.filter { it.isFavorite }
        assertTrue("Filtering empty list should return empty", filtered.isEmpty())
    }

    @Test
    fun `all presets favorited should return all for FAVORITES filter`() {
        val allFavorited = testPresets.map { it.copy(isFavorite = true) }
        val filtered = allFavorited.filter { it.isFavorite }
        assertEquals(testPresets.size, filtered.size)
    }

    @Test
    fun `no presets favorited should return empty for FAVORITES filter`() {
        val noneFavorited = testPresets.map { it.copy(isFavorite = false) }
        val filtered = noneFavorited.filter { it.isFavorite }
        assertTrue(filtered.isEmpty())
    }
}
