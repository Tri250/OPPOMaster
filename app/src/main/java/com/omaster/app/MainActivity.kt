package com.omaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.omaster.app.navigation.Screen
import com.omaster.app.ui.screens.*
import com.omaster.app.ui.theme.OMasterTheme
import com.omaster.app.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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
    val viewModel: MainViewModel = hiltViewModel()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

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
                onCreatePresetClick = { navController.navigate(Screen.CreatePreset.route) },
                onSceneDetectionClick = { navController.navigate(Screen.SceneDetection.route) }
            )
        }
        composable(
            route = "detail/{preset_id}",
            arguments = listOf(navArgument("preset_id") { type = NavType.StringType })
        ) { backStackEntry ->
            val presetId = backStackEntry.arguments?.getString("preset_id")
            val presets by viewModel.presets.collectAsStateWithLifecycle()
            val preset = presets.find { it.id == presetId }

            preset?.let {
                DetailScreen(
                    preset = it,
                    onBack = { navController.popBackStack() },
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    onEditPreset = { if (it.isCustom) navController.navigate(Screen.EditPreset.createRoute(it.id)) },
                    onDeletePreset = { if (it.isCustom) {
                        coroutineScope.launch {
                            viewModel.deleteCustomPreset(it.id)
                            navController.popBackStack()
                        }
                    }},
                    onApplyPreset = { appliedPreset ->
                        coroutineScope.launch {
                            viewModel.applyPreset(appliedPreset)
                            Timber.d("应用预设: ${appliedPreset.name}")
                        }
                    }
                )
            }
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.CreatePreset.route) {
            CreateEditPresetScreen(
                preset = null,
                onBack = { navController.popBackStack() },
                onSavePreset = { preset ->
                    coroutineScope.launch {
                        viewModel.createCustomPreset(
                            name = preset.name,
                            coverPath = preset.coverPath,
                            cameraParams = preset.cameraParams ?: com.omaster.app.model.CameraParams(),
                            sections = preset.sections,
                            deviceModel = preset.deviceModel,
                            tags = preset.tags
                        )
                        navController.popBackStack()
                    }
                }
            )
        }
        composable(
            route = "edit_preset/{preset_id}",
            arguments = listOf(navArgument("preset_id") { type = NavType.StringType })
        ) { backStackEntry ->
            val presetId = backStackEntry.arguments?.getString("preset_id")
            val presets by viewModel.presets.collectAsStateWithLifecycle()
            val preset = presets.find { it.id == presetId }
            
            preset?.let {
                CreateEditPresetScreen(
                    preset = it,
                    onBack = { navController.popBackStack() },
                    onSavePreset = { updatedPreset ->
                        coroutineScope.launch {
                            viewModel.updateCustomPreset(updatedPreset)
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
        composable(Screen.SceneDetection.route) {
            SceneDetectionScreen(
                onBack = { navController.popBackStack() },
                onPresetSelected = { preset ->
                    navController.navigate(Screen.Detail.createRoute(preset.id))
                }
            )
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadCustomPresets()
    }
}
