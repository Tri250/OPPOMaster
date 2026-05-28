package com.omaster.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omaster.app.ui.animation.AnimationConfig
import com.omaster.app.ui.theme.*

// ==================== ColorOS 16 专家级搜索栏 ====================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ColorOSSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    
    val suggestions = remember {
        listOf("人像", "风景", "夜景", "美食", "街拍", "哈苏", "自然", "城市", "建筑")
    }
    
    val animatedElevation by animateDpAsState(
        targetValue = if (isFocused) 8.dp else 2.dp,
        animationSpec = tween(
            durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
            easing = AnimationConfig.ColorOSDefaultEasing
        ),
        label = "elevation"
    )
    
    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isFocused) 1.5.dp else 0.5.dp,
        animationSpec = tween(
            durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
            easing = AnimationConfig.ColorOSDefaultEasing
        ),
        label = "border"
    )
    
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isFocused) AccentPrimary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(
            durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
            easing = AnimationConfig.ColorOSDefaultEasing
        ),
        label = "borderColor"
    )
    
    Column(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
            border = BorderStroke(animatedBorderWidth, animatedBorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 搜索图标
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = if (isFocused) AccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                
                // 输入框
                TextField(
                    value = query,
                    onValueChange = { 
                        onQueryChange(it)
                        isExpanded = it.isEmpty()
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "搜索预设、场景、风格...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    maxLines = 1,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { onSearch() }
                    )
                )
                
                // 操作按钮
                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    IconButton(
                        onClick = {
                            onClearQuery()
                            isExpanded = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "清空",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                AnimatedVisibility(
                    visible = query.isEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "推荐",
                            tint = AccentPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        
        // 推荐搜索词
        AnimatedVisibility(
            visible = isExpanded && query.isEmpty(),
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                    delayMillis = 50
                )
            ) + slideInVertically(
                initialOffsetY = { -20 },
                animationSpec = tween(
                    durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                    easing = AnimationConfig.ColorOSDecelerateEasing
                )
            ),
            exit = fadeOut() + slideOutVertically()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(suggestions) { suggestion ->
                    ColorOSSuggestionChip(
                        text = suggestion,
                        onClick = {
                            onQueryChange(suggestion)
                            isExpanded = false
                        }
                    )
                }
            }
        }
    }
}

// ==================== ColorOS 16 推荐词Chip ====================
@Composable
private fun ColorOSSuggestionChip(
    text: String,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = 0.9f,
            stiffness = 500f
        ),
        label = "chipScale"
    )
    
    Surface(
        modifier = Modifier.scale(scale),
        onClick = onClick,
        shape = RoundedCornerShape(100.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource().also { interactionSource ->
            LaunchedEffect(Unit) {
                interactionSource.interactions.collect { interaction ->
                    if (interaction is androidx.compose.foundation.interaction.PressInteraction.Press) {
                        isPressed = true
                    } else if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release ||
                        interaction is androidx.compose.foundation.interaction.PressInteraction.Cancel
                    ) {
                        isPressed = false
                    }
                }
            }
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = AccentPrimary.copy(alpha = 0.7f)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== ColorOS 16 筛选栏 ====================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColorOSFilterBar(
    selectedFilter: com.omaster.app.viewmodel.FilterType,
    onFilterSelected: (com.omaster.app.viewmodel.FilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = remember {
        listOf(
            FilterInfo(com.omaster.app.viewmodel.FilterType.ALL, "全部", Icons.Default.Apps),
            FilterInfo(com.omaster.app.viewmodel.FilterType.FAVORITES, "收藏", Icons.Default.Favorite),
            FilterInfo(com.omaster.app.viewmodel.FilterType.HNCS, "HNCS", Icons.Default.Star),
            FilterInfo(com.omaster.app.viewmodel.FilterType.FIND_X, "Find X", Icons.Default.PhoneAndroid),
            FilterInfo(com.omaster.app.viewmodel.FilterType.RENO, "Reno", Icons.Default.PhoneAndroid),
            FilterInfo(com.omaster.app.viewmodel.FilterType.NEW, "最新", Icons.Default.NewReleases),
            FilterInfo(com.omaster.app.viewmodel.FilterType.TRENDING, "热门", Icons.Default.FireExtinguisher)
        )
    }
    
    val scrollState = rememberScrollState()
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            filters.forEachIndexed { index, filterInfo ->
                val isSelected = selectedFilter == filterInfo.type
                ColorOSFilterChip(
                    filterInfo = filterInfo,
                    selected = isSelected,
                    onClick = { onFilterSelected(filterInfo.type) },
                    index = index
                )
            }
        }
        
        // ColorOS 16 滚动指示器
        AnimatedVisibility(
            visible = scrollState.canScrollForward || scrollState.canScrollBackward,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.width(40.dp).height(4.dp)
                ) {}
            }
        }
    }
}

