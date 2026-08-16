package com.exposures.datalayer.mapper

import com.exposures.datalayer.dto.CameraBodyDto
import com.exposures.datalayer.dto.ExposureDto
import com.exposures.datalayer.dto.FilmRollDto
import com.exposures.datalayer.dto.LensDto
import com.exposures.datalayer.dto.PhotoStatusDto
import com.exposures.datalayer.dto.ShutterSpeedDto
import com.exposures.model.CameraBody
import com.exposures.model.Exposure
import com.exposures.model.FilmFormat
import com.exposures.model.FilmRoll
import com.exposures.model.Lens
import com.exposures.model.PhotoStatus
import com.exposures.model.RollStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.ShutterSpeedKind
import com.exposures.model.StopIncrement
import com.exposures.model.SyncStatus

fun ShutterSpeed.toDto() = ShutterSpeedDto(kind.name, numerator, denominator)

fun ShutterSpeedDto.toDomain() = ShutterSpeed(ShutterSpeedKind.valueOf(kind), numerator, denominator)

fun CameraBody.toDto() = CameraBodyDto(
    id = id,
    name = name,
    manufacturer = manufacturer,
    availableShutterSpeeds = availableShutterSpeeds.map { it.toDto() },
    hasBulbMode = hasBulbMode,
    createdAt = createdAt,
    updatedAt = updatedAt,
    remoteId = remoteId,
)

/** [syncStatus] isn't part of the wire contract (it's local bookkeeping) — the receiving side sets its own. */
fun CameraBodyDto.toDomain(syncStatus: SyncStatus) = CameraBody(
    id = id,
    name = name,
    manufacturer = manufacturer,
    availableShutterSpeeds = availableShutterSpeeds.map { it.toDomain() },
    hasBulbMode = hasBulbMode,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun Lens.toDto() = LensDto(
    id = id,
    name = name,
    minAperture = minAperture,
    maxAperture = maxAperture,
    stopIncrement = stopIncrement.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    remoteId = remoteId,
)

fun LensDto.toDomain(syncStatus: SyncStatus) = Lens(
    id = id,
    name = name,
    minAperture = minAperture,
    maxAperture = maxAperture,
    stopIncrement = StopIncrement.valueOf(stopIncrement),
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun FilmRoll.toDto() = FilmRollDto(
    id = id,
    name = name,
    filmStock = filmStock,
    boxSpeedIso = boxSpeedIso,
    format = format.name,
    cameraBodyId = cameraBodyId,
    targetFrameCount = targetFrameCount,
    status = status.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    remoteId = remoteId,
)

fun FilmRollDto.toDomain(syncStatus: SyncStatus) = FilmRoll(
    id = id,
    name = name,
    filmStock = filmStock,
    boxSpeedIso = boxSpeedIso,
    format = FilmFormat.valueOf(format),
    cameraBodyId = cameraBodyId,
    targetFrameCount = targetFrameCount,
    status = RollStatus.valueOf(status),
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun Exposure.toDto() = ExposureDto(
    id = id,
    filmRollId = filmRollId,
    frameNumber = frameNumber,
    lensId = lensId,
    shutterSpeed = shutterSpeed.toDto(),
    aperture = aperture,
    isoUsed = isoUsed,
    notes = notes,
    capturedAt = capturedAt,
    referencePhotoStatus = referencePhotoStatus.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    remoteId = remoteId,
)

fun ExposureDto.toDomain(syncStatus: SyncStatus) = Exposure(
    id = id,
    filmRollId = filmRollId,
    frameNumber = frameNumber,
    lensId = lensId,
    shutterSpeed = shutterSpeed.toDomain(),
    aperture = aperture,
    isoUsed = isoUsed,
    notes = notes,
    capturedAt = capturedAt,
    referencePhotoStatus = PhotoStatus.valueOf(referencePhotoStatus),
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun Exposure.toPhotoStatusDto() = PhotoStatusDto(
    exposureId = id,
    referencePhotoStatus = referencePhotoStatus.name,
    remoteUrl = null,
)
