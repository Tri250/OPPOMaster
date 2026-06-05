package com.omaster.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omaster.app.model.CameraParams
import com.omaster.app.ui.animation.ColorOSAnimationDuration
import com.omaster.app.ui.animation.ColorOSScale
import com.omaster.app.ui.theme.*

/**
 * 参数应用状态
 */
sealed class ParamApplyState {
    object Idle : ParamApplyState()
    data class Applying(val params: CameraParams) : ParamApplyState()
    data class Success(
        val oldParams: Map<String, String>,
        val newParams: Map<String, String>,
        val appliedParams: CameraParams
    ) : ParamApplyState()
    data class Failed(val error: String) : ParamApplyState()
}

/**
 * 参数应用反馈卡片 - P0-1 功能改善
 * 显示参数差异对比、写入成功确认、撤销功能
 */
@Composable
fun ParamApplyFeedbackCard(
    state: ParamApplyState,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state != ParamApplyState.Idle,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 400f
            )
        ) + fadeIn(animationSpec = tween(ColorOSAnimationDuration.MEDIUM)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(ColorOSAnimationDuration.FAST)
        ) + fadeOut(animationSpec = tween(ColorOSAnimationDuration.FAST)),
        modifier = modifier
    ) {
        when (state) {
            is ParamApplyState.Applying -> ApplyingCard(state.params)
            is ParamApplyState.Success -> SuccessCard(
                oldParams = state.oldParams,
                newParams = state.newParams,
                appliedParams = state.appliedParams,
                onUndo = onUndo,
                onDismiss = onDismiss
            )
            is ParamApplyState.Failed -> FailedCard(
                error = state.error,
                onDismiss = onDismiss
            )
            is ParamApplyState.Idle -> {}
        }
    }
}

@Composable
private fun ApplyingCard(params: CameraParams) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // 加载动画
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Colors.HasselbladOrange,
                strokeWidth = 3.dp
            )
            
            Column {
                Text(
                    text = "正在应用参数...",
                    style = Typography.TitleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Colors.OnSurface
                )
                Text(
                    text = params.formatParamsForDisplay(),
                    style = Typography.BodySmall,
                    color = Colors.OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SuccessCard(
    oldParams: Map<String, String>,
    newParams: Map<String, String>,
    appliedParams: CameraParams,
    onUndo: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }
    
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            // 成功标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Colors.AccentGreen.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "成功",
                            tint = Colors.AccentGreen,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(4.dp)
                        )
                    }
                    
                    Text(
                        text = "参数已应用",
                        style = Typography.TitleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Colors.AccentGreen
                    )
                }
                
                // 撤销按钮
                var isPressed by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) ColorOSScale.Pressed else 1f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                    label = "undoScale"
                )
                
                Surface(
                    modifier = Modifier.scale(scale),
                    shape = RoundedCornerShape(8.dp),
                    color = Colors.HasselbladOrange.copy(alpha = 0.15f),
                    onClick = {
                        isPressed = true
                        onUndo()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "撤销",
                            tint = Colors.HasselbladOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "撤销",
                            style = Typography.LabelMedium,
                            fontWeight = FontWeight.Medium,
                            color = Colors.HasselbladOrange
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // 参数摘要
            Text(
                text = appliedParams.formatParamsForDisplay(),
                style = Typography.BodyMedium,
                color = Colors.OnSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // 展开详情按钮
            TextButton(
                onClick = { showDetails = !showDetails }
            ) {
                Text(
                    text = if (showDetails) "隐藏详情" else "查看参数变化",
                    style = Typography.LabelMedium,
                    color = Colors.HasselbladOrange
                )
                Icon(
                    imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Colors.HasselbladOrange,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            // 参数差异详情
            AnimatedVisibility(
                visible = showDetails,
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                ) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut()
            ) {
                ParamDiffList(
                    oldParams = oldParams,
                    newParams = newParams
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // 关闭按钮
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "关闭",
                    style = Typography.LabelMedium,
                    color = Colors.OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ParamDiffList(
    oldParams: Map<String, String>,
    newParams: Map<String, String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Colors.GlassBackground.copy(alpha = 0.3f))
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = "参数变化对比",
            style = Typography.LabelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Colors.HasselbladOrange
        )
        
        // 显示关键参数变化
        val keyParams = listOf("ISO", "快门", "曝光补偿", "白平衡", "焦距", "光圈")
        
        keyParams.forEach { key ->
            val oldValue = oldParams[key] ?: "-"
            val newValue = newParams[key] ?: "-"
            
            if (oldValue != newValue) {
                ParamDiffItem(
                    label = key,
                    oldValue = oldValue,
                    newValue = newValue
                )
            }
        }
    }
}

@Composable
private fun ParamDiffItem(
    label: String,
    oldValue: String,
    newValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = Typography.BodySmall,
            color = Colors.OnSurfaceVariant
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 旧值
            Text(
                text = oldValue,
                style = Typography.BodySmall,
                color = Colors.OnSurfaceVariant.copy(alpha = 0.6f)
            )
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "变化",
                tint = Colors.HasselbladOrange,
                modifier = Modifier.size(16.dp)
            )
            
            // 新值 - 高亮
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Colors.HasselbladOrange.copy(alpha = 0.15f)
            ) {
                Text(
                    text = newValue,
                    style = Typography.LabelMedium,
                    fontWeight = FontWeight.Medium,
                    color = Colors.HasselbladOrange,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun FailedCard(
    error: String,
    onDismiss: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Colors.AccentRed.copy(alpha = 0.15f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "失败",
                        tint = Colors.AccentRed,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp)
                    )
                }
                
                Text(
                    text = "参数应用失败",
                    style = Typography.TitleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Colors.AccentRed
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            Text(
                text = error,
                style = Typography.BodyMedium,
                color = Colors.OnSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // 建议
            Text(
                text = "建议：请确保已开启无障碍服务权限，并打开相机应用的专业模式。",
                style = Typography.BodySmall,
                color = Colors.OnSurfaceVariant.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "关闭",
                    style = Typography.LabelMedium,
                    color = Colors.OnSurfaceVariant
                )
            }
        }
    }
}

/**
 * 参数应用进度指示器 - 用于底部栏
 */
@Composable
fun ParamApplyProgressIndicator(
    isApplying: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isApplying,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Colors.HasselbladOrange,
                strokeWidth = 2.dp
            )
            Text(
                text = "正在写入...",
                style = Typography.LabelSmall,
                color = Colors.HasselbladOrange
            )
        }
    }
}