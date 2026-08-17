package com.exposures.database.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.exposures.database.ExposuresDatabase
import com.exposures.model.CameraBody
import com.exposures.model.Exposure
import com.exposures.model.FilmColorType
import com.exposures.model.FilmFormat
import com.exposures.model.FilmRoll
import com.exposures.model.Lens
import com.exposures.model.LightMeter
import com.exposures.model.LightMeterType
import com.exposures.model.PhotoStatus
import com.exposures.model.ReferencePhoto
import com.exposures.model.RollStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.StopIncrement
import com.exposures.model.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class EquipmentRepositoryTest {

    private lateinit var database: ExposuresDatabase
    private lateinit var repository: EquipmentRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ExposuresDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = EquipmentRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun cameraBody(id: String = UUID.randomUUID().toString()) = CameraBody(
        id = id,
        name = "RZ67 Pro II",
        manufacturer = "Mamiya",
        availableShutterSpeeds = ShutterSpeed.standardRange(ShutterSpeed.fraction(400), ShutterSpeed.wholeSeconds(8)),
        hasBulbMode = true,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.PENDING_SYNC,
        remoteId = null,
    )

    private fun lens(id: String = UUID.randomUUID().toString()) = Lens(
        id = id,
        name = "110mm f/2.8 W",
        cameraBodyId = null,
        minAperture = 2.8,
        maxAperture = 32.0,
        stopIncrement = StopIncrement.HALF_STOP,
        referencePhotoZoomRatio = 1.0,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.PENDING_SYNC,
        remoteId = null,
    )

    private fun lightMeter(id: String = UUID.randomUUID().toString()) = LightMeter(
        id = id,
        name = "Spotmeter V",
        manufacturer = "Pentax",
        type = LightMeterType.SPOT,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.PENDING_SYNC,
        remoteId = null,
    )

    private fun filmRoll(id: String = UUID.randomUUID().toString(), cameraBodyId: String, lightMeterId: String? = null) = FilmRoll(
        id = id,
        name = "Portra 400 — Roll 1",
        filmStock = "Kodak Portra 400",
        boxSpeedIso = 400,
        format = FilmFormat.MEDIUM_FORMAT_120,
        colorType = FilmColorType.COLOR,
        cameraBodyId = cameraBodyId,
        lightMeterId = lightMeterId,
        targetFrameCount = 10,
        status = RollStatus.AVAILABLE,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.PENDING_SYNC,
        remoteId = null,
    )

    private fun exposure(filmRollId: String, frameNumber: Int) = Exposure(
        id = UUID.randomUUID().toString(),
        filmRollId = filmRollId,
        frameNumber = frameNumber,
        lensId = "lens-1",
        shutterSpeed = ShutterSpeed.fraction(125),
        aperture = 8.0,
        isoUsed = 400,
        zone = null,
        notes = null,
        capturedAt = 0L,
        referencePhotoStatus = PhotoStatus.NONE,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    @Test
    fun `saved camera body is observable`() = runTest {
        val body = cameraBody()
        repository.saveCameraBody(body)

        assertEquals(listOf(body), repository.observeCameraBodies().first())
    }

    @Test
    fun `saving a camera body with the same id updates it in place`() = runTest {
        val body = cameraBody(id = "body-1")
        repository.saveCameraBody(body)

        val renamed = body.copy(name = "RZ67 Pro II (backup body)")
        repository.saveCameraBody(renamed)

        val all = repository.observeCameraBodies().first()
        assertEquals(1, all.size)
        assertEquals("RZ67 Pro II (backup body)", all.single().name)
    }

    @Test
    fun `deleting a camera body removes it`() = runTest {
        val body = cameraBody()
        repository.saveCameraBody(body)

        repository.deleteCameraBody(body)

        assertTrue(repository.observeCameraBodies().first().isEmpty())
        assertNull(repository.getCameraBody(body.id))
    }

    @Test
    fun `lens CRUD round-trips`() = runTest {
        val savedLens = lens()
        repository.saveLens(savedLens)
        assertEquals(savedLens, repository.getLens(savedLens.id))

        repository.deleteLens(savedLens)
        assertNull(repository.getLens(savedLens.id))
    }

    @Test
    fun `a lens's associated camera body persists`() = runTest {
        val body = cameraBody()
        repository.saveCameraBody(body)
        val savedLens = lens().copy(cameraBodyId = body.id)

        repository.saveLens(savedLens)

        assertEquals(body.id, repository.getLens(savedLens.id)?.cameraBodyId)
    }

    @Test
    fun `light meter CRUD round-trips`() = runTest {
        val savedLightMeter = lightMeter()
        repository.saveLightMeter(savedLightMeter)
        assertEquals(savedLightMeter, repository.getLightMeter(savedLightMeter.id))

        repository.deleteLightMeter(savedLightMeter)
        assertNull(repository.getLightMeter(savedLightMeter.id))
    }

    @Test
    fun `film roll CRUD round-trips`() = runTest {
        val body = cameraBody()
        repository.saveCameraBody(body)
        val roll = filmRoll(cameraBodyId = body.id)

        repository.saveFilmRoll(roll)

        assertEquals(roll, repository.getFilmRoll(roll.id))
    }

    @Test
    fun `film roll CRUD round-trips with a light meter assigned`() = runTest {
        val body = cameraBody()
        repository.saveCameraBody(body)
        val meter = lightMeter()
        repository.saveLightMeter(meter)
        val roll = filmRoll(cameraBodyId = body.id, lightMeterId = meter.id)

        repository.saveFilmRoll(roll)

        assertEquals(meter.id, repository.getFilmRoll(roll.id)?.lightMeterId)
    }

    @Test
    fun `mergeExposureSync replaces the entire exposure mirror`() = runTest {
        val rollId = "roll-1"
        repository.mergeExposureSync(listOf(exposure(rollId, 1), exposure(rollId, 2)))

        // A second sync from the watch (e.g. after the watch's own state changed) should fully
        // replace the mirror, not merge with what's already there.
        repository.mergeExposureSync(listOf(exposure(rollId, 1)))

        val exposures = repository.observeExposures(rollId).first()
        assertEquals(1, exposures.size)
    }

    @Test
    fun `mergeExposureSync with an empty list clears the mirror`() = runTest {
        repository.mergeExposureSync(listOf(exposure("roll-1", 1)))

        repository.mergeExposureSync(emptyList())

        assertTrue(repository.observeAllExposures().first().isEmpty())
    }

    @Test
    fun `mergeExposureSync preserves a locally-known photo status over a stale incoming one`() = runTest {
        val original = exposure("roll-1", 1)
        repository.mergeExposureSync(listOf(original))
        repository.updateExposurePhotoStatus(original.id, PhotoStatus.CAPTURED)

        // The watch hasn't caught up yet and re-syncs with the old NONE status for this exposure.
        repository.mergeExposureSync(listOf(original.copy(referencePhotoStatus = PhotoStatus.NONE)))

        val merged = repository.observeExposures("roll-1").first().single()
        assertEquals(PhotoStatus.CAPTURED, merged.referencePhotoStatus)
    }

    @Test
    fun `mergeExposureSync takes the incoming status for an exposure the phone has never seen`() = runTest {
        val brandNew = exposure("roll-1", 1).copy(referencePhotoStatus = PhotoStatus.NONE)

        repository.mergeExposureSync(listOf(brandNew))

        assertEquals(PhotoStatus.NONE, repository.observeExposures("roll-1").first().single().referencePhotoStatus)
    }

    @Test
    fun `updateExposurePhotoStatus only touches the targeted exposure`() = runTest {
        val a = exposure("roll-1", 1)
        val b = exposure("roll-1", 2)
        repository.mergeExposureSync(listOf(a, b))

        repository.updateExposurePhotoStatus(a.id, PhotoStatus.CAPTURED)

        val exposures = repository.observeExposures("roll-1").first().associateBy { it.id }
        assertEquals(PhotoStatus.CAPTURED, exposures.getValue(a.id).referencePhotoStatus)
        assertEquals(PhotoStatus.NONE, exposures.getValue(b.id).referencePhotoStatus)
    }

    @Test
    fun `getExposure returns a synced exposure by id`() = runTest {
        val exposure = exposure("roll-1", 1)
        repository.mergeExposureSync(listOf(exposure))

        assertEquals(exposure, repository.getExposure(exposure.id))
    }

    @Test
    fun `getExposure returns null for an unknown id`() = runTest {
        assertNull(repository.getExposure("does-not-exist"))
    }

    @Test
    fun `reference photo round-trips by exposure id`() = runTest {
        val photo = ReferencePhoto(
            id = UUID.randomUUID().toString(),
            exposureId = "exp-1",
            localUri = "file:///photo.jpg",
            remoteUrl = null,
            latitude = 47.6,
            longitude = -122.3,
            capturedAt = 1000L,
            uploadStatus = SyncStatus.PENDING_SYNC,
            retryCount = 0,
            lastError = null,
        )

        repository.saveReferencePhoto(photo)

        assertEquals(photo, repository.getReferencePhoto("exp-1"))
    }

    @Test
    fun `saving a new reference photo for the same exposure replaces the earlier attempt`() = runTest {
        val firstAttempt = ReferencePhoto(
            id = UUID.randomUUID().toString(), exposureId = "exp-1", localUri = null, remoteUrl = null,
            latitude = null, longitude = null, capturedAt = null, uploadStatus = SyncStatus.SYNC_FAILED,
            retryCount = 1, lastError = "camera busy",
        )
        repository.saveReferencePhoto(firstAttempt)

        val retry = ReferencePhoto(
            id = UUID.randomUUID().toString(), exposureId = "exp-1", localUri = "file:///retry.jpg",
            remoteUrl = null, latitude = null, longitude = null, capturedAt = 2000L,
            uploadStatus = SyncStatus.PENDING_SYNC, retryCount = 0, lastError = null,
        )
        repository.saveReferencePhoto(retry)

        assertEquals(retry, repository.getReferencePhoto("exp-1"))
    }

    @Test
    fun `getReferencePhoto returns null when none has been captured`() = runTest {
        assertNull(repository.getReferencePhoto("exp-without-a-photo"))
    }

    @Test
    fun `getDirtyExposures excludes exposures already synced to the backend`() = runTest {
        val dirty = exposure("roll-1", 1).copy(syncStatus = SyncStatus.PENDING_SYNC)
        val synced = exposure("roll-1", 2).copy(syncStatus = SyncStatus.SYNCED)
        repository.mergeExposureSync(listOf(dirty, synced))

        val dirtyExposures = repository.getDirtyExposures()

        assertEquals(listOf(dirty.id), dirtyExposures.map { it.id })
    }

    @Test
    fun `markExposureSynced records the remote id and SYNCED status`() = runTest {
        val original = exposure("roll-1", 1).copy(syncStatus = SyncStatus.PENDING_SYNC)
        repository.mergeExposureSync(listOf(original))

        repository.markExposureSynced(original, remoteId = "server-1")

        val updated = repository.getExposure(original.id)!!
        assertEquals(SyncStatus.SYNCED, updated.syncStatus)
        assertEquals("server-1", updated.remoteId)
    }

    @Test
    fun `markExposureSyncFailed records SYNC_FAILED without a remote id`() = runTest {
        val original = exposure("roll-1", 1).copy(syncStatus = SyncStatus.PENDING_SYNC)
        repository.mergeExposureSync(listOf(original))

        repository.markExposureSyncFailed(original)

        val updated = repository.getExposure(original.id)!!
        assertEquals(SyncStatus.SYNC_FAILED, updated.syncStatus)
        assertNull(updated.remoteId)
    }

    @Test
    fun `getDirtyReferencePhotos excludes photos already synced and photos with no local file`() = runTest {
        val synced = ReferencePhoto(
            id = UUID.randomUUID().toString(), exposureId = "exp-synced", localUri = "file:///synced.jpg",
            remoteUrl = "https://cdn.example/synced.jpg", latitude = null, longitude = null, capturedAt = 1L,
            uploadStatus = SyncStatus.SYNCED, retryCount = 0, lastError = null,
        )
        val pending = ReferencePhoto(
            id = UUID.randomUUID().toString(), exposureId = "exp-pending", localUri = "file:///pending.jpg",
            remoteUrl = null, latitude = null, longitude = null, capturedAt = 2L,
            uploadStatus = SyncStatus.PENDING_SYNC, retryCount = 0, lastError = null,
        )
        val captureFailed = ReferencePhoto(
            id = UUID.randomUUID().toString(), exposureId = "exp-capture-failed", localUri = null,
            remoteUrl = null, latitude = null, longitude = null, capturedAt = null,
            uploadStatus = SyncStatus.SYNC_FAILED, retryCount = 0, lastError = "camera busy",
        )
        repository.saveReferencePhoto(synced)
        repository.saveReferencePhoto(pending)
        repository.saveReferencePhoto(captureFailed)

        val dirty = repository.getDirtyReferencePhotos()

        assertEquals(listOf("exp-pending"), dirty.map { it.exposureId })
    }

    @Test
    fun `markReferencePhotoSynced records the remote url and clears any previous error`() = runTest {
        val photo = ReferencePhoto(
            id = UUID.randomUUID().toString(), exposureId = "exp-1", localUri = "file:///photo.jpg",
            remoteUrl = null, latitude = null, longitude = null, capturedAt = 1L,
            uploadStatus = SyncStatus.SYNC_FAILED, retryCount = 1, lastError = "timed out",
        )
        repository.saveReferencePhoto(photo)

        repository.markReferencePhotoSynced(photo, remoteUrl = "https://cdn.example/photo.jpg")

        val updated = repository.getReferencePhoto("exp-1")!!
        assertEquals(SyncStatus.SYNCED, updated.uploadStatus)
        assertEquals("https://cdn.example/photo.jpg", updated.remoteUrl)
        assertNull(updated.lastError)
    }

    @Test
    fun `markReferencePhotoSyncFailed increments the retry count and records the error`() = runTest {
        val photo = ReferencePhoto(
            id = UUID.randomUUID().toString(), exposureId = "exp-1", localUri = "file:///photo.jpg",
            remoteUrl = null, latitude = null, longitude = null, capturedAt = 1L,
            uploadStatus = SyncStatus.PENDING_SYNC, retryCount = 0, lastError = null,
        )
        repository.saveReferencePhoto(photo)

        repository.markReferencePhotoSyncFailed(photo, error = "connection reset")

        val updated = repository.getReferencePhoto("exp-1")!!
        assertEquals(SyncStatus.SYNC_FAILED, updated.uploadStatus)
        assertEquals(1, updated.retryCount)
        assertEquals("connection reset", updated.lastError)
    }
}
