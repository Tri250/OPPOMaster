package com.omaster.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omaster.app.data.PresetRepository
import com.omaster.app.model.Preset
import com.omaster.app.ui.components.PresetCard
import com.omaster.app.ui.theme.*

@Composable
fun HomeScreen(
    repository: PresetRepository,
    onPresetClick: (Preset) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presets by repository.presets.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = DeepSpace,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "OMaster",
                        style = MaterialTheme.typography.displaySmall,
                        color = AccentPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSpace
                ),
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = TextPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(presets) { preset ->
                PresetCard(
                    preset = preset,
                    onClick = { onPresetClick(preset) },
                    onFavoriteToggle = { repository.toggleFavorite(preset.id) }
                )
            }
        }
    }
}
