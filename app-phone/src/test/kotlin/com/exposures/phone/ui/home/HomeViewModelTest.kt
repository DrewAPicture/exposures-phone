package com.exposures.phone.ui.home

import com.exposures.phone.MainDispatcherRule
import com.exposures.phone.createTestRepository
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
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `reports the watch as reachable when the gateway finds a node`() = runTest {
        val gateway = FakeDataLayerGateway().apply { reachableNodeId = "watch-node" }
        val viewModel = HomeViewModel(createTestRepository(), gateway)

        val state = viewModel.uiState.first { it.watchReachable != null }

        assertTrue(state.watchReachable == true)
    }

    @Test
    fun `reports the watch as unreachable when the gateway finds no node`() = runTest {
        val gateway = FakeDataLayerGateway().apply { reachableNodeId = null }
        val viewModel = HomeViewModel(createTestRepository(), gateway)

        val state = viewModel.uiState.first { it.watchReachable != null }

        assertFalse(state.watchReachable == true)
    }

    @Test
    fun `refreshPairingStatus re-checks reachability`() = runTest {
        val gateway = FakeDataLayerGateway().apply { reachableNodeId = null }
        val viewModel = HomeViewModel(createTestRepository(), gateway)
        viewModel.uiState.first { it.watchReachable != null }

        gateway.reachableNodeId = "watch-node"
        viewModel.refreshPairingStatus()

        val state = viewModel.uiState.first { it.watchReachable == true }
        assertEquals(true, state.watchReachable)
    }
}
