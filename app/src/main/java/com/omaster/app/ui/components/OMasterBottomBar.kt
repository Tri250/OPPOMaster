package com.omaster.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omaster.app.navigation.OMasterScreen
import com.omaster.app.navigation.omasterBottomTabScreens
import com.omaster.app.ui.theme.Colors
import com.omaster.app.ui.theme.Spacing
import com.omaster.app.ui.theme.Typography

/**
 * ColorOS 16 风格底部导航栏
 * 简洁大气，符合OPPO高端摄影体验
 */
@Composable
fun OMasterBottomBar(
    currentScreen: OMasterScreen,
    onScreenSelected: (OMasterScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 顶部装饰线 - 渐变分割线
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Colors.HasselbladOrange.copy(alpha = 0.0f),
                            Colors.HasselbladOrange.copy(alpha = 0.3f),
                            Colors.HasselbladOrange.copy(alpha = 0.0f)
                        )
                    )
                )
        )
        
        // 导航栏主体 - 玻璃态背景
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Colors.HasselbladOrange.copy(alpha = 0.03f))
                .padding(
                    horizontal = Spacing.sm,
                    vertical = Spacing.sm
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.omaster.app.navigation.omasterBottomTabScreens.forEach { screen ->
                    OMasterBottomNavItem(
                        screen = screen,
                        isSelected = currentScreen.route == screen.route,
                        onClick = { onScreenSelected(screen) }
                    )
                }
            }
        }
    }
}

/**
 * 单个底部导航项 - ColorOS 16 风格
 */
@Composable
fun OMasterBottomNavItem(
    screen: OMasterScreen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "nav_item_scale"
    )
    
    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = Spacing.sm,
                vertical = Spacing.xs
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(if (isSelected) 36.dp else 32.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        Colors.HasselbladOrange.copy(alpha = 0.15f)
                    } else {
                        Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                contentDescription = screen.title,
                tint = if (isSelected) Colors.HasselbladOrange else Colors.OnSurfaceVariant,
                modifier = Modifier.size(if (isSelected) 24.dp else 20.dp)
            )
        }
        
        // 标签文字
        Text(
            text = screen.title,
            style = Typography.labelSmall,
            color = if (isSelected) Colors.HasselbladOrange else Colors.OnSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * ColorOS 16 风格主屏幕顶部栏
 */
@Composable
fun OMasterTopBar(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Spacing.ScreenPadding,
                vertical = Spacing.sm
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo和品牌信息
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Colors.HasselbladOrange
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "哈苏",
                        tint = Colors.OnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Column {
                Text(
                    text = "哈苏影像",
                    style = Typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Colors.HasselbladOrange
                )
                Text(
                    text = "OPPO · 完美呈现",
                    style = Typography.bodySmall,
                    color = Colors.OnSurfaceVariant
                )
            }
        }
        
        // 设置按钮
        GlassIconButton(
            icon = androidx.compose.material.icons.Icons.Default.Settings,
            onClick = onSettingsClick,
            contentDescription = "设置",
            size = 44.dp
        )
    }
}

// 临时Surface替代，避免循环依赖
@Composable
private fun Surface(
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape,
    color: Color,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(shape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
