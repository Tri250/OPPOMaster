package com.omaster.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.omaster.app.ui.theme.*
import com.omaster.app.ui.theme.OMasterColors as Colors
import com.omaster.app.ui.theme.OMasterSpacing as Spacing
import com.omaster.app.ui.theme.OMasterRadius as Radius
import com.omaster.app.ui.theme.OMasterElevation as Elevation
import com.omaster.app.ui.theme.OMasterTypography as Typography

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val backgroundColor = animateColorAsState(
        targetValue = if (selected) {
            Colors.GlassBackground.copy(alpha = 0.4f)
        } else {
            Colors.GlassBackground.copy(alpha = 0.25f)
        },
        animationSpec = tween(300),
        label = "background"
    )
    
    val borderColor = animateColorAsState(
        targetValue = if (selected) {
            Colors.HasselbladOrange.copy(alpha = 0.6f)
        } else {
            Colors.GlassBorder.copy(alpha = if (enabled) 1f else 0.5f)
        },
        animationSpec = tween(300),
        label = "border"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "scale"
    )
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "pressScale"
    )
    
    Surface(
        modifier = modifier
            .scale(scale * pressScale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick ?: {}
            ),
        shape = RoundedCornerShape(Radius.Card),
        color = Color.Transparent,
        shadowElevation = Elevation.Card
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            backgroundColor.value.copy(alpha = 0.3f),
                            backgroundColor.value.copy(alpha = 0.1f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            borderColor.value.copy(alpha = 0.5f),
                            borderColor.value.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(Radius.Card)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.CardPadding),
                content = content
            )
        }
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    isLoading: Boolean = false
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (enabled) {
            Colors.HasselbladOrange.copy(alpha = 0.9f)
        } else {
            Colors.HasselbladOrange.copy(alpha = 0.4f)
        },
        animationSpec = tween(300),
        label = "background"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.95f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "scale"
    )
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "pressScale"
    )
    
    Surface(
        modifier = modifier
            .scale(scale * pressScale)
            .height(52.dp)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onTap = { onClick() }
                    )
                }
            },
        shape = RoundedCornerShape(Radius.Button),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            backgroundColor,
                            backgroundColor.copy(alpha = 0.8f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Colors.HasselbladOrange.copy(alpha = 0.8f),
                            Colors.HasselbladOrange.copy(alpha = 0.4f)
                        )
                    ),
                    shape = RoundedCornerShape(Radius.Button)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Colors.OnPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Colors.OnPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        style = Typography.LabelLarge,
                        color = Colors.OnPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    size: Dp = 48.dp
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "scale"
    )
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "pressScale"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale * pressScale)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onTap = { onClick() }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(size * 0.5f),
            tint = if (enabled) Colors.OnSurface else Colors.Disabled
        )
    }
}

@Composable
fun GlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            Colors.HasselbladOrange.copy(alpha = 0.3f)
        } else {
            Colors.GlassBackground.copy(alpha = 0.2f)
        },
        animationSpec = tween(200),
        label = "background"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            Colors.HasselbladOrange.copy(alpha = 0.6f)
        } else {
            Colors.GlassBorder.copy(alpha = 0.5f)
        },
        animationSpec = tween(200),
        label = "border"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (selected) {
            Colors.HasselbladOrange
        } else {
            Colors.OnSurface
        },
        animationSpec = tween(200),
        label = "textColor"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "scale"
    )
    
    Surface(
        modifier = modifier
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            },
        shape = RoundedCornerShape(Radius.Chip),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(backgroundColor)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(Radius.Chip)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = textColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = text,
                    style = Typography.LabelMedium,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true
) {
    val progress by animateFloatAsState(
        targetValue = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start),
        animationSpec = tween(100),
        label = "progress"
    )
    
    val thumbScale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "thumbScale"
    )
    
    Box(
        modifier = modifier
            .height(32.dp)
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.Center)
                .background(
                    color = Colors.GlassBackground.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(2.dp)
                )
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(4.dp)
                .align(Alignment.CenterStart)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Colors.HasselbladOrange.copy(alpha = 0.6f),
                            Colors.HasselbladOrange
                        )
                    ),
                    shape = RoundedCornerShape(2.dp)
                )
        )
        
        Box(
            modifier = Modifier
                .size((12 * thumbScale).dp)
                .align(Alignment.CenterStart)
                .offset(x = (progress * 200).dp - 6.dp)
                .background(
                    color = Colors.HasselbladOrange,
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = Colors.OnPrimary,
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun GlassBottomSheet(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(
            topStart = Radius.BottomSheet,
            topEnd = Radius.BottomSheet
        ),
        color = Color.Transparent,
        shadowElevation = Elevation.BottomSheet
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Colors.Surface.copy(alpha = 0.95f),
                            Colors.Surface.copy(alpha = 0.98f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Colors.GlassBorder.copy(alpha = 0.3f),
                            Colors.GlassBorder.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(
                        topStart = Radius.BottomSheet,
                        topEnd = Radius.BottomSheet
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                content = content
            )
        }
    }
}

@Composable
fun GlassDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    text: String? = null,
    confirmButton: @Composable () -> Unit = {},
    dismissButton: @Composable () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = Color.Transparent,
        title = title?.let {
            {
                Text(
                    text = it,
                    style = Typography.HeadlineSmall,
                    color = Colors.OnSurface
                )
            }
        },
        text = text?.let {
            {
                Text(
                    text = it,
                    style = Typography.BodyMedium,
                    color = Colors.OnSurfaceVariant
                )
            }
        },
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}

@Composable
fun GlassProgressIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Dp = 3.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    CircularProgressIndicator(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation },
        color = Colors.HasselbladOrange,
        strokeWidth = strokeWidth,
        trackColor = Colors.GlassBackground.copy(alpha = 0.3f)
    )
}

@Composable
fun GlassDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier,
        color = Colors.Divider,
        thickness = 0.5.dp
    )
}

@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    baseColor: Color = Colors.SurfaceVariant,
    highlightColor: Color = Colors.SurfaceElevated
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    val brush = Brush.linearGradient(
        colors = listOf(
            baseColor,
            highlightColor.copy(alpha = 0.5f),
            baseColor
        ),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )
    
    Box(
        modifier = modifier.background(brush)
    )
}

@Composable
fun AnimatedVisibilityFadeSlide(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn(animationSpec = tween(300)) + slideInVertically(
        animationSpec = tween(300),
        initialOffsetY = { it / 4 }
    ),
    exit: ExitTransition = fadeOut(animationSpec = tween(300)) + slideOutVertically(
        animationSpec = tween(300),
        targetOffsetY = { it / 4 }
    ),
    label: String = "fadeSlide",
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
        label = label
    ) {
        content()
    }
}
