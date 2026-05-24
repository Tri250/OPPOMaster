package com.omaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.omaster.app.data.PresetRepository
import com.omaster.app.model.Preset
import com.omaster.app.ui.theme.*

@Composable
fun DetailScreen(
    preset: Preset,
    repository: PresetRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPreset by remember {
        produceState(initialValue = preset) {
            repository.presets.collect { presets ->
                value = presets.find { it.id == preset.id } ?: preset
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = DeepSpace,
        topBar = {
            TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = TextPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            actions = {
                IconButton(onClick = { repository.toggleFavorite(currentPreset.id) }) {
                    Icon(
                        imageVector = if (currentPreset.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (currentPreset.isFavorite) "取消收藏" else "收藏",
                        tint = if (currentPreset.isFavorite) AccentPrimary else TextSecondary
                    )
                }
                IconButton(onClick = { /* 分享功能 */ }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "分享",
                        tint = TextPrimary
                    )
                }
            }
        )
    } { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AsyncImage(
                    model = "https://picsum.photos/seed/${preset.coverPath}/800/600",
                    contentDescription = preset.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    DeepSpace
                                ),
                                startY = 200f
                            )
                        )
                )

                if (currentPreset.cameraParams?.hasselblad_hncs == true) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        color = HasselbladOrange,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "HNCS",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepSpace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = currentPreset.name,
                    style = MaterialTheme.typography.displaySmall,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (currentPreset.deviceModel.isNotEmpty()) {
                    Surface(
                        color = GlassBackground,
                        shape = RoundedCornerShape(12.dp)
                    {
                        Text(
                            text = "适配: ${currentPreset.deviceModel}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                currentPreset.cameraParams?.let { params ->
                    Text(
                        text = "相机参数",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GridParamsGrid(params)

                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (currentPreset.sections.isNotEmpty()) {
                    Text(
                        text = "详细说明",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    currentPreset.sections.forEach { section ->
                        SectionItem(section)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                Button(
                    onClick = { repository.selectPreset(currentPreset) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "应用预设",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepSpace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun GridParamsGrid(params: com.omaster.app.model.CameraParams) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ParamItem("ISO", params.iso.toString())
        ParamItem("快门", params.shutter)
        ParamItem("曝光补偿", params.ev)
        ParamItem("白平衡", params.wb)
        if (params.filter.isNotEmpty()) {
            ParamItem("滤镜", params.filter)
        }
    }
}

@Composable
fun ParamItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DeepSpaceLight)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = AccentPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SectionItem(section: com.omaster.app.model.Section) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DeepSpaceLight
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleLarge,
                color = AccentPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = section.content,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
