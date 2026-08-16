package com.exposures.phone.ui.camerabody

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.CameraBody
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
import com.exposures.phone.sync.EquipmentSyncPusher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class CameraBodyEditUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val manufacturer: String = "",
    val fastestShutterSpeed: ShutterSpeed = ShutterSpeed.STANDARD_FULL_STOPS.first(),
    val slowestShutterSpeed: ShutterSpeed = ShutterSpeed.STANDARD_FULL_STOPS.last(),
    val hasBulbMode: Boolean = true,
    val done: Boolean = false,
) {
    val canSave: Boolean
        get() = name.isNotBlank() && manufacturer.isNotBlank() && fastestShutterSpeed <= slowestShutterSpeed
}

class CameraBodyEditViewModel(
    private val repository: EquipmentRepository,
    private val syncPusher: EquipmentSyncPusher,
    private val existingId: String?,
) : ViewModel() {

    private val id = existingId ?: UUID.randomUUID().toString()
    private val _uiState = MutableStateFlow(CameraBodyEditUiState(isNew = existingId == null))
    val uiState: StateFlow<CameraBodyEditUiState> = _uiState.asStateFlow()

    init {
        if (existingId == null) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        } else {
            viewModelScope.launch {
                val body = repository.getCameraBody(existingId)
                _uiState.value = if (body == null) {
                    _uiState.value.copy(isLoading = false)
                } else {
                    _uiState.value.copy(
                        isLoading = false,
                        name = body.name,
                        manufacturer = body.manufacturer,
                        fastestShutterSpeed = body.availableShutterSpeeds.filter { it != ShutterSpeed.BULB }.minOrNull()
                            ?: ShutterSpeed.STANDARD_FULL_STOPS.first(),
                        slowestShutterSpeed = body.availableShutterSpeeds.filter { it != ShutterSpeed.BULB }.maxOrNull()
                            ?: ShutterSpeed.STANDARD_FULL_STOPS.last(),
                        hasBulbMode = body.hasBulbMode,
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

    fun setFastestShutterSpeed(speed: ShutterSpeed) {
        _uiState.value = _uiState.value.copy(fastestShutterSpeed = speed)
    }

    fun setSlowestShutterSpeed(speed: ShutterSpeed) {
        _uiState.value = _uiState.value.copy(slowestShutterSpeed = speed)
    }

    fun setHasBulbMode(hasBulbMode: Boolean) {
        _uiState.value = _uiState.value.copy(hasBulbMode = hasBulbMode)
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = repository.getCameraBody(id)
            val body = CameraBody(
                id = id,
                name = state.name,
                manufacturer = state.manufacturer,
                availableShutterSpeeds = ShutterSpeed.standardRange(
                    fastest = state.fastestShutterSpeed,
                    slowest = state.slowestShutterSpeed,
                    includeBulb = state.hasBulbMode,
                ),
                hasBulbMode = state.hasBulbMode,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_SYNC,
                remoteId = existing?.remoteId,
            )
            repository.saveCameraBody(body)
            syncPusher.pushCameraBodies()
            _uiState.value = _uiState.value.copy(done = true)
        }
    }

    fun delete() {
        if (_uiState.value.isNew) return
        viewModelScope.launch {
            repository.getCameraBody(id)?.let { repository.deleteCameraBody(it) }
            syncPusher.pushCameraBodies()
            _uiState.value = _uiState.value.copy(done = true)
        }
    }
}
