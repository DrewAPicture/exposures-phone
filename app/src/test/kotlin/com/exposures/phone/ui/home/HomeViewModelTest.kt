package com.exposures.phone.ui.home

import com.exposures.model.Exposure
import com.exposures.model.PhotoStatus
import com.exposures.model.ShutterSpeed
import com.exposures.model.SyncStatus
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

    private fun exposure(id: String, syncStatus: SyncStatus) = Exposure(
        id = id, filmRollId = "roll-1", frameNumber = 1, lensId = "lens-1",
        shutterSpeed = ShutterSpeed.fraction(125), aperture = 8.0, isoUsed = 400, zone = null, notes = null,
        capturedAt = 0L, referencePhotoStatus = PhotoStatus.NONE, createdAt = 0L, updatedAt = 0L,
        syncStatus = syncStatus, remoteId = null,
    )

    @Test
    fun `pendingSyncCount counts exposures not yet uploaded to the backend`() = runTest {
        val repository = createTestRepository()
        repository.mergeExposureSync(
            listOf(exposure("exp-1", SyncStatus.PENDING_SYNC), exposure("exp-2", SyncStatus.SYNCED)),
        )
        val viewModel = HomeViewModel(repository, FakeDataLayerGateway())

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(1, state.pendingSyncCount)
    }

    @Test
    fun `syncNow invokes the injected upload trigger`() = runTest {
        var triggered = false
        val viewModel = HomeViewModel(createTestRepository(), FakeDataLayerGateway(), triggerUpload = { triggered = true })

        viewModel.syncNow()

        assertTrue(triggered)
    }
}
