package com.exposures.phone.ui.filmback

import com.exposures.model.CameraBody
import com.exposures.model.FilmBack
import com.exposures.model.FilmBackType
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
class FilmBackListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `ui state includes the associated camera body name`() = runTest {
        val repository = createTestRepository()
        repository.saveCameraBody(
            CameraBody(
                id = "body-1",
                name = "RZ67 Pro II",
                manufacturer = "Mamiya",
                availableShutterSpeeds = listOf(ShutterSpeed.fraction(400)),
                hasBulbMode = true,
                createdAt = 0L,
                updatedAt = 0L,
                syncStatus = SyncStatus.SYNCED,
                remoteId = null,
            ),
        )
        repository.saveFilmBack(
            FilmBack(
                id = "back-1",
                name = "6x7 back",
                cameraBodyId = "body-1",
                type = FilmBackType.ROLL_6X7,
                availableFrameCounts = listOf(10, 11),
                createdAt = 0L,
                updatedAt = 0L,
                syncStatus = SyncStatus.SYNCED,
                remoteId = null,
            ),
        )

        val state = FilmBackListViewModel(repository).uiState.first { !it.isLoading }

        val item = state.filmBacks.single()
        assertEquals("6x7 back", item.filmBack.name)
        assertEquals("RZ67 Pro II", item.cameraBodyName)
    }
}
