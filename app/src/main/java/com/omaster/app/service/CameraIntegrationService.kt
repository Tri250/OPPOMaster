package com.omaster.app.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraIntegrationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    data class CameraAppInfo(
        val packageName: String,
        val appName: String,
        val isDefault: Boolean
    )

    data class PresetApplicationResult(
        val success: Boolean,
        val message: String,
        val method: ApplicationMethod
    )

    enum class ApplicationMethod {
        INTENT_WITH_PARAMS,
        CONTENT_URI,
        CLIPBOARD,
        BROADCAST,
        NOT_SUPPORTED
    }

    companion object {
        private val OPPO_CAMERA_PACKAGES = listOf(
            "com.oppo.camera",
            "com.coloros.camera",
            "com.coloros.safecenter",
            "com.heytap.mcs"
        )

        private const val OPPO_CAMERA_ACTION = "android.media.action.IMAGE_CAPTURE"
        private const val EXTRA_CAMERA_DATA = "android.intent.extras.CAMERA_DATA"
        private const val EXTRA_ISO = "iso"
        private const val EXTRA_SHUTTER_SPEED = "shutter-speed"
        private const val EXTRA_EXPOSURE_COMPENSATION = "exposure-compensation"
        private const val EXTRA_WHITE_BALANCE = "white-balance"
        private const val EXTRA_FOCUS_MODE = "focus-mode"
    }

    suspend fun openSystemCamera(): Boolean = withContext(Dispatchers.Main) {
        try {
            val intent = createCameraIntent()
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                Toast.makeText(context, "未找到相机应用", Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "打开相机失败")
            Toast.makeText(context, "无法打开相机: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    suspend fun applyPresetToCamera(params: CameraParams): PresetApplicationResult = withContext(Dispatchers.Main) {
        try {
            val method = detectApplicationMethod()
            
            when (method) {
                ApplicationMethod.INTENT_WITH_PARAMS -> {
                    applyViaIntentWithParams(params)
                }
                ApplicationMethod.CONTENT_URI -> {
                    applyViaContentUri(params)
                }
                ApplicationMethod.CLIPBOARD -> {
                    applyViaClipboard(params)
                }
                ApplicationMethod.BROADCAST -> {
                    applyViaBroadcast(params)
                }
                ApplicationMethod.NOT_SUPPORTED -> {
                    PresetApplicationResult(
                        success = false,
                        message = "当前设备不支持此功能",
                        method = method
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "应用预设失败")
            PresetApplicationResult(
                success = false,
                message = "应用预设失败: ${e.message}",
                method = ApplicationMethod.NOT_SUPPORTED
            )
        }
    }

    private fun createCameraIntent(): Intent? {
        val pm = context.packageManager
        val resolveInfos = pm.queryIntentActivities(
            Intent(OPPO_CAMERA_ACTION),
            PackageManager.MATCH_DEFAULT_ONLY
        )

        for (info in resolveInfos) {
            val packageName = info.activityInfo.packageName
            if (OPPO_CAMERA_PACKAGES.any { packageName.contains(it, ignoreCase = true) }) {
                return Intent(OPPO_CAMERA_ACTION).apply {
                    setPackage(packageName)
                }
            }
        }

        if (resolveInfos.isNotEmpty()) {
            val firstInfo = resolveInfos[0]
            return Intent(OPPO_CAMERA_ACTION).apply {
                setComponent(
                    ComponentName(
                        firstInfo.activityInfo.packageName,
                        firstInfo.activityInfo.name
                    )
                )
            }
        }

        return pm.resolveActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            },
            PackageManager.MATCH_DEFAULT_ONLY
        )?.let { defaultApp ->
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setComponent(
                    ComponentName(
                        defaultApp.activityInfo.packageName,
                        defaultApp.activityInfo.name
                    )
                )
            }
        }
    }

    private fun detectApplicationMethod(): ApplicationMethod {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        
        return when {
            manufacturer.contains("oppo") || brand.contains("oppo") ||
            manufacturer.contains("realme") || brand.contains("realme") ||
            manufacturer.contains("oneplus") || brand.contains("oneplus") ||
            manufacturer.contains("vivo") || brand.contains("vivo") -> {
                ApplicationMethod.INTENT_WITH_PARAMS
            }
            manufacturer.contains("huawei") || brand.contains("huawei") ||
            manufacturer.contains("honor") || brand.contains("honor") -> {
                ApplicationMethod.BROADCAST
            }
            manufacturer.contains("xiaomi") || brand.contains("xiaomi") ||
            manufacturer.contains("redmi") || brand.contains("redmi") ||
            manufacturer.contains("poco") || brand.contains("poco") -> {
                ApplicationMethod.CONTENT_URI
            }
            else -> {
                ApplicationMethod.CLIPBOARD
            }
        }
    }

    private fun applyViaIntentWithParams(params: CameraParams): PresetApplicationResult {
        return try {
            val intent = createCameraIntent() ?: return PresetApplicationResult(
                success = false,
                message = "无法创建相机意图",
                method = ApplicationMethod.INTENT_WITH_PARAMS
            )

            intent.putExtra(EXTRA_ISO, params.iso.toString())
            intent.putExtra(EXTRA_SHUTTER_SPEED, params.shutter)
            intent.putExtra(EXTRA_EXPOSURE_COMPENSATION, params.ev)
            intent.putExtra(EXTRA_WHITE_BALANCE, params.wb)
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
            PresetApplicationResult(
                success = true,
                message = "已在相机中设置参数（部分参数可能不生效）",
                method = ApplicationMethod.INTENT_WITH_PARAMS
            )
        } catch (e: Exception) {
            Timber.e(e, "通过意图应用参数失败")
            PresetApplicationResult(
                success = false,
                message = "应用参数失败: ${e.message}",
                method = ApplicationMethod.INTENT_WITH_PARAMS
            )
        }
    }

    private fun applyViaContentUri(params: CameraParams): PresetApplicationResult {
        return try {
            val format = buildParameterString(params)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("OMaster参数", format)
            clipboard.setPrimaryClip(clip)
            
            openSystemCamera()
            
            PresetApplicationResult(
                success = true,
                message = "参数已复制到剪贴板，请在相机中粘贴使用",
                method = ApplicationMethod.CLIPBOARD
            )
        } catch (e: Exception) {
            Timber.e(e, "通过内容URI应用参数失败")
            PresetApplicationResult(
                success = false,
                message = "应用参数失败: ${e.message}",
                method = ApplicationMethod.CONTENT_URI
            )
        }
    }

    private fun applyViaClipboard(params: CameraParams): PresetApplicationResult {
        return try {
            val format = buildParameterString(params)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("OMaster参数", format)
            clipboard.setPrimaryClip(clip)
            
            Toast.makeText(
                context,
                "参数已复制: ISO=${params.iso}, 快门=${params.shutter}, EV=${params.ev}",
                Toast.LENGTH_LONG
            ).show()
            
            PresetApplicationResult(
                success = true,
                message = "参数已复制到剪贴板",
                method = ApplicationMethod.CLIPBOARD
            )
        } catch (e: Exception) {
            Timber.e(e, "通过剪贴板应用参数失败")
            PresetApplicationResult(
                success = false,
                message = "应用参数失败: ${e.message}",
                method = ApplicationMethod.CLIPBOARD
            )
        }
    }

    private fun applyViaBroadcast(params: CameraParams): PresetApplicationResult {
        return try {
            val intent = Intent("com.android.camera.action.UPDATE_PARAMS").apply {
                putExtra(EXTRA_ISO, params.iso)
                putExtra(EXTRA_SHUTTER_SPEED, params.shutter)
                putExtra(EXTRA_EXPOSURE_COMPENSATION, params.ev)
                putExtra(EXTRA_WHITE_BALANCE, params.wb)
            }
            
            context.sendBroadcast(intent)
            
            PresetApplicationResult(
                success = true,
                message = "参数已发送到相机应用",
                method = ApplicationMethod.BROADCAST
            )
        } catch (e: Exception) {
            Timber.e(e, "通过广播应用参数失败")
            PresetApplicationResult(
                success = false,
                message = "应用参数失败: ${e.message}",
                method = ApplicationMethod.BROADCAST
            )
        }
    }

    private fun buildParameterString(params: CameraParams): String {
        return buildString {
            appendLine("=== OMaster 相机参数 ===")
            appendLine()
            appendLine("📷 建议相机设置:")
            appendLine("• ISO: ${params.iso}")
            appendLine("• 快门: ${params.shutter}")
            appendLine("• 曝光补偿: ${params.ev}")
            appendLine("• 白平衡: ${params.wb}")
            if (params.filter.isNotEmpty()) {
                appendLine("• 滤镜: ${params.filter}")
            }
            appendLine()
            if (params.hasselblad_hncs) {
                appendLine("✨ 推荐开启 HNCS 模式")
            }
            appendLine()
            appendLine("来自 OMaster - OPPO 哈苏影像系统")
        }
    }

    fun getAvailableCameraApps(): List<CameraAppInfo> {
        val apps = mutableListOf<CameraAppInfo>()
        val pm = context.packageManager
        
        val intent = Intent(OPPO_CAMERA_ACTION)
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        
        for (info in resolveInfos) {
            val appName = info.loadLabel(pm).toString()
            apps.add(
                CameraAppInfo(
                    packageName = info.activityInfo.packageName,
                    appName = appName,
                    isDefault = OPPO_CAMERA_PACKAGES.any {
                        info.activityInfo.packageName.contains(it, ignoreCase = true)
                    }
                )
            )
        }
        
        return apps.sortedByDescending { it.isDefault }
    }

    fun isCameraPermissionGranted(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.CAMERA) == 
                PackageManager.PERMISSION_GRANTED
    }

    fun hasSystemOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun requestSystemOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    data class CameraParams(
        val iso: Int,
        val shutter: String,
        val ev: String,
        val wb: String,
        val filter: String = "",
        val hasselblad_hncs: Boolean = false
    )
}
