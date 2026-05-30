package com.omaster.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地数据加密管理器
 * DATA-STO-001: 本地数据加密
 * 使用AES-256-GCM加密，密钥由Android Keystore系统管理
 */
@Singleton
class LocalDataEncryption @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "omaster_data_encryption_key"
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256
        private const val IV_SIZE = 12
        private const val TAG_SIZE = 128
    }
    
    private var secretKey: SecretKey? = null
    
    init {
        initialize()
    }
    
    /**
     * 初始化加密管理器
     * DATA-STO-001: 从Android Keystore获取或生成密钥
     */
    private fun initialize() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            
            // 检查密钥是否存在
            if (keyStore.containsAlias(KEY_ALIAS)) {
                // 获取现有密钥
                val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
                secretKey = entry.secretKey
                Timber.d("使用现有加密密钥")
            } else {
                // 生成新密钥
                secretKey = generateKey()
                Timber.d("生成新加密密钥")
            }
        } catch (e: Exception) {
            Timber.e(e, "初始化加密管理器失败")
            // 如果Keystore失败，生成临时密钥（不推荐用于生产环境）
            secretKey = generateTemporaryKey()
        }
    }
    
    /**
     * 生成密钥（存储在Android Keystore）
     * DATA-STO-001: 密钥无法被导出或提取
     */
    private fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false) // 根据需要启用
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
    
    /**
     * 生成临时密钥（仅在Keystore不可用时使用）
     * 注意：这不如使用Keystore安全，仅作为后备方案
     */
    private fun generateTemporaryKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(KEY_SIZE, java.security.SecureRandom())
        return keyGenerator.generateKey()
    }
    
    /**
     * 加密数据
     * DATA-STO-001: 使用AES-256-GCM加密
     */
    fun encrypt(data: ByteArray): ByteArray {
        val key = secretKey ?: throw IllegalStateException("加密密钥未初始化")
        
        // 生成随机IV
        val iv = ByteArray(IV_SIZE)
        java.security.SecureRandom().nextBytes(iv)
        
        // 加密
        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
        
        val encryptedBytes = cipher.doFinal(data)
        
        // 组合IV和加密数据
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
        
        return combined
    }
    
    /**
     * 加密字符串
     */
    fun encryptString(plainText: String): ByteArray {
        return encrypt(plainText.toByteArray(Charsets.UTF_8))
    }
    
    /**
     * 解密数据
     */
    fun decrypt(encryptedData: ByteArray): ByteArray {
        val key = secretKey ?: throw IllegalStateException("加密密钥未初始化")
        
        // 分离IV和加密数据
        val iv = ByteArray(IV_SIZE)
        val encryptedBytes = ByteArray(encryptedData.size - IV_SIZE)
        System.arraycopy(encryptedData, 0, iv, 0, IV_SIZE)
        System.arraycopy(encryptedData, IV_SIZE, encryptedBytes, 0, encryptedBytes.size)
        
        // 解密
        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
        
        return cipher.doFinal(encryptedBytes)
    }
    
    /**
     * 解密为字符串
     */
    fun decryptToString(encryptedData: ByteArray): String {
        return String(decrypt(encryptedData), Charsets.UTF_8)
    }
    
    /**
     * 加密并Base64编码
     * DATA-STO-001: 用于存储加密数据
     */
    fun encryptToBase64(plainText: String): String {
        val encrypted = encryptString(plainText)
        return android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT)
    }
    
    /**
     * Base64解码并解密
     */
    fun decryptFromBase64(encryptedBase64: String): String {
        val encrypted = android.util.Base64.decode(encryptedBase64, android.util.Base64.DEFAULT)
        return decryptToString(encrypted)
    }
    
    /**
     * 检查密钥是否来自Keystore
     * DATA-STO-001: 验证密钥安全性
     */
    fun isKeyFromKeystore(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.containsAlias(KEY_ALIAS)
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * 安全存储管理器
 * DATA-STO-001: 使用EncryptedSharedPreferences存储敏感数据
 */
@Singleton
class SecureStorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryption: LocalDataEncryption
) {
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * 保存加密数据
     */
    fun putEncryptedString(key: String, value: String) {
        encryptedPrefs.edit()
            .putString(key, encryption.encryptToBase64(value))
            .apply()
    }
    
    /**
     * 获取解密数据
     */
    fun getEncryptedString(key: String): String? {
        val encrypted = encryptedPrefs.getString(key, null) ?: return null
        return try {
            encryption.decryptFromBase64(encrypted)
        } catch (e: Exception) {
            Timber.e(e, "解密失败: $key")
            null
        }
    }
    
    /**
     * 保存预设数据（加密）
     */
    fun savePreset(key: String, presetJson: String) {
        putEncryptedString("preset_$key", presetJson)
    }
    
    /**
     * 获取预设数据（解密）
     */
    fun getPreset(key: String): String? {
        return getEncryptedString("preset_$key")
    }
    
    /**
     * 删除加密数据
     */
    fun remove(key: String) {
        encryptedPrefs.edit()
            .remove(key)
            .apply()
    }
    
    /**
     * 清空所有加密数据
     * DATA-STO-001: 应用卸载后所有加密数据自动删除
     */
    fun clear() {
        encryptedPrefs.edit()
            .clear()
            .apply()
    }
}

