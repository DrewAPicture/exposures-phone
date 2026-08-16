package com.exposures.phone.ui.filmroll

import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.CameraBody
import com.exposures.model.LightMeter
import com.exposures.model.LightMeterType
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

    private suspend fun seededLightMeter(repository: EquipmentRepository): LightMeter {
        val meter = LightMeter(
            id = "meter-1", name = "Spotmeter V", manufacturer = "Pentax", type = LightMeterType.SPOT,
            createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, remoteId = null,
        )
        repository.saveLightMeter(meter)
        return meter
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

    @Test
    fun `defaults to no light meter when none is selected`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)

        val state = viewModel.uiState.first { !it.isLoading }

        assertNull(state.lightMeterId)
    }

    @Test
    fun `a roll is savable with no light meter selected`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        seededLightMeter(repository)
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("Portra 400 — Roll 1")
        viewModel.setFilmStock("Kodak Portra 400")
        viewModel.setBoxSpeedIso("400")
        viewModel.setTargetFrameCount("10")

        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `save persists the selected light meter`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val meter = seededLightMeter(repository)
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("Portra 400 — Roll 1")
        viewModel.setFilmStock("Kodak Portra 400")
        viewModel.setBoxSpeedIso("400")
        viewModel.setTargetFrameCount("10")
        viewModel.setLightMeter(meter.id)

        viewModel.save()
        viewModel.uiState.first { it.done }

        assertEquals(meter.id, repository.observeFilmRolls().first().single().lightMeterId)
    }

    @Test
    fun `setting the light meter back to null clears a previously selected one`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val meter = seededLightMeter(repository)
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setLightMeter(meter.id)

        viewModel.setLightMeter(null)

        assertNull(viewModel.uiState.value.lightMeterId)
    }
}
