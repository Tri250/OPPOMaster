package com.omaster.app.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omaster.app.data.ThemeMode
import com.omaster.app.ui.animation.clickableWithColorOSFeedback
import com.omaster.app.ui.theme.*

/**
 * ==================== ProSettingsScreen - 专业设置页 ====================
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
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary
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
                            tint = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary
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
            Spacer(modifier = Modifier.height(16.dp))
            
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
            
            Spacer(modifier = Modifier.height(20.dp))
            
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
                    thickness = 1.dp
                )
                ProSwitchItem(
                    title = "悬浮窗",
                    description = "显示实时相机参数悬浮窗",
                    checked = overlayEnabled,
                    onCheckedChange = onOverlayToggle,
                    isDark = isDark
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 关于分组
            ProSettingsGroup(
                title = "关于",
                icon = Icons.Default.Info,
                isDark = isDark
            ) {
                ProInfoItem(
                    title = "版本",
                    value = "1.0.0",
                    isDark = isDark
                )
                Divider(
                    color = if (isDark) ColorOSBorder else ColorOSLightBorder,
                    thickness = 1.dp
                )
                ProInfoItem(
                    title = "开发者",
                    value = "哈苏影像实验室",
                    isDark = isDark
                )
                Divider(
                    color = if (isDark) ColorOSBorder else ColorOSLightBorder,
                    thickness = 1.dp
                )
                ProInfoItem(
                    title = "开源协议",
                    value = "MIT License",
                    isDark = isDark
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 底部信息
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = HasselbladOrange.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "H",
                            color = HasselbladOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "哈苏影像 × OPPO",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) ColorOSTextSecondary else ColorOSLightTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "为专业摄影而生",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) ColorOSTextTertiary else ColorOSLightTextTertiary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * ==================== ProSettingsGroup - 专业设置分组 ====================
 */
@Composable
fun ProSettingsGroup(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = HasselbladOrange.copy(alpha = 0.15f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = HasselbladOrange,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        modes.forEach { (mode, option) ->
            val isSelected = currentMode == mode.value
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) {
                    HasselbladOrange.copy(alpha = 0.15f)
                } else {
                    Color.Transparent
                },
                onClick = { onModeChange(mode) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
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
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) HasselbladOrange
                            else if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary
                        )
                        Text(
                            text = option.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) ColorOSTextTertiary else ColorOSLightTextTertiary
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
                                tint = Color.Black,
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
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) ColorOSTextTertiary else ColorOSLightTextTertiary
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = HasselbladOrange,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary,
                uncheckedTrackColor = if (isDark) ColorOSGrey700 else ColorOSGrey300,
                uncheckedBorderColor = Color.Transparent
            )
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
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) ColorOSTextPrimary else ColorOSLightTextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDark) ColorOSTextSecondary else ColorOSLightTextSecondary
        )
    }
}
