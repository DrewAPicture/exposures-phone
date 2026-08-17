package com.exposures.phone.ui.filmroll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.CameraBody
import com.exposures.model.FilmColorType
import com.exposures.model.FilmFormat
import com.exposures.model.FilmRoll
import com.exposures.model.LightMeter
import com.exposures.model.RollStatus
import com.exposures.model.SyncStatus
import com.exposures.phone.sync.EquipmentSyncPusher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

data class FilmRollEditUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val availableCameraBodies: List<CameraBody> = emptyList(),
    val availableLightMeters: List<LightMeter> = emptyList(),
    val name: String = "",
    val filmStock: String = "",
    val boxSpeedIso: String = "",
    val format: FilmFormat = FilmFormat.MEDIUM_FORMAT_120,
    val colorType: FilmColorType = FilmColorType.COLOR,
    val cameraBodyId: String? = null,
    val lightMeterId: String? = null,
    val targetFrameCount: String = "",
    val done: Boolean = false,
) {
    val canSave: Boolean
        get() = name.isNotBlank() &&
            filmStock.isNotBlank() &&
            boxSpeedIso.toIntOrNull()?.let { it > 0 } == true &&
            cameraBodyId != null &&
            targetFrameCount.toIntOrNull()?.let { it > 0 } == true
}

class FilmRollEditViewModel(
    private val repository: EquipmentRepository,
    private val syncPusher: EquipmentSyncPusher,
    private val existingId: String?,
) : ViewModel() {

    private val id = existingId ?: UUID.randomUUID().toString()
    private val _uiState = MutableStateFlow(FilmRollEditUiState(isNew = existingId == null))
    val uiState: StateFlow<FilmRollEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val cameraBodies = repository.observeCameraBodies().first()
            val lightMeters = repository.observeLightMeters().first()
            val existing = existingId?.let { repository.getFilmRoll(it) }
            _uiState.value = if (existing == null) {
                _uiState.value.copy(
                    isLoading = false,
                    availableCameraBodies = cameraBodies,
                    availableLightMeters = lightMeters,
                    cameraBodyId = cameraBodies.firstOrNull()?.id,
                )
            } else {
                _uiState.value.copy(
                    isLoading = false,
                    availableCameraBodies = cameraBodies,
                    availableLightMeters = lightMeters,
                    name = existing.name,
                    filmStock = existing.filmStock,
                    boxSpeedIso = existing.boxSpeedIso.toString(),
                    format = existing.format,
                    colorType = existing.colorType,
                    cameraBodyId = existing.cameraBodyId,
                    lightMeterId = existing.lightMeterId,
                    targetFrameCount = existing.targetFrameCount.toString(),
                )
            }
        }
    }

    fun setName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun setFilmStock(filmStock: String) {
        _uiState.value = _uiState.value.copy(filmStock = filmStock)
    }

    fun setBoxSpeedIso(value: String) {
        _uiState.value = _uiState.value.copy(boxSpeedIso = value)
    }

    fun setFormat(format: FilmFormat) {
        _uiState.value = _uiState.value.copy(format = format)
    }

    fun setColorType(colorType: FilmColorType) {
        _uiState.value = _uiState.value.copy(colorType = colorType)
    }

    fun setCameraBody(cameraBodyId: String) {
        _uiState.value = _uiState.value.copy(cameraBodyId = cameraBodyId)
    }

    /** Null clears the roll's light meter — most rolls don't use a handheld meter at all. */
    fun setLightMeter(lightMeterId: String?) {
        _uiState.value = _uiState.value.copy(lightMeterId = lightMeterId)
    }

    fun setTargetFrameCount(value: String) {
        _uiState.value = _uiState.value.copy(targetFrameCount = value)
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = repository.getFilmRoll(id)
            val roll = FilmRoll(
                id = id,
                name = state.name,
                filmStock = state.filmStock,
                boxSpeedIso = requireNotNull(state.boxSpeedIso.toIntOrNull()),
                format = state.format,
                colorType = state.colorType,
                cameraBodyId = requireNotNull(state.cameraBodyId),
                lightMeterId = state.lightMeterId,
                targetFrameCount = requireNotNull(state.targetFrameCount.toIntOrNull()),
                status = existing?.status ?: RollStatus.AVAILABLE,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_SYNC,
                remoteId = existing?.remoteId,
            )
            repository.saveFilmRoll(roll)
            syncPusher.pushFilmRolls()
            _uiState.value = _uiState.value.copy(done = true)
        }
    }

    fun delete() {
        if (_uiState.value.isNew) return
        viewModelScope.launch {
            repository.getFilmRoll(id)?.let { repository.deleteFilmRoll(it) }
            syncPusher.pushFilmRolls()
            _uiState.value = _uiState.value.copy(done = true)
        }
    }
}
