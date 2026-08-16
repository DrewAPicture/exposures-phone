package com.exposures.phone.ui.lens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.CameraBody
import com.exposures.model.Lens
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LensListItem(
    val lens: Lens,
    val cameraBodyName: String?,
)

data class LensListUiState(val isLoading: Boolean = true, val lenses: List<LensListItem> = emptyList())

class LensListViewModel(repository: EquipmentRepository) : ViewModel() {
    val uiState: StateFlow<LensListUiState> = combine(
        repository.observeLenses(),
        repository.observeCameraBodies(),
    ) { lenses, bodies ->
        LensListUiState(isLoading = false, lenses = lenses.toLensListItems(bodies))
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LensListUiState())
}

private fun List<Lens>.toLensListItems(bodies: List<CameraBody>): List<LensListItem> {
    val bodyNamesById = bodies.associate { it.id to it.name }
    return map { lens ->
        LensListItem(
            lens = lens,
            cameraBodyName = lens.cameraBodyId?.let(bodyNamesById::get),
        )
    }
}
