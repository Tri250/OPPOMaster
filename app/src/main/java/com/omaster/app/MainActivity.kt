package com.omaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.omaster.app.navigation.Screen
import com.omaster.app.ui.screens.*
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

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : BottomNavItem(
        route = Screen.Home.route,
        label = "首页",
        icon = Icons.Filled.Home
    )
    
    data object SceneDetection : BottomNavItem(
        route = Screen.SceneDetection.route,
        label = "AI识别",
        icon = Icons.Filled.AutoAwesome
    )
    
    data object FilterLibrary : BottomNavItem(
        route = Screen.FilterLibrary.route,
        label = "预设库",
        icon = Icons.Filled.FilterAlt
    )
    
    data object FloatingWindow : BottomNavItem(
        route = Screen.FloatingWindow.route,
        label = "悬浮窗",
        icon = Icons.Filled.Layers
    )
    
    data object Settings : BottomNavItem(
        route = Screen.Settings.route,
        label = "我的",
        icon = Icons.Filled.Person
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.SceneDetection,
    BottomNavItem.FilterLibrary,
    BottomNavItem.FloatingWindow,
    BottomNavItem.Settings
)

@Composable
fun OMasterApp(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                ColorOSHomeScreen(
                    onPresetClick = { preset ->
                        navController.navigate(Screen.Detail.createRoute(preset.id))
                    },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onSceneDetectionClick = { navController.navigate(Screen.SceneDetection.route) },
                    onFilterLibraryClick = { navController.navigate(Screen.FilterLibrary.route) },
                    onNativeCameraClick = { navController.navigate(Screen.NativeCamera.route) },
                    onPresetEditorClick = { navController.navigate(Screen.PresetEditor.route) },
                    onLutManagerClick = { navController.navigate(Screen.LutManager.route) },
                    onTestVerificationClick = { navController.navigate(Screen.TestVerification.route) },
                    onWatermarkClick = { navController.navigate(Screen.Watermark.route) }
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
                            Timber.d("应用预设: ${appliedPreset.name}")
                        }
                    )
                }
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onNativeCameraClick = { navController.navigate(Screen.NativeCamera.route) },
                    onFloatingWindowClick = { navController.navigate(Screen.FloatingWindow.route) },
                    onPresetEditorClick = { navController.navigate(Screen.PresetEditor.route) },
                    onLutManagerClick = { navController.navigate(Screen.LutManager.route) },
                    onTestVerificationClick = { navController.navigate(Screen.TestVerification.route) },
                    onWatermarkClick = { navController.navigate(Screen.Watermark.route) }
                )
            }
            
            composable(Screen.SceneDetection.route) {
                SceneDetectionScreen(
                    aiService = com.omaster.app.service.AiService(),
                    allPresets = emptyList(),
                    onBack = { navController.popBackStack() },
                    onPresetClick = { preset ->
                        navController.navigate(Screen.Detail.createRoute(preset.id))
                    },
                    onFavoriteToggle = {}
                )
            }
            
            composable(Screen.NativeCamera.route) {
                NativeCameraScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.FloatingWindow.route) {
                FloatingWindowScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.FilterLibrary.route) {
                FilterLibraryScreen(
                    onBack = { navController.popBackStack() },
                    onPresetClick = { preset ->
                        navController.navigate(Screen.Detail.createRoute(preset.id))
                    }
                )
            }
            
            composable(Screen.PresetEditor.route) {
                PresetEditorScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.LutManager.route) {
                LutManagerScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.TestVerification.route) {
                TestVerificationScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.Watermark.route) {
                WatermarkScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
