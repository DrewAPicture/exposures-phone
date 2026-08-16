package com.exposures.phone.ui.lightmeter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.LightMeter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class LightMeterListUiState(val isLoading: Boolean = true, val lightMeters: List<LightMeter> = emptyList())

class LightMeterListViewModel(repository: EquipmentRepository) : ViewModel() {
    val uiState: StateFlow<LightMeterListUiState> = repository.observeLightMeters()
        .map { LightMeterListUiState(isLoading = false, lightMeters = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LightMeterListUiState())
}
