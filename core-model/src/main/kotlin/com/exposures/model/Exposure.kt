package com.exposures.model

/** Watch-authoritative: exposures are recorded on the watch against the currently active roll. */
data class Exposure(
    val id: String,
    val filmRollId: String,
    val frameNumber: Int,
    val lensId: String,
    val shutterSpeed: ShutterSpeed,
    val aperture: Double,
    val isoUsed: Int,
    /** 0..10 (Zone 0..X); set only when the roll's light meter is [LightMeterType.SPOT]. */
    val zone: Int?,
    val notes: String?,
    val capturedAt: Long,
    val referencePhotoStatus: PhotoStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
)
