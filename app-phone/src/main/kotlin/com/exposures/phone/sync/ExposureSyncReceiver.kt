package com.exposures.phone.sync

import com.exposures.database.repository.EquipmentRepository
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.mapper.toDomain
import com.exposures.model.SyncStatus

/** Applies an incoming exposures payload from the watch to the local mirror. */
class ExposureSyncReceiver(private val repository: EquipmentRepository) {
    suspend fun handlePayload(json: String) {
        val exposures = DataLayerJson.decodeExposures(json).map { it.toDomain(syncStatus = SyncStatus.SYNCED) }
        repository.mergeExposureSync(exposures)
    }
}
