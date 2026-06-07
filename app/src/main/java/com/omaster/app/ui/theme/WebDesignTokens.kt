package com.omaster.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==================== Web Design System Tokens ====================
// 基于Web设计的完整设计系统适配

object WebColors {
    // Zinc 色系 - Web主背景色
    val Zinc50 = Color(0xFFFAFAFA)
    val Zinc100 = Color(0xFFF4F4F5)
    val Zinc200 = Color(0xFFE4E4E7)
    val Zinc300 = Color(0xFFD4D4D8)
    val Zinc400 = Color(0xFFA1A1AA)
    val Zinc500 = Color(0xFF71717A)
    val Zinc600 = Color(0xFF52525B)
    val Zinc700 = Color(0xFF3F3F46)
    val Zinc800 = Color(0xFF27272A)
    val Zinc900 = Color(0xFF18181B)
    val Zinc950 = Color(0xFF09090B)

    // Orange 色系 - Web强调色
    val Orange50 = Color(0xFFFFF7ED)
    val Orange100 = Color(0xFFFFEDD5)
    val Orange200 = Color(0xFFFED7AA)
    val Orange300 = Color(0xFFFDBA74)
    val Orange400 = Color(0xFFFB923C)
    val Orange500 = Color(0xFFF97316)  // Web主橙色
    val Orange600 = Color(0xFFEA580C)
    val Orange700 = Color(0xFFC2410C)
    val Orange800 = Color(0xFF9A3412)
    val Orange900 = Color(0xFF7C2D12)
    val Orange950 = Color(0xFF431407)

    // Web背景色
    val BackgroundPrimary = Zinc900      // bg-zinc-900
    val BackgroundSecondary = Zinc800    // bg-zinc-800
    val BackgroundTertiary = Zinc950     // bg-zinc-950

    // Web卡片色
    val CardBackground = Zinc800.copy(alpha = 0.5f)  // bg-zinc-800/50
    val CardBackgroundSolid = Zinc800
    val CardBorder = Zinc700.copy(alpha = 0.5f)      // border-zinc-700/50
    val CardBorderHover = Orange500.copy(alpha = 0.3f) // hover:border-orange-500/30

    // Web文字色
    val TextPrimary = Color.White
    val TextSecondary = Zinc400          // text-zinc-400
    val TextTertiary = Zinc500           // text-zinc-500
    val TextMuted = Zinc600              // text-zinc-600

    // Web强调色
    val AccentPrimary = Orange500
    val AccentSecondary = Orange600
    val AccentGradientStart = Orange500
    val AccentGradientEnd = Orange600

    // Web功能色
    val Success = Color(0xFF22C55E)      // Green-500
    val Warning = Color(0xFFEAB308)      // Yellow-500
    val Error = Color(0xFFEF4444)        // Red-500
    val Info = Color(0xFF3B82F6)         // Blue-500

    // Web标签/徽章背景
    val TagBackground = Zinc700.copy(alpha = 0.5f)
    val TagText = Zinc300

    // Web渐变
    val GradientOrange = listOf(Orange500, Orange600)
    val GradientCardOverlay = listOf(
        Zinc900.copy(alpha = 0f),
        Zinc900.copy(alpha = 0.8f)
    )
}

object WebSpacing {
    // 基础间距
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val base = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xl2 = 32.dp
    val xl3 = 48.dp
    val xl4 = 64.dp
    val xl5 = 80.dp
    val xl6 = 96.dp

    // 组件间距
    val CardPadding = 16.dp      // p-4
    val CardGap = 24.dp          // gap-6
    val SectionPadding = 96.dp   // py-24
    val ContainerPadding = 24.dp // px-6
}

object WebRadius {
    val sm = 4.dp
    val md = 6.dp
    val base = 8.dp
    val lg = 12.dp
    val xl = 16.dp
    val xl2 = 20.dp
    val xl3 = 24.dp
    val full = 9999.dp
}

object WebTypography {
    // 字体大小
    val xs = 12.sp
    val sm = 14.sp
    val base = 16.sp
    val lg = 18.sp
    val xl = 20.sp
    val xl2 = 24.sp
    val xl3 = 30.sp
    val xl4 = 36.sp
    val xl5 = 48.sp

    // 行高
    val LeadingTight = 20.sp
    val LeadingSnug = 24.sp
    val LeadingNormal = 28.sp
    val LeadingRelaxed = 32.sp
    val LeadingLoose = 40.sp

    // 字间距
    val TrackingTight = (-0.8).sp
    val TrackingNormal = 0.sp
    val TrackingWide = 0.4.sp
}

object WebShadows {
    val sm = androidx.compose.ui.graphics.RectangleShape
    val md = androidx.compose.ui.graphics.RectangleShape
    val lg = androidx.compose.ui.graphics.RectangleShape
    val xl = androidx.compose.ui.graphics.RectangleShape

    // 阴影颜色透明度
    const val ShadowOpacityLight = 0.1f
    const val ShadowOpacityMedium = 0.25f
    const val ShadowOpacityHeavy = 0.4f
}

object WebAnimations {
    // 动画时长
    const val DurationFast = 150
    const val DurationNormal = 300
    const val DurationSlow = 500

    // 动画延迟
    const val StaggerDelay = 100

    // 缓动函数
    val EaseOut = androidx.compose.animation.core.FastOutSlowInEasing
    val EaseInOut = androidx.compose.animation.core.LinearOutSlowInEasing
}

// Web设计语义化颜色方案
object WebSemanticColors {
    // 背景
    val Background = WebColors.BackgroundPrimary
    val Surface = WebColors.CardBackground
    val SurfaceSolid = WebColors.CardBackgroundSolid

    // 文字
    val OnBackground = WebColors.TextPrimary
    val OnSurface = WebColors.TextPrimary
    val OnSurfaceVariant = WebColors.TextSecondary

    // 主色
    val Primary = WebColors.AccentPrimary
    val OnPrimary = Color.White
    val PrimaryContainer = WebColors.Orange600
    val OnPrimaryContainer = Color.White

    // 边框
    val Outline = WebColors.CardBorder
    val OutlineVariant = WebColors.Zinc800

    // 功能色
    val Error = WebColors.Error
    val Success = WebColors.Success
    val Warning = WebColors.Warning
    val Info = WebColors.Info
}
