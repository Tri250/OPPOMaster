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
    
    @Test
    fun `preset isHncsCertified should match cameraParams hasselblad_hncs`() {
        val presetWithHncs = Preset(
            id = "1",
            name = "HNCS Preset",
            coverPath = "test",
            cameraParams = CameraParams(hasselblad_hncs = true)
        )
        
        val presetWithoutHncs = Preset(
            id = "2",
            name = "Non-HNCS Preset",
            coverPath = "test",
            cameraParams = CameraParams(hasselblad_hncs = false)
        )
        
        assertTrue(presetWithHncs.isHncsCertified)
        assertFalse(presetWithoutHncs.isHncsCertified)
    }
    
    @Test
    fun `preset formatted download count should be correct`() {
        val millionDownload = Preset(
            id = "1",
            name = "Popular",
            coverPath = "test",
            downloadCount = 1500000
        )
        
        val thousandDownload = Preset(
            id = "2",
            name = "Medium",
            coverPath = "test",
            downloadCount = 15000
        )
        
        val smallDownload = Preset(
            id = "3",
            name = "Small",
            coverPath = "test",
            downloadCount = 500
        )
        
        assertEquals("1.5M", millionDownload.getFormattedDownloadCount())
        assertEquals("15.0K", thousandDownload.getFormattedDownloadCount())
        assertEquals("500", smallDownload.getFormattedDownloadCount())
    }
    
    @Test
    fun `preset device display should be correct`() {
        val findXPreset = Preset(
            id = "1",
            name = "Find X",
            coverPath = "test",
            deviceModel = "OPPO Find X8 Ultra"
        )
        
        val renoPreset = Preset(
            id = "2",
            name = "Reno",
            coverPath = "test",
            deviceModel = "OPPO Reno12 Pro"
        )
        
        val genericPreset = Preset(
            id = "3",
            name = "Generic",
            coverPath = "test",
            deviceModel = ""
        )
        
        assertEquals("OPPO Find X8 Ultra", findXPreset.deviceModel)
        assertEquals("OPPO Reno12 Pro", renoPreset.deviceModel)
        assertEquals("", genericPreset.deviceModel)
    }
    
    @Test
    fun `preset scene type display should be correct`() {
        val portraitPreset = Preset(
            id = "1",
            name = "Portrait",
            coverPath = "test",
            sceneType = "portrait"
        )
        
        val landscapePreset = Preset(
            id = "2",
            name = "Landscape",
            coverPath = "test",
            sceneType = "landscape"
        )
        
        val nightPreset = Preset(
            id = "3",
            name = "Night",
            coverPath = "test",
            sceneType = "night"
        )
        
        assertEquals("人像摄影", portraitPreset.getSceneTypeDisplay())
        assertEquals("风景摄影", landscapePreset.getSceneTypeDisplay())
        assertEquals("夜景摄影", nightPreset.getSceneTypeDisplay())
    }
    
    @Test
    fun `preset toJson and fromJson should work correctly`() {
        val originalPreset = Preset(
            id = "test_json",
            name = "JSON Test",
            coverPath = "test_cover",
            coverUrl = "https://example.com/cover.jpg",
            deviceModel = "OPPO Find X8 Ultra",
            author = "哈苏影像实验室",
            description = "测试预设",
            sceneType = "portrait",
            tags = listOf("人像", "HNCS"),
            rating = 4.8f,
            downloadCount = 10000,
            version = "3.0",
            isHncsCertified = true,
            cameraParams = CameraParams(
                mode = "哈苏大师",
                hasselblad_hncs = true
            )
        )
        
        val json = originalPreset.toJson()
        val restoredPreset = Preset.fromJson(json)
        
        assertEquals(originalPreset.id, restoredPreset.id)
        assertEquals(originalPreset.name, restoredPreset.name)
        assertEquals(originalPreset.coverUrl, restoredPreset.coverUrl)
        assertEquals(originalPreset.deviceModel, restoredPreset.deviceModel)
        assertEquals(originalPreset.author, restoredPreset.author)
        assertEquals(originalPreset.rating, restoredPreset.rating)
        assertEquals(originalPreset.downloadCount, restoredPreset.downloadCount)
        assertEquals(originalPreset.isHncsCertified, restoredPreset.isHncsCertified)
    }
    
    @Test
    fun `preset sample presets should be valid`() {
        val samplePresets = Preset.createSamplePresets()
        
        assertTrue(samplePresets.isNotEmpty())
        assertTrue(samplePresets.any { it.isHncsCertified })
        assertTrue(samplePresets.any { it.deviceModel.contains("OPPO") })
        
        for (preset in samplePresets) {
            assertTrue(preset.id.isNotEmpty())
            assertTrue(preset.name.isNotEmpty())
            assertTrue(preset.coverPath.isNotEmpty())
        }
    }
}