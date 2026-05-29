package com.omaster.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val OppoOrange = Color(0xFFFF6B00)
val OppoOrangeLight = Color(0xFFFF8C33)
val OppoOrangeDark = Color(0xFFCC5500)

val Hasselblad = Color(0xFFD4A574)
val HasselbladLight = Color(0xFFE5C8A0)

val DeepSpace = Color(0xFF0D0D0D)
val Surface = Color(0xFF121212)
val SurfaceElevated = Color(0xFF1A1A1A)
val SurfaceHover = Color(0xFF222222)

val BorderSubtle = Color(0xFF2A2A2A)
val BorderLight = Color(0xFF3A3A3A)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0B0B0)
val TextTertiary = Color(0xFF6B6B6B)

val OppoGreen = Color(0xFF00C853)
val OppoRed = Color(0xFFFF1744)
val OppoBlue = Color(0xFF2979FF)
val OceanBlue = Color(0xFF00BCD4)
val OppoSunriseGold = Color(0xFFFFD740)

val ColorScheme.darkColorScheme: ColorScheme
    @Composable
    @ReadOnlyComposable
    get() = darkColorScheme(
        primary = OppoOrange,
        onPrimary = DeepSpace,
        primaryContainer = OppoOrangeLight,
        onPrimaryContainer = DeepSpace,
        secondary = Hasselblad,
        onSecondary = DeepSpace,
        secondaryContainer = HasselbladLight,
        onSecondaryContainer = DeepSpace,
        background = DeepSpace,
        onBackground = TextPrimary,
        surface = Surface,
        onSurface = TextPrimary,
        surfaceVariant = SurfaceElevated,
        onSurfaceVariant = TextSecondary,
        error = OppoRed,
        onError = Color.White,
        errorContainer = OppoRed,
        onErrorContainer = Color.White,
        outline = BorderSubtle,
        outlineVariant = BorderLight,
        scrim = DeepSpace,
        inverseSurface = SurfaceElevated,
        inverseOnSurface = TextPrimary,
        inversePrimary = OppoOrangeLight
    )

@Composable
fun OMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: Int = 0,
    content: @Composable () -> Unit
) {
    val colors = darkColorScheme
    
    val typography = Typography(
        displayLarge = androidx.compose.material3.Typography().displayLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = androidx.compose.material3.Typography().displayMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 45.sp,
            lineHeight = 52.sp
        ),
        displaySmall = androidx.compose.material3.Typography().displaySmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp
        ),
        headlineLarge = androidx.compose.material3.Typography().headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp
        ),
        headlineMedium = androidx.compose.material3.Typography().headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp
        ),
        headlineSmall = androidx.compose.material3.Typography().headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp
        ),
        titleLarge = androidx.compose.material3.Typography().titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        titleMedium = androidx.compose.material3.Typography().titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.1.sp
        ),
        titleSmall = androidx.compose.material3.Typography().titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = androidx.compose.material3.Typography().bodyLarge.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = androidx.compose.material3.Typography().bodyMedium.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = androidx.compose.material3.Typography().bodySmall.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        labelLarge = androidx.compose.material3.Typography().labelLarge.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = androidx.compose.material3.Typography().labelMedium.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = androidx.compose.material3.Typography().labelSmall.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
    
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(28.dp)
    )
    
    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = shapes,
        content = content
    )
}

val Typography.bodyBold: TextStyle
    @Composable
    @ReadOnlyComposable
    get() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )

val Typography.caption: TextStyle
    @Composable
    @ReadOnlyComposable
    get() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        color = TextTertiary
    )
