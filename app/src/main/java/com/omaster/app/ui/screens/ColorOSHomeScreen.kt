package com.omaster.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaster.app.R
import com.omaster.app.model.Preset
import com.omaster.app.ui.animation.AnimationConfig
import com.omaster.app.ui.components.ColorOSPresetCard
import com.omaster.app.ui.theme.*
import com.omaster.app.viewmodel.FilterType
import com.omaster.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ColorOSHomeScreen(
    onPresetClick: (Preset) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        isLoading = if (presets.isNotEmpty()) false else true
    }
    
    LaunchedEffect(presets) {
        if (presets.isNotEmpty()) {
            kotlinx.coroutines.delay(200)
            isLoading = false
        }
    }
    
    val filteredPresets = remember(presets, searchQuery, filterType) {
        presets.filter { preset ->
            val matchesQuery = searchQuery.isEmpty() ||
                preset.name.contains(searchQuery, ignoreCase = true) ||
                preset.deviceModel?.contains(searchQuery, ignoreCase = true) == true
            val matchesFilter = when (filterType) {
                FilterType.ALL -> true
                FilterType.FAVORITES -> preset.isFavorite
                FilterType.HNCS -> preset.cameraParams?.hasselblad_hncs == true
                FilterType.FIND_X -> preset.deviceModel?.contains("Find X", ignoreCase = true) == true
                FilterType.RENO -> preset.deviceModel?.contains("Reno", ignoreCase = true) == true
                FilterType.NEW -> true
                FilterType.TRENDING -> true
            }
            matchesQuery && matchesFilter
        }
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            ColorOSTopBar(
                onSettingsClick = onSettingsClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeroSection()
            }
            
            item {
                QuickActionsSection()
            }
            
            item {
                SectionTitle(title = "精选影像推荐")
            }
            
            if (isLoading) {
                items(3) { index ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(
                            animationSpec = tween(
                                durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                                delayMillis = index * 80,
                                easing = AnimationConfig.ColorOSDecelerateEasing
                            )
                        ) + slideInVertically(
                            initialOffsetY = { 30 },
                            animationSpec = tween(
                                durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                                delayMillis = index * 80,
                                easing = AnimationConfig.ColorOSDecelerateEasing
                            )
                        )
                    ) {
                        ColorOSSkeletonCard()
                    }
                }
            } else if (filteredPresets.isEmpty()) {
                item {
                    ColorOSEmptyState(
                        message = if (searchQuery.isNotEmpty()) "没有找到匹配的预设，试试其他关键词" else "暂无预设，看看热门推荐",
                        isSearchEmpty = searchQuery.isEmpty()
                    )
                }
            } else {
                itemsIndexed(
                    items = filteredPresets.take(6),
                    key = { _, preset -> preset.id }
                ) { index, preset ->
                    ColorOSAnimatedPresetCard(
                        preset = preset,
                        index = index,
                        onClick = { onPresetClick(preset) },
                        onFavoriteToggle = { viewModel.toggleFavorite(preset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardSurface
        ),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            OppoOrange.copy(alpha = 0.3f),
                            Hasselblad.copy(alpha = 0.2f),
                            DeepSpace
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(OppoOrange, Hasselblad)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "Camera",
                        tint = DeepSpace,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "OPPO Master",
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "哈苏影像系统级参数中枢",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection() {
    val actions = listOf(
        Triple(Icons.Default.AutoMode, "AI 场景识别", OppoOrange),
        Triple(Icons.Default.Palette, "影像推荐", OppoGreen),
        Triple(Icons.Default.Camera, "参数设置", OceanBlue),
        Triple(Icons.Default.Info, "关于", OppoSunriseGold)
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actions.forEach { (icon, label, color) ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(color.copy(alpha = 0.2f))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = TextPrimary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ColorOSTopBar(
    onSettingsClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "OPPO Master",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Surface(
                    shape = CircleShape,
                    color = SurfaceElevated,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = DeepSpace
        )
    )
}

@Composable
private fun ColorOSAnimatedPresetCard(
    preset: Preset,
    index: Int,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                delayMillis = index * 60,
                easing = AnimationConfig.ColorOSDecelerateEasing
            )
        ) + slideInVertically(
            initialOffsetY = { 40 },
            animationSpec = tween(
                durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                delayMillis = index * 60,
                easing = AnimationConfig.ColorOSDecelerateEasing
            )
        )
    ) {
        ColorOSPresetCard(
            preset = preset,
            onClick = onClick,
            onFavoriteToggle = onFavoriteToggle,
            modifier = Modifier.padding(vertical = 8.dp),
            isNew = index < 3
        )
    }
}

@Composable
private fun ColorOSSkeletonCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = OppoOrange,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun ColorOSEmptyState(
    message: String,
    isSearchEmpty: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSearchEmpty) Icons.Default.PhotoLibrary else Icons.Default.Search,
                contentDescription = null,
                tint = TextTeritary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
