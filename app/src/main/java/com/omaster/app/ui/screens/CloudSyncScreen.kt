package com.omaster.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaster.app.service.CloudSyncService
import com.omaster.app.ui.theme.AccentPrimary
import com.omaster.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudSyncScreen(
    onBack: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cloudSyncService = remember { CloudSyncService(context) }
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    
    var isSyncing by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf("") }
    
    val lastSyncTime = remember { cloudSyncService.getLastSyncTime() }
    val formattedLastSync = if (lastSyncTime > 0) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(lastSyncTime))
    } else {
        "从未同步"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("云端同步") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = AccentPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "同步状态",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "上次同步时间: $formattedLastSync",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "本地预设数量: ${presets.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (isSyncing) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = AccentPrimary
                        )
                        Text(
                            text = syncStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            isSyncing = true
                            syncStatus = "正在同步..."
                            
                            val result = cloudSyncService.syncPresets(presets)
                            
                            isSyncing = false
                            syncStatus = ""
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("同步到云端")
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        isSyncing = true
                        syncStatus = "正在下载..."
                        
                        val result = cloudSyncService.downloadPresets()
                        
                        isSyncing = false
                        syncStatus = ""
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        
                        result.presets?.let { downloadedPresets ->
                            downloadedPresets.forEach { preset ->
                                viewModel.importPreset(preset)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSyncing,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("从云端下载")
            }

            Divider()

            Text(
                "备份管理",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "本地备份",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "当云端同步失败时，系统会自动创建本地备份",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val backupFiles = remember { cloudSyncService.getBackupFiles() }
            if (backupFiles.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(backupFiles) { file ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = SimpleDateFormat(
                                            "yyyy-MM-dd HH:mm",
                                            Locale.getDefault()
                                        ).format(Date(file.lastModified())),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            val presets = cloudSyncService.restoreFromBackup()
                                            presets?.forEach { preset ->
                                                viewModel.importPreset(preset)
                                            }
                                            Toast.makeText(
                                                context,
                                                "已从备份恢复",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = "恢复")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "提示: 云端同步功能需要网络连接。在同步过程中，请保持网络畅通。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
