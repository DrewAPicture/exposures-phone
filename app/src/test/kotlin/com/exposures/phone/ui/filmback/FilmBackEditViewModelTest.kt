package com.exposures.phone.ui.filmback

import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.CameraBody
import com.exposures.model.FilmBackType
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
class FilmBackEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private suspend fun seededCameraBody(repository: EquipmentRepository, id: String = "body-1") = CameraBody(
        id = id, name = "RZ67 Pro II", manufacturer = "Mamiya",
        availableShutterSpeeds = listOf(ShutterSpeed.fraction(400)),
        hasBulbMode = true, createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, remoteId = null,
    ).also { repository.saveCameraBody(it) }

    @Test
    fun `cannot save without a camera body when none exist yet`() = runTest {
        val repository = createTestRepository()
        val viewModel = FilmBackEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(null, state.cameraBodyId)
        assertFalse(state.canSave)
    }

    @Test
    fun `auto-selects the camera body when exactly one exists`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository, id = "body-1")
        val viewModel = FilmBackEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals("body-1", state.cameraBodyId)
    }

    @Test
    fun `does not auto-select a camera body when more than one exists and none is specified`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository, id = "body-1")
        seededCameraBody(repository, id = "body-2")
        val viewModel = FilmBackEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(null, state.cameraBodyId)
    }

    @Test
    fun `cannot save without a name or a primary frame count`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val viewModel = FilmBackEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }

        assertFalse(viewModel.uiState.value.canSave)

        viewModel.setName("6x7 back")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.setPrimaryFrameCount("10")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `an invalid alternate frame count blocks save even though it's optional`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val viewModel = FilmBackEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("6x7 back")
        viewModel.setPrimaryFrameCount("10")

        viewModel.setAlternateFrameCount("not a number")

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `choosing a type prefills the primary frame count for a brand-new, untouched back`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val viewModel = FilmBackEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }

        viewModel.setType(FilmBackType.ROLL_6X6)

        assertEquals("12", viewModel.uiState.value.primaryFrameCount)
    }

    @Test
    fun `choosing a type does not overwrite a primary frame count the user already typed`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val viewModel = FilmBackEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setPrimaryFrameCount("9")

        viewModel.setType(FilmBackType.ROLL_6X6)

        assertEquals("9", viewModel.uiState.value.primaryFrameCount)
    }

    @Test
    fun `choosing a type while editing an existing back never prefills`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val gateway = FakeDataLayerGateway()
        val createViewModel = FilmBackEditViewModel(repository, EquipmentSyncPusher(repository, gateway), null)
        createViewModel.uiState.first { !it.isLoading }
        createViewModel.setName("6x7 back")
        createViewModel.setPrimaryFrameCount("10")
        createViewModel.save()
        val savedId = createViewModel.uiState.first { it.done }.let { repository.observeFilmBacks().first().single().id }

        val editViewModel = FilmBackEditViewModel(repository, EquipmentSyncPusher(repository, gateway), savedId)
        editViewModel.uiState.first { !it.isLoading }

        editViewModel.setType(FilmBackType.ROLL_6X6)

        assertEquals("10", editViewModel.uiState.value.primaryFrameCount)
    }

    @Test
    fun `save persists a sorted, deduped list of available frame counts`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val gateway = FakeDataLayerGateway()
        val viewModel = FilmBackEditViewModel(repository, EquipmentSyncPusher(repository, gateway), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("6x7 back")
        viewModel.setType(FilmBackType.ROLL_6X7)
        viewModel.setPrimaryFrameCount("11")
        viewModel.setAlternateFrameCount("10")

        viewModel.save()
        viewModel.uiState.first { it.done }

        val saved = repository.observeFilmBacks().first().single()
        assertEquals(listOf(10, 11), saved.availableFrameCounts)
        assertTrue(gateway.putPayloads.isNotEmpty())
    }

    @Test
    fun `a blank alternate frame count saves just the primary count`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val viewModel = FilmBackEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("6x7 back")
        viewModel.setPrimaryFrameCount("10")

        viewModel.save()
        viewModel.uiState.first { it.done }

        assertEquals(listOf(10), repository.observeFilmBacks().first().single().availableFrameCounts)
    }

    @Test
    fun `editing an existing back loads its name, type, and frame counts`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val gateway = FakeDataLayerGateway()
        val createViewModel = FilmBackEditViewModel(repository, EquipmentSyncPusher(repository, gateway), null)
        createViewModel.uiState.first { !it.isLoading }
        createViewModel.setName("6x6 back")
        createViewModel.setType(FilmBackType.ROLL_6X6)
        createViewModel.setPrimaryFrameCount("12")
        createViewModel.setAlternateFrameCount("13")
        createViewModel.save()
        val savedId = createViewModel.uiState.first { it.done }.let { repository.observeFilmBacks().first().single().id }

        val editViewModel = FilmBackEditViewModel(repository, EquipmentSyncPusher(repository, gateway), savedId)

        val state = editViewModel.uiState.first { !it.isLoading }
        assertEquals("6x6 back", state.name)
        assertEquals(FilmBackType.ROLL_6X6, state.type)
        assertEquals("12", state.primaryFrameCount)
        assertEquals("13", state.alternateFrameCount)
    }

    @Test
    fun `a new back with an initial camera body seeds the camera body dropdown`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository, id = "body-1")
        seededCameraBody(repository, id = "body-2")
        val viewModel = FilmBackEditViewModel(
            repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null, initialCameraBodyId = "body-2",
        )

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals("body-2", state.cameraBodyId)
    }

    @Test
    fun `an initial camera body that no longer exists falls back to the first available body`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository, id = "body-1")
        val viewModel = FilmBackEditViewModel(
            repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null, initialCameraBodyId = "missing-body",
        )

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals("body-1", state.cameraBodyId)
    }

    @Test
    fun `editing an existing back ignores the initial camera body`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository, id = "body-1")
        seededCameraBody(repository, id = "body-2")
        val gateway = FakeDataLayerGateway()
        val createViewModel = FilmBackEditViewModel(repository, EquipmentSyncPusher(repository, gateway), null)
        createViewModel.uiState.first { !it.isLoading }
        // Two bodies exist, so camera body isn't auto-selected here — pick one explicitly.
        createViewModel.setCameraBody("body-1")
        createViewModel.setName("6x7 back")
        createViewModel.setPrimaryFrameCount("10")
        createViewModel.save()
        val savedId = createViewModel.uiState.first { it.done }.let { repository.observeFilmBacks().first().single().id }

        val editViewModel = FilmBackEditViewModel(
            repository, EquipmentSyncPusher(repository, gateway), savedId, initialCameraBodyId = "body-2",
        )

        val state = editViewModel.uiState.first { !it.isLoading }
        assertEquals("body-1", state.cameraBodyId)
    }

    @Test
    fun `save exposes the new back's id as savedId`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val viewModel = FilmBackEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("6x7 back")
        viewModel.setPrimaryFrameCount("10")

        viewModel.save()

        val state = viewModel.uiState.first { it.done }
        assertEquals(repository.observeFilmBacks().first().single().id, state.savedId)
    }

    @Test
    fun `delete never sets savedId`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val gateway = FakeDataLayerGateway()
        val createViewModel = FilmBackEditViewModel(repository, EquipmentSyncPusher(repository, gateway), null)
        createViewModel.uiState.first { !it.isLoading }
        createViewModel.setName("6x7 back")
        createViewModel.setPrimaryFrameCount("10")
        createViewModel.save()
        val savedId = createViewModel.uiState.first { it.done }.let { repository.observeFilmBacks().first().single().id }
        val editViewModel = FilmBackEditViewModel(repository, EquipmentSyncPusher(repository, gateway), savedId)
        editViewModel.uiState.first { !it.isLoading }

        editViewModel.delete()

        val state = editViewModel.uiState.first { it.done }
        assertNull(state.savedId)
    }

    @Test
    fun `delete removes the film back and pushes to the watch`() = runTest {
        val repository = createTestRepository()
        seededCameraBody(repository)
        val gateway = FakeDataLayerGateway()
        val createViewModel = FilmBackEditViewModel(repository, EquipmentSyncPusher(repository, gateway), null)
        createViewModel.uiState.first { !it.isLoading }
        createViewModel.setName("6x7 back")
        createViewModel.setPrimaryFrameCount("10")
        createViewModel.save()
        val savedId = createViewModel.uiState.first { it.done }.let { repository.observeFilmBacks().first().single().id }
        val editViewModel = FilmBackEditViewModel(repository, EquipmentSyncPusher(repository, gateway), savedId)
        editViewModel.uiState.first { !it.isLoading }

        editViewModel.delete()
        editViewModel.uiState.first { it.done }

        assertTrue(repository.observeFilmBacks().first().isEmpty())
    }
}
