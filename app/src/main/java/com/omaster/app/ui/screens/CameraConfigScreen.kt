package com.omaster.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.omaster.app.data.CameraConfigRepository
import com.omaster.app.model.CameraConfig
import com.omaster.app.model.CameraParams
import com.omaster.app.ui.components.CameraParamControls
import com.omaster.app.ui.components.SaveConfigDialog
import com.omaster.app.ui.theme.hasselbladOrange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 相机配置管理ViewModel
 */
@HiltViewModel
class CameraConfigViewModel @Inject constructor(
    private val repository: CameraConfigRepository
) : ViewModel() {
    val configs: StateFlow<List<CameraConfig>> = repository.configs

    private val _selectedConfig = MutableStateFlow<CameraConfig?>(null)
    val selectedConfig: StateFlow<CameraConfig?> = _selectedConfig.asStateFlow()

    private val _currentParams = MutableStateFlow(CameraParams())
    val currentParams: StateFlow<CameraParams> = _currentParams.asStateFlow()

    private val _editingConfigId = MutableStateFlow<String?>(null)
    val editingConfigId: StateFlow<String?> = _editingConfigId.asStateFlow()

    private val _selectedIds = MutableStateFlow<List<String>>(emptyList())
    val selectedIds: StateFlow<List<String>> = _selectedIds.asStateFlow()

    private val _showSaveDialog = MutableStateFlow(false)
    val showSaveDialog: StateFlow<Boolean> = _showSaveDialog.asStateFlow()

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    fun selectConfig(config: CameraConfig) {
        _selectedConfig.value = config
        _currentParams.value = config.params
    }

    fun updateParams(params: CameraParams) {
        _currentParams.value = params
    }

    fun applyConfig(config: CameraConfig) {
        _currentParams.value = config.params
    }

    fun toggleSelection(configId: String) {
        val current = _selectedIds.value.toMutableList()
        if (configId in current) {
            current.remove(configId)
        } else {
            current.add(configId)
        }
        _selectedIds.value = current
    }

    fun clearSelection() {
        _selectedIds.value = emptyList()
    }

    fun showSaveDialog() {
        _showSaveDialog.value = true
    }

    fun hideSaveDialog() {
        _showSaveDialog.value = false
    }

    fun saveConfig(name: String, description: String) = viewModelScope.launch {
        val config = CameraConfig.fromParams(name, _currentParams.value, description)
        repository.addConfig(config)
        _showSaveDialog.value = false
    }

    fun deleteConfig(configId: String) = viewModelScope.launch {
        repository.deleteConfig(configId)
        if (_selectedConfig.value?.id == configId) {
            _selectedConfig.value = null
        }
    }

    fun deleteSelectedConfigs() = viewModelScope.launch {
        repository.deleteConfigs(_selectedIds.value)
        _selectedIds.value = emptyList()
    }

    fun toggleFavorite(configId: String) = viewModelScope.launch {
        repository.toggleFavorite(configId)
    }

    fun setTab(tab: Int) {
        _currentTab.value = tab
    }

    fun setEditingId(id: String?) {
        _editingConfigId.value = id
    }
}

