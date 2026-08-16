package com.exposures.phone.sync

import com.exposures.database.repository.EquipmentRepository
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.mapper.toDomain
import com.exposures.model.SyncStatus

/** Applies an incoming exposures payload from the watch to the local mirror. */
class ExposureSyncReceiver(private val repository: EquipmentRepository) {
    suspend fun handlePayload(json: String) {
        // PENDING_SYNC here means "not yet uploaded to the remote backend" — this syncStatus is
        // unrelated to the watch<->phone Data Layer sync that just delivered this payload, which
        // is already complete by the time this runs. mergeExposureSync preserves the real,
        // locally-known syncStatus for any exposure the phone has already seen; this default only
        // takes effect for exposures the phone is seeing for the very first time.
        val exposures = DataLayerJson.decodeExposures(json).map { it.toDomain(syncStatus = SyncStatus.PENDING_SYNC) }
        repository.mergeExposureSync(exposures)
    }
}
