package com.omaster.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omaster.app.ui.animation.clickableWithColorOSFeedback
import com.omaster.app.ui.theme.*
import com.omaster.app.watermark.WatermarkTemplate

/**
 * ==================== WatermarkEditorScreen - 水印编辑器 ====================
 * ColorOS 16 专业设计规范
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkEditorScreen(
    isDark: Boolean,
    onBack: () -> Unit,
    onTemplateSelected: (WatermarkTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTemplate by remember { mutableStateOf(WatermarkTemplate.HASSELBLAD) }
    var selectedPosition by remember { mutableStateOf(0) }
    var opacity by remember { mutableStateOf(0.8f) }
    var showPreview by remember { mutableStateOf(true) }
    
    val positions = listOf(
        "右下角" to Icons.Default.Badge,
        "左下角" to Icons.Default.Badge,
        "右上角" to Icons.Default.Badge,
        "左上角" to Icons.Default.Badge,
        "居中" to Icons.Default.CenterFocusStrong
    )
    
    val templates = listOf(
        WatermarkTemplate.HASSELBLAD to "哈苏",
        WatermarkTemplate.OPPO to "OPPO",
        WatermarkTemplate.ONEPLUS to "OnePlus",
        WatermarkTemplate.REALME to "realme",
        WatermarkTemplate.FILM_STYLE to "胶片风格",
        WatermarkTemplate.MINIMAL_PARAMS to "极简参数",
        WatermarkTemplate.TIMESTAMP to "时间戳",
        WatermarkTemplate.BRAND_SIMPLE to "简约品牌"
    )
    
    Scaffold(
        containerColor = if (isDark) ColorOSBlack else ColorOSLightBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "水印编辑器",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.clickableWithColorOSFeedback()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onTemplateSelected(selectedTemplate) },
                        modifier = Modifier.clickableWithColorOSFeedback()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "确认",
                            tint = HasselbladOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // 预览区域
            if (showPreview) {
                WatermarkPreviewCard(
                    selectedTemplate = selectedTemplate,
                    isDark = isDark
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 水印模板选择
            ProSettingsGroup(
                title = "水印模板",
                icon = Icons.Default.Brush,
                isDark = isDark
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(templates) { (template, name) ->
                        TemplateItem(
                            template = template,
                            name = name,
                            isSelected = selectedTemplate == template,
                            isDark = isDark,
                            onClick = { selectedTemplate = template }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 水印位置
            ProSettingsGroup(
                title = "水印位置",
                icon = Icons.Default.OpenWith,
                isDark = isDark
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    positions.forEachIndexed { index, (name, icon) ->
                        PositionItem(
                            name = name,
                            icon = icon,
                            isSelected = selectedPosition == index,
                            isDark = isDark,
                            onClick = { selectedPosition = index }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 透明度调节
            ProSettingsGroup(
                title = "水印设置",
                icon = Icons.Default.Settings,
                isDark = isDark
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 透明度滑块
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "透明度",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${(opacity * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HasselbladOrange,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = opacity,
                            onValueChange = { opacity = it },
                            valueRange = 0.3f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = HasselbladOrange,
                                activeTrackColor = HasselbladOrange,
                                inactiveTrackColor = if (isDark) ColorOSGrey700 else ColorOSGrey300
                            )
                        )
                    }
                    
                    // 预览开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "实时预览",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary,
                            fontSize = 16.sp
                        )
                        Switch(
                            checked = showPreview,
                            onCheckedChange = { showPreview = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = HasselbladOrange,
                                checkedBorderColor = Color.Transparent,
                                uncheckedThumbColor = if (isDark) ColorOSGrey400 else ColorOSGrey500,
                                uncheckedTrackColor = if (isDark) ColorOSGrey700 else ColorOSGrey300,
                                uncheckedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.scale(1.1f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 应用按钮
            Button(
                onClick = { onTemplateSelected(selectedTemplate) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HasselbladOrange
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isDark) ColorOSBlack else Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "应用水印",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDark) ColorOSBlack else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * ==================== TemplateItem - 水印模板项 ====================
 */
@Composable
fun TemplateItem(
    template: WatermarkTemplate,
    name: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickableWithColorOSFeedback(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) HasselbladOrange.copy(alpha = 0.2f) else (if (isDark) ColorOSCard else ColorOSLightCard),
            border = if (isSelected) BorderStroke(2.dp, HasselbladOrange) else null,
            modifier = Modifier.size(64.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (template) {
                        WatermarkTemplate.HASSELBLAD -> Icons.Default.PhotoCamera
                        WatermarkTemplate.OPPO -> Icons.Default.PhoneAndroid
                        WatermarkTemplate.ONEPLUS -> Icons.Default.PhoneAndroid
                        WatermarkTemplate.REALME -> Icons.Default.PhoneAndroid
                        WatermarkTemplate.FILM_STYLE -> Icons.Default.Movie
                        WatermarkTemplate.MINIMAL_PARAMS -> Icons.Default.Settings
                        WatermarkTemplate.TIMESTAMP -> Icons.Default.AccessTime
                        WatermarkTemplate.BRAND_SIMPLE -> Icons.Default.Badge
                        else -> Icons.Default.PhotoCamera
                    },
                    contentDescription = name,
                    tint = if (isSelected) HasselbladOrange else (if (isDark) ColorOSTextSecondary else ColorOSLightTextSecondary),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) HasselbladOrange else (if (isDark) ColorOSTextSecondary else ColorOSLightTextSecondary),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * ==================== PositionItem - 水印位置项 ====================
 */
@Composable
fun PositionItem(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickableWithColorOSFeedback(onClick = onClick)
            .weight(1f)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isSelected) HasselbladOrange.copy(alpha = 0.2f) else (if (isDark) ColorOSCard else ColorOSLightCard),
            border = if (isSelected) BorderStroke(1.5.dp, HasselbladOrange) else null,
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = if (isSelected) HasselbladOrange else (if (isDark) ColorOSTextTertiary else ColorOSLightTextTertiary),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) HasselbladOrange else (if (isDark) ColorOSTextTertiary else ColorOSLightTextTertiary),
            fontSize = 11.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * ==================== WatermarkPreviewCard - 水印预览卡片 ====================
 */
@Composable
fun WatermarkPreviewCard(
    selectedTemplate: WatermarkTemplate,
    isDark: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) ColorOSCard else ColorOSLightCard
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "水印预览",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // 模拟预览图
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) ColorOSGrey700 else ColorOSGrey300,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.padding(16.dp)
                ) {
                    // 水印预览
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = when (selectedTemplate) {
                                WatermarkTemplate.HASSELBLAD -> "HASSELBLAD"
                                WatermarkTemplate.OPPO -> "OPPO"
                                WatermarkTemplate.ONEPLUS -> "OnePlus"
                                WatermarkTemplate.REALME -> "realme"
                                WatermarkTemplate.FILM_STYLE -> "ISO 100 · f/1.8 · 1/1000s"
                                WatermarkTemplate.MINIMAL_PARAMS -> "f/1.8 · 1/1000s · ISO 100"
                                WatermarkTemplate.TIMESTAMP -> "2024-01-01 12:00"
                                WatermarkTemplate.BRAND_SIMPLE -> "Shot on OPPO"
                                else -> "Watermark"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
