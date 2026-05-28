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

// ==================== ColorOS 16 深色配色方案 ====================
private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = DeepSpace,
    primaryContainer = AccentPrimaryDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = AccentSecondary,
    onSecondary = Color.White,
    secondaryContainer = AccentSecondaryDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = HasselbladOrange,
    onTertiary = DeepSpace,
    tertiaryContainer = HasselbladDark,
    onTertiaryContainer = TextPrimaryDark,
    background = DeepSpace,
    onBackground = TextPrimaryDark,
    surface = DeepSpaceCard,
    onSurface = TextPrimaryDark,
    surfaceVariant = DeepSpaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    outlineVariant = BorderDark,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.15f),
    onErrorContainer = TextPrimaryDark
)

// ==================== ColorOS 16 浅色配色方案 ====================
private val LightColorScheme = lightColorScheme(
    primary = AccentPrimary,
    onPrimary = Color.White,
    primaryContainer = AccentPrimaryLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = AccentSecondary,
    onSecondary = Color.White,
    secondaryContainer = AccentSecondaryLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = HasselbladOrange,
    onTertiary = Color.White,
    tertiaryContainer = GradientHasselblad.first(),
    onTertiaryContainer = TextPrimaryLight,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    outlineVariant = BorderLight,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.15f),
    onErrorContainer = TextPrimaryLight
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
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
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
        typography = Typography,
        shapes = ColorOSShapes,
        content = content
    )
}