/**
 * 文件加密工具
 * DATA-STO-001: 加密存储文件
 */
@Singleton
class FileEncryptionManager @Inject constructor(
    private val encryption: LocalDataEncryption
) {
    /**
     * 加密文件
     * DATA-STO-001: 使用AES-256加密文件
     */
    fun encryptFile(inputFile: java.io.File, outputFile: java.io.File) {
        try {
            val plainData = inputFile.readBytes()
            val encryptedData = encryption.encrypt(plainData)
            outputFile.writeBytes(encryptedData)
            Timber.d("文件加密成功: ${inputFile.name}")
        } catch (e: Exception) {
            Timber.e(e, "文件加密失败: ${inputFile.name}")
            throw e
        }
    }
    
    /**
     * 解密文件
     */
    fun decryptFile(encryptedFile: java.io.File, outputFile: java.io.File) {
        try {
            val encryptedData = encryptedFile.readBytes()
            val plainData = encryption.decrypt(encryptedData)
            outputFile.writeBytes(plainData)
            Timber.d("文件解密成功: ${encryptedFile.name}")
        } catch (e: Exception) {
            Timber.e(e, "文件解密失败: ${encryptedFile.name}")
            throw e
        }
    }
    
    /**
     * 生成文件校验和
     * DATA-STO-002: 防止文件被篡改
     */
    fun generateChecksum(file: java.io.File): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(file.readBytes())
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 验证文件完整性
     * DATA-STO-002: 验证文件是否被篡改
     */
    fun verifyChecksum(file: java.io.File, expectedChecksum: String): Boolean {
        val actualChecksum = generateChecksum(file)
        return actualChecksum == expectedChecksum
    }
}

/**
 * 缓存数据管理器
 * DATA-STO-003: 管理缓存数据，确保不包含敏感信息
 */
@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 获取缓存目录大小
     * DATA-STO-003: 用于显示缓存占用
     */
    fun getCacheSize(): Long {
        return calculateDirectorySize(context.cacheDir)
    }
    
    /**
     * 清理缓存
     * DATA-STO-003: 清理所有缓存数据
     */
    fun clearCache() {
        try {
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            }
            Timber.d("缓存已清理")
        } catch (e: Exception) {
            Timber.e(e, "清理缓存失败")
        }
    }
    
    /**
     * 清理外部缓存
     */
    fun clearExternalCache() {
        try {
            context.externalCacheDir?.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            }
            Timber.d("外部缓存已清理")
        } catch (e: Exception) {
            Timber.e(e, "清理外部缓存失败")
        }
    }
    
    private fun calculateDirectorySize(directory: java.io.File): Long {
        var size = 0L
        directory.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }
        return size
    }
}
