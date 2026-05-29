package com.omaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omaster.app.model.CameraParamConstants
import com.omaster.app.model.CameraParams
import com.omaster.app.model.PresetEditorState
import com.omaster.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetEditorScreen(
    onBack: () -> Unit,
    onSave: (PresetEditorState) -> Unit,
    initialState: PresetEditorState = PresetEditorState()
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600
    
    val editorState = remember { mutableStateOf(initialState) }
    val paramsState = remember { mutableStateOf(CameraParams()) }
    val isEditing = remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预设编辑器") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isEditing.value = true
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "保存草稿")
                    }
                    IconButton(onClick = {
                        onSave(editorState.value)
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "完成")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceElevated,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = Surface
    ) { paddingValues ->
        if (isTablet) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                EditorPreviewSection(
                    modifier = Modifier.weight(1f),
                    params = paramsState.value
                )
                EditorControlsSection(
                    modifier = Modifier.weight(1f),
                    paramsState = paramsState,
                    editorState = editorState
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                EditorPreviewSection(
                    modifier = Modifier.height(280.dp),
                    params = paramsState.value
                )
                EditorControlsSection(
                    modifier = Modifier.weight(1f),
                    paramsState = paramsState,
                    editorState = editorState
                )
            }
        }
    }
}

@Composable
fun EditorPreviewSection(
    modifier: Modifier = Modifier,
    params: CameraParams
) {
    var imageSize by remember { mutableStateOf(DpSize.Zero) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceElevated)
                .onSizeChanged { size ->
                    imageSize = DpSize(size.width.dp, size.height.dp)
                },
            contentAlignment = Alignment.Center
        ) {
            ImagePreview(params = params)
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ParamBadge("ISO ${params.iso ?: "100"}")
                    ParamBadge("${params.shutter ?: "1/30"}s")
                    ParamBadge("EV ${params.ev ?: "0"}")
                    ParamBadge("WB ${params.wb ?: "Auto"}")
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {}) {
                Text("原图", fontSize = 14.sp)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(OppoOrange)
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = "对比",
                    fontSize = 12.sp,
                    color = DeepSpace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxSize(),
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = {}) {
                Text("效果", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ParamBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(6.dp)
    ) {
        Text(text, fontSize = 12.sp, color = Color.White)
    }
}

@Composable
fun ImagePreview(params: CameraParams) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(OppoOrange.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text("📷", fontSize = 48.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "预览区域",
            color = TextTertiary,
            fontSize = 14.sp
        )
        Text(
            text = "点击导入照片",
            color = TextTertiary,
            fontSize = 12.sp
        )
    }
}

@Composable
fun EditorControlsSection(
    modifier: Modifier = Modifier,
    paramsState: MutableState<CameraParams>,
    editorState: MutableState<PresetEditorState>
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // 预设名称
        OutlinedTextField(
            value = editorState.value.name,
            onValueChange = { editorState.value.name = it },
            label = { Text("预设名称") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OppoOrange,
                unfocusedBorderColor = BorderSubtle
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 描述
        BasicTextField(
            value = editorState.value.description,
            onValueChange = { editorState.value.description = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .padding(12.dp),
            decorationBox = { innerTextField ->
                if (editorState.value.description.isEmpty()) {
                    Text("添加描述（可选）", color = TextTertiary)
                }
                innerTextField()
            }
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // 参数分类
        ParamSection(
            title = "曝光",
            icon = "📷",
            params = CameraParamConstants.EXPOSURE_PARAMS,
            paramsState = paramsState
        )
        
        ParamSection(
            title = "白平衡",
            icon = "⚖️",
            params = CameraParamConstants.WHITE_BALANCE_PARAMS,
            paramsState = paramsState
        )
        
        ParamSection(
            title = "HSL",
            icon = "🌈",
            params = CameraParamConstants.HSL_PARAMS,
            paramsState = paramsState
        )
        
        ParamSection(
            title = "细节",
            icon = "✨",
            params = CameraParamConstants.DETAIL_PARAMS,
            paramsState = paramsState
        )
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ParamSection(
    title: String,
    icon: String,
    params: List<com.omaster.app.model.EditorParam>,
    paramsState: MutableState<CameraParams>
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { isExpanded = !isExpanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .rotation(if (isExpanded) 90f else 0f)
            )
        }
        
        if (isExpanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                params.forEach { param ->
                    ParamSlider(
                        param = param,
                        paramsState = paramsState
                    )
                }
            }
        }
    }
}

@Composable
fun ParamSlider(
    param: com.omaster.app.model.EditorParam,
    paramsState: MutableState<CameraParams>
) {
    val value = remember { mutableStateOf(param.defaultValue) }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                param.displayName,
                fontSize = 14.sp,
                color = TextSecondary
            )
            Text(
                "${value.value.toInt()}${param.unit}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = OppoOrange
            )
        }
        
        Slider(
            value = value.value,
            onValueChange = {
                value.value = it
                updateParamsState(param.key, it, paramsState)
            },
            valueRange = param.min..param.max,
            steps = ((param.max - param.min) / param.step).toInt(),
            colors = SliderDefaults.colors(
                thumbColor = OppoOrange,
                activeTrackColor = OppoOrange,
                inactiveTrackColor = SurfaceHover
            )
        )
    }
}

fun updateParamsState(
    key: String,
    value: Float,
    paramsState: MutableState<CameraParams>
) {
    when (key) {
        "ev" -> paramsState.value.ev = value.toString()
        "iso" -> paramsState.value.iso = value.toInt().toString()
        "shutter" -> paramsState.value.shutter = value.toString()
        "colorTemp" -> paramsState.value.colorTemp = value
        "tint" -> paramsState.value.tint = value
        "hue" -> paramsState.value.hue = value
        "saturation" -> paramsState.value.saturation = value
        "luminance" -> paramsState.value.luminance = value
        "contrast" -> paramsState.value.contrast = value
        "sharpness" -> paramsState.value.sharpness = value
        "vignette" -> paramsState.value.vignette = value
    }
}
