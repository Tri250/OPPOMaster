package com.omaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.omaster.app.navigation.Screen
import com.omaster.app.ui.screens.AiFineTuneScreen
import com.omaster.app.ui.screens.ColorOSHomeScreen
import com.omaster.app.ui.screens.DetailScreen
import com.omaster.app.ui.screens.HomeScreen
import com.omaster.app.ui.screens.SceneDetectionScreen
import com.omaster.app.ui.screens.SettingsScreen
import com.omaster.app.ui.components.WatermarkEditorDialog
import com.omaster.app.ui.theme.OMasterTheme
import com.omaster.app.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            
            OMasterTheme(themeMode = themeMode) {
                OMasterApp()
            }
        }
    }
}

@Composable
fun OMasterApp(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val navController = rememberNavController()

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
                onSceneDetectionClick = { navController.navigate(Screen.SceneDetection.route) },
                onAiFineTuneClick = { navController.navigate(Screen.AiFineTune.route) },
                onWatermarkClick = { navController.navigate(Screen.WatermarkEditor.route) },
                onColorOSHomeClick = { navController.navigate(Screen.ColorOSHome.route) }
            )
        }
        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("preset_id") { type = NavType.StringType })
        ) { backStackEntry ->
            val presetId = backStackEntry.arguments?.getString("preset_id")
            val viewModel: MainViewModel = hiltViewModel()
            val presets by viewModel.presets.collectAsStateWithLifecycle()
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
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.SceneDetection.route) {
            SceneDetectionScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AiFineTune.route) {
            AiFineTuneScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.WatermarkEditor.route) {
            WatermarkEditorDialog(
                onDismiss = { navController.popBackStack() }
            )
        }
        composable(Screen.ColorOSHome.route) {
            ColorOSHomeScreen(
                onPresetClick = { preset ->
                    navController.navigate(Screen.Detail.createRoute(preset.id))
                },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }
    }
}
