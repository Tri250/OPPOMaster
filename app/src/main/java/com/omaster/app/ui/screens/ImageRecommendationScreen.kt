package com.omaster.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omaster.app.model.Preset
import com.omaster.app.model.PresetCategory
import com.omaster.app.ui.components.PresetCard
import com.omaster.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageRecommendationScreen(
    onBack: () -> Unit,
    onPresetClick: (Preset) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<PresetCategory?>(null) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val recommendedPresets = remember(selectedCategory) {
        getRecommendedPresetsForCategory(selectedCategory)
    }

    Scaffold(
        modifier = modifier,
        containerColor = OppoDeepSpace,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "精选影像推荐",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OppoTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = OppoTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCategoryPicker = true }) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "选择分类",
                            tint = HasselbladOrangePro
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OppoDeepSpace
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedCategory != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = HasselbladOrangePro.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "当前分类",
                                style = MaterialTheme.typography.labelMedium,
                                color = OppoTextSecondary
                            )
                            Text(
                                text = selectedCategory!!.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                color = HasselbladOrangePro,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = { selectedCategory = null }) {
                            Text("清除", color = OppoTextSecondary)
                        }
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    Text(
                        text = if (selectedCategory != null) {
                            "为您推荐 ${selectedCategory!!.displayName} 类预设"
                        } else {
                            "为您精选的哈苏大师预设"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = OppoTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (recommendedPresets.isEmpty()) {
                    item {
                        EmptyRecommendationState(
                            category = selectedCategory,
                            onSelectCategory = { showCategoryPicker = true }
                        )
                    }
                } else {
                    items(recommendedPresets, key = { it.id }) { preset ->
                        PresetCard(
                            preset = preset,
                            onClick = { onPresetClick(preset) }
                        )
                    }
                }
            }
        }
    }

    if (showCategoryPicker) {
        CategoryPickerDialog(
            selectedCategory = selectedCategory,
            onCategorySelected = { category ->
                selectedCategory = category
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false }
        )
    }
}

@Composable
private fun EmptyRecommendationState(
    category: PresetCategory?,
    onSelectCategory: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = OppoCardSurface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = if (category != null) Icons.Default.PhotoCamera else Icons.Default.CameraAlt,
                contentDescription = null,
                tint = OppoTextSecondary,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = if (category != null) {
                    "暂无 ${category.displayName} 类预设"
                } else {
                    "暂无推荐预设"
                },
                style = MaterialTheme.typography.titleMedium,
                color = OppoTextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "试试选择其他分类吧",
                style = MaterialTheme.typography.bodyMedium,
                color = OppoTextSecondary,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onSelectCategory,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HasselbladOrangePro
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("选择分类", color = OppoDeepSpace)
            }
        }
    }
}

@Composable
private fun CategoryPickerDialog(
    selectedCategory: PresetCategory?,
    onCategorySelected: (PresetCategory) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择影像分类",
                style = MaterialTheme.typography.titleLarge,
                color = OppoTextPrimary
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PresetCategory.entries.toList()) { category ->
                    CategoryItem(
                        category = category,
                        isSelected = category == selectedCategory,
                        onClick = { onCategorySelected(category) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = OppoTextSecondary)
            }
        },
        containerColor = OppoLightSurface,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun CategoryItem(
    category: PresetCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                HasselbladOrangePro.copy(alpha = 0.15f)
            } else {
                OppoCardSurface
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) HasselbladOrangePro else OppoTextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "已选择",
                    tint = HasselbladOrangePro,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun getRecommendedPresetsForCategory(category: PresetCategory?): List<Preset> {
    return getSamplePresets().filter { preset ->
        if (category == null) true
        else preset.matchesCategory(category)
    }
}

private fun getSamplePresets(): List<Preset> {
    return listOf(
        Preset(
            id = "rec_001",
            name = "哈苏人像经典",
            coverPath = "hasselblad_portrait",
            deviceModel = "OPPO Find X8 Pro",
            source = "official",
            category = PresetCategory.PORTRAIT
        ),
        Preset(
            id = "rec_002",
            name = "自然风光大师",
            coverPath = "landscape_master",
            deviceModel = "OPPO Find X8 Pro",
            source = "official",
            category = PresetCategory.LANDSCAPE
        ),
        Preset(
            id = "rec_003",
            name = "城市夜景之王",
            coverPath = "night_city",
            deviceModel = "OPPO Find X8 Ultra",
            source = "official",
            category = PresetCategory.NIGHT
        ),
        Preset(
            id = "rec_004",
            name = "美食诱人",
            coverPath = "food_delicious",
            deviceModel = "OPPO Find X8 Pro",
            source = "official",
            category = PresetCategory.FOOD
        ),
        Preset(
            id = "rec_005",
            name = "金色时刻",
            coverPath = "golden_hour",
            deviceModel = "OnePlus 12",
            source = "official",
            category = PresetCategory.SUNSET
        ),
        Preset(
            id = "rec_006",
            name = "哈苏街头模式",
            coverPath = "street_mode",
            deviceModel = "OnePlus 13 Pro",
            source = "official",
            category = PresetCategory.STREET
        ),
        Preset(
            id = "rec_007",
            name = "复古胶片",
            coverPath = "vintage_film",
            deviceModel = "OPPO Reno 12",
            source = "community",
            category = PresetCategory.VINTAGE
        ),
        Preset(
            id = "rec_008",
            name = "经典黑白",
            coverPath = "classic_bw",
            deviceModel = "OPPO Find X8 Pro",
            source = "community",
            category = PresetCategory.BLACK_WHITE
        )
    )
}
