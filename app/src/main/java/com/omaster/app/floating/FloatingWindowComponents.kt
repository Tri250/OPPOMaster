package com.omaster.app.floating

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omaster.app.ui.theme.AccentPrimary

@Composable
fun FloatingWindowToggleButton(
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier
            .semantics {
                contentDescription = if (isEnabled) "关闭悬浮窗" else "开启悬浮窗"
                stateDescription = if (isEnabled) "悬浮窗已开启" else "悬浮窗已关闭"
            }
    ) {
        Icon(
            imageVector = if (isEnabled) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
            contentDescription = "悬浮窗",
            tint = if (isEnabled) AccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PermissionGuidanceDialog(
    systemBrand: String,
    specialGuidance: String,
    onDismiss: () -> Unit,
    onAuthorize: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = AccentPrimary
            )
        },
        title = {
            Text(
                text = "需要悬浮窗权限",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "OMaster需要悬浮窗权限来显示调色参数，方便您在拍照时快速参考。",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "当前系统: $systemBrand",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (specialGuidance.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = specialGuidance,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAuthorize,
                modifier = Modifier.semantics {
                    contentDescription = "前往授权"
                }
            ) {
                Text("去授权")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "取消授权"
                }
            ) {
                Text("取消")
            }
        }
    )
}

@Composable
fun OpacityAdjustmentSlider(
    currentOpacity: Float,
    onOpacityChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "调节透明度",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "30%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Slider(
                    value = currentOpacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0.3f..1f,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = "透明度调节滑块，当前值${(currentOpacity * 100).toInt()}%"
                        }
                )
                
                Text(
                    text = "100%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = "当前透明度: ${(currentOpacity * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("完成")
            }
        }
    }
}

@Composable
fun CategorySelectionPanel(
    selectedCategory: String,
    categories: List<String>,
    onCategorySelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "分类筛选",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            categories.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "选择分类：$category"
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = category == selectedCategory,
                        onClick = { onCategorySelect(category) }
                    )
                    
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FavoriteSyncIndicator(
    isSyncing: Boolean,
    lastSyncTime: Long,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isSyncing,
        modifier = modifier
    ) {
        Surface(
            color = AccentPrimary.copy(alpha = 0.1f),
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = AccentPrimary
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "同步中...",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentPrimary
                )
            }
        }
    }
}
