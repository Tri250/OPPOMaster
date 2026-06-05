package com.omaster.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omaster.app.BuildConfig
import com.omaster.app.data.ThemeMode
import com.omaster.app.ui.animation.ColorOSAnimationDuration
import com.omaster.app.ui.animation.ColorOSEasing
import com.omaster.app.ui.animation.ColorOSScale
import com.omaster.app.ui.components.*
import com.omaster.app.ui.theme.*

@Composable
fun ProSettingsScreenV2(
    themeMode: Int,
    onThemeModeChange: (ThemeMode) -> Unit,
    fluidCloudEnabled: Boolean,
    onFluidCloudToggle: (Boolean) -> Unit,
    overlayEnabled: Boolean,
    onOverlayToggle: (Boolean) -> Unit,
    syncEnabled: Boolean,
    onSyncToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Scaffold(
        containerColor = Colors.Background,
        topBar = {
            GlassTopAppBar(
                title = "设置",
                onBackClick = onBack
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
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            ProSettingsGroupV2(
                title = "外观",
                icon = Icons.Default.Palette
            ) {
                ThemeModeSelectionV2(
                    currentMode = themeMode,
                    onModeChange = onThemeModeChange
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            ProSettingsGroupV2(
                title = "功能",
                icon = Icons.Default.Tune
            ) {
                ProSwitchItemV2(
                    title = "网络同步",
                    description = "自动同步预设和数据",
                    checked = syncEnabled,
                    onCheckedChange = onSyncToggle
                )
                
                GlassDivider()
                
                ProSwitchItemV2(
                    title = "流体云胶囊",
                    description = "在侧边栏显示快速访问入口",
                    checked = fluidCloudEnabled,
                    onCheckedChange = onFluidCloudToggle
                )
                
                GlassDivider()
                
                ProSwitchItemV2(
                    title = "悬浮窗",
                    description = "显示实时相机参数悬浮窗",
                    checked = overlayEnabled,
                    onCheckedChange = onOverlayToggle
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            ProSettingsGroupV2(
                title = "关于",
                icon = Icons.Default.Info
            ) {
                ProInfoItemV2(
                    title = "版本",
                    value = BuildConfig.VERSION_NAME
                )
                
                GlassDivider()
                
                ProInfoItemV2(
                    title = "开发者",
                    value = "小O帮帮"
                )
                
                GlassDivider()
                
                ProInfoItemV2(
                    title = "开源协议",
                    value = "MIT License"
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            BrandSectionV2()
            
            Spacer(modifier = Modifier.height(Spacing.xxl))
        }
    }
}

@Composable
private fun GlassTopAppBar(
    title: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(
            icon = Icons.Default.ArrowBack,
            onClick = onBackClick,
            contentDescription = "返回",
            size = 44.dp
        )
        
        Spacer(modifier = Modifier.width(Spacing.md))
        
        Text(
            text = title,
            style = Typography.HeadlineMedium,
            fontWeight = FontWeight.Bold,
            color = Colors.OnBackground
        )
    }
}

@Composable
private fun ProSettingsGroupV2(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
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
        label = "groupAlpha"
    )
    
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 30f,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Decelerate
        ),
        label = "groupOffsetY"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ScreenPadding)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(Radius.md))
                            .background(Colors.HasselbladOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Colors.HasselbladOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Text(
                        text = title,
                        style = Typography.TitleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Colors.OnSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(Spacing.lg))
                
                content()
            }
        }
    }
}

@Composable
private fun ThemeModeSelectionV2(
    currentMode: Int,
    onModeChange: (ThemeMode) -> Unit
) {
    val modes = listOf(
        ThemeMode.SYSTEM to Triple(Icons.Default.AutoAwesome, "跟随系统", "自动切换浅色/深色主题"),
        ThemeMode.LIGHT to Triple(Icons.Default.LightMode, "浅色模式", "明亮清爽的视觉体验"),
        ThemeMode.DARK to Triple(Icons.Default.DarkMode, "深色模式", "专业摄影的沉浸式体验")
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        modes.forEach { (mode, config) ->
            val (icon, title, description) = config
            val isSelected = currentMode == mode.value
            
            ThemeModeItemV2(
                icon = icon,
                title = title,
                description = description,
                isSelected = isSelected,
                onClick = { onModeChange(mode) }
            )
        }
    }
}

