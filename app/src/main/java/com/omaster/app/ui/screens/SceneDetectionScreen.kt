package com.omaster.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch
import java.io.File

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
    
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = {
                Text(
                    text = "选择图片来源",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
        containerColor = DeepSpace,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI 场景识别",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary
                        )
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ImageSelectionArea(
                selectedImage = selectedImage?.toString(),
                onSelectImage = { checkAndRequestPermission() }
            )

            Button(
                onClick = {
                    scope.launch {
                        isDetecting = true
                        val scene = aiService.detectScene(selectedImage?.toString())
                        detectedScene = scene
                        recommendedPresets = aiService.getRecommendedPresets(scene, allPresets)
                        isDetecting = false
                    }
                },
                enabled = selectedImage != null && !isDetecting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isDetecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = DeepSpace,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "正在识别场景...",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepSpace
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "识别",
                        tint = DeepSpace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedImage == null) "请先选择图片" else "开始AI场景识别",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepSpace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            detectedScene?.let { scene ->
                SceneResultCard(scene = scene)
            }

            if (recommendedPresets.isNotEmpty()) {
                Text(
                    text = "为您推荐的哈苏大师预设",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(recommendedPresets) { preset ->
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AccentPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = AccentPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ImageSelectionArea(
    selectedImage: String?,
    onSelectImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .clickable(onClick = onSelectImage),
        colors = CardDefaults.cardColors(
            containerColor = DeepSpaceLight
        ),
        shape = RoundedCornerShape(16.dp)
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
                                    DeepSpace.copy(alpha = 0.8f)
                                ),
                                startY = 150f
                            )
                        )
                )
                Icon(
                    imageVector = Icons.Default.ChangeCircle,
                    contentDescription = "更换图片",
                    tint = TextPrimary,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "选择图片",
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "点击选择拍摄的样张",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
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
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = AccentPrimary,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
fun SceneResultCard(scene: SceneType, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DeepSpaceLight
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AccentPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getSceneIcon(scene),
                    contentDescription = scene.displayName,
                    tint = AccentPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Column {
                Text(
                    text = "识别结果",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Text(
                    text = scene.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = scene.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Surface(
                color = AccentPrimary.copy(alpha = 0.2f),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已识别",
                    tint = AccentPrimary,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

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
        SceneType.UNKNOWN -> Icons.Default.Help
    }
}
