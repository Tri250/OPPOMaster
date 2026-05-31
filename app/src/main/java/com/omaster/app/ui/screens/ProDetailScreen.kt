package com.omaster.app.ui.screens

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.omaster.app.data.ThemeMode
import com.omaster.app.model.Preset
import com.omaster.app.ui.animation.clickableWithColorOSFeedback
import com.omaster.app.ui.theme.*

/**
 * ==================== ProDetailScreen - 专业预设详情页 ====================
 */
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
    
    Scaffold(
        containerColor = if (isDark) ColorOSBlack else ColorOSLightBackground,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.clickableWithColorOSFeedback()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.clickableWithColorOSFeedback()
                    ) {
                        Icon(
                            imageVector = if (preset.isFavorite) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                            contentDescription = if (preset.isFavorite) "取消收藏" else "收藏",
                            tint = if (preset.isFavorite) {
                                HasselbladOrange
                            } else {
                                if (isDark) ColorOSTextSecondary else ColorOSLightTextSecondary
                            }
                        )
                    }
                    IconButton(
                        onClick = { /* TODO: 分享 */ },
                        modifier = Modifier.clickableWithColorOSFeedback()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "分享",
                            tint = if (isDark) ColorOSTextSecondary else ColorOSLightTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            ProBottomBar(
                onApplyPreset = onApplyPreset,
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
            // 大图展示区域
            ProDetailHeader(
                preset = preset,
                isDark = isDark
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 内容区域
            Column(
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                // 标题区域
                ProDetailTitle(
                    preset = preset,
                    isDark = isDark
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 相机参数区域
                ProParamsSection(
                    preset = preset,
                    isDark = isDark
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 作者/说明区域
                ProDescriptionSection(
                    preset = preset,
                    isDark = isDark
                )
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

/**
 * ==================== ProDetailHeader - 详情页头部 ====================
 */
@Composable
fun ProDetailHeader(
    preset: Preset,
    isDark: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        AsyncImage(
            model = preset.coverUrl,
            contentDescription = preset.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // 渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            if (isDark) ColorOSBlack else ColorOSLightBackground,
                            Color.Transparent,
                            Color.Transparent,
                            if (isDark) ColorOSBlack else ColorOSLightBackground
                        ),
                        startY = 0f,
                        endY = 400f
                    )
                )
        )
        
        // HNCS 标志
        if (preset.hasselbladHncs) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp),
                shape = RoundedCornerShape(12.dp),
                color = HasselbladOrange.copy(alpha = 0.9f)
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
                            color = HasselbladOrange,
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
    }
}

/**
 * ==================== ProDetailTitle - 详情页标题 ====================
 */
@Composable
fun ProDetailTitle(
    preset: Preset,
    isDark: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = preset.name,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary
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
                    color = if (isDark) ColorOSGrey700 else ColorOSGrey200
                ) {
                    Text(
                        text = preset.author.first().toString().uppercase(),
                        color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                
                Column {
                    Text(
                        text = preset.author,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary
                    )
                    Text(
                        text = preset.device,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) ColorOSTextTertiary else ColorOSLightTextTertiary
                    )
                }
            }
            
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = when (preset.sceneType) {
                    "portrait" -> SunsetRed.copy(alpha = 0.15f)
                    "landscape" -> AuroraGreen.copy(alpha = 0.15f)
                    "night" -> DeepOceanBlue.copy(alpha = 0.15f)
                    "sunset" -> HasselbladOrange.copy(alpha = 0.15f)
                    "food" -> CosmicPurple.copy(alpha = 0.15f)
                    else -> ColorOSGrey400.copy(alpha = 0.15f)
                }
            ) {
                Text(
                    text = when (preset.sceneType) {
                        "portrait" -> "人像摄影"
                        "landscape" -> "风景摄影"
                        "night" -> "夜景摄影"
                        "sunset" -> "日落摄影"
                        "food" -> "美食摄影"
                        else -> "通用摄影"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = when (preset.sceneType) {
                        "portrait" -> SunsetRed
                        "landscape" -> AuroraGreen
                        "night" -> DeepOceanBlue
                        "sunset" -> HasselbladOrange
                        "food" -> CosmicPurple
                        else -> if (isDark) ColorOSTextTertiary else ColorOSLightTextTertiary
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * ==================== ProParamsSection - 专业参数区域 ====================
 */
@Composable
fun ProParamsSection(
    preset: Preset,
    isDark: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) ColorOSCard else ColorOSLightCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "相机参数",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            GridParamsList(
                preset = preset,
                isDark = isDark
            )
        }
    }
}

/**
 * ==================== GridParamsList - 网格参数列表 ====================
 */
@Composable
fun GridParamsList(
    preset: Preset,
    isDark: Boolean
) {
    val params = listOf(
        ProParamItem("ISO", preset.cameraParams.iso.toString(), ColorISO),
        ProParamItem("快门速度", preset.cameraParams.shutterSpeed, ColorShutter),
        ProParamItem("曝光补偿", if (preset.cameraParams.ev >= 0) "+${preset.cameraParams.ev}" else preset.cameraParams.ev.toString(), ColorEV),
        ProParamItem("白平衡", preset.cameraParams.whiteBalance, ColorWB),
        ProParamItem("光圈", preset.cameraParams.aperture, DeepOceanBlue),
        ProParamItem("焦距", preset.cameraParams.focalLength, AuroraGreen)
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        params.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { param ->
                    ProParamCard(
                        param = param,
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * ==================== ProParamItem - 专业参数项数据类 ====================
 */
data class ProParamItem(
    val label: String,
    val value: String,
    val color: Color
)

/**
 * ==================== ProParamCard - 专业参数卡片 ====================
 */
@Composable
fun ProParamCard(
    param: ProParamItem,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) ColorOSBlackElevated else ColorOSLightSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = param.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) ColorOSTextTertiary else ColorOSLightTextTertiary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = param.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = param.color
            )
        }
    }
}

/**
 * ==================== ProDescriptionSection - 说明区域 ====================
 */
@Composable
fun ProDescriptionSection(
    preset: Preset,
    isDark: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) ColorOSCard else ColorOSLightCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "使用说明",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "${preset.name} 是由 ${preset.author} 为 ${preset.device} 设备精心调校的专业摄影预设。" +
                        "该预设特别适合在 ${preset.sceneType} 场景下使用，能够帮助您快速获得专业级别的摄影效果。" +
                        "建议在光线良好的情况下使用，并可根据实际拍摄环境微调参数。",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) ColorOSTextSecondary else ColorOSLightTextSecondary,
                lineHeight = 24.sp
            )
        }
    }
}

/**
 * ==================== ProBottomBar - 专业底部操作栏 ====================
 */
@Composable
fun ProBottomBar(
    onApplyPreset: () -> Unit,
    isDark: Boolean
) {
    Surface(
        color = if (isDark) ColorOSBlack else ColorOSLightBackground,
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
                    containerColor = HasselbladOrange,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "应用预设",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
