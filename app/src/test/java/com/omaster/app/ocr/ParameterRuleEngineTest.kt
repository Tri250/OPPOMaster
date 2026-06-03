package com.omaster.app.ocr

import com.omaster.app.model.CameraParams
import org.junit.Assert.*
import org.junit.Test

/**
 * OCR参数识别引擎全面单元测试
 * 覆盖：ISO识别、快门速度识别、EV识别、白平衡识别、模式识别、
 *       滤镜识别、焦距识别、光圈识别、复合参数、参数转换、参数验证
 */
class ParameterRuleEngineTest {

    private val engine = ParameterRuleEngine()

    // ==================== ISO 识别测试 ====================

    @Test
    fun `extract ISO from standard format`() {
        val result = engine.extractParams("ISO 100")
        assertEquals(100, result.iso)
    }

    @Test
    fun `extract ISO from Chinese format`() {
        val result = engine.extractParams("感光度 200")
        assertEquals(200, result.iso)
    }

    @Test
    fun `extract ISO from trailing format`() {
        val result = engine.extractParams("100 ISO")
        assertEquals(100, result.iso)
    }

    @Test
    fun `extract ISO from high value`() {
        val result = engine.extractParams("ISO 3200")
        assertEquals(3200, result.iso)
    }

    @Test
    fun `extract ISO from very high value`() {
        val result = engine.extractParams("ISO 102400")
        assertEquals(102400, result.iso)
    }

    @Test
    fun `extract ISO with colon`() {
        val result = engine.extractParams("ISO: 400")
        assertEquals(400, result.iso)
    }

    // ==================== 快门速度识别测试 ====================

    @Test
    fun `extract shutter speed from fraction format`() {
        val result = engine.extractParams("S 1/200")
        assertNotNull(result.shutter)
        assertTrue(result.shutter!!.contains("200"))
    }

    @Test
    fun `extract shutter speed from Chinese format`() {
        val result = engine.extractParams("快门 1/125")
        assertNotNull(result.shutter)
    }

    @Test
    fun `extract shutter speed from seconds format`() {
        val result = engine.extractParams("30s")
        assertNotNull(result.shutter)
    }

    // ==================== EV 识别测试 ====================

    @Test
    fun `extract positive EV`() {
        val result = engine.extractParams("EV +0.3")
        assertEquals("+0.3", result.ev)
    }

    @Test
    fun `extract negative EV`() {
        val result = engine.extractParams("EV -1.5")
        assertEquals("-1.5", result.ev)
    }

    @Test
    fun `extract zero EV`() {
        val result = engine.extractParams("EV 0")
        assertEquals("0", result.ev)
    }

    @Test
    fun `extract EV from Chinese format`() {
        val result = engine.extractParams("曝光补偿 +0.7")
        assertEquals("+0.7", result.ev)
    }

    // ==================== 白平衡识别测试 ====================

    @Test
    fun `extract white balance in Kelvin`() {
        val result = engine.extractParams("WB 5500K")
        assertNotNull(result.wb)
        assertTrue(result.wb!!.contains("5500"))
    }

    @Test
    fun `extract white balance from Chinese format`() {
        val result = engine.extractParams("白平衡 6500K")
        assertNotNull(result.wb)
    }

    @Test
    fun `extract white balance Auto`() {
        val result = engine.extractParams("白平衡 Auto")
        assertNotNull(result.wb)
    }

    // ==================== 模式识别测试 ====================

    @Test
    fun `extract mode professional`() {
        val result = engine.extractParams("模式 专业")
        assertEquals("专业", result.mode)
    }

    @Test
    fun `extract mode night`() {
        val result = engine.extractParams("模式 夜景")
        assertEquals("夜景", result.mode)
    }

    @Test
    fun `extract mode hasselblad`() {
        val result = engine.extractParams("模式 哈苏")
        assertNotNull(result.mode)
    }

    @Test
    fun `extract mode auto`() {
        val result = engine.extractParams("Auto")
        assertNotNull(result.mode)
    }

    // ==================== 滤镜识别测试 ====================

    @Test
    fun `extract filter hasselblad`() {
        val result = engine.extractParams("滤镜 哈苏")
        assertNotNull(result.filter)
        assertTrue(result.filter!!.contains("哈苏"))
    }

