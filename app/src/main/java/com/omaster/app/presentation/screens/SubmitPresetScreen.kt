package com.omaster.app.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.omaster.app.data.CommunityRepository
import com.omaster.app.domain.model.SubmissionRequest
import com.omaster.app.domain.model.CameraParams
import com.omaster.app.domain.model.ColorStyle
import com.omaster.app.presentation.theme.*
import kotlinx.coroutines.launch

/**
 * 投稿页面
 * 用于用户提交新的预设投稿
 * 包含预设信息填写、样张上传、参数设置、提交审核功能
 *
 * @param onBackClick 返回按钮回调
 * @param onSubmitSuccess 投稿成功回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitPresetScreen(
    onBackClick: () -> Unit,
    onSubmitSuccess: () -> Unit,
    viewModel: SubmitPresetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // 提交成功处理
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onSubmitSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("投稿预设", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // 保存草稿按钮
                    TextButton(
                        onClick = { viewModel.saveDraft() },
                        enabled = !uiState.isLoading
                    ) {
                        Text("保存草稿", color = HasselbladOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSpace
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DeepSpace)
                .verticalScroll(scrollState)
        ) {
            // 样张上传区域
            SampleImagesSection(
                images = uiState.sampleImages,
                onAddImage = { viewModel.addSampleImage(it) },
                onRemoveImage = { viewModel.removeSampleImage(it) },
                maxImages = 9
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 预设信息表单
            PresetInfoForm(
                presetName = uiState.presetName,
                onPresetNameChange = viewModel::updatePresetName,
                description = uiState.description,
                onDescriptionChange = viewModel::updateDescription,
                deviceModel = uiState.deviceModel,
                onDeviceModelChange = viewModel::updateDeviceModel,
                selectedTags = uiState.tags,
                onTagToggle = viewModel::toggleTag,
                availableTags = viewModel.availableTags
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 相机参数设置
            CameraParamsSection(
                cameraParams = uiState.cameraParams,
                onParamsChange = viewModel::updateCameraParams,
                selectedColorStyle = uiState.colorStyle,
                onColorStyleChange = viewModel::updateColorStyle
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 提交按钮
            SubmitButton(
                onClick = { viewModel.submitPreset() },
                isLoading = uiState.isLoading,
                isValid = uiState.isValid(),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // 错误提示
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                uiState.errorMessage?.let { error ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = ErrorPro.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = ErrorPro
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = ErrorPro,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 样张上传区域
 */
@Composable
private fun SampleImagesSection(
    images: List<String>,
    onAddImage: (String) -> Unit,
    onRemoveImage: (String) -> Unit,
    maxImages: Int = 9
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "样张图片",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = "${images.size}/$maxImages",
                fontSize = 14.sp,
                color = ColorOSTextTertiary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 图片网格
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 已上传的图片
            images.forEach { imageUrl ->
                SampleImageItem(
                    imageUrl = imageUrl,
                    onRemove = { onRemoveImage(imageUrl) }
                )
            }

            // 添加图片按钮
            if (images.size < maxImages) {
                AddImageButton(onClick = {
                    // 模拟添加图片，实际应打开图片选择器
                    onAddImage("https://picsum.photos/400/400?random=${System.currentTimeMillis()}")
                })
            }
        }

        if (images.isEmpty()) {
            Text(
                text = "请上传至少1张样张图片，最多$maxImages 张",
                fontSize = 12.sp,
                color = ColorOSTextTertiary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * 样张图片项
 */
@Composable
private fun SampleImageItem(
    imageUrl: String,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier.size(100.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        // 删除按钮
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "删除",
                modifier = Modifier
                    .size(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                tint = Color.White
            )
        }
    }
}

/**
 * 添加图片按钮
 */
@Composable
private fun AddImageButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ColorOSGrey800)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = "添加图片",
                modifier = Modifier.size(32.dp),
                tint = HasselbladOrange
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "添加图片",
                fontSize = 12.sp,
                color = ColorOSTextTertiary
            )
        }
    }
}

/**
 * 预设信息表单
 */
