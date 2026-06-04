package com.omaster.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

class AutoFillAccessibilityService : AccessibilityService() {

    companion object {
        // 使用 AtomicReference 确保线程安全
        private val currentParams = AtomicReference<Map<String, String>>(null)

        // 静态常量：品牌相机映射
        private val brandCameraMap = mapOf(
            "com.oppo.camera" to OPPOCameraHelper,
            "com.oneplus.camera" to OnePlusCameraHelper,
            "com.realme.camera" to RealmeCameraHelper,
            "com.android.camera" to GenericCameraHelper
        )

        fun setParams(params: Map<String, String>) {
            // 创建不可变副本
            currentParams.set(params.toMap())
        }

        fun getParams(): Map<String, String>? {
            return currentParams.get()
        }

        fun isServiceEnabled(context: Context): Boolean {
            return try {
                val pref = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                pref?.contains("${context.packageName}/.accessibility.AutoFillAccessibilityService") ?: false
            } catch (e: Exception) {
                Timber.e(e, "Failed to check accessibility service status")
                false
            }
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.d("Accessibility service connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.d("Accessibility service unbound")
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        var rootNode: AccessibilityNodeInfo? = null
        try {
            rootNode = rootInActiveWindow ?: return

            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    tryAutoFillParams(rootNode)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in onAccessibilityEvent")
        } finally {
            rootNode?.recycle()
        }
    }

    private fun tryAutoFillParams(rootNode: AccessibilityNodeInfo) {
        val params = currentParams.get() ?: return

        Timber.d("Trying to auto-fill params: $params")

        val packageName = rootNode.packageName?.toString()
        if (packageName == null) {
            Timber.d("Package name is null, skipping auto-fill")
            return
        }

        try {
            val helper = brandCameraMap.entries.find { packageName.contains(it.key) }?.value
            helper?.autoFillParams(rootNode, params)
        } catch (e: Exception) {
            Timber.e(e, "Error in tryAutoFillParams")
        }
    }

    override fun onInterrupt() {
        Timber.d("Accessibility service interrupted")
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
        var nodes: List<AccessibilityNodeInfo>? = null
        try {
            nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
            nodes.firstOrNull()?.apply {
                if (isClickable || isFocusable) {
                    performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in fillParam for $paramName")
        } finally {
            nodes?.forEach { it.recycle() }
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