package com.omaster.app.ui.theme

import androidx.compose.ui.graphics.Color

// ColorOS 16 主色调
val ColorOSOrange = Color(0xFFFF6B35)
val ColorOSOrangeLight = Color(0xFFFF8A5C)
val ColorOSOrangeDark = Color(0xFFE55A28)

// ColorOS 16 辅助色
val ColorOSDeepBlue = Color(0xFF1A1A2E)
val ColorOSTechBlue = Color(0xFF00D9FF)

// ColorOS 16 功能色
val ColorOSSuccess = Color(0xFF10B981)
val ColorOSWarning = Color(0xFFF59E0B)
val ColorOSError = Color(0xFFEF4444)
val ColorOSInfo = Color(0xFF3B82F6)

// ColorOS 16 中性色 - 浅色主题
val ColorOSLightBackground = Color(0xFFFAFAFA)
val ColorOSLightSurface = Color(0xFFFFFFFF)
val ColorOSLightTextPrimary = Color(0xFF1A1A2E)
val ColorOSLightTextSecondary = Color(0xFF6B7280)
val ColorOSLightTextDisabled = Color(0xFF9CA3AF)
val ColorOSLightBorder = Color(0xFFE5E7EB)
val ColorOSLightSurfaceVariant = Color(0xFFF3F4F6)

// ColorOS 16 中性色 - 深色主题
val ColorOSDarkBackground = Color(0xFF0F0F1A)
val ColorOSDarkSurface = Color(0xFF1A1A2E)
val ColorOSDarkSurfaceVariant = Color(0xFF252538)
val ColorOSDarkTextPrimary = Color(0xFFFFFFFF)
val ColorOSDarkTextSecondary = Color(0xFFB4B4C4)
val ColorOSDarkTextDisabled = Color(0xFF6B6B7B)
val ColorOSDarkBorder = Color(0xFF3A3A4E)

// 品牌色 (保持原有命名兼容性)
val HasselbladOrange = ColorOSOrange
val OppoGreen = ColorOSSuccess
val AccentPrimary = ColorOSOrange
val AccentSecondary = ColorOSTechBlue

// 深色主题
val DeepSpace = ColorOSDarkBackground
val DeepSpaceLight = ColorOSDarkSurface
val TextPrimaryDark = ColorOSDarkTextPrimary
val TextSecondaryDark = ColorOSDarkTextSecondary
val GlassBackgroundDark = Color(0x401A1A2E)

// 浅色主题
val LightBackground = ColorOSLightBackground
val LightSurface = ColorOSLightSurface
val TextPrimaryLight = ColorOSLightTextPrimary
val TextSecondaryLight = ColorOSLightTextSecondary
val GlassBackgroundLight = Color(0x40FAFAFA)

// ColorOS 毛玻璃效果
val GlassMorphismLight = Color(0xB3FFFFFF)
val GlassMorphismDark = Color(0xB31A1A2E)

// ColorOS 参数卡片专用深色
val ParamCardBackground = ColorOSDeepBlue
val ParamCardText = Color(0xFFFFFFFF)
val ParamCardLabel = Color(0xFF9CA3AF)

// ColorOS 渐变色
val GradientOrange = listOf(ColorOSOrange, ColorOSOrangeLight)
val GradientTech = listOf(ColorOSTechBlue, ColorOSOrange)
val GradientDark = listOf(ColorOSDeepBlue, Color(0xFF2D2D4A))
