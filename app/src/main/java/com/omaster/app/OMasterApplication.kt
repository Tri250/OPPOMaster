package com.omaster.app

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.os.Build
import com.omaster.app.security.AppIntegrityChecker
import com.omaster.app.security.SensitiveDataManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.security.MessageDigest

/**
 * OMaster应用类
 * SIG-SEC-003: 应用完整性验证和签名验证
 * PERM-SEC-003: 权限撤销处理
 */
@HiltAndroidApp
class OMasterApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // 初始化日志
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // 初始化敏感数据管理器
        SensitiveDataManager.initialize(this)
        
        // SIG-SEC-003: 验证应用完整性
        verifyAppIntegrity()
        
        // PERM-COL-002: 注册生命周期回调
        registerActivityLifecycleCallbacks(AppLifecycleCallback())
        
        Timber.d("OMaster Application已启动")
    }
    
    /**
     * SIG-SEC-003: 验证应用完整性
     * 检测应用是否被篡改或重签名
     */
    private fun verifyAppIntegrity() {
        try {
            // 获取应用签名
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }
            
            // 验证签名
            val isValidSignature = AppIntegrityChecker.verifySignature(packageInfo)
            
            if (!isValidSignature) {
                Timber.w("⚠️ 应用签名验证失败，可能是重签名或篡改")
                // 可以在这里添加额外的安全措施
                // 例如：禁用核心功能、提示用户等
            }
            
            // SIG-SEC-003: 验证应用完整性（检查APK未被篡改）
            val isIntegrityValid = AppIntegrityChecker.verifyAppIntegrity(this)
            
            if (!isIntegrityValid) {
                Timber.w("⚠️ 应用完整性验证失败，可能被篡改")
            }
            
            // PERM-COL-003: 检查是否为官方渠道安装
            val isFromOfficialStore = AppIntegrityChecker.isFromOfficialStore(this)
            
            if (!isFromOfficialStore) {
                Timber.w("⚠️ 应用并非从官方渠道安装")
            }
            
            Timber.d("应用完整性验证完成: 签名=$isValidSignature, 完整性=$isIntegrityValid, 官方渠道=$isFromOfficialStore")
            
        } catch (e: Exception) {
            Timber.e(e, "应用完整性验证失败")
        }
    }
    
    /**
     * 获取应用签名信息
     * SIG-SEC-003: 用于多签名验证
     */
    fun getSignatureInfo(): SignatureInfo? {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = packageInfo.signingInfo
                val signatures = signingInfo?.apkContentsSigners
                
                signatures?.firstOrNull()?.let { sig ->
                    SignatureInfo(
                        sha256 = calculateSHA256(sig.toByteArray()),
                        sha1 = calculateSHA1(sig.toByteArray()),
                        isFromPlaySigning = signingInfo.hasMultipleSigners(),
                        isSignedWithV2Scheme = signingInfo.hasMultipleSigners(),
                        isSignedWithV3Scheme = signingInfo.hasMultipleSigners()
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures?.firstOrNull()?.let { sig ->
                    SignatureInfo(
                        sha256 = calculateSHA256(sig.toByteArray()),
                        sha1 = calculateSHA1(sig.toByteArray()),
                        isFromPlaySigning = false,
                        isSignedWithV2Scheme = true,
                        isSignedWithV3Scheme = false
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "获取签名信息失败")
            null
        }
    }
    
    private fun calculateSHA256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString(":") { "%02X".format(it) }
    }
    
    private fun calculateSHA1(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val hash = digest.digest(data)
        return hash.joinToString(":") { "%02X".format(it) }
    }
    
    /**
     * 获取应用版本信息
     */
    fun getVersionInfo(): VersionInfo {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            VersionInfo(
                versionName = packageInfo.versionName ?: "1.0.0",
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                },
                isDebug = BuildConfig.DEBUG
            )
        } catch (e: Exception) {
            VersionInfo(
                versionName = "1.0.0",
                versionCode = 1,
                isDebug = BuildConfig.DEBUG
            )
        }
    }
}

/**
 * 签名信息
 * SIG-SEC-002: 多签名验证
 */
data class SignatureInfo(
    val sha256: String,              // SHA-256指纹
    val sha1: String,               // SHA-1指纹
    val isFromPlaySigning: Boolean,  // 是否来自Google Play签名
    val isSignedWithV2Scheme: Boolean,  // 是否使用V2签名
    val isSignedWithV3Scheme: Boolean   // 是否使用V3签名
)

/**
 * 版本信息
 */
data class VersionInfo(
    val versionName: String,
    val versionCode: Long,
    val isDebug: Boolean
)

/**
 * 应用生命周期回调
 * PERM-SEC-003: 权限撤销处理
 */
class AppLifecycleCallback : androidx.appcompat.app.AppCompatActivity() {
    // 这个类用于处理应用生命周期事件
}
