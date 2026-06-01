package com.omaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omaster.app.ui.theme.Spacing
import com.omaster.app.ui.theme.Typography
import com.omaster.app.ui.theme.hasselbladOrange
import kotlinx.coroutines.launch

/**
 * 隐私设置页面 - PRIV测试用例实现
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onBack: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit = {},
    onManageCameraPermission: () -> Unit = {},
    onManageStoragePermission: () -> Unit = {}
) {
    val viewModel = remember { PrivacySettingsViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showClearDataDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "隐私设置",
                        style = Typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            // 数据收集部分
            PrivacySectionCard(
                title = "数据收集",
                items = listOf(
                    PrivacySettingItem(
                        icon = Icons.Default.Analytics,
                        title = "使用统计",
                        description = "收集应用使用数据以改善体验",
                        isChecked = uiState.isUsageStatsEnabled,
                        onCheckedChange = { viewModel.toggleUsageStats(it) }
                    ),
                    PrivacySettingItem(
                        icon = Icons.Default.DataSaverOn,
                        title = "崩溃报告",
                        description = "自动发送崩溃信息以修复问题",
                        isChecked = uiState.isCrashReportEnabled,
                        onCheckedChange = { viewModel.toggleCrashReport(it) }
                    )
                )
            )

            // 权限管理部分
            PrivacySectionCard(
                title = "权限管理",
                items = listOf(
                    PrivacySettingItem(
                        icon = Icons.Default.PhotoCamera,
                        title = "相机权限",
                        description = "管理相机访问权限",
                        isArrow = true,
                        onClick = onManageCameraPermission
                    ),
                    PrivacySettingItem(
                        icon = Icons.Default.Storage,
                        title = "存储权限",
                        description = "管理文件存储访问权限",
                        isArrow = true,
                        onClick = onManageStoragePermission
                    )
                )
            )

            // 数据共享部分
            PrivacySectionCard(
                title = "数据共享",
                items = listOf(
                    PrivacySettingItem(
                        icon = Icons.Default.Share,
                        title = "第三方数据共享",
                        description = "是否向第三方共享用户数据",
                        isChecked = uiState.isThirdPartySharingEnabled,
                        onCheckedChange = { viewModel.toggleThirdPartySharing(it) },
                        isDanger = false,
                        showDescription = true
                    )
                )
            )

            // 数据清除部分
            PrivacySectionCard(
                title = "数据管理",
                items = listOf(
                    PrivacySettingItem(
                        icon = Icons.Default.DeleteSweep,
                        title = "清除个人数据",
                        description = "清除所有个人数据（收藏、下载记录等）",
                        isArrow = true,
                        onClick = { showClearDataDialog = true },
                        isDanger = true
                    )
                )
            )

            // 底部链接
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onOpenPrivacyPolicy) {
                    Text(
                        text = "隐私政策",
                        style = Typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // 清除数据确认对话框
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("清除个人数据") },
            text = {
                Text(
                    "此操作将清除所有个人数据，包括收藏、下载记录和使用统计，无法恢复。确定要继续吗？"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.clearPersonalData()
                            showClearDataDialog = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 隐私设置分区卡片
 */
@Composable
fun PrivacySectionCard(
    title: String,
    items: List<PrivacySettingItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.medium)
        ) {
            Text(
                text = title,
                style = Typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            items.forEachIndexed { index, item ->
                PrivacySettingRow(item = item)
                
                if (index < items.size - 1) {
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp
                    )
                    Spacer(modifier = Modifier.height(Spacing.medium))
                }
            }
        }
    }
}

/**
 * 隐私设置项
 */
data class PrivacySettingItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String,
    val isChecked: Boolean? = null,
    val onCheckedChange: ((Boolean) -> Unit)? = null,
    val isArrow: Boolean = false,
    val onClick: (() -> Unit)? = null,
    val isDanger: Boolean = false,
    val showDescription: Boolean = true
)

/**
 * 隐私设置行
 */
@Composable
fun PrivacySettingRow(item: PrivacySettingItem) {
    val contentColor = if (item.isDanger) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (item.onClick != null) {
                    Modifier.clickable(onClick = item.onClick)
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        // 图标
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(10.dp),
            color = hasselbladOrange.copy(alpha = 0.1f)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = if (item.isDanger) MaterialTheme.colorScheme.error else hasselbladOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 文字内容
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.title,
                style = Typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
            if (item.showDescription) {
                Text(
                    text = item.description,
                    style = Typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 开关或箭头
        if (item.isChecked != null && item.onCheckedChange != null) {
            Switch(
                checked = item.isChecked,
                onCheckedChange = item.onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = hasselbladOrange,
                    checkedTrackColor = hasselbladOrange.copy(alpha = 0.5f)
                )
            )
        } else if (item.isArrow) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "更多",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 隐私设置ViewModel
 */
class PrivacySettingsViewModel {
    private val _uiState = MutableStateFlow(PrivacySettingsUiState())
    val uiState: androidx.compose.runtime.StateFlow<PrivacySettingsUiState> = _uiState

    fun toggleUsageStats(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isUsageStatsEnabled = enabled)
    }

    fun toggleCrashReport(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isCrashReportEnabled = enabled)
    }

    fun toggleThirdPartySharing(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isThirdPartySharingEnabled = enabled)
    }

    fun clearPersonalData() {
        // 模拟清除数据操作
    }
}

data class PrivacySettingsUiState(
    val isUsageStatsEnabled: Boolean = true,
    val isCrashReportEnabled: Boolean = true,
    val isThirdPartySharingEnabled: Boolean = false
)
