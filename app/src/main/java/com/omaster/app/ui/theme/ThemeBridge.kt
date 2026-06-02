package com.omaster.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object Colors {
    val Primary = HasselbladOrange
    val PrimaryVariant = HasselbladOrangeDark
    val Secondary = DeepOceanBlue
    val SecondaryVariant = DeepOceanBlueDark
    val Background = ColorOSBlack
    val Surface = ColorOSCard
    val OnPrimary = ColorOSBlack
    val OnSecondary = Color.White
    val OnBackground = ColorOSTextPrimary
    val OnSurface = ColorOSTextPrimary
    val Accent = AuroraGreen
    val Error = ErrorPro
}

object Spacing {
    val tiny = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val xLarge = 24.dp
    val xxLarge = 32.dp
}

object Typography {
    val displayLarge get() = ColorOSTypography.displayLarge
    val displayMedium get() = ColorOSTypography.displayMedium
    val displaySmall get() = ColorOSTypography.displaySmall
    val headlineLarge get() = ColorOSTypography.headlineLarge
    val headlineMedium get() = ColorOSTypography.headlineMedium
    val headlineSmall get() = ColorOSTypography.headlineSmall
    val titleLarge get() = ColorOSTypography.titleLarge
    val titleMedium get() = ColorOSTypography.titleMedium
    val titleSmall get() = ColorOSTypography.titleSmall
    val bodyLarge get() = ColorOSTypography.bodyLarge
    val bodyMedium get() = ColorOSTypography.bodyMedium
    val bodySmall get() = ColorOSTypography.bodySmall
    val labelLarge get() = ColorOSTypography.labelLarge
    val labelMedium get() = ColorOSTypography.labelMedium
    val labelSmall get() = ColorOSTypography.labelSmall
}

val hasselbladOrange: Color = HasselbladOrange
