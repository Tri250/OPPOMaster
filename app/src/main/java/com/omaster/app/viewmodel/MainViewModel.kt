package com.omaster.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {
    val presets = repository.presets
    val themeMode = preferencesDataStore.themeMode
    val fluidCloudEnabled = preferencesDataStore.fluidCloudEnabled
    val overlayEnabled = preferencesDataStore.overlayEnabled

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType.asStateFlow()

    private val _selectedPreset = MutableStateFlow<Preset?>(null)
    val selectedPreset: StateFlow<Preset?> = _selectedPreset.asStateFlow()

    private val _isLoadingCommunityPresets = MutableStateFlow(false)
    val isLoadingCommunityPresets: StateFlow<Boolean> = _isLoadingCommunityPresets.asStateFlow()

    private val _communityPresetsLoaded = MutableStateFlow(false)
    val communityPresetsLoaded: StateFlow<Boolean> = _communityPresetsLoaded.asStateFlow()

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

    fun loadCommunityPresets() {
        viewModelScope.launch {
            if (_communityPresetsLoaded.value) {
                Timber.d("Community presets already loaded, skipping")
                return@launch
            }
            
            _isLoadingCommunityPresets.value = true
            try {
                val loadedPresets = repository.loadCommunityPresets()
                _communityPresetsLoaded.value = true
                Timber.d("Successfully loaded ${loadedPresets.size} community presets")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load community presets")
            } finally {
                _isLoadingCommunityPresets.value = false
            }
        }
    }

    fun refreshCommunityPresets() {
        viewModelScope.launch {
            _isLoadingCommunityPresets.value = true
            try {
                repository.clearCommunityCache()
                val loadedPresets = repository.refreshCommunityPresets()
                Timber.d("Successfully refreshed ${loadedPresets.size} community presets")
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh community presets")
            } finally {
                _isLoadingCommunityPresets.value = false
            }
        }
    }
}

enum class FilterType {
    ALL,
    FAVORITES,
    HNCS,
    FIND_X,
    RENO,
    COMMUNITY
}
