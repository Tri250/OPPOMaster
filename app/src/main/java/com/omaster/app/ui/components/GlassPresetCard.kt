package com.omaster.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.omaster.app.model.Preset
import com.omaster.app.ui.animation.ColorOSAnimationDuration
import com.omaster.app.ui.animation.ColorOSEasing
import com.omaster.app.ui.animation.ColorOSScale
import com.omaster.app.ui.theme.*

@Composable
fun GlassPresetCard(
    preset: Preset,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
    isNew: Boolean = false,
    index: Int = 0
) {
    var isVisible by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> ColorOSScale.Pressed
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "scale"
    )
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 60L)
        isVisible = true
    }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            delayMillis = 0,
            easing = ColorOSEasing.Decelerate
        ),
        label = "alpha"
    )
    
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 40f,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            delayMillis = 0,
            easing = ColorOSEasing.Decelerate
        ),
        label = "offsetY"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffsetY
                scaleX = scale
                scaleY = scale
            }
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            selected = false
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(Radius.md))
                ) {
                    AsyncImage(
                        model = "https://picsum.photos/seed/${preset.coverPath}/600/400",
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
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                    
                    if (preset.cameraParams?.hasselblad_hncs == true) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(Spacing.sm),
                            color = Colors.HasselbladOrange,
                            shape = RoundedCornerShape(Radius.sm)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Colors.OnPrimary
                                )
                                Text(
                                    text = "HNCS",
                                    style = Typography.LabelSmall,
                                    color = Colors.OnPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    if (isNew) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(Spacing.sm),
                            color = Colors.AccentOrange,
                            shape = RoundedCornerShape(Radius.sm)
                        ) {
                            Text(
                                text = "NEW",
                                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                                style = Typography.LabelSmall,
                                color = Colors.OnPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    FavoriteButton(
                        isFavorite = preset.isFavorite,
                        onClick = onFavoriteToggle,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(Spacing.sm)
                    )
                }
                
                Spacer(modifier = Modifier.height(Spacing.md))
                
                Text(
                    text = preset.name,
                    style = Typography.TitleMedium,
                    color = Colors.OnSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                if (preset.deviceModel.isNotEmpty()) {
                    GlassChip(
                        text = preset.deviceModel,
                        selected = false,
                        onClick = {},
                        modifier = Modifier
                    )
                }

                preset.cameraParams?.let { params ->
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        if (params.hasselblad_hncs) {
                            CameraParamBadge(text = "HNCS", color = Colors.HasselbladOrange)
                        }
                        if (params.focalLength.isNotEmpty()) {
                            CameraParamBadge(text = params.focalLength, color = Colors.AccentBlue)
                        }
                        if (params.aperture.isNotEmpty()) {
                            CameraParamBadge(text = params.aperture, color = Colors.AccentGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isFavorite) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 400f
        ),
        label = "favoriteScale"
    )
    
    val heartColor by animateColorAsState(
        targetValue = if (isFavorite) Colors.AccentRed else Color.White,
        animationSpec = tween(200),
        label = "heartColor"
    )
    
    Box(
        modifier = modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavorite) "取消收藏" else "收藏",
            tint = heartColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CameraParamBadge(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(Radius.sm)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            style = Typography.LabelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun GlassPresetCardShimmer(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(Radius.Card))
            .background(Colors.GlassBackground)
            .border(
                width = 1.dp,
                color = Colors.GlassBorder,
                shape = RoundedCornerShape(Radius.Card)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.CardPadding)
        ) {
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(Radius.md))
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(Radius.xs))
            )
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(Radius.xs))
            )
        }
    }
}
