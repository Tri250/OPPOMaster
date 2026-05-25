package com.omaster.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ============================================
// OPPO 光场设计圆角系统 - ColorOS 16风格
// ============================================
val OppoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// 毛玻璃卡片专用圆角
val GlassCardShape = RoundedCornerShape(20.dp)

// 按钮专用圆角
val ButtonShape = RoundedCornerShape(14.dp)

// HNCS认证标识专用圆角
val HncsBadgeShape = RoundedCornerShape(10.dp)

// 预设封面卡片圆角
val PresetCoverShape = RoundedCornerShape(18.dp)
