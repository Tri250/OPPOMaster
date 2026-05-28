package com.omaster.app.model

sealed interface ValidationResult {
    object Valid : ValidationResult
    data class Invalid(val errors: List<String>) : ValidationResult
}

data class CameraParams(
    val mode: String = "master",
    val filter: String = "",
    val iso: Int = 100,
    val shutter: String = "1/125",
    val ev: String = "0",
    val wb: String = "5500K",
    val hasselblad_hncs: Boolean = false,
    val softLight: Int = 0,
    val tone: Int = 0,
    val saturation: Int = 0,
    val warmth: Int = 0,
    val cyanMagenta: Int = 0,
    val sharpness: Int = 0,
    val vignetting: Int = 0,
    val hue: String = ""
) {
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        // ISO 校验
        if (iso < 50 || iso > 25600) {
            errors.add("ISO 超出范围（应在 50-25600 之间）")
        }
        
        // 快门格式校验
        if (!isValidShutterSpeed(shutter)) {
            errors.add("快门格式无效")
        }
        
        // EV 格式校验
        if (!isValidExposureValue(ev)) {
            errors.add("EV 格式无效")
        }
        
        // 柔光参数校验 (-100 到 100)
        if (softLight < -100 || softLight > 100) {
            errors.add("柔光参数超出范围（应在 -100 到 100 之间）")
        }
        
        // 影调参数校验 (-100 到 100)
        if (tone < -100 || tone > 100) {
            errors.add("影调参数超出范围（应在 -100 到 100 之间）")
        }
        
        // 饱和度参数校验 (-100 到 100)
        if (saturation < -100 || saturation > 100) {
            errors.add("饱和度参数超出范围（应在 -100 到 100 之间）")
        }
        
        // 冷暖参数校验 (-100 到 100)
        if (warmth < -100 || warmth > 100) {
            errors.add("冷暖参数超出范围（应在 -100 到 100 之间）")
        }
        
        // 青品参数校验 (-100 到 100)
        if (cyanMagenta < -100 || cyanMagenta > 100) {
            errors.add("青品参数超出范围（应在 -100 到 100 之间）")
        }
        
        // 锐度参数校验 (-100 到 100)
        if (sharpness < -100 || sharpness > 100) {
            errors.add("锐度参数超出范围（应在 -100 到 100 之间）")
        }
        
        // 暗角参数校验 (-100 到 100)
        if (vignetting < -100 || vignetting > 100) {
            errors.add("暗角参数超出范围（应在 -100 到 100 之间）")
        }
        
        return if (errors.isEmpty()) ValidationResult.Valid 
               else ValidationResult.Invalid(errors)
    }
    
    private fun isValidShutterSpeed(shutter: String): Boolean {
        val fractionRegex = Regex("""^1/\d+$""")
        val numberRegex = Regex("""^\d+(\.\d+)?$""")
        return fractionRegex.matches(shutter) || numberRegex.matches(shutter)
    }
    
    private fun isValidExposureValue(ev: String): Boolean {
        val evRegex = Regex("""^[+-]?\d+(\.\d+)?$""")
        return evRegex.matches(ev)
    }
}
