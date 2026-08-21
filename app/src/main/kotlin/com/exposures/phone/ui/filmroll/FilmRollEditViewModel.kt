package com.exposures.phone.ui.filmroll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.CameraBody
import com.exposures.model.FilmBack
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
    val availableFilmBacks: List<FilmBack> = emptyList(),
    val name: String = "",
    val filmStock: String = "",
    val boxSpeedIso: String = "",
    val format: FilmFormat = FilmFormat.MEDIUM_FORMAT_120,
    val colorType: FilmColorType = FilmColorType.BLACK_AND_WHITE,
    val cameraBodyId: String? = null,
    val lightMeterId: String? = null,
    val filmBackId: String? = null,
    val targetFrameCount: Int? = null,
    val done: Boolean = false,
) {
    /** Film backs scoped to the currently-selected camera body — a back belongs to exactly one
     * body, so a back for a different body is never a valid choice. Shared by the screen's
     * dropdown and the single-option auto-select logic below, so both always agree on what "the
     * only option" is. */
    val filmBacksForSelectedBody: List<FilmBack>
        get() = availableFilmBacks.filter { it.cameraBodyId == cameraBodyId }

    val canSave: Boolean
        get() = name.isNotBlank() &&
            filmStock.isNotBlank() &&
            boxSpeedIso.toIntOrNull()?.let { it > 0 } == true &&
            cameraBodyId != null &&
            filmBackId != null &&
            targetFrameCount != null
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
            val filmBacks = repository.observeFilmBacks().first()
            val existing = existingId?.let { repository.getFilmRoll(it) }
            _uiState.value = if (existing == null) {
                // Auto-fill each picker only when it has exactly one option — never "first of many",
                // which would silently pick for the user instead of leaving an ambiguous choice to
                // them. Resolved in dependency order: body -> the backs scoped to that body -> that
                // back's own frame counts, so a single back for a single body cascades all the way
                // down to a savable roll with no manual interaction.
                val cameraBodyId = cameraBodies.singleOrNull()?.id
                val filmBack = filmBacks.filter { it.cameraBodyId == cameraBodyId }.singleOrNull()
                _uiState.value.copy(
                    isLoading = false,
                    availableCameraBodies = cameraBodies,
                    availableLightMeters = lightMeters,
                    availableFilmBacks = filmBacks,
                    cameraBodyId = cameraBodyId,
                    lightMeterId = lightMeters.singleOrNull()?.id,
                    filmBackId = filmBack?.id,
                    targetFrameCount = filmBack?.availableFrameCounts?.singleOrNull(),
                )
            } else {
                _uiState.value.copy(
                    isLoading = false,
                    availableCameraBodies = cameraBodies,
                    availableLightMeters = lightMeters,
                    availableFilmBacks = filmBacks,
                    name = existing.name,
                    filmStock = existing.filmStock,
                    boxSpeedIso = existing.boxSpeedIso.toString(),
                    format = existing.format,
                    colorType = existing.colorType,
                    cameraBodyId = existing.cameraBodyId,
                    lightMeterId = existing.lightMeterId,
                    filmBackId = existing.filmBackId,
                    targetFrameCount = existing.targetFrameCount,
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

    /** Backs are body-specific — a film back that no longer belongs to the new body can't stay
     * selected. When it's cleared, re-applies the same single-option auto-select the initial load
     * uses, rather than leaving the user to manually reselect a back that has only one possible
     * choice for the new body. */
    fun setCameraBody(cameraBodyId: String) {
        val state = _uiState.value
        val selectedBack = state.availableFilmBacks.firstOrNull { it.id == state.filmBackId }
        val backStillValid = selectedBack != null && selectedBack.cameraBodyId == cameraBodyId
        _uiState.value = if (backStillValid) {
            state.copy(cameraBodyId = cameraBodyId)
        } else {
            val filmBack = state.availableFilmBacks.filter { it.cameraBodyId == cameraBodyId }.singleOrNull()
            state.copy(
                cameraBodyId = cameraBodyId,
                filmBackId = filmBack?.id,
                targetFrameCount = filmBack?.availableFrameCounts?.singleOrNull(),
            )
        }
    }

    /** Null clears the roll's light meter — most rolls don't use a handheld meter at all. */
    fun setLightMeter(lightMeterId: String?) {
        _uiState.value = _uiState.value.copy(lightMeterId = lightMeterId)
    }

    /** The target frame count is only meaningful for the newly-selected back's own declared
     * counts — auto-fills it when that back has exactly one, the same single-option rule applied
     * to every other picker on this screen. */
    fun setFilmBack(filmBackId: String) {
        val state = _uiState.value
        val back = state.availableFilmBacks.firstOrNull { it.id == filmBackId }
        val countStillValid = back != null && state.targetFrameCount in back.availableFrameCounts
        _uiState.value = if (countStillValid) {
            state.copy(filmBackId = filmBackId)
        } else {
            state.copy(filmBackId = filmBackId, targetFrameCount = back?.availableFrameCounts?.singleOrNull())
        }
    }

    fun setTargetFrameCount(count: Int) {
        _uiState.value = _uiState.value.copy(targetFrameCount = count)
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
                filmBackId = requireNotNull(state.filmBackId),
                targetFrameCount = requireNotNull(state.targetFrameCount),
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
