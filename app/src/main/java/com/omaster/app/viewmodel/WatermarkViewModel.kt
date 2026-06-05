package com.omaster.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omaster.app.data.PreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatermarkViewModel @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {
    
    // 水印配置Flow
    val watermarkTemplate = preferencesDataStore.watermarkTemplate
        .stateIn(viewModelScope, SharingStarted.Eagerly, "HASSELBLAD")
    
    val watermarkPosition = preferencesDataStore.watermarkPosition
        .stateIn(viewModelScope, SharingStarted.Eagerly, "BOTTOM_RIGHT")
    
    val watermarkOpacity = preferencesDataStore.watermarkOpacity
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.8f)
    
    val watermarkShowTimestamp = preferencesDataStore.watermarkShowTimestamp
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    
    val watermarkShowDevice = preferencesDataStore.watermarkShowDevice
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    
    val watermarkCustomText = preferencesDataStore.watermarkCustomText
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    
    val watermarkTextSize = preferencesDataStore.watermarkTextSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)
    
    // 保存水印配置
    fun saveWatermarkConfig(
        template: String,
        position: String,
        opacity: Float,
        showTimestamp: Boolean,
        showDevice: Boolean,
        customText: String,
        textSize: Float
    ) {
        viewModelScope.launch {
            preferencesDataStore.saveWatermarkConfig(
                template = template,
                position = position,
                opacity = opacity,
                showTimestamp = showTimestamp,
                showDevice = showDevice,
                customText = customText,
                textSize = textSize
            )
        }
    }
}