package com.omaster.app.presentation.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.omaster.app.R

/**
 * 毛玻璃效果卡片组件
 * 提供 backdropBlur 风格的毛玻璃视觉效果
 *
 * @param modifier 修饰符
 * @param transparency 背景透明度 (0.0 - 1.0)
 * @param cornerRadius 圆角半径
 * @param blurRadius 模糊半径 (仅支持 Android 12+)
 * @param content 卡片内容
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    transparency: Float = 0.15f,
    cornerRadius: Dp = 16.dp,
    blurRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val baseColor = if (isDarkTheme) {
        Color.White.copy(alpha = transparency)
    } else {
        Color.Black.copy(alpha = transparency)
    }

    // Android 12+ 使用原生模糊效果
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(cornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = baseColor
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                baseColor.copy(alpha = 0.3f),
                                baseColor.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .glassmorphicBorder(cornerRadius, isDarkTheme),
                content = content
            )
        }
    } else {
        // 低版本使用渐变模拟毛玻璃效果
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(cornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                baseColor.copy(alpha = transparency + 0.1f),
                                baseColor.copy(alpha = transparency)
                            )
                        )
                    )
                    .glassmorphicBorder(cornerRadius, isDarkTheme),
                content = content
            )
        }
    }
}

/**
 * 毛玻璃边框修饰符
 */
private fun Modifier.glassmorphicBorder(
    cornerRadius: Dp,
    isDarkTheme: Boolean
): Modifier = this.then(
    Modifier.border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = if (isDarkTheme) {
                listOf(
                    Color.White.copy(alpha = 0.3f),
                    Color.White.copy(alpha = 0.05f)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.6f),
                    Color.White.copy(alpha = 0.2f)
                )
            }
        ),
        shape = RoundedCornerShape(cornerRadius)
    )
)

/**
 * 高级毛玻璃卡片 - 支持更多自定义选项
 *
 * @param modifier 修饰符
 * @param backgroundColors 自定义背景渐变颜色
 * @param cornerRadius 圆角半径
 * @param borderWidth 边框宽度
 * @param content 卡片内容
 */
@Composable
fun AdvancedGlassCard(
    modifier: Modifier = Modifier,
    backgroundColors: List<Color>? = null,
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val defaultColors = if (isDarkTheme) {
        listOf(
            Color(0xFF2A2A2A).copy(alpha = 0.6f),
            Color(0xFF1A1A1A).copy(alpha = 0.4f)
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.7f),
            Color.White.copy(alpha = 0.4f)
        )
    }

    val colors = backgroundColors ?: defaultColors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(colors = colors)
            )
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = if (isDarkTheme) {
                        listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.5f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    }
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        content()
    }
}

/**
 * 计算颜色亮度
 */
private fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
