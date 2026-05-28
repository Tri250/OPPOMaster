package com.omaster.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.omaster.app.ui.animation.AnimationConfig
import com.omaster.app.ui.theme.*

@Composable
fun EnhancedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val suggestions = remember {
        listOf("人像", "风景", "夜景", "美食", "街拍", "哈苏", "自然", "城市")
    }
    
    val animatedBackgroundAlpha by animateFloatAsState(
        targetValue = when {
            isFocused -> 0.6f
            query.isNotEmpty() -> 0.4f
            else -> 0.35f
        },
        animationSpec = tween(
            durationMillis = AnimationConfig.SMALL_TRANSITION_DURATION,
            easing = AnimationConfig.DecelerateEasing
        ),
        label = "searchBarBackground"
    )
    
    val animatedBorderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(
            durationMillis = AnimationConfig.SMALL_TRANSITION_DURATION,
            easing = AnimationConfig.DecelerateEasing
        ),
        label = "searchBarBorder"
    )
    
    val animatedIconAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.7f,
        animationSpec = tween(
            durationMillis = AnimationConfig.SMALL_TRANSITION_DURATION,
            easing = AnimationConfig.DecelerateEasing
        ),
        label = "searchIconAlpha"
    )
    
    Box(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ColorOSOrange.copy(alpha = animatedBackgroundAlpha * 0.3f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = animatedBackgroundAlpha)
                        )
                    )
                )
                .then(
                    if (isFocused) {
                        Modifier.background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    ColorOSOrange.copy(alpha = 0.1f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        )
                    } else Modifier
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(22.dp),
                tint = ColorOSOrange.copy(alpha = animatedIconAlpha)
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = { onQueryChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    decorationBox = { innerTextField ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    text = "搜索预设、场景、风格...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
            
            AnimatedContent(
                targetState = query.isNotEmpty(),
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) togetherWith
                    fadeOut(animationSpec = tween(150))
                },
                label = "clearButton"
            ) { showClear ->
                if (showClear) {
                    IconButton(
                        onClick = {
                            onClearQuery()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清空",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "相机",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = isFocused,
            enter = fadeIn(animationSpec = tween(200)) + expandVertically(),
            exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Surface(
                modifier = Modifier
                    .padding(top = 56.dp, end = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Text(
                    text = "按 Enter 搜索",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    AnimatedVisibility(
        visible = query.isEmpty(),
        enter = fadeIn(animationSpec = tween(200)) + slideInVertically(
            initialOffsetY = { -20 },
            animationSpec = tween(200, easing = AnimationConfig.DecelerateEasing)
        ),
        exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(
            targetOffsetY = { -20 },
            animationSpec = tween(150)
        )
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(suggestions) { suggestion ->
                ColorOSSuggestionChip(
                    suggestion = suggestion,
                    onClick = { onQueryChange(suggestion) }
                )
            }
        }
    }
}

@Composable
private fun ColorOSSuggestionChip(
    suggestion: String,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "suggestionScale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(100),
        label = "suggestionBackground"
    )
    
    Surface(
        modifier = Modifier
            .scale(scale)
            .height(32.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        onClick = onClick
    ) {
        Text(
            text = suggestion,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
