package com.omaster.app.ui.screens
import android.content.Context
import androidx.core.content.FileProvider

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChangeCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.omaster.app.model.Preset
import com.omaster.app.model.SceneType
import com.omaster.app.service.AiService
import com.omaster.app.ui.animation.clickableWithColorOSFeedback
import com.omaster.app.ui.components.ProPresetCard
import com.omaster.app.ui.theme.ColorOSBlack
import com.omaster.app.ui.theme.ColorOSLightBackground
import com.omaster.app.ui.theme.HasselbladOrange
import kotlinx.coroutines.launch
import java.io.File

/**
 * AI 场景识别界面 - 专业设计版本
 * 支持AI-SC-001到AI-SC-035所有测试用例
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    
    var isDetecting by remember { mutableStateOf(false) }
    var detectedScene by remember { mutableStateOf<SceneType?>(null) }
    var recommendedPresets by remember { mutableStateOf<List<Preset>>(emptyList()) }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var detectionStartTime by remember { mutableStateOf<Long?>(null) }
    var detectionTime by remember { mutableStateOf<Int?>(null) }
    
    val scaleAnimation = remember { Animatable(1f) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showImageSourceDialog = true
        }
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            selectedImage = it
            detectedScene = null
            recommendedPresets = emptyList()
        }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            selectedImage = tempCameraUri
            detectedScene = null
            recommendedPresets = emptyList()
        }
    }
    
    fun createTempCameraFile(): Uri {
        val tempFile = File.createTempFile(
            "camera_photo_",
            ".jpg",
            context.cacheDir
        )
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }
    
    fun checkAndRequestPermission() {
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
                permissionLauncher.launch(permission)
            }
        }
    }
    
    suspend fun startDetection() {
        if (selectedImage == null) return
        
        isDetecting = true
        detectionStartTime = System.currentTimeMillis()
        
        try {
            val scene = aiService.detectScene(selectedImage?.toString())
            val endTime = System.currentTimeMillis()
            
            detectedScene = scene
            detectionTime = (endTime - (detectionStartTime ?: 0)).toInt()
            
            recommendedPresets = aiService.getRecommendedPresets(scene, allPresets)
            
            // 检测成功的动画效果
            scaleAnimation.animateTo(
                1.05f,
                animationSpec = tween(150)
            )
            scaleAnimation.animateTo(
                1f,
                animationSpec = tween(150)
            )
        } catch (e: Exception) {
            // 异常处理
        } finally {
            isDetecting = false
        }
    }
    
    if (showImageSourceDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = {
                Text(
                    text = "选择图片来源",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ImageSourceOption(
                        icon = Icons.Default.AddPhotoAlternate,
                        title = "从相册选择",
                        subtitle = "选择已拍摄的样张",
                        onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        }
                    )
                    ImageSourceOption(
                        icon = Icons.Default.CameraAlt,
                        title = "拍照",
                        subtitle = "使用相机拍摄新样张",
                        onClick = {
                            showImageSourceDialog = false
                            tempCameraUri = createTempCameraFile()
                            tempCameraUri?.let { cameraLauncher.launch(it) }
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    Scaffold(
        modifier = modifier,
        containerColor = ColorOSBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI 场景识别",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // 图片选择区域 - 专业设计
            ImageSelectionAreaPro(
                selectedImage = selectedImage?.toString(),
                onSelectImage = { checkAndRequestPermission() }
            )
            
            // AI识别按钮
            Button(
                onClick = {
                    scope.launch {
                        startDetection()
                    }
                },
                enabled = selectedImage != null && !isDetecting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HasselbladOrange
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (isDetecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = ColorOSBlack,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "正在识别场景...",
                        style = MaterialTheme.typography.titleMedium,
                        color = ColorOSBlack,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI识别",
                        tint = ColorOSBlack,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (selectedImage == null) "请先选择图片" else "开始 AI 场景识别",
                        style = MaterialTheme.typography.titleMedium,
                        color = ColorOSBlack,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
            
            // 识别结果展示
            detectedScene?.let { scene ->
                SceneResultCardPro(
                    scene = scene,
                    detectionTime = detectionTime,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 推荐预设展示
            if (recommendedPresets.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "为您推荐的哈苏大师预设",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recommendedPresets) { preset ->
                            ProPresetCard(
                                preset = preset,
                                onClick = { onPresetClick(preset) },
                                onFavoriteToggle = { onFavoriteToggle(preset.id) }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * 专业图片选择区域
 */
