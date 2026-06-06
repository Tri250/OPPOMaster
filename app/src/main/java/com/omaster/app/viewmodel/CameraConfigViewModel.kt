package com.omaster.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omaster.app.data.CameraConfigRepository
import com.omaster.app.domain.model.CameraConfig
import com.omaster.app.domain.model.CameraParams
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