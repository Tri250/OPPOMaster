package com.omaster.app.floating

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.omaster.app.model.Preset
import com.omaster.app.ui.theme.AccentPrimary
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.roundToInt

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
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    val screenWidthPx = with(density) { 400.dp.toPx() }
    val screenHeightPx = with(density) { 800.dp.toPx() }
    
    val targetX = if (offsetX > 0) screenWidthPx - 80f else 0f
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (!isDragging && offsetX != 0f) targetX else offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offsetX"
    )
    
    Box(
        modifier = modifier
            .offset { IntOffset(animatedOffsetX.roundToInt(), offsetY.roundToInt()) }
            .alpha(opacity)
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                scaleIn(
                    animationSpec = tween(150, easing = FastOutSlowInEasing),
                    initialScale = 0.8f
                ) + fadeIn(tween(150)) togetherWith
                scaleOut(
                    animationSpec = tween(150, easing = FastOutSlowInEasing),
                    targetScale = 0.8f
                ) + fadeOut(tween(150))
            },
            label = "expandCollapse"
        ) { expanded ->
            if (expanded) {
                ExpandedFloatingWindow(
                    currentPreset = currentPreset,
                    presets = presets,
                    onPresetSelect = onPresetSelect,
                    onFavoriteToggle = onFavoriteToggle,
                    onSwipeLeft = onSwipeLeft,
                    onSwipeRight = onSwipeRight,
                    onCollapse = onExpandToggle,
                    onClose = onClose,
                    opacity = opacity
                )
            } else {
                CollapsedFloatingBall(
                    onClick = onExpandToggle,
                    onDoubleClick = onClose,
                    isFavorite = currentPreset?.isFavorite == true
                )
            }
        }
        
        DraggableSurface(
            offsetX = offsetX,
            offsetY = offsetY,
            onDragStart = { isDragging = true },
            onDragEnd = {
                isDragging = false
                val newOffsetX = if (offsetX > screenWidthPx / 2) screenWidthPx - 80f else 0f
                offsetX = newOffsetX
            },
            onDrag = { change ->
                offsetX += change.x
                offsetY += change.y
                
                val safeAreaTop = 100f
                val safeAreaBottom = screenHeightPx - 200f
                offsetY = offsetY.coerceIn(safeAreaTop, safeAreaBottom)
            }
        )
    }
}

@Composable
private fun ExpandedFloatingWindow(
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
    val hapticFeedback = LocalHapticFeedback.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    
    Card(
        modifier = Modifier
            .width(300.dp)
            .wrapContentHeight()
            .semantics {
                contentDescription = "悬浮窗预设：${currentPreset?.name ?: "无"}"
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = opacity)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentPreset?.name ?: "OMaster",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row {
                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onFavoriteToggle()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = if (currentPreset?.isFavorite == true) "取消收藏" else "添加收藏"
                        }
                    ) {
                        Icon(
                            imageVector = if (currentPreset?.isFavorite == true) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (currentPreset?.isFavorite == true) AccentPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier.semantics {
                            contentDescription = "收起悬浮窗"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "收起",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            currentPreset?.deviceModel?.let { device ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = device,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            HorizontalPager(
                state = rememberPagerState(
                    initialPage = presets.indexOf(currentPreset).coerceAtLeast(0),
                    pageCount = { presets.size }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX < -100f) {
                                    onSwipeLeft()
                                } else if (offsetX > 100f) {
                                    onSwipeRight()
                                }
                                offsetX = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                offsetX += dragAmount
                            }
                        )
                    },
                contentKey = { presets.getOrNull(it)?.id ?: it }
            ) { page ->
                val preset = presets.getOrNull(page)
                preset?.let {
                    PresetPreviewCard(
                        preset = it,
                        isSelected = it.id == currentPreset?.id,
                        onClick = { onPresetSelect(it) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onSwipeRight,
                    modifier = Modifier.semantics {
                        contentDescription = "上一条预设"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "上一个",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                IconButton(
                    onClick = onSwipeLeft,
                    modifier = Modifier.semantics {
                        contentDescription = "下一条预设"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "下一个",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetPreviewCard(
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
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
            width = 2.dp,
            brush = androidx.compose.ui.graphics.SolidColor(AccentPrimary)
        ) else null
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

@Composable
private fun CollapsedFloatingBall(
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    isFavorite: Boolean
) {
    val hapticFeedback = LocalHapticFeedback.current
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    Surface(
        modifier = Modifier
            .size(48.dp)
            .semantics {
                contentDescription = "悬浮球"
            }
            .clickable(
                onClickLabel = "展开悬浮窗"
            ) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime < 300) {
                    onDoubleClick()
                } else {
                    onClick()
                }
                lastClickTime = currentTime
            },
        shape = CircleShape,
        color = AccentPrimary,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Default.PhotoCamera,
                contentDescription = "悬浮球",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun DraggableSurface(
    offsetX: Float,
    offsetY: Float,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (change: androidx.compose.ui.input.pointer.PointerInputChange) -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDrag = onDrag
                )
            }
    ) {
        content()
    }
}

@Composable
private fun HorizontalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentKey: (Int) -> Any,
    content: @Composable (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    Box(
        modifier = modifier
    ) {
        content(state.currentPage)
        
        LaunchedEffect(state.currentPage) {
            state.animateScrollToPage(state.currentPage)
        }
    }
}

@Stable
@Composable
private fun rememberPagerState(
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

@Stable
private class PagerState(
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
