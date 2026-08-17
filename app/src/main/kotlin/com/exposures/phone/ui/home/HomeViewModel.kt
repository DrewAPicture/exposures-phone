package com.exposures.phone.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import com.exposures.datalayer.DataLayerGateway
import com.exposures.model.Exposure
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val cameraBodyCount: Int = 0,
    val lensCount: Int = 0,
    val lightMeterCount: Int = 0,
    val filmBackCount: Int = 0,
    val filmRollCount: Int = 0,
    val exposureCount: Int = 0,
    val syncedTodayCount: Int = 0,
    val pendingSyncCount: Int = 0,
    val favoriteCameraName: String = "-",
    val watchReachable: Boolean? = null,
)

/** combine() only has direct overloads up to 5 flows, so the equipment counts are pre-combined into one value to keep the outer combine within that limit. */
private data class EquipmentCounts(
    val cameraBodyCount: Int,
    val lensCount: Int,
    val lightMeterCount: Int,
    val filmBackCount: Int,
)

private data class RollStats(
    val filmRollCount: Int,
    val favoriteCameraName: String,
)

class HomeViewModel(
    private val repository: EquipmentRepository,
    private val gateway: DataLayerGateway,
) : ViewModel() {
    private val watchReachable = MutableStateFlow<Boolean?>(null)

    private val equipmentCounts = combine(
        repository.observeCameraBodies(),
        repository.observeLenses(),
        repository.observeLightMeters(),
        repository.observeFilmBacks(),
    ) { bodies, lenses, lightMeters, filmBacks ->
        EquipmentCounts(bodies.size, lenses.size, lightMeters.size, filmBacks.size)
    }

    private val rollStats = combine(
        repository.observeFilmRolls(),
        repository.observeCameraBodies(),
    ) { rolls, cameraBodies ->
        val cameraById = cameraBodies.associateBy { it.id }
        val favoriteCameraName = rolls
            .groupingBy { it.cameraBodyId }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?.let { cameraById[it]?.name ?: "Unknown" }
            ?: "-"
        RollStats(
            filmRollCount = rolls.size,
            favoriteCameraName = favoriteCameraName,
        )
    }

    private val pendingSyncCount = combine(
        repository.observeDirtyExposures(),
        repository.observeDirtyReferencePhotos(),
    ) { exposures, photos -> exposures.size + photos.size }

    val uiState: StateFlow<HomeUiState> = combine(
        equipmentCounts,
        rollStats,
        repository.observeAllExposures(),
        watchReachable,
        pendingSyncCount,
    ) { equipment, rollStats, exposures, reachable, pending ->
        HomeUiState(
            isLoading = false,
            cameraBodyCount = equipment.cameraBodyCount,
            lensCount = equipment.lensCount,
            lightMeterCount = equipment.lightMeterCount,
            filmBackCount = equipment.filmBackCount,
            filmRollCount = rollStats.filmRollCount,
            exposureCount = exposures.size,
            syncedTodayCount = exposures.countSyncedToday(),
            pendingSyncCount = pending,
            favoriteCameraName = rollStats.favoriteCameraName,
            watchReachable = reachable,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refreshPairingStatus()
    }

    fun refreshPairingStatus() {
        watchReachable.value = null
        viewModelScope.launch {
            watchReachable.value = gateway.findReachableNodeId() != null
        }
    }

    private fun List<Exposure>.countSyncedToday(): Int {
        val zoneId = ZoneId.systemDefault()
        val startOfToday = ZonedDateTime.now(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
        return count { it.capturedAt >= startOfToday && it.capturedAt <= Instant.now().toEpochMilli() }
    }
}
