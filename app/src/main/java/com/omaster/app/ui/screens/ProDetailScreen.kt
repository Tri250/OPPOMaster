package com.omaster.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.ui.animation.ColorOSAnimationDuration
import com.omaster.app.ui.animation.ColorOSEasing
import com.omaster.app.ui.animation.ColorOSScale
import com.omaster.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProDetailScreen(
    preset: Preset,
    onBack: () -> Unit,
    onApply: () -> Unit,
    onShare: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = Colors.Background,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            ProDetailHeader(
                preset = preset,
                onBack = onBack,
                onFavoriteToggle = onFavoriteToggle
            )

            ProDetailContent(
                preset = preset,
                onApply = onApply,
                onShare = onShare
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))
        }
    }
}

@Composable
private fun ProDetailHeader(
    preset: Preset,
    onBack: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        AsyncImage(
            model = preset.coverUrl.takeIf { it.isNotEmpty() }
                ?: "https://picsum.photos/seed/${preset.coverPath}/800/600",
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
                            Colors.Background.copy(alpha = 0.6f),
                            Color.Transparent,
                            Colors.Background.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBack,
                contentDescription = "返回",
                size = 44.dp
            )

            GlassIconButton(
                icon = if (preset.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                onClick = onFavoriteToggle,
                contentDescription = if (preset.isFavorite) "取消收藏" else "收藏",
                size = 44.dp
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.lg)
        ) {
            if (preset.isHncsCertified) {
                Surface(
                    shape = RoundedCornerShape(Radius.sm),
                    color = Colors.HasselbladOrange.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "哈苏 HNCS 认证",
                        style = Typography.LabelSmall,
                        color = Colors.HasselbladOrange,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            Text(
                text = preset.name,
                style = Typography.HeadlineMedium,
                fontWeight = FontWeight.Bold,
                color = Colors.OnBackground
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = preset.getDeviceDisplay(),
                style = Typography.BodyLarge,
                color = Colors.OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProDetailContent(
    preset: Preset,
    onApply: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = Spacing.ScreenPadding)
    ) {
        ProDetailActions(
            onApply = onApply,
            onShare = onShare
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        preset.cameraParams?.let { params ->
            ProCameraParamsCard(params = params)

            Spacer(modifier = Modifier.height(Spacing.lg))
        }

        ProDescriptionCard(preset = preset)

        Spacer(modifier = Modifier.height(Spacing.lg))

        ProTagsCard(preset = preset)
    }
}

@Composable
private fun ProDetailActions(
    onApply: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        GlassButton(
            text = "应用预设",
            onClick = onApply,
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Check
        )

        GlassButton(
            text = "分享",
            onClick = onShare,
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Share
        )
    }
}

@Composable
private fun ProCameraParamsCard(params: CameraParams) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        isVisible = true
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Decelerate
        ),
        label = "paramsAlpha"
    )

    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 30f,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Decelerate
        ),
        label = "paramsOffsetY"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffsetY
            }
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = {}
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "相机参数",
                    style = Typography.TitleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Colors.OnSurface
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                ProParamGrid(params = params)
            }
        }
    }
}

@Composable
private fun ProParamGrid(params: CameraParams) {
    val paramItems = buildList {
        if (params.iso > 0) add("ISO" to params.iso.toString())
        if (params.shutter.isNotEmpty()) add("快门" to params.shutter)
        if (params.ev.isNotEmpty()) add("曝光补偿" to params.ev)
        if (params.wb.isNotEmpty()) add("白平衡" to params.wb)
        if (params.focalLength.isNotEmpty()) add("焦距" to params.focalLength)
        if (params.aperture.isNotEmpty()) add("光圈" to params.aperture)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        paramItems.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                rowItems.forEach { (label, value) ->
                    ProParamItem(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProParamItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) ColorOSScale.Pressed else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "paramScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(Radius.md))
            .background(Colors.GlassBackground.copy(alpha = 0.2f))
            .border(
                width = 1.dp,
                color = Colors.GlassBorder.copy(alpha = 0.3f),
                shape = RoundedCornerShape(Radius.md)
            )
            .padding(Spacing.md)
    ) {
        Column {
            Text(
                text = label,
                style = Typography.LabelSmall,
                color = Colors.OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = value,
                style = Typography.TitleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Colors.OnSurface
            )
        }
    }
}

@Composable
private fun ProDescriptionCard(preset: Preset) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        isVisible = true
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Decelerate
        ),
        label = "descAlpha"
    )

    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 30f,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Decelerate
        ),
        label = "descOffsetY"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffsetY
            }
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = {}
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "预设说明",
                    style = Typography.TitleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Colors.OnSurface
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                Text(
                    text = preset.description.ifEmpty { "暂无说明" },
                    style = Typography.BodyMedium,
                    color = Colors.OnSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xl)
                ) {
                    ProMetric(
                        value = preset.getFormattedDownloadCount(),
                        label = "下载量"
                    )
                    ProMetric(
                        value = preset.getFormattedFavoriteCount(),
                        label = "收藏量"
                    )
                    ProMetric(
                        value = "${preset.rating}",
                        label = "评分"
                    )
                }
            }
        }
    }
}

@Composable
private fun ProTagsCard(preset: Preset) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        isVisible = true
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Decelerate
        ),
        label = "tagsAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = animatedAlpha }
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = {}
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "标签",
                    style = Typography.TitleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Colors.OnSurface
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    preset.tags.forEach { tag ->
                        ProTagChip(tag = tag)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProTagChip(tag: String) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) ColorOSScale.Pressed else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "tagScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(Radius.sm))
            .background(Colors.GlassBackground.copy(alpha = 0.2f))
            .border(
                width = 1.dp,
                color = Colors.GlassBorder.copy(alpha = 0.3f),
                shape = RoundedCornerShape(Radius.sm)
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    ) {
        Text(
            text = tag,
            style = Typography.LabelMedium,
            color = Colors.OnSurfaceVariant
        )
    }
}

@Composable
private fun ProMetric(
    value: String,
    label: String
) {
    Column {
        Text(
            text = value,
            style = Typography.TitleLarge,
            fontWeight = FontWeight.Bold,
            color = Colors.HasselbladOrange
        )
        Text(
            text = label,
            style = Typography.LabelSmall,
            color = Colors.OnSurfaceVariant
        )
    }
}
