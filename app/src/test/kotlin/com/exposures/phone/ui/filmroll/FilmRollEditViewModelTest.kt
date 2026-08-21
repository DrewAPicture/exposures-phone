package com.exposures.phone.ui.filmroll

import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.CameraBody
import com.exposures.model.FilmBack
import com.exposures.model.FilmBackType
import com.exposures.model.FilmColorType
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

    private suspend fun seededCameraBody(repository: EquipmentRepository, id: String = "body-1") = CameraBody(
        id = id, name = "RZ67 Pro II", manufacturer = "Mamiya",
        availableShutterSpeeds = ShutterSpeed.standardRange(ShutterSpeed.fraction(400), ShutterSpeed.wholeSeconds(8)),
        hasBulbMode = true, createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, remoteId = null,
    ).also { repository.saveCameraBody(it) }

    private suspend fun seededLightMeter(repository: EquipmentRepository) = LightMeter(
        id = "meter-1", name = "Spotmeter V", manufacturer = "Pentax", type = LightMeterType.SPOT,
        createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, remoteId = null,
    ).also { repository.saveLightMeter(it) }

    private suspend fun seededFilmBack(repository: EquipmentRepository, cameraBodyId: String, id: String = "back-1") = FilmBack(
        id = id, name = "6x7 back", cameraBodyId = cameraBodyId, type = FilmBackType.ROLL_6X7,
        availableFrameCounts = listOf(10, 11), createdAt = 0L, updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED, remoteId = null,
    ).also { repository.saveFilmBack(it) }

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
    fun `does not auto-select a camera body when more than one exists`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository, id = "body-1")
        seededCameraBody(repository, id = "body-2")
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)

        val state = viewModel.uiState.first { !it.isLoading }

        assertNull(state.cameraBodyId)
    }

    @Test
    fun `auto-selects the film back when exactly one exists for the auto-selected camera body`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository, id = "body-1")
        val back = seededFilmBack(repository, cameraBodyId = "body-1")
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(back.id, state.filmBackId)
    }

    @Test
    fun `does not auto-select a film back when more than one exists for the camera body`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository, id = "body-1")
        seededFilmBack(repository, cameraBodyId = "body-1", id = "back-a")
        seededFilmBack(repository, cameraBodyId = "body-1", id = "back-b")
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)

        val state = viewModel.uiState.first { !it.isLoading }

        assertNull(state.filmBackId)
    }

    @Test
    fun `auto-selects the target frame count when the auto-selected film back has only one available count`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository, id = "body-1")
        val back = FilmBack(
            id = "back-1", name = "6x7 back", cameraBodyId = "body-1", type = FilmBackType.ROLL_6X7,
            availableFrameCounts = listOf(10), createdAt = 0L, updatedAt = 0L,
            syncStatus = SyncStatus.SYNCED, remoteId = null,
        ).also { repository.saveFilmBack(it) }
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(back.id, state.filmBackId)
        assertEquals(10, state.targetFrameCount)
    }

    @Test
    fun `does not auto-select the target frame count when the film back has multiple available counts`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository, id = "body-1")
        seededFilmBack(repository, cameraBodyId = "body-1") // default availableFrameCounts = [10, 11]
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)

        val state = viewModel.uiState.first { !it.isLoading }

        assertNull(state.targetFrameCount)
    }

    @Test
    fun `auto-selects the light meter when exactly one exists`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val meter = seededLightMeter(repository)
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(meter.id, state.lightMeterId)
    }

    @Test
    fun `changing camera body auto-selects the only film back belonging to the new body`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository, id = "body-1")
        seededCameraBody(repository, id = "body-2")
        val backForBody1 = seededFilmBack(repository, cameraBodyId = "body-1", id = "back-1")
        val backForBody2 = seededFilmBack(repository, cameraBodyId = "body-2", id = "back-2")
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setCameraBody("body-1")
        check(viewModel.uiState.value.filmBackId == backForBody1.id)

        viewModel.setCameraBody("body-2")

        assertEquals(backForBody2.id, viewModel.uiState.value.filmBackId)
    }

    @Test
    fun `cannot save without a film back selected`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }

        viewModel.setName("Portra 400 — Roll 1")
        viewModel.setFilmStock("Kodak Portra 400")
        viewModel.setBoxSpeedIso("400")

        assertNull(viewModel.uiState.value.filmBackId)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `save persists the roll and pushes it to the watch`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val back = seededFilmBack(repository, cameraBodyId = "body-1")
        val gateway = FakeDataLayerGateway()
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, gateway), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("Portra 400 — Roll 1")
        viewModel.setFilmStock("Kodak Portra 400")
        viewModel.setBoxSpeedIso("400")
        viewModel.setFilmBack(back.id)
        viewModel.setTargetFrameCount(10)

        viewModel.save()
        viewModel.uiState.first { it.done }

        val saved = repository.observeFilmRolls().first().single()
        assertEquals("Portra 400 — Roll 1", saved.name)
        assertEquals(400, saved.boxSpeedIso)
        assertEquals(back.id, saved.filmBackId)
        assertEquals(10, saved.targetFrameCount)
        assertTrue(gateway.putPayloads.isNotEmpty())
    }

    @Test
    fun `save persists the selected color type`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val back = seededFilmBack(repository, cameraBodyId = "body-1")
        val gateway = FakeDataLayerGateway()
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, gateway), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("HP5 Plus — Roll 1")
        viewModel.setFilmStock("Ilford HP5 Plus")
        viewModel.setBoxSpeedIso("400")
        viewModel.setFilmBack(back.id)
        viewModel.setTargetFrameCount(10)
        viewModel.setColorType(FilmColorType.BLACK_AND_WHITE)

        viewModel.save()
        viewModel.uiState.first { it.done }

        assertEquals(FilmColorType.BLACK_AND_WHITE, repository.observeFilmRolls().first().single().colorType)
    }

    @Test
    fun `editing an existing roll loads its color type`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val back = seededFilmBack(repository, cameraBodyId = "body-1")
        val gateway = FakeDataLayerGateway()
        val createViewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, gateway), null)
        createViewModel.uiState.first { !it.isLoading }
        createViewModel.setName("HP5 Plus — Roll 1")
        createViewModel.setFilmStock("Ilford HP5 Plus")
        createViewModel.setBoxSpeedIso("400")
        createViewModel.setFilmBack(back.id)
        createViewModel.setTargetFrameCount(10)
        createViewModel.setColorType(FilmColorType.BLACK_AND_WHITE)
        createViewModel.save()
        val savedId = createViewModel.uiState.first { it.done }.let { repository.observeFilmRolls().first().single().id }

        val editViewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, gateway), savedId)

        val state = editViewModel.uiState.first { !it.isLoading }
        assertEquals(FilmColorType.BLACK_AND_WHITE, state.colorType)
    }

    @Test
    fun `defaults to black and white for a new roll`() = runTest {
        val repository = createTestRepository()
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(FilmColorType.BLACK_AND_WHITE, state.colorType)
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
        val back = seededFilmBack(repository, cameraBodyId = "body-1")
        seededLightMeter(repository)
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        // The single seeded light meter auto-selects on load — clear it to exercise "no light
        // meter selected" as a deliberate choice, not just an absence of one to auto-fill.
        viewModel.setLightMeter(null)
        viewModel.setName("Portra 400 — Roll 1")
        viewModel.setFilmStock("Kodak Portra 400")
        viewModel.setBoxSpeedIso("400")
        viewModel.setFilmBack(back.id)
        viewModel.setTargetFrameCount(10)

        assertTrue(viewModel.uiState.value.canSave)
        assertNull(viewModel.uiState.value.lightMeterId)
    }

    @Test
    fun `save persists the selected light meter`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val back = seededFilmBack(repository, cameraBodyId = "body-1")
        val meter = seededLightMeter(repository)
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("Portra 400 — Roll 1")
        viewModel.setFilmStock("Kodak Portra 400")
        viewModel.setBoxSpeedIso("400")
        viewModel.setFilmBack(back.id)
        viewModel.setTargetFrameCount(10)
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

    @Test
    fun `changing camera body clears a film back that no longer belongs to it`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository, id = "body-1")
        seededCameraBody(repository, id = "body-2")
        val back = seededFilmBack(repository, cameraBodyId = "body-1")
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setFilmBack(back.id)
        viewModel.setTargetFrameCount(10)

        viewModel.setCameraBody("body-2")

        assertNull(viewModel.uiState.value.filmBackId)
        assertNull(viewModel.uiState.value.targetFrameCount)
    }

    @Test
    fun `changing camera body keeps a film back that still belongs to it`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository, id = "body-1")
        val back = seededFilmBack(repository, cameraBodyId = "body-1")
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setFilmBack(back.id)
        viewModel.setTargetFrameCount(10)

        viewModel.setCameraBody("body-1")

        assertEquals(back.id, viewModel.uiState.value.filmBackId)
        assertEquals(10, viewModel.uiState.value.targetFrameCount)
    }

    @Test
    fun `selecting a film back whose available counts don't include the current target frame count clears it`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository, id = "body-1")
        val backA = seededFilmBack(repository, cameraBodyId = "body-1", id = "back-a")
        val backB = FilmBack(
            id = "back-b", name = "Other back", cameraBodyId = "body-1", type = FilmBackType.ROLL_6X6,
            availableFrameCounts = listOf(12, 13), createdAt = 0L, updatedAt = 0L,
            syncStatus = SyncStatus.SYNCED, remoteId = null,
        ).also { repository.saveFilmBack(it) }
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setFilmBack(backA.id)
        viewModel.setTargetFrameCount(10)

        viewModel.setFilmBack(backB.id)

        assertEquals(backB.id, viewModel.uiState.value.filmBackId)
        assertNull(viewModel.uiState.value.targetFrameCount)
    }

    @Test
    fun `selecting a film back with only one available frame count auto-fills it`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository, id = "body-1")
        val backA = seededFilmBack(repository, cameraBodyId = "body-1", id = "back-a")
        val backB = FilmBack(
            id = "back-b", name = "Other back", cameraBodyId = "body-1", type = FilmBackType.ROLL_6X6,
            availableFrameCounts = listOf(12), createdAt = 0L, updatedAt = 0L,
            syncStatus = SyncStatus.SYNCED, remoteId = null,
        ).also { repository.saveFilmBack(it) }
        val viewModel = FilmRollEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setFilmBack(backA.id)
        viewModel.setTargetFrameCount(10)

        viewModel.setFilmBack(backB.id)

        assertEquals(backB.id, viewModel.uiState.value.filmBackId)
        assertEquals(12, viewModel.uiState.value.targetFrameCount)
    }
}
