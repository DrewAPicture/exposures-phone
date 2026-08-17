package com.exposures.phone.sync

import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.model.CameraBody
import com.exposures.model.FilmBack
import com.exposures.model.FilmBackType
import com.exposures.model.FilmColorType
import com.exposures.model.FilmFormat
import com.exposures.model.FilmRoll
import com.exposures.model.Lens
import com.exposures.model.RollStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.StopIncrement
import com.exposures.model.SyncStatus
import com.exposures.phone.createTestRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RequestRollsSyncHandlerTest {

    private fun cameraBody() = CameraBody(
        id = "body-1",
        name = "RZ67 Pro II",
        manufacturer = "Mamiya",
        availableShutterSpeeds = listOf(ShutterSpeed.fraction(400)),
        hasBulbMode = true,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    private fun filmBack() = FilmBack(
        id = "back-1",
        name = "6x7 back",
        cameraBodyId = "body-1",
        type = FilmBackType.ROLL_6X7,
        availableFrameCounts = listOf(10),
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    private fun roll() = FilmRoll(
        id = "roll-1",
        name = "Portra 400",
        filmStock = "Kodak Portra 400",
        boxSpeedIso = 400,
        format = FilmFormat.MEDIUM_FORMAT_120,
        colorType = FilmColorType.COLOR,
        cameraBodyId = "body-1",
        lightMeterId = null,
        targetFrameCount = 10,
        status = RollStatus.AVAILABLE,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    private fun lens() = Lens(
        id = "lens-1",
        name = "110mm",
        cameraBodyId = "body-1",
        minAperture = 2.8,
        maxAperture = 32.0,
        stopIncrement = StopIncrement.HALF_STOP,
        referencePhotoZoomRatio = 1.0,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    @Test
    fun `handle pushes full equipment snapshot to all equipment data paths`() = runTest {
        val repository = createTestRepository()
        repository.saveCameraBody(cameraBody())
        repository.saveFilmBack(filmBack())
        repository.saveLens(lens())
        repository.saveFilmRoll(roll())
        val gateway = FakeDataLayerGateway()
        val pusher = EquipmentSyncPusher(repository, gateway)

        RequestRollsSyncHandler(pusher).handle()

        val bodiesPayload = requireNotNull(gateway.lastPayload(DataLayerPaths.CAMERA_BODIES))
        val filmBacksPayload = requireNotNull(gateway.lastPayload(DataLayerPaths.FILM_BACKS))
        val lensesPayload = requireNotNull(gateway.lastPayload(DataLayerPaths.LENSES))
        val rollsPayload = requireNotNull(gateway.lastPayload(DataLayerPaths.ROLLS))
        val lightMetersPayload = requireNotNull(gateway.lastPayload(DataLayerPaths.LIGHT_METERS))

        assertEquals("body-1", DataLayerJson.decodeCameraBodies(bodiesPayload).single().id)
        assertEquals("back-1", DataLayerJson.decodeFilmBacks(filmBacksPayload).single().id)
        assertEquals("lens-1", DataLayerJson.decodeLenses(lensesPayload).single().id)
        assertEquals("roll-1", DataLayerJson.decodeRolls(rollsPayload).single().id)
        assertTrue(DataLayerJson.decodeLightMeters(lightMetersPayload).isEmpty())
    }
}
