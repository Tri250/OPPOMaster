package com.omaster.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omaster.app.data.db.DatabaseProvider
import com.omaster.app.data.db.entity.CameraPresetEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PresetEditorViewModel @Inject constructor(
    private val databaseProvider: DatabaseProvider
) : ViewModel() {
    private val _editorState = MutableStateFlow(PresetEditorState())
    val editorState: StateFlow<PresetEditorState> = _editorState.asStateFlow()

    fun setPresetName(name: String) {
        _editorState.value = _editorState.value.copy(name = name)
    }

    fun setAuthor(author: String) {
        _editorState.value = _editorState.value.copy(author = author)
    }

    fun setScene(scene: String) {
        _editorState.value = _editorState.value.copy(scene = scene)
    }

    fun setFilter(filter: String) {
        _editorState.value = _editorState.value.copy(filter = filter)
    }

    fun setTags(tags: List<String>) {
        _editorState.value = _editorState.value.copy(tags = tags)
    }

    fun setISO(iso: Int) {
        _editorState.value = _editorState.value.copy(iso = iso)
    }

    fun setShutter(shutter: String) {
        _editorState.value = _editorState.value.copy(shutter = shutter)
    }

    fun setEV(ev: Float) {
        _editorState.value = _editorState.value.copy(ev = ev)
    }

    fun setWB(wb: String) {
        _editorState.value = _editorState.value.copy(wb = wb)
    }

    fun setDeviceModel(deviceModel: String) {
        _editorState.value = _editorState.value.copy(deviceModel = deviceModel)
    }

    fun savePreset(): Boolean {
        val state = _editorState.value
        
        if (state.name.isBlank()) {
            Timber.w("Preset name cannot be empty")
            return false
        }

        val presetEntity = CameraPresetEntity(
            id = state.id.ifBlank { UUID.randomUUID().toString() },
            name = state.name,
            author = state.author.ifBlank { "Anonymous" },
            coverPath = "",
            tags = state.tags.joinToString(","),
            scene = state.scene,
            mode = "master",
            filter = state.filter,
            iso = state.iso,
            shutter = state.shutter,
            ev = state.ev.toString(),
            wb = state.wb,
            deviceModel = state.deviceModel,
            source = "user_custom",
            isFavorite = false,
            useCount = 0,
            createTime = System.currentTimeMillis()
        )

        viewModelScope.launch {
            try {
                databaseProvider.database.presetDao().insertPreset(presetEntity)
                Timber.d("Preset saved successfully: ${state.name}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to save preset")
            }
        }

        return true
    }

    fun loadPreset(presetId: String) {
        viewModelScope.launch {
            try {
                val preset = databaseProvider.database.presetDao().getPresetById(presetId)
                preset?.let {
                    _editorState.value = PresetEditorState(
                        id = it.id,
                        name = it.name,
                        author = it.author,
                        scene = it.scene,
                        filter = it.filter,
                        tags = it.tags.split(",").filter { tag -> tag.isNotBlank() },
                        iso = it.iso,
                        shutter = it.shutter,
                        ev = it.ev.toFloatOrNull() ?: 0f,
                        wb = it.wb,
                        deviceModel = it.deviceModel
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load preset")
            }
        }
    }

    fun reset() {
        _editorState.value = PresetEditorState()
    }
}

data class PresetEditorState(
    val id: String = "",
    val name: String = "",
    val author: String = "",
    val scene: String = "通用",
    val filter: String = "默认",
    val tags: List<String> = emptyList(),
    val iso: Int = 100,
    val shutter: String = "1/125",
    val ev: Float = 0f,
    val wb: String = "5500K",
    val deviceModel: String = ""
)

val ISO_OPTIONS = listOf(50, 100, 200, 400, 800, 1600, 3200, 6400)
val SHUTTER_OPTIONS = listOf("1/1000", "1/500", "1/250", "1/125", "1/60", "1/30", "1/15", "1/8", "1/4", "1/2", "1", "2", "4")
val WB_OPTIONS = listOf("自动", "2800K", "3200K", "4000K", "4500K", "5000K", "5500K", "6000K", "7000K", "8000K", "9000K", "10000K")
val SCENE_OPTIONS = listOf("通用", "人像", "风景", "夜景", "美食", "街拍", "微距", "建筑")
val FILTER_OPTIONS = listOf("默认", "复古", "胶片", "清新", "黑白", "暖调", "冷调", "鲜艳", "自然")