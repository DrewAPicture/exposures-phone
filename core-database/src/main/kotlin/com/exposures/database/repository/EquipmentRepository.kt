package com.exposures.database.repository

import com.exposures.database.ExposuresDatabase
import com.exposures.database.mapper.toDomain
import com.exposures.database.mapper.toEntity
import com.exposures.model.CameraBody
import com.exposures.model.Exposure
import com.exposures.model.FilmRoll
import com.exposures.model.Lens
import com.exposures.model.LightMeter
import com.exposures.model.PhotoStatus
import com.exposures.model.ReferencePhoto
import com.exposures.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Phone's data-access surface: full CRUD for the equipment/rolls it's authoritative for, plus a
 * read-only mirror of the watch's exposures (see [mergeExposureSync]).
 */
class EquipmentRepository(private val database: ExposuresDatabase) {

    fun observeCameraBodies(): Flow<List<CameraBody>> =
        database.cameraBodyDao().getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getCameraBody(id: String): CameraBody? = database.cameraBodyDao().getById(id)?.toDomain()

    suspend fun saveCameraBody(body: CameraBody) = database.cameraBodyDao().save(body.toEntity())

    suspend fun deleteCameraBody(body: CameraBody) = database.cameraBodyDao().delete(body.toEntity())

    fun observeLenses(): Flow<List<Lens>> =
        database.lensDao().getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getLens(id: String): Lens? = database.lensDao().getById(id)?.toDomain()

    suspend fun saveLens(lens: Lens) = database.lensDao().save(lens.toEntity())

    suspend fun deleteLens(lens: Lens) = database.lensDao().delete(lens.toEntity())

    fun observeLightMeters(): Flow<List<LightMeter>> =
        database.lightMeterDao().getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getLightMeter(id: String): LightMeter? = database.lightMeterDao().getById(id)?.toDomain()

    suspend fun saveLightMeter(lightMeter: LightMeter) = database.lightMeterDao().save(lightMeter.toEntity())

    suspend fun deleteLightMeter(lightMeter: LightMeter) = database.lightMeterDao().delete(lightMeter.toEntity())

    fun observeFilmRolls(): Flow<List<FilmRoll>> =
        database.filmRollDao().getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getFilmRoll(id: String): FilmRoll? = database.filmRollDao().getById(id)?.toDomain()

    suspend fun saveFilmRoll(roll: FilmRoll) = database.filmRollDao().save(roll.toEntity())

    suspend fun deleteFilmRoll(roll: FilmRoll) = database.filmRollDao().delete(roll.toEntity())

    fun observeExposures(filmRollId: String): Flow<List<Exposure>> =
        database.exposureDao().getByRoll(filmRollId).map { entities -> entities.map { it.toDomain() } }

    fun observeAllExposures(): Flow<List<Exposure>> =
        database.exposureDao().getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getExposure(id: String): Exposure? = database.exposureDao().getById(id)?.toDomain()

    /**
     * Merges a fresh exposure list from the watch into the local mirror. The watch is
     * authoritative for everything about an exposure *except* [Exposure.referencePhotoStatus]
     * (phone-owned, see [updateExposurePhotoStatus]) and [Exposure.syncStatus]/[Exposure.remoteId]
     * (phone-owned relative to the *remote backend*, see [markExposureSynced]) — those can lag
     * behind what the phone already knows, so a locally-known value always wins over an incoming
     * one rather than being clobbered by a stale sync. An exposure the phone has never seen before
     * has no local state to preserve, so it's merged in exactly as the watch sent it — still
     * PENDING_SYNC to the backend, since the wire payload never carries syncStatus at all (see
     * ExposureSyncReceiver).
     */
    suspend fun mergeExposureSync(incoming: List<Exposure>) {
        val existingById = database.exposureDao().getAll().first().associate { it.id to it.toDomain() }
        val merged = incoming.map { exposure ->
            val known = existingById[exposure.id]
            if (known != null) {
                exposure.copy(
                    referencePhotoStatus = known.referencePhotoStatus,
                    syncStatus = known.syncStatus,
                    remoteId = known.remoteId,
                )
            } else {
                exposure
            }
        }
        database.exposureDao().replaceAll(merged.map { it.toEntity() })
    }

    /** Phone-local update after a capture attempt — does not touch any other field. */
    suspend fun updateExposurePhotoStatus(exposureId: String, status: PhotoStatus) {
        database.exposureDao().updatePhotoStatus(exposureId, status, System.currentTimeMillis())
    }

    /** Exposures not yet uploaded to the remote backend — see [com.exposures.phone.sync.UploadCoordinator]. */
    fun observeDirtyExposures(): Flow<List<Exposure>> =
        observeAllExposures().map { exposures -> exposures.filter { it.syncStatus != SyncStatus.SYNCED } }

    suspend fun getDirtyExposures(): List<Exposure> = observeDirtyExposures().first()

    suspend fun markExposureSynced(exposure: Exposure, remoteId: String) {
        val updated = exposure.copy(syncStatus = SyncStatus.SYNCED, remoteId = remoteId, updatedAt = System.currentTimeMillis())
        database.exposureDao().upsertAll(listOf(updated.toEntity()))
    }

    suspend fun markExposureSyncFailed(exposure: Exposure) {
        val updated = exposure.copy(syncStatus = SyncStatus.SYNC_FAILED, updatedAt = System.currentTimeMillis())
        database.exposureDao().upsertAll(listOf(updated.toEntity()))
    }

    suspend fun saveReferencePhoto(photo: ReferencePhoto) = database.referencePhotoDao().save(photo.toEntity())

    suspend fun getReferencePhoto(exposureId: String): ReferencePhoto? =
        database.referencePhotoDao().getByExposureId(exposureId)?.toDomain()

    fun observeAllReferencePhotos(): Flow<List<ReferencePhoto>> =
        database.referencePhotoDao().getAll().map { entities -> entities.map { it.toDomain() } }

    /** Captured photos not yet uploaded to the remote backend — a failed capture has no file to upload. */
    fun observeDirtyReferencePhotos(): Flow<List<ReferencePhoto>> =
        observeAllReferencePhotos().map { photos ->
            photos.filter { it.uploadStatus != SyncStatus.SYNCED && it.localUri != null }
        }

    suspend fun getDirtyReferencePhotos(): List<ReferencePhoto> = observeDirtyReferencePhotos().first()

    suspend fun markReferencePhotoSynced(photo: ReferencePhoto, remoteUrl: String) {
        saveReferencePhoto(photo.copy(uploadStatus = SyncStatus.SYNCED, remoteUrl = remoteUrl, lastError = null))
    }

    suspend fun markReferencePhotoSyncFailed(photo: ReferencePhoto, error: String) {
        saveReferencePhoto(photo.copy(uploadStatus = SyncStatus.SYNC_FAILED, retryCount = photo.retryCount + 1, lastError = error))
    }
}
