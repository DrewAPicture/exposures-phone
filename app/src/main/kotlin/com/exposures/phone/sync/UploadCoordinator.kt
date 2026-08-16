package com.exposures.phone.sync

import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.Exposure
import com.exposures.model.ReferencePhoto
import com.exposures.sync.DrainResult
import com.exposures.sync.SyncApi
import com.exposures.sync.SyncDrainer
import com.exposures.sync.dto.ExposureSyncDto
import com.exposures.sync.dto.ShutterSpeedSyncDto
import java.io.File
import java.net.URI
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

/**
 * Drains locally-recorded exposures and their reference photos to the (not-yet-built) remote
 * backend. Exposures are drained first — not because photo upload depends on it (association is
 * by exposureId, which is stable regardless of upload order), but because it keeps the common
 * case (exposure already synced, photo still pending) the more common shape in a partial drain.
 */
class UploadCoordinator(
    private val repository: EquipmentRepository,
    private val syncApi: SyncApi,
    private val drainer: SyncDrainer = SyncDrainer(),
) {
    suspend fun drainExposures(): DrainResult = drainer.drain(
        items = repository.getDirtyExposures(),
        upload = { exposure -> syncApi.uploadExposure(exposure.toSyncDto()).remoteId },
        onSuccess = { exposure, remoteId -> repository.markExposureSynced(exposure, remoteId) },
        onFailure = { exposure, _ -> repository.markExposureSyncFailed(exposure) },
    )

    suspend fun drainReferencePhotos(): DrainResult = drainer.drain(
        items = repository.getDirtyReferencePhotos(),
        upload = { photo -> syncApi.uploadReferencePhoto(photo.exposureId, photo.toMultipart()).remoteUrl },
        onSuccess = { photo, remoteUrl -> repository.markReferencePhotoSynced(photo, remoteUrl) },
        onFailure = { photo, error -> repository.markReferencePhotoSyncFailed(photo, error) },
    )

    suspend fun drainAll(): DrainResult {
        val exposures = drainExposures()
        val photos = drainReferencePhotos()
        return DrainResult(exposures.succeeded + photos.succeeded, exposures.failed + photos.failed)
    }
}

private fun Exposure.toSyncDto() = ExposureSyncDto(
    id = id,
    filmRollId = filmRollId,
    frameNumber = frameNumber,
    lensId = lensId,
    shutterSpeed = ShutterSpeedSyncDto(shutterSpeed.kind.name, shutterSpeed.numerator, shutterSpeed.denominator),
    aperture = aperture,
    isoUsed = isoUsed,
    zone = zone,
    notes = notes,
    capturedAt = capturedAt,
)

/** [ReferencePhoto.localUri] is guaranteed non-null here — [EquipmentRepository.getDirtyReferencePhotos] filters out photos with none. */
private fun ReferencePhoto.toMultipart(): MultipartBody.Part {
    val file = File(URI(requireNotNull(localUri)))
    return MultipartBody.Part.createFormData("photo", file.name, file.asRequestBody("image/jpeg".toMediaType()))
}
