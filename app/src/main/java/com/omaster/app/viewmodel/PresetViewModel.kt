package com.omaster.app.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omaster.app.data.PreferencesDataStore
import com.omaster.app.data.PresetRepository
import com.omaster.app.feature.aifeature.PresetRecommender
import com.omaster.app.feature.aifeature.SceneRecognitionEngine
import com.omaster.app.feature.aifeature.model.RecommendedPreset
import com.omaster.app.feature.aifeature.model.SceneAnalysis
import com.omaster.app.feature.aifeature.model.SkillLevel
import com.omaster.app.feature.aifeature.model.UserProfile
import com.omaster.app.feature.editor.ImageAdjustments
import com.omaster.app.feature.editor.ImageProcessingPipeline
import com.omaster.app.model.Preset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class PresetUiState(
    val presets: List<Preset> = emptyList(),
    val filteredPresets: List<Preset> = emptyList(),
    val selectedPreset: Preset? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val filterType: FilterType = FilterType.ALL,
    val aiRecommendations: List<RecommendedPreset> = emptyList(),
    val sceneAnalysis: SceneAnalysis? = null,
    val imageAdjustments: ImageAdjustments = ImageAdjustments(),
    val processedBitmap: Bitmap? = null
)

sealed class PresetUiEvent {
    data class LoadPresets(val filterType: FilterType? = null) : PresetUiEvent()
    data class SearchPresets(val query: String) : PresetUiEvent()
    data class SelectPreset(val preset: Preset) : PresetUiEvent()
    data class ToggleFavorite(val presetId: String) : PresetUiEvent()
    data class AnalyzeImage(val bitmap: Bitmap) : PresetUiEvent()
    data class ApplyImageAdjustments(val adjustments: ImageAdjustments) : PresetUiEvent()
    data class ProcessImage(val bitmap: Bitmap, val adjustments: ImageAdjustments) : PresetUiEvent()
    data class SetFilter(val filterType: FilterType) : PresetUiEvent()
    object ClearError : PresetUiEvent()
}

@HiltViewModel
class PresetViewModel @Inject constructor(
    private val repository: PresetRepository,
    private val preferencesDataStore: PreferencesDataStore,
    private val sceneRecognitionEngine: SceneRecognitionEngine,
    private val presetRecommender: PresetRecommender,
    private val imageProcessingPipeline: ImageProcessingPipeline
) : ViewModel() {

    private val _uiState = MutableStateFlow(PresetUiState())
    val uiState: StateFlow<PresetUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeSampleData()
        }
        observePresets()
    }

    private fun observePresets() {
        viewModelScope.launch {
            repository.presets.collect { presets ->
                _uiState.update { state ->
                    state.copy(
                        presets = presets,
                        filteredPresets = applyFilterAndSearch(presets, state.filterType, state.searchQuery)
                    )
                }
            }
        }
    }

    fun onEvent(event: PresetUiEvent) {
        when (event) {
            is PresetUiEvent.LoadPresets -> loadPresets(event.filterType)
            is PresetUiEvent.SearchPresets -> searchPresets(event.query)
            is PresetUiEvent.SelectPreset -> selectPreset(event.preset)
            is PresetUiEvent.ToggleFavorite -> toggleFavorite(event.presetId)
            is PresetUiEvent.AnalyzeImage -> analyzeImage(event.bitmap)
            is PresetUiEvent.ApplyImageAdjustments -> applyImageAdjustments(event.adjustments)
            is PresetUiEvent.ProcessImage -> processImage(event.bitmap, event.adjustments)
            is PresetUiEvent.SetFilter -> setFilter(event.filterType)
            is PresetUiEvent.ClearError -> clearError()
        }
    }

    private fun loadPresets(filterType: FilterType?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val type = filterType ?: _uiState.value.filterType
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        filterType = type,
                        filteredPresets = applyFilterAndSearch(state.presets, type, state.searchQuery)
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                Timber.e(e, "加载预设失败")
            }
        }
    }

    private fun searchPresets(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredPresets = applyFilterAndSearch(state.presets, state.filterType, query)
            )
        }
    }

    private fun selectPreset(preset: Preset) {
        viewModelScope.launch {
            repository.updateLastAccessed(preset.id)
            _uiState.update { it.copy(selectedPreset = preset) }
        }
    }

    private fun toggleFavorite(presetId: String) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(presetId)
                Timber.d("切换收藏状态: $presetId")
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
                Timber.e(e, "切换收藏失败")
            }
        }
    }

    private fun analyzeImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val sceneAnalysis = sceneRecognitionEngine.analyzeScene(bitmap)
                
                val userProfile = UserProfile(
                    userId = "default_user",
                    preferredStyles = emptyMap(),
                    usageHistory = emptyList(),
                    deviceModel = "OPPO",
                    skillLevel = SkillLevel.BEGINNER
                )
                
                val recommendations = presetRecommender.recommendPresets(
                    sceneAnalysis,
                    userProfile,
                    _uiState.value.presets
                )
                
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        sceneAnalysis = sceneAnalysis,
                        aiRecommendations = recommendations
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                Timber.e(e, "图像分析失败")
            }
        }
    }

    private fun applyImageAdjustments(adjustments: ImageAdjustments) {
        _uiState.update { it.copy(imageAdjustments = adjustments) }
    }

    private fun processImage(bitmap: Bitmap, adjustments: ImageAdjustments) {
        viewModelScope.launch {
            try {
                val processed = imageProcessingPipeline.applyPresetRealtime(bitmap, adjustments)
                _uiState.update { it.copy(processedBitmap = processed) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
                Timber.e(e, "图像处理失败")
            }
        }
    }

    private fun setFilter(filterType: FilterType) {
        _uiState.update { state ->
            state.copy(
                filterType = filterType,
                filteredPresets = applyFilterAndSearch(state.presets, filterType, state.searchQuery)
            )
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun applyFilterAndSearch(
        presets: List<Preset>,
        filterType: FilterType,
        searchQuery: String
    ): List<Preset> {
        var filtered = when (filterType) {
            FilterType.ALL -> presets
            FilterType.FAVORITES -> presets.filter { it.isFavorite }
            FilterType.HNCS -> presets.filter { it.cameraParams?.hasselblad_hncs == true }
            FilterType.FIND_X -> presets.filter { it.deviceModel.contains("Find X", ignoreCase = true) }
            FilterType.RENO -> presets.filter { it.deviceModel.contains("Reno", ignoreCase = true) }
        }

        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.deviceModel.contains(searchQuery, ignoreCase = true)
            }
        }

        return filtered
    }

    override fun onCleared() {
        super.onCleared()
        sceneRecognitionEngine.close()
        imageProcessingPipeline.release()
    }
}
