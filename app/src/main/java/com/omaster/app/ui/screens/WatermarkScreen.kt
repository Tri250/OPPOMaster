package com.omaster.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.omaster.app.ui.animation.AnimationConfig
import com.omaster.app.ui.theme.*

enum class WatermarkPosition(val displayName: String) {
    TOP_LEFT("左上"),
    TOP_RIGHT("右上"),
    BOTTOM_LEFT("左下"),
    BOTTOM_RIGHT("右下"),
    CENTER("居中")
}

enum class WatermarkFont(val displayName: String) {
    SANS("思源黑体"),
    SERIF("思源宋体"),
    MONOSPACE("等宽字体")
}

data class WatermarkTemplate(
    val id: Int,
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val defaultText: String,
    val fontSize: Float = 32f,
    val color: Color = OppoWhite,
    val opacity: Float = 0.8f,
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    val rotation: Float = 0f,
    val font: WatermarkFont = WatermarkFont.SANS
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WatermarkScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val selectedTemplateId = remember { mutableStateOf<Int?>(null) }
    
    val watermarkText = remember { mutableStateOf("小O帮帮") }
    val selectedFont = remember { mutableStateOf(WatermarkFont.SANS) }
    val fontSize = remember { mutableStateOf(32f) }
    val watermarkColor = remember { mutableStateOf(OppoWhite) }
    val opacity = remember { mutableStateOf(80f) }
    val selectedPosition = remember { mutableStateOf(WatermarkPosition.BOTTOM_RIGHT) }
    val rotation = remember { mutableStateOf(0f) }
    
    val templates = remember {
        listOf(
            WatermarkTemplate(1, "小O帮帮", Icons.Default.WaterDrop, "小O帮帮", color = OppoSunriseGold),
            WatermarkTemplate(2, "摄影工作室", Icons.Default.Camera, "光影工作室"),
            WatermarkTemplate(3, "旅行日记", Icons.Default.Map, "旅行日记", rotation = -15f),
            WatermarkTemplate(4, "美食记录", Icons.Default.Restaurant, "美食日记"),
            WatermarkTemplate(5, "人像摄影", Icons.Default.Person, "Portrait", font = WatermarkFont.SERIF),
            WatermarkTemplate(6, "风景摄影", Icons.Default.Mountain, "Landscape"),
            WatermarkTemplate(7, "建筑摄影", Icons.Default.Building, "Architecture", rotation = -5f),
            WatermarkTemplate(8, "微距摄影", Icons.Default.Flower2, "Macro", fontSize = 28f),
            WatermarkTemplate(9, "宠物摄影", Icons.Default.Pets, "My Pet", color = HasselbladOrange),
            WatermarkTemplate(10, "自定义模板", Icons.Default.Edit, "自定义")
        )
    }
    
    val availableColors = listOf(
        OppoWhite, OppoSunriseGold, HasselbladOrange, Success, Info, Warning, Error
    )
    
    fun applyTemplate(template: WatermarkTemplate) {
        selectedTemplateId.value = template.id
        watermarkText.value = template.defaultText
        selectedFont.value = template.font
        fontSize.value = template.fontSize
        watermarkColor.value = template.color
        opacity.value = (template.opacity * 100)
        selectedPosition.value = template.position
        rotation.value = template.rotation
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            WatermarkTopBar(onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            WatermarkTemplateGrid(
                templates = templates,
                selectedId = selectedTemplateId.value,
                onTemplateSelected = { applyTemplate(it) }
            )
            
            WatermarkPreview(
                text = watermarkText.value,
                fontSize = fontSize.value,
                color = watermarkColor.value,
                opacity = opacity.value / 100,
                position = selectedPosition.value,
                rotation = rotation.value
            )
            
            WatermarkEditor(
                text = watermarkText.value,
                onTextChange = { watermarkText.value = it },
                selectedFont = selectedFont.value,
                onFontChange = { selectedFont.value = it },
                fontSize = fontSize.value,
                onFontSizeChange = { fontSize.value = it },
                color = watermarkColor.value,
                onColorChange = { watermarkColor.value = it },
                availableColors = availableColors,
                opacity = opacity.value,
                onOpacityChange = { opacity.value = it },
                selectedPosition = selectedPosition.value,
                onPositionChange = { selectedPosition.value = it },
                rotation = rotation.value,
                onRotationChange = { rotation.value = it }
            )
            
            ActionButtons(
                onApplyClick = { /* Apply watermark to photos */ },
                onSaveTemplate = { /* Save template */ }
            )
            
            UsageInstructions()
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkTopBar(onBack: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = OppoSunriseGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "水印工具",
                                style = MaterialTheme.typography.headlineSmall,
                                color = OppoSunriseGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "自定义水印模板，一键应用到照片",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WatermarkTemplateGrid(
    templates: List<WatermarkTemplate>,
    selectedId: Int?,
    onTemplateSelected: (WatermarkTemplate) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(
            text = "选择模板",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(templates, key = { it.id }) { template ->
                val isSelected = selectedId == template.id
                
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.1f else 1f,
                    animationSpec = AnimationConfig.BouncySpringSpec,
                    label = "template_scale"
                )
                
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        AccentPrimary.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    animationSpec = tween(
                        durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                        easing = AnimationConfig.ColorOSDefaultEasing
                    ),
                    label = "template_bg"
                )
                
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) AccentPrimary else Color.Transparent,
                    animationSpec = tween(
                        durationMillis = AnimationConfig.STATE_TRANSITION_DURATION,
                        easing = AnimationConfig.ColorOSDefaultEasing
                    ),
                    label = "template_border"
                )
                
                Surface(
                    onClick = { onTemplateSelected(template) },
                    color = backgroundColor,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, borderColor),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = template.icon,
                            contentDescription = template.name,
                            tint = if (isSelected) AccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = template.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) AccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WatermarkPreview(
    text: String,
    fontSize: Float,
    color: Color,
    opacity: Float,
    position: WatermarkPosition,
    rotation: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "效果预览",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = "https://picsum.photos/800/600",
                    contentDescription = "预览背景",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = BitmapPainter(
                        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
                            eraseColor(MaterialTheme.colorScheme.surfaceVariant.toArgb())
                        }
                    )
                )
                
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = when (position) {
                        WatermarkPosition.TOP_LEFT -> Alignment.TopStart
                        WatermarkPosition.TOP_RIGHT -> Alignment.TopEnd
                        WatermarkPosition.BOTTOM_LEFT -> Alignment.BottomStart
                        WatermarkPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                        WatermarkPosition.CENTER -> Alignment.Center
                    }
                ) {
                    Text(
                        text = text,
                        fontSize = fontSize.sp,
                        color = color.copy(alpha = opacity),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(20.dp)
                            .graphicsLayer {
                                rotationZ = rotation
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun WatermarkEditor(
    text: String,
    onTextChange: (String) -> Unit,
    selectedFont: WatermarkFont,
    onFontChange: (WatermarkFont) -> Unit,
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    color: Color,
    onColorChange: (Color) -> Unit,
    availableColors: List<Color>,
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
    selectedPosition: WatermarkPosition,
    onPositionChange: (WatermarkPosition) -> Unit,
    rotation: Float,
    onRotationChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(
            text = "自定义编辑",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        TextInputField(
            label = "水印文字",
            value = text,
            onValueChange = onTextChange,
            placeholder = "输入水印文字"
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FontSelector(
            selectedFont = selectedFont,
            onFontSelected = onFontChange
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SliderControl(
            label = "字号",
            value = fontSize,
            onValueChange = onFontSizeChange,
            range = 12f..72f,
            steps = 60,
            valueFormatter = { "${it.toInt()}sp" }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ColorSelector(
            selectedColor = color,
            onColorSelected = onColorChange,
            availableColors = availableColors
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SliderControl(
            label = "透明度",
            value = opacity,
            onValueChange = onOpacityChange,
            range = 0f..100f,
            steps = 100,
            valueFormatter = { "${it.toInt()}%" }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        PositionSelector(
            selectedPosition = selectedPosition,
            onPositionSelected = onPositionChange
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SliderControl(
            label = "旋转角度",
            value = rotation,
            onValueChange = onRotationChange,
            range = -180f..180f,
            steps = 360,
            valueFormatter = { "${it.toInt()}°" }
        )
    }
}

@Composable
fun TextInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun FontSelector(
    selectedFont: WatermarkFont,
    onFontSelected: (WatermarkFont) -> Unit
) {
    Column {
        Text(
            text = "字体选择",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WatermarkFont.values().forEach { font ->
                val isSelected = selectedFont == font
                
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) AccentPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    animationSpec = tween(
                        durationMillis = AnimationConfig.STATE_TRANSITION_DURATION_FAST,
                        easing = AnimationConfig.ColorOSDefaultEasing
                    ),
                    label = "font_bg"
                )
                
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) AccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(
                        durationMillis = AnimationConfig.STATE_TRANSITION_DURATION_FAST,
                        easing = AnimationConfig.ColorOSDefaultEasing
                    ),
                    label = "font_content"
                )
                
                Surface(
                    onClick = { onFontSelected(font) },
                    color = backgroundColor,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = font.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SliderControl(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueFormatter: (Float) -> String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.labelMedium,
                color = AccentPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = AccentPrimary,
                activeTrackColor = AccentPrimary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ColorSelector(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    availableColors: List<Color>
) {
    Column {
        Text(
            text = "颜色选择",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            availableColors.forEach { color ->
                val isSelected = selectedColor == color
                
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1f,
                    animationSpec = AnimationConfig.BouncySpringSpec,
                    label = "color_scale"
                )
                
                Surface(
                    onClick = { onColorSelected(color) },
                    color = color,
                    shape = CircleShape,
                    border = if (isSelected) {
                        BorderStroke(3.dp, AccentPrimary)
                    } else {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                ) {}
            }
        }
    }
}

@Composable
fun PositionSelector(
    selectedPosition: WatermarkPosition,
    onPositionSelected: (WatermarkPosition) -> Unit
) {
    Column {
        Text(
            text = "位置选择",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WatermarkPosition.values().forEach { position ->
                val isSelected = selectedPosition == position
                
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) AccentPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    animationSpec = tween(
                        durationMillis = AnimationConfig.STATE_TRANSITION_DURATION_FAST,
                        easing = AnimationConfig.ColorOSDefaultEasing
                    ),
                    label = "position_bg"
                )
                
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) AccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(
                        durationMillis = AnimationConfig.STATE_TRANSITION_DURATION_FAST,
                        easing = AnimationConfig.ColorOSDefaultEasing
                    ),
                    label = "position_icon"
                )
                
                Surface(
                    onClick = { onPositionSelected(position) },
                    color = backgroundColor,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = when (position) {
                            WatermarkPosition.TOP_LEFT -> Alignment.TopStart
                            WatermarkPosition.TOP_RIGHT -> Alignment.TopEnd
                            WatermarkPosition.BOTTOM_LEFT -> Alignment.BottomStart
                            WatermarkPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                            WatermarkPosition.CENTER -> Alignment.Center
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = position.displayName,
                            tint = iconColor,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButtons(
    onApplyClick: () -> Unit,
    onSaveTemplate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.98f else 1f,
            animationSpec = AnimationConfig.SoftSpringSpec,
            label = "apply_btn_scale"
        )
        
        Button(
            onClick = onApplyClick,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "应用水印到照片",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        OutlinedButton(
            onClick = onSaveTemplate,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "保存模板",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun UsageInstructions() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(
            text = "使用说明",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                val instructions = listOf(
                    "选择或自定义水印模板" to Icons.Default.SelectAll,
                    "调整水印参数（文字、字体、颜色等）" to Icons.Default.Settings,
                    "一键应用到照片" to Icons.Default.PhotoCamera
                )
                
                instructions.forEachIndexed { index, (text, icon) ->
                    Row(
                        modifier = Modifier.padding(bottom = if (index < instructions.size - 1) 12.dp else 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AccentPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = AccentPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "${index + 1}. $text",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}