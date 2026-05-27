package com.omaster.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaster.app.camera.CameraManager
import com.omaster.app.ui.theme.AccentPrimary

@Composable
fun CameraParamsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraManager = remember { CameraManager(context) }
    val isSupported by cameraManager.isSupported.collectAsStateWithLifecycle()
    val cameraParams by cameraManager.cameraParams.collectAsStateWithLifecycle()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, start camera
        }
    }

    LaunchedEffect(Unit) {
        if (isSupported && cameraManager.hasCameraPermission()) {
            // Start camera
        } else if (!cameraManager.hasCameraPermission()) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("相机参数") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isSupported) {
                Text(
                    text = "该功能在此设备上不可用",
                    style = MaterialTheme.typography.titleLarge
                )
            } else {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            cameraManager.startCamera(lifecycleOwner, this)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                cameraParams?.let { params ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ParamDisplayItem("ISO", params.iso.toString())
                        ParamDisplayItem("快门", params.shutter)
                        ParamDisplayItem("曝光补偿", params.ev)
                        ParamDisplayItem("白平衡", params.wb)
                    }
                }
            }
        }
    }
}

@Composable
fun ParamDisplayItem(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = AccentPrimary
            )
        }
    }
}
