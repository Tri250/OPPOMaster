package com.omaster.app.floating

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val REQUEST_CODE_OVERLAY_PERMISSION = 1001
    }

    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun requestOverlayPermission(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun getSystemPermissionIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun getCustomPermissionIntent(): Intent? {
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        return when {
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                Intent().apply {
                    setClassName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                }
            }
            manufacturer.contains("oneplus") -> {
                Intent().apply {
                    setClassName(
                        "com.oneplus.security",
                        "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                    )
                }
            }
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                    setClassName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.permissions.PermissionsEditorActivity"
                    )
                    putExtra("extra_pkgname", context.packageName)
                }
            }
            manufacturer.contains("vivo") -> {
                Intent().apply {
                    setClassName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                }
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                // HarmonyOS/Huawei
                Intent().apply {
                    setClassName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                }
            }
            else -> null
        }
    }

    fun getColorOSSpecialGuidanceIntent(): Intent? {
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        return if (manufacturer.contains("oppo") || manufacturer.contains("realme")) {
            Intent().apply {
                setClassName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            }
        } else null
    }

    fun isColorOS(): Boolean {
        return Build.MANUFACTURER.lowercase().contains("oppo") ||
                Build.MANUFACTURER.lowercase().contains("realme")
    }

    fun isOxygenOS(): Boolean {
        return Build.MANUFACTURER.lowercase().contains("oneplus")
    }

    fun isMIUI(): Boolean {
        return Build.MANUFACTURER.lowercase().contains("xiaomi") ||
                Build.MANUFACTURER.lowercase().contains("redmi")
    }

    fun isOriginOS(): Boolean {
        return Build.MANUFACTURER.lowercase().contains("vivo")
    }
    
    fun isHarmonyOS(): Boolean {
        return Build.MANUFACTURER.lowercase().contains("huawei") || 
                Build.MANUFACTURER.lowercase().contains("honor")
    }

    fun getSystemBrand(): String {
        return when {
            isColorOS() -> "ColorOS"
            isOxygenOS() -> "OxygenOS"
            isMIUI() -> "MIUI"
            isOriginOS() -> "OriginOS"
            isHarmonyOS() -> "HarmonyOS"
            else -> "原生Android"
        }
    }

    fun shouldShowSpecialGuidance(): Boolean {
        return isColorOS() || isOxygenOS() || isMIUI() || isHarmonyOS()
    }

    fun getSpecialGuidanceText(): String {
        return when {
            isColorOS() -> """
                请按以下步骤授予 ColorOS 悬浮窗权限：
                
                1. 点击「去授权」按钮
                2. 在「权限与隐私」→「悬浮窗」中找到 OPPO Master
                3. 开启「允许显示悬浮窗」
                4. 同时建议在「自启动管理」中开启本应用
            """.trimIndent()
            isOxygenOS() -> """
                请按以下步骤授予 OxygenOS 悬浮窗权限：
                
                1. 点击「去授权」按钮
                2. 在「权限」→「悬浮窗」中找到 OPPO Master
                3. 开启「允许显示悬浮窗」
                4. 建议在「电池优化」中选择「不优化」
            """.trimIndent()
            isMIUI() -> """
                请按以下步骤授予 MIUI 悬浮窗权限：
                
                1. 点击「去授权」按钮
                2. 在「权限管理」→「显示悬浮窗」中找到 OPPO Master
                3. 选择「允许」
                4. 同时建议在「后台弹出界面」中也选择「允许」
            """.trimIndent()
            isHarmonyOS() -> """
                请按以下步骤授予 HarmonyOS 悬浮窗权限：
                
                1. 点击「去授权」按钮
                2. 在「应用权限」→「悬浮窗」中找到 OPPO Master
                3. 开启「允许」
                4. 建议同时开启「后台弹出界面」权限
            """.trimIndent()
            else -> """
                请按以下步骤授予悬浮窗权限：
                
                1. 点击「去授权」按钮
                2. 在设置中找到「显示悬浮窗」选项
                3. 授予 OPPO Master 权限
            """.trimIndent()
        }
    }

    fun checkAllPermissions(): PermissionStatus {
        return PermissionStatus(
            canDrawOverlays = canDrawOverlays(),
            systemBrand = getSystemBrand(),
            shouldShowSpecialGuidance = shouldShowSpecialGuidance()
        )
    }
}

data class PermissionStatus(
    val canDrawOverlays: Boolean,
    val systemBrand: String,
    val shouldShowSpecialGuidance: Boolean
)
