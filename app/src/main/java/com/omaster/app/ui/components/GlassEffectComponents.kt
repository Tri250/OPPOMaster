package com.omaster.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omaster.app.ui.theme.*

/**
 * ==================== ColorOS 16 玻璃液态效果组件 ====================
 * 基于 Aquatic Design 设计理念
 */

/**
 * GlassCard - 玻璃液态卡片
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    roundedCorners: Dp = 24.dp,
    borderVisible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val backgroundColor = if (isDark) ColorOSGlass else ColorOSLightGlass
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.15f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(roundedCorners),
        color = backgroundColor,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (borderVisible) {
                        Modifier.border(
                            width = 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(roundedCorners)
                        )
                    } else {
                        Modifier
                    }
                ),
            content = content
        )
    }
}

/**
 * GlassButton - 玻璃液态按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    primaryColor: Color = HasselbladOrange,
    enabled: Boolean = true
) {
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
    
    val backgroundColor = if (isDark) {
        if (isPressed) primaryColor.copy(alpha = 0.3f) else primaryColor.copy(alpha = 0.2f)
    } else {
        if (isPressed) primaryColor.copy(alpha = 0.25f) else primaryColor.copy(alpha = 0.15f)
    }
    
    val borderColor = if (isDark) {
        primaryColor.copy(alpha = 0.3f)
    } else {
        primaryColor.copy(alpha = 0.2f)
    }
    
    Card(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = text,
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = primaryColor
            )
        }
    }
}

/**
 * GlassToggle - 玻璃态开关
 */
@Composable
fun GlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    activeColor: Color = HasselbladOrange
) {
    val thumbPosition by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 200f
        ),
        label = "togglePosition"
    )
    
    val backgroundColor = if (isDark) {
        if (checked) activeColor.copy(alpha = 0.3f) else ColorOSGrey700.copy(alpha = 0.5f)
    } else {
        if (checked) activeColor.copy(alpha = 0.2f) else ColorOSGrey200.copy(alpha = 0.8f)
    }
    
    val trackColor = if (isDark) {
        if (checked) activeColor.copy(alpha = 0.2f) else ColorOSBorder
    } else {
        if (checked) activeColor.copy(alpha = 0.15f) else ColorOSLightBorder
    }
    
    val thumbColor = if (checked) activeColor else {
        if (isDark) ColorOSGrey400 else ColorOSGrey300
    }
    
    Box(
        modifier = modifier
            .width(52.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = trackColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 4.dp)
                .offset(
                    x = (20.dp * thumbPosition).value.dp,
                    y = 0.dp
                )
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(thumbColor)
        )
    }
}

/**
 * GlassChip - 玻璃态标签
 */
@Composable
fun GlassChip(
    text: String,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    selected: Boolean = false,
    accentColor: Color = HasselbladOrange,
    onClick: () -> Unit = {}
) {
    val backgroundColor = if (selected) {
        accentColor.copy(alpha = if (isDark) 0.25f else 0.18f)
    } else {
        if (isDark) ColorOSCard else ColorOSLightCard
    }
    
    val textColor = if (selected) {
        accentColor
    } else {
        if (isDark) ColorOSTextSecondary else ColorOSLightTextSecondary
    }
    
    val borderColor = if (selected) {
        accentColor.copy(alpha = if (isDark) 0.3f else 0.2f)
    } else {
        if (isDark) ColorOSBorder.copy(alpha = 0.5f) else ColorOSLightBorder
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        onClick = onClick,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                color = textColor
            )
        }
    }
}

/**
 * GlassIconButton - 玻璃态图标按钮
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    tint: Color? = null,
    size: Dp = 44.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 300f
        ),
        label = "iconScale"
    )
    
    val backgroundColor = if (isDark) {
        if (isPressed) ColorOSGlass.copy(alpha = 0.9f) else ColorOSGlass.copy(alpha = 0.7f)
    } else {
        if (isPressed) ColorOSLightGlass.copy(alpha = 0.9f) else ColorOSLightGlass.copy(alpha = 0.7f)
    }
    
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }
    
    val iconColor = tint ?: if (isDark) ColorOSTextSecondary else ColorOSLightTextSecondary
    
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(size / 2),
        color = backgroundColor,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(size / 2)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

/**
 * GlassGradientBackground - 玻璃态渐变背景
 */
@Composable
fun GlassGradientBackground(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    gradientColors: List<Color> = GradientHasselbladMaster,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isDark) ColorOSBlack else ColorOSLightBackground
            )
    ) {
        // 渐变背景层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors.map { it.copy(alpha = 0.15f) }
                    )
                )
        )
        
        // 内容层
        content()
    }
}

/**
 * GlassBottomSheet - 玻璃态底部弹窗
 */
@Composable
fun GlassBottomSheet(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        color = if (isDark) ColorOSGlass else ColorOSLightGlass,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = if (isDark) {
                        Color.White.copy(alpha = 0.1f)
                    } else {
                        Color.Black.copy(alpha = 0.05f)
                    },
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 把手
            Surface(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(36.dp)
                    .height(4.dp),
                shape = RoundedCornerShape(2.dp),
                color = if (isDark) ColorOSGrey600 else ColorOSGrey300
            ) {}
            
            content()
        }
    }
}
