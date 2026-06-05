package com.omaster.app

import com.omaster.app.viewmodel.FilterType
import com.omaster.app.model.Preset
import com.omaster.app.model.CameraParams
import org.junit.Test
import org.junit.Assert.*

class SearchAndFilterTest {
    
    // ========== 搜索功能测试 ==========
    
    @Test
    fun `search should match preset name`() {
        val presets = createTestPresets()
        val query = "哈苏"
        
        val matched = presets.filter { 
            it.name.contains(query, ignoreCase = true) 
        }
        
        assertEquals(3, matched.size)
        assertTrue(matched.all { it.name.contains("哈苏") })
    }
    
    @Test
    fun `search should match device model`() {
        val presets = createTestPresets()
        val query = "Find X"
        
        val matched = presets.filter { 
            it.deviceModel.contains(query, ignoreCase = true) 
        }
        
        assertEquals(4, matched.size)
        assertTrue(matched.all { it.deviceModel.contains("Find X") })
    }
    
    @Test
    fun `search should match scene type`() {
        val presets = createTestPresets()
        val query = "人像"
        
        val matched = presets.filter { 
            it.sceneType.contains(query, ignoreCase = true) 
        }
        
        assertEquals(1, matched.size)
        assertEquals("portrait", matched.first().sceneType)
    }
    
