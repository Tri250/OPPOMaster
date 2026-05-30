package com.omaster.app.security

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import timber.log.Timber

/**
 * 权限管理器
 * PERM-SEC-002: 运行时权限管理
 * PERM-SEC-003: 权限撤销处理
 * PERM-COL-001: ColorOS悬浮窗权限
 */
object PermissionManager {
    
    // 存储权限组
    private val STORAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    
    // 悬浮窗权限
    private val OVERLAY_PERMISSION = Manifest.permission.SYSTEM_ALERT_WINDOW
    
    // 通知权限
    private val NOTIFICATION_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }
    
    /**
     * PERM-SEC-002: 检查存储权限
     */
    fun hasStoragePermission(context: Context): Boolean {
        return STORAGE_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * PERM-SEC-002: 检查悬浮窗权限
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }
    
    /**
     * PERM-SEC-002: 检查通知权限
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 12及以下不需要通知权限
        }
    }
    
    /**
     * PERM-SEC-001: 获取所有必需的权限
     */
    fun getRequiredPermissions(): Array<String> {
        return STORAGE_PERMISSIONS
    }
    
    /**
     * PERM-SEC-002: 获取需要申请的权限（未授权的）
     */
    fun getNeededPermissions(context: Context): Array<String> {
        return STORAGE_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }
    
    /**
     * PERM-SEC-002: 请求存储权限
     * 返回true表示需要请求权限，false表示已有权限
     */
    fun requestStoragePermission(activity: FragmentActivity, requestCode: Int): Boolean {
        val neededPermissions = getNeededPermissions(activity)
        
        if (neededPermissions.isEmpty()) {
            Timber.d("存储权限已授权")
            return false
        }
        
        if (activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Timber.d("需要向用户解释为什么需要存储权限")
            // 返回true让调用者决定是否显示说明
            return true
        }
        
        activity.requestPermissions(neededPermissions, requestCode)
        return true
    }
    
    /**
     * PERM-COL-001: 请求悬浮窗权限 - 跳转到ColorOS专用设置页面
     */
    fun requestOverlayPermission(activity: Activity) {
        Timber.d("请求悬浮窗权限，跳转到ColorOS设置页面")
        
        try {
            // 尝试跳转到ColorOS特定的悬浮窗设置页面
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:${activity.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "跳转到悬浮窗设置页面失败")
            // 降级到通用设置页面
            val fallbackIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(fallbackIntent)
        }
    }
    
    /**
     * PERM-SEC-002: 请求通知权限
     */
    fun requestNotificationPermission(activity: FragmentActivity, requestCode: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false // 不需要请求
        }
        
        if (hasNotificationPermission(activity)) {
            return false
        }
        
        activity.requestPermissions(NOTIFICATION_PERMISSION, requestCode)
        return true
    }
    
    /**
     * PERM-SEC-003: 处理权限结果
     */
    fun handlePermissionResult(
        permissions: Array<out String>,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onDeniedPermanently: (permissions: List<String>) -> Unit
    ) {
        val deniedPermissions = mutableListOf<String>()
        
        permissions.forEachIndexed { index, permission ->
            if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permission)
            }
        }
        
        if (deniedPermissions.isEmpty()) {
            Timber.d("所有请求的权限都已授权")
            onGranted()
        } else {
            Timber.w("部分权限被拒绝: $deniedPermissions")
            onDenied()
            
            // 检查是否被永久拒绝
            // 注意：在Activity中需要检查shouldShowRequestPermissionRationale
        }
    }
    
    /**
     * PERM-SEC-003: 检查权限是否被永久拒绝
     */
    fun isPermissionPermanentlyDenied(
        activity: FragmentActivity,
        permissions: Array<String>
    ): Boolean {
        return permissions.any { permission ->
            !activity.shouldShowRequestPermissionRationale(permission) &&
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * PERM-SEC-003: 打开应用设置页面
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * PERM-COL-002: 检查应用是否在后台被优化
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }
    
    /**
     * PERM-COL-002: 请求忽略电池优化
     */
    fun requestIgnoreBatteryOptimizations(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations(activity)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                try {
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    Timber.e(e, "请求忽略电池优化失败")
                }
            }
        }
    }
    
    /**
     * PERM-SEC-002: 生成权限说明文本
     */
    fun getPermissionRationale(permission: String): String {
        return when (permission) {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES -> 
                "存储权限用于导入和导出预设文件，以及保存水印截图。请放心，我们不会访问您的其他私人文件。"
            
            Manifest.permission.POST_NOTIFICATIONS ->
                "通知权限用于提醒您预设同步状态和社区更新。您可以随时在设置中关闭通知。"
            
            Manifest.permission.SYSTEM_ALERT_WINDOW ->
                "悬浮窗权限用于在相机上层显示参数信息。这是实现参数自动填入功能的关键权限。"
            
            else -> "此权限是应用功能所必需的。"
        }
    }
}

/**
 * 权限状态
 */
enum class PermissionState {
    GRANTED,           // 已授权
    DENIED,            // 被拒绝（可再次请求）
    DENIED_PERMANENTLY, // 永久拒绝（需要手动到设置中开启）
    NOT_DETERMINED     // 尚未请求
}

/**
 * 权限检查结果
 */
data class PermissionCheckResult(
    val permission: String,
    val state: PermissionState,
    val shouldShowRationale: Boolean = false
)
