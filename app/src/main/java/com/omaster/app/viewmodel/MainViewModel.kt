package com.omaster.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omaster.app.data.PreferencesDataStore
import com.omaster.app.data.ThemeMode
import com.omaster.app.data.PresetRepository
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import com.omaster.app.model.Section
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()

    init {
        loadCustomPresets()
    }

    fun loadCustomPresets() {
        viewModelScope.launch {
            repository.loadCustomPresets()
        }
    }

    fun getAllTags(): List<String> {
        return repository.getAllTags()
    }

    fun toggleTag(tag: String) {
        val currentTags = _selectedTags.value.toMutableSet()
        if (currentTags.contains(tag)) {
            currentTags.remove(tag)
        } else {
            currentTags.add(tag)
        }
        _selectedTags.value = currentTags
    }

    fun clearTags() {
        _selectedTags.value = emptySet()
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

    suspend fun createCustomPreset(
        name: String,
        coverPath: String,
        cameraParams: CameraParams,
        sections: List<Section> = emptyList(),
        deviceModel: String = "",
        tags: List<String> = emptyList()
    ): Preset {
        val preset = Preset(
            id = "",
            name = name,
            coverPath = coverPath,
            sections = sections,
            cameraParams = cameraParams,
            deviceModel = deviceModel,
            source = "custom",
            tags = tags
        )
        repository.createCustomPreset(preset)
        return presets.firstOrNull()?.lastOrNull() ?: preset
    }

    suspend fun updateCustomPreset(preset: Preset) {
        repository.updateCustomPreset(preset)
    }

    suspend fun deleteCustomPreset(presetId: String) {
        repository.deleteCustomPreset(presetId)
    }

    suspend fun applyPreset(preset: Preset) {
        repository.incrementUsageCount(preset.id)
    }

    suspend fun exportPreset(preset: Preset): String {
        return repository.exportPreset(preset)
    }

    suspend fun importPreset(jsonString: String): Preset? {
        return repository.importPreset(jsonString)
    }
}

enum class FilterType {
    ALL,
    FAVORITES,
    HNCS,
    FIND_X,
    RENO,
    CUSTOM,
    RECENT
}