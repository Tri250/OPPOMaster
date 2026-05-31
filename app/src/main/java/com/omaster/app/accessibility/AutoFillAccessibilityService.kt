package com.omaster.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import timber.log.Timber

class AutoFillAccessibilityService : AccessibilityService() {

    companion object {
        private var currentParams: Map<String, String>? = null

        fun setParams(params: Map<String, String>) {
            currentParams = params
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
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        val rootNode = rootInActiveWindow ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                tryAutoFillParams(rootNode)
            }
        }
    }

    private fun tryAutoFillParams(rootNode: AccessibilityNodeInfo) {
        currentParams ?: return

        Timber.d("Trying to auto-fill params: $currentParams")

        val brandCameraMap = mapOf(
            "com.oppo.camera" to OPPOCameraHelper,
            "com.oneplus.camera" to OnePlusCameraHelper,
            "com.realme.camera" to RealmeCameraHelper,
            "com.android.camera" to GenericCameraHelper
        )

        val packageName = rootNode.packageName?.toString()
        val helper = brandCameraMap.entries.find { packageName?.contains(it.key) == true }?.value

        val success = helper?.autoFillParams(rootNode, currentParams!!) ?: false
        
        // 如果自动填充失败，降级到复制到剪贴板
        if (!success) {
            copyToClipboard(currentParams!!)
        }
    }

    private fun copyToClipboard(params: Map<String, String>) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = params.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        val clip = ClipData.newPlainText("相机参数", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "参数已复制到剪贴板", Toast.LENGTH_SHORT).show()
        Timber.d("Params copied to clipboard: $text")
    }

    override fun onInterrupt() {
        Timber.d("Accessibility service interrupted")
    }
}

interface CameraAutoFillHelper {
    fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>): Boolean
}

object OPPOCameraHelper : CameraAutoFillHelper {
    override fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>): Boolean {
        Timber.d("OPPO camera auto-fill")
        
        var filledCount = 0
        
        // 尝试通过 ID 填充
        filledCount += fillParamById(rootNode, "iso", "com.oppo.camera:id/iso_value", params["iso"])
        filledCount += fillParamById(rootNode, "shutter", "com.oppo.camera:id/shutter_value", params["shutter"])
        filledCount += fillParamById(rootNode, "ev", "com.oppo.camera:id/ev_value", params["ev"])
        filledCount += fillParamById(rootNode, "wb", "com.oppo.camera:id/wb_value", params["wb"])
        
        // 如果 ID 没有找到，尝试通过文本模糊匹配
        if (filledCount == 0) {
            filledCount += fillParamByText(rootNode, "iso", "ISO", params["iso"])
            filledCount += fillParamByText(rootNode, "shutter", "快门", params["shutter"])
            filledCount += fillParamByText(rootNode, "ev", "EV", params["ev"])
            filledCount += fillParamByText(rootNode, "wb", "白平衡", params["wb"])
        }
        
        return filledCount > 0
    }

    private fun fillParamById(
        rootNode: AccessibilityNodeInfo, 
        paramName: String, 
        viewId: String, 
        value: String?
    ): Int {
        value ?: return 0
        return try {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
            nodes.firstOrNull()?.apply {
                if (isClickable || isFocusable) {
                    performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Timber.d("Filled $paramName by ID: $viewId")
                }
            }
            if (nodes.isNotEmpty()) 1 else 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to fill $paramName by ID")
            0
        }
    }
    
    private fun fillParamByText(
        rootNode: AccessibilityNodeInfo,
        paramName: String,
        searchText: String,
        value: String?
    ): Int {
        value ?: return 0
        return try {
            val nodes = rootNode.findAccessibilityNodeInfosByText(searchText)
            var found = false
            
            // 查找包含文本的节点的父节点或兄弟节点中可点击的元素
            for (node in nodes) {
                var current: AccessibilityNodeInfo? = node
                for (i in 0..3) { // 向上查找3层
                    current = current?.parent ?: break
                    if (current.isClickable || current.isFocusable) {
                        current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Timber.d("Filled $paramName by text: $searchText")
                        found = true
                        break
                    }
                }
                if (found) break
            }
            
            if (found) 1 else 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to fill $paramName by text")
            0
        }
    }
}

object OnePlusCameraHelper : CameraAutoFillHelper {
    override fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>): Boolean {
        Timber.d("OnePlus camera auto-fill")
        return OPPOCameraHelper.autoFillParams(rootNode, params)
    }
}

object RealmeCameraHelper : CameraAutoFillHelper {
    override fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>): Boolean {
        Timber.d("Realme camera auto-fill")
        return OPPOCameraHelper.autoFillParams(rootNode, params)
    }
}

object GenericCameraHelper : CameraAutoFillHelper {
    override fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>): Boolean {
        Timber.d("Generic camera auto-fill")
        // 通用相机也尝试模糊匹配
        var filledCount = 0
        filledCount += fillParamByText(rootNode, "iso", "ISO", params["iso"])
        filledCount += fillParamByText(rootNode, "shutter", "快门", params["shutter"])
        filledCount += fillParamByText(rootNode, "ev", "EV", params["ev"])
        return filledCount > 0
    }
    
    private fun fillParamByText(
        rootNode: AccessibilityNodeInfo,
        paramName: String,
        searchText: String,
        value: String?
    ): Int {
        value ?: return 0
        return try {
            val nodes = rootNode.findAccessibilityNodeInfosByText(searchText)
            var found = false
            
            for (node in nodes) {
                var current: AccessibilityNodeInfo? = node
                for (i in 0..3) {
                    current = current?.parent ?: break
                    if (current.isClickable || current.isFocusable) {
                        current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Timber.d("Filled $paramName by text: $searchText")
                        found = true
                        break
                    }
                }
                if (found) break
            }
            
            if (found) 1 else 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to fill $paramName by text")
            0
        }
    }
}
