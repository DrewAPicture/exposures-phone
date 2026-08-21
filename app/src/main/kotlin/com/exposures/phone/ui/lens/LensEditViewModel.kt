package com.exposures.phone.ui.lens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.CameraBody
import com.exposures.model.Lens
import com.exposures.model.LensType
import com.exposures.model.StopIncrement
import com.exposures.model.SyncStatus
import com.exposures.phone.sync.EquipmentSyncPusher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

data class LensEditUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val availableCameraBodies: List<CameraBody> = emptyList(),
    val cameraBodyId: String? = null,
    val name: String = "",
    val minAperture: String = "",
    val maxAperture: String = "",
    val stopIncrement: StopIncrement = StopIncrement.HALF_STOP,
    val referencePhotoZoomRatio: String = "1.0",
    val lensType: LensType = LensType.PRIME,
    val focalLengthMm: String = "",
    val focalLengthMinMm: String = "",
    val focalLengthMaxMm: String = "",
    val done: Boolean = false,
) {
    private val minApertureValue get() = minAperture.toDoubleOrNull()
    private val maxApertureValue get() = maxAperture.toDoubleOrNull()
    private val zoomRatioValue get() = referencePhotoZoomRatio.toDoubleOrNull()

    private val focalLengthValid: Boolean
        get() = when (lensType) {
            LensType.PRIME -> (focalLengthMm.toIntOrNull() ?: -1) > 0
            LensType.ZOOM -> {
                val min = focalLengthMinMm.toIntOrNull()
                val max = focalLengthMaxMm.toIntOrNull()
                min != null && min > 0 && max != null && max >= min
            }
        }

    val canSave: Boolean
        get() {
            val min = minApertureValue
            val max = maxApertureValue
            val zoom = zoomRatioValue
            return name.isNotBlank() && min != null && min > 0.0 && max != null && max >= min &&
                zoom != null && zoom > 0.0 && focalLengthValid
        }
}

class LensEditViewModel(
    private val repository: EquipmentRepository,
    private val syncPusher: EquipmentSyncPusher,
    private val existingId: String?,
) : ViewModel() {

    private val id = existingId ?: UUID.randomUUID().toString()
    private val _uiState = MutableStateFlow(LensEditUiState(isNew = existingId == null))
    val uiState: StateFlow<LensEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val cameraBodies = repository.observeCameraBodies().first()
            val lens = existingId?.let { repository.getLens(it) }
            _uiState.value = if (lens == null) {
                _uiState.value.copy(
                    isLoading = false,
                    availableCameraBodies = cameraBodies,
                    cameraBodyId = cameraBodies.firstOrNull()?.id,
                )
            } else {
                _uiState.value.copy(
                    isLoading = false,
                    availableCameraBodies = cameraBodies,
                    cameraBodyId = lens.cameraBodyId ?: cameraBodies.firstOrNull()?.id,
                    name = lens.name,
                    minAperture = lens.minAperture.toString(),
                    maxAperture = lens.maxAperture.toString(),
                    stopIncrement = lens.stopIncrement,
                    referencePhotoZoomRatio = lens.referencePhotoZoomRatio.toString(),
                    lensType = lens.lensType,
                    focalLengthMm = lens.focalLengthMm?.toString() ?: "",
                    focalLengthMinMm = lens.focalLengthMinMm?.toString() ?: "",
                    focalLengthMaxMm = lens.focalLengthMaxMm?.toString() ?: "",
                )
            }
        }
    }

    fun setCameraBody(cameraBodyId: String?) {
        _uiState.value = _uiState.value.copy(cameraBodyId = cameraBodyId)
    }

    /**
     * Also auto-detects a `NNNmm` focal length out of [name] and prefills [LensEditUiState.focalLengthMm]
     * — a first-load convenience only: it never overwrites a value the user (or a loaded lens)
     * already has, and only applies for [LensType.PRIME] (a zoom's focal length is a range, not a
     * single number a lens name would encode).
     */
    fun setName(name: String) {
        val state = _uiState.value
        val detected = if (state.lensType == LensType.PRIME && state.focalLengthMm.isBlank()) {
            detectFocalLengthMm(name)
        } else {
            null
        }
        _uiState.value = state.copy(name = name, focalLengthMm = detected?.toString() ?: state.focalLengthMm)
    }

    fun setMinAperture(value: String) {
        _uiState.value = _uiState.value.copy(minAperture = value)
    }

    fun setMaxAperture(value: String) {
        _uiState.value = _uiState.value.copy(maxAperture = value)
    }

    fun setStopIncrement(value: StopIncrement) {
        _uiState.value = _uiState.value.copy(stopIncrement = value)
    }

    fun setReferencePhotoZoomRatio(value: String) {
        _uiState.value = _uiState.value.copy(referencePhotoZoomRatio = value)
    }

    fun setLensType(value: LensType) {
        _uiState.value = _uiState.value.copy(lensType = value)
    }

    fun setFocalLengthMm(value: String) {
        _uiState.value = _uiState.value.copy(focalLengthMm = value)
    }

    fun setFocalLengthMinMm(value: String) {
        _uiState.value = _uiState.value.copy(focalLengthMinMm = value)
    }

    fun setFocalLengthMaxMm(value: String) {
        _uiState.value = _uiState.value.copy(focalLengthMaxMm = value)
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = repository.getLens(id)
            val lens = Lens(
                id = id,
                name = state.name,
                cameraBodyId = state.cameraBodyId,
                minAperture = requireNotNull(state.minAperture.toDoubleOrNull()),
                maxAperture = requireNotNull(state.maxAperture.toDoubleOrNull()),
                stopIncrement = state.stopIncrement,
                referencePhotoZoomRatio = requireNotNull(state.referencePhotoZoomRatio.toDoubleOrNull()),
                lensType = state.lensType,
                focalLengthMm = state.focalLengthMm.toIntOrNull().takeIf { state.lensType == LensType.PRIME },
                focalLengthMinMm = state.focalLengthMinMm.toIntOrNull().takeIf { state.lensType == LensType.ZOOM },
                focalLengthMaxMm = state.focalLengthMaxMm.toIntOrNull().takeIf { state.lensType == LensType.ZOOM },
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_SYNC,
                remoteId = existing?.remoteId,
            )
            repository.saveLens(lens)
            syncPusher.pushLenses()
            _uiState.value = _uiState.value.copy(done = true)
        }
    }

    fun delete() {
        if (_uiState.value.isNew) return
        viewModelScope.launch {
            repository.getLens(id)?.let { repository.deleteLens(it) }
            syncPusher.pushLenses()
            _uiState.value = _uiState.value.copy(done = true)
        }
    }

    private companion object {
        val FOCAL_LENGTH_PATTERN = Regex("""(\d+)\s*mm""", RegexOption.IGNORE_CASE)

        fun detectFocalLengthMm(name: String): Int? = FOCAL_LENGTH_PATTERN.find(name)?.groupValues?.get(1)?.toIntOrNull()
    }
}
