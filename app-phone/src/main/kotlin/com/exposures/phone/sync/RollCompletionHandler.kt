package com.exposures.phone.sync

import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.RollStatus

/**
 * Handles the watch's complete-roll command. The phone remains the sole writer of
 * `FilmRoll.status` — the watch only ever triggers the change, never applies it directly.
 */
class RollCompletionHandler(
    private val repository: EquipmentRepository,
    private val syncPusher: EquipmentSyncPusher,
) {
    suspend fun handle(rollId: String) {
        val roll = repository.getFilmRoll(rollId) ?: return
        repository.saveFilmRoll(roll.copy(status = RollStatus.COMPLETED, updatedAt = System.currentTimeMillis()))
        syncPusher.pushFilmRolls()
    }
}
