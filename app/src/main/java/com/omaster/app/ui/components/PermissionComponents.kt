package com.omaster.app.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.omaster.app.security.PermissionManager
import com.omaster.app.ui.theme.*

/**
 * 权限申请对话框组件
 * PERM-SEC-002: 运行时权限申请
 * PERM-COL-001: ColorOS悬浮窗权限
 */
@Composable
fun PermissionRequestDialog(
    permission: String,
    title: String,
    description: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = OppoPrimary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OppoPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("去授权", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("暂不授权", color = TextSecondary)
            }
        }
    )
}

/**
 * 悬浮窗权限申请卡片
 * PERM-COL-001: ColorOS悬浮窗权限
 */
@Composable
fun OverlayPermissionCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasPermission = remember { mutableStateOf(PermissionManager.hasOverlayPermission(context)) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPermission.value) OppoGreen.copy(alpha = 0.1f) else OppoPrimary.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (hasPermission.value) Icons.Default.CheckCircle else Icons.Default.Layers,
                    contentDescription = null,
                    tint = if (hasPermission.value) OppoGreen else OppoPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "悬浮窗权限",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasPermission.value) "已授权" else "点击授权",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasPermission.value) OppoGreen else TextSecondary
                    )
                }
            }
            
            if (!hasPermission.value) {
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OppoPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("授权", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 存储权限申请卡片
 * PERM-SEC-002: 运行时权限申请
 */
@Composable
fun StoragePermissionCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasPermission = remember { mutableStateOf(PermissionManager.hasStoragePermission(context)) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPermission.value) OppoGreen.copy(alpha = 0.1f) else OppoPrimary.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (hasPermission.value) Icons.Default.CheckCircle else Icons.Default.Folder,
                    contentDescription = null,
                    tint = if (hasPermission.value) OppoGreen else OppoPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "存储权限",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasPermission.value) "已授权" else "导入/导出预设需要",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasPermission.value) OppoGreen else TextSecondary
                    )
                }
            }
            
            if (!hasPermission.value) {
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OppoPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("授权", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 电池优化权限卡片
 * PERM-COL-002: 后台运行权限
 */
@Composable
fun BatteryOptimizationCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isIgnoring = remember { mutableStateOf(PermissionManager.isIgnoringBatteryOptimizations(context)) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isIgnoring.value) OppoGreen.copy(alpha = 0.1f) else HasselbladOrange.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isIgnoring.value) Icons.Default.CheckCircle else Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = if (isIgnoring.value) OppoGreen else HasselbladOrange,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "后台运行权限",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isIgnoring.value) "已优化" else "建议开启以确保后台同步",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isIgnoring.value) OppoGreen else TextSecondary
                    )
                }
            }
            
            if (!isIgnoring.value) {
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HasselbladOrange
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("优化", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 权限说明对话框
 * PERM-SEC-002: 权限用途说明
 */
@Composable
fun PermissionRationaleDialog(
    permission: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val title = when (permission) {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_MEDIA_IMAGES -> "存储权限"
        Manifest.permission.POST_NOTIFICATIONS -> "通知权限"
        Manifest.permission.SYSTEM_ALERT_WINDOW -> "悬浮窗权限"
        else -> "权限"
    }
    
    val description = PermissionManager.getPermissionRationale(permission)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = HasselbladOrange,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "为什么需要$tittle",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OppoPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("继续", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}

/**
 * 权限被永久拒绝对话框
 * PERM-SEC-003: 权限撤销处理
 */
@Composable
fun PermissionDeniedPermanentlyDialog(
    permission: String,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val permissionName = when (permission) {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_MEDIA_IMAGES -> "存储权限"
        Manifest.permission.POST_NOTIFICATIONS -> "通知权限"
        Manifest.permission.SYSTEM_ALERT_WINDOW -> "悬浮窗权限"
        else -> "该权限"
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "$permissionName被永久拒绝",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "要使用此功能，请到设置中手动开启$permissionName。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                
                Surface(
                    color = HasselbladOrange.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "设置 > 应用 > $permissionName > 允许",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = HasselbladOrange,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OppoPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("打开设置", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后", color = TextSecondary)
            }
        }
    )
}

/**
 * 权限设置引导组件
 * PERM-COL-001: ColorOS专用权限引导
 */
@Composable
fun PermissionGuideCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = OppoPrimary.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = OppoPrimary
                )
                Text(
                    text = "悬浮窗功能说明",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "悬浮窗功能可在相机上层显示参数信息，支持六大品牌相机（OPPO、一加、Realme、小米、华为、vivo）。",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = OppoGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "六大品牌适配",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = OppoGreen
                    )
                }
                Surface(
                    color = HasselbladOrange.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "95%+适配率",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = HasselbladOrange
                    )
                }
            }
        }
    }
}
