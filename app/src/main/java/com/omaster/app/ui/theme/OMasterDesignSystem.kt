package com.omaster.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object OMasterColors {
    val Primary = Color(0xFFD4A574)
    val PrimaryVariant = Color(0xFFC9976A)
    val Secondary = Color(0xFF1A1A1A)
    val SecondaryVariant = Color(0xFF2D2D2D)
    
    val Background = Color(0xFF0A0A0A)
    val Surface = Color(0xFF141414)
    val SurfaceVariant = Color(0xFF1E1E1E)
    val SurfaceElevated = Color(0xFF252525)
    
    val OnPrimary = Color(0xFF000000)
    val OnSecondary = Color(0xFFFFFFFF)
    val OnBackground = Color(0xFFFFFFFF)
    val OnSurface = Color(0xFFFFFFFF)
    val OnSurfaceVariant = Color(0xFFB3B3B3)
    
    val AccentOrange = Color(0xFFFF8C42)
    val AccentGold = Color(0xFFC9A962)
    val AccentBlue = Color(0xFF4A90D9)
    val AccentGreen = Color(0xFF4CAF50)
    val AccentRed = Color(0xFFE53935)
    
    val HasselbladOrange = Color(0xFFD4A574)
    val HasselbladGold = Color(0xFFC9A962)
    val HasselbladBrown = Color(0xFF8B6914)
    
    val GradientStart = Color(0xFF2D2D2D)
    val GradientEnd = Color(0xFF1A1A1A)
    
    val GlassBackground = Color(0x1AFFFFFF)
    val GlassBorder = Color(0x33FFFFFF)
    val GlassHighlight = Color(0x0DFFFFFF)
    
    val Divider = Color(0x1AFFFFFF)
    val Disabled = Color(0x4DFFFFFF)
    val Ripple = Color(0x33FFFFFF)
}

object OMasterTypography {
    val DisplayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    )
    
    val DisplayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    )
    
    val DisplaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    )
    
    val HeadlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    )
    
    val HeadlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )
    
    val HeadlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    )
    
    val TitleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    )
    
    val TitleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )
    
    val TitleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
    
    val BodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    
    val BodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
    
    val BodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
    
    val LabelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
    
    val LabelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    
    val LabelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
}

object OMasterSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
    val huge = 64.dp
    
    val ScreenPadding = 20.dp
    val CardPadding = 16.dp
    val ItemSpacing = 12.dp
    val SectionSpacing = 24.dp
}

object OMasterRadius {
    val none = 0.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val full = 999.dp
    
    val Card = 16.dp
    val Button = 12.dp
    val Chip = 8.dp
    val Dialog = 24.dp
    val BottomSheet = 28.dp
}

object OMasterElevation {
    val none = 0.dp
    val low = 2.dp
    val medium = 4.dp
    val high = 8.dp
    val highest = 16.dp
    
    val Card = 4.dp
    val AppBar = 0.dp
    val BottomSheet = 16.dp
    val Dialog = 24.dp
    val Floating = 8.dp
}

object OMasterAnimation {
    val DurationFast = 150
    val DurationNormal = 300
    val DurationSlow = 500
    val DurationVerySlow = 800

    val EasingStandard = androidx.compose.animation.core.FastOutSlowInEasing
    val EasingDecelerate = androidx.compose.animation.core.DecelerateEasing
    val EasingAccelerate = androidx.compose.animation.core.AccelerateDecelerateEasing
}

object OMasterShadow {
    val SoftElevation = 4.dp
    val MediumElevation = 8.dp
    val StrongElevation = 16.dp
    val GlowElevation = 12.dp

    val SoftColor = Color(0x40000000)
    val MediumColor = Color(0x60000000)
    val StrongColor = Color(0x80000000)
    val GlowColor = Color(0x40D4A574)
}

object OMasterIcons {
    val SizeSmall = 16.dp
    val SizeMedium = 24.dp
    val SizeLarge = 32.dp
    val SizeXLarge = 48.dp
    
    val StrokeWidth = 1.5.dp
    val StrokeWidthBold = 2.dp
}
