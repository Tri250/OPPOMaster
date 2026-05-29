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
    
    data object ImageRecommendation : Screen(
        route = "image_recommendation",
        title = "精选影像推荐",
        selectedIcon = Icons.Filled.PhotoCamera,
        unselectedIcon = Icons.Outlined.PhotoCamera
    )
    
    data object SceneDetection : Screen(
        route = "scene_detection",
        title = "AI场景识别",
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome
    )
    
    data object NativeCamera : Screen(
        route = "native_camera",
        title = "原生相机参数",
        selectedIcon = Icons.Filled.CameraAlt,
        unselectedIcon = Icons.Outlined.CameraAlt
    )
    
    data object FloatingWindow : Screen(
        route = "floating_window",
        title = "智能悬浮窗",
        selectedIcon = Icons.Filled.Layers,
        unselectedIcon = Icons.Outlined.Layers
    )
    
    data object FilterLibrary : Screen(
        route = "filter_library",
        title = "预设库",
        selectedIcon = Icons.Filled.FilterAlt,
        unselectedIcon = Icons.Outlined.FilterAlt
    )
    
    data object PresetEditor : Screen(
        route = "preset_editor",
        title = "预设编辑器",
        selectedIcon = Icons.Filled.Edit,
        unselectedIcon = Icons.Outlined.Edit
    )
    
    data object LutManager : Screen(
        route = "lut_manager",
        title = "预设管理",
        selectedIcon = Icons.Filled.Storage,
        unselectedIcon = Icons.Outlined.Storage
    )
    
    data object TestVerification : Screen(
        route = "test_verification",
        title = "测试验证中心",
        selectedIcon = Icons.Filled.VerifiedUser,
        unselectedIcon = Icons.Outlined.VerifiedUser
    )
    
    data object Watermark : Screen(
        route = "watermark",
        title = "水印工具",
        selectedIcon = Icons.Filled.WaterDrop,
        unselectedIcon = Icons.Outlined.WaterDrop
    )
}
