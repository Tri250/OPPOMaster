package com.omaster.app.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 输入验证器
 * CODE-SEC-003: 输入验证
 * 防止SQL注入、XSS、命令注入等攻击
 */
@Singleton
class InputValidator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val SQL_INJECTION_PATTERNS = listOf(
            Pattern.compile("(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|--|;|--)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(or|and)\\s+\\d+\\s*=\\s*\\d+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)'\\s*(or|and)\\s+'", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)union\\s+all\\s+select", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)exec\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)xp_", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)sp_", Pattern.CASE_INSENSITIVE)
        )

        private val XSS_PATTERNS = listOf(
            Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL),
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<[^>]+>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("&lt;", Pattern.CASE_INSENSITIVE),
            Pattern.compile("&gt;", Pattern.CASE_INSENSITIVE)
        )

        private val COMMAND_INJECTION_PATTERNS = listOf(
            Pattern.compile("[;&|`$]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\brce\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcat\\b.*\\b/etc/passwd\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bwget\\b.*\\bhttp", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcurl\\b.*\\bhttp", Pattern.CASE_INSENSITIVE)
        )

        private val PATH_TRAVERSAL_PATTERNS = listOf(
            Pattern.compile("\\.\\./", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\.\\.\\\\", Pattern.CASE_INSENSITIVE),
            Pattern.compile("/etc/passwd", Pattern.CASE_INSENSITIVE),
            Pattern.compile("c:\\\\windows", Pattern.CASE_INSENSITIVE)
        )

        private const val MAX_INPUT_LENGTH = 1000
        private const val MAX_DEPTH = 10
    }

    /**
     * 验证输入是否包含SQL注入
     * CODE-SEC-003: 防止SQL注入
     */
    fun isSqlInjectionSafe(input: String): Boolean {
        if (input.isEmpty()) return true

        SQL_INJECTION_PATTERNS.forEach { pattern ->
            if (pattern.matcher(input).find()) {
                Timber.w("检测到SQL注入尝试: $input")
                return false
            }
        }

        return true
    }

    /**
     * 验证输入是否包含XSS攻击
     * CODE-SEC-003: 防止XSS攻击
     */
    fun isXssSafe(input: String): Boolean {
        if (input.isEmpty()) return true

        XSS_PATTERNS.forEach { pattern ->
            if (pattern.matcher(input).find()) {
                Timber.w("检测到XSS攻击尝试: $input")
                return false
            }
        }

        return true
    }

    /**
     * 验证输入是否包含命令注入
     * CODE-SEC-003: 防止命令注入
     */
    fun isCommandInjectionSafe(input: String): Boolean {
        if (input.isEmpty()) return true

        COMMAND_INJECTION_PATTERNS.forEach { pattern ->
            if (pattern.matcher(input).find()) {
                Timber.w("检测到命令注入尝试: $input")
                return false
            }
        }

        return true
    }

    /**
     * 验证路径遍历攻击
     * CODE-SEC-003: 防止路径遍历
     */
    fun isPathTraversalSafe(input: String): Boolean {
        if (input.isEmpty()) return true

        PATH_TRAVERSAL_PATTERNS.forEach { pattern ->
            if (pattern.matcher(input).find()) {
                Timber.w("检测到路径遍历尝试: $input")
                return false
            }
        }

        return true
    }

    /**
     * 综合输入验证
     * CODE-SEC-003: 所有用户输入都经过严格验证和过滤
     */
    fun validateInput(input: String): ValidationResult {
        if (input.isEmpty()) {
            return ValidationResult(true, emptyList())
        }

        val violations = mutableListOf<String>()

        if (!isSqlInjectionSafe(input)) {
            violations.add("SQL_INJECTION")
        }

        if (!isXssSafe(input)) {
            violations.add("XSS")
        }

        if (!isCommandInjectionSafe(input)) {
            violations.add("COMMAND_INJECTION")
        }

        if (!isPathTraversalSafe(input)) {
            violations.add("PATH_TRAVERSAL")
        }

        if (input.length > MAX_INPUT_LENGTH) {
            violations.add("MAX_LENGTH_EXCEEDED")
        }

        return ValidationResult(violations.isEmpty(), violations)
    }

    /**
     * 清理输入
     * CODE-SEC-003: 输入过滤
     */
    fun sanitizeInput(input: String): String {
        return input
            .replace(Regex("<"), "&lt;")
            .replace(Regex(">"), "&gt;")
            .replace(Regex("\""), "&quot;")
            .replace(Regex("'"), "&#x27;")
            .replace(Regex("/"), "&#x2F;")
            .trim()
            .take(MAX_INPUT_LENGTH)
    }

    /**
     * 验证文件名
     * CODE-SEC-003: 文件名格式限制
     */
    fun isValidFileName(fileName: String): Boolean {
        if (fileName.isEmpty() || fileName.length > 255) {
            return false
        }

        val invalidChars = Pattern.compile("[\\\\/:*?\"<>|]")
        if (invalidChars.matcher(fileName).find()) {
            return false
        }

        if (fileName.contains("..") || fileName.startsWith(".")) {
            return false
        }

        return true
    }

    /**
     * 验证邮箱格式
     */
    fun isValidEmail(email: String): Boolean {
        val emailPattern = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )
        return emailPattern.matcher(email).matches()
    }

    /**
     * 验证URL格式
     */
    fun isValidUrl(url: String): Boolean {
        val urlPattern = Pattern.compile(
            "^(https?)://[A-Za-z0-9.-]+\\.[A-Za-z]{2,}(/.*)?$",
            Pattern.CASE_INSENSITIVE
        )
        return urlPattern.matcher(url).matches()
    }

    /**
     * 验证数字范围
     */
    fun isValidNumberRange(value: String, min: Long, max: Long): Boolean {
        return try {
            val number = value.toLong()
            number in min..max
        } catch (e: NumberFormatException) {
            false
        }
    }

    /**
     * 验证JSON格式
     */
    fun isValidJson(json: String): Boolean {
        return try {
            val trimmed = json.trim()
            (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                    (trimmed.startsWith("[") && trimmed.endsWith("]"))
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查输入复杂度
     */
    fun checkInputComplexity(input: String): InputComplexity {
        return InputComplexity(
            length = input.length,
            hasUpperCase = input.any { it.isUpperCase() },
            hasLowerCase = input.any { it.isLowerCase() },
            hasDigit = input.any { it.isDigit() },
            hasSpecialChar = input.any { !it.isLetterOrDigit() }
        )
    }
}

/**
 * 验证结果
 */
data class ValidationResult(
    val isValid: Boolean,
    val violations: List<String>
)

/**
 * 输入复杂度
 */
data class InputComplexity(
    val length: Int,
    val hasUpperCase: Boolean,
    val hasLowerCase: Boolean,
    val hasDigit: Boolean,
    val hasSpecialChar: Boolean
)
