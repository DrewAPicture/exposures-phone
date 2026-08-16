package com.exposures.model

/** Phone-authoritative: camera bodies are configured on the phone, the watch only ever reads them. */
data class CameraBody(
    val id: String,
    val name: String,
    val manufacturer: String,
    val availableShutterSpeeds: List<ShutterSpeed>,
    val hasBulbMode: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val remoteId: String?,
)
