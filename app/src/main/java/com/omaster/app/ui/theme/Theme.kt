package com.omaster.app.ui.theme

import android.app.Activity
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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
import com.omaster.app.data.EyeProtectionMode
import com.omaster.app.data.ThemeMode

// ==================== ColorOS 16 专业摄影深色配色方案 ====================
private val ColorOSDarkColorScheme = darkColorScheme(
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

// ==================== ColorOS 16 专业摄影浅色配色方案 ====================
private val ColorOSLightColorScheme = lightColorScheme(
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
    eyeProtectionMode: EyeProtectionMode = EyeProtectionMode.OFF,
    eyeProtectionIntensity: Float = 0.3f,
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

            if (eyeProtectionMode != EyeProtectionMode.OFF && eyeProtectionIntensity > 0f) {
                applyEyeProtectionFilter(view, eyeProtectionMode, eyeProtectionIntensity)
            } else {
                view.paint.colorFilter = null
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ColorOSTypography,
        shapes = ColorOSShapes,
        content = content
    )
}

private fun applyEyeProtectionFilter(
    view: android.view.View,
    mode: EyeProtectionMode,
    intensity: Float
) {
    try {
        val colorTemperature = mode.colorTemperature
        val safeIntensity = intensity.coerceIn(0f, 1f)
        val tempRatio = (6500 - colorTemperature).toFloat() / 3500f
        val blueReduction = safeIntensity * tempRatio

        val matrix = ColorMatrix(floatArrayOf(
            1f + safeIntensity * 0.05f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, (1f - blueReduction).coerceIn(0.4f, 1f), 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))

        val satScale = 1f - safeIntensity * 0.1f
        val satMatrix = ColorMatrix().apply { setSaturation(satScale) }
        matrix.postConcat(satMatrix)

        view.paint.colorFilter = ColorMatrixColorFilter(matrix)
    } catch (e: Exception) {
        timber.log.Timber.e(e, "护眼模式应用失败")
    }
}
