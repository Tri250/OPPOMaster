package com.omaster.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.omaster.app.model.Preset
import com.omaster.app.model.SceneType
import com.omaster.app.service.AiService
import com.omaster.app.ui.components.PresetCard
import com.omaster.app.ui.theme.*
import com.omaster.app.util.BitmapUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

// ColorOS 16 动画时长规范
private const val ANIMATION_DURATION_FAST = 200
private const val ANIMATION_DURATION_MEDIUM = 350
private const val ANIMATION_DURATION_SLOW = 500

@Composable
fun SceneDetectionScreen(
    aiService: AiService,
    allPresets: List<Preset>,
    onBack: () -> Unit,
    onPresetClick: (Preset) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // UI状态 - 更新为支持新的识别结果类型
    var isDetecting by remember { mutableStateOf(false) }
    var detectionResult by remember { mutableStateOf<AiService.SceneDetectionResult?>(null) }
    var recommendedPresets by remember { mutableStateOf<List<Preset>>(emptyList()) }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    
    // 对话框状态
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var showPermissionDenied by remember { mutableStateOf(false) }
    var requestedPermissionType by remember { mutableStateOf(PermissionType.NONE) }
    
    // 骨架屏加载状态
    var showSkeleton by remember { mutableStateOf(false) }

    // 权限启动器
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showImageSourceDialog = true
        } else {
            showPermissionDenied = true
            requestedPermissionType = PermissionType.STORAGE
        }
    }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera(context) { uri ->
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            }
        } else {
            showPermissionDenied = true
            requestedPermissionType = PermissionType.CAMERA
        }
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            selectedImage = it
            detectionResult = null
            recommendedPresets = emptyList()
        }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            selectedImage = tempCameraUri
            detectionResult = null
            recommendedPresets = emptyList()
        }
    }
    
    fun openCamera(context: Context, onUriReady: (Uri) -> Unit) {
        val tempFile = File.createTempFile(
            "camera_photo_",
            ".jpg",
            context.cacheDir
        )
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        onUriReady(uri)
    }
    
    fun checkStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        when {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                showImageSourceDialog = true
            }
            else -> {
                showPermissionRationale = true
                requestedPermissionType = PermissionType.STORAGE
            }
        }
    }
    
    fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        storagePermissionLauncher.launch(permission)
    }
    
    fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openCamera(context) { uri ->
                    tempCameraUri = uri
                    cameraLauncher.launch(uri)
                }
            }
            else -> {
                showPermissionRationale = true
                requestedPermissionType = PermissionType.CAMERA
            }
        }
    }
    
    fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
    
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
    
    // 权限说明对话框
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = {
                Text(
                    text = when (requestedPermissionType) {
                        PermissionType.STORAGE -> "存储权限"
                        PermissionType.CAMERA -> "相机权限"
                        PermissionType.NONE -> "权限请求"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = OppoLightTextPrimary
                )
            },
            text = {
                Text(
                    text = when (requestedPermissionType) {
                        PermissionType.STORAGE -> "需要存储权限才能选择相册中的照片进行场景识别。"
                        PermissionType.CAMERA -> "需要相机权限才能拍摄照片进行场景识别。"
                        PermissionType.NONE -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = OppoLightTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionRationale = false
                        when (requestedPermissionType) {
                            PermissionType.STORAGE -> requestStoragePermission()
                            PermissionType.CAMERA -> requestCameraPermission()
                            PermissionType.NONE -> {}
                        }
                    }
                ) {
                    Text("授权", color = HasselbladOrangePro)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("取消", color = OppoLightTextSecondary)
                }
            },
            containerColor = OppoLightSurface
        )
    }
    
    // 权限被拒绝对话框
    if (showPermissionDenied) {
        AlertDialog(
            onDismissRequest = { showPermissionDenied = false },
            title = {
                Text(
                    text = "权限被拒绝",
                    style = MaterialTheme.typography.titleLarge,
                    color = OppoLightTextPrimary
                )
            },
            text = {
                Text(
                    text = when (requestedPermissionType) {
                        PermissionType.STORAGE -> "您已拒绝存储权限。请在设置中启用该权限以使用此功能。"
                        PermissionType.CAMERA -> "您已拒绝相机权限。请在设置中启用该权限以使用此功能。"
                        PermissionType.NONE -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = OppoLightTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDenied = false
                        openAppSettings(context)
                    }
                ) {
                    Text("去设置", color = HasselbladOrangePro)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDenied = false }) {
                    Text("取消", color = OppoLightTextSecondary)
                }
            },
            containerColor = OppoLightSurface
        )
    }
    
    // 图片来源选择对话框 - ColorOS 16 风格
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = {
                Text(
                    text = "选择图片来源",
                    style = MaterialTheme.typography.headlineSmall,
                    color = OppoLightTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ImageSourceOption(
                        icon = Icons.Default.PhotoLibrary,
                        title = "从相册选择",
                        subtitle = "选择已拍摄的照片",
                        onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        }
                    )
                    ImageSourceOption(
                        icon = Icons.Default.CameraAlt,
                        title = "拍照",
                        subtitle = "使用相机拍摄",
                        onClick = {
                            showImageSourceDialog = false
                            checkCameraPermission()
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text("取消", color = OppoLightTextSecondary)
                }
            },
            containerColor = OppoLightSurface,
            shape = RoundedCornerShape(24.dp)
        )
    }
    
    Scaffold(
        modifier = modifier,
        containerColor = OppoDeepSpace,
        topBar = {
            OppoTopAppBar(
                title = "AI 场景识别",
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 图片选择区域
            ImageSelectionArea(
                selectedImage = selectedImage?.toString(),
                onSelectImage = { checkStoragePermission() },
                onReplaceImage = {
                    detectionResult = null
                    recommendedPresets = emptyList()
                    checkStoragePermission()
                }
            )

            // 识别按钮 - 优化性能，响应更快
            DetectButton(
                onClick = {
                    scope.launch {
                        isDetecting = true
                        showSkeleton = true
                        
                        // 使用 BitmapUtils 解码 Uri 为 Bitmap
                        val bitmap = selectedImage?.let { uri ->
                            BitmapUtils.decodeUriToBitmap(context, uri, maxDimension = 1024)
                        }
                        
                        // 使用新的识别API
                        val result = aiService.detectScene(
                            imageUri = selectedImage?.toString(),
                            bitmap = bitmap
                        )
                        detectionResult = result
                        
                        // 如果是边界场景，不显示预设
                        if (!result.isEdgeCase) {
                            recommendedPresets = aiService.getRecommendedPresets(result, allPresets)
                        } else {
                            recommendedPresets = emptyList()
                        }
                        
                        // 稍微延迟一下让动画更自然
                        delay(300)
                        isDetecting = false
                        showSkeleton = false
                    }
                },
                enabled = selectedImage != null && !isDetecting,
                isLoading = isDetecting
            )

            // 场景识别结果 - 更新为支持新的结果类型
            AnimatedVisibility(
                visible = detectionResult != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 20 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 })
            ) {
                detectionResult?.let { result ->
                    if (result.isEdgeCase) {
                        EdgeCaseResultCard(
                            result = result,
                            onRetry = {
                                scope.launch {
                                    isDetecting = true
                                    detectionResult = null
                                    val retryResult = aiService.detectScene(selectedImage?.toString())
                                    detectionResult = retryResult
                                    if (!retryResult.isEdgeCase) {
                                        recommendedPresets = aiService.getRecommendedPresets(retryResult, allPresets)
                                    } else {
                                        recommendedPresets = emptyList()
                                    }
                                    isDetecting = false
                                }
                            }
                        )
                    } else {
                        SceneResultCard(result = result)
                    }
                }
            }

            // 推荐预设列表 - 带骨架屏，仅在非边界场景显示
            AnimatedContent(
                targetState = showSkeleton to recommendedPresets.isNotEmpty(),
                label = "presets_animation"
            ) { (showSk, hasPresets) ->
                when {
                    showSk -> PresetSkeletonList()
                    hasPresets -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "为您推荐的哈苏大师预设",
                                style = MaterialTheme.typography.headlineSmall,
                                color = OppoTextPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(recommendedPresets, key = { it.id }) { preset ->
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f),
                                        exit = fadeOut() + scaleOut()
                                    ) {
                                        PresetCard(
                                            preset = preset,
                                            onClick = { onPresetClick(preset) },
                                            onFavoriteToggle = { onFavoriteToggle(preset.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// ColorOS 16 风格的顶部AppBar
@Composable
fun OppoTopAppBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = OppoTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = OppoTextPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = OppoDeepSpace
        ),
        modifier = modifier
    )
}

// 识别按钮组件
@Composable
fun DetectButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.95f,
        label = "button_scale"
    )
    
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        colors = ButtonDefaults.buttonColors(
            containerColor = HasselbladOrangePro,
            disabledContainerColor = OppoGrey700
        ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = OppoDeepSpace,
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "AI正在识别场景...",
                style = MaterialTheme.typography.titleLarge,
                color = OppoDeepSpace,
                fontWeight = FontWeight.Bold
            )
        } else {
            Icon(
                imageVector = Icons.Default.AutoFixHigh,
                contentDescription = "识别",
                tint = OppoDeepSpace
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (!enabled) "请先选择图片" else "开始AI场景识别",
                style = MaterialTheme.typography.titleLarge,
                color = OppoDeepSpace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 权限类型枚举
private enum class PermissionType {
    NONE,
    STORAGE,
    CAMERA
}

@Composable
private fun ImageSourceOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(HasselbladOrangePro.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = HasselbladOrangePro,
                modifier = Modifier.size(26.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = OppoLightTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = OppoLightTextSecondary
            )
        }
    }
}

@Composable
fun ImageSelectionArea(
    selectedImage: String?,
    onSelectImage: () -> Unit,
    onReplaceImage: () -> Unit = onSelectImage,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clickable(onClick = if (selectedImage == null) onSelectImage else onReplaceImage),
        colors = CardDefaults.cardColors(
            containerColor = OppoCardSurface
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (selectedImage != null) {
                AsyncImage(
                    model = selectedImage,
                    contentDescription = "已选图片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    OppoDeepSpace.copy(alpha = 0.85f)
                                ),
                                startY = 0.5f
                            )
                        )
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                        .size(56.dp),
                    shape = CircleShape,
                    color = HasselbladOrangePro.copy(alpha = 0.9f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChangeCircle,
                        contentDescription = "更换图片",
                        tint = OppoDeepSpace,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(12.dp)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(OppoGrey800),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "选择图片",
                            tint = OppoTextSecondary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Text(
                        text = "点击选择拍摄的样张",
                        style = MaterialTheme.typography.titleLarge,
                        color = OppoTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        IconLabel(Icons.Default.Camera, "拍照")
                        IconLabel(Icons.Default.Image, "相册")
                    }
                }
            }
        }
    }
}

