package com.omaster.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omaster.app.ui.components.*
import com.omaster.app.ui.theme.WebAnimations
import com.omaster.app.ui.theme.WebColors
import com.omaster.app.ui.theme.WebRadius
import com.omaster.app.ui.theme.WebSpacing

// ==================== Web风格首页 ====================

@Composable
fun WebHomeScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WebColors.BackgroundPrimary),
        contentPadding = PaddingValues(bottom = WebSpacing.xl3)
    ) {
        // Hero区域
        item {
            WebHeroSection()
        }

        // 功能展示
        item {
            WebFeaturesSection()
        }

        // 预设展示
        item {
            WebPresetsSection()
        }

        // AI功能展示
        item {
            WebAiFeatureSection()
        }

        // CTA区域
        item {
            WebCtaSection()
        }

        // 页脚
        item {
            WebFooter()
        }
    }
}

@Composable
fun WebHeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        WebColors.Zinc800,
                        WebColors.BackgroundPrimary
                    )
                )
            )
            .padding(WebSpacing.ContainerPadding)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题动画
            var titleVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { titleVisible = true }

            val titleAlpha by animateFloatAsState(
                targetValue = if (titleVisible) 1f else 0f,
                animationSpec = tween(600, easing = WebAnimations.EaseOut)
            )

            val titleOffset by animateFloatAsState(
                targetValue = if (titleVisible) 0f else 30f,
                animationSpec = tween(600, easing = WebAnimations.EaseOut)
            )

            Text(
                text = "OPPO Master",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = WebColors.TextPrimary
                ),
                modifier = Modifier
                    .alpha(titleAlpha)
                    .offset(y = titleOffset.dp)
            )

            Spacer(modifier = Modifier.height(WebSpacing.sm))

            Text(
                text = "专业摄影参数预设管理",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = WebColors.AccentPrimary
                ),
                modifier = Modifier
                    .alpha(titleAlpha)
                    .offset(y = titleOffset.dp)
            )

            Spacer(modifier = Modifier.height(WebSpacing.lg))

            Text(
                text = "哈苏HNCS认证 · 500+专业预设 · AI场景识别",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = WebColors.TextSecondary
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(titleAlpha)
                    .offset(y = titleOffset.dp)
            )

            Spacer(modifier = Modifier.height(WebSpacing.xl2))

            // 按钮组
            Row(
                horizontalArrangement = Arrangement.spacedBy(WebSpacing.base),
                modifier = Modifier
                    .alpha(titleAlpha)
                    .offset(y = titleOffset.dp)
            ) {
                WebButton(
                    text = "开始使用",
                    onClick = { },
                    icon = Icons.Default.PlayArrow
                )
                WebButton(
                    text = "了解更多",
                    onClick = { },
                    variant = WebButtonVariant.Ghost
                )
            }
        }

        // 装饰性渐变
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            WebColors.AccentPrimary.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        radius = 400f
                    )
                )
        )
    }
}

@Composable
fun WebFeaturesSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WebColors.BackgroundPrimary)
            .padding(vertical = WebSpacing.xl3)
    ) {
        WebSectionTitle(
            title = "核心功能",
            subtitle = "专业级摄影工具，为创作者打造极致拍摄体验",
            modifier = Modifier.padding(bottom = WebSpacing.xl2)
        )

        val features = listOf(
            Triple(
                Icons.Default.CameraAlt,
                "AI场景识别",
                "智能识别35+拍摄场景，自动推荐最佳参数配置"
            ),
            Triple(
                Icons.Default.Palette,
                "哈苏色彩科学",
                "官方HNCS认证，还原专业级哈苏色彩表现"
            ),
            Triple(
                Icons.Default.Folder,
                "预设管理",
                "500+专业预设，支持导入导出与云端同步"
            ),
            Triple(
                Icons.Default.Tune,
                "参数精细调节",
                "专业级参数控制，实时预览调节效果"
            ),
            Triple(
                Icons.Default.WaterDrop,
                "水印编辑器",
                "12+水印模板，支持品牌、功能、开源多种风格"
            ),
            Triple(
                Icons.Default.AutoAwesome,
                "智能优化",
                "AI一键优化，根据图片特征智能调整参数"
            )
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = WebSpacing.ContainerPadding),
            horizontalArrangement = Arrangement.spacedBy(WebSpacing.base),
            verticalArrangement = Arrangement.spacedBy(WebSpacing.base),
            modifier = Modifier.height(480.dp)
        ) {
            itemsIndexed(features) { index, feature ->
                WebFeatureCard(
                    icon = feature.first,
                    title = feature.second,
                    description = feature.third,
                    features = when (index) {
                        0 -> listOf("实时识别", "参数推荐", "场景优化")
                        1 -> listOf("HNCS 3.0", "自然色彩", "大师风格")
                        2 -> listOf("分类筛选", "收藏管理", "一键应用")
                        3 -> listOf("ISO/快门", "白平衡", "曝光补偿")
                        4 -> listOf("品牌水印", "版权保护", "自定义样式")
                        else -> listOf("自动优化", "风格迁移", "智能蒙版")
                    },
                    index = index
                )
            }
        }
    }
}

