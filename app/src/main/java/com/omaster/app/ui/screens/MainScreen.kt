package com.omaster.app.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.omaster.app.navigation.Screen
import com.omaster.app.viewmodel.MainViewModel
import timber.log.Timber

sealed class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    object Home : BottomNavItem("预设", Icons.Default.GridView, Screen.Home.route)
    object Discover : BottomNavItem("发现", Icons.Default.Explore, Screen.Discover.route)
    object Settings : BottomNavItem("设置", Icons.Default.Settings, Screen.Settings.route)
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        },
        modifier = modifier
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onPresetClick = { preset ->
                        navController.navigate(Screen.Detail.createRoute(preset.id))
                    },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onSceneDetectionClick = { navController.navigate(Screen.SceneDetection.route) },
                    onCreatePresetClick = { navController.navigate(Screen.CreatePreset.route) }
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
                        onApplyPreset = { appliedPreset ->
                            Timber.d("应用预设: ${appliedPreset.name}")
                        }
                    )
                }
            }
            
            composable(Screen.Discover.route) {
                DiscoverScreen()
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.SceneDetection.route) {
                SceneDetectionScreen(
                    onBack = { navController.popBackStack() },
                    onPresetClick = { preset ->
                        navController.navigate(Screen.Detail.createRoute(preset.id))
                    }
                )
            }
            
            composable(Screen.CreatePreset.route) {
                CreatePresetScreen(
                    onBack = { navController.popBackStack() },
                    onSave = { preset ->
                        kotlinx.coroutines.GlobalScope.launch {
                            viewModel.repository.savePreset(preset)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Discover,
        BottomNavItem.Settings
    )
    
    NavigationBar {
        val currentRoute = navController.currentDestination?.route
        
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(text = item.title)
                }
            )
        }
    }
}
