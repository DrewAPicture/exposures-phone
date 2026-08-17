package com.exposures.phone.sync

/**
 * Handles the watch's explicit refresh request by pushing the full phone-authoritative equipment
 * snapshot (bodies/lenses/meters/rolls), without waiting for local edit events to trigger normal
 * incremental pushes.
 */
class RequestRollsSyncHandler(private val syncPusher: EquipmentSyncPusher) {
    suspend fun handle() {
        syncPusher.pushCameraBodies()
        syncPusher.pushFilmBacks()
        syncPusher.pushLenses()
        syncPusher.pushLightMeters()
        syncPusher.pushFilmRolls()
    }
}