@Composable
fun IconLabel(icon: ImageVector, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = HasselbladOrangePro,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = OppoTextSecondary
        )
    }
}

// 边界场景结果卡片
@Composable
fun EdgeCaseResultCard(
    result: AiService.SceneDetectionResult,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = OppoCardSurface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(OppoWarningRed, OppoWarningRed.copy(alpha = 0.7f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (result.primaryScene) {
                            SceneType.BLACK -> Icons.Default.Warning
                            SceneType.WHITE -> Icons.Default.HelpOutline
                            SceneType.BLURRY -> Icons.Default.BlurOn
                            else -> Icons.Default.Error
                        },
                        contentDescription = result.primaryScene.displayName,
                        tint = OppoDeepSpace,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "无法识别",
                        style = MaterialTheme.typography.labelMedium,
                        color = OppoTextTertiary
                    )
                    Text(
                        text = result.primaryScene.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = OppoTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    result.edgeCaseMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OppoTextSecondary
                        )
                    }
                }
                
                Surface(
                    color = OppoWarningRed.copy(alpha = 0.15f),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "提示",
                        tint = OppoWarningRed,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = HasselbladOrangePro
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, HasselbladOrangePro.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重试",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "重新识别",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Button(
                    onClick = { /* 手动选择场景 */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HasselbladOrangePro
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "手动选择",
                        tint = OppoDeepSpace,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "手动选择",
                        style = MaterialTheme.typography.titleMedium,
                        color = OppoDeepSpace
                    )
                }
            }
        }
    }
}

