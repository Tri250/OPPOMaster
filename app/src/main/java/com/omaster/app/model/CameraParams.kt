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
    val hasselblad_hncs: Boolean = false
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