@Composable
fun ImageSelectionAreaPro(
    selectedImage: String?,
    onSelectImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clickableWithColorOSFeedback(onClick = onSelectImage),
        colors = CardDefaults.cardColors(
            containerColor = ColorOSLightBackground.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.5.dp,
            HasselbladOrange.copy(alpha = 0.3f)
        )
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
                                    ColorOSBlack.copy(alpha = 0.7f)
                                ),
                                startY = 180f
                            )
                        )
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    shape = CircleShape,
                    color = HasselbladOrange
                ) {
                    Icon(
                        imageVector = Icons.Default.ChangeCircle,
                        contentDescription = "更换图片",
                        tint = ColorOSBlack,
                        modifier = Modifier.size(28.dp).padding(6.dp)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = HasselbladOrange.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "选择图片",
                            tint = HasselbladOrange,
                            modifier = Modifier.size(80.dp).padding(24.dp)
                        )
                    }
                    Text(
                        text = "点击选择拍摄的样张",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "AI 将根据场景推荐最佳哈苏预设",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        IconLabel(Icons.Default.CameraAlt, "拍照")
                        IconLabel(Icons.Default.AddPhotoAlternate, "相册")
                    }
                }
            }
        }
    }
}

/**
 * 专业场景识别结果卡片
 */
@Composable
fun SceneResultCardPro(
    scene: SceneType,
    detectionTime: Int? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ColorOSLightBackground.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            HasselbladOrange.copy(alpha = 0.2f)
        )
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
                Surface(
                    shape = CircleShape,
                    color = HasselbladOrange.copy(alpha = 0.18f)
                ) {
                    Icon(
                        imageVector = getSceneIcon(scene),
                        contentDescription = scene.displayName,
                        tint = HasselbladOrange,
                        modifier = Modifier.size(64.dp).padding(18.dp)
                    )
                }
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "识别结果",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = scene.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Text(
                        text = scene.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
                
                Surface(
                    color = HasselbladOrange.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "已识别",
                        tint = HasselbladOrange,
                        modifier = Modifier.size(44.dp).padding(10.dp)
                    )
                }
            }
            
            // 识别时间和哈苏模式
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                detectionTime?.let { time ->
                    Chip(
                        label = "识别时间 ${time}ms",
                        icon = Icons.Default.CheckCircle
                    )
                }
                Chip(
                    label = scene.getHasselbladModeName(),
                    icon = Icons.Default.AutoAwesome,
                    isPrimary = true
                )
            }
            
            // 异常场景提示
            if (SceneType.isErrorScene(scene)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            HasselbladOrange.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = HasselbladOrange
                        )
                        Text(
                            text = "建议：请确保光线充足、画面清晰后重试",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 图片来源选项卡片
 */
@Composable
fun ImageSourceOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HasselbladOrange.copy(alpha = 0.08f))
            .clickableWithColorOSFeedback(onClick = onClick)
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = HasselbladOrange.copy(alpha = 0.2f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = HasselbladOrange,
                modifier = Modifier.size(48.dp).padding(12.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }
    }
}

/**
 * 图标标签组件
 */
@Composable
fun IconLabel(icon: ImageVector, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = HasselbladOrange,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}

/**
 * 小标签组件
 */
@Composable
fun Chip(
    label: String,
    icon: ImageVector? = null,
    isPrimary: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = if (isPrimary) HasselbladOrange.copy(alpha = 0.2f) 
                else Color.White.copy(alpha = 0.08f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            icon?.let { 
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (isPrimary) HasselbladOrange else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isPrimary) HasselbladOrange else Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * 获取场景图标
 */
fun getSceneIcon(scene: SceneType): ImageVector {
    return when (scene) {
        SceneType.LANDSCAPE, SceneType.CITYSCAPE, SceneType.RAINY_FOGGY -> Icons.Default.Landscape
        SceneType.PORTRAIT, SceneType.NIGHT_PORTRAIT, SceneType.MIXED_LANDSCAPE -> Icons.Default.Person
        SceneType.NIGHT, SceneType.STARRY_NIGHT -> Icons.Default.Nightlight
        SceneType.SUNSET, SceneType.FLOWERS_SUNSET -> Icons.Default.WbSunny
        SceneType.FOOD, SceneType.MIXED_FOOD -> Icons.Default.Restaurant
        SceneType.STREET -> Icons.Default.Commute
        SceneType.NATURE -> Icons.Default.Eco
        SceneType.ARCHITECTURE -> Icons.Default.Apartment
        SceneType.MACRO, SceneType.FLOWER, SceneType.INSECT, SceneType.OBJECT_DETAIL -> 
            Icons.Default.CenterFocusStrong
        SceneType.MOTION -> Icons.Default.AutoAwesome
        SceneType.TOO_DARK, SceneType.TOO_BRIGHT, SceneType.TOO_BLURRY, 
        SceneType.INDOOR_WARM, SceneType.STILL_LIFE -> Icons.Default.Warning
        SceneType.UNKNOWN -> Icons.Default.Warning
    }
}
