package com.exposures.phone.sync

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Drains locally-recorded exposures and their reference photos to the (not-yet-built) remote
 * backend. Exposures are drained first — not because photo upload depends on it (association is
 * by exposureId, which is stable regardless of upload order), but because it keeps the common
 * case (exposure already synced, photo still pending) the more common shape in a partial drain.
 *
 * [context] is only needed to read a `content://` reference photo (see [toMultipart]) — the
 * MediaStore-backed capture path on API 29+ produces one. It defaults to null so existing tests
 * that only exercise `file://` photos don't need to supply one; the one production call site
 * ([UploadWorker]) always passes [android.content.Context.getApplicationContext].
 */
class UploadCoordinator(
    private val repository: EquipmentRepository,
    private val syncApi: SyncApi,
    private val context: Context? = null,
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
        upload = { photo -> syncApi.uploadReferencePhoto(photo.exposureId, photo.toMultipart(context)).remoteUrl },
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

/**
 * [ReferencePhoto.localUri] is guaranteed non-null here — [EquipmentRepository.getDirtyReferencePhotos]
 * filters out photos with none. Handles both URI shapes [com.exposures.phone.capture.CaptureForegroundService] can produce:
 * `content://` (the API 29+ MediaStore capture path, read via [context]'s resolver — falls back to
 * `id.jpg` if the provider has no display name for it) and legacy `file://` (pre-API-29 fallback).
 */
private fun ReferencePhoto.toMultipart(context: Context?): MultipartBody.Part {
    val uri = Uri.parse(requireNotNull(localUri))
    if (uri.scheme == "content") {
        val resolver = requireNotNull(context) { "Context is required to upload content:// reference photos." }.contentResolver
        val bytes = requireNotNull(resolver.openInputStream(uri)) { "Unable to open $uri" }.use { it.readBytes() }
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: "${id}.jpg"
        return MultipartBody.Part.createFormData(
            "photo",
            displayName,
            bytes.toRequestBody("image/jpeg".toMediaType()),
        )
    }

    val file = File(URI(uri.toString()))
    return MultipartBody.Part.createFormData("photo", file.name, file.asRequestBody("image/jpeg".toMediaType()))
}
