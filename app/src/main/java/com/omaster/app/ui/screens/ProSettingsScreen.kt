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
 * ==================== AboutMeSection - 关于我展示区域 ====================
 * 专业设计 - 个人开发者介绍
 */
@Composable
fun BrandSection(isDark: Boolean) {
    val pulseAnimation = rememberInfiniteTransition()
    val scale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = ColorOSEasing.Standard),
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
            // 个人头像LOGO
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                HasselbladOrange,
                                HasselbladOrangeLight,
                                DeepOceanBlue
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📷",
                        fontSize = 36.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 标题
            Text(
                text = "小O帮帮",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary,
                fontSize = 24.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 副标题
            Text(
                text = "热爱摄影的开发者",
                style = MaterialTheme.typography.bodyMedium,
                color = HasselbladOrange,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 分隔线
            Surface(
                modifier = Modifier
                    .width(100.dp)
                    .height(2.dp),
                color = HasselbladOrange.copy(alpha = 0.5f),
                shape = RoundedCornerShape(2.dp)
            ) {}
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 介绍文字
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) {
                        ColorOSBorder.copy(alpha = 0.2f)
                    } else {
                        ColorOSLightBorder.copy(alpha = 0.2f)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "关于我",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary,
                        fontSize = 17.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "你好！我是\"带娃的小陈工\"，一名热爱摄影的开发者。小O帮帮诞生于对完美摄影体验的追求。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) ColorOSTextSecondary else ColorOSLightTextSecondary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "我相信，每一次按下快门都值得被认真对待。从一键闪记到流体云胶囊，从HNCS认证预设到AI智能推荐，每一个功能都凝聚了我对\"专业却简单\"这一理念的坚持。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) ColorOSTextSecondary else ColorOSLightTextSecondary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "希望小O帮帮能帮助你拍出更美的照片！",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HasselbladOrange,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 联系方式
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) {
                        HasselbladOrange.copy(alpha = 0.1f)
                    } else {
                        HasselbladOrange.copy(alpha = 0.08f)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "联系我",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HasselbladOrange,
                        fontSize = 17.sp
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = "有任何问题或建议？抖音、小红书搜索\"带娃的小陈工\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) ColorOSTextSecondary else ColorOSLightTextSecondary,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 底部标语
            Text(
                text = "用影像记录生活的美好",
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) ColorOSTextTertiary else ColorOSLightTextTertiary,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
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
