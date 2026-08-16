package com.exposures.phone.ui.filmroll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.FilmRoll
import com.exposures.phone.export.CsvExportCoordinator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class FilmRollListUiState(val isLoading: Boolean = true, val rolls: List<FilmRoll> = emptyList())

class FilmRollListViewModel(
    repository: EquipmentRepository,
    private val csvExportCoordinator: CsvExportCoordinator,
) : ViewModel() {
    val uiState: StateFlow<FilmRollListUiState> = repository.observeFilmRolls()
        .map { FilmRollListUiState(isLoading = false, rolls = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FilmRollListUiState())

    suspend fun exportCsv(rollId: String): String? = csvExportCoordinator.exportRoll(rollId)
}
