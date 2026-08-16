package com.exposures.sync.dto

import kotlinx.serialization.Serializable

// Wire-format DTOs for the (not-yet-built) remote backend. Deliberately separate from both
// core-model's domain types and core-datalayer's watch/phone wire DTOs — this is an independent
// wire boundary with its own versioning, even though today its shape mirrors the domain closely.

@Serializable
data class ShutterSpeedSyncDto(val kind: String, val numerator: Int, val denominator: Int)

@Serializable
data class ExposureSyncDto(
    val id: String,
    val filmRollId: String,
    val frameNumber: Int,
    val lensId: String,
    val shutterSpeed: ShutterSpeedSyncDto,
    val aperture: Double,
    val isoUsed: Int,
    val zone: Int? = null,
    val notes: String? = null,
    val capturedAt: Long,
)

@Serializable
data class SyncAckDto(val remoteId: String)

@Serializable
data class ReferencePhotoAckDto(val remoteUrl: String)
