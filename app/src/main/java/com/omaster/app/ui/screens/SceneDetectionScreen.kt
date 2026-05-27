package com.omaster.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaster.app.model.Preset
import com.omaster.app.ui.components.PresetCard
import com.omaster.app.viewmodel.MainViewModel

data class SceneType(
    val id: String,
    val name: String,
    val icon: String,
    val tags: List<String>
)

val sceneTypes = listOf(
    SceneType("landscape", "风景", "🏔️", listOf("风景", "自然")),
    SceneType("portrait", "人像", "👤", listOf("人像")),
    SceneType("night", "夜景", "🌃", listOf("夜景")),
    SceneType("food", "美食", "🍜", listOf("美食")),
    SceneType("street", "街拍", "📷", listOf("街拍")),
    SceneType("architecture", "建筑", "🏛️", listOf("建筑")),
    SceneType("sunset", "日落", "🌅", listOf("日落"))
)

@Composable
fun SceneDetectionScreen(
    onBack: () -> Unit,
    onPresetSelected: (Preset) -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    var selectedScene by remember { mutableStateOf<SceneType?>(null) }

    val recommendedPresets = remember(selectedScene, presets) {
        selectedScene?.let { scene ->
            presets.filter { preset ->
                preset.tags.any { tag ->
                    scene.tags.contains(tag)
                }
            }
        } ?: presets
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("场景检测") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                "选择拍摄场景",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(16.dp, 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sceneTypes) { scene ->
                    FilterChip(
                        selected = selectedScene == scene,
                        onClick = { selectedScene = if (selectedScene == scene) null else scene },
                        label = { Text("${scene.icon} ${scene.name}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Divider()

            if (selectedScene != null) {
                Text(
                    "推荐预设",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                )

                if (recommendedPresets.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无推荐预设")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(recommendedPresets, key = { it.id }) { preset ->
                            PresetCard(
                                preset = preset,
                                onClick = { onPresetSelected(preset) },
                                onFavoriteToggle = { viewModel.toggleFavorite(preset) }
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "选择一个场景",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "我们将为您推荐合适的预设",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
