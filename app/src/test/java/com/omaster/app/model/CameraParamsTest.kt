package com.omaster.app.model

import org.junit.Assert.*
import org.junit.Test

/**
 * CameraParams 模型全面单元测试
 * 覆盖：验证逻辑、JSON序列化/反序列化、参数格式化、边界值、枚举
 */
class CameraParamsTest {

    // ==================== 验证逻辑测试 ====================

    @Test
    fun `valid CameraParams should pass validation`() {
        val params = CameraParams(
            iso = 100,
            shutter = "1/200",
            ev = "+0.3",
            wb = "5500K",
            colorTemperature = 5500,
            saturation = 50
        )
        val result = params.validate()
        assertTrue("Valid params should pass validation", result is ValidationResult.Valid)
    }

    @Test
    fun `ISO below minimum should fail validation`() {
        val params = CameraParams(iso = 16, colorTemperature = 5500, saturation = 50)
        val result = params.validate()
        assertTrue("ISO 16 should fail", result is ValidationResult.Invalid)
        assertTrue(
            "Error should mention ISO",
            (result as ValidationResult.Invalid).errors.any { it.contains("ISO") }
        )
    }

    @Test
    fun `ISO above maximum should fail validation`() {
        val params = CameraParams(iso = 204800, colorTemperature = 5500, saturation = 50)
        val result = params.validate()
        assertTrue("ISO 204800 should fail", result is ValidationResult.Invalid)
    }

    @Test
    fun `ISO at minimum boundary should pass`() {
        val params = CameraParams(iso = 32, colorTemperature = 5500, saturation = 50)
        val result = params.validate()
        assertTrue("ISO 32 (min) should pass", result is ValidationResult.Valid)
    }

    @Test
    fun `ISO at maximum boundary should pass`() {
        val params = CameraParams(iso = 102400, colorTemperature = 5500, saturation = 50)
        val result = params.validate()
        assertTrue("ISO 102400 (max) should pass", result is ValidationResult.Valid)
    }

    @Test
    fun `invalid shutter speed format should fail validation`() {
        val params = CameraParams(shutter = "abc", colorTemperature = 5500, saturation = 50)
        val result = params.validate()
        assertTrue("Invalid shutter format should fail", result is ValidationResult.Invalid)
    }

    @Test
    fun `valid fraction shutter speed should pass`() {
        val params = CameraParams(shutter = "1/500", colorTemperature = 5500, saturation = 50)
        val result = params.validate()
        assertTrue("1/500 format should pass", result is ValidationResult.Valid)
    }

    @Test
    fun `valid seconds shutter speed should pass`() {
        val params = CameraParams(shutter = "1.5s", colorTemperature = 5500, saturation = 50)
        val result = params.validate()
        assertTrue("1.5s format should pass", result is ValidationResult.Valid)
    }

    @Test
    fun `valid plain number shutter speed should pass`() {
        val params = CameraParams(shutter = "30", colorTemperature = 5500, saturation = 50)
        val result = params.validate()
        assertTrue("Plain number 30 should pass", result is ValidationResult.Valid)
    }

    @Test
    fun `invalid EV format should fail validation`() {
        val params = CameraParams(ev = "auto", colorTemperature = 5500, saturation = 50)
        val result = params.validate()
        assertTrue("EV 'auto' should fail", result is ValidationResult.Invalid)
    }

    @Test
    fun `valid positive EV should pass`() {
        val params = CameraParams(ev = "+1.5", colorTemperature = 5500, saturation = 50)
        val result = params.validate()
        assertTrue("EV +1.5 should pass", result is ValidationResult.Valid)
    }

    @Test
    fun `valid negative EV should pass`() {
        val params = CameraParams(ev = "-2.0", colorTemperature = 5500, saturation = 50)
        val result = params.validate()
        assertTrue("EV -2.0 should pass", result is ValidationResult.Valid)
    }

    @Test
    fun `valid zero EV should pass`() {
        val params = CameraParams(ev = "0", colorTemperature = 5500, saturation = 50)
        val result = params.validate()
        assertTrue("EV 0 should pass", result is ValidationResult.Valid)
    }

