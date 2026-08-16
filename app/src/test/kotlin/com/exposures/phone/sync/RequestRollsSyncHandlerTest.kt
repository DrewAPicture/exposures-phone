package com.exposures.phone.sync

import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.model.CameraBody
import com.exposures.model.FilmFormat
import com.exposures.model.FilmRoll
import com.exposures.model.RollStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
import com.exposures.phone.createTestRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    private fun roll() = FilmRoll(
        id = "roll-1",
        name = "Portra 400",
        filmStock = "Kodak Portra 400",
        boxSpeedIso = 400,
        format = FilmFormat.MEDIUM_FORMAT_120,
        cameraBodyId = "body-1",
        lightMeterId = null,
        targetFrameCount = 10,
        status = RollStatus.AVAILABLE,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    @Test
    fun `handle pushes the current film roll list to the rolls data path`() = runTest {
        val repository = createTestRepository()
        repository.saveCameraBody(cameraBody())
        repository.saveFilmRoll(roll())
        val gateway = FakeDataLayerGateway()
        val pusher = EquipmentSyncPusher(repository, gateway)

        RequestRollsSyncHandler(pusher).handle()

        val payload = requireNotNull(gateway.lastPayload(DataLayerPaths.ROLLS))
        val pushed = DataLayerJson.decodeRolls(payload).single()
        assertEquals("roll-1", pushed.id)
    }
}
