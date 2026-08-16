package com.exposures.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.exposures.database.repository.EquipmentRepository
import com.exposures.datalayer.DataLayerGateway
import com.exposures.phone.sync.EquipmentSyncPusher
import com.exposures.phone.ui.camerabody.CameraBodyEditViewModel
import com.exposures.phone.ui.camerabody.CameraBodyListViewModel
import com.exposures.phone.ui.filmroll.FilmRollEditViewModel
import com.exposures.phone.ui.filmroll.FilmRollListViewModel
import com.exposures.phone.ui.home.HomeViewModel
import com.exposures.phone.ui.lens.LensEditViewModel
import com.exposures.phone.ui.lens.LensListViewModel

/** Manual ViewModel factory, matching exposures-watch's approach — see AppContainer. */
class ExposuresViewModelFactory(
    private val repository: EquipmentRepository,
    private val syncPusher: EquipmentSyncPusher,
    private val dataLayerGateway: DataLayerGateway,
    private val entityId: String? = null,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        HomeViewModel::class.java -> HomeViewModel(repository, dataLayerGateway)
        CameraBodyListViewModel::class.java -> CameraBodyListViewModel(repository)
        CameraBodyEditViewModel::class.java -> CameraBodyEditViewModel(repository, syncPusher, entityId)
        LensListViewModel::class.java -> LensListViewModel(repository)
        LensEditViewModel::class.java -> LensEditViewModel(repository, syncPusher, entityId)
        FilmRollListViewModel::class.java -> FilmRollListViewModel(repository)
        FilmRollEditViewModel::class.java -> FilmRollEditViewModel(repository, syncPusher, entityId)
        else -> error("Unknown ViewModel class: $modelClass")
    } as T
}
