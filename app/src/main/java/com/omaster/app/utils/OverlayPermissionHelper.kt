package com.omaster.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
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
        val isMIUI = manufacturer.contains("xiaomi") || manufacturer.contains("redmi")
        val isOriginOS = manufacturer.contains("vivo")
        
        val dialogMessage = if (isColorOS || isOxygenOS) {
            """
            OMaster 需要悬浮窗权限来显示预设参数。
            
            请按照以下步骤操作：
            1. 点击「去授权」按钮
            2. 找到「悬浮窗」选项
            3. 将权限设置为「允许」
            4. 返回应用即可使用
            
            💡 提示：在 ColorOS/OxygenOS 系统上，还需要关闭「省电管理」中的后台限制，才能保证悬浮窗在后台持续显示。
            """.trimIndent()
        } else if (isMIUI) {
            """
            OMaster 需要悬浮窗权限来显示预设参数。
            
            请按照以下步骤操作：
            1. 点击「去授权」按钮
            2. 找到「悬浮窗」选项
            3. 将权限设置为「允许」
            4. 返回应用即可使用
            
            💡 提示：在 MIUI 系统上，建议同时在「电量和性能」设置中关闭省电策略，确保悬浮窗后台保活。
            """.trimIndent()
        } else if (isOriginOS) {
            """
            OMaster 需要悬浮窗权限来显示预设参数。
            
            请按照以下步骤操作：
            1. 点击「去授权」按钮
            2. 找到「悬浮窗」选项
            3. 将权限设置为「允许」
            4. 返回应用即可使用
            
            💡 提示：在 OriginOS 系统上，建议同时关闭后台弹窗限制。
            """.trimIndent()
        } else {
            """
            OMaster 需要悬浮窗权限来显示预设参数。
            
            请按照以下步骤操作：
            1. 点击「去授权」按钮
            2. 找到「悬浮窗」选项
            3. 将权限设置为「允许」
            4. 返回应用即可使用
            """.trimIndent()
        }
        
        MaterialAlertDialogBuilder(context)
            .setTitle("需要悬浮窗权限")
            .setMessage(dialogMessage)
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
            .setCancelable(false)
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
            shouldShowRequestPermissionRationale(context) -> {
                Timber.d("Should show request permission rationale")
                onNeedsRequest()
            }
            else -> {
                Timber.d("Need to request overlay permission")
                onNeedsRequest()
            }
        }
    }
    
    private fun shouldShowRequestPermissionRationale(context: Context): Boolean {
        return false
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