    @Test
    fun `color temperature below range should fail`() {
        val params = CameraParams(colorTemperature = 2000, saturation = 50)
        val result = params.validate()
        assertTrue("Color temp 2000K should fail", result is ValidationResult.Invalid)
    }

    @Test
    fun `color temperature above range should fail`() {
        val params = CameraParams(colorTemperature = 11000, saturation = 50)
        val result = params.validate()
        assertTrue("Color temp 11000K should fail", result is ValidationResult.Invalid)
    }

    @Test
    fun `color temperature at boundaries should pass`() {
        val minParams = CameraParams(colorTemperature = 2500, saturation = 50)
        val maxParams = CameraParams(colorTemperature = 10000, saturation = 50)
        assertTrue("2500K should pass", minParams.validate() is ValidationResult.Valid)
        assertTrue("10000K should pass", maxParams.validate() is ValidationResult.Valid)
    }

    @Test
    fun `saturation below range should fail`() {
        val params = CameraParams(saturation = -1, colorTemperature = 5500)
        val result = params.validate()
        assertTrue("Saturation -1 should fail", result is ValidationResult.Invalid)
    }

    @Test
    fun `saturation above range should fail`() {
        val params = CameraParams(saturation = 101, colorTemperature = 5500)
        val result = params.validate()
        assertTrue("Saturation 101 should fail", result is ValidationResult.Invalid)
    }

    @Test
    fun `saturation at boundaries should pass`() {
        val minParams = CameraParams(saturation = 0, colorTemperature = 5500)
        val maxParams = CameraParams(saturation = 100, colorTemperature = 5500)
        assertTrue("Saturation 0 should pass", minParams.validate() is ValidationResult.Valid)
        assertTrue("Saturation 100 should pass", maxParams.validate() is ValidationResult.Valid)
    }

    @Test
    fun `multiple validation errors should all be reported`() {
        val params = CameraParams(
            iso = 10,
            shutter = "invalid",
            ev = "auto",
            colorTemperature = 2000,
            saturation = -5
        )
        val result = params.validate()
        assertTrue("Multiple errors should be reported", result is ValidationResult.Invalid)
        assertEquals(5, (result as ValidationResult.Invalid).errors.size)
    }

    // ==================== 参数格式化测试 ====================

    @Test
    fun `formatParamsForDisplay should format basic params`() {
        val params = CameraParams(
            iso = 100,
            shutter = "1/200",
            ev = "+0.3",
            wb = "5500K",
            hasselblad_hncs = true
        )
        val display = params.formatParamsForDisplay()
        assertTrue("Should contain ISO", display.contains("ISO 100"))
        assertTrue("Should contain shutter", display.contains("1/200"))
        assertTrue("Should contain EV", display.contains("EV +0.3"))
        assertTrue("Should contain WB", display.contains("5500K"))
        assertTrue("Should contain HNCS", display.contains("HNCS"))
    }

    @Test
    fun `formatParamsForDisplay should skip zero EV`() {
        val params = CameraParams(ev = "+0.0")
        val display = params.formatParamsForDisplay()
        assertFalse("Should not show +0.0 EV", display.contains("EV +0.0"))
    }

    @Test
    fun `formatParamsForDisplay should skip zero EV variant`() {
        val params = CameraParams(ev = "0")
        val display = params.formatParamsForDisplay()
        assertFalse("Should not show 0 EV", display.contains("EV 0"))
    }

    @Test
    fun `formatParamsForDisplay without HNCS should not show HNCS`() {
        val params = CameraParams(hasselblad_hncs = false)
        val display = params.formatParamsForDisplay()
        assertFalse("Should not show HNCS", display.contains("HNCS"))
    }

