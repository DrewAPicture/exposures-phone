package com.exposures.phone.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import com.exposures.datalayer.DataLayerGateway
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
    val filmRollCount: Int = 0,
    val exposureCount: Int = 0,
    val watchReachable: Boolean? = null, // null while unchecked
)

class HomeViewModel(
    repository: EquipmentRepository,
    private val gateway: DataLayerGateway,
) : ViewModel() {

    private val _watchReachable = MutableStateFlow<Boolean?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeCameraBodies(),
        repository.observeLenses(),
        repository.observeFilmRolls(),
        repository.observeAllExposures(),
        _watchReachable,
    ) { bodies, lenses, rolls, exposures, reachable ->
        HomeUiState(
            isLoading = false,
            cameraBodyCount = bodies.size,
            lensCount = lenses.size,
            filmRollCount = rolls.size,
            exposureCount = exposures.size,
            watchReachable = reachable,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refreshPairingStatus()
    }

    fun refreshPairingStatus() {
        viewModelScope.launch {
            _watchReachable.value = gateway.findReachableNodeId() != null
        }
    }
}
