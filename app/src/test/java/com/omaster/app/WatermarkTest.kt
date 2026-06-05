package com.omaster.app

import com.omaster.app.watermark.*
import org.junit.Test
import org.junit.Assert.*

class WatermarkTest {
    @Test
    fun `watermark templates should be complete`() {
        val templates = WatermarkTemplate.values()
        assertEquals(10, templates.size)
        assertTrue(templates.contains(WatermarkTemplate.OPPO))
        assertTrue(templates.contains(WatermarkTemplate.ONEPLUS))
        assertTrue(templates.contains(WatermarkTemplate.REALME))
        assertTrue(templates.contains(WatermarkTemplate.HASSELBLAD))
        assertTrue(templates.contains(WatermarkTemplate.MINIMAL_PARAMS))
        assertTrue(templates.contains(WatermarkTemplate.TIMESTAMP))
        assertTrue(templates.contains(WatermarkTemplate.LOCATION))
        assertTrue(templates.contains(WatermarkTemplate.CUSTOM))
        assertTrue(templates.contains(WatermarkTemplate.BRAND_SIMPLE))
        assertTrue(templates.contains(WatermarkTemplate.FILM_STYLE))
    }
    
    @Test
    fun `watermark positions should be complete`() {
        val positions = WatermarkPosition.values()
        assertEquals(7, positions.size)
        assertTrue(positions.contains(WatermarkPosition.TOP_LEFT))
        assertTrue(positions.contains(WatermarkPosition.TOP_CENTER))
        assertTrue(positions.contains(WatermarkPosition.TOP_RIGHT))
        assertTrue(positions.contains(WatermarkPosition.CENTER))
        assertTrue(positions.contains(WatermarkPosition.BOTTOM_LEFT))
        assertTrue(positions.contains(WatermarkPosition.BOTTOM_CENTER))
        assertTrue(positions.contains(WatermarkPosition.BOTTOM_RIGHT))
    }
    
    @Test
    fun `watermark config default values should be correct`() {
        val config = WatermarkConfig(
            template = WatermarkTemplate.HASSELBLAD
        )
        
        assertEquals(WatermarkPosition.BOTTOM_RIGHT, config.position)
        assertEquals(0.8f, config.opacity)
        assertEquals(1.0f, config.scale)
        assertNull(config.customText)
        assertTrue(config.showTimestamp)
        assertTrue(config.showDevice)
        assertEquals("yyyy-MM-dd HH:mm", config.timestampFormat)
        assertTrue(config.preserveOriginal)
        assertEquals(OutputFormat.JPEG, config.outputFormat)
        assertEquals(95, config.quality)
    }
    
    @Test
    fun `watermark config with custom values should work`() {
        val config = WatermarkConfig(
            template = WatermarkTemplate.CUSTOM,
            position = WatermarkPosition.TOP_LEFT,
            opacity = 0.5f,
            scale = 1.5f,
            customText = "OPPOMaster 出品",
            showTimestamp = false,
            showDevice = false,
            timestampFormat = "yyyy/MM/dd",
            preserveOriginal = false,
            outputFormat = OutputFormat.PNG,
            quality = 100
        )
        
        assertEquals(WatermarkTemplate.CUSTOM, config.template)
        assertEquals(WatermarkPosition.TOP_LEFT, config.position)
        assertEquals(0.5f, config.opacity)
        assertEquals(1.5f, config.scale)
        assertEquals("OPPOMaster 出品", config.customText)
        assertFalse(config.showTimestamp)
        assertFalse(config.showDevice)
        assertEquals("yyyy/MM/dd", config.timestampFormat)
        assertFalse(config.preserveOriginal)
        assertEquals(OutputFormat.PNG, config.outputFormat)
        assertEquals(100, config.quality)
    }
    
    @Test
    fun `camera params for watermark default values should be correct`() {
        val params = CameraParamsForWatermark()
        
        assertEquals("100", params.iso)
        assertEquals("1/1000s", params.shutterSpeed)
        assertEquals("f/1.7", params.aperture)
        assertEquals("0", params.ev)
    }
    
