package com.exposures.phone.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val isLoading: Boolean = true,
    val cameraBodyCount: Int = 0,
    val lensCount: Int = 0,
    val lightMeterCount: Int = 0,
    val filmBackCount: Int = 0,
    val filmRollCount: Int = 0,
    val exposureCount: Int = 0,
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
) : ViewModel() {
    private val equipmentCounts = combine(
        repository.observeCameraBodies(),
        repository.observeLenses(),
        repository.observeLightMeters(),
        repository.observeFilmBacks(),
    ) { bodies, lenses, lightMeters, filmBacks ->
        EquipmentCounts(bodies.size, lenses.size, lightMeters.size, filmBacks.size)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        equipmentCounts,
        repository.observeFilmRolls(),
        repository.observeAllExposures(),
    ) { equipment, rolls, exposures ->
        HomeUiState(
            isLoading = false,
            cameraBodyCount = equipment.cameraBodyCount,
            lensCount = equipment.lensCount,
            lightMeterCount = equipment.lightMeterCount,
            filmBackCount = equipment.filmBackCount,
            filmRollCount = rolls.size,
            exposureCount = exposures.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
