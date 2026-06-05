package com.omaster.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.omaster.app.accessibility.AutoFillAccessibilityService
import com.omaster.app.data.ThemeMode
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.ui.animation.ColorOSAnimationDuration
import com.omaster.app.ui.animation.ColorOSScale
import com.omaster.app.ui.components.ParamApplyFeedbackCard
import com.omaster.app.ui.components.ParamApplyState
import com.omaster.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 预设详情页增强版 - 集成 P0-1 参数应用反馈
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProDetailScreenEnhanced(
    preset: Preset,
    onBack: () -> Unit,
    onFavoriteToggle: () -> Unit,
    themeMode: Int,
    modifier: Modifier = Modifier,
    onApplyPresetWithFeedback: (Preset, (Boolean) -> Unit) -> Unit = { _, callback -> callback(true) }
) {
    val isDark = themeMode == ThemeMode.DARK.value
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    
    var showFullScreenPreview by remember { mutableStateOf(false) }
    var showComparison by remember { mutableStateOf(false) }
    
    // 参数应用状态
    var applyState by remember { mutableStateOf<ParamApplyState>(ParamApplyState.Idle) }
    var previousParams by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isApplying by remember { mutableStateOf(false) }
    
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
            ProDetailBottomBarEnhanced(
                onApplyPreset = {
                    if (!isApplying && preset.canModify) {
                        isApplying = true
                        
                        // 获取当前相机参数作为旧值
                        val currentParams = AutoFillAccessibilityService.getParams()
                        previousParams = currentParams ?: emptyMap()
                        
                        // 设置应用中状态
                        applyState = ParamApplyState.Applying(preset.cameraParams ?: CameraParams())
                        
                        // 调用应用回调
                        onApplyPresetWithFeedback(preset) { success ->
                            if (success) {
                                val newParams = preset.cameraParams?.formatFullParams() ?: emptyMap()
                                applyState = ParamApplyState.Success(
                                    oldParams = previousParams,
                                    newParams = newParams,
                                    appliedParams = preset.cameraParams ?: CameraParams()
                                )
                                
                                // 5秒后自动关闭成功提示
                                scope.launch {
                                    delay(5000)
                                    if (applyState is ParamApplyState.Success) {
                                        applyState = ParamApplyState.Idle
                                    }
                                }
                            } else {
                                applyState = ParamApplyState.Failed("参数写入失败，请检查无障碍服务权限")
                            }
                            isApplying = false
                        }
                    }
                },
                canModify = preset.canModify,
                isDark = isDark,
                isApplying = isApplying
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 主内容
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
                    
                    ProParamsSectionEnhanced(
                        preset = preset,
                        isDark = isDark,
                        oldParams = if (applyState is ParamApplyState.Success) {
                            (applyState as ParamApplyState.Success).oldParams
                        } else null
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
            
            // 参数应用反馈卡片 - 底部悬浮
            ParamApplyFeedbackCard(
                state = applyState,
                onUndo = {
                    // 撤销：恢复旧参数
                    previousParams.forEach { (key, value) ->
                        AutoFillAccessibilityService.setParams(mapOf(key to value))
                    }
                    applyState = ParamApplyState.Idle
                },
                onDismiss = {
                    applyState = ParamApplyState.Idle
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 80.dp)
                    .fillMaxWidth()
            )
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

@Composable
private fun ProParamsSectionEnhanced(
    preset: Preset,
    isDark: Boolean,
    oldParams: Map<String, String>?
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
                // 参数网格显示
                ParamGridEnhanced(
                    params = params,
                    oldParams = oldParams,
                    isDark = isDark
                )
                
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
private fun ParamGridEnhanced(
    params: CameraParams,
    oldParams: Map<String, String>?,
    isDark: Boolean
) {
    val paramList = listOf(
        ("ISO" to params.iso.toString()),
        ("快门" to params.shutter),
        ("光圈" to params.aperture),
        ("焦距" to params.focalLength),
        ("白平衡" to params.wb),
        ("曝光补偿" to params.ev)
    )
    
    Column {
        // 第一行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            paramList.take(3).forEach { (label, value) ->
                ParamChipEnhanced(
                    label = label,
                    value = value,
                    oldValue = oldParams?.get(label),
                    isDark = isDark
                )
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.sm))
        
        // 第二行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            paramList.drop(3).forEach { (label, value) ->
                ParamChipEnhanced(
                    label = label,
                    value = value,
                    oldValue = oldParams?.get(label),
                    isDark = isDark
                )
            }
        }
        
        // HNCS 标签
        if (params.hasselblad_hncs) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Colors.HasselbladOrange.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Colors.HasselbladOrange,
                            modifier = Modifier.size(16.dp)
                        ) {
                            Text(
                                text = "H",
                                style = Typography.LabelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Colors.OnPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                        Text(
                            text = "HNCS",
                            style = Typography.LabelMedium,
                            fontWeight = FontWeight.Medium,
                            color = Colors.HasselbladOrange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParamChipEnhanced(
    label: String,
    value: String,
    oldValue: String?,
    isDark: Boolean
) {
    val hasChanged = oldValue != null && oldValue != value
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (hasChanged) {
            Colors.HasselbladOrange.copy(alpha = 0.15f)
        } else {
            Colors.GlassBackground.copy(alpha = 0.5f)
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = Typography.LabelSmall,
                color = if (hasChanged) Colors.HasselbladOrange else Colors.OnSurfaceVariant
            )
            
            if (hasChanged) {
                // 显示变化
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = oldValue ?: "-",
                        style = Typography.LabelSmall,
                        color = Colors.OnSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "变化",
                        tint = Colors.HasselbladOrange,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = value,
                        style = Typography.LabelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Colors.HasselbladOrange
                    )
                }
            } else {
                Text(
                    text = value,
                    style = Typography.TitleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Colors.HasselbladOrange else ColorOSLightAccent
                )
            }
        }
    }
}

