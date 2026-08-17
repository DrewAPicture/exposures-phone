package com.exposures.phone.ui.home

import com.exposures.model.CameraBody
import com.exposures.model.Exposure
import com.exposures.model.FilmBack
import com.exposures.model.FilmBackType
import com.exposures.model.PhotoStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
import com.exposures.phone.MainDispatcherRule
import com.exposures.phone.createTestRepository
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
        val viewModel = HomeViewModel(repository)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(0, state.cameraBodyCount)
        assertEquals(0, state.lensCount)
        assertEquals(0, state.lightMeterCount)
        assertEquals(0, state.filmBackCount)
        assertEquals(0, state.filmRollCount)
        assertEquals(0, state.exposureCount)
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
        val viewModel = HomeViewModel(repository)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(2, state.exposureCount)
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
        val viewModel = HomeViewModel(repository)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(1, state.filmBackCount)
    }

    private fun exposure(id: String, syncStatus: SyncStatus) = Exposure(
        id = id,
        filmRollId = "roll-1",
        frameNumber = 1,
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
        syncStatus = syncStatus,
        remoteId = null,
    )
}
