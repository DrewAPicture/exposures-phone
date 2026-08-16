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
}
