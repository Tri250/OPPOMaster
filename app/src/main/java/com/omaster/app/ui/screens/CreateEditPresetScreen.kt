package com.omaster.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.model.Section

@Composable
fun CreateEditPresetScreen(
    preset: Preset?,
    onBack: () -> Unit,
    onSavePreset: (Preset) -> Unit
) {
    var name by remember { mutableStateOf(preset?.name ?: "") }
    var coverPath by remember { mutableStateOf(preset?.coverPath ?: "custom_preset_${System.currentTimeMillis()}") }
    var deviceModel by remember { mutableStateOf(preset?.deviceModel ?: "") }
    var tagsInput by remember { mutableStateOf(preset?.tags?.joinToString(", ") ?: "") }
    
    var mode by remember { mutableStateOf(preset?.cameraParams?.mode ?: "master") }
    var filter by remember { mutableStateOf(preset?.cameraParams?.filter ?: "") }
    var iso by remember { mutableStateOf((preset?.cameraParams?.iso ?: 100).toString()) }
    var shutter by remember { mutableStateOf(preset?.cameraParams?.shutter ?: "1/125") }
    var ev by remember { mutableStateOf(preset?.cameraParams?.ev ?: "0") }
    var wb by remember { mutableStateOf(preset?.cameraParams?.wb ?: "5500K") }
    var hasselbladHncs by remember { mutableStateOf(preset?.cameraParams?.hasselblad_hncs ?: false) }
    
    var sections by remember { mutableStateOf(preset?.sections ?: emptyList()) }
    var newSectionTitle by remember { mutableStateOf("") }
    var newSectionContent by remember { mutableStateOf("") }

    val isEditing = preset != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑预设" else "创建预设") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val cameraParams = CameraParams(
                            mode = mode,
                            filter = filter,
                            iso = iso.toIntOrNull() ?: 100,
                            shutter = shutter,
                            ev = ev,
                            wb = wb,
                            hasselblad_hncs = hasselbladHncs
                        )
                        val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        
                        val newPreset = preset?.copy(
                            name = name,
                            coverPath = coverPath,
                            deviceModel = deviceModel,
                            tags = tags,
                            sections = sections,
                            cameraParams = cameraParams
                        ) ?: Preset(
                            id = "",
                            name = name,
                            coverPath = coverPath,
                            sections = sections,
                            cameraParams = cameraParams,
                            deviceModel = deviceModel,
                            source = "custom",
                            tags = tags
                        )
                        onSavePreset(newPreset)
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("预设名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = deviceModel,
                onValueChange = { deviceModel = it },
                label = { Text("适配机型") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("例如：Find X8 Pro") }
            )

            OutlinedTextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                label = { Text("标签") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("用逗号分隔，例如：风景, 人像") }
            )

            Divider()

            Text("相机参数", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = mode,
                onValueChange = { mode = it },
                label = { Text("拍摄模式") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                label = { Text("滤镜") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = iso,
                onValueChange = { iso = it },
                label = { Text("ISO") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = shutter,
                onValueChange = { shutter = it },
                label = { Text("快门速度") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("例如：1/125 或 30") }
            )

            OutlinedTextField(
                value = ev,
                onValueChange = { ev = it },
                label = { Text("曝光补偿") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("例如：+0.3 或 -1") }
            )

            OutlinedTextField(
                value = wb,
                onValueChange = { wb = it },
                label = { Text("白平衡") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("例如：5500K 或 自动") }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = hasselbladHncs,
                    onCheckedChange = { hasselbladHncs = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("哈苏自然色彩解决方案 (HNCS)")
            }

            Divider()

            Text("参数说明", style = MaterialTheme.typography.titleLarge)

            if (sections.isNotEmpty()) {
                sections.forEachIndexed { index, section ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(section.title, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(section.content, style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = {
                                sections = sections.filterIndexed { i, _ -> i != index }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除")
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = newSectionTitle,
                onValueChange = { newSectionTitle = it },
                label = { Text("说明标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = newSectionContent,
                onValueChange = { newSectionContent = it },
                label = { Text("说明内容") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Button(
                onClick = {
                    if (newSectionTitle.isNotEmpty() && newSectionContent.isNotEmpty()) {
                        sections = sections + Section(newSectionTitle, newSectionContent)
                        newSectionTitle = ""
                        newSectionContent = ""
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = newSectionTitle.isNotEmpty() && newSectionContent.isNotEmpty()
            ) {
                Text("添加说明")
            }
        }
    }
}
