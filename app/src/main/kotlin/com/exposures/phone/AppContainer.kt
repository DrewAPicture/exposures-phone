package com.exposures.phone

import android.app.Application
import com.exposures.database.ExposuresDatabase
import com.exposures.database.ExposuresDatabaseProvider
import com.exposures.database.repository.EquipmentRepository
import com.exposures.datalayer.DataLayerClient
import com.exposures.phone.export.CsvExportCoordinator
import com.exposures.phone.sync.EquipmentSyncPusher
import com.exposures.phone.sync.UploadScheduler
import com.exposures.sync.NoOpAuthProvider
import com.exposures.sync.SyncApi
import com.exposures.sync.SyncApiFactory

/**
 * Hand-rolled DI container, matching the same reasoning as exposures-watch's AppContainer: this
 * app's dependency graph is small enough that Hilt's ceremony isn't earning its keep yet.
 */
interface AppContainer {
    val repository: EquipmentRepository
    val dataLayerClient: DataLayerClient
    val syncPusher: EquipmentSyncPusher
    val syncApi: SyncApi
    val csvExportCoordinator: CsvExportCoordinator

    /** Enqueues an upload-drain attempt (see UploadScheduler) — waits for connectivity if offline. */
    fun triggerUpload()
}

class DefaultAppContainer(private val application: Application) : AppContainer {

    private val database: ExposuresDatabase by lazy { ExposuresDatabaseProvider.create(application) }

    override val repository: EquipmentRepository by lazy { EquipmentRepository(database) }
    override val dataLayerClient: DataLayerClient by lazy { DataLayerClient(application) }
    override val syncPusher: EquipmentSyncPusher by lazy { EquipmentSyncPusher(repository, dataLayerClient) }

    // Placeholder — the backend doesn't exist yet (see core-sync's SyncApi doc). ".invalid" is the
    // RFC 2606 reserved TLD for addresses that are guaranteed never to resolve.
    override val syncApi: SyncApi by lazy { SyncApiFactory.create("https://sync.exposures.invalid/", NoOpAuthProvider) }
    override val csvExportCoordinator: CsvExportCoordinator by lazy { CsvExportCoordinator(repository) }

    override fun triggerUpload() = UploadScheduler.enqueue(application)
}