// ==================== ColorOS 16 筛选Chip ====================
@Composable
private fun ColorOSFilterChip(
    filterInfo: FilterInfo,
    selected: Boolean,
    onClick: () -> Unit,
    index: Int
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val animatedContainerColor by animateColorAsState(
        targetValue = if (selected) AccentPrimary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(
            durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
            easing = AnimationConfig.ColorOSDefaultEasing
        ),
        label = "containerColor"
    )
    
    val animatedContentColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(
            durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
            easing = AnimationConfig.ColorOSDefaultEasing
        ),
        label = "contentColor"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = 0.9f,
            stiffness = 500f
        ),
        label = "chipScale"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (selected) 4.dp else 0.dp,
        animationSpec = tween(
            durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
            easing = AnimationConfig.ColorOSDefaultEasing
        ),
        label = "elevation"
    )
    
    Surface(
        modifier = Modifier.scale(scale),
        onClick = onClick,
        shape = RoundedCornerShape(100.dp),
        color = animatedContainerColor,
        shadowElevation = elevation,
        border = if (!selected) {
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        } else null,
        interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource().also { interactionSource ->
            LaunchedEffect(Unit) {
                interactionSource.interactions.collect { interaction ->
                    if (interaction is androidx.compose.foundation.interaction.PressInteraction.Press) {
                        isPressed = true
                    } else if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release ||
                        interaction is androidx.compose.foundation.interaction.PressInteraction.Cancel
                    ) {
                        isPressed = false
                    }
                }
            }
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = filterInfo.icon,
                contentDescription = filterInfo.label,
                modifier = Modifier.size(18.dp),
                tint = animatedContentColor
            )
            Text(
                text = filterInfo.label,
                style = MaterialTheme.typography.labelMedium,
                color = animatedContentColor,
                fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
            )
        }
    }
}

private data class FilterInfo(
    val type: com.omaster.app.viewmodel.FilterType,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

// ==================== ColorOS 16 骨架屏卡片 ====================
@Composable
fun ColorOSSkeletonCard(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AnimationConfig.SKELETON_SWEEP_DURATION,
                easing = CubicBezierEasing(0.4f, 0.0f, 0.6f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .alpha(alpha)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(24.dp)
                        .alpha(alpha)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(28.dp)
                        .alpha(alpha)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(100.dp)
                        )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(18.dp)
                        .alpha(alpha)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                )
            }
        }
    }
}

// ==================== ColorOS 16 空状态 ====================
@Composable
fun ColorOSEmptyState(
    message: String,
    isSearchEmpty: Boolean = false,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                    delayMillis = 100,
                    easing = AnimationConfig.ColorOSDecelerateEasing
                )
            ) + slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = tween(
                    durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                    delayMillis = 100,
                    easing = AnimationConfig.ColorOSDecelerateEasing
                )
            )
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSearchEmpty) Icons.Default.ImageNotSupported else Icons.Default.SearchOff,
                        contentDescription = if (isSearchEmpty) "暂无预设" else "搜索无结果",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                    delayMillis = 200,
                    easing = AnimationConfig.ColorOSDecelerateEasing
                )
            ) + slideInVertically(
                initialOffsetY = { 20 },
                animationSpec = tween(
                    durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                    delayMillis = 200,
                    easing = AnimationConfig.ColorOSDecelerateEasing
                )
            )
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                    delayMillis = 300,
                    easing = AnimationConfig.ColorOSDecelerateEasing
                )
            ) + slideInVertically(
                initialOffsetY = { 15 },
                animationSpec = tween(
                    durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                    delayMillis = 300,
                    easing = AnimationConfig.ColorOSDecelerateEasing
                )
            )
        ) {
            Text(
                text = if (isSearchEmpty) "期待更多精彩预设" else "换个关键词试试",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