@Composable
private fun ProDetailBottomBarEnhanced(
    onApplyPreset: () -> Unit,
    canModify: Boolean,
    isDark: Boolean,
    isApplying: Boolean
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
                    contentColor = Colors.OnPrimary,
                    disabledContainerColor = Colors.Disabled
                ),
                enabled = canModify && !isApplying
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Colors.OnPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "正在应用...",
                        style = Typography.TitleMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
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
            
            if (!canModify) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "此预设为哈苏官方认证，参数不可修改",
                    style = Typography.LabelSmall,
                    color = Colors.OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// 复用原有组件
@Composable
private fun ProDetailHeader(
    preset: Preset,
    isDark: Boolean,
    onPreviewClick: () -> Unit
) {
    var isPressed by remember(preset.id) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) ColorOSScale.Pressed else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "previewScale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onPreviewClick
            )
    ) {
        AsyncImage(
            model = preset.coverUrl.ifEmpty { "https://picsum.photos/seed/${preset.coverPath}/800/600" },
            contentDescription = preset.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
            error = painterResource(id = android.R.drawable.ic_menu_report_image)
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
    Column(modifier = Modifier.fillMaxWidth()) {
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
                        text = preset.author.takeIf { it.isNotEmpty() }?.first()?.toString()?.uppercase() ?: "",
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
        StatItem(icon = Icons.Default.Download, value = preset.getFormattedDownloadCount(), label = "下载量", isDark = isDark)
        StatItem(icon = Icons.Default.Favorite, value = preset.getFormattedFavoriteCount(), label = "收藏量", isDark = isDark)
        StatItem(icon = Icons.Default.Star, value = String.format("%.1f", preset.rating), label = "评分", isDark = isDark)
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    isDark: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = label, tint = Colors.HasselbladOrange, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = Typography.TitleLarge, fontWeight = FontWeight.Bold, color = if (isDark) Colors.OnSurface else ColorOSLightTextPrimary)
        Text(text = label, style = Typography.LabelSmall, color = if (isDark) Colors.OnSurfaceVariant else ColorOSLightTextTertiary)
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
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "返回", tint = if (isDark) Colors.OnSurface else ColorOSLightTextPrimary)
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
                Icon(imageVector = Icons.Default.Share, contentDescription = "分享", tint = if (isDark) Colors.OnSurface else ColorOSLightTextPrimary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
private fun ProHncsCertificationSection(certificationText: String, isDark: Boolean) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = {}) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Surface(shape = CircleShape, color = Colors.HasselbladOrange) {
                    Text(text = "H", color = Colors.OnPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
                }
                Text(text = "HNCS 认证", style = Typography.TitleMedium, fontWeight = FontWeight.Bold, color = Colors.HasselbladOrange)
            }
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(text = certificationText, style = Typography.BodyMedium, color = if (isDark) Colors.OnSurface else ColorOSLightTextSecondary)
        }
    }
}

