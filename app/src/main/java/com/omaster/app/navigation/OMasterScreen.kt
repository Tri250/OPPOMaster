package com.omaster.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * ColorOS 16 风格统一导航系统
 * 简洁大气，符合OPPO高端摄影体验
 */
sealed class OMasterScreen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    /**
     * 哈苏预设首页 - 主入口
     */
    data object Home : OMasterScreen(
        route = "home",
        title = "哈苏预设",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    
    /**
     * 预设详情页
     */
    data object Detail : OMasterScreen(
        route = "detail/{preset_id}",
        title = "预设详情",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ) {
        fun createRoute(presetId: String) = "detail/$presetId"
    }
    
    /**
     * AI场景检测
     */
    data object SceneDetection : OMasterScreen(
        route = "scene_detection",
        title = "AI场景",
        selectedIcon = Icons.Filled.Visibility,
        unselectedIcon = Icons.Outlined.Visibility
    )
    
    /**
     * AI专业微调
     */
    data object AiFineTune : OMasterScreen(
        route = "ai_fine_tune",
        title = "AI微调",
        selectedIcon = Icons.Filled.AutoFixHigh,
        unselectedIcon = Icons.Outlined.AutoFixHigh
    )
    
    /**
     * 哈苏水印编辑器
     */
    data object WatermarkEditor : OMasterScreen(
        route = "watermark_editor",
        title = "哈苏水印",
        selectedIcon = Icons.Filled.Brush,
        unselectedIcon = Icons.Outlined.Brush
    )
    
    /**
     * 设置页面
     */
    data object Settings : OMasterScreen(
        route = "settings",
        title = "设置",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

/**
 * 底部导航标签栏 - ColorOS 16 风格
 */
val omasterBottomTabScreens = listOf(
    OMasterScreen.Home,
    OMasterScreen.SceneDetection,
    OMasterScreen.AiFineTune,
    OMasterScreen.WatermarkEditor,
    OMasterScreen.Settings
)
