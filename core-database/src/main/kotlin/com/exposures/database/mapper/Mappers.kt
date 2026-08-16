package com.exposures.database.mapper

import com.exposures.database.entity.CameraBodyEntity
import com.exposures.database.entity.ExposureEntity
import com.exposures.database.entity.FilmRollEntity
import com.exposures.database.entity.LensEntity
import com.exposures.model.CameraBody
import com.exposures.model.Exposure
import com.exposures.model.FilmRoll
import com.exposures.model.Lens

fun CameraBodyEntity.toDomain() = CameraBody(
    id = id,
    name = name,
    manufacturer = manufacturer,
    availableShutterSpeeds = availableShutterSpeeds,
    hasBulbMode = hasBulbMode,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun CameraBody.toEntity() = CameraBodyEntity(
    id = id,
    name = name,
    manufacturer = manufacturer,
    availableShutterSpeeds = availableShutterSpeeds,
    hasBulbMode = hasBulbMode,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun LensEntity.toDomain() = Lens(
    id = id,
    name = name,
    minAperture = minAperture,
    maxAperture = maxAperture,
    stopIncrement = stopIncrement,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun Lens.toEntity() = LensEntity(
    id = id,
    name = name,
    minAperture = minAperture,
    maxAperture = maxAperture,
    stopIncrement = stopIncrement,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun FilmRollEntity.toDomain() = FilmRoll(
    id = id,
    name = name,
    filmStock = filmStock,
    boxSpeedIso = boxSpeedIso,
    format = format,
    cameraBodyId = cameraBodyId,
    targetFrameCount = targetFrameCount,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun FilmRoll.toEntity() = FilmRollEntity(
    id = id,
    name = name,
    filmStock = filmStock,
    boxSpeedIso = boxSpeedIso,
    format = format,
    cameraBodyId = cameraBodyId,
    targetFrameCount = targetFrameCount,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun ExposureEntity.toDomain() = Exposure(
    id = id,
    filmRollId = filmRollId,
    frameNumber = frameNumber,
    lensId = lensId,
    shutterSpeed = shutterSpeed,
    aperture = aperture,
    isoUsed = isoUsed,
    notes = notes,
    capturedAt = capturedAt,
    referencePhotoStatus = referencePhotoStatus,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun Exposure.toEntity() = ExposureEntity(
    id = id,
    filmRollId = filmRollId,
    frameNumber = frameNumber,
    lensId = lensId,
    shutterSpeed = shutterSpeed,
    aperture = aperture,
    isoUsed = isoUsed,
    notes = notes,
    capturedAt = capturedAt,
    referencePhotoStatus = referencePhotoStatus,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)
