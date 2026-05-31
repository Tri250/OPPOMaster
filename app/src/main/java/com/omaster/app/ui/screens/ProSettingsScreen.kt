package com.omaster.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omaster.app.BuildConfig
import com.omaster.app.data.ThemeMode
import com.omaster.app.ui.animation.clickableWithColorOSFeedback
import com.omaster.app.ui.theme.*

/**
 * ==================== ProSettingsScreen - 专业设置页 ====================
 * ColorOS 16 专业设计规范
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProSettingsScreen(
    themeMode: Int,
    onThemeModeChange: (ThemeMode) -> Unit,
    fluidCloudEnabled: Boolean,
    onFluidCloudToggle: (Boolean) -> Unit,
    overlayEnabled: Boolean,
    onOverlayToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = themeMode == ThemeMode.DARK.value
    val scrollState = rememberScrollState()
    
    Scaffold(
        containerColor = if (isDark) ColorOSBlack else ColorOSLightBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
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
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary,
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
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // 外观设置分组
            ProSettingsGroup(
                title = "外观",
                icon = Icons.Default.Palette,
                isDark = isDark
            ) {
                ThemeModeSelection(
                    currentMode = themeMode,
                    onModeChange = onThemeModeChange,
                    isDark = isDark
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 功能设置分组
            ProSettingsGroup(
                title = "功能",
                icon = Icons.Default.Tune,
                isDark = isDark
            ) {
                ProSwitchItem(
                    title = "流体云胶囊",
                    description = "在侧边栏显示快速访问入口",
                    checked = fluidCloudEnabled,
                    onCheckedChange = onFluidCloudToggle,
                    isDark = isDark
                )
                Divider(
                    color = if (isDark) ColorOSBorder else ColorOSLightBorder,
                    thickness = 0.5.dp
                )
                ProSwitchItem(
                    title = "悬浮窗",
                    description = "显示实时相机参数悬浮窗",
                    checked = overlayEnabled,
                    onCheckedChange = onOverlayToggle,
                    isDark = isDark
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 关于分组
            ProSettingsGroup(
                title = "关于",
                icon = Icons.Default.Info,
                isDark = isDark
            ) {
                ProInfoItem(
                    title = "版本",
                    value = BuildConfig.VERSION_NAME,
                    isDark = isDark
                )
                Divider(
                    color = if (isDark) ColorOSBorder else ColorOSLightBorder,
                    thickness = 0.5.dp
                )
                ProInfoItem(
                    title = "开发者",
                    value = "小O帮帮",
                    isDark = isDark
                )
                Divider(
                    color = if (isDark) ColorOSBorder else ColorOSLightBorder,
                    thickness = 0.5.dp
                )
                ProInfoItem(
                    title = "开源协议",
                    value = "MIT License",
                    isDark = isDark
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 品牌展示区域 - 更新图片内容
            BrandSection(isDark = isDark)
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * ==================== BrandSection - 品牌展示区域 ====================
 * 专业设计 - 哈苏 × OPPO 联名
 */
