package com.omaster.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.omaster.app.model.Preset
import com.omaster.app.model.StyleType
import com.omaster.app.ui.theme.*

@Composable
fun PresetCard(
    preset: Preset,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_scale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 6.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_elevation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                onClick = onClick,
                indication = rememberRipple(bounded = true),
                interactionSource = remember { MutableInteractionSource() }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        shape = GlassCardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (isSystemInDarkTheme()) {
                        GlassSurfaceGradientDark
                    } else {
                        GlassSurfaceGradientLight
                    },
                    alpha = 0.85f
                )
                .blur(
                    radiusX = 0.5.dp,
                    radiusY = 0.5.dp,
                    edgeTreatment = BlurredEdgeTreatment.Unbounded
                )
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(PresetCoverShape)
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
                                        Color.Black.copy(alpha = 0.6f)
                                    ),
                                    startY = 50f
                                )
                            )
                    )

                    if (preset.isHncsCertified) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(14.dp),
                            color = HasselbladOrange.copy(alpha = 0.9f),
                            shape = HncsBadgeShape,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .drawBehind {
                                            drawCircle(
                                                color = HasselbladBlack,
                                                style = Stroke(width = 1.5.dp.toPx())
                                            )
                                        }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "HNCS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HasselbladBlack,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawCircle(
                                    color = if (preset.isHncsCertified) {
                                        HasselbladOrange.copy(alpha = glowAlpha)
                                    } else {
                                        LightFieldPrimary.copy(alpha = glowAlpha)
                                    },
                                    radius = size.minDimension * 0.8f,
                                    center = Offset(size.width * 0.8f, size.height * 0.2f)
                                )
                            }
                    )

                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .size(40.dp)
                            .background(
                                color = if (isSystemInDarkTheme()) {
                                    GlassMediumDark
                                } else {
                                    GlassMediumLight
                                },
                                shape = RoundedCornerShape(50)
                            )
                    ) {
                        Icon(
                            imageVector = if (preset.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (preset.isFavorite) "取消收藏" else "收藏",
                            tint = if (preset.isFavorite) {
                                LightFieldPrimary
                            } else if (isSystemInDarkTheme()) {
                                LightFieldOnSurfaceDark
                            } else {
                                LightFieldOnSurfaceLight
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = preset.stats.usageCount > 0,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp),
                            color = if (isSystemInDarkTheme()) {
                                GlassMediumDark
                            } else {
                                GlassMediumLight
                            },
                            shape = RoundedCornerShape(100)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${preset.stats.usageCount / 1000}K",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSystemInDarkTheme()) {
                                        LightFieldOnSurfaceVariantDark
                                    } else {
                                        LightFieldOnSurfaceVariantLight
                                    },
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "使用",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSystemInDarkTheme()) {
                                        LightFieldOnSurfaceVariantDark
                                    } else {
                                        LightFieldOnSurfaceVariantLight
                                    }
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isSystemInDarkTheme()) {
                            LightFieldOnSurfaceDark
                        } else {
                            LightFieldOnSurfaceLight
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = when (preset.styleType) {
                                StyleType.NATURAL -> LightFieldPrimary.copy(alpha = 0.15f)
                                StyleType.FILM -> HasselbladOrange.copy(alpha = 0.15f)
                                StyleType.CINEMATIC -> Info.copy(alpha = 0.15f)
                                StyleType.PORTRAIT -> Success.copy(alpha = 0.15f)
                                StyleType.LANDSCAPE -> OppoGreen.copy(alpha = 0.15f)
                                StyleType.NIGHT -> LightFieldBackgroundDark.copy(alpha = 0.5f)
                                else -> LightFieldSecondary.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(100)
                        ) {
                            Text(
                                text = preset.styleType.displayName,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = when (preset.styleType) {
                                    StyleType.NATURAL -> LightFieldPrimary
                                    StyleType.FILM -> HasselbladOrange
                                    StyleType.CINEMATIC -> Info
                                    StyleType.PORTRAIT -> Success
                                    StyleType.LANDSCAPE -> OppoGreen
                                    StyleType.NIGHT -> LightFieldOnSurfaceDark
                                    else -> LightFieldSecondary
                                },
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (preset.deviceModel.isNotEmpty()) {
                            Surface(
                                color = if (isSystemInDarkTheme()) {
                                    LightFieldSurfaceVariantDark
                                } else {
                                    LightFieldSurfaceVariantLight
                                },
                                shape = RoundedCornerShape(100)
                            ) {
                                Text(
                                    text = preset.deviceModel,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSystemInDarkTheme()) {
                                        LightFieldOnSurfaceVariantDark
                                    } else {
                                        LightFieldOnSurfaceVariantLight
                                    }
                                )
                            }
                        }

                        if (preset.stats.rating > 0) {
                            Surface(
                                color = Warning.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(100)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "★",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Warning,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = String.format("%.1f", preset.stats.rating),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Warning,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    if (preset.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSystemInDarkTheme()) {
                                LightFieldOnSurfaceVariantDark
                            } else {
                                LightFieldOnSurfaceVariantLight
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
