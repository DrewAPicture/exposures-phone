package com.exposures.phone.ui.lightmeter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.LightMeter
import com.exposures.model.LightMeterType
import com.exposures.model.SyncStatus
import com.exposures.phone.sync.EquipmentSyncPusher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class LightMeterEditUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val manufacturer: String = "",
    val type: LightMeterType = LightMeterType.SPOT,
    val done: Boolean = false,
) {
    val canSave: Boolean
        get() = name.isNotBlank() && manufacturer.isNotBlank()
}

class LightMeterEditViewModel(
    private val repository: EquipmentRepository,
    private val syncPusher: EquipmentSyncPusher,
    private val existingId: String?,
) : ViewModel() {

    private val id = existingId ?: UUID.randomUUID().toString()
    private val _uiState = MutableStateFlow(LightMeterEditUiState(isNew = existingId == null))
    val uiState: StateFlow<LightMeterEditUiState> = _uiState.asStateFlow()

    init {
        if (existingId == null) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        } else {
            viewModelScope.launch {
                val lightMeter = repository.getLightMeter(existingId)
                _uiState.value = if (lightMeter == null) {
                    _uiState.value.copy(isLoading = false)
                } else {
                    _uiState.value.copy(
                        isLoading = false,
                        name = lightMeter.name,
                        manufacturer = lightMeter.manufacturer,
                        type = lightMeter.type,
                    )
                }
            }
        }
    }

    fun setName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun setManufacturer(manufacturer: String) {
        _uiState.value = _uiState.value.copy(manufacturer = manufacturer)
    }

    fun setType(type: LightMeterType) {
        _uiState.value = _uiState.value.copy(type = type)
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = repository.getLightMeter(id)
            val lightMeter = LightMeter(
                id = id,
                name = state.name,
                manufacturer = state.manufacturer,
                type = state.type,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_SYNC,
                remoteId = existing?.remoteId,
            )
            repository.saveLightMeter(lightMeter)
            syncPusher.pushLightMeters()
            _uiState.value = _uiState.value.copy(done = true)
        }
    }

    fun delete() {
        if (_uiState.value.isNew) return
        viewModelScope.launch {
            repository.getLightMeter(id)?.let { repository.deleteLightMeter(it) }
            syncPusher.pushLightMeters()
            _uiState.value = _uiState.value.copy(done = true)
        }
    }
}