    @Test
    fun `formatFullParams should return complete parameter map`() {
        val params = CameraParams(
            mode = "哈苏大师",
            iso = 200,
            shutter = "1/500",
            ev = "+0.7",
            wb = "6500K",
            focalLength = "23mm",
            aperture = "f/2.8",
            hdr = true,
            nightMode = false,
            portraitMode = true,
            aiOptimization = true,
            hasselblad_hncs = true,
            hasselbladMasterStyle = "Landscape",
            hasselbladColorScience = "HNCS 3.0",
            colorStyle = "Vivid",
            sharpness = 55,
            contrast = 55,
            saturation = 55
        )
        val fullParams = params.formatFullParams()

        assertEquals("哈苏大师", fullParams["模式"])
        assertEquals("200", fullParams["ISO"])
        assertEquals("1/500", fullParams["快门速度"])
        assertEquals("+0.7", fullParams["曝光补偿"])
        assertEquals("6500K", fullParams["白平衡"])
        assertEquals("23mm", fullParams["焦距"])
        assertEquals("f/2.8", fullParams["光圈"])
        assertEquals("Vivid", fullParams["色彩风格"])
        assertEquals("开启", fullParams["HDR"])
        assertEquals("关闭", fullParams["夜景模式"])
        assertEquals("开启", fullParams["人像模式"])
        assertEquals("开启", fullParams["AI优化"])
        assertEquals("认证", fullParams["哈苏HNCS"])
        assertEquals("Landscape", fullParams["哈苏风格"])
        assertEquals("HNCS 3.0", fullParams["色彩科学"])
        assertEquals("55%", fullParams["清晰度"])
        assertEquals("55%", fullParams["对比度"])
        assertEquals("55%", fullParams["饱和度"])
    }

    // ==================== JSON 序列化/反序列化测试 ====================

    @Test
    fun `toJsonMap and fromJsonMap should be inverse operations`() {
        val original = CameraParams(
            mode = "哈苏大师",
            filter = "自然色彩",
            iso = 100,
            shutter = "1/200",
            ev = "+0.3",
            wb = "5500K",
            focalLength = "50mm",
            aperture = "f/1.6",
            hdr = true,
            nightMode = false,
            portraitMode = true,
            aiOptimization = true,
            hasselblad_hncs = true,
            hasselbladNaturalColor = true,
            hasselbladMasterStyle = "Portrait Pro",
            hasselbladColorScience = "HNCS 3.0",
            colorProfile = "自然",
            colorStyle = "Portrait",
            colorTemperature = 5500,
            sharpness = 45,
            contrast = 50,
            saturation = 50,
            version = "3.0"
        )

        val jsonMap = original.toJsonMap()
        val restored = CameraParams.fromJsonMap(jsonMap)

        assertEquals(original.mode, restored.mode)
        assertEquals(original.filter, restored.filter)
        assertEquals(original.iso, restored.iso)
        assertEquals(original.shutter, restored.shutter)
        assertEquals(original.ev, restored.ev)
        assertEquals(original.wb, restored.wb)
        assertEquals(original.focalLength, restored.focalLength)
        assertEquals(original.aperture, restored.aperture)
        assertEquals(original.hdr, restored.hdr)
        assertEquals(original.nightMode, restored.nightMode)
        assertEquals(original.portraitMode, restored.portraitMode)
        assertEquals(original.aiOptimization, restored.aiOptimization)
        assertEquals(original.hasselblad_hncs, restored.hasselblad_hncs)
        assertEquals(original.hasselbladNaturalColor, restored.hasselbladNaturalColor)
        assertEquals(original.hasselbladMasterStyle, restored.hasselbladMasterStyle)
        assertEquals(original.hasselbladColorScience, restored.hasselbladColorScience)
        assertEquals(original.colorProfile, restored.colorProfile)
        assertEquals(original.colorStyle, restored.colorStyle)
        assertEquals(original.colorTemperature, restored.colorTemperature)
        assertEquals(original.sharpness, restored.sharpness)
        assertEquals(original.contrast, restored.contrast)
        assertEquals(original.saturation, restored.saturation)
        assertEquals(original.version, restored.version)
    }