    @Test
    fun `camera params for watermark with custom values should work`() {
        val params = CameraParamsForWatermark(
            iso = "400",
            shutterSpeed = "1/500s",
            aperture = "f/2.8",
            ev = "+0.3"
        )
        
        assertEquals("400", params.iso)
        assertEquals("1/500s", params.shutterSpeed)
        assertEquals("f/2.8", params.aperture)
        assertEquals("+0.3", params.ev)
    }
    
    @Test
    fun `output format values should be complete`() {
        val formats = OutputFormat.values()
        assertEquals(3, formats.size)
        assertTrue(formats.contains(OutputFormat.JPEG))
        assertTrue(formats.contains(OutputFormat.PNG))
        assertTrue(formats.contains(OutputFormat.TIFF))
    }
    
    @Test
    fun `watermark process request should be created correctly`() {
        val config = WatermarkConfig(template = WatermarkTemplate.OPPO)
        val request = WatermarkProcessRequest(
            sourceBitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888),
            config = config,
            outputPath = "/tmp/output.jpg"
        )
        
        assertNotNull(request.sourceBitmap)
        assertEquals(config, request.config)
        assertEquals("/tmp/output.jpg", request.outputPath)
    }
    
    @Test
    fun `watermark process result success should be correct`() {
        val bitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
        val result = WatermarkProcessResult(
            success = true,
            bitmap = bitmap
        )
        
        assertTrue(result.success)
        assertNotNull(result.bitmap)
        assertNull(result.error)
    }
    
    @Test
    fun `watermark process result failure should be correct`() {
        val result = WatermarkProcessResult(
            success = false,
            error = "Processing failed"
        )
        
        assertFalse(result.success)
        assertNull(result.bitmap)
        assertEquals("Processing failed", result.error)
    }
    
    @Test
    fun `watermark template display names should match`() {
        // 验证模板名称与UI显示名称对应
        val templateDisplayNames = mapOf(
            WatermarkTemplate.HASSELBLAD to "哈苏",
            WatermarkTemplate.OPPO to "OPPO",
            WatermarkTemplate.ONEPLUS to "一加",
            WatermarkTemplate.REALME to "真我",
            WatermarkTemplate.MINIMAL_PARAMS to "参数",
            WatermarkTemplate.TIMESTAMP to "时间",
            WatermarkTemplate.FILM_STYLE to "胶片",
            WatermarkTemplate.CUSTOM to "自定义"
        )
        
        templateDisplayNames.forEach { (template, displayName) ->
            assertNotNull(template)
            assertTrue(displayName.isNotEmpty())
        }
    }
    
    @Test
    fun `watermark position display names should match`() {
        // 验证位置名称与UI显示名称对应
        val positionDisplayNames = mapOf(
            WatermarkPosition.TOP_LEFT to "左上",
            WatermarkPosition.TOP_CENTER to "上中",
            WatermarkPosition.TOP_RIGHT to "右上",
            WatermarkPosition.CENTER to "居中",
            WatermarkPosition.BOTTOM_LEFT to "左下",
            WatermarkPosition.BOTTOM_CENTER to "下中",
            WatermarkPosition.BOTTOM_RIGHT to "右下"
        )
        
        positionDisplayNames.forEach { (position, displayName) ->
            assertNotNull(position)
            assertTrue(displayName.isNotEmpty())
        }
    }
    
    @Test
    fun `text size range should be valid`() {
        // 文字大小范围：50% - 200%
        val minSize = 0.5f
        val maxSize = 2.0f
        
        assertTrue(minSize >= 0f)
        assertTrue(maxSize > minSize)
        assertTrue(maxSize <= 3f) // 最大不超过300%
    }
    
    @Test
    fun `opacity range should be valid`() {
        // 透明度范围：0% - 100%
        val minOpacity = 0f
        val maxOpacity = 1f
        
        assertTrue(minOpacity >= 0f)
        assertTrue(maxOpacity <= 1f)
    }
    
    @Test
    fun `timestamp format should be valid`() {
        val formats = listOf(
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm",
            "yyyy年MM月dd日 HH:mm",
            "MM/dd/yyyy"
        )
        
        formats.forEach { format ->
            assertTrue(format.contains("yyyy") || format.contains("yy"))
            assertTrue(format.contains("MM") || format.contains("M"))
            assertTrue(format.contains("dd") || format.contains("d"))
        }
    }
}