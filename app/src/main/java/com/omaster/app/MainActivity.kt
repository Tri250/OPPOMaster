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
import com.omaster.app.data.ThemeMode
import com.omaster.app.navigation.Screen
import com.omaster.app.service.AiService
import com.omaster.app.ui.screens.AiFineTuneScreen
import com.omaster.app.ui.screens.ColorOSHomeScreen
import com.omaster.app.ui.screens.DetailScreen
import com.omaster.app.ui.screens.HomeScreen
import com.omaster.app.ui.screens.ProDetailScreen
import com.omaster.app.ui.screens.ProHomeScreen
import com.omaster.app.ui.screens.ProSettingsScreen
import com.omaster.app.ui.screens.SceneDetectionScreen
import com.omaster.app.ui.screens.SettingsScreen
import com.omaster.app.ui.components.WatermarkEditorDialog
import com.omaster.app.ui.theme.OMasterTheme
import com.omaster.app.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.scopes.ActivityScoped
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var aiService: AiService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val fluidCloudEnabled by viewModel.fluidCloudEnabled.collectAsStateWithLifecycle()
            val overlayEnabled by viewModel.overlayEnabled.collectAsStateWithLifecycle()
            
            OMasterTheme(themeMode = themeMode) {
                OMasterApp(
                    viewModel, 
                    aiService, 
                    themeMode,
                    fluidCloudEnabled,
                    overlayEnabled
                )
            }
        }
    }
}

@Composable
fun OMasterApp(
    viewModel: MainViewModel,
    aiService: AiService,
    themeMode: Int,
    fluidCloudEnabled: Boolean,
    overlayEnabled: Boolean,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val navController = rememberNavController()
    val presets by viewModel.presets.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            // 专业版首页
            ProHomeScreen(
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
            val preset = presets.find { it.id == presetId }

            preset?.let {
                ProDetailScreen(
                    preset = it,
                    onBack = { navController.popBackStack() },
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    onApplyPreset = { appliedPreset ->
                        Timber.d("应用预设: ${appliedPreset.name}")
                    },
                    themeMode = themeMode
                )
            }
        }
        composable(Screen.Settings.route) {
            ProSettingsScreen(
                themeMode = themeMode,
                onThemeModeChange = { viewModel.setThemeMode(it) },
                fluidCloudEnabled = fluidCloudEnabled,
                onFluidCloudToggle = { viewModel.setFluidCloudEnabled(it) },
                overlayEnabled = overlayEnabled,
                onOverlayToggle = { viewModel.setOverlayEnabled(it) },
                onBack = { navController.popBackStack() }
            )
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
        composable(Screen.AiFineTune.route) {
            AiFineTuneScreen(
                aiService = aiService,
                preset = null,
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
