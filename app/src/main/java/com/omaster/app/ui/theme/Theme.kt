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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.omaster.app.data.ThemeMode

// ==================== OPPO 2026 深色配色方案 ====================
private val OppoDarkColorScheme = darkColorScheme(
    primary = OppoSunriseGold,
    onPrimary = OppoDeepSpace,
    primaryContainer = OppoSunriseGoldDark,
    onPrimaryContainer = OppoTextPrimary,
    secondary = OceanBlue,
    onSecondary = Color.White,
    secondaryContainer = OceanBlueDark,
    onSecondaryContainer = OppoTextPrimary,
    tertiary = HasselbladOrangePro,
    onTertiary = OppoDeepSpace,
    tertiaryContainer = HasselbladDarkPro,
    onTertiaryContainer = OppoTextPrimary,
    background = OppoDeepSpace,
    onBackground = OppoTextPrimary,
    surface = OppoCardSurface,
    onSurface = OppoTextPrimary,
    surfaceVariant = OppoElevated,
    onSurfaceVariant = OppoTextSecondary,
    outline = OppoBorder,
    outlineVariant = OppoBorderLight,
    error = ErrorVital,
    onError = Color.White,
    errorContainer = ErrorVital.copy(alpha = 0.15f),
    onErrorContainer = OppoTextPrimary
)

// ==================== OPPO 2026 浅色配色方案 ====================
private val OppoLightColorScheme = lightColorScheme(
    primary = OppoSunriseGold,
    onPrimary = Color.White,
    primaryContainer = OppoSunriseGoldLight,
    onPrimaryContainer = OppoLightTextPrimary,
    secondary = OceanBlue,
    onSecondary = Color.White,
    secondaryContainer = OceanBlueLight,
    onSecondaryContainer = OppoLightTextPrimary,
    tertiary = HasselbladOrangePro,
    onTertiary = Color.White,
    tertiaryContainer = HasselbladOrangeVibrant,
    onTertiaryContainer = OppoLightTextPrimary,
    background = OppoLightBackground,
    onBackground = OppoLightTextPrimary,
    surface = OppoLightSurface,
    onSurface = OppoLightTextPrimary,
    surfaceVariant = OppoLightElevated,
    onSurfaceVariant = OppoLightTextSecondary,
    outline = OppoLightBorder,
    outlineVariant = OppoLightBorderLight,
    error = ErrorVital,
    onError = Color.White,
    errorContainer = ErrorVital.copy(alpha = 0.15f),
    onErrorContainer = OppoLightTextPrimary
)

@Composable
fun OMasterTheme(
    themeMode: Int = ThemeMode.SYSTEM.value,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT.value -> false
        ThemeMode.DARK.value -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> OppoDarkColorScheme
        else -> OppoLightColorScheme
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
        typography = OppoTypography,
        shapes = OppoShapes,
        content = content
    )
}
