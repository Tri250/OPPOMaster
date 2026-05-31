package com.omaster.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
     * 相机配置管理
     */
    data object CameraConfig : OMasterScreen(
        route = "camera_config",
        title = "相机配置",
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune
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
     * 用户个人页面
     */
    data object Profile : OMasterScreen(
        route = "profile",
        title = "我的",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
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
    OMasterScreen.CameraConfig,
    OMasterScreen.AiFineTune,
    OMasterScreen.Profile
)
