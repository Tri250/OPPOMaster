package com.omaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.omaster.app.data.PresetRepository
import com.omaster.app.model.Preset
import com.omaster.app.service.AiService
import com.omaster.app.ui.screens.AiFineTuneScreen
import com.omaster.app.ui.screens.DetailScreen
import com.omaster.app.ui.screens.HomeScreen
import com.omaster.app.ui.screens.SceneDetectionScreen
import com.omaster.app.ui.screens.SettingsScreen
import com.omaster.app.ui.theme.OMasterTheme

class MainActivity : ComponentActivity() {
    private val repository = PresetRepository()
    private val aiService = AiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OMasterTheme {
                OMasterApp(repository, aiService)
            }
        }
    }
}

@Composable
fun OMasterApp(repository: PresetRepository, aiService: AiService) {
    val navController = rememberNavController()
    var selectedPreset by remember { mutableStateOf<Preset?>(null) }
    val presets by repository.presets.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                repository = repository,
                onPresetClick = { preset ->
                    selectedPreset = preset
                    navController.navigate("detail")
                },
                onSettingsClick = { navController.navigate("settings") },
                onSceneDetectionClick = { navController.navigate("scene_detection") },
                onAiFineTuneClick = { navController.navigate("ai_finetune") }
            )
        }
        composable("detail") {
            selectedPreset?.let { preset ->
                DetailScreen(
                    preset = preset,
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onAiFineTuneClick = { navController.navigate("ai_finetune") }
                )
            }
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("scene_detection") {
            SceneDetectionScreen(
                aiService = aiService,
                allPresets = presets,
                onBack = { navController.popBackStack() },
                onPresetClick = { preset ->
                    selectedPreset = preset
                    navController.navigate("detail")
                },
                onFavoriteToggle = { repository.toggleFavorite(it) }
            )
        }
        composable("ai_finetune") {
            AiFineTuneScreen(
                aiService = aiService,
                preset = selectedPreset,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
