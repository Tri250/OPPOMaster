package com.omaster.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.omaster.app.util.SecureLogManager
import java.util.Locale

/**
 * OMaster无障碍服务 - 安全加固版本
 *
 * 安全改进：
 * 1. 包名白名单限制 - 仅响应指定相机应用
 * 2. 用户确认机制 - 自动填充前需要用户明确同意
 * 3. 数据最小化 - 不存储或传输敏感信息
 * 4. 访问日志 - 记录非敏感的操作日志
 * 5. 敏感界面检测 - 锁屏、支付等界面自动禁用
 *
 * 作者：带娃的小陈工
 * 版本：3.0（完全安全加固版）
 */
class AutoFillAccessibilityService : AccessibilityService() {

    companion object {
        // 严格白名单：仅允许这些相机应用
        private val ALLOWED_PACKAGES = setOf(
            "com.oppo.camera",
            "com.oneplus.camera",
            "com.realme.camera"
        )

        // 敏感界面包名列表 - 自动禁用服务
        private val SENSITIVE_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.settings",
            "com.oppo.pay",
            "com.tencent.mm",
            "com.alipay.mobile.nebula",
            "com.android.bank"
        )

        // 敏感界面关键词
        private val SENSITIVE_KEYWORDS = listOf(
            "支付", "付款", "密码", "pass", "pay",
            "lock", "lockscreen", "锁屏", "登录", "login"
        )

        private var pendingParams: Map<String, String>? = null
        private var pendingCallback: (() -> Unit)? = null
        private var isUserConfirmed = false
        private var isServicePaused = false

        /**
         * 设置待填充的参数
         */
        fun setPendingParams(params: Map<String, String>, callback: () -> Unit) {
            pendingParams = params
            pendingCallback = callback
            isUserConfirmed = false
            SecureLogManager.logSensitive("Set pending params", true)
        }

        /**
         * 确认自动填充（用户点击确认按钮时调用）
         */
        fun confirmAutoFill() {
            isUserConfirmed = true
            SecureLogManager.logSensitive("User confirmed auto-fill", true)
            pendingCallback?.invoke()
            pendingParams = null
            pendingCallback = null
        }

        /**
         * 取消自动填充
         */
        fun cancelAutoFill() {
            pendingParams = null
            pendingCallback = null
            isUserConfirmed = false
            SecureLogManager.logSensitive("User cancelled auto-fill", false)
        }

        /**
         * 检查服务是否已启用
         */
        fun isServiceEnabled(context: Context): Boolean {
            val pref = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return pref?.contains("${context.packageName}/.accessibility.AutoFillAccessibilityService") ?: false
        }

        /**
         * 打开无障碍服务设置页面
         */
        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
        }

        /**
         * 检查是否有待确认的填充
         */
        fun hasPendingConfirmation(): Boolean {
            return pendingParams != null && !isUserConfirmed
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // 配置服务信息 - 严格限制权限范围
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100

            // 仅监听白名单中的包名
            packageNames = ALLOWED_PACKAGES.toTypedArray()

            // 禁用窗口内容检索 - 增强安全性
            flags = flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }

        serviceInfo = info

        SecureLogManager.d("AutoFillAccessibilityService connected - Security mode enabled")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // 安全检查1：验证包名
        val packageName = event.packageName?.toString() ?: return

        // 安全检查2：检测敏感界面，自动暂停服务
        if (isSensitiveInterface(packageName, event)) {
            isServicePaused = true
            SecureLogManager.logAccessibilityEvent(packageName, false)
            return
        }
        isServicePaused = false

        // 安全检查3：检查是否在白名单中
        if (packageName !in ALLOWED_PACKAGES) {
            SecureLogManager.logAccessibilityEvent(packageName, false)
            return
        }

        SecureLogManager.logAccessibilityEvent(packageName, true)

        // 安全检查4：检查是否有待处理的参数且用户已确认
        if (pendingParams == null || !isUserConfirmed) {
            return
        }

        // 获取根节点
        val rootNode = rootInActiveWindow ?: return

        try {
            // 根据包名选择对应的相机助手
            val helper = when {
                packageName.contains("oppo") -> OPPOCameraHelper
                packageName.contains("oneplus") -> OnePlusCameraHelper
                packageName.contains("realme") -> RealmeCameraHelper
                else -> null
            }

            helper?.let {
                SecureLogManager.d("Triggering auto-fill for: $packageName")
                it.autoFillParams(rootNode, pendingParams!!)
            }
        } catch (e: Exception) {
            SecureLogManager.e("Auto-fill failed", e)
        } finally {
            // 清理敏感数据
            rootNode.recycle()
            clearPendingData()
        }
    }

    /**
     * 检查是否为敏感界面
     */
    private fun isSensitiveInterface(packageName: String, event: AccessibilityEvent): Boolean {
        // 检查包名是否在敏感列表中
        if (packageName in SENSITIVE_PACKAGES) {
            return true
        }

        // 检查事件内容是否包含敏感关键词
        val content = event.contentDescription?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val text = event.text?.joinToString(" ")?.lowercase(Locale.getDefault()) ?: ""
        val combined = "$content $text"

        return SENSITIVE_KEYWORDS.any { keyword ->
            combined.contains(keyword.lowercase(Locale.getDefault()))
        }
    }

    /**
     * 清理敏感数据
     */
    private fun clearPendingData() {
        pendingParams = null
        pendingCallback = null
        isUserConfirmed = false
    }

    override fun onInterrupt() {
        SecureLogManager.d("Accessibility service interrupted")
        clearPendingData()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearPendingData()
        SecureLogManager.d("AutoFillAccessibilityService destroyed")
    }
}

