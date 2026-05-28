package com.omaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.omaster.app.data.PrivacyDataStore
import com.omaster.app.navigation.Screen
import com.omaster.app.ui.screens.DetailScreen
import com.omaster.app.ui.screens.HomeScreen
import com.omaster.app.ui.screens.SettingsScreen
import com.omaster.app.ui.theme.AccentPrimary
import com.omaster.app.ui.theme.OMasterTheme
import com.omaster.app.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

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
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val privacyViewModel: PrivacyViewModel = hiltViewModel()
    val privacyAccepted by privacyViewModel.privacyAccepted.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    // 显示隐私政策弹窗
    if (!privacyAccepted) {
        PrivacyPolicyAcceptanceDialog(
            onAccept = {
            scope.launch {
                privacyViewModel.setPrivacyAccepted(true)
            }
        },
            onDecline = {
                scope.launch {
                    privacyViewModel.setPrivacyAccepted(false)
                }
            }
        )
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
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
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
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun PrivacyPolicyAcceptanceDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("同意隐私政策")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDecline,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("拒绝")
            }
        },
        title = { Text("欢迎使用 OMaster") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrivacyPolicyContent()
            }
        }
    )
}

@Composable
fun PrivacyPolicyContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "感谢您使用 OMaster！",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AccentPrimary
        )

        Text(
            text = "在开始使用前，请先了解我们的隐私政策。",
            style = MaterialTheme.typography.bodyMedium
        )

        Divider()

        Text(
            text = "核心承诺",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        InfoItem(
            icon = Icons.Default.CheckCircle,
            title = "纯本地化运作",
            description = "所有用户数据（收藏、自定义预设、设置）均存储在应用私有目录中，不会上传到服务器"
        )

        InfoItem(
            icon = Icons.Default.CheckCircle,
            title = "最小必要权限",
            description = "仅申请实现功能必需的权限，绝不申请相机、位置等非必要权限"
        )

        InfoItem(
            icon = Icons.Default.CheckCircle,
            title = "透明数据控制",
            description = "您对自己的数据拥有完全的控制权，可以随时删除或导出"
        )

        Divider()

        Text(
            text = "权限说明",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        InfoItem(
            icon = Icons.Default.CheckCircle,
            title = "悬浮窗权限",
            description = "仅用于在相机上层展示调色参数"
        )

        InfoItem(
            icon = Icons.Default.CheckCircle,
            title = "存储权限",
            description = "仅在您主动触发时保存参数卡片和样片到相册"
        )

        InfoItem(
            icon = Icons.Default.CheckCircle,
            title = "网络权限",
            description = "仅在您主动触发时更新预设数据"
        )

        InfoItem(
            icon = Icons.Default.CheckCircle,
            title = "通知权限",
            description = "仅用于悬浮窗常驻通知"
        )

        Divider()

        Text(
            text = "统计功能（可选）",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        InfoItem(
            icon = Icons.Default.CheckCircle,
            title = "默认关闭",
            description = "统计功能默认完全关闭，您可以随时在设置中开启"
        )

        InfoItem(
            icon = Icons.Default.CheckCircle,
            title = "完全匿名",
            description = "开启后仅收集功能使用频次数据，用于优化产品体验，不会收集任何个人信息"
        )

        Divider()

        Text(
            text = "点击「同意」即表示您已阅读并理解上述内容，同意开始使用 OMaster",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentPrimary,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val privacyDataStore: PrivacyDataStore
) : androidx.lifecycle.ViewModel() {
    val privacyAccepted = privacyDataStore.privacyAccepted

    fun setPrivacyAccepted(accepted: Boolean) {
        viewModelScope.launch {
            privacyDataStore.setPrivacyAccepted(accepted)
        }
    }
}
