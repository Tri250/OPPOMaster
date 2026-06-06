package com.omaster.app.presentation.components

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * 毛玻璃主题配置数据类
 * 包含毛玻璃效果所需的所有配置参数
 *
 * @param backgroundColor 背景颜色
 * @param surfaceColor 表面颜色
 * @param blurRadius 模糊半径
 * @param transparency 透明度
 * @param borderWidth 边框宽度
 * @param borderColor 边框颜色
 * @param cornerRadius 圆角半径
 * @param isDarkTheme 是否为深色主题
 */
data class GlassmorphismConfig(
    val backgroundColor: Color = Color.White.copy(alpha = 0.15f),
    val surfaceColor: Color = Color.White.copy(alpha = 0.2f),
    val blurRadius: Dp = 20.dp,
    val transparency: Float = 0.15f,
    val borderWidth: Dp = 1.dp,
    val borderColor: Color = Color.White.copy(alpha = 0.3f),
    val cornerRadius: Dp = 16.dp,
    val isDarkTheme: Boolean = false
) {
    companion object {
        /**
         * 浅色主题默认配置
         */
        fun lightDefault() = GlassmorphismConfig(
            backgroundColor = Color.White.copy(alpha = 0.7f),
            surfaceColor = Color.White.copy(alpha = 0.5f),
            blurRadius = 24.dp,
            transparency = 0.7f,
            borderWidth = 1.dp,
            borderColor = Color.White.copy(alpha = 0.6f),
            cornerRadius = 20.dp,
            isDarkTheme = false
        )

        /**
         * 深色主题默认配置
         */
        fun darkDefault() = GlassmorphismConfig(
            backgroundColor = Color(0xFF2A2A2A).copy(alpha = 0.6f),
            surfaceColor = Color(0xFF1A1A1A).copy(alpha = 0.4f),
            blurRadius = 24.dp,
            transparency = 0.6f,
            borderWidth = 1.dp,
            borderColor = Color.White.copy(alpha = 0.2f),
            cornerRadius = 20.dp,
            isDarkTheme = true
        )

        /**
         * 高透明度配置（更透明）
         */
        fun highTransparency(isDark: Boolean = false) = if (isDark) {
            darkDefault().copy(
                transparency = 0.3f,
                backgroundColor = Color(0xFF2A2A2A).copy(alpha = 0.3f),
                surfaceColor = Color(0xFF1A1A1A).copy(alpha = 0.2f)
            )
        } else {
            lightDefault().copy(
                transparency = 0.4f,
                backgroundColor = Color.White.copy(alpha = 0.4f),
                surfaceColor = Color.White.copy(alpha = 0.3f)
            )
        }

        /**
         * 低透明度配置（更不透明）
         */
        fun lowTransparency(isDark: Boolean = false) = if (isDark) {
            darkDefault().copy(
                transparency = 0.85f,
                backgroundColor = Color(0xFF2A2A2A).copy(alpha = 0.85f),
                surfaceColor = Color(0xFF1A1A1A).copy(alpha = 0.7f)
            )
        } else {
            lightDefault().copy(
                transparency = 0.9f,
                backgroundColor = Color.White.copy(alpha = 0.9f),
                surfaceColor = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * 毛玻璃模糊半径常量定义
 */
object GlassmorphismBlurRadius {
    /** 轻微模糊 */
    val LIGHT = 8.dp

    /** 中等模糊 */
    val MEDIUM = 16.dp

    /** 标准模糊 */
    val STANDARD = 24.dp

    /** 强烈模糊 */
    val HEAVY = 40.dp

    /** 最大模糊 */
    val MAXIMUM = 60.dp
}

/**
 * 毛玻璃透明度常量定义
 */
object GlassmorphismTransparency {
    /** 几乎透明 */
    const val MINIMAL = 0.1f

    /** 轻微透明 */
    const val LIGHT = 0.25f

    /** 中等透明 */
    const val MEDIUM = 0.5f

    /** 标准透明 */
    const val STANDARD = 0.7f

    /** 几乎不透明 */
    const val HEAVY = 0.9f
}

/**
 * 毛玻璃背景颜色定义
 */
object GlassmorphismColors {
    /** 浅色主题 - 主背景 */
    val LightBackground = Color(0xFFFFFFFF).copy(alpha = 0.7f)

    /** 浅色主题 - 表面背景 */
    val LightSurface = Color(0xFFFFFFFF).copy(alpha = 0.5f)

    /** 浅色主题 - 边框 */
    val LightBorder = Color(0xFFFFFFFF).copy(alpha = 0.6f)

    /** 深色主题 - 主背景 */
    val DarkBackground = Color(0xFF2A2A2A).copy(alpha = 0.6f)

    /** 深色主题 - 表面背景 */
    val DarkSurface = Color(0xFF1A1A1A).copy(alpha = 0.4f)

    /** 深色主题 - 边框 */
    val DarkBorder = Color(0xFFFFFFFF).copy(alpha = 0.2f)

    /** 强调色 - 蓝色 */
    val AccentBlue = Color(0xFF2196F3).copy(alpha = 0.3f)

    /** 强调色 - 紫色 */
    val AccentPurple = Color(0xFF9C27B0).copy(alpha = 0.3f)

    /** 强调色 - 绿色 */
    val AccentGreen = Color(0xFF4CAF50).copy(alpha = 0.3f)

    /** 强调色 - 橙色 */
    val AccentOrange = Color(0xFFFF9800).copy(alpha = 0.3f)
}

/**
 * 本地毛玻璃配置
 * 用于在 Compose 树中传递毛玻璃配置
 */
val LocalGlassmorphismConfig = compositionLocalOf {
    GlassmorphismConfig.lightDefault()
}

/**
 * 毛玻璃主题管理器
 * 用于管理主题切换和配置更新
 */
object GlassmorphismThemeManager {
    private var _currentConfig by mutableStateOf(GlassmorphismConfig.lightDefault())

    /** 当前毛玻璃配置 */
    val currentConfig: GlassmorphismConfig
        get() = _currentConfig

    /**
     * 更新配置
     */
    fun updateConfig(config: GlassmorphismConfig) {
        _currentConfig = config
    }

    /**
     * 切换到浅色主题
     */
    fun switchToLightTheme() {
        _currentConfig = GlassmorphismConfig.lightDefault()
    }

    /**
     * 切换到深色主题
     */
    fun switchToDarkTheme() {
        _currentConfig = GlassmorphismConfig.darkDefault()
    }

    /**
     * 切换主题
     */
    fun toggleTheme() {
        _currentConfig = if (_currentConfig.isDarkTheme) {
            GlassmorphismConfig.lightDefault()
        } else {
            GlassmorphismConfig.darkDefault()
        }
    }
}

/**
 * 毛玻璃主题
 * 提供完整的 Material Design 3 主题支持，集成毛玻璃效果
 *
 * @param darkTheme 是否使用深色主题
 * @param dynamicColor 是否使用动态颜色（Android 12+）
 * @param glassmorphismConfig 毛玻璃配置
 * @param content 主题内容
 */
@Composable
fun GlassmorphismTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    glassmorphismConfig: GlassmorphismConfig = if (darkTheme) {
        GlassmorphismConfig.darkDefault()
    } else {
        GlassmorphismConfig.lightDefault()
    },
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorSchemeWithGlassmorphism()
        else -> lightColorSchemeWithGlassmorphism()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalGlassmorphismConfig provides glassmorphismConfig
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

/**
 * 带毛玻璃效果的浅色配色方案
 */
private fun lightColorSchemeWithGlassmorphism(): ColorScheme {
    return lightColorScheme(
        primary = Color(0xFF2196F3),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFBBDEFB),
        onPrimaryContainer = Color(0xFF1565C0),
        secondary = Color(0xFF9C27B0),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE1BEE7),
        onSecondaryContainer = Color(0xFF7B1FA2),
        tertiary = Color(0xFF4CAF50),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFC8E6C9),
        onTertiaryContainer = Color(0xFF388E3C),
        background = Color(0xFFF5F5F5),
        onBackground = Color(0xFF212121),
        surface = GlassmorphismColors.LightSurface,
        onSurface = Color(0xFF212121),
        surfaceVariant = Color(0xFFE0E0E0),
        onSurfaceVariant = Color(0xFF616161),
        outline = GlassmorphismColors.LightBorder
    )
}

/**
 * 带毛玻璃效果的深色配色方案
 */
private fun darkColorSchemeWithGlassmorphism(): ColorScheme {
    return darkColorScheme(
        primary = Color(0xFF90CAF9),
        onPrimary = Color(0xFF0D47A1),
        primaryContainer = Color(0xFF1565C0),
        onPrimaryContainer = Color(0xFFBBDEFB),
        secondary = Color(0xFFCE93D8),
        onSecondary = Color(0xFF4A148C),
        secondaryContainer = Color(0xFF7B1FA2),
        onSecondaryContainer = Color(0xFFE1BEE7),
        tertiary = Color(0xFFA5D6A7),
        onTertiary = Color(0xFF1B5E20),
        tertiaryContainer = Color(0xFF388E3C),
        onTertiaryContainer = Color(0xFFC8E6C9),
        background = Color(0xFF121212),
        onBackground = Color(0xFFE0E0E0),
        surface = GlassmorphismColors.DarkSurface,
        onSurface = Color(0xFFE0E0E0),
        surfaceVariant = Color(0xFF424242),
        onSurfaceVariant = Color(0xFFBDBDBD),
        outline = GlassmorphismColors.DarkBorder
    )
}

/**
 * 获取当前毛玻璃配置
 */
@Composable
fun currentGlassmorphismConfig(): GlassmorphismConfig {
    return LocalGlassmorphismConfig.current
}

/**
 * 毛玻璃背景容器
 * 为整个页面提供毛玻璃背景效果
 *
 * @param modifier 修饰符
 * @param backgroundColor 背景颜色
 * @param content 内容
 */
@Composable
fun GlassmorphismBackground(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable () -> Unit
) {
    val config = currentGlassmorphismConfig()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        content()
    }
}

/**
 * 主题切换按钮组件（示例）
 * 展示如何使用主题管理器
 */
@Composable
fun ThemeToggleExample() {
    val config = currentGlassmorphismConfig()
    var isDark by remember { mutableStateOf(config.isDarkTheme) }

    // 使用 GlassCard 创建带毛玻璃效果的切换按钮
    GlassCard(
        transparency = if (isDark) 0.6f else 0.7f,
        cornerRadius = 12.dp
    ) {
        // 切换主题逻辑
        // GlassmorphismThemeManager.toggleTheme()
    }
}