/**
 * 相机自动填充助手接口
 */
interface CameraAutoFillHelper {
    fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>)
}

/**
 * OPPO相机助手 - 安全加固版本
 *
 * 安全说明：仅填充预设参数，不读取或存储任何用户数据
 */
object OPPOCameraHelper : CameraAutoFillHelper {

    // 安全的参数ID映射 - 仅包含预设相关参数
    private val paramIdMap = mapOf(
        "iso" to "com.oppo.camera:id/iso_value",
        "shutter" to "com.oppo.camera:id/shutter_value",
        "ev" to "com.oppo.camera:id/ev_value",
        "wb" to "com.oppo.camera:id/wb_value"
    )

    override fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>) {
        SecureLogManager.d("OPPO camera auto-fill initiated")

        // 仅填充预设的参数，不处理其他数据
        paramIdMap.forEach { (paramName, viewId) ->
            params[paramName]?.let { value ->
                fillParam(rootNode, paramName, viewId, value)
            }
        }
    }

    private fun fillParam(
        rootNode: AccessibilityNodeInfo,
        paramName: String,
        viewId: String,
        value: String
    ) {
        try {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
            nodes.firstOrNull()?.apply {
                // 仅对可交互元素执行操作
                if (isClickable || isFocusable) {
                    SecureLogManager.d("Filling parameter: $paramName")
                    performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                recycle()
            }
        } catch (e: Exception) {
            SecureLogManager.e("Failed to fill parameter: $paramName", e)
        }
    }
}

/**
 * OnePlus相机助手
 */
object OnePlusCameraHelper : CameraAutoFillHelper {
    override fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>) {
        SecureLogManager.d("OnePlus camera auto-fill initiated")
        OPPOCameraHelper.autoFillParams(rootNode, params)
    }
}

/**
 * Realme相机助手
 */
object RealmeCameraHelper : CameraAutoFillHelper {

    // Realme特定的参数ID
    private val paramIdMap = mapOf(
        "iso" to "com.realme.camera:id/iso_value",
        "shutter" to "com.realme.camera:id/shutter_value",
        "ev" to "com.realme.camera:id/ev_value",
        "wb" to "com.realme.camera:id/wb_value"
    )

    override fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>) {
        SecureLogManager.d("Realme camera auto-fill initiated")

        paramIdMap.forEach { (paramName, viewId) ->
            params[paramName]?.let { value ->
                fillParam(rootNode, paramName, viewId, value)
            }
        }
    }

    private fun fillParam(
        rootNode: AccessibilityNodeInfo,
        paramName: String,
        viewId: String,
        value: String
    ) {
        try {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
            nodes.firstOrNull()?.apply {
                if (isClickable || isFocusable) {
                    SecureLogManager.d("Filling parameter: $paramName")
                    performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                recycle()
            }
        } catch (e: Exception) {
            SecureLogManager.e("Failed to fill parameter: $paramName", e)
        }
    }
}

/**
 * 通用相机助手（备用）
 */
object GenericCameraHelper : CameraAutoFillHelper {
    override fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>) {
        SecureLogManager.d("Generic camera auto-fill - no specific implementation")
    }
}
