package com.exposures.datalayer.dto

import kotlinx.serialization.Serializable

// Wire-format DTOs for the Data Layer contract. Deliberately separate from core-model's domain
// types: these are what crosses the watch/phone boundary, so they carry no local-only bookkeeping
// (notably no syncStatus — that's per-device state, not something to transmit).

@Serializable
data class ShutterSpeedDto(val kind: String, val numerator: Int, val denominator: Int)

@Serializable
data class CameraBodyDto(
    val id: String,
    val name: String,
    val manufacturer: String,
    val availableShutterSpeeds: List<ShutterSpeedDto>,
    val hasBulbMode: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String? = null,
)

@Serializable
data class LensDto(
    val id: String,
    val name: String,
    val minAperture: Double,
    val maxAperture: Double,
    val stopIncrement: String,
    val referencePhotoZoomRatio: Double,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String? = null,
)

@Serializable
data class LightMeterDto(
    val id: String,
    val name: String,
    val manufacturer: String,
    val type: String,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String? = null,
)

@Serializable
data class FilmRollDto(
    val id: String,
    val name: String,
    val filmStock: String,
    val boxSpeedIso: Int,
    val format: String,
    val cameraBodyId: String,
    val lightMeterId: String? = null,
    val targetFrameCount: Int,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String? = null,
)

@Serializable
data class ExposureDto(
    val id: String,
    val filmRollId: String,
    val frameNumber: Int,
    val lensId: String,
    val shutterSpeed: ShutterSpeedDto,
    val aperture: Double,
    val isoUsed: Int,
    val zone: Int? = null,
    val notes: String? = null,
    val capturedAt: Long,
    val referencePhotoStatus: String,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String? = null,
)

@Serializable
data class PhotoStatusDto(
    val exposureId: String,
    val referencePhotoStatus: String,
    val remoteUrl: String? = null,
)

@Serializable
data class CapturePhotoCommand(
    val exposureId: String,
    val filmRollId: String,
    val frameNumber: Int,
)

@Serializable
data class CaptureResultCommand(
    val exposureId: String,
    val status: String,
)

@Serializable
data class CompleteRollCommand(
    val rollId: String,
)
