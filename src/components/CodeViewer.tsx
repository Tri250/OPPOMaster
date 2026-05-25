import React from 'react';
import { X, Copy, Check } from 'lucide-react';

interface CodeViewerProps {
  filePath: string;
  onClose: () => void;
}

const mockCodeFiles: Record<string, string> = {
  'OMaster/app/src/main/java/com/omaster/app/MainActivity.kt': `package com.omaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.omaster.app.navigation.AppNavHost
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
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}`,
  'OMaster/app/src/main/java/com/omaster/app/viewmodel/MainViewModel.kt': `package com.omaster.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omaster.app.data.PreferencesDataStore
import com.omaster.app.data.ThemeMode
import com.omaster.app.data.PresetRepository
import com.omaster.app.model.Preset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: PresetRepository,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {
    val presets = repository.presets
    val themeMode = preferencesDataStore.themeMode
    val fluidCloudEnabled = preferencesDataStore.fluidCloudEnabled
    val overlayEnabled = preferencesDataStore.overlayEnabled

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        Timber.d("Search query changed: \$query")
    }

    fun toggleFavorite(preset: Preset) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(preset.id)
                Timber.d("Toggled favorite for preset: \${preset.id}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle favorite")
            }
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            preferencesDataStore.setThemeMode(themeMode)
            Timber.d("Theme mode changed: \$themeMode")
        }
    }
}`,
  'OMaster/app/src/main/java/com/omaster/app/data/PreferencesDataStore.kt': `package com.omaster.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "omaster_preferences")

@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val FAVORITE_PRESETS = stringSetPreferencesKey("favorite_presets")
        val THEME_MODE = intPreferencesKey("theme_mode")
        val FLUID_CLOUD_ENABLED = intPreferencesKey("fluid_cloud_enabled")
        val OVERLAY_ENABLED = intPreferencesKey("overlay_enabled")
    }

    val favoritePresets: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.FAVORITE_PRESETS] ?: emptySet()
        }

    val fluidCloudEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            (preferences[PreferencesKeys.FLUID_CLOUD_ENABLED] ?: 1) == 1
        }

    suspend fun toggleFavorite(presetId: String) {
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[PreferencesKeys.FAVORITE_PRESETS]?.toMutableSet() 
                ?: mutableSet()
            if (currentFavorites.contains(presetId)) {
                currentFavorites.remove(presetId)
            } else {
                currentFavorites.add(presetId)
            }
            preferences[PreferencesKeys.FAVORITE_PRESETS] = currentFavorites
        }
    }
}`,
  'OMaster/app/src/main/java/com/omaster/app/service/FluidCloudService.kt': `package com.omaster.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.omaster.app.MainActivity
import com.omaster.app.R
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class FluidCloudService : Service() {
    
    companion object {
        const val ACTION_SHOW_CAPSULE = "com.omaster.app.action.SHOW_CAPSULE"
        const val ACTION_HIDE_CAPSULE = "com.omaster.app.action.HIDE_CAPSULE"
        const val ACTION_APPLY_PRESET = "com.omaster.app.action.APPLY_PRESET"
        private const val NOTIFICATION_CHANNEL_ID = "fluid_cloud_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_CAPSULE -> showFluidCloudCapsule()
            ACTION_HIDE_CAPSULE -> hideFluidCloudCapsule()
            ACTION_APPLY_PRESET -> applyPresetToCamera()
        }
        return START_STICKY
    }

    private fun showFluidCloudCapsule() {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("OMaster")
            .setContentText("流体云胶囊已激活")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun applyPresetToCamera() {
        sendBroadcast(Intent("com.omaster.app.ACTION_PRESET_APPLIED"))
    }
}`,
  'OMaster/app/src/main/java/com/omaster/app/navigation/AppNavHost.kt': `package com.omaster.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.omaster.app.ui.screens.DetailScreen
import com.omaster.app.ui.screens.HomeScreen
import com.omaster.app.ui.screens.SettingsScreen
import com.omaster.app.viewmodel.MainViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
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
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("preset_id") { type = Screen.Detail.presetIdType })
        ) { backStackEntry ->
            val presetId = backStackEntry.arguments?.getString("preset_id")
            val viewModel: MainViewModel = hiltViewModel()
            val presets by viewModel.presets.collectAsStateWithLifecycle()
            val preset = presets.find { it.id == presetId }

            preset?.let {
                DetailScreen(
                    preset = it,
                    onBack = { navController.popBackStack() },
                    onFavoriteToggle = { viewModel.toggleFavorite(it) }
                )
            }
        }
    }
}`,
  'OMaster/README.md': `# OMaster - OPPO 哈苏影像系统级参数中枢

## 项目简介

OMaster 是一个为 OPPO 哈苏影像系统设计的专业调色参数库应用，完全重构以拥抱 ColorOS 16 和 Android 16 的系统级能力。

## 核心特性

- **流体云胶囊集成**: 替代传统悬浮窗，实现无缝参数流转
- **一键闪记支持**: 与系统相机深度集成
- **金标设计语言**: 采用 ColorOS 16 Aqua Design 水生设计风格
- **哈苏专业体验**: HNCS 认证预设，拟物化参数控件

## 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose + Material 3
- **依赖注入**: Hilt
- **数据持久化**: DataStore Preferences
- **网络请求**: Retrofit + OkHttp

## 项目结构

\`\`\`
OMaster/
├── app/
│   ├── src/main/java/com/omaster/app/
│   │   ├── data/          # 数据层
│   │   ├── di/            # Hilt 依赖注入
│   │   ├── model/         # 数据模型
│   │   ├── navigation/    # 导航定义
│   │   ├── network/       # 网络请求 API
│   │   ├── service/       # 系统服务
│   │   ├── ui/            # UI 组件
│   │   ├── viewmodel/     # ViewModel
│   └── src/test/          # 测试
└── gradle/                # Gradle 配置
\`\`\``,
};

export function CodeViewer({ filePath, onClose }: CodeViewerProps) {
  const [copied, setCopied] = React.useState(false);
  const code = mockCodeFiles[filePath] || `// File content not available
// Path: ${filePath}`;

  const handleCopy = () => {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="h-full flex flex-col bg-gray-900">
      <div className="flex items-center justify-between px-4 py-3 bg-gray-800 border-b border-gray-700">
        <div className="flex items-center gap-2">
          <div className="flex gap-1.5">
            <div className="w-3 h-3 rounded-full bg-red-500" />
            <div className="w-3 h-3 rounded-full bg-yellow-500" />
            <div className="w-3 h-3 rounded-full bg-green-500" />
          </div>
          <span className="text-sm text-gray-400 ml-3 truncate max-w-md">
            {filePath}
          </span>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={handleCopy}
            className="p-2 hover:bg-gray-700 rounded transition-colors"
            title="Copy code"
          >
            {copied ? (
              <Check className="w-4 h-4 text-green-500" />
            ) : (
              <Copy className="w-4 h-4 text-gray-400" />
            )}
          </button>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-700 rounded transition-colors"
            title="Close"
          >
            <X className="w-4 h-4 text-gray-400" />
          </button>
        </div>
      </div>
      <div className="flex-1 overflow-auto p-4">
        <pre className="text-sm text-gray-300 font-mono whitespace-pre-wrap">
          <code>{code}</code>
        </pre>
      </div>
    </div>
  );
}