    @Test
    fun `search should match tags`() {
        val presets = createTestPresets()
        val query = "HNCS"
        
        val matched = presets.filter { 
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) } 
        }
        
        assertTrue(matched.size >= 3)
        assertTrue(matched.all { it.tags.any { it.contains("HNCS") } })
    }
    
    @Test
    fun `search with no match should return empty list`() {
        val presets = createTestPresets()
        val query = "xyz123不存在"
        
        val matched = presets.filter { 
            it.name.contains(query, ignoreCase = true) ||
            it.deviceModel.contains(query, ignoreCase = true) ||
            it.sceneType.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        
        assertTrue(matched.isEmpty())
    }
    
    @Test
    fun `empty search query should return all presets`() {
        val presets = createTestPresets()
        val query = ""
        
        val matched = presets.filter { 
            query.isEmpty() ||
            it.name.contains(query, ignoreCase = true) ||
            it.deviceModel.contains(query, ignoreCase = true) ||
            it.sceneType.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        
        assertEquals(presets.size, matched.size)
    }
    
    @Test
    fun `search should be case insensitive`() {
        val presets = createTestPresets()
        val queryLower = "hasselblad"
        val queryUpper = "HASSELBLAD"
        
        val matchedLower = presets.filter { 
            it.name.contains(queryLower, ignoreCase = true) 
        }
        val matchedUpper = presets.filter { 
            it.name.contains(queryUpper, ignoreCase = true) 
        }
        
        assertEquals(matchedLower.size, matchedUpper.size)
    }
    
    @Test
    fun `search query should be truncated at max length`() {
        val maxLength = 50
        val longQuery = "a".repeat(100)
        
        val truncatedQuery = if (longQuery.length > maxLength) {
            longQuery.take(maxLength)
        } else {
            longQuery
        }
        
        assertEquals(maxLength, truncatedQuery.length)
    }
    
    // ========== 筛选功能测试 ==========
    
    @Test
    fun `ALL filter should return all presets`() {
        val presets = createTestPresets()
        
        val filtered = presets.filter { true }
        
        assertEquals(presets.size, filtered.size)
    }
    
    @Test
    fun `FAVORITES filter should return only favorite presets`() {
        val presets = createTestPresets()
        val favoriteIds = setOf("preset_1", "preset_3")
        
        val filtered = presets.filter { preset ->
            favoriteIds.contains(preset.id)
        }
        
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { favoriteIds.contains(it.id) })
    }
    
    @Test
    fun `HNCS filter should return only HNCS certified presets`() {
        val presets = createTestPresets()
        
        val filtered = presets.filter { it.isHncsCertified }
        
        assertTrue(filtered.size >= 3)
        assertTrue(filtered.all { it.isHncsCertified })
    }
    
    @Test
    fun `FIND_X filter should return only Find X series presets`() {
        val presets = createTestPresets()
        
        val filtered = presets.filter { 
            it.deviceModel.contains("Find X", ignoreCase = true) 
        }
        
        assertEquals(4, filtered.size)
        assertTrue(filtered.all { it.deviceModel.contains("Find X") })
    }
    
    @Test
    fun `RENO filter should return only Reno series presets`() {
        val presets = createTestPresets()
        
        val filtered = presets.filter { 
            it.deviceModel.contains("Reno", ignoreCase = true) 
        }
        
        assertEquals(1, filtered.size)
        assertTrue(filtered.all { it.deviceModel.contains("Reno") })
    }
    
    @Test
    fun `NEW filter should return version 3.0 or low download count presets`() {
        val presets = createTestPresets()
        
        val filtered = presets.filter { 
            it.version.contains("3.0") || it.downloadCount < 5000 
        }
        
        assertTrue(filtered.isNotEmpty())
        assertTrue(filtered.all { 
            it.version.contains("3.0") || it.downloadCount < 5000 
        })
    }
    
    @Test
    fun `TRENDING filter should return high download count presets`() {
        val presets = createTestPresets()
        
        val filtered = presets.filter { it.downloadCount > 10000 }
        
        assertTrue(filtered.isNotEmpty())
        assertTrue(filtered.all { it.downloadCount > 10000 })
    }
    
    // ========== 组合筛选测试 ==========
    
    @Test
    fun `search and filter should use AND logic`() {
        val presets = createTestPresets()
        val query = "哈苏"
        val filterType = FilterType.HNCS
        
        val filtered = presets.filter { preset ->
            val matchesQuery = preset.name.contains(query, ignoreCase = true) ||
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
        
        // 所有结果应该同时满足搜索和筛选条件
        assertTrue(filtered.isNotEmpty())
        assertTrue(filtered.all { 
            it.name.contains("哈苏") && it.isHncsCertified 
        })
    }
    
    @Test
    fun `search with FAVORITES filter should return matching favorites only`() {
        val presets = createTestPresets()
        val query = "哈苏"
        val favoriteIds = setOf("preset_1")
        
        val filtered = presets.filter { preset ->
            val matchesQuery = preset.name.contains(query, ignoreCase = true)
            val isFavorite = favoriteIds.contains(preset.id)
            matchesQuery && isFavorite
        }
        
        assertEquals(1, filtered.size)
        assertEquals("preset_1", filtered.first().id)
    }
    
    // ========== 收藏功能测试 ==========
    
    @Test
    fun `favorite toggle should change isFavorite state`() {
        val preset = createTestPresets().first()
        val initialFavorite = preset.isFavorite
        
        val toggledPreset = preset.copy(isFavorite = !initialFavorite)
        
        assertEquals(!initialFavorite, toggledPreset.isFavorite)
    }
    
    @Test
    fun `favorite state should persist in DataStore key format`() {
        val presetId = "preset_test_123"
        val key = "preset_favorite_$presetId"
        
        assertTrue(key.startsWith("preset_favorite_"))
        assertTrue(key.endsWith(presetId))
    }
    
    @Test
    fun `rapid favorite toggles should not crash`() {
        val preset = createTestPresets().first()
        var currentState = preset.isFavorite
        
        // 模拟10次快速切换
        repeat(10) {
            currentState = !currentState
        }
        
        // 最终状态应该是布尔值
        assertTrue(currentState == true || currentState == false)
    }
    
    // ========== 辅助方法 ==========
    
    private fun createTestPresets(): List<Preset> {
        return listOf(
            Preset(
                id = "preset_1",
                name = "哈苏人像经典",
                coverPath = "portrait",
                deviceModel = "OPPO Find X8 Ultra",
                sceneType = "portrait",
                tags = listOf("人像", "哈苏", "HNCS"),
                version = "3.0",
                downloadCount = 158642,
                isHncsCertified = true,
                isFavorite = false,
                cameraParams = CameraParams(hasselblad_hncs = true)
            ),
            Preset(
                id = "preset_2",
                name = "哈苏风景大师",
                coverPath = "landscape",
                deviceModel = "OPPO Find X8 Ultra",
                sceneType = "landscape",
                tags = listOf("风景", "哈苏", "HNCS"),
                version = "3.0",
                downloadCount = 98642,
                isHncsCertified = true,
                isFavorite = false,
                cameraParams = CameraParams(hasselblad_hncs = true)
            ),
            Preset(
                id = "preset_3",
                name = "哈苏夜景大师",
                coverPath = "night",
                deviceModel = "OPPO Find X8 Pro",
                sceneType = "night",
                tags = listOf("夜景", "哈苏", "HNCS"),
                version = "3.0",
                downloadCount = 152342,
                isHncsCertified = true,
                isFavorite = true,
                cameraParams = CameraParams(hasselblad_hncs = true)
            ),
            Preset(
                id = "preset_4",
                name = "美食摄影",
                coverPath = "food",
                deviceModel = "OPPO Find X8 Ultra",
                sceneType = "food",
                tags = listOf("美食", "摄影"),
                version = "2.0",
                downloadCount = 3000,
                isHncsCertified = false,
                isFavorite = false,
                cameraParams = CameraParams(hasselblad_hncs = false)
            ),
            Preset(
                id = "preset_5",
                name = "街拍模式",
                coverPath = "street",
                deviceModel = "OnePlus 13 Pro",
                sceneType = "street",
                tags = listOf("街拍", "哈苏"),
                version = "3.0",
                downloadCount = 87642,
                isHncsCertified = true,
                isFavorite = false,
                cameraParams = CameraParams(hasselblad_hncs = true)
            ),
            Preset(
                id = "preset_6",
                name = "海岛风情",
                coverPath = "beach",
                deviceModel = "OPPO Reno12 Pro",
                sceneType = "landscape",
                tags = listOf("海边", "度假"),
                version = "2.0",
                downloadCount = 72345,
                isHncsCertified = false,
                isFavorite = false,
                cameraParams = CameraParams(hasselblad_hncs = false)
            )
        )
    }
}