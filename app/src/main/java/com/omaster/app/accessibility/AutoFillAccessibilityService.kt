package com.omaster.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

class AutoFillAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoFillService"

        // 待填充的预设（静态存储）
        private val pendingPresetRef = AtomicReference<Preset?>(null)

        // 相机应用包名列表
        private val CAMERA_PACKAGES = listOf(
            "com.oppo.camera",
            "com.oneplus.camera",
            "com.realme.camera",
            "com.android.camera",
            "com.android.camera2",
            "com.google.android.GoogleCamera"
        )

        fun setPendingPreset(preset: Preset?) {
            pendingPresetRef.set(preset)
            Timber.tag(TAG).d("Pending preset set: ${preset?.name}")
        }

        fun getPendingPreset(): Preset? {
            return pendingPresetRef.get()
        }

        fun clearPendingPreset() {
            pendingPresetRef.set(null)
        }

        fun isServiceEnabled(context: Context): Boolean {
            val serviceName = "${context.packageName}/${AutoFillAccessibilityService::class.java.canonicalName}"

            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )

            if (enabledServices.isNullOrEmpty()) {
                return false
            }

            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabledServices)
            while (splitter.hasNext()) {
                val componentName = splitter.next()
                if (componentName.equals(serviceName, ignoreCase = true) ||
                    componentName.endsWith("/${AutoFillAccessibilityService::class.java.simpleName}")) {
                    return true
                }
            }

            return false
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.tag(TAG).d("Accessibility service connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(TAG).d("Accessibility service destroyed")
    }

    override fun onInterrupt() {
        Timber.tag(TAG).w("Accessibility service interrupted")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pendingPreset = pendingPresetRef.get() ?: return

        // 只处理相机应用的事件
        val packageName = event.packageName?.toString() ?: return
        if (!CAMERA_PACKAGES.any { packageName.contains(it, ignoreCase = true) }) {
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                tryFillParameters(pendingPreset)
            }
        }
    }

    /**
     * 尝试填充参数到相机界面
     */
    private fun tryFillParameters(preset: Preset) {
        val rootNode = rootInActiveWindow ?: return
        val params = preset.cameraParams ?: return

        try {
            var filledCount = 0

            // 填充ISO
            params.iso?.let { iso ->
                if (fillValue(rootNode, "ISO", iso.toString())) {
                    filledCount++
                }
            }

            // 填充快门速度
            params.shutter?.let { shutter ->
                if (fillValue(rootNode, "快门", shutter)) {
                    filledCount++
                }
                if (fillValue(rootNode, "Shutter", shutter)) {
                    filledCount++
                }
            }

            // 填充白平衡
            params.wb?.let { wb ->
                if (fillValue(rootNode, "白平衡", wb)) {
                    filledCount++
                }
                if (fillValue(rootNode, "WB", wb)) {
                    filledCount++
                }
            }

            // 填充曝光补偿
            params.ev?.let { ev ->
                if (fillValue(rootNode, "曝光", ev)) {
                    filledCount++
                }
                if (fillValue(rootNode, "EV", ev)) {
                    filledCount++
                }
            }

            Timber.tag(TAG).d("Filled $filledCount parameters for preset: ${preset.name}")

            // 填充完成后清除待填充预设
            if (filledCount > 0) {
                clearPendingPreset()
            }
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * 在节点树中查找并填充值
     */
    private fun fillValue(rootNode: AccessibilityNodeInfo, label: String, value: String): Boolean {
        val nodes = rootNode.findAccessibilityNodeInfosByText(label)
        for (node in nodes) {
            val parent = node.parent ?: continue
            val clickable = findClickableParent(parent)
            clickable?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            // 这里可以进一步输入具体值
            return true
        }
        return false
    }

    /**
     * 查找可点击的父节点
     */
    private fun findClickableParent(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current
            }
            current = current.parent
        }
        return null
    }
}

interface CameraAutoFillHelper {
    fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>)
}

object OPPOCameraHelper : CameraAutoFillHelper {
    override fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>) {
        Timber.d("OPPO camera auto-fill")
        fillParam(rootNode, "iso", "com.oppo.camera:id/iso_value", params["iso"])
        fillParam(rootNode, "shutter", "com.oppo.camera:id/shutter_value", params["shutter"])
        fillParam(rootNode, "ev", "com.oppo.camera:id/ev_value", params["ev"])
        fillParam(rootNode, "wb", "com.oppo.camera:id/wb_value", params["wb"])
    }

    private fun fillParam(rootNode: AccessibilityNodeInfo, paramName: String, viewId: String, value: String?) {
        value ?: return
        val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
        nodes.firstOrNull()?.apply {
            if (isClickable || isFocusable) {
                performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
    }
}

object OnePlusCameraHelper : CameraAutoFillHelper {
    override fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>) {
        Timber.d("OnePlus camera auto-fill")
        OPPOCameraHelper.autoFillParams(rootNode, params)
    }
}

object RealmeCameraHelper : CameraAutoFillHelper {
    override fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>) {
        Timber.d("Realme camera auto-fill")
        OPPOCameraHelper.autoFillParams(rootNode, params)
    }
}

object GenericCameraHelper : CameraAutoFillHelper {
    override fun autoFillParams(rootNode: AccessibilityNodeInfo, params: Map<String, String>) {
        Timber.d("Generic camera auto-fill")
    }
}
