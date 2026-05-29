package com.omaster.app.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== OPPO Master V2.0 UI色彩系统 ====================
// 基于ColorOS 16 Aquatic Design - OPPO官方品牌和哈苏合作规范

// ==================== 核心色彩规范 ====================
// OPPO 主橙 - 官方品牌色
val OppoOrange = Color(0xFFFF6B35)
val OppoOrangeDark = Color(0xFFE55A2B)
val OppoOrangeLight = Color(0xFFFFB399)

// 哈苏橙 - 哈苏联名品牌色
val HasselbladOrange = Color(0xFFD4A574)

// OPPO 黑/白
val OppoBlack = Color(0xFF0F0F0F)
val OppoWhite = Color(0xFFFFFFFF)

// ==================== 中性灰系统 - OPPO 官方规范 ====================
val Neutral50 = Color(0xFFF5F5F5)
val Neutral100 = Color(0xFFE5E5E5)
val Neutral200 = Color(0xFFD4D4D4)
val Neutral300 = Color(0xFFA3A3A3)
val Neutral400 = Color(0xFF737373)
val Neutral500 = Color(0xFF525252)
val Neutral600 = Color(0xFF404040)
val Neutral700 = Color(0xFF262626)
val Neutral800 = Color(0xFF1A1A1A)
val Neutral900 = Color(0xFF0F0F0F)

// ==================== 功能色 - OPPO 官方规范 ====================
val Success = Color(0xFF22C55E)
val Warning = Color(0xFFF59E0B)
val Error = Color(0xFFEF4444)
val Info = Color(0xFF3B82F6)

// ==================== 深色主题 - ColorOS 16 规范 ====================
val BgPrimary = OppoBlack
val BgSecondary = Neutral800
val BgTertiary = Neutral700
val BgElevated = Color(0xFF1C1C1E)

val TextPrimary = OppoWhite
val TextSecondary = Color(0xFFB3B3B3)
val TextTertiary = Neutral500
val TextQuaternary = Neutral400

val BorderDefault = Color(0xFF2A2A2A)
val BorderLight = Neutral600

// ==================== 浅色主题 - ColorOS 16 规范 ====================
val BgLightPrimary = OppoWhite
val BgLightSecondary = Neutral50

val TextLightPrimary = Neutral900
val TextLightSecondary = Neutral600
val TextLightTertiary = Neutral400

val BorderLightDefault = Neutral200

// ==================== 渐变色彩 - OPPO 品牌渐变 ====================
val GradientOppoBrand = listOf(
    OppoOrange,
    HasselbladOrange
)

val GradientHasselbladPro = listOf(
    HasselbladOrange,
    Color(0xFFB89A5C)
)

// ==================== 透明度规范 - OPPO ====================
const val AlphaBlackout = 0.8f
const val AlphaMedium = 0.6f
const val AlphaLight = 0.3f
const val AlphaVeryLight = 0.1f

// 兼容性别名（保持向后兼容）
val AccentPrimary = OppoOrange
val AccentSecondary = Info
val OppoSunriseGold = OppoOrange
val OppoSunriseGoldLight = OppoOrangeLight
val OppoSunriseGoldDark = OppoOrangeDark
val HasselbladOrangePro = HasselbladOrange
val OppoVitalGreen = Success
val OceanBlue = Info
val OppoDeepSpace = BgPrimary
val OppoCardSurface = BgSecondary
val OppoElevated = BgElevated
val OppoTextPrimary = TextPrimary
val OppoTextSecondary = TextSecondary
val OppoTextTertiary = TextTertiary
val OppoBorder = BorderDefault
val OppoBorderLight = BorderLight
val OppoLightBackground = BgLightPrimary
val OppoLightSurface = BgLightSecondary
val OppoLightTextPrimary = TextLightPrimary
val OppoLightTextSecondary = TextLightSecondary
val SuccessVital = Success
val WarningVital = Warning
val ErrorVital = Error
val InfoVital = Info
val DeepSpace = OppoBlack
