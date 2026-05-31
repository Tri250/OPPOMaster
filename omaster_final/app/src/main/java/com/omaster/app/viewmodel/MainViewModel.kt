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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType.asStateFlow()

    private val _selectedPreset = MutableStateFlow<Preset?>(null)
    val selectedPreset: StateFlow<Preset?> = _selectedPreset.asStateFlow()

    // Camera related
    val cameraStatus: LiveData<CameraCompatibilityStatus> = cameraParamProvider.status
    val cameraParams: LiveData<RealTimeCameraParams> = cameraParamProvider.params

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