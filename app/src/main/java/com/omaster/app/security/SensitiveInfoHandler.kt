package com.omaster.app.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 敏感信息处理器
 * CODE-SEC-001: 代码扫描
 * CODE-SEC-002: 敏感信息硬编码
 * 处理敏感信息的检测、存储和传输
 */
@Singleton
class SensitiveInfoHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorageManager: SecureStorageManager
) {
    companion object {
        private val SENSITIVE_PATTERNS = listOf(
            Pattern.compile("(?i)(api[_-]?key|apikey)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(secret[_-]?key|secretkey)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(password|passwd|pwd)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(private[_-]?key|privatekey)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(access[_-]?token|accesstoken)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(bearer\\s+[a-zA-Z0-9._-]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(token)\\s*[:=]\\s*['\"]?[a-zA-Z0-9._-]{16,}['\"]?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(jwt)\\s*[:=]\\s*['\"]?[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+['\"]?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(phone|mobile|telephone)\\s*[:=]\\s*['\"]?\\d{10,}", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(credit[_-]?card|card[_-]?number|cvv|expiry)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(ssn|social[_-]?security)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(aes[_-]?key|rsa[_-]?key|encryption[_-]?key)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(keystore|key[_-]?store)", Pattern.CASE_INSENSITIVE)
        )

        private val API_KEY_PATTERNS = listOf(
            Pattern.compile("(?i)AIza[0-9A-Za-z_-]{35}", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)sk-[0-9A-Za-z_-]{48}", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)pk_[0-9A-Za-z_-]{48}", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)xox[baprs]-[0-9A-Za-z_-]{10,}", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)ghp_[0-9A-Za-z]{36}", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)AKIA[0-9A-Z]{16}", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)SG\\.[0-9A-Za-z_-]{22}\\.[0-9A-Za-z_-]{43}", Pattern.CASE_INSENSITIVE)
        )

        private val WHITELISTED_STRINGS = setOf(
            "example.com",
            "test@example.com",
            "your-api-key",
            "YOUR_API_KEY",
            "placeholder",
            "xxxxx",
            "****",
            "TODO",
            "FIXME",
            "TODO:",
            "FIXME:"
        )
    }

    /**
     * 检测代码中是否包含敏感信息
     * CODE-SEC-002: 搜索代码中所有硬编码的字符串
     */
    fun detectSensitiveInfo(code: String): List<SensitiveInfoMatch> {
        val matches = mutableListOf<SensitiveInfoMatch>()

        SENSITIVE_PATTERNS.forEachIndexed { index, pattern ->
            val matcher = pattern.matcher(code)
            while (matcher.find()) {
                val matchedText = matcher.group()
                if (!isWhitelisted(matchedText)) {
                    matches.add(
                        SensitiveInfoMatch(
                            type = getSensitiveType(index),
                            value = maskSensitiveValue(matchedText),
                            startIndex = matcher.start(),
                            endIndex = matcher.end()
                        )
                    )
                }
            }
        }

        API_KEY_PATTERNS.forEach { pattern ->
            val matcher = pattern.matcher(code)
            while (matcher.find()) {
                matches.add(
                    SensitiveInfoMatch(
                        type = SensitiveType.API_KEY,
                        value = maskApiKey(matcher.group()),
                        startIndex = matcher.start(),
                        endIndex = matcher.end()
                    )
                )
            }
        }

        return matches.distinctBy { it.value }
    }

    /**
     * 检查是否为白名单字符串
     */
    private fun isWhitelisted(text: String): Boolean {
        return WHITELISTED_STRINGS.any {
            text.contains(it, ignoreCase = true)
        }
    }

    /**
     * 获取敏感信息类型
     */
    private fun getSensitiveType(index: Int): SensitiveType {
        return when (index) {
            0 -> SensitiveType.API_KEY
            1 -> SensitiveType.SECRET_KEY
            2 -> SensitiveType.PASSWORD
            3 -> SensitiveType.PRIVATE_KEY
            4 -> SensitiveType.ACCESS_TOKEN
            5 -> SensitiveType.BEARER_TOKEN
            6 -> SensitiveType.TOKEN
            7 -> SensitiveType.JWT
            8 -> SensitiveType.EMAIL
            9 -> SensitiveType.PHONE
            10 -> SensitiveType.CREDIT_CARD
            11 -> SensitiveType.SSN
            12 -> SensitiveType.ENCRYPTION_KEY
            13 -> SensitiveType.KEYSTORE
            else -> SensitiveType.OTHER
        }
    }

    /**
     * 脱敏显示
     */
    private fun maskSensitiveValue(value: String): String {
        return when {
            value.length <= 4 -> "****"
            value.length <= 8 -> value.take(2) + "****"
            else -> value.take(4) + "****" + value.takeLast(4)
        }
    }

    /**
     * 脱敏API Key
     */
    private fun maskApiKey(key: String): String {
        return if (key.length > 8) {
            key.take(4) + "****" + key.takeLast(4)
        } else {
            "****"
        }
    }

    /**
     * 安全存储敏感信息
     * CODE-SEC-002: 配置文件中的敏感信息已加密
     */
    fun storeSensitiveInfo(key: String, value: String) {
        try {
            secureStorageManager.putEncryptedString("sensitive_$key", value)
            Timber.d("敏感信息已安全存储: $key")
        } catch (e: Exception) {
            Timber.e(e, "存储敏感信息失败: $key")
        }
    }

    /**
     * 获取敏感信息
     */
    fun getSensitiveInfo(key: String): String? {
        return try {
            secureStorageManager.getEncryptedString("sensitive_$key")
        } catch (e: Exception) {
            Timber.e(e, "获取敏感信息失败: $key")
            null
        }
    }

    /**
     * 删除敏感信息
     */
    fun deleteSensitiveInfo(key: String) {
        secureStorageManager.remove("sensitive_$key")
        Timber.d("敏感信息已删除: $key")
    }

    /**
     * 清理所有敏感信息
     */
    fun clearAllSensitiveInfo() {
        secureStorageManager.clear()
        Timber.d("所有敏感信息已清理")
    }

    /**
     * 扫描APK中是否包含敏感信息
     * CODE-SEC-002: 检测APK中硬编码的敏感信息
     */
    fun scanApkForSensitiveInfo(apkPath: String): ScanResult {
        val matches = mutableListOf<SensitiveInfoMatch>()
        val issues = mutableListOf<ScanIssue>()

        try {
            val dexFiles = extractDexFiles(apkPath)
            dexFiles.forEach { dexFile ->
                val content = String(dexFile, Charsets.UTF_8)
                val foundMatches = detectSensitiveInfo(content)
                matches.addAll(foundMatches)
            }

            if (matches.isNotEmpty()) {
                issues.add(
                    ScanIssue(
                        severity = IssueSeverity.HIGH,
                        category = IssueCategory.SENSITIVE_INFO,
                        description = "在APK中发现${matches.size}处可能的敏感信息",
                        details = matches.map { it.value }
                    )
                )
            }

            val nativeLibraries = extractNativeLibraries(apkPath)
            if (nativeLibraries.isNotEmpty()) {
                issues.add(
                    ScanIssue(
                        severity = IssueSeverity.INFO,
                        category = IssueCategory.NATIVE_LIBS,
                        description = "APK包含${nativeLibraries.size}个原生库"
                    )
                )
            }

        } catch (e: Exception) {
            Timber.e(e, "APK扫描失败")
            return ScanResult(false, listOf(
                ScanIssue(
                    severity = IssueSeverity.ERROR,
                    category = IssueCategory.SCAN_ERROR,
                    description = "扫描过程出错: ${e.message}"
                )
            ))
        }

        return ScanResult(true, issues)
    }

    /**
     * 提取DEX文件
     */
    private fun extractDexFiles(apkPath: String): List<ByteArray> {
        return try {
            val apkFile = java.io.File(apkPath)
            if (!apkFile.exists()) {
                return emptyList()
            }

            val dexFiles = mutableListOf<ByteArray>()
            val zipFile = java.util.zip.ZipFile(apkFile)
            val entries = zipFile.entries()

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.endsWith(".dex")) {
                    val buffer = ByteArray(entry.size.toInt())
                    zipFile.getInputStream(entry).use { input ->
                        input.read(buffer)
                    }
                    dexFiles.add(buffer)
                }
            }
            zipFile.close()

            dexFiles
        } catch (e: Exception) {
            Timber.e(e, "提取DEX文件失败")
            emptyList()
        }
    }

    /**
     * 提取原生库列表
     */
    private fun extractNativeLibraries(apkPath: String): List<String> {
        return try {
            val apkFile = java.io.File(apkPath)
            if (!apkFile.exists()) {
                return emptyList()
            }

            val libraries = mutableListOf<String>()
            val zipFile = java.util.zip.ZipFile(apkFile)
            val entries = zipFile.entries()

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.startsWith("lib/") && entry.name.endsWith(".so")) {
                    libraries.add(entry.name)
                }
            }
            zipFile.close()

            libraries
        } catch (e: Exception) {
            Timber.e(e, "提取原生库失败")
            emptyList()
        }
    }
}

