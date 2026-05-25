package com.omaster.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.omaster.app.model.*
import com.omaster.app.ui.theme.*

@Composable
fun DetailScreen(
    preset: Preset,
    onBack: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onApplyPreset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var isApplying by remember { mutableStateOf(false) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "detail_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    Surface(
                        modifier = Modifier.padding(8.dp),
                        color = if (isSystemInDarkTheme()) {
                            GlassMediumDark
                        } else {
                            GlassMediumLight
                        },
                        shape = RoundedCornerShape(50)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = if (isSystemInDarkTheme()) {
                                    LightFieldOnSurfaceDark
                                } else {
                                    LightFieldOnSurfaceLight
                                }
                            )
                        }
                    }
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
                        shape = RoundedCornerShape(50)
                    ) {
                        IconButton(onClick = onFavoriteToggle) {
                            Icon(
                                imageVector = if (preset.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (preset.isFavorite) "取消收藏" else "收藏",
                                tint = if (preset.isFavorite) {
                                    LightFieldPrimary
                                } else if (isSystemInDarkTheme()) {
                                    LightFieldOnSurfaceDark
                                } else {
                                    LightFieldOnSurfaceLight
                                }
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        color = if (isSystemInDarkTheme()) {
                            GlassMediumDark
                        } else {
                            GlassMediumLight
                        },
                        shape = RoundedCornerShape(50)
                    ) {
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "分享",
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
                            color = if (preset.isHncsCertified) {
                                HasselbladOrange.copy(alpha = glowAlpha * 0.5f)
                            } else {
                                LightFieldPrimary.copy(alpha = glowAlpha * 0.5f)
                            },
                            radius = size.maxDimension * 0.8f,
                            center = Offset(size.width * 0.5f, size.height * 0.1f)
                        )
                    }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    AsyncImage(
                        model = "https://picsum.photos/seed/${preset.coverPath}/1200/800",
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
                                        if (isSystemInDarkTheme()) {
                                            LightFieldBackgroundDark
                                        } else {
                                            LightFieldBackgroundLight
                                        }
                                    ),
                                    startY = 200f
                                )
                            )
                    )

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
                                    radius = size.minDimension * 1.2f,
                                    center = Offset(size.width * 0.7f, size.height * 0.3f)
                                )
                            }
                    )

                    if (preset.isHncsCertified) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(18.dp),
                            color = HasselbladOrange.copy(alpha = 0.95f),
                            shape = HncsBadgeShape,
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .drawBehind {
                                            drawCircle(
                                                color = HasselbladBlack,
                                                style = Stroke(width = 2.dp.toPx())
                                            )
                                        }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "HNCS 认证",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HasselbladBlack,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.displaySmall,
                        color = if (isSystemInDarkTheme()) {
                            LightFieldOnSurfaceDark
                        } else {
                            LightFieldOnSurfaceLight
                        },
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSystemInDarkTheme()) {
                                        LightFieldOnSurfaceVariantDark
                                    } else {
                                        LightFieldOnSurfaceVariantLight
                                    }
                                )
                            }
                        }
                    }

                    if (preset.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSystemInDarkTheme()) {
                                LightFieldOnSurfaceVariantDark
                            } else {
                                LightFieldOnSurfaceVariantLight
                            }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("使用次数", "${preset.stats.usageCount / 1000}K")
                        StatItem("收藏", "${preset.stats.favoriteCount}")
                        StatItem("评分", String.format("%.1f", preset.stats.rating))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (preset.cameraParams != null) {
                        SectionHeader("相机参数")
                        Spacer(modifier = Modifier.height(12.dp))
                        CameraParamsCard(preset.cameraParams)
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    SectionHeader("精细调节")
                    Spacer(modifier = Modifier.height(12.dp))
                    FineTuneParamsCard(preset.fineTuneParams)
                    Spacer(modifier = Modifier.height(20.dp))

                    if (preset.sceneTags.isNotEmpty()) {
                        SectionHeader("适用场景")
                        Spacer(modifier = Modifier.height(12.dp))
                        SceneTagsRow(preset.sceneTags)
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    if (preset.usageTips.isNotEmpty()) {
                        SectionHeader("使用提示")
                        Spacer(modifier = Modifier.height(12.dp))
                        UsageTipsCard(preset.usageTips)
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    if (preset.sections.isNotEmpty()) {
                        SectionHeader("详细说明")
                        Spacer(modifier = Modifier.height(12.dp))
                        preset.sections.forEach { section ->
                            SectionItem(section)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    Button(
                        onClick = {
                            isApplying = true
                            onApplyPreset()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (preset.isHncsCertified) {
                                HasselbladOrange
                            } else {
                                LightFieldPrimary
                            }
                        ),
                        shape = ButtonShape,
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = HasselbladBlack,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "正在应用...",
                                style = MaterialTheme.typography.titleLarge,
                                color = HasselbladBlack,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "一键应用到相机",
                                style = MaterialTheme.typography.titleLarge,
                                color = HasselbladBlack,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = if (isSystemInDarkTheme()) {
            LightFieldOnSurfaceDark
        } else {
            LightFieldOnSurfaceLight
        },
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = LightFieldPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSystemInDarkTheme()) {
                LightFieldOnSurfaceVariantDark
            } else {
                LightFieldOnSurfaceVariantLight
            }
        )
    }
}

@Composable
fun CameraParamsCard(params: CameraParams) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = GlassCardShape,
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
                    alpha = 0.7f
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ParamItem("ISO", params.iso.toString())
                ParamItem("快门", params.shutter)
                ParamItem("曝光补偿", params.ev)
                ParamItem("白平衡", params.wb)
                if (params.filter.isNotEmpty()) {
                    ParamItem("滤镜", params.filter)
                }
                if (params.hasselblad_hncs) {
                    ParamItem("HNCS", "已启用")
                }
            }
        }
    }
}

