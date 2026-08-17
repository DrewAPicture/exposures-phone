package com.exposures.datalayer.mapper

import com.exposures.datalayer.dto.CameraBodyDto
import com.exposures.datalayer.dto.ExposureDto
import com.exposures.datalayer.dto.FilmBackDto
import com.exposures.datalayer.dto.FilmRollDto
import com.exposures.datalayer.dto.LensDto
import com.exposures.datalayer.dto.LightMeterDto
import com.exposures.datalayer.dto.PhotoStatusDto
import com.exposures.datalayer.dto.ShutterSpeedDto
import com.exposures.model.CameraBody
import com.exposures.model.Exposure
import com.exposures.model.FilmBack
import com.exposures.model.FilmBackType
import com.exposures.model.FilmColorType
import com.exposures.model.FilmFormat
import com.exposures.model.FilmRoll
import com.exposures.model.Lens
import com.exposures.model.LightMeter
import com.exposures.model.LightMeterType
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
    cameraBodyId = cameraBodyId,
    minAperture = minAperture,
    maxAperture = maxAperture,
    stopIncrement = stopIncrement.name,
    referencePhotoZoomRatio = referencePhotoZoomRatio,
    createdAt = createdAt,
    updatedAt = updatedAt,
    remoteId = remoteId,
)

fun LensDto.toDomain(syncStatus: SyncStatus) = Lens(
    id = id,
    name = name,
    cameraBodyId = cameraBodyId,
    minAperture = minAperture,
    maxAperture = maxAperture,
    stopIncrement = StopIncrement.valueOf(stopIncrement),
    referencePhotoZoomRatio = referencePhotoZoomRatio,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun LightMeter.toDto() = LightMeterDto(
    id = id,
    name = name,
    manufacturer = manufacturer,
    type = type.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    remoteId = remoteId,
)

fun LightMeterDto.toDomain(syncStatus: SyncStatus) = LightMeter(
    id = id,
    name = name,
    manufacturer = manufacturer,
    type = LightMeterType.valueOf(type),
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun FilmBack.toDto() = FilmBackDto(
    id = id,
    name = name,
    cameraBodyId = cameraBodyId,
    type = type.name,
    availableFrameCounts = availableFrameCounts,
    createdAt = createdAt,
    updatedAt = updatedAt,
    remoteId = remoteId,
)

fun FilmBackDto.toDomain(syncStatus: SyncStatus) = FilmBack(
    id = id,
    name = name,
    cameraBodyId = cameraBodyId,
    type = FilmBackType.valueOf(type),
    availableFrameCounts = availableFrameCounts,
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
    colorType = colorType.name,
    cameraBodyId = cameraBodyId,
    lightMeterId = lightMeterId,
    filmBackId = filmBackId,
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
    colorType = FilmColorType.valueOf(colorType),
    cameraBodyId = cameraBodyId,
    lightMeterId = lightMeterId,
    filmBackId = filmBackId,
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
    zone = zone,
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
    zone = zone,
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
