package com.exposures.phone.sync

import com.exposures.database.repository.EquipmentRepository
import com.exposures.datalayer.DataLayerGateway
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.datalayer.mapper.toDto
import kotlinx.coroutines.flow.first

/**
 * Pushes the phone's equipment/roll state to the watch. Called explicitly after each save/delete
 * rather than continuously observed in the background — this app's write volume is low enough
 * (a person editing gear/rolls, not a stream of data) that an explicit push-on-change is simpler
 * than running a background observer for Phase 2.
 */
class EquipmentSyncPusher(
    private val repository: EquipmentRepository,
    private val gateway: DataLayerGateway,
) {
    suspend fun pushCameraBodies() {
        val bodies = repository.observeCameraBodies().first().map { it.toDto() }
        gateway.putPayload(DataLayerPaths.CAMERA_BODIES, DataLayerJson.encodeCameraBodies(bodies))
    }

    suspend fun pushLenses() {
        val lenses = repository.observeLenses().first().map { it.toDto() }
        gateway.putPayload(DataLayerPaths.LENSES, DataLayerJson.encodeLenses(lenses))
    }

    suspend fun pushLightMeters() {
        val lightMeters = repository.observeLightMeters().first().map { it.toDto() }
        gateway.putPayload(DataLayerPaths.LIGHT_METERS, DataLayerJson.encodeLightMeters(lightMeters))
    }

    suspend fun pushFilmRolls() {
        val rolls = repository.observeFilmRolls().first().map { it.toDto() }
        gateway.putPayload(DataLayerPaths.ROLLS, DataLayerJson.encodeRolls(rolls))
    }
}
