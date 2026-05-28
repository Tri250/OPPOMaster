package com.omaster.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.omaster.app.data.ThemeMode

private val ColorOSLightColorScheme = lightColorScheme(
    primary = ColorOSOrange,
    onPrimary = Color.White,
    primaryContainer = ColorOSOrangeLight.copy(alpha = 0.12f),
    onPrimaryContainer = ColorOSOrangeDark,
    
    secondary = ColorOSTechBlue,
    onSecondary = Color.White,
    secondaryContainer = ColorOSTechBlue.copy(alpha = 0.12f),
    onSecondaryContainer = ColorOSDeepBlue,
    
    tertiary = HasselbladOrange,
    onTertiary = Color.White,
    tertiaryContainer = HasselbladOrange.copy(alpha = 0.12f),
    onTertiaryContainer = ColorOSOrangeDark,
    
    background = ColorOSLightBackground,
    onBackground = ColorOSLightTextPrimary,
    
    surface = ColorOSLightSurface,
    onSurface = ColorOSLightTextPrimary,
    surfaceVariant = ColorOSLightSurfaceVariant,
    onSurfaceVariant = ColorOSLightTextSecondary,
    
    outline = ColorOSLightBorder,
    outlineVariant = ColorOSLightBorder.copy(alpha = 0.5f),
    
    error = ColorOSError,
    onError = Color.White,
    errorContainer = ColorOSError.copy(alpha = 0.12f),
    onErrorContainer = ColorOSError,
    
    inverseSurface = ColorOSDarkSurface,
    inverseOnSurface = ColorOSDarkTextPrimary,
    inversePrimary = ColorOSOrangeLight,
    
    surfaceTint = ColorOSOrange
)

private val ColorOSDarkColorScheme = darkColorScheme(
    primary = ColorOSOrange,
    onPrimary = Color.White,
    primaryContainer = ColorOSOrangeDark.copy(alpha = 0.24f),
    onPrimaryContainer = ColorOSOrangeLight,
    
    secondary = ColorOSTechBlue,
    onSecondary = ColorOSDeepBlue,
    secondaryContainer = ColorOSTechBlue.copy(alpha = 0.24f),
    onSecondaryContainer = ColorOSTechBlue,
    
    tertiary = HasselbladOrange,
    onTertiary = Color.White,
    tertiaryContainer = HasselbladOrange.copy(alpha = 0.24f),
    onTertiaryContainer = HasselbladOrange,
    
    background = ColorOSDarkBackground,
    onBackground = ColorOSDarkTextPrimary,
    
    surface = ColorOSDarkSurface,
    onSurface = ColorOSDarkTextPrimary,
    surfaceVariant = ColorOSDarkSurfaceVariant,
    onSurfaceVariant = ColorOSDarkTextSecondary,
    
    outline = ColorOSDarkBorder,
    outlineVariant = ColorOSDarkBorder.copy(alpha = 0.5f),
    
    error = ColorOSError,
    onError = Color.White,
    errorContainer = ColorOSError.copy(alpha = 0.24f),
    onErrorContainer = ColorOSError,
    
    inverseSurface = ColorOSLightSurface,
    inverseOnSurface = ColorOSLightTextPrimary,
    inversePrimary = ColorOSOrangeDark,
    
    surfaceTint = ColorOSOrange
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

    val colorScheme = if (darkTheme) ColorOSDarkColorScheme else ColorOSLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
