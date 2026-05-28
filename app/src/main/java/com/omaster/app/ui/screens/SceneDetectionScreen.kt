package com.omaster.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaster.app.model.Preset
import com.omaster.app.model.SceneType
import com.omaster.app.service.AiService
import com.omaster.app.ui.components.PresetCard
import com.omaster.app.ui.theme.AccentPrimary
import com.omaster.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SceneDetectionScreen(
    onBack: () -> Unit,
    onPresetClick: (Preset) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
    aiService: AiService = AiService()
) {
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    var detectedScene by remember { mutableStateOf<SceneType?>(null) }
    var recommendedPresets by remember { mutableStateOf<List<Preset>>(emptyList()) }
    var isDetecting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "场景检测",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "场景检测",
                        modifier = Modifier.size(64.dp),
                        tint = AccentPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "AI 场景检测",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "自动识别场景并推荐最佳预设",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                isDetecting = true
                                scope.launch {
                                    detectedScene = aiService.detectScene()
                                    recommendedPresets = aiService.getRecommendedPresets(
                                        detectedScene ?: SceneType.UNKNOWN,
                                        presets
                                    )
                                    isDetecting = false
                                }
                            },
                            enabled = !isDetecting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentPrimary
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "拍照检测",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isDetecting) "检测中..." else "拍照检测",
                                color = androidx.compose.ui.graphics.Color.Black
                            )
                        }
                        
                        Button(
                            onClick = {
                                isDetecting = true
                                scope.launch {
                                    detectedScene = aiService.detectScene()
                                    recommendedPresets = aiService.getRecommendedPresets(
                                        detectedScene ?: SceneType.UNKNOWN,
                                        presets
                                    )
                                    isDetecting = false
                                }
                            },
                            enabled = !isDetecting,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "从相册选择",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("相册选择")
                        }
                    }
                }
            }

            if (isDetecting) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = AccentPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "正在分析场景...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (detectedScene != null) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = AccentPrimary,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(
                                text = detectedScene?.displayName ?: "",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = androidx.compose.ui.graphics.Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "检测到场景",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = detectedScene?.description ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "推荐预设",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(recommendedPresets, key = { it.id }) { preset ->
                        PresetCard(
                            preset = preset,
                            onClick = { onPresetClick(preset) },
                            onFavoriteToggle = { viewModel.toggleFavorite(preset) }
                        )
                    }
                }
            }
        }
    }
}