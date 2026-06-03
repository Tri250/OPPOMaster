package com.omaster.app.watermark

import org.junit.Assert.*
import org.junit.Test

/**
 * 水印模型全面单元测试
 * 覆盖：Watermark、TextWatermarkConfig、ImageWatermarkConfig、WatermarkEditorState、
 *       WatermarkTemplate、ExportConfig、ExportProgress
 */
class WatermarkModelsTest {

    // ==================== WatermarkType 测试 ====================

    @Test
    fun `WatermarkType should have 3 values`() {
        assertEquals(3, WatermarkType.values().size)
        assertTrue(WatermarkType.values().contains(WatermarkType.TEXT))
        assertTrue(WatermarkType.values().contains(WatermarkType.IMAGE))
        assertTrue(WatermarkType.values().contains(WatermarkType.TEMPLATE))
    }

    // ==================== Watermark 测试 ====================

    @Test
    fun `Watermark default creation should have valid id`() {
        val watermark = Watermark(type = WatermarkType.TEXT)
        assertTrue("Watermark ID should not be empty", watermark.id.isNotEmpty())
    }

    @Test
    fun `Watermark text type should have default text config`() {
        val watermark = Watermark(type = WatermarkType.TEXT)
        assertNotNull(watermark.textConfig)
        assertEquals(24f, watermark.textConfig.fontSize, 0.01f)
    }

    @Test
    fun `Watermark image type should have default image config`() {
        val watermark = Watermark(type = WatermarkType.IMAGE)
        assertNotNull(watermark.imageConfig)
        assertTrue(watermark.imageConfig.preserveAspectRatio)
    }

    @Test
    fun `Watermark default position should be center`() {
        val watermark = Watermark(type = WatermarkType.TEXT)
        assertEquals(0.5f, watermark.position.x, 0.01f)
        assertEquals(0.5f, watermark.position.y, 0.01f)
    }

    @Test
    fun `Watermark default opacity should be 1`() {
        val watermark = Watermark(type = WatermarkType.TEXT)
        assertEquals(1f, watermark.opacity, 0.01f)
    }

    @Test
    fun `Watermark default scale should be 1`() {
        val watermark = Watermark(type = WatermarkType.TEXT)
        assertEquals(1f, watermark.scale, 0.01f)
    }

    @Test
    fun `Watermark default rotation should be 0`() {
        val watermark = Watermark(type = WatermarkType.TEXT)
        assertEquals(0f, watermark.rotation, 0.01f)
    }

    @Test
    fun `Watermark default blend mode should be NORMAL`() {
        val watermark = Watermark(type = WatermarkType.TEXT)
        assertEquals(BlendMode.NORMAL, watermark.mixMode)
    }

    @Test
    fun `Watermark should not be selected by default`() {
        val watermark = Watermark(type = WatermarkType.TEXT)
        assertFalse(watermark.isSelected)
    }

    @Test
    fun `Watermark IDs should be unique`() {
        val w1 = Watermark(type = WatermarkType.TEXT)
        val w2 = Watermark(type = WatermarkType.TEXT)
        assertNotEquals("Watermark IDs should be unique", w1.id, w2.id)
    }

    // ==================== TextWatermarkConfig 测试 ====================

    @Test
    fun `TextWatermarkConfig defaults should be correct`() {
        val config = TextWatermarkConfig()
        assertEquals(24f, config.fontSize, 0.01f)
        assertFalse(config.isBold)
        assertFalse(config.isItalic)
        assertFalse(config.isUnderline)
        assertFalse(config.isStrikethrough)
        assertFalse(config.hasStroke)
        assertFalse(config.hasShadow)
        assertEquals(TextAlignment.CENTER, config.alignment)
        assertEquals(1.2f, config.lineSpacing, 0.01f)
    }

    @Test
    fun `TextWatermarkConfig stroke width should be positive`() {
        val config = TextWatermarkConfig(hasStroke = true, strokeWidth = 2f)
        assertTrue(config.strokeWidth > 0)
    }

    @Test
    fun `TextWatermarkConfig shadow blur radius should be positive`() {
        val config = TextWatermarkConfig(hasShadow = true, shadowBlurRadius = 4f)
        assertTrue(config.shadowBlurRadius > 0)
    }

