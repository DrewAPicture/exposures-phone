package com.exposures.model

/**
 * Phone-authoritative: film backs are configured on the phone, the watch only ever reads them.
 * Backs are body-specific, hence the required [cameraBodyId]. [availableFrameCounts] is
 * user-declared rather than computed — real-world frame-count variance (loading tightness,
 * backing paper, batch) has no clean formula, so the equipment owner just states what their own
 * back actually yields (usually one value, occasionally two).
 */
data class FilmBack(
    val id: String,
    val name: String,
    val cameraBodyId: String,
    val type: FilmBackType,
    val availableFrameCounts: List<Int>,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
)
