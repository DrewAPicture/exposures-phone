package com.exposures.model

/** Phone-authoritative: rolls are created/configured on the phone. The watch can only select among them. */
data class FilmRoll(
    val id: String,
    val name: String,
    val filmStock: String,
    val boxSpeedIso: Int,
    val format: FilmFormat,
    val colorType: FilmColorType,
    val cameraBodyId: String,
    val lightMeterId: String?,
    val filmBackId: String,
    val targetFrameCount: Int,
    val status: RollStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
)