@Composable
fun BrandSection(isDark: Boolean) {
    val pulseAnimation = rememberInfiniteTransition()
    val scale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = ColorOSEasing.Standard),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) {
                Brush.verticalGradient(
                    colors = listOf(
                        ColorOSCard,
                        ColorOSGlass
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        ColorOSLightCard,
                        ColorOSLightGlass
                    )
                )
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 哈苏LOGO - 使用渐变色
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                HasselbladOrange,
                                HasselbladOrangeLight,
                                HasselbladOrangeDark
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "小O",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    letterSpacing = (-1).sp
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 标题
            Text(
                text = "哈苏影像 × OPPO",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary,
                fontSize = 20.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 副标题
            Text(
                text = "为专业摄影而生",
                style = MaterialTheme.typography.bodyMedium,
                color = HasselbladOrange,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 分隔线
            Surface(
                modifier = Modifier
                    .width(80.dp)
                    .height(2.dp),
                color = HasselbladOrange.copy(alpha = 0.5f),
                shape = RoundedCornerShape(2.dp)
            ) {}
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 核心特性列表
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "HNCS 认证预设",
                    description = "哈苏自然色彩科学",
                    isDark = isDark
                )
                FeatureRow(
                    icon = Icons.Default.Camera,
                    title = "实时相机参数",
                    description = "专业摄影必备工具",
                    isDark = isDark
                )
                FeatureRow(
                    icon = Icons.Default.CloudSync,
                    title = "云端同步",
                    description = "预设数据安全存储",
                    isDark = isDark
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 底部标语
            Text(
                text = "追求完美，记录精彩",
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) ColorOSTextTertiary else ColorOSLightTextTertiary,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * ==================== FeatureRow - 特性展示行 ====================
 */
@Composable
fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isDark) ColorOSBorder.copy(alpha = 0.3f)
                else ColorOSLightBorder.copy(alpha = 0.3f)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(HasselbladOrange.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = HasselbladOrange,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary,
                fontSize = 15.sp
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) ColorOSTextTertiary else ColorOSLightTextTertiary,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * ==================== ProSettingsGroup - 专业设置分组 ====================
 */
@Composable
fun ProSettingsGroup(
    title: String,
    icon: ImageVector,
    isDark: Boolean,
    content: @Composable ColumnScope.() -> Unit
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HasselbladOrange.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = HasselbladOrange,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary,
                    fontSize = 18.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            content()
        }
    }
}

/**
 * ==================== ThemeModeSelection - 主题模式选择 ====================
 */
@Composable
fun ThemeModeSelection(
    currentMode: Int,
    onModeChange: (ThemeMode) -> Unit,
    isDark: Boolean
) {
    val modes = listOf(
        ThemeMode.SYSTEM to ThemeOption(
            icon = Icons.Default.AutoAwesome,
            title = "跟随系统",
            description = "自动切换浅色/深色主题"
        ),
        ThemeMode.LIGHT to ThemeOption(
            icon = Icons.Default.LightMode,
            title = "浅色模式",
            description = "明亮清爽的视觉体验"
        ),
        ThemeMode.DARK to ThemeOption(
            icon = Icons.Default.DarkMode,
            title = "深色模式",
            description = "专业摄影的沉浸式体验"
        )
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        modes.forEach { (mode, option) ->
            val isSelected = currentMode == mode.value
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.98f,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = 300f
                ),
                label = "scale"
            )
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale),
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) {
                    HasselbladOrange.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                },
                border = if (isSelected) BorderStroke(
                    1.5.dp,
                    HasselbladOrange.copy(alpha = 0.4f)
                ) else BorderStroke(
                    0.dp,
                    Color.Transparent
                ),
                onClick = { onModeChange(mode) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) HasselbladOrange.copy(alpha = 0.2f)
                        else Color.Transparent
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = option.title,
                            tint = if (isSelected) HasselbladOrange
                            else if (isDark) ColorOSTextSecondary else ColorOSLightTextSecondary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(14.dp))
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) HasselbladOrange
                            else if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary,
                            fontSize = 16.sp
                        )
                        Text(
                            text = option.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) ColorOSTextTertiary else ColorOSLightTextTertiary,
                            fontSize = 13.sp
                        )
                    }
                    
                    if (isSelected) {
                        Surface(
                            shape = CircleShape,
                            color = HasselbladOrange
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "已选择",
                                tint = Color.White,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ==================== ThemeOption - 主题选项数据类 ====================
 */
data class ThemeOption(
    val icon: ImageVector,
    val title: String,
    val description: String
)

/**
 * ==================== ProSwitchItem - 专业开关项 ====================
 */
@Composable
fun ProSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary,
                fontSize = 16.sp
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) ColorOSTextTertiary else ColorOSLightTextTertiary,
                fontSize = 13.sp
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
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

/**
 * ==================== ProInfoItem - 专业信息项 ====================
 */
@Composable
fun ProInfoItem(
    title: String,
    value: String,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = HasselbladOrange,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
    }
}