// 正常场景结果卡片 - 更新为支持新的结果类型
@Composable
fun SceneResultCard(result: AiService.SceneDetectionResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = OppoCardSurface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = GradientHasselbladPro
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getSceneIcon(result.primaryScene),
                        contentDescription = result.primaryScene.displayName,
                        tint = OppoDeepSpace,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "识别结果",
                        style = MaterialTheme.typography.labelMedium,
                        color = OppoTextTertiary
                    )
                    Text(
                        text = result.primaryScene.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = OppoTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = result.primaryScene.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OppoTextSecondary
                    )
                }
                
                Surface(
                    color = OppoVitalGreen.copy(alpha = 0.15f),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "已识别",
                        tint = OppoVitalGreen,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            // 显示次要场景（如果有）
            result.secondaryScene?.let { secondary ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getSceneIcon(secondary),
                        contentDescription = secondary.displayName,
                        tint = OppoTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "同时检测到: ${secondary.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OppoTextSecondary
                    )
                }
            }
            
            // 显示置信度
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "置信度: ${(result.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = OppoTextTertiary
                )
                // 置信度条
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(OppoGrey700)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(result.confidence)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = GradientHasselbladPro
                                )
                            )
                    )
                }
            }
        }
    }
}

// 骨架屏组件 - ColorOS 16 风格
@Composable
fun PresetSkeletonList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 标题骨架
        SkeletonBox(
            modifier = Modifier
                .width(200.dp)
                .height(24.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 预设卡片骨架
        repeat(4) {
            PresetSkeletonCard()
        }
    }
}

