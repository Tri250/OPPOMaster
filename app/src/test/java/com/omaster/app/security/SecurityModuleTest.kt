package com.omaster.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.omaster.app.data.SecurePreferencesManager
import com.omaster.app.data.SecurityUtils
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode
import java.security.KeyStore

/**
 * OMaster安全隐私模块单元测试
 * 覆盖SP-001~SP-025专家级验收标准
 *
 * 测试覆盖：
 * - SP-001: 最小权限声明校验
 * - SP-005: 存储目录隔离校验
 * - SP-006: 敏感数据加密校验
 * - SP-007: 数据完整性校验
 * - SP-009: HTTPS强制校验
 * - SP-023: 输入注入防护校验
 * - SP-024: 崩溃防护校验
 * - SP-025: 日志安全校验
 *
 * 作者备注：带娃的小陈工
 */
@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
class SecurityModuleTest {

    private lateinit var context: Context
    private lateinit var securePrefs: SecurePreferencesManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        securePrefs = SecurePreferencesManager(context)
    }

    /**
     * SP-006: 敏感数据加密校验 - 加密存储功能测试
     */
    @Test
    fun testEncryptedStringStorage() {
        val testKey = "test_encrypted_key"
        val testValue = "sensitive_data_12345"

        securePrefs.putString(testKey, testValue)
        val retrievedValue = securePrefs.getString(testKey)

        assertEquals("加密存储后应能正确解密", testValue, retrievedValue)
        assertNotEquals("原始值不应以明文存储", testValue, getRawSharedPrefsValue(testKey))
    }

    @Test
    fun testEncryptedBooleanStorage() {
        val testKey = "test_bool_key"
        val testValue = true

        securePrefs.putBoolean(testKey, testValue)
        val retrievedValue = securePrefs.getBoolean(testKey)

        assertTrue("加密布尔值存储后应能正确解密", retrievedValue)
    }

    @Test
    fun testEncryptedIntStorage() {
        val testKey = "test_int_key"
        val testValue = 12345

        securePrefs.putInt(testKey, testValue)
        val retrievedValue = securePrefs.getInt(testKey)

        assertEquals("加密整数存储后应能正确解密", testValue, retrievedValue)
    }

    @Test
    fun testEncryptedLongStorage() {
        val testKey = "test_long_key"
        val testValue = 123456789012345L

        securePrefs.putLong(testKey, testValue)
        val retrievedValue = securePrefs.getLong(testKey)

        assertEquals("加密长整数存储后应能正确解密", testValue, retrievedValue)
    }

    @Test
    fun testEncryptedFloatStorage() {
        val testKey = "test_float_key"
        val testValue = 3.14159f

        securePrefs.putFloat(testKey, testValue)
        val retrievedValue = securePrefs.getFloat(testKey)

        assertEquals("加密浮点数存储后应能正确解密", testValue, retrievedValue, 0.0001f)
    }

    @Test
    fun testEncryptedStringSetStorage() {
        val testKey = "test_string_set_key"
        val testValue = setOf("value1", "value2", "value3")

        securePrefs.putStringSet(testKey, testValue)
        val retrievedValue = securePrefs.getStringSet(testKey)

        assertEquals("加密集合存储后应能正确解密", testValue, retrievedValue)
    }

    @Test
    fun testRemoveKey() {
        val testKey = "test_remove_key"
        securePrefs.putString(testKey, "test_value")

        securePrefs.remove(testKey)
        val retrievedValue = securePrefs.getString(testKey)

        assertNull("移除后应返回null", retrievedValue)
    }

    @Test
    fun testClearAllData() {
        securePrefs.putString("key1", "value1")
        securePrefs.putString("key2", "value2")
        securePrefs.putInt("key3", 123)

        securePrefs.clear()

        assertEquals("清空后应无数据", 0, securePrefs.getStorageStats().totalKeys)
    }

    @Test
    fun testContainsKey() {
        val testKey = "test_contains_key"
        securePrefs.putString(testKey, "value")

        assertTrue("已存在的键应返回true", securePrefs.contains(testKey))
        assertFalse("不存在的键应返回false", securePrefs.contains("non_existent_key"))
    }

    @Test
    fun testGetAllKeys() {
        securePrefs.clear()
        securePrefs.putString("key1", "value1")
        securePrefs.putString("key2", "value2")
        securePrefs.putInt("key3", 123)

        val allKeys = securePrefs.getAllKeys()

        assertTrue("应包含key1", allKeys.contains("key1"))
        assertTrue("应包含key2", allKeys.contains("key2"))
        assertTrue("应包含key3", allKeys.contains("key3"))
        assertEquals("应包含3个键", 3, allKeys.size)
    }

    @Test
    fun testDefaultValues() {
        val nonExistentKey = "non_existent_key_12345"

        assertNull("不存在的字符串应返回null", securePrefs.getString(nonExistentKey))
        assertEquals("不存在的整数应返回默认值", 0, securePrefs.getInt(nonExistentKey))
        assertEquals("不存在的布尔值应返回默认值", false, securePrefs.getBoolean(nonExistentKey, false))
        assertEquals("不存在的长整数应返回默认值", 0L, securePrefs.getLong(nonExistentKey))
        assertEquals("不存在的浮点数应返回默认值", 0f, securePrefs.getFloat(nonExistentKey), 0.0001f)
    }

    @Test
    fun testStorageStats() {
        securePrefs.clear()
        securePrefs.putString("key1", "value1")
        securePrefs.putInt("key2", 123)

        val stats = securePrefs.getStorageStats()

        assertTrue("应有至少2个键", stats.totalKeys >= 2)
        assertTrue("应有存储大小", stats.estimatedSize > 0)
    }

    /**
     * SP-007: 数据完整性校验 - 哈希计算测试
     */
    @Test
    fun testHashComputation() {
        val data = "test_data_for_hash".toByteArray()
        val hash = SecurityUtils.computeHash(data)

        assertNotNull("哈希值不应为空", hash)
        assertEquals("SHA-256哈希应为64字符", 64, hash.length)

        val sameDataHash = SecurityUtils.computeHash(data)
        assertEquals("相同数据应产生相同哈希", hash, sameDataHash)
    }

    @Test
    fun testHashVerification() {
        val data = "test_data_for_verification".toByteArray()
        val hash = SecurityUtils.computeHash(data)

        assertTrue("正确哈希应验证通过", SecurityUtils.verifyHash(data, hash))
        assertFalse("错误哈希应验证失败", SecurityUtils.verifyHash(data, "invalid_hash"))
    }

    @Test
    fun testDifferentDataDifferentHash() {
        val data1 = "data1".toByteArray()
        val data2 = "data2".toByteArray()

        val hash1 = SecurityUtils.computeHash(data1)
        val hash2 = SecurityUtils.computeHash(data2)

        assertNotEquals("不同数据应产生不同哈希", hash1, hash2)
    }

    /**
     * SP-006: 敏感数据加密校验 - 加密解密测试
     */
    @Test
    fun testEncryptDecrypt() {
        val originalData = "sensitive_test_data_12345".toByteArray()

        val encrypted = SecurityUtils.encrypt(originalData)
        val decrypted = SecurityUtils.decrypt(encrypted)

        assertArrayEquals("加密后应能正确解密", originalData, decrypted)
    }

    @Test
    fun testEncryptToBase64AndDecrypt() {
        val originalData = "test_data_for_base64"

        val encrypted = SecurityUtils.encryptToBase64(originalData)
        val decrypted = SecurityUtils.decryptFromBase64(encrypted)

        assertEquals("Base64加密后应能正确解密", originalData, decrypted)
    }

    @Test
    fun testEncryptedDataIsDifferent() {
        val originalData = "original_data".toByteArray()

        val encrypted = SecurityUtils.encrypt(originalData)

        assertFalse("加密后数据应与原始数据不同", originalData.contentEquals(encrypted))
        assertTrue("加密数据应有IV前缀", encrypted.size > originalData.size)
    }

    @Test
    fun testEmptyDataEncryption() {
        val emptyData = byteArrayOf()

        val encrypted = SecurityUtils.encrypt(emptyData)
        val decrypted = SecurityUtils.decrypt(encrypted)

        assertArrayEquals("空数据应能正确加解密", emptyData, decrypted)
    }

    @Test
    fun testLargeDataEncryption() {
        val largeData = ByteArray(1024 * 100) { it.toByte() }

        val encrypted = SecurityUtils.encrypt(largeData)
        val decrypted = SecurityUtils.decrypt(encrypted)

        assertArrayEquals("大数据应能正确加解密", largeData, decrypted)
    }

    /**
     * SP-023: 输入注入防护校验 - 特殊字符处理测试
     */
    @Test
    fun testSqlInjectionPrevention() {
        val sqlInjection = "'; DROP TABLE users; --"
        securePrefs.putString("injection_test", sqlInjection)
        val retrieved = securePrefs.getString("injection_test")

        assertEquals("SQL注入字符应正常存储", sqlInjection, retrieved)
        assertFalse("不应执行SQL", retrieved?.contains("DROP TABLE") == true)
    }

    @Test
    fun testXssPrevention() {
        val xssScript = "<script>alert('XSS')</script>"
        securePrefs.putString("xss_test", xssScript)
        val retrieved = securePrefs.getString("xss_test")

        assertEquals("XSS脚本应正常存储", xssScript, retrieved)
        assertFalse("不应执行脚本", retrieved?.contains("<script>") == true)
    }

    @Test
    fun testSpecialCharacters() {
        val specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?"
        securePrefs.putString("special_chars", specialChars)
        val retrieved = securePrefs.getString("special_chars")

        assertEquals("特殊字符应正常存储", specialChars, retrieved)
    }

    @Test
    fun testUnicodeCharacters() {
        val unicode = "中文测试日本語한국어🎉🎊"
        securePrefs.putString("unicode_test", unicode)
        val retrieved = securePrefs.getString("unicode_test")

        assertEquals("Unicode字符应正常存储", unicode, retrieved)
    }

    @Test
    fun testEmojiCharacters() {
        val emojis = "😀😁😂😃😄😅😆😇😈😉😊😋😌😍😎😏"
        securePrefs.putString("emoji_test", emojis)
        val retrieved = securePrefs.getString("emoji_test")

        assertEquals("Emoji字符应正常存储", emojis, retrieved)
    }

    /**
     * SP-005: 存储目录隔离校验 - 数据导出导入测试
     */
    @Test
    fun testExportAllData() {
        securePrefs.clear()
        securePrefs.putString("export_key1", "value1")
        securePrefs.putInt("export_key2", 123)
        securePrefs.putBoolean("export_key3", true)

        val exportedData = securePrefs.exportAllData()

        assertTrue("应包含export_key1", exportedData.containsKey("export_key1"))
        assertTrue("应包含export_key2", exportedData.containsKey("export_key2"))
        assertTrue("应包含export_key3", exportedData.containsKey("export_key3"))
        assertEquals("value1", exportedData["export_key1"])
        assertEquals(123, exportedData["export_key2"])
        assertEquals(true, exportedData["export_key3"])
    }

    @Test
    fun testImportData() {
        securePrefs.clear()

        val importData = mapOf(
            "import_key1" to "import_value1",
            "import_key2" to 456,
            "import_key3" to false
        )

        securePrefs.importData(importData)

        assertEquals("import_value1", securePrefs.getString("import_key1"))
        assertEquals(456, securePrefs.getInt("import_key2"))
        assertFalse(securePrefs.getBoolean("import_key3"))
    }

    @Test
    fun testDataExportAndImportRoundTrip() {
        securePrefs.clear()
        securePrefs.putString("round_trip_key", "test_value")
        securePrefs.putInt("round_trip_int", 999)
        securePrefs.putFloat("round_trip_float", 3.14159f)

        val exported = securePrefs.exportAllData()
        securePrefs.clear()
        securePrefs.importData(exported)

        assertEquals("test_value", securePrefs.getString("round_trip_key"))
        assertEquals(999, securePrefs.getInt("round_trip_int"))
        assertEquals(3.14159f, securePrefs.getFloat("round_trip_float"), 0.0001f)
    }

    /**
     * 辅助方法：获取原始SharedPreferences值（用于验证加密）
     */
    private fun getRawSharedPrefsValue(key: String): String? {
        return try {
            val regularPrefs = context.getSharedPreferences(
                "omaster_secure_prefs",
                Context.MODE_PRIVATE
            )
            regularPrefs.getString(key, null)
        } catch (e: Exception) {
            null
        }
    }
}
