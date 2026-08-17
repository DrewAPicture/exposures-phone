package com.exposures.phone.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import com.exposures.datalayer.DataLayerGateway
import com.exposures.phone.export.CsvExportCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val pendingSyncCount: Int = 0, // exposures + reference photos not yet uploaded to the remote backend
    val watchReachable: Boolean? = null, // null while unchecked
)

/** combine() only has direct overloads up to 5 flows, so the equipment counts are pre-combined into one value to keep the outer combine within that limit. */
private data class EquipmentCounts(
    val cameraBodyCount: Int,
    val lensCount: Int,
    val lightMeterCount: Int,
    val filmBackCount: Int,
)

class HomeViewModel(
    private val repository: EquipmentRepository,
    private val gateway: DataLayerGateway,
    private val csvExportCoordinator: CsvExportCoordinator,
    private val triggerUpload: () -> Unit = {},
) : ViewModel() {

    private val _watchReachable = MutableStateFlow<Boolean?>(null)

    private val equipmentCounts = combine(
        repository.observeCameraBodies(),
        repository.observeLenses(),
        repository.observeLightMeters(),
        repository.observeFilmBacks(),
    ) { bodies, lenses, lightMeters, filmBacks ->
        EquipmentCounts(bodies.size, lenses.size, lightMeters.size, filmBacks.size)
    }

    private val pendingSyncCount = combine(
        repository.observeDirtyExposures(),
        repository.observeDirtyReferencePhotos(),
    ) { exposures, photos -> exposures.size + photos.size }

    val uiState: StateFlow<HomeUiState> = combine(
        equipmentCounts,
        repository.observeFilmRolls(),
        repository.observeAllExposures(),
        _watchReachable,
        pendingSyncCount,
    ) { equipment, rolls, exposures, reachable, pendingSync ->
        HomeUiState(
            isLoading = false,
            cameraBodyCount = equipment.cameraBodyCount,
            lensCount = equipment.lensCount,
            lightMeterCount = equipment.lightMeterCount,
            filmBackCount = equipment.filmBackCount,
            filmRollCount = rolls.size,
            exposureCount = exposures.size,
            pendingSyncCount = pendingSync,
            watchReachable = reachable,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refreshPairingStatus()
    }

    fun refreshPairingStatus() {
        _watchReachable.value = null
        viewModelScope.launch {
            _watchReachable.value = gateway.findReachableNodeId() != null
        }
    }

    fun syncNow() = triggerUpload()

    suspend fun exportAllCsv(): String = csvExportCoordinator.exportAll()
}
