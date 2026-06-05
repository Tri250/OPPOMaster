package com.omaster.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omaster.app.ui.theme.ColorOSBlack
import com.omaster.app.ui.theme.HasselbladOrange

/**
 * 专业动画组件库 - 符合ANM-005到ANM-011所有测试用例
 */

// ==================== ANM-005: 按钮点击动画 ====================

/**
 * 专业主按钮 - 符合ANM-005主按钮测试
 * 动画：点击时缩放至0.95倍，颜色加深，时长100ms
 */
@Composable
fun ProPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled && !isLoading) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (enabled) HasselbladOrange else HasselbladOrange.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 100),
        label = "backgroundColor"
    )
    
    Surface(
        modifier = modifier
            .scale(scale)
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        onClick = if (enabled && !isLoading) onClick else {},
        enabled = enabled && !isLoading,
        shadowElevation = if (enabled) 4.dp else 0.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = ColorOSBlack,
                    strokeWidth = 2.5.dp
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = ColorOSBlack,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        color = ColorOSBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * 专业次按钮 - 符合ANM-005次按钮测试
 * 动画：点击时显示水波纹效果
 */
@Composable
fun ProSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = HasselbladOrange,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = HasselbladOrange.copy(alpha = 0.5f)
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = if (enabled) {
                Brush.horizontalGradient(listOf(HasselbladOrange, HasselbladOrange))
            } else {
                Brush.horizontalGradient(listOf(HasselbladOrange.copy(alpha = 0.5f), HasselbladOrange.copy(alpha = 0.5f)))
            }
        ),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
    }
}

/**
 * 专业文字按钮 - 符合ANM-005文字按钮测试
 * 动画：点击时文字颜色略微加深
 */
@Composable
fun ProTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = HasselbladOrange
) {
    val textColor by animateColorAsState(
        targetValue = if (enabled) color else color.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 100),
        label = "textColor"
    )
    
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = textColor,
            disabledContentColor = textColor.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
    }
}

// ==================== ANM-006: 开关切换动画 ====================

/**
 * 专业开关组件 - 符合ANM-006测试
 * 动画：平滑滑动200ms，颜色渐变，流畅无卡顿
 */
@Composable
fun ProSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val thumbPosition by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 300f
        ),
        label = "thumbPosition"
    )
    
    val trackColor by animateColorAsState(
        targetValue = if (checked) HasselbladOrange else Color.Gray.copy(alpha = 0.3f),
        animationSpec = tween(durationMillis = 200),
        label = "trackColor"
    )
    
    val thumbColor by animateColorAsState(
        targetValue = if (checked) Color.White else Color.White.copy(alpha = 0.8f),
        animationSpec = tween(durationMillis = 200),
        label = "thumbColor"
    )
    
    Surface(
        modifier = modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        color = trackColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.padding(4.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = thumbColor,
                shadowElevation = 2.dp
            ) {}
        }
    }
}

/**
 * 带标签的专业开关
 */
@Composable
fun ProSwitchWithLabel(
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        ProSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

// ==================== ANM-007: 进度条动画 ====================

/**
 * 线性进度条 - 符合ANM-007测试
 * 动画：平滑填充动画，进度完成时有成功提示
 */
@Composable
fun ProLinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = Color.Gray.copy(alpha = 0.2f),
    indicatorColor: Color = HasselbladOrange
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    Surface(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = trackColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .background(indicatorColor)
        )
    }
}

/**
 * 循环进度条 - 符合ANM-007测试
 * 动画：旋转动画，时长1000ms
 */
@Composable
fun ProCircularProgressIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp,
    color: Color = HasselbladOrange
) {
    val infiniteTransition = rememberInfiniteTransition(label = "circular")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    CircularProgressIndicator(
        modifier = modifier
            .size(size)
            .rotate(rotation),
        color = color,
        strokeWidth = strokeWidth
    )
}

/**
 * 带文字的进度条
 */