@Composable
private fun ThemeModeItemV2(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) ColorOSScale.Pressed else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "themeScale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            Colors.HasselbladOrange.copy(alpha = 0.12f)
        } else {
            Colors.GlassBackground.copy(alpha = 0.1f)
        },
        animationSpec = tween(200),
        label = "themeBackground"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(Radius.md))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(Spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(
                        if (isSelected) {
                            Colors.HasselbladOrange.copy(alpha = 0.2f)
                        } else {
                            Colors.GlassBackground.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected) Colors.HasselbladOrange else Colors.OnSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = Typography.TitleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Colors.HasselbladOrange else Colors.OnSurface
                )
                Text(
                    text = description,
                    style = Typography.BodySmall,
                    color = Colors.OnSurfaceVariant
                )
            }
            
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Colors.HasselbladOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选择",
                        tint = Colors.OnPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProSwitchItemV2(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = Typography.TitleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Colors.OnSurface
            )
            Text(
                text = description,
                style = Typography.BodySmall,
                color = Colors.OnSurfaceVariant
            )
        }
        
        GlassSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) {
            Colors.HasselbladOrange
        } else {
            Colors.GlassBackground.copy(alpha = 0.3f)
        },
        animationSpec = tween(200),
        label = "trackColor"
    )
    
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 20f else 0f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "thumbOffset"
    )
    
    Box(
        modifier = Modifier
            .width(52.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(trackColor)
            .clickable { onCheckedChange(!checked) }
            .padding(Spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .offset(x = thumbOffset.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Colors.HasselbladOrange,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ProInfoItemV2(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = Typography.TitleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Colors.OnSurface,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = value,
            style = Typography.BodyMedium,
            color = Colors.HasselbladOrange,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BrandSectionV2() {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        isVisible = true
    }
    
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = ColorOSEasing.Standard),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.SLOW,
            easing = ColorOSEasing.Decelerate
        ),
        label = "brandAlpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ScreenPadding)
            .graphicsLayer { alpha = animatedAlpha }
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .scale(pulseScale),
            onClick = {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Colors.HasselbladOrange,
                                    Colors.HasselbladGold,
                                    Colors.AccentBlue
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
                
                Spacer(modifier = Modifier.height(Spacing.lg))
                
                Text(
                    text = "小O帮帮",
                    style = Typography.HeadlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Colors.OnSurface
                )
                
                Text(
                    text = "热爱摄影的开发者",
                    style = Typography.BodyLarge,
                    color = Colors.HasselbladOrange,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(Spacing.md))
                
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(2.dp)
                        .background(Colors.HasselbladOrange.copy(alpha = 0.5f))
                )
                
                Spacer(modifier = Modifier.height(Spacing.lg))
                
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {}
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg)
                    ) {
                        Text(
                            text = "关于我",
                            style = Typography.TitleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Colors.OnSurface
                        )
                        
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        
                        Text(
                            text = "你好！我是\"带娃的小陈工\"，一名热爱摄影的开发者。小O帮帮诞生于对完美摄影体验的追求。",
                            style = Typography.BodyMedium,
                            color = Colors.OnSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        
                        Text(
                            text = "我相信，每一次按下快门都值得被认真对待。从一键闪记到流体云胶囊，从HNCS认证预设到AI智能推荐，每一个功能都凝聚了我对\"专业却简单\"这一理念的坚持。",
                            style = Typography.BodyMedium,
                            color = Colors.OnSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        
                        Text(
                            text = "希望小O帮帮能帮助你拍出更美的照片！",
                            style = Typography.BodyMedium,
                            color = Colors.HasselbladOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.lg))
                
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {}
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "联系我",
                            style = Typography.TitleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Colors.HasselbladOrange
                        )
                        
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        
                        Text(
                            text = "有任何问题或建议？抖音、小红书搜索\"带娃的小陈工\"",
                            style = Typography.BodyMedium,
                            color = Colors.OnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.lg))
                
                Text(
                    text = "用影像记录生活的美好",
                    style = Typography.BodySmall,
                    color = Colors.OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
