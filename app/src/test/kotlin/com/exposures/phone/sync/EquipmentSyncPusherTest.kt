package com.exposures.phone.sync

import com.exposures.database.repository.EquipmentRepository
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.model.CameraBody
import com.exposures.model.FilmFormat
import com.exposures.model.FilmRoll
import com.exposures.model.Lens
import com.exposures.model.LightMeter
import com.exposures.model.LightMeterType
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
class EquipmentSyncPusherTest {

    private fun cameraBody() = CameraBody(
        id = "body-1", name = "RZ67 Pro II", manufacturer = "Mamiya",
        availableShutterSpeeds = ShutterSpeed.standardRange(ShutterSpeed.fraction(400), ShutterSpeed.wholeSeconds(8)),
        hasBulbMode = true, createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.PENDING_SYNC, remoteId = null,
    )

    private fun lens() = Lens(
        id = "lens-1", name = "110mm f/2.8 W", cameraBodyId = null, minAperture = 2.8, maxAperture = 32.0,
        stopIncrement = StopIncrement.HALF_STOP, referencePhotoZoomRatio = 1.0, createdAt = 0L, updatedAt = 0L,
        syncStatus = SyncStatus.PENDING_SYNC, remoteId = null,
    )

    private fun lightMeter() = LightMeter(
        id = "meter-1", name = "Spotmeter V", manufacturer = "Pentax", type = LightMeterType.SPOT,
        createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.PENDING_SYNC, remoteId = null,
    )

    private fun filmRoll() = FilmRoll(
        id = "roll-1", name = "Portra 400", filmStock = "Kodak Portra 400", boxSpeedIso = 400,
        format = FilmFormat.MEDIUM_FORMAT_120, cameraBodyId = "body-1", lightMeterId = null, targetFrameCount = 10,
        status = RollStatus.AVAILABLE, createdAt = 0L, updatedAt = 0L,
        syncStatus = SyncStatus.PENDING_SYNC, remoteId = null,
    )

    @Test
    fun `pushCameraBodies puts the current list at the camera-bodies path`() = runTest {
        val repository: EquipmentRepository = createTestRepository()
        repository.saveCameraBody(cameraBody())
        val gateway = FakeDataLayerGateway()
        val pusher = EquipmentSyncPusher(repository, gateway)

        pusher.pushCameraBodies()

        val payload = requireNotNull(gateway.lastPayload(DataLayerPaths.CAMERA_BODIES))
        assertEquals("body-1", DataLayerJson.decodeCameraBodies(payload).single().id)
    }

    @Test
    fun `pushLenses puts the current list at the lenses path`() = runTest {
        val repository = createTestRepository()
        repository.saveLens(lens())
        val gateway = FakeDataLayerGateway()
        val pusher = EquipmentSyncPusher(repository, gateway)

        pusher.pushLenses()

        val payload = requireNotNull(gateway.lastPayload(DataLayerPaths.LENSES))
        assertEquals("lens-1", DataLayerJson.decodeLenses(payload).single().id)
    }

    @Test
    fun `pushLightMeters puts the current list at the light-meters path`() = runTest {
        val repository = createTestRepository()
        repository.saveLightMeter(lightMeter())
        val gateway = FakeDataLayerGateway()
        val pusher = EquipmentSyncPusher(repository, gateway)

        pusher.pushLightMeters()

        val payload = requireNotNull(gateway.lastPayload(DataLayerPaths.LIGHT_METERS))
        assertEquals("meter-1", DataLayerJson.decodeLightMeters(payload).single().id)
    }

    @Test
    fun `pushFilmRolls puts the current list at the rolls path`() = runTest {
        val repository = createTestRepository()
        repository.saveCameraBody(cameraBody()) // filmRoll() FKs to this camera body
        repository.saveFilmRoll(filmRoll())
        val gateway = FakeDataLayerGateway()
        val pusher = EquipmentSyncPusher(repository, gateway)

        pusher.pushFilmRolls()

        val payload = requireNotNull(gateway.lastPayload(DataLayerPaths.ROLLS))
        assertEquals("roll-1", DataLayerJson.decodeRolls(payload).single().id)
    }

    @Test
    fun `pushing an empty equipment list still writes an empty payload, not nothing`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway()
        val pusher = EquipmentSyncPusher(repository, gateway)

        pusher.pushCameraBodies()

        assertTrue(DataLayerJson.decodeCameraBodies(requireNotNull(gateway.lastPayload(DataLayerPaths.CAMERA_BODIES))).isEmpty())
    }
}
