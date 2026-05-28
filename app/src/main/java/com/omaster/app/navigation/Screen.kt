package com.omaster.app.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    data class Detail(val presetId: String) : Screen("detail/{presetId}") {
        companion object {
            fun createRoute(presetId: String): String = "detail/$presetId"
        }
    }
    object Settings : Screen("settings")
    object SceneDetection : Screen("scene_detection")
}