package com.exposures.phone.ui.camerabody

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.CameraBody
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CameraBodyListUiState(val isLoading: Boolean = true, val cameraBodies: List<CameraBody> = emptyList())

class CameraBodyListViewModel(repository: EquipmentRepository) : ViewModel() {
    val uiState: StateFlow<CameraBodyListUiState> = repository.observeCameraBodies()
        .map { CameraBodyListUiState(isLoading = false, cameraBodies = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CameraBodyListUiState())
}
