@file:JvmName("AppMappersKt")

package com.exposures.database.mapper

import com.exposures.database.entity.ExposureEntity
import com.exposures.database.entity.FilmBackEntity
import com.exposures.database.entity.FilmRollEntity
import com.exposures.database.entity.ReferencePhotoEntity
import com.exposures.model.Exposure
import com.exposures.model.FilmBack
import com.exposures.model.FilmRoll
import com.exposures.model.ReferencePhoto

fun FilmBackEntity.toDomain() = FilmBack(
    id = id,
    name = name,
    cameraBodyId = cameraBodyId,
    type = type,
    availableFrameCounts = availableFrameCounts,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus,
    remoteId = remoteId,
)

fun FilmBack.toEntity() = FilmBackEntity(
    id = id,
    name = name,
    cameraBodyId = cameraBodyId,
    type = type,
    availableFrameCounts = availableFrameCounts,
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
    filmBackId = filmBackId,
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
    filmBackId = filmBackId,
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
    focalLengthMm = focalLengthMm,
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
    focalLengthMm = focalLengthMm,
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
