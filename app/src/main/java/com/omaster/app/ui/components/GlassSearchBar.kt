package com.omaster.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.omaster.app.ui.animation.ColorOSAnimationDuration
import com.omaster.app.ui.animation.ColorOSEasing
import com.omaster.app.ui.animation.ColorOSScale
import com.omaster.app.ui.theme.*

@Composable
fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索预设...",
    onSearch: () -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> ColorOSScale.Pressed
            isFocused -> 1.02f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "scale"
    )
    
    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> Colors.HasselbladOrange.copy(alpha = 0.6f)
            else -> Colors.GlassBorder.copy(alpha = 0.3f)
        },
        animationSpec = tween(200),
        label = "borderColor"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> Colors.GlassBackground.copy(alpha = 0.35f)
            else -> Colors.GlassBackground.copy(alpha = 0.2f)
        },
        animationSpec = tween(200),
        label = "backgroundColor"
    )
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.sm)
            .clip(RoundedCornerShape(Radius.xl))
            .background(backgroundColor)
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(Radius.xl)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                focusRequester.requestFocus()
            }
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = if (isFocused) Colors.HasselbladOrange else Colors.OnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused },
                textStyle = TextStyle(
                    color = Colors.OnSurface,
                    fontSize = Typography.BodyMedium.fontSize
                ),
                singleLine = true,
                cursorBrush = SolidColor(Colors.HasselbladOrange),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch()
                        focusManager.clearFocus()
                    }
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = Typography.BodyMedium,
                                color = Colors.OnSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )
            
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "清空",
                    tint = Colors.OnSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onClearQuery() }
                )
            }
        }
    }
}

@Composable
fun GlassAnimatedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索预设...",
    delayMillis: Int = 0
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        isVisible = true
    }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Decelerate
        ),
        label = "alpha"
    )
    
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 20f,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Decelerate
        ),
        label = "offsetY"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffsetY
            }
    ) {
        GlassSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onClearQuery = onClearQuery,
            placeholder = placeholder
        )
    }
}

@Composable
fun GlassSearchBarWithVoice(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索预设..."
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Colors.HasselbladOrange.copy(alpha = 0.6f) else Colors.GlassBorder.copy(alpha = 0.3f),
        animationSpec = tween(200),
        label = "borderColor"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isFocused) Colors.GlassBackground.copy(alpha = 0.35f) else Colors.GlassBackground.copy(alpha = 0.2f),
        animationSpec = tween(200),
        label = "backgroundColor"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.sm)
            .clip(RoundedCornerShape(Radius.xl))
            .background(backgroundColor)
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(Radius.xl)
            )
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = if (isFocused) Colors.HasselbladOrange else Colors.OnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = Colors.OnSurface,
                    fontSize = Typography.BodyMedium.fontSize
                ),
                singleLine = true,
                cursorBrush = SolidColor(Colors.HasselbladOrange),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = Typography.BodyMedium,
                                color = Colors.OnSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )
            
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "清空",
                    tint = Colors.OnSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onClearQuery() }
                )
            }
            
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Mic,
                contentDescription = "语音",
                tint = Colors.OnSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onVoiceClick() }
            )
        }
    }
}
