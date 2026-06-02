package com.omaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.omaster.app.ui.theme.Spacing
import com.omaster.app.ui.theme.Typography
import com.omaster.app.ui.theme.hasselbladOrange
import kotlinx.coroutines.launch

/**
 * 意见反馈页面 - FEED测试用例实现
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    onViewFeedbackHistory: () -> Unit = {}
) {
    val viewModel = remember { FeedbackViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "意见反馈",
                        style = Typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            // 反馈类型选择
            FeedbackTypeSelector(
                selectedType = uiState.selectedType,
                onTypeSelected = { viewModel.selectFeedbackType(it) }
            )

            // 问题描述
            FeedbackTextInput(
                title = "问题描述",
                description = "详细描述您遇到的问题或建议",
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                maxLength = 1000
            )

            // 添加图片
            FeedbackImageSection(
                images = uiState.images,
                onAddImage = { showBottomSheet = true },
                onRemoveImage = { viewModel.removeImage(it) }
            )

            // 联系方式
            FeedbackTextInput(
                title = "联系方式",
                description = "可选，方便我们联系您处理问题",
                value = uiState.contact,
                onValueChange = { viewModel.updateContact(it) },
                maxLength = 100,
                isOptional = true
            )

            // 提交按钮
            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.submitFeedback()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canSubmit && !uiState.isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = hasselbladOrange
                )
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(Spacing.small))
                    Text("提交中...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "提交",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.small))
                    Text("提交反馈")
                }
            }

            // 反馈历史链接
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onViewFeedbackHistory) {
                    Text(
                        text = "查看反馈历史",
                        style = Typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // 底部操作菜单
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.medium)
            ) {
                Text(
                    text = "选择图片来源",
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(Spacing.medium))

                BottomSheetOption(
                    icon = Icons.Default.PhotoLibrary,
                    title = "从相册选择",
                    description = "选择已保存的图片",
                    onClick = {
                        showBottomSheet = false
                        viewModel.addImage("album_image_1.jpg")
                    }
                )

                Spacer(modifier = Modifier.height(Spacing.medium))

                BottomSheetOption(
                    icon = Icons.Default.PhotoCamera,
                    title = "拍照",
                    description = "立即拍照上传",
                    onClick = {
                        showBottomSheet = false
                        viewModel.addImage("camera_image_1.jpg")
                    }
                )

                Spacer(modifier = Modifier.height(Spacing.large))

                OutlinedButton(
                    onClick = { showBottomSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("取消")
                }
            }
        }
    }

    // 提交成功提示
    if (uiState.showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideSuccessDialog() },
            title = { Text("提交成功") },
            text = { Text("感谢您的反馈！我们会尽快处理。") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.hideSuccessDialog() 
                    viewModel.resetForm()
                }) {
                    Text("确定")
                }
            }
        )
    }
}

/**
 * 反馈类型选择器
 */
@Composable
fun FeedbackTypeSelector(
    selectedType: FeedbackType?,
    onTypeSelected: (FeedbackType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            Text(
                text = "反馈类型",
                style = Typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            val feedbackTypes = listOf(
                FeedbackType.Suggestion,
                FeedbackType.Bug,
                FeedbackType.Performance,
                FeedbackType.Other
            )

            feedbackTypes.forEach { type ->
                val isSelected = selectedType == type
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) hasselbladOrange.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable { onTypeSelected(type) }
                        .padding(Spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) hasselbladOrange
                        else MaterialTheme.colorScheme.outlineVariant
                    ) {
                        if (isSelected) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "已选",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(Spacing.medium))

                    Column {
                        Text(
                            text = type.title,
                            style = Typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                        Text(
                            text = type.description,
                            style = Typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (type != feedbackTypes.last()) {
                    Spacer(modifier = Modifier.height(Spacing.small))
                }
            }
        }
    }
}

/**
 * 文本输入区域
 */
@Composable
fun FeedbackTextInput(
    title: String,
    description: String,
    value: String,
    onValueChange: (String) -> Unit,
    maxLength: Int,
    isOptional: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title + if (isOptional) " (可选)" else "",
                    style = Typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${value.length}/$maxLength",
                    style = Typography.bodySmall,
                    color = if (value.length > maxLength) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            Text(
                text = description,
                style = Typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= maxLength) onValueChange(it) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

/**
 * 图片添加区域
 */
@Composable
fun FeedbackImageSection(
    images: List<String>,
    onAddImage: () -> Unit,
    onRemoveImage: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            Text(
                text = "添加图片",
                style = Typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            Text(
                text = "最多添加5张图片",
                style = Typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                // 添加按钮
                if (images.size < 5) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(onClick = onAddImage),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "添加图片",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "添加",
                                style = Typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 已添加的图片
                images.forEachIndexed { index, image ->
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "图片",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(20.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Box(
                                modifier = Modifier.clickable { onRemoveImage(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "删除",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 底部菜单选项
 */
@Composable
fun BottomSheetOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(Spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(10.dp),
            color = hasselbladOrange.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = hasselbladOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.medium))

        Column {
            Text(
                text = title,
                style = Typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = Typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 反馈类型
 */
enum class FeedbackType(
    val title: String,
    val description: String
) {
    Suggestion("功能建议", "提出您的功能改进建议"),
    Bug("Bug反馈", "报告应用存在的问题"),
    Performance("性能问题", "反馈应用运行速度等性能问题"),
    Other("其他", "其他需要反馈的问题")
}

/**
 * 反馈ViewModel
 */
class FeedbackViewModel {
    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: androidx.compose.runtime.StateFlow<FeedbackUiState> = _uiState

    fun selectFeedbackType(type: FeedbackType) {
        _uiState.value = _uiState.value.copy(selectedType = type)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateContact(contact: String) {
        _uiState.value = _uiState.value.copy(contact = contact)
    }

    fun addImage(image: String) {
        val currentImages = _uiState.value.images.toMutableList()
        if (currentImages.size < 5) {
            currentImages.add(image)
            _uiState.value = _uiState.value.copy(images = currentImages)
        }
    }

    fun removeImage(index: Int) {
        val currentImages = _uiState.value.images.toMutableList()
        if (index in currentImages.indices) {
            currentImages.removeAt(index)
            _uiState.value = _uiState.value.copy(images = currentImages)
        }
    }

    suspend fun submitFeedback() {
        if (!_uiState.value.canSubmit) return

        _uiState.value = _uiState.value.copy(isSubmitting = true)
        
        // 模拟提交
        kotlinx.coroutines.delay(1500)
        
        _uiState.value = _uiState.value.copy(
            isSubmitting = false,
            showSuccessDialog = true
        )
    }

    fun hideSuccessDialog() {
        _uiState.value = _uiState.value.copy(showSuccessDialog = false)
    }

    fun resetForm() {
        _uiState.value = FeedbackUiState()
    }
}

data class FeedbackUiState(
    val selectedType: FeedbackType? = null,
    val description: String = "",
    val contact: String = "",
    val images: List<String> = emptyList(),
    val isSubmitting: Boolean = false,
    val showSuccessDialog: Boolean = false
) {
    val canSubmit: Boolean
        get() = selectedType != null && description.isNotEmpty()
}
