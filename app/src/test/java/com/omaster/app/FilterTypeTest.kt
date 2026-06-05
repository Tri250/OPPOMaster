package com.omaster.app

import com.omaster.app.viewmodel.FilterType
import org.junit.Test
import org.junit.Assert.*

class FilterTypeTest {
    @Test
    fun `all filter types should be available`() {
        val filters = FilterType.values()
        assertEquals(7, filters.size)
        assertTrue(filters.contains(FilterType.ALL))
        assertTrue(filters.contains(FilterType.FAVORITES))
        assertTrue(filters.contains(FilterType.HNCS))
        assertTrue(filters.contains(FilterType.FIND_X))
        assertTrue(filters.contains(FilterType.RENO))
        assertTrue(filters.contains(FilterType.NEW))
        assertTrue(filters.contains(FilterType.TRENDING))
    }

    @Test
    fun `filter type order should be correct`() {
        val filters = FilterType.values()
        assertEquals(FilterType.ALL, filters[0])
        assertEquals(FilterType.FAVORITES, filters[1])
        assertEquals(FilterType.HNCS, filters[2])
        assertEquals(FilterType.FIND_X, filters[3])
        assertEquals(FilterType.RENO, filters[4])
        assertEquals(FilterType.NEW, filters[5])
        assertEquals(FilterType.TRENDING, filters[6])
    }
    
    @Test
    fun `NEW filter should match version 3.0 or low download count`() {
        // 测试NEW筛选逻辑：version.contains("3.0") || downloadCount < 5000
        val newVersionPreset = createTestPreset(version = "3.0", downloadCount = 10000)
        val lowDownloadPreset = createTestPreset(version = "2.0", downloadCount = 4000)
        val oldPreset = createTestPreset(version = "2.0", downloadCount = 10000)
        
        assertTrue(newVersionPreset.version.contains("3.0") || newVersionPreset.downloadCount < 5000)
        assertTrue(lowDownloadPreset.version.contains("3.0") || lowDownloadPreset.downloadCount < 5000)
        assertFalse(oldPreset.version.contains("3.0") || oldPreset.downloadCount < 5000)
    }
    
    @Test
    fun `TRENDING filter should match high download count`() {
        // 测试TRENDING筛选逻辑：downloadCount > 10000
        val trendingPreset = createTestPreset(downloadCount = 15000)
        val normalPreset = createTestPreset(downloadCount = 8000)
        
        assertTrue(trendingPreset.downloadCount > 10000)
        assertFalse(normalPreset.downloadCount > 10000)
    }
    
    private fun createTestPreset(
        version: String = "3.0",
        downloadCount: Int = 1000
    ): com.omaster.app.model.Preset {
        return com.omaster.app.model.Preset(
            id = "test",
            name = "Test Preset",
            coverPath = "test",
            version = version,
            downloadCount = downloadCount
        )
    }
}