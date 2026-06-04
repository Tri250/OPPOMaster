package com.omaster.app.viewmodel

import androidx.lifecycle.Observer
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: PresetRepository,
    private val preferencesDataStore: PreferencesDataStore,
    private val cameraParamProvider: CameraParamProvider
) : ViewModel() {
    // Repository Flow exposed with stateIn for optimization
    val presets = repository.presets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType.asStateFlow()

    private val _selectedPreset = MutableStateFlow<Preset?>(null)
    val selectedPreset: StateFlow<Preset?> = _selectedPreset.asStateFlow()

    // Error state Flow
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    // Loading state Flow
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Camera related - Convert LiveData to StateFlow for consistency
    private val _cameraStatus = MutableStateFlow(CameraCompatibilityStatus.NotSupported)
    val cameraStatus: StateFlow<CameraCompatibilityStatus> = _cameraStatus.asStateFlow()
    
    private val _cameraParams = MutableStateFlow(RealTimeCameraParams())
    val cameraParams: StateFlow<RealTimeCameraParams> = _cameraParams.asStateFlow()

    // Camera monitoring flag
    private var isCameraMonitoring = false

    // 保存 LiveData 观察者引用，便于 onCleared 时清理，避免内存泄漏
    private val statusObserver = Observer<CameraCompatibilityStatus> { status ->
        _cameraStatus.value = status
    }
    private val paramsObserver = Observer<RealTimeCameraParams> { params ->
        _cameraParams.value = params
    }

    // Observe LiveData and update StateFlow
    init {
        cameraParamProvider.status.observeForever(statusObserver)
        cameraParamProvider.params.observeForever(paramsObserver)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        Timber.d("Search query changed: $query")
    }

    fun onFilterTypeChanged(filterType: FilterType) {
        _filterType.value = filterType
        Timber.d("Filter type changed: $filterType")
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
            try {
                preferencesDataStore.setThemeMode(themeMode)
                Timber.d("Theme mode changed: $themeMode")
            } catch (e: Exception) {
                Timber.e(e, "Failed to set theme mode")
            }
        }
    }

    fun setFluidCloudEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesDataStore.setFluidCloudEnabled(enabled)
                Timber.d("Fluid cloud enabled: $enabled")
            } catch (e: Exception) {
                Timber.e(e, "Failed to set fluid cloud enabled")
            }
        }
    }

    fun setOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesDataStore.setOverlayEnabled(enabled)
                Timber.d("Overlay enabled: $enabled")
            } catch (e: Exception) {
                Timber.e(e, "Failed to set overlay enabled")
            }
        }
    }

    fun setSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesDataStore.setSyncEnabled(enabled)
                Timber.d("Sync enabled: $enabled")
            } catch (e: Exception) {
                Timber.e(e, "Failed to set sync enabled")
            }
        }
    }

    fun syncPresets() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _syncError.value = null
                repository.syncPresets()
                Timber.d("Presets synced successfully")
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync presets")
                _syncError.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
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
        // 移除 LiveData 观察者，避免内存泄漏
        cameraParamProvider.status.removeObserver(statusObserver)
        cameraParamProvider.params.removeObserver(paramsObserver)
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