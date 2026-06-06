package com.omaster.app.floating

import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.omaster.app.model.Preset
import com.omaster.app.ui.theme.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * 悬浮窗手势配置
 */
object FloatingGestureConfig {
    const val DOUBLE_CLICK_DELAY = 300L          // 双击间隔时间（毫秒）
    const val LONG_PRESS_DELAY = 500L            // 长按触发时间（毫秒）
    const val EDGE_SNAP_THRESHOLD = 100f         // 边缘吸附阈值
    const val SWIPE_THRESHOLD = 50f              // 滑动手势阈值
    const val ANIMATION_DURATION = 300           // 动画持续时间
    const val MIN_OPACITY = 0.3f                 // 最小透明度
    const val MAX_OPACITY = 1.0f                 // 最大透明度
}

/**
 * 悬浮窗手势状态
 */
sealed class FloatingGestureState {
    object Idle : FloatingGestureState()
    object Dragging : FloatingGestureState()
    object LongPressing : FloatingGestureState()
    data class Swiping(val direction: SwipeDirection) : FloatingGestureState()
}

/**
 * 滑动方向
 */
enum class SwipeDirection {
    LEFT, RIGHT, UP, DOWN
}

/**
 * 快捷操作菜单项
 */
data class QuickActionItem(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color = Color.Unspecified
)

/**
 * 完整的悬浮窗容器组件
 */
@Composable
fun FloatingWindowContainer(
    isExpanded: Boolean,
    currentPreset: Preset?,
    presets: List<Preset>,
    opacity: Float,
    onExpandToggle: () -> Unit,
    onClose: () -> Unit,
    onPresetSelect: (Preset) -> Unit,
    onFavoriteToggle: () -> Unit,
    onSharePreset: () -> Unit,
    onCopyParams: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val view = LocalView.current

    // 屏幕尺寸
    val screenWidth = remember { FloatingWindowManager.getScreenSize(context).first }
    val screenHeight = remember { FloatingWindowManager.getScreenSize(context).second }

    // 位置状态
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isRightSide by remember { mutableStateOf(true) }

    // 手势状态
    var gestureState by remember { mutableStateOf<FloatingGestureState>(FloatingGestureState.Idle) }
    var showQuickMenu by remember { mutableStateOf(false) }

    // 动画状态
    val animatedOffsetX by animateFloatAsState(
        targetValue = when (gestureState) {
            is FloatingGestureState.Dragging -> offsetX
            else -> if (isRightSide) screenWidth - 80f else 0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offsetX"
    )

    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offsetY"
    )

    // 加载保存的位置
    LaunchedEffect(Unit) {
        val settings = FloatingWindowManager.settings.value
        offsetX = settings.position.x.toFloat()
        offsetY = settings.position.y.toFloat()
        isRightSide = settings.position.isRightSide
    }

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    animatedOffsetX.roundToInt(),
                    animatedOffsetY.roundToInt()
                )
            }
            .alpha(opacity)
    ) {
        // 内容区域
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                scaleIn(
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                    initialScale = 0.8f
                ) + fadeIn(tween(200)) togetherWith
                scaleOut(
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                    targetScale = 0.8f
                ) + fadeOut(tween(200))
            },
            label = "expandCollapse"
        ) { expanded ->
            if (expanded) {
                ExpandedFloatingWindowWithGestures(
                    currentPreset = currentPreset,
                    presets = presets,
                    onPresetSelect = onPresetSelect,
                    onFavoriteToggle = onFavoriteToggle,
                    onSharePreset = onSharePreset,
                    onCopyParams = onCopyParams,
                    onCollapse = onExpandToggle,
                    onClose = onClose,
                    onSwipeLeft = onSwipeLeft,
                    onSwipeRight = onSwipeRight,
                    opacity = opacity
                )
            } else {
                CollapsedFloatingBallWithGestures(
                    onClick = onExpandToggle,
                    onDoubleClick = onClose,
                    onLongPress = { showQuickMenu = true },
                    isFavorite = currentPreset?.isFavorite == true
                )
            }
        }

        // 手势识别层
        if (!isExpanded) {
            FloatingGestureDetector(
                onDragStart = {
                    gestureState = FloatingGestureState.Dragging
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDragEnd = {
                    gestureState = FloatingGestureState.Idle
                    // 边缘吸附逻辑
                    val centerX = offsetX + 40.dp.value * density.density
                    isRightSide = centerX > screenWidth / 2

                    // 保存位置
                    FloatingWindowManager.updatePosition(
                        offsetX.roundToInt(),
                        offsetY.roundToInt(),
                        isRightSide
                    )

                    // 更新窗口位置
                    FloatingWindowManager.updateWindowPosition(
                        offsetX.roundToInt(),
                        offsetY.roundToInt(),
                        isRightSide
                    )
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y

                    // 边界检测
                    val minY = 100f
                    val maxY = screenHeight - 200f
                    offsetY = offsetY.coerceIn(minY, maxY)

                    // X轴边界
                    offsetX = offsetX.coerceIn(-50f, screenWidth - 50f)
                },
                onDoubleClick = onClose,
                onLongPress = {
                    showQuickMenu = true
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onSwipeLeft = onSwipeLeft,
                onSwipeRight = onSwipeRight
            )
        }

        // 快捷菜单
        if (showQuickMenu) {
            QuickActionMenu(
                isFavorite = currentPreset?.isFavorite == true,
                onDismiss = { showQuickMenu = false },
                onFavoriteToggle = {
                    onFavoriteToggle()
                    showQuickMenu = false
                },
                onSharePreset = {
                    onSharePreset()
                    showQuickMenu = false
                },
                onCopyParams = {
                    onCopyParams()
                    showQuickMenu = false
                },
                onClose = {
                    onClose()
                    showQuickMenu = false
                }
            )
        }
    }
}

