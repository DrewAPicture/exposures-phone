package com.exposures.phone.ui.lens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.Lens
import com.exposures.model.StopIncrement
import com.exposures.model.SyncStatus
import com.exposures.phone.sync.EquipmentSyncPusher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class LensEditUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val minAperture: String = "",
    val maxAperture: String = "",
    val stopIncrement: StopIncrement = StopIncrement.HALF_STOP,
    val done: Boolean = false,
) {
    private val minApertureValue get() = minAperture.toDoubleOrNull()
    private val maxApertureValue get() = maxAperture.toDoubleOrNull()

    val canSave: Boolean
        get() {
            val min = minApertureValue
            val max = maxApertureValue
            return name.isNotBlank() && min != null && min > 0.0 && max != null && max >= min
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
        if (existingId == null) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        } else {
            viewModelScope.launch {
                val lens = repository.getLens(existingId)
                _uiState.value = if (lens == null) {
                    _uiState.value.copy(isLoading = false)
                } else {
                    _uiState.value.copy(
                        isLoading = false,
                        name = lens.name,
                        minAperture = lens.minAperture.toString(),
                        maxAperture = lens.maxAperture.toString(),
                        stopIncrement = lens.stopIncrement,
                    )
                }
            }
        }
    }

    fun setName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
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

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = repository.getLens(id)
            val lens = Lens(
                id = id,
                name = state.name,
                minAperture = requireNotNull(state.minAperture.toDoubleOrNull()),
                maxAperture = requireNotNull(state.maxAperture.toDoubleOrNull()),
                stopIncrement = state.stopIncrement,
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
}
