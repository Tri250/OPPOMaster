package com.omaster.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.omaster.app.navigation.Screen
import com.omaster.app.service.AiService
import com.omaster.app.ui.screens.DetailScreen
import com.omaster.app.ui.screens.HomeScreen
import com.omaster.app.ui.screens.ImageRecommendationScreen
import com.omaster.app.ui.screens.SceneDetectionScreen
import com.omaster.app.ui.screens.SettingsScreen
import com.omaster.app.viewmodel.MainViewModel
import timber.log.Timber

@Composable
fun OMasterApp(
    viewModel: MainViewModel,
    aiService: AiService,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val presets by viewModel.presets.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onPresetClick = { preset ->
                    navController.navigate(Screen.Detail.createRoute(preset.id))
                },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onSceneDetectionClick = { navController.navigate(Screen.SceneDetection.route) }
            )
        }

        composable(
            route = "detail/{preset_id}",
            arguments = listOf(navArgument("preset_id") { type = NavType.StringType })
        ) { backStackEntry ->
            val presetId = backStackEntry.arguments?.getString("preset_id")
            val preset = presets.find { it.id == presetId }

            preset?.let {
                DetailScreen(
                    preset = it,
                    onBack = { navController.popBackStack() },
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    onApplyPreset = { appliedPreset ->
                        Timber.d("应用预设: ${appliedPreset.name}")
                    }
                )
            }
        }

        composable(Screen.SceneDetection.route) {
            SceneDetectionScreen(
                aiService = aiService,
                allPresets = presets,
                onBack = { navController.popBackStack() },
                onPresetClick = { preset ->
                    navController.navigate(Screen.Detail.createRoute(preset.id))
                },
                onFavoriteToggle = { viewModel.toggleFavorite(it) }
            )
        }

        composable(Screen.ImageRecommendation.route) {
            ImageRecommendationScreen(
                onBack = { navController.popBackStack() },
                onPresetClick = { preset ->
                    navController.navigate(Screen.Detail.createRoute(preset.id))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
