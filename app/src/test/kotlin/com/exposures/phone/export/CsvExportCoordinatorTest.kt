package com.exposures.phone.export

import com.exposures.model.CameraBody
import com.exposures.model.Exposure
import com.exposures.model.FilmBack
import com.exposures.model.FilmBackType
import com.exposures.model.FilmColorType
import com.exposures.model.FilmFormat
import com.exposures.model.FilmRoll
import com.exposures.model.Lens
import com.exposures.model.PhotoStatus
import com.exposures.model.RollStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.StopIncrement
import com.exposures.model.SyncStatus
import com.exposures.phone.createTestRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CsvExportCoordinatorTest {

    private fun cameraBody(id: String) = CameraBody(
        id = id, name = "RZ67", manufacturer = "Mamiya", availableShutterSpeeds = listOf(ShutterSpeed.fraction(125)),
        hasBulbMode = true, createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, remoteId = null,
    )

    private fun filmBack(id: String) = FilmBack(
        id = id, name = "6x7 back", cameraBodyId = "body-1", type = FilmBackType.ROLL_6X7,
        availableFrameCounts = listOf(10), createdAt = 0L, updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED, remoteId = null,
    )

    private fun roll(id: String, name: String) = FilmRoll(
        id = id, name = name, filmStock = "Portra 400", boxSpeedIso = 400,
        format = FilmFormat.MEDIUM_FORMAT_120, colorType = FilmColorType.COLOR, cameraBodyId = "body-1", lightMeterId = null,
        filmBackId = "back-1", targetFrameCount = 10, status = RollStatus.AVAILABLE,
        createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, remoteId = null,
    )

    private fun lens(id: String, name: String) = Lens(
        id = id, name = name, cameraBodyId = null, minAperture = 4.5, maxAperture = 45.0, stopIncrement = StopIncrement.HALF_STOP,
        referencePhotoZoomRatio = 1.0, createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, remoteId = null,
    )

    private fun exposure(id: String, filmRollId: String, frameNumber: Int) = Exposure(
        id = id, filmRollId = filmRollId, frameNumber = frameNumber, lensId = "lens-1",
        shutterSpeed = ShutterSpeed.fraction(125), aperture = 8.0, isoUsed = 400, zone = null, notes = null,
        capturedAt = 0L, referencePhotoStatus = PhotoStatus.CAPTURED, createdAt = 0L, updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED, remoteId = null,
    )

    @Test
    fun `exportRoll returns null when the roll doesn't exist`() = runTest {
        val coordinator = CsvExportCoordinator(createTestRepository())

        assertNull(coordinator.exportRoll("missing-roll"))
    }

    @Test
    fun `exportRoll produces a CSV of only that roll's exposures`() = runTest {
        val repository = createTestRepository()
        repository.saveCameraBody(cameraBody("body-1"))
        repository.saveFilmBack(filmBack("back-1"))
        repository.saveFilmRoll(roll("roll-1", "Portra 400 — Roll 1"))
        repository.saveFilmRoll(roll("roll-2", "Portra 400 — Roll 2"))
        repository.saveLens(lens("lens-1", "110mm f/2.8"))
        repository.mergeExposureSync(listOf(exposure("exp-1", "roll-1", 1), exposure("exp-2", "roll-2", 1)))
        val coordinator = CsvExportCoordinator(repository)

        val csv = requireNotNull(coordinator.exportRoll("roll-1"))

        val dataLines = csv.lines().drop(1)
        assertEquals(1, dataLines.size)
        assertTrue(dataLines[0].startsWith("Portra 400 — Roll 1,1,110mm f/2.8,"))
    }

    @Test
    fun `exportAll produces a CSV covering every roll, distinguished by the Roll column`() = runTest {
        val repository = createTestRepository()
        repository.saveCameraBody(cameraBody("body-1"))
        repository.saveFilmBack(filmBack("back-1"))
        repository.saveFilmRoll(roll("roll-1", "Alpha Roll"))
        repository.saveFilmRoll(roll("roll-2", "Beta Roll"))
        repository.saveLens(lens("lens-1", "Lens"))
        repository.mergeExposureSync(listOf(exposure("exp-1", "roll-1", 1), exposure("exp-2", "roll-2", 1)))
        val coordinator = CsvExportCoordinator(repository)

        val csv = coordinator.exportAll()

        val rollNames = csv.lines().drop(1).map { it.split(",")[0] }
        assertEquals(listOf("Alpha Roll", "Beta Roll"), rollNames)
    }
}
