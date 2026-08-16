package com.exposures.phone.ui.filmroll

import com.exposures.model.CameraBody
import com.exposures.model.FilmFormat
import com.exposures.model.FilmRoll
import com.exposures.model.RollStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
import com.exposures.phone.MainDispatcherRule
import com.exposures.phone.createTestRepository
import com.exposures.phone.export.CsvExportCoordinator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FilmRollListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `uiState reflects the repository's rolls`() = runTest {
        val repository = createTestRepository()
        repository.saveCameraBody(
            CameraBody(
                id = "body-1", name = "RZ67", manufacturer = "Mamiya", availableShutterSpeeds = listOf(ShutterSpeed.fraction(125)),
                hasBulbMode = true, createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, remoteId = null,
            ),
        )
        repository.saveFilmRoll(
            FilmRoll(
                id = "roll-1", name = "Portra 400 — Roll 1", filmStock = "Portra 400", boxSpeedIso = 400,
                format = FilmFormat.MEDIUM_FORMAT_120, cameraBodyId = "body-1", lightMeterId = null,
                targetFrameCount = 10, status = RollStatus.AVAILABLE,
                createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, remoteId = null,
            ),
        )
        val viewModel = FilmRollListViewModel(repository, CsvExportCoordinator(repository))

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(listOf("Portra 400 — Roll 1"), state.rolls.map { it.name })
    }

    @Test
    fun `exportCsv delegates to the csv export coordinator`() = runTest {
        val repository = createTestRepository()
        val viewModel = FilmRollListViewModel(repository, CsvExportCoordinator(repository))

        assertNull(viewModel.exportCsv("missing-roll"))
    }
}
