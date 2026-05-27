package com.omaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.omaster.app.data.ConfigCenter
import com.omaster.app.data.DataMigrationHandler
import com.omaster.app.navigation.Screen
import com.omaster.app.ui.screens.DetailScreen
import com.omaster.app.ui.screens.HomeScreen
import com.omaster.app.ui.screens.SettingsScreen
import com.omaster.app.ui.theme.OMasterTheme
import com.omaster.app.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var configCenter: ConfigCenter

    @Inject
    lateinit var migrationHandler: DataMigrationHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            OMasterTheme(themeMode = themeMode) {
                OMasterApp(
                    configCenter = configCenter,
                    migrationHandler = migrationHandler
                )
            }
        }
    }
}

@Composable
fun OMasterApp(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    configCenter: ConfigCenter,
    migrationHandler: DataMigrationHandler
) {
    val navController = rememberNavController()
    var showPrivacyDialog by remember { mutableStateOf(true) }
    var showMigrationDialog by remember { mutableStateOf(false) }
    var isMigrating by remember { mutableStateOf(false) }
    var migrationError by remember { mutableStateOf<String?>(null) }

    val userAgreed by configCenter.userAgreedPrivacy.collectAsState()
    val migrationCompleted by configCenter.migrationCompleted.collectAsState()

    LaunchedEffect(Unit) {
        showPrivacyDialog = !userAgreed
        showMigrationDialog = migrationHandler.shouldShowMigrationDialog()
    }

    if (showPrivacyDialog && !userAgreed) {
        PrivacyDialog(
            onDismiss = { showPrivacyDialog = false },
            onAgree = {
                kotlinx.coroutines.GlobalScope.launch {
                    try {
                        configCenter.setUserAgreedPrivacy(true)
                        showPrivacyDialog = false
                        Timber.i("User agreed to privacy policy")
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to save privacy agreement")
                    }
                }
            },
            onDisagree = {
                finishActivity()
            }
        )
    }

    if (showMigrationDialog && !migrationCompleted) {
        MigrationDialog(
            isMigrating = isMigrating,
            error = migrationError,
            onStartMigration = {
                isMigrating = true
                kotlinx.coroutines.GlobalScope.launch {
                    try {
                        migrationHandler.executeMigration { result ->
                            when (result) {
                                is DataMigrationHandler.MigrationResult.Success -> {
                                    isMigrating = false
                                    kotlinx.coroutines.GlobalScope.launch {
                                        configCenter.setMigrationCompleted(true)
                                    }
                                    showMigrationDialog = false
                                    Timber.i("Migration completed successfully")
                                }
                                is DataMigrationHandler.MigrationResult.Failed -> {
                                    isMigrating = false
                                    migrationError = result.error
                                    Timber.e("Migration failed: ${result.error}")
                                }
                                DataMigrationHandler.MigrationResult.Skipped -> {
                                    isMigrating = false
                                    showMigrationDialog = false
                                    Timber.i("Migration skipped by user")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        isMigrating = false
                        migrationError = e.message ?: "Unknown error"
                        Timber.e(e, "Migration process failed")
                    }
                }
            },
            onSkip = {
                kotlinx.coroutines.GlobalScope.launch {
                    try {
                        migrationHandler.markMigrationSkipped()
                        showMigrationDialog = false
                        Timber.i("Migration skipped")
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to mark migration as skipped")
                    }
                }
            },
            onDismiss = {
                showMigrationDialog = false
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
                    onFavoriteToggle = { viewModel.toggleFavorite(it) }
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
fun PrivacyDialog(
    onDismiss: () -> Unit,
    onAgree: () -> Unit,
    onDisagree: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("隐私政策") },
        text = {
            Text("感谢您使用OMaster应用！在使用前，请您仔细阅读我们的隐私政策。我们重视您的隐私保护。")
        },
        confirmButton = {
            Button(onClick = onAgree) {
                Text("同意")
            }
        },
        dismissButton = {
            TextButton(onClick = onDisagree) {
                Text("拒绝")
            }
        }
    )
}

@Composable
fun MigrationDialog(
    isMigrating: Boolean,
    error: String?,
    onStartMigration: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("数据更新") },
        text = {
            Column {
                if (isMigrating) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.padding(8.dp))
                        Text("正在更新数据，请稍候...")
                    }
                } else if (error != null) {
                    Column {
                        Text("更新失败: $error")
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    Text("我们有一些重要的数据更新，这将提供更好的用户体验。是否现在开始更新？")
                }
            }
        },
        confirmButton = {
            if (!isMigrating) {
                Button(
                    onClick = onStartMigration,
                    enabled = error == null
                ) {
                    Text("立即更新")
                }
            }
        },
        dismissButton = {
            if (!isMigrating) {
                TextButton(onClick = onSkip) {
                    Text("跳过")
                }
            }
        }
    )
}

fun finishActivity() {
    System.exit(0)
}
