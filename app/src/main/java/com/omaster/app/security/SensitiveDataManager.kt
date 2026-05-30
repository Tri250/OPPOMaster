package com.omaster.app.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets.UTF_8

/**
 * 敏感数据加密管理器
 * BLD-SEC-004: 对敏感字符串和API进行AES-256加密
 */
object SensitiveDataManager {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128
    
    private var secretKey: SecretKey? = null
    
    /**
     * 初始化加密管理器
     * BLD-SEC-004: 从安全存储加载或生成密钥
     */
    fun initialize(context: Context) {
        if (secretKey == null) {
            secretKey = getOrCreateKey(context)
        }
    }
    
    /**
     * 获取或创建加密密钥
     * BLD-SEC-004: 使用Android Keystore安全存储密钥
     */
    private fun getOrCreateKey(context: Context): SecretKey {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        // 使用EncryptedSharedPreferences存储密钥信息
        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            "secure_keys_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        val keyString = encryptedPrefs.getString("encryption_key", null)
        
        return if (keyString != null) {
            // 从安全存储加载密钥
            val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
            SecretKeySpec(keyBytes, "AES")
        } else {
            // 生成新密钥
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(KEY_SIZE, SecureRandom())
            val newKey = keyGenerator.generateKey()
            
            // 安全存储密钥
            val keyBytes = newKey.encoded
            encryptedPrefs.edit()
                .putString("encryption_key", Base64.encodeToString(keyBytes, Base64.DEFAULT))
                .apply()
            
            newKey
        }
    }
    
    /**
     * 加密敏感数据
     * BLD-SEC-004: 使用AES-256-GCM加密
     */
    fun encrypt(plainText: String): String {
        val key = secretKey ?: throw IllegalStateException("敏感数据管理器未初始化")
        
        // 生成随机IV
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)
        
        // 加密
        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
        
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(UTF_8))
        
        // 组合IV和加密数据
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
        
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }
    
    /**
     * 解密敏感数据
     * BLD-SEC-004: 使用AES-256-GCM解密
     */
    fun decrypt(encryptedText: String): String {
        val key = secretKey ?: throw IllegalStateException("敏感数据管理器未初始化")
        
        val combined = Base64.decode(encryptedText, Base64.DEFAULT)
        
        // 分离IV和加密数据
        val iv = ByteArray(IV_SIZE)
        val encryptedBytes = ByteArray(combined.size - IV_SIZE)
        System.arraycopy(combined, 0, iv, 0, IV_SIZE)
        System.arraycopy(combined, IV_SIZE, encryptedBytes, 0, encryptedBytes.size)
        
        // 解密
        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
        
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, UTF_8)
    }
    
    /**
     * 加密API密钥
     * BLD-SEC-004: API密钥使用AES-256加密存储
     */
    fun encryptApiKey(apiKey: String): String {
        return encrypt(apiKey)
    }
    
    /**
     * 解密API密钥
     * BLD-SEC-004: 安全解密API密钥
     */
    fun decryptApiKey(encryptedApiKey: String): String {
        return decrypt(encryptedApiKey)
    }
    
    /**
     * 加密敏感配置
     * BLD-SEC-004: 加密存储敏感配置信息
     */
    fun encryptSensitiveConfig(config: Map<String, String>): String {
        val configString = config.entries.joinToString("&") { "${it.key}=${it.value}" }
        return encrypt(configString)
    }
    
    /**
     * 解密敏感配置
     * BLD-SEC-004: 解密敏感配置信息
     */
    fun decryptSensitiveConfig(encryptedConfig: String): Map<String, String> {
        val configString = decrypt(encryptedConfig)
        return configString.split("&").associate {
            val parts = it.split("=")
            parts[0] to parts.getOrElse(1) { "" }
        }
    }
    
    /**
     * 安全清除密钥
     * BLD-SEC-004: 安全清理敏感数据
     */
    fun clearKeys() {
        secretKey = null
    }
}

/**
 * API密钥配置
 * BLD-SEC-004: 加密存储API密钥
 */
object ApiKeyConfig {
    // TODO: 替换为实际的API密钥加密存储实现
    // 示例：使用SensitiveDataManager进行加密存储
    
    private const val ENCRYPTED_API_KEY_PREF = "encrypted_api_key"
    
    /**
     * 保存加密的API密钥
     */
    fun saveEncryptedApiKey(context: Context, apiKey: String) {
        val encrypted = SensitiveDataManager.encryptApiKey(apiKey)
        context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString(ENCRYPTED_API_KEY_PREF, encrypted)
            .apply()
    }
    
    /**
     * 获取解密的API密钥
     */
    fun getDecryptedApiKey(context: Context): String? {
        val prefs = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
        val encrypted = prefs.getString(ENCRYPTED_API_KEY_PREF, null) ?: return null
        
        return try {
            SensitiveDataManager.decryptApiKey(encrypted)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 清除API密钥
     */
    fun clearApiKey(context: Context) {
        context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove(ENCRYPTED_API_KEY_PREF)
            .apply()
    }
}

/**
 * 敏感字符串常量
 * BLD-SEC-004: 运行时解密敏感字符串
 */
object SensitiveStrings {
    // TODO: 实现运行时解密敏感字符串
    // 注意：所有敏感字符串应在运行时从加密存储解密，不应硬编码在代码中
    
    /**
     * 获取解密后的API Base URL
     */
    fun getApiBaseUrl(): String {
        // TODO: 从加密存储获取并解密
        return "https://api.omaster.app"
    }
    
    /**
     * 获取解密后的API密钥
     */
    fun getApiKey(): String? {
        // TODO: 从加密存储获取并解密
        return null
    }
}
