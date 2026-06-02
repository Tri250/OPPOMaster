package com.omaster.app.ui.effects

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import com.omaster.app.ui.animation.ColorOSAnimationDuration
import com.omaster.app.ui.animation.ColorOSEasing
import com.omaster.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ColorOSProEffectsConfig {
    val BlurRadiusLight = 16.dp
    val BlurRadiusMedium = 24.dp
    val BlurRadiusHeavy = 32.dp
    val BlurRadiusUltra = 48.dp
    
    val RippleDuration = 600
    val RippleMaxRadius = 120.dp
    val RippleMinRadius = 20.dp
    
    val ShadowElevationLow = 2.dp
    val ShadowElevationMedium = 4.dp
    val ShadowElevationHigh = 8.dp
    val ShadowElevationUltra = 16.dp
    
    val LinkUnderlineAnimationDuration = 200
}

fun Modifier.colorOSBlur(
    radius: Dp = ColorOSProEffectsConfig.BlurRadiusMedium,
    enabled: Boolean = true
): Modifier = composed {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && enabled) {
        this.graphicsLayer {
            renderEffect = RenderEffect
                .createBlurEffect(
                    radius.value,
                    radius.value,
                    Shader.TileMode.CLAMP
                )
                .asComposeRenderEffect()
        }
    } else {
        this.alpha(0.95f)
    }
}

fun Modifier.colorOSDynamicShadow(
    elevation: Dp = ColorOSProEffectsConfig.ShadowElevationMedium,
    shape: Shape = RoundedCornerShape(16.dp),
    isDark: Boolean = true,
    ambientColor: Color = Color.Black.copy(alpha = if (isDark) 0.4f else 0.2f),
    spotColor: Color = HasselbladOrange.copy(alpha = if (isDark) 0.15f else 0.1f)
): Modifier = composed {
    val shadowElevation by animateDpAsState(
        targetValue = elevation,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 200f
        ),
        label = "shadowElevation"
    )
    
    this
        .graphicsLayer {
            this.shadowElevation = shadowElevation.value
            this.ambientShadowColor = ambientColor
            this.spotShadowColor = spotColor
            this.shape = shape
        }
}

@Composable
fun rememberLiquidRippleState(): LiquidRippleState {
    val scope = rememberCoroutineScope()
    val state = remember { mutableStateListOf<RippleWave>() }
    
    return remember {
        LiquidRippleState(
            waves = state,
            addWave = { x, y ->
                scope.launch {
                    val wave = RippleWave(
                        id = System.currentTimeMillis(),
                        centerX = x,
                        centerY = y,
                        startTime = System.currentTimeMillis()
                    )
                    state.add(wave)
                    delay(ColorOSProEffectsConfig.RippleDuration.toLong())
                    state.remove(wave)
                }
            }
        )
    }
}

data class RippleWave(
    val id: Long,
    val centerX: Float,
    val centerY: Float,
    val startTime: Long
)

class LiquidRippleState(
    val waves: List<RippleWave>,
    val addWave: (Float, Float) -> Unit
)

@Composable
fun LiquidRippleCanvas(
    rippleState: LiquidRippleState,
    modifier: Modifier = Modifier,
    rippleColor: Color = HasselbladOrange.copy(alpha = 0.3f)
) {
    val duration = ColorOSProEffectsConfig.RippleDuration
    
    Canvas(modifier = modifier) {
        rippleState.waves.forEach { wave ->
            val elapsed = (System.currentTimeMillis() - wave.startTime).toFloat()
            val progress = elapsed / duration
            
            val maxRadius = ColorOSProEffectsConfig.RippleMaxRadius.value
            val currentRadius = maxRadius * progress
            
            val alpha = (1f - progress) * 0.4f
            
            drawCircle(
                color = rippleColor.copy(alpha = alpha),
                radius = currentRadius,
                center = Offset(wave.centerX, wave.centerY),
                style = Stroke(width = 2.dp.toPx())
            )
            
            if (progress < 0.5f) {
                drawCircle(
                    color = rippleColor.copy(alpha = alpha * 0.5f),
                    radius = currentRadius * 0.6f,
                    center = Offset(wave.centerX, wave.centerY),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorOSLiquidCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    rippleColor: Color = HasselbladOrange.copy(alpha = 0.25f),
    content: @Composable BoxScope.() -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val rippleState = rememberLiquidRippleState()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 300f
        ),
        label = "cardScale"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 0.dp else ColorOSProEffectsConfig.ShadowElevationMedium,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 200f
        ),
        label = "cardElevation"
    )
    
    val backgroundColor = if (isDark) ColorOSCard else ColorOSLightCard
    val borderColor = if (isDark) {
        Color.White.copy(alpha = if (isPressed) 0.2f else 0.1f)
    } else {
        Color.Black.copy(alpha = if (isPressed) 0.1f else 0.05f)
    }
    
    Box(
        modifier = modifier
            .scale(scale)
            .graphicsLayer {
                shadowElevation = elevation.value
                ambientShadowColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.15f)
                spotShadowColor = rippleColor.copy(alpha = 0.1f)
                shape = RoundedCornerShape(24.dp)
            }
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        rippleState.addWave(offset.x, offset.y)
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onTap = { onClick() }
                )
            }
    ) {
        content()
        
        LiquidRippleCanvas(
            rippleState = rippleState,
            modifier = Modifier.matchParentSize(),
            rippleColor = rippleColor
        )
    }
}

