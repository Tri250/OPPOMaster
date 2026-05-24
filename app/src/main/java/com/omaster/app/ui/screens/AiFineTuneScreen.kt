package com.omaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.omaster.app.model.AiAdjustmentParams
import com.omaster.app.model.Preset
import com.omaster.app.service.AiService
import com.omaster.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AiFineTuneScreen(
    aiService: AiService,
    preset: Preset?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var adjustmentParams by remember { mutableStateOf(AiAdjustmentParams.DEFAULT) }
    var selectedImage by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = DeepSpace,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI 样张微调",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                },
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
                    containerColor = DeepSpace
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ImagePreviewArea(
                selectedImage = selectedImage,
                onSelectImage = { 
                    selectedImage = "https://picsum.photos/seed/finetune_${System.currentTimeMillis()}/800/600"
                    showResult = false
                }
            )

            preset?.let {
                PresetInfoCard(preset = it)
            }

            Button(
                onClick = {
                    scope.launch {
                        isProcessing = true
                        adjustmentParams = aiService.fineTuneImage(selectedImage ?: "", preset)
                        isProcessing = false
                        showResult = true
                    }
                },
                enabled = selectedImage != null && !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HasselbladOrange
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = DeepSpace,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI 微调中...",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepSpace
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "微调",
                        tint = DeepSpace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedImage == null) "请先选择样张" else "开始 AI 智能微调",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepSpace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showResult) {
                AdjustmentResultsCard(params = adjustmentParams)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                ActionButtons(
                    onApply = { /* 应用参数 */ },
                    onSave = { /* 保存参数 */ }
                )
            }
        }
    }
}

@Composable
fun ImagePreviewArea(
    selectedImage: String?,
    onSelectImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .clickable(onClick = onSelectImage),
        colors = CardDefaults.cardColors(
            containerColor = DeepSpaceLight
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (selectedImage != null) {
                AsyncImage(
                    model = selectedImage,
                    contentDescription = "待微调样张",
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
                                    DeepSpace.copy(alpha = 0.7f)
                                ),
                                startY = 200f
                            )
                        )
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    color = HasselbladOrange,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.ChangeCircle,
                        contentDescription = "更换图片",
                        tint = DeepSpace,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "添加样张",
                        tint = TextSecondary,
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = "选择您拍摄的样张",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "AI 将根据哈苏大师预设进行智能优化",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun PresetInfoCard(preset: Preset, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DeepSpaceLight
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = AccentPrimary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "https://picsum.photos/seed/${preset.coverPath}/100/100",
                        contentDescription = preset.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "当前预设",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
            }
            
            if (preset.cameraParams?.hasselblad_hncs == true) {
                Surface(
                    color = HasselbladOrange,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "HNCS",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepSpace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AdjustmentResultsCard(params: AiAdjustmentParams, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DeepSpaceLight
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = HasselbladOrange.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI 优化",
                        tint = HasselbladOrange,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Column {
                    Text(
                        text = "AI 微调参数",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "哈苏色彩科学优化方案",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            params.toDisplayMap().forEach { (label, value) ->
                if (value != 0f) {
                    AdjustmentRow(label = label, value = value)
                }
            }
        }
    }
}

@Composable
fun AdjustmentRow(label: String, value: Float, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary
        )
        Surface(
            color = if (value > 0) AccentPrimary.copy(alpha = 0.2f) else AccentSecondary.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (value > 0) "+$value" else "$value",
                style = MaterialTheme.typography.titleMedium,
                color = if (value > 0) AccentPrimary else AccentSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun ActionButtons(onApply: () -> Unit, onSave: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = "保存"
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "保存参数",
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        Button(
            onClick = onApply,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "应用",
                tint = DeepSpace
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "应用到相机",
                style = MaterialTheme.typography.titleMedium,
                color = DeepSpace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