@Composable
fun PresetSkeletonCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = OppoCardSurface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 图片骨架
            SkeletonBox(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            
            // 文字骨架
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(20.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp)
                )
            }
        }
    }
}

@Composable
fun SkeletonBox(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_animation")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 1000,
                easing = androidx.compose.animation.core.LinearEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    
    Box(
        modifier = modifier
            .background(
                color = OppoGrey700.copy(alpha = alpha),
                shape = RoundedCornerShape(8.dp)
            )
    )
}

// 更新场景图标映射，包含所有新场景
fun getSceneIcon(scene: SceneType): ImageVector {
    return when (scene) {
        SceneType.LANDSCAPE -> Icons.Default.Landscape
        SceneType.PORTRAIT -> Icons.Default.Person
        SceneType.NIGHT -> Icons.Default.Nightlight
        SceneType.SUNSET -> Icons.Default.WbSunny
        SceneType.FOOD -> Icons.Default.Restaurant
        SceneType.STREET -> Icons.Default.Commute
        SceneType.NATURE -> Icons.Default.Eco
        SceneType.ARCHITECTURE -> Icons.Default.Apartment
        SceneType.MACRO -> Icons.Default.CenterFocusStrong
        SceneType.SPORTS -> Icons.Default.DirectionsRun
        SceneType.NIGHT_PORTRAIT -> Icons.Default.Person
        SceneType.BLACK -> Icons.Default.Warning
        SceneType.WHITE -> Icons.Default.HelpOutline
        SceneType.BLURRY -> Icons.Default.BlurOn
        SceneType.UNKNOWN -> Icons.Default.Help
    }
}
