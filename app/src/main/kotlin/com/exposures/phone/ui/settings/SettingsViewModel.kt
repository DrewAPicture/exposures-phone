package com.exposures.phone.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exposures.database.repository.EquipmentRepository
import com.exposures.datalayer.DataLayerGateway
import com.exposures.phone.export.CsvExportCoordinator
import com.exposures.phone.settings.AppThemePreference
import com.exposures.phone.settings.CaptureCameraPreference
import com.exposures.phone.settings.CaptureCameraPreferences
import com.exposures.phone.settings.ThemePreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val pendingSyncCount: Int = 0,
    val watchReachable: Boolean? = null,
    val themePreference: AppThemePreference = AppThemePreference.SYSTEM,
    val captureCameraPreference: CaptureCameraPreference = CaptureCameraPreference.REAR,
)

class SettingsViewModel(
    repository: EquipmentRepository,
    private val gateway: DataLayerGateway,
    private val csvExportCoordinator: CsvExportCoordinator,
    private val themePreferences: ThemePreferences,
    private val captureCameraPreferences: CaptureCameraPreferences,
    private val triggerUpload: () -> Unit = {},
) : ViewModel() {
    private val _savedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** One-shot "a setting was just saved" signal for the screen to show a transient
     * confirmation. Buffered (rather than the default zero-capacity) so a second save arriving
     * while the collector is still mid-`collect` on the first — e.g. its Snackbar coroutine is
     * still suspended showing the previous "Saved" toast — gets queued instead of silently
     * dropped by `tryEmit`. This does *not* help a collector that starts *after* an emission: with
     * `replay = 0`, a late subscriber never sees it regardless of buffer capacity — the buffer
     * only holds values for subscribers already collecting when they were emitted. */
    val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

    private val watchReachable = MutableStateFlow<Boolean?>(null)
    private val pendingSyncCount = combine(
        repository.observeDirtyExposures(),
        repository.observeDirtyReferencePhotos(),
    ) { exposures, photos -> exposures.size + photos.size }

    val uiState: StateFlow<SettingsUiState> = combine(
        watchReachable,
        pendingSyncCount,
        themePreferences.preference,
        captureCameraPreferences.preference,
    ) { reachable, pendingSync, themePreference, captureCameraPreference ->
        SettingsUiState(
            isLoading = false,
            watchReachable = reachable,
            pendingSyncCount = pendingSync,
            themePreference = themePreference,
            captureCameraPreference = captureCameraPreference,
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

    fun setThemePreference(value: AppThemePreference) {
        themePreferences.setPreference(value)
        _savedEvent.tryEmit(Unit)
    }

    fun setCaptureCameraPreference(value: CaptureCameraPreference) {
        captureCameraPreferences.setPreference(value)
        _savedEvent.tryEmit(Unit)
    }
}
