package com.omaster.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omaster.app.floating.FloatingWindowManager
import com.omaster.app.floating.PermissionHelper
import com.omaster.app.ui.theme.*

/**
 * 悬浮窗权限申请对话框 - ColorOS 16风格
 * Float-001: 悬浮窗权限申请与授予
 */
@Composable
fun OverlayPermissionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    permissionHelper: PermissionHelper
) {
    val systemBrand = permissionHelper.getSystemBrand()
    val showSpecialGuidance = permissionHelper.shouldShowSpecialGuidance()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = OppoPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = OppoPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "需要悬浮窗权限",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "为了在相机上层显示参数信息，需要授予悬浮窗权限",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                
                // 安全说明
                Surface(
                    color = OppoGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = OppoGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "安全说明",
                                style = MaterialTheme.typography.titleSmall,
                                color = OppoGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "• 仅在您授权后生效，可随时关闭\n• 不会读取或收集任何隐私数据\n• 悬浮窗仅用于显示参数信息",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                
                // 特殊引导（ColorOS/OxygenOS）
                if (showSpecialGuidance) {
                    Surface(
                        color = HasselbladOrange.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = HasselbladOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "$systemBrand 特殊说明",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = HasselbladOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (permissionHelper.isColorOS()) {
                                    """
                                    在ColorOS系统中，您可能还需要：
                                    1. 在「权限与隐私」中开启「自启动」
                                    2. 在「电池」中关闭「后台耗电优化」
                                    """.trimIndent()
                                } else {
                                    """
                                    在OxygenOS系统中，建议：
                                    1. 在「电池」设置中关闭优化
                                    2. 确保应用不会被后台清理
                                    """.trimIndent()
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
                
                // 适配品牌列表
                Surface(
                    color = CardBackground.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "支持的品牌",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BrandChip("OPPO")
                            BrandChip("一加")
                            BrandChip("Realme")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BrandChip("小米")
                            BrandChip("华为")
                            BrandChip("vivo")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OppoPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "去授权",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "暂不授权",
                    color = TextSecondary
                )
            }
        }
    )
}

@Composable
private fun BrandChip(brand: String) {
    Surface(
        color = OppoPrimary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = brand,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = OppoPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 悬浮窗功能受限提示 - Float-005: 后台保活场景
 */
@Composable
fun FloatingWindowRestrictedDialog(
    reason: String,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = ErrorRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "悬浮窗功能受限",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                
                Surface(
                    color = HasselbladOrange.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "建议操作",
                            style = MaterialTheme.typography.titleSmall,
                            color = HasselbladOrange,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• 授予悬浮窗权限\n• 允许应用后台运行\n• 关闭后台耗电优化",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
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
                Text(
                    text = "去设置",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "知道了",
                    color = TextSecondary
                )
            }
        }
    )
}

/**
 * 悬浮窗使用提示 - Float-004: 交互操作说明
 */
@Composable
fun FloatingWindowUsageTip(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = OppoPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = OppoPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "悬浮窗使用提示",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "悬浮窗已开启，您可以在相机上层查看参数",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                
                Surface(
                    color = CardBackground.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TipItem(
                            icon = Icons.Default.TouchApp,
                            title = "点击",
                            description = "展开/收起悬浮窗"
                        )
                        TipItem(
                            icon = Icons.Default.TouchApp,
                            title = "双击",
                            description = "关闭悬浮窗"
                        )
                        TipItem(
                            icon = Icons.Default.PanTool,
                            title = "拖动",
                            description = "移动悬浮窗位置"
                        )
                        TipItem(
                            icon = Icons.Default.ContentCopy,
                            title = "一键复制",
                            description = "复制所有参数到剪贴板"
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OppoPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "知道了",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun TipItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OppoPrimary,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