@Composable
private fun ProDescriptionSection(preset: Preset, isDark: Boolean) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = {}) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
            Text(text = "预设介绍", style = Typography.TitleMedium, fontWeight = FontWeight.Bold, color = if (isDark) Colors.OnSurface else ColorOSLightTextPrimary)
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = preset.description.ifEmpty { "这是一款专业的摄影预设，为您的照片带来独特的视觉风格。" },
                style = Typography.BodyMedium,
                color = if (isDark) Colors.OnSurface else ColorOSLightTextSecondary
            )
        }
    }
}

@Composable
private fun ProVersionSection(version: String, publishDate: String, source: String, isOfficial: Boolean, isDark: Boolean) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = {}) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
            Text(text = "版本信息", style = Typography.TitleMedium, fontWeight = FontWeight.Bold, color = if (isDark) Colors.OnSurface else ColorOSLightTextPrimary)
            Spacer(modifier = Modifier.height(Spacing.md))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text(text = "版本", style = Typography.LabelSmall, color = Colors.OnSurfaceVariant); Text(text = version, style = Typography.BodyMedium, fontWeight = FontWeight.Medium, color = Colors.HasselbladOrange) }
                Column { Text(text = "发布日期", style = Typography.LabelSmall, color = Colors.OnSurfaceVariant); Text(text = publishDate, style = Typography.BodyMedium, fontWeight = FontWeight.Medium, color = Colors.HasselbladOrange) }
            }
        }
    }
}

@Composable
private fun ProComparisonSection(originalImageUrl: String, presetImageUrl: String, isDark: Boolean) {
    var sliderPosition by rememberSaveable { mutableFloatStateOf(0.5f) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "效果对比", style = Typography.TitleMedium, fontWeight = FontWeight.Bold, color = if (isDark) Colors.OnSurface else ColorOSLightTextPrimary)
        Spacer(modifier = Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(Radius.lg))) {
            AsyncImage(model = originalImageUrl.ifEmpty { "https://picsum.photos/seed/${originalImageUrl}_original/800/600" }, contentDescription = "原图", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxWidth(sliderPosition).fillMaxHeight().clip(RoundedCornerShape(Radius.lg))) {
                AsyncImage(model = presetImageUrl, contentDescription = "应用预设后", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Box(modifier = Modifier.size(40.dp).align(Alignment.CenterStart).offset(x = (sliderPosition * 300 - 20).dp).clip(CircleShape).background(Colors.HasselbladOrange).pointerInput(Unit) { detectHorizontalDragGestures { change, _ -> change.consume(); sliderPosition = (change.position.x / size.width).coerceIn(0f, 1f) } }, contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.Compare, contentDescription = "拖动对比", tint = Colors.OnPrimary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun FullScreenPreviewDialog(imageUrl: String, presetName: String, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) { detectTransformGestures { _, pan, zoom, _ -> scale = (scale * zoom).coerceIn(1f, 4f); if (scale > 1f) offset = Offset(offset.x + pan.x, offset.y + pan.y) else offset = Offset.Zero } }.pointerInput(Unit) { detectTapGestures(onDoubleTap = { if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f }, onTap = { onDismiss() }) }) {
        AsyncImage(model = imageUrl.ifEmpty { "https://picsum.photos/seed/${imageUrl}/1200/900" }, contentDescription = presetName, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y })
        Surface(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp), shape = CircleShape, color = Colors.GlassBackground.copy(alpha = 0.8f), onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "关闭", tint = Colors.OnSurface, modifier = Modifier.size(44.dp).padding(10.dp))
        }
    }
}