@Composable
fun ColorOSProLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    linkStyle: ColorOSLinkStyle = ColorOSLinkStyle.Primary,
    showUnderline: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val linkColor = when (linkStyle) {
        ColorOSLinkStyle.Primary -> HasselbladOrange
        ColorOSLinkStyle.Secondary -> if (isDark) ColorOSTextSecondary else ColorOSLightTextSecondary
        ColorOSLinkStyle.Accent -> AuroraGreen
    }
    
    var underlineWidth by remember { mutableStateOf(0f) }
    var underlineAlpha by remember { mutableStateOf(0f) }
    
    val animatedUnderlineWidth by animateFloatAsState(
        targetValue = if (isPressed) 1f else underlineWidth,
        animationSpec = tween(
            durationMillis = ColorOSProEffectsConfig.LinkUnderlineAnimationDuration,
            easing = ColorOSEasing.Standard
        ),
        label = "underlineWidth"
    )
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.7f else 1f,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.FAST,
            easing = ColorOSEasing.Standard
        ),
        label = "linkAlpha"
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 300f
        ),
        label = "linkScale"
    )
    
    Row(
        modifier = modifier
            .scale(animatedScale)
            .alpha(animatedAlpha)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
            .semantics {
                onClick(label = text) { onClick() }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = linkColor
            )
        }
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = linkColor,
            fontWeight = FontWeight.Medium,
            textDecoration = if (showUnderline && animatedUnderlineWidth > 0.5f) {
                TextDecoration.Underline
            } else null
        )
    }
    
    LaunchedEffect(showUnderline) {
        if (showUnderline) {
            delay(100)
            underlineWidth = 1f
            underlineAlpha = 1f
        }
    }
}

enum class ColorOSLinkStyle {
    Primary,
    Secondary,
    Accent
}

@Composable
fun ColorOSAnimatedLinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    backgroundColor: Color = HasselbladOrange.copy(alpha = 0.15f)
) {
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 300f
        ),
        label = "buttonScale"
    )
    
    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (isPressed) {
            HasselbladOrange.copy(alpha = 0.25f)
        } else {
            backgroundColor
        },
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.FAST,
            easing = ColorOSEasing.Standard
        ),
        label = "backgroundColor"
    )
    
    val animatedElevation by animateDpAsState(
        targetValue = if (isPressed) 0.dp else ColorOSProEffectsConfig.ShadowElevationLow,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 200f
        ),
        label = "buttonElevation"
    )
    
    Surface(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(12.dp),
        color = animatedBackgroundColor,
        shadowElevation = animatedElevation,
        border = BorderStroke(
            width = 1.dp,
            color = HasselbladOrange.copy(alpha = if (isPressed) 0.4f else 0.3f)
        ),
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = HasselbladOrange,
                fontWeight = FontWeight.SemiBold
            )
            
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = HasselbladOrange
            )
        }
    }
}