@Composable
fun FineTuneParamsCard(params: FineTuneParams) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = GlassCardShape,
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
                    alpha = 0.7f
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FineTuneSliderItem("锐度", params.sharpness, -20, 40)
                FineTuneSliderItem("对比度", params.contrast, -20, 40)
                FineTuneSliderItem("饱和度", params.saturation, -20, 20)
                FineTuneSliderItem("色温", params.colorTemperature, -20, 20)
                FineTuneSliderItem("色调", params.tint, -20, 20)
                ParamItem("暗角", when (params.vignette) {
                    0 -> "关闭"
                    1 -> "低"
                    2 -> "中"
                    3 -> "高"
                    else -> "中"
                })
                ParamItem("柔光", when (params.softness) {
                    0 -> "关闭"
                    1 -> "低"
                    2 -> "中"
                    3 -> "高"
                    else -> "中"
                })
                ParamItem("高光保护", "${params.highlightProtection}")
                ParamItem("阴影提升", "${params.shadowLift}")
                ParamItem("降噪强度", when (params.noiseReduction) {
                    0 -> "关闭"
                    1 -> "低"
                    2 -> "中"
                    3 -> "高"
                    4 -> "极高"
                    5 -> "最大"
                    else -> "中"
                })
                ParamItem("肤色优化", if (params.skinToneOptimization) "已启用" else "关闭")
            }
        }
    }
}

@Composable
fun FineTuneSliderItem(label: String, value: Int, min: Int, max: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSystemInDarkTheme()) {
                LightFieldOnSurfaceVariantDark
            } else {
                LightFieldOnSurfaceVariantLight
            }
        )
        Surface(
            color = LightFieldPrimary.copy(alpha = 0.15f),
            shape = RoundedCornerShape(100)
        ) {
            Text(
                text = if (value > 0) "+$value" else value.toString(),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleMedium,
                color = LightFieldPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ParamItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSystemInDarkTheme()) {
                LightFieldOnSurfaceVariantDark
            } else {
                LightFieldOnSurfaceVariantLight
            }
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = LightFieldPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SceneTagsRow(tags: List<SceneTag>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = GlassCardShape,
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
                    alpha = 0.7f
                )
                .padding(16.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    Surface(
                        color = OppoGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(100)
                    ) {
                        Text(
                            text = tag.displayName,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = OppoGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UsageTipsCard(tips: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = GlassCardShape,
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
                    alpha = 0.7f
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                tips.forEach { tip ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = LightFieldPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(50)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.titleMedium,
                                color = LightFieldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSystemInDarkTheme()) {
                                LightFieldOnSurfaceVariantDark
                            } else {
                                LightFieldOnSurfaceVariantLight
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionItem(section: Section) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = GlassCardShape,
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
                    alpha = 0.7f
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = LightFieldPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = section.content,
                    style = MaterialTheme.typography.bodyMedium,
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
