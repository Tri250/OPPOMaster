package com.omaster.app.privacy

import android.content.Context
import android.os.Build
import com.omaster.app.security.AppIntegrityChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据收集追踪器
 * DATA-PRV-002: 数据收集最小化
 * DATA-PRV-003: 用户数据控制权
 */
@Singleton
class DataCollectionTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val privacyPolicyManager: PrivacyPolicyManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled
    
    // 数据收集事件
    data class AnalyticsEvent(
        val eventName: String,
        val properties: Map<String, Any?> = emptyMap(),
        val timestamp: Long = System.currentTimeMillis()
    )
    
    init {
        // 监听数据收集状态
        scope.launch {
            privacyPolicyManager.isDataCollectionEnabled.collect { enabled ->
                _isEnabled.value = enabled
                Timber.d("数据收集状态: $enabled")
            }
        }
    }
    
    /**
     * 记录应用启动事件
     * DATA-PRV-002: 匿名统计
     */
    fun trackAppOpen() {
        if (!_isEnabled.value) return
        
        val event = AnalyticsEvent(
            eventName = "app_open",
            properties = mapOf(
                "app_version" to getAppVersion(),
                "os_version" to Build.VERSION.SDK_INT,
                "device_brand" to Build.BRAND,
                "device_model" to Build.MODEL
            )
        )
        logEvent(event)
    }
    
    /**
     * 记录预设创建事件
     * DATA-PRV-002: 匿名统计
     */
    fun trackPresetCreated(presetName: String, presetType: String) {
        if (!_isEnabled.value) return
        
        val event = AnalyticsEvent(
            eventName = "preset_created",
            properties = mapOf(
                "preset_name" to presetName,
                "preset_type" to presetType
            )
        )
        logEvent(event)
    }
    
    /**
     * 记录功能使用事件
     * DATA-PRV-002: 匿名统计
     */
    fun trackFeatureUsed(featureName: String, duration: Long? = null) {
        if (!_isEnabled.value) return
        
        val properties = mutableMapOf<String, Any?>(
            "feature_name" to featureName
        )
        duration?.let { properties["duration_ms"] = it }
        
        val event = AnalyticsEvent(
            eventName = "feature_used",
            properties = properties
        )
        logEvent(event)
    }
    
    /**
     * 记录错误事件
     * DATA-PRV-002: 匿名崩溃日志
     */
    fun trackError(errorMessage: String, stackTrace: String? = null) {
        if (!_isEnabled.value) return
        
        val properties = mutableMapOf<String, Any?>(
            "error_message" to errorMessage
        )
        stackTrace?.let { properties["stack_trace"] = it }
        
        val event = AnalyticsEvent(
            eventName = "error",
            properties = properties
        )
        logEvent(event)
    }
    
    /**
     * 记录隐私相关操作
     * DATA-PRV-003: 用户数据控制权追踪
     */
    fun trackPrivacyAction(action: String) {
        Timber.d("隐私操作: $action")
        // 隐私操作本地记录，不上传
    }
    
    /**
     * 记录数据导出
     * DATA-PRV-003: 数据导出追踪
     */
    fun trackDataExport() {
        trackFeatureUsed("data_export")
    }
    
    /**
     * 记录数据删除
     * DATA-PRV-003: 数据删除追踪
     */
    fun trackDataDeletion() {
        trackFeatureUsed("data_deletion")
    }
    
    /**
     * 记录数据收集设置变更
     * DATA-PRV-003: 用户数据控制权
     */
    suspend fun trackDataCollectionSettingChange(enabled: Boolean) {
        scope.launch {
            privacyPolicyManager.setDataCollectionEnabled(enabled)
            trackPrivacyAction("data_collection_${if (enabled) "enabled" else "disabled"}")
        }
    }
    
    /**
     * 记录隐私政策接受
     * DATA-PRV-001: 隐私政策追踪
     */
    suspend fun trackPrivacyPolicyAccepted() {
        scope.launch {
            privacyPolicyManager.acceptPrivacyPolicy()
            trackPrivacyAction("privacy_policy_accepted")
        }
    }
    
    /**
     * 记录隐私政策拒绝
     * DATA-PRV-001: 隐私政策追踪
     */
    suspend fun trackPrivacyPolicyDeclined() {
        scope.launch {
            privacyPolicyManager.declinePrivacyPolicy()
            trackPrivacyAction("privacy_policy_declined")
        }
    }
    
    /**
     * 获取应用版本
     * DATA-PRV-002: 用于统计
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * 记录事件（本地日志）
     * DATA-PRV-002: 实际生产中应发送到分析服务器
     */
    private fun logEvent(event: AnalyticsEvent) {
        Timber.d("数据分析事件: ${event.eventName}, 属性: ${event.properties}")
        
        // 实际生产中，这里应该将事件发送到分析服务器
        // 例如使用Firebase Analytics、Mixpanel等
        // 注意：不应发送任何可识别个人身份的信息
        
        // 示例：发送到Firebase Analytics
        // firebaseAnalytics.logEvent(event.eventName) {
        //     event.properties.forEach { (key, value) ->
        //         param(key, value?.toString() ?: "")
        //     }
        // }
    }
    
    /**
     * 生成匿名设备ID
     * DATA-PRV-002: 不使用IMEI等唯一标识符
     */
    fun getAnonymousDeviceId(): String {
        val deviceInfo = "${Build.BRAND}-${Build.MODEL}-${Build.VERSION.SDK_INT}"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(deviceInfo.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }.take(16)
    }
    
    /**
     * 检查是否可以收集数据
     * DATA-PRV-002: 数据收集最小化
     */
    suspend fun canCollectData(): Boolean {
        val isDataCollectionEnabled = privacyPolicyManager.isDataCollectionEnabled.first()
        val isPrivacyPolicyAccepted = privacyPolicyManager.isPrivacyPolicyAccepted.first()
        
        return isDataCollectionEnabled && isPrivacyPolicyAccepted
    }
}
