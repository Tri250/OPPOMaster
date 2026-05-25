package com.omaster.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaster.app.model.Preset
import com.omaster.app.model.SceneTag
import com.omaster.app.model.StyleType
import com.omaster.app.ui.components.FilterChips
import com.omaster.app.ui.components.PresetCard
import com.omaster.app.ui.components.SearchBar
import com.omaster.app.ui.theme.*
import com.omaster.app.viewmodel.FilterType
import com.omaster.app.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    onPresetClick: (Preset) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    
    var selectedStyle by remember { mutableStateOf<StyleType?>(null) }
    var selectedScene by remember { mutableStateOf<SceneTag?>(null) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "home_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val filteredPresets = remember(presets, searchQuery, filterType, selectedStyle, selectedScene) {
        presets.filter { preset ->
            val matchesQuery = searchQuery.isEmpty() ||
                    preset.name.contains(searchQuery, ignoreCase = true) ||
                    preset.description.contains(searchQuery, ignoreCase = true)
                    
            val matchesFilter = when (filterType) {
                FilterType.ALL -> true
                FilterType.FAVORITES -> preset.isFavorite
                FilterType.HNCS -> preset.cameraParams?.hasselblad_hncs == true
                FilterType.FIND_X -> preset.deviceModel.contains("Find X", ignoreCase = true)
                FilterType.RENO -> preset.deviceModel.contains("Reno", ignoreCase = true)
            }
            
            val matchesStyle = selectedStyle == null || preset.styleType == selectedStyle
            val matchesScene = selectedScene == null || preset.sceneTags.contains(selectedScene)
            
            matchesQuery && matchesFilter && matchesStyle && matchesScene
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "OMaster",
                        style = MaterialTheme.typography.displaySmall,
                        color = LightFieldPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    Surface(
                        modifier = Modifier.padding(4.dp),
                        color = if (isSystemInDarkTheme()) {
                            GlassMediumDark
                        } else {
                            GlassMediumLight
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                    ) {
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "设置",
                                tint = if (isSystemInDarkTheme()) {
                                    LightFieldOnSurfaceDark
                                } else {
                                    LightFieldOnSurfaceLight
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isSystemInDarkTheme()) {
                        LightFieldBackgroundDark
                    } else {
                        LightFieldBackgroundLight
                    }
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            color = LightFieldPrimary.copy(alpha = glowAlpha),
                            radius = size.maxDimension * 0.7f,
                            center = Offset(size.width * 0.2f, size.height * 0.1f)
                        )
                        drawCircle(
                            color = HasselbladOrange.copy(alpha = glowAlpha * 0.6f),
                            radius = size.maxDimension * 0.5f,
                            center = Offset(size.width * 0.8f, size.height * 0.9f)
                        )
                    }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChanged(it) },
                        onClearQuery = { viewModel.onSearchQueryChanged("") }
                    )
                }
                
                item {
                    FilterChips(
                        selectedFilter = filterType,
                        onFilterSelected = { viewModel.onFilterTypeChanged(it) }
                    )
                }
                
                item {
                    StyleFilterRow(
                        selectedStyle = selectedStyle,
                        onStyleSelected = { selectedStyle = if (selectedStyle == it) null else it }
                    )
                }
                
                item {
                    SceneFilterRow(
                        selectedScene = selectedScene,
                        onSceneSelected = { selectedScene = if (selectedScene == it) null else it }
                    )
                }

                if (filteredPresets.isEmpty()) {
                    item {
                        EmptyState(
                            message = if (searchQuery.isNotEmpty() || filterType != FilterType.ALL || 
                                        selectedStyle != null || selectedScene != null)
                                "没有找到匹配的预设" else "暂无预设",
                            modifier = Modifier.fillParentMaxWidth()
                        )
                    }
                } else {
                    items(filteredPresets, key = { it.id }) { preset ->
                        PresetCard(
                            preset = preset,
                            onClick = { onPresetClick(preset) },
                            onFavoriteToggle = { viewModel.toggleFavorite(preset) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun StyleFilterRow(
    selectedStyle: StyleType?,
    onStyleSelected: (StyleType) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "风格",
            style = MaterialTheme.typography.titleMedium,
            color = if (isSystemInDarkTheme()) {
                LightFieldOnSurfaceDark
            } else {
                LightFieldOnSurfaceLight
            },
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        
        androidx.compose.foundation.horizontalScroll(
            rememberScrollState(),
            modifier = Modifier.padding(start = 16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StyleType.values().forEach { style ->
                    FilterChip(
                        selected = selectedStyle == style,
                        onClick = { onStyleSelected(style) },
                        label = {
                            Text(
                                text = style.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LightFieldPrimary.copy(alpha = 0.18f),
                            selectedLabelColor = LightFieldPrimary,
                            containerColor = if (isSystemInDarkTheme()) {
                                LightFieldSurfaceVariantDark
                            } else {
                                LightFieldSurfaceVariantLight
                            },
                            labelColor = if (isSystemInDarkTheme()) {
                                LightFieldOnSurfaceVariantDark
                            } else {
                                LightFieldOnSurfaceVariantLight
                            }
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(100),
                        border = null
                    )
                }
            }
        }
    }
}

@Composable
fun SceneFilterRow(
    selectedScene: SceneTag?,
    onSceneSelected: (SceneTag) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "场景",
            style = MaterialTheme.typography.titleMedium,
            color = if (isSystemInDarkTheme()) {
                LightFieldOnSurfaceDark
            } else {
                LightFieldOnSurfaceLight
            },
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
        )
        
        androidx.compose.foundation.horizontalScroll(
            rememberScrollState(),
            modifier = Modifier.padding(start = 16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    SceneTag.PORTRAIT,
                    SceneTag.LANDSCAPE,
                    SceneTag.NIGHT_SCENE,
                    SceneTag.STREET,
                    SceneTag.FOOD,
                    SceneTag.TRAVEL,
                    SceneTag.SUNRISE,
                    SceneTag.SUNSET,
                    SceneTag.BLUE_HOUR
                ).forEach { scene ->
                    FilterChip(
                        selected = selectedScene == scene,
                        onClick = { onSceneSelected(scene) },
                        label = {
                            Text(
                                text = scene.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OppoGreen.copy(alpha = 0.18f),
                            selectedLabelColor = OppoGreen,
                            containerColor = if (isSystemInDarkTheme()) {
                                LightFieldSurfaceVariantDark
                            } else {
                                LightFieldSurfaceVariantLight
                            },
                            labelColor = if (isSystemInDarkTheme()) {
                                LightFieldOnSurfaceVariantDark
                            } else {
                                LightFieldOnSurfaceVariantLight
                            }
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(100),
                        border = null
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = if (isSystemInDarkTheme()) {
                GlassMediumDark
            } else {
                GlassMediumLight
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSystemInDarkTheme()) {
                    LightFieldOnSurfaceVariantDark
                } else {
                    LightFieldOnSurfaceVariantLight
                },
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}
