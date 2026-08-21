package com.exposures.phone.ui.filmback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.CameraBody
import com.exposures.model.FilmBack
import com.exposures.model.FilmBackType
import com.exposures.model.SyncStatus
import com.exposures.phone.sync.EquipmentSyncPusher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

/** Starting point for a freshly-typed back of this type — editable afterward, never enforced. */
private fun defaultFrameCount(type: FilmBackType): Int = when (type) {
    FilmBackType.ROLL_6X7 -> 10
    FilmBackType.ROLL_6X6 -> 12
    FilmBackType.POLAROID -> 10
    FilmBackType.INSTAX -> 10
}

data class FilmBackEditUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val availableCameraBodies: List<CameraBody> = emptyList(),
    val name: String = "",
    val cameraBodyId: String? = null,
    val type: FilmBackType = FilmBackType.ROLL_6X7,
    val primaryFrameCount: String = "",
    /** A second frame count this same back sometimes yields — e.g. tight loading squeezing out one extra frame. */
    val alternateFrameCount: String = "",
    val done: Boolean = false,
    /** The id this back was just saved under — set only by [FilmBackEditViewModel.save], never
     * [FilmBackEditViewModel.delete], so a deleted back's id can never flow back through the same
     * result channel as a created one. */
    val savedId: String? = null,
) {
    private val alternateCountValid: Boolean
        get() = alternateFrameCount.isBlank() || alternateFrameCount.toIntOrNull()?.let { it > 0 } == true

    val canSave: Boolean
        get() = name.isNotBlank() &&
            cameraBodyId != null &&
            primaryFrameCount.toIntOrNull()?.let { it > 0 } == true &&
            alternateCountValid
}

class FilmBackEditViewModel(
    private val repository: EquipmentRepository,
    private val syncPusher: EquipmentSyncPusher,
    private val existingId: String?,
    /** Preselects the camera body dropdown for a brand-new back — used when a back is created
     * inline from New Film Roll, so it comes back already scoped to the roll's camera body. Never
     * applies when editing an existing back. */
    private val initialCameraBodyId: String? = null,
) : ViewModel() {

    private val id = existingId ?: UUID.randomUUID().toString()
    private val _uiState = MutableStateFlow(FilmBackEditUiState(isNew = existingId == null))
    val uiState: StateFlow<FilmBackEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val cameraBodies = repository.observeCameraBodies().first()
            val existing = existingId?.let { repository.getFilmBack(it) }
            _uiState.value = if (existing == null) {
                _uiState.value.copy(
                    isLoading = false,
                    availableCameraBodies = cameraBodies,
                    cameraBodyId = cameraBodies.firstOrNull { it.id == initialCameraBodyId }?.id
                        ?: cameraBodies.firstOrNull()?.id,
                )
            } else {
                val sortedCounts = existing.availableFrameCounts.sorted()
                _uiState.value.copy(
                    isLoading = false,
                    availableCameraBodies = cameraBodies,
                    name = existing.name,
                    cameraBodyId = existing.cameraBodyId,
                    type = existing.type,
                    primaryFrameCount = sortedCounts.getOrNull(0)?.toString().orEmpty(),
                    alternateFrameCount = sortedCounts.getOrNull(1)?.toString().orEmpty(),
                )
            }
        }
    }

    fun setName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun setCameraBody(cameraBodyId: String) {
        _uiState.value = _uiState.value.copy(cameraBodyId = cameraBodyId)
    }

    /** Only nudges the primary count for a brand-new, still-untouched back — never overwrites a
     * value the user already typed, and never touches an existing back being edited. */
    fun setType(type: FilmBackType) {
        val state = _uiState.value
        _uiState.value = if (state.isNew && state.primaryFrameCount.isBlank()) {
            state.copy(type = type, primaryFrameCount = defaultFrameCount(type).toString())
        } else {
            state.copy(type = type)
        }
    }

    fun setPrimaryFrameCount(value: String) {
        _uiState.value = _uiState.value.copy(primaryFrameCount = value)
    }

    fun setAlternateFrameCount(value: String) {
        _uiState.value = _uiState.value.copy(alternateFrameCount = value)
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = repository.getFilmBack(id)
            val primary = requireNotNull(state.primaryFrameCount.toIntOrNull())
            val alternate = state.alternateFrameCount.toIntOrNull()
            val filmBack = FilmBack(
                id = id,
                name = state.name,
                cameraBodyId = requireNotNull(state.cameraBodyId),
                type = state.type,
                availableFrameCounts = listOfNotNull(primary, alternate).distinct().sorted(),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_SYNC,
                remoteId = existing?.remoteId,
            )
            repository.saveFilmBack(filmBack)
            syncPusher.pushFilmBacks()
            _uiState.value = _uiState.value.copy(done = true, savedId = id)
        }
    }

    fun delete() {
        if (_uiState.value.isNew) return
        viewModelScope.launch {
            repository.getFilmBack(id)?.let { repository.deleteFilmBack(it) }
            syncPusher.pushFilmBacks()
            _uiState.value = _uiState.value.copy(done = true)
        }
    }
}
