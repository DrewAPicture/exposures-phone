package com.exposures.phone.ui.camerabody

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.CameraBody
import com.exposures.model.ShutterSpeed
import com.exposures.model.ShutterSpeedKind
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
    /** Null selects the "Other" option — a fastest speed that doesn't land on a standard stop
     * (e.g. a leaf shutter topping out at 1/400), entered via [otherFastShutterSpeedDenominator]
     * instead. The two are mutually exclusive by construction: a standard stop is never combined
     * with a custom one, so a body can't end up with an unreachable "fastest" left over from
     * before a custom speed was set. See [ShutterSpeed.standardRange]. */
    val fastestShutterSpeed: ShutterSpeed? = ShutterSpeed.STANDARD_FULL_STOPS.first(),
    val slowestShutterSpeed: ShutterSpeed = ShutterSpeed.STANDARD_FULL_STOPS.last(),
    val otherFastShutterSpeedDenominator: String = "",
    val hasBulbMode: Boolean = true,
    val done: Boolean = false,
) {
    private val otherShutterSpeed: ShutterSpeed?
        get() = otherFastShutterSpeedDenominator.toIntOrNull()?.takeIf { it > 0 }?.let(ShutterSpeed::fraction)

    val canSave: Boolean
        get() = name.isNotBlank() && manufacturer.isNotBlank() &&
            when (val fastest = fastestShutterSpeed) {
                null -> otherShutterSpeed?.let { it <= slowestShutterSpeed } == true
                else -> fastest <= slowestShutterSpeed
            }
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
                    // Separate the standard-stop speeds (drive the Fastest/Slowest bounds) from any
                    // extra speed outside that scale (the "Other" field) — see the field's doc comment.
                    val nonBulb = body.availableShutterSpeeds.filter { it != ShutterSpeed.BULB }
                    val standardSpeeds = nonBulb.filter { it in ShutterSpeed.STANDARD_FULL_STOPS }
                    val otherSpeed = nonBulb.firstOrNull { it !in ShutterSpeed.STANDARD_FULL_STOPS }
                    _uiState.value.copy(
                        isLoading = false,
                        name = body.name,
                        manufacturer = body.manufacturer,
                        fastestShutterSpeed = if (otherSpeed != null) {
                            null
                        } else {
                            standardSpeeds.minOrNull() ?: ShutterSpeed.STANDARD_FULL_STOPS.first()
                        },
                        slowestShutterSpeed = standardSpeeds.maxOrNull() ?: ShutterSpeed.STANDARD_FULL_STOPS.last(),
                        otherFastShutterSpeedDenominator = otherSpeed
                            ?.takeIf { it.kind == ShutterSpeedKind.FRACTION }
                            ?.denominator?.toString().orEmpty(),
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

    fun setFastestShutterSpeed(speed: ShutterSpeed?) {
        _uiState.value = _uiState.value.copy(fastestShutterSpeed = speed)
    }

    fun setSlowestShutterSpeed(speed: ShutterSpeed) {
        _uiState.value = _uiState.value.copy(slowestShutterSpeed = speed)
    }

    fun setOtherFastShutterSpeedDenominator(value: String) {
        _uiState.value = _uiState.value.copy(otherFastShutterSpeedDenominator = value)
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
            // "Other" (fastestShutterSpeed == null) and a standard fastest stop are mutually
            // exclusive selections in the UI, so whichever one is active becomes the actual
            // fastest bound passed to standardRange() — there's no separate union step, so an
            // unreachable standard stop faster than a custom speed can never end up in the saved
            // list. See the field's doc comment on CameraBodyEditUiState.
            // Only read the "Other" text field when it's the active selection — otherwise stale
            // leftover text from a prior "Other" selection (now switched back to a standard stop)
            // would get silently reunioned into the saved list.
            val otherSpeed = state.otherFastShutterSpeedDenominator.toIntOrNull()
                ?.takeIf { it > 0 }
                ?.let(ShutterSpeed::fraction)
                ?.takeIf { state.fastestShutterSpeed == null }
            val effectiveFastest = state.fastestShutterSpeed ?: requireNotNull(otherSpeed)
            val standardSpeeds = ShutterSpeed.standardRange(
                fastest = effectiveFastest,
                slowest = state.slowestShutterSpeed,
                includeBulb = state.hasBulbMode,
            )
            val body = CameraBody(
                id = id,
                name = state.name,
                manufacturer = state.manufacturer,
                availableShutterSpeeds = (standardSpeeds + listOfNotNull(otherSpeed)).distinct().sorted(),
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
