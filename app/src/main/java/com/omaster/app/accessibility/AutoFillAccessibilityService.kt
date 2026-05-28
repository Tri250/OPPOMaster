package com.omaster.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import timber.log.Timber

/**
 * OPPOMaster无障碍服务 - 安全加固版本
 * 
 * 安全改进：
 * 1. 包名白名单限制 - 只响应指定相机应用
 * 2. 用户确认机制 - 填充前需要用户确认
 * 3. 数据最小化 - 不存储敏感信息
 * 4. 访问日志 - 记录非敏感的操作日志
 * 
 * 作者：带娃的小陈工
 * 版本：2.0（安全加固版）
 */
class AutoFillAccessibilityService : AccessibilityService() {

    companion object {
        // 严格的白名单：只允许这些包名使用自动填充
        private val ALLOWED_PACKAGES = setOf(
            "com.oppo.camera",
            "com.oneplus.camera",
            "com.realme.camera"
        )
        
        private var pendingParams: Map<String, String>? = null
        private var pendingCallback: (() -> Unit)? = null
        private var isUserConfirmed = false

        fun setPendingParams(params: Map<String, String>, callback: () -> Unit) {
            pendingParams = params
            pendingCallback = callback
            isUserConfirmed = false
        }

        fun isServiceEnabled(context: Context): Boolean {
            val pref = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return pref?.contains("${context.packageName}/.accessibility.AutoFillAccessibilityService") ?: false
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
        }

        fun hasPendingConfirmation(): Boolean {
            return pendingParams != null && !isUserConfirmed
        }

        fun confirmAutoFill() {
            isUserConfirmed = true
            pendingCallback?.invoke()
            pendingParams = null
            pendingCallback = null
        }

        fun cancelAutoFill() {
            pendingParams = null
            pendingCallback = null
            isUserConfirmed = false
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        // 配置服务信息 - 严格限制权限范围
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            // 只监听白名单中的包
            packageNames = ALLOWED_PACKAGES.toTypedArray()
            // 禁用窗口内容检索 - 提高安全性
            canRetrieveWindowContent = false
        }
        
        serviceInfo = info
        
        Timber.d("AutoFillAccessibilityService connected - Security mode enabled")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // 安全检查1：验证包名是否在白名单中
        val packageName = event.packageName?.toString()
        if (packageName !in ALLOWED_PACKAGES) {
            Timber.d("Blocked accessibility event from non-whitelisted package: $packageName")
            return
        }

        // 安全检查2：检查是否有待处理的参数且用户已确认
        if (pendingParams == null || !isUserConfirmed) {
            return
        }

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
                // 只记录非敏感信息
                Timber.d("Auto-fill triggered for package: $packageName")
                it.autoFillParams(rootNode, pendingParams!!)
            }
        } catch (e: Exception) {
            Timber.e(e, "Auto-fill failed")
        } finally {
            // 清理敏感数据
            rootNode.recycle()
            clearPendingData()
        }
    }

    private fun clearPendingData() {
        pendingParams = null
        pendingCallback = null
        isUserConfirmed = false
    }

    override fun onInterrupt() {
        Timber.d("Accessibility service interrupted")
        clearPendingData()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearPendingData()
        Timber.d("AutoFillAccessibilityService destroyed")
    }
}

/**
 * 相机自动填充助手接口
 */
interface CameraAutoFillHelper {
    fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>)
}

/**
 * OPPO相机助手
 * 
 * 安全说明：此实现仅填充预设参数，不读取或存储任何用户数据
 */
object OPPOCameraHelper : CameraAutoFillHelper {
    
    // 安全的参数ID映射 - 只包含预设参数
    private val paramIdMap = mapOf(
        "iso" to "com.oppo.camera:id/iso_value",
        "shutter" to "com.oppo.camera:id/shutter_value",
        "ev" to "com.oppo.camera:id/ev_value",
        "wb" to "com.oppo.camera:id/wb_value"
    )

    override fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>) {
        Timber.d("OPPO camera auto-fill initiated")
        
        // 只填充预设的参数，不处理其他数据
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
            // 查找目标视图
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
            nodes.firstOrNull()?.apply {
                // 只对可交互元素执行点击
                if (isClickable || isFocusable) {
                    Timber.d("Filling parameter: $paramName = $value")
                    performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                recycle()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fill parameter: $paramName")
        }
    }
}

/**
 * OnePlus相机助手
 * 继承OPPO的实现（因为ColorOS和一加的相机界面类似）
 */
object OnePlusCameraHelper : CameraAutoFillHelper {
    override fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>) {
        Timber.d("OnePlus camera auto-fill initiated")
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
        Timber.d("Realme camera auto-fill initiated")
        
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
                    Timber.d("Filling parameter: $paramName = $value")
                    performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                recycle()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fill parameter: $paramName")
        }
    }
}

/**
 * 通用相机助手（备用）
 */
object GenericCameraHelper : CameraAutoFillHelper {
    override fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>) {
        Timber.d("Generic camera auto-fill - no specific implementation")
    }
}
