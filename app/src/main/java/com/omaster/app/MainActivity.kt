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
import com.omaster.app.ui.screens.DetailScreen
import com.omaster.app.ui.screens.HomeScreen
import com.omaster.app.ui.screens.SettingsScreen
import com.omaster.app.ui.theme.OMasterTheme

class MainActivity : ComponentActivity() {
    private val repository = PresetRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OMasterTheme {
                OMasterApp(repository)
            }
        }
    }
}

@Composable
fun OMasterApp(repository: PresetRepository) {
    val navController = rememberNavController()
    var selectedPreset by remember { mutableStateOf<Preset?>(null) }

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
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        composable("detail") {
            selectedPreset?.let { preset ->
                DetailScreen(
                    preset = preset,
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