@Composable
private fun PresetInfoForm(
    presetName: String,
    onPresetNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    deviceModel: String,
    onDeviceModelChange: (String) -> Unit,
    selectedTags: List<String>,
    onTagToggle: (String) -> Unit,
    availableTags: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "预设信息",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 预设名称
        OutlinedTextField(
            value = presetName,
            onValueChange = onPresetNameChange,
            label = { Text("预设名称 *", color = ColorOSTextTertiary) },
            placeholder = { Text("给你的预设起个名字", color = ColorOSTextQuaternary) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HasselbladOrange,
                unfocusedBorderColor = ColorOSBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = ColorOSTextTertiary
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 设备型号
        OutlinedTextField(
            value = deviceModel,
            onValueChange = onDeviceModelChange,
            label = { Text("适用设备", color = ColorOSTextTertiary) },
            placeholder = { Text("如：OPPO Find X8 Ultra", color = ColorOSTextQuaternary) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HasselbladOrange,
                unfocusedBorderColor = ColorOSBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = ColorOSTextTertiary
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 预设描述
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("预设描述 *", color = ColorOSTextTertiary) },
            placeholder = { Text("描述一下这个预设的特点、适用场景...", color = ColorOSTextQuaternary) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HasselbladOrange,
                unfocusedBorderColor = ColorOSBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = ColorOSTextTertiary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 标签选择
        Text(
            text = "标签",
            fontSize = 14.sp,
            color = ColorOSTextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableTags.forEach { tag ->
                val isSelected = selectedTags.contains(tag)
                FilterChip(
                    selected = isSelected,
                    onClick = { onTagToggle(tag) },
                    label = { Text(tag) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HasselbladOrange,
                        selectedLabelColor = Color.Black,
                        containerColor = ColorOSGrey800,
                        labelColor = ColorOSTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = ColorOSBorder
                    )
                )
            }
        }
    }
}

/**
 * 相机参数设置区域
 */
@Composable
private fun CameraParamsSection(
    cameraParams: CameraParams?,
    onParamsChange: (CameraParams) -> Unit,
    selectedColorStyle: String,
    onColorStyleChange: (String) -> Unit
) {
    var expandedSection by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "相机参数",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 色彩风格选择
        Text(
            text = "色彩风格",
            fontSize = 14.sp,
            color = ColorOSTextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ColorStyle.entries.toTypedArray().toList()) { style ->
                val isSelected = selectedColorStyle == style.name
                FilterChip(
                    selected = isSelected,
                    onClick = { onColorStyleChange(style.name) },
                    label = { Text(style.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HasselbladOrange,
                        selectedLabelColor = Color.Black,
                        containerColor = ColorOSGrey800,
                        labelColor = ColorOSTextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 基本参数设置
        val params = cameraParams ?: CameraParams()

        // ISO
        ParameterSlider(
            label = "ISO",
            value = params.iso?.toFloat() ?: 100f,
            onValueChange = {
                onParamsChange(params.copy(iso = it.toInt()))
            },
            valueRange = 50f..12800f,
            steps = 127
        )

        // 锐度
        ParameterSlider(
            label = "锐度",
            value = params.sharpness?.toFloat() ?: 50f,
            onValueChange = {
                onParamsChange(params.copy(sharpness = it.toInt()))
            },
            valueRange = 0f..100f,
            steps = 99
        )

        // 对比度
        ParameterSlider(
            label = "对比度",
            value = params.contrast?.toFloat() ?: 50f,
            onValueChange = {
                onParamsChange(params.copy(contrast = it.toInt()))
            },
            valueRange = 0f..100f,
            steps = 99
        )

        // 饱和度
        ParameterSlider(
            label = "饱和度",
            value = params.saturation?.toFloat() ?: 50f,
            onValueChange = {
                onParamsChange(params.copy(saturation = it.toInt()))
            },
            valueRange = 0f..100f,
            steps = 99
        )

        // 高级参数折叠区域
        Spacer(modifier = Modifier.height(8.dp))

        AdvancedParamsSection(
            cameraParams = params,
            onParamsChange = onParamsChange
        )
    }
}

/**
 * 参数滑块
 */
@Composable
private fun ParameterSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = ColorOSTextSecondary
            )
            Text(
                text = "${value.toInt()}",
                fontSize = 14.sp,
                color = HasselbladOrange
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = HasselbladOrange,
                activeTrackColor = HasselbladOrange,
                inactiveTrackColor = ColorOSGrey700
            )
        )
    }
}

/**
 * 高级参数区域
 */
@Composable
private fun AdvancedParamsSection(
    cameraParams: CameraParams,
    onParamsChange: (CameraParams) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "高级参数",
                fontSize = 14.sp,
                color = ColorOSTextSecondary
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = ColorOSTextTertiary
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                // 拍摄模式
                OutlinedTextField(
                    value = cameraParams.mode ?: "",
                    onValueChange = { onParamsChange(cameraParams.copy(mode = it)) },
                    label = { Text("拍摄模式", color = ColorOSTextTertiary) },
                    placeholder = { Text("如：人像模式、专业模式", color = ColorOSTextQuaternary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HasselbladOrange,
                        unfocusedBorderColor = ColorOSBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 滤镜
                OutlinedTextField(
                    value = cameraParams.filter ?: "",
                    onValueChange = { onParamsChange(cameraParams.copy(filter = it)) },
                    label = { Text("滤镜", color = ColorOSTextTertiary) },
                    placeholder = { Text("如：自然、鲜艳、黑白", color = ColorOSTextQuaternary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HasselbladOrange,
                        unfocusedBorderColor = ColorOSBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 快门速度
                OutlinedTextField(
                    value = cameraParams.shutter ?: "",
                    onValueChange = { onParamsChange(cameraParams.copy(shutter = it)) },
                    label = { Text("快门速度", color = ColorOSTextTertiary) },
                    placeholder = { Text("如：1/200", color = ColorOSTextQuaternary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HasselbladOrange,
                        unfocusedBorderColor = ColorOSBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 白平衡
                OutlinedTextField(
                    value = cameraParams.wb ?: "",
                    onValueChange = { onParamsChange(cameraParams.copy(wb = it)) },
                    label = { Text("白平衡", color = ColorOSTextTertiary) },
                    placeholder = { Text("如：5500K", color = ColorOSTextQuaternary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HasselbladOrange,
                        unfocusedBorderColor = ColorOSBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // EV曝光补偿
                OutlinedTextField(
                    value = cameraParams.ev ?: "",
                    onValueChange = { onParamsChange(cameraParams.copy(ev = it)) },
                    label = { Text("曝光补偿", color = ColorOSTextTertiary) },
                    placeholder = { Text("如：+0.3", color = ColorOSTextQuaternary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HasselbladOrange,
                        unfocusedBorderColor = ColorOSBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        }
    }
}

/**
 * 提交按钮
 */
@Composable
private fun SubmitButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    isValid: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isValid) 1f else 0.98f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale),
        enabled = isValid && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = HasselbladOrange,
            contentColor = Color.Black,
            disabledContainerColor = ColorOSGrey700,
            disabledContentColor = ColorOSTextTertiary
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.Black,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.Default.Upload,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "提交审核",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * FlowRow 布局组件
 */
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val hGapPx = 8.dp.roundToPx()
        val vGapPx = 8.dp.roundToPx()

        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        val rowWidths = mutableListOf<Int>()
        val rowHeights = mutableListOf<Int>()

        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        var currentRowHeight = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)

            if (currentRow.isNotEmpty() &&
                currentRowWidth + hGapPx + placeable.width > constraints.maxWidth
            ) {
                rows.add(currentRow)
                rowWidths.add(currentRowWidth)
                rowHeights.add(currentRowHeight)
                currentRow = mutableListOf()
                currentRowWidth = 0
                currentRowHeight = 0
            }

            currentRow.add(placeable)
            currentRowWidth += if (currentRow.size == 1) placeable.width else hGapPx + placeable.width
            currentRowHeight = maxOf(currentRowHeight, placeable.height)
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            rowWidths.add(currentRowWidth)
            rowHeights.add(currentRowHeight)
        }

        val width = constraints.maxWidth
        val height = rowHeights.sum() + (rowHeights.size - 1).coerceAtLeast(0) * vGapPx

        androidx.compose.ui.layout.layout(width, height) {
            var y = 0
            rows.forEachIndexed { rowIndex, row ->
                var x = when (horizontalArrangement) {
                    Arrangement.End -> width - rowWidths[rowIndex]
                    Arrangement.Center -> (width - rowWidths[rowIndex]) / 2
                    else -> 0
                }

                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + hGapPx
                }

                y += rowHeights[rowIndex] + vGapPx
            }
        }
    }
}

/**
 * 投稿页面 UI 状态
 */
data class SubmitPresetUiState(
    val presetName: String = "",
    val description: String = "",
    val deviceModel: String = "",
    val sampleImages: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val cameraParams: CameraParams? = null,
    val colorStyle: String = ColorStyle.Natural.name,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    fun isValid(): Boolean {
        return presetName.isNotBlank() &&
               description.isNotBlank() &&
               sampleImages.isNotEmpty()
    }
}

/**
 * 投稿页面 ViewModel
 */
@ androidx.lifecycle.ViewModel
class SubmitPresetViewModel @javax.inject.Inject constructor(
    private val communityRepository: CommunityRepository
) : androidx.lifecycle.ViewModel() {

    private val _uiState = MutableStateFlow(SubmitPresetUiState())
    val uiState: StateFlow<SubmitPresetUiState> = _uiState.asStateFlow()

    // 可用标签列表
    val availableTags = listOf(
        "人像", "风景", "夜景", "街拍", "美食",
        "胶片", "复古", "清新", "黑白", "日系",
        "赛博朋克", "自然", "城市", "旅行", "宠物"
    )

    fun updatePresetName(name: String) {
        _uiState.value = _uiState.value.copy(presetName = name, errorMessage = null)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description, errorMessage = null)
    }

    fun updateDeviceModel(deviceModel: String) {
        _uiState.value = _uiState.value.copy(deviceModel = deviceModel)
    }

    fun addSampleImage(imageUrl: String) {
        val currentImages = _uiState.value.sampleImages
        if (currentImages.size < 9) {
            _uiState.value = _uiState.value.copy(
                sampleImages = currentImages + imageUrl,
                errorMessage = null
            )
        }
    }

    fun removeSampleImage(imageUrl: String) {
        _uiState.value = _uiState.value.copy(
            sampleImages = _uiState.value.sampleImages.filter { it != imageUrl }
        )
    }

    fun toggleTag(tag: String) {
        val currentTags = _uiState.value.tags
        _uiState.value = _uiState.value.copy(
            tags = if (currentTags.contains(tag)) {
                currentTags - tag
            } else {
                currentTags + tag
            }
        )
    }

    fun updateCameraParams(params: CameraParams) {
        _uiState.value = _uiState.value.copy(cameraParams = params)
    }

    fun updateColorStyle(style: String) {
        _uiState.value = _uiState.value.copy(colorStyle = style)
    }

    fun submitPreset() {
        val state = _uiState.value

        if (!state.isValid()) {
            _uiState.value = state.copy(
                errorMessage = when {
                    state.presetName.isBlank() -> "请输入预设名称"
                    state.description.isBlank() -> "请输入预设描述"
                    state.sampleImages.isEmpty() -> "请上传至少1张样张图片"
                    else -> "请完善必填信息"
                }
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)

            val request = SubmissionRequest(
                presetName = state.presetName,
                description = state.description,
                deviceModel = state.deviceModel,
                sampleImages = state.sampleImages,
                tags = state.tags,
                cameraParams = state.cameraParams?.copy(
                    colorStyle = state.colorStyle
                )
            )

            communityRepository.submitPreset(request)
                .onSuccess {
                    _uiState.value = state.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                }
                .onFailure { error ->
                    _uiState.value = state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "提交失败，请重试"
                    )
                }
        }
    }

    fun saveDraft() {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true)

            val request = SubmissionRequest(
                presetName = state.presetName.ifBlank { "未命名预设" },
                description = state.description,
                deviceModel = state.deviceModel,
                sampleImages = state.sampleImages,
                tags = state.tags,
                cameraParams = state.cameraParams?.copy(
                    colorStyle = state.colorStyle
                )
            )

            communityRepository.saveDraft(request)
                .onSuccess {
                    _uiState.value = state.copy(
                        isLoading = false,
                        errorMessage = null
                    )
                    // 可以显示一个保存成功的提示
                }
                .onFailure { error ->
                    _uiState.value = state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "保存草稿失败"
                    )
                }
        }
    }
}
