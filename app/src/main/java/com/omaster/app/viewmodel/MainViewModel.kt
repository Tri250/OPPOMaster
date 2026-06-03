package com.omaster.app.viewmodel

import androidx.annotation.Keep
import androidx.annotation.MainThread
import androidx.annotation.NonNull
import androidx.annotation.Nullable
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

@Keep
@HiltViewModel
class MainViewModel @Inject constructor(
    @NonNull private val repository: PresetRepository,
    @NonNull private val preferencesDataStore: PreferencesDataStore,
    @NonNull private val cameraParamProvider: CameraParamProvider
) : ViewModel() {
    @NonNull val presets = repository.presets
    @NonNull val themeMode = preferencesDataStore.themeMode
    @NonNull val fluidCloudEnabled = preferencesDataStore.fluidCloudEnabled
    @NonNull val overlayEnabled = preferencesDataStore.overlayEnabled
    @NonNull val syncEnabled = preferencesDataStore.syncEnabled

    private val _searchQuery = MutableStateFlow("")
    @NonNull val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow(FilterType.ALL)
    @NonNull val filterType: StateFlow<FilterType> = _filterType.asStateFlow()

    private val _selectedPreset = MutableStateFlow<Preset?>(null)
    @NonNull val selectedPreset: StateFlow<Preset?> = _selectedPreset.asStateFlow()

    // Camera related
    @NonNull val cameraStatus: LiveData<CameraCompatibilityStatus> = cameraParamProvider.status
    @NonNull val cameraParams: LiveData<RealTimeCameraParams> = cameraParamProvider.params

    @MainThread
    fun onSearchQueryChanged(@NonNull query: String) {
        _searchQuery.value = query
        Timber.d("Search query changed: $query")
    }

    @MainThread
    fun onFilterTypeChanged(@NonNull filterType: FilterType) {
        _filterType.value = filterType
        Timber.d("Filter type changed: $filterType")
    }

    @MainThread
    fun toggleFavorite(@NonNull preset: Preset) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(preset.id)
                Timber.d("Toggled favorite for preset: ${preset.id}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle favorite")
            }
        }
    }

    @MainThread
    fun selectPreset(@NonNull preset: Preset) {
        _selectedPreset.value = preset
    }

    @MainThread
    fun setThemeMode(@NonNull themeMode: ThemeMode) {
        viewModelScope.launch {
            preferencesDataStore.setThemeMode(themeMode)
            Timber.d("Theme mode changed: $themeMode")
        }
    }

    @MainThread
    fun setFluidCloudEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setFluidCloudEnabled(enabled)
            Timber.d("Fluid cloud enabled: $enabled")
        }
    }

    @MainThread
    fun setOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setOverlayEnabled(enabled)
            Timber.d("Overlay enabled: $enabled")
        }
    }

    @MainThread
    fun setSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.setSyncEnabled(enabled)
            Timber.d("Sync enabled: $enabled")
        }
    }

    @MainThread
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
    @MainThread
    fun startCameraMonitor() {
        cameraParamProvider.startMonitor()
        Timber.d("Camera monitor started")
    }

    @MainThread
    fun stopCameraMonitor() {
        cameraParamProvider.stopMonitor()
        Timber.d("Camera monitor stopped")
    }

    @MainThread
    override fun onCleared() {
        super.onCleared()
        cameraParamProvider.release()
    }
}

@Keep
enum class FilterType {
    ALL,
    FAVORITES,
    HNCS,
    FIND_X,
    RENO,
    NEW,
    TRENDING
}