package com.omaster.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        title = "首页",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    
    data object Detail : Screen(
        route = "detail/{preset_id}",
        title = "详情",
        selectedIcon = Icons.Filled.Info,
        unselectedIcon = Icons.Outlined.Info
    ) {
        fun createRoute(presetId: String) = "detail/$presetId"
    }
    
    data object Settings : Screen(
        route = "settings",
        title = "设置",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
    
    data object SceneDetection : Screen(
        route = "scene_detection",
        title = "场景检测",
        selectedIcon = Icons.Filled.Visibility,
        unselectedIcon = Icons.Outlined.Visibility
    )
    
    data object AiFineTune : Screen(
        route = "ai_fine_tune",
        title = "AI微调",
        selectedIcon = Icons.Filled.AutoFixHigh,
        unselectedIcon = Icons.Outlined.AutoFixHigh
    )
    
    data object WatermarkEditor : Screen(
        route = "watermark_editor",
        title = "水印编辑器",
        selectedIcon = Icons.Filled.Brush,
        unselectedIcon = Icons.Outlined.Brush
    )
    
    data object ColorOSHome : Screen(
        route = "coloros_home",
        title = "ColorOS首页",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    
    data object ImageRecommendation : Screen(
        route = "image_recommendation",
        title = "精选影像推荐",
        selectedIcon = Icons.Filled.PhotoCamera,
        unselectedIcon = Icons.Outlined.PhotoCamera
    )
}