/**
 * 悬浮窗手势检测器
 */
@Composable
fun FloatingGestureDetector(
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
    onDoubleClick: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var longPressJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                coroutineScope {
                    detectDragGestures(
                        onDragStart = { onDragStart() },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() },
                        onDrag = onDrag
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTime < FloatingGestureConfig.DOUBLE_CLICK_DELAY) {
                            // 双击
                            longPressJob?.cancel()
                            onDoubleClick()
                        } else {
                            // 开始长按检测
                            longPressJob = scope.launch {
                                delay(FloatingGestureConfig.LONG_PRESS_DELAY)
                                onLongPress()
                            }
                        }
                        lastClickTime = currentTime
                    },
                    onDoubleTap = {
                        longPressJob?.cancel()
                        onDoubleClick()
                    },
                    onLongPress = {
                        longPressJob?.cancel()
                        onLongPress()
                    }
                )
            }
    )
}

/**
 * 带手势的展开悬浮窗
 */
@Composable
fun ExpandedFloatingWindowWithGestures(
    currentPreset: Preset?,
    presets: List<Preset>,
    onPresetSelect: (Preset) -> Unit,
    onFavoriteToggle: () -> Unit,
    onSharePreset: () -> Unit,
    onCopyParams: () -> Unit,
    onCollapse: () -> Unit,
    onClose: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    opacity: Float
) {
    val hapticFeedback = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }
    var showOpacitySlider by remember { mutableStateOf(false) }
    var swipeOffset by remember { mutableFloatStateOf(0f) }

    Card(
        modifier = Modifier
            .width(320.dp)
            .wrapContentHeight()
            .alpha(opacity)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            swipeOffset < -FloatingGestureConfig.SWIPE_THRESHOLD -> {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSwipeLeft()
                            }
                            swipeOffset > FloatingGestureConfig.SWIPE_THRESHOLD -> {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSwipeRight()
                            }
                        }
                        swipeOffset = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        swipeOffset += dragAmount
                    }
                )
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 预设图标
                    Surface(
                        color = AccentPrimary.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = currentPreset?.name ?: "OMaster",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        currentPreset?.deviceModel?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row {
                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onFavoriteToggle()
                        }
                    ) {
                        Icon(
                            imageVector = if (currentPreset?.isFavorite == true)
                                Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (currentPreset?.isFavorite == true) "取消收藏" else "收藏",
                            tint = if (currentPreset?.isFavorite == true)
                                AccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多选项",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onCollapse) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "收起",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 参数卡片
            currentPreset?.let { preset ->
                PresetParamsCard(preset = preset)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 透明度控制
            AnimatedVisibility(
                visible = showOpacitySlider,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Text(
                        text = "透明度: ${(opacity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = opacity,
                        onValueChange = { FloatingWindowManager.setOpacity(it) },
                        valueRange = 0.3f..1.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 底部操作栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 复制按钮
                FilledTonalButton(
                    onClick = onCopyParams,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("复制参数")
                }

                // 透明度按钮
                IconButton(
                    onClick = { showOpacitySlider = !showOpacitySlider },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Opacity,
                        contentDescription = "透明度",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 关闭按钮
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = ErrorPro
                    )
                }
            }

            // 滑动提示
            if (presets.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "左右滑动切换预设",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    // 更多菜单
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("分享预设") },
            leadingIcon = {
                Icon(Icons.Default.Share, contentDescription = null)
            },
            onClick = {
                onSharePreset()
                showMenu = false
            }
        )
        DropdownMenuItem(
            text = { Text(if (currentPreset?.isFavorite == true) "取消收藏" else "添加收藏") },
            leadingIcon = {
                Icon(
                    if (currentPreset?.isFavorite == true)
                        Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null
                )
            },
            onClick = {
                onFavoriteToggle()
                showMenu = false
            }
        )
        DropdownMenuItem(
            text = { Text("复制参数") },
            leadingIcon = {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
            },
            onClick = {
                onCopyParams()
                showMenu = false
            }
        )
        Divider()
        DropdownMenuItem(
            text = { Text("关闭悬浮窗") },
            leadingIcon = {
                Icon(Icons.Default.Close, contentDescription = null)
            },
            onClick = {
                onClose()
                showMenu = false
            }
        )
    }
}

