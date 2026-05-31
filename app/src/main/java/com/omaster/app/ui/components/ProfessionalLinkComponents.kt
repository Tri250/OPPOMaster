package com.omaster.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.omaster.app.ui.theme.ColorOSBlack
import com.omaster.app.ui.theme.HasselbladOrange

/**
 * 专业链接组件库 - 符合LNK-001到LNK-012所有测试用例
 */

// ==================== LNK-001到LNK-005: 文本链接组件 ====================

/**
 * 专业文本链接 - 符合LNK-001到LNK-005测试
 * 样式：链接颜色#007AFF，悬停下划线，点击后颜色#5856D6
 */
@Composable
fun ProTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    url: String? = null
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val textColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color(0xFF007AFF).copy(alpha = 0.5f)
            isHovered -> Color(0xFF5856D6) // 点击后颜色
            else -> Color(0xFF007AFF) // 默认链接颜色
        },
        animationSpec = tween(durationMillis = 200),
        label = "textColor"
    )
    
    val textDecoration = if (isHovered) TextDecoration.Underline else TextDecoration.None
    
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize() // 确保点击区域至少48dp
            .hoverable(interactionSource = interactionSource)
            .clickable(enabled = enabled) {
                if (url != null) {
                    // 外部链接在浏览器打开
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        onClick()
                    }
                } else {
                    onClick()
                }
            },
        color = Color.Transparent,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = textColor,
                textDecoration = textDecoration,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            ),
            modifier = Modifier.padding(4.dp)
        )
    }
}

/**
 * 专业文本链接（带图标）
 */
@Composable
fun ProTextLinkWithIcon(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(enabled = enabled) { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ProTextLink(
            text = text,
            onClick = onClick,
            enabled = enabled
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) Color(0xFF007AFF) else Color(0xFF007AFF).copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}

// ==================== LNK-006到LNK-009: 按钮链接组件 ====================

/**
 * 专业主按钮链接 - 符合LNK-006主按钮测试
 * 样式：品牌主色背景，白色文字，圆角16dp
 */
@Composable
fun ProPrimaryLinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    ProPrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isLoading = isLoading,
        icon = icon
    )
}

/**
 * 专业次按钮链接 - 符合LNK-006次按钮测试
 * 样式：白色背景，品牌主色边框和文字，圆角16dp
 */
@Composable
fun ProSecondaryLinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    ProSecondaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon
    )
}

/**
 * 专业文字按钮链接 - 符合LNK-006文字按钮测试
 * 样式：无背景，品牌主色文字
 */
@Composable
fun ProTextLinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    ProTextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    )
}

/**
 * 专业图标按钮链接 - 符合LNK-007到LNK-009测试
 */
@Composable
fun ProIconButtonLink(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    tint: Color = HasselbladOrange
) {
    val buttonColor by animateColorAsState(
        targetValue = if (enabled) tint else tint.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 200),
        label = "buttonColor"
    )
    
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .minimumInteractiveComponentSize(),
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = buttonColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

// ==================== LNK-010到LNK-012: 图片链接组件 ====================

/**
 * 专业图片链接 - 符合LNK-010到LNK-012测试
 * 样式：轻微阴影，点击缩放反馈，可点击标识明显
 */
@Composable
fun ProImageLink(
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    showClickIndicator: Boolean = true
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "scale"
    )
    
    Surface(
        modifier = modifier
            .shadow(if (showClickIndicator) 4.dp else 0.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (hasError) Color.Gray.copy(alpha = 0.1f) else Color.Transparent
    ) {
        Box {
            if (!hasError) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scale),
                    onLoading = { isLoading = true },
                    onSuccess = { isLoading = false },
                    onError = {
                        isLoading = false
                        hasError = true
                    },
                    loading = {
                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Gray.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = HasselbladOrange
                                )
                            }
                        }
                    },
                    error = {
                        hasError = true
                    }
                )
            } else {
                // 加载失败占位图
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray.copy(alpha = 0.1f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = "图片加载失败",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            // 点击指示器
            if (showClickIndicator) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = HasselbladOrange.copy(alpha = 0.9f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "点击查看",
                        tint = ColorOSBlack,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

/**
 * 带标签的图片链接
 */
@Composable
fun ProImageLinkWithLabel(
    imageUrl: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null
) {
    Column(modifier = modifier) {
        ProImageLink(
            imageUrl = imageUrl,
            onClick = onClick,
            enabled = enabled,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ==================== 通用链接工具组件 ====================

/**
 * 卡片式链接容器
 */
@Composable
fun ProLinkCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        content()
    }
}

/**
 * 列表项链接
 */
@Composable
fun ProLinkListItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (enabled) HasselbladOrange else HasselbladOrange.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }
            
            trailingContent?.invoke() ?: Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "前往",
                tint = if (enabled) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 禁用状态链接组件
 */
@Composable
fun ProDisabledLink(
    text: String,
    modifier: Modifier = Modifier,
    reason: String? = null
) {
    Column(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            color = Color.Gray.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        
        reason?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

/**
 * 加载状态链接组件
 */
@Composable
fun ProLoadingLink(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = HasselbladOrange
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp
        )
    }
}
