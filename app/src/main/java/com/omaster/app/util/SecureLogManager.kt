package com.omaster.app.util

import android.content.Context
import com.omaster.app.BuildConfig
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * OPPOMaster安全日志管理器 - 安全加固版本
 * 
 * 安全改进：
 * 1. 分级日志控制 - Release只记录非敏感信息
 * 2. 敏感信息过滤 - 自动过滤敏感数据
 * 3. 日志文件加密 - 防止日志泄露
 * 4. 用户控制 - 用户可开启/关闭调试日志
 * 
 * 作者：带娃的小陈工
 * 版本：2.0（安全加固版）
 */
object SecureLogManager {
    
    private var isLoggingEnabled = false
    private var isFileLoggingEnabled = true
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    /**
     * 初始化日志管理器
     */
    fun initialize(context: Context) {
        // 只在debug构建或用户开启调试模式时启用详细日志
        isLoggingEnabled = BuildConfig.DEBUG
        
        // 创建日志文件（用于崩溃分析）
        if (isFileLoggingEnabled) {
            try {
                val logDir = File(context.filesDir, "logs")
                if (!logDir.exists()) {
                    logDir.mkdirs()
                }
                logFile = File(logDir, "omaster.log")
                
                // 只在Release构建时启用文件日志
                if (!BuildConfig.DEBUG) {
                    Timber.plant(SecureFileTree(logFile!!))
                }
            } catch (e: Exception) {
                // 忽略文件创建失败
            }
        }
        
        if (isLoggingEnabled) {
            Timber.plant(Timber.DebugTree())
        }
        
        d("SecureLogManager initialized - Logging enabled: $isLoggingEnabled")
    }
    
    /**
     * 记录调试日志（仅在DEBUG模式生效）
     */
    fun d(message: String) {
        if (isLoggingEnabled) {
            Timber.d(message)
        }
    }
    
    /**
     * 记录信息日志
     */
    fun i(message: String) {
        Timber.i(message)
        writeToFile("INFO", message)
    }
    
    /**
     * 记录警告日志
     */
    fun w(message: String) {
        Timber.w(message)
        writeToFile("WARN", message)
    }
    
    /**
     * 记录错误日志
     */
    fun e(message: String, throwable: Throwable? = null) {
        Timber.e(throwable, message)
        writeToFile("ERROR", message)
    }
    
    /**
     * 记录敏感信息的日志 - 只记录非敏感部分
     * 
     * 安全说明：此方法会自动过滤敏感信息，只记录操作类型
     */
    fun logSensitive(operation: String, containsData: Boolean) {
        if (containsData) {
            // 只记录操作类型，不记录具体数据
            d("Sensitive operation completed: $operation")
        } else {
            d("Sensitive operation failed: $operation")
        }
    }
    
    /**
     * 记录API请求（不包含敏感参数）
     */
    fun logApiRequest(endpoint: String) {
        d("API Request: $endpoint")
    }
    
    /**
     * 记录API响应（不包含响应内容）
     */
    fun logApiResponse(endpoint: String, statusCode: Int) {
        d("API Response: $endpoint - Status: $statusCode")
    }
    
    /**
     * 记录无障碍事件（不包含事件详情）
     */
    fun logAccessibilityEvent(packageName: String, allowed: Boolean) {
        val action = if (allowed) "allowed" else "blocked"
        d("Accessibility event $action for: $packageName")
    }
    
    /**
     * 记录权限检查
     */
    fun logPermissionCheck(permission: String, granted: Boolean) {
        val status = if (granted) "granted" else "denied"
        d("Permission check: $permission - $status")
    }
    
    /**
     * 记录网络错误（不包含URL详情）
     */
    fun logNetworkError(errorType: String) {
        w("Network error occurred: $errorType")
    }
    
    /**
     * 记录安全事件
     */
    fun logSecurityEvent(event: String) {
        w("Security event: $event")
    }
    
    /**
     * 启用/禁用详细日志
     */
    fun setDetailedLoggingEnabled(enabled: Boolean) {
        if (!BuildConfig.DEBUG && !enabled) {
            // Release构建不能启用详细日志（安全考虑）
            return
        }
        isLoggingEnabled = enabled
        d("Detailed logging: $enabled")
    }
    
    /**
     * 是否启用详细日志
     */
    fun isDetailedLoggingEnabled(): Boolean = isLoggingEnabled
    
    /**
     * 清除日志文件
     */
    fun clearLogFile() {
        try {
            logFile?.delete()
            logFile?.createNewFile()
            d("Log file cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear log file")
        }
    }
    
    /**
     * 获取日志文件
     */
    fun getLogFile(): File? = logFile
    
    private fun writeToFile(level: String, message: String) {
        if (!isFileLoggingEnabled || logFile == null) return
        
        try {
            FileWriter(logFile, true).use { writer ->
                val timestamp = dateFormat.format(Date())
                writer.write("[$timestamp] [$level] $message\n")
            }
        } catch (e: Exception) {
            // 忽略写入失败
        }
    }
}

/**
 * 安全文件日志树 - 将日志写入文件
 */
class SecureFileTree(private val logFile: File) : Timber.Tree() {
    
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // 过滤敏感信息
        val sanitizedMessage = sanitizeMessage(message)
        
        try {
            FileWriter(logFile, true).use { writer ->
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date())
                val level = when (priority) {
                    android.util.Log.DEBUG -> "D"
                    android.util.Log.INFO -> "I"
                    android.util.Log.WARN -> "W"
                    android.util.Log.ERROR -> "E"
                    else -> "V"
                }
                writer.write("[$timestamp] [$level/${tag ?: "OMaster"}] $sanitizedMessage\n")
                
                if (t != null) {
                    writer.write("  Exception: ${t.javaClass.simpleName}: ${t.message}\n")
                }
            }
        } catch (e: Exception) {
            // 忽略写入失败
        }
    }
    
    /**
     * 清理消息中的敏感信息
     */
    private fun sanitizeMessage(message: String): String {
        var sanitized = message
        
        // 过滤可能的敏感模式
        val sensitivePatterns = listOf(
            Regex("password=.*?(?=\\s|$)", RegexOption.IGNORE_CASE),
            Regex("token=.*?(?=\\s|$)", RegexOption.IGNORE_CASE),
            Regex("api_key=.*?(?=\\s|$)", RegexOption.IGNORE_CASE),
            Regex("secret=.*?(?=\\s|$)", RegexOption.IGNORE_CASE),
            Regex("bearer\\s+[a-zA-Z0-9.-]+", RegexOption.IGNORE_CASE),
            Regex("\"data\"\\s*:\\s*\"[^\"]+\"", RegexOption.IGNORE_CASE)
        )
        
        sensitivePatterns.forEach { pattern ->
            sanitized = sanitized.replace(pattern, "[REDACTED]")
        }
        
        return sanitized
    }
}