/**
 * 相机配置管理主屏幕
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CameraConfigScreen(
    onBack: () -> Unit,
    viewModel: CameraConfigViewModel = hiltViewModel()
) {
    val configs by viewModel.configs.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val showSaveDialog by viewModel.showSaveDialog.collectAsStateWithLifecycle()
    val currentParams by viewModel.currentParams.collectAsStateWithLifecycle()

    val isEditing = selectedIds.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "已选择 ${selectedIds.size} 个"
                        else "相机配置"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditing) {
                            viewModel.clearSelection()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Close
                            else Icons.Default.ArrowBack,
                            contentDescription = if (isEditing) "取消" else "返回"
                        )
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { viewModel.deleteSelectedConfigs() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (!isEditing) {
                FloatingActionButton(
                    onClick = { viewModel.showSaveDialog() },
                    containerColor = hasselbladOrange
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新建配置"
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = currentTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { viewModel.setTab(0) },
                    text = { Text("配置列表") }
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { viewModel.setTab(1) },
                    text = { Text("参数调整") }
                )
            }

            AnimatedVisibility(
                visible = currentTab == 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ConfigListContent(
                    configs = configs,
                    selectedIds = selectedIds,
                    onConfigClick = { config ->
                        if (isEditing) {
                            viewModel.toggleSelection(config.id)
                        } else {
                            viewModel.applyConfig(config)
                        }
                    },
                    onConfigLongClick = { config ->
                        viewModel.toggleSelection(config.id)
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onDelete = { viewModel.deleteConfig(it) }
                )
            }

            AnimatedVisibility(
                visible = currentTab == 1,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ParamEditContent(
                    params = currentParams,
                    onParamsChanged = { viewModel.updateParams(it) },
                    onSave = { viewModel.showSaveDialog() }
                )
            }
        }
    }

    if (showSaveDialog) {
        SaveConfigDialog(
            onDismiss = { viewModel.hideSaveDialog() },
            onSave = { name, description ->
                viewModel.saveConfig(name, description)
            }
        )
    }
}

/**
 * 配置列表内容
 */
@Composable
private fun ConfigListContent(
    configs: List<CameraConfig>,
    selectedIds: List<String>,
    onConfigClick: (CameraConfig) -> Unit,
    onConfigLongClick: (CameraConfig) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val favorites = remember(configs) {
        configs.filter { it.isFavorite }
    }
    val others = remember(configs) {
        configs.filter { !it.isFavorite }
    }

    val isEditing = selectedIds.isNotEmpty()

    if (configs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "还没有配置",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "点击右下角按钮创建第一个配置",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (favorites.isNotEmpty()) {
                item {
                    Text(
                        text = "收藏",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(favorites, key = { it.id }) { config ->
                    ConfigCard(
                        config = config,
                        isSelected = config.id in selectedIds,
                        isEditing = isEditing,
                        onClick = { onConfigClick(config) },
                        onLongClick = { onConfigLongClick(config) },
                        onToggleFavorite = { onToggleFavorite(config.id) },
                        onDelete = { onDelete(config.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "所有配置",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(others, key = { it.id }) { config ->
                ConfigCard(
                    config = config,
                    isSelected = config.id in selectedIds,
                    isEditing = isEditing,
                    onClick = { onConfigClick(config) },
                    onLongClick = { onConfigLongClick(config) },
                    onToggleFavorite = { onToggleFavorite(config.id) },
                    onDelete = { onDelete(config.id) }
                )
            }
        }
    }
}

/**
 * 配置卡片
 */
@Composable
private fun ConfigCard(
    config: CameraConfig,
    isSelected: Boolean,
    isEditing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) hasselbladOrange.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(
            2.dp,
            hasselbladOrange
        ) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = config.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (config.description.isNotEmpty()) {
                        Text(
                            text = config.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row {
                    if (!isEditing) {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (config.isFavorite)
                                    Icons.Default.Favorite
                                else
                                    Icons.Default.FavoriteBorder,
                                contentDescription = if (config.isFavorite)
                                    "取消收藏" else "收藏",
                                tint = if (config.isFavorite)
                                    hasselbladOrange
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 参数摘要
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ParamChip(label = "ISO", value = config.params.iso.toString())
                ParamChip(label = "快门", value = config.params.shutter)
                ParamChip(label = "EV", value = config.params.ev)
                ParamChip(label = "WB", value = config.params.wb)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (config.tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    config.tags.take(3).forEach { tag ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(tag, fontSize = 12.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = config.getFormattedDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 参数标签
 */
@Composable
private fun ParamChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/**
 * 参数编辑内容
 */
@Composable
private fun ParamEditContent(
    params: CameraParams,
    onParamsChanged: (CameraParams) -> Unit,
    onSave: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 当前参数预览
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "当前参数",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = params.formatParamsForDisplay(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = hasselbladOrange
                    )
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("保存配置")
                }
            }
        }

        // 参数控制
        CameraParamControls(
            params = params,
            onParamsChanged = onParamsChanged,
            modifier = Modifier.weight(1f)
        )
    }
}
