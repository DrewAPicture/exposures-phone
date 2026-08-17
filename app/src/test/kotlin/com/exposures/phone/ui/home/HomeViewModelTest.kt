package com.exposures.phone.ui.home

import com.exposures.model.CameraBody
import com.exposures.model.Exposure
import com.exposures.model.FilmColorType
import com.exposures.model.FilmBack
import com.exposures.model.FilmBackType
import com.exposures.model.FilmFormat
import com.exposures.model.FilmRoll
import com.exposures.model.PhotoStatus
import com.exposures.model.RollStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
import com.exposures.phone.MainDispatcherRule
import com.exposures.phone.createTestRepository
import com.exposures.phone.sync.FakeDataLayerGateway
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `default home state is empty`() = runTest {
        val repository = createTestRepository()
        val viewModel = HomeViewModel(repository, FakeDataLayerGateway())

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(0, state.cameraBodyCount)
        assertEquals(0, state.lensCount)
        assertEquals(0, state.lightMeterCount)
        assertEquals(0, state.filmBackCount)
        assertEquals(0, state.filmRollCount)
        assertEquals(0, state.exposureCount)
        assertEquals(0, state.syncedTodayCount)
        assertEquals(0, state.pendingSyncCount)
        assertEquals("-", state.favoriteCameraName)
    }

    @Test
    fun `exposure count reflects synced watch exposures`() = runTest {
        val repository = createTestRepository()
        repository.mergeExposureSync(
            listOf(
                exposure("exp-1", SyncStatus.PENDING_SYNC),
                exposure("exp-2", SyncStatus.SYNCED),
            ),
        )
        val viewModel = HomeViewModel(repository, FakeDataLayerGateway())

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(2, state.exposureCount)
        assertEquals(1, state.pendingSyncCount)
    }

    @Test
    fun `syncedTodayCount only includes exposures captured today`() = runTest {
        val repository = createTestRepository()
        val zoneId = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zoneId).toInstant().toEpochMilli()
        val yesterday = ZonedDateTime.now(zoneId).minusDays(1).toInstant().toEpochMilli()
        repository.mergeExposureSync(
            listOf(
                exposure("today-1", SyncStatus.SYNCED, capturedAt = now),
                exposure("yesterday-1", SyncStatus.SYNCED, capturedAt = yesterday),
            ),
        )
        val viewModel = HomeViewModel(repository, FakeDataLayerGateway())

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(1, state.syncedTodayCount)
    }

    @Test
    fun `filmBackCount reflects the number of configured film backs`() = runTest {
        val repository = createTestRepository()
        val body = CameraBody(
            id = "body-1", name = "RZ67 Pro II", manufacturer = "Mamiya",
            availableShutterSpeeds = listOf(ShutterSpeed.fraction(400)),
            hasBulbMode = true, createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, remoteId = null,
        )
        repository.saveCameraBody(body)
        repository.saveFilmBack(
            FilmBack(
                id = "back-1", name = "6x7 back", cameraBodyId = body.id, type = FilmBackType.ROLL_6X7,
                availableFrameCounts = listOf(10), createdAt = 0L, updatedAt = 0L,
                syncStatus = SyncStatus.SYNCED, remoteId = null,
            ),
        )
        val viewModel = HomeViewModel(repository, FakeDataLayerGateway())

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(1, state.filmBackCount)
    }

    @Test
    fun `favoriteCameraName reflects camera with most rolls`() = runTest {
        val repository = createTestRepository()
        val bodyA = cameraBody("body-a", "RZ67")
        val bodyB = cameraBody("body-b", "Hasselblad")
        repository.saveCameraBody(bodyA)
        repository.saveCameraBody(bodyB)
        repository.saveFilmBack(filmBack("back-a", bodyA.id))
        repository.saveFilmBack(filmBack("back-b", bodyB.id))
        repository.saveFilmRoll(filmRoll("roll-1", bodyA.id, "back-a"))
        repository.saveFilmRoll(filmRoll("roll-2", bodyA.id, "back-a"))
        repository.saveFilmRoll(filmRoll("roll-3", bodyB.id, "back-b"))

        val viewModel = HomeViewModel(repository, FakeDataLayerGateway())
        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals("RZ67", state.favoriteCameraName)
    }

    private fun cameraBody(id: String, name: String) = CameraBody(
        id = id,
        name = name,
        manufacturer = "Test",
        availableShutterSpeeds = listOf(ShutterSpeed.fraction(125)),
        hasBulbMode = true,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    private fun filmBack(id: String, cameraBodyId: String) = FilmBack(
        id = id,
        name = "6x7 back",
        cameraBodyId = cameraBodyId,
        type = FilmBackType.ROLL_6X7,
        availableFrameCounts = listOf(10),
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    private fun filmRoll(id: String, cameraBodyId: String, filmBackId: String) = FilmRoll(
        id = id,
        name = id,
        filmStock = "Tri-X",
        boxSpeedIso = 400,
        format = FilmFormat.MEDIUM_FORMAT_120,
        colorType = FilmColorType.BLACK_AND_WHITE,
        cameraBodyId = cameraBodyId,
        lightMeterId = null,
        filmBackId = filmBackId,
        targetFrameCount = 10,
        status = RollStatus.AVAILABLE,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )

    private fun exposure(id: String, syncStatus: SyncStatus, capturedAt: Long = 0L) = Exposure(
        id = id,
        filmRollId = "roll-1",
        frameNumber = 1,
        lensId = "lens-1",
        shutterSpeed = ShutterSpeed.fraction(125),
        aperture = 8.0,
        isoUsed = 400,
        zone = null,
        notes = null,
        capturedAt = capturedAt,
        referencePhotoStatus = PhotoStatus.NONE,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = syncStatus,
        remoteId = null,
    )
}