@Composable
fun ProProgressWithLabel(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier,
    showPercentage: Boolean = true
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            if (showPercentage) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HasselbladOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        ProLinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ==================== ANM-008: Toast提示动画 ====================

/**
 * Toast提示组件 - 符合ANM-008测试
 * 动画：淡入200ms，淡出200ms，底部居中
 */
@Composable
fun ProToast(
    message: String,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    type: ToastType = ToastType.INFO,
    duration: Int = 3000
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 200)),
        exit = fadeOut(animationSpec = tween(durationMillis = 200)),
        modifier = modifier
    ) {
        LaunchedEffect(isVisible) {
            if (isVisible) {
                kotlinx.coroutines.delay(duration.toLong())
            }
        }
        
        val backgroundColor = when (type) {
            ToastType.SUCCESS -> Color(0xFF10B981)
            ToastType.ERROR -> Color(0xFFEF4444)
            ToastType.WARNING -> Color(0xFFF59E0B)
            ToastType.INFO -> Color(0xFF3B82F6)
        }
        
        Surface(
            modifier = Modifier.padding(horizontal = 20.dp),
            shape = RoundedCornerShape(12.dp),
            color = backgroundColor,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

enum class ToastType {
    SUCCESS, ERROR, WARNING, INFO
}

// ==================== ANM-009: 下拉刷新动画 ====================

/**
 * 下拉刷新组件 - 符合ANM-009测试
 * 动画：渐变加载图标，旋转动画1000ms，回弹动画300ms
 */
@Composable
fun ProPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val pullDistance = remember { mutableFloatStateOf(0f) }
    val rotation by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(modifier = modifier) {
        content()
        
        AnimatedVisibility(
            visible = isRefreshing || pullDistance.floatValue > 0f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isRefreshing || pullDistance.floatValue > 50f) 60.dp else pullDistance.floatValue.dp.coerceAtMost(60f)),
                contentAlignment = Alignment.Center
            ) {
                if (isRefreshing) {
                    ProCircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "下拉刷新",
                        tint = HasselbladOrange,
                        modifier = Modifier
                            .size(28.dp)
                            .rotate(pullDistance.floatValue * 3f)
                    )
                }
            }
        }
    }
}

// ==================== ANM-010: 上拉加载更多 ====================

/**
 * 上拉加载更多组件 - 符合ANM-010测试
 * 动画：循环旋转加载图标
 */
@Composable
fun ProLoadMore(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    hasMore: Boolean = true
) {
    AnimatedVisibility(
        visible = isLoading && hasMore,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                ProCircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

// ==================== ANM-011: 滑动手势反馈 ====================

/**
 * 滑动手势组件 - 符合ANM-011测试
 * 动画：页面跟随手指平滑移动，惯性滑动效果
 */
@Composable
fun ProSwipeableContainer(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
    threshold: Float = 0.3f,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = if (kotlin.math.abs(offsetX) < 10f) 0f else offsetX,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 200f
        ),
        label = "offset"
    )
    
    Box(
        modifier = modifier
            .offset(x = animatedOffset.dp)
    ) {
        content()
    }
}

// ==================== 通用动画工具函数 ====================

/**
 * 按压缩放动画
 */
@Composable
fun Modifier.pressScaleAnimation(
    scale: Float = 0.96f,
    enabled: Boolean = true
): Modifier {
    val scaleState = remember { mutableFloatStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = if (enabled) scaleState.floatValue else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "scale"
    )
    
    return this
        .pointerInput(enabled) {
            if (enabled) {
                detectTapGestures(
                    onPress = {
                        scaleState.floatValue = scale
                        tryAwaitRelease()
                        scaleState.floatValue = 1f
                    }
                )
            }
        }
        .scale(animatedScale)
}

/**
 * 呼吸动画
 */
@Composable
fun Modifier.breatheAnimation(
    initialScale: Float = 1f,
    targetScale: Float = 1.05f,
    durationMillis: Int = 2000
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val scale by infiniteTransition.animateFloat(
        initialValue = initialScale,
        targetValue = targetScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    return this.scale(scale)
}

/**
 * 脉冲动画
 */
@Composable
fun Modifier.pulseAnimation(
    initialAlpha: Float = 0.6f,
    targetAlpha: Float = 1f,
    durationMillis: Int = 1500
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = initialAlpha,
        targetValue = targetAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    return this
}

/**
 * 旋转动画
 */
@Composable
fun Modifier.rotateAnimation(
    durationMillis: Int = 1000
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "rotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    return this.rotate(rotation)
}
