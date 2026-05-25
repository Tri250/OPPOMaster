package com.omaster.app.ui.theme

import android.app.Activity
import android.os.Build
import android.view.View
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

// ============================================
// ColorOS 16 光场设计色彩方案
// ============================================
private val LightFieldDarkColorScheme = darkColorScheme(
    primary = LightFieldPrimary,
    onPrimary = HasselbladBlack,
    primaryContainer = LightFieldPrimaryDark,
    onPrimaryContainer = LightFieldOnSurfaceDark,
    secondary = LightFieldSecondary,
    onSecondary = HasselbladBlack,
    secondaryContainer = OppoGreenDark,
    onSecondaryContainer = LightFieldOnSurfaceDark,
    tertiary = LightFieldTertiary,
    onTertiary = HasselbladBlack,
    tertiaryContainer = HasselbladOrange.copy(alpha = 0.3f),
    onTertiaryContainer = LightFieldOnSurfaceDark,
    background = LightFieldBackgroundDark,
    onBackground = LightFieldOnSurfaceDark,
    surface = LightFieldSurfaceDark,
    onSurface = LightFieldOnSurfaceDark,
    surfaceVariant = LightFieldSurfaceElevatedDark,
    onSurfaceVariant = LightFieldOnSurfaceVariantDark,
    outline = LightFieldOutlineDark,
    outlineVariant = LightFieldOutlineDark.copy(alpha = 0.5f),
    error = Error,
    onError = Color.White,
    errorContainer = Error.copy(alpha = 0.3f),
    onErrorContainer = LightFieldOnSurfaceDark
)

private val LightFieldLightColorScheme = lightColorScheme(
    primary = LightFieldPrimary,
    onPrimary = Color.White,
    primaryContainer = LightFieldPrimaryLight.copy(alpha = 0.3f),
    onPrimaryContainer = LightFieldOnSurfaceLight,
    secondary = LightFieldSecondary,
    onSecondary = Color.White,
    secondaryContainer = OppoGreenLight.copy(alpha = 0.3f),
    onSecondaryContainer = LightFieldOnSurfaceLight,
    tertiary = LightFieldTertiary,
    onTertiary = Color.White,
    tertiaryContainer = HasselbladOrange.copy(alpha = 0.2f),
    onTertiaryContainer = LightFieldOnSurfaceLight,
    background = LightFieldBackgroundLight,
    onBackground = LightFieldOnSurfaceLight,
    surface = LightFieldSurfaceLight,
    onSurface = LightFieldOnSurfaceLight,
    surfaceVariant = LightFieldSurfaceElevatedLight,
    onSurfaceVariant = LightFieldOnSurfaceVariantLight,
    outline = LightFieldOutlineLight,
    outlineVariant = LightFieldOutlineLight.copy(alpha = 0.5f),
    error = Error,
    onError = Color.White,
    errorContainer = Error.copy(alpha = 0.2f),
    onErrorContainer = LightFieldOnSurfaceLight
)

// ============================================
// ColorOS 16 金标主题系统
// ============================================
@Composable
fun OMasterTheme(
    themeMode: Int = ThemeMode.SYSTEM.value,
    dynamicColor: Boolean = false, // 默认关闭动态取色，使用光场设计
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
        darkTheme -> LightFieldDarkColorScheme
        else -> LightFieldLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            // ColorOS 16 边缘沉浸式效果
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            
            // 启用光场设计的边缘光效
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode = 
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = OppoShapes,
        content = content
    )
}
