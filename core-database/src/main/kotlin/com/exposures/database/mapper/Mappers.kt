package com.exposures.database.mapper

import com.exposures.database.entity.CameraBodyEntity
import com.exposures.database.entity.ExposureEntity
import com.exposures.database.entity.FilmRollEntity
import com.exposures.database.entity.LensEntity
import com.exposures.database.entity.LightMeterEntity
import com.exposures.database.entity.ReferencePhotoEntity
import com.exposures.model.CameraBody
import com.exposures.model.Exposure
import com.exposures.model.FilmRoll
import com.exposures.model.Lens
import com.exposures.model.LightMeter
import com.exposures.model.ReferencePhoto

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
    cameraBodyId = cameraBodyId,
    minAperture = minAperture,
    maxAperture = maxAperture,
    stopIncrement = stopIncrement,
    referencePhotoZoomRatio = referencePhotoZoomRatio,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun Lens.toEntity() = LensEntity(
    id = id,
    name = name,
    cameraBodyId = cameraBodyId,
    minAperture = minAperture,
    maxAperture = maxAperture,
    stopIncrement = stopIncrement,
    referencePhotoZoomRatio = referencePhotoZoomRatio,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun LightMeterEntity.toDomain() = LightMeter(
    id = id,
    name = name,
    manufacturer = manufacturer,
    type = type,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun LightMeter.toEntity() = LightMeterEntity(
    id = id,
    name = name,
    manufacturer = manufacturer,
    type = type,
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
    colorType = colorType,
    cameraBodyId = cameraBodyId,
    lightMeterId = lightMeterId,
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
    colorType = colorType,
    cameraBodyId = cameraBodyId,
    lightMeterId = lightMeterId,
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
    zone = zone,
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
    zone = zone,
    notes = notes,
    capturedAt = capturedAt,
    referencePhotoStatus = referencePhotoStatus,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun ReferencePhotoEntity.toDomain() = ReferencePhoto(
    id = id,
    exposureId = exposureId,
    localUri = localUri,
    remoteUrl = remoteUrl,
    latitude = latitude,
    longitude = longitude,
    capturedAt = capturedAt,
    uploadStatus = uploadStatus,
    retryCount = retryCount,
    lastError = lastError,
)

fun ReferencePhoto.toEntity() = ReferencePhotoEntity(
    id = id,
    exposureId = exposureId,
    localUri = localUri,
    remoteUrl = remoteUrl,
    latitude = latitude,
    longitude = longitude,
    capturedAt = capturedAt,
    uploadStatus = uploadStatus,
    retryCount = retryCount,
    lastError = lastError,
)
