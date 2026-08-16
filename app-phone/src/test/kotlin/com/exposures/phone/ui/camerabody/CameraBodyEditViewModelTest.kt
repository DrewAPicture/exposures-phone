package com.exposures.phone.ui.camerabody

import com.exposures.database.repository.EquipmentRepository
import com.exposures.model.ShutterSpeed
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
class CameraBodyEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun newViewModel(
        repository: EquipmentRepository = createTestRepository(),
        gateway: FakeDataLayerGateway = FakeDataLayerGateway(),
        existingId: String? = null,
    ) = Triple(CameraBodyEditViewModel(repository, EquipmentSyncPusher(repository, gateway), existingId), repository, gateway)

    @Test
    fun `a new body cannot be saved until name and manufacturer are filled in`() = runTest {
        val (viewModel, _, _) = newViewModel()
        viewModel.uiState.first { !it.isLoading }
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.setName("RZ67 Pro II")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.setManufacturer("Mamiya")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `cannot save when the fastest speed is slower than the slowest speed`() = runTest {
        val (viewModel, _, _) = newViewModel()
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("RZ67 Pro II")
        viewModel.setManufacturer("Mamiya")

        viewModel.setFastestShutterSpeed(ShutterSpeed.wholeSeconds(8))
        viewModel.setSlowestShutterSpeed(ShutterSpeed.fraction(400))

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `save computes the standard shutter range and pushes it to the watch`() = runTest {
        val (viewModel, repository, gateway) = newViewModel()
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("RZ67 Pro II")
        viewModel.setManufacturer("Mamiya")
        viewModel.setFastestShutterSpeed(ShutterSpeed.fraction(400))
        viewModel.setSlowestShutterSpeed(ShutterSpeed.wholeSeconds(8))
        viewModel.setHasBulbMode(true)

        viewModel.save()

        val state = viewModel.uiState.first { it.done }
        val saved = repository.observeCameraBodies().first().single()
        assertEquals(
            ShutterSpeed.standardRange(ShutterSpeed.fraction(400), ShutterSpeed.wholeSeconds(8), includeBulb = true),
            saved.availableShutterSpeeds,
        )
        assertTrue(state.done)
        assertTrue(gateway.putPayloads.isNotEmpty())
    }

    @Test
    fun `editing an existing body loads its current values`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway()
        val (createViewModel, _, _) = newViewModel(repository, gateway)
        createViewModel.setName("RZ67 Pro II")
        createViewModel.setManufacturer("Mamiya")
        createViewModel.setFastestShutterSpeed(ShutterSpeed.fraction(400))
        createViewModel.setSlowestShutterSpeed(ShutterSpeed.wholeSeconds(8))
        createViewModel.save()
        val savedId = createViewModel.uiState.first { it.done }.let { repository.observeCameraBodies().first().single().id }

        val (editViewModel, _, _) = newViewModel(repository, gateway, existingId = savedId)

        val state = editViewModel.uiState.first { !it.isLoading }
        assertEquals("RZ67 Pro II", state.name)
        assertFalse(state.isNew)
    }

    @Test
    fun `delete is a no-op for a body that was never saved`() = runTest {
        val (viewModel, repository, gateway) = newViewModel()
        viewModel.uiState.first { !it.isLoading }

        viewModel.delete()

        assertTrue(repository.observeCameraBodies().first().isEmpty())
        assertTrue(gateway.putPayloads.isEmpty())
    }
}
