package com.omaster.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaster.app.viewmodel.FILTER_OPTIONS
import com.omaster.app.viewmodel.ISO_OPTIONS
import com.omaster.app.viewmodel.PresetEditorViewModel
import com.omaster.app.viewmodel.SCENE_OPTIONS
import com.omaster.app.viewmodel.SHUTTER_OPTIONS
import com.omaster.app.viewmodel.WB_OPTIONS

@Composable
fun PresetEditorScreen(
    onBack: () -> Unit,
    onSave: () -> Unit,
    presetId: String? = null,
    viewModel: PresetEditorViewModel = hiltViewModel()
) {
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    
    LaunchedEffect(presetId) {
        presetId?.let { viewModel.loadPreset(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (presetId != null) "编辑预设" else "创建预设") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (viewModel.savePreset()) {
                                onSave()
                            }
                        }
                    ) {
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
        ) {
            SectionTitle("基本信息")
            
            TextField(
                value = editorState.name,
                onValueChange = { viewModel.setPresetName(it) },
                label = { Text("预设名称") },
                placeholder = { Text("输入预设名称...") },
                modifier = Modifier.padding(horizontal = 16.dp),
                leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) }
            )

            TextField(
                value = editorState.author,
                onValueChange = { viewModel.setAuthor(it) },
                label = { Text("作者") },
                placeholder = { Text("输入作者名称...") },
                modifier = Modifier.padding(horizontal = 16.dp),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )

            SectionTitle("分类设置")
            
            CategoryDropdown(
                label = "场景",
                options = SCENE_OPTIONS,
                selected = editorState.scene,
                onSelected = { viewModel.setScene(it) }
            )

            CategoryDropdown(
                label = "滤镜风格",
                options = FILTER_OPTIONS,
                selected = editorState.filter,
                onSelected = { viewModel.setFilter(it) }
            )

            TagSelector(
                tags = editorState.tags,
                availableTags = listOf("人像", "风景", "夜景", "美食", "街拍", "胶片", "复古", "清新", "黑白", "自然"),
                onTagsChanged = { viewModel.setTags(it) }
            )

            SectionTitle("相机参数")
            
            ParameterDropdown(
                label = "ISO",
                options = ISO_OPTIONS.map { it.toString() },
                selected = editorState.iso.toString(),
                onSelected = { viewModel.setISO(it.toInt()) }
            )

            ParameterDropdown(
                label = "快门速度",
                options = SHUTTER_OPTIONS,
                selected = editorState.shutter,
                onSelected = { viewModel.setShutter(it) }
            )

            ParameterDropdown(
                label = "白平衡",
                options = WB_OPTIONS,
                selected = editorState.wb,
                onSelected = { viewModel.setWB(it) }
            )

            EVSlider(
                value = editorState.ev,
                onValueChange = { viewModel.setEV(it) }
            )

            TextField(
                value = editorState.deviceModel,
                onValueChange = { viewModel.setDeviceModel(it) },
                label = { Text("适配机型") },
                placeholder = { Text("如：Find X8 Pro") },
                modifier = Modifier.padding(horizontal = 16.dp),
                leadingIcon = { Icon(Icons.Default.DeviceMobile, contentDescription = null) }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun CategoryDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                        selected = option == selected
                    )
                }
            }
        }
    }
}

@Composable
private fun ParameterDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                        selected = option == selected
                    )
                }
            }
        }
    }
}

@Composable
private fun EVSlider(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "曝光补偿 (EV)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = String.format("%.1f", value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -3f..3f,
            steps = 11,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("-3", style = MaterialTheme.typography.caption)
            Text("0", style = MaterialTheme.typography.caption)
            Text("+3", style = MaterialTheme.typography.caption)
        }
    }
}

@Composable
private fun TagSelector(
    tags: List<String>,
    availableTags: List<String>,
    onTagsChanged: (List<String>) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "标签",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableTags.forEach { tag ->
                val isSelected = tags.contains(tag)
                
                AssistChip(
                    onClick = {
                        val newTags = if (isSelected) {
                            tags - tag
                        } else {
                            tags + tag
                        }
                        onTagsChanged(newTags)
                    },
                    label = { Text(tag) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        labelColor = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                )
            }
        }
    }
}