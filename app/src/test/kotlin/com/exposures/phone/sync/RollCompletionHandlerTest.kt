package com.exposures.phone.sync

import com.exposures.database.repository.EquipmentRepository
import com.exposures.datalayer.DataLayerJson
import com.exposures.datalayer.DataLayerPaths
import com.exposures.model.CameraBody
import com.exposures.model.FilmBack
import com.exposures.model.FilmBackType
import com.exposures.model.FilmColorType
import com.exposures.model.FilmFormat
import com.exposures.model.FilmRoll
import com.exposures.model.RollStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
import com.exposures.phone.createTestRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RollCompletionHandlerTest {

    private fun cameraBody(id: String = "body-1") = CameraBody(
        id = id,
        name = "RZ67 Pro II",
        manufacturer = "Mamiya",
        availableShutterSpeeds = listOf(ShutterSpeed.fraction(400)),
        hasBulbMode = true,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    private fun filmBack(id: String = "back-1") = FilmBack(
        id = id,
        name = "6x7 back",
        cameraBodyId = "body-1",
        type = FilmBackType.ROLL_6X7,
        availableFrameCounts = listOf(10),
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    private fun roll(id: String = "roll-1", status: RollStatus = RollStatus.AVAILABLE) = FilmRoll(
        id = id,
        name = "Portra 400 — Roll 1",
        filmStock = "Kodak Portra 400",
        boxSpeedIso = 400,
        format = FilmFormat.MEDIUM_FORMAT_120,
        colorType = FilmColorType.COLOR,
        cameraBodyId = "body-1",
        lightMeterId = null,
        filmBackId = "back-1",
        targetFrameCount = 10,
        status = status,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    private suspend fun handler(repository: EquipmentRepository, gateway: FakeDataLayerGateway) =
        RollCompletionHandler(repository, EquipmentSyncPusher(repository, gateway))

    @Test
    fun `handle marks the roll completed`() = runTest {
        val repository = createTestRepository()
        repository.saveCameraBody(cameraBody())
        repository.saveFilmBack(filmBack())
        repository.saveFilmRoll(roll())
        val gateway = FakeDataLayerGateway()

        handler(repository, gateway).handle("roll-1")

        assertEquals(RollStatus.COMPLETED, repository.getFilmRoll("roll-1")?.status)
    }

    @Test
    fun `handle re-pushes the roll list so the watch's mirror catches up`() = runTest {
        val repository = createTestRepository()
        repository.saveCameraBody(cameraBody())
        repository.saveFilmBack(filmBack())
        repository.saveFilmRoll(roll())
        val gateway = FakeDataLayerGateway()

        handler(repository, gateway).handle("roll-1")

        val payload = requireNotNull(gateway.lastPayload(DataLayerPaths.ROLLS))
        val rolls = DataLayerJson.decodeRolls(payload)
        assertEquals("COMPLETED", rolls.single { it.id == "roll-1" }.status)
    }

    @Test
    fun `handle is a no-op for an unknown roll id`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway()

        handler(repository, gateway).handle("does-not-exist")

        assertTrue(repository.observeFilmRolls().first().isEmpty())
    }

    @Test
    fun `handle only affects the targeted roll`() = runTest {
        val repository = createTestRepository()
        repository.saveCameraBody(cameraBody())
        repository.saveFilmBack(filmBack())
        repository.saveFilmRoll(roll(id = "roll-1"))
        repository.saveFilmRoll(roll(id = "roll-2"))
        val gateway = FakeDataLayerGateway()

        handler(repository, gateway).handle("roll-1")

        assertEquals(RollStatus.COMPLETED, repository.getFilmRoll("roll-1")?.status)
        assertEquals(RollStatus.AVAILABLE, repository.getFilmRoll("roll-2")?.status)
    }
}
