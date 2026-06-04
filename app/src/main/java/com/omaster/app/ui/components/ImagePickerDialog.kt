package com.omaster.app.ui.components

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.omaster.app.ui.theme.ColorOSBlack
import com.omaster.app.ui.theme.ColorOSLightBackground
import com.omaster.app.ui.theme.HasselbladOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 图片选择器组件 - 支持S001-S010测试用例
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerDialog(
    onImageSelected: (Uri) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var images by remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var sortOrder by remember { mutableStateOf(SortOrder.DATE_DESC) }
    var filterFormat by remember { mutableStateOf(ImageFormat.ALL) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            loadImages(context, sortOrder, filterFormat) { imageList ->
                images = imageList
                isLoading = false
            }
        }
    }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { 
            selectedImage = it
            onImageSelected(it)
            onDismiss()
        }
    }
    
    LaunchedEffect(Unit) {
        hasPermission = checkMediaPermission(context)
        if (hasPermission) {
            loadImages(context, sortOrder, filterFormat) { imageList ->
                images = imageList
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }
    
    Scaffold(
        modifier = modifier,
        containerColor = ColorOSBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "选择样张",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        imagePickerLauncher.launch("image/*")
                    }) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "从相册选择",
                            tint = HasselbladOrange
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
        ) {
            if (!hasPermission) {
                PermissionRequestCard(
                    onRequestPermission = {
                        requestMediaPermission(context, permissionLauncher)
                    },
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                SortAndFilterBar(
                    currentSort = sortOrder,
                    currentFilter = filterFormat,
                    onSortChange = { newSort ->
                        sortOrder = newSort
                        isLoading = true
                        loadImages(context, newSort, filterFormat) { imageList ->
                            images = imageList
                            isLoading = false
                        }
                    },
                    onFilterChange = { newFilter ->
                        filterFormat = newFilter
                        isLoading = true
                        loadImages(context, sortOrder, newFilter) { imageList ->
                            images = imageList
                            isLoading = false
                        }
                    }
                )
                
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = HasselbladOrange
                        )
                    }
                } else if (images.isEmpty()) {
                    EmptyStateCard(modifier = Modifier.fillMaxSize())
                } else {
                    ImageGrid(
                        images = images,
                        selectedImage = selectedImage,
                        onImageClick = { image ->
                            selectedImage = image.uri
                            onImageSelected(image.uri)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRequestCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ColorOSLightBackground.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                tint = HasselbladOrange,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "需要相册权限",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "请授权访问相册，以便导入您的样张进行AI微调",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HasselbladOrange
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "授予权限",
                    color = ColorOSBlack,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SortAndFilterBar(
    currentSort: SortOrder,
    currentFilter: ImageFormat,
    onSortChange: (SortOrder) -> Unit,
    onFilterChange: (ImageFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            FilterChip(
                selected = true,
                onClick = { showSortMenu = true },
                label = {
                    Text(
                        text = when (currentSort) {
                            SortOrder.DATE_DESC -> "最新优先"
                            SortOrder.DATE_ASC -> "最旧优先"
                            SortOrder.NAME_ASC -> "名称↑"
                            SortOrder.NAME_DESC -> "名称↓"
                            SortOrder.SIZE_DESC -> "最大优先"
                            SortOrder.SIZE_ASC -> "最小优先"
                        },
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                SortOrder.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                when (sort) {
                                    SortOrder.DATE_DESC -> "最新优先"
                                    SortOrder.DATE_ASC -> "最旧优先"
                                    SortOrder.NAME_ASC -> "名称升序"
                                    SortOrder.NAME_DESC -> "名称降序"
                                    SortOrder.SIZE_DESC -> "大小降序"
                                    SortOrder.SIZE_ASC -> "大小升序"
                                }
                            )
                        },
                        onClick = {
                            onSortChange(sort)
                            showSortMenu = false
                        }
                    )
                }
            }
        }
        
        Box {
            FilterChip(
                selected = currentFilter != ImageFormat.ALL,
                onClick = { showFilterMenu = true },
                label = {
                    Text(
                        text = when (currentFilter) {
                            ImageFormat.ALL -> "所有格式"
                            ImageFormat.JPG -> "JPG"
                            ImageFormat.PNG -> "PNG"
                            ImageFormat.HEIC -> "HEIC"
                            ImageFormat.WEBP -> "WebP"
                        },
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            DropdownMenu(
                expanded = showFilterMenu,
                onDismissRequest = { showFilterMenu = false }
            ) {
                ImageFormat.entries.forEach { format ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                when (format) {
                                    ImageFormat.ALL -> "所有格式"
                                    ImageFormat.JPG -> "JPG"
                                    ImageFormat.PNG -> "PNG"
                                    ImageFormat.HEIC -> "HEIC"
                                    ImageFormat.WEBP -> "WebP"
                                }
                            )
                        },
                        onClick = {
                            onFilterChange(format)
                            showFilterMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ImageGrid(
    images: List<ImageItem>,
    selectedImage: Uri?,
    onImageClick: (ImageItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(images, key = { it.uri.toString() }) { image ->
            ImageGridItem(
                image = image,
                isSelected = selectedImage == image.uri,
                onClick = { onImageClick(image) }
            )
        }
    }
}

@Composable
fun ImageGridItem(
    image: ImageItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, HasselbladOrange, RoundedCornerShape(8.dp))
                } else Modifier
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(image.uri)
                .crossfade(true)
                .size(300)
                .build(),
            contentDescription = image.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        if (image.format != "jpg" && image.format != "jpeg") {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                color = ColorOSBlack.copy(alpha = 0.7f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = image.format.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = HasselbladOrange,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ImageNotSupported,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "未找到图片",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = "请检查相册中是否有支持的图片格式",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

enum class SortOrder {
    DATE_DESC,
    DATE_ASC,
    NAME_ASC,
    NAME_DESC,
    SIZE_DESC,
    SIZE_ASC
}

enum class ImageFormat {
    ALL,
    JPG,
    PNG,
    HEIC,
    WEBP
}

data class ImageItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val date: Long,
    val format: String
)

suspend fun loadImages(
    context: Context,
    sortOrder: SortOrder,
    filter: ImageFormat,
    onResult: (List<ImageItem>) -> Unit
) {
    withContext(Dispatchers.IO) {
        val images = mutableListOf<ImageItem>()
        
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.MIME_TYPE
        )
        
        val sortColumn = when (sortOrder) {
            SortOrder.DATE_DESC, SortOrder.DATE_ASC -> MediaStore.Images.Media.DATE_MODIFIED
            SortOrder.NAME_ASC, SortOrder.NAME_DESC -> MediaStore.Images.Media.DISPLAY_NAME
            SortOrder.SIZE_DESC, SortOrder.SIZE_ASC -> MediaStore.Images.Media.SIZE
        }
        
        val sortOrderString = when (sortOrder) {
            SortOrder.DATE_DESC, SortOrder.NAME_DESC, SortOrder.SIZE_DESC -> "$sortColumn DESC"
            SortOrder.DATE_ASC, SortOrder.NAME_ASC, SortOrder.SIZE_ASC -> "$sortColumn ASC"
        }
        
        val mimeFilter = when (filter) {
            ImageFormat.ALL -> null
            ImageFormat.JPG -> "image/jpeg"
            ImageFormat.PNG -> "image/png"
            ImageFormat.HEIC -> "image/heic"
            ImageFormat.WEBP -> "image/webp"
        }
        
        val selection = mimeFilter?.let { "${MediaStore.Images.Media.MIME_TYPE} = ?" }
        val selectionArgs = mimeFilter?.let { arrayOf(it) }
        
        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrderString
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val mimeColumn = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val size = cursor.getLong(sizeColumn)
                val date = cursor.getLong(dateColumn)
                val mimeType = if (mimeColumn >= 0) cursor.getString(mimeColumn) ?: "image/jpeg" else "image/jpeg"
                val format = when {
                    mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
                    mimeType.contains("png") -> "png"
                    mimeType.contains("heic") -> "heic"
                    mimeType.contains("webp") -> "webp"
                    else -> "unknown"
                }
                
                val contentUri = Uri.withAppendedPath(collection, id.toString())
                
                images.add(
                    ImageItem(
                        uri = contentUri,
                        name = name,
                        size = size,
                        date = date,
                        format = format
                    )
                )
            }
        }
        
        withContext(Dispatchers.Main) {
            onResult(images)
        }
    }
}

fun checkMediaPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

fun requestMediaPermission(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<String>
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        launcher.launch(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

@Composable
fun rememberStoragePermissionLauncher(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) {
        onPermissionGranted()
    } else {
        onPermissionDenied()
    }
}

fun hasStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Android 13+ 需要READ_MEDIA_IMAGES权限
        context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Android 10-12 分区存储，读取自己的文件不需要权限
        // 但读取其他应用的媒体文件仍需要READ_EXTERNAL_STORAGE
        context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        // Android 9及以下需要WRITE_EXTERNAL_STORAGE权限
        context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
