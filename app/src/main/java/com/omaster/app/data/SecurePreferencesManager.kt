package com.omaster.app.data

import android.content.Context
import android.content.SharedPreferences
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
import android.util.Base64

/**
 * OMaster安全存储管理器
 * 遵循SP-006敏感数据加密校验标准
 *
 * 实现功能：
 * 1. 使用AES-256-GCM加密算法
 * 2. 密钥存储在Android Keystore中
 * 3. 支持加密SharedPreferences
 * 4. 防止敏感数据明文存储
 *
 * 作者备注：带娃的小陈工
 */
@Singleton
class SecurePreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEYSTORE_ALIAS = "omaster_master_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val PREFS_FILE_NAME = "omaster_secure_prefs"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create encrypted shared preferences")
            throw SecurityException("无法创建加密存储，请检查设备安全性设置")
        }
    }

    /**
     * 保存加密字符串
     */
    fun putString(key: String, value: String) {
        try {
            encryptedPrefs.edit().putString(key, value).apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save encrypted string for key: $key")
        }
    }

    /**
     * 获取加密字符串
     */
    fun getString(key: String, defaultValue: String? = null): String? {
        return try {
            encryptedPrefs.getString(key, defaultValue)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get encrypted string for key: $key")
            defaultValue
        }
    }

    /**
     * 保存加密布尔值
     */
    fun putBoolean(key: String, value: Boolean) {
        try {
            encryptedPrefs.edit().putBoolean(key, value).apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save encrypted boolean for key: $key")
        }
    }

    /**
     * 获取加密布尔值
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return try {
            encryptedPrefs.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get encrypted boolean for key: $key")
            defaultValue
        }
    }

    /**
     * 保存加密整数
     */
    fun putInt(key: String, value: Int) {
        try {
            encryptedPrefs.edit().putInt(key, value).apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save encrypted int for key: $key")
        }
    }

    /**
     * 获取加密整数
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return try {
            encryptedPrefs.getInt(key, defaultValue)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get encrypted int for key: $key")
            defaultValue
        }
    }

    /**
     * 保存加密长整数
     */
    fun putLong(key: String, value: Long) {
        try {
            encryptedPrefs.edit().putLong(key, value).apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save encrypted long for key: $key")
        }
    }

    /**
     * 获取加密长整数
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return try {
            encryptedPrefs.getLong(key, defaultValue)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get encrypted long for key: $key")
            defaultValue
        }
    }

    /**
     * 保存加密浮点数
     */
    fun putFloat(key: String, value: Float) {
        try {
            encryptedPrefs.edit().putFloat(key, value).apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save encrypted float for key: $key")
        }
    }

    /**
     * 获取加密浮点数
     */
    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return try {
            encryptedPrefs.getFloat(key, defaultValue)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get encrypted float for key: $key")
            defaultValue
        }
    }

    /**
     * 保存加密字符串集合
     */
    fun putStringSet(key: String, value: Set<String>) {
        try {
            encryptedPrefs.edit().putStringSet(key, value).apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save encrypted string set for key: $key")
        }
    }

    /**
     * 获取加密字符串集合
     */
    fun getStringSet(key: String, defaultValue: Set<String>? = null): Set<String>? {
        return try {
            encryptedPrefs.getStringSet(key, defaultValue)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get encrypted string set for key: $key")
            defaultValue
        }
    }

    /**
     * 移除指定键值
     */
    fun remove(key: String) {
        try {
            encryptedPrefs.edit().remove(key).apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove key: $key")
        }
    }

    /**
     * 清空所有加密数据
     */
    fun clear() {
        try {
            encryptedPrefs.edit().clear().apply()
            Timber.d("Secure preferences cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear secure preferences")
        }
    }

    /**
     * 检查是否存在指定键
     */
    fun contains(key: String): Boolean {
        return try {
            encryptedPrefs.contains(key)
        } catch (e: Exception) {
            Timber.e(e, "Failed to check key existence: $key")
            false
        }
    }

    /**
     * 获取所有键
     */
    fun getAllKeys(): Set<String> {
        return try {
            encryptedPrefs.all.keys
        } catch (e: Exception) {
            Timber.e(e, "Failed to get all keys")
            emptySet()
        }
    }

    /**
     * 注册监听器
     */
    fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        encryptedPrefs.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * 注销监听器
     */
    fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        encryptedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    /**
     * 获取存储统计信息
     */
    fun getStorageStats(): StorageStats {
        return try {
            val allData = encryptedPrefs.all
            StorageStats(
                totalKeys = allData.size,
                estimatedSize = estimateStorageSize(allData)
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get storage stats")
            StorageStats(0, 0)
        }
    }

    private fun estimateStorageSize(data: Map<String, *>): Long {
        return data.entries.sumOf { (key, value) ->
            key.toByteArray().size + (value?.toString()?.toByteArray()?.size ?: 0)
        }.toLong()
    }

    /**
     * 导出所有数据（用于用户数据导出功能SP-017）
     */
    fun exportAllData(): Map<String, *> {
        return try {
            encryptedPrefs.all.toMap()
        } catch (e: Exception) {
            Timber.e(e, "Failed to export data")
            emptyMap()
        }
    }

    /**
     * 导入数据（用于用户数据恢复功能SP-017）
     */
    fun importData(data: Map<String, *>) {
        try {
            val editor = encryptedPrefs.edit()
            data.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
            editor.apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to import data")
            throw SecurityException("数据导入失败")
        }
    }
}

/**
 * 存储统计信息
 */
data class StorageStats(
    val totalKeys: Int,
    val estimatedSize: Long
)

/**
 * OMaster安全加密工具类
 * 提供AES-256-GCM加密解密功能
 */
object SecurityUtils {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "omaster_data_encryption_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    /**
     * 生成加密密钥（存储在Android Keystore）
     */
    fun generateKey(): SecretKey {
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
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * 获取加密密钥
     */
    fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    /**
     * 加密数据
     */
    fun encrypt(data: ByteArray, key: SecretKey = getKey()): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)

        return iv + encryptedData
    }

    /**
     * 解密数据
     */
    fun decrypt(encryptedData: ByteArray, key: SecretKey = getKey()): ByteArray {
        val iv = encryptedData.copyOfRange(0, GCM_IV_LENGTH)
        val data = encryptedData.copyOfRange(GCM_IV_LENGTH, encryptedData.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        return cipher.doFinal(data)
    }

    /**
     * Base64编码加密
     */
    fun encryptToBase64(data: String): String {
        val encrypted = encrypt(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    /**
     * Base64解码解密
     */
    fun decryptFromBase64(encryptedBase64: String): String {
        val encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val decrypted = decrypt(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }

    /**
     * 计算数据哈希（用于数据完整性校验SP-007）
     */
    fun computeHash(data: ByteArray): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * 验证数据哈希（用于数据完整性校验SP-007）
     */
    fun verifyHash(data: ByteArray, expectedHash: String): Boolean {
        val actualHash = computeHash(data)
        return actualHash.equals(expectedHash, ignoreCase = true)
    }
}
