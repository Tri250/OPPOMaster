package com.omaster.app.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Preset 模型全面单元测试
 * 覆盖：JSON序列化/反序列化、格式化方法、设备显示名、场景类型、收藏/下载量格式化、HNCS认证
 */
class PresetModelTest {

    // ==================== 基础创建测试 ====================

    @Test
    fun `preset creation with all fields`() {
        val preset = Preset(
            id = "test-1",
            name = "测试预设",
            coverPath = "test_cover",
            coverUrl = "https://example.com/cover.jpg",
            sections = listOf(Section("标题", "内容")),
            cameraParams = CameraParams(iso = 200),
            deviceModel = "OPPO Find X8 Ultra",
            source = "hasselblad_official",
            isFavorite = true,
            author = "哈苏官方",
            description = "测试描述",
            sceneType = "portrait",
            tags = listOf("人像", "HNCS"),
            rating = 4.8f,
            downloadCount = 100000,
            favoriteCount = 20000,
            version = "3.0"
        )

        assertEquals("test-1", preset.id)
        assertEquals("测试预设", preset.name)
        assertEquals("test_cover", preset.coverPath)
        assertEquals("https://example.com/cover.jpg", preset.coverUrl)
        assertEquals(1, preset.sections.size)
        assertEquals(200, preset.cameraParams?.iso)
        assertEquals("OPPO Find X8 Ultra", preset.deviceModel)
        assertEquals("hasselblad_official", preset.source)
        assertTrue(preset.isFavorite)
        assertEquals("哈苏官方", preset.author)
        assertEquals("测试描述", preset.description)
        assertEquals("portrait", preset.sceneType)
        assertEquals(listOf("人像", "HNCS"), preset.tags)
        assertEquals(4.8f, preset.rating, 0.01f)
        assertEquals(100000, preset.downloadCount)
        assertEquals(20000, preset.favoriteCount)
        assertEquals("3.0", preset.version)
    }

