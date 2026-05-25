package com.omaster.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector

// ============================================
// OPPO 哈苏品牌色 - ColorOS 16光场设计体系
// ============================================

// 哈苏自然色彩系统 (HNCS) 核心色
val HasselbladOrange = Color(0xFFD4A574)
val HasselbladBlack = Color(0xFF0A0A0A)
val HasselbladNeutral = Color(0xFF888888)

// OPPO品牌色 - 绿色系列
val OppoGreen = Color(0xFF00C853)
val OppoGreenLight = Color(0xFF66FFA3)
val OppoGreenDark = Color(0xFF009624)

// 光场设计主色 - 动态光效
val LightFieldPrimary = Color(0xFFE5A94A)
val LightFieldPrimaryLight = Color(0xFFFFC97A)
val LightFieldPrimaryDark = Color(0xFFB37B1E)
val LightFieldSecondary = Color(0xFF4CAF50)
val LightFieldTertiary = HasselbladOrange

// ============================================
// 光场设计 - 深色主题
// ============================================
val LightFieldBackgroundDark = Color(0xFF121212)
val LightFieldSurfaceDark = Color(0xFF1E1E1E)
val LightFieldSurfaceElevatedDark = Color(0xFF252525)
val LightFieldOnSurfaceDark = Color(0xFFFFFFFF)
val LightFieldOnSurfaceVariantDark = Color(0xFFB3B3B3)
val LightFieldOutlineDark = Color(0xFF444444)

// 半毛玻璃背景 - 不同透明度层级
val GlassLightDark = Color(0x40121212)
val GlassMediumDark = Color(0x60121212)
val GlassHeavyDark = Color(0x80121212)

// 光场光晕渐变
val LightFieldGlowDark = Brush.radialGradient(
    0f to LightFieldPrimary.copy(alpha = 0.3f),
    1f to Color.Transparent
)

val LightFieldEdgeDark = Brush.horizontalGradient(
    0f to LightFieldPrimary.copy(alpha = 0.15f),
    0.5f to Color.Transparent,
    1f to LightFieldPrimary.copy(alpha = 0.15f)
)

// ============================================
// 光场设计 - 浅色主题
// ============================================
val LightFieldBackgroundLight = Color(0xFFFAFAFA)
val LightFieldSurfaceLight = Color(0xFFFFFFFF)
val LightFieldSurfaceElevatedLight = Color(0xFFF5F5F5)
val LightFieldOnSurfaceLight = Color(0xFF121212)
val LightFieldOnSurfaceVariantLight = Color(0xFF666666)
val LightFieldOutlineLight = Color(0xFFE0E0E0)

// 半毛玻璃背景 - 不同透明度层级
val GlassLightLight = Color(0x40FAFAFA)
val GlassMediumLight = Color(0x60FAFAFA)
val GlassHeavyLight = Color(0x80FAFAFA)

// 光场光晕渐变
val LightFieldGlowLight = Brush.radialGradient(
    0f to LightFieldPrimary.copy(alpha = 0.2f),
    1f to Color.Transparent
)

val LightFieldEdgeLight = Brush.horizontalGradient(
    0f to LightFieldPrimary.copy(alpha = 0.1f),
    0.5f to Color.Transparent,
    1f to LightFieldPrimary.copy(alpha = 0.1f)
)

// ============================================
// 功能色 - 语义化命名
// ============================================
val Success = Color(0xFF4CAF50)
val Warning = Color(0xFFFF9800)
val Error = Color(0xFFF44336)
val Info = Color(0xFF2196F3)

// ============================================
// 渐变预设
// ============================================
val HasselbladGradient = Brush.linearGradient(
    0f to HasselbladOrange,
    1f to LightFieldPrimary
)

val OppoGreenGradient = Brush.linearGradient(
    0f to OppoGreenLight,
    1f to OppoGreen
)

val LightFieldGradient = Brush.linearGradient(
    0f to LightFieldPrimaryLight,
    0.5f to LightFieldPrimary,
    1f to LightFieldPrimaryDark
)

val GlassSurfaceGradientDark = Brush.verticalGradient(
    0f to GlassMediumDark,
    1f to GlassHeavyDark
)

val GlassSurfaceGradientLight = Brush.verticalGradient(
    0f to GlassMediumLight,
    1f to GlassHeavyLight
)
