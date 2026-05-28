package com.omaster.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import timber.log.Timber

object OverlayPermissionHelper {
    
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }
    
    fun requestOverlayPermission(context: Context, onGranted: () -> Unit, onDenied: () -> Unit) {
        if (canDrawOverlays(context)) {
            onGranted()
            return
        }
        
        val manufacturer = Build.MANUFACTURER.lowercase()
        val isColorOS = manufacturer.contains("oppo") || manufacturer.contains("realme")
        val isOxygenOS = manufacturer.contains("oneplus")
        
        // 构建基础权限用途说明 - 严格符合隐私承诺
        val baseMessage = """
            OMaster 需要悬浮窗权限来显示预设参数。
            
            ⚠️ 安全声明：
            • 悬浮窗权限仅用于在相机上层展示调色参数
            • 不会获取您的相机画面
            • 不会监听您的任何操作
            • 不会记录您的任何输入
            
            请按照以下步骤操作：
            1. 点击「去授权」按钮
            2. 找到「悬浮窗」选项
            3. 将权限设置为「允许」
            4. 返回应用即可使用
        """.trimIndent()
        
        // ColorOS/OxygenOS 专属安全提示
        val colorOSMessage = if (isColorOS || isOxygenOS) {
            """
                $baseMessage
                
                💡 ColorOS/OxygenOS 安全提示：
                解除授权限制后，请确保只授予悬浮窗权限，其他敏感权限可根据需要选择是否授予。
                
                为保证悬浮窗在后台持续显示，建议：
                • 在「电池」设置中关闭对 OMaster 的省电限制
                • 添加 OMaster 到后台清理白名单
            """.trimIndent()
        } else {
            baseMessage
        }
        
        // 显示权限申请弹窗
        MaterialAlertDialogBuilder(context)
            .setTitle("需要悬浮窗权限")
            .setMessage(colorOSMessage)
            .setPositiveButton("去授权") { dialog, _ ->
                dialog.dismiss()
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to open overlay permission settings")
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (e2: Exception) {
                        Timber.e(e2, "Failed to open app settings")
                        onDenied()
                    }
                }
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                onDenied()
            }
            .setCancelable(true)
            .show()
    }
    
    /**
     * 显示权限被拒绝时的友好提示 - 非强制
     */
    fun showPermissionDeniedTip(context: Context, message: String = "该功能需要对应权限才能使用") {
        MaterialAlertDialogBuilder(context)
            .setTitle("权限未授予")
            .setMessage("$message\n\n您可以稍后在设置中授予权限，其他功能仍可正常使用。")
            .setPositiveButton("知道了") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("去设置") { dialog, _ ->
                dialog.dismiss()
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to open app settings")
                }
            }
            .show()
    }
    
    fun checkAndRequestPermission(
        context: Context,
        onSuccess: () -> Unit,
        onNeedsRequest: () -> Unit,
        onDenied: () -> Unit
    ) {
        when {
            canDrawOverlays(context) -> {
                Timber.d("Overlay permission already granted")
                onSuccess()
            }
            else -> {
                Timber.d("Need to request overlay permission")
                onNeedsRequest()
            }
        }
    }
    
    fun getOverlayPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }
    
    fun getAppSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
}
