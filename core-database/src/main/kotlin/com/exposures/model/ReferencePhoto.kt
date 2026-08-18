package com.exposures.model

/** Phone-owned: the reference photo captured for an exposure, plus its upload state. */
data class ReferencePhoto(
    val id: String,
    val exposureId: String,
    val localUri: String?,
    val remoteUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
    val capturedAt: Long?,
    val uploadStatus: SyncStatus,
    val retryCount: Int,
    val lastError: String?,
)
