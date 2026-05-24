package com.omaster.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.omaster.app.data.PresetRepository
import com.omaster.app.model.Preset
import com.omaster.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    preset: Preset,
    repository: PresetRepository,
    onBack: () -> Unit,
    onAiFineTuneClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentPreset by remember {
        produceState(initialValue = preset) {
            repository.presets.collect { presets ->
                value = presets.find { it.id == preset.id } ?: preset
            }
        }
    }

    val scrollState = rememberScrollState()
    val pagerState = rememberPagerState { 
        if (currentPreset.galleryImages.isEmpty()) 1 else currentPreset.galleryImages.size + 1 
    }

    Scaffold(
        modifier = modifier,
        containerColor = DeepSpace,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    Surface(
                        modifier = Modifier.padding(start = 16.dp),
                        color = GlassBackgroundStrong,
                        shape = CircleShape
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = GlassBackgroundStrong,
                            shape = CircleShape
                        ) {
                            IconButton(onClick = { repository.toggleFavorite(currentPreset.id) }) {
                                Icon(
                                    imageVector = if (currentPreset.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (currentPreset.isFavorite) "取消收藏" else "收藏",
                                    tint = if (currentPreset.isFavorite) AccentPrimary else TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Surface(
                            color = GlassBackgroundStrong,
                            shape = CircleShape
                        ) {
                            IconButton(onClick = { }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "分享",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            )
        },
        bottomBar = {
            BottomActionsBar(
                preset = currentPreset,
                onApply = { repository.selectPreset(currentPreset) },
                onAiFineTune = onAiFineTuneClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            GallerySection(preset = currentPreset, pagerState = pagerState)

            Spacer(modifier = Modifier.height(20.dp))

            ContentSection(preset = currentPreset)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GallerySection(
    preset: Preset,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    val images = remember {
        listOf(preset.coverPath) + preset.galleryImages
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(380.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Box {
                AsyncImage(
                    model = images.getOrNull(page) ?: preset.coverPath,
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
                                    DeepSpace.copy(alpha = 0.3f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    DeepSpace
                                )
                            )
                        )
                )
            }
        }

        if (images.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(images.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Surface(
                        color = if (isSelected) AccentPrimary else GlassBackgroundStrong,
                        shape = CircleShape,
                        modifier = Modifier.size(
                            width = if (isSelected) 24.dp else 8.dp,
                            height = 8.dp
                        )
                    ) { }
                }
            }
        }
    }
}

@Composable
fun ContentSection(
    preset: Preset,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        TitleRow(preset = preset)
        
        DescriptionCard(preset = preset)
        
        ParametersSection(preset = preset)
        
        TipsSection(preset = preset)
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun TitleRow(preset: Preset, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (preset.isNew) {
                Surface(
                    color = AccentPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = "NEW",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (preset.cameraParams?.hasselblad_hncs == true) {
                Surface(
                    color = HasselbladOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "HNCS",
                            tint = HasselbladOrange,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "HNCS",
                            style = MaterialTheme.typography.labelSmall,
                            color = HasselbladOrange,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        Text(
            text = preset.name,
            style = MaterialTheme.typography.displaySmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (preset.author.isNotEmpty()) {
                Surface(
                    color = GlassBackground,
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Surface(
                            color = AccentPrimary.copy(alpha = 0.2f),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "作者",
                                tint = AccentPrimary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(4.dp)
                            )
                        }
                        Text(
                            text = preset.author,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (preset.source.isNotEmpty()) {
                Surface(
                    color = GlassBackground,
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = preset.source,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun DescriptionCard(preset: Preset, modifier: Modifier = Modifier) {
    preset.description?.let { desc ->
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = DeepSpaceLight
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = HasselbladOrange.copy(alpha = 0.15f),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "信息",
                            tint = HasselbladOrange,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Text(
                        text = desc.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = desc.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 1.7.sp
                )
            }
        }
    }
}

@Composable
fun ParametersSection(preset: Preset, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "参数设置",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        preset.sections.forEach { section ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = DeepSpaceLight
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    val items = section.items
                    val gridItems = items.chunked(2)

                    gridItems.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { item ->
                                ParameterItem(
                                    label = item.label,
                                    value = item.value,
                                    modifier = Modifier.weight(item.span.toFloat())
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParameterItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = GlassBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextTertiary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TipsSection(preset: Preset, modifier: Modifier = Modifier) {
    if (preset.cameraParams != null || preset.deviceModel.isNotEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "相机设置",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = DeepSpaceLight
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    preset.cameraParams?.let { params ->
                        QuickParamRow(
                            icon = Icons.Default.Camera,
                            label = "模式",
                            value = params.mode
                        )
                        QuickParamRow(
                            icon = Icons.Default.Filter,
                            label = "滤镜",
                            value = params.filter
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickParamRow(
                                icon = Icons.Default.Speed,
                                label = "ISO",
                                value = params.iso.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            QuickParamRow(
                                icon = Icons.Default.Timer,
                                label = "快门",
                                value = params.shutter,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickParamRow(
                                icon = Icons.Default.Exposure,
                                label = "曝光",
                                value = params.ev,
                                modifier = Modifier.weight(1f)
                            )
                            QuickParamRow(
                                icon = Icons.Default.BrightnessMedium,
                                label = "白平衡",
                                value = params.wb,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (preset.deviceModel.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = GlassBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = AccentSecondary.copy(alpha = 0.15f),
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = "设备",
                                        tint = AccentSecondary,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "推荐机型",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextTertiary
                                    )
                                    Text(
                                        text = preset.deviceModel,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickParamRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = GlassBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = AccentPrimary.copy(alpha = 0.15f),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = AccentPrimary,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BottomActionsBar(
    preset: Preset,
    onApply: () -> Unit,
    onAiFineTune: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        color = Color.Transparent
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onAiFineTune != null) {
                OutlinedButton(
                    onClick = onAiFineTune,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = HasselbladOrange
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, HasselbladOrange.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI微调",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI 微调",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Button(
                onClick = onApply,
                modifier = if (onAiFineTune != null) Modifier.weight(1.3f) else Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "应用",
                    tint = DeepSpace,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "应用预设",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepSpace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