    @Test
    fun `fromJsonMap with null values should use defaults`() {
        val jsonMap = emptyMap<String, Any?>()
        val params = CameraParams.fromJsonMap(jsonMap)

        assertEquals(CameraMode.HasselbladMaster.displayName, params.mode)
        assertEquals("", params.filter)
        assertEquals(100, params.iso)
        assertEquals("1/200", params.shutter)
        assertEquals("+0.0", params.ev)
        assertEquals("5500K", params.wb)
        assertEquals("24mm", params.focalLength)
        assertEquals("f/1.8", params.aperture)
        assertFalse(params.hdr)
        assertFalse(params.nightMode)
        assertFalse(params.portraitMode)
        assertTrue(params.aiOptimization)
        assertTrue(params.hasselblad_hncs)
        assertTrue(params.hasselbladNaturalColor)
        assertEquals("", params.hasselbladMasterStyle)
        assertEquals("HNCS 3.0", params.hasselbladColorScience)
        assertEquals(ColorStyle.Natural.displayName, params.colorProfile)
        assertEquals(ColorStyle.Natural.name, params.colorStyle)
        assertEquals(5500, params.colorTemperature)
        assertEquals(50, params.sharpness)
        assertEquals(50, params.contrast)
        assertEquals(50, params.saturation)
        assertEquals("3.0", params.version)
    }

    @Test
    fun `fromJsonMap should handle snake_case aliases`() {
        val jsonMap = mapOf<String, Any?>(
            "focal_length" to "35mm",
            "hasselblad_natural_color" to false,
            "hasselblad_master_style" to "Street",
            "color_profile" to "鲜明",
            "ai_optimization" to false,
            "ai_scene_recognition" to "portrait",
            "focus_distance" to "2.5m"
        )
        val params = CameraParams.fromJsonMap(jsonMap)

        assertEquals("35mm", params.focalLength)
        assertFalse(params.hasselbladNaturalColor)
        assertEquals("Street", params.hasselbladMasterStyle)
        assertEquals("鲜明", params.colorProfile)
        assertFalse(params.ai_optimization)
        assertEquals("portrait", params.ai_scene_recognition)
        assertEquals("2.5m", params.focus_distance)
    }

    // ==================== 枚举测试 ====================

    @Test
    fun `CameraMode should have all expected values`() {
        val modes = CameraMode.entries
        assertEquals(8, modes.size)
        assertEquals("哈苏大师", CameraMode.HasselbladMaster.displayName)
        assertEquals("哈苏人像", CameraMode.HasselbladPortrait.displayName)
        assertEquals("哈苏风景", CameraMode.HasselbladLandscape.displayName)
        assertEquals("哈苏夜景", CameraMode.HasselbladNight.displayName)
        assertEquals("哈苏街拍", CameraMode.HasselbladStreet.displayName)
        assertEquals("哈苏专业", CameraMode.HasselbladPro.displayName)
        assertEquals("智能模式", CameraMode.AutoMode.displayName)
        assertEquals("专业模式", CameraMode.ManualMode.displayName)
    }

    @Test
    fun `ColorStyle should have all expected values`() {
        val styles = ColorStyle.entries
        assertEquals(10, styles.size)
        assertEquals("自然", ColorStyle.Natural.displayName)
        assertEquals("鲜明", ColorStyle.Vivid.displayName)
        assertEquals("电影感", ColorStyle.Cinematic.displayName)
        assertEquals("专业", ColorStyle.Professional.displayName)
        assertEquals("暖调", ColorStyle.Warm.displayName)
        assertEquals("冷调", ColorStyle.Cool.displayName)
        assertEquals("经典", ColorStyle.Classic.displayName)
        assertEquals("黑白", ColorStyle.BlackWhite.displayName)
        assertEquals("人像", ColorStyle.Portrait.displayName)
        assertEquals("美食", ColorStyle.Food.displayName)
    }