    // ==================== TextAlignment 测试 ====================

    @Test
    fun `TextAlignment should have 3 values`() {
        assertEquals(3, TextAlignment.values().size)
        assertTrue(TextAlignment.values().contains(TextAlignment.LEFT))
        assertTrue(TextAlignment.values().contains(TextAlignment.CENTER))
        assertTrue(TextAlignment.values().contains(TextAlignment.RIGHT))
    }

    // ==================== BlendMode 测试 ====================

    @Test
    fun `BlendMode should have 6 values`() {
        assertEquals(6, BlendMode.values().size)
        assertTrue(BlendMode.values().contains(BlendMode.NORMAL))
        assertTrue(BlendMode.values().contains(BlendMode.MULTIPLY))
        assertTrue(BlendMode.values().contains(BlendMode.SCREEN))
        assertTrue(BlendMode.values().contains(BlendMode.OVERLAY))
        assertTrue(BlendMode.values().contains(BlendMode.DARKEN))
        assertTrue(BlendMode.values().contains(BlendMode.LIGHTEN))
    }

    // ==================== ImageWatermarkConfig 测试 ====================

    @Test
    fun `ImageWatermarkConfig defaults should be correct`() {
        val config = ImageWatermarkConfig()
        assertNull(config.bitmap)
        assertTrue(config.preserveAspectRatio)
        assertNull(config.cropRect)
        assertFalse(config.flipHorizontal)
        assertFalse(config.flipVertical)
    }

    // ==================== WatermarkEditorState 测试 ====================

    @Test
    fun `WatermarkEditorState defaults should be correct`() {
        val state = WatermarkEditorState()
        assertNull(state.imageUri)
        assertTrue(state.watermarks.isEmpty())
        assertNull(state.selectedWatermarkId)
        assertFalse(state.isProcessing)
        assertEquals(-1, state.historyIndex)
        assertEquals(20, state.maxHistorySize)
    }

    @Test
    fun `WatermarkEditorState selectedWatermark should return correct watermark`() {
        val w1 = Watermark(id = "w1", type = WatermarkType.TEXT)
        val w2 = Watermark(id = "w2", type = WatermarkType.IMAGE)
        val state = WatermarkEditorState(
            watermarks = listOf(w1, w2),
            selectedWatermarkId = "w2"
        )
        assertEquals("w2", state.selectedWatermark?.id)
        assertEquals(WatermarkType.IMAGE, state.selectedWatermark?.type)
    }

    @Test
    fun `WatermarkEditorState selectedWatermark should return null when not found`() {
        val state = WatermarkEditorState(
            watermarks = listOf(Watermark(id = "w1", type = WatermarkType.TEXT)),
            selectedWatermarkId = "nonexistent"
        )
        assertNull(state.selectedWatermark)
    }

    @Test
    fun `WatermarkEditorState selectedWatermark should return null when no selection`() {
        val state = WatermarkEditorState(
            watermarks = listOf(Watermark(id = "w1", type = WatermarkType.TEXT))
        )
        assertNull(state.selectedWatermark)
    }

    // ==================== WatermarkTemplate 测试 ====================

    @Test
    fun `WatermarkTemplate should have valid id`() {
        val template = WatermarkTemplate(name = "Test Template", watermarks = emptyList())
        assertTrue(template.id.isNotEmpty())
    }

    @Test
    fun `WatermarkTemplate defaults should be correct`() {
        val template = WatermarkTemplate(name = "Test", watermarks = emptyList())
        assertEquals("", template.description)
        assertNull(template.thumbnail)
        assertFalse(template.isSystem)
        assertFalse(template.isCustom)
    }

    @Test
    fun `WatermarkTemplate IDs should be unique`() {
        val t1 = WatermarkTemplate(name = "Template 1", watermarks = emptyList())
        val t2 = WatermarkTemplate(name = "Template 2", watermarks = emptyList())
        assertNotEquals(t1.id, t2.id)
    }

    // ==================== ExportConfig 测试 ====================

