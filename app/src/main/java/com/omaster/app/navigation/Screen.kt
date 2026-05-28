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
    object Discover : Screen("discover")
    object CreatePreset : Screen("create_preset")
    data class EditPreset(val presetId: String) : Screen("edit_preset/{presetId}") {
        companion object {
            fun createRoute(presetId: String): String = "edit_preset/$presetId"
        }
    }
}