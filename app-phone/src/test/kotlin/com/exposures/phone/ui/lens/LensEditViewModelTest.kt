package com.exposures.phone.ui.lens

import com.exposures.model.StopIncrement
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
}
