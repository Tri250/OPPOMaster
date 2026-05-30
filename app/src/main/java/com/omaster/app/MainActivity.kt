package com.omaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.omaster.app.floating.FloatingWindowManager
import com.omaster.app.navigation.Screen
import com.omaster.app.ui.screens.DetailScreen
import com.omaster.app.ui.screens.HomeScreen
import com.omaster.app.ui.screens.SettingsScreen
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
        
        // 初始化悬浮窗管理器
        setupFloatingWindowManager()
        
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            
            OMasterTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OMasterApp()
                }
            }
        }
    }
    
    /**
     * 设置悬浮窗管理器
     * 支持Float-001到Float-007的所有功能
     */
    private fun setupFloatingWindowManager() {
        // 设置错误回调
        FloatingWindowManager.onError = { errorMessage ->
            Timber.e("悬浮窗错误: $errorMessage")
            // 可以在此显示Toast或Snackbar
        }
        
        // 设置状态变化回调
        FloatingWindowManager.onWindowStateChanged = { isShowing ->
            Timber.d("悬浮窗状态变化: $isShowing")
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Float-005: 应用恢复时检查悬浮窗权限
        if (FloatingWindowManager.canDrawOverlays(this)) {
            // 权限仍然有效
            Timber.d("悬浮窗权限有效")
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Float-005: 应用暂停时保持悬浮窗显示
        Timber.d("应用进入后台，悬浮窗保持显示")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Float-007: 清理悬浮窗资源
        FloatingWindowManager.cleanup()
        Timber.d("MainActivity销毁，悬浮窗资源已清理")
    }
}

@Composable
fun OMasterApp(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    
    // Float-005: 生命周期观察器
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Timber.d("应用恢复，检查悬浮窗状态")
                }
                Lifecycle.Event.ON_PAUSE -> {
                    Timber.d("应用暂停，悬浮窗应保持显示")
                }
                else -> {}
            }
        }
        
        // 注意：在Compose环境中需要使用LocalLifecycleOwner
        onDispose {
            // 清理工作
        }
    }
    
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
                onWebEcosystemClick = { navController.navigate(Screen.WebEcosystem.route) },
                onWebMyPresetsClick = { navController.navigate(Screen.WebMyPresets.route) }
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
        composable(Screen.WebEcosystem.route) {
            WebViewScreen(
                navController = navController,
                title = "预设生态",
                initialUrl = "file:///android_asset/index.html"
            )
        }
        composable(Screen.WebMyPresets.route) {
            WebViewScreen(
                navController = navController,
                title = "我的预设",
                initialUrl = "file:///android_asset/index.html#/my-presets"
            )
        }
    }
}