@Composable
fun WebPresetsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WebColors.BackgroundSecondary)
            .padding(vertical = WebSpacing.xl3)
    ) {
        WebSectionTitle(
            title = "精选预设",
            subtitle = "专业摄影师精心调校的参数预设，一键获得大片效果",
            modifier = Modifier.padding(bottom = WebSpacing.xl2)
        )

        val presets = listOf(
            PresetData(
                name = "哈苏大师人像",
                author = "摄影大师",
                deviceModel = "Find X8 Ultra",
                description = "经典哈苏人像风格，肤色自然，背景虚化柔和",
                rating = 4.9f,
                downloadCount = "12.5K",
                tags = listOf("人像", "哈苏", "专业"),
                iso = "100",
                shutter = "1/200",
                aperture = "f/1.8",
                isHncsCertified = true
            ),
            PresetData(
                name = "夜景大师",
                author = "夜景专家",
                deviceModel = "Find X8 Pro",
                description = "城市夜景专用，高光压制，暗部细节丰富",
                rating = 4.8f,
                downloadCount = "8.3K",
                tags = listOf("夜景", "城市", "长曝光"),
                iso = "800",
                shutter = "1/30",
                aperture = "f/2.8",
                isHncsCertified = true
            ),
            PresetData(
                name = "街拍模式",
                author = "街头摄影师",
                deviceModel = "Reno 12 Pro",
                description = "快速抓拍，色彩鲜明，对比度适中",
                rating = 4.7f,
                downloadCount = "6.1K",
                tags = listOf("街拍", "快拍", "日常"),
                iso = "200",
                shutter = "1/500",
                aperture = "f/4.0",
                isHncsCertified = false
            ),
            PresetData(
                name = "风景大片",
                author = "风光摄影师",
                deviceModel = "Find X8 Ultra",
                description = "广阔风景专用，HDR增强，色彩饱和",
                rating = 4.9f,
                downloadCount = "9.7K",
                tags = listOf("风景", "HDR", "广角"),
                iso = "100",
                shutter = "1/125",
                aperture = "f/8.0",
                isHncsCertified = true
            )
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = WebSpacing.ContainerPadding),
            horizontalArrangement = Arrangement.spacedBy(WebSpacing.base),
            verticalArrangement = Arrangement.spacedBy(WebSpacing.base),
            modifier = Modifier.height(600.dp)
        ) {
            itemsIndexed(presets) { index, preset ->
                WebPresetCard(
                    name = preset.name,
                    author = preset.author,
                    deviceModel = preset.deviceModel,
                    description = preset.description,
                    rating = preset.rating,
                    downloadCount = preset.downloadCount,
                    tags = preset.tags,
                    iso = preset.iso,
                    shutter = preset.shutter,
                    aperture = preset.aperture,
                    isHncsCertified = preset.isHncsCertified,
                    index = index,
                    onClick = { }
                )
            }
        }
    }
}

