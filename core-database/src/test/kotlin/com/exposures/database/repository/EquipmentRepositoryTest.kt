package com.exposures.database.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.exposures.database.ExposuresDatabase
import com.exposures.model.CameraBody
import com.exposures.model.Exposure
import com.exposures.model.FilmFormat
import com.exposures.model.FilmRoll
import com.exposures.model.Lens
import com.exposures.model.PhotoStatus
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
        minAperture = 2.8,
        maxAperture = 32.0,
        stopIncrement = StopIncrement.HALF_STOP,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.PENDING_SYNC,
        remoteId = null,
    )

    private fun filmRoll(id: String = UUID.randomUUID().toString(), cameraBodyId: String) = FilmRoll(
        id = id,
        name = "Portra 400 — Roll 1",
        filmStock = "Kodak Portra 400",
        boxSpeedIso = 400,
        format = FilmFormat.MEDIUM_FORMAT_120,
        cameraBodyId = cameraBodyId,
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
    fun `film roll CRUD round-trips`() = runTest {
        val body = cameraBody()
        repository.saveCameraBody(body)
        val roll = filmRoll(cameraBodyId = body.id)

        repository.saveFilmRoll(roll)

        assertEquals(roll, repository.getFilmRoll(roll.id))
    }

    @Test
    fun `applyExposureSync replaces the entire exposure mirror`() = runTest {
        val rollId = "roll-1"
        repository.applyExposureSync(listOf(exposure(rollId, 1), exposure(rollId, 2)))

        // A second sync from the watch (e.g. after the watch's own state changed) should fully
        // replace the mirror, not merge with what's already there.
        repository.applyExposureSync(listOf(exposure(rollId, 1)))

        val exposures = repository.observeExposures(rollId).first()
        assertEquals(1, exposures.size)
    }

    @Test
    fun `applyExposureSync with an empty list clears the mirror`() = runTest {
        repository.applyExposureSync(listOf(exposure("roll-1", 1)))

        repository.applyExposureSync(emptyList())

        assertTrue(repository.observeAllExposures().first().isEmpty())
    }
}
