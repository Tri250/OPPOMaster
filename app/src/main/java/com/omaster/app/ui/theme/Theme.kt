package com.omaster.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.omaster.app.data.ThemeMode

// ==================== Web Design System 配色方案 ====================
// 基于Web设计系统的现代化深色配色
val WebDarkColorScheme = darkColorScheme(
    primary = WebColors.AccentPrimary,
    onPrimary = Color.White,
    primaryContainer = WebColors.Orange600,
    onPrimaryContainer = Color.White,
    secondary = WebColors.Zinc600,
    onSecondary = Color.White,
    secondaryContainer = WebColors.Zinc700,
    onSecondaryContainer = Color.White,
    tertiary = WebColors.Orange300,
    onTertiary = WebColors.Zinc900,
    tertiaryContainer = WebColors.Orange200,
    onTertiaryContainer = WebColors.Zinc900,
    background = WebColors.BackgroundPrimary,
    onBackground = WebColors.TextPrimary,
    surface = WebColors.SurfaceSolid,
    onSurface = WebColors.TextPrimary,
    surfaceVariant = WebColors.Zinc800,
    onSurfaceVariant = WebColors.TextSecondary,
    outline = WebColors.Outline,
    outlineVariant = WebColors.OutlineVariant,
    error = WebColors.Error,
    onError = Color.White,
    errorContainer = WebColors.Error.copy(alpha = 0.15f),
    onErrorContainer = Color.White
)

// ==================== Web Design System 浅色配色方案 ====================
val WebLightColorScheme = lightColorScheme(
    primary = WebColors.Orange600,
    onPrimary = Color.White,
    primaryContainer = WebColors.Orange100,
    onPrimaryContainer = WebColors.Zinc900,
    secondary = WebColors.Zinc500,
    onSecondary = Color.White,
    secondaryContainer = WebColors.Zinc100,
    onSecondaryContainer = WebColors.Zinc900,
    tertiary = WebColors.Orange400,
    onTertiary = WebColors.Zinc900,
    tertiaryContainer = WebColors.Orange50,
    onTertiaryContainer = WebColors.Zinc900,
    background = WebColors.Zinc50,
    onBackground = WebColors.Zinc900,
    surface = Color.White,
    onSurface = WebColors.Zinc900,
    surfaceVariant = WebColors.Zinc100,
    onSurfaceVariant = WebColors.Zinc600,
    outline = WebColors.Zinc300,
    outlineVariant = WebColors.Zinc200,
    error = WebColors.Error,
    onError = Color.White,
    errorContainer = WebColors.Error.copy(alpha = 0.15f),
    onErrorContainer = Color.White
)

// ==================== ColorOS 16 专业摄影深色配色方案 (Legacy) ====================
val ColorOSDarkColorScheme = darkColorScheme(
    primary = HasselbladOrange,
    onPrimary = ColorOSBlack,
    primaryContainer = HasselbladOrangeDark,
    onPrimaryContainer = ColorOSTextPrimary,
    secondary = DeepOceanBlue,
    onSecondary = Color.White,
    secondaryContainer = DeepOceanBlueDark,
    onSecondaryContainer = ColorOSTextPrimary,
    tertiary = OppoGold,
    onTertiary = ColorOSBlack,
    tertiaryContainer = OppoGoldDark,
    onTertiaryContainer = ColorOSTextPrimary,
    background = ColorOSBlack,
    onBackground = ColorOSTextPrimary,
    surface = ColorOSCard,
    onSurface = ColorOSTextPrimary,
    surfaceVariant = ColorOSBlackElevated,
    onSurfaceVariant = ColorOSTextSecondary,
    outline = ColorOSBorder,
    outlineVariant = ColorOSBorderLight,
    error = ErrorPro,
    onError = Color.White,
    errorContainer = ErrorPro.copy(alpha = 0.15f),
    onErrorContainer = ColorOSTextPrimary
)

