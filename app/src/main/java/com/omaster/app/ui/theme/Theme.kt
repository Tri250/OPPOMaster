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

// ==================== OPPO Master V2.0 深色配色方案 ====================
private val OppoDarkColorScheme = darkColorScheme(
    primary = OppoOrange,
    onPrimary = OppoBlack,
    primaryContainer = OppoOrangeDark,
    onPrimaryContainer = TextPrimary,
    secondary = Info,
    onSecondary = OppoWhite,
    secondaryContainer = Neutral700,
    onSecondaryContainer = TextPrimary,
    tertiary = HasselbladOrange,
    onTertiary = OppoBlack,
    tertiaryContainer = Neutral700,
    onTertiaryContainer = TextPrimary,
    background = BgPrimary,
    onBackground = TextPrimary,
    surface = BgSecondary,
    onSurface = TextPrimary,
    surfaceVariant = BgElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderDefault,
    outlineVariant = BorderLight,
    error = Error,
    onError = OppoWhite,
    errorContainer = Error.copy(alpha = 0.15f),
    onErrorContainer = TextPrimary
)

// ==================== OPPO Master V2.0 浅色配色方案 ====================
private val OppoLightColorScheme = lightColorScheme(
    primary = OppoOrange,
    onPrimary = OppoWhite,
    primaryContainer = OppoOrangeLight,
    onPrimaryContainer = TextLightPrimary,
    secondary = Info,
    onSecondary = OppoWhite,
    secondaryContainer = Neutral100,
    onSecondaryContainer = TextLightPrimary,
    tertiary = HasselbladOrange,
    onTertiary = OppoWhite,
    tertiaryContainer = Neutral100,
    onTertiaryContainer = TextLightPrimary,
    background = BgLightPrimary,
    onBackground = TextLightPrimary,
    surface = BgLightSecondary,
    onSurface = TextLightPrimary,
    surfaceVariant = Neutral50,
    onSurfaceVariant = TextLightSecondary,
    outline = BorderLightDefault,
    outlineVariant = Neutral200,
    error = Error,
    onError = OppoWhite,
    errorContainer = Error.copy(alpha = 0.15f),
    onErrorContainer = TextLightPrimary
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
