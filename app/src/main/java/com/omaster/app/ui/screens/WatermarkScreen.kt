package com.omaster.app.ui.screens

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.omaster.app.image.ImageProcessor
import com.omaster.app.image.WatermarkStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

@Composable
fun WatermarkScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageProcessor = remember { ImageProcessor(context) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedStyle by remember { mutableStateOf(WatermarkStyle.Simple) }
    var watermarkText by remember { mutableStateOf("OMaster") }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pickImageLauncher.launch("image/*")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("水印") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                            pickImageLauncher.launch("image/*")
                        }
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            pickImageLauncher.launch("image/*")
                        }
                        else -> {
                            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("选择图片")
            }

            if (selectedImageUri != null) {
                OutlinedTextField(
                    value = watermarkText,
                    onValueChange = { watermarkText = it },
                    label = { Text("水印文字") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("选择水印风格:")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        WatermarkStyle.Simple to "简约",
                        WatermarkStyle.Hasselblad to "哈苏",
                        WatermarkStyle.Brand to "品牌"
                    ).forEach { (style, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label)
                            RadioButton(
                                selected = selectedStyle == style,
                                onClick = { selectedStyle = style }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            val inputStream = context.contentResolver.openInputStream(selectedImageUri!!)
                            val sourceBitmap = withContext(Dispatchers.IO) {
                                android.graphics.BitmapFactory.decodeStream(inputStream)
                            }
                            processedBitmap = imageProcessor.addWatermarkToImage(
                                sourceBitmap = sourceBitmap,
                                watermarkText = watermarkText,
                                style = selectedStyle
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("应用水印")
                }

                if (processedBitmap != null) {
                    Image(
                        bitmap = processedBitmap!!.asImageBitmap(),
                        contentDescription = "处理后的图片",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                saveImageToGallery(context, processedBitmap!!)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存到相册")
                    }
                }
            }
        }
    }
}

private suspend fun saveImageToGallery(context: android.content.Context, bitmap: Bitmap) = withContext(Dispatchers.IO) {
    val contentResolver: ContentResolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "watermarked_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        val outputStream: OutputStream? = contentResolver.openOutputStream(it)
        outputStream?.use { os ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(it, contentValues, null, null)
        }
    }
}
