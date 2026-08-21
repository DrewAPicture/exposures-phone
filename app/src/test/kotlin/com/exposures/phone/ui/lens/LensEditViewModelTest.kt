package com.exposures.phone.ui.lens

import com.exposures.model.StopIncrement
import com.exposures.model.CameraBody
import com.exposures.model.LensType
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LensEditViewModelTest {
    private fun cameraBody(id: String = "body-1", name: String = "RZ67 Pro II") = CameraBody(
        id = id,
        name = name,
        manufacturer = "Mamiya",
        availableShutterSpeeds = listOf(ShutterSpeed.fraction(400)),
        hasBulbMode = true,
        createdAt = 0L,
        updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED,
        remoteId = null,
    )


    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `cannot save with a non-numeric aperture`() = runTest {
        val repository = createTestRepository()
        val viewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }

        viewModel.setName("110mm f/2.8 W")
        viewModel.setMinAperture("not a number")
        viewModel.setMaxAperture("32")

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `cannot save when max aperture is smaller than min aperture`() = runTest {
        val repository = createTestRepository()
        val viewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }

        viewModel.setName("110mm f/2.8 W")
        viewModel.setMinAperture("32")
        viewModel.setMaxAperture("2.8")

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `save persists the parsed aperture values and stop increment`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway()
        repository.saveCameraBody(cameraBody())
        val viewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, gateway), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("110mm f/2.8 W")
        viewModel.setMinAperture("2.8")
        viewModel.setMaxAperture("32")
        viewModel.setStopIncrement(StopIncrement.THIRD_STOP)

        viewModel.save()
        viewModel.uiState.first { it.done }

        val saved = repository.observeLenses().first().single()
        assertEquals(2.8, saved.minAperture, 0.0)
        assertEquals(32.0, saved.maxAperture, 0.0)
        assertEquals(StopIncrement.THIRD_STOP, saved.stopIncrement)
        assertTrue(gateway.putPayloads.isNotEmpty())
    }

    @Test
    fun `new lens defaults camera body to the first available body`() = runTest {
        val repository = createTestRepository()
        repository.saveCameraBody(cameraBody(id = "body-1", name = "RZ67 Pro II"))
        repository.saveCameraBody(cameraBody(id = "body-2", name = "RZ67 Pro"))
        val viewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(state.availableCameraBodies.firstOrNull()?.id, state.cameraBodyId)
    }

    @Test
    fun `save persists selected camera body id`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway()
        repository.saveCameraBody(cameraBody(id = "body-1"))
        repository.saveCameraBody(cameraBody(id = "body-2", name = "RB67 Pro S"))
        val viewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, gateway), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("180mm f/4.5 W")
        viewModel.setMinAperture("4.5")
        viewModel.setMaxAperture("32")
        viewModel.setCameraBody("body-2")

        viewModel.save()
        viewModel.uiState.first { it.done }

        assertEquals("body-2", repository.observeLenses().first().single().cameraBodyId)
    }

    @Test
    fun `defaults to a 1x reference photo zoom for a new lens`() = runTest {
        val repository = createTestRepository()
        val viewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals("1.0", state.referencePhotoZoomRatio)
    }

    @Test
    fun `cannot save with a non-numeric reference photo zoom`() = runTest {
        val repository = createTestRepository()
        val viewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("180mm f/4.5 W")
        viewModel.setMinAperture("4.5")
        viewModel.setMaxAperture("32")

        viewModel.setReferencePhotoZoomRatio("not a number")

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `cannot save with a zero or negative reference photo zoom`() = runTest {
        val repository = createTestRepository()
        val viewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("180mm f/4.5 W")
        viewModel.setMinAperture("4.5")
        viewModel.setMaxAperture("32")

        viewModel.setReferencePhotoZoomRatio("0")

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `save persists the reference photo zoom ratio`() = runTest {
        val repository = createTestRepository()
        val viewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("180mm f/4.5 W")
        viewModel.setMinAperture("4.5")
        viewModel.setMaxAperture("32")
        viewModel.setReferencePhotoZoomRatio("3.0")

        viewModel.save()
        viewModel.uiState.first { it.done }

        assertEquals(3.0, repository.observeLenses().first().single().referencePhotoZoomRatio, 0.0)
    }

    @Test
    fun `editing an existing lens loads its current zoom ratio`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway()
        val createViewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, gateway), null)
        createViewModel.uiState.first { !it.isLoading }
        createViewModel.setName("180mm f/4.5 W")
        createViewModel.setMinAperture("4.5")
        createViewModel.setMaxAperture("32")
        createViewModel.setReferencePhotoZoomRatio("3.0")
        createViewModel.save()
        val savedId = createViewModel.uiState.first { it.done }.let { repository.observeLenses().first().single().id }

        val editViewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, gateway), savedId)

        val state = editViewModel.uiState.first { !it.isLoading }
        assertEquals("3.0", state.referencePhotoZoomRatio)
    }

    @Test
    fun `a new lens defaults to prime`() = runTest {
        val repository = createTestRepository()
        val viewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(LensType.PRIME, state.lensType)
    }

    @Test
    fun `cannot save a prime lens with no focal length`() = runTest {
        val repository = createTestRepository()
        val viewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("Custom Lens")
        viewModel.setMinAperture("2.8")
        viewModel.setMaxAperture("32")

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `setting the lens name auto-detects and prefills a prime focal length`() = runTest {
        val repository = createTestRepository()
        val viewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }

        viewModel.setName("Mamiya-Sekor Z 110mm f/2.8 W")

        assertEquals("110", viewModel.uiState.value.focalLengthMm)
    }

    @Test
    fun `auto-detected focal length never overwrites a value already typed in`() = runTest {
        val repository = createTestRepository()
        val viewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setFocalLengthMm("85")

        viewModel.setName("Mamiya-Sekor Z 110mm f/2.8 W")

        assertEquals("85", viewModel.uiState.value.focalLengthMm)
    }

    @Test
    fun `auto-detect does not apply once the lens type is zoom`() = runTest {
        val repository = createTestRepository()
        val viewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setLensType(LensType.ZOOM)

        viewModel.setName("24-70mm f/2.8")

        assertEquals("", viewModel.uiState.value.focalLengthMm)
    }

    @Test
    fun `save persists a prime lens's focal length`() = runTest {
        val repository = createTestRepository()
        val viewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("50mm f/1.8")
        viewModel.setMinAperture("1.8")
        viewModel.setMaxAperture("16")
        viewModel.setFocalLengthMm("50")

        viewModel.save()
        viewModel.uiState.first { it.done }

        val saved = repository.observeLenses().first().single()
        assertEquals(LensType.PRIME, saved.lensType)
        assertEquals(50, saved.focalLengthMm)
        assertEquals(null, saved.focalLengthMinMm)
        assertEquals(null, saved.focalLengthMaxMm)
    }

    @Test
    fun `cannot save a zoom lens with an inverted focal length range`() = runTest {
        val repository = createTestRepository()
        val viewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("24-70mm f/2.8")
        viewModel.setMinAperture("2.8")
        viewModel.setMaxAperture("22")
        viewModel.setLensType(LensType.ZOOM)
        viewModel.setFocalLengthMinMm("70")
        viewModel.setFocalLengthMaxMm("24")

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `save persists a zoom lens's focal length range`() = runTest {
        val repository = createTestRepository()
        val viewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("24-70mm f/2.8")
        viewModel.setMinAperture("2.8")
        viewModel.setMaxAperture("22")
        viewModel.setLensType(LensType.ZOOM)
        viewModel.setFocalLengthMinMm("24")
        viewModel.setFocalLengthMaxMm("70")

        viewModel.save()
        viewModel.uiState.first { it.done }

        val saved = repository.observeLenses().first().single()
        assertEquals(LensType.ZOOM, saved.lensType)
        assertEquals(null, saved.focalLengthMm)
        assertEquals(24, saved.focalLengthMinMm)
        assertEquals(70, saved.focalLengthMaxMm)
    }

    @Test
    fun `editing an existing zoom lens loads its lens type and focal length range`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway()
        val createViewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, gateway), null)
        createViewModel.uiState.first { !it.isLoading }
        createViewModel.setName("24-70mm f/2.8")
        createViewModel.setMinAperture("2.8")
        createViewModel.setMaxAperture("22")
        createViewModel.setLensType(LensType.ZOOM)
        createViewModel.setFocalLengthMinMm("24")
        createViewModel.setFocalLengthMaxMm("70")
        createViewModel.save()
        val savedId = createViewModel.uiState.first { it.done }.let { repository.observeLenses().first().single().id }

        val editViewModel = LensEditViewModel(repository, EquipmentSyncPusher(repository, gateway), savedId)

        val state = editViewModel.uiState.first { !it.isLoading }
        assertEquals(LensType.ZOOM, state.lensType)
        assertEquals("24", state.focalLengthMinMm)
        assertEquals("70", state.focalLengthMaxMm)
    }
}