@Composable
fun ColorOSGlassBlurCard(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    blurRadius: Dp = ColorOSProEffectsConfig.BlurRadiusMedium,
    content: @Composable ColumnScope.() -> Unit
) {
    val backgroundColor = if (isDark) {
        ColorOSGlass.copy(alpha = 0.85f)
    } else {
        ColorOSLightGlass.copy(alpha = 0.9f)
    }
    
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }
    
    Surface(
        modifier = modifier
            .colorOSBlur(radius = blurRadius)
            .colorOSDynamicShadow(
                elevation = ColorOSProEffectsConfig.ShadowElevationMedium,
                shape = RoundedCornerShape(24.dp),
                isDark = isDark
            ),
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun ColorOSProFloatingButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    backgroundColor: Color = HasselbladOrange,
    contentColor: Color = ColorOSBlack,
    size: Dp = 56.dp
) {
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 350f
        ),
        label = "fabScale"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 4.dp else ColorOSProEffectsConfig.ShadowElevationHigh,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 200f
        ),
        label = "fabElevation"
    )
    
    val rippleState = rememberLiquidRippleState()
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .graphicsLayer {
                shadowElevation = elevation.value
                ambientShadowColor = Color.Black.copy(alpha = 0.3f)
                spotShadowColor = backgroundColor.copy(alpha = 0.2f)
                shape = CircleShape
            }
            .clip(CircleShape)
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        rippleState.addWave(offset.x, offset.y)
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(size * 0.45f)
        )
        
        LiquidRippleCanvas(
            rippleState = rippleState,
            modifier = Modifier.matchParentSize(),
            rippleColor = contentColor.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun ColorOSDepthCard(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    depthLevel: ColorOSDepthLevel = ColorOSDepthLevel.Medium,
    content: @Composable ColumnScope.() -> Unit
) {
    val elevation = when (depthLevel) {
        ColorOSDepthLevel.Flat -> 0.dp
        ColorOSDepthLevel.Low -> ColorOSProEffectsConfig.ShadowElevationLow
        ColorOSDepthLevel.Medium -> ColorOSProEffectsConfig.ShadowElevationMedium
        ColorOSDepthLevel.High -> ColorOSProEffectsConfig.ShadowElevationHigh
        ColorOSDepthLevel.Ultra -> ColorOSProEffectsConfig.ShadowElevationUltra
    }
    
    val backgroundColor = when (depthLevel) {
        ColorOSDepthLevel.Flat -> if (isDark) ColorOSCard else ColorOSLightCard
        ColorOSDepthLevel.Low -> if (isDark) ColorOSBlackElevated else ColorOSLightSurface
        ColorOSDepthLevel.Medium -> if (isDark) ColorOSCard else ColorOSLightCard
        ColorOSDepthLevel.High -> if (isDark) ColorOSCard.copy(alpha = 0.95f) else ColorOSLightSurface
        ColorOSDepthLevel.Ultra -> if (isDark) ColorOSGlass else ColorOSLightGlass
    }
    
    val borderColor = if (isDark) {
        Color.White.copy(alpha = when (depthLevel) {
            ColorOSDepthLevel.Flat -> 0.05f
            ColorOSDepthLevel.Low -> 0.08f
            ColorOSDepthLevel.Medium -> 0.1f
            ColorOSDepthLevel.High -> 0.12f
            ColorOSDepthLevel.Ultra -> 0.15f
        })
    } else {
        Color.Black.copy(alpha = when (depthLevel) {
            ColorOSDepthLevel.Flat -> 0.02f
            ColorOSDepthLevel.Low -> 0.04f
            ColorOSDepthLevel.Medium -> 0.05f
            ColorOSDepthLevel.High -> 0.06f
            ColorOSDepthLevel.Ultra -> 0.08f
        })
    }
    
    val cornerRadius = when (depthLevel) {
        ColorOSDepthLevel.Flat -> 16.dp
        ColorOSDepthLevel.Low -> 20.dp
        ColorOSDepthLevel.Medium -> 24.dp
        ColorOSDepthLevel.High -> 28.dp
        ColorOSDepthLevel.Ultra -> 32.dp
    }
    
    Surface(
        modifier = modifier
            .colorOSDynamicShadow(
                elevation = elevation,
                shape = RoundedCornerShape(cornerRadius),
                isDark = isDark
            )
            .then(
                if (depthLevel == ColorOSDepthLevel.Ultra) {
                    Modifier.colorOSBlur(ColorOSProEffectsConfig.BlurRadiusLight)
                } else Modifier
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
                .padding(16.dp),
            content = content
        )
    }
}

enum class ColorOSDepthLevel {
    Flat,
    Low,
    Medium,
    High,
    Ultra
}