package com.omaster.app.floating

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
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

    fun getSystemBrand(): String {
        return when {
            isColorOS() -> "ColorOS"
            isOxygenOS() -> "OxygenOS"
            isMIUI() -> "MIUI"
            isOriginOS() -> "OriginOS"
            else -> "原生Android"
        }
    }

    fun shouldShowSpecialGuidance(): Boolean {
        return isColorOS() || isOxygenOS()
    }

    fun getSpecialGuidanceText(): String {
        return when {
            isColorOS() -> """
                请按以下步骤解除ColorOS授权限制：
                
                1. 点击「去授权」按钮
                2. 在「权限与隐私」→「自启动管理」中找到OMaster
                3. 开启「允许自启动」和「允许后台活动」
                4. 返回后点击「允许」授予悬浮窗权限
            """.trimIndent()
            isOxygenOS() -> """
                请按以下步骤解除OxygenOS授权限制：
                
                1. 点击「去授权」按钮
                2. 在「电池」→「电池优化」中找到OMaster
                3. 选择「不允许」以防止后台被清理
                4. 返回后授予悬浮窗权限
            """.trimIndent()
            else -> ""
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
