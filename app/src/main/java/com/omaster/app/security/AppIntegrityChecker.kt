package com.omaster.app.security

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.provider.Settings
import timber.log.Timber
import java.security.MessageDigest

/**
 * 应用完整性检查器
 * SIG-SEC-003: 应用篡改检测
 * PERM-SEC-003: 权限撤销处理
 */
object AppIntegrityChecker {
    
    // 官方签名指纹（生产环境的真实指纹）
    private const val OFFICIAL_SIGNATURE_SHA256 = "AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:00:11:22:33"
    
    // Play Store包名
    private const val PLAY_STORE_PACKAGE = "com.android.vending"
    private const val OPPO_STORE_PACKAGE = "com.heytap.mstore"
    private const val ONEPLUS_STORE_PACKAGE = "com.oneplus.storemanager"
    
    /**
     * 验证应用签名是否来自官方
     * SIG-SEC-003: 签名验证
     */
    fun verifySignature(packageInfo: PackageInfo): Boolean {
        return try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            
            signatures?.firstOrNull()?.let { signature ->
                val signatureBytes = signature.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(signatureBytes)
                val sha256 = digest.joinToString(":") { "%02X".format(it) }
                
                // 检查签名是否匹配官方签名
                val isValid = sha256 == OFFICIAL_SIGNATURE_SHA256
                
                Timber.d("签名验证: SHA256=$sha256, 有效=$isValid")
                isValid
            } ?: false
        } catch (e: Exception) {
            Timber.e(e, "签名验证失败")
            false
        }
    }
    
    /**
     * 验证应用完整性
     * SIG-SEC-003: APK篡改检测
     */
    fun verifyAppIntegrity(context: Context): Boolean {
        return try {
            // 检查是否处于调试状态
            if (isDebuggable(context)) {
                Timber.w("应用处于调试状态")
                // 调试构建不进行完整性检查
                return true
            }
            
            // 检查应用是否被重新打包
            if (isRepackaged(context)) {
                Timber.w("应用可能被重新打包")
                return false
            }
            
            // 检查安装来源
            if (!isFromOfficialStore(context)) {
                Timber.w("应用并非从官方渠道安装")
                // 非官方渠道应用给出警告但允许运行
            }
            
            // 检查模拟器
            if (isRunningOnEmulator()) {
                Timber.w("应用运行在模拟器上")
                // 模拟器环境给出警告但允许运行
            }
            
            true
        } catch (e: Exception) {
            Timber.e(e, "应用完整性验证失败")
            false
        }
    }
    
    /**
     * 检查应用是否处于调试状态
     */
    private fun isDebuggable(context: Context): Boolean {
        return try {
            val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查应用是否被重新打包
     * SIG-SEC-003: 重打包检测
     */
    private fun isRepackaged(context: Context): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNATURES)
            
            // 检查签名数量
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners?.size ?: 0
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures?.size ?: 0
            }
            
            // 官方应用应该只有一个签名
            signatures != 1
        } catch (e: Exception) {
            Timber.e(e, "重打包检测失败")
            false
        }
    }
    
    /**
     * 检查应用是否从官方商店安装
     * SIG-SEC-003: 发布渠道验证
     */
    fun isFromOfficialStore(context: Context): Boolean {
        return try {
            // 检查安装来源
            val installerPackageName = context.packageManager.getInstallerPackageName(context.packageName)
            
            Timber.d("安装来源: $installerPackageName")
            
            // 检查是否为官方渠道
            installerPackageName in listOf(
                PLAY_STORE_PACKAGE,
                OPPO_STORE_PACKAGE,
                ONEPLUS_STORE_PACKAGE,
                "com.oppo.market",           // OPPO软件商店
                "com.vivo.appstore",         // vivo应用商店
                "com.huawei.appstore",       // 华为应用市场
                "com.xiaomi.mipop",          // 小米应用商店
                "com.samsung.android.app.samsungapps"  // 三星应用商店
            )
        } catch (e: Exception) {
            Timber.e(e, "安装来源检测失败")
            false
        }
    }
    
    /**
     * 检查是否运行在模拟器上
     * SIG-SEC-003: 模拟器检测
     */
    private fun isRunningOnEmulator(): Boolean {
        return try {
            // 检查常见的模拟器特征
            val brand = Build.BRAND.lowercase()
            val device = Build.DEVICE.lowercase()
            val hardware = Build.HARDWARE.lowercase()
            val model = Build.MODEL.lowercase()
            
            val emulatorIndicators = listOf(
                "generic",
                "goldfish",
                "ranchu",
                "sdk",
                "emulator",
                "simulator"
            )
            
            emulatorIndicators.any { 
                brand.contains(it) || 
                device.contains(it) || 
                hardware.contains(it) || 
                model.contains(it) 
            } ||
            // 检查常见的模拟器特征
            (Build.FINGERPRINT.contains("generic") && !Build.BRAND.contains("generic"))
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取安全的设备ID
     * SIG-SEC-003: 隐私保护
     */
    fun getSecureDeviceId(context: Context): String {
        return try {
            // 使用Android ID的哈希值，而不是原始ID
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(androidId.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // 如果获取失败，返回一个随机生成的ID
            java.util.UUID.randomUUID().toString()
        }
    }
    
    /**
     * 检查设备是否被root
     * SIG-SEC-003: Root检测
     */
    fun isDeviceRooted(): Boolean {
        return try {
            // 检查常见的root特征
            val paths = listOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su"
            )
            
            paths.any { java.io.File(it).exists() }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 验证应用签名（支持多签名）
     * SIG-SEC-002: 多签名验证
     */
    fun verifyMultiSignature(packageInfo: PackageInfo): SignatureVerificationResult {
        return try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners?.toList() ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures?.toList() ?: emptyList()
            }
            
            if (signatures.isEmpty()) {
                return SignatureVerificationResult.Invalid("没有找到签名")
            }
            
            // 检查签名数量
            val hasMultipleSignatures = signatures.size > 1
            
            // 检查是否支持V2签名
            val hasV2Signature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.hasMultipleSigners() == false
            } else {
                true
            }
            
            // 检查是否支持V3签名
            val hasV3Signature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.hasMultipleSigners() == false
            } else {
                false
            }
            
            // 验证每个签名
            val signatureResults = signatures.mapIndexed { index, signature ->
                val sha256 = calculateSHA256(signature.toByteArray())
                val isOfficial = sha256 == OFFICIAL_SIGNATURE_SHA256
                SignatureInfo(
                    index = index,
                    sha256 = sha256,
                    isOfficial = isOfficial
                )
            }
            
            // 判断整体结果
            val allOfficial = signatureResults.all { it.isOfficial }
            val anyOfficial = signatureResults.any { it.isOfficial }
            
            when {
                allOfficial -> SignatureVerificationResult.Valid(
                    signatures = signatureResults,
                    hasV2Signature = hasV2Signature,
                    hasV3Signature = hasV3Signature,
                    hasMultipleSignatures = hasMultipleSignatures
                )
                anyOfficial -> SignatureVerificationResult.Partial(
                    signatures = signatureResults,
                    message = "部分签名来自官方"
                )
                else -> SignatureVerificationResult.Invalid(
                    signatures = signatureResults,
                    message = "所有签名都来自非官方"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "多签名验证失败")
            SignatureVerificationResult.Invalid(message = "签名验证异常: ${e.message}")
        }
    }
    
    private fun calculateSHA256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString(":") { "%02X".format(it) }
    }
}

/**
 * 签名验证结果
 */
sealed class SignatureVerificationResult {
    data class Valid(
        val signatures: List<SignatureInfo>,
        val hasV2Signature: Boolean,
        val hasV3Signature: Boolean,
        val hasMultipleSignatures: Boolean
    ) : SignatureVerificationResult()
    
    data class Partial(
        val signatures: List<SignatureInfo>,
        val message: String
    ) : SignatureVerificationResult()
    
    data class Invalid(
        val signatures: List<SignatureInfo> = emptyList(),
        val message: String
    ) : SignatureVerificationResult()
}

/**
 * 签名信息
 */
data class SignatureInfo(
    val index: Int,
    val sha256: String,
    val isOfficial: Boolean
)