/**
 * 敏感信息类型
 */
enum class SensitiveType {
    API_KEY,
    SECRET_KEY,
    PASSWORD,
    PRIVATE_KEY,
    ACCESS_TOKEN,
    BEARER_TOKEN,
    TOKEN,
    JWT,
    EMAIL,
    PHONE,
    CREDIT_CARD,
    SSN,
    ENCRYPTION_KEY,
    KEYSTORE,
    OTHER
}

/**
 * 敏感信息匹配
 */
data class SensitiveInfoMatch(
    val type: SensitiveType,
    val value: String,
    val startIndex: Int,
    val endIndex: Int
)

/**
 * 扫描问题
 */
data class ScanIssue(
    val severity: IssueSeverity,
    val category: IssueCategory,
    val description: String,
    val details: List<String> = emptyList()
)

/**
 * 问题严重程度
 */
enum class IssueSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
    ERROR,
    INFO
}

/**
 * 问题类别
 */
enum class IssueCategory {
    SENSITIVE_INFO,
    NATIVE_LIBS,
    PERMISSIONS,
    VULNERABILITY,
    CONFIGURATION,
    SCAN_ERROR
}

/**
 * 扫描结果
 */
data class ScanResult(
    val success: Boolean,
    val issues: List<ScanIssue>
)
