package com.omaster.app.util

import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

enum class ExceptionSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

sealed class ExceptionHandlerResult {
    data object Ignored : ExceptionHandlerResult()
    data class Logged(val severity: ExceptionSeverity) : ExceptionHandlerResult()
    data class UserNotified(val message: String, val severity: ExceptionSeverity) : ExceptionHandlerResult()
    data class Recovered(val fallbackValue: Any?) : ExceptionHandlerResult()
}

@Singleton
class UnifiedExceptionHandler @Inject constructor() {

    private val handledExceptionCount = AtomicInteger(0)

    private val exceptionStatistics = mutableMapOf<String, Int>()

    fun handleException(
        throwable: Throwable,
        context: String = "Unknown",
        severity: ExceptionSeverity = ExceptionSeverity.MEDIUM,
        userMessage: String? = null,
        showToUser: Boolean = false
    ): ExceptionHandlerResult {
        handledExceptionCount.incrementAndGet()

        val exceptionClassName = throwable::class.java.simpleName
        exceptionStatistics[exceptionClassName] = (exceptionStatistics[exceptionClassName] ?: 0) + 1

        val logMessage = "[$context] ${throwable::class.java.simpleName}: ${throwable.message}"

        return when (severity) {
            ExceptionSeverity.LOW -> {
                Timber.d(logMessage, throwable)
                ExceptionHandlerResult.Logged(severity)
            }
            ExceptionSeverity.MEDIUM -> {
                Timber.w(logMessage, throwable)
                if (showToUser && userMessage != null) {
                    ExceptionHandlerResult.UserNotified(userMessage, severity)
                } else {
                    ExceptionHandlerResult.Logged(severity)
                }
            }
            ExceptionSeverity.HIGH -> {
                Timber.e(logMessage, throwable)
                if (userMessage != null) {
                    ExceptionHandlerResult.UserNotified(userMessage, severity)
                } else {
                    ExceptionHandlerResult.Logged(severity)
                }
            }
            ExceptionSeverity.CRITICAL -> {
                Timber wtf(throwable, "CRITICAL: $logMessage")
                ExceptionHandlerResult.UserNotified(
                    userMessage ?: "发生严重错误，请重启应用",
                    severity
                )
            }
        }
    }

    fun <T> handleWithFallback(
        fallbackValue: T,
        context: String = "Unknown",
        operation: () -> T
    ): T {
        return try {
            operation()
        } catch (e: Exception) {
            handleException(
                throwable = e,
                context = context,
                severity = ExceptionSeverity.MEDIUM
            )
            fallbackValue
        }
    }

    fun <T> handleWithRecovery(
        fallbackValue: T,
        recoveryOperation: () -> T,
        context: String = "Unknown"
    ): T {
        return try {
            recoveryOperation()
        } catch (e: Exception) {
            handleException(
                throwable = e,
                context = context,
                severity = ExceptionSeverity.HIGH
            )
            fallbackValue
        }
    }

    fun handleSilently(operation: () -> Unit) {
        try {
            operation()
        } catch (e: Exception) {
            Timber.d("[SilentHandler] ${e::class.java.simpleName}: ${e.message}")
        }
    }

    fun <T> safeExecute(
        operation: () -> T,
        onError: ((Exception) -> T)? = null
    ): T? {
        return try {
            operation()
        } catch (e: Exception) {
            Timber.e(e, "Safe execute failed")
            onError?.invoke(e)
            null
        }
    }

    fun getHandledExceptionCount(): Int = handledExceptionCount.get()

    fun getExceptionStatistics(): Map<String, Int> = exceptionStatistics.toMap()

    fun resetStatistics() {
        handledExceptionCount.set(0)
        exceptionStatistics.clear()
        Timber.d("Exception statistics reset")
    }
}

@Singleton
class AtomicVersionManager @Inject constructor() {

    @Volatile
    private var currentVersion: Int = 0

    private val pendingVersion = AtomicInteger(-1)

    fun setVersion(version: Int): Boolean {
        val result = pendingVersion.compareAndSet(-1, version)
        if (result) {
            currentVersion = version
            Timber.d("Version updated to: $version")
        }
        return result
    }

    fun getVersion(): Int = currentVersion

    fun isUpdatePending(): Boolean = pendingVersion.get() != -1 && pendingVersion.get() != currentVersion

    fun confirmUpdate(): Boolean {
        if (pendingVersion.get() != -1) {
            currentVersion = pendingVersion.get()
            pendingVersion.set(-1)
            Timber.d("Version update confirmed: $currentVersion")
            return true
        }
        return false
    }

    fun cancelUpdate(): Boolean {
        if (pendingVersion.get() != -1) {
            pendingVersion.set(-1)
            Timber.d("Version update cancelled, current: $currentVersion")
            return true
        }
        return false
    }
}

inline fun <T> runCatching(
    context: String,
    severity: ExceptionSeverity = ExceptionSeverity.MEDIUM,
    block: () -> T
): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        Timber.e(e, "[$context] ${e::class.java.simpleName}: ${e.message}")
        Result.failure(e)
    }
}

inline fun <T> runCatchingWithFallback(
    fallback: T,
    context: String = "Unknown",
    block: () -> T
): T {
    return try {
        block()
    } catch (e: Exception) {
        Timber.e(e, "[$context] Fallback triggered: ${e::class.java.simpleName}")
        fallback
    }
}