/**
 * 参数卡片
 */
@Composable
fun PresetParamsCard(preset: Preset) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ISO
            ParamRow(
                icon = Icons.Default.Exposure,
                label = "ISO",
                value = preset.cameraParams?.iso?.toString() ?: "自动",
                color = ColorISO
            )
            // 快门
            ParamRow(
                icon = Icons.Default.Timer,
                label = "快门",
                value = preset.cameraParams?.shutter ?: "自动",
                color = ColorShutter
            )
            // EV
            ParamRow(
                icon = Icons.Default.Exposure,
                label = "EV",
                value = preset.cameraParams?.ev ?: "0",
                color = ColorEV
            )
            // WB
            ParamRow(
                icon = Icons.Default.WbTwilight,
                label = "白平衡",
                value = preset.cameraParams?.wb ?: "自动",
                color = ColorWB
            )
        }
    }
}

/**
 * 参数行
 */
@Composable
fun ParamRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 带手势的收起悬浮球
 */
@Composable
fun CollapsedFloatingBallWithGestures(
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onLongPress: () -> Unit,
    isFavorite: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = CircleShape,
        color = AccentPrimary,
        shadowElevation = 8.dp,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.PhotoCamera,
                contentDescription = "悬浮球",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * 快捷操作菜单
 */
@Composable
fun QuickActionMenu(
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onSharePreset: () -> Unit,
    onCopyParams: () -> Unit,
    onClose: () -> Unit
) {
    val actions = listOf(
        QuickActionItem(
            id = "favorite",
            label = if (isFavorite) "取消收藏" else "添加收藏",
            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            tint = if (isFavorite) AccentPrimary else Color.Unspecified
        ),
        QuickActionItem(
            id = "share",
            label = "分享预设",
            icon = Icons.Default.Share
        ),
        QuickActionItem(
            id = "copy",
            label = "复制参数",
            icon = Icons.Default.ContentCopy
        ),
        QuickActionItem(
            id = "close",
            label = "关闭悬浮窗",
            icon = Icons.Default.Close,
            tint = ErrorPro
        )
    )

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                actions.forEach { action ->
                    QuickActionItemView(
                        item = action,
                        onClick = {
                            when (action.id) {
                                "favorite" -> onFavoriteToggle()
                                "share" -> onSharePreset()
                                "copy" -> onCopyParams()
                                "close" -> onClose()
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * 快捷操作项视图
 */
@Composable
fun QuickActionItemView(
    item: QuickActionItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = if (item.tint != Color.Unspecified) item.tint
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.tint != Color.Unspecified) item.tint
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 边缘吸附动画效果
 */
@Composable
fun EdgeSnapEffect(
    isSnapping: Boolean,
    isRightSide: Boolean,
    content: @Composable () -> Unit
) {
    val offsetX by animateFloatAsState(
        targetValue = if (isSnapping) {
            if (isRightSide) 0f else -20f
        } else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "edgeSnap"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            translationX = offsetX
        }
    ) {
        content()
    }
}

/**
 * 悬浮窗拖拽指示器
 */
@Composable
fun DragIndicator(
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isDragging,
        enter = fadeIn() + expandIn(),
        exit = fadeOut() + shrinkOut(),
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = AccentPrimary.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "拖动中",
                    tint = AccentPrimary
                )
            }
        }
    }
}

// ==================== 原始组件兼容层 ====================

/**
 * 原始展开悬浮窗（兼容旧代码）
 */
@Composable
fun ExpandedFloatingWindow(
    currentPreset: Preset?,
    presets: List<Preset>,
    onPresetSelect: (Preset) -> Unit,
    onFavoriteToggle: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onCollapse: () -> Unit,
    onClose: () -> Unit,
    opacity: Float
) {
    ExpandedFloatingWindowWithGestures(
        currentPreset = currentPreset,
        presets = presets,
        onPresetSelect = onPresetSelect,
        onFavoriteToggle = onFavoriteToggle,
        onSharePreset = {},
        onCopyParams = {},
        onCollapse = onCollapse,
        onClose = onClose,
        onSwipeLeft = onSwipeLeft,
        onSwipeRight = onSwipeRight,
        opacity = opacity
    )
}

/**
 * 原始收起悬浮球（兼容旧代码）
 */
@Composable
fun CollapsedFloatingBall(
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    isFavorite: Boolean
) {
    CollapsedFloatingBallWithGestures(
        onClick = onClick,
        onDoubleClick = onDoubleClick,
        onLongPress = {},
        isFavorite = isFavorite
    )
}

/**
 * 预设预览卡片（兼容旧代码）
 */
@Composable
fun PresetPreviewCard(
    preset: Preset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = AccentPrimary
            )
        } else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Photo,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isSelected) AccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) AccentPrimary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 可拖拽表面（兼容旧代码）
 */
@Composable
fun DraggableSurface(
    offsetX: Float,
    offsetY: Float,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (change: PointerInputChange) -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDrag = { change, _ -> onDrag(change) }
                )
            }
    ) {
        content()
    }
}

/**
 * 水平分页器（兼容旧代码）
 */
@Composable
fun HorizontalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentKey: (Int) -> Any,
    content: @Composable (Int) -> Unit
) {
    Box(modifier = modifier) {
        content(state.currentPage)
    }
}

/**
 * 分页器状态（兼容旧代码）
 */
@Stable
class PagerState(
    currentPage: Int,
    pageCount: () -> Int
) {
    var currentPage by mutableIntStateOf(currentPage)
        private set

    val pageCount: () -> Int = pageCount

    suspend fun animateScrollToPage(page: Int) {
        currentPage = page
    }
}

/**
 * 记住分页器状态（兼容旧代码）
 */
@Stable
@Composable
fun rememberPagerState(
    initialPage: Int = 0,
    pageCount: () -> Int
): PagerState {
    return remember {
        PagerState(
            currentPage = initialPage,
            pageCount = { pageCount() }
        )
    }
}