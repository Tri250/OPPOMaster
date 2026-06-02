package com.omaster.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.omaster.app.data.ThemeMode
import com.omaster.app.model.Preset
import com.omaster.app.ui.animation.ColorOSAnimationDuration
import com.omaster.app.ui.animation.ColorOSEasing
import com.omaster.app.ui.animation.ColorOSScale
import com.omaster.app.ui.theme.*
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProDetailScreen(
    preset: Preset,
    onBack: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onApplyPreset: () -> Unit,
    themeMode: Int,
    modifier: Modifier = Modifier
) {
    val isDark = themeMode == ThemeMode.DARK.value
    val scrollState = rememberScrollState()
    var showFullScreenPreview by remember { mutableStateOf(false) }
    var showComparison by remember { mutableStateOf(false) }
    
    Scaffold(
        containerColor = if (isDark) Colors.Background else ColorOSLightBackground,
        topBar = {
            ProDetailTopBar(
                onBack = onBack,
                onFavoriteToggle = onFavoriteToggle,
                onShare = { },
                isFavorite = preset.isFavorite,
                isDark = isDark
            )
        },
        bottomBar = {
            ProDetailBottomBar(
                onApplyPreset = onApplyPreset,
                canModify = preset.canModify,
                isDark = isDark
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
        ) {
            ProDetailHeader(
                preset = preset,
                isDark = isDark,
                onPreviewClick = { showFullScreenPreview = true }
            )
            
            Column(
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                ProDetailTitle(
                    preset = preset,
                    isDark = isDark
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                ProStatsSection(
                    preset = preset,
                    isDark = isDark
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                ProParamsSection(
                    preset = preset,
                    isDark = isDark
                )
                
                if (preset.isHncsCertified) {
                    Spacer(modifier = Modifier.height(24.dp))
                    ProHncsCertificationSection(
                        certificationText = preset.getHncsCertificationText(),
                        isDark = isDark
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                ProDescriptionSection(
                    preset = preset,
                    isDark = isDark
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                ProVersionSection(
                    version = preset.getVersionInfo(),
                    publishDate = preset.getFormattedPublishDate(),
                    source = preset.source,
                    isOfficial = preset.isOfficialSource,
                    isDark = isDark
                )
                
                if (showComparison) {
                    Spacer(modifier = Modifier.height(24.dp))
                    ProComparisonSection(
                        originalImageUrl = preset.coverUrl,
                        presetImageUrl = "https://picsum.photos/seed/${preset.coverPath}_applied/800/600",
                        isDark = isDark
                    )
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
    
    if (showFullScreenPreview) {
        FullScreenPreviewDialog(
            imageUrl = preset.coverUrl,
            presetName = preset.name,
            onDismiss = { showFullScreenPreview = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProDetailTopBar(
    onBack: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onShare: () -> Unit,
    isFavorite: Boolean,
    isDark: Boolean
) {
    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = if (isDark) Colors.OnSurface else ColorOSLightTextPrimary
                )
            }
        },
        actions = {
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "取消收藏" else "收藏",
                    tint = if (isFavorite) Colors.AccentRed else if (isDark) Colors.OnSurface else ColorOSLightTextPrimary
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "分享",
                    tint = if (isDark) Colors.OnSurface else ColorOSLightTextPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun ProDetailHeader(
    preset: Preset,
    isDark: Boolean,
    onPreviewClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "previewScale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() }.also { interactionSource ->
                    val listener = object : androidx.compose.foundation.interaction.DragInteraction.Start {
                        override fun toString(): String = "Start"
                    }
                },
                indication = null,
                onClick = onPreviewClick
            )
    ) {
        AsyncImage(
            model = preset.coverUrl.ifEmpty { "https://picsum.photos/seed/${preset.coverPath}/800/600" },
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
                            Color.Transparent,
                            if (isDark) Colors.Background.copy(alpha = 0.8f) else ColorOSLightBackground
                        )
                    )
                )
        )
        
        if (preset.isHncsCertified) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Colors.HasselbladOrange
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black
                    ) {
                        Text(
                            text = "H",
                            color = Colors.HasselbladOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                    Text(
                        text = "HNCS CERTIFIED",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            shape = CircleShape,
            color = Colors.GlassBackground.copy(alpha = 0.8f),
            onClick = onPreviewClick
        ) {
            Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = "放大查看",
                tint = Colors.OnSurface,
                modifier = Modifier
                    .size(44.dp)
                    .padding(10.dp)
            )
        }
    }
}

@Composable
private fun ProDetailTitle(
    preset: Preset,
    isDark: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = preset.name,
            style = Typography.DisplaySmall,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Colors.OnSurface else ColorOSLightTextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isDark) Colors.SurfaceVariant else ColorOSLightSurface
                ) {
                    Text(
                        text = preset.author.first().toString().uppercase(),
                        color = if (isDark) Colors.OnSurface else ColorOSLightTextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                Column {
                    Text(
                        text = preset.author,
                        style = Typography.TitleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Colors.OnSurface else ColorOSLightTextPrimary
                    )
                    Text(
                        text = preset.getDeviceDisplay(),
                        style = Typography.BodySmall,
                        color = if (isDark) Colors.OnSurfaceVariant else ColorOSLightTextTertiary
                    )
                }
            }
            
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = when (preset.sceneType.lowercase()) {
                    "portrait" -> Colors.AccentOrange.copy(alpha = 0.15f)
                    "landscape" -> Colors.AccentGreen.copy(alpha = 0.15f)
                    "night" -> Colors.AccentBlue.copy(alpha = 0.15f)
                    else -> Colors.GlassBackground
                }
            ) {
                Text(
                    text = preset.getSceneTypeDisplay(),
                    style = Typography.LabelMedium,
                    fontWeight = FontWeight.Medium,
                    color = when (preset.sceneType.lowercase()) {
                        "portrait" -> Colors.AccentOrange
                        "landscape" -> Colors.AccentGreen
                        "night" -> Colors.AccentBlue
                        else -> Colors.OnSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ProStatsSection(
    preset: Preset,
    isDark: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            icon = Icons.Default.Download,
            value = preset.getFormattedDownloadCount(),
            label = "下载量",
            isDark = isDark
        )
        StatItem(
            icon = Icons.Default.Favorite,
            value = preset.getFormattedFavoriteCount(),
            label = "收藏量",
            isDark = isDark
        )
        StatItem(
            icon = Icons.Default.Star,
            value = String.format("%.1f", preset.rating),
            label = "评分",
            isDark = isDark
        )
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    isDark: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Colors.HasselbladOrange,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = Typography.TitleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Colors.OnSurface else ColorOSLightTextPrimary
        )
        Text(
            text = label,
            style = Typography.LabelSmall,
            color = if (isDark) Colors.OnSurfaceVariant else ColorOSLightTextTertiary
        )
    }
}

@Composable
private fun ProParamsSection(
    preset: Preset,
    isDark: Boolean
) {
    val params = preset.cameraParams
    
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Text(
                text = "相机参数",
                style = Typography.TitleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Colors.OnSurface else ColorOSLightTextPrimary
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            if (params != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (params.hasselblad_hncs) {
                        GlassChip(
                            text = "HNCS",
                            selected = true,
                            onClick = {},
                            modifier = Modifier
                        )
                    }

                    if (params.focal_length.isNotEmpty()) {
                        ParamChip(label = "焦距", value = "${params.focal_length}mm")
                    }

                    if (params.aperture.isNotEmpty()) {
                        ParamChip(label = "光圈", value = "f/${params.aperture}")
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (params.shutter.isNotEmpty()) {
                        ParamChip(label = "快门", value = params.shutter)
                    }

                    ParamChip(label = "ISO", value = params.iso.toString())

                    if (params.wb.isNotEmpty()) {
                        ParamChip(label = "白平衡", value = "${params.wb}K")
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ParamChip(label = "曝光补偿", value = "${if (params.exposureCompensation >= 0) "+" else ""}${params.exposureCompensation} EV")

                    params.focus_distance?.let {
                        ParamChip(label = "对焦距离", value = it)
                    }
                }
                
                if (params.ai_scene_recognition != null || params.hasselblad_master_style != null) {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    
                    Text(
                        text = "AI 智能参数",
                        style = Typography.LabelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Colors.HasselbladOrange
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    
                    params.ai_scene_recognition?.let {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AI 场景识别",
                                style = Typography.BodyMedium,
                                color = if (isDark) Colors.OnSurfaceVariant else ColorOSLightTextSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Colors.AccentGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = it,
                                    style = Typography.LabelMedium,
                                    color = Colors.AccentGreen,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    
                    params.hasselblad_master_style?.let {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "哈苏大师风格",
                                style = Typography.BodyMedium,
                                color = if (isDark) Colors.OnSurfaceVariant else ColorOSLightTextSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Colors.HasselbladOrange.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = it,
                                    style = Typography.LabelMedium,
                                    color = Colors.HasselbladOrange,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "暂无参数信息",
                    style = Typography.BodyMedium,
                    color = if (isDark) Colors.OnSurfaceVariant else ColorOSLightTextTertiary
                )
            }
        }
    }
}

@Composable
private fun ParamChip(
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Colors.GlassBackground.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = Typography.LabelSmall,
                color = Colors.OnSurfaceVariant
            )
            Text(
                text = value,
                style = Typography.TitleMedium,
                fontWeight = FontWeight.Bold,
                color = Colors.HasselbladOrange
            )
        }
    }
}

@Composable
private fun ProHncsCertificationSection(
    certificationText: String,
    isDark: Boolean
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Colors.HasselbladOrange
                ) {
                    Text(
                        text = "H",
                        color = Colors.OnPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Text(
                    text = "HNCS 认证",
                    style = Typography.TitleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Colors.HasselbladOrange
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            Text(
                text = certificationText,
                style = Typography.BodyMedium,
                color = if (isDark) Colors.OnSurface else ColorOSLightTextSecondary
            )
        }
    }
}

@Composable
private fun ProDescriptionSection(
    preset: Preset,
    isDark: Boolean
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Text(
                text = "预设介绍",
                style = Typography.TitleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Colors.OnSurface else ColorOSLightTextPrimary
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            if (preset.description.isNotEmpty()) {
                Text(
                    text = preset.description,
                    style = Typography.BodyMedium,
                    color = if (isDark) Colors.OnSurface else ColorOSLightTextSecondary
                )
            } else {
                Text(
                    text = "这是一款专业的摄影预设，为您的照片带来独特的视觉风格。",
                    style = Typography.BodyMedium,
                    color = if (isDark) Colors.OnSurfaceVariant else ColorOSLightTextTertiary
                )
            }
            
            if (preset.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.md))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    preset.tags.forEach { tag ->
                        GlassChip(
                            text = tag,
                            selected = false,
                            onClick = {}
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProVersionSection(
    version: String,
    publishDate: String,
    source: String,
    isOfficial: Boolean,
    isDark: Boolean
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Text(
                text = "版本信息",
                style = Typography.TitleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Colors.OnSurface else ColorOSLightTextPrimary
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(label = "版本", value = version)
                InfoItem(label = "发布日期", value = publishDate)
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(label = "来源", value = if (isOfficial) "哈苏官方" else "用户上传")
                InfoItem(label = "可修改", value = if (isOfficial) "否" else "是")
            }
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = Typography.LabelSmall,
            color = Colors.OnSurfaceVariant
        )
        Text(
            text = value,
            style = Typography.BodyMedium,
            fontWeight = FontWeight.Medium,
            color = Colors.HasselbladOrange
        )
    }
}

@Composable
private fun ProComparisonSection(
    originalImageUrl: String,
    presetImageUrl: String,
    isDark: Boolean
) {
    var sliderPosition by remember { mutableFloatStateOf(0.5f) }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "效果对比",
            style = Typography.TitleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Colors.OnSurface else ColorOSLightTextPrimary
        )
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(Radius.lg))
        ) {
            AsyncImage(
                model = originalImageUrl.ifEmpty { "https://picsum.photos/seed/${originalImageUrl}_original/800/600" },
                contentDescription = "原图",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth(sliderPosition)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(Radius.lg))
            ) {
                AsyncImage(
                    model = presetImageUrl,
                    contentDescription = "应用预设后",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth(sliderPosition)
                    .height(2.dp)
                    .align(Alignment.CenterStart)
                    .background(Colors.HasselbladOrange)
            )
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = (sliderPosition * 300 - 20).dp)
                    .clip(CircleShape)
                    .background(Colors.HasselbladOrange)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val newPosition = (change.position.x / size.width).coerceIn(0f, 1f)
                            sliderPosition = newPosition
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Compare,
                    contentDescription = "拖动对比",
                    tint = Colors.OnPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Text(
                text = "原图",
                style = Typography.LabelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            
            Text(
                text = "应用预设",
                style = Typography.LabelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.sm))
        
        Text(
            text = "← 拖动滑块查看对比效果 →",
            style = Typography.LabelSmall,
            color = Colors.OnSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FullScreenPreviewDialog(
    imageUrl: String,
    presetName: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 4f)
                    if (scale > 1f) {
                        offset = Offset(
                            x = offset.x + pan.x,
                            y = offset.y + pan.y
                        )
                    } else {
                        offset = Offset.Zero
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                    onTap = { onDismiss() }
                )
            }
    ) {
        AsyncImage(
            model = imageUrl.ifEmpty { "https://picsum.photos/seed/${imageUrl}/1200/900" },
            contentDescription = presetName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
        
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            shape = CircleShape,
            color = Colors.GlassBackground.copy(alpha = 0.8f),
            onClick = onDismiss
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = Colors.OnSurface,
                modifier = Modifier
                    .size(44.dp)
                    .padding(10.dp)
            )
        }
        
        Text(
            text = "双指缩放 · 点击关闭",
            style = Typography.LabelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ProDetailBottomBar(
    onApplyPreset: () -> Unit,
    canModify: Boolean,
    isDark: Boolean
) {
    Surface(
        color = if (isDark) Colors.Background else ColorOSLightBackground,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = onApplyPreset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Colors.HasselbladOrange,
                    contentColor = Colors.OnPrimary
                ),
                enabled = canModify
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (canModify) "应用预设" else "官方预设不可修改",
                    style = Typography.TitleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
