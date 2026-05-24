package com.omaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
    onSceneDetectionClick: () -> Unit,
    onAiFineTuneClick: () -> Unit,
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
            item {
                FeatureButtons(
                    onSceneDetectionClick = onSceneDetectionClick,
                    onAiFineTuneClick = onAiFineTuneClick
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            item {
                Text(
                    text = "哈苏大师预设",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
            }
            
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

@Composable
fun FeatureButtons(
    onSceneDetectionClick: () -> Unit,
    onAiFineTuneClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FeatureCard(
            title = "AI 场景识别",
            description = "智能识别场景，推荐最佳预设",
            icon = Icons.Default.AutoFixHigh,
            iconColor = AccentPrimary,
            onClick = onSceneDetectionClick,
            modifier = Modifier.weight(1f)
        )
        
        FeatureCard(
            title = "AI 样张微调",
            description = "优化您的拍摄样张",
            icon = Icons.Default.AutoAwesome,
            iconColor = HasselbladOrange,
            onClick = onAiFineTuneClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FeatureCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = DeepSpaceLight
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
