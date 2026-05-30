package com.omaster.app.model

sealed interface ValidationResult {
    object Valid : ValidationResult
    data class Invalid(val errors: List<String>) : ValidationResult
}

data class CameraParams(
    val mode: String = "master",
    val filter: String = "",
    val iso: Int = 64,
    val shutter: String = "1/200",
    val ev: String = "0",
    val wb: String = "5500K",
    val focal_length: String = "24mm",
    val aperture: String = "f/1.8",
    val hdr: Boolean = false,
    val night_mode: Boolean = false,
    val portrait_mode: Boolean = false,
    val macro_mode: Boolean = false,
    val sports_mode: Boolean = false,
    val ai_optimization: Boolean = true,
    val hasselblad_hncs: Boolean = false,
    val hasselblad_natural_color: Boolean = true,
    val hasselblad_master_style: String = "",
    val color_profile: String = "Natural",
    val sharpness: Int = 50,
    val contrast: Int = 50,
    val saturation: Int = 50
) {
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        // ISO 校验 - 2026年OPPO最新范围
        if (iso < 50 || iso > 102400) {
            errors.add("ISO 超出范围（应在 50-102400 之间）")
        }
        
        // 快门格式校验
        if (!isValidShutterSpeed(shutter)) {
            errors.add("快门格式无效")
        }
        
        // EV 格式校验
        if (!isValidExposureValue(ev)) {
            errors.add("EV 格式无效")
        }
        
        return if (errors.isEmpty()) ValidationResult.Valid 
               else ValidationResult.Invalid(errors)
    }
    
    private fun isValidShutterSpeed(shutter: String): Boolean {
        val fractionRegex = Regex("""^1/\d+$""")
        val numberRegex = Regex("""^\d+(\.\d+)?s$""")
        val plainNumberRegex = Regex("""^\d+(\.\d+)?$""")
        return fractionRegex.matches(shutter) || 
               numberRegex.matches(shutter) || 
               plainNumberRegex.matches(shutter)
    }
    
    private fun isValidExposureValue(ev: String): Boolean {
        val evRegex = Regex("""^[+-]?\d+(\.\d+)?$""")
        return evRegex.matches(ev)
    }
    
    fun formatParamsForDisplay(): String {
        return buildString {
            append("ISO $iso")
            append(" · $shutter")
            if (ev != "0") append(" · EV $ev")
            append(" · $wb")
            if (hasselblad_hncs) append(" · HNCS")
        }
    }
}