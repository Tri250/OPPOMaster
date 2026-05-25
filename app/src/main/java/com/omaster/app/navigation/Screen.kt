package com.omaster.app.navigation

import androidx.navigation.NavType

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Detail : Screen("detail/{preset_id}") {
        val presetIdType = NavType.StringType
        
        fun createRoute(presetId: String): String = "detail/$presetId"
    }
    object Settings : Screen("settings")
}