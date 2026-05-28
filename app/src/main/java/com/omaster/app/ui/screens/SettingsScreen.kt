package com.omaster.app.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaster.app.data.ThemeMode
import com.omaster.app.service.FloatingWindowService
import com.omaster.app.ui.theme.*
import com.omaster.app.utils.OverlayPermissionHelper
import com.omaster.app.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val fluidCloudEnabled by viewModel.fluidCloudEnabled.collectAsStateWithLifecycle()
    val overlayEnabled by viewModel.overlayEnabled.collectAsStateWithLifecycle()
    val analyticsEnabled by viewModel.analyticsEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    
    fun toggleOverlayWithPermission() {
        OverlayPermissionHelper.checkAndRequestPermission(
            context = context,
            onSuccess = {
                scope.launch {
                    viewModel.setOverlayEnabled(!overlayEnabled)
                    if (!overlayEnabled) {
                        FloatingWindowService.updatePresets(emptyList())
                        FloatingWindowService.showOverlay(context)
                    } else {
                        FloatingWindowService.hideOverlay(context)
                    }
                }
            },
            onNeedsRequest = {
                OverlayPermissionHelper.requestOverlayPermission(
                    context = context,
                    onGranted = {
                        scope.launch {
                            viewModel.setOverlayEnabled(true)
                            FloatingWindowService.updatePresets(emptyList())
                            FloatingWindowService.showOverlay(context)
                        }
                    },
                    onDenied = {
                        OverlayPermissionHelper.showPermissionDeniedTip(
                            context,
                            "悬浮窗功能需要权限才能使用"
                        )
                    }
                )
            },
            onDenied = {
                OverlayPermissionHelper.showPermissionDeniedTip(
                    context,
                    "悬浮窗功能需要权限才能使用"
                )
            }
        )
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 外观设置
            SectionTitle(text = "外观")
            
            SettingsItem(
                title = "主题",
                description = when (currentThemeMode) {
                    ThemeMode.SYSTEM.value -> "跟随系统"
                    ThemeMode.LIGHT.value -> "浅色模式"
                    ThemeMode.DARK.value -> "深色模式"
                    else -> "跟随系统"
                },
                icon = Icons.Default.Palette,
                onClick = { showThemeDialog = true }
            )
            
            // 系统能力
            Spacer(modifier = Modifier.height(8.dp))
            SectionTitle(text = "系统能力")
            
            SettingsItem(
                title = "流体云胶囊",
                description = "在系统侧边栏显示当前选中的预设",
                icon = Icons.Default.BubbleChart,
                checked = fluidCloudEnabled,
                onCheckedChange = { viewModel.setFluidCloudEnabled(it) }
            )
            
            SettingsItem(
                title = "悬浮窗",
                description = if (overlayEnabled) "悬浮窗已开启" else "悬浮窗已关闭",
                icon = Icons.Default.Layers,
                checked = overlayEnabled,
                onCheckedChange = { toggleOverlayWithPermission() }
            )
            
            // 隐私设置
            Spacer(modifier = Modifier.height(8.dp))
            SectionTitle(text = "隐私与安全")
            
            SettingsItem(
                title = "隐私政策",
                description = "查看 OMaster 隐私政策",
                icon = Icons.Default.PrivacyTip,
                onClick = { showPrivacyDialog = true }
            )
            
            SettingsItem(
                title = "匿名使用统计",
                description = "仅收集匿名功能使用频次，用于优化产品体验",
                icon = Icons.Default.Analytics,
                checked = analyticsEnabled,
                onCheckedChange = { 
                    if (!analyticsEnabled) {
                        showAnalyticsConsentDialog(context) { 
                            scope.launch {
                                viewModel.setAnalyticsEnabled(it)
                            }
                        }
                    } else {
                        scope.launch {
                            viewModel.setAnalyticsEnabled(false)
                        }
                    }
                }
            )
            
            // 数据管理
            Spacer(modifier = Modifier.height(8.dp))
            SectionTitle(text = "数据管理")
            
            SettingsItem(
                title = "导出预设数据",
                description = "导出自定义预设和收藏数据",
                icon = Icons.Default.Download,
                onClick = { 
                    showExportDataDialog(context)
                }
            )
            
            SettingsItem(
                title = "清除所有本地数据",
                description = "删除所有收藏、自定义预设，恢复初始状态",
                icon = Icons.Default.DeleteForever,
                isDangerous = true,
                onClick = { showClearDataDialog = true }
            )
            
            // 关于
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle(text = "关于")
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "OMaster",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AccentPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "版本 1.5.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "OPPO 哈苏影像系统级参数中枢\n\n© 2025 OMaster Team. CC BY-NC-SA 4.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    // 对话框
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentThemeMode = currentThemeMode,
            onThemeSelected = { themeMode ->
                viewModel.setThemeMode(themeMode)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
    
    if (showPrivacyDialog) {
        PrivacyPolicyDialog(
            onDismiss = { showPrivacyDialog = false }
        )
    }
    
    if (showClearDataDialog) {
        ClearDataConfirmDialog(
            onConfirm = {
                scope.launch {
                    viewModel.clearAllUserData()
                    showClearDataDialog = false
                }
            },
            onDismiss = { showClearDataDialog = false }
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun SettingsItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    isDangerous: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = if (onClick != null) onClick else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (checked != null && onCheckedChange != null) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (isDangerous) MaterialTheme.colorScheme.error else AccentPrimary,
                        checkedTrackColor = if (isDangerous) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else AccentPrimary.copy(alpha = 0.3f)
                    )
                )
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ThemeSelectionDialog(
    currentThemeMode: Int,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主题") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOption(
                    title = "跟随系统",
                    description = "使用系统设置",
                    selected = currentThemeMode == ThemeMode.SYSTEM.value,
                    onClick = { onThemeSelected(ThemeMode.SYSTEM) }
                )
                ThemeOption(
                    title = "浅色模式",
                    description = "始终使用浅色主题",
                    selected = currentThemeMode == ThemeMode.LIGHT.value,
                    onClick = { onThemeSelected(ThemeMode.LIGHT) }
                )
                ThemeOption(
                    title = "深色模式",
                    description = "始终使用深色主题",
                    selected = currentThemeMode == ThemeMode.DARK.value,
                    onClick = { onThemeSelected(ThemeMode.DARK) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun ThemeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = AccentPrimary
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("OMaster 隐私政策") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "最后更新：2025年5月",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "## 1. 数据处理原则\n\nOMaster 严格遵循「纯本地化运作」的核心承诺：",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "• 所有用户数据（收藏、自定义预设、透明度设置）均完整存储在应用私有目录中\n• 未开启统计功能时，不会发起任何网络请求（除用户主动触发的云端预设更新外）\n• 不会收集、存储或传输任何可识别用户身份的信息",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "## 2. 权限说明",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "• **悬浮窗权限 (SYSTEM_ALERT_WINDOW)**：仅用于在相机上层展示调色参数\n• **存储权限 (READ_MEDIA_IMAGES)**：仅用于用户主动触发时保存参数卡片和样片到相册\n• **通知权限 (POST_NOTIFICATIONS)**：仅用于悬浮窗常驻通知和更新提示\n• **网络权限 (INTERNET)**：仅用于用户主动触发的云端预设更新",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "## 3. 用户权利",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "• 您有权随时查看、删除或导出您的所有数据\n• 您有权随时开启或关闭统计功能\n• 您有权拒绝任何非必要的权限申请",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "## 4. 统计功能",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "统计功能默认完全关闭。开启后仅收集匿名功能使用频次，用于优化产品体验，不会收集任何个人数据、本地预设或操作轨迹。",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun ClearDataConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("清除所有本地数据") },
        text = {
            Text(
                text = "此操作将删除所有收藏、自定义预设和个性化设置，恢复应用初始状态。此操作不可恢复，确定要继续吗？",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("清除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

fun showAnalyticsConsentDialog(context: Context, onConsent: (Boolean) -> Unit) {
    MaterialAlertDialogBuilder(context)
        .setTitle("匿名使用统计")
        .setMessage("""
            是否开启匿名使用统计？
            
            • 仅收集功能使用频次（例如：收藏预设次数、悬浮窗使用时长）
            • 完全匿名化，不会收集任何可识别身份的信息
            • 不会上传本地预设、收藏或任何个人数据
            • 数据仅用于优化产品体验
            • 您可以随时在设置中关闭
            
            是否开启？
        """.trimIndent())
        .setPositiveButton("开启") { dialog, _ ->
            dialog.dismiss()
            onConsent(true)
        }
        .setNegativeButton("不开启") { dialog, _ ->
            dialog.dismiss()
            onConsent(false)
        }
        .setCancelable(false)
        .show()
}

fun showExportDataDialog(context: Context) {
    MaterialAlertDialogBuilder(context)
        .setTitle("导出预设数据")
        .setMessage("""
            导出功能即将上线！
            
            导出数据将包含：
            • 所有收藏的预设
            • 自定义预设参数
            • 个性化设置
            
            数据将以 JSON 格式保存到您的设备存储中，不包含任何个人信息。
        """.trimIndent())
        .setPositiveButton("知道了") { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}
