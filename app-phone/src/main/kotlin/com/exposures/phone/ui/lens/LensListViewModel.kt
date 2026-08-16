package com.exposures.phone.ui.lens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.Lens
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class LensListUiState(val isLoading: Boolean = true, val lenses: List<Lens> = emptyList())

class LensListViewModel(repository: EquipmentRepository) : ViewModel() {
    val uiState: StateFlow<LensListUiState> = repository.observeLenses()
        .map { LensListUiState(isLoading = false, lenses = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LensListUiState())
}
