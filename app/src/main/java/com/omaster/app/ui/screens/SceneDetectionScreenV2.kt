package com.omaster.app.ui.screens

import android.content.Context
import androidx.core.content.FileProvider
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import com.omaster.app.ui.animation.ColorOSAnimationDuration
import com.omaster.app.ui.animation.ColorOSEasing
import com.omaster.app.ui.animation.ColorOSScale
import com.omaster.app.ui.components.*
import com.omaster.app.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SceneDetectionScreenV2(
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
        val startTime = System.currentTimeMillis()
        
        try {
            val scene = aiService.detectScene(selectedImage?.toString())
            val endTime = System.currentTimeMillis()
            
            detectedScene = scene
            detectionTime = (endTime - startTime).toInt()
            
            recommendedPresets = aiService.getRecommendedPresets(scene, allPresets)
            
            scaleAnimation.animateTo(
                1.05f,
                animationSpec = tween(150)
            )
            scaleAnimation.animateTo(
                1f,
                animationSpec = tween(150)
            )
        } catch (e: Exception) {
        } finally {
            isDetecting = false
        }
    }
    
    if (showImageSourceDialog) {
        GlassDialog(
            onDismiss = { showImageSourceDialog = false },
            title = "选择图片来源",
            text = "请选择图片来源以进行AI场景识别",
            confirmButton = {
                GlassButton(
                    text = "从相册选择",
                    onClick = {
                        showImageSourceDialog = false
                        galleryLauncher.launch("image/*")
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    Scaffold(
        modifier = modifier,
        containerColor = Colors.Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = Spacing.xxl)
        ) {
            item {
                GlassTopAppBar(
                    title = "AI 场景识别",
                    onBackClick = onBack
                )
            }
            
            item {
                ImageSelectionAreaV2(
                    selectedImage = selectedImage?.toString(),
                    onSelectImage = { checkAndRequestPermission() },
                    modifier = Modifier.padding(horizontal = Spacing.ScreenPadding)
                )
            }
            
            item {
                GlassButton(
                    text = if (selectedImage == null) "请先选择图片" else "开始 AI 场景识别",
                    onClick = {
                        scope.launch {
                            startDetection()
                        }
                    },
                    enabled = selectedImage != null && !isDetecting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.md),
                    isLoading = isDetecting,
                    icon = Icons.Default.AutoAwesome
                )
            }
            
            detectedScene?.let { scene ->
                item {
                    SceneResultCardV2(
                        scene = scene,
                        detectionTime = detectionTime,
                        modifier = Modifier.padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.sm)
                    )
                }
            }
            
            if (recommendedPresets.isNotEmpty()) {
                item {
                    SectionHeaderV2(
                        title = "为您推荐的哈苏大师预设",
                        modifier = Modifier.padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.sm)
                    )
                }
                
                items(recommendedPresets) { preset ->
                    GlassPresetCard(
                        preset = preset,
                        onClick = { onPresetClick(preset) },
                        onFavoriteToggle = { onFavoriteToggle(preset.id) },
                        modifier = Modifier.padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.sm)
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageSelectionAreaV2(
    selectedImage: String?,
    onSelectImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) ColorOSScale.Pressed else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "imageScale"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .scale(scale)
            .clip(RoundedCornerShape(Radius.xxl))
            .background(Colors.GlassBackground)
            .border(
                width = 1.5.dp,
                color = Colors.HasselbladOrange.copy(alpha = 0.3f),
                shape = RoundedCornerShape(Radius.xxl)
            )
            .clickable {
                isPressed = true
                onSelectImage()
            },
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
                                Colors.Background.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
            
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.md),
                shape = CircleShape,
                color = Colors.HasselbladOrange,
                onClick = onSelectImage
            ) {
                Icon(
                    imageVector = Icons.Default.ChangeCircle,
                    contentDescription = "更换图片",
                    tint = Colors.OnPrimary,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(Spacing.sm)
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Colors.HasselbladOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "选择图片",
                        tint = Colors.HasselbladOrange,
                        modifier = Modifier.size(60.dp)
                    )
                }
                
                Text(
                    text = "点击选择拍摄的样张",
                    style = Typography.TitleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Colors.OnSurface
                )
                
                Text(
                    text = "AI 将根据场景推荐最佳哈苏预设",
                    style = Typography.BodyMedium,
                    color = Colors.OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(Spacing.md))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xl)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "拍照",
                            tint = Colors.HasselbladOrange,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "拍照",
                            style = Typography.LabelMedium,
                            color = Colors.OnSurfaceVariant
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "相册",
                            tint = Colors.HasselbladOrange,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "相册",
                            style = Typography.LabelMedium,
                            color = Colors.OnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SceneResultCardV2(
    scene: SceneType,
    detectionTime: Int? = null,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = ColorOSAnimationDuration.MEDIUM,
            easing = ColorOSEasing.Decelerate
        ),
        label = "resultAlpha"
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        ),
        label = "resultScale"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatedAlpha
                scaleX = animatedScale
                scaleY = animatedScale
            }
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Colors.HasselbladOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getSceneIconV2(scene),
                            contentDescription = scene.displayName,
                            tint = Colors.HasselbladOrange,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "识别结果",
                            style = Typography.LabelMedium,
                            color = Colors.OnSurfaceVariant
                        )
                        
                        Text(
                            text = scene.displayName,
                            style = Typography.HeadlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Colors.HasselbladOrange
                        )
                        
                        Text(
                            text = scene.description,
                            style = Typography.BodyMedium,
                            color = Colors.OnSurfaceVariant
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Colors.HasselbladOrange.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "已识别",
                            tint = Colors.HasselbladOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    detectionTime?.let { time ->
                        GlassChip(
                            text = "识别时间 ${time}ms",
                            selected = false,
                            onClick = {}
                        )
                    }
                    
                    GlassChip(
                        text = scene.getHasselbladModeName(),
                        selected = true,
                        onClick = {}
                    )
                }
                
                if (SceneType.isErrorScene(scene)) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {}
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Colors.HasselbladOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            
                            Text(
                                text = "建议：请确保光线充足、画面清晰后重试",
                                style = Typography.BodyMedium,
                                color = Colors.OnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getSceneIconV2(scene: SceneType): ImageVector {
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
