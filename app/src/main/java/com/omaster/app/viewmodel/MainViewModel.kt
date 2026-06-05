package com.omaster.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omaster.app.camera.CameraCompatibilityStatus
import com.omaster.app.camera.CameraParamProvider
import com.omaster.app.camera.RealTimeCameraParams
import com.omaster.app.data.PreferencesDataStore
import com.omaster.app.data.ThemeMode
import com.omaster.app.data.PresetRepository
import com.omaster.app.model.Preset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: PresetRepository,
    private val preferencesDataStore: PreferencesDataStore,
    private val cameraParamProvider: CameraParamProvider
) : ViewModel() {
    val presets = repository.presets
    val themeMode = preferencesDataStore.themeMode
    val fluidCloudEnabled = preferencesDataStore.fluidCloudEnabled
    val overlayEnabled = preferencesDataStore.overlayEnabled
    val syncEnabled = preferencesDataStore.syncEnabled
    val favoritePresets = preferencesDataStore.favoritePresets

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // 用于实际筛选的搜索查询（带防抖）
    private val _debouncedSearchQuery = MutableStateFlow("")
    val debouncedSearchQuery: StateFlow<String> = _debouncedSearchQuery.asStateFlow()

    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType.asStateFlow()

    private val _selectedPreset = MutableStateFlow<Preset?>(null)
    val selectedPreset: StateFlow<Preset?> = _selectedPreset.asStateFlow()
    
    // 收藏操作反馈消息
    private val _favoriteMessage = MutableStateFlow<String?>(null)
    val favoriteMessage: StateFlow<String?> = _favoriteMessage.asStateFlow()

    // Camera related
    val cameraStatus: LiveData<CameraCompatibilityStatus> = cameraParamProvider.status
    val cameraParams: LiveData<RealTimeCameraParams> = cameraParamProvider.params
    
    // 搜索最大长度限制
    private val MAX_SEARCH_LENGTH = 50
    
    init {
        // 设置搜索防抖（250ms延迟，符合 < 300ms 要求）
        _searchQuery
            .debounce(250)
            .onEach { query ->
                _debouncedSearchQuery.value = query
                Timber.d("Debounced search query: $query")
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        // 截断超长字符串
        val truncatedQuery = if (query.length > MAX_SEARCH_LENGTH) {
            query.take(MAX_SEARCH_LENGTH)
        } else {
            query
        }
        _searchQuery.value = truncatedQuery
        Timber.d("Search query changed: $truncatedQuery (original: ${query.length})")
    }

    fun onFilterTypeChanged(filterType: FilterType) {
        _filterType.value = filterType
        Timber.d("Filter type changed: $filterType")
    }

    fun toggleFavorite(preset: Preset) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(preset.id)
                val newState = !preset.isFavorite
                _favoriteMessage.value = if (newState) "已收藏" else "已取消收藏"
                Timber.d("Toggled favorite for preset: ${preset.id}, new state: $newState")
                
                // 3秒后清除消息
                kotlinx.coroutines.delay(3000)
                _favoriteMessage.value = null
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle favorite")
                _favoriteMessage.value = "收藏操作失败"
            }
        }
    }
    
    fun clearFavoriteMessage() {
        _favoriteMessage.value = null
    }

    fun selectPreset(preset: Preset) {
        _selectedPreset.value = preset
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            preferencesDataStore.setThemeMode(themeMode)
            Timber.d("Theme mode changed: $themeMode")
        }
    }

    fun setFluidCloudEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setFluidCloudEnabled(enabled)
            Timber.d("Fluid cloud enabled: $enabled")
        }
    }

    fun setOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setOverlayEnabled(enabled)
            Timber.d("Overlay enabled: $enabled")
        }
    }

    fun setSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setSyncEnabled(enabled)
            Timber.d("Sync enabled: $enabled")
        }
    }

    fun syncPresets() {
        viewModelScope.launch {
            try {
                repository.syncPresets()
                Timber.d("Presets synced successfully")
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync presets")
            }
        }
    }

    // Camera related functions
    fun startCameraMonitor() {
        cameraParamProvider.startMonitor()
        Timber.d("Camera monitor started")
    }

    fun stopCameraMonitor() {
        cameraParamProvider.stopMonitor()
        Timber.d("Camera monitor stopped")
    }

    override fun onCleared() {
        super.onCleared()
        cameraParamProvider.release()
    }
}

enum class FilterType {
    ALL,
    FAVORITES,
    HNCS,
    FIND_X,
    RENO,
    NEW,
    TRENDING
}