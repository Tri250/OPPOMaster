package com.omaster.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.model.Section
import com.omaster.app.ui.theme.AccentPrimary
import com.omaster.app.ui.theme.DeepSpace
import java.util.UUID

@Composable
fun CreatePresetScreen(
    onBack: () -> Unit,
    onSave: (Preset) -> Unit,
    editPreset: Preset? = null,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(editPreset?.name ?: "") }
    var coverPath by remember { mutableStateOf(editPreset?.coverPath ?: "custom_${UUID.randomUUID()}") }
    var deviceModel by remember { mutableStateOf(editPreset?.deviceModel ?: "") }
    
    var filter by remember { mutableStateOf(editPreset?.cameraParams?.filter ?: "") }
    var iso by remember { mutableStateOf(editPreset?.cameraParams?.iso?.toString() ?: "100") }
    var shutter by remember { mutableStateOf(editPreset?.cameraParams?.shutter ?: "1/125") }
    var ev by remember { mutableStateOf(editPreset?.cameraParams?.ev ?: "0") }
    var wb by remember { mutableStateOf(editPreset?.cameraParams?.wb ?: "5500K") }
    
    var softLight by remember { mutableStateOf(editPreset?.cameraParams?.softLight?.toString() ?: "0") }
    var tone by remember { mutableStateOf(editPreset?.cameraParams?.tone?.toString() ?: "0") }
    var saturation by remember { mutableStateOf(editPreset?.cameraParams?.saturation?.toString() ?: "0") }
    var warmth by remember { mutableStateOf(editPreset?.cameraParams?.warmth?.toString() ?: "0") }
    var cyanMagenta by remember { mutableStateOf(editPreset?.cameraParams?.cyanMagenta?.toString() ?: "0") }
    var sharpness by remember { mutableStateOf(editPreset?.cameraParams?.sharpness?.toString() ?: "0") }
    var vignetting by remember { mutableStateOf(editPreset?.cameraParams?.vignetting?.toString() ?: "0") }
    
    var tagsText by remember { mutableStateOf(editPreset?.tags?.joinToString(",") ?: "") }
    var isHncs by remember { mutableStateOf(editPreset?.cameraParams?.hasselblad_hncs ?: false) }
    
    val isFormValid = name.isNotBlank()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (editPreset != null) "编辑预设" else "创建预设",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val cameraParams = CameraParams(
                                filter = filter,
                                iso = iso.toIntOrNull() ?: 100,
                                shutter = shutter,
                                ev = ev,
                                wb = wb,
                                hasselblad_hncs = isHncs,
                                softLight = softLight.toIntOrNull() ?: 0,
                                tone = tone.toIntOrNull() ?: 0,
                                saturation = saturation.toIntOrNull() ?: 0,
                                warmth = warmth.toIntOrNull() ?: 0,
                                cyanMagenta = cyanMagenta.toIntOrNull() ?: 0,
                                sharpness = sharpness.toIntOrNull() ?: 0,
                                vignetting = vignetting.toIntOrNull() ?: 0
                            )
                            
                            val preset = Preset(
                                id = editPreset?.id ?: UUID.randomUUID().toString(),
                                name = name,
                                coverPath = coverPath,
                                cameraParams = cameraParams,
                                deviceModel = deviceModel,
                                source = "custom",
                                isCustom = true,
                                tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            )
                            onSave(preset)
                            onBack()
                        },
                        enabled = isFormValid
                    ) {
                        Icon(Icons.Default.Check, "保存")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = deviceModel,
                onValueChange = { deviceModel = it },
                label = { Text("适配机型（可选）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                label = { Text("标签（逗号分隔）") },
                placeholder = { Text("如：风景、人像、复古") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Divider()

            Text(
                text = "基础参数",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                label = { Text("滤镜风格") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = iso,
                    onValueChange = { iso = it },
                    label = { Text("ISO") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = shutter,
                    onValueChange = { shutter = it },
                    label = { Text("快门") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = ev,
                    onValueChange = { ev = it },
                    label = { Text("曝光补偿") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = wb,
                    onValueChange = { wb = it },
                    label = { Text("白平衡") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isHncs,
                    onCheckedChange = { isHncs = it },
                    colors = CheckboxDefaults.colors(checkedColor = AccentPrimary)
                )
                Text("哈苏认证 HNCS")
            }

            Divider()

            Text(
                text = "高级参数",
                style = MaterialTheme.typography.headlineSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = softLight,
                    onValueChange = { softLight = it },
                    label = { Text("柔光") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = tone,
                    onValueChange = { tone = it },
                    label = { Text("影调") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = saturation,
                    onValueChange = { saturation = it },
                    label = { Text("饱和度") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = warmth,
                    onValueChange = { warmth = it },
                    label = { Text("冷暖") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = cyanMagenta,
                    onValueChange = { cyanMagenta = it },
                    label = { Text("青品") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = sharpness,
                    onValueChange = { sharpness = it },
                    label = { Text("锐度") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = vignetting,
                onValueChange = { vignetting = it },
                label = { Text("暗角") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
