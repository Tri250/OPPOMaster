package com.omaster.app.navigation

// ============================================
// OPPO OMaster 导航系统 - 底部Tab导航
// ============================================

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "预设")
    object Community : Screen("community", "社区")
    object AiFineTune : Screen("ai_fine_tune", "AI")
    object Profile : Screen("profile", "我的")
}

data class BottomNavItem(
    val screen: Screen,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconOutline: androidx.compose.ui.graphics.vector.ImageVector
)
