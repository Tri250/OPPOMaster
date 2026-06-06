package com.omaster.app.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omaster.app.data.camera.CameraCompatibilityStatus
import com.omaster.app.data.camera.CameraParamProvider
import com.omaster.app.data.camera.RealTimeCameraParams
import com.omaster.app.data.AppLanguage
import com.omaster.app.data.LanguageManager
import com.omaster.app.data.PreferencesDataStore
import com.omaster.app.data.ThemeMode
import com.omaster.app.data.PresetRepository
import com.omaster.app.data.SearchManager
import com.omaster.app.domain.model.Preset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: PresetRepository,
    private val preferencesDataStore: PreferencesDataStore,
    private val languageManager: LanguageManager,
    private val cameraParamProvider: CameraParamProvider,
    private val searchManager: SearchManager
) : ViewModel() {

    // 从Repository暴露的Flow
    val presets = repository.presets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isLoading = repository.isLoading.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val error = repository.error.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val themeMode = preferencesDataStore.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM.value
    )

    val fluidCloudEnabled = preferencesDataStore.fluidCloudEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val overlayEnabled = preferencesDataStore.overlayEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val syncEnabled = preferencesDataStore.syncEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val selectedLanguage = languageManager.selectedLanguage

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType.asStateFlow()

    private val _selectedPreset = MutableStateFlow<Preset?>(null)
    val selectedPreset: StateFlow<Preset?> = _selectedPreset.asStateFlow()

    // Camera related
    val cameraStatus: LiveData<CameraCompatibilityStatus> = cameraParamProvider.status
    val cameraParams: LiveData<RealTimeCameraParams> = cameraParamProvider.params

    // Search suggestions
    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    // Camera monitoring flag
    private var isCameraMonitoring = false

    // Combined search and filter results
    val filteredPresets = combine(
        presets,
        searchQuery,
        filterType
    ) { presetList, query, filter ->
        searchManager.searchAndFilter(query, filter, presetList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Hot search keywords
    val hotSearchKeywords = searchManager.hotSearchKeywords

    // Search history
    val searchHistory: List<String> get() = searchManager.searchHistory

    init {
        // 初始化时从远程加载预设
        viewModelScope.launch {
            repository.loadPresetsFromRemote()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query

        // Update search suggestions
        viewModelScope.launch {
            _searchSuggestions.value = searchManager.getSearchSuggestions(query, presets.value)
        }

        Timber.d("Search query changed: $query")
    }

    fun onFilterTypeChanged(filterType: FilterType) {
        _filterType.value = filterType
        Timber.d("Filter type changed: $filterType")
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
        _searchSuggestions.value = emptyList()
    }

    fun clearSearchHistory() {
        searchManager.clearSearchHistory()
    }

    /**
     * 按风格筛选
     */
    fun filterByStyle(style: String) {
        viewModelScope.launch {
            val filtered = searchManager.filterByStyle(presets.value, style)
            Timber.d("Filtered by style: $style, count: ${filtered.size}")
        }
    }

    /**
     * 按场景筛选
     */
    fun filterByScene(scene: String) {
        viewModelScope.launch {
            val filtered = searchManager.filterByScene(presets.value, scene)
            Timber.d("Filtered by scene: $scene, count: ${filtered.size}")
        }
    }

    /**
     * 按设备筛选
     */
    fun filterByDevice(device: String) {
        viewModelScope.launch {
            val filtered = searchManager.filterByDevice(presets.value, device)
            Timber.d("Filtered by device: $device, count: ${filtered.size}")
        }
    }

    /**
     * 按摄影师筛选
     */
    fun filterByPhotographer(photographer: String) {
        viewModelScope.launch {
            val filtered = searchManager.filterByPhotographer(presets.value, photographer)
            Timber.d("Filtered by photographer: $photographer, count: ${filtered.size}")
        }
    }

    fun toggleFavorite(preset: Preset) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(preset.id)
                Timber.d("Toggled favorite for preset: ${preset.id}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle favorite")
            }
        }
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

    /**
     * 设置应用语言
     * @param language 目标语言
     */
    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            languageManager.setLanguage(language)
            Timber.d("Language changed: $language")
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
        if (!isCameraMonitoring) {
            cameraParamProvider.startMonitor()
            isCameraMonitoring = true
            Timber.d("Camera monitor started")
        }
    }

    fun stopCameraMonitor() {
        if (isCameraMonitoring) {
            cameraParamProvider.stopMonitor()
            isCameraMonitoring = false
            Timber.d("Camera monitor stopped")
        }
    }

    override fun onCleared() {
        stopCameraMonitor()
        cameraParamProvider.release()
        super.onCleared()
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