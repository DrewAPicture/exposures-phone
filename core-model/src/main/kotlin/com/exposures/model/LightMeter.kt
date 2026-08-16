package com.exposures.model

/** Phone-authoritative: light meters are configured on the phone, the watch only ever reads them. */
data class LightMeter(
    val id: String,
    val name: String,
    val manufacturer: String,
    val type: LightMeterType,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
)