    @Test
    fun `ExportConfig defaults should be correct`() {
        val config = ExportConfig()
        assertEquals(ExportFormat.JPEG, config.format)
        assertEquals(95, config.quality)
        assertEquals(ExportResolution.ORIGINAL, config.resolution)
        assertNull(config.outputUri)
    }

    @Test
    fun `ExportConfig quality should be in valid range`() {
        val config = ExportConfig(quality = 95)
        assertTrue(config.quality in 1..100)
    }

    // ==================== ExportFormat 测试 ====================

    @Test
    fun `ExportFormat should have 3 values`() {
        assertEquals(3, ExportFormat.values().size)
        assertTrue(ExportFormat.values().contains(ExportFormat.JPEG))
        assertTrue(ExportFormat.values().contains(ExportFormat.PNG))
        assertTrue(ExportFormat.values().contains(ExportFormat.WEBP))
    }

    // ==================== ExportResolution 测试 ====================

    @Test
    fun `ExportResolution ORIGINAL should have null dimensions`() {
        assertNull(ExportResolution.ORIGINAL.width)
        assertNull(ExportResolution.ORIGINAL.height)
    }

    @Test
    fun `ExportResolution HD_720 should have correct dimensions`() {
        assertEquals(1280, ExportResolution.HD_720.width)
        assertEquals(720, ExportResolution.HD_720.height)
    }

    @Test
    fun `ExportResolution FHD_1080 should have correct dimensions`() {
        assertEquals(1920, ExportResolution.FHD_1080.width)
        assertEquals(1080, ExportResolution.FHD_1080.height)
    }

    @Test
    fun `ExportResolution QHD_1440 should have correct dimensions`() {
        assertEquals(2560, ExportResolution.QHD_1440.width)
        assertEquals(1440, ExportResolution.QHD_1440.height)
    }

    @Test
    fun `ExportResolution UHD_4K should have correct dimensions`() {
        assertEquals(3840, ExportResolution.UHD_4K.width)
        assertEquals(2160, ExportResolution.UHD_4K.height)
    }

    @Test
    fun `ExportResolution dimensions should increase`() {
        assertTrue(
            ExportResolution.HD_720.width!! < ExportResolution.FHD_1080.width!! &&
            ExportResolution.FHD_1080.width!! < ExportResolution.QHD_1440.width!! &&
            ExportResolution.QHD_1440.width!! < ExportResolution.UHD_4K.width!!
        )
    }

    // ==================== ExportProgress 测试 ====================

    @Test
    fun `ExportProgress default should be zero`() {
        val progress = ExportProgress()
        assertEquals(0, progress.current)
        assertEquals(1, progress.total)
        assertFalse(progress.isProcessing)
        assertFalse(progress.isCompleted)
        assertNull(progress.error)
    }

    @Test
    fun `ExportProgress progress calculation should be correct`() {
        val progress = ExportProgress(current = 5, total = 10)
        assertEquals(0.5f, progress.progress, 0.01f)
    }

    @Test
    fun `ExportProgress zero total should return zero progress`() {
        val progress = ExportProgress(current = 0, total = 0)
        assertEquals(0f, progress.progress, 0.01f)
    }

    @Test
    fun `ExportProgress completed should have full progress`() {
        val progress = ExportProgress(current = 10, total = 10, isCompleted = true)
        assertEquals(1f, progress.progress, 0.01f)
        assertTrue(progress.isCompleted)
    }

    @Test
    fun `ExportProgress with error should have error message`() {
        val progress = ExportProgress(error = "Network error")
        assertNotNull(progress.error)
        assertEquals("Network error", progress.error)
    }

    // ==================== BatchExportRequest 测试 ====================

    @Test
    fun `BatchExportRequest should hold all data`() {
        val template = WatermarkTemplate(name = "Test", watermarks = emptyList())
        val config = ExportConfig(format = ExportFormat.PNG)
        val request = BatchExportRequest(
            sourceUris = emptyList(),
            template = template,
            config = config
        )
        assertTrue(request.sourceUris.isEmpty())
        assertEquals("Test", request.template.name)
        assertEquals(ExportFormat.PNG, request.config.format)
    }
}
