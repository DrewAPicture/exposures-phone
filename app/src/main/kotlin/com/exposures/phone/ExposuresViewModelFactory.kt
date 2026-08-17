package com.exposures.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.exposures.database.repository.EquipmentRepository
import com.exposures.datalayer.DataLayerGateway
import com.exposures.phone.export.CsvExportCoordinator
import com.exposures.phone.settings.ThemePreferences
import com.exposures.phone.sync.EquipmentSyncPusher
import com.exposures.phone.ui.camerabody.CameraBodyEditViewModel
import com.exposures.phone.ui.camerabody.CameraBodyListViewModel
import com.exposures.phone.ui.filmback.FilmBackEditViewModel
import com.exposures.phone.ui.filmback.FilmBackListViewModel
import com.exposures.phone.ui.filmroll.FilmRollEditViewModel
import com.exposures.phone.ui.filmroll.FilmRollListViewModel
import com.exposures.phone.ui.home.HomeViewModel
import com.exposures.phone.ui.lens.LensEditViewModel
import com.exposures.phone.ui.lens.LensListViewModel
import com.exposures.phone.ui.lightmeter.LightMeterEditViewModel
import com.exposures.phone.ui.lightmeter.LightMeterListViewModel
import com.exposures.phone.ui.settings.SettingsViewModel

/** Manual ViewModel factory, matching exposures-watch's approach — see AppContainer. */
class ExposuresViewModelFactory(
    private val repository: EquipmentRepository,
    private val syncPusher: EquipmentSyncPusher,
    private val dataLayerGateway: DataLayerGateway,
    private val csvExportCoordinator: CsvExportCoordinator? = null,
    private val themePreferences: ThemePreferences? = null,
    private val entityId: String? = null,
    private val triggerUpload: () -> Unit = {},
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        HomeViewModel::class.java -> HomeViewModel(repository)
        SettingsViewModel::class.java ->
            SettingsViewModel(
                repository,
                dataLayerGateway,
                requireNotNull(csvExportCoordinator),
                requireNotNull(themePreferences),
                triggerUpload,
            )
        CameraBodyListViewModel::class.java -> CameraBodyListViewModel(repository)
        CameraBodyEditViewModel::class.java -> CameraBodyEditViewModel(repository, syncPusher, entityId)
        LensListViewModel::class.java -> LensListViewModel(repository)
        LensEditViewModel::class.java -> LensEditViewModel(repository, syncPusher, entityId)
        LightMeterListViewModel::class.java -> LightMeterListViewModel(repository)
        LightMeterEditViewModel::class.java -> LightMeterEditViewModel(repository, syncPusher, entityId)
        FilmBackListViewModel::class.java -> FilmBackListViewModel(repository)
        FilmBackEditViewModel::class.java -> FilmBackEditViewModel(repository, syncPusher, entityId)
        FilmRollListViewModel::class.java -> FilmRollListViewModel(repository, requireNotNull(csvExportCoordinator))
        FilmRollEditViewModel::class.java -> FilmRollEditViewModel(repository, syncPusher, entityId)
        else -> error("Unknown ViewModel class: $modelClass")
    } as T
}
