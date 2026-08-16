package com.exposures.phone

import android.app.Application
import com.exposures.database.ExposuresDatabase
import com.exposures.database.ExposuresDatabaseProvider
import com.exposures.database.repository.EquipmentRepository
import com.exposures.datalayer.DataLayerClient

/**
 * Hand-rolled DI container, matching the same reasoning as exposures-watch's AppContainer: this
 * app's dependency graph is small enough that Hilt's ceremony isn't earning its keep yet.
 */
interface AppContainer {
    val repository: EquipmentRepository
    val dataLayerClient: DataLayerClient
}

class DefaultAppContainer(private val application: Application) : AppContainer {

    private val database: ExposuresDatabase by lazy { ExposuresDatabaseProvider.create(application) }

    override val repository: EquipmentRepository by lazy { EquipmentRepository(database) }
    override val dataLayerClient: DataLayerClient by lazy { DataLayerClient(application) }
}
