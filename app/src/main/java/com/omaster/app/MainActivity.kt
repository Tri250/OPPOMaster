package com.omaster.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.omaster.app.model.Preset
import com.omaster.app.navigation.Screen
import com.omaster.app.navigation.BottomNavItem
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
                OMasterApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun OMasterApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val bottomNavItems = remember {
        listOf(
            BottomNavItem(
                screen = Screen.Home,
                icon = Icons.Filled.Home,
                iconOutline = Icons.Outlined.Home
            ),
            BottomNavItem(
                screen = Screen.Community,
                icon = Icons.Filled.People,
                iconOutline = Icons.Outlined.People
            ),
            BottomNavItem(
                screen = Screen.AiFineTune,
                icon = Icons.Filled.AutoAwesome,
                iconOutline = Icons.Outlined.AutoAwesome
            ),
            BottomNavItem(
                screen = Screen.Profile,
                icon = Icons.Filled.Person,
                iconOutline = Icons.Outlined.Person
            )
        )
    }
    
    val showBottomBar = bottomNavItems.any { it.screen.route == currentDestination?.route }
    
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                OMasterBottomBar(
                    destinations = bottomNavItems,
                    currentDestination = currentDestination,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onPresetClick = { preset ->
                        navController.navigate("detail/${preset.id}")
                    },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) }
                )
            }
            
            composable(Screen.Community.route) {
                CommunityScreen(
                    onWorkClick = { },
                    onProfileClick = { },
                    onCreateWorkClick = { },
                    onMyWorksClick = { },
                    onMyPresetsClick = { }
                )
            }
            
            composable(Screen.AiFineTune.route) {
                AiFineTuneScreen(
                    onSettingsClick = { navController.navigate(Screen.Settings.route) }
                )
            }
            
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onMyWorksClick = { },
                    onMyPresetsClick = { },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) }
                )
            }
            
            composable(route = "detail/{preset_id}") { backStackEntry ->
                val presetId = backStackEntry.arguments?.getString("preset_id")
                val presets by viewModel.presets.collectAsStateWithLifecycle()
                val preset = presets.find { it.id == presetId }
                
                val context = androidx.compose.ui.platform.LocalContext.current
                
                preset?.let {
                    DetailScreen(
                        preset = it,
                        onBack = { navController.popBackStack() },
                        onFavoriteToggle = { viewModel.toggleFavorite(it) },
                        onApplyPreset = {
                            Toast.makeText(
                                context,
                                "已将预设「${it.name}」应用到相机",
                                Toast.LENGTH_SHORT
                            ).show()
                            Timber.d("Applied preset: ${it.id}")
                        }
                    )
                }
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun OMasterBottomBar(
    destinations: List<BottomNavItem>,
    currentDestination: NavDestination?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = com.omaster.app.ui.theme.GlassMediumDark,
        contentColor = com.omaster.app.ui.theme.LightFieldOnSurfaceDark
    ) {
        destinations.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.screen.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination.screen) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.icon else destination.iconOutline,
                        contentDescription = destination.screen.title,
                        tint = if (selected) com.omaster.app.ui.theme.LightFieldPrimary else com.omaster.app.ui.theme.LightFieldOnSurfaceVariantDark
                    )
                },
                label = {
                    Text(
                        text = destination.screen.title,
                        color = if (selected) com.omaster.app.ui.theme.LightFieldPrimary else com.omaster.app.ui.theme.LightFieldOnSurfaceVariantDark
                    )
                }
            )
        }
    }
}