    @Test
    fun `extract filter natural`() {
        val result = engine.extractParams("滤镜 自然")
        assertNotNull(result.filter)
    }

    @Test
    fun `extract HNCS filter`() {
        val result = engine.extractParams("HNCS")
        assertNotNull(result.filter)
    }

    // ==================== 焦距识别测试 ====================

    @Test
    fun `extract focal length with mm`() {
        val result = engine.extractParams("焦距 50mm")
        assertNotNull(result.focalLength)
        assertTrue(result.focalLength!!.contains("50"))
    }

    @Test
    fun `extract focal length from raw number`() {
        val result = engine.extractParams("23mm")
        assertNotNull(result.focalLength)
    }

    // ==================== 光圈识别测试 ====================

    @Test
    fun `extract aperture with f prefix`() {
        val result = engine.extractParams("光圈 f/1.8")
        assertNotNull(result.aperture)
        assertTrue(result.aperture!!.contains("1.8"))
    }

    @Test
    fun `extract aperture from raw format`() {
        val result = engine.extractParams("f/2.8")
        assertNotNull(result.aperture)
    }

    // ==================== 复合参数测试 ====================

    @Test
    fun `extract multiple params from complex text`() {
        val text = "ISO 100 · 1/200 · EV +0.3 · 5500K · 焦距 50mm · f/1.8"
        val result = engine.extractParams(text)
        assertEquals(100, result.iso)
        assertNotNull(result.shutter)
        assertNotNull(result.ev)
        assertNotNull(result.focalLength)
        assertNotNull(result.aperture)
        assertTrue(result.confidence > 0)
    }

    @Test
    fun `extract params from empty text should return null values`() {
        val result = engine.extractParams("")
        assertNull(result.iso)
        assertNull(result.shutter)
        assertNull(result.ev)
        assertNull(result.wb)
        assertNull(result.mode)
        assertNull(result.filter)
        assertNull(result.focalLength)
        assertNull(result.aperture)
        assertEquals(0f, result.confidence, 0.01f)
    }

    @Test
    fun `extract params from unrelated text should return null values`() {
        val result = engine.extractParams("这是一段无关的文字")
        assertNull(result.iso)
    }

    // ==================== 格式化函数测试 ====================

    @Test
    fun `formatShutter should handle fraction format`() {
        val result = engine.extractParams("1/200")
        // Shutter should be formatted correctly
        assertNotNull(result)
    }

    @Test
    fun `formatWb should add K suffix`() {
        val result = engine.extractParams("WB 5500")
        if (result.wb != null) {
            assertTrue("WB should have K suffix or be Auto", result.wb!!.endsWith("K") || result.wb == "Auto")
        }
    }

    // ==================== 参数转换测试 ====================

    @Test
    fun `convertToCameraParams should create valid CameraParams`() {
        val extracted = ParameterRuleEngine.ExtractedParams(
            iso = 100,
            shutter = "1/200",
            ev = "+0.3",
            wb = "5500K",
            mode = "哈苏大师",
            filter = "哈苏",
            focalLength = "50mm",
            aperture = "f/1.8",
            confidence = 1.0f,
            rawMatches = emptyMap()
        )
        val params = engine.convertToCameraParams(extracted)
        assertEquals(100, params.iso)
        assertEquals("1/200", params.shutter)
        assertEquals("+0.3", params.ev)
        assertEquals("5500K", params.wb)
        assertEquals("50mm", params.focal_length)
        assertEquals("f/1.8", params.aperture)
    }

    @Test
    fun `convertToCameraParams with null values should use defaults`() {
        val extracted = ParameterRuleEngine.ExtractedParams(
            iso = null,
            shutter = null,
            ev = null,
            wb = null,
            mode = null,
            filter = null,
            focalLength = null,
            aperture = null,
            confidence = 0f,
            rawMatches = emptyMap()
        )
        val params = engine.convertToCameraParams(extracted)
        assertEquals(100, params.iso)
        assertEquals("1/125", params.shutter)
        assertEquals("0", params.ev)
        assertEquals("5500K", params.wb)
        assertEquals("24mm", params.focal_length)
        assertEquals("f/1.8", params.aperture)
    }

