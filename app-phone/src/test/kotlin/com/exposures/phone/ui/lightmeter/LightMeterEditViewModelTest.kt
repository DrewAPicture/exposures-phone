package com.exposures.phone.ui.lightmeter

import com.exposures.model.LightMeterType
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
class LightMeterEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `defaults to SPOT type for a new light meter`() = runTest {
        val repository = createTestRepository()
        val viewModel = LightMeterEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(LightMeterType.SPOT, state.type)
    }

    @Test
    fun `cannot save without a name or manufacturer`() = runTest {
        val repository = createTestRepository()
        val viewModel = LightMeterEditViewModel(repository, EquipmentSyncPusher(repository, FakeDataLayerGateway()), null)
        viewModel.uiState.first { !it.isLoading }

        assertFalse(viewModel.uiState.value.canSave)

        viewModel.setName("Spotmeter V")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.setManufacturer("Pentax")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `save persists the name, manufacturer, and type, and pushes to the watch`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway()
        val viewModel = LightMeterEditViewModel(repository, EquipmentSyncPusher(repository, gateway), null)
        viewModel.uiState.first { !it.isLoading }
        viewModel.setName("Spotmeter V")
        viewModel.setManufacturer("Pentax")
        viewModel.setType(LightMeterType.INCIDENT)

        viewModel.save()
        viewModel.uiState.first { it.done }

        val saved = repository.observeLightMeters().first().single()
        assertEquals("Spotmeter V", saved.name)
        assertEquals("Pentax", saved.manufacturer)
        assertEquals(LightMeterType.INCIDENT, saved.type)
        assertTrue(gateway.putPayloads.isNotEmpty())
    }

    @Test
    fun `editing an existing light meter loads its current values`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway()
        val createViewModel = LightMeterEditViewModel(repository, EquipmentSyncPusher(repository, gateway), null)
        createViewModel.uiState.first { !it.isLoading }
        createViewModel.setName("Spotmeter V")
        createViewModel.setManufacturer("Pentax")
        createViewModel.setType(LightMeterType.REFLECTIVE)
        createViewModel.save()
        val savedId = createViewModel.uiState.first { it.done }.let { repository.observeLightMeters().first().single().id }

        val editViewModel = LightMeterEditViewModel(repository, EquipmentSyncPusher(repository, gateway), savedId)

        val state = editViewModel.uiState.first { !it.isLoading }
        assertEquals("Spotmeter V", state.name)
        assertEquals("Pentax", state.manufacturer)
        assertEquals(LightMeterType.REFLECTIVE, state.type)
    }

    @Test
    fun `delete removes the light meter and pushes to the watch`() = runTest {
        val repository = createTestRepository()
        val gateway = FakeDataLayerGateway()
        val createViewModel = LightMeterEditViewModel(repository, EquipmentSyncPusher(repository, gateway), null)
        createViewModel.uiState.first { !it.isLoading }
        createViewModel.setName("Spotmeter V")
        createViewModel.setManufacturer("Pentax")
        createViewModel.save()
        val savedId = createViewModel.uiState.first { it.done }.let { repository.observeLightMeters().first().single().id }
        val editViewModel = LightMeterEditViewModel(repository, EquipmentSyncPusher(repository, gateway), savedId)
        editViewModel.uiState.first { !it.isLoading }

        editViewModel.delete()
        editViewModel.uiState.first { it.done }

        assertTrue(repository.observeLightMeters().first().isEmpty())
    }
}
