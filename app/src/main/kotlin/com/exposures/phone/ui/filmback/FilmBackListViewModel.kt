package com.exposures.phone.ui.filmback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.CameraBody
import com.exposures.model.FilmBack
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class FilmBackListItem(
    val filmBack: FilmBack,
    val cameraBodyName: String,
)

data class FilmBackListUiState(val isLoading: Boolean = true, val filmBacks: List<FilmBackListItem> = emptyList())

class FilmBackListViewModel(repository: EquipmentRepository) : ViewModel() {
    val uiState: StateFlow<FilmBackListUiState> = combine(
        repository.observeFilmBacks(),
        repository.observeCameraBodies(),
    ) { filmBacks, bodies ->
        FilmBackListUiState(isLoading = false, filmBacks = filmBacks.toFilmBackListItems(bodies))
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FilmBackListUiState())
}

private fun List<FilmBack>.toFilmBackListItems(bodies: List<CameraBody>): List<FilmBackListItem> {
    val bodyNamesById = bodies.associate { it.id to it.name }
    return map { filmBack ->
        FilmBackListItem(
            filmBack = filmBack,
            cameraBodyName = bodyNamesById[filmBack.cameraBodyId] ?: "Unknown body",
        )
    }
}