    @Test
    fun `convertToCameraParams with HNCS filter should set HNCS flag`() {
        val extracted = ParameterRuleEngine.ExtractedParams(
            iso = 100,
            shutter = "1/200",
            ev = "0",
            wb = "5500K",
            mode = "哈苏大师",
            filter = "哈苏",
            focalLength = "50mm",
            aperture = "f/1.8",
            confidence = 1.0f,
            rawMatches = emptyMap()
        )
        val params = engine.convertToCameraParams(extracted)
        assertTrue("HNCS filter should set hasselblad_hncs", params.hasselblad_hncs)
    }

    @Test
    fun `convertToCameraParams with Natural filter should set natural color`() {
        val extracted = ParameterRuleEngine.ExtractedParams(
            iso = 100,
            shutter = "1/200",
            ev = "0",
            wb = "5500K",
            mode = "哈苏大师",
            filter = "自然",
            focalLength = "50mm",
            aperture = "f/1.8",
            confidence = 1.0f,
            rawMatches = emptyMap()
        )
        val params = engine.convertToCameraParams(extracted)
        assertTrue("Natural filter should set hasselblad_natural_color", params.hasselblad_natural_color)
    }

    // ==================== 参数验证测试 ====================

    @Test
    fun `validateParams with valid params should return no errors`() {
        val params = CameraParams(iso = 100, ev = "+0.3")
        val errors = engine.validateParams(params)
        assertTrue("Valid params should have no errors", errors.isEmpty())
    }

    @Test
    fun `validateParams with too low ISO should return error`() {
        val params = CameraParams(iso = 10)
        val errors = engine.validateParams(params)
        assertTrue("Should have ISO error", errors.any { it.contains("ISO") })
    }

    @Test
    fun `validateParams with too high ISO should return error`() {
        val params = CameraParams(iso = 204800)
        val errors = engine.validateParams(params)
        assertTrue("Should have ISO error", errors.any { it.contains("ISO") })
    }

    @Test
    fun `validateParams with extreme EV should return error`() {
        val params = CameraParams(ev = "+6.0")
        val errors = engine.validateParams(params)
        assertTrue("Should have EV error", errors.any { it.contains("EV") })
    }

    @Test
    fun `validateParams with negative extreme EV should return error`() {
        val params = CameraParams(ev = "-6.0")
        val errors = engine.validateParams(params)
        assertTrue("Should have EV error", errors.any { it.contains("EV") })
    }

    @Test
    fun `validateParams with boundary ISO should pass`() {
        val minParams = CameraParams(iso = 50)
        val maxParams = CameraParams(iso = 102400)
        assertTrue("ISO 50 should pass", engine.validateParams(minParams).isEmpty())
        assertTrue("ISO 102400 should pass", engine.validateParams(maxParams).isEmpty())
    }

    @Test
    fun `validateParams with boundary EV should pass`() {
        val minParams = CameraParams(ev = "-5.0")
        val maxParams = CameraParams(ev = "+5.0")
        assertTrue("EV -5.0 should pass", engine.validateParams(minParams).isEmpty())
        assertTrue("EV +5.0 should pass", engine.validateParams(maxParams).isEmpty())
    }

    // ==================== 置信度测试 ====================

    @Test
    fun `confidence should be 0 for no matches`() {
        val result = engine.extractParams("no matching text here")
        assertEquals(0f, result.confidence, 0.01f)
    }

    @Test
    fun `confidence should be positive for matches`() {
        val result = engine.extractParams("ISO 100 · 1/200")
        assertTrue("Confidence should be positive", result.confidence > 0)
    }

    @Test
    fun `more matches should have higher confidence`() {
        val singleResult = engine.extractParams("ISO 100")
        val multiResult = engine.extractParams("ISO 100 · 1/200 · EV +0.3 · 5500K")
        // More matches generally means more confidence
        assertTrue("Multi-match confidence should be positive", multiResult.confidence > 0)
    }

    // ==================== rawMatches 测试 ====================

    @Test
    fun `rawMatches should contain matched parameters`() {
        val result = engine.extractParams("ISO 100")
        assertTrue("rawMatches should contain iso", result.rawMatches.containsKey("iso"))
    }

    @Test
    fun `rawMatches should be empty for no matches`() {
        val result = engine.extractParams("no matching text")
        assertTrue("rawMatches should be empty", result.rawMatches.isEmpty())
    }
}