    @Test
    fun `preset default values should be correct`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test")
        assertEquals("", preset.coverUrl)
        assertEquals(emptyList<Section>(), preset.sections)
        assertNull(preset.cameraParams)
        assertEquals("", preset.deviceModel)
        assertEquals("omaster_cloud", preset.source)
        assertFalse(preset.isFavorite)
        assertEquals("哈苏影像实验室", preset.author)
        assertEquals("", preset.description)
        assertEquals("", preset.sceneType)
        assertEquals(emptyList<String>(), preset.tags)
        assertEquals(5.0f, preset.rating, 0.01f)
        assertEquals(0, preset.downloadCount)
        assertEquals(0, preset.favoriteCount)
        assertEquals("3.0", preset.version)
    }

    // ==================== getDeviceDisplay 测试 ====================

    @Test
    fun `getDeviceDisplay for empty deviceModel should return generic`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", deviceModel = "")
        assertEquals("通用设备", preset.getDeviceDisplay())
    }

    @Test
    fun `getDeviceDisplay for Find X device should return series name`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", deviceModel = "OPPO Find X8 Ultra")
        assertEquals("OPPO Find X 系列", preset.getDeviceDisplay())
    }

    @Test
    fun `getDeviceDisplay for Find X7 should return series name`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", deviceModel = "Find X7 Ultra")
        assertEquals("OPPO Find X 系列", preset.getDeviceDisplay())
    }

    @Test
    fun `getDeviceDisplay for Reno device should return series name`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", deviceModel = "OPPO Reno12 Pro")
        assertEquals("OPPO Reno 系列", preset.getDeviceDisplay())
    }

    @Test
    fun `getDeviceDisplay for Find N device should return series name`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", deviceModel = "OPPO Find N3")
        assertEquals("OPPO Find N 系列", preset.getDeviceDisplay())
    }

    @Test
    fun `getDeviceDisplay for generic device should return generic`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", deviceModel = "通用")
        assertEquals("通用设备", preset.getDeviceDisplay())
    }

    @Test
    fun `getDeviceDisplay for OnePlus device should return raw name`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", deviceModel = "OnePlus 13 Pro")
        assertEquals("OnePlus 13 Pro", preset.getDeviceDisplay())
    }

    @Test
    fun `getDeviceDisplay for realme device should return raw name`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", deviceModel = "realme GT7 Pro")
        assertEquals("realme GT7 Pro", preset.getDeviceDisplay())
    }

    // ==================== getSceneTypeDisplay 测试 ====================

    @Test
    fun `getSceneTypeDisplay for portrait`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", sceneType = "portrait")
        assertEquals("人像摄影", preset.getSceneTypeDisplay())
    }

    @Test
    fun `getSceneTypeDisplay for landscape`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", sceneType = "landscape")
        assertEquals("风景摄影", preset.getSceneTypeDisplay())
    }

    @Test
    fun `getSceneTypeDisplay for night`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", sceneType = "night")
        assertEquals("夜景摄影", preset.getSceneTypeDisplay())
    }

    @Test
    fun `getSceneTypeDisplay for sunset`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", sceneType = "sunset")
        assertEquals("日落摄影", preset.getSceneTypeDisplay())
    }

    @Test
    fun `getSceneTypeDisplay for food`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", sceneType = "food")
        assertEquals("美食摄影", preset.getSceneTypeDisplay())
    }

    @Test
    fun `getSceneTypeDisplay for street`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", sceneType = "street")
        assertEquals("街拍摄影", preset.getSceneTypeDisplay())
    }

    @Test
    fun `getSceneTypeDisplay for macro`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", sceneType = "macro")
        assertEquals("微距摄影", preset.getSceneTypeDisplay())
    }

    @Test
    fun `getSceneTypeDisplay for still_life`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", sceneType = "still_life")
        assertEquals("静物摄影", preset.getSceneTypeDisplay())
    }

    @Test
    fun `getSceneTypeDisplay for Chinese scene types`() {
        val chineseScenes = mapOf(
            "人像" to "人像摄影",
            "风景" to "风景摄影",
            "夜景" to "夜景摄影",
            "日落" to "日落摄影",
            "美食" to "美食摄影",
            "街拍" to "街拍摄影",
            "微距" to "微距摄影",
            "静物" to "静物摄影"
        )
        chineseScenes.forEach { (input, expected) ->
            val preset = Preset(id = "1", name = "Test", coverPath = "test", sceneType = input)
            assertEquals("Scene '$input' should display as '$expected'", expected, preset.getSceneTypeDisplay())
        }
    }

    @Test
    fun `getSceneTypeDisplay for unknown type should return generic`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", sceneType = "unknown")
        assertEquals("通用摄影", preset.getSceneTypeDisplay())
    }

    @Test
    fun `getSceneTypeDisplay for empty type should return generic`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", sceneType = "")
        assertEquals("通用摄影", preset.getSceneTypeDisplay())
    }

    // ==================== 下载量/收藏量格式化测试 ====================

    @Test
    fun `getFormattedDownloadCount for millions`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", downloadCount = 1500000)
        assertEquals("1.5M", preset.getFormattedDownloadCount())
    }

    @Test
    fun `getFormattedDownloadCount for thousands`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", downloadCount = 158642)
        assertEquals("158.6K", preset.getFormattedDownloadCount())
    }

    @Test
    fun `getFormattedDownloadCount for small numbers`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", downloadCount = 999)
        assertEquals("999", preset.getFormattedDownloadCount())
    }

    @Test
    fun `getFormattedDownloadCount for zero`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", downloadCount = 0)
        assertEquals("0", preset.getFormattedDownloadCount())
    }

    @Test
    fun `getFormattedFavoriteCount for millions`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", favoriteCount = 2300000)
        assertEquals("2.3M", preset.getFormattedFavoriteCount())
    }

    @Test
    fun `getFormattedFavoriteCount for thousands`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", favoriteCount = 28453)
        assertEquals("28.5K", preset.getFormattedFavoriteCount())
    }

    @Test
    fun `getFormattedFavoriteCount for small numbers`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", favoriteCount = 500)
        assertEquals("500", preset.getFormattedFavoriteCount())
    }

    // ==================== HNCS 认证测试 ====================

    @Test
    fun `isHncsCertified should be true when cameraParams has HNCS`() {
        val preset = Preset(
            id = "1", name = "Test", coverPath = "test",
            cameraParams = CameraParams(hasselblad_hncs = true)
        )
        assertTrue(preset.isHncsCertified)
    }

    @Test
    fun `isHncsCertified should be false when cameraParams has no HNCS`() {
        val preset = Preset(
            id = "1", name = "Test", coverPath = "test",
            cameraParams = CameraParams(hasselblad_hncs = false)
        )
        assertFalse(preset.isHncsCertified)
    }

    @Test
    fun `isHncsCertified should be false when no cameraParams`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test")
        assertFalse(preset.isHncsCertified)
    }

    @Test
    fun `hasselbladHncs compatibility alias should match isHncsCertified`() {
        val presetWithHncs = Preset(
            id = "1", name = "Test", coverPath = "test",
            cameraParams = CameraParams(hasselblad_hncs = true)
        )
        assertEquals(presetWithHncs.isHncsCertified, presetWithHncs.hasselbladHncs)
    }

    @Test
    fun `getHncsCertificationText for certified preset`() {
        val preset = Preset(
            id = "1", name = "Test", coverPath = "test",
            cameraParams = CameraParams(hasselblad_hncs = true)
        )
        val text = preset.getHncsCertificationText()
        assertTrue(text.contains("哈苏自然色彩解决方案"))
        assertTrue(text.contains("HNCS"))
    }

    @Test
    fun `getHncsCertificationText for non-certified preset`() {
        val preset = Preset(
            id = "1", name = "Test", coverPath = "test",
            cameraParams = CameraParams(hasselblad_hncs = false)
        )
        assertEquals("", preset.getHncsCertificationText())
    }

    // ==================== isOfficialSource 测试 ====================

    @Test
    fun `isOfficialSource for hasselblad_official`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", source = "hasselblad_official")
        assertTrue(preset.isOfficialSource)
    }

    @Test
    fun `isOfficialSource for author containing 哈苏官方`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", author = "哈苏官方团队")
        assertTrue(preset.isOfficialSource)
    }

    @Test
    fun `isOfficialSource for omaster_cloud`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", source = "omaster_cloud")
        assertFalse(preset.isOfficialSource)
    }

    @Test
    fun `isOfficialSource for official source without hasselblad prefix`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", source = "official")
        assertFalse(preset.isOfficialSource)
    }

    // ==================== canModify 测试 ====================

    @Test
    fun `canModify for non-HNCS preset should be true`() {
        val preset = Preset(
            id = "1", name = "Test", coverPath = "test",
            cameraParams = CameraParams(hasselblad_hncs = false),
            source = "omaster_cloud"
        )
        assertTrue(preset.canModify)
    }

    @Test
    fun `canModify for HNCS non-official preset should be true`() {
        val preset = Preset(
            id = "1", name = "Test", coverPath = "test",
            cameraParams = CameraParams(hasselblad_hncs = true),
            source = "omaster_cloud"
        )
        // isHncsCertified=true, isOfficialSource=false, so canModify = !true || !false = true
        assertTrue(preset.canModify)
    }

    @Test
    fun `canModify for HNCS official preset should be false`() {
        val preset = Preset(
            id = "1", name = "Test", coverPath = "test",
            cameraParams = CameraParams(hasselblad_hncs = true),
            source = "hasselblad_official"
        )
        // isHncsCertified=true, isOfficialSource=true, so canModify = !true || !true = false
        assertFalse(preset.canModify)
    }

    // ==================== 版本信息测试 ====================

    @Test
    fun `getVersionInfo should return version with v prefix`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test", version = "3.0")
        assertEquals("v3.0", preset.getVersionInfo())
    }

    @Test
    fun `getVersionInfo for different versions`() {
        val versions = listOf("1.0", "2.5", "3.0", "4.0-beta")
        versions.forEach { version ->
            val preset = Preset(id = "1", name = "Test", coverPath = "test", version = version)
            assertEquals("v$version", preset.getVersionInfo())
        }
    }

    // ==================== 发布日期格式化测试 ====================

    @Test
    fun `getFormattedPublishDate should return formatted date`() {
        val timestamp = 1704067200000L // 2024-01-01 00:00:00 UTC
        val preset = Preset(id = "1", name = "Test", coverPath = "test", publishDate = timestamp)
        val dateStr = preset.getFormattedPublishDate()
        assertNotNull(dateStr)
        assertTrue(dateStr.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    // ==================== JSON 序列化/反序列化测试 ====================

    @Test
    fun `toJson and fromJson should preserve basic fields`() {
        val original = Preset(
            id = "test-1",
            name = "哈苏人像大师",
            coverPath = "hncs_portrait",
            coverUrl = "https://example.com/cover.jpg",
            deviceModel = "OPPO Find X8 Ultra",
            source = "hasselblad_official",
            author = "哈苏官方",
            description = "人像摄影预设",
            sceneType = "portrait",
            tags = listOf("人像", "HNCS"),
            rating = 4.8f,
            downloadCount = 158642,
            favoriteCount = 28453,
            version = "3.0",
            isHncsCertified = true
        )

        val json = original.toJson()
        val restored = Preset.fromJson(json)

        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.coverUrl, restored.coverUrl)
        assertEquals(original.deviceModel, restored.deviceModel)
        assertEquals(original.source, restored.source)
        assertEquals(original.author, restored.author)
        assertEquals(original.description, restored.description)
        assertEquals(original.sceneType, restored.sceneType)
        assertEquals(original.tags, restored.tags)
        assertEquals(original.rating, restored.rating, 0.01f)
        assertEquals(original.downloadCount, restored.downloadCount)
        assertEquals(original.favoriteCount, restored.favoriteCount)
        assertEquals(original.version, restored.version)
    }

    @Test
    fun `toJson and fromJson with cameraParams should preserve params`() {
        val original = Preset(
            id = "test-1",
            name = "Test",
            coverPath = "test",
            cameraParams = CameraParams(
                iso = 200,
                shutter = "1/250",
                ev = "+0.3",
                wb = "5600K",
                hasselblad_hncs = true
            )
        )

        val json = original.toJson()
        val restored = Preset.fromJson(json)

        assertNotNull(restored.cameraParams)
        assertEquals(200, restored.cameraParams?.iso)
        assertEquals("1/250", restored.cameraParams?.shutter)
        assertEquals("+0.3", restored.cameraParams?.ev)
        assertEquals("5600K", restored.cameraParams?.wb)
        assertTrue(restored.cameraParams?.hasselblad_hncs == true)
    }

    @Test
    fun `fromJson with null values should use defaults`() {
        val json = emptyMap<String, Any?>()
        val preset = Preset.fromJson(json)

        assertEquals("", preset.id)
        assertEquals("", preset.name)
        assertEquals("", preset.coverPath)
        assertEquals("", preset.coverUrl)
        assertNull(preset.cameraParams)
        assertEquals("", preset.deviceModel)
        assertEquals("omaster_cloud", preset.source)
        assertEquals("哈苏影像实验室", preset.author)
        assertEquals("", preset.description)
        assertEquals("", preset.sceneType)
        assertEquals(emptyList<String>(), preset.tags)
        assertEquals(5.0f, preset.rating, 0.01f)
        assertEquals(0, preset.downloadCount)
        assertEquals(0, preset.favoriteCount)
        assertEquals("3.0", preset.version)
    }

    // ==================== SampleImage 测试 ====================

    @Test
    fun `sampleImages should be preserved in preset`() {
        val images = listOf(
            SampleImage("img1", "path1", "Title 1", "Desc 1", false, true),
            SampleImage("img2", "path2", "Title 2", "Desc 2", true, false)
        )
        val preset = Preset(
            id = "1", name = "Test", coverPath = "test",
            sampleImages = images
        )
        assertEquals(2, preset.sampleImages.size)
        assertEquals("img1", preset.sampleImages[0].id)
        assertFalse(preset.sampleImages[0].isBeforeImage)
        assertTrue(preset.sampleImages[0].isAfterImage)
        assertTrue(preset.sampleImages[1].isBeforeImage)
        assertFalse(preset.sampleImages[1].isAfterImage)
    }

    // ==================== createSamplePresets 测试 ====================

    @Test
    fun `createSamplePresets should return non-empty list`() {
        val presets = Preset.createSamplePresets()
        assertTrue("Sample presets should not be empty", presets.isNotEmpty())
    }

    @Test
    fun `createSamplePresets should have unique IDs`() {
        val presets = Preset.createSamplePresets()
        val ids = presets.map { it.id }.toSet()
        assertEquals("All preset IDs should be unique", presets.size, ids.size)
    }

    @Test
    fun `createSamplePresets should have valid names`() {
        val presets = Preset.createSamplePresets()
        presets.forEach { preset ->
            assertTrue("Preset name should not be empty", preset.name.isNotEmpty())
        }
    }

    @Test
    fun `createSamplePresets should have different scene types`() {
        val presets = Preset.createSamplePresets()
        val sceneTypes = presets.map { it.sceneType }.toSet()
        assertTrue("Should have multiple scene types", sceneTypes.size > 1)
    }

    // ==================== getMarketingParams 测试 ====================

    @Test
    fun `getMarketingParams with cameraParams should return formatted params`() {
        val preset = Preset(
            id = "1", name = "Test", coverPath = "test",
            cameraParams = CameraParams(iso = 100, shutter = "1/200")
        )
        val params = preset.getMarketingParams()
        assertTrue("Should contain ISO", params.containsKey("ISO"))
        assertTrue("Should contain 快门速度", params.containsKey("快门速度"))
    }

    @Test
    fun `getMarketingParams without cameraParams should return empty map`() {
        val preset = Preset(id = "1", name = "Test", coverPath = "test")
        val params = preset.getMarketingParams()
        assertTrue("Should return empty map", params.isEmpty())
    }

    // ==================== copy 测试 ====================

    @Test
    fun `preset copy should work correctly`() {
        val original = Preset(
            id = "1", name = "Test", coverPath = "test",
            isFavorite = false
        )
        val modified = original.copy(isFavorite = true)
        assertFalse(original.isFavorite)
        assertTrue(modified.isFavorite)
        assertEquals(original.id, modified.id)
        assertEquals(original.name, modified.name)
    }
}
