package com.omaster.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import timber.log.Timber

object RomUtils {
    
    enum class RomType {
        MIUI, COLOR_OS, HARMONY_OS, EMUI, ORIGIN_OS, FLYME, SAMSUNG, OTHER
    }
    
    fun getRomType(): RomType {
        val brand = Build.BRAND.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        return when {
            isMiui() -> RomType.MIUI
            isColorOs() -> RomType.COLOR_OS
            isHarmonyOs() -> RomType.HARMONY_OS
            isEmui() -> RomType.EMUI
            isOriginOs() -> RomType.ORIGIN_OS
            isFlyme() -> RomType.FLYME
            isSamsung() -> RomType.SAMSUNG
            else -> RomType.OTHER
        }
    }
    
    private fun isMiui(): Boolean {
        return getSystemProperty("ro.miui.ui.version.name") != null
    }
    
    private fun isColorOs(): Boolean {
        return getSystemProperty("ro.build.version.opporom") != null ||
               getSystemProperty("ro.oplus.version.name") != null
    }
    
    private fun isHarmonyOs(): Boolean {
        return getSystemProperty("hw_sc.build.platform.version") != null
    }
    
    private fun isEmui(): Boolean {
        return getSystemProperty("ro.build.version.emui") != null
    }
    
    private fun isOriginOs(): Boolean {
        return getSystemProperty("ro.vivo.os.version") != null
    }
    
    private fun isFlyme(): Boolean {
        return getSystemProperty("ro.build.display.id")?.contains("flyme", ignoreCase = true) == true
    }
    
    private fun isSamsung(): Boolean {
        return Build.BRAND.equals("samsung", ignoreCase = true)
    }
    
    private fun getSystemProperty(key: String): String? {
        return try {
            val c = Class.forName("android.os.SystemProperties")
            val get = c.getMethod("get", String::class.java)
            get.invoke(c, key) as? String
        } catch (e: Exception) {
            null
        }
    }
    
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to open battery optimization settings")
            openAppSettings(context)
        }
    }
    
    fun openAutostartSettings(context: Context) {
        try {
            val intent = when (getRomType()) {
                RomType.MIUI -> {
                    Intent().apply {
                        component = android.content.ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.autostart.AutoStartManagementActivity"
                        )
                    }
                }
                RomType.COLOR_OS -> {
                    Intent().apply {
                        component = android.content.ComponentName(
                            "com.coloros.safecenter",
                            "com.coloros.safecenter.startupapp.StartupAppListActivity"
                        )
                    }
                }
                RomType.EMUI, RomType.HARMONY_OS -> {
                    Intent().apply {
                        component = android.content.ComponentName(
                            "com.huawei.systemmanager",
                            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                        )
                    }
                }
                RomType.ORIGIN_OS -> {
                    Intent().apply {
                        component = android.content.ComponentName(
                            "com.iqoo.secure",
                            "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                        )
                    }
                }
                RomType.FLYME -> {
                    Intent().apply {
                        component = android.content.ComponentName(
                            "com.meizu.safe",
                            "com.meizu.safe.permission.SmartBGActivity"
                        )
                    }
                }
                RomType.SAMSUNG, RomType.OTHER -> {
                    null
                }
            }
            
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            } ?: run {
                openAppSettings(context)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to open autostart settings")
            openAppSettings(context)
        }
    }
    
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    fun openOverlayPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
