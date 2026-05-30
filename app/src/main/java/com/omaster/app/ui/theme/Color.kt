package com.omaster.app.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== OPPO 2026 年轻UI色彩系统 ====================
// 基于ColorOS 16.1 Aquatic Design - 年轻化、活力，科技感

// ========== 品牌主色调 - 按照测试用例要求 ==========
// OPPO品牌主色 #FF6B35
val OppoPrimary = Color(0xFFFF6B35)
val OppoPrimaryLight = Color(0xFFFF8F6B)
val OppoPrimaryDark = Color(0xFFE55A2D)

// AccentPrimary - 用于保持向后兼容，与OppoPrimary相同
val AccentPrimary = OppoPrimary

// 哈苏橙 #D4A574 - 一级标题用色
val HasselbladOrange = Color(0xFFD4A574)
val HasselbladOrangeLight = Color(0xFFE0B894)
val HasselbladOrangeDark = Color(0xFFB88A54)

// OPPO 绿 - 年轻活力
val OppoVitalGreen = Color(0xFF2DB47A)
val OppoVitalGreenLight = Color(0xFF68D391)
val OppoVitalGreenDark = Color(0xFF249D6B)

// ========== 辅助色彩系统 - OPPO 2026 年轻风格 ==========
// 海洋蓝 - 科技感
val OceanBlue = Color(0xFF3B82F6)
val OceanBlueLight = Color(0xFF60A5FA)
val OceanBlueDark = Color(0xFF2563EB)

// 樱花粉 - 柔和活力
val SakuraPink = Color(0xFFEC4899)
val SakuraPinkLight = Color(0xFFF472B6)
val SakuraPinkDark = Color(0xFFDB2777)

// 极光紫 - 年轻科技
val AuroraPurple = Color(0xFF8B5CF6)
val AuroraPurpleLight = Color(0xFFA78BFA)
val AuroraPurpleDark = Color(0xFF7C3AED)

// ========== 功能色 - OPPO 2026规范 ==========
val SuccessVital = Color(0xFF22C55E)
val WarningVital = Color(0xFFF59E0B)
val ErrorVital = Color(0xFFEF4444)
val InfoVital = Color(0xFF3B82F6)

// ========== 深色主题 - 按照测试用例要求 ==========
// 页面背景色 #0F0F0F，卡片背景色 #1A1A1A
val DeepSpace = Color(0xFF0F0F0F)
val CardBackground = Color(0xFF1A1A1A)
val SurfaceElevated = Color(0xFF242424)
val GlassEffect = Color(0x990F0F0F)

// OPPO绿 #00C853
val OppoGreen = Color(0xFF00C853)
val OppoGreenLight = Color(0xFF33D976)

// 错误红
val ErrorRed = Color(0xFFFF4444)

// 文字层级 - 按照测试用例要求
val TextPrimary = Color(0xFFFFFFFF)      // 一级文字 #FFFFFF
val TextSecondary = Color(0xFFCCCCCC)    // 二级文字 #CCCCCC
val TextTertiary = Color(0xFF999999)     // 三级文字 #999999
val TextQuaternary = Color(0xFF666666)

// 边框分隔
val OppoBorder = Color(0xFF272727)
val OppoBorderLight = Color(0xFF404040)

// ========== 浅色主题 - OPPO 2026 晨曦白 Pro ==========
val OppoLightBackground = Color(0xFFF8F8F8)
val OppoLightSurface = Color(0xFFFFFFFF)
val OppoLightElevated = Color(0xFFFAFAFA)
val OppoLightGlass = Color(0x99FFFFFF)

// 文字层级
val OppoLightTextPrimary = Color(0xFF1A1A1A)
val OppoLightTextSecondary = Color(0xFF6B6B6B)
val OppoLightTextTertiary = Color(0xFFA3A3A3)
val OppoLightTextQuaternary = Color(0xFFC4C4C4)

// 边框分隔
val OppoLightBorder = Color(0xFFE5E5E5)
val OppoLightBorderLight = Color(0xFFF0F0F0)

// ========== 渐变色彩 - OPPO 2026 青春活力 ==========
// 日落金渐变
val GradientSunriseVital = listOf(
    Color(0xFFFFD166),
    Color(0xFFFF9B47)
)

// 哈苏专业渐变
val GradientHasselbladPro = listOf(
    Color(0xFFF3C177),
    Color(0xFFD48D3F)
)

// 海洋科技渐变
val GradientOceanVital = listOf(
    Color(0xFF06B6D4),
    Color(0xFF3B82F6)
)

// 极光梦幻渐变
val GradientAuroraVital = listOf(
    Color(0xFFA78BFA),
    Color(0xFF8B5CF6)
)

// 森林生机渐变
val GradientForestVital = listOf(
    Color(0xFF68D391),
    Color(0xFF2DB47A)
)

// 多色活力渐变（用于装饰）
val GradientVibrant = listOf(
    Color(0xFFFF6B6B),
    Color(0xFFFFD93D),
    Color(0xFF6BCB77),
    Color(0xFF4D96FF)
)

// ========== 中性灰阶系统 - OPPO 2026 规范 ==========
val OppoGrey50 = Color(0xFFFAFAFA)
val OppoGrey100 = Color(0xFFF5F5F5)
val OppoGrey200 = Color(0xFFE5E5E5)
val OppoGrey300 = Color(0xFFD4D4D4)
val OppoGrey400 = Color(0xFFA3A3A3)
val OppoGrey500 = Color(0xFF737373)
val OppoGrey600 = Color(0xFF525252)
val OppoGrey700 = Color(0xFF404040)
val OppoGrey800 = Color(0xFF262626)
val OppoGrey900 = Color(0xFF171717)

// ========== 透明度规范 - OPPO 2026 ==========
const val AlphaBlackout = 0.8f    // 完全遮蔽
const val AlphaMedium = 0.6f      // 中度遮挡
const val AlphaLight = 0.3f       // 轻微遮挡
const val AlphaVeryLight = 0.1f   // 极轻遮挡
