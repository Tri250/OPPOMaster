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
import com.omaster.app.data.PreferencesDataStore
import com.omaster.app.navigation.Screen
import com.omaster.app.ui.screens.CameraParamsScreen
import com.omaster.app.ui.screens.DetailScreen
import com.omaster.app.ui.screens.HomeScreen
import com.omaster.app.ui.screens.OnboardingScreen
import com.omaster.app.ui.screens.SettingsScreen
import com.omaster.app.ui.screens.WatermarkScreen
import com.omaster.app.ui.theme.OMasterTheme
import com.omaster.app.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesDataStore: PreferencesDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val onboardingCompleted by preferencesDataStore.onboardingCompleted.collectAsStateWithLifecycle(initialValue = false)
            
            OMasterTheme(themeMode = themeMode) {
                OMasterApp(
                    onboardingCompleted = onboardingCompleted,
                    onOnboardingComplete = {
                        lifecycleScope.launch {
                            preferencesDataStore.setOnboardingCompleted(true)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun OMasterApp(
    onboardingCompleted: Boolean,
    onOnboardingComplete: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val navController = rememberNavController()
    val startDestination = if (onboardingCompleted) Screen.Home.route else Screen.Onboarding.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onComplete = {
                onOnboardingComplete()
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onPresetClick = { preset ->
                    navController.navigate(Screen.Detail.createRoute(preset.id))
                },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onWatermarkClick = { navController.navigate(Screen.Watermark.route) },
                onCameraParamsClick = { navController.navigate(Screen.CameraParams.route) }
            )
        }
        composable(
            route = "detail/{preset_id}",
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
                        // 模拟应用预设的功能
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
        composable(Screen.Watermark.route) {
            WatermarkScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.CameraParams.route) {
            CameraParamsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