// ==================== ColorOS 16 专业摄影浅色配色方案 (Legacy) ====================
val ColorOSLightColorScheme = lightColorScheme(
    primary = HasselbladOrange,
    onPrimary = Color.White,
    primaryContainer = HasselbladOrangeLight,
    onPrimaryContainer = ColorOSLightTextPrimary,
    secondary = DeepOceanBlue,
    onSecondary = Color.White,
    secondaryContainer = DeepOceanBlueLight,
    onSecondaryContainer = ColorOSLightTextPrimary,
    tertiary = OppoGold,
    onTertiary = Color.White,
    tertiaryContainer = OppoGoldLight,
    onTertiaryContainer = ColorOSLightTextPrimary,
    background = ColorOSLightBackground,
    onBackground = ColorOSLightTextPrimary,
    surface = ColorOSLightSurface,
    onSurface = ColorOSLightTextPrimary,
    surfaceVariant = ColorOSLightCard,
    onSurfaceVariant = ColorOSLightTextSecondary,
    outline = ColorOSLightBorder,
    outlineVariant = ColorOSLightBorderLight,
    error = ErrorPro,
    onError = Color.White,
    errorContainer = ErrorPro.copy(alpha = 0.15f),
    onErrorContainer = ColorOSLightTextPrimary
)

@Composable
fun OMasterTheme(
    themeMode: Int = ThemeMode.SYSTEM.value,
    dynamicColor: Boolean = false,
    useWebDesign: Boolean = true,  // 新增参数：是否使用Web设计系统
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT.value -> false
        ThemeMode.DARK.value -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        useWebDesign -> if (darkTheme) WebDarkColorScheme else WebLightColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ColorOSDarkColorScheme
        else -> ColorOSLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = if (useWebDesign) WebDesignTypography else ColorOSTypography,
        shapes = if (useWebDesign) WebDesignShapes else ColorOSShapes,
        content = content
    )
}

// ==================== Web Design Typography ====================
val WebDesignTypography = androidx.compose.material3.Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontSize = WebTypography.xl5,
        lineHeight = WebTypography.LeadingLoose,
        letterSpacing = WebTypography.TrackingTight
    ),
    displayMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontSize = WebTypography.xl4,
        lineHeight = WebTypography.LeadingRelaxed,
        letterSpacing = (-0.4).sp
    ),
    displaySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        fontSize = WebTypography.xl3,
        lineHeight = WebSpacing.xl3.toSp(),
        letterSpacing = (-0.2).sp
    ),
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontSize = WebTypography.xl2,
        lineHeight = WebSpacing.xl2.toSp(),
        letterSpacing = 0.sp
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        fontSize = WebTypography.xl,
        lineHeight = WebSpacing.xl.toSp(),
        letterSpacing = 0.sp
    ),
    headlineSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        fontSize = WebTypography.lg,
        lineHeight = WebSpacing.lg.toSp(),
        letterSpacing = 0.sp
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        fontSize = WebTypography.base,
        lineHeight = WebSpacing.base.toSp(),
        letterSpacing = 0.sp
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = WebTypography.sm,
        lineHeight = WebTypography.LeadingTight,
        letterSpacing = 0.1.sp
    ),
    titleSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = WebTypography.xs,
        lineHeight = WebSpacing.sm.toSp(),
        letterSpacing = 0.2.sp
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = WebTypography.base,
        lineHeight = WebTypography.LeadingSnug,
        letterSpacing = 0.sp
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = WebTypography.sm,
        lineHeight = WebTypography.LeadingNormal,
        letterSpacing = 0.sp
    ),
    bodySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = WebTypography.xs,
        lineHeight = WebTypography.LeadingTight,
        letterSpacing = 0.sp
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        fontSize = WebTypography.sm,
        lineHeight = WebSpacing.base.toSp(),
        letterSpacing = 0.1.sp
    ),
    labelMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = WebTypography.xs,
        lineHeight = WebSpacing.sm.toSp(),
        letterSpacing = 0.2.sp
    ),
    labelSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = WebSpacing.xs.toSp(),
        letterSpacing = 0.3.sp
    )
)

// ==================== Web Design Shapes ====================
val WebDesignShapes = androidx.compose.material3.Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(WebRadius.sm),
    small = androidx.compose.foundation.shape.RoundedCornerShape(WebRadius.md),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(WebRadius.lg),
    large = androidx.compose.foundation.shape.RoundedCornerShape(WebRadius.xl),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(WebRadius.xl3)
)

// 辅助函数
private fun androidx.compose.ui.unit.Dp.toSp() = this.value.sp
