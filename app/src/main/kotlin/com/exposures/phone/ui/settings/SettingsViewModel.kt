package com.exposures.phone.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import com.exposures.datalayer.DataLayerGateway
import com.exposures.phone.export.CsvExportCoordinator
import com.exposures.phone.settings.AppThemePreference
import com.exposures.phone.settings.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val pendingSyncCount: Int = 0,
    val watchReachable: Boolean? = null,
    val themePreference: AppThemePreference = AppThemePreference.SYSTEM,
)

class SettingsViewModel(
    repository: EquipmentRepository,
    private val gateway: DataLayerGateway,
    private val csvExportCoordinator: CsvExportCoordinator,
    private val themePreferences: ThemePreferences,
    private val triggerUpload: () -> Unit = {},
) : ViewModel() {
    private val watchReachable = MutableStateFlow<Boolean?>(null)
    private val pendingSyncCount = combine(
        repository.observeDirtyExposures(),
        repository.observeDirtyReferencePhotos(),
    ) { exposures, photos -> exposures.size + photos.size }

    val uiState: StateFlow<SettingsUiState> = combine(
        watchReachable,
        pendingSyncCount,
        themePreferences.preference,
    ) { reachable, pendingSync, themePreference ->
        SettingsUiState(
            isLoading = false,
            watchReachable = reachable,
            pendingSyncCount = pendingSync,
            themePreference = themePreference,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        refreshPairingStatus()
    }

    fun refreshPairingStatus() {
        watchReachable.value = null
        viewModelScope.launch {
            watchReachable.value = gateway.findReachableNodeId() != null
        }
    }

    fun syncNow() = triggerUpload()

    suspend fun exportAllCsv(): String = csvExportCoordinator.exportAll()

    fun setThemePreference(value: AppThemePreference) = themePreferences.setPreference(value)
}
