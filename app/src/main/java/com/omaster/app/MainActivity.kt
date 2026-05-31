package com.omaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.omaster.app.data.ThemeMode
import com.omaster.app.navigation.OMasterScreen
import com.omaster.app.navigation.omasterBottomTabScreens
import com.omaster.app.service.AiService
import com.omaster.app.ui.components.OMasterBottomBar
import com.omaster.app.ui.components.OMasterTopBar
import com.omaster.app.ui.screens.AiFineTuneScreen
import com.omaster.app.ui.screens.CameraConfigScreen
import com.omaster.app.ui.screens.ProDetailScreen
import com.omaster.app.ui.screens.ProHomeScreenV2
import com.omaster.app.ui.screens.ProSettingsScreenV2
import com.omaster.app.ui.screens.ProfileScreen
import com.omaster.app.ui.screens.SceneDetectionScreenV2
import com.omaster.app.ui.theme.OMasterTheme
import com.omaster.app.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

/**
 * ColorOS 16 风格主入口
 * 简洁大气，符合OPPO高端哈苏摄影用户体验
 */
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
                    viewModel = viewModel,
                    aiService = aiService,
                    themeMode = themeMode,
                    fluidCloudEnabled = fluidCloudEnabled,
                    overlayEnabled = overlayEnabled
                )
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        (application as? OMasterApplication)?.registerActivity(this)
    }
    
    override fun onStop() {
        super.onStop()
        (application as? OMasterApplication)?.unregisterActivity()
    }
}

/**
 * 统一ColorOS 16风格应用主界面
 */
@Composable
fun OMasterApp(
    viewModel: MainViewModel,
    aiService: AiService,
    themeMode: Int,
    fluidCloudEnabled: Boolean,
    overlayEnabled: Boolean
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: OMasterScreen.Home.route
    
    val presets by viewModel.presets.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    
    Scaffold(
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxSize(),
        topBar = {
            // 根据当前路由决定是否显示顶部栏
            if (currentRoute in omasterBottomTabScreens.map { it.route }) {
                OMasterTopBar(
                    onSettingsClick = { navController.navigate(OMasterScreen.Settings.route) }
                )
            }
        },
        bottomBar = {
            // 仅在底部导航页面显示底部栏
            if (currentRoute in omasterBottomTabScreens.map { it.route }) {
                OMasterBottomBar(
                    currentScreen = when (currentRoute) {
                        OMasterScreen.Home.route -> OMasterScreen.Home
                        OMasterScreen.SceneDetection.route -> OMasterScreen.SceneDetection
                        OMasterScreen.CameraConfig.route -> OMasterScreen.CameraConfig
                        OMasterScreen.AiFineTune.route -> OMasterScreen.AiFineTune
                        OMasterScreen.Profile.route -> OMasterScreen.Profile
                        else -> OMasterScreen.Home
                    },
                    onScreenSelected = { screen ->
                        navController.navigate(screen.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(OMasterScreen.Home.route) {
                                saveState = true
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = OMasterScreen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = com.omaster.app.ui.animation.ColorOSEasing.Decelerate
                    )
                )
            },
            exitTransition = {
                fadeOut(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = com.omaster.app.ui.animation.ColorOSEasing.Accelerate
                    )
                )
            }
        ) {
            // 哈苏预设首页 - 主入口
            composable(OMasterScreen.Home.route) {
                ProHomeScreenV2(
                    onPresetClick = { preset ->
                        navController.navigate(OMasterScreen.Detail.createRoute(preset.id))
                    },
                    onSettingsClick = { navController.navigate(OMasterScreen.Settings.route) },
                    onSceneDetectionClick = { navController.navigate(OMasterScreen.SceneDetection.route) },
                    onAiFineTuneClick = { navController.navigate(OMasterScreen.AiFineTune.route) },
                    onWatermarkClick = { /* 水印功能暂未实现 */ },
                    onColorOSHomeClick = { /* ColorOS 首页功能暂未实现 */ }
                )
            }
            
            // 预设详情页
            composable(
                route = OMasterScreen.Detail.route,
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
                } ?: run {
                    // 预设未找到时的备用界面 - 简单处理
                    navController.popBackStack()
                }
            }
            
            // AI场景检测
            composable(OMasterScreen.SceneDetection.route) {
                SceneDetectionScreenV2(
                    aiService = aiService,
                    allPresets = presets,
                    onBack = { navController.popBackStack() },
                    onPresetClick = { preset ->
                        navController.navigate(OMasterScreen.Detail.createRoute(preset.id))
                    },
                    onFavoriteToggle = { viewModel.toggleFavorite(it) }
                )
            }
            
            // 相机配置管理
            composable(OMasterScreen.CameraConfig.route) {
                CameraConfigScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            // AI专业微调
            composable(OMasterScreen.AiFineTune.route) {
                AiFineTuneScreen(
                    aiService = aiService,
                    preset = null,
                    onBack = { navController.popBackStack() }
                )
            }
            
            // 用户个人页面
            composable(OMasterScreen.Profile.route) {
                ProfileScreen(
                    onSettingsClick = { navController.navigate(OMasterScreen.Settings.route) },
                    onCameraConfigClick = { navController.navigate(OMasterScreen.CameraConfig.route) }
                )
            }
            
            // 设置页面
            composable(OMasterScreen.Settings.route) {
                ProSettingsScreenV2(
                    themeMode = themeMode,
                    onThemeModeChange = { viewModel.setThemeMode(it) },
                    fluidCloudEnabled = fluidCloudEnabled,
                    onFluidCloudToggle = { viewModel.setFluidCloudEnabled(it) },
                    overlayEnabled = overlayEnabled,
                    onOverlayToggle = { viewModel.setOverlayEnabled(it) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