    @Test
    fun `FocalLengthMode should have all expected values`() {
        val modes = FocalLengthMode.entries
        assertEquals(8, modes.size)
        assertEquals("超广角", FocalLengthMode.UltraWide.displayName)
        assertEquals("广角", FocalLengthMode.Wide.displayName)
        assertEquals("标准", FocalLengthMode.Standard.displayName)
        assertEquals("人像焦", FocalLengthMode.Portrait.displayName)
        assertEquals("长焦", FocalLengthMode.Telephoto.displayName)
        assertEquals("超长焦", FocalLengthMode.UltraTelephoto.displayName)
        assertEquals("微距", FocalLengthMode.Macro.displayName)
        assertEquals("超级微距", FocalLengthMode.SuperMacro.displayName)
    }

    // ==================== 默认值测试 ====================

    @Test
    fun `defaultHasselbladMaster should have correct defaults`() {
        val params = CameraParams.defaultHasselbladMaster()
        assertEquals(CameraMode.HasselbladMaster.displayName, params.mode)
        assertTrue(params.hasselblad_hncs)
        assertTrue(params.hasselbladNaturalColor)
        assertTrue(params.hasselbladProMode)
        assertEquals("HNCS 3.0", params.hasselbladColorScience)
        assertEquals(ColorStyle.Natural.name, params.colorStyle)
        assertEquals(ColorStyle.Natural.displayName, params.colorProfile)
        assertTrue(params.aiOptimization)
        assertTrue(params.proMode)
    }

    @Test
    fun `default CameraParams should have sensible defaults`() {
        val params = CameraParams()
        assertEquals(100, params.iso)
        assertEquals("1/200", params.shutter)
        assertEquals("+0.0", params.ev)
        assertEquals("5500K", params.wb)
        assertEquals("24mm", params.focalLength)
        assertEquals("f/1.8", params.aperture)
        assertFalse(params.hdr)
        assertFalse(params.nightMode)
        assertFalse(params.portraitMode)
        assertTrue(params.aiOptimization)
        assertTrue(params.autoFocus)
        assertTrue(params.opticalStabilization)
        assertFalse(params.rawCapture)
        assertTrue(params.proMode)
        assertTrue(params.hasselblad_hncs)
        assertEquals(50, params.sharpness)
        assertEquals(50, params.contrast)
        assertEquals(50, params.saturation)
    }

    // ==================== 兼容性属性测试 ====================

    @Test
    fun `compatibility aliases should match primary properties`() {
        val params = CameraParams(
            aiOptimization = true,
            focalLength = "50mm",
            hasselbladMasterStyle = "Portrait",
            hasselbladNaturalColor = true,
            colorProfile = "鲜明"
        )
        assertEquals(params.aiOptimization, params.ai_optimization)
        assertEquals(params.focalLength, params.focal_length)
        assertEquals(params.hasselbladMasterStyle, params.hasselblad_master_style)
        assertEquals(params.hasselbladNaturalColor, params.hasselblad_natural_color)
        assertEquals(params.colorProfile, params.color_profile)
    }

    @Test
    fun `empty hasselbladMasterStyle should result in null alias`() {
        val params = CameraParams(hasselbladMasterStyle = "")
        assertNull(params.hasselblad_master_style)
    }

    @Test
    fun `non-empty hasselbladMasterStyle should result in non-null alias`() {
        val params = CameraParams(hasselbladMasterStyle = "Portrait")
        assertEquals("Portrait", params.hasselblad_master_style)
    }

    // ==================== 快门速度格式测试 ====================

    @Test
    fun `various valid shutter speed formats`() {
        val validShutters = listOf("1/30", "1/60", "1/125", "1/200", "1/500", "1/1000", "1/2000", "1/4000", "1/8000")
        validShutters.forEach { shutter ->
            val params = CameraParams(shutter = shutter, colorTemperature = 5500, saturation = 50)
            val result = params.validate()
            assertTrue("Shutter '$shutter' should be valid", result is ValidationResult.Valid)
        }
    }

    @Test
    fun `various invalid shutter speed formats`() {
        val invalidShutters = listOf("auto", "1/", "/200", "abc", "1/0", "")
        invalidShutters.forEach { shutter ->
            val params = CameraParams(shutter = shutter, colorTemperature = 5500, saturation = 50)
            val result = params.validate()
            assertTrue("Shutter '$shutter' should be invalid", result is ValidationResult.Invalid)
        }
    }
}
