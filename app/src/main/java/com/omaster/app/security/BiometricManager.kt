package com.omaster.app.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OMaster生物识别管理器
 * 遵循SP-009生物识别校验标准
 *
 * 功能：
 * 1. 生物识别可用性检查
 * 2. 应用锁保护
 * 3. 安全验证
 *
 * 作者：小O帮帮
 */
@Singleton
class BiometricSecurityManager @Inject constructor(
    private val context: Context
) {

    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isAppLocked = MutableStateFlow(true)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _securityLevel = MutableStateFlow(SecurityLevel.None)
    val securityLevel: StateFlow<SecurityLevel> = _securityLevel.asStateFlow()

    private val biometricManager = BiometricManager.from(context)

    /**
     * 检查生物识别可用性
     */
    fun checkBiometricAvailability(): BiometricAvailability {
        val canAuthenticate = biometricManager.canAuthenticate(
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        )

        return when (canAuthenticate) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                BiometricAvailability.Available
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                BiometricAvailability.NotEnrolled
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                BiometricAvailability.NoHardware
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                BiometricAvailability.Unavailable
            }
            else -> {
                BiometricAvailability.Unsupported
            }
        }
    }

    /**
     * 显示生物识别验证对话框
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "验证身份",
        subtitle: String = "使用生物识别解锁应用",
        description: String = "请使用指纹或面部识别",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor, 
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Timber.e("Biometric authentication error: $errString")
                    onError(errString.toString())
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Timber.d("Biometric authentication succeeded")
                    _isAppLocked.value = false
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Timber.w("Biometric authentication failed")
                    onError("验证失败，请重试")
                }

                override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence) {
                    super.onAuthenticationHelp(helpCode, helpString)
                    Timber.w("Biometric authentication help: $helpString")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * 启用应用锁
     */
    fun enableAppLock() {
        _isBiometricEnabled.value = true
        _isAppLocked.value = true
        Timber.d("App lock enabled")
    }

    /**
     * 禁用应用锁
     */
    fun disableAppLock() {
        _isBiometricEnabled.value = false
        _isAppLocked.value = false
        Timber.d("App lock disabled")
    }

    /**
     * 锁定应用
     */
    fun lockApp() {
        if (_isBiometricEnabled.value) {
            _isAppLocked.value = true
            Timber.d("App locked")
        }
    }

    /**
     * 解锁应用
     */
    fun unlockApp() {
        _isAppLocked.value = false
        Timber.d("App unlocked")
    }

    /**
     * 设置安全级别
     */
    fun setSecurityLevel(level: SecurityLevel) {
        _securityLevel.value = level
        Timber.d("Security level set to: $level")
    }

    /**
     * 检查是否需要验证
     */
    fun shouldRequireAuth(): Boolean {
        return _isBiometricEnabled.value && _isAppLocked.value
    }
}

/**
 * 生物识别可用性状态
 */
sealed interface BiometricAvailability {
    object Available : BiometricAvailability
    object NotEnrolled : BiometricAvailability
    object NoHardware : BiometricAvailability
    object Unavailable : BiometricAvailability
    object Unsupported : BiometricAvailability
}

/**
 * 安全级别
 */
enum class SecurityLevel(val description: String) {
    None("无保护"),
    Basic("基础保护"),
    High("高级保护"),
    Maximum("最高保护")
}
