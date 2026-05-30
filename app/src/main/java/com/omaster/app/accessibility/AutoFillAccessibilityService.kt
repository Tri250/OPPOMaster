package com.omaster.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.ForegroundInfo
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

class AutoFillAccessibilityService : AccessibilityService() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private val currentParams = AtomicReference<Map<String, String>?>(null)

        private var serviceScope: CoroutineScope? = null

        fun setParams(params: Map<String, String>) {
            currentParams.set(HashMap(params))
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
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        Timber.d("AutoFillAccessibilityService created")
    }

    override fun onDestroy() {
        serviceScope?.cancel()
        serviceScope = null
        super.onDestroy()
        Timber.d("AutoFillAccessibilityService destroyed")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.d("AutoFillAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        if (event.packageName == null) return

        val packageName = event.packageName.toString()
        if (!isTargetPackage(packageName)) return

        serviceScope?.launch {
            try {
                handleAccessibilityEvent(event, packageName)
            } catch (e: Exception) {
                Timber.e(e, "Error handling accessibility event")
            }
        }
    }

    private fun isTargetPackage(packageName: String): Boolean {
        return packageName in listOf(
            "com.oppo.camera",
            "com.oneplus.camera",
            "com.realme.camera",
            "com.android.camera"
        )
    }

    private suspend fun handleAccessibilityEvent(event: AccessibilityEvent, packageName: String) {
        val rootNode = withContext(Dispatchers.Main) {
            rootInActiveWindow
        } ?: return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            tryAutoFillParams(rootNode, packageName)
        }

        withContext(Dispatchers.Main) {
            rootNode.recycle()
        }
    }

    private suspend fun tryAutoFillParams(rootNode: AccessibilityNodeInfo, packageName: String) {
        val params = currentParams.get() ?: return

        Timber.d("Trying to auto-fill params for $packageName")

        withContext(Dispatchers.Default) {
            val helper = when {
                packageName.contains("oppo") -> OPPOCameraHelper
                packageName.contains("oneplus") -> OnePlusCameraHelper
                packageName.contains("realme") -> RealmeCameraHelper
                else -> GenericCameraHelper
            }

            helper.autoFillParams(rootNode, params)
        }
    }

    override fun onInterrupt() {
        Timber.d("Accessibility service interrupted")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.d("AutoFillAccessibilityService unbound")
        return super.onUnbind(intent)
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
        fillParam(rootev, "ev", "com.oppo.camera:id/ev_value", params["ev"])
        fillParam(rootNode, "wb", "com.oppo.camera:id/wb_value", params["wb"])
    }

    private fun fillParam(rootNode: AccessibilityNodeInfo, paramName: String, viewId: String, value: String?) {
        value ?: return
        try {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
            nodes.firstOrNull()?.apply {
                if (isClickable || isFocusable) {
                    performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                recycle()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error filling param: $paramName")
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
