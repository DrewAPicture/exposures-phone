package com.exposures.phone.ui.filmroll

import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.CameraBody
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
import com.exposures.phone.MainDispatcherRule
import com.exposures.phone.createTestRepository
import com.exposures.phone.sync.EquipmentSyncPusher
import com.exposures.phone.sync.FakeDataLayerGateway
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FilmRollEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private suspend fun seededCameraBody(repository: EquipmentRepository): CameraBody {
        val body = CameraBody(
            id = "body-1", name = "RZ67 Pro II", manufacturer = "Mamiya",
            availableShutterSpeeds = ShutterSpeed.standardRange(ShutterSpeed.fraction(400), ShutterSpeed.wholeSeconds(8)),
            hasBulbMode = true, createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, remoteId = null,
        )
        repository.saveCameraBody(body)
        return body
    }

    @Test
    fun `cannot save without a camera body when none exist yet`() = runTest {
        val repository = createTestRepository()
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        val state = viewModel.uiState.first { !it.isLoading }

        assertNull(state.cameraBodyId)
        assertFalse(state.canSave)
    }

    @Test
    fun `defaults to the first available camera body when one exists`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals("body-1", state.cameraBodyId)
    }

    @Test
    fun `cannot save with a non-positive target frame count`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }

        viewModel.setName("Portra 400 — Roll 1")
        viewModel.setFilmStock("Kodak Portra 400")
        viewModel.setBoxSpeedIso("400")
        viewModel.setTargetFrameCount("0")

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `save persists the roll and pushes it to the watch`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val gateway = FakeDataLayerGateway()
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, gateway), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("Portra 400 — Roll 1")
        viewModel.setFilmStock("Kodak Portra 400")
        viewModel.setBoxSpeedIso("400")
        viewModel.setTargetFrameCount("10")

        viewModel.save()
        viewModel.uiState.first { it.done }

        val saved = repository.observeFilmRolls().first().single()
        assertEquals("Portra 400 — Roll 1", saved.name)
        assertEquals(400, saved.boxSpeedIso)
        assertEquals(10, saved.targetFrameCount)
        assertTrue(gateway.putPayloads.isNotEmpty())
    }
}