@Composable
fun WebAiFeatureSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WebColors.BackgroundSecondary)
            .padding(WebSpacing.ContainerPadding)
            .padding(vertical = WebSpacing.xl3),
        horizontalArrangement = Arrangement.spacedBy(WebSpacing.xl2)
    ) {
        // 左侧文字
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "AI场景识别",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = WebColors.TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(WebSpacing.base))

            Text(
                text = "基于深度学习的场景识别引擎，支持35+拍摄场景自动识别。智能推荐最佳参数配置，让每个人都能拍出专业级照片。",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = WebColors.TextSecondary,
                    lineHeight = WebSpacing.xl2.toSp()
                )
            )

            Spacer(modifier = Modifier.height(WebSpacing.lg))

            val features = listOf(
                Pair("人像场景", "智能美颜、肤色优化、背景虚化"),
                Pair("夜景场景", "降噪增强、高光抑制、暗部提亮"),
                Pair("美食场景", "色彩增强、饱和度优化、暖色调"),
                Pair("风景场景", "HDR增强、广角优化、动态范围")
            )

            features.forEach { (label, desc) ->
                Row(
                    modifier = Modifier.padding(vertical = WebSpacing.sm),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(WebColors.AccentPrimary)
                            .padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(WebSpacing.base))
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = WebColors.TextPrimary
                            )
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = WebColors.TextTertiary
                            )
                        )
                    }
                }
            }
        }

        // 右侧展示
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(WebRadius.xl2))
                .background(
                    Brush.linearGradient(
                        listOf(WebColors.Zinc700, WebColors.Zinc800)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = WebColors.AccentPrimary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(WebSpacing.base))
                Text(
                    text = "35+",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = WebColors.TextPrimary
                    )
                )
                Text(
                    text = "支持场景类型",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = WebColors.TextSecondary
                    )
                )
            }

            // 装饰光效
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                WebColors.AccentPrimary.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun WebCtaSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WebColors.BackgroundPrimary)
            .padding(vertical = WebSpacing.xl3)
            .padding(horizontal = WebSpacing.ContainerPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(600, easing = WebAnimations.EaseOut)
        )

        val offsetY by animateFloatAsState(
            targetValue = if (visible) 0f else 20f,
            animationSpec = tween(600, easing = WebAnimations.EaseOut)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alpha)
                .offset(y = offsetY.dp)
        ) {
            Text(
                text = "开始你的专业摄影之旅",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = WebColors.TextPrimary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(WebSpacing.base))

            Text(
                text = "下载OMaster，体验哈苏色彩科学的魅力",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = WebColors.TextSecondary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(WebSpacing.xl))

            Row(
                horizontalArrangement = Arrangement.spacedBy(WebSpacing.base)
            ) {
                WebButton(
                    text = "下载 Android 版",
                    onClick = { },
                    icon = Icons.Default.Download
                )
                WebButton(
                    text = "GitHub",
                    onClick = { },
                    variant = WebButtonVariant.Ghost
                )
            }
        }
    }
}

@Composable
fun WebFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WebColors.BackgroundPrimary)
            .padding(WebSpacing.ContainerPadding)
            .padding(vertical = WebSpacing.xl2)
    ) {
        HorizontalDivider(
            color = WebColors.Zinc800,
            thickness = 1.dp,
            modifier = Modifier.padding(bottom = WebSpacing.lg)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "© 2025 OMaster. 专业摄影参数预设管理应用",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = WebColors.TextTertiary
                )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(WebSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "哈苏 HNCS 官方认证",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = WebColors.TextTertiary
                    )
                )
                Text(
                    text = "·",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = WebColors.TextTertiary
                    )
                )
                Text(
                    text = "OPPO Find 系列",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = WebColors.TextTertiary
                    )
                )
            }
        }
    }
}

// 数据类
data class PresetData(
    val name: String,
    val author: String,
    val deviceModel: String,
    val description: String,
    val rating: Float,
    val downloadCount: String,
    val tags: List<String>,
    val iso: String,
    val shutter: String,
    val aperture: String,
    val isHncsCertified: Boolean
)

// 扩展函数
private fun Dp.toSp() = this.value.sp
