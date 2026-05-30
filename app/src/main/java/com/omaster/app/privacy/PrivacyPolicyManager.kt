package com.omaster.app.privacy

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 隐私政策管理器
 * DATA-PRV-001: 隐私政策管理和同意追踪
 */
@Singleton
class PrivacyPolicyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "privacy_prefs")
    
    companion object {
        private val PRIVACY_POLICY_ACCEPTED = booleanPreferencesKey("privacy_policy_accepted")
        private val PRIVACY_POLICY_ACCEPTED_VERSION = stringPreferencesKey("privacy_policy_accepted_version")
        private val DATA_COLLECTION_ENABLED = booleanPreferencesKey("data_collection_enabled")
        private val LAST_UPDATED = longPreferencesKey("last_updated")
        
        // 当前隐私政策版本
        const val CURRENT_VERSION = "1.2.1"
    }
    
    /**
     * 检查隐私政策是否已接受
     * DATA-PRV-001: 首次启动时需要显示隐私政策
     */
    val isPrivacyPolicyAccepted: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val accepted = preferences[PRIVACY_POLICY_ACCEPTED] ?: false
            val version = preferences[PRIVACY_POLICY_ACCEPTED_VERSION] ?: ""
            Timber.d("隐私政策状态: accepted=$accepted, version=$version")
            accepted && version == CURRENT_VERSION
        }
    
    /**
     * 检查数据收集是否启用
     * DATA-PRV-003: 用户可以关闭数据收集
     */
    val isDataCollectionEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[DATA_COLLECTION_ENABLED] ?: true // 默认为启用
        }
    
    /**
     * 接受隐私政策
     * DATA-PRV-001: 用户同意隐私政策
     */
    suspend fun acceptPrivacyPolicy() {
        try {
            context.dataStore.edit { preferences ->
                preferences[PRIVACY_POLICY_ACCEPTED] = true
                preferences[PRIVACY_POLICY_ACCEPTED_VERSION] = CURRENT_VERSION
                preferences[LAST_UPDATED] = System.currentTimeMillis()
                preferences[DATA_COLLECTION_ENABLED] = true
            }
            Timber.d("隐私政策已接受")
        } catch (e: Exception) {
            Timber.e(e, "接受隐私政策失败")
        }
    }
    
    /**
     * 拒绝隐私政策
     * DATA-PRV-001: 用户拒绝隐私政策，应用只能使用基础功能
     */
    suspend fun declinePrivacyPolicy() {
        try {
            context.dataStore.edit { preferences ->
                preferences[PRIVACY_POLICY_ACCEPTED] = false
                preferences[PRIVACY_POLICY_ACCEPTED_VERSION] = ""
                preferences[DATA_COLLECTION_ENABLED] = false // 拒绝隐私政策意味着禁用数据收集
            }
            Timber.d("隐私政策已拒绝")
        } catch (e: Exception) {
            Timber.e(e, "拒绝隐私政策失败")
        }
    }
    
    /**
     * 设置数据收集开关
     * DATA-PRV-003: 用户可以关闭数据收集
     */
    suspend fun setDataCollectionEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[DATA_COLLECTION_ENABLED] = enabled
                preferences[LAST_UPDATED] = System.currentTimeMillis()
            }
            Timber.d("数据收集设置: enabled=$enabled")
        } catch (e: Exception) {
            Timber.e(e, "设置数据收集失败")
        }
    }
    
    /**
     * 重置隐私政策状态（用于测试）
     */
    suspend fun resetPrivacyPolicy() {
        try {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
            Timber.d("隐私政策状态已重置")
        } catch (e: Exception) {
            Timber.e(e, "重置隐私政策失败")
        }
    }
}

/**
 * 隐私政策内容
 * DATA-PRV-001: 符合中国个人信息保护法的隐私政策
 */
object PrivacyPolicyContent {
    
    fun getPrivacyPolicyText(): String = """
        小O帮帮隐私政策
        
        更新时间：2026年5月30日
        版本号：1.2.1
        
        尊敬的用户：
        
        小O帮帮（以下简称"我们"）非常重视您的个人信息和隐私保护。本隐私政策将帮助您了解我们在您使用我们的产品和服务时如何收集、使用、存储和保护您的个人信息。
        
        一、我们收集的信息
        
        1.1 您主动提供的信息
        - 您创建的滤镜预设参数（ISO、快门速度、白平衡等）
        - 您设置的个性化偏好
        - 您反馈的问题和建议
        
        1.2 您在使用服务时自动收集的信息
        - 设备型号和操作系统版本
        - 应用使用统计数据（匿名）
        - 崩溃日志（匿名，用于改善应用稳定性）
        
        1.3 我们不收集的信息
        - 我们不会收集您的IMEI、MAC地址等设备唯一标识符
        - 我们不会收集您的照片、视频等媒体文件
        - 我们不会收集您的位置信息
        
        二、我们如何使用信息
        
        2.1 滤镜预设同步
        - 我们使用收集的预设数据为您提供云同步服务
        
        2.2 统计和分析
        - 我们使用匿名统计数据分析应用使用情况，以改善产品体验
        
        2.3 崩溃报告
        - 我们使用匿名崩溃日志来修复应用问题，提高稳定性
        
        三、信息的存储和保护
        
        3.1 存储地点
        - 您的个人信息存储在中国境内的服务器
        
        3.2 存储期限
        - 您可以随时删除自己的数据，删除后不可恢复
        
        3.3 安全措施
        - 我们采用行业标准的安全措施保护您的数据
        - 数据传输使用加密通道
        
        四、您的权利
        
        4.1 访问权
        - 您可以随时查看您的预设数据
        
        4.2 更正权
        - 您可以修改您的预设参数
        
        4.3 删除权
        - 您可以删除您的账户及所有相关数据
        
        4.4 撤回同意权
        - 您可以随时在设置中关闭数据收集
        
        五、联系我们
        
        如您对本隐私政策有任何疑问，请通过以下方式联系我们：
        - 邮箱：privacy@omaster.app
        
        六、隐私政策更新
        
        我们可能会不时更新本隐私政策。如果有重大变更，我们将通过应用内通知方式告知您。
        
        继续使用我们的服务即表示您同意本隐私政策。
    """.trimIndent()
}

/**
 * 隐私政策状态
 */
sealed class PrivacyPolicyState {
    object NotAccepted : PrivacyPolicyState()
    object Accepted : PrivacyPolicyState()
    data class AcceptedWithLimitedFeatures(val reason: String) : PrivacyPolicyState()
}
