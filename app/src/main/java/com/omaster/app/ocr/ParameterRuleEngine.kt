package com.omaster.app.ocr

import com.omaster.app.model.CameraParams
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParameterRuleEngine @Inject constructor() {

    companion object {
        // 静态 Regex 模式，避免每次调用时重新创建
        private val COLD_WARM_REGEX = Regex("""冷暖[:\s]*([+-]?\d+)""")
        private val CYAN_MAGENTA_REGEX = Regex("""青品[:\s]*([+-]?\d+)""")
        private val SATURATION_REGEX = Regex("""饱和度[:\s]*(?:[+-]?(\d+)|(高|中|低|标准))""")
        private val VIGNETTE_REGEX = Regex("""暗角[:\s]*([+-]?\d+)""")
        private val SHARPNESS_REGEX = Regex("""锐度[:\s]*([+-]?\d+)""")
        private val CONTRAST_REGEX = Regex("""对比度[:\s]*([+-]?\d+)""")
    }

    data class ExtractedParams(
        val iso: Int?,
        val shutter: String?,
        val ev: String?,
        val wb: String?,
        val mode: String?,
        val filter: String?,
        val focalLength: String?,
        val aperture: String?,
        val confidence: Float,
        val rawMatches: Map<String, String>
    )

    private val rulePatterns = listOf(
        // ISO patterns - 多种格式支持
        PatternRule(
            "iso",
            listOf(
                Regex("""ISO[:\s]*(\d+)""", RegexOption.IGNORE_CASE),
                Regex("""iso[:\s]*(\d+)""", RegexOption.IGNORE_CASE),
                Regex("""感光度[:\s]*(\d+)"""),
                Regex("""\b(\d{2,5})\s*ISO\b""", RegexOption.IGNORE_CASE),
                Regex("""\bISO\s*(\d{2,5})\b""", RegexOption.IGNORE_CASE)
            ),
            "iso"
        ),
        
        // Shutter speed patterns - 快门速度
        PatternRule(
            "shutter",
            listOf(
                Regex("""S[:\s=]*1/(\d+)""", RegexOption.IGNORE_CASE),
                Regex("""快门[:\s]*1/(\d+)"""),
                Regex("""(\d+)s\b"""),
                Regex("""\b1/(\d+)\s*(?:s|秒)?\b"""),
                Regex("""\b(\d{3,5})\s*(?:s|秒)\b""")
            ),
            "shutter"
        ),
        
        // EV patterns - 曝光补偿
        PatternRule(
            "ev",
            listOf(
                Regex("""EV\s*([+-]?\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE),
                Regex("""曝光补偿[:\s]*([+-]?\d+(?:\.\d+)?)"""),
                Regex("""EV[:\s]*([+-]?\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE),
                Regex("""\b([+-]?\d+(?:\.\d+)?)\s*EV\b""")
            ),
            "ev"
        ),
        
        // White Balance patterns - 白平衡
        PatternRule(
            "wb",
            listOf(
                Regex("""WB[:\s]*(\d+)K?""", RegexOption.IGNORE_CASE),
                Regex("""白平衡[:\s]*(?:(\d+)K?|Auto)"""),
                Regex("""\b(\d{4})K?\b"""),
                Regex("""\b(Auto|手动|日光|阴天|荧光|钨丝)\b""", RegexOption.IGNORE_CASE)
            ),
            "wb"
        ),
        
        // Mode patterns - 拍摄模式
        PatternRule(
            "mode",
            listOf(
                Regex("""模式[:\s]*(专业|普通|自动|夜景|人像|风景|HDR|大师|哈苏)"""),
                Regex("""\b(专业|普通|自动|夜景|人像|风景|HDR|大师|哈苏)\s*模式\b"""),
                Regex("""\b(M|P|A|S|Auto|Manual)\b""", RegexOption.IGNORE_CASE)
            ),
            "mode"
        ),
        
        // Filter/风格 patterns - 滤镜风格
        PatternRule(
            "filter",
            listOf(
                Regex("""滤镜[:\s]*(哈苏|自然|美食|夜景|人像|胶片|黑白|Vivid|Natural|Cinematic)"""),
                Regex("""风格[:\s]*(哈苏|自然|美食|夜景|人像|胶片|黑白|Vivid|Natural|Cinematic)"""),
                Regex("""\b(HNCS|LOG|D-Cinelike)\b""", RegexOption.IGNORE_CASE),
                Regex("""影调[:\s]*(高调|低调|中间调)"""),
                Regex("""饱和度[:\s]*(高|中|低|标准)""")
            ),
            "filter"
        ),
        
        // Focal Length patterns - 焦距
        PatternRule(
            "focal",
            listOf(
                Regex("""焦距[:\s]*(\d+(?:\.\d+)?)\s*mm"""),
                Regex("""\b(\d+(?:\.\d+)?)\s*mm\b"""),
                Regex("""\b(\d+)mm""")
            ),
            "focal"
        ),
        
        // Aperture patterns - 光圈
        PatternRule(
            "aperture",
            listOf(
                Regex("""光圈[:\s]*(f/\d+(?:\.\d+)?)"""),
                Regex("""\b(f/\d+(?:\.\d+)?)\b"""),
                Regex("""\bF(\d+(?:\.\d+)?)\b""")
            ),
            "aperture"
        )
    )

    data class PatternRule(
        val paramName: String,
        val patterns: List<Regex>,
        val outputKey: String
    )

    fun extractParams(rawText: String): ExtractedParams {
        Timber.d("Extracting params from text: $rawText")
        
        val matches = mutableMapOf<String, String>()
        var totalConfidence = 0f
        var patternCount = 0

        rulePatterns.forEach { rule ->
            rule.patterns.forEach { pattern ->
                val matchResult = pattern.find(rawText)
                if (matchResult != null) {
                    val value = matchResult.groupValues.getOrNull(1) ?: matchResult.value
                    if (matches[rule.paramName] == null) {
                        matches[rule.paramName] = value
                        totalConfidence += 1.0f
                        patternCount++
                        Timber.d("Matched ${rule.paramName}: $value")
                    }
                }
            }
        }

        // 额外处理复合参数
        processCompoundParams(rawText, matches)

        val confidence = if (patternCount > 0) totalConfidence / patternCount else 0f

        return ExtractedParams(
            iso = matches["iso"]?.toIntOrNull(),
            shutter = matches["shutter"]?.let { formatShutter(it) },
            ev = matches["ev"],
            wb = matches["wb"]?.let { formatWb(it) },
            mode = matches["mode"]?.let { formatMode(it) },
            filter = matches["filter"],
            focalLength = matches["focal"]?.let { "${it}mm" },
            aperture = matches["aperture"]?.let { formatAperture(it) },
            confidence = confidence,
            rawMatches = matches.toMap()
        )
    }

    private fun processCompoundParams(rawText: String, matches: MutableMap<String, String>) {
        // 处理冷暖/青品等高级参数 - 使用静态 Regex
        val advancedParams = mapOf(
            "冷暖" to COLD_WARM_REGEX.find(rawText)?.groupValues?.getOrNull(1),
            "青品" to CYAN_MAGENTA_REGEX.find(rawText)?.groupValues?.getOrNull(1),
            "饱和度" to SATURATION_REGEX.find(rawText)?.let { 
                it.groupValues.getOrNull(1) ?: it.groupValues.getOrNull(2)
            },
            "暗角" to VIGNETTE_REGEX.find(rawText)?.groupValues?.getOrNull(1),
            "锐度" to SHARPNESS_REGEX.find(rawText)?.groupValues?.getOrNull(1),
            "对比度" to CONTRAST_REGEX.find(rawText)?.groupValues?.getOrNull(1)
        )
        
        advancedParams.forEach { (key, value) ->
            if (value != null && matches[key] == null) {
                matches[key] = value
            }
        }
    }

    private fun formatShutter(value: String): String {
        return when {
            value.contains("1/") -> value
            value.endsWith("s") -> value
            value.toDoubleOrNull() != null -> "${value}s"
            else -> "1/$value"
        }
    }

    private fun formatWb(value: String): String {
        return when {
            value.endsWith("K", ignoreCase = true) -> value.uppercase()
            value.toIntOrNull() != null -> "${value}K"
            value.equals("Auto", ignoreCase = true) -> "Auto"
            else -> "${value}K"
        }
    }

    private fun formatMode(value: String): String {
        return when (value.lowercase()) {
            "m", "manual" -> "专业"
            "p" -> "程序"
            "a" -> "光圈优先"
            "s" -> "快门优先"
            "auto" -> "自动"
            "hdr" -> "HDR"
            "大师", "哈苏" -> "哈苏大师"
            else -> value
        }
    }

    private fun formatAperture(value: String): String {
        return if (value.startsWith("f", ignoreCase = true) || value.startsWith("F")) {
            value
        } else {
            "f/$value"
        }
    }

    fun convertToCameraParams(extracted: ExtractedParams): CameraParams {
        return CameraParams(
            mode = extracted.mode ?: "哈苏大师",
            filter = extracted.filter ?: "",
            iso = extracted.iso ?: 100,
            shutter = extracted.shutter ?: "1/125",
            ev = extracted.ev ?: "0",
            wb = extracted.wb ?: "5500K",
            focal_length = extracted.focalLength ?: "24mm",
            aperture = extracted.aperture ?: "f/1.8",
            hasselblad_hncs = extracted.filter?.contains("哈苏", ignoreCase = true) == true ||
                            extracted.filter?.contains("HNCS", ignoreCase = true) == true,
            hasselblad_natural_color = extracted.filter?.contains("Natural", ignoreCase = true) == true ||
                                       extracted.filter?.contains("自然", ignoreCase = true) == true,
            hasselblad_master_style = extracted.mode ?: "",
            color_profile = extracted.filter ?: "Natural"
        )
    }

    fun validateParams(params: CameraParams): List<String> {
        val errors = mutableListOf<String>()
        
        // ISO范围校验 - 2026年OPPO最新范围
        if (params.iso < 50 || params.iso > 102400) {
            errors.add("ISO ${params.iso} 超出范围（应为 50-102400）")
        }
        
        // EV范围校验
        val evValue = params.ev.replace("+", "").toDoubleOrNull() ?: 0.0
        if (evValue < -5.0 || evValue > 5.0) {
            errors.add("EV ${params.ev} 超出范围（应为 -5.0 到 +5.0）")
        }
        
        return errors
    }
}