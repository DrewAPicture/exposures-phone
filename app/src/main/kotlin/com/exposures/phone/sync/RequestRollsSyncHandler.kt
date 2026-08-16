package com.exposures.phone.sync

/**
 * Handles the watch's explicit "refresh rolls" request by pushing the phone-authoritative roll
 * list immediately, without waiting for a local edit event to trigger a normal sync push.
 */
class RequestRollsSyncHandler(private val syncPusher: EquipmentSyncPusher) {
    suspend fun handle() {
        syncPusher.pushFilmRolls()
    }
}
