package com.exposures.database.repository

import com.exposures.database.ExposuresDatabase
import com.exposures.database.mapper.toDomain
import com.exposures.database.mapper.toEntity
import com.exposures.model.CameraBody
import com.exposures.model.Exposure
import com.exposures.model.FilmRoll
import com.exposures.model.Lens
import com.exposures.model.PhotoStatus
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

    fun observeFilmRolls(): Flow<List<FilmRoll>> =
        database.filmRollDao().getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getFilmRoll(id: String): FilmRoll? = database.filmRollDao().getById(id)?.toDomain()

    suspend fun saveFilmRoll(roll: FilmRoll) = database.filmRollDao().save(roll.toEntity())

    suspend fun deleteFilmRoll(roll: FilmRoll) = database.filmRollDao().delete(roll.toEntity())

    fun observeExposures(filmRollId: String): Flow<List<Exposure>> =
        database.exposureDao().getByRoll(filmRollId).map { entities -> entities.map { it.toDomain() } }

    fun observeAllExposures(): Flow<List<Exposure>> =
        database.exposureDao().getAll().map { entities -> entities.map { it.toDomain() } }

    /**
     * Merges a fresh exposure list from the watch into the local mirror. The watch is
     * authoritative for everything about an exposure *except* [Exposure.referencePhotoStatus] —
     * that's phone-owned (see [updateExposurePhotoStatus]) and the watch's copy of it can lag
     * behind what the phone already knows, so a locally-known status always wins over an
     * incoming one rather than being clobbered by a stale sync.
     */
    suspend fun mergeExposureSync(incoming: List<Exposure>) {
        val existingById = database.exposureDao().getAll().first().associate { it.id to it.toDomain() }
        val merged = incoming.map { exposure ->
            val knownStatus = existingById[exposure.id]?.referencePhotoStatus
            if (knownStatus != null) exposure.copy(referencePhotoStatus = knownStatus) else exposure
        }
        database.exposureDao().replaceAll(merged.map { it.toEntity() })
    }

    /** Phone-local update after a (currently stubbed) capture — does not touch any other field. */
    suspend fun updateExposurePhotoStatus(exposureId: String, status: PhotoStatus) {
        database.exposureDao().updatePhotoStatus(exposureId, status, System.currentTimeMillis())
    }
}